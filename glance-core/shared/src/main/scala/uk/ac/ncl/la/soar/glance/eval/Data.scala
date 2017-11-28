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
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

/**
  * Sealed ADT which represents the various data types which may be required by visualisations in Glance
  *
  * Note that instances of `Data` are not data types themselves, so much as they describe meta-data like api endpoints
  */
sealed trait Data {

  /** The type of the Data itself */
  type T

  /** Implicit evidence that T is Encodable/Decodable */
  implicit def encoderT: Encoder[T]
  implicit def decoderT: Decoder[T]

  /** The api endpoint. As it is assumed that all this data will be survey specific, the full endpoint will be
    * something like `host:port/surveys/id/$endpoint`
    */
  def endpoint: String
}

case object RecapData extends Data {

  override type T = SessionSummary

  override implicit def encoderT: Encoder[SessionSummary] = Encoder[SessionSummary]
  override implicit def decoderT: Decoder[SessionSummary] = Decoder[SessionSummary]

  override def endpoint: String = "recap"
}

case object ClusterData extends Data {

  override type T = SessionSummary

  override implicit def encoderT: Encoder[SessionSummary] = Encoder[SessionSummary]
  override implicit def decoderT: Decoder[SessionSummary] = Decoder[SessionSummary]

  override def endpoint: String = "cluster"
}

//case object VLEData
//case object MeetingData
//case object MarksData
