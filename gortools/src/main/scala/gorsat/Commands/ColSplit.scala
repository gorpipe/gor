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

import gorsat.Analysis.ColSplitAnalysis
import gorsat.Commands.CommandParseUtilities._
import org.gorpipe.gor.session.GorContext

class ColSplit extends CommandInfo("COLSPLIT",
  CommandArguments("-o", "-s -m", 3, 3),
  CommandOptions(gorCommand = true, norCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {

    val sepVal = replaceSingleQuotes(stringValueOfOptionWithDefault(args, "-s", ","))
    val missVal = replaceSingleQuotes(stringValueOfOptionWithDefault(args, "-m",""))

    val splitCol = columnFromHeader(iargs(0), forcedInputHeader, executeNor)
    val colNum = parseIntWithRangeCheck("colnum", iargs(1), 2, 100)
    val prefix = iargs(2)

    val writeCols = hasOption(args, "-o")

    val existingColumns = forcedInputHeader.split("\t").toList
    val colsColumn = if(writeCols) List(s"${prefix}_Cols") else List()
    val newColumns = Range(1, colNum + 1).map(x => prefix + "_" + x).toList

    val outgoingColumns = existingColumns ::: colsColumn ::: newColumns
    val outgoingTypes = existingColumns.indices.map(x => x.toString).toList :::
      colsColumn.map(_ => "I") ::: newColumns.map( _ => "S")

    val outgoingHeader = RowHeader(outgoingColumns.toArray, outgoingTypes.toArray)

    val combinedHeader = outgoingHeader.toString

    val pipeStep: Analysis = ColSplitAnalysis(splitCol, colNum, sepVal, missVal, writeCols, outgoingHeader)

    CommandParsingResult(pipeStep, combinedHeader)
  }
}