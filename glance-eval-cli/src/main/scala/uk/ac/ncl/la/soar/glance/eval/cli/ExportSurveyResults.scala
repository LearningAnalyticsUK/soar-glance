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
import kantan.csv.cats._
import kantan.csv.generic._
import monix.eval.Task
import uk.ac.ncl.la.soar.StudentNumber
import uk.ac.ncl.la.soar.glance.eval.{Ranking, SurveyResponse}
import uk.ac.ncl.la.soar.glance.eval.server.Repositories



/**
  * Job which exports results from the survey tables
  */
object ExportSurveyResults extends Command[ExportSurveyResultsConfig, Unit] {

  sealed trait RankingType
  case object Simple extends RankingType
  case object Detailed extends RankingType
  case object True extends RankingType

  override def run(conf: ExportSurveyResultsConfig): Task[Unit] = {
    for {
      db <- Repositories.SurveyResponse
      responses <- db.listForSurvey(UUID.fromString(conf.surveyId))
      outDir <- findOrCreateDirectory(conf.outputPath)
      _ <- Task.zip3(
        writeResultsRows(responses, outDir, Simple, conf.surveyId),
        writeResultsRows(responses, outDir, Detailed, conf.surveyId),
        writeResultsRows(responses, outDir, True, conf.surveyId))
    } yield ()
  }


  private def writeResultsRows(responses: List[SurveyResponse],
                               outputPath: Path,
                               rankingType: RankingType,
                               surveyId: String) = Task {

    if(responses.nonEmpty) {

      val survey = responses.head.survey

      val headers = "respondent" :: survey.queries

      val out = rankingType match {
        case Simple => outputPath.resolve("simple.csv")
        case Detailed => outputPath.resolve("detailed.csv")
        case True => outputPath.resolve("true.csv")
      }

      out.writeCsv(responses.map(getResultsRow(_, rankingType)), rfc.withHeader(headers:_*))
    } else {
      println(s"There were no responses for the survey with id $surveyId")
    }

  }

  private def findOrCreateDirectory(outputPath: String) =  Task {
    val out = Paths.get(outputPath)

    if(Files.exists(out))
      out
    else
      Files.createDirectories(out)
  }

  private def getResultsRow(response: SurveyResponse, rankingType: RankingType) = {

    //Turn a ranking into a list of ranks sorted by the column order of query students
    def rankingToRow(r: Ranking, queryColumns: List[StudentNumber]) = {
      val ranks = r.ranks.zipWithIndex.toMap
      queryColumns.flatMap(ranks.get).map(_.toString)
    }

    //Get the list of query student numbers
    val queryColumns = response.survey.queries
    val module = response.survey.moduleToRank

    rankingType match {
      case Simple =>
        response.respondent :: rankingToRow(response.simple, queryColumns)
      case Detailed =>
        response.respondent :: rankingToRow(response.detailed, queryColumns)
      case True =>
        val qSet = queryColumns.toSet
        //Get the records for query students from the survey
        val qStudentRecords = response.survey.entries.iterator.filter(r => qSet.contains(r.number))
        //Drop all scores for query students except the scores in the module they were ranked for
        val trueScores = qStudentRecords.flatMap(r => r.record.get(module).map(r.number -> _))
        //Sort students by the score, then drop it and replace with a rank index
        val ranks = trueScores.toList.sortBy(_._2).map(_._1).zipWithIndex.toMap
        //Get the rank index for each column in the row, and add the respondent as the first cell
        response.respondent :: queryColumns.flatMap(ranks.get).map(_.toString)
    }

  }
}

