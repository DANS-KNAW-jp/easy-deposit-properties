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

import cats.data.NonEmptyList
import cats.instances.option._
import cats.syntax.either._
import cats.syntax.functor._
import nl.knaw.dans.easy.properties.app.database.SQLErrorHandler
import nl.knaw.dans.easy.properties.app.model.identifier.IdentifierType.IdentifierType
import nl.knaw.dans.easy.properties.app.model.identifier.{ Identifier, IdentifierType, InputIdentifier }
import nl.knaw.dans.easy.properties.app.model.{ Deposit, DepositId }
import nl.knaw.dans.easy.properties.app.repository.{ IdentifierDao, InvalidValueError, MutationError, MutationErrorOr, NoSuchDepositError, QueryErrorOr }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import resource.managed

class SQLIdentifierDao(implicit connection: Connection, errorHandler: SQLErrorHandler) extends IdentifierDao with CommonResultSetParsers with DebugEnhancedLogging {

  private def parseIdentifier(resultSet: ResultSet): Either[InvalidValueError, Identifier] = {
    for {
      identifierSchema <- parseEnumValue(IdentifierType, "identifier type")(resultSet.getString("identifierSchema"))
      identifierId = resultSet.getString("identifierId")
      identifierValue = resultSet.getString("identifierValue")
      timestamp <- parseDateTime(resultSet.getTimestamp("timestamp", timeZone), timeZone)
    } yield Identifier(identifierId, identifierSchema, identifierValue, timestamp)
  }

  private def parseDepositIdAndIdentifier(resultSet: ResultSet): Either[InvalidValueError, (DepositId, Identifier)] = {
    for {
      depositId <- parseDepositId(resultSet.getString("depositId"))
      identifier <- parseIdentifier(resultSet)
    } yield depositId -> identifier
  }

  private def parseIdentifierIdAndDeposit(resultSet: ResultSet): Either[InvalidValueError, (String, Deposit)] = {
    for {
      deposit <- parseDeposit(resultSet)
      identifierId = resultSet.getString("identifierId")
    } yield identifierId -> deposit
  }

  override def getById(ids: Seq[String]): QueryErrorOr[Seq[(String, Option[Identifier])]] = {
    trace(ids)

    executeGetById(parseIdentifier)(QueryGenerator.getElementsById("Identifier", "identifierId"))(ids)
  }

  override def getByType(ids: Seq[(DepositId, IdentifierType)]): QueryErrorOr[Seq[((DepositId, IdentifierType), Option[Identifier])]] = {
    trace(ids)

    def collectResults(stream: Stream[(DepositId, Identifier)]): Seq[((DepositId, IdentifierType), Option[Identifier])] = {
      val results = stream.toList
        .groupBy { case (depositId, identifier) => (depositId, identifier.idType) }
        .flatMap {
          case (key, values) => values.headOption.map { case (_, identifier) => identifier }.tupleLeft(key)
        }
      ids.map(key => key -> results.get(key))
    }

    NonEmptyList.fromList(ids.toList)
      .map(QueryGenerator.getIdentifierByDepositIdAndType)
      .map(executeQuery(parseDepositIdAndIdentifier)(collectResults))
      .getOrElse(Seq.empty.asRight)
  }

  override def getByTypesAndValues(ids: Seq[(IdentifierType, String)]): QueryErrorOr[Seq[((IdentifierType, String), Option[Identifier])]] = {
    trace(ids)

    def collectResults(stream: Stream[Identifier]): Seq[((IdentifierType, String), Option[Identifier])] = {
      val results = stream.toList
        .groupBy(identifier => (identifier.idType, identifier.idValue))
        .flatMap {
          case (key, values) => values.headOption.tupleLeft(key)
        }
      ids.map(key => key -> results.get(key))
    }

    NonEmptyList.fromList(ids.toList)
      .map(QueryGenerator.getIdentifierByTypeAndValue)
      .map(executeQuery(parseIdentifier)(collectResults))
      .getOrElse(Seq.empty.asRight)
  }

  override def getAll(ids: Seq[DepositId]): QueryErrorOr[Seq[(DepositId, Seq[Identifier])]] = {
    trace(ids)

    executeGetAll(parseDepositIdAndIdentifier)(QueryGenerator.getAllElementsByDepositId("Identifier"))(ids)
  }

  override def store(id: DepositId, identifier: InputIdentifier): MutationErrorOr[Identifier] = {
    trace(id, identifier)
    val query = QueryGenerator.storeIdentifier

    val managedResultSet = for {
      prepStatement <- managed(connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS))
      _ = prepStatement.setString(1, id.toString)
      _ = prepStatement.setString(2, identifier.idType.toString)
      _ = prepStatement.setString(3, identifier.idValue)
      _ = prepStatement.setTimestamp(4, identifier.timestamp, timeZone)
      _ = prepStatement.executeUpdate()
      resultSetForKey <- managed(prepStatement.getGeneratedKeys)
    } yield resultSetForKey

    managedResultSet
      .map {
        case resultSet if resultSet.next() => resultSet.getLong(1).toString.asRight
        case _ => throw new Exception(s"not able to insert identifier (${ identifier.idType }, ${ identifier.idValue }, ${ identifier.timestamp })")
      }
      .either
      .either
      .leftMap(ts => {
        assert(ts.nonEmpty)
        ts.collectFirst {
          case t if errorHandler.isForeignKeyError(t) => NoSuchDepositError(id)
          case t if errorHandler.isUniquenessConstraintError(t) =>
            // we can't really decide which uniqueness constraint is violated here given the error returned from the database
            val msg = s"Cannot insert this identifier: identifier ${ identifier.idType } already exists for depositId $id or " +
              s"timestamp '${ identifier.timestamp }' is already used for another identifier associated to depositId $id."
            MutationError(msg)
        }.getOrElse(MutationError(ts.head.getMessage))
      })
      .flatMap(identity)
      .map(identifier.toOutput)
  }

  override def getDepositsById(ids: Seq[String]): QueryErrorOr[Seq[(String, Option[Deposit])]] = {
    trace(ids)

    executeGetDepositById(parseIdentifierIdAndDeposit)(QueryGenerator.getDepositsById("Identifier", "identifierId"))(ids)
  }
}
