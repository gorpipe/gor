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

import gorsat.Commands.Analysis
import org.gorpipe.model.genome.files.gor.Row
import org.gorpipe.model.gor.RowObj
import org.gorpipe.model.gor.iterators.RowSource

case class MergeSources(rightSource : RowSource, empty : String, addRightLeft : Boolean, ilCols : Array[Int], irCols : Array[Int], same : Boolean) extends Analysis {
  var lastRightRow : Row = _
  val rCols = irCols
  val lCols = ilCols

  def outputRightRow(r : Row) {
    if (same) {
      if (addRightLeft) super.process(r.rowWithAddedColumns("R"))
      else super.process(r)
    } else {
      val s = new java.lang.StringBuilder(r.length+20)
      var i = 0
      while (i < rCols.length) {
        if (rCols(i) >= 0) s.append(r.colAsString(rCols(i))) else s.append(empty)
        i += 1
        if (i < rCols.length) {
          s.append('\t')
        }
      }
      if (addRightLeft) s.append("\tR")
      super.process(RowObj(s))
    }
  }
  def outputLeftRow(r : Row) {
    if (same) {
      if (addRightLeft) super.process(r.rowWithAddedColumns("L"))
      else super.process(r)
    } else {
      val s = new java.lang.StringBuilder(r.length+20)
      var i = 0
      while (i < lCols.length) {
        if (lCols(i) >= 0) s.append(r.colAsString(lCols(i))) else s.append(empty)
        i += 1
        if (i < lCols.length) {
          s.append('\t')
        }
      }
      if (addRightLeft) s.append("\tL")
      super.process(RowObj(s))
    }
  }

  override def setup { val dummy = rightSource.hasNext }

  override def process(lr : Row) {
    if (lastRightRow != null && lastRightRow.advancedCompare(lr, null) < 0) {
      outputRightRow(lastRightRow); lastRightRow = null
    }
    var tryOutputFromRight = lastRightRow == null
    while (tryOutputFromRight && !wantsNoMore && rightSource.hasNext) {
      val rr = rightSource.next()
      if (rr.advancedCompare(lr, null) < 0) outputRightRow(rr) else {
        lastRightRow = rr; tryOutputFromRight = false
      }
    }
    outputLeftRow(lr)
  }
  override def finish {
    try {
      if (!isInErrorState) {
        if (lastRightRow != null) outputRightRow(lastRightRow)
        while (!wantsNoMore && rightSource.hasNext) outputRightRow(rightSource.next)
      }
    } finally {
      rightSource.close
    }
  }
}