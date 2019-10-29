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

import cats.data.NonEmptyList
import cats.instances.either._
import cats.instances.vector._
import cats.syntax.either._
import cats.syntax.functor._
import cats.syntax.traverse._
import nl.knaw.dans.easy.properties.app.model.{ Deposit, DepositId, Timestamp }
import nl.knaw.dans.easy.properties.app.repository.{ BagNameAlreadySetError, DepositAlreadyExistsError, DepositDao, DepositFilters, InvalidValueError, MutationError, MutationErrorOr, NoSuchDepositError, QueryErrorOr }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import resource.managed

class SQLDepositDao(override implicit val connection: Connection) extends DepositDao with SQLDeletable with CommonResultSetParsers with DebugEnhancedLogging {

  override private[sql] val daoName = "Deposit"

  private def parseLastModifiedResponse(resultSet: ResultSet): Either[InvalidValueError, (DepositId, Timestamp)] = {
    for {
      depositId <- parseDepositId(resultSet.getString("depositId"))
      maxTimestamp <- parseDateTime(resultSet.getTimestamp("max_timestamp", timeZone), timeZone)
    } yield depositId -> maxTimestamp
  }

  override def getAll: QueryErrorOr[Seq[Deposit]] = {
    trace(())

    executeQuery(parseDeposit)(_.toList)(QueryGenerator.getAllDeposits -> List.empty)
  }

  override def find(ids: Seq[DepositId]): QueryErrorOr[Seq[Deposit]] = {
    trace(ids)

    NonEmptyList.fromList(ids.toList)
      .map(QueryGenerator.findDeposits)
      .map(executeQuery(parseDeposit)(identity))
      .getOrElse(Seq.empty.asRight)
  }

  private def search(filters: DepositFilters): QueryErrorOr[Seq[Deposit]] = {
    executeQuery(parseDeposit)(_.toList)(QueryGenerator.searchDeposits(filters))
  }

  override def search(filters: Seq[DepositFilters]): QueryErrorOr[Seq[(DepositFilters, Seq[Deposit])]] = {
    trace(filters)
    // ideally this would be implemented with one query,
    // but that would be quite difficult (if not impossible) to do.
    // Also, 'filters' will usually have one element and can only get larger with deep nesting.
    filters.toVector.traverse(fs => search(fs).tupleLeft(fs))
  }

  override def store(deposit: Deposit): MutationErrorOr[Deposit] = {
    trace(deposit)
    val query = QueryGenerator.storeDeposit

    managed(connection.prepareStatement(query))
      .executeUpdateWith(deposit.id, deposit.bagName.orNull, deposit.creationTimestamp, deposit.depositorId, deposit.origin)
      .leftMap(_ => DepositAlreadyExistsError(deposit.id))
      .map(_ => deposit)
  }

  override def storeBagName(depositId: DepositId, bagName: String): MutationErrorOr[DepositId] = {
    trace(depositId, bagName)
    val query = QueryGenerator.storeBagName

    managed(connection.prepareStatement(query))
      .executeUpdateWith(bagName, depositId)
      .leftMap(_ => NoSuchDepositError(depositId))
      .flatMap {
        case 0 =>
          for {
            deposits <- find(Seq(depositId)).leftMap(error => MutationError(error.msg)) // not expected to have an error here, but just to be sure
            depId <- deposits.find(_.id == depositId)
              .map(_ => BagNameAlreadySetError(depositId)) // deposit was found, this means the bagName was already set for this deposit
              .getOrElse(NoSuchDepositError(depositId)) // deposit did not occur in search results; cannot set bagName for not-existing deposit
              .asLeft[DepositId]
              .map(_ => depositId)
          } yield depId
        case 1 => depositId.asRight
        case n => MutationError(s"Storing the bag's name caused $n rows to be updated. Only one updated row was expected.").asLeft
      }
  }

  override def lastModified(ids: Seq[DepositId]): QueryErrorOr[Seq[(DepositId, Timestamp)]] = {
    trace(ids)

    NonEmptyList.fromList(ids.toList)
      .map(QueryGenerator.getLastModifiedDate)
      .map(executeQuery(parseLastModifiedResponse)(identity))
      .getOrElse(Seq.empty.asRight)
  }
}
