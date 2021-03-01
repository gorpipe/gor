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

package gorsat.Analysis

import gorsat.Commands.{Analysis, RowHeader}
import org.gorpipe.exceptions.GorDataException
import org.gorpipe.gor.model.Row

/**
  * A pipe step to infer column types by looking at column values for some number of rows.
  */
case class InferColumnTypes() extends Analysis {
  val MAX_ROW_BUFFER_SIZE: Int = System.getProperty("gor.gorfiltering.max_rows_buffered", "10000").toInt
  val MAX_BUFFERED_BYTES: Int = System.getProperty("gor.gorfiltering.max_bytes_buffered", "1073741824").toInt  // Default 1 GB

  var collectInBuffer = true
  var buffCount = 0
  var rowBuff = new Array[Row](MAX_ROW_BUFFER_SIZE)

  val NUM_LINES_TO_ESTIMATE_LINE_SIZE = 100
  var estimatedLineSize = 0
  var estimatedBufferedBytes = 0

  var columnCount: Int = 0
  var columnTypes: Array[String] = _

  var hasAnalyzedTypes = false

  override def process(r: Row): Unit = {
    if(collectInBuffer) {
      if (buffCount < MAX_ROW_BUFFER_SIZE) {
        rowBuff(buffCount) = r
        buffCount += 1
      }
      if (buffCount < NUM_LINES_TO_ESTIMATE_LINE_SIZE) {
        val lineSize = r.length()
        estimatedLineSize = if (buffCount == 1) lineSize else (estimatedLineSize * (buffCount - 1) + lineSize) / buffCount
        estimatedBufferedBytes += lineSize
      } else {
        estimatedBufferedBytes += estimatedLineSize
      }

      if (buffCount == MAX_ROW_BUFFER_SIZE || estimatedBufferedBytes > MAX_BUFFERED_BYTES) emptyBuffer()
    } else {
      super.process(r)
    }
  }

  def emptyBuffer(): Unit = {
    analyzeColumnTypes(rowBuff, buffCount)
    hasAnalyzedTypes = true

    val outgoingHeader = if(columnTypes != null) {
      val columnNames = if(rowHeader == null) {
        val columnNames = new Array[String](columnTypes.length)
        for(ix <- 1 to columnTypes.length) {
          columnNames(ix-1) = s"Col$ix"
        }
        columnNames
      } else {
        rowHeader.columnNames
      }
      if (columnNames.length != columnTypes.length) {
        throw new GorDataException("Row doesn't match header")
      }
      RowHeader(columnNames, columnTypes)
    } else {
      null
    }
    setRowHeader(outgoingHeader)

    // Pass all buffered rows on to the rest of the pipe
    var ix = 0
    while(ix < buffCount && !wantsNoMore) {
      super.process(rowBuff(ix))
      ix += 1
    }
    rowBuff = null
    collectInBuffer = false
  }

  override def finish(): Unit = {
    if(collectInBuffer) emptyBuffer()
  }

  override def setRowHeader(header: RowHeader): Unit = {
    if(header == null || header.isMissingTypes || !hasAnalyzedTypes) {
      rowHeader = header
    } else {
      collectInBuffer = false
      if(pipeTo != null) pipeTo.setRowHeader(header)
    }
  }

  def analyzeColumnTypes(rowBuff : Array[Row], bCount : Int ) {
    columnCount = if(bCount > 0) rowBuff(0).numCols() else 0
    if (columnCount == 0) {
      return
    }

    columnTypes = new Array[String](columnCount)

    columnTypes(0) = "S"
    columnTypes(1) = "I"
    for (c <- 2 until columnCount) {
      columnTypes(c) = determineColumnType(c, rowBuff, bCount)
    }
  }

  private def determineColumnType(c: Int, rowBuff: Array[Row], bCount: Int) = {
    if (isColumnAllEmpty(c, rowBuff, bCount)) {
      "I"
    } else if (isColumnAllIntOrEmpty(c, rowBuff, bCount)) {
      "I"
    } else if (isColumnAllLongOrEmpty(c, rowBuff, bCount)) {
      "L"
    } else if (isColumnAllDoubleOrEmpty(c, rowBuff, bCount)) {
      "D"
    } else {
      "S"
    }
  }

  private def isColumnAllEmpty(c: Int, rowBuff: Array[Row], bCount: Int): Boolean = {
    for(i <- 0 until bCount) {
      val row = rowBuff(i)
      if(row.numCols() != columnCount) {
        throw new GorDataException("Row has different number of columns than header suggests", row.toString)
      }
      if(row.colAsString(c).length() > 0) return false
    }
    true
  }

  private def isColumnAllIntOrEmpty(c: Int, rowBuff: Array[Row], bCount: Int): Boolean = {
    for(i <- 0 until bCount) {
      val row = rowBuff(i)
      if(row.numCols() != columnCount) {
        throw new GorDataException("Row has different number of columns than header suggests", row.toString)
      }
      if(row.colAsString(c).length() > 0) {
        try {
          val d = row.colAsLong(c)
          if(d > Int.MaxValue) return false
          if(d < Int.MinValue) return false

          // colAsLong doesn't behave the same as colAsInt wrt NaN - work around that
          // by calling colAsInt here to have it throw exception
          row.colAsInt(c)
        } catch {
          case e: Exception => return false
        }
      }
    }
    true
  }

  private def isColumnAllLongOrEmpty(c: Int, rowBuff: Array[Row], bCount: Int): Boolean = {
    for(i <- 0 until bCount) {
      val row = rowBuff(i)
      if(row.numCols() != columnCount) {
        throw new GorDataException("Row has different number of columns than header suggests", row.toString)
      }
      if(row.colAsString(c).length() > 0) {
        try {
          val d = row.colAsLong(c)
        } catch {
          case e: Exception => return false
        }
      }
    }
    true
  }

  private def isColumnAllDoubleOrEmpty(c: Int, rowBuff: Array[Row], bCount: Int): Boolean = {
    for(i <- 0 until bCount) {
      val row = rowBuff(i)
      if(row.numCols() != columnCount) {
        throw new GorDataException("Row has different number of columns than header suggests", row.toString)
      }
      if(row.colAsString(c).length() > 0) {
        try {
          val d = row.colAsDouble(c)
        } catch {
          case e: Exception => return false
        }
      }
    }
    true
  }
}
