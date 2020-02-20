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
import org.gorpipe.gor.GorContext
import org.gorpipe.model.gor.RowObj
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class UTestReplaceAnalysis extends FlatSpec {
  // This is needed to initialize things needed by GorPipeSession
  val se = ScriptExecutionEngine
  val context = new GenericSessionFactory().create().getGorContext

  "process" should "handle a single column replace" in {
    val sink = AnalysisSink()
    val header = "chrom\tpos\tA"
    val pipe = ReplaceAnalysis(context, executeNor = false, "lower(A)", header, List(2).toArray) | sink
    pipe.setRowHeader(RowHeader(header, List("S", "I", "S").toArray))

    val r = RowObj("chr1\t1\tABC")
    pipe.process(r)

    val resultingRow = sink.rows.head
    assert(resultingRow.toString == "chr1\t1\tabc")
  }

  it should "handle a single column replace when data doesn't match type" in {
    val sink = AnalysisSink()
    val header = "chrom\tpos\tA"
    val pipe = ReplaceAnalysis(context, executeNor = false, "if(A='NA', 'NaN', A)", header, List(2).toArray) | sink
    pipe.setRowHeader(RowHeader(header, List("S", "I", "S").toArray))

    val r = RowObj("chr1\t1\t3.14")
    pipe.process(r)

    val resultingRow = sink.rows.head
    assert(resultingRow.toString == "chr1\t1\t3.14")
  }

  it should "handle a multiple column replace with single expression" in {
    val sink = AnalysisSink()
    val header = "chrom\tpos\tA\tB\tC\tD"
    val pipe = ReplaceAnalysis(context, executeNor = false, "lower(#rc)", header, List(2, 3, 4).toArray) | sink
    pipe.setRowHeader(RowHeader(header, List("S", "I", "S", "I", "D", "S").toArray))

    val r = RowObj("chr1\t1\tABC\t14\t-3.14\tTHIS is the end")
    pipe.process(r)

    val resultingRow = sink.rows.head
    assert(resultingRow.toString == "chr1\t1\tabc\t14\t-3.14\tTHIS is the end")
  }

  it should "handle a multiple column replace" in {
    val sink = AnalysisSink()
    val header = "chrom\tpos\tA\tB\tC\tD"
    val pipe = ReplaceAnalysis(context, executeNor = false, "lower(A),3*B,abs(C)", header, List(2, 3, 4).toArray) | sink
    pipe.setRowHeader(RowHeader(header, List("S", "I", "S", "I", "D", "S").toArray))

    val r = RowObj("chr1\t1\tABC\t14\t-3.14\tthis is the end")
    pipe.process(r)

    val resultingRow = sink.rows.head
    assert(resultingRow.toString == "chr1\t1\tabc\t42\t3.14\tthis is the end")
  }

  it should "handle a multiple column replace with a gap" in {
    val sink = AnalysisSink()
    val header = "chrom\tpos\tA\tB\tC\tD"
    val pipe = ReplaceAnalysis(context, executeNor = false, "'XX'", header, List(2, 4).toArray) | sink
    pipe.setRowHeader(RowHeader(header, List("S", "I", "S", "I", "D", "S").toArray))

    val r = RowObj("chr1\t1\tABC\t14\t-3.14\tthis is the end")
    pipe.process(r)

    val resultingRow = sink.rows.head
    assert(resultingRow.toString == "chr1\t1\tXX\t14\tXX\tthis is the end")
  }

  it should "handle a multiple column replace with columns out of order" in {
    val sink = AnalysisSink()
    val header = "chrom\tpos\tA\tB\tC\tD"
    val pipe = ReplaceAnalysis(context, executeNor = false, "3*B,lower(A),abs(C)", header, List(3, 2, 4).toArray) | sink
    pipe.setRowHeader(RowHeader(header, List("S", "I", "S", "I", "D", "S").toArray))

    val r = RowObj("chr1\t1\tABC\t14\t-3.14\tthis is the end")
    pipe.process(r)

    val resultingRow = sink.rows.head
    assert(resultingRow.toString == "chr1\t1\tabc\t42\t3.14\tthis is the end")
  }

  it should "correctly replace last column" in {
    val sink = AnalysisSink()
    val header = "chrom\tpos\tA\tB\tC\tD"
    val pipe = ReplaceAnalysis(context, executeNor = false, "upper(#rc)", header, List(5).toArray) | sink
    pipe.setRowHeader(RowHeader(header, List("S", "I", "S", "I", "D", "S").toArray))

    val r = RowObj("chr1\t1\tABC\t14\t-3.14\tthis is the end")
    pipe.process(r)

    val resultingRow = sink.rows.head
    assert(resultingRow.toString == "chr1\t1\tABC\t14\t-3.14\tTHIS IS THE END")
  }

  it should "correctly replace last column even when empty" in {
    val sink = AnalysisSink()
    val header = "chrom\tpos\tA\tB\tC\tD"
    val pipe = ReplaceAnalysis(context, executeNor = false, "'THIS IS THE END'", header, List(5).toArray) | sink
    pipe.setRowHeader(RowHeader(header, List("S", "I", "S", "I", "D", "S").toArray))

    val r = RowObj("chr1\t1\tABC\t14\t-3.14\t")
    pipe.process(r)

    val resultingRow = sink.rows.head
    assert(resultingRow.toString == "chr1\t1\tABC\t14\t-3.14\tTHIS IS THE END")
    assert(resultingRow.numCols() == 6)
  }

  it should "correctly replace second to last column when last column is empty" in {
    val sink = AnalysisSink()
    val header = "chrom\tpos\tA\tB\tC\tD"
    val pipe = ReplaceAnalysis(context, executeNor = false, "'THIS IS THE END'", header, List(4).toArray) | sink
    pipe.setRowHeader(RowHeader(header, List("S", "I", "S", "I", "D", "S").toArray))

    val r = RowObj("chr1\t1\tABC\t14\t-3.14\t")
    pipe.process(r)

    val resultingRow = sink.rows.head
    assert(resultingRow.toString == "chr1\t1\tABC\t14\tTHIS IS THE END\t")
    assert(resultingRow.numCols() == 6)
  }
}
