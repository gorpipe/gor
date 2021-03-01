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
import org.gorpipe.gor.session.GorContext

case class CalcAnalysis(context: GorContext, executeNor: Boolean, exprSrc: Array[String], header: String,
                        newColumns: Array[String]
                       ) extends Analysis with Expressions
{
  override def isTypeInformationNeeded: Boolean = true
  override def isTypeInformationMaintained: Boolean = true

  override def setRowHeader(incomingHeader: RowHeader): Unit = {
    if(incomingHeader == null || incomingHeader.isMissingTypes) return

    // todo: Once header is passed safely through remove this
    rowHeader = RowHeader(header.split('\t'), incomingHeader.columnTypes)

    prepareExpressions(exprSrc.length, context, executeNor)
    compileExpressions(rowHeader, exprSrc, "CALC", newColumns.mkString(","))

    if(pipeTo != null) {
      // The header we pass on must include the columns we add
      val columnNames = rowHeader.columnNames ++ newColumns
      val columnTypes = rowHeader.columnTypes ++ expressionTypes.map(x => x.toString)

      pipeTo.setRowHeader(RowHeader(columnNames, columnTypes))
    }
  }

  override def process(r: Row): Unit = {
    val size = r.numCols() - 2
    r.addColumns(expressions.length)
    expressions.indices.foreach(i => {
      try {
        val columnValue = evalFunction(r, expressions(i), expressionTypes(i))
        r.setColumn(size + i, columnValue)
      } catch {
        case e: Throwable =>
          val paramString = exprSrc.mkString(" ")
          val msg = s"Error in step: CALC ${newColumns.mkString("\t")} $paramString\n${e.getMessage}"
          throw new GorDataException(msg, -1, header, r.getAllCols.toString, e)
      }
    })
    super.process(r)
  }

  override def finish(): Unit = {
    closeExpressions()
  }
}
