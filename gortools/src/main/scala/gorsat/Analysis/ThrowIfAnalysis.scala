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
import org.gorpipe.exceptions.GorDataException
import org.gorpipe.gor.model.Row
import org.gorpipe.gor.session.GorContext

case class ThrowIfAnalysis(context: GorContext, executeNor: Boolean, filterSrc: String, header: String, isRetriable: Boolean = false)
  extends Analysis with Filtering
{
  filter.setContext(context, executeNor)

  override def isTypeInformationNeeded: Boolean = true

  override def isTypeInformationMaintained: Boolean = true

  override def setRowHeader(incomingHeader: RowHeader): Unit = {
    if (incomingHeader == null || incomingHeader.isMissingTypes) return

    // todo: Once header is passed safely through remove this
    rowHeader = RowHeader(header.split('\t'), incomingHeader.columnTypes)

    compileFilter(rowHeader, filterSrc)

    if (pipeTo != null) {
      // Filtering doesn't change the columns
      pipeTo.setRowHeader(rowHeader)
    }
  }

  override def process(r: Row): Unit = {
    if (filter.evalBooleanFunction(r)) {
      val ex = new GorDataException(s"Gor throw on: $filterSrc", -1)
      if (isRetriable) {
        ex.fullRetry()
      }
      throw ex
    }
    else
      super.process(r)
  }

  override def finish(): Unit = {
    filter.close()
  }
}
