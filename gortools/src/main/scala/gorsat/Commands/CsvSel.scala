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

import gorsat.Analysis.GorCsvSel.CsvSelAnalysis
import gorsat.Commands.CommandParseUtilities._
import gorsat.DynIterator.DynamicNorSource
import gorsat.Utilities.IteratorUtilities.validHeader
import gorsat.PnBucketTable
import gorsat.process.SourceProvider
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.session.GorContext

import scala.collection.mutable

class CsvSel extends CommandInfo("CSVSEL",
  CommandArguments("-dose -vcf", "-gc -vs -s -tag -u -hide -threshold", 2),
  CommandOptions(gorCommand = true, norCommand = true, cancelCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {
    if(!(hasOption(args, "-s") || hasOption(args, "-vs"))) {
      throw new GorParsingException("Either a separator (-s) or a fixed value size (-vs) must be specified")
    }
    if(hasOption(args, "-s") && hasOption(args, "-vs")) {
      throw new GorParsingException("Either a separator (-s) or a fixed value size (-vs) must be specified, but not both")
    }

    var sepVal = replaceSingleQuotes(stringValueOfOptionWithDefault(args, "-s", ""))
    if (sepVal.length > 1) throw new GorParsingException("Separator must be a single character or empty char for fixed value size")

    var valSize = -1
    if (hasOption(args, "-vs")) {
      valSize = intValueOfOptionWithRangeCheck(args, "-vs", 1)
      sepVal = ""
    }

    val uv = stringValueOfOptionWithDefault(args, "-u","")

    val outputRows = hasOption(args, "-tag")

    val hideSome = hasOption(args, "-hide")

    val toVCF = hasOption(args, "-vcf")

    val hasThreshold = hasOption(args, "-threshold")

    if (hasThreshold && !toVCF) throw new GorParsingException("The -threshold option is only allowed together with -vcf")

    val vcfThreshold = if (hasThreshold) doubleValueOfOptionWithDefaultWithRangeCheck(args, "-threshold", 0.9, 0.0, 1.0) else -1.0

    val doseOption = hasOption(args, "-dose")

    if (doseOption && !toVCF) throw new GorParsingException("The -dose option is only allowed together with the -vcf option")

    if (toVCF && hideSome) throw new GorParsingException("The -hide option is not allowed together with the -vcf option.")

    if (hideSome && !outputRows) throw new GorParsingException("The -hide option is only allowed together with -tag option.")

    if (toVCF && vcfThreshold == -1 && !(valSize == 1 || valSize == -1)) throw new GorParsingException("The -vs can only take value 1 if -vcf is in use without threshold.")

    if (toVCF && vcfThreshold >= 0 && !(valSize == 2 || valSize == -1)) throw new GorParsingException("The -vs can only take value 2 if -vcf and -threshold are in use.")

    val inputHeader = forcedInputHeader

    val gcCols: List[Int] = columnsOfOptionWithNil(args, "-gc", inputHeader, executeNor).distinct
    val valCol: Int = columnFromHeader("values", inputHeader.toLowerCase, executeNor)
    val buckCol: Int = columnFromHeader("bucket", inputHeader.toLowerCase, executeNor)

    var iteratorCommand1 = ""
    var dsource1: DynamicNorSource = null
    var rightHeader1 = ""
    var iteratorCommand2 = ""
    var dsource2: DynamicNorSource = null
    var rightHeader2 = ""
    try {
      var rightFile1 = iargs(0).trim
      // Read a TSV file via nested quer to handle # in header properly
      if ((rightFile1.toUpperCase.endsWith(".NORZ") || rightFile1.toUpperCase.endsWith(".TSV") || rightFile1.toUpperCase.endsWith(".NOR")) && !(rightFile1.slice(0, 2) == "<(")) rightFile1 = "<(nor " + rightFile1 + " )"

      val inputSource1 = SourceProvider(rightFile1, context, executeNor = executeNor, isNor = true)
      iteratorCommand1 = inputSource1.iteratorCommand
      dsource1 = inputSource1.dynSource.asInstanceOf[DynamicNorSource]
      rightHeader1 = inputSource1.header

      var rightFile2 = iargs(1).trim
      if ((rightFile2.toUpperCase.endsWith(".NORZ") || rightFile2.toUpperCase.endsWith(".TSV") || rightFile2.toUpperCase.endsWith(".NOR")) && !(rightFile2.slice(0, 2) == "<(")) rightFile2 = "<(nor " + rightFile2 + " )"

      val inputSource2 = SourceProvider(rightFile2, context, executeNor = executeNor, isNor = true)
      iteratorCommand2 = inputSource2.iteratorCommand
      dsource2 = inputSource2.dynSource.asInstanceOf[DynamicNorSource]
      rightHeader2 = inputSource2.header


      if (rightHeader1.split("\t").length != 2) {
        throw new GorParsingException(s"buckettagfile must have 2 tab-delimited columns: Tag/PN (distinct), bucketID.\nThe relative position of tag in bucket specifies the csv order.\nCurrent header is: $rightHeader1")
      }

      if (rightHeader2.split("\t").length != 1) {
        throw new GorParsingException(s"outputtagfile must have a single column with distinct tag ids.\n\\nCurrent header is: $rightHeader2")
      }

      val toHide = if (hideSome) stringValueOfOption(args, "-hide").split(",").map(_.replace("\'","")).to[mutable.HashSet] else null
      val pipeStep = CsvSelAnalysis(rightFile1, iteratorCommand1, dsource1, rightFile2, iteratorCommand2, dsource2, buckCol, valCol, gcCols, sepVal, outputRows, hideSome, toHide, valSize, toVCF, vcfThreshold, doseOption, uv, context.getSession)

      val hcol = inputHeader.split("\t")
      val outputHeaderBuilder = new StringBuilder(hcol.slice(0, 2).mkString("\t"))
      gcCols.foreach(c => {
        outputHeaderBuilder.append("\t")
        outputHeaderBuilder.append(hcol(c))
      })
      if (outputRows) {
        outputHeaderBuilder.append('\t')
        outputHeaderBuilder.append(stringValueOfOption(args, "-tag"))
        outputHeaderBuilder.append("\tvalue")
      } else if (toVCF) {
        val tags = pipeStep.session.getCache.getObjectHashMap.get(pipeStep.lookupSignature).asInstanceOf[PnBucketTable].pnIdxToName
        outputHeaderBuilder.append("\tQUAL")
        outputHeaderBuilder.append("\tFILTER")
        outputHeaderBuilder.append("\tINFO")
        outputHeaderBuilder.append("\tFORMAT")
        tags.foreach(tag => {
          outputHeaderBuilder.append('\t')
          outputHeaderBuilder.append(tag)
        })
      } else {
        outputHeaderBuilder.append("\tvalues")
      }
      val combinedHeader = validHeader(outputHeaderBuilder.toString)
      CommandParsingResult(pipeStep, combinedHeader)

    } catch {
      case e: Exception =>
        if (dsource1 != null) dsource1.close()
        if (dsource2 != null) dsource2.close()
        throw e
    }
  }
}
