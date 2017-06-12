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

import uk.ac.ncl.la.soar.ModuleCode
import uk.ac.ncl.la.soar.data.StudentRecords
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

import scala.collection.immutable.SortedMap
import scala.scalajs.js

/**
  * Simple component for rendering bar charts describing some stat for an individual student
  */
object StudentBars {

  case class Props(student: Option[StudentRecords[SortedMap, ModuleCode, Double]])

  class Backend(bs: BackendScope[Props, Unit]) {

    def mounted(p: Props) = Callback {
//      p.student.foreach(drawBars)
    }

    def render(p: Props): VdomElement = {
      println("trying to rerender")
      <.div(
          ^.id := "detailed",
          <.p("Click on a Student").unless(p.student.nonEmpty)
        )
    }

    /** Construct detailed representation of student scores, including d3 viz */
    private def drawBars(records: StudentRecords[SortedMap, ModuleCode, Double]): Unit = {

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
//      d3.select("#student-bars").remove()
      val svg = d3.select("#detailed").append("svg")
        .attr("width", "100%")
        .attr("height", "250px")
        .attr("id", "student-bars")
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
  }

  val component = ScalaComponent.builder[Props]("StudentBars")
    .renderBackend[Backend]
    .componentDidMount(scope => scope.backend.mounted(scope.props))
    .build

}
