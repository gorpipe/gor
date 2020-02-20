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

import gorsat.Analysis.UnPivot2
import gorsat.Commands.CommandParseUtilities._
import gorsat.IteratorUtilities.validHeader
import org.gorpipe.gor.GorContext

class UnPivot extends CommandInfo("UNPIVOT",
  CommandArguments("", "", 1, 1),
  CommandOptions(gorCommand = true, norCommand = true, verifyCommand = true)) {
  override def processArguments(context: GorContext, argString: String, iargs: Array[String],
                                args: Array[String], executeNor: Boolean, forcedInputHeader: String)
  : CommandParsingResult =
  {
    val colNames = args(0)
    val unPivCols = columnsFromHeader(colNames, forcedInputHeader, executeNor) filterNot (List(0, 1) contains)
    val unPivColsAsSet = unPivCols.toSet

    val incomingColumns = forcedInputHeader.split("\t")
    val allColumns = columnsFromHeader(forcedInputHeader.replace("\t", ","), forcedInputHeader, executeNor)
    val remainingColumns = allColumns filterNot (unPivColsAsSet contains)

    val existingOutgoingColumns = remainingColumns.map(x => incomingColumns(x))
    val existingOutgoingTypes = remainingColumns.map(x => x.toString)

    val outgoingColumns = existingOutgoingColumns ::: List("Col_Name", "Col_Value")
    val outgoingTypes = existingOutgoingTypes ::: List("S", "S")
    val oldHeader = forcedInputHeader
    val outgoingHeader = RowHeader(outgoingColumns.toArray, outgoingTypes.toArray)

    val pipeStep = UnPivot2(oldHeader, unPivCols, remainingColumns, outgoingHeader)
    CommandParsingResult(pipeStep, validHeader(outgoingHeader.toString))
  }

}
