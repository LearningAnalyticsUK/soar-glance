/** soar
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
package uk.ac.ncl.la.soar.eval

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths, StandardOpenOption}

import scala.io.Source
import scala.collection.mutable
import scopt._
import cats._
import cats.implicits._
import org.apache.log4j.{Level, LogManager}
import resource._
import uk.ac.ncl.la.soar.{ModuleCode, ModuleScore, StudentRecord}

import scala.collection.immutable.SortedMap
import scala.util.{Properties, Random}


/** Entry point to the Eval script
  *
  * @author hugofirth
  */
object Main {

  def main(args: Array[String]): Unit = {
    //Set up the logger
//    val log = LogManager.getRootLogger
//    log.setLevel(Level.WARN)

    //Create a config object from the command line arguments provided
    //TODO: Add string apply to Config object to pick correct parser and parse based on head argument
    //Either that or look into scopt commands
    val parseCli = Config.generatorParser.parse(args, GeneratorConfig()).fold {
      Left(new IllegalArgumentException("Failed to correctly parse command line arguments!")): Either[Throwable, GeneratorConfig]
    } { Right(_) }

    //TODO: Write out meta data file saying which modules have been dropped and which seed was used to generate.

    (for {
      conf <- parseCli
      scores <- parseScores(conf.recordsPath)
      records <- Either.right(groupByStudents(scores))
      surveys <- sample(records, conf)
      _ <- writeOut(getAllModules(scores), surveys, conf)
    } yield ()) match {
      case Left(e) =>
        //In the event of an error, log and crash out.
        System.err.println(e.toString)
        sys.exit(1)
      case Right(_) =>
        //In the event of a successful job, log and finish
        println("Job finished.")
    }
  }

  /** Retrieve and parse all ModuleScores from provided file if possible */
  private def parseScores(recordsPath: String): Either[Throwable, List[ModuleScore]] = Either.catchNonFatal {

    //Read in ModuleScore CSV
    val lines = Source.fromFile(recordsPath).getLines()
    //In order to groupBy the current naive implementation requires sufficient memory to hold all ModuleScores
    ModuleScore.parse(lines, ',').toList
  }

  /** Group module scores by studnet numbers and construct StudentRecords */
  private def groupByStudents(scores: List[ModuleScore]): List[StudentRecord[String]] = {

    //Group by studentNumber and construct records
    val fullRecords = scores.groupBy(_.student).map { case (stud, studScores) =>
      val full = SortedMap.newBuilder[ModuleCode, Double] ++= studScores.iterator.map(s => s.module -> s.score)
      StudentRecord(stud, full.result)
    }

    //TODO: replace magic number filter to drop students with few records with a conf option
    fullRecords.filter(_.moduleRecords.size > 10).toList
  }

  /** Get the list of distinct ModuleCodes, sorted alphanumerically (therefore chronologically) */
  private def getAllModules(scores: List[ModuleScore]): List[ModuleCode] = scores.map(_.module).sortWith(_ < _).distinct

  /** Randomly sample the student records, selecting conf.elided students *per* module, and removing both the score for
    * that module and the score for any module which follows it in the order (where alphanum ~ chronological). */
  private def sample(records: List[StudentRecord[String]],
                     conf: GeneratorConfig): Either[Throwable, Map[ModuleCode, List[StudentRecord[ModuleCode]]]] =
    Either.catchNonFatal {
      //Create the rng with provided seed
      val rand = new Random(conf.seed)
      //Shuffle the records list using Random
      val shuffled = rand.shuffle(records)
      //First take the "training data" which is a fixed n student records, where n = elided * 2
      val (t, s) = shuffled.splitAt(conf.elided*2) match {
        case (_, Nil) => throw new IllegalArgumentException("The number of students for which you have records must be " +
          s"greater than the formula (elided * #modules) + (elided * 2). You provided elided:${conf.elided}, #modules: " +
          s"${conf.modules.size} and students: ${shuffled.size}.")
        case a => a
      }

      //Then chunk s into segments the size of elided, then drop modules from each chunk to create survey pieces
      //TODO: Handle duplicates in the modules list
      val surveyChunks = conf.modules.iterator.zip(s.grouped(conf.elided)).map({ case (module, students) =>
        module -> students.map { s =>
          val truncated = s.moduleRecords.to(module).updated(module, -1.0)
          s.copy(moduleRecords = truncated)
        }
      }).toMap

      //If a common module has been specified, retrieve its chunk and remove it from surveyChunks
      val commonChunk = conf.common.flatMap(surveyChunks.get).getOrElse(List.empty[StudentRecord[String]])
      val chunksNoCommon = conf.common.fold(surveyChunks)(surveyChunks - _)

      //Combine training, common and a survey chunk to produce a survey of records, sorted by studentNumber.
      chunksNoCommon.mapValues(c => (t ::: commonChunk ::: c).sortWith(_.number < _.number))
  }

  private def writeOut(modules: List[ModuleCode],
                       chunks: Map[ModuleCode, List[StudentRecord[ModuleCode]]],
                       conf: GeneratorConfig): Either[Throwable, Unit] = Either.catchNonFatal {

    val header = s"Student Number, ${modules.mkString(", ")}"
    val csvs = chunks.mapValues { surveyLines =>
      surveyLines.map(_.toCsv(modules)).mkString(Properties.lineSeparator)
    }

    val out = Paths.get(conf.outputPath)

    val outPath = if(Files.exists(out)) out else Files.createDirectories(out)

    //Prepare folder structure
    val subPaths = chunks.map { case (k,_) => k -> Files.createDirectory(Paths.get(s"$outPath/$k")) }
    //Foreach subpath, write out survey

    def write(module: ModuleCode, entries: Map[ModuleCode, String], path: Path) = {
      //Scala monadic version of try with resources
      for {
        writer <- managed(Files.newBufferedWriter(path.resolve("survey.csv"), StandardCharsets.UTF_8, StandardOpenOption.CREATE,
          StandardOpenOption.APPEND))
      } {
        writer.write(header)
        writer.newLine()
        writer.write(entries.getOrElse(module, ""))
      }
    }

    subPaths.foreach { case (k,v) => write(k, csvs, v) }
  }

}
