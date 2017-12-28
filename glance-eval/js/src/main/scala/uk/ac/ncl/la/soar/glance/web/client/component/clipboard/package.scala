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
package uk.ac.ncl.la.soar.glance.web.client.component

import scala.scalajs.js
import japgolly.scalajs.react.Callback

/**
  * Lifted straight from https://github.com/cquiroz/scalajs-react-clipboard while
  * I work on a pull request to cross publish that lib for 2.11
  */
package clipboard {

  import scala.scalajs.js.annotation.ScalaJSDefined

  // Options
  @ScalaJSDefined
  trait ClipboardOptions extends js.Object {
    def debug: Boolean
    def message: String
  }

  object ClipboardOptions {
    def apply(debug: Boolean, message: String): ClipboardOptions = {

      js.Dynamic
        .literal(
          debug = debug,
          message = message
        )
        .asInstanceOf[ClipboardOptions]
    }
  }

}

/**
  * External facade
  */
package object clipboard {
  // OnCopy callback
  type OnCopy = (String, Boolean) => Callback

  private[clipboard] object raw {
    type RawOnCopy = js.Function2[String, Boolean, Unit]
  }
}
