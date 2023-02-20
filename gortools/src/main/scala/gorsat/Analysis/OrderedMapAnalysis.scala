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

package gorsat.Analysis

import gorsat.Commands.Analysis
import gorsat.DynIterator.DynamicNorSource
import org.gorpipe.exceptions.GorDataException
import org.gorpipe.gor.model.Row
import org.gorpipe.gor.session.GorSession

import scala.collection.mutable.ArrayBuffer

case class OrderedMapAnalysis(session: GorSession,
                              rightSource: DynamicNorSource,
                              columns: Array[Int],
                              negate: Boolean,
                              caseInsensitive: Boolean,
                              outCols: Array[Int],
                              missingVal: String,
                              returnMissing: Boolean,
                              inSet: Boolean,
                              inSetCol: Boolean,
                              skipEmpty: Boolean,
                              multipleRows: Boolean
                             ) extends Analysis {

  private val singleCol: Boolean = columns.length == 1
  private var currentKey: String = _
  private var rightKey: String = _
  private var rightValues: Array[Row] = _
  private var rightRow: Row = _
  private var nextRightRow: Row = _
  private val adjustedOutCols: Array[Int] = outCols.map(x => x+2)
  private val lookupIndices: Array[Int] = Array.range(0, columns.length).map(x => x+2) // The indices into the lookup table are always the first n.

  override def setup(): Unit = {
    if (rightSource.hasNext) {
      rightRow = rightSource.next()
      rightKey = getKeyFromRightRow(rightRow, rightKey)
    }
  }

  override def process(r: Row): Unit = {
    currentKey = getKeyFromRow(r, currentKey)
    while (rightHasNext && currentKey > rightKey) {
      advanceRight()
    }

    if (currentKey.equals(rightKey)) {
      if (inSet) {
        if (inSetCol) {
          val x = if (negate) "0" else "1"
          r.addSingleColumnToRow(x)
          super.process(r)
        } else if (!negate) {
          super.process(r)
        }
      } else {
        fillRightValuesIfNeeded()
        rightValues.foreach(right => {
          val fromRight = right.selectedColumns(adjustedOutCols)
          super.process(r.rowWithAddedColumns(fromRight))
        })
      }
    } else {
      if (inSet) {
        if (inSetCol) {
          val x = if (negate) "1" else "0"
          r.addSingleColumnToRow(x)
          super.process(r)
        } else if (negate) {
          super.process(r)
        }
      } else if (returnMissing) {
        super.process(r.rowWithAddedColumn(missingVal))
      }
    }
  }

  private def fillRightValuesIfNeeded(): Unit = {
    if (rightValues == null) {
      fillRightValues()
    }
  }

  private def fillRightValues(): Unit = {
    val buffer = ArrayBuffer[Row]()
    var nextRightKey = rightKey
    nextRightRow = rightRow
    while (rightKey.equals(nextRightKey)) {
      buffer.append(nextRightRow)
      if (rightSource.hasNext) {
        nextRightRow = rightSource.next()
        nextRightKey = getKeyFromRightRow(nextRightRow, nextRightKey)
      } else {
        nextRightKey = null
      }
    }
    rightRow = null
    rightValues = buffer.toArray

    if (!multipleRows) {
      collapseRows()
    }
  }

  private def collapseRows(): Unit = {
    val collapsed = rightValues(0).copyRow()
    for (column <- 2 until collapsed.numCols()) {
      val values = rightValues.map(r => r.colAsString(column))
      val filteredValues = if (skipEmpty) values.filter(s => s.length() > 0) else values
      val joined = filteredValues.mkString(",")
      collapsed.setColumn(column - 2, joined)
    }

    rightValues = Array[Row](collapsed)
  }

  private def getKeyFromRow(r: Row, prevKey: String) = {
    var key = if (singleCol) {
      r.colAsString(columns.head).toString
    } else {
      r.selectedColumns(columns)
    }
    key = if (caseInsensitive) key.toUpperCase() else key
    validateKeyOrder(r, key, prevKey, "Left")
    key
  }

  private def getKeyFromRightRow(r: Row, prevKey: String) = {
    var key = if (singleCol) {
      r.colAsString(2).toString
    } else {
      r.selectedColumns(lookupIndices)
    }
    key = if (caseInsensitive) key.toUpperCase() else key

    validateKeyOrder(r, key, prevKey, "Right")

    key
  }

  private def validateKeyOrder(r: Row, key: String, prevKey: String, source: String): Unit = {
    if (prevKey != null && !prevKey.isEmpty && prevKey > key) {
      throw new GorDataException(
        String.format("%s source is not ordered, as required if the -ordered options is used.  " +
          "Row '%s' is out of order.  Key/Prevkey was '%s'/'%s'.",
          source, r.toString, key, prevKey));
    }
  }

  private def rightHasNext: Boolean = {
    if (nextRightRow != null) {
      true
    } else {
      rightSource.hasNext
    }
  }

  private def advanceRight(): Unit = {
    if (nextRightRow != null) {
      rightRow = nextRightRow
      nextRightRow = null
    } else {
      rightRow = rightSource.next()
    }
    rightKey = getKeyFromRightRow(rightRow, rightKey)
    rightValues = null
  }
}
