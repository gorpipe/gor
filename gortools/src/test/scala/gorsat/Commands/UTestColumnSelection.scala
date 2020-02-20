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

package gorsat.Commands

import gorsat.Script.ScriptExecutionEngine
import gorsat.process.{GenericSessionFactory, GorInputSources, GorPipeCommands}
import org.gorpipe.gor.GorContext
import org.gorpipe.test.utils.FileTestUtils
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, FlatSpec}

@RunWith(classOf[JUnitRunner])
class UTestColumnSelection extends FlatSpec with BeforeAndAfterAll {
  // This is needed to initialize things needed by GorPipeSession
  val se = ScriptExecutionEngine
  private val context = new GenericSessionFactory().create().getGorContext

  protected var pnsTxtPath = ""
  protected var pnsTsvPath = ""

  override protected def beforeAll(): Unit = {
    GorPipeCommands.register()
    GorInputSources.register()
    var tempDirectory = FileTestUtils.createTempDirectory(this.getClass.getName)
    var pnsTxt = FileTestUtils.createPNTxtFile(tempDirectory)
    var pnsTsv = FileTestUtils.createPNTsvFile(tempDirectory)
    pnsTxtPath = pnsTxt.getCanonicalPath
    pnsTsvPath = pnsTsv.getCanonicalPath
  }

  "isRange" should "return false for an empty selection" in {
    val cs = ColumnSelection("", "", context)
    assert(!cs.isRange)
    assert(cs.isEmpty)
  }

  it should "return true for a single column" in {
    val cs = ColumnSelection("a\tb\tc", "2", context)
    assert(cs.isRange)
    assert(!cs.isEmpty)
  }

  it should "return true for a single column using name" in {
    val cs = ColumnSelection("a\tb\tc", "b", context)
    assert(cs.isRange)
    assert(!cs.isEmpty)
  }

  it should "return true for a single column using name when case doesn't match" in {
    val cs = ColumnSelection("A\tB\tC", "b", context)
    assert(cs.isRange)
    assert(!cs.isEmpty)
  }

  it should "return true for an open-ended range" in {
    val cs = ColumnSelection("a\tb\tc", "a-", context)
    assert(cs.isRange)
    assert(!cs.isEmpty)
  }

  "isList" should "return false for an empty selection" in {
    val cs = ColumnSelection("", "", context)
    assert(!cs.isList)
    assert(cs.isEmpty)
  }

  it should "return true for a simple list" in {
    val cs = ColumnSelection("a\tb\tc", "1,2,3", context)
    assert(cs.isList)
    assert(!cs.isEmpty)
  }

  "isEmpty" should "return true for an empty selection" in {
    val cs = ColumnSelection("", "", context)
    assert(cs.isEmpty)
  }

  "isQuery" should "return false for an empty selection" in {
    val cs = ColumnSelection("", "", context)
    assert(!cs.isQuery)
    assert(cs.isEmpty)
  }

  it should "return true for a .nor file" in {
    val cs = ColumnSelection("", "test.nor", context)
    assert(cs.isQuery)
    assert(!cs.isEmpty)
  }

  it should "return true for a .norz file" in {
    val cs = ColumnSelection("", "test.norz", context)
    assert(cs.isQuery)
    assert(!cs.isEmpty)
  }

  it should "return true for a .gor file" in {
    val cs = ColumnSelection("", "test.gor", context)
    assert(cs.isQuery)
    assert(!cs.isEmpty)
  }

  it should "return true for a .gorz file" in {
    val cs = ColumnSelection("", "test.gorz", context)
    assert(cs.isQuery)
    assert(!cs.isEmpty)
  }

  it should "return true for a nested query" in {
    val cs = ColumnSelection("", "<()", context)
    assert(cs.isQuery)
    assert(!cs.isEmpty)
  }

  "columns" should "have entries for a simple list" in {
    val cs = ColumnSelection("a\tb\tc", "1,2,3", context)
    assert(cs.columns == List(0, 1, 2))
  }

