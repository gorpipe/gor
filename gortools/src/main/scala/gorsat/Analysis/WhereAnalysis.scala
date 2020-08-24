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

case class WhereAnalysis(context: GorContext, executeNor: Boolean, paramString: String, header: String,
                         ignoreFilterError: Boolean = false)
  extends Analysis with Filtering
{
  statsSenderName = "Where"
  statsSenderAnnotation = paramString
  setContext(context)

  filter.setContext(context, executeNor)

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
      // Filtering doesn't change the columns
      pipeTo.setRowHeader(rowHeader)
    }
  }

  override def process(r: Row) {
    if (!isFilterValid || filter.evalBooleanFunction(r)) {
      // Row is passed if the filter is invalid (TRYWHERE) or if the filter evaluates to true
      statsInc("rows passed through filter")
      super.process(r)
    } else {
      statsInc("rows filtered out")
    }
  }

  override def finish() {
    filter.close()
  }
}
