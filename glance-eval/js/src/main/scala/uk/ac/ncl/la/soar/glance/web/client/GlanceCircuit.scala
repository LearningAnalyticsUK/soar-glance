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
package uk.ac.ncl.la.soar.glance.web.client

import java.util.UUID

import scala.concurrent.ExecutionContext.Implicits.global
import cats._
import cats.implicits._
import diode._
import diode.ActionHandler._
import diode.data._
import diode.util._
import diode.react.ReactConnector
import io.circe._
import uk.ac.ncl.la.soar.{ModuleCode, StudentNumber}
import uk.ac.ncl.la.soar.glance.eval.{SessionSummary, Survey, SurveyResponse}
import uk.ac.ncl.la.soar.glance.web.client.data.CohortAttainmentSummary
import japgolly.scalajs.react.extra.router.{RouterCtl, Action => RouterAction}

/**
  * Hierarchical definition of Application model, composing various other models.
  */
final case class GlanceModel(survey: Pot[SurveyModel])

/**
  * Container for the survey data (a `glance.Survey`) which is bound to various UI elements throughout the Glance
  * application. There may be other models containing other data, but this is the primary one.
  *
  * TODO: Work out if all elements have to be wrapped in Option. Understand response may be OK + empty, but does Pot not
  * have the ability to encode this? Otherwise it feels like we're almost creating an option of an option (Pot ~ Option)
  */
case class SurveyModel(survey: Survey,
                       attainmentSummary: CohortAttainmentSummary,
                       clusterSummary: SessionSummary,
                       recapSummary: SessionSummary)

/**
  * ADT representing the set of actions which may be taken to update a `SurveyModel`. These actions encapsulate no
  * behaviour. Instead the behaviour is defined in a handler/interpreter method provided in the `GlanceCircuit` object.
  */
sealed trait SurveyAction extends Action
final case class InitSurveys(surveyIds: Either[Error, List[UUID]]) extends SurveyAction
final case class InitSurvey(info: Either[Error, (Survey, SessionSummary, SessionSummary)]) extends SurveyAction
final case class SelectStudent(id: StudentNumber) extends SurveyAction
final case class SubmitSurveyResponse(response: SurveyResponse) extends SurveyAction
final case class RefreshSurvey(id: UUID) extends SurveyAction
case object RefreshSurveys extends SurveyAction
case object DoNothing extends SurveyAction

/**
  * Handles actions related to Surveys
  */
class SurveyHandler[M](modelRW: ModelRW[M, Pot[SurveyModel]]) extends ActionHandler(modelRW) {

  private def loadSurveyInfo(id: UUID) = Effect {
    val info = (ApiClient.loadSurveyT(id) |@|
      ApiClient.loadClustersT(id) |@|
      ApiClient.loadRecapsT(id)).map((_, _, _)).value

    info.map(i => InitSurvey(i))
  }

  override def handle = {
    case RefreshSurveys =>
      effectOnly(Effect(ApiClient.loadSurveyIds.map(s => InitSurveys(s))))
    case RefreshSurvey(id) =>
      effectOnly(loadSurveyInfo(id))
    case InitSurveys(decodedSurveyIds) =>
      decodedSurveyIds.fold(
        err => updated(Failed(err)),
        surveyIds => surveyIds.headOption.fold(updated(Empty))( id => effectOnly(loadSurveyInfo(id)) )
      )
    case InitSurvey(decodedInfo) =>
      decodedInfo.fold(
        err => updated(Failed(err)),
        { case (srv, cS, rS) =>
          updated(Ready(SurveyModel(srv, CohortAttainmentSummary(srv.entries), cS, rS)))
        }
      )
    case SubmitSurveyResponse(response) =>
      effectOnly(Effect(ApiClient.postResponse(response).map(_ => DoNothing)))
    case DoNothing => noChange
  }
}

/**
  * `GlanceCircuit` object provides an instance of the application's [[GlanceModel]], along with handlers for various
  * actions.
  */
object GlanceCircuit extends Circuit[GlanceModel] with ReactConnector[GlanceModel] {

  override protected def initialModel = GlanceModel(Empty)

  override protected def actionHandler: GlanceCircuit.HandlerFunction = composeHandlers(
    new SurveyHandler(zoomTo(_.survey))
  )



}
