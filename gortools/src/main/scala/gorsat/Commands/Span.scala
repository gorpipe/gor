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

import gorsat.Analysis.{CheckOrder, ChopSegment, ProjectSegments, SortAnalysis}
import gorsat.Commands.CommandParseUtilities._
import gorsat.IteratorUtilities.validHeader
import org.gorpipe.gor.{GorContext, GorSession}

import scala.collection.mutable.ListBuffer


object Span {

  def parseSpanInfo(command:String, session: GorSession, iargs: Array[String], args: Array[String], header: String): CommandParsingResult = {
    var pipeStep: Analysis = null

    val gcCols = columnsOfOptionWithNil(args, "-gc", header).distinct
    val window = intValueOfOptionWithDefaultWithRangeCheck(args, "-maxseg", 4000000, 0)

    val hcol = header.split("\t")
    val columns = ListBuffer[ColumnHeader]()
    columns += ColumnHeader("Chrom", "S")
    columns += ColumnHeader("bpStart", "I")
    columns += ColumnHeader("bpStop", "I")
    columns += ColumnHeader("segCount", "I")
    for (c <- gcCols) columns += ColumnHeader(hcol(c), c.toString)

    val outgoingHeader = RowHeader(columns)
    val combinedHeader = validHeader(outgoingHeader.toString)

    if (gcCols.nonEmpty) {
      pipeStep = ProjectSegments(gcCols, window, outgoingHeader) | SortAnalysis(combinedHeader, session, window * 2) | CheckOrder("SEGSPAN " + args.mkString(" "))
    } else if (hasOption(args, "-maxseg")) {
      pipeStep = ProjectSegments(gcCols, window, outgoingHeader) | ChopSegment(window)
    } else {
      pipeStep = ProjectSegments(gcCols, window, outgoingHeader)
    }

    CommandParsingResult(pipeStep, combinedHeader)
  }

  class Span extends CommandInfo("SPAN",
    CommandArguments("", "-gc -maxseg", 0, 0),
    CommandOptions(gorCommand = true, verifyCommand = true))
  {
    override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {
      parseSpanInfo(name, context.getSession, iargs, args, forcedInputHeader)
    }
  }

  class SegSpan extends CommandInfo("SEGSPAN",
    CommandArguments("", "-gc -maxseg", 0, 0),
    CommandOptions(gorCommand = true, memoryMonitorCommand = true, verifyCommand = true))
  {
    override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {
      parseSpanInfo(name, context.getSession, iargs, args, forcedInputHeader)
    }
  }
}
