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
package nl.knaw.dans.easy.properties.app.graphql.types

import nl.knaw.dans.easy.properties.app.model.{ Timestamp, Timestamped, timestampOrdering }
import sangria.marshalling.FromInput.coercedScalaInput
import sangria.schema.{ Argument, OptionInputType, WithArguments }

import scala.Ordering.Implicits._
import scala.language.postfixOps

trait TimebasedSearch {
  this: Scalars =>

  case class TimebasedSearchArguments(earlierThan: Option[Timestamp],
                                      laterThan: Option[Timestamp],
                                      atTimestamp: Option[Timestamp],
                                     )
  object TimebasedSearchArguments {
    def apply(args: WithArguments): TimebasedSearchArguments = {
      TimebasedSearchArguments(
        args arg earlierThanArgument,
        args arg laterThanArgument,
        args arg atTimestampArgument,
      )
    }
  }

  def filterTimebased(args: TimebasedSearchArguments)(timestamped: Timestamped): Boolean = {
    args match {
      case TimebasedSearchArguments(None, None, None) => true
      case TimebasedSearchArguments(Some(earlierThan), None, None) =>
        timestamped.timestamp < earlierThan
      case TimebasedSearchArguments(None, Some(laterThan), None) =>
        timestamped.timestamp > laterThan
      case TimebasedSearchArguments(Some(earlierThan), Some(laterThan), None) if earlierThan > laterThan =>
        timestamped.timestamp < earlierThan && timestamped.timestamp > laterThan
      case TimebasedSearchArguments(Some(earlierThan), Some(laterThan), None) =>
        timestamped.timestamp < earlierThan || timestamped.timestamp > laterThan
      case TimebasedSearchArguments(None, None, Some(atTimestamp)) =>
        timestamped.timestamp equiv atTimestamp
      case _ => throw new IllegalArgumentException("argument 'atTimestamp' cannot be used in conjunction with arguments 'earlierThan' or 'laterThan'")
    }
  }

  def filterTimebased(args: WithArguments)(timestamped: Timestamped): Boolean = {
    filterTimebased(TimebasedSearchArguments(args))(timestamped)
  }

  val earlierThanArgument: Argument[Option[Timestamp]] = Argument(
    name = "earlierThan",
    argumentType = OptionInputType(DateTimeType),
    description = Some("List only those elements that have a timestamp earlier than this given timestamp."),
    defaultValue = None,
    fromInput = coercedScalaInput,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  val laterThanArgument: Argument[Option[Timestamp]] = Argument(
    name = "laterThan",
    argumentType = OptionInputType(DateTimeType),
    description = Some("List only those elements that have a timestamp later than this given timestamp."),
    defaultValue = None,
    fromInput = coercedScalaInput,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  val atTimestampArgument: Argument[Option[Timestamp]] = Argument(
    name = "atTimestamp",
    argumentType = OptionInputType(DateTimeType),
    description = Some("List only those elements that have a timestamp equal to the given timestamp."),
    defaultValue = None,
    fromInput = coercedScalaInput,
    astDirectives = Vector.empty,
    astNodes = Vector.empty,
  )
  val timebasedSearchArguments = List(
    earlierThanArgument,
    laterThanArgument,
    atTimestampArgument,
  )

  def timebasedFilterAndSort[T <: Timestamped, Ord <: Ordering[T]](orderArgument: Argument[Option[Ord]])
                                                                  (input: Seq[T])
                                                                  (implicit context: WithArguments): Seq[T] = {
    val filtered = input.filter(filterTimebased(context))

    context.arg(orderArgument)
      .fold(filtered)(filtered.sorted(_))
  }
}
