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
import java.nio.file._
import java.time.Period
import java.util.UUID

import cats._
import cats.implicits._
import cats.data.OptionT
import kantan.csv._
import kantan.csv.ops._
import kantan.csv.generic._
import monix.eval.Task
import monix.cats._
import uk.ac.ncl.la.soar.{ModuleCode, StudentNumber}
import uk.ac.ncl.la.soar.data.StudentRecords
import uk.ac.ncl.la.soar.glance.eval.{
  ClusterSession,
  RecapSession,
  Session,
  Survey
}
import uk.ac.ncl.la.soar.glance.eval.db._
import uk.ac.ncl.la.soar.glance.util.Time

import scala.collection.immutable.SortedMap

/**
  * Job which exports surveys from the database as csvs
  */
object ExportSurveys extends Command[ExportSurveysConfig, Unit] {

  override def run(conf: ExportSurveysConfig): Task[Unit] = {
    for {
      sDb <- Repositories.Survey
      rDb <- Repositories.RecapSession
      cDb <- Repositories.ClusterSession
      survey <- sDb.find(UUID.fromString(conf.surveyId))
      outDir <- findOrCreateDirectory(conf.outputPath)
      _ <- survey.traverse { s =>
        Task.zip4(
          writeModuleScores(s, outDir),
          writeRecapUsage(s, outDir, rDb, sDb),
          writeClusterUsage(s, outDir, cDb, sDb),
          writeSurveyInfo(s, outDir)
        )
      }
    } yield ()
  }

  private def writeModuleScores(survey: Survey, outputPath: Path) = Task {

    val out = outputPath.resolve("module_scores.csv")

    val allModules = survey.modules.toList.sortWith(_ < _)

    val headers = "Student Number" :: allModules

    val queries = survey.queries.toSet
    val entries = survey.entries.filter(s => queries.contains(s.number))

    out.writeCsv(entries.map(getRecordsRow(_, allModules)),
                 rfc.withHeader(headers: _*))
  }

  private def writeRecapUsage(survey: Survey,
                              outputPath: Path,
                              rDb: RecapSessionDb,
                              sDb: SurveyDb) = {

    val getOut = Task.now(outputPath.resolve("recap_usage.csv"))

    val getSummaries = for {
      surveyDates <- sDb.findDateRange(survey.id)
      sessions <- surveyDates match {
        case Some((start, end)) => rDb.findBetween(start, end)
        case None               => Task.now(List.empty[RecapSession])
      }
    } yield {
      surveyDates.map {
        case (s, e) => Session.getSummary(sessions, s, e, Period.ofDays(7))
      }
    }

    (for {
      out <- OptionT.liftF(getOut)
      summary <- OptionT(getSummaries)
    } yield {
      val headers = "Student Number" :: getSessionWeeks(summary.meanDuration)

      val queries = survey.queries.toSet
      val entries = summary.studentDuration.filterKeys(queries.contains)

      out.writeCsv(entries.map(getSessionsRow), rfc.withHeader(headers: _*))
    }).getOrElse(())
  }

  //TODO: Not very DRY I know - *Db classes aren't the easiest to be polymorphic over. Revisit
  private def writeClusterUsage(survey: Survey,
                                outputPath: Path,
                                cDb: ClusterSessionDb,
                                sDb: SurveyDb) = {

    val getOut = Task.now(outputPath.resolve("cluster_usage.csv"))

    val getSummaries = for {
      surveyDates <- sDb.findDateRange(survey.id)
      sessions <- surveyDates match {
        case Some((start, end)) => cDb.findBetween(start, end)
        case None               => Task.now(List.empty[ClusterSession])
      }
    } yield {
      surveyDates.map {
        case (s, e) => Session.getSummary(sessions, s, e, Period.ofDays(7))
      }
    }

    (for {
      out <- OptionT.liftF(getOut)
      summary <- OptionT(getSummaries)
    } yield {
      val headers = "Student Number" :: getSessionWeeks(summary.meanDuration)

      val queries = survey.queries.toSet
      val entries = summary.studentDuration.filterKeys(queries.contains)

      out.writeCsv(entries.map(getSessionsRow), rfc.withHeader(headers: _*))
    }).getOrElse(())
  }

  //TODO: Make sure underlying Map implementation preserves ordering. Perhaps by using a SortedMap
  //  otherwise this feels a but fishy.
  private def getSessionsRow(
      entry: (StudentNumber, Map[(Time, Time), Double])) =
    entry._1 :: entry._2.values.map(_.toString).toList

  private def getSessionWeeks(durations: Map[(Time, Time), Double]) =
    durations.zipWithIndex.map({ case (_, i) => s"Week ${i + 1}" }).toList

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

  private def writeSurveyInfo(survey: Survey, outputPath: Path) = Task {

    val out = outputPath.resolve("survey_info.txt")

    val meta =
      s"""Survey Id: ${survey.id}
         |Survey module: ${survey.moduleToRank}
         |Survey students: ${survey.visualisations.mkString(",")}
       """.stripMargin

    Files.write(out,
                meta.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE_NEW)
  }
}
