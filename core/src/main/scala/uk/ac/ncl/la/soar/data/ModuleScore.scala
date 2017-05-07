/** student-attainment-predictor
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
package uk.ac.ncl.la.soar.data

import cats.implicits._
import uk.ac.ncl.la.soar.{ModuleCode, StudentNumber}

import scala.util.control.Exception._

/** General purpose Record Struct */
final class ModuleScore private(val student: StudentNumber, val module: ModuleCode, val score: Double)

/** Record Companion */
object ModuleScore {

  /** apply Factory method checks for bounded range of score and returns option to represent failure */
  def apply(student: StudentNumber, module: ModuleCode, score: Double): Option[ModuleScore] = {
    if (score >= 0 && score <= 100)
      Some(new ModuleScore(student, module, score))
    else None
  }

  def unapply(arg: ModuleScore): Option[(StudentNumber, ModuleCode, Double)] =
    Some((arg.student, arg.module, arg.score))

  def parse(lines: Iterator[String], sep: Char): Iterator[ModuleScore] = lines.flatMap(parseLine(_, sep))

  def parseStrict[E](lines: Iterator[String], sep: Char, err: (String, Int) => E): Either[E, List[ModuleScore]] = {
    //Associate line numbers with the lines
    val lineList = lines.zipWithIndex.toList
    //Traverse the lines list and bail at first error, preserving the error line and line number
    lineList.traverse(parseLineStrict(_, sep)).left.map {
      case (line, number) => err(line, number)
    }
  }

  private[soar] def parseLine(line: String, sep: Char): Option[ModuleScore] =
    line.split(sep) match {
      case Array(st, mc, sc) =>
        //Parse elements of record, returning None in the event of an error
        for {
          score <- catching(classOf[NumberFormatException]) opt sc.toDouble
          record <- ModuleScore(st, mc, score)
        } yield record
      case _ => None
    }

  private[soar] def parseLineStrict(line: (String, Int), sep: Char): Either[(String, Int), ModuleScore] =
    parseLine(line._1, sep).fold(Either.left[(String, Int), ModuleScore](line))(r => Either.right(r))

  //TODO: provide typeclass instances for ModuleScore
}

