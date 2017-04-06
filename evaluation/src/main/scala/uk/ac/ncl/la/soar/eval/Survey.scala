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


//TODO: Workout if we think the types below should share an ADT heirachy? Don't think we'll need pattern matching...

/**
  * Case class representing an unanswered survey which will be presented to members of staff to fill out.
  */
case class Survey(modules: Set[ModuleCode], queries: Map[StudentNumber, ModuleCode],
                  entries: List[StudentRecords[Map, ModuleCode, Double]])

/**
  * Case class representing an answered survey which has been submitted by a member of staff.
  */
case class SurveyResponse(survey: Survey, response: Map[StudentNumber, ModuleScore], respondent: String)
