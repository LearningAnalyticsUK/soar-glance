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
package uk.ac.ncl.la.soar.glance

import java.util.UUID
import java.time.{Instant, LocalDateTime}

import cats._
import cats.data.OptionT
import cats.implicits._
import doobie.imports._
import monix.eval.Task
import monix.cats._
import scala.concurrent.ExecutionContext.Implicits.global
import com.typesafe.config.ConfigFactory.parseString
import pureconfig.loadConfigOrThrow
import uk.ac.ncl.la.soar.data.{ModuleScore, StudentRecords}
import uk.ac.ncl.la.soar.{ModuleCode, StudentNumber}
import Implicits._

class SurveyDb private[glance] (xa: Transactor[Task]) extends Repository[Survey] {

  /**
    * core
    * server
    * model
    * glance
    * glance-eval
    * glance-eval-cli
    */
  import SurveyDb._

  type PK = UUID

  //TODO: Investigate Sink as a means to neaten this but also get to grips with this fs2/stream stuff
  override val init: Task[Unit] = initQ.transact(xa)

  //TODO: Consider the performance implications of list over stream/process.
  // Not a problem here but could be for higher volume data.
  override val list: Task[List[Survey]] = listQ.transact(xa)

  override def find(id: UUID): Task[Option[Survey]] = findQ(id).transact(xa)

  override def save(entry: Survey): Task[Unit] = saveQ(entry).transact(xa)

  override def delete(id: UUID): Task[Boolean] = deleteQ(id).transact(xa)
}

object SurveyDb extends RepositoryCompanion[Survey, SurveyDb] {

  implicit val uuidMeta: Meta[UUID] = Meta[String].nxmap(UUID.fromString, _.toString)

