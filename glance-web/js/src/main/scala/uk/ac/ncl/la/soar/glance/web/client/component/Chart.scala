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

  def apply(data: Seq[Double],
            label: String, backgroundColor: Seq[String], borderColor: Seq[String]): ChartDataset = {
    js.Dynamic.literal(
      label = label,
      data = data.toJSArray,
      backgroundColor = backgroundColor.toJSArray,
      borderColor = borderColor.toJSArray
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
trait ChartLegendOptions extends js.Object {
  def display: Boolean = js.native
}

@js.native
trait ChartOptions extends js.Object {
  def responsive: Boolean = js.native
  def legend: ChartLegendOptions = js.native
}

object ChartOptions {
  def apply(responsive: Boolean = true): ChartOptions = {
    js.Dynamic.literal(
      responsive = responsive,
      legend = legend
    ).asInstanceOf[ChartOptions]
  }

  private val legend: ChartLegendOptions = {
    js.Dynamic.literal(
      display = false
    ).asInstanceOf[ChartLegendOptions]
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

@js.native
trait ChartWithData extends js.Object {
  def data: ChartData = js.native
  def update(): Unit = js.native
}

// define a class to access the Chart.js component
@js.native
@JSGlobal("Chart")
class JSChart(ctx: js.Dynamic, config: ChartConfiguration) extends js.Object with ChartWithData

object Chart {

  // available chart styles
  sealed trait ChartStyle
  case object LineChart extends ChartStyle
  case object BarChart extends ChartStyle

  case class State(chart: Option[JSChart])

  case class Props(name: String,
                   style: ChartStyle,
                   data: ChartData,
                   width: Option[Int] = Some(1110),
                   height: Option[Int] = Some(300))

  class Backend(bs: BackendScope[Props, State]) {

    def mounted(p: Props) =
      for {
        node <- bs.getDOMNode
        chart <- CallbackTo[JSChart] {
          // access context of the canvas
          // TODO: Fix the horrendous hack!
          val ctx = node.asInstanceOf[HTMLCanvasElement].getContext("2d")
          // create the actual chart using the 3rd party component
          p.style match {
            case LineChart => new JSChart(ctx, ChartConfiguration("line", p.data))
            case BarChart => new JSChart(ctx, ChartConfiguration("bar", p.data))
          }
        }
        _ <- bs.modState(s => State(Some(chart)))
      } yield ()

    def willRecieveProps(next: Props) = {
      //TODO: Clean this up a bit, could just be a single map statement rather than a for comprehension
      for {
        _ <- removeAllData
        _ <- addAllData(next.data)
        state <- bs.state
      } yield state.chart.foreach(_.update())
    }

    def render(p: Props) =
      <.canvas(
        ^.className := "chart",
        p.width.map(w => VdomAttr("width") := w).whenDefined,
        p.height.map(h => VdomAttr("height") := h).whenDefined)

    private def addAllData(newData: ChartData) = bs.state.map{s =>
      s.chart.foreach{c => addData(c, newData) }
    }

    private def addData(chart: JSChart, newData: ChartData) = {
      chart.data.labels.push(newData.labels:_*)
      chart.data.datasets.push(newData.datasets:_*)
    }

    //TODO: Perhaps an OptionT stack would work better here?
    private def removeAllData = bs.state.map{s =>
      s.chart.foreach{c => removeData(c) }
    }

    private def removeData(chart: JSChart) = {
      for(i <- 0 until chart.data.datasets.length) { chart.data.datasets.pop() }
      for(i <- 0 until chart.data.labels.length) { chart.data.labels.pop() }
    }
  }

  val component = ScalaComponent.builder[Props]("Chart")
    .initialState(State(None))
    .renderBackend[Backend]
    .componentDidMount(scope => scope.backend.mounted(scope.props))
    .componentWillReceiveProps(scope => scope.backend.willRecieveProps(scope.nextProps))
    .build
}

