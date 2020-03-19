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

import java.io.FileWriter
import java.nio.file.Files

import org.apache.commons.io.FileUtils
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class UTestGeneralQueryHandler extends FunSuite {

  test("verifyOrderInCreateStatement") {
    val tmpdir = Files.createTempDirectory("test_gor_write")
    var success = false
    try {
      val tmpFile = tmpdir.resolve("drasl.gor")
      tmpFile.toFile.deleteOnExit()
      val fw = new FileWriter(tmpFile.toAbsolutePath.normalize().toString)
      fw.write("CHROM\tPOS\tDUMMY\nchr2\t1\tdummy\nchr1\t1\tdummydummy\n")
      fw.close()

      try {
        TestUtils.runGorPipeCount("create x = gor " + tmpFile.toAbsolutePath.normalize().toString + " ; gor [x]")
      } catch {
        case _:Any => success = true
      }
    } finally {
      FileUtils.deleteDirectory(tmpdir.toFile)
      assert(success)
    }
  }
}
