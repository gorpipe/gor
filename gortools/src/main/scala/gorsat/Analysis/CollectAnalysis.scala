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

import gorsat.Commands.{Analysis, RowHeader}
import org.gorpipe.gor.model.{Row, RowBase}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

case class CollectAnalysis(column: Int, columnName: String, windowSize: Int, calcAve: Boolean, calcSum: Boolean, calcVar: Boolean,
                           calcStd: Boolean) extends Analysis {

  val circularArray = ArrayBuffer.fill(windowSize) {0.0}
  var index = 0
  var isFull = false
  var sum = 0.0;
  var average = 0.0;
  var variance = 0.0;


  override def process(r: Row): Unit = {
    val value = r.colAsDouble(column)

    val (oldValue, width) = if (isFull) {
      index %= windowSize;
      val oldValue = circularArray(index)
      circularArray(index) = value
      index += 1
      (oldValue, windowSize)
    } else {
      circularArray(index) = value
      index += 1
      if (index >= windowSize) isFull = true
      (0.0, index)
    }

    sum += (value - oldValue)
    val oldAverage = average
    average = sum / (width)

    if ((calcStd || calcVar) && width > 1) {
      if (isFull) {
        val diff = value - average
        val oldDiff = oldValue - oldAverage
        variance += (value - oldValue) * (diff + oldDiff)
      } else {
          variance += (value - oldAverage) * (value - average)
      }
    }

    val varc = variance / width
    val stdc = Math.sqrt(varc)

    val builder = new mutable.StringBuilder(r.toString)

    if (calcSum) {
      builder.append(s"\t${sum}")
    }

    if (calcAve) {
      builder.append(s"\t${average}")
    }

    if (calcVar) {
      builder.append(s"\t${varc}")
    }

    if (calcStd) {
      builder.append(s"\t${stdc}")
    }

    super.process(new RowBase(builder.toString))
  }

  override def isTypeInformationMaintained: Boolean = true

  override def setRowHeader(header: RowHeader): Unit = {

    val newColumns = ArrayBuffer.empty[String]

    if (calcSum) {
      newColumns.append(s"${header}_sum")
    }

    if (calcAve) {
      newColumns.append(s"${header}_average")
    }

    if (calcVar) {
      newColumns.append(s"${header}_variance")
    }

    if (calcStd) {
      newColumns.append(s"${header}_std")
    }

    // The header we pass on must include the columns we add
    val columnNames = header.columnNames ++ newColumns
    val columnTypes = header.columnTypes ++ ArrayBuffer.fill(newColumns.length) {"D"}

     rowHeader = new RowHeader(columnNames, columnTypes)

    if (pipeTo != null) {
      pipeTo.setRowHeader(rowHeader)
    }
  }
}
