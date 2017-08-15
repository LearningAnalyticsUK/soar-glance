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

import cats._
import cats.implicits._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import uk.ac.ncl.la.soar.data.StudentRecords
import uk.ac.ncl.la.soar.{ModuleCode, StudentNumber}
import uk.ac.ncl.la.soar.glance.web.client.component.sortable._

import scala.collection.immutable.SortedMap

object StudentsSortableTable {

  type Record = StudentRecords[SortedMap, ModuleCode, Double]

  case class Props(rankModule: ModuleCode,
                   queryRecords: List[Record],
                   headings: List[String],
                   renderCell: (Record, String) => String,
                   selectStudent: Record => Callback) {

    val rankModuleIdx = headings.indexWhere(_ == rankModule)
  }

  // As in original SortableComponent
  class Backend(bs: BackendScope[Props, List[Record]]) {

    private def tableView(wrappedP: Props) = ScalaComponent.builder[List[Record]]("TableView")
      .render(bs => {
        <.table(
          ^.className := "react-sortable-list table table-bordered table-hover",
          ^.id := "ranking-table",
          <.thead(
            <.tr(
              wrappedP.headings match {
                case hd :: tl =>
                  (<.th(" ") :: <.th(hd) :: tl.map { h =>
                    <.th(
                      if(h == wrappedP.rankModule) {
                        ^.className := "warning long-heading"
                      } else {
                        ^.className := "long-heading"
                      },
                      <.span(h))
                  }).toTagMod
                case a =>
                  a.toTagMod
              }
            )
          ),
          <.tbody(
            bs.props.zipWithIndex.toTagMod { case (value, index) =>
              sortableTr(wrappedP)(SortableElement.Props(index = index))(value)
            }
          )
        )
      })
      .build

    private def trView(wrappedP: Props) = ScalaComponent.builder[Record]("TrView")
      .render(bs => {
        //Get the row columns for the given record
        val columns = wrappedP.headings.map(h => wrappedP.renderCell(bs.props, h))

        val renderedColumns = columns.iterator.zipWithIndex.map({ case (c, idx) =>
          <.td(
            (^.className := "warning").when(idx == wrappedP.rankModuleIdx),
            ^.onClick --> wrappedP.selectStudent(bs.props),
            c
          )
        }).toList

        <.tr(
          ^.className := "react-sortable-item",
          TagMod.fromTraversableOnce(<.td(SortableView.handle) :: renderedColumns)
        )
      })
      .build

    private def sortableTr(p: Props) = SortableElement.wrap(trView(p))

    // As in original demo
    private def sortableTable(p: Props) = SortableContainer.wrap(tableView(p))

    def render(props: Props, items: List[Record]) = {
      sortableTable(props)(
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


  val component = ScalaComponent.builder[Props]("SortableContainerDemo")
    .initialStateFromProps(p => p.queryRecords)
    .renderBackend[Backend]
    .build

}

