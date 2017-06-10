///** soar
//  *
//  * Copyright (c) 2017 Hugo Firth
//  * Email: <me@hugofirth.com/>
//  *
//  * Licensed under the Apache License, Version 2.0 (the "License");
//  * you may not use this file except in compliance with the License.
//  * You may obtain a copy of the License at:
//  *
//  * http://www.apache.org/licenses/LICENSE-2.0
//  *
//  * Unless required by applicable law or agreed to in writing, software
//  * distributed under the License is distributed on an "AS IS" BASIS,
//  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  * See the License for the specific language governing permissions and
//  * limitations under the License.
//  */
//package uk.ac.ncl.la.soar.glance.web.client
//
//import com.thoughtworks.binding.Binding.{BindingSeq, Constants, Var}
//import com.thoughtworks.binding.{Binding, dom}
//import org.scalajs.dom.html.TableRow
//import org.scalajs.dom.raw.{Event, Node}
//import org.singlespaced.d3js.Ops._
//import org.singlespaced.d3js.d3
//import uk.ac.ncl.la.soar.ModuleCode
//
//import scala.scalajs.js
////TODO: Build soar.implicits module or use export hook for easy to remember imports
//import uk.ac.ncl.la.soar._
//import uk.ac.ncl.la.soar.data._
//import uk.ac.ncl.la.soar.Record._
//import uk.ac.ncl.la.soar.glance.Survey
//
//import scala.collection.immutable.SortedMap
//
//
///**
//  * Survey view ADT
//  */
//trait View[A] {
//
//  def model: Binding[A]
//  def view: Binding[Node]
//
//}
//
//case class BaseSurvey(model: Binding[Option[Survey]]) extends View[Option[Survey]] {
//
//  //Additional component of the ViewModel, along with model field
//  private val selected = Var(None: Option[StudentRecords[SortedMap, ModuleCode, Double]])
//
//  @dom
//  private def tableHeader(survey: Survey) = {
//    val columns = "Student Number" :: survey.modules.toList.sorted
//    th(columns).bind
//  }
//
//  @dom
//  private def tableRow(modules: Set[String], studentRecords: StudentRecords[SortedMap, ModuleCode, Double]) = {
//    //Get the table columns (minuse student number)
//    val moduleCols = modules.toList.sorted
//    //Fill in blanks where student has no score for module
//    val recordEntries = moduleCols.map(c => studentRecords.record.get(c).fold(" ")(_.toString))
//    //Add student number
//    val columns = studentRecords.number :: recordEntries
//    //Bind the row
//    tr(columns, studentRecords).bind
//  }
//
//  @dom
//  private def th(columns: List[String]): Binding[TableRow] = {
//    <tr>
//        {
//          for(col <- Constants(columns:_*)) yield { <td> { col } </td> }
//        }
//    </tr>
//  }
//
//  @dom
//  private def tr(columns: List[String], record: StudentRecords[SortedMap, ModuleCode, Double]): Binding[TableRow] = {
//    <tr onclick= { e: Event => selected.value = Some(record) }>
//      {
//        for(col <- Constants(columns:_*)) yield { <td> { col } </td> }
//      }
//    </tr>
//  }
//
//  //TODO: On click even to create bar graph for student
//
//  @dom private val table: Binding[Node] = {
//    model.bind match {
//      case Some(s) =>
//        <div class="table-responsive">
//          <table class="table table-striped table-bordered table-hover" id="training-table">
//            <thead>{ tableHeader(s).bind }</thead>
//            <tbody>
//              {
//              for (entry <- Constants(s.entries:_*)) yield { tableRow(s.modules, entry).bind }
//              }
//            </tbody>
//          </table>
//        </div>
//
//      case None => <p>Loading...</p>
//    }
//  }
//
//  private def drawBars(records: StudentRecords[SortedMap, ModuleCode, Double], selector: String): Unit = {
//
//    println("Redrawing bars")
//    //Round scores
//    val scores = records.record.iterator.map(_._2.toInt).toList
//
//    val graphHeight = 250
//    //The width of each bar.
//    val barWidth = 40
//    //The distance between each bar.
//    val barSeparation = 5
//    //The maximum value of the data.
//    val maxData = 100
//    //The actual horizontal distance from drawing one bar rectangle to drawing the next.
//    val horizontalBarDistance = barWidth + barSeparation
//    //The value to multiply each bar's value by to get its height.
//    val barHeightMultiplier = graphHeight / maxData
//    //Color for start
//    val fail = d3.rgb(203, 49, 49)
//    val pass = d3.rgb(203, 199, 84)
//    val good = d3.rgb(71, 203, 80)
//
//    def colorPicker(score: Int) = {
//      if (score < 40) fail
//      else if (score < 60) pass
//      else good
//    }
//
//    val rectXFun = (d: Int, i: Int) => i * horizontalBarDistance
//    val rectYFun = (d: Int) => graphHeight - d * barHeightMultiplier
//    val rectHeightFun = (d: Int) => d * barHeightMultiplier
//    val rectColorFun = (d: Int, i: Int) => colorPicker(d).toString
//
//    //Clear existing
//    d3.select(".student-bars").remove()
//    val svg = d3.select(selector).append("svg")
//      .attr("width", "100%")
//      .attr("height", "250px")
//      .attr("class", "student-bars")
//    import js.JSConverters._
//    val sel = svg.selectAll("rect").data(scores.toJSArray)
//    sel.enter()
//      .append("rect")
//      .attr("x", rectXFun)
//      .attr("y", rectYFun)
//      .attr("width", barWidth)
//      .attr("height", rectHeightFun)
//      .style("fill", rectColorFun)
//    ()
//  }
//
//
//  @dom
//  private def graph(entry: Binding[Option[StudentRecords[SortedMap, ModuleCode, Double]]]) = {
//    entry.bind match {
//      case Some(e) =>
//        drawBars(e, "#cells")
//        <p></p> // This is horrid - find a framework which works with Cats or port Binding.scala so we can use Monadic sequencing
//      case None =>
//        <p>Select a student</p>
//    }
//  }
//
//  @dom
//  def view: Binding[Node] = {
//    <section id="training-section">
//      <h2>Training Data</h2>
//        { table.bind }
//      <h3>Detailed View</h3>
//      <div id="cells">
//        { graph(selected).bind }
//      </div>
//    </section>
//  }
//}
//
//
