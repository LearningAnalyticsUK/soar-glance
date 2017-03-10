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
import simulacrum.typeclass
import uk.ac.ncl.la.soar.data.{ModuleRecords, StudentRecords}

import scala.collection.immutable.SortedMap

/** Record typeclass. Types which form a Record include [[StudentRecords]] (which is a record of a student's performance
  * across a collection of modules) and [[ModuleRecords]] (which is a record of the performance of a collection of
  * students on a single module).
  *
  * Records require one primary feature: a collection of entries (Which are Tuple2s / Map Entries).
  *
  * As such the typeclass has a ternary type constructor: the types of an entry and the type of a collection
  *
  * TODO: Look into Fix and CoFree datatypes
  * TODO: Look into which typeclasses this TC should extend, if any?
  * TODO: Look into an ADT rather than a TC - though I think this is still unsatisfying.
  *
  * @author hugofirth
  */
trait Record[F[_, _]] extends Any with Serializable { self =>
  /** Import companion object */
  import Record._
  import Filter._

  /** Get a specific element of a record. Return none if that element does not exist
    *
    * Decided against an `iterator(r).find(_ == k)` based implementation as complexity is still O(N) and most records will be
    * map types, in which case `toMap(r).get` is amortized O(1)
    */
  def get[A, B](r: F[A, B], k: A): Option[B] = self.toMap(r).get(k)

  /** Return iterator of tuples for record entries. Cannot define in terms of Traverse instance as that fixes on `A` */
  def iterator[A, B](r: F[A, B]): Iterator[(A, B)]

  /** Produce a List of tuples from a record instance */
  def toList[A, B](r: F[A, B]): List[(A, B)] = self.iterator(r).toList

  /** Produce a Map from a record instance - should be overriden by types whose internal datastructure *is* a Map */
  def toMap[A, B](r: F[A, B]): Map[A, B] = self.iterator(r).toMap

  /** Truncate records by key upto given key inclusive. Note that the key need not explicitly exist in the record */
  def toKey[A: Order, B](r: F[A, B], lim: A)(implicit ev: Filter[F[?, B]]): F[A, B] = ev.filter(r)({_ <= lim})

  /** Truncate records by key from given key inclusive. Note that the key need not explicitly exist in the record */
  def fromKey[A: Order, B](r: F[A, B], lim: A)(implicit ev: Filter[F[?, B]]): F[A, B] = ev.filter(r)({_ >= lim})

  /** Truncate records by value upt given value inclusive. */
  def to[A, B: Order](r: F[A, B], lim: B)(implicit ev: Filter[F[A, ?]]): F[A, B] = r.filter(_ <= lim)

  /** Truncate records by value from given value inclusive. */
  def from[A, B: Order](r: F[A, B], lim: B)(implicit ev: Filter[F[A, ?]]): F[A, B] = r.filter(_ >= lim)


}

/** Record */
object Record {

  /** Access an implicit `Record`. */
  @inline final def apply[F[_, _]](implicit ev: Record[F]): Record[F] = ev

//  /** Any Record type should implement [[Traverse]] with the key type fixed - does this work? */
//  implicit def recordTraverse[F[_, _], A](r: Record[F])
//                                         (implicit ev: TraverseFilter[F[A,?]]): TraverseFilter[F[A, ?]] = ev

  /** Implicit syntax enrichment */
  final implicit class RecordOps[F[_,_], A, B](val r: F[A, B]) extends AnyVal {

    def get(k: A)(implicit ev: Record[F]) = ev.get(r, k)
    def iterator(implicit ev: Record[F]) = ev.iterator(r)
    def toList(implicit ev: Record[F]) = ev.toList(r)
    def toMap(implicit ev: Record[F]) = ev.toMap(r)
    def toKey(lim: A)(implicit ev: Record[F], ev2: Filter[F[?, B]], ev3: Order[A]) = ev.toKey(r, lim)

  }

  /** Default Record Instances */
  implicit val sortedMapRecord: Record[SortedMap] = new Record[SortedMap] {

    override def iterator[A, B](r: SortedMap[A, B]): Iterator[(A, B)] = r.iterator

    override def toMap[A, B](r: SortedMap[A, B]): Map[A, B] = r

    /** Truncate records by key upto given key inclusive. Note that the key need not explicitly exist in the record */
    override def toKey[A: Order, B](r: SortedMap[A, B], lim: A)
                                   (implicit ev: Filter[SortedMap[?, B]]): SortedMap[A, B] = r.to(lim)

    /** Truncate records by key from given key inclusive. Note that the key need not explicitly exist in the record */
    override def fromKey[A: Order, B](r: SortedMap[A, B], lim: A)
                                     (implicit ev: Filter[SortedMap[?, B]]): SortedMap[A, B] = r.from(lim)


  }

  //Define Filter instance for SortedMap
  implicit def sortedMapKeyFilter[V]: Filter[SortedMap[?, V]] = new Filter[SortedMap[?, V]] {
    /**
      * Apply a filter to a structure such that the output structure contains all
      * `A` elements in the input structure that satisfy the predicate `f` but none
      * that don't.
      */
    override def filter[A](fa: SortedMap[A, V])(f: (A) => Boolean): SortedMap[A, V] = fa.filterKeys(f)
  }

}
