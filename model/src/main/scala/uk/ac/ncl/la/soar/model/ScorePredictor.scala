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
package uk.ac.ncl.la.soar.model

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths, StandardOpenOption}

import cats.implicits._
import org.apache.log4j.{Level, LogManager}
import org.apache.spark.mllib.evaluation.RegressionMetrics
import org.apache.spark.mllib.recommendation.{ALS, MatrixFactorizationModel, Rating}
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import resource._
import uk.ac.ncl.la.soar.{ModuleScore}

/** Simple singleton entry point to Spark job
  *
  * Refactor if we turn it into a long running web service
  *
  * @author hugofirth
  */
object ScorePredictor {


  def main(args: Array[String]): Unit = {
    //Set up the logger
    val log = LogManager.getRootLogger
    log.setLevel(Level.WARN)

    //Create a config object from the command line arguments provided
    val parseCli = Config.parser.parse(args, Config()).fold {
      Left(new IllegalArgumentException("Failed to correctly parse command line arguments!")): Either[Throwable, Config]
    } { Right(_) }

    //Compose stages of job
    //TODO: Lift ops into effect capturing type like Task[_], rather than just Either[NonFatal, _]
    (for {
      conf <- parseCli.right
      spark <- boot(conf).right
      rs <- readRatings(spark, conf).right
      rSplit <- split(rs).right
      Array(tr, ev) = rSplit //If Either supported withFilter we could do this in one step ...
      model <- produceModel(spark, tr, conf).right
      rmse <- evaluateModel(spark, ev, model, conf).right
      _ <- writeOut(spark, model, rmse, rs.count(), conf).right
    } yield ()) match {
      case Left(e) =>
        //In the event of an error, log and crash out.
        log.error(e.toString)
        sys.exit(1)
      case Right(_) =>
        //In the event of a successful job, log and finish
        log.info("Job finished.")
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
  private def readRatings(spark: SparkContext, conf: Config): Either[Throwable, RDD[Rating]] =
    Either.catchNonFatal {

      //Import the records
      val recordLines = spark.textFile(conf.recordsPath)

      //Parse the records and drop errors
      val records = recordLines.flatMap(line => ModuleScore.parseLine(line, ','))

      //Turn the records into Ratings.
      records.map(_.toRating)
    }

  /** Split a ratings RDD into training and evaluation data */
  private def split(rs: RDD[Rating]): Either[Throwable, Array[RDD[Rating]]] =
    Either.catchNonFatal(rs.randomSplit(Array(0.7, 0.3)))

  /** Produce the predictive model */
  private def produceModel(spark: SparkContext,
                           tr: RDD[Rating],
                           conf: Config): Either[Throwable, MatrixFactorizationModel] = Either.catchNonFatal {

    //Persist training rdd
    val training = tr.persist()

    //Train ALS using the config params
    val model = ALS.train(training, conf.rank, conf.iter, conf.lambda)
    //Unpersist the training RDD
    training.unpersist()
    //Return the model
    model
  }

  /** Evaluate a predictive model */
  private def evaluateModel(spark: SparkContext,
                            ev: RDD[Rating],
                            model: MatrixFactorizationModel,
                            conf: Config): Either[Throwable, Double] = Either.catchNonFatal {

    //TODO: Evaluate training model over space (using k-cover cross validation) and return best one.
    //Persist the eval rdd
    val eval = ev.persist()

    //Evaluate ALS
    //Drop the ratings to leave you with just the user product pairs
    val evProductPairs = eval.map(r => (r.user, r.product))
    //Pass this pair RDD to the model to generate predictions
    val evPredictions = model.predict(evProductPairs)
    //Split out ratings from predictions from ratings to give an RDD[((Int, Int), Double)] (A pair RDD with (Int, Int) key)
    val splitFn = (r: Rating) => (r.user, r.product) -> r.rating
    val evPredictedRatings = evPredictions.map(splitFn)
    //Perform the same split on the original ratings from ev, and then join the two RDD[((Int, Int), Double)]. This merges
    // on key and produces an RDD[((Int, Int), (Double, Double))] so that we can compare predicted and actual ratings
    val evActualRatings = eval.map(splitFn)
    //Extract just the predicted and actual ratings as an RDD
    val evJoinedRatings = evPredictedRatings.join(evActualRatings).map { case ((_, _), (predicted, actual)) =>
      (predicted, actual)
    }

    //Get RMSE
    val evaluation = new RegressionMetrics(evJoinedRatings).rootMeanSquaredError
    //Unpersist eval rdd
    eval.unpersist()
    //Return RMSE
    evaluation
  }

  /** Write out the model, parameters and RMSE score */
  private def writeOut(spark: SparkContext,
                       model: MatrixFactorizationModel,
                       rmse: Double,
                       numRecs: Long,
                       conf: Config): Either[Throwable, Unit] = Either.catchNonFatal {

    val out = Paths.get(conf.outputPath)

    val outPath = if(Files.exists(out)) out else Files.createDirectories(out)
    //Scala monadic version of try with resources
    for {
      writer <- managed(Files.newBufferedWriter(Paths.get(s"${outPath.toAbsolutePath}/model-summary.txt"),
        StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND))
    } {
      //Write out summary stats
      val summary =
        s"""
           |The number of records provided: $numRecs
           |The rank parameter used: ${conf.rank}
           |The numIterations parameter used: ${conf.iter}
           |The alpha parameter used: ${conf.alpha}
           |The lambda parameter used: ${conf.lambda}
           |The Model's RMSE score: $rmse
         """.stripMargin
      writer.write(summary)
    }

    //Wite out the model
    model.save(spark, s"${outPath.toAbsolutePath}/model")
  }

}
