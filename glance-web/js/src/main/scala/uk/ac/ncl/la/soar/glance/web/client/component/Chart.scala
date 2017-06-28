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

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import org.scalajs.dom.raw.HTMLCanvasElement

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.{JSGlobal, JSName}

@js.native
trait ChartDataset extends js.Object {
  def label: String = js.native
  def data: js.Array[Double] = js.native
  def fillColor: String = js.native
  def strokeColor: String = js.native
}

object ChartDataset {
  def apply(data: Seq[Double],
            label: String, backgroundColor: String = "#8080FF", borderColor: String = "#404080"): ChartDataset = {
    js.Dynamic.literal(
      label = label,
      data = data.toJSArray,
      backgroundColor = backgroundColor,
      borderColor = borderColor
    ).asInstanceOf[ChartDataset]
  }
}

@js.native
trait ChartData extends js.Object {
  def labels: js.Array[String] = js.native
  def datasets: js.Array[ChartDataset] = js.native
}

object ChartData {
  def apply(labels: Seq[String], datasets: Seq[ChartDataset]): ChartData = {
    js.Dynamic.literal(
      labels = labels.toJSArray,
      datasets = datasets.toJSArray
    ).asInstanceOf[ChartData]
  }
}

@js.native
trait ChartOptions extends js.Object {
  def responsive: Boolean = js.native
}

object ChartOptions {
  def apply(responsive: Boolean = true): ChartOptions = {
    js.Dynamic.literal(
      responsive = responsive
    ).asInstanceOf[ChartOptions]
  }
}

@js.native
trait ChartConfiguration extends js.Object {
  def `type`: String = js.native
  def data: ChartData = js.native
  def options: ChartOptions = js.native
}

object ChartConfiguration {
  def apply(`type`: String, data: ChartData, options: ChartOptions = ChartOptions(false)): ChartConfiguration = {
    js.Dynamic.literal(
      `type` = `type`,
      data = data,
      options = options
    ).asInstanceOf[ChartConfiguration]
  }
}

// define a class to access the Chart.js component
@js.native
@JSGlobal("Chart")
class JSChart(ctx: js.Dynamic, config: ChartConfiguration) extends js.Object

object Chart {

  // available chart styles
  sealed trait ChartStyle
  case object LineChart extends ChartStyle
  case object BarChart extends ChartStyle

  case class Props(name: String,
                   style: ChartStyle,
                   data: ChartData,
                   width: Int = 500,
                   height: Int = 300)

  class Backend(bs: BackendScope[Props, Unit]) {

    def mounted(p: Props) =
      for {
        node <- bs.getDOMNode
        _ <- Callback {
          // access context of the canvas
          // TODO: Fix the horrendous hack!
          val ctx = node.asInstanceOf[HTMLCanvasElement].getContext("2d")
          // create the actual chart using the 3rd party component
          p.style match {
            case LineChart => new JSChart(ctx, ChartConfiguration("line", p.data))
            case BarChart => new JSChart(ctx, ChartConfiguration("bar", p.data))
            case _ => throw new IllegalArgumentException
          }
        }
      } yield ()

    def render(p: Props) = <.canvas(VdomAttr("width") := p.width, VdomAttr("height") := p.height)
   
  }

  val component = ScalaComponent.builder[Props]("Chart")
    .renderBackend[Backend]
    .componentDidMount(scope => scope.backend.mounted(scope.props))
    .build
}

