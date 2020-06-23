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

import java.io.{BufferedWriter, File, FileWriter}
import java.nio.file.Files

import gorsat.TestUtils
import gorsat.process.{GenericSessionFactory, SourceProvider}
import org.apache.commons.io.FileUtils
import org.gorpipe.model.gor.RowObj
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FunSuite}

@RunWith(classOf[JUnitRunner])
class UTestPrGtGenAnalysis extends FunSuite with BeforeAndAfter {
  var tmpDir: File =_
  var tmpDirPath: String =_
  val context = new GenericSessionFactory().create().getGorContext
  var btPath: String = _
  var afanPath: String = _
  var segPath: String = _
  var gorPath: String = _
  var moreGorPath: String = _
  val gorLines = List("chr1\t1\tA\tC\t0.1;0.8;0.1\tPN3",
    "chr1\t1\tA\tG\t0.0;0.9;0.1\tPN7",
    "chr1\t1\tA\tC\t0.1;0.1;0.8\tPN7",
    "chr1\t1\tA\tG\t0.0;0.0;1.0\tPN3",
    "chr1\t10\tA\tC\t0.0;1.0;0.0\tPN2",
    "chr1\t10\tA\tC\t0.1;0.0;0.9\tPN10",
    "chr1\t11\tA\tC\t0.0;0.0;1.0\tPN2",
    "chr1\t11\tA\tC\t0.0;0.5;0.5\tPN10")

  val moreGorLines = List("chr2\t1\tA\tC\t0.1;0.8;0.1\tPN3",
    "chr2\t1\tA\tG\t0.0;0.9;0.1\tPN7",
    "chr2\t1\tA\tC\t0.1;0.1;0.8\tPN7",
    "chr2\t1\tA\tG\t0.0;0.0;1.0\tPN3",
    "chr5\t10\tA\tC\t0.0;1.0;0.0\tPN2",
    "chr5\t10\tA\tC\t0.1;0.0;0.9\tPN10",
    "chr5\t11\tA\tC\t0.0;0.0;1.0\tPN2",
    "chr5\t11\tA\tC\t0.0;0.5;0.5\tPN10")

  before {
    tmpDir = Files.createTempDirectory("uTestImputedGTGen").toFile
    tmpDirPath = tmpDir.getAbsolutePath
    btPath = writeBTFile(tmpDirPath)
    afanPath = writeAFFile(tmpDirPath)
    segPath = writeSegFile(tmpDirPath)
    gorPath = writeLinesToFile(tmpDirPath, "gorFile.gor", gorLines, "CHROM\tPOS\tREF\tALT\tGP\tPN")
    moreGorPath = writeLinesToFile(tmpDirPath, "moreGorFile.gor", gorLines, "CHROM\tPOS\tREF\tALT\tGP\tPN")
  }

  def writeLinesToFile(dir: String, fileName: String, lines: Traversable[String], header: String = null): String = {
    val file = new File(dir, fileName)
    val writer = new BufferedWriter(new FileWriter(file))
    if (header != null) writer.write(header + "\n")
    lines.foreach(line => writer.write(line + "\n"))
    writer.close()
    file.getAbsolutePath
  }

  def writeBTFile(dir: String): String = {
    val lines = Range.inclusive(1, 10).map(pn => s"PN$pn\tBucket${(pn - 1) / 5 + 1}")
    writeLinesToFile(dir, "btFile.tsv", lines)
  }

  def writeAFFile(dir: String): String = {
    val lines = List("chr1\t1\tA\tG\t0.5\t1000", "chr1\t1\tA\tC\t0.2\t2000")
    writeLinesToFile(dir, "afFile.gor", lines, "CHROM\tPOS\tREF\tALT\tAF\tAN")
  }

  def writeSegFile(dir: String): String = {
    writeLinesToFile(dir, "segFile.gor", List("chr1\t1\t10\tPN5\t20"), "CHROM\tBEGIN\tEND\tPN\tDEPTH")
  }

