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

import java.io.{File, FileWriter}
import java.nio.file.Files

import org.junit.Assert
import org.junit.runner.RunWith
import org.scalatest.{BeforeAndAfter, FunSuite}
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class UTestCsvsel2 extends FunSuite with BeforeAndAfter {
  var tmpDir: File = _
  var bucketFile: String =_
  var pnFile: String =_
  var gorFileHC: String =_
  var gorFilePr: String =_
  val bucketCont: String = "TAG\tBUCKET\n" + Range(0, 25).map(id => id + "\t" + (id / 5)).mkString("\n") + "\n"
  val pnCont: String = Range(0, 25).mkString("\n") + "\n"
  val gorContHC: String = "CHROM\tPOS\tREF\tALT\tBUCKET\tVALUES\n" +
    "chr1\t1\tA\tC\t0\t01230\n" +
    "chr1\t1\tA\tC\t1\t11220\n" +
    "chr1\t1\tA\tC\t2\t00111\n" +
    "chr1\t1\tA\tC\t3\t22222\n" +
    "chr1\t1\tA\tC\t4\t12131\n"
  val gorContPr: String = "CHROM\tPOS\tREF\tALT\tBUCKET\tVALUES\n" +
    "chr1\t1\tA\tC\t0\t!~|zx|~!$}\n" +
    "chr1\t1\tA\tC\t1\t}')|&z}~){\n" +
    "chr1\t1\tA\tC\t2\t~%{}}'~&~}\n" +
    "chr1\t1\tA\tC\t3\ty{})~&&~\"}\n" +
    "chr1\t1\tA\tC\t4\t~#~\"~%{|\"}\n"

  before {
    tmpDir = Files.createTempDirectory("uTestCsvsel").toFile
    gorFileHC = writeToFile(tmpDir, "hc.gor", gorContHC)
    gorFilePr = writeToFile(tmpDir, "pr.gor", gorContPr)
    bucketFile = writeToFile(tmpDir,  "buckets.tsv", bucketCont)
    pnFile = writeToFile(tmpDir, "pn.tsv", pnCont)
  }

  after {
    tmpDir.listFiles().foreach(f => f.delete())
    tmpDir.delete()
  }

  test("test vcf probabilistic") {
    val gorQuery = String.join(" ", "gor", gorFilePr, "| csvsel", bucketFile, pnFile, "-gc #3,#4 -vs 2 -vcf -threshold 0.9 ")
    val result = TestUtils.runGorPipe(gorQuery)
    val expected = "CHROM\tPOS\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\t0\t1\t2\t3\t4\t5\t6\t7\t8\t9\t10\t11\t12\t13\t14\t15\t16\t17\t18\t19\t20\t21\t22\t23\t24\nchr1\t1\tA\tC\t.\t.\t.\tGT:GP\t0/1:0;1;0\t0/0:0.935;0.022;0.043\t0/0:0.914;0.065;0.022\t1/1:0;0;1\t0/1:0.022;0.968;0.011\t1/1:0.054;0.011;0.935\t0/1:0.065;0.914;0.022\t0/1:0.011;0.946;0.043\t0/0:0.989;0.011;0\t0/1:0.054;0.914;0.032\t1/1:0.043;0;0.957\t0/0:0.957;0.032;0.011\t1/1:0.054;0.011;0.935\t1/1:0.054;0;0.946\t0/0:0.989;0;0.011\t0/0:0.914;0.054;0.032\t1/1:0.075;0.011;0.914\t1/1:0.054;0;0.946\t0/1:0.054;0.946;0\t0/1:0;0.989;0.011\t1/1:0.022;0;0.978\t1/1:0.011;0;0.989\t1/1:0.043;0;0.957\t0/0:0.946;0.032;0.022\t0/1:0;0.989;0.011\n"
    Assert.assertEquals(expected, result)
  }

  test("test vcf probabilistic - select") {
    val gorQuery = String.join(" ", "gor", gorFilePr, "| csvsel", bucketFile, pnFile, "-gc #3,#4 -vs 2 -vcf -threshold 0.9", "| select 1-10")
    val result = TestUtils.runGorPipe(gorQuery)
    val expected = "CHROM\tPOS\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\t0\t1\nchr1\t1\tA\tC\t.\t.\t.\tGT:GP\t0/1:0;1;0\t0/0:0.935;0.022;0.043\n"
    Assert.assertEquals(expected, result)
  }

  test("test vcf probabilistic - select2") {
    val gorQuery = String.join(" ", "gor", gorFilePr, "| csvsel", bucketFile, pnFile, "-gc #3,#4 -vs 2 -vcf -threshold 0.9", "| select 1-4,15-16,19")
    val result = TestUtils.runGorPipe(gorQuery)
    val expected = "CHROM\tPOS\tREF\tALT\t6\t7\t10\nchr1\t1\tA\tC\t0/1:0.065;0.914;0.022\t0/1:0.011;0.946;0.043\t1/1:0.043;0;0.957\n"
    Assert.assertEquals(expected, result)
  }


  test("test vcf hardcalls") {
    val query = String.join(" ", "gor", gorFileHC, "| csvsel", bucketFile, pnFile, "-gc #3,#4 -vs 1 -vcf")
    val result = TestUtils.runGorPipe(query)
    val expected = "CHROM\tPOS\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\t0\t1\t2\t3\t4\t5\t6\t7\t8\t9\t10\t11\t12\t13\t14\t15\t16\t17\t18\t19\t20\t21\t22\t23\t24\nchr1\t1\tA\tC\t.\t.\t.\tGT\t0/0\t0/1\t1/1\t./.\t0/0\t0/1\t0/1\t1/1\t1/1\t0/0\t0/0\t0/0\t0/1\t0/1\t0/1\t1/1\t1/1\t1/1\t1/1\t1/1\t0/1\t1/1\t0/1\t./.\t0/1\n"
    Assert.assertEquals(expected, result)
  }

  def writeToFile(dir: File, fileName: String, content: String): String = {
    val file = new File(dir, fileName)
    val writer = new FileWriter(file)
    writer.write(content)
    writer.close()
    file.getAbsolutePath
  }
}
