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
import org.gorpipe.model.genome.files.gor.Row
import org.gorpipe.model.gor.RowObj

case class ColNumAnalysis() extends Analysis {
  override def isTypeInformationMaintained: Boolean = true

  override def setRowHeader(header: RowHeader): Unit = {
    rowHeader = header
    if(pipeTo != null) {
      val columnTypes = rowHeader.columnTypes
      for(ix <- 2 to columnTypes.length - 1) {
        columnTypes(ix) = "S"
      }
      pipeTo.setRowHeader(RowHeader(rowHeader.columnNames, columnTypes))
    }
  }

  override def process(r: Row) {
    if (r.numCols == 2) super.process(r)
    else {
      if (r.numCols == 3)
        super.process(RowObj(r.chr, r.pos, "3(" + r.otherCols + ")"))
      else {
        var tc = "3(" + r.colAsString(2) + ")"
        for (i <- 4 to r.numCols) {
          tc += "\t" + i + "(" + r.colAsString(i - 1) + ")"
        }
        super.process(RowObj(r.chr, r.pos, tc))
      }
    }
  }
}