  after {
    FileUtils.deleteDirectory(tmpDir)
  }

  test("test left source analysis") {
    val as = AnalysisSink()
    val lsa = PrGtGenAnalysis.LeftSourceAnalysis(context, "uniqueLookupSignature", btPath, "", null, glCol = -1, gpCol = 4, -1, -1, -1, pnCol = 5, grCols = List(2, 3), error = 0.1) | as

    lsa.process(RowObj("chr1\t1\tA\tC\t0.1;0.8;0.1\tPN3"))
    lsa.process(RowObj("chr1\t1\tA\tG\t0.0;0.9;0.1\tPN7"))
    lsa.process(RowObj("chr1\t1\tA\tC\t0.1;0.1;0.8\tPN7"))
    lsa.process(RowObj("chr1\t1\tA\tG\t0.0;0.0;1.0\tPN3"))
    lsa.securedFinish(null)

    assert(as.rows.length == 2)
    assert(as.rows(0).toString == "chr1\t1\tA\tC")
    assert(as.rows(1).toString == "chr1\t1\tA\tG")

    val gh1 = as.rows(0).bH.asInstanceOf[PrGtGenAnalysis.GroupHolder]
    val gh2 = as.rows(1).bH.asInstanceOf[PrGtGenAnalysis.GroupHolder]

    assert(gh1.gtGen.getNumberOfSamples == 10)
    assert(gh2.gtGen.getNumberOfSamples == 10)

    Range(0, 10).foreach(i => {
      if (i == 2 || i == 6) {
        assert(gh1.gtGen.hasCoverage(i))
        assert(gh2.gtGen.hasCoverage(i))
      } else {
        assert(!gh1.gtGen.hasCoverage(i))
        assert(!gh2.gtGen.hasCoverage(i))
      }
    })
  }

  test("test afan source analysis") {
    val afSource = new SourceProvider(afanPath, context, false, false).source

    val as = AnalysisSink()
    val lsa = PrGtGenAnalysis.LeftSourceAnalysis(context, "ulus2", btPath, "", null, glCol = -1, gpCol = 4, -1, -1, -1, pnCol = 5, grCols = List(2, 3), error = 0.1)|
      PrGtGenAnalysis.AFANSourceAnalysis(afSource, context, "ulus2", grCols = List(2,3), 4, 5) | as

    gorLines.iterator.take(4).map(RowObj(_)).foreach(lsa.process)
    lsa.securedFinish(null)

    assert(as.rows.length == 2)
    assert(as.rows(0).toString == "chr1\t1\tA\tC")
    assert(as.rows(1).toString == "chr1\t1\tA\tG")

    val gh1 = as.rows(0).bH.asInstanceOf[PrGtGenAnalysis.GroupHolder]
    val gh2 = as.rows(1).bH.asInstanceOf[PrGtGenAnalysis.GroupHolder]

    assert(gh1.gtGen.getPriorAf == 0.2)
    assert(gh1.gtGen.getPriorAn == 2000)
    assert(gh2.gtGen.getPriorAf == 0.5)
    assert(gh2.gtGen.getPriorAn == 1000)
  }

