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

import com.thoughtworks.binding.Binding.BindingSeq
import com.thoughtworks.binding.{Binding, dom}
import org.scalajs.dom.html.TableRow
import org.scalajs.dom.raw.Node
import uk.ac.ncl.la.soar.ModuleCode
//TODO: Build soar.implicits module or use export hook for easy to remember imports
import uk.ac.ncl.la.soar._
import uk.ac.ncl.la.soar.data._
import uk.ac.ncl.la.soar.Record._
import uk.ac.ncl.la.soar.glance.Survey

import scala.collection.immutable.SortedMap


/**
  * Survey view ADT
  */
sealed trait SurveyView {

  @dom
  def main(survey: Binding[Option[Survey]]): Binding[Node]
}

object BaseSurvey extends SurveyView {

  @dom
  private def tableHeader(survey: Survey) = {
    val columns = "Student Number" :: survey.modules.toList.sorted
    tr(columns).bind
  }

  @dom
  private def tableRow(modules: Set[String], studentRecords: StudentRecords[SortedMap, ModuleCode, Double]) = {
    //Get the table columns (minuse student number)
    val moduleCols = modules.toList.sorted
    //Fill in blanks where student has no score for module
    val recordEntries = moduleCols.map(c => studentRecords.record.getOrElse(c, " "))
    //Add student number
    val columns = studentRecords.number :: recordEntries
    //Bind the row
    tr(columns).bind
  }

  @dom
  private def tr(columns: List[String]): Binding[TableRow] = {
    <tr>
      {
        for(col <- columns) yield (<td> { col } </td>).bind
      }
    </tr>
  }

  @dom
  override def main(survey: Binding[Option[Survey]]): Binding[Node] = {
    //TODO Fix this
    survey.bind match {
      case Some(s) =>

        <div class="table-responsive">
          <table class="table table-striped">
            <thead>{ tableHeader(s).bind }</thead>
            <tbody>
              {
              for (entry <- s.entries) yield tableRow(s.modules, entry).bind
              }
            </tbody>
          </table>
        </div>

      case None => <p>ERROR!</p>
    }

  }
}


