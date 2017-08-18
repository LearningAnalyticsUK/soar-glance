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

import org.apache.log4j.{Level, LogManager}
import cats._
import cats.implicits._
import monix.eval.Task
import monix.cats._
import monix.execution.Scheduler.Implicits.global
import scala.util.{Failure, Success}

/** Entry point to the Cli version of the Evaluation program
  *
  * @author hugofirth
  */
object Main {

  def main(args: Array[String]): Unit = {
    //Set up the logger
    val log = LogManager.getRootLogger
    log.setLevel(Level.WARN)

    //Bring in Args - pass to Config factory
    val conf = CommandConfig(args).fold(
      Task.raiseError[CommandConfig](
        new IllegalArgumentException("Failed to parse command line arguments! " +
          "Format: ./submit.sh [command] --options"))
    )(Task.now)

    //TODO: Fix the horrible pattern match anon function below. Uses type annotations....
    conf.flatMap {
      case a: GenerateConfig => GenerateSurveys.run(a)
      case a: AssessConfig => AssessSurveyResponses.run(a)
      case a: TansformConfig => TransformData.run(a)
      case a: LoadSupportConfig => Task.unit
    } runOnComplete {
      case Failure(e) =>
        //In the event of an error, log and crash out.
        System.err.println(e.toString)
        sys.exit(1)
      case Success(_) =>
        //In the event of a successful job, log and finish
        println("Job finished.")
    }
    ()
  }

}

