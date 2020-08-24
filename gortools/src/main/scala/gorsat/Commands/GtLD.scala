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

import gorsat.Analysis.GroupAnalysis
import gorsat.Analysis.GtLDAnalysis.{LDSelfJoinAnalysis, LDcalculation}
import gorsat.Commands.CommandParseUtilities._
import gorsat.Utilities.IteratorUtilities
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.session.GorContext

class GtLD extends CommandInfo("GTLD",
  CommandArguments("-sum -calc", "-f", 0, 0),
  CommandOptions(gorCommand = true, verifyCommand = true, cancelCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {
    val binN = 100
    var leftHeader = forcedInputHeader

    val fuzzFactor = intValueOfOptionWithDefaultWithRangeCheck(args, "-f", 0, 0)

    if (!hasOption(args, "-calc") && !hasOption(args, "-sum")) {
      throw new GorParsingException("Pease specify either -sumLD, -calcLD, or both of these options if not running in parallel over partitions.")
    }

    var combinedHeader = leftHeader
    val allCols = leftHeader.split("\t")

    val bucketCol = leftHeader.split("\t",-1).indexWhere( x => x.toUpperCase == "BUCKET" )
    val valuesCol = leftHeader.split("\t",-1).indexWhere( x => x.toUpperCase == "VALUES" )
    val useOnlyAsLeftVar = leftHeader.split("\t",-1).indexWhere( x => x.toUpperCase == "USEONLYASLEFTVAR" )
    var otherCols: List[Int] = Range(0,allCols.length).toList

    val req = if (bucketCol >= 0) List(bucketCol) else Nil

    if (hasOption(args, "-sum")) {
      otherCols = otherCols filterNot (x => x == bucketCol || x == valuesCol || x <= 1)

      leftHeader = leftHeader.split("\t").slice(0, 2).mkString("\t") + "\t" + otherCols.map(allCols(_)).mkString("\t")
      combinedHeader = leftHeader + "\tdistance\t" + leftHeader.split("\t").slice(1, leftHeader.length).mkString("\t")

      combinedHeader += "\tLD_x11\tLD_x12\tLD_x21\tLD_x22"
      combinedHeader = IteratorUtilities.validHeader(combinedHeader)
    }

    val headerLength = combinedHeader.split("\t").length
    val gcCols = Range(2,headerLength-4).toList
    val icCols = Range(headerLength-4,headerLength).toList

    val missingSEG = "" // not used here

    val binsize = 2 * (2 + fuzzFactor / binN)

    var pipeStep: Analysis = null
    var aggrUsed = false

    if (hasOption(args, "-sum")) {

      pipeStep = LDSelfJoinAnalysis(binsize, missingSEG, fuzzFactor, req, otherCols, valuesCol, useOnlyAsLeftVar, binN)
      if (bucketCol >= 0 || hasOption(args, "-calc")) {
        aggrUsed = true
        pipeStep |= GroupAnalysis.Aggregate(1, useCount = false, useCdist = false, useMax = false, useMin = false, useMed = false, useDis = false, useSet = false, useLis = false, useAvg = false, useStd = false, useSum = true, Nil, icCols, Nil, gcCols, 10000, ",", null)
      }
    }
    if (hasOption(args, "-calc")) {
      if (hasOption(args, "-sum") && !aggrUsed) {
        pipeStep |= GroupAnalysis.Aggregate(1, useCount = false, useCdist = false, useMax = false, useMin = false, useMed = false, useDis = false, useSet = false, useLis = false, useAvg = false, useStd = false, useSum = true, Nil, icCols, Nil, gcCols, 10000, ",", null)
      }
      if (!hasOption(args, "-sum")) {
        pipeStep = GroupAnalysis.Aggregate(1, useCount = false, useCdist = false, useMax = false, useMin = false, useMed = false, useDis = false, useSet = false, useLis = false, useAvg = false, useStd = false, useSum = true, Nil, icCols, Nil, gcCols, 10000, ",", null)
        // Here we add pipeStep |= calc LD and R
      }
      val x11Col = combinedHeader.split("\t",-1).indexWhere( x => x.toUpperCase == "LD_X11" )
      if (x11Col < 0) {
        throw new GorParsingException("For the -calc option the input must have the columns LD_x11,LD_x12,LD_x21, and LD_x22.  You need to apply -sum as well.")
      }
      pipeStep |= LDcalculation(x11Col)
      combinedHeader += "\tLD_Dp\tLD_r"
    }

    CommandParsingResult(pipeStep, combinedHeader)
  }

}
