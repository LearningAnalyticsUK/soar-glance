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

import java.time.Instant

import kantan.csv.RowEncoder
import uk.ac.ncl.la.soar.data.ModuleScore
import uk.ac.ncl.la.soar.{ModuleCode, StudentNumber}

sealed trait CsvRow
sealed trait HasStudent { this: CsvRow =>
  def student: StudentNumber
}

object CsvRow {

  final case class NessMarkRow(student: StudentNumber, study: Int, stage: Int, year: String, progression: String, module: ModuleCode,
                         score: Int, component: String, componentAttempt: Int, componentScore: Int,
                         componentWeight: Double, due: Option[Instant]) extends CsvRow with HasStudent

  final case class ClusterSessionRow(start: String, end: String, student: StudentNumber, study: Int, stage: Int,
                               machine: String) extends CsvRow with HasStudent

  final case class RecapSessionRow(start: String, recap: String, student: StudentNumber, study: Int, stage: Int,
                             duration: Double) extends CsvRow with HasStudent

  implicit val moduleScoreEncoder: RowEncoder[ModuleScore] = RowEncoder.ordered { ms: ModuleScore =>
    (ms.student, ms.module, ms.score)
  }
}

