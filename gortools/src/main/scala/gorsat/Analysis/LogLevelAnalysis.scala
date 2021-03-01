/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
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
import org.slf4j.{LoggerFactory, MDC}

case class LogLevelAnalysis(loggerName: String, level: String, label: String, fromMain: Boolean) extends Analysis {

  var oldConsoleLogLevel : ch.qos.logback.classic.Level = _
  var labelName = loggerName + ":loglabel"

  override def setup(): Unit = {

    if (label == null || label == "") {
      MDC.remove(labelName)
    } else {
      MDC.put(labelName, label)
    }

    val logLevel = ch.qos.logback.classic.Level.toLevel(level)
    val mainLogger = LoggerFactory.getLogger(loggerName).asInstanceOf[ch.qos.logback.classic.Logger]

    oldConsoleLogLevel = mainLogger.getEffectiveLevel
    mainLogger.setLevel(logLevel)
  }

  override def finish: Unit = {
    if (oldConsoleLogLevel != null && !fromMain) {
      val mainLogger = LoggerFactory.getLogger(loggerName).asInstanceOf[ch.qos.logback.classic.Logger]
      mainLogger.setLevel(oldConsoleLogLevel)
      MDC.remove(labelName)
    }
  }
}
