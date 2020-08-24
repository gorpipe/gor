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

package gorsat.parser

import org.gorpipe.model.genome.files.gor.Row
import org.gorpipe.model.gor.RowObj
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class UTestFixedSizeIteratorCvp extends FlatSpec {
  private def getRow(contents: String): Row = {
    val prefix = "chr1\t1\t"
    val row = RowObj(prefix + contents)
    row
  }

  private def getIterator(data: String, itemSize: Int = 1): FixedSizeIteratorCvp = {
    var r = getRow(data)
    val x = FixedSizeIteratorCvp(r, data, itemSize)
    x
  }

  "hasNext" should "return false on empty string" in {
    val x = getIterator("")
    assert(!x.hasNext)
  }

  it should "return true for a single item list" in {
    val x = getIterator("a")
    assert(x.hasNext)
  }

  "next" should "work for a single item list" in {
    val x = getIterator("a")
    x.next()
    assert(!x.hasNext)
  }

  it should "iterate over whole list" in {
    val x = getIterator("abcdefgh")
    var count = 0
    while(x.hasNext) {
      count += 1
      x.next()
    }
    assert(count == 8)
  }

  it should "iterate over whole list when item size is 2" in {
    val x = getIterator("abcdefgh", 2)
    var count = 0
    while(x.hasNext) {
      count += 1
      x.next()
    }
    assert(count == 4)
  }

  "stringValue" should "support list item" in {
    val x = getIterator("abcdef")
    x.next()
    assert(x.stringValue(SpecialColumns.ListItem) == "a")
    x.next()
    assert(x.stringValue(SpecialColumns.ListItem) == "b")
    x.next()
    assert(x.stringValue(SpecialColumns.ListItem) == "c")
  }

  it should "support list index" in {
    val x = getIterator("abcdef")
    x.next()
    assert(x.stringValue(SpecialColumns.ListIndex) == "1")
    x.next()
    assert(x.stringValue(SpecialColumns.ListIndex) == "2")
    x.next()
    assert(x.stringValue(SpecialColumns.ListIndex) == "3")
  }

  it should "support getting string data from row" in {
    val x = getIterator("abcdef")
    val value = x.stringValue(0)
    assert(value == "chr1")
  }

  "intValue" should "support getting int data from row" in {
    val x = getIterator("a")
    val value = x.intValue(1)
    assert(value == 1)
  }

  it should "support list index" in {
    val x = getIterator("10")
    x.next()
    val value = x.intValue(SpecialColumns.ListIndex)
    assert(value == 1)
  }

  it should "support list item" in {
    val x = getIterator("10", 2)
    x.next()
    val value = x.intValue(SpecialColumns.ListItem)
    assert(value == 10)
  }

  "longValue" should "support getting long data from row" in {
    val x = getIterator("10", 2)
    val value = x.longValue(1)
    assert(value == 1)
  }

  it should "support list index" in {
    val x = getIterator("10", 2)
    x.next()
    val value = x.longValue(SpecialColumns.ListIndex)
    assert(value == 1)
  }

  it should "support list item" in {
    val x = getIterator("10", 2)
    x.next()
    val value = x.longValue(SpecialColumns.ListItem)
    assert(value == 10)
  }

  "doubleValue" should "support getting double data from row" in {
    val x = getIterator("10", 2)
    val value = x.doubleValue(1)
    assert(value == 1.0)
  }

  it should "support list index" in {
    val x = getIterator("10", 2)
    x.next()
    val value = x.doubleValue(SpecialColumns.ListIndex)
    assert(value == 1.0)
  }

  it should "support list item" in {
    val x = getIterator("10", 2)
    x.next()
    val value = x.doubleValue(SpecialColumns.ListItem)
    assert(value == 10.0)
  }

  "next" should "throw on empty list" in {
    val x = getIterator("")
    assertThrows[NoSuchElementException](x.next())
  }

}
