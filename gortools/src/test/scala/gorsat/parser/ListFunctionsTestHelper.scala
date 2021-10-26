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

package gorsat.parser

import gorsat.process.GenericSessionFactory
import org.gorpipe.gor.model.Row
import org.gorpipe.model.gor.RowObj
import org.scalatest.{Inside, Inspectors, OptionValues}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

abstract class UnitSpec extends AnyFlatSpec with Matchers with
  OptionValues with Inside with Inspectors

abstract class ListFunctionsTestHelper extends UnitSpec {
  protected def evalExpression(expr: String, contents: String = ""): String = {
    val filter = createFilter(expr)
    val row: Row = getRow(contents)
    val result = filter.evalStringFunction(row)
    result
  }

  protected def createFilter(expr: String): ParseArith = {
    val context = new GenericSessionFactory().create().getGorContext
    val filter = new ParseArith()
    filter.setContext(context, executeNor = false)
    filter.setColumnNamesAndTypes(Array("Chrom", "pos", "col1"), Array("S", "I", "S"))
    filter.compileCalculation(expr)
    filter
  }

  protected def getRow(contents: String): Row = {
    val prefix = "chr1\t1\t"
    val row = RowObj(prefix + contents)
    row
  }
}


