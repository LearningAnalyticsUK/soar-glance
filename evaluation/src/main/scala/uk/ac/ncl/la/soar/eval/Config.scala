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

/** Config "bag" case class and accompanying scopt parser
  *
  * @author hugofirth
  */
final case class Config(recordsPath: String = "", outputPath: String = "", elided: Int = 10, numSurveys: Int = 0,
                        fixed: ModuleCode = 0, specialized: Seq[ModuleCode] = Seq.empty[ModuleCode], seed: Int = 1921437)

object Config {

  /** Package private helper object for parsing command line arguments, provided by scopt */
  private[eval] val parser = new OptionParser[Config]("SoarEval") {
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

  }
}

