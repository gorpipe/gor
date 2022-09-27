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
import org.slf4j.LoggerFactory

object CountRows {

  private val consoleLogger = LoggerFactory.getLogger("console." + this.getClass)

  case class CountRowsAnalysis() extends Analysis {
    var m = 0L
    var chrLastReported = "chr"
    var mLastReported = 0L

    override def process(r: Row): Unit = {
      if (r.chr != chrLastReported) {
        if (chrLastReported != "chr") {
          consoleLogger.info(chrLastReported + ": " + (m - mLastReported) + " rows")
        }
        mLastReported = m
        chrLastReported = r.chr
      }
      m += 1
      super.process(r)
    }

    override def finish(): Unit = {
      consoleLogger.info(chrLastReported + ": " + (m - mLastReported) + " rows")
      consoleLogger.info("Total rows: " + m + " rows")
    }
  }

}
