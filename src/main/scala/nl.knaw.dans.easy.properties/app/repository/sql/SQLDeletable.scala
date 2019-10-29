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

import java.sql.Connection

import cats.data.NonEmptyList
import cats.syntax.either._
import nl.knaw.dans.easy.properties.app.model.DepositId
import nl.knaw.dans.easy.properties.app.repository.{ Deletable, MutationError, MutationErrorOr }
import resource.managed

trait SQLDeletable extends Deletable {
  implicit val connection: Connection
  private[sql] val daoName: String

  /** @return number of delete rows */
  def deleteBy(ids: Seq[DepositId]): MutationErrorOr[Int] = {
    NonEmptyList.fromList(ids.toList)
      .map(delete)
      .getOrElse(0.asRight)
  }

  /** @return rowCount */
  private def delete(ids: NonEmptyList[DepositId]): MutationErrorOr[Int] = {
    managed(connection.prepareStatement(getQuery(ids)))
      .map(statement =>
        statement.executeUpdateWith(ids.map(_.toString).toList: _*)
      )
      .either
      .either
      .leftMap(throwables => {
        assert(throwables.nonEmpty)
        MutationError(throwables.map(_.getMessage).mkString("; "))
      })
  }

  private[sql] def getQuery(ids: NonEmptyList[DepositId]): String = {
    QueryGenerator.deleteByDepositId(daoName)(ids)
  }
}
