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
import uk.ac.ncl.la.soar.glance.eval.{ClusterSession, RecapSession, Session, Survey}
import uk.ac.ncl.la.soar.server.Implicits._

/**
  * Class defines the REST api for Surveys
  */
class SurveysApi(surveyRepository: SurveyDb, clusterRepository: ClusterSessionDb, recapRepository: RecapSessionDb) {

  /** Endpoint returns all survey ids in response to `GET /surveys` */
  lazy val list = get("surveys") {
    surveyRepository.list.toFuture.map(ss => Ok(ss.map(_.id)))
  }

  /** Endpoint to retrieve a specific Survey by id */
  val read = get("surveys" :: path[UUID] ) { (id: UUID) =>
    println(s"received a request for this survey: $id")
    surveyRepository.find(id).toFuture.map {
      case Some(s) => Ok(Survey.truncateQueryRecords(s))
      case None => notFound(id)
    }
  }

  /** Endpoint to get cluster data for a specific survey by id */
  val readClusters = get("surveys" :: path[UUID] :: "cluster") { (id: UUID) =>
    val fetchSummary = for {
      surveyDates <- surveyRepository.findDateRange(id)
      sessions <- surveyDates match {
        case Some((start, end)) => clusterRepository.findBetween(start, end)
        case None => Task.now(List.empty[ClusterSession])
      }
    } yield {
      surveyDates.map { case (s, e) => Session.getSummary(sessions, s, e, Period.ofDays(7)) }
    }

    fetchSummary.toFuture.map {
      case Some(s) => Ok(s)
      case None => notFound(id)
    }
  }

  /** Endpoint to get recap data for a specific survey by id */
  val readRecap = get("surveys" :: path[UUID] :: "recap") { (id: UUID) =>
    val fetchSummary = for {
      surveyDates <- surveyRepository.findDateRange(id)
      sessions <- surveyDates match {
        case Some((start, end)) => recapRepository.findBetween(start, end)
        case None => Task.now(List.empty[RecapSession])
      }
    } yield {
      surveyDates.map { case (s, e) => Session.getSummary(sessions, s, e, Period.ofDays(7)) }
    }

    fetchSummary.toFuture.map {
      case Some(s) => Ok(s)
      case None => notFound(id)
    }
  }

  /** Pre-flight endpoint for CORS headers */
  //Clunky Unit syntax - fix?
  val preflight = options(*) { Ok(()) }

  /** All endpoints in this API, enriched with CORS headers. */
  val endpoints = (list :+: read :+: readClusters :+: readRecap :+: preflight).withCorsHeaders

  /** API endpoints exposed as a service */
  val service = endpoints.toService

  /** Response when a survey with the given id is not found in the database */
  private[server] def notFound(id: UUID) = NotFound(new RuntimeException(s"Survey not found with id:$id"))

}
