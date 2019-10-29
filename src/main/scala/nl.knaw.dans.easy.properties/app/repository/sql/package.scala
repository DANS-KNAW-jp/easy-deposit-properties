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

import java.sql.{ PreparedStatement, ResultSet, Timestamp }
import java.util.Calendar

import nl.knaw.dans.easy.properties.app.model.DepositId
import org.joda.time.format.{ DateTimeFormatter, ISODateTimeFormat }
import org.joda.time.{ DateTime, DateTimeZone }
import resource.{ ManagedResource, managed }

import scala.language.implicitConversions

package object sql {

  val dateTimeFormatter: DateTimeFormatter = ISODateTimeFormat.dateTime()
  val timeZone: DateTimeZone = DateTimeZone.UTC

  implicit def timeZoneToCalendar(timeZone: DateTimeZone): Calendar = {
    Calendar.getInstance(timeZone.toTimeZone)
  }

  implicit def dateTimeToTimestamp(dt: DateTime): Timestamp = new Timestamp(dt.getMillis)

  type PrepStatementResolver = (PreparedStatement, Int) => Unit

  def setString(s: String): PrepStatementResolver = (ps, i) => ps.setString(i, s)

  def setDepositId(depositId: DepositId): PrepStatementResolver = setString(depositId.toString)

  def setInt(int: => Int): PrepStatementResolver = (ps, i) => ps.setInt(i, int)

  def setInt(s: String): PrepStatementResolver = setInt(s.toInt)

  implicit class RichManagedStatement(val statement: ManagedResource[PreparedStatement]) extends AnyVal {

    /** @return rowCount */
    def executeUpdateWith(values: Any*): Either[Seq[Throwable], Int] = {
      statement
        .map(executeUpdateWith(_, values))
        .either
        .either
    }

    def getResultSetForUpdateWith(values: Any*): ManagedResource[ResultSet] = {
      for {
        prepStatement <- statement
        _ = executeUpdateWith(prepStatement, values)
        resultSetForKey <- managed(prepStatement.getGeneratedKeys)
      } yield resultSetForKey
    }

    private def executeUpdateWith(prep: PreparedStatement, values: Seq[Any]): Int = {
      values.zipWithIndex.foreach {
        case (null, i) => prep.setString(i + 1, null)
        case (value: Boolean, i) => prep.setBoolean(i + 1, value)
        case (value: DateTime, i) => prep.setTimestamp(i + 1, value, timeZone)
        case (value: String, i) => prep.setString(i + 1, value)
        case (value, i) => prep.setString(i + 1, value.toString)
      }
      prep.executeUpdate()
    }
  }
}
