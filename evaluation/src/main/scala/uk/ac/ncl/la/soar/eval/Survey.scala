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
package uk.ac.ncl.la.soar.eval


import cats._
import cats.implicits._
import uk.ac.ncl.la.soar._
import uk.ac.ncl.la.soar.data._
import uk.ac.ncl.la.soar.Record._

import scala.collection.immutable.SortedMap
import scala.util.Random


/**
  * Survey ADT, surveys may be empty, or contain responses.
  */
sealed trait Survey {

  /** Hase the survey been completed or not yet? Default answer is no */
  def completed: Boolean

  //TODO: Factor out common getter methods as defs. Will involve re-reifying elements as Suvery
}

/**
  * Case class representing an unanswered survey which will be presented to members of staff to fill out.
  */
case class EmptySurvey(modules: Set[ModuleCode], queries: Map[StudentNumber, ModuleCode],
                  entries: List[StudentRecords[Map, ModuleCode, Double]]) extends Survey {

  val completed = false
}

//From records by line
//From CSV

/**
  * Case class representing an a partially answered survey which is being completed by a member of staff.
  */
case class SurveyResponse(modules: Set[ModuleCode], queries: Map[StudentNumber, ModuleCode],
                          response: Map[StudentNumber, ModuleScore], respondent: String,
                          entries: List[StudentRecords[Map, ModuleCode, Double]]) extends Survey {

  def completed = queries.size == response.size
}


/**
  * Case class representing a completed survey.
  */
case class CompletedSurvey(modules: Set[ModuleCode], responses: Map[StudentNumber, ModuleScore], respondent: String,
                           entries: List[StudentRecords[Map, ModuleCode, Double]]) extends Survey {

  val completed = true
}

object Survey {

  /**
    * Factory method for `EmptySurvey`s.
    */
  def generate(records: List[ModuleScore], elided: Int, modules: Seq[ModuleCode], common: Option[String],
               seed: Int): List[Survey] = {
    val stRecords = groupByStudents(records)

    // TODO: refactor sample to return a candidate set of queries only. Can be elided later on.
    // TODO: provide an assess method on the trait which calculates RMSE between response and actual
    // TODO: provide helper methods like: trainingRecords, groundTruth etc...
    // TODO: Write some simple tests.
  }


  /** Group module scores by studnet numbers and construct StudentRecords */
  private def groupByStudents(scores: List[ModuleScore]): List[StudentRecords[SortedMap, ModuleCode, Double]] = {
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
      //First take the "training data" which is a fixed n student records, where n = elided * 2
      val (t, s) = shuffled.splitAt(elided*2) match {
        case (_, Nil) => throw new IllegalArgumentException("The number of students for which you have records must be " +
          s"greater than the formula (elided * #modules) + (elided * 2). You provided elided:$elided, #modules: " +
          s"${modules.size} and students: ${shuffled.size}.")
        case a => a
      }

      //Then chunk s into segments the size of elided, then drop modules from each chunk to create survey pieces
      val surveyChunks = modules.distinct.iterator.zip(s.grouped(elided)).map({ case (module, students) =>
        module -> students.map { s =>
          val truncated = s.record.toKey(module).updated(module, -1.0)
          s.copy(record = truncated)
        }
      }).toMap

      //If a common module has been specified, retrieve its chunk and remove it from surveyChunks
      val commonChunk = common.flatMap(surveyChunks.get).getOrElse(List.empty[StudentRecords[SortedMap, ModuleCode, Double]])
      val chunksNoCommon = common.fold(surveyChunks)(surveyChunks - _)

      //Combine training, common and a survey chunk to produce a survey of records, sorted by studentNumber.
      chunksNoCommon.mapValues(c => (t ::: commonChunk ::: c).sortWith(_.number < _.number))
  }

  /** Get the list of distinct ModuleCodes, sorted alphanumerically (therefore chronologically) */
  private def getAllModules(scores: List[ModuleScore]): List[ModuleCode] = scores.map(_.module).sortWith(_ < _).distinct
}