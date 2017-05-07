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

import fs2.{Task, Stream}
import uk.ac.ncl.la.soar.{ModuleCode, StudentNumber}
import uk.ac.ncl.la.soar.data.ModuleScore

/**
  * Repository trait for retrieving objects from the [[Survey]] ADT from a database
  * TODO: Move to a JVM folder
  */
sealed trait Repository[+A] {
  val init: Task[Unit]
  val list: Task[List[A]]
  def find(id: UUID): Task[Option[A]]
  def save[B >: A](entry: B): Task[A]
  def delete(id: UUID): Task[Boolean]
  def sync[B >: A](entries: List[B]): Task[List[A]]
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
  private lazy val createTableQuery: ConnectionIO[Int] =
    sql"""
      CREATE TABLE IF NOT EXISTS surveys (
        id VARCHAR PRIMARY KEY
      );

      CREATE TABLE IF NOT EXISTS students (
        number VARCHAR(10) PRIMARY KEY
      );

      CREATE TABLE IF NOT EXISTS surveys_students (
        id VARCHAR PRIMARY KEY,
        survey_id VARCHAR REFERENCES surveys(id),
        student_number VARCHAR REFERENCES students(number)
      );

      CREATE TABLE IF NOT EXISTS survey_queries (
        id VARCHAR PRIMARY KEY,
        survey_id VARCHAR REFERENCES surveys(id),
        student_number VARCHAR REFERENCES students(number),
        module VARCHAR(80) NOT NULL
      );

      CREATE TABLE IF NOT EXISTS module_score (
        id VARCHAR PRIMARY KEY,
        student_number VARCHAR REFERENCES students(number) ON DELETE CASCADE,
        score DECIMAL(5,2) NOT NULL,
        CHECK (score > 0.00),
        CHECK (score < 100.00),
        module VARCHAR(80) NOT NULL
      );
    """.update.run

  //TODO: Investigate Sink as a means to neaten this but also get to grips with this fs2/stream stuff
  override val init: Task[Unit] = createTableQuery.map(_ => ()).transact(xa)

  override val list: Task[List[EmptySurvey]] = Task.now(List.empty[EmptySurvey])

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

  override def save[B >: EmptySurvey](entry: B): Task[EmptySurvey] = ???

  override def delete(id: UUID): Task[Boolean] = ???

  override def sync[B >: EmptySurvey](entries: List[B]): Task[List[EmptySurvey]] = ???

  //Why lazy
  private def findSurveyWithId(id: UUID): ConnectionIO[Option[UUID]] =
    sql"""
      SELECT s.id FROM surveys s WHERE s.id = $id
    """.query[UUID].option
  
  private def listScoresForSurvey(id: UUID): ConnectionIO[List[ModuleScore]] = {
    val query = sql"""
        SELECT ss.student_number, ms.module, ms.score
        FROM surveys_students ss, module_score ms
        WHERE ss.survey_id = $id
        AND ms.student_number = ss.student_number
      """.query[(StudentNumber, ModuleCode, Double)]

    query.process.map({ case (sn, mc, sc) => ModuleScore(sn, mc, sc) }).collect({ case Some(ms) => ms }).list
  }

  //TODO: Figure out better way to accumulate to a Map, this requires two passes, could duplicated toMap of stdLib and
  // PR Doobie?
  private def listQueriesForSurvey(id: UUID): ConnectionIO[Map[StudentNumber, ModuleCode]] =
    sql"""
      SELECT sq.student_number, sq.module
      FROM survey_queries sq
      WHERE sq.survey_id = $id
    """.query[(StudentNumber, ModuleCode)].list.map(_.toMap)

  private def saveQ(survey: EmptySurvey): ConnectionIO[EmptySurvey] = {
    //First, break out the pieces of the Survey which correspond to tables
    val EmptySurvey(_, queries, records) = survey
    //First add a survey id. As this is a unary type which is program generated there really isn't a concept of collision
    // here, perhaps not the best idea, but there we are.
    //Once we have added the survey id, then add break down the records into a series of modulescores and add those.
    //Finally add the queries.
  }

}


