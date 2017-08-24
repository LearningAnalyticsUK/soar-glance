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
                                elided: Int = 10,
                                modules: Seq[String] = Seq.empty[String],
                                seed: Int = 1921437) extends CommandConfig

/**
  * Config "bag" case class for the survey evaluator and accompanying scopt parser.
  */
final case class AssessConfig(inputPath: String = "",
                              outputPath: String = "",
                              modelPath: String = "",
                              metric: String = "rmse") extends CommandConfig

/**
  * Config "bag" case class for the job which transforms Soar data csv's and its accompanying scopt parser.
  */
final case class TansformConfig(clusterPath: String = "",
                                recapPath: String = "",
                                nessMarkPath: String = "",
                                outputPath: String = "",
                                prefix: String = "",
                                start: String = "",
                                stage: Int = 0) extends CommandConfig

/**
  * Config "bag" case class for the job which loads transformed soar data to support surveys. Also the parser ...
  */
final case class LoadSupportConfig(clusterPath: String = "", recapPath: String = "") extends CommandConfig

object CommandConfig {

  /** Factory method for Config objects - similar to scopt commands, but they seem inflexible */
  def apply(args: Array[String]): Option[CommandConfig] = args.headOption.flatMap {
    case "generate" => generateParser.parse(args.tail, GenerateConfig())
    case "assess" => assessParser.parse(args.tail, AssessConfig())
    case "transform" => transformParser.parse(args.tail, TansformConfig())
    case "load-support" => loadParser.parse(args.tail, LoadSupportConfig())
    case _ => None
  }

  /** Package private helper object for parsing command line arguments, provided by scopt */
  private[cli] val generateParser = new OptionParser[GenerateConfig]("SoarEvalGen") {
    //Define the header for the command line display text
    head("Soar Evaluation Survey Generator", "0.1.x")

    //Define the individual command line options
    opt[String]('i', "input").required().valueName("<file>")
      .action((x, c) => c.copy(recordsPath = x))
      .text("input is a required .csv file containing student/module scores. " +
        "Format \"StudentNumber, Module Code, Percentage\"")

    opt[Int]('e', "elided").valueName("e.g. 20")
      .action((x, c) => c.copy(elided = x))
      .text("elided is an optional parameter specifying how many student records to partially elide in the generated " +
        "surveys.")

    opt[Seq[String]]('m', "modules").required().valueName("e.g. CSC1021, CSC2024...")
      .action((x, c) => c.copy(modules = x))
      .text("modules is the list of modules for which to elide a students records. Only one module record will be " +
        "elided per student. One survey is generated per elided module code.")

    opt[Int]('s', "seed").valueName("<int>")
      .action((x, c) => c.copy(seed = x))
      .text("seed is an optional parameter specifying a number to use as a seed when randomly selecting student " +
        "records to elide.")
  }

  private[cli] val assessParser = new OptionParser[AssessConfig]("SoarEvalAssess") {
    //Define the header for the command line display text
    head("Soar Evaluation Survey Assessor", "0.1.x")

    //Define the individual command line options
    opt[String]('i', "input").required().valueName("<directory>")
      .action((x, c) => c.copy(inputPath = x))
      .text("input is a required directory containing completed survey csvs, in the folder structure producted by " +
        "`generate`.")

    opt[String]('o', "output").required().valueName("<file>")
      .action((x, c) => c.copy(outputPath = x))
      .text("output is a required parameter specifying the file to write survey evaluations to.")

    opt[String]('m', "model").required().valueName("<directory>")
      .action((x, c) => c.copy(modelPath = x))
      .text("model is a required directory containing the Spark based predictive model against which we are " +
        "comparing surveys.")

    opt[String]("metric").valueName("e.g. mse, rmse...")
      .action((x, c) => c.copy(metric = x))
      .text("metric is an optional parameter specifying the metric to be used in the comparison between predictive " +
        "model and surveys. Default is rmse.")
  }

  private[cli] val transformParser = new OptionParser[TansformConfig]("SoarEvalTransform") {
    //Define the header for the command line display text
    head("Soar Evaluation Data Transformer", "0.1.x")

    //Define the individual command line options
    opt[String]('c', "cluster-sessions").required().valueName("<file>")
      .action((x, c) => c.copy(clusterPath = x))
      .text("cluster-sessions is a required .csv file containing student sessions using University clusters " +
        "Format \"SessionStartTimestamp, SessionEndTimestamp, StudentId, StudyId, StageId, MachineName\"")

    opt[String]('r', "recap-sessions").required().valueName("<file>")
      .action((x, c) => c.copy(recapPath = x))
      .text("recap-sessions is a required .csv file containing student sessions using the ReCap video lecture service " +
        "Format \"SessionStartTime, RecapId, StudentId, StudyId, StageId, SecondsListened\"")

    opt[String]('m', "marks").required().valueName("<file>")
      .action((x, c) => c.copy(nessMarkPath = x))
      .text("marks is a required .csv file containing student marks " +
        "Format \"StudentId, StudyId, StageId, AcademicYear, ProgressionCode, ModuleCode, ModuleMark, ComponentText, " +
        "ComponentAttempt, ComonentMark, Weighting, TimestampDue\"")

    opt[String]('o', "output").required().valueName("<directory>")
      .action((x, c) => c.copy(outputPath = x))
      .text("output is a required parameter specifying the directory to write transformed data to.")

    opt[String]('p', "prefix").required().valueName("e.g. CSC")
      .action((x, c) => c.copy(prefix = x))
      .text("prefix is a required parameter which indicates the module code prefix for which we should transform marks")

    opt[String]('y', "year").required().valueName("e.g. 2015")
      .action((x, c) => c.copy(start = x))
      .text("year is a required parameter which indicates the earliest academic year for which to transform marks")

    opt[Int]('s', "stage").required().valueName("e.g. 2")
      .action((x, c) => c.copy(stage = x))
      .text("stage is a required parameter which indicates the earliest academic stage for which to transform marks")
  }

  private[cli] val loadParser = new OptionParser[LoadSupportConfig]("SoarEvalLoadSupport") {
    //Define the header for the command line display text
    head("Soar Evaluation Support Data Loader", "0.1.x")

    //Define the individual command line options
    opt[String]('c', "cluster-sessions").required().valueName("<file>")
      .action((x, c) => c.copy(clusterPath = x))
      .text("cluster-sessions is a required .csv file containing student sessions using University clusters " +
        "Format \"SessionStartTimestamp, SessionEndTimestamp, StudentId, StudyId, StageId, MachineName\"")

    opt[String]('r', "recap-sessions").required().valueName("<file>")
      .action((x, c) => c.copy(recapPath = x))
      .text("recap-sessions is a required .csv file containing student sessions using the ReCap video lecture service " +
        "Format \"SessionStartTime, RecapId, StudentId, StudyId, StageId, SecondsListened\"")
  }
}


