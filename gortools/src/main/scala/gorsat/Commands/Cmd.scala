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

import java.util.Optional

import gorsat.AnalysisUtilities
import gorsat.Commands.CommandParseUtilities._
import gorsat.process.ProcessIteratorAdaptor
import org.gorpipe.gor.GorContext
import org.gorpipe.model.gor.Utilities.makeTempFile
import org.gorpipe.model.gor.iterators.RowSource

object Cmd {

  class Cmd extends CommandInfo("CMD",
    CommandArguments("-u -h -e", "-s -f", 1, 1),
    CommandOptions(gorCommand = true, norCommand = true)) {
    override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String, commandRuntime: CommandRuntime): CommandParsingResult = {

      if (context.getSession.getSystemContext.getServer) {
        return CommandParsingResult(null, null)
      }

      AnalysisUtilities.validateExternalSource(iargs(0))

      var server = hasOption(args, "-u")
      var skipheader = hasOption(args, "-h")
      var skip: Optional[String] = if (hasOption(args, "-s")) {
        Optional.of(stringValueOfOption(args, "-s"))
      } else Optional.empty()
      var allowerror = hasOption(args, "-e")

      var paramString = AnalysisUtilities.extractExternalSource(iargs(0))
      val trim = paramString.trim
      var k = trim.indexOf(" ")
      if (k == -1) k = trim.length
      val command = trim.substring(0, k)
      if (command.contains(".yml ") || command.contains(".yml(") || command.contains(".yml?") || command.contains(".yml:")) {
        var qr = context.getSession.getSystemContext.getReportBuilder.parse(command)
        if (qr.startsWith("#!")) {
          qr = makeTempFile(qr, commandRuntime.cacheDir)
        }
        paramString = qr + trim.substring(k)
      }

      val filter = if (hasOption(args, "-f")) stringValueOfOption(args, "-f") else null

      val pip = new ProcessIteratorAdaptor(context, paramString, "cmd", commandRuntime.inputSource, commandRuntime.activePipeStep, forcedInputHeader, skipheader, skip, allowerror, executeNor)

      CommandParsingResult(null, pip.getHeader, null, pip.asInstanceOf[RowSource])
    }
  }
}