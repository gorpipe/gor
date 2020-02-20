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

package gorsat.Analysis

import java.io.{File, FileWriter}
import java.nio.file.Files

import gorsat.TestUtils
import org.apache.commons.io.FileUtils
import org.junit.Assert
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FunSuite}

@RunWith(classOf[JUnitRunner])
class UTestAdjust extends FunSuite with BeforeAndAfter {

  var workdir: File =_

  before {
    workdir = Files.createTempDirectory("adjustTest").toFile
  }

  after {
    FileUtils.deleteDirectory(workdir)
  }

  test("test basic") {
    val wanted = "CHROM\tPOS\tP_VAlUES\tGC\tQQ\tBONF\tHOLM\tSS\tSD\tBH\tBY\n" +
      "chr1\t1\t0.02\t0.40413\t0.10000\t0.10000\t0.10000\t0.096079\t0.096079\t0.10000\t0.22833\n" +
      "chr1\t2\t0.04\t0.46142\t0.30000\t0.20000\t0.16000\t0.18463\t0.15065\t0.10000\t0.22833\n" +
      "chr1\t3\t0.06\t0.50000\t0.50000\t0.30000\t0.18000\t0.26610\t0.16942\t0.10000\t0.22833\n" +
      "chr1\t4\t0.08\t0.53011\t0.70000\t0.40000\t0.18000\t0.34092\t0.16942\t0.10000\t0.22833\n" +
      "chr1\t5\t0.1\t0.55527\t0.90000\t0.50000\t0.18000\t0.40951\t0.16942\t0.10000\t0.22833\n"
    val cont = "CHROM\tPOS\tP_VAlUES\nchr1\t1\t0.02\n" +
      "chr1\t2\t0.04\n" +
      "chr1\t3\t0.06\n" +
      "chr1\t4\t0.08\n" +
      "chr1\t5\t0.1\n"
    val gorFileName = writeToFile("basic.gor", cont)
    val query = "gor " + gorFileName + " | adjust -pc 3 -gcc -bonf -holm -ss -sd -bh -by -qq"
    val results = TestUtils.runGorPipe(query)
    Assert.assertEquals(wanted, results)
  }

  test("test basic 0 p values don't cause an error") {
    val cont = "CHROM\tPOS\tP_VAlUES\nchr1\t1\t0.02\n" +
      "chr1\t2\t0.0\n" +
      "chr1\t3\t0\n" +
      "chr1\t4\t0.08\n" +
      "chr1\t5\t0.1\n"
    val gorFileName = writeToFile("basic.gor", cont)
    val query = "gor " + gorFileName + " | adjust -pc 3 -gcc -bonf -holm -ss -sd -bh -by -qq"
    val results = TestUtils.runGorPipeCount(query)
    Assert.assertEquals(5, results)
  }

  test("test basic - 2") {
    val wanted = "CHROM\tPOS\tP_VAlUES\tGC\tQQ\tBONF\tHOLM\tSS\tSD\tBH\tBY\n" +
      "chr1\t1\t0.02\t0.40413\t0.10000\t0.10000\t0.10000\t0.096079\t0.096079\t0.10000\t0.22833\n" +
      "chr1\t2\t0.1\t0.55527\t0.90000\t0.50000\t0.18000\t0.40951\t0.16942\t0.10000\t0.22833\n" +
      "chr1\t3\t0.06\t0.50000\t0.50000\t0.30000\t0.18000\t0.26610\t0.16942\t0.10000\t0.22833\n" +
      "chr1\t4\t0.08\t0.53011\t0.70000\t0.40000\t0.18000\t0.34092\t0.16942\t0.10000\t0.22833\n" +
      "chr1\t5\t0.04\t0.46142\t0.30000\t0.20000\t0.16000\t0.18463\t0.15065\t0.10000\t0.22833\n"
    val cont = "CHROM\tPOS\tP_VAlUES\nchr1\t1\t0.02\n" +
      "chr1\t2\t0.1\n" +
      "chr1\t3\t0.06\n" +
      "chr1\t4\t0.08\n" +
      "chr1\t5\t0.04\n"
    val gorFileName = writeToFile("basic.gor", cont)
    val query = "gor " + gorFileName + " | adjust -pc 3 -gcc -bonf -holm -ss -sd -bh -by -qq"
    val results = TestUtils.runGorPipe(query)
    Assert.assertEquals(wanted, results)
  }

  test("test group by column") {
    val cont = "CHROM\tPOS\tGroupCol\tP\n" +
      "chr1\t1\t1\t0.17\n" +
      "chr1\t1\t2\t0.17\n" +
      "chr1\t2\t1\t0.33\n" +
      "chr1\t2\t2\t0.33\n" +
      "chr1\t3\t1\t0.5\n" +
      "chr1\t3\t2\t0.5\n" +
      "chr1\t4\t1\t0.67\n" +
      "chr1\t4\t2\t0.67\n" +
      "chr1\t5\t1\t0.83\n" +
      "chr1\t5\t2\t0.83\n"
    val gorFileName = writeToFile("gc.gor", cont)
    val query = "gor " + gorFileName + " | adjust -gc 3 -pc 4 -gcc"
    val results = TestUtils.runGorPipe(query)
    results.split('\n').tail.map(line => line.substring(line.lastIndexOf('\t'))).grouped(2).foreach(gr => {
      Assert.assertEquals(gr(0), gr(1))
    })
  }

  test("test group by column - 2") {
    val cont = "CHROM\tPOS\tGROUPCOL\tP\n" +
              "chr1\t1\t1\t0.33\n" +
              "chr1\t1\t2\t0.33\n" +
              "chr1\t2\t1\t0.67\n" +
              "chr1\t2\t3\t0.33\n" +
              "chr1\t3\t2\t0.67\n" +
              "chr1\t3\t3\t0.67\n"
    val gorFileName = writeToFile("gc2.gor", cont)
    val query = "gor " + gorFileName + " | adjust -gc #3 -pc 4 -gcc"
    val results = TestUtils.runGorPipe(query)
    val wanted = "CHROM\tPOS\tGROUPCOL\tP\tGC\n" +
      "chr1\t1\t1\t0.33\t0.38217\n" +
      "chr1\t1\t2\t0.33\t0.38217\n" +
      "chr1\t2\t1\t0.67\t0.70223\n" +
      "chr1\t2\t3\t0.33\t0.38217\n" +
      "chr1\t3\t2\t0.67\t0.70223\n" +
      "chr1\t3\t3\t0.67\t0.70223\n"
    Assert.assertEquals(wanted, results)
  }

  test("check ordering") {
    val cont = "CHROM\tPOS\tP\n" +
      "chr1\t1\t0.15\n" +
      "chr1\t2\t0.5\n" +
      "chr1\t3\t0.10\n"
    val gorFileName = writeToFile("gc2.gor", cont)
    val query = "gor " + gorFileName + " | adjust -pc 3 -bonf"
    val results = TestUtils.runGorPipe(query)
    val wanted = "CHROM\tPOS\tP\tBONF\n" +
      "chr1\t1\t0.15\t0.45000\n" +
      "chr1\t2\t0.5\t1.0000\n" +
      "chr1\t3\t0.10\t0.30000\n"
    Assert.assertEquals(wanted, results)
  }

  def writeToFile(fileName: String, cont: String): String = {
    val gorFile = new File(workdir, fileName)
    val gorFileWriter = new FileWriter(gorFile)
    gorFileWriter.write(cont)
    gorFileWriter.close()
    gorFile.getAbsolutePath
  }
}
