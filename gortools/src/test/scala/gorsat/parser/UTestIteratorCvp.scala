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

package gorsat.parser

import org.gorpipe.gor.model.Row
import org.gorpipe.model.gor.RowObj
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class UTestIteratorCvp extends FlatSpec {
  private def getRow(contents: String): Row = {
    val prefix = "chr1\t1\t"
    val row = RowObj(prefix + contents)
    row
  }

  private def getIteratorCVP(data: String, delimiter: String = ",") = {
    var r = getRow(data)
    val x = IteratorCvp(r, data, delimiter)
    x
  }

  "hasNext" should "allow empty string" in {
    val x: IteratorCvp = getIteratorCVP("")
    assert(!x.hasNext)
  }

  it should "allow single item" in {
    val x: IteratorCvp = getIteratorCVP("a")
    assert(x.hasNext)
    x.next()
    assert(x.stringValue(SpecialColumns.ListItem) == "a")
    assert(!x.hasNext)
  }

  it should "allow multiple items" in {
    val x: IteratorCvp = getIteratorCVP("a,b,c,d,e,f")
    assert(x.hasNext)
    var count = 0
    while(x.hasNext) {
      count += 1
      x.next()
    }
    assert(count == 6)
  }

  "stringValue" should "support list item" in {
    val x: IteratorCvp = getIteratorCVP("a")

    x.next()
    val current = x.stringValue(SpecialColumns.ListItem)
    assert(current == "a")
  }

  it should "support list index" in {
    val x: IteratorCvp = getIteratorCVP("a,b,c,d,e,f")
    x.next()
    assert(x.stringValue(SpecialColumns.ListIndex) == "1")
    x.next()
    assert(x.stringValue(SpecialColumns.ListIndex) == "2")
    x.next()
    assert(x.stringValue(SpecialColumns.ListIndex) == "3")
  }

  it should "support getting string data from row" in {
    val x: IteratorCvp = getIteratorCVP("a")
    val value = x.stringValue(0)
    assert(value == "chr1")
  }

  "intValue" should "support getting int data from row" in {
    val x: IteratorCvp = getIteratorCVP("a")
    val value = x.intValue(1)
    assert(value == 1)
  }

  it should "support list index" in {
    val x: IteratorCvp = getIteratorCVP("10")
    x.next()
    val value = x.intValue(SpecialColumns.ListIndex)
    assert(value == 1)
  }

  it should "support list item" in {
    val x: IteratorCvp = getIteratorCVP("10")
    x.next()
    val value = x.intValue(SpecialColumns.ListItem)
    assert(value == 10)
  }

  "longValue" should "support getting long data from row" in {
    val r = getRow("a")
    val x = IteratorCvp(r, "a")
    val value = x.longValue(1)
    assert(value == 1)
  }

  it should "support list index" in {
    val x: IteratorCvp = getIteratorCVP("10")
    x.next()
    val value = x.longValue(SpecialColumns.ListIndex)
    assert(value == 1)
  }

  it should "support list item" in {
    val x: IteratorCvp = getIteratorCVP("10")
    x.next()
    val value = x.longValue(SpecialColumns.ListItem)
    assert(value == 10)
  }

  "doubleValue" should "support getting double data from row" in {
    val r = getRow("a")
    val x = IteratorCvp(r, "a")
    val value = x.doubleValue(1)
    assert(value == 1.0)
  }

  it should "support list index" in {
    val x: IteratorCvp = getIteratorCVP("10")
    x.next()
    val value = x.doubleValue(SpecialColumns.ListIndex)
    assert(value == 1.0)
  }

  it should "support list item" in {
    val x: IteratorCvp = getIteratorCVP("10")
    x.next()
    val value = x.doubleValue(SpecialColumns.ListItem)
    assert(value == 10.0)
  }

  "next" should "throw on empty list" in {
    val x: IteratorCvp = getIteratorCVP("")
    assertThrows[NoSuchElementException](x.next())
  }

  it should "throw on end of list" in {
    val x = getIteratorCVP("1,2,3")
    x.next()
    x.next()
    x.next()
    assertThrows[NoSuchElementException](x.next())
  }

  it should "support two char delimiter" in {
    val x = getIteratorCVP("1xx2xx3", "xx")
    x.next()
    assert(x.stringValue(SpecialColumns.ListItem) == "1")
    x.next()
    assert(x.stringValue(SpecialColumns.ListItem) == "2")
    x.next()
    assert(x.stringValue(SpecialColumns.ListItem) == "3")
    assert(!x.hasNext)
  }

  it should "support empty delimiter" in {
    val x = getIteratorCVP("123", "")
    x.next()
    assert(x.stringValue(SpecialColumns.ListItem) == "1")
    x.next()
    assert(x.stringValue(SpecialColumns.ListItem) == "2")
    x.next()
    assert(x.stringValue(SpecialColumns.ListItem) == "3")
    assert(!x.hasNext)
  }
}

