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
package uk.ac.ncl.la.soar.glance.eval.server

import java.time.Instant
import java.util.UUID

import doobie.imports._
import monix.eval.Task
import cats._
import cats.implicits._
import uk.ac.ncl.la.soar.StudentNumber
import uk.ac.ncl.la.soar.db.{RepositoryCompanion, Repository => DbRepository}


sealed trait SessionTable {
  type Row
  def name: String
}
case object ClusterSessionTable extends SessionTable {
  type Row = (UUID, Instant, Instant, String, StudentNumber, UUID)
  val name = "cluster_session"
}
case object RecapSessionTable extends SessionTable {
  val name = "recap_session"
}

class SessionDb[S <: SessionTable] private[glance] (xa: Transactor[Task]) extends DbRepository[S] {

  override type PK = UUID
  override val init = _
  override val list = _

  override def find(id: UUID) = ???

  override def save(entry: S) = ???

  override def delete(id: UUID) = ???
}

case class SessionDbCompanion[S <: SessionTable](table: S) extends RepositoryCompanion[S, SessionDb[S]] {

  override val initQ = _
  override val listQ = _

  override def findQ(id: UUID) = ???

  override def saveQ(entry: S) = ???

  override def deleteQ(id: UUID) = ???
}
