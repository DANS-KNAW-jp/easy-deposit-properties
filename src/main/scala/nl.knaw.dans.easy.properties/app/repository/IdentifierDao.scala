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
package nl.knaw.dans.easy.properties.app.repository

import nl.knaw.dans.easy.properties.app.model.identifier.IdentifierType.IdentifierType
import nl.knaw.dans.easy.properties.app.model.identifier.{ Identifier, InputIdentifier }
import nl.knaw.dans.easy.properties.app.model.{ Deposit, DepositId }

trait IdentifierDao {

  def getById(ids: Seq[String]): QueryErrorOr[Seq[(String, Option[Identifier])]]

  def getByType(ids: Seq[(DepositId, IdentifierType)]): QueryErrorOr[Seq[((DepositId, IdentifierType), Option[Identifier])]]

  def getByTypesAndValues(ids: Seq[(IdentifierType, String)]): QueryErrorOr[Seq[((IdentifierType, String), Option[Identifier])]]

  def getAll(ids: Seq[DepositId]): QueryErrorOr[Seq[(DepositId, Seq[Identifier])]]

  def store(id: DepositId, identifier: InputIdentifier): MutationErrorOr[Identifier]

  def getDepositsById(ids: Seq[String]): QueryErrorOr[Seq[(String, Option[Deposit])]]
}
