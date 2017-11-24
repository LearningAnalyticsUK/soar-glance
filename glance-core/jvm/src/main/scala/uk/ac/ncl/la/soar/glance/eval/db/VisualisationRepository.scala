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

import cats._
import cats.implicits._
import doobie.imports._
import monix.eval.Task
import monix.cats._
import uk.ac.ncl.la.soar.db.{Repository, RepositoryCompanion}
import uk.ac.ncl.la.soar.glance.eval.Visualisation

/**
  * Database repository for Visualisations
  */
class VisualisationDb private[glance] (xa: Transactor[Task]) extends Repository[Visualisation]{

  import VisualisationDbCompanion._

  override type PK = String
  override val init = initQ.transact(xa)
  override val list = listQ.transact(xa)

  override def find(id: String) = findQ(id).transact(xa)

  override def save(entry: Visualisation) = saveQ(entry).transact(xa)

  override def delete(id: String) = deleteQ(id).transact(xa)
}

object VisualisationDbCompanion extends RepositoryCompanion[Visualisation, VisualisationDb] {

  override val initQ: ConnectionIO[Unit] = Visualisation.all.traverse(saveQ).void
  override val listQ: ConnectionIO[List[Visualisation]] =
    sql"SELECT * FROM visualisation;".query[Visualisation].list

  override def findQ(id: String): ConnectionIO[Option[Visualisation]] =
    sql"SELECT v FROM visualisation v WHERE v.id = $id;".query[Visualisation].option
  
  override def saveQ(entry: Visualisation): ConnectionIO[Unit] =
    sql"""
      INSERT INTO visualisation (id, name, description)
      VALUES (${entry.id}, ${entry.name}, ${entry.description}) ON CONFLICT (id) DO UPDATE
      SET name = ${entry.name}, description = ${entry.description};
    """.update.run.void

  override def deleteQ(id: String): ConnectionIO[Boolean] =
    sql"DELETE FROM visualisation v WHERE v.id = $id;".update.run.map(_ > 0)
}
