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

import gorsat.Analysis.SelfJoinAnalysis.SelfJoinAnalysis
import gorsat.Commands.CommandParseUtilities._
import gorsat.IteratorUtilities.validHeader
import org.gorpipe.gor.GorContext

class SelfJoin extends CommandInfo("SELFJOIN",
  CommandArguments("-h", "-f -x", 0, 0),
  CommandOptions(gorCommand = true, verifyCommand = true, cancelCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {
    val binN = 100
    val leftHeader = forcedInputHeader

    val doLeftJoin = true
    val emptyString = ""

    val fuzzFactor = intValueOfOptionWithDefaultWithRangeCheck(args, "-f", 0, 0)

    val req = columnsOfOptionWithNil(args, "-x", leftHeader).distinct

    var combinedHeader = leftHeader
    val rightHeader = leftHeader
    val rightCols = rightHeader.split("\t").toList

    combinedHeader = rightCols.slice(1, rightCols.length).foldLeft(leftHeader + "\tdistance") (_ + "\t" + _)

    val missingSEG = if (doLeftJoin) Range(0, rightCols.length - 1).toList.map(x => emptyString).mkString("\t") else ""

    if (hasOption(args, "-h")) combinedHeader = null else combinedHeader = validHeader(combinedHeader)

    val binsize = 2 * (2 + fuzzFactor / binN)
    val pipeStep: Analysis = SelfJoinAnalysis(binsize, missingSEG, fuzzFactor, req, binN)

    CommandParsingResult(pipeStep, combinedHeader)
  }
}
