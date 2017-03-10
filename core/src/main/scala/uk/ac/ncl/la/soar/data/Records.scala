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
package uk.ac.ncl.la.soar.data

import uk.ac.ncl.la.soar.{ModuleCode, Record, StudentNumber}
import cats._
import cats.implicits._
import Record._

/** ADT of types which form Records in our application
  *
  * @author hugofirth
  */
sealed trait Records[F[_, _], A, B] { self =>

  implicit def F: Record[F]
  implicit def A: Order[A]

  def record: F[A, B]

  /** Produce a CSV string from a record instance */
  def toCSV[M <: Records[F, A, B]](metaColumns: List[M => String], keyColumns: List[A], sep: String = ","): String = {
    val recMap = record.toMap
    val es = keyColumns.map { c => recMap.get(c).fold(" ")(_.toString) }
    (metaColumns.map(mf => mf(self.asInstanceOf[M])) ::: es).mkString(sep)
  }

}

/** Associates a student number with a map of student records, whose keys provide an ordering
  *
  * @author hugofirth
  */
final case class StudentRecords[F[_, _]](number: StudentNumber,
                                         record: F[ModuleCode, Double])
                                        (implicit val F: Record[F],
                                         val A: Order[ModuleCode]) extends Records[F, ModuleCode, Double]


/** Instances of this class contain the [[ModuleScore]]s for an entire cohort of students and a
  * given module.
  *
  * @author hugofirth
  */
final case class ModuleRecords[F[_, _]](module: ModuleCode, record: F[StudentNumber, Double])
                                      (implicit val F: Record[F],
                                       val A: Order[StudentNumber]) extends Records[F, StudentNumber, Double]

object Records {

  /** Typeclass instances for StudentRecord */
//  implicit def studentRecordShow[F[_, _]: Record, A, B: Order]: Show[Records[F, A, B]] =
//    new Show[Records[F, A, B]] {
//
//      override def show(f: Records[F, A, B]): String = {
//        val entries = f.record.map({ case (k,v) => s""""$k": $v%""" })
//          .intercalate(s",${sys.props("line.separator")}")
//
//        val (typeName, entryName, idName, id) = f match {
//          case StudentRecords(number, _) => ("Student", "modules", "number", number)
//          case ModuleRecords(code, _) => ("Module", "students", "code", code)
//        }
//
//        s"""
//           |$typeName {
//           |  $idName: $id,
//           |  $entryName: {
//           |    $entries
//           |  }
//           |}
//        """.stripMargin
//      }
//    }

}
