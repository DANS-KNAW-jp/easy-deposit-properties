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

import cats.scalatest.{ EitherMatchers, EitherValues }
import nl.knaw.dans.easy.properties.app.model.springfield.{ InputSpringfield, Springfield, SpringfieldPlayMode }
import nl.knaw.dans.easy.properties.app.repository.{ DepositIdAndTimestampAlreadyExistError, InvalidValueError, NoSuchDepositError }
import nl.knaw.dans.easy.properties.fixture.{ DatabaseDataFixture, DatabaseFixture, FileSystemSupport, TestSupportFixture }
import org.joda.time.DateTime

class SQLSpringfieldDaoSpec extends TestSupportFixture
  with FileSystemSupport
  with DatabaseFixture
  with DatabaseDataFixture
  with EitherValues
  with EitherMatchers {

  "getById" should "find springfield configurations identified by their springfieldId" in {
    val springfields = new SQLSpringfieldDao

    springfields.getById(Seq("0", "1", "2")).value should contain inOrderOnly(springfield0, springfield1, springfield2)
  }

  it should "return an empty collection if the springfieldId is unknown" in {
    val springfields = new SQLSpringfieldDao
    val unknownspringfieldId = "102"

    springfields.getById(Seq(unknownspringfieldId)).value shouldBe empty
  }

  it should "return an empty collection when the input collection is empty" in {
    val springfields = new SQLSpringfieldDao

    springfields.getById(Seq.empty).value shouldBe empty
  }

  it should "fail when an invalid springfieldId is given" in {
    val springfields = new SQLSpringfieldDao

    springfields.getById(Seq("12", "invalid-id", "29")).leftValue shouldBe InvalidValueError("invalid id 'invalid-id'")
  }

  "getCurrent" should "return the current springfield configurations of the given deposits" in {
    val springfields = new SQLSpringfieldDao

    springfields.getCurrent(Seq(depositId1, depositId5)).value should contain only (depositId1 -> springfield0)
  }

  it should "return an empty collection if the depositId is unknown" in {
    val springfields = new SQLSpringfieldDao
    val depositId6 = UUID.fromString("00000000-0000-0000-0000-000000000006")

    springfields.getCurrent(Seq(depositId6)).value shouldBe empty
  }

  it should "return an empty collection when the input collection is empty" in {
    val springfields = new SQLSpringfieldDao

    springfields.getCurrent(Seq.empty).value shouldBe empty
  }

  "getAll" should "return all springfield configurations associated with the given deposits" in {
    val springfields = new SQLSpringfieldDao

    springfields.getAll(Seq(depositId2, depositId5)).value should contain only(
      depositId2 -> Seq(springfield1, springfield2),
      depositId5 -> Seq.empty,
    )
  }

  it should "return an empty collection if the depositId is unknown" in {
    val springfields = new SQLSpringfieldDao
    val depositId6 = UUID.fromString("00000000-0000-0000-0000-000000000006")

    springfields.getAll(Seq(depositId6)).value should contain only (depositId6 -> Seq.empty)
  }

  it should "return an empty collection when the input collection is empty" in {
    val springfields = new SQLSpringfieldDao

    springfields.getAll(Seq.empty).value shouldBe empty
  }

  "store" should "insert a new springfield into the database" in {
    val springfields = new SQLSpringfieldDao
    val timestamp = new DateTime(2019, 7, 19, 22, 45, timeZone)
    val inputSpringfield = InputSpringfield("ddd", "uuu", "ccc", SpringfieldPlayMode.MENU, timestamp)
    val expectedSpringfield = Springfield("3", "ddd", "uuu", "ccc", SpringfieldPlayMode.MENU, timestamp)

    springfields.store(depositId1, inputSpringfield).value shouldBe expectedSpringfield
    springfields.getById(Seq("3")).value should contain only expectedSpringfield
    springfields.getCurrent(Seq(depositId1)).value should contain only (depositId1 -> expectedSpringfield)
    springfields.getAll(Seq(depositId1)).value.toMap.apply(depositId1) should contain(expectedSpringfield)
  }

  it should "fail when the given depositId does not exist" in {
    val springfields = new SQLSpringfieldDao
    val depositId6 = UUID.fromString("00000000-0000-0000-0000-000000000006")
    val timestamp = new DateTime(2019, 7, 18, 22, 38, timeZone)
    val inputSpringfield = InputSpringfield("domain", "user", "collection", SpringfieldPlayMode.MENU, timestamp)

    springfields.store(depositId6, inputSpringfield).leftValue shouldBe NoSuchDepositError(depositId6)
  }

  it should "fail when the depositId and timestamp combination is already present, even though the other values are different" in {
    val springfields = new SQLSpringfieldDao
    val depositId = depositId1
    val timestamp = new DateTime(2019, 1, 1, 6, 6, timeZone)
    val inputSpringfield1 = InputSpringfield("domain1", "user1", "collection1", SpringfieldPlayMode.CONTINUOUS, timestamp)
    val inputSpringfield2 = InputSpringfield("domain2", "user2", "collection2", SpringfieldPlayMode.MENU, timestamp)

    springfields.store(depositId, inputSpringfield1) shouldBe right
    springfields.store(depositId, inputSpringfield2).leftValue shouldBe DepositIdAndTimestampAlreadyExistError(depositId, timestamp, "springfield")
  }

  "getDepositsById" should "find deposits identified by these springfieldIds" in {
    val springfields = new SQLSpringfieldDao

    springfields.getDepositsById(Seq("0", "1", "2")).value should contain only(
      "0" -> deposit1,
      "1" -> deposit2,
      "2" -> deposit2,
    )
  }

  it should "return an empty collection if the springfieldId is unknown" in {
    val springfields = new SQLSpringfieldDao
    val unknownspringfieldId = "102"

    springfields.getDepositsById(Seq(unknownspringfieldId)).value shouldBe empty
  }

  it should "return an empty collection when the input collection is empty" in {
    val springfields = new SQLSpringfieldDao

    springfields.getDepositsById(Seq.empty).value shouldBe empty
  }

  it should "fail when an invalid springfieldId is given" in {
    val springfields = new SQLSpringfieldDao

    springfields.getDepositsById(Seq("12", "invalid-id", "29")).leftValue shouldBe InvalidValueError("invalid id 'invalid-id'")
  }
}
