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
package uk.ac.ncl.la.soar.glance.web.client.component.chart

import cats.data.NonEmptyVector
import cats.implicits._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import uk.ac.ncl.la.soar.{ModuleCode, StudentNumber}
import uk.ac.ncl.la.soar.data.StudentRecords
import uk.ac.ncl.la.soar.glance.eval.{SessionSummary, VisualisationType}
import uk.ac.ncl.la.soar.glance.web.client.SurveyData
import uk.ac.ncl.la.soar.glance.web.client.component._
import uk.ac.ncl.la.soar.glance.web.client.style.Icon

import scala.collection.immutable.SortedMap
import scala.collection.mutable.{ArrayBuffer, ListBuffer}

/**
  * Simple component for rendering charts describing some stat for an individual student
  */
object StudentChartsContainer {

  type Filter = (ModuleCode, Double) => Boolean

  private val options: NonEmptyVector[Select.Choice[Filter]] =
    NonEmptyVector(
      Select.Choice((_, _) => true, "None"),
      Vector.empty[Select.Choice[Filter]])

  case class Props(studentL: Option[StudentRecords[SortedMap, ModuleCode, Double]],
                   studentR: Option[StudentRecords[SortedMap, ModuleCode, Double]],
                   selectingR: Boolean,
                   handleClearStudent: Callback,
                   toggleSelecting: (Boolean) => Callback,
                   data: SurveyData,
                   viz: List[VisualisationType],
                   filterChoices: NonEmptyVector[Select.Choice[Filter]] = options)

  case class State(selectedFilters: Set[Select.Choice[Filter]], cohortComparison: Boolean)

  class Backend(bs: BackendScope[Props, State]) {

    type Student = StudentRecords[SortedMap, ModuleCode, Double]
    type Renderable = (Student, Props, State) => VdomElement

    /** Define charts inline here for now */
    private val charts: Map[VisualisationType, StudentChart] = Map(
      VisualisationType.histPerfBars -> StudentChart.HistPerfBars,
      VisualisationType.avgVTime -> StudentChart.AvgTrendLine,
      VisualisationType.clusterVTime -> StudentChart.ClusterUsageLine,
      VisualisationType.recapVTime -> StudentChart.RecapUsageLine
    )

    def mounted(p: Props) = Callback {}

    def render(p: Props, s: State): VdomElement = {

      //Get the list of charts to draw
      val renderCharts = p.viz.flatMap(charts.get)
      //Split the charts into columns
      val (leftCharts, rightCharts) = renderCharts.splitAt((renderCharts.length + 1) / 2)


      //WARNING! Normally prefer stdlib abstractions/combinators like flatMap but they were freaking out with the
      // existential type stuff which goes on in StudentChart. The scala-js-react methods (.whenDefined, .toTagMod)
      // seem to work, though it is not immediately clear why. Keep an eye on this as a potential trouble spot!
      <.div(
        ^.id := "detailed",
        p.studentL.fold[TagMod](<.p(^.className := "chart-placedholder", "Click on a student")) { student =>
          List(
            <.div(
              ^.className := "chart-controls",
              <.div(
                ^.className := "row",
                drawFilters(p.filterChoices, s.selectedFilters),
                drawStudComparisonControls(
                  p.studentL,
                  p.studentR,
                  p.toggleSelecting,
                  p.selectingR,
                  p.handleClearStudent
                ),
                drawCheckbox(s.cohortComparison)
              )
            ),
            <.div(
              ^.className := "row border-between",
              <.div(
                ^.className := "col-md-6",
                leftCharts.toTagMod(chart => chart.component(p, s).whenDefined)
              ),
              <.div(
                ^.className := "col-md-6",
                rightCharts.toTagMod(chart => chart.component(p, s).whenDefined)
              )
            )
          ).toTagMod
        }
      )
    }

