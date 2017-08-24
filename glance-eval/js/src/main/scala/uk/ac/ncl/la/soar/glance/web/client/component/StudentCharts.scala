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

import uk.ac.ncl.la.soar.{ModuleCode, StudentNumber}
import diode.data._
import diode.react.ReactPot._
import diode.react._
import cats._
import cats.data.NonEmptyVector
import cats.implicits._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import uk.ac.ncl.la.soar.ModuleCode
import uk.ac.ncl.la.soar.data.StudentRecords
import uk.ac.ncl.la.soar.glance.eval.SessionSummary
import uk.ac.ncl.la.soar.glance.web.client.data.CohortAttainmentSummary
import uk.ac.ncl.la.soar.glance.web.client.style.Icon
import uk.ac.ncl.la.soar.glance.util.Times
import scala.collection.immutable.SortedMap
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.scalajs.js

/**
  * Simple component for rendering charts describing some stat for an individual student
  */
object StudentCharts {

  type Filter = (ModuleCode, Double) => Boolean

  // Default options for filter component prototype, will be read in as part of props eventually.
  private val options: NonEmptyVector[Select.Choice[Filter]] =
    NonEmptyVector(
      Select.Choice((_, _) => true, "None"),
      Vector(
        Select.Choice((mc, _) => mc <= "CSC3723", "Stage 3"),
        Select.Choice((mc, _) => mc <= "CSC2026", "Stage 2"),
        Select.Choice((mc, _) => mc <= "CSC1026", "Stage 1")
      )
    )

  case class Props(studentL: Option[StudentRecords[SortedMap, ModuleCode, Double]],
                   studentR: Option[StudentRecords[SortedMap, ModuleCode, Double]],
                   selectingR: Boolean,
                   handleClearStudent: Callback,
                   toggleSelecting: (Boolean) => Callback,
                   cohort: CohortAttainmentSummary,
                   clusters: SessionSummary,
                   recaps: SessionSummary,
                   filterChoices: NonEmptyVector[Select.Choice[Filter]] = options)

  case class State(selectedFilters: Set[Select.Choice[Filter]], cohortComparison: Boolean)

  class Backend(bs: BackendScope[Props, State]) {

    def mounted(p: Props) = Callback { println("Bars did mount") }

    def render(p: Props, s: State): VdomElement = {
      <.div(
        ^.id := "detailed",
        p.studentL.fold[TagMod](<.p(^.className := "chart-placedholder", "Click on a student")) { student =>
          List(
            <.div(
              ^.className := "chart-controls",
              <.div(
                ^.className := "row",
                drawFilters(p.filterChoices, s.selectedFilters),
                drawStudComparisonControls(
                  p.studentL,
                  p.studentR,
                  p.toggleSelecting,
                  p.selectingR,
                  p.handleClearStudent
                ),
                drawCheckbox(s.cohortComparison)
              )
            ),
            <.div(
              ^.className := "row border-between",
              <.div(
                ^.className := "col-md-6",
                drawAttainmentBars(filtered(student, s.selectedFilters),
                  p.studentR.map(filtered(_, s.selectedFilters)),
                  filtered(p.cohort.toRecord, s.selectedFilters),
                  s.cohortComparison),
                drawAvgTrend(filtered(student, s.selectedFilters),
                  p.studentR.map(filtered(_, s.selectedFilters)),
                  filtered(p.cohort.toRecord, s.selectedFilters),
                  s.cohortComparison)
              ),
              <.div(
                ^.className := "col-md-6",
                drawIndexedSession(student.number, p.studentR.map(_.number), p.clusters,
                  "Relative cluster usage", "Cluster usage over time",
                  "rgba(111, 203, 118, 0.1)", "#47CB50"),
                drawIndexedSession(student.number, p.studentR.map(_.number), p.recaps,
                  "Relative recap usage", "Cluster usage over time",
                  "rgba(111, 203, 118, 0.1)", "#47CB50")
              )
            )
          ).toTagMod
        }
      )
    }

