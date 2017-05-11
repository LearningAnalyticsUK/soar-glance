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
import cats.implicits._
import doobie.imports._
import fs2.Task
import uk.ac.ncl.la.soar.data.{ModuleScore, StudentRecords}
import uk.ac.ncl.la.soar.{ModuleCode, StudentNumber}

/**
  * Repository trait for retrieving objects from the [[Survey]] ADT from a database
  */
trait Repository[A] {
  val init: Task[Unit]
//  val tearDown: Task[Unit]
  val list: Task[List[A]]
  def find(id: UUID): Task[Option[A]]
  def save(entry: A): Task[Unit]
  def delete(id: UUID): Task[Boolean]
}

//TODO: Define a RepositoryCompanion which is package private or private[Repository] and which defines query methods

trait RepositoryCompanion[A] {
  protected val initQ: ConnectionIO[Unit]
  protected val listQ: ConnectionIO[List[A]]
  protected def findQ(id: UUID): ConnectionIO[Option[A]]
  protected def saveQ(entry: A): ConnectionIO[Unit]
  protected def deleteQ(id: UUID): ConnectionIO[Boolean]
}

object Repository {

  lazy val Survey: Task[SurveyDb] = createSchema

  /** Init method to set up the database */
  private val createSchema: Task[SurveyDb] = {
    //TODO: Work out conf for db connection details
    val xa = DriverManagerTransactor[Task]("org.postgresql.Driver", "jdbc:postgresql:surveys", "postgres",
      "mysecretpassword")

    val sDb = new SurveyDb(xa)
    for {
      _ <- sDb.init
    } yield sDb
  }
}

class SurveyDb private[glance] (xa: Transactor[Task]) extends Repository[Survey] {

  import SurveyDb._

  implicit val uuidMeta: Meta[UUID] = Meta[String].nxmap(UUID.fromString, _.toString)

  //TODO: Investigate Sink as a means to neaten this but also get to grips with this fs2/stream stuff
  override val init: Task[Unit] = initQ.map(_ => ()).transact(xa)

  //TODO: Consider the performance implications of list over stream/process.
  // Not a problem here but could be for higher volume data.
  override val list: Task[List[Survey]] = listQ.transact(xa)

  override def find(id: UUID): Task[Option[Survey]] = findQ(id).transact(xa)

  override def save(entry: Survey): Task[Unit] = saveQ(entry).transact(xa)

  override def delete(id: UUID): Task[Boolean] = deleteQ(id).transact(xa)

}

object SurveyDb extends RepositoryCompanion[Survey] {
  override protected val initQ: ConnectionIO[Unit] = {
    sql"""
      CREATE TABLE IF NOT EXISTS surveys (
        id VARCHAR PRIMARY KEY
      );

      CREATE TABLE IF NOT EXISTS students (
        num VARCHAR(10) PRIMARY KEY
      );

      CREATE TABLE IF NOT EXISTS surveys_students (
        id VARCHAR PRIMARY KEY,
        survey_id VARCHAR REFERENCES surveys(id) ON DELETE CASCADE,
        student_num VARCHAR REFERENCES students(num) ON DELETE CASCADE
      );

      CREATE TABLE IF NOT EXISTS survey_queries (
        id VARCHAR PRIMARY KEY,
        survey_id VARCHAR REFERENCES surveys(id) ON DELETE CASCADE,
        student_number VARCHAR REFERENCES students(num) ON DELETE CASCADE,
        module_num VARCHAR(80) NOT NULL
      );

      CREATE TABLE IF NOT EXISTS module_score (
        id VARCHAR PRIMARY KEY,
        student_num VARCHAR REFERENCES students(num) ON DELETE CASCADE,
        score DECIMAL(5,2) NOT NULL,
        CHECK (score > 0.00),
        CHECK (score < 100.00),
        module_num VARCHAR(80) NOT NULL
      );
    """.update.run
  }

  override protected val listQ: ConnectionIO[List[Survey]] = {
    for {
      ids <- listSurveyIdsQ
      surveyOpts <- ids.traverse(findSurveyWithIdQ)
    } yield surveyOpts.flatten
  }

