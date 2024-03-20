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

import gorsat.Analysis.DagMapAnalysis.DAGMultiMapLookup
import gorsat.Commands.RowHeader
import gorsat.Script.ScriptExecutionEngine
import gorsat.process.GenericSessionFactory
import org.apache.commons.io.FileUtils
import org.gorpipe.exceptions.GorDataException
import org.gorpipe.model.gor.RowObj
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.junit.JUnitRunner

import java.io.File
import java.nio.file.Files

@RunWith(classOf[JUnitRunner])
class UTestDAGMapAnalysis extends AnyFlatSpec with BeforeAndAfter {
  // This is needed to initialize things needed by GorPipeSession
  private val se = ScriptExecutionEngine
  private val context = new GenericSessionFactory().create().getGorContext

  var tmpDir: File =_
  var tmpDirPath: String =_

  before {
    tmpDir = Files.createTempDirectory("uTestBGenOut").toFile
    tmpDirPath = tmpDir.getAbsolutePath
  }

  after {
    FileUtils.deleteDirectory(tmpDir)
  }

  "process" should "find find full relationship between child and grandparents" in {
    val sink = AnalysisSink()

    Files.write(tmpDir.toPath.resolve("test"),
      ("\tP1\n" +
        "\tP2\n" +
        "P1\tA\n" +
        "P2\tA\n" +
        "A\tC1\n").getBytes)

    val pipe = DAGMultiMapLookup(context.getSession, "", null,
      tmpDir.toPath.resolve("test").toString, List(2), caseInsensitive = true, "Missing", returnMiss = true, showDAGPAth = true, "->", 20) | sink
    pipe.setup()

    val header = "ChromNOR\tPosNOR\tPN"
    pipe.setRowHeader(RowHeader(header, List("S", "I", "S").toArray))

    pipe.process(RowObj("ChromN\t0\tP1"))
    pipe.securedFinish(null)

    assert(sink.rows.size == 3)
    assert(sink.rows(2).toString == "ChromN\t0\tP1\tC1\t2\tP1->A->C1")
  }

  it should "throw exception when cyclic graph (with depth 30)" in {
    val sink = AnalysisSink()

    Files.write(tmpDir.toPath.resolve("cyclictest"),
      ("\tP1\n" +
        "P1\tA\n" +
        "A\tC1\n" +
        "C1\tA\n" +
        "A\tC2\n" +
        "C2\tA").getBytes)

    val pipe = DAGMultiMapLookup(context.getSession, "", null,
      tmpDir.toPath.resolve("cyclictest").toString, List(2), caseInsensitive = true, "Missing", returnMiss = true, showDAGPAth = true, "->", 50) | sink
    pipe.setup()

    val header = "ChromNOR\tPosNOR\tPN"
    pipe.setRowHeader(RowHeader(header, List("S", "I", "S").toArray))

    val thrown = intercept[GorDataException](pipe.process(RowObj("ChromN\t0\tP1")))
    assert(thrown.getMessage == "Depth > 50 detected!!! The graph used with INDAG is most likely not a DAG!")
  }
}
