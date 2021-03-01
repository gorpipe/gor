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

import gorsat.Commands.{Analysis, RowHeader}
import org.gorpipe.gor.model.Row
import org.gorpipe.model.gor.RowObj

import scala.util.matching.Regex

case class RegSelAnalysis(pattern: String,
                          source: Integer,
                          columns: Seq[String],
                          emptyString: String,
                          outgoingHeader: RowHeader
                         ) extends Analysis
{
  val regex: Regex = pattern.r
  override def isTypeInformationMaintained: Boolean = true

  override def setRowHeader(header: RowHeader): Unit = {
    rowHeader = header
    if(pipeTo != null) {
      pipeTo.setRowHeader(outgoingHeader.propagateTypes(rowHeader))
    }
  }

  override def process(r: Row): Unit = {
    val input = r.colAsString(source)
    val values = regex.findFirstMatchIn(input) match {
      case Some(m) =>
        try {
          columns.map(col => m.group(col))
        } catch {
          case _: IllegalArgumentException =>
            (1 to columns.length).map(ix => m.group(ix))
        }
      case _ => columns.map(_ => emptyString)
    }

    val rowAddition = values.mkString("\t")
    val rowText = r.getAllCols + "\t" + rowAddition
    val newRow = RowObj(rowText)
    super.process(newRow)
  }
}
