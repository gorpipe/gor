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
import org.gorpipe.gor.session.GorSession
import org.gorpipe.model.gor.RowObj

case class Chopper(fuzzfac: Int, header: String, session: GorSession) extends Analysis {
  val colNum: Int = header.split("\t").length
  var maxEnd: Int = 0

  // Note that this is cheating a bit - this step adds a column but it is removed again
  // in VennSeg - these steps work together to implement in SeqProj so they simply pass
  // the header with types along.
  override def isTypeInformationMaintained: Boolean = true

  override def process(r: Row) {
    val stop = r.colAsInt(2)
    var newStart = r.pos - fuzzfac
    if (newStart < 0) newStart = 0
    try {
      maxEnd = session.getProjectContext.getReferenceBuild.getBuildSize.get(r.chr)
    } catch {
      case _: Exception =>
        maxEnd = 1000000000
    }

    var newEnd = stop + fuzzfac
    if (newEnd > maxEnd) newEnd = maxEnd
    if (newStart >= newEnd) newStart = newEnd - 1
    if (colNum > 3) {
      super.process(RowObj(r.chr, newStart, "sta\t" + r.colsSlice(3, colNum)))
      super.process(RowObj(r.chr, newEnd, "sto\t" + r.colsSlice(3, colNum)))
    } else {
      super.process(RowObj(r.chr, newStart, "sta"))
      super.process(RowObj(r.chr, newEnd, "sto"))
    }
  }
}
