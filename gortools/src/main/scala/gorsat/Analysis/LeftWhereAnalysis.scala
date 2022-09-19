/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2019 WuXi NextCode Inc.
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
import org.gorpipe.gor.session.GorSession
import org.gorpipe.model.gor.RowObj

case class LeftWhereAnalysis(session: GorSession, executeNor: Boolean, paramString: String,
                             header: String, colNum: Int, empty: String, gorcommand: String) extends Analysis with
  Filtering
{
  val lastCol: Int = header.split("\t").length
  var lastGroupID = ""
  var groupID = ""
  var buffer = new scala.collection.mutable.ArrayBuffer[Row]
  val emptyLine: String = header.slice(colNum + 1, lastCol).toList.tail.foldLeft(empty) ((x, y) => x + "\t" + empty)

  override def isTypeInformationNeeded: Boolean = true

  override def isTypeInformationMaintained: Boolean = true

  override def setRowHeader(incomingHeader: RowHeader): Unit = {
    if (incomingHeader == null || incomingHeader.isMissingTypes) return

    // todo: Once header is passed safely through remove this
    rowHeader = RowHeader(header.split('\t'), incomingHeader.columnTypes)

    compileFilter(rowHeader, paramString)

    if (pipeTo != null) {
      // Filtering doesn't change the columns
      pipeTo.setRowHeader(rowHeader)
    }
  }

  def emptyBuffer(): Unit = {
    var c = 0
    buffer.foreach(x => {
      if (filter.evalBooleanFunction(x)) {
        super.process(x)
        c += 1
      }
    })
    if (c == 0) super.process(RowObj(lastGroupID + "\t" + emptyLine))
    buffer = new scala.collection.mutable.ArrayBuffer[Row]
  }

  override def process(r: Row): Unit = {
    groupID = r.colsSlice(0, colNum + 1).toString
    if (groupID != lastGroupID && lastGroupID != "") emptyBuffer()
    buffer += r
    lastGroupID = groupID
  }

  override def finish(): Unit = {
    if (!isInErrorState && buffer.nonEmpty) emptyBuffer()
    filter.close()
  }
}