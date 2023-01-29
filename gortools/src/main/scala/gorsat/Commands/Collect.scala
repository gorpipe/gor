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

import gorsat.Analysis.{CollectAnalysis, DeflateAnalysis}
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.session.GorContext

import scala.collection.mutable.ArrayBuffer

class Collect extends CommandInfo("COLLECT",
  CommandArguments("-ave -sum -var -std", "", 2, 2),
  CommandOptions(gorCommand = true, norCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {
    val columnIndex = CommandParseUtilities.columnFromHeader(iargs(0), forcedInputHeader, executeNor)
    val windowSize = CommandParseUtilities.parseIntWithRangeCheck("Windows size", iargs(1), 4)

    val calcAverage = CommandParseUtilities.hasOption(args, "-ave")
    val calcSum = CommandParseUtilities.hasOption(args, "-sum")
    val calcVariance = CommandParseUtilities.hasOption(args, "-var")
    val calcStd = CommandParseUtilities.hasOption(args, "-std")

    if (!calcAverage && !calcSum && !calcStd  && !calcVariance) {
      throw new GorParsingException("No collection option selected, select at least one of -ave, -sum, -std, -var")
    }

    val headerItems = ArrayBuffer.from(forcedInputHeader.split("\t"))
    val header = headerItems(columnIndex)

    if (calcSum) {
      headerItems += s"${header}_sum"
    }

    if (calcAverage) {
      headerItems += s"${header}_average"
    }

    if (calcStd) {
      headerItems += s"${header}_std"
    }

    if (calcVariance) {
      headerItems += s"${header}_variance"
    }

    CommandParsingResult(
      CollectAnalysis(columnIndex, header, windowSize, calcAverage, calcSum, calcVariance, calcStd),
      headerItems.mkString("\t")
    )
  }
}

