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

import gorsat.Commands.RowHeader
import gorsat.Script.ScriptExecutionEngine
import gorsat.process.GenericSessionFactory
import org.gorpipe.exceptions.GorDataException
import org.gorpipe.gor.GorContext
import org.gorpipe.model.gor.RowObj
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class UTestThrowIfAnalysis extends FlatSpec {
  // This is needed to initialize things needed by GorPipeSession
  private val se = ScriptExecutionEngine
  private val context = new GenericSessionFactory().create().getGorContext

  "process" should "pass single row through when condition is false" in {
    val sink = AnalysisSink()
    val header = "chrom\tpos\tA"
    val pipe = ThrowIfAnalysis(context, executeNor = false, "A=='CDE'", header) | sink
    pipe.setRowHeader(RowHeader(header, List("S", "I", "S").toArray))

    val r = RowObj("chr1\t1\tABC")
    pipe.process(r)

    val resultingRow = sink.rows.head
    assert(resultingRow.toString == "chr1\t1\tABC")
  }

  it should "throw exception on single row when condition is true" in {
    val sink = AnalysisSink()
    val header = "chrom\tpos\tA"
    val pipe = ThrowIfAnalysis(context, executeNor = false, "A=='ABC'", header) | sink
    pipe.setRowHeader(RowHeader(header, List("S", "I", "S").toArray))

    val r = RowObj("chr1\t1\tABC")
    val thrown = intercept[GorDataException](pipe.process(r))
    assert(thrown.getMessage == "Gor throw on: A=='ABC'")
  }

}
