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
package uk.ac.ncl.la.soar.server

import com.twitter.util.{Future, Promise}
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import io.finch.Endpoint

import scala.util.{Failure, Success}
import fs2.util.{Attempt, Catchable, Suspendable}

/**
  * Collection of utilities and typeclass instances to help with creating REST services with [[http://finch.io]], doobie
  * and Monix.
  */
trait Implicits {

  /**
    * Class defines simple operation to convert from [[fs2.Task]] to [[com.twitter.util.Future]]
    *
    * Full on cribbed from dave gurnell's typelevel-todomvc: [[https://github.com/davegurnell/typelevel-todomvc]]
    */
  implicit class TaskOps[A](task: Task[A]) {
    def toFuture: Future[A] = {
      val promise = Promise[A]
      task.runOnComplete {
        case Failure(err) => promise.setException(err)
        case Success(ans) => promise.setValue(ans)
      }
      promise
    }
  }

  /**
    * Class defines operation to add headers to a Finch Endpoint's response.
    */
  implicit class EndpointOps[A](endpoint: Endpoint[A]) {
    def withHeaders(headers: Map[String, String]): Endpoint[A] =
      headers.foldLeft(endpoint)((e, h) => e.withHeader(h._1 ->  h._2))

    def withCorsHeaders = withHeaders(corsHeaders)
  }

  /** The headers needed to make our apis CORS compliant */
  private[server] val corsHeaders = Map(
    "Access-Control-Allow-Origin"  -> "*",
    "Access-Control-Allow-Methods" -> "GET,POST,PUT,DELETE,HEAD,OPTIONS",
    "Access-Control-Max-Age"       -> "300",
    "Access-Control-Allow-Headers" -> "Origin,X-Requested-With,Content-Type,Accept"
  )


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

object Implicits extends Implicits

