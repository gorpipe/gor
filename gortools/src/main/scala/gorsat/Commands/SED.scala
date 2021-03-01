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

import gorsat.Analysis.ColSed
import gorsat.Commands.CommandParseUtilities._
import org.gorpipe.gor.session.GorContext

class SED extends CommandInfo("SED",
  CommandArguments("-i -f", "-c", 2, 2),
  CommandOptions(gorCommand = true, norCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {

    val caseInsensitive = hasOption(args, "-i")
    val replaceAll = !hasOption(args, "-f")
    val matchPattern = replaceSingleQuotes(iargs(0))
    val replacePattern = replaceSingleQuotes(iargs(1))
    var useAllCols = true
    var mCols: List[Int] = Nil

    if (hasOption(args, "-c")) {
      useAllCols = false
      mCols = columnsOfOption(args, "-c", forcedInputHeader, executeNor)
    }

    val numCols = forcedInputHeader.split("\t").length

    val replaceCols = new Array[Boolean](numCols)
    for (i <- 0 until numCols) if (mCols.indexWhere(x => {
      x == i
    }) >= 0 || useAllCols) replaceCols(i) = true else replaceCols(i) = false

    val pipeStep: Analysis = ColSed(matchPattern, replacePattern, replaceCols, caseInsensitive, replaceAll)

    CommandParsingResult(pipeStep, forcedInputHeader)
  }
}
