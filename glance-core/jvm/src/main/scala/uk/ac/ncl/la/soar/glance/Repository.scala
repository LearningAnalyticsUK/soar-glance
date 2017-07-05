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
import fs2._
import fs2.interop.cats._
import scala.concurrent.ExecutionContext.Implicits.global
import com.typesafe.config.ConfigFactory.parseString
import pureconfig.loadConfigOrThrow
import uk.ac.ncl.la.soar.data.{ModuleScore, StudentRecords}
import uk.ac.ncl.la.soar.{ModuleCode, StudentNumber}

/**
  * Repository trait which defines the behaviour of a Repository, retrieving objects from a database
  *
  * Note: The repository is sealed at  the moment for clarity, but should be unsealed and move to a separate file if
  * this pattern ends up being needed in other places throughout our codebase (which is likely ... its db access).
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
  lazy val Survey: Task[SurveyDb] = createSchema.map(_._2)
  lazy val Response: Task[SurveyResponseDb] = createSchema.map(_._3)

  /** Init method to set up the database */
  private val createSchema: Task[(StudentDb, SurveyDb, SurveyResponseDb)] = {

    //TODO: Work out if this is even vaguely sane?
    //Lazy config for memoization?
    lazy val config = loadConfigOrThrow[Config]

    for {
      cfg <- Task.delay(config)
      xa = DriverManagerTransactor[Task](
        "org.postgresql.Driver",
        s"jdbc:postgresql:${cfg.database.name}",
        cfg.database.user,
        cfg.database.password
      )
      stDb = new StudentDb(xa)
      sDb = new SurveyDb(xa)
      rDb = new SurveyResponseDb(xa)
      - <- { println("Initialising Student tables");stDb.init }
      _ <- { println("Initialising Survey tables");sDb.init }
      _ <- { println("Initialising Response tables");rDb.init }
    } yield (stDb, sDb, rDb)
  }
}

