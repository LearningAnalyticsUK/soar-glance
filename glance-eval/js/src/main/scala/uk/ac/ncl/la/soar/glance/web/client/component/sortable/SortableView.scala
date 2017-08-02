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
package uk.ac.ncl.la.soar.glance.web.client.component.sortable

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

object SortableView {

  import japgolly.scalajs.react.vdom.SvgTags._
  import japgolly.scalajs.react.vdom.SvgAttrs._

  private val handleGrip = ScalaComponent.builder[Unit]("HandleGrip")
    .renderStatic(
      <.div(
        ^.className := "react-sortable-handle",
        svg(
          ^.className := "react-sortable-handle-svg",
          ^.width := "24px",
          ^.height := "24px",
          viewBox := "0 0 24 24",
          path(d := "M9,8c1.1,0,2-0.9,2-2s-0.9-2-2-2S7,4.9,7,6S7.9,8,9,8z M9,10c-1.1,0-2,0.9-2,2s0.9,2,2,2s2-0.9,2-2S10.1,10,9,10z M9,16c-1.1,0-2,0.9-2,2s0.9,2,2,2s2-0.9,2-2S10.1,16,9,16z"),
          path(d := "M15,8c1.1,0,2-0.9,2-2s-0.9-2-2-2s-2,0.9-2,2S13.9,8,15,8z M15,10c-1.1,0-2,0.9-2,2s0.9,2,2,2s2-0.9,2-2S16.1,10,15,10z M15,16c-1.1,0-2,0.9-2,2s0.9,2,2,2s2-0.9,2-2S16.1,16,15,16z")
        )
      )
    )
    .build

  val handle = SortableHandle.wrap(handleGrip)(())

}

