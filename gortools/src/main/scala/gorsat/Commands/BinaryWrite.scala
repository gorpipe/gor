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

import gorsat.Analysis.{BGenWriteAnalysis, PGenWriteAnalysis}
import gorsat.Commands.CommandParseUtilities._
import org.apache.commons.io.FilenameUtils
import org.gorpipe.exceptions.{GorParsingException, GorResourceException}
import org.gorpipe.gor.session.GorContext

class BinaryWrite extends CommandInfo("BINARYWRITE",
  CommandArguments("-imp -gv", "-threshold -batch", 1),
  CommandOptions(gorCommand = true, verifyCommand = true)) {

  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {

    val thresholdOptionString = "-threshold"

    val fileName = replaceSingleQuotes(iargs.mkString(" "))

    val fileEnding = FilenameUtils.getExtension(fileName)

    val imputed = hasOption(args, "-imp")

    val group = hasOption(args, "-gv")

    val threshold = doubleValueOfOptionWithDefaultWithRangeCheck(args, thresholdOptionString, 0.9, 0, 1)

    val batch = intValueOfOptionWithDefaultWithRangeCheck(args, "-batch", 0, 0)

    if (imputed && group) throw new GorParsingException("The -imp and -gv option are currently not allowed together.")

    val colsToUpper = forcedInputHeader.split('\t').map(_.toUpperCase())
    val REF_COLUMN_BEGINNING = "REF"
    val ALT_COLUMN_BEGINNING = "ALT"
    val RS_COLUMN_BEGINNING = "RS"
    val VAR_COLUMN_BEGINNING = "VAR"
    val VALUE_COLUMN_BEGINNING = "VALUE"
    if (!unambiguousColumnNames(colsToUpper, REF_COLUMN_BEGINNING, ALT_COLUMN_BEGINNING, RS_COLUMN_BEGINNING, VAR_COLUMN_BEGINNING, VALUE_COLUMN_BEGINNING)) {
      throw new GorResourceException("Ambiguous column names in header:\t" + forcedInputHeader + "\n", fileName)
    }
    val colToIdx = colsToUpper.zipWithIndex
    var refIdx: Int = -1
    var altIdx: Int = -1
    var rsIdIdx: Int = -1
    var varIdIdx: Int = -1
    var valIdx: Int = -1

    colToIdx.foreach { case (col, idx) =>
      if (col.startsWith(REF_COLUMN_BEGINNING)) {
        refIdx = idx
      } else if (col.startsWith(ALT_COLUMN_BEGINNING)) {
        altIdx = idx
      } else if (col.startsWith(RS_COLUMN_BEGINNING)) {
        rsIdIdx = idx
      } else if (col.startsWith(VAR_COLUMN_BEGINNING)) {
        varIdIdx = idx
      } else if (col.startsWith(VALUE_COLUMN_BEGINNING)) {
        valIdx = idx
      }
    }

    if (refIdx == -1) throw new GorResourceException("There must be one column with name REF or REFERENCE", fileName)
    if (altIdx == -1) throw new GorResourceException("There must be one column with name ALT or ALTERNATIVE", fileName)
    if (valIdx == -1) throw new GorResourceException("There must be one column with name VALUES", fileName)

    val inputHeader = if (batch>0) "Chrom\tPos\tfileName" else forcedInputHeader
    if (fileEnding == "pgen") {
      CommandParsingResult(PGenWriteAnalysis(fileName, batch, imputed, threshold.toFloat, group, refIdx, altIdx, rsIdIdx, valIdx, context.getSession.getProjectContext.getFileReader), inputHeader)
    } else if (fileEnding == "bgen") {
      CommandParsingResult(BGenWriteAnalysis(fileName, batch, group, imputed, refIdx, altIdx, rsIdIdx, varIdIdx, valIdx), inputHeader)
    } else {
      throw new GorParsingException("Unknown file ending: " + fileEnding)
    }
  }

  private def unambiguousColumnNames(cols: Iterable[String], nameBeginnings: String*): Boolean = {
    nameBeginnings.forall(beginning => {
      cols.count(_.startsWith(beginning)) <= 1
    })
  }
}
