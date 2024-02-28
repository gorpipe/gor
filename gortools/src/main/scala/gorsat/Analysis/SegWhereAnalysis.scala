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
import org.gorpipe.data.Segment
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.model.Row
import org.gorpipe.gor.session.GorContext
import org.gorpipe.model.gor.RowObj



case class SegWhereAnalysis(context: GorContext,
                            minseg: Int,
                            startHalf: Boolean,
                            endHalf: Boolean,
                            paramString: String,
                            header: String,
                            ignoreFilterError: Boolean = false)
  extends Analysis with Filtering
{
  var segment: Segment = _
  var lastRow: Row = _
  var lastRowValue: Boolean = false

  filter.setContext(context, executeNor =  false)

  var isFilterValid: Boolean = false

  override def isTypeInformationNeeded: Boolean = true

  override def isTypeInformationMaintained: Boolean = true

  override def setRowHeader(incomingHeader: RowHeader): Unit = {
    if (incomingHeader == null || incomingHeader.isMissingTypes) return

    // todo: Once header is passed safely through remove this
    rowHeader = RowHeader(header.split('\t'), incomingHeader.columnTypes)

    try {
      compileFilter(rowHeader, paramString)

      isFilterValid = true
    } catch {
      case e: GorParsingException =>
        if(!ignoreFilterError)
          throw e
    }

    if (pipeTo != null) {
      val newRowHeader = RowHeader(Array("Chrom", "bpStart", "bpStop"), Array("S", "I", "I"))
      pipeTo.setRowHeader(newRowHeader)
    }
  }

  override def process(r: Row): Unit = {

    if (lastRow != null &&  r.chr != lastRow.chr) {
      produceRow()
      lastRow = null
      lastRowValue = false
    }

    val currentValue = if (isFilterValid) filter.evalBooleanFunction(r) else false

    if (lastRowValue && !currentValue) {
      // End of segment
      if (endHalf) {
        val half = (r.pos + lastRow.pos) / 2
        segment = new Segment(segment.start, half)
      }
      produceRow()
    } else if (!lastRowValue && currentValue) {
      // Start of segment
      if (startHalf) {
        val half = (r.pos + lastRow.pos) / 2
        segment = new Segment(half, r.pos)
      } else {
        segment = new Segment(r.pos, r.pos)
      }
    } else if (lastRowValue && currentValue) {
      // In segment, extend to the current position
      segment = new Segment(segment.start, r.pos)
    } else {
      // Not in segment
      segment = null
    }

    lastRow = r
    lastRowValue = currentValue
  }

  private def produceRow(): Unit = {
    if (segment == null || lastRow == null) return

    if (segment.length == 0) {
      segment = new Segment(segment.start, segment.start + 1)
    }

    if (minseg <= 0 || segment.length() >= minseg)
      super.process(RowObj.apply(s"${lastRow.chr}\t${segment.start}\t${segment.end}"))

    segment = null
  }


  override def finish(): Unit = {
    produceRow()
    filter.close()
  }
}
