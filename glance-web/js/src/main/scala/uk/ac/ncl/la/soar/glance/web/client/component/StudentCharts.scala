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
object StudentCharts {

  case class Props(student: Option[StudentRecords[SortedMap, ModuleCode, Double]])

  class Backend(bs: BackendScope[Props, Unit]) {

    def mounted(p: Props) = Callback { println("Bars did mount") }
    def willUpdate = Callback { }
    def didUpdate(p: Props) = Callback { }

    def render(p: Props): VdomElement = {
      println("Rendering bars")
      <.div(
        ^.id := "detailed",
        p.student.fold(<.p(^.className := "chart-placedholder", "Click on a student"): TagMod) { s =>
          Seq(
            <.div(
              ^.className := "chart-container",
              Chart.component(drawBars(s))
              ),
            <.div(
              ^.className := "chart-container",
              Chart.component(drawLines(s))
            )
          ).toTagMod

        }
      )
    }

    /** Construct line chart representation of student average over time, as a proof of concept */
    private def drawLines(records: StudentRecords[SortedMap, ModuleCode, Double]): Chart.Props = {
      print("Drawing lines")
      //Very mutable, but I'm trying to get back into the habit of method local mutability.
      var total = 0.0
      var counter = 0.0
      val aBldr = ListBuffer.empty[Double]
      for((_, r) <- records.record){
        total += r
        counter += 1
        aBldr += (total / counter)
      }
      val averages = aBldr.result()
      val labels = averages.map(_ => "")
      println(s"Averages: $averages")
      val data = ChartData(labels, Seq(ChartDataset(averages, "Average score")))
      Chart.Props("Average Score Over Time", Chart.LineChart, data)
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

      val (fillColours, borderColours) = colourBars(scores)
      val data = ChartData(modules, Seq(ChartDataset(scores, "Module Scores", fillColours, borderColours)))
      Chart.Props("Student Module Scores", Chart.BarChart, data)
    }

    /** Calculate List of colours for student bars */
    private def colourBars(scores: List[Double]): (List[String], List[String]) = {
      //Constants representing colours (fail, pass, good)
      val borderColours = ("#CB3131", "#CBC754", "#47CB50")
      val fillColours = ("#CB4243", "#CBCB72", "#6FCB76")

      def colourPicker(score: Int, colours: (String, String, String)) = {
        if(score < 40) colours._1
        else if (score < 60) colours._2
        else colours._3
      }

      val fills = scores.iterator.map(s => colourPicker(s.toInt, fillColours))
      val borders = scores.iterator.map(s => colourPicker(s.toInt, borderColours))
      (fills.toList, borders.toList)
    }

    /** Draw filter form group */
    private def drawFilters

  }

  val component = ScalaComponent.builder[Props]("StudentBars")
    .renderBackend[Backend]
    .componentDidMount(scope => scope.backend.mounted(scope.props))
    .componentWillUpdate(scope => scope.backend.willUpdate)
    .componentDidUpdate(scope => scope.backend.didUpdate(scope.currentProps))
    .build

}
