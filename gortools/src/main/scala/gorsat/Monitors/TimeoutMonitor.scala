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

import gorsat.Commands.Analysis
import org.gorpipe.exceptions.custom.GorTimeoutException
import org.gorpipe.model.genome.files.gor.Row
import org.gorpipe.util.collection.extract.Extract

case class TimeoutMonitor() extends Analysis {
  val start = System.currentTimeMillis()

  // Check for timeout once every X rows. Enforce X >= 1
  val checkEveryXRows = math.max(1, System.getProperty("gor.querylimits.checkeveryxrows", "1000").toInt)

  // Default value 0 ignores timeout checks. Enforce value >= 0
  val timeout_millis = math.max(System.getProperty("gor.querylimits.timeout.seconds", "0").toInt, 0).toLong * 1000

  // Check for timeout every (checkEveryXRows) rows, and also at the first call to process on the instance
  var resettingRowCounter = checkEveryXRows


  override def isTypeInformationMaintained: Boolean = true

  override def process(r: Row) {
    //noinspection ScalaUselessExpression
    resettingRowCounter += 1
    if (timeout_millis > 0 && resettingRowCounter >= checkEveryXRows) {
      val elapsed = System.currentTimeMillis() - start
      if (elapsed > timeout_millis) throw new GorTimeoutException(
        String.format("Execution time: %s exceeded timeout: %s",
          Extract.durationString(elapsed),
          Extract.durationString(timeout_millis)))
      resettingRowCounter = 0
    }
    super.process(r)
  }
}
