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
import uk.ac.ncl.la.soar.glance.Survey

import scala.collection.immutable.SortedMap

/**
  * Container for the survey data (a `glance.Survey`) which is bound to various UI elements throughout the Glance
  * application. There may be other models containing other data, but this is the primary one.
  */
case class SurveyModel(survey: Option[Survey], selected: Option[StudentNumber])

/**
  * ADT representing the set of actions which may be taken to update a `SurveyModel`. These actions encapsulate no
  * behaviour. Instead the behaviour is defined in a handler/interpreter method provided in the `GlanceCircuit` object.
  */
sealed trait SurveyAction
final case class InitSurvey(survey: Survey) extends SurveyAction
final case class SelectStudent(id: StudentNumber) extends SurveyAction
case object ResetSurvey extends SurveyAction

object SurveyAction
