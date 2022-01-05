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

import Outputs.NullOut
import process.{GenericSessionFactory, GorPipeCommands}
import org.gorpipe.model.gor.RowObj
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class UTestValidateColumns extends AnyFunSuite with BeforeAndAfter {

  before {
    GorPipeCommands.register()
  }

  test("Validate columns") {
    val info = GorPipeCommands.getInfo("VALIDATECOLUMNS")
    val context = new GenericSessionFactory().create().getGorContext
    val result = info.init(context, executeNor = false, "chrom\tpos\tdata\tvalue","", Array("foo", "-n", "1"))

    val analysisStep = result.step | new NullOut
    analysisStep.process(RowObj.apply("chr1", 1000, "t1\t10"))

    var thrown = intercept[RuntimeException] {
      analysisStep.process(RowObj.apply("chr1", 1000, "t1"))
    }
    assert (thrown != null)

    thrown = intercept[RuntimeException] {
      analysisStep.process(RowObj.apply("chr1", 1000, "t1\t10\tfoo"))
    }
    assert (thrown != null)
  }
}