    /** Filter Student Records */
    private def filtered(records: StudentRecords[SortedMap, ModuleCode, Double],
                         filters: Set[Select.Choice[Filter]]) = {

      //TODO: Composing functions is interesting, but should I apply (lazily) and then reduce rather than applying *as* I
      // reduce?
      val combinedFilter = filters.iterator.map(_.value).reduceOption { (fAcc, f) =>
        (mc: ModuleCode, s: Double) => fAcc(mc, s) && f(mc, s)
      }
      combinedFilter.fold(records.record)(choice => records.record.filter { case (mc, s) => choice(mc, s) })
    }

    /** Construct line chart representation of student average over time, as a proof of concept */
    private def drawAvgTrend(studentScores: SortedMap[ModuleCode, Double],
                          comparedStudentScores: Option[SortedMap[ModuleCode, Double]],
                          cohortSummary: SortedMap[ModuleCode, Double],
                          drawCohortSummary: Boolean) = {
      //Calculate trend line
      def trend(data: Iterable[(ModuleCode, Double)]): List[Double] = {
        //Very mutable, but I'm trying to get back into the habit of method local mutability.
        var total = 0.0
        var counter = 0.0
        val aBldr = ListBuffer.empty[Double]
        for((_, r) <- data) {
          total += r
          counter += 1
          aBldr += (total / counter)
        }
        aBldr.result()
      }

      val averages = trend(studentScores)

      //Create the dataset for the clicked student
      val datasets = ListBuffer(ChartDataset(averages, "Average score", "rgba(111, 203, 118, 0.1)", "#47CB50"))

      //Create the dataset for the compared student if it exists
      comparedStudentScores.foreach { sc =>
        datasets += ChartDataset(trend(sc), "Compared student average score", "rgba(128, 128, 255, 0.1)")
      }

      //Create the dataset for the cohort summary if requested
      if(drawCohortSummary) {
        val cohortAverages = trend(cohortSummary)
        datasets += ChartDataset(cohortAverages, "Cohort average score", "rgba(159, 159, 159, 0.2)", "#606060")
      }

      //List of blank strings required rather than just having no labels as otherwise Chart.js only renders first point
      val labels = averages.map(_ => "")
      val chartData = ChartData(labels, datasets)
      val p = Chart.Props("Average Score Over Time", Chart.LineChart, chartData,
        ChartOptions(displayLegend = true))

      <.div(^.className := "chart-container", Chart.component(p))
    }

    /** Construct detailed representation of student scores, including viz */
    private def drawAttainmentBars(data: SortedMap[ModuleCode, Double],
                         comparedStudentScores: Option[SortedMap[ModuleCode, Double]],
                         cohortSummary: SortedMap[ModuleCode, Double],
                         drawCohortSummary: Boolean)  = {

      //TODO: Make colours package private constants

      //Generate custom legend for student bars
      val generateLegends: JSChart => Seq[ChartLegendItem] = { chart =>

        val legendItems = ArrayBuffer(
          ChartLegendItem("Distinction", "#6FCB76", "#47CB50"),
          ChartLegendItem("Pass", "#CBCB72", "#CBC754"),
          ChartLegendItem("Fail", "#CB4243", "#CB3131")
        )

        // Stringly typed :'( why Javascript?
        if(chart.data.datasets.length >= 2 && chart.data.datasets.exists(ds => ds.label == "compared"))
          legendItems += ChartLegendItem("Compared Student Scores", "#8080FF", "#404080")

        if(chart.data.datasets.length >= 2 && chart.data.datasets.exists(ds => ds.label == "cohort"))
          legendItems += ChartLegendItem("Cohort Module Scores", "#9F9F9F", "#606060")

        legendItems
      }

      //Create a props object for the chart component based on a StudentRecords object
      //Get the module labels and scores
      val mB = ListBuffer.empty[ModuleCode]
      val sB = ListBuffer.empty[Double]
      for ((module, score) <- data) {
        mB += module
        sB += score
      }
      val modules = mB.toList
      val scores = sB.toList

      val (fillColours, borderColours) = colourBars(scores)

      val datasets = ListBuffer.empty[ChartDataset]

      //Create the dataset for the clicked student
      datasets += ChartDataset(scores, "student", fillColours, borderColours)

      //Add the cohort summary if its been selected
      if(drawCohortSummary) datasets += ChartDataset(cohortSummary.values.toSeq, "cohort", "#9F9F9F", "#606060")

      //Draw a comparison student if one exists
      comparedStudentScores.foreach { sc => datasets += ChartDataset(sc.values.toSeq, "compared") }

      val chartData = ChartData(modules, datasets)
      val p = Chart.Props("Student Module Scores", Chart.BarChart, chartData,
        ChartOptions(displayLegend = true, generateLegend = generateLegends.some))

      <.div(^.className := "chart-container", Chart.component(p))
    }

