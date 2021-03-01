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

import gorsat.Commands.RowHeader
import org.gorpipe.model.gor.RowObj
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class UTestInferColumnTypes extends FlatSpec {
  private val header = "chrom\tpos\tA\tB\tC"

  "Constructor" should "construct" in {
    val step = InferColumnTypes()
  }

  "type inferral" should "work for single row, all strings" in {
    val sink = AnalysisSink()
    val step = InferColumnTypes()
    step.setRowHeader(RowHeader(header))
    val pipe = step | sink
    val r = RowObj("chr1\t1\tabc\tdef\tghi")

    step.process(r)
    step.finish()

    assert(step.columnTypes(2) == "S")
    assert(step.columnTypes(3) == "S")
    assert(step.columnTypes(4) == "S")
  }

  it should "work for single row, with numbers" in {
    val sink = AnalysisSink()
    val step = InferColumnTypes()
    step.setRowHeader(RowHeader(header))
    val pipe = step | sink
    val r = RowObj("chr1\t1\t10\t3.14\t123456789012")

    step.process(r)
    step.finish()

    assert(step.columnTypes(2) == "I")
    assert(step.columnTypes(3) == "D")
    assert(step.columnTypes(4) == "L")
  }

  it should "work for multiple rows, with numbers" in {
    val sink = AnalysisSink()
    val step = InferColumnTypes()
    step.setRowHeader(RowHeader(header))
    val pipe = step | sink

    step.process(RowObj("chr1\t1\t10\t3.14\t123456789012"))
    step.process(RowObj("chr1\t1\t11\t4.14\t223456789012"))
    step.process(RowObj("chr1\t1\t12\t5.14\t323456789012"))
    step.process(RowObj("chr1\t1\t13\t6.14\t423456789012"))
    step.finish()

    assert(step.columnTypes(2) == "I")
    assert(step.columnTypes(3) == "D")
    assert(step.columnTypes(4) == "L")
  }

  it should "work for multiple rows, numbers in all but one" in {
    val sink = AnalysisSink()
    val step = InferColumnTypes()
    step.setRowHeader(RowHeader(header))
    val pipe = step | sink

    step.process(RowObj("chr1\t1\t10\t3.14\t123456789012"))
    step.process(RowObj("chr1\t1\t11\t4.14\t223456789012"))
    step.process(RowObj("chr1\t1\t12\t5.14\t323456789012"))
    step.process(RowObj("chr1\t1\tx13\ty6.14\tz423456789012"))
    step.finish()

    assert(step.columnTypes(2) == "S")
    assert(step.columnTypes(3) == "S")
    assert(step.columnTypes(4) == "S")
  }

  it should "set column as Integer when all values are empty" in {
    val sink = AnalysisSink()
    val step = InferColumnTypes()
    step.setRowHeader(RowHeader(header))
    val pipe = step | sink

    step.process(RowObj("chr1\t1\t10\t3.14\t"))
    step.process(RowObj("chr1\t1\t11\t4.14\t"))
    step.process(RowObj("chr1\t1\t12\t5.14\t"))
    step.process(RowObj("chr1\t1\t13\t6.14\t"))
    step.finish()

    assert(step.columnTypes(2) == "I")
    assert(step.columnTypes(3) == "D")
    assert(step.columnTypes(4) == "I")
  }

  it should "set column as number when all values are numbers or empty" in {
    val sink = AnalysisSink()
    val step = InferColumnTypes()
    step.setRowHeader(RowHeader(header))
    val pipe = step | sink

    step.process(RowObj("chr1\t1\t10\t3.14\t123456789012"))
    step.process(RowObj("chr1\t1\t\t4.14\t123456789012"))
    step.process(RowObj("chr1\t1\t12\t\t123456789012"))
    step.process(RowObj("chr1\t1\t13\t6.14\t"))
    step.finish()

    assert(step.columnTypes(2) == "I")
    assert(step.columnTypes(3) == "D")
    assert(step.columnTypes(4) == "L")
  }

  it should "set header on next step" in {
    val sink = AnalysisSink()
    val step = InferColumnTypes()
    step.setRowHeader(RowHeader(header))
    val pipe = step | sink

    step.process(RowObj("chr1\t1\t10\t3.14\t123456789012"))
    step.finish()

    assert(sink.rowHeader.columnTypes(2) == "I")
  }
}
