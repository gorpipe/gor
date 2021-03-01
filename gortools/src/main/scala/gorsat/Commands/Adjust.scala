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

import gorsat.Analysis.{AdjustAnalysis, AdjustOptions}
import gorsat.Commands.CommandParseUtilities._
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.session.GorContext

class Adjust extends CommandInfo("ADJUST",
  CommandArguments("-gcc -qq -bh -by -ss -sd -holm -bonf", "-gc -pc", 0, 0),
  CommandOptions(gorCommand = true, norCommand = true)) {

  override def processArguments(context: GorContext, argString: String, iArgs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {
    val gcc = hasOption(args, "-gcc")
    val qq = hasOption(args, "-qq")
    val bh = hasOption(args, "-bh")
    val by = hasOption(args, "-by")
    val ss = hasOption(args, "-ss")
    val sd = hasOption(args, "-sd")
    val holm = hasOption(args, "-holm")
    val bonf = hasOption(args, "-bonf")

    if (!(gcc || bh || by || ss || sd || holm || bonf)) {
      throw new GorParsingException("You must specify at least one of the following options:\n"
        + "-gcc\t: Genomic control correction\n"
        + "-bonf\t: Bonferroni correction\n"
        + "-holm\t: Holm-Bonferroni correction\n"
        + "-ss\t: Sidak single step correction.\n"
        + "-sd\t: Sidak step down correction.\n"
        + "-bh\t: Benjamini-Hochberg correction.\n"
        + "-by\t: Benjamini-Yekutieli correction.\n"
      )
    }

    if (!hasOption(args, "-pc")) {
      throw new GorParsingException("There must be a column containing p values.")
    }

    val groupCols = columnsOfOptionWithNil(args, "-gc", forcedInputHeader)
    val pValueCol = if (hasOption(args, "-pc")) columnOfOption(args, "-pc", forcedInputHeader) else -1

    val outHeaderBuilder = new StringBuilder(forcedInputHeader)

    if (gcc) outHeaderBuilder.append("\tGC")
    if (qq) outHeaderBuilder.append("\tQQ")
    if (bonf) outHeaderBuilder.append("\tBONF")
    if (holm) outHeaderBuilder.append("\tHOLM")
    if (ss) outHeaderBuilder.append("\tSS")
    if (sd) outHeaderBuilder.append("\tSD")
    if (bh) outHeaderBuilder.append("\tBH")
    if (by) outHeaderBuilder.append("\tBY")

    val outHeader = outHeaderBuilder.toString()
    val adOpt = AdjustOptions(gcc, qq, bh, by, ss, sd, holm, bonf)

    CommandParsingResult(AdjustAnalysis(adOpt, pValueCol, groupCols), outHeader)
  }
}
