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
package uk.ac.ncl.la.soar.glance.cli

import org.apache.log4j.{LogManager, Level}
import cats._
import cats.implicits._

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
    val conf = CommandConfig(args).toRight {
      throw new IllegalArgumentException("Failed to parse command line arguments! " +
        "Format: ./submit.sh [command] --options")
    }

    //TODO: Fix the horrible pattern match anon function below. Uses type annotations....
    conf.flatMap {
      case a: GeneratorConfig => Generator.run(a).unsafeAttemptRun()
      case a: AssessorConfig => Assessor.run(a).unsafeAttemptRun()
    } match {
      case Left(e) =>
        //In the event of an error, log and crash out.
        System.err.println(e.toString)
        sys.exit(1)
      case Right(_) =>
        //In the event of a successful job, log and finish
        println("Job finished.")
    }
  }

}

