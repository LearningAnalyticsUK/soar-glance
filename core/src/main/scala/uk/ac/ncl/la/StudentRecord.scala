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
package uk.ac.ncl.la

import cats._
import cats.implicits._
import scala.collection.SortedMap

/** Associates a student number with a map of student records, whose keys provide an ordering
  *
  * @author hugofirth
  */
final case class StudentRecord[C : Ordering](number: StudentNumber, moduleRecords: SortedMap[C, Double]) {

  //TODO: Consider moving to eval module, is a pretty specialised method.
  def toCsv(columns: List[C], sep: Char = ','): String = {
    val modules = columns.map{ c =>
      moduleRecords.get(c).fold(" ")(_.toString)
    }
    val sepS = sep.toString
    s"$number$sepS${modules.mkString(sepS)}"
  }
}

object StudentRecord {

  /** Typeclass instances for StudentRecord */
  implicit def studentRecordShow[C : Ordering]: Show[StudentRecord[C]] = new Show[StudentRecord[C]] {

    override def show(f: StudentRecord[C]): String = {

      val modules = f.moduleRecords.iterator.map({ case (k,v) => s""""$k": $v%""" })
        .mkString(s",${sys.props("line.separator")}")

      s"""
         |Student {
         |  number: ${f.number},
         |  modules: {
         |    $modules
         |  }
         |}
       """.stripMargin
    }
  }
}