    /** Calculate List of colours for student bars */
    private def colourBars(scores: List[Double]): (List[String], List[String]) = {
      //Constants representing colours (fail, pass, good)
      val borderColours = ("#CB3131", "#CBC754", "#47CB50")
      val fillColours = ("#CB4243", "#CBCB72", "#6FCB76")

      def colourPicker(score: Int, colours: (String, String, String)) = {
        if(score < 40) colours._1
        else if (score < 60) colours._2
        else colours._3
      }

      val fills = scores.iterator.map(s => colourPicker(s.toInt, fillColours))
      val borders = scores.iterator.map(s => colourPicker(s.toInt, borderColours))
      (fills.toList, borders.toList)
    }

    /** Draw a mean indexed line for sessions */
    private def drawIndexedSession(student: StudentNumber,
                                   comparedStudent: Option[StudentNumber],
                                   sessionSummary: SessionSummary,
                                   legendTitle: String,
                                   chartTitle: String,
                                   fillColor: String,
                                   borderColor: String) = {

      def indexDurationsFor(student: StudentNumber) = {
        val studentDurations = sessionSummary.studentDuration.getOrElse(student, sessionSummary.meanDuration)

        studentDurations.map { case (k, v) => k -> (v - sessionSummary.meanDuration.getOrElse(k, 0D)) }
      }

      //Derive student datapoints indexed on the meanDuration
      val sIdxDuration = indexDurationsFor(student).mapValues(_/3600)
      //Do the same for the compared student if it exists
      val cSIdxDuration = comparedStudent.map(s => indexDurationsFor(s).mapValues(_/3600))

      //Create the session dataset for the clicked student
      val datasets = ListBuffer(
        ChartDataset(sIdxDuration.valuesIterator.toList, legendTitle, fillColor, borderColor, fill = false)
      )

      //Create the dataset for the compared student if it existsi
      cSIdxDuration.foreach { sc =>
        datasets += ChartDataset(sc.valuesIterator.toList, "Compared student", fill = false)
      }

      //List of blank strings required rather than just having no labels as otherwise Chart.js only renders first point
      val labels = sIdxDuration.valuesIterator.zipWithIndex.map({ case (_, i) => s"Week $i" }).toSeq
      val chartData = ChartData(labels, datasets)

      //Find min and max values for meanDurations (this is unsound for positive values ... consider)
      val max = sessionSummary.meanDuration.valuesIterator.max
      val scaleMax = ((max/3600) * 2.5).ceil

      val p = Chart.Props(chartTitle, Chart.LineChart, chartData,
        ChartOptions(displayLegend = true, axisStyle = TimeAxis(-1 * scaleMax, scaleMax)))

      <.div(^.className := "chart-container", Chart.component(p))
    }

