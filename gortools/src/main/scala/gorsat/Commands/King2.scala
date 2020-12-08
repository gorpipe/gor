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

import gorsat.Analysis.GorKing.{KingAggregate, KingAnalysis}
import gorsat.Commands.CommandParseUtilities._
import gorsat.DynIterator.DynamicNorSource
import gorsat.Utilities.IteratorUtilities.validHeader
import gorsat.process.SourceProvider
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.session.GorContext

class King2 extends CommandInfo("KING2",
  CommandArguments("", "-gc -vs -s -pi0thr -phithr -thetathr", 2),
  CommandOptions(gorCommand = true, cancelCommand = true))
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

    var pi0thr = 0.0f
    var t_pi0 = false
    if (hasOption(args, "-pi0thr")) {
      pi0thr = doubleValueOfOption(args, "-pi0thr").toFloat
      t_pi0 = true
    }

    var phithr = 0.0f
    var t_phi = false
    if (hasOption(args, "-phithr")) {
      phithr = doubleValueOfOption(args, "-phithr").toFloat
      t_phi = true
    }

    var thetathr = 0.0f
    var t_theta = false
    if (hasOption(args, "-thetathr")) {
      pi0thr = doubleValueOfOption(args, "-thetathr").toFloat
      t_theta = true
    }

    val uv = "3"

    val inputHeader = forcedInputHeader

    val gcCols: List[Int] = columnsOfOptionWithNil(args, "-gc", inputHeader, executeNor).distinct
    val valCol: Int = columnFromHeader("values", inputHeader.toLowerCase, executeNor)
    val buckCol: Int = columnFromHeader("bucket", inputHeader.toLowerCase, executeNor)
    val afCol: Int = columnFromHeader("af", inputHeader.toLowerCase, executeNor)
    if (!gcCols.exists(_ == afCol)) {
      throw new GorParsingException(s"There must be a AF column in the -gc column list.")
    }
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

      if (rightHeader2.split("\t").length != 2) {
        throw new GorParsingException(s"The tagfile must have two columns with all the PN/tag pairs to test, e.g. (PN1,PN2).\n\\nCurrent header is: $rightHeader2")
      }



      val pipeStep = KingAnalysis(rightFile1, iteratorCommand1, dsource1, rightFile2, iteratorCommand2, dsource2, buckCol, valCol, gcCols, afCol, sepVal, valSize, uv, context.getSession) | KingAggregate(pi0thr,phithr,thetathr,t_pi0,t_phi,t_theta,context.getSession.getSystemContext.getMonitor)

      val combinedHeader = validHeader("Chrom\tPos\tPN1\tPN2\tIBS0\tXX\ttpq\tkpq\tNhet\tNhom\tNAai\tNAaj\tcount\tpi0\tphi\ttheta")
      CommandParsingResult(pipeStep, combinedHeader)

    } catch {
      case e: Exception =>
        if (dsource1 != null) dsource1.close()
        if (dsource2 != null) dsource2.close()
        throw e
    }
  }
}
