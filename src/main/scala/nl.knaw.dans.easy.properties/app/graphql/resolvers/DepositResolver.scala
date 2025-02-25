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
package nl.knaw.dans.easy.properties.app.graphql.resolvers

import nl.knaw.dans.easy.properties.app.graphql.DataContext
import nl.knaw.dans.easy.properties.app.model.{ Deposit, DepositId, Timestamp }
import nl.knaw.dans.easy.properties.app.repository.DepositFilters
import sangria.execution.deferred.Fetcher
import sangria.schema.DeferredValue

object DepositResolver {

  type DepositByIdFetcher = Fetcher[DataContext, (DepositId, Option[Deposit]), (DepositId, Option[Deposit]), DepositId]
  type DepositFetcher = Fetcher[DataContext, (DepositFilters, Seq[Deposit]), (DepositFilters, Seq[Deposit]), DepositFilters]

  val byIdFetcher: DepositByIdFetcher = Fetcher.caching(_.repo.deposits.find(_).toFuture)
  val depositsFetcher: DepositFetcher = Fetcher.caching(_.repo.deposits.search(_).toFuture)
  val lastModifiedFetcher: CurrentFetcher[Timestamp] = fetchCurrent(_.repo.deposits.lastModified)

  def depositById(id: DepositId)(implicit ctx: DataContext): DeferredValue[DataContext, Option[Deposit]] = {
    DeferredValue(byIdFetcher.defer(id))
      .map { case (_, optDeposit) => optDeposit }
  }

  def findDeposit(depositFilters: DepositFilters)(implicit ctx: DataContext): DeferredValue[DataContext, Seq[Deposit]] = {
    DeferredValue(depositsFetcher.defer(depositFilters))
      .map { case (_, deposits) => deposits }
  }

  def lastModified(depositId: DepositId)(implicit ctx: DataContext): DeferredValue[DataContext, Option[Timestamp]] = {
    DeferredValue(lastModifiedFetcher.defer(depositId))
      .map { case (_, lastModified) => lastModified }
  }
}
