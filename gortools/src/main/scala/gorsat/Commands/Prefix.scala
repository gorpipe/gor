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

import gorsat.Commands.CommandParseUtilities._
import gorsat.IteratorUtilities.validHeader
import org.gorpipe.gor.GorContext

class Prefix extends CommandInfo("PREFIX",
  CommandArguments("", "", 2),
  CommandOptions(gorCommand = true, norCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {
    val colName = iargs(0)
    val prefix = iargs(1)
    val colNums = columnsFromHeader(colName, forcedInputHeader, executeNor).toSet

    var cnum = 0
    val oCols = forcedInputHeader.split("\t")
    val nCols = oCols.map(cn => {
      cnum += 1
      if (cnum > 2 && colNums.contains(cnum - 1)) prefix + "_" + cn else cn
    })

    CommandParsingResult(null, validHeader(nCols.mkString("\t")))
  }

}