    /** Draw filter form group */
    private def drawFilters(choices: NonEmptyVector[Select.Choice[Filter]], selected: Set[Select.Choice[Filter]]) = {
      <.div(
        ^.className := "col-md-5",
        <.div(
          ^.className := "input-group",
          <.div(
            ^.className := "input-group-addon",
            Icon.filter(Icon.Small), "Filters:  "),
          <.div(
            ^.className := "bootstrap-tagsinput",
            if(selected.isEmpty) {
              <.span(^.id := "filters-placeholder", "Active Filters ...")
            } else {
              selected.toTagMod { s =>
                <.span(
                  ^.className := "tag label label-info",
                  s.label,
                  <.span(
                    VdomAttr("data-role") := "remove",
                    ^.onClick --> filterRemove(s)
                  )
                )
              }
            }
          ),
          Select.component(Select.Props(selected.headOption.getOrElse(choices.head),
            choices.toVector, filterSelect, "Choose  ".some))
        )
      )
    }

    /** Handle filter select */
    private def filterSelect(selected: Select.Choice[Filter]) =
      bs.modState(s => s.copy(selectedFilters = s.selectedFilters + selected))

    /** Handle filter remove */
    private def filterRemove(removed: Select.Choice[Filter]) =
      bs.modState(s => s.copy(selectedFilters = s.selectedFilters - removed))

    /** Handle cohort comparison toggle */
    private def cohortToggle(e: ReactEventFromInput) = bs.modState(s => s.copy(cohortComparison = !s.cohortComparison))

    /** Draw student to student comparison controls */

    private def drawStudComparisonControls(selected: Option[StudentRecords[SortedMap, ModuleCode, Double]],
                                           compareTo: Option[StudentRecords[SortedMap, ModuleCode, Double]],
                                           toggleSelect: Boolean => Callback,
                                           selectingR: Boolean,
                                           handleClearStudent: Callback) = {
      <.div(
        ^.className := "col-md-5",
        <.div(
          ^.className := "row",
          <.div(
            ^.className := "col-md-5",
            <.label(
              ^.className := "sr-only",
              ^.`for` := "selectedStudentL",
              "Selected student"
            ),
            <.div(
              if(selectingR) {
                ^.className := "input-group"
              } else {
                ^.className := "input-group has-success"
              },
              <.div(
                ^.className := "input-group-addon",
                "Select Student",
                ^.onClick --> toggleSelect(false)
              ),
              <.div(
                ^.className := "form-control",
                ^.id := "selectedStudentL",
                selected.fold[TagMod]("Student Number")(s => s.number),
                ^.onClick --> toggleSelect(false)
              )
            )
          ),
          <.div(
            ^.className := "col-md-7",
            <.label(
              ^.className := "sr-only",
              ^.`for` := "selectedStudentR",
              "Compared to"),
            <.div(
              if(selectingR) {
                ^.className := "input-group has-info"
              } else {
                ^.className := "input-group"
              },
              <.div(
                ^.className := "input-group-addon",
                "Compared to",
                ^.onClick --> toggleSelect(true)
              ),
              <.div(
                ^.className := "form-control",
                ^.id := "selectedStudentR",
                compareTo.fold[TagMod]("Student Number")(s => s.number),
                ^.onClick --> toggleSelect(true)
              ),
              <.div(
                ^.className := "input-group-btn",
                <.button(
                  ^.className := "btn btn-default",
                  ^.`type` := "button",
                  <.strong("X"),
                  ^.onClick --> { toggleSelect(true) >> handleClearStudent >> toggleSelect(false) }
                )
              )
            )
          )
        )
      )
    }

    /** Draw cohort comparison checkbox */
    private def drawCheckbox(cohortSummary: Boolean): VdomElement = {
      <.div(
        ^.className := "col-md-1 col-md-offset-1",
        <.div(
          ^.className := "input-group",
          ^.id := "cohort-summary",
          <.label(
            if(cohortSummary)
              ^.className := "btn btn-primary"
            else
              ^.className := "btn btn-default",
            <.input(
              ^.id := "cohort-summary-toggle",
              ^.`type` := "checkbox",
              ^.selected := cohortSummary,
              ^.onChange ==> cohortToggle
            ),
            "  Cohort Summary"
          )
        )

      )
    }

  }

  val component = ScalaComponent.builder[Props]("StudentBars")
    .initialStateFromProps(p => State(Set.empty[Select.Choice[Filter]], cohortComparison = false))
    .renderBackend[Backend]
    .componentDidMount(scope => scope.backend.mounted(scope.props))
    .build

}