  test("test right source analysis") {
    val segSource = new SourceProvider(segPath, context, false, false).source

    val as = AnalysisSink()
    val lsa = PrGtGenAnalysis.LeftSourceAnalysis(context, "ulus3", btPath, "", null, glCol = -1, gpCol = 4, -1, -1, -1, pnCol = 5, grCols = List(2, 3), 0.05, 0.1)|
      PrGtGenAnalysis.RightSourceAnalysis(segSource, context, "ulus3", 4, 3, -1.0, 100) | as

    gorLines.iterator.map(RowObj(_)).foreach(lsa.process)
    lsa.securedFinish(null)

    val wanted = "chr1\t1\tA\tC\t0.66615\tBucket1\t    ~!  ~~\n" +
      "chr1\t1\tA\tC\t0.66615\tBucket2\t  ~!      \n" +
      "chr1\t1\tA\tG\t0.66667\tBucket1\t    ~!  ~~\n" +
      "chr1\t1\tA\tG\t0.66667\tBucket2\t  ~!      \n" +
      "chr1\t10\tA\tC\t0.49990\tBucket1\t  !~    ~~\n" +
      "chr1\t10\tA\tC\t0.49990\tBucket2\t        ~!\n" +
      "chr1\t11\tA\tC\t1.0000\tBucket1\t  ~!      \n" +
      "chr1\t11\tA\tC\t1.0000\tBucket2\t        ~!"
    val actual = as.rows.mkString("\n")
    assert(wanted == actual)
  }

  test("Go through all") {
    val afSource = new SourceProvider(afanPath, context, false, false).source
    val segSource = new SourceProvider(segPath, context, false, false).source

    val as = AnalysisSink()
    val lsa = PrGtGenAnalysis.LeftSourceAnalysis(context, "ulus3", btPath, "", null, glCol = -1, gpCol = 4, -1, -1, -1, pnCol = 5, grCols = List(2, 3), 0.05, 0.1)|
      PrGtGenAnalysis.AFANSourceAnalysis(afSource, context, "ulus2", grCols = List(2,3), 4, 5) |
      PrGtGenAnalysis.RightSourceAnalysis(segSource, context, "ulus3", 4, 3, -1.0, 100) | as

    gorLines.iterator.map(RowObj(_)).foreach(lsa.process)
    lsa.securedFinish(null)

    val wanted = "chr1\t1\tA\tC\t0.20053\tBucket1\t    E\\  ~~\n" +
      "chr1\t1\tA\tC\t0.20053\tBucket2\t  |$      \n" +
      "chr1\t1\tA\tG\t0.50034\tBucket1\t    ~!  ~~\n" +
      "chr1\t1\tA\tG\t0.50034\tBucket2\t  `?      \n" +
      "chr1\t10\tA\tC\t0.49990\tBucket1\t  !~    ~~\n" +
      "chr1\t10\tA\tC\t0.49990\tBucket2\t        ~!\n" +
      "chr1\t11\tA\tC\t1.0000\tBucket1\t  ~!      \n" +
      "chr1\t11\tA\tC\t1.0000\tBucket2\t        ~!"
    val actual = as.rows.mkString("\n")
    assert(wanted == actual)
  }

  test("Test all") {
    val query = s"gor $gorPath | prgtgen -gc 3,4 $btPath $afanPath $segPath -e 0.05"
    val results = TestUtils.runGorPipe(query)
    val wanted = "CHROM\tPOS\tREF\tALT\tAF\tBucket\tValues\n" +
      "chr1\t1\tA\tC\t0.20053\tBucket1\t    E\\  ~~\n" +
      "chr1\t1\tA\tC\t0.20053\tBucket2\t  |$      \n" +
      "chr1\t1\tA\tG\t0.50034\tBucket1\t    ~!  ~~\n" +
      "chr1\t1\tA\tG\t0.50034\tBucket2\t  `?      \n" +
      "chr1\t10\tA\tC\t0.49990\tBucket1\t  !~    ~~\n" +
      "chr1\t10\tA\tC\t0.49990\tBucket2\t        ~!\n" +
      "chr1\t11\tA\tC\t1.0000\tBucket1\t  ~!      \n" +
      "chr1\t11\tA\tC\t1.0000\tBucket2\t        ~!\n"
    assert(wanted == results)
  }

