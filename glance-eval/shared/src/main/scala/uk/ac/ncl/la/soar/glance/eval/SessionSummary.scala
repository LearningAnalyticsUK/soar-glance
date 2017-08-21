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

import io.circe._
import io.circe.syntax._
import uk.ac.ncl.la.soar.StudentNumber
import uk.ac.ncl.la.soar.glance.util.Time

trait SessionSummary {

  def start: Time
  def end: Time
  def meanDuration: Map[(Time, Time), Double]
  def studentDuration: Map[StudentNumber, Map[(Time, Time), Double]]

}

object SessionSummary {

  import Time._

  /** Typeclass instances */
  implicit val encodeSessionSummary: Encoder[SessionSummary] = new Encoder[SessionSummary] {
    final def apply(a: SessionSummary): Json = Json.obj(
      "start" -> a.start.asJson,
      "end" -> a.end.asJson,
      "meanDuration" -> a.meanDuration.asJson,
      "studentDuration" -> a.studentDuration.asJson
    )
  }

  implicit val decodeSessionSummary: Decoder[SessionSummary] = new Decoder[SessionSummary] {
    override def apply(c: HCursor): Decoder.Result[SessionSummary] = {
      for {
        start <- c.downField("start").as[Time]
        end <- c.downField("end").as[Time]
        meanDuration <- c.downField("meanDuration").as[Map[(Time, Time), Double]]
        studentDuration <- c.downField("studentDuration").as[Map[StudentNumber, Map[(Time, Time), Double]]]
      } yield {
        new SessionSummary {

          override def meanDuration: Map[(Time, Time), Double] = meanDuration

          override def start: Time = start

          override def studentDuration: Map[StudentNumber, Map[(Time, Time), Double]] = studentDuration

          override def end: Time = end
        }
      }
    }
  }
}
