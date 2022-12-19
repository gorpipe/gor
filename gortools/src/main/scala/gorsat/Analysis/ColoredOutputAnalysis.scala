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
import org.gorpipe.gor.model.{Row, RowColorize}
import org.gorpipe.model.gor.RowObj

import scala.io.AnsiColor._

case class ColoredOutputAnalysis(formatter: RowColorize) extends Analysis {

  override def process(r: Row): Unit = {
    val builder = new StringBuilder()

    for (i <- 2 until r.numCols) {
      val columnValue = r.colAsString(i)
      var cc = formatter.formatColumn(i, columnValue.toString(), this.rowHeader.columnTypes(i))
      builder.append(cc)
      builder.append("\t")
    }

    super.process(RowObj(r.chr, r.pos, builder.toString().stripMargin('\t')))
  }
}