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

import cats.data.EitherT
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.raw.XMLHttpRequest

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import uk.ac.ncl.la.soar.data.Module
import uk.ac.ncl.la.soar.glance.eval.{SessionSummary, Survey, SurveyResponse}
import org.scalajs.dom

/**
  * Object defining methods for talking to remote API and parsing results
  */
object ApiClient {

  private val protocol = dom.window.location.protocol
  private val host = dom.window.location.host
  private val port = "8080"

  /** TODO: Figure out if there is a way to do compiletime config? Or something of that sort. Setting this here is bad */
  def url(rel: String) = s"$protocol//$host:$port/$rel"

  /* Surveys */

  /** Load the survey ids from the surveys API, then decode them using circe */
  def loadSurveyIds: Future[Either[Error, List[UUID]]] = Ajax.get(url("surveys")).map(decodeReq[List[UUID]])

  /** Load the survey ids, decode them then lift into an either transformer. Would be preferable to do this always, but
    * [[diode.Effect]]'s `apply` method expects an un evaluated future and it seems crazy to lift only to call `.value`
    * immediately. So, for now, this is just a helper.
    */
  def loadSurveysIdsT: EitherT[Future, Error, List[UUID]] = EitherT(loadSurveyIds)

  /** Load a survey with the given id from the surveys API, then decode using circe */
  def loadSurvey(id: UUID): Future[Either[Error, Survey]] = Ajax.get(url(s"surveys/$id")).map(decodeReq[Survey])

  def loadSurveyT(id: UUID): EitherT[Future, Error, Survey] = EitherT(loadSurvey(id))


  /* Sessions */

  /** Load the recap sessions relevant to a given survey */
  def loadRecaps(id: UUID): Future[Either[Error, SessionSummary]] =
    Ajax.get(url(s"surveys/$id/recap")).map(decodeReq[SessionSummary])

  def loadRecapsT(id: UUID): EitherT[Future, Error, SessionSummary] = EitherT(loadRecaps(id))

  def loadClusters(id: UUID): Future[Either[Error, SessionSummary]] =
    Ajax.get(url(s"surveys/$id/cluster")).map(decodeReq[SessionSummary])

  def loadClustersT(id: UUID): EitherT[Future, Error, SessionSummary] = EitherT(loadClusters(id))

  /* SurveyResponses */

  /** Post a survey response */
  def postResponse(r: SurveyResponse) = Ajax.post(url("responses"), r.asJson.noSpaces)

  private def decodeReq[A: Decoder](xhr: XMLHttpRequest) = decode[A](xhr.responseText)

  /* Modules */

  /** Load the modules for this survey */
  def loadModules: Future[Either[Error, List[Module]]] = Ajax.get(url("modules")).map(decodeReq[List[Module]])

  def loadModulesT: EitherT[Future, Error, List[Module]] = EitherT(loadModules)
}
