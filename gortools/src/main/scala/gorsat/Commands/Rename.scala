/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
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

import gorsat.Commands.CommandParseUtilities._
import gorsat.Utilities.IteratorUtilities.validHeader
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.session.GorContext

class Rename extends CommandInfo("RENAME",
  CommandArguments("-s", "", 2, -1, ignoreIllegalArguments = true),
  CommandOptions(gorCommand = true, norCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {
    val colName = iargs(0)
    val newName = iargs(1)
    val cols = forcedInputHeader.split("\t")
    val strict = hasOption(args, "-s")
    var foundMatch = false
    for (i <- 0 until cols.size) {
      val R = ("(?i)" + colName).r
      cols(i) match {
        case R(b1, b2, b3, b4, b5, b6) =>
          foundMatch = true
          cols(i) = replaceParts(newName, Array(b1, b2, b3, b4, b5, b6))
        case R(b1, b2, b3, b4, b5) =>
          foundMatch = true
          cols(i) = replaceParts(newName, Array(b1, b2, b3, b4, b5))
        case R(b1, b2, b3, b4) =>
          foundMatch = true
          cols(i) = replaceParts(newName, Array(b1, b2, b3, b4))
        case R(b1, b2, b3) =>
          foundMatch = true
          cols(i) = replaceParts(newName, Array(b1, b2, b3))
        case R(b1, b2) =>
          foundMatch = true
          cols(i) = replaceParts(newName, Array(b1, b2))
        case R(b1) =>
          foundMatch = true
          cols(i) = replaceParts(newName, Array(b1))
        case _ =>
          if (!(newName.contains("#{") || newName.contains( """${"""))) {
            val colNum: Int = getColumnIndex(colName, forcedInputHeader, executeNor)
            cols(colNum) = newName
            foundMatch = true
          }
      }
    }

    if (strict && !foundMatch) {
      throw new GorParsingException("No column found that matches expression")
    }

    CommandParsingResult(null, validHeader(cols.mkString("\t")))
  }

  private def getColumnIndex(colName: String, header: String, forNor: Boolean) = {
    val columnIndices = columnsFromHeader(colName, header, forNor)
    if (columnIndices.isEmpty) {
      throw new GorParsingException("No column found")
    }
    if (columnIndices.length > 1) {
      throw new GorParsingException("Column range not allowed for RENAME - use a regular expression")
    }
    val colNum = columnIndices.head
    colNum
  }

  def replaceParts(old: String, newValues: Array[String]): String = {
    var result = old
    newValues.zipWithIndex.foreach {
      case(v, ix) =>
        result = replacePart(result, ix+1, v)
    }
    checkExtraBindings(result)
  }

  private def checkExtraBindings(result: String): String = {
    val r = ".*([$#]\\{\\d\\}).*".r
    result match {
      case r(ix) => throw new GorParsingException(s"Invalid regex binding $ix")
      case _ =>
    }
    result
  }

  def replacePart(old: String, ix: Int, newValue: String): String = {
    val target = s"{$ix}"
    old.replace("$"+target, newValue).replace("#"+target, newValue)
  }
}

