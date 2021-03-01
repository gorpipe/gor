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
import org.gorpipe.gor.model.Row
import org.gorpipe.model.gor.RowObj

case class ColTypeAnalysis() extends Analysis {
  override def isTypeInformationNeeded: Boolean = true

  override def process(r: Row) {
    if (r.numCols == 2) {
      super.process(r)
    } else {
      var tc = getTypedColumnValue(r, 2)
      for (i <- 3 until r.numCols) {
        tc += "\t"
        tc += getTypedColumnValue(r, i)
      }
      super.process(RowObj(r.chr, r.pos, tc))
    }
  }
  private def getTypedColumnValue(r: Row, i: Int) = {
    val columnType = rowHeader.columnTypes(i)
    s"$columnType(" + r.colAsString(i) + ")"
  }
}
