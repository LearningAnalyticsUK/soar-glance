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

import java.nio.file.{Files, Paths}
import java.time.{Instant, Year}

import cats._
import cats.implicits._
import kantan.csv._
import kantan.csv.ops._
import kantan.csv.cats._
import kantan.csv.generic._
import kantan.csv.java8._
import monix.eval.Task
import uk.ac.ncl.la.soar.{ModuleCode, StudentNumber}
import uk.ac.ncl.la.soar.data.ModuleScore

/**
  * Job which transforms a selection of input .csvs containing Soar data
  */
object TransformData extends Command[TansformConfig, Unit] {

  /** Types which correspond to csv rows */
  //More trouble than its worth perhaps? More as an intellectual exercise, how would I auto derive instances for Row
  // types if HasStudent was a TC?
  sealed trait HasStudent {
    def student: StudentNumber
  }

  //TODO: Create encoder/decoder for Year type and switch back to using it
  case class NessMarkRow(student: StudentNumber, study: Int, stage: Int, year: String, progression: String, module: ModuleCode,
                         score: Int, component: String, componentAttempt: Int, componentScore: Int,
                         componentWeight: Double, due: Option[Instant]) extends HasStudent

  case class ClusterSessionRow(start: Instant, end: Instant, student: StudentNumber, study: Int, stage: Int,
                               machine: String) extends HasStudent

  case class RecapSessionRow(start: Instant, student: StudentNumber, study: Int, stage: Int,
                             duration: Int) extends HasStudent

  implicit val moduleScoreEncoder: RowEncoder[ModuleScore] = RowEncoder.ordered { ms: ModuleScore =>
    (ms.student, ms.module, ms.score)
  }

  override def run(conf: TansformConfig): Task[Unit] = {
    for{
      m <- parseMarks(conf.nessMarkPath, conf.prefix, conf.start, conf.stage)
      s <- Task.zip2(parseSessions[ClusterSessionRow](conf.clusterPath, m._2),
        parseSessions[RecapSessionRow](conf.recapPath, m._2))
      _ <- Task.zip3(writeMarks(conf.outputPath, m._1),
        writeSessions(conf.outputPath, s._1, "clusterSessions.csv"),
        writeSessions(conf.outputPath, s._2, "recapSessions.csv"))
    } yield ()
  }

  /** Retrieve and parse all Mark rows from provided file if possible */
  private def parseMarks(marksPath: String, prefix: String,
                         year: String, stage: Int): Task[(List[ModuleScore], Set[StudentNumber])] = Task {

    //Filter to find all NessMarks belonging to a particular student cohort, given start year and stage
    def rightCohort(m: NessMarkRow) =  m.year == year && m.stage == stage

    //Pull in the NessMarks
    val readMarks = Paths.get(marksPath).asCsvReader[NessMarkRow](rfc.withHeader)

    //Drop errors and rows with the wrong prefix
    val prefixedRows = readMarks.collect({ case Success(m) if m.module.startsWith(prefix) => m }).toList

    //Find studentCohort
    val studentCohort = prefixedRows.iterator.collect({ case m if rightCohort(m) => m.student}).toSet

    //Find all marks from all years belonging to student Cohort (on modules with given prefix)
    (prefixedRows.collect({ case m if studentCohort.contains(m.student) =>
      ModuleScore(m.student, m.module, m.score.toDouble)
    }).flatten.distinct, studentCohort)
  }

  /** Retrieve and parse all session rows from the provided file if possible */
  private def parseSessions[R <: HasStudent : RowDecoder](sessionsPath: String,
                                              studentCohort: Set[StudentNumber]): Task[List[R]] = Task {
    //Pull in the Sessions
    val readSessions = Paths.get(sessionsPath).asCsvReader[R](rfc.withHeader)

    //Drop sessions not associated with cohort students
    readSessions.collect({ case Success(cs) if !studentCohort.contains(cs.student) => cs }).toList
  }

  private def writeMarks(outputPath: String, marks: List[ModuleScore]): Task[Unit] = Task {
    val out = Paths.get(outputPath)

    val outDir = if(Files.exists(out)) out else Files.createDirectories(out)

    outDir.resolve("marks.csv").writeCsv(marks, rfc)
  }

  private def writeSessions[R : RowEncoder](outputPath: String, rows: List[R], fileName: String): Task[Unit] = Task {
    val out = Paths.get(outputPath)

    val outDir = if(Files.exists(out)) out else Files.createDirectories(out)

    outDir.resolve(fileName).writeCsv(rows, rfc)
  }

}
