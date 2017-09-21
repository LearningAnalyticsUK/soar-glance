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
package uk.ac.ncl.la.soar.db

import java.sql.Date
import java.util.UUID

import cats._
import cats.implicits._
import doobie.imports._
import doobie.postgres.imports._
import monix.eval._
import monix.cats._
import uk.ac.ncl.la.soar.{ModuleCode, StudentNumber}
import uk.ac.ncl.la.soar.data.{Module, ModuleRecords, ModuleScore}
import Implicits._



class ModuleScoreDb(xa: Transactor[Task]) extends Repository[ModuleScore] {

  import ModuleScoreDb._

  override type PK = UUID
  override val init: Task[Unit] = initQ.transact(xa)
  override val list: Task[List[ModuleScore]] = listQ.transact(xa)

  override def find(id: UUID): Task[Option[ModuleScore]] = findQ(id).transact(xa)

  override def save(entry: ModuleScore): Task[Unit] = saveQ(entry).transact(xa)

  def saveBatch(entries: List[ModuleScore]): Task[Int] = saveBatchQ(entries).transact(xa)

  override def delete(id: UUID): Task[Boolean] = deleteQ(id).transact(xa)
}

object ModuleScoreDb extends RepositoryCompanion[ModuleScore, ModuleScoreDb] {

  type ModuleScoreRow = (UUID, StudentNumber, ModuleCode, Double)

  override val initQ = ().pure[ConnectionIO]


  //TODO: Fix
  override val listQ: ConnectionIO[List[ModuleScore]] =
    sql"SELECT * FROM module_score;".query[ModuleScoreRow].list.map(_.flatMap {
      case (id, stud, mod, sc) => ModuleScore(stud, mod, sc)
    })

  override def findQ(id: UUID): ConnectionIO[Option[ModuleScore]] =
    sql"SELECT * FROM module_score ms WHERE ms.id = $id;".query[ModuleScoreRow].option.map(_.flatMap {
      case (_, stud, mod, sc) => ModuleScore(stud, mod, sc)
    })

  override def saveQ(entry: ModuleScore): ConnectionIO[Unit] =
    sql"""
      INSERT INTO module_score (id, student_num, module_num, score)
      VALUES (${UUID.randomUUID()}, ${entry.student}, ${entry.module}, ${entry.score});
    """.update.run.map(_ => ())

  def saveBatchQ(entries: List[ModuleScore]): ConnectionIO[Int] = {
    val moduleScoreSQL =
      """
        INSERT INTO module_score (id, student_num, module_num, score)
        SELECT ?, ?, ?, ?
        WHERE EXISTS (SELECT * FROM student WHERE num = ?)
        ON CONFLICT (id) DO NOTHING;
      """
    val mSRows = entries.map({ case ModuleScore(stud, module, score) => (UUID.randomUUID, stud, module, score, stud) })
    Update[(UUID, StudentNumber, ModuleCode, Double, StudentNumber)](moduleScoreSQL).updateMany(mSRows)
  }

  override def deleteQ(id: UUID): ConnectionIO[Boolean] = ???
}



