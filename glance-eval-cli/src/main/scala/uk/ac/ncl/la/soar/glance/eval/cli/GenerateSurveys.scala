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
import java.util.UUID

import scala.io.Source
import scopt._
import cats._
import cats.data.NonEmptyVector
import cats.implicits._
import monix.eval.Task
import monix.cats._
import resource._
import uk.ac.ncl.la.soar.{ModuleCode, Record}
import uk.ac.ncl.la.soar.Record._
import uk.ac.ncl.la.soar.data.{ModuleScore, StudentRecords}
import uk.ac.ncl.la.soar.server.Implicits._
import uk.ac.ncl.la.soar.glance.eval.{Collection, Survey, VisualisationType}
import uk.ac.ncl.la.soar.glance.eval.db.Repositories

import scala.collection.immutable.SortedMap
import scala.util.{Properties, Random}

/**
  * Job which generates csv based "surveys" which present student module scores in a table and elides certain results
  * so that they may be filled in (predicted) later by domain experts (module leaders).
  *
  */
object GenerateSurveys extends Command[GenerateConfig, Unit] {

  override def run(conf: GenerateConfig): Task[Unit] = {

    //Retrieve the visualisation objects specified in the command line options, discarding any that are unrecognised
    val viz = conf.visualisations.flatMap(VisualisationType.factory).toList
    val eol = s"${sys.props("line.separator")}      "

    for {
      scores <- parseScores(conf.recordsPath)
      surveys <- Task.now(
        Survey.generate(scores,
                        conf.numStudents,
                        conf.modules,
                        viz,
                        conf.collection,
                        conf.seed))
      surveyDb <- Repositories.Survey
      _ <- surveys.traverse(surveyDb.save)
      collectionDb <- Repositories.Collection
      collections = conf.collection.fold(List.empty[Collection]) {
        buildCollections(_, conf.modules.toSet, surveys)
      }
      _ <- collections.traverse(collectionDb.save)
    } yield {

      if (conf.collection.nonEmpty)
        println(
          s"[INFO] - Survey collections generated with ids: $eol${collections.map(_.id).mkString(eol)}")

      println(
        s"[INFO] - Surveys generated with the following ids: $eol${surveys.map(_.id).mkString(eol)}")
    }
  }

  private def buildCollections(num: Int,
                               modules: Set[ModuleCode],
                               surveys: List[Survey]) = {

//    val sByM = surveys.groupBy(_.moduleToRank).filter {
//      case (k, v) => modules.contains(k)
//    }
//
//    sByM.flatMap { case (m, s) =>
//
//      val id = UUID.randomUUID()
//      val surveyIds = s.take(num).map(_.id)
//
//      surveyIds match {
//        case hd :: tl => Some(Collection(id, m, NonEmptyVector(hd, tl.toVector)))
//        case Nil => None
//      }
//    }

    (for {
      (m, s) <- surveys.groupBy(_.moduleToRank) if modules.contains(m)
      id = UUID.randomUUID()
      surveyIds = s.take(num).map(_.id)
      c <- surveyIds match {
        case hd :: tl =>
          Some(Collection(id, m, NonEmptyVector(hd, tl.toVector)))
        case Nil => None
      }
    } yield c).toList
  }

  /** Retrieve and parse all ModuleScores from provided file if possible */
  private def parseScores(recordsPath: String): Task[List[ModuleScore]] =
    Task.delay {

      //Read in ModuleScore CSV
      val lines = Source.fromFile(recordsPath).getLines()
      //In order to groupBy the current naive implementation requires sufficient memory to hold all ModuleScores
      val lineList = lines.toList
      println(s"There are ${lineList.size} lines in $recordsPath")
      ModuleScore.parse(lineList.iterator, ',').toList
    }

}
