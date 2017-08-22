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
package uk.ac.ncl.la.soar.glance.util

import cats._
import cats.implicits._
import scala.scalajs.js



/** Javascript implementation of simple Time type */
object Times extends TimeCompanion {

  /** Get the current time as a Double. This is the *whole* number of milliseconds since Epoch */
  def now = Point(js.Date.now())

  /** Some constructors, all milliseconds since Epoch */
  override def fromDouble(a: Double) = Point(a)

  override def fromLong(a: Long) = Point(a.toDouble)

  /** Convert to local js Date */
  final implicit class JsTimeOps(val t: Time) extends AnyVal {
    def toDate: js.Date = new js.Date(t.millis)
  }

  /** Typeclass instances */
  override implicit val itsShowTime: Show[Time] = Show.show[Time] { t =>
    new js.Date(t.millis).toISOString()
  }

}


