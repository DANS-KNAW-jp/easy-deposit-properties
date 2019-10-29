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

import java.sql.{ Connection, ResultSet, Statement }

import cats.syntax.either._
import nl.knaw.dans.easy.properties.app.database.SQLErrorHandler
import nl.knaw.dans.easy.properties.app.model.ingestStep.{ IngestStep, IngestStepLabel, InputIngestStep }
import nl.knaw.dans.easy.properties.app.model.{ Deposit, DepositId }
import nl.knaw.dans.easy.properties.app.repository.{ DepositIdAndTimestampAlreadyExistError, IngestStepDao, InvalidValueError, MutationError, MutationErrorOr, NoSuchDepositError, QueryErrorOr }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import resource.managed

class SQLIngestStepDao(override implicit val connection: Connection, errorHandler: SQLErrorHandler) extends IngestStepDao with SQLDeletableProperty with CommonResultSetParsers with DebugEnhancedLogging {

  override private[sql] val daoName = "IngestStep"
  override private[sql] val key = "ingest-step"

  private def parseIngestStep(resultSet: ResultSet): Either[InvalidValueError, IngestStep] = {
    for {
      value <- parseEnumValue(IngestStepLabel, "ingest step label")(resultSet.getString("value"))
      id = resultSet.getString("propertyId")
      timestamp <- parseDateTime(resultSet.getTimestamp("timestamp", timeZone), timeZone)
    } yield IngestStep(id, value, timestamp)
  }

  private def parseDepositIdAndIngestStep(resultSet: ResultSet): Either[InvalidValueError, (DepositId, IngestStep)] = {
    for {
      depositId <- parseDepositId(resultSet.getString("depositId"))
      ingestStep <- parseIngestStep(resultSet)
    } yield depositId -> ingestStep
  }

  private def parseIngestStepIdAndDeposit(resultSet: ResultSet): Either[InvalidValueError, (String, Deposit)] = {
    for {
      deposit <- parseDeposit(resultSet)
      ingestStepId = resultSet.getString("propertyId")
    } yield ingestStepId -> deposit
  }

  override def getById(ids: Seq[String]): QueryErrorOr[Seq[IngestStep]] = {
    trace(ids)

    executeGetById(parseIngestStep)(QueryGenerator.getSimplePropsElementsById(key))(ids)
  }

  override def getCurrent(ids: Seq[DepositId]): QueryErrorOr[Seq[(DepositId, IngestStep)]] = {
    trace(ids)

    executeGetCurrent(parseDepositIdAndIngestStep)(QueryGenerator.getSimplePropsCurrentElementByDepositId(key))(ids)
  }

  override def getAll(ids: Seq[DepositId]): QueryErrorOr[Seq[(DepositId, Seq[IngestStep])]] = {
    trace(ids)

    executeGetAll(parseDepositIdAndIngestStep)(QueryGenerator.getSimplePropsAllElementsByDepositId(key))(ids)
  }

  override def store(id: DepositId, step: InputIngestStep): MutationErrorOr[IngestStep] = {
    trace(id, step)

    val query = QueryGenerator.storeSimpleProperty

    val managedResultSet = for {
      prepStatement <- managed(connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS))
      _ = prepStatement.executeUpdateWith(id, key, step.step, step.timestamp)
      resultSetForKey <- managed(prepStatement.getGeneratedKeys)
    } yield resultSetForKey

    managedResultSet
      .map {
        case resultSet if resultSet.next() => resultSet.getLong(1).toString.asRight
        case _ => throw new Exception(s"not able to insert ingest step (${ step.step }, ${ step.timestamp })")
      }
      .either
      .either
      .leftMap(ts => {
        assert(ts.nonEmpty)
        ts.collectFirst {
          case t if errorHandler.isForeignKeyError(t) => NoSuchDepositError(id)
          case t if errorHandler.isUniquenessConstraintError(t) => DepositIdAndTimestampAlreadyExistError(id, step.timestamp, "ingest step")
        }.getOrElse(MutationError(ts.head.getMessage))
      })
      .flatMap(identity)
      .map(step.toOutput)
  }

  override def getDepositsById(ids: Seq[String]): QueryErrorOr[Seq[(String, Deposit)]] = {
    trace(ids)

    executeGetDepositById(parseIngestStepIdAndDeposit)(QueryGenerator.getSimplePropsDepositsById(key))(ids)
  }
}
