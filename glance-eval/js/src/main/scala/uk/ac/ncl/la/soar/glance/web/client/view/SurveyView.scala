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

import cats._
import cats.implicits._
import diode.data._
import diode.react.ReactPot._
import diode.react._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import uk.ac.ncl.la.soar.{ModuleCode, StudentNumber}
import uk.ac.ncl.la.soar.data.StudentRecords
import uk.ac.ncl.la.soar.glance.eval.Survey
import uk.ac.ncl.la.soar.glance.web.client.SurveyModel
import uk.ac.ncl.la.soar.glance.web.client.component.{Select, StudentCharts, StudentsDataTable, StudentsSortableTable}
import uk.ac.ncl.la.soar.glance.web.client.data.CohortAttainmentSummary
import uk.ac.ncl.la.soar.glance.web.client.style.Icon

import scala.collection.immutable.SortedMap
import scala.scalajs.js

/**
  * React Component for the SurveyView
  */
object SurveyView {
  

  type Props = ModelProxy[Pot[SurveyModel]]

  //TODO: Start thinking about moving this state to the Circuit model, not just external (remote) resources.
  case class State(selectedL: Option[StudentRecords[SortedMap, ModuleCode, Double]],
                   selectedR: Option[StudentRecords[SortedMap, ModuleCode, Double]],
                   selectingR: Boolean)

  class Backend(bs: BackendScope[Props, State]) {

    def mounted(props: Props) = Callback {}

    def handleStudentClick(student: StudentRecords[SortedMap, ModuleCode, Double]) =
      bs.modState { s =>
        if(s.selectingR)
          s.copy(selectedR = student.some)
        else
          s.copy(selectedL = student.some)
      }

    def handleClearStudent = bs.modState { s =>
      if(s.selectingR) {
        println("selectingR: true, modding selectedR to None")
        s.copy(selectedR = None)
      } else {
        println("selectedR: false, modding selectedL to None")
        s.copy(selectedL = None)
      }
    }

    def handleToggleSelecting(right: Boolean) = bs.modState(s => s.copy(selectingR = right))

    val indexCol = "Student Number"

    /** Construct the presentation of the modules as a sorted list to fill some table headings */
    private def modules(survey: Survey) = survey.modules.toList.sorted

    /** Construct the full presentation of table headings, including modules */
    private def headings(survey: Survey) = indexCol :: modules(survey)

    /** Construct the presentation of the students to fill table rows */
    private def students(survey: Survey) = survey.entries

    /** Construct the presentation of the query students to fill the rankable table rows */
    private def queryStudents(survey: Survey) = survey.entries.filter( r => survey.queries.contains(r.number) )

    /** Construct the function which provides the presentation of a table cell, given a StudentRecord and string key */
    private def renderCell(default: String)(student: StudentRecords[SortedMap, ModuleCode, Double], key: String) =
      key match {
        case k if k == indexCol => student.number
        case k => student.record.get(k).fold(default)(_.toString)
      }

    //TODO: Lookup React-collapse. Preferrably do not render unless expanded. For now we just don't show the training table
    private val trainingTable = ScalaComponent.builder[Pot[SurveyModel]]("TrainingTable")
      .render($ => {
        val model = $.props

          <.div(
            ^.className := "col-md-6",
            <.span(
              ^.className := "sub-title",
              Icon.list(Icon.Medium),
              <.h2("Training Data")),
            <.div(
              ^.className := "table-responsive",
              model.renderFailed(ex => "Error loading survey"),
              model.renderPending(_ > 50, _ => <.p("Loading ...")),
              model.render { sm =>
                StudentsDataTable.component(
                  StudentsDataTable.Props(
                    students(sm.survey),
                    headings(sm.survey),
                    renderCell(" "),
                    handleStudentClick
                  )
                )
              }
            )
          )
      })
      .build

    private val rankingTable = ScalaComponent.builder[Pot[SurveyModel]]("RankingTable")
      .render($ => {
        val model = $.props

        <.div(
          ^.className := "col-md-12",
          ^.id := "ranking",
          <.span(
            ^.className := "sub-title",
            Icon.listOl(Icon.Medium),
            <.h2("Rank students")
          ),
          model.render { sm =>

            val rankModule = sm.survey.queries.values.head
            <.div(
              ^.className := "table-responsive",
              StudentsSortableTable.component(
                StudentsSortableTable.Props(
                  rankModule, //TODO: Fix the hack by restructuring Survey
                  queryStudents(sm.survey).take(10),
                  headings(sm.survey),
                  renderCell(" "),
                  handleStudentClick
                )
              )
            )
          }
        )
      })
      .build

    def render(p: Props, s: State): VdomElement = {
      //Get the necessary data from the model
      //This is a bit of a nested Mess - TODO: Make sure we're understanding the model construction properly
      val model = p()

      val detailedView = {
        <.div(
          ^.className := "row",
          <.span(
            ^.className := "sub-title",
            Icon.search(Icon.Medium),
            <.h2("Detailed View")
          ),
          model.render { sm =>
            StudentCharts.component(
              StudentCharts.Props(
                s.selectedL,
                s.selectedR,
                s.selectingR,
                handleClearStudent,
                handleToggleSelecting,
                sm.summary
              )
            )
          }
        )
      }

      <.div(
        model.render { sm =>
          val rankModule = sm.survey.queries.values.head
          <.div(
            ^.className := "alert alert-success",
            ^.role := "alert",
            <.strong("Welcome"),
            " Please rank students on the right by how you believe they will perform in the module ",
            <.strong(rankModule),
            ". Higher is better."
          )
        },
        <.div(
          ^.className := "row",
          ^.id := "training",
          rankingTable(model)
        ),
        detailedView
      )

    }

  }

  val component = ScalaComponent.builder[ModelProxy[Pot[SurveyModel]]]("SurveyView")
    .initialStateFromProps(p => State(None, None, selectingR = false))
    .renderBackend[Backend]
    .componentDidMount(scope => scope.backend.mounted(scope.props))
    .build

}
