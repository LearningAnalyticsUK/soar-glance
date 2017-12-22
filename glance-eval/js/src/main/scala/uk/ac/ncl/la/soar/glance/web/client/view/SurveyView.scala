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
import cats.data.NonEmptyVector
import cats.implicits._
import diode.data._
import diode.react.ReactPot._
import diode.react._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import uk.ac.ncl.la.soar.{ModuleCode, StudentNumber}
import uk.ac.ncl.la.soar.data.{Module, StudentRecords}
import uk.ac.ncl.la.soar.glance.eval.{Collection, Survey}
import uk.ac.ncl.la.soar.glance.web.client._
import uk.ac.ncl.la.soar.glance.web.client.component._
import uk.ac.ncl.la.soar.glance.web.client.component.chart.StudentChartsContainer
import uk.ac.ncl.la.soar.glance.web.client.data.CohortAttainmentSummary
import uk.ac.ncl.la.soar.glance.web.client.style.Icon

import scala.collection.immutable.SortedMap
import scala.scalajs.js

/**
  * React Component for the SurveyView
  */
object SurveyView {

  case class Props(proxy: ModelProxy[Pot[SurveyModel]], ctrl: RouterCtl[Main.SurveyLoc])

  //TODO: Start thinking about moving this state to the Circuit model, not just external (remote) resources.
  case class State(selectedL: Option[StudentRecords[SortedMap, ModuleCode, Double]],
                   selectedR: Option[StudentRecords[SortedMap, ModuleCode, Double]],
                   selectingR: Boolean)

  class Backend(bs: BackendScope[Props, State]) {

    def mounted(props: Props) = Callback {}

    def handleStudentClick(student: StudentRecords[SortedMap, ModuleCode, Double]) =
      bs.modState { s =>
        if (s.selectingR)
          s.copy(selectedR = student.some)
        else
          s.copy(selectedL = student.some)
      }

    def handleClearStudent =
      bs.modState { s =>
        if (s.selectingR)
          s.copy(selectedR = None)
        else
          s.copy(selectedL = None)
      }

    def handleToggleSelecting(right: Boolean) = bs.modState(s => s.copy(selectingR = right))

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
    private def queryStudents(survey: Survey) =
      survey.entries.filter(r => survey.queries.contains(r.number))

    /** Construct the function which provides the presentation of a table cell, given a StudentRecord and string key */
    private def renderCell(default: String)(student: StudentRecords[SortedMap, ModuleCode, Double],
                                            key: String) =
      key match {
        case k if k == indexCol => student.number
        case k                  => student.record.get(k).fold(default)(_.toString)
      }

    /** Construct the NEL of filters from the survey model, to be passed to the charts component */
    private def buildFilters(modules: Iterable[Module]) = {
      val modKeywordPairs = modules.flatMap(m => m.keywords.map(_ -> m.code))
      val keywordModSets = modKeywordPairs.groupBy(_._1).mapValues(_.map(_._2).toSet)
      NonEmptyVector(
        Select.Choice((_: ModuleCode, _: Double) => true, "None"),
        keywordModSets
          .map({
            case (keyword, modSet) =>
              Select.Choice((mc: ModuleCode, _: Double) => modSet.contains(mc), keyword)
          })
          .toVector
      )
    }

    private val rankingTable =
      ScalaComponent
        .builder[(ModelProxy[Pot[SurveyModel]], State)]("RankingTable")
        .render($ => {
          val (proxy, state) = $.props
          val model = proxy()

          val focused = (state.selectedL, state.selectedR)

          <.div(
            ^.id := "ranking",
            <.span(
              ^.className := "sub-title",
              Icon.listOl(Icon.Medium),
              <.h2("Rank students")
            ),
            model.render {
              sm =>
                val rankModule = sm.survey.moduleToRank
                <.div(
                  ^.classSet1(
                    "table-responsive",
                    "selecting" -> !state.selectingR,
                    "comparing" -> state.selectingR
                  ),
                  StudentsSortableTable.component(
                    StudentsSortableTable.Props(
                      rankModule,
                      queryStudents(sm.survey),
                      headings(sm.survey, sm.modules),
                      renderCell(" "),
                      handleStudentClick,
                      (ranks, change) => proxy.dispatchCB(ChangeRanks(ranks, change)),
                      focused
                    )
                  )
                )
            }
          )
        })
        .build

