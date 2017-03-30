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
import uk.ac.ncl.la.soar.{ModuleCode, StudentNumber}
import uk.ac.ncl.la.soar.data.{ModuleScore, StudentRecords}

import scala.io.Source

/**
  * Job which reads in the completed survey CSVs created earlier using [[Generator]].
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
object Assessor extends Job[AssessorConfig] {

  /** Simple struct to represent a survey ... TODO: explain more about what a survey is here */
  case class Survey(modules: Set[ModuleCode], queries: Map[StudentNumber, ModuleCode],
                    entries: List[StudentRecords[Map, ModuleCode, Double]])

  //TODO: Make a Meta struct (or Type Alias to Map) which will be populated from the meta.json file which is going to
  // have to be generated for each survey folder.

  def run(conf: AssessorConfig): Either[Throwable, Unit] = {
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

  private def parseSurvey(surveyPath: String): Either[String, Survey] = {
    //Read in Survey csv
    val lines = Source.fromFile(surveyPath).getLines()
    //Split off headers
    val (header, entries) =
      if (lines.hasNext)
        (Option(lines.next()), lines)
      else
        (None, Iterator.empty)
    //If the csv is not empty, parse the header
    val columns = header.toRight("Incorrectly formatted survey csv. The survey csv is empty!").flatMap(parseHeader)
    //For each line - parse a list of module scores.
    entries.map(parseModuleScore(columns, _))

    //Take tail and zip with columns. Map the resulting (Code, Score) pairs to ModuleScore objects
    //  Create student record

  }

  private def parseModuleScore(moduleCodes: Either[String, Seq[ModuleCode]], entry: String): Either[String, Seq[ModuleScore]] = {
    //Split on comma and trim
    val cleaned = entry.split(',').map(_.trim)
    val scores = cleaned.tail
    //Convert blank score strings to -1.0 as these are valid doubles but will get dropped by flatMap(ModuleScore) later,
    // taking their associated module codes with them. Perhaps a bit yucky, but given that we don't zip with the module
    // codes until later I can't think of a better way right now.
    val doubleScores = scores.toList.traverse {
      case "" => Either.right[NumberFormatException, Double](-1.0)
      case a => Either.catchOnly[NumberFormatException](a.toDouble)
    }
    //Get the student number
    val stNo = cleaned.headOption.toRight("The entry was malformatted! Expected e.g. 100937864, 68.5, 48.0, ...")

    //Using the student (cleaned head), flatMap to create Modulescores
    for {
      student <- stNo
      sc <- doubleScores.leftMap(_ => "The entry was malformatted! One of the scores was not a valid double.")
      //Take doubleScores and zip with module
      pairs <- moduleCodes.map(_.zip(sc))
      //And finally - if all the above works, pass the values to the option returning ModuleScore factory
    } yield pairs.flatMap(p => ModuleScore(student, p._1, p._2))
  }

  //TODO: Seq ok here?
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

