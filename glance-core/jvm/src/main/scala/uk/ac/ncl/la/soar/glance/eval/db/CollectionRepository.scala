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
import cats.data.{NonEmptyVector, OptionT}
import cats.implicits._
import uk.ac.ncl.la.soar.{ModuleCode, StudentNumber}
import uk.ac.ncl.la.soar.db.{Repository, RepositoryCompanion}
import uk.ac.ncl.la.soar.glance.eval.{
  ClusterSession,
  Collection,
  RecapSession,
  Survey
}

class CollectionDb private[glance] (xa: Transactor[Task])
    extends Repository[Collection] {

  import CollectionDb._

  type PK = UUID

  override val init = initQ.transact(xa)
  override val list = listQ.transact(xa)

  override def find(id: UUID) = findQ(id).transact(xa)

  def findIdx(id: UUID, idx: Int): F[Option[Survey]] =
    findIdxQ(id, idx).transact(xa)

  def findFirst(id: UUID): F[Option[Survey]] = findIdx(id, 0)

  override def save(entry: Collection) = saveQ(entry).transact(xa)

  override def delete(id: UUID) = deleteQ(id).transact(xa)
}

object CollectionDb extends RepositoryCompanion[Collection, CollectionDb] {

  type CollectionRow = (UUID, ModuleCode, Int)
  type CollectionMembership = (UUID, Int, Boolean)
  type CollectionMembershipRow = (UUID, UUID, Int, Boolean)

  implicit val uuidMeta: Meta[UUID] =
    Meta[String].nxmap(UUID.fromString, _.toString)

  override val initQ: ConnectionIO[Unit] = ().pure[ConnectionIO]
  override val listQ: ConnectionIO[List[Collection]] = {
    type Row = (UUID, ModuleCode, Int, UUID, Int)
    //TODO: Read up on process/stream api from fs2!
    val rowsQ =
      sql"""
        SELECT c.id, c.module_num, c.num_entries, cm.survey_id, cm.membership_idx
        FROM collection c, collection_membership cm
        WHERE c.id == cm.collection_id;
      """.query[Row].list

    rowsQ.map { rows: List[Row] =>
      val collToMem = rows.groupBy(r => (r._1, r._2, r._3))
      val memSorted = collToMem.collect {
        case (c, m) if m.nonEmpty => c -> m.sortBy(_._2).map(_._1)
      }
      val collOpts = memSorted.flatMap {
        case (c, hd :: tl) =>
          Some(Collection(c._1, c._2, NonEmptyVector(hd, tl.toVector)))
        case (c, Nil) => None
      }
      collOpts.toList
    }
  }

  def findIdxQ(id: UUID, idx: Int): ConnectionIO[Option[Survey]] = {

    val surveyOpt = for {
      collection <- OptionT(findQ(id))
      surveyId <- OptionT.fromOption[ConnectionIO](
        collection.surveyIds.get(idx))
      survey <- OptionT(SurveyDb.findQ(surveyId))
    } yield survey

    surveyOpt.value
  }

  //TODO: Look at refactoring or factoring out this method and the above. One must be better than the other
  override def findQ(id: UUID): ConnectionIO[Option[Collection]] = {
    for {
      cR <- findCollectionRowQ(id)
      cMR <- findCollectionMembershipsQ(id)
    } yield
      cR.flatMap {
        case (cId, module, num) =>
          cMR.toVector.sortBy(_._2).map(_._1) match {
            case hd +: tl =>
              Some(Collection(cId, module, NonEmptyVector(hd, tl)))
            case _ => None
          }
      }
  }

  private def findCollectionRowQ(id: UUID) =
    sql"""
      SELECT c.id, c.module_num, c.num_entries FROM collection c WHERE c.id = $id;
    """.query[CollectionRow].option

  private def findCollectionMembershipsQ(id: UUID) =
    sql"""
      SELECT c.survey_id, c.membership_idx, c.last
      FROM collection_membership c WHERE c.collection_id = $id;
    """.query[CollectionMembership].list

  override def saveQ(entry: Collection): ConnectionIO[Unit] = {

    //Batch insert entries into collection_membership table
    val addMembershipsSQL =
      """
        INSERT INTO collection_membership (collection_id, survey_id, membership_idx, last)
        VALUES (?, ?, ?, ?);
      """

    val addCollectionQ =
      sql"""
        INSERT INTO collection (id, module_num, num_entries)
        VALUES (${entry.id}, ${entry.module}, ${entry.numEntries});
      """.update.run

    //Prepare membership rows
    val surveyIndices = entry.surveyIds.toVector.zipWithIndex

    //Add collection id and "last" flag
    val last = entry.numEntries - 1
    val membershipRows = surveyIndices.map {
      case (s, i) if i == last => (entry.id, s, i, true)
      case (s, i)              => (entry.id, s, i, false)
    }

    //Add all db rows
    for {
      _ <- addCollectionQ
      _ <- Update[CollectionMembershipRow](addMembershipsSQL)
        .updateMany(membershipRows)
    } yield ()
  }

  override def deleteQ(id: UUID) =
    sql"DELETE FROM collection c WHERE c.id = $id;".update.run.map(_ > 0)
}
