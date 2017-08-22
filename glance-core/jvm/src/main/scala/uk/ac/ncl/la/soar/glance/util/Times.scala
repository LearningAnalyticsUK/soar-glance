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
import java.time.Instant

import cats.{Eq, Order, Show}
import io.circe.{Decoder, HCursor}

/**
  * Scala implemenetation of simple wrapper Time type
  */
object Times extends TimeCompanion {
  /** Constructor for the current time, defined agnostically across systems */
  override def now: Time = fromLong(Instant.now.toEpochMilli)

  /** Some constructors, all milliseconds since Epoch */
  override def fromDouble(a: Double): Time = Point(a)

  override def fromLong(a: Long): Time = Point(a.toDouble)

  /** Typeclass instances */
  override implicit def itsShowTime: Show[Time] = Show.show[Time] { t =>
    t.toInstant.toString
  }

  /** Implicit ops classes and utilities */
  final implicit class InstantOps(val a: Instant) extends AnyVal {
    def toTime: Time = Point(a.toEpochMilli.toDouble)
  }

  final implicit class TimeOps(val t: Time) extends AnyVal {
    def toInstant: Instant = Instant.ofEpochMilli(t.millis.toLong)
  }
}
