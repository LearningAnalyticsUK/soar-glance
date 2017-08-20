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

import cats._
import cats.implicits._
import kantan.csv._
import kantan.csv.ops._
import kantan.csv.cats._
import kantan.csv.generic._
import kantan.csv.java8._
import monix.eval.Task
import CsvRow._

/**
  * Job which transforms a selection of input .csvs containing Soar data
  */
object LoadSupportData extends Command[TansformConfig, Unit] {

  override def run(conf: TansformConfig) = ???

  /** Retrieve and parse all session rows from the provided file if possible */
  private def parseSessions[R <: HasStudent : RowDecoder](sessionsPath: String): Task[List[R]] = Task {

    println("parsing sessions")

    //Pull in the Sessions
    val readSessions = Paths.get(sessionsPath).asCsvReader[R](rfc)


  }

}
