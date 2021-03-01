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

import gorsat.Commands.CommandParseUtilities.{hasOption, rangeOfOption, stringValueOfOption}
import gorsat.Commands._
import gorsat.Iterators.CountingGorRowIterator
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.session.GorContext

class Gorrows() extends InputSourceInfo("GORROWS", CommandArguments("","-p -segment -step", 0, 0)) {

  override def processArguments(context: GorContext, argString: String, iargs: Array[String],
                                args: Array[String]): InputSourceParsingResult = {

    if (!hasOption(args, "-p")) {
      throw new GorParsingException("Position option '-p' is required, e.g.: gorrows -p chr1:1-1000")
    }

    val positionValue: GenomicRange.Range = parseAndValidatePositionOption(args)

    val segmentValue: Int = parseAndValidateIntValueOption(args, "-segment", minimumValue = 1, defaultValue = Int.MinValue)

    val stepValue: Int = parseAndValidateIntValueOption(args, "-step", minimumValue= 1, defaultValue = 1)

    val inputSource = CountingGorRowIterator(positionValue, segmentValue, stepValue)
    InputSourceParsingResult(inputSource, null, isNorContext = false)
  }

  private def parseAndValidatePositionOption(args: Array[String]) = {
    val positionOptionValue = rangeOfOption(args, "-p")

    CommandParseUtilities.testIntValueRange("start_position", positionOptionValue.start)

    if (positionOptionValue.stop == Integer.MAX_VALUE) {
      throw new GorParsingException(s"Error stop_position is required in the position option value '${stringValueOfOption(args, "-p")}', e.g.: gorrows -p chromosome:start_position-stop_position ")
    }

    CommandParseUtilities.testIntValueRange("stop_position", positionOptionValue.stop, positionOptionValue.start + 1)

    positionOptionValue
  }

  private def parseAndValidateIntValueOption(args: Array[String], option: String, minimumValue: Int = Int.MinValue, maximumValue: Int = Int.MaxValue, defaultValue :Int = Int.MinValue) = {
    val intValue = if (hasOption(args, option))
      CommandParseUtilities.intValueOfOptionWithDefaultWithRangeCheck(args, option, defaultValue, minimumValue, maximumValue)
    else
      defaultValue

    intValue
  }
}
