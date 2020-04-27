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

import gorsat.Analysis.Select2
import gorsat.Commands.CommandParseUtilities._
import gorsat.IteratorUtilities.validHeader
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.GorContext

object Select {

  def parseArguments(iargs : Array[String], args : Array[String], executeNor : Boolean, forcedInputHeader : String = "") : CommandParsingResult = {

    val allArgs = iargs.mkString(",")

    var ignoreNonExisting = false
    if (hasOption(args,"-t")) {
      ignoreNonExisting = true
    }

    var useCols = (if (executeNor) List(0,1) else Nil) ::: columnsFromHeader(allArgs,forcedInputHeader,executeNor,ignoreNonExisting)

    var pickCols = useCols.map( _ + 1 )
    val pickColsSet = pickCols.toSet

    if (hasOption(args,"-s")) {
      val colNum = forcedInputHeader.split("\t",-1).length
      pickCols = List(1,2) ::: (Range(3,colNum+1).toList filterNot (pickColsSet contains))
      useCols = pickCols.map( _ - 1 )
    }

    if (hasOption(args,"-sort")) {
      val colNames = forcedInputHeader.split("\t",-1)
      val colNum = colNames.length
      if (pickCols.length < 3) pickCols = List(1,2)
      val sortCols = Range(1,colNum+1).toList filterNot (pickColsSet contains)
      pickCols = pickCols ::: sortCols.map(x => (x,colNames(x-1))).sortWith( (x,y) => x._2.toUpperCase < y._2.toUpperCase ).map(x => x._1)
      useCols = pickCols.map( _ - 1 )
    }

    var combinedHeader: String = null

    val hcols = forcedInputHeader.split("\t",-1)

    try {
      val sb = new StringBuilder()
      sb.append(hcols(useCols.head))
      useCols.tail.foreach(n => {
        sb.append("\t")
        sb.append(hcols(n))
      })
      combinedHeader = sb.toString
    } catch {
      case iobe:IndexOutOfBoundsException =>
        throw new GorParsingException("Failed to merge columns in select statement. Header: " + forcedInputHeader, iobe)
    }

    combinedHeader = validHeader(combinedHeader)

    if (hasOption(args,"-h")) combinedHeader = null

    val pipeStep:Analysis = Select2(pickCols:_*)

    CommandParsingResult(pipeStep, combinedHeader)
  }

  class Select extends CommandInfo("SELECT",
    CommandArguments("-s -t -sort", "", 1, -1, ignoreIllegalArguments = true),
    CommandOptions(gorCommand = true, norCommand = true))
  {
    override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {
      parseArguments(iargs, args, executeNor, forcedInputHeader)
    }
  }
}
