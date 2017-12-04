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
package uk.ac.ncl.la.soar.glance.eval.db

import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

import doobie.imports._
import monix.eval.Task
import monix.cats._
import cats._
import cats.data.NonEmptyVector
import cats.implicits._
import uk.ac.ncl.la.soar.{ModuleCode, StudentNumber}
import uk.ac.ncl.la.soar.db.{Repository, RepositoryCompanion}
import uk.ac.ncl.la.soar.glance.eval.{ClusterSession, Collection, RecapSession, Survey}

class CollectionDb private[glance] (xa: Transactor[Task]) extends Repository[Collection] {

  import CollectionDb._

  type PK = UUID

  override val init = initQ.transact(xa)
  override val list = listQ.transact(xa)

  override def find(id: UUID) = ???

  override def save(entry: Collection) = ???

  override def delete(id: UUID) =  ???
}

object CollectionDb extends RepositoryCompanion[Collection, CollectionDb] {

  type CollectionRow = (UUID, ModuleCode, Int)
  type CollectionMembership = (UUID, Int, Boolean)

  implicit val uuidMeta: Meta[UUID] = Meta[String].nxmap(UUID.fromString, _.toString)

  override val initQ = ().pure[ConnectionIO]
  override val listQ = List.empty[Collection].pure[ConnectionIO]

  override def findQ(id: UUID) = {
    for {
      cR <- findCollectionRowQ(id)
      cMR <- findCollectionMembershipsQ(id)
    } yield cR.flatMap {
      case (cId, module, num) =>
        cMR.toVector.sortBy(_._2).map(_._1) match {
          case hd +: tl => Some(Collection(cId, module, NonEmptyVector(hd, tl)))
          case _ => None
        }
    }
  }

  private def findCollectionRowQ(id: UUID): ConnectionIO[Option[CollectionRow]] =
    sql"""
      SELECT c.id, c.module_num, c.num_entries FROM collection c WHERE c.id = $id;
    """.query[CollectionRow].option

  private def findCollectionMembershipsQ(id: UUID): ConnectionIO[List[CollectionMembership]] =
    sql"""
      SELECT c.collection_id, c.survey_id, c.membership_idx, c.last
      FROM collection_membership c WHERE c.collection_id = $id;
    """.query[CollectionMembership].list


  override def saveQ(entry: Collection) = ???

  override def deleteQ(id: UUID) = ???
}