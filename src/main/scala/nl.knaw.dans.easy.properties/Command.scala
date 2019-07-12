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

import better.files.File
import nl.knaw.dans.easy.DataciteService
import nl.knaw.dans.easy.properties.app.database.DatabaseAccess
import nl.knaw.dans.easy.properties.app.legacyImport.{ ImportProps, Interactor }
import nl.knaw.dans.easy.properties.app.repository.demo.DemoRepo
import nl.knaw.dans.easy.properties.server.{ DepositPropertiesGraphQLServlet, EasyDepositPropertiesService, EasyDepositPropertiesServlet, GraphiQLServlet }
import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.language.reflectiveCalls
import scala.util.control.NonFatal
import scala.util.{ Failure, Try }

object Command extends App with DebugEnhancedLogging {
  type FeedBackMessage = String

  val configuration = Configuration(File(System.getProperty("app.home")))
  val commandLine: CommandLineOptions = new CommandLineOptions(args, configuration) {
    verify()
  }
  val database = new DatabaseAccess(configuration.databaseConfig)

  runSubcommand()
    .doIfSuccess(msg => println(s"OK: $msg"))
    .doIfFailure { case e => logger.error(e.getMessage, e) }
    .doIfFailure { case NonFatal(e) => println(s"FAILED: ${ e.getMessage }") }

  private def runSubcommand(): Try[FeedBackMessage] = {
    commandLine.subcommand
      .collect {
        case loadProps @ commandLine.loadProps =>
          val propsFile = loadProps.properties()
          val importer = new ImportProps(
            repository = new DemoRepo().repository,
            interactor = new Interactor,
            datacite = new DataciteService(configuration.dataciteConfig),
          )

          Try {
            importer.loadDepositProperties(propsFile)
              .fold(_.msg, identity)
          }
        case commandLine.runService => runAsService()
      }
      .getOrElse(Failure(new IllegalArgumentException(s"Unknown command: ${ commandLine.subcommand }")))
  }

  private def runAsService(): Try[FeedBackMessage] = Try {
    val service = new EasyDepositPropertiesService(configuration.serverPort, Map(
      "/" -> new EasyDepositPropertiesServlet(configuration.version),
      "/graphql" -> DepositPropertiesGraphQLServlet(() => new DemoRepo().repository, configuration.auth, configuration.profilingConfig),
      "/graphiql" -> new GraphiQLServlet("/graphql"),
    ))
    Runtime.getRuntime.addShutdownHook(new Thread("service-shutdown") {
      override def run(): Unit = {
        service.stop().unsafeGetOrThrow
        database.closeConnectionPool().unsafeGetOrThrow
        service.destroy().unsafeGetOrThrow
      }
    })

    database.initConnectionPool().unsafeGetOrThrow
    service.start().unsafeGetOrThrow
    Thread.currentThread.join()
    "Service terminated normally."
  }
}
