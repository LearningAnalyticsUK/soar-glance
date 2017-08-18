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

import java.time._
import java.util.UUID

import uk.ac.ncl.la.soar.{ModuleCode, StudentNumber}

/**
  * ADT describing student actions according to the student actions grammar
  * [[https://github.com/NewcastleComputingScience/student-outcome-accelerator/wiki/Student-Actions]]
  */
sealed trait Action

case class Printed(printer: Int,
                   location: String,
                   time: Instant,
                   student: StudentNumber,
                   sides: Int) extends Action

case class AttendedPractical(clusterUse: UsedCluster,
                             scheduled: (ModuleCode, Instant, Duration)) extends Action

case class UsedCluster(startTime: Instant,
                       duration: Duration,
                       student: StudentNumber,
                       machine: String,
                       location: String) extends Action

case class WatchedRecap(recapId: UUID,
                        moduleCode: ModuleCode,
                        startTime: Instant,
                        duration: Duration) extends Action

case class UsedBlackboard(a: Int) extends Action
case class CompletedWork(a: Int) extends Action
case class MetTutor(a: Int) extends Action

/** These grammar terms are less important initially */
case class Progressed(code: String) extends Action
case class CompletedModule(code: ModuleCode, progression: Progressed) extends Action
case class AttendedLecture(a: Int) extends Action

