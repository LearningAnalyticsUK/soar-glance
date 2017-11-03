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
package uk.ac.ncl.la.soar.glance.eval.db

import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

import cats.data._
import cats.implicits._
import doobie.imports._
import monix.eval.Task
import uk.ac.ncl.la.soar.data.ModuleScore
import uk.ac.ncl.la.soar.db.{RepositoryCompanion, Repository => DbRepository}
import uk.ac.ncl.la.soar.glance.eval.{CompleteResponse, Ranking, Survey, SurveyResponse}
import uk.ac.ncl.la.soar.glance.util.{Time, Times}
import uk.ac.ncl.la.soar.glance.web.client.component.sortable.IndexChange
import uk.ac.ncl.la.soar.{ModuleCode, StudentNumber}

import scala.collection.mutable.ListBuffer


class SurveyDb private[glance] (xa: Transactor[Task]) extends DbRepository[Survey] {

  import SurveyDb._

  type PK = UUID

  //TODO: Investigate Sink as a means to neaten this but also get to grips with this fs2/stream stuff
  override val init: Task[Unit] = initQ.transact(xa)

  //TODO: Consider the performance implications of list over stream/process.
  // Not a problem here but could be for higher volume data.
  override val list: Task[List[Survey]] = listQ.transact(xa)

  override def find(id: UUID): Task[Option[Survey]] = findQ(id).transact(xa)

  def findDateRange(id: UUID): Task[Option[(Instant, Instant)]] = findDateRangeQ(id).transact(xa)

  override def save(entry: Survey): Task[Unit] = saveQ(entry).transact(xa)

  override def delete(id: UUID): Task[Boolean] = deleteQ(id).transact(xa)
}

object SurveyDb extends RepositoryCompanion[Survey, SurveyDb] {

  type SurveyRow = (UUID, ModuleCode)
  
  implicit val uuidMeta: Meta[UUID] = Meta[String].nxmap(UUID.fromString, _.toString)

  override val initQ: ConnectionIO[Unit] = ().pure[ConnectionIO]

  override val listQ: ConnectionIO[List[Survey]] = {
    //TODO: The below cannot be the best way to do this? Look into some better SQL foo. Also for SurveyResponseDb listQ
    for {
      ids <- listSurveyIdsQ
      surveyOpts <- ids.traverse(findQ)
    } yield surveyOpts.flatten
  }

  override def findQ(id: UUID): ConnectionIO[Option[Survey]] = {
    //TODO: work out how to take advantage of the first query for early bail out
    
    for {
      sr <- findSurveyRowQ(id)
      scores <- listScoresForSurveyQ(id)
      qs <- listQueriesForSurveyQ(id)
    } yield {
      val moduleSet = scores.iterator.map(_.module).toSet
      //In memory group by potentially fine, but fs2 Stream has own groupBy operator. TODO: Check fs2.Stream.groupBy
      val records = Survey.groupByStudents(scores)
      sr.map { case (id, rankModule) => Survey(moduleSet, rankModule, qs, records, id) }
    }
  }

  //Look at returning a ConnectionIO of Int, or Option[EmptySurvey]
  //TODO: This seems like a lot of imperative brittle boilerplate. Am I really doing this right?
  override def saveQ(entry: Survey): ConnectionIO[Unit] = {
    //First, break out the pieces of the Survey which correspond to tables
    val Survey(_, rankModule, queries, records, sId) = entry
    //First add a survey id. As this is a unary type which is generated there really isn't a concept of collision
    val addSurveyQ = sql"INSERT INTO survey (id, module_num) VALUES ($sId, $rankModule);".update.run
    //Once we have added the survey id, add students and surveys_students

    //Then, break down the records into a series of modulescores and add those.
    val mS = for {
      stud <- records
      (module, score) <- stud.record.iterator
      m <- ModuleScore(stud.number, module, score)
    } yield m

    //Next, add students
    //TODO: Should this be extracted to the StudentDb companion?
    val addStudentSQL =
      """
        INSERT INTO student (num) VALUES (?) ON CONFLICT (num) DO NOTHING;
      """
    val studRows = records.map(_.number)
    val addStudentsQ = Update[(StudentNumber)](addStudentSQL).updateMany(studRows)

    //Next, add module entries
    val addModuleSQL =
      """
        INSERT INTO module (num) VALUES (?) ON CONFLICT (num) DO NOTHING;
      """
    val modRows = entry.modules
    val addModulesQ = Update[ModuleCode](addModuleSQL).updateMany(modRows)

    //Next, add survey entries
    val addEntrySQL =
      """
        INSERT INTO survey_student (survey_id, student_num)
        VALUES (?, ?) ON CONFLICT (survey_id, student_num) DO NOTHING;
      """
    val entryRows = records.map(r => (sId, r.number))
    val addEntryQ = Update[(UUID, StudentNumber)](addEntrySQL).updateMany(entryRows)

    val moduleScoreSQL =
      """
        INSERT INTO module_score (id, student_num, score, module_num)
        VALUES (?, ?, ?, ?) ON CONFLICT (id) DO NOTHING;
      """
    val mSRows = mS.map({ case ModuleScore(stud, module, score) => (UUID.randomUUID, stud, score, module) })
    val addModuleScoreQ = Update[(UUID, StudentNumber, Double, ModuleCode)](moduleScoreSQL).updateMany(mSRows)

    val addSurveyQuerySQL =
      """
        INSERT INTO survey_query (survey_id, student_num)
        VALUES (?, ?) ON CONFLICT (survey_id, student_num) DO NOTHING;
      """

    val sQRows = queries.iterator.map(stud => (sId, stud)).toList

    val addSQueryQ = Update[(UUID, StudentNumber)](addSurveyQuerySQL).updateMany(sQRows)

    //Return combined query
    //TODO: Work out why we're actually using Cartesian ops here rather than normal monadic sequencing?
    for{
      _ <- addModulesQ *> addSurveyQ *> addStudentsQ
      _ <- addEntryQ *> addModuleScoreQ *> addSQueryQ
    } yield ()
  }

  override def deleteQ(id: UUID): ConnectionIO[Boolean] =
    sql"DELETE FROM survey WHERE id = $id;".update.run.map(_ > 0)

  def findDateRangeQ(id: UUID): ConnectionIO[Option[(Instant, Instant)]] = {
    val q = sql"""
      SELECT m.start_date, EXTRACT(epoch FROM m.length)
      FROM survey s JOIN module m
      ON m.num = s.module_num
      WHERE s.id = $id;
    """.query[(Instant, Long)].option

    q.map(_.map({ case (start, length) => (start, start.plusSeconds(length)) }))
  }

  private lazy val listSurveyIdsQ: ConnectionIO[List[UUID]] = sql"SELECT s.id FROM survey s;".query[UUID].list

  private def findSurveyRowQ(id: UUID): ConnectionIO[Option[SurveyRow]] =
    sql"""
      SELECT s.id, s.module_num FROM survey s WHERE s.id = $id;
    """.query[SurveyRow].option

  private def listScoresForSurveyQ(id: UUID): ConnectionIO[List[ModuleScore]] = {
    val query = sql"""
        SELECT ss.student_num, ms.module_num, ms.score
        FROM survey_student ss, module_score ms
        WHERE ss.survey_id = $id
        AND ms.student_num = ss.student_num;
      """.query[(StudentNumber, ModuleCode, Double)]

    query.process
      .map({ case (sn, mc, sc) => ModuleScore(sn, mc, sc) })
      .collect({ case Some(ms) => ms })
      .list
  }

  private def listQueriesForSurveyQ(id: UUID): ConnectionIO[List[StudentNumber]] =
    sql"""
      SELECT sq.student_num
      FROM survey_query sq
      WHERE sq.survey_id = $id;
    """.query[StudentNumber].list
}

class SurveyResponseDb private[glance] (xa: Transactor[Task]) extends DbRepository[SurveyResponse] {

  import SurveyResponseDb._

  override type PK = UUID

  override val init: Task[Unit] = initQ.transact(xa)
  override val list: Task[List[SurveyResponse]] = listQ.transact(xa)

  override def find(id: UUID): Task[Option[SurveyResponse]] = findQ(id).transact(xa)

  def listForSurvey(id: UUID): Task[List[SurveyResponse]] = listForSurveyQ(id).transact(xa)

  override def save(entry: SurveyResponse): Task[Unit] = saveQ(entry).transact(xa)

  override def delete(id: UUID): Task[Boolean] = deleteQ(id).transact(xa)

}

object SurveyResponseDb extends RepositoryCompanion[SurveyResponse, SurveyResponseDb] {

  /* Struct types and type aliases to represent Db rows */
  case class ResponseRow(id: UUID,
                         respondentEmail: String,
                         surveyId: UUID,
                         started: Timestamp,
                         finished: Timestamp,
                         dRankingId: UUID,
                         sRankingId: UUID)

  type RankRow = (StudentNumber, Int)
  //TODO: Update this alias
  type RankChangeRow = (StudentNumber, IndexChange, Timestamp)

  implicit val uuidMeta: Meta[UUID] = Meta[String].nxmap(UUID.fromString, _.toString)
  implicit val timeMeta: Meta[Time] = Meta[Double].xmap(Times.fromDouble , _.millis)

  override val initQ: ConnectionIO[Unit] = ().pure[ConnectionIO]

  private val listRespondentIdsQ: ConnectionIO[List[UUID]] =
    sql"SELECT r.id FROM survey_response r;".query[UUID].list

  override val listQ: ConnectionIO[List[SurveyResponse]] = {
    val rowsQ: ConnectionIO[List[ResponseRow]] =
      sql"SELECT * FROM survey_response;".query[ResponseRow].list

    for {
      responseRows <- rowsQ
      completeResponse <- responseRows.traverse(responseFromRow).map(_.flatten)
    } yield completeResponse
  }

  private def listForSurveyQ(id: UUID): ConnectionIO[List[SurveyResponse]] = {
    val rowsQ: ConnectionIO[List[ResponseRow]] = 
      sql"SELECT * FROM survey_response WHERE survey_id = $id;".query[ResponseRow].list

    for {
      responseRows <- rowsQ
      completeResponse <- responseRows.traverse(responseFromRow).map(_.flatten)
    } yield completeResponse
  }

  private def rankRowsQ(id: UUID): ConnectionIO[List[RankRow]] = {
    sql"""
      SELECT rnk.student_num, rnk.rank
      FROM student_rank rnk
      WHERE rnk.ranking_id = $id;
    """.query[RankRow].list
  }

  private def rankChangeRowsQ(id: UUID): ConnectionIO[List[RankChangeRow]] = {
    sql"""
      SELECT rnkC.student_num, rnkC.start_rank, rnkC.end_rank, rnkC.time
      FROM rank_change rnkC WHERE rnkC.ranking_id = $id;
    """.query[RankChangeRow].list
  }

  //TODO: Fix horrible TS to Time conversions by basing the Time Meta on timestamp
  private def rankingQ(id: UUID): ConnectionIO[Ranking] = (id, id).bitraverse(rankRowsQ, rankChangeRowsQ).map {
    case (ranks, rankChanges) =>
      Ranking(ranks.sortBy(_._2).map(_._1), rankChanges.map { case (s, i, ts) => (s, i, Times.fromLong(ts.getTime)) })
  }

  private def responseFromRow(row: ResponseRow): ConnectionIO[Option[SurveyResponse]] = {

    //Get the survey
    //If it exists, get the ranks and rank changes
    //Build the complete response
    //Note that the return type annotation is required because ConnectionIO is not Covariant
    val responseT: OptionT[ConnectionIO, SurveyResponse] = for {
      survey <- OptionT(SurveyDb.findQ(row.surveyId))
      sR <- OptionT.liftF(rankingQ(row.sRankingId))
      dR <- OptionT.liftF(rankingQ(row.dRankingId))
    } yield {
      CompleteResponse(survey, sR, dR, row.respondentEmail, row.started.getTime.toDouble,
        row.finished.getTime.toDouble, row.id)
    }
    responseT.value
  }


  override def findQ(id: UUID): ConnectionIO[Option[SurveyResponse]] = {
    val rowQ = sql"SELECT rsp FROM survey_response rsp WHERE rsp.id = $id;".query[ResponseRow].option

    val responseT = for {
      row <- OptionT(rowQ)
      completeResponse <- OptionT(responseFromRow(row))
    } yield completeResponse

    responseT.value
  }

  //TODO: Return persisted Response
  override def saveQ(entry: SurveyResponse): ConnectionIO[Unit] = {


    //Cast the start time from Double to Timestamp
    val startTs = new Timestamp(entry.start.toLong)

    //Create UUIDs for detailed and simple rankings
    val sRankingId = UUID.randomUUID()
    val dRankingId = UUID.randomUUID()

    //Insert entry in respondents table
    val addResponseQ =
      sql"""
         INSERT INTO survey_response (id, respondent_email, survey_id, time_started, time_finished, detailed_ranking, simple_ranking)
         VALUES (${entry.id}, ${entry.respondent}, ${entry.survey.id}, $startTs, CURRENT_TIMESTAMP, $dRankingId, $sRankingId);
      """.update.run

    //Insert entries into ranking table;
    val addRankingsQ =
      sql"""
         INSERT INTO ranking (id, response_id, detailed)
         VALUES ($sRankingId, ${entry.id},  FALSE), ($dRankingId, ${entry.id}, TRUE);
      """.update.run

    //Actually construct the combined query program
    for {
      _ <- addResponseQ
      _ <- addRankingsQ
      _ <- saveRankingQ(entry.simple, sRankingId) *> saveRankingQ(entry.detailed, dRankingId)
    } yield ()
  }

  private def saveRankingQ(ranking: Ranking, id: UUID) = {

    val ranks = ListBuffer.empty[(String, UUID, Int)]
    for ( (e, idx) <- ranking.ranks.zipWithIndex ) {
      ranks += ((e, id, idx))
    }

    val rankHistory = ListBuffer.empty[(StudentNumber, Int, Int, Timestamp, UUID)]
    for( r <- ranking.rankHistory ) {
      val (student, change, time) = r
      val ts = new Timestamp(time.millis.toLong)
      rankHistory += ((student, change.oldIndex, change.newIndex, ts, id))
    }

    //Then batch insert entries in student_ranks table
    val addRanksSQL =
      """
        INSERT INTO student_rank (student_num, ranking_id, rank)
        VALUES (?, ?, ?);
      """

    //Finally batch insert entries in rank_change table
    val addChangesSQL =
      """
        INSERT INTO rank_change (student_num, start_rank, end_rank, time, ranking_id)
        VALUES (?, ?, ?, ?, ?);
      """


    val addRanksQ = Update[(StudentNumber, UUID, Int)](addRanksSQL).updateMany(ranks.result())
    val addRankChangesQ = Update[(StudentNumber, Int, Int, Timestamp, UUID)](addChangesSQL).updateMany(rankHistory.result())
    addRanksQ *> addRankChangesQ
  }

  override def deleteQ(id: UUID): ConnectionIO[Boolean] =
    sql"DELETE FROM survey_response WHERE id = $id;".update.run.map(_ > 0)

}


