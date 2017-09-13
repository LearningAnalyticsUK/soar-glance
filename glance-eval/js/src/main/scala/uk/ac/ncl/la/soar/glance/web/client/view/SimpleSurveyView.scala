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

import cats._
import cats.implicits._
import diode.react.ModelProxy
import uk.ac.ncl.la.soar.ModuleCode
import uk.ac.ncl.la.soar.data.{Module, StudentRecords}
import uk.ac.ncl.la.soar.glance.eval.Survey
import uk.ac.ncl.la.soar.glance.web.client.style.Icon
import uk.ac.ncl.la.soar.glance.web.client.{ChangeRanks, Main, ProgressSimpleSurvey, SurveyModel}
import diode.data._
import diode.react.ReactPot._
import diode.react._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import uk.ac.ncl.la.soar.glance.web.client.component.StudentsSortableTable

import scala.collection.immutable.SortedMap

/**
  * React Component for Simple Survey View
  */
object SimpleSurveyView {

  case class Props(proxy: ModelProxy[Pot[SurveyModel]], ctrl: RouterCtl[Main.SurveyLoc])

  class Backend(bs: BackendScope[Props, Unit]) {

    val indexCol = "Student Number"

    /** Construct the presentation of the modules as a sorted list to fill some table headings */
    private def modules(survey: Survey, moduleInfo: Map[ModuleCode, Module]) =
      survey.modules.map(k => k -> moduleInfo.get(k).flatMap(_.title)).toList.sorted

    /** Construct the full presentation of table headings, including modules and tool tips */
    private def headings(survey: Survey, moduleInfo: Map[ModuleCode, Module]) =
      (indexCol, none[String]) :: modules(survey, moduleInfo)

    /** Construct the presentation of the students to fill table rows */
    private def students(survey: Survey) = survey.entries

    /** Construct the presentation of the query students to fill the rankable table rows */
    private def queryStudents(survey: Survey) = survey.entries.filter( r => survey.queries.contains(r.number) )

    /** Construct the function which provides the presentation of a table cell, given a StudentRecord and string key */
    private def renderCell(default: String)(student: StudentRecords[SortedMap, ModuleCode, Double], key: String) =
      key match {
        case k if k == indexCol => student.number
        case k => student.record.get(k).fold(default)(_.toString)
      }

    private val rankingTable =
      ScalaComponent.builder[ModelProxy[Pot[SurveyModel]]]("RankingTable")
        .render($ => {
          val proxy = $.props
          val model = proxy()

          <.div(
            ^.id := "ranking",
            <.span(
              ^.className := "sub-title",
              Icon.listOl(Icon.Medium),
              <.h2("Rank students")
            ),
            model.render { sm =>

              val rankModule = sm.survey.moduleToRank
              <.div(
                ^.className := "table-responsive",
                StudentsSortableTable.component(
                  StudentsSortableTable.Props(
                    rankModule,
                    queryStudents(sm.survey),
                    headings(sm.survey, sm.modules),
                    renderCell(" "),
                    _ => Callback.empty,
                    (ranks, change) => proxy.dispatchCB(ChangeRanks(ranks, change))
                  )
                )
              )
            }
          )
        })
        .build

    def render(p: Props): VdomElement = {
      //Get the necessary data from the model
      val model = p.proxy()
      //TODO: find a neater way to do the below than two rendering blocks.
      <.div(
        model.render { sm =>
          val rankModule = sm.survey.moduleToRank
          <.div(
            ^.className := "alert alert-success welcome-banner",
            ^.role := "alert",
            <.p(
              <.strong("Welcome"),
              " Please rank students below by how you believe they will perform in the module ",
              <.strong(s"$rankModule: ${sm.modules.get(rankModule).flatMap(m => m.title).getOrElse("")}"),
              ". Higher is better."
            ),
            //Why is the type annotation necessary below?
            <.p(<.strong("Module aims: "), sm.modules.get(rankModule).flatMap(m => m.description).getOrElse(""): String),
            <.p(
              <.strong("Module keywords: "),
              sm.modules.get(rankModule).fold(List.empty[String])(_.keywords).mkString(", ")
            )
          )
        },
        <.div(
          ^.id := "training",
          rankingTable(p.proxy)
        ),
        model.render { sm =>
          <.div(
            ^.className := "row",
            <.div(
              ^.className := "col-md-4 col-md-offset-8",
              <.button(
                ^.`type` := "button",
                ^.className := "btn btn-primary pull-right",
                "Continue",
                ^.onClick --> {
                  p.proxy.dispatchCB(ProgressSimpleSurvey) >> p.ctrl.set(Main.SurveyFormLoc(sm.survey.id))
                }
              )
            )
          )
        }
      )
    }
  }

  val component = ScalaComponent.builder[Props]("SimpleSurveyView")
    .renderBackend[Backend]
    .build

}
