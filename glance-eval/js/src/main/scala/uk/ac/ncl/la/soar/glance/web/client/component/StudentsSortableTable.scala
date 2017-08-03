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
import uk.ac.ncl.la.soar.data.StudentRecords
import uk.ac.ncl.la.soar.{ModuleCode, StudentNumber}
import uk.ac.ncl.la.soar.glance.web.client.component.sortable._

import scala.collection.immutable.SortedMap

object StudentsSortableTable {

  case class Props(rankModule: ModuleCode,
                   queryRecords: Seq[StudentRecords[SortedMap, ModuleCode, Double]],
                   headings: Seq[String],
                   renderCell: (StudentRecords[SortedMap, ModuleCode, Double], ModuleCode) => String,
                   selectStudent: StudentRecords[SortedMap, ModuleCode, Double] => Callback)

  type State = List[StudentRecords[SortedMap, ModuleCode, Double]]

  // As in original SortableComponent
  class Backend(bs: BackendScope[Props, List[String]]) {

    // Equivalent of ({value}) => <li>{value}</li> in original demo
    private val itemView = ScalaComponent.builder[String]("liView")
      .render(d => {
        <.div(
          ^.className := "react-sortable-item",
          SortableView.handle,
          <.span(s"${d.props}")
        )
      })
      .build


    // As in original demo
    private val sortableItem = SortableElement.wrap(itemView)

    // Equivalent of the `({items}) =>` lambda passed to SortableContainer in original demo
    private val listView = ScalaComponent.builder[List[String]]("listView")
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

    private val tableView = ScalaComponent.builder[Props]("TableView")
      .render(bs => {
        <.table(
          ^.className := "react-sortable-list table table-striped table-bordered table-hover",
          ^.id := "ranking-table",
          <.thead(
            <.tr(
              TagMod.fromTraversableOnce(bs.props.headings.map(<.td(_)))
            )
          ),
          <.tbody(
            TagMod.fromTraversableOnce(bs.state.map(itemTrView(bs.props, _)))
          )
        )
      })
      .build

    private def itemTrView(p: Props, record: StudentRecords[SortedMap, ModuleCode, Double]) = {
      <.tr(
        ^.className := "react-sortable-item",
        ^.onClick --> p.selectStudent(record),
        SortableView.handle,
        p.renderCell(record, p.rankModule)
      )
    }

    // As in original demo
    private val sortableTable = SortableContainer.wrap(tableView)

    def render(props: Props, items: State) = {
      sortableTable(
        SortableContainer.Props(
          onSortEnd = p =>
            bs.modState(
              l => p.updatedList(l)
            ),
          useDragHandle = true,
          helperClass = "react-sortable-handler"
        )
      )(items)
    }
  }

  val defaultItems = Range(0, 10).map("Item " + _).toList

  val component = ScalaComponent.builder[Props]("SortableContainerDemo")
    .initialStateFromProps(p => p.queryRecords)
    .renderBackend[Backend]
    .build

}

//object StudentSortableRowView {
//
//  case class Props(rankModule: ModuleCode,
//                   record: StudentRecords[SortedMap, ModuleCode, Double],
//                   renderCell: (StudentRecords[SortedMap, ModuleCode, Double], ModuleCode) => String,
//                   selectStudent: StudentRecords[SortedMap, ModuleCode, Double] => Callback)
//
//
//  val component = ScalaComponent.builder[Props]("StudentSortableRow")
//    .stateless
//    .render(bs => {
//      <.tr(
//        ^.className := "react-sortable-item",
//        ^.onClick --> bs.props.selectStudent(bs.props.record),
//        SortableView.handle,
//        bs.props.renderCell(bs.props.record, bs.props.rankModule)
//      )
//    })
//    .build
//}

object StudentSortableTableView {

}