    /** Draw filter form group */
    //TODO: Abstract Multiselect features into its own component at some point
    private def drawFilters(choices: NonEmptyVector[Select.Choice[Filter]], selected: Set[Select.Choice[Filter]]) = {
      <.div(
        ^.className := "col-md-5",
        <.div(
          ^.className := "input-group",
          <.div(
            ^.className := "input-group-addon",
            Icon.filter(Icon.Small), "Filters:  "),
          <.div(
            ^.className := "bootstrap-tagsinput",
            if(selected.isEmpty) {
              <.span(^.id := "filters-placeholder", "Active Filters ...")
            } else {
              selected.toTagMod { s =>
                <.span(
                  ^.className := "tag label label-info",
                  s.label,
                  <.span(
                    VdomAttr("data-role") := "remove",
                    ^.onClick --> filterRemove(s)
                  )
                )
              }
            }
          ),
          Select.component(Select.Props(selected.headOption.getOrElse(choices.head),
            choices.toVector, filterSelect, "Choose  ".some))
        )
      )
    }

    /** Handle filter select */
    private def filterSelect(selected: Select.Choice[Filter]) =
      bs.modState(s => s.copy(selectedFilters = s.selectedFilters + selected))

    /** Handle filter remove */
    private def filterRemove(removed: Select.Choice[Filter]) =
      bs.modState(s => s.copy(selectedFilters = s.selectedFilters - removed))

    /** Handle cohort comparison toggle */
    private def cohortToggle(e: ReactEventFromInput) = bs.modState(s => s.copy(cohortComparison = !s.cohortComparison))

    /** Draw student to student comparison controls */

    private def drawStudComparisonControls(selected: Option[StudentRecords[SortedMap, ModuleCode, Double]],
                                           compareTo: Option[StudentRecords[SortedMap, ModuleCode, Double]],
                                           toggleSelect: Boolean => Callback,
                                           selectingR: Boolean,
                                           handleClearStudent: Callback) = {
      <.div(
        ^.className := "col-md-5",
        <.div(
          ^.className := "row",
          <.div(
            ^.className := "col-md-5",
            <.label(
              ^.className := "sr-only",
              ^.`for` := "selectedStudentL",
              "Selected student"
            ),
            <.div(
              if(selectingR) {
                ^.className := "input-group"
              } else {
                ^.className := "input-group has-success"
              },
              <.div(^.className := "input-group-addon", "Select Student"),
              <.div(
                ^.className := "form-control",
                ^.id := "selectedStudentL",
                selected.fold[TagMod]("Student Number")(s => s.number),
                ^.onClick --> toggleSelect(false)
              )
            )
          ),
          <.div(
            ^.className := "col-md-7",
            <.label(
              ^.className := "sr-only",
              ^.`for` := "selectedStudentR",
              "Compared to"),
            <.div(
              if(selectingR) {
                ^.className := "input-group has-success"
              } else {
                ^.className := "input-group"
              },
              <.div(^.className := "input-group-addon", "Compared to"),
              <.div(
                ^.className := "form-control",
                ^.id := "selectedStudentR",
                compareTo.fold[TagMod]("Student Number")(s => s.number),
                ^.onClick --> toggleSelect(true)
              ),
              <.div(
                ^.className := "input-group-btn",
                <.button(
                  ^.className := "btn btn-default",
                  ^.`type` := "button",
                  <.strong("X"),
                  ^.onClick --> { toggleSelect(true) >> handleClearStudent >> toggleSelect(false) }
                )
              )
            )
          )
        )
      )
    }

    /** Draw cohort comparison checkbox */
    private def drawCheckbox(cohortSummary: Boolean): VdomElement = {
      <.div(
        ^.className := "col-md-1 col-md-offset-1",
        <.div(
          ^.className := "input-group",
          ^.id := "cohort-summary",
          <.label(
            if(cohortSummary)
              ^.className := "btn btn-primary"
            else
              ^.className := "btn btn-default",
            <.input(
              ^.id := "cohort-summary-toggle",
              ^.`type` := "checkbox",
              ^.selected := cohortSummary,
              ^.onChange ==> cohortToggle
            ),
            "  Cohort Summary"
          )
        )

      )
    }

  }

  val component = ScalaComponent.builder[Props]("StudentBars")
    .initialStateFromProps(p => State(Set.empty[Select.Choice[Filter]], cohortComparison = false))
    .renderBackend[Backend]
    .componentDidMount(scope => scope.backend.mounted(scope.props))
    .build

}
