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

import com.sun.tools.javac.comp.Todo
import diode.react.ModelProxy
import japgolly.scalajs.react.{BackendScope, Callback}
import japgolly.scalajs.react.vdom.html_<^._
import uk.ac.ncl.la.soar.{ModuleCode, StudentNumber}
import uk.ac.ncl.la.soar.data.StudentRecords
import uk.ac.ncl.la.soar.glance.Survey

import scala.collection.immutable.SortedMap

/**
  * React Component for the SurveyView
  */
object SurveyView {

  case class Props(proxy: ModelProxy[Option[Survey]])

  case class State(selected: Option[StudentNumber])

  class Backend($: BackendScope[Props, State]) {

    def mounted(props: Props) = Callback {}

    def handleStudentClick(num: StudentNumber) = Callback { println(s"Clicked: $num") }

    def render(p: Props, s: State) = {
      //Get the necessary data from the model
      val survey = p.proxy()
      val modules = survey.map(_.modules).getOrElse(Set.empty)
      val students = survey.map(_.entries).getOrElse(List.empty)

      //Build UI elements
      val columns = "Student Number" :: modules.toList.sorted

      <.section(
        ^.id := "training-section",
        <.h2("Training Data"),
        <.div(
          ^.className := "table-responsive",
          <.table(
            ^.className := "table table-striped table-bordered table-hover",
            ^.id := "training-data",
            <.thead(
              <.tr(
                columns.map(<.td(_)):_*
              )
            ),
            <.tbody(
              students.map(tableRow(modules, _)):_*
            )
          )
        )
      )
    }

    def tableRow(modules: Set[String], studentRecords: StudentRecords[SortedMap, ModuleCode, Double]) = {
      //Get the table columns (minuse student number)
      val moduleCols = modules.toList.sorted
      //Fill in blanks where student has no score for module
      val recordEntries = moduleCols.map(c => studentRecords.record.get(c).fold(" ")(_.toString))
      //Add student number
      val columns = studentRecords.number :: recordEntries
      //Build the table row
      <.tr(
        ^.onClick --> handleStudentClick(studentRecords.number),
        columns.map(<.td(_)):_*
      )
    }

  }

  private val component = ReactComponentB[Props]("Survey")
    .initialState_P(p => State(None))
    .renderBackend[Backend]
    .componentDidMount(scope => scope.backend.mounted(scope.props))
    .build

  def apply(proxy: ModelProxy[Seq[Todo]], currentFilter: TodoFilter, ctl: RouterCtl[TodoFilter]) = component(Props(proxy, currentFilter, ctl))
}
