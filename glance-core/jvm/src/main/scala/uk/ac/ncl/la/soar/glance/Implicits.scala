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
package uk.ac.ncl.la.soar.glance

import fs2.util.{Attempt, Catchable, Suspendable}
import monix.eval.Task

import scala.util.{Failure, Success}

/**
  * Implicits needed throughout the glance project, mostly typeclass instances
  */
object Implicits {
  
  implicit object TaskCatchable extends Catchable[Task] with Suspendable[Task] {
    def pure[A](a: A): Task[A] = Task.pure(a)

    def flatMap[A, B](a: Task[A])(f: A => Task[B]): Task[B] =
      a.flatMap(f)

    def fail[A](err: Throwable): Task[A] =
      Task.raiseError(err)

    def attempt[A](fa: Task[A]): Task[Attempt[A]] =
      fa.materialize.map {
        case Success(value) => Right(value)
        case Failure(e) => Left(e)
      }

    def suspend[A](fa: => Task[A]): Task[A] =
      Task.suspend(fa)
  }
}
