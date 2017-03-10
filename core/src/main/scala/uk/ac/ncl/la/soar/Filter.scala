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

/** Plain filter marker typeclass without any default implementations.
  *
  * Included because there are types which may be reasonably filtered which do not form proper `Functor`s
  * (e.g. [[scala.collection.immutable.SortedMap]]) but `Filter` functionality is implemented in terms of
  * `mapFilter` in Cats and therefore is found only the [[cats.FunctorFilter]] typeclass or sub-typeclasses.
  *
  * @author hugofirth
  */
trait Filter[F[_]] extends Any with Serializable {

  /**
    * Apply a filter to a structure such that the output structure contains all
    * `A` elements in the input structure that satisfy the predicate `f` but none
    * that don't.
    */
  def filter[A](fa: F[A])(f: A => Boolean): F[A]

}

object Filter {

  /** Summon Filter instances through apply */
  @inline final def apply[F[_]](implicit ev: Filter[F]): Filter[F] = ev

  /** Implicit syntax enrichment */
  final implicit class FilterOps[F[_], A](val fa: F[A]) extends AnyVal {
    def filter(f: A => Boolean)(implicit ev: Filter[F]) = ev.filter(fa)(f)
  }
}