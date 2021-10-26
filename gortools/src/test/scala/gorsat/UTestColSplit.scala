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

import org.junit.Assert
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class UTestColSplit extends AnyFunSuite {

  test("UTestColSplit") {
    TestUtils.assertGorpipeResults("Chrom\tPOS\treference\tallele\trsIDs\thjalti\tblabla_1\tblabla_2\tblabla_3\tblabla_4\tblabla_5\tblabla_6\nchr1\t10179\tC\tCC\trs367896724\t1,2,3,4,5,6,7\t1\t2\t3\t4\t5\t6\n","gor ../tests/data/gor/dbsnp_test.gorz | top 1 | calc hjalti '1,2,3,4,5,6,7' |colsplit hjalti 6 blabla")
    TestUtils.assertGorpipeResults("Chrom\tPOS\treference\tallele\tdifferentrsIDs\thjalti\tblabla_1\tblabla_2\tblabla_3\tblabla_4\tblabla_5\tblabla_6\tblabla_7\nchr1\t10179\tC\tCC\trs367896724\t1,2,3,4,5,6,7\t1\t2\t3\t4\t5\t6\t7\n", "gor ../tests/data/gor/dbsnp_test.gor | top 1 | calc hjalti '1,2,3,4,5,6,7' | colsplit hjalti 7 blabla")
    TestUtils.assertGorpipeResults("Chrom\tPOS\treference\tallele\tdifferentrsIDs\thjalti\tblabla_1\tblabla_2\tblabla_3\tblabla_4\tblabla_5\nchr1\t10179\tC\tCC\trs367896724\t1,2,3\t1\t2\t3\tmissing\tmissing\n", "gor ../tests/data/gor/dbsnp_test.gor | top 1 | calc hjalti '1,2,3' | colsplit hjalti 5 blabla -m 'missing'")
  }

  test("colsplit triggers type inference") {
    val lines = TestUtils.runGorPipeLines("gorrow 1,1 | calc data \"1,2,3\" | colsplit data 3 split | calc sum split_1+split_2+split_3")
    Assert.assertEquals(2, lines.length)
    Assert.assertEquals("chrom\tpos\tdata\tsplit_1\tsplit_2\tsplit_3\tsum\n", lines(0))
    Assert.assertEquals("chr1\t1\t1,2,3\t1\t2\t3\t6\n", lines(1))
  }

}
