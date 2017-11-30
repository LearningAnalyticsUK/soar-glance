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
package uk.ac.ncl.la.soar.glance.web.client.component.chart

import cats._
import cats.implicits._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import uk.ac.ncl.la.soar.data.StudentRecords
import uk.ac.ncl.la.soar.glance.eval.SessionSummary
import uk.ac.ncl.la.soar.glance.web.client.component.Select
import uk.ac.ncl.la.soar.glance.web.client.component.chart.StudentChartsContainer.Filter
import uk.ac.ncl.la.soar.{ModuleCode, StudentNumber}

import scala.collection.immutable.SortedMap
import scala.collection.mutable.{ArrayBuffer, ListBuffer}

/**
  * Sealed ADT for student charts
  */
sealed trait StudentChart {

  /** Some aliases */
  type CtnrP = StudentChartsContainer.Props
  type CtnrS = StudentChartsContainer.State

  type Props
  type Back

  /** Each student chart must define a constructor for its Props given the Props and State of the ChartContainer
    *
    * At the moment the signature of this method ensures that its implementations are tightly bound ton concerns of the
    * container, which is not ideal. Really this functionality is to be considered as a halfway house between putting
    * all logic within the Container component and having something truly decoupled.
    */
  protected def propsFactory(p: CtnrP, s: CtnrS): Option[Props]

  protected def _component: ScalaComponent[Props, Unit, Back, CtorType.Props]

  def component(p: CtnrP, s: CtnrS) = propsFactory(p, s).map(p => _component(p))
}

/**
  * Companion object for StudentChart ADT
  */
object StudentChart {

  /** Some type aliases to make things easier */
  type AttainmentProps = (SortedMap[ModuleCode, Double], Option[SortedMap[ModuleCode, Double]], Option[SortedMap[ModuleCode, Double]])
  type SessionProps = (StudentNumber, Option[StudentNumber], SessionSummary)

  /** ADT Members */