    def render(p: Props, s: State): VdomElement = {
      //Get the necessary data from the model
      val model = p.proxy()

      //TODO: Why are these lazy - any benefit?
      lazy val detailedView = {
        <.div(
          <.span(
            ^.className := "sub-title",
            Icon.search(Icon.Medium),
            <.h2("Detailed View")
          ),
          model.render { sm =>
            StudentChartsContainer.component(
              StudentChartsContainer.Props(
                s.selectedL,
                s.selectedR,
                s.selectingR,
                handleClearStudent,
                handleToggleSelecting,
                sm.data,
                sm.survey.visualisations,
                buildFilters(sm.modules.values)
              )
            )
          }
        )
      }

      lazy val submissionForm = {
        <.div(
          <.span(
            ^.className := "sub-title",
            Icon.save(Icon.Medium),
            <.h2("Submit Survey")
          ),
          model.render { sm =>
            val smConnector = p.proxy.connect(_.get)
            SurveyResponseForm.component(
              SurveyResponseForm.Props(
                p.proxy, {
                  case Some(r) =>
                    p.proxy.dispatchCB(SubmitSurveyResponse(r)) >> p.ctrl.set(
                      Main.SurveyCompleteLoc)
                  case None => Callback.empty
                }
              )
            )
          }
        )
      }

      <.div(
        model.render { sm =>
          val rankModule = sm.survey.moduleToRank
          <.div(
            ^.className := "alert alert-success welcome-banner",
            ^.role := "alert",
            <.p(
              <.strong("Welcome"),
              " Please rank students below by how you believe they will perform in the module ",
              <.strong(
                s"$rankModule: ${sm.modules.get(rankModule).flatMap(m => m.title).getOrElse("")}"),
              ". Higher is better."
            ),
            //Why is the type annotation necessary below?
            <.p(<.strong("Module aims: "),
                sm.modules.get(rankModule).flatMap(m => m.description).getOrElse(""): String),
            <.p(
              <.strong("Module keywords: "),
              sm.modules.get(rankModule).fold(List.empty[String])(_.keywords).mkString(", ")
            )
          )
        },
        <.div(
          ^.id := "training",
          rankingTable((p.proxy, s))
        ),
        detailedView,
        submissionForm
      )

    }

  }

  val component = ScalaComponent
    .builder[Props]("SurveyView")
    .initialStateFromProps(p => State(None, None, selectingR = false))
    .renderBackend[Backend]
    .componentDidMount(scope => scope.backend.mounted(scope.props))
    .build

}

/**
  * React Component for the SurveyCompleteView, shown after a Survey is submitted
  */
object SurveyCompleteView {
  val component =
    ScalaComponent
      .builder[(ModelProxy[Pot[Collection]], RouterCtl[Main.Loc])]("Survey Complete")
      .render_P({ p =>
        val (model, ctl) = p

        <.div(
          ^.className := "row",
          model().render {
            c =>
              val callToAction = {
                if (c.currentIsLast) {
                  <.p("We have no more surveys at this time. Thank you for all your help!")
                } else {
                  List(
                    <.p("If you have time, please consider doing another one."),
                    <.p("Otherwise, if you'd like to resume at a later date, please do so using " +
                      "the link below. This helps us track which surveys you have completed."),
                    <.div(
                      ^.className := "input-group",
                      <.div(
                        ^.className := "bootstrap-tagsinput",
                        <.span(ctl.urlFor(Main.CollectionIdxLoc(c.id, c.currentIdx + 1)).value)
                      ),
                      <.div(
                        ^.className := "input-group-btn",
                        <.button(
                          ^.`type` := "button",
                          ^.className := "btn btn-primary",
                          Icon.copy(Icon.Small)
                        )
                      )
                    ),
                    <.button(
                      ^.`type` := "button",
                      ^.className := "btn btn-primary",
                      "Next Survey",
                      ^.onClick --> {
                        model.dispatchCB(
                          NextCollectionSurvey(ctl.set(Main.NextCollectionLoc(c.id))))
                      }
                    )
                  ).toTagMod
                }
              }

              <.div(
                ^.className := "col-md-12",
                <.h4("Thank you for completing a survey"),
                callToAction
              )
          }
        )
      })
      .build
}
