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

import java.io.{File, FileInputStream, FileWriter}
import java.nio.ByteOrder
import java.nio.file.Files

import gorsat.TestUtils
import org.apache.commons.io.FileUtils
import org.gorpipe.model.gor.RowObj
import org.gorpipe.util.collection.ByteArray
import org.junit.Assert
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FunSuite}

@RunWith(classOf[JUnitRunner])
class UTestBGenWriteAnalysis extends FunSuite with BeforeAndAfter {
  var tmpDir: File =_
  var tmpDirPath: String =_

  before {
    tmpDir = Files.createTempDirectory("uTestBGenOut").toFile
    tmpDirPath = tmpDir.getAbsolutePath
  }

  after {
    FileUtils.deleteDirectory(tmpDir)
  }

  test("test basic") {
    val bgenFile = new File(tmpDir, "bgenFile.bgen")
    val bgenFilePath = bgenFile.getAbsolutePath

    val bgenOut = BGenWriteAnalysis(bgenFilePath, group = false, imputed = false, 2, 3, -1, -1, 4)
    bgenOut.process(RowObj("chr1\t1\tA\tC\t012301230123"))
    bgenOut.process(RowObj("chr1\t2\tA\tC\t012301230123"))
    bgenOut.finish()

    val buffer = new Array[Byte](16)
    val fis = new FileInputStream(bgenFile)
    fis.read(buffer)
    Assert.assertEquals(2, ByteArray.readInt(buffer, 8, ByteOrder.LITTLE_ENDIAN))
    Assert.assertEquals(12, ByteArray.readInt(buffer, 12, ByteOrder.LITTLE_ENDIAN))
  }

  test("test basic - from gorpipe") {
    val bgenFile = new File(tmpDir, "bgenFile.bgen")
    val bgenFilePath = bgenFile.getAbsolutePath
    val gorFile = new File(tmpDir, "gorFile.gor")

    val cont = "CHROM\tPOS\tREF\tALT\tVALUES\n" +
      "chr1\t1\tA\tC\t012301230123\n" +
      "chr1\t2\tA\tC\t012301230123\n"
    val fw = new FileWriter(gorFile)
    fw.write(cont)
    fw.close()

    TestUtils.runGorPipe("gor " + gorFile.getAbsolutePath + " | binarywrite " + bgenFilePath)

    val buffer = new Array[Byte](16)
    val fis = new FileInputStream(bgenFile)
    fis.read(buffer)
    Assert.assertEquals(2, ByteArray.readInt(buffer, 8, ByteOrder.LITTLE_ENDIAN))
    Assert.assertEquals(12, ByteArray.readInt(buffer, 12, ByteOrder.LITTLE_ENDIAN))
  }
}

