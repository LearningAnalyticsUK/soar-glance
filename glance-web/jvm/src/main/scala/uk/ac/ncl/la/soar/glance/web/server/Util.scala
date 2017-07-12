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
package uk.ac.ncl.la.soar.glance.web.server

import com.twitter.util.{Future, Promise}
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import io.finch.Endpoint

import scala.util.{Failure, Success}

/**
  * Collection of utilities to help with creating REST apis with [[http://finch.io]]
  */
trait Util {

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
  private[web] val corsHeaders = Map(
    "Access-Control-Allow-Origin"  -> "*",
    "Access-Control-Allow-Methods" -> "GET,POST,PUT,DELETE,HEAD,OPTIONS",
    "Access-Control-Max-Age"       -> "300",
    "Access-Control-Allow-Headers" -> "Origin,X-Requested-With,Content-Type,Accept"
  )

}

object Util extends Util
