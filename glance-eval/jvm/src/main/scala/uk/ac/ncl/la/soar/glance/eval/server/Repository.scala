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
package uk.ac.ncl.la.soar.glance.eval.server

import doobie.util.transactor.DriverManagerTransactor
import monix.eval.Task
import org.flywaydb.core.Flyway
import pureconfig.loadConfigOrThrow
import uk.ac.ncl.la.soar.db.Config
import uk.ac.ncl.la.soar.server.Implicits._

/** Description of Class
  *
  * @author hugofirth
  */
object Repository {

  //Is this the best/safest way to expose the Repository classes? Not really....
  lazy val Survey: Task[SurveyDb] = createSchema.map(_._1)
  lazy val SurveyResponse: Task[SurveyResponseDb] = createSchema.map(_._2)

  /** Method to perform db db.migrations */
  private def migrate(dbUrl: String, user: String, pass: String) = Task {
    val flyway = new Flyway()
    flyway.setDataSource(dbUrl, user, pass)
    flyway.migrate()
  }

  /** Init method to set up the database */
  private val createSchema: Task[(SurveyDb, SurveyResponseDb)] = {

    //TODO: Work out if this is even vaguely sane?
    //Lazy config for memoization?
    lazy val config = loadConfigOrThrow[Config]

    for {
      cfg <- Task(config)
      _ <- migrate(
        s"${cfg.database.url}${cfg.database.name}",
        cfg.database.user,
        cfg.database.password
      )
      xa = DriverManagerTransactor[Task](
        "org.postgresql.Driver",
        s"${cfg.database.url}${cfg.database.name}",
        cfg.database.user,
        cfg.database.password
      )
      sDb = new SurveyDb(xa)
      sRDb = new SurveyResponseDb(xa)
      - <- { println("Initialising Survey tables");sDb.init }
      _ <- { println("Initialising Survey tables");sRDb.init }
    } yield (sDb, sRDb)
  }
}
