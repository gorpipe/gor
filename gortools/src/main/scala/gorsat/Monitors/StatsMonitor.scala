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

package gorsat.Monitors

import gorsat.Commands.{Analysis, RowHeader}
import org.gorpipe.gor.model.Row

/**
 * Collect basic stats about the analysis stream.
 */
case class StatsMonitor() extends Analysis {
  var startTime: Long = System.currentTimeMillis
  var stopTime = 0L

  var rowCount = 0L
  var bytesCount = 0L

  def elapsedTime(): Long =  if (stopTime > 0)  stopTime - startTime else System.currentTimeMillis - startTime

  override def process(r : Row): Unit = {
    bytesCount += r.length();
    rowCount += 1;

    super.process(r)
  }
  override def finish(): Unit = {
    if (rowHeader == null) {
      rowHeader = new RowHeader(Array(), Array())
    }
    stopTime = System.currentTimeMillis;
  }

  /**
   * Does execution of this step preserve column type information?
   * The value false (default) means if a downstream step requires types, then they must be inferred.
   * true means that columns, except for those returned by columnsWithoutTypes(), already
   * have type info which need not be inferred.
   */
  override def isTypeInformationMaintained: Boolean = true
}
