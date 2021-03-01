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

package gorsat.Commands

import gorsat.Analysis.ValidateColumnsAnalysis
import gorsat.Commands.CommandParseUtilities._
import org.gorpipe.gor.session.GorContext

class ValidateColumns extends CommandInfo("VALIDATECOLUMNS",
  CommandArguments("", "-n", 0, 1),
  CommandOptions(gorCommand = true, norCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {
    var message = "unknown"
    val testNthRow = intValueOfOptionWithDefaultWithRangeCheck(args, "-n", 10000, 1)
    if (iargs.length > 0) message = iargs(0)

    val headerCount = forcedInputHeader.split("\t").length

    CommandParsingResult(ValidateColumnsAnalysis(headerCount, message, testNthRow), forcedInputHeader)
  }
}
