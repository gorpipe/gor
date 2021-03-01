/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
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

import gorsat.Commands.{ColumnSelection, RowHeader}
import gorsat.Script.ScriptExecutionEngine
import gorsat.process.GenericSessionFactory
import org.gorpipe.model.gor.RowObj
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class UTestCols2ListAnalysis extends FlatSpec {
  // This is needed to initialize things needed by GorPipeSession
  private val se = ScriptExecutionEngine
  private val context = new GenericSessionFactory().create().getGorContext

  "Constructor" should "construct" in {
    val columns = ColumnSelection("", "", context)
    val include = ColumnSelection("", "", context)

    val c = Cols2ListAnalysis(columns, include, ",", forNor = false, "", RowHeader(""))
  }

  "process" should "collapse columns" in {
    val sink = AnalysisSink()
    val header = "chrom\tpos\tA\tB\tC\tD\tE\tF\tG\tH\tI"
    val columns = ColumnSelection(header, "a,b,c", context)
    val include = ColumnSelection(header, "chrom,pos", context)

    val c = Cols2ListAnalysis(columns, include, ",", forNor = false, "", RowHeader(header)) | sink

    val r = RowObj("chr1\t1\tA\tB\tC\tD\tE\tF\tG\tH\tI")

    c.process(r)

    val resultingRow = sink.rows.head
    assert(resultingRow.numCols() == 3)
    assert(resultingRow.toString == "chr1\t1\tA,B,C")
  }

  it should "collapse columns in a nor context" in {
    val sink = AnalysisSink()
    val header = "chromNOR\tposNOR\tA\tB\tC\tD\tE\tF\tG\tH\tI"
    val columns = ColumnSelection(header, "a,b,c", context)
    val include = ColumnSelection(header, "chromNOR,posNOR", context)

    val c = Cols2ListAnalysis(columns, include, ",", forNor = true, "", RowHeader(header)) | sink

    val r = RowObj("chrN\t0\tA\tB\tC\tD\tE\tF\tG\tH\tI")

    c.process(r)

    val resultingRow = sink.rows.head
    assert(resultingRow.numCols() == 3)
    assert(resultingRow.toString == "chrN\t0\tA,B,C")
  }

  it should "collapse using range" in {
    val sink = AnalysisSink()
    val header = "chrom\tpos\tA\tB\tC\tD\tE\tF\tG\tH\tI"
    val columns = ColumnSelection(header, "a-c", context)
    val include = ColumnSelection(header, "chrom,pos", context)

    val c = Cols2ListAnalysis(columns, include, ",", forNor = false, "", RowHeader(header)) | sink

    val r = RowObj("chr1\t1\tA\tB\tC\tD\tE\tF\tG\tH\tI")

    c.process(r)

    val resultingRow = sink.rows.head
    assert(resultingRow.numCols() == 3)
    assert(resultingRow.toString == "chr1\t1\tA,B,C")
    assert(columns.isRange)
  }

  it should "collapse columns with a map expression" in {
    val sink = AnalysisSink()
    val header = "chrom\tpos\tA\tB\tC\tD\tE\tF\tG\tH\tI"
    val columns = ColumnSelection(header, "a,b,c", context)
    val include = ColumnSelection(header, "chrom,pos", context)

    val c = Cols2ListAnalysis(columns, include, ",", forNor = false, "lower(x)", RowHeader(header)) | sink

    val r = RowObj("chr1\t1\tA\tB\tC\tD\tE\tF\tG\tH\tI")

    c.process(r)

    val resultingRow = sink.rows.head
    assert(resultingRow.numCols() == 3)
    assert(resultingRow.toString == "chr1\t1\ta,b,c")
  }

  it should "collapse columns with a map expression referencing included columns" in {
    val sink = AnalysisSink()
    val header = "chrom\tpos\tA\tB\tC\tD\tE\tF\tG\tH\tI"
    val columns = ColumnSelection(header, "d-", context)
    val include = ColumnSelection(header, "chrom,pos,a,b", context)

    val c = Cols2ListAnalysis(columns, include, ",", forNor = false, "lower(x)+lower(B)", RowHeader(header)) | sink

    val r = RowObj("chr1\t1\tA\tB\tC\tD\tE\tF\tG\tH\tI")

    c.process(r)

    val resultingRow = sink.rows.head
    assert(resultingRow.numCols() == 5)
    assert(resultingRow.toString == "chr1\t1\tA\tB\tdb,eb,fb,gb,hb,ib")
  }
}
