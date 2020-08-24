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

import gorsat.Analysis.UpToAnalysis
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.session.GorContext

class UpTo extends CommandInfo("UPTO",
  CommandArguments("", "", 1, -1, ignoreIllegalArguments = true),
  CommandOptions(gorCommand = true, verifyCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {
    var stopChr = "chr1"
    var stopPos = 1000
    try {
      val range = iargs.mkString(" ").replace(",", "").replace(" ", "")
      val rcol = range.split(':')
      stopChr = rcol(0)
      stopPos = rcol(1).toInt
    } catch {
      case _: Exception => throw new GorParsingException(s"Error in stop position - Specify the stop position ($stopPos) as chrX:bpPos.")
    }

    CommandParsingResult(UpToAnalysis(stopChr, stopPos), forcedInputHeader)
  }
}