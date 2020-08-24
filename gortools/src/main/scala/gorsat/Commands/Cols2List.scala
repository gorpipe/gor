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
import gorsat.Analysis.Cols2ListAnalysis
import gorsat.Commands.CommandParseUtilities.{replaceSingleQuotes, stringValueOfOptionWithDefault}
import org.gorpipe.gor.GorContext

class Cols2List extends CommandInfo(
  "COLS2LIST",
  CommandArguments("", "-sep -gc -map", 2, 2),
  CommandOptions(gorCommand = true, norCommand = true)
)
{
  override def processArguments(
                                 context: GorContext,
                                 argString: String,
                                 inputArguments: Array[String],
                                 options: Array[String],
                                 executeNor: Boolean,
                                 forcedInputHeader: String): CommandParsingResult =
  {
    val selectionCriteria = inputArguments(0)
    val newColumn = inputArguments(1)

    val sepVal = replaceSingleQuotes(stringValueOfOptionWithDefault(options, "-sep", ","))
    val inclusionCriteria = stringValueOfOptionWithDefault(options, "-gc", "")
    val mapOptionValue = stringValueOfOptionWithDefault(options, "-map", "")
    val mapExpression = replaceSingleQuotes(mapOptionValue)

    val columns = ColumnSelection(forcedInputHeader, selectionCriteria, context, forNor = executeNor)
    val included = ColumnSelection(forcedInputHeader, inclusionCriteria, context, forNor = executeNor)
    included.addPosColumns()

    val incomingHeader = RowHeader(forcedInputHeader)
    val pipeStep: Analysis = Cols2ListAnalysis(columns, included, sepVal, executeNor, mapExpression, incomingHeader)
    val prefix = if(included.isEmpty) "" else included.header + "\t"

    val newHeader = prefix + newColumn
    pipeStep.setRowHeader(incomingHeader)

    CommandParsingResult(pipeStep, newHeader)
  }
}
