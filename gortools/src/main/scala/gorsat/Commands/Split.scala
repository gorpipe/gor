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

import gorsat.Analysis.{ColumnSplit, MultiColumnSplit}
import gorsat.Commands.CommandParseUtilities._
import gorsat.Utilities.IteratorUtilities.validHeader
import org.gorpipe.gor.session.GorContext

class Split extends CommandInfo("SPLIT",
  CommandArguments("", "-s -e", 1, 1),
  CommandOptions(gorCommand = true, norCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {

    val inputHeader = forcedInputHeader
    val allArgs = iargs(0)
    val useCols = columnsFromHeader(allArgs,inputHeader,executeNor)
    val splitPattern = replaceSingleQuotes(stringValueOfOptionWithDefault(args,"-s",","))

    var combinedHeader = inputHeader

    combinedHeader = validHeader(combinedHeader)

    val colNum = combinedHeader.split("\t").length

    val emptyString = replaceSingleQuotes(stringValueOfOptionWithDefault(args,"-e",""))

    if (hasOption(args,"-h")) combinedHeader = null

    var pipeStep: Analysis = null

    if (useCols.length == 1) {
      pipeStep = ColumnSplit(colNum,useCols.head,splitPattern)
    } else {
      pipeStep = MultiColumnSplit(colNum,useCols.toArray,splitPattern,emptyString)
    }

    CommandParsingResult(pipeStep, combinedHeader)
  }
}
