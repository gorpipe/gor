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

package gorsat.Utilities

import java.nio.file.Paths
import gorsat.TestUtils
import gorsat.process.{GenericSessionFactory, PipeInstance}
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.util.Util
import org.gorpipe.test.SlowTests
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class UTestGorpipe extends FunSuite {

  test("GOR nested query") {
    var result = TestUtils.runGorPipeLines("gor ../tests/data/gor/dbsnp_test.gorz | group 100 -fc pos -count | top 10")
    result = TestUtils.runGorPipeLines("create xxx = pgor ../tests/data/gor/dbsnp_test.gorz | calc foo 1.0 | group 1 -max -fc foo | top 10 | signature -timeres 1; gor [xxx]")
  }

  test("Create with definition replacement") {
    var result = TestUtils.runGorPipeLines("gor ../tests/data/gor/dbsnp_test.gorz | group 100 -fc pos -count | top 10")
    result = TestUtils.runGorPipeLines("def ##foo## = ../tests/data/gor/dbsnp_test.gorz;create xxx = pgor ##foo## | calc foo 1.0 | group 1 -max -fc foo | top 10 | signature -timeres 1; gor [xxx]")
  }


  test("White list commands from config") {

    val factory = new GenericSessionFactory()
    val pipe = PipeInstance.createGorIterator(factory.create().getGorContext)
    val orginalVal = System.getProperty("gor.cmd.whitelist.file")
    try {
      val currentPath = Paths.get("").toRealPath()

      System.setProperty("gor.cmd.whitelist.file", "")
      assertResult(null, "")(AnalysisUtilities.getWhiteListCommandFilePath(pipe.getSession.getProjectContext.getRealProjectRootPath))

      System.setProperty("gor.cmd.whitelist.file", "/mnt/csa")
      val expected = if (Util.isWindowsOS()) "C:\\mnt\\csa" else "/mnt/csa"
      assertResult(expected, "")(AnalysisUtilities.getWhiteListCommandFilePath(pipe.getSession.getProjectContext.getRealProjectRootPath).toString)

      System.setProperty("gor.cmd.whitelist.file", "test")
      assertResult(currentPath.resolve("test").toString, "")(AnalysisUtilities.getWhiteListCommandFilePath(pipe.getSession.getProjectContext.getRealProjectRootPath).toString)

      System.setProperty("gor.cmd.whitelist.file", "test2")
      val expected2 = if (Util.isWindowsOS()) "\\tmp\\gorroot\\test2" else "/tmp/gorroot/test2"
      assertResult(expected2, "")(AnalysisUtilities.getWhiteListCommandFilePath(Paths.get("/tmp/gorroot")).toString)

    } finally {
      System.setProperty("gor.cmd.whitelist.file", if (orginalVal != null) orginalVal else "")
    }
  }

}

@RunWith(classOf[JUnitRunner])
@Category(Array(classOf[SlowTests]))
class UTestSlowGorpipe extends FunSuite {

  test("PGOR split test") {

    TestUtils.assertGorpipeResults("Chrom\tbpStart\tbpStop\tallCount\n", "create xxx = pgor ../tests/data/gor/dbsnp_test.gorz | group genome -count | signature -timeres 1;create yyy = pgor -split 100 ../tests/data/gor/dbsnp_test.gorz; gor [xxx] | join [yyy] -snpsnp -xl 3- -xr 3- | group genome -count | signature -timeres 1")

    TestUtils.assertGorpipeResults("Chrom\tbpStart\tbpStop\tallCount\n", "create xxx = pgor ../tests/data/gor/dbsnp_test.gorz | group genome -count | signature -timeres 1;create yyy = pgor -split 300000000 ../tests/data/gor/dbsnp_test.gorz; gor [xxx] | join [yyy] -snpsnp -xl 3- -xr 3- | group genome -count| signature -timeres 1")

    TestUtils.assertGorpipeResults("Chrom\tbpStart\tbpStop\tallCount\n", "create xxx = pgor ../tests/data/gor/dbsnp_test.gorz | group genome -count | signature -timeres 1;create yyy = pgor -split 300000000:1000 ../tests/data/gor/dbsnp_test.gorz; gor [xxx] | join [yyy] -snpsnp -xl 3- -xr 3- | group genome -count| signature -timeres 1")

    TestUtils.assertGorpipeResults("Chrom\tbpStart\tbpStop\tallCount\n", "create xxx = pgor ../tests/data/gor/dbsnp_test.gorz | group genome -count | signature -timeres 1;create yyy = pgor -split 3000000:100 ../tests/data/gor/dbsnp_test.gorz; gor [xxx] | join [yyy] -snpsnp -xl 3- -xr 3- | group genome -count| signature -timeres 1")

    intercept[GorParsingException] {
      TestUtils.assertGorpipeResults("Chrom\tbpStart\tbpStop\tallCount\n", "create xxx = pgor ../tests/data/gor/dbsnp_test.gorz | group genome -count | signature -timeres 1;create yyy = pgor -split 1500000:100 ../tests/data/gor/dbsnp_test.gorz; gor [xxx] | join [yyy] -snpsnp -xl 3- -xr 3- | group genome -count| signature -timeres 1")
    }
  }
}