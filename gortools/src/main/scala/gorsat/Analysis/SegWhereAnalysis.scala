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
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.model.Row
import org.gorpipe.gor.session.GorContext
import org.gorpipe.model.gor.RowObj

case class SegWhereAnalysis(context: GorContext, maxSeg: Int, paramString: String, header: String,
                            ignoreFilterError: Boolean = false)
  extends Analysis with Filtering
{
  var firstRow: Row = _
  var currentRow: Row = _

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

    if (firstRow != null &&  r.chr != firstRow.chr) {
      produceRow()
      firstRow = null
      currentRow = null
    }

    if (!isFilterValid || filter.evalBooleanFunction(r)) {
      if (firstRow == null) {
        firstRow = r
        currentRow = r
      } else {
        val segLength = r.pos - currentRow.pos

        if (segLength > maxSeg) {
          produceRow()
          firstRow = r
          currentRow = r
        } else {
          currentRow = r
        }
      }
    } else {
      produceRow()
    }
  }

  private def produceRow(): Unit = {
    if (firstRow == null) return

    val segLength = currentRow.pos - firstRow.pos
    if (segLength > 0) {
      super.process(RowObj.apply(s"${firstRow.chr}\t${firstRow.pos}\t${firstRow.pos + segLength}"))
    }

    currentRow = null
    firstRow = null
  }


  override def finish(): Unit = {
    produceRow()
    filter.close()
  }
}
