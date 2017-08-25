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
package uk.ac.ncl.la.soar.glance.eval.server

import cats._
import cats.implicits._
import io.circe._
import io.circe.generic.auto._
import io.finch._
import io.finch.circe._
import monix.cats._
import monix.eval.Task
import uk.ac.ncl.la.soar.db.ModuleDb
import uk.ac.ncl.la.soar.server.Implicits._

/**
  * Class defines the REST api for Modules
  */
class ModulesApi(moduleRepository: ModuleDb) {

  /** Endpoint returns all modules in response to `GET /surveys` */
  val list = get("modules") { moduleRepository.list.toFuture.map(Ok) }

  /** Pre-flight endpoint for CORS headers */
  val preflight = options(*) { Ok(()) }

  /** All endpoints in this API, enriched with CORS headers */
  val endpoints = (list :+: preflight).withCorsHeaders

  /** APU endpoints exposed as a service */
  val service = endpoints.toService
}
