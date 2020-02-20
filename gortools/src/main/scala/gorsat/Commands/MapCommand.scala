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

import gorsat.Analysis.{MapLookup, MultiMapLookup}
import gorsat.Commands.CommandParseUtilities._
import gorsat.DynIterator.DynamicNorSource
import gorsat.IteratorUtilities.validHeader
import gorsat.process.SourceProvider
import org.gorpipe.exceptions.{GorParsingException, GorResourceException}
import org.gorpipe.gor.GorContext

import scala.collection.mutable.ListBuffer

object MapCommand {
  class Map extends CommandInfo("MAP",
    CommandArguments("-b -h -e -cis -not -cartesian -l", "-c -m -n", 1, 1),
    CommandOptions(gorCommand = true, norCommand = true)) {
    override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {
      processArgumentsMapAndMultiMap(context, argString, iargs, args,executeNor,  forcedInputHeader)
    }
  }

  def processArgumentsMapAndMultiMap(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {
    var negate = false
    var usedFiles = ListBuffer.empty[String]
    val mapFileName = iargs(0).trim
    val inputHeader = forcedInputHeader
    var iteratorCommand = ""
    var rightHeader = ""
    var dsource: DynamicNorSource = null
    try {
      var rightFile = iargs(0).trim
      // Read a TSV file via nested quer to handle # in header properly
      val headerOption = if (hasOption(args, "-h") || hasOption(args, "-n")) "-h " else ""
      if (rightFile.slice(0, 2) != "<(") {
        rightFile = "<(nor " + headerOption + rightFile + " )"
      }

      val inputSource = SourceProvider(rightFile, context, executeNor = executeNor, isNor = true)
      iteratorCommand = inputSource.iteratorCommand
      dsource = inputSource.dynSource.asInstanceOf[DynamicNorSource]
      usedFiles = ListBuffer.empty[String] ++ inputSource.usedFiles
      rightHeader = inputSource.header

      if (rightHeader == null || rightHeader == "") {
        throw new GorResourceException("Cannot open the map file", mapFileName)
      }
      val hCols = rightHeader.split("\t", -1).length
      val mh = rightHeader

      val cartesian = hasOption(args, "-cartesian")
      val skipEmpty = hasOption(args, "-e")

      var mCols: List[Int] = columnsOfOptionWithNil(args, "-c", inputHeader, executeNor)

      if (mCols.isEmpty && !cartesian) {
        throw new GorParsingException("One or more columns must be specified for mapping lookup!!!.", "-c", stringValueOfOption(args, "-c"))
      }

      val outColNum = hCols - mCols.length
      if (cartesian) {
        if (mCols != Nil) mCols = mCols.drop(1)
      }

      var missingVal = ""
      var returnMissing = false
      if (hasOption(args, "-m")) {
        missingVal = replaceSingleQuotes(stringValueOfOption(args, "-m"))
        returnMissing = true
      }

      val inSet = hasOption(args, "-s")
      var inSetCol = false
      if (hasOption(args, "-b")) {
        inSetCol = true
        missingVal = "0"
      }

      val caseInsensitive = hasOption(args, "-cis")
      var outCols = if (hasOption(args, "-b")) "inSet" else "mVal"

      var actualOutCols: List[Int] = Nil

      if (hasOption(args, "-n")) {
        outCols = stringValueOfOption(args, "-n").replace(",", "\t")
        actualOutCols = columnsOfOption(args, "-n", mh, executeNor)
      } else {
        if (!inSet) {
          val headerColList = mh.split("\t").slice(mCols.length, 10000).mkString(",")
          if (headerColList.isEmpty) {
            throw new GorParsingException("The number of columns in the lookup file are too few. No valid output column found.")
          }
          outCols = headerColList.replace(",", "\t")
          actualOutCols = columnsFromHeader(headerColList, mh, executeNor)
        } else {
          if (outColNum == 1) {
            outCols = "mVal"
            actualOutCols = List(1)
          } else {
            outCols = Range(1, outColNum + 1).map(x => "mVal" + x).mkString("\t")
            actualOutCols = Range(mCols.length, mCols.length + outColNum).toList
          }
        }
      }

      var combinedHeader = ""
      var pipeStep: Analysis = null

      missingVal = outCols.split("\t", -1).map(x => missingVal).mkString("\t")

      if (!inSet) {
        var message = ""
        if (outColNum < actualOutCols.length) {
          message = "The number of columns in the lookup file are too few:\nFirst line in lookup file:" + mh.replace("\t", ",") + "\nlookup cols: " +
            mCols.map(x => inputHeader.split("\t", -1)(x)).mkString(",") + "\noutput cols: " + outCols.replace("\t", ",")
        } else if (actualOutCols.isEmpty) {
          message = "The number of columns in the lookup file are too few. No valid output column found."
        }

        if (!message.isEmpty) {
          throw new GorParsingException(message)
        }
      }
      negate = hasOption(args, "-not")
      if (inSet && hasOption(args, "-b")) {
        outCols = "inSet"
        if (negate) missingVal = "1" else missingVal = "0"
      }

      if (inSet && !inSetCol) {
        combinedHeader = inputHeader
      } else {
        combinedHeader = inputHeader + "\t" + outCols
      }

      combinedHeader = validHeader(combinedHeader)

      if (hasOption(args, "-l") && !(cartesian && actualOutCols.length == 1) || (cartesian && actualOutCols.length > 1)) {
        pipeStep = MultiMapLookup(context.getSession, iteratorCommand, dsource, mapFileName, mCols.toArray,
          caseInsensitive, actualOutCols.toArray, missingVal, returnMissing, cartesian)
      }
      else {
        pipeStep = MapLookup(context.getSession, iteratorCommand, dsource, mapFileName, mCols.toArray, negate,
          caseInsensitive, actualOutCols.toArray, missingVal, returnMissing, inSet, inSetCol, cartesian, skipEmpty)
      }

      CommandParsingResult(pipeStep, combinedHeader)
    } catch {
      case e: Exception =>
        if (dsource != null) dsource.close()
        throw e
    }
  }
}