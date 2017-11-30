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
package uk.ac.ncl.la.soar.glance.eval

/** Simple struct for containing Visualisation description
  *
  * TODO: Investigate how Doobie would behave if this was a normal sealed ADT, as this encoding with extractors is silly
  */
case class VisualisationType(id: String, name: String, description: String)

object VisualisationType {

  @inline final def factory(id: String): Option[VisualisationType] = all.find(_.id == id)

  val histPerfBars = VisualisationType("historic_performance_bars", "Historic Performance Bars", "")
  val clusterVTime = VisualisationType("cluster_vs_time", "Cluster Usage vs Time", "")
  val recapVTime = VisualisationType("recap_vs_time", "Recap Usage vs Time", "")
  val avgVTime = VisualisationType("avg_trend_vs_time", "Trend in Student Average vs Time", "")

  val all = List(histPerfBars, clusterVTime, recapVTime, avgVTime)

  /** Extractors to help with pattern matching viz types throughout code, no exhaustiveness checking though */
  object HistPerfBars {
    def unapply(viz: VisualisationType): Boolean = viz == histPerfBars
  }

  object ClusterVTime {
    def unapply(viz: VisualisationType): Boolean = viz == clusterVTime
  }

  object RecapVTime {
    def unapply(viz: VisualisationType): Boolean = viz == recapVTime
  }

  object AvgVTime {
    def unapply(viz: VisualisationType): Boolean = viz == avgVTime
  }
  
}

