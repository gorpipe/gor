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

import gorsat.IteratorUtilities
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.GorContext

/**
  * ColumnSelection represents a selection of columns from the given header. The selection can be specified
  * in one of three ways:
  * <ul>
  *   <li>Range
  *   <li>List of columns
  *   <li>External list, as a file reference or a nested nor query
  * </ul>
  * <h2>Range</h2>
  * The range is specified as
  * <i>first</i>-<i>last</i>
  * where first/last can be a column name or its index (one-based). Last can also be empty,
  * interpreted as the last column.
  * <h2>List of columns</h2>
  * A comma (or semicolon) separated list of column names (or indices) or ranges, for example:<br>
  *   pos,first-last,5,7<br>
  * <h2>External list</h2>
  * The name of a file with a column reference per line, or a nor query that returns single column
  * with column references per line.
  * @param inputHeader A tab-separated list of column names
  * @param selection A selection expression
  * @param context A GorContext instance
  * @param forNor Is this a nor query?
  */
case class ColumnSelection(
                            inputHeader: String,
                            selection: String,
                            context: GorContext,
                            forNor: Boolean = false
) {

  /**
    * Gets a list of columns this selection refers to. Note that this collapses a range into
    * a list - for a large range it may be more appropriate to check if the selection is a
    * range and treat as such.
    * A side effect of this function is to collapse the range or perform the query, populating
    * the list.
    *
    * @return A list of column indices
    */
  def columns: List[Int] = {
    if(isQuery) readColumnsFromQuery()
    if(isRange) populateColumnsFromRange()

    columnValues
  }

  /**
    * Gets a the list of columns in a header string format.
    * @return A string representing the header for the selected columns
    */
  def header: String = {
    val range = if (isRange) {
      firstInRange to lastInRange
    } else {
      columns
    }
    val headerArray = inputHeader.split('\t')

    range.map(col => headerArray(col)).mkString("\t")
  }

  def addPosColumns(): Unit = {
    if(isRange) populateColumnsFromRange()
    columnValues = (List(0, 1) ::: columnValues).distinct
    isEmpty = false
  }

  var isEmpty: Boolean = true
  var isRange: Boolean = false
  var isList: Boolean = false
  var isQuery: Boolean = false
  var firstInRange: Int = -1
  var lastInRange: Int = -1
  private var columnValues: List[Int] = List[Int]()

  parseSelection()

  private def parseSelection(): Unit = {
    if(selection == "") return

    if(selection.startsWith("<(") && selection.endsWith(")")) {
      // Treat selection as a nested query
      isQuery = true
      isEmpty = false
      return
    }

    val cols = selection.split("[,;]")
    if(cols.length > 1 || selection.contains('*')) {
      // Treat selection as a comma (or semicolon) separated list
      isList = true
      isEmpty = false
      columnValues = CommandParseUtilities.columnsFromHeader(selection, inputHeader, forNor)
    } else {
      // Treat selection as a range
      val headerArray = inputHeader.toUpperCase.split('\t')
      val headerIndexMap = headerArray.zipWithIndex.toMap
      val su: String = selection.toUpperCase
      val sanitizedSelection = su.replace("#", "").replace("$", "")
      val parts = CommandParseUtilities.quoteSquareBracketsSafeSplit(sanitizedSelection, '-')
      try {
        firstInRange = CommandParseUtilities.columnNumber(parts(0), headerIndexMap, headerArray, forNor) - 1
        if(parts.length > 1) {
          lastInRange = CommandParseUtilities.columnNumber(parts(1), headerIndexMap, headerArray, forNor) - 1
        } else {
          lastInRange = if(sanitizedSelection.contains("-")) headerArray.length - 1 else firstInRange
        }
        isRange = true
        isEmpty = false
      } catch {
        case e: GorParsingException =>
          // Selection could not be parsed as a range, assume it is a file reference
          isQuery = true
          isEmpty = false
      }
    }
  }

  private def populateColumnsFromRange(): Unit = {
    columnValues = (firstInRange to lastInRange).toList
    isList = true
    isRange = false
  }

  private def readColumnsFromQuery(): Unit = {
    val rawInput = IteratorUtilities.getStringArrayFromFileOrNestedQuery(selection, context)

    val headerArray = inputHeader.toUpperCase.split('\t')
    val headerIndexMap = headerArray.zipWithIndex.toMap
    columnValues = rawInput.map(x => CommandParseUtilities.columnNumber(x, headerIndexMap, headerArray, forNor) - 1)
      .toList
    isList = true
    isQuery = false
  }
}
