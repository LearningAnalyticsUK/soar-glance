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
package uk.ac.ncl.la.soar.glance.eval.cli

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths, StandardOpenOption}

import scala.io.Source
import scopt._
import cats._
import cats.implicits._
import monix.eval.Task
import monix.cats._
import resource._
import uk.ac.ncl.la.soar.{ModuleCode, Record}
import uk.ac.ncl.la.soar.Record._
import uk.ac.ncl.la.soar.data.{ModuleScore, StudentRecords}
import uk.ac.ncl.la.soar.server.Implicits._
import uk.ac.ncl.la.soar.glance.eval.Survey
import uk.ac.ncl.la.soar.glance.eval.db.Repositories

import scala.collection.immutable.SortedMap
import scala.util.{Properties, Random}


/**
  * Job which generates csv based "surveys" which present student module scores in a table and elides certain results
  * so that they may be filled in (predicted) later by domain experts (module leaders).
  */
object GenerateSurveys extends Command[GenerateConfig, Unit] {


  override def run(conf: GenerateConfig): Task[Unit] = {
    for {
      scores <- parseScores(conf.recordsPath)
      surveys <- Task.now(Survey.generate(scores, conf.numStudents, conf.modules, conf.seed))
      db <- Repositories.Survey
      _ <- { println("Finished creating tables."); surveys.traverse(db.save) }
    } yield ()
  }

  /** Retrieve and parse all ModuleScores from provided file if possible */
  private def parseScores(recordsPath: String): Task[List[ModuleScore]] = Task.delay {

    //Read in ModuleScore CSV
    val lines = Source.fromFile(recordsPath).getLines()
    //In order to groupBy the current naive implementation requires sufficient memory to hold all ModuleScores
    val lineList = lines.toList
    println(s"There are ${lineList.size} lines in $recordsPath")
    ModuleScore.parse(lineList.iterator, ',').toList
  }

}