  override protected def findQ(id: UUID): ConnectionIO[Option[Survey]] = {
    //TODO: work out how to take advantage of the first query for early bail out
    for {
      ident <- findSurveyIdQ(id)
      scores <- listScoresForSurveyQ(id)
      qs <- listQueriesForSurveyQ(id)
    } yield {
      val moduleSet = scores.iterator.map(_.module).toSet
      //In memory group by potentially fine, but fs2 Stream has own groupBy operator. TODO: Check fs2.Stream.groupBy
      val records = Survey.groupByStudents(scores)
      ident.map(_ => Survey(moduleSet, qs, records))
    }
  }

  //Look at returning a ConnectionIO of Int, or Option[EmptySurvey]
  //TODO: This seems like a lot of imperative brittle boilerplate. Am I really doing this right?
  override protected def saveQ(entry: Survey): ConnectionIO[Unit] = {
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
    val addStudentSQL =
      """
        INSERT INTO students (num) KEY (num) VALUES (?) ON CONFLICT (num) DO NOTHING;
      """
    val studRows = records.map(_.number)
    val addStudentsQ = Update[(StudentNumber)](addStudentSQL).updateMany(studRows)

    //Next, add survey entries
    val addEntrySQL =
      """
        INSERT INTO surveys_students (id, survey_id, student_num) KEY (id)
        VALUES (?, ?, ?) ON CONFLICT (survey_id, student_num) DO NOTHING;
      """
    val entryRows = records.map(r => (UUID.randomUUID, sId, r.number))
    val addEntryQ = Update[(UUID, UUID, StudentNumber)](addEntrySQL).updateMany(entryRows)

    val moduleScoreSQL =
      """
        INSERT INTO module_score (id, student_num, score, module_num) KEY (id)
        VALUES (?, ?, ?, ?) ON CONFLICT (id) DO NOTHING;
      """
    val mSRows = mS.map({ case ModuleScore(stud, module, score) => (UUID.randomUUID, stud, score, module) })
    val addModuleScoreQ = Update[(UUID, StudentNumber, Double, ModuleCode)](moduleScoreSQL).updateMany(mSRows)

    //Finally add the queries
    val querySQL =
      """
        INSERT INTO survey_queries (id, survey_id, student_num, module_num) KEY (id)
        VALUES (?, ?, ?, ?) ON CONFLICT (id) DO NOTHING;
      """
    val qRows = queries.iterator.map({ case (stud, module) => (UUID.randomUUID, sId, stud, module) }).toList
    val addQueryQ = Update[(UUID, UUID, StudentNumber, ModuleCode)](querySQL).updateMany(qRows)
    //Return combined query
    for{
      _ <- addSurveyQ *> addStudentsQ
      _ <- addEntryQ *> addModuleScoreQ *> addQueryQ
    } yield ()
  }

  override protected def deleteQ(id: UUID): ConnectionIO[Boolean] =
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

  override val init: Task[Unit] = createTableQuery.map(_ => ()).transact(xa)
  override val list: Task[List[SurveyResponse]] = _

  override def find(id: UUID): Task[Option[SurveyResponse]] = ???

  override def save(entry: SurveyResponse): Task[Unit] = ???

  override def delete(id: UUID): Task[Boolean] = deleteQ(id).transact(xa)

  /** Queries */
  private lazy val createTableQuery: ConnectionIO[Int] = {
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
        query_id VARCHAR REFERENCES survey_queries(id) ON DELETE CASCADE
      );
    """.update.run
  }

  //findRespondentIdQ
  private def findRespondentIdQ(id: UUID): ConnectionIO[Option[UUID]] =
    sql"SELECT ssrs.id FROM surveys_respondents ssrs WHERE ssrs.id = $id".query[UUID].option


  private def findRespondentWithId(id: UUID): ConnectionIO[Option[SurveyResponse]] =  {
    type RespondentRow = (UUID, UUID, String, Instant)
    val selectRespondents = sql"SELECT * FROM surveys_respondents r WHERE r.id = $id".query[RespondentRow].option

    val action = for {
      respondentOpt <- selectRespondents
      surveyOpt <- ???

    }
  }


  //findRespondentWithId

  private def deleteQ(id: UUID): ConnectionIO[Boolean] =
    sql"DELETE FROM surveys_respondents WHERE id = $id;".update.run.map(_ > 0)
}


