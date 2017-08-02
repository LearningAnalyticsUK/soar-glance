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

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import uk.ac.ncl.la.soar.glance.web.client.component.sortable._

object StudentsSortableTable {

  // Equivalent of ({value}) => <li>{value}</li> in original demo
  val itemView = ScalaComponent.builder[String]("liView")
    .render(d => {
      <.div(
        ^.className := "react-sortable-item",
        SortableView.handle,
        <.span(s"${d.props}")
      )
    })
    .build

  // As in original demo
  val sortableItem = SortableElement.wrap(itemView)

  // Equivalent of the `({items}) =>` lambda passed to SortableContainer in original demo
  val listView = ScalaComponent.builder[List[String]]("listView")
    .render(d => {
      <.div(
        ^.className := "react-sortable-list",
        d.props.zipWithIndex.toTagMod {
          case (value, index) =>
            sortableItem(SortableElement.Props(index = index))(value)
        }
      )
    })
    .build

  // As in original demo
  val sortableList = SortableContainer.wrap(listView)

  // As in original SortableComponent
  class Backend(scope: BackendScope[Unit, List[String]]) {
    def render(props: Unit, items: List[String]) = {
      sortableList(
        SortableContainer.Props(
          onSortEnd = p =>
            scope.modState(
              l => p.updatedList(l)
            ),
          useDragHandle = true,
          helperClass = "react-sortable-handler"
        )
      )(items)
    }
  }

  val defaultItems = Range(0, 10).map("Item " + _).toList

  val component = ScalaComponent.builder[Unit]("SortableContainerDemo")
    .initialState(defaultItems)
    .backend(new Backend(_))
    .render(s => s.backend.render(s.props, s.state))
    .build

}
