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

import gorsat.Analysis.PivotAnalysis.PivotAnalysis
import gorsat.Analysis.{PedigreeLookup, Select2}
import gorsat.Commands.CommandParseUtilities._
import gorsat.IteratorUtilities
import gorsat.IteratorUtilities.validHeader
import org.gorpipe.exceptions.{GorParsingException, GorResourceException}
import org.gorpipe.gor.GorContext

class PedPivot extends CommandInfo("PEDPIVOT",
  CommandArguments("-v -a", "-gc -e", 2, 2),
  CommandOptions(gorCommand = true, norCommand = true, verifyCommand = true, cancelCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {

    val pedigreeFile = iargs(1)

    val mh = IteratorUtilities.getFirstLine(pedigreeFile, context.getSession)
    val hCols = mh.split("\t", -1).length

    if (mh == null) {
      throw new GorResourceException("Failed to open pedigree file.", pedigreeFile)
    }

    if (hCols != 3) {
      throw new GorParsingException(s"Error in pedigree file - pedigreeFile $pedigreeFile does not have the necessary 3 columns (PN,FN,MN): ")
    }

    val emptyString = replaceSingleQuotes(stringValueOfOptionWithDefault(args, "-e","?"))

    val expandPedigree = hasOption(args, "-a")
    val verboseColumns = hasOption(args, "-v")

    var inputHeader = forcedInputHeader

    val personCol = columnFromHeader(iargs(0), inputHeader, executeNor)

    inputHeader += "\tpedpivotPersonGroup\tpedpivotPersonOrFatherOrMother"

    val pivotCol = columnFromHeader("pedpivotPersonOrFatherOrMother", inputHeader, executeNor)
    val personGroupCol = columnFromHeader("pedpivotPersonGroup", inputHeader, executeNor)

    var gCols = columnsOfOptionWithNil(args, "-gc", inputHeader, executeNor).distinct
    gCols ::= personGroupCol

    val pivotValues = Array("P", "F", "M")

    val hcol = inputHeader.split("\t")

    val oCols = hcol.indices filterNot (gCols ::: List(0, 1, pivotCol) contains)

    gCols = (List(0, 1) ::: gCols).distinct.sorted

    val sb = new StringBuilder
    sb.append(hcol(gCols.head))
    gCols.tail.foreach(x => {
      sb.append('\t')
      sb.append(hcol(x))
    })

    var combinedHeader = sb.toString()

    val columnPrefixes = if (verboseColumns) Array("Person", "Father", "Mother") else pivotValues
    columnPrefixes.foreach(v => {
      val otherCols = oCols.tail.map(hcol(_)).foldLeft(v + "_" + hcol(oCols.head)) (_ + "\t" + v + "_" + _)
      combinedHeader += "\t" + otherCols
    })

    combinedHeader = validHeader(combinedHeader)

    val newPersonGroupCol = columnFromHeader("pedpivotPersonGroup", combinedHeader, executeNor)

    val pickCols = (combinedHeader.split("\t").indices.toList filterNot (List(newPersonGroupCol) contains)).map(_ + 1)


    combinedHeader = combinedHeader.replace("\tpedpivotPersonGroup", "")


    if (hasOption(args, "-h")) combinedHeader = null

    val pipeStep: Analysis = PedigreeLookup(context.getSession, pedigreeFile, personCol, expandPedigree) | PivotAnalysis(gCols.toArray, pivotCol, pivotValues, oCols.toArray, emptyString, null) | Select2(pickCols: _*)

    CommandParsingResult(pipeStep, combinedHeader)
  }
}
