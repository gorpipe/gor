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
import org.gorpipe.gor.model.Row
import org.gorpipe.model.gor.RowObj

case class ChopSegment(maxSegSize: Int) extends Analysis {

  override def isTypeInformationMaintained: Boolean = true

  def recursiveChop(r: Row): Unit = {
    val rangeStopPos = r.colAsInt(2)
    val rangeStopStringLength = r.colAsString(2).length
    if (rangeStopPos - r.pos > maxSegSize) {
      recursiveChop(RowObj(r.chr, r.pos, (r.pos + (rangeStopPos - r.pos) / 2).toString + r.otherCols.slice(rangeStopStringLength, r.otherCols.length)))
      recursiveChop(RowObj(r.chr, r.pos + (rangeStopPos - r.pos) / 2, rangeStopPos.toString + r.otherCols.slice(rangeStopStringLength, r.otherCols.length)))
    } else super.process(r)
  }

  override def process(r: Row): Unit = {
    if (r.colAsInt(2) - r.pos > maxSegSize) recursiveChop(r)
    else super.process(r)
  }
}
