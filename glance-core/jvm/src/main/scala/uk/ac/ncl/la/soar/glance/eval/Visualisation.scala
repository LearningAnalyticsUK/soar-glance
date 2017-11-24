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

/** Simple struct for containing Visualisation description */
case class Visualisation(id: String, name: String, description: String)

object Visualisation {

  val all = List(
    Visualisation("historic_performance_bars", "Historic Performance Bars", ""),
    Visualisation("cluster_vs_time", "Cluster Usage vs Time", ""),
    Visualisation("recap_vs_time", "Recap Usage vs Time", ""),
    Visualisation("avg_trend_vs_time", "Trend in Student Average vs Time", ""))

}

