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

package gorsat.InputSources

import gorsat.AnalysisUtilities
import gorsat.Commands.CommandParseUtilities._
import gorsat.Commands._
import gorsat.process.{GorJavaUtilities, ProcessRowSource}
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.{GorContext, GorSession}

object Cmd {

  private def processAllArguments(session: GorSession, argString: String, iargs: Array[String],
                                  args: Array[String], isNorContext: Boolean): InputSourceParsingResult = {

    if (session.getSystemContext.getServer) {
      throw new GorParsingException("CMD input source is not allowed when running in server mode")
    }

    AnalysisUtilities.validateExternalSource(iargs(0))

    val useCommandServer = hasOption(args, "-u")
    val commandType = stringValueOfOptionWithDefaultWithErrorCheck(args, "-s", "gor", Array("gor", "vcf", "bam"))
    val bufferSize = intValueOfOptionWithDefault(args, "-b", -1)

    val range = if (hasOption(args, "-p")) rangeOfOption(args, "-p") else GenomicRange.empty
    var command = AnalysisUtilities.extractExternalSource(iargs(0))

    if (command.contains(".yml?")) {
      var e = command.indexOf(' ')
      if (e == -1) e = command.length
      val qr = session.getSystemContext.getReportBuilder.parse(command.substring(0, e))
      command = qr + command.substring(e)
    }
    else if (command.contains(".yml")) {
      val e = command.indexOf(')')
      if (e == -1) command = session.getSystemContext.getReportBuilder.parse(command)
      else command = session.getSystemContext.getReportBuilder.parse(command.substring(0, e + 1)) + command.substring(e + 1)
    }

    val projectRoot = session.getProjectContext.getRealProjectRoot
    command = GorJavaUtilities.projectReplacement(command, session)

    val filter: String = if (hasOption(args, "-f")) stringValueOfOption(args, "-f") else null

    val inputSource = {
      if (bufferSize != -1) new ProcessRowSource(command, commandType, isNorContext, session, range, filter, bufferSize)
      else new ProcessRowSource(command, commandType, isNorContext, session, range, filter)
    }

    InputSourceParsingResult(inputSource, "", isNorContext)
  }


  class Cmd() extends InputSourceInfo("CMD", CommandArguments("-n -u", "-p -s -b -f", 1, 1)) {

    override def processArguments(context: GorContext, argString: String, iargs: Array[String],
                                  args: Array[String]): InputSourceParsingResult = {

      processAllArguments(context.getSession, argString, iargs, args, hasOption(args, "-n"))
    }
  }

  class GorCmd() extends InputSourceInfo("GORCMD", CommandArguments("-u", "-p -s -b -f", 1, 1)) {

    override def processArguments(context: GorContext, argString: String, iargs: Array[String],
                                  args: Array[String]): InputSourceParsingResult = {

      processAllArguments(context.getSession, argString, iargs, args, isNorContext = false)
    }
  }

  class NorCmd() extends InputSourceInfo("NORCMD", CommandArguments("-u", "-p -s -b -f", 1, 1), isNorCommand = true) {

    override def processArguments(context: GorContext, argString: String, iargs: Array[String],
                                  args: Array[String]): InputSourceParsingResult = {

      processAllArguments(context.getSession, argString, iargs, args, isNorContext = true)
    }
  }

}
