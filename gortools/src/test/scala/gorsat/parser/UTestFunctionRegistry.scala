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

import gorsat.parser.FunctionSignature._
import gorsat.parser.FunctionTypes.{dFun, iFun, lFun, sFun}
import org.junit.runner.RunWith
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class UTestFunctionRegistry extends AnyFlatSpec {
  "register" should "handle an arity 0 function" in {
    def func(): lFun = {
      _ => 42L
    }
    val signature = getSignatureEmpty2Long(() => func())
    val fr = FunctionRegistry()
    fr.register("TEST", signature, func _ )
    val wrapper = fr.lookupWrapper(s"TEST_$signature")
    assert(wrapper.name == "TEST")
  }

  it should "handle an arity 0 function with owner arg" in {
    def func(owner: ParseArith): iFun = {
      _ => 42
    }
    val fr = FunctionRegistry()
    val signature = getSignatureEmpty2Int(removeOwner(func))
    fr.registerWithOwner("TEST", signature, func _)
    val wrapper = fr.lookupWrapper(s"TEST_$signature")
    assert(wrapper.name == "TEST")
  }

  it should "handle an arity 1 function" in {
    def func(owner: ParseArith, ex: sFun): iFun = {
      _ => 42
    }
    val fr = FunctionRegistry()
    val signature = getSignatureString2Int(removeOwner(func))
    fr.registerWithOwner("TEST", signature, func _)
    val wrapper = fr.lookupWrapper(s"TEST_$signature")
    assert(wrapper.name == "TEST")
  }

  it should "handle an arity 2 function" in {
    def func(ex1: sFun, ex2: dFun): iFun = {
      _ => 42
    }
    val fr = FunctionRegistry()
    val signature = getSignatureStringDouble2Int(func)
    fr.register("TEST", signature, func _)
    val wrapper = fr.lookupWrapper(s"TEST_$signature")
    assert(wrapper.name == "TEST")
  }

  it should "handle an arity 2 function with string literal list" in {
    def func(ex1: sFun, ex2: List[String]): iFun = {
      _ => 42
    }
    val fr = FunctionRegistry()
    val signature = getSignatureStringStringList2Int(func)
    fr.register("TEST", signature, func _)
    val wrapper = fr.lookupWrapper(s"TEST_$signature")
    assert(wrapper.name == "TEST")
  }

  it should "handle an arity 3 function" in {
    def func(ex1: sFun, ex2: dFun, ex3: iFun): iFun = {
      _ => 42
    }
    val fr = FunctionRegistry()
    val signature = getSignatureStringDoubleInt2Int(func)
    fr.register("TEST", signature, func _)
    val wrapper = fr.lookupWrapper(s"TEST_$signature")
    assert(wrapper.name == "TEST")
  }

  it should "handle an arity 4 function" in {
    def func(ex1: sFun, ex2: dFun, ex3: iFun, ex4: sFun): iFun = {
      _ => 42
    }
    val fr = FunctionRegistry()
    val signature = getSignatureStringDoubleIntString2Int(func)
    fr.register("TEST", signature, func _)
    val wrapper = fr.lookupWrapper(s"TEST_$signature")
    assert(wrapper.name == "TEST")
  }

  it should "allow different arity functions with the same name" in {
    def func1(ex: sFun): iFun = {
      _ => 42
    }
    def func3(ex1: sFun, ex2: dFun, ex3: iFun): iFun = {
      _ => 42
    }
    def func4(ex1: sFun, ex2: dFun, ex3: iFun, ex4: sFun): iFun = {
      _ => 42
    }

    val fr = FunctionRegistry()
    fr.register("TEST", getSignatureString2Int(func1), func1 _)
    fr.register("TEST", getSignatureStringDoubleInt2Int(func3), func3 _)
    fr.register("TEST", getSignatureStringDoubleIntString2Int(func4), func4 _)

    val variants = fr.getVariants("TEST")

    assert(variants.length == 3)
  }

  it should "allow different return types for functions with the same name" in {
    def func1(ex: sFun): dFun = {
      _ => 42.0
    }
    def func3(ex1: sFun, ex2: dFun, ex3: iFun): iFun = {
      _ => 42
    }

    val fr = FunctionRegistry()
    fr.register("TEST", getSignatureString2Double(func1), func1 _)
    fr.register("TEST", getSignatureStringDoubleInt2Int(func3), func3 _)

    val variants = fr.getVariants("TEST")

    assert(variants.length == 2)
  }

  "lookupWrapper" should "find the function" in {
    def func1(ex: sFun): dFun = {
      _ => 42.0
    }

    val fr = FunctionRegistry()
    fr.register("TEST", getSignatureString2Double(func1), func1 _)

    val wrapper = fr.lookupWrapper("TEST_String2Double")
    assert(wrapper.name == "TEST")
  }

  it should "find the function with underscores in the name" in {
    def func1(ex: sFun): dFun = {
      _ => 42.0
    }

    val fr = FunctionRegistry()
    fr.register("THIS_IS_A_TEST", getSignatureString2Double(func1), func1 _)

    val wrapper = fr.lookupWrapper("THIS_IS_A_TEST_String2Double")
    assert(wrapper.name == "THIS_IS_A_TEST")
  }

  it should "find an arity2 function with underscores in the name" in {
    def func(ex1: sFun, ex2: iFun): dFun = {
      _ => 42.0
    }

    val fr = FunctionRegistry()
    val sig = getSignatureStringInt2Double(func)
    fr.register("THIS_IS_A_TEST", sig, func _)

    val wrapper = fr.lookupWrapper("THIS_IS_A_TEST_" + sig)
    assert(wrapper.name == "THIS_IS_A_TEST")
  }

  "getVariants" should "return an empty list when function does not exist" in {
    val fr = FunctionRegistry()

    val variants = fr.getVariants("bingo")
    assert(variants.isEmpty)
  }

  "getVariantsByReturnType" should "return all variants that match the return type" in {
    def func1(ex1: sFun, ex2: iFun): dFun = {
      _ => 42.0
    }
    def func2(ex1: sFun, ex2: iFun, ex3: sFun): dFun = {
      _ => 42.0
    }
    def func3(ex1: sFun, ex2: iFun, ex3: sFun): iFun = {
      _ => 42
    }

    val fr = FunctionRegistry()
    fr.register("TEST", getSignatureStringInt2Double(func1), func1 _)
    fr.register("TEST", getSignatureStringIntString2Double(func2), func2 _)
    fr.register("TEST", getSignatureStringIntString2Int(func3), func3 _)

    val variants = fr.getVariantsByReturnType("TEST", FunctionTypes.DoubleFun)
    assert(variants.length == 2)
  }
}
