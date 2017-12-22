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
import japgolly.scalajs.react.CallbackTo
import uk.ac.ncl.la.soar.{ModuleCode, StudentNumber}
import uk.ac.ncl.la.soar.glance.eval._
import uk.ac.ncl.la.soar.glance.web.client.data.CohortAttainmentSummary
import japgolly.scalajs.react.extra.router.{RouterCtl, Action => RouterAction}
import uk.ac.ncl.la.soar.data.{Module, StudentRecords}
import uk.ac.ncl.la.soar.glance.util.{Time, Times}
import uk.ac.ncl.la.soar.glance.web.client.component.sortable.IndexChange

import scala.scalajs.js.Date
import scala.collection.immutable.SortedMap
import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Hierarchical definition of Application model, composing various other models.
  */
final case class GlanceModel(survey: Pot[SurveyModel], collection: Pot[Collection])

/** Struct which contains the data depended on by a survey's */
case class SurveyData(cohort: CohortAttainmentSummary,
                      recap: SessionSummary,
                      cluster: SessionSummary)

/**
  * Container for the survey data (a `glance.Survey`) which is bound to various UI elements throughout the Glance
  * application. There may be other models containing other data, but this is the primary one.
  */
case class SurveyModel(survey: Survey,
                       data: SurveyData,
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
final case class RefreshSurveyWithEffect(id: UUID, eff: Effect) extends SurveyAction
case object ProgressSimpleSurvey extends SurveyAction
case object RefreshSurveys extends SurveyAction

sealed trait RankingAction extends Action
final case class ChangeRanks(newRanks: List[StudentNumber], change: IndexChange)
    extends RankingAction

sealed trait CollectionAction extends Action
final case class LoadCollection(id: UUID, idx: Int = 0) extends CollectionAction
final case class InitCollection(coll: Either[Error, Collection], idx: Int) extends CollectionAction
final case class NextCollectionSurvey(redirect: CallbackTo[Unit]) extends CollectionAction
final case class LoadCollectionSurvey(coll: Collection, idx: Int) extends CollectionAction

/**
  * Handles actions related to Surveys
  */
class SurveyHandler[M](modelRW: ModelRW[M, Pot[SurveyModel]]) extends ActionHandler(modelRW) {

  //For the moment this method should load all endpoints required by any possible set of visualisations. Yes this is
  // Wasteful, but actually getting the visualisations to dictate what data they want without throwing away all code
  // structure or type safety is worse, so for now we load too much. In theory the endpoints will be empty anyway.
  //TODO: Try again to get the selective data loading working
  private def loadSurveyInfo(id: UUID) = Effect {
    val info = (ApiClient.loadSurveyT(id),
                ApiClient.loadClustersT(id),
                ApiClient.loadRecapsT(id),
                ApiClient.loadModulesT).map4((_, _, _, _))

    val preppedInfo = info.map {
      case (s, c, r, ms) => (s, c, r, ms.iterator.map(m => m.code -> m).toMap)
    }

    preppedInfo.value.map(i => InitSurvey(i))
  }

  override def handle = {
    case RefreshSurveys =>
      effectOnly(Effect(ApiClient.loadSurveyIds.map(s => InitSurveys(s))))

    case RefreshSurvey(id) =>
      println(s"[INFO] - Submitting rest requests for survey $id")
      effectOnly(loadSurveyInfo(id))

    case RefreshSurveyWithEffect(id, eff) =>
      println(s"[INFO] - Submitting rest requests for survey $id")
      effectOnly(loadSurveyInfo(id) >> eff)

    case InitSurveys(decodedSurveyIds) =>
      decodedSurveyIds.fold(
        err => updated(Failed(err)),
        surveyIds =>
          surveyIds.headOption.fold(updated(Empty))(id => effectOnly(loadSurveyInfo(id))))

    case InitSurvey(decodedInfo) =>
      decodedInfo.fold(
        err => updated(Failed(err)), {
          case (srv, cS, rS, ms) =>
            val data = SurveyData(CohortAttainmentSummary(srv.entries), cS, rS)
            val sm = SurveyModel(srv, data, ms, Ranking(srv.queries), Ranking(srv.queries))
            updated(Ready(sm))
        }
      )

    case SubmitSurveyResponse(response) =>
      effectOnly(Effect(ApiClient.postResponse(response).map(_ => NoAction)))

    case ProgressSimpleSurvey =>
      updated(value.map(_.copy(detailedStage = true)))
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
        m.copy(ranks = ranks,
               rankHistory = (ranks(change.newIndex), change, Times.now) :: m.rankHistory)
      })
  }
}

