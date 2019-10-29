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
import nl.knaw.dans.easy.properties.app.model.state.{ InputState, State, StateLabel }
import nl.knaw.dans.easy.properties.app.model.{ Deposit, DepositId }
import nl.knaw.dans.easy.properties.app.repository.{ DepositIdAndTimestampAlreadyExistError, InvalidValueError, MutationError, MutationErrorOr, NoSuchDepositError, QueryErrorOr, StateDao }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import resource.managed

class SQLStateDao(override implicit val connection: Connection, errorHandler: SQLErrorHandler) extends StateDao with SQLDeletable with CommonResultSetParsers with DebugEnhancedLogging {

  override private[sql] val daoName = "State"

  private def parseState(resultSet: ResultSet): Either[InvalidValueError, State] = {
    for {
      label <- parseEnumValue(StateLabel, "state label")(resultSet.getString("label"))
      stateId = resultSet.getString("stateId")
      description = resultSet.getString("description")
      timestamp <- parseDateTime(resultSet.getTimestamp("timestamp", timeZone), timeZone)
    } yield State(stateId, label, description, timestamp)
  }

  private def parseDepositIdAndState(resultSet: ResultSet): Either[InvalidValueError, (DepositId, State)] = {
    for {
      depositId <- parseDepositId(resultSet.getString("depositId"))
      state <- parseState(resultSet)
    } yield depositId -> state
  }

  private def parseStateIdAndDeposit(resultSet: ResultSet): Either[InvalidValueError, (String, Deposit)] = {
    for {
      deposit <- parseDeposit(resultSet)
      stateId = resultSet.getString("stateId")
    } yield stateId -> deposit
  }

  override def getById(ids: Seq[String]): QueryErrorOr[Seq[State]] = {
    trace(ids)

    executeGetById(parseState)(QueryGenerator.getElementsById(daoName, "stateId"))(ids)
  }

  override def getCurrent(ids: Seq[DepositId]): QueryErrorOr[Seq[(DepositId, State)]] = {
    trace(ids)

    executeGetCurrent(parseDepositIdAndState)(QueryGenerator.getCurrentElementByDepositId(daoName))(ids)
  }

  override def getAll(ids: Seq[DepositId]): QueryErrorOr[Seq[(DepositId, Seq[State])]] = {
    trace(ids)

    executeGetAll(parseDepositIdAndState)(QueryGenerator.getAllElementsByDepositId(daoName))(ids)
  }

  override def store(id: DepositId, state: InputState): MutationErrorOr[State] = {
    trace(id, state)
    val query = QueryGenerator.storeState

    val managedResultSet = for {
      prepStatement <- managed(connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS))
      _ = prepStatement.executeUpdateWith(id, state.label, state.description, state.timestamp)
      resultSetForKey <- managed(prepStatement.getGeneratedKeys)
    } yield resultSetForKey

    managedResultSet
      .map {
        case resultSet if resultSet.next() => resultSet.getLong(1).toString.asRight
        case _ => throw new Exception(s"not able to insert state (${ state.label }, ${ state.description }, ${ state.timestamp })")
      }
      .either
      .either
      .leftMap(ts => {
        assert(ts.nonEmpty)
        ts.collectFirst {
          case t if errorHandler.isForeignKeyError(t) => NoSuchDepositError(id)
          case t if errorHandler.isUniquenessConstraintError(t) => DepositIdAndTimestampAlreadyExistError(id, state.timestamp, daoName.toLowerCase)
        }.getOrElse(MutationError(ts.head.getMessage))
      })
      .flatMap(identity)
      .map(state.toOutput)
  }

  override def getDepositsById(ids: Seq[String]): QueryErrorOr[Seq[(String, Deposit)]] = {
    trace(ids)

    executeGetDepositById(parseStateIdAndDeposit)(QueryGenerator.getDepositsById(daoName, "stateId"))(ids)
  }
}
