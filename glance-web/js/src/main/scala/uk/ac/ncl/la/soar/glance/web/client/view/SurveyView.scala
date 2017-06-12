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
package uk.ac.ncl.la.soar.glance.web.client.view

import diode.data._
import diode.react.ReactPot._
import diode.react._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import org.singlespaced.d3js.Ops._
import org.singlespaced.d3js.d3
import uk.ac.ncl.la.soar.ModuleCode
import uk.ac.ncl.la.soar.data.StudentRecords
import uk.ac.ncl.la.soar.glance.Survey
import uk.ac.ncl.la.soar.glance.web.client.SurveyModel
import uk.ac.ncl.la.soar.glance.web.client.component.{StudentBars, StudentsTable}

import scala.collection.immutable.SortedMap
import scala.scalajs.js

/**
  * React Component for the SurveyView
  */
object SurveyView {

  type Props = ModelProxy[Pot[SurveyModel]]

  case class State(selected: Option[StudentRecords[SortedMap, ModuleCode, Double]])

  class Backend(bs: BackendScope[Props, State]) {

    def mounted(props: Props) = Callback {}

    def handleStudentClick(bs: BackendScope[Props, State])
                          (student: StudentRecords[SortedMap, ModuleCode, Double]) = {
      println("Changing state")
      bs.modState(s => s.copy(selected = Some(student)))
    }

    val indexCol = "Student Number"

    /** Construct the presentation of the modules as a sorted list to fill some table headings */
    private def modules(survey: Option[Survey]) = survey.map(_.modules.toList).getOrElse(List.empty).sorted

    /** Construct the full presentation of table headings, including modules */
    private def headings(survey: Option[Survey]) = indexCol :: modules(survey)

    /** Construct the presentation of the students to fill table rows */
    private def students(survey: Option[Survey]) = survey.map(_.entries).getOrElse(List.empty)

    /** Construct the function which provides the presentation of a table cell, given a StudentRecord and string key */
    private def renderCell(default: String)(student: StudentRecords[SortedMap, ModuleCode, Double], key: String) =
      key match {
        case k if k == indexCol => student.number
        case k => student.record.get(k).fold(default)(_.toString)
      }


    def render(p: Props, s: State): VdomElement = {
      //Get the necessary data from the model
      //This is a bit of a nested Mess - TODO: Make sure we're understanding the model construction properly
      val model = p()

      //Build UI elements

      //TODO: investigate why we're having to call TagMod.fromTraversableOnce directly. Most examples don't.
      <.div(
        ^.id := "training",
        <.h2("Training Data"),
        <.div(
          ^.className := "table-responsive",
          model.renderFailed(ex => "Error loading survey"),
          model.renderPending(_ > 50, _ => <.p("Loading ...")),
          model.render { sm =>
            StudentsTable.component(
              StudentsTable.Props(
                students(sm.survey),
                headings(sm.survey),
                renderCell(" "),
                handleStudentClick(bs)
              )
            )
          }
        ),
        <.h2("Detailed View"),
        StudentBars.component(
          StudentBars.Props(s.selected)
        )
      )
    }

  }

  val component = ScalaComponent.builder[ModelProxy[Pot[SurveyModel]]]("SurveyView")
    .initialStateFromProps(p => State(None))
    .renderBackend[Backend]
    .componentDidMount(scope => scope.backend.mounted(scope.props))
    .build

}
