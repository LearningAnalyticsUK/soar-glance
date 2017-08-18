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

/**
  * Simple struct type representing a programme of study
  */
sealed trait StudyType
case object PG extends StudyType
case object UG extends StudyType

case class Study(id: Int, code: String, title: String, studyType: StudyType, school: School)

/**
  * Simple struct type representing university school
  */
case class School(code: String, title: String, facultyCode: String)

/**
  * Simple struct type representing the stage of a student's study
  */
case class Stage(id: Int, number: Int, description: String)
