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
import gorsat.Analysis.VerifyVariantAnalysis
import gorsat.Commands.CommandParseUtilities.hasOption
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.session.GorContext

class VerifyVariant extends CommandInfo(
  "VERIFYVARIANT",
  CommandArguments("", "-ref -alt -maxlen", 0, 0),
  CommandOptions(gorCommand = true)
) {
  override protected def processArguments(context: GorContext, argString: String, inputArguments: Array[String],
                                          options: Array[String], executeNor: Boolean,
                                          forcedInputHeader: String): CommandParsingResult = {

    val columns = forcedInputHeader.split("\t").map(_.toUpperCase)

    var refColumn = -1
    if (hasOption(options, "-ref")) refColumn = CommandParseUtilities.columnOfOption(options, "-ref", forcedInputHeader)
    if (refColumn < 0) refColumn = columns.indexOf("REF")
    if (refColumn < 0) refColumn = columns.indexOf("REFERENCE")
    if (refColumn < 0) throw new GorParsingException("No REF or REFERENCE column found")

    var altColumn = -1
    if (hasOption(options, "-alt")) altColumn = CommandParseUtilities.columnOfOption(options, "-alt", forcedInputHeader)
    if (altColumn < 0) altColumn = columns.indexOf("ALT")
    if (altColumn < 0) altColumn = columns.indexOf("CALL")
    if (altColumn < 0) altColumn = columns.indexOf("ALLELE")
    if (altColumn < 0) throw new GorParsingException("No ALT, CALL or ALLELE column found")

    val maxLen =CommandParseUtilities.intValueOfOptionWithDefault(options, "-maxlen", 16)
    CommandParsingResult(VerifyVariantAnalysis(context.getSession, refColumn, altColumn, maxLen), forcedInputHeader)
  }
}
