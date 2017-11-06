/** Default (Template) Project
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
package uk.ac.ncl.la.soar.db

import cats._
import cats.implicits._
import doobie.imports._
import monix.eval.Task
import monix.cats._
import org.flywaydb.core.Flyway
import pureconfig._
import pureconfig.module.cats._
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

//object Repository {
//
//  //Is this the best/safest way to expose the Repository classes? Not really....
//  lazy val Student: Task[StudentDb] = createSchema.map(_._1)
//  lazy val Module: Task[ModuleDb] = createSchema.map(_._2)
//
//  /** Method to perform db db.db.migrations */
//  private def migrate(dbUrl: String, user: String, pass: String) = Task {
//    val flyway = new Flyway()
//    flyway.setDataSource(dbUrl, user, pass)
//    flyway.migrate()
//  }
//
//  /** Init method to set up the database */
//  private val createSchema: Task[(StudentDb, ModuleDb)] = {
//
//    //TODO: use loadConfig to Either and lift result into EitherT[Task,...]
//    //TODO: paramaterise over transactor as in gem example then return tuple or HList of repositories
//
//    for {
//      cfg <- Task(loadConfigOrThrow[Config])
//      _ <- migrate(
//        s"${cfg.database.url}${cfg.database.name}",
//        cfg.database.user,
//        cfg.database.password
//      )
//      xa = DriverManagerTransactor[Task](
//        "org.postgresql.Driver",
//        s"${cfg.database.url}${cfg.database.name}",
//        cfg.database.user,
//        cfg.database.password
//      )
//      stDb = new StudentDb(xa)
//      mDb = new ModuleDb(xa)
//      - <- { println("Initialising Student tables");stDb.init }
//      _ <- { println("Initialising Module tables");mDb.init }
//    } yield (stDb, mDb)
//  }
//}