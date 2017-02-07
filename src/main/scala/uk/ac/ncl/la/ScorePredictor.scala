/** student-attainment-predictor
  *
  * Copyright (c) 2017 Hugo Firth
  * Email: <me@hugofirth.com/>
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at:
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
package uk.ac.ncl.la

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths, StandardOpenOption}

import org.apache.spark.{SparkConf, SparkContext}
import cats._
import cats.data._
import cats.implicits._
import org.apache.log4j.{Level, LogManager}
import org.apache.spark.mllib.evaluation.RegressionMetrics
import org.apache.spark.mllib.recommendation.{ALS, MatrixFactorizationModel, Rating}
import org.apache.spark.rdd.RDD
import resource._

import scala.util.control.Exception._

/** Simple singleton entry point to Spark job
  *
  * Refactor if we turn it into a long running web service
  *
  * @author hugofirth
  */
object ScorePredictor {

  /** Some type aliases for clarity */
  type StudentNumber = String
  type ModuleCode = Int

  /** General purpose Record Struct */
  final class ModuleRecord private (val student: StudentNumber, val module: ModuleCode, val score: Double)

  /** Record Companion */
  object ModuleRecord {

    /** apply method parses a record string (comma separated) */
    def apply(record: String): Option[ModuleRecord] = record.split(",").toList match {
      case List(st, mc, sc) =>
        //Parse elements of record, returning None in the event of an error
        for {
          module <- catching(classOf[NumberFormatException]) opt mc.toInt
          score <- catching(classOf[NumberFormatException]) opt sc.toDouble
          if score >= 0 && score <= 100
        } yield new ModuleRecord(st, module, score)
      case _ => None
    }
  }

  /** Syntax enrichment from ModuleRecord to built in Rating type */
  final implicit class ModuleRecordOps(val mr: ModuleRecord) extends AnyVal {
    @inline def toRating: Rating = Rating(mr.student.toInt, mr.module, mr.score)
  }

  def main(args: Array[String]): Unit = {
    //Set up the logger
    val log = LogManager.getRootLogger
    log.setLevel(Level.INFO)

    //Create a config object from the command line arguments provided
    val parseCli = parser.parse(args, Config()).fold {
      Left(new IllegalArgumentException("Failed to correctly parse command line arguments!")): Either[Throwable, Config]
    } { Right(_) }

    //Compose stages of job
    for {
      conf <- parseCli.right
      spark <- boot(conf).right
      recs <- readrecords(spark, conf).right
      model <- produceModel(spark, recs, conf).right
      _ <- writeOut(spark, model._1, model._2, recs.count(), conf).right
    } yield { r: Either[Throwable, Unit] =>
      r match {
        case Left(e) =>
          //In the event of an error, log and crash out.
          log.error(e.toString)
          sys.exit(1)
        case Right(_) =>
          //In the event of a successful job, log and finish
          log.info("Job finished.")
      }
    }

  }

  /** Create and configure the spark context */
  private def boot(conf: Config): Either[Throwable, SparkContext] = Either.catchNonFatal {
    //Create spark config object from options provided
    val sConf = new SparkConf()
      .setAppName("ScorePredictor")
      .setMaster(conf.master)
      .set("spark.executor.memory", conf.exMem)
    //Create SC if it doesn't already exist
    SparkContext.getOrCreate(sConf)
  }

  /** Import training data */
  private def readrecords(spark: SparkContext, conf: Config): Either[Throwable, RDD[ModuleRecord]] =
    Either.catchNonFatal {

      //Import the records
      val recordLines = spark.textFile(conf.recordsPath)
      //Parse the records and drop errors
      recordLines.flatMap(line => ModuleRecord(line))

    }


  /** Produce the predictive model */
  private def produceModel(spark: SparkContext,
                           records: RDD[ModuleRecord],
                           conf: Config): Either[Throwable, (MatrixFactorizationModel, Double)] = Either.catchNonFatal {

    //Turn the records into Ratings.
    val ratings = records.map(_.toRating)

    //Split Ratings RDD into a training and eval set with a ratio of 70% / 30% respectively
    val Array(tr, ev) = ratings.randomSplit(Array(0.7, 0.3))
    //Then persist both
    val training = tr.persist()
    val eval = ev.persist()

    //Train ALS using the config params
    //TODO: Evaluate training model over space (using k-cover) and return best one.
    val model = ALS.train(training, conf.rank, conf.iter, conf.lambda)


    //Evaluate ALS
    //Drop the ratings to leave you with just the user product pairs
    val evProductPairs = ev.map(r => (r.user, r.product))
    //Pass this pair RDD to the model to generate predictions
    val evPredictions = model.predict(evProductPairs)
    //Split out ratings from predictions from ratings to give an RDD[((Int, Int), Double)] (A pair RDD with (Int, Int) key)
    val splitFn = (r: Rating) => (r.user, r.product) -> r.rating
    val evPredictedRatings = evPredictions.map(splitFn)
    //Perform the same split on the original ratings from ev, and then join the two RDD[((Int, Int), Double)]. This merges
    // on key and produces an RDD[((Int, Int), (Double, Double))] so that we can compare predicted and actual ratings
    val evActiualRatings = ev.map(splitFn)
    //Extract just the predicted and actual ratings as an RDD
    val evJoinedRatings = evPredictedRatings.join(evActiualRatings).map { case ((_, _), (predicted, actual)) =>
      (predicted, actual)
    }

    val evaluation = new RegressionMetrics(evJoinedRatings)

    (model, evaluation.rootMeanSquaredError)
  }

  /** Write out the model, parameters and RMSE score */
  private def writeOut(spark: SparkContext,
                       model: MatrixFactorizationModel,
                       rmse: Double,
                       numRecs: Long,
                       conf: Config): Either[Throwable, Unit] = Either.catchNonFatal {
    //Scala monadic version of try with resources
    //TODO: check path is valid directory not file. (needed?)
    for {
      writer <- managed(Files.newBufferedWriter(Paths.get(s"${conf.outputPath}/model-summary.txt"),
        StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND))
    } {
      //Write out summary stats
      writer.write(s"The number of records provided: $numRecs")
      writer.newLine()
      writer.write(s"The rank parameter used: ${conf.rank}")
      writer.newLine()
      writer.write(s"The numIterations parameter used: ${conf.iter}")
      writer.newLine()
      writer.write(s"The alpha parameter used: ${conf.alpha}")
      writer.newLine()
      writer.write(s"Model RMSE score: $rmse")
    }

    //Wite out the model
    model.save(spark, s"${conf.outputPath}/model")
  }

}