/**
  * Handles actions related to Collections
  */
class CollectionHandler[M](modelRW: ModelRW[M, Pot[Collection]]) extends ActionHandler(modelRW) {

  private def isCached(pc: Pot[Collection], id: UUID) = pc.fold(false)(_.id == id)

  override protected def handle = {
    case LoadCollection(id, idx) =>
      if (isCached(modelRW.value, id)) {
        value.fold(noChange)(c => effectOnly(Effect.action(LoadCollectionSurvey(c, idx))))
      } else {
        effectOnly(Effect(ApiClient.loadCollection(id).map(c => InitCollection(c, idx))))
      }

    case InitCollection(decodedCollection, idx) =>
      decodedCollection match {
        case Left(err) =>
          println(s"[WARN] - failed to decode collection: $err")
          updated(Failed(err))
        case Right(coll) =>
          effectOnly(Effect.action(LoadCollectionSurvey(coll, idx)))
      }

    case NextCollectionSurvey(redir) =>
      println(s"[INFO] - Received Action to load next survey")
      value match {
        case Ready(c) if c.currentIsLast => noChange
        case Ready(c) =>
          val nextIdx = c.currentIdx + 1
          println(s"[INFO] - Next survey index is $nextIdx")
          //TODO: Fix this horrible mess
          val surveyId = c.surveyIds.get(nextIdx).getOrElse(c.surveyIds.head)
          val redirEffect = Effect(redir.toFuture.map(_ => NoAction))
          val eff = Effect.action(RefreshSurveyWithEffect(surveyId, redirEffect))
          val newColl = c.copy(currentIdx = nextIdx)
          println(
            s"[INFO] - Loading survey (id: $surveyId) from collection " +
              s"(id: ${c.id}), with index $nextIdx.")
          updated(Ready(newColl), eff)
        case _ => noChange
      }

    case LoadCollectionSurvey(c, idx) =>
      val surveyId = c.surveyIds.get(idx).getOrElse(c.surveyIds.head)

      val newColl = c.copy(currentIdx = idx)
      println(
        s"[INFO] - Loading survey (id: $surveyId) from collection " +
          s"(id: ${c.id}), with index $idx.")
      updated(Ready(newColl), Effect.action(RefreshSurvey(surveyId)))
  }

}

/**
  * `GlanceCircuit` object provides an instance of the application's [[GlanceModel]], along with handlers for various
  * actions.
  */
object GlanceCircuit extends Circuit[GlanceModel] with ReactConnector[GlanceModel] {

  override protected def initialModel = GlanceModel(Empty, Empty)

  private val collectionRW = zoomTo(_.collection)
  private val surveyRW = zoomTo(_.survey)
  //TODO: Figure out if this RW is an absolute nightmare.
  private val rankRW = {
    val reader = { (s: SurveyModel) =>
      if (s.detailedStage) s.detailedRanking else s.simpleRanking
    }
    val writer = { (m: GlanceModel, r: Pot[Ranking]) =>
      val newSurvey = for {
        survey <- m.survey
        newRankings <- r
      } yield {
        if (survey.detailedStage)
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
    new CollectionHandler(collectionRW),
    new SurveyHandler(surveyRW),
    new RankingHandler(rankRW)
  )
}
