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
import io.circe.{Decoder, Encoder, HCursor, Json}

/**
  * Wrapper to support basic time operations in a cross platform manner because ... javascript. The api surface will
  * grow organically as I discover requirements. Not to be treated as complete or stable!
  */
trait Time { self =>

  import Period._

  /** Milli precision getter */
  def millis: Double

  /** Arbitrary precision getter */
  def sinceEpoch(p: Period): Double = p match {
    case Millisecond => self.millis
    case o => scala.math.floor(self.millis / o.millis)
  }

  /** Checks if this time is within an interval defined by a start time (inclusive) and end time (exclusive) */
  def isBetween(start: Time, end: Time) = self.millis >= start.millis && self.millis < end.millis

}

/**
  * Common implementation shared between platform specific implementations of our basic Time type
  */
abstract class TimeCompanion extends TimeInstances {

  /** Private internal representation */
  protected case class Point(millis: Double) extends Time

  /** Constructor for the current time, defined agnostically across systems */
  def now: Time

  /** Some constructors, all milliseconds since Epoch */
  def fromDouble(a: Double): Time
  def fromLong(a: Long): Time

  override implicit def decodeTime: Decoder[Time] = new Decoder[Time] {
    override def apply(c: HCursor): Decoder.Result[Time] = Decoder.decodeDouble.map(d => Point(d))(c)
  }
}

/** Require certain typeclasses of both Time implementations */
trait TimeInstances {

  implicit def itsShowTime: Show[Time]
  implicit val timeOrder: Order[Time] = Order.by(_.millis)
  implicit val eqTime: Eq[Time] = Eq.fromUniversalEquals

  implicit val encodeTime: Encoder[Time] = new Encoder[Time] {
    override def apply(a: Time): Json = Encoder.encodeDouble(a.millis)
  }
  implicit def decodeTime: Decoder[Time]
}

/** Enum of periods. Ignore Month, Year etc... as we don't need them and they have special cases */
sealed trait Period { def millis: Double }

object Period {
  case object Millisecond extends Period { val millis = 1D}
  case object Second extends Period { val millis = 1000D }
  case object Minute extends Period { val millis = Second.millis * 60D }
  case object Hour extends Period { val millis = Minute.millis * 60D }
  case object Day extends Period { val millis = Hour.millis * 24D }
  case object Week extends Period { val millis = Day.millis * 7D }
}

