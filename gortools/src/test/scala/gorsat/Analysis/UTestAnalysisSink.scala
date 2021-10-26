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

import org.gorpipe.model.gor.RowObj
import org.junit.runner.RunWith
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class UTestAnalysisSink extends AnyFlatSpec {
  "Constructor" should "construct object with empty rows list" in {
    val s = AnalysisSink()
    assert(s.rows.isEmpty)
  }

  "process" should "retain rows processed" in {
    val s = AnalysisSink()
    s.process(RowObj("chr1\t1\tA"))
    s.process(RowObj("chr1\t1\tB"))
    s.process(RowObj("chr1\t1\tC"))
    s.process(RowObj("chr1\t1\tD"))

    assert(s.rows.length == 4)
  }
}
