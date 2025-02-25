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
package nl.knaw.dans.easy.properties.app.graphql

import sangria.execution.deferred.HasId
import sangria.marshalling.{ CoercedScalaResultMarshaller, FromInput, ResultMarshaller }
import sangria.relay.{ Identifiable, Node }
import sangria.schema.Context

import scala.language.implicitConversions

package object types {

  private[types] implicit def dataContextFromContext(implicit ctx: Context[DataContext, _]): DataContext = ctx.ctx

  implicit def keyBasedHasId[K, V]: HasId[(K, V), K] = HasId { case (id, _) => id }

  implicit def nodeIdentifiable[T <: Node]: Identifiable[T] = _.id

  implicit def fromInput[T](create: Map[String, Any] => T): FromInput[T] = new FromInput[T] {
    override val marshaller: ResultMarshaller = CoercedScalaResultMarshaller.default

    override def fromResult(node: marshaller.Node): T = create(node.asInstanceOf[Map[String, Any]])
  }
}
