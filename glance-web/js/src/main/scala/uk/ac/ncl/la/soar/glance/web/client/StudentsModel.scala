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
package uk.ac.ncl.la.soar.glance.web.client

import diode.ActionHandler
import diode._
import uk.ac.ncl.la.soar.{ModuleCode, StudentNumber}
import uk.ac.ncl.la.soar.data.StudentRecords

import scala.collection.immutable.SortedMap

/**
  * Container for the student data (a `Seq[StudentRecords[SortedMap, ModuleCode, Double]\]`) which is bound to various
  * UI elements throughout the Glance application. There may be other models containing other data, but this is the
  * primary one.
  */
case class StudentsModel(students: Seq[StudentRecords[SortedMap, ModuleCode, Double]], selected: Seq[StudentNumber])

/**
  * ADT representing the set of actions which may be taken to update a `StudentsModel`. These actions encapsulate no
  * behaviour. Instead the behaviour is defined in a handler/interpreter method provided in the `GlanceCircuit` object.
  */
sealed trait StudentsAction
final case class InitStudents(students: Seq[StudentRecords[SortedMap, ModuleCode, Double]]) extends StudentsAction
final case class SelectStudents(ids: Seq[StudentNumber]) extends StudentsAction
final case class AddSelectedStudent(id: StudentNumber) extends StudentsAction
case object ResetStudents extends StudentsAction

object StudentsAction
