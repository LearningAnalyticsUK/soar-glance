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
import japgolly.scalajs.react.raw.SyntheticEvent
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.raw.HTMLSelectElement

import scala.collection.mutable.ListBuffer
import scala.scalajs.js
import scala.util.Try

object Select {

  case class Choice[A](value: A,
                       label: String,
                       disabled: Boolean = false)

  case class Props[A](selected: Choice[A],
                      choices: IndexedSeq[Choice[A]],
                      select: Choice[A] => Callback,
                      contents: Option[String] = None)

  class Backend[A](bs: BackendScope[Props[A], Unit]) {

    def render(p: Props[A]): VdomElement = {

      val onChange: Int => Callback = p.choices andThen p.select
      buildSelect(p.selected, p.choices, onChange, p.contents)
    }

    /** Build Select dom - either vanilla select or ul/li based depending on props override */
    //TODO: Consider whether we should stop using the List datatype even on these internal methods
    private def buildSelect(selected: Choice[A],
                            choices: IndexedSeq[Choice[A]],
                            onChange: Int => Callback,
                            contents: Option[String]): VdomElement = {

      val (options, selectedIdx) = buildOptions(selected, choices, onChange)

      <.div(
        ^.className := "input-group-btn",
        <.button(
          ^.`type` := "button",
          ^.className := "btn btn-default dropdown-toggle",
          VdomAttr("data-toggle") := "dropdown",
          ^.aria.hasPopup := true,
          ^.aria.expanded := false,
          (contents.getOrElse(choices(selectedIdx).label): String),
          <.span(^.className := "caret")
        ),
        <.ul(
          ^.className := "dropdown-menu",
          options.toTagMod
        )
      )
    }

    /** Construct a List of option vdom elements, as well as detecting the index which is to be selected */
    private def buildOptions(selected: Choice[A],
                             choices: IndexedSeq[Choice[A]],
                             onChange: Int => Callback): (List[VdomElement], Int) = {
      //Create Array of option elements
      val oBldr = ListBuffer.empty[VdomElement]
      var i = 0
      //Flag checking if we've seen the selected value yet
      var seenSelected = false
      var selectedIdx = -1
      for (c <- choices) {
        oBldr += buildOption(i, c.disabled, c.label, onChange)
        if (!seenSelected && c.label == selected.label) {
          seenSelected = true
          selectedIdx = i
        }
        i += 1
      }
      (oBldr.result(), selectedIdx)
    }

    private def buildOption(value: Int,
                            disabled: Boolean,
                            label: String,
                            onChange: Int => Callback): VdomElement = {

      //The below is needed if we have to reintroduce href tags to the links, for some reason
//      def change(value: Int)(e: ReactEvent): Callback = {
//        e.preventDefault()
//        e.nativeEvent.stopImmediatePropagation()
//        onChange(value)
//      }

      <.li(
        VdomAttr("data-select-index") := value,
        <.a(
          ^.onClick --> onChange(value),
          label
        )
      )
    }
  }

  def component[A](p: Props[A]) = ScalaComponent.builder[Props[A]]("Select")
    .backend(new Backend(_))
    .renderBackend
    .build
    .apply(p)
}

