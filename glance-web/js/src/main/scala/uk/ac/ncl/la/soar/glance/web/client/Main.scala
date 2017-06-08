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

import org.scalajs.dom.raw.Node
import org.scalajs.dom
import cats._
import cats.implicits._
import io.circe._
import japgolly.scalajs.react.extra.router._
import japgolly.scalajs.react.vdom.TopNode
import uk.ac.ncl.la.soar.glance.Survey

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js

/**
  * Entry point for client program
  */

object Main extends js.JSApp {


  /** Define routes for glance dashboard SPA */
  val baseUrl = BaseUrl.until_#


  /** Define the locations (views) used in this application */
  sealed trait Loc
  case object StudentListLoc extends Loc
  case object CohortGlanceLoc extends Loc
  case object StudentGlanceLoc extends Loc
  case object DashboardLoc extends Loc

  /** Lets initialise the router config */
  val routerConfig = RouterConfigDsl[Loc].buildConfig { dsl =>
    import dsl._

    //Construct student list Route
    val listRt = staticRoute(root, StudentListLoc) ~> renderR(router => GlanceCircuit.connect(_.students)(proxy => ???))

    //Construct and return final routing table, adding a "Not Found" behaviour
    listRt.notFound(redirectToPage(StudentListLoc)(Redirect.Replace))

  }

  /** Mount the router React Component */
  val router: ReactComponentU[Unit, Resolution[Loc], Any, TopNode] = Router(baseUrl, routerConfig.logToConsole)()

  //Lets get the survey data
  val surveysJson = ApiClient.loadSurveys

//  val baseUrl = BaseUrl(dom.window.location.href.takeWhile(_ != '#'))
//
//  val routerConfig: RouterConfig[TodoFilter] = RouterConfigDsl[TodoFilter].buildConfig { dsl =>
//    import dsl._
//
//    /* how the application renders the list given a filter */
//    def filterRoute(s: TodoFilter): Rule = staticRoute("#/" + s.link, s) ~> renderR(router => AppCircuit.connect(_.todos)(p => TodoList(p, s, router)))
//
//    val filterRoutes: Rule = TodoFilter.values.map(filterRoute).reduce(_ | _)
//
//    /* build a final RouterConfig with a default page */
//    filterRoutes.notFound(redirectToPage(TodoFilter.All)(Redirect.Replace))
//  }

}
