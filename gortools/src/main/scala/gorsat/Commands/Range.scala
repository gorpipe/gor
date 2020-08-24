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

import gorsat.Analysis.InRange
import gorsat.Commands.CommandParseUtilities._
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.GorContext


class Range extends CommandInfo("RANGE",
  CommandArguments("", "", 1, -1, ignoreIllegalArguments = true),
  CommandOptions())
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {
    var stopChr = "chr1"
    var stopPos = -1
    var startChr = "chr1"
    var startPos = -1
    try {
      val range = args.mkString(" ").replace(",", "").replace(" ", "")
      val rcol = range.split("[:|-]")
      if (rcol.nonEmpty) {
        startChr = rcol(0)
        stopChr = startChr
      }
      if (rcol.length > 1) {
        startPos = parseIntWithRangeCheck("startPos", rcol(1))
        if (!range.endsWith("-")) stopPos = parseIntWithRangeCheck("stopPos", rcol(1))
      }
      if (rcol.length > 2) stopPos = parseIntWithRangeCheck("stopPos", rcol(2))
      if (startPos < 0) startPos = 1
      if (stopPos < 0) stopPos = startPos
    } catch {
      case _ : Throwable => throw new GorParsingException(s"Error in stop position - specify the stop position ($stopPos) as chrX:bpStart-bpStop: ")
    }

    CommandParsingResult(InRange(startChr, startPos, stopChr, stopPos), forcedInputHeader)
  }
}