  test("Test all 2") {
    val query2 = s"gor $gorPath | prgtgen -gc 3,4 $btPath $segPath -e 0.05"
    val results2 = TestUtils.runGorPipe(query2)
    val wanted = "CHROM\tPOS\tREF\tALT\tAF\tBucket\tValues\n" +
      "chr1\t1\tA\tC\t0.66615\tBucket1\t    ~!  ~~\n" +
      "chr1\t1\tA\tC\t0.66615\tBucket2\t  ~!      \n" +
      "chr1\t1\tA\tG\t0.66667\tBucket1\t    ~!  ~~\n" +
      "chr1\t1\tA\tG\t0.66667\tBucket2\t  ~!      \n" +
      "chr1\t10\tA\tC\t0.49990\tBucket1\t  !~    ~~\n" +
      "chr1\t10\tA\tC\t0.49990\tBucket2\t        ~!\n" +
      "chr1\t11\tA\tC\t1.0000\tBucket1\t  ~!      \n" +
      "chr1\t11\tA\tC\t1.0000\tBucket2\t        ~!\n"
    assert(wanted == results2)
  }

  test("Test all 3") {
    val query3 = s"gor $gorPath | prgtgen -gc 3,4 $btPath $segPath -e 0.05 -th 0.95"
    val results3 = TestUtils.runGorPipe(query3)
    val wanted = "CHROM\tPOS\tREF\tALT\tAF\tBucket\tValues\n" +
      "chr1\t1\tA\tC\t0.66615\tBucket1\t33230\n" +
      "chr1\t1\tA\tC\t0.66615\tBucket2\t32333\n" +
      "chr1\t1\tA\tG\t0.66667\tBucket1\t33230\n" +
      "chr1\t1\tA\tG\t0.66667\tBucket2\t32333\n" +
      "chr1\t10\tA\tC\t0.49990\tBucket1\t31330\n" +
      "chr1\t10\tA\tC\t0.49990\tBucket2\t33332\n" +
      "chr1\t11\tA\tC\t1.0000\tBucket1\t32333\n" +
      "chr1\t11\tA\tC\t1.0000\tBucket2\t33332\n"
    assert(wanted == results3)
  }

  test("Test all 4") {
    val query4 = s"gor $gorPath | prgtgen -gc 3,4 $btPath $segPath -e 0.05 -osep \',\'"
    val results4 = TestUtils.runGorPipe(query4)
    val wanted = "CHROM\tPOS\tREF\tALT\tAF\tBucket\tValues\n" +
      "chr1\t1\tA\tC\t0.66615\tBucket1\t;;,;;,0.0013864;2.8113e-07;0.99861,;;,1.0000;7.0981e-12;5.3081e-26\n" +
      "chr1\t1\tA\tC\t0.66615\tBucket2\t;;,0.00017351;4.3980e-09;0.99983,;;,;;,;;\n" +
      "chr1\t1\tA\tG\t0.66667\tBucket1\t;;,;;,0.0000;0.0000;1.0000,;;,1.0000;1.2811e-12;5.3206e-26\n" +
      "chr1\t1\tA\tG\t0.66667\tBucket2\t;;,0.0000;5.7028e-08;1.0000,;;,;;,;;\n" +
      "chr1\t10\tA\tC\t0.49990\tBucket1\t;;,0.0000;1.0000;0.0000,;;,;;,1.0000;2.6595e-06;2.6586e-26\n" +
      "chr1\t10\tA\tC\t0.49990\tBucket2\t;;,;;,;;,;;,0.00030788;0.0000;0.99969\n" +
      "chr1\t11\tA\tC\t1.0000\tBucket1\t;;,0.0000;0.0000;1.0000,;;,;;,;;\n" +
      "chr1\t11\tA\tC\t1.0000\tBucket2\t;;,;;,;;,;;,0.0000;2.6177e-10;1.0000\n"
    assert(wanted == results4)
  }

  //Test join logic

