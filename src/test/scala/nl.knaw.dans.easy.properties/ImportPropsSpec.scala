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
package nl.knaw.dans.easy.properties

import java.net.URL
import java.util.{ TimeZone, UUID }

import better.files.File
import cats.scalatest.{ EitherMatchers, EitherValues }
import cats.syntax.either._
import cats.syntax.option._
import nl.knaw.dans.easy.properties.app.legacyImport.{ ImportProps, Interactor, NoDepositIdError, NoSuchPropertiesFileError }
import nl.knaw.dans.easy.properties.app.model.contentType.{ ContentType, ContentTypeValue, InputContentType }
import nl.knaw.dans.easy.properties.app.model.curator.{ Curator, InputCurator }
import nl.knaw.dans.easy.properties.app.model.identifier.{ Identifier, IdentifierType, InputIdentifier }
import nl.knaw.dans.easy.properties.app.model.ingestStep.{ IngestStep, IngestStepLabel, InputIngestStep }
import nl.knaw.dans.easy.properties.app.model.springfield.{ InputSpringfield, Springfield, SpringfieldPlayMode }
import nl.knaw.dans.easy.properties.app.model.state.{ InputState, State, StateLabel }
import nl.knaw.dans.easy.properties.app.model.{ CurationPerformedEvent, CurationRequiredEvent, Deposit, DepositId, DoiAction, DoiActionEvent, DoiRegisteredEvent, IsNewVersionEvent, Timestamp }
import nl.knaw.dans.easy.properties.app.repository.DepositRepository
import nl.knaw.dans.easy.properties.fixture.{ FileSystemSupport, TestSupportFixture }
import nl.knaw.dans.easy.{ DataciteService, DataciteServiceConfiguration, DataciteServiceException }
import org.joda.time.{ DateTime, DateTimeZone }
import org.scalamock.scalatest.MockFactory

