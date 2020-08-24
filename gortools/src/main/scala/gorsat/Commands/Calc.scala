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

import gorsat.Analysis.CalcAnalysis
import gorsat.Commands.CommandParseUtilities.validateColumn
import gorsat.IteratorUtilities.validHeader
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.GorContext

class Calc extends CommandInfo("CALC",
  CommandArguments("", "", 2, -1, ignoreIllegalArguments = true),
  CommandOptions(gorCommand = true, norCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String],
                                args: Array[String], executeNor: Boolean,
                                forcedInputHeader: String): CommandParsingResult = {
    val columnNames = Calc.getColumnsFromArgs(args)
    val exprSrc = Calc.getExpressionsFromArgs(args, columnNames)

    val outgoingHeader = validHeader(forcedInputHeader + "\t" + columnNames.mkString("\t"))

    val pipeStep = CalcAnalysis(context, executeNor, exprSrc, forcedInputHeader, columnNames)

    CommandParsingResult(pipeStep, outgoingHeader)
  }
}

object Calc {
  def getExpressionsFromArgs(args: Array[String], columnNames: Array[String]): Array[String] = {
    val cmdStart = if (args.length > 2 && (args(1) == "=" || args(1).toUpperCase == "AS")) 2 else 1
    val formula = args.slice(cmdStart, args.length).mkString(" ")

    // We may have multiple expressions, comma separated
    val exprSrc: Array[String] = CommandParseUtilities.quoteSafeSplit(formula, ',')
    if (exprSrc.length < columnNames.length) {
      throw new GorParsingException("Not enough expressions in CALC command")
    } else if (exprSrc.length > columnNames.length) {
      throw new GorParsingException("Too many expressions in CALC command")
    }
    exprSrc
  }

  def getColumnsFromArgs(args: Array[String]): Array[String] = {
    val columnNames = args(0).split(',')
    for (col <- columnNames) validateColumn(col)
    columnNames
  }

}