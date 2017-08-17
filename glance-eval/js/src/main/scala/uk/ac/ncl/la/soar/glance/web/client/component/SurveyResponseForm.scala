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
import uk.ac.ncl.la.soar.glance.eval.{IncompleteResponse, Survey, SurveyResponse}
import uk.ac.ncl.la.soar.glance.web.client.SurveyModel
import uk.ac.ncl.la.soar.glance.web.client.style.Icon

import scala.scalajs.js.Date

/** Description of Class
  *
  * @author hugofirth
  */
object SurveyResponseForm {

  case class Props(survey: Survey, submitHandler: State => Callback)

  type State = IncompleteResponse

  sealed trait FormField
  case object EmailField extends FormField
  case object NotesField extends FormField

  class Backend(bs: BackendScope[Props, State]) {

    private def formValueChange(field: FormField)(e: ReactEventFromInput) = {
      val text = e.target.value
      field match {
        case EmailField => bs.modState(s => s.copy(respondent = text))
        case NotesField => bs.modState(s => s.copy(notes = text))
      }
    }

    def render(p: Props, s: State): VdomElement = {
      <.form(
        <.div(
          ^.className := "form-group",
          <.label(^.`for` := "emailInput", "University Email"),
          <.input(
            ^.`type` := "email",
            ^.className := "form-control",
            ^.id := "emailInput",
            ^.placeholder := "Email",
            ^.onChange ==> formValueChange(EmailField)
          ),
        ),
        <.div(
          ^.className := "form-group",
          <.label(^.`for` := "notesInput", "Please enter your notes about this experiment..."),
          <.textarea(
            ^.className := "form-control",
            ^.rows := 3,
            ^.onChange ==> formValueChange(NotesField)
          )
        ),
        <.button(
          ^.`type` := "button",
          ^.className := "btn btn-primary pull-right",
          "Submit",
          ^.onClick --> p.submitHandler(s)
        )
      )
    }
  }

  val component = ScalaComponent.builder[Props]("SurveyResponseForm")
    .initialStateFromProps({ p =>
      val s =  p.survey
      IncompleteResponse(s, s.queries.keysIterator.toVector, "", Date.now, UUID.randomUUID, "")
    })
    .renderBackend[Backend]
    .build

}
