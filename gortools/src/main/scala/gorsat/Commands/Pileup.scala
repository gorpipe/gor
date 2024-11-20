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

import gorsat.Analysis.GorPileup._
import gorsat.Analysis.InRange
import gorsat.Commands.CommandParseUtilities._
import gorsat.Utilities.IteratorUtilities.validHeader
import org.gorpipe.exceptions.GorDataException
import org.gorpipe.gor.session.GorContext

class Pileup extends CommandInfo("PILEUP",
  CommandArguments(" -nf -df -sex -gt -depth -soc", "-p -i -q -bq -gc -mprob -span", 0, 0),
  CommandOptions(gorCommand = true, verifyCommand = true, cancelCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {
    var startChr = "chr1"
    var stopChr = "chrY"
    var startPos = 0
    var stopPos = 250000000
    var rangeSpecified = false
    val columns = PileupColumns()

    if (hasOption(args,"-p")) {
      val range = stringValueOfOption(args,"-p").replace(",","").replace(" ","")
      val rcol = range.split("[:|-]")
      if (rcol.nonEmpty) { startChr = rcol(0); stopChr = startChr }
      if (rcol.length>1) { startPos = parseIntWithRangeCheck("startPos", rcol(1)); if (!range.endsWith("-")) stopPos = parseIntWithRangeCheck("stopPos", rcol(1)) }
      if (rcol.length>2) stopPos = parseIntWithRangeCheck("stopPos", rcol(2))
      if (startPos<0) startPos = 0
      rangeSpecified = true
    }

    var maxReadSpan = 1000
    if (hasOption(args,"-span")) maxReadSpan = 1000.max(intValueOfOptionWithRangeCheck(args,"-span", 0))

    val pa = Parameters()

    if (hasOption(args,"-i")) pa.maxInsert = intValueOfOptionWithRangeCheck(args,"-i",0)
    if (hasOption(args,"-q")) pa.minQual = intValueOfOptionWithRangeCheck(args,"-q", 0, 200)
    if (hasOption(args,"-bq")) pa.minBaseQual = intValueOfOptionWithRangeCheck(args,"-bq", 0, 200)
    if (hasOption(args,"-nf")) pa.noFilter = true
    if (hasOption(args,"-df")) pa.noFilter = false

    if (hasOption(args,"-mprob")) pa.nonReferenceProb = doubleValueOfOptionWithRangeCheck(args,"-mprob", 0.0)
    if (hasOption(args,"-sex")) pa.numGTs = 4
    if (hasOption(args,"-gt")) pa.callSnps = true
    if (hasOption(args,"-soc")) pa.singleOverlapCount = true

    if (hasOption(args,"-depth")) { pa.depthOnly = true; pa.callSnps = false }

    val inputHeader = forcedInputHeader

    try {
      val useCol = columnsFromHeader("iSize,Seq,Flag,MapQ,Cigar,mrnm,mpos", inputHeader)
      columns.iSizeCol = useCol.head
      columns.seqBasesCol = useCol(1)
      columns.flagCol = useCol(2)
      columns.qualityCol = useCol(3)
      columns.cigarCol = useCol(4)
      columns.mrnmCol = useCol(5)
      columns.mPosCol = useCol(6)
    } catch {
      case e : Exception => throw new GorDataException("Necessary BAM columns not found:\n"+e.getMessage, e)
    }

    try {
      val useCol = columnsFromHeader("Qual",inputHeader)
      columns.baseQualCol = useCol.head
    } catch {
      case e : Exception => throw new GorDataException("The BAM Qual column is not found:\n"+e.getMessage, e)
    }

    val gcCols = columnsOfOptionWithNil(args,"-gc",inputHeader).distinct
    val hcol = inputHeader.split("\t")
    var combinedHeader = "Chrom\tPos"
    for (c <- gcCols) combinedHeader += "\t"+hcol(c)

    if (pa.depthOnly) combinedHeader = validHeader(combinedHeader+"\tRefBase\tDepth")
    else combinedHeader = validHeader(combinedHeader+"\tRefBase\tMajorAllele\tSecondAllele\tChi\tDepth\tAdepth\tCdepth\tGdepth\tTdepth\tDels\tIns")

    if (pa.callSnps) combinedHeader += "\tGT\tpGT\tLOD\tGT2\tSNP"

    if (!pa.depthOnly) {
      try {
        val useCol = columnsFromHeader("Chi,Dels,Ins", combinedHeader)
        columns.ChiCol = useCol.head
        columns.DelsCol = useCol(1)
        columns.InsCol = useCol(2)
      } catch {
        case e: Exception => throw new GorDataException("The columns Chi, Dels and Ins are not found in the output header!", e)
      }
    }

    if (hasOption(args,"-h")) combinedHeader = null

    var pipeStep: Analysis = pooledPileup(context.getSession, gcCols,pa, columns,maxReadSpan, context.getSession.getProjectContext.createRefSeq())
    pipeStep = InRange(startChr,startPos,stopChr,stopPos) | pipeStep

    CommandParsingResult(pipeStep, combinedHeader)
  }
}
