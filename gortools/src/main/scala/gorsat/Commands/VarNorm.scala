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

import gorsat.Analysis.{SortAnalysis, VarNormAnalysis}
import gorsat.Commands.CommandParseUtilities._
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.session.GorContext

class VarNorm extends CommandInfo("VARNORM",
  CommandArguments("-seg -left -right -trim", "-span", 2, 2),
  CommandOptions(gorCommand = true, verifyCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {
    var refCol = -1
    var alleleCol = -1
    var seg = false
    var leftNormalize = true
    var vcfForm = true

    if (hasOption(args, "-left") && hasOption(args, "-right")) {
      throw new GorParsingException("Error in options - -left and -right are mutually exclusive. Select either -left or -right: ")
    }

    if (hasOption(args, "-seg")) seg = true
    if (hasOption(args, "-trim")) vcfForm = false
    if (hasOption(args, "-left")) leftNormalize = true
    if (hasOption(args, "-right")) leftNormalize = false
    var mergeSpan = intValueOfOptionWithDefaultWithRangeCheck(args, "-span", 1000, 0)
    if (mergeSpan > 1000000) { throw new GorParsingException("Span cannot exceed 1Mb!  This leads to slow execution and heave memory. Consider eliminating problematic variants.") }
    refCol = columnFromHeader(iargs(0), forcedInputHeader, executeNor)
    alleleCol = columnFromHeader(iargs(1), forcedInputHeader, executeNor)
    if (refCol < 0 || alleleCol < 0) {
      throw new GorParsingException(s"Error in columns - specify the 2 columns for the reference ($refCol) and the alternative ($alleleCol) allele: ")
    }

    CommandParsingResult(VarNormAnalysis(refCol, alleleCol, vcfForm, seg, forcedInputHeader, leftNormalize, mergeSpan, context.getSession) | SortAnalysis(forcedInputHeader, context.getSession, 1.max(mergeSpan)), forcedInputHeader)
  }
}