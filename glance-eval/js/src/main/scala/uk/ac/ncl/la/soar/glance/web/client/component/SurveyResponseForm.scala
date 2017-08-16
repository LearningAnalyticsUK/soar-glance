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
import diode.react.ModelProxy
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import uk.ac.ncl.la.soar.StudentNumber
import uk.ac.ncl.la.soar.glance.eval.{Survey, SurveyResponse}
import uk.ac.ncl.la.soar.glance.web.client.SurveyModel
import uk.ac.ncl.la.soar.glance.web.client.style.Icon

import scala.scalajs.js.Date

/** Description of Class
  *
  * @author hugofirth
  */
object SurveyResponseForm {

  type Props = Survey

  type State = SurveyResponse

  class Backend(bs: BackendScope[Props, State]) {

    def render(p: Props, s: State): VdomElement = {
      <.form(
        <.div(
          ^.className := "form-group",
          <.label(^.`for` := "emailInput", "University Email"),
          <.input(
            ^.`type` := "email",
            ^.className := "form-control",
            ^.id := "emailInput",
            ^.placeholder := "Email"
          ),
        ),
        <.div(
          ^.className := "form-group",
          <.label(^.`for` := "notesInput", "Please enter your notes about this experiment..."),
          <.textarea(
            ^.className := "form-control",
            ^.rows := 3
          )
        ),
        <.button(
          ^.`type` := "submit",
          ^.className := "btn btn-primary",
          "Submit"
        )
      )
    }
  }

  val component = ScalaComponent.builder[Props]("SurveyResponseForm")
    .initialStateFromProps(p => SurveyResponse(p, p.queries.keysIterator.toVector, "", Instant.now, UUID.randomUUID, ""))
    .renderBackend[Backend]
    .build

}