  case object HistPerfBars extends StudentChart {

    override type Props = AttainmentProps
    override type Back = Backend

    override protected def propsFactory(p: CtnrP, s: CtnrS): Option[Props] = p.studentL.map { student =>

      val cohort = if(s.cohortComparison) Some(filtered(p.data.cohort.toRecord, s.selectedFilters)) else None

      (filtered(student, s.selectedFilters), p.studentR.map(filtered(_, s.selectedFilters)), cohort)
    }

    class Backend(bs: BackendScope[Props, Unit]) {

      /** Construct detailed representation of student scores, including viz */
      def render(p: Props): VdomElement = {

        val (studentScores, comparedStudentScores, cohortSummary) = p

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
        for ((module, score) <- studentScores) {
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
        cohortSummary.foreach(cS => datasets += ChartDataset(cS.values.toSeq, "cohort", "#9F9F9F", "#606060"))

        //Draw a comparison student if one exists
        comparedStudentScores.foreach { sc => datasets += ChartDataset(sc.values.toSeq, "compared") }

        val chartData = ChartData(modules, datasets)
        val chartProps = Chart.Props("Student Module Scores", Chart.BarChart, chartData,
          ChartOptions(displayLegend = true, generateLegend = generateLegends.some))

        <.div(^.className := "chart-container", Chart.component(chartProps))
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

    }

    override val _component = ScalaComponent.builder[Props]("HistPerfBars")
      .renderBackend[Backend]
      .build
  }

  case object AvgTrendLine extends StudentChart {

    override type Props = AttainmentProps
    override type Back = Backend

    override protected def propsFactory(p: CtnrP, s: CtnrS): Option[Props] = p.studentL.map { student =>

      val cohort = if(s.cohortComparison) Some(filtered(p.data.cohort.toRecord, s.selectedFilters)) else None

      (filtered(student, s.selectedFilters), p.studentR.map(filtered(_, s.selectedFilters)), cohort)
    }

    class Backend(bs: BackendScope[Props, Unit]) {

      def render(p: Props): VdomElement = {

        val (studentScores, comparedStudentScores, cohortSummary) = p

        val averages = trend(studentScores)

        //Create the dataset for the clicked student
        val datasets = ListBuffer(ChartDataset(averages, "Average score", "rgba(111, 203, 118, 0.1)", "#47CB50"))

        //Create the dataset for the compared student if it exists
        comparedStudentScores.foreach { sc =>
          datasets += ChartDataset(trend(sc), "Compared student average score", "rgba(128, 128, 255, 0.1)")
        }

        //Create the dataset for the cohort summary if requested
        cohortSummary.foreach { cS =>
          val cohortAverages = trend(cS)
          datasets += ChartDataset(cohortAverages, "Cohort average score", "rgba(159, 159, 159, 0.2)", "#606060")
        }

        //List of blank strings required rather than just having no labels as otherwise Chart.js only renders first point
        val labels = averages.map(_ => "")
        val chartData = ChartData(labels, datasets)
        val chartProps = Chart.Props("Average Score Over Time", Chart.LineChart, chartData,
          ChartOptions(displayLegend = true))

        <.div(^.className := "chart-container", Chart.component(chartProps))
      }

      /** Calculate trend line */
      private def trend(data: Iterable[(ModuleCode, Double)]): List[Double] = {
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
    }

    override val _component = ScalaComponent.builder[Props]("AvgTrendLine")
      .renderBackend[Backend]
      .build
  }

  case object ClusterUsageLine extends StudentChart {

    override type Props = SessionProps
    override type Back = Backend

    override protected def propsFactory(p: CtnrP, s: CtnrS): Option[Props] = p.studentL.map { student =>
      (student.number, p.studentR.map(_.number), p.data.cluster)
    }

    class Backend(bs: BackendScope[Props, Unit]) {

      def render(p: Props): VdomElement =
        drawIndexedSession(p, "Relative cluster usage", "Cluster usage over time",
          "rgba(111, 203, 118, 0.1)", "#47CB50")
    }

    override val _component = ScalaComponent.builder[Props]("ClusterUsage")
      .renderBackend[Backend]
      .build
  }

  case object RecapUsageLine extends StudentChart {

    override type Props = SessionProps
    override type Back = Backend

    override protected def propsFactory(p: CtnrP, s: CtnrS): Option[Props] = p.studentL.map { student =>
      (student.number, p.studentR.map(_.number), p.data.recap)
    }

    class Backend(bs: BackendScope[Props, Unit]) {

      def render(p: Props): VdomElement =
        drawIndexedSession(p, "Relative recap usage",
          "Recap usage over time", "rgba(111, 203, 118, 0.1)", "#47CB50")
    }

    override val _component = ScalaComponent.builder[Props]("RecapUsage")
      .renderBackend[Backend]
      .build
  }

  /** Utility methods */


  /** Filter a students record based on a set of filter choices */
  private def filtered(records: StudentRecords[SortedMap, ModuleCode, Double],
                       filters: Set[Select.Choice[Filter]]) = {

    //TODO: Composing functions is interesting, but should I apply (lazily) and then reduce rather than applying *as* I
    // reduce?
    val combinedFilter = filters.iterator.map(_.value).reduceOption { (fAcc, f) =>
      (mc: ModuleCode, s: Double) => fAcc(mc, s) && f(mc, s)
    }
    combinedFilter.fold(records.record)(choice => records.record.filter { case (mc, s) => choice(mc, s) })
  }

  /** Draw a mean indexed line for sessions */
  private def drawIndexedSession(data: SessionProps,
                                 legendTitle: String,
                                 chartTitle: String,
                                 fillColor: String,
                                 borderColor: String) = {

    val (student, comparedStudent, sessionSummary) = data

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
    //TODO: Magic numbers, put in a config file somewhere or at least make then named constants with comments
    // explaining values
    val max = sessionSummary.meanDuration.valuesIterator.max
    val scaleMax = ((max/3600) * 2.5).ceil

    val p = Chart.Props(chartTitle, Chart.LineChart, chartData,
      ChartOptions(displayLegend = true, axisStyle = TimeAxis(-1 * scaleMax, scaleMax)))

    <.div(^.className := "chart-container", Chart.component(p))
  }
}

