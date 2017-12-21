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
package uk.ac.ncl.la.soar.glance.web.client.component

import java.time.Instant
import java.util.UUID

import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import uk.ac.ncl.la.soar.StudentNumber
import uk.ac.ncl.la.soar.glance.eval.{IncompleteResponse, Survey, SurveyResponse}
import uk.ac.ncl.la.soar.glance.web.client.SurveyModel
import uk.ac.ncl.la.soar.glance.web.client.style.Icon

import scala.scalajs.js.Date

/** Description of Class
  *
  * @author hugofirth
  */
object SurveyResponseForm {

  case class Props(proxy: ModelProxy[Pot[SurveyModel]],
                   submitHandler: Option[IncompleteResponse] => Callback)

  case class State(respondent: String)

  sealed trait FormField
  case object EmailField extends FormField

  class Backend(bs: BackendScope[Props, State]) {

    private def formValueChange(e: ReactEventFromInput) = {
      val text = e.target.value
      bs.modState(s => s.copy(respondent = text))
    }

    private val emailRegex =
      """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r

    private def validEmail(e: String): Boolean = {
      if (e.trim.isEmpty)
        false
      else if (emailRegex.findFirstMatchIn(e).isDefined)
        true
      else
        false
    }

    private def buildResponseFromProps(p: Props, s: State) = {
      p.proxy().toOption.map { sm =>
        IncompleteResponse(sm.survey,
                           sm.simpleRanking,
                           sm.detailedRanking,
                           s.respondent,
                           sm.startTime,
                           UUID.randomUUID)
      }
    }

    def render(p: Props, s: State): VdomElement = {
      <.form(
        <.div(
          ^.className := (if (validEmail(s.respondent)) "form-group" else "form-group has-error"),
          <.label(^.`for` := "emailInput", "University Email"),
          <.input(
            ^.`type` := "email",
            ^.className := "form-control",
            ^.id := "emailInput",
            ^.placeholder := "Email",
            ^.onChange ==> formValueChange
          )
        ),
        <.button(
          ^.`type` := "button",
          ^.className := "btn btn-primary pull-right",
          "Submit",
          (^.disabled := true).when(!validEmail(s.respondent)),
          ^.onClick --> p.submitHandler(buildResponseFromProps(p, s))
        )
      )
    }
  }

  val component = ScalaComponent
    .builder[Props]("SurveyResponseForm")
    .initialStateFromProps(p => State(""))
    .renderBackend[Backend]
    .build

}
