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
import gorsat.gorsatGorIterator.MemoryMonitorUtil
import org.gorpipe.gor.model.Row

case class MemoryMonitor(logname: String,
                         minFreeMemMB: Int = MemoryMonitorUtil.memoryMonitorMinFreeMemMB,
                         minFreeMemRatio: Float = MemoryMonitorUtil.memoryMonitorMinFreeMemRatio) extends Analysis {
  val mmu: MemoryMonitorUtil = new MemoryMonitorUtil(MemoryMonitorUtil.basicOutOfMemoryHandler, minFreeMemMB, minFreeMemRatio)

  override def isTypeInformationMaintained: Boolean = true

  override def process(r: Row): Unit = {
    mmu.check(logname, mmu.lineNum, r)
    super.process(r)
  }
}