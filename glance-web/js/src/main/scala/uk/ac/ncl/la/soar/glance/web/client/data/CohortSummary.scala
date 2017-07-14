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
package uk.ac.ncl.la.soar.glance.web.client.data

import cats._
import cats.implicits._
import uk.ac.ncl.la.soar.{ModuleCode, StudentNumber}
import uk.ac.ncl.la.soar.data.{ModuleRecords, StudentRecords}

import scala.collection.immutable.SortedMap
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import Numeric._

/**
  * Object for constructing summaries of a cohorts attainment efficiently given a list of student records
  *
  * There is significant duplication of work between this and the `soar.core` package. Also, it is unclear whether this
  * should be computed on the backend and requested by the front, visa versa, or a mixture of both.
  *
  * TODO: Build consistent and generic summarisation strategy for collections of records
  */
sealed abstract class CohortSummary[A: Order] {

  def cohortRecords: List[StudentRecords[SortedMap, A, Double]]

  def avgByOrder: List[Double] = avgByKey.valuesIterator.toList

  def avgByKey: SortedMap[A, Double] = {

    //Nasty hack - why am I having to do this? Implicit resolution is just giving up
    val keys = mutable.HashMap.empty[A, (Int, Double)]

    for {
      r <- cohortRecords
      (k, v) <- r.record
    } {
      val totK = keys.get(k)
      val deltaTot = totK.fold((1, v)) { case (num, tot) => (num + 1, tot + v) }
      keys.update(k, deltaTot)
    }

    val ev = implicitly[Order[A]]
    SortedMap.empty[A, Double](ev.toOrdering) ++ keys.mapValues { case (num, tot) => tot / num }
  }

  def toRecord: StudentRecords[SortedMap, A, Double] = StudentRecords("N/A",avgByKey)
}

case class CohortAttainmentSummary(cohortRecords: List[StudentRecords[SortedMap, ModuleCode, Double]]) extends CohortSummary[ModuleCode]

