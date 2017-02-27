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

import scopt._
import uk.ac.ncl.la.soar.ModuleCode

/** Config "bag" case class for the survey generator and accompanying scopt parser
  *
  * @author hugofirth
  */
final case class GeneratorConfig(recordsPath: String = "", outputPath: String = "", elided: Int = 10,
                                 modules: Seq[String] = Seq.empty[String], common: Option[String] = None, seed: Int = 1921437)

/** Config "bag" case class for the survey evaluator and accompanying scopt parser.
  *
  * @author hugofirth
  */
final case class EvaluatorConfig(inputPath: String = "", outputPath: String = "", modelPath: String = "",
                                 metric: String = "rmse")

object Config {

  /** Package private helper object for parsing command line arguments, provided by scopt */
  private[eval] val generatorParser = new OptionParser[GeneratorConfig]("SoarEvalGen") {
    //Define the header for the command line display text
    head("Soar Evaluation Survey generator", "0.1.x")

    //Define the individual command line options
    opt[String]('i', "input").required().valueName("<file>")
      .action((x, c) => c.copy(recordsPath = x))
      .text("input is a required .csv file containing student/module scores. " +
        "Format \"StudentNumber, Module Code, Percentage\"")

    opt[String]('o', "output").required().valueName("<directory>")
      .action((x, c) => c.copy(outputPath = x))
      .text("output is a required parameter specifying the directory to write the surveys to.")

    opt[Int]('e', "elided").valueName("e.g. 20")
      .action((x, c) => c.copy(elided = x))
      .text("elided is an optional parameter specifying how many student records to partially elide in the generated " +
        "surveys.")

    opt[Seq[String]]('m', "modules").required().valueName("e.g. CSC1021, CSC2024...")
      .action((x, c) => c.copy(modules = x))
      .text("modules is the list of modules for which to elide a students records. Only one module record will be " +
        "elided per student. One survey is generated per elided module code.")

    opt[String]('c', "common").valueName("e.g. CSC2024")
      .action({ (x, c) =>
        val cmn = if(x == "") None else Option(x)
        c.copy(common = cmn)
      })
      .text("common is an optional parameter specifying an additional module to elide student records for in *all* " +
        "generated surveys.")

    opt[Int]('s', "seed").valueName("<int>")
      .action((x, c) => c.copy(seed = x))
      .text("seed is an optional parameter specifying a number to use as a seed when randomly selecting student " +
        "records to elide.")
  }
}

