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
package nl.knaw.dans.easy.properties.app

import cats.syntax.either._
import nl.knaw.dans.easy.properties.app.model.DepositId
import nl.knaw.dans.easy.properties.app.repository.{ MutationError, MutationErrorOr, Repository }

class Deleter(repository: => Repository) {

  def deleteDepositsBy(ids: Seq[DepositId]): MutationErrorOr[Seq[DepositId]] = {
    for {
      actualIds <- findActualDepositIds(ids)
      _ <- repository.states.deleteBy(actualIds)
      _ <- repository.identifiers.deleteBy(actualIds)
      _ <- repository.curation.deleteBy(actualIds)
      _ <- repository.springfield.deleteBy(actualIds)
      _ <- repository.ingestSteps.deleteBy(actualIds)
      _ <- repository.doiRegistered.deleteBy(actualIds)
      _ <- repository.doiAction.deleteBy(actualIds)
      _ <- repository.contentType.deleteBy(actualIds)
      _ <- repository.deposits.deleteBy(actualIds) // last because of foreign keys by the other.deleteBy(actualIds)
    } yield actualIds
  }

  private def findActualDepositIds(ids: Seq[DepositId]): MutationErrorOr[List[DepositId]] = {
    repository.deposits
      .find(ids)
      .bimap(error => MutationError(error.msg), _.map(_.id).toList)
  }
}
