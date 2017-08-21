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

import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

import doobie.imports._
import monix.eval.Task
import monix.cats._
import cats._
import cats.implicits._
import uk.ac.ncl.la.soar.StudentNumber
import uk.ac.ncl.la.soar.db.{RepositoryCompanion, Repository => DbRepository}


sealed trait SessionTable {
  type PK
  type Row
  def name: String
  def pkName: String = "id"
}
case object ClusterSessionTable extends SessionTable {
  type PK = UUID
  type Row = (UUID, Instant, Instant, String, StudentNumber)
  val name = "cluster_session"
}
case object RecapSessionTable extends SessionTable {
  type PK = UUID
  type Row = (UUID, Instant, StudentNumber, Int)
  val name = "recap_session"
}

class ClusterSessionDb private[glance] (xa: Transactor[Task]) extends DbRepository[ClusterSessionTable.Row] {

  import ClusterSessionDbCompanion._

  override type PK = UUID
  override val init = initQ.transact(xa)
  override val list = listQ.transact(xa)

  override def find(id: UUID) = findQ(id).transact(xa)

  override def save(entry: ClusterSessionTable.Row) = saveQ(entry).transact(xa)

  override def delete(id: UUID) = deleteQ(id).transact(xa)
}

object ClusterSessionDbCompanion extends RepositoryCompanion[ClusterSessionTable.Row, ClusterSessionDb] {

  implicit val uuidMeta: Meta[UUID] = Meta[String].nxmap(UUID.fromString, _.toString)
  implicit val InstantMeta: Meta[Instant] = Meta[Timestamp].nxmap(_.toInstant, Timestamp.from)

  override val initQ: ConnectionIO[Unit] = ().pure[ConnectionIO]

  override val listQ: ConnectionIO[List[ClusterSessionTable.Row]] =
    sql"SELECT * FROM cluster_session;".query[ClusterSessionTable.Row].list

  override def findQ(id: UUID): ConnectionIO[Option[ClusterSessionTable.Row]] =
    sql"SELECT s FROM cluster_session s WHERE s.id = $id;".query[ClusterSessionTable.Row].option

  override def saveQ(entry: ClusterSessionTable.Row): ConnectionIO[Unit] = {
      val addClusterSql =
        """
          INSERT INTO cluster_session (id, start_time, end_time, machine_name, student_num)
          VALUES (?, ?, ?, ?, ?) ON CONFLICT (id) DO NOTHING;
        """
      Update[ClusterSessionTable.Row](addClusterSql).toUpdate0(entry).run.void
  }

  override def deleteQ(id: UUID): ConnectionIO[Boolean] =
    sql"DELETE FROM cluster_session WHERE id = $id;".update.run.map(_ > 0)
}

class RecapSessionDb private[glance] (xa: Transactor[Task]) extends DbRepository[RecapSessionTable.Row] {

  import RecapSessionDbCompanion._

  override type PK = UUID
  override val init = initQ.transact(xa)
  override val list = listQ.transact(xa)

  override def find(id: UUID) = findQ(id).transact(xa)

  override def save(entry: RecapSessionTable.Row) = saveQ(entry).transact(xa)

  override def delete(id: UUID) = deleteQ(id).transact(xa)
}

object RecapSessionDbCompanion extends RepositoryCompanion[RecapSessionTable.Row, RecapSessionDb] {

  implicit val uuidMeta: Meta[UUID] = Meta[String].nxmap(UUID.fromString, _.toString)
  implicit val InstantMeta: Meta[Instant] = Meta[Timestamp].nxmap(_.toInstant, Timestamp.from)

  override val initQ: ConnectionIO[Unit] = ().pure[ConnectionIO]

  override val listQ: ConnectionIO[List[RecapSessionTable.Row]] =
    sql"SELECT * FROM recap_session;".query[RecapSessionTable.Row].list

  override def findQ(id: UUID): ConnectionIO[Option[RecapSessionTable.Row]] =
    sql"SELECT s FROM recap_session s WHERE s.id = $id;".query[RecapSessionTable.Row].option

  override def saveQ(entry: RecapSessionTable.Row): ConnectionIO[Unit] = {
    val addRecapSql =
      """
          INSERT INTO recap_session (id, start_time, student_num, seconds_listened)
          VALUES (?, ?, ?, ?) ON CONFLICT (id) DO NOTHING;
      """
    Update[RecapSessionTable.Row](addRecapSql).toUpdate0(entry).run.void
  }

  override def deleteQ(id: UUID): ConnectionIO[Boolean] =
    sql"DELETE FROM recap_session WHERE id = $id;".update.run.map(_ > 0)
}
