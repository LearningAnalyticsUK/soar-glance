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
package uk.ac.ncl.la.soar.glance.web.client.style

import scalacss.DevDefaults._

/**
  * Singleton object containing typed definitions of applications css
  */
object GlobalStyle extends StyleSheet.Inline {

  import dsl._

  /** Define the global application colour palette */

  val primaryChrome = c"#003965"
  val secondaryChrome = c"#3B8ECB"
  val highlightChrome = c"#fff"
  val chromeText = c"#f5f5f5"
  val chromeTextInverse = primaryChrome

  /** Declare bootstrap wrapper style */
  val bootstrapStyle = new BootStrapStyle

  /** Declare main style */
  style(unsafeRoot("body")(
    paddingTop(50.px)
  ))

}
