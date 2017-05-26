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

import cats._
import cats.implicits._
import io.circe._
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom
import scala.concurrent.ExecutionContext.Implicits.global
import org.singlespaced.d3js.Ops._
import org.singlespaced.d3js.d3

import scala.scalajs.js

object SurveyPage {

  def main(): Unit = {


    //Lets get the survey data
    val surveys = ApiClient.loadSurveys

    //Lets map the dom element for the training data
    val cells = dom.document.getElementById("cells")

    surveys.fold(
      { case e @ DecodingFailure(_, _) =>
          //Create error message
          val err = dom.document.createElement("p")
          err.textContent = e.show
          //Append error message to cells
          cells.appendChild(err)
      },
      { case a =>
          //Create success message
          val succ = dom.document.createElement("p")
          succ.textContent = a.size.toString
          //Append success to cells
          cells.appendChild(succ)
      }
    )

    /**
      * Adapted from http://thecodingtutorials.blogspot.ch/2012/07/introduction-to-d3.html
      */
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
    val c = d3.rgb("DarkSlateBlue")

    val rectXFun = (d: Int, i: Int) => i * horizontalBarDistance
    val rectYFun = (d: Int) => graphHeight - d * barHeightMultiplier
    val rectHeightFun = (d: Int) => d * barHeightMultiplier
    val rectColorFun = (d: Int, i: Int) => c.brighter(i * 0.5).toString

    val svg = d3.select("#cells").append("svg").attr("width", "100%").attr("height", "450px")
    val sel = svg.selectAll("rect").data(js.Array(8, 22, 31, 36, 48, 17, 25))
    sel.enter()
      .append("rect")
      .attr("x", rectXFun)
      .attr("y", rectYFun)
      .attr("width", barWidth)
      .attr("height", rectHeightFun)
      .style("fill", rectColorFun)

    //Explicit Unit return to supress warning. Why is this a thing? Find out.
    ()
  }

}
