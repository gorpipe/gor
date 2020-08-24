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
import gorsat.Utilities.IteratorUtilities
import gorsat.process.SourceProvider
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.session.GorContext
import org.gorpipe.model.gor.iterators.RowSource

class GtGen extends CommandInfo("GTGEN",
  CommandArguments("", "-tag -gc -maxseg", 2, 2),
  CommandOptions(gorCommand = true, cancelCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {

    val leftHeader = forcedInputHeader

    val lhl = leftHeader.toLowerCase

    val PNCol = if (hasOption(args, "-tag")) {
      val pnCols: List[Int] = columnsOfOptionWithNil(args, "-tag", leftHeader, executeNor)
      if (pnCols == Nil || pnCols.length > 1) {
        throw new GorParsingException("Illegal -tag option: " +
          stringValueOfOption(args, "-tag") + "\n\nPlease specify a single column for -tag.")
      }
      pnCols.head
    } else {
      columnFromHeader("pn", lhl, executeNor)
    }

    val gtCol: Int = columnFromHeader("gt", lhl, executeNor)
    val gcCols: List[Int] = columnsOfOptionWithNil(args, "-gc", leftHeader, executeNor).distinct

    var buckTagItCommand = ""
    var buckTagDNS: DynamicNorSource = null
    var buckTagHeader = ""
    val buckTagFile = {
      val cand = iargs.head
      val cl = cand.toUpperCase
      if ((cl.endsWith(".NORZ") || cl.endsWith(".TSV") || cl.endsWith(".NOR")) && !(cl.slice(0, 2) == "<(")) "<(nor " + cand + " )" else cand
    }

    try {
      val buckTagSource = SourceProvider(buckTagFile, context, executeNor = executeNor, isNor = true)
      buckTagItCommand = buckTagSource.iteratorCommand
      buckTagDNS = buckTagSource.dynSource.asInstanceOf[DynamicNorSource]
      buckTagHeader = buckTagSource.header
    } catch {
      case e: Exception =>
        if (buckTagDNS != null) buckTagDNS.close()
        throw e
    }

    if (buckTagHeader.split("\t").length != 2) {
      throw new GorParsingException("buckettagfile must have 2 tab-delimited columns: Tag/PN (distinct), bucketID.\nThe relative position of tag in bucket specifies the csv order.\nCurrent header is: $buckTagHeader")
    }

    val segFile: String = iargs.last.trim
    var segSource: RowSource = null
    var rightHeader = ""
    try {
      val inputSource = new SourceProvider(segFile, context, executeNor = executeNor, isNor = false)
      segSource = inputSource.source
      rightHeader = inputSource.header
    } catch {
      case e: Exception =>
        if (segSource != null) segSource.close()
        throw e
    }
    val PNCol2 = if (hasOption(args, "-tag")) PNCol else columnFromHeader("pn", rightHeader.toLowerCase, executeNor)

    val hcol = leftHeader.split("\t")
    var outputHeader = hcol.slice(0, 2).mkString("\t")
    if (gcCols.nonEmpty) outputHeader += "\t" + gcCols.map(hcol(_)).mkString("\t")
    outputHeader += "\tBucket\tValues"

    val bucketCol =  columnFromHeader("bucket", outputHeader.toLowerCase, executeNor)
    var maxSegSize = 10000
    if (hasOption(args, "-maxseg")) maxSegSize = intValueOfOption(args, "-maxseg")
    val lookupSignature = buckTagFile + "#" + buckTagItCommand + "#" + segFile

    val combinedHeader = IteratorUtilities.validHeader(outputHeader)
    val pipeStep: Analysis = {
      GtGenAnalysis(buckTagFile, buckTagItCommand, buckTagDNS, gtCol, PNCol, gcCols, context, lookupSignature) | CoverageOverlap(segSource, bucketCol, PNCol2, maxSegSize, context, lookupSignature)
    }

    CommandParsingResult(pipeStep, combinedHeader)
  }
}

