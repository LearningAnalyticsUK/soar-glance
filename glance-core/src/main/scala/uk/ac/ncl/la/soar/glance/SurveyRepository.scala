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

import doobie.imports._
import java.util.UUID

import fs2.{Stream, Task}, fs2.interop.cats._
import cats._, cats.implicits._
import uk.ac.ncl.la.soar.{ModuleCode, StudentNumber}, uk.ac.ncl.la.soar.data.ModuleScore

/**
  * Repository trait for retrieving objects from the [[Survey]] ADT from a database
  * TODO: Move to a JVM folder
  */
sealed trait Repository[A] {
  val init: Task[Unit]
//  val list: Task[List[A]]
  def find(id: UUID): Task[Option[A]]
  def save(entry: A): Task[Unit]
  def delete(id: UUID): Task[Boolean]
}

object Repository {

  lazy val Survey: Task[SurveyDb] = createSchema

  /** Init method to set up the database */
  private val createSchema: Task[SurveyDb] = {
    val xa = DriverManagerTransactor[Task]("org.postgresql.Driver", "jdbc:postgresql:surveys", "postgres",
      "mysecretpassword")

    val sDb = new SurveyDb(xa)
    for {
      _ <- sDb.init
    } yield sDb
  }
}

class SurveyDb private[glance] (xa: Transactor[Task]) extends Repository[EmptySurvey] {

  implicit val uuidMeta: Meta[UUID] = Meta[String].nxmap(UUID.fromString, _.toString)

  //Survey: pk varchar id, string module, string common - nope, unary type for now.
  //SurveyEntry: pk varchar id, fk varchar SurveyId, fk varchar StudentNumber
  //SurveyResponse: pk varchar id, varchar respondent, ? date, int type
  //Query: pk varchar id, fk varchar SurveyId, fk varchar StudentNumber, varchar ModuleCode
  //QueryResponse: pk varchar id, fk varchar SurveyResponseId, fk varchar QueryId, int Score not null
  //Student: pk varchar number
  //ModuleScore: pk varchar id, fk varchar StudentNumber, int score not null, varchar ModuleCode
  private lazy val createTableQuery: ConnectionIO[Int] = {
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

  //TODO: Investigate Sink as a means to neaten this but also get to grips with this fs2/stream stuff
  override val init: Task[Unit] = createTableQuery.map(_ => ()).transact(xa)

//  override val list: Task[List[EmptySurvey]] = Task.now(List.empty[EmptySurvey])

  override def find(id: UUID): Task[Option[EmptySurvey]] = {
    //TODO: work out how to take advantage of the first query for early bail out
    val action = for {
      ident <- findSurveyWithId(id)
      scores <- listScoresForSurvey(id)
      qs <- listQueriesForSurvey(id)
    } yield {
      val moduleSet = scores.iterator.map(_.module).toSet
      //In memory group by potentially fine, but fs2 Stream has own groupBy operator. TODO: Check fs2.Stream.groupBy
      val records = Survey.groupByStudents(scores)
      ident.map(_ => EmptySurvey(moduleSet, qs, records))
    }
    action.transact(xa)
  }

  def findSurveyScores(id: UUID): Task[List[ModuleScore]] = listScoresForSurvey(id).transact(xa)

  def findSurveyQueries(id: UUID): Task[Map[StudentNumber, ModuleCode]] = listQueriesForSurvey(id).transact(xa)

  override def save(entry: EmptySurvey): Task[Unit] = saveQ(entry).transact(xa)

  override def delete(id: UUID): Task[Boolean] = ???

  private def findSurveyWithId(id: UUID): ConnectionIO[Option[UUID]] =
    sql"""
      SELECT s.id FROM surveys s WHERE s.id = $id;
    """.query[UUID].option
  
  private def listScoresForSurvey(id: UUID): ConnectionIO[List[ModuleScore]] = {
    val query = sql"""
        SELECT ss.student_num, ms.module_num, ms.score
        FROM surveys_students ss, module_score ms
        WHERE ss.survey_id = $id
        AND ms.student_num = ss.student_num;
      """.query[(StudentNumber, ModuleCode, Double)]

    query.process.map({ case (sn, mc, sc) => ModuleScore(sn, mc, sc) }).collect({ case Some(ms) => ms }).list
  }

  //TODO: Figure out better way to accumulate to a Map, this requires two passes, could duplicate toMap of stdLib and
  // PR Doobie?
  private def listQueriesForSurvey(id: UUID): ConnectionIO[Map[StudentNumber, ModuleCode]] =
    sql"""
      SELECT sq.student_num, sq.module_num
      FROM survey_queries sq
      WHERE sq.survey_id = $id;
    """.query[(StudentNumber, ModuleCode)].list.map(_.toMap)

  //Look at returning a ConnectionIO of Int, or Option[EmptySurvey]
  //TODO: Clean up this monstrosity
  private def saveQ(survey: EmptySurvey): ConnectionIO[Unit] = {
    //First, break out the pieces of the Survey which correspond to tables
    val EmptySurvey(_, queries, records, sId) = survey
    //First add a survey id. As this is a unary type which is generated there really isn't a concept of collision
    val addSurveyQ = sql"INSERT INTO surveys (id) VALUES ($sId) ON CONFLICT (id) DO NOTHING;".update.run
    //Once we have added the survey id, add students and surveys_students

    //Then, break down the records into a series of modulescores and add those.
    val mS = for {
      stud <- records
      (module, score) <- stud.record.iterator
      m <- ModuleScore(stud.number, module, score)
    } yield m

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
      _ <- addSurveyQ
      _ <- addModuleScoreQ
      _ <- addQueryQ
    } yield ()
  }

}


