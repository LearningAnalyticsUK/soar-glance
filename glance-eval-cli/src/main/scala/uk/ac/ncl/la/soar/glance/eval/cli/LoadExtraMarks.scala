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

import monix.eval.Task
import uk.ac.ncl.la.soar.data.ModuleScore
import uk.ac.ncl.la.soar.glance.eval.server.Repositories

import scala.io.Source

/** Description of Class
  *
  * @author hugofirth
  */
object LoadExtraMarks extends Command[LoadExtraMarksConfig, Unit] {

  override def run(conf: LoadExtraMarksConfig) = {
    for{
      r <- parseMarks(conf.extraMarks)
      db <- Repositories.ModuleScore
      _ <- db.saveBatch(r)
    } yield ()
  }


  /** Retrieve and parse all markRows */
  private def parseMarks(marksPath: String): Task[List[ModuleScore]] = Task {
    //Read in ModuleScore CSV
    val lines = Source.fromFile(marksPath).getLines()
    //In order to groupBy the current naive implementation requires sufficient memory to hold all ModuleScores
    val lineList = lines.toList
    ModuleScore.parse(lineList.iterator, ',').toList
  }
}
