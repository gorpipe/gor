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
import org.gorpipe.gor.GorConstants
import org.gorpipe.gor.model.Row
import org.gorpipe.model.gor.RowObj

case class NClosest(groupCols: Int, n: Int) extends Analysis {
  // In addition to GOR, this methods assumes fully ordered left-source
  var lastKey: String = GorConstants.FIRST_POSSIBLE_CHROMOSOME_VALUE
  var lastRow: Row = _
  var rightRows: List[(Int, String)] = Nil

  def outputRows(r: Row, rightRows: List[(Int, String)]) = {
    rightRows.sortWith((x, y) => scala.math.abs(x._1) < scala.math.abs(y._1)).slice(0, n).map(x => x._2).foreach(x => super.process(RowObj(r.chr, r.pos, r.otherCols + "\t" + x)))
  }

  override def process(rComb: Row) {
    val r = rComb.slicedRow(2, groupCols + 2)
    val rightCols = rComb.otherColsSlice(2 + groupCols, rComb.numCols).toString
    var distance = Int.MaxValue
    try {
      distance = rComb.colAsInt(groupCols + 2)
    } catch {
      case e: Exception => /* do nothing */
    }
    val newKey = r.chr + "#" + r.pos + r.otherCols
    if (newKey == lastKey) {
      rightRows = (distance, rightCols) :: rightRows
    } else {
      if (lastKey != GorConstants.FIRST_POSSIBLE_CHROMOSOME_VALUE) outputRows(lastRow, rightRows.reverse)
      lastRow = r
      lastKey = newKey
      rightRows = List((distance, rightCols))
    }
  }

  override def finish {
    outputRows(lastRow, rightRows.reverse)
  }
}
