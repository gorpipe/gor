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

import gorsat.Analysis.GorCsvCC.CsvCCAnalysis
import gorsat.Commands.CommandParseUtilities._
import gorsat.DynIterator.DynamicNorSource
import gorsat.Utilities.IteratorUtilities.validHeader
import gorsat.process.SourceProvider
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.session.GorContext

class CsvCC extends CommandInfo("CSVCC",
  CommandArguments("-probunphased -probphased", "-gc -vs -s -u -threshold", 2, 2),
  CommandOptions(gorCommand = true, norCommand = true, cancelCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {

    var sepVal =  replaceSingleQuotes(stringValueOfOptionWithDefault(args, "-s",","))
    if (sepVal.length > 1) throw new GorParsingException("Separator must be a single character or empty char for fixed value size")

    var valSize = -1
    if (hasOption(args, "-vs")) {
      valSize = intValueOfOptionWithRangeCheck(args, "-vs", 1)
      sepVal = ""
    }

    var use_threshold = false
    var p_threshold = 0.95
    var use_prob = false
    var use_phase = false

    if (hasOption(args, "-probunphased")) use_prob = true
    if (hasOption(args, "-probphased")) { use_phase = true; use_prob = true }
    if (hasOption(args, "-threshold")) { p_threshold = doubleValueOfOptionWithRangeCheck(args, "-threshold", 0.5, 1.0); use_threshold = true }

    val inputHeader = forcedInputHeader

    val gcCols: List[Int] = columnsOfOptionWithNil(args, "-gc", inputHeader, executeNor).distinct
    val valCol: Int = columnFromHeader("values", inputHeader.toLowerCase, executeNor)
    val buckCol: Int = columnFromHeader("bucket", inputHeader.toLowerCase, executeNor)

    var iteratorCommand1 = ""
    var dsource1: DynamicNorSource = null
    var rightheader1 = ""
    var iteratorCommand2 = ""
    var dsource2: DynamicNorSource = null
    var rightheader2 = ""
    var usePheno = false

    val uv = replaceSingleQuotes(stringValueOfOptionWithDefault(args, "-u","3"))

    if (hasOption(args, "-u") && !(uv == "0" || uv == "3" || uv == "4" || uv == "  ")) throw new GorParsingException("Missing data value must be either 0 (hzr), 3 (unk), 4 (other) or '  ' (double spaces).")
    try {
      var rightFile1 = iargs(0).trim

      if ((rightFile1.toUpperCase.endsWith(".NORZ") || rightFile1.toUpperCase.endsWith(".TSV") || rightFile1.toUpperCase.endsWith(".NOR")) && !(rightFile1.slice(0, 2) == "<(")) rightFile1 = "<(nor " + rightFile1 + " )"

      val inputSource1 = SourceProvider(rightFile1, context, executeNor = executeNor, isNor = true)
      iteratorCommand1 = inputSource1.iteratorCommand
      dsource1 = inputSource1.dynSource.asInstanceOf[DynamicNorSource]
      rightheader1 = inputSource1.header

      var rightFile2 = iargs(1).trim
      if ((rightFile2.toUpperCase.endsWith(".NORZ") || rightFile2.toUpperCase.endsWith(".TSV") || rightFile2.toUpperCase.endsWith(".NOR")) && !(rightFile2.slice(0, 2) == "<(")) rightFile2 = "<(nor " + rightFile2 + " )"

      val inputSource2 = SourceProvider(rightFile2, context, executeNor = executeNor, isNor = true)
      iteratorCommand2 = inputSource2.iteratorCommand
      dsource2 = inputSource2.dynSource.asInstanceOf[DynamicNorSource]
      rightheader2 = inputSource2.header



      if (rightheader1.split("\t").length != 2) {
        throw new GorParsingException(s"buckettagfile must have 2 tab-delimited columns: Tag/PN (distinct), bucketID.\nThe relative position of tag in bucket specifies the csv order.\nCurrent header is: $rightheader1")
      }

      if (!(rightheader2.split("\t").length == 2 || rightheader2.split("\t").length == 3)) {
        throw new GorParsingException(s"outputtagfile must have two or three columns with (tag,pheno,cc-status) or (tag,cc-status).\nCurrent header is: ${rightheader2.replace('\t', ',')}")
      }
      if (rightheader2.split("\t").length == 3) usePheno = true

      val hcol = inputHeader.split("\t")
      var outputHeader = hcol.slice(0, 2).mkString("\t")
      for (c <- gcCols) outputHeader += "\t" + hcol(c)
      if (usePheno) {
        outputHeader += "\tPheno\tCC\tGT\tGTcount"
      } else {
        outputHeader += "\tCC\tGT\tGTcount"
      }

      val combinedHeader = validHeader(outputHeader)
      val pipeStep: Analysis = CsvCCAnalysis(rightFile1, iteratorCommand1, dsource1, rightFile2, iteratorCommand2, dsource2, buckCol, valCol, gcCols, sepVal, valSize, uv, use_phase, use_prob, use_threshold, p_threshold, context.getSession)

      CommandParsingResult(pipeStep, combinedHeader)
    } catch {
      case e: Exception =>
        if (dsource1 != null) dsource1.close()
        if (dsource2 != null) dsource2.close()
        throw e
    }
  }
}
