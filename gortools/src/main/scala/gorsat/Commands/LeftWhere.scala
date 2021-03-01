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

import gorsat.Analysis.LeftWhereAnalysis
import gorsat.Commands.CommandParseUtilities._
import org.gorpipe.gor.session.GorContext

class LeftWhere extends CommandInfo("LEFTWHERE",
  CommandArguments("", "-e", 2, -1, ignoreIllegalArguments = true),
  CommandOptions(gorCommand = true, verifyCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {
    val empty = replaceSingleQuotes(stringValueOfOptionWithDefault(args, "-e",""))

    val colName = iargs(0)
    val formula = iargs.slice(1, iargs.length).mkString(" ")
    val colNum = columnFromHeader(colName, forcedInputHeader, executeNor)

    CommandParsingResult(LeftWhereAnalysis(context.getSession, executeNor, formula, forcedInputHeader, colNum, empty, argString), forcedInputHeader)
  }

}

