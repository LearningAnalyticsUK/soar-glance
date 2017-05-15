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
package uk.ac.ncl.la.soar.glance.cli

import cats.MonadError
import fs2.Task

/**
  * Non-sealed trait which defines a structure for creating cli commands.
  *
  * TODO: Work out whether I should seal this trait. The downside is that both assessor and generator whould have to be
  *   placed in this file, which would hurt readability. Upside is pattern matching and neatness. Alternatives are to
  *   seal this heirarchy and create simple case objects which mix in Assessor and Generator traits defined elsewhere or
  *   to make this a typeclass, which has the benefit of consistency with what I do in other places, but feels overweight.
  */
trait Command[A <: CommandConfig, B] {

  //TODO: abstract over some effect type F. I thought that MonadError was the appropriate typeclass, but Doobie requires
  //  Catchable and Suspendable. Could attempt and rewrap using fromEither, but that seems crazy
  def run(conf: A): Task[B]
}
