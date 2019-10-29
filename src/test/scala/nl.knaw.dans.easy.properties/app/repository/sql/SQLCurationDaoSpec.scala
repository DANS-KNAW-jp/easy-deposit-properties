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
import cats.syntax.option._
import nl.knaw.dans.easy.properties.app.model.curation.{ Curation, InputCuration }
import nl.knaw.dans.easy.properties.app.repository.{ DepositIdAndTimestampAlreadyExistError, InvalidValueError, NoSuchDepositError }
import nl.knaw.dans.easy.properties.fixture.{ DatabaseDataFixture, DatabaseFixture, FileSystemSupport, TestSupportFixture }
import org.joda.time.DateTime

class SQLCurationDaoSpec extends TestSupportFixture
  with FileSystemSupport
  with DatabaseFixture
  with DatabaseDataFixture
  with EitherValues
  with EitherMatchers {

  "getById" should "find curation configurations identified by their curationId" in {
    val curations = new SQLCurationDao

    curations.getById(Seq("2", "4", "9")).value should contain inOrderOnly(curation2, curation4, curation9)
  }

  it should "return an empty collection if the curationId is unknown" in {
    val curations = new SQLCurationDao
    val unknowncurationId = "102"

    curations.getById(Seq(unknowncurationId)).value shouldBe empty
  }

  it should "return an empty collection when the input collection is empty" in {
    val curations = new SQLCurationDao

    curations.getById(Seq.empty).value shouldBe empty
  }

  it should "fail when an invalid curationId is given" in {
    val curations = new SQLCurationDao

    curations.getById(Seq("2", "invalid-id", "29")).leftValue shouldBe InvalidValueError("invalid id 'invalid-id'")
  }

  "getCurrent" should "return the current curation configurations of the given deposits" in {
    val curations = new SQLCurationDao

    curations.getCurrent(Seq(depositId1, depositId2)).value should contain only (depositId1 -> curation2)
  }

  it should "return an empty collection if the depositId is unknown" in {
    val curations = new SQLCurationDao
    val depositId6 = UUID.fromString("00000000-0000-0000-0000-000000000006")

    curations.getCurrent(Seq(depositId6)).value shouldBe empty
  }

  it should "return an empty collection when the input collection is empty" in {
    val curations = new SQLCurationDao

    curations.getCurrent(Seq.empty).value shouldBe empty
  }

  "getAll" should "return all curation configurations associated with the given deposits" in {
    val curations = new SQLCurationDao

    curations.getAll(Seq(depositId4, depositId2)).value should contain only(
      depositId4 -> Seq(curation6, curation7),
      depositId2 -> Seq.empty,
    )
  }

  it should "return an empty collection if the depositId is unknown" in {
    val curations = new SQLCurationDao
    val depositId6 = UUID.fromString("00000000-0000-0000-0000-000000000006")

    curations.getAll(Seq(depositId6)).value should contain only (depositId6 -> Seq.empty)
  }

  it should "return an empty collection when the input collection is empty" in {
    val curations = new SQLCurationDao

    curations.getAll(Seq.empty).value shouldBe empty
  }

  "store" should "insert a new curation into the database" in {
    val curations = new SQLCurationDao
    val timestamp = new DateTime(2019, 7, 20, 21, 12, timeZone)
    val inputCuration = InputCuration(isNewVersion = true.some, isRequired = true, isPerformed = false, "my-username", "foo@bar.com", timestamp)
    val expectedCuration = Curation("10", isNewVersion = true.some, isRequired = true, isPerformed = false, "my-username", "foo@bar.com", timestamp)

    curations.store(depositId1, inputCuration).value shouldBe expectedCuration
    curations.getById(Seq("10")).value should contain only expectedCuration
    curations.getCurrent(Seq(depositId1)).value should contain only (depositId1 -> expectedCuration)
    curations.getAll(Seq(depositId1)).value.toMap.apply(depositId1) should contain(expectedCuration)
  }

  it should "insert a new curation into the database with NULL for isNewVersion" in {
    val curations = new SQLCurationDao
    val timestamp = new DateTime(2019, 7, 20, 21, 12, timeZone)
    val inputCuration = InputCuration(isNewVersion = none, isRequired = true, isPerformed = false, "my-username", "foo@bar.com", timestamp)
    val expectedCuration = Curation("10", isNewVersion = none, isRequired = true, isPerformed = false, "my-username", "foo@bar.com", timestamp)

    curations.store(depositId1, inputCuration).value shouldBe expectedCuration
    curations.getById(Seq("10")).value should contain only expectedCuration
    curations.getCurrent(Seq(depositId1)).value should contain only (depositId1 -> expectedCuration)
    curations.getAll(Seq(depositId1)).value.toMap.apply(depositId1) should contain(expectedCuration)
  }

  it should "fail when the given depositId does not exist" in {
    val curations = new SQLCurationDao
    val depositId6 = UUID.fromString("00000000-0000-0000-0000-000000000006")
    val timestamp = new DateTime(2019, 7, 18, 22, 38, timeZone)
    val inputCuration = InputCuration(isNewVersion = true.some, isRequired = true, isPerformed = false, "my-username", "foo@bar.com", timestamp)

    curations.store(depositId6, inputCuration).leftValue shouldBe NoSuchDepositError(depositId6)
  }

  it should "fail when the depositId and timestamp combination is already present, even though the other values are different" in {
    val curations = new SQLCurationDao
    val depositId = depositId1
    val timestamp = new DateTime(2019, 1, 1, 5, 5, timeZone)
    val inputCuration1 = InputCuration(isNewVersion = true.some, isRequired = true, isPerformed = false, "my-username", "foo@bar.com", timestamp)
    val inputCuration2 = InputCuration(isNewVersion = false.some, isRequired = false, isPerformed = true, "foo", "foo@bar.com", timestamp)

    curations.store(depositId, inputCuration1) shouldBe right
    curations.store(depositId, inputCuration2).leftValue shouldBe DepositIdAndTimestampAlreadyExistError(depositId, timestamp, "curation")
  }

  "getDepositsById" should "find deposits identified by these curationIds" in {
    val curations = new SQLCurationDao

    curations.getDepositsById(Seq("1", "4", "7", "10")).value should contain only(
      "1" -> deposit1,
      "4" -> deposit3,
      "7" -> deposit4,
    )
  }

  it should "return an empty collection if the curationId is unknown" in {
    val curations = new SQLCurationDao
    val unknowncurationId = "102"

    curations.getDepositsById(Seq(unknowncurationId)).value shouldBe empty
  }

  it should "return an empty collection when the input collection is empty" in {
    val curations = new SQLCurationDao

    curations.getDepositsById(Seq.empty).value shouldBe empty
  }

  it should "fail when an invalid curationId is given" in {
    val curations = new SQLCurationDao

    curations.getDepositsById(Seq("12", "invalid-id", "29")).leftValue shouldBe InvalidValueError("invalid id 'invalid-id'")
  }
}
