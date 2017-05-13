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

import cats._
import cats.implicits._
import uk.ac.ncl.la.soar._
import uk.ac.ncl.la.soar.data._
import uk.ac.ncl.la.soar.Record._

import scala.collection.immutable.SortedMap
import scala.util.Random


/**
  * TODO: provide an assess method on the trait which calculates RMSE between response and actual
  * TODO: provide helper methods like: trainingRecords, groundTruth etc...
  * TODO: Write some simple tests.
  * TODO: Clean up with a type alias
  * TODO: Explicitly represent common modules?
  * TODO: Refactor away from common ADT for Surveys and responses. Its confusing things
  */
/**
  * Case class representing an unanswered survey which will be presented to members of staff to fill out.
  */
case class Survey(modules: Set[ModuleCode], queries: Map[StudentNumber, ModuleCode],
                  entries: List[StudentRecords[SortedMap, ModuleCode, Double]],
                  id: UUID = UUID.randomUUID)

/**
  * Case class representing an a partially answered survey which is being completed by a member of staff.
  */
case class SurveyResponse(survey: Survey, responses: Map[StudentNumber, ModuleScore], respondent: String, id: UUID)

/**
  * Boolean extractor to detect completed survey responses
  */
object CompletedResponse {
  def unapply(arg: SurveyResponse): Boolean = arg.responses.size == arg.survey.queries.size
}

object Survey {

  /**
    * Factory method for `EmptySurvey`s.
    */
  def generate(records: List[ModuleScore], numQueries: Int, queryModules: Seq[ModuleCode],
               commonQuery: Option[ModuleCode], seed: Int): List[Survey] = {
    //Group the records by student and turn them into StudentRecords objects
    val stRecords = groupByStudents(records)
    //Build the set of modules across all entries.
    val allModules = records.iterator.map(_.module).toSet
    //Add the common module to moduleSet
    val queryModuleSet = (queryModules ++ commonQuery).toSet
    //Pass the StudentRecords objects to generate a set of queries
    //TODO: provide a config parameter for training rather than magically deriving it
    val queries = sampleQueries(stRecords, numQueries*2, numQueries, queryModuleSet, seed)
    //Get common queries if they exist and convert to a list (Nil if None)
    val cmnQ = commonQuery.flatMap(queries.get).getOrElse(List.empty[StudentNumber])
    //Trim the common query set from the map of query sets, then add the common queries to each set.
    val blendedQ = (queries -- commonQuery).mapValues(cmnQ ::: _)
    //Each entry in blendedQ represents the query set for one survey. Split them out and and make surveys
    blendedQ.iterator.map { case (module, students) =>
        val queryMap = students.map(_ -> module).toMap
        Survey(allModules, queryMap, stRecords)
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
    fullRecords.filter(_.record.size > 10).toList
  }

  /**
    * Randomly sample the student records, selecting conf.elided students *per* module, and removing both the score for
    * that module and the score for any module which follows it in the order (where alphanum ~ chronological).
    */
  private def sample(records: List[StudentRecords[SortedMap, ModuleCode, Double]],
                     elided: Int,
                     modules: Seq[ModuleCode],
                     common: Option[ModuleCode],
                     seed: Int): Map[ModuleCode, List[StudentRecords[SortedMap, ModuleCode, Double]]] = {

      //Create the rng with provided seed
      val rand = new Random(seed)
      //Shuffle the records list using Random
      val shuffled = rand.shuffle(records)
      //TODO: Remove this magically derived number (elided*2) and replace with a config option of some kind.
      //First take the "training data" which is a fixed n student records, where n = elided * 2
      val (training, students) = shuffled.splitAt(elided*2) match {
        case (_, Nil) => throw new IllegalArgumentException("The number of students for which you have records must be " +
          s"greater than the formula (elided * #modules) + (elided * 2). You provided elided:$elided, #modules: " +
          s"${modules.size} and students: ${shuffled.size}.")
        case a => a
      }

      //Then chunk students into segments the size of elided, then drop modules from each chunk to create survey pieces
      val surveyChunks = modules.distinct.iterator.zip(students.grouped(elided)).map({ case (module, studentChunk) =>
        module -> studentChunk.map { student =>
          val truncated = student.record.toKey(module).updated(module, -1.0)
          student.copy(record = truncated)
        }
      }).toMap

      //If a common module has been specified, retrieve its chunk and remove it from surveyChunks
      val commonChunk = common.flatMap(surveyChunks.get).getOrElse(List.empty[StudentRecords[SortedMap, ModuleCode, Double]])
      val chunksNoCommon = common.fold(surveyChunks)(surveyChunks - _)

      //Combine training, common and a survey chunk to produce a survey of records, sorted by studentNumber.
      chunksNoCommon.mapValues(c => (training ::: commonChunk ::: c).sortWith(_.number < _.number))
  }

  /**
    * Second take on the sample method as the existing one confuses me enough that I don't want to undertake a refactor
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