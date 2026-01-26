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

import gorsat.Analysis.ReplaceAnalysis
import gorsat.Commands.CommandParseUtilities._
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.session.GorContext


class Replace extends CommandInfo("REPLACE",
  CommandArguments("", "", 2, -1, true),
  CommandOptions(gorCommand = true, norCommand = true)) {
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String],
                                executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {
    val colName = iargs(0)
    val formula = iargs.slice(1, args.length).mkString(" ")
    val colNums = columnsFromHeader(colName, forcedInputHeader, executeNor).toArray

    val filteredColNums = colNums.filter(i => i>= 2)
    if(!colNums.isEmpty && filteredColNums.isEmpty) {
      throw new GorParsingException("REPLACE is not allowed on Chrom/Pos columns")
    }
    CommandParsingResult(ReplaceAnalysis(context, executeNor, formula, forcedInputHeader, filteredColNums), forcedInputHeader)
  }

}