  it should "have entries for a mix of range and columns" in {
    val cs = ColumnSelection("a\tb\tc\td\te\tf\tg\th", "b-d,f,h", context)
    assert(cs.columns == List(1, 2, 3, 5, 7))
    assert(cs.isList)
    assert(!cs.isQuery)
    assert(!cs.isEmpty)
  }

  it should "have entries for a nested nor query" in {
    val cs = ColumnSelection("a\tb\tc\td\te\tf\tg\th", "<(nor " + pnsTsvPath + ")", context)
    assert(cs.columns == List(0, 1))
    assert(cs.isList)
  }

  it should "have entries for a .txt file reference (no header in file)" in {
    val cs = ColumnSelection("a\tb\tc\td\te\tf\tg\th", pnsTsvPath, context)
    assert(cs.columns == List(0, 1))
    assert(cs.isList)
  }

  it should "have entries for a .tsv file reference" in {
    val cs = ColumnSelection("a\tb\tc\td\te\tf\tg\th", pnsTsvPath, context)
    assert(cs.columns == List(0, 1))
    assert(cs.isList)
  }

  it should "have entries for a range" in {
    val cs = ColumnSelection("a\tb\tc\td\te\tf\tg\th", "2-4", context)
    assert(cs.columns == List(1, 2, 3))
  }

  it should "have entries when using a wildcard" in {
    val cs = ColumnSelection("Aa\tb\tAc\td\tAe\tf\tg\th", "A*", context)
    assert(cs.columns == List(0, 2, 4))
  }

  it should "have entries when using a wildcard in a list" in {
    val cs = ColumnSelection("Aa\tb\tAc\td\tAe\tf\tg\th", "A*,f-h", context)
    assert(cs.columns == List(0, 2, 4, 5, 6, 7))
  }

  "range" should "be valid for single column index" in {
    val cs = ColumnSelection("a\tb\tc", "2", context)
    assert(cs.firstInRange == 1)
    assert(cs.lastInRange == 1)
  }

  it should "be valid for single column name" in {
    val cs = ColumnSelection("a\tb\tc", "b", context)
    assert(cs.firstInRange == 1)
    assert(cs.lastInRange == 1)
  }

  it should "be valid for a simple range" in {
    val cs = ColumnSelection("a\tb\tc\td\te\tf\tg\th", "2-4", context)
    assert(cs.firstInRange == 1)
    assert(cs.lastInRange == 3)
  }

  it should "be valid for a simple range using names" in {
    val cs = ColumnSelection("a\tb\tc\td\te\tf\tg\th", "b-d", context)
    assert(cs.firstInRange == 1)
    assert(cs.lastInRange == 3)
  }

  it should "be valid for an open-ended range" in {
    val cs = ColumnSelection("a\tb\tc\td\te\tf\tg\th", "2-", context)
    assert(cs.firstInRange == 1)
    assert(cs.lastInRange == 7)
  }

  it should "be valid for an open-ended range using names" in {
    val cs = ColumnSelection("a\tb\tc\td\te\tf\tg\th", "b-", context)
    assert(cs.firstInRange == 1)
    assert(cs.lastInRange == 7)
  }

  it should "not be valid for a non-existing start column" in {
    val cs = ColumnSelection("a\tb\tc\td\te\tf\tg\th", "14-17", context)
    assert(!cs.isRange)
  }

  it should "be valid when using positive relative column reference" in {
    val cs = ColumnSelection("a\tb\tc\td\te\tf\tg\th", "b[+1]-e", context)
    assert(cs.firstInRange == 2)
    assert(cs.lastInRange == 4)
  }

  it should "be valid when using negative relative column reference" in {
    val cs = ColumnSelection("a\tb\tc\td\te\tf\tg\th", "b[+1]-f[-1]", context)
    assert(cs.firstInRange == 2)
    assert(cs.lastInRange == 4)
  }

  "header" should "return valid header" in {
    val cs = ColumnSelection("a\tb\tC\td\te\tf\tg\th", "b-d", context)
    assert(cs.header == "b\tC\td")
  }
}
