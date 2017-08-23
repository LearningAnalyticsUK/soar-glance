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
import uk.ac.ncl.la.soar.glance.util.{Time, Times}

import scala.annotation.tailrec

/**
  * ADT describing session types, describing student activities
  * TODO: Fix Time for JS compilation
  */
sealed trait Session { self =>

  def start: Instant
  def duration: Int
  def student: StudentNumber

}

case class ClusterSession(start: Instant,
                          end: Instant,
                          machine: String,
                          student: StudentNumber,
                          id: UUID) extends Session {

  //TODO: Investigate better handling of the time situation
  override val duration = Duration.between(start, end).getSeconds.toInt
}
case class RecapSession(id: UUID,
                        start: Instant,
                        student: StudentNumber,
                        duration: Int) extends Session


object Session {

  def getSummary[S <: Session](sessions: List[S],
                               startInstant: Instant,
                               endInstant: Instant,
                               chunk: TemporalAmount): SessionSummary = new SessionSummary {


    import Times._

    //Build list of time chunks
    @tailrec
    private def constructChunks(chunks: List[(Instant,Instant)], cursor: Instant): List[(Instant,Instant)] = {
      if (cursor.isBefore(endInstant)) {
        val dCursor = cursor.plus(chunk)
        constructChunks((cursor, dCursor) :: chunks, dCursor)
      } else chunks
    }

    private val chunks = constructChunks(List.empty[(Instant, Instant)], startInstant)

    //TODO: Construct more performant recursive version of this
    def chunkSessions[A](sessions: List[S], process: List[S] => A) = chunks.map({ case c @ (cStart, cEnd) =>
      c -> process(sessions.filter(s => s.start.isAfter(cStart) && s.start.isBefore(cEnd)))
    }).toMap

    private val meanDurationPerChunk = chunkSessions(sessions, identity).mapValues{ ss =>
      val totDuration = ss.map(_.duration).sum
      val numStudents = ss.groupBy(_.student).size
      totDuration.toDouble / numStudents
    }

    private def instantKeysToTime[V](m: Map[(Instant, Instant), V]) = m.map { case ((s, e), v) =>
      (s.toTime, e.toTime) -> v.
    }

    private val perStudentDurationPerChunk: Map[StudentNumber, Map[(Time, Time), Double]] = {
      //Group sessions by student
      val byStudent: Map[StudentNumber, List[S]] = sessions.groupBy(_.student)

      //Chunk Sessions for each student and sum the duration
      byStudent.mapValues { stS =>
        val cS = chunkSessions(stS, ss => ss.map(_.duration.toDouble).sum)
        instantKeysToTime(cS)
      }
    }

    override def meanDuration: Map[(Time, Time), Double] = instantKeysToTime(meanDurationPerChunk)

    override def start: Time = startInstant.toTime

    override def studentDuration: Map[StudentNumber, Map[(Time, Time), Double]] = perStudentDurationPerChunk

    override def end: Time = endInstant.toTime
  }
}


