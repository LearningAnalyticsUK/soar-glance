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

import com.twitter.finagle.Http
import com.twitter.server.TwitterServer
import com.twitter.util.Await
import uk.ac.ncl.la.soar.glance.Repository
import Util._
import monix.eval.Task
import org.flywaydb.core.Flyway

/**
  * Main class for the glance server
  */
object Main extends TwitterServer {

  def main(): Unit = {

    //TODO: Neaten up - perhaps introduce limited point free style to clarify
    val api = Await.result(Repository.Survey.map(new SurveysApi(_)).toFuture)

    val server = Http.server.serve(":8080", api.service)

    //Explicitly return Unit to supress discarded non unit value error
    onExit { server.close(); () }

    Await.ready(server)
    ()
  }

}
