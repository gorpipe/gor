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

package gorsat.InputSources

import gorsat.Commands.{CommandArguments, CommandParseUtilities, InputSourceInfo, InputSourceParsingResult}
import gorsat.Iterators.CountingNorRowIterator
import org.gorpipe.gor.GorContext

class Norrows() extends InputSourceInfo("NORROWS", CommandArguments("","-step -offset", 1, 1), isNorCommand = true) {

  override def processArguments(context: GorContext, argString: String, iargs: Array[String],
                                args: Array[String]): InputSourceParsingResult = {
    val norrowNumberOfRows = CommandParseUtilities.parseIntWithRangeCheck("Norrows number of rows", iargs(0), 0)
    val step = CommandParseUtilities.intValueOfOptionWithDefaultWithRangeCheck(args, "-step", 1, 1)
    val offset = CommandParseUtilities.intValueOfOptionWithDefaultWithRangeCheck(args, "-offset", 0, 0)


    val inputSource = CountingNorRowIterator(norrowNumberOfRows, offset, step)
    val header = "RowNum"
    inputSource.setHeader(header)

    InputSourceParsingResult(inputSource, header, isNorContext = true)
  }
}