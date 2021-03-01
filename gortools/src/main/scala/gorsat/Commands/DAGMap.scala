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

import gorsat.Analysis.DagMapAnalysis.DAGMultiMapLookup
import gorsat.Commands.CommandParseUtilities._
import gorsat.DynIterator.DynamicNorSource
import gorsat.Utilities.IteratorUtilities.validHeader
import gorsat.process.SourceProvider
import org.gorpipe.exceptions.{GorParsingException, GorResourceException}
import org.gorpipe.gor.session.GorContext

import scala.collection.mutable.ListBuffer

class DAGMap extends CommandInfo("DAGMAP",
  CommandArguments("-cis -dp", "-c -m -ps -dl", 1, 1),
  CommandOptions(gorCommand = true, norCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {
    var usedFiles = ListBuffer.empty[String]
    val mapFileName = iargs(0).trim
    val inputHeader = forcedInputHeader
    var iteratorCommand = ""
    var rightHeader = ""
    var dsource: DynamicNorSource = null
    try {
      var rightFile = iargs(0).trim
      val rightFileUpper = rightFile.toUpperCase
      val extensionstoWrap = Array(".NORZ", ".NOR", ".TSV")
      // Read a TSV file via nested quer to handle # in header properly
      if (extensionstoWrap.exists(x => rightFileUpper.endsWith(x)) && rightFile.slice(0, 2) != "<(") {
        rightFile = "<(nor " + rightFile + " )"
      }

      val inputSource = SourceProvider(rightFile, context, executeNor = executeNor, isNor = true)
      iteratorCommand = inputSource.iteratorCommand
      dsource = inputSource.dynSource.asInstanceOf[DynamicNorSource]
      usedFiles = ListBuffer.empty[String] ++ inputSource.usedFiles
      rightHeader = inputSource.header

      if (rightHeader == null || rightHeader == "") {
        throw new GorResourceException("Cannot open the map file", mapFileName)
      }
      val mCols: List[Int] = columnsOfOptionWithNil(args, "-c", inputHeader, executeNor)

      if (mCols.isEmpty) {
        throw new GorParsingException("One or more columns must be specified for mapping lookup!!!.", "-c", stringValueOfOption(args, "-c"))
      }

      var missingVal = ""
      var returnMissing = false
      if (hasOption(args, "-m")) {
        missingVal = replaceSingleQuotes(stringValueOfOption(args, "-m"))
        returnMissing = true
      }

      val caseInsensitive = hasOption(args, "-cis")

      var combinedHeader = ""
      var pipeStep: Analysis = null

      // Test if we are showing the DAG path
      var showDAGPath = false
      var dagPathHeader = ""
      if (hasOption(args, "-dp")) {
        showDAGPath = true
        dagPathHeader = "\tDAG_path"
      }

      // Test if a custom path seperator is defined
      var pathSeparator = "->"
      if (hasOption(args, "-ps")) {
        pathSeparator = replaceSingleQuotes(stringValueOfOption(args, "-ps"))
      }

      // Test if DAG level is defined
      var dagLevel = 20
      if (hasOption(args, "-dl")) {
        dagLevel = intValueOfOptionWithRangeCheck(args, "-dl", 0)
      }

      val columnHeader = "DAG_node\tDAG_dist" + dagPathHeader

      combinedHeader = validHeader(inputHeader + "\t" + columnHeader)
      val missingCols = missingVal + "\t-1" + (if (showDAGPath) "\t" + missingVal else "")
      pipeStep = DAGMultiMapLookup(context.getSession, iteratorCommand, dsource, mapFileName, mCols, caseInsensitive, missingCols, returnMissing, showDAGPath, pathSeparator, dagLevel)

      CommandParsingResult(pipeStep, combinedHeader)
    } catch {
      case e: Exception =>
        if (dsource != null) dsource.close()
        throw e
    }
  }
}
