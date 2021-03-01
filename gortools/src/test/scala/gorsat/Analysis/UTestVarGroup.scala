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

import java.io.File
import java.nio.file.Files

import gorsat.TestUtils
import org.apache.commons.io.FileUtils
import org.gorpipe.exceptions.GorResourceException
import org.gorpipe.test.utils.FileTestUtils
import org.junit.Assert
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FunSuite}

@RunWith(classOf[JUnitRunner])
class UTestVarGroup extends FunSuite with BeforeAndAfter {

  var wd: File =_

  before {
    wd = Files.createTempDirectory("utestvargroup").toFile
  }

  after {
    FileUtils.deleteDirectory(wd)
  }

  test("no multiallelic variants") {
    val cont = "CHROM\tPOS\tREF\tALT\tVALUES\n" +
              "chr1\t1\tA\tC\t01230123\n" +
              "chr1\t2\tA\tC\t01230123\n"
    val wanted = "CHROM\tPOS\tREF\tALT\tVALUES\n" +
      "chr1\t1\tA\tC\t0/0,0/1,1/1,./.,0/0,0/1,1/1,./.\n" +
      "chr1\t2\tA\tC\t0/0,0/1,1/1,./.,0/0,0/1,1/1,./.\n"
    testAgainstExpectedResults(cont, wanted)
  }

  test("no multiallelic variants nested") {
    val cont = "CHROM\tPOS\tREF\tALT\tVALUES\n" +
      "chr1\t1\tA\tC\t01230123\n" +
      "chr1\t2\tA\tC\t01230123\n"
    val wanted = "CHROM\tPOS\tREF\tALT\tVALUES\n" +
      "chr1\t1\tA\tC\t0/0,0/1,1/1,./.,0/0,0/1,1/1,./.\n" +
      "chr1\t2\tA\tC\t0/0,0/1,1/1,./.,0/0,0/1,1/1,./.\n"
    testNestedAgainstExpectedResults(cont, wanted)
  }

  test("multiallelic variant - two alternatives") {
    val cont = "CHROM\tPOS\tREF\tALT\tVALUES\n" +
      "chr1\t1\tA\tC\t011020\n" +
      "chr1\t1\tA\tG\t010102\n"
    val wanted = "CHROM\tPOS\tREF\tALT\tVALUES\n" +
      "chr1\t1\tA\tC,G\t0/0,1/2,0/1,0/2,1/1,2/2\n"
    testAgainstExpectedResults(cont, wanted)
  }

  test("multiallelic variant - three alternatives") {
    val cont = "CHROM\tPOS\tREF\tALT\tVALUES\n" +
      "chr1\t1\tA\tC\t0101100200\n" +
      "chr1\t1\tA\tG\t0110010020\n" +
      "chr1\t1\tA\tT\t0011001002\n"
    val wanted = "CHROM\tPOS\tREF\tALT\tVALUES\n" +
      "chr1\t1\tA\tC,G,T\t0/0,1/2,2/3,1/3,0/1,0/2,0/3,1/1,2/2,3/3\n"
    testAgainstExpectedResults(cont, wanted)
  }

  test("multiallelic variant - four alternatives") {
    val cont = "CHROM\tPOS\tREF\tALT\tVALUES\n" +
      "chr1\t1\tA\tB\t010011020001000\n" +
      "chr1\t1\tA\tC\t011000102000100\n" +
      "chr1\t1\tA\tD\t001101000200010\n" +
      "chr1\t1\tA\tE\t000110100020001\n"
    val wanted = "CHROM\tPOS\tREF\tALT\tVALUES\n" +
      "chr1\t1\tA\tB,C,D,E\t0/0,1/2,2/3,3/4,1/4,1/3,2/4,1/1,2/2,3/3,4/4,0/1,0/2,0/3,0/4\n"
    testAgainstExpectedResults(cont, wanted)
  }

  test("group columns") {
    val cont = "CHROM\tPOS\tREF\tALT\tGC\tVALUES\n" +
      "chr1\t1\tA\tC\t0\t01\n" +
      "chr1\t1\tA\tG\t1\t01\n"
    val wanted = "CHROM\tPOS\tREF\tALT\tGC\tVALUES\n" +
      "chr1\t1\tA\tC\t0\t0/0,0/1\n" +
      "chr1\t1\tA\tG\t1\t0/0,0/1\n"
    testAgainstExpectedResults(cont, wanted, "-gc #5")
  }

  test("group columns and multiple alleles") {
    val cont = "CHROM\tPOS\tREF\tALT\tGC\tVALUES\n" +
      "chr1\t1\tA\tC\t0\t01\n" +
      "chr1\t1\tA\tC\t1\t10\n" +
      "chr1\t1\tA\tG\t0\t01\n" +
      "chr1\t1\tA\tG\t1\t10\n"
    val wanted = "CHROM\tPOS\tREF\tALT\tGC\tVALUES\n" +
      "chr1\t1\tA\tC,G\t0\t0/0,1/2\n" +
      "chr1\t1\tA\tC,G\t1\t1/2,0/0\n"
    testAgainstExpectedResults(cont, wanted, "-gc #5")
  }

  test("group columns - various positions") {
    val cont = "CHROM\tPOS\tREF\tALT\tGC\tVALUES\n" +
      "chr1\t1\tA\tG\t0\t01\n" +
      "chr1\t1\tA\tG\t1\t10\n" +
      "chr1\t2\tA\tC\t2\t01\n" +
      "chr1\t2\tA\tC\t3\t10\n"
    val wanted = "CHROM\tPOS\tREF\tALT\tGC\tVALUES\n" +
      "chr1\t1\tA\tG\t0\t0/0,0/1\n" +
      "chr1\t1\tA\tG\t1\t0/1,0/0\n" +
      "chr1\t2\tA\tC\t2\t0/0,0/1\n" +
      "chr1\t2\tA\tC\t3\t0/1,0/0\n"
    testAgainstExpectedResults(cont, wanted, "-gc #5")
  }

  test("illegal arguments") {
    val cont = "CHROM\tPOS\tREF\tALT\tVALUES\n" +
      "chr1\t1\tA\tC\t4\n"
    testIllegalArguments(cont)
  }

  test("illegal arguments - 2") {
    val cont = "CHROM\tPOS\tREF\tALT\tVALUES\n" +
      "chr1\t1\tA\tC\t4\n" +
      "chr1\t1\tA\tG\t0\n"
    testIllegalArguments(cont)
  }

  test("illegal arguments - 3") {
    val cont = "CHROM\tPOS\tREF\tALT\tVALUES\n" +
      "chr1\t1\tA\tC\t0\n" +
      "chr1\t1\tA\tG\t4\n"
    testIllegalArguments(cont)
  }

  test("illegal arguments - 4") {
    val cont = "CHROM\tPOS\tREF\tALT\tVALUES\n" +
      "chr1\t1\tA\tC\t1\n" +
      "chr1\t1\tA\tG\t2\n"
    testIllegalArguments(cont)
  }

  test("illegal arguments - 5") {
    val cont = "CHROM\tPOS\tREF\tALT\tVALUES\n" +
      "chr1\t1\tA\tC\t2\n" +
      "chr1\t1\tA\tG\t1\n"
    testIllegalArguments(cont)
  }

  test("illegal arguments - 6") {
    val cont = "CHROM\tPOS\tREF\tALT\tVALUES\n" +
      "chr1\t1\tA\tC\t1\n" +
      "chr1\t1\tA\tG\t1\n" +
      "chr1\t1\tA\tT\t1"
    testIllegalArguments(cont)
  }

  test("illegal arguments - 7") {
    val cont = "CHROM\tPOS\tREF\tALT\tVALUES\n" +
      "chr1\t1\tA\tC\t4\n" +
      "chr1\t1\tA\tG\t1\n"
    testIllegalArguments(cont)
  }

  test("illegal arguments - 8") {
    val cont = "CHROM\tPOS\tREF\tALT\tVALUES\n" +
      "chr1\t1\tA\tC\t2\n" +
      "chr1\t1\tA\tG\t1\n" +
      "chr1\t1\tA\tT\t1\n"
    testIllegalArguments(cont)
  }

  test("illegal arguments - 9") {
    val cont = "CHROM\tPOS\tREF\tALT\tVALUES\n" +
      "chr1\t1\tA\tC\t4\n" +
      "chr1\t1\tA\tG\t1\n" +
      "chr1\t1\tA\tT\t1\n"
    testIllegalArguments(cont)
  }

  test("illegal arguments - 10") {
    val cont = "CHROM\tPOS\tREF\tALT\tVALUES\n" +
      "chr1\t1\tA\tC\t3\n" +
      "chr1\t1\tA\tG\t1\n" +
      "chr1\t1\tA\tT\t1\n"
    testIllegalArguments(cont)
  }

  test("illegal arguments - 11") {
    val cont = "CHROM\tPOS\tREF\tALT\tVALUES\n" +
      "chr1\t1\tA\tC\t0\n" +
      "chr1\t1\tA\tG\t00\n"
    testIllegalArguments(cont)
  }

  def testAgainstExpectedResults(cont: String, wanted: String, varGroupOptions: String = ""): Unit = {
    val fileName = FileTestUtils.createTempFile(wd, "test.gor", cont).getAbsolutePath
    val query = "gor " + fileName + " | vargroup " + varGroupOptions
    val results = TestUtils.runGorPipe(query)
    Assert.assertEquals(wanted, results)
  }

  def testNestedAgainstExpectedResults(cont: String, wanted: String, varGroupOptions: String = ""): Unit = {
    val fileName = FileTestUtils.createTempFile(wd, "test.gor", cont).getAbsolutePath
    val query = "gor <(gor " + fileName + " | vargroup " + varGroupOptions + ")"
    val results = TestUtils.runGorPipe(query)
    Assert.assertEquals(wanted, results)
  }

  def testIllegalArguments(cont: String): Unit = {
    val fileName = FileTestUtils.createTempFile(wd, "badFile.gor", cont).getAbsolutePath
    val query = "gor " + fileName + " | vargroup"
    var success = false
    try {
      TestUtils.runGorPipe(query)
    } catch {
      case _: GorResourceException => success = true
      case _=> //Bad :(
    }
    Assert.assertTrue(success)
  }
}
