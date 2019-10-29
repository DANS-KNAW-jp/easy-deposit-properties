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
import nl.knaw.dans.easy.properties.app.model.contentType.{ ContentType, ContentTypeValue, InputContentType }
import nl.knaw.dans.easy.properties.app.model.{ Deposit, DepositId }
import nl.knaw.dans.easy.properties.app.repository.{ ContentTypeDao, DepositIdAndTimestampAlreadyExistError, InvalidValueError, MutationError, MutationErrorOr, NoSuchDepositError, QueryErrorOr }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import resource.managed

class SQLContentTypeDao(override implicit val connection: Connection, errorHandler: SQLErrorHandler) extends ContentTypeDao with SQLDeletableProperty with CommonResultSetParsers with DebugEnhancedLogging {

  override private[sql] val daoName = "ContentType"
  override private[sql] val key = "content-type"

  private def parseContentType(resultSet: ResultSet): Either[InvalidValueError, ContentType] = {
    for {
      value <- parseEnumValue(ContentTypeValue, "content type")(resultSet.getString("value"))
      id = resultSet.getString("propertyId")
      timestamp <- parseDateTime(resultSet.getTimestamp("timestamp", timeZone), timeZone)
    } yield ContentType(id, value, timestamp)
  }

  private def parseDepositIdAndContentType(resultSet: ResultSet): Either[InvalidValueError, (DepositId, ContentType)] = {
    for {
      depositId <- parseDepositId(resultSet.getString("depositId"))
      contentType <- parseContentType(resultSet)
    } yield depositId -> contentType
  }

  private def parseContentTypeIdAndDeposit(resultSet: ResultSet): Either[InvalidValueError, (String, Deposit)] = {
    for {
      deposit <- parseDeposit(resultSet)
      contentTypeId = resultSet.getString("propertyId")
    } yield contentTypeId -> deposit
  }

  override def getById(ids: Seq[String]): QueryErrorOr[Seq[ContentType]] = {
    trace(ids)

    executeGetById(parseContentType)(QueryGenerator.getSimplePropsElementsById(key))(ids)
  }

  override def getCurrent(ids: Seq[DepositId]): QueryErrorOr[Seq[(DepositId, ContentType)]] = {
    trace(ids)

    executeGetCurrent(parseDepositIdAndContentType)(QueryGenerator.getSimplePropsCurrentElementByDepositId(key))(ids)
  }

  override def getAll(ids: Seq[DepositId]): QueryErrorOr[Seq[(DepositId, Seq[ContentType])]] = {
    trace(ids)

    executeGetAll(parseDepositIdAndContentType)(QueryGenerator.getSimplePropsAllElementsByDepositId(key))(ids)
  }

  override def store(id: DepositId, contentType: InputContentType): MutationErrorOr[ContentType] = {
    trace(id, contentType)

    val query = QueryGenerator.storeSimpleProperty

    val managedResultSet = for {
      prepStatement <- managed(connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS))
      _ = prepStatement.executeUpdateWith(id, key, contentType.value, contentType.timestamp)
      resultSetForKey <- managed(prepStatement.getGeneratedKeys)
    } yield resultSetForKey

    managedResultSet
      .map {
        case resultSet if resultSet.next() => resultSet.getLong(1).toString.asRight
        case _ => throw new Exception(s"not able to insert content type (${ contentType.value }, ${ contentType.timestamp })")
      }
      .either
      .either
      .leftMap(ts => {
        assert(ts.nonEmpty)
        ts.collectFirst {
          case t if errorHandler.isForeignKeyError(t) => NoSuchDepositError(id)
          case t if errorHandler.isUniquenessConstraintError(t) => DepositIdAndTimestampAlreadyExistError(id, contentType.timestamp, "content type")
        }.getOrElse(MutationError(ts.head.getMessage))
      })
      .flatMap(identity)
      .map(contentType.toOutput)
  }

  override def getDepositsById(ids: Seq[String]): QueryErrorOr[Seq[(String, Deposit)]] = {
    trace(ids)

    executeGetDepositById(parseContentTypeIdAndDeposit)(QueryGenerator.getSimplePropsDepositsById(key))(ids)
  }
}
