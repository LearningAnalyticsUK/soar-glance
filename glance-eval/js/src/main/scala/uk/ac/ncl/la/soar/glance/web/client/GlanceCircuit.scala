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
import uk.ac.ncl.la.soar.glance.eval.{Ranking, SessionSummary, Survey, SurveyResponse}
import uk.ac.ncl.la.soar.glance.web.client.data.CohortAttainmentSummary
import japgolly.scalajs.react.extra.router.{RouterCtl, Action => RouterAction}
import uk.ac.ncl.la.soar.data.{Module, StudentRecords}
import uk.ac.ncl.la.soar.glance.util.{Time, Times}
import uk.ac.ncl.la.soar.glance.web.client.component.sortable.IndexChange

import scala.scalajs.js.Date
import scala.collection.immutable.SortedMap

/**
  * Hierarchical definition of Application model, composing various other models.
  */
final case class GlanceModel(survey: Pot[SurveyModel])

/**
  * Container for the survey data (a `glance.Survey`) which is bound to various UI elements throughout the Glance
  * application. There may be other models containing other data, but this is the primary one.
  */
case class SurveyModel(survey: Survey,
                       attainmentSummary: CohortAttainmentSummary,
                       clusterSummary: SessionSummary,
                       recapSummary: SessionSummary,
                       modules: Map[ModuleCode, Module],
                       simpleRanking: Ranking,
                       detailedRanking: Ranking,
                       startTime: Double = Date.now,
                       detailedStage: Boolean = false)


object SurveyModel {
  type Info = (Survey, SessionSummary, SessionSummary, Map[ModuleCode, Module])
}


/**
  * ADT representing the set of actions which may be taken to update a `SurveyModel`. These actions encapsulate no
  * behaviour. Instead the behaviour is defined in a handler/interpreter method provided in the `GlanceCircuit` object.
  */
sealed trait SurveyAction extends Action
final case class InitSurveys(surveyIds: Either[Error, List[UUID]]) extends SurveyAction
final case class InitSurvey(info: Either[Error, SurveyModel.Info]) extends SurveyAction
final case class SelectStudent(id: StudentNumber) extends SurveyAction
final case class SubmitSurveyResponse(response: SurveyResponse) extends SurveyAction
final case class RefreshSurvey(id: UUID) extends SurveyAction
case object ProgressSimpleSurvey extends SurveyAction
case object RefreshSurveys extends SurveyAction
case object DoNothing extends SurveyAction

sealed trait RankingAction extends Action
final case class ChangeRanks(newRanks: List[StudentNumber], change: IndexChange) extends RankingAction



/**
  * Handles actions related to Surveys
  */
class SurveyHandler[M](modelRW: ModelRW[M, Pot[SurveyModel]]) extends ActionHandler(modelRW) {

  private def loadSurveyInfo(id: UUID) = Effect {
    val info = (ApiClient.loadSurveyT(id),
      ApiClient.loadClustersT(id),
      ApiClient.loadRecapsT(id),
      ApiClient.loadModulesT).map4((_, _, _, _))

    val preppedInfo = info.map { case (s, c, r, ms) => (s, c, r, ms.iterator.map(m => m.code -> m).toMap) }

    preppedInfo.value.map(i => InitSurvey(i))
  }


  override def handle = {
    case RefreshSurveys =>
      effectOnly(Effect(ApiClient.loadSurveyIds.map(s => InitSurveys(s))))

    case RefreshSurvey(id) =>
      effectOnly(loadSurveyInfo(id))

    case InitSurveys(decodedSurveyIds) =>
      decodedSurveyIds.fold(
        err => updated(Failed(err)),
        surveyIds => surveyIds.headOption.fold(updated(Empty))( id => effectOnly(loadSurveyInfo(id)) ))

    case InitSurvey(decodedInfo) =>
      decodedInfo.fold(
        err => updated(Failed(err)),
        { case (srv, cS, rS, ms) =>
          val sm = SurveyModel(srv, CohortAttainmentSummary(srv.entries), cS, rS, ms, Ranking(srv.queries),
            Ranking(srv.queries))
          updated(Ready(sm))
        })

    case SubmitSurveyResponse(response) =>
      effectOnly(Effect(ApiClient.postResponse(response).map(_ => DoNothing)))

    case ProgressSimpleSurvey =>
      updated(modelRW.value.map(_.copy(detailedStage = true)))
  }
}

/**
  * Handles actions related to Rankings
  */
class RankingHandler[M](modelRW: ModelRW[M, Pot[Ranking]]) extends ActionHandler(modelRW) {
  override protected def handle = {
    case ChangeRanks(ranks, change) =>
      //TODO: Make the ranks/select more efficient and safer at some point (IndexedSeq and applyOrElse)
      updated(value.map { m =>
        m.copy(ranks = ranks, rankHistory = (ranks(change.newIndex), change, Times.now) :: m.rankHistory)
      })
  }
}

/**
  * `GlanceCircuit` object provides an instance of the application's [[GlanceModel]], along with handlers for various
  * actions.
  */
object GlanceCircuit extends Circuit[GlanceModel] with ReactConnector[GlanceModel] {

  override protected def initialModel = GlanceModel(Empty)

  private val surveyRW = zoomTo(_.survey)
  //TODO: Figure out if this RW is an absolute nightmare.
  private val rankRW = {
    val reader = { (s: SurveyModel) => if(s.detailedStage) s.detailedRanking else s.simpleRanking }
    val writer = { (m: GlanceModel, r: Pot[Ranking]) =>

      val newSurvey = for {
        survey <- m.survey
        newRankings <- r
      } yield {
        if(survey.detailedStage)
          survey.copy(detailedRanking = newRankings)
        else
          survey.copy(simpleRanking = newRankings)
      }
      m.copy(survey = newSurvey)
    }
    zoomMapRW(_.survey)(reader)(writer)
  }

  //TODO: Look into zoom vs zoomMapRW
  override protected def actionHandler: GlanceCircuit.HandlerFunction = composeHandlers(
    new SurveyHandler(surveyRW),
    new RankingHandler(rankRW)
  )



}