  override val initQ: ConnectionIO[Unit] = {
    sql"""
      CREATE TABLE IF NOT EXISTS surveys (
        id VARCHAR(40) PRIMARY KEY
      );

      CREATE TABLE IF NOT EXISTS surveys_students (
        survey_id VARCHAR(40) REFERENCES surveys(id) ON DELETE CASCADE,
        student_num VARCHAR(10) REFERENCES students(num) ON DELETE CASCADE,
        PRIMARY KEY (survey_id, student_num)
      );

      CREATE TABLE IF NOT EXISTS queries (
        student_num VARCHAR(10) REFERENCES students(num) ON DELETE CASCADE,
        module_num VARCHAR(8),
        PRIMARY KEY (student_num, module_num)
      );

      CREATE TABLE IF NOT EXISTS survey_queries (
        survey_id VARCHAR(40) REFERENCES surveys(id) ON DELETE CASCADE,
        student_num VARCHAR(10) NOT NULL,
        module_num VARCHAR(8) NOT NULL,
        PRIMARY KEY (survey_id, student_num, module_num),
        FOREIGN KEY (student_num, module_num) REFERENCES queries(student_num, module_num) ON DELETE CASCADE
      );

      CREATE TABLE IF NOT EXISTS module_score (
        id VARCHAR(40) PRIMARY KEY,
        student_num VARCHAR(10) REFERENCES students(num) ON DELETE CASCADE,
        score DECIMAL(5,2) NOT NULL,
        CHECK (score >= 0.00),
        CHECK (score <= 100.00),
        module_num VARCHAR(8) NOT NULL
      );
    """.update.run.void
  }

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
      ident <- findSurveyIdQ(id)
      scores <- listScoresForSurveyQ(id)
      qs <- listQueriesForSurveyQ(id)
    } yield {
      val moduleSet = scores.iterator.map(_.module).toSet
      //In memory group by potentially fine, but fs2 Stream has own groupBy operator. TODO: Check fs2.Stream.groupBy
      val records = Survey.groupByStudents(scores)
      ident.map(id => Survey(moduleSet, qs, records, id))
    }
  }

  //Look at returning a ConnectionIO of Int, or Option[EmptySurvey]
  //TODO: This seems like a lot of imperative brittle boilerplate. Am I really doing this right?
  override def saveQ(entry: Survey): ConnectionIO[Unit] = {
    //First, break out the pieces of the Survey which correspond to tables
    val Survey(_, queries, records, sId) = entry
    //First add a survey id. As this is a unary type which is generated there really isn't a concept of collision
    val addSurveyQ = sql"INSERT INTO surveys (id) VALUES ($sId);".update.run
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
        INSERT INTO students (num) VALUES (?) ON CONFLICT (num) DO NOTHING;
      """
    val studRows = records.map(_.number)
    val addStudentsQ = Update[(StudentNumber)](addStudentSQL).updateMany(studRows)

    //Next, add survey entries
    val addEntrySQL =
      """
        INSERT INTO surveys_students (survey_id, student_num)
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

    //Finally add the queries
    val addQuerySQL = "INSERT INTO queries (student_num, module_num) VALUES (?, ?) ON CONFLICT DO NOTHING;"

    val addSurveyQuerySQL =
      """
        INSERT INTO survey_queries (survey_id, student_num, module_num)
        VALUES (?, ?, ?) ON CONFLICT (survey_id, student_num, module_num) DO NOTHING;
      """

    val qRows = queries.toList
    val sQRows = queries.iterator.map({ case (stud, module) => (sId, stud, module) }).toList

    val addQueryQ = Update[(StudentNumber, ModuleCode)](addQuerySQL).updateMany(qRows)
    val addSQueryQ = Update[(UUID, StudentNumber, ModuleCode)](addSurveyQuerySQL).updateMany(sQRows)

    //Return combined query
    for{
      _ <- addSurveyQ *> addStudentsQ
      _ <- addEntryQ *> addModuleScoreQ *> addQueryQ *> addSQueryQ
    } yield ()
  }

  override def deleteQ(id: UUID): ConnectionIO[Boolean] =
    sql"DELETE FROM surveys WHERE id = $id;".update.run.map(_ > 0)

  private lazy val listSurveyIdsQ: ConnectionIO[List[UUID]] = sql"SELECT s.id FROM surveys s;".query[UUID].list

  private def findSurveyIdQ(id: UUID): ConnectionIO[Option[UUID]] =
    sql"""
      SELECT s.id FROM surveys s WHERE s.id = $id;
    """.query[UUID].option

  private def listScoresForSurveyQ(id: UUID): ConnectionIO[List[ModuleScore]] = {
    val query = sql"""
        SELECT ss.student_num, ms.module_num, ms.score
        FROM surveys_students ss, module_score ms
        WHERE ss.survey_id = $id
        AND ms.student_num = ss.student_num;
      """.query[(StudentNumber, ModuleCode, Double)]

    query.process
      .map({ case (sn, mc, sc) => ModuleScore(sn, mc, sc) })
      .collect({ case Some(ms) => ms })
      .list
  }

  private def listQueriesForSurveyQ(id: UUID): ConnectionIO[Map[StudentNumber, ModuleCode]] =
    sql"""
      SELECT sq.student_num, sq.module_num
      FROM survey_queries sq
      WHERE sq.survey_id = $id;
    """.query[(StudentNumber, ModuleCode)].list.map(_.toMap)
}

class SurveyResponseDb private[glance] (xa: Transactor[Task]) extends Repository[SurveyResponse] {

  import SurveyResponseDb._

  override type PK = UUID

  override val init: Task[Unit] = initQ.transact(xa)
  override val list: Task[List[SurveyResponse]] = listQ.transact(xa)

  override def find(id: UUID): Task[Option[SurveyResponse]] = findQ(id).transact(xa)

  override def save(entry: SurveyResponse): Task[Unit] = saveQ(entry).transact(xa)

  override def delete(id: UUID): Task[Boolean] = deleteQ(id).transact(xa)

}

object SurveyResponseDb extends RepositoryCompanion[SurveyResponse, SurveyResponseDb] {

  /** Type aliases for Db rows */
  type RespondentRow = (UUID, UUID, String, Instant)
  type ResponseRow = (UUID, UUID, StudentNumber, ModuleCode, Double)

  implicit val uuidMeta: Meta[UUID] = Meta[String].nxmap(UUID.fromString, _.toString)

