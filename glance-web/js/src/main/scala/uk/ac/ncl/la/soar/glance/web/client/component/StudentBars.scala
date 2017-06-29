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
import scala.collection.mutable.ListBuffer
import scala.scalajs.js
import scala.util.Random

/**
  * Simple component for rendering bar charts describing some stat for an individual student
  */
object StudentBars {

  val failColour = "#CB3131"
  val passColour = "#CBC754"
  val goodColour = "#47CB50"

  case class Props(student: Option[StudentRecords[SortedMap, ModuleCode, Double]])

  class Backend(bs: BackendScope[Props, Unit]) {

    def mounted(p: Props) = Callback { println("Bars did mount") }
    def willUpdate = Callback {
      println("Destroying chart")
      d3.select(chartSelector).remove()
    }
    def didUpdate(p: Props) = Callback {
      println("Initialising chart")
      p.student.foreach(drawBars)
    }

    val chartSelector = "#student-bars"

    def render(p: Props): VdomElement = {
      println("Rendering bars")
      println(p.student)
      <.div(
          ^.id := "detailed",
          p.student.fold(<.p("Click on a student"): TagMod) { s =>
            Chart.component(drawBars(s))
          }
        )
    }

    /** Construct detailed representation of student scores, including d3 viz */
    private def drawBars(records: StudentRecords[SortedMap, ModuleCode, Double]): Chart.Props = {
      println("Drawing bars")
      //Create a props object for the chart component based on a StudentRecords object
      //Get the module labels and scores
      val mB = ListBuffer.empty[ModuleCode]
      val sB = ListBuffer.empty[Double]
      for ((module, score) <- records.record) {
        mB += module
        sB += score
      }
      val modules = mB.toList
      val scores = sB.toList

      //Get width and height of parent

      val data = ChartData(modules, Seq(ChartDataset(scores, "Module Scores")))
      Chart.Props("Student Module Scores", Chart.BarChart, data)
    }

//    private def drawBars(records: StudentRecords[SortedMap, ModuleCode, Double]): Unit = {
//
//      //Round scores
//      val scores = records.record.iterator.map(_._2.toInt).toList
//
//      val graphHeight = 250
//      //The width of each bar.
//      val barWidth = 40
//      //The distance between each bar.
//      val barSeparation = 5
//      //The maximum value of the data.
//      val maxData = 100
//      //The actual horizontal distance from drawing one bar rectangle to drawing the next.
//      val horizontalBarDistance = barWidth + barSeparation
//      //The value to multiply each bar's value by to get its height.
//      val barHeightMultiplier = graphHeight / maxData
//      //Color for start
//      val fail = d3.rgb(203, 49, 49)
//      val pass = d3.rgb(203, 199, 84)
//      val good = d3.rgb(71, 203, 80)
//
//      def colorPicker(score: Int) = {
//        if (score < 40) fail
//        else if (score < 60) pass
//        else good
//      }
//
//      val rectXFun = (d: Int, i: Int) => i * horizontalBarDistance
//      val rectYFun = (d: Int) => graphHeight - d * barHeightMultiplier
//      val rectHeightFun = (d: Int) => d * barHeightMultiplier
//      val rectColorFun = (d: Int, i: Int) => colorPicker(d).toString
//
//      //Clear existing
//      val svg = d3.select("#detailed").append("svg")
//        .attr("width", "100%")
//        .attr("height", "250px")
//        .attr("id", "student-bars")
//      import js.JSConverters._
//      val sel = svg.selectAll("rect").data(scores.toJSArray)
//      sel.enter()
//        .append("rect")
//        .attr("x", rectXFun)
//        .attr("y", rectYFun)
//        .attr("width", barWidth)
//        .attr("height", rectHeightFun)
//        .style("fill", rectColorFun)
//      ()
//    }
  }

  val component = ScalaComponent.builder[Props]("StudentBars")
    .renderBackend[Backend]
    .componentDidMount(scope => scope.backend.mounted(scope.props))
    .componentWillUpdate(scope => scope.backend.willUpdate)
    .componentDidUpdate(scope => scope.backend.didUpdate(scope.currentProps))
    .build

}
