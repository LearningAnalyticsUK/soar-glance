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
package uk.ac.ncl.la.soar.glance.cli

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths, StandardOpenOption}

import scala.io.Source
import scopt._
import cats._
import cats.implicits._
import fs2.Task
import fs2.interop.cats._
import resource._
import uk.ac.ncl.la.soar.{ModuleCode, Record}
import uk.ac.ncl.la.soar.data.{ModuleScore, StudentRecords}
import uk.ac.ncl.la.soar.Record._
import uk.ac.ncl.la.soar.glance.{Repository, Survey}

import scala.collection.immutable.SortedMap
import scala.util.{Properties, Random}

/**
  * Job which generates csv based "surveys" which present student module scores in a table and elides certain results
  * so that they may be filled in (predicted) later by domain experts (module leaders).
  */
object Generator extends Command[GeneratorConfig, Unit] {


  override def run(conf: GeneratorConfig): Task[Unit] = {
    for {
      scores <- parseScores(conf.recordsPath)
      surveys <- Task.now(Survey.generate(scores, conf.elided, conf.modules, conf.common, conf.seed))
      db <- Repository.Survey
      _ <- { println("Finished creating tables.");surveys.traverse(db.save) }
    } yield ()
  }

  /** Retrieve and parse all ModuleScores from provided file if possible */
  private def parseScores[F[_]](recordsPath: String): Task[List[ModuleScore]] = Task.delay {

    //Read in ModuleScore CSV
    val lines = Source.fromFile(recordsPath).getLines()
    //In order to groupBy the current naive implementation requires sufficient memory to hold all ModuleScores
    ModuleScore.parse(lines, ',').toList
  }

//  private def writeOut(modules: List[ModuleCode],
//                       chunks: Map[ModuleCode, List[StudentRecords[SortedMap, ModuleCode, Double]]],
//                       conf: GeneratorConfig): Either[Throwable, Unit] = Either.catchNonFatal {
//
//    val header = s"Student Number, ${modules.mkString(", ")}"
//    val metaFn = List((s: StudentRecords[SortedMap, ModuleCode, Double]) => s.number)
//    val csvs = chunks.mapValues { surveyLines =>
//      surveyLines.map(_.toCSV(metaFn, modules)).mkString(Properties.lineSeparator)
//    }
//
//    val out = Paths.get(conf.outputPath)
//
//    val outPath = if(Files.exists(out)) out else Files.createDirectories(out)
//
//    //Prepare folder structure
//    val subPaths = chunks.map { case (k,_) => k -> Files.createDirectory(Paths.get(s"$outPath/$k")) }
//    //Foreach subpath, write out survey
//
//    def write(module: ModuleCode, entries: Map[ModuleCode, String], path: Path) = {
//      //Scala monadic version of try with resources
//      for {
//        writer <- managed(Files.newBufferedWriter(path.resolve("survey.csv"), StandardCharsets.UTF_8, StandardOpenOption.CREATE,
//          StandardOpenOption.APPEND))
//      } {
//        writer.write(header)
//        writer.newLine()
//        writer.write(entries.getOrElse(module, ""))
//      }
//    }
//
//    subPaths.foreach { case (k,v) => write(k, csvs, v) }
//  }
}


