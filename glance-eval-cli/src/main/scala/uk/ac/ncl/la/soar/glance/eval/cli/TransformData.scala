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
import monix.cats._
import uk.ac.ncl.la.soar.{ModuleCode, StudentNumber}
import uk.ac.ncl.la.soar.data.ModuleScore
import CsvRow._

/**
  * Job which transforms a selection of input .csvs containing Soar data
  */
object TransformData extends Command[TansformConfig, Unit] {

  override def run(conf: TansformConfig): Task[Unit] = {
    for {
      //Read compulsory files (marks)
      marks <- readMarks(conf.nessMarkPath, conf.prefix, conf.start, conf.stage)
      //TODO: module info
      //Transform and write compulsory files
      _ <- writeMarks(conf.outputPath, marks._1, "marks.csv")
      //Read, write and transform from whichever optional files are provided (recap, cluster, bb sessions etc...)
      _ <- Task.zip2(
        readWriteOptional[ClusterSessionRow](conf.clusterPath, marks._2, conf.outputPath, "clusterSessions.csv"),
        readWriteOptional[RecapSessionRow](conf.recapPath, marks._2, conf.outputPath, "recapSessions.csv"))
    } yield ()
  }

  private def readWriteOptional[R <: HasStudent : RowDecoder : RowEncoder](inPath: Option[String],
                                                                           cohort: Set[StudentNumber],
                                                                           outPath: String,
                                                                           outName: String) = {

    //If an optional file has been provided, read it in, transform it and write it out, otherwise, perform a no-op.
    inPath match {
      case Some(in) =>
        val parseTask = parseRecords[R](in, cohort)
        parseTask.flatMap(r => writeRecords(outPath, r, outName))
      case None =>
        Task.unit
    }
  }

  /** Retrieve and parse all Mark rows from provided file if possible */
  private def readMarks(marksPath: String, prefix: String,
                         year: String, stage: Int): Task[(Set[ModuleScore], Set[StudentNumber])] = Task {

    //Filter to find all NessMarks belonging to a particular student cohort, given start year and stage
    def rightCohort(m: NessMarkRow) =  m.year == year && m.module.startsWith(prefix+stage)

    //Pull in the NessMarks
    val readMarks = Paths.get(marksPath).asCsvReader[NessMarkRow](rfc.withHeader)

    //TODO: Check that the below isn't doing more passes than necessary, there is something funny about the collects
    //  could they be combined?

    //Drop errors and rows with the wrong prefix
    val prefixedRows = readMarks.collect({ case Success(m) if m.module.startsWith(prefix) => m }).toList

    //Find studentCohort
    val studentCohort = prefixedRows.iterator.collect({ case m if rightCohort(m) => m.student}).toSet

    //Find all marks from all years belonging to student Cohort (on modules with given prefix)
    (prefixedRows.collect({ case m if studentCohort.contains(m.student) =>
      ModuleScore(m.student, m.module, m.score.toDouble)
    }).flatten.toSet, studentCohort)
  }

  /** Retrieve and parse all rows from a file which contains a list of student records (i.e. contains a student number) */
  private def parseRecords[R <: HasStudent : RowDecoder](recordsPath: String, cohort: Set[StudentNumber]) = Task {
    //Pull in the Records
    val readRecords = Paths.get(recordsPath).asCsvReader[R](rfc.withHeader)

    //Drop the records not associated with the selected cohort of students
    readRecords.collect({ case Success(cs) if cohort.contains(cs.student) => cs }).toList
  }

  private def writeMarks(outputPath: String, marks: Set[ModuleScore], fileName: String): Task[Unit] = Task {
    val out = Paths.get(outputPath)

    val outDir = if(Files.exists(out)) out else Files.createDirectories(out)

    outDir.resolve(fileName).writeCsv(marks, rfc)
  }

  private def writeRecords[R : RowEncoder](outputPath: String, rows: List[R], fileName: String): Task[Unit] = Task {

    val out = Paths.get(outputPath)

    val outDir = if(Files.exists(out)) out else Files.createDirectories(out)

    outDir.resolve(fileName).writeCsv(rows, rfc)
  }

}
