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
import org.gorpipe.gor.model.Row

case class Select2(columns: Int*) extends Analysis {
  val colArray = columns.map(x => x - 1).toArray

  override def process(r: Row): Unit = {
    super.process(r.rowWithSelectedColumns(colArray))
  }

  override def isTypeInformationMaintained: Boolean = true

  /*
   * Determine new positions of any columns without types
   */
  override def columnsWithoutTypes(invalidOnInput: Array[Int]): Array[Int] = {
    if (invalidOnInput == null) null else {
      Array.range(0, colArray.length).filter(x => invalidOnInput.contains(colArray(x)))
    }
  }

  override def setRowHeader(header: RowHeader): Unit = {
    rowHeader = header
    if(pipeTo != null) {
      val columnNames = colArray.map(x => rowHeader.columnNames(x))
      val columnTypes = colArray.map(x => rowHeader.columnTypes(x))
      pipeTo.setRowHeader(RowHeader(columnNames.mkString("\t"), columnTypes))
    }
  }
}
