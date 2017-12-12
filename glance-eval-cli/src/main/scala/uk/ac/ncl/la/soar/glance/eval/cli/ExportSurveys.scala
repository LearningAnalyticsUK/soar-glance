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
import java.nio.file.{Files, Path, Paths}
import java.util.UUID

import cats._
import cats.implicits._
import kantan.csv._
import kantan.csv.ops._
import kantan.csv.generic._
import monix.eval.Task
import uk.ac.ncl.la.soar.ModuleCode
import uk.ac.ncl.la.soar.data.StudentRecords
import uk.ac.ncl.la.soar.glance.eval.Survey
import uk.ac.ncl.la.soar.glance.eval.db.Repositories

import scala.collection.immutable.SortedMap

/**
  * Job which exports surveys from the database as csvs
  */
object ExportSurveys extends Command[ExportSurveysConfig, Unit] {

  override def run(conf: ExportSurveysConfig): Task[Unit] = {
    for {
      db <- Repositories.Survey
      survey <- db.find(UUID.fromString(conf.surveyId))
      outDir <- findOrCreateDirectory(conf.outputPath)
      _ <- ???
    } yield ()
  }

  private def writeModuleScores(survey: Survey, outputPath: Path) = Task {

    val out = outputPath.resolve("module_scores.csv")

    val allModules = survey.modules.toList.sortWith(_ < _)

    val headers = "Student Number" :: allModules

    out.writeCsv(survey.entries.map(getRecordsRow(_, allModules)),
                 rfc.withHeader(headers: _*))
  }

  private def findOrCreateDirectory(outputPath: String) = Task {
    val out = Paths.get(outputPath)

    if (Files.exists(out))
      out
    else
      Files.createDirectories(out)
  }

  private def getRecordsRow(s: StudentRecords[SortedMap, ModuleCode, Double],
                            allModules: List[ModuleCode]) = {

    val num = s.number

    val scores = allModules.map(s.record.get(_).fold(" ")(_.toString))

    num :: scores
  }
}
