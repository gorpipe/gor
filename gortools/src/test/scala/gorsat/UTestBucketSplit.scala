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

import org.gorpipe.exceptions.GorUserException
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class UTestBucketSplit extends AnyFunSuite {


  val input1 = "gorrow chr1,1,2 | calc values 'Aabc::fghiBjklmnorstC123456789'"
  val input2 = "gorrow chr1,1,2 | calc values 'Aab,c::f,ghi,Bjkl,mnorst,C123456789'" // 6 elements
  val input3 = "gorrow chr1,1,2 | calc values 'Aabc,B1234' | merge <(gorrow chr1,2,3 | calc values 'Cxyzv')"
  val header = "chrom\tbpStart\tbpStop\tvalues\tbucket\n"


  test("testBucketSplitNormalUsingSize") {

    TestUtils.assertGorpipeResults(header + "chr1\t1\t2\tAabc::fghi\tb_1\nchr1\t1\t2\tBjklmnorst\tb_2\nchr1\t1\t2\tC123456789\tb_3\n",
      input1 + " | bucketsplit #4 10 -vs 1")

    TestUtils.assertGorpipeResults(header + "chr1\t1\t2\tAabc::fghi\tb_1\nchr1\t1\t2\tBjklmnorst\tb_2\nchr1\t1\t2\tC123456789\tb_3\n",
      input1 + " | bucketsplit values 10 -vs 1")

    TestUtils.assertGorpipeResults(header + "chr1\t1\t2\tAabc::fghi\tb_1\nchr1\t1\t2\tBjklmnorst\tb_2\nchr1\t1\t2\tC123456789\tb_3\n",
      input1 + " | bucketsplit #4 5 -vs 2")

    TestUtils.assertGorpipeResults(header + "chr1\t1\t2\tAabc::fghiBjklmnorst\tb_1\nchr1\t1\t2\tC123456789\tb_2\n",
      input1 + " | bucketsplit #4 10 -vs 2")

    TestUtils.assertGorpipeResults(header + "chr1\t1\t2\tAabc::fghiBjklmnorst\tb_1\nchr1\t1\t2\tC123456789\tb_2\n",
      input1 + " | bucketsplit #4 2 -vs 10")

    TestUtils.assertGorpipeResults(header + "chr1\t1\t2\tAabc::fghiBjklmnorstC123456789\tb_1\n",
      input1 + " | bucketsplit #4 100 -vs 10")

    TestUtils.assertGorpipeResults(header + "chr1\t1\t2\tAabc::fghiBjklmnorstC123456789\tbucket_1\n",
      input1 + " | bucketsplit #4 100 -vs 10 -b 'bucket_'")

    TestUtils.assertGorpipeResults("chrom\tbpStart\tbpStop\tvalues\tc4\tbucket\n" + "chr1\t1\t2\tAabc::fghiBjklmnorstC123456789\tdummy\tb_1\n",
      input1 + " | calc c4 'dummy' | bucketsplit #4 100 -vs 10")

  }


  test("testBucketSplitNormalUsingSeparator") {
    TestUtils.assertGorpipeResults(header + "chr1\t1\t2\tAab,c::f,ghi\tb_1\nchr1\t1\t2\tBjkl,mnorst,C123456789\tb_2\n",
      input2 + " | bucketsplit #4 3 -s ','")

    TestUtils.assertGorpipeResults(header + "chr1\t1\t2\tAab,c::f,ghi\tb_1\nchr1\t1\t2\tBjkl,mnorst,C123456789\tb_2\n",
      input2 + " | bucketsplit values 3 -s ','")

    TestUtils.assertGorpipeResults(header + "chr1\t1\t2\tAab,c::f,ghi,Bjkl\tb_1\nchr1\t1\t2\tmnorst,C123456789\tb_2\n",
      input2 + " | bucketsplit #4 4 -s ','")

    TestUtils.assertGorpipeResults(header + "chr1\t1\t2\tAab,c::f,ghi,Bjkl,mnorst,C123456789\tb_1\n",
      input2 + " | bucketsplit #4 10 -s ','")

    TestUtils.assertGorpipeResults(header + "chr1\t1\t2\tAab,c::f,ghi,Bjkl,mnorst,C123456789\tb_1\n",
      input2 + " | bucketsplit #4 2 -s '-'")

    TestUtils.assertGorpipeResults(header + "chr1\t1\t2\tAab,c::f,ghi,Bjkl,mnorst,C123456789\tbucket_1\n",
      input2 + " | bucketsplit #4 2 -s '-' -b 'bucket_'")

    TestUtils.assertGorpipeResults("chrom\tbpStart\tbpStop\tvalues\tc4\tbucket\n" + "chr1\t1\t2\tAab,c::f,ghi,Bjkl,mnorst,C123456789\tdummy\tb_1\n",
      input2 + " | calc c4 'dummy' | bucketsplit #4 2 -s '-'")

    TestUtils.assertGorpipeResults(header + "chr1\t1\t2\tAabc::fghiBjklmnorstC123456789\tb_1\n",
      input1 + " | bucketsplit #4 2 -s '-'")

    TestUtils.assertGorpipeResults(header + "chr1\t1\t2\tAab,c:\tb_1\nchr1\t1\t2\tf,ghi,Bjkl,mnorst,C123456789\tb_2\n",
      input2 + " | bucketsplit #4 2 -s ':'")


    TestUtils.assertGorpipeResults(header + "chr1\t1\t2\t:1\tb_1\nchr1\t1\t2\t2:3\tb_2\n",
      "gorrow chr1,1,2 | calc values ':1:2:3' | bucketsplit #4 2 -s ':'")

    TestUtils.assertGorpipeResults(header + "chr1\t1\t2\t1:2\tb_1\nchr1\t1\t2\t3:\tb_2\n",
      "gorrow chr1,1,2 | calc values '1:2:3:' | bucketsplit #4 2 -s ':'")

  }


  test("UTestBGen") {

    val bgenheader = "CHROM\tPOS\tREF\tALT\tRSID\tVARIANTID\tVALUES\n"

    // Pns=1000, value size = 1, bucketsize = 200
    TestUtils.assertTwoGorpipeResults(
      "gor ../tests/data/external/bgen/testfile1_chr1.bgen | top 5 | multimap -cartesian -h <(norrows 5) | sort genome | calc x substr(values,rownum*200,(rownum+1)*200) " +
                " | calc bucketnum rownum + 1 | calc bucket 'b_'+ bucketnum | hide rownum,values,bucketnum | rename x VALUES ",
      "gor ../tests/data/external/bgen/testfile1_chr1.bgen | top 5 | bucketsplit values 200 -vs 1")
  }


  test("testBucketSplitDataIntegrityUsingSize") {

    assertThrows[GorUserException] { // Result type: Assertion
      TestUtils.runGorPipe(input1 + " | bucketsplit #4 70 -vs 7") // Column length mod value size != 0
    }

    assertThrows[GorUserException] { // Result type: Assertion
      TestUtils.runGorPipe(input1 + " | bucketsplit #4 100 -vs 31") // Value size > column size
    }
  }


  test("testBucketSplitLineCheckskUsingSize") {

    TestUtils.assertGorpipeResults(header + "chr1\t1\t2\tAabc,\tb_1\nchr1\t1\t2\tB1234\tb_2\nchr1\t2\t3\tCxyzv\tb_1\n",
      input3 + " | bucketsplit #4 1 -vs 5 -cs")

    assertThrows[GorUserException] { // Result type: Assertion
      TestUtils.runGorPipe(input3 + " | bucketsplit #4 1 -vs 5")  // Not all col values are the same size.
    }

    assertThrows[GorUserException] { // Result type: Assertion
      TestUtils.runGorPipe("gorrow chr1,1,2 | calc values '' | bucketsplit #4 10 -vs 1")  //Value size > column size
    }

    assertThrows[GorUserException] { // Result type: Assertion
      TestUtils.runGorPipe("gorrow chr1,1,2 | calc values '' | bucketsplit #4 10 -vs 2 -cs")  //Value size > column size
    }
  }


  test("testBucketSplitLineCheckUsingSeparator") {

    TestUtils.assertGorpipeResults(header + "chr1\t1\t2\tAabc\tb_1\nchr1\t1\t2\tB1234\tb_2\nchr1\t2\t3\tCxyzv\tb_1\n",
      input3 + " | bucketsplit #4 1 -s ',' -cs")

    assertThrows[GorUserException] { // Result type: Assertion
      TestUtils.runGorPipe(input3 + " | bucketsplit #4 10 -s '',")  // Value size > column size
    }

    TestUtils.assertGorpipeResults(header + "chr1\t1\t2\t\tb_1\n",
      "gorrow chr1,1,2 | calc values '' | bucketsplit #4 2 -s ':' -cs")

    assertThrows[GorUserException] { // Result type: Assertion
      TestUtils.runGorPipe("gorrow chr1,1,2 | calc values '' | bucketsplit #4 2 -s ':'")  // Value size > column size
    }
  }

}
