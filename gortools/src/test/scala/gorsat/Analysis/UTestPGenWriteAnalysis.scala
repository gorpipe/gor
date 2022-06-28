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

import java.nio.file.{Files, Path}
import gorsat.TestUtils
import org.apache.commons.io.FileUtils
import org.gorpipe.model.gor.RowObj
import org.junit.Assert
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner

import scala.io.Source

@RunWith(classOf[JUnitRunner])
class UTestPGenWriteAnalysis extends AnyFunSuite with BeforeAndAfter {
  var tmpDir: Path =_

  before {
    tmpDir = Files.createTempDirectory("uTestPGenOut")
  }

  after {
    FileUtils.deleteDirectory(tmpDir.toFile)
  }

  test("test writing - imputed") {
    val pgenFilePath = tmpDir.resolve("pgenFileImp.pgen")

    val pgenOut = PGenWriteAnalysis(pgenFilePath.toString, batch = 0, imputed = true, 0.99f, group = false, 2, 3, 4, 5)
    pgenOut.process(RowObj("chr1\t1\tA\tC\tchr1:1:A:C\t~~~~~~~~~~~~~~~~~~~~"))
    pgenOut.finish()

    val wantedFileLen = 35 //12 bytes for header, 3 bytes for hardcalls and 20 for dosages.
    Assert.assertEquals(wantedFileLen, Files.size(pgenFilePath))

    val pgenFileReader = Files.newInputStream(pgenFilePath)
    val buffer = new Array[Byte](wantedFileLen)
    Assert.assertEquals(wantedFileLen, pgenFileReader.read(buffer))
    Assert.assertEquals(0x6c, buffer(0)) //First magic byte
    Assert.assertEquals(0x1b, buffer(1)) //Second magic byte
    Assert.assertEquals(0x03, buffer(2)) //Type byte
    Assert.assertEquals(1, buffer(3)) //Number of variants
    Assert.assertEquals(0, buffer(4))
    Assert.assertEquals(0, buffer(5))
    Assert.assertEquals(0, buffer(6))
    Assert.assertEquals(10, buffer(7)) //Number of samples
    Assert.assertEquals(0, buffer(8))
    Assert.assertEquals(0, buffer(9))
    Assert.assertEquals(0, buffer(10))
    Assert.assertEquals(64, buffer(11))

    buffer.drop(12).forall(b => b == 0)

    val src = Source.fromFile(tmpDir + "/pgenFileImp.pvar")
    val pvarFileSource = src.getLines()
    Assert.assertEquals("#CHROM\tID\tPOS\tALT\tREF", pvarFileSource.next())
    Assert.assertEquals("1\tchr1:1:A:C\t1\tC\tA", pvarFileSource.next())
    Assert.assertFalse(pvarFileSource.hasNext)
    src.close()
  }

  test("test writing - imputed - from gorpipe") {
    val cont = "CHROM\tPOS\tREF\tALT\tRSID\tVALUES\nchr1\t1\tA\tC\tchr1:1:A:C\t~~~~~~~~~~~~~~~~~~~~\n"

    val gorFile = tmpDir.resolve("gorFileImpGorp.gor")
    val gorFileWriter = Files.newBufferedWriter(gorFile)
    gorFileWriter.write(cont)
    gorFileWriter.close()

    val pgenFilePath = tmpDir.resolve("pgenFileImpGorp.pgen")
    TestUtils.runGorPipe("gor " + gorFile.toAbsolutePath + " | binarywrite -imp " + pgenFilePath.toAbsolutePath.toString)

    val wantedFileLen = 35 //12 bytes for header, 3 bytes for hardcalls and 20 for dosages.
    Assert.assertEquals(wantedFileLen, Files.size(pgenFilePath))

    val pgenFileReader = Files.newInputStream(pgenFilePath)
    val buffer = new Array[Byte](wantedFileLen)
    Assert.assertEquals(wantedFileLen, pgenFileReader.read(buffer))
    Assert.assertEquals(0x6c, buffer(0)) //First magic byte
    Assert.assertEquals(0x1b, buffer(1)) //Second magic byte
    Assert.assertEquals(0x03, buffer(2)) //Type byte
    Assert.assertEquals(1, buffer(3)) //Number of variants
    Assert.assertEquals(0, buffer(4))
    Assert.assertEquals(0, buffer(5))
    Assert.assertEquals(0, buffer(6))
    Assert.assertEquals(10, buffer(7)) //Number of samples
    Assert.assertEquals(0, buffer(8))
    Assert.assertEquals(0, buffer(9))
    Assert.assertEquals(0, buffer(10))
    Assert.assertEquals(64, buffer(11))

    buffer.drop(12).forall(b => b == 0)

    val src = Source.fromFile(tmpDir + "/pgenFileImpGorp.pvar");
    val pvarFileSource = src.getLines()
    Assert.assertEquals("#CHROM\tID\tPOS\tALT\tREF", pvarFileSource.next())
    Assert.assertEquals("1\tchr1:1:A:C\t1\tC\tA", pvarFileSource.next())
    Assert.assertFalse(pvarFileSource.hasNext)
    src.close()
  }

  test("test writing - hardcalls") {
    val pgenFile = tmpDir.resolve("pgenFileHardCalls.pgen")

    val pgenOut = PGenWriteAnalysis(pgenFile.toString, batch = 0, false, -1f, false, 2, 3, 4, 5)
    pgenOut.setup()
    pgenOut.process(RowObj("chr1\t1\tA\tC\tchr1:1:A:C\t0000000000"))
    pgenOut.finish()

    val wantedFileLen = 15 //12 bytes for header, 3 bytes for hardcalls.
    Assert.assertEquals(wantedFileLen, Files.size(pgenFile))

    val pgenFileReader = Files.newInputStream(pgenFile)
    val buffer = new Array[Byte](wantedFileLen)
    Assert.assertEquals(wantedFileLen, pgenFileReader.read(buffer))
    Assert.assertEquals(0x6c, buffer(0)) //First magic byte
    Assert.assertEquals(0x1b, buffer(1)) //Second magic byte
    Assert.assertEquals(0x02, buffer(2)) //Type byte
    Assert.assertEquals(1, buffer(3)) //Number of variants
    Assert.assertEquals(0, buffer(4))
    Assert.assertEquals(0, buffer(5))
    Assert.assertEquals(0, buffer(6))
    Assert.assertEquals(10, buffer(7)) //Number of samples
    Assert.assertEquals(0, buffer(8))
    Assert.assertEquals(0, buffer(9))
    Assert.assertEquals(0, buffer(10))
    Assert.assertEquals(64, buffer(11))

    buffer.drop(12).forall(b => b == 0)

    val pvarFileSource = Source.fromFile(tmpDir + "/pgenFileHardCalls.pvar").getLines()
    Assert.assertEquals("#CHROM\tID\tPOS\tALT\tREF", pvarFileSource.next())
    Assert.assertEquals("1\tchr1:1:A:C\t1\tC\tA", pvarFileSource.next())
    Assert.assertFalse(pvarFileSource.hasNext)
  }

  test("test writing - hardcalls - from  gorpipe") {
    val cont = "CHROM\tPOS\tREF\tALT\tRSID\tVALUES\nchr1\t1\tA\tC\tchr1:1:A:C\t0000000000\n"

    val gorFile = tmpDir.resolve("gorFileHardCallsGorp.gor")
    val gorFileWriter = Files.newBufferedWriter(gorFile)
    gorFileWriter.write(cont)
    gorFileWriter.close()

    val pgenFilePath = tmpDir.resolve("pgenFileHardCallsGorp.pgen")
    TestUtils.runGorPipe("gor " + gorFile.toAbsolutePath + " | binarywrite " + pgenFilePath)

    val wantedFileLen = 15 //12 bytes for header, 3 bytes for hardcalls.
    Assert.assertEquals(wantedFileLen, Files.size(pgenFilePath))

    val pgenFileReader = Files.newInputStream(pgenFilePath)
    val buffer = new Array[Byte](wantedFileLen)
    Assert.assertEquals(wantedFileLen, pgenFileReader.read(buffer))
    Assert.assertEquals(0x6c, buffer(0)) //First magic byte
    Assert.assertEquals(0x1b, buffer(1)) //Second magic byte
    Assert.assertEquals(0x02, buffer(2)) //Type byte
    Assert.assertEquals(1, buffer(3)) //Number of variants
    Assert.assertEquals(0, buffer(4))
    Assert.assertEquals(0, buffer(5))
    Assert.assertEquals(0, buffer(6))
    Assert.assertEquals(10, buffer(7)) //Number of samples
    Assert.assertEquals(0, buffer(8))
    Assert.assertEquals(0, buffer(9))
    Assert.assertEquals(0, buffer(10))
    Assert.assertEquals(64, buffer(11))

    buffer.drop(12).forall(b => b == 0)

    val src = Source.fromFile(tmpDir + "/pgenFileHardCallsGorp.pvar")
    val pvarFileSource = src.getLines()
    Assert.assertEquals("#CHROM\tID\tPOS\tALT\tREF", pvarFileSource.next())
    Assert.assertEquals("1\tchr1:1:A:C\t1\tC\tA", pvarFileSource.next())
    Assert.assertFalse(pvarFileSource.hasNext)
    src.close
  }
}
