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

import gorsat.Commands.{Analysis, ColumnSelection, RowHeader}
import gorsat.parser.ParseArith
import org.gorpipe.gor.ColumnValueProvider
import org.gorpipe.model.genome.files.gor.Row
import org.gorpipe.model.gor.RowObj

/**
  * Cols2List collapses columns with the given separator between values. The resulting row
  * will contain the columns indicated by the <i>include</i> column selection, plus the newly
  * added column. Any columns in the <i>collapse</i> column selection are removed, unless they
  * are explicitly referenced in the <i>include</i> selection.
  * <p>
  * A map expression can optionally be applied to each value as it is being gathered into the
  * list. The value can be referenced as 'x' and is assumed to be a string. Other columns can't
  * be referenced in the expression.
  *
  * @param collapse      The selection of columns to include in the new column
  * @param include       The selection of columns to include in the resulting row
  * @param separator     Separator to use between values in the new column
  * @param forNor True when the step is run in the context of NOR
  * @param mapExpression An optional expression that is applied to each value
  */
case class Cols2ListAnalysis(collapse: ColumnSelection, include: ColumnSelection, separator: String,
                             forNor: Boolean, mapExpression: String, incomingHeader: RowHeader) extends Analysis {

  private val ex: (Row, CharSequence) => CharSequence = compileExpression(mapExpression)

  override def isTypeInformationMaintained: Boolean = true

  override def setRowHeader(header: RowHeader): Unit = {
    rowHeader = header
    if(pipeTo != null) {
      val columnNames = include.columns.map(col => header.columnNames(col)) ::: List("values")
      val columnTypes = include.columns.map(col => header.columnTypes(col)) ::: List("S")
      pipeTo.setRowHeader(RowHeader(columnNames.toArray, columnTypes.toArray))
    }
  }

  override def process(r: Row): Unit = {
    val buffer = new java.lang.StringBuilder()

    val offset = if(forNor) 2 else 0

    for(col <- include.columns) {
      buffer.append(r.colAsString(col))
      buffer.append('\t')
    }

    val range = if (collapse.isRange)
      collapse.firstInRange - offset to collapse.lastInRange - offset
    else
      collapse.columns.map(c => c - offset)

    var needsSeparator = false
    for (col <- range) {
      if (needsSeparator)
        buffer.append(separator)
      else
        needsSeparator = true
      val value = ex(r, r.colAsString(col + offset))
      buffer.append(value)
    }
    super.process(RowObj(buffer))
  }

  /**
    * Compiles an expression using ParseArith. If the expression is empty, the value is returned unchanged.
    * @param src The expression
    * @return A lambda that performs the calculation specified in the source.
    */
  def compileExpression(src: String): (Row, CharSequence) => CharSequence = {
    if (src.isEmpty) {
      (_: Row, x: CharSequence) => x
    } else {
      val incomingColumnNames = incomingHeader.columnNames.toList ::: List("x")
      val incomingColumnTypes = incomingHeader.getTypesOrDefault("S").toList ::: List("S")

      val p = ParseArith()
      p.setColumnNamesAndTypes(incomingColumnNames.toArray, incomingColumnTypes.toArray)
      p.compileCalculation(src)

      val cvp = new XColumnValueProvider(incomingHeader.columnNames.length)
      (r: Row, x: CharSequence) => {
        cvp.row = r
        cvp.x = x.toString
        p.evalFunction(cvp)
      }
    }
  }
}

/**
  * Helper class for map expressions in Cols2List.
  */
class XColumnValueProvider(val xIx: Int) extends ColumnValueProvider {
  var x: String = ""
  var row: ColumnValueProvider = _

  override def stringValue(col: Int): String = if(col == xIx) x else row.stringValue(col)
  override def intValue(col: Int): Int = if(col == xIx) x.toInt else row.intValue(col)
  override def longValue(col: Int): Long = if(col == xIx) x.toLong else row.longValue(col)
  override def doubleValue(col: Int): Double = if(col == xIx) x.toDouble else row.doubleValue(col)
}