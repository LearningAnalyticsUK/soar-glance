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

/**
  * Job which reads in the completed survey csvs created earlier using [[Generator]].
  */ 
object Assessor extends Job {

  def run(args: Array[String]): Either[Throwable, Unit] = {
    //Create a config object from the command line arguments provided
    //TODO: Add string apply to Config object to pick correct parser and parse based on head argument
    //Either that or look into scopt commands
    val parseCli = Config.generatorParser.parse(args, GeneratorConfig()).fold {
      Left(new IllegalArgumentException("Failed to correctly parse command line arguments!")): Either[Throwable, GeneratorConfig]
    } { Right(_) }

    for {
      conf <- parseCli
    } yield ()
  }
}

