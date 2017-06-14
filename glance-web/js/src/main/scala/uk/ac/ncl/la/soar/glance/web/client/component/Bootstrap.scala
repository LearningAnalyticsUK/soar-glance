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
import org.scalajs.jquery.{JQuery, JQueryEventObject, JQueryStatic}

import scala.scalajs.js
import scalacss.ScalaCssReact._
import scalacss.DevDefaults._
import uk.ac.ncl.la.soar.glance.web.client.style.GlobalStyle

import scala.scalajs.js.annotation.JSImport
import scalacss.StyleA

/**
  * Wraps all Bootstrap/jQuery extensions in Scalajs-react components for easy integration
  *
  * Much of the ScalaCSS code in this project has been cribbed wholesale from ochrons' great
  * [[https://github.com/ochrons/scalajs-spa-tutorial scala-spa-tutorial]]
  */
object Bootstrap {

  @inline private def bss = GlobalStyle.bootstrapStyle

  @js.native
  @JSImport("jquery", JSImport.Namespace)
  object jquery extends JQueryStatic

  @js.native
  trait BootstrapJQuery extends JQuery {
    def modal(action: String): BootstrapJQuery = js.native
    def modal(options: js.Any): BootstrapJQuery = js.native
  }

  implicit final def jq2bootstrap(jq: JQuery): BootstrapJQuery = jq.asInstanceOf[BootstrapJQuery]
  //The below is not necessary in spa-tutorial. TODO: Check implicit conversion interaction with Seq
  implicit final def seqStylesToTagMod(styles: Seq[StyleA]): TagMod = styles.map(s => ^.className := s.htmlClass).toTagMod

  /** Common Bootstrap contextual styles */
  // Why an Enumeration rather than a sealed ADT of objects?
  object CommonStyle extends Enumeration {
    val default, primary, success, info, warning, danger = Value
  }

  /** Bootstrap button component */
  object Button {

    case class Props(onClick: Callback, style: CommonStyle.Value = CommonStyle.default, addStyles: Seq[StyleA] = Seq())

    val component = ScalaComponent.builder[Props]("Button")
      .renderPC((_, p, c) =>
        <.button(bss.buttonOpt(p.style), p.addStyles, ^.tpe := "button", ^.onClick --> p.onClick, c)
      ).build

    def apply(props: Props, children: VdomNode*) = component(props)(children: _*)
    def apply() = component
  }

  /** Bootstrap panel component */
  object Panel {

    case class Props(heading: String, style: CommonStyle.Value = CommonStyle.default)

    val component = ScalaComponent.builder[Props]("Panel")
      .renderPC((_, p, c) =>
        <.div(bss.panelOpt(p.style),
          <.div(bss.panelHeading, p.heading),
          <.div(bss.panelBody, c)
        )
      ).build

    def apply(props: Props, children: VdomNode*) = component(props)(children: _*)
    def apply() = component
  }

  /** Bootstrap Modal component */
  object Modal {

    // header and footer are functions, so that they can get access to the the hide() function for their buttons
    case class Props(header: Callback => VdomNode, footer: Callback => VdomNode, closed: Callback,
                     backdrop: Boolean = true, keyboard: Boolean = true)

    class Backend(t: BackendScope[Props, Unit]) {
      def hide = t.getDOMNode.map {n =>
        jquery(n).modal("hide")
        ()
      }

      // jQuery event handler to be fired when the modal has been hidden
      def hidden(e: JQueryEventObject): js.Any = {
        // inform the owner of the component that the modal was closed/hidden
        t.props.flatMap(_.closed).runNow()
      }

      def render(p: Props, c: PropsChildren) = {
        val modalStyle = bss.modal
        <.div(modalStyle.modal, modalStyle.fade, ^.role := "dialog", ^.aria.hidden := true,
          <.div(modalStyle.dialog,
            <.div(modalStyle.content,
              <.div(modalStyle.header, p.header(hide)),
              <.div(modalStyle.body, c),
              <.div(modalStyle.footer, p.footer(hide))
            )
          )
        )
      }
    }

    val component = ScalaComponent.builder[Props]("Modal")
      .renderBackendWithChildren[Backend]
      .componentDidMount(scope => Callback {
        val p = scope.props
        // instruct Bootstrap to show the modal
        jquery(scope.getDOMNode).modal(js.Dynamic.literal("backdrop" -> p.backdrop, "keyboard" -> p.keyboard, "show" -> true))
        // register event listener to be notified when the modal is closed
        jquery(scope.getDOMNode).on("hidden.bs.modal", null, null, scope.backend.hidden _)
      })
      .build

    def apply(props: Props, children: VdomElement*) = component(props)(children: _*)
    def apply() = component
  }


}
