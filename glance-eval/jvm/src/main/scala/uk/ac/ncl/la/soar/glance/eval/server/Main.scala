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

import com.twitter.finagle.Http
import com.twitter.server.TwitterServer
import com.twitter.util.Await
import uk.ac.ncl.la.soar.server.Implicits._
import monix.eval.Task
import monix.cats._
import cats._
import cats.implicits._
import cats.data._
import io.finch._
import io.finch.circe._
import io.circe.generic.auto._
import uk.ac.ncl.la.soar.data.Module

/**
  * Main class for the glance server
  */
object Main extends TwitterServer {

  def main(): Unit = {

    val surveysTask =
      (Repositories.Survey,
        Repositories.ClusterSession,
        Repositories.RecapSession).map3(new SurveysApi(_, _, _))

    val responsesTask = Repositories.SurveyResponse.map(new SurveyResponsesApi(_))

    val modulesTask = Repositories.Module.map(new ModulesApi(_))

    val (surveysApi, responsesApi, modulesApi) = Await.result(Task.zip3(surveysTask, responsesTask, modulesTask).toFuture)

    val service = (surveysApi.endpoints :+: responsesApi.endpoints :+: modulesApi.endpoints).toService

    val server = Http.server.serve(":8080", service)

    //Explicitly return Unit to supress discarded non unit value error
    onExit { server.close(); () }

    Await.ready(server)
    ()
  }

}
