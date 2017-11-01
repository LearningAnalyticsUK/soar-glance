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

import cats._
import cats.implicits._
import monix.eval.Task
import monix.cats._
import uk.ac.ncl.la.soar.{ModuleCode, StudentNumber}
import uk.ac.ncl.la.soar.data.{ModuleScore, Records, StudentRecords}
import uk.ac.ncl.la.soar.glance.eval.Survey

import scala.collection.immutable.SortedMap
import scala.io.Source

/**
  * Job which reads in the completed survey CSVs created earlier using [[GenerateSurveys]].
  *
  * TODO: genericise on Monad TC, also look into using Task and how best to do a Task[ Either[ ] ]
  *
  * So brief reading would imply that Task is just another Error monad in this context - just like Either, so perhaps
  * genericising on MonadError will be enough? No idea how without a major refactor though? Check out MonadError
  *
  * Hmm - this point seems to be more contentious - implying (as I originally thought) that Either and Task do similar
  * things but at different levels - Either is for the recoverable user level errors, Task is for the system level
  * errors. I.e. the 4-500 divide in http servers. In particular this comment by /u/m50d is interesting:
  *
  * https://www.reddit.com/r/scala/comments/5clxku/scalaz_task_taska_versus_taskeithera_b/
  */
object AssessSurveyResponses extends Command[AssessConfig, Unit] {

  //TODO: Make a Meta struct (or Type Alias to Map) which will be populated from the meta.json file which is going to
  // have to be generated for each survey folder.

  def run(conf: AssessConfig): Task[Unit] = {
    ???
  }

  /**
    * Method takes the path of a directory which contains several sub-directories, where each sub-directory corresponds
    * to a survey. The name of each sub-directory is taken to be
    *
    */
  private def parseSurveys(inputPath: String): Either[String, List[Survey]] = {

    //Get all directories in input path.


    //Get the directory names


    //Parse individual survey - probably another method

    ???
  }

  /**
    * Given the path to a sub-directory corresponding to a single survey, parse the contents and produce an error
    * message or a Survey object
    */
  private def parseSurvey(surveyPath: String): Either[String, Survey] = {
    //Read in Survey csv
    val lines = Source.fromFile(surveyPath).getLines()
    val entries = lines.toList

    //TODO: Simplify and refactor the below

    for {
    //If the csv is not empty, parse the header
      hd <- entries.headOption.toRight("Incorrectly formatted survey csv. The survey csv is empty!")
      columns <- parseHeader(hd)
      //For each line - parse a list of module scores.
      scores <- entries.tail.traverse(parseModuleScore(columns, _))
      //Map the right hand side of scores to StudentRecords
      records <- scores.traverse(scoresToStudentRecord(_:_*)).toRight("Incorrectly formatted survey csv. Empty entry!")
      //Map the right hand side of scores to a Survey
      //TODO: Parse map of queries from meta.json - below is a filler
      rankModule <- Either.right("")
      queries <- Either.right(List.empty[StudentNumber])
    } yield Survey(columns.toSet, rankModule, queries, records)
  }

  /** Converts a list fo ModuleScores to a StudentRecord - be aware that this student record uses the student id
    * associated with the first modulescore in the varargs.
    *
    * TODO: Map vs SortedMap here?
    */
  private def scoresToStudentRecord(scores: ModuleScore*): Option[StudentRecords[SortedMap, ModuleCode, Double]] = {
    val student = scores.headOption.map(_.student)
    val bldr = SortedMap.newBuilder[ModuleCode, Double]
    for (score <- scores) {
      bldr += (score.module -> score.score)
    }
    student.map(StudentRecords(_, bldr.result()))
  }

  /**
    * Given a sequence of module codes in column order, parse a sequence of module scores, or produce an error message
    *
    * Column order is the order in which module codes appear in a survey csv's header.
    */
  private def parseModuleScore(moduleCodes: Seq[ModuleCode], entry: String): Either[String, Seq[ModuleScore]] = {
    //Split on comma and trim
    val cleaned = entry.split(',').map(_.trim).toList
    val scores = cleaned.tail
    //Convert blank score strings to -1.0 as these are valid doubles but will get dropped by flatMap(ModuleScore) later,
    // taking their associated module codes with them. Perhaps a bit yucky, but given that we don't zip with the module
    // codes until later I can't think of a better way right now.
    val doubleScores = scores.traverse {
      case "" => Either.right[NumberFormatException, Double](-1.0)
      case a => Either.catchOnly[NumberFormatException](a.toDouble)
    }

    //Using the student (cleaned head), flatMap to create Modulescores
    for {
    //Get the student number
      student <- cleaned.headOption.toRight("The entry was malformatted! Expected e.g. 100937864, 68.5, 48.0, ...")
      sc <- doubleScores.leftMap(_ => "The entry was malformatted! One of the scores was not a valid double.")
      //Take doubleScores and zip with module
      pairs <- Either.cond(moduleCodes.nonEmpty, moduleCodes.zip(sc), "Incorrectly formatted survey csv!")
    //And finally - if all the above works, pass the values to the option returning ModuleScore factory
    } yield pairs.flatMap(p => ModuleScore(student, p._1, p._2))
  }

  /**
    * Given the header of a csv, return all the column headers except the first.
    * These are the module codes in the survey
    */

  private def parseHeader(header: String): Either[String, Seq[ModuleCode]] =  {
    //Parse out "Student Number" and the module codes as an indexed vector
    val columns = header.split(',').map(_.trim)
    //Check if student number is first column -
    if(columns(0) == "Student Number")
      Left("Incorrectly formatted survey csv. Expected \"Student Number\" as first column")
    else
      Right(columns.tail)
  }

}

