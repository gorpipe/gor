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

import gorsat.Analysis.RowNumAnalysis
import gorsat.IteratorUtilities.validHeader
import org.gorpipe.gor.GorContext

class RowNum extends CommandInfo("ROWNUM",
  CommandArguments("", "", 0, 0),
  CommandOptions(gorCommand = true, norCommand = true)) {
  override def processArguments(context: GorContext, argString: String, iargs: Array[String],
                                args: Array[String], executeNor: Boolean, forcedInputHeader: String)
  : CommandParsingResult =
  {
    val incomingColumns = forcedInputHeader.split("\t").toList
    val outgoingColumns = incomingColumns ::: List("rownum")
    val outgoingTypes = incomingColumns.indices.map(x => x.toString).toList ::: List("I")
    val outgoingHeader = RowHeader(outgoingColumns.toArray, outgoingTypes.toArray)
    CommandParsingResult(RowNumAnalysis(outgoingHeader), validHeader(outgoingHeader.toString))
  }
}
