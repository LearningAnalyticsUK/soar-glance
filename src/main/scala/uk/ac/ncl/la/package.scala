/** student-attainment-predictor
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
package uk.ac.ncl

import scopt._

/** Package object containing command line options and the parser object
  *
  * @author hugofirth
  */
package object la {

  /** Config bag */
  case class Config(recordsPath: String = "", outputPath: String = "", rank: Int = 10, iter: Int = 5,
                    lambda: Double = 0.01, alpha: Double = 1.0, master: String = "local[2]", exMem: String = "4g")

  /** Package private helper object for parsing command line arguments, provided by scopt */
  private[la] val parser = new OptionParser[Config]("ScorePredictor") {
    //Define the header for the command line display text
    head("Score Predictor", "0.1.x")

    //Define the individual command line options
    opt[String]('i', "input").required().valueName("<file>")
      .action( (x, c) => c.copy(recordsPath = x) )
        .text("input is a required .csv file containing student/module scores. " +
          "Format \"StudentNumber, Module Code, Percentage\"")

    opt[String]('o', "output").required().valueName("<directory>")
      .action( (x, c) => c.copy(outputPath = x) )
      .text("output is a required parameter specifying the directory to write model output.")

    opt[Int]("rank").valueName("[int]")
      .action( (x, c) => c.copy(rank = x) )
      .text("rank is an optional tuning parameter for tuning the ALS predictive model.")

    opt[Int]("iter").valueName("[int]")
      .action( (x, c) => c.copy(iter = x) )
      .text("iter is an optional tuning parameter for tuning the ALS predictive model.")

    opt[Double]("lambda").valueName("[double]")
      .action( (x, c) => c.copy(lambda = x) )
      .text("lambda is an optional tuning parameter for tuning the ALS predictive model.")

    opt[String]("master").valueName("[string]")
      .action( (x, c) => c.copy(master = x) )
      .text("master is an optional parameter for configuring the application SparkContext.")

    opt[String]("executor").valueName("[string]")
      .action( (x, c) => c.copy(exMem = x) )
      .text("executor is an optional parameter for configuring the application SparkContext.")
  }

}
