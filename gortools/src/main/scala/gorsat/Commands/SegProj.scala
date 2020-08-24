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

import gorsat.Analysis.SegProjAnalysis
import gorsat.Commands.CommandParseUtilities._
import gorsat.IteratorUtilities.validHeader
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.GorContext

import scala.collection.mutable.ListBuffer

class SegProj extends CommandInfo("SEGPROJ",
  CommandArguments("", "-f -maxseg -gc -sumcol", 0, 0),
  CommandOptions(gorCommand = true, verifyCommand = true)) {
  override def processArguments(context: GorContext, argString: String, iargs: Array[String],
                                args: Array[String], executeNor: Boolean, forcedInputHeader: String)
  : CommandParsingResult = {
    var pipeStep: Analysis = null
    var combinedHeader = ""

    var gcCols: List[Int] = Nil
    gcCols = if (hasOption(args, "-gc")) {
      columnsOfOption(args, "-gc", forcedInputHeader).distinct
    } else {
      Nil
    }

    val fuzzfac = if (hasOption(args, "-f")) {
      intValueOfOptionWithRangeCheck(args, "-f", 0)
    } else {
      0
    }
    val maxseg = if (hasOption(args, "-maxseg")) {
      intValueOfOptionWithRangeCheck(args, "-maxseg", 1) + fuzzfac * 2
    } else {
      4000000 + fuzzfac * 2
    }
    var sumColumns: List[Int] = Nil
    sumColumns = if (hasOption(args, "-sumcol")) {
      columnsOfOptionWithValidation(args, "-sumcol", forcedInputHeader,
        executeNor, 1, 1).distinct
    } else {
      Nil
    }

    if (sumColumns != Nil && sumColumns.length > 1) {
      throw new GorParsingException("Error in # of sumcol columns - can only be one sumcol column specified: ")
    }


    val hcol = forcedInputHeader.split("\t")

    val columns = ListBuffer[ColumnHeader]()
    columns += ColumnHeader(hcol(0), "S")
    columns += ColumnHeader(hcol(1), "I")
    columns += ColumnHeader(hcol(2), "I")
    for (col <- gcCols) {
      columns += ColumnHeader(hcol(col), col.toString)
    }
    columns += ColumnHeader("segCount", "I")

    val header = RowHeader(columns)
    combinedHeader = validHeader(header.toString)

    pipeStep = SegProjAnalysis(gcCols, maxseg, fuzzfac, sumColumns, forcedInputHeader, context.getSession, header)

    CommandParsingResult(pipeStep, combinedHeader)
  }
}
