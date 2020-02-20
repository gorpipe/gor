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

import gorsat.Analysis.RankAnalysis._
import gorsat.Commands.CommandParseUtilities._
import gorsat.IteratorUtilities.validHeader
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.GorContext

import scala.collection.mutable.ListBuffer


class Rank extends CommandInfo("RANK",
  CommandArguments("-q -z -b -c", "-rmax -gc -o", 1, 2),
  CommandOptions(gorCommand = true, norCommand = true, memoryMonitorCommand = true, verifyCommand = true, cancelCommand = true, ignoreSplitCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {

    if (iargs.length == 2 && executeNor) {
      throw new GorParsingException(s"Cannot have binSize option when running a nor query: ${iargs(0)}. Command has 2 input options but only accepts 1.")
    }

    if ((executeNor && iargs.length < 1) || (!executeNor && iargs.length < 2)) {
      throw new GorParsingException("Too few input arguments supplied for rank.")
    }

    val pa = Parameters()

    pa.asc = false
    if (hasOption(args,"-o")) {
      if (stringValueOfOption(args,"-o").toUpperCase.startsWith("ASC")) pa.asc = true
    } else pa.asc = false

    pa.useZ = hasOption(args,"-z")
    pa.useDistr = hasOption(args,"-q")
    pa.useCount = hasOption(args,"-c")
    pa.useRankOne = hasOption(args,"-b")
    if (hasOption(args,"-rmax")) pa.maxRank = intValueOfOptionWithRangeCheck(args,"-rmax", 1, 100)

    val chrGen = if (executeNor) "1" else iargs(0).toUpperCase
    var binSize = if (executeNor) 1 else 1000000000

    if (!(chrGen.startsWith("CHR") || chrGen.startsWith("GEN") || executeNor)) binSize = parseIntWithRangeCheck( "binSize", iargs(0), 1)

    val inputHeader = forcedInputHeader
    val rankColumnIndex = if (executeNor) 0 else 1
    val rankColumn = columnFromHeader(iargs(rankColumnIndex),inputHeader,executeNor)

    val gcCols = columnsOfOptionWithNil(args,"-gc",inputHeader,executeNor).distinct

    val hcol = inputHeader.split("\t")

    val columns = ListBuffer[ColumnHeader]()
    for (col <- hcol.indices) {
      // Outgoing header has all the columns from the original, with the same types as the original
      columns += ColumnHeader(hcol(col), col.toString)
    }

    columns += ColumnHeader("rank_" + hcol(rankColumn), "I")
    if (pa.useDistr) {
      columns += ColumnHeader("lowOReqRank", "I")
      columns += ColumnHeader("eqRank", "I")
    }
    if (pa.useZ) {
      columns += ColumnHeader("z_"+iargs(rankColumnIndex), "D")
    }
    if (pa.useCount) {
      columns += ColumnHeader("binCount", "I")
    }
    if (pa.useRankOne) {
      columns += ColumnHeader("rank1_"+iargs(rankColumnIndex), "I")
    }

    val header = RowHeader(columns)
    var combinedHeader = validHeader(header.toString)

    // INFO: Undocumented command, seems to remove the header??
    if (hasOption(args,"-h")) combinedHeader = null

    var pipeStep: Analysis = null

    if (chrGen.startsWith("CHR")) {
      pipeStep = ChromRank(context.getSession, rankColumn, gcCols, pa, header)
    } else if (chrGen.startsWith("GEN")) {
      pipeStep = GenomeRank(rankColumn, gcCols, pa, header)
    } else {
      pipeStep = Rank(binSize, rankColumn, gcCols, pa, header)
    }

    CommandParsingResult(pipeStep, combinedHeader)
  }
}

