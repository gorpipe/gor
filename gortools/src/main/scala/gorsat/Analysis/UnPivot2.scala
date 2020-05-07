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
import gorsat.Commands.RowHeader
import org.gorpipe.model.genome.files.gor.Row
import org.gorpipe.model.gor.RowObj

case class NamedColumn(index: Int, name: String)

case class UnPivot2(header: String, unpivCols: List[Int], oCols: List[Int], outgoingHeader: RowHeader) extends Analysis {
  /**
    * Column names from the original row, used to fill in the Col_Name column
    */
  val columnNames: Array[String] = header.split("\t")

  val namedColumns: List[NamedColumn] = unpivCols.map(x => NamedColumn(x, columnNames(x)))

  /**
    * Columns that remain unchanged from the original row
    */
  val remainingColumns: Array[Int] = oCols.toArray


  override def isTypeInformationMaintained: Boolean = true


  override def setRowHeader(header: RowHeader): Unit = {
    rowHeader = header
    if(pipeTo != null) {
      // Work out the type of the added column, as the narrowest type that matches all the columns
      // being unpivoted. If they're all integers (or doubles) they should stay that way, but if
      // any of them is a string it becomes a string
      var colType = rowHeader.columnTypes(unpivCols.head)
      unpivCols.tail.foreach(col => {
        val tp = rowHeader.columnTypes(col)
        if(tp != colType) {
          colType = (colType, tp) match {
            case (_, "S") => "S"
            case ("I", x) => x
            case ("S", _) => "S"
            case _ => colType
          }
        }
      })

      // Pick up the types from the incoming header and set the new column type
      val fixedUpHeader = outgoingHeader.propagateTypes(header)
      fixedUpHeader.columnTypes(fixedUpHeader.columnTypes.length - 1) = colType
      pipeTo.setRowHeader(fixedUpHeader)
    }
  }

  override def process(r: Row) {
    val baseRow = r.rowWithSelectedColumns(remainingColumns)
    for (col <- namedColumns) {
      val addedColumns = Array(col.name, r.colAsString(col.index))
      val row = baseRow.rowWithAddedColumns(addedColumns)
      super.process(row)
    }
  }
}
