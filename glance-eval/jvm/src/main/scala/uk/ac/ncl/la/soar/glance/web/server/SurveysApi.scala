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
package uk.ac.ncl.la.soar.glance.web.server

import java.util.UUID

import io.circe.generic.auto._
import io.finch._
import io.finch.circe._
import Util._
import uk.ac.ncl.la.soar.glance.SurveyDb

/**
  * Class defines the REST api for Surveys
  */
class SurveysApi(repository: SurveyDb) {

  /** Endpoint returns all surveys in response to `GET /surveys` */
  lazy val list = get("surveys") {
    repository.list.toFuture.map(Ok(_))
  }

  /** Endpoint to retrieve a specific Survey by id */
  val read = get("surveys" :: path[UUID] ) { (id: UUID) =>
    println(s"received a request for this survey: $id")
    repository.find(id).toFuture.map {
      case Some(s) => Ok(s)
      case None => notFound(id)
    }
  }

  /** Pre-flight endpoint for CORS headers */
  //Clunky Unit syntax - fix?
  val preflight = options(*) { Ok(()) }

  /** All endpoints in this API, enriched with CORS headers. */
  val endpoints = (list :+: read :+: preflight).withCorsHeaders

  /** API endpoints exposed as a service */
  val service = endpoints.toService

  /** Response when a survey with the given id is not found in the database */
  private[web] def notFound(id: UUID) = NotFound(new RuntimeException(s"Survey not found with id:$id"))

}
