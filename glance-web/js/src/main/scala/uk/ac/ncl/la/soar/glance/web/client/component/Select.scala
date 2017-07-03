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

  case class Choice[A](value: A, label: String, disabled: Boolean = false)

  case class Props[A](selected: Choice[A], choices: IndexedSeq[Choice[A]], select: Choice[A] => Callback)

  class Backend[A](bs: BackendScope[Props[A], Unit]) {

    def mounted = Callback {}
    def willRecieveProps = Callback {}

    def render(p: Props[A]): VdomElement = {
      //Create Array of option elements
      val oBldr = ListBuffer.empty[VdomElement]
      var i = 0
      //Flag checking if we've seen the selected value yet
      var seenSelected = false
      var selected = -1
      for (c <- p.choices) {
        oBldr += <.option(^.value := i, ^.key := i, ^.disabled := c.disabled, c.label)
        if(!seenSelected && c.label == p.selected.label) {
          seenSelected = true
          selected = i
        }
        i += 1
      }

      //TODO: Clean up the below a bit - lots of wrapping and unwrapping.
      def onChange: SyntheticEvent[HTMLSelectElement] => Option[Callback] = { event =>
        for {
          j <- Try(event.target.value.toInt).toOption
          v = p.choices(j)
          fn <- Option(p.select)
        } yield fn(v)
      }

      val options = oBldr.result()

      <.select(
        ^.className := "form-control",
        ^.value := selected,
        ^.onChange ==>? onChange,
        options.toTagMod
      )
    }

  }


  def component[A](p: Props[A]) = ScalaComponent.builder[Props[A]]("Select")
    .backend(new Backend(_))
    .renderBackend
    .componentDidMount(scope => scope.backend.mounted)
    .build
    .apply(p)
}

