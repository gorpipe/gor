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

import gorsat.Analysis.AddFlankingSeqs
import gorsat.Commands.CommandParseUtilities._
import gorsat.Utilities.IteratorUtilities.validHeader
import org.gorpipe.gor.session.GorContext

import scala.collection.mutable.ListBuffer


class Seq extends CommandInfo("SEQ",
  CommandArguments("", "-c -l", 0, 0),
  CommandOptions(gorCommand = true, cancelCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {
    var flankLength = 10

    if (hasOption(args,"-l")) flankLength = intValueOfOptionWithRangeCheck(args,"-l",0)

    var rCols : List[Int] = List(1)

    val hcol = forcedInputHeader.split("\t")

    val columns = ListBuffer[ColumnHeader]()
    for (col <- hcol.indices) {
      // Outgoing header has all the columns from the original, with the same types as the original
      columns += ColumnHeader(hcol(col), col.toString)
    }

    if (hasOption(args, "-c")) {
      rCols = columnsOfOption(args, "-c", forcedInputHeader).distinct
      for (r <- rCols) columns += ColumnHeader("refseq_"+hcol(r), "S")
    } else {
      columns += ColumnHeader("refSeq", "S")
    }

    val header = RowHeader(columns)
    val pipeStep: Analysis = AddFlankingSeqs(context.getSession, flankLength, rCols.toArray, header)

    val combinedHeader = validHeader(header.toString)
    CommandParsingResult(pipeStep, combinedHeader)
  }
}
