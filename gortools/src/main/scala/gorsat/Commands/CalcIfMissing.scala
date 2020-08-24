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
import gorsat.IteratorUtilities.validHeader
import org.gorpipe.gor.GorContext

import scala.collection.mutable.ArrayBuffer

class CalcIfMissing extends CommandInfo("CALCIFMISSING",
  CommandArguments("", "", 2, -1, ignoreIllegalArguments = true),
  CommandOptions(gorCommand = true, norCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String],
                                args: Array[String], executeNor: Boolean,
                                forcedInputHeader: String): CommandParsingResult =
  {
    val columnNames = Calc.getColumnsFromArgs(args)
    val exprSrc = Calc.getExpressionsFromArgs(args, columnNames)

    val existingColumns = forcedInputHeader.split('\t')
    val newColumns = ArrayBuffer[String]()
    val expressions = ArrayBuffer[String]()
    for (i <- columnNames.indices) {
      if (!existingColumns.contains(columnNames(i))) {
        newColumns += columnNames(i)
        expressions += exprSrc(i)
      }
    }

    if (newColumns.nonEmpty) {
      val outgoingHeader = validHeader(forcedInputHeader + "\t" + newColumns.mkString("\t"))
      val pipeStep = CalcAnalysis(context, executeNor, expressions.toArray, forcedInputHeader, newColumns.toArray)
      CommandParsingResult(pipeStep, outgoingHeader)
    } else {
      CommandParsingResult(null, forcedInputHeader)
    }
  }
}
