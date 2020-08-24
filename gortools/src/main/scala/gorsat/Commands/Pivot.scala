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

import gorsat.Analysis.PivotAnalysis
import gorsat.Commands.CommandParseUtilities._
import gorsat.IteratorUtilities
import gorsat.IteratorUtilities.validHeader
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.GorContext

import scala.collection.mutable.ListBuffer

class Pivot extends CommandInfo("PIVOT",
  CommandArguments("-h -ordered", "-gc -e -v -vf -vp", 1, 1),
  CommandOptions(gorCommand = true, norCommand = true, verifyCommand = true, cancelCommand = true)) {

  override def processArguments(context: GorContext, argString: String, iargs: Array[String],
                                args: Array[String], executeNor: Boolean, forcedInputHeader: String)
  : CommandParsingResult = {
    val emptyString = replaceSingleQuotes(stringValueOfOptionWithDefault(args, "-e", "?"))
    val assumeOrdered = hasOption(args, "-ordered")

    val inputHeader = forcedInputHeader
    val pivotCol = columnFromHeader(iargs(0), inputHeader, executeNor)

    val gCols = getGroupingColumns(args, executeNor, inputHeader)
    val pivotValues = getPivotValues(args, context)
    val prefixValues = getPrefixValues(args, context)

    val hcol = inputHeader.split("\t")
    val oCols = hcol.indices filterNot (gCols ::: List(0, 1, pivotCol)).contains

    val columns = ListBuffer[ColumnHeader]()
    gCols.foreach(col => {
      columns += ColumnHeader(hcol(col), col.toString)
    })

    val prefixes = if(prefixValues.nonEmpty) prefixValues else pivotValues
    prefixes.foreach(v => {
      oCols.foreach(col => {
        columns += ColumnHeader(v + "_" + hcol(col), col.toString)
      })
    })

    val header = RowHeader(columns)
    val combinedHeader = validHeader(header.toString)

    val pipeStep: Analysis = if(assumeOrdered) {
      PivotAnalysis.PivotAnalysisOrdered(gCols.toArray, pivotCol, pivotValues, oCols.toArray, emptyString, header)
    } else {
      PivotAnalysis.PivotAnalysis(gCols.toArray, pivotCol, pivotValues, oCols.toArray, emptyString, header)
    }

    CommandParsingResult(pipeStep, combinedHeader)
  }

  private def getGroupingColumns(args: Array[String], executeNor: Boolean, inputHeader: String) = {
    (columnsOfOptionWithNil(args, "-gc", inputHeader, executeNor) ::: List(0, 1)).distinct.sorted
  }

  private def getPivotValues(args: Array[String], context: GorContext): Array[String] = {
    if(hasOption(args, "-v")) {
      stringValueOfOption(args, "-v").split("[, ]").map(x => replaceSingleQuotes(x))
    } else if(hasOption(args, "-vf")) {
      IteratorUtilities.getStringArrayFromFileOrNestedQuery(stringValueOfOption(args, "-vf"), context)
    } else {
      throw new GorParsingException("PIVOT command requires either -v or -vf to specify pivot values")
    }
  }

  def getPrefixValues(args: Array[String], context: GorContext): Array[String] =
    if(hasOption(args, "-vp")) {
      IteratorUtilities.getStringArrayFromFileOrNestedQuery(stringValueOfOption(args, "-vp"), context)
    } else {
      Array[String]()
    }
}
