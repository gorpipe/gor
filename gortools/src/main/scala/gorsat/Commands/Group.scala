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
import gorsat.Commands.CommandParseUtilities._
import gorsat.Utilities.IteratorUtilities.validHeader
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.session.GorContext

import scala.collection.mutable.ListBuffer

class Group extends CommandInfo("GROUP",
  CommandArguments("-count -cdist -min -med -max -dis -set -lis -avg -std -sum -h -ordered -notruncate", "-gc -sc -ac -ic -fc -len -steps " +
    "-s", 0, 1),
  CommandOptions(gorCommand = true, norCommand = true, memoryMonitorCommand = true, verifyCommand = true,
    cancelCommand = true, ignoreSplitCommand = true)) {
  override def processArguments(context: GorContext, argString: String, iargs: Array[String],
                                args: Array[String], executeNor: Boolean, forcedInputHeader: String)
  : CommandParsingResult =
{
    if (iargs.length == 1 && executeNor) {
      throw new GorParsingException(s"Cannot have binSize option when running a nor query; ${iargs(0)}. Command has 1" +
        s" input option but accepts none.")
    }

    if (!executeNor && iargs.length < 1) {
      throw new GorParsingException("Too few input arguments supplied for group.")
    }

    var setLen = 10000
    val sepVal = replaceSingleQuotes(stringValueOfOptionWithDefault(args, "-s", ","))


    val useCount = hasOption(args, "-count")
    val useCdist = hasOption(args, "-cdist")
    val useMax = hasOption(args, "-max")
    val useMin = hasOption(args, "-min")
    val useMed = hasOption(args, "-med")
    val useDis = hasOption(args, "-dis")
    val useSet = hasOption(args, "-set")
    val useLis = hasOption(args, "-lis")
    val useAvg = hasOption(args, "-avg")
    val useStd = hasOption(args, "-std")
    val useSum = hasOption(args, "-sum")
    val truncate = !hasOption(args, "-notruncate")

    val assumeOrdered = hasOption(args, "-ordered")

    if (hasOption(args, "-len")) setLen = intValueOfOptionWithRangeCheck(args, "-len", 10, 1000000)

    val chrGen = if (executeNor) "1" else iargs(0).toUpperCase

    var binSize = if (executeNor) 1 else 1000000000

    if (!(chrGen.startsWith("CHR") || chrGen.startsWith("GEN") || executeNor)) {
      binSize = parseIntWithRangeCheck("binSize", iargs(0), 1)
    }

    var slideSteps = intValueOfOptionWithDefaultWithRangeCheck(args, "-steps", 1, 1, 100)
    if (binSize / slideSteps == 0) slideSteps = binSize

    if (slideSteps > binSize) throw new GorParsingException("-steps cannot be larger than binSize")

    val inputHeader = forcedInputHeader

    val gcCols: List[Int] = columnsOfOptionWithNil(args, "-gc", inputHeader, executeNor).distinct
    var acCols: List[Int] = if (hasOption(args, "-ac")) {
      columnsOfOption(args, "-ac", inputHeader, executeNor).distinct
    } else if (hasOption(args, "-sc")) {
      columnsOfOption(args, "-sc", inputHeader, executeNor).distinct
    } else {
      Nil
    }
    var icCols: List[Int] = columnsOfOptionWithNil(args, "-ic", inputHeader, executeNor).distinct
    val fcCols: List[Int] = columnsOfOptionWithNil(args, "-fc", inputHeader, executeNor).distinct

    gcCols.foreach { x =>
      if (acCols.contains(x) || icCols.contains(x) || fcCols.contains(x)) {
        throw new GorParsingException(s"Grouping column $x is selected for aggregation.")
      }
    }

    acCols = acCols filterNot (x => icCols.contains(x) || fcCols.contains(x))
    icCols = icCols filterNot (x => fcCols.contains(x))

    if (useMax || useMin || useMed || useDis || useSet || useLis || useAvg || useStd || useSum) {
      if (gcCols.length + acCols.length + icCols.length + fcCols.length == 0) {
        throw new GorParsingException("No columns selected for aggregation. Use -gc, -sc, -ic, or -fc to specify " +
          "which columns should be aggregated.")
      }
    }

    val hcol = inputHeader.split("\t")
    var columns = ListBuffer[ColumnHeader]()

    if (binSize > 1) {
      // Note:  Know we are not using NOR here as NOR not allowed binning.
      //        Hence don't have to worry about NOR column names here.
      columns += ColumnHeader(hcol(0), "S")
      columns += ColumnHeader("bpStart", "I")
      columns += ColumnHeader("bpStop", "I")
    }
    else {
      columns += ColumnHeader(hcol(0), "S")
      columns += ColumnHeader(hcol(1), "I")
    }

    for (c <- gcCols) {
      columns += ColumnHeader(hcol(c), c.toString)
    }

    if (useCount) {
      columns += ColumnHeader("allCount", "I")
    }
    if (useCdist) {
      columns += ColumnHeader("distCount", "I")
    }
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
    var combinedHeader = validHeader(header.toString)

    if (hasOption(args, "-h")) combinedHeader = null

    var pipeStep: Analysis = null
    var overwriteValidtion = false

    if (chrGen.startsWith("CHR")) {
      pipeStep = GroupAnalysis.ChromAggregate(context.getSession, useCount, useCdist, useMax, useMin, useMed, useDis, useSet,
        useLis, useAvg, useStd, useSum, acCols, icCols, fcCols, gcCols, setLen, truncate, sepVal, header)
    } else if (chrGen.startsWith("GEN")) {
      overwriteValidtion = true
      pipeStep = GroupAnalysis.GenomeAggregate(useCount, useCdist, useMax, useMin, useMed, useDis, useSet, useLis,
        useAvg, useStd, useSum, acCols, icCols, fcCols, gcCols, setLen, truncate, sepVal, header)
    } else {
      if (slideSteps > 1) {
        pipeStep = GroupAnalysis.SlideAggregate(slideSteps, binSize, useCount, useCdist, useMax, useMin, useMed,
          useDis, useSet, useLis, useAvg, useStd, useSum, acCols, icCols, fcCols, gcCols, setLen, truncate, sepVal, header)
      } else {
        pipeStep = if (assumeOrdered)
          GroupAnalysis.OrderedAggregate(binSize, useCount, useCdist, useMax, useMin, useMed, useDis, useSet,
            useLis, useAvg, useStd, useSum, acCols, icCols, fcCols, gcCols, setLen, truncate, sepVal, header)
          else
          GroupAnalysis.Aggregate(binSize, useCount, useCdist, useMax, useMin, useMed, useDis, useSet,
            useLis, useAvg, useStd, useSum, acCols, icCols, fcCols, gcCols, setLen, truncate, sepVal, header)
      }
    }

    val results = CommandParsingResult(pipeStep, combinedHeader)
    results.excludeValidation = overwriteValidtion
    results
  }
}