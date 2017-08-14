/** Default (Template) Project
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
package uk.ac.ncl.la.soar.glance.eval

import java.time.Instant
import java.util.UUID

import uk.ac.ncl.la.soar.StudentNumber

/**
  * ADT representing an a survey which is completed by a member of staff.
  */
sealed trait SurveyResponse {
  def survey: Survey
  def ranks: IndexedSeq[StudentNumber]
  def respondent: String
  def start: Instant
  def id: UUID
  def notes: String
}

case class IncompleteResponse(survey: Survey,
                              ranks: IndexedSeq[StudentNumber],
                              respondent: String,
                              start: Instant,
                              id: UUID,
                              notes: String) extends SurveyResponse

case class CompleteResponse(survey: Survey,
                            ranks: IndexedSeq[StudentNumber],
                            respondent: String,
                            start: Instant,
                            finish: Instant,
                            id: UUID,
                            notes: String) extends SurveyResponse