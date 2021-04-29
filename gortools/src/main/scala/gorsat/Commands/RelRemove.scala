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

import gorsat.Analysis.{RelRemove}
import gorsat.Commands.CommandParseUtilities._
import gorsat.DynIterator.DynamicNorSource
import gorsat.Utilities.IteratorUtilities.validHeader
import gorsat.process.SourceProvider
import org.gorpipe.exceptions.{GorParsingException, GorResourceException}
import org.gorpipe.gor.session.GorContext

import scala.collection.mutable.ListBuffer

object RelRemoveCommand {
  class RelRemove extends CommandInfo("RELREMOVE",
    CommandArguments("-sepcc ", "-rsymb -weightcol", 0, 1),
    CommandOptions(gorCommand = false, norCommand = true)) {
    override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {
      processArgumentsRelRemove(context, argString, iargs, args,executeNor,  forcedInputHeader)
    }
  }

  def processArgumentsRelRemove(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {
    var negate = false
    var usedFiles = ListBuffer.empty[String]
    val mapFileName = iargs(0).trim
    val inputHeader = forcedInputHeader
    var iteratorCommand = ""
    var rightHeader = ""
    var dsource: DynamicNorSource = null

    val sepcc = if (hasOption(args, "-sepcc")) true else false
    var removeSymbol = ""
    if (hasOption(args, "-rsymb")) {
      removeSymbol = replaceSingleQuotes(stringValueOfOption(args, "-rsymb"))
    }

    val weightCols = columnsOfOptionWithNil(args, "-weightcol", inputHeader, executeNor).distinct

    try {
      var rightFile = iargs(0).trim

      if (rightFile.slice(0, 2) != "<(") {
        rightFile = "<(nor " + rightFile + " )"
      }

      val inputSource = SourceProvider(rightFile, context, executeNor = executeNor, isNor = true)
      iteratorCommand = inputSource.iteratorCommand
      dsource = inputSource.dynSource.asInstanceOf[DynamicNorSource]
      usedFiles = ListBuffer.empty[String] ++ inputSource.usedFiles
      rightHeader = inputSource.header

      if (rightHeader == null || rightHeader == "") {
        throw new GorResourceException("Cannot open the relationship file", mapFileName)
      }

      var combinedHeader = inputHeader
      var pipeStep: Analysis = null
      val colNum = inputHeader.split("\t").length-3

      combinedHeader = validHeader(combinedHeader /* +"\tIOOA\telim\trelsize" */)

      pipeStep = RelRemove(context.getSession, dsource, sepcc, removeSymbol, colNum, weightCols)

      CommandParsingResult(pipeStep, combinedHeader)
    } catch {
      case e: Exception =>
        if (dsource != null) dsource.close()
        throw e
    }
  }
}

