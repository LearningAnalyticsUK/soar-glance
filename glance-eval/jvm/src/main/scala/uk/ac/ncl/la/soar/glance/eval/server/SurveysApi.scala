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
package uk.ac.ncl.la.soar.glance.eval.server

import java.time.Period
import java.util.UUID

import cats.data.OptionT
import io.circe.generic.auto._
import io.finch._
import io.finch.circe._
import monix.cats._
import monix.eval.Task
import uk.ac.ncl.la.soar.glance.eval.db.{ClusterSessionDb, CollectionDb, RecapSessionDb, SurveyDb}
import uk.ac.ncl.la.soar.glance.eval.{ClusterSession, RecapSession, Session, Survey}
import uk.ac.ncl.la.soar.server.Implicits._

/**
  * Class defines the REST api for Surveys
  */
class SurveysApi(surveyRepository: SurveyDb,
                 collectionRepository: CollectionDb,
                 clusterRepository: ClusterSessionDb,
                 recapRepository: RecapSessionDb) {

  /** Endpoint returns the the first survey in a collection in response to `GET /collections/id` */
  val collectionFirst = get("collections" :: path[UUID]) { (id: UUID) =>
    //TODO: Switch this from Option to Either or Validated so that we can actually have a purpose build notfound message
    //  At the moment nominally a collection id could exist but a survey id could not. Tbf this does indicate an issue
    //  on our end rather than user error.
    collectionRepository.find(id).toFuture.map {
      case Some(c) => Ok(c)
      case None    => genNotFound("Collection", id)
    }
  }

  //collection/id/index - return indexed survey in collection, if exists
  //TODO: Work out if this endpoint is actually needed on the backend - its never called.
  /** Endpoint returns the survey in a collection at a given index in response to `GET/collections/id/idx` */
  val collectionIdx = get("collections" :: path[UUID] :: path[Int]) { (id: UUID, idx: Int) =>
    collectionRepository.findIdx(id, idx).toFuture.map {
      case Some(s) => Ok(Survey.truncateQueryRecords(s))
      case None    => genNotFound("Collection", id)
    }
  }

  /** Endpoint returns all survey ids in response to `GET /surveys` */
  val list = get("surveys") {
    surveyRepository.list.toFuture.map(ss => Ok(ss.map(_.id)))
  }

  /** Endpoint to retrieve a specific Survey by id */
  val read = get("surveys" :: path[UUID]) { (id: UUID) =>
    println(s"received a request for this survey: $id")
    surveyRepository.find(id).toFuture.map {
      case Some(s) => Ok(Survey.truncateQueryRecords(s))
      case None    => notFound(id)
    }
  }

  /** Endpoint to get cluster data for a specific survey by id */
  val readClusters = get("surveys" :: path[UUID] :: "cluster") { (id: UUID) =>
    val fetchSummary = for {
      surveyDates <- surveyRepository.findDateRange(id)
      sessions <- surveyDates match {
        case Some((start, end)) => clusterRepository.findBetween(start, end)
        case None               => Task.now(List.empty[ClusterSession])
      }
    } yield {
      surveyDates.map {
        case (s, e) => Session.getSummary(sessions, s, e, Period.ofDays(7))
      }
    }

    fetchSummary.toFuture.map {
      case Some(s) => Ok(s)
      case None    => notFound(id)
    }
  }

  /** Endpoint to get recap data for a specific survey by id */
  val readRecap = get("surveys" :: path[UUID] :: "recap") { (id: UUID) =>
    val fetchSummary = for {
      surveyDates <- surveyRepository.findDateRange(id)
      sessions <- surveyDates match {
        case Some((start, end)) => recapRepository.findBetween(start, end)
        case None               => Task.now(List.empty[RecapSession])
      }
    } yield {
      surveyDates.map {
        case (s, e) => Session.getSummary(sessions, s, e, Period.ofDays(7))
      }
    }

    fetchSummary.toFuture.map {
      case Some(s) => Ok(s)
      case None    => notFound(id)
    }
  }

  /** Pre-flight endpoint for CORS headers */
  //Clunky Unit syntax - fix?
  val preflight = options(*) { Ok(()) }

  /** All endpoints in this API, enriched with CORS headers. */
  val endpoints =
    (list :+: read :+: readClusters :+: readRecap :+: collectionFirst :+: collectionIdx :+: preflight).withCorsHeaders

  /** API endpoints exposed as a service */
  val service = endpoints.toService

  /** Response when a survey with the given id is not found in the database */
  private[server] def notFound(id: UUID) = genNotFound("Survey", id)

  private[server] def genNotFound(name: String, id: UUID) =
    NotFound(new RuntimeException(s"$name not found with id:$id"))

}
