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
package uk.ac.ncl.la.soar.glance.web.client.view

import uk.ac.ncl.la.soar.glance.web.client.Main
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import uk.ac.ncl.la.soar.glance.web.client.Main._

/** Description of Class
  *
  * @author hugofirth
  */
object MainMenuView {

  case class Props(router: RouterCtl[Main.Loc], currentLoc: Loc)

  private case class MenuItem(idx: Int, label: String, target: Loc, collapsed: Boolean)

  private val menuItems = Seq(
    MenuItem(1, "Modules", ModuleLoc, collapsed = false),
    MenuItem(2, "Students", StudentLoc, collapsed = false),
    MenuItem(3, "About", AboutLoc, collapsed = false),
    MenuItem(4, "Settings", SettingsLoc, collapsed = true),
    MenuItem(5, "Logout", SettingsLoc, collapsed = true)
  )

  class Backend(bs: BackendScope[Props, Unit]) {

    def mounted(p: Props) = Callback {}

    def render(p: Props): VdomElement = {
      <.ul(
        ^.id := "main-menu",
        ^.className := "nav navbar-nav",
        menuItems.map({ item =>
          <.li(
            ^.key := item.idx,
            (^.className := "active").when(p.currentLoc == item.target),
//            p.router.link(item.target)(item.label)
            <.a(
              ^.href := "#",
              item.label
            )
          )
        }).toTagMod
      )
    }
  }

  val component = ScalaComponent.builder[Props]("MainMenu")
    .renderBackend[Backend]
    .build
  
}
