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

import doobie.imports._
import monix.eval.Task
import uk.ac.ncl.la.soar.StudentNumber
import uk.ac.ncl.la.soar.data.Student

class StudentDb private[glance] (xa: Transactor[Task]) extends Repository[Student] {

  import StudentDb._

  type PK = StudentNumber

  override val init: Task[Unit] = initQ.transact(xa)

  override val list: Task[List[Student]] = listQ.transact(xa)

  override def find(id: StudentNumber): Task[Option[Student]] = findQ(id).transact(xa)

  override def save(entry: Student): Task[Unit] = saveQ(entry).transact(xa)

  override def delete(id: StudentNumber): Task[Boolean] = deleteQ(id).transact(xa)
}

object StudentDb extends RepositoryCompanion[Student, StudentDb] {

  //TODO, remove unnecessary DDL now that we use Flyway for migrations
  override val initQ: ConnectionIO[Unit] = {
    sql"""
      CREATE TABLE IF NOT EXISTS students(
        num VARCHAR(10) PRIMARY KEY
      );
    """.update.run.void
  }

  override val listQ: ConnectionIO[List[Student]] = sql"SELECT * FROM students;".query[Student].list

  override def findQ(id: StudentNumber): ConnectionIO[Option[Student]] =
    sql"SELECT * FROM students s WHERE s.num = $id;".query[Student].option

  override def saveQ(entry: Student): ConnectionIO[Unit] =
    sql"INSERT INTO students (num) VALUES (${entry.number});".update.run.void

  override def deleteQ(id: StudentNumber): ConnectionIO[Boolean] =
    sql"DELETE FROM students s WHERE s.num = $id;".update.run.map(_ > 0)
}
