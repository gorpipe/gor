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
import gorsat.Commands.RowHeader
import gorsat.parser.ParseArith
import org.gorpipe.exceptions.{GorDataException, GorParsingException}
import org.gorpipe.gor.model.ColumnValueProvider
import org.gorpipe.gor.session.GorContext

/**
  * A trait that adds an array of expressions and their types. Useful for Analysis steps that work with
  * expressions (Calc and Replace, in particular).
  */
trait Expressions {
  var expressionTypes: Array[Char] = _
  var expressions: Array[ParseArith] = _

  def prepareExpressions(n: Int, context: GorContext, forNor: Boolean): Unit = {
    expressionTypes = Array.fill(n)('S')
    expressions = Array.fill(n)(new ParseArith())
    expressions.foreach(ex => ex.setContext(context, forNor))
  }

  def compileExpressions(rowHeader: RowHeader, exprSrc: Seq[String], cmd: String, newColumns: String): Unit = {
    expressions.foreach(filter => {
      filter.setColumnNamesAndTypes(rowHeader.columnNames, rowHeader.columnTypes)
    })
    expressions.indices.foreach(i => {
      val filter = expressions(i)
      try {
        val compileResult = filter.compileCalculation(exprSrc(i))
        expressionTypes(i) = compileResult(0)
      } catch {
        case e: GorParsingException =>
          val msg = s"Error in step: $cmd $newColumns ${exprSrc.mkString(",")}\n${e.getMessage}"
          throw new GorDataException(msg, -1, rowHeader.toString, "Null row", e)
      }
    })
  }

  def evalFunction(cvp: ColumnValueProvider, filter: ParseArith, calcType: Char): String = {
    filter.evalFunction(cvp)
  }

  def closeExpressions(): Unit = {
    if(expressions != null)
      expressions.foreach(f => f.close())
  }
}
