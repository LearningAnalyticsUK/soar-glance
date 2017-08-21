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
package uk.ac.ncl.la.soar.glance.eval

import java.time.{Duration, Instant}
import java.time.temporal.TemporalAmount
import java.util.UUID

import uk.ac.ncl.la.soar.StudentNumber

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer

/**
  * ADT describing session types, describing student activities
  * TODO: Fix Time for JS compilation, sor
  */
sealed trait Session {
  def start: Instant
  def duration: Int
  def student: StudentNumber
}

case class ClusterSession(id: UUID,
                          start: Instant,
                          end: Instant,
                          machine: String,
                          student: StudentNumber) extends Session {

  //TODO: Investigate better handling of the time situation
  override val duration = Duration.between(start, end).getSeconds.toInt
}
case class RecapSession(id: UUID,
                        start: Instant,
                        student: StudentNumber,
                        duration: Int) extends Session

/**
  * Type representing a collection of sessions
  *
  * TODO: Refactor this to be part of the core packages with other Records types once ClusterSession and RecapSession
  * types stabilise in core
  */
case class SessionRecords[S <: Session](sessions: List[S], start: Instant, end: Instant, chunk: TemporalAmount) {


  //Build list of time chunks
  @tailrec
  private def constructChunks(chunks: List[(Instant,Instant)], cursor: Instant): List[(Instant,Instant)] = {
    if (cursor.isBefore(end)) {
      val dCursor = cursor.plus(chunk)
      constructChunks((cursor, dCursor) :: chunks, dCursor)
    } else chunks
  }

  //Assign sessions to time chunks
  val chunks = constructChunks(List.empty[(Instant, Instant)], start)

  //TODO: Construct more performant recursive version of this
  val chunkedSessions = chunks.map({ case c @ (cStart, cEnd) =>
    c -> sessions.filter(s => s.start.isAfter(cStart) && s.start.isBefore(cEnd))
  }).toMap

  val meanDurationPerChunk = chunkedSessions.mapValues{ sessions =>
    val totDuration = sessions.map(_.duration).sum
    val numStudents = sessions.groupBy(_.student).size
    totDuration.toDouble / numStudents
  }

  val perStudentDurationPerChunk: Map[StudentNumber, List[((Instant, Instant), Int)]] = ???



}

