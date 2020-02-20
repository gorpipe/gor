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

import gorsat.Analysis.DistLocAnalysis
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.GorContext

class DistLoc extends CommandInfo("DISTLOC",
  CommandArguments("", "", 0, 1),
  CommandOptions(gorCommand = true, verifyCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {
    var TopNv = 1
    try {
      if (iargs.length > 0) {
        TopNv = iargs(0).toInt
      }
    } catch {
      case e: Exception => throw new GorParsingException("Error in input to distloc - invalid input to distloc: " + iargs(0))
    }

    if (TopNv < 1) throw new GorParsingException("Error in input - input cannot be less than 1.")

    CommandParsingResult(DistLocAnalysis(TopNv), forcedInputHeader)
  }
}