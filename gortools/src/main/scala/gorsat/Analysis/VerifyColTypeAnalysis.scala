/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
 *  All Rights Reserved.
 *
 *  GORpipe is free software: you can redistribute it and/or modify
 *  it under the terms of the AFFERO GNU General Public License as published by
 *  the Free Software Foundation.
 *
 *  GORpipe is distributed "AS-IS" AND WITHOUT ANY WARRANTY OF ANY KIND,
 *  INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 *  NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR PURPOSE. See
 *  the AFFERO GNU General Public License for the complete license terms.
 *
 *  You should have received a copy of the AFFERO GNU General Public License
 *  along with GORpipe.  If not, see <http://www.gnu.org/licenses/agpl-3.0.html>
 *
 *  END_COPYRIGHT
 */

package gorsat.Analysis

import gorsat.Commands.Analysis
import org.gorpipe.exceptions.GorDataException
import org.gorpipe.gor.model.Row

case class VerifyColTypeAnalysis() extends Analysis {
  private var rowCounter = 0

  override def isTypeInformationNeeded: Boolean = true
  override def isTypeInformationMaintained: Boolean = true

  override def process(r: Row): Unit = {
    rowCounter += 1

    for(i <- rowHeader.columnTypes.indices) {
      val tp = rowHeader.columnTypes(i)
      tp match {
        case "I" =>
          try {
            r.colAsInt(i)
          } catch {
            case e: NumberFormatException =>
              throwGorDataException(r, i, e, "integer")
          }
        case "L" =>
          try {
            r.colAsLong(i)
          } catch {
            case e: NumberFormatException =>
              throwGorDataException(r, i, e, "long")
          }
        case "D" =>
          try {
            r.colAsDouble(i)
          } catch {
            case e: NumberFormatException =>
              throwGorDataException(r, i, e, "number")
          }
        case "S" =>
          r.colAsString(i)
      }
    }
    super.process(r)
  }

  private def throwGorDataException(row: Row, column: Int, exception: NumberFormatException, typename: String) = {
    val col = rowHeader.columnNames(column)
    val rowContents = row.getAllCols.toString
    val msg = s"Error in step: VERIFYCOLTYPE\nInvalid $typename in $col in row $rowCounter - $rowContents"
    throw new GorDataException(msg, -1, rowHeader.toString, rowContents, exception)
  }
}
