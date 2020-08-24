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
import gorsat.Iterators.RowListIterator
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.driver.providers.stream.datatypes.gor.GorHeader
import org.gorpipe.gor.session.GorContext
import org.gorpipe.model.gor.RowObj

class Gorrow() extends InputSourceInfo("GORROW", CommandArguments("","", 1, 1)) {

  override def processArguments(context: GorContext, argString: String, iargs: Array[String],
                                args: Array[String]): InputSourceParsingResult = {
    val gorrowParams = iargs(0).split(',')

    if (gorrowParams.length < 2 || gorrowParams.length > 3) {
      throw new GorParsingException("Error in number of columns - Two or three columns are required: chromosome,position or chromosome,start_position,stop_position")
    }

    var chromRowValue = gorrowParams(0)
    if (!chromRowValue.startsWith("chr")) {
      chromRowValue = "chr" + chromRowValue
    }

    var rowValues: String = chromRowValue
    var headerValues: String = "chrom"

    var gorHeader = new GorHeader()
    gorHeader.addColumn("chrom", "S")

    if (gorrowParams.length == 2) {
      val posValue = CommandParseUtilities.parseIntWithRangeCheck("position", gorrowParams(1))

      rowValues += "\t" + posValue
      headerValues += "\tpos"
      gorHeader.addColumn("pos", "I")
    } else {
      val start = CommandParseUtilities.parseIntWithRangeCheck("start_position", gorrowParams(1))
      val stop = CommandParseUtilities.parseIntWithRangeCheck("stop_position", gorrowParams(2), start)

      rowValues += "\t" + start + "\t" + stop
      headerValues += "\tbpStart\tbpStop"
      gorHeader.addColumn("bpStart", "I")
      gorHeader.addColumn("bpStop", "I")
    }

    val lRows = List(RowObj(rowValues))
    val inputSource = RowListIterator(lRows)
    inputSource.setHeader(headerValues)
    inputSource.setGorHeader(gorHeader)

    InputSourceParsingResult(inputSource, headerValues, isNorContext = false)
  }
}
