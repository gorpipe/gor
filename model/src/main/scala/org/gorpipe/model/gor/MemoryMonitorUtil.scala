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

package org.gorpipe.model.gor

import org.slf4j.{Logger, LoggerFactory}


/**
  * Helper class to implement simple memory monitor.
  *
  * @param handler            called when the free mem goes under the threshold.   The handler arguments are
  *                           <actualFreeMem> followed by arguments passed to the <check> function in an array.
  * @param reqMinFreeMemMB    threshold for miniumum free mem.
  * @param reqMinFreeMemRatio min free mem can be no larger than this ratio of the max mem (Xmx).
  * @param rowsBetweenChecks  check frequency, the memory will be checked every <rowsBetweenChecks> time the check method is called.
  * @param gcRatio            ratio of min free mem left when we try GC.
  *
  */
class MemoryMonitorUtil(handler: (Long, List[_]) => Unit,
                        val reqMinFreeMemMB: Int = MemoryMonitorUtil.memoryMonitorMinFreeMemMB,
                        val reqMinFreeMemRatio: Float = MemoryMonitorUtil.memoryMonitorMinFreeMemRatio,
                        val rowsBetweenChecks: Int = MemoryMonitorUtil.memoryMonitorRowsBetweenChecks,
                        val gcRatio: Float = MemoryMonitorUtil.memoryMonitorGCRatio) {
  var linesSinceLastCheck = 0L
  var lineNum = 0L
  val bytesInMB: Long = 1024L * 1024L
  val maxRuntimeMem: Long = Runtime.getRuntime.maxMemory(); // Maximum that can be allocated.
  val minFreeMem: Double = if (reqMinFreeMemMB > 0 && reqMinFreeMemRatio > 0) Math.min(bytesInMB * reqMinFreeMemMB, maxRuntimeMem * reqMinFreeMemRatio.toDouble)
  else if (reqMinFreeMemRatio > 0) maxRuntimeMem * reqMinFreeMemRatio.toDouble
  else bytesInMB * reqMinFreeMemMB
  val gcMinFreeMem: Long = Math.round(minFreeMem * gcRatio.toDouble)
  var lastGCTime = 0L
  var lastGCDuration = 0L

  def profile[R](code: => R, t: Long = System.currentTimeMillis()): (R, Long) = (code, System.currentTimeMillis - t)

  /**
    * Check the memory conditions, calls the set handler with <args> if the we are below the memory limits.
    *
    * @param args arguments to pass to the handler if we are below the memory limits.
    */
  def check(args: Any*): Unit = {
    if (minFreeMem > 0 && MemoryMonitorUtil.memoryMonitorRowsBetweenChecks > 0) { // Check if active.
      linesSinceLastCheck += 1
      lineNum += 1
      if (linesSinceLastCheck >= rowsBetweenChecks) {
        val total = Runtime.getRuntime.totalMemory() // Allocated.
        val free = Runtime.getRuntime.freeMemory() // Allocated but not yet used.
        val actualFreeMem = free + maxRuntimeMem - total
        val currentTimeMillis = System.currentTimeMillis()

        if (actualFreeMem < minFreeMem) {
          MemoryMonitorUtil.log.warn("MemoryMonitor: Calling handler free memory {} less than {}", actualFreeMem, minFreeMem)
          handler(actualFreeMem, args.toList)
        } else if (actualFreeMem < gcMinFreeMem && (currentTimeMillis - lastGCTime) * MemoryMonitorUtil.memoryMonitorMaxGCTimeRatio > lastGCDuration) {
          val gcTimeRatio = lastGCDuration.toFloat / (currentTimeMillis - lastGCTime).toFloat
          val msg = "MemoryMonitor: Forcing garbage collection as free memory ({}) less than {}, gc time ratio: {} less than {}"
          if (gcTimeRatio < 0.01) {
            MemoryMonitorUtil.log.debug(msg, actualFreeMem.toString, gcMinFreeMem.toString, gcTimeRatio.toString, MemoryMonitorUtil.memoryMonitorMaxGCTimeRatio.toString)
          } else {
            MemoryMonitorUtil.log.warn(msg, actualFreeMem.toString, gcMinFreeMem.toString, gcTimeRatio.toString, MemoryMonitorUtil.memoryMonitorMaxGCTimeRatio.toString)
          }
          lastGCTime = currentTimeMillis
          val (_, time) = profile {
            System.gc()
          }
          lastGCDuration = time
        }

        linesSinceLastCheck = 0L
      }
    }
  }
}

object MemoryMonitorUtil {
  private val log: Logger = LoggerFactory.getLogger("gor.gorsatUtilities")

  // You can specify the memory monitor limits either as MB or ratios of Xmx.
  // If both are specified the smaller MB number is used.
  var memoryMonitorMinFreeMemMB: Int = System.getProperty("gor.memoryMonitor.minFreeMemMB", "-1").toInt
  var memoryMonitorMinFreeMemRatio: Float = System.getProperty("gor.memoryMonitor.minFreeMemRatio", "-1").toFloat

  // Ratio of min free mem left when we try GC.
  var memoryMonitorGCRatio: Float = System.getProperty("gor.memoryMonitor.gcRatio", "1.5").toFloat
  var memoryMonitorMaxGCTimeRatio: Float = System.getProperty("gor.memoryMonitor.maxGCTimeRatio", "0.5").toFloat
  var memoryMonitorRowsBetweenChecks: Int = System.getProperty("gor.memoryMonitor.rowsBetweenChecks", "10000").toInt

  var memoryMonitorActive: Boolean = (memoryMonitorMinFreeMemMB > 0 || memoryMonitorMinFreeMemRatio > 0) && memoryMonitorRowsBetweenChecks > 0

  def basicOutOfMemoryHandler(actualFreeMem: Long, args: List[_]) : Unit = {
    val logName = args.head
    val lineNum = args(1)
    val line = args(2)
    val msg = "MemoryMonitor: Out of memory executing " + logName + " (line " + lineNum + ").  Free mem down to " + actualFreeMem / (1024L * 1024L) + " MB.\n" + line
    throw new Exception(msg)
  }
}