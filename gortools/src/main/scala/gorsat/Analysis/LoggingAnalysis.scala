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

import java.io.{FileWriter, IOException}

import gorsat.Commands.Analysis
import org.gorpipe.gor.model.Row
import org.gorpipe.gor.session.GorSession
import org.slf4j.{Logger, LoggerFactory}

object LoggingAnalysis {

  private val consoleLogger = LoggerFactory.getLogger("console." + this.getClass)

  case class LogAnalysis(session: GorSession, loggerName: String, logNthRow: Int, logTimeInterval: Int, filepath: String, level: String = null, label: String) extends Analysis {
    // Row number
    var m = 0L

    // The following five variables are only used when logging by time:
    // Rows between time checks
    var tn = 1L
    // Rows since last check
    var tm = 0L
    // Timestamp from last log
    var lastlog = 0L
    // Timestamp from last check
    var lastcheck = 0L
    // The targeted difference between checks
    val diff: Int = logTimeInterval * 10

    var fw: FileWriter = _

    override def isTypeInformationMaintained: Boolean = true

    override def setup() {
      if (filepath != null && filepath.length > 0) {
        try {
          fw = new FileWriter(filepath, true)
        } catch {
          case ex: IOException =>
            throw new RuntimeException("Unable to create filewriter", ex)
        }
      }
    }

    override def finish() {
      if (fw != null) {
        try {
          fw.close()
        } catch {
          case ex: IOException =>
            throw new RuntimeException("Unable to close filewriter", ex)
        }
      }
    }

    override def process(r: Row) {
      var doLog = false
      tm += 1

      if (logTimeInterval > 0) {
        if (tm % tn == 0) {
          val current = System.currentTimeMillis()
          if (current <= lastcheck + diff) tn += 1
          else if (tn > 1) tn -= 1
          lastcheck = current
          if ((current - lastlog) >= logTimeInterval * 1000) {
            doLog = true
            lastlog = current
          }
          tm = 0
        }
      }
      else {
        doLog = (m % logNthRow) == 0
      }

      if (doLog) {
        val max = Runtime.getRuntime.maxMemory()
        val total = Runtime.getRuntime.totalMemory()
        val free = Runtime.getRuntime.freeMemory()

        // This appears to be intentionally printing to System.err
        var logline = "Logger " + loggerName + " (" + m + ") mem (M): " + ((free + max - total) / 1000000).toInt + ", " + r
        if (label != "") logline += " - " + label
        if (level != null && level.length > 0) {
          if (!log(session, consoleLogger, level, logline)) throw new RuntimeException("Wrong logging level: " + level)
        } else {
          if (fw == null) System.err.println(logline)
          else {
            try {
              fw.write(logline + "\n")
            } catch {
              case ex: IOException =>
                try {
                  fw.close()
                } catch {
                  case ex: IOException =>
                    /*Ignore close error*/
                }
                throw new RuntimeException("Unable to write to file with filewriter", ex)
            }
          }
        }
      }
      m += 1
      super.process(r)
    }
  }

  def log(session: GorSession, log: Logger, level: String, logline: String): Boolean = {

    if (session.getSystemContext.getServer) return false
    else if ("warn".equalsIgnoreCase(level)) log.warn(logline)
    else if ("debug".equalsIgnoreCase(level)) log.debug(logline)
    else if ("info".equalsIgnoreCase(level)) log.info(logline)
    else if ("error".equalsIgnoreCase(level)) log.error(logline)
    else if ("trace".equalsIgnoreCase(level)) log.trace(logline)
    else return false
    true
  }
}