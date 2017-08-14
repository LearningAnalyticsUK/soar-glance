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

import cats._
import cats.implicits._
import uk.ac.ncl.la.soar._
import uk.ac.ncl.la.soar.data._
import uk.ac.ncl.la.soar.Record._

import io.circe._
import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax._
import io.circe.generic.auto._
import io.circe.generic.semiauto._

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

object SurveyResponse {

  def apply(survey: Survey,
            ranks: IndexedSeq[StudentNumber],
            respondent: String,
            start: Instant,
            id: UUID,
            notes: String) = IncompleteResponse(survey, ranks, respondent, start, id, notes)

  /** Typeclass instances for SurveResponse */
  private implicit val encodeInstant: Encoder[Instant] = Encoder.encodeString.contramap[Instant](_.toString)

  private implicit val decodeInstant: Decoder[Instant] = Decoder.decodeString.emap { str =>
    Either.catchNonFatal(Instant.parse(str)).leftMap(t => "Instant")
  }

  implicit val encodeSurveyResponse: Encoder[SurveyResponse] = new Encoder[SurveyResponse] {
    final def apply(a: SurveyResponse): Json = Json.obj(
      "id" -> a.id.toString.asJson,
      "survey" -> a.survey.asJson,
      "ranks" -> a.ranks.asJson,
      "respondent" -> a.respondent.asJson,
      "start" -> a.start.asJson,
      "notes" -> a.notes.asJson
    )
  }

  implicit val decodeSurveyResponse: Decoder[SurveyResponse] = new Decoder[SurveyResponse] {

    override def apply(c: HCursor): Decoder.Result[SurveyResponse] = {
      for {
        id <- c.downField("id").as[String]
        d <- decodeIdLessResponse(c)
      } yield d(UUID.fromString(id))
    }
  }

  //TODO: Work out why we couldn't get auto/semiauto to work for us here?
  implicit val decodeIdLessResponse: Decoder[UUID => SurveyResponse] = new Decoder[UUID => SurveyResponse] {

    override def apply(c: HCursor): Decoder.Result[UUID => SurveyResponse] = {
      for {
        survey <- c.downField("survey").as[Survey]
        ranks <- c.downField("ranks").as[Vector[StudentNumber]]
        respondent <- c.downField("respondent").as[String]
        start <- c.downField("start").as[Instant]
        notes <- c.downField("notes").as[String]
      } yield { id: UUID =>
        IncompleteResponse(survey, ranks, respondent, start, id, notes)
      }
    }
  }
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