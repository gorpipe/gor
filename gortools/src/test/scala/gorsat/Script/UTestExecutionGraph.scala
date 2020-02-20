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

package gorsat.Script

import org.scalatest.FlatSpec

class UTestExecutionGraph extends FlatSpec {
  "Empty graph" should "have empty root" in {
    val g = ExecutionGraph(Array[String]())
    assert(g.root == null)
  }

  "Single command" should "have the command as the root" in {
    val g = ExecutionGraph(Array("gor test.gor"))
    assert(g.root.groupName == ScriptExecutionEngine.GOR_FINAL)
  }

  "Simple chain" should "work for single create" in {
    val g = ExecutionGraph(Array("create x = gor test.gor", "gor [x]"))
    assert(g.root.groupName == ScriptExecutionEngine.GOR_FINAL)
    assert(g.blocks.size == 2)
    assert(g.levels.length == 2)
  }

  it should "work for several creates" in {
    val g = ExecutionGraph(Array("create x = gor test.gor", "create y = gor [x]", "create z = gor [y]", "gor [z]"))
    assert(g.root.groupName == ScriptExecutionEngine.GOR_FINAL)
    assert(g.blocks.size == 4)
    assert(g.levels.length == 4)
    assert(g.levels(0).head.groupName == "x")
    assert(g.levels(1).head.groupName == "y")
    assert(g.levels(2).head.groupName == "z")
    assert(g.levels(3).head.groupName == ScriptExecutionEngine.GOR_FINAL)
  }

  "Simple graph" should "work for several creates" in {
    val g = ExecutionGraph(Array(
      "create w = gor test.gor",
      "create x = gor right.gor",
      "create y = gor left.gor | join [x]" ,
      "create z = gor [y] [w]",
      "gor [z]"
    ))
    assert(g.blocks.size == 5)
    assert(g.levels.length == 4)
  }

  it should "correctly handle dependencies across levels" in {
    val g = ExecutionGraph(Array(
      "create w = gor test.gor",
      "create x = gor right.gor",
      "create y = gor left.gor | join [x]" ,
      "gor [w] [x] [y]"
    ))
    assert(g.blocks.size == 4)
    assert(g.levels.length == 3)
  }
}