  override val initQ: ConnectionIO[Unit] = {
    sql"""
      CREATE TABLE IF NOT EXISTS surveys_respondents (
        id VARCHAR PRIMARY KEY,
        survey_id VARCHAR REFERENCES surveys(id) ON DELETE CASCADE,
        respondent VARCHAR UNIQUE NOT NULL,
        submitted TIMESTAMP WITH TIME ZONE NOT NULL
      );

      CREATE TABLE IF NOT EXISTS survey_response (
        id VARCHAR PRIMARY KEY,
        respondent_id VARCHAR REFERENCES surveys_respondents(id) ON DELETE CASCADE,
        student_num VARCHAR(10) NOT NULL,
        module_num VARCHAR NOT NULL,
        predicted_score DECIMAL(5,2) NOT NULL,
        CHECK (predicted_score > 0.00),
        CHECK (predicted_score < 100.00),
        FOREIGN KEY (student_num, module_num) REFERENCES queries(student_num, module_num) ON DELETE CASCADE
      );
    """.update.run.void
  }

  private val listRespondentIdsQ: ConnectionIO[List[UUID]] =
    sql"SELECT r.id FROM surveys_respondents r;".query[UUID].list

  override val listQ: ConnectionIO[List[SurveyResponse]] = {
    for {
      ids <- listRespondentIdsQ
      surveyOpts <- ids.traverse(findQ)
    } yield surveyOpts.flatten
  }

  override def findQ(id: UUID): ConnectionIO[Option[SurveyResponse]] = {

    val selectRespondents = sql"SELECT * FROM surveys_respondents r WHERE r.id = $id;".query[RespondentRow].option

    def selectResponses(id: UUID) =
      sql"""
      SELECT (rs.student_num, rs.module_num, rs.predicted_score)
      FROM survey_response rs
      WHERE rs.respondent_id = $id
      """.query[(StudentNumber, ModuleCode, Double)].list

    //Assign query to action as we need to use OptionT transformer which we'll need to unwrap at the end.
    val action = for {
      respondent <- OptionT(selectRespondents)
      survey <- OptionT(SurveyDb.findQ(respondent._2))
      responses <- OptionT.liftF(selectResponses(respondent._1))
    } yield {
      //Turn the response rows into a responses map as expected for the SurveyResponse constructor
      val responsesMap = responses.iterator.flatMap({ case (stud,mod,sc) =>
        ModuleScore(stud,mod,sc)
      }).map({ case m @ ModuleScore(stud,_, _) =>
        stud -> m
      }).toMap

      SurveyResponse(survey, responsesMap, respondent._3, id)
    }
    action.value
  }

  override def saveQ(entry: SurveyResponse): ConnectionIO[Unit] = {

    //Get the retrieve survey Id from nested survey in entry
    val sId = entry.id

    //Get responses from entry
    val responses = entry.responses

    //Insert entry in respondents table
    val addRespondentQ =
      sql"""
         INSERT INTO surveys_respondents (id, survey_id, respondent, submitted)
         VALUES (${entry.id}, ${entry.survey.id}, ${entry.respondent}, CURRENT_TIMESTAMP);
      """.update.run

    //Then batch insert entries in responses table
    val addResponseSQL =
      """
        INSERT INTO survey_responses (id, respondent_id, student_num, module_num, predicted_score)
        VALUES (?, ?, ?, ?, ?);
      """
    val responseRows = entry.responses.iterator.map({  case (student, ModuleScore(_, module, score)) =>
      (UUID.randomUUID, entry.id, student, module, score)
    }).toList
    val addResponsesQ = Update[ResponseRow](addResponseSQL).updateMany(responseRows)

    //Actually construct the combined query program
    for {
      _ <- addRespondentQ
      _ <- addResponsesQ
    } yield ()
  }

  override def deleteQ(id: UUID): ConnectionIO[Boolean] =
    sql"DELETE FROM surveys_respondents WHERE id = $id;".update.run.map(_ > 0)

  private def findRespondentIdQ(id: UUID): ConnectionIO[Option[UUID]] =
    sql"SELECT ssrs.id FROM surveys_respondents ssrs WHERE ssrs.id = $id;".query[UUID].option

}


