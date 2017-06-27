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

import scalajs.js
import japgolly.scalajs.react._

import scala.scalajs.js.annotation.JSName

/**
  * Wrapping React FixedDataTable component to avoid hacking in the Jquery datatable plugin
  * @author hugofirth
  */
object FixedDataTable {

  @JSName("FixedDataTable")
  @js.native
  object RawComponent extends js.Object

  @js.native
  trait Props extends js.Object {
    var width: Int = js.native
    var height: Int = js.native
    var rowHeight: Int = js.native
    var rowsCount: Int = js.native
    var headerHeight: Int = js.native
  }

  def props(width: Int, height: Int, rowHeight: Int, rowsCount: Int, headerHeight: Int): Props = {
    //Construct Props object
    val p = (new js.Object).asInstanceOf[Props]
    p.width = width
    p.height = height
    p.rowHeight = rowHeight
    p.rowsCount = rowsCount

    p
  }

  val component = JsComponent[Props, Children.Varargs, Null](RawComponent)
}



