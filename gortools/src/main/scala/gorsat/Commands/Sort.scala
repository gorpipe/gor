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

import gorsat.Analysis.{CheckOrder, SortAnalysis, SortGenome}
import gorsat.Commands.CommandParseUtilities._
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.model.Row
import org.gorpipe.gor.session.GorContext

class Sort extends CommandInfo("SORT",
  CommandArguments("", "-c", 0, 1),
  CommandOptions(gorCommand = true, norCommand = true, memoryMonitorCommand = true, ignoreSplitCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {

    if (iargs.length == 1 && executeNor) {
      throw new GorParsingException(s"Cannot have binSize option when running a nor query: ${iargs(0)}. Command has 1 input option but accepts none.")
    }

    if (!executeNor && iargs.length < 1) {
      throw new GorParsingException("Too few input arguments supplied for sort.")
    }

    var window = 1
    var useTempfiles = false
    val sortInfo: Array[Row.SortInfo] = Sort.parseSortColumns(args, executeNor, forcedInputHeader)

    if (!executeNor && iargs.length > 0) {
      if (iargs(0).toUpperCase.contains("GENOME")) {
        window = 1000000000
        useTempfiles = true
      } else if (iargs(0).toUpperCase.contains("CHROM")) {
        window = 500000000
      } else {
        window = parseIntWithRangeCheck("binSize", iargs(0), 1)
      }
      if (window < 100) {
        window = 100
      }
    }

    val pipeStep = if (useTempfiles) {
      SortGenome(forcedInputHeader, context.getSession, sortInfo)
    } else {
      SortAnalysis(forcedInputHeader, context.getSession, window, sortInfo) | CheckOrder(name + " " + args.mkString(" "))
    }

    CommandParsingResult(pipeStep, forcedInputHeader)
  }
}

object Sort {
  def parseSortColumns(args: Array[String], executeNor: Boolean, forcedInputHeader: String): Array[Row.SortInfo] = {
    var sortInfo: List[Row.SortInfo] = Nil

    try {
      val ucols = if (hasOption(args, "-c")) stringValueOfOption(args, "-c").split(',') else Array.empty[String]
      val colOrder = ucols.map(x => if (x.toLowerCase.endsWith(":rn") || x.toLowerCase.endsWith(":r") || x
        .toLowerCase.endsWith(":nr")) "r" else "f")
      val colType = ucols.map(x => if (x.toLowerCase.endsWith(":rn") || x.toLowerCase.endsWith(":n") || x.toLowerCase
        .endsWith(":nr")) "n" else "s")
      val cleanCols = ucols.map(x => {
        if (x.toLowerCase.endsWith(":rn") || x.toLowerCase.endsWith(":nr")) {
          x.slice(0, x.length - 3)
        } else if (x.toLowerCase.endsWith(":n") || x.toLowerCase.endsWith(":r")) x.slice(0, x.length - 2) else x
      })

      for (i <- ucols.indices) {
        val colsToAdd = columnsFromHeader(cleanCols(i), forcedInputHeader, executeNor)
        colsToAdd.reverse.map(y => new Row.SortInfo(y, if (colOrder(i) == "r") Row.SortOrder.REVERSE else Row
          .SortOrder.FORWARD,
          if (colType(i) == "n") Row.SortType.NUMBERIC else Row.SortType.STRING)).foreach(sortInfo ::= _)
      }
    } catch {
      case e: Exception =>
        throw new GorParsingException("Illegal columns: " + stringValueOfOption(args, "-c"), e)
    }
    sortInfo.reverse.toArray
  }
}