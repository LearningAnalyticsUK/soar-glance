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

import cats.data.EitherT
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.raw.XMLHttpRequest

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import uk.ac.ncl.la.soar.glance.Survey

/**
  * Object defining methods for talking to remote API and parsing results
  */
object ApiClient {

  /** TODO: Figure out if there is a way to do compiletime config? Or something of that sort. Setting this here is bad*/
  val url = "http://localhost:8080/surveys"

  //Could maybe make these Tasks and assign to lazy vals?
  def loadSurveys: EitherT[Future, Error, List[Survey]] = EitherT(Ajax.get(s"$url").map(decodeSurveys))

  private def decodeSurveys(xhr: XMLHttpRequest) = decode[List[Survey]](xhr.responseText)


}
