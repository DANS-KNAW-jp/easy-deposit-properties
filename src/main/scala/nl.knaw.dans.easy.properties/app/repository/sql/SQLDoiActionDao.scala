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
import nl.knaw.dans.easy.properties.app.model.{ DepositId, DoiAction, DoiActionEvent }
import nl.knaw.dans.easy.properties.app.repository.{ DepositIdAndTimestampAlreadyExistError, DoiActionDao, InvalidValueError, MutationError, MutationErrorOr, NoSuchDepositError, QueryErrorOr }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import resource.managed

class SQLDoiActionDao(override implicit val connection: Connection, errorHandler: SQLErrorHandler) extends DoiActionDao with SQLDeletableProperty with CommonResultSetParsers with DebugEnhancedLogging {

  override private[sql] val daoName = "DoiAction"
  override private[sql] val key = "doi-action"

  private def parseDoiActionEvent(resultSet: ResultSet): Either[InvalidValueError, DoiActionEvent] = {
    for {
      timestamp <- parseDateTime(resultSet.getTimestamp("timestamp", timeZone), timeZone)
      action <- parseEnumValue(DoiAction, "doi action")(resultSet.getString("value"))
    } yield DoiActionEvent(action, timestamp)
  }

  private def parseDepositIdAndDoiActionEvent(resultSet: ResultSet): Either[InvalidValueError, (DepositId, DoiActionEvent)] = {
    for {
      depositId <- parseDepositId(resultSet.getString("depositId"))
      doiActionEvent <- parseDoiActionEvent(resultSet)
    } yield depositId -> doiActionEvent
  }

  override def getCurrent(ids: Seq[DepositId]): QueryErrorOr[Seq[(DepositId, DoiActionEvent)]] = {
    trace(ids)

    executeGetCurrent(parseDepositIdAndDoiActionEvent)(QueryGenerator.getSimplePropsCurrentElementByDepositId(key))(ids)
  }

  override def getAll(ids: Seq[DepositId]): QueryErrorOr[Seq[(DepositId, Seq[DoiActionEvent])]] = {
    trace(ids)

    executeGetAll(parseDepositIdAndDoiActionEvent)(QueryGenerator.getSimplePropsAllElementsByDepositId(key))(ids)
  }

  override def store(id: DepositId, action: DoiActionEvent): MutationErrorOr[DoiActionEvent] = {
    trace(id, action)

    val query = QueryGenerator.storeSimpleProperty

    managed(connection.prepareStatement(query))
      .map(_.executeUpdateWith(id, key, action.value, action.timestamp))
      .either
      .either
      .leftMap(ts => {
        assert(ts.nonEmpty)
        ts.collectFirst {
          case t if errorHandler.isForeignKeyError(t) => NoSuchDepositError(id)
          case t if errorHandler.isUniquenessConstraintError(t) => DepositIdAndTimestampAlreadyExistError(id, action.timestamp, "doi action event")
        }.getOrElse(MutationError(ts.head.getMessage))
      })
      .map(_ => action)
  }
}
