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
import gorsat.Commands.{CommandArguments, InputSourceInfo, InputSourceParsingResult}
import gorsat.process.GorJavaUtilities
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.{GorContext, GorSession}

object Sql {

  private def processAllArguments(session: GorSession, argString: String, iargs: Array[String],
                                  args: Array[String], isNorContext: Boolean): InputSourceParsingResult = {

    if (session.getSystemContext.getServer) {
      throw new GorParsingException("SQL input source is not allowed when running in server mode")
    }

    AnalysisUtilities.validateExternalSource(iargs(0))

    var myCommand = AnalysisUtilities.extractExternalSource(iargs(0))
    var chr:String = null
    var start = 0
    var end = -1

    if (hasOption(args, "-p")) {
      val range = rangeOfOption(args, "-p")
      chr = range.chromosome
      start = range.start
      end = range.stop
    }

    val filter = if (hasOption(args, "-f")) stringValueOfOption(args, "-f") else null
    val source = if (hasOption(args, "-s")) stringValueOfOption(args, "-s") else null

    val sPos = myCommand.indexOf("#(S:")
    if (sPos != -1) {
      val sEnd = myCommand.indexOf(')', sPos + 1)
      var seek = ""
      if (chr != null) {
        seek = myCommand.substring(sPos + 4, sEnd).replace("chr", chr)
        var pos = seek.indexOf("pos-end")
        if (pos != -1) seek = seek.replace("pos", (start + 1) + "").replace("end", end + "")
        else if (seek.contains("pos")) {
          pos = seek.indexOf("pos-")
          if (end == -1) seek = seek.replace("pos", start + "")
          else if (start == end && pos != -1) seek = seek.replace("pos-", start + "")
          else seek = seek.replace("pos", start + "-") + end
        }
      }
      myCommand = myCommand.substring(0, sPos) + seek + myCommand.substring(sEnd + 1, myCommand.length)
    }
    myCommand = GorJavaUtilities.projectReplacement(myCommand, session)
    val iteratorSource = GorJavaUtilities.getDbIteratorSource(myCommand, !isNorContext, source, false)

    InputSourceParsingResult(iteratorSource, "", isNorContext)
  }

  class Sql() extends InputSourceInfo("SQL", CommandArguments("-n", "-p", 1, 1)) {

    override def processArguments(context: GorContext, argString: String, iargs: Array[String],
                                  args: Array[String]): InputSourceParsingResult = {
      processAllArguments(context.getSession, argString, iargs, args, hasOption(args, "-n"))
    }
  }

  class GorSql() extends InputSourceInfo("GORSQL", CommandArguments("", "-p", 1, 1)) {

    override def processArguments(context: GorContext, argString: String, iargs: Array[String],
                                  args: Array[String]): InputSourceParsingResult = {
      processAllArguments(context.getSession, argString, iargs, args, isNorContext = false)
    }
  }

  class NorSql() extends InputSourceInfo("NORSQL", CommandArguments("", "-p", 1, 1), isNorCommand = true) {

    override def processArguments(context: GorContext, argString: String, iargs: Array[String],
                                  args: Array[String]): InputSourceParsingResult = {
      processAllArguments(context.getSession, argString, iargs, args, isNorContext = true)
    }
  }
}
