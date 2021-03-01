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

import scala.collection.mutable.ArrayBuffer

case class ColSplitAnalysis(col: Int, colnum: Int, sepval: String, missingVal: String, writeCols: Boolean,
                            outgoingHeader: RowHeader) extends Analysis {

  override def isTypeInformationMaintained: Boolean = false

  override def setRowHeader(header: RowHeader): Unit = {
    rowHeader = header

    if (pipeTo != null) {
      val fixedUpHeader = outgoingHeader.propagateTypes(rowHeader)
      pipeTo.setRowHeader(fixedUpHeader)
    }
  }

  override def process(r: Row) {
    val colSplit = r.colAsString(col).toString.split(sepval, -1)
    var columnsToAdd = ArrayBuffer[CharSequence]()

    if (writeCols) {
      columnsToAdd += colSplit.length.toString
    }
    if (colnum >= colSplit.length) {
      columnsToAdd ++= colSplit
      for (_ <- colSplit.length + 1 to colnum) {
        columnsToAdd += missingVal
      }
    } else {
      for (i <- 0 to colnum-1) {
        columnsToAdd += colSplit(i)
      }
    }
    super.process(r.rowWithAddedColumns(columnsToAdd.toArray))
  }
}