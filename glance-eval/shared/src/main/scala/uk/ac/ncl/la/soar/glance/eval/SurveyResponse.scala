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
import uk.ac.ncl.la.soar.glance.util.Time
import uk.ac.ncl.la.soar.glance.util.Times._
import uk.ac.ncl.la.soar.glance.web.client.component.sortable.IndexChange

/**
  * ADT representing an a survey which is completed by a member of staff.
  *
  * Note: Have to use Doubles for times because Javascript ... figure out a better solution to this. Longs better, Instants
  * preferable. Facade type on one side with implicit conversions?
  */
sealed trait SurveyResponse {
  def survey: Survey
  def simple: Ranking
  def detailed: Ranking
  def respondent: String
  def start: Double
  def id: UUID
}

/**
  * Simple structure representing Ranking info. Reified here because we store two sets of ranking info per survey
  * response.
  */
final case class Ranking(ranks: List[StudentNumber], rankHistory: List[(StudentNumber, IndexChange, Time)] = Nil)

object SurveyResponse {

  def apply(survey: Survey,
            simple: Ranking,
            detailed: Ranking,
            respondent: String,
            start: Double,
            id: UUID): SurveyResponse = IncompleteResponse(survey, simple, detailed, respondent, start, id)


  /* Typeclass instances for SurveResponse */
  implicit val encodeSurveyResponse: Encoder[SurveyResponse] = new Encoder[SurveyResponse] {
    final def apply(a: SurveyResponse): Json = Json.obj(
      "id" -> a.id.toString.asJson,
      "survey" -> a.survey.asJson,
      "simple" -> a.simple.asJson,
      "detailed" -> a.detailed.asJson,
      "respondent" -> a.respondent.asJson,
      "start" -> a.start.asJson,
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
        simple <- c.downField("simple").as[Ranking]
        detailed <- c.downField("detailed").as[Ranking]
        respondent <- c.downField("respondent").as[String]
        start <- c.downField("start").as[Double]
      } yield { id: UUID =>
        IncompleteResponse(survey, simple, detailed, respondent, start, id)
      }
    }
  }


}

case class IncompleteResponse(survey: Survey,
                              simple: Ranking,
                              detailed: Ranking,
                              respondent: String,
                              start: Double,
                              id: UUID) extends SurveyResponse

case class CompleteResponse(survey: Survey,
                            simple: Ranking,
                            detailed: Ranking,
                            respondent: String,
                            start: Double,
                            finish: Double,
                            id: UUID) extends SurveyResponse