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
package uk.ac.ncl.la.soar.glance.web.client.component.clipboard

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomNode
import scala.scalajs.js
import js.JSConverters._
import japgolly.scalajs.react.component.Js.{RawMounted, UnmountedMapped}
import japgolly.scalajs.react.internal.Effect.Id

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

import raw._
import japgolly.scalajs.react.Callback

/**
  * Lifted straight from https://github.com/cquiroz/scalajs-react-clipboard while
  * I work on a pull request to cross publish that lib for 2.11
  */
object CopyToClipboard {
  @js.native
  @JSImport("react-copy-to-clipboard", JSImport.Namespace, "CopyToClipboard")
  object RawComponent extends js.Object

  @js.native
  trait Props extends js.Object {

    /** Text to be copied to clipboard */
    def text: String = js.native

    /** Optional callback, will be called when text is copied */
    def onCopy: RawOnCopy = js.native

    /** Optional copy-to-clipboard options. */
    def options: js.UndefOr[ClipboardOptions] = js.native
  }

  def props(
      text: String,
      onCopy: OnCopy = (_, _) => Callback.empty,
      options: Option[ClipboardOptions] = None
  ): Props = {
    js.Dynamic
      .literal(
        text = text,
        onCopy = (s: String, b: Boolean) => onCopy(s, b).runNow(),
        options = options.orUndefined
      )
      .asInstanceOf[Props]
  }

  private val component = JsComponent[Props, Children.Varargs, Null](RawComponent)

  def apply(p: Props,
            children: VdomNode*): UnmountedMapped[Id, Props, Null, RawMounted, Props, Null] =
    component(p)(children: _*)
}
