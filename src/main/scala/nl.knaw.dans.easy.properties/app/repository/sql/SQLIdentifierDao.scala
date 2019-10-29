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
import cats.syntax.either._
import cats.syntax.option._
import nl.knaw.dans.easy.properties.app.database.SQLErrorHandler
import nl.knaw.dans.easy.properties.app.model.identifier.IdentifierType.IdentifierType
import nl.knaw.dans.easy.properties.app.model.identifier.{ Identifier, IdentifierType, InputIdentifier }
import nl.knaw.dans.easy.properties.app.model.{ Deposit, DepositId }
import nl.knaw.dans.easy.properties.app.repository.{ IdentifierDao, InvalidValueError, MutationError, MutationErrorOr, NoSuchDepositError, QueryErrorOr }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import resource.managed

class SQLIdentifierDao(override implicit val connection: Connection, errorHandler: SQLErrorHandler) extends IdentifierDao with SQLDeletable with CommonResultSetParsers with DebugEnhancedLogging {

  override private[sql] val daoName = "Identifier"

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

  override def getById(ids: Seq[String]): QueryErrorOr[Seq[Identifier]] = {
    trace(ids)

    executeGetById(parseIdentifier)(QueryGenerator.getElementsById(daoName, "identifierId"))(ids)
  }

  override def getByType(ids: Seq[(DepositId, IdentifierType)]): QueryErrorOr[Seq[((DepositId, IdentifierType), Identifier)]] = {
    trace(ids)

    def collectResults(stream: Stream[(DepositId, Identifier)]): Seq[((DepositId, IdentifierType), Identifier)] = {
      stream.toList
        .groupBy { case (depositId, identifier) => (depositId, identifier.idType) }
        .flatMap {
          case (key, (_, identifier) :: Nil) => (key -> identifier).some
          case (_, Nil) => None // should not occur due to the nature of `groupBy`
          case ((depositId, identifierType), _) =>
            assert(assertion = false, s"a unique result was expected for combination ($depositId, $identifierType); but found multiple results")
            None // should not occur, as `(depositId, identifierType)` is a unique combination (see SQL schema)
        }
        .toSeq
    }

    NonEmptyList.fromList(ids.toList)
      .map(QueryGenerator.getIdentifierByDepositIdAndType)
      .map(executeQuery(parseDepositIdAndIdentifier)(collectResults))
      .getOrElse(Seq.empty.asRight)
  }

  override def getByTypesAndValues(ids: Seq[(IdentifierType, String)]): QueryErrorOr[Seq[((IdentifierType, String), Identifier)]] = {
    trace(ids)

    def collectResults(stream: Stream[Identifier]): Seq[((IdentifierType, String), Identifier)] = {
      stream.toList
        .groupBy(identifier => (identifier.idType, identifier.idValue))
        .flatMap {
          case (key, value :: Nil) => (key -> value).some
          case (_, Nil) => None // should not occur due to the nature of `groupBy`
          case ((identifierType, identifierValue), _) =>
            assert(assertion = false, s"a unique result was expected for combination ($identifierType, $identifierValue); but found multiple results")
            None // should not occur
        }
        .toSeq
    }

    NonEmptyList.fromList(ids.toList)
      .map(QueryGenerator.getIdentifierByTypeAndValue)
      .map(executeQuery(parseIdentifier)(collectResults))
      .getOrElse(Seq.empty.asRight)
  }

  override def getAll(ids: Seq[DepositId]): QueryErrorOr[Seq[(DepositId, Seq[Identifier])]] = {
    trace(ids)

    executeGetAll(parseDepositIdAndIdentifier)(QueryGenerator.getAllElementsByDepositId(daoName))(ids)
  }

  override def store(id: DepositId, identifier: InputIdentifier): MutationErrorOr[Identifier] = {
    trace(id, identifier)
    val query = QueryGenerator.storeIdentifier

    managed(connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS))
      .getResultSetForUpdateWith(id, identifier.idType, identifier.idValue, identifier.timestamp)
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
            val msg = s"Cannot insert this identifier: identifier ${ identifier.idType } already exists for depositId $id with timestamp '${ identifier.timestamp }' or " +
              s"${ identifier.idType } '${ identifier.idValue }' is already associated with another deposit."
            MutationError(msg)
        }.getOrElse(MutationError(ts.head.getMessage))
      })
      .flatMap(identity)
      .map(identifier.toOutput)
  }

  override def getDepositsById(ids: Seq[String]): QueryErrorOr[Seq[(String, Deposit)]] = {
    trace(ids)

    executeGetDepositById(parseIdentifierIdAndDeposit)(QueryGenerator.getDepositsById(daoName, "identifierId"))(ids)
  }
}
