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

package gorsat

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

/**
  * Created by hjaltii on 28/07/17.
  */
@RunWith(classOf[JUnitRunner])
class UTestListFunctions extends FunSuite {
  test("Test equality of list map function") {
    val query = "gor ../tests/data/gor/dbsnp_test.gorz | rownum | calc a LISTMAP(rownum,'str(x)+\\'text\\'')"
    val newResult = TestUtils.runGorPipe(query)
    System.setProperty("gor.gorpipe.formulas.useNewListFunctions","false")
    TestUtils.assertGorpipeResults(newResult,query)
  }

  test("Test equality of list filter function") {
    val query = "gor ../tests/data/gor/dbsnp_test.gorz | rownum | calc a LISTFILTER(rownum,'int(x) > 5')"
    val newResult = TestUtils.runGorPipe(query)
    System.setProperty("gor.gorpipe.formulas.useNewListFunctions","false")
    TestUtils.assertGorpipeResults(newResult,query)
  }

  test("Test equality of list zip filter function") {
    val query = "gor ../tests/data/gor/dbsnp_test.gorz | rownum | calc r mod(rownum,3) | calc a LISTZIPFILTER(rownum,r,'int(x) > 1')"
    val newResult = TestUtils.runGorPipe(query)
    System.setProperty("gor.gorpipe.formulas.useNewListFunctions","false")
    TestUtils.assertGorpipeResults(newResult,query)
  }
}