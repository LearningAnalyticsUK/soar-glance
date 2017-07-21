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

import cats._
import cats.implicits._
import doobie.imports._
import monix.eval.Task
import monix.cats._
import monix.execution.Scheduler.Implicits.global
import pureconfig.loadConfigOrThrow
import org.flywaydb.core.Flyway
import Implicits._

/**
  * Repository trait which defines the behaviour of a Repository, retrieving objects from a database
  *
  * TODO: Add teardown
  */
abstract class Repository[A] {

  type PK
  type F[B] = Task[B]
  def M: Monad[F] = Monad[Task]

  val init: F[Unit]
  val list: F[List[A]]
  def find(id: PK): F[Option[A]]
  def save(entry: A): F[Unit]
  def delete(id: PK): F[Boolean]
}

/**
  * Trait which defines raw query methods on the companion objects of [[Repository]].
  * TODO: Decide on the scoping of these methods - public for now but could be `private[glance]`
  */
trait RepositoryCompanion[A, R <: Repository[A]] {
  val initQ: ConnectionIO[Unit]
  val listQ: ConnectionIO[List[A]]
  def findQ(id: R#PK): ConnectionIO[Option[A]]
  def saveQ(entry: A): ConnectionIO[Unit]
  def deleteQ(id: R#PK): ConnectionIO[Boolean]
}

object Repository {

  //Is this the best/safest way to expose the Repository classes? Not really....
  lazy val Student: Task[StudentDb] = createSchema.map(_._1)
  lazy val Module: Task[ModuleDb] = createSchema.map(_._2)
  lazy val Survey: Task[SurveyDb] = createSchema.map(_._3)
  lazy val Response: Task[SurveyResponseDb] = createSchema.map(_._4)

  /** Method to perform db migrations */
  private def migrate(dbUrl: String, user: String, pass: String) = Task {
    val flyway = new Flyway()
    flyway.setDataSource(dbUrl, user, pass)
    flyway.migrate()
  }

  /** Init method to set up the database */
  private val createSchema: Task[(StudentDb, ModuleDb, SurveyDb, SurveyResponseDb)] = {

    //TODO: Work out if this is even vaguely sane?
    //Lazy config for memoization?
    lazy val config = loadConfigOrThrow[Config]

    for {
      cfg <- Task(config)
//      _ <- migrate(
//        s"jdbc:postgresql:${cfg.database.name}",
//        cfg.database.user,
//        cfg.database.password
//      )
      xa = DriverManagerTransactor[Task](
        "org.postgresql.Driver",
        s"jdbc:postgresql:${cfg.database.name}",
        cfg.database.user,
        cfg.database.password
      )
      stDb = new StudentDb(xa)
      mDb = new ModuleDb(xa)
      sDb = new SurveyDb(xa)
      rDb = new SurveyResponseDb(xa)
      - <- { println("Initialising Student tables");stDb.init }
      _ <- { println("Initialising Module tables");mDb.init }
      _ <- { println("Initialising Survey tables");sDb.init }
      _ <- { println("Initialising Response tables");rDb.init }
    } yield (stDb, mDb, sDb, rDb)
  }
}