class ImportPropsSpec extends TestSupportFixture
  with FileSystemSupport
  with MockFactory
  with EitherMatchers with EitherValues {

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    File(getClass.getResource("/legacy-import").toURI) copyTo testDir
  }

  private class MockDataciteService extends DataciteService(new DataciteServiceConfiguration() {
    setDatasetResolver(new URL("http://does.not.exist.dans.knaw.nl"))
  })

  private val repo = mock[DepositRepository]
  private val interactor = mock[Interactor]
  private val datacite = mock[MockDataciteService]
  private val timeZone = DateTimeZone.forTimeZone(TimeZone.getTimeZone("Europe/Amsterdam"))
  private val time = new DateTime(2019, 1, 1, 0, 0, timeZone)
  private val importProps = new ImportProps(repo, interactor, datacite)

  private def fileProps(file: File): (DepositId, Timestamp, Timestamp) = {
    (
      UUID.fromString(file.parent.name),
      new DateTime(file.attributes.creationTime().toMillis),
      new DateTime(file.attributes.lastModifiedTime().toMillis),
    )
  }

  "loadDepositProperties" should "read the given deposit.properties file and call the repository on it" in {
    val file = testDir / "readProps" / "bf729483-5d9b-4509-a8f2-91db639fb52f" / "deposit.properties"
    val (depositId, _, lastModified) = fileProps(file)

    inSequence {
      repo.addDeposit _ expects where(isDeposit(Deposit(depositId, "bag".some, time, "user001"))) returning Deposit(depositId, "bag".some, time, "user001").asRight
      repo.setState _ expects(depositId, InputState(StateLabel.SUBMITTED, "my description", lastModified)) returning State("my-id", StateLabel.SUBMITTED, "my description", lastModified).asRight
      repo.setIngestStep _ expects(depositId, InputIngestStep(IngestStepLabel.BAGSTORE, lastModified)) returning IngestStep("my-id", IngestStepLabel.BAGSTORE, lastModified).asRight
      repo.addIdentifier _ expects(depositId, InputIdentifier(IdentifierType.DOI, "my-doi-value", lastModified)) returning Identifier("my-id", IdentifierType.DOI, "my-doi-value", lastModified).asRight
      repo.addIdentifier _ expects(depositId, InputIdentifier(IdentifierType.URN, "my-urn-value", lastModified)) returning Identifier("my-id", IdentifierType.URN, "my-urn-value", lastModified).asRight
      repo.addIdentifier _ expects(depositId, InputIdentifier(IdentifierType.FEDORA, "my-fedora-value", lastModified)) returning Identifier("my-id", IdentifierType.FEDORA, "my-fedora-value", lastModified).asRight
      repo.addIdentifier _ expects(depositId, InputIdentifier(IdentifierType.BAG_STORE, "my-bag-store-value", lastModified)) returning Identifier("my-id", IdentifierType.BAG_STORE, "my-bag-store-value", lastModified).asRight
      repo.setDoiRegistered _ expects(depositId, DoiRegisteredEvent(value = true, lastModified)) returning DoiRegisteredEvent(value = true, lastModified).asRight
      repo.setDoiAction _ expects(depositId, DoiActionEvent(DoiAction.UPDATE, lastModified)) returning DoiActionEvent(DoiAction.UPDATE, lastModified).asRight
      repo.setCurator _ expects(depositId, InputCurator("archie001", "does.not.exists@dans.knaw.nl", lastModified)) returning Curator("my-id", "archie001", "does.not.exists@dans.knaw.nl", lastModified).asRight
      repo.setIsNewVersionAction _ expects(depositId, IsNewVersionEvent(isNewVersion = true, lastModified)) returning IsNewVersionEvent(isNewVersion = true, lastModified).asRight
      repo.setCurationRequiredAction _ expects(depositId, CurationRequiredEvent(curationRequired = false, lastModified)) returning CurationRequiredEvent(curationRequired = false, lastModified).asRight
      repo.setCurationPerformedAction _ expects(depositId, CurationPerformedEvent(curationPerformed = false, lastModified)) returning CurationPerformedEvent(curationPerformed = false, lastModified).asRight
      repo.setSpringfield _ expects(depositId, InputSpringfield("domain", "user", "collection", SpringfieldPlayMode.CONTINUOUS, lastModified)) returning Springfield("my-id", "domain", "user", "collection", SpringfieldPlayMode.CONTINUOUS, lastModified).asRight
      repo.setContentType _ expects(depositId, InputContentType(ContentTypeValue.ZIP, lastModified)) returning ContentType("my-id", ContentTypeValue.ZIP, lastModified).asRight
    }

    importProps.loadDepositProperties(file) shouldBe right
  }

  it should "interact with the user when necessary values don't exist" in {
    val file = testDir / "interact_max" / "0eb8c353-4b41-4db7-9b1c-15e06a69c143" / "deposit.properties"
    val (depositId, creationTime, lastModified) = fileProps(file)

    inSequence {
      (interactor.ask(_: String)) expects * returning "user001"
      repo.addDeposit _ expects where(isDeposit(Deposit(depositId, none, creationTime, "user001"))) returning Deposit(depositId, none, creationTime, "user001").asRight
      (interactor.ask(_: Enumeration)(_: String)) expects(StateLabel, *) returning StateLabel.SUBMITTED.asRight
      (interactor.ask(_: String)) expects * returning "my description"
      repo.setState _ expects(depositId, InputState(StateLabel.SUBMITTED, "my description", lastModified)) returning State("my-id", StateLabel.SUBMITTED, "my-description", lastModified).asRight
      // no repo.setIngestStep
      (interactor.ask(_: String)) expects * returning "my-doi-value"
      repo.addIdentifier _ expects(depositId, InputIdentifier(IdentifierType.DOI, "my-doi-value", lastModified)) returning Identifier("my-id", IdentifierType.DOI, "my-doi-value", lastModified).asRight
      (interactor.ask(_: String)) expects * returning "my-urn-value"
      repo.addIdentifier _ expects(depositId, InputIdentifier(IdentifierType.URN, "my-urn-value", lastModified)) returning Identifier("my-id", IdentifierType.URN, "my-urn-value", lastModified).asRight
      (interactor.ask(_: String)) expects * returning "my-fedora-value"
      repo.addIdentifier _ expects(depositId, InputIdentifier(IdentifierType.FEDORA, "my-fedora-value", lastModified)) returning Identifier("my-id", IdentifierType.FEDORA, "my-fedora-value", lastModified).asRight
      repo.addIdentifier _ expects(depositId, InputIdentifier(IdentifierType.BAG_STORE, depositId.toString, lastModified)) returning Identifier("my-id", IdentifierType.BAG_STORE, depositId.toString, lastModified).asRight
      repo.setDoiRegistered _ expects(depositId, DoiRegisteredEvent(value = true, lastModified)) returning DoiRegisteredEvent(value = true, lastModified).asRight
      repo.setDoiAction _ expects(depositId, DoiActionEvent(DoiAction.CREATE, lastModified)) returning DoiActionEvent(DoiAction.CREATE, lastModified).asRight
      // no repo.setCurator
      // no repo.setIsNewVersionAction
      // no repo.setCurationRequiredAction
      // no repo.setCurationPerformedAction
      // no repo.setSpringfield
      // no repo.setContentType
    }

    importProps.loadDepositProperties(file) shouldBe right
  }

  it should "interact with the user when the state label is invalid" in {
    val time = new DateTime(2019, 1, 1, 0, 0, timeZone)
    val file = testDir / "invalid_state_label" / "667ece00-2083-4930-a1ab-f265aca41021" / "deposit.properties"
    val (depositId, _, lastModified) = fileProps(file)

    inSequence {
      repo.addDeposit _ expects where(isDeposit(Deposit(depositId, "bag".some, time, "user001"))) returning Deposit(depositId, "bag".some, time, "user001").asRight
      (interactor.ask(_: Enumeration)(_: String)) expects(StateLabel, *) returning StateLabel.SUBMITTED.asRight
      repo.setState _ expects(depositId, InputState(StateLabel.SUBMITTED, "my description", lastModified)) returning State("my-id", StateLabel.SUBMITTED, "my description", lastModified).asRight
      repo.setIngestStep _ expects(depositId, InputIngestStep(IngestStepLabel.BAGSTORE, lastModified)) returning IngestStep("my-id", IngestStepLabel.BAGSTORE, lastModified).asRight
      repo.addIdentifier _ expects(depositId, InputIdentifier(IdentifierType.DOI, "my-doi-value", lastModified)) returning Identifier("my-id", IdentifierType.DOI, "my-doi-value", lastModified).asRight
      repo.addIdentifier _ expects(depositId, InputIdentifier(IdentifierType.URN, "my-urn-value", lastModified)) returning Identifier("my-id", IdentifierType.URN, "my-urn-value", lastModified).asRight
      repo.addIdentifier _ expects(depositId, InputIdentifier(IdentifierType.FEDORA, "my-fedora-value", lastModified)) returning Identifier("my-id", IdentifierType.FEDORA, "my-fedora-value", lastModified).asRight
      repo.addIdentifier _ expects(depositId, InputIdentifier(IdentifierType.BAG_STORE, "my-bag-store-value", lastModified)) returning Identifier("my-id", IdentifierType.BAG_STORE, "my-bag-store-value", lastModified).asRight
      repo.setDoiRegistered _ expects(depositId, DoiRegisteredEvent(value = true, lastModified)) returning DoiRegisteredEvent(value = true, lastModified).asRight
      repo.setDoiAction _ expects(depositId, DoiActionEvent(DoiAction.UPDATE, lastModified)) returning DoiActionEvent(DoiAction.UPDATE, lastModified).asRight
      repo.setCurator _ expects(depositId, InputCurator("archie001", "does.not.exists@dans.knaw.nl", lastModified)) returning Curator("my-id", "archie001", "does.not.exists@dans.knaw.nl", lastModified).asRight
      repo.setIsNewVersionAction _ expects(depositId, IsNewVersionEvent(isNewVersion = true, lastModified)) returning IsNewVersionEvent(isNewVersion = true, lastModified).asRight
      repo.setCurationRequiredAction _ expects(depositId, CurationRequiredEvent(curationRequired = false, lastModified)) returning CurationRequiredEvent(curationRequired = false, lastModified).asRight
      repo.setCurationPerformedAction _ expects(depositId, CurationPerformedEvent(curationPerformed = false, lastModified)) returning CurationPerformedEvent(curationPerformed = false, lastModified).asRight
      repo.setSpringfield _ expects(depositId, InputSpringfield("domain", "user", "collection", SpringfieldPlayMode.CONTINUOUS, lastModified)) returning Springfield("my-id", "domain", "user", "collection", SpringfieldPlayMode.CONTINUOUS, lastModified).asRight
      repo.setContentType _ expects(depositId, InputContentType(ContentTypeValue.ZIP, lastModified)) returning ContentType("my-id", ContentTypeValue.ZIP, lastModified).asRight
    }

    importProps.loadDepositProperties(file) shouldBe right
  }

  it should "choose ingest step COMPLETED when it is not provided and state = ARCHIVED" in {
    val time = new DateTime(2019, 1, 1, 0, 0, timeZone)
    val file = testDir / "ingest_step_completed" / "0e00e954-8236-403f-80d3-e67792164b26" / "deposit.properties"
    val (depositId, _, lastModified) = fileProps(file)

    inSequence {
      repo.addDeposit _ expects where(isDeposit(Deposit(depositId, "bag".some, time, "user001"))) returning Deposit(depositId, "bag".some, time, "user001").asRight
      repo.setState _ expects(depositId, InputState(StateLabel.ARCHIVED, "my description", lastModified)) returning State("my-id", StateLabel.ARCHIVED, "my description", lastModified).asRight
      repo.setIngestStep _ expects(depositId, InputIngestStep(IngestStepLabel.COMPLETED, lastModified)) returning IngestStep("my-id", IngestStepLabel.COMPLETED, lastModified).asRight
      repo.addIdentifier _ expects(depositId, InputIdentifier(IdentifierType.DOI, "my-doi-value", lastModified)) returning Identifier("my-id", IdentifierType.DOI, "my-doi-value", lastModified).asRight
      repo.addIdentifier _ expects(depositId, InputIdentifier(IdentifierType.URN, "my-urn-value", lastModified)) returning Identifier("my-id", IdentifierType.URN, "my-urn-value", lastModified).asRight
      repo.addIdentifier _ expects(depositId, InputIdentifier(IdentifierType.FEDORA, "my-fedora-value", lastModified)) returning Identifier("my-id", IdentifierType.FEDORA, "my-fedora-value", lastModified).asRight
      repo.addIdentifier _ expects(depositId, InputIdentifier(IdentifierType.BAG_STORE, "my-bag-store-value", lastModified)) returning Identifier("my-id", IdentifierType.BAG_STORE, "my-bag-store-value", lastModified).asRight
      repo.setDoiRegistered _ expects(depositId, DoiRegisteredEvent(value = true, lastModified)) returning DoiRegisteredEvent(value = true, lastModified).asRight
      repo.setDoiAction _ expects(depositId, DoiActionEvent(DoiAction.UPDATE, lastModified)) returning DoiActionEvent(DoiAction.UPDATE, lastModified).asRight
      repo.setCurator _ expects(depositId, InputCurator("archie001", "does.not.exists@dans.knaw.nl", lastModified)) returning Curator("my-id", "archie001", "does.not.exists@dans.knaw.nl", lastModified).asRight
      repo.setIsNewVersionAction _ expects(depositId, IsNewVersionEvent(isNewVersion = true, lastModified)) returning IsNewVersionEvent(isNewVersion = true, lastModified).asRight
      repo.setCurationRequiredAction _ expects(depositId, CurationRequiredEvent(curationRequired = false, lastModified)) returning CurationRequiredEvent(curationRequired = false, lastModified).asRight
      repo.setCurationPerformedAction _ expects(depositId, CurationPerformedEvent(curationPerformed = false, lastModified)) returning CurationPerformedEvent(curationPerformed = false, lastModified).asRight
      repo.setSpringfield _ expects(depositId, InputSpringfield("domain", "user", "collection", SpringfieldPlayMode.CONTINUOUS, lastModified)) returning Springfield("my-id", "domain", "user", "collection", SpringfieldPlayMode.CONTINUOUS, lastModified).asRight
      repo.setContentType _ expects(depositId, InputContentType(ContentTypeValue.ZIP, lastModified)) returning ContentType("my-id", ContentTypeValue.ZIP, lastModified).asRight
    }

    importProps.loadDepositProperties(file) shouldBe right
  }

  it should "interact with DataCite when the identifier.dans-doi.registered property is not set" in {
    val time = new DateTime(2019, 1, 1, 0, 0, timeZone)
    val file = testDir / "doi_registered_datacite" / "caa9e50a-a6a9-4cc1-a415-33d8384d4df5" / "deposit.properties"
    val (depositId, _, lastModified) = fileProps(file)

    inSequence {
      repo.addDeposit _ expects where(isDeposit(Deposit(depositId, "bag".some, time, "user001"))) returning Deposit(depositId, "bag".some, time, "user001").asRight
      repo.setState _ expects(depositId, InputState(StateLabel.SUBMITTED, "my description", lastModified)) returning State("my-id", StateLabel.SUBMITTED, "my description", lastModified).asRight
      repo.setIngestStep _ expects(depositId, InputIngestStep(IngestStepLabel.BAGSTORE, lastModified)) returning IngestStep("my-id", IngestStepLabel.BAGSTORE, lastModified).asRight
      repo.addIdentifier _ expects(depositId, InputIdentifier(IdentifierType.DOI, "my-doi-value", lastModified)) returning Identifier("my-id", IdentifierType.DOI, "my-doi-value", lastModified).asRight
      repo.addIdentifier _ expects(depositId, InputIdentifier(IdentifierType.URN, "my-urn-value", lastModified)) returning Identifier("my-id", IdentifierType.URN, "my-urn-value", lastModified).asRight
      repo.addIdentifier _ expects(depositId, InputIdentifier(IdentifierType.FEDORA, "my-fedora-value", lastModified)) returning Identifier("my-id", IdentifierType.FEDORA, "my-fedora-value", lastModified).asRight
      repo.addIdentifier _ expects(depositId, InputIdentifier(IdentifierType.BAG_STORE, "my-bag-store-value", lastModified)) returning Identifier("my-id", IdentifierType.BAG_STORE, "my-bag-store-value", lastModified).asRight
      datacite.doiExists _ expects "my-doi-value" returning true
      repo.setDoiRegistered _ expects(depositId, DoiRegisteredEvent(value = true, lastModified)) returning DoiRegisteredEvent(value = true, lastModified).asRight
      repo.setDoiAction _ expects(depositId, DoiActionEvent(DoiAction.UPDATE, lastModified)) returning DoiActionEvent(DoiAction.UPDATE, lastModified).asRight
      repo.setCurator _ expects(depositId, InputCurator("archie001", "does.not.exists@dans.knaw.nl", lastModified)) returning Curator("my-id", "archie001", "does.not.exists@dans.knaw.nl", lastModified).asRight
      repo.setIsNewVersionAction _ expects(depositId, IsNewVersionEvent(isNewVersion = true, lastModified)) returning IsNewVersionEvent(isNewVersion = true, lastModified).asRight
      repo.setCurationRequiredAction _ expects(depositId, CurationRequiredEvent(curationRequired = false, lastModified)) returning CurationRequiredEvent(curationRequired = false, lastModified).asRight
      repo.setCurationPerformedAction _ expects(depositId, CurationPerformedEvent(curationPerformed = false, lastModified)) returning CurationPerformedEvent(curationPerformed = false, lastModified).asRight
      repo.setSpringfield _ expects(depositId, InputSpringfield("domain", "user", "collection", SpringfieldPlayMode.CONTINUOUS, lastModified)) returning Springfield("my-id", "domain", "user", "collection", SpringfieldPlayMode.CONTINUOUS, lastModified).asRight
      repo.setContentType _ expects(depositId, InputContentType(ContentTypeValue.ZIP, lastModified)) returning ContentType("my-id", ContentTypeValue.ZIP, lastModified).asRight
    }

    importProps.loadDepositProperties(file) shouldBe right
  }

  it should "interact with the user when interaction with DataCite fails" in {
    val time = new DateTime(2019, 1, 1, 0, 0, timeZone)
    val file = testDir / "doi_registered_interact" / "7eb43c8d-0565-427d-9e6c-1c9c05d8a3f7" / "deposit.properties"
    val (depositId, _, lastModified) = fileProps(file)

    inSequence {
      repo.addDeposit _ expects where(isDeposit(Deposit(depositId, "bag".some, time, "user001"))) returning Deposit(depositId, "bag".some, time, "user001").asRight
      repo.setState _ expects(depositId, InputState(StateLabel.SUBMITTED, "my description", lastModified)) returning State("my-id", StateLabel.SUBMITTED, "my description", lastModified).asRight
      repo.setIngestStep _ expects(depositId, InputIngestStep(IngestStepLabel.BAGSTORE, lastModified)) returning IngestStep("my-id", IngestStepLabel.BAGSTORE, lastModified).asRight
      repo.addIdentifier _ expects(depositId, InputIdentifier(IdentifierType.DOI, "my-doi-value", lastModified)) returning Identifier("my-id", IdentifierType.DOI, "my-doi-value", lastModified).asRight
      repo.addIdentifier _ expects(depositId, InputIdentifier(IdentifierType.URN, "my-urn-value", lastModified)) returning Identifier("my-id", IdentifierType.URN, "my-urn-value", lastModified).asRight
      repo.addIdentifier _ expects(depositId, InputIdentifier(IdentifierType.FEDORA, "my-fedora-value", lastModified)) returning Identifier("my-id", IdentifierType.FEDORA, "my-fedora-value", lastModified).asRight
      repo.addIdentifier _ expects(depositId, InputIdentifier(IdentifierType.BAG_STORE, "my-bag-store-value", lastModified)) returning Identifier("my-id", IdentifierType.BAG_STORE, "my-bag-store-value", lastModified).asRight
      datacite.doiExists _ expects "my-doi-value" throws new DataciteServiceException("FAIL!!!", 418)
      (interactor.ask(_: String => Boolean)(_: String)) expects(*, *) returning true
      repo.setDoiRegistered _ expects(depositId, DoiRegisteredEvent(value = true, lastModified)) returning DoiRegisteredEvent(value = true, lastModified).asRight
      repo.setDoiAction _ expects(depositId, DoiActionEvent(DoiAction.UPDATE, lastModified)) returning DoiActionEvent(DoiAction.UPDATE, lastModified).asRight
      repo.setCurator _ expects(depositId, InputCurator("archie001", "does.not.exists@dans.knaw.nl", lastModified)) returning Curator("my-id", "archie001", "does.not.exists@dans.knaw.nl", lastModified).asRight
      repo.setIsNewVersionAction _ expects(depositId, IsNewVersionEvent(isNewVersion = true, lastModified)) returning IsNewVersionEvent(isNewVersion = true, lastModified).asRight
      repo.setCurationRequiredAction _ expects(depositId, CurationRequiredEvent(curationRequired = false, lastModified)) returning CurationRequiredEvent(curationRequired = false, lastModified).asRight
      repo.setCurationPerformedAction _ expects(depositId, CurationPerformedEvent(curationPerformed = false, lastModified)) returning CurationPerformedEvent(curationPerformed = false, lastModified).asRight
      repo.setSpringfield _ expects(depositId, InputSpringfield("domain", "user", "collection", SpringfieldPlayMode.CONTINUOUS, lastModified)) returning Springfield("my-id", "domain", "user", "collection", SpringfieldPlayMode.CONTINUOUS, lastModified).asRight
      repo.setContentType _ expects(depositId, InputContentType(ContentTypeValue.ZIP, lastModified)) returning ContentType("my-id", ContentTypeValue.ZIP, lastModified).asRight
    }

    importProps.loadDepositProperties(file) shouldBe right
  }

  it should "interact with the user when an invalid value is given for springfield.playmode" in {
    val time = new DateTime(2019, 1, 1, 0, 0, timeZone)
    val file = testDir / "invalid_springfield_playmode" / "26d3763e-566c-4860-9abe-ca161d40cd1f" / "deposit.properties"
    val (depositId, _, lastModified) = fileProps(file)

    inSequence {
      repo.addDeposit _ expects where(isDeposit(Deposit(depositId, "bag".some, time, "user001"))) returning Deposit(depositId, "bag".some, time, "user001").asRight
      repo.setState _ expects(depositId, InputState(StateLabel.SUBMITTED, "my description", lastModified)) returning State("my-id", StateLabel.SUBMITTED, "my description", lastModified).asRight
      repo.setIngestStep _ expects(depositId, InputIngestStep(IngestStepLabel.BAGSTORE, lastModified)) returning IngestStep("my-id", IngestStepLabel.BAGSTORE, lastModified).asRight
      repo.addIdentifier _ expects(depositId, InputIdentifier(IdentifierType.DOI, "my-doi-value", lastModified)) returning Identifier("my-id", IdentifierType.DOI, "my-doi-value", lastModified).asRight
      repo.addIdentifier _ expects(depositId, InputIdentifier(IdentifierType.URN, "my-urn-value", lastModified)) returning Identifier("my-id", IdentifierType.URN, "my-urn-value", lastModified).asRight
      repo.addIdentifier _ expects(depositId, InputIdentifier(IdentifierType.FEDORA, "my-fedora-value", lastModified)) returning Identifier("my-id", IdentifierType.FEDORA, "my-fedora-value", lastModified).asRight
      repo.addIdentifier _ expects(depositId, InputIdentifier(IdentifierType.BAG_STORE, "my-bag-store-value", lastModified)) returning Identifier("my-id", IdentifierType.BAG_STORE, "my-bag-store-value", lastModified).asRight
      repo.setDoiRegistered _ expects(depositId, DoiRegisteredEvent(value = true, lastModified)) returning DoiRegisteredEvent(value = true, lastModified).asRight
      repo.setDoiAction _ expects(depositId, DoiActionEvent(DoiAction.UPDATE, lastModified)) returning DoiActionEvent(DoiAction.UPDATE, lastModified).asRight
      repo.setCurator _ expects(depositId, InputCurator("archie001", "does.not.exists@dans.knaw.nl", lastModified)) returning Curator("my-id", "archie001", "does.not.exists@dans.knaw.nl", lastModified).asRight
      repo.setIsNewVersionAction _ expects(depositId, IsNewVersionEvent(isNewVersion = true, lastModified)) returning IsNewVersionEvent(isNewVersion = true, lastModified).asRight
      repo.setCurationRequiredAction _ expects(depositId, CurationRequiredEvent(curationRequired = false, lastModified)) returning CurationRequiredEvent(curationRequired = false, lastModified).asRight
      repo.setCurationPerformedAction _ expects(depositId, CurationPerformedEvent(curationPerformed = false, lastModified)) returning CurationPerformedEvent(curationPerformed = false, lastModified).asRight
      (interactor.ask(_: Enumeration)(_: String)) expects(SpringfieldPlayMode, *) returning SpringfieldPlayMode.MENU.asRight
      repo.setSpringfield _ expects(depositId, InputSpringfield("domain", "user", "collection", SpringfieldPlayMode.MENU, lastModified)) returning Springfield("my-id", "domain", "user", "collection", SpringfieldPlayMode.MENU, lastModified).asRight
      repo.setContentType _ expects(depositId, InputContentType(ContentTypeValue.ZIP, lastModified)) returning ContentType("my-id", ContentTypeValue.ZIP, lastModified).asRight
    }

    importProps.loadDepositProperties(file) shouldBe right
  }

  it should "interact with the user when an invalid value is given for easy-sword2.client-message.content-type" in {
    val time = new DateTime(2019, 1, 1, 0, 0, timeZone)
    val file = testDir / "invalid_content_type" / "d317ff0d-842f-49f4-8a18-cb396ce85a27" / "deposit.properties"
    val (depositId, _, lastModified) = fileProps(file)

    inSequence {
      repo.addDeposit _ expects where(isDeposit(Deposit(depositId, "bag".some, time, "user001"))) returning Deposit(depositId, "bag".some, time, "user001").asRight
      repo.setState _ expects(depositId, InputState(StateLabel.SUBMITTED, "my description", lastModified)) returning State("my-id", StateLabel.SUBMITTED, "my description", lastModified).asRight
      repo.setIngestStep _ expects(depositId, InputIngestStep(IngestStepLabel.BAGSTORE, lastModified)) returning IngestStep("my-id", IngestStepLabel.BAGSTORE, lastModified).asRight
      repo.addIdentifier _ expects(depositId, InputIdentifier(IdentifierType.DOI, "my-doi-value", lastModified)) returning Identifier("my-id", IdentifierType.DOI, "my-doi-value", lastModified).asRight
      repo.addIdentifier _ expects(depositId, InputIdentifier(IdentifierType.URN, "my-urn-value", lastModified)) returning Identifier("my-id", IdentifierType.URN, "my-urn-value", lastModified).asRight
      repo.addIdentifier _ expects(depositId, InputIdentifier(IdentifierType.FEDORA, "my-fedora-value", lastModified)) returning Identifier("my-id", IdentifierType.FEDORA, "my-fedora-value", lastModified).asRight
      repo.addIdentifier _ expects(depositId, InputIdentifier(IdentifierType.BAG_STORE, "my-bag-store-value", lastModified)) returning Identifier("my-id", IdentifierType.BAG_STORE, "my-bag-store-value", lastModified).asRight
      repo.setDoiRegistered _ expects(depositId, DoiRegisteredEvent(value = true, lastModified)) returning DoiRegisteredEvent(value = true, lastModified).asRight
      repo.setDoiAction _ expects(depositId, DoiActionEvent(DoiAction.UPDATE, lastModified)) returning DoiActionEvent(DoiAction.UPDATE, lastModified).asRight
      repo.setCurator _ expects(depositId, InputCurator("archie001", "does.not.exists@dans.knaw.nl", lastModified)) returning Curator("my-id", "archie001", "does.not.exists@dans.knaw.nl", lastModified).asRight
      repo.setIsNewVersionAction _ expects(depositId, IsNewVersionEvent(isNewVersion = true, lastModified)) returning IsNewVersionEvent(isNewVersion = true, lastModified).asRight
      repo.setCurationRequiredAction _ expects(depositId, CurationRequiredEvent(curationRequired = false, lastModified)) returning CurationRequiredEvent(curationRequired = false, lastModified).asRight
      repo.setCurationPerformedAction _ expects(depositId, CurationPerformedEvent(curationPerformed = false, lastModified)) returning CurationPerformedEvent(curationPerformed = false, lastModified).asRight
      repo.setSpringfield _ expects(depositId, InputSpringfield("domain", "user", "collection", SpringfieldPlayMode.CONTINUOUS, lastModified)) returning Springfield("my-id", "domain", "user", "collection", SpringfieldPlayMode.CONTINUOUS, lastModified).asRight
      (interactor.ask(_: Enumeration)(_: String)) expects(ContentTypeValue, *) returning ContentTypeValue.ZIP.asRight
      repo.setContentType _ expects(depositId, InputContentType(ContentTypeValue.ZIP, lastModified)) returning ContentType("my-id", ContentTypeValue.ZIP, lastModified).asRight
    }

    importProps.loadDepositProperties(file) shouldBe right
  }

  it should "fail when the properties file doesn't exist" in {
    val file = (testDir / "no_properties_file" / UUID.randomUUID().toString).createDirectoryIfNotExists(createParents = true) / "deposit.properties"
    file shouldNot exist

    importProps.loadDepositProperties(file).leftValue shouldBe NoSuchPropertiesFileError(file)
  }

  it should "fail when the parent directory is not a valid depositId" in {
    val file = testDir / "invalid_depositId" / "invalid-depositId" / "deposit.properties"

    importProps.loadDepositProperties(file).leftValue shouldBe NoDepositIdError("invalid-depositId")
  }

  private def isDeposit(deposit: Deposit): Deposit => Boolean = d => {
    d.id == deposit.id &&
      d.depositorId == deposit.depositorId &&
      d.bagName == deposit.bagName &&
      d.creationTimestamp.getMillis == deposit.creationTimestamp.getMillis
  }
}