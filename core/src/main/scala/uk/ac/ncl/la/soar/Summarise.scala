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
package uk.ac.ncl.la.soar

import cats._
import cats.implicits._

/** Simple typeclass to provide basic summary statistics over a record instance
  *
  * TODO: Any good reason this extends Record - seems like it could be foldable?
  *
  * @author hugofirth
  */
trait Summarise[F[_, _]] extends Any with Serializable { self =>

  /** Import companion object */
  import Summarise._

  /** Any Summarise must be a Record */
  def record: Record[F]

  /** Return quartile bounds for a Record */
  def quartiles[A, B: Order](r: F[A, B])(implicit ev: Foldable[F[A, ?]]): Option[(B, B, B)] = {
    //TODO: Investigate if Vector is the right choice here? Or Array? Look at grouped implementation over large indexed seqs
    //Get the entries, then convert then to a list and sort by B's Order
    val es = self.record.toList(r).sortBy(_._2)(Order[B].toOrdering)
    //Group the list of entries into 4, find the heads
    val qb = es.grouped(4).map(_.headOption).toList
    //Sequence List of Option[C] to get an Option[List[(A, B)]] to represent < 4 entries
    qb.sequence.collect { case _ :: q1 :: q2 :: q3 :: Nil => (q1._2, q2._2, q3._2) }
  }

  /** Return the quartile of a given `E`
    *
    * Note that the `E` parameter does not necessarily need to pre-exist in the record.
    */
  def quartile[A, B: Order](r: F[A, B], e: (A, B))(implicit ev: Foldable[F[A, ?]]): Option[Quartile] =
    self.quartiles(r).map {
      case (q1, _, _) if e._2 <= q1 => Q1
      case (_, q2, _) if e._2 <= q2 => Q2
      case (_, _, q3) if e._2 <= q3 => Q3
      case _ => Q4
    }
}

object Summarise {

  /** Summarise instance for any type which has a Record */


  /** Quartile ADT for records - simple enum essentially. */
  sealed trait Quartile { def toInt: Int }
  case object Q1 extends Quartile { val toInt = 1 }
  case object Q2 extends Quartile { val toInt = 2 }
  case object Q3 extends Quartile { val toInt = 3 }
  case object Q4 extends Quartile { val toInt = 4 }
}
