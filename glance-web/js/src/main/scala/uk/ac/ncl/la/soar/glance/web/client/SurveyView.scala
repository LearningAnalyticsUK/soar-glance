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

import diode._
import diode.react._
import diode.react.ReactPot._
import diode.data._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router._
import japgolly.scalajs.react.vdom.html_<^._
import uk.ac.ncl.la.soar.{ModuleCode, StudentNumber}
import uk.ac.ncl.la.soar.data.StudentRecords
import uk.ac.ncl.la.soar.glance.Survey
import uk.ac.ncl.la.soar.glance.web.client.components.StudentTable

import scala.collection.immutable.SortedMap

/**
  * React Component for the SurveyView
  */
object SurveyView {

  type Props = ModelProxy[Pot[SurveyModel]]

  case class State(selected: Option[StudentRecords[SortedMap, ModuleCode, Double]])

  class Backend(bs: BackendScope[Props, State]) {

    def mounted(props: Props) = Callback {}

    def handleStudentClick(bs: BackendScope[Props, State])
                          (student: StudentRecords[SortedMap, ModuleCode, Double]) = Callback {
      bs.modState(s => s.copy(selected = Some(student)))
      println(s"Clicked: ${student.number}")
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

    /** Construct detailed representation of student scores, including d3 viz */
    private def drawBars(records: StudentRecords[SortedMap, ModuleCode, Double], selector: String): Unit = {

      println("Redrawing bars")
      //Round scores
      val scores = records.record.iterator.map(_._2.toInt).toList

      val graphHeight = 250
      //The width of each bar.
      val barWidth = 40
      //The distance between each bar.
      val barSeparation = 5
      //The maximum value of the data.
      val maxData = 100
      //The actual horizontal distance from drawing one bar rectangle to drawing the next.
      val horizontalBarDistance = barWidth + barSeparation
      //The value to multiply each bar's value by to get its height.
      val barHeightMultiplier = graphHeight / maxData
      //Color for start
      val fail = d3.rgb(203, 49, 49)
      val pass = d3.rgb(203, 199, 84)
      val good = d3.rgb(71, 203, 80)

      def colorPicker(score: Int) = {
        if (score < 40) fail
        else if (score < 60) pass
        else good
      }

      val rectXFun = (d: Int, i: Int) => i * horizontalBarDistance
      val rectYFun = (d: Int) => graphHeight - d * barHeightMultiplier
      val rectHeightFun = (d: Int) => d * barHeightMultiplier
      val rectColorFun = (d: Int, i: Int) => colorPicker(d).toString

      //Clear existing
      d3.select(".student-bars").remove()
      val svg = d3.select(selector).append("svg")
        .attr("width", "100%")
        .attr("height", "250px")
        .attr("class", "student-bars")
      import js.JSConverters._
      val sel = svg.selectAll("rect").data(scores.toJSArray)
      sel.enter()
        .append("rect")
        .attr("x", rectXFun)
        .attr("y", rectYFun)
        .attr("width", barWidth)
        .attr("height", rectHeightFun)
        .style("fill", rectColorFun)
      ()
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
            StudentTable.component(
              StudentTable.Props(
                students(sm.survey),
                headings(sm.survey),
                renderCell(" "),
                handleStudentClick(bs)
              )
            )
          }
        ),
        <.h2("Detailed View")
        <.div()
      )
    }

  }

  val component = ScalaComponent.builder[ModelProxy[Pot[SurveyModel]]]("SurveyView")
    .initialStateFromProps(p => State(None))
    .renderBackend[Backend]
    .componentDidMount(scope => scope.backend.mounted(scope.props))
    .build

}
