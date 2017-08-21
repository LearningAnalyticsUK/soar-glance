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

import cats._
import cats.implicits._
import uk.ac.ncl.la.soar._
import uk.ac.ncl.la.soar.data._
import uk.ac.ncl.la.soar.Record._

import scala.collection.immutable.SortedMap
import scala.util.Random
import io.circe._
import io.circe.Decoder.Result
import io.circe.syntax._
import io.circe.generic.auto._

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
  def generate(records: List[ModuleScore], numQueries: Int, queryModules: Seq[ModuleCode], seed: Int): List[Survey] = {
    //Group the records by student and turn them into StudentRecords objects
    val stRecords = groupByStudents(records)
    //Build the set of modules across all entries.
    val allModules = records.iterator.map(_.module).toSet
    //Pass the StudentRecords objects to generate a set of queries
    //TODO: provide a config parameter for training rather than magically deriving it
    val queries = sampleQueries(stRecords, numQueries*2, numQueries, queryModules.toSet, seed)
    //Each entry in queries represents the query set for one survey. Split them out and and make surveys
    queries.iterator.map { case (module, students) =>
        val queryMap = students.map(_ -> module).toMap
        Survey(allModules, module, students, stRecords)
    }.toList
  }


  /** Group module scores by studnet numbers and construct StudentRecords */
  private[glance] def groupByStudents(scores: List[ModuleScore]): List[StudentRecords[SortedMap, ModuleCode, Double]] = {
    //Group by studentNumber and construct records
    val fullRecords = scores.groupBy(_.student).map { case (stud, studScores) =>
      val full = SortedMap.newBuilder[ModuleCode, Double] ++= studScores.iterator.map(s => s.module -> s.score)
      StudentRecords(stud, full.result)
    }

    //TODO: replace magic number filter to drop students with few records with a conf option
    fullRecords.filter(_.record.size > 7).toList
  }

  /**
    * Randomly sample the student records, selecting conf.elided students *per* module, and removing both the score for
    * that module and the score for any module which follows it in the order (where alphanum ~ chronological).
    */
  private def sampleQueries(studentRecords: List[StudentRecords[SortedMap, ModuleCode, Double]],
                      trainingData: Int,
                      numQueries: Int,
                      queryModules: Set[ModuleCode],
                      seed: Int): Map[ModuleCode, List[StudentNumber]] = {

    //Take the list of students, shuffle then split off the training data from the head.
    //Create the rng with provided seed
    val rand = new Random(seed)
    //Map student records to simple student numbers then shuffle with the rng
    val shuffled = rand.shuffle(studentRecords.map(_.number))
    //First take the "training data" which is a fixed n student records
    val (trainingStudents, queryStudents) = shuffled.splitAt(trainingData) match {
      case (_, Nil) => throw new IllegalArgumentException("The number of students for which you have records must be " +
        "greater than the formula (Number of queries * number of modules) + (Number of training records). Instead, " +
        s"you provided #queries:$numQueries, #modules: ${queryModules.size}, #training: $trainingData and " +
        s"students: ${studentRecords.size}.")
      case a => a
    }

    //Take students to be used for queries (queryStudents) and group them into chunks the size of queriesPerModule
    val queryStudentChunks = queryStudents.grouped(numQueries)
    //Assign each of these chunks of students to one of the module codes for which we need queries and return the map
    queryModules.iterator.zip(queryStudentChunks).toMap
  }

  /** get the list of distinct modulecodes, sorted alphanumerically (therefore chronologically) */
  private def getAllModules(scores: List[ModuleScore]): List[ModuleCode] = scores.map(_.module).sortWith(_ < _).distinct
}