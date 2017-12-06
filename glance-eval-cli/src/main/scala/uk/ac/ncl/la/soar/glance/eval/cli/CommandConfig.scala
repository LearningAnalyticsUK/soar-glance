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

import java.time.Year

import scopt._
import uk.ac.ncl.la.soar.ModuleCode

import scala.util.Try

/** ADT for Config types */
sealed trait CommandConfig

/**
  * Config "bag" case class for the survey generator and accompanying scopt parser
  * TODO: Remove common Option[String]
  */
final case class GenerateConfig(recordsPath: String = "",
                                numStudents: Int = 10,
                                students: Seq[String] = Seq.empty[String],
                                visualisations: Seq[String] = Seq.empty[String],
                                modules: Seq[String] = Seq.empty[String],
                                collection: Option[Int] = None,
                                seed: Option[Int] = None)
    extends CommandConfig

/**
  * Config "bag" case class for the job which transforms Soar data csv's and its accompanying scopt parser.
  */
final case class TansformConfig(clusterPath: Option[String] = None,
                                recapPath: Option[String] = None,
                                printedPath: Option[String] = None,
                                vlePath: Option[String] = None,
                                meetingsPath: Option[String] = None,
                                nessMarkPath: String = "",
//                                moduleInfoPath: String = "",
                                outputPath: String = "",
                                prefix: String = "",
                                start: String = "",
                                stage: Int = 0)
    extends CommandConfig

/**
  * Config "bag" case class for the job which loads transformed soar data to support surveys. Also the parser ...
  */
final case class LoadSupportConfig(clusterPath: String = "",
                                   recapPath: String = "")
    extends CommandConfig

/**
  * Config "bag" case class for the job which loads extra marks data in, besides that depended on by a survey.
  * Useful if students in a survey are expected to have taken modules with a different prefix code.
  */
final case class LoadExtraMarksConfig(extraMarks: String = "")
    extends CommandConfig

/**
  * Config "bag" case class for the job which exports csv results from survey response tables in the database.
  */
final case class ExportSurveyResultsConfig(outputPath: String = "",
                                           surveyId: String = "")
    extends CommandConfig

object CommandConfig {

  /** Factory method for Config objects - similar to scopt commands, but they seem inflexible */
  def apply(args: Array[String]): Option[CommandConfig] =
    args.headOption.flatMap {
      case "generate"     => generateParser.parse(args.tail, GenerateConfig())
      case "transform"    => transformParser.parse(args.tail, TansformConfig())
      case "load-support" => loadParser.parse(args.tail, LoadSupportConfig())
      case "load-extra-marks" =>
        extraMarkParser.parse(args.tail, LoadExtraMarksConfig())
      case "export-results" =>
        exportResultsParser.parse(args.tail, ExportSurveyResultsConfig())
      case _ => None
    }

  /** Package private helper object for parsing command line arguments, provided by scopt */
  private[cli] val generateParser =
    new OptionParser[GenerateConfig]("GlanceGen") {
      //Define the header for the command line display text
      head("Glance Survey Generator", "0.1.x")

      //Define the individual command line options
      opt[String]('i', "input")
        .required()
        .valueName("<file>")
        .action((x, c) => c.copy(recordsPath = x))
        .text("a required .csv file containing student marks.")

      opt[Int]('n', "num-students")
        .valueName("e.g. 10")
        .action((x, c) => c.copy(numStudents = x))
        .text("an optional parameter specifying how many student records to include in the generated surveys")

      opt[Seq[String]]('v', "visualisations")
        .required()
        .valueName("e.g. recap_vs_time,stud_module_scores,...")
        .action((x, c) => c.copy(visualisations = x))
        .text("visualisations is a required parameter detailing the list of visualisations to use in a Glance survey.")

      opt[Seq[String]]('m', "modules")
        .required()
        .valueName("e.g. CSC1021, CSC2024...")
        .action((x, c) => c.copy(modules = x))
        .text(
          "a required list of modules for which to generate surveys. Unless the --collection option is used, only one survey " +
            "will be generated per module.")

      opt[Seq[String]]('s', "students")
        .valueName("e.g. 3789,11722,98,...")
        .action((x, c) => c.copy(students = x))
        .text("an optional parameter specifying exactly which student records to include in the generated surveys. " +
          "Note that if --students are provided then --num-students and --collection will be ignored.")

      opt[Int]('c', "collection")
        .valueName("<int>")
        .action((x, c) => c.copy(collection = Option(x)))
        .text(
          "a number of surveys to generate in series. They will all use different students and may be completed one " +
            "after another.")

      opt[Int]('r', "random-seed")
        .valueName("<int>")
        .action((x, c) => c.copy(seed = Option(x)))
        .text(
          "random-seed is an optional integer to use as a seed when randomly (uniformly) selecting student records " +
            "to include in a survey.")

    }

  private[cli] val transformParser =
    new OptionParser[TansformConfig]("GlanceTransform") {
      //Define the header for the command line display text
      head("Glance Data Transformer", "0.1.x")

      //Define the individual command line options
      opt[String]('m', "marks")
        .required()
        .valueName("<file>")
        .action((x, c) => c.copy(nessMarkPath = x))
        .text("marks is a required .csv file containing student marks.")

      //    opt[String]('i', "module-info").required().valueName("<file>")
      //      .action((x, c) => c.copy(moduleInfoPath = x))
      //      .text("module-info is a required .csv file containing information about each of the modules for which a student " +
      //        "may have received marks.")

      opt[String]('o', "output")
        .required()
        .valueName("<path>")
        .action((x, c) => c.copy(outputPath = x))
        .text("output is a required parameter specifying the directory to write transformed data to.")

      opt[String]('p', "prefix")
        .required()
        .valueName("e.g. CSC")
        .action((x, c) => c.copy(prefix = x))
        .text("prefix is a required parameter which indicates the module code prefix for which we should transform marks.")

      opt[String]('y', "year")
        .required()
        .valueName("e.g. 2015")
        .action((x, c) => c.copy(start = x))
        .text("year is a required parameter which indicates the earliest academic year for which to transform marks.")

      opt[Int]('s', "stage")
        .required()
        .valueName("e.g. 2")
        .action((x, c) => c.copy(stage = x))
        .text("stage is a required parameter which indicates the earliest academic stage for which to transform marks.")

      opt[String]("cluster")
        .valueName("<file>")
        .action((x, c) => c.copy(clusterPath = Option(x)))
        .text("cluster is an optional .csv file containing student sessions using University clusters.")

      opt[String]("recap")
        .valueName("<file>")
        .action((x, c) => c.copy(recapPath = Option(x)))
        .text("recap is an optional .csv file containing student sessions using the ReCap video lecture service.")

      opt[String]("printed")
        .valueName("<file>")
        .action((x, c) => c.copy(printedPath = Option(x)))
        .text(
          "printed is an optional .csv file containing student print events.")

      opt[String]("vle")
        .valueName("<file>")
        .action((x, c) => c.copy(vlePath = Option(x)))
        .text(
          "vlePath is an optional .csv file containing student VLE sessions.")

      opt[String]("meetings")
        .valueName("<file>")
        .action((x, c) => c.copy(meetingsPath = Option(x)))
        .text("meetingsPath is an optional .csv file containing student meeting records.")
    }

  private[cli] val loadParser =
    new OptionParser[LoadSupportConfig]("GlanceLoadSupport") {
      //Define the header for the command line display text
      head("Glance Support Data Loader", "0.1.x")

      //Define the individual command line options
      opt[String]('c', "cluster-sessions")
        .required()
        .valueName("<file>")
        .action((x, c) => c.copy(clusterPath = x))
        .text("cluster-sessions is a required .csv file containing student sessions using University clusters.")

      opt[String]('r', "recap-sessions")
        .required()
        .valueName("<file>")
        .action((x, c) => c.copy(recapPath = x))
        .text("recap-sessions is a required .csv file containing student sessions using the ReCap video lecture service.")

    }

  private[cli] val extraMarkParser =
    new OptionParser[LoadExtraMarksConfig]("GlanceLoadExtraMarks") {
      //Define the header for the command line display text
      head("Glance Extra Marks Loader", "0.1.x")

      //Define the individual command line options
      opt[String]('m', "marks")
        .required()
        .valueName("<file>")
        .action((x, c) => c.copy(extraMarks = x))
        .text(
          "marks is an optional .csv file containing extra student marks to load (perhaps with a different module " +
            "code).")
    }

  private[cli] val exportResultsParser =
    new OptionParser[ExportSurveyResultsConfig]("GlanceExportResults") {
      //Define the header for the command line display text
      head("Glance Survey Result Exporter", "0.1.x")

      //Define the individual command line options
      opt[String]('o', "output")
        .required()
        .valueName("<directory>")
        .action((x, c) => c.copy(outputPath = x))
        .text("ouput is a required parameter specifying the directory to write the survey results into")

      opt[String]('s', "survey")
        .required()
        .valueName("<uuid>")
        .action((x, c) => c.copy(surveyId = x))
        .text("survey is a required parameter specifying the survey id to write the results for")
    }
}
