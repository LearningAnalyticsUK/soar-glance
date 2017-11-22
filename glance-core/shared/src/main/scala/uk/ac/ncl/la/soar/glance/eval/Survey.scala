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
package uk.ac.ncl.la.soar.glance.eval

import java.util.UUID

import cats.implicits._
import io.circe.Decoder.Result
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import uk.ac.ncl.la.soar.Record._
import uk.ac.ncl.la.soar._
import uk.ac.ncl.la.soar.data._

import scala.collection.immutable.SortedMap
import scala.collection.mutable.ListBuffer
import scala.util.Random

/**
  * Case class representing an unanswered survey which will be presented to members of staff to fill out.
  */
case class Survey(modules: Set[ModuleCode], moduleToRank: ModuleCode, queries: List[StudentNumber],
                  entries: List[StudentRecords[SortedMap, ModuleCode, Double]],
                  id: UUID = UUID.randomUUID)

object Survey {

  /** Typeclass instances for Survey */
  implicit val encodeSurvey: Encoder[Survey] = new Encoder[Survey] {
    final def apply(a: Survey): Json = Json.obj(
      "id" -> a.id.toString.asJson,
      "modules" -> a.modules.asJson,
      "moduleToRank" -> a.moduleToRank.asJson,
      "queries" -> a.queries.asJson,
      "entries" -> a.entries.map(recordAsJson).asJson
    )

    private def recordAsJson(a: StudentRecords[SortedMap, ModuleCode, Double]) = Json.obj(
      "student_number" -> a.number.asJson,
      "scores" -> a.record.asJson
    )
  }

  implicit val decodeSurvey: Decoder[Survey] = new Decoder[Survey] {

    //TODO: Fix what I can only assume is a fairly inefficient decoder
    override def apply(c: HCursor): Result[Survey] = {
      for {
        id <- c.downField("id").as[String]
        modules <- c.downField("modules").as[Set[ModuleCode]]
        moduleToRank <- c.downField("moduleToRank").as[ModuleCode]
        queries <- c.downField("queries").as[List[StudentNumber]]
        entries <- c.downField("entries").as[List[(StudentNumber, Map[ModuleCode, Double])]]
      } yield {
        Survey(modules, moduleToRank, queries, recordsFrom(entries), UUID.fromString(id))
      }
    }

    private implicit val decodeRecord = Decoder.forProduct2("student_number", "scores")((s: StudentNumber, r: Map[ModuleCode, Double]) => (s,r))

    private def recordsFrom(part: List[(StudentNumber, Map[ModuleCode, Double])]) = part.map { case (stNum, records) =>
      val bldr = SortedMap.newBuilder[ModuleCode, Double]
      for(entry <- records) { bldr += entry }

      StudentRecords[SortedMap, ModuleCode, Double](stNum, bldr.result())
    }
  }

  /**
    * Factory method for new surveys.
    */
  def generate(records: List[ModuleScore], numQueries: Int, queryModules: Seq[ModuleCode], seed: Option[Int]): List[Survey] = {
    //Group the records by student and turn them into StudentRecords objects
    val stRecords = groupByStudents(records)
    println(s"Number of scores: ${records.size} Number of records: ${stRecords.size}")
    //Build the set of modules across all entries.
    val allModules = records.iterator.map(_.module).toSet
    //Pass the StudentRecords objects to generate a set of queries
    //TODO: provide a config parameter for training rather than magically deriving it
    val queries = sampleQueries(stRecords, 5, numQueries, queryModules.toSet, seed)
    //Each entry in queries represents the query set for one survey. Split them out and and make surveys
    queries.iterator.map { case (module, students) =>
        Survey(allModules, module, students, stRecords)
    }.toList
  }

  /**
    * Removing the score for queries (student -> module) and the score for any module which follows it
    * (where alphanum ~ chronological). Note: Likely do this on front-end after persistence.
    */
  private[glance] def truncateQueryRecords(s: Survey): Survey = {

    val qSet = s.queries.toSet
    val dEntries = ListBuffer.empty[StudentRecords[SortedMap, ModuleCode, Double]]
    
    for(e <- s.entries) {
      if (qSet.contains(e.number)) {
        dEntries += StudentRecords(e.number, e.record - s.moduleToRank)
      } else {
        dEntries += e
      }
    }

    s.copy(entries = dEntries.result())
  }

  /** Group module scores by studnet numbers and construct StudentRecords */
  private[glance] def groupByStudents(scores: List[ModuleScore]): List[StudentRecords[SortedMap, ModuleCode, Double]] = {
    //Group by studentNumber and construct records
    val fullRecords = scores.groupBy(_.student).map { case (stud, studScores) =>
      val full = SortedMap.newBuilder[ModuleCode, Double] ++= studScores.iterator.map(s => s.module -> s.score)
      StudentRecords(stud, full.result)
    }

    //TODO: replace magic number filter to drop students with few records with a conf option
    fullRecords.filter(_.record.size > 4).toList
  }

  /** Split a list of student records into records with a score for the given module (upto n records) and the remainder */
  private def splitNWithModule(records: List[StudentRecords[SortedMap, ModuleCode, Double]],
                               module: ModuleCode, n: Int) = {
    var counter = 0
    records.partition { r =>
      if (r.record.contains(module) && counter < n) { counter += 1; true } else false
    }
  }

  /**
    * Randomly sample the student records, selecting conf.elided students *per* module
    */
  private def sampleQueries(studentRecords: List[StudentRecords[SortedMap, ModuleCode, Double]],
                      trainingData: Int,
                      numQueries: Int,
                      queryModules: Set[ModuleCode],
                      seed: Option[Int]): Map[ModuleCode, List[StudentNumber]] = {

    require(studentRecords.size > (numQueries * queryModules.size) + trainingData, "The number of students for which " +
      "you have records must be greater than the formula (Number of queries * number of modules) + (Number of " +
      s"training records). Instead, you provided #queries:$numQueries, #modules: ${queryModules.size}, " +
      s"#training: $trainingData and students: ${studentRecords.size}.")

    //Take the list of students, shuffle then split off the training data from the head.
    //Create the rng with provided seed
    val rand = seed match {
      case Some(i) => new Random(i)
      case None => new Random()
    }

    def findNWithModule(n: Int, records: List[StudentRecords[SortedMap, ModuleCode, Double]], module: ModuleCode) = {
      records.iterator.filter(_.record.contains(module)).map(_.number).take(n).toList
    }
    //Split off a random first n student records which have a score for a given query module
    val studentsPerQuery = for {
      qM <- queryModules
    } yield qM -> findNWithModule(numQueries, rand.shuffle(studentRecords), qM)
    
    studentsPerQuery.toMap
  }

  /** get the list of distinct modulecodes, sorted alphanumerically (therefore chronologically) */
  private def getAllModules(scores: List[ModuleScore]): List[ModuleCode] = scores.map(_.module).sortWith(_ < _).distinct
}