  test("Test join - exact fit") {
    val priorLines = List("chr2\t1\tA\tC\t0.99\t1000000", "chr2\t1\tA\tG\t0.99\t1000000", "chr5\t10\tA\tC\t0.01\t1000000", "chr5\t11\tA\tC\t0.5\t1000000")
    val priorFile = writeLinesToFile(tmpDirPath, "prior1.gor", priorLines, "CHROM\tPOS\tAF\tAN")
    val segLines = List("chr2\t1\t100\tPN1\t10",
      "chr2\t1\t100\tPN2\t10",
      "chr2\t1\t100\tPN3\t10",
      "chr2\t1\t100\tPN4\t10",
      "chr2\t1\t100\tPN5\t10",
      "chr2\t1\t100\tPN6\t10",
      "chr2\t1\t100\tPN7\t10",
      "chr2\t1\t100\tPN8\t10",
      "chr2\t1\t100\tPN9\t10",
      "chr2\t1\t100\tPN10\t10",
      "chr5\t1\t100\tPN1\t10",
      "chr5\t1\t100\tPN2\t10",
      "chr5\t1\t100\tPN3\t10",
      "chr5\t1\t100\tPN4\t10",
      "chr5\t1\t100\tPN5\t10",
      "chr5\t1\t100\tPN6\t10",
      "chr5\t1\t100\tPN7\t10",
      "chr5\t1\t100\tPN8\t10",
      "chr5\t1\t100\tPN9\t10",
      "chr5\t1\t100\tPN10\t10")
    val segFile = writeLinesToFile(tmpDirPath, "seg1.gor", segLines, "CHROM\tBEGIN\tEND\tPN\tDEPTH")

    val afSource = new SourceProvider(priorFile, context, false, false).source
    val segSource = new SourceProvider(segFile, context, false, false).source

    val as = AnalysisSink()
    val lsa = PrGtGenAnalysis.LeftSourceAnalysis(context, "ulus4", btPath, "", null, glCol = -1, gpCol = 4, -1, -1, -1, pnCol = 5, grCols = List(2, 3), error = 0.1)|
      PrGtGenAnalysis.AFANSourceAnalysis(afSource, context, "ulus4", grCols = List(2,3), 4, 5) |
      PrGtGenAnalysis.RightSourceAnalysis(segSource, context, "ulus4", 4, 3, -1.0, 100) | as

    moreGorLines.iterator.map(RowObj(_)).foreach(lsa.process)
    lsa.securedFinish(null)

    val actual = as.rows.map(_.toString).mkString("\n")
    val wanted = "chr2\t1\tA\tC\t0.98999\tBucket1\t^~^~~!^~^~\n" +
      "chr2\t1\tA\tC\t0.98999\tBucket2\t^~~!^~^~^~\n" +
      "chr2\t1\tA\tG\t0.98999\tBucket1\t^~^~~!^~^~\n" +
      "chr2\t1\tA\tG\t0.98999\tBucket2\t^~~!^~^~^~\n" +
      "chr5\t10\tA\tC\t0.010001\tBucket1\t~~!~~~~~~~\n" +
      "chr5\t10\tA\tC\t0.010001\tBucket2\t~~~~~~~~~g\n" +
      "chr5\t11\tA\tC\t0.50000\tBucket1\t}~~!}~}~}~\n" +
      "chr5\t11\tA\tC\t0.50000\tBucket2\t}~}~}~}~y&"
    assert(wanted == actual)
  }

