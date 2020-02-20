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

import gorsat.Analysis.MergeSources
import gorsat.Commands.CommandParseUtilities._
import gorsat.IteratorUtilities.validHeader
import gorsat.process.SourceProvider
import org.gorpipe.gor.GorContext

class Merge extends CommandInfo("MERGE",
  CommandArguments("-u -s -i", "-e", 1, 1),
  CommandOptions(gorCommand = true, norCommand = true, verifyCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {
    val emptyString = replaceSingleQuotes(stringValueOfOptionWithDefault(args, "-e", ""))
    val inputSource = new SourceProvider(iargs(0).trim, context, executeNor = executeNor, isNor = false)
    val segSource = inputSource.source
    try {
      val lCols = forcedInputHeader.split("\t", -1).toList.slice(2, 1000)
      val rCols = segSource.getHeader.split("\t", -1).toList.slice(2, 1000)
      val same = if (lCols.toString.toUpperCase == rCols.toString.toUpperCase) true else false
      val iCols = rCols.filter(x => lCols.map(_.toUpperCase).indexOf(x.toUpperCase) >= 0)
      val rColsExtra = rCols.filter(x => iCols.map(_.toUpperCase).indexOf(x.toUpperCase) < 0)

      var combinedHeader = ""

      if (hasOption(args, "-i")) {
        combinedHeader = forcedInputHeader.split("\t", -1).slice(0, 2).mkString("\t") + (if (iCols.nonEmpty) "\t" else "") + iCols.mkString("\t")
      } else {
        combinedHeader = forcedInputHeader + (if (rColsExtra.nonEmpty) "\t" else "") + rColsExtra.mkString("\t")
      }

      val lPickCols = List(0, 1) ::: combinedHeader.split("\t", -1).map(x => lCols.map(_.toUpperCase).indexOf(x.toUpperCase)).toList.slice(2, 1000).map(x => if (x >= 0) x + 2 else x)
      val rPickCols = List(0, 1) ::: combinedHeader.split("\t", -1).map(x => rCols.map(_.toUpperCase).indexOf(x.toUpperCase)).toList.slice(2, 1000).map(x => if (x >= 0) x + 2 else x)

      val addRightLeft = hasOption(args, "-s")

      if (addRightLeft) combinedHeader = validHeader(combinedHeader + "\tSource")

      val pipeStep: Analysis = MergeSources(segSource, emptyString, addRightLeft, lPickCols.toArray, rPickCols.toArray, same)

      CommandParsingResult(pipeStep, combinedHeader, inputSource.usedFiles)
    } catch {
      case e: Exception => if (segSource != null) segSource.close(); throw e
    }

  }
}
