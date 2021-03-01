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

import gorsat.Analysis.BucketSplitAnalysis
import gorsat.Commands.CommandParseUtilities._
import gorsat.Utilities.IteratorUtilities
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.session.GorContext

class BucketSplit extends CommandInfo("BUCKETSPLIT",
  CommandArguments("-cs", "-vs -s -b", 2, 2),
  CommandOptions(gorCommand = true, norCommand = true)) {

  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {

    var sepVal = replaceSingleQuotes(stringValueOfOptionWithDefault(args,"-s",""))
    if (sepVal.length > 1) throw new GorParsingException("Separator must be a single character or empty char for fixed value size")

    var valSize = -1
    if (hasOption(args, "-vs")) {
      valSize = intValueOfOptionWithRangeCheck(args, "-vs", 1)
    }

    var doValidation = !hasOption(args, "-cs")

    if (sepVal.length == 1 && valSize > 0) throw new GorParsingException("Options -vs and -s can not be specified at the same time")
    if (sepVal.length == 0 && valSize == -1) throw new GorParsingException("Either option -vs or -s must be specified")

    val bucketPrefix = replaceSingleQuotes(stringValueOfOptionWithDefault(args, "-b","b_"))

    val valuesColumn = columnFromHeader(iargs(0).trim, forcedInputHeader, executeNor)
    val bucketSize =  parseIntWithRangeCheck("bucketSize", iargs(1), 1)

    var combinedHeader = IteratorUtilities.validHeader(forcedInputHeader + "\tbucket")
    if (hasOption(args,"-h")) combinedHeader = null

    val pipeStep: Analysis = BucketSplitAnalysis(valuesColumn, bucketSize, sepVal, valSize, bucketPrefix, doValidation)

    CommandParsingResult(pipeStep, combinedHeader)
  }
}
