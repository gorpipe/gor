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

import gorsat.Analysis.SegWhereAnalysis
import gorsat.Commands.CommandParseUtilities.{hasOption, intValueOfOptionWithDefaultWithRangeCheck}
import org.gorpipe.gor.session.GorContext

class SegWhere extends CommandInfo("SEGWHERE",
  CommandArguments("-sh -eh", "-minseg", 1, -1, ignoreIllegalArguments = true), CommandOptions(gorCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {

    val minseg = intValueOfOptionWithDefaultWithRangeCheck(args, "-minseg", -1, 1, Int.MaxValue)
    val startHalf = hasOption(args, "-sh")
    val endHalf = hasOption(args, "-eh")

    val header = "Chrom\tbpStart\tbpStop"

    CommandParsingResult(SegWhereAnalysis(context, minseg, startHalf, endHalf, iargs.mkString(" "), forcedInputHeader), header)
  }
}