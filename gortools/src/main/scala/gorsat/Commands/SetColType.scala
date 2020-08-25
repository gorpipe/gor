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

import gorsat.Analysis.SetColTypeAnalysis
import gorsat.Commands.CommandParseUtilities.columnsFromHeader
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.session.GorContext

class SetColType() extends CommandInfo(
  "SETCOLTYPE",
  CommandArguments("", "", 2, -1),
  CommandOptions(gorCommand = true, norCommand = true))
{
  val KNOWN_TYPES = List("I", "L", "D", "S", "INTEGER", "LONG", "DOUBLE", "STRING")

  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String],
                                executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {

    val columns = columnsFromHeader(iargs(0), forcedInputHeader, executeNor).toArray
    val types = iargs(1).toUpperCase.split(',')
    validateInputs(columns, types)

    val outgoingHeader = RowHeader(forcedInputHeader)
    for(i <- columns.indices) {
      outgoingHeader.columnTypes(columns(i)) = types(i).substring(0, 1)
    }
    CommandParsingResult(SetColTypeAnalysis(outgoingHeader), forcedInputHeader)
  }

  private def validateInputs(columns: Array[Int], types: Array[String]): Unit = {
    if (columns.length > types.length) {
      throw new GorParsingException("SETCOLTYPE: Fewer types than columns given")
    }
    if (columns.length < types.length) {
      throw new GorParsingException("SETCOLTYPE: Too many types for columns given")
    }
    types.foreach(t => if (!KNOWN_TYPES.contains(t)) {
      throw new GorParsingException(s"SETCOLTYPE: Unknown type $t")
    })
    columns.foreach(c => if(c < 2) {
      throw new GorParsingException("SETCOLTYPE: Setting type of Chrom or Pos column is not allowed")
    })
  }
}
