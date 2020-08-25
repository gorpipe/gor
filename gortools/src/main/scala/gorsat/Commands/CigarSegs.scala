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

import gorsat.Analysis.{CigarSegsAnalysis, CigarVarSegs, SortAnalysis}
import gorsat.Commands.CommandParseUtilities._
import org.gorpipe.gor.session.GorContext

class CigarSegs extends CommandInfo("CIGARSEGS",
  CommandArguments("-seq", "-gc -readlength", 0),
  CommandOptions(gorCommand = true, verifyCommand = true)) {

  override def processArguments(context: GorContext,
                                argString: String,
                                iargs: Array[String],
                                args: Array[String],
                                executeNor: Boolean,
                                forcedInputHeader: String): CommandParsingResult = {
    var seqBasesCol = -1
    var seqQualCol = -1
    var useSeq = false
    val sortWindow = intValueOfOptionWithDefaultWithRangeCheck(args, "-readlength", 990, 1, 1000) + 10

    val cigarCol = columnFromHeader("CIGAR", forcedInputHeader, executeNor)
    val gcCols = columnsOfOptionWithNil(args, "-gc", forcedInputHeader).distinct

    useSeq = hasOption(args, "-seq")

    if (useSeq) seqBasesCol = columnFromHeader("SEQ", forcedInputHeader, executeNor)

    var header = if (useSeq) "Chrom\tPos\tSeq\tID" else "Chrom\tbpStart\tbpStop\tID"
    val hcol = forcedInputHeader.split("\t")
    for (c <- gcCols) header += "\t" + hcol(c)

    var pipeStep: Analysis = null
    if (useSeq) {
      seqQualCol = seqBasesCol
      pipeStep = CigarVarSegs(cigarCol, gcCols.toArray, useRef = false, outputBases = false, seqBasesCol, seqQualCol, context.getSession) | SortAnalysis(forcedInputHeader, context.getSession, sortWindow)
    } else {
      pipeStep = CigarSegsAnalysis(cigarCol, gcCols.toArray) | SortAnalysis(forcedInputHeader, context.getSession, sortWindow)
    }

    CommandParsingResult(pipeStep, header)
  }
}
