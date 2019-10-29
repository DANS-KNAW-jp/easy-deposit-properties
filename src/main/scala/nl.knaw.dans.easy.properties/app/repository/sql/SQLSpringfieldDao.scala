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
import nl.knaw.dans.easy.properties.app.model.springfield.{ InputSpringfield, Springfield, SpringfieldPlayMode }
import nl.knaw.dans.easy.properties.app.model.{ Deposit, DepositId }
import nl.knaw.dans.easy.properties.app.repository.{ DepositIdAndTimestampAlreadyExistError, InvalidValueError, MutationError, MutationErrorOr, NoSuchDepositError, QueryErrorOr, SpringfieldDao }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import resource.managed

class SQLSpringfieldDao(override implicit val connection: Connection, errorHandler: SQLErrorHandler) extends SpringfieldDao with SQLDeletable with CommonResultSetParsers with DebugEnhancedLogging {

  override private[sql] val daoName = "Springfield"

  private def parseSpringfield(resultSet: ResultSet): Either[InvalidValueError, Springfield] = {
    for {
      playMode <- parseEnumValue(SpringfieldPlayMode, "springfield playmode")(resultSet.getString("playmode"))
      springfieldId = resultSet.getString("springfieldId")
      domain = resultSet.getString("domain")
      user = resultSet.getString("springfield_user")
      collection = resultSet.getString("collection")
      timestamp <- parseDateTime(resultSet.getTimestamp("timestamp", timeZone), timeZone)
    } yield Springfield(springfieldId, domain, user, collection, playMode, timestamp)
  }

  private def parseDepositIdAndSpringfield(resultSet: ResultSet): Either[InvalidValueError, (DepositId, Springfield)] = {
    for {
      depositId <- parseDepositId(resultSet.getString("depositId"))
      springfield <- parseSpringfield(resultSet)
    } yield depositId -> springfield
  }

  private def parseSpringfieldIdAndDeposit(resultSet: ResultSet): Either[InvalidValueError, (String, Deposit)] = {
    for {
      deposit <- parseDeposit(resultSet)
      springfieldId = resultSet.getString("springfieldId")
    } yield springfieldId -> deposit
  }

  override def getById(ids: Seq[String]): QueryErrorOr[Seq[Springfield]] = {
    trace(ids)

    executeGetById(parseSpringfield)(QueryGenerator.getElementsById(daoName, "springfieldId"))(ids)
  }

  override def getCurrent(ids: Seq[DepositId]): QueryErrorOr[Seq[(DepositId, Springfield)]] = {
    trace(ids)

    executeGetCurrent(parseDepositIdAndSpringfield)(QueryGenerator.getCurrentElementByDepositId(daoName))(ids)
  }

  override def getAll(ids: Seq[DepositId]): QueryErrorOr[Seq[(DepositId, Seq[Springfield])]] = {
    trace(ids)

    executeGetAll(parseDepositIdAndSpringfield)(QueryGenerator.getAllElementsByDepositId(daoName))(ids)
  }

  override def store(id: DepositId, springfield: InputSpringfield): MutationErrorOr[Springfield] = {
    trace(id, springfield)
    val query = QueryGenerator.storeSpringfield

    val managedResultSet = for {
      prepStatement <- managed(connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS))
      _ = prepStatement.executeUpdateWith(id, springfield.domain, springfield.user, springfield.collection, springfield.playmode, springfield.timestamp)
      resultSetForKey <- managed(prepStatement.getGeneratedKeys)
    } yield resultSetForKey

    managedResultSet
      .map {
        case resultSet if resultSet.next() => resultSet.getLong(1).toString.asRight
        case _ => throw new Exception(s"not able to insert springfield configuration (${ springfield.domain }, ${ springfield.user }, ${ springfield.collection }, ${ springfield.playmode }, ${ springfield.timestamp })")
      }
      .either
      .either
      .leftMap(ts => {
        assert(ts.nonEmpty)
        ts.collectFirst {
          case t if errorHandler.isForeignKeyError(t) => NoSuchDepositError(id)
          case t if errorHandler.isUniquenessConstraintError(t) => DepositIdAndTimestampAlreadyExistError(id, springfield.timestamp, daoName.toLowerCase)
        }.getOrElse(MutationError(ts.head.getMessage))
      })
      .flatMap(identity)
      .map(springfield.toOutput)
  }

  override def getDepositsById(ids: Seq[String]): QueryErrorOr[Seq[(String, Deposit)]] = {
    trace(ids)

    executeGetDepositById(parseSpringfieldIdAndDeposit)(QueryGenerator.getDepositsById(daoName, "springfieldId"))(ids)
  }
}
