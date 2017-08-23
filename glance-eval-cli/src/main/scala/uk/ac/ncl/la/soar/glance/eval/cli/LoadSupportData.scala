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
package uk.ac.ncl.la.soar.glance.eval.cli

import java.nio.file.Paths
import java.time.Instant
import java.util.UUID

import cats._
import cats.implicits._
import kantan.csv._
import kantan.csv.ops._
import kantan.csv.cats._
import kantan.csv.generic._
import kantan.csv.java8._
import monix.eval.Task
import monix.cats._
import CsvRow._
import uk.ac.ncl.la.soar.glance.eval.{ClusterSession, RecapSession}
import uk.ac.ncl.la.soar.glance.eval.server._

import scala.util.Try

/**
  * Job which transforms a selection of input .csvs containing Soar data
  */
object LoadSupportData extends Command[LoadSupportConfig, Unit] {

  override def run(conf: LoadSupportConfig) = {
    for {
      r <- Task.zip2(parseSessions[ClusterSessionRow](conf.clusterPath), parseSessions[RecapSessionRow](conf.recapPath))
      cSDb <- Repository.ClusterSession
      rSDb <- Repository.RecapSession
      _ <- Task.zip2(cSDb.saveBatch(r._1.map(prepareClusterRow)), rSDb.saveBatch(r._2.map(prepareRecapRow)))
    } yield ()
  }

  /** Retrieve and parse all session rows from the provided file if possible */
  private def parseSessions[R <: HasStudent : RowDecoder](sessionsPath: String): Task[List[R]] = Task {

    println("parsing sessions")

    //Pull in the Sessions
    val readSessions = Paths.get(sessionsPath).asCsvReader[R](rfc)

    val sessions = readSessions.collect({ case Success(s) => s}).toList
    println(s"parsed ${sessions.size} sessions")

    sessions
  }

  /** Data provided with ms precision which is mal-formatted, so we drop it */
  private def prepTs(ts: String) = Try(Instant.parse(ts.dropRight(4).concat("Z"))).getOrElse(Instant.now)

  private def prepareClusterRow(r: ClusterSessionRow): ClusterSessionTable.Row =
    ClusterSession(prepTs(r.start), prepTs(r.end), r.machine, r.student, UUID.randomUUID)

  private def prepareRecapRow(r: RecapSessionRow): RecapSessionTable.Row =
    RecapSession(UUID.randomUUID, prepTs(r.start), r.student, r.duration.toInt)
}
