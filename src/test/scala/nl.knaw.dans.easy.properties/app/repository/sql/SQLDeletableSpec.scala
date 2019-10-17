/**
 * Copyright (C) 2019 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.properties.app.repository.sql

import java.util.UUID

import cats.scalatest.EitherValues
import cats.syntax.either._
import nl.knaw.dans.easy.properties.fixture.{ DatabaseDataFixture, DatabaseFixture, FileSystemSupport, TestSupportFixture }

import scala.util.{ Failure, Success, Try }

class SQLDeletableSpec extends TestSupportFixture
  with FileSystemSupport
  with DatabaseFixture
  with DatabaseDataFixture
  with EitherValues {

  private val uuid5: UUID = UUID.fromString("00000000-0000-0000-0000-000000000005") // exists
  private val uuid6: UUID = UUID.fromString("00000000-0000-0000-0000-000000000006") // does not exist

  "delete" should "fail with a foreign key violation" in {
    new SQLDepositDao().deleteBy(Seq(uuid5)).leftValue.msg shouldBe
      "integrity constraint violation: foreign key no action; SYS_FK_10153 table: STATE"
  }

  it should "fail with a NullPointerException" in {
    // TODO MutationError?
    val repo = new SQLRepo()
    Try(repo.deleteDepositsBy(Seq(null))) should matchPattern {
      case Failure(e) if e.isInstanceOf[NullPointerException] =>
    }
  }

  it should "fail on a null as id" in {
    new SQLStateDao().deleteBy(Seq(null)).leftValue.msg shouldBe "null"
  }

  it should "succeed with a mix of existing and non existing IDs" in {
    val uuids = Seq(uuid5, uuid6)
    val repo = new SQLRepo()
    val stateDao = repo.repository.states

    // just sampling preconditions of one other table
    stateDao.getAll(uuids).getOrElse(fail) should matchPattern {
      case List((_, List(_, _, _, _)), (_, List())) => // 4 states were found
    }

    repo.deleteDepositsBy(uuids) should matchPattern {
      case Right(List(_)) => // just one of the IDs is found in the deposits table
    }
    repo.repository.deposits.find(uuids).getOrElse(fail) shouldBe empty

    // sampling post conditions of the same other table
    stateDao.getAll(uuids).getOrElse(fail) should matchPattern {
      case List((_, List()), (_, List())) => // no states were found
    }
  }

  it should "succeed with a non existing ID" in {
    new SQLRepo().deleteDepositsBy(Seq(uuid6)) should matchPattern {
      case Right(List()) =>
    }
  }

  it should "succeed when one of the tables was empty" in {
    val uuids = Seq(uuid5)
    val repo = new SQLRepo()
    repo.repository.identifiers.deleteBy(uuids) // clear one of the DAOs

    Try(repo.deleteDepositsBy(uuids)) shouldBe Success(uuids.toList.asRight)
  }
}
