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

case class ToList(groupCols: Int, withCount: Boolean = false) extends Analysis {
  var lastKey: String = GorConstants.FIRST_POSSIBLE_CHROMOSOME_VALUE
  var lastRow: Row = _
  var rightRows: List[Row] = Nil

  def outputRows(r: Row, rightRows: List[Row]) = {
    if (rightRows.size > 1) {
      val allCols = rightRows.head.colsSlice(2 + groupCols, rightRows.head.numCols).toString.split("\t", -1)
      rightRows.tail.foreach(y => {
        var i = 2 + groupCols
        while (i < y.numCols) {
          allCols(i - (2 + groupCols)) += "," + y.colAsString(i); i += 1
        }
      }) // space eliminated for Gisli

      val listCols = allCols.toList.tail.foldLeft(allCols.toList.head) (_ + "\t" + _)
      if (withCount) super.process(r.rowWithAddedColumns(rightRows.size + "\t" + listCols))
      else super.process(r.rowWithAddedColumns(listCols))
    } else {
      if (withCount) super.process(r.rowWithAddedColumns("1\t" + rightRows.head.colsSlice(2 + groupCols, rightRows.head.numCols)))
      else super.process(r.rowWithAddedColumns(rightRows.head.colsSlice(2 + groupCols, rightRows.head.numCols)))
    }
  }

  override def process(rComb: Row) {
    val r = rComb.slicedRow(2, 2 + groupCols)
    val newKey = r.toString
    if (newKey == lastKey) {
      rightRows = rComb :: rightRows
    } else {
      if (lastKey != GorConstants.FIRST_POSSIBLE_CHROMOSOME_VALUE) outputRows(lastRow, rightRows.reverse)
      lastRow = r
      lastKey = newKey
      rightRows = List(rComb)
    }
  }

  override def finish {
    if (lastRow != null) outputRows(lastRow, rightRows.reverse)
  }
}
