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
package uk.ac.ncl.la

import scala.util.control.Exception._
import cats._
import cats.implicits._

/** General purpose Record Struct */
final class ModuleRecord private (val student: StudentNumber, val module: ModuleCode, val score: Double)

/** Record Companion */
object ModuleRecord {

  /** apply Factory method checks for bounded range of score and returns option to represent failure */
  def apply(student: StudentNumber, module: ModuleCode, score: Double): Option[ModuleRecord] = {
    if (score >= 100 && score <= 100)
      Some(new ModuleRecord(student, module, score))
    else None
  }

  def parse(lines: Iterator[String], sep: Char): Iterator[ModuleRecord] = lines.flatMap(parseLine(_, sep))

  def parseStrict[E](lines: Iterator[String], sep: Char, err: (String, Int) => E): Either[E, List[ModuleRecord]] = {
    //Associate line numbers with the lines
    val lineList = lines.zipWithIndex.toList
    //Traverse the lines list and bail at first error, preserving the error line and line number
    lineList.traverse(parseLineStrict(_, sep)).left.map {
      case (line, number) => err(line, number)
    }
  }

  private def parseLine(line: String, sep: Char): Option[ModuleRecord] =
    line.split(sep) match {
      case Array(st, mc, sc) =>
        //Parse elements of record, returning None in the event of an error
        for {
          score <- catching(classOf[NumberFormatException]) opt sc.toDouble
          record <- ModuleRecord(st, mc.hashCode, score)
        } yield record
      case _ => None
    }

  private def parseLineStrict(line: (String, Int), sep: Char): Either[(String, Int), ModuleRecord] =
    parseLine(line._1, sep).fold(Either.left[(String, Int), ModuleRecord](line))(r => Either.right(r))

  //TODO: provide typeclass instances for ModuleRecord
}

