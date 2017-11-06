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
package uk.ac.ncl.la.soar.glance.eval.db

import doobie.util.transactor.DriverManagerTransactor
import monix.eval.Task
import org.flywaydb.core.Flyway
import pureconfig.loadConfigOrThrow
import uk.ac.ncl.la.soar.db.{Config, ModuleDb, ModuleScoreDb}
import uk.ac.ncl.la.soar.server.Implicits._

/** Description of Class
  *
  * @author hugofirth
  */
object Repositories {

  //TODO: Really refactor this next of lazy snakes. Sure this is stupid

  //Is this the best/safest way to expose the Repository classes? Not really....
  lazy val Survey: Task[SurveyDb] = schema.map(_._1)
  lazy val SurveyResponse: Task[SurveyResponseDb] = schema.map(_._2)
  lazy val ClusterSession: Task[ClusterSessionDb] = schema.map(_._3)
  lazy val RecapSession: Task[RecapSessionDb] = schema.map(_._4)
  lazy val Module: Task[ModuleDb] = schema.map(_._5)
  lazy val ModuleScore: Task[ModuleScoreDb] = schema.map(_._6)

  /** Method to perform db db.db.migrations */
  private def migrate(dbUrl: String, user: String, pass: String) = Task {
    val flyway = new Flyway()
    flyway.setDataSource(dbUrl, user, pass)
    flyway.migrate()
  }

  private lazy val schema = createSchema.memoize

  /* Init method to set up the database */
  private val createSchema: Task[(SurveyDb, SurveyResponseDb, ClusterSessionDb, RecapSessionDb, ModuleDb, ModuleScoreDb)] = {

    //Lazy config for memoization?
    lazy val config = loadConfigOrThrow[Config]

    for {
      cfg <- Task {println(s"HI THIS IS CONFIG: $config"); config}
      xa = DriverManagerTransactor[Task](
        "org.postgresql.Driver",
        s"${cfg.database.url}${cfg.database.name}",
        cfg.database.user,
        cfg.database.password
      )
      sDb = new SurveyDb(xa)
      sRDb = new SurveyResponseDb(xa)
      cSDb = new ClusterSessionDb(xa)
      rSDb = new RecapSessionDb(xa)
      mDb = new ModuleDb(xa)
      msDb = new ModuleScoreDb(xa)
      _ <- { println("Initialising Survey tables");sDb.init }
      _ <- { println("Initialising Survey response tables");sRDb.init }
      _ <- { println("Initialising Cluster session tables");cSDb.init }
      _ <- { println("Initialising Recap session tables");rSDb.init }
      _ <- { println("Initialising Module tables");mDb.init }
      _ <- { println("Initialising Module Score tables");msDb.init }
    } yield (sDb, sRDb, cSDb, rSDb, mDb, msDb)
  }
}
