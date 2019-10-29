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

import java.sql.{ Connection, ResultSet }

import cats.syntax.either._
import nl.knaw.dans.easy.properties.app.database.SQLErrorHandler
import nl.knaw.dans.easy.properties.app.model.{ DepositId, DoiRegisteredEvent }
import nl.knaw.dans.easy.properties.app.repository.{ DepositIdAndTimestampAlreadyExistError, DoiRegisteredDao, InvalidValueError, MutationError, MutationErrorOr, NoSuchDepositError, QueryErrorOr }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import resource.managed

class SQLDoiRegisteredDao(override implicit val connection: Connection, errorHandler: SQLErrorHandler) extends DoiRegisteredDao with SQLDeletableProperty with CommonResultSetParsers with DebugEnhancedLogging {

  override private[sql] val daoName = "Registered"
  override private[sql] val key = "doi-registered"

  private def parseDoiRegisteredEvent(resultSet: ResultSet): Either[InvalidValueError, DoiRegisteredEvent] = {
    for {
      timestamp <- parseDateTime(resultSet.getTimestamp("timestamp", timeZone), timeZone)
      value = resultSet.getBoolean("value")
    } yield DoiRegisteredEvent(value, timestamp)
  }

  private def parseDepositIdAndDoiRegisteredEvent(resultSet: ResultSet): Either[InvalidValueError, (DepositId, DoiRegisteredEvent)] = {
    for {
      depositId <- parseDepositId(resultSet.getString("depositId"))
      doiRegisteredEvent <- parseDoiRegisteredEvent(resultSet)
    } yield depositId -> doiRegisteredEvent
  }

  override def getCurrent(ids: Seq[DepositId]): QueryErrorOr[Seq[(DepositId, DoiRegisteredEvent)]] = {
    trace(ids)

    executeGetCurrent(parseDepositIdAndDoiRegisteredEvent)(QueryGenerator.getSimplePropsCurrentElementByDepositId(key))(ids)
  }

  override def getAll(ids: Seq[DepositId]): QueryErrorOr[Seq[(DepositId, Seq[DoiRegisteredEvent])]] = {
    trace(ids)

    executeGetAll(parseDepositIdAndDoiRegisteredEvent)(QueryGenerator.getSimplePropsAllElementsByDepositId(key))(ids)
  }

  override def store(id: DepositId, registered: DoiRegisteredEvent): MutationErrorOr[DoiRegisteredEvent] = {
    trace(id, registered)

    val query = QueryGenerator.storeSimpleProperty

    managed(connection.prepareStatement(query))
      .map(_.executeUpdateWith(id, key, registered.value.toString, registered.timestamp))
      .either
      .either
      .leftMap(ts => {
        assert(ts.nonEmpty)
        ts.collectFirst {
          case t if errorHandler.isForeignKeyError(t) => NoSuchDepositError(id)
          case t if errorHandler.isUniquenessConstraintError(t) => DepositIdAndTimestampAlreadyExistError(id, registered.timestamp, "doi registered event")
        }.getOrElse(MutationError(ts.head.getMessage))
      })
      .map(_ => registered)
  }
}
