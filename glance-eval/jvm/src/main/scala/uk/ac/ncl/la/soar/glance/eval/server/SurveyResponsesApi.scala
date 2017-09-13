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

import java.util.UUID

import io.finch._
import io.finch.circe._
import io.circe.generic.auto._
import uk.ac.ncl.la.soar.glance.eval.SurveyResponse
import uk.ac.ncl.la.soar.glance.eval.SurveyResponse._
import uk.ac.ncl.la.soar.server.Implicits._

/**
  * Class defines the REST api for survey responses
  */
class SurveyResponsesApi(repository: SurveyResponseDb) {

  /** Endpoint returns all survey responses in response to `GET /responses` */
  lazy val list = get("responses") {
    repository.list.toFuture.map(Ok(_))
  }

  /** Endpoint to retrieve a specific SurveyResponse by id */
  val read = get("responses" :: path[UUID]) { id: UUID =>
    println(s"received a request for this survey response: $id")
    repository.find(id).toFuture.map {
      case Some(s) => Ok(s)
      case None => notFound(id)
    }
  }

  private val postedResponse = jsonBody[UUID => SurveyResponse].map(_(UUID.randomUUID()))

  /** Endpoint to accept new SurveyResponses */
  val write: Endpoint[SurveyResponse] = post("responses" :: postedResponse) { r: SurveyResponse =>
    repository.save(r).toFuture.map(_ => Created(r))
  }

  /** Pre-flight endpoint for CORS headers */
  //Clunky Unit syntax - fix?
  val preflight = options(*) { Ok(()) }

  /** All endpoints in this API, enriched with CORS headers. */
  val endpoints = (list :+: read :+: write :+: preflight).withCorsHeaders

  /** API endpoints exposed as a service */
  val service = endpoints.toService

  /** Response when a survey response with the given id is not found in the database */
  private[server] def notFound(id: UUID) = NotFound(new RuntimeException(s"Survey response not found with id:$id"))

}
