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

import java.io._
import java.nio.file.Files

import org.gorpipe.gor.binsearch.GorIndexType
import org.gorpipe.model.gor.RowObj
import org.junit.Assert
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FunSuite}

import scala.io.Source

@RunWith(classOf[JUnitRunner])
class UTestPrefixOption extends FunSuite with BeforeAndAfter {
  var tmpFolder: File = _

  before {
    tmpFolder = Files.createTempDirectory("uTestWrite").toFile
  }

  after {
    tmpFolder.listFiles().foreach(f => f.delete())
    tmpFolder.delete()
  }

  test("test outfile -header not written - gor") {
    val headerFileCont = List("##This is header line", "##This is another header line")
    val headerFilePath = writeContentToFile("header.tsv", headerFileCont)

    val inputFileCont = Array("CHROM\tPOS\tREF\tALT", "chr1\t117\tA\tC")

    val outPutFileName = new File(tmpFolder, "output.gor").getAbsolutePath

    val outFile = Outputs.OutFile(outPutFileName, inputFileCont.head, false, false, false, false, GorIndexType.NONE, Option(headerFilePath))
    outFile.setup
    outFile.process(RowObj(inputFileCont(1)))
    outFile.finish

    val lines = Source.fromFile(outPutFileName).getLines()

    headerFileCont.foreach(hl => {
      Assert.assertTrue(lines.hasNext)
      Assert.assertEquals(hl, lines.next())
    })

    inputFileCont.foreach(l => {
      Assert.assertTrue(lines.hasNext)
      Assert.assertEquals(l, lines.next())
    })
    Assert.assertFalse(lines.hasNext)
  }

  test("test outfile - header not written - gorz") {
    val headerFileCont = List("##This is header line", "##This is another header line")
    val headerFilePath = writeContentToFile("header.tsv", headerFileCont)

    val inputFileCont = Array("CHROM\tPOS\tREF\tALT", "chr1\t117\tA\tC")

    val outPutFileName = new File(tmpFolder, "output.gorz").getAbsolutePath

    val outFile = Outputs.OutFile(outPutFileName, inputFileCont.head, false, false, false, false, GorIndexType.NONE, Option(headerFilePath))
    outFile.setup
    outFile.process(RowObj(inputFileCont(1)))
    outFile.finish

    val lines = Source.fromFile(outPutFileName).getLines()

    val is = new FileInputStream(outPutFileName)
    val buffer = new Array[Byte](1024)
    val len = is.read(buffer)
    val actualCont = new String(buffer, 0, len).split('\n')

    Assert.assertEquals(4, actualCont.length)
    Assert.assertEquals(headerFileCont(0), actualCont(0))
    Assert.assertEquals(headerFileCont(1), actualCont(1))
    Assert.assertEquals(inputFileCont(0), actualCont(2))
  }

  test("test outfile - header written - gor") {
    val inputFileCont = Array("chr1\t117\tA\tC")

    val outPutFileName = new File(tmpFolder, "output.gor").getAbsolutePath

    val outFile = Outputs.OutFile(outPutFileName, "", true, false, false, false, GorIndexType.NONE, Option(""))
    outFile.setup
    outFile.process(RowObj(inputFileCont(0)))
    outFile.finish

    val lines = Source.fromFile(outPutFileName).getLines()
    inputFileCont.foreach(l => {
      Assert.assertTrue(lines.hasNext)
      Assert.assertEquals(l, lines.next())
    })
    Assert.assertFalse(lines.hasNext)
  }

  test("test from gorpipe") {
    val headerFileCont = List("##Headerline1", "##Headerline2")
    val headerFilePath = writeContentToFile("header.tsv", headerFileCont)

    val inputFileCont = List("CHROM\tPOS\tREF\tALT", "chr1\t117\tA\tC")
    val inputFilePath = writeContentToFile("input.gor", inputFileCont)

    val outputFilePath = tmpFolder.getAbsoluteFile + "/output.vcf"

    val gorQuery = "gor " + inputFilePath + " | write -prefix " + headerFilePath + " " + outputFilePath
    TestUtils.runGorPipe(gorQuery)

    val outputFileLines = Source.fromFile(outputFilePath).getLines()

    headerFileCont.foreach(hl => {
      Assert.assertTrue(outputFileLines.hasNext)
      Assert.assertEquals(hl, outputFileLines.next)
    })

    inputFileCont.foreach(il => {
      Assert.assertTrue(outputFileLines.hasNext)
      Assert.assertEquals(il, outputFileLines.next())
    })
    Assert.assertFalse(outputFileLines.hasNext)
  }

  def writeContentToFile(fileName: String, content: Traversable[String]): String = {
    val file = new File(tmpFolder, fileName)
    val fileWriter = new FileWriter(file)
    content.foreach(line => fileWriter.write(line + "\n"))
    fileWriter.close()
    file.getAbsolutePath
  }
}
