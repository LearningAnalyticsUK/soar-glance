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
package uk.ac.ncl.la.soar.glance.web.client.data

import java.util.UUID

import io.circe._
import cats._
import cats.implicits._
import cats.data.EitherT
import uk.ac.ncl.la.soar.glance.eval.{SessionSummary, VisualisationType}
import uk.ac.ncl.la.soar.glance.web.client.ApiClient
import uk.ac.ncl.la.soar.glance.web.client.component.chart.StudentChart

import scala.concurrent.Future

/**
  * Sealed ADT defining actual visualisations
  *
  * At the moment this ADT is not being used. Leaving it here for now to reflect on its flaws and the possible
  * Isomorphism to Reader before I finally pull it out.
  * TODO: Remove the Visualisation ADT.
  */
sealed trait Visualisation {

  def tpe: VisualisationType
  def ui: StudentChart
}

/**
  * Companion object for Visualisation
  */
object Visualisation {

  /** Type Aliases for Clarity */
  type DataSource[D] = UUID => EitherT[Future, Error, D]

  /** Members of the ADT */
  case class Unpopulated[D](tpe: VisualisationType, datasource: DataSource[D], ui: StudentChart) extends Visualisation


  case class Populated[D](tpe: VisualisationType, data: D, ui: StudentChart) extends Visualisation

  /** Factory method for producing Visualisation instances given `VisualisationType`s */

  @inline final def apply(tpe: VisualisationType) = tpe match {
    //Noop datasource in the event of a HistPerfBars/AvgVTime chart as no extra info needed
    case t @ VisualisationType.HistPerfBars() =>
      Unpopulated[Unit](t, _ => ApiClient.noopT, StudentChart.HistPerfBars)
    case t @ VisualisationType.AvgVTime() =>
      Unpopulated[Unit](t, _ => ApiClient.noopT, StudentChart.AvgTrendLine)
    case t @ VisualisationType.ClusterVTime() =>
      Unpopulated[SessionSummary](t, ApiClient.loadClustersT, StudentChart.ClusterUsageLine)
    case t @ VisualisationType.RecapVTime() =>
      Unpopulated[SessionSummary](t, ApiClient.loadRecapsT, StudentChart.RecapUsageLine)

  }
}


