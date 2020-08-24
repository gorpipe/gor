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

import gorsat.Analysis.VarGroupAnalysis
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.session.GorContext

class VarGroup extends CommandInfo("VARGROUP",
  CommandArguments("", "-sep -gc", 0, 0),
  CommandOptions(gorCommand = true, cancelCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], opts: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {
    val headerCols = forcedInputHeader.split('\t')
    val headerColsUpper = headerCols.map(_.toUpperCase())
    val valueIdx = headerColsUpper.indexWhere(_.equals("VALUES"))
    val refIdx = headerColsUpper.indexWhere(_.startsWith("REF"))
    val altIdx = headerColsUpper.indexWhere(_.startsWith("ALT"))
    val sep = CommandParseUtilities.stringValueOfOptionWithDefault(opts, "-sep", ",")
    val grCols = CommandParseUtilities.columnsOfOptionWithNil(opts, "-gc", forcedInputHeader)

    if (valueIdx == -1) throw new GorParsingException("There must be a column named 'values' which contains genotypes.")
    if (refIdx == -1) throw new GorParsingException("There must be a column which starts with 'ref' and contains the reference allele.")
    if (altIdx == -1) throw new GorParsingException("There must be a column which starts with 'alt' and contains the alternative allele.")

    val outHeaderBuilder = new StringBuilder
    outHeaderBuilder.append(headerCols(0))
    outHeaderBuilder.append('\t')
    outHeaderBuilder.append(headerCols(1))
    outHeaderBuilder.append('\t')
    outHeaderBuilder.append(headerCols(refIdx))
    outHeaderBuilder.append('\t')
    outHeaderBuilder.append(headerCols(altIdx))
    grCols.foreach(idx => {
      outHeaderBuilder.append('\t')
      outHeaderBuilder.append(headerCols(idx))
    })
    outHeaderBuilder.append('\t')
    outHeaderBuilder.append(headerCols(valueIdx))

    val outHeader = outHeaderBuilder.toString

    CommandParsingResult(VarGroupAnalysis(refIdx, altIdx, valueIdx, grCols, sep), outHeader)
  }
}
