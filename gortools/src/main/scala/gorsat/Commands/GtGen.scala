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

import gorsat.Analysis.GtGenAnalysis.{CoverageOverlap, GtGenAnalysis}
import gorsat.Commands.CommandParseUtilities._
import gorsat.DynIterator.DynamicNorSource
import gorsat.IteratorUtilities
import gorsat.process.SourceProvider
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.GorContext
import org.gorpipe.model.gor.iterators.RowSource

import scala.collection.mutable.ListBuffer

class GtGen extends CommandInfo("GTGEN",
  CommandArguments("-tag", "-gc -maxseg", 1, 2),
  CommandOptions(gorCommand = true, cancelCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {


    val inputHeader = forcedInputHeader

    var PNCol = -1
    if (hasOption(args, "-tag")) {
      val pnCols: List[Int] = columnsOfOptionWithNil(args, "-tag", inputHeader, executeNor)
      if (pnCols == Nil || pnCols.length > 1) {
        throw new GorParsingException("Illegal -tag option: " +
          stringValueOfOption(args, "-tag") + "\n\nPlease specify a single column for -tag.")
      }
      PNCol = pnCols.head
    } else {
      PNCol = columnFromHeader("pn", inputHeader.toLowerCase, executeNor)
    }

    val GtCol: Int = columnFromHeader("gt", inputHeader.toLowerCase, executeNor)

    val gcCols: List[Int] = columnsOfOptionWithNil(args, "-gc", inputHeader, executeNor).distinct


    var iteratorCommand1 = ""
    var dsource1: DynamicNorSource = null
    var rightheader1 = ""
    val iteratorCommand2 = ""
    val dsource2: DynamicNorSource = null
    var rightheader2 = ""
    var usePheno = false


    try {
      var rightFile1 = iargs(0).trim

      if ((rightFile1.toUpperCase.endsWith(".NORZ") || rightFile1.toUpperCase.endsWith(".TSV") || rightFile1.toUpperCase.endsWith(".NOR")) && !(rightFile1.slice(0, 2) == "<(")) rightFile1 = "<(nor " + rightFile1 + " )"

      val inputSource1 = SourceProvider(rightFile1, context, executeNor = executeNor, isNor = true)
      iteratorCommand1 = inputSource1.iteratorCommand
      dsource1 = inputSource1.dynSource.asInstanceOf[DynamicNorSource]
      rightheader1 = inputSource1.header

      if (rightheader1.split("\t").length != 2) {
        throw new GorParsingException("buckettagfile must have 2 tab-delimited columns: Tag/PN (distinct), bucketID.\nThe relative position of tag in bucket specifies the csv order.\nCurrent header is: $rightheader1")
      }

      var usedFiles = ListBuffer.empty[String]
      var rightFile2: String = null
      var stdInput: RowSource = null
      var segSource: RowSource = null
      var isSourceSet = false
      var rightHeader = ""
      rightFile2 = iargs(1).trim
      try {
        val inputSource = new SourceProvider(rightFile2, context, executeNor = executeNor, isNor = false)
        segSource = inputSource.source
        usedFiles = ListBuffer.empty[String] ++ inputSource.usedFiles
        rightHeader = inputSource.header
        isSourceSet = inputSource.dynSource.ne(null)
      } catch {
        case e: Exception =>
          if (segSource != null) segSource.close()
          throw e
      }
      var PNCol2 = -1
      if (hasOption(args, "-tag")) {
        val pnCols: List[Int] = columnsOfOptionWithNil(args, "-tag", rightHeader, executeNor)
        if (pnCols == Nil || pnCols.length > 1) {
          throw new GorParsingException("Illegal -tag option: " +
            stringValueOfOption(args, "-tag") + "\n\nPlease specify a single column for -tag.")
        }
        PNCol = pnCols.head
      } else {
        PNCol2 = columnFromHeader("pn", rightHeader.toLowerCase, executeNor)
      }


      val hcol = inputHeader.split("\t")
      var outputHeader = hcol.slice(0, 2).mkString("\t")
      for (c <- gcCols) outputHeader += "\t" + hcol(c)
      outputHeader += "\tBucket\tValues"


      val bucketCol =  columnFromHeader("bucket", outputHeader.toLowerCase, executeNor)
      var maxSegSize = 10000 //
      if (hasOption(args, "-maxseg")) maxSegSize = intValueOfOption(args, "-maxseg")
      val lookupSignature = rightFile1 + "#" + iteratorCommand1 + "#" + rightFile2 + "#" + iteratorCommand2

      val combinedHeader = IteratorUtilities.validHeader(outputHeader)
      val pipeStep: Analysis = GtGenAnalysis(rightFile1, iteratorCommand1, dsource1, rightFile2, iteratorCommand2, dsource2, GtCol, PNCol, gcCols, context.getSession) | CoverageOverlap(segSource, bucketCol, PNCol2, maxSegSize, context.getSession, lookupSignature)



      CommandParsingResult(pipeStep, combinedHeader);
    } catch {
      case e: Exception =>
        if (dsource1 != null) dsource1.close()
        if (dsource2 != null) dsource2.close()
        throw e
    }
  }
  
}
