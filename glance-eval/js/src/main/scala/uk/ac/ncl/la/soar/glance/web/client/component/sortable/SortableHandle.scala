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
package uk.ac.ncl.la.soar.glance.web.client.component.sortable


import japgolly.scalajs.react._
import japgolly.scalajs.react.raw.ReactElement

import scala.scalajs.js

object SortableHandle {
  /**
    * Wrap another component
    *
    * @param wrappedComponent The wrapped component itself
    * @tparam P The type of Props of the wrapped component
    * @return A component wrapping the wrapped component
    */

  def wrap[P, CT[_, _]](wrappedComponent: GenericComponent[P, CT, _]): P => JsComponent.Unmounted[js.Object, Null] = {
    (wrappedProps) => {
      val reactElement = js.Dynamic.global.SortableHOC.SortableHandle(wrappedComponent.raw).asInstanceOf[ReactElement]
      val component = JsComponent[js.Object, Children.None, Null](reactElement)
      val mergedProps = js.Dynamic.literal()
      mergedProps.updateDynamic("a")(wrappedProps.asInstanceOf[js.Any])
      component(mergedProps.asInstanceOf[js.Object])
    }
  }
}

