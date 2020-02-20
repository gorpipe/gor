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

import gorsat.Analysis.GrannoAnalysis.{Aggregate, ChromAggregate, GenomeAggregate}
import gorsat.Analysis.RangeAggregate
import gorsat.Commands.CommandParseUtilities._
import gorsat.IteratorUtilities.validHeader
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.GorContext

import scala.collection.mutable.ListBuffer


class Granno extends CommandInfo("GRANNO",
  CommandArguments("-range -count -cdist -min -med -max -dis -set -lis -avg -std -sum -h", "-gc -ac -sc -ic -fc -len " +
    "-s", 0, 1),
  CommandOptions(gorCommand = true, norCommand = true, memoryMonitorCommand = true, verifyCommand = true,
    cancelCommand = true, ignoreSplitCommand = true)) {
  override def processArguments(context: GorContext, argString: String, iargs: Array[String],
                                args: Array[String], executeNor: Boolean, forcedInputHeader: String)
  : CommandParsingResult = {

    if (iargs.length == 1 && executeNor) {
      throw new GorParsingException(s"Cannot have binSize option when running a nor query: ${iargs(0)}. Command has 1" +
        s" input option but accepts none.")
    }

    if (!executeNor && iargs.length < 1) {
      throw new GorParsingException("Too few input arguments supplied for granno.")
    }

    var useCount = false
    var useCdist = false
    var useMax = false
    var useMin = false
    var useMed = false
    var useDis = false
    var useSet = false
    var useLis = false
    var useAvg = false
    var useStd = false
    var useSum = false
    var setLen = 10000
    var sepVal = replaceSingleQuotes(stringValueOfOptionWithDefault(args, "-s", ","))

    if (hasOption(args, "-count")) useCount = true
    if (hasOption(args, "-cdist")) useCdist = true
    if (hasOption(args, "-max")) useMax = true
    if (hasOption(args, "-min")) useMin = true
    if (hasOption(args, "-med")) useMed = true
    if (hasOption(args, "-dis")) useDis = true
    if (hasOption(args, "-set")) useSet = true
    if (hasOption(args, "-lis")) useLis = true
    if (hasOption(args, "-avg")) useAvg = true
    if (hasOption(args, "-std")) useStd = true
    if (hasOption(args, "-sum")) useSum = true

    if (hasOption(args, "-len")) setLen = intValueOfOptionWithRangeCheck(args, "-len", 10, 1000000)

    val chrGen = if (executeNor) "1" else iargs(0).toUpperCase
    var binSize = 1000000000

    if (!(chrGen.startsWith("CHR") || chrGen.startsWith("GEN") || executeNor)) binSize =
      parseIntWithRangeCheck("binSize", iargs(0), 1)

    val inputHeader = forcedInputHeader

    var gcCols: List[Int] = columnsOfOptionWithNil(args, "-gc", inputHeader, executeNor).distinct
    var acCols: List[Int] = if (hasOption(args, "-ac")) {
      columnsOfOptionWithNil(args, "-ac", inputHeader, executeNor)
        .distinct
    } else if (hasOption(args, "-sc")) columnsOfOptionWithNil(args, "-sc", inputHeader, executeNor).distinct else Nil
    var icCols: List[Int] = columnsOfOptionWithNil(args, "-ic", inputHeader, executeNor).distinct
    val fcCols: List[Int] = columnsOfOptionWithNil(args, "-fc", inputHeader, executeNor).distinct

    gcCols = gcCols filterNot (x => icCols.contains(x) || fcCols.contains(x) || acCols.contains(x))
    acCols = acCols filterNot (x => icCols.contains(x) || fcCols.contains(x))
    icCols = icCols filterNot (fcCols contains)

    val hcol = inputHeader.split("\t")

    val columns = ListBuffer[ColumnHeader]()
    for (col <- hcol.indices) {
      // Outgoing header has all the columns from the original, with the same types as the original
      columns += ColumnHeader(hcol(col), col.toString)
    }

    if (useCount) columns += ColumnHeader("allCount", "I")
    if (useCdist) columns += ColumnHeader("distCount", "I")
    for (i <- 0 until hcol.length) {
      if (icCols.contains(i) || fcCols.contains(i) || acCols.contains(i)) {
        if (useMin) {
          columns += ColumnHeader("min_" + hcol(i), i.toString)
        }
        if (useMed) {
          columns += ColumnHeader("med_" + hcol(i), i.toString)
        }
        if (useMax) {
          columns += ColumnHeader("max_" + hcol(i), i.toString)
        }
        if (useSet) {
          columns += ColumnHeader("set_" + hcol(i), i.toString)
        }
        if (useLis) {
          columns += ColumnHeader("lis_" + hcol(i), i.toString)
        }
        if (useDis) {
          columns += ColumnHeader("dis_" + hcol(i), i.toString)
        }
        if (icCols.contains(i) || fcCols.contains(i)) {
          if (useAvg) {
            columns += ColumnHeader("avg_" + hcol(i), i.toString)
          }
          if (useStd) {
            columns += ColumnHeader("std_" + hcol(i), i.toString)
          }
          if (useSum) {
            columns += ColumnHeader("sum_" + hcol(i), i.toString)
          }
        }
      }
    }

    val header = RowHeader(columns)
    var combinedHeader = header.toString
    if (combinedHeader == inputHeader) {
      throw new GorParsingException("Error in aggregate calculation - no columns to calculate an aggregate. Use -gc, " +
        "-sc, -ic or -fc to select aggregation columns.")
    }
    else {
      combinedHeader = validHeader(combinedHeader)
    }

    if (hasOption(args, "-h")) combinedHeader = null

    var pipeStep: Analysis = null

    if (hasOption(args, "-range")) {
      if (chrGen.startsWith("GENE")) binSize = 3000000
      pipeStep = RangeAggregate(binSize, useCount, useCdist, useMax, useMin, useMed, useDis, useSet, useLis, useAvg,
        useStd, useSum, acCols, icCols, fcCols, gcCols, setLen, sepVal, header)
    } else {
      if (chrGen.startsWith("CHR")) {
        pipeStep = ChromAggregate(context.getSession, useCount, useCdist, useMax, useMin, useMed, useDis, useSet, useLis,
          useAvg, useStd, useSum, acCols, icCols, fcCols, gcCols, setLen, sepVal, header)
      } else if (chrGen.startsWith("GEN")) {
        pipeStep = GenomeAggregate(useCount, useCdist, useMax, useMin, useMed, useDis, useSet, useLis, useAvg,
          useStd, useSum, acCols, icCols, fcCols, gcCols, setLen, sepVal, header)
      } else {
        pipeStep = Aggregate(binSize, useCount, useCdist, useMax, useMin, useMed, useDis, useSet, useLis, useAvg,
          useStd, useSum, acCols, icCols, fcCols, gcCols, setLen, sepVal, header)
      }
    }

    CommandParsingResult(pipeStep, combinedHeader)
  }
}
