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

import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class UTestFunctionTypes extends FlatSpec {
  "getArgumentTypesFromSignature" should "return a list of args" in {
    val types = FunctionTypes.getArgumentTypesFromSignature("Int:Double:String2String")

    assert(types.length == 3)
  }

  it should "return an empty list for a function with no args" in {
    val types = FunctionTypes.getArgumentTypesFromSignature("e2String")

    assert(types.isEmpty)
  }

  "getReturnTypeFromSignature" should "return the return type" in {
    val rt = FunctionTypes.getReturnTypeFromSignature("e2String")
    assert(rt == "String")
  }

  "getSignature" should "return the proper signature for arity 0" in {
    val sig = FunctionTypes.getSignature(Nil, FunctionTypes.StringFun)
    assert(sig == "e2String")
  }
}
