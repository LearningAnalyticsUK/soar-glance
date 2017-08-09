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
package uk.ac.ncl.la.soar.glance.web.client

import cats._
import cats.implicits._
import diode.data.Pot
import diode.react._
import io.circe._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router._
import japgolly.scalajs.react.extra.OnUnmount
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom
import uk.ac.ncl.la.soar.glance.web.client.style.GlobalStyle
import uk.ac.ncl.la.soar.glance.web.client.view.{MainMenuView, SurveyView}

import scala.scalajs.js
import scalacss.DevDefaults._
import scalacss.ScalaCssReact._

/**
  * Entry point for client program
  */
object Main extends js.JSApp {

  /** Define routes for glance dashboard SPA */
  val baseUrl: BaseUrl = BaseUrl.until_#

  /** Define the locations (views) used in this application */
  sealed trait Loc
  case object StudentLoc extends Loc
  case object ModuleLoc extends Loc
  case object AboutLoc extends Loc
  case object SettingsLoc extends Loc

  /** Lets initialise the router config */
  val routerConfig: RouterConfig[Loc] = RouterConfigDsl[Loc].buildConfig({ dsl =>
    import dsl._

    val surveyConnector: ReactConnectProxy[Pot[SurveyModel]] = GlanceCircuit.connect(_.survey)
    //Construct student list Route
    val listRt =
      staticRoute(root, StudentLoc) ~> renderR(ctl => surveyConnector(p => SurveyView.component(p)))

    //Construct and return final routing table, adding a "Not Found" behaviour
    listRt.notFound(redirectToPage(StudentLoc)(Redirect.Replace))
  }).renderWith(layout)

  // base layout for all pages
  private def layout(c: RouterCtl[Loc], r: Resolution[Loc]) = {
    <.div(
      // here we use plain Bootstrap class names as these are specific to the top level layout defined here
      <.nav(^.className := "navbar navbar-inverse navbar-fixed-top",
        <.div(^.className := "container",
          <.div(^.className := "navbar-header",
            <.span(
              ^.className := "navbar-brand",
              ^.id := "title",
              <.img(^.src := "assets/ncl-shield.png"),
              "SOAR Glance - Evaluation")),
          <.div(^.className := "collapse navbar-collapse",
            MainMenuView.component(
              MainMenuView.Props(c, r.page)
            )
          )
        )
      ),
      // currently active module is shown in this container
      <.div(^.className := "container", ^.id := "app", r.render())
    )
  }

  /** Mount the router React Component */
  val router: ScalaComponent.Unmounted[Unit, Resolution[Loc], OnUnmount.Backend] = Router(baseUrl, routerConfig.logToConsole)()



  /** Main method where we kick everything off */
  override def main(): Unit = {
    //Load the styles
    GlobalStyle.addToDocument()
    //Load the survey data and render
    GlanceCircuit.dispatch(RefreshSurvey)
    //Find undeprecated way of doing this
    router.renderIntoDOM(dom.document.getElementById("soar-app"))
    ()
  }

}
