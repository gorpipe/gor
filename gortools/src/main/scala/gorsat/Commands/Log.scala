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

package gorsat.Commands

import java.nio.file.Paths

import gorsat.Analysis.LoggingAnalysis.LogAnalysis
import gorsat.Commands.CommandParseUtilities._
import gorsat.Monitors.MonitorLog
import gorsat.Utilities.StringUtilities
import org.gorpipe.gor.session.GorContext


class Log extends CommandInfo("LOG",
  CommandArguments("", "-t -a -l", 0, 1),
  CommandOptions(gorCommand = true, norCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {
    var logN = 1
    var time = 0
    val label = stringValueOfOptionWithDefault(args, "-l","")
    var logCount = 0

    logCount += 1
    val level = stringValueOfOptionWithDefault(args, "-a","")

    if (hasOption(args, "-t")) {
      time = intValueOfOptionWithRangeCheck(args, "-t", 0)
    } else if (iargs.length == 1) {
      logN = parseIntWithRangeCheck("logN", iargs(0), 0)
    }

    var filePath: String = null
    if (context.getSession.getProjectContext.getLogDirectory != null) {
      filePath = Paths.get(context.getSession.getProjectContext.getLogDirectory, StringUtilities.createMD5(argString) + ".log").toString
    }

    if (context.getSession.getSystemContext.getMonitor == null || !context.getSession.getSystemContext.getServer) {
      CommandParsingResult(LogAnalysis(context.getSession, logCount.toString, logN, time, filePath, level, label), forcedInputHeader)
    } else {
      CommandParsingResult(MonitorLog(logCount.toString, logN, context.getSession.getSystemContext.getMonitor), forcedInputHeader)
    }

    // TODO: We need to move this logging mechanism to the new logging framework
    /*else {
      val commandToExecute = argString
      var fileSignature = ""
      if (commandToExecute.startsWith("gordict")) {
        fileSignature = createMD5(usedFiles.mkString(" "))
      } else {
        val fileListKey = usedFiles.mkString(" ")
        fileSignature = createMD5(usedFiles.map(x => fileFingerPrint(x, gorPipeSession)).mkString(" "))
      }
      val querySignature = createMD5(commandToExecute + fileSignature)
      aPipeStep = GorLog(gorPipeSession, nc, paramString, combinedHeader, level, argString, querySignature)
    }*/
  }
}