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

package gorsat

import gorsat.Outputs.{NullOut, ToList}
import gorsat.process.{GenericSessionFactory, GorPipeCommands}
import org.gorpipe.gor.model.Row
import org.gorpipe.model.gor.RowObj
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner

import java.util
import scala.collection.mutable.ListBuffer

@RunWith(classOf[JUnitRunner])
class UTestFilterInvalidRows extends AnyFunSuite with BeforeAndAfter {

  before {
    GorPipeCommands.register()
  }

  test("filter invalid rows") {
    val info = GorPipeCommands.getInfo("FILTERINVALIDROWS")
    val context = new GenericSessionFactory().create().getGorContext
    val result = info.init(context, executeNor = false, "chrom\tpos\tdata\tvalue", "", Array())

    val buffer = new ListBuffer[Row]
    val analysisStep = result.step | ToList(buffer)

    analysisStep.process(RowObj.apply("chr1", 1000, "t1\t10"))
    assert (buffer.size == 1)

    analysisStep.process(RowObj.apply("chr1", 1000, "t1"))
    assert (buffer.size == 1)

    analysisStep.process(RowObj.apply("chr1", 1000, "t1\t10\tfoo"))
    assert (buffer.size == 1)
  }
}


