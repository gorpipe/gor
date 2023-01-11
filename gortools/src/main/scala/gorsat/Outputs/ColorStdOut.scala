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

package gorsat.Outputs

import gorsat.Commands.RowHeader
import gorsat.process.PipeInstance
import org.gorpipe.gor.model.{Row, RowColorize}

case class ColorStdOut(instance: PipeInstance = null, colorFormatter: RowColorize)
  extends OutStream(null , System.out)  {

  var headerPrinted : Boolean = false

  override protected def processRow(r: Row): String = {
    if (instance == null) return r.toString()

    val builder = new StringBuilder()
    val rowHeader = this.instance.lastStep.rowHeader

    for (i <- 0 until r.numCols) {
      val columnValue = r.colAsString(i)
      var cc = colorFormatter.formatColumn(i, columnValue.toString(), rowHeader.columnTypes(i))
      builder.append(cc)
      builder.append("\t")
    }

    if (!this.headerPrinted) {
      val headerBuilder = new StringBuilder()
      for (i <- 0 until r.numCols()) {
        headerBuilder.append(colorFormatter.formatHeaderColumn(i, rowHeader.columnNames(i), rowHeader.columnTypes(i)))
        headerBuilder.append("\t")
      }
      val headerLine = headerBuilder.toString()

      this.headerPrinted = true

      out.write(headerLine.stripMargin('\t'))
      out.write("\n")
    }

    builder.toString()
  }
}
