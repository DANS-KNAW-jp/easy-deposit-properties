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
package nl.knaw.dans.easy.properties

import java.sql.Connection
import java.util.concurrent.Executors

import better.files.File
import nl.knaw.dans.easy.properties.app.database.{ DatabaseAccess, SQLErrorHandler }
import nl.knaw.dans.easy.properties.app.repository.sql.SQLRepo
import nl.knaw.dans.easy.properties.server.{ DepositPropertiesGraphQLServlet, EasyDepositPropertiesService, EasyDepositPropertiesServlet, GraphiQLServlet }
import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.commons.daemon.{ Daemon, DaemonContext }

import scala.concurrent.{ ExecutionContext, ExecutionContextExecutor }

class ServiceStarter extends Daemon with DebugEnhancedLogging {
  var database: DatabaseAccess = _
  var service: EasyDepositPropertiesService = _

  override def init(context: DaemonContext): Unit = {
    logger.info("Initializing service...")
    val configuration = Configuration(File(System.getProperty("app.home")))
    database = new DatabaseAccess(configuration.databaseConfig)
    implicit val executionContext: ExecutionContextExecutor = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(16))
    implicit val sqlErrorHandler: SQLErrorHandler = SQLErrorHandler(configuration.databaseConfig)
    service = new EasyDepositPropertiesService(configuration.serverPort, Map(
      "/" -> new EasyDepositPropertiesServlet(configuration.version),
      "/graphql" -> DepositPropertiesGraphQLServlet[Connection](database.futureTransaction, implicit conn => new SQLRepo().repository, configuration.auth, configuration.profilingConfig),
      "/graphiql" -> new GraphiQLServlet("/graphql"),
    ))
    logger.info("Service initialized.")
  }

  override def start(): Unit = {
    logger.info("Starting service...")
    database.initConnectionPool().unsafeGetOrThrow
    service.start().unsafeGetOrThrow
    logger.info("Service started.")
  }

  override def stop(): Unit = {
    logger.info("Stopping service...")
    service.stop().unsafeGetOrThrow
    database.closeConnectionPool().unsafeGetOrThrow
  }

  override def destroy(): Unit = {
    service.destroy().unsafeGetOrThrow
    logger.info("Service stopped.")
  }
}
