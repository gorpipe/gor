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

import gorsat.Analysis.VarCountState.VarCountAggregate
import gorsat.Analysis.{CigarVarSegs, PhaseReadVariants, SortAnalysis}
import gorsat.Commands.CommandParseUtilities._
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.session.GorContext

class Variants extends CommandInfo("VARIANTS",
  CommandArguments("-count", "-gc -readlength -bpmergedist", 0, 0),
  CommandOptions(gorCommand = true, memoryMonitorCommand = true, verifyCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {
    var seqBasesCol = -1
    var seqQualCol = -1

    var useRef = false
    var useSeq = false
    var useQual = false
    val sortWindow = intValueOfOptionWithDefault(args, "-readlength",990) + 10
    val maxBpMergeDist = intValueOfOptionWithDefault(args, "-bpmergedist",3)
    val cigarCol = columnFromHeader("CIGAR", forcedInputHeader, executeNor)
    val useVarAggr = hasOption(args, "-count")
    val gcCols = columnsOfOptionWithNil(args, "-gc", forcedInputHeader).distinct

    useRef = true
    useSeq = true
    useQual = true

    if (useQual) seqQualCol = columnFromHeader("QUAL", forcedInputHeader, executeNor)
    if (useSeq) seqBasesCol = columnFromHeader("SEQ", forcedInputHeader, executeNor)


    val hcol = forcedInputHeader.split("\t")
    var header = "Chrom\tPos\tRef\tAlt\tReadPos\tBaseQual\tID"

    for (c <- gcCols) header += "\t" + hcol(c)

    var pipeStep: Analysis = CigarVarSegs(cigarCol, gcCols.toArray, useRef, outputBases = false, seqBasesCol, seqQualCol, context.getSession)

    if (maxBpMergeDist > 0) {
      pipeStep = pipeStep | PhaseReadVariants(maxBpMergeDist, context.getSession)
    }
    if (useVarAggr) {
      val allcols = header.split("\t").length
      try {
        val theGrCols = Range(0, allcols).slice(2, 4).toList ::: Range(0, allcols).slice(7, allcols).toList
        pipeStep = pipeStep | VarCountAggregate(theGrCols, sortWindow)
      } catch {
        case _:Throwable => throw new GorParsingException(s"Error in group columns - number of columns is incorrect: $allcols")
      }

      if (useVarAggr) {
        header = "Chrom\tPos\tRef\tAlt"
        for (c <- gcCols) header += "\t" + hcol(c)
        header += "\tvarCount"
      }

    } else {
      pipeStep = pipeStep | SortAnalysis(header, context.getSession, sortWindow)
    }

    CommandParsingResult(pipeStep, header)
  }
}
