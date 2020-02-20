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

import gorsat.Analysis.{CigarVarSegs, SortAnalysis}
import gorsat.Commands.CommandParseUtilities._
import org.gorpipe.gor.GorContext

class Bases extends CommandInfo("BASES",
  CommandArguments("-count", "-gc -readlength", 0, 0),
  CommandOptions(gorCommand = true, verifyCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {
    var seqBasesCol = -1
    var seqQualCol = -1

    val useRef = true
    val useSeq = true
    val useQual = true
    val sortWindow = intValueOfOptionWithDefaultWithRangeCheck(args, "-readlength",990, 1, 1000) + 10

    val cigarCol = columnFromHeader("CIGAR", forcedInputHeader, executeNor)
    val gcCols = columnsOfOptionWithNil(args, "-gc", forcedInputHeader).distinct

    if (useQual) seqQualCol = columnFromHeader("QUAL", forcedInputHeader, executeNor)
    if (useSeq) seqBasesCol = columnFromHeader("SEQ", forcedInputHeader, executeNor)

    val hcol = forcedInputHeader.split("\t")
    var header = "Chrom\tPos\tRef\tBase\tReadPos\tBaseQual\tMDI"
    for (c <- gcCols) header += "\t" + hcol(c)
    val pipeStep: Analysis = CigarVarSegs(cigarCol, gcCols.toArray, useRef, outputBases = true, seqBasesCol, seqQualCol, context.getSession) | SortAnalysis(forcedInputHeader, context.getSession, sortWindow)

    CommandParsingResult(pipeStep, header)
  }
}
