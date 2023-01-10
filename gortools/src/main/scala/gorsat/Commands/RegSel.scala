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

import gorsat.Analysis.RegSelAnalysis
import gorsat.Commands.CommandParseUtilities._
import org.gorpipe.gor.session.GorContext

class RegSel extends CommandInfo(
  "REGSEL",
  CommandArguments("", "-e", 3, 3),
  CommandOptions(gorCommand = true, norCommand = true)
)
{
  override def processArguments(
                                 context: GorContext,
                                 argString: String,
                                 inputArguments: Array[String],
                                 options: Array[String],
                                 executeNor: Boolean,
                                 forcedInputHeader: String
   ): CommandParsingResult =
  {
    val columns = inputArguments(0).split(',').toSeq
    val source = columnFromHeader(inputArguments(1), forcedInputHeader, executeNor)
    val pattern = replaceSingleQuotes(inputArguments(2))
    val emptyString = replaceSingleQuotes(stringValueOfOptionWithDefault(options,"-e",""))

    val incomingColumns = forcedInputHeader.split("\t").toList
    val outgoingColumns = incomingColumns ::: columns.toList
    val outgoingTypes = incomingColumns.indices.map(x => x.toString).toList ::: columns.map( _ => "S").toList
    val outgoingHeader = RowHeader(outgoingColumns.toArray, outgoingTypes.toArray)

    val pipeStep: Analysis = RegSelAnalysis(pattern, source, columns, emptyString, outgoingHeader)

    val newHeader = outgoingHeader.toString
    CommandParsingResult(pipeStep, newHeader)
  }
}