  test("Test join - no prior fit") {
    val priorLines = List("chr1\t1\tA\tC\t0.99\t1000000", "chr1\t1\tA\tG\t0.99\t1000000", "chr1\t2\tA\tC\t0.01\t1000000", "chr1\t2\tA\tC\t0.5\t1000000")
    val priorFile = writeLinesToFile(tmpDirPath, "prior2.gor", priorLines, "CHROM\tPOS\tAF\tAN")
    val segLines = List("chr2\t1\t100\tPN1\t10",
      "chr2\t1\t100\tPN2\t10",
      "chr2\t1\t100\tPN4\t10",
      "chr2\t1\t100\tPN5\t10",
      "chr2\t1\t100\tPN6\t10",
      "chr2\t1\t100\tPN8\t10",
      "chr2\t1\t100\tPN9\t10",
      "chr2\t1\t100\tPN10\t10",
      "chr5\t1\t100\tPN1\t10",
      "chr5\t1\t100\tPN3\t10",
      "chr5\t1\t100\tPN4\t10",
      "chr5\t1\t100\tPN5\t10",
      "chr5\t1\t100\tPN6\t10",
      "chr5\t1\t100\tPN7\t10",
      "chr5\t1\t100\tPN8\t10",
      "chr5\t1\t100\tPN9\t10")
    val segFile = writeLinesToFile(tmpDirPath, "seg2.gor", segLines, "CHROM\tBEGIN\tEND\tPN\tDEPTH")

    val afSource = new SourceProvider(priorFile, context, false, false).source
    val segSource = new SourceProvider(segFile, context, false, false).source

    val as = AnalysisSink()
    val lsa = PrGtGenAnalysis.LeftSourceAnalysis(context, "ulus5", btPath, "", null, glCol = -1, gpCol = 4, -1, -1, -1, pnCol = 5, grCols = List(2, 3), error = 0.1) |
      PrGtGenAnalysis.AFANSourceAnalysis(afSource, context, "ulus5", grCols = List(2, 3), 4, 5) |
      PrGtGenAnalysis.RightSourceAnalysis(segSource, context, "ulus5", 4, 3, -1.0, 100) | as

    moreGorLines.iterator.map(RowObj(_)).foreach(lsa.process)
    lsa.securedFinish(null)

    val actual = as.rows.map(_.toString).mkString("\n")
    val wanted = "chr2\t1\tA\tC\t0.19876\tBucket1\t~~~~~\"~~~~\n" +
      "chr2\t1\tA\tC\t0.19876\tBucket2\t~~~!~~~~~~\n" +
      "chr2\t1\tA\tG\t0.20000\tBucket1\t~~~~~!~~~~\n" +
      "chr2\t1\tA\tG\t0.20000\tBucket2\t~~~!~~~~~~\n" +
      "chr5\t10\tA\tC\t0.14989\tBucket1\t~~!~~~~~~~\n" +
      "chr5\t10\tA\tC\t0.14989\tBucket2\t~~~~~~~~~!\n" +
      "chr5\t11\tA\tC\t0.20000\tBucket1\t~~~!~~~~~~\n" +
      "chr5\t11\tA\tC\t0.20000\tBucket2\t~~~~~~~~~!"
    assert(wanted == actual)
  }

  test("test - divergence") {
    val query5 = s"gor $gorPath | prgtgen -gc 3,4 $btPath $segPath -e 0.05 -th 0.95 -maxit 0 -tol 0.0 | where bucket = 'Bucket1'"
    val results5 = TestUtils.runGorPipe(query5)
    val wanted = "CHROM\tPOS\tREF\tALT\tAF\tBucket\tValues\n" +
      "chr1\t1\tA\tC\t.\tBucket1\t\n" +
      "chr1\t1\tA\tG\t.\tBucket1\t\n" +
      "chr1\t10\tA\tC\t.\tBucket1\t\n" +
      "chr1\t11\tA\tC\t.\tBucket1\t\n"
    assert(wanted == results5)
  }

  test("test - divergence 2") {
    val query6 = s"gor $gorPath | where alt = 'C' | prgtgen $btPath $segPath -e 0.05 -maxit 0 -tol 0.0 | where bucket = 'Bucket1'"
    val results6 = TestUtils.runGorPipe(query6)
    val wanted = "CHROM\tPOS\tAF\tBucket\tValues\n" +
      "chr1\t1\t.\tBucket1\t\n" +
      "chr1\t10\t.\tBucket1\t\n" +
      "chr1\t11\t.\tBucket1\t\n"
    assert(wanted == results6)
  }
}
