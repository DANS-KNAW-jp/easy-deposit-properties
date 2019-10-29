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
package nl.knaw.dans.easy.properties.app.graphql.types

import nl.knaw.dans.easy.properties.app.graphql.DataContext
import nl.knaw.dans.easy.properties.app.graphql.middleware.Authentication.RequiresAuthentication
import nl.knaw.dans.easy.properties.app.graphql.resolvers.{ ContentTypeResolver, CurationResolver, DepositResolver, IdentifierResolver, IngestStepResolver, SpringfieldResolver, StateResolver }
import nl.knaw.dans.easy.properties.app.model.Origin.Origin
import nl.knaw.dans.easy.properties.app.model.contentType.{ ContentTypeValue, InputContentType }
import nl.knaw.dans.easy.properties.app.model.curation.InputCuration
import nl.knaw.dans.easy.properties.app.model.identifier.{ IdentifierType, InputIdentifier }
import nl.knaw.dans.easy.properties.app.model.ingestStep.{ IngestStepLabel, InputIngestStep }
import nl.knaw.dans.easy.properties.app.model.springfield.{ InputSpringfield, SpringfieldPlayMode }
import nl.knaw.dans.easy.properties.app.model.state.{ InputState, StateLabel }
import nl.knaw.dans.easy.properties.app.model.{ Deposit, DepositId, DepositorId, DoiAction, DoiActionEvent, DoiRegisteredEvent, Origin, Timestamp }
import sangria.relay.Mutation
import sangria.schema.{ Action, BooleanType, Context, Field, InputField, InputObjectType, ListInputType, ListType, ObjectType, OptionInputType, OptionType, ScalarType, StringType, fields }

trait MutationType {
  this: DepositType
    with StateType
    with IngestStepType
    with IdentifierGraphQLType
    with DoiEventTypes
    with CurationType
    with SpringfieldType
    with ContentTypeGraphQLType
    with Scalars =>

  case class AddDepositPayload(clientMutationId: Option[String], depositId: DepositId) extends Mutation
  case class AddBagNamePayload(clientMutationId: Option[String], depositId: DepositId) extends Mutation
  case class UpdateStatePayload(clientMutationId: Option[String], objectId: String) extends Mutation
  case class UpdateIngestStepPayload(clientMutationId: Option[String], objectId: String) extends Mutation
  case class AddIdentifierPayload(clientMutationId: Option[String], objectId: String) extends Mutation
  case class SetDoiRegisteredPayload(clientMutationId: Option[String], obj: DoiRegisteredEvent) extends Mutation
  case class SetDoiActionPayload(clientMutationId: Option[String], obj: DoiActionEvent) extends Mutation
  case class SetCurationPayload(clientMutationId: Option[String], objectId: String) extends Mutation
  case class SetSpringfieldPayload(clientMutationId: Option[String], objectId: String) extends Mutation
  case class SetContentTypePayload(clientMutationId: Option[String], objectId: String) extends Mutation
  case class RegisterDepositPayload(clientMutationId: Option[String], depositId: DepositId) extends Mutation
  case class DeleteDepositsPayload(clientMutationId: Option[String], depositIds: Seq[DepositId]) extends Mutation

  private val depositIdsListInputField: InputField[Seq[DepositId]] = InputField(
    name = "depositIds",
    description = Some("A list of deposit identifiers."),
    defaultValue = None,
    fieldType = ListInputType(UUIDType),
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  private val depositIdInputField: InputField[DepositId] = InputField(
    name = "depositId",
    description = Some("The deposit's identifier."),
    defaultValue = None,
    fieldType = UUIDType,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  private val bagNameOptionInputField: InputField[Option[String]] = InputField(
    name = "bagName",
    description = Some("The name of the deposited bag."),
    defaultValue = None,
    fieldType = OptionInputType(StringType),
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  private val originInputField: InputField[Origin.Value] = InputField(
    name = "origin",
    description = Some("The origin of the deposited bag."),
    defaultValue = None,
    fieldType = OriginType,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  private val bagNameInputField: InputField[String] = InputField(
    name = "bagName",
    description = Some("The name of the deposited bag."),
    defaultValue = None,
    fieldType = StringType,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  private val creationTimestampInputField: InputField[Timestamp] = InputField(
    name = "creationTimestamp",
    description = Some("The timestamp at which this deposit was created."),
    defaultValue = None,
    fieldType = DateTimeType,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  private val depositorIdInputField: InputField[DepositorId] = InputField(
    name = "depositorId",
    description = Some("The depositor that submits this deposit."),
    defaultValue = None,
    fieldType = StringType,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  private val stateLabelInputField: InputField[StateLabel.Value] = InputField(
    name = "label",
    description = Some("The state label of the deposit."),
    defaultValue = None,
    fieldType = StateLabelType,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  private val stateDescriptionInputField: InputField[String] = InputField(
    name = "description",
    description = Some("Additional information about the state."),
    defaultValue = None,
    fieldType = StringType,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  private val stateTimestampInputField: InputField[Timestamp] = InputField(
    name = "timestamp",
    description = Some("The timestamp at which the deposit got into this state."),
    defaultValue = None,
    fieldType = DateTimeType,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  private val ingestStepLabelInputField: InputField[IngestStepLabel.Value] = InputField(
    name = "step",
    description = Some("The label of the ingest step."),
    defaultValue = None,
    fieldType = StepLabelType,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  private val ingestStepTimestampInputField: InputField[Timestamp] = InputField(
    name = "timestamp",
    description = Some("The timestamp at which the deposit got into this ingest step."),
    defaultValue = None,
    fieldType = DateTimeType,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  private val identifierTypeInputField: InputField[IdentifierType.Value] = InputField(
    name = "type",
    description = Some("The type of identifier."),
    defaultValue = None,
    fieldType = IdentifierTypeType,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  private val identifierValueInputField: InputField[String] = InputField(
    name = "value",
    description = Some("The value of the identifier."),
    defaultValue = None,
    fieldType = StringType,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  private val identifierTimestampInputField: InputField[Timestamp] = InputField(
    name = "timestamp",
    description = Some("The timestamp at which the identifier got added to this deposit."),
    defaultValue = None,
    fieldType = DateTimeType,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  private val doiRegisteredValueInputField: InputField[Boolean] = InputField(
    name = "value",
    description = Some("Whether the DOI is registered in DataCite."),
    defaultValue = None,
    fieldType = BooleanType,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  private val doiRegisteredTimestampInputField: InputField[Timestamp] = InputField(
    name = "timestamp",
    description = Some("The timestamp at which the DOI was registered in DataCite."),
    defaultValue = None,
    fieldType = DateTimeType,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  private val doiActionValueInputField: InputField[DoiAction.Value] = InputField(
    name = "value",
    description = Some("Whether the DOI must be 'created' or 'updated' when registering in DataCite."),
    defaultValue = None,
    fieldType = DoiActionType,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  private val doiActionTimestampInputField: InputField[Timestamp] = InputField(
    name = "timestamp",
    description = Some("The timestamp at which this value was added."),
    defaultValue = None,
    fieldType = DateTimeType,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  private val curationUserIdInputField: InputField[String] = InputField(
    name = "datamanagerUserId",
    description = Some("The data manager's username in EASY."),
    defaultValue = None,
    fieldType = StringType,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  private val curationEmailInputField: InputField[String] = InputField(
    name = "datamanagerEmail",
    description = Some("The data manager's email address."),
    defaultValue = None,
    fieldType = StringType,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  private val isNewVersionValueInputField: InputField[Option[Boolean]] = InputField(
    name = "isNewVersion",
    description = Some("True if the deposit is a new version."),
    defaultValue = None,
    fieldType = OptionInputType(BooleanType),
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  private val curationRequiredValueInputField: InputField[Boolean] = InputField(
    name = "isCurationRequired",
    description = Some("True if curation by a data manager is required."),
    defaultValue = None,
    fieldType = BooleanType,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  private val curationPerformedValueInputField: InputField[Boolean] = InputField(
    name = "isCurationPerformed",
    description = Some("True if curation by the data manager has been performed."),
    defaultValue = None,
    fieldType = BooleanType,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  private val curationTimestampInputField: InputField[Timestamp] = InputField(
    name = "timestamp",
    description = Some("The timestamp at which the curation event was assigned to this deposit."),
    defaultValue = None,
    fieldType = DateTimeType,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  private val springfieldDomainInputField: InputField[String] = InputField(
    name = "domain",
    description = Some("The domain of Springfield."),
    defaultValue = None,
    fieldType = StringType,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  private val springfieldUserInputField: InputField[String] = InputField(
    name = "user",
    description = Some("The user of Springfield."),
    defaultValue = None,
    fieldType = StringType,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  private val springfieldCollectionInputField: InputField[String] = InputField(
    name = "collection",
    description = Some("The collection of Springfield."),
    defaultValue = None,
    fieldType = StringType,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  private val springfieldPlayModeInputField: InputField[SpringfieldPlayMode.Value] = InputField(
    name = "playmode",
    description = Some("The playmode used in Springfield."),
    defaultValue = None,
    fieldType = SpringfieldPlayModeType,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  private val springfieldTimestampInputField: InputField[Timestamp] = InputField(
    name = "timestamp",
    description = Some("The timestamp at which this springfield configuration was associated with the deposit."),
    defaultValue = None,
    fieldType = DateTimeType,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  private val contentTypeValueInputField: InputField[ContentTypeValue.Value] = InputField(
    name = "value",
    description = Some("The content type associated with this deposit."),
    defaultValue = None,
    fieldType = ContentTypeValueType,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  private val contentTypeTimestampInputField: InputField[Timestamp] = InputField(
    name = "timestamp",
    description = Some("The timestamp at which this springfield configuration was associated with the deposit."),
    defaultValue = None,
    fieldType = DateTimeType,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  private val depositPropertiesInputField: InputField[String] = InputField(
    name = "depositProperties",
    description = Some("The 'deposit.properties' describing the deposit to be registered."),
    defaultValue = None,
    fieldType = StringType,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )

  private val depositField: Field[DataContext, AddDepositPayload] = Field(
    name = "deposit",
    fieldType = OptionType(DepositType),
    resolve = ctx => DepositResolver.depositById(ctx.value.depositId)(ctx.ctx),
  )
  private val depositForAddBagNameField: Field[DataContext, AddBagNamePayload] = Field(
    name = "deposit",
    fieldType = OptionType(DepositType),
    resolve = ctx => DepositResolver.depositById(ctx.value.depositId)(ctx.ctx),
  )
  private val stateField: Field[DataContext, UpdateStatePayload] = Field(
    name = "state",
    fieldType = OptionType(StateType),
    resolve = ctx => StateResolver.stateById(ctx.value.objectId)(ctx.ctx),
  )
  private val ingestStepField: Field[DataContext, UpdateIngestStepPayload] = Field(
    name = "ingestStep",
    fieldType = OptionType(IngestStepType),
    resolve = ctx => IngestStepResolver.ingestStepById(ctx.value.objectId)(ctx.ctx),
  )
  private val identifierField: Field[DataContext, AddIdentifierPayload] = Field(
    name = "identifier",
    fieldType = OptionType(IdentifierObjectType),
    resolve = ctx => IdentifierResolver.identifierById(ctx.value.objectId)(ctx.ctx),
  )
  private val doiRegisteredField: Field[DataContext, SetDoiRegisteredPayload] = Field(
    name = "doiRegistered",
    fieldType = OptionType(DoiRegisteredEventType),
    resolve = ctx => ctx.value.obj,
  )
  private val doiActionField: Field[DataContext, SetDoiActionPayload] = Field(
    name = "doiAction",
    fieldType = OptionType(DoiActionEventType),
    resolve = ctx => ctx.value.obj,
  )
  private val curationField: Field[DataContext, SetCurationPayload] = Field(
    name = "curation",
    fieldType = OptionType(CurationType),
    resolve = ctx => CurationResolver.curationById(ctx.value.objectId)(ctx.ctx)
  )
  private val springfieldField: Field[DataContext, SetSpringfieldPayload] = Field(
    name = "springfield",
    fieldType = OptionType(SpringfieldType),
    resolve = ctx => SpringfieldResolver.springfieldById(ctx.value.objectId)(ctx.ctx),
  )
  private val contentTypeField: Field[DataContext, SetContentTypePayload] = Field(
    name = "contentType",
    fieldType = OptionType(ContentTypeType),
    resolve = ctx => ContentTypeResolver.contentTypeById(ctx.value.objectId)(ctx.ctx),
  )
  private val depositFieldForProperties: Field[DataContext, RegisterDepositPayload] = Field(
    name = "deposit",
    fieldType = OptionType(DepositType),
    resolve = ctx => DepositResolver.depositById(ctx.value.depositId)(ctx.ctx),
  )

  private val depositIdsFieldForDelete: Field[DataContext, DeleteDepositsPayload] = Field(
    name = "depositIds",
    fieldType = ListType[DepositId](UUIDType),
    resolve = _.value.depositIds,
  )

  private val addDepositField: Field[DataContext, Unit] = Mutation.fieldWithClientMutationId[DataContext, Unit, AddDepositPayload, InputObjectType.DefaultInput](
    fieldName = "addDeposit",
    typeName = "AddDeposit",
    tags = List(
      RequiresAuthentication,
    ),
    fieldDescription = Some("Register a new deposit with 'id', 'creationTimestamp', 'depositId' and 'origin'."),
    inputFields = List(
      depositIdInputField,
      bagNameOptionInputField,
      creationTimestampInputField,
      depositorIdInputField,
      originInputField,
    ),
    outputFields = fields(
      depositField,
    ),
    mutateAndGetPayload = addDeposit,
  )
  private val addBagNameField: Field[DataContext, Unit] = Mutation.fieldWithClientMutationId[DataContext, Unit, AddBagNamePayload, InputObjectType.DefaultInput](
    fieldName = "addBagName",
    typeName = "AddBagName",
    tags = List(
      RequiresAuthentication,
    ),
    fieldDescription = Some("Register the bag's name for the deposit identified by 'id'."),
    inputFields = List(
      depositIdInputField,
      bagNameInputField,
    ),
    outputFields = fields(
      depositForAddBagNameField,
    ),
    mutateAndGetPayload = addBagName,
  )
  private val updateStateField: Field[DataContext, Unit] = Mutation.fieldWithClientMutationId[DataContext, Unit, UpdateStatePayload, InputObjectType.DefaultInput](
    fieldName = "updateState",
    typeName = "UpdateState",
    tags = List(
      RequiresAuthentication,
    ),
    fieldDescription = Some("Update the state of the deposit identified by 'id'."),
    inputFields = List(
      depositIdInputField,
      stateLabelInputField,
      stateDescriptionInputField,
      stateTimestampInputField,
    ),
    outputFields = fields(
      stateField,
    ),
    mutateAndGetPayload = updateState,
  )
  private val updateIngestStepField: Field[DataContext, Unit] = Mutation.fieldWithClientMutationId[DataContext, Unit, UpdateIngestStepPayload, InputObjectType.DefaultInput](
    fieldName = "updateIngestStep",
    typeName = "UpdateIngestStep",
    tags = List(
      RequiresAuthentication,
    ),
    fieldDescription = Some("Update the ingest step of the deposit identified by 'id'."),
    inputFields = List(
      depositIdInputField,
      ingestStepLabelInputField,
      ingestStepTimestampInputField,
    ),
    outputFields = fields(
      ingestStepField,
    ),
    mutateAndGetPayload = updateIngestStep,
  )
  private val addIdentifierField: Field[DataContext, Unit] = Mutation.fieldWithClientMutationId[DataContext, Unit, AddIdentifierPayload, InputObjectType.DefaultInput](
    fieldName = "addIdentifier",
    typeName = "AddIdentifier",
    tags = List(
      RequiresAuthentication,
    ),
    fieldDescription = Some("Add an identifier to the deposit identified by 'id'."),
    inputFields = List(
      depositIdInputField,
      identifierTypeInputField,
      identifierValueInputField,
      identifierTimestampInputField,
    ),
    outputFields = fields(
      identifierField,
    ),
    mutateAndGetPayload = addIdentifier,
  )
  private val setDoiRegisteredField: Field[DataContext, Unit] = Mutation.fieldWithClientMutationId[DataContext, Unit, SetDoiRegisteredPayload, InputObjectType.DefaultInput](
    fieldName = "setDoiRegistered",
    typeName = "SetDoiRegistered",
    tags = List(
      RequiresAuthentication,
    ),
    fieldDescription = Some("Set whether the DOI has been registered in DataCite."),
    inputFields = List(
      depositIdInputField,
      doiRegisteredValueInputField,
      doiRegisteredTimestampInputField,
    ),
    outputFields = fields(
      doiRegisteredField,
    ),
    mutateAndGetPayload = setDoiRegistered,
  )
  private val setDoiActionField: Field[DataContext, Unit] = Mutation.fieldWithClientMutationId[DataContext, Unit, SetDoiActionPayload, InputObjectType.DefaultInput](
    fieldName = "setDoiAction",
    typeName = "SetDoiAction",
    tags = List(
      RequiresAuthentication,
    ),
    fieldDescription = Some("Set whether the DOI should be 'created' or 'updated' on registration in DataCite."),
    inputFields = List(
      depositIdInputField,
      doiActionValueInputField,
      doiActionTimestampInputField,
    ),
    outputFields = fields(
      doiActionField,
    ),
    mutateAndGetPayload = setDoiAction,
  )
  private val setCurationField: Field[DataContext, Unit] = Mutation.fieldWithClientMutationId[DataContext, Unit, SetCurationPayload, InputObjectType.DefaultInput](
    fieldName = "setCuration",
    typeName = "SetCuration",
    tags = List(
      RequiresAuthentication,
    ),
    fieldDescription = Some("Assign a curation event to the deposit identified by 'id'."),
    inputFields = List(
      depositIdInputField,
      curationUserIdInputField,
      curationEmailInputField,
      isNewVersionValueInputField,
      curationRequiredValueInputField,
      curationPerformedValueInputField,
      curationTimestampInputField,
    ),
    outputFields = fields(
      curationField,
    ),
    mutateAndGetPayload = setCuration,
  )
  private val setSpringfieldField: Field[DataContext, Unit] = Mutation.fieldWithClientMutationId[DataContext, Unit, SetSpringfieldPayload, InputObjectType.DefaultInput](
    fieldName = "setSpringfield",
    typeName = "SetSpringfield",
    tags = List(
      RequiresAuthentication,
    ),
    fieldDescription = Some("Set the springfield configuration for this deposit."),
    inputFields = List(
      depositIdInputField,
      springfieldDomainInputField,
      springfieldUserInputField,
      springfieldCollectionInputField,
      springfieldPlayModeInputField,
      springfieldTimestampInputField,
    ),
    outputFields = fields(
      springfieldField,
    ),
    mutateAndGetPayload = setSpringfield,
  )
  private val setContentTypeField: Field[DataContext, Unit] = Mutation.fieldWithClientMutationId[DataContext, Unit, SetContentTypePayload, InputObjectType.DefaultInput](
    fieldName = "setContentType",
    typeName = "SetContentType",
    tags = List(
      RequiresAuthentication,
    ),
    fieldDescription = Some("Set the content type for this deposit."),
    inputFields = List(
      depositIdInputField,
      contentTypeValueInputField,
      contentTypeTimestampInputField,
    ),
    outputFields = fields(
      contentTypeField,
    ),
    mutateAndGetPayload = setContentType,
  )

  private val registerDepositField: Field[DataContext, Unit] = Mutation.fieldWithClientMutationId[DataContext, Unit, RegisterDepositPayload, InputObjectType.DefaultInput](
    fieldName = "registerDeposit",
    typeName = "RegisterDeposit",
    tags = List(
      RequiresAuthentication,
    ),
    fieldDescription = Some("Register a new deposit with initial properties from a 'deposit.properties' string."),
    inputFields = List(
      depositIdInputField,
      depositPropertiesInputField,
    ),
    outputFields = fields(
      depositFieldForProperties,
    ),
    mutateAndGetPayload = registerDeposit,
  )

  private val deleteDepositsField: Field[DataContext, Unit] = Mutation.fieldWithClientMutationId[DataContext, Unit, DeleteDepositsPayload, InputObjectType.DefaultInput](
    fieldName = "deleteDeposits",
    typeName = "DeleteDeposits",
    tags = List(
      RequiresAuthentication,
    ),
    fieldDescription = Some("Delete deposits."),
    inputFields = List(
      depositIdsListInputField,
    ),
    outputFields = fields(
      depositIdsFieldForDelete,
    ),
    mutateAndGetPayload = deleteDeposits,
  )

  private def addDeposit(input: InputObjectType.DefaultInput, context: Context[DataContext, Unit]): Action[DataContext, AddDepositPayload] = {
    context.ctx.repo.deposits
      .store(Deposit(
        id = input(depositIdInputField.name).asInstanceOf[DepositId],
        bagName = input.get(bagNameOptionInputField.name).flatMap(_.asInstanceOf[Option[String]]),
        creationTimestamp = input(creationTimestampInputField.name).asInstanceOf[Timestamp],
        depositorId = input(depositorIdInputField.name).asInstanceOf[String],
        origin = input(originInputField.name).asInstanceOf[Origin]
      ))
      .map(deposit => AddDepositPayload(
        clientMutationId = input.get(Mutation.ClientMutationIdFieldName).flatMap(_.asInstanceOf[Option[String]]),
        depositId = deposit.id,
      ))
      .toTry
  }

  private def addBagName(input: InputObjectType.DefaultInput, context: Context[DataContext, Unit]): Action[DataContext, AddBagNamePayload] = {
    context.ctx.repo.deposits
      .storeBagName(
        depositId = input(depositIdInputField.name).asInstanceOf[DepositId],
        bagName = input(bagNameInputField.name).asInstanceOf[String],
      )
      .map(depositId => AddBagNamePayload(
        clientMutationId = input.get(Mutation.ClientMutationIdFieldName).flatMap(_.asInstanceOf[Option[String]]),
        depositId = depositId,
      ))
      .toTry
  }

  private def updateState(input: InputObjectType.DefaultInput, context: Context[DataContext, Unit]): Action[DataContext, UpdateStatePayload] = {
    context.ctx.repo.states
      .store(
        id = input(depositIdInputField.name).asInstanceOf[DepositId],
        state = InputState(
          label = input(stateLabelInputField.name).asInstanceOf[StateLabel.Value],
          description = input(stateDescriptionInputField.name).asInstanceOf[String],
          timestamp = input(stateTimestampInputField.name).asInstanceOf[Timestamp],
        ),
      )
      .map(state => UpdateStatePayload(
        clientMutationId = input.get(Mutation.ClientMutationIdFieldName).flatMap(_.asInstanceOf[Option[String]]),
        objectId = state.id,
      ))
      .toTry
  }

  private def updateIngestStep(input: InputObjectType.DefaultInput, context: Context[DataContext, Unit]): Action[DataContext, UpdateIngestStepPayload] = {
    context.ctx.repo.ingestSteps
      .store(
        id = input(depositIdInputField.name).asInstanceOf[DepositId],
        step = InputIngestStep(
          step = input(ingestStepLabelInputField.name).asInstanceOf[IngestStepLabel.Value],
          timestamp = input(ingestStepTimestampInputField.name).asInstanceOf[Timestamp],
        ),
      )
      .map(ingestStep => UpdateIngestStepPayload(
        clientMutationId = input.get(Mutation.ClientMutationIdFieldName).flatMap(_.asInstanceOf[Option[String]]),
        objectId = ingestStep.id,
      ))
      .toTry
  }

  private def addIdentifier(input: InputObjectType.DefaultInput, context: Context[DataContext, Unit]): Action[DataContext, AddIdentifierPayload] = {
    context.ctx.repo.identifiers
      .store(
        id = input(depositIdInputField.name).asInstanceOf[DepositId],
        identifier = InputIdentifier(
          idType = input(identifierTypeInputField.name).asInstanceOf[IdentifierType.Value],
          idValue = input(identifierValueInputField.name).asInstanceOf[String],
          timestamp = input(identifierTimestampInputField.name).asInstanceOf[Timestamp],
        ),
      )
      .map(identifier => AddIdentifierPayload(
        clientMutationId = input.get(Mutation.ClientMutationIdFieldName).flatMap(_.asInstanceOf[Option[String]]),
        objectId = identifier.id,
      ))
      .toTry
  }

  private def setDoiRegistered(input: InputObjectType.DefaultInput, context: Context[DataContext, Unit]): Action[DataContext, SetDoiRegisteredPayload] = {
    context.ctx.repo.doiRegistered
      .store(
        id = input(depositIdInputField.name).asInstanceOf[DepositId],
        registered = DoiRegisteredEvent(
          value = input(doiRegisteredValueInputField.name).asInstanceOf[Boolean],
          timestamp = input(doiRegisteredTimestampInputField.name).asInstanceOf[Timestamp],
        ),
      )
      .map(doiRegisteredEvent => SetDoiRegisteredPayload(
        input.get(Mutation.ClientMutationIdFieldName).flatMap(_.asInstanceOf[Option[String]]),
        doiRegisteredEvent,
      ))
      .toTry
  }

  private def setDoiAction(input: InputObjectType.DefaultInput, context: Context[DataContext, Unit]): Action[DataContext, SetDoiActionPayload] = {
    context.ctx.repo.doiAction
      .store(
        id = input(depositIdInputField.name).asInstanceOf[DepositId],
        action = DoiActionEvent(
          value = input(doiActionValueInputField.name).asInstanceOf[DoiAction.Value],
          timestamp = input(doiActionTimestampInputField.name).asInstanceOf[Timestamp],
        ),
      )
      .map(doiActionEvent => SetDoiActionPayload(
        clientMutationId = input.get(Mutation.ClientMutationIdFieldName).flatMap(_.asInstanceOf[Option[String]]),
        obj = doiActionEvent,
      ))
      .toTry
  }

  private def setCuration(input: InputObjectType.DefaultInput, context: Context[DataContext, Unit]): Action[DataContext, SetCurationPayload] = {
    context.ctx.repo.curation
      .store(
        id = input(depositIdInputField.name).asInstanceOf[DepositId],
        curation = InputCuration(
          isNewVersion = input.get(isNewVersionValueInputField.name).flatMap(_.asInstanceOf[Option[Boolean]]),
          isRequired = input(curationRequiredValueInputField.name).asInstanceOf[Boolean],
          isPerformed = input(curationPerformedValueInputField.name).asInstanceOf[Boolean],
          datamanagerUserId = input(curationUserIdInputField.name).asInstanceOf[String],
          datamanagerEmail = input(curationEmailInputField.name).asInstanceOf[String],
          timestamp = input(curationTimestampInputField.name).asInstanceOf[Timestamp],
        ),
      )
      .map(curation => SetCurationPayload(
        clientMutationId = input.get(Mutation.ClientMutationIdFieldName).flatMap(_.asInstanceOf[Option[String]]),
        objectId = curation.id,
      ))
      .toTry
  }

  private def setSpringfield(input: InputObjectType.DefaultInput, context: Context[DataContext, Unit]): Action[DataContext, SetSpringfieldPayload] = {
    context.ctx.repo.springfield
      .store(
        id = input(depositIdInputField.name).asInstanceOf[DepositId],
        springfield = InputSpringfield(
          domain = input(springfieldDomainInputField.name).asInstanceOf[String],
          user = input(springfieldUserInputField.name).asInstanceOf[String],
          collection = input(springfieldCollectionInputField.name).asInstanceOf[String],
          playmode = input(springfieldPlayModeInputField.name).asInstanceOf[SpringfieldPlayMode.Value],
          timestamp = input(springfieldTimestampInputField.name).asInstanceOf[Timestamp],
        ),
      )
      .map(springfield => SetSpringfieldPayload(
        clientMutationId = input.get(Mutation.ClientMutationIdFieldName).flatMap(_.asInstanceOf[Option[String]]),
        objectId = springfield.id,
      ))
      .toTry
  }

  private def setContentType(input: InputObjectType.DefaultInput, context: Context[DataContext, Unit]): Action[DataContext, SetContentTypePayload] = {
    context.ctx.repo.contentType
      .store(
        id = input(depositIdInputField.name).asInstanceOf[DepositId],
        contentType = InputContentType(
          value = input(contentTypeValueInputField.name).asInstanceOf[ContentTypeValue.Value],
          timestamp = input(contentTypeTimestampInputField.name).asInstanceOf[Timestamp],
        ),
      )
      .map(contentType => SetContentTypePayload(
        input.get(Mutation.ClientMutationIdFieldName).flatMap(_.asInstanceOf[Option[String]]),
        contentType.id,
      ))
      .toTry
  }

  private def registerDeposit(input: InputObjectType.DefaultInput, context: Context[DataContext, Unit]): Action[DataContext, RegisterDepositPayload] = {
    context.ctx.registration
      .register(
        depositId = input(depositIdInputField.name).asInstanceOf[DepositId],
        props = input(depositPropertiesInputField.name).asInstanceOf[String],
      )
      .map(depositId => RegisterDepositPayload(
        input.get(Mutation.ClientMutationIdFieldName).flatMap(_.asInstanceOf[Option[String]]),
        depositId,
      ))
      .toTry
  }

  private def deleteDeposits(input: InputObjectType.DefaultInput, context: Context[DataContext, Unit]): Action[DataContext, DeleteDepositsPayload] = {
    context.ctx.deleter
      .deleteDepositsBy(
        ids = input(depositIdsListInputField.name).asInstanceOf[Seq[DepositId]],
      )
      .map(depositIds => DeleteDepositsPayload(
        input.get(Mutation.ClientMutationIdFieldName).flatMap(_.asInstanceOf[Option[String]]),
        depositIds,
      ))
      .toTry
  }

  implicit val MutationType: ObjectType[DataContext, Unit] = ObjectType(
    name = "Mutation",
    description = "The root query for implementing GraphQL mutations.",
    fields = fields[DataContext, Unit](
      addDepositField,
      addBagNameField,
      updateStateField,
      updateIngestStepField,
      addIdentifierField,
      setDoiRegisteredField,
      setDoiActionField,
      setCurationField,
      setSpringfieldField,
      setContentTypeField,
      registerDepositField,
      deleteDepositsField,
    ),
  )
}
