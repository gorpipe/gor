
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

// This file is generated from a Python script - do not modify!!!
package gorsat.parser

import gorsat.parser.FunctionTypes._
import org.gorpipe.gor.model.ColumnValueProvider
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

    
@RunWith(classOf[JUnitRunner])
class UTestFunctionSignature extends FunSuite {
    
  private def iFunDummy: iFun = (cvp: ColumnValueProvider) => 42
  private def lFunDummy: lFun = (cvp: ColumnValueProvider) => 1
  private def dFunDummy: dFun = (cvp: ColumnValueProvider) => 3.14
  private def sFunDummy: sFun = (cvp: ColumnValueProvider) => "bingo"
  private def bFunDummy: bFun = (cvp: ColumnValueProvider) => false

  def helperEmpty2Int(): iFun = iFunDummy
  test("getSignature_iFun_1") {
    var result = true
    result &= FunctionSignature.getSignatureEmpty2Int(() => helperEmpty2Int()) == "e2Int"
    assert(result)
  }
  def helperEmpty2Long(): lFun = lFunDummy
  test("getSignature_lFun_1") {
    var result = true
    result &= FunctionSignature.getSignatureEmpty2Long(() => helperEmpty2Long()) == "e2Long"
    assert(result)
  }
  def helperEmpty2Double(): dFun = dFunDummy
  test("getSignature_dFun_1") {
    var result = true
    result &= FunctionSignature.getSignatureEmpty2Double(() => helperEmpty2Double()) == "e2Double"
    assert(result)
  }
  def helperEmpty2String(): sFun = sFunDummy
  test("getSignature_sFun_1") {
    var result = true
    result &= FunctionSignature.getSignatureEmpty2String(() => helperEmpty2String()) == "e2String"
    assert(result)
  }
  def helperEmpty2Boolean(): bFun = bFunDummy
  test("getSignature_bFun_1") {
    var result = true
    result &= FunctionSignature.getSignatureEmpty2Boolean(() => helperEmpty2Boolean()) == "e2Boolean"
    assert(result)
  }
  def helperInt2Int(a1: iFun): iFun = iFunDummy
  def helperLong2Int(a1: lFun): iFun = iFunDummy
  def helperDouble2Int(a1: dFun): iFun = iFunDummy
  def helperString2Int(a1: sFun): iFun = iFunDummy
  def helperBoolean2Int(a1: bFun): iFun = iFunDummy
  test("getSignature_iFun_2") {
    var result = true
    result &= FunctionSignature.getSignatureInt2Int(helperInt2Int) == "Int2Int"
    result &= FunctionSignature.getSignatureLong2Int(helperLong2Int) == "Long2Int"
    result &= FunctionSignature.getSignatureDouble2Int(helperDouble2Int) == "Double2Int"
    result &= FunctionSignature.getSignatureString2Int(helperString2Int) == "String2Int"
    result &= FunctionSignature.getSignatureBoolean2Int(helperBoolean2Int) == "Boolean2Int"
    assert(result)
  }
  def helperInt2Long(a1: iFun): lFun = lFunDummy
  def helperLong2Long(a1: lFun): lFun = lFunDummy
  def helperDouble2Long(a1: dFun): lFun = lFunDummy
  def helperString2Long(a1: sFun): lFun = lFunDummy
  def helperBoolean2Long(a1: bFun): lFun = lFunDummy
  test("getSignature_lFun_2") {
    var result = true
    result &= FunctionSignature.getSignatureInt2Long(helperInt2Long) == "Int2Long"
    result &= FunctionSignature.getSignatureLong2Long(helperLong2Long) == "Long2Long"
    result &= FunctionSignature.getSignatureDouble2Long(helperDouble2Long) == "Double2Long"
    result &= FunctionSignature.getSignatureString2Long(helperString2Long) == "String2Long"
    result &= FunctionSignature.getSignatureBoolean2Long(helperBoolean2Long) == "Boolean2Long"
    assert(result)
  }
  def helperInt2Double(a1: iFun): dFun = dFunDummy
  def helperLong2Double(a1: lFun): dFun = dFunDummy
  def helperDouble2Double(a1: dFun): dFun = dFunDummy
  def helperString2Double(a1: sFun): dFun = dFunDummy
  def helperBoolean2Double(a1: bFun): dFun = dFunDummy
  test("getSignature_dFun_2") {
    var result = true
    result &= FunctionSignature.getSignatureInt2Double(helperInt2Double) == "Int2Double"
    result &= FunctionSignature.getSignatureLong2Double(helperLong2Double) == "Long2Double"
    result &= FunctionSignature.getSignatureDouble2Double(helperDouble2Double) == "Double2Double"
    result &= FunctionSignature.getSignatureString2Double(helperString2Double) == "String2Double"
    result &= FunctionSignature.getSignatureBoolean2Double(helperBoolean2Double) == "Boolean2Double"
    assert(result)
  }
  def helperInt2String(a1: iFun): sFun = sFunDummy
  def helperLong2String(a1: lFun): sFun = sFunDummy
  def helperDouble2String(a1: dFun): sFun = sFunDummy
  def helperString2String(a1: sFun): sFun = sFunDummy
  def helperBoolean2String(a1: bFun): sFun = sFunDummy
  test("getSignature_sFun_2") {
    var result = true
    result &= FunctionSignature.getSignatureInt2String(helperInt2String) == "Int2String"
    result &= FunctionSignature.getSignatureLong2String(helperLong2String) == "Long2String"
    result &= FunctionSignature.getSignatureDouble2String(helperDouble2String) == "Double2String"
    result &= FunctionSignature.getSignatureString2String(helperString2String) == "String2String"
    result &= FunctionSignature.getSignatureBoolean2String(helperBoolean2String) == "Boolean2String"
    assert(result)
  }
  def helperInt2Boolean(a1: iFun): bFun = bFunDummy
  def helperLong2Boolean(a1: lFun): bFun = bFunDummy
  def helperDouble2Boolean(a1: dFun): bFun = bFunDummy
  def helperString2Boolean(a1: sFun): bFun = bFunDummy
  def helperBoolean2Boolean(a1: bFun): bFun = bFunDummy
  test("getSignature_bFun_2") {
    var result = true
    result &= FunctionSignature.getSignatureInt2Boolean(helperInt2Boolean) == "Int2Boolean"
    result &= FunctionSignature.getSignatureLong2Boolean(helperLong2Boolean) == "Long2Boolean"
    result &= FunctionSignature.getSignatureDouble2Boolean(helperDouble2Boolean) == "Double2Boolean"
    result &= FunctionSignature.getSignatureString2Boolean(helperString2Boolean) == "String2Boolean"
    result &= FunctionSignature.getSignatureBoolean2Boolean(helperBoolean2Boolean) == "Boolean2Boolean"
    assert(result)
  }
  def helperIntInt2Int(a1: iFun, a2: iFun): iFun = iFunDummy
  def helperIntLong2Int(a1: iFun, a2: lFun): iFun = iFunDummy
  def helperIntDouble2Int(a1: iFun, a2: dFun): iFun = iFunDummy
  def helperIntString2Int(a1: iFun, a2: sFun): iFun = iFunDummy
  def helperIntBoolean2Int(a1: iFun, a2: bFun): iFun = iFunDummy
  def helperLongInt2Int(a1: lFun, a2: iFun): iFun = iFunDummy
  def helperLongLong2Int(a1: lFun, a2: lFun): iFun = iFunDummy
  def helperLongDouble2Int(a1: lFun, a2: dFun): iFun = iFunDummy
  def helperLongString2Int(a1: lFun, a2: sFun): iFun = iFunDummy
  def helperLongBoolean2Int(a1: lFun, a2: bFun): iFun = iFunDummy
  def helperDoubleInt2Int(a1: dFun, a2: iFun): iFun = iFunDummy
  def helperDoubleLong2Int(a1: dFun, a2: lFun): iFun = iFunDummy
  def helperDoubleDouble2Int(a1: dFun, a2: dFun): iFun = iFunDummy
  def helperDoubleString2Int(a1: dFun, a2: sFun): iFun = iFunDummy
  def helperDoubleBoolean2Int(a1: dFun, a2: bFun): iFun = iFunDummy
  def helperStringInt2Int(a1: sFun, a2: iFun): iFun = iFunDummy
  def helperStringLong2Int(a1: sFun, a2: lFun): iFun = iFunDummy
  def helperStringDouble2Int(a1: sFun, a2: dFun): iFun = iFunDummy
  def helperStringString2Int(a1: sFun, a2: sFun): iFun = iFunDummy
  def helperStringBoolean2Int(a1: sFun, a2: bFun): iFun = iFunDummy
  def helperBooleanInt2Int(a1: bFun, a2: iFun): iFun = iFunDummy
  def helperBooleanLong2Int(a1: bFun, a2: lFun): iFun = iFunDummy
  def helperBooleanDouble2Int(a1: bFun, a2: dFun): iFun = iFunDummy
  def helperBooleanString2Int(a1: bFun, a2: sFun): iFun = iFunDummy
  def helperBooleanBoolean2Int(a1: bFun, a2: bFun): iFun = iFunDummy
  test("getSignature_iFun_3") {
    var result = true
    result &= FunctionSignature.getSignatureIntInt2Int(helperIntInt2Int) == "Int:Int2Int"
    result &= FunctionSignature.getSignatureIntLong2Int(helperIntLong2Int) == "Int:Long2Int"
    result &= FunctionSignature.getSignatureIntDouble2Int(helperIntDouble2Int) == "Int:Double2Int"
    result &= FunctionSignature.getSignatureIntString2Int(helperIntString2Int) == "Int:String2Int"
    result &= FunctionSignature.getSignatureIntBoolean2Int(helperIntBoolean2Int) == "Int:Boolean2Int"
    result &= FunctionSignature.getSignatureLongInt2Int(helperLongInt2Int) == "Long:Int2Int"
    result &= FunctionSignature.getSignatureLongLong2Int(helperLongLong2Int) == "Long:Long2Int"
    result &= FunctionSignature.getSignatureLongDouble2Int(helperLongDouble2Int) == "Long:Double2Int"
    result &= FunctionSignature.getSignatureLongString2Int(helperLongString2Int) == "Long:String2Int"
    result &= FunctionSignature.getSignatureLongBoolean2Int(helperLongBoolean2Int) == "Long:Boolean2Int"
    result &= FunctionSignature.getSignatureDoubleInt2Int(helperDoubleInt2Int) == "Double:Int2Int"
    result &= FunctionSignature.getSignatureDoubleLong2Int(helperDoubleLong2Int) == "Double:Long2Int"
    result &= FunctionSignature.getSignatureDoubleDouble2Int(helperDoubleDouble2Int) == "Double:Double2Int"
    result &= FunctionSignature.getSignatureDoubleString2Int(helperDoubleString2Int) == "Double:String2Int"
    result &= FunctionSignature.getSignatureDoubleBoolean2Int(helperDoubleBoolean2Int) == "Double:Boolean2Int"
    result &= FunctionSignature.getSignatureStringInt2Int(helperStringInt2Int) == "String:Int2Int"
    result &= FunctionSignature.getSignatureStringLong2Int(helperStringLong2Int) == "String:Long2Int"
    result &= FunctionSignature.getSignatureStringDouble2Int(helperStringDouble2Int) == "String:Double2Int"
    result &= FunctionSignature.getSignatureStringString2Int(helperStringString2Int) == "String:String2Int"
    result &= FunctionSignature.getSignatureStringBoolean2Int(helperStringBoolean2Int) == "String:Boolean2Int"
    result &= FunctionSignature.getSignatureBooleanInt2Int(helperBooleanInt2Int) == "Boolean:Int2Int"
    result &= FunctionSignature.getSignatureBooleanLong2Int(helperBooleanLong2Int) == "Boolean:Long2Int"
    result &= FunctionSignature.getSignatureBooleanDouble2Int(helperBooleanDouble2Int) == "Boolean:Double2Int"
    result &= FunctionSignature.getSignatureBooleanString2Int(helperBooleanString2Int) == "Boolean:String2Int"
    result &= FunctionSignature.getSignatureBooleanBoolean2Int(helperBooleanBoolean2Int) == "Boolean:Boolean2Int"
    assert(result)
  }
  def helperIntInt2Long(a1: iFun, a2: iFun): lFun = lFunDummy
  def helperIntLong2Long(a1: iFun, a2: lFun): lFun = lFunDummy
  def helperIntDouble2Long(a1: iFun, a2: dFun): lFun = lFunDummy
  def helperIntString2Long(a1: iFun, a2: sFun): lFun = lFunDummy
  def helperIntBoolean2Long(a1: iFun, a2: bFun): lFun = lFunDummy
  def helperLongInt2Long(a1: lFun, a2: iFun): lFun = lFunDummy
  def helperLongLong2Long(a1: lFun, a2: lFun): lFun = lFunDummy
  def helperLongDouble2Long(a1: lFun, a2: dFun): lFun = lFunDummy
  def helperLongString2Long(a1: lFun, a2: sFun): lFun = lFunDummy
  def helperLongBoolean2Long(a1: lFun, a2: bFun): lFun = lFunDummy
  def helperDoubleInt2Long(a1: dFun, a2: iFun): lFun = lFunDummy
  def helperDoubleLong2Long(a1: dFun, a2: lFun): lFun = lFunDummy
  def helperDoubleDouble2Long(a1: dFun, a2: dFun): lFun = lFunDummy
  def helperDoubleString2Long(a1: dFun, a2: sFun): lFun = lFunDummy
  def helperDoubleBoolean2Long(a1: dFun, a2: bFun): lFun = lFunDummy
  def helperStringInt2Long(a1: sFun, a2: iFun): lFun = lFunDummy
  def helperStringLong2Long(a1: sFun, a2: lFun): lFun = lFunDummy
  def helperStringDouble2Long(a1: sFun, a2: dFun): lFun = lFunDummy
  def helperStringString2Long(a1: sFun, a2: sFun): lFun = lFunDummy
  def helperStringBoolean2Long(a1: sFun, a2: bFun): lFun = lFunDummy
  def helperBooleanInt2Long(a1: bFun, a2: iFun): lFun = lFunDummy
  def helperBooleanLong2Long(a1: bFun, a2: lFun): lFun = lFunDummy
  def helperBooleanDouble2Long(a1: bFun, a2: dFun): lFun = lFunDummy
  def helperBooleanString2Long(a1: bFun, a2: sFun): lFun = lFunDummy
  def helperBooleanBoolean2Long(a1: bFun, a2: bFun): lFun = lFunDummy
  test("getSignature_lFun_3") {
    var result = true
    result &= FunctionSignature.getSignatureIntInt2Long(helperIntInt2Long) == "Int:Int2Long"
    result &= FunctionSignature.getSignatureIntLong2Long(helperIntLong2Long) == "Int:Long2Long"
    result &= FunctionSignature.getSignatureIntDouble2Long(helperIntDouble2Long) == "Int:Double2Long"
    result &= FunctionSignature.getSignatureIntString2Long(helperIntString2Long) == "Int:String2Long"
    result &= FunctionSignature.getSignatureIntBoolean2Long(helperIntBoolean2Long) == "Int:Boolean2Long"
    result &= FunctionSignature.getSignatureLongInt2Long(helperLongInt2Long) == "Long:Int2Long"
    result &= FunctionSignature.getSignatureLongLong2Long(helperLongLong2Long) == "Long:Long2Long"
    result &= FunctionSignature.getSignatureLongDouble2Long(helperLongDouble2Long) == "Long:Double2Long"
    result &= FunctionSignature.getSignatureLongString2Long(helperLongString2Long) == "Long:String2Long"
    result &= FunctionSignature.getSignatureLongBoolean2Long(helperLongBoolean2Long) == "Long:Boolean2Long"
    result &= FunctionSignature.getSignatureDoubleInt2Long(helperDoubleInt2Long) == "Double:Int2Long"
    result &= FunctionSignature.getSignatureDoubleLong2Long(helperDoubleLong2Long) == "Double:Long2Long"
    result &= FunctionSignature.getSignatureDoubleDouble2Long(helperDoubleDouble2Long) == "Double:Double2Long"
    result &= FunctionSignature.getSignatureDoubleString2Long(helperDoubleString2Long) == "Double:String2Long"
    result &= FunctionSignature.getSignatureDoubleBoolean2Long(helperDoubleBoolean2Long) == "Double:Boolean2Long"
    result &= FunctionSignature.getSignatureStringInt2Long(helperStringInt2Long) == "String:Int2Long"
    result &= FunctionSignature.getSignatureStringLong2Long(helperStringLong2Long) == "String:Long2Long"
    result &= FunctionSignature.getSignatureStringDouble2Long(helperStringDouble2Long) == "String:Double2Long"
    result &= FunctionSignature.getSignatureStringString2Long(helperStringString2Long) == "String:String2Long"
    result &= FunctionSignature.getSignatureStringBoolean2Long(helperStringBoolean2Long) == "String:Boolean2Long"
    result &= FunctionSignature.getSignatureBooleanInt2Long(helperBooleanInt2Long) == "Boolean:Int2Long"
    result &= FunctionSignature.getSignatureBooleanLong2Long(helperBooleanLong2Long) == "Boolean:Long2Long"
    result &= FunctionSignature.getSignatureBooleanDouble2Long(helperBooleanDouble2Long) == "Boolean:Double2Long"
    result &= FunctionSignature.getSignatureBooleanString2Long(helperBooleanString2Long) == "Boolean:String2Long"
    result &= FunctionSignature.getSignatureBooleanBoolean2Long(helperBooleanBoolean2Long) == "Boolean:Boolean2Long"
    assert(result)
  }
  def helperIntInt2Double(a1: iFun, a2: iFun): dFun = dFunDummy
  def helperIntLong2Double(a1: iFun, a2: lFun): dFun = dFunDummy
  def helperIntDouble2Double(a1: iFun, a2: dFun): dFun = dFunDummy
  def helperIntString2Double(a1: iFun, a2: sFun): dFun = dFunDummy
  def helperIntBoolean2Double(a1: iFun, a2: bFun): dFun = dFunDummy
  def helperLongInt2Double(a1: lFun, a2: iFun): dFun = dFunDummy
  def helperLongLong2Double(a1: lFun, a2: lFun): dFun = dFunDummy
  def helperLongDouble2Double(a1: lFun, a2: dFun): dFun = dFunDummy
  def helperLongString2Double(a1: lFun, a2: sFun): dFun = dFunDummy
  def helperLongBoolean2Double(a1: lFun, a2: bFun): dFun = dFunDummy
  def helperDoubleInt2Double(a1: dFun, a2: iFun): dFun = dFunDummy
  def helperDoubleLong2Double(a1: dFun, a2: lFun): dFun = dFunDummy
  def helperDoubleDouble2Double(a1: dFun, a2: dFun): dFun = dFunDummy
  def helperDoubleString2Double(a1: dFun, a2: sFun): dFun = dFunDummy
  def helperDoubleBoolean2Double(a1: dFun, a2: bFun): dFun = dFunDummy
  def helperStringInt2Double(a1: sFun, a2: iFun): dFun = dFunDummy
  def helperStringLong2Double(a1: sFun, a2: lFun): dFun = dFunDummy
  def helperStringDouble2Double(a1: sFun, a2: dFun): dFun = dFunDummy
  def helperStringString2Double(a1: sFun, a2: sFun): dFun = dFunDummy
  def helperStringBoolean2Double(a1: sFun, a2: bFun): dFun = dFunDummy
  def helperBooleanInt2Double(a1: bFun, a2: iFun): dFun = dFunDummy
  def helperBooleanLong2Double(a1: bFun, a2: lFun): dFun = dFunDummy
  def helperBooleanDouble2Double(a1: bFun, a2: dFun): dFun = dFunDummy
  def helperBooleanString2Double(a1: bFun, a2: sFun): dFun = dFunDummy
  def helperBooleanBoolean2Double(a1: bFun, a2: bFun): dFun = dFunDummy
  test("getSignature_dFun_3") {
    var result = true
    result &= FunctionSignature.getSignatureIntInt2Double(helperIntInt2Double) == "Int:Int2Double"
    result &= FunctionSignature.getSignatureIntLong2Double(helperIntLong2Double) == "Int:Long2Double"
    result &= FunctionSignature.getSignatureIntDouble2Double(helperIntDouble2Double) == "Int:Double2Double"
    result &= FunctionSignature.getSignatureIntString2Double(helperIntString2Double) == "Int:String2Double"
    result &= FunctionSignature.getSignatureIntBoolean2Double(helperIntBoolean2Double) == "Int:Boolean2Double"
    result &= FunctionSignature.getSignatureLongInt2Double(helperLongInt2Double) == "Long:Int2Double"
    result &= FunctionSignature.getSignatureLongLong2Double(helperLongLong2Double) == "Long:Long2Double"
    result &= FunctionSignature.getSignatureLongDouble2Double(helperLongDouble2Double) == "Long:Double2Double"
    result &= FunctionSignature.getSignatureLongString2Double(helperLongString2Double) == "Long:String2Double"
    result &= FunctionSignature.getSignatureLongBoolean2Double(helperLongBoolean2Double) == "Long:Boolean2Double"
    result &= FunctionSignature.getSignatureDoubleInt2Double(helperDoubleInt2Double) == "Double:Int2Double"
    result &= FunctionSignature.getSignatureDoubleLong2Double(helperDoubleLong2Double) == "Double:Long2Double"
    result &= FunctionSignature.getSignatureDoubleDouble2Double(helperDoubleDouble2Double) == "Double:Double2Double"
    result &= FunctionSignature.getSignatureDoubleString2Double(helperDoubleString2Double) == "Double:String2Double"
    result &= FunctionSignature.getSignatureDoubleBoolean2Double(helperDoubleBoolean2Double) == "Double:Boolean2Double"
    result &= FunctionSignature.getSignatureStringInt2Double(helperStringInt2Double) == "String:Int2Double"
    result &= FunctionSignature.getSignatureStringLong2Double(helperStringLong2Double) == "String:Long2Double"
    result &= FunctionSignature.getSignatureStringDouble2Double(helperStringDouble2Double) == "String:Double2Double"
    result &= FunctionSignature.getSignatureStringString2Double(helperStringString2Double) == "String:String2Double"
    result &= FunctionSignature.getSignatureStringBoolean2Double(helperStringBoolean2Double) == "String:Boolean2Double"
    result &= FunctionSignature.getSignatureBooleanInt2Double(helperBooleanInt2Double) == "Boolean:Int2Double"
    result &= FunctionSignature.getSignatureBooleanLong2Double(helperBooleanLong2Double) == "Boolean:Long2Double"
    result &= FunctionSignature.getSignatureBooleanDouble2Double(helperBooleanDouble2Double) == "Boolean:Double2Double"
    result &= FunctionSignature.getSignatureBooleanString2Double(helperBooleanString2Double) == "Boolean:String2Double"
    result &= FunctionSignature.getSignatureBooleanBoolean2Double(helperBooleanBoolean2Double) == "Boolean:Boolean2Double"
    assert(result)
  }
  def helperIntInt2String(a1: iFun, a2: iFun): sFun = sFunDummy
  def helperIntLong2String(a1: iFun, a2: lFun): sFun = sFunDummy
  def helperIntDouble2String(a1: iFun, a2: dFun): sFun = sFunDummy
  def helperIntString2String(a1: iFun, a2: sFun): sFun = sFunDummy
  def helperIntBoolean2String(a1: iFun, a2: bFun): sFun = sFunDummy
  def helperLongInt2String(a1: lFun, a2: iFun): sFun = sFunDummy
  def helperLongLong2String(a1: lFun, a2: lFun): sFun = sFunDummy
  def helperLongDouble2String(a1: lFun, a2: dFun): sFun = sFunDummy
  def helperLongString2String(a1: lFun, a2: sFun): sFun = sFunDummy
  def helperLongBoolean2String(a1: lFun, a2: bFun): sFun = sFunDummy
  def helperDoubleInt2String(a1: dFun, a2: iFun): sFun = sFunDummy
  def helperDoubleLong2String(a1: dFun, a2: lFun): sFun = sFunDummy
  def helperDoubleDouble2String(a1: dFun, a2: dFun): sFun = sFunDummy
  def helperDoubleString2String(a1: dFun, a2: sFun): sFun = sFunDummy
  def helperDoubleBoolean2String(a1: dFun, a2: bFun): sFun = sFunDummy
  def helperStringInt2String(a1: sFun, a2: iFun): sFun = sFunDummy
  def helperStringLong2String(a1: sFun, a2: lFun): sFun = sFunDummy
  def helperStringDouble2String(a1: sFun, a2: dFun): sFun = sFunDummy
  def helperStringString2String(a1: sFun, a2: sFun): sFun = sFunDummy
  def helperStringBoolean2String(a1: sFun, a2: bFun): sFun = sFunDummy
  def helperBooleanInt2String(a1: bFun, a2: iFun): sFun = sFunDummy
  def helperBooleanLong2String(a1: bFun, a2: lFun): sFun = sFunDummy
  def helperBooleanDouble2String(a1: bFun, a2: dFun): sFun = sFunDummy
  def helperBooleanString2String(a1: bFun, a2: sFun): sFun = sFunDummy
  def helperBooleanBoolean2String(a1: bFun, a2: bFun): sFun = sFunDummy
  test("getSignature_sFun_3") {
    var result = true
    result &= FunctionSignature.getSignatureIntInt2String(helperIntInt2String) == "Int:Int2String"
    result &= FunctionSignature.getSignatureIntLong2String(helperIntLong2String) == "Int:Long2String"
    result &= FunctionSignature.getSignatureIntDouble2String(helperIntDouble2String) == "Int:Double2String"
    result &= FunctionSignature.getSignatureIntString2String(helperIntString2String) == "Int:String2String"
    result &= FunctionSignature.getSignatureIntBoolean2String(helperIntBoolean2String) == "Int:Boolean2String"
    result &= FunctionSignature.getSignatureLongInt2String(helperLongInt2String) == "Long:Int2String"
    result &= FunctionSignature.getSignatureLongLong2String(helperLongLong2String) == "Long:Long2String"
    result &= FunctionSignature.getSignatureLongDouble2String(helperLongDouble2String) == "Long:Double2String"
    result &= FunctionSignature.getSignatureLongString2String(helperLongString2String) == "Long:String2String"
    result &= FunctionSignature.getSignatureLongBoolean2String(helperLongBoolean2String) == "Long:Boolean2String"
    result &= FunctionSignature.getSignatureDoubleInt2String(helperDoubleInt2String) == "Double:Int2String"
    result &= FunctionSignature.getSignatureDoubleLong2String(helperDoubleLong2String) == "Double:Long2String"
    result &= FunctionSignature.getSignatureDoubleDouble2String(helperDoubleDouble2String) == "Double:Double2String"
    result &= FunctionSignature.getSignatureDoubleString2String(helperDoubleString2String) == "Double:String2String"
    result &= FunctionSignature.getSignatureDoubleBoolean2String(helperDoubleBoolean2String) == "Double:Boolean2String"
    result &= FunctionSignature.getSignatureStringInt2String(helperStringInt2String) == "String:Int2String"
    result &= FunctionSignature.getSignatureStringLong2String(helperStringLong2String) == "String:Long2String"
    result &= FunctionSignature.getSignatureStringDouble2String(helperStringDouble2String) == "String:Double2String"
    result &= FunctionSignature.getSignatureStringString2String(helperStringString2String) == "String:String2String"
    result &= FunctionSignature.getSignatureStringBoolean2String(helperStringBoolean2String) == "String:Boolean2String"
    result &= FunctionSignature.getSignatureBooleanInt2String(helperBooleanInt2String) == "Boolean:Int2String"
    result &= FunctionSignature.getSignatureBooleanLong2String(helperBooleanLong2String) == "Boolean:Long2String"
    result &= FunctionSignature.getSignatureBooleanDouble2String(helperBooleanDouble2String) == "Boolean:Double2String"
    result &= FunctionSignature.getSignatureBooleanString2String(helperBooleanString2String) == "Boolean:String2String"
    result &= FunctionSignature.getSignatureBooleanBoolean2String(helperBooleanBoolean2String) == "Boolean:Boolean2String"
    assert(result)
  }
  def helperIntInt2Boolean(a1: iFun, a2: iFun): bFun = bFunDummy
  def helperIntLong2Boolean(a1: iFun, a2: lFun): bFun = bFunDummy
  def helperIntDouble2Boolean(a1: iFun, a2: dFun): bFun = bFunDummy
  def helperIntString2Boolean(a1: iFun, a2: sFun): bFun = bFunDummy
  def helperIntBoolean2Boolean(a1: iFun, a2: bFun): bFun = bFunDummy
  def helperLongInt2Boolean(a1: lFun, a2: iFun): bFun = bFunDummy
  def helperLongLong2Boolean(a1: lFun, a2: lFun): bFun = bFunDummy
  def helperLongDouble2Boolean(a1: lFun, a2: dFun): bFun = bFunDummy
  def helperLongString2Boolean(a1: lFun, a2: sFun): bFun = bFunDummy
  def helperLongBoolean2Boolean(a1: lFun, a2: bFun): bFun = bFunDummy
  def helperDoubleInt2Boolean(a1: dFun, a2: iFun): bFun = bFunDummy
  def helperDoubleLong2Boolean(a1: dFun, a2: lFun): bFun = bFunDummy
  def helperDoubleDouble2Boolean(a1: dFun, a2: dFun): bFun = bFunDummy
  def helperDoubleString2Boolean(a1: dFun, a2: sFun): bFun = bFunDummy
  def helperDoubleBoolean2Boolean(a1: dFun, a2: bFun): bFun = bFunDummy
  def helperStringInt2Boolean(a1: sFun, a2: iFun): bFun = bFunDummy
  def helperStringLong2Boolean(a1: sFun, a2: lFun): bFun = bFunDummy
  def helperStringDouble2Boolean(a1: sFun, a2: dFun): bFun = bFunDummy
  def helperStringString2Boolean(a1: sFun, a2: sFun): bFun = bFunDummy
  def helperStringBoolean2Boolean(a1: sFun, a2: bFun): bFun = bFunDummy
  def helperBooleanInt2Boolean(a1: bFun, a2: iFun): bFun = bFunDummy
  def helperBooleanLong2Boolean(a1: bFun, a2: lFun): bFun = bFunDummy
  def helperBooleanDouble2Boolean(a1: bFun, a2: dFun): bFun = bFunDummy
  def helperBooleanString2Boolean(a1: bFun, a2: sFun): bFun = bFunDummy
  def helperBooleanBoolean2Boolean(a1: bFun, a2: bFun): bFun = bFunDummy
  test("getSignature_bFun_3") {
    var result = true
    result &= FunctionSignature.getSignatureIntInt2Boolean(helperIntInt2Boolean) == "Int:Int2Boolean"
    result &= FunctionSignature.getSignatureIntLong2Boolean(helperIntLong2Boolean) == "Int:Long2Boolean"
    result &= FunctionSignature.getSignatureIntDouble2Boolean(helperIntDouble2Boolean) == "Int:Double2Boolean"
    result &= FunctionSignature.getSignatureIntString2Boolean(helperIntString2Boolean) == "Int:String2Boolean"
    result &= FunctionSignature.getSignatureIntBoolean2Boolean(helperIntBoolean2Boolean) == "Int:Boolean2Boolean"
    result &= FunctionSignature.getSignatureLongInt2Boolean(helperLongInt2Boolean) == "Long:Int2Boolean"
    result &= FunctionSignature.getSignatureLongLong2Boolean(helperLongLong2Boolean) == "Long:Long2Boolean"
    result &= FunctionSignature.getSignatureLongDouble2Boolean(helperLongDouble2Boolean) == "Long:Double2Boolean"
    result &= FunctionSignature.getSignatureLongString2Boolean(helperLongString2Boolean) == "Long:String2Boolean"
    result &= FunctionSignature.getSignatureLongBoolean2Boolean(helperLongBoolean2Boolean) == "Long:Boolean2Boolean"
    result &= FunctionSignature.getSignatureDoubleInt2Boolean(helperDoubleInt2Boolean) == "Double:Int2Boolean"
    result &= FunctionSignature.getSignatureDoubleLong2Boolean(helperDoubleLong2Boolean) == "Double:Long2Boolean"
    result &= FunctionSignature.getSignatureDoubleDouble2Boolean(helperDoubleDouble2Boolean) == "Double:Double2Boolean"
    result &= FunctionSignature.getSignatureDoubleString2Boolean(helperDoubleString2Boolean) == "Double:String2Boolean"
    result &= FunctionSignature.getSignatureDoubleBoolean2Boolean(helperDoubleBoolean2Boolean) == "Double:Boolean2Boolean"
    result &= FunctionSignature.getSignatureStringInt2Boolean(helperStringInt2Boolean) == "String:Int2Boolean"
    result &= FunctionSignature.getSignatureStringLong2Boolean(helperStringLong2Boolean) == "String:Long2Boolean"
    result &= FunctionSignature.getSignatureStringDouble2Boolean(helperStringDouble2Boolean) == "String:Double2Boolean"
    result &= FunctionSignature.getSignatureStringString2Boolean(helperStringString2Boolean) == "String:String2Boolean"
    result &= FunctionSignature.getSignatureStringBoolean2Boolean(helperStringBoolean2Boolean) == "String:Boolean2Boolean"
    result &= FunctionSignature.getSignatureBooleanInt2Boolean(helperBooleanInt2Boolean) == "Boolean:Int2Boolean"
    result &= FunctionSignature.getSignatureBooleanLong2Boolean(helperBooleanLong2Boolean) == "Boolean:Long2Boolean"
    result &= FunctionSignature.getSignatureBooleanDouble2Boolean(helperBooleanDouble2Boolean) == "Boolean:Double2Boolean"
    result &= FunctionSignature.getSignatureBooleanString2Boolean(helperBooleanString2Boolean) == "Boolean:String2Boolean"
    result &= FunctionSignature.getSignatureBooleanBoolean2Boolean(helperBooleanBoolean2Boolean) == "Boolean:Boolean2Boolean"
    assert(result)
  }
  def helperIntIntInt2Int(a1: iFun, a2: iFun, a3: iFun): iFun = iFunDummy
  def helperIntIntLong2Int(a1: iFun, a2: iFun, a3: lFun): iFun = iFunDummy
  def helperIntIntDouble2Int(a1: iFun, a2: iFun, a3: dFun): iFun = iFunDummy
  def helperIntIntString2Int(a1: iFun, a2: iFun, a3: sFun): iFun = iFunDummy
  def helperIntIntBoolean2Int(a1: iFun, a2: iFun, a3: bFun): iFun = iFunDummy
  def helperIntLongInt2Int(a1: iFun, a2: lFun, a3: iFun): iFun = iFunDummy
  def helperIntLongLong2Int(a1: iFun, a2: lFun, a3: lFun): iFun = iFunDummy
  def helperIntLongDouble2Int(a1: iFun, a2: lFun, a3: dFun): iFun = iFunDummy
  def helperIntLongString2Int(a1: iFun, a2: lFun, a3: sFun): iFun = iFunDummy
  def helperIntLongBoolean2Int(a1: iFun, a2: lFun, a3: bFun): iFun = iFunDummy
  def helperIntDoubleInt2Int(a1: iFun, a2: dFun, a3: iFun): iFun = iFunDummy
  def helperIntDoubleLong2Int(a1: iFun, a2: dFun, a3: lFun): iFun = iFunDummy
  def helperIntDoubleDouble2Int(a1: iFun, a2: dFun, a3: dFun): iFun = iFunDummy
  def helperIntDoubleString2Int(a1: iFun, a2: dFun, a3: sFun): iFun = iFunDummy
  def helperIntDoubleBoolean2Int(a1: iFun, a2: dFun, a3: bFun): iFun = iFunDummy
  def helperIntStringInt2Int(a1: iFun, a2: sFun, a3: iFun): iFun = iFunDummy
  def helperIntStringLong2Int(a1: iFun, a2: sFun, a3: lFun): iFun = iFunDummy
  def helperIntStringDouble2Int(a1: iFun, a2: sFun, a3: dFun): iFun = iFunDummy
  def helperIntStringString2Int(a1: iFun, a2: sFun, a3: sFun): iFun = iFunDummy
  def helperIntStringBoolean2Int(a1: iFun, a2: sFun, a3: bFun): iFun = iFunDummy
  def helperIntBooleanInt2Int(a1: iFun, a2: bFun, a3: iFun): iFun = iFunDummy
  def helperIntBooleanLong2Int(a1: iFun, a2: bFun, a3: lFun): iFun = iFunDummy
  def helperIntBooleanDouble2Int(a1: iFun, a2: bFun, a3: dFun): iFun = iFunDummy
  def helperIntBooleanString2Int(a1: iFun, a2: bFun, a3: sFun): iFun = iFunDummy
  def helperIntBooleanBoolean2Int(a1: iFun, a2: bFun, a3: bFun): iFun = iFunDummy
  def helperLongIntInt2Int(a1: lFun, a2: iFun, a3: iFun): iFun = iFunDummy
  def helperLongIntLong2Int(a1: lFun, a2: iFun, a3: lFun): iFun = iFunDummy
  def helperLongIntDouble2Int(a1: lFun, a2: iFun, a3: dFun): iFun = iFunDummy
  def helperLongIntString2Int(a1: lFun, a2: iFun, a3: sFun): iFun = iFunDummy
  def helperLongIntBoolean2Int(a1: lFun, a2: iFun, a3: bFun): iFun = iFunDummy
  def helperLongLongInt2Int(a1: lFun, a2: lFun, a3: iFun): iFun = iFunDummy
  def helperLongLongLong2Int(a1: lFun, a2: lFun, a3: lFun): iFun = iFunDummy
  def helperLongLongDouble2Int(a1: lFun, a2: lFun, a3: dFun): iFun = iFunDummy
  def helperLongLongString2Int(a1: lFun, a2: lFun, a3: sFun): iFun = iFunDummy
  def helperLongLongBoolean2Int(a1: lFun, a2: lFun, a3: bFun): iFun = iFunDummy
  def helperLongDoubleInt2Int(a1: lFun, a2: dFun, a3: iFun): iFun = iFunDummy
  def helperLongDoubleLong2Int(a1: lFun, a2: dFun, a3: lFun): iFun = iFunDummy
  def helperLongDoubleDouble2Int(a1: lFun, a2: dFun, a3: dFun): iFun = iFunDummy
  def helperLongDoubleString2Int(a1: lFun, a2: dFun, a3: sFun): iFun = iFunDummy
  def helperLongDoubleBoolean2Int(a1: lFun, a2: dFun, a3: bFun): iFun = iFunDummy
  def helperLongStringInt2Int(a1: lFun, a2: sFun, a3: iFun): iFun = iFunDummy
  def helperLongStringLong2Int(a1: lFun, a2: sFun, a3: lFun): iFun = iFunDummy
  def helperLongStringDouble2Int(a1: lFun, a2: sFun, a3: dFun): iFun = iFunDummy
  def helperLongStringString2Int(a1: lFun, a2: sFun, a3: sFun): iFun = iFunDummy
  def helperLongStringBoolean2Int(a1: lFun, a2: sFun, a3: bFun): iFun = iFunDummy
  def helperLongBooleanInt2Int(a1: lFun, a2: bFun, a3: iFun): iFun = iFunDummy
  def helperLongBooleanLong2Int(a1: lFun, a2: bFun, a3: lFun): iFun = iFunDummy
  def helperLongBooleanDouble2Int(a1: lFun, a2: bFun, a3: dFun): iFun = iFunDummy
  def helperLongBooleanString2Int(a1: lFun, a2: bFun, a3: sFun): iFun = iFunDummy
  def helperLongBooleanBoolean2Int(a1: lFun, a2: bFun, a3: bFun): iFun = iFunDummy
  def helperDoubleIntInt2Int(a1: dFun, a2: iFun, a3: iFun): iFun = iFunDummy
  def helperDoubleIntLong2Int(a1: dFun, a2: iFun, a3: lFun): iFun = iFunDummy
  def helperDoubleIntDouble2Int(a1: dFun, a2: iFun, a3: dFun): iFun = iFunDummy
  def helperDoubleIntString2Int(a1: dFun, a2: iFun, a3: sFun): iFun = iFunDummy
  def helperDoubleIntBoolean2Int(a1: dFun, a2: iFun, a3: bFun): iFun = iFunDummy
  def helperDoubleLongInt2Int(a1: dFun, a2: lFun, a3: iFun): iFun = iFunDummy
  def helperDoubleLongLong2Int(a1: dFun, a2: lFun, a3: lFun): iFun = iFunDummy
  def helperDoubleLongDouble2Int(a1: dFun, a2: lFun, a3: dFun): iFun = iFunDummy
  def helperDoubleLongString2Int(a1: dFun, a2: lFun, a3: sFun): iFun = iFunDummy
  def helperDoubleLongBoolean2Int(a1: dFun, a2: lFun, a3: bFun): iFun = iFunDummy
  def helperDoubleDoubleInt2Int(a1: dFun, a2: dFun, a3: iFun): iFun = iFunDummy
  def helperDoubleDoubleLong2Int(a1: dFun, a2: dFun, a3: lFun): iFun = iFunDummy
  def helperDoubleDoubleDouble2Int(a1: dFun, a2: dFun, a3: dFun): iFun = iFunDummy
  def helperDoubleDoubleString2Int(a1: dFun, a2: dFun, a3: sFun): iFun = iFunDummy
  def helperDoubleDoubleBoolean2Int(a1: dFun, a2: dFun, a3: bFun): iFun = iFunDummy
  def helperDoubleStringInt2Int(a1: dFun, a2: sFun, a3: iFun): iFun = iFunDummy
  def helperDoubleStringLong2Int(a1: dFun, a2: sFun, a3: lFun): iFun = iFunDummy
  def helperDoubleStringDouble2Int(a1: dFun, a2: sFun, a3: dFun): iFun = iFunDummy
  def helperDoubleStringString2Int(a1: dFun, a2: sFun, a3: sFun): iFun = iFunDummy
  def helperDoubleStringBoolean2Int(a1: dFun, a2: sFun, a3: bFun): iFun = iFunDummy
  def helperDoubleBooleanInt2Int(a1: dFun, a2: bFun, a3: iFun): iFun = iFunDummy
  def helperDoubleBooleanLong2Int(a1: dFun, a2: bFun, a3: lFun): iFun = iFunDummy
  def helperDoubleBooleanDouble2Int(a1: dFun, a2: bFun, a3: dFun): iFun = iFunDummy
  def helperDoubleBooleanString2Int(a1: dFun, a2: bFun, a3: sFun): iFun = iFunDummy
  def helperDoubleBooleanBoolean2Int(a1: dFun, a2: bFun, a3: bFun): iFun = iFunDummy
  def helperStringIntInt2Int(a1: sFun, a2: iFun, a3: iFun): iFun = iFunDummy
  def helperStringIntLong2Int(a1: sFun, a2: iFun, a3: lFun): iFun = iFunDummy
  def helperStringIntDouble2Int(a1: sFun, a2: iFun, a3: dFun): iFun = iFunDummy
  def helperStringIntString2Int(a1: sFun, a2: iFun, a3: sFun): iFun = iFunDummy
  def helperStringIntBoolean2Int(a1: sFun, a2: iFun, a3: bFun): iFun = iFunDummy
  def helperStringLongInt2Int(a1: sFun, a2: lFun, a3: iFun): iFun = iFunDummy
  def helperStringLongLong2Int(a1: sFun, a2: lFun, a3: lFun): iFun = iFunDummy
  def helperStringLongDouble2Int(a1: sFun, a2: lFun, a3: dFun): iFun = iFunDummy
  def helperStringLongString2Int(a1: sFun, a2: lFun, a3: sFun): iFun = iFunDummy
  def helperStringLongBoolean2Int(a1: sFun, a2: lFun, a3: bFun): iFun = iFunDummy
  def helperStringDoubleInt2Int(a1: sFun, a2: dFun, a3: iFun): iFun = iFunDummy
  def helperStringDoubleLong2Int(a1: sFun, a2: dFun, a3: lFun): iFun = iFunDummy
  def helperStringDoubleDouble2Int(a1: sFun, a2: dFun, a3: dFun): iFun = iFunDummy
  def helperStringDoubleString2Int(a1: sFun, a2: dFun, a3: sFun): iFun = iFunDummy
  def helperStringDoubleBoolean2Int(a1: sFun, a2: dFun, a3: bFun): iFun = iFunDummy
  def helperStringStringInt2Int(a1: sFun, a2: sFun, a3: iFun): iFun = iFunDummy
  def helperStringStringLong2Int(a1: sFun, a2: sFun, a3: lFun): iFun = iFunDummy
  def helperStringStringDouble2Int(a1: sFun, a2: sFun, a3: dFun): iFun = iFunDummy
  def helperStringStringString2Int(a1: sFun, a2: sFun, a3: sFun): iFun = iFunDummy
  def helperStringStringBoolean2Int(a1: sFun, a2: sFun, a3: bFun): iFun = iFunDummy
  def helperStringBooleanInt2Int(a1: sFun, a2: bFun, a3: iFun): iFun = iFunDummy
  def helperStringBooleanLong2Int(a1: sFun, a2: bFun, a3: lFun): iFun = iFunDummy
  def helperStringBooleanDouble2Int(a1: sFun, a2: bFun, a3: dFun): iFun = iFunDummy
  def helperStringBooleanString2Int(a1: sFun, a2: bFun, a3: sFun): iFun = iFunDummy
  def helperStringBooleanBoolean2Int(a1: sFun, a2: bFun, a3: bFun): iFun = iFunDummy
  def helperBooleanIntInt2Int(a1: bFun, a2: iFun, a3: iFun): iFun = iFunDummy
  def helperBooleanIntLong2Int(a1: bFun, a2: iFun, a3: lFun): iFun = iFunDummy
  def helperBooleanIntDouble2Int(a1: bFun, a2: iFun, a3: dFun): iFun = iFunDummy
  def helperBooleanIntString2Int(a1: bFun, a2: iFun, a3: sFun): iFun = iFunDummy
  def helperBooleanIntBoolean2Int(a1: bFun, a2: iFun, a3: bFun): iFun = iFunDummy
  def helperBooleanLongInt2Int(a1: bFun, a2: lFun, a3: iFun): iFun = iFunDummy
  def helperBooleanLongLong2Int(a1: bFun, a2: lFun, a3: lFun): iFun = iFunDummy
  def helperBooleanLongDouble2Int(a1: bFun, a2: lFun, a3: dFun): iFun = iFunDummy
  def helperBooleanLongString2Int(a1: bFun, a2: lFun, a3: sFun): iFun = iFunDummy
  def helperBooleanLongBoolean2Int(a1: bFun, a2: lFun, a3: bFun): iFun = iFunDummy
  def helperBooleanDoubleInt2Int(a1: bFun, a2: dFun, a3: iFun): iFun = iFunDummy
  def helperBooleanDoubleLong2Int(a1: bFun, a2: dFun, a3: lFun): iFun = iFunDummy
  def helperBooleanDoubleDouble2Int(a1: bFun, a2: dFun, a3: dFun): iFun = iFunDummy
  def helperBooleanDoubleString2Int(a1: bFun, a2: dFun, a3: sFun): iFun = iFunDummy
  def helperBooleanDoubleBoolean2Int(a1: bFun, a2: dFun, a3: bFun): iFun = iFunDummy
  def helperBooleanStringInt2Int(a1: bFun, a2: sFun, a3: iFun): iFun = iFunDummy
  def helperBooleanStringLong2Int(a1: bFun, a2: sFun, a3: lFun): iFun = iFunDummy
  def helperBooleanStringDouble2Int(a1: bFun, a2: sFun, a3: dFun): iFun = iFunDummy
  def helperBooleanStringString2Int(a1: bFun, a2: sFun, a3: sFun): iFun = iFunDummy
  def helperBooleanStringBoolean2Int(a1: bFun, a2: sFun, a3: bFun): iFun = iFunDummy
  def helperBooleanBooleanInt2Int(a1: bFun, a2: bFun, a3: iFun): iFun = iFunDummy
  def helperBooleanBooleanLong2Int(a1: bFun, a2: bFun, a3: lFun): iFun = iFunDummy
  def helperBooleanBooleanDouble2Int(a1: bFun, a2: bFun, a3: dFun): iFun = iFunDummy
  def helperBooleanBooleanString2Int(a1: bFun, a2: bFun, a3: sFun): iFun = iFunDummy
  def helperBooleanBooleanBoolean2Int(a1: bFun, a2: bFun, a3: bFun): iFun = iFunDummy
  test("getSignature_iFun_4") {
    var result = true
    result &= FunctionSignature.getSignatureIntIntInt2Int(helperIntIntInt2Int) == "Int:Int:Int2Int"
    result &= FunctionSignature.getSignatureIntIntLong2Int(helperIntIntLong2Int) == "Int:Int:Long2Int"
    result &= FunctionSignature.getSignatureIntIntDouble2Int(helperIntIntDouble2Int) == "Int:Int:Double2Int"
    result &= FunctionSignature.getSignatureIntIntString2Int(helperIntIntString2Int) == "Int:Int:String2Int"
    result &= FunctionSignature.getSignatureIntIntBoolean2Int(helperIntIntBoolean2Int) == "Int:Int:Boolean2Int"
    result &= FunctionSignature.getSignatureIntLongInt2Int(helperIntLongInt2Int) == "Int:Long:Int2Int"
    result &= FunctionSignature.getSignatureIntLongLong2Int(helperIntLongLong2Int) == "Int:Long:Long2Int"
    result &= FunctionSignature.getSignatureIntLongDouble2Int(helperIntLongDouble2Int) == "Int:Long:Double2Int"
    result &= FunctionSignature.getSignatureIntLongString2Int(helperIntLongString2Int) == "Int:Long:String2Int"
    result &= FunctionSignature.getSignatureIntLongBoolean2Int(helperIntLongBoolean2Int) == "Int:Long:Boolean2Int"
    result &= FunctionSignature.getSignatureIntDoubleInt2Int(helperIntDoubleInt2Int) == "Int:Double:Int2Int"
    result &= FunctionSignature.getSignatureIntDoubleLong2Int(helperIntDoubleLong2Int) == "Int:Double:Long2Int"
    result &= FunctionSignature.getSignatureIntDoubleDouble2Int(helperIntDoubleDouble2Int) == "Int:Double:Double2Int"
    result &= FunctionSignature.getSignatureIntDoubleString2Int(helperIntDoubleString2Int) == "Int:Double:String2Int"
    result &= FunctionSignature.getSignatureIntDoubleBoolean2Int(helperIntDoubleBoolean2Int) == "Int:Double:Boolean2Int"
    result &= FunctionSignature.getSignatureIntStringInt2Int(helperIntStringInt2Int) == "Int:String:Int2Int"
    result &= FunctionSignature.getSignatureIntStringLong2Int(helperIntStringLong2Int) == "Int:String:Long2Int"
    result &= FunctionSignature.getSignatureIntStringDouble2Int(helperIntStringDouble2Int) == "Int:String:Double2Int"
    result &= FunctionSignature.getSignatureIntStringString2Int(helperIntStringString2Int) == "Int:String:String2Int"
    result &= FunctionSignature.getSignatureIntStringBoolean2Int(helperIntStringBoolean2Int) == "Int:String:Boolean2Int"
    result &= FunctionSignature.getSignatureIntBooleanInt2Int(helperIntBooleanInt2Int) == "Int:Boolean:Int2Int"
    result &= FunctionSignature.getSignatureIntBooleanLong2Int(helperIntBooleanLong2Int) == "Int:Boolean:Long2Int"
    result &= FunctionSignature.getSignatureIntBooleanDouble2Int(helperIntBooleanDouble2Int) == "Int:Boolean:Double2Int"
    result &= FunctionSignature.getSignatureIntBooleanString2Int(helperIntBooleanString2Int) == "Int:Boolean:String2Int"
    result &= FunctionSignature.getSignatureIntBooleanBoolean2Int(helperIntBooleanBoolean2Int) == "Int:Boolean:Boolean2Int"
    result &= FunctionSignature.getSignatureLongIntInt2Int(helperLongIntInt2Int) == "Long:Int:Int2Int"
    result &= FunctionSignature.getSignatureLongIntLong2Int(helperLongIntLong2Int) == "Long:Int:Long2Int"
    result &= FunctionSignature.getSignatureLongIntDouble2Int(helperLongIntDouble2Int) == "Long:Int:Double2Int"
    result &= FunctionSignature.getSignatureLongIntString2Int(helperLongIntString2Int) == "Long:Int:String2Int"
    result &= FunctionSignature.getSignatureLongIntBoolean2Int(helperLongIntBoolean2Int) == "Long:Int:Boolean2Int"
    result &= FunctionSignature.getSignatureLongLongInt2Int(helperLongLongInt2Int) == "Long:Long:Int2Int"
    result &= FunctionSignature.getSignatureLongLongLong2Int(helperLongLongLong2Int) == "Long:Long:Long2Int"
    result &= FunctionSignature.getSignatureLongLongDouble2Int(helperLongLongDouble2Int) == "Long:Long:Double2Int"
    result &= FunctionSignature.getSignatureLongLongString2Int(helperLongLongString2Int) == "Long:Long:String2Int"
    result &= FunctionSignature.getSignatureLongLongBoolean2Int(helperLongLongBoolean2Int) == "Long:Long:Boolean2Int"
    result &= FunctionSignature.getSignatureLongDoubleInt2Int(helperLongDoubleInt2Int) == "Long:Double:Int2Int"
    result &= FunctionSignature.getSignatureLongDoubleLong2Int(helperLongDoubleLong2Int) == "Long:Double:Long2Int"
    result &= FunctionSignature.getSignatureLongDoubleDouble2Int(helperLongDoubleDouble2Int) == "Long:Double:Double2Int"
    result &= FunctionSignature.getSignatureLongDoubleString2Int(helperLongDoubleString2Int) == "Long:Double:String2Int"
    result &= FunctionSignature.getSignatureLongDoubleBoolean2Int(helperLongDoubleBoolean2Int) == "Long:Double:Boolean2Int"
    result &= FunctionSignature.getSignatureLongStringInt2Int(helperLongStringInt2Int) == "Long:String:Int2Int"
    result &= FunctionSignature.getSignatureLongStringLong2Int(helperLongStringLong2Int) == "Long:String:Long2Int"
    result &= FunctionSignature.getSignatureLongStringDouble2Int(helperLongStringDouble2Int) == "Long:String:Double2Int"
    result &= FunctionSignature.getSignatureLongStringString2Int(helperLongStringString2Int) == "Long:String:String2Int"
    result &= FunctionSignature.getSignatureLongStringBoolean2Int(helperLongStringBoolean2Int) == "Long:String:Boolean2Int"
    result &= FunctionSignature.getSignatureLongBooleanInt2Int(helperLongBooleanInt2Int) == "Long:Boolean:Int2Int"
    result &= FunctionSignature.getSignatureLongBooleanLong2Int(helperLongBooleanLong2Int) == "Long:Boolean:Long2Int"
    result &= FunctionSignature.getSignatureLongBooleanDouble2Int(helperLongBooleanDouble2Int) == "Long:Boolean:Double2Int"
    result &= FunctionSignature.getSignatureLongBooleanString2Int(helperLongBooleanString2Int) == "Long:Boolean:String2Int"
    result &= FunctionSignature.getSignatureLongBooleanBoolean2Int(helperLongBooleanBoolean2Int) == "Long:Boolean:Boolean2Int"
    result &= FunctionSignature.getSignatureDoubleIntInt2Int(helperDoubleIntInt2Int) == "Double:Int:Int2Int"
    result &= FunctionSignature.getSignatureDoubleIntLong2Int(helperDoubleIntLong2Int) == "Double:Int:Long2Int"
    result &= FunctionSignature.getSignatureDoubleIntDouble2Int(helperDoubleIntDouble2Int) == "Double:Int:Double2Int"
    result &= FunctionSignature.getSignatureDoubleIntString2Int(helperDoubleIntString2Int) == "Double:Int:String2Int"
    result &= FunctionSignature.getSignatureDoubleIntBoolean2Int(helperDoubleIntBoolean2Int) == "Double:Int:Boolean2Int"
    result &= FunctionSignature.getSignatureDoubleLongInt2Int(helperDoubleLongInt2Int) == "Double:Long:Int2Int"
    result &= FunctionSignature.getSignatureDoubleLongLong2Int(helperDoubleLongLong2Int) == "Double:Long:Long2Int"
    result &= FunctionSignature.getSignatureDoubleLongDouble2Int(helperDoubleLongDouble2Int) == "Double:Long:Double2Int"
    result &= FunctionSignature.getSignatureDoubleLongString2Int(helperDoubleLongString2Int) == "Double:Long:String2Int"
    result &= FunctionSignature.getSignatureDoubleLongBoolean2Int(helperDoubleLongBoolean2Int) == "Double:Long:Boolean2Int"
    result &= FunctionSignature.getSignatureDoubleDoubleInt2Int(helperDoubleDoubleInt2Int) == "Double:Double:Int2Int"
    result &= FunctionSignature.getSignatureDoubleDoubleLong2Int(helperDoubleDoubleLong2Int) == "Double:Double:Long2Int"
    result &= FunctionSignature.getSignatureDoubleDoubleDouble2Int(helperDoubleDoubleDouble2Int) == "Double:Double:Double2Int"
    result &= FunctionSignature.getSignatureDoubleDoubleString2Int(helperDoubleDoubleString2Int) == "Double:Double:String2Int"
    result &= FunctionSignature.getSignatureDoubleDoubleBoolean2Int(helperDoubleDoubleBoolean2Int) == "Double:Double:Boolean2Int"
    result &= FunctionSignature.getSignatureDoubleStringInt2Int(helperDoubleStringInt2Int) == "Double:String:Int2Int"
    result &= FunctionSignature.getSignatureDoubleStringLong2Int(helperDoubleStringLong2Int) == "Double:String:Long2Int"
    result &= FunctionSignature.getSignatureDoubleStringDouble2Int(helperDoubleStringDouble2Int) == "Double:String:Double2Int"
    result &= FunctionSignature.getSignatureDoubleStringString2Int(helperDoubleStringString2Int) == "Double:String:String2Int"
    result &= FunctionSignature.getSignatureDoubleStringBoolean2Int(helperDoubleStringBoolean2Int) == "Double:String:Boolean2Int"
    result &= FunctionSignature.getSignatureDoubleBooleanInt2Int(helperDoubleBooleanInt2Int) == "Double:Boolean:Int2Int"
    result &= FunctionSignature.getSignatureDoubleBooleanLong2Int(helperDoubleBooleanLong2Int) == "Double:Boolean:Long2Int"
    result &= FunctionSignature.getSignatureDoubleBooleanDouble2Int(helperDoubleBooleanDouble2Int) == "Double:Boolean:Double2Int"
    result &= FunctionSignature.getSignatureDoubleBooleanString2Int(helperDoubleBooleanString2Int) == "Double:Boolean:String2Int"
    result &= FunctionSignature.getSignatureDoubleBooleanBoolean2Int(helperDoubleBooleanBoolean2Int) == "Double:Boolean:Boolean2Int"
    result &= FunctionSignature.getSignatureStringIntInt2Int(helperStringIntInt2Int) == "String:Int:Int2Int"
    result &= FunctionSignature.getSignatureStringIntLong2Int(helperStringIntLong2Int) == "String:Int:Long2Int"
    result &= FunctionSignature.getSignatureStringIntDouble2Int(helperStringIntDouble2Int) == "String:Int:Double2Int"
    result &= FunctionSignature.getSignatureStringIntString2Int(helperStringIntString2Int) == "String:Int:String2Int"
    result &= FunctionSignature.getSignatureStringIntBoolean2Int(helperStringIntBoolean2Int) == "String:Int:Boolean2Int"
    result &= FunctionSignature.getSignatureStringLongInt2Int(helperStringLongInt2Int) == "String:Long:Int2Int"
    result &= FunctionSignature.getSignatureStringLongLong2Int(helperStringLongLong2Int) == "String:Long:Long2Int"
    result &= FunctionSignature.getSignatureStringLongDouble2Int(helperStringLongDouble2Int) == "String:Long:Double2Int"
    result &= FunctionSignature.getSignatureStringLongString2Int(helperStringLongString2Int) == "String:Long:String2Int"
    result &= FunctionSignature.getSignatureStringLongBoolean2Int(helperStringLongBoolean2Int) == "String:Long:Boolean2Int"
    result &= FunctionSignature.getSignatureStringDoubleInt2Int(helperStringDoubleInt2Int) == "String:Double:Int2Int"
    result &= FunctionSignature.getSignatureStringDoubleLong2Int(helperStringDoubleLong2Int) == "String:Double:Long2Int"
    result &= FunctionSignature.getSignatureStringDoubleDouble2Int(helperStringDoubleDouble2Int) == "String:Double:Double2Int"
    result &= FunctionSignature.getSignatureStringDoubleString2Int(helperStringDoubleString2Int) == "String:Double:String2Int"
    result &= FunctionSignature.getSignatureStringDoubleBoolean2Int(helperStringDoubleBoolean2Int) == "String:Double:Boolean2Int"
    result &= FunctionSignature.getSignatureStringStringInt2Int(helperStringStringInt2Int) == "String:String:Int2Int"
    result &= FunctionSignature.getSignatureStringStringLong2Int(helperStringStringLong2Int) == "String:String:Long2Int"
    result &= FunctionSignature.getSignatureStringStringDouble2Int(helperStringStringDouble2Int) == "String:String:Double2Int"
    result &= FunctionSignature.getSignatureStringStringString2Int(helperStringStringString2Int) == "String:String:String2Int"
    result &= FunctionSignature.getSignatureStringStringBoolean2Int(helperStringStringBoolean2Int) == "String:String:Boolean2Int"
    result &= FunctionSignature.getSignatureStringBooleanInt2Int(helperStringBooleanInt2Int) == "String:Boolean:Int2Int"
    result &= FunctionSignature.getSignatureStringBooleanLong2Int(helperStringBooleanLong2Int) == "String:Boolean:Long2Int"
    result &= FunctionSignature.getSignatureStringBooleanDouble2Int(helperStringBooleanDouble2Int) == "String:Boolean:Double2Int"
    result &= FunctionSignature.getSignatureStringBooleanString2Int(helperStringBooleanString2Int) == "String:Boolean:String2Int"
    result &= FunctionSignature.getSignatureStringBooleanBoolean2Int(helperStringBooleanBoolean2Int) == "String:Boolean:Boolean2Int"
    result &= FunctionSignature.getSignatureBooleanIntInt2Int(helperBooleanIntInt2Int) == "Boolean:Int:Int2Int"
    result &= FunctionSignature.getSignatureBooleanIntLong2Int(helperBooleanIntLong2Int) == "Boolean:Int:Long2Int"
    result &= FunctionSignature.getSignatureBooleanIntDouble2Int(helperBooleanIntDouble2Int) == "Boolean:Int:Double2Int"
    result &= FunctionSignature.getSignatureBooleanIntString2Int(helperBooleanIntString2Int) == "Boolean:Int:String2Int"
    result &= FunctionSignature.getSignatureBooleanIntBoolean2Int(helperBooleanIntBoolean2Int) == "Boolean:Int:Boolean2Int"
    result &= FunctionSignature.getSignatureBooleanLongInt2Int(helperBooleanLongInt2Int) == "Boolean:Long:Int2Int"
    result &= FunctionSignature.getSignatureBooleanLongLong2Int(helperBooleanLongLong2Int) == "Boolean:Long:Long2Int"
    result &= FunctionSignature.getSignatureBooleanLongDouble2Int(helperBooleanLongDouble2Int) == "Boolean:Long:Double2Int"
    result &= FunctionSignature.getSignatureBooleanLongString2Int(helperBooleanLongString2Int) == "Boolean:Long:String2Int"
    result &= FunctionSignature.getSignatureBooleanLongBoolean2Int(helperBooleanLongBoolean2Int) == "Boolean:Long:Boolean2Int"
    result &= FunctionSignature.getSignatureBooleanDoubleInt2Int(helperBooleanDoubleInt2Int) == "Boolean:Double:Int2Int"
    result &= FunctionSignature.getSignatureBooleanDoubleLong2Int(helperBooleanDoubleLong2Int) == "Boolean:Double:Long2Int"
    result &= FunctionSignature.getSignatureBooleanDoubleDouble2Int(helperBooleanDoubleDouble2Int) == "Boolean:Double:Double2Int"
    result &= FunctionSignature.getSignatureBooleanDoubleString2Int(helperBooleanDoubleString2Int) == "Boolean:Double:String2Int"
    result &= FunctionSignature.getSignatureBooleanDoubleBoolean2Int(helperBooleanDoubleBoolean2Int) == "Boolean:Double:Boolean2Int"
    result &= FunctionSignature.getSignatureBooleanStringInt2Int(helperBooleanStringInt2Int) == "Boolean:String:Int2Int"
    result &= FunctionSignature.getSignatureBooleanStringLong2Int(helperBooleanStringLong2Int) == "Boolean:String:Long2Int"
    result &= FunctionSignature.getSignatureBooleanStringDouble2Int(helperBooleanStringDouble2Int) == "Boolean:String:Double2Int"
    result &= FunctionSignature.getSignatureBooleanStringString2Int(helperBooleanStringString2Int) == "Boolean:String:String2Int"
    result &= FunctionSignature.getSignatureBooleanStringBoolean2Int(helperBooleanStringBoolean2Int) == "Boolean:String:Boolean2Int"
    result &= FunctionSignature.getSignatureBooleanBooleanInt2Int(helperBooleanBooleanInt2Int) == "Boolean:Boolean:Int2Int"
    result &= FunctionSignature.getSignatureBooleanBooleanLong2Int(helperBooleanBooleanLong2Int) == "Boolean:Boolean:Long2Int"
    result &= FunctionSignature.getSignatureBooleanBooleanDouble2Int(helperBooleanBooleanDouble2Int) == "Boolean:Boolean:Double2Int"
    result &= FunctionSignature.getSignatureBooleanBooleanString2Int(helperBooleanBooleanString2Int) == "Boolean:Boolean:String2Int"
    result &= FunctionSignature.getSignatureBooleanBooleanBoolean2Int(helperBooleanBooleanBoolean2Int) == "Boolean:Boolean:Boolean2Int"
    assert(result)
  }
  def helperIntIntInt2Long(a1: iFun, a2: iFun, a3: iFun): lFun = lFunDummy
  def helperIntIntLong2Long(a1: iFun, a2: iFun, a3: lFun): lFun = lFunDummy
  def helperIntIntDouble2Long(a1: iFun, a2: iFun, a3: dFun): lFun = lFunDummy
  def helperIntIntString2Long(a1: iFun, a2: iFun, a3: sFun): lFun = lFunDummy
  def helperIntIntBoolean2Long(a1: iFun, a2: iFun, a3: bFun): lFun = lFunDummy
  def helperIntLongInt2Long(a1: iFun, a2: lFun, a3: iFun): lFun = lFunDummy
  def helperIntLongLong2Long(a1: iFun, a2: lFun, a3: lFun): lFun = lFunDummy
  def helperIntLongDouble2Long(a1: iFun, a2: lFun, a3: dFun): lFun = lFunDummy
  def helperIntLongString2Long(a1: iFun, a2: lFun, a3: sFun): lFun = lFunDummy
  def helperIntLongBoolean2Long(a1: iFun, a2: lFun, a3: bFun): lFun = lFunDummy
  def helperIntDoubleInt2Long(a1: iFun, a2: dFun, a3: iFun): lFun = lFunDummy
  def helperIntDoubleLong2Long(a1: iFun, a2: dFun, a3: lFun): lFun = lFunDummy
  def helperIntDoubleDouble2Long(a1: iFun, a2: dFun, a3: dFun): lFun = lFunDummy
  def helperIntDoubleString2Long(a1: iFun, a2: dFun, a3: sFun): lFun = lFunDummy
  def helperIntDoubleBoolean2Long(a1: iFun, a2: dFun, a3: bFun): lFun = lFunDummy
  def helperIntStringInt2Long(a1: iFun, a2: sFun, a3: iFun): lFun = lFunDummy
  def helperIntStringLong2Long(a1: iFun, a2: sFun, a3: lFun): lFun = lFunDummy
  def helperIntStringDouble2Long(a1: iFun, a2: sFun, a3: dFun): lFun = lFunDummy
  def helperIntStringString2Long(a1: iFun, a2: sFun, a3: sFun): lFun = lFunDummy
  def helperIntStringBoolean2Long(a1: iFun, a2: sFun, a3: bFun): lFun = lFunDummy
  def helperIntBooleanInt2Long(a1: iFun, a2: bFun, a3: iFun): lFun = lFunDummy
  def helperIntBooleanLong2Long(a1: iFun, a2: bFun, a3: lFun): lFun = lFunDummy
  def helperIntBooleanDouble2Long(a1: iFun, a2: bFun, a3: dFun): lFun = lFunDummy
  def helperIntBooleanString2Long(a1: iFun, a2: bFun, a3: sFun): lFun = lFunDummy
  def helperIntBooleanBoolean2Long(a1: iFun, a2: bFun, a3: bFun): lFun = lFunDummy
  def helperLongIntInt2Long(a1: lFun, a2: iFun, a3: iFun): lFun = lFunDummy
  def helperLongIntLong2Long(a1: lFun, a2: iFun, a3: lFun): lFun = lFunDummy
  def helperLongIntDouble2Long(a1: lFun, a2: iFun, a3: dFun): lFun = lFunDummy
  def helperLongIntString2Long(a1: lFun, a2: iFun, a3: sFun): lFun = lFunDummy
  def helperLongIntBoolean2Long(a1: lFun, a2: iFun, a3: bFun): lFun = lFunDummy
  def helperLongLongInt2Long(a1: lFun, a2: lFun, a3: iFun): lFun = lFunDummy
  def helperLongLongLong2Long(a1: lFun, a2: lFun, a3: lFun): lFun = lFunDummy
  def helperLongLongDouble2Long(a1: lFun, a2: lFun, a3: dFun): lFun = lFunDummy
  def helperLongLongString2Long(a1: lFun, a2: lFun, a3: sFun): lFun = lFunDummy
  def helperLongLongBoolean2Long(a1: lFun, a2: lFun, a3: bFun): lFun = lFunDummy
  def helperLongDoubleInt2Long(a1: lFun, a2: dFun, a3: iFun): lFun = lFunDummy
  def helperLongDoubleLong2Long(a1: lFun, a2: dFun, a3: lFun): lFun = lFunDummy
  def helperLongDoubleDouble2Long(a1: lFun, a2: dFun, a3: dFun): lFun = lFunDummy
  def helperLongDoubleString2Long(a1: lFun, a2: dFun, a3: sFun): lFun = lFunDummy
  def helperLongDoubleBoolean2Long(a1: lFun, a2: dFun, a3: bFun): lFun = lFunDummy
  def helperLongStringInt2Long(a1: lFun, a2: sFun, a3: iFun): lFun = lFunDummy
  def helperLongStringLong2Long(a1: lFun, a2: sFun, a3: lFun): lFun = lFunDummy
  def helperLongStringDouble2Long(a1: lFun, a2: sFun, a3: dFun): lFun = lFunDummy
  def helperLongStringString2Long(a1: lFun, a2: sFun, a3: sFun): lFun = lFunDummy
  def helperLongStringBoolean2Long(a1: lFun, a2: sFun, a3: bFun): lFun = lFunDummy
  def helperLongBooleanInt2Long(a1: lFun, a2: bFun, a3: iFun): lFun = lFunDummy
  def helperLongBooleanLong2Long(a1: lFun, a2: bFun, a3: lFun): lFun = lFunDummy
  def helperLongBooleanDouble2Long(a1: lFun, a2: bFun, a3: dFun): lFun = lFunDummy
  def helperLongBooleanString2Long(a1: lFun, a2: bFun, a3: sFun): lFun = lFunDummy
  def helperLongBooleanBoolean2Long(a1: lFun, a2: bFun, a3: bFun): lFun = lFunDummy
  def helperDoubleIntInt2Long(a1: dFun, a2: iFun, a3: iFun): lFun = lFunDummy
  def helperDoubleIntLong2Long(a1: dFun, a2: iFun, a3: lFun): lFun = lFunDummy
  def helperDoubleIntDouble2Long(a1: dFun, a2: iFun, a3: dFun): lFun = lFunDummy
  def helperDoubleIntString2Long(a1: dFun, a2: iFun, a3: sFun): lFun = lFunDummy
  def helperDoubleIntBoolean2Long(a1: dFun, a2: iFun, a3: bFun): lFun = lFunDummy
  def helperDoubleLongInt2Long(a1: dFun, a2: lFun, a3: iFun): lFun = lFunDummy
  def helperDoubleLongLong2Long(a1: dFun, a2: lFun, a3: lFun): lFun = lFunDummy
  def helperDoubleLongDouble2Long(a1: dFun, a2: lFun, a3: dFun): lFun = lFunDummy
  def helperDoubleLongString2Long(a1: dFun, a2: lFun, a3: sFun): lFun = lFunDummy
  def helperDoubleLongBoolean2Long(a1: dFun, a2: lFun, a3: bFun): lFun = lFunDummy
  def helperDoubleDoubleInt2Long(a1: dFun, a2: dFun, a3: iFun): lFun = lFunDummy
  def helperDoubleDoubleLong2Long(a1: dFun, a2: dFun, a3: lFun): lFun = lFunDummy
  def helperDoubleDoubleDouble2Long(a1: dFun, a2: dFun, a3: dFun): lFun = lFunDummy
  def helperDoubleDoubleString2Long(a1: dFun, a2: dFun, a3: sFun): lFun = lFunDummy
  def helperDoubleDoubleBoolean2Long(a1: dFun, a2: dFun, a3: bFun): lFun = lFunDummy
  def helperDoubleStringInt2Long(a1: dFun, a2: sFun, a3: iFun): lFun = lFunDummy
  def helperDoubleStringLong2Long(a1: dFun, a2: sFun, a3: lFun): lFun = lFunDummy
  def helperDoubleStringDouble2Long(a1: dFun, a2: sFun, a3: dFun): lFun = lFunDummy
  def helperDoubleStringString2Long(a1: dFun, a2: sFun, a3: sFun): lFun = lFunDummy
  def helperDoubleStringBoolean2Long(a1: dFun, a2: sFun, a3: bFun): lFun = lFunDummy
  def helperDoubleBooleanInt2Long(a1: dFun, a2: bFun, a3: iFun): lFun = lFunDummy
  def helperDoubleBooleanLong2Long(a1: dFun, a2: bFun, a3: lFun): lFun = lFunDummy
  def helperDoubleBooleanDouble2Long(a1: dFun, a2: bFun, a3: dFun): lFun = lFunDummy
  def helperDoubleBooleanString2Long(a1: dFun, a2: bFun, a3: sFun): lFun = lFunDummy
  def helperDoubleBooleanBoolean2Long(a1: dFun, a2: bFun, a3: bFun): lFun = lFunDummy
  def helperStringIntInt2Long(a1: sFun, a2: iFun, a3: iFun): lFun = lFunDummy
  def helperStringIntLong2Long(a1: sFun, a2: iFun, a3: lFun): lFun = lFunDummy
  def helperStringIntDouble2Long(a1: sFun, a2: iFun, a3: dFun): lFun = lFunDummy
  def helperStringIntString2Long(a1: sFun, a2: iFun, a3: sFun): lFun = lFunDummy
  def helperStringIntBoolean2Long(a1: sFun, a2: iFun, a3: bFun): lFun = lFunDummy
  def helperStringLongInt2Long(a1: sFun, a2: lFun, a3: iFun): lFun = lFunDummy
  def helperStringLongLong2Long(a1: sFun, a2: lFun, a3: lFun): lFun = lFunDummy
  def helperStringLongDouble2Long(a1: sFun, a2: lFun, a3: dFun): lFun = lFunDummy
  def helperStringLongString2Long(a1: sFun, a2: lFun, a3: sFun): lFun = lFunDummy
  def helperStringLongBoolean2Long(a1: sFun, a2: lFun, a3: bFun): lFun = lFunDummy
  def helperStringDoubleInt2Long(a1: sFun, a2: dFun, a3: iFun): lFun = lFunDummy
  def helperStringDoubleLong2Long(a1: sFun, a2: dFun, a3: lFun): lFun = lFunDummy
  def helperStringDoubleDouble2Long(a1: sFun, a2: dFun, a3: dFun): lFun = lFunDummy
  def helperStringDoubleString2Long(a1: sFun, a2: dFun, a3: sFun): lFun = lFunDummy
  def helperStringDoubleBoolean2Long(a1: sFun, a2: dFun, a3: bFun): lFun = lFunDummy
  def helperStringStringInt2Long(a1: sFun, a2: sFun, a3: iFun): lFun = lFunDummy
  def helperStringStringLong2Long(a1: sFun, a2: sFun, a3: lFun): lFun = lFunDummy
  def helperStringStringDouble2Long(a1: sFun, a2: sFun, a3: dFun): lFun = lFunDummy
  def helperStringStringString2Long(a1: sFun, a2: sFun, a3: sFun): lFun = lFunDummy
  def helperStringStringBoolean2Long(a1: sFun, a2: sFun, a3: bFun): lFun = lFunDummy
  def helperStringBooleanInt2Long(a1: sFun, a2: bFun, a3: iFun): lFun = lFunDummy
  def helperStringBooleanLong2Long(a1: sFun, a2: bFun, a3: lFun): lFun = lFunDummy
  def helperStringBooleanDouble2Long(a1: sFun, a2: bFun, a3: dFun): lFun = lFunDummy
  def helperStringBooleanString2Long(a1: sFun, a2: bFun, a3: sFun): lFun = lFunDummy
  def helperStringBooleanBoolean2Long(a1: sFun, a2: bFun, a3: bFun): lFun = lFunDummy
  def helperBooleanIntInt2Long(a1: bFun, a2: iFun, a3: iFun): lFun = lFunDummy
  def helperBooleanIntLong2Long(a1: bFun, a2: iFun, a3: lFun): lFun = lFunDummy
  def helperBooleanIntDouble2Long(a1: bFun, a2: iFun, a3: dFun): lFun = lFunDummy
  def helperBooleanIntString2Long(a1: bFun, a2: iFun, a3: sFun): lFun = lFunDummy
  def helperBooleanIntBoolean2Long(a1: bFun, a2: iFun, a3: bFun): lFun = lFunDummy
  def helperBooleanLongInt2Long(a1: bFun, a2: lFun, a3: iFun): lFun = lFunDummy
  def helperBooleanLongLong2Long(a1: bFun, a2: lFun, a3: lFun): lFun = lFunDummy
  def helperBooleanLongDouble2Long(a1: bFun, a2: lFun, a3: dFun): lFun = lFunDummy
  def helperBooleanLongString2Long(a1: bFun, a2: lFun, a3: sFun): lFun = lFunDummy
  def helperBooleanLongBoolean2Long(a1: bFun, a2: lFun, a3: bFun): lFun = lFunDummy
  def helperBooleanDoubleInt2Long(a1: bFun, a2: dFun, a3: iFun): lFun = lFunDummy
  def helperBooleanDoubleLong2Long(a1: bFun, a2: dFun, a3: lFun): lFun = lFunDummy
  def helperBooleanDoubleDouble2Long(a1: bFun, a2: dFun, a3: dFun): lFun = lFunDummy
  def helperBooleanDoubleString2Long(a1: bFun, a2: dFun, a3: sFun): lFun = lFunDummy
  def helperBooleanDoubleBoolean2Long(a1: bFun, a2: dFun, a3: bFun): lFun = lFunDummy
  def helperBooleanStringInt2Long(a1: bFun, a2: sFun, a3: iFun): lFun = lFunDummy
  def helperBooleanStringLong2Long(a1: bFun, a2: sFun, a3: lFun): lFun = lFunDummy
  def helperBooleanStringDouble2Long(a1: bFun, a2: sFun, a3: dFun): lFun = lFunDummy
  def helperBooleanStringString2Long(a1: bFun, a2: sFun, a3: sFun): lFun = lFunDummy
  def helperBooleanStringBoolean2Long(a1: bFun, a2: sFun, a3: bFun): lFun = lFunDummy
  def helperBooleanBooleanInt2Long(a1: bFun, a2: bFun, a3: iFun): lFun = lFunDummy
  def helperBooleanBooleanLong2Long(a1: bFun, a2: bFun, a3: lFun): lFun = lFunDummy
  def helperBooleanBooleanDouble2Long(a1: bFun, a2: bFun, a3: dFun): lFun = lFunDummy
  def helperBooleanBooleanString2Long(a1: bFun, a2: bFun, a3: sFun): lFun = lFunDummy
  def helperBooleanBooleanBoolean2Long(a1: bFun, a2: bFun, a3: bFun): lFun = lFunDummy
  test("getSignature_lFun_4") {
    var result = true
    result &= FunctionSignature.getSignatureIntIntInt2Long(helperIntIntInt2Long) == "Int:Int:Int2Long"
    result &= FunctionSignature.getSignatureIntIntLong2Long(helperIntIntLong2Long) == "Int:Int:Long2Long"
    result &= FunctionSignature.getSignatureIntIntDouble2Long(helperIntIntDouble2Long) == "Int:Int:Double2Long"
    result &= FunctionSignature.getSignatureIntIntString2Long(helperIntIntString2Long) == "Int:Int:String2Long"
    result &= FunctionSignature.getSignatureIntIntBoolean2Long(helperIntIntBoolean2Long) == "Int:Int:Boolean2Long"
    result &= FunctionSignature.getSignatureIntLongInt2Long(helperIntLongInt2Long) == "Int:Long:Int2Long"
    result &= FunctionSignature.getSignatureIntLongLong2Long(helperIntLongLong2Long) == "Int:Long:Long2Long"
    result &= FunctionSignature.getSignatureIntLongDouble2Long(helperIntLongDouble2Long) == "Int:Long:Double2Long"
    result &= FunctionSignature.getSignatureIntLongString2Long(helperIntLongString2Long) == "Int:Long:String2Long"
    result &= FunctionSignature.getSignatureIntLongBoolean2Long(helperIntLongBoolean2Long) == "Int:Long:Boolean2Long"
    result &= FunctionSignature.getSignatureIntDoubleInt2Long(helperIntDoubleInt2Long) == "Int:Double:Int2Long"
    result &= FunctionSignature.getSignatureIntDoubleLong2Long(helperIntDoubleLong2Long) == "Int:Double:Long2Long"
    result &= FunctionSignature.getSignatureIntDoubleDouble2Long(helperIntDoubleDouble2Long) == "Int:Double:Double2Long"
    result &= FunctionSignature.getSignatureIntDoubleString2Long(helperIntDoubleString2Long) == "Int:Double:String2Long"
    result &= FunctionSignature.getSignatureIntDoubleBoolean2Long(helperIntDoubleBoolean2Long) == "Int:Double:Boolean2Long"
    result &= FunctionSignature.getSignatureIntStringInt2Long(helperIntStringInt2Long) == "Int:String:Int2Long"
    result &= FunctionSignature.getSignatureIntStringLong2Long(helperIntStringLong2Long) == "Int:String:Long2Long"
    result &= FunctionSignature.getSignatureIntStringDouble2Long(helperIntStringDouble2Long) == "Int:String:Double2Long"
    result &= FunctionSignature.getSignatureIntStringString2Long(helperIntStringString2Long) == "Int:String:String2Long"
    result &= FunctionSignature.getSignatureIntStringBoolean2Long(helperIntStringBoolean2Long) == "Int:String:Boolean2Long"
    result &= FunctionSignature.getSignatureIntBooleanInt2Long(helperIntBooleanInt2Long) == "Int:Boolean:Int2Long"
    result &= FunctionSignature.getSignatureIntBooleanLong2Long(helperIntBooleanLong2Long) == "Int:Boolean:Long2Long"
    result &= FunctionSignature.getSignatureIntBooleanDouble2Long(helperIntBooleanDouble2Long) == "Int:Boolean:Double2Long"
    result &= FunctionSignature.getSignatureIntBooleanString2Long(helperIntBooleanString2Long) == "Int:Boolean:String2Long"
    result &= FunctionSignature.getSignatureIntBooleanBoolean2Long(helperIntBooleanBoolean2Long) == "Int:Boolean:Boolean2Long"
    result &= FunctionSignature.getSignatureLongIntInt2Long(helperLongIntInt2Long) == "Long:Int:Int2Long"
    result &= FunctionSignature.getSignatureLongIntLong2Long(helperLongIntLong2Long) == "Long:Int:Long2Long"
    result &= FunctionSignature.getSignatureLongIntDouble2Long(helperLongIntDouble2Long) == "Long:Int:Double2Long"
    result &= FunctionSignature.getSignatureLongIntString2Long(helperLongIntString2Long) == "Long:Int:String2Long"
    result &= FunctionSignature.getSignatureLongIntBoolean2Long(helperLongIntBoolean2Long) == "Long:Int:Boolean2Long"
    result &= FunctionSignature.getSignatureLongLongInt2Long(helperLongLongInt2Long) == "Long:Long:Int2Long"
    result &= FunctionSignature.getSignatureLongLongLong2Long(helperLongLongLong2Long) == "Long:Long:Long2Long"
    result &= FunctionSignature.getSignatureLongLongDouble2Long(helperLongLongDouble2Long) == "Long:Long:Double2Long"
    result &= FunctionSignature.getSignatureLongLongString2Long(helperLongLongString2Long) == "Long:Long:String2Long"
    result &= FunctionSignature.getSignatureLongLongBoolean2Long(helperLongLongBoolean2Long) == "Long:Long:Boolean2Long"
    result &= FunctionSignature.getSignatureLongDoubleInt2Long(helperLongDoubleInt2Long) == "Long:Double:Int2Long"
    result &= FunctionSignature.getSignatureLongDoubleLong2Long(helperLongDoubleLong2Long) == "Long:Double:Long2Long"
    result &= FunctionSignature.getSignatureLongDoubleDouble2Long(helperLongDoubleDouble2Long) == "Long:Double:Double2Long"
    result &= FunctionSignature.getSignatureLongDoubleString2Long(helperLongDoubleString2Long) == "Long:Double:String2Long"
    result &= FunctionSignature.getSignatureLongDoubleBoolean2Long(helperLongDoubleBoolean2Long) == "Long:Double:Boolean2Long"
    result &= FunctionSignature.getSignatureLongStringInt2Long(helperLongStringInt2Long) == "Long:String:Int2Long"
    result &= FunctionSignature.getSignatureLongStringLong2Long(helperLongStringLong2Long) == "Long:String:Long2Long"
    result &= FunctionSignature.getSignatureLongStringDouble2Long(helperLongStringDouble2Long) == "Long:String:Double2Long"
    result &= FunctionSignature.getSignatureLongStringString2Long(helperLongStringString2Long) == "Long:String:String2Long"
    result &= FunctionSignature.getSignatureLongStringBoolean2Long(helperLongStringBoolean2Long) == "Long:String:Boolean2Long"
    result &= FunctionSignature.getSignatureLongBooleanInt2Long(helperLongBooleanInt2Long) == "Long:Boolean:Int2Long"
    result &= FunctionSignature.getSignatureLongBooleanLong2Long(helperLongBooleanLong2Long) == "Long:Boolean:Long2Long"
    result &= FunctionSignature.getSignatureLongBooleanDouble2Long(helperLongBooleanDouble2Long) == "Long:Boolean:Double2Long"
    result &= FunctionSignature.getSignatureLongBooleanString2Long(helperLongBooleanString2Long) == "Long:Boolean:String2Long"
    result &= FunctionSignature.getSignatureLongBooleanBoolean2Long(helperLongBooleanBoolean2Long) == "Long:Boolean:Boolean2Long"
    result &= FunctionSignature.getSignatureDoubleIntInt2Long(helperDoubleIntInt2Long) == "Double:Int:Int2Long"
    result &= FunctionSignature.getSignatureDoubleIntLong2Long(helperDoubleIntLong2Long) == "Double:Int:Long2Long"
    result &= FunctionSignature.getSignatureDoubleIntDouble2Long(helperDoubleIntDouble2Long) == "Double:Int:Double2Long"
    result &= FunctionSignature.getSignatureDoubleIntString2Long(helperDoubleIntString2Long) == "Double:Int:String2Long"
    result &= FunctionSignature.getSignatureDoubleIntBoolean2Long(helperDoubleIntBoolean2Long) == "Double:Int:Boolean2Long"
    result &= FunctionSignature.getSignatureDoubleLongInt2Long(helperDoubleLongInt2Long) == "Double:Long:Int2Long"
    result &= FunctionSignature.getSignatureDoubleLongLong2Long(helperDoubleLongLong2Long) == "Double:Long:Long2Long"
    result &= FunctionSignature.getSignatureDoubleLongDouble2Long(helperDoubleLongDouble2Long) == "Double:Long:Double2Long"
    result &= FunctionSignature.getSignatureDoubleLongString2Long(helperDoubleLongString2Long) == "Double:Long:String2Long"
    result &= FunctionSignature.getSignatureDoubleLongBoolean2Long(helperDoubleLongBoolean2Long) == "Double:Long:Boolean2Long"
    result &= FunctionSignature.getSignatureDoubleDoubleInt2Long(helperDoubleDoubleInt2Long) == "Double:Double:Int2Long"
    result &= FunctionSignature.getSignatureDoubleDoubleLong2Long(helperDoubleDoubleLong2Long) == "Double:Double:Long2Long"
    result &= FunctionSignature.getSignatureDoubleDoubleDouble2Long(helperDoubleDoubleDouble2Long) == "Double:Double:Double2Long"
    result &= FunctionSignature.getSignatureDoubleDoubleString2Long(helperDoubleDoubleString2Long) == "Double:Double:String2Long"
    result &= FunctionSignature.getSignatureDoubleDoubleBoolean2Long(helperDoubleDoubleBoolean2Long) == "Double:Double:Boolean2Long"
    result &= FunctionSignature.getSignatureDoubleStringInt2Long(helperDoubleStringInt2Long) == "Double:String:Int2Long"
    result &= FunctionSignature.getSignatureDoubleStringLong2Long(helperDoubleStringLong2Long) == "Double:String:Long2Long"
    result &= FunctionSignature.getSignatureDoubleStringDouble2Long(helperDoubleStringDouble2Long) == "Double:String:Double2Long"
    result &= FunctionSignature.getSignatureDoubleStringString2Long(helperDoubleStringString2Long) == "Double:String:String2Long"
    result &= FunctionSignature.getSignatureDoubleStringBoolean2Long(helperDoubleStringBoolean2Long) == "Double:String:Boolean2Long"
    result &= FunctionSignature.getSignatureDoubleBooleanInt2Long(helperDoubleBooleanInt2Long) == "Double:Boolean:Int2Long"
    result &= FunctionSignature.getSignatureDoubleBooleanLong2Long(helperDoubleBooleanLong2Long) == "Double:Boolean:Long2Long"
    result &= FunctionSignature.getSignatureDoubleBooleanDouble2Long(helperDoubleBooleanDouble2Long) == "Double:Boolean:Double2Long"
    result &= FunctionSignature.getSignatureDoubleBooleanString2Long(helperDoubleBooleanString2Long) == "Double:Boolean:String2Long"
    result &= FunctionSignature.getSignatureDoubleBooleanBoolean2Long(helperDoubleBooleanBoolean2Long) == "Double:Boolean:Boolean2Long"
    result &= FunctionSignature.getSignatureStringIntInt2Long(helperStringIntInt2Long) == "String:Int:Int2Long"
    result &= FunctionSignature.getSignatureStringIntLong2Long(helperStringIntLong2Long) == "String:Int:Long2Long"
    result &= FunctionSignature.getSignatureStringIntDouble2Long(helperStringIntDouble2Long) == "String:Int:Double2Long"
    result &= FunctionSignature.getSignatureStringIntString2Long(helperStringIntString2Long) == "String:Int:String2Long"
    result &= FunctionSignature.getSignatureStringIntBoolean2Long(helperStringIntBoolean2Long) == "String:Int:Boolean2Long"
    result &= FunctionSignature.getSignatureStringLongInt2Long(helperStringLongInt2Long) == "String:Long:Int2Long"
    result &= FunctionSignature.getSignatureStringLongLong2Long(helperStringLongLong2Long) == "String:Long:Long2Long"
    result &= FunctionSignature.getSignatureStringLongDouble2Long(helperStringLongDouble2Long) == "String:Long:Double2Long"
    result &= FunctionSignature.getSignatureStringLongString2Long(helperStringLongString2Long) == "String:Long:String2Long"
    result &= FunctionSignature.getSignatureStringLongBoolean2Long(helperStringLongBoolean2Long) == "String:Long:Boolean2Long"
    result &= FunctionSignature.getSignatureStringDoubleInt2Long(helperStringDoubleInt2Long) == "String:Double:Int2Long"
    result &= FunctionSignature.getSignatureStringDoubleLong2Long(helperStringDoubleLong2Long) == "String:Double:Long2Long"
    result &= FunctionSignature.getSignatureStringDoubleDouble2Long(helperStringDoubleDouble2Long) == "String:Double:Double2Long"
    result &= FunctionSignature.getSignatureStringDoubleString2Long(helperStringDoubleString2Long) == "String:Double:String2Long"
    result &= FunctionSignature.getSignatureStringDoubleBoolean2Long(helperStringDoubleBoolean2Long) == "String:Double:Boolean2Long"
    result &= FunctionSignature.getSignatureStringStringInt2Long(helperStringStringInt2Long) == "String:String:Int2Long"
    result &= FunctionSignature.getSignatureStringStringLong2Long(helperStringStringLong2Long) == "String:String:Long2Long"
    result &= FunctionSignature.getSignatureStringStringDouble2Long(helperStringStringDouble2Long) == "String:String:Double2Long"
    result &= FunctionSignature.getSignatureStringStringString2Long(helperStringStringString2Long) == "String:String:String2Long"
    result &= FunctionSignature.getSignatureStringStringBoolean2Long(helperStringStringBoolean2Long) == "String:String:Boolean2Long"
    result &= FunctionSignature.getSignatureStringBooleanInt2Long(helperStringBooleanInt2Long) == "String:Boolean:Int2Long"
    result &= FunctionSignature.getSignatureStringBooleanLong2Long(helperStringBooleanLong2Long) == "String:Boolean:Long2Long"
    result &= FunctionSignature.getSignatureStringBooleanDouble2Long(helperStringBooleanDouble2Long) == "String:Boolean:Double2Long"
    result &= FunctionSignature.getSignatureStringBooleanString2Long(helperStringBooleanString2Long) == "String:Boolean:String2Long"
    result &= FunctionSignature.getSignatureStringBooleanBoolean2Long(helperStringBooleanBoolean2Long) == "String:Boolean:Boolean2Long"
    result &= FunctionSignature.getSignatureBooleanIntInt2Long(helperBooleanIntInt2Long) == "Boolean:Int:Int2Long"
    result &= FunctionSignature.getSignatureBooleanIntLong2Long(helperBooleanIntLong2Long) == "Boolean:Int:Long2Long"
    result &= FunctionSignature.getSignatureBooleanIntDouble2Long(helperBooleanIntDouble2Long) == "Boolean:Int:Double2Long"
    result &= FunctionSignature.getSignatureBooleanIntString2Long(helperBooleanIntString2Long) == "Boolean:Int:String2Long"
    result &= FunctionSignature.getSignatureBooleanIntBoolean2Long(helperBooleanIntBoolean2Long) == "Boolean:Int:Boolean2Long"
    result &= FunctionSignature.getSignatureBooleanLongInt2Long(helperBooleanLongInt2Long) == "Boolean:Long:Int2Long"
    result &= FunctionSignature.getSignatureBooleanLongLong2Long(helperBooleanLongLong2Long) == "Boolean:Long:Long2Long"
    result &= FunctionSignature.getSignatureBooleanLongDouble2Long(helperBooleanLongDouble2Long) == "Boolean:Long:Double2Long"
    result &= FunctionSignature.getSignatureBooleanLongString2Long(helperBooleanLongString2Long) == "Boolean:Long:String2Long"
    result &= FunctionSignature.getSignatureBooleanLongBoolean2Long(helperBooleanLongBoolean2Long) == "Boolean:Long:Boolean2Long"
    result &= FunctionSignature.getSignatureBooleanDoubleInt2Long(helperBooleanDoubleInt2Long) == "Boolean:Double:Int2Long"
    result &= FunctionSignature.getSignatureBooleanDoubleLong2Long(helperBooleanDoubleLong2Long) == "Boolean:Double:Long2Long"
    result &= FunctionSignature.getSignatureBooleanDoubleDouble2Long(helperBooleanDoubleDouble2Long) == "Boolean:Double:Double2Long"
    result &= FunctionSignature.getSignatureBooleanDoubleString2Long(helperBooleanDoubleString2Long) == "Boolean:Double:String2Long"
    result &= FunctionSignature.getSignatureBooleanDoubleBoolean2Long(helperBooleanDoubleBoolean2Long) == "Boolean:Double:Boolean2Long"
    result &= FunctionSignature.getSignatureBooleanStringInt2Long(helperBooleanStringInt2Long) == "Boolean:String:Int2Long"
    result &= FunctionSignature.getSignatureBooleanStringLong2Long(helperBooleanStringLong2Long) == "Boolean:String:Long2Long"
    result &= FunctionSignature.getSignatureBooleanStringDouble2Long(helperBooleanStringDouble2Long) == "Boolean:String:Double2Long"
    result &= FunctionSignature.getSignatureBooleanStringString2Long(helperBooleanStringString2Long) == "Boolean:String:String2Long"
    result &= FunctionSignature.getSignatureBooleanStringBoolean2Long(helperBooleanStringBoolean2Long) == "Boolean:String:Boolean2Long"
    result &= FunctionSignature.getSignatureBooleanBooleanInt2Long(helperBooleanBooleanInt2Long) == "Boolean:Boolean:Int2Long"
    result &= FunctionSignature.getSignatureBooleanBooleanLong2Long(helperBooleanBooleanLong2Long) == "Boolean:Boolean:Long2Long"
    result &= FunctionSignature.getSignatureBooleanBooleanDouble2Long(helperBooleanBooleanDouble2Long) == "Boolean:Boolean:Double2Long"
    result &= FunctionSignature.getSignatureBooleanBooleanString2Long(helperBooleanBooleanString2Long) == "Boolean:Boolean:String2Long"
    result &= FunctionSignature.getSignatureBooleanBooleanBoolean2Long(helperBooleanBooleanBoolean2Long) == "Boolean:Boolean:Boolean2Long"
    assert(result)
  }
  def helperIntIntInt2Double(a1: iFun, a2: iFun, a3: iFun): dFun = dFunDummy
  def helperIntIntLong2Double(a1: iFun, a2: iFun, a3: lFun): dFun = dFunDummy
  def helperIntIntDouble2Double(a1: iFun, a2: iFun, a3: dFun): dFun = dFunDummy
  def helperIntIntString2Double(a1: iFun, a2: iFun, a3: sFun): dFun = dFunDummy
  def helperIntIntBoolean2Double(a1: iFun, a2: iFun, a3: bFun): dFun = dFunDummy
  def helperIntLongInt2Double(a1: iFun, a2: lFun, a3: iFun): dFun = dFunDummy
  def helperIntLongLong2Double(a1: iFun, a2: lFun, a3: lFun): dFun = dFunDummy
  def helperIntLongDouble2Double(a1: iFun, a2: lFun, a3: dFun): dFun = dFunDummy
  def helperIntLongString2Double(a1: iFun, a2: lFun, a3: sFun): dFun = dFunDummy
  def helperIntLongBoolean2Double(a1: iFun, a2: lFun, a3: bFun): dFun = dFunDummy
  def helperIntDoubleInt2Double(a1: iFun, a2: dFun, a3: iFun): dFun = dFunDummy
  def helperIntDoubleLong2Double(a1: iFun, a2: dFun, a3: lFun): dFun = dFunDummy
  def helperIntDoubleDouble2Double(a1: iFun, a2: dFun, a3: dFun): dFun = dFunDummy
  def helperIntDoubleString2Double(a1: iFun, a2: dFun, a3: sFun): dFun = dFunDummy
  def helperIntDoubleBoolean2Double(a1: iFun, a2: dFun, a3: bFun): dFun = dFunDummy
  def helperIntStringInt2Double(a1: iFun, a2: sFun, a3: iFun): dFun = dFunDummy
  def helperIntStringLong2Double(a1: iFun, a2: sFun, a3: lFun): dFun = dFunDummy
  def helperIntStringDouble2Double(a1: iFun, a2: sFun, a3: dFun): dFun = dFunDummy
  def helperIntStringString2Double(a1: iFun, a2: sFun, a3: sFun): dFun = dFunDummy
  def helperIntStringBoolean2Double(a1: iFun, a2: sFun, a3: bFun): dFun = dFunDummy
  def helperIntBooleanInt2Double(a1: iFun, a2: bFun, a3: iFun): dFun = dFunDummy
  def helperIntBooleanLong2Double(a1: iFun, a2: bFun, a3: lFun): dFun = dFunDummy
  def helperIntBooleanDouble2Double(a1: iFun, a2: bFun, a3: dFun): dFun = dFunDummy
  def helperIntBooleanString2Double(a1: iFun, a2: bFun, a3: sFun): dFun = dFunDummy
  def helperIntBooleanBoolean2Double(a1: iFun, a2: bFun, a3: bFun): dFun = dFunDummy
  def helperLongIntInt2Double(a1: lFun, a2: iFun, a3: iFun): dFun = dFunDummy
  def helperLongIntLong2Double(a1: lFun, a2: iFun, a3: lFun): dFun = dFunDummy
  def helperLongIntDouble2Double(a1: lFun, a2: iFun, a3: dFun): dFun = dFunDummy
  def helperLongIntString2Double(a1: lFun, a2: iFun, a3: sFun): dFun = dFunDummy
  def helperLongIntBoolean2Double(a1: lFun, a2: iFun, a3: bFun): dFun = dFunDummy
  def helperLongLongInt2Double(a1: lFun, a2: lFun, a3: iFun): dFun = dFunDummy
  def helperLongLongLong2Double(a1: lFun, a2: lFun, a3: lFun): dFun = dFunDummy
  def helperLongLongDouble2Double(a1: lFun, a2: lFun, a3: dFun): dFun = dFunDummy
  def helperLongLongString2Double(a1: lFun, a2: lFun, a3: sFun): dFun = dFunDummy
  def helperLongLongBoolean2Double(a1: lFun, a2: lFun, a3: bFun): dFun = dFunDummy
  def helperLongDoubleInt2Double(a1: lFun, a2: dFun, a3: iFun): dFun = dFunDummy
  def helperLongDoubleLong2Double(a1: lFun, a2: dFun, a3: lFun): dFun = dFunDummy
  def helperLongDoubleDouble2Double(a1: lFun, a2: dFun, a3: dFun): dFun = dFunDummy
  def helperLongDoubleString2Double(a1: lFun, a2: dFun, a3: sFun): dFun = dFunDummy
  def helperLongDoubleBoolean2Double(a1: lFun, a2: dFun, a3: bFun): dFun = dFunDummy
  def helperLongStringInt2Double(a1: lFun, a2: sFun, a3: iFun): dFun = dFunDummy
  def helperLongStringLong2Double(a1: lFun, a2: sFun, a3: lFun): dFun = dFunDummy
  def helperLongStringDouble2Double(a1: lFun, a2: sFun, a3: dFun): dFun = dFunDummy
  def helperLongStringString2Double(a1: lFun, a2: sFun, a3: sFun): dFun = dFunDummy
  def helperLongStringBoolean2Double(a1: lFun, a2: sFun, a3: bFun): dFun = dFunDummy
  def helperLongBooleanInt2Double(a1: lFun, a2: bFun, a3: iFun): dFun = dFunDummy
  def helperLongBooleanLong2Double(a1: lFun, a2: bFun, a3: lFun): dFun = dFunDummy
  def helperLongBooleanDouble2Double(a1: lFun, a2: bFun, a3: dFun): dFun = dFunDummy
  def helperLongBooleanString2Double(a1: lFun, a2: bFun, a3: sFun): dFun = dFunDummy
  def helperLongBooleanBoolean2Double(a1: lFun, a2: bFun, a3: bFun): dFun = dFunDummy
  def helperDoubleIntInt2Double(a1: dFun, a2: iFun, a3: iFun): dFun = dFunDummy
  def helperDoubleIntLong2Double(a1: dFun, a2: iFun, a3: lFun): dFun = dFunDummy
  def helperDoubleIntDouble2Double(a1: dFun, a2: iFun, a3: dFun): dFun = dFunDummy
  def helperDoubleIntString2Double(a1: dFun, a2: iFun, a3: sFun): dFun = dFunDummy
  def helperDoubleIntBoolean2Double(a1: dFun, a2: iFun, a3: bFun): dFun = dFunDummy
  def helperDoubleLongInt2Double(a1: dFun, a2: lFun, a3: iFun): dFun = dFunDummy
  def helperDoubleLongLong2Double(a1: dFun, a2: lFun, a3: lFun): dFun = dFunDummy
  def helperDoubleLongDouble2Double(a1: dFun, a2: lFun, a3: dFun): dFun = dFunDummy
  def helperDoubleLongString2Double(a1: dFun, a2: lFun, a3: sFun): dFun = dFunDummy
  def helperDoubleLongBoolean2Double(a1: dFun, a2: lFun, a3: bFun): dFun = dFunDummy
  def helperDoubleDoubleInt2Double(a1: dFun, a2: dFun, a3: iFun): dFun = dFunDummy
  def helperDoubleDoubleLong2Double(a1: dFun, a2: dFun, a3: lFun): dFun = dFunDummy
  def helperDoubleDoubleDouble2Double(a1: dFun, a2: dFun, a3: dFun): dFun = dFunDummy
  def helperDoubleDoubleString2Double(a1: dFun, a2: dFun, a3: sFun): dFun = dFunDummy
  def helperDoubleDoubleBoolean2Double(a1: dFun, a2: dFun, a3: bFun): dFun = dFunDummy
  def helperDoubleStringInt2Double(a1: dFun, a2: sFun, a3: iFun): dFun = dFunDummy
  def helperDoubleStringLong2Double(a1: dFun, a2: sFun, a3: lFun): dFun = dFunDummy
  def helperDoubleStringDouble2Double(a1: dFun, a2: sFun, a3: dFun): dFun = dFunDummy
  def helperDoubleStringString2Double(a1: dFun, a2: sFun, a3: sFun): dFun = dFunDummy
  def helperDoubleStringBoolean2Double(a1: dFun, a2: sFun, a3: bFun): dFun = dFunDummy
  def helperDoubleBooleanInt2Double(a1: dFun, a2: bFun, a3: iFun): dFun = dFunDummy
  def helperDoubleBooleanLong2Double(a1: dFun, a2: bFun, a3: lFun): dFun = dFunDummy
  def helperDoubleBooleanDouble2Double(a1: dFun, a2: bFun, a3: dFun): dFun = dFunDummy
  def helperDoubleBooleanString2Double(a1: dFun, a2: bFun, a3: sFun): dFun = dFunDummy
  def helperDoubleBooleanBoolean2Double(a1: dFun, a2: bFun, a3: bFun): dFun = dFunDummy
  def helperStringIntInt2Double(a1: sFun, a2: iFun, a3: iFun): dFun = dFunDummy
  def helperStringIntLong2Double(a1: sFun, a2: iFun, a3: lFun): dFun = dFunDummy
  def helperStringIntDouble2Double(a1: sFun, a2: iFun, a3: dFun): dFun = dFunDummy
  def helperStringIntString2Double(a1: sFun, a2: iFun, a3: sFun): dFun = dFunDummy
  def helperStringIntBoolean2Double(a1: sFun, a2: iFun, a3: bFun): dFun = dFunDummy
  def helperStringLongInt2Double(a1: sFun, a2: lFun, a3: iFun): dFun = dFunDummy
  def helperStringLongLong2Double(a1: sFun, a2: lFun, a3: lFun): dFun = dFunDummy
  def helperStringLongDouble2Double(a1: sFun, a2: lFun, a3: dFun): dFun = dFunDummy
  def helperStringLongString2Double(a1: sFun, a2: lFun, a3: sFun): dFun = dFunDummy
  def helperStringLongBoolean2Double(a1: sFun, a2: lFun, a3: bFun): dFun = dFunDummy
  def helperStringDoubleInt2Double(a1: sFun, a2: dFun, a3: iFun): dFun = dFunDummy
  def helperStringDoubleLong2Double(a1: sFun, a2: dFun, a3: lFun): dFun = dFunDummy
  def helperStringDoubleDouble2Double(a1: sFun, a2: dFun, a3: dFun): dFun = dFunDummy
  def helperStringDoubleString2Double(a1: sFun, a2: dFun, a3: sFun): dFun = dFunDummy
  def helperStringDoubleBoolean2Double(a1: sFun, a2: dFun, a3: bFun): dFun = dFunDummy
  def helperStringStringInt2Double(a1: sFun, a2: sFun, a3: iFun): dFun = dFunDummy
  def helperStringStringLong2Double(a1: sFun, a2: sFun, a3: lFun): dFun = dFunDummy
  def helperStringStringDouble2Double(a1: sFun, a2: sFun, a3: dFun): dFun = dFunDummy
  def helperStringStringString2Double(a1: sFun, a2: sFun, a3: sFun): dFun = dFunDummy
  def helperStringStringBoolean2Double(a1: sFun, a2: sFun, a3: bFun): dFun = dFunDummy
  def helperStringBooleanInt2Double(a1: sFun, a2: bFun, a3: iFun): dFun = dFunDummy
  def helperStringBooleanLong2Double(a1: sFun, a2: bFun, a3: lFun): dFun = dFunDummy
  def helperStringBooleanDouble2Double(a1: sFun, a2: bFun, a3: dFun): dFun = dFunDummy
  def helperStringBooleanString2Double(a1: sFun, a2: bFun, a3: sFun): dFun = dFunDummy
  def helperStringBooleanBoolean2Double(a1: sFun, a2: bFun, a3: bFun): dFun = dFunDummy
  def helperBooleanIntInt2Double(a1: bFun, a2: iFun, a3: iFun): dFun = dFunDummy
  def helperBooleanIntLong2Double(a1: bFun, a2: iFun, a3: lFun): dFun = dFunDummy
  def helperBooleanIntDouble2Double(a1: bFun, a2: iFun, a3: dFun): dFun = dFunDummy
  def helperBooleanIntString2Double(a1: bFun, a2: iFun, a3: sFun): dFun = dFunDummy
  def helperBooleanIntBoolean2Double(a1: bFun, a2: iFun, a3: bFun): dFun = dFunDummy
  def helperBooleanLongInt2Double(a1: bFun, a2: lFun, a3: iFun): dFun = dFunDummy
  def helperBooleanLongLong2Double(a1: bFun, a2: lFun, a3: lFun): dFun = dFunDummy
  def helperBooleanLongDouble2Double(a1: bFun, a2: lFun, a3: dFun): dFun = dFunDummy
  def helperBooleanLongString2Double(a1: bFun, a2: lFun, a3: sFun): dFun = dFunDummy
  def helperBooleanLongBoolean2Double(a1: bFun, a2: lFun, a3: bFun): dFun = dFunDummy
  def helperBooleanDoubleInt2Double(a1: bFun, a2: dFun, a3: iFun): dFun = dFunDummy
  def helperBooleanDoubleLong2Double(a1: bFun, a2: dFun, a3: lFun): dFun = dFunDummy
  def helperBooleanDoubleDouble2Double(a1: bFun, a2: dFun, a3: dFun): dFun = dFunDummy
  def helperBooleanDoubleString2Double(a1: bFun, a2: dFun, a3: sFun): dFun = dFunDummy
  def helperBooleanDoubleBoolean2Double(a1: bFun, a2: dFun, a3: bFun): dFun = dFunDummy
  def helperBooleanStringInt2Double(a1: bFun, a2: sFun, a3: iFun): dFun = dFunDummy
  def helperBooleanStringLong2Double(a1: bFun, a2: sFun, a3: lFun): dFun = dFunDummy
  def helperBooleanStringDouble2Double(a1: bFun, a2: sFun, a3: dFun): dFun = dFunDummy
  def helperBooleanStringString2Double(a1: bFun, a2: sFun, a3: sFun): dFun = dFunDummy
  def helperBooleanStringBoolean2Double(a1: bFun, a2: sFun, a3: bFun): dFun = dFunDummy
  def helperBooleanBooleanInt2Double(a1: bFun, a2: bFun, a3: iFun): dFun = dFunDummy
  def helperBooleanBooleanLong2Double(a1: bFun, a2: bFun, a3: lFun): dFun = dFunDummy
  def helperBooleanBooleanDouble2Double(a1: bFun, a2: bFun, a3: dFun): dFun = dFunDummy
  def helperBooleanBooleanString2Double(a1: bFun, a2: bFun, a3: sFun): dFun = dFunDummy
  def helperBooleanBooleanBoolean2Double(a1: bFun, a2: bFun, a3: bFun): dFun = dFunDummy
  test("getSignature_dFun_4") {
    var result = true
    result &= FunctionSignature.getSignatureIntIntInt2Double(helperIntIntInt2Double) == "Int:Int:Int2Double"
    result &= FunctionSignature.getSignatureIntIntLong2Double(helperIntIntLong2Double) == "Int:Int:Long2Double"
    result &= FunctionSignature.getSignatureIntIntDouble2Double(helperIntIntDouble2Double) == "Int:Int:Double2Double"
    result &= FunctionSignature.getSignatureIntIntString2Double(helperIntIntString2Double) == "Int:Int:String2Double"
    result &= FunctionSignature.getSignatureIntIntBoolean2Double(helperIntIntBoolean2Double) == "Int:Int:Boolean2Double"
    result &= FunctionSignature.getSignatureIntLongInt2Double(helperIntLongInt2Double) == "Int:Long:Int2Double"
    result &= FunctionSignature.getSignatureIntLongLong2Double(helperIntLongLong2Double) == "Int:Long:Long2Double"
    result &= FunctionSignature.getSignatureIntLongDouble2Double(helperIntLongDouble2Double) == "Int:Long:Double2Double"
    result &= FunctionSignature.getSignatureIntLongString2Double(helperIntLongString2Double) == "Int:Long:String2Double"
    result &= FunctionSignature.getSignatureIntLongBoolean2Double(helperIntLongBoolean2Double) == "Int:Long:Boolean2Double"
    result &= FunctionSignature.getSignatureIntDoubleInt2Double(helperIntDoubleInt2Double) == "Int:Double:Int2Double"
    result &= FunctionSignature.getSignatureIntDoubleLong2Double(helperIntDoubleLong2Double) == "Int:Double:Long2Double"
    result &= FunctionSignature.getSignatureIntDoubleDouble2Double(helperIntDoubleDouble2Double) == "Int:Double:Double2Double"
    result &= FunctionSignature.getSignatureIntDoubleString2Double(helperIntDoubleString2Double) == "Int:Double:String2Double"
    result &= FunctionSignature.getSignatureIntDoubleBoolean2Double(helperIntDoubleBoolean2Double) == "Int:Double:Boolean2Double"
    result &= FunctionSignature.getSignatureIntStringInt2Double(helperIntStringInt2Double) == "Int:String:Int2Double"
    result &= FunctionSignature.getSignatureIntStringLong2Double(helperIntStringLong2Double) == "Int:String:Long2Double"
    result &= FunctionSignature.getSignatureIntStringDouble2Double(helperIntStringDouble2Double) == "Int:String:Double2Double"
    result &= FunctionSignature.getSignatureIntStringString2Double(helperIntStringString2Double) == "Int:String:String2Double"
    result &= FunctionSignature.getSignatureIntStringBoolean2Double(helperIntStringBoolean2Double) == "Int:String:Boolean2Double"
    result &= FunctionSignature.getSignatureIntBooleanInt2Double(helperIntBooleanInt2Double) == "Int:Boolean:Int2Double"
    result &= FunctionSignature.getSignatureIntBooleanLong2Double(helperIntBooleanLong2Double) == "Int:Boolean:Long2Double"
    result &= FunctionSignature.getSignatureIntBooleanDouble2Double(helperIntBooleanDouble2Double) == "Int:Boolean:Double2Double"
    result &= FunctionSignature.getSignatureIntBooleanString2Double(helperIntBooleanString2Double) == "Int:Boolean:String2Double"
    result &= FunctionSignature.getSignatureIntBooleanBoolean2Double(helperIntBooleanBoolean2Double) == "Int:Boolean:Boolean2Double"
    result &= FunctionSignature.getSignatureLongIntInt2Double(helperLongIntInt2Double) == "Long:Int:Int2Double"
    result &= FunctionSignature.getSignatureLongIntLong2Double(helperLongIntLong2Double) == "Long:Int:Long2Double"
    result &= FunctionSignature.getSignatureLongIntDouble2Double(helperLongIntDouble2Double) == "Long:Int:Double2Double"
    result &= FunctionSignature.getSignatureLongIntString2Double(helperLongIntString2Double) == "Long:Int:String2Double"
    result &= FunctionSignature.getSignatureLongIntBoolean2Double(helperLongIntBoolean2Double) == "Long:Int:Boolean2Double"
    result &= FunctionSignature.getSignatureLongLongInt2Double(helperLongLongInt2Double) == "Long:Long:Int2Double"
    result &= FunctionSignature.getSignatureLongLongLong2Double(helperLongLongLong2Double) == "Long:Long:Long2Double"
    result &= FunctionSignature.getSignatureLongLongDouble2Double(helperLongLongDouble2Double) == "Long:Long:Double2Double"
    result &= FunctionSignature.getSignatureLongLongString2Double(helperLongLongString2Double) == "Long:Long:String2Double"
    result &= FunctionSignature.getSignatureLongLongBoolean2Double(helperLongLongBoolean2Double) == "Long:Long:Boolean2Double"
    result &= FunctionSignature.getSignatureLongDoubleInt2Double(helperLongDoubleInt2Double) == "Long:Double:Int2Double"
    result &= FunctionSignature.getSignatureLongDoubleLong2Double(helperLongDoubleLong2Double) == "Long:Double:Long2Double"
    result &= FunctionSignature.getSignatureLongDoubleDouble2Double(helperLongDoubleDouble2Double) == "Long:Double:Double2Double"
    result &= FunctionSignature.getSignatureLongDoubleString2Double(helperLongDoubleString2Double) == "Long:Double:String2Double"
    result &= FunctionSignature.getSignatureLongDoubleBoolean2Double(helperLongDoubleBoolean2Double) == "Long:Double:Boolean2Double"
    result &= FunctionSignature.getSignatureLongStringInt2Double(helperLongStringInt2Double) == "Long:String:Int2Double"
    result &= FunctionSignature.getSignatureLongStringLong2Double(helperLongStringLong2Double) == "Long:String:Long2Double"
    result &= FunctionSignature.getSignatureLongStringDouble2Double(helperLongStringDouble2Double) == "Long:String:Double2Double"
    result &= FunctionSignature.getSignatureLongStringString2Double(helperLongStringString2Double) == "Long:String:String2Double"
    result &= FunctionSignature.getSignatureLongStringBoolean2Double(helperLongStringBoolean2Double) == "Long:String:Boolean2Double"
    result &= FunctionSignature.getSignatureLongBooleanInt2Double(helperLongBooleanInt2Double) == "Long:Boolean:Int2Double"
    result &= FunctionSignature.getSignatureLongBooleanLong2Double(helperLongBooleanLong2Double) == "Long:Boolean:Long2Double"
    result &= FunctionSignature.getSignatureLongBooleanDouble2Double(helperLongBooleanDouble2Double) == "Long:Boolean:Double2Double"
    result &= FunctionSignature.getSignatureLongBooleanString2Double(helperLongBooleanString2Double) == "Long:Boolean:String2Double"
    result &= FunctionSignature.getSignatureLongBooleanBoolean2Double(helperLongBooleanBoolean2Double) == "Long:Boolean:Boolean2Double"
    result &= FunctionSignature.getSignatureDoubleIntInt2Double(helperDoubleIntInt2Double) == "Double:Int:Int2Double"
    result &= FunctionSignature.getSignatureDoubleIntLong2Double(helperDoubleIntLong2Double) == "Double:Int:Long2Double"
    result &= FunctionSignature.getSignatureDoubleIntDouble2Double(helperDoubleIntDouble2Double) == "Double:Int:Double2Double"
    result &= FunctionSignature.getSignatureDoubleIntString2Double(helperDoubleIntString2Double) == "Double:Int:String2Double"
    result &= FunctionSignature.getSignatureDoubleIntBoolean2Double(helperDoubleIntBoolean2Double) == "Double:Int:Boolean2Double"
    result &= FunctionSignature.getSignatureDoubleLongInt2Double(helperDoubleLongInt2Double) == "Double:Long:Int2Double"
    result &= FunctionSignature.getSignatureDoubleLongLong2Double(helperDoubleLongLong2Double) == "Double:Long:Long2Double"
    result &= FunctionSignature.getSignatureDoubleLongDouble2Double(helperDoubleLongDouble2Double) == "Double:Long:Double2Double"
    result &= FunctionSignature.getSignatureDoubleLongString2Double(helperDoubleLongString2Double) == "Double:Long:String2Double"
    result &= FunctionSignature.getSignatureDoubleLongBoolean2Double(helperDoubleLongBoolean2Double) == "Double:Long:Boolean2Double"
    result &= FunctionSignature.getSignatureDoubleDoubleInt2Double(helperDoubleDoubleInt2Double) == "Double:Double:Int2Double"
    result &= FunctionSignature.getSignatureDoubleDoubleLong2Double(helperDoubleDoubleLong2Double) == "Double:Double:Long2Double"
    result &= FunctionSignature.getSignatureDoubleDoubleDouble2Double(helperDoubleDoubleDouble2Double) == "Double:Double:Double2Double"
    result &= FunctionSignature.getSignatureDoubleDoubleString2Double(helperDoubleDoubleString2Double) == "Double:Double:String2Double"
    result &= FunctionSignature.getSignatureDoubleDoubleBoolean2Double(helperDoubleDoubleBoolean2Double) == "Double:Double:Boolean2Double"
    result &= FunctionSignature.getSignatureDoubleStringInt2Double(helperDoubleStringInt2Double) == "Double:String:Int2Double"
    result &= FunctionSignature.getSignatureDoubleStringLong2Double(helperDoubleStringLong2Double) == "Double:String:Long2Double"
    result &= FunctionSignature.getSignatureDoubleStringDouble2Double(helperDoubleStringDouble2Double) == "Double:String:Double2Double"
    result &= FunctionSignature.getSignatureDoubleStringString2Double(helperDoubleStringString2Double) == "Double:String:String2Double"
    result &= FunctionSignature.getSignatureDoubleStringBoolean2Double(helperDoubleStringBoolean2Double) == "Double:String:Boolean2Double"
    result &= FunctionSignature.getSignatureDoubleBooleanInt2Double(helperDoubleBooleanInt2Double) == "Double:Boolean:Int2Double"
    result &= FunctionSignature.getSignatureDoubleBooleanLong2Double(helperDoubleBooleanLong2Double) == "Double:Boolean:Long2Double"
    result &= FunctionSignature.getSignatureDoubleBooleanDouble2Double(helperDoubleBooleanDouble2Double) == "Double:Boolean:Double2Double"
    result &= FunctionSignature.getSignatureDoubleBooleanString2Double(helperDoubleBooleanString2Double) == "Double:Boolean:String2Double"
    result &= FunctionSignature.getSignatureDoubleBooleanBoolean2Double(helperDoubleBooleanBoolean2Double) == "Double:Boolean:Boolean2Double"
    result &= FunctionSignature.getSignatureStringIntInt2Double(helperStringIntInt2Double) == "String:Int:Int2Double"
    result &= FunctionSignature.getSignatureStringIntLong2Double(helperStringIntLong2Double) == "String:Int:Long2Double"
    result &= FunctionSignature.getSignatureStringIntDouble2Double(helperStringIntDouble2Double) == "String:Int:Double2Double"
    result &= FunctionSignature.getSignatureStringIntString2Double(helperStringIntString2Double) == "String:Int:String2Double"
    result &= FunctionSignature.getSignatureStringIntBoolean2Double(helperStringIntBoolean2Double) == "String:Int:Boolean2Double"
    result &= FunctionSignature.getSignatureStringLongInt2Double(helperStringLongInt2Double) == "String:Long:Int2Double"
    result &= FunctionSignature.getSignatureStringLongLong2Double(helperStringLongLong2Double) == "String:Long:Long2Double"
    result &= FunctionSignature.getSignatureStringLongDouble2Double(helperStringLongDouble2Double) == "String:Long:Double2Double"
    result &= FunctionSignature.getSignatureStringLongString2Double(helperStringLongString2Double) == "String:Long:String2Double"
    result &= FunctionSignature.getSignatureStringLongBoolean2Double(helperStringLongBoolean2Double) == "String:Long:Boolean2Double"
    result &= FunctionSignature.getSignatureStringDoubleInt2Double(helperStringDoubleInt2Double) == "String:Double:Int2Double"
    result &= FunctionSignature.getSignatureStringDoubleLong2Double(helperStringDoubleLong2Double) == "String:Double:Long2Double"
    result &= FunctionSignature.getSignatureStringDoubleDouble2Double(helperStringDoubleDouble2Double) == "String:Double:Double2Double"
    result &= FunctionSignature.getSignatureStringDoubleString2Double(helperStringDoubleString2Double) == "String:Double:String2Double"
    result &= FunctionSignature.getSignatureStringDoubleBoolean2Double(helperStringDoubleBoolean2Double) == "String:Double:Boolean2Double"
    result &= FunctionSignature.getSignatureStringStringInt2Double(helperStringStringInt2Double) == "String:String:Int2Double"
    result &= FunctionSignature.getSignatureStringStringLong2Double(helperStringStringLong2Double) == "String:String:Long2Double"
    result &= FunctionSignature.getSignatureStringStringDouble2Double(helperStringStringDouble2Double) == "String:String:Double2Double"
    result &= FunctionSignature.getSignatureStringStringString2Double(helperStringStringString2Double) == "String:String:String2Double"
    result &= FunctionSignature.getSignatureStringStringBoolean2Double(helperStringStringBoolean2Double) == "String:String:Boolean2Double"
    result &= FunctionSignature.getSignatureStringBooleanInt2Double(helperStringBooleanInt2Double) == "String:Boolean:Int2Double"
    result &= FunctionSignature.getSignatureStringBooleanLong2Double(helperStringBooleanLong2Double) == "String:Boolean:Long2Double"
    result &= FunctionSignature.getSignatureStringBooleanDouble2Double(helperStringBooleanDouble2Double) == "String:Boolean:Double2Double"
    result &= FunctionSignature.getSignatureStringBooleanString2Double(helperStringBooleanString2Double) == "String:Boolean:String2Double"
    result &= FunctionSignature.getSignatureStringBooleanBoolean2Double(helperStringBooleanBoolean2Double) == "String:Boolean:Boolean2Double"
    result &= FunctionSignature.getSignatureBooleanIntInt2Double(helperBooleanIntInt2Double) == "Boolean:Int:Int2Double"
    result &= FunctionSignature.getSignatureBooleanIntLong2Double(helperBooleanIntLong2Double) == "Boolean:Int:Long2Double"
    result &= FunctionSignature.getSignatureBooleanIntDouble2Double(helperBooleanIntDouble2Double) == "Boolean:Int:Double2Double"
    result &= FunctionSignature.getSignatureBooleanIntString2Double(helperBooleanIntString2Double) == "Boolean:Int:String2Double"
    result &= FunctionSignature.getSignatureBooleanIntBoolean2Double(helperBooleanIntBoolean2Double) == "Boolean:Int:Boolean2Double"
    result &= FunctionSignature.getSignatureBooleanLongInt2Double(helperBooleanLongInt2Double) == "Boolean:Long:Int2Double"
    result &= FunctionSignature.getSignatureBooleanLongLong2Double(helperBooleanLongLong2Double) == "Boolean:Long:Long2Double"
    result &= FunctionSignature.getSignatureBooleanLongDouble2Double(helperBooleanLongDouble2Double) == "Boolean:Long:Double2Double"
    result &= FunctionSignature.getSignatureBooleanLongString2Double(helperBooleanLongString2Double) == "Boolean:Long:String2Double"
    result &= FunctionSignature.getSignatureBooleanLongBoolean2Double(helperBooleanLongBoolean2Double) == "Boolean:Long:Boolean2Double"
    result &= FunctionSignature.getSignatureBooleanDoubleInt2Double(helperBooleanDoubleInt2Double) == "Boolean:Double:Int2Double"
    result &= FunctionSignature.getSignatureBooleanDoubleLong2Double(helperBooleanDoubleLong2Double) == "Boolean:Double:Long2Double"
    result &= FunctionSignature.getSignatureBooleanDoubleDouble2Double(helperBooleanDoubleDouble2Double) == "Boolean:Double:Double2Double"
    result &= FunctionSignature.getSignatureBooleanDoubleString2Double(helperBooleanDoubleString2Double) == "Boolean:Double:String2Double"
    result &= FunctionSignature.getSignatureBooleanDoubleBoolean2Double(helperBooleanDoubleBoolean2Double) == "Boolean:Double:Boolean2Double"
    result &= FunctionSignature.getSignatureBooleanStringInt2Double(helperBooleanStringInt2Double) == "Boolean:String:Int2Double"
    result &= FunctionSignature.getSignatureBooleanStringLong2Double(helperBooleanStringLong2Double) == "Boolean:String:Long2Double"
    result &= FunctionSignature.getSignatureBooleanStringDouble2Double(helperBooleanStringDouble2Double) == "Boolean:String:Double2Double"
    result &= FunctionSignature.getSignatureBooleanStringString2Double(helperBooleanStringString2Double) == "Boolean:String:String2Double"
    result &= FunctionSignature.getSignatureBooleanStringBoolean2Double(helperBooleanStringBoolean2Double) == "Boolean:String:Boolean2Double"
    result &= FunctionSignature.getSignatureBooleanBooleanInt2Double(helperBooleanBooleanInt2Double) == "Boolean:Boolean:Int2Double"
    result &= FunctionSignature.getSignatureBooleanBooleanLong2Double(helperBooleanBooleanLong2Double) == "Boolean:Boolean:Long2Double"
    result &= FunctionSignature.getSignatureBooleanBooleanDouble2Double(helperBooleanBooleanDouble2Double) == "Boolean:Boolean:Double2Double"
    result &= FunctionSignature.getSignatureBooleanBooleanString2Double(helperBooleanBooleanString2Double) == "Boolean:Boolean:String2Double"
    result &= FunctionSignature.getSignatureBooleanBooleanBoolean2Double(helperBooleanBooleanBoolean2Double) == "Boolean:Boolean:Boolean2Double"
    assert(result)
  }
  def helperIntIntInt2String(a1: iFun, a2: iFun, a3: iFun): sFun = sFunDummy
  def helperIntIntLong2String(a1: iFun, a2: iFun, a3: lFun): sFun = sFunDummy
  def helperIntIntDouble2String(a1: iFun, a2: iFun, a3: dFun): sFun = sFunDummy
  def helperIntIntString2String(a1: iFun, a2: iFun, a3: sFun): sFun = sFunDummy
  def helperIntIntBoolean2String(a1: iFun, a2: iFun, a3: bFun): sFun = sFunDummy
  def helperIntLongInt2String(a1: iFun, a2: lFun, a3: iFun): sFun = sFunDummy
  def helperIntLongLong2String(a1: iFun, a2: lFun, a3: lFun): sFun = sFunDummy
  def helperIntLongDouble2String(a1: iFun, a2: lFun, a3: dFun): sFun = sFunDummy
  def helperIntLongString2String(a1: iFun, a2: lFun, a3: sFun): sFun = sFunDummy
  def helperIntLongBoolean2String(a1: iFun, a2: lFun, a3: bFun): sFun = sFunDummy
  def helperIntDoubleInt2String(a1: iFun, a2: dFun, a3: iFun): sFun = sFunDummy
  def helperIntDoubleLong2String(a1: iFun, a2: dFun, a3: lFun): sFun = sFunDummy
  def helperIntDoubleDouble2String(a1: iFun, a2: dFun, a3: dFun): sFun = sFunDummy
  def helperIntDoubleString2String(a1: iFun, a2: dFun, a3: sFun): sFun = sFunDummy
  def helperIntDoubleBoolean2String(a1: iFun, a2: dFun, a3: bFun): sFun = sFunDummy
  def helperIntStringInt2String(a1: iFun, a2: sFun, a3: iFun): sFun = sFunDummy
  def helperIntStringLong2String(a1: iFun, a2: sFun, a3: lFun): sFun = sFunDummy
  def helperIntStringDouble2String(a1: iFun, a2: sFun, a3: dFun): sFun = sFunDummy
  def helperIntStringString2String(a1: iFun, a2: sFun, a3: sFun): sFun = sFunDummy
  def helperIntStringBoolean2String(a1: iFun, a2: sFun, a3: bFun): sFun = sFunDummy
  def helperIntBooleanInt2String(a1: iFun, a2: bFun, a3: iFun): sFun = sFunDummy
  def helperIntBooleanLong2String(a1: iFun, a2: bFun, a3: lFun): sFun = sFunDummy
  def helperIntBooleanDouble2String(a1: iFun, a2: bFun, a3: dFun): sFun = sFunDummy
  def helperIntBooleanString2String(a1: iFun, a2: bFun, a3: sFun): sFun = sFunDummy
  def helperIntBooleanBoolean2String(a1: iFun, a2: bFun, a3: bFun): sFun = sFunDummy
  def helperLongIntInt2String(a1: lFun, a2: iFun, a3: iFun): sFun = sFunDummy
  def helperLongIntLong2String(a1: lFun, a2: iFun, a3: lFun): sFun = sFunDummy
  def helperLongIntDouble2String(a1: lFun, a2: iFun, a3: dFun): sFun = sFunDummy
  def helperLongIntString2String(a1: lFun, a2: iFun, a3: sFun): sFun = sFunDummy
  def helperLongIntBoolean2String(a1: lFun, a2: iFun, a3: bFun): sFun = sFunDummy
  def helperLongLongInt2String(a1: lFun, a2: lFun, a3: iFun): sFun = sFunDummy
  def helperLongLongLong2String(a1: lFun, a2: lFun, a3: lFun): sFun = sFunDummy
  def helperLongLongDouble2String(a1: lFun, a2: lFun, a3: dFun): sFun = sFunDummy
  def helperLongLongString2String(a1: lFun, a2: lFun, a3: sFun): sFun = sFunDummy
  def helperLongLongBoolean2String(a1: lFun, a2: lFun, a3: bFun): sFun = sFunDummy
  def helperLongDoubleInt2String(a1: lFun, a2: dFun, a3: iFun): sFun = sFunDummy
  def helperLongDoubleLong2String(a1: lFun, a2: dFun, a3: lFun): sFun = sFunDummy
  def helperLongDoubleDouble2String(a1: lFun, a2: dFun, a3: dFun): sFun = sFunDummy
  def helperLongDoubleString2String(a1: lFun, a2: dFun, a3: sFun): sFun = sFunDummy
  def helperLongDoubleBoolean2String(a1: lFun, a2: dFun, a3: bFun): sFun = sFunDummy
  def helperLongStringInt2String(a1: lFun, a2: sFun, a3: iFun): sFun = sFunDummy
  def helperLongStringLong2String(a1: lFun, a2: sFun, a3: lFun): sFun = sFunDummy
  def helperLongStringDouble2String(a1: lFun, a2: sFun, a3: dFun): sFun = sFunDummy
  def helperLongStringString2String(a1: lFun, a2: sFun, a3: sFun): sFun = sFunDummy
  def helperLongStringBoolean2String(a1: lFun, a2: sFun, a3: bFun): sFun = sFunDummy
  def helperLongBooleanInt2String(a1: lFun, a2: bFun, a3: iFun): sFun = sFunDummy
  def helperLongBooleanLong2String(a1: lFun, a2: bFun, a3: lFun): sFun = sFunDummy
  def helperLongBooleanDouble2String(a1: lFun, a2: bFun, a3: dFun): sFun = sFunDummy
  def helperLongBooleanString2String(a1: lFun, a2: bFun, a3: sFun): sFun = sFunDummy
  def helperLongBooleanBoolean2String(a1: lFun, a2: bFun, a3: bFun): sFun = sFunDummy
  def helperDoubleIntInt2String(a1: dFun, a2: iFun, a3: iFun): sFun = sFunDummy
  def helperDoubleIntLong2String(a1: dFun, a2: iFun, a3: lFun): sFun = sFunDummy
  def helperDoubleIntDouble2String(a1: dFun, a2: iFun, a3: dFun): sFun = sFunDummy
  def helperDoubleIntString2String(a1: dFun, a2: iFun, a3: sFun): sFun = sFunDummy
  def helperDoubleIntBoolean2String(a1: dFun, a2: iFun, a3: bFun): sFun = sFunDummy
  def helperDoubleLongInt2String(a1: dFun, a2: lFun, a3: iFun): sFun = sFunDummy
  def helperDoubleLongLong2String(a1: dFun, a2: lFun, a3: lFun): sFun = sFunDummy
  def helperDoubleLongDouble2String(a1: dFun, a2: lFun, a3: dFun): sFun = sFunDummy
  def helperDoubleLongString2String(a1: dFun, a2: lFun, a3: sFun): sFun = sFunDummy
  def helperDoubleLongBoolean2String(a1: dFun, a2: lFun, a3: bFun): sFun = sFunDummy
  def helperDoubleDoubleInt2String(a1: dFun, a2: dFun, a3: iFun): sFun = sFunDummy
  def helperDoubleDoubleLong2String(a1: dFun, a2: dFun, a3: lFun): sFun = sFunDummy
  def helperDoubleDoubleDouble2String(a1: dFun, a2: dFun, a3: dFun): sFun = sFunDummy
  def helperDoubleDoubleString2String(a1: dFun, a2: dFun, a3: sFun): sFun = sFunDummy
  def helperDoubleDoubleBoolean2String(a1: dFun, a2: dFun, a3: bFun): sFun = sFunDummy
  def helperDoubleStringInt2String(a1: dFun, a2: sFun, a3: iFun): sFun = sFunDummy
  def helperDoubleStringLong2String(a1: dFun, a2: sFun, a3: lFun): sFun = sFunDummy
  def helperDoubleStringDouble2String(a1: dFun, a2: sFun, a3: dFun): sFun = sFunDummy
  def helperDoubleStringString2String(a1: dFun, a2: sFun, a3: sFun): sFun = sFunDummy
  def helperDoubleStringBoolean2String(a1: dFun, a2: sFun, a3: bFun): sFun = sFunDummy
  def helperDoubleBooleanInt2String(a1: dFun, a2: bFun, a3: iFun): sFun = sFunDummy
  def helperDoubleBooleanLong2String(a1: dFun, a2: bFun, a3: lFun): sFun = sFunDummy
  def helperDoubleBooleanDouble2String(a1: dFun, a2: bFun, a3: dFun): sFun = sFunDummy
  def helperDoubleBooleanString2String(a1: dFun, a2: bFun, a3: sFun): sFun = sFunDummy
  def helperDoubleBooleanBoolean2String(a1: dFun, a2: bFun, a3: bFun): sFun = sFunDummy
  def helperStringIntInt2String(a1: sFun, a2: iFun, a3: iFun): sFun = sFunDummy
  def helperStringIntLong2String(a1: sFun, a2: iFun, a3: lFun): sFun = sFunDummy
  def helperStringIntDouble2String(a1: sFun, a2: iFun, a3: dFun): sFun = sFunDummy
  def helperStringIntString2String(a1: sFun, a2: iFun, a3: sFun): sFun = sFunDummy
  def helperStringIntBoolean2String(a1: sFun, a2: iFun, a3: bFun): sFun = sFunDummy
  def helperStringLongInt2String(a1: sFun, a2: lFun, a3: iFun): sFun = sFunDummy
  def helperStringLongLong2String(a1: sFun, a2: lFun, a3: lFun): sFun = sFunDummy
  def helperStringLongDouble2String(a1: sFun, a2: lFun, a3: dFun): sFun = sFunDummy
  def helperStringLongString2String(a1: sFun, a2: lFun, a3: sFun): sFun = sFunDummy
  def helperStringLongBoolean2String(a1: sFun, a2: lFun, a3: bFun): sFun = sFunDummy
  def helperStringDoubleInt2String(a1: sFun, a2: dFun, a3: iFun): sFun = sFunDummy
  def helperStringDoubleLong2String(a1: sFun, a2: dFun, a3: lFun): sFun = sFunDummy
  def helperStringDoubleDouble2String(a1: sFun, a2: dFun, a3: dFun): sFun = sFunDummy
  def helperStringDoubleString2String(a1: sFun, a2: dFun, a3: sFun): sFun = sFunDummy
  def helperStringDoubleBoolean2String(a1: sFun, a2: dFun, a3: bFun): sFun = sFunDummy
  def helperStringStringInt2String(a1: sFun, a2: sFun, a3: iFun): sFun = sFunDummy
  def helperStringStringLong2String(a1: sFun, a2: sFun, a3: lFun): sFun = sFunDummy
  def helperStringStringDouble2String(a1: sFun, a2: sFun, a3: dFun): sFun = sFunDummy
  def helperStringStringString2String(a1: sFun, a2: sFun, a3: sFun): sFun = sFunDummy
  def helperStringStringBoolean2String(a1: sFun, a2: sFun, a3: bFun): sFun = sFunDummy
  def helperStringBooleanInt2String(a1: sFun, a2: bFun, a3: iFun): sFun = sFunDummy
  def helperStringBooleanLong2String(a1: sFun, a2: bFun, a3: lFun): sFun = sFunDummy
  def helperStringBooleanDouble2String(a1: sFun, a2: bFun, a3: dFun): sFun = sFunDummy
  def helperStringBooleanString2String(a1: sFun, a2: bFun, a3: sFun): sFun = sFunDummy
  def helperStringBooleanBoolean2String(a1: sFun, a2: bFun, a3: bFun): sFun = sFunDummy
  def helperBooleanIntInt2String(a1: bFun, a2: iFun, a3: iFun): sFun = sFunDummy
  def helperBooleanIntLong2String(a1: bFun, a2: iFun, a3: lFun): sFun = sFunDummy
  def helperBooleanIntDouble2String(a1: bFun, a2: iFun, a3: dFun): sFun = sFunDummy
  def helperBooleanIntString2String(a1: bFun, a2: iFun, a3: sFun): sFun = sFunDummy
  def helperBooleanIntBoolean2String(a1: bFun, a2: iFun, a3: bFun): sFun = sFunDummy
  def helperBooleanLongInt2String(a1: bFun, a2: lFun, a3: iFun): sFun = sFunDummy
  def helperBooleanLongLong2String(a1: bFun, a2: lFun, a3: lFun): sFun = sFunDummy
  def helperBooleanLongDouble2String(a1: bFun, a2: lFun, a3: dFun): sFun = sFunDummy
  def helperBooleanLongString2String(a1: bFun, a2: lFun, a3: sFun): sFun = sFunDummy
  def helperBooleanLongBoolean2String(a1: bFun, a2: lFun, a3: bFun): sFun = sFunDummy
  def helperBooleanDoubleInt2String(a1: bFun, a2: dFun, a3: iFun): sFun = sFunDummy
  def helperBooleanDoubleLong2String(a1: bFun, a2: dFun, a3: lFun): sFun = sFunDummy
  def helperBooleanDoubleDouble2String(a1: bFun, a2: dFun, a3: dFun): sFun = sFunDummy
  def helperBooleanDoubleString2String(a1: bFun, a2: dFun, a3: sFun): sFun = sFunDummy
  def helperBooleanDoubleBoolean2String(a1: bFun, a2: dFun, a3: bFun): sFun = sFunDummy
  def helperBooleanStringInt2String(a1: bFun, a2: sFun, a3: iFun): sFun = sFunDummy
  def helperBooleanStringLong2String(a1: bFun, a2: sFun, a3: lFun): sFun = sFunDummy
  def helperBooleanStringDouble2String(a1: bFun, a2: sFun, a3: dFun): sFun = sFunDummy
  def helperBooleanStringString2String(a1: bFun, a2: sFun, a3: sFun): sFun = sFunDummy
  def helperBooleanStringBoolean2String(a1: bFun, a2: sFun, a3: bFun): sFun = sFunDummy
  def helperBooleanBooleanInt2String(a1: bFun, a2: bFun, a3: iFun): sFun = sFunDummy
  def helperBooleanBooleanLong2String(a1: bFun, a2: bFun, a3: lFun): sFun = sFunDummy
  def helperBooleanBooleanDouble2String(a1: bFun, a2: bFun, a3: dFun): sFun = sFunDummy
  def helperBooleanBooleanString2String(a1: bFun, a2: bFun, a3: sFun): sFun = sFunDummy
  def helperBooleanBooleanBoolean2String(a1: bFun, a2: bFun, a3: bFun): sFun = sFunDummy
  test("getSignature_sFun_4") {
    var result = true
    result &= FunctionSignature.getSignatureIntIntInt2String(helperIntIntInt2String) == "Int:Int:Int2String"
    result &= FunctionSignature.getSignatureIntIntLong2String(helperIntIntLong2String) == "Int:Int:Long2String"
    result &= FunctionSignature.getSignatureIntIntDouble2String(helperIntIntDouble2String) == "Int:Int:Double2String"
    result &= FunctionSignature.getSignatureIntIntString2String(helperIntIntString2String) == "Int:Int:String2String"
    result &= FunctionSignature.getSignatureIntIntBoolean2String(helperIntIntBoolean2String) == "Int:Int:Boolean2String"
    result &= FunctionSignature.getSignatureIntLongInt2String(helperIntLongInt2String) == "Int:Long:Int2String"
    result &= FunctionSignature.getSignatureIntLongLong2String(helperIntLongLong2String) == "Int:Long:Long2String"
    result &= FunctionSignature.getSignatureIntLongDouble2String(helperIntLongDouble2String) == "Int:Long:Double2String"
    result &= FunctionSignature.getSignatureIntLongString2String(helperIntLongString2String) == "Int:Long:String2String"
    result &= FunctionSignature.getSignatureIntLongBoolean2String(helperIntLongBoolean2String) == "Int:Long:Boolean2String"
    result &= FunctionSignature.getSignatureIntDoubleInt2String(helperIntDoubleInt2String) == "Int:Double:Int2String"
    result &= FunctionSignature.getSignatureIntDoubleLong2String(helperIntDoubleLong2String) == "Int:Double:Long2String"
    result &= FunctionSignature.getSignatureIntDoubleDouble2String(helperIntDoubleDouble2String) == "Int:Double:Double2String"
    result &= FunctionSignature.getSignatureIntDoubleString2String(helperIntDoubleString2String) == "Int:Double:String2String"
    result &= FunctionSignature.getSignatureIntDoubleBoolean2String(helperIntDoubleBoolean2String) == "Int:Double:Boolean2String"
    result &= FunctionSignature.getSignatureIntStringInt2String(helperIntStringInt2String) == "Int:String:Int2String"
    result &= FunctionSignature.getSignatureIntStringLong2String(helperIntStringLong2String) == "Int:String:Long2String"
    result &= FunctionSignature.getSignatureIntStringDouble2String(helperIntStringDouble2String) == "Int:String:Double2String"
    result &= FunctionSignature.getSignatureIntStringString2String(helperIntStringString2String) == "Int:String:String2String"
    result &= FunctionSignature.getSignatureIntStringBoolean2String(helperIntStringBoolean2String) == "Int:String:Boolean2String"
    result &= FunctionSignature.getSignatureIntBooleanInt2String(helperIntBooleanInt2String) == "Int:Boolean:Int2String"
    result &= FunctionSignature.getSignatureIntBooleanLong2String(helperIntBooleanLong2String) == "Int:Boolean:Long2String"
    result &= FunctionSignature.getSignatureIntBooleanDouble2String(helperIntBooleanDouble2String) == "Int:Boolean:Double2String"
    result &= FunctionSignature.getSignatureIntBooleanString2String(helperIntBooleanString2String) == "Int:Boolean:String2String"
    result &= FunctionSignature.getSignatureIntBooleanBoolean2String(helperIntBooleanBoolean2String) == "Int:Boolean:Boolean2String"
    result &= FunctionSignature.getSignatureLongIntInt2String(helperLongIntInt2String) == "Long:Int:Int2String"
    result &= FunctionSignature.getSignatureLongIntLong2String(helperLongIntLong2String) == "Long:Int:Long2String"
    result &= FunctionSignature.getSignatureLongIntDouble2String(helperLongIntDouble2String) == "Long:Int:Double2String"
    result &= FunctionSignature.getSignatureLongIntString2String(helperLongIntString2String) == "Long:Int:String2String"
    result &= FunctionSignature.getSignatureLongIntBoolean2String(helperLongIntBoolean2String) == "Long:Int:Boolean2String"
    result &= FunctionSignature.getSignatureLongLongInt2String(helperLongLongInt2String) == "Long:Long:Int2String"
    result &= FunctionSignature.getSignatureLongLongLong2String(helperLongLongLong2String) == "Long:Long:Long2String"
    result &= FunctionSignature.getSignatureLongLongDouble2String(helperLongLongDouble2String) == "Long:Long:Double2String"
    result &= FunctionSignature.getSignatureLongLongString2String(helperLongLongString2String) == "Long:Long:String2String"
    result &= FunctionSignature.getSignatureLongLongBoolean2String(helperLongLongBoolean2String) == "Long:Long:Boolean2String"
    result &= FunctionSignature.getSignatureLongDoubleInt2String(helperLongDoubleInt2String) == "Long:Double:Int2String"
    result &= FunctionSignature.getSignatureLongDoubleLong2String(helperLongDoubleLong2String) == "Long:Double:Long2String"
    result &= FunctionSignature.getSignatureLongDoubleDouble2String(helperLongDoubleDouble2String) == "Long:Double:Double2String"
    result &= FunctionSignature.getSignatureLongDoubleString2String(helperLongDoubleString2String) == "Long:Double:String2String"
    result &= FunctionSignature.getSignatureLongDoubleBoolean2String(helperLongDoubleBoolean2String) == "Long:Double:Boolean2String"
    result &= FunctionSignature.getSignatureLongStringInt2String(helperLongStringInt2String) == "Long:String:Int2String"
    result &= FunctionSignature.getSignatureLongStringLong2String(helperLongStringLong2String) == "Long:String:Long2String"
    result &= FunctionSignature.getSignatureLongStringDouble2String(helperLongStringDouble2String) == "Long:String:Double2String"
    result &= FunctionSignature.getSignatureLongStringString2String(helperLongStringString2String) == "Long:String:String2String"
    result &= FunctionSignature.getSignatureLongStringBoolean2String(helperLongStringBoolean2String) == "Long:String:Boolean2String"
    result &= FunctionSignature.getSignatureLongBooleanInt2String(helperLongBooleanInt2String) == "Long:Boolean:Int2String"
    result &= FunctionSignature.getSignatureLongBooleanLong2String(helperLongBooleanLong2String) == "Long:Boolean:Long2String"
    result &= FunctionSignature.getSignatureLongBooleanDouble2String(helperLongBooleanDouble2String) == "Long:Boolean:Double2String"
    result &= FunctionSignature.getSignatureLongBooleanString2String(helperLongBooleanString2String) == "Long:Boolean:String2String"
    result &= FunctionSignature.getSignatureLongBooleanBoolean2String(helperLongBooleanBoolean2String) == "Long:Boolean:Boolean2String"
    result &= FunctionSignature.getSignatureDoubleIntInt2String(helperDoubleIntInt2String) == "Double:Int:Int2String"
    result &= FunctionSignature.getSignatureDoubleIntLong2String(helperDoubleIntLong2String) == "Double:Int:Long2String"
    result &= FunctionSignature.getSignatureDoubleIntDouble2String(helperDoubleIntDouble2String) == "Double:Int:Double2String"
    result &= FunctionSignature.getSignatureDoubleIntString2String(helperDoubleIntString2String) == "Double:Int:String2String"
    result &= FunctionSignature.getSignatureDoubleIntBoolean2String(helperDoubleIntBoolean2String) == "Double:Int:Boolean2String"
    result &= FunctionSignature.getSignatureDoubleLongInt2String(helperDoubleLongInt2String) == "Double:Long:Int2String"
    result &= FunctionSignature.getSignatureDoubleLongLong2String(helperDoubleLongLong2String) == "Double:Long:Long2String"
    result &= FunctionSignature.getSignatureDoubleLongDouble2String(helperDoubleLongDouble2String) == "Double:Long:Double2String"
    result &= FunctionSignature.getSignatureDoubleLongString2String(helperDoubleLongString2String) == "Double:Long:String2String"
    result &= FunctionSignature.getSignatureDoubleLongBoolean2String(helperDoubleLongBoolean2String) == "Double:Long:Boolean2String"
    result &= FunctionSignature.getSignatureDoubleDoubleInt2String(helperDoubleDoubleInt2String) == "Double:Double:Int2String"
    result &= FunctionSignature.getSignatureDoubleDoubleLong2String(helperDoubleDoubleLong2String) == "Double:Double:Long2String"
    result &= FunctionSignature.getSignatureDoubleDoubleDouble2String(helperDoubleDoubleDouble2String) == "Double:Double:Double2String"
    result &= FunctionSignature.getSignatureDoubleDoubleString2String(helperDoubleDoubleString2String) == "Double:Double:String2String"
    result &= FunctionSignature.getSignatureDoubleDoubleBoolean2String(helperDoubleDoubleBoolean2String) == "Double:Double:Boolean2String"
    result &= FunctionSignature.getSignatureDoubleStringInt2String(helperDoubleStringInt2String) == "Double:String:Int2String"
    result &= FunctionSignature.getSignatureDoubleStringLong2String(helperDoubleStringLong2String) == "Double:String:Long2String"
    result &= FunctionSignature.getSignatureDoubleStringDouble2String(helperDoubleStringDouble2String) == "Double:String:Double2String"
    result &= FunctionSignature.getSignatureDoubleStringString2String(helperDoubleStringString2String) == "Double:String:String2String"
    result &= FunctionSignature.getSignatureDoubleStringBoolean2String(helperDoubleStringBoolean2String) == "Double:String:Boolean2String"
    result &= FunctionSignature.getSignatureDoubleBooleanInt2String(helperDoubleBooleanInt2String) == "Double:Boolean:Int2String"
    result &= FunctionSignature.getSignatureDoubleBooleanLong2String(helperDoubleBooleanLong2String) == "Double:Boolean:Long2String"
    result &= FunctionSignature.getSignatureDoubleBooleanDouble2String(helperDoubleBooleanDouble2String) == "Double:Boolean:Double2String"
    result &= FunctionSignature.getSignatureDoubleBooleanString2String(helperDoubleBooleanString2String) == "Double:Boolean:String2String"
    result &= FunctionSignature.getSignatureDoubleBooleanBoolean2String(helperDoubleBooleanBoolean2String) == "Double:Boolean:Boolean2String"
    result &= FunctionSignature.getSignatureStringIntInt2String(helperStringIntInt2String) == "String:Int:Int2String"
    result &= FunctionSignature.getSignatureStringIntLong2String(helperStringIntLong2String) == "String:Int:Long2String"
    result &= FunctionSignature.getSignatureStringIntDouble2String(helperStringIntDouble2String) == "String:Int:Double2String"
    result &= FunctionSignature.getSignatureStringIntString2String(helperStringIntString2String) == "String:Int:String2String"
    result &= FunctionSignature.getSignatureStringIntBoolean2String(helperStringIntBoolean2String) == "String:Int:Boolean2String"
    result &= FunctionSignature.getSignatureStringLongInt2String(helperStringLongInt2String) == "String:Long:Int2String"
    result &= FunctionSignature.getSignatureStringLongLong2String(helperStringLongLong2String) == "String:Long:Long2String"
    result &= FunctionSignature.getSignatureStringLongDouble2String(helperStringLongDouble2String) == "String:Long:Double2String"
    result &= FunctionSignature.getSignatureStringLongString2String(helperStringLongString2String) == "String:Long:String2String"
    result &= FunctionSignature.getSignatureStringLongBoolean2String(helperStringLongBoolean2String) == "String:Long:Boolean2String"
    result &= FunctionSignature.getSignatureStringDoubleInt2String(helperStringDoubleInt2String) == "String:Double:Int2String"
    result &= FunctionSignature.getSignatureStringDoubleLong2String(helperStringDoubleLong2String) == "String:Double:Long2String"
    result &= FunctionSignature.getSignatureStringDoubleDouble2String(helperStringDoubleDouble2String) == "String:Double:Double2String"
    result &= FunctionSignature.getSignatureStringDoubleString2String(helperStringDoubleString2String) == "String:Double:String2String"
    result &= FunctionSignature.getSignatureStringDoubleBoolean2String(helperStringDoubleBoolean2String) == "String:Double:Boolean2String"
    result &= FunctionSignature.getSignatureStringStringInt2String(helperStringStringInt2String) == "String:String:Int2String"
    result &= FunctionSignature.getSignatureStringStringLong2String(helperStringStringLong2String) == "String:String:Long2String"
    result &= FunctionSignature.getSignatureStringStringDouble2String(helperStringStringDouble2String) == "String:String:Double2String"
    result &= FunctionSignature.getSignatureStringStringString2String(helperStringStringString2String) == "String:String:String2String"
    result &= FunctionSignature.getSignatureStringStringBoolean2String(helperStringStringBoolean2String) == "String:String:Boolean2String"
    result &= FunctionSignature.getSignatureStringBooleanInt2String(helperStringBooleanInt2String) == "String:Boolean:Int2String"
    result &= FunctionSignature.getSignatureStringBooleanLong2String(helperStringBooleanLong2String) == "String:Boolean:Long2String"
    result &= FunctionSignature.getSignatureStringBooleanDouble2String(helperStringBooleanDouble2String) == "String:Boolean:Double2String"
    result &= FunctionSignature.getSignatureStringBooleanString2String(helperStringBooleanString2String) == "String:Boolean:String2String"
    result &= FunctionSignature.getSignatureStringBooleanBoolean2String(helperStringBooleanBoolean2String) == "String:Boolean:Boolean2String"
    result &= FunctionSignature.getSignatureBooleanIntInt2String(helperBooleanIntInt2String) == "Boolean:Int:Int2String"
    result &= FunctionSignature.getSignatureBooleanIntLong2String(helperBooleanIntLong2String) == "Boolean:Int:Long2String"
    result &= FunctionSignature.getSignatureBooleanIntDouble2String(helperBooleanIntDouble2String) == "Boolean:Int:Double2String"
    result &= FunctionSignature.getSignatureBooleanIntString2String(helperBooleanIntString2String) == "Boolean:Int:String2String"
    result &= FunctionSignature.getSignatureBooleanIntBoolean2String(helperBooleanIntBoolean2String) == "Boolean:Int:Boolean2String"
    result &= FunctionSignature.getSignatureBooleanLongInt2String(helperBooleanLongInt2String) == "Boolean:Long:Int2String"
    result &= FunctionSignature.getSignatureBooleanLongLong2String(helperBooleanLongLong2String) == "Boolean:Long:Long2String"
    result &= FunctionSignature.getSignatureBooleanLongDouble2String(helperBooleanLongDouble2String) == "Boolean:Long:Double2String"
    result &= FunctionSignature.getSignatureBooleanLongString2String(helperBooleanLongString2String) == "Boolean:Long:String2String"
    result &= FunctionSignature.getSignatureBooleanLongBoolean2String(helperBooleanLongBoolean2String) == "Boolean:Long:Boolean2String"
    result &= FunctionSignature.getSignatureBooleanDoubleInt2String(helperBooleanDoubleInt2String) == "Boolean:Double:Int2String"
    result &= FunctionSignature.getSignatureBooleanDoubleLong2String(helperBooleanDoubleLong2String) == "Boolean:Double:Long2String"
    result &= FunctionSignature.getSignatureBooleanDoubleDouble2String(helperBooleanDoubleDouble2String) == "Boolean:Double:Double2String"
    result &= FunctionSignature.getSignatureBooleanDoubleString2String(helperBooleanDoubleString2String) == "Boolean:Double:String2String"
    result &= FunctionSignature.getSignatureBooleanDoubleBoolean2String(helperBooleanDoubleBoolean2String) == "Boolean:Double:Boolean2String"
    result &= FunctionSignature.getSignatureBooleanStringInt2String(helperBooleanStringInt2String) == "Boolean:String:Int2String"
    result &= FunctionSignature.getSignatureBooleanStringLong2String(helperBooleanStringLong2String) == "Boolean:String:Long2String"
    result &= FunctionSignature.getSignatureBooleanStringDouble2String(helperBooleanStringDouble2String) == "Boolean:String:Double2String"
    result &= FunctionSignature.getSignatureBooleanStringString2String(helperBooleanStringString2String) == "Boolean:String:String2String"
    result &= FunctionSignature.getSignatureBooleanStringBoolean2String(helperBooleanStringBoolean2String) == "Boolean:String:Boolean2String"
    result &= FunctionSignature.getSignatureBooleanBooleanInt2String(helperBooleanBooleanInt2String) == "Boolean:Boolean:Int2String"
    result &= FunctionSignature.getSignatureBooleanBooleanLong2String(helperBooleanBooleanLong2String) == "Boolean:Boolean:Long2String"
    result &= FunctionSignature.getSignatureBooleanBooleanDouble2String(helperBooleanBooleanDouble2String) == "Boolean:Boolean:Double2String"
    result &= FunctionSignature.getSignatureBooleanBooleanString2String(helperBooleanBooleanString2String) == "Boolean:Boolean:String2String"
    result &= FunctionSignature.getSignatureBooleanBooleanBoolean2String(helperBooleanBooleanBoolean2String) == "Boolean:Boolean:Boolean2String"
    assert(result)
  }
  def helperIntIntInt2Boolean(a1: iFun, a2: iFun, a3: iFun): bFun = bFunDummy
  def helperIntIntLong2Boolean(a1: iFun, a2: iFun, a3: lFun): bFun = bFunDummy
  def helperIntIntDouble2Boolean(a1: iFun, a2: iFun, a3: dFun): bFun = bFunDummy
  def helperIntIntString2Boolean(a1: iFun, a2: iFun, a3: sFun): bFun = bFunDummy
  def helperIntIntBoolean2Boolean(a1: iFun, a2: iFun, a3: bFun): bFun = bFunDummy
  def helperIntLongInt2Boolean(a1: iFun, a2: lFun, a3: iFun): bFun = bFunDummy
  def helperIntLongLong2Boolean(a1: iFun, a2: lFun, a3: lFun): bFun = bFunDummy
  def helperIntLongDouble2Boolean(a1: iFun, a2: lFun, a3: dFun): bFun = bFunDummy
  def helperIntLongString2Boolean(a1: iFun, a2: lFun, a3: sFun): bFun = bFunDummy
  def helperIntLongBoolean2Boolean(a1: iFun, a2: lFun, a3: bFun): bFun = bFunDummy
  def helperIntDoubleInt2Boolean(a1: iFun, a2: dFun, a3: iFun): bFun = bFunDummy
  def helperIntDoubleLong2Boolean(a1: iFun, a2: dFun, a3: lFun): bFun = bFunDummy
  def helperIntDoubleDouble2Boolean(a1: iFun, a2: dFun, a3: dFun): bFun = bFunDummy
  def helperIntDoubleString2Boolean(a1: iFun, a2: dFun, a3: sFun): bFun = bFunDummy
  def helperIntDoubleBoolean2Boolean(a1: iFun, a2: dFun, a3: bFun): bFun = bFunDummy
  def helperIntStringInt2Boolean(a1: iFun, a2: sFun, a3: iFun): bFun = bFunDummy
  def helperIntStringLong2Boolean(a1: iFun, a2: sFun, a3: lFun): bFun = bFunDummy
  def helperIntStringDouble2Boolean(a1: iFun, a2: sFun, a3: dFun): bFun = bFunDummy
  def helperIntStringString2Boolean(a1: iFun, a2: sFun, a3: sFun): bFun = bFunDummy
  def helperIntStringBoolean2Boolean(a1: iFun, a2: sFun, a3: bFun): bFun = bFunDummy
  def helperIntBooleanInt2Boolean(a1: iFun, a2: bFun, a3: iFun): bFun = bFunDummy
  def helperIntBooleanLong2Boolean(a1: iFun, a2: bFun, a3: lFun): bFun = bFunDummy
  def helperIntBooleanDouble2Boolean(a1: iFun, a2: bFun, a3: dFun): bFun = bFunDummy
  def helperIntBooleanString2Boolean(a1: iFun, a2: bFun, a3: sFun): bFun = bFunDummy
  def helperIntBooleanBoolean2Boolean(a1: iFun, a2: bFun, a3: bFun): bFun = bFunDummy
  def helperLongIntInt2Boolean(a1: lFun, a2: iFun, a3: iFun): bFun = bFunDummy
  def helperLongIntLong2Boolean(a1: lFun, a2: iFun, a3: lFun): bFun = bFunDummy
  def helperLongIntDouble2Boolean(a1: lFun, a2: iFun, a3: dFun): bFun = bFunDummy
  def helperLongIntString2Boolean(a1: lFun, a2: iFun, a3: sFun): bFun = bFunDummy
  def helperLongIntBoolean2Boolean(a1: lFun, a2: iFun, a3: bFun): bFun = bFunDummy
  def helperLongLongInt2Boolean(a1: lFun, a2: lFun, a3: iFun): bFun = bFunDummy
  def helperLongLongLong2Boolean(a1: lFun, a2: lFun, a3: lFun): bFun = bFunDummy
  def helperLongLongDouble2Boolean(a1: lFun, a2: lFun, a3: dFun): bFun = bFunDummy
  def helperLongLongString2Boolean(a1: lFun, a2: lFun, a3: sFun): bFun = bFunDummy
  def helperLongLongBoolean2Boolean(a1: lFun, a2: lFun, a3: bFun): bFun = bFunDummy
  def helperLongDoubleInt2Boolean(a1: lFun, a2: dFun, a3: iFun): bFun = bFunDummy
  def helperLongDoubleLong2Boolean(a1: lFun, a2: dFun, a3: lFun): bFun = bFunDummy
  def helperLongDoubleDouble2Boolean(a1: lFun, a2: dFun, a3: dFun): bFun = bFunDummy
  def helperLongDoubleString2Boolean(a1: lFun, a2: dFun, a3: sFun): bFun = bFunDummy
  def helperLongDoubleBoolean2Boolean(a1: lFun, a2: dFun, a3: bFun): bFun = bFunDummy
  def helperLongStringInt2Boolean(a1: lFun, a2: sFun, a3: iFun): bFun = bFunDummy
  def helperLongStringLong2Boolean(a1: lFun, a2: sFun, a3: lFun): bFun = bFunDummy
  def helperLongStringDouble2Boolean(a1: lFun, a2: sFun, a3: dFun): bFun = bFunDummy
  def helperLongStringString2Boolean(a1: lFun, a2: sFun, a3: sFun): bFun = bFunDummy
  def helperLongStringBoolean2Boolean(a1: lFun, a2: sFun, a3: bFun): bFun = bFunDummy
  def helperLongBooleanInt2Boolean(a1: lFun, a2: bFun, a3: iFun): bFun = bFunDummy
  def helperLongBooleanLong2Boolean(a1: lFun, a2: bFun, a3: lFun): bFun = bFunDummy
  def helperLongBooleanDouble2Boolean(a1: lFun, a2: bFun, a3: dFun): bFun = bFunDummy
  def helperLongBooleanString2Boolean(a1: lFun, a2: bFun, a3: sFun): bFun = bFunDummy
  def helperLongBooleanBoolean2Boolean(a1: lFun, a2: bFun, a3: bFun): bFun = bFunDummy
  def helperDoubleIntInt2Boolean(a1: dFun, a2: iFun, a3: iFun): bFun = bFunDummy
  def helperDoubleIntLong2Boolean(a1: dFun, a2: iFun, a3: lFun): bFun = bFunDummy
  def helperDoubleIntDouble2Boolean(a1: dFun, a2: iFun, a3: dFun): bFun = bFunDummy
  def helperDoubleIntString2Boolean(a1: dFun, a2: iFun, a3: sFun): bFun = bFunDummy
  def helperDoubleIntBoolean2Boolean(a1: dFun, a2: iFun, a3: bFun): bFun = bFunDummy
  def helperDoubleLongInt2Boolean(a1: dFun, a2: lFun, a3: iFun): bFun = bFunDummy
  def helperDoubleLongLong2Boolean(a1: dFun, a2: lFun, a3: lFun): bFun = bFunDummy
  def helperDoubleLongDouble2Boolean(a1: dFun, a2: lFun, a3: dFun): bFun = bFunDummy
  def helperDoubleLongString2Boolean(a1: dFun, a2: lFun, a3: sFun): bFun = bFunDummy
  def helperDoubleLongBoolean2Boolean(a1: dFun, a2: lFun, a3: bFun): bFun = bFunDummy
  def helperDoubleDoubleInt2Boolean(a1: dFun, a2: dFun, a3: iFun): bFun = bFunDummy
  def helperDoubleDoubleLong2Boolean(a1: dFun, a2: dFun, a3: lFun): bFun = bFunDummy
  def helperDoubleDoubleDouble2Boolean(a1: dFun, a2: dFun, a3: dFun): bFun = bFunDummy
  def helperDoubleDoubleString2Boolean(a1: dFun, a2: dFun, a3: sFun): bFun = bFunDummy
  def helperDoubleDoubleBoolean2Boolean(a1: dFun, a2: dFun, a3: bFun): bFun = bFunDummy
  def helperDoubleStringInt2Boolean(a1: dFun, a2: sFun, a3: iFun): bFun = bFunDummy
  def helperDoubleStringLong2Boolean(a1: dFun, a2: sFun, a3: lFun): bFun = bFunDummy
  def helperDoubleStringDouble2Boolean(a1: dFun, a2: sFun, a3: dFun): bFun = bFunDummy
  def helperDoubleStringString2Boolean(a1: dFun, a2: sFun, a3: sFun): bFun = bFunDummy
  def helperDoubleStringBoolean2Boolean(a1: dFun, a2: sFun, a3: bFun): bFun = bFunDummy
  def helperDoubleBooleanInt2Boolean(a1: dFun, a2: bFun, a3: iFun): bFun = bFunDummy
  def helperDoubleBooleanLong2Boolean(a1: dFun, a2: bFun, a3: lFun): bFun = bFunDummy
  def helperDoubleBooleanDouble2Boolean(a1: dFun, a2: bFun, a3: dFun): bFun = bFunDummy
  def helperDoubleBooleanString2Boolean(a1: dFun, a2: bFun, a3: sFun): bFun = bFunDummy
  def helperDoubleBooleanBoolean2Boolean(a1: dFun, a2: bFun, a3: bFun): bFun = bFunDummy
  def helperStringIntInt2Boolean(a1: sFun, a2: iFun, a3: iFun): bFun = bFunDummy
  def helperStringIntLong2Boolean(a1: sFun, a2: iFun, a3: lFun): bFun = bFunDummy
  def helperStringIntDouble2Boolean(a1: sFun, a2: iFun, a3: dFun): bFun = bFunDummy
  def helperStringIntString2Boolean(a1: sFun, a2: iFun, a3: sFun): bFun = bFunDummy
  def helperStringIntBoolean2Boolean(a1: sFun, a2: iFun, a3: bFun): bFun = bFunDummy
  def helperStringLongInt2Boolean(a1: sFun, a2: lFun, a3: iFun): bFun = bFunDummy
  def helperStringLongLong2Boolean(a1: sFun, a2: lFun, a3: lFun): bFun = bFunDummy
  def helperStringLongDouble2Boolean(a1: sFun, a2: lFun, a3: dFun): bFun = bFunDummy
  def helperStringLongString2Boolean(a1: sFun, a2: lFun, a3: sFun): bFun = bFunDummy
  def helperStringLongBoolean2Boolean(a1: sFun, a2: lFun, a3: bFun): bFun = bFunDummy
  def helperStringDoubleInt2Boolean(a1: sFun, a2: dFun, a3: iFun): bFun = bFunDummy
  def helperStringDoubleLong2Boolean(a1: sFun, a2: dFun, a3: lFun): bFun = bFunDummy
  def helperStringDoubleDouble2Boolean(a1: sFun, a2: dFun, a3: dFun): bFun = bFunDummy
  def helperStringDoubleString2Boolean(a1: sFun, a2: dFun, a3: sFun): bFun = bFunDummy
  def helperStringDoubleBoolean2Boolean(a1: sFun, a2: dFun, a3: bFun): bFun = bFunDummy
  def helperStringStringInt2Boolean(a1: sFun, a2: sFun, a3: iFun): bFun = bFunDummy
  def helperStringStringLong2Boolean(a1: sFun, a2: sFun, a3: lFun): bFun = bFunDummy
  def helperStringStringDouble2Boolean(a1: sFun, a2: sFun, a3: dFun): bFun = bFunDummy
  def helperStringStringString2Boolean(a1: sFun, a2: sFun, a3: sFun): bFun = bFunDummy
  def helperStringStringBoolean2Boolean(a1: sFun, a2: sFun, a3: bFun): bFun = bFunDummy
  def helperStringBooleanInt2Boolean(a1: sFun, a2: bFun, a3: iFun): bFun = bFunDummy
  def helperStringBooleanLong2Boolean(a1: sFun, a2: bFun, a3: lFun): bFun = bFunDummy
  def helperStringBooleanDouble2Boolean(a1: sFun, a2: bFun, a3: dFun): bFun = bFunDummy
  def helperStringBooleanString2Boolean(a1: sFun, a2: bFun, a3: sFun): bFun = bFunDummy
  def helperStringBooleanBoolean2Boolean(a1: sFun, a2: bFun, a3: bFun): bFun = bFunDummy
  def helperBooleanIntInt2Boolean(a1: bFun, a2: iFun, a3: iFun): bFun = bFunDummy
  def helperBooleanIntLong2Boolean(a1: bFun, a2: iFun, a3: lFun): bFun = bFunDummy
  def helperBooleanIntDouble2Boolean(a1: bFun, a2: iFun, a3: dFun): bFun = bFunDummy
  def helperBooleanIntString2Boolean(a1: bFun, a2: iFun, a3: sFun): bFun = bFunDummy
  def helperBooleanIntBoolean2Boolean(a1: bFun, a2: iFun, a3: bFun): bFun = bFunDummy
  def helperBooleanLongInt2Boolean(a1: bFun, a2: lFun, a3: iFun): bFun = bFunDummy
  def helperBooleanLongLong2Boolean(a1: bFun, a2: lFun, a3: lFun): bFun = bFunDummy
  def helperBooleanLongDouble2Boolean(a1: bFun, a2: lFun, a3: dFun): bFun = bFunDummy
  def helperBooleanLongString2Boolean(a1: bFun, a2: lFun, a3: sFun): bFun = bFunDummy
  def helperBooleanLongBoolean2Boolean(a1: bFun, a2: lFun, a3: bFun): bFun = bFunDummy
  def helperBooleanDoubleInt2Boolean(a1: bFun, a2: dFun, a3: iFun): bFun = bFunDummy
  def helperBooleanDoubleLong2Boolean(a1: bFun, a2: dFun, a3: lFun): bFun = bFunDummy
  def helperBooleanDoubleDouble2Boolean(a1: bFun, a2: dFun, a3: dFun): bFun = bFunDummy
  def helperBooleanDoubleString2Boolean(a1: bFun, a2: dFun, a3: sFun): bFun = bFunDummy
  def helperBooleanDoubleBoolean2Boolean(a1: bFun, a2: dFun, a3: bFun): bFun = bFunDummy
  def helperBooleanStringInt2Boolean(a1: bFun, a2: sFun, a3: iFun): bFun = bFunDummy
  def helperBooleanStringLong2Boolean(a1: bFun, a2: sFun, a3: lFun): bFun = bFunDummy
  def helperBooleanStringDouble2Boolean(a1: bFun, a2: sFun, a3: dFun): bFun = bFunDummy
  def helperBooleanStringString2Boolean(a1: bFun, a2: sFun, a3: sFun): bFun = bFunDummy
  def helperBooleanStringBoolean2Boolean(a1: bFun, a2: sFun, a3: bFun): bFun = bFunDummy
  def helperBooleanBooleanInt2Boolean(a1: bFun, a2: bFun, a3: iFun): bFun = bFunDummy
  def helperBooleanBooleanLong2Boolean(a1: bFun, a2: bFun, a3: lFun): bFun = bFunDummy
  def helperBooleanBooleanDouble2Boolean(a1: bFun, a2: bFun, a3: dFun): bFun = bFunDummy
  def helperBooleanBooleanString2Boolean(a1: bFun, a2: bFun, a3: sFun): bFun = bFunDummy
  def helperBooleanBooleanBoolean2Boolean(a1: bFun, a2: bFun, a3: bFun): bFun = bFunDummy
  test("getSignature_bFun_4") {
    var result = true
    result &= FunctionSignature.getSignatureIntIntInt2Boolean(helperIntIntInt2Boolean) == "Int:Int:Int2Boolean"
    result &= FunctionSignature.getSignatureIntIntLong2Boolean(helperIntIntLong2Boolean) == "Int:Int:Long2Boolean"
    result &= FunctionSignature.getSignatureIntIntDouble2Boolean(helperIntIntDouble2Boolean) == "Int:Int:Double2Boolean"
    result &= FunctionSignature.getSignatureIntIntString2Boolean(helperIntIntString2Boolean) == "Int:Int:String2Boolean"
    result &= FunctionSignature.getSignatureIntIntBoolean2Boolean(helperIntIntBoolean2Boolean) == "Int:Int:Boolean2Boolean"
    result &= FunctionSignature.getSignatureIntLongInt2Boolean(helperIntLongInt2Boolean) == "Int:Long:Int2Boolean"
    result &= FunctionSignature.getSignatureIntLongLong2Boolean(helperIntLongLong2Boolean) == "Int:Long:Long2Boolean"
    result &= FunctionSignature.getSignatureIntLongDouble2Boolean(helperIntLongDouble2Boolean) == "Int:Long:Double2Boolean"
    result &= FunctionSignature.getSignatureIntLongString2Boolean(helperIntLongString2Boolean) == "Int:Long:String2Boolean"
    result &= FunctionSignature.getSignatureIntLongBoolean2Boolean(helperIntLongBoolean2Boolean) == "Int:Long:Boolean2Boolean"
    result &= FunctionSignature.getSignatureIntDoubleInt2Boolean(helperIntDoubleInt2Boolean) == "Int:Double:Int2Boolean"
    result &= FunctionSignature.getSignatureIntDoubleLong2Boolean(helperIntDoubleLong2Boolean) == "Int:Double:Long2Boolean"
    result &= FunctionSignature.getSignatureIntDoubleDouble2Boolean(helperIntDoubleDouble2Boolean) == "Int:Double:Double2Boolean"
    result &= FunctionSignature.getSignatureIntDoubleString2Boolean(helperIntDoubleString2Boolean) == "Int:Double:String2Boolean"
    result &= FunctionSignature.getSignatureIntDoubleBoolean2Boolean(helperIntDoubleBoolean2Boolean) == "Int:Double:Boolean2Boolean"
    result &= FunctionSignature.getSignatureIntStringInt2Boolean(helperIntStringInt2Boolean) == "Int:String:Int2Boolean"
    result &= FunctionSignature.getSignatureIntStringLong2Boolean(helperIntStringLong2Boolean) == "Int:String:Long2Boolean"
    result &= FunctionSignature.getSignatureIntStringDouble2Boolean(helperIntStringDouble2Boolean) == "Int:String:Double2Boolean"
    result &= FunctionSignature.getSignatureIntStringString2Boolean(helperIntStringString2Boolean) == "Int:String:String2Boolean"
    result &= FunctionSignature.getSignatureIntStringBoolean2Boolean(helperIntStringBoolean2Boolean) == "Int:String:Boolean2Boolean"
    result &= FunctionSignature.getSignatureIntBooleanInt2Boolean(helperIntBooleanInt2Boolean) == "Int:Boolean:Int2Boolean"
    result &= FunctionSignature.getSignatureIntBooleanLong2Boolean(helperIntBooleanLong2Boolean) == "Int:Boolean:Long2Boolean"
    result &= FunctionSignature.getSignatureIntBooleanDouble2Boolean(helperIntBooleanDouble2Boolean) == "Int:Boolean:Double2Boolean"
    result &= FunctionSignature.getSignatureIntBooleanString2Boolean(helperIntBooleanString2Boolean) == "Int:Boolean:String2Boolean"
    result &= FunctionSignature.getSignatureIntBooleanBoolean2Boolean(helperIntBooleanBoolean2Boolean) == "Int:Boolean:Boolean2Boolean"
    result &= FunctionSignature.getSignatureLongIntInt2Boolean(helperLongIntInt2Boolean) == "Long:Int:Int2Boolean"
    result &= FunctionSignature.getSignatureLongIntLong2Boolean(helperLongIntLong2Boolean) == "Long:Int:Long2Boolean"
    result &= FunctionSignature.getSignatureLongIntDouble2Boolean(helperLongIntDouble2Boolean) == "Long:Int:Double2Boolean"
    result &= FunctionSignature.getSignatureLongIntString2Boolean(helperLongIntString2Boolean) == "Long:Int:String2Boolean"
    result &= FunctionSignature.getSignatureLongIntBoolean2Boolean(helperLongIntBoolean2Boolean) == "Long:Int:Boolean2Boolean"
    result &= FunctionSignature.getSignatureLongLongInt2Boolean(helperLongLongInt2Boolean) == "Long:Long:Int2Boolean"
    result &= FunctionSignature.getSignatureLongLongLong2Boolean(helperLongLongLong2Boolean) == "Long:Long:Long2Boolean"
    result &= FunctionSignature.getSignatureLongLongDouble2Boolean(helperLongLongDouble2Boolean) == "Long:Long:Double2Boolean"
    result &= FunctionSignature.getSignatureLongLongString2Boolean(helperLongLongString2Boolean) == "Long:Long:String2Boolean"
    result &= FunctionSignature.getSignatureLongLongBoolean2Boolean(helperLongLongBoolean2Boolean) == "Long:Long:Boolean2Boolean"
    result &= FunctionSignature.getSignatureLongDoubleInt2Boolean(helperLongDoubleInt2Boolean) == "Long:Double:Int2Boolean"
    result &= FunctionSignature.getSignatureLongDoubleLong2Boolean(helperLongDoubleLong2Boolean) == "Long:Double:Long2Boolean"
    result &= FunctionSignature.getSignatureLongDoubleDouble2Boolean(helperLongDoubleDouble2Boolean) == "Long:Double:Double2Boolean"
    result &= FunctionSignature.getSignatureLongDoubleString2Boolean(helperLongDoubleString2Boolean) == "Long:Double:String2Boolean"
    result &= FunctionSignature.getSignatureLongDoubleBoolean2Boolean(helperLongDoubleBoolean2Boolean) == "Long:Double:Boolean2Boolean"
    result &= FunctionSignature.getSignatureLongStringInt2Boolean(helperLongStringInt2Boolean) == "Long:String:Int2Boolean"
    result &= FunctionSignature.getSignatureLongStringLong2Boolean(helperLongStringLong2Boolean) == "Long:String:Long2Boolean"
    result &= FunctionSignature.getSignatureLongStringDouble2Boolean(helperLongStringDouble2Boolean) == "Long:String:Double2Boolean"
    result &= FunctionSignature.getSignatureLongStringString2Boolean(helperLongStringString2Boolean) == "Long:String:String2Boolean"
    result &= FunctionSignature.getSignatureLongStringBoolean2Boolean(helperLongStringBoolean2Boolean) == "Long:String:Boolean2Boolean"
    result &= FunctionSignature.getSignatureLongBooleanInt2Boolean(helperLongBooleanInt2Boolean) == "Long:Boolean:Int2Boolean"
    result &= FunctionSignature.getSignatureLongBooleanLong2Boolean(helperLongBooleanLong2Boolean) == "Long:Boolean:Long2Boolean"
    result &= FunctionSignature.getSignatureLongBooleanDouble2Boolean(helperLongBooleanDouble2Boolean) == "Long:Boolean:Double2Boolean"
    result &= FunctionSignature.getSignatureLongBooleanString2Boolean(helperLongBooleanString2Boolean) == "Long:Boolean:String2Boolean"
    result &= FunctionSignature.getSignatureLongBooleanBoolean2Boolean(helperLongBooleanBoolean2Boolean) == "Long:Boolean:Boolean2Boolean"
    result &= FunctionSignature.getSignatureDoubleIntInt2Boolean(helperDoubleIntInt2Boolean) == "Double:Int:Int2Boolean"
    result &= FunctionSignature.getSignatureDoubleIntLong2Boolean(helperDoubleIntLong2Boolean) == "Double:Int:Long2Boolean"
    result &= FunctionSignature.getSignatureDoubleIntDouble2Boolean(helperDoubleIntDouble2Boolean) == "Double:Int:Double2Boolean"
    result &= FunctionSignature.getSignatureDoubleIntString2Boolean(helperDoubleIntString2Boolean) == "Double:Int:String2Boolean"
    result &= FunctionSignature.getSignatureDoubleIntBoolean2Boolean(helperDoubleIntBoolean2Boolean) == "Double:Int:Boolean2Boolean"
    result &= FunctionSignature.getSignatureDoubleLongInt2Boolean(helperDoubleLongInt2Boolean) == "Double:Long:Int2Boolean"
    result &= FunctionSignature.getSignatureDoubleLongLong2Boolean(helperDoubleLongLong2Boolean) == "Double:Long:Long2Boolean"
    result &= FunctionSignature.getSignatureDoubleLongDouble2Boolean(helperDoubleLongDouble2Boolean) == "Double:Long:Double2Boolean"
    result &= FunctionSignature.getSignatureDoubleLongString2Boolean(helperDoubleLongString2Boolean) == "Double:Long:String2Boolean"
    result &= FunctionSignature.getSignatureDoubleLongBoolean2Boolean(helperDoubleLongBoolean2Boolean) == "Double:Long:Boolean2Boolean"
    result &= FunctionSignature.getSignatureDoubleDoubleInt2Boolean(helperDoubleDoubleInt2Boolean) == "Double:Double:Int2Boolean"
    result &= FunctionSignature.getSignatureDoubleDoubleLong2Boolean(helperDoubleDoubleLong2Boolean) == "Double:Double:Long2Boolean"
    result &= FunctionSignature.getSignatureDoubleDoubleDouble2Boolean(helperDoubleDoubleDouble2Boolean) == "Double:Double:Double2Boolean"
    result &= FunctionSignature.getSignatureDoubleDoubleString2Boolean(helperDoubleDoubleString2Boolean) == "Double:Double:String2Boolean"
    result &= FunctionSignature.getSignatureDoubleDoubleBoolean2Boolean(helperDoubleDoubleBoolean2Boolean) == "Double:Double:Boolean2Boolean"
    result &= FunctionSignature.getSignatureDoubleStringInt2Boolean(helperDoubleStringInt2Boolean) == "Double:String:Int2Boolean"
    result &= FunctionSignature.getSignatureDoubleStringLong2Boolean(helperDoubleStringLong2Boolean) == "Double:String:Long2Boolean"
    result &= FunctionSignature.getSignatureDoubleStringDouble2Boolean(helperDoubleStringDouble2Boolean) == "Double:String:Double2Boolean"
    result &= FunctionSignature.getSignatureDoubleStringString2Boolean(helperDoubleStringString2Boolean) == "Double:String:String2Boolean"
    result &= FunctionSignature.getSignatureDoubleStringBoolean2Boolean(helperDoubleStringBoolean2Boolean) == "Double:String:Boolean2Boolean"
    result &= FunctionSignature.getSignatureDoubleBooleanInt2Boolean(helperDoubleBooleanInt2Boolean) == "Double:Boolean:Int2Boolean"
    result &= FunctionSignature.getSignatureDoubleBooleanLong2Boolean(helperDoubleBooleanLong2Boolean) == "Double:Boolean:Long2Boolean"
    result &= FunctionSignature.getSignatureDoubleBooleanDouble2Boolean(helperDoubleBooleanDouble2Boolean) == "Double:Boolean:Double2Boolean"
    result &= FunctionSignature.getSignatureDoubleBooleanString2Boolean(helperDoubleBooleanString2Boolean) == "Double:Boolean:String2Boolean"
    result &= FunctionSignature.getSignatureDoubleBooleanBoolean2Boolean(helperDoubleBooleanBoolean2Boolean) == "Double:Boolean:Boolean2Boolean"
    result &= FunctionSignature.getSignatureStringIntInt2Boolean(helperStringIntInt2Boolean) == "String:Int:Int2Boolean"
    result &= FunctionSignature.getSignatureStringIntLong2Boolean(helperStringIntLong2Boolean) == "String:Int:Long2Boolean"
    result &= FunctionSignature.getSignatureStringIntDouble2Boolean(helperStringIntDouble2Boolean) == "String:Int:Double2Boolean"
    result &= FunctionSignature.getSignatureStringIntString2Boolean(helperStringIntString2Boolean) == "String:Int:String2Boolean"
    result &= FunctionSignature.getSignatureStringIntBoolean2Boolean(helperStringIntBoolean2Boolean) == "String:Int:Boolean2Boolean"
    result &= FunctionSignature.getSignatureStringLongInt2Boolean(helperStringLongInt2Boolean) == "String:Long:Int2Boolean"
    result &= FunctionSignature.getSignatureStringLongLong2Boolean(helperStringLongLong2Boolean) == "String:Long:Long2Boolean"
    result &= FunctionSignature.getSignatureStringLongDouble2Boolean(helperStringLongDouble2Boolean) == "String:Long:Double2Boolean"
    result &= FunctionSignature.getSignatureStringLongString2Boolean(helperStringLongString2Boolean) == "String:Long:String2Boolean"
    result &= FunctionSignature.getSignatureStringLongBoolean2Boolean(helperStringLongBoolean2Boolean) == "String:Long:Boolean2Boolean"
    result &= FunctionSignature.getSignatureStringDoubleInt2Boolean(helperStringDoubleInt2Boolean) == "String:Double:Int2Boolean"
    result &= FunctionSignature.getSignatureStringDoubleLong2Boolean(helperStringDoubleLong2Boolean) == "String:Double:Long2Boolean"
    result &= FunctionSignature.getSignatureStringDoubleDouble2Boolean(helperStringDoubleDouble2Boolean) == "String:Double:Double2Boolean"
    result &= FunctionSignature.getSignatureStringDoubleString2Boolean(helperStringDoubleString2Boolean) == "String:Double:String2Boolean"
    result &= FunctionSignature.getSignatureStringDoubleBoolean2Boolean(helperStringDoubleBoolean2Boolean) == "String:Double:Boolean2Boolean"
    result &= FunctionSignature.getSignatureStringStringInt2Boolean(helperStringStringInt2Boolean) == "String:String:Int2Boolean"
    result &= FunctionSignature.getSignatureStringStringLong2Boolean(helperStringStringLong2Boolean) == "String:String:Long2Boolean"
    result &= FunctionSignature.getSignatureStringStringDouble2Boolean(helperStringStringDouble2Boolean) == "String:String:Double2Boolean"
    result &= FunctionSignature.getSignatureStringStringString2Boolean(helperStringStringString2Boolean) == "String:String:String2Boolean"
    result &= FunctionSignature.getSignatureStringStringBoolean2Boolean(helperStringStringBoolean2Boolean) == "String:String:Boolean2Boolean"
    result &= FunctionSignature.getSignatureStringBooleanInt2Boolean(helperStringBooleanInt2Boolean) == "String:Boolean:Int2Boolean"
    result &= FunctionSignature.getSignatureStringBooleanLong2Boolean(helperStringBooleanLong2Boolean) == "String:Boolean:Long2Boolean"
    result &= FunctionSignature.getSignatureStringBooleanDouble2Boolean(helperStringBooleanDouble2Boolean) == "String:Boolean:Double2Boolean"
    result &= FunctionSignature.getSignatureStringBooleanString2Boolean(helperStringBooleanString2Boolean) == "String:Boolean:String2Boolean"
    result &= FunctionSignature.getSignatureStringBooleanBoolean2Boolean(helperStringBooleanBoolean2Boolean) == "String:Boolean:Boolean2Boolean"
    result &= FunctionSignature.getSignatureBooleanIntInt2Boolean(helperBooleanIntInt2Boolean) == "Boolean:Int:Int2Boolean"
    result &= FunctionSignature.getSignatureBooleanIntLong2Boolean(helperBooleanIntLong2Boolean) == "Boolean:Int:Long2Boolean"
    result &= FunctionSignature.getSignatureBooleanIntDouble2Boolean(helperBooleanIntDouble2Boolean) == "Boolean:Int:Double2Boolean"
    result &= FunctionSignature.getSignatureBooleanIntString2Boolean(helperBooleanIntString2Boolean) == "Boolean:Int:String2Boolean"
    result &= FunctionSignature.getSignatureBooleanIntBoolean2Boolean(helperBooleanIntBoolean2Boolean) == "Boolean:Int:Boolean2Boolean"
    result &= FunctionSignature.getSignatureBooleanLongInt2Boolean(helperBooleanLongInt2Boolean) == "Boolean:Long:Int2Boolean"
    result &= FunctionSignature.getSignatureBooleanLongLong2Boolean(helperBooleanLongLong2Boolean) == "Boolean:Long:Long2Boolean"
    result &= FunctionSignature.getSignatureBooleanLongDouble2Boolean(helperBooleanLongDouble2Boolean) == "Boolean:Long:Double2Boolean"
    result &= FunctionSignature.getSignatureBooleanLongString2Boolean(helperBooleanLongString2Boolean) == "Boolean:Long:String2Boolean"
    result &= FunctionSignature.getSignatureBooleanLongBoolean2Boolean(helperBooleanLongBoolean2Boolean) == "Boolean:Long:Boolean2Boolean"
    result &= FunctionSignature.getSignatureBooleanDoubleInt2Boolean(helperBooleanDoubleInt2Boolean) == "Boolean:Double:Int2Boolean"
    result &= FunctionSignature.getSignatureBooleanDoubleLong2Boolean(helperBooleanDoubleLong2Boolean) == "Boolean:Double:Long2Boolean"
    result &= FunctionSignature.getSignatureBooleanDoubleDouble2Boolean(helperBooleanDoubleDouble2Boolean) == "Boolean:Double:Double2Boolean"
    result &= FunctionSignature.getSignatureBooleanDoubleString2Boolean(helperBooleanDoubleString2Boolean) == "Boolean:Double:String2Boolean"
    result &= FunctionSignature.getSignatureBooleanDoubleBoolean2Boolean(helperBooleanDoubleBoolean2Boolean) == "Boolean:Double:Boolean2Boolean"
    result &= FunctionSignature.getSignatureBooleanStringInt2Boolean(helperBooleanStringInt2Boolean) == "Boolean:String:Int2Boolean"
    result &= FunctionSignature.getSignatureBooleanStringLong2Boolean(helperBooleanStringLong2Boolean) == "Boolean:String:Long2Boolean"
    result &= FunctionSignature.getSignatureBooleanStringDouble2Boolean(helperBooleanStringDouble2Boolean) == "Boolean:String:Double2Boolean"
    result &= FunctionSignature.getSignatureBooleanStringString2Boolean(helperBooleanStringString2Boolean) == "Boolean:String:String2Boolean"
    result &= FunctionSignature.getSignatureBooleanStringBoolean2Boolean(helperBooleanStringBoolean2Boolean) == "Boolean:String:Boolean2Boolean"
    result &= FunctionSignature.getSignatureBooleanBooleanInt2Boolean(helperBooleanBooleanInt2Boolean) == "Boolean:Boolean:Int2Boolean"
    result &= FunctionSignature.getSignatureBooleanBooleanLong2Boolean(helperBooleanBooleanLong2Boolean) == "Boolean:Boolean:Long2Boolean"
    result &= FunctionSignature.getSignatureBooleanBooleanDouble2Boolean(helperBooleanBooleanDouble2Boolean) == "Boolean:Boolean:Double2Boolean"
    result &= FunctionSignature.getSignatureBooleanBooleanString2Boolean(helperBooleanBooleanString2Boolean) == "Boolean:Boolean:String2Boolean"
    result &= FunctionSignature.getSignatureBooleanBooleanBoolean2Boolean(helperBooleanBooleanBoolean2Boolean) == "Boolean:Boolean:Boolean2Boolean"
    assert(result)
  }
  def helperIntIntIntInt2Int(a1: iFun, a2: iFun, a3: iFun, a4: iFun): iFun = iFunDummy
  def helperIntIntIntLong2Int(a1: iFun, a2: iFun, a3: iFun, a4: lFun): iFun = iFunDummy
  def helperIntIntIntDouble2Int(a1: iFun, a2: iFun, a3: iFun, a4: dFun): iFun = iFunDummy
  def helperIntIntIntString2Int(a1: iFun, a2: iFun, a3: iFun, a4: sFun): iFun = iFunDummy
  def helperIntIntIntBoolean2Int(a1: iFun, a2: iFun, a3: iFun, a4: bFun): iFun = iFunDummy
  def helperIntIntLongInt2Int(a1: iFun, a2: iFun, a3: lFun, a4: iFun): iFun = iFunDummy
  def helperIntIntLongLong2Int(a1: iFun, a2: iFun, a3: lFun, a4: lFun): iFun = iFunDummy
  def helperIntIntLongDouble2Int(a1: iFun, a2: iFun, a3: lFun, a4: dFun): iFun = iFunDummy
  def helperIntIntLongString2Int(a1: iFun, a2: iFun, a3: lFun, a4: sFun): iFun = iFunDummy
  def helperIntIntLongBoolean2Int(a1: iFun, a2: iFun, a3: lFun, a4: bFun): iFun = iFunDummy
  def helperIntIntDoubleInt2Int(a1: iFun, a2: iFun, a3: dFun, a4: iFun): iFun = iFunDummy
  def helperIntIntDoubleLong2Int(a1: iFun, a2: iFun, a3: dFun, a4: lFun): iFun = iFunDummy
  def helperIntIntDoubleDouble2Int(a1: iFun, a2: iFun, a3: dFun, a4: dFun): iFun = iFunDummy
  def helperIntIntDoubleString2Int(a1: iFun, a2: iFun, a3: dFun, a4: sFun): iFun = iFunDummy
  def helperIntIntDoubleBoolean2Int(a1: iFun, a2: iFun, a3: dFun, a4: bFun): iFun = iFunDummy
  def helperIntIntStringInt2Int(a1: iFun, a2: iFun, a3: sFun, a4: iFun): iFun = iFunDummy
  def helperIntIntStringLong2Int(a1: iFun, a2: iFun, a3: sFun, a4: lFun): iFun = iFunDummy
  def helperIntIntStringDouble2Int(a1: iFun, a2: iFun, a3: sFun, a4: dFun): iFun = iFunDummy
  def helperIntIntStringString2Int(a1: iFun, a2: iFun, a3: sFun, a4: sFun): iFun = iFunDummy
  def helperIntIntStringBoolean2Int(a1: iFun, a2: iFun, a3: sFun, a4: bFun): iFun = iFunDummy
  def helperIntIntBooleanInt2Int(a1: iFun, a2: iFun, a3: bFun, a4: iFun): iFun = iFunDummy
  def helperIntIntBooleanLong2Int(a1: iFun, a2: iFun, a3: bFun, a4: lFun): iFun = iFunDummy
  def helperIntIntBooleanDouble2Int(a1: iFun, a2: iFun, a3: bFun, a4: dFun): iFun = iFunDummy
  def helperIntIntBooleanString2Int(a1: iFun, a2: iFun, a3: bFun, a4: sFun): iFun = iFunDummy
  def helperIntIntBooleanBoolean2Int(a1: iFun, a2: iFun, a3: bFun, a4: bFun): iFun = iFunDummy
  def helperIntLongIntInt2Int(a1: iFun, a2: lFun, a3: iFun, a4: iFun): iFun = iFunDummy
  def helperIntLongIntLong2Int(a1: iFun, a2: lFun, a3: iFun, a4: lFun): iFun = iFunDummy
  def helperIntLongIntDouble2Int(a1: iFun, a2: lFun, a3: iFun, a4: dFun): iFun = iFunDummy
  def helperIntLongIntString2Int(a1: iFun, a2: lFun, a3: iFun, a4: sFun): iFun = iFunDummy
  def helperIntLongIntBoolean2Int(a1: iFun, a2: lFun, a3: iFun, a4: bFun): iFun = iFunDummy
  def helperIntLongLongInt2Int(a1: iFun, a2: lFun, a3: lFun, a4: iFun): iFun = iFunDummy
  def helperIntLongLongLong2Int(a1: iFun, a2: lFun, a3: lFun, a4: lFun): iFun = iFunDummy
  def helperIntLongLongDouble2Int(a1: iFun, a2: lFun, a3: lFun, a4: dFun): iFun = iFunDummy
  def helperIntLongLongString2Int(a1: iFun, a2: lFun, a3: lFun, a4: sFun): iFun = iFunDummy
  def helperIntLongLongBoolean2Int(a1: iFun, a2: lFun, a3: lFun, a4: bFun): iFun = iFunDummy
  def helperIntLongDoubleInt2Int(a1: iFun, a2: lFun, a3: dFun, a4: iFun): iFun = iFunDummy
  def helperIntLongDoubleLong2Int(a1: iFun, a2: lFun, a3: dFun, a4: lFun): iFun = iFunDummy
  def helperIntLongDoubleDouble2Int(a1: iFun, a2: lFun, a3: dFun, a4: dFun): iFun = iFunDummy
  def helperIntLongDoubleString2Int(a1: iFun, a2: lFun, a3: dFun, a4: sFun): iFun = iFunDummy
  def helperIntLongDoubleBoolean2Int(a1: iFun, a2: lFun, a3: dFun, a4: bFun): iFun = iFunDummy
  def helperIntLongStringInt2Int(a1: iFun, a2: lFun, a3: sFun, a4: iFun): iFun = iFunDummy
  def helperIntLongStringLong2Int(a1: iFun, a2: lFun, a3: sFun, a4: lFun): iFun = iFunDummy
  def helperIntLongStringDouble2Int(a1: iFun, a2: lFun, a3: sFun, a4: dFun): iFun = iFunDummy
  def helperIntLongStringString2Int(a1: iFun, a2: lFun, a3: sFun, a4: sFun): iFun = iFunDummy
  def helperIntLongStringBoolean2Int(a1: iFun, a2: lFun, a3: sFun, a4: bFun): iFun = iFunDummy
  def helperIntLongBooleanInt2Int(a1: iFun, a2: lFun, a3: bFun, a4: iFun): iFun = iFunDummy
  def helperIntLongBooleanLong2Int(a1: iFun, a2: lFun, a3: bFun, a4: lFun): iFun = iFunDummy
  def helperIntLongBooleanDouble2Int(a1: iFun, a2: lFun, a3: bFun, a4: dFun): iFun = iFunDummy
  def helperIntLongBooleanString2Int(a1: iFun, a2: lFun, a3: bFun, a4: sFun): iFun = iFunDummy
  def helperIntLongBooleanBoolean2Int(a1: iFun, a2: lFun, a3: bFun, a4: bFun): iFun = iFunDummy
  def helperIntDoubleIntInt2Int(a1: iFun, a2: dFun, a3: iFun, a4: iFun): iFun = iFunDummy
  def helperIntDoubleIntLong2Int(a1: iFun, a2: dFun, a3: iFun, a4: lFun): iFun = iFunDummy
  def helperIntDoubleIntDouble2Int(a1: iFun, a2: dFun, a3: iFun, a4: dFun): iFun = iFunDummy
  def helperIntDoubleIntString2Int(a1: iFun, a2: dFun, a3: iFun, a4: sFun): iFun = iFunDummy
  def helperIntDoubleIntBoolean2Int(a1: iFun, a2: dFun, a3: iFun, a4: bFun): iFun = iFunDummy
  def helperIntDoubleLongInt2Int(a1: iFun, a2: dFun, a3: lFun, a4: iFun): iFun = iFunDummy
  def helperIntDoubleLongLong2Int(a1: iFun, a2: dFun, a3: lFun, a4: lFun): iFun = iFunDummy
  def helperIntDoubleLongDouble2Int(a1: iFun, a2: dFun, a3: lFun, a4: dFun): iFun = iFunDummy
  def helperIntDoubleLongString2Int(a1: iFun, a2: dFun, a3: lFun, a4: sFun): iFun = iFunDummy
  def helperIntDoubleLongBoolean2Int(a1: iFun, a2: dFun, a3: lFun, a4: bFun): iFun = iFunDummy
  def helperIntDoubleDoubleInt2Int(a1: iFun, a2: dFun, a3: dFun, a4: iFun): iFun = iFunDummy
  def helperIntDoubleDoubleLong2Int(a1: iFun, a2: dFun, a3: dFun, a4: lFun): iFun = iFunDummy
  def helperIntDoubleDoubleDouble2Int(a1: iFun, a2: dFun, a3: dFun, a4: dFun): iFun = iFunDummy
  def helperIntDoubleDoubleString2Int(a1: iFun, a2: dFun, a3: dFun, a4: sFun): iFun = iFunDummy
  def helperIntDoubleDoubleBoolean2Int(a1: iFun, a2: dFun, a3: dFun, a4: bFun): iFun = iFunDummy
  def helperIntDoubleStringInt2Int(a1: iFun, a2: dFun, a3: sFun, a4: iFun): iFun = iFunDummy
  def helperIntDoubleStringLong2Int(a1: iFun, a2: dFun, a3: sFun, a4: lFun): iFun = iFunDummy
  def helperIntDoubleStringDouble2Int(a1: iFun, a2: dFun, a3: sFun, a4: dFun): iFun = iFunDummy
  def helperIntDoubleStringString2Int(a1: iFun, a2: dFun, a3: sFun, a4: sFun): iFun = iFunDummy
  def helperIntDoubleStringBoolean2Int(a1: iFun, a2: dFun, a3: sFun, a4: bFun): iFun = iFunDummy
  def helperIntDoubleBooleanInt2Int(a1: iFun, a2: dFun, a3: bFun, a4: iFun): iFun = iFunDummy
  def helperIntDoubleBooleanLong2Int(a1: iFun, a2: dFun, a3: bFun, a4: lFun): iFun = iFunDummy
  def helperIntDoubleBooleanDouble2Int(a1: iFun, a2: dFun, a3: bFun, a4: dFun): iFun = iFunDummy
  def helperIntDoubleBooleanString2Int(a1: iFun, a2: dFun, a3: bFun, a4: sFun): iFun = iFunDummy
  def helperIntDoubleBooleanBoolean2Int(a1: iFun, a2: dFun, a3: bFun, a4: bFun): iFun = iFunDummy
  def helperIntStringIntInt2Int(a1: iFun, a2: sFun, a3: iFun, a4: iFun): iFun = iFunDummy
  def helperIntStringIntLong2Int(a1: iFun, a2: sFun, a3: iFun, a4: lFun): iFun = iFunDummy
  def helperIntStringIntDouble2Int(a1: iFun, a2: sFun, a3: iFun, a4: dFun): iFun = iFunDummy
  def helperIntStringIntString2Int(a1: iFun, a2: sFun, a3: iFun, a4: sFun): iFun = iFunDummy
  def helperIntStringIntBoolean2Int(a1: iFun, a2: sFun, a3: iFun, a4: bFun): iFun = iFunDummy
  def helperIntStringLongInt2Int(a1: iFun, a2: sFun, a3: lFun, a4: iFun): iFun = iFunDummy
  def helperIntStringLongLong2Int(a1: iFun, a2: sFun, a3: lFun, a4: lFun): iFun = iFunDummy
  def helperIntStringLongDouble2Int(a1: iFun, a2: sFun, a3: lFun, a4: dFun): iFun = iFunDummy
  def helperIntStringLongString2Int(a1: iFun, a2: sFun, a3: lFun, a4: sFun): iFun = iFunDummy
  def helperIntStringLongBoolean2Int(a1: iFun, a2: sFun, a3: lFun, a4: bFun): iFun = iFunDummy
  def helperIntStringDoubleInt2Int(a1: iFun, a2: sFun, a3: dFun, a4: iFun): iFun = iFunDummy
  def helperIntStringDoubleLong2Int(a1: iFun, a2: sFun, a3: dFun, a4: lFun): iFun = iFunDummy
  def helperIntStringDoubleDouble2Int(a1: iFun, a2: sFun, a3: dFun, a4: dFun): iFun = iFunDummy
  def helperIntStringDoubleString2Int(a1: iFun, a2: sFun, a3: dFun, a4: sFun): iFun = iFunDummy
  def helperIntStringDoubleBoolean2Int(a1: iFun, a2: sFun, a3: dFun, a4: bFun): iFun = iFunDummy
  def helperIntStringStringInt2Int(a1: iFun, a2: sFun, a3: sFun, a4: iFun): iFun = iFunDummy
  def helperIntStringStringLong2Int(a1: iFun, a2: sFun, a3: sFun, a4: lFun): iFun = iFunDummy
  def helperIntStringStringDouble2Int(a1: iFun, a2: sFun, a3: sFun, a4: dFun): iFun = iFunDummy
  def helperIntStringStringString2Int(a1: iFun, a2: sFun, a3: sFun, a4: sFun): iFun = iFunDummy
  def helperIntStringStringBoolean2Int(a1: iFun, a2: sFun, a3: sFun, a4: bFun): iFun = iFunDummy
  def helperIntStringBooleanInt2Int(a1: iFun, a2: sFun, a3: bFun, a4: iFun): iFun = iFunDummy
  def helperIntStringBooleanLong2Int(a1: iFun, a2: sFun, a3: bFun, a4: lFun): iFun = iFunDummy
  def helperIntStringBooleanDouble2Int(a1: iFun, a2: sFun, a3: bFun, a4: dFun): iFun = iFunDummy
  def helperIntStringBooleanString2Int(a1: iFun, a2: sFun, a3: bFun, a4: sFun): iFun = iFunDummy
  def helperIntStringBooleanBoolean2Int(a1: iFun, a2: sFun, a3: bFun, a4: bFun): iFun = iFunDummy
  def helperIntBooleanIntInt2Int(a1: iFun, a2: bFun, a3: iFun, a4: iFun): iFun = iFunDummy
  def helperIntBooleanIntLong2Int(a1: iFun, a2: bFun, a3: iFun, a4: lFun): iFun = iFunDummy
  def helperIntBooleanIntDouble2Int(a1: iFun, a2: bFun, a3: iFun, a4: dFun): iFun = iFunDummy
  def helperIntBooleanIntString2Int(a1: iFun, a2: bFun, a3: iFun, a4: sFun): iFun = iFunDummy
  def helperIntBooleanIntBoolean2Int(a1: iFun, a2: bFun, a3: iFun, a4: bFun): iFun = iFunDummy
  def helperIntBooleanLongInt2Int(a1: iFun, a2: bFun, a3: lFun, a4: iFun): iFun = iFunDummy
  def helperIntBooleanLongLong2Int(a1: iFun, a2: bFun, a3: lFun, a4: lFun): iFun = iFunDummy
  def helperIntBooleanLongDouble2Int(a1: iFun, a2: bFun, a3: lFun, a4: dFun): iFun = iFunDummy
  def helperIntBooleanLongString2Int(a1: iFun, a2: bFun, a3: lFun, a4: sFun): iFun = iFunDummy
  def helperIntBooleanLongBoolean2Int(a1: iFun, a2: bFun, a3: lFun, a4: bFun): iFun = iFunDummy
  def helperIntBooleanDoubleInt2Int(a1: iFun, a2: bFun, a3: dFun, a4: iFun): iFun = iFunDummy
  def helperIntBooleanDoubleLong2Int(a1: iFun, a2: bFun, a3: dFun, a4: lFun): iFun = iFunDummy
  def helperIntBooleanDoubleDouble2Int(a1: iFun, a2: bFun, a3: dFun, a4: dFun): iFun = iFunDummy
  def helperIntBooleanDoubleString2Int(a1: iFun, a2: bFun, a3: dFun, a4: sFun): iFun = iFunDummy
  def helperIntBooleanDoubleBoolean2Int(a1: iFun, a2: bFun, a3: dFun, a4: bFun): iFun = iFunDummy
  def helperIntBooleanStringInt2Int(a1: iFun, a2: bFun, a3: sFun, a4: iFun): iFun = iFunDummy
  def helperIntBooleanStringLong2Int(a1: iFun, a2: bFun, a3: sFun, a4: lFun): iFun = iFunDummy
  def helperIntBooleanStringDouble2Int(a1: iFun, a2: bFun, a3: sFun, a4: dFun): iFun = iFunDummy
  def helperIntBooleanStringString2Int(a1: iFun, a2: bFun, a3: sFun, a4: sFun): iFun = iFunDummy
  def helperIntBooleanStringBoolean2Int(a1: iFun, a2: bFun, a3: sFun, a4: bFun): iFun = iFunDummy
  def helperIntBooleanBooleanInt2Int(a1: iFun, a2: bFun, a3: bFun, a4: iFun): iFun = iFunDummy
  def helperIntBooleanBooleanLong2Int(a1: iFun, a2: bFun, a3: bFun, a4: lFun): iFun = iFunDummy
  def helperIntBooleanBooleanDouble2Int(a1: iFun, a2: bFun, a3: bFun, a4: dFun): iFun = iFunDummy
  def helperIntBooleanBooleanString2Int(a1: iFun, a2: bFun, a3: bFun, a4: sFun): iFun = iFunDummy
  def helperIntBooleanBooleanBoolean2Int(a1: iFun, a2: bFun, a3: bFun, a4: bFun): iFun = iFunDummy
  def helperLongIntIntInt2Int(a1: lFun, a2: iFun, a3: iFun, a4: iFun): iFun = iFunDummy
  def helperLongIntIntLong2Int(a1: lFun, a2: iFun, a3: iFun, a4: lFun): iFun = iFunDummy
  def helperLongIntIntDouble2Int(a1: lFun, a2: iFun, a3: iFun, a4: dFun): iFun = iFunDummy
  def helperLongIntIntString2Int(a1: lFun, a2: iFun, a3: iFun, a4: sFun): iFun = iFunDummy
  def helperLongIntIntBoolean2Int(a1: lFun, a2: iFun, a3: iFun, a4: bFun): iFun = iFunDummy
  def helperLongIntLongInt2Int(a1: lFun, a2: iFun, a3: lFun, a4: iFun): iFun = iFunDummy
  def helperLongIntLongLong2Int(a1: lFun, a2: iFun, a3: lFun, a4: lFun): iFun = iFunDummy
  def helperLongIntLongDouble2Int(a1: lFun, a2: iFun, a3: lFun, a4: dFun): iFun = iFunDummy
  def helperLongIntLongString2Int(a1: lFun, a2: iFun, a3: lFun, a4: sFun): iFun = iFunDummy
  def helperLongIntLongBoolean2Int(a1: lFun, a2: iFun, a3: lFun, a4: bFun): iFun = iFunDummy
  def helperLongIntDoubleInt2Int(a1: lFun, a2: iFun, a3: dFun, a4: iFun): iFun = iFunDummy
  def helperLongIntDoubleLong2Int(a1: lFun, a2: iFun, a3: dFun, a4: lFun): iFun = iFunDummy
  def helperLongIntDoubleDouble2Int(a1: lFun, a2: iFun, a3: dFun, a4: dFun): iFun = iFunDummy
  def helperLongIntDoubleString2Int(a1: lFun, a2: iFun, a3: dFun, a4: sFun): iFun = iFunDummy
  def helperLongIntDoubleBoolean2Int(a1: lFun, a2: iFun, a3: dFun, a4: bFun): iFun = iFunDummy
  def helperLongIntStringInt2Int(a1: lFun, a2: iFun, a3: sFun, a4: iFun): iFun = iFunDummy
  def helperLongIntStringLong2Int(a1: lFun, a2: iFun, a3: sFun, a4: lFun): iFun = iFunDummy
  def helperLongIntStringDouble2Int(a1: lFun, a2: iFun, a3: sFun, a4: dFun): iFun = iFunDummy
  def helperLongIntStringString2Int(a1: lFun, a2: iFun, a3: sFun, a4: sFun): iFun = iFunDummy
  def helperLongIntStringBoolean2Int(a1: lFun, a2: iFun, a3: sFun, a4: bFun): iFun = iFunDummy
  def helperLongIntBooleanInt2Int(a1: lFun, a2: iFun, a3: bFun, a4: iFun): iFun = iFunDummy
  def helperLongIntBooleanLong2Int(a1: lFun, a2: iFun, a3: bFun, a4: lFun): iFun = iFunDummy
  def helperLongIntBooleanDouble2Int(a1: lFun, a2: iFun, a3: bFun, a4: dFun): iFun = iFunDummy
  def helperLongIntBooleanString2Int(a1: lFun, a2: iFun, a3: bFun, a4: sFun): iFun = iFunDummy
  def helperLongIntBooleanBoolean2Int(a1: lFun, a2: iFun, a3: bFun, a4: bFun): iFun = iFunDummy
  def helperLongLongIntInt2Int(a1: lFun, a2: lFun, a3: iFun, a4: iFun): iFun = iFunDummy
  def helperLongLongIntLong2Int(a1: lFun, a2: lFun, a3: iFun, a4: lFun): iFun = iFunDummy
  def helperLongLongIntDouble2Int(a1: lFun, a2: lFun, a3: iFun, a4: dFun): iFun = iFunDummy
  def helperLongLongIntString2Int(a1: lFun, a2: lFun, a3: iFun, a4: sFun): iFun = iFunDummy
  def helperLongLongIntBoolean2Int(a1: lFun, a2: lFun, a3: iFun, a4: bFun): iFun = iFunDummy
  def helperLongLongLongInt2Int(a1: lFun, a2: lFun, a3: lFun, a4: iFun): iFun = iFunDummy
  def helperLongLongLongLong2Int(a1: lFun, a2: lFun, a3: lFun, a4: lFun): iFun = iFunDummy
  def helperLongLongLongDouble2Int(a1: lFun, a2: lFun, a3: lFun, a4: dFun): iFun = iFunDummy
  def helperLongLongLongString2Int(a1: lFun, a2: lFun, a3: lFun, a4: sFun): iFun = iFunDummy
  def helperLongLongLongBoolean2Int(a1: lFun, a2: lFun, a3: lFun, a4: bFun): iFun = iFunDummy
  def helperLongLongDoubleInt2Int(a1: lFun, a2: lFun, a3: dFun, a4: iFun): iFun = iFunDummy
  def helperLongLongDoubleLong2Int(a1: lFun, a2: lFun, a3: dFun, a4: lFun): iFun = iFunDummy
  def helperLongLongDoubleDouble2Int(a1: lFun, a2: lFun, a3: dFun, a4: dFun): iFun = iFunDummy
  def helperLongLongDoubleString2Int(a1: lFun, a2: lFun, a3: dFun, a4: sFun): iFun = iFunDummy
  def helperLongLongDoubleBoolean2Int(a1: lFun, a2: lFun, a3: dFun, a4: bFun): iFun = iFunDummy
  def helperLongLongStringInt2Int(a1: lFun, a2: lFun, a3: sFun, a4: iFun): iFun = iFunDummy
  def helperLongLongStringLong2Int(a1: lFun, a2: lFun, a3: sFun, a4: lFun): iFun = iFunDummy
  def helperLongLongStringDouble2Int(a1: lFun, a2: lFun, a3: sFun, a4: dFun): iFun = iFunDummy
  def helperLongLongStringString2Int(a1: lFun, a2: lFun, a3: sFun, a4: sFun): iFun = iFunDummy
  def helperLongLongStringBoolean2Int(a1: lFun, a2: lFun, a3: sFun, a4: bFun): iFun = iFunDummy
  def helperLongLongBooleanInt2Int(a1: lFun, a2: lFun, a3: bFun, a4: iFun): iFun = iFunDummy
  def helperLongLongBooleanLong2Int(a1: lFun, a2: lFun, a3: bFun, a4: lFun): iFun = iFunDummy
  def helperLongLongBooleanDouble2Int(a1: lFun, a2: lFun, a3: bFun, a4: dFun): iFun = iFunDummy
  def helperLongLongBooleanString2Int(a1: lFun, a2: lFun, a3: bFun, a4: sFun): iFun = iFunDummy
  def helperLongLongBooleanBoolean2Int(a1: lFun, a2: lFun, a3: bFun, a4: bFun): iFun = iFunDummy
  def helperLongDoubleIntInt2Int(a1: lFun, a2: dFun, a3: iFun, a4: iFun): iFun = iFunDummy
  def helperLongDoubleIntLong2Int(a1: lFun, a2: dFun, a3: iFun, a4: lFun): iFun = iFunDummy
  def helperLongDoubleIntDouble2Int(a1: lFun, a2: dFun, a3: iFun, a4: dFun): iFun = iFunDummy
  def helperLongDoubleIntString2Int(a1: lFun, a2: dFun, a3: iFun, a4: sFun): iFun = iFunDummy
  def helperLongDoubleIntBoolean2Int(a1: lFun, a2: dFun, a3: iFun, a4: bFun): iFun = iFunDummy
  def helperLongDoubleLongInt2Int(a1: lFun, a2: dFun, a3: lFun, a4: iFun): iFun = iFunDummy
  def helperLongDoubleLongLong2Int(a1: lFun, a2: dFun, a3: lFun, a4: lFun): iFun = iFunDummy
  def helperLongDoubleLongDouble2Int(a1: lFun, a2: dFun, a3: lFun, a4: dFun): iFun = iFunDummy
  def helperLongDoubleLongString2Int(a1: lFun, a2: dFun, a3: lFun, a4: sFun): iFun = iFunDummy
  def helperLongDoubleLongBoolean2Int(a1: lFun, a2: dFun, a3: lFun, a4: bFun): iFun = iFunDummy
  def helperLongDoubleDoubleInt2Int(a1: lFun, a2: dFun, a3: dFun, a4: iFun): iFun = iFunDummy
  def helperLongDoubleDoubleLong2Int(a1: lFun, a2: dFun, a3: dFun, a4: lFun): iFun = iFunDummy
  def helperLongDoubleDoubleDouble2Int(a1: lFun, a2: dFun, a3: dFun, a4: dFun): iFun = iFunDummy
  def helperLongDoubleDoubleString2Int(a1: lFun, a2: dFun, a3: dFun, a4: sFun): iFun = iFunDummy
  def helperLongDoubleDoubleBoolean2Int(a1: lFun, a2: dFun, a3: dFun, a4: bFun): iFun = iFunDummy
  def helperLongDoubleStringInt2Int(a1: lFun, a2: dFun, a3: sFun, a4: iFun): iFun = iFunDummy
  def helperLongDoubleStringLong2Int(a1: lFun, a2: dFun, a3: sFun, a4: lFun): iFun = iFunDummy
  def helperLongDoubleStringDouble2Int(a1: lFun, a2: dFun, a3: sFun, a4: dFun): iFun = iFunDummy
  def helperLongDoubleStringString2Int(a1: lFun, a2: dFun, a3: sFun, a4: sFun): iFun = iFunDummy
  def helperLongDoubleStringBoolean2Int(a1: lFun, a2: dFun, a3: sFun, a4: bFun): iFun = iFunDummy
  def helperLongDoubleBooleanInt2Int(a1: lFun, a2: dFun, a3: bFun, a4: iFun): iFun = iFunDummy
  def helperLongDoubleBooleanLong2Int(a1: lFun, a2: dFun, a3: bFun, a4: lFun): iFun = iFunDummy
  def helperLongDoubleBooleanDouble2Int(a1: lFun, a2: dFun, a3: bFun, a4: dFun): iFun = iFunDummy
  def helperLongDoubleBooleanString2Int(a1: lFun, a2: dFun, a3: bFun, a4: sFun): iFun = iFunDummy
  def helperLongDoubleBooleanBoolean2Int(a1: lFun, a2: dFun, a3: bFun, a4: bFun): iFun = iFunDummy
  def helperLongStringIntInt2Int(a1: lFun, a2: sFun, a3: iFun, a4: iFun): iFun = iFunDummy
  def helperLongStringIntLong2Int(a1: lFun, a2: sFun, a3: iFun, a4: lFun): iFun = iFunDummy
  def helperLongStringIntDouble2Int(a1: lFun, a2: sFun, a3: iFun, a4: dFun): iFun = iFunDummy
  def helperLongStringIntString2Int(a1: lFun, a2: sFun, a3: iFun, a4: sFun): iFun = iFunDummy
  def helperLongStringIntBoolean2Int(a1: lFun, a2: sFun, a3: iFun, a4: bFun): iFun = iFunDummy
  def helperLongStringLongInt2Int(a1: lFun, a2: sFun, a3: lFun, a4: iFun): iFun = iFunDummy
  def helperLongStringLongLong2Int(a1: lFun, a2: sFun, a3: lFun, a4: lFun): iFun = iFunDummy
  def helperLongStringLongDouble2Int(a1: lFun, a2: sFun, a3: lFun, a4: dFun): iFun = iFunDummy
  def helperLongStringLongString2Int(a1: lFun, a2: sFun, a3: lFun, a4: sFun): iFun = iFunDummy
  def helperLongStringLongBoolean2Int(a1: lFun, a2: sFun, a3: lFun, a4: bFun): iFun = iFunDummy
  def helperLongStringDoubleInt2Int(a1: lFun, a2: sFun, a3: dFun, a4: iFun): iFun = iFunDummy
  def helperLongStringDoubleLong2Int(a1: lFun, a2: sFun, a3: dFun, a4: lFun): iFun = iFunDummy
  def helperLongStringDoubleDouble2Int(a1: lFun, a2: sFun, a3: dFun, a4: dFun): iFun = iFunDummy
  def helperLongStringDoubleString2Int(a1: lFun, a2: sFun, a3: dFun, a4: sFun): iFun = iFunDummy
  def helperLongStringDoubleBoolean2Int(a1: lFun, a2: sFun, a3: dFun, a4: bFun): iFun = iFunDummy
  def helperLongStringStringInt2Int(a1: lFun, a2: sFun, a3: sFun, a4: iFun): iFun = iFunDummy
  def helperLongStringStringLong2Int(a1: lFun, a2: sFun, a3: sFun, a4: lFun): iFun = iFunDummy
  def helperLongStringStringDouble2Int(a1: lFun, a2: sFun, a3: sFun, a4: dFun): iFun = iFunDummy
  def helperLongStringStringString2Int(a1: lFun, a2: sFun, a3: sFun, a4: sFun): iFun = iFunDummy
  def helperLongStringStringBoolean2Int(a1: lFun, a2: sFun, a3: sFun, a4: bFun): iFun = iFunDummy
  def helperLongStringBooleanInt2Int(a1: lFun, a2: sFun, a3: bFun, a4: iFun): iFun = iFunDummy
  def helperLongStringBooleanLong2Int(a1: lFun, a2: sFun, a3: bFun, a4: lFun): iFun = iFunDummy
  def helperLongStringBooleanDouble2Int(a1: lFun, a2: sFun, a3: bFun, a4: dFun): iFun = iFunDummy
  def helperLongStringBooleanString2Int(a1: lFun, a2: sFun, a3: bFun, a4: sFun): iFun = iFunDummy
  def helperLongStringBooleanBoolean2Int(a1: lFun, a2: sFun, a3: bFun, a4: bFun): iFun = iFunDummy
  def helperLongBooleanIntInt2Int(a1: lFun, a2: bFun, a3: iFun, a4: iFun): iFun = iFunDummy
  def helperLongBooleanIntLong2Int(a1: lFun, a2: bFun, a3: iFun, a4: lFun): iFun = iFunDummy
  def helperLongBooleanIntDouble2Int(a1: lFun, a2: bFun, a3: iFun, a4: dFun): iFun = iFunDummy
  def helperLongBooleanIntString2Int(a1: lFun, a2: bFun, a3: iFun, a4: sFun): iFun = iFunDummy
  def helperLongBooleanIntBoolean2Int(a1: lFun, a2: bFun, a3: iFun, a4: bFun): iFun = iFunDummy
  def helperLongBooleanLongInt2Int(a1: lFun, a2: bFun, a3: lFun, a4: iFun): iFun = iFunDummy
  def helperLongBooleanLongLong2Int(a1: lFun, a2: bFun, a3: lFun, a4: lFun): iFun = iFunDummy
  def helperLongBooleanLongDouble2Int(a1: lFun, a2: bFun, a3: lFun, a4: dFun): iFun = iFunDummy
  def helperLongBooleanLongString2Int(a1: lFun, a2: bFun, a3: lFun, a4: sFun): iFun = iFunDummy
  def helperLongBooleanLongBoolean2Int(a1: lFun, a2: bFun, a3: lFun, a4: bFun): iFun = iFunDummy
  def helperLongBooleanDoubleInt2Int(a1: lFun, a2: bFun, a3: dFun, a4: iFun): iFun = iFunDummy
  def helperLongBooleanDoubleLong2Int(a1: lFun, a2: bFun, a3: dFun, a4: lFun): iFun = iFunDummy
  def helperLongBooleanDoubleDouble2Int(a1: lFun, a2: bFun, a3: dFun, a4: dFun): iFun = iFunDummy
  def helperLongBooleanDoubleString2Int(a1: lFun, a2: bFun, a3: dFun, a4: sFun): iFun = iFunDummy
  def helperLongBooleanDoubleBoolean2Int(a1: lFun, a2: bFun, a3: dFun, a4: bFun): iFun = iFunDummy
  def helperLongBooleanStringInt2Int(a1: lFun, a2: bFun, a3: sFun, a4: iFun): iFun = iFunDummy
  def helperLongBooleanStringLong2Int(a1: lFun, a2: bFun, a3: sFun, a4: lFun): iFun = iFunDummy
  def helperLongBooleanStringDouble2Int(a1: lFun, a2: bFun, a3: sFun, a4: dFun): iFun = iFunDummy
  def helperLongBooleanStringString2Int(a1: lFun, a2: bFun, a3: sFun, a4: sFun): iFun = iFunDummy
  def helperLongBooleanStringBoolean2Int(a1: lFun, a2: bFun, a3: sFun, a4: bFun): iFun = iFunDummy
  def helperLongBooleanBooleanInt2Int(a1: lFun, a2: bFun, a3: bFun, a4: iFun): iFun = iFunDummy
  def helperLongBooleanBooleanLong2Int(a1: lFun, a2: bFun, a3: bFun, a4: lFun): iFun = iFunDummy
  def helperLongBooleanBooleanDouble2Int(a1: lFun, a2: bFun, a3: bFun, a4: dFun): iFun = iFunDummy
  def helperLongBooleanBooleanString2Int(a1: lFun, a2: bFun, a3: bFun, a4: sFun): iFun = iFunDummy
  def helperLongBooleanBooleanBoolean2Int(a1: lFun, a2: bFun, a3: bFun, a4: bFun): iFun = iFunDummy
  def helperDoubleIntIntInt2Int(a1: dFun, a2: iFun, a3: iFun, a4: iFun): iFun = iFunDummy
  def helperDoubleIntIntLong2Int(a1: dFun, a2: iFun, a3: iFun, a4: lFun): iFun = iFunDummy
  def helperDoubleIntIntDouble2Int(a1: dFun, a2: iFun, a3: iFun, a4: dFun): iFun = iFunDummy
  def helperDoubleIntIntString2Int(a1: dFun, a2: iFun, a3: iFun, a4: sFun): iFun = iFunDummy
  def helperDoubleIntIntBoolean2Int(a1: dFun, a2: iFun, a3: iFun, a4: bFun): iFun = iFunDummy
  def helperDoubleIntLongInt2Int(a1: dFun, a2: iFun, a3: lFun, a4: iFun): iFun = iFunDummy
  def helperDoubleIntLongLong2Int(a1: dFun, a2: iFun, a3: lFun, a4: lFun): iFun = iFunDummy
  def helperDoubleIntLongDouble2Int(a1: dFun, a2: iFun, a3: lFun, a4: dFun): iFun = iFunDummy
  def helperDoubleIntLongString2Int(a1: dFun, a2: iFun, a3: lFun, a4: sFun): iFun = iFunDummy
  def helperDoubleIntLongBoolean2Int(a1: dFun, a2: iFun, a3: lFun, a4: bFun): iFun = iFunDummy
  def helperDoubleIntDoubleInt2Int(a1: dFun, a2: iFun, a3: dFun, a4: iFun): iFun = iFunDummy
  def helperDoubleIntDoubleLong2Int(a1: dFun, a2: iFun, a3: dFun, a4: lFun): iFun = iFunDummy
  def helperDoubleIntDoubleDouble2Int(a1: dFun, a2: iFun, a3: dFun, a4: dFun): iFun = iFunDummy
  def helperDoubleIntDoubleString2Int(a1: dFun, a2: iFun, a3: dFun, a4: sFun): iFun = iFunDummy
  def helperDoubleIntDoubleBoolean2Int(a1: dFun, a2: iFun, a3: dFun, a4: bFun): iFun = iFunDummy
  def helperDoubleIntStringInt2Int(a1: dFun, a2: iFun, a3: sFun, a4: iFun): iFun = iFunDummy
  def helperDoubleIntStringLong2Int(a1: dFun, a2: iFun, a3: sFun, a4: lFun): iFun = iFunDummy
  def helperDoubleIntStringDouble2Int(a1: dFun, a2: iFun, a3: sFun, a4: dFun): iFun = iFunDummy
  def helperDoubleIntStringString2Int(a1: dFun, a2: iFun, a3: sFun, a4: sFun): iFun = iFunDummy
  def helperDoubleIntStringBoolean2Int(a1: dFun, a2: iFun, a3: sFun, a4: bFun): iFun = iFunDummy
  def helperDoubleIntBooleanInt2Int(a1: dFun, a2: iFun, a3: bFun, a4: iFun): iFun = iFunDummy
  def helperDoubleIntBooleanLong2Int(a1: dFun, a2: iFun, a3: bFun, a4: lFun): iFun = iFunDummy
  def helperDoubleIntBooleanDouble2Int(a1: dFun, a2: iFun, a3: bFun, a4: dFun): iFun = iFunDummy
  def helperDoubleIntBooleanString2Int(a1: dFun, a2: iFun, a3: bFun, a4: sFun): iFun = iFunDummy
  def helperDoubleIntBooleanBoolean2Int(a1: dFun, a2: iFun, a3: bFun, a4: bFun): iFun = iFunDummy
  def helperDoubleLongIntInt2Int(a1: dFun, a2: lFun, a3: iFun, a4: iFun): iFun = iFunDummy
  def helperDoubleLongIntLong2Int(a1: dFun, a2: lFun, a3: iFun, a4: lFun): iFun = iFunDummy
  def helperDoubleLongIntDouble2Int(a1: dFun, a2: lFun, a3: iFun, a4: dFun): iFun = iFunDummy
  def helperDoubleLongIntString2Int(a1: dFun, a2: lFun, a3: iFun, a4: sFun): iFun = iFunDummy
  def helperDoubleLongIntBoolean2Int(a1: dFun, a2: lFun, a3: iFun, a4: bFun): iFun = iFunDummy
  def helperDoubleLongLongInt2Int(a1: dFun, a2: lFun, a3: lFun, a4: iFun): iFun = iFunDummy
  def helperDoubleLongLongLong2Int(a1: dFun, a2: lFun, a3: lFun, a4: lFun): iFun = iFunDummy
  def helperDoubleLongLongDouble2Int(a1: dFun, a2: lFun, a3: lFun, a4: dFun): iFun = iFunDummy
  def helperDoubleLongLongString2Int(a1: dFun, a2: lFun, a3: lFun, a4: sFun): iFun = iFunDummy
  def helperDoubleLongLongBoolean2Int(a1: dFun, a2: lFun, a3: lFun, a4: bFun): iFun = iFunDummy
  def helperDoubleLongDoubleInt2Int(a1: dFun, a2: lFun, a3: dFun, a4: iFun): iFun = iFunDummy
  def helperDoubleLongDoubleLong2Int(a1: dFun, a2: lFun, a3: dFun, a4: lFun): iFun = iFunDummy
  def helperDoubleLongDoubleDouble2Int(a1: dFun, a2: lFun, a3: dFun, a4: dFun): iFun = iFunDummy
  def helperDoubleLongDoubleString2Int(a1: dFun, a2: lFun, a3: dFun, a4: sFun): iFun = iFunDummy
  def helperDoubleLongDoubleBoolean2Int(a1: dFun, a2: lFun, a3: dFun, a4: bFun): iFun = iFunDummy
  def helperDoubleLongStringInt2Int(a1: dFun, a2: lFun, a3: sFun, a4: iFun): iFun = iFunDummy
  def helperDoubleLongStringLong2Int(a1: dFun, a2: lFun, a3: sFun, a4: lFun): iFun = iFunDummy
  def helperDoubleLongStringDouble2Int(a1: dFun, a2: lFun, a3: sFun, a4: dFun): iFun = iFunDummy
  def helperDoubleLongStringString2Int(a1: dFun, a2: lFun, a3: sFun, a4: sFun): iFun = iFunDummy
  def helperDoubleLongStringBoolean2Int(a1: dFun, a2: lFun, a3: sFun, a4: bFun): iFun = iFunDummy
  def helperDoubleLongBooleanInt2Int(a1: dFun, a2: lFun, a3: bFun, a4: iFun): iFun = iFunDummy
  def helperDoubleLongBooleanLong2Int(a1: dFun, a2: lFun, a3: bFun, a4: lFun): iFun = iFunDummy
  def helperDoubleLongBooleanDouble2Int(a1: dFun, a2: lFun, a3: bFun, a4: dFun): iFun = iFunDummy
  def helperDoubleLongBooleanString2Int(a1: dFun, a2: lFun, a3: bFun, a4: sFun): iFun = iFunDummy
  def helperDoubleLongBooleanBoolean2Int(a1: dFun, a2: lFun, a3: bFun, a4: bFun): iFun = iFunDummy
  def helperDoubleDoubleIntInt2Int(a1: dFun, a2: dFun, a3: iFun, a4: iFun): iFun = iFunDummy
  def helperDoubleDoubleIntLong2Int(a1: dFun, a2: dFun, a3: iFun, a4: lFun): iFun = iFunDummy
  def helperDoubleDoubleIntDouble2Int(a1: dFun, a2: dFun, a3: iFun, a4: dFun): iFun = iFunDummy
  def helperDoubleDoubleIntString2Int(a1: dFun, a2: dFun, a3: iFun, a4: sFun): iFun = iFunDummy
  def helperDoubleDoubleIntBoolean2Int(a1: dFun, a2: dFun, a3: iFun, a4: bFun): iFun = iFunDummy
  def helperDoubleDoubleLongInt2Int(a1: dFun, a2: dFun, a3: lFun, a4: iFun): iFun = iFunDummy
  def helperDoubleDoubleLongLong2Int(a1: dFun, a2: dFun, a3: lFun, a4: lFun): iFun = iFunDummy
  def helperDoubleDoubleLongDouble2Int(a1: dFun, a2: dFun, a3: lFun, a4: dFun): iFun = iFunDummy
  def helperDoubleDoubleLongString2Int(a1: dFun, a2: dFun, a3: lFun, a4: sFun): iFun = iFunDummy
  def helperDoubleDoubleLongBoolean2Int(a1: dFun, a2: dFun, a3: lFun, a4: bFun): iFun = iFunDummy
  def helperDoubleDoubleDoubleInt2Int(a1: dFun, a2: dFun, a3: dFun, a4: iFun): iFun = iFunDummy
  def helperDoubleDoubleDoubleLong2Int(a1: dFun, a2: dFun, a3: dFun, a4: lFun): iFun = iFunDummy
  def helperDoubleDoubleDoubleDouble2Int(a1: dFun, a2: dFun, a3: dFun, a4: dFun): iFun = iFunDummy
  def helperDoubleDoubleDoubleString2Int(a1: dFun, a2: dFun, a3: dFun, a4: sFun): iFun = iFunDummy
  def helperDoubleDoubleDoubleBoolean2Int(a1: dFun, a2: dFun, a3: dFun, a4: bFun): iFun = iFunDummy
  def helperDoubleDoubleStringInt2Int(a1: dFun, a2: dFun, a3: sFun, a4: iFun): iFun = iFunDummy
  def helperDoubleDoubleStringLong2Int(a1: dFun, a2: dFun, a3: sFun, a4: lFun): iFun = iFunDummy
  def helperDoubleDoubleStringDouble2Int(a1: dFun, a2: dFun, a3: sFun, a4: dFun): iFun = iFunDummy
  def helperDoubleDoubleStringString2Int(a1: dFun, a2: dFun, a3: sFun, a4: sFun): iFun = iFunDummy
  def helperDoubleDoubleStringBoolean2Int(a1: dFun, a2: dFun, a3: sFun, a4: bFun): iFun = iFunDummy
  def helperDoubleDoubleBooleanInt2Int(a1: dFun, a2: dFun, a3: bFun, a4: iFun): iFun = iFunDummy
  def helperDoubleDoubleBooleanLong2Int(a1: dFun, a2: dFun, a3: bFun, a4: lFun): iFun = iFunDummy
  def helperDoubleDoubleBooleanDouble2Int(a1: dFun, a2: dFun, a3: bFun, a4: dFun): iFun = iFunDummy
  def helperDoubleDoubleBooleanString2Int(a1: dFun, a2: dFun, a3: bFun, a4: sFun): iFun = iFunDummy
  def helperDoubleDoubleBooleanBoolean2Int(a1: dFun, a2: dFun, a3: bFun, a4: bFun): iFun = iFunDummy
  def helperDoubleStringIntInt2Int(a1: dFun, a2: sFun, a3: iFun, a4: iFun): iFun = iFunDummy
  def helperDoubleStringIntLong2Int(a1: dFun, a2: sFun, a3: iFun, a4: lFun): iFun = iFunDummy
  def helperDoubleStringIntDouble2Int(a1: dFun, a2: sFun, a3: iFun, a4: dFun): iFun = iFunDummy
  def helperDoubleStringIntString2Int(a1: dFun, a2: sFun, a3: iFun, a4: sFun): iFun = iFunDummy
  def helperDoubleStringIntBoolean2Int(a1: dFun, a2: sFun, a3: iFun, a4: bFun): iFun = iFunDummy
  def helperDoubleStringLongInt2Int(a1: dFun, a2: sFun, a3: lFun, a4: iFun): iFun = iFunDummy
  def helperDoubleStringLongLong2Int(a1: dFun, a2: sFun, a3: lFun, a4: lFun): iFun = iFunDummy
  def helperDoubleStringLongDouble2Int(a1: dFun, a2: sFun, a3: lFun, a4: dFun): iFun = iFunDummy
  def helperDoubleStringLongString2Int(a1: dFun, a2: sFun, a3: lFun, a4: sFun): iFun = iFunDummy
  def helperDoubleStringLongBoolean2Int(a1: dFun, a2: sFun, a3: lFun, a4: bFun): iFun = iFunDummy
  def helperDoubleStringDoubleInt2Int(a1: dFun, a2: sFun, a3: dFun, a4: iFun): iFun = iFunDummy
  def helperDoubleStringDoubleLong2Int(a1: dFun, a2: sFun, a3: dFun, a4: lFun): iFun = iFunDummy
  def helperDoubleStringDoubleDouble2Int(a1: dFun, a2: sFun, a3: dFun, a4: dFun): iFun = iFunDummy
  def helperDoubleStringDoubleString2Int(a1: dFun, a2: sFun, a3: dFun, a4: sFun): iFun = iFunDummy
  def helperDoubleStringDoubleBoolean2Int(a1: dFun, a2: sFun, a3: dFun, a4: bFun): iFun = iFunDummy
  def helperDoubleStringStringInt2Int(a1: dFun, a2: sFun, a3: sFun, a4: iFun): iFun = iFunDummy
  def helperDoubleStringStringLong2Int(a1: dFun, a2: sFun, a3: sFun, a4: lFun): iFun = iFunDummy
  def helperDoubleStringStringDouble2Int(a1: dFun, a2: sFun, a3: sFun, a4: dFun): iFun = iFunDummy
  def helperDoubleStringStringString2Int(a1: dFun, a2: sFun, a3: sFun, a4: sFun): iFun = iFunDummy
  def helperDoubleStringStringBoolean2Int(a1: dFun, a2: sFun, a3: sFun, a4: bFun): iFun = iFunDummy
  def helperDoubleStringBooleanInt2Int(a1: dFun, a2: sFun, a3: bFun, a4: iFun): iFun = iFunDummy
  def helperDoubleStringBooleanLong2Int(a1: dFun, a2: sFun, a3: bFun, a4: lFun): iFun = iFunDummy
  def helperDoubleStringBooleanDouble2Int(a1: dFun, a2: sFun, a3: bFun, a4: dFun): iFun = iFunDummy
  def helperDoubleStringBooleanString2Int(a1: dFun, a2: sFun, a3: bFun, a4: sFun): iFun = iFunDummy
  def helperDoubleStringBooleanBoolean2Int(a1: dFun, a2: sFun, a3: bFun, a4: bFun): iFun = iFunDummy
  def helperDoubleBooleanIntInt2Int(a1: dFun, a2: bFun, a3: iFun, a4: iFun): iFun = iFunDummy
  def helperDoubleBooleanIntLong2Int(a1: dFun, a2: bFun, a3: iFun, a4: lFun): iFun = iFunDummy
  def helperDoubleBooleanIntDouble2Int(a1: dFun, a2: bFun, a3: iFun, a4: dFun): iFun = iFunDummy
  def helperDoubleBooleanIntString2Int(a1: dFun, a2: bFun, a3: iFun, a4: sFun): iFun = iFunDummy
  def helperDoubleBooleanIntBoolean2Int(a1: dFun, a2: bFun, a3: iFun, a4: bFun): iFun = iFunDummy
  def helperDoubleBooleanLongInt2Int(a1: dFun, a2: bFun, a3: lFun, a4: iFun): iFun = iFunDummy
  def helperDoubleBooleanLongLong2Int(a1: dFun, a2: bFun, a3: lFun, a4: lFun): iFun = iFunDummy
  def helperDoubleBooleanLongDouble2Int(a1: dFun, a2: bFun, a3: lFun, a4: dFun): iFun = iFunDummy
  def helperDoubleBooleanLongString2Int(a1: dFun, a2: bFun, a3: lFun, a4: sFun): iFun = iFunDummy
  def helperDoubleBooleanLongBoolean2Int(a1: dFun, a2: bFun, a3: lFun, a4: bFun): iFun = iFunDummy
  def helperDoubleBooleanDoubleInt2Int(a1: dFun, a2: bFun, a3: dFun, a4: iFun): iFun = iFunDummy
  def helperDoubleBooleanDoubleLong2Int(a1: dFun, a2: bFun, a3: dFun, a4: lFun): iFun = iFunDummy
  def helperDoubleBooleanDoubleDouble2Int(a1: dFun, a2: bFun, a3: dFun, a4: dFun): iFun = iFunDummy
  def helperDoubleBooleanDoubleString2Int(a1: dFun, a2: bFun, a3: dFun, a4: sFun): iFun = iFunDummy
  def helperDoubleBooleanDoubleBoolean2Int(a1: dFun, a2: bFun, a3: dFun, a4: bFun): iFun = iFunDummy
  def helperDoubleBooleanStringInt2Int(a1: dFun, a2: bFun, a3: sFun, a4: iFun): iFun = iFunDummy
  def helperDoubleBooleanStringLong2Int(a1: dFun, a2: bFun, a3: sFun, a4: lFun): iFun = iFunDummy
  def helperDoubleBooleanStringDouble2Int(a1: dFun, a2: bFun, a3: sFun, a4: dFun): iFun = iFunDummy
  def helperDoubleBooleanStringString2Int(a1: dFun, a2: bFun, a3: sFun, a4: sFun): iFun = iFunDummy
  def helperDoubleBooleanStringBoolean2Int(a1: dFun, a2: bFun, a3: sFun, a4: bFun): iFun = iFunDummy
  def helperDoubleBooleanBooleanInt2Int(a1: dFun, a2: bFun, a3: bFun, a4: iFun): iFun = iFunDummy
  def helperDoubleBooleanBooleanLong2Int(a1: dFun, a2: bFun, a3: bFun, a4: lFun): iFun = iFunDummy
  def helperDoubleBooleanBooleanDouble2Int(a1: dFun, a2: bFun, a3: bFun, a4: dFun): iFun = iFunDummy
  def helperDoubleBooleanBooleanString2Int(a1: dFun, a2: bFun, a3: bFun, a4: sFun): iFun = iFunDummy
  def helperDoubleBooleanBooleanBoolean2Int(a1: dFun, a2: bFun, a3: bFun, a4: bFun): iFun = iFunDummy
  def helperStringIntIntInt2Int(a1: sFun, a2: iFun, a3: iFun, a4: iFun): iFun = iFunDummy
  def helperStringIntIntLong2Int(a1: sFun, a2: iFun, a3: iFun, a4: lFun): iFun = iFunDummy
  def helperStringIntIntDouble2Int(a1: sFun, a2: iFun, a3: iFun, a4: dFun): iFun = iFunDummy
  def helperStringIntIntString2Int(a1: sFun, a2: iFun, a3: iFun, a4: sFun): iFun = iFunDummy
  def helperStringIntIntBoolean2Int(a1: sFun, a2: iFun, a3: iFun, a4: bFun): iFun = iFunDummy
  def helperStringIntLongInt2Int(a1: sFun, a2: iFun, a3: lFun, a4: iFun): iFun = iFunDummy
  def helperStringIntLongLong2Int(a1: sFun, a2: iFun, a3: lFun, a4: lFun): iFun = iFunDummy
  def helperStringIntLongDouble2Int(a1: sFun, a2: iFun, a3: lFun, a4: dFun): iFun = iFunDummy
  def helperStringIntLongString2Int(a1: sFun, a2: iFun, a3: lFun, a4: sFun): iFun = iFunDummy
  def helperStringIntLongBoolean2Int(a1: sFun, a2: iFun, a3: lFun, a4: bFun): iFun = iFunDummy
  def helperStringIntDoubleInt2Int(a1: sFun, a2: iFun, a3: dFun, a4: iFun): iFun = iFunDummy
  def helperStringIntDoubleLong2Int(a1: sFun, a2: iFun, a3: dFun, a4: lFun): iFun = iFunDummy
  def helperStringIntDoubleDouble2Int(a1: sFun, a2: iFun, a3: dFun, a4: dFun): iFun = iFunDummy
  def helperStringIntDoubleString2Int(a1: sFun, a2: iFun, a3: dFun, a4: sFun): iFun = iFunDummy
  def helperStringIntDoubleBoolean2Int(a1: sFun, a2: iFun, a3: dFun, a4: bFun): iFun = iFunDummy
  def helperStringIntStringInt2Int(a1: sFun, a2: iFun, a3: sFun, a4: iFun): iFun = iFunDummy
  def helperStringIntStringLong2Int(a1: sFun, a2: iFun, a3: sFun, a4: lFun): iFun = iFunDummy
  def helperStringIntStringDouble2Int(a1: sFun, a2: iFun, a3: sFun, a4: dFun): iFun = iFunDummy
  def helperStringIntStringString2Int(a1: sFun, a2: iFun, a3: sFun, a4: sFun): iFun = iFunDummy
  def helperStringIntStringBoolean2Int(a1: sFun, a2: iFun, a3: sFun, a4: bFun): iFun = iFunDummy
  def helperStringIntBooleanInt2Int(a1: sFun, a2: iFun, a3: bFun, a4: iFun): iFun = iFunDummy
  def helperStringIntBooleanLong2Int(a1: sFun, a2: iFun, a3: bFun, a4: lFun): iFun = iFunDummy
  def helperStringIntBooleanDouble2Int(a1: sFun, a2: iFun, a3: bFun, a4: dFun): iFun = iFunDummy
  def helperStringIntBooleanString2Int(a1: sFun, a2: iFun, a3: bFun, a4: sFun): iFun = iFunDummy
  def helperStringIntBooleanBoolean2Int(a1: sFun, a2: iFun, a3: bFun, a4: bFun): iFun = iFunDummy
  def helperStringLongIntInt2Int(a1: sFun, a2: lFun, a3: iFun, a4: iFun): iFun = iFunDummy
  def helperStringLongIntLong2Int(a1: sFun, a2: lFun, a3: iFun, a4: lFun): iFun = iFunDummy
  def helperStringLongIntDouble2Int(a1: sFun, a2: lFun, a3: iFun, a4: dFun): iFun = iFunDummy
  def helperStringLongIntString2Int(a1: sFun, a2: lFun, a3: iFun, a4: sFun): iFun = iFunDummy
  def helperStringLongIntBoolean2Int(a1: sFun, a2: lFun, a3: iFun, a4: bFun): iFun = iFunDummy
  def helperStringLongLongInt2Int(a1: sFun, a2: lFun, a3: lFun, a4: iFun): iFun = iFunDummy
  def helperStringLongLongLong2Int(a1: sFun, a2: lFun, a3: lFun, a4: lFun): iFun = iFunDummy
  def helperStringLongLongDouble2Int(a1: sFun, a2: lFun, a3: lFun, a4: dFun): iFun = iFunDummy
  def helperStringLongLongString2Int(a1: sFun, a2: lFun, a3: lFun, a4: sFun): iFun = iFunDummy
  def helperStringLongLongBoolean2Int(a1: sFun, a2: lFun, a3: lFun, a4: bFun): iFun = iFunDummy
  def helperStringLongDoubleInt2Int(a1: sFun, a2: lFun, a3: dFun, a4: iFun): iFun = iFunDummy
  def helperStringLongDoubleLong2Int(a1: sFun, a2: lFun, a3: dFun, a4: lFun): iFun = iFunDummy
  def helperStringLongDoubleDouble2Int(a1: sFun, a2: lFun, a3: dFun, a4: dFun): iFun = iFunDummy
  def helperStringLongDoubleString2Int(a1: sFun, a2: lFun, a3: dFun, a4: sFun): iFun = iFunDummy
  def helperStringLongDoubleBoolean2Int(a1: sFun, a2: lFun, a3: dFun, a4: bFun): iFun = iFunDummy
  def helperStringLongStringInt2Int(a1: sFun, a2: lFun, a3: sFun, a4: iFun): iFun = iFunDummy
  def helperStringLongStringLong2Int(a1: sFun, a2: lFun, a3: sFun, a4: lFun): iFun = iFunDummy
  def helperStringLongStringDouble2Int(a1: sFun, a2: lFun, a3: sFun, a4: dFun): iFun = iFunDummy
  def helperStringLongStringString2Int(a1: sFun, a2: lFun, a3: sFun, a4: sFun): iFun = iFunDummy
  def helperStringLongStringBoolean2Int(a1: sFun, a2: lFun, a3: sFun, a4: bFun): iFun = iFunDummy
  def helperStringLongBooleanInt2Int(a1: sFun, a2: lFun, a3: bFun, a4: iFun): iFun = iFunDummy
  def helperStringLongBooleanLong2Int(a1: sFun, a2: lFun, a3: bFun, a4: lFun): iFun = iFunDummy
  def helperStringLongBooleanDouble2Int(a1: sFun, a2: lFun, a3: bFun, a4: dFun): iFun = iFunDummy
  def helperStringLongBooleanString2Int(a1: sFun, a2: lFun, a3: bFun, a4: sFun): iFun = iFunDummy
  def helperStringLongBooleanBoolean2Int(a1: sFun, a2: lFun, a3: bFun, a4: bFun): iFun = iFunDummy
  def helperStringDoubleIntInt2Int(a1: sFun, a2: dFun, a3: iFun, a4: iFun): iFun = iFunDummy
  def helperStringDoubleIntLong2Int(a1: sFun, a2: dFun, a3: iFun, a4: lFun): iFun = iFunDummy
  def helperStringDoubleIntDouble2Int(a1: sFun, a2: dFun, a3: iFun, a4: dFun): iFun = iFunDummy
  def helperStringDoubleIntString2Int(a1: sFun, a2: dFun, a3: iFun, a4: sFun): iFun = iFunDummy
  def helperStringDoubleIntBoolean2Int(a1: sFun, a2: dFun, a3: iFun, a4: bFun): iFun = iFunDummy
  def helperStringDoubleLongInt2Int(a1: sFun, a2: dFun, a3: lFun, a4: iFun): iFun = iFunDummy
  def helperStringDoubleLongLong2Int(a1: sFun, a2: dFun, a3: lFun, a4: lFun): iFun = iFunDummy
  def helperStringDoubleLongDouble2Int(a1: sFun, a2: dFun, a3: lFun, a4: dFun): iFun = iFunDummy
  def helperStringDoubleLongString2Int(a1: sFun, a2: dFun, a3: lFun, a4: sFun): iFun = iFunDummy
  def helperStringDoubleLongBoolean2Int(a1: sFun, a2: dFun, a3: lFun, a4: bFun): iFun = iFunDummy
  def helperStringDoubleDoubleInt2Int(a1: sFun, a2: dFun, a3: dFun, a4: iFun): iFun = iFunDummy
  def helperStringDoubleDoubleLong2Int(a1: sFun, a2: dFun, a3: dFun, a4: lFun): iFun = iFunDummy
  def helperStringDoubleDoubleDouble2Int(a1: sFun, a2: dFun, a3: dFun, a4: dFun): iFun = iFunDummy
  def helperStringDoubleDoubleString2Int(a1: sFun, a2: dFun, a3: dFun, a4: sFun): iFun = iFunDummy
  def helperStringDoubleDoubleBoolean2Int(a1: sFun, a2: dFun, a3: dFun, a4: bFun): iFun = iFunDummy
  def helperStringDoubleStringInt2Int(a1: sFun, a2: dFun, a3: sFun, a4: iFun): iFun = iFunDummy
  def helperStringDoubleStringLong2Int(a1: sFun, a2: dFun, a3: sFun, a4: lFun): iFun = iFunDummy
  def helperStringDoubleStringDouble2Int(a1: sFun, a2: dFun, a3: sFun, a4: dFun): iFun = iFunDummy
  def helperStringDoubleStringString2Int(a1: sFun, a2: dFun, a3: sFun, a4: sFun): iFun = iFunDummy
  def helperStringDoubleStringBoolean2Int(a1: sFun, a2: dFun, a3: sFun, a4: bFun): iFun = iFunDummy
  def helperStringDoubleBooleanInt2Int(a1: sFun, a2: dFun, a3: bFun, a4: iFun): iFun = iFunDummy
  def helperStringDoubleBooleanLong2Int(a1: sFun, a2: dFun, a3: bFun, a4: lFun): iFun = iFunDummy
  def helperStringDoubleBooleanDouble2Int(a1: sFun, a2: dFun, a3: bFun, a4: dFun): iFun = iFunDummy
  def helperStringDoubleBooleanString2Int(a1: sFun, a2: dFun, a3: bFun, a4: sFun): iFun = iFunDummy
  def helperStringDoubleBooleanBoolean2Int(a1: sFun, a2: dFun, a3: bFun, a4: bFun): iFun = iFunDummy
  def helperStringStringIntInt2Int(a1: sFun, a2: sFun, a3: iFun, a4: iFun): iFun = iFunDummy
  def helperStringStringIntLong2Int(a1: sFun, a2: sFun, a3: iFun, a4: lFun): iFun = iFunDummy
  def helperStringStringIntDouble2Int(a1: sFun, a2: sFun, a3: iFun, a4: dFun): iFun = iFunDummy
  def helperStringStringIntString2Int(a1: sFun, a2: sFun, a3: iFun, a4: sFun): iFun = iFunDummy
  def helperStringStringIntBoolean2Int(a1: sFun, a2: sFun, a3: iFun, a4: bFun): iFun = iFunDummy
  def helperStringStringLongInt2Int(a1: sFun, a2: sFun, a3: lFun, a4: iFun): iFun = iFunDummy
  def helperStringStringLongLong2Int(a1: sFun, a2: sFun, a3: lFun, a4: lFun): iFun = iFunDummy
  def helperStringStringLongDouble2Int(a1: sFun, a2: sFun, a3: lFun, a4: dFun): iFun = iFunDummy
  def helperStringStringLongString2Int(a1: sFun, a2: sFun, a3: lFun, a4: sFun): iFun = iFunDummy
  def helperStringStringLongBoolean2Int(a1: sFun, a2: sFun, a3: lFun, a4: bFun): iFun = iFunDummy
  def helperStringStringDoubleInt2Int(a1: sFun, a2: sFun, a3: dFun, a4: iFun): iFun = iFunDummy
  def helperStringStringDoubleLong2Int(a1: sFun, a2: sFun, a3: dFun, a4: lFun): iFun = iFunDummy
  def helperStringStringDoubleDouble2Int(a1: sFun, a2: sFun, a3: dFun, a4: dFun): iFun = iFunDummy
  def helperStringStringDoubleString2Int(a1: sFun, a2: sFun, a3: dFun, a4: sFun): iFun = iFunDummy
  def helperStringStringDoubleBoolean2Int(a1: sFun, a2: sFun, a3: dFun, a4: bFun): iFun = iFunDummy
  def helperStringStringStringInt2Int(a1: sFun, a2: sFun, a3: sFun, a4: iFun): iFun = iFunDummy
  def helperStringStringStringLong2Int(a1: sFun, a2: sFun, a3: sFun, a4: lFun): iFun = iFunDummy
  def helperStringStringStringDouble2Int(a1: sFun, a2: sFun, a3: sFun, a4: dFun): iFun = iFunDummy
  def helperStringStringStringString2Int(a1: sFun, a2: sFun, a3: sFun, a4: sFun): iFun = iFunDummy
  def helperStringStringStringBoolean2Int(a1: sFun, a2: sFun, a3: sFun, a4: bFun): iFun = iFunDummy
  def helperStringStringBooleanInt2Int(a1: sFun, a2: sFun, a3: bFun, a4: iFun): iFun = iFunDummy
  def helperStringStringBooleanLong2Int(a1: sFun, a2: sFun, a3: bFun, a4: lFun): iFun = iFunDummy
  def helperStringStringBooleanDouble2Int(a1: sFun, a2: sFun, a3: bFun, a4: dFun): iFun = iFunDummy
  def helperStringStringBooleanString2Int(a1: sFun, a2: sFun, a3: bFun, a4: sFun): iFun = iFunDummy
  def helperStringStringBooleanBoolean2Int(a1: sFun, a2: sFun, a3: bFun, a4: bFun): iFun = iFunDummy
  def helperStringBooleanIntInt2Int(a1: sFun, a2: bFun, a3: iFun, a4: iFun): iFun = iFunDummy
  def helperStringBooleanIntLong2Int(a1: sFun, a2: bFun, a3: iFun, a4: lFun): iFun = iFunDummy
  def helperStringBooleanIntDouble2Int(a1: sFun, a2: bFun, a3: iFun, a4: dFun): iFun = iFunDummy
  def helperStringBooleanIntString2Int(a1: sFun, a2: bFun, a3: iFun, a4: sFun): iFun = iFunDummy
  def helperStringBooleanIntBoolean2Int(a1: sFun, a2: bFun, a3: iFun, a4: bFun): iFun = iFunDummy
  def helperStringBooleanLongInt2Int(a1: sFun, a2: bFun, a3: lFun, a4: iFun): iFun = iFunDummy
  def helperStringBooleanLongLong2Int(a1: sFun, a2: bFun, a3: lFun, a4: lFun): iFun = iFunDummy
  def helperStringBooleanLongDouble2Int(a1: sFun, a2: bFun, a3: lFun, a4: dFun): iFun = iFunDummy
  def helperStringBooleanLongString2Int(a1: sFun, a2: bFun, a3: lFun, a4: sFun): iFun = iFunDummy
  def helperStringBooleanLongBoolean2Int(a1: sFun, a2: bFun, a3: lFun, a4: bFun): iFun = iFunDummy
  def helperStringBooleanDoubleInt2Int(a1: sFun, a2: bFun, a3: dFun, a4: iFun): iFun = iFunDummy
  def helperStringBooleanDoubleLong2Int(a1: sFun, a2: bFun, a3: dFun, a4: lFun): iFun = iFunDummy
  def helperStringBooleanDoubleDouble2Int(a1: sFun, a2: bFun, a3: dFun, a4: dFun): iFun = iFunDummy
  def helperStringBooleanDoubleString2Int(a1: sFun, a2: bFun, a3: dFun, a4: sFun): iFun = iFunDummy
  def helperStringBooleanDoubleBoolean2Int(a1: sFun, a2: bFun, a3: dFun, a4: bFun): iFun = iFunDummy
  def helperStringBooleanStringInt2Int(a1: sFun, a2: bFun, a3: sFun, a4: iFun): iFun = iFunDummy
  def helperStringBooleanStringLong2Int(a1: sFun, a2: bFun, a3: sFun, a4: lFun): iFun = iFunDummy
  def helperStringBooleanStringDouble2Int(a1: sFun, a2: bFun, a3: sFun, a4: dFun): iFun = iFunDummy
  def helperStringBooleanStringString2Int(a1: sFun, a2: bFun, a3: sFun, a4: sFun): iFun = iFunDummy
  def helperStringBooleanStringBoolean2Int(a1: sFun, a2: bFun, a3: sFun, a4: bFun): iFun = iFunDummy
  def helperStringBooleanBooleanInt2Int(a1: sFun, a2: bFun, a3: bFun, a4: iFun): iFun = iFunDummy
  def helperStringBooleanBooleanLong2Int(a1: sFun, a2: bFun, a3: bFun, a4: lFun): iFun = iFunDummy
  def helperStringBooleanBooleanDouble2Int(a1: sFun, a2: bFun, a3: bFun, a4: dFun): iFun = iFunDummy
  def helperStringBooleanBooleanString2Int(a1: sFun, a2: bFun, a3: bFun, a4: sFun): iFun = iFunDummy
  def helperStringBooleanBooleanBoolean2Int(a1: sFun, a2: bFun, a3: bFun, a4: bFun): iFun = iFunDummy
  def helperBooleanIntIntInt2Int(a1: bFun, a2: iFun, a3: iFun, a4: iFun): iFun = iFunDummy
  def helperBooleanIntIntLong2Int(a1: bFun, a2: iFun, a3: iFun, a4: lFun): iFun = iFunDummy
  def helperBooleanIntIntDouble2Int(a1: bFun, a2: iFun, a3: iFun, a4: dFun): iFun = iFunDummy
  def helperBooleanIntIntString2Int(a1: bFun, a2: iFun, a3: iFun, a4: sFun): iFun = iFunDummy
  def helperBooleanIntIntBoolean2Int(a1: bFun, a2: iFun, a3: iFun, a4: bFun): iFun = iFunDummy
  def helperBooleanIntLongInt2Int(a1: bFun, a2: iFun, a3: lFun, a4: iFun): iFun = iFunDummy
  def helperBooleanIntLongLong2Int(a1: bFun, a2: iFun, a3: lFun, a4: lFun): iFun = iFunDummy
  def helperBooleanIntLongDouble2Int(a1: bFun, a2: iFun, a3: lFun, a4: dFun): iFun = iFunDummy
  def helperBooleanIntLongString2Int(a1: bFun, a2: iFun, a3: lFun, a4: sFun): iFun = iFunDummy
  def helperBooleanIntLongBoolean2Int(a1: bFun, a2: iFun, a3: lFun, a4: bFun): iFun = iFunDummy
  def helperBooleanIntDoubleInt2Int(a1: bFun, a2: iFun, a3: dFun, a4: iFun): iFun = iFunDummy
  def helperBooleanIntDoubleLong2Int(a1: bFun, a2: iFun, a3: dFun, a4: lFun): iFun = iFunDummy
  def helperBooleanIntDoubleDouble2Int(a1: bFun, a2: iFun, a3: dFun, a4: dFun): iFun = iFunDummy
  def helperBooleanIntDoubleString2Int(a1: bFun, a2: iFun, a3: dFun, a4: sFun): iFun = iFunDummy
  def helperBooleanIntDoubleBoolean2Int(a1: bFun, a2: iFun, a3: dFun, a4: bFun): iFun = iFunDummy
  def helperBooleanIntStringInt2Int(a1: bFun, a2: iFun, a3: sFun, a4: iFun): iFun = iFunDummy
  def helperBooleanIntStringLong2Int(a1: bFun, a2: iFun, a3: sFun, a4: lFun): iFun = iFunDummy
  def helperBooleanIntStringDouble2Int(a1: bFun, a2: iFun, a3: sFun, a4: dFun): iFun = iFunDummy
  def helperBooleanIntStringString2Int(a1: bFun, a2: iFun, a3: sFun, a4: sFun): iFun = iFunDummy
  def helperBooleanIntStringBoolean2Int(a1: bFun, a2: iFun, a3: sFun, a4: bFun): iFun = iFunDummy
  def helperBooleanIntBooleanInt2Int(a1: bFun, a2: iFun, a3: bFun, a4: iFun): iFun = iFunDummy
  def helperBooleanIntBooleanLong2Int(a1: bFun, a2: iFun, a3: bFun, a4: lFun): iFun = iFunDummy
  def helperBooleanIntBooleanDouble2Int(a1: bFun, a2: iFun, a3: bFun, a4: dFun): iFun = iFunDummy
  def helperBooleanIntBooleanString2Int(a1: bFun, a2: iFun, a3: bFun, a4: sFun): iFun = iFunDummy
  def helperBooleanIntBooleanBoolean2Int(a1: bFun, a2: iFun, a3: bFun, a4: bFun): iFun = iFunDummy
  def helperBooleanLongIntInt2Int(a1: bFun, a2: lFun, a3: iFun, a4: iFun): iFun = iFunDummy
  def helperBooleanLongIntLong2Int(a1: bFun, a2: lFun, a3: iFun, a4: lFun): iFun = iFunDummy
  def helperBooleanLongIntDouble2Int(a1: bFun, a2: lFun, a3: iFun, a4: dFun): iFun = iFunDummy
  def helperBooleanLongIntString2Int(a1: bFun, a2: lFun, a3: iFun, a4: sFun): iFun = iFunDummy
  def helperBooleanLongIntBoolean2Int(a1: bFun, a2: lFun, a3: iFun, a4: bFun): iFun = iFunDummy
  def helperBooleanLongLongInt2Int(a1: bFun, a2: lFun, a3: lFun, a4: iFun): iFun = iFunDummy
  def helperBooleanLongLongLong2Int(a1: bFun, a2: lFun, a3: lFun, a4: lFun): iFun = iFunDummy
  def helperBooleanLongLongDouble2Int(a1: bFun, a2: lFun, a3: lFun, a4: dFun): iFun = iFunDummy
  def helperBooleanLongLongString2Int(a1: bFun, a2: lFun, a3: lFun, a4: sFun): iFun = iFunDummy
  def helperBooleanLongLongBoolean2Int(a1: bFun, a2: lFun, a3: lFun, a4: bFun): iFun = iFunDummy
  def helperBooleanLongDoubleInt2Int(a1: bFun, a2: lFun, a3: dFun, a4: iFun): iFun = iFunDummy
  def helperBooleanLongDoubleLong2Int(a1: bFun, a2: lFun, a3: dFun, a4: lFun): iFun = iFunDummy
  def helperBooleanLongDoubleDouble2Int(a1: bFun, a2: lFun, a3: dFun, a4: dFun): iFun = iFunDummy
  def helperBooleanLongDoubleString2Int(a1: bFun, a2: lFun, a3: dFun, a4: sFun): iFun = iFunDummy
  def helperBooleanLongDoubleBoolean2Int(a1: bFun, a2: lFun, a3: dFun, a4: bFun): iFun = iFunDummy
  def helperBooleanLongStringInt2Int(a1: bFun, a2: lFun, a3: sFun, a4: iFun): iFun = iFunDummy
  def helperBooleanLongStringLong2Int(a1: bFun, a2: lFun, a3: sFun, a4: lFun): iFun = iFunDummy
  def helperBooleanLongStringDouble2Int(a1: bFun, a2: lFun, a3: sFun, a4: dFun): iFun = iFunDummy
  def helperBooleanLongStringString2Int(a1: bFun, a2: lFun, a3: sFun, a4: sFun): iFun = iFunDummy
  def helperBooleanLongStringBoolean2Int(a1: bFun, a2: lFun, a3: sFun, a4: bFun): iFun = iFunDummy
  def helperBooleanLongBooleanInt2Int(a1: bFun, a2: lFun, a3: bFun, a4: iFun): iFun = iFunDummy
  def helperBooleanLongBooleanLong2Int(a1: bFun, a2: lFun, a3: bFun, a4: lFun): iFun = iFunDummy
  def helperBooleanLongBooleanDouble2Int(a1: bFun, a2: lFun, a3: bFun, a4: dFun): iFun = iFunDummy
  def helperBooleanLongBooleanString2Int(a1: bFun, a2: lFun, a3: bFun, a4: sFun): iFun = iFunDummy
  def helperBooleanLongBooleanBoolean2Int(a1: bFun, a2: lFun, a3: bFun, a4: bFun): iFun = iFunDummy
  def helperBooleanDoubleIntInt2Int(a1: bFun, a2: dFun, a3: iFun, a4: iFun): iFun = iFunDummy
  def helperBooleanDoubleIntLong2Int(a1: bFun, a2: dFun, a3: iFun, a4: lFun): iFun = iFunDummy
  def helperBooleanDoubleIntDouble2Int(a1: bFun, a2: dFun, a3: iFun, a4: dFun): iFun = iFunDummy
  def helperBooleanDoubleIntString2Int(a1: bFun, a2: dFun, a3: iFun, a4: sFun): iFun = iFunDummy
  def helperBooleanDoubleIntBoolean2Int(a1: bFun, a2: dFun, a3: iFun, a4: bFun): iFun = iFunDummy
  def helperBooleanDoubleLongInt2Int(a1: bFun, a2: dFun, a3: lFun, a4: iFun): iFun = iFunDummy
  def helperBooleanDoubleLongLong2Int(a1: bFun, a2: dFun, a3: lFun, a4: lFun): iFun = iFunDummy
  def helperBooleanDoubleLongDouble2Int(a1: bFun, a2: dFun, a3: lFun, a4: dFun): iFun = iFunDummy
  def helperBooleanDoubleLongString2Int(a1: bFun, a2: dFun, a3: lFun, a4: sFun): iFun = iFunDummy
  def helperBooleanDoubleLongBoolean2Int(a1: bFun, a2: dFun, a3: lFun, a4: bFun): iFun = iFunDummy
  def helperBooleanDoubleDoubleInt2Int(a1: bFun, a2: dFun, a3: dFun, a4: iFun): iFun = iFunDummy
  def helperBooleanDoubleDoubleLong2Int(a1: bFun, a2: dFun, a3: dFun, a4: lFun): iFun = iFunDummy
  def helperBooleanDoubleDoubleDouble2Int(a1: bFun, a2: dFun, a3: dFun, a4: dFun): iFun = iFunDummy
  def helperBooleanDoubleDoubleString2Int(a1: bFun, a2: dFun, a3: dFun, a4: sFun): iFun = iFunDummy
  def helperBooleanDoubleDoubleBoolean2Int(a1: bFun, a2: dFun, a3: dFun, a4: bFun): iFun = iFunDummy
  def helperBooleanDoubleStringInt2Int(a1: bFun, a2: dFun, a3: sFun, a4: iFun): iFun = iFunDummy
  def helperBooleanDoubleStringLong2Int(a1: bFun, a2: dFun, a3: sFun, a4: lFun): iFun = iFunDummy
  def helperBooleanDoubleStringDouble2Int(a1: bFun, a2: dFun, a3: sFun, a4: dFun): iFun = iFunDummy
  def helperBooleanDoubleStringString2Int(a1: bFun, a2: dFun, a3: sFun, a4: sFun): iFun = iFunDummy
  def helperBooleanDoubleStringBoolean2Int(a1: bFun, a2: dFun, a3: sFun, a4: bFun): iFun = iFunDummy
  def helperBooleanDoubleBooleanInt2Int(a1: bFun, a2: dFun, a3: bFun, a4: iFun): iFun = iFunDummy
  def helperBooleanDoubleBooleanLong2Int(a1: bFun, a2: dFun, a3: bFun, a4: lFun): iFun = iFunDummy
  def helperBooleanDoubleBooleanDouble2Int(a1: bFun, a2: dFun, a3: bFun, a4: dFun): iFun = iFunDummy
  def helperBooleanDoubleBooleanString2Int(a1: bFun, a2: dFun, a3: bFun, a4: sFun): iFun = iFunDummy
  def helperBooleanDoubleBooleanBoolean2Int(a1: bFun, a2: dFun, a3: bFun, a4: bFun): iFun = iFunDummy
  def helperBooleanStringIntInt2Int(a1: bFun, a2: sFun, a3: iFun, a4: iFun): iFun = iFunDummy
  def helperBooleanStringIntLong2Int(a1: bFun, a2: sFun, a3: iFun, a4: lFun): iFun = iFunDummy
  def helperBooleanStringIntDouble2Int(a1: bFun, a2: sFun, a3: iFun, a4: dFun): iFun = iFunDummy
  def helperBooleanStringIntString2Int(a1: bFun, a2: sFun, a3: iFun, a4: sFun): iFun = iFunDummy
  def helperBooleanStringIntBoolean2Int(a1: bFun, a2: sFun, a3: iFun, a4: bFun): iFun = iFunDummy
  def helperBooleanStringLongInt2Int(a1: bFun, a2: sFun, a3: lFun, a4: iFun): iFun = iFunDummy
  def helperBooleanStringLongLong2Int(a1: bFun, a2: sFun, a3: lFun, a4: lFun): iFun = iFunDummy
  def helperBooleanStringLongDouble2Int(a1: bFun, a2: sFun, a3: lFun, a4: dFun): iFun = iFunDummy
  def helperBooleanStringLongString2Int(a1: bFun, a2: sFun, a3: lFun, a4: sFun): iFun = iFunDummy
  def helperBooleanStringLongBoolean2Int(a1: bFun, a2: sFun, a3: lFun, a4: bFun): iFun = iFunDummy
  def helperBooleanStringDoubleInt2Int(a1: bFun, a2: sFun, a3: dFun, a4: iFun): iFun = iFunDummy
  def helperBooleanStringDoubleLong2Int(a1: bFun, a2: sFun, a3: dFun, a4: lFun): iFun = iFunDummy
  def helperBooleanStringDoubleDouble2Int(a1: bFun, a2: sFun, a3: dFun, a4: dFun): iFun = iFunDummy
  def helperBooleanStringDoubleString2Int(a1: bFun, a2: sFun, a3: dFun, a4: sFun): iFun = iFunDummy
  def helperBooleanStringDoubleBoolean2Int(a1: bFun, a2: sFun, a3: dFun, a4: bFun): iFun = iFunDummy
  def helperBooleanStringStringInt2Int(a1: bFun, a2: sFun, a3: sFun, a4: iFun): iFun = iFunDummy
  def helperBooleanStringStringLong2Int(a1: bFun, a2: sFun, a3: sFun, a4: lFun): iFun = iFunDummy
  def helperBooleanStringStringDouble2Int(a1: bFun, a2: sFun, a3: sFun, a4: dFun): iFun = iFunDummy
  def helperBooleanStringStringString2Int(a1: bFun, a2: sFun, a3: sFun, a4: sFun): iFun = iFunDummy
  def helperBooleanStringStringBoolean2Int(a1: bFun, a2: sFun, a3: sFun, a4: bFun): iFun = iFunDummy
  def helperBooleanStringBooleanInt2Int(a1: bFun, a2: sFun, a3: bFun, a4: iFun): iFun = iFunDummy
  def helperBooleanStringBooleanLong2Int(a1: bFun, a2: sFun, a3: bFun, a4: lFun): iFun = iFunDummy
  def helperBooleanStringBooleanDouble2Int(a1: bFun, a2: sFun, a3: bFun, a4: dFun): iFun = iFunDummy
  def helperBooleanStringBooleanString2Int(a1: bFun, a2: sFun, a3: bFun, a4: sFun): iFun = iFunDummy
  def helperBooleanStringBooleanBoolean2Int(a1: bFun, a2: sFun, a3: bFun, a4: bFun): iFun = iFunDummy
  def helperBooleanBooleanIntInt2Int(a1: bFun, a2: bFun, a3: iFun, a4: iFun): iFun = iFunDummy
  def helperBooleanBooleanIntLong2Int(a1: bFun, a2: bFun, a3: iFun, a4: lFun): iFun = iFunDummy
  def helperBooleanBooleanIntDouble2Int(a1: bFun, a2: bFun, a3: iFun, a4: dFun): iFun = iFunDummy
  def helperBooleanBooleanIntString2Int(a1: bFun, a2: bFun, a3: iFun, a4: sFun): iFun = iFunDummy
  def helperBooleanBooleanIntBoolean2Int(a1: bFun, a2: bFun, a3: iFun, a4: bFun): iFun = iFunDummy
  def helperBooleanBooleanLongInt2Int(a1: bFun, a2: bFun, a3: lFun, a4: iFun): iFun = iFunDummy
  def helperBooleanBooleanLongLong2Int(a1: bFun, a2: bFun, a3: lFun, a4: lFun): iFun = iFunDummy
  def helperBooleanBooleanLongDouble2Int(a1: bFun, a2: bFun, a3: lFun, a4: dFun): iFun = iFunDummy
  def helperBooleanBooleanLongString2Int(a1: bFun, a2: bFun, a3: lFun, a4: sFun): iFun = iFunDummy
  def helperBooleanBooleanLongBoolean2Int(a1: bFun, a2: bFun, a3: lFun, a4: bFun): iFun = iFunDummy
  def helperBooleanBooleanDoubleInt2Int(a1: bFun, a2: bFun, a3: dFun, a4: iFun): iFun = iFunDummy
  def helperBooleanBooleanDoubleLong2Int(a1: bFun, a2: bFun, a3: dFun, a4: lFun): iFun = iFunDummy
  def helperBooleanBooleanDoubleDouble2Int(a1: bFun, a2: bFun, a3: dFun, a4: dFun): iFun = iFunDummy
  def helperBooleanBooleanDoubleString2Int(a1: bFun, a2: bFun, a3: dFun, a4: sFun): iFun = iFunDummy
  def helperBooleanBooleanDoubleBoolean2Int(a1: bFun, a2: bFun, a3: dFun, a4: bFun): iFun = iFunDummy
  def helperBooleanBooleanStringInt2Int(a1: bFun, a2: bFun, a3: sFun, a4: iFun): iFun = iFunDummy
  def helperBooleanBooleanStringLong2Int(a1: bFun, a2: bFun, a3: sFun, a4: lFun): iFun = iFunDummy
  def helperBooleanBooleanStringDouble2Int(a1: bFun, a2: bFun, a3: sFun, a4: dFun): iFun = iFunDummy
  def helperBooleanBooleanStringString2Int(a1: bFun, a2: bFun, a3: sFun, a4: sFun): iFun = iFunDummy
  def helperBooleanBooleanStringBoolean2Int(a1: bFun, a2: bFun, a3: sFun, a4: bFun): iFun = iFunDummy
  def helperBooleanBooleanBooleanInt2Int(a1: bFun, a2: bFun, a3: bFun, a4: iFun): iFun = iFunDummy
  def helperBooleanBooleanBooleanLong2Int(a1: bFun, a2: bFun, a3: bFun, a4: lFun): iFun = iFunDummy
  def helperBooleanBooleanBooleanDouble2Int(a1: bFun, a2: bFun, a3: bFun, a4: dFun): iFun = iFunDummy
  def helperBooleanBooleanBooleanString2Int(a1: bFun, a2: bFun, a3: bFun, a4: sFun): iFun = iFunDummy
  def helperBooleanBooleanBooleanBoolean2Int(a1: bFun, a2: bFun, a3: bFun, a4: bFun): iFun = iFunDummy
  test("getSignature_iFun_5") {
    var result = true
    result &= FunctionSignature.getSignatureIntIntIntInt2Int(helperIntIntIntInt2Int) == "Int:Int:Int:Int2Int"
    result &= FunctionSignature.getSignatureIntIntIntLong2Int(helperIntIntIntLong2Int) == "Int:Int:Int:Long2Int"
    result &= FunctionSignature.getSignatureIntIntIntDouble2Int(helperIntIntIntDouble2Int) == "Int:Int:Int:Double2Int"
    result &= FunctionSignature.getSignatureIntIntIntString2Int(helperIntIntIntString2Int) == "Int:Int:Int:String2Int"
    result &= FunctionSignature.getSignatureIntIntIntBoolean2Int(helperIntIntIntBoolean2Int) == "Int:Int:Int:Boolean2Int"
    result &= FunctionSignature.getSignatureIntIntLongInt2Int(helperIntIntLongInt2Int) == "Int:Int:Long:Int2Int"
    result &= FunctionSignature.getSignatureIntIntLongLong2Int(helperIntIntLongLong2Int) == "Int:Int:Long:Long2Int"
    result &= FunctionSignature.getSignatureIntIntLongDouble2Int(helperIntIntLongDouble2Int) == "Int:Int:Long:Double2Int"
    result &= FunctionSignature.getSignatureIntIntLongString2Int(helperIntIntLongString2Int) == "Int:Int:Long:String2Int"
    result &= FunctionSignature.getSignatureIntIntLongBoolean2Int(helperIntIntLongBoolean2Int) == "Int:Int:Long:Boolean2Int"
    result &= FunctionSignature.getSignatureIntIntDoubleInt2Int(helperIntIntDoubleInt2Int) == "Int:Int:Double:Int2Int"
    result &= FunctionSignature.getSignatureIntIntDoubleLong2Int(helperIntIntDoubleLong2Int) == "Int:Int:Double:Long2Int"
    result &= FunctionSignature.getSignatureIntIntDoubleDouble2Int(helperIntIntDoubleDouble2Int) == "Int:Int:Double:Double2Int"
    result &= FunctionSignature.getSignatureIntIntDoubleString2Int(helperIntIntDoubleString2Int) == "Int:Int:Double:String2Int"
    result &= FunctionSignature.getSignatureIntIntDoubleBoolean2Int(helperIntIntDoubleBoolean2Int) == "Int:Int:Double:Boolean2Int"
    result &= FunctionSignature.getSignatureIntIntStringInt2Int(helperIntIntStringInt2Int) == "Int:Int:String:Int2Int"
    result &= FunctionSignature.getSignatureIntIntStringLong2Int(helperIntIntStringLong2Int) == "Int:Int:String:Long2Int"
    result &= FunctionSignature.getSignatureIntIntStringDouble2Int(helperIntIntStringDouble2Int) == "Int:Int:String:Double2Int"
    result &= FunctionSignature.getSignatureIntIntStringString2Int(helperIntIntStringString2Int) == "Int:Int:String:String2Int"
    result &= FunctionSignature.getSignatureIntIntStringBoolean2Int(helperIntIntStringBoolean2Int) == "Int:Int:String:Boolean2Int"
    result &= FunctionSignature.getSignatureIntIntBooleanInt2Int(helperIntIntBooleanInt2Int) == "Int:Int:Boolean:Int2Int"
    result &= FunctionSignature.getSignatureIntIntBooleanLong2Int(helperIntIntBooleanLong2Int) == "Int:Int:Boolean:Long2Int"
    result &= FunctionSignature.getSignatureIntIntBooleanDouble2Int(helperIntIntBooleanDouble2Int) == "Int:Int:Boolean:Double2Int"
    result &= FunctionSignature.getSignatureIntIntBooleanString2Int(helperIntIntBooleanString2Int) == "Int:Int:Boolean:String2Int"
    result &= FunctionSignature.getSignatureIntIntBooleanBoolean2Int(helperIntIntBooleanBoolean2Int) == "Int:Int:Boolean:Boolean2Int"
    result &= FunctionSignature.getSignatureIntLongIntInt2Int(helperIntLongIntInt2Int) == "Int:Long:Int:Int2Int"
    result &= FunctionSignature.getSignatureIntLongIntLong2Int(helperIntLongIntLong2Int) == "Int:Long:Int:Long2Int"
    result &= FunctionSignature.getSignatureIntLongIntDouble2Int(helperIntLongIntDouble2Int) == "Int:Long:Int:Double2Int"
    result &= FunctionSignature.getSignatureIntLongIntString2Int(helperIntLongIntString2Int) == "Int:Long:Int:String2Int"
    result &= FunctionSignature.getSignatureIntLongIntBoolean2Int(helperIntLongIntBoolean2Int) == "Int:Long:Int:Boolean2Int"
    result &= FunctionSignature.getSignatureIntLongLongInt2Int(helperIntLongLongInt2Int) == "Int:Long:Long:Int2Int"
    result &= FunctionSignature.getSignatureIntLongLongLong2Int(helperIntLongLongLong2Int) == "Int:Long:Long:Long2Int"
    result &= FunctionSignature.getSignatureIntLongLongDouble2Int(helperIntLongLongDouble2Int) == "Int:Long:Long:Double2Int"
    result &= FunctionSignature.getSignatureIntLongLongString2Int(helperIntLongLongString2Int) == "Int:Long:Long:String2Int"
    result &= FunctionSignature.getSignatureIntLongLongBoolean2Int(helperIntLongLongBoolean2Int) == "Int:Long:Long:Boolean2Int"
    result &= FunctionSignature.getSignatureIntLongDoubleInt2Int(helperIntLongDoubleInt2Int) == "Int:Long:Double:Int2Int"
    result &= FunctionSignature.getSignatureIntLongDoubleLong2Int(helperIntLongDoubleLong2Int) == "Int:Long:Double:Long2Int"
    result &= FunctionSignature.getSignatureIntLongDoubleDouble2Int(helperIntLongDoubleDouble2Int) == "Int:Long:Double:Double2Int"
    result &= FunctionSignature.getSignatureIntLongDoubleString2Int(helperIntLongDoubleString2Int) == "Int:Long:Double:String2Int"
    result &= FunctionSignature.getSignatureIntLongDoubleBoolean2Int(helperIntLongDoubleBoolean2Int) == "Int:Long:Double:Boolean2Int"
    result &= FunctionSignature.getSignatureIntLongStringInt2Int(helperIntLongStringInt2Int) == "Int:Long:String:Int2Int"
    result &= FunctionSignature.getSignatureIntLongStringLong2Int(helperIntLongStringLong2Int) == "Int:Long:String:Long2Int"
    result &= FunctionSignature.getSignatureIntLongStringDouble2Int(helperIntLongStringDouble2Int) == "Int:Long:String:Double2Int"
    result &= FunctionSignature.getSignatureIntLongStringString2Int(helperIntLongStringString2Int) == "Int:Long:String:String2Int"
    result &= FunctionSignature.getSignatureIntLongStringBoolean2Int(helperIntLongStringBoolean2Int) == "Int:Long:String:Boolean2Int"
    result &= FunctionSignature.getSignatureIntLongBooleanInt2Int(helperIntLongBooleanInt2Int) == "Int:Long:Boolean:Int2Int"
    result &= FunctionSignature.getSignatureIntLongBooleanLong2Int(helperIntLongBooleanLong2Int) == "Int:Long:Boolean:Long2Int"
    result &= FunctionSignature.getSignatureIntLongBooleanDouble2Int(helperIntLongBooleanDouble2Int) == "Int:Long:Boolean:Double2Int"
    result &= FunctionSignature.getSignatureIntLongBooleanString2Int(helperIntLongBooleanString2Int) == "Int:Long:Boolean:String2Int"
    result &= FunctionSignature.getSignatureIntLongBooleanBoolean2Int(helperIntLongBooleanBoolean2Int) == "Int:Long:Boolean:Boolean2Int"
    result &= FunctionSignature.getSignatureIntDoubleIntInt2Int(helperIntDoubleIntInt2Int) == "Int:Double:Int:Int2Int"
    result &= FunctionSignature.getSignatureIntDoubleIntLong2Int(helperIntDoubleIntLong2Int) == "Int:Double:Int:Long2Int"
    result &= FunctionSignature.getSignatureIntDoubleIntDouble2Int(helperIntDoubleIntDouble2Int) == "Int:Double:Int:Double2Int"
    result &= FunctionSignature.getSignatureIntDoubleIntString2Int(helperIntDoubleIntString2Int) == "Int:Double:Int:String2Int"
    result &= FunctionSignature.getSignatureIntDoubleIntBoolean2Int(helperIntDoubleIntBoolean2Int) == "Int:Double:Int:Boolean2Int"
    result &= FunctionSignature.getSignatureIntDoubleLongInt2Int(helperIntDoubleLongInt2Int) == "Int:Double:Long:Int2Int"
    result &= FunctionSignature.getSignatureIntDoubleLongLong2Int(helperIntDoubleLongLong2Int) == "Int:Double:Long:Long2Int"
    result &= FunctionSignature.getSignatureIntDoubleLongDouble2Int(helperIntDoubleLongDouble2Int) == "Int:Double:Long:Double2Int"
    result &= FunctionSignature.getSignatureIntDoubleLongString2Int(helperIntDoubleLongString2Int) == "Int:Double:Long:String2Int"
    result &= FunctionSignature.getSignatureIntDoubleLongBoolean2Int(helperIntDoubleLongBoolean2Int) == "Int:Double:Long:Boolean2Int"
    result &= FunctionSignature.getSignatureIntDoubleDoubleInt2Int(helperIntDoubleDoubleInt2Int) == "Int:Double:Double:Int2Int"
    result &= FunctionSignature.getSignatureIntDoubleDoubleLong2Int(helperIntDoubleDoubleLong2Int) == "Int:Double:Double:Long2Int"
    result &= FunctionSignature.getSignatureIntDoubleDoubleDouble2Int(helperIntDoubleDoubleDouble2Int) == "Int:Double:Double:Double2Int"
    result &= FunctionSignature.getSignatureIntDoubleDoubleString2Int(helperIntDoubleDoubleString2Int) == "Int:Double:Double:String2Int"
    result &= FunctionSignature.getSignatureIntDoubleDoubleBoolean2Int(helperIntDoubleDoubleBoolean2Int) == "Int:Double:Double:Boolean2Int"
    result &= FunctionSignature.getSignatureIntDoubleStringInt2Int(helperIntDoubleStringInt2Int) == "Int:Double:String:Int2Int"
    result &= FunctionSignature.getSignatureIntDoubleStringLong2Int(helperIntDoubleStringLong2Int) == "Int:Double:String:Long2Int"
    result &= FunctionSignature.getSignatureIntDoubleStringDouble2Int(helperIntDoubleStringDouble2Int) == "Int:Double:String:Double2Int"
    result &= FunctionSignature.getSignatureIntDoubleStringString2Int(helperIntDoubleStringString2Int) == "Int:Double:String:String2Int"
    result &= FunctionSignature.getSignatureIntDoubleStringBoolean2Int(helperIntDoubleStringBoolean2Int) == "Int:Double:String:Boolean2Int"
    result &= FunctionSignature.getSignatureIntDoubleBooleanInt2Int(helperIntDoubleBooleanInt2Int) == "Int:Double:Boolean:Int2Int"
    result &= FunctionSignature.getSignatureIntDoubleBooleanLong2Int(helperIntDoubleBooleanLong2Int) == "Int:Double:Boolean:Long2Int"
    result &= FunctionSignature.getSignatureIntDoubleBooleanDouble2Int(helperIntDoubleBooleanDouble2Int) == "Int:Double:Boolean:Double2Int"
    result &= FunctionSignature.getSignatureIntDoubleBooleanString2Int(helperIntDoubleBooleanString2Int) == "Int:Double:Boolean:String2Int"
    result &= FunctionSignature.getSignatureIntDoubleBooleanBoolean2Int(helperIntDoubleBooleanBoolean2Int) == "Int:Double:Boolean:Boolean2Int"
    result &= FunctionSignature.getSignatureIntStringIntInt2Int(helperIntStringIntInt2Int) == "Int:String:Int:Int2Int"
    result &= FunctionSignature.getSignatureIntStringIntLong2Int(helperIntStringIntLong2Int) == "Int:String:Int:Long2Int"
    result &= FunctionSignature.getSignatureIntStringIntDouble2Int(helperIntStringIntDouble2Int) == "Int:String:Int:Double2Int"
    result &= FunctionSignature.getSignatureIntStringIntString2Int(helperIntStringIntString2Int) == "Int:String:Int:String2Int"
    result &= FunctionSignature.getSignatureIntStringIntBoolean2Int(helperIntStringIntBoolean2Int) == "Int:String:Int:Boolean2Int"
    result &= FunctionSignature.getSignatureIntStringLongInt2Int(helperIntStringLongInt2Int) == "Int:String:Long:Int2Int"
    result &= FunctionSignature.getSignatureIntStringLongLong2Int(helperIntStringLongLong2Int) == "Int:String:Long:Long2Int"
    result &= FunctionSignature.getSignatureIntStringLongDouble2Int(helperIntStringLongDouble2Int) == "Int:String:Long:Double2Int"
    result &= FunctionSignature.getSignatureIntStringLongString2Int(helperIntStringLongString2Int) == "Int:String:Long:String2Int"
    result &= FunctionSignature.getSignatureIntStringLongBoolean2Int(helperIntStringLongBoolean2Int) == "Int:String:Long:Boolean2Int"
    result &= FunctionSignature.getSignatureIntStringDoubleInt2Int(helperIntStringDoubleInt2Int) == "Int:String:Double:Int2Int"
    result &= FunctionSignature.getSignatureIntStringDoubleLong2Int(helperIntStringDoubleLong2Int) == "Int:String:Double:Long2Int"
    result &= FunctionSignature.getSignatureIntStringDoubleDouble2Int(helperIntStringDoubleDouble2Int) == "Int:String:Double:Double2Int"
    result &= FunctionSignature.getSignatureIntStringDoubleString2Int(helperIntStringDoubleString2Int) == "Int:String:Double:String2Int"
    result &= FunctionSignature.getSignatureIntStringDoubleBoolean2Int(helperIntStringDoubleBoolean2Int) == "Int:String:Double:Boolean2Int"
    result &= FunctionSignature.getSignatureIntStringStringInt2Int(helperIntStringStringInt2Int) == "Int:String:String:Int2Int"
    result &= FunctionSignature.getSignatureIntStringStringLong2Int(helperIntStringStringLong2Int) == "Int:String:String:Long2Int"
    result &= FunctionSignature.getSignatureIntStringStringDouble2Int(helperIntStringStringDouble2Int) == "Int:String:String:Double2Int"
    result &= FunctionSignature.getSignatureIntStringStringString2Int(helperIntStringStringString2Int) == "Int:String:String:String2Int"
    result &= FunctionSignature.getSignatureIntStringStringBoolean2Int(helperIntStringStringBoolean2Int) == "Int:String:String:Boolean2Int"
    result &= FunctionSignature.getSignatureIntStringBooleanInt2Int(helperIntStringBooleanInt2Int) == "Int:String:Boolean:Int2Int"
    result &= FunctionSignature.getSignatureIntStringBooleanLong2Int(helperIntStringBooleanLong2Int) == "Int:String:Boolean:Long2Int"
    result &= FunctionSignature.getSignatureIntStringBooleanDouble2Int(helperIntStringBooleanDouble2Int) == "Int:String:Boolean:Double2Int"
    result &= FunctionSignature.getSignatureIntStringBooleanString2Int(helperIntStringBooleanString2Int) == "Int:String:Boolean:String2Int"
    result &= FunctionSignature.getSignatureIntStringBooleanBoolean2Int(helperIntStringBooleanBoolean2Int) == "Int:String:Boolean:Boolean2Int"
    result &= FunctionSignature.getSignatureIntBooleanIntInt2Int(helperIntBooleanIntInt2Int) == "Int:Boolean:Int:Int2Int"
    result &= FunctionSignature.getSignatureIntBooleanIntLong2Int(helperIntBooleanIntLong2Int) == "Int:Boolean:Int:Long2Int"
    result &= FunctionSignature.getSignatureIntBooleanIntDouble2Int(helperIntBooleanIntDouble2Int) == "Int:Boolean:Int:Double2Int"
    result &= FunctionSignature.getSignatureIntBooleanIntString2Int(helperIntBooleanIntString2Int) == "Int:Boolean:Int:String2Int"
    result &= FunctionSignature.getSignatureIntBooleanIntBoolean2Int(helperIntBooleanIntBoolean2Int) == "Int:Boolean:Int:Boolean2Int"
    result &= FunctionSignature.getSignatureIntBooleanLongInt2Int(helperIntBooleanLongInt2Int) == "Int:Boolean:Long:Int2Int"
    result &= FunctionSignature.getSignatureIntBooleanLongLong2Int(helperIntBooleanLongLong2Int) == "Int:Boolean:Long:Long2Int"
    result &= FunctionSignature.getSignatureIntBooleanLongDouble2Int(helperIntBooleanLongDouble2Int) == "Int:Boolean:Long:Double2Int"
    result &= FunctionSignature.getSignatureIntBooleanLongString2Int(helperIntBooleanLongString2Int) == "Int:Boolean:Long:String2Int"
    result &= FunctionSignature.getSignatureIntBooleanLongBoolean2Int(helperIntBooleanLongBoolean2Int) == "Int:Boolean:Long:Boolean2Int"
    result &= FunctionSignature.getSignatureIntBooleanDoubleInt2Int(helperIntBooleanDoubleInt2Int) == "Int:Boolean:Double:Int2Int"
    result &= FunctionSignature.getSignatureIntBooleanDoubleLong2Int(helperIntBooleanDoubleLong2Int) == "Int:Boolean:Double:Long2Int"
    result &= FunctionSignature.getSignatureIntBooleanDoubleDouble2Int(helperIntBooleanDoubleDouble2Int) == "Int:Boolean:Double:Double2Int"
    result &= FunctionSignature.getSignatureIntBooleanDoubleString2Int(helperIntBooleanDoubleString2Int) == "Int:Boolean:Double:String2Int"
    result &= FunctionSignature.getSignatureIntBooleanDoubleBoolean2Int(helperIntBooleanDoubleBoolean2Int) == "Int:Boolean:Double:Boolean2Int"
    result &= FunctionSignature.getSignatureIntBooleanStringInt2Int(helperIntBooleanStringInt2Int) == "Int:Boolean:String:Int2Int"
    result &= FunctionSignature.getSignatureIntBooleanStringLong2Int(helperIntBooleanStringLong2Int) == "Int:Boolean:String:Long2Int"
    result &= FunctionSignature.getSignatureIntBooleanStringDouble2Int(helperIntBooleanStringDouble2Int) == "Int:Boolean:String:Double2Int"
    result &= FunctionSignature.getSignatureIntBooleanStringString2Int(helperIntBooleanStringString2Int) == "Int:Boolean:String:String2Int"
    result &= FunctionSignature.getSignatureIntBooleanStringBoolean2Int(helperIntBooleanStringBoolean2Int) == "Int:Boolean:String:Boolean2Int"
    result &= FunctionSignature.getSignatureIntBooleanBooleanInt2Int(helperIntBooleanBooleanInt2Int) == "Int:Boolean:Boolean:Int2Int"
    result &= FunctionSignature.getSignatureIntBooleanBooleanLong2Int(helperIntBooleanBooleanLong2Int) == "Int:Boolean:Boolean:Long2Int"
    result &= FunctionSignature.getSignatureIntBooleanBooleanDouble2Int(helperIntBooleanBooleanDouble2Int) == "Int:Boolean:Boolean:Double2Int"
    result &= FunctionSignature.getSignatureIntBooleanBooleanString2Int(helperIntBooleanBooleanString2Int) == "Int:Boolean:Boolean:String2Int"
    result &= FunctionSignature.getSignatureIntBooleanBooleanBoolean2Int(helperIntBooleanBooleanBoolean2Int) == "Int:Boolean:Boolean:Boolean2Int"
    result &= FunctionSignature.getSignatureLongIntIntInt2Int(helperLongIntIntInt2Int) == "Long:Int:Int:Int2Int"
    result &= FunctionSignature.getSignatureLongIntIntLong2Int(helperLongIntIntLong2Int) == "Long:Int:Int:Long2Int"
    result &= FunctionSignature.getSignatureLongIntIntDouble2Int(helperLongIntIntDouble2Int) == "Long:Int:Int:Double2Int"
    result &= FunctionSignature.getSignatureLongIntIntString2Int(helperLongIntIntString2Int) == "Long:Int:Int:String2Int"
    result &= FunctionSignature.getSignatureLongIntIntBoolean2Int(helperLongIntIntBoolean2Int) == "Long:Int:Int:Boolean2Int"
    result &= FunctionSignature.getSignatureLongIntLongInt2Int(helperLongIntLongInt2Int) == "Long:Int:Long:Int2Int"
    result &= FunctionSignature.getSignatureLongIntLongLong2Int(helperLongIntLongLong2Int) == "Long:Int:Long:Long2Int"
    result &= FunctionSignature.getSignatureLongIntLongDouble2Int(helperLongIntLongDouble2Int) == "Long:Int:Long:Double2Int"
    result &= FunctionSignature.getSignatureLongIntLongString2Int(helperLongIntLongString2Int) == "Long:Int:Long:String2Int"
    result &= FunctionSignature.getSignatureLongIntLongBoolean2Int(helperLongIntLongBoolean2Int) == "Long:Int:Long:Boolean2Int"
    result &= FunctionSignature.getSignatureLongIntDoubleInt2Int(helperLongIntDoubleInt2Int) == "Long:Int:Double:Int2Int"
    result &= FunctionSignature.getSignatureLongIntDoubleLong2Int(helperLongIntDoubleLong2Int) == "Long:Int:Double:Long2Int"
    result &= FunctionSignature.getSignatureLongIntDoubleDouble2Int(helperLongIntDoubleDouble2Int) == "Long:Int:Double:Double2Int"
    result &= FunctionSignature.getSignatureLongIntDoubleString2Int(helperLongIntDoubleString2Int) == "Long:Int:Double:String2Int"
    result &= FunctionSignature.getSignatureLongIntDoubleBoolean2Int(helperLongIntDoubleBoolean2Int) == "Long:Int:Double:Boolean2Int"
    result &= FunctionSignature.getSignatureLongIntStringInt2Int(helperLongIntStringInt2Int) == "Long:Int:String:Int2Int"
    result &= FunctionSignature.getSignatureLongIntStringLong2Int(helperLongIntStringLong2Int) == "Long:Int:String:Long2Int"
    result &= FunctionSignature.getSignatureLongIntStringDouble2Int(helperLongIntStringDouble2Int) == "Long:Int:String:Double2Int"
    result &= FunctionSignature.getSignatureLongIntStringString2Int(helperLongIntStringString2Int) == "Long:Int:String:String2Int"
    result &= FunctionSignature.getSignatureLongIntStringBoolean2Int(helperLongIntStringBoolean2Int) == "Long:Int:String:Boolean2Int"
    result &= FunctionSignature.getSignatureLongIntBooleanInt2Int(helperLongIntBooleanInt2Int) == "Long:Int:Boolean:Int2Int"
    result &= FunctionSignature.getSignatureLongIntBooleanLong2Int(helperLongIntBooleanLong2Int) == "Long:Int:Boolean:Long2Int"
    result &= FunctionSignature.getSignatureLongIntBooleanDouble2Int(helperLongIntBooleanDouble2Int) == "Long:Int:Boolean:Double2Int"
    result &= FunctionSignature.getSignatureLongIntBooleanString2Int(helperLongIntBooleanString2Int) == "Long:Int:Boolean:String2Int"
    result &= FunctionSignature.getSignatureLongIntBooleanBoolean2Int(helperLongIntBooleanBoolean2Int) == "Long:Int:Boolean:Boolean2Int"
    result &= FunctionSignature.getSignatureLongLongIntInt2Int(helperLongLongIntInt2Int) == "Long:Long:Int:Int2Int"
    result &= FunctionSignature.getSignatureLongLongIntLong2Int(helperLongLongIntLong2Int) == "Long:Long:Int:Long2Int"
    result &= FunctionSignature.getSignatureLongLongIntDouble2Int(helperLongLongIntDouble2Int) == "Long:Long:Int:Double2Int"
    result &= FunctionSignature.getSignatureLongLongIntString2Int(helperLongLongIntString2Int) == "Long:Long:Int:String2Int"
    result &= FunctionSignature.getSignatureLongLongIntBoolean2Int(helperLongLongIntBoolean2Int) == "Long:Long:Int:Boolean2Int"
    result &= FunctionSignature.getSignatureLongLongLongInt2Int(helperLongLongLongInt2Int) == "Long:Long:Long:Int2Int"
    result &= FunctionSignature.getSignatureLongLongLongLong2Int(helperLongLongLongLong2Int) == "Long:Long:Long:Long2Int"
    result &= FunctionSignature.getSignatureLongLongLongDouble2Int(helperLongLongLongDouble2Int) == "Long:Long:Long:Double2Int"
    result &= FunctionSignature.getSignatureLongLongLongString2Int(helperLongLongLongString2Int) == "Long:Long:Long:String2Int"
    result &= FunctionSignature.getSignatureLongLongLongBoolean2Int(helperLongLongLongBoolean2Int) == "Long:Long:Long:Boolean2Int"
    result &= FunctionSignature.getSignatureLongLongDoubleInt2Int(helperLongLongDoubleInt2Int) == "Long:Long:Double:Int2Int"
    result &= FunctionSignature.getSignatureLongLongDoubleLong2Int(helperLongLongDoubleLong2Int) == "Long:Long:Double:Long2Int"
    result &= FunctionSignature.getSignatureLongLongDoubleDouble2Int(helperLongLongDoubleDouble2Int) == "Long:Long:Double:Double2Int"
    result &= FunctionSignature.getSignatureLongLongDoubleString2Int(helperLongLongDoubleString2Int) == "Long:Long:Double:String2Int"
    result &= FunctionSignature.getSignatureLongLongDoubleBoolean2Int(helperLongLongDoubleBoolean2Int) == "Long:Long:Double:Boolean2Int"
    result &= FunctionSignature.getSignatureLongLongStringInt2Int(helperLongLongStringInt2Int) == "Long:Long:String:Int2Int"
    result &= FunctionSignature.getSignatureLongLongStringLong2Int(helperLongLongStringLong2Int) == "Long:Long:String:Long2Int"
    result &= FunctionSignature.getSignatureLongLongStringDouble2Int(helperLongLongStringDouble2Int) == "Long:Long:String:Double2Int"
    result &= FunctionSignature.getSignatureLongLongStringString2Int(helperLongLongStringString2Int) == "Long:Long:String:String2Int"
    result &= FunctionSignature.getSignatureLongLongStringBoolean2Int(helperLongLongStringBoolean2Int) == "Long:Long:String:Boolean2Int"
    result &= FunctionSignature.getSignatureLongLongBooleanInt2Int(helperLongLongBooleanInt2Int) == "Long:Long:Boolean:Int2Int"
    result &= FunctionSignature.getSignatureLongLongBooleanLong2Int(helperLongLongBooleanLong2Int) == "Long:Long:Boolean:Long2Int"
    result &= FunctionSignature.getSignatureLongLongBooleanDouble2Int(helperLongLongBooleanDouble2Int) == "Long:Long:Boolean:Double2Int"
    result &= FunctionSignature.getSignatureLongLongBooleanString2Int(helperLongLongBooleanString2Int) == "Long:Long:Boolean:String2Int"
    result &= FunctionSignature.getSignatureLongLongBooleanBoolean2Int(helperLongLongBooleanBoolean2Int) == "Long:Long:Boolean:Boolean2Int"
    result &= FunctionSignature.getSignatureLongDoubleIntInt2Int(helperLongDoubleIntInt2Int) == "Long:Double:Int:Int2Int"
    result &= FunctionSignature.getSignatureLongDoubleIntLong2Int(helperLongDoubleIntLong2Int) == "Long:Double:Int:Long2Int"
    result &= FunctionSignature.getSignatureLongDoubleIntDouble2Int(helperLongDoubleIntDouble2Int) == "Long:Double:Int:Double2Int"
    result &= FunctionSignature.getSignatureLongDoubleIntString2Int(helperLongDoubleIntString2Int) == "Long:Double:Int:String2Int"
    result &= FunctionSignature.getSignatureLongDoubleIntBoolean2Int(helperLongDoubleIntBoolean2Int) == "Long:Double:Int:Boolean2Int"
    result &= FunctionSignature.getSignatureLongDoubleLongInt2Int(helperLongDoubleLongInt2Int) == "Long:Double:Long:Int2Int"
    result &= FunctionSignature.getSignatureLongDoubleLongLong2Int(helperLongDoubleLongLong2Int) == "Long:Double:Long:Long2Int"
    result &= FunctionSignature.getSignatureLongDoubleLongDouble2Int(helperLongDoubleLongDouble2Int) == "Long:Double:Long:Double2Int"
    result &= FunctionSignature.getSignatureLongDoubleLongString2Int(helperLongDoubleLongString2Int) == "Long:Double:Long:String2Int"
    result &= FunctionSignature.getSignatureLongDoubleLongBoolean2Int(helperLongDoubleLongBoolean2Int) == "Long:Double:Long:Boolean2Int"
    result &= FunctionSignature.getSignatureLongDoubleDoubleInt2Int(helperLongDoubleDoubleInt2Int) == "Long:Double:Double:Int2Int"
    result &= FunctionSignature.getSignatureLongDoubleDoubleLong2Int(helperLongDoubleDoubleLong2Int) == "Long:Double:Double:Long2Int"
    result &= FunctionSignature.getSignatureLongDoubleDoubleDouble2Int(helperLongDoubleDoubleDouble2Int) == "Long:Double:Double:Double2Int"
    result &= FunctionSignature.getSignatureLongDoubleDoubleString2Int(helperLongDoubleDoubleString2Int) == "Long:Double:Double:String2Int"
    result &= FunctionSignature.getSignatureLongDoubleDoubleBoolean2Int(helperLongDoubleDoubleBoolean2Int) == "Long:Double:Double:Boolean2Int"
    result &= FunctionSignature.getSignatureLongDoubleStringInt2Int(helperLongDoubleStringInt2Int) == "Long:Double:String:Int2Int"
    result &= FunctionSignature.getSignatureLongDoubleStringLong2Int(helperLongDoubleStringLong2Int) == "Long:Double:String:Long2Int"
    result &= FunctionSignature.getSignatureLongDoubleStringDouble2Int(helperLongDoubleStringDouble2Int) == "Long:Double:String:Double2Int"
    result &= FunctionSignature.getSignatureLongDoubleStringString2Int(helperLongDoubleStringString2Int) == "Long:Double:String:String2Int"
    result &= FunctionSignature.getSignatureLongDoubleStringBoolean2Int(helperLongDoubleStringBoolean2Int) == "Long:Double:String:Boolean2Int"
    result &= FunctionSignature.getSignatureLongDoubleBooleanInt2Int(helperLongDoubleBooleanInt2Int) == "Long:Double:Boolean:Int2Int"
    result &= FunctionSignature.getSignatureLongDoubleBooleanLong2Int(helperLongDoubleBooleanLong2Int) == "Long:Double:Boolean:Long2Int"
    result &= FunctionSignature.getSignatureLongDoubleBooleanDouble2Int(helperLongDoubleBooleanDouble2Int) == "Long:Double:Boolean:Double2Int"
    result &= FunctionSignature.getSignatureLongDoubleBooleanString2Int(helperLongDoubleBooleanString2Int) == "Long:Double:Boolean:String2Int"
    result &= FunctionSignature.getSignatureLongDoubleBooleanBoolean2Int(helperLongDoubleBooleanBoolean2Int) == "Long:Double:Boolean:Boolean2Int"
    result &= FunctionSignature.getSignatureLongStringIntInt2Int(helperLongStringIntInt2Int) == "Long:String:Int:Int2Int"
    result &= FunctionSignature.getSignatureLongStringIntLong2Int(helperLongStringIntLong2Int) == "Long:String:Int:Long2Int"
    result &= FunctionSignature.getSignatureLongStringIntDouble2Int(helperLongStringIntDouble2Int) == "Long:String:Int:Double2Int"
    result &= FunctionSignature.getSignatureLongStringIntString2Int(helperLongStringIntString2Int) == "Long:String:Int:String2Int"
    result &= FunctionSignature.getSignatureLongStringIntBoolean2Int(helperLongStringIntBoolean2Int) == "Long:String:Int:Boolean2Int"
    result &= FunctionSignature.getSignatureLongStringLongInt2Int(helperLongStringLongInt2Int) == "Long:String:Long:Int2Int"
    result &= FunctionSignature.getSignatureLongStringLongLong2Int(helperLongStringLongLong2Int) == "Long:String:Long:Long2Int"
    result &= FunctionSignature.getSignatureLongStringLongDouble2Int(helperLongStringLongDouble2Int) == "Long:String:Long:Double2Int"
    result &= FunctionSignature.getSignatureLongStringLongString2Int(helperLongStringLongString2Int) == "Long:String:Long:String2Int"
    result &= FunctionSignature.getSignatureLongStringLongBoolean2Int(helperLongStringLongBoolean2Int) == "Long:String:Long:Boolean2Int"
    result &= FunctionSignature.getSignatureLongStringDoubleInt2Int(helperLongStringDoubleInt2Int) == "Long:String:Double:Int2Int"
    result &= FunctionSignature.getSignatureLongStringDoubleLong2Int(helperLongStringDoubleLong2Int) == "Long:String:Double:Long2Int"
    result &= FunctionSignature.getSignatureLongStringDoubleDouble2Int(helperLongStringDoubleDouble2Int) == "Long:String:Double:Double2Int"
    result &= FunctionSignature.getSignatureLongStringDoubleString2Int(helperLongStringDoubleString2Int) == "Long:String:Double:String2Int"
    result &= FunctionSignature.getSignatureLongStringDoubleBoolean2Int(helperLongStringDoubleBoolean2Int) == "Long:String:Double:Boolean2Int"
    result &= FunctionSignature.getSignatureLongStringStringInt2Int(helperLongStringStringInt2Int) == "Long:String:String:Int2Int"
    result &= FunctionSignature.getSignatureLongStringStringLong2Int(helperLongStringStringLong2Int) == "Long:String:String:Long2Int"
    result &= FunctionSignature.getSignatureLongStringStringDouble2Int(helperLongStringStringDouble2Int) == "Long:String:String:Double2Int"
    result &= FunctionSignature.getSignatureLongStringStringString2Int(helperLongStringStringString2Int) == "Long:String:String:String2Int"
    result &= FunctionSignature.getSignatureLongStringStringBoolean2Int(helperLongStringStringBoolean2Int) == "Long:String:String:Boolean2Int"
    result &= FunctionSignature.getSignatureLongStringBooleanInt2Int(helperLongStringBooleanInt2Int) == "Long:String:Boolean:Int2Int"
    result &= FunctionSignature.getSignatureLongStringBooleanLong2Int(helperLongStringBooleanLong2Int) == "Long:String:Boolean:Long2Int"
    result &= FunctionSignature.getSignatureLongStringBooleanDouble2Int(helperLongStringBooleanDouble2Int) == "Long:String:Boolean:Double2Int"
    result &= FunctionSignature.getSignatureLongStringBooleanString2Int(helperLongStringBooleanString2Int) == "Long:String:Boolean:String2Int"
    result &= FunctionSignature.getSignatureLongStringBooleanBoolean2Int(helperLongStringBooleanBoolean2Int) == "Long:String:Boolean:Boolean2Int"
    result &= FunctionSignature.getSignatureLongBooleanIntInt2Int(helperLongBooleanIntInt2Int) == "Long:Boolean:Int:Int2Int"
    result &= FunctionSignature.getSignatureLongBooleanIntLong2Int(helperLongBooleanIntLong2Int) == "Long:Boolean:Int:Long2Int"
    result &= FunctionSignature.getSignatureLongBooleanIntDouble2Int(helperLongBooleanIntDouble2Int) == "Long:Boolean:Int:Double2Int"
    result &= FunctionSignature.getSignatureLongBooleanIntString2Int(helperLongBooleanIntString2Int) == "Long:Boolean:Int:String2Int"
    result &= FunctionSignature.getSignatureLongBooleanIntBoolean2Int(helperLongBooleanIntBoolean2Int) == "Long:Boolean:Int:Boolean2Int"
    result &= FunctionSignature.getSignatureLongBooleanLongInt2Int(helperLongBooleanLongInt2Int) == "Long:Boolean:Long:Int2Int"
    result &= FunctionSignature.getSignatureLongBooleanLongLong2Int(helperLongBooleanLongLong2Int) == "Long:Boolean:Long:Long2Int"
    result &= FunctionSignature.getSignatureLongBooleanLongDouble2Int(helperLongBooleanLongDouble2Int) == "Long:Boolean:Long:Double2Int"
    result &= FunctionSignature.getSignatureLongBooleanLongString2Int(helperLongBooleanLongString2Int) == "Long:Boolean:Long:String2Int"
    result &= FunctionSignature.getSignatureLongBooleanLongBoolean2Int(helperLongBooleanLongBoolean2Int) == "Long:Boolean:Long:Boolean2Int"
    result &= FunctionSignature.getSignatureLongBooleanDoubleInt2Int(helperLongBooleanDoubleInt2Int) == "Long:Boolean:Double:Int2Int"
    result &= FunctionSignature.getSignatureLongBooleanDoubleLong2Int(helperLongBooleanDoubleLong2Int) == "Long:Boolean:Double:Long2Int"
    result &= FunctionSignature.getSignatureLongBooleanDoubleDouble2Int(helperLongBooleanDoubleDouble2Int) == "Long:Boolean:Double:Double2Int"
    result &= FunctionSignature.getSignatureLongBooleanDoubleString2Int(helperLongBooleanDoubleString2Int) == "Long:Boolean:Double:String2Int"
    result &= FunctionSignature.getSignatureLongBooleanDoubleBoolean2Int(helperLongBooleanDoubleBoolean2Int) == "Long:Boolean:Double:Boolean2Int"
    result &= FunctionSignature.getSignatureLongBooleanStringInt2Int(helperLongBooleanStringInt2Int) == "Long:Boolean:String:Int2Int"
    result &= FunctionSignature.getSignatureLongBooleanStringLong2Int(helperLongBooleanStringLong2Int) == "Long:Boolean:String:Long2Int"
    result &= FunctionSignature.getSignatureLongBooleanStringDouble2Int(helperLongBooleanStringDouble2Int) == "Long:Boolean:String:Double2Int"
    result &= FunctionSignature.getSignatureLongBooleanStringString2Int(helperLongBooleanStringString2Int) == "Long:Boolean:String:String2Int"
    result &= FunctionSignature.getSignatureLongBooleanStringBoolean2Int(helperLongBooleanStringBoolean2Int) == "Long:Boolean:String:Boolean2Int"
    result &= FunctionSignature.getSignatureLongBooleanBooleanInt2Int(helperLongBooleanBooleanInt2Int) == "Long:Boolean:Boolean:Int2Int"
    result &= FunctionSignature.getSignatureLongBooleanBooleanLong2Int(helperLongBooleanBooleanLong2Int) == "Long:Boolean:Boolean:Long2Int"
    result &= FunctionSignature.getSignatureLongBooleanBooleanDouble2Int(helperLongBooleanBooleanDouble2Int) == "Long:Boolean:Boolean:Double2Int"
    result &= FunctionSignature.getSignatureLongBooleanBooleanString2Int(helperLongBooleanBooleanString2Int) == "Long:Boolean:Boolean:String2Int"
    result &= FunctionSignature.getSignatureLongBooleanBooleanBoolean2Int(helperLongBooleanBooleanBoolean2Int) == "Long:Boolean:Boolean:Boolean2Int"
    result &= FunctionSignature.getSignatureDoubleIntIntInt2Int(helperDoubleIntIntInt2Int) == "Double:Int:Int:Int2Int"
    result &= FunctionSignature.getSignatureDoubleIntIntLong2Int(helperDoubleIntIntLong2Int) == "Double:Int:Int:Long2Int"
    result &= FunctionSignature.getSignatureDoubleIntIntDouble2Int(helperDoubleIntIntDouble2Int) == "Double:Int:Int:Double2Int"
    result &= FunctionSignature.getSignatureDoubleIntIntString2Int(helperDoubleIntIntString2Int) == "Double:Int:Int:String2Int"
    result &= FunctionSignature.getSignatureDoubleIntIntBoolean2Int(helperDoubleIntIntBoolean2Int) == "Double:Int:Int:Boolean2Int"
    result &= FunctionSignature.getSignatureDoubleIntLongInt2Int(helperDoubleIntLongInt2Int) == "Double:Int:Long:Int2Int"
    result &= FunctionSignature.getSignatureDoubleIntLongLong2Int(helperDoubleIntLongLong2Int) == "Double:Int:Long:Long2Int"
    result &= FunctionSignature.getSignatureDoubleIntLongDouble2Int(helperDoubleIntLongDouble2Int) == "Double:Int:Long:Double2Int"
    result &= FunctionSignature.getSignatureDoubleIntLongString2Int(helperDoubleIntLongString2Int) == "Double:Int:Long:String2Int"
    result &= FunctionSignature.getSignatureDoubleIntLongBoolean2Int(helperDoubleIntLongBoolean2Int) == "Double:Int:Long:Boolean2Int"
    result &= FunctionSignature.getSignatureDoubleIntDoubleInt2Int(helperDoubleIntDoubleInt2Int) == "Double:Int:Double:Int2Int"
    result &= FunctionSignature.getSignatureDoubleIntDoubleLong2Int(helperDoubleIntDoubleLong2Int) == "Double:Int:Double:Long2Int"
    result &= FunctionSignature.getSignatureDoubleIntDoubleDouble2Int(helperDoubleIntDoubleDouble2Int) == "Double:Int:Double:Double2Int"
    result &= FunctionSignature.getSignatureDoubleIntDoubleString2Int(helperDoubleIntDoubleString2Int) == "Double:Int:Double:String2Int"
    result &= FunctionSignature.getSignatureDoubleIntDoubleBoolean2Int(helperDoubleIntDoubleBoolean2Int) == "Double:Int:Double:Boolean2Int"
    result &= FunctionSignature.getSignatureDoubleIntStringInt2Int(helperDoubleIntStringInt2Int) == "Double:Int:String:Int2Int"
    result &= FunctionSignature.getSignatureDoubleIntStringLong2Int(helperDoubleIntStringLong2Int) == "Double:Int:String:Long2Int"
    result &= FunctionSignature.getSignatureDoubleIntStringDouble2Int(helperDoubleIntStringDouble2Int) == "Double:Int:String:Double2Int"
    result &= FunctionSignature.getSignatureDoubleIntStringString2Int(helperDoubleIntStringString2Int) == "Double:Int:String:String2Int"
    result &= FunctionSignature.getSignatureDoubleIntStringBoolean2Int(helperDoubleIntStringBoolean2Int) == "Double:Int:String:Boolean2Int"
    result &= FunctionSignature.getSignatureDoubleIntBooleanInt2Int(helperDoubleIntBooleanInt2Int) == "Double:Int:Boolean:Int2Int"
    result &= FunctionSignature.getSignatureDoubleIntBooleanLong2Int(helperDoubleIntBooleanLong2Int) == "Double:Int:Boolean:Long2Int"
    result &= FunctionSignature.getSignatureDoubleIntBooleanDouble2Int(helperDoubleIntBooleanDouble2Int) == "Double:Int:Boolean:Double2Int"
    result &= FunctionSignature.getSignatureDoubleIntBooleanString2Int(helperDoubleIntBooleanString2Int) == "Double:Int:Boolean:String2Int"
    result &= FunctionSignature.getSignatureDoubleIntBooleanBoolean2Int(helperDoubleIntBooleanBoolean2Int) == "Double:Int:Boolean:Boolean2Int"
    result &= FunctionSignature.getSignatureDoubleLongIntInt2Int(helperDoubleLongIntInt2Int) == "Double:Long:Int:Int2Int"
    result &= FunctionSignature.getSignatureDoubleLongIntLong2Int(helperDoubleLongIntLong2Int) == "Double:Long:Int:Long2Int"
    result &= FunctionSignature.getSignatureDoubleLongIntDouble2Int(helperDoubleLongIntDouble2Int) == "Double:Long:Int:Double2Int"
    result &= FunctionSignature.getSignatureDoubleLongIntString2Int(helperDoubleLongIntString2Int) == "Double:Long:Int:String2Int"
    result &= FunctionSignature.getSignatureDoubleLongIntBoolean2Int(helperDoubleLongIntBoolean2Int) == "Double:Long:Int:Boolean2Int"
    result &= FunctionSignature.getSignatureDoubleLongLongInt2Int(helperDoubleLongLongInt2Int) == "Double:Long:Long:Int2Int"
    result &= FunctionSignature.getSignatureDoubleLongLongLong2Int(helperDoubleLongLongLong2Int) == "Double:Long:Long:Long2Int"
    result &= FunctionSignature.getSignatureDoubleLongLongDouble2Int(helperDoubleLongLongDouble2Int) == "Double:Long:Long:Double2Int"
    result &= FunctionSignature.getSignatureDoubleLongLongString2Int(helperDoubleLongLongString2Int) == "Double:Long:Long:String2Int"
    result &= FunctionSignature.getSignatureDoubleLongLongBoolean2Int(helperDoubleLongLongBoolean2Int) == "Double:Long:Long:Boolean2Int"
    result &= FunctionSignature.getSignatureDoubleLongDoubleInt2Int(helperDoubleLongDoubleInt2Int) == "Double:Long:Double:Int2Int"
    result &= FunctionSignature.getSignatureDoubleLongDoubleLong2Int(helperDoubleLongDoubleLong2Int) == "Double:Long:Double:Long2Int"
    result &= FunctionSignature.getSignatureDoubleLongDoubleDouble2Int(helperDoubleLongDoubleDouble2Int) == "Double:Long:Double:Double2Int"
    result &= FunctionSignature.getSignatureDoubleLongDoubleString2Int(helperDoubleLongDoubleString2Int) == "Double:Long:Double:String2Int"
    result &= FunctionSignature.getSignatureDoubleLongDoubleBoolean2Int(helperDoubleLongDoubleBoolean2Int) == "Double:Long:Double:Boolean2Int"
    result &= FunctionSignature.getSignatureDoubleLongStringInt2Int(helperDoubleLongStringInt2Int) == "Double:Long:String:Int2Int"
    result &= FunctionSignature.getSignatureDoubleLongStringLong2Int(helperDoubleLongStringLong2Int) == "Double:Long:String:Long2Int"
    result &= FunctionSignature.getSignatureDoubleLongStringDouble2Int(helperDoubleLongStringDouble2Int) == "Double:Long:String:Double2Int"
    result &= FunctionSignature.getSignatureDoubleLongStringString2Int(helperDoubleLongStringString2Int) == "Double:Long:String:String2Int"
    result &= FunctionSignature.getSignatureDoubleLongStringBoolean2Int(helperDoubleLongStringBoolean2Int) == "Double:Long:String:Boolean2Int"
    result &= FunctionSignature.getSignatureDoubleLongBooleanInt2Int(helperDoubleLongBooleanInt2Int) == "Double:Long:Boolean:Int2Int"
    result &= FunctionSignature.getSignatureDoubleLongBooleanLong2Int(helperDoubleLongBooleanLong2Int) == "Double:Long:Boolean:Long2Int"
    result &= FunctionSignature.getSignatureDoubleLongBooleanDouble2Int(helperDoubleLongBooleanDouble2Int) == "Double:Long:Boolean:Double2Int"
    result &= FunctionSignature.getSignatureDoubleLongBooleanString2Int(helperDoubleLongBooleanString2Int) == "Double:Long:Boolean:String2Int"
    result &= FunctionSignature.getSignatureDoubleLongBooleanBoolean2Int(helperDoubleLongBooleanBoolean2Int) == "Double:Long:Boolean:Boolean2Int"
    result &= FunctionSignature.getSignatureDoubleDoubleIntInt2Int(helperDoubleDoubleIntInt2Int) == "Double:Double:Int:Int2Int"
    result &= FunctionSignature.getSignatureDoubleDoubleIntLong2Int(helperDoubleDoubleIntLong2Int) == "Double:Double:Int:Long2Int"
    result &= FunctionSignature.getSignatureDoubleDoubleIntDouble2Int(helperDoubleDoubleIntDouble2Int) == "Double:Double:Int:Double2Int"
    result &= FunctionSignature.getSignatureDoubleDoubleIntString2Int(helperDoubleDoubleIntString2Int) == "Double:Double:Int:String2Int"
    result &= FunctionSignature.getSignatureDoubleDoubleIntBoolean2Int(helperDoubleDoubleIntBoolean2Int) == "Double:Double:Int:Boolean2Int"
    result &= FunctionSignature.getSignatureDoubleDoubleLongInt2Int(helperDoubleDoubleLongInt2Int) == "Double:Double:Long:Int2Int"
    result &= FunctionSignature.getSignatureDoubleDoubleLongLong2Int(helperDoubleDoubleLongLong2Int) == "Double:Double:Long:Long2Int"
    result &= FunctionSignature.getSignatureDoubleDoubleLongDouble2Int(helperDoubleDoubleLongDouble2Int) == "Double:Double:Long:Double2Int"
    result &= FunctionSignature.getSignatureDoubleDoubleLongString2Int(helperDoubleDoubleLongString2Int) == "Double:Double:Long:String2Int"
    result &= FunctionSignature.getSignatureDoubleDoubleLongBoolean2Int(helperDoubleDoubleLongBoolean2Int) == "Double:Double:Long:Boolean2Int"
    result &= FunctionSignature.getSignatureDoubleDoubleDoubleInt2Int(helperDoubleDoubleDoubleInt2Int) == "Double:Double:Double:Int2Int"
    result &= FunctionSignature.getSignatureDoubleDoubleDoubleLong2Int(helperDoubleDoubleDoubleLong2Int) == "Double:Double:Double:Long2Int"
    result &= FunctionSignature.getSignatureDoubleDoubleDoubleDouble2Int(helperDoubleDoubleDoubleDouble2Int) == "Double:Double:Double:Double2Int"
    result &= FunctionSignature.getSignatureDoubleDoubleDoubleString2Int(helperDoubleDoubleDoubleString2Int) == "Double:Double:Double:String2Int"
    result &= FunctionSignature.getSignatureDoubleDoubleDoubleBoolean2Int(helperDoubleDoubleDoubleBoolean2Int) == "Double:Double:Double:Boolean2Int"
    result &= FunctionSignature.getSignatureDoubleDoubleStringInt2Int(helperDoubleDoubleStringInt2Int) == "Double:Double:String:Int2Int"
    result &= FunctionSignature.getSignatureDoubleDoubleStringLong2Int(helperDoubleDoubleStringLong2Int) == "Double:Double:String:Long2Int"
    result &= FunctionSignature.getSignatureDoubleDoubleStringDouble2Int(helperDoubleDoubleStringDouble2Int) == "Double:Double:String:Double2Int"
    result &= FunctionSignature.getSignatureDoubleDoubleStringString2Int(helperDoubleDoubleStringString2Int) == "Double:Double:String:String2Int"
    result &= FunctionSignature.getSignatureDoubleDoubleStringBoolean2Int(helperDoubleDoubleStringBoolean2Int) == "Double:Double:String:Boolean2Int"
    result &= FunctionSignature.getSignatureDoubleDoubleBooleanInt2Int(helperDoubleDoubleBooleanInt2Int) == "Double:Double:Boolean:Int2Int"
    result &= FunctionSignature.getSignatureDoubleDoubleBooleanLong2Int(helperDoubleDoubleBooleanLong2Int) == "Double:Double:Boolean:Long2Int"
    result &= FunctionSignature.getSignatureDoubleDoubleBooleanDouble2Int(helperDoubleDoubleBooleanDouble2Int) == "Double:Double:Boolean:Double2Int"
    result &= FunctionSignature.getSignatureDoubleDoubleBooleanString2Int(helperDoubleDoubleBooleanString2Int) == "Double:Double:Boolean:String2Int"
    result &= FunctionSignature.getSignatureDoubleDoubleBooleanBoolean2Int(helperDoubleDoubleBooleanBoolean2Int) == "Double:Double:Boolean:Boolean2Int"
    result &= FunctionSignature.getSignatureDoubleStringIntInt2Int(helperDoubleStringIntInt2Int) == "Double:String:Int:Int2Int"
    result &= FunctionSignature.getSignatureDoubleStringIntLong2Int(helperDoubleStringIntLong2Int) == "Double:String:Int:Long2Int"
    result &= FunctionSignature.getSignatureDoubleStringIntDouble2Int(helperDoubleStringIntDouble2Int) == "Double:String:Int:Double2Int"
    result &= FunctionSignature.getSignatureDoubleStringIntString2Int(helperDoubleStringIntString2Int) == "Double:String:Int:String2Int"
    result &= FunctionSignature.getSignatureDoubleStringIntBoolean2Int(helperDoubleStringIntBoolean2Int) == "Double:String:Int:Boolean2Int"
    result &= FunctionSignature.getSignatureDoubleStringLongInt2Int(helperDoubleStringLongInt2Int) == "Double:String:Long:Int2Int"
    result &= FunctionSignature.getSignatureDoubleStringLongLong2Int(helperDoubleStringLongLong2Int) == "Double:String:Long:Long2Int"
    result &= FunctionSignature.getSignatureDoubleStringLongDouble2Int(helperDoubleStringLongDouble2Int) == "Double:String:Long:Double2Int"
    result &= FunctionSignature.getSignatureDoubleStringLongString2Int(helperDoubleStringLongString2Int) == "Double:String:Long:String2Int"
    result &= FunctionSignature.getSignatureDoubleStringLongBoolean2Int(helperDoubleStringLongBoolean2Int) == "Double:String:Long:Boolean2Int"
    result &= FunctionSignature.getSignatureDoubleStringDoubleInt2Int(helperDoubleStringDoubleInt2Int) == "Double:String:Double:Int2Int"
    result &= FunctionSignature.getSignatureDoubleStringDoubleLong2Int(helperDoubleStringDoubleLong2Int) == "Double:String:Double:Long2Int"
    result &= FunctionSignature.getSignatureDoubleStringDoubleDouble2Int(helperDoubleStringDoubleDouble2Int) == "Double:String:Double:Double2Int"
    result &= FunctionSignature.getSignatureDoubleStringDoubleString2Int(helperDoubleStringDoubleString2Int) == "Double:String:Double:String2Int"
    result &= FunctionSignature.getSignatureDoubleStringDoubleBoolean2Int(helperDoubleStringDoubleBoolean2Int) == "Double:String:Double:Boolean2Int"
    result &= FunctionSignature.getSignatureDoubleStringStringInt2Int(helperDoubleStringStringInt2Int) == "Double:String:String:Int2Int"
    result &= FunctionSignature.getSignatureDoubleStringStringLong2Int(helperDoubleStringStringLong2Int) == "Double:String:String:Long2Int"
    result &= FunctionSignature.getSignatureDoubleStringStringDouble2Int(helperDoubleStringStringDouble2Int) == "Double:String:String:Double2Int"
    result &= FunctionSignature.getSignatureDoubleStringStringString2Int(helperDoubleStringStringString2Int) == "Double:String:String:String2Int"
    result &= FunctionSignature.getSignatureDoubleStringStringBoolean2Int(helperDoubleStringStringBoolean2Int) == "Double:String:String:Boolean2Int"
    result &= FunctionSignature.getSignatureDoubleStringBooleanInt2Int(helperDoubleStringBooleanInt2Int) == "Double:String:Boolean:Int2Int"
    result &= FunctionSignature.getSignatureDoubleStringBooleanLong2Int(helperDoubleStringBooleanLong2Int) == "Double:String:Boolean:Long2Int"
    result &= FunctionSignature.getSignatureDoubleStringBooleanDouble2Int(helperDoubleStringBooleanDouble2Int) == "Double:String:Boolean:Double2Int"
    result &= FunctionSignature.getSignatureDoubleStringBooleanString2Int(helperDoubleStringBooleanString2Int) == "Double:String:Boolean:String2Int"
    result &= FunctionSignature.getSignatureDoubleStringBooleanBoolean2Int(helperDoubleStringBooleanBoolean2Int) == "Double:String:Boolean:Boolean2Int"
    result &= FunctionSignature.getSignatureDoubleBooleanIntInt2Int(helperDoubleBooleanIntInt2Int) == "Double:Boolean:Int:Int2Int"
    result &= FunctionSignature.getSignatureDoubleBooleanIntLong2Int(helperDoubleBooleanIntLong2Int) == "Double:Boolean:Int:Long2Int"
    result &= FunctionSignature.getSignatureDoubleBooleanIntDouble2Int(helperDoubleBooleanIntDouble2Int) == "Double:Boolean:Int:Double2Int"
    result &= FunctionSignature.getSignatureDoubleBooleanIntString2Int(helperDoubleBooleanIntString2Int) == "Double:Boolean:Int:String2Int"
    result &= FunctionSignature.getSignatureDoubleBooleanIntBoolean2Int(helperDoubleBooleanIntBoolean2Int) == "Double:Boolean:Int:Boolean2Int"
    result &= FunctionSignature.getSignatureDoubleBooleanLongInt2Int(helperDoubleBooleanLongInt2Int) == "Double:Boolean:Long:Int2Int"
    result &= FunctionSignature.getSignatureDoubleBooleanLongLong2Int(helperDoubleBooleanLongLong2Int) == "Double:Boolean:Long:Long2Int"
    result &= FunctionSignature.getSignatureDoubleBooleanLongDouble2Int(helperDoubleBooleanLongDouble2Int) == "Double:Boolean:Long:Double2Int"
    result &= FunctionSignature.getSignatureDoubleBooleanLongString2Int(helperDoubleBooleanLongString2Int) == "Double:Boolean:Long:String2Int"
    result &= FunctionSignature.getSignatureDoubleBooleanLongBoolean2Int(helperDoubleBooleanLongBoolean2Int) == "Double:Boolean:Long:Boolean2Int"
    result &= FunctionSignature.getSignatureDoubleBooleanDoubleInt2Int(helperDoubleBooleanDoubleInt2Int) == "Double:Boolean:Double:Int2Int"
    result &= FunctionSignature.getSignatureDoubleBooleanDoubleLong2Int(helperDoubleBooleanDoubleLong2Int) == "Double:Boolean:Double:Long2Int"
    result &= FunctionSignature.getSignatureDoubleBooleanDoubleDouble2Int(helperDoubleBooleanDoubleDouble2Int) == "Double:Boolean:Double:Double2Int"
    result &= FunctionSignature.getSignatureDoubleBooleanDoubleString2Int(helperDoubleBooleanDoubleString2Int) == "Double:Boolean:Double:String2Int"
    result &= FunctionSignature.getSignatureDoubleBooleanDoubleBoolean2Int(helperDoubleBooleanDoubleBoolean2Int) == "Double:Boolean:Double:Boolean2Int"
    result &= FunctionSignature.getSignatureDoubleBooleanStringInt2Int(helperDoubleBooleanStringInt2Int) == "Double:Boolean:String:Int2Int"
    result &= FunctionSignature.getSignatureDoubleBooleanStringLong2Int(helperDoubleBooleanStringLong2Int) == "Double:Boolean:String:Long2Int"
    result &= FunctionSignature.getSignatureDoubleBooleanStringDouble2Int(helperDoubleBooleanStringDouble2Int) == "Double:Boolean:String:Double2Int"
    result &= FunctionSignature.getSignatureDoubleBooleanStringString2Int(helperDoubleBooleanStringString2Int) == "Double:Boolean:String:String2Int"
    result &= FunctionSignature.getSignatureDoubleBooleanStringBoolean2Int(helperDoubleBooleanStringBoolean2Int) == "Double:Boolean:String:Boolean2Int"
    result &= FunctionSignature.getSignatureDoubleBooleanBooleanInt2Int(helperDoubleBooleanBooleanInt2Int) == "Double:Boolean:Boolean:Int2Int"
    result &= FunctionSignature.getSignatureDoubleBooleanBooleanLong2Int(helperDoubleBooleanBooleanLong2Int) == "Double:Boolean:Boolean:Long2Int"
    result &= FunctionSignature.getSignatureDoubleBooleanBooleanDouble2Int(helperDoubleBooleanBooleanDouble2Int) == "Double:Boolean:Boolean:Double2Int"
    result &= FunctionSignature.getSignatureDoubleBooleanBooleanString2Int(helperDoubleBooleanBooleanString2Int) == "Double:Boolean:Boolean:String2Int"
    result &= FunctionSignature.getSignatureDoubleBooleanBooleanBoolean2Int(helperDoubleBooleanBooleanBoolean2Int) == "Double:Boolean:Boolean:Boolean2Int"
    result &= FunctionSignature.getSignatureStringIntIntInt2Int(helperStringIntIntInt2Int) == "String:Int:Int:Int2Int"
    result &= FunctionSignature.getSignatureStringIntIntLong2Int(helperStringIntIntLong2Int) == "String:Int:Int:Long2Int"
    result &= FunctionSignature.getSignatureStringIntIntDouble2Int(helperStringIntIntDouble2Int) == "String:Int:Int:Double2Int"
    result &= FunctionSignature.getSignatureStringIntIntString2Int(helperStringIntIntString2Int) == "String:Int:Int:String2Int"
    result &= FunctionSignature.getSignatureStringIntIntBoolean2Int(helperStringIntIntBoolean2Int) == "String:Int:Int:Boolean2Int"
    result &= FunctionSignature.getSignatureStringIntLongInt2Int(helperStringIntLongInt2Int) == "String:Int:Long:Int2Int"
    result &= FunctionSignature.getSignatureStringIntLongLong2Int(helperStringIntLongLong2Int) == "String:Int:Long:Long2Int"
    result &= FunctionSignature.getSignatureStringIntLongDouble2Int(helperStringIntLongDouble2Int) == "String:Int:Long:Double2Int"
    result &= FunctionSignature.getSignatureStringIntLongString2Int(helperStringIntLongString2Int) == "String:Int:Long:String2Int"
    result &= FunctionSignature.getSignatureStringIntLongBoolean2Int(helperStringIntLongBoolean2Int) == "String:Int:Long:Boolean2Int"
    result &= FunctionSignature.getSignatureStringIntDoubleInt2Int(helperStringIntDoubleInt2Int) == "String:Int:Double:Int2Int"
    result &= FunctionSignature.getSignatureStringIntDoubleLong2Int(helperStringIntDoubleLong2Int) == "String:Int:Double:Long2Int"
    result &= FunctionSignature.getSignatureStringIntDoubleDouble2Int(helperStringIntDoubleDouble2Int) == "String:Int:Double:Double2Int"
    result &= FunctionSignature.getSignatureStringIntDoubleString2Int(helperStringIntDoubleString2Int) == "String:Int:Double:String2Int"
    result &= FunctionSignature.getSignatureStringIntDoubleBoolean2Int(helperStringIntDoubleBoolean2Int) == "String:Int:Double:Boolean2Int"
    result &= FunctionSignature.getSignatureStringIntStringInt2Int(helperStringIntStringInt2Int) == "String:Int:String:Int2Int"
    result &= FunctionSignature.getSignatureStringIntStringLong2Int(helperStringIntStringLong2Int) == "String:Int:String:Long2Int"
    result &= FunctionSignature.getSignatureStringIntStringDouble2Int(helperStringIntStringDouble2Int) == "String:Int:String:Double2Int"
    result &= FunctionSignature.getSignatureStringIntStringString2Int(helperStringIntStringString2Int) == "String:Int:String:String2Int"
    result &= FunctionSignature.getSignatureStringIntStringBoolean2Int(helperStringIntStringBoolean2Int) == "String:Int:String:Boolean2Int"
    result &= FunctionSignature.getSignatureStringIntBooleanInt2Int(helperStringIntBooleanInt2Int) == "String:Int:Boolean:Int2Int"
    result &= FunctionSignature.getSignatureStringIntBooleanLong2Int(helperStringIntBooleanLong2Int) == "String:Int:Boolean:Long2Int"
    result &= FunctionSignature.getSignatureStringIntBooleanDouble2Int(helperStringIntBooleanDouble2Int) == "String:Int:Boolean:Double2Int"
    result &= FunctionSignature.getSignatureStringIntBooleanString2Int(helperStringIntBooleanString2Int) == "String:Int:Boolean:String2Int"
    result &= FunctionSignature.getSignatureStringIntBooleanBoolean2Int(helperStringIntBooleanBoolean2Int) == "String:Int:Boolean:Boolean2Int"
    result &= FunctionSignature.getSignatureStringLongIntInt2Int(helperStringLongIntInt2Int) == "String:Long:Int:Int2Int"
    result &= FunctionSignature.getSignatureStringLongIntLong2Int(helperStringLongIntLong2Int) == "String:Long:Int:Long2Int"
    result &= FunctionSignature.getSignatureStringLongIntDouble2Int(helperStringLongIntDouble2Int) == "String:Long:Int:Double2Int"
    result &= FunctionSignature.getSignatureStringLongIntString2Int(helperStringLongIntString2Int) == "String:Long:Int:String2Int"
    result &= FunctionSignature.getSignatureStringLongIntBoolean2Int(helperStringLongIntBoolean2Int) == "String:Long:Int:Boolean2Int"
    result &= FunctionSignature.getSignatureStringLongLongInt2Int(helperStringLongLongInt2Int) == "String:Long:Long:Int2Int"
    result &= FunctionSignature.getSignatureStringLongLongLong2Int(helperStringLongLongLong2Int) == "String:Long:Long:Long2Int"
    result &= FunctionSignature.getSignatureStringLongLongDouble2Int(helperStringLongLongDouble2Int) == "String:Long:Long:Double2Int"
    result &= FunctionSignature.getSignatureStringLongLongString2Int(helperStringLongLongString2Int) == "String:Long:Long:String2Int"
    result &= FunctionSignature.getSignatureStringLongLongBoolean2Int(helperStringLongLongBoolean2Int) == "String:Long:Long:Boolean2Int"
    result &= FunctionSignature.getSignatureStringLongDoubleInt2Int(helperStringLongDoubleInt2Int) == "String:Long:Double:Int2Int"
    result &= FunctionSignature.getSignatureStringLongDoubleLong2Int(helperStringLongDoubleLong2Int) == "String:Long:Double:Long2Int"
    result &= FunctionSignature.getSignatureStringLongDoubleDouble2Int(helperStringLongDoubleDouble2Int) == "String:Long:Double:Double2Int"
    result &= FunctionSignature.getSignatureStringLongDoubleString2Int(helperStringLongDoubleString2Int) == "String:Long:Double:String2Int"
    result &= FunctionSignature.getSignatureStringLongDoubleBoolean2Int(helperStringLongDoubleBoolean2Int) == "String:Long:Double:Boolean2Int"
    result &= FunctionSignature.getSignatureStringLongStringInt2Int(helperStringLongStringInt2Int) == "String:Long:String:Int2Int"
    result &= FunctionSignature.getSignatureStringLongStringLong2Int(helperStringLongStringLong2Int) == "String:Long:String:Long2Int"
    result &= FunctionSignature.getSignatureStringLongStringDouble2Int(helperStringLongStringDouble2Int) == "String:Long:String:Double2Int"
    result &= FunctionSignature.getSignatureStringLongStringString2Int(helperStringLongStringString2Int) == "String:Long:String:String2Int"
    result &= FunctionSignature.getSignatureStringLongStringBoolean2Int(helperStringLongStringBoolean2Int) == "String:Long:String:Boolean2Int"
    result &= FunctionSignature.getSignatureStringLongBooleanInt2Int(helperStringLongBooleanInt2Int) == "String:Long:Boolean:Int2Int"
    result &= FunctionSignature.getSignatureStringLongBooleanLong2Int(helperStringLongBooleanLong2Int) == "String:Long:Boolean:Long2Int"
    result &= FunctionSignature.getSignatureStringLongBooleanDouble2Int(helperStringLongBooleanDouble2Int) == "String:Long:Boolean:Double2Int"
    result &= FunctionSignature.getSignatureStringLongBooleanString2Int(helperStringLongBooleanString2Int) == "String:Long:Boolean:String2Int"
    result &= FunctionSignature.getSignatureStringLongBooleanBoolean2Int(helperStringLongBooleanBoolean2Int) == "String:Long:Boolean:Boolean2Int"
    result &= FunctionSignature.getSignatureStringDoubleIntInt2Int(helperStringDoubleIntInt2Int) == "String:Double:Int:Int2Int"
    result &= FunctionSignature.getSignatureStringDoubleIntLong2Int(helperStringDoubleIntLong2Int) == "String:Double:Int:Long2Int"
    result &= FunctionSignature.getSignatureStringDoubleIntDouble2Int(helperStringDoubleIntDouble2Int) == "String:Double:Int:Double2Int"
    result &= FunctionSignature.getSignatureStringDoubleIntString2Int(helperStringDoubleIntString2Int) == "String:Double:Int:String2Int"
    result &= FunctionSignature.getSignatureStringDoubleIntBoolean2Int(helperStringDoubleIntBoolean2Int) == "String:Double:Int:Boolean2Int"
    result &= FunctionSignature.getSignatureStringDoubleLongInt2Int(helperStringDoubleLongInt2Int) == "String:Double:Long:Int2Int"
    result &= FunctionSignature.getSignatureStringDoubleLongLong2Int(helperStringDoubleLongLong2Int) == "String:Double:Long:Long2Int"
    result &= FunctionSignature.getSignatureStringDoubleLongDouble2Int(helperStringDoubleLongDouble2Int) == "String:Double:Long:Double2Int"
    result &= FunctionSignature.getSignatureStringDoubleLongString2Int(helperStringDoubleLongString2Int) == "String:Double:Long:String2Int"
    result &= FunctionSignature.getSignatureStringDoubleLongBoolean2Int(helperStringDoubleLongBoolean2Int) == "String:Double:Long:Boolean2Int"
    result &= FunctionSignature.getSignatureStringDoubleDoubleInt2Int(helperStringDoubleDoubleInt2Int) == "String:Double:Double:Int2Int"
    result &= FunctionSignature.getSignatureStringDoubleDoubleLong2Int(helperStringDoubleDoubleLong2Int) == "String:Double:Double:Long2Int"
    result &= FunctionSignature.getSignatureStringDoubleDoubleDouble2Int(helperStringDoubleDoubleDouble2Int) == "String:Double:Double:Double2Int"
    result &= FunctionSignature.getSignatureStringDoubleDoubleString2Int(helperStringDoubleDoubleString2Int) == "String:Double:Double:String2Int"
    result &= FunctionSignature.getSignatureStringDoubleDoubleBoolean2Int(helperStringDoubleDoubleBoolean2Int) == "String:Double:Double:Boolean2Int"
    result &= FunctionSignature.getSignatureStringDoubleStringInt2Int(helperStringDoubleStringInt2Int) == "String:Double:String:Int2Int"
    result &= FunctionSignature.getSignatureStringDoubleStringLong2Int(helperStringDoubleStringLong2Int) == "String:Double:String:Long2Int"
    result &= FunctionSignature.getSignatureStringDoubleStringDouble2Int(helperStringDoubleStringDouble2Int) == "String:Double:String:Double2Int"
    result &= FunctionSignature.getSignatureStringDoubleStringString2Int(helperStringDoubleStringString2Int) == "String:Double:String:String2Int"
    result &= FunctionSignature.getSignatureStringDoubleStringBoolean2Int(helperStringDoubleStringBoolean2Int) == "String:Double:String:Boolean2Int"
    result &= FunctionSignature.getSignatureStringDoubleBooleanInt2Int(helperStringDoubleBooleanInt2Int) == "String:Double:Boolean:Int2Int"
    result &= FunctionSignature.getSignatureStringDoubleBooleanLong2Int(helperStringDoubleBooleanLong2Int) == "String:Double:Boolean:Long2Int"
    result &= FunctionSignature.getSignatureStringDoubleBooleanDouble2Int(helperStringDoubleBooleanDouble2Int) == "String:Double:Boolean:Double2Int"
    result &= FunctionSignature.getSignatureStringDoubleBooleanString2Int(helperStringDoubleBooleanString2Int) == "String:Double:Boolean:String2Int"
    result &= FunctionSignature.getSignatureStringDoubleBooleanBoolean2Int(helperStringDoubleBooleanBoolean2Int) == "String:Double:Boolean:Boolean2Int"
    result &= FunctionSignature.getSignatureStringStringIntInt2Int(helperStringStringIntInt2Int) == "String:String:Int:Int2Int"
    result &= FunctionSignature.getSignatureStringStringIntLong2Int(helperStringStringIntLong2Int) == "String:String:Int:Long2Int"
    result &= FunctionSignature.getSignatureStringStringIntDouble2Int(helperStringStringIntDouble2Int) == "String:String:Int:Double2Int"
    result &= FunctionSignature.getSignatureStringStringIntString2Int(helperStringStringIntString2Int) == "String:String:Int:String2Int"
    result &= FunctionSignature.getSignatureStringStringIntBoolean2Int(helperStringStringIntBoolean2Int) == "String:String:Int:Boolean2Int"
    result &= FunctionSignature.getSignatureStringStringLongInt2Int(helperStringStringLongInt2Int) == "String:String:Long:Int2Int"
    result &= FunctionSignature.getSignatureStringStringLongLong2Int(helperStringStringLongLong2Int) == "String:String:Long:Long2Int"
    result &= FunctionSignature.getSignatureStringStringLongDouble2Int(helperStringStringLongDouble2Int) == "String:String:Long:Double2Int"
    result &= FunctionSignature.getSignatureStringStringLongString2Int(helperStringStringLongString2Int) == "String:String:Long:String2Int"
    result &= FunctionSignature.getSignatureStringStringLongBoolean2Int(helperStringStringLongBoolean2Int) == "String:String:Long:Boolean2Int"
    result &= FunctionSignature.getSignatureStringStringDoubleInt2Int(helperStringStringDoubleInt2Int) == "String:String:Double:Int2Int"
    result &= FunctionSignature.getSignatureStringStringDoubleLong2Int(helperStringStringDoubleLong2Int) == "String:String:Double:Long2Int"
    result &= FunctionSignature.getSignatureStringStringDoubleDouble2Int(helperStringStringDoubleDouble2Int) == "String:String:Double:Double2Int"
    result &= FunctionSignature.getSignatureStringStringDoubleString2Int(helperStringStringDoubleString2Int) == "String:String:Double:String2Int"
    result &= FunctionSignature.getSignatureStringStringDoubleBoolean2Int(helperStringStringDoubleBoolean2Int) == "String:String:Double:Boolean2Int"
    result &= FunctionSignature.getSignatureStringStringStringInt2Int(helperStringStringStringInt2Int) == "String:String:String:Int2Int"
    result &= FunctionSignature.getSignatureStringStringStringLong2Int(helperStringStringStringLong2Int) == "String:String:String:Long2Int"
    result &= FunctionSignature.getSignatureStringStringStringDouble2Int(helperStringStringStringDouble2Int) == "String:String:String:Double2Int"
    result &= FunctionSignature.getSignatureStringStringStringString2Int(helperStringStringStringString2Int) == "String:String:String:String2Int"
    result &= FunctionSignature.getSignatureStringStringStringBoolean2Int(helperStringStringStringBoolean2Int) == "String:String:String:Boolean2Int"
    result &= FunctionSignature.getSignatureStringStringBooleanInt2Int(helperStringStringBooleanInt2Int) == "String:String:Boolean:Int2Int"
    result &= FunctionSignature.getSignatureStringStringBooleanLong2Int(helperStringStringBooleanLong2Int) == "String:String:Boolean:Long2Int"
    result &= FunctionSignature.getSignatureStringStringBooleanDouble2Int(helperStringStringBooleanDouble2Int) == "String:String:Boolean:Double2Int"
    result &= FunctionSignature.getSignatureStringStringBooleanString2Int(helperStringStringBooleanString2Int) == "String:String:Boolean:String2Int"
    result &= FunctionSignature.getSignatureStringStringBooleanBoolean2Int(helperStringStringBooleanBoolean2Int) == "String:String:Boolean:Boolean2Int"
    result &= FunctionSignature.getSignatureStringBooleanIntInt2Int(helperStringBooleanIntInt2Int) == "String:Boolean:Int:Int2Int"
    result &= FunctionSignature.getSignatureStringBooleanIntLong2Int(helperStringBooleanIntLong2Int) == "String:Boolean:Int:Long2Int"
    result &= FunctionSignature.getSignatureStringBooleanIntDouble2Int(helperStringBooleanIntDouble2Int) == "String:Boolean:Int:Double2Int"
    result &= FunctionSignature.getSignatureStringBooleanIntString2Int(helperStringBooleanIntString2Int) == "String:Boolean:Int:String2Int"
    result &= FunctionSignature.getSignatureStringBooleanIntBoolean2Int(helperStringBooleanIntBoolean2Int) == "String:Boolean:Int:Boolean2Int"
    result &= FunctionSignature.getSignatureStringBooleanLongInt2Int(helperStringBooleanLongInt2Int) == "String:Boolean:Long:Int2Int"
    result &= FunctionSignature.getSignatureStringBooleanLongLong2Int(helperStringBooleanLongLong2Int) == "String:Boolean:Long:Long2Int"
    result &= FunctionSignature.getSignatureStringBooleanLongDouble2Int(helperStringBooleanLongDouble2Int) == "String:Boolean:Long:Double2Int"
    result &= FunctionSignature.getSignatureStringBooleanLongString2Int(helperStringBooleanLongString2Int) == "String:Boolean:Long:String2Int"
    result &= FunctionSignature.getSignatureStringBooleanLongBoolean2Int(helperStringBooleanLongBoolean2Int) == "String:Boolean:Long:Boolean2Int"
    result &= FunctionSignature.getSignatureStringBooleanDoubleInt2Int(helperStringBooleanDoubleInt2Int) == "String:Boolean:Double:Int2Int"
    result &= FunctionSignature.getSignatureStringBooleanDoubleLong2Int(helperStringBooleanDoubleLong2Int) == "String:Boolean:Double:Long2Int"
    result &= FunctionSignature.getSignatureStringBooleanDoubleDouble2Int(helperStringBooleanDoubleDouble2Int) == "String:Boolean:Double:Double2Int"
    result &= FunctionSignature.getSignatureStringBooleanDoubleString2Int(helperStringBooleanDoubleString2Int) == "String:Boolean:Double:String2Int"
    result &= FunctionSignature.getSignatureStringBooleanDoubleBoolean2Int(helperStringBooleanDoubleBoolean2Int) == "String:Boolean:Double:Boolean2Int"
    result &= FunctionSignature.getSignatureStringBooleanStringInt2Int(helperStringBooleanStringInt2Int) == "String:Boolean:String:Int2Int"
    result &= FunctionSignature.getSignatureStringBooleanStringLong2Int(helperStringBooleanStringLong2Int) == "String:Boolean:String:Long2Int"
    result &= FunctionSignature.getSignatureStringBooleanStringDouble2Int(helperStringBooleanStringDouble2Int) == "String:Boolean:String:Double2Int"
    result &= FunctionSignature.getSignatureStringBooleanStringString2Int(helperStringBooleanStringString2Int) == "String:Boolean:String:String2Int"
    result &= FunctionSignature.getSignatureStringBooleanStringBoolean2Int(helperStringBooleanStringBoolean2Int) == "String:Boolean:String:Boolean2Int"
    result &= FunctionSignature.getSignatureStringBooleanBooleanInt2Int(helperStringBooleanBooleanInt2Int) == "String:Boolean:Boolean:Int2Int"
    result &= FunctionSignature.getSignatureStringBooleanBooleanLong2Int(helperStringBooleanBooleanLong2Int) == "String:Boolean:Boolean:Long2Int"
    result &= FunctionSignature.getSignatureStringBooleanBooleanDouble2Int(helperStringBooleanBooleanDouble2Int) == "String:Boolean:Boolean:Double2Int"
    result &= FunctionSignature.getSignatureStringBooleanBooleanString2Int(helperStringBooleanBooleanString2Int) == "String:Boolean:Boolean:String2Int"
    result &= FunctionSignature.getSignatureStringBooleanBooleanBoolean2Int(helperStringBooleanBooleanBoolean2Int) == "String:Boolean:Boolean:Boolean2Int"
    result &= FunctionSignature.getSignatureBooleanIntIntInt2Int(helperBooleanIntIntInt2Int) == "Boolean:Int:Int:Int2Int"
    result &= FunctionSignature.getSignatureBooleanIntIntLong2Int(helperBooleanIntIntLong2Int) == "Boolean:Int:Int:Long2Int"
    result &= FunctionSignature.getSignatureBooleanIntIntDouble2Int(helperBooleanIntIntDouble2Int) == "Boolean:Int:Int:Double2Int"
    result &= FunctionSignature.getSignatureBooleanIntIntString2Int(helperBooleanIntIntString2Int) == "Boolean:Int:Int:String2Int"
    result &= FunctionSignature.getSignatureBooleanIntIntBoolean2Int(helperBooleanIntIntBoolean2Int) == "Boolean:Int:Int:Boolean2Int"
    result &= FunctionSignature.getSignatureBooleanIntLongInt2Int(helperBooleanIntLongInt2Int) == "Boolean:Int:Long:Int2Int"
    result &= FunctionSignature.getSignatureBooleanIntLongLong2Int(helperBooleanIntLongLong2Int) == "Boolean:Int:Long:Long2Int"
    result &= FunctionSignature.getSignatureBooleanIntLongDouble2Int(helperBooleanIntLongDouble2Int) == "Boolean:Int:Long:Double2Int"
    result &= FunctionSignature.getSignatureBooleanIntLongString2Int(helperBooleanIntLongString2Int) == "Boolean:Int:Long:String2Int"
    result &= FunctionSignature.getSignatureBooleanIntLongBoolean2Int(helperBooleanIntLongBoolean2Int) == "Boolean:Int:Long:Boolean2Int"
    result &= FunctionSignature.getSignatureBooleanIntDoubleInt2Int(helperBooleanIntDoubleInt2Int) == "Boolean:Int:Double:Int2Int"
    result &= FunctionSignature.getSignatureBooleanIntDoubleLong2Int(helperBooleanIntDoubleLong2Int) == "Boolean:Int:Double:Long2Int"
    result &= FunctionSignature.getSignatureBooleanIntDoubleDouble2Int(helperBooleanIntDoubleDouble2Int) == "Boolean:Int:Double:Double2Int"
    result &= FunctionSignature.getSignatureBooleanIntDoubleString2Int(helperBooleanIntDoubleString2Int) == "Boolean:Int:Double:String2Int"
    result &= FunctionSignature.getSignatureBooleanIntDoubleBoolean2Int(helperBooleanIntDoubleBoolean2Int) == "Boolean:Int:Double:Boolean2Int"
    result &= FunctionSignature.getSignatureBooleanIntStringInt2Int(helperBooleanIntStringInt2Int) == "Boolean:Int:String:Int2Int"
    result &= FunctionSignature.getSignatureBooleanIntStringLong2Int(helperBooleanIntStringLong2Int) == "Boolean:Int:String:Long2Int"
    result &= FunctionSignature.getSignatureBooleanIntStringDouble2Int(helperBooleanIntStringDouble2Int) == "Boolean:Int:String:Double2Int"
    result &= FunctionSignature.getSignatureBooleanIntStringString2Int(helperBooleanIntStringString2Int) == "Boolean:Int:String:String2Int"
    result &= FunctionSignature.getSignatureBooleanIntStringBoolean2Int(helperBooleanIntStringBoolean2Int) == "Boolean:Int:String:Boolean2Int"
    result &= FunctionSignature.getSignatureBooleanIntBooleanInt2Int(helperBooleanIntBooleanInt2Int) == "Boolean:Int:Boolean:Int2Int"
    result &= FunctionSignature.getSignatureBooleanIntBooleanLong2Int(helperBooleanIntBooleanLong2Int) == "Boolean:Int:Boolean:Long2Int"
    result &= FunctionSignature.getSignatureBooleanIntBooleanDouble2Int(helperBooleanIntBooleanDouble2Int) == "Boolean:Int:Boolean:Double2Int"
    result &= FunctionSignature.getSignatureBooleanIntBooleanString2Int(helperBooleanIntBooleanString2Int) == "Boolean:Int:Boolean:String2Int"
    result &= FunctionSignature.getSignatureBooleanIntBooleanBoolean2Int(helperBooleanIntBooleanBoolean2Int) == "Boolean:Int:Boolean:Boolean2Int"
    result &= FunctionSignature.getSignatureBooleanLongIntInt2Int(helperBooleanLongIntInt2Int) == "Boolean:Long:Int:Int2Int"
    result &= FunctionSignature.getSignatureBooleanLongIntLong2Int(helperBooleanLongIntLong2Int) == "Boolean:Long:Int:Long2Int"
    result &= FunctionSignature.getSignatureBooleanLongIntDouble2Int(helperBooleanLongIntDouble2Int) == "Boolean:Long:Int:Double2Int"
    result &= FunctionSignature.getSignatureBooleanLongIntString2Int(helperBooleanLongIntString2Int) == "Boolean:Long:Int:String2Int"
    result &= FunctionSignature.getSignatureBooleanLongIntBoolean2Int(helperBooleanLongIntBoolean2Int) == "Boolean:Long:Int:Boolean2Int"
    result &= FunctionSignature.getSignatureBooleanLongLongInt2Int(helperBooleanLongLongInt2Int) == "Boolean:Long:Long:Int2Int"
    result &= FunctionSignature.getSignatureBooleanLongLongLong2Int(helperBooleanLongLongLong2Int) == "Boolean:Long:Long:Long2Int"
    result &= FunctionSignature.getSignatureBooleanLongLongDouble2Int(helperBooleanLongLongDouble2Int) == "Boolean:Long:Long:Double2Int"
    result &= FunctionSignature.getSignatureBooleanLongLongString2Int(helperBooleanLongLongString2Int) == "Boolean:Long:Long:String2Int"
    result &= FunctionSignature.getSignatureBooleanLongLongBoolean2Int(helperBooleanLongLongBoolean2Int) == "Boolean:Long:Long:Boolean2Int"
    result &= FunctionSignature.getSignatureBooleanLongDoubleInt2Int(helperBooleanLongDoubleInt2Int) == "Boolean:Long:Double:Int2Int"
    result &= FunctionSignature.getSignatureBooleanLongDoubleLong2Int(helperBooleanLongDoubleLong2Int) == "Boolean:Long:Double:Long2Int"
    result &= FunctionSignature.getSignatureBooleanLongDoubleDouble2Int(helperBooleanLongDoubleDouble2Int) == "Boolean:Long:Double:Double2Int"
    result &= FunctionSignature.getSignatureBooleanLongDoubleString2Int(helperBooleanLongDoubleString2Int) == "Boolean:Long:Double:String2Int"
    result &= FunctionSignature.getSignatureBooleanLongDoubleBoolean2Int(helperBooleanLongDoubleBoolean2Int) == "Boolean:Long:Double:Boolean2Int"
    result &= FunctionSignature.getSignatureBooleanLongStringInt2Int(helperBooleanLongStringInt2Int) == "Boolean:Long:String:Int2Int"
    result &= FunctionSignature.getSignatureBooleanLongStringLong2Int(helperBooleanLongStringLong2Int) == "Boolean:Long:String:Long2Int"
    result &= FunctionSignature.getSignatureBooleanLongStringDouble2Int(helperBooleanLongStringDouble2Int) == "Boolean:Long:String:Double2Int"
    result &= FunctionSignature.getSignatureBooleanLongStringString2Int(helperBooleanLongStringString2Int) == "Boolean:Long:String:String2Int"
    result &= FunctionSignature.getSignatureBooleanLongStringBoolean2Int(helperBooleanLongStringBoolean2Int) == "Boolean:Long:String:Boolean2Int"
    result &= FunctionSignature.getSignatureBooleanLongBooleanInt2Int(helperBooleanLongBooleanInt2Int) == "Boolean:Long:Boolean:Int2Int"
    result &= FunctionSignature.getSignatureBooleanLongBooleanLong2Int(helperBooleanLongBooleanLong2Int) == "Boolean:Long:Boolean:Long2Int"
    result &= FunctionSignature.getSignatureBooleanLongBooleanDouble2Int(helperBooleanLongBooleanDouble2Int) == "Boolean:Long:Boolean:Double2Int"
    result &= FunctionSignature.getSignatureBooleanLongBooleanString2Int(helperBooleanLongBooleanString2Int) == "Boolean:Long:Boolean:String2Int"
    result &= FunctionSignature.getSignatureBooleanLongBooleanBoolean2Int(helperBooleanLongBooleanBoolean2Int) == "Boolean:Long:Boolean:Boolean2Int"
    result &= FunctionSignature.getSignatureBooleanDoubleIntInt2Int(helperBooleanDoubleIntInt2Int) == "Boolean:Double:Int:Int2Int"
    result &= FunctionSignature.getSignatureBooleanDoubleIntLong2Int(helperBooleanDoubleIntLong2Int) == "Boolean:Double:Int:Long2Int"
    result &= FunctionSignature.getSignatureBooleanDoubleIntDouble2Int(helperBooleanDoubleIntDouble2Int) == "Boolean:Double:Int:Double2Int"
    result &= FunctionSignature.getSignatureBooleanDoubleIntString2Int(helperBooleanDoubleIntString2Int) == "Boolean:Double:Int:String2Int"
    result &= FunctionSignature.getSignatureBooleanDoubleIntBoolean2Int(helperBooleanDoubleIntBoolean2Int) == "Boolean:Double:Int:Boolean2Int"
    result &= FunctionSignature.getSignatureBooleanDoubleLongInt2Int(helperBooleanDoubleLongInt2Int) == "Boolean:Double:Long:Int2Int"
    result &= FunctionSignature.getSignatureBooleanDoubleLongLong2Int(helperBooleanDoubleLongLong2Int) == "Boolean:Double:Long:Long2Int"
    result &= FunctionSignature.getSignatureBooleanDoubleLongDouble2Int(helperBooleanDoubleLongDouble2Int) == "Boolean:Double:Long:Double2Int"
    result &= FunctionSignature.getSignatureBooleanDoubleLongString2Int(helperBooleanDoubleLongString2Int) == "Boolean:Double:Long:String2Int"
    result &= FunctionSignature.getSignatureBooleanDoubleLongBoolean2Int(helperBooleanDoubleLongBoolean2Int) == "Boolean:Double:Long:Boolean2Int"
    result &= FunctionSignature.getSignatureBooleanDoubleDoubleInt2Int(helperBooleanDoubleDoubleInt2Int) == "Boolean:Double:Double:Int2Int"
    result &= FunctionSignature.getSignatureBooleanDoubleDoubleLong2Int(helperBooleanDoubleDoubleLong2Int) == "Boolean:Double:Double:Long2Int"
    result &= FunctionSignature.getSignatureBooleanDoubleDoubleDouble2Int(helperBooleanDoubleDoubleDouble2Int) == "Boolean:Double:Double:Double2Int"
    result &= FunctionSignature.getSignatureBooleanDoubleDoubleString2Int(helperBooleanDoubleDoubleString2Int) == "Boolean:Double:Double:String2Int"
    result &= FunctionSignature.getSignatureBooleanDoubleDoubleBoolean2Int(helperBooleanDoubleDoubleBoolean2Int) == "Boolean:Double:Double:Boolean2Int"
    result &= FunctionSignature.getSignatureBooleanDoubleStringInt2Int(helperBooleanDoubleStringInt2Int) == "Boolean:Double:String:Int2Int"
    result &= FunctionSignature.getSignatureBooleanDoubleStringLong2Int(helperBooleanDoubleStringLong2Int) == "Boolean:Double:String:Long2Int"
    result &= FunctionSignature.getSignatureBooleanDoubleStringDouble2Int(helperBooleanDoubleStringDouble2Int) == "Boolean:Double:String:Double2Int"
    result &= FunctionSignature.getSignatureBooleanDoubleStringString2Int(helperBooleanDoubleStringString2Int) == "Boolean:Double:String:String2Int"
    result &= FunctionSignature.getSignatureBooleanDoubleStringBoolean2Int(helperBooleanDoubleStringBoolean2Int) == "Boolean:Double:String:Boolean2Int"
    result &= FunctionSignature.getSignatureBooleanDoubleBooleanInt2Int(helperBooleanDoubleBooleanInt2Int) == "Boolean:Double:Boolean:Int2Int"
    result &= FunctionSignature.getSignatureBooleanDoubleBooleanLong2Int(helperBooleanDoubleBooleanLong2Int) == "Boolean:Double:Boolean:Long2Int"
    result &= FunctionSignature.getSignatureBooleanDoubleBooleanDouble2Int(helperBooleanDoubleBooleanDouble2Int) == "Boolean:Double:Boolean:Double2Int"
    result &= FunctionSignature.getSignatureBooleanDoubleBooleanString2Int(helperBooleanDoubleBooleanString2Int) == "Boolean:Double:Boolean:String2Int"
    result &= FunctionSignature.getSignatureBooleanDoubleBooleanBoolean2Int(helperBooleanDoubleBooleanBoolean2Int) == "Boolean:Double:Boolean:Boolean2Int"
    result &= FunctionSignature.getSignatureBooleanStringIntInt2Int(helperBooleanStringIntInt2Int) == "Boolean:String:Int:Int2Int"
    result &= FunctionSignature.getSignatureBooleanStringIntLong2Int(helperBooleanStringIntLong2Int) == "Boolean:String:Int:Long2Int"
    result &= FunctionSignature.getSignatureBooleanStringIntDouble2Int(helperBooleanStringIntDouble2Int) == "Boolean:String:Int:Double2Int"
    result &= FunctionSignature.getSignatureBooleanStringIntString2Int(helperBooleanStringIntString2Int) == "Boolean:String:Int:String2Int"
    result &= FunctionSignature.getSignatureBooleanStringIntBoolean2Int(helperBooleanStringIntBoolean2Int) == "Boolean:String:Int:Boolean2Int"
    result &= FunctionSignature.getSignatureBooleanStringLongInt2Int(helperBooleanStringLongInt2Int) == "Boolean:String:Long:Int2Int"
    result &= FunctionSignature.getSignatureBooleanStringLongLong2Int(helperBooleanStringLongLong2Int) == "Boolean:String:Long:Long2Int"
    result &= FunctionSignature.getSignatureBooleanStringLongDouble2Int(helperBooleanStringLongDouble2Int) == "Boolean:String:Long:Double2Int"
    result &= FunctionSignature.getSignatureBooleanStringLongString2Int(helperBooleanStringLongString2Int) == "Boolean:String:Long:String2Int"
    result &= FunctionSignature.getSignatureBooleanStringLongBoolean2Int(helperBooleanStringLongBoolean2Int) == "Boolean:String:Long:Boolean2Int"
    result &= FunctionSignature.getSignatureBooleanStringDoubleInt2Int(helperBooleanStringDoubleInt2Int) == "Boolean:String:Double:Int2Int"
    result &= FunctionSignature.getSignatureBooleanStringDoubleLong2Int(helperBooleanStringDoubleLong2Int) == "Boolean:String:Double:Long2Int"
    result &= FunctionSignature.getSignatureBooleanStringDoubleDouble2Int(helperBooleanStringDoubleDouble2Int) == "Boolean:String:Double:Double2Int"
    result &= FunctionSignature.getSignatureBooleanStringDoubleString2Int(helperBooleanStringDoubleString2Int) == "Boolean:String:Double:String2Int"
    result &= FunctionSignature.getSignatureBooleanStringDoubleBoolean2Int(helperBooleanStringDoubleBoolean2Int) == "Boolean:String:Double:Boolean2Int"
    result &= FunctionSignature.getSignatureBooleanStringStringInt2Int(helperBooleanStringStringInt2Int) == "Boolean:String:String:Int2Int"
    result &= FunctionSignature.getSignatureBooleanStringStringLong2Int(helperBooleanStringStringLong2Int) == "Boolean:String:String:Long2Int"
    result &= FunctionSignature.getSignatureBooleanStringStringDouble2Int(helperBooleanStringStringDouble2Int) == "Boolean:String:String:Double2Int"
    result &= FunctionSignature.getSignatureBooleanStringStringString2Int(helperBooleanStringStringString2Int) == "Boolean:String:String:String2Int"
    result &= FunctionSignature.getSignatureBooleanStringStringBoolean2Int(helperBooleanStringStringBoolean2Int) == "Boolean:String:String:Boolean2Int"
    result &= FunctionSignature.getSignatureBooleanStringBooleanInt2Int(helperBooleanStringBooleanInt2Int) == "Boolean:String:Boolean:Int2Int"
    result &= FunctionSignature.getSignatureBooleanStringBooleanLong2Int(helperBooleanStringBooleanLong2Int) == "Boolean:String:Boolean:Long2Int"
    result &= FunctionSignature.getSignatureBooleanStringBooleanDouble2Int(helperBooleanStringBooleanDouble2Int) == "Boolean:String:Boolean:Double2Int"
    result &= FunctionSignature.getSignatureBooleanStringBooleanString2Int(helperBooleanStringBooleanString2Int) == "Boolean:String:Boolean:String2Int"
    result &= FunctionSignature.getSignatureBooleanStringBooleanBoolean2Int(helperBooleanStringBooleanBoolean2Int) == "Boolean:String:Boolean:Boolean2Int"
    result &= FunctionSignature.getSignatureBooleanBooleanIntInt2Int(helperBooleanBooleanIntInt2Int) == "Boolean:Boolean:Int:Int2Int"
    result &= FunctionSignature.getSignatureBooleanBooleanIntLong2Int(helperBooleanBooleanIntLong2Int) == "Boolean:Boolean:Int:Long2Int"
    result &= FunctionSignature.getSignatureBooleanBooleanIntDouble2Int(helperBooleanBooleanIntDouble2Int) == "Boolean:Boolean:Int:Double2Int"
    result &= FunctionSignature.getSignatureBooleanBooleanIntString2Int(helperBooleanBooleanIntString2Int) == "Boolean:Boolean:Int:String2Int"
    result &= FunctionSignature.getSignatureBooleanBooleanIntBoolean2Int(helperBooleanBooleanIntBoolean2Int) == "Boolean:Boolean:Int:Boolean2Int"
    result &= FunctionSignature.getSignatureBooleanBooleanLongInt2Int(helperBooleanBooleanLongInt2Int) == "Boolean:Boolean:Long:Int2Int"
    result &= FunctionSignature.getSignatureBooleanBooleanLongLong2Int(helperBooleanBooleanLongLong2Int) == "Boolean:Boolean:Long:Long2Int"
    result &= FunctionSignature.getSignatureBooleanBooleanLongDouble2Int(helperBooleanBooleanLongDouble2Int) == "Boolean:Boolean:Long:Double2Int"
    result &= FunctionSignature.getSignatureBooleanBooleanLongString2Int(helperBooleanBooleanLongString2Int) == "Boolean:Boolean:Long:String2Int"
    result &= FunctionSignature.getSignatureBooleanBooleanLongBoolean2Int(helperBooleanBooleanLongBoolean2Int) == "Boolean:Boolean:Long:Boolean2Int"
    result &= FunctionSignature.getSignatureBooleanBooleanDoubleInt2Int(helperBooleanBooleanDoubleInt2Int) == "Boolean:Boolean:Double:Int2Int"
    result &= FunctionSignature.getSignatureBooleanBooleanDoubleLong2Int(helperBooleanBooleanDoubleLong2Int) == "Boolean:Boolean:Double:Long2Int"
    result &= FunctionSignature.getSignatureBooleanBooleanDoubleDouble2Int(helperBooleanBooleanDoubleDouble2Int) == "Boolean:Boolean:Double:Double2Int"
    result &= FunctionSignature.getSignatureBooleanBooleanDoubleString2Int(helperBooleanBooleanDoubleString2Int) == "Boolean:Boolean:Double:String2Int"
    result &= FunctionSignature.getSignatureBooleanBooleanDoubleBoolean2Int(helperBooleanBooleanDoubleBoolean2Int) == "Boolean:Boolean:Double:Boolean2Int"
    result &= FunctionSignature.getSignatureBooleanBooleanStringInt2Int(helperBooleanBooleanStringInt2Int) == "Boolean:Boolean:String:Int2Int"
    result &= FunctionSignature.getSignatureBooleanBooleanStringLong2Int(helperBooleanBooleanStringLong2Int) == "Boolean:Boolean:String:Long2Int"
    result &= FunctionSignature.getSignatureBooleanBooleanStringDouble2Int(helperBooleanBooleanStringDouble2Int) == "Boolean:Boolean:String:Double2Int"
    result &= FunctionSignature.getSignatureBooleanBooleanStringString2Int(helperBooleanBooleanStringString2Int) == "Boolean:Boolean:String:String2Int"
    result &= FunctionSignature.getSignatureBooleanBooleanStringBoolean2Int(helperBooleanBooleanStringBoolean2Int) == "Boolean:Boolean:String:Boolean2Int"
    result &= FunctionSignature.getSignatureBooleanBooleanBooleanInt2Int(helperBooleanBooleanBooleanInt2Int) == "Boolean:Boolean:Boolean:Int2Int"
    result &= FunctionSignature.getSignatureBooleanBooleanBooleanLong2Int(helperBooleanBooleanBooleanLong2Int) == "Boolean:Boolean:Boolean:Long2Int"
    result &= FunctionSignature.getSignatureBooleanBooleanBooleanDouble2Int(helperBooleanBooleanBooleanDouble2Int) == "Boolean:Boolean:Boolean:Double2Int"
    result &= FunctionSignature.getSignatureBooleanBooleanBooleanString2Int(helperBooleanBooleanBooleanString2Int) == "Boolean:Boolean:Boolean:String2Int"
    result &= FunctionSignature.getSignatureBooleanBooleanBooleanBoolean2Int(helperBooleanBooleanBooleanBoolean2Int) == "Boolean:Boolean:Boolean:Boolean2Int"
    assert(result)
  }
  def helperIntIntIntInt2Long(a1: iFun, a2: iFun, a3: iFun, a4: iFun): lFun = lFunDummy
  def helperIntIntIntLong2Long(a1: iFun, a2: iFun, a3: iFun, a4: lFun): lFun = lFunDummy
  def helperIntIntIntDouble2Long(a1: iFun, a2: iFun, a3: iFun, a4: dFun): lFun = lFunDummy
  def helperIntIntIntString2Long(a1: iFun, a2: iFun, a3: iFun, a4: sFun): lFun = lFunDummy
  def helperIntIntIntBoolean2Long(a1: iFun, a2: iFun, a3: iFun, a4: bFun): lFun = lFunDummy
  def helperIntIntLongInt2Long(a1: iFun, a2: iFun, a3: lFun, a4: iFun): lFun = lFunDummy
  def helperIntIntLongLong2Long(a1: iFun, a2: iFun, a3: lFun, a4: lFun): lFun = lFunDummy
  def helperIntIntLongDouble2Long(a1: iFun, a2: iFun, a3: lFun, a4: dFun): lFun = lFunDummy
  def helperIntIntLongString2Long(a1: iFun, a2: iFun, a3: lFun, a4: sFun): lFun = lFunDummy
  def helperIntIntLongBoolean2Long(a1: iFun, a2: iFun, a3: lFun, a4: bFun): lFun = lFunDummy
  def helperIntIntDoubleInt2Long(a1: iFun, a2: iFun, a3: dFun, a4: iFun): lFun = lFunDummy
  def helperIntIntDoubleLong2Long(a1: iFun, a2: iFun, a3: dFun, a4: lFun): lFun = lFunDummy
  def helperIntIntDoubleDouble2Long(a1: iFun, a2: iFun, a3: dFun, a4: dFun): lFun = lFunDummy
  def helperIntIntDoubleString2Long(a1: iFun, a2: iFun, a3: dFun, a4: sFun): lFun = lFunDummy
  def helperIntIntDoubleBoolean2Long(a1: iFun, a2: iFun, a3: dFun, a4: bFun): lFun = lFunDummy
  def helperIntIntStringInt2Long(a1: iFun, a2: iFun, a3: sFun, a4: iFun): lFun = lFunDummy
  def helperIntIntStringLong2Long(a1: iFun, a2: iFun, a3: sFun, a4: lFun): lFun = lFunDummy
  def helperIntIntStringDouble2Long(a1: iFun, a2: iFun, a3: sFun, a4: dFun): lFun = lFunDummy
  def helperIntIntStringString2Long(a1: iFun, a2: iFun, a3: sFun, a4: sFun): lFun = lFunDummy
  def helperIntIntStringBoolean2Long(a1: iFun, a2: iFun, a3: sFun, a4: bFun): lFun = lFunDummy
  def helperIntIntBooleanInt2Long(a1: iFun, a2: iFun, a3: bFun, a4: iFun): lFun = lFunDummy
  def helperIntIntBooleanLong2Long(a1: iFun, a2: iFun, a3: bFun, a4: lFun): lFun = lFunDummy
  def helperIntIntBooleanDouble2Long(a1: iFun, a2: iFun, a3: bFun, a4: dFun): lFun = lFunDummy
  def helperIntIntBooleanString2Long(a1: iFun, a2: iFun, a3: bFun, a4: sFun): lFun = lFunDummy
  def helperIntIntBooleanBoolean2Long(a1: iFun, a2: iFun, a3: bFun, a4: bFun): lFun = lFunDummy
  def helperIntLongIntInt2Long(a1: iFun, a2: lFun, a3: iFun, a4: iFun): lFun = lFunDummy
  def helperIntLongIntLong2Long(a1: iFun, a2: lFun, a3: iFun, a4: lFun): lFun = lFunDummy
  def helperIntLongIntDouble2Long(a1: iFun, a2: lFun, a3: iFun, a4: dFun): lFun = lFunDummy
  def helperIntLongIntString2Long(a1: iFun, a2: lFun, a3: iFun, a4: sFun): lFun = lFunDummy
  def helperIntLongIntBoolean2Long(a1: iFun, a2: lFun, a3: iFun, a4: bFun): lFun = lFunDummy
  def helperIntLongLongInt2Long(a1: iFun, a2: lFun, a3: lFun, a4: iFun): lFun = lFunDummy
  def helperIntLongLongLong2Long(a1: iFun, a2: lFun, a3: lFun, a4: lFun): lFun = lFunDummy
  def helperIntLongLongDouble2Long(a1: iFun, a2: lFun, a3: lFun, a4: dFun): lFun = lFunDummy
  def helperIntLongLongString2Long(a1: iFun, a2: lFun, a3: lFun, a4: sFun): lFun = lFunDummy
  def helperIntLongLongBoolean2Long(a1: iFun, a2: lFun, a3: lFun, a4: bFun): lFun = lFunDummy
  def helperIntLongDoubleInt2Long(a1: iFun, a2: lFun, a3: dFun, a4: iFun): lFun = lFunDummy
  def helperIntLongDoubleLong2Long(a1: iFun, a2: lFun, a3: dFun, a4: lFun): lFun = lFunDummy
  def helperIntLongDoubleDouble2Long(a1: iFun, a2: lFun, a3: dFun, a4: dFun): lFun = lFunDummy
  def helperIntLongDoubleString2Long(a1: iFun, a2: lFun, a3: dFun, a4: sFun): lFun = lFunDummy
  def helperIntLongDoubleBoolean2Long(a1: iFun, a2: lFun, a3: dFun, a4: bFun): lFun = lFunDummy
  def helperIntLongStringInt2Long(a1: iFun, a2: lFun, a3: sFun, a4: iFun): lFun = lFunDummy
  def helperIntLongStringLong2Long(a1: iFun, a2: lFun, a3: sFun, a4: lFun): lFun = lFunDummy
  def helperIntLongStringDouble2Long(a1: iFun, a2: lFun, a3: sFun, a4: dFun): lFun = lFunDummy
  def helperIntLongStringString2Long(a1: iFun, a2: lFun, a3: sFun, a4: sFun): lFun = lFunDummy
  def helperIntLongStringBoolean2Long(a1: iFun, a2: lFun, a3: sFun, a4: bFun): lFun = lFunDummy
  def helperIntLongBooleanInt2Long(a1: iFun, a2: lFun, a3: bFun, a4: iFun): lFun = lFunDummy
  def helperIntLongBooleanLong2Long(a1: iFun, a2: lFun, a3: bFun, a4: lFun): lFun = lFunDummy
  def helperIntLongBooleanDouble2Long(a1: iFun, a2: lFun, a3: bFun, a4: dFun): lFun = lFunDummy
  def helperIntLongBooleanString2Long(a1: iFun, a2: lFun, a3: bFun, a4: sFun): lFun = lFunDummy
  def helperIntLongBooleanBoolean2Long(a1: iFun, a2: lFun, a3: bFun, a4: bFun): lFun = lFunDummy
  def helperIntDoubleIntInt2Long(a1: iFun, a2: dFun, a3: iFun, a4: iFun): lFun = lFunDummy
  def helperIntDoubleIntLong2Long(a1: iFun, a2: dFun, a3: iFun, a4: lFun): lFun = lFunDummy
  def helperIntDoubleIntDouble2Long(a1: iFun, a2: dFun, a3: iFun, a4: dFun): lFun = lFunDummy
  def helperIntDoubleIntString2Long(a1: iFun, a2: dFun, a3: iFun, a4: sFun): lFun = lFunDummy
  def helperIntDoubleIntBoolean2Long(a1: iFun, a2: dFun, a3: iFun, a4: bFun): lFun = lFunDummy
  def helperIntDoubleLongInt2Long(a1: iFun, a2: dFun, a3: lFun, a4: iFun): lFun = lFunDummy
  def helperIntDoubleLongLong2Long(a1: iFun, a2: dFun, a3: lFun, a4: lFun): lFun = lFunDummy
  def helperIntDoubleLongDouble2Long(a1: iFun, a2: dFun, a3: lFun, a4: dFun): lFun = lFunDummy
  def helperIntDoubleLongString2Long(a1: iFun, a2: dFun, a3: lFun, a4: sFun): lFun = lFunDummy
  def helperIntDoubleLongBoolean2Long(a1: iFun, a2: dFun, a3: lFun, a4: bFun): lFun = lFunDummy
  def helperIntDoubleDoubleInt2Long(a1: iFun, a2: dFun, a3: dFun, a4: iFun): lFun = lFunDummy
  def helperIntDoubleDoubleLong2Long(a1: iFun, a2: dFun, a3: dFun, a4: lFun): lFun = lFunDummy
  def helperIntDoubleDoubleDouble2Long(a1: iFun, a2: dFun, a3: dFun, a4: dFun): lFun = lFunDummy
  def helperIntDoubleDoubleString2Long(a1: iFun, a2: dFun, a3: dFun, a4: sFun): lFun = lFunDummy
  def helperIntDoubleDoubleBoolean2Long(a1: iFun, a2: dFun, a3: dFun, a4: bFun): lFun = lFunDummy
  def helperIntDoubleStringInt2Long(a1: iFun, a2: dFun, a3: sFun, a4: iFun): lFun = lFunDummy
  def helperIntDoubleStringLong2Long(a1: iFun, a2: dFun, a3: sFun, a4: lFun): lFun = lFunDummy
  def helperIntDoubleStringDouble2Long(a1: iFun, a2: dFun, a3: sFun, a4: dFun): lFun = lFunDummy
  def helperIntDoubleStringString2Long(a1: iFun, a2: dFun, a3: sFun, a4: sFun): lFun = lFunDummy
  def helperIntDoubleStringBoolean2Long(a1: iFun, a2: dFun, a3: sFun, a4: bFun): lFun = lFunDummy
  def helperIntDoubleBooleanInt2Long(a1: iFun, a2: dFun, a3: bFun, a4: iFun): lFun = lFunDummy
  def helperIntDoubleBooleanLong2Long(a1: iFun, a2: dFun, a3: bFun, a4: lFun): lFun = lFunDummy
  def helperIntDoubleBooleanDouble2Long(a1: iFun, a2: dFun, a3: bFun, a4: dFun): lFun = lFunDummy
  def helperIntDoubleBooleanString2Long(a1: iFun, a2: dFun, a3: bFun, a4: sFun): lFun = lFunDummy
  def helperIntDoubleBooleanBoolean2Long(a1: iFun, a2: dFun, a3: bFun, a4: bFun): lFun = lFunDummy
  def helperIntStringIntInt2Long(a1: iFun, a2: sFun, a3: iFun, a4: iFun): lFun = lFunDummy
  def helperIntStringIntLong2Long(a1: iFun, a2: sFun, a3: iFun, a4: lFun): lFun = lFunDummy
  def helperIntStringIntDouble2Long(a1: iFun, a2: sFun, a3: iFun, a4: dFun): lFun = lFunDummy
  def helperIntStringIntString2Long(a1: iFun, a2: sFun, a3: iFun, a4: sFun): lFun = lFunDummy
  def helperIntStringIntBoolean2Long(a1: iFun, a2: sFun, a3: iFun, a4: bFun): lFun = lFunDummy
  def helperIntStringLongInt2Long(a1: iFun, a2: sFun, a3: lFun, a4: iFun): lFun = lFunDummy
  def helperIntStringLongLong2Long(a1: iFun, a2: sFun, a3: lFun, a4: lFun): lFun = lFunDummy
  def helperIntStringLongDouble2Long(a1: iFun, a2: sFun, a3: lFun, a4: dFun): lFun = lFunDummy
  def helperIntStringLongString2Long(a1: iFun, a2: sFun, a3: lFun, a4: sFun): lFun = lFunDummy
  def helperIntStringLongBoolean2Long(a1: iFun, a2: sFun, a3: lFun, a4: bFun): lFun = lFunDummy
  def helperIntStringDoubleInt2Long(a1: iFun, a2: sFun, a3: dFun, a4: iFun): lFun = lFunDummy
  def helperIntStringDoubleLong2Long(a1: iFun, a2: sFun, a3: dFun, a4: lFun): lFun = lFunDummy
  def helperIntStringDoubleDouble2Long(a1: iFun, a2: sFun, a3: dFun, a4: dFun): lFun = lFunDummy
  def helperIntStringDoubleString2Long(a1: iFun, a2: sFun, a3: dFun, a4: sFun): lFun = lFunDummy
  def helperIntStringDoubleBoolean2Long(a1: iFun, a2: sFun, a3: dFun, a4: bFun): lFun = lFunDummy
  def helperIntStringStringInt2Long(a1: iFun, a2: sFun, a3: sFun, a4: iFun): lFun = lFunDummy
  def helperIntStringStringLong2Long(a1: iFun, a2: sFun, a3: sFun, a4: lFun): lFun = lFunDummy
  def helperIntStringStringDouble2Long(a1: iFun, a2: sFun, a3: sFun, a4: dFun): lFun = lFunDummy
  def helperIntStringStringString2Long(a1: iFun, a2: sFun, a3: sFun, a4: sFun): lFun = lFunDummy
  def helperIntStringStringBoolean2Long(a1: iFun, a2: sFun, a3: sFun, a4: bFun): lFun = lFunDummy
  def helperIntStringBooleanInt2Long(a1: iFun, a2: sFun, a3: bFun, a4: iFun): lFun = lFunDummy
  def helperIntStringBooleanLong2Long(a1: iFun, a2: sFun, a3: bFun, a4: lFun): lFun = lFunDummy
  def helperIntStringBooleanDouble2Long(a1: iFun, a2: sFun, a3: bFun, a4: dFun): lFun = lFunDummy
  def helperIntStringBooleanString2Long(a1: iFun, a2: sFun, a3: bFun, a4: sFun): lFun = lFunDummy
  def helperIntStringBooleanBoolean2Long(a1: iFun, a2: sFun, a3: bFun, a4: bFun): lFun = lFunDummy
  def helperIntBooleanIntInt2Long(a1: iFun, a2: bFun, a3: iFun, a4: iFun): lFun = lFunDummy
  def helperIntBooleanIntLong2Long(a1: iFun, a2: bFun, a3: iFun, a4: lFun): lFun = lFunDummy
  def helperIntBooleanIntDouble2Long(a1: iFun, a2: bFun, a3: iFun, a4: dFun): lFun = lFunDummy
  def helperIntBooleanIntString2Long(a1: iFun, a2: bFun, a3: iFun, a4: sFun): lFun = lFunDummy
  def helperIntBooleanIntBoolean2Long(a1: iFun, a2: bFun, a3: iFun, a4: bFun): lFun = lFunDummy
  def helperIntBooleanLongInt2Long(a1: iFun, a2: bFun, a3: lFun, a4: iFun): lFun = lFunDummy
  def helperIntBooleanLongLong2Long(a1: iFun, a2: bFun, a3: lFun, a4: lFun): lFun = lFunDummy
  def helperIntBooleanLongDouble2Long(a1: iFun, a2: bFun, a3: lFun, a4: dFun): lFun = lFunDummy
  def helperIntBooleanLongString2Long(a1: iFun, a2: bFun, a3: lFun, a4: sFun): lFun = lFunDummy
  def helperIntBooleanLongBoolean2Long(a1: iFun, a2: bFun, a3: lFun, a4: bFun): lFun = lFunDummy
  def helperIntBooleanDoubleInt2Long(a1: iFun, a2: bFun, a3: dFun, a4: iFun): lFun = lFunDummy
  def helperIntBooleanDoubleLong2Long(a1: iFun, a2: bFun, a3: dFun, a4: lFun): lFun = lFunDummy
  def helperIntBooleanDoubleDouble2Long(a1: iFun, a2: bFun, a3: dFun, a4: dFun): lFun = lFunDummy
  def helperIntBooleanDoubleString2Long(a1: iFun, a2: bFun, a3: dFun, a4: sFun): lFun = lFunDummy
  def helperIntBooleanDoubleBoolean2Long(a1: iFun, a2: bFun, a3: dFun, a4: bFun): lFun = lFunDummy
  def helperIntBooleanStringInt2Long(a1: iFun, a2: bFun, a3: sFun, a4: iFun): lFun = lFunDummy
  def helperIntBooleanStringLong2Long(a1: iFun, a2: bFun, a3: sFun, a4: lFun): lFun = lFunDummy
  def helperIntBooleanStringDouble2Long(a1: iFun, a2: bFun, a3: sFun, a4: dFun): lFun = lFunDummy
  def helperIntBooleanStringString2Long(a1: iFun, a2: bFun, a3: sFun, a4: sFun): lFun = lFunDummy
  def helperIntBooleanStringBoolean2Long(a1: iFun, a2: bFun, a3: sFun, a4: bFun): lFun = lFunDummy
  def helperIntBooleanBooleanInt2Long(a1: iFun, a2: bFun, a3: bFun, a4: iFun): lFun = lFunDummy
  def helperIntBooleanBooleanLong2Long(a1: iFun, a2: bFun, a3: bFun, a4: lFun): lFun = lFunDummy
  def helperIntBooleanBooleanDouble2Long(a1: iFun, a2: bFun, a3: bFun, a4: dFun): lFun = lFunDummy
  def helperIntBooleanBooleanString2Long(a1: iFun, a2: bFun, a3: bFun, a4: sFun): lFun = lFunDummy
  def helperIntBooleanBooleanBoolean2Long(a1: iFun, a2: bFun, a3: bFun, a4: bFun): lFun = lFunDummy
  def helperLongIntIntInt2Long(a1: lFun, a2: iFun, a3: iFun, a4: iFun): lFun = lFunDummy
  def helperLongIntIntLong2Long(a1: lFun, a2: iFun, a3: iFun, a4: lFun): lFun = lFunDummy
  def helperLongIntIntDouble2Long(a1: lFun, a2: iFun, a3: iFun, a4: dFun): lFun = lFunDummy
  def helperLongIntIntString2Long(a1: lFun, a2: iFun, a3: iFun, a4: sFun): lFun = lFunDummy
  def helperLongIntIntBoolean2Long(a1: lFun, a2: iFun, a3: iFun, a4: bFun): lFun = lFunDummy
  def helperLongIntLongInt2Long(a1: lFun, a2: iFun, a3: lFun, a4: iFun): lFun = lFunDummy
  def helperLongIntLongLong2Long(a1: lFun, a2: iFun, a3: lFun, a4: lFun): lFun = lFunDummy
  def helperLongIntLongDouble2Long(a1: lFun, a2: iFun, a3: lFun, a4: dFun): lFun = lFunDummy
  def helperLongIntLongString2Long(a1: lFun, a2: iFun, a3: lFun, a4: sFun): lFun = lFunDummy
  def helperLongIntLongBoolean2Long(a1: lFun, a2: iFun, a3: lFun, a4: bFun): lFun = lFunDummy
  def helperLongIntDoubleInt2Long(a1: lFun, a2: iFun, a3: dFun, a4: iFun): lFun = lFunDummy
  def helperLongIntDoubleLong2Long(a1: lFun, a2: iFun, a3: dFun, a4: lFun): lFun = lFunDummy
  def helperLongIntDoubleDouble2Long(a1: lFun, a2: iFun, a3: dFun, a4: dFun): lFun = lFunDummy
  def helperLongIntDoubleString2Long(a1: lFun, a2: iFun, a3: dFun, a4: sFun): lFun = lFunDummy
  def helperLongIntDoubleBoolean2Long(a1: lFun, a2: iFun, a3: dFun, a4: bFun): lFun = lFunDummy
  def helperLongIntStringInt2Long(a1: lFun, a2: iFun, a3: sFun, a4: iFun): lFun = lFunDummy
  def helperLongIntStringLong2Long(a1: lFun, a2: iFun, a3: sFun, a4: lFun): lFun = lFunDummy
  def helperLongIntStringDouble2Long(a1: lFun, a2: iFun, a3: sFun, a4: dFun): lFun = lFunDummy
  def helperLongIntStringString2Long(a1: lFun, a2: iFun, a3: sFun, a4: sFun): lFun = lFunDummy
  def helperLongIntStringBoolean2Long(a1: lFun, a2: iFun, a3: sFun, a4: bFun): lFun = lFunDummy
  def helperLongIntBooleanInt2Long(a1: lFun, a2: iFun, a3: bFun, a4: iFun): lFun = lFunDummy
  def helperLongIntBooleanLong2Long(a1: lFun, a2: iFun, a3: bFun, a4: lFun): lFun = lFunDummy
  def helperLongIntBooleanDouble2Long(a1: lFun, a2: iFun, a3: bFun, a4: dFun): lFun = lFunDummy
  def helperLongIntBooleanString2Long(a1: lFun, a2: iFun, a3: bFun, a4: sFun): lFun = lFunDummy
  def helperLongIntBooleanBoolean2Long(a1: lFun, a2: iFun, a3: bFun, a4: bFun): lFun = lFunDummy
  def helperLongLongIntInt2Long(a1: lFun, a2: lFun, a3: iFun, a4: iFun): lFun = lFunDummy
  def helperLongLongIntLong2Long(a1: lFun, a2: lFun, a3: iFun, a4: lFun): lFun = lFunDummy
  def helperLongLongIntDouble2Long(a1: lFun, a2: lFun, a3: iFun, a4: dFun): lFun = lFunDummy
  def helperLongLongIntString2Long(a1: lFun, a2: lFun, a3: iFun, a4: sFun): lFun = lFunDummy
  def helperLongLongIntBoolean2Long(a1: lFun, a2: lFun, a3: iFun, a4: bFun): lFun = lFunDummy
  def helperLongLongLongInt2Long(a1: lFun, a2: lFun, a3: lFun, a4: iFun): lFun = lFunDummy
  def helperLongLongLongLong2Long(a1: lFun, a2: lFun, a3: lFun, a4: lFun): lFun = lFunDummy
  def helperLongLongLongDouble2Long(a1: lFun, a2: lFun, a3: lFun, a4: dFun): lFun = lFunDummy
  def helperLongLongLongString2Long(a1: lFun, a2: lFun, a3: lFun, a4: sFun): lFun = lFunDummy
  def helperLongLongLongBoolean2Long(a1: lFun, a2: lFun, a3: lFun, a4: bFun): lFun = lFunDummy
  def helperLongLongDoubleInt2Long(a1: lFun, a2: lFun, a3: dFun, a4: iFun): lFun = lFunDummy
  def helperLongLongDoubleLong2Long(a1: lFun, a2: lFun, a3: dFun, a4: lFun): lFun = lFunDummy
  def helperLongLongDoubleDouble2Long(a1: lFun, a2: lFun, a3: dFun, a4: dFun): lFun = lFunDummy
  def helperLongLongDoubleString2Long(a1: lFun, a2: lFun, a3: dFun, a4: sFun): lFun = lFunDummy
  def helperLongLongDoubleBoolean2Long(a1: lFun, a2: lFun, a3: dFun, a4: bFun): lFun = lFunDummy
  def helperLongLongStringInt2Long(a1: lFun, a2: lFun, a3: sFun, a4: iFun): lFun = lFunDummy
  def helperLongLongStringLong2Long(a1: lFun, a2: lFun, a3: sFun, a4: lFun): lFun = lFunDummy
  def helperLongLongStringDouble2Long(a1: lFun, a2: lFun, a3: sFun, a4: dFun): lFun = lFunDummy
  def helperLongLongStringString2Long(a1: lFun, a2: lFun, a3: sFun, a4: sFun): lFun = lFunDummy
  def helperLongLongStringBoolean2Long(a1: lFun, a2: lFun, a3: sFun, a4: bFun): lFun = lFunDummy
  def helperLongLongBooleanInt2Long(a1: lFun, a2: lFun, a3: bFun, a4: iFun): lFun = lFunDummy
  def helperLongLongBooleanLong2Long(a1: lFun, a2: lFun, a3: bFun, a4: lFun): lFun = lFunDummy
  def helperLongLongBooleanDouble2Long(a1: lFun, a2: lFun, a3: bFun, a4: dFun): lFun = lFunDummy
  def helperLongLongBooleanString2Long(a1: lFun, a2: lFun, a3: bFun, a4: sFun): lFun = lFunDummy
  def helperLongLongBooleanBoolean2Long(a1: lFun, a2: lFun, a3: bFun, a4: bFun): lFun = lFunDummy
  def helperLongDoubleIntInt2Long(a1: lFun, a2: dFun, a3: iFun, a4: iFun): lFun = lFunDummy
  def helperLongDoubleIntLong2Long(a1: lFun, a2: dFun, a3: iFun, a4: lFun): lFun = lFunDummy
  def helperLongDoubleIntDouble2Long(a1: lFun, a2: dFun, a3: iFun, a4: dFun): lFun = lFunDummy
  def helperLongDoubleIntString2Long(a1: lFun, a2: dFun, a3: iFun, a4: sFun): lFun = lFunDummy
  def helperLongDoubleIntBoolean2Long(a1: lFun, a2: dFun, a3: iFun, a4: bFun): lFun = lFunDummy
  def helperLongDoubleLongInt2Long(a1: lFun, a2: dFun, a3: lFun, a4: iFun): lFun = lFunDummy
  def helperLongDoubleLongLong2Long(a1: lFun, a2: dFun, a3: lFun, a4: lFun): lFun = lFunDummy
  def helperLongDoubleLongDouble2Long(a1: lFun, a2: dFun, a3: lFun, a4: dFun): lFun = lFunDummy
  def helperLongDoubleLongString2Long(a1: lFun, a2: dFun, a3: lFun, a4: sFun): lFun = lFunDummy
  def helperLongDoubleLongBoolean2Long(a1: lFun, a2: dFun, a3: lFun, a4: bFun): lFun = lFunDummy
  def helperLongDoubleDoubleInt2Long(a1: lFun, a2: dFun, a3: dFun, a4: iFun): lFun = lFunDummy
  def helperLongDoubleDoubleLong2Long(a1: lFun, a2: dFun, a3: dFun, a4: lFun): lFun = lFunDummy
  def helperLongDoubleDoubleDouble2Long(a1: lFun, a2: dFun, a3: dFun, a4: dFun): lFun = lFunDummy
  def helperLongDoubleDoubleString2Long(a1: lFun, a2: dFun, a3: dFun, a4: sFun): lFun = lFunDummy
  def helperLongDoubleDoubleBoolean2Long(a1: lFun, a2: dFun, a3: dFun, a4: bFun): lFun = lFunDummy
  def helperLongDoubleStringInt2Long(a1: lFun, a2: dFun, a3: sFun, a4: iFun): lFun = lFunDummy
  def helperLongDoubleStringLong2Long(a1: lFun, a2: dFun, a3: sFun, a4: lFun): lFun = lFunDummy
  def helperLongDoubleStringDouble2Long(a1: lFun, a2: dFun, a3: sFun, a4: dFun): lFun = lFunDummy
  def helperLongDoubleStringString2Long(a1: lFun, a2: dFun, a3: sFun, a4: sFun): lFun = lFunDummy
  def helperLongDoubleStringBoolean2Long(a1: lFun, a2: dFun, a3: sFun, a4: bFun): lFun = lFunDummy
  def helperLongDoubleBooleanInt2Long(a1: lFun, a2: dFun, a3: bFun, a4: iFun): lFun = lFunDummy
  def helperLongDoubleBooleanLong2Long(a1: lFun, a2: dFun, a3: bFun, a4: lFun): lFun = lFunDummy
  def helperLongDoubleBooleanDouble2Long(a1: lFun, a2: dFun, a3: bFun, a4: dFun): lFun = lFunDummy
  def helperLongDoubleBooleanString2Long(a1: lFun, a2: dFun, a3: bFun, a4: sFun): lFun = lFunDummy
  def helperLongDoubleBooleanBoolean2Long(a1: lFun, a2: dFun, a3: bFun, a4: bFun): lFun = lFunDummy
  def helperLongStringIntInt2Long(a1: lFun, a2: sFun, a3: iFun, a4: iFun): lFun = lFunDummy
  def helperLongStringIntLong2Long(a1: lFun, a2: sFun, a3: iFun, a4: lFun): lFun = lFunDummy
  def helperLongStringIntDouble2Long(a1: lFun, a2: sFun, a3: iFun, a4: dFun): lFun = lFunDummy
  def helperLongStringIntString2Long(a1: lFun, a2: sFun, a3: iFun, a4: sFun): lFun = lFunDummy
  def helperLongStringIntBoolean2Long(a1: lFun, a2: sFun, a3: iFun, a4: bFun): lFun = lFunDummy
  def helperLongStringLongInt2Long(a1: lFun, a2: sFun, a3: lFun, a4: iFun): lFun = lFunDummy
  def helperLongStringLongLong2Long(a1: lFun, a2: sFun, a3: lFun, a4: lFun): lFun = lFunDummy
  def helperLongStringLongDouble2Long(a1: lFun, a2: sFun, a3: lFun, a4: dFun): lFun = lFunDummy
  def helperLongStringLongString2Long(a1: lFun, a2: sFun, a3: lFun, a4: sFun): lFun = lFunDummy
  def helperLongStringLongBoolean2Long(a1: lFun, a2: sFun, a3: lFun, a4: bFun): lFun = lFunDummy
  def helperLongStringDoubleInt2Long(a1: lFun, a2: sFun, a3: dFun, a4: iFun): lFun = lFunDummy
  def helperLongStringDoubleLong2Long(a1: lFun, a2: sFun, a3: dFun, a4: lFun): lFun = lFunDummy
  def helperLongStringDoubleDouble2Long(a1: lFun, a2: sFun, a3: dFun, a4: dFun): lFun = lFunDummy
  def helperLongStringDoubleString2Long(a1: lFun, a2: sFun, a3: dFun, a4: sFun): lFun = lFunDummy
  def helperLongStringDoubleBoolean2Long(a1: lFun, a2: sFun, a3: dFun, a4: bFun): lFun = lFunDummy
  def helperLongStringStringInt2Long(a1: lFun, a2: sFun, a3: sFun, a4: iFun): lFun = lFunDummy
  def helperLongStringStringLong2Long(a1: lFun, a2: sFun, a3: sFun, a4: lFun): lFun = lFunDummy
  def helperLongStringStringDouble2Long(a1: lFun, a2: sFun, a3: sFun, a4: dFun): lFun = lFunDummy
  def helperLongStringStringString2Long(a1: lFun, a2: sFun, a3: sFun, a4: sFun): lFun = lFunDummy
  def helperLongStringStringBoolean2Long(a1: lFun, a2: sFun, a3: sFun, a4: bFun): lFun = lFunDummy
  def helperLongStringBooleanInt2Long(a1: lFun, a2: sFun, a3: bFun, a4: iFun): lFun = lFunDummy
  def helperLongStringBooleanLong2Long(a1: lFun, a2: sFun, a3: bFun, a4: lFun): lFun = lFunDummy
  def helperLongStringBooleanDouble2Long(a1: lFun, a2: sFun, a3: bFun, a4: dFun): lFun = lFunDummy
  def helperLongStringBooleanString2Long(a1: lFun, a2: sFun, a3: bFun, a4: sFun): lFun = lFunDummy
  def helperLongStringBooleanBoolean2Long(a1: lFun, a2: sFun, a3: bFun, a4: bFun): lFun = lFunDummy
  def helperLongBooleanIntInt2Long(a1: lFun, a2: bFun, a3: iFun, a4: iFun): lFun = lFunDummy
  def helperLongBooleanIntLong2Long(a1: lFun, a2: bFun, a3: iFun, a4: lFun): lFun = lFunDummy
  def helperLongBooleanIntDouble2Long(a1: lFun, a2: bFun, a3: iFun, a4: dFun): lFun = lFunDummy
  def helperLongBooleanIntString2Long(a1: lFun, a2: bFun, a3: iFun, a4: sFun): lFun = lFunDummy
  def helperLongBooleanIntBoolean2Long(a1: lFun, a2: bFun, a3: iFun, a4: bFun): lFun = lFunDummy
  def helperLongBooleanLongInt2Long(a1: lFun, a2: bFun, a3: lFun, a4: iFun): lFun = lFunDummy
  def helperLongBooleanLongLong2Long(a1: lFun, a2: bFun, a3: lFun, a4: lFun): lFun = lFunDummy
  def helperLongBooleanLongDouble2Long(a1: lFun, a2: bFun, a3: lFun, a4: dFun): lFun = lFunDummy
  def helperLongBooleanLongString2Long(a1: lFun, a2: bFun, a3: lFun, a4: sFun): lFun = lFunDummy
  def helperLongBooleanLongBoolean2Long(a1: lFun, a2: bFun, a3: lFun, a4: bFun): lFun = lFunDummy
  def helperLongBooleanDoubleInt2Long(a1: lFun, a2: bFun, a3: dFun, a4: iFun): lFun = lFunDummy
  def helperLongBooleanDoubleLong2Long(a1: lFun, a2: bFun, a3: dFun, a4: lFun): lFun = lFunDummy
  def helperLongBooleanDoubleDouble2Long(a1: lFun, a2: bFun, a3: dFun, a4: dFun): lFun = lFunDummy
  def helperLongBooleanDoubleString2Long(a1: lFun, a2: bFun, a3: dFun, a4: sFun): lFun = lFunDummy
  def helperLongBooleanDoubleBoolean2Long(a1: lFun, a2: bFun, a3: dFun, a4: bFun): lFun = lFunDummy
  def helperLongBooleanStringInt2Long(a1: lFun, a2: bFun, a3: sFun, a4: iFun): lFun = lFunDummy
  def helperLongBooleanStringLong2Long(a1: lFun, a2: bFun, a3: sFun, a4: lFun): lFun = lFunDummy
  def helperLongBooleanStringDouble2Long(a1: lFun, a2: bFun, a3: sFun, a4: dFun): lFun = lFunDummy
  def helperLongBooleanStringString2Long(a1: lFun, a2: bFun, a3: sFun, a4: sFun): lFun = lFunDummy
  def helperLongBooleanStringBoolean2Long(a1: lFun, a2: bFun, a3: sFun, a4: bFun): lFun = lFunDummy
  def helperLongBooleanBooleanInt2Long(a1: lFun, a2: bFun, a3: bFun, a4: iFun): lFun = lFunDummy
  def helperLongBooleanBooleanLong2Long(a1: lFun, a2: bFun, a3: bFun, a4: lFun): lFun = lFunDummy
  def helperLongBooleanBooleanDouble2Long(a1: lFun, a2: bFun, a3: bFun, a4: dFun): lFun = lFunDummy
  def helperLongBooleanBooleanString2Long(a1: lFun, a2: bFun, a3: bFun, a4: sFun): lFun = lFunDummy
  def helperLongBooleanBooleanBoolean2Long(a1: lFun, a2: bFun, a3: bFun, a4: bFun): lFun = lFunDummy
  def helperDoubleIntIntInt2Long(a1: dFun, a2: iFun, a3: iFun, a4: iFun): lFun = lFunDummy
  def helperDoubleIntIntLong2Long(a1: dFun, a2: iFun, a3: iFun, a4: lFun): lFun = lFunDummy
  def helperDoubleIntIntDouble2Long(a1: dFun, a2: iFun, a3: iFun, a4: dFun): lFun = lFunDummy
  def helperDoubleIntIntString2Long(a1: dFun, a2: iFun, a3: iFun, a4: sFun): lFun = lFunDummy
  def helperDoubleIntIntBoolean2Long(a1: dFun, a2: iFun, a3: iFun, a4: bFun): lFun = lFunDummy
  def helperDoubleIntLongInt2Long(a1: dFun, a2: iFun, a3: lFun, a4: iFun): lFun = lFunDummy
  def helperDoubleIntLongLong2Long(a1: dFun, a2: iFun, a3: lFun, a4: lFun): lFun = lFunDummy
  def helperDoubleIntLongDouble2Long(a1: dFun, a2: iFun, a3: lFun, a4: dFun): lFun = lFunDummy
  def helperDoubleIntLongString2Long(a1: dFun, a2: iFun, a3: lFun, a4: sFun): lFun = lFunDummy
  def helperDoubleIntLongBoolean2Long(a1: dFun, a2: iFun, a3: lFun, a4: bFun): lFun = lFunDummy
  def helperDoubleIntDoubleInt2Long(a1: dFun, a2: iFun, a3: dFun, a4: iFun): lFun = lFunDummy
  def helperDoubleIntDoubleLong2Long(a1: dFun, a2: iFun, a3: dFun, a4: lFun): lFun = lFunDummy
  def helperDoubleIntDoubleDouble2Long(a1: dFun, a2: iFun, a3: dFun, a4: dFun): lFun = lFunDummy
  def helperDoubleIntDoubleString2Long(a1: dFun, a2: iFun, a3: dFun, a4: sFun): lFun = lFunDummy
  def helperDoubleIntDoubleBoolean2Long(a1: dFun, a2: iFun, a3: dFun, a4: bFun): lFun = lFunDummy
  def helperDoubleIntStringInt2Long(a1: dFun, a2: iFun, a3: sFun, a4: iFun): lFun = lFunDummy
  def helperDoubleIntStringLong2Long(a1: dFun, a2: iFun, a3: sFun, a4: lFun): lFun = lFunDummy
  def helperDoubleIntStringDouble2Long(a1: dFun, a2: iFun, a3: sFun, a4: dFun): lFun = lFunDummy
  def helperDoubleIntStringString2Long(a1: dFun, a2: iFun, a3: sFun, a4: sFun): lFun = lFunDummy
  def helperDoubleIntStringBoolean2Long(a1: dFun, a2: iFun, a3: sFun, a4: bFun): lFun = lFunDummy
  def helperDoubleIntBooleanInt2Long(a1: dFun, a2: iFun, a3: bFun, a4: iFun): lFun = lFunDummy
  def helperDoubleIntBooleanLong2Long(a1: dFun, a2: iFun, a3: bFun, a4: lFun): lFun = lFunDummy
  def helperDoubleIntBooleanDouble2Long(a1: dFun, a2: iFun, a3: bFun, a4: dFun): lFun = lFunDummy
  def helperDoubleIntBooleanString2Long(a1: dFun, a2: iFun, a3: bFun, a4: sFun): lFun = lFunDummy
  def helperDoubleIntBooleanBoolean2Long(a1: dFun, a2: iFun, a3: bFun, a4: bFun): lFun = lFunDummy
  def helperDoubleLongIntInt2Long(a1: dFun, a2: lFun, a3: iFun, a4: iFun): lFun = lFunDummy
  def helperDoubleLongIntLong2Long(a1: dFun, a2: lFun, a3: iFun, a4: lFun): lFun = lFunDummy
  def helperDoubleLongIntDouble2Long(a1: dFun, a2: lFun, a3: iFun, a4: dFun): lFun = lFunDummy
  def helperDoubleLongIntString2Long(a1: dFun, a2: lFun, a3: iFun, a4: sFun): lFun = lFunDummy
  def helperDoubleLongIntBoolean2Long(a1: dFun, a2: lFun, a3: iFun, a4: bFun): lFun = lFunDummy
  def helperDoubleLongLongInt2Long(a1: dFun, a2: lFun, a3: lFun, a4: iFun): lFun = lFunDummy
  def helperDoubleLongLongLong2Long(a1: dFun, a2: lFun, a3: lFun, a4: lFun): lFun = lFunDummy
  def helperDoubleLongLongDouble2Long(a1: dFun, a2: lFun, a3: lFun, a4: dFun): lFun = lFunDummy
  def helperDoubleLongLongString2Long(a1: dFun, a2: lFun, a3: lFun, a4: sFun): lFun = lFunDummy
  def helperDoubleLongLongBoolean2Long(a1: dFun, a2: lFun, a3: lFun, a4: bFun): lFun = lFunDummy
  def helperDoubleLongDoubleInt2Long(a1: dFun, a2: lFun, a3: dFun, a4: iFun): lFun = lFunDummy
  def helperDoubleLongDoubleLong2Long(a1: dFun, a2: lFun, a3: dFun, a4: lFun): lFun = lFunDummy
  def helperDoubleLongDoubleDouble2Long(a1: dFun, a2: lFun, a3: dFun, a4: dFun): lFun = lFunDummy
  def helperDoubleLongDoubleString2Long(a1: dFun, a2: lFun, a3: dFun, a4: sFun): lFun = lFunDummy
  def helperDoubleLongDoubleBoolean2Long(a1: dFun, a2: lFun, a3: dFun, a4: bFun): lFun = lFunDummy
  def helperDoubleLongStringInt2Long(a1: dFun, a2: lFun, a3: sFun, a4: iFun): lFun = lFunDummy
  def helperDoubleLongStringLong2Long(a1: dFun, a2: lFun, a3: sFun, a4: lFun): lFun = lFunDummy
  def helperDoubleLongStringDouble2Long(a1: dFun, a2: lFun, a3: sFun, a4: dFun): lFun = lFunDummy
  def helperDoubleLongStringString2Long(a1: dFun, a2: lFun, a3: sFun, a4: sFun): lFun = lFunDummy
  def helperDoubleLongStringBoolean2Long(a1: dFun, a2: lFun, a3: sFun, a4: bFun): lFun = lFunDummy
  def helperDoubleLongBooleanInt2Long(a1: dFun, a2: lFun, a3: bFun, a4: iFun): lFun = lFunDummy
  def helperDoubleLongBooleanLong2Long(a1: dFun, a2: lFun, a3: bFun, a4: lFun): lFun = lFunDummy
  def helperDoubleLongBooleanDouble2Long(a1: dFun, a2: lFun, a3: bFun, a4: dFun): lFun = lFunDummy
  def helperDoubleLongBooleanString2Long(a1: dFun, a2: lFun, a3: bFun, a4: sFun): lFun = lFunDummy
  def helperDoubleLongBooleanBoolean2Long(a1: dFun, a2: lFun, a3: bFun, a4: bFun): lFun = lFunDummy
  def helperDoubleDoubleIntInt2Long(a1: dFun, a2: dFun, a3: iFun, a4: iFun): lFun = lFunDummy
  def helperDoubleDoubleIntLong2Long(a1: dFun, a2: dFun, a3: iFun, a4: lFun): lFun = lFunDummy
  def helperDoubleDoubleIntDouble2Long(a1: dFun, a2: dFun, a3: iFun, a4: dFun): lFun = lFunDummy
  def helperDoubleDoubleIntString2Long(a1: dFun, a2: dFun, a3: iFun, a4: sFun): lFun = lFunDummy
  def helperDoubleDoubleIntBoolean2Long(a1: dFun, a2: dFun, a3: iFun, a4: bFun): lFun = lFunDummy
  def helperDoubleDoubleLongInt2Long(a1: dFun, a2: dFun, a3: lFun, a4: iFun): lFun = lFunDummy
  def helperDoubleDoubleLongLong2Long(a1: dFun, a2: dFun, a3: lFun, a4: lFun): lFun = lFunDummy
  def helperDoubleDoubleLongDouble2Long(a1: dFun, a2: dFun, a3: lFun, a4: dFun): lFun = lFunDummy
  def helperDoubleDoubleLongString2Long(a1: dFun, a2: dFun, a3: lFun, a4: sFun): lFun = lFunDummy
  def helperDoubleDoubleLongBoolean2Long(a1: dFun, a2: dFun, a3: lFun, a4: bFun): lFun = lFunDummy
  def helperDoubleDoubleDoubleInt2Long(a1: dFun, a2: dFun, a3: dFun, a4: iFun): lFun = lFunDummy
  def helperDoubleDoubleDoubleLong2Long(a1: dFun, a2: dFun, a3: dFun, a4: lFun): lFun = lFunDummy
  def helperDoubleDoubleDoubleDouble2Long(a1: dFun, a2: dFun, a3: dFun, a4: dFun): lFun = lFunDummy
  def helperDoubleDoubleDoubleString2Long(a1: dFun, a2: dFun, a3: dFun, a4: sFun): lFun = lFunDummy
  def helperDoubleDoubleDoubleBoolean2Long(a1: dFun, a2: dFun, a3: dFun, a4: bFun): lFun = lFunDummy
  def helperDoubleDoubleStringInt2Long(a1: dFun, a2: dFun, a3: sFun, a4: iFun): lFun = lFunDummy
  def helperDoubleDoubleStringLong2Long(a1: dFun, a2: dFun, a3: sFun, a4: lFun): lFun = lFunDummy
  def helperDoubleDoubleStringDouble2Long(a1: dFun, a2: dFun, a3: sFun, a4: dFun): lFun = lFunDummy
  def helperDoubleDoubleStringString2Long(a1: dFun, a2: dFun, a3: sFun, a4: sFun): lFun = lFunDummy
  def helperDoubleDoubleStringBoolean2Long(a1: dFun, a2: dFun, a3: sFun, a4: bFun): lFun = lFunDummy
  def helperDoubleDoubleBooleanInt2Long(a1: dFun, a2: dFun, a3: bFun, a4: iFun): lFun = lFunDummy
  def helperDoubleDoubleBooleanLong2Long(a1: dFun, a2: dFun, a3: bFun, a4: lFun): lFun = lFunDummy
  def helperDoubleDoubleBooleanDouble2Long(a1: dFun, a2: dFun, a3: bFun, a4: dFun): lFun = lFunDummy
  def helperDoubleDoubleBooleanString2Long(a1: dFun, a2: dFun, a3: bFun, a4: sFun): lFun = lFunDummy
  def helperDoubleDoubleBooleanBoolean2Long(a1: dFun, a2: dFun, a3: bFun, a4: bFun): lFun = lFunDummy
  def helperDoubleStringIntInt2Long(a1: dFun, a2: sFun, a3: iFun, a4: iFun): lFun = lFunDummy
  def helperDoubleStringIntLong2Long(a1: dFun, a2: sFun, a3: iFun, a4: lFun): lFun = lFunDummy
  def helperDoubleStringIntDouble2Long(a1: dFun, a2: sFun, a3: iFun, a4: dFun): lFun = lFunDummy
  def helperDoubleStringIntString2Long(a1: dFun, a2: sFun, a3: iFun, a4: sFun): lFun = lFunDummy
  def helperDoubleStringIntBoolean2Long(a1: dFun, a2: sFun, a3: iFun, a4: bFun): lFun = lFunDummy
  def helperDoubleStringLongInt2Long(a1: dFun, a2: sFun, a3: lFun, a4: iFun): lFun = lFunDummy
  def helperDoubleStringLongLong2Long(a1: dFun, a2: sFun, a3: lFun, a4: lFun): lFun = lFunDummy
  def helperDoubleStringLongDouble2Long(a1: dFun, a2: sFun, a3: lFun, a4: dFun): lFun = lFunDummy
  def helperDoubleStringLongString2Long(a1: dFun, a2: sFun, a3: lFun, a4: sFun): lFun = lFunDummy
  def helperDoubleStringLongBoolean2Long(a1: dFun, a2: sFun, a3: lFun, a4: bFun): lFun = lFunDummy
  def helperDoubleStringDoubleInt2Long(a1: dFun, a2: sFun, a3: dFun, a4: iFun): lFun = lFunDummy
  def helperDoubleStringDoubleLong2Long(a1: dFun, a2: sFun, a3: dFun, a4: lFun): lFun = lFunDummy
  def helperDoubleStringDoubleDouble2Long(a1: dFun, a2: sFun, a3: dFun, a4: dFun): lFun = lFunDummy
  def helperDoubleStringDoubleString2Long(a1: dFun, a2: sFun, a3: dFun, a4: sFun): lFun = lFunDummy
  def helperDoubleStringDoubleBoolean2Long(a1: dFun, a2: sFun, a3: dFun, a4: bFun): lFun = lFunDummy
  def helperDoubleStringStringInt2Long(a1: dFun, a2: sFun, a3: sFun, a4: iFun): lFun = lFunDummy
  def helperDoubleStringStringLong2Long(a1: dFun, a2: sFun, a3: sFun, a4: lFun): lFun = lFunDummy
  def helperDoubleStringStringDouble2Long(a1: dFun, a2: sFun, a3: sFun, a4: dFun): lFun = lFunDummy
  def helperDoubleStringStringString2Long(a1: dFun, a2: sFun, a3: sFun, a4: sFun): lFun = lFunDummy
  def helperDoubleStringStringBoolean2Long(a1: dFun, a2: sFun, a3: sFun, a4: bFun): lFun = lFunDummy
  def helperDoubleStringBooleanInt2Long(a1: dFun, a2: sFun, a3: bFun, a4: iFun): lFun = lFunDummy
  def helperDoubleStringBooleanLong2Long(a1: dFun, a2: sFun, a3: bFun, a4: lFun): lFun = lFunDummy
  def helperDoubleStringBooleanDouble2Long(a1: dFun, a2: sFun, a3: bFun, a4: dFun): lFun = lFunDummy
  def helperDoubleStringBooleanString2Long(a1: dFun, a2: sFun, a3: bFun, a4: sFun): lFun = lFunDummy
  def helperDoubleStringBooleanBoolean2Long(a1: dFun, a2: sFun, a3: bFun, a4: bFun): lFun = lFunDummy
  def helperDoubleBooleanIntInt2Long(a1: dFun, a2: bFun, a3: iFun, a4: iFun): lFun = lFunDummy
  def helperDoubleBooleanIntLong2Long(a1: dFun, a2: bFun, a3: iFun, a4: lFun): lFun = lFunDummy
  def helperDoubleBooleanIntDouble2Long(a1: dFun, a2: bFun, a3: iFun, a4: dFun): lFun = lFunDummy
  def helperDoubleBooleanIntString2Long(a1: dFun, a2: bFun, a3: iFun, a4: sFun): lFun = lFunDummy
  def helperDoubleBooleanIntBoolean2Long(a1: dFun, a2: bFun, a3: iFun, a4: bFun): lFun = lFunDummy
  def helperDoubleBooleanLongInt2Long(a1: dFun, a2: bFun, a3: lFun, a4: iFun): lFun = lFunDummy
  def helperDoubleBooleanLongLong2Long(a1: dFun, a2: bFun, a3: lFun, a4: lFun): lFun = lFunDummy
  def helperDoubleBooleanLongDouble2Long(a1: dFun, a2: bFun, a3: lFun, a4: dFun): lFun = lFunDummy
  def helperDoubleBooleanLongString2Long(a1: dFun, a2: bFun, a3: lFun, a4: sFun): lFun = lFunDummy
  def helperDoubleBooleanLongBoolean2Long(a1: dFun, a2: bFun, a3: lFun, a4: bFun): lFun = lFunDummy
  def helperDoubleBooleanDoubleInt2Long(a1: dFun, a2: bFun, a3: dFun, a4: iFun): lFun = lFunDummy
  def helperDoubleBooleanDoubleLong2Long(a1: dFun, a2: bFun, a3: dFun, a4: lFun): lFun = lFunDummy
  def helperDoubleBooleanDoubleDouble2Long(a1: dFun, a2: bFun, a3: dFun, a4: dFun): lFun = lFunDummy
  def helperDoubleBooleanDoubleString2Long(a1: dFun, a2: bFun, a3: dFun, a4: sFun): lFun = lFunDummy
  def helperDoubleBooleanDoubleBoolean2Long(a1: dFun, a2: bFun, a3: dFun, a4: bFun): lFun = lFunDummy
  def helperDoubleBooleanStringInt2Long(a1: dFun, a2: bFun, a3: sFun, a4: iFun): lFun = lFunDummy
  def helperDoubleBooleanStringLong2Long(a1: dFun, a2: bFun, a3: sFun, a4: lFun): lFun = lFunDummy
  def helperDoubleBooleanStringDouble2Long(a1: dFun, a2: bFun, a3: sFun, a4: dFun): lFun = lFunDummy
  def helperDoubleBooleanStringString2Long(a1: dFun, a2: bFun, a3: sFun, a4: sFun): lFun = lFunDummy
  def helperDoubleBooleanStringBoolean2Long(a1: dFun, a2: bFun, a3: sFun, a4: bFun): lFun = lFunDummy
  def helperDoubleBooleanBooleanInt2Long(a1: dFun, a2: bFun, a3: bFun, a4: iFun): lFun = lFunDummy
  def helperDoubleBooleanBooleanLong2Long(a1: dFun, a2: bFun, a3: bFun, a4: lFun): lFun = lFunDummy
  def helperDoubleBooleanBooleanDouble2Long(a1: dFun, a2: bFun, a3: bFun, a4: dFun): lFun = lFunDummy
  def helperDoubleBooleanBooleanString2Long(a1: dFun, a2: bFun, a3: bFun, a4: sFun): lFun = lFunDummy
  def helperDoubleBooleanBooleanBoolean2Long(a1: dFun, a2: bFun, a3: bFun, a4: bFun): lFun = lFunDummy
  def helperStringIntIntInt2Long(a1: sFun, a2: iFun, a3: iFun, a4: iFun): lFun = lFunDummy
  def helperStringIntIntLong2Long(a1: sFun, a2: iFun, a3: iFun, a4: lFun): lFun = lFunDummy
  def helperStringIntIntDouble2Long(a1: sFun, a2: iFun, a3: iFun, a4: dFun): lFun = lFunDummy
  def helperStringIntIntString2Long(a1: sFun, a2: iFun, a3: iFun, a4: sFun): lFun = lFunDummy
  def helperStringIntIntBoolean2Long(a1: sFun, a2: iFun, a3: iFun, a4: bFun): lFun = lFunDummy
  def helperStringIntLongInt2Long(a1: sFun, a2: iFun, a3: lFun, a4: iFun): lFun = lFunDummy
  def helperStringIntLongLong2Long(a1: sFun, a2: iFun, a3: lFun, a4: lFun): lFun = lFunDummy
  def helperStringIntLongDouble2Long(a1: sFun, a2: iFun, a3: lFun, a4: dFun): lFun = lFunDummy
  def helperStringIntLongString2Long(a1: sFun, a2: iFun, a3: lFun, a4: sFun): lFun = lFunDummy
  def helperStringIntLongBoolean2Long(a1: sFun, a2: iFun, a3: lFun, a4: bFun): lFun = lFunDummy
  def helperStringIntDoubleInt2Long(a1: sFun, a2: iFun, a3: dFun, a4: iFun): lFun = lFunDummy
  def helperStringIntDoubleLong2Long(a1: sFun, a2: iFun, a3: dFun, a4: lFun): lFun = lFunDummy
  def helperStringIntDoubleDouble2Long(a1: sFun, a2: iFun, a3: dFun, a4: dFun): lFun = lFunDummy
  def helperStringIntDoubleString2Long(a1: sFun, a2: iFun, a3: dFun, a4: sFun): lFun = lFunDummy
  def helperStringIntDoubleBoolean2Long(a1: sFun, a2: iFun, a3: dFun, a4: bFun): lFun = lFunDummy
  def helperStringIntStringInt2Long(a1: sFun, a2: iFun, a3: sFun, a4: iFun): lFun = lFunDummy
  def helperStringIntStringLong2Long(a1: sFun, a2: iFun, a3: sFun, a4: lFun): lFun = lFunDummy
  def helperStringIntStringDouble2Long(a1: sFun, a2: iFun, a3: sFun, a4: dFun): lFun = lFunDummy
  def helperStringIntStringString2Long(a1: sFun, a2: iFun, a3: sFun, a4: sFun): lFun = lFunDummy
  def helperStringIntStringBoolean2Long(a1: sFun, a2: iFun, a3: sFun, a4: bFun): lFun = lFunDummy
  def helperStringIntBooleanInt2Long(a1: sFun, a2: iFun, a3: bFun, a4: iFun): lFun = lFunDummy
  def helperStringIntBooleanLong2Long(a1: sFun, a2: iFun, a3: bFun, a4: lFun): lFun = lFunDummy
  def helperStringIntBooleanDouble2Long(a1: sFun, a2: iFun, a3: bFun, a4: dFun): lFun = lFunDummy
  def helperStringIntBooleanString2Long(a1: sFun, a2: iFun, a3: bFun, a4: sFun): lFun = lFunDummy
  def helperStringIntBooleanBoolean2Long(a1: sFun, a2: iFun, a3: bFun, a4: bFun): lFun = lFunDummy
  def helperStringLongIntInt2Long(a1: sFun, a2: lFun, a3: iFun, a4: iFun): lFun = lFunDummy
  def helperStringLongIntLong2Long(a1: sFun, a2: lFun, a3: iFun, a4: lFun): lFun = lFunDummy
  def helperStringLongIntDouble2Long(a1: sFun, a2: lFun, a3: iFun, a4: dFun): lFun = lFunDummy
  def helperStringLongIntString2Long(a1: sFun, a2: lFun, a3: iFun, a4: sFun): lFun = lFunDummy
  def helperStringLongIntBoolean2Long(a1: sFun, a2: lFun, a3: iFun, a4: bFun): lFun = lFunDummy
  def helperStringLongLongInt2Long(a1: sFun, a2: lFun, a3: lFun, a4: iFun): lFun = lFunDummy
  def helperStringLongLongLong2Long(a1: sFun, a2: lFun, a3: lFun, a4: lFun): lFun = lFunDummy
  def helperStringLongLongDouble2Long(a1: sFun, a2: lFun, a3: lFun, a4: dFun): lFun = lFunDummy
  def helperStringLongLongString2Long(a1: sFun, a2: lFun, a3: lFun, a4: sFun): lFun = lFunDummy
  def helperStringLongLongBoolean2Long(a1: sFun, a2: lFun, a3: lFun, a4: bFun): lFun = lFunDummy
  def helperStringLongDoubleInt2Long(a1: sFun, a2: lFun, a3: dFun, a4: iFun): lFun = lFunDummy
  def helperStringLongDoubleLong2Long(a1: sFun, a2: lFun, a3: dFun, a4: lFun): lFun = lFunDummy
  def helperStringLongDoubleDouble2Long(a1: sFun, a2: lFun, a3: dFun, a4: dFun): lFun = lFunDummy
  def helperStringLongDoubleString2Long(a1: sFun, a2: lFun, a3: dFun, a4: sFun): lFun = lFunDummy
  def helperStringLongDoubleBoolean2Long(a1: sFun, a2: lFun, a3: dFun, a4: bFun): lFun = lFunDummy
  def helperStringLongStringInt2Long(a1: sFun, a2: lFun, a3: sFun, a4: iFun): lFun = lFunDummy
  def helperStringLongStringLong2Long(a1: sFun, a2: lFun, a3: sFun, a4: lFun): lFun = lFunDummy
  def helperStringLongStringDouble2Long(a1: sFun, a2: lFun, a3: sFun, a4: dFun): lFun = lFunDummy
  def helperStringLongStringString2Long(a1: sFun, a2: lFun, a3: sFun, a4: sFun): lFun = lFunDummy
  def helperStringLongStringBoolean2Long(a1: sFun, a2: lFun, a3: sFun, a4: bFun): lFun = lFunDummy
  def helperStringLongBooleanInt2Long(a1: sFun, a2: lFun, a3: bFun, a4: iFun): lFun = lFunDummy
  def helperStringLongBooleanLong2Long(a1: sFun, a2: lFun, a3: bFun, a4: lFun): lFun = lFunDummy
  def helperStringLongBooleanDouble2Long(a1: sFun, a2: lFun, a3: bFun, a4: dFun): lFun = lFunDummy
  def helperStringLongBooleanString2Long(a1: sFun, a2: lFun, a3: bFun, a4: sFun): lFun = lFunDummy
  def helperStringLongBooleanBoolean2Long(a1: sFun, a2: lFun, a3: bFun, a4: bFun): lFun = lFunDummy
  def helperStringDoubleIntInt2Long(a1: sFun, a2: dFun, a3: iFun, a4: iFun): lFun = lFunDummy
  def helperStringDoubleIntLong2Long(a1: sFun, a2: dFun, a3: iFun, a4: lFun): lFun = lFunDummy
  def helperStringDoubleIntDouble2Long(a1: sFun, a2: dFun, a3: iFun, a4: dFun): lFun = lFunDummy
  def helperStringDoubleIntString2Long(a1: sFun, a2: dFun, a3: iFun, a4: sFun): lFun = lFunDummy
  def helperStringDoubleIntBoolean2Long(a1: sFun, a2: dFun, a3: iFun, a4: bFun): lFun = lFunDummy
  def helperStringDoubleLongInt2Long(a1: sFun, a2: dFun, a3: lFun, a4: iFun): lFun = lFunDummy
  def helperStringDoubleLongLong2Long(a1: sFun, a2: dFun, a3: lFun, a4: lFun): lFun = lFunDummy
  def helperStringDoubleLongDouble2Long(a1: sFun, a2: dFun, a3: lFun, a4: dFun): lFun = lFunDummy
  def helperStringDoubleLongString2Long(a1: sFun, a2: dFun, a3: lFun, a4: sFun): lFun = lFunDummy
  def helperStringDoubleLongBoolean2Long(a1: sFun, a2: dFun, a3: lFun, a4: bFun): lFun = lFunDummy
  def helperStringDoubleDoubleInt2Long(a1: sFun, a2: dFun, a3: dFun, a4: iFun): lFun = lFunDummy
  def helperStringDoubleDoubleLong2Long(a1: sFun, a2: dFun, a3: dFun, a4: lFun): lFun = lFunDummy
  def helperStringDoubleDoubleDouble2Long(a1: sFun, a2: dFun, a3: dFun, a4: dFun): lFun = lFunDummy
  def helperStringDoubleDoubleString2Long(a1: sFun, a2: dFun, a3: dFun, a4: sFun): lFun = lFunDummy
  def helperStringDoubleDoubleBoolean2Long(a1: sFun, a2: dFun, a3: dFun, a4: bFun): lFun = lFunDummy
  def helperStringDoubleStringInt2Long(a1: sFun, a2: dFun, a3: sFun, a4: iFun): lFun = lFunDummy
  def helperStringDoubleStringLong2Long(a1: sFun, a2: dFun, a3: sFun, a4: lFun): lFun = lFunDummy
  def helperStringDoubleStringDouble2Long(a1: sFun, a2: dFun, a3: sFun, a4: dFun): lFun = lFunDummy
  def helperStringDoubleStringString2Long(a1: sFun, a2: dFun, a3: sFun, a4: sFun): lFun = lFunDummy
  def helperStringDoubleStringBoolean2Long(a1: sFun, a2: dFun, a3: sFun, a4: bFun): lFun = lFunDummy
  def helperStringDoubleBooleanInt2Long(a1: sFun, a2: dFun, a3: bFun, a4: iFun): lFun = lFunDummy
  def helperStringDoubleBooleanLong2Long(a1: sFun, a2: dFun, a3: bFun, a4: lFun): lFun = lFunDummy
  def helperStringDoubleBooleanDouble2Long(a1: sFun, a2: dFun, a3: bFun, a4: dFun): lFun = lFunDummy
  def helperStringDoubleBooleanString2Long(a1: sFun, a2: dFun, a3: bFun, a4: sFun): lFun = lFunDummy
  def helperStringDoubleBooleanBoolean2Long(a1: sFun, a2: dFun, a3: bFun, a4: bFun): lFun = lFunDummy
  def helperStringStringIntInt2Long(a1: sFun, a2: sFun, a3: iFun, a4: iFun): lFun = lFunDummy
  def helperStringStringIntLong2Long(a1: sFun, a2: sFun, a3: iFun, a4: lFun): lFun = lFunDummy
  def helperStringStringIntDouble2Long(a1: sFun, a2: sFun, a3: iFun, a4: dFun): lFun = lFunDummy
  def helperStringStringIntString2Long(a1: sFun, a2: sFun, a3: iFun, a4: sFun): lFun = lFunDummy
  def helperStringStringIntBoolean2Long(a1: sFun, a2: sFun, a3: iFun, a4: bFun): lFun = lFunDummy
  def helperStringStringLongInt2Long(a1: sFun, a2: sFun, a3: lFun, a4: iFun): lFun = lFunDummy
  def helperStringStringLongLong2Long(a1: sFun, a2: sFun, a3: lFun, a4: lFun): lFun = lFunDummy
  def helperStringStringLongDouble2Long(a1: sFun, a2: sFun, a3: lFun, a4: dFun): lFun = lFunDummy
  def helperStringStringLongString2Long(a1: sFun, a2: sFun, a3: lFun, a4: sFun): lFun = lFunDummy
  def helperStringStringLongBoolean2Long(a1: sFun, a2: sFun, a3: lFun, a4: bFun): lFun = lFunDummy
  def helperStringStringDoubleInt2Long(a1: sFun, a2: sFun, a3: dFun, a4: iFun): lFun = lFunDummy
  def helperStringStringDoubleLong2Long(a1: sFun, a2: sFun, a3: dFun, a4: lFun): lFun = lFunDummy
  def helperStringStringDoubleDouble2Long(a1: sFun, a2: sFun, a3: dFun, a4: dFun): lFun = lFunDummy
  def helperStringStringDoubleString2Long(a1: sFun, a2: sFun, a3: dFun, a4: sFun): lFun = lFunDummy
  def helperStringStringDoubleBoolean2Long(a1: sFun, a2: sFun, a3: dFun, a4: bFun): lFun = lFunDummy
  def helperStringStringStringInt2Long(a1: sFun, a2: sFun, a3: sFun, a4: iFun): lFun = lFunDummy
  def helperStringStringStringLong2Long(a1: sFun, a2: sFun, a3: sFun, a4: lFun): lFun = lFunDummy
  def helperStringStringStringDouble2Long(a1: sFun, a2: sFun, a3: sFun, a4: dFun): lFun = lFunDummy
  def helperStringStringStringString2Long(a1: sFun, a2: sFun, a3: sFun, a4: sFun): lFun = lFunDummy
  def helperStringStringStringBoolean2Long(a1: sFun, a2: sFun, a3: sFun, a4: bFun): lFun = lFunDummy
  def helperStringStringBooleanInt2Long(a1: sFun, a2: sFun, a3: bFun, a4: iFun): lFun = lFunDummy
  def helperStringStringBooleanLong2Long(a1: sFun, a2: sFun, a3: bFun, a4: lFun): lFun = lFunDummy
  def helperStringStringBooleanDouble2Long(a1: sFun, a2: sFun, a3: bFun, a4: dFun): lFun = lFunDummy
  def helperStringStringBooleanString2Long(a1: sFun, a2: sFun, a3: bFun, a4: sFun): lFun = lFunDummy
  def helperStringStringBooleanBoolean2Long(a1: sFun, a2: sFun, a3: bFun, a4: bFun): lFun = lFunDummy
  def helperStringBooleanIntInt2Long(a1: sFun, a2: bFun, a3: iFun, a4: iFun): lFun = lFunDummy
  def helperStringBooleanIntLong2Long(a1: sFun, a2: bFun, a3: iFun, a4: lFun): lFun = lFunDummy
  def helperStringBooleanIntDouble2Long(a1: sFun, a2: bFun, a3: iFun, a4: dFun): lFun = lFunDummy
  def helperStringBooleanIntString2Long(a1: sFun, a2: bFun, a3: iFun, a4: sFun): lFun = lFunDummy
  def helperStringBooleanIntBoolean2Long(a1: sFun, a2: bFun, a3: iFun, a4: bFun): lFun = lFunDummy
  def helperStringBooleanLongInt2Long(a1: sFun, a2: bFun, a3: lFun, a4: iFun): lFun = lFunDummy
  def helperStringBooleanLongLong2Long(a1: sFun, a2: bFun, a3: lFun, a4: lFun): lFun = lFunDummy
  def helperStringBooleanLongDouble2Long(a1: sFun, a2: bFun, a3: lFun, a4: dFun): lFun = lFunDummy
  def helperStringBooleanLongString2Long(a1: sFun, a2: bFun, a3: lFun, a4: sFun): lFun = lFunDummy
  def helperStringBooleanLongBoolean2Long(a1: sFun, a2: bFun, a3: lFun, a4: bFun): lFun = lFunDummy
  def helperStringBooleanDoubleInt2Long(a1: sFun, a2: bFun, a3: dFun, a4: iFun): lFun = lFunDummy
  def helperStringBooleanDoubleLong2Long(a1: sFun, a2: bFun, a3: dFun, a4: lFun): lFun = lFunDummy
  def helperStringBooleanDoubleDouble2Long(a1: sFun, a2: bFun, a3: dFun, a4: dFun): lFun = lFunDummy
  def helperStringBooleanDoubleString2Long(a1: sFun, a2: bFun, a3: dFun, a4: sFun): lFun = lFunDummy
  def helperStringBooleanDoubleBoolean2Long(a1: sFun, a2: bFun, a3: dFun, a4: bFun): lFun = lFunDummy
  def helperStringBooleanStringInt2Long(a1: sFun, a2: bFun, a3: sFun, a4: iFun): lFun = lFunDummy
  def helperStringBooleanStringLong2Long(a1: sFun, a2: bFun, a3: sFun, a4: lFun): lFun = lFunDummy
  def helperStringBooleanStringDouble2Long(a1: sFun, a2: bFun, a3: sFun, a4: dFun): lFun = lFunDummy
  def helperStringBooleanStringString2Long(a1: sFun, a2: bFun, a3: sFun, a4: sFun): lFun = lFunDummy
  def helperStringBooleanStringBoolean2Long(a1: sFun, a2: bFun, a3: sFun, a4: bFun): lFun = lFunDummy
  def helperStringBooleanBooleanInt2Long(a1: sFun, a2: bFun, a3: bFun, a4: iFun): lFun = lFunDummy
  def helperStringBooleanBooleanLong2Long(a1: sFun, a2: bFun, a3: bFun, a4: lFun): lFun = lFunDummy
  def helperStringBooleanBooleanDouble2Long(a1: sFun, a2: bFun, a3: bFun, a4: dFun): lFun = lFunDummy
  def helperStringBooleanBooleanString2Long(a1: sFun, a2: bFun, a3: bFun, a4: sFun): lFun = lFunDummy
  def helperStringBooleanBooleanBoolean2Long(a1: sFun, a2: bFun, a3: bFun, a4: bFun): lFun = lFunDummy
  def helperBooleanIntIntInt2Long(a1: bFun, a2: iFun, a3: iFun, a4: iFun): lFun = lFunDummy
  def helperBooleanIntIntLong2Long(a1: bFun, a2: iFun, a3: iFun, a4: lFun): lFun = lFunDummy
  def helperBooleanIntIntDouble2Long(a1: bFun, a2: iFun, a3: iFun, a4: dFun): lFun = lFunDummy
  def helperBooleanIntIntString2Long(a1: bFun, a2: iFun, a3: iFun, a4: sFun): lFun = lFunDummy
  def helperBooleanIntIntBoolean2Long(a1: bFun, a2: iFun, a3: iFun, a4: bFun): lFun = lFunDummy
  def helperBooleanIntLongInt2Long(a1: bFun, a2: iFun, a3: lFun, a4: iFun): lFun = lFunDummy
  def helperBooleanIntLongLong2Long(a1: bFun, a2: iFun, a3: lFun, a4: lFun): lFun = lFunDummy
  def helperBooleanIntLongDouble2Long(a1: bFun, a2: iFun, a3: lFun, a4: dFun): lFun = lFunDummy
  def helperBooleanIntLongString2Long(a1: bFun, a2: iFun, a3: lFun, a4: sFun): lFun = lFunDummy
  def helperBooleanIntLongBoolean2Long(a1: bFun, a2: iFun, a3: lFun, a4: bFun): lFun = lFunDummy
  def helperBooleanIntDoubleInt2Long(a1: bFun, a2: iFun, a3: dFun, a4: iFun): lFun = lFunDummy
  def helperBooleanIntDoubleLong2Long(a1: bFun, a2: iFun, a3: dFun, a4: lFun): lFun = lFunDummy
  def helperBooleanIntDoubleDouble2Long(a1: bFun, a2: iFun, a3: dFun, a4: dFun): lFun = lFunDummy
  def helperBooleanIntDoubleString2Long(a1: bFun, a2: iFun, a3: dFun, a4: sFun): lFun = lFunDummy
  def helperBooleanIntDoubleBoolean2Long(a1: bFun, a2: iFun, a3: dFun, a4: bFun): lFun = lFunDummy
  def helperBooleanIntStringInt2Long(a1: bFun, a2: iFun, a3: sFun, a4: iFun): lFun = lFunDummy
  def helperBooleanIntStringLong2Long(a1: bFun, a2: iFun, a3: sFun, a4: lFun): lFun = lFunDummy
  def helperBooleanIntStringDouble2Long(a1: bFun, a2: iFun, a3: sFun, a4: dFun): lFun = lFunDummy
  def helperBooleanIntStringString2Long(a1: bFun, a2: iFun, a3: sFun, a4: sFun): lFun = lFunDummy
  def helperBooleanIntStringBoolean2Long(a1: bFun, a2: iFun, a3: sFun, a4: bFun): lFun = lFunDummy
  def helperBooleanIntBooleanInt2Long(a1: bFun, a2: iFun, a3: bFun, a4: iFun): lFun = lFunDummy
  def helperBooleanIntBooleanLong2Long(a1: bFun, a2: iFun, a3: bFun, a4: lFun): lFun = lFunDummy
  def helperBooleanIntBooleanDouble2Long(a1: bFun, a2: iFun, a3: bFun, a4: dFun): lFun = lFunDummy
  def helperBooleanIntBooleanString2Long(a1: bFun, a2: iFun, a3: bFun, a4: sFun): lFun = lFunDummy
  def helperBooleanIntBooleanBoolean2Long(a1: bFun, a2: iFun, a3: bFun, a4: bFun): lFun = lFunDummy
  def helperBooleanLongIntInt2Long(a1: bFun, a2: lFun, a3: iFun, a4: iFun): lFun = lFunDummy
  def helperBooleanLongIntLong2Long(a1: bFun, a2: lFun, a3: iFun, a4: lFun): lFun = lFunDummy
  def helperBooleanLongIntDouble2Long(a1: bFun, a2: lFun, a3: iFun, a4: dFun): lFun = lFunDummy
  def helperBooleanLongIntString2Long(a1: bFun, a2: lFun, a3: iFun, a4: sFun): lFun = lFunDummy
  def helperBooleanLongIntBoolean2Long(a1: bFun, a2: lFun, a3: iFun, a4: bFun): lFun = lFunDummy
  def helperBooleanLongLongInt2Long(a1: bFun, a2: lFun, a3: lFun, a4: iFun): lFun = lFunDummy
  def helperBooleanLongLongLong2Long(a1: bFun, a2: lFun, a3: lFun, a4: lFun): lFun = lFunDummy
  def helperBooleanLongLongDouble2Long(a1: bFun, a2: lFun, a3: lFun, a4: dFun): lFun = lFunDummy
  def helperBooleanLongLongString2Long(a1: bFun, a2: lFun, a3: lFun, a4: sFun): lFun = lFunDummy
  def helperBooleanLongLongBoolean2Long(a1: bFun, a2: lFun, a3: lFun, a4: bFun): lFun = lFunDummy
  def helperBooleanLongDoubleInt2Long(a1: bFun, a2: lFun, a3: dFun, a4: iFun): lFun = lFunDummy
  def helperBooleanLongDoubleLong2Long(a1: bFun, a2: lFun, a3: dFun, a4: lFun): lFun = lFunDummy
  def helperBooleanLongDoubleDouble2Long(a1: bFun, a2: lFun, a3: dFun, a4: dFun): lFun = lFunDummy
  def helperBooleanLongDoubleString2Long(a1: bFun, a2: lFun, a3: dFun, a4: sFun): lFun = lFunDummy
  def helperBooleanLongDoubleBoolean2Long(a1: bFun, a2: lFun, a3: dFun, a4: bFun): lFun = lFunDummy
  def helperBooleanLongStringInt2Long(a1: bFun, a2: lFun, a3: sFun, a4: iFun): lFun = lFunDummy
  def helperBooleanLongStringLong2Long(a1: bFun, a2: lFun, a3: sFun, a4: lFun): lFun = lFunDummy
  def helperBooleanLongStringDouble2Long(a1: bFun, a2: lFun, a3: sFun, a4: dFun): lFun = lFunDummy
  def helperBooleanLongStringString2Long(a1: bFun, a2: lFun, a3: sFun, a4: sFun): lFun = lFunDummy
  def helperBooleanLongStringBoolean2Long(a1: bFun, a2: lFun, a3: sFun, a4: bFun): lFun = lFunDummy
  def helperBooleanLongBooleanInt2Long(a1: bFun, a2: lFun, a3: bFun, a4: iFun): lFun = lFunDummy
  def helperBooleanLongBooleanLong2Long(a1: bFun, a2: lFun, a3: bFun, a4: lFun): lFun = lFunDummy
  def helperBooleanLongBooleanDouble2Long(a1: bFun, a2: lFun, a3: bFun, a4: dFun): lFun = lFunDummy
  def helperBooleanLongBooleanString2Long(a1: bFun, a2: lFun, a3: bFun, a4: sFun): lFun = lFunDummy
  def helperBooleanLongBooleanBoolean2Long(a1: bFun, a2: lFun, a3: bFun, a4: bFun): lFun = lFunDummy
  def helperBooleanDoubleIntInt2Long(a1: bFun, a2: dFun, a3: iFun, a4: iFun): lFun = lFunDummy
  def helperBooleanDoubleIntLong2Long(a1: bFun, a2: dFun, a3: iFun, a4: lFun): lFun = lFunDummy
  def helperBooleanDoubleIntDouble2Long(a1: bFun, a2: dFun, a3: iFun, a4: dFun): lFun = lFunDummy
  def helperBooleanDoubleIntString2Long(a1: bFun, a2: dFun, a3: iFun, a4: sFun): lFun = lFunDummy
  def helperBooleanDoubleIntBoolean2Long(a1: bFun, a2: dFun, a3: iFun, a4: bFun): lFun = lFunDummy
  def helperBooleanDoubleLongInt2Long(a1: bFun, a2: dFun, a3: lFun, a4: iFun): lFun = lFunDummy
  def helperBooleanDoubleLongLong2Long(a1: bFun, a2: dFun, a3: lFun, a4: lFun): lFun = lFunDummy
  def helperBooleanDoubleLongDouble2Long(a1: bFun, a2: dFun, a3: lFun, a4: dFun): lFun = lFunDummy
  def helperBooleanDoubleLongString2Long(a1: bFun, a2: dFun, a3: lFun, a4: sFun): lFun = lFunDummy
  def helperBooleanDoubleLongBoolean2Long(a1: bFun, a2: dFun, a3: lFun, a4: bFun): lFun = lFunDummy
  def helperBooleanDoubleDoubleInt2Long(a1: bFun, a2: dFun, a3: dFun, a4: iFun): lFun = lFunDummy
  def helperBooleanDoubleDoubleLong2Long(a1: bFun, a2: dFun, a3: dFun, a4: lFun): lFun = lFunDummy
  def helperBooleanDoubleDoubleDouble2Long(a1: bFun, a2: dFun, a3: dFun, a4: dFun): lFun = lFunDummy
  def helperBooleanDoubleDoubleString2Long(a1: bFun, a2: dFun, a3: dFun, a4: sFun): lFun = lFunDummy
  def helperBooleanDoubleDoubleBoolean2Long(a1: bFun, a2: dFun, a3: dFun, a4: bFun): lFun = lFunDummy
  def helperBooleanDoubleStringInt2Long(a1: bFun, a2: dFun, a3: sFun, a4: iFun): lFun = lFunDummy
  def helperBooleanDoubleStringLong2Long(a1: bFun, a2: dFun, a3: sFun, a4: lFun): lFun = lFunDummy
  def helperBooleanDoubleStringDouble2Long(a1: bFun, a2: dFun, a3: sFun, a4: dFun): lFun = lFunDummy
  def helperBooleanDoubleStringString2Long(a1: bFun, a2: dFun, a3: sFun, a4: sFun): lFun = lFunDummy
  def helperBooleanDoubleStringBoolean2Long(a1: bFun, a2: dFun, a3: sFun, a4: bFun): lFun = lFunDummy
  def helperBooleanDoubleBooleanInt2Long(a1: bFun, a2: dFun, a3: bFun, a4: iFun): lFun = lFunDummy
  def helperBooleanDoubleBooleanLong2Long(a1: bFun, a2: dFun, a3: bFun, a4: lFun): lFun = lFunDummy
  def helperBooleanDoubleBooleanDouble2Long(a1: bFun, a2: dFun, a3: bFun, a4: dFun): lFun = lFunDummy
  def helperBooleanDoubleBooleanString2Long(a1: bFun, a2: dFun, a3: bFun, a4: sFun): lFun = lFunDummy
  def helperBooleanDoubleBooleanBoolean2Long(a1: bFun, a2: dFun, a3: bFun, a4: bFun): lFun = lFunDummy
  def helperBooleanStringIntInt2Long(a1: bFun, a2: sFun, a3: iFun, a4: iFun): lFun = lFunDummy
  def helperBooleanStringIntLong2Long(a1: bFun, a2: sFun, a3: iFun, a4: lFun): lFun = lFunDummy
  def helperBooleanStringIntDouble2Long(a1: bFun, a2: sFun, a3: iFun, a4: dFun): lFun = lFunDummy
  def helperBooleanStringIntString2Long(a1: bFun, a2: sFun, a3: iFun, a4: sFun): lFun = lFunDummy
  def helperBooleanStringIntBoolean2Long(a1: bFun, a2: sFun, a3: iFun, a4: bFun): lFun = lFunDummy
  def helperBooleanStringLongInt2Long(a1: bFun, a2: sFun, a3: lFun, a4: iFun): lFun = lFunDummy
  def helperBooleanStringLongLong2Long(a1: bFun, a2: sFun, a3: lFun, a4: lFun): lFun = lFunDummy
  def helperBooleanStringLongDouble2Long(a1: bFun, a2: sFun, a3: lFun, a4: dFun): lFun = lFunDummy
  def helperBooleanStringLongString2Long(a1: bFun, a2: sFun, a3: lFun, a4: sFun): lFun = lFunDummy
  def helperBooleanStringLongBoolean2Long(a1: bFun, a2: sFun, a3: lFun, a4: bFun): lFun = lFunDummy
  def helperBooleanStringDoubleInt2Long(a1: bFun, a2: sFun, a3: dFun, a4: iFun): lFun = lFunDummy
  def helperBooleanStringDoubleLong2Long(a1: bFun, a2: sFun, a3: dFun, a4: lFun): lFun = lFunDummy
  def helperBooleanStringDoubleDouble2Long(a1: bFun, a2: sFun, a3: dFun, a4: dFun): lFun = lFunDummy
  def helperBooleanStringDoubleString2Long(a1: bFun, a2: sFun, a3: dFun, a4: sFun): lFun = lFunDummy
  def helperBooleanStringDoubleBoolean2Long(a1: bFun, a2: sFun, a3: dFun, a4: bFun): lFun = lFunDummy
  def helperBooleanStringStringInt2Long(a1: bFun, a2: sFun, a3: sFun, a4: iFun): lFun = lFunDummy
  def helperBooleanStringStringLong2Long(a1: bFun, a2: sFun, a3: sFun, a4: lFun): lFun = lFunDummy
  def helperBooleanStringStringDouble2Long(a1: bFun, a2: sFun, a3: sFun, a4: dFun): lFun = lFunDummy
  def helperBooleanStringStringString2Long(a1: bFun, a2: sFun, a3: sFun, a4: sFun): lFun = lFunDummy
  def helperBooleanStringStringBoolean2Long(a1: bFun, a2: sFun, a3: sFun, a4: bFun): lFun = lFunDummy
  def helperBooleanStringBooleanInt2Long(a1: bFun, a2: sFun, a3: bFun, a4: iFun): lFun = lFunDummy
  def helperBooleanStringBooleanLong2Long(a1: bFun, a2: sFun, a3: bFun, a4: lFun): lFun = lFunDummy
  def helperBooleanStringBooleanDouble2Long(a1: bFun, a2: sFun, a3: bFun, a4: dFun): lFun = lFunDummy
  def helperBooleanStringBooleanString2Long(a1: bFun, a2: sFun, a3: bFun, a4: sFun): lFun = lFunDummy
  def helperBooleanStringBooleanBoolean2Long(a1: bFun, a2: sFun, a3: bFun, a4: bFun): lFun = lFunDummy
  def helperBooleanBooleanIntInt2Long(a1: bFun, a2: bFun, a3: iFun, a4: iFun): lFun = lFunDummy
  def helperBooleanBooleanIntLong2Long(a1: bFun, a2: bFun, a3: iFun, a4: lFun): lFun = lFunDummy
  def helperBooleanBooleanIntDouble2Long(a1: bFun, a2: bFun, a3: iFun, a4: dFun): lFun = lFunDummy
  def helperBooleanBooleanIntString2Long(a1: bFun, a2: bFun, a3: iFun, a4: sFun): lFun = lFunDummy
  def helperBooleanBooleanIntBoolean2Long(a1: bFun, a2: bFun, a3: iFun, a4: bFun): lFun = lFunDummy
  def helperBooleanBooleanLongInt2Long(a1: bFun, a2: bFun, a3: lFun, a4: iFun): lFun = lFunDummy
  def helperBooleanBooleanLongLong2Long(a1: bFun, a2: bFun, a3: lFun, a4: lFun): lFun = lFunDummy
  def helperBooleanBooleanLongDouble2Long(a1: bFun, a2: bFun, a3: lFun, a4: dFun): lFun = lFunDummy
  def helperBooleanBooleanLongString2Long(a1: bFun, a2: bFun, a3: lFun, a4: sFun): lFun = lFunDummy
  def helperBooleanBooleanLongBoolean2Long(a1: bFun, a2: bFun, a3: lFun, a4: bFun): lFun = lFunDummy
  def helperBooleanBooleanDoubleInt2Long(a1: bFun, a2: bFun, a3: dFun, a4: iFun): lFun = lFunDummy
  def helperBooleanBooleanDoubleLong2Long(a1: bFun, a2: bFun, a3: dFun, a4: lFun): lFun = lFunDummy
  def helperBooleanBooleanDoubleDouble2Long(a1: bFun, a2: bFun, a3: dFun, a4: dFun): lFun = lFunDummy
  def helperBooleanBooleanDoubleString2Long(a1: bFun, a2: bFun, a3: dFun, a4: sFun): lFun = lFunDummy
  def helperBooleanBooleanDoubleBoolean2Long(a1: bFun, a2: bFun, a3: dFun, a4: bFun): lFun = lFunDummy
  def helperBooleanBooleanStringInt2Long(a1: bFun, a2: bFun, a3: sFun, a4: iFun): lFun = lFunDummy
  def helperBooleanBooleanStringLong2Long(a1: bFun, a2: bFun, a3: sFun, a4: lFun): lFun = lFunDummy
  def helperBooleanBooleanStringDouble2Long(a1: bFun, a2: bFun, a3: sFun, a4: dFun): lFun = lFunDummy
  def helperBooleanBooleanStringString2Long(a1: bFun, a2: bFun, a3: sFun, a4: sFun): lFun = lFunDummy
  def helperBooleanBooleanStringBoolean2Long(a1: bFun, a2: bFun, a3: sFun, a4: bFun): lFun = lFunDummy
  def helperBooleanBooleanBooleanInt2Long(a1: bFun, a2: bFun, a3: bFun, a4: iFun): lFun = lFunDummy
  def helperBooleanBooleanBooleanLong2Long(a1: bFun, a2: bFun, a3: bFun, a4: lFun): lFun = lFunDummy
  def helperBooleanBooleanBooleanDouble2Long(a1: bFun, a2: bFun, a3: bFun, a4: dFun): lFun = lFunDummy
  def helperBooleanBooleanBooleanString2Long(a1: bFun, a2: bFun, a3: bFun, a4: sFun): lFun = lFunDummy
  def helperBooleanBooleanBooleanBoolean2Long(a1: bFun, a2: bFun, a3: bFun, a4: bFun): lFun = lFunDummy
  test("getSignature_lFun_5") {
    var result = true
    result &= FunctionSignature.getSignatureIntIntIntInt2Long(helperIntIntIntInt2Long) == "Int:Int:Int:Int2Long"
    result &= FunctionSignature.getSignatureIntIntIntLong2Long(helperIntIntIntLong2Long) == "Int:Int:Int:Long2Long"
    result &= FunctionSignature.getSignatureIntIntIntDouble2Long(helperIntIntIntDouble2Long) == "Int:Int:Int:Double2Long"
    result &= FunctionSignature.getSignatureIntIntIntString2Long(helperIntIntIntString2Long) == "Int:Int:Int:String2Long"
    result &= FunctionSignature.getSignatureIntIntIntBoolean2Long(helperIntIntIntBoolean2Long) == "Int:Int:Int:Boolean2Long"
    result &= FunctionSignature.getSignatureIntIntLongInt2Long(helperIntIntLongInt2Long) == "Int:Int:Long:Int2Long"
    result &= FunctionSignature.getSignatureIntIntLongLong2Long(helperIntIntLongLong2Long) == "Int:Int:Long:Long2Long"
    result &= FunctionSignature.getSignatureIntIntLongDouble2Long(helperIntIntLongDouble2Long) == "Int:Int:Long:Double2Long"
    result &= FunctionSignature.getSignatureIntIntLongString2Long(helperIntIntLongString2Long) == "Int:Int:Long:String2Long"
    result &= FunctionSignature.getSignatureIntIntLongBoolean2Long(helperIntIntLongBoolean2Long) == "Int:Int:Long:Boolean2Long"
    result &= FunctionSignature.getSignatureIntIntDoubleInt2Long(helperIntIntDoubleInt2Long) == "Int:Int:Double:Int2Long"
    result &= FunctionSignature.getSignatureIntIntDoubleLong2Long(helperIntIntDoubleLong2Long) == "Int:Int:Double:Long2Long"
    result &= FunctionSignature.getSignatureIntIntDoubleDouble2Long(helperIntIntDoubleDouble2Long) == "Int:Int:Double:Double2Long"
    result &= FunctionSignature.getSignatureIntIntDoubleString2Long(helperIntIntDoubleString2Long) == "Int:Int:Double:String2Long"
    result &= FunctionSignature.getSignatureIntIntDoubleBoolean2Long(helperIntIntDoubleBoolean2Long) == "Int:Int:Double:Boolean2Long"
    result &= FunctionSignature.getSignatureIntIntStringInt2Long(helperIntIntStringInt2Long) == "Int:Int:String:Int2Long"
    result &= FunctionSignature.getSignatureIntIntStringLong2Long(helperIntIntStringLong2Long) == "Int:Int:String:Long2Long"
    result &= FunctionSignature.getSignatureIntIntStringDouble2Long(helperIntIntStringDouble2Long) == "Int:Int:String:Double2Long"
    result &= FunctionSignature.getSignatureIntIntStringString2Long(helperIntIntStringString2Long) == "Int:Int:String:String2Long"
    result &= FunctionSignature.getSignatureIntIntStringBoolean2Long(helperIntIntStringBoolean2Long) == "Int:Int:String:Boolean2Long"
    result &= FunctionSignature.getSignatureIntIntBooleanInt2Long(helperIntIntBooleanInt2Long) == "Int:Int:Boolean:Int2Long"
    result &= FunctionSignature.getSignatureIntIntBooleanLong2Long(helperIntIntBooleanLong2Long) == "Int:Int:Boolean:Long2Long"
    result &= FunctionSignature.getSignatureIntIntBooleanDouble2Long(helperIntIntBooleanDouble2Long) == "Int:Int:Boolean:Double2Long"
    result &= FunctionSignature.getSignatureIntIntBooleanString2Long(helperIntIntBooleanString2Long) == "Int:Int:Boolean:String2Long"
    result &= FunctionSignature.getSignatureIntIntBooleanBoolean2Long(helperIntIntBooleanBoolean2Long) == "Int:Int:Boolean:Boolean2Long"
    result &= FunctionSignature.getSignatureIntLongIntInt2Long(helperIntLongIntInt2Long) == "Int:Long:Int:Int2Long"
    result &= FunctionSignature.getSignatureIntLongIntLong2Long(helperIntLongIntLong2Long) == "Int:Long:Int:Long2Long"
    result &= FunctionSignature.getSignatureIntLongIntDouble2Long(helperIntLongIntDouble2Long) == "Int:Long:Int:Double2Long"
    result &= FunctionSignature.getSignatureIntLongIntString2Long(helperIntLongIntString2Long) == "Int:Long:Int:String2Long"
    result &= FunctionSignature.getSignatureIntLongIntBoolean2Long(helperIntLongIntBoolean2Long) == "Int:Long:Int:Boolean2Long"
    result &= FunctionSignature.getSignatureIntLongLongInt2Long(helperIntLongLongInt2Long) == "Int:Long:Long:Int2Long"
    result &= FunctionSignature.getSignatureIntLongLongLong2Long(helperIntLongLongLong2Long) == "Int:Long:Long:Long2Long"
    result &= FunctionSignature.getSignatureIntLongLongDouble2Long(helperIntLongLongDouble2Long) == "Int:Long:Long:Double2Long"
    result &= FunctionSignature.getSignatureIntLongLongString2Long(helperIntLongLongString2Long) == "Int:Long:Long:String2Long"
    result &= FunctionSignature.getSignatureIntLongLongBoolean2Long(helperIntLongLongBoolean2Long) == "Int:Long:Long:Boolean2Long"
    result &= FunctionSignature.getSignatureIntLongDoubleInt2Long(helperIntLongDoubleInt2Long) == "Int:Long:Double:Int2Long"
    result &= FunctionSignature.getSignatureIntLongDoubleLong2Long(helperIntLongDoubleLong2Long) == "Int:Long:Double:Long2Long"
    result &= FunctionSignature.getSignatureIntLongDoubleDouble2Long(helperIntLongDoubleDouble2Long) == "Int:Long:Double:Double2Long"
    result &= FunctionSignature.getSignatureIntLongDoubleString2Long(helperIntLongDoubleString2Long) == "Int:Long:Double:String2Long"
    result &= FunctionSignature.getSignatureIntLongDoubleBoolean2Long(helperIntLongDoubleBoolean2Long) == "Int:Long:Double:Boolean2Long"
    result &= FunctionSignature.getSignatureIntLongStringInt2Long(helperIntLongStringInt2Long) == "Int:Long:String:Int2Long"
    result &= FunctionSignature.getSignatureIntLongStringLong2Long(helperIntLongStringLong2Long) == "Int:Long:String:Long2Long"
    result &= FunctionSignature.getSignatureIntLongStringDouble2Long(helperIntLongStringDouble2Long) == "Int:Long:String:Double2Long"
    result &= FunctionSignature.getSignatureIntLongStringString2Long(helperIntLongStringString2Long) == "Int:Long:String:String2Long"
    result &= FunctionSignature.getSignatureIntLongStringBoolean2Long(helperIntLongStringBoolean2Long) == "Int:Long:String:Boolean2Long"
    result &= FunctionSignature.getSignatureIntLongBooleanInt2Long(helperIntLongBooleanInt2Long) == "Int:Long:Boolean:Int2Long"
    result &= FunctionSignature.getSignatureIntLongBooleanLong2Long(helperIntLongBooleanLong2Long) == "Int:Long:Boolean:Long2Long"
    result &= FunctionSignature.getSignatureIntLongBooleanDouble2Long(helperIntLongBooleanDouble2Long) == "Int:Long:Boolean:Double2Long"
    result &= FunctionSignature.getSignatureIntLongBooleanString2Long(helperIntLongBooleanString2Long) == "Int:Long:Boolean:String2Long"
    result &= FunctionSignature.getSignatureIntLongBooleanBoolean2Long(helperIntLongBooleanBoolean2Long) == "Int:Long:Boolean:Boolean2Long"
    result &= FunctionSignature.getSignatureIntDoubleIntInt2Long(helperIntDoubleIntInt2Long) == "Int:Double:Int:Int2Long"
    result &= FunctionSignature.getSignatureIntDoubleIntLong2Long(helperIntDoubleIntLong2Long) == "Int:Double:Int:Long2Long"
    result &= FunctionSignature.getSignatureIntDoubleIntDouble2Long(helperIntDoubleIntDouble2Long) == "Int:Double:Int:Double2Long"
    result &= FunctionSignature.getSignatureIntDoubleIntString2Long(helperIntDoubleIntString2Long) == "Int:Double:Int:String2Long"
    result &= FunctionSignature.getSignatureIntDoubleIntBoolean2Long(helperIntDoubleIntBoolean2Long) == "Int:Double:Int:Boolean2Long"
    result &= FunctionSignature.getSignatureIntDoubleLongInt2Long(helperIntDoubleLongInt2Long) == "Int:Double:Long:Int2Long"
    result &= FunctionSignature.getSignatureIntDoubleLongLong2Long(helperIntDoubleLongLong2Long) == "Int:Double:Long:Long2Long"
    result &= FunctionSignature.getSignatureIntDoubleLongDouble2Long(helperIntDoubleLongDouble2Long) == "Int:Double:Long:Double2Long"
    result &= FunctionSignature.getSignatureIntDoubleLongString2Long(helperIntDoubleLongString2Long) == "Int:Double:Long:String2Long"
    result &= FunctionSignature.getSignatureIntDoubleLongBoolean2Long(helperIntDoubleLongBoolean2Long) == "Int:Double:Long:Boolean2Long"
    result &= FunctionSignature.getSignatureIntDoubleDoubleInt2Long(helperIntDoubleDoubleInt2Long) == "Int:Double:Double:Int2Long"
    result &= FunctionSignature.getSignatureIntDoubleDoubleLong2Long(helperIntDoubleDoubleLong2Long) == "Int:Double:Double:Long2Long"
    result &= FunctionSignature.getSignatureIntDoubleDoubleDouble2Long(helperIntDoubleDoubleDouble2Long) == "Int:Double:Double:Double2Long"
    result &= FunctionSignature.getSignatureIntDoubleDoubleString2Long(helperIntDoubleDoubleString2Long) == "Int:Double:Double:String2Long"
    result &= FunctionSignature.getSignatureIntDoubleDoubleBoolean2Long(helperIntDoubleDoubleBoolean2Long) == "Int:Double:Double:Boolean2Long"
    result &= FunctionSignature.getSignatureIntDoubleStringInt2Long(helperIntDoubleStringInt2Long) == "Int:Double:String:Int2Long"
    result &= FunctionSignature.getSignatureIntDoubleStringLong2Long(helperIntDoubleStringLong2Long) == "Int:Double:String:Long2Long"
    result &= FunctionSignature.getSignatureIntDoubleStringDouble2Long(helperIntDoubleStringDouble2Long) == "Int:Double:String:Double2Long"
    result &= FunctionSignature.getSignatureIntDoubleStringString2Long(helperIntDoubleStringString2Long) == "Int:Double:String:String2Long"
    result &= FunctionSignature.getSignatureIntDoubleStringBoolean2Long(helperIntDoubleStringBoolean2Long) == "Int:Double:String:Boolean2Long"
    result &= FunctionSignature.getSignatureIntDoubleBooleanInt2Long(helperIntDoubleBooleanInt2Long) == "Int:Double:Boolean:Int2Long"
    result &= FunctionSignature.getSignatureIntDoubleBooleanLong2Long(helperIntDoubleBooleanLong2Long) == "Int:Double:Boolean:Long2Long"
    result &= FunctionSignature.getSignatureIntDoubleBooleanDouble2Long(helperIntDoubleBooleanDouble2Long) == "Int:Double:Boolean:Double2Long"
    result &= FunctionSignature.getSignatureIntDoubleBooleanString2Long(helperIntDoubleBooleanString2Long) == "Int:Double:Boolean:String2Long"
    result &= FunctionSignature.getSignatureIntDoubleBooleanBoolean2Long(helperIntDoubleBooleanBoolean2Long) == "Int:Double:Boolean:Boolean2Long"
    result &= FunctionSignature.getSignatureIntStringIntInt2Long(helperIntStringIntInt2Long) == "Int:String:Int:Int2Long"
    result &= FunctionSignature.getSignatureIntStringIntLong2Long(helperIntStringIntLong2Long) == "Int:String:Int:Long2Long"
    result &= FunctionSignature.getSignatureIntStringIntDouble2Long(helperIntStringIntDouble2Long) == "Int:String:Int:Double2Long"
    result &= FunctionSignature.getSignatureIntStringIntString2Long(helperIntStringIntString2Long) == "Int:String:Int:String2Long"
    result &= FunctionSignature.getSignatureIntStringIntBoolean2Long(helperIntStringIntBoolean2Long) == "Int:String:Int:Boolean2Long"
    result &= FunctionSignature.getSignatureIntStringLongInt2Long(helperIntStringLongInt2Long) == "Int:String:Long:Int2Long"
    result &= FunctionSignature.getSignatureIntStringLongLong2Long(helperIntStringLongLong2Long) == "Int:String:Long:Long2Long"
    result &= FunctionSignature.getSignatureIntStringLongDouble2Long(helperIntStringLongDouble2Long) == "Int:String:Long:Double2Long"
    result &= FunctionSignature.getSignatureIntStringLongString2Long(helperIntStringLongString2Long) == "Int:String:Long:String2Long"
    result &= FunctionSignature.getSignatureIntStringLongBoolean2Long(helperIntStringLongBoolean2Long) == "Int:String:Long:Boolean2Long"
    result &= FunctionSignature.getSignatureIntStringDoubleInt2Long(helperIntStringDoubleInt2Long) == "Int:String:Double:Int2Long"
    result &= FunctionSignature.getSignatureIntStringDoubleLong2Long(helperIntStringDoubleLong2Long) == "Int:String:Double:Long2Long"
    result &= FunctionSignature.getSignatureIntStringDoubleDouble2Long(helperIntStringDoubleDouble2Long) == "Int:String:Double:Double2Long"
    result &= FunctionSignature.getSignatureIntStringDoubleString2Long(helperIntStringDoubleString2Long) == "Int:String:Double:String2Long"
    result &= FunctionSignature.getSignatureIntStringDoubleBoolean2Long(helperIntStringDoubleBoolean2Long) == "Int:String:Double:Boolean2Long"
    result &= FunctionSignature.getSignatureIntStringStringInt2Long(helperIntStringStringInt2Long) == "Int:String:String:Int2Long"
    result &= FunctionSignature.getSignatureIntStringStringLong2Long(helperIntStringStringLong2Long) == "Int:String:String:Long2Long"
    result &= FunctionSignature.getSignatureIntStringStringDouble2Long(helperIntStringStringDouble2Long) == "Int:String:String:Double2Long"
    result &= FunctionSignature.getSignatureIntStringStringString2Long(helperIntStringStringString2Long) == "Int:String:String:String2Long"
    result &= FunctionSignature.getSignatureIntStringStringBoolean2Long(helperIntStringStringBoolean2Long) == "Int:String:String:Boolean2Long"
    result &= FunctionSignature.getSignatureIntStringBooleanInt2Long(helperIntStringBooleanInt2Long) == "Int:String:Boolean:Int2Long"
    result &= FunctionSignature.getSignatureIntStringBooleanLong2Long(helperIntStringBooleanLong2Long) == "Int:String:Boolean:Long2Long"
    result &= FunctionSignature.getSignatureIntStringBooleanDouble2Long(helperIntStringBooleanDouble2Long) == "Int:String:Boolean:Double2Long"
    result &= FunctionSignature.getSignatureIntStringBooleanString2Long(helperIntStringBooleanString2Long) == "Int:String:Boolean:String2Long"
    result &= FunctionSignature.getSignatureIntStringBooleanBoolean2Long(helperIntStringBooleanBoolean2Long) == "Int:String:Boolean:Boolean2Long"
    result &= FunctionSignature.getSignatureIntBooleanIntInt2Long(helperIntBooleanIntInt2Long) == "Int:Boolean:Int:Int2Long"
    result &= FunctionSignature.getSignatureIntBooleanIntLong2Long(helperIntBooleanIntLong2Long) == "Int:Boolean:Int:Long2Long"
    result &= FunctionSignature.getSignatureIntBooleanIntDouble2Long(helperIntBooleanIntDouble2Long) == "Int:Boolean:Int:Double2Long"
    result &= FunctionSignature.getSignatureIntBooleanIntString2Long(helperIntBooleanIntString2Long) == "Int:Boolean:Int:String2Long"
    result &= FunctionSignature.getSignatureIntBooleanIntBoolean2Long(helperIntBooleanIntBoolean2Long) == "Int:Boolean:Int:Boolean2Long"
    result &= FunctionSignature.getSignatureIntBooleanLongInt2Long(helperIntBooleanLongInt2Long) == "Int:Boolean:Long:Int2Long"
    result &= FunctionSignature.getSignatureIntBooleanLongLong2Long(helperIntBooleanLongLong2Long) == "Int:Boolean:Long:Long2Long"
    result &= FunctionSignature.getSignatureIntBooleanLongDouble2Long(helperIntBooleanLongDouble2Long) == "Int:Boolean:Long:Double2Long"
    result &= FunctionSignature.getSignatureIntBooleanLongString2Long(helperIntBooleanLongString2Long) == "Int:Boolean:Long:String2Long"
    result &= FunctionSignature.getSignatureIntBooleanLongBoolean2Long(helperIntBooleanLongBoolean2Long) == "Int:Boolean:Long:Boolean2Long"
    result &= FunctionSignature.getSignatureIntBooleanDoubleInt2Long(helperIntBooleanDoubleInt2Long) == "Int:Boolean:Double:Int2Long"
    result &= FunctionSignature.getSignatureIntBooleanDoubleLong2Long(helperIntBooleanDoubleLong2Long) == "Int:Boolean:Double:Long2Long"
    result &= FunctionSignature.getSignatureIntBooleanDoubleDouble2Long(helperIntBooleanDoubleDouble2Long) == "Int:Boolean:Double:Double2Long"
    result &= FunctionSignature.getSignatureIntBooleanDoubleString2Long(helperIntBooleanDoubleString2Long) == "Int:Boolean:Double:String2Long"
    result &= FunctionSignature.getSignatureIntBooleanDoubleBoolean2Long(helperIntBooleanDoubleBoolean2Long) == "Int:Boolean:Double:Boolean2Long"
    result &= FunctionSignature.getSignatureIntBooleanStringInt2Long(helperIntBooleanStringInt2Long) == "Int:Boolean:String:Int2Long"
    result &= FunctionSignature.getSignatureIntBooleanStringLong2Long(helperIntBooleanStringLong2Long) == "Int:Boolean:String:Long2Long"
    result &= FunctionSignature.getSignatureIntBooleanStringDouble2Long(helperIntBooleanStringDouble2Long) == "Int:Boolean:String:Double2Long"
    result &= FunctionSignature.getSignatureIntBooleanStringString2Long(helperIntBooleanStringString2Long) == "Int:Boolean:String:String2Long"
    result &= FunctionSignature.getSignatureIntBooleanStringBoolean2Long(helperIntBooleanStringBoolean2Long) == "Int:Boolean:String:Boolean2Long"
    result &= FunctionSignature.getSignatureIntBooleanBooleanInt2Long(helperIntBooleanBooleanInt2Long) == "Int:Boolean:Boolean:Int2Long"
    result &= FunctionSignature.getSignatureIntBooleanBooleanLong2Long(helperIntBooleanBooleanLong2Long) == "Int:Boolean:Boolean:Long2Long"
    result &= FunctionSignature.getSignatureIntBooleanBooleanDouble2Long(helperIntBooleanBooleanDouble2Long) == "Int:Boolean:Boolean:Double2Long"
    result &= FunctionSignature.getSignatureIntBooleanBooleanString2Long(helperIntBooleanBooleanString2Long) == "Int:Boolean:Boolean:String2Long"
    result &= FunctionSignature.getSignatureIntBooleanBooleanBoolean2Long(helperIntBooleanBooleanBoolean2Long) == "Int:Boolean:Boolean:Boolean2Long"
    result &= FunctionSignature.getSignatureLongIntIntInt2Long(helperLongIntIntInt2Long) == "Long:Int:Int:Int2Long"
    result &= FunctionSignature.getSignatureLongIntIntLong2Long(helperLongIntIntLong2Long) == "Long:Int:Int:Long2Long"
    result &= FunctionSignature.getSignatureLongIntIntDouble2Long(helperLongIntIntDouble2Long) == "Long:Int:Int:Double2Long"
    result &= FunctionSignature.getSignatureLongIntIntString2Long(helperLongIntIntString2Long) == "Long:Int:Int:String2Long"
    result &= FunctionSignature.getSignatureLongIntIntBoolean2Long(helperLongIntIntBoolean2Long) == "Long:Int:Int:Boolean2Long"
    result &= FunctionSignature.getSignatureLongIntLongInt2Long(helperLongIntLongInt2Long) == "Long:Int:Long:Int2Long"
    result &= FunctionSignature.getSignatureLongIntLongLong2Long(helperLongIntLongLong2Long) == "Long:Int:Long:Long2Long"
    result &= FunctionSignature.getSignatureLongIntLongDouble2Long(helperLongIntLongDouble2Long) == "Long:Int:Long:Double2Long"
    result &= FunctionSignature.getSignatureLongIntLongString2Long(helperLongIntLongString2Long) == "Long:Int:Long:String2Long"
    result &= FunctionSignature.getSignatureLongIntLongBoolean2Long(helperLongIntLongBoolean2Long) == "Long:Int:Long:Boolean2Long"
    result &= FunctionSignature.getSignatureLongIntDoubleInt2Long(helperLongIntDoubleInt2Long) == "Long:Int:Double:Int2Long"
    result &= FunctionSignature.getSignatureLongIntDoubleLong2Long(helperLongIntDoubleLong2Long) == "Long:Int:Double:Long2Long"
    result &= FunctionSignature.getSignatureLongIntDoubleDouble2Long(helperLongIntDoubleDouble2Long) == "Long:Int:Double:Double2Long"
    result &= FunctionSignature.getSignatureLongIntDoubleString2Long(helperLongIntDoubleString2Long) == "Long:Int:Double:String2Long"
    result &= FunctionSignature.getSignatureLongIntDoubleBoolean2Long(helperLongIntDoubleBoolean2Long) == "Long:Int:Double:Boolean2Long"
    result &= FunctionSignature.getSignatureLongIntStringInt2Long(helperLongIntStringInt2Long) == "Long:Int:String:Int2Long"
    result &= FunctionSignature.getSignatureLongIntStringLong2Long(helperLongIntStringLong2Long) == "Long:Int:String:Long2Long"
    result &= FunctionSignature.getSignatureLongIntStringDouble2Long(helperLongIntStringDouble2Long) == "Long:Int:String:Double2Long"
    result &= FunctionSignature.getSignatureLongIntStringString2Long(helperLongIntStringString2Long) == "Long:Int:String:String2Long"
    result &= FunctionSignature.getSignatureLongIntStringBoolean2Long(helperLongIntStringBoolean2Long) == "Long:Int:String:Boolean2Long"
    result &= FunctionSignature.getSignatureLongIntBooleanInt2Long(helperLongIntBooleanInt2Long) == "Long:Int:Boolean:Int2Long"
    result &= FunctionSignature.getSignatureLongIntBooleanLong2Long(helperLongIntBooleanLong2Long) == "Long:Int:Boolean:Long2Long"
    result &= FunctionSignature.getSignatureLongIntBooleanDouble2Long(helperLongIntBooleanDouble2Long) == "Long:Int:Boolean:Double2Long"
    result &= FunctionSignature.getSignatureLongIntBooleanString2Long(helperLongIntBooleanString2Long) == "Long:Int:Boolean:String2Long"
    result &= FunctionSignature.getSignatureLongIntBooleanBoolean2Long(helperLongIntBooleanBoolean2Long) == "Long:Int:Boolean:Boolean2Long"
    result &= FunctionSignature.getSignatureLongLongIntInt2Long(helperLongLongIntInt2Long) == "Long:Long:Int:Int2Long"
    result &= FunctionSignature.getSignatureLongLongIntLong2Long(helperLongLongIntLong2Long) == "Long:Long:Int:Long2Long"
    result &= FunctionSignature.getSignatureLongLongIntDouble2Long(helperLongLongIntDouble2Long) == "Long:Long:Int:Double2Long"
    result &= FunctionSignature.getSignatureLongLongIntString2Long(helperLongLongIntString2Long) == "Long:Long:Int:String2Long"
    result &= FunctionSignature.getSignatureLongLongIntBoolean2Long(helperLongLongIntBoolean2Long) == "Long:Long:Int:Boolean2Long"
    result &= FunctionSignature.getSignatureLongLongLongInt2Long(helperLongLongLongInt2Long) == "Long:Long:Long:Int2Long"
    result &= FunctionSignature.getSignatureLongLongLongLong2Long(helperLongLongLongLong2Long) == "Long:Long:Long:Long2Long"
    result &= FunctionSignature.getSignatureLongLongLongDouble2Long(helperLongLongLongDouble2Long) == "Long:Long:Long:Double2Long"
    result &= FunctionSignature.getSignatureLongLongLongString2Long(helperLongLongLongString2Long) == "Long:Long:Long:String2Long"
    result &= FunctionSignature.getSignatureLongLongLongBoolean2Long(helperLongLongLongBoolean2Long) == "Long:Long:Long:Boolean2Long"
    result &= FunctionSignature.getSignatureLongLongDoubleInt2Long(helperLongLongDoubleInt2Long) == "Long:Long:Double:Int2Long"
    result &= FunctionSignature.getSignatureLongLongDoubleLong2Long(helperLongLongDoubleLong2Long) == "Long:Long:Double:Long2Long"
    result &= FunctionSignature.getSignatureLongLongDoubleDouble2Long(helperLongLongDoubleDouble2Long) == "Long:Long:Double:Double2Long"
    result &= FunctionSignature.getSignatureLongLongDoubleString2Long(helperLongLongDoubleString2Long) == "Long:Long:Double:String2Long"
    result &= FunctionSignature.getSignatureLongLongDoubleBoolean2Long(helperLongLongDoubleBoolean2Long) == "Long:Long:Double:Boolean2Long"
    result &= FunctionSignature.getSignatureLongLongStringInt2Long(helperLongLongStringInt2Long) == "Long:Long:String:Int2Long"
    result &= FunctionSignature.getSignatureLongLongStringLong2Long(helperLongLongStringLong2Long) == "Long:Long:String:Long2Long"
    result &= FunctionSignature.getSignatureLongLongStringDouble2Long(helperLongLongStringDouble2Long) == "Long:Long:String:Double2Long"
    result &= FunctionSignature.getSignatureLongLongStringString2Long(helperLongLongStringString2Long) == "Long:Long:String:String2Long"
    result &= FunctionSignature.getSignatureLongLongStringBoolean2Long(helperLongLongStringBoolean2Long) == "Long:Long:String:Boolean2Long"
    result &= FunctionSignature.getSignatureLongLongBooleanInt2Long(helperLongLongBooleanInt2Long) == "Long:Long:Boolean:Int2Long"
    result &= FunctionSignature.getSignatureLongLongBooleanLong2Long(helperLongLongBooleanLong2Long) == "Long:Long:Boolean:Long2Long"
    result &= FunctionSignature.getSignatureLongLongBooleanDouble2Long(helperLongLongBooleanDouble2Long) == "Long:Long:Boolean:Double2Long"
    result &= FunctionSignature.getSignatureLongLongBooleanString2Long(helperLongLongBooleanString2Long) == "Long:Long:Boolean:String2Long"
    result &= FunctionSignature.getSignatureLongLongBooleanBoolean2Long(helperLongLongBooleanBoolean2Long) == "Long:Long:Boolean:Boolean2Long"
    result &= FunctionSignature.getSignatureLongDoubleIntInt2Long(helperLongDoubleIntInt2Long) == "Long:Double:Int:Int2Long"
    result &= FunctionSignature.getSignatureLongDoubleIntLong2Long(helperLongDoubleIntLong2Long) == "Long:Double:Int:Long2Long"
    result &= FunctionSignature.getSignatureLongDoubleIntDouble2Long(helperLongDoubleIntDouble2Long) == "Long:Double:Int:Double2Long"
    result &= FunctionSignature.getSignatureLongDoubleIntString2Long(helperLongDoubleIntString2Long) == "Long:Double:Int:String2Long"
    result &= FunctionSignature.getSignatureLongDoubleIntBoolean2Long(helperLongDoubleIntBoolean2Long) == "Long:Double:Int:Boolean2Long"
    result &= FunctionSignature.getSignatureLongDoubleLongInt2Long(helperLongDoubleLongInt2Long) == "Long:Double:Long:Int2Long"
    result &= FunctionSignature.getSignatureLongDoubleLongLong2Long(helperLongDoubleLongLong2Long) == "Long:Double:Long:Long2Long"
    result &= FunctionSignature.getSignatureLongDoubleLongDouble2Long(helperLongDoubleLongDouble2Long) == "Long:Double:Long:Double2Long"
    result &= FunctionSignature.getSignatureLongDoubleLongString2Long(helperLongDoubleLongString2Long) == "Long:Double:Long:String2Long"
    result &= FunctionSignature.getSignatureLongDoubleLongBoolean2Long(helperLongDoubleLongBoolean2Long) == "Long:Double:Long:Boolean2Long"
    result &= FunctionSignature.getSignatureLongDoubleDoubleInt2Long(helperLongDoubleDoubleInt2Long) == "Long:Double:Double:Int2Long"
    result &= FunctionSignature.getSignatureLongDoubleDoubleLong2Long(helperLongDoubleDoubleLong2Long) == "Long:Double:Double:Long2Long"
    result &= FunctionSignature.getSignatureLongDoubleDoubleDouble2Long(helperLongDoubleDoubleDouble2Long) == "Long:Double:Double:Double2Long"
    result &= FunctionSignature.getSignatureLongDoubleDoubleString2Long(helperLongDoubleDoubleString2Long) == "Long:Double:Double:String2Long"
    result &= FunctionSignature.getSignatureLongDoubleDoubleBoolean2Long(helperLongDoubleDoubleBoolean2Long) == "Long:Double:Double:Boolean2Long"
    result &= FunctionSignature.getSignatureLongDoubleStringInt2Long(helperLongDoubleStringInt2Long) == "Long:Double:String:Int2Long"
    result &= FunctionSignature.getSignatureLongDoubleStringLong2Long(helperLongDoubleStringLong2Long) == "Long:Double:String:Long2Long"
    result &= FunctionSignature.getSignatureLongDoubleStringDouble2Long(helperLongDoubleStringDouble2Long) == "Long:Double:String:Double2Long"
    result &= FunctionSignature.getSignatureLongDoubleStringString2Long(helperLongDoubleStringString2Long) == "Long:Double:String:String2Long"
    result &= FunctionSignature.getSignatureLongDoubleStringBoolean2Long(helperLongDoubleStringBoolean2Long) == "Long:Double:String:Boolean2Long"
    result &= FunctionSignature.getSignatureLongDoubleBooleanInt2Long(helperLongDoubleBooleanInt2Long) == "Long:Double:Boolean:Int2Long"
    result &= FunctionSignature.getSignatureLongDoubleBooleanLong2Long(helperLongDoubleBooleanLong2Long) == "Long:Double:Boolean:Long2Long"
    result &= FunctionSignature.getSignatureLongDoubleBooleanDouble2Long(helperLongDoubleBooleanDouble2Long) == "Long:Double:Boolean:Double2Long"
    result &= FunctionSignature.getSignatureLongDoubleBooleanString2Long(helperLongDoubleBooleanString2Long) == "Long:Double:Boolean:String2Long"
    result &= FunctionSignature.getSignatureLongDoubleBooleanBoolean2Long(helperLongDoubleBooleanBoolean2Long) == "Long:Double:Boolean:Boolean2Long"
    result &= FunctionSignature.getSignatureLongStringIntInt2Long(helperLongStringIntInt2Long) == "Long:String:Int:Int2Long"
    result &= FunctionSignature.getSignatureLongStringIntLong2Long(helperLongStringIntLong2Long) == "Long:String:Int:Long2Long"
    result &= FunctionSignature.getSignatureLongStringIntDouble2Long(helperLongStringIntDouble2Long) == "Long:String:Int:Double2Long"
    result &= FunctionSignature.getSignatureLongStringIntString2Long(helperLongStringIntString2Long) == "Long:String:Int:String2Long"
    result &= FunctionSignature.getSignatureLongStringIntBoolean2Long(helperLongStringIntBoolean2Long) == "Long:String:Int:Boolean2Long"
    result &= FunctionSignature.getSignatureLongStringLongInt2Long(helperLongStringLongInt2Long) == "Long:String:Long:Int2Long"
    result &= FunctionSignature.getSignatureLongStringLongLong2Long(helperLongStringLongLong2Long) == "Long:String:Long:Long2Long"
    result &= FunctionSignature.getSignatureLongStringLongDouble2Long(helperLongStringLongDouble2Long) == "Long:String:Long:Double2Long"
    result &= FunctionSignature.getSignatureLongStringLongString2Long(helperLongStringLongString2Long) == "Long:String:Long:String2Long"
    result &= FunctionSignature.getSignatureLongStringLongBoolean2Long(helperLongStringLongBoolean2Long) == "Long:String:Long:Boolean2Long"
    result &= FunctionSignature.getSignatureLongStringDoubleInt2Long(helperLongStringDoubleInt2Long) == "Long:String:Double:Int2Long"
    result &= FunctionSignature.getSignatureLongStringDoubleLong2Long(helperLongStringDoubleLong2Long) == "Long:String:Double:Long2Long"
    result &= FunctionSignature.getSignatureLongStringDoubleDouble2Long(helperLongStringDoubleDouble2Long) == "Long:String:Double:Double2Long"
    result &= FunctionSignature.getSignatureLongStringDoubleString2Long(helperLongStringDoubleString2Long) == "Long:String:Double:String2Long"
    result &= FunctionSignature.getSignatureLongStringDoubleBoolean2Long(helperLongStringDoubleBoolean2Long) == "Long:String:Double:Boolean2Long"
    result &= FunctionSignature.getSignatureLongStringStringInt2Long(helperLongStringStringInt2Long) == "Long:String:String:Int2Long"
    result &= FunctionSignature.getSignatureLongStringStringLong2Long(helperLongStringStringLong2Long) == "Long:String:String:Long2Long"
    result &= FunctionSignature.getSignatureLongStringStringDouble2Long(helperLongStringStringDouble2Long) == "Long:String:String:Double2Long"
    result &= FunctionSignature.getSignatureLongStringStringString2Long(helperLongStringStringString2Long) == "Long:String:String:String2Long"
    result &= FunctionSignature.getSignatureLongStringStringBoolean2Long(helperLongStringStringBoolean2Long) == "Long:String:String:Boolean2Long"
    result &= FunctionSignature.getSignatureLongStringBooleanInt2Long(helperLongStringBooleanInt2Long) == "Long:String:Boolean:Int2Long"
    result &= FunctionSignature.getSignatureLongStringBooleanLong2Long(helperLongStringBooleanLong2Long) == "Long:String:Boolean:Long2Long"
    result &= FunctionSignature.getSignatureLongStringBooleanDouble2Long(helperLongStringBooleanDouble2Long) == "Long:String:Boolean:Double2Long"
    result &= FunctionSignature.getSignatureLongStringBooleanString2Long(helperLongStringBooleanString2Long) == "Long:String:Boolean:String2Long"
    result &= FunctionSignature.getSignatureLongStringBooleanBoolean2Long(helperLongStringBooleanBoolean2Long) == "Long:String:Boolean:Boolean2Long"
    result &= FunctionSignature.getSignatureLongBooleanIntInt2Long(helperLongBooleanIntInt2Long) == "Long:Boolean:Int:Int2Long"
    result &= FunctionSignature.getSignatureLongBooleanIntLong2Long(helperLongBooleanIntLong2Long) == "Long:Boolean:Int:Long2Long"
    result &= FunctionSignature.getSignatureLongBooleanIntDouble2Long(helperLongBooleanIntDouble2Long) == "Long:Boolean:Int:Double2Long"
    result &= FunctionSignature.getSignatureLongBooleanIntString2Long(helperLongBooleanIntString2Long) == "Long:Boolean:Int:String2Long"
    result &= FunctionSignature.getSignatureLongBooleanIntBoolean2Long(helperLongBooleanIntBoolean2Long) == "Long:Boolean:Int:Boolean2Long"
    result &= FunctionSignature.getSignatureLongBooleanLongInt2Long(helperLongBooleanLongInt2Long) == "Long:Boolean:Long:Int2Long"
    result &= FunctionSignature.getSignatureLongBooleanLongLong2Long(helperLongBooleanLongLong2Long) == "Long:Boolean:Long:Long2Long"
    result &= FunctionSignature.getSignatureLongBooleanLongDouble2Long(helperLongBooleanLongDouble2Long) == "Long:Boolean:Long:Double2Long"
    result &= FunctionSignature.getSignatureLongBooleanLongString2Long(helperLongBooleanLongString2Long) == "Long:Boolean:Long:String2Long"
    result &= FunctionSignature.getSignatureLongBooleanLongBoolean2Long(helperLongBooleanLongBoolean2Long) == "Long:Boolean:Long:Boolean2Long"
    result &= FunctionSignature.getSignatureLongBooleanDoubleInt2Long(helperLongBooleanDoubleInt2Long) == "Long:Boolean:Double:Int2Long"
    result &= FunctionSignature.getSignatureLongBooleanDoubleLong2Long(helperLongBooleanDoubleLong2Long) == "Long:Boolean:Double:Long2Long"
    result &= FunctionSignature.getSignatureLongBooleanDoubleDouble2Long(helperLongBooleanDoubleDouble2Long) == "Long:Boolean:Double:Double2Long"
    result &= FunctionSignature.getSignatureLongBooleanDoubleString2Long(helperLongBooleanDoubleString2Long) == "Long:Boolean:Double:String2Long"
    result &= FunctionSignature.getSignatureLongBooleanDoubleBoolean2Long(helperLongBooleanDoubleBoolean2Long) == "Long:Boolean:Double:Boolean2Long"
    result &= FunctionSignature.getSignatureLongBooleanStringInt2Long(helperLongBooleanStringInt2Long) == "Long:Boolean:String:Int2Long"
    result &= FunctionSignature.getSignatureLongBooleanStringLong2Long(helperLongBooleanStringLong2Long) == "Long:Boolean:String:Long2Long"
    result &= FunctionSignature.getSignatureLongBooleanStringDouble2Long(helperLongBooleanStringDouble2Long) == "Long:Boolean:String:Double2Long"
    result &= FunctionSignature.getSignatureLongBooleanStringString2Long(helperLongBooleanStringString2Long) == "Long:Boolean:String:String2Long"
    result &= FunctionSignature.getSignatureLongBooleanStringBoolean2Long(helperLongBooleanStringBoolean2Long) == "Long:Boolean:String:Boolean2Long"
    result &= FunctionSignature.getSignatureLongBooleanBooleanInt2Long(helperLongBooleanBooleanInt2Long) == "Long:Boolean:Boolean:Int2Long"
    result &= FunctionSignature.getSignatureLongBooleanBooleanLong2Long(helperLongBooleanBooleanLong2Long) == "Long:Boolean:Boolean:Long2Long"
    result &= FunctionSignature.getSignatureLongBooleanBooleanDouble2Long(helperLongBooleanBooleanDouble2Long) == "Long:Boolean:Boolean:Double2Long"
    result &= FunctionSignature.getSignatureLongBooleanBooleanString2Long(helperLongBooleanBooleanString2Long) == "Long:Boolean:Boolean:String2Long"
    result &= FunctionSignature.getSignatureLongBooleanBooleanBoolean2Long(helperLongBooleanBooleanBoolean2Long) == "Long:Boolean:Boolean:Boolean2Long"
    result &= FunctionSignature.getSignatureDoubleIntIntInt2Long(helperDoubleIntIntInt2Long) == "Double:Int:Int:Int2Long"
    result &= FunctionSignature.getSignatureDoubleIntIntLong2Long(helperDoubleIntIntLong2Long) == "Double:Int:Int:Long2Long"
    result &= FunctionSignature.getSignatureDoubleIntIntDouble2Long(helperDoubleIntIntDouble2Long) == "Double:Int:Int:Double2Long"
    result &= FunctionSignature.getSignatureDoubleIntIntString2Long(helperDoubleIntIntString2Long) == "Double:Int:Int:String2Long"
    result &= FunctionSignature.getSignatureDoubleIntIntBoolean2Long(helperDoubleIntIntBoolean2Long) == "Double:Int:Int:Boolean2Long"
    result &= FunctionSignature.getSignatureDoubleIntLongInt2Long(helperDoubleIntLongInt2Long) == "Double:Int:Long:Int2Long"
    result &= FunctionSignature.getSignatureDoubleIntLongLong2Long(helperDoubleIntLongLong2Long) == "Double:Int:Long:Long2Long"
    result &= FunctionSignature.getSignatureDoubleIntLongDouble2Long(helperDoubleIntLongDouble2Long) == "Double:Int:Long:Double2Long"
    result &= FunctionSignature.getSignatureDoubleIntLongString2Long(helperDoubleIntLongString2Long) == "Double:Int:Long:String2Long"
    result &= FunctionSignature.getSignatureDoubleIntLongBoolean2Long(helperDoubleIntLongBoolean2Long) == "Double:Int:Long:Boolean2Long"
    result &= FunctionSignature.getSignatureDoubleIntDoubleInt2Long(helperDoubleIntDoubleInt2Long) == "Double:Int:Double:Int2Long"
    result &= FunctionSignature.getSignatureDoubleIntDoubleLong2Long(helperDoubleIntDoubleLong2Long) == "Double:Int:Double:Long2Long"
    result &= FunctionSignature.getSignatureDoubleIntDoubleDouble2Long(helperDoubleIntDoubleDouble2Long) == "Double:Int:Double:Double2Long"
    result &= FunctionSignature.getSignatureDoubleIntDoubleString2Long(helperDoubleIntDoubleString2Long) == "Double:Int:Double:String2Long"
    result &= FunctionSignature.getSignatureDoubleIntDoubleBoolean2Long(helperDoubleIntDoubleBoolean2Long) == "Double:Int:Double:Boolean2Long"
    result &= FunctionSignature.getSignatureDoubleIntStringInt2Long(helperDoubleIntStringInt2Long) == "Double:Int:String:Int2Long"
    result &= FunctionSignature.getSignatureDoubleIntStringLong2Long(helperDoubleIntStringLong2Long) == "Double:Int:String:Long2Long"
    result &= FunctionSignature.getSignatureDoubleIntStringDouble2Long(helperDoubleIntStringDouble2Long) == "Double:Int:String:Double2Long"
    result &= FunctionSignature.getSignatureDoubleIntStringString2Long(helperDoubleIntStringString2Long) == "Double:Int:String:String2Long"
    result &= FunctionSignature.getSignatureDoubleIntStringBoolean2Long(helperDoubleIntStringBoolean2Long) == "Double:Int:String:Boolean2Long"
    result &= FunctionSignature.getSignatureDoubleIntBooleanInt2Long(helperDoubleIntBooleanInt2Long) == "Double:Int:Boolean:Int2Long"
    result &= FunctionSignature.getSignatureDoubleIntBooleanLong2Long(helperDoubleIntBooleanLong2Long) == "Double:Int:Boolean:Long2Long"
    result &= FunctionSignature.getSignatureDoubleIntBooleanDouble2Long(helperDoubleIntBooleanDouble2Long) == "Double:Int:Boolean:Double2Long"
    result &= FunctionSignature.getSignatureDoubleIntBooleanString2Long(helperDoubleIntBooleanString2Long) == "Double:Int:Boolean:String2Long"
    result &= FunctionSignature.getSignatureDoubleIntBooleanBoolean2Long(helperDoubleIntBooleanBoolean2Long) == "Double:Int:Boolean:Boolean2Long"
    result &= FunctionSignature.getSignatureDoubleLongIntInt2Long(helperDoubleLongIntInt2Long) == "Double:Long:Int:Int2Long"
    result &= FunctionSignature.getSignatureDoubleLongIntLong2Long(helperDoubleLongIntLong2Long) == "Double:Long:Int:Long2Long"
    result &= FunctionSignature.getSignatureDoubleLongIntDouble2Long(helperDoubleLongIntDouble2Long) == "Double:Long:Int:Double2Long"
    result &= FunctionSignature.getSignatureDoubleLongIntString2Long(helperDoubleLongIntString2Long) == "Double:Long:Int:String2Long"
    result &= FunctionSignature.getSignatureDoubleLongIntBoolean2Long(helperDoubleLongIntBoolean2Long) == "Double:Long:Int:Boolean2Long"
    result &= FunctionSignature.getSignatureDoubleLongLongInt2Long(helperDoubleLongLongInt2Long) == "Double:Long:Long:Int2Long"
    result &= FunctionSignature.getSignatureDoubleLongLongLong2Long(helperDoubleLongLongLong2Long) == "Double:Long:Long:Long2Long"
    result &= FunctionSignature.getSignatureDoubleLongLongDouble2Long(helperDoubleLongLongDouble2Long) == "Double:Long:Long:Double2Long"
    result &= FunctionSignature.getSignatureDoubleLongLongString2Long(helperDoubleLongLongString2Long) == "Double:Long:Long:String2Long"
    result &= FunctionSignature.getSignatureDoubleLongLongBoolean2Long(helperDoubleLongLongBoolean2Long) == "Double:Long:Long:Boolean2Long"
    result &= FunctionSignature.getSignatureDoubleLongDoubleInt2Long(helperDoubleLongDoubleInt2Long) == "Double:Long:Double:Int2Long"
    result &= FunctionSignature.getSignatureDoubleLongDoubleLong2Long(helperDoubleLongDoubleLong2Long) == "Double:Long:Double:Long2Long"
    result &= FunctionSignature.getSignatureDoubleLongDoubleDouble2Long(helperDoubleLongDoubleDouble2Long) == "Double:Long:Double:Double2Long"
    result &= FunctionSignature.getSignatureDoubleLongDoubleString2Long(helperDoubleLongDoubleString2Long) == "Double:Long:Double:String2Long"
    result &= FunctionSignature.getSignatureDoubleLongDoubleBoolean2Long(helperDoubleLongDoubleBoolean2Long) == "Double:Long:Double:Boolean2Long"
    result &= FunctionSignature.getSignatureDoubleLongStringInt2Long(helperDoubleLongStringInt2Long) == "Double:Long:String:Int2Long"
    result &= FunctionSignature.getSignatureDoubleLongStringLong2Long(helperDoubleLongStringLong2Long) == "Double:Long:String:Long2Long"
    result &= FunctionSignature.getSignatureDoubleLongStringDouble2Long(helperDoubleLongStringDouble2Long) == "Double:Long:String:Double2Long"
    result &= FunctionSignature.getSignatureDoubleLongStringString2Long(helperDoubleLongStringString2Long) == "Double:Long:String:String2Long"
    result &= FunctionSignature.getSignatureDoubleLongStringBoolean2Long(helperDoubleLongStringBoolean2Long) == "Double:Long:String:Boolean2Long"
    result &= FunctionSignature.getSignatureDoubleLongBooleanInt2Long(helperDoubleLongBooleanInt2Long) == "Double:Long:Boolean:Int2Long"
    result &= FunctionSignature.getSignatureDoubleLongBooleanLong2Long(helperDoubleLongBooleanLong2Long) == "Double:Long:Boolean:Long2Long"
    result &= FunctionSignature.getSignatureDoubleLongBooleanDouble2Long(helperDoubleLongBooleanDouble2Long) == "Double:Long:Boolean:Double2Long"
    result &= FunctionSignature.getSignatureDoubleLongBooleanString2Long(helperDoubleLongBooleanString2Long) == "Double:Long:Boolean:String2Long"
    result &= FunctionSignature.getSignatureDoubleLongBooleanBoolean2Long(helperDoubleLongBooleanBoolean2Long) == "Double:Long:Boolean:Boolean2Long"
    result &= FunctionSignature.getSignatureDoubleDoubleIntInt2Long(helperDoubleDoubleIntInt2Long) == "Double:Double:Int:Int2Long"
    result &= FunctionSignature.getSignatureDoubleDoubleIntLong2Long(helperDoubleDoubleIntLong2Long) == "Double:Double:Int:Long2Long"
    result &= FunctionSignature.getSignatureDoubleDoubleIntDouble2Long(helperDoubleDoubleIntDouble2Long) == "Double:Double:Int:Double2Long"
    result &= FunctionSignature.getSignatureDoubleDoubleIntString2Long(helperDoubleDoubleIntString2Long) == "Double:Double:Int:String2Long"
    result &= FunctionSignature.getSignatureDoubleDoubleIntBoolean2Long(helperDoubleDoubleIntBoolean2Long) == "Double:Double:Int:Boolean2Long"
    result &= FunctionSignature.getSignatureDoubleDoubleLongInt2Long(helperDoubleDoubleLongInt2Long) == "Double:Double:Long:Int2Long"
    result &= FunctionSignature.getSignatureDoubleDoubleLongLong2Long(helperDoubleDoubleLongLong2Long) == "Double:Double:Long:Long2Long"
    result &= FunctionSignature.getSignatureDoubleDoubleLongDouble2Long(helperDoubleDoubleLongDouble2Long) == "Double:Double:Long:Double2Long"
    result &= FunctionSignature.getSignatureDoubleDoubleLongString2Long(helperDoubleDoubleLongString2Long) == "Double:Double:Long:String2Long"
    result &= FunctionSignature.getSignatureDoubleDoubleLongBoolean2Long(helperDoubleDoubleLongBoolean2Long) == "Double:Double:Long:Boolean2Long"
    result &= FunctionSignature.getSignatureDoubleDoubleDoubleInt2Long(helperDoubleDoubleDoubleInt2Long) == "Double:Double:Double:Int2Long"
    result &= FunctionSignature.getSignatureDoubleDoubleDoubleLong2Long(helperDoubleDoubleDoubleLong2Long) == "Double:Double:Double:Long2Long"
    result &= FunctionSignature.getSignatureDoubleDoubleDoubleDouble2Long(helperDoubleDoubleDoubleDouble2Long) == "Double:Double:Double:Double2Long"
    result &= FunctionSignature.getSignatureDoubleDoubleDoubleString2Long(helperDoubleDoubleDoubleString2Long) == "Double:Double:Double:String2Long"
    result &= FunctionSignature.getSignatureDoubleDoubleDoubleBoolean2Long(helperDoubleDoubleDoubleBoolean2Long) == "Double:Double:Double:Boolean2Long"
    result &= FunctionSignature.getSignatureDoubleDoubleStringInt2Long(helperDoubleDoubleStringInt2Long) == "Double:Double:String:Int2Long"
    result &= FunctionSignature.getSignatureDoubleDoubleStringLong2Long(helperDoubleDoubleStringLong2Long) == "Double:Double:String:Long2Long"
    result &= FunctionSignature.getSignatureDoubleDoubleStringDouble2Long(helperDoubleDoubleStringDouble2Long) == "Double:Double:String:Double2Long"
    result &= FunctionSignature.getSignatureDoubleDoubleStringString2Long(helperDoubleDoubleStringString2Long) == "Double:Double:String:String2Long"
    result &= FunctionSignature.getSignatureDoubleDoubleStringBoolean2Long(helperDoubleDoubleStringBoolean2Long) == "Double:Double:String:Boolean2Long"
    result &= FunctionSignature.getSignatureDoubleDoubleBooleanInt2Long(helperDoubleDoubleBooleanInt2Long) == "Double:Double:Boolean:Int2Long"
    result &= FunctionSignature.getSignatureDoubleDoubleBooleanLong2Long(helperDoubleDoubleBooleanLong2Long) == "Double:Double:Boolean:Long2Long"
    result &= FunctionSignature.getSignatureDoubleDoubleBooleanDouble2Long(helperDoubleDoubleBooleanDouble2Long) == "Double:Double:Boolean:Double2Long"
    result &= FunctionSignature.getSignatureDoubleDoubleBooleanString2Long(helperDoubleDoubleBooleanString2Long) == "Double:Double:Boolean:String2Long"
    result &= FunctionSignature.getSignatureDoubleDoubleBooleanBoolean2Long(helperDoubleDoubleBooleanBoolean2Long) == "Double:Double:Boolean:Boolean2Long"
    result &= FunctionSignature.getSignatureDoubleStringIntInt2Long(helperDoubleStringIntInt2Long) == "Double:String:Int:Int2Long"
    result &= FunctionSignature.getSignatureDoubleStringIntLong2Long(helperDoubleStringIntLong2Long) == "Double:String:Int:Long2Long"
    result &= FunctionSignature.getSignatureDoubleStringIntDouble2Long(helperDoubleStringIntDouble2Long) == "Double:String:Int:Double2Long"
    result &= FunctionSignature.getSignatureDoubleStringIntString2Long(helperDoubleStringIntString2Long) == "Double:String:Int:String2Long"
    result &= FunctionSignature.getSignatureDoubleStringIntBoolean2Long(helperDoubleStringIntBoolean2Long) == "Double:String:Int:Boolean2Long"
    result &= FunctionSignature.getSignatureDoubleStringLongInt2Long(helperDoubleStringLongInt2Long) == "Double:String:Long:Int2Long"
    result &= FunctionSignature.getSignatureDoubleStringLongLong2Long(helperDoubleStringLongLong2Long) == "Double:String:Long:Long2Long"
    result &= FunctionSignature.getSignatureDoubleStringLongDouble2Long(helperDoubleStringLongDouble2Long) == "Double:String:Long:Double2Long"
    result &= FunctionSignature.getSignatureDoubleStringLongString2Long(helperDoubleStringLongString2Long) == "Double:String:Long:String2Long"
    result &= FunctionSignature.getSignatureDoubleStringLongBoolean2Long(helperDoubleStringLongBoolean2Long) == "Double:String:Long:Boolean2Long"
    result &= FunctionSignature.getSignatureDoubleStringDoubleInt2Long(helperDoubleStringDoubleInt2Long) == "Double:String:Double:Int2Long"
    result &= FunctionSignature.getSignatureDoubleStringDoubleLong2Long(helperDoubleStringDoubleLong2Long) == "Double:String:Double:Long2Long"
    result &= FunctionSignature.getSignatureDoubleStringDoubleDouble2Long(helperDoubleStringDoubleDouble2Long) == "Double:String:Double:Double2Long"
    result &= FunctionSignature.getSignatureDoubleStringDoubleString2Long(helperDoubleStringDoubleString2Long) == "Double:String:Double:String2Long"
    result &= FunctionSignature.getSignatureDoubleStringDoubleBoolean2Long(helperDoubleStringDoubleBoolean2Long) == "Double:String:Double:Boolean2Long"
    result &= FunctionSignature.getSignatureDoubleStringStringInt2Long(helperDoubleStringStringInt2Long) == "Double:String:String:Int2Long"
    result &= FunctionSignature.getSignatureDoubleStringStringLong2Long(helperDoubleStringStringLong2Long) == "Double:String:String:Long2Long"
    result &= FunctionSignature.getSignatureDoubleStringStringDouble2Long(helperDoubleStringStringDouble2Long) == "Double:String:String:Double2Long"
    result &= FunctionSignature.getSignatureDoubleStringStringString2Long(helperDoubleStringStringString2Long) == "Double:String:String:String2Long"
    result &= FunctionSignature.getSignatureDoubleStringStringBoolean2Long(helperDoubleStringStringBoolean2Long) == "Double:String:String:Boolean2Long"
    result &= FunctionSignature.getSignatureDoubleStringBooleanInt2Long(helperDoubleStringBooleanInt2Long) == "Double:String:Boolean:Int2Long"
    result &= FunctionSignature.getSignatureDoubleStringBooleanLong2Long(helperDoubleStringBooleanLong2Long) == "Double:String:Boolean:Long2Long"
    result &= FunctionSignature.getSignatureDoubleStringBooleanDouble2Long(helperDoubleStringBooleanDouble2Long) == "Double:String:Boolean:Double2Long"
    result &= FunctionSignature.getSignatureDoubleStringBooleanString2Long(helperDoubleStringBooleanString2Long) == "Double:String:Boolean:String2Long"
    result &= FunctionSignature.getSignatureDoubleStringBooleanBoolean2Long(helperDoubleStringBooleanBoolean2Long) == "Double:String:Boolean:Boolean2Long"
    result &= FunctionSignature.getSignatureDoubleBooleanIntInt2Long(helperDoubleBooleanIntInt2Long) == "Double:Boolean:Int:Int2Long"
    result &= FunctionSignature.getSignatureDoubleBooleanIntLong2Long(helperDoubleBooleanIntLong2Long) == "Double:Boolean:Int:Long2Long"
    result &= FunctionSignature.getSignatureDoubleBooleanIntDouble2Long(helperDoubleBooleanIntDouble2Long) == "Double:Boolean:Int:Double2Long"
    result &= FunctionSignature.getSignatureDoubleBooleanIntString2Long(helperDoubleBooleanIntString2Long) == "Double:Boolean:Int:String2Long"
    result &= FunctionSignature.getSignatureDoubleBooleanIntBoolean2Long(helperDoubleBooleanIntBoolean2Long) == "Double:Boolean:Int:Boolean2Long"
    result &= FunctionSignature.getSignatureDoubleBooleanLongInt2Long(helperDoubleBooleanLongInt2Long) == "Double:Boolean:Long:Int2Long"
    result &= FunctionSignature.getSignatureDoubleBooleanLongLong2Long(helperDoubleBooleanLongLong2Long) == "Double:Boolean:Long:Long2Long"
    result &= FunctionSignature.getSignatureDoubleBooleanLongDouble2Long(helperDoubleBooleanLongDouble2Long) == "Double:Boolean:Long:Double2Long"
    result &= FunctionSignature.getSignatureDoubleBooleanLongString2Long(helperDoubleBooleanLongString2Long) == "Double:Boolean:Long:String2Long"
    result &= FunctionSignature.getSignatureDoubleBooleanLongBoolean2Long(helperDoubleBooleanLongBoolean2Long) == "Double:Boolean:Long:Boolean2Long"
    result &= FunctionSignature.getSignatureDoubleBooleanDoubleInt2Long(helperDoubleBooleanDoubleInt2Long) == "Double:Boolean:Double:Int2Long"
    result &= FunctionSignature.getSignatureDoubleBooleanDoubleLong2Long(helperDoubleBooleanDoubleLong2Long) == "Double:Boolean:Double:Long2Long"
    result &= FunctionSignature.getSignatureDoubleBooleanDoubleDouble2Long(helperDoubleBooleanDoubleDouble2Long) == "Double:Boolean:Double:Double2Long"
    result &= FunctionSignature.getSignatureDoubleBooleanDoubleString2Long(helperDoubleBooleanDoubleString2Long) == "Double:Boolean:Double:String2Long"
    result &= FunctionSignature.getSignatureDoubleBooleanDoubleBoolean2Long(helperDoubleBooleanDoubleBoolean2Long) == "Double:Boolean:Double:Boolean2Long"
    result &= FunctionSignature.getSignatureDoubleBooleanStringInt2Long(helperDoubleBooleanStringInt2Long) == "Double:Boolean:String:Int2Long"
    result &= FunctionSignature.getSignatureDoubleBooleanStringLong2Long(helperDoubleBooleanStringLong2Long) == "Double:Boolean:String:Long2Long"
    result &= FunctionSignature.getSignatureDoubleBooleanStringDouble2Long(helperDoubleBooleanStringDouble2Long) == "Double:Boolean:String:Double2Long"
    result &= FunctionSignature.getSignatureDoubleBooleanStringString2Long(helperDoubleBooleanStringString2Long) == "Double:Boolean:String:String2Long"
    result &= FunctionSignature.getSignatureDoubleBooleanStringBoolean2Long(helperDoubleBooleanStringBoolean2Long) == "Double:Boolean:String:Boolean2Long"
    result &= FunctionSignature.getSignatureDoubleBooleanBooleanInt2Long(helperDoubleBooleanBooleanInt2Long) == "Double:Boolean:Boolean:Int2Long"
    result &= FunctionSignature.getSignatureDoubleBooleanBooleanLong2Long(helperDoubleBooleanBooleanLong2Long) == "Double:Boolean:Boolean:Long2Long"
    result &= FunctionSignature.getSignatureDoubleBooleanBooleanDouble2Long(helperDoubleBooleanBooleanDouble2Long) == "Double:Boolean:Boolean:Double2Long"
    result &= FunctionSignature.getSignatureDoubleBooleanBooleanString2Long(helperDoubleBooleanBooleanString2Long) == "Double:Boolean:Boolean:String2Long"
    result &= FunctionSignature.getSignatureDoubleBooleanBooleanBoolean2Long(helperDoubleBooleanBooleanBoolean2Long) == "Double:Boolean:Boolean:Boolean2Long"
    result &= FunctionSignature.getSignatureStringIntIntInt2Long(helperStringIntIntInt2Long) == "String:Int:Int:Int2Long"
    result &= FunctionSignature.getSignatureStringIntIntLong2Long(helperStringIntIntLong2Long) == "String:Int:Int:Long2Long"
    result &= FunctionSignature.getSignatureStringIntIntDouble2Long(helperStringIntIntDouble2Long) == "String:Int:Int:Double2Long"
    result &= FunctionSignature.getSignatureStringIntIntString2Long(helperStringIntIntString2Long) == "String:Int:Int:String2Long"
    result &= FunctionSignature.getSignatureStringIntIntBoolean2Long(helperStringIntIntBoolean2Long) == "String:Int:Int:Boolean2Long"
    result &= FunctionSignature.getSignatureStringIntLongInt2Long(helperStringIntLongInt2Long) == "String:Int:Long:Int2Long"
    result &= FunctionSignature.getSignatureStringIntLongLong2Long(helperStringIntLongLong2Long) == "String:Int:Long:Long2Long"
    result &= FunctionSignature.getSignatureStringIntLongDouble2Long(helperStringIntLongDouble2Long) == "String:Int:Long:Double2Long"
    result &= FunctionSignature.getSignatureStringIntLongString2Long(helperStringIntLongString2Long) == "String:Int:Long:String2Long"
    result &= FunctionSignature.getSignatureStringIntLongBoolean2Long(helperStringIntLongBoolean2Long) == "String:Int:Long:Boolean2Long"
    result &= FunctionSignature.getSignatureStringIntDoubleInt2Long(helperStringIntDoubleInt2Long) == "String:Int:Double:Int2Long"
    result &= FunctionSignature.getSignatureStringIntDoubleLong2Long(helperStringIntDoubleLong2Long) == "String:Int:Double:Long2Long"
    result &= FunctionSignature.getSignatureStringIntDoubleDouble2Long(helperStringIntDoubleDouble2Long) == "String:Int:Double:Double2Long"
    result &= FunctionSignature.getSignatureStringIntDoubleString2Long(helperStringIntDoubleString2Long) == "String:Int:Double:String2Long"
    result &= FunctionSignature.getSignatureStringIntDoubleBoolean2Long(helperStringIntDoubleBoolean2Long) == "String:Int:Double:Boolean2Long"
    result &= FunctionSignature.getSignatureStringIntStringInt2Long(helperStringIntStringInt2Long) == "String:Int:String:Int2Long"
    result &= FunctionSignature.getSignatureStringIntStringLong2Long(helperStringIntStringLong2Long) == "String:Int:String:Long2Long"
    result &= FunctionSignature.getSignatureStringIntStringDouble2Long(helperStringIntStringDouble2Long) == "String:Int:String:Double2Long"
    result &= FunctionSignature.getSignatureStringIntStringString2Long(helperStringIntStringString2Long) == "String:Int:String:String2Long"
    result &= FunctionSignature.getSignatureStringIntStringBoolean2Long(helperStringIntStringBoolean2Long) == "String:Int:String:Boolean2Long"
    result &= FunctionSignature.getSignatureStringIntBooleanInt2Long(helperStringIntBooleanInt2Long) == "String:Int:Boolean:Int2Long"
    result &= FunctionSignature.getSignatureStringIntBooleanLong2Long(helperStringIntBooleanLong2Long) == "String:Int:Boolean:Long2Long"
    result &= FunctionSignature.getSignatureStringIntBooleanDouble2Long(helperStringIntBooleanDouble2Long) == "String:Int:Boolean:Double2Long"
    result &= FunctionSignature.getSignatureStringIntBooleanString2Long(helperStringIntBooleanString2Long) == "String:Int:Boolean:String2Long"
    result &= FunctionSignature.getSignatureStringIntBooleanBoolean2Long(helperStringIntBooleanBoolean2Long) == "String:Int:Boolean:Boolean2Long"
    result &= FunctionSignature.getSignatureStringLongIntInt2Long(helperStringLongIntInt2Long) == "String:Long:Int:Int2Long"
    result &= FunctionSignature.getSignatureStringLongIntLong2Long(helperStringLongIntLong2Long) == "String:Long:Int:Long2Long"
    result &= FunctionSignature.getSignatureStringLongIntDouble2Long(helperStringLongIntDouble2Long) == "String:Long:Int:Double2Long"
    result &= FunctionSignature.getSignatureStringLongIntString2Long(helperStringLongIntString2Long) == "String:Long:Int:String2Long"
    result &= FunctionSignature.getSignatureStringLongIntBoolean2Long(helperStringLongIntBoolean2Long) == "String:Long:Int:Boolean2Long"
    result &= FunctionSignature.getSignatureStringLongLongInt2Long(helperStringLongLongInt2Long) == "String:Long:Long:Int2Long"
    result &= FunctionSignature.getSignatureStringLongLongLong2Long(helperStringLongLongLong2Long) == "String:Long:Long:Long2Long"
    result &= FunctionSignature.getSignatureStringLongLongDouble2Long(helperStringLongLongDouble2Long) == "String:Long:Long:Double2Long"
    result &= FunctionSignature.getSignatureStringLongLongString2Long(helperStringLongLongString2Long) == "String:Long:Long:String2Long"
    result &= FunctionSignature.getSignatureStringLongLongBoolean2Long(helperStringLongLongBoolean2Long) == "String:Long:Long:Boolean2Long"
    result &= FunctionSignature.getSignatureStringLongDoubleInt2Long(helperStringLongDoubleInt2Long) == "String:Long:Double:Int2Long"
    result &= FunctionSignature.getSignatureStringLongDoubleLong2Long(helperStringLongDoubleLong2Long) == "String:Long:Double:Long2Long"
    result &= FunctionSignature.getSignatureStringLongDoubleDouble2Long(helperStringLongDoubleDouble2Long) == "String:Long:Double:Double2Long"
    result &= FunctionSignature.getSignatureStringLongDoubleString2Long(helperStringLongDoubleString2Long) == "String:Long:Double:String2Long"
    result &= FunctionSignature.getSignatureStringLongDoubleBoolean2Long(helperStringLongDoubleBoolean2Long) == "String:Long:Double:Boolean2Long"
    result &= FunctionSignature.getSignatureStringLongStringInt2Long(helperStringLongStringInt2Long) == "String:Long:String:Int2Long"
    result &= FunctionSignature.getSignatureStringLongStringLong2Long(helperStringLongStringLong2Long) == "String:Long:String:Long2Long"
    result &= FunctionSignature.getSignatureStringLongStringDouble2Long(helperStringLongStringDouble2Long) == "String:Long:String:Double2Long"
    result &= FunctionSignature.getSignatureStringLongStringString2Long(helperStringLongStringString2Long) == "String:Long:String:String2Long"
    result &= FunctionSignature.getSignatureStringLongStringBoolean2Long(helperStringLongStringBoolean2Long) == "String:Long:String:Boolean2Long"
    result &= FunctionSignature.getSignatureStringLongBooleanInt2Long(helperStringLongBooleanInt2Long) == "String:Long:Boolean:Int2Long"
    result &= FunctionSignature.getSignatureStringLongBooleanLong2Long(helperStringLongBooleanLong2Long) == "String:Long:Boolean:Long2Long"
    result &= FunctionSignature.getSignatureStringLongBooleanDouble2Long(helperStringLongBooleanDouble2Long) == "String:Long:Boolean:Double2Long"
    result &= FunctionSignature.getSignatureStringLongBooleanString2Long(helperStringLongBooleanString2Long) == "String:Long:Boolean:String2Long"
    result &= FunctionSignature.getSignatureStringLongBooleanBoolean2Long(helperStringLongBooleanBoolean2Long) == "String:Long:Boolean:Boolean2Long"
    result &= FunctionSignature.getSignatureStringDoubleIntInt2Long(helperStringDoubleIntInt2Long) == "String:Double:Int:Int2Long"
    result &= FunctionSignature.getSignatureStringDoubleIntLong2Long(helperStringDoubleIntLong2Long) == "String:Double:Int:Long2Long"
    result &= FunctionSignature.getSignatureStringDoubleIntDouble2Long(helperStringDoubleIntDouble2Long) == "String:Double:Int:Double2Long"
    result &= FunctionSignature.getSignatureStringDoubleIntString2Long(helperStringDoubleIntString2Long) == "String:Double:Int:String2Long"
    result &= FunctionSignature.getSignatureStringDoubleIntBoolean2Long(helperStringDoubleIntBoolean2Long) == "String:Double:Int:Boolean2Long"
    result &= FunctionSignature.getSignatureStringDoubleLongInt2Long(helperStringDoubleLongInt2Long) == "String:Double:Long:Int2Long"
    result &= FunctionSignature.getSignatureStringDoubleLongLong2Long(helperStringDoubleLongLong2Long) == "String:Double:Long:Long2Long"
    result &= FunctionSignature.getSignatureStringDoubleLongDouble2Long(helperStringDoubleLongDouble2Long) == "String:Double:Long:Double2Long"
    result &= FunctionSignature.getSignatureStringDoubleLongString2Long(helperStringDoubleLongString2Long) == "String:Double:Long:String2Long"
    result &= FunctionSignature.getSignatureStringDoubleLongBoolean2Long(helperStringDoubleLongBoolean2Long) == "String:Double:Long:Boolean2Long"
    result &= FunctionSignature.getSignatureStringDoubleDoubleInt2Long(helperStringDoubleDoubleInt2Long) == "String:Double:Double:Int2Long"
    result &= FunctionSignature.getSignatureStringDoubleDoubleLong2Long(helperStringDoubleDoubleLong2Long) == "String:Double:Double:Long2Long"
    result &= FunctionSignature.getSignatureStringDoubleDoubleDouble2Long(helperStringDoubleDoubleDouble2Long) == "String:Double:Double:Double2Long"
    result &= FunctionSignature.getSignatureStringDoubleDoubleString2Long(helperStringDoubleDoubleString2Long) == "String:Double:Double:String2Long"
    result &= FunctionSignature.getSignatureStringDoubleDoubleBoolean2Long(helperStringDoubleDoubleBoolean2Long) == "String:Double:Double:Boolean2Long"
    result &= FunctionSignature.getSignatureStringDoubleStringInt2Long(helperStringDoubleStringInt2Long) == "String:Double:String:Int2Long"
    result &= FunctionSignature.getSignatureStringDoubleStringLong2Long(helperStringDoubleStringLong2Long) == "String:Double:String:Long2Long"
    result &= FunctionSignature.getSignatureStringDoubleStringDouble2Long(helperStringDoubleStringDouble2Long) == "String:Double:String:Double2Long"
    result &= FunctionSignature.getSignatureStringDoubleStringString2Long(helperStringDoubleStringString2Long) == "String:Double:String:String2Long"
    result &= FunctionSignature.getSignatureStringDoubleStringBoolean2Long(helperStringDoubleStringBoolean2Long) == "String:Double:String:Boolean2Long"
    result &= FunctionSignature.getSignatureStringDoubleBooleanInt2Long(helperStringDoubleBooleanInt2Long) == "String:Double:Boolean:Int2Long"
    result &= FunctionSignature.getSignatureStringDoubleBooleanLong2Long(helperStringDoubleBooleanLong2Long) == "String:Double:Boolean:Long2Long"
    result &= FunctionSignature.getSignatureStringDoubleBooleanDouble2Long(helperStringDoubleBooleanDouble2Long) == "String:Double:Boolean:Double2Long"
    result &= FunctionSignature.getSignatureStringDoubleBooleanString2Long(helperStringDoubleBooleanString2Long) == "String:Double:Boolean:String2Long"
    result &= FunctionSignature.getSignatureStringDoubleBooleanBoolean2Long(helperStringDoubleBooleanBoolean2Long) == "String:Double:Boolean:Boolean2Long"
    result &= FunctionSignature.getSignatureStringStringIntInt2Long(helperStringStringIntInt2Long) == "String:String:Int:Int2Long"
    result &= FunctionSignature.getSignatureStringStringIntLong2Long(helperStringStringIntLong2Long) == "String:String:Int:Long2Long"
    result &= FunctionSignature.getSignatureStringStringIntDouble2Long(helperStringStringIntDouble2Long) == "String:String:Int:Double2Long"
    result &= FunctionSignature.getSignatureStringStringIntString2Long(helperStringStringIntString2Long) == "String:String:Int:String2Long"
    result &= FunctionSignature.getSignatureStringStringIntBoolean2Long(helperStringStringIntBoolean2Long) == "String:String:Int:Boolean2Long"
    result &= FunctionSignature.getSignatureStringStringLongInt2Long(helperStringStringLongInt2Long) == "String:String:Long:Int2Long"
    result &= FunctionSignature.getSignatureStringStringLongLong2Long(helperStringStringLongLong2Long) == "String:String:Long:Long2Long"
    result &= FunctionSignature.getSignatureStringStringLongDouble2Long(helperStringStringLongDouble2Long) == "String:String:Long:Double2Long"
    result &= FunctionSignature.getSignatureStringStringLongString2Long(helperStringStringLongString2Long) == "String:String:Long:String2Long"
    result &= FunctionSignature.getSignatureStringStringLongBoolean2Long(helperStringStringLongBoolean2Long) == "String:String:Long:Boolean2Long"
    result &= FunctionSignature.getSignatureStringStringDoubleInt2Long(helperStringStringDoubleInt2Long) == "String:String:Double:Int2Long"
    result &= FunctionSignature.getSignatureStringStringDoubleLong2Long(helperStringStringDoubleLong2Long) == "String:String:Double:Long2Long"
    result &= FunctionSignature.getSignatureStringStringDoubleDouble2Long(helperStringStringDoubleDouble2Long) == "String:String:Double:Double2Long"
    result &= FunctionSignature.getSignatureStringStringDoubleString2Long(helperStringStringDoubleString2Long) == "String:String:Double:String2Long"
    result &= FunctionSignature.getSignatureStringStringDoubleBoolean2Long(helperStringStringDoubleBoolean2Long) == "String:String:Double:Boolean2Long"
    result &= FunctionSignature.getSignatureStringStringStringInt2Long(helperStringStringStringInt2Long) == "String:String:String:Int2Long"
    result &= FunctionSignature.getSignatureStringStringStringLong2Long(helperStringStringStringLong2Long) == "String:String:String:Long2Long"
    result &= FunctionSignature.getSignatureStringStringStringDouble2Long(helperStringStringStringDouble2Long) == "String:String:String:Double2Long"
    result &= FunctionSignature.getSignatureStringStringStringString2Long(helperStringStringStringString2Long) == "String:String:String:String2Long"
    result &= FunctionSignature.getSignatureStringStringStringBoolean2Long(helperStringStringStringBoolean2Long) == "String:String:String:Boolean2Long"
    result &= FunctionSignature.getSignatureStringStringBooleanInt2Long(helperStringStringBooleanInt2Long) == "String:String:Boolean:Int2Long"
    result &= FunctionSignature.getSignatureStringStringBooleanLong2Long(helperStringStringBooleanLong2Long) == "String:String:Boolean:Long2Long"
    result &= FunctionSignature.getSignatureStringStringBooleanDouble2Long(helperStringStringBooleanDouble2Long) == "String:String:Boolean:Double2Long"
    result &= FunctionSignature.getSignatureStringStringBooleanString2Long(helperStringStringBooleanString2Long) == "String:String:Boolean:String2Long"
    result &= FunctionSignature.getSignatureStringStringBooleanBoolean2Long(helperStringStringBooleanBoolean2Long) == "String:String:Boolean:Boolean2Long"
    result &= FunctionSignature.getSignatureStringBooleanIntInt2Long(helperStringBooleanIntInt2Long) == "String:Boolean:Int:Int2Long"
    result &= FunctionSignature.getSignatureStringBooleanIntLong2Long(helperStringBooleanIntLong2Long) == "String:Boolean:Int:Long2Long"
    result &= FunctionSignature.getSignatureStringBooleanIntDouble2Long(helperStringBooleanIntDouble2Long) == "String:Boolean:Int:Double2Long"
    result &= FunctionSignature.getSignatureStringBooleanIntString2Long(helperStringBooleanIntString2Long) == "String:Boolean:Int:String2Long"
    result &= FunctionSignature.getSignatureStringBooleanIntBoolean2Long(helperStringBooleanIntBoolean2Long) == "String:Boolean:Int:Boolean2Long"
    result &= FunctionSignature.getSignatureStringBooleanLongInt2Long(helperStringBooleanLongInt2Long) == "String:Boolean:Long:Int2Long"
    result &= FunctionSignature.getSignatureStringBooleanLongLong2Long(helperStringBooleanLongLong2Long) == "String:Boolean:Long:Long2Long"
    result &= FunctionSignature.getSignatureStringBooleanLongDouble2Long(helperStringBooleanLongDouble2Long) == "String:Boolean:Long:Double2Long"
    result &= FunctionSignature.getSignatureStringBooleanLongString2Long(helperStringBooleanLongString2Long) == "String:Boolean:Long:String2Long"
    result &= FunctionSignature.getSignatureStringBooleanLongBoolean2Long(helperStringBooleanLongBoolean2Long) == "String:Boolean:Long:Boolean2Long"
    result &= FunctionSignature.getSignatureStringBooleanDoubleInt2Long(helperStringBooleanDoubleInt2Long) == "String:Boolean:Double:Int2Long"
    result &= FunctionSignature.getSignatureStringBooleanDoubleLong2Long(helperStringBooleanDoubleLong2Long) == "String:Boolean:Double:Long2Long"
    result &= FunctionSignature.getSignatureStringBooleanDoubleDouble2Long(helperStringBooleanDoubleDouble2Long) == "String:Boolean:Double:Double2Long"
    result &= FunctionSignature.getSignatureStringBooleanDoubleString2Long(helperStringBooleanDoubleString2Long) == "String:Boolean:Double:String2Long"
    result &= FunctionSignature.getSignatureStringBooleanDoubleBoolean2Long(helperStringBooleanDoubleBoolean2Long) == "String:Boolean:Double:Boolean2Long"
    result &= FunctionSignature.getSignatureStringBooleanStringInt2Long(helperStringBooleanStringInt2Long) == "String:Boolean:String:Int2Long"
    result &= FunctionSignature.getSignatureStringBooleanStringLong2Long(helperStringBooleanStringLong2Long) == "String:Boolean:String:Long2Long"
    result &= FunctionSignature.getSignatureStringBooleanStringDouble2Long(helperStringBooleanStringDouble2Long) == "String:Boolean:String:Double2Long"
    result &= FunctionSignature.getSignatureStringBooleanStringString2Long(helperStringBooleanStringString2Long) == "String:Boolean:String:String2Long"
    result &= FunctionSignature.getSignatureStringBooleanStringBoolean2Long(helperStringBooleanStringBoolean2Long) == "String:Boolean:String:Boolean2Long"
    result &= FunctionSignature.getSignatureStringBooleanBooleanInt2Long(helperStringBooleanBooleanInt2Long) == "String:Boolean:Boolean:Int2Long"
    result &= FunctionSignature.getSignatureStringBooleanBooleanLong2Long(helperStringBooleanBooleanLong2Long) == "String:Boolean:Boolean:Long2Long"
    result &= FunctionSignature.getSignatureStringBooleanBooleanDouble2Long(helperStringBooleanBooleanDouble2Long) == "String:Boolean:Boolean:Double2Long"
    result &= FunctionSignature.getSignatureStringBooleanBooleanString2Long(helperStringBooleanBooleanString2Long) == "String:Boolean:Boolean:String2Long"
    result &= FunctionSignature.getSignatureStringBooleanBooleanBoolean2Long(helperStringBooleanBooleanBoolean2Long) == "String:Boolean:Boolean:Boolean2Long"
    result &= FunctionSignature.getSignatureBooleanIntIntInt2Long(helperBooleanIntIntInt2Long) == "Boolean:Int:Int:Int2Long"
    result &= FunctionSignature.getSignatureBooleanIntIntLong2Long(helperBooleanIntIntLong2Long) == "Boolean:Int:Int:Long2Long"
    result &= FunctionSignature.getSignatureBooleanIntIntDouble2Long(helperBooleanIntIntDouble2Long) == "Boolean:Int:Int:Double2Long"
    result &= FunctionSignature.getSignatureBooleanIntIntString2Long(helperBooleanIntIntString2Long) == "Boolean:Int:Int:String2Long"
    result &= FunctionSignature.getSignatureBooleanIntIntBoolean2Long(helperBooleanIntIntBoolean2Long) == "Boolean:Int:Int:Boolean2Long"
    result &= FunctionSignature.getSignatureBooleanIntLongInt2Long(helperBooleanIntLongInt2Long) == "Boolean:Int:Long:Int2Long"
    result &= FunctionSignature.getSignatureBooleanIntLongLong2Long(helperBooleanIntLongLong2Long) == "Boolean:Int:Long:Long2Long"
    result &= FunctionSignature.getSignatureBooleanIntLongDouble2Long(helperBooleanIntLongDouble2Long) == "Boolean:Int:Long:Double2Long"
    result &= FunctionSignature.getSignatureBooleanIntLongString2Long(helperBooleanIntLongString2Long) == "Boolean:Int:Long:String2Long"
    result &= FunctionSignature.getSignatureBooleanIntLongBoolean2Long(helperBooleanIntLongBoolean2Long) == "Boolean:Int:Long:Boolean2Long"
    result &= FunctionSignature.getSignatureBooleanIntDoubleInt2Long(helperBooleanIntDoubleInt2Long) == "Boolean:Int:Double:Int2Long"
    result &= FunctionSignature.getSignatureBooleanIntDoubleLong2Long(helperBooleanIntDoubleLong2Long) == "Boolean:Int:Double:Long2Long"
    result &= FunctionSignature.getSignatureBooleanIntDoubleDouble2Long(helperBooleanIntDoubleDouble2Long) == "Boolean:Int:Double:Double2Long"
    result &= FunctionSignature.getSignatureBooleanIntDoubleString2Long(helperBooleanIntDoubleString2Long) == "Boolean:Int:Double:String2Long"
    result &= FunctionSignature.getSignatureBooleanIntDoubleBoolean2Long(helperBooleanIntDoubleBoolean2Long) == "Boolean:Int:Double:Boolean2Long"
    result &= FunctionSignature.getSignatureBooleanIntStringInt2Long(helperBooleanIntStringInt2Long) == "Boolean:Int:String:Int2Long"
    result &= FunctionSignature.getSignatureBooleanIntStringLong2Long(helperBooleanIntStringLong2Long) == "Boolean:Int:String:Long2Long"
    result &= FunctionSignature.getSignatureBooleanIntStringDouble2Long(helperBooleanIntStringDouble2Long) == "Boolean:Int:String:Double2Long"
    result &= FunctionSignature.getSignatureBooleanIntStringString2Long(helperBooleanIntStringString2Long) == "Boolean:Int:String:String2Long"
    result &= FunctionSignature.getSignatureBooleanIntStringBoolean2Long(helperBooleanIntStringBoolean2Long) == "Boolean:Int:String:Boolean2Long"
    result &= FunctionSignature.getSignatureBooleanIntBooleanInt2Long(helperBooleanIntBooleanInt2Long) == "Boolean:Int:Boolean:Int2Long"
    result &= FunctionSignature.getSignatureBooleanIntBooleanLong2Long(helperBooleanIntBooleanLong2Long) == "Boolean:Int:Boolean:Long2Long"
    result &= FunctionSignature.getSignatureBooleanIntBooleanDouble2Long(helperBooleanIntBooleanDouble2Long) == "Boolean:Int:Boolean:Double2Long"
    result &= FunctionSignature.getSignatureBooleanIntBooleanString2Long(helperBooleanIntBooleanString2Long) == "Boolean:Int:Boolean:String2Long"
    result &= FunctionSignature.getSignatureBooleanIntBooleanBoolean2Long(helperBooleanIntBooleanBoolean2Long) == "Boolean:Int:Boolean:Boolean2Long"
    result &= FunctionSignature.getSignatureBooleanLongIntInt2Long(helperBooleanLongIntInt2Long) == "Boolean:Long:Int:Int2Long"
    result &= FunctionSignature.getSignatureBooleanLongIntLong2Long(helperBooleanLongIntLong2Long) == "Boolean:Long:Int:Long2Long"
    result &= FunctionSignature.getSignatureBooleanLongIntDouble2Long(helperBooleanLongIntDouble2Long) == "Boolean:Long:Int:Double2Long"
    result &= FunctionSignature.getSignatureBooleanLongIntString2Long(helperBooleanLongIntString2Long) == "Boolean:Long:Int:String2Long"
    result &= FunctionSignature.getSignatureBooleanLongIntBoolean2Long(helperBooleanLongIntBoolean2Long) == "Boolean:Long:Int:Boolean2Long"
    result &= FunctionSignature.getSignatureBooleanLongLongInt2Long(helperBooleanLongLongInt2Long) == "Boolean:Long:Long:Int2Long"
    result &= FunctionSignature.getSignatureBooleanLongLongLong2Long(helperBooleanLongLongLong2Long) == "Boolean:Long:Long:Long2Long"
    result &= FunctionSignature.getSignatureBooleanLongLongDouble2Long(helperBooleanLongLongDouble2Long) == "Boolean:Long:Long:Double2Long"
    result &= FunctionSignature.getSignatureBooleanLongLongString2Long(helperBooleanLongLongString2Long) == "Boolean:Long:Long:String2Long"
    result &= FunctionSignature.getSignatureBooleanLongLongBoolean2Long(helperBooleanLongLongBoolean2Long) == "Boolean:Long:Long:Boolean2Long"
    result &= FunctionSignature.getSignatureBooleanLongDoubleInt2Long(helperBooleanLongDoubleInt2Long) == "Boolean:Long:Double:Int2Long"
    result &= FunctionSignature.getSignatureBooleanLongDoubleLong2Long(helperBooleanLongDoubleLong2Long) == "Boolean:Long:Double:Long2Long"
    result &= FunctionSignature.getSignatureBooleanLongDoubleDouble2Long(helperBooleanLongDoubleDouble2Long) == "Boolean:Long:Double:Double2Long"
    result &= FunctionSignature.getSignatureBooleanLongDoubleString2Long(helperBooleanLongDoubleString2Long) == "Boolean:Long:Double:String2Long"
    result &= FunctionSignature.getSignatureBooleanLongDoubleBoolean2Long(helperBooleanLongDoubleBoolean2Long) == "Boolean:Long:Double:Boolean2Long"
    result &= FunctionSignature.getSignatureBooleanLongStringInt2Long(helperBooleanLongStringInt2Long) == "Boolean:Long:String:Int2Long"
    result &= FunctionSignature.getSignatureBooleanLongStringLong2Long(helperBooleanLongStringLong2Long) == "Boolean:Long:String:Long2Long"
    result &= FunctionSignature.getSignatureBooleanLongStringDouble2Long(helperBooleanLongStringDouble2Long) == "Boolean:Long:String:Double2Long"
    result &= FunctionSignature.getSignatureBooleanLongStringString2Long(helperBooleanLongStringString2Long) == "Boolean:Long:String:String2Long"
    result &= FunctionSignature.getSignatureBooleanLongStringBoolean2Long(helperBooleanLongStringBoolean2Long) == "Boolean:Long:String:Boolean2Long"
    result &= FunctionSignature.getSignatureBooleanLongBooleanInt2Long(helperBooleanLongBooleanInt2Long) == "Boolean:Long:Boolean:Int2Long"
    result &= FunctionSignature.getSignatureBooleanLongBooleanLong2Long(helperBooleanLongBooleanLong2Long) == "Boolean:Long:Boolean:Long2Long"
    result &= FunctionSignature.getSignatureBooleanLongBooleanDouble2Long(helperBooleanLongBooleanDouble2Long) == "Boolean:Long:Boolean:Double2Long"
    result &= FunctionSignature.getSignatureBooleanLongBooleanString2Long(helperBooleanLongBooleanString2Long) == "Boolean:Long:Boolean:String2Long"
    result &= FunctionSignature.getSignatureBooleanLongBooleanBoolean2Long(helperBooleanLongBooleanBoolean2Long) == "Boolean:Long:Boolean:Boolean2Long"
    result &= FunctionSignature.getSignatureBooleanDoubleIntInt2Long(helperBooleanDoubleIntInt2Long) == "Boolean:Double:Int:Int2Long"
    result &= FunctionSignature.getSignatureBooleanDoubleIntLong2Long(helperBooleanDoubleIntLong2Long) == "Boolean:Double:Int:Long2Long"
    result &= FunctionSignature.getSignatureBooleanDoubleIntDouble2Long(helperBooleanDoubleIntDouble2Long) == "Boolean:Double:Int:Double2Long"
    result &= FunctionSignature.getSignatureBooleanDoubleIntString2Long(helperBooleanDoubleIntString2Long) == "Boolean:Double:Int:String2Long"
    result &= FunctionSignature.getSignatureBooleanDoubleIntBoolean2Long(helperBooleanDoubleIntBoolean2Long) == "Boolean:Double:Int:Boolean2Long"
    result &= FunctionSignature.getSignatureBooleanDoubleLongInt2Long(helperBooleanDoubleLongInt2Long) == "Boolean:Double:Long:Int2Long"
    result &= FunctionSignature.getSignatureBooleanDoubleLongLong2Long(helperBooleanDoubleLongLong2Long) == "Boolean:Double:Long:Long2Long"
    result &= FunctionSignature.getSignatureBooleanDoubleLongDouble2Long(helperBooleanDoubleLongDouble2Long) == "Boolean:Double:Long:Double2Long"
    result &= FunctionSignature.getSignatureBooleanDoubleLongString2Long(helperBooleanDoubleLongString2Long) == "Boolean:Double:Long:String2Long"
    result &= FunctionSignature.getSignatureBooleanDoubleLongBoolean2Long(helperBooleanDoubleLongBoolean2Long) == "Boolean:Double:Long:Boolean2Long"
    result &= FunctionSignature.getSignatureBooleanDoubleDoubleInt2Long(helperBooleanDoubleDoubleInt2Long) == "Boolean:Double:Double:Int2Long"
    result &= FunctionSignature.getSignatureBooleanDoubleDoubleLong2Long(helperBooleanDoubleDoubleLong2Long) == "Boolean:Double:Double:Long2Long"
    result &= FunctionSignature.getSignatureBooleanDoubleDoubleDouble2Long(helperBooleanDoubleDoubleDouble2Long) == "Boolean:Double:Double:Double2Long"
    result &= FunctionSignature.getSignatureBooleanDoubleDoubleString2Long(helperBooleanDoubleDoubleString2Long) == "Boolean:Double:Double:String2Long"
    result &= FunctionSignature.getSignatureBooleanDoubleDoubleBoolean2Long(helperBooleanDoubleDoubleBoolean2Long) == "Boolean:Double:Double:Boolean2Long"
    result &= FunctionSignature.getSignatureBooleanDoubleStringInt2Long(helperBooleanDoubleStringInt2Long) == "Boolean:Double:String:Int2Long"
    result &= FunctionSignature.getSignatureBooleanDoubleStringLong2Long(helperBooleanDoubleStringLong2Long) == "Boolean:Double:String:Long2Long"
    result &= FunctionSignature.getSignatureBooleanDoubleStringDouble2Long(helperBooleanDoubleStringDouble2Long) == "Boolean:Double:String:Double2Long"
    result &= FunctionSignature.getSignatureBooleanDoubleStringString2Long(helperBooleanDoubleStringString2Long) == "Boolean:Double:String:String2Long"
    result &= FunctionSignature.getSignatureBooleanDoubleStringBoolean2Long(helperBooleanDoubleStringBoolean2Long) == "Boolean:Double:String:Boolean2Long"
    result &= FunctionSignature.getSignatureBooleanDoubleBooleanInt2Long(helperBooleanDoubleBooleanInt2Long) == "Boolean:Double:Boolean:Int2Long"
    result &= FunctionSignature.getSignatureBooleanDoubleBooleanLong2Long(helperBooleanDoubleBooleanLong2Long) == "Boolean:Double:Boolean:Long2Long"
    result &= FunctionSignature.getSignatureBooleanDoubleBooleanDouble2Long(helperBooleanDoubleBooleanDouble2Long) == "Boolean:Double:Boolean:Double2Long"
    result &= FunctionSignature.getSignatureBooleanDoubleBooleanString2Long(helperBooleanDoubleBooleanString2Long) == "Boolean:Double:Boolean:String2Long"
    result &= FunctionSignature.getSignatureBooleanDoubleBooleanBoolean2Long(helperBooleanDoubleBooleanBoolean2Long) == "Boolean:Double:Boolean:Boolean2Long"
    result &= FunctionSignature.getSignatureBooleanStringIntInt2Long(helperBooleanStringIntInt2Long) == "Boolean:String:Int:Int2Long"
    result &= FunctionSignature.getSignatureBooleanStringIntLong2Long(helperBooleanStringIntLong2Long) == "Boolean:String:Int:Long2Long"
    result &= FunctionSignature.getSignatureBooleanStringIntDouble2Long(helperBooleanStringIntDouble2Long) == "Boolean:String:Int:Double2Long"
    result &= FunctionSignature.getSignatureBooleanStringIntString2Long(helperBooleanStringIntString2Long) == "Boolean:String:Int:String2Long"
    result &= FunctionSignature.getSignatureBooleanStringIntBoolean2Long(helperBooleanStringIntBoolean2Long) == "Boolean:String:Int:Boolean2Long"
    result &= FunctionSignature.getSignatureBooleanStringLongInt2Long(helperBooleanStringLongInt2Long) == "Boolean:String:Long:Int2Long"
    result &= FunctionSignature.getSignatureBooleanStringLongLong2Long(helperBooleanStringLongLong2Long) == "Boolean:String:Long:Long2Long"
    result &= FunctionSignature.getSignatureBooleanStringLongDouble2Long(helperBooleanStringLongDouble2Long) == "Boolean:String:Long:Double2Long"
    result &= FunctionSignature.getSignatureBooleanStringLongString2Long(helperBooleanStringLongString2Long) == "Boolean:String:Long:String2Long"
    result &= FunctionSignature.getSignatureBooleanStringLongBoolean2Long(helperBooleanStringLongBoolean2Long) == "Boolean:String:Long:Boolean2Long"
    result &= FunctionSignature.getSignatureBooleanStringDoubleInt2Long(helperBooleanStringDoubleInt2Long) == "Boolean:String:Double:Int2Long"
    result &= FunctionSignature.getSignatureBooleanStringDoubleLong2Long(helperBooleanStringDoubleLong2Long) == "Boolean:String:Double:Long2Long"
    result &= FunctionSignature.getSignatureBooleanStringDoubleDouble2Long(helperBooleanStringDoubleDouble2Long) == "Boolean:String:Double:Double2Long"
    result &= FunctionSignature.getSignatureBooleanStringDoubleString2Long(helperBooleanStringDoubleString2Long) == "Boolean:String:Double:String2Long"
    result &= FunctionSignature.getSignatureBooleanStringDoubleBoolean2Long(helperBooleanStringDoubleBoolean2Long) == "Boolean:String:Double:Boolean2Long"
    result &= FunctionSignature.getSignatureBooleanStringStringInt2Long(helperBooleanStringStringInt2Long) == "Boolean:String:String:Int2Long"
    result &= FunctionSignature.getSignatureBooleanStringStringLong2Long(helperBooleanStringStringLong2Long) == "Boolean:String:String:Long2Long"
    result &= FunctionSignature.getSignatureBooleanStringStringDouble2Long(helperBooleanStringStringDouble2Long) == "Boolean:String:String:Double2Long"
    result &= FunctionSignature.getSignatureBooleanStringStringString2Long(helperBooleanStringStringString2Long) == "Boolean:String:String:String2Long"
    result &= FunctionSignature.getSignatureBooleanStringStringBoolean2Long(helperBooleanStringStringBoolean2Long) == "Boolean:String:String:Boolean2Long"
    result &= FunctionSignature.getSignatureBooleanStringBooleanInt2Long(helperBooleanStringBooleanInt2Long) == "Boolean:String:Boolean:Int2Long"
    result &= FunctionSignature.getSignatureBooleanStringBooleanLong2Long(helperBooleanStringBooleanLong2Long) == "Boolean:String:Boolean:Long2Long"
    result &= FunctionSignature.getSignatureBooleanStringBooleanDouble2Long(helperBooleanStringBooleanDouble2Long) == "Boolean:String:Boolean:Double2Long"
    result &= FunctionSignature.getSignatureBooleanStringBooleanString2Long(helperBooleanStringBooleanString2Long) == "Boolean:String:Boolean:String2Long"
    result &= FunctionSignature.getSignatureBooleanStringBooleanBoolean2Long(helperBooleanStringBooleanBoolean2Long) == "Boolean:String:Boolean:Boolean2Long"
    result &= FunctionSignature.getSignatureBooleanBooleanIntInt2Long(helperBooleanBooleanIntInt2Long) == "Boolean:Boolean:Int:Int2Long"
    result &= FunctionSignature.getSignatureBooleanBooleanIntLong2Long(helperBooleanBooleanIntLong2Long) == "Boolean:Boolean:Int:Long2Long"
    result &= FunctionSignature.getSignatureBooleanBooleanIntDouble2Long(helperBooleanBooleanIntDouble2Long) == "Boolean:Boolean:Int:Double2Long"
    result &= FunctionSignature.getSignatureBooleanBooleanIntString2Long(helperBooleanBooleanIntString2Long) == "Boolean:Boolean:Int:String2Long"
    result &= FunctionSignature.getSignatureBooleanBooleanIntBoolean2Long(helperBooleanBooleanIntBoolean2Long) == "Boolean:Boolean:Int:Boolean2Long"
    result &= FunctionSignature.getSignatureBooleanBooleanLongInt2Long(helperBooleanBooleanLongInt2Long) == "Boolean:Boolean:Long:Int2Long"
    result &= FunctionSignature.getSignatureBooleanBooleanLongLong2Long(helperBooleanBooleanLongLong2Long) == "Boolean:Boolean:Long:Long2Long"
    result &= FunctionSignature.getSignatureBooleanBooleanLongDouble2Long(helperBooleanBooleanLongDouble2Long) == "Boolean:Boolean:Long:Double2Long"
    result &= FunctionSignature.getSignatureBooleanBooleanLongString2Long(helperBooleanBooleanLongString2Long) == "Boolean:Boolean:Long:String2Long"
    result &= FunctionSignature.getSignatureBooleanBooleanLongBoolean2Long(helperBooleanBooleanLongBoolean2Long) == "Boolean:Boolean:Long:Boolean2Long"
    result &= FunctionSignature.getSignatureBooleanBooleanDoubleInt2Long(helperBooleanBooleanDoubleInt2Long) == "Boolean:Boolean:Double:Int2Long"
    result &= FunctionSignature.getSignatureBooleanBooleanDoubleLong2Long(helperBooleanBooleanDoubleLong2Long) == "Boolean:Boolean:Double:Long2Long"
    result &= FunctionSignature.getSignatureBooleanBooleanDoubleDouble2Long(helperBooleanBooleanDoubleDouble2Long) == "Boolean:Boolean:Double:Double2Long"
    result &= FunctionSignature.getSignatureBooleanBooleanDoubleString2Long(helperBooleanBooleanDoubleString2Long) == "Boolean:Boolean:Double:String2Long"
    result &= FunctionSignature.getSignatureBooleanBooleanDoubleBoolean2Long(helperBooleanBooleanDoubleBoolean2Long) == "Boolean:Boolean:Double:Boolean2Long"
    result &= FunctionSignature.getSignatureBooleanBooleanStringInt2Long(helperBooleanBooleanStringInt2Long) == "Boolean:Boolean:String:Int2Long"
    result &= FunctionSignature.getSignatureBooleanBooleanStringLong2Long(helperBooleanBooleanStringLong2Long) == "Boolean:Boolean:String:Long2Long"
    result &= FunctionSignature.getSignatureBooleanBooleanStringDouble2Long(helperBooleanBooleanStringDouble2Long) == "Boolean:Boolean:String:Double2Long"
    result &= FunctionSignature.getSignatureBooleanBooleanStringString2Long(helperBooleanBooleanStringString2Long) == "Boolean:Boolean:String:String2Long"
    result &= FunctionSignature.getSignatureBooleanBooleanStringBoolean2Long(helperBooleanBooleanStringBoolean2Long) == "Boolean:Boolean:String:Boolean2Long"
    result &= FunctionSignature.getSignatureBooleanBooleanBooleanInt2Long(helperBooleanBooleanBooleanInt2Long) == "Boolean:Boolean:Boolean:Int2Long"
    result &= FunctionSignature.getSignatureBooleanBooleanBooleanLong2Long(helperBooleanBooleanBooleanLong2Long) == "Boolean:Boolean:Boolean:Long2Long"
    result &= FunctionSignature.getSignatureBooleanBooleanBooleanDouble2Long(helperBooleanBooleanBooleanDouble2Long) == "Boolean:Boolean:Boolean:Double2Long"
    result &= FunctionSignature.getSignatureBooleanBooleanBooleanString2Long(helperBooleanBooleanBooleanString2Long) == "Boolean:Boolean:Boolean:String2Long"
    result &= FunctionSignature.getSignatureBooleanBooleanBooleanBoolean2Long(helperBooleanBooleanBooleanBoolean2Long) == "Boolean:Boolean:Boolean:Boolean2Long"
    assert(result)
  }
  def helperIntIntIntInt2Double(a1: iFun, a2: iFun, a3: iFun, a4: iFun): dFun = dFunDummy
  def helperIntIntIntLong2Double(a1: iFun, a2: iFun, a3: iFun, a4: lFun): dFun = dFunDummy
  def helperIntIntIntDouble2Double(a1: iFun, a2: iFun, a3: iFun, a4: dFun): dFun = dFunDummy
  def helperIntIntIntString2Double(a1: iFun, a2: iFun, a3: iFun, a4: sFun): dFun = dFunDummy
  def helperIntIntIntBoolean2Double(a1: iFun, a2: iFun, a3: iFun, a4: bFun): dFun = dFunDummy
  def helperIntIntLongInt2Double(a1: iFun, a2: iFun, a3: lFun, a4: iFun): dFun = dFunDummy
  def helperIntIntLongLong2Double(a1: iFun, a2: iFun, a3: lFun, a4: lFun): dFun = dFunDummy
  def helperIntIntLongDouble2Double(a1: iFun, a2: iFun, a3: lFun, a4: dFun): dFun = dFunDummy
  def helperIntIntLongString2Double(a1: iFun, a2: iFun, a3: lFun, a4: sFun): dFun = dFunDummy
  def helperIntIntLongBoolean2Double(a1: iFun, a2: iFun, a3: lFun, a4: bFun): dFun = dFunDummy
  def helperIntIntDoubleInt2Double(a1: iFun, a2: iFun, a3: dFun, a4: iFun): dFun = dFunDummy
  def helperIntIntDoubleLong2Double(a1: iFun, a2: iFun, a3: dFun, a4: lFun): dFun = dFunDummy
  def helperIntIntDoubleDouble2Double(a1: iFun, a2: iFun, a3: dFun, a4: dFun): dFun = dFunDummy
  def helperIntIntDoubleString2Double(a1: iFun, a2: iFun, a3: dFun, a4: sFun): dFun = dFunDummy
  def helperIntIntDoubleBoolean2Double(a1: iFun, a2: iFun, a3: dFun, a4: bFun): dFun = dFunDummy
  def helperIntIntStringInt2Double(a1: iFun, a2: iFun, a3: sFun, a4: iFun): dFun = dFunDummy
  def helperIntIntStringLong2Double(a1: iFun, a2: iFun, a3: sFun, a4: lFun): dFun = dFunDummy
  def helperIntIntStringDouble2Double(a1: iFun, a2: iFun, a3: sFun, a4: dFun): dFun = dFunDummy
  def helperIntIntStringString2Double(a1: iFun, a2: iFun, a3: sFun, a4: sFun): dFun = dFunDummy
  def helperIntIntStringBoolean2Double(a1: iFun, a2: iFun, a3: sFun, a4: bFun): dFun = dFunDummy
  def helperIntIntBooleanInt2Double(a1: iFun, a2: iFun, a3: bFun, a4: iFun): dFun = dFunDummy
  def helperIntIntBooleanLong2Double(a1: iFun, a2: iFun, a3: bFun, a4: lFun): dFun = dFunDummy
  def helperIntIntBooleanDouble2Double(a1: iFun, a2: iFun, a3: bFun, a4: dFun): dFun = dFunDummy
  def helperIntIntBooleanString2Double(a1: iFun, a2: iFun, a3: bFun, a4: sFun): dFun = dFunDummy
  def helperIntIntBooleanBoolean2Double(a1: iFun, a2: iFun, a3: bFun, a4: bFun): dFun = dFunDummy
  def helperIntLongIntInt2Double(a1: iFun, a2: lFun, a3: iFun, a4: iFun): dFun = dFunDummy
  def helperIntLongIntLong2Double(a1: iFun, a2: lFun, a3: iFun, a4: lFun): dFun = dFunDummy
  def helperIntLongIntDouble2Double(a1: iFun, a2: lFun, a3: iFun, a4: dFun): dFun = dFunDummy
  def helperIntLongIntString2Double(a1: iFun, a2: lFun, a3: iFun, a4: sFun): dFun = dFunDummy
  def helperIntLongIntBoolean2Double(a1: iFun, a2: lFun, a3: iFun, a4: bFun): dFun = dFunDummy
  def helperIntLongLongInt2Double(a1: iFun, a2: lFun, a3: lFun, a4: iFun): dFun = dFunDummy
  def helperIntLongLongLong2Double(a1: iFun, a2: lFun, a3: lFun, a4: lFun): dFun = dFunDummy
  def helperIntLongLongDouble2Double(a1: iFun, a2: lFun, a3: lFun, a4: dFun): dFun = dFunDummy
  def helperIntLongLongString2Double(a1: iFun, a2: lFun, a3: lFun, a4: sFun): dFun = dFunDummy
  def helperIntLongLongBoolean2Double(a1: iFun, a2: lFun, a3: lFun, a4: bFun): dFun = dFunDummy
  def helperIntLongDoubleInt2Double(a1: iFun, a2: lFun, a3: dFun, a4: iFun): dFun = dFunDummy
  def helperIntLongDoubleLong2Double(a1: iFun, a2: lFun, a3: dFun, a4: lFun): dFun = dFunDummy
  def helperIntLongDoubleDouble2Double(a1: iFun, a2: lFun, a3: dFun, a4: dFun): dFun = dFunDummy
  def helperIntLongDoubleString2Double(a1: iFun, a2: lFun, a3: dFun, a4: sFun): dFun = dFunDummy
  def helperIntLongDoubleBoolean2Double(a1: iFun, a2: lFun, a3: dFun, a4: bFun): dFun = dFunDummy
  def helperIntLongStringInt2Double(a1: iFun, a2: lFun, a3: sFun, a4: iFun): dFun = dFunDummy
  def helperIntLongStringLong2Double(a1: iFun, a2: lFun, a3: sFun, a4: lFun): dFun = dFunDummy
  def helperIntLongStringDouble2Double(a1: iFun, a2: lFun, a3: sFun, a4: dFun): dFun = dFunDummy
  def helperIntLongStringString2Double(a1: iFun, a2: lFun, a3: sFun, a4: sFun): dFun = dFunDummy
  def helperIntLongStringBoolean2Double(a1: iFun, a2: lFun, a3: sFun, a4: bFun): dFun = dFunDummy
  def helperIntLongBooleanInt2Double(a1: iFun, a2: lFun, a3: bFun, a4: iFun): dFun = dFunDummy
  def helperIntLongBooleanLong2Double(a1: iFun, a2: lFun, a3: bFun, a4: lFun): dFun = dFunDummy
  def helperIntLongBooleanDouble2Double(a1: iFun, a2: lFun, a3: bFun, a4: dFun): dFun = dFunDummy
  def helperIntLongBooleanString2Double(a1: iFun, a2: lFun, a3: bFun, a4: sFun): dFun = dFunDummy
  def helperIntLongBooleanBoolean2Double(a1: iFun, a2: lFun, a3: bFun, a4: bFun): dFun = dFunDummy
  def helperIntDoubleIntInt2Double(a1: iFun, a2: dFun, a3: iFun, a4: iFun): dFun = dFunDummy
  def helperIntDoubleIntLong2Double(a1: iFun, a2: dFun, a3: iFun, a4: lFun): dFun = dFunDummy
  def helperIntDoubleIntDouble2Double(a1: iFun, a2: dFun, a3: iFun, a4: dFun): dFun = dFunDummy
  def helperIntDoubleIntString2Double(a1: iFun, a2: dFun, a3: iFun, a4: sFun): dFun = dFunDummy
  def helperIntDoubleIntBoolean2Double(a1: iFun, a2: dFun, a3: iFun, a4: bFun): dFun = dFunDummy
  def helperIntDoubleLongInt2Double(a1: iFun, a2: dFun, a3: lFun, a4: iFun): dFun = dFunDummy
  def helperIntDoubleLongLong2Double(a1: iFun, a2: dFun, a3: lFun, a4: lFun): dFun = dFunDummy
  def helperIntDoubleLongDouble2Double(a1: iFun, a2: dFun, a3: lFun, a4: dFun): dFun = dFunDummy
  def helperIntDoubleLongString2Double(a1: iFun, a2: dFun, a3: lFun, a4: sFun): dFun = dFunDummy
  def helperIntDoubleLongBoolean2Double(a1: iFun, a2: dFun, a3: lFun, a4: bFun): dFun = dFunDummy
  def helperIntDoubleDoubleInt2Double(a1: iFun, a2: dFun, a3: dFun, a4: iFun): dFun = dFunDummy
  def helperIntDoubleDoubleLong2Double(a1: iFun, a2: dFun, a3: dFun, a4: lFun): dFun = dFunDummy
  def helperIntDoubleDoubleDouble2Double(a1: iFun, a2: dFun, a3: dFun, a4: dFun): dFun = dFunDummy
  def helperIntDoubleDoubleString2Double(a1: iFun, a2: dFun, a3: dFun, a4: sFun): dFun = dFunDummy
  def helperIntDoubleDoubleBoolean2Double(a1: iFun, a2: dFun, a3: dFun, a4: bFun): dFun = dFunDummy
  def helperIntDoubleStringInt2Double(a1: iFun, a2: dFun, a3: sFun, a4: iFun): dFun = dFunDummy
  def helperIntDoubleStringLong2Double(a1: iFun, a2: dFun, a3: sFun, a4: lFun): dFun = dFunDummy
  def helperIntDoubleStringDouble2Double(a1: iFun, a2: dFun, a3: sFun, a4: dFun): dFun = dFunDummy
  def helperIntDoubleStringString2Double(a1: iFun, a2: dFun, a3: sFun, a4: sFun): dFun = dFunDummy
  def helperIntDoubleStringBoolean2Double(a1: iFun, a2: dFun, a3: sFun, a4: bFun): dFun = dFunDummy
  def helperIntDoubleBooleanInt2Double(a1: iFun, a2: dFun, a3: bFun, a4: iFun): dFun = dFunDummy
  def helperIntDoubleBooleanLong2Double(a1: iFun, a2: dFun, a3: bFun, a4: lFun): dFun = dFunDummy
  def helperIntDoubleBooleanDouble2Double(a1: iFun, a2: dFun, a3: bFun, a4: dFun): dFun = dFunDummy
  def helperIntDoubleBooleanString2Double(a1: iFun, a2: dFun, a3: bFun, a4: sFun): dFun = dFunDummy
  def helperIntDoubleBooleanBoolean2Double(a1: iFun, a2: dFun, a3: bFun, a4: bFun): dFun = dFunDummy
  def helperIntStringIntInt2Double(a1: iFun, a2: sFun, a3: iFun, a4: iFun): dFun = dFunDummy
  def helperIntStringIntLong2Double(a1: iFun, a2: sFun, a3: iFun, a4: lFun): dFun = dFunDummy
  def helperIntStringIntDouble2Double(a1: iFun, a2: sFun, a3: iFun, a4: dFun): dFun = dFunDummy
  def helperIntStringIntString2Double(a1: iFun, a2: sFun, a3: iFun, a4: sFun): dFun = dFunDummy
  def helperIntStringIntBoolean2Double(a1: iFun, a2: sFun, a3: iFun, a4: bFun): dFun = dFunDummy
  def helperIntStringLongInt2Double(a1: iFun, a2: sFun, a3: lFun, a4: iFun): dFun = dFunDummy
  def helperIntStringLongLong2Double(a1: iFun, a2: sFun, a3: lFun, a4: lFun): dFun = dFunDummy
  def helperIntStringLongDouble2Double(a1: iFun, a2: sFun, a3: lFun, a4: dFun): dFun = dFunDummy
  def helperIntStringLongString2Double(a1: iFun, a2: sFun, a3: lFun, a4: sFun): dFun = dFunDummy
  def helperIntStringLongBoolean2Double(a1: iFun, a2: sFun, a3: lFun, a4: bFun): dFun = dFunDummy
  def helperIntStringDoubleInt2Double(a1: iFun, a2: sFun, a3: dFun, a4: iFun): dFun = dFunDummy
  def helperIntStringDoubleLong2Double(a1: iFun, a2: sFun, a3: dFun, a4: lFun): dFun = dFunDummy
  def helperIntStringDoubleDouble2Double(a1: iFun, a2: sFun, a3: dFun, a4: dFun): dFun = dFunDummy
  def helperIntStringDoubleString2Double(a1: iFun, a2: sFun, a3: dFun, a4: sFun): dFun = dFunDummy
  def helperIntStringDoubleBoolean2Double(a1: iFun, a2: sFun, a3: dFun, a4: bFun): dFun = dFunDummy
  def helperIntStringStringInt2Double(a1: iFun, a2: sFun, a3: sFun, a4: iFun): dFun = dFunDummy
  def helperIntStringStringLong2Double(a1: iFun, a2: sFun, a3: sFun, a4: lFun): dFun = dFunDummy
  def helperIntStringStringDouble2Double(a1: iFun, a2: sFun, a3: sFun, a4: dFun): dFun = dFunDummy
  def helperIntStringStringString2Double(a1: iFun, a2: sFun, a3: sFun, a4: sFun): dFun = dFunDummy
  def helperIntStringStringBoolean2Double(a1: iFun, a2: sFun, a3: sFun, a4: bFun): dFun = dFunDummy
  def helperIntStringBooleanInt2Double(a1: iFun, a2: sFun, a3: bFun, a4: iFun): dFun = dFunDummy
  def helperIntStringBooleanLong2Double(a1: iFun, a2: sFun, a3: bFun, a4: lFun): dFun = dFunDummy
  def helperIntStringBooleanDouble2Double(a1: iFun, a2: sFun, a3: bFun, a4: dFun): dFun = dFunDummy
  def helperIntStringBooleanString2Double(a1: iFun, a2: sFun, a3: bFun, a4: sFun): dFun = dFunDummy
  def helperIntStringBooleanBoolean2Double(a1: iFun, a2: sFun, a3: bFun, a4: bFun): dFun = dFunDummy
  def helperIntBooleanIntInt2Double(a1: iFun, a2: bFun, a3: iFun, a4: iFun): dFun = dFunDummy
  def helperIntBooleanIntLong2Double(a1: iFun, a2: bFun, a3: iFun, a4: lFun): dFun = dFunDummy
  def helperIntBooleanIntDouble2Double(a1: iFun, a2: bFun, a3: iFun, a4: dFun): dFun = dFunDummy
  def helperIntBooleanIntString2Double(a1: iFun, a2: bFun, a3: iFun, a4: sFun): dFun = dFunDummy
  def helperIntBooleanIntBoolean2Double(a1: iFun, a2: bFun, a3: iFun, a4: bFun): dFun = dFunDummy
  def helperIntBooleanLongInt2Double(a1: iFun, a2: bFun, a3: lFun, a4: iFun): dFun = dFunDummy
  def helperIntBooleanLongLong2Double(a1: iFun, a2: bFun, a3: lFun, a4: lFun): dFun = dFunDummy
  def helperIntBooleanLongDouble2Double(a1: iFun, a2: bFun, a3: lFun, a4: dFun): dFun = dFunDummy
  def helperIntBooleanLongString2Double(a1: iFun, a2: bFun, a3: lFun, a4: sFun): dFun = dFunDummy
  def helperIntBooleanLongBoolean2Double(a1: iFun, a2: bFun, a3: lFun, a4: bFun): dFun = dFunDummy
  def helperIntBooleanDoubleInt2Double(a1: iFun, a2: bFun, a3: dFun, a4: iFun): dFun = dFunDummy
  def helperIntBooleanDoubleLong2Double(a1: iFun, a2: bFun, a3: dFun, a4: lFun): dFun = dFunDummy
  def helperIntBooleanDoubleDouble2Double(a1: iFun, a2: bFun, a3: dFun, a4: dFun): dFun = dFunDummy
  def helperIntBooleanDoubleString2Double(a1: iFun, a2: bFun, a3: dFun, a4: sFun): dFun = dFunDummy
  def helperIntBooleanDoubleBoolean2Double(a1: iFun, a2: bFun, a3: dFun, a4: bFun): dFun = dFunDummy
  def helperIntBooleanStringInt2Double(a1: iFun, a2: bFun, a3: sFun, a4: iFun): dFun = dFunDummy
  def helperIntBooleanStringLong2Double(a1: iFun, a2: bFun, a3: sFun, a4: lFun): dFun = dFunDummy
  def helperIntBooleanStringDouble2Double(a1: iFun, a2: bFun, a3: sFun, a4: dFun): dFun = dFunDummy
  def helperIntBooleanStringString2Double(a1: iFun, a2: bFun, a3: sFun, a4: sFun): dFun = dFunDummy
  def helperIntBooleanStringBoolean2Double(a1: iFun, a2: bFun, a3: sFun, a4: bFun): dFun = dFunDummy
  def helperIntBooleanBooleanInt2Double(a1: iFun, a2: bFun, a3: bFun, a4: iFun): dFun = dFunDummy
  def helperIntBooleanBooleanLong2Double(a1: iFun, a2: bFun, a3: bFun, a4: lFun): dFun = dFunDummy
  def helperIntBooleanBooleanDouble2Double(a1: iFun, a2: bFun, a3: bFun, a4: dFun): dFun = dFunDummy
  def helperIntBooleanBooleanString2Double(a1: iFun, a2: bFun, a3: bFun, a4: sFun): dFun = dFunDummy
  def helperIntBooleanBooleanBoolean2Double(a1: iFun, a2: bFun, a3: bFun, a4: bFun): dFun = dFunDummy
  def helperLongIntIntInt2Double(a1: lFun, a2: iFun, a3: iFun, a4: iFun): dFun = dFunDummy
  def helperLongIntIntLong2Double(a1: lFun, a2: iFun, a3: iFun, a4: lFun): dFun = dFunDummy
  def helperLongIntIntDouble2Double(a1: lFun, a2: iFun, a3: iFun, a4: dFun): dFun = dFunDummy
  def helperLongIntIntString2Double(a1: lFun, a2: iFun, a3: iFun, a4: sFun): dFun = dFunDummy
  def helperLongIntIntBoolean2Double(a1: lFun, a2: iFun, a3: iFun, a4: bFun): dFun = dFunDummy
  def helperLongIntLongInt2Double(a1: lFun, a2: iFun, a3: lFun, a4: iFun): dFun = dFunDummy
  def helperLongIntLongLong2Double(a1: lFun, a2: iFun, a3: lFun, a4: lFun): dFun = dFunDummy
  def helperLongIntLongDouble2Double(a1: lFun, a2: iFun, a3: lFun, a4: dFun): dFun = dFunDummy
  def helperLongIntLongString2Double(a1: lFun, a2: iFun, a3: lFun, a4: sFun): dFun = dFunDummy
  def helperLongIntLongBoolean2Double(a1: lFun, a2: iFun, a3: lFun, a4: bFun): dFun = dFunDummy
  def helperLongIntDoubleInt2Double(a1: lFun, a2: iFun, a3: dFun, a4: iFun): dFun = dFunDummy
  def helperLongIntDoubleLong2Double(a1: lFun, a2: iFun, a3: dFun, a4: lFun): dFun = dFunDummy
  def helperLongIntDoubleDouble2Double(a1: lFun, a2: iFun, a3: dFun, a4: dFun): dFun = dFunDummy
  def helperLongIntDoubleString2Double(a1: lFun, a2: iFun, a3: dFun, a4: sFun): dFun = dFunDummy
  def helperLongIntDoubleBoolean2Double(a1: lFun, a2: iFun, a3: dFun, a4: bFun): dFun = dFunDummy
  def helperLongIntStringInt2Double(a1: lFun, a2: iFun, a3: sFun, a4: iFun): dFun = dFunDummy
  def helperLongIntStringLong2Double(a1: lFun, a2: iFun, a3: sFun, a4: lFun): dFun = dFunDummy
  def helperLongIntStringDouble2Double(a1: lFun, a2: iFun, a3: sFun, a4: dFun): dFun = dFunDummy
  def helperLongIntStringString2Double(a1: lFun, a2: iFun, a3: sFun, a4: sFun): dFun = dFunDummy
  def helperLongIntStringBoolean2Double(a1: lFun, a2: iFun, a3: sFun, a4: bFun): dFun = dFunDummy
  def helperLongIntBooleanInt2Double(a1: lFun, a2: iFun, a3: bFun, a4: iFun): dFun = dFunDummy
  def helperLongIntBooleanLong2Double(a1: lFun, a2: iFun, a3: bFun, a4: lFun): dFun = dFunDummy
  def helperLongIntBooleanDouble2Double(a1: lFun, a2: iFun, a3: bFun, a4: dFun): dFun = dFunDummy
  def helperLongIntBooleanString2Double(a1: lFun, a2: iFun, a3: bFun, a4: sFun): dFun = dFunDummy
  def helperLongIntBooleanBoolean2Double(a1: lFun, a2: iFun, a3: bFun, a4: bFun): dFun = dFunDummy
  def helperLongLongIntInt2Double(a1: lFun, a2: lFun, a3: iFun, a4: iFun): dFun = dFunDummy
  def helperLongLongIntLong2Double(a1: lFun, a2: lFun, a3: iFun, a4: lFun): dFun = dFunDummy
  def helperLongLongIntDouble2Double(a1: lFun, a2: lFun, a3: iFun, a4: dFun): dFun = dFunDummy
  def helperLongLongIntString2Double(a1: lFun, a2: lFun, a3: iFun, a4: sFun): dFun = dFunDummy
  def helperLongLongIntBoolean2Double(a1: lFun, a2: lFun, a3: iFun, a4: bFun): dFun = dFunDummy
  def helperLongLongLongInt2Double(a1: lFun, a2: lFun, a3: lFun, a4: iFun): dFun = dFunDummy
  def helperLongLongLongLong2Double(a1: lFun, a2: lFun, a3: lFun, a4: lFun): dFun = dFunDummy
  def helperLongLongLongDouble2Double(a1: lFun, a2: lFun, a3: lFun, a4: dFun): dFun = dFunDummy
  def helperLongLongLongString2Double(a1: lFun, a2: lFun, a3: lFun, a4: sFun): dFun = dFunDummy
  def helperLongLongLongBoolean2Double(a1: lFun, a2: lFun, a3: lFun, a4: bFun): dFun = dFunDummy
  def helperLongLongDoubleInt2Double(a1: lFun, a2: lFun, a3: dFun, a4: iFun): dFun = dFunDummy
  def helperLongLongDoubleLong2Double(a1: lFun, a2: lFun, a3: dFun, a4: lFun): dFun = dFunDummy
  def helperLongLongDoubleDouble2Double(a1: lFun, a2: lFun, a3: dFun, a4: dFun): dFun = dFunDummy
  def helperLongLongDoubleString2Double(a1: lFun, a2: lFun, a3: dFun, a4: sFun): dFun = dFunDummy
  def helperLongLongDoubleBoolean2Double(a1: lFun, a2: lFun, a3: dFun, a4: bFun): dFun = dFunDummy
  def helperLongLongStringInt2Double(a1: lFun, a2: lFun, a3: sFun, a4: iFun): dFun = dFunDummy
  def helperLongLongStringLong2Double(a1: lFun, a2: lFun, a3: sFun, a4: lFun): dFun = dFunDummy
  def helperLongLongStringDouble2Double(a1: lFun, a2: lFun, a3: sFun, a4: dFun): dFun = dFunDummy
  def helperLongLongStringString2Double(a1: lFun, a2: lFun, a3: sFun, a4: sFun): dFun = dFunDummy
  def helperLongLongStringBoolean2Double(a1: lFun, a2: lFun, a3: sFun, a4: bFun): dFun = dFunDummy
  def helperLongLongBooleanInt2Double(a1: lFun, a2: lFun, a3: bFun, a4: iFun): dFun = dFunDummy
  def helperLongLongBooleanLong2Double(a1: lFun, a2: lFun, a3: bFun, a4: lFun): dFun = dFunDummy
  def helperLongLongBooleanDouble2Double(a1: lFun, a2: lFun, a3: bFun, a4: dFun): dFun = dFunDummy
  def helperLongLongBooleanString2Double(a1: lFun, a2: lFun, a3: bFun, a4: sFun): dFun = dFunDummy
  def helperLongLongBooleanBoolean2Double(a1: lFun, a2: lFun, a3: bFun, a4: bFun): dFun = dFunDummy
  def helperLongDoubleIntInt2Double(a1: lFun, a2: dFun, a3: iFun, a4: iFun): dFun = dFunDummy
  def helperLongDoubleIntLong2Double(a1: lFun, a2: dFun, a3: iFun, a4: lFun): dFun = dFunDummy
  def helperLongDoubleIntDouble2Double(a1: lFun, a2: dFun, a3: iFun, a4: dFun): dFun = dFunDummy
  def helperLongDoubleIntString2Double(a1: lFun, a2: dFun, a3: iFun, a4: sFun): dFun = dFunDummy
  def helperLongDoubleIntBoolean2Double(a1: lFun, a2: dFun, a3: iFun, a4: bFun): dFun = dFunDummy
  def helperLongDoubleLongInt2Double(a1: lFun, a2: dFun, a3: lFun, a4: iFun): dFun = dFunDummy
  def helperLongDoubleLongLong2Double(a1: lFun, a2: dFun, a3: lFun, a4: lFun): dFun = dFunDummy
  def helperLongDoubleLongDouble2Double(a1: lFun, a2: dFun, a3: lFun, a4: dFun): dFun = dFunDummy
  def helperLongDoubleLongString2Double(a1: lFun, a2: dFun, a3: lFun, a4: sFun): dFun = dFunDummy
  def helperLongDoubleLongBoolean2Double(a1: lFun, a2: dFun, a3: lFun, a4: bFun): dFun = dFunDummy
  def helperLongDoubleDoubleInt2Double(a1: lFun, a2: dFun, a3: dFun, a4: iFun): dFun = dFunDummy
  def helperLongDoubleDoubleLong2Double(a1: lFun, a2: dFun, a3: dFun, a4: lFun): dFun = dFunDummy
  def helperLongDoubleDoubleDouble2Double(a1: lFun, a2: dFun, a3: dFun, a4: dFun): dFun = dFunDummy
  def helperLongDoubleDoubleString2Double(a1: lFun, a2: dFun, a3: dFun, a4: sFun): dFun = dFunDummy
  def helperLongDoubleDoubleBoolean2Double(a1: lFun, a2: dFun, a3: dFun, a4: bFun): dFun = dFunDummy
  def helperLongDoubleStringInt2Double(a1: lFun, a2: dFun, a3: sFun, a4: iFun): dFun = dFunDummy
  def helperLongDoubleStringLong2Double(a1: lFun, a2: dFun, a3: sFun, a4: lFun): dFun = dFunDummy
  def helperLongDoubleStringDouble2Double(a1: lFun, a2: dFun, a3: sFun, a4: dFun): dFun = dFunDummy
  def helperLongDoubleStringString2Double(a1: lFun, a2: dFun, a3: sFun, a4: sFun): dFun = dFunDummy
  def helperLongDoubleStringBoolean2Double(a1: lFun, a2: dFun, a3: sFun, a4: bFun): dFun = dFunDummy
  def helperLongDoubleBooleanInt2Double(a1: lFun, a2: dFun, a3: bFun, a4: iFun): dFun = dFunDummy
  def helperLongDoubleBooleanLong2Double(a1: lFun, a2: dFun, a3: bFun, a4: lFun): dFun = dFunDummy
  def helperLongDoubleBooleanDouble2Double(a1: lFun, a2: dFun, a3: bFun, a4: dFun): dFun = dFunDummy
  def helperLongDoubleBooleanString2Double(a1: lFun, a2: dFun, a3: bFun, a4: sFun): dFun = dFunDummy
  def helperLongDoubleBooleanBoolean2Double(a1: lFun, a2: dFun, a3: bFun, a4: bFun): dFun = dFunDummy
  def helperLongStringIntInt2Double(a1: lFun, a2: sFun, a3: iFun, a4: iFun): dFun = dFunDummy
  def helperLongStringIntLong2Double(a1: lFun, a2: sFun, a3: iFun, a4: lFun): dFun = dFunDummy
  def helperLongStringIntDouble2Double(a1: lFun, a2: sFun, a3: iFun, a4: dFun): dFun = dFunDummy
  def helperLongStringIntString2Double(a1: lFun, a2: sFun, a3: iFun, a4: sFun): dFun = dFunDummy
  def helperLongStringIntBoolean2Double(a1: lFun, a2: sFun, a3: iFun, a4: bFun): dFun = dFunDummy
  def helperLongStringLongInt2Double(a1: lFun, a2: sFun, a3: lFun, a4: iFun): dFun = dFunDummy
  def helperLongStringLongLong2Double(a1: lFun, a2: sFun, a3: lFun, a4: lFun): dFun = dFunDummy
  def helperLongStringLongDouble2Double(a1: lFun, a2: sFun, a3: lFun, a4: dFun): dFun = dFunDummy
  def helperLongStringLongString2Double(a1: lFun, a2: sFun, a3: lFun, a4: sFun): dFun = dFunDummy
  def helperLongStringLongBoolean2Double(a1: lFun, a2: sFun, a3: lFun, a4: bFun): dFun = dFunDummy
  def helperLongStringDoubleInt2Double(a1: lFun, a2: sFun, a3: dFun, a4: iFun): dFun = dFunDummy
  def helperLongStringDoubleLong2Double(a1: lFun, a2: sFun, a3: dFun, a4: lFun): dFun = dFunDummy
  def helperLongStringDoubleDouble2Double(a1: lFun, a2: sFun, a3: dFun, a4: dFun): dFun = dFunDummy
  def helperLongStringDoubleString2Double(a1: lFun, a2: sFun, a3: dFun, a4: sFun): dFun = dFunDummy
  def helperLongStringDoubleBoolean2Double(a1: lFun, a2: sFun, a3: dFun, a4: bFun): dFun = dFunDummy
  def helperLongStringStringInt2Double(a1: lFun, a2: sFun, a3: sFun, a4: iFun): dFun = dFunDummy
  def helperLongStringStringLong2Double(a1: lFun, a2: sFun, a3: sFun, a4: lFun): dFun = dFunDummy
  def helperLongStringStringDouble2Double(a1: lFun, a2: sFun, a3: sFun, a4: dFun): dFun = dFunDummy
  def helperLongStringStringString2Double(a1: lFun, a2: sFun, a3: sFun, a4: sFun): dFun = dFunDummy
  def helperLongStringStringBoolean2Double(a1: lFun, a2: sFun, a3: sFun, a4: bFun): dFun = dFunDummy
  def helperLongStringBooleanInt2Double(a1: lFun, a2: sFun, a3: bFun, a4: iFun): dFun = dFunDummy
  def helperLongStringBooleanLong2Double(a1: lFun, a2: sFun, a3: bFun, a4: lFun): dFun = dFunDummy
  def helperLongStringBooleanDouble2Double(a1: lFun, a2: sFun, a3: bFun, a4: dFun): dFun = dFunDummy
  def helperLongStringBooleanString2Double(a1: lFun, a2: sFun, a3: bFun, a4: sFun): dFun = dFunDummy
  def helperLongStringBooleanBoolean2Double(a1: lFun, a2: sFun, a3: bFun, a4: bFun): dFun = dFunDummy
  def helperLongBooleanIntInt2Double(a1: lFun, a2: bFun, a3: iFun, a4: iFun): dFun = dFunDummy
  def helperLongBooleanIntLong2Double(a1: lFun, a2: bFun, a3: iFun, a4: lFun): dFun = dFunDummy
  def helperLongBooleanIntDouble2Double(a1: lFun, a2: bFun, a3: iFun, a4: dFun): dFun = dFunDummy
  def helperLongBooleanIntString2Double(a1: lFun, a2: bFun, a3: iFun, a4: sFun): dFun = dFunDummy
  def helperLongBooleanIntBoolean2Double(a1: lFun, a2: bFun, a3: iFun, a4: bFun): dFun = dFunDummy
  def helperLongBooleanLongInt2Double(a1: lFun, a2: bFun, a3: lFun, a4: iFun): dFun = dFunDummy
  def helperLongBooleanLongLong2Double(a1: lFun, a2: bFun, a3: lFun, a4: lFun): dFun = dFunDummy
  def helperLongBooleanLongDouble2Double(a1: lFun, a2: bFun, a3: lFun, a4: dFun): dFun = dFunDummy
  def helperLongBooleanLongString2Double(a1: lFun, a2: bFun, a3: lFun, a4: sFun): dFun = dFunDummy
  def helperLongBooleanLongBoolean2Double(a1: lFun, a2: bFun, a3: lFun, a4: bFun): dFun = dFunDummy
  def helperLongBooleanDoubleInt2Double(a1: lFun, a2: bFun, a3: dFun, a4: iFun): dFun = dFunDummy
  def helperLongBooleanDoubleLong2Double(a1: lFun, a2: bFun, a3: dFun, a4: lFun): dFun = dFunDummy
  def helperLongBooleanDoubleDouble2Double(a1: lFun, a2: bFun, a3: dFun, a4: dFun): dFun = dFunDummy
  def helperLongBooleanDoubleString2Double(a1: lFun, a2: bFun, a3: dFun, a4: sFun): dFun = dFunDummy
  def helperLongBooleanDoubleBoolean2Double(a1: lFun, a2: bFun, a3: dFun, a4: bFun): dFun = dFunDummy
  def helperLongBooleanStringInt2Double(a1: lFun, a2: bFun, a3: sFun, a4: iFun): dFun = dFunDummy
  def helperLongBooleanStringLong2Double(a1: lFun, a2: bFun, a3: sFun, a4: lFun): dFun = dFunDummy
  def helperLongBooleanStringDouble2Double(a1: lFun, a2: bFun, a3: sFun, a4: dFun): dFun = dFunDummy
  def helperLongBooleanStringString2Double(a1: lFun, a2: bFun, a3: sFun, a4: sFun): dFun = dFunDummy
  def helperLongBooleanStringBoolean2Double(a1: lFun, a2: bFun, a3: sFun, a4: bFun): dFun = dFunDummy
  def helperLongBooleanBooleanInt2Double(a1: lFun, a2: bFun, a3: bFun, a4: iFun): dFun = dFunDummy
  def helperLongBooleanBooleanLong2Double(a1: lFun, a2: bFun, a3: bFun, a4: lFun): dFun = dFunDummy
  def helperLongBooleanBooleanDouble2Double(a1: lFun, a2: bFun, a3: bFun, a4: dFun): dFun = dFunDummy
  def helperLongBooleanBooleanString2Double(a1: lFun, a2: bFun, a3: bFun, a4: sFun): dFun = dFunDummy
  def helperLongBooleanBooleanBoolean2Double(a1: lFun, a2: bFun, a3: bFun, a4: bFun): dFun = dFunDummy
  def helperDoubleIntIntInt2Double(a1: dFun, a2: iFun, a3: iFun, a4: iFun): dFun = dFunDummy
  def helperDoubleIntIntLong2Double(a1: dFun, a2: iFun, a3: iFun, a4: lFun): dFun = dFunDummy
  def helperDoubleIntIntDouble2Double(a1: dFun, a2: iFun, a3: iFun, a4: dFun): dFun = dFunDummy
  def helperDoubleIntIntString2Double(a1: dFun, a2: iFun, a3: iFun, a4: sFun): dFun = dFunDummy
  def helperDoubleIntIntBoolean2Double(a1: dFun, a2: iFun, a3: iFun, a4: bFun): dFun = dFunDummy
  def helperDoubleIntLongInt2Double(a1: dFun, a2: iFun, a3: lFun, a4: iFun): dFun = dFunDummy
  def helperDoubleIntLongLong2Double(a1: dFun, a2: iFun, a3: lFun, a4: lFun): dFun = dFunDummy
  def helperDoubleIntLongDouble2Double(a1: dFun, a2: iFun, a3: lFun, a4: dFun): dFun = dFunDummy
  def helperDoubleIntLongString2Double(a1: dFun, a2: iFun, a3: lFun, a4: sFun): dFun = dFunDummy
  def helperDoubleIntLongBoolean2Double(a1: dFun, a2: iFun, a3: lFun, a4: bFun): dFun = dFunDummy
  def helperDoubleIntDoubleInt2Double(a1: dFun, a2: iFun, a3: dFun, a4: iFun): dFun = dFunDummy
  def helperDoubleIntDoubleLong2Double(a1: dFun, a2: iFun, a3: dFun, a4: lFun): dFun = dFunDummy
  def helperDoubleIntDoubleDouble2Double(a1: dFun, a2: iFun, a3: dFun, a4: dFun): dFun = dFunDummy
  def helperDoubleIntDoubleString2Double(a1: dFun, a2: iFun, a3: dFun, a4: sFun): dFun = dFunDummy
  def helperDoubleIntDoubleBoolean2Double(a1: dFun, a2: iFun, a3: dFun, a4: bFun): dFun = dFunDummy
  def helperDoubleIntStringInt2Double(a1: dFun, a2: iFun, a3: sFun, a4: iFun): dFun = dFunDummy
  def helperDoubleIntStringLong2Double(a1: dFun, a2: iFun, a3: sFun, a4: lFun): dFun = dFunDummy
  def helperDoubleIntStringDouble2Double(a1: dFun, a2: iFun, a3: sFun, a4: dFun): dFun = dFunDummy
  def helperDoubleIntStringString2Double(a1: dFun, a2: iFun, a3: sFun, a4: sFun): dFun = dFunDummy
  def helperDoubleIntStringBoolean2Double(a1: dFun, a2: iFun, a3: sFun, a4: bFun): dFun = dFunDummy
  def helperDoubleIntBooleanInt2Double(a1: dFun, a2: iFun, a3: bFun, a4: iFun): dFun = dFunDummy
  def helperDoubleIntBooleanLong2Double(a1: dFun, a2: iFun, a3: bFun, a4: lFun): dFun = dFunDummy
  def helperDoubleIntBooleanDouble2Double(a1: dFun, a2: iFun, a3: bFun, a4: dFun): dFun = dFunDummy
  def helperDoubleIntBooleanString2Double(a1: dFun, a2: iFun, a3: bFun, a4: sFun): dFun = dFunDummy
  def helperDoubleIntBooleanBoolean2Double(a1: dFun, a2: iFun, a3: bFun, a4: bFun): dFun = dFunDummy
  def helperDoubleLongIntInt2Double(a1: dFun, a2: lFun, a3: iFun, a4: iFun): dFun = dFunDummy
  def helperDoubleLongIntLong2Double(a1: dFun, a2: lFun, a3: iFun, a4: lFun): dFun = dFunDummy
  def helperDoubleLongIntDouble2Double(a1: dFun, a2: lFun, a3: iFun, a4: dFun): dFun = dFunDummy
  def helperDoubleLongIntString2Double(a1: dFun, a2: lFun, a3: iFun, a4: sFun): dFun = dFunDummy
  def helperDoubleLongIntBoolean2Double(a1: dFun, a2: lFun, a3: iFun, a4: bFun): dFun = dFunDummy
  def helperDoubleLongLongInt2Double(a1: dFun, a2: lFun, a3: lFun, a4: iFun): dFun = dFunDummy
  def helperDoubleLongLongLong2Double(a1: dFun, a2: lFun, a3: lFun, a4: lFun): dFun = dFunDummy
  def helperDoubleLongLongDouble2Double(a1: dFun, a2: lFun, a3: lFun, a4: dFun): dFun = dFunDummy
  def helperDoubleLongLongString2Double(a1: dFun, a2: lFun, a3: lFun, a4: sFun): dFun = dFunDummy
  def helperDoubleLongLongBoolean2Double(a1: dFun, a2: lFun, a3: lFun, a4: bFun): dFun = dFunDummy
  def helperDoubleLongDoubleInt2Double(a1: dFun, a2: lFun, a3: dFun, a4: iFun): dFun = dFunDummy
  def helperDoubleLongDoubleLong2Double(a1: dFun, a2: lFun, a3: dFun, a4: lFun): dFun = dFunDummy
  def helperDoubleLongDoubleDouble2Double(a1: dFun, a2: lFun, a3: dFun, a4: dFun): dFun = dFunDummy
  def helperDoubleLongDoubleString2Double(a1: dFun, a2: lFun, a3: dFun, a4: sFun): dFun = dFunDummy
  def helperDoubleLongDoubleBoolean2Double(a1: dFun, a2: lFun, a3: dFun, a4: bFun): dFun = dFunDummy
  def helperDoubleLongStringInt2Double(a1: dFun, a2: lFun, a3: sFun, a4: iFun): dFun = dFunDummy
  def helperDoubleLongStringLong2Double(a1: dFun, a2: lFun, a3: sFun, a4: lFun): dFun = dFunDummy
  def helperDoubleLongStringDouble2Double(a1: dFun, a2: lFun, a3: sFun, a4: dFun): dFun = dFunDummy
  def helperDoubleLongStringString2Double(a1: dFun, a2: lFun, a3: sFun, a4: sFun): dFun = dFunDummy
  def helperDoubleLongStringBoolean2Double(a1: dFun, a2: lFun, a3: sFun, a4: bFun): dFun = dFunDummy
  def helperDoubleLongBooleanInt2Double(a1: dFun, a2: lFun, a3: bFun, a4: iFun): dFun = dFunDummy
  def helperDoubleLongBooleanLong2Double(a1: dFun, a2: lFun, a3: bFun, a4: lFun): dFun = dFunDummy
  def helperDoubleLongBooleanDouble2Double(a1: dFun, a2: lFun, a3: bFun, a4: dFun): dFun = dFunDummy
  def helperDoubleLongBooleanString2Double(a1: dFun, a2: lFun, a3: bFun, a4: sFun): dFun = dFunDummy
  def helperDoubleLongBooleanBoolean2Double(a1: dFun, a2: lFun, a3: bFun, a4: bFun): dFun = dFunDummy
  def helperDoubleDoubleIntInt2Double(a1: dFun, a2: dFun, a3: iFun, a4: iFun): dFun = dFunDummy
  def helperDoubleDoubleIntLong2Double(a1: dFun, a2: dFun, a3: iFun, a4: lFun): dFun = dFunDummy
  def helperDoubleDoubleIntDouble2Double(a1: dFun, a2: dFun, a3: iFun, a4: dFun): dFun = dFunDummy
  def helperDoubleDoubleIntString2Double(a1: dFun, a2: dFun, a3: iFun, a4: sFun): dFun = dFunDummy
  def helperDoubleDoubleIntBoolean2Double(a1: dFun, a2: dFun, a3: iFun, a4: bFun): dFun = dFunDummy
  def helperDoubleDoubleLongInt2Double(a1: dFun, a2: dFun, a3: lFun, a4: iFun): dFun = dFunDummy
  def helperDoubleDoubleLongLong2Double(a1: dFun, a2: dFun, a3: lFun, a4: lFun): dFun = dFunDummy
  def helperDoubleDoubleLongDouble2Double(a1: dFun, a2: dFun, a3: lFun, a4: dFun): dFun = dFunDummy
  def helperDoubleDoubleLongString2Double(a1: dFun, a2: dFun, a3: lFun, a4: sFun): dFun = dFunDummy
  def helperDoubleDoubleLongBoolean2Double(a1: dFun, a2: dFun, a3: lFun, a4: bFun): dFun = dFunDummy
  def helperDoubleDoubleDoubleInt2Double(a1: dFun, a2: dFun, a3: dFun, a4: iFun): dFun = dFunDummy
  def helperDoubleDoubleDoubleLong2Double(a1: dFun, a2: dFun, a3: dFun, a4: lFun): dFun = dFunDummy
  def helperDoubleDoubleDoubleDouble2Double(a1: dFun, a2: dFun, a3: dFun, a4: dFun): dFun = dFunDummy
  def helperDoubleDoubleDoubleString2Double(a1: dFun, a2: dFun, a3: dFun, a4: sFun): dFun = dFunDummy
  def helperDoubleDoubleDoubleBoolean2Double(a1: dFun, a2: dFun, a3: dFun, a4: bFun): dFun = dFunDummy
  def helperDoubleDoubleStringInt2Double(a1: dFun, a2: dFun, a3: sFun, a4: iFun): dFun = dFunDummy
  def helperDoubleDoubleStringLong2Double(a1: dFun, a2: dFun, a3: sFun, a4: lFun): dFun = dFunDummy
  def helperDoubleDoubleStringDouble2Double(a1: dFun, a2: dFun, a3: sFun, a4: dFun): dFun = dFunDummy
  def helperDoubleDoubleStringString2Double(a1: dFun, a2: dFun, a3: sFun, a4: sFun): dFun = dFunDummy
  def helperDoubleDoubleStringBoolean2Double(a1: dFun, a2: dFun, a3: sFun, a4: bFun): dFun = dFunDummy
  def helperDoubleDoubleBooleanInt2Double(a1: dFun, a2: dFun, a3: bFun, a4: iFun): dFun = dFunDummy
  def helperDoubleDoubleBooleanLong2Double(a1: dFun, a2: dFun, a3: bFun, a4: lFun): dFun = dFunDummy
  def helperDoubleDoubleBooleanDouble2Double(a1: dFun, a2: dFun, a3: bFun, a4: dFun): dFun = dFunDummy
  def helperDoubleDoubleBooleanString2Double(a1: dFun, a2: dFun, a3: bFun, a4: sFun): dFun = dFunDummy
  def helperDoubleDoubleBooleanBoolean2Double(a1: dFun, a2: dFun, a3: bFun, a4: bFun): dFun = dFunDummy
  def helperDoubleStringIntInt2Double(a1: dFun, a2: sFun, a3: iFun, a4: iFun): dFun = dFunDummy
  def helperDoubleStringIntLong2Double(a1: dFun, a2: sFun, a3: iFun, a4: lFun): dFun = dFunDummy
  def helperDoubleStringIntDouble2Double(a1: dFun, a2: sFun, a3: iFun, a4: dFun): dFun = dFunDummy
  def helperDoubleStringIntString2Double(a1: dFun, a2: sFun, a3: iFun, a4: sFun): dFun = dFunDummy
  def helperDoubleStringIntBoolean2Double(a1: dFun, a2: sFun, a3: iFun, a4: bFun): dFun = dFunDummy
  def helperDoubleStringLongInt2Double(a1: dFun, a2: sFun, a3: lFun, a4: iFun): dFun = dFunDummy
  def helperDoubleStringLongLong2Double(a1: dFun, a2: sFun, a3: lFun, a4: lFun): dFun = dFunDummy
  def helperDoubleStringLongDouble2Double(a1: dFun, a2: sFun, a3: lFun, a4: dFun): dFun = dFunDummy
  def helperDoubleStringLongString2Double(a1: dFun, a2: sFun, a3: lFun, a4: sFun): dFun = dFunDummy
  def helperDoubleStringLongBoolean2Double(a1: dFun, a2: sFun, a3: lFun, a4: bFun): dFun = dFunDummy
  def helperDoubleStringDoubleInt2Double(a1: dFun, a2: sFun, a3: dFun, a4: iFun): dFun = dFunDummy
  def helperDoubleStringDoubleLong2Double(a1: dFun, a2: sFun, a3: dFun, a4: lFun): dFun = dFunDummy
  def helperDoubleStringDoubleDouble2Double(a1: dFun, a2: sFun, a3: dFun, a4: dFun): dFun = dFunDummy
  def helperDoubleStringDoubleString2Double(a1: dFun, a2: sFun, a3: dFun, a4: sFun): dFun = dFunDummy
  def helperDoubleStringDoubleBoolean2Double(a1: dFun, a2: sFun, a3: dFun, a4: bFun): dFun = dFunDummy
  def helperDoubleStringStringInt2Double(a1: dFun, a2: sFun, a3: sFun, a4: iFun): dFun = dFunDummy
  def helperDoubleStringStringLong2Double(a1: dFun, a2: sFun, a3: sFun, a4: lFun): dFun = dFunDummy
  def helperDoubleStringStringDouble2Double(a1: dFun, a2: sFun, a3: sFun, a4: dFun): dFun = dFunDummy
  def helperDoubleStringStringString2Double(a1: dFun, a2: sFun, a3: sFun, a4: sFun): dFun = dFunDummy
  def helperDoubleStringStringBoolean2Double(a1: dFun, a2: sFun, a3: sFun, a4: bFun): dFun = dFunDummy
  def helperDoubleStringBooleanInt2Double(a1: dFun, a2: sFun, a3: bFun, a4: iFun): dFun = dFunDummy
  def helperDoubleStringBooleanLong2Double(a1: dFun, a2: sFun, a3: bFun, a4: lFun): dFun = dFunDummy
  def helperDoubleStringBooleanDouble2Double(a1: dFun, a2: sFun, a3: bFun, a4: dFun): dFun = dFunDummy
  def helperDoubleStringBooleanString2Double(a1: dFun, a2: sFun, a3: bFun, a4: sFun): dFun = dFunDummy
  def helperDoubleStringBooleanBoolean2Double(a1: dFun, a2: sFun, a3: bFun, a4: bFun): dFun = dFunDummy
  def helperDoubleBooleanIntInt2Double(a1: dFun, a2: bFun, a3: iFun, a4: iFun): dFun = dFunDummy
  def helperDoubleBooleanIntLong2Double(a1: dFun, a2: bFun, a3: iFun, a4: lFun): dFun = dFunDummy
  def helperDoubleBooleanIntDouble2Double(a1: dFun, a2: bFun, a3: iFun, a4: dFun): dFun = dFunDummy
  def helperDoubleBooleanIntString2Double(a1: dFun, a2: bFun, a3: iFun, a4: sFun): dFun = dFunDummy
  def helperDoubleBooleanIntBoolean2Double(a1: dFun, a2: bFun, a3: iFun, a4: bFun): dFun = dFunDummy
  def helperDoubleBooleanLongInt2Double(a1: dFun, a2: bFun, a3: lFun, a4: iFun): dFun = dFunDummy
  def helperDoubleBooleanLongLong2Double(a1: dFun, a2: bFun, a3: lFun, a4: lFun): dFun = dFunDummy
  def helperDoubleBooleanLongDouble2Double(a1: dFun, a2: bFun, a3: lFun, a4: dFun): dFun = dFunDummy
  def helperDoubleBooleanLongString2Double(a1: dFun, a2: bFun, a3: lFun, a4: sFun): dFun = dFunDummy
  def helperDoubleBooleanLongBoolean2Double(a1: dFun, a2: bFun, a3: lFun, a4: bFun): dFun = dFunDummy
  def helperDoubleBooleanDoubleInt2Double(a1: dFun, a2: bFun, a3: dFun, a4: iFun): dFun = dFunDummy
  def helperDoubleBooleanDoubleLong2Double(a1: dFun, a2: bFun, a3: dFun, a4: lFun): dFun = dFunDummy
  def helperDoubleBooleanDoubleDouble2Double(a1: dFun, a2: bFun, a3: dFun, a4: dFun): dFun = dFunDummy
  def helperDoubleBooleanDoubleString2Double(a1: dFun, a2: bFun, a3: dFun, a4: sFun): dFun = dFunDummy
  def helperDoubleBooleanDoubleBoolean2Double(a1: dFun, a2: bFun, a3: dFun, a4: bFun): dFun = dFunDummy
  def helperDoubleBooleanStringInt2Double(a1: dFun, a2: bFun, a3: sFun, a4: iFun): dFun = dFunDummy
  def helperDoubleBooleanStringLong2Double(a1: dFun, a2: bFun, a3: sFun, a4: lFun): dFun = dFunDummy
  def helperDoubleBooleanStringDouble2Double(a1: dFun, a2: bFun, a3: sFun, a4: dFun): dFun = dFunDummy
  def helperDoubleBooleanStringString2Double(a1: dFun, a2: bFun, a3: sFun, a4: sFun): dFun = dFunDummy
  def helperDoubleBooleanStringBoolean2Double(a1: dFun, a2: bFun, a3: sFun, a4: bFun): dFun = dFunDummy
  def helperDoubleBooleanBooleanInt2Double(a1: dFun, a2: bFun, a3: bFun, a4: iFun): dFun = dFunDummy
  def helperDoubleBooleanBooleanLong2Double(a1: dFun, a2: bFun, a3: bFun, a4: lFun): dFun = dFunDummy
  def helperDoubleBooleanBooleanDouble2Double(a1: dFun, a2: bFun, a3: bFun, a4: dFun): dFun = dFunDummy
  def helperDoubleBooleanBooleanString2Double(a1: dFun, a2: bFun, a3: bFun, a4: sFun): dFun = dFunDummy
  def helperDoubleBooleanBooleanBoolean2Double(a1: dFun, a2: bFun, a3: bFun, a4: bFun): dFun = dFunDummy
  def helperStringIntIntInt2Double(a1: sFun, a2: iFun, a3: iFun, a4: iFun): dFun = dFunDummy
  def helperStringIntIntLong2Double(a1: sFun, a2: iFun, a3: iFun, a4: lFun): dFun = dFunDummy
  def helperStringIntIntDouble2Double(a1: sFun, a2: iFun, a3: iFun, a4: dFun): dFun = dFunDummy
  def helperStringIntIntString2Double(a1: sFun, a2: iFun, a3: iFun, a4: sFun): dFun = dFunDummy
  def helperStringIntIntBoolean2Double(a1: sFun, a2: iFun, a3: iFun, a4: bFun): dFun = dFunDummy
  def helperStringIntLongInt2Double(a1: sFun, a2: iFun, a3: lFun, a4: iFun): dFun = dFunDummy
  def helperStringIntLongLong2Double(a1: sFun, a2: iFun, a3: lFun, a4: lFun): dFun = dFunDummy
  def helperStringIntLongDouble2Double(a1: sFun, a2: iFun, a3: lFun, a4: dFun): dFun = dFunDummy
  def helperStringIntLongString2Double(a1: sFun, a2: iFun, a3: lFun, a4: sFun): dFun = dFunDummy
  def helperStringIntLongBoolean2Double(a1: sFun, a2: iFun, a3: lFun, a4: bFun): dFun = dFunDummy
  def helperStringIntDoubleInt2Double(a1: sFun, a2: iFun, a3: dFun, a4: iFun): dFun = dFunDummy
  def helperStringIntDoubleLong2Double(a1: sFun, a2: iFun, a3: dFun, a4: lFun): dFun = dFunDummy
  def helperStringIntDoubleDouble2Double(a1: sFun, a2: iFun, a3: dFun, a4: dFun): dFun = dFunDummy
  def helperStringIntDoubleString2Double(a1: sFun, a2: iFun, a3: dFun, a4: sFun): dFun = dFunDummy
  def helperStringIntDoubleBoolean2Double(a1: sFun, a2: iFun, a3: dFun, a4: bFun): dFun = dFunDummy
  def helperStringIntStringInt2Double(a1: sFun, a2: iFun, a3: sFun, a4: iFun): dFun = dFunDummy
  def helperStringIntStringLong2Double(a1: sFun, a2: iFun, a3: sFun, a4: lFun): dFun = dFunDummy
  def helperStringIntStringDouble2Double(a1: sFun, a2: iFun, a3: sFun, a4: dFun): dFun = dFunDummy
  def helperStringIntStringString2Double(a1: sFun, a2: iFun, a3: sFun, a4: sFun): dFun = dFunDummy
  def helperStringIntStringBoolean2Double(a1: sFun, a2: iFun, a3: sFun, a4: bFun): dFun = dFunDummy
  def helperStringIntBooleanInt2Double(a1: sFun, a2: iFun, a3: bFun, a4: iFun): dFun = dFunDummy
  def helperStringIntBooleanLong2Double(a1: sFun, a2: iFun, a3: bFun, a4: lFun): dFun = dFunDummy
  def helperStringIntBooleanDouble2Double(a1: sFun, a2: iFun, a3: bFun, a4: dFun): dFun = dFunDummy
  def helperStringIntBooleanString2Double(a1: sFun, a2: iFun, a3: bFun, a4: sFun): dFun = dFunDummy
  def helperStringIntBooleanBoolean2Double(a1: sFun, a2: iFun, a3: bFun, a4: bFun): dFun = dFunDummy
  def helperStringLongIntInt2Double(a1: sFun, a2: lFun, a3: iFun, a4: iFun): dFun = dFunDummy
  def helperStringLongIntLong2Double(a1: sFun, a2: lFun, a3: iFun, a4: lFun): dFun = dFunDummy
  def helperStringLongIntDouble2Double(a1: sFun, a2: lFun, a3: iFun, a4: dFun): dFun = dFunDummy
  def helperStringLongIntString2Double(a1: sFun, a2: lFun, a3: iFun, a4: sFun): dFun = dFunDummy
  def helperStringLongIntBoolean2Double(a1: sFun, a2: lFun, a3: iFun, a4: bFun): dFun = dFunDummy
  def helperStringLongLongInt2Double(a1: sFun, a2: lFun, a3: lFun, a4: iFun): dFun = dFunDummy
  def helperStringLongLongLong2Double(a1: sFun, a2: lFun, a3: lFun, a4: lFun): dFun = dFunDummy
  def helperStringLongLongDouble2Double(a1: sFun, a2: lFun, a3: lFun, a4: dFun): dFun = dFunDummy
  def helperStringLongLongString2Double(a1: sFun, a2: lFun, a3: lFun, a4: sFun): dFun = dFunDummy
  def helperStringLongLongBoolean2Double(a1: sFun, a2: lFun, a3: lFun, a4: bFun): dFun = dFunDummy
  def helperStringLongDoubleInt2Double(a1: sFun, a2: lFun, a3: dFun, a4: iFun): dFun = dFunDummy
  def helperStringLongDoubleLong2Double(a1: sFun, a2: lFun, a3: dFun, a4: lFun): dFun = dFunDummy
  def helperStringLongDoubleDouble2Double(a1: sFun, a2: lFun, a3: dFun, a4: dFun): dFun = dFunDummy
  def helperStringLongDoubleString2Double(a1: sFun, a2: lFun, a3: dFun, a4: sFun): dFun = dFunDummy
  def helperStringLongDoubleBoolean2Double(a1: sFun, a2: lFun, a3: dFun, a4: bFun): dFun = dFunDummy
  def helperStringLongStringInt2Double(a1: sFun, a2: lFun, a3: sFun, a4: iFun): dFun = dFunDummy
  def helperStringLongStringLong2Double(a1: sFun, a2: lFun, a3: sFun, a4: lFun): dFun = dFunDummy
  def helperStringLongStringDouble2Double(a1: sFun, a2: lFun, a3: sFun, a4: dFun): dFun = dFunDummy
  def helperStringLongStringString2Double(a1: sFun, a2: lFun, a3: sFun, a4: sFun): dFun = dFunDummy
  def helperStringLongStringBoolean2Double(a1: sFun, a2: lFun, a3: sFun, a4: bFun): dFun = dFunDummy
  def helperStringLongBooleanInt2Double(a1: sFun, a2: lFun, a3: bFun, a4: iFun): dFun = dFunDummy
  def helperStringLongBooleanLong2Double(a1: sFun, a2: lFun, a3: bFun, a4: lFun): dFun = dFunDummy
  def helperStringLongBooleanDouble2Double(a1: sFun, a2: lFun, a3: bFun, a4: dFun): dFun = dFunDummy
  def helperStringLongBooleanString2Double(a1: sFun, a2: lFun, a3: bFun, a4: sFun): dFun = dFunDummy
  def helperStringLongBooleanBoolean2Double(a1: sFun, a2: lFun, a3: bFun, a4: bFun): dFun = dFunDummy
  def helperStringDoubleIntInt2Double(a1: sFun, a2: dFun, a3: iFun, a4: iFun): dFun = dFunDummy
  def helperStringDoubleIntLong2Double(a1: sFun, a2: dFun, a3: iFun, a4: lFun): dFun = dFunDummy
  def helperStringDoubleIntDouble2Double(a1: sFun, a2: dFun, a3: iFun, a4: dFun): dFun = dFunDummy
  def helperStringDoubleIntString2Double(a1: sFun, a2: dFun, a3: iFun, a4: sFun): dFun = dFunDummy
  def helperStringDoubleIntBoolean2Double(a1: sFun, a2: dFun, a3: iFun, a4: bFun): dFun = dFunDummy
  def helperStringDoubleLongInt2Double(a1: sFun, a2: dFun, a3: lFun, a4: iFun): dFun = dFunDummy
  def helperStringDoubleLongLong2Double(a1: sFun, a2: dFun, a3: lFun, a4: lFun): dFun = dFunDummy
  def helperStringDoubleLongDouble2Double(a1: sFun, a2: dFun, a3: lFun, a4: dFun): dFun = dFunDummy
  def helperStringDoubleLongString2Double(a1: sFun, a2: dFun, a3: lFun, a4: sFun): dFun = dFunDummy
  def helperStringDoubleLongBoolean2Double(a1: sFun, a2: dFun, a3: lFun, a4: bFun): dFun = dFunDummy
  def helperStringDoubleDoubleInt2Double(a1: sFun, a2: dFun, a3: dFun, a4: iFun): dFun = dFunDummy
  def helperStringDoubleDoubleLong2Double(a1: sFun, a2: dFun, a3: dFun, a4: lFun): dFun = dFunDummy
  def helperStringDoubleDoubleDouble2Double(a1: sFun, a2: dFun, a3: dFun, a4: dFun): dFun = dFunDummy
  def helperStringDoubleDoubleString2Double(a1: sFun, a2: dFun, a3: dFun, a4: sFun): dFun = dFunDummy
  def helperStringDoubleDoubleBoolean2Double(a1: sFun, a2: dFun, a3: dFun, a4: bFun): dFun = dFunDummy
  def helperStringDoubleStringInt2Double(a1: sFun, a2: dFun, a3: sFun, a4: iFun): dFun = dFunDummy
  def helperStringDoubleStringLong2Double(a1: sFun, a2: dFun, a3: sFun, a4: lFun): dFun = dFunDummy
  def helperStringDoubleStringDouble2Double(a1: sFun, a2: dFun, a3: sFun, a4: dFun): dFun = dFunDummy
  def helperStringDoubleStringString2Double(a1: sFun, a2: dFun, a3: sFun, a4: sFun): dFun = dFunDummy
  def helperStringDoubleStringBoolean2Double(a1: sFun, a2: dFun, a3: sFun, a4: bFun): dFun = dFunDummy
  def helperStringDoubleBooleanInt2Double(a1: sFun, a2: dFun, a3: bFun, a4: iFun): dFun = dFunDummy
  def helperStringDoubleBooleanLong2Double(a1: sFun, a2: dFun, a3: bFun, a4: lFun): dFun = dFunDummy
  def helperStringDoubleBooleanDouble2Double(a1: sFun, a2: dFun, a3: bFun, a4: dFun): dFun = dFunDummy
  def helperStringDoubleBooleanString2Double(a1: sFun, a2: dFun, a3: bFun, a4: sFun): dFun = dFunDummy
  def helperStringDoubleBooleanBoolean2Double(a1: sFun, a2: dFun, a3: bFun, a4: bFun): dFun = dFunDummy
  def helperStringStringIntInt2Double(a1: sFun, a2: sFun, a3: iFun, a4: iFun): dFun = dFunDummy
  def helperStringStringIntLong2Double(a1: sFun, a2: sFun, a3: iFun, a4: lFun): dFun = dFunDummy
  def helperStringStringIntDouble2Double(a1: sFun, a2: sFun, a3: iFun, a4: dFun): dFun = dFunDummy
  def helperStringStringIntString2Double(a1: sFun, a2: sFun, a3: iFun, a4: sFun): dFun = dFunDummy
  def helperStringStringIntBoolean2Double(a1: sFun, a2: sFun, a3: iFun, a4: bFun): dFun = dFunDummy
  def helperStringStringLongInt2Double(a1: sFun, a2: sFun, a3: lFun, a4: iFun): dFun = dFunDummy
  def helperStringStringLongLong2Double(a1: sFun, a2: sFun, a3: lFun, a4: lFun): dFun = dFunDummy
  def helperStringStringLongDouble2Double(a1: sFun, a2: sFun, a3: lFun, a4: dFun): dFun = dFunDummy
  def helperStringStringLongString2Double(a1: sFun, a2: sFun, a3: lFun, a4: sFun): dFun = dFunDummy
  def helperStringStringLongBoolean2Double(a1: sFun, a2: sFun, a3: lFun, a4: bFun): dFun = dFunDummy
  def helperStringStringDoubleInt2Double(a1: sFun, a2: sFun, a3: dFun, a4: iFun): dFun = dFunDummy
  def helperStringStringDoubleLong2Double(a1: sFun, a2: sFun, a3: dFun, a4: lFun): dFun = dFunDummy
  def helperStringStringDoubleDouble2Double(a1: sFun, a2: sFun, a3: dFun, a4: dFun): dFun = dFunDummy
  def helperStringStringDoubleString2Double(a1: sFun, a2: sFun, a3: dFun, a4: sFun): dFun = dFunDummy
  def helperStringStringDoubleBoolean2Double(a1: sFun, a2: sFun, a3: dFun, a4: bFun): dFun = dFunDummy
  def helperStringStringStringInt2Double(a1: sFun, a2: sFun, a3: sFun, a4: iFun): dFun = dFunDummy
  def helperStringStringStringLong2Double(a1: sFun, a2: sFun, a3: sFun, a4: lFun): dFun = dFunDummy
  def helperStringStringStringDouble2Double(a1: sFun, a2: sFun, a3: sFun, a4: dFun): dFun = dFunDummy
  def helperStringStringStringString2Double(a1: sFun, a2: sFun, a3: sFun, a4: sFun): dFun = dFunDummy
  def helperStringStringStringBoolean2Double(a1: sFun, a2: sFun, a3: sFun, a4: bFun): dFun = dFunDummy
  def helperStringStringBooleanInt2Double(a1: sFun, a2: sFun, a3: bFun, a4: iFun): dFun = dFunDummy
  def helperStringStringBooleanLong2Double(a1: sFun, a2: sFun, a3: bFun, a4: lFun): dFun = dFunDummy
  def helperStringStringBooleanDouble2Double(a1: sFun, a2: sFun, a3: bFun, a4: dFun): dFun = dFunDummy
  def helperStringStringBooleanString2Double(a1: sFun, a2: sFun, a3: bFun, a4: sFun): dFun = dFunDummy
  def helperStringStringBooleanBoolean2Double(a1: sFun, a2: sFun, a3: bFun, a4: bFun): dFun = dFunDummy
  def helperStringBooleanIntInt2Double(a1: sFun, a2: bFun, a3: iFun, a4: iFun): dFun = dFunDummy
  def helperStringBooleanIntLong2Double(a1: sFun, a2: bFun, a3: iFun, a4: lFun): dFun = dFunDummy
  def helperStringBooleanIntDouble2Double(a1: sFun, a2: bFun, a3: iFun, a4: dFun): dFun = dFunDummy
  def helperStringBooleanIntString2Double(a1: sFun, a2: bFun, a3: iFun, a4: sFun): dFun = dFunDummy
  def helperStringBooleanIntBoolean2Double(a1: sFun, a2: bFun, a3: iFun, a4: bFun): dFun = dFunDummy
  def helperStringBooleanLongInt2Double(a1: sFun, a2: bFun, a3: lFun, a4: iFun): dFun = dFunDummy
  def helperStringBooleanLongLong2Double(a1: sFun, a2: bFun, a3: lFun, a4: lFun): dFun = dFunDummy
  def helperStringBooleanLongDouble2Double(a1: sFun, a2: bFun, a3: lFun, a4: dFun): dFun = dFunDummy
  def helperStringBooleanLongString2Double(a1: sFun, a2: bFun, a3: lFun, a4: sFun): dFun = dFunDummy
  def helperStringBooleanLongBoolean2Double(a1: sFun, a2: bFun, a3: lFun, a4: bFun): dFun = dFunDummy
  def helperStringBooleanDoubleInt2Double(a1: sFun, a2: bFun, a3: dFun, a4: iFun): dFun = dFunDummy
  def helperStringBooleanDoubleLong2Double(a1: sFun, a2: bFun, a3: dFun, a4: lFun): dFun = dFunDummy
  def helperStringBooleanDoubleDouble2Double(a1: sFun, a2: bFun, a3: dFun, a4: dFun): dFun = dFunDummy
  def helperStringBooleanDoubleString2Double(a1: sFun, a2: bFun, a3: dFun, a4: sFun): dFun = dFunDummy
  def helperStringBooleanDoubleBoolean2Double(a1: sFun, a2: bFun, a3: dFun, a4: bFun): dFun = dFunDummy
  def helperStringBooleanStringInt2Double(a1: sFun, a2: bFun, a3: sFun, a4: iFun): dFun = dFunDummy
  def helperStringBooleanStringLong2Double(a1: sFun, a2: bFun, a3: sFun, a4: lFun): dFun = dFunDummy
  def helperStringBooleanStringDouble2Double(a1: sFun, a2: bFun, a3: sFun, a4: dFun): dFun = dFunDummy
  def helperStringBooleanStringString2Double(a1: sFun, a2: bFun, a3: sFun, a4: sFun): dFun = dFunDummy
  def helperStringBooleanStringBoolean2Double(a1: sFun, a2: bFun, a3: sFun, a4: bFun): dFun = dFunDummy
  def helperStringBooleanBooleanInt2Double(a1: sFun, a2: bFun, a3: bFun, a4: iFun): dFun = dFunDummy
  def helperStringBooleanBooleanLong2Double(a1: sFun, a2: bFun, a3: bFun, a4: lFun): dFun = dFunDummy
  def helperStringBooleanBooleanDouble2Double(a1: sFun, a2: bFun, a3: bFun, a4: dFun): dFun = dFunDummy
  def helperStringBooleanBooleanString2Double(a1: sFun, a2: bFun, a3: bFun, a4: sFun): dFun = dFunDummy
  def helperStringBooleanBooleanBoolean2Double(a1: sFun, a2: bFun, a3: bFun, a4: bFun): dFun = dFunDummy
  def helperBooleanIntIntInt2Double(a1: bFun, a2: iFun, a3: iFun, a4: iFun): dFun = dFunDummy
  def helperBooleanIntIntLong2Double(a1: bFun, a2: iFun, a3: iFun, a4: lFun): dFun = dFunDummy
  def helperBooleanIntIntDouble2Double(a1: bFun, a2: iFun, a3: iFun, a4: dFun): dFun = dFunDummy
  def helperBooleanIntIntString2Double(a1: bFun, a2: iFun, a3: iFun, a4: sFun): dFun = dFunDummy
  def helperBooleanIntIntBoolean2Double(a1: bFun, a2: iFun, a3: iFun, a4: bFun): dFun = dFunDummy
  def helperBooleanIntLongInt2Double(a1: bFun, a2: iFun, a3: lFun, a4: iFun): dFun = dFunDummy
  def helperBooleanIntLongLong2Double(a1: bFun, a2: iFun, a3: lFun, a4: lFun): dFun = dFunDummy
  def helperBooleanIntLongDouble2Double(a1: bFun, a2: iFun, a3: lFun, a4: dFun): dFun = dFunDummy
  def helperBooleanIntLongString2Double(a1: bFun, a2: iFun, a3: lFun, a4: sFun): dFun = dFunDummy
  def helperBooleanIntLongBoolean2Double(a1: bFun, a2: iFun, a3: lFun, a4: bFun): dFun = dFunDummy
  def helperBooleanIntDoubleInt2Double(a1: bFun, a2: iFun, a3: dFun, a4: iFun): dFun = dFunDummy
  def helperBooleanIntDoubleLong2Double(a1: bFun, a2: iFun, a3: dFun, a4: lFun): dFun = dFunDummy
  def helperBooleanIntDoubleDouble2Double(a1: bFun, a2: iFun, a3: dFun, a4: dFun): dFun = dFunDummy
  def helperBooleanIntDoubleString2Double(a1: bFun, a2: iFun, a3: dFun, a4: sFun): dFun = dFunDummy
  def helperBooleanIntDoubleBoolean2Double(a1: bFun, a2: iFun, a3: dFun, a4: bFun): dFun = dFunDummy
  def helperBooleanIntStringInt2Double(a1: bFun, a2: iFun, a3: sFun, a4: iFun): dFun = dFunDummy
  def helperBooleanIntStringLong2Double(a1: bFun, a2: iFun, a3: sFun, a4: lFun): dFun = dFunDummy
  def helperBooleanIntStringDouble2Double(a1: bFun, a2: iFun, a3: sFun, a4: dFun): dFun = dFunDummy
  def helperBooleanIntStringString2Double(a1: bFun, a2: iFun, a3: sFun, a4: sFun): dFun = dFunDummy
  def helperBooleanIntStringBoolean2Double(a1: bFun, a2: iFun, a3: sFun, a4: bFun): dFun = dFunDummy
  def helperBooleanIntBooleanInt2Double(a1: bFun, a2: iFun, a3: bFun, a4: iFun): dFun = dFunDummy
  def helperBooleanIntBooleanLong2Double(a1: bFun, a2: iFun, a3: bFun, a4: lFun): dFun = dFunDummy
  def helperBooleanIntBooleanDouble2Double(a1: bFun, a2: iFun, a3: bFun, a4: dFun): dFun = dFunDummy
  def helperBooleanIntBooleanString2Double(a1: bFun, a2: iFun, a3: bFun, a4: sFun): dFun = dFunDummy
  def helperBooleanIntBooleanBoolean2Double(a1: bFun, a2: iFun, a3: bFun, a4: bFun): dFun = dFunDummy
  def helperBooleanLongIntInt2Double(a1: bFun, a2: lFun, a3: iFun, a4: iFun): dFun = dFunDummy
  def helperBooleanLongIntLong2Double(a1: bFun, a2: lFun, a3: iFun, a4: lFun): dFun = dFunDummy
  def helperBooleanLongIntDouble2Double(a1: bFun, a2: lFun, a3: iFun, a4: dFun): dFun = dFunDummy
  def helperBooleanLongIntString2Double(a1: bFun, a2: lFun, a3: iFun, a4: sFun): dFun = dFunDummy
  def helperBooleanLongIntBoolean2Double(a1: bFun, a2: lFun, a3: iFun, a4: bFun): dFun = dFunDummy
  def helperBooleanLongLongInt2Double(a1: bFun, a2: lFun, a3: lFun, a4: iFun): dFun = dFunDummy
  def helperBooleanLongLongLong2Double(a1: bFun, a2: lFun, a3: lFun, a4: lFun): dFun = dFunDummy
  def helperBooleanLongLongDouble2Double(a1: bFun, a2: lFun, a3: lFun, a4: dFun): dFun = dFunDummy
  def helperBooleanLongLongString2Double(a1: bFun, a2: lFun, a3: lFun, a4: sFun): dFun = dFunDummy
  def helperBooleanLongLongBoolean2Double(a1: bFun, a2: lFun, a3: lFun, a4: bFun): dFun = dFunDummy
  def helperBooleanLongDoubleInt2Double(a1: bFun, a2: lFun, a3: dFun, a4: iFun): dFun = dFunDummy
  def helperBooleanLongDoubleLong2Double(a1: bFun, a2: lFun, a3: dFun, a4: lFun): dFun = dFunDummy
  def helperBooleanLongDoubleDouble2Double(a1: bFun, a2: lFun, a3: dFun, a4: dFun): dFun = dFunDummy
  def helperBooleanLongDoubleString2Double(a1: bFun, a2: lFun, a3: dFun, a4: sFun): dFun = dFunDummy
  def helperBooleanLongDoubleBoolean2Double(a1: bFun, a2: lFun, a3: dFun, a4: bFun): dFun = dFunDummy
  def helperBooleanLongStringInt2Double(a1: bFun, a2: lFun, a3: sFun, a4: iFun): dFun = dFunDummy
  def helperBooleanLongStringLong2Double(a1: bFun, a2: lFun, a3: sFun, a4: lFun): dFun = dFunDummy
  def helperBooleanLongStringDouble2Double(a1: bFun, a2: lFun, a3: sFun, a4: dFun): dFun = dFunDummy
  def helperBooleanLongStringString2Double(a1: bFun, a2: lFun, a3: sFun, a4: sFun): dFun = dFunDummy
  def helperBooleanLongStringBoolean2Double(a1: bFun, a2: lFun, a3: sFun, a4: bFun): dFun = dFunDummy
  def helperBooleanLongBooleanInt2Double(a1: bFun, a2: lFun, a3: bFun, a4: iFun): dFun = dFunDummy
  def helperBooleanLongBooleanLong2Double(a1: bFun, a2: lFun, a3: bFun, a4: lFun): dFun = dFunDummy
  def helperBooleanLongBooleanDouble2Double(a1: bFun, a2: lFun, a3: bFun, a4: dFun): dFun = dFunDummy
  def helperBooleanLongBooleanString2Double(a1: bFun, a2: lFun, a3: bFun, a4: sFun): dFun = dFunDummy
  def helperBooleanLongBooleanBoolean2Double(a1: bFun, a2: lFun, a3: bFun, a4: bFun): dFun = dFunDummy
  def helperBooleanDoubleIntInt2Double(a1: bFun, a2: dFun, a3: iFun, a4: iFun): dFun = dFunDummy
  def helperBooleanDoubleIntLong2Double(a1: bFun, a2: dFun, a3: iFun, a4: lFun): dFun = dFunDummy
  def helperBooleanDoubleIntDouble2Double(a1: bFun, a2: dFun, a3: iFun, a4: dFun): dFun = dFunDummy
  def helperBooleanDoubleIntString2Double(a1: bFun, a2: dFun, a3: iFun, a4: sFun): dFun = dFunDummy
  def helperBooleanDoubleIntBoolean2Double(a1: bFun, a2: dFun, a3: iFun, a4: bFun): dFun = dFunDummy
  def helperBooleanDoubleLongInt2Double(a1: bFun, a2: dFun, a3: lFun, a4: iFun): dFun = dFunDummy
  def helperBooleanDoubleLongLong2Double(a1: bFun, a2: dFun, a3: lFun, a4: lFun): dFun = dFunDummy
  def helperBooleanDoubleLongDouble2Double(a1: bFun, a2: dFun, a3: lFun, a4: dFun): dFun = dFunDummy
  def helperBooleanDoubleLongString2Double(a1: bFun, a2: dFun, a3: lFun, a4: sFun): dFun = dFunDummy
  def helperBooleanDoubleLongBoolean2Double(a1: bFun, a2: dFun, a3: lFun, a4: bFun): dFun = dFunDummy
  def helperBooleanDoubleDoubleInt2Double(a1: bFun, a2: dFun, a3: dFun, a4: iFun): dFun = dFunDummy
  def helperBooleanDoubleDoubleLong2Double(a1: bFun, a2: dFun, a3: dFun, a4: lFun): dFun = dFunDummy
  def helperBooleanDoubleDoubleDouble2Double(a1: bFun, a2: dFun, a3: dFun, a4: dFun): dFun = dFunDummy
  def helperBooleanDoubleDoubleString2Double(a1: bFun, a2: dFun, a3: dFun, a4: sFun): dFun = dFunDummy
  def helperBooleanDoubleDoubleBoolean2Double(a1: bFun, a2: dFun, a3: dFun, a4: bFun): dFun = dFunDummy
  def helperBooleanDoubleStringInt2Double(a1: bFun, a2: dFun, a3: sFun, a4: iFun): dFun = dFunDummy
  def helperBooleanDoubleStringLong2Double(a1: bFun, a2: dFun, a3: sFun, a4: lFun): dFun = dFunDummy
  def helperBooleanDoubleStringDouble2Double(a1: bFun, a2: dFun, a3: sFun, a4: dFun): dFun = dFunDummy
  def helperBooleanDoubleStringString2Double(a1: bFun, a2: dFun, a3: sFun, a4: sFun): dFun = dFunDummy
  def helperBooleanDoubleStringBoolean2Double(a1: bFun, a2: dFun, a3: sFun, a4: bFun): dFun = dFunDummy
  def helperBooleanDoubleBooleanInt2Double(a1: bFun, a2: dFun, a3: bFun, a4: iFun): dFun = dFunDummy
  def helperBooleanDoubleBooleanLong2Double(a1: bFun, a2: dFun, a3: bFun, a4: lFun): dFun = dFunDummy
  def helperBooleanDoubleBooleanDouble2Double(a1: bFun, a2: dFun, a3: bFun, a4: dFun): dFun = dFunDummy
  def helperBooleanDoubleBooleanString2Double(a1: bFun, a2: dFun, a3: bFun, a4: sFun): dFun = dFunDummy
  def helperBooleanDoubleBooleanBoolean2Double(a1: bFun, a2: dFun, a3: bFun, a4: bFun): dFun = dFunDummy
  def helperBooleanStringIntInt2Double(a1: bFun, a2: sFun, a3: iFun, a4: iFun): dFun = dFunDummy
  def helperBooleanStringIntLong2Double(a1: bFun, a2: sFun, a3: iFun, a4: lFun): dFun = dFunDummy
  def helperBooleanStringIntDouble2Double(a1: bFun, a2: sFun, a3: iFun, a4: dFun): dFun = dFunDummy
  def helperBooleanStringIntString2Double(a1: bFun, a2: sFun, a3: iFun, a4: sFun): dFun = dFunDummy
  def helperBooleanStringIntBoolean2Double(a1: bFun, a2: sFun, a3: iFun, a4: bFun): dFun = dFunDummy
  def helperBooleanStringLongInt2Double(a1: bFun, a2: sFun, a3: lFun, a4: iFun): dFun = dFunDummy
  def helperBooleanStringLongLong2Double(a1: bFun, a2: sFun, a3: lFun, a4: lFun): dFun = dFunDummy
  def helperBooleanStringLongDouble2Double(a1: bFun, a2: sFun, a3: lFun, a4: dFun): dFun = dFunDummy
  def helperBooleanStringLongString2Double(a1: bFun, a2: sFun, a3: lFun, a4: sFun): dFun = dFunDummy
  def helperBooleanStringLongBoolean2Double(a1: bFun, a2: sFun, a3: lFun, a4: bFun): dFun = dFunDummy
  def helperBooleanStringDoubleInt2Double(a1: bFun, a2: sFun, a3: dFun, a4: iFun): dFun = dFunDummy
  def helperBooleanStringDoubleLong2Double(a1: bFun, a2: sFun, a3: dFun, a4: lFun): dFun = dFunDummy
  def helperBooleanStringDoubleDouble2Double(a1: bFun, a2: sFun, a3: dFun, a4: dFun): dFun = dFunDummy
  def helperBooleanStringDoubleString2Double(a1: bFun, a2: sFun, a3: dFun, a4: sFun): dFun = dFunDummy
  def helperBooleanStringDoubleBoolean2Double(a1: bFun, a2: sFun, a3: dFun, a4: bFun): dFun = dFunDummy
  def helperBooleanStringStringInt2Double(a1: bFun, a2: sFun, a3: sFun, a4: iFun): dFun = dFunDummy
  def helperBooleanStringStringLong2Double(a1: bFun, a2: sFun, a3: sFun, a4: lFun): dFun = dFunDummy
  def helperBooleanStringStringDouble2Double(a1: bFun, a2: sFun, a3: sFun, a4: dFun): dFun = dFunDummy
  def helperBooleanStringStringString2Double(a1: bFun, a2: sFun, a3: sFun, a4: sFun): dFun = dFunDummy
  def helperBooleanStringStringBoolean2Double(a1: bFun, a2: sFun, a3: sFun, a4: bFun): dFun = dFunDummy
  def helperBooleanStringBooleanInt2Double(a1: bFun, a2: sFun, a3: bFun, a4: iFun): dFun = dFunDummy
  def helperBooleanStringBooleanLong2Double(a1: bFun, a2: sFun, a3: bFun, a4: lFun): dFun = dFunDummy
  def helperBooleanStringBooleanDouble2Double(a1: bFun, a2: sFun, a3: bFun, a4: dFun): dFun = dFunDummy
  def helperBooleanStringBooleanString2Double(a1: bFun, a2: sFun, a3: bFun, a4: sFun): dFun = dFunDummy
  def helperBooleanStringBooleanBoolean2Double(a1: bFun, a2: sFun, a3: bFun, a4: bFun): dFun = dFunDummy
  def helperBooleanBooleanIntInt2Double(a1: bFun, a2: bFun, a3: iFun, a4: iFun): dFun = dFunDummy
  def helperBooleanBooleanIntLong2Double(a1: bFun, a2: bFun, a3: iFun, a4: lFun): dFun = dFunDummy
  def helperBooleanBooleanIntDouble2Double(a1: bFun, a2: bFun, a3: iFun, a4: dFun): dFun = dFunDummy
  def helperBooleanBooleanIntString2Double(a1: bFun, a2: bFun, a3: iFun, a4: sFun): dFun = dFunDummy
  def helperBooleanBooleanIntBoolean2Double(a1: bFun, a2: bFun, a3: iFun, a4: bFun): dFun = dFunDummy
  def helperBooleanBooleanLongInt2Double(a1: bFun, a2: bFun, a3: lFun, a4: iFun): dFun = dFunDummy
  def helperBooleanBooleanLongLong2Double(a1: bFun, a2: bFun, a3: lFun, a4: lFun): dFun = dFunDummy
  def helperBooleanBooleanLongDouble2Double(a1: bFun, a2: bFun, a3: lFun, a4: dFun): dFun = dFunDummy
  def helperBooleanBooleanLongString2Double(a1: bFun, a2: bFun, a3: lFun, a4: sFun): dFun = dFunDummy
  def helperBooleanBooleanLongBoolean2Double(a1: bFun, a2: bFun, a3: lFun, a4: bFun): dFun = dFunDummy
  def helperBooleanBooleanDoubleInt2Double(a1: bFun, a2: bFun, a3: dFun, a4: iFun): dFun = dFunDummy
  def helperBooleanBooleanDoubleLong2Double(a1: bFun, a2: bFun, a3: dFun, a4: lFun): dFun = dFunDummy
  def helperBooleanBooleanDoubleDouble2Double(a1: bFun, a2: bFun, a3: dFun, a4: dFun): dFun = dFunDummy
  def helperBooleanBooleanDoubleString2Double(a1: bFun, a2: bFun, a3: dFun, a4: sFun): dFun = dFunDummy
  def helperBooleanBooleanDoubleBoolean2Double(a1: bFun, a2: bFun, a3: dFun, a4: bFun): dFun = dFunDummy
  def helperBooleanBooleanStringInt2Double(a1: bFun, a2: bFun, a3: sFun, a4: iFun): dFun = dFunDummy
  def helperBooleanBooleanStringLong2Double(a1: bFun, a2: bFun, a3: sFun, a4: lFun): dFun = dFunDummy
  def helperBooleanBooleanStringDouble2Double(a1: bFun, a2: bFun, a3: sFun, a4: dFun): dFun = dFunDummy
  def helperBooleanBooleanStringString2Double(a1: bFun, a2: bFun, a3: sFun, a4: sFun): dFun = dFunDummy
  def helperBooleanBooleanStringBoolean2Double(a1: bFun, a2: bFun, a3: sFun, a4: bFun): dFun = dFunDummy
  def helperBooleanBooleanBooleanInt2Double(a1: bFun, a2: bFun, a3: bFun, a4: iFun): dFun = dFunDummy
  def helperBooleanBooleanBooleanLong2Double(a1: bFun, a2: bFun, a3: bFun, a4: lFun): dFun = dFunDummy
  def helperBooleanBooleanBooleanDouble2Double(a1: bFun, a2: bFun, a3: bFun, a4: dFun): dFun = dFunDummy
  def helperBooleanBooleanBooleanString2Double(a1: bFun, a2: bFun, a3: bFun, a4: sFun): dFun = dFunDummy
  def helperBooleanBooleanBooleanBoolean2Double(a1: bFun, a2: bFun, a3: bFun, a4: bFun): dFun = dFunDummy
  test("getSignature_dFun_5") {
    var result = true
    result &= FunctionSignature.getSignatureIntIntIntInt2Double(helperIntIntIntInt2Double) == "Int:Int:Int:Int2Double"
    result &= FunctionSignature.getSignatureIntIntIntLong2Double(helperIntIntIntLong2Double) == "Int:Int:Int:Long2Double"
    result &= FunctionSignature.getSignatureIntIntIntDouble2Double(helperIntIntIntDouble2Double) == "Int:Int:Int:Double2Double"
    result &= FunctionSignature.getSignatureIntIntIntString2Double(helperIntIntIntString2Double) == "Int:Int:Int:String2Double"
    result &= FunctionSignature.getSignatureIntIntIntBoolean2Double(helperIntIntIntBoolean2Double) == "Int:Int:Int:Boolean2Double"
    result &= FunctionSignature.getSignatureIntIntLongInt2Double(helperIntIntLongInt2Double) == "Int:Int:Long:Int2Double"
    result &= FunctionSignature.getSignatureIntIntLongLong2Double(helperIntIntLongLong2Double) == "Int:Int:Long:Long2Double"
    result &= FunctionSignature.getSignatureIntIntLongDouble2Double(helperIntIntLongDouble2Double) == "Int:Int:Long:Double2Double"
    result &= FunctionSignature.getSignatureIntIntLongString2Double(helperIntIntLongString2Double) == "Int:Int:Long:String2Double"
    result &= FunctionSignature.getSignatureIntIntLongBoolean2Double(helperIntIntLongBoolean2Double) == "Int:Int:Long:Boolean2Double"
    result &= FunctionSignature.getSignatureIntIntDoubleInt2Double(helperIntIntDoubleInt2Double) == "Int:Int:Double:Int2Double"
    result &= FunctionSignature.getSignatureIntIntDoubleLong2Double(helperIntIntDoubleLong2Double) == "Int:Int:Double:Long2Double"
    result &= FunctionSignature.getSignatureIntIntDoubleDouble2Double(helperIntIntDoubleDouble2Double) == "Int:Int:Double:Double2Double"
    result &= FunctionSignature.getSignatureIntIntDoubleString2Double(helperIntIntDoubleString2Double) == "Int:Int:Double:String2Double"
    result &= FunctionSignature.getSignatureIntIntDoubleBoolean2Double(helperIntIntDoubleBoolean2Double) == "Int:Int:Double:Boolean2Double"
    result &= FunctionSignature.getSignatureIntIntStringInt2Double(helperIntIntStringInt2Double) == "Int:Int:String:Int2Double"
    result &= FunctionSignature.getSignatureIntIntStringLong2Double(helperIntIntStringLong2Double) == "Int:Int:String:Long2Double"
    result &= FunctionSignature.getSignatureIntIntStringDouble2Double(helperIntIntStringDouble2Double) == "Int:Int:String:Double2Double"
    result &= FunctionSignature.getSignatureIntIntStringString2Double(helperIntIntStringString2Double) == "Int:Int:String:String2Double"
    result &= FunctionSignature.getSignatureIntIntStringBoolean2Double(helperIntIntStringBoolean2Double) == "Int:Int:String:Boolean2Double"
    result &= FunctionSignature.getSignatureIntIntBooleanInt2Double(helperIntIntBooleanInt2Double) == "Int:Int:Boolean:Int2Double"
    result &= FunctionSignature.getSignatureIntIntBooleanLong2Double(helperIntIntBooleanLong2Double) == "Int:Int:Boolean:Long2Double"
    result &= FunctionSignature.getSignatureIntIntBooleanDouble2Double(helperIntIntBooleanDouble2Double) == "Int:Int:Boolean:Double2Double"
    result &= FunctionSignature.getSignatureIntIntBooleanString2Double(helperIntIntBooleanString2Double) == "Int:Int:Boolean:String2Double"
    result &= FunctionSignature.getSignatureIntIntBooleanBoolean2Double(helperIntIntBooleanBoolean2Double) == "Int:Int:Boolean:Boolean2Double"
    result &= FunctionSignature.getSignatureIntLongIntInt2Double(helperIntLongIntInt2Double) == "Int:Long:Int:Int2Double"
    result &= FunctionSignature.getSignatureIntLongIntLong2Double(helperIntLongIntLong2Double) == "Int:Long:Int:Long2Double"
    result &= FunctionSignature.getSignatureIntLongIntDouble2Double(helperIntLongIntDouble2Double) == "Int:Long:Int:Double2Double"
    result &= FunctionSignature.getSignatureIntLongIntString2Double(helperIntLongIntString2Double) == "Int:Long:Int:String2Double"
    result &= FunctionSignature.getSignatureIntLongIntBoolean2Double(helperIntLongIntBoolean2Double) == "Int:Long:Int:Boolean2Double"
    result &= FunctionSignature.getSignatureIntLongLongInt2Double(helperIntLongLongInt2Double) == "Int:Long:Long:Int2Double"
    result &= FunctionSignature.getSignatureIntLongLongLong2Double(helperIntLongLongLong2Double) == "Int:Long:Long:Long2Double"
    result &= FunctionSignature.getSignatureIntLongLongDouble2Double(helperIntLongLongDouble2Double) == "Int:Long:Long:Double2Double"
    result &= FunctionSignature.getSignatureIntLongLongString2Double(helperIntLongLongString2Double) == "Int:Long:Long:String2Double"
    result &= FunctionSignature.getSignatureIntLongLongBoolean2Double(helperIntLongLongBoolean2Double) == "Int:Long:Long:Boolean2Double"
    result &= FunctionSignature.getSignatureIntLongDoubleInt2Double(helperIntLongDoubleInt2Double) == "Int:Long:Double:Int2Double"
    result &= FunctionSignature.getSignatureIntLongDoubleLong2Double(helperIntLongDoubleLong2Double) == "Int:Long:Double:Long2Double"
    result &= FunctionSignature.getSignatureIntLongDoubleDouble2Double(helperIntLongDoubleDouble2Double) == "Int:Long:Double:Double2Double"
    result &= FunctionSignature.getSignatureIntLongDoubleString2Double(helperIntLongDoubleString2Double) == "Int:Long:Double:String2Double"
    result &= FunctionSignature.getSignatureIntLongDoubleBoolean2Double(helperIntLongDoubleBoolean2Double) == "Int:Long:Double:Boolean2Double"
    result &= FunctionSignature.getSignatureIntLongStringInt2Double(helperIntLongStringInt2Double) == "Int:Long:String:Int2Double"
    result &= FunctionSignature.getSignatureIntLongStringLong2Double(helperIntLongStringLong2Double) == "Int:Long:String:Long2Double"
    result &= FunctionSignature.getSignatureIntLongStringDouble2Double(helperIntLongStringDouble2Double) == "Int:Long:String:Double2Double"
    result &= FunctionSignature.getSignatureIntLongStringString2Double(helperIntLongStringString2Double) == "Int:Long:String:String2Double"
    result &= FunctionSignature.getSignatureIntLongStringBoolean2Double(helperIntLongStringBoolean2Double) == "Int:Long:String:Boolean2Double"
    result &= FunctionSignature.getSignatureIntLongBooleanInt2Double(helperIntLongBooleanInt2Double) == "Int:Long:Boolean:Int2Double"
    result &= FunctionSignature.getSignatureIntLongBooleanLong2Double(helperIntLongBooleanLong2Double) == "Int:Long:Boolean:Long2Double"
    result &= FunctionSignature.getSignatureIntLongBooleanDouble2Double(helperIntLongBooleanDouble2Double) == "Int:Long:Boolean:Double2Double"
    result &= FunctionSignature.getSignatureIntLongBooleanString2Double(helperIntLongBooleanString2Double) == "Int:Long:Boolean:String2Double"
    result &= FunctionSignature.getSignatureIntLongBooleanBoolean2Double(helperIntLongBooleanBoolean2Double) == "Int:Long:Boolean:Boolean2Double"
    result &= FunctionSignature.getSignatureIntDoubleIntInt2Double(helperIntDoubleIntInt2Double) == "Int:Double:Int:Int2Double"
    result &= FunctionSignature.getSignatureIntDoubleIntLong2Double(helperIntDoubleIntLong2Double) == "Int:Double:Int:Long2Double"
    result &= FunctionSignature.getSignatureIntDoubleIntDouble2Double(helperIntDoubleIntDouble2Double) == "Int:Double:Int:Double2Double"
    result &= FunctionSignature.getSignatureIntDoubleIntString2Double(helperIntDoubleIntString2Double) == "Int:Double:Int:String2Double"
    result &= FunctionSignature.getSignatureIntDoubleIntBoolean2Double(helperIntDoubleIntBoolean2Double) == "Int:Double:Int:Boolean2Double"
    result &= FunctionSignature.getSignatureIntDoubleLongInt2Double(helperIntDoubleLongInt2Double) == "Int:Double:Long:Int2Double"
    result &= FunctionSignature.getSignatureIntDoubleLongLong2Double(helperIntDoubleLongLong2Double) == "Int:Double:Long:Long2Double"
    result &= FunctionSignature.getSignatureIntDoubleLongDouble2Double(helperIntDoubleLongDouble2Double) == "Int:Double:Long:Double2Double"
    result &= FunctionSignature.getSignatureIntDoubleLongString2Double(helperIntDoubleLongString2Double) == "Int:Double:Long:String2Double"
    result &= FunctionSignature.getSignatureIntDoubleLongBoolean2Double(helperIntDoubleLongBoolean2Double) == "Int:Double:Long:Boolean2Double"
    result &= FunctionSignature.getSignatureIntDoubleDoubleInt2Double(helperIntDoubleDoubleInt2Double) == "Int:Double:Double:Int2Double"
    result &= FunctionSignature.getSignatureIntDoubleDoubleLong2Double(helperIntDoubleDoubleLong2Double) == "Int:Double:Double:Long2Double"
    result &= FunctionSignature.getSignatureIntDoubleDoubleDouble2Double(helperIntDoubleDoubleDouble2Double) == "Int:Double:Double:Double2Double"
    result &= FunctionSignature.getSignatureIntDoubleDoubleString2Double(helperIntDoubleDoubleString2Double) == "Int:Double:Double:String2Double"
    result &= FunctionSignature.getSignatureIntDoubleDoubleBoolean2Double(helperIntDoubleDoubleBoolean2Double) == "Int:Double:Double:Boolean2Double"
    result &= FunctionSignature.getSignatureIntDoubleStringInt2Double(helperIntDoubleStringInt2Double) == "Int:Double:String:Int2Double"
    result &= FunctionSignature.getSignatureIntDoubleStringLong2Double(helperIntDoubleStringLong2Double) == "Int:Double:String:Long2Double"
    result &= FunctionSignature.getSignatureIntDoubleStringDouble2Double(helperIntDoubleStringDouble2Double) == "Int:Double:String:Double2Double"
    result &= FunctionSignature.getSignatureIntDoubleStringString2Double(helperIntDoubleStringString2Double) == "Int:Double:String:String2Double"
    result &= FunctionSignature.getSignatureIntDoubleStringBoolean2Double(helperIntDoubleStringBoolean2Double) == "Int:Double:String:Boolean2Double"
    result &= FunctionSignature.getSignatureIntDoubleBooleanInt2Double(helperIntDoubleBooleanInt2Double) == "Int:Double:Boolean:Int2Double"
    result &= FunctionSignature.getSignatureIntDoubleBooleanLong2Double(helperIntDoubleBooleanLong2Double) == "Int:Double:Boolean:Long2Double"
    result &= FunctionSignature.getSignatureIntDoubleBooleanDouble2Double(helperIntDoubleBooleanDouble2Double) == "Int:Double:Boolean:Double2Double"
    result &= FunctionSignature.getSignatureIntDoubleBooleanString2Double(helperIntDoubleBooleanString2Double) == "Int:Double:Boolean:String2Double"
    result &= FunctionSignature.getSignatureIntDoubleBooleanBoolean2Double(helperIntDoubleBooleanBoolean2Double) == "Int:Double:Boolean:Boolean2Double"
    result &= FunctionSignature.getSignatureIntStringIntInt2Double(helperIntStringIntInt2Double) == "Int:String:Int:Int2Double"
    result &= FunctionSignature.getSignatureIntStringIntLong2Double(helperIntStringIntLong2Double) == "Int:String:Int:Long2Double"
    result &= FunctionSignature.getSignatureIntStringIntDouble2Double(helperIntStringIntDouble2Double) == "Int:String:Int:Double2Double"
    result &= FunctionSignature.getSignatureIntStringIntString2Double(helperIntStringIntString2Double) == "Int:String:Int:String2Double"
    result &= FunctionSignature.getSignatureIntStringIntBoolean2Double(helperIntStringIntBoolean2Double) == "Int:String:Int:Boolean2Double"
    result &= FunctionSignature.getSignatureIntStringLongInt2Double(helperIntStringLongInt2Double) == "Int:String:Long:Int2Double"
    result &= FunctionSignature.getSignatureIntStringLongLong2Double(helperIntStringLongLong2Double) == "Int:String:Long:Long2Double"
    result &= FunctionSignature.getSignatureIntStringLongDouble2Double(helperIntStringLongDouble2Double) == "Int:String:Long:Double2Double"
    result &= FunctionSignature.getSignatureIntStringLongString2Double(helperIntStringLongString2Double) == "Int:String:Long:String2Double"
    result &= FunctionSignature.getSignatureIntStringLongBoolean2Double(helperIntStringLongBoolean2Double) == "Int:String:Long:Boolean2Double"
    result &= FunctionSignature.getSignatureIntStringDoubleInt2Double(helperIntStringDoubleInt2Double) == "Int:String:Double:Int2Double"
    result &= FunctionSignature.getSignatureIntStringDoubleLong2Double(helperIntStringDoubleLong2Double) == "Int:String:Double:Long2Double"
    result &= FunctionSignature.getSignatureIntStringDoubleDouble2Double(helperIntStringDoubleDouble2Double) == "Int:String:Double:Double2Double"
    result &= FunctionSignature.getSignatureIntStringDoubleString2Double(helperIntStringDoubleString2Double) == "Int:String:Double:String2Double"
    result &= FunctionSignature.getSignatureIntStringDoubleBoolean2Double(helperIntStringDoubleBoolean2Double) == "Int:String:Double:Boolean2Double"
    result &= FunctionSignature.getSignatureIntStringStringInt2Double(helperIntStringStringInt2Double) == "Int:String:String:Int2Double"
    result &= FunctionSignature.getSignatureIntStringStringLong2Double(helperIntStringStringLong2Double) == "Int:String:String:Long2Double"
    result &= FunctionSignature.getSignatureIntStringStringDouble2Double(helperIntStringStringDouble2Double) == "Int:String:String:Double2Double"
    result &= FunctionSignature.getSignatureIntStringStringString2Double(helperIntStringStringString2Double) == "Int:String:String:String2Double"
    result &= FunctionSignature.getSignatureIntStringStringBoolean2Double(helperIntStringStringBoolean2Double) == "Int:String:String:Boolean2Double"
    result &= FunctionSignature.getSignatureIntStringBooleanInt2Double(helperIntStringBooleanInt2Double) == "Int:String:Boolean:Int2Double"
    result &= FunctionSignature.getSignatureIntStringBooleanLong2Double(helperIntStringBooleanLong2Double) == "Int:String:Boolean:Long2Double"
    result &= FunctionSignature.getSignatureIntStringBooleanDouble2Double(helperIntStringBooleanDouble2Double) == "Int:String:Boolean:Double2Double"
    result &= FunctionSignature.getSignatureIntStringBooleanString2Double(helperIntStringBooleanString2Double) == "Int:String:Boolean:String2Double"
    result &= FunctionSignature.getSignatureIntStringBooleanBoolean2Double(helperIntStringBooleanBoolean2Double) == "Int:String:Boolean:Boolean2Double"
    result &= FunctionSignature.getSignatureIntBooleanIntInt2Double(helperIntBooleanIntInt2Double) == "Int:Boolean:Int:Int2Double"
    result &= FunctionSignature.getSignatureIntBooleanIntLong2Double(helperIntBooleanIntLong2Double) == "Int:Boolean:Int:Long2Double"
    result &= FunctionSignature.getSignatureIntBooleanIntDouble2Double(helperIntBooleanIntDouble2Double) == "Int:Boolean:Int:Double2Double"
    result &= FunctionSignature.getSignatureIntBooleanIntString2Double(helperIntBooleanIntString2Double) == "Int:Boolean:Int:String2Double"
    result &= FunctionSignature.getSignatureIntBooleanIntBoolean2Double(helperIntBooleanIntBoolean2Double) == "Int:Boolean:Int:Boolean2Double"
    result &= FunctionSignature.getSignatureIntBooleanLongInt2Double(helperIntBooleanLongInt2Double) == "Int:Boolean:Long:Int2Double"
    result &= FunctionSignature.getSignatureIntBooleanLongLong2Double(helperIntBooleanLongLong2Double) == "Int:Boolean:Long:Long2Double"
    result &= FunctionSignature.getSignatureIntBooleanLongDouble2Double(helperIntBooleanLongDouble2Double) == "Int:Boolean:Long:Double2Double"
    result &= FunctionSignature.getSignatureIntBooleanLongString2Double(helperIntBooleanLongString2Double) == "Int:Boolean:Long:String2Double"
    result &= FunctionSignature.getSignatureIntBooleanLongBoolean2Double(helperIntBooleanLongBoolean2Double) == "Int:Boolean:Long:Boolean2Double"
    result &= FunctionSignature.getSignatureIntBooleanDoubleInt2Double(helperIntBooleanDoubleInt2Double) == "Int:Boolean:Double:Int2Double"
    result &= FunctionSignature.getSignatureIntBooleanDoubleLong2Double(helperIntBooleanDoubleLong2Double) == "Int:Boolean:Double:Long2Double"
    result &= FunctionSignature.getSignatureIntBooleanDoubleDouble2Double(helperIntBooleanDoubleDouble2Double) == "Int:Boolean:Double:Double2Double"
    result &= FunctionSignature.getSignatureIntBooleanDoubleString2Double(helperIntBooleanDoubleString2Double) == "Int:Boolean:Double:String2Double"
    result &= FunctionSignature.getSignatureIntBooleanDoubleBoolean2Double(helperIntBooleanDoubleBoolean2Double) == "Int:Boolean:Double:Boolean2Double"
    result &= FunctionSignature.getSignatureIntBooleanStringInt2Double(helperIntBooleanStringInt2Double) == "Int:Boolean:String:Int2Double"
    result &= FunctionSignature.getSignatureIntBooleanStringLong2Double(helperIntBooleanStringLong2Double) == "Int:Boolean:String:Long2Double"
    result &= FunctionSignature.getSignatureIntBooleanStringDouble2Double(helperIntBooleanStringDouble2Double) == "Int:Boolean:String:Double2Double"
    result &= FunctionSignature.getSignatureIntBooleanStringString2Double(helperIntBooleanStringString2Double) == "Int:Boolean:String:String2Double"
    result &= FunctionSignature.getSignatureIntBooleanStringBoolean2Double(helperIntBooleanStringBoolean2Double) == "Int:Boolean:String:Boolean2Double"
    result &= FunctionSignature.getSignatureIntBooleanBooleanInt2Double(helperIntBooleanBooleanInt2Double) == "Int:Boolean:Boolean:Int2Double"
    result &= FunctionSignature.getSignatureIntBooleanBooleanLong2Double(helperIntBooleanBooleanLong2Double) == "Int:Boolean:Boolean:Long2Double"
    result &= FunctionSignature.getSignatureIntBooleanBooleanDouble2Double(helperIntBooleanBooleanDouble2Double) == "Int:Boolean:Boolean:Double2Double"
    result &= FunctionSignature.getSignatureIntBooleanBooleanString2Double(helperIntBooleanBooleanString2Double) == "Int:Boolean:Boolean:String2Double"
    result &= FunctionSignature.getSignatureIntBooleanBooleanBoolean2Double(helperIntBooleanBooleanBoolean2Double) == "Int:Boolean:Boolean:Boolean2Double"
    result &= FunctionSignature.getSignatureLongIntIntInt2Double(helperLongIntIntInt2Double) == "Long:Int:Int:Int2Double"
    result &= FunctionSignature.getSignatureLongIntIntLong2Double(helperLongIntIntLong2Double) == "Long:Int:Int:Long2Double"
    result &= FunctionSignature.getSignatureLongIntIntDouble2Double(helperLongIntIntDouble2Double) == "Long:Int:Int:Double2Double"
    result &= FunctionSignature.getSignatureLongIntIntString2Double(helperLongIntIntString2Double) == "Long:Int:Int:String2Double"
    result &= FunctionSignature.getSignatureLongIntIntBoolean2Double(helperLongIntIntBoolean2Double) == "Long:Int:Int:Boolean2Double"
    result &= FunctionSignature.getSignatureLongIntLongInt2Double(helperLongIntLongInt2Double) == "Long:Int:Long:Int2Double"
    result &= FunctionSignature.getSignatureLongIntLongLong2Double(helperLongIntLongLong2Double) == "Long:Int:Long:Long2Double"
    result &= FunctionSignature.getSignatureLongIntLongDouble2Double(helperLongIntLongDouble2Double) == "Long:Int:Long:Double2Double"
    result &= FunctionSignature.getSignatureLongIntLongString2Double(helperLongIntLongString2Double) == "Long:Int:Long:String2Double"
    result &= FunctionSignature.getSignatureLongIntLongBoolean2Double(helperLongIntLongBoolean2Double) == "Long:Int:Long:Boolean2Double"
    result &= FunctionSignature.getSignatureLongIntDoubleInt2Double(helperLongIntDoubleInt2Double) == "Long:Int:Double:Int2Double"
    result &= FunctionSignature.getSignatureLongIntDoubleLong2Double(helperLongIntDoubleLong2Double) == "Long:Int:Double:Long2Double"
    result &= FunctionSignature.getSignatureLongIntDoubleDouble2Double(helperLongIntDoubleDouble2Double) == "Long:Int:Double:Double2Double"
    result &= FunctionSignature.getSignatureLongIntDoubleString2Double(helperLongIntDoubleString2Double) == "Long:Int:Double:String2Double"
    result &= FunctionSignature.getSignatureLongIntDoubleBoolean2Double(helperLongIntDoubleBoolean2Double) == "Long:Int:Double:Boolean2Double"
    result &= FunctionSignature.getSignatureLongIntStringInt2Double(helperLongIntStringInt2Double) == "Long:Int:String:Int2Double"
    result &= FunctionSignature.getSignatureLongIntStringLong2Double(helperLongIntStringLong2Double) == "Long:Int:String:Long2Double"
    result &= FunctionSignature.getSignatureLongIntStringDouble2Double(helperLongIntStringDouble2Double) == "Long:Int:String:Double2Double"
    result &= FunctionSignature.getSignatureLongIntStringString2Double(helperLongIntStringString2Double) == "Long:Int:String:String2Double"
    result &= FunctionSignature.getSignatureLongIntStringBoolean2Double(helperLongIntStringBoolean2Double) == "Long:Int:String:Boolean2Double"
    result &= FunctionSignature.getSignatureLongIntBooleanInt2Double(helperLongIntBooleanInt2Double) == "Long:Int:Boolean:Int2Double"
    result &= FunctionSignature.getSignatureLongIntBooleanLong2Double(helperLongIntBooleanLong2Double) == "Long:Int:Boolean:Long2Double"
    result &= FunctionSignature.getSignatureLongIntBooleanDouble2Double(helperLongIntBooleanDouble2Double) == "Long:Int:Boolean:Double2Double"
    result &= FunctionSignature.getSignatureLongIntBooleanString2Double(helperLongIntBooleanString2Double) == "Long:Int:Boolean:String2Double"
    result &= FunctionSignature.getSignatureLongIntBooleanBoolean2Double(helperLongIntBooleanBoolean2Double) == "Long:Int:Boolean:Boolean2Double"
    result &= FunctionSignature.getSignatureLongLongIntInt2Double(helperLongLongIntInt2Double) == "Long:Long:Int:Int2Double"
    result &= FunctionSignature.getSignatureLongLongIntLong2Double(helperLongLongIntLong2Double) == "Long:Long:Int:Long2Double"
    result &= FunctionSignature.getSignatureLongLongIntDouble2Double(helperLongLongIntDouble2Double) == "Long:Long:Int:Double2Double"
    result &= FunctionSignature.getSignatureLongLongIntString2Double(helperLongLongIntString2Double) == "Long:Long:Int:String2Double"
    result &= FunctionSignature.getSignatureLongLongIntBoolean2Double(helperLongLongIntBoolean2Double) == "Long:Long:Int:Boolean2Double"
    result &= FunctionSignature.getSignatureLongLongLongInt2Double(helperLongLongLongInt2Double) == "Long:Long:Long:Int2Double"
    result &= FunctionSignature.getSignatureLongLongLongLong2Double(helperLongLongLongLong2Double) == "Long:Long:Long:Long2Double"
    result &= FunctionSignature.getSignatureLongLongLongDouble2Double(helperLongLongLongDouble2Double) == "Long:Long:Long:Double2Double"
    result &= FunctionSignature.getSignatureLongLongLongString2Double(helperLongLongLongString2Double) == "Long:Long:Long:String2Double"
    result &= FunctionSignature.getSignatureLongLongLongBoolean2Double(helperLongLongLongBoolean2Double) == "Long:Long:Long:Boolean2Double"
    result &= FunctionSignature.getSignatureLongLongDoubleInt2Double(helperLongLongDoubleInt2Double) == "Long:Long:Double:Int2Double"
    result &= FunctionSignature.getSignatureLongLongDoubleLong2Double(helperLongLongDoubleLong2Double) == "Long:Long:Double:Long2Double"
    result &= FunctionSignature.getSignatureLongLongDoubleDouble2Double(helperLongLongDoubleDouble2Double) == "Long:Long:Double:Double2Double"
    result &= FunctionSignature.getSignatureLongLongDoubleString2Double(helperLongLongDoubleString2Double) == "Long:Long:Double:String2Double"
    result &= FunctionSignature.getSignatureLongLongDoubleBoolean2Double(helperLongLongDoubleBoolean2Double) == "Long:Long:Double:Boolean2Double"
    result &= FunctionSignature.getSignatureLongLongStringInt2Double(helperLongLongStringInt2Double) == "Long:Long:String:Int2Double"
    result &= FunctionSignature.getSignatureLongLongStringLong2Double(helperLongLongStringLong2Double) == "Long:Long:String:Long2Double"
    result &= FunctionSignature.getSignatureLongLongStringDouble2Double(helperLongLongStringDouble2Double) == "Long:Long:String:Double2Double"
    result &= FunctionSignature.getSignatureLongLongStringString2Double(helperLongLongStringString2Double) == "Long:Long:String:String2Double"
    result &= FunctionSignature.getSignatureLongLongStringBoolean2Double(helperLongLongStringBoolean2Double) == "Long:Long:String:Boolean2Double"
    result &= FunctionSignature.getSignatureLongLongBooleanInt2Double(helperLongLongBooleanInt2Double) == "Long:Long:Boolean:Int2Double"
    result &= FunctionSignature.getSignatureLongLongBooleanLong2Double(helperLongLongBooleanLong2Double) == "Long:Long:Boolean:Long2Double"
    result &= FunctionSignature.getSignatureLongLongBooleanDouble2Double(helperLongLongBooleanDouble2Double) == "Long:Long:Boolean:Double2Double"
    result &= FunctionSignature.getSignatureLongLongBooleanString2Double(helperLongLongBooleanString2Double) == "Long:Long:Boolean:String2Double"
    result &= FunctionSignature.getSignatureLongLongBooleanBoolean2Double(helperLongLongBooleanBoolean2Double) == "Long:Long:Boolean:Boolean2Double"
    result &= FunctionSignature.getSignatureLongDoubleIntInt2Double(helperLongDoubleIntInt2Double) == "Long:Double:Int:Int2Double"
    result &= FunctionSignature.getSignatureLongDoubleIntLong2Double(helperLongDoubleIntLong2Double) == "Long:Double:Int:Long2Double"
    result &= FunctionSignature.getSignatureLongDoubleIntDouble2Double(helperLongDoubleIntDouble2Double) == "Long:Double:Int:Double2Double"
    result &= FunctionSignature.getSignatureLongDoubleIntString2Double(helperLongDoubleIntString2Double) == "Long:Double:Int:String2Double"
    result &= FunctionSignature.getSignatureLongDoubleIntBoolean2Double(helperLongDoubleIntBoolean2Double) == "Long:Double:Int:Boolean2Double"
    result &= FunctionSignature.getSignatureLongDoubleLongInt2Double(helperLongDoubleLongInt2Double) == "Long:Double:Long:Int2Double"
    result &= FunctionSignature.getSignatureLongDoubleLongLong2Double(helperLongDoubleLongLong2Double) == "Long:Double:Long:Long2Double"
    result &= FunctionSignature.getSignatureLongDoubleLongDouble2Double(helperLongDoubleLongDouble2Double) == "Long:Double:Long:Double2Double"
    result &= FunctionSignature.getSignatureLongDoubleLongString2Double(helperLongDoubleLongString2Double) == "Long:Double:Long:String2Double"
    result &= FunctionSignature.getSignatureLongDoubleLongBoolean2Double(helperLongDoubleLongBoolean2Double) == "Long:Double:Long:Boolean2Double"
    result &= FunctionSignature.getSignatureLongDoubleDoubleInt2Double(helperLongDoubleDoubleInt2Double) == "Long:Double:Double:Int2Double"
    result &= FunctionSignature.getSignatureLongDoubleDoubleLong2Double(helperLongDoubleDoubleLong2Double) == "Long:Double:Double:Long2Double"
    result &= FunctionSignature.getSignatureLongDoubleDoubleDouble2Double(helperLongDoubleDoubleDouble2Double) == "Long:Double:Double:Double2Double"
    result &= FunctionSignature.getSignatureLongDoubleDoubleString2Double(helperLongDoubleDoubleString2Double) == "Long:Double:Double:String2Double"
    result &= FunctionSignature.getSignatureLongDoubleDoubleBoolean2Double(helperLongDoubleDoubleBoolean2Double) == "Long:Double:Double:Boolean2Double"
    result &= FunctionSignature.getSignatureLongDoubleStringInt2Double(helperLongDoubleStringInt2Double) == "Long:Double:String:Int2Double"
    result &= FunctionSignature.getSignatureLongDoubleStringLong2Double(helperLongDoubleStringLong2Double) == "Long:Double:String:Long2Double"
    result &= FunctionSignature.getSignatureLongDoubleStringDouble2Double(helperLongDoubleStringDouble2Double) == "Long:Double:String:Double2Double"
    result &= FunctionSignature.getSignatureLongDoubleStringString2Double(helperLongDoubleStringString2Double) == "Long:Double:String:String2Double"
    result &= FunctionSignature.getSignatureLongDoubleStringBoolean2Double(helperLongDoubleStringBoolean2Double) == "Long:Double:String:Boolean2Double"
    result &= FunctionSignature.getSignatureLongDoubleBooleanInt2Double(helperLongDoubleBooleanInt2Double) == "Long:Double:Boolean:Int2Double"
    result &= FunctionSignature.getSignatureLongDoubleBooleanLong2Double(helperLongDoubleBooleanLong2Double) == "Long:Double:Boolean:Long2Double"
    result &= FunctionSignature.getSignatureLongDoubleBooleanDouble2Double(helperLongDoubleBooleanDouble2Double) == "Long:Double:Boolean:Double2Double"
    result &= FunctionSignature.getSignatureLongDoubleBooleanString2Double(helperLongDoubleBooleanString2Double) == "Long:Double:Boolean:String2Double"
    result &= FunctionSignature.getSignatureLongDoubleBooleanBoolean2Double(helperLongDoubleBooleanBoolean2Double) == "Long:Double:Boolean:Boolean2Double"
    result &= FunctionSignature.getSignatureLongStringIntInt2Double(helperLongStringIntInt2Double) == "Long:String:Int:Int2Double"
    result &= FunctionSignature.getSignatureLongStringIntLong2Double(helperLongStringIntLong2Double) == "Long:String:Int:Long2Double"
    result &= FunctionSignature.getSignatureLongStringIntDouble2Double(helperLongStringIntDouble2Double) == "Long:String:Int:Double2Double"
    result &= FunctionSignature.getSignatureLongStringIntString2Double(helperLongStringIntString2Double) == "Long:String:Int:String2Double"
    result &= FunctionSignature.getSignatureLongStringIntBoolean2Double(helperLongStringIntBoolean2Double) == "Long:String:Int:Boolean2Double"
    result &= FunctionSignature.getSignatureLongStringLongInt2Double(helperLongStringLongInt2Double) == "Long:String:Long:Int2Double"
    result &= FunctionSignature.getSignatureLongStringLongLong2Double(helperLongStringLongLong2Double) == "Long:String:Long:Long2Double"
    result &= FunctionSignature.getSignatureLongStringLongDouble2Double(helperLongStringLongDouble2Double) == "Long:String:Long:Double2Double"
    result &= FunctionSignature.getSignatureLongStringLongString2Double(helperLongStringLongString2Double) == "Long:String:Long:String2Double"
    result &= FunctionSignature.getSignatureLongStringLongBoolean2Double(helperLongStringLongBoolean2Double) == "Long:String:Long:Boolean2Double"
    result &= FunctionSignature.getSignatureLongStringDoubleInt2Double(helperLongStringDoubleInt2Double) == "Long:String:Double:Int2Double"
    result &= FunctionSignature.getSignatureLongStringDoubleLong2Double(helperLongStringDoubleLong2Double) == "Long:String:Double:Long2Double"
    result &= FunctionSignature.getSignatureLongStringDoubleDouble2Double(helperLongStringDoubleDouble2Double) == "Long:String:Double:Double2Double"
    result &= FunctionSignature.getSignatureLongStringDoubleString2Double(helperLongStringDoubleString2Double) == "Long:String:Double:String2Double"
    result &= FunctionSignature.getSignatureLongStringDoubleBoolean2Double(helperLongStringDoubleBoolean2Double) == "Long:String:Double:Boolean2Double"
    result &= FunctionSignature.getSignatureLongStringStringInt2Double(helperLongStringStringInt2Double) == "Long:String:String:Int2Double"
    result &= FunctionSignature.getSignatureLongStringStringLong2Double(helperLongStringStringLong2Double) == "Long:String:String:Long2Double"
    result &= FunctionSignature.getSignatureLongStringStringDouble2Double(helperLongStringStringDouble2Double) == "Long:String:String:Double2Double"
    result &= FunctionSignature.getSignatureLongStringStringString2Double(helperLongStringStringString2Double) == "Long:String:String:String2Double"
    result &= FunctionSignature.getSignatureLongStringStringBoolean2Double(helperLongStringStringBoolean2Double) == "Long:String:String:Boolean2Double"
    result &= FunctionSignature.getSignatureLongStringBooleanInt2Double(helperLongStringBooleanInt2Double) == "Long:String:Boolean:Int2Double"
    result &= FunctionSignature.getSignatureLongStringBooleanLong2Double(helperLongStringBooleanLong2Double) == "Long:String:Boolean:Long2Double"
    result &= FunctionSignature.getSignatureLongStringBooleanDouble2Double(helperLongStringBooleanDouble2Double) == "Long:String:Boolean:Double2Double"
    result &= FunctionSignature.getSignatureLongStringBooleanString2Double(helperLongStringBooleanString2Double) == "Long:String:Boolean:String2Double"
    result &= FunctionSignature.getSignatureLongStringBooleanBoolean2Double(helperLongStringBooleanBoolean2Double) == "Long:String:Boolean:Boolean2Double"
    result &= FunctionSignature.getSignatureLongBooleanIntInt2Double(helperLongBooleanIntInt2Double) == "Long:Boolean:Int:Int2Double"
    result &= FunctionSignature.getSignatureLongBooleanIntLong2Double(helperLongBooleanIntLong2Double) == "Long:Boolean:Int:Long2Double"
    result &= FunctionSignature.getSignatureLongBooleanIntDouble2Double(helperLongBooleanIntDouble2Double) == "Long:Boolean:Int:Double2Double"
    result &= FunctionSignature.getSignatureLongBooleanIntString2Double(helperLongBooleanIntString2Double) == "Long:Boolean:Int:String2Double"
    result &= FunctionSignature.getSignatureLongBooleanIntBoolean2Double(helperLongBooleanIntBoolean2Double) == "Long:Boolean:Int:Boolean2Double"
    result &= FunctionSignature.getSignatureLongBooleanLongInt2Double(helperLongBooleanLongInt2Double) == "Long:Boolean:Long:Int2Double"
    result &= FunctionSignature.getSignatureLongBooleanLongLong2Double(helperLongBooleanLongLong2Double) == "Long:Boolean:Long:Long2Double"
    result &= FunctionSignature.getSignatureLongBooleanLongDouble2Double(helperLongBooleanLongDouble2Double) == "Long:Boolean:Long:Double2Double"
    result &= FunctionSignature.getSignatureLongBooleanLongString2Double(helperLongBooleanLongString2Double) == "Long:Boolean:Long:String2Double"
    result &= FunctionSignature.getSignatureLongBooleanLongBoolean2Double(helperLongBooleanLongBoolean2Double) == "Long:Boolean:Long:Boolean2Double"
    result &= FunctionSignature.getSignatureLongBooleanDoubleInt2Double(helperLongBooleanDoubleInt2Double) == "Long:Boolean:Double:Int2Double"
    result &= FunctionSignature.getSignatureLongBooleanDoubleLong2Double(helperLongBooleanDoubleLong2Double) == "Long:Boolean:Double:Long2Double"
    result &= FunctionSignature.getSignatureLongBooleanDoubleDouble2Double(helperLongBooleanDoubleDouble2Double) == "Long:Boolean:Double:Double2Double"
    result &= FunctionSignature.getSignatureLongBooleanDoubleString2Double(helperLongBooleanDoubleString2Double) == "Long:Boolean:Double:String2Double"
    result &= FunctionSignature.getSignatureLongBooleanDoubleBoolean2Double(helperLongBooleanDoubleBoolean2Double) == "Long:Boolean:Double:Boolean2Double"
    result &= FunctionSignature.getSignatureLongBooleanStringInt2Double(helperLongBooleanStringInt2Double) == "Long:Boolean:String:Int2Double"
    result &= FunctionSignature.getSignatureLongBooleanStringLong2Double(helperLongBooleanStringLong2Double) == "Long:Boolean:String:Long2Double"
    result &= FunctionSignature.getSignatureLongBooleanStringDouble2Double(helperLongBooleanStringDouble2Double) == "Long:Boolean:String:Double2Double"
    result &= FunctionSignature.getSignatureLongBooleanStringString2Double(helperLongBooleanStringString2Double) == "Long:Boolean:String:String2Double"
    result &= FunctionSignature.getSignatureLongBooleanStringBoolean2Double(helperLongBooleanStringBoolean2Double) == "Long:Boolean:String:Boolean2Double"
    result &= FunctionSignature.getSignatureLongBooleanBooleanInt2Double(helperLongBooleanBooleanInt2Double) == "Long:Boolean:Boolean:Int2Double"
    result &= FunctionSignature.getSignatureLongBooleanBooleanLong2Double(helperLongBooleanBooleanLong2Double) == "Long:Boolean:Boolean:Long2Double"
    result &= FunctionSignature.getSignatureLongBooleanBooleanDouble2Double(helperLongBooleanBooleanDouble2Double) == "Long:Boolean:Boolean:Double2Double"
    result &= FunctionSignature.getSignatureLongBooleanBooleanString2Double(helperLongBooleanBooleanString2Double) == "Long:Boolean:Boolean:String2Double"
    result &= FunctionSignature.getSignatureLongBooleanBooleanBoolean2Double(helperLongBooleanBooleanBoolean2Double) == "Long:Boolean:Boolean:Boolean2Double"
    result &= FunctionSignature.getSignatureDoubleIntIntInt2Double(helperDoubleIntIntInt2Double) == "Double:Int:Int:Int2Double"
    result &= FunctionSignature.getSignatureDoubleIntIntLong2Double(helperDoubleIntIntLong2Double) == "Double:Int:Int:Long2Double"
    result &= FunctionSignature.getSignatureDoubleIntIntDouble2Double(helperDoubleIntIntDouble2Double) == "Double:Int:Int:Double2Double"
    result &= FunctionSignature.getSignatureDoubleIntIntString2Double(helperDoubleIntIntString2Double) == "Double:Int:Int:String2Double"
    result &= FunctionSignature.getSignatureDoubleIntIntBoolean2Double(helperDoubleIntIntBoolean2Double) == "Double:Int:Int:Boolean2Double"
    result &= FunctionSignature.getSignatureDoubleIntLongInt2Double(helperDoubleIntLongInt2Double) == "Double:Int:Long:Int2Double"
    result &= FunctionSignature.getSignatureDoubleIntLongLong2Double(helperDoubleIntLongLong2Double) == "Double:Int:Long:Long2Double"
    result &= FunctionSignature.getSignatureDoubleIntLongDouble2Double(helperDoubleIntLongDouble2Double) == "Double:Int:Long:Double2Double"
    result &= FunctionSignature.getSignatureDoubleIntLongString2Double(helperDoubleIntLongString2Double) == "Double:Int:Long:String2Double"
    result &= FunctionSignature.getSignatureDoubleIntLongBoolean2Double(helperDoubleIntLongBoolean2Double) == "Double:Int:Long:Boolean2Double"
    result &= FunctionSignature.getSignatureDoubleIntDoubleInt2Double(helperDoubleIntDoubleInt2Double) == "Double:Int:Double:Int2Double"
    result &= FunctionSignature.getSignatureDoubleIntDoubleLong2Double(helperDoubleIntDoubleLong2Double) == "Double:Int:Double:Long2Double"
    result &= FunctionSignature.getSignatureDoubleIntDoubleDouble2Double(helperDoubleIntDoubleDouble2Double) == "Double:Int:Double:Double2Double"
    result &= FunctionSignature.getSignatureDoubleIntDoubleString2Double(helperDoubleIntDoubleString2Double) == "Double:Int:Double:String2Double"
    result &= FunctionSignature.getSignatureDoubleIntDoubleBoolean2Double(helperDoubleIntDoubleBoolean2Double) == "Double:Int:Double:Boolean2Double"
    result &= FunctionSignature.getSignatureDoubleIntStringInt2Double(helperDoubleIntStringInt2Double) == "Double:Int:String:Int2Double"
    result &= FunctionSignature.getSignatureDoubleIntStringLong2Double(helperDoubleIntStringLong2Double) == "Double:Int:String:Long2Double"
    result &= FunctionSignature.getSignatureDoubleIntStringDouble2Double(helperDoubleIntStringDouble2Double) == "Double:Int:String:Double2Double"
    result &= FunctionSignature.getSignatureDoubleIntStringString2Double(helperDoubleIntStringString2Double) == "Double:Int:String:String2Double"
    result &= FunctionSignature.getSignatureDoubleIntStringBoolean2Double(helperDoubleIntStringBoolean2Double) == "Double:Int:String:Boolean2Double"
    result &= FunctionSignature.getSignatureDoubleIntBooleanInt2Double(helperDoubleIntBooleanInt2Double) == "Double:Int:Boolean:Int2Double"
    result &= FunctionSignature.getSignatureDoubleIntBooleanLong2Double(helperDoubleIntBooleanLong2Double) == "Double:Int:Boolean:Long2Double"
    result &= FunctionSignature.getSignatureDoubleIntBooleanDouble2Double(helperDoubleIntBooleanDouble2Double) == "Double:Int:Boolean:Double2Double"
    result &= FunctionSignature.getSignatureDoubleIntBooleanString2Double(helperDoubleIntBooleanString2Double) == "Double:Int:Boolean:String2Double"
    result &= FunctionSignature.getSignatureDoubleIntBooleanBoolean2Double(helperDoubleIntBooleanBoolean2Double) == "Double:Int:Boolean:Boolean2Double"
    result &= FunctionSignature.getSignatureDoubleLongIntInt2Double(helperDoubleLongIntInt2Double) == "Double:Long:Int:Int2Double"
    result &= FunctionSignature.getSignatureDoubleLongIntLong2Double(helperDoubleLongIntLong2Double) == "Double:Long:Int:Long2Double"
    result &= FunctionSignature.getSignatureDoubleLongIntDouble2Double(helperDoubleLongIntDouble2Double) == "Double:Long:Int:Double2Double"
    result &= FunctionSignature.getSignatureDoubleLongIntString2Double(helperDoubleLongIntString2Double) == "Double:Long:Int:String2Double"
    result &= FunctionSignature.getSignatureDoubleLongIntBoolean2Double(helperDoubleLongIntBoolean2Double) == "Double:Long:Int:Boolean2Double"
    result &= FunctionSignature.getSignatureDoubleLongLongInt2Double(helperDoubleLongLongInt2Double) == "Double:Long:Long:Int2Double"
    result &= FunctionSignature.getSignatureDoubleLongLongLong2Double(helperDoubleLongLongLong2Double) == "Double:Long:Long:Long2Double"
    result &= FunctionSignature.getSignatureDoubleLongLongDouble2Double(helperDoubleLongLongDouble2Double) == "Double:Long:Long:Double2Double"
    result &= FunctionSignature.getSignatureDoubleLongLongString2Double(helperDoubleLongLongString2Double) == "Double:Long:Long:String2Double"
    result &= FunctionSignature.getSignatureDoubleLongLongBoolean2Double(helperDoubleLongLongBoolean2Double) == "Double:Long:Long:Boolean2Double"
    result &= FunctionSignature.getSignatureDoubleLongDoubleInt2Double(helperDoubleLongDoubleInt2Double) == "Double:Long:Double:Int2Double"
    result &= FunctionSignature.getSignatureDoubleLongDoubleLong2Double(helperDoubleLongDoubleLong2Double) == "Double:Long:Double:Long2Double"
    result &= FunctionSignature.getSignatureDoubleLongDoubleDouble2Double(helperDoubleLongDoubleDouble2Double) == "Double:Long:Double:Double2Double"
    result &= FunctionSignature.getSignatureDoubleLongDoubleString2Double(helperDoubleLongDoubleString2Double) == "Double:Long:Double:String2Double"
    result &= FunctionSignature.getSignatureDoubleLongDoubleBoolean2Double(helperDoubleLongDoubleBoolean2Double) == "Double:Long:Double:Boolean2Double"
    result &= FunctionSignature.getSignatureDoubleLongStringInt2Double(helperDoubleLongStringInt2Double) == "Double:Long:String:Int2Double"
    result &= FunctionSignature.getSignatureDoubleLongStringLong2Double(helperDoubleLongStringLong2Double) == "Double:Long:String:Long2Double"
    result &= FunctionSignature.getSignatureDoubleLongStringDouble2Double(helperDoubleLongStringDouble2Double) == "Double:Long:String:Double2Double"
    result &= FunctionSignature.getSignatureDoubleLongStringString2Double(helperDoubleLongStringString2Double) == "Double:Long:String:String2Double"
    result &= FunctionSignature.getSignatureDoubleLongStringBoolean2Double(helperDoubleLongStringBoolean2Double) == "Double:Long:String:Boolean2Double"
    result &= FunctionSignature.getSignatureDoubleLongBooleanInt2Double(helperDoubleLongBooleanInt2Double) == "Double:Long:Boolean:Int2Double"
    result &= FunctionSignature.getSignatureDoubleLongBooleanLong2Double(helperDoubleLongBooleanLong2Double) == "Double:Long:Boolean:Long2Double"
    result &= FunctionSignature.getSignatureDoubleLongBooleanDouble2Double(helperDoubleLongBooleanDouble2Double) == "Double:Long:Boolean:Double2Double"
    result &= FunctionSignature.getSignatureDoubleLongBooleanString2Double(helperDoubleLongBooleanString2Double) == "Double:Long:Boolean:String2Double"
    result &= FunctionSignature.getSignatureDoubleLongBooleanBoolean2Double(helperDoubleLongBooleanBoolean2Double) == "Double:Long:Boolean:Boolean2Double"
    result &= FunctionSignature.getSignatureDoubleDoubleIntInt2Double(helperDoubleDoubleIntInt2Double) == "Double:Double:Int:Int2Double"
    result &= FunctionSignature.getSignatureDoubleDoubleIntLong2Double(helperDoubleDoubleIntLong2Double) == "Double:Double:Int:Long2Double"
    result &= FunctionSignature.getSignatureDoubleDoubleIntDouble2Double(helperDoubleDoubleIntDouble2Double) == "Double:Double:Int:Double2Double"
    result &= FunctionSignature.getSignatureDoubleDoubleIntString2Double(helperDoubleDoubleIntString2Double) == "Double:Double:Int:String2Double"
    result &= FunctionSignature.getSignatureDoubleDoubleIntBoolean2Double(helperDoubleDoubleIntBoolean2Double) == "Double:Double:Int:Boolean2Double"
    result &= FunctionSignature.getSignatureDoubleDoubleLongInt2Double(helperDoubleDoubleLongInt2Double) == "Double:Double:Long:Int2Double"
    result &= FunctionSignature.getSignatureDoubleDoubleLongLong2Double(helperDoubleDoubleLongLong2Double) == "Double:Double:Long:Long2Double"
    result &= FunctionSignature.getSignatureDoubleDoubleLongDouble2Double(helperDoubleDoubleLongDouble2Double) == "Double:Double:Long:Double2Double"
    result &= FunctionSignature.getSignatureDoubleDoubleLongString2Double(helperDoubleDoubleLongString2Double) == "Double:Double:Long:String2Double"
    result &= FunctionSignature.getSignatureDoubleDoubleLongBoolean2Double(helperDoubleDoubleLongBoolean2Double) == "Double:Double:Long:Boolean2Double"
    result &= FunctionSignature.getSignatureDoubleDoubleDoubleInt2Double(helperDoubleDoubleDoubleInt2Double) == "Double:Double:Double:Int2Double"
    result &= FunctionSignature.getSignatureDoubleDoubleDoubleLong2Double(helperDoubleDoubleDoubleLong2Double) == "Double:Double:Double:Long2Double"
    result &= FunctionSignature.getSignatureDoubleDoubleDoubleDouble2Double(helperDoubleDoubleDoubleDouble2Double) == "Double:Double:Double:Double2Double"
    result &= FunctionSignature.getSignatureDoubleDoubleDoubleString2Double(helperDoubleDoubleDoubleString2Double) == "Double:Double:Double:String2Double"
    result &= FunctionSignature.getSignatureDoubleDoubleDoubleBoolean2Double(helperDoubleDoubleDoubleBoolean2Double) == "Double:Double:Double:Boolean2Double"
    result &= FunctionSignature.getSignatureDoubleDoubleStringInt2Double(helperDoubleDoubleStringInt2Double) == "Double:Double:String:Int2Double"
    result &= FunctionSignature.getSignatureDoubleDoubleStringLong2Double(helperDoubleDoubleStringLong2Double) == "Double:Double:String:Long2Double"
    result &= FunctionSignature.getSignatureDoubleDoubleStringDouble2Double(helperDoubleDoubleStringDouble2Double) == "Double:Double:String:Double2Double"
    result &= FunctionSignature.getSignatureDoubleDoubleStringString2Double(helperDoubleDoubleStringString2Double) == "Double:Double:String:String2Double"
    result &= FunctionSignature.getSignatureDoubleDoubleStringBoolean2Double(helperDoubleDoubleStringBoolean2Double) == "Double:Double:String:Boolean2Double"
    result &= FunctionSignature.getSignatureDoubleDoubleBooleanInt2Double(helperDoubleDoubleBooleanInt2Double) == "Double:Double:Boolean:Int2Double"
    result &= FunctionSignature.getSignatureDoubleDoubleBooleanLong2Double(helperDoubleDoubleBooleanLong2Double) == "Double:Double:Boolean:Long2Double"
    result &= FunctionSignature.getSignatureDoubleDoubleBooleanDouble2Double(helperDoubleDoubleBooleanDouble2Double) == "Double:Double:Boolean:Double2Double"
    result &= FunctionSignature.getSignatureDoubleDoubleBooleanString2Double(helperDoubleDoubleBooleanString2Double) == "Double:Double:Boolean:String2Double"
    result &= FunctionSignature.getSignatureDoubleDoubleBooleanBoolean2Double(helperDoubleDoubleBooleanBoolean2Double) == "Double:Double:Boolean:Boolean2Double"
    result &= FunctionSignature.getSignatureDoubleStringIntInt2Double(helperDoubleStringIntInt2Double) == "Double:String:Int:Int2Double"
    result &= FunctionSignature.getSignatureDoubleStringIntLong2Double(helperDoubleStringIntLong2Double) == "Double:String:Int:Long2Double"
    result &= FunctionSignature.getSignatureDoubleStringIntDouble2Double(helperDoubleStringIntDouble2Double) == "Double:String:Int:Double2Double"
    result &= FunctionSignature.getSignatureDoubleStringIntString2Double(helperDoubleStringIntString2Double) == "Double:String:Int:String2Double"
    result &= FunctionSignature.getSignatureDoubleStringIntBoolean2Double(helperDoubleStringIntBoolean2Double) == "Double:String:Int:Boolean2Double"
    result &= FunctionSignature.getSignatureDoubleStringLongInt2Double(helperDoubleStringLongInt2Double) == "Double:String:Long:Int2Double"
    result &= FunctionSignature.getSignatureDoubleStringLongLong2Double(helperDoubleStringLongLong2Double) == "Double:String:Long:Long2Double"
    result &= FunctionSignature.getSignatureDoubleStringLongDouble2Double(helperDoubleStringLongDouble2Double) == "Double:String:Long:Double2Double"
    result &= FunctionSignature.getSignatureDoubleStringLongString2Double(helperDoubleStringLongString2Double) == "Double:String:Long:String2Double"
    result &= FunctionSignature.getSignatureDoubleStringLongBoolean2Double(helperDoubleStringLongBoolean2Double) == "Double:String:Long:Boolean2Double"
    result &= FunctionSignature.getSignatureDoubleStringDoubleInt2Double(helperDoubleStringDoubleInt2Double) == "Double:String:Double:Int2Double"
    result &= FunctionSignature.getSignatureDoubleStringDoubleLong2Double(helperDoubleStringDoubleLong2Double) == "Double:String:Double:Long2Double"
    result &= FunctionSignature.getSignatureDoubleStringDoubleDouble2Double(helperDoubleStringDoubleDouble2Double) == "Double:String:Double:Double2Double"
    result &= FunctionSignature.getSignatureDoubleStringDoubleString2Double(helperDoubleStringDoubleString2Double) == "Double:String:Double:String2Double"
    result &= FunctionSignature.getSignatureDoubleStringDoubleBoolean2Double(helperDoubleStringDoubleBoolean2Double) == "Double:String:Double:Boolean2Double"
    result &= FunctionSignature.getSignatureDoubleStringStringInt2Double(helperDoubleStringStringInt2Double) == "Double:String:String:Int2Double"
    result &= FunctionSignature.getSignatureDoubleStringStringLong2Double(helperDoubleStringStringLong2Double) == "Double:String:String:Long2Double"
    result &= FunctionSignature.getSignatureDoubleStringStringDouble2Double(helperDoubleStringStringDouble2Double) == "Double:String:String:Double2Double"
    result &= FunctionSignature.getSignatureDoubleStringStringString2Double(helperDoubleStringStringString2Double) == "Double:String:String:String2Double"
    result &= FunctionSignature.getSignatureDoubleStringStringBoolean2Double(helperDoubleStringStringBoolean2Double) == "Double:String:String:Boolean2Double"
    result &= FunctionSignature.getSignatureDoubleStringBooleanInt2Double(helperDoubleStringBooleanInt2Double) == "Double:String:Boolean:Int2Double"
    result &= FunctionSignature.getSignatureDoubleStringBooleanLong2Double(helperDoubleStringBooleanLong2Double) == "Double:String:Boolean:Long2Double"
    result &= FunctionSignature.getSignatureDoubleStringBooleanDouble2Double(helperDoubleStringBooleanDouble2Double) == "Double:String:Boolean:Double2Double"
    result &= FunctionSignature.getSignatureDoubleStringBooleanString2Double(helperDoubleStringBooleanString2Double) == "Double:String:Boolean:String2Double"
    result &= FunctionSignature.getSignatureDoubleStringBooleanBoolean2Double(helperDoubleStringBooleanBoolean2Double) == "Double:String:Boolean:Boolean2Double"
    result &= FunctionSignature.getSignatureDoubleBooleanIntInt2Double(helperDoubleBooleanIntInt2Double) == "Double:Boolean:Int:Int2Double"
    result &= FunctionSignature.getSignatureDoubleBooleanIntLong2Double(helperDoubleBooleanIntLong2Double) == "Double:Boolean:Int:Long2Double"
    result &= FunctionSignature.getSignatureDoubleBooleanIntDouble2Double(helperDoubleBooleanIntDouble2Double) == "Double:Boolean:Int:Double2Double"
    result &= FunctionSignature.getSignatureDoubleBooleanIntString2Double(helperDoubleBooleanIntString2Double) == "Double:Boolean:Int:String2Double"
    result &= FunctionSignature.getSignatureDoubleBooleanIntBoolean2Double(helperDoubleBooleanIntBoolean2Double) == "Double:Boolean:Int:Boolean2Double"
    result &= FunctionSignature.getSignatureDoubleBooleanLongInt2Double(helperDoubleBooleanLongInt2Double) == "Double:Boolean:Long:Int2Double"
    result &= FunctionSignature.getSignatureDoubleBooleanLongLong2Double(helperDoubleBooleanLongLong2Double) == "Double:Boolean:Long:Long2Double"
    result &= FunctionSignature.getSignatureDoubleBooleanLongDouble2Double(helperDoubleBooleanLongDouble2Double) == "Double:Boolean:Long:Double2Double"
    result &= FunctionSignature.getSignatureDoubleBooleanLongString2Double(helperDoubleBooleanLongString2Double) == "Double:Boolean:Long:String2Double"
    result &= FunctionSignature.getSignatureDoubleBooleanLongBoolean2Double(helperDoubleBooleanLongBoolean2Double) == "Double:Boolean:Long:Boolean2Double"
    result &= FunctionSignature.getSignatureDoubleBooleanDoubleInt2Double(helperDoubleBooleanDoubleInt2Double) == "Double:Boolean:Double:Int2Double"
    result &= FunctionSignature.getSignatureDoubleBooleanDoubleLong2Double(helperDoubleBooleanDoubleLong2Double) == "Double:Boolean:Double:Long2Double"
    result &= FunctionSignature.getSignatureDoubleBooleanDoubleDouble2Double(helperDoubleBooleanDoubleDouble2Double) == "Double:Boolean:Double:Double2Double"
    result &= FunctionSignature.getSignatureDoubleBooleanDoubleString2Double(helperDoubleBooleanDoubleString2Double) == "Double:Boolean:Double:String2Double"
    result &= FunctionSignature.getSignatureDoubleBooleanDoubleBoolean2Double(helperDoubleBooleanDoubleBoolean2Double) == "Double:Boolean:Double:Boolean2Double"
    result &= FunctionSignature.getSignatureDoubleBooleanStringInt2Double(helperDoubleBooleanStringInt2Double) == "Double:Boolean:String:Int2Double"
    result &= FunctionSignature.getSignatureDoubleBooleanStringLong2Double(helperDoubleBooleanStringLong2Double) == "Double:Boolean:String:Long2Double"
    result &= FunctionSignature.getSignatureDoubleBooleanStringDouble2Double(helperDoubleBooleanStringDouble2Double) == "Double:Boolean:String:Double2Double"
    result &= FunctionSignature.getSignatureDoubleBooleanStringString2Double(helperDoubleBooleanStringString2Double) == "Double:Boolean:String:String2Double"
    result &= FunctionSignature.getSignatureDoubleBooleanStringBoolean2Double(helperDoubleBooleanStringBoolean2Double) == "Double:Boolean:String:Boolean2Double"
    result &= FunctionSignature.getSignatureDoubleBooleanBooleanInt2Double(helperDoubleBooleanBooleanInt2Double) == "Double:Boolean:Boolean:Int2Double"
    result &= FunctionSignature.getSignatureDoubleBooleanBooleanLong2Double(helperDoubleBooleanBooleanLong2Double) == "Double:Boolean:Boolean:Long2Double"
    result &= FunctionSignature.getSignatureDoubleBooleanBooleanDouble2Double(helperDoubleBooleanBooleanDouble2Double) == "Double:Boolean:Boolean:Double2Double"
    result &= FunctionSignature.getSignatureDoubleBooleanBooleanString2Double(helperDoubleBooleanBooleanString2Double) == "Double:Boolean:Boolean:String2Double"
    result &= FunctionSignature.getSignatureDoubleBooleanBooleanBoolean2Double(helperDoubleBooleanBooleanBoolean2Double) == "Double:Boolean:Boolean:Boolean2Double"
    result &= FunctionSignature.getSignatureStringIntIntInt2Double(helperStringIntIntInt2Double) == "String:Int:Int:Int2Double"
    result &= FunctionSignature.getSignatureStringIntIntLong2Double(helperStringIntIntLong2Double) == "String:Int:Int:Long2Double"
    result &= FunctionSignature.getSignatureStringIntIntDouble2Double(helperStringIntIntDouble2Double) == "String:Int:Int:Double2Double"
    result &= FunctionSignature.getSignatureStringIntIntString2Double(helperStringIntIntString2Double) == "String:Int:Int:String2Double"
    result &= FunctionSignature.getSignatureStringIntIntBoolean2Double(helperStringIntIntBoolean2Double) == "String:Int:Int:Boolean2Double"
    result &= FunctionSignature.getSignatureStringIntLongInt2Double(helperStringIntLongInt2Double) == "String:Int:Long:Int2Double"
    result &= FunctionSignature.getSignatureStringIntLongLong2Double(helperStringIntLongLong2Double) == "String:Int:Long:Long2Double"
    result &= FunctionSignature.getSignatureStringIntLongDouble2Double(helperStringIntLongDouble2Double) == "String:Int:Long:Double2Double"
    result &= FunctionSignature.getSignatureStringIntLongString2Double(helperStringIntLongString2Double) == "String:Int:Long:String2Double"
    result &= FunctionSignature.getSignatureStringIntLongBoolean2Double(helperStringIntLongBoolean2Double) == "String:Int:Long:Boolean2Double"
    result &= FunctionSignature.getSignatureStringIntDoubleInt2Double(helperStringIntDoubleInt2Double) == "String:Int:Double:Int2Double"
    result &= FunctionSignature.getSignatureStringIntDoubleLong2Double(helperStringIntDoubleLong2Double) == "String:Int:Double:Long2Double"
    result &= FunctionSignature.getSignatureStringIntDoubleDouble2Double(helperStringIntDoubleDouble2Double) == "String:Int:Double:Double2Double"
    result &= FunctionSignature.getSignatureStringIntDoubleString2Double(helperStringIntDoubleString2Double) == "String:Int:Double:String2Double"
    result &= FunctionSignature.getSignatureStringIntDoubleBoolean2Double(helperStringIntDoubleBoolean2Double) == "String:Int:Double:Boolean2Double"
    result &= FunctionSignature.getSignatureStringIntStringInt2Double(helperStringIntStringInt2Double) == "String:Int:String:Int2Double"
    result &= FunctionSignature.getSignatureStringIntStringLong2Double(helperStringIntStringLong2Double) == "String:Int:String:Long2Double"
    result &= FunctionSignature.getSignatureStringIntStringDouble2Double(helperStringIntStringDouble2Double) == "String:Int:String:Double2Double"
    result &= FunctionSignature.getSignatureStringIntStringString2Double(helperStringIntStringString2Double) == "String:Int:String:String2Double"
    result &= FunctionSignature.getSignatureStringIntStringBoolean2Double(helperStringIntStringBoolean2Double) == "String:Int:String:Boolean2Double"
    result &= FunctionSignature.getSignatureStringIntBooleanInt2Double(helperStringIntBooleanInt2Double) == "String:Int:Boolean:Int2Double"
    result &= FunctionSignature.getSignatureStringIntBooleanLong2Double(helperStringIntBooleanLong2Double) == "String:Int:Boolean:Long2Double"
    result &= FunctionSignature.getSignatureStringIntBooleanDouble2Double(helperStringIntBooleanDouble2Double) == "String:Int:Boolean:Double2Double"
    result &= FunctionSignature.getSignatureStringIntBooleanString2Double(helperStringIntBooleanString2Double) == "String:Int:Boolean:String2Double"
    result &= FunctionSignature.getSignatureStringIntBooleanBoolean2Double(helperStringIntBooleanBoolean2Double) == "String:Int:Boolean:Boolean2Double"
    result &= FunctionSignature.getSignatureStringLongIntInt2Double(helperStringLongIntInt2Double) == "String:Long:Int:Int2Double"
    result &= FunctionSignature.getSignatureStringLongIntLong2Double(helperStringLongIntLong2Double) == "String:Long:Int:Long2Double"
    result &= FunctionSignature.getSignatureStringLongIntDouble2Double(helperStringLongIntDouble2Double) == "String:Long:Int:Double2Double"
    result &= FunctionSignature.getSignatureStringLongIntString2Double(helperStringLongIntString2Double) == "String:Long:Int:String2Double"
    result &= FunctionSignature.getSignatureStringLongIntBoolean2Double(helperStringLongIntBoolean2Double) == "String:Long:Int:Boolean2Double"
    result &= FunctionSignature.getSignatureStringLongLongInt2Double(helperStringLongLongInt2Double) == "String:Long:Long:Int2Double"
    result &= FunctionSignature.getSignatureStringLongLongLong2Double(helperStringLongLongLong2Double) == "String:Long:Long:Long2Double"
    result &= FunctionSignature.getSignatureStringLongLongDouble2Double(helperStringLongLongDouble2Double) == "String:Long:Long:Double2Double"
    result &= FunctionSignature.getSignatureStringLongLongString2Double(helperStringLongLongString2Double) == "String:Long:Long:String2Double"
    result &= FunctionSignature.getSignatureStringLongLongBoolean2Double(helperStringLongLongBoolean2Double) == "String:Long:Long:Boolean2Double"
    result &= FunctionSignature.getSignatureStringLongDoubleInt2Double(helperStringLongDoubleInt2Double) == "String:Long:Double:Int2Double"
    result &= FunctionSignature.getSignatureStringLongDoubleLong2Double(helperStringLongDoubleLong2Double) == "String:Long:Double:Long2Double"
    result &= FunctionSignature.getSignatureStringLongDoubleDouble2Double(helperStringLongDoubleDouble2Double) == "String:Long:Double:Double2Double"
    result &= FunctionSignature.getSignatureStringLongDoubleString2Double(helperStringLongDoubleString2Double) == "String:Long:Double:String2Double"
    result &= FunctionSignature.getSignatureStringLongDoubleBoolean2Double(helperStringLongDoubleBoolean2Double) == "String:Long:Double:Boolean2Double"
    result &= FunctionSignature.getSignatureStringLongStringInt2Double(helperStringLongStringInt2Double) == "String:Long:String:Int2Double"
    result &= FunctionSignature.getSignatureStringLongStringLong2Double(helperStringLongStringLong2Double) == "String:Long:String:Long2Double"
    result &= FunctionSignature.getSignatureStringLongStringDouble2Double(helperStringLongStringDouble2Double) == "String:Long:String:Double2Double"
    result &= FunctionSignature.getSignatureStringLongStringString2Double(helperStringLongStringString2Double) == "String:Long:String:String2Double"
    result &= FunctionSignature.getSignatureStringLongStringBoolean2Double(helperStringLongStringBoolean2Double) == "String:Long:String:Boolean2Double"
    result &= FunctionSignature.getSignatureStringLongBooleanInt2Double(helperStringLongBooleanInt2Double) == "String:Long:Boolean:Int2Double"
    result &= FunctionSignature.getSignatureStringLongBooleanLong2Double(helperStringLongBooleanLong2Double) == "String:Long:Boolean:Long2Double"
    result &= FunctionSignature.getSignatureStringLongBooleanDouble2Double(helperStringLongBooleanDouble2Double) == "String:Long:Boolean:Double2Double"
    result &= FunctionSignature.getSignatureStringLongBooleanString2Double(helperStringLongBooleanString2Double) == "String:Long:Boolean:String2Double"
    result &= FunctionSignature.getSignatureStringLongBooleanBoolean2Double(helperStringLongBooleanBoolean2Double) == "String:Long:Boolean:Boolean2Double"
    result &= FunctionSignature.getSignatureStringDoubleIntInt2Double(helperStringDoubleIntInt2Double) == "String:Double:Int:Int2Double"
    result &= FunctionSignature.getSignatureStringDoubleIntLong2Double(helperStringDoubleIntLong2Double) == "String:Double:Int:Long2Double"
    result &= FunctionSignature.getSignatureStringDoubleIntDouble2Double(helperStringDoubleIntDouble2Double) == "String:Double:Int:Double2Double"
    result &= FunctionSignature.getSignatureStringDoubleIntString2Double(helperStringDoubleIntString2Double) == "String:Double:Int:String2Double"
    result &= FunctionSignature.getSignatureStringDoubleIntBoolean2Double(helperStringDoubleIntBoolean2Double) == "String:Double:Int:Boolean2Double"
    result &= FunctionSignature.getSignatureStringDoubleLongInt2Double(helperStringDoubleLongInt2Double) == "String:Double:Long:Int2Double"
    result &= FunctionSignature.getSignatureStringDoubleLongLong2Double(helperStringDoubleLongLong2Double) == "String:Double:Long:Long2Double"
    result &= FunctionSignature.getSignatureStringDoubleLongDouble2Double(helperStringDoubleLongDouble2Double) == "String:Double:Long:Double2Double"
    result &= FunctionSignature.getSignatureStringDoubleLongString2Double(helperStringDoubleLongString2Double) == "String:Double:Long:String2Double"
    result &= FunctionSignature.getSignatureStringDoubleLongBoolean2Double(helperStringDoubleLongBoolean2Double) == "String:Double:Long:Boolean2Double"
    result &= FunctionSignature.getSignatureStringDoubleDoubleInt2Double(helperStringDoubleDoubleInt2Double) == "String:Double:Double:Int2Double"
    result &= FunctionSignature.getSignatureStringDoubleDoubleLong2Double(helperStringDoubleDoubleLong2Double) == "String:Double:Double:Long2Double"
    result &= FunctionSignature.getSignatureStringDoubleDoubleDouble2Double(helperStringDoubleDoubleDouble2Double) == "String:Double:Double:Double2Double"
    result &= FunctionSignature.getSignatureStringDoubleDoubleString2Double(helperStringDoubleDoubleString2Double) == "String:Double:Double:String2Double"
    result &= FunctionSignature.getSignatureStringDoubleDoubleBoolean2Double(helperStringDoubleDoubleBoolean2Double) == "String:Double:Double:Boolean2Double"
    result &= FunctionSignature.getSignatureStringDoubleStringInt2Double(helperStringDoubleStringInt2Double) == "String:Double:String:Int2Double"
    result &= FunctionSignature.getSignatureStringDoubleStringLong2Double(helperStringDoubleStringLong2Double) == "String:Double:String:Long2Double"
    result &= FunctionSignature.getSignatureStringDoubleStringDouble2Double(helperStringDoubleStringDouble2Double) == "String:Double:String:Double2Double"
    result &= FunctionSignature.getSignatureStringDoubleStringString2Double(helperStringDoubleStringString2Double) == "String:Double:String:String2Double"
    result &= FunctionSignature.getSignatureStringDoubleStringBoolean2Double(helperStringDoubleStringBoolean2Double) == "String:Double:String:Boolean2Double"
    result &= FunctionSignature.getSignatureStringDoubleBooleanInt2Double(helperStringDoubleBooleanInt2Double) == "String:Double:Boolean:Int2Double"
    result &= FunctionSignature.getSignatureStringDoubleBooleanLong2Double(helperStringDoubleBooleanLong2Double) == "String:Double:Boolean:Long2Double"
    result &= FunctionSignature.getSignatureStringDoubleBooleanDouble2Double(helperStringDoubleBooleanDouble2Double) == "String:Double:Boolean:Double2Double"
    result &= FunctionSignature.getSignatureStringDoubleBooleanString2Double(helperStringDoubleBooleanString2Double) == "String:Double:Boolean:String2Double"
    result &= FunctionSignature.getSignatureStringDoubleBooleanBoolean2Double(helperStringDoubleBooleanBoolean2Double) == "String:Double:Boolean:Boolean2Double"
    result &= FunctionSignature.getSignatureStringStringIntInt2Double(helperStringStringIntInt2Double) == "String:String:Int:Int2Double"
    result &= FunctionSignature.getSignatureStringStringIntLong2Double(helperStringStringIntLong2Double) == "String:String:Int:Long2Double"
    result &= FunctionSignature.getSignatureStringStringIntDouble2Double(helperStringStringIntDouble2Double) == "String:String:Int:Double2Double"
    result &= FunctionSignature.getSignatureStringStringIntString2Double(helperStringStringIntString2Double) == "String:String:Int:String2Double"
    result &= FunctionSignature.getSignatureStringStringIntBoolean2Double(helperStringStringIntBoolean2Double) == "String:String:Int:Boolean2Double"
    result &= FunctionSignature.getSignatureStringStringLongInt2Double(helperStringStringLongInt2Double) == "String:String:Long:Int2Double"
    result &= FunctionSignature.getSignatureStringStringLongLong2Double(helperStringStringLongLong2Double) == "String:String:Long:Long2Double"
    result &= FunctionSignature.getSignatureStringStringLongDouble2Double(helperStringStringLongDouble2Double) == "String:String:Long:Double2Double"
    result &= FunctionSignature.getSignatureStringStringLongString2Double(helperStringStringLongString2Double) == "String:String:Long:String2Double"
    result &= FunctionSignature.getSignatureStringStringLongBoolean2Double(helperStringStringLongBoolean2Double) == "String:String:Long:Boolean2Double"
    result &= FunctionSignature.getSignatureStringStringDoubleInt2Double(helperStringStringDoubleInt2Double) == "String:String:Double:Int2Double"
    result &= FunctionSignature.getSignatureStringStringDoubleLong2Double(helperStringStringDoubleLong2Double) == "String:String:Double:Long2Double"
    result &= FunctionSignature.getSignatureStringStringDoubleDouble2Double(helperStringStringDoubleDouble2Double) == "String:String:Double:Double2Double"
    result &= FunctionSignature.getSignatureStringStringDoubleString2Double(helperStringStringDoubleString2Double) == "String:String:Double:String2Double"
    result &= FunctionSignature.getSignatureStringStringDoubleBoolean2Double(helperStringStringDoubleBoolean2Double) == "String:String:Double:Boolean2Double"
    result &= FunctionSignature.getSignatureStringStringStringInt2Double(helperStringStringStringInt2Double) == "String:String:String:Int2Double"
    result &= FunctionSignature.getSignatureStringStringStringLong2Double(helperStringStringStringLong2Double) == "String:String:String:Long2Double"
    result &= FunctionSignature.getSignatureStringStringStringDouble2Double(helperStringStringStringDouble2Double) == "String:String:String:Double2Double"
    result &= FunctionSignature.getSignatureStringStringStringString2Double(helperStringStringStringString2Double) == "String:String:String:String2Double"
    result &= FunctionSignature.getSignatureStringStringStringBoolean2Double(helperStringStringStringBoolean2Double) == "String:String:String:Boolean2Double"
    result &= FunctionSignature.getSignatureStringStringBooleanInt2Double(helperStringStringBooleanInt2Double) == "String:String:Boolean:Int2Double"
    result &= FunctionSignature.getSignatureStringStringBooleanLong2Double(helperStringStringBooleanLong2Double) == "String:String:Boolean:Long2Double"
    result &= FunctionSignature.getSignatureStringStringBooleanDouble2Double(helperStringStringBooleanDouble2Double) == "String:String:Boolean:Double2Double"
    result &= FunctionSignature.getSignatureStringStringBooleanString2Double(helperStringStringBooleanString2Double) == "String:String:Boolean:String2Double"
    result &= FunctionSignature.getSignatureStringStringBooleanBoolean2Double(helperStringStringBooleanBoolean2Double) == "String:String:Boolean:Boolean2Double"
    result &= FunctionSignature.getSignatureStringBooleanIntInt2Double(helperStringBooleanIntInt2Double) == "String:Boolean:Int:Int2Double"
    result &= FunctionSignature.getSignatureStringBooleanIntLong2Double(helperStringBooleanIntLong2Double) == "String:Boolean:Int:Long2Double"
    result &= FunctionSignature.getSignatureStringBooleanIntDouble2Double(helperStringBooleanIntDouble2Double) == "String:Boolean:Int:Double2Double"
    result &= FunctionSignature.getSignatureStringBooleanIntString2Double(helperStringBooleanIntString2Double) == "String:Boolean:Int:String2Double"
    result &= FunctionSignature.getSignatureStringBooleanIntBoolean2Double(helperStringBooleanIntBoolean2Double) == "String:Boolean:Int:Boolean2Double"
    result &= FunctionSignature.getSignatureStringBooleanLongInt2Double(helperStringBooleanLongInt2Double) == "String:Boolean:Long:Int2Double"
    result &= FunctionSignature.getSignatureStringBooleanLongLong2Double(helperStringBooleanLongLong2Double) == "String:Boolean:Long:Long2Double"
    result &= FunctionSignature.getSignatureStringBooleanLongDouble2Double(helperStringBooleanLongDouble2Double) == "String:Boolean:Long:Double2Double"
    result &= FunctionSignature.getSignatureStringBooleanLongString2Double(helperStringBooleanLongString2Double) == "String:Boolean:Long:String2Double"
    result &= FunctionSignature.getSignatureStringBooleanLongBoolean2Double(helperStringBooleanLongBoolean2Double) == "String:Boolean:Long:Boolean2Double"
    result &= FunctionSignature.getSignatureStringBooleanDoubleInt2Double(helperStringBooleanDoubleInt2Double) == "String:Boolean:Double:Int2Double"
    result &= FunctionSignature.getSignatureStringBooleanDoubleLong2Double(helperStringBooleanDoubleLong2Double) == "String:Boolean:Double:Long2Double"
    result &= FunctionSignature.getSignatureStringBooleanDoubleDouble2Double(helperStringBooleanDoubleDouble2Double) == "String:Boolean:Double:Double2Double"
    result &= FunctionSignature.getSignatureStringBooleanDoubleString2Double(helperStringBooleanDoubleString2Double) == "String:Boolean:Double:String2Double"
    result &= FunctionSignature.getSignatureStringBooleanDoubleBoolean2Double(helperStringBooleanDoubleBoolean2Double) == "String:Boolean:Double:Boolean2Double"
    result &= FunctionSignature.getSignatureStringBooleanStringInt2Double(helperStringBooleanStringInt2Double) == "String:Boolean:String:Int2Double"
    result &= FunctionSignature.getSignatureStringBooleanStringLong2Double(helperStringBooleanStringLong2Double) == "String:Boolean:String:Long2Double"
    result &= FunctionSignature.getSignatureStringBooleanStringDouble2Double(helperStringBooleanStringDouble2Double) == "String:Boolean:String:Double2Double"
    result &= FunctionSignature.getSignatureStringBooleanStringString2Double(helperStringBooleanStringString2Double) == "String:Boolean:String:String2Double"
    result &= FunctionSignature.getSignatureStringBooleanStringBoolean2Double(helperStringBooleanStringBoolean2Double) == "String:Boolean:String:Boolean2Double"
    result &= FunctionSignature.getSignatureStringBooleanBooleanInt2Double(helperStringBooleanBooleanInt2Double) == "String:Boolean:Boolean:Int2Double"
    result &= FunctionSignature.getSignatureStringBooleanBooleanLong2Double(helperStringBooleanBooleanLong2Double) == "String:Boolean:Boolean:Long2Double"
    result &= FunctionSignature.getSignatureStringBooleanBooleanDouble2Double(helperStringBooleanBooleanDouble2Double) == "String:Boolean:Boolean:Double2Double"
    result &= FunctionSignature.getSignatureStringBooleanBooleanString2Double(helperStringBooleanBooleanString2Double) == "String:Boolean:Boolean:String2Double"
    result &= FunctionSignature.getSignatureStringBooleanBooleanBoolean2Double(helperStringBooleanBooleanBoolean2Double) == "String:Boolean:Boolean:Boolean2Double"
    result &= FunctionSignature.getSignatureBooleanIntIntInt2Double(helperBooleanIntIntInt2Double) == "Boolean:Int:Int:Int2Double"
    result &= FunctionSignature.getSignatureBooleanIntIntLong2Double(helperBooleanIntIntLong2Double) == "Boolean:Int:Int:Long2Double"
    result &= FunctionSignature.getSignatureBooleanIntIntDouble2Double(helperBooleanIntIntDouble2Double) == "Boolean:Int:Int:Double2Double"
    result &= FunctionSignature.getSignatureBooleanIntIntString2Double(helperBooleanIntIntString2Double) == "Boolean:Int:Int:String2Double"
    result &= FunctionSignature.getSignatureBooleanIntIntBoolean2Double(helperBooleanIntIntBoolean2Double) == "Boolean:Int:Int:Boolean2Double"
    result &= FunctionSignature.getSignatureBooleanIntLongInt2Double(helperBooleanIntLongInt2Double) == "Boolean:Int:Long:Int2Double"
    result &= FunctionSignature.getSignatureBooleanIntLongLong2Double(helperBooleanIntLongLong2Double) == "Boolean:Int:Long:Long2Double"
    result &= FunctionSignature.getSignatureBooleanIntLongDouble2Double(helperBooleanIntLongDouble2Double) == "Boolean:Int:Long:Double2Double"
    result &= FunctionSignature.getSignatureBooleanIntLongString2Double(helperBooleanIntLongString2Double) == "Boolean:Int:Long:String2Double"
    result &= FunctionSignature.getSignatureBooleanIntLongBoolean2Double(helperBooleanIntLongBoolean2Double) == "Boolean:Int:Long:Boolean2Double"
    result &= FunctionSignature.getSignatureBooleanIntDoubleInt2Double(helperBooleanIntDoubleInt2Double) == "Boolean:Int:Double:Int2Double"
    result &= FunctionSignature.getSignatureBooleanIntDoubleLong2Double(helperBooleanIntDoubleLong2Double) == "Boolean:Int:Double:Long2Double"
    result &= FunctionSignature.getSignatureBooleanIntDoubleDouble2Double(helperBooleanIntDoubleDouble2Double) == "Boolean:Int:Double:Double2Double"
    result &= FunctionSignature.getSignatureBooleanIntDoubleString2Double(helperBooleanIntDoubleString2Double) == "Boolean:Int:Double:String2Double"
    result &= FunctionSignature.getSignatureBooleanIntDoubleBoolean2Double(helperBooleanIntDoubleBoolean2Double) == "Boolean:Int:Double:Boolean2Double"
    result &= FunctionSignature.getSignatureBooleanIntStringInt2Double(helperBooleanIntStringInt2Double) == "Boolean:Int:String:Int2Double"
    result &= FunctionSignature.getSignatureBooleanIntStringLong2Double(helperBooleanIntStringLong2Double) == "Boolean:Int:String:Long2Double"
    result &= FunctionSignature.getSignatureBooleanIntStringDouble2Double(helperBooleanIntStringDouble2Double) == "Boolean:Int:String:Double2Double"
    result &= FunctionSignature.getSignatureBooleanIntStringString2Double(helperBooleanIntStringString2Double) == "Boolean:Int:String:String2Double"
    result &= FunctionSignature.getSignatureBooleanIntStringBoolean2Double(helperBooleanIntStringBoolean2Double) == "Boolean:Int:String:Boolean2Double"
    result &= FunctionSignature.getSignatureBooleanIntBooleanInt2Double(helperBooleanIntBooleanInt2Double) == "Boolean:Int:Boolean:Int2Double"
    result &= FunctionSignature.getSignatureBooleanIntBooleanLong2Double(helperBooleanIntBooleanLong2Double) == "Boolean:Int:Boolean:Long2Double"
    result &= FunctionSignature.getSignatureBooleanIntBooleanDouble2Double(helperBooleanIntBooleanDouble2Double) == "Boolean:Int:Boolean:Double2Double"
    result &= FunctionSignature.getSignatureBooleanIntBooleanString2Double(helperBooleanIntBooleanString2Double) == "Boolean:Int:Boolean:String2Double"
    result &= FunctionSignature.getSignatureBooleanIntBooleanBoolean2Double(helperBooleanIntBooleanBoolean2Double) == "Boolean:Int:Boolean:Boolean2Double"
    result &= FunctionSignature.getSignatureBooleanLongIntInt2Double(helperBooleanLongIntInt2Double) == "Boolean:Long:Int:Int2Double"
    result &= FunctionSignature.getSignatureBooleanLongIntLong2Double(helperBooleanLongIntLong2Double) == "Boolean:Long:Int:Long2Double"
    result &= FunctionSignature.getSignatureBooleanLongIntDouble2Double(helperBooleanLongIntDouble2Double) == "Boolean:Long:Int:Double2Double"
    result &= FunctionSignature.getSignatureBooleanLongIntString2Double(helperBooleanLongIntString2Double) == "Boolean:Long:Int:String2Double"
    result &= FunctionSignature.getSignatureBooleanLongIntBoolean2Double(helperBooleanLongIntBoolean2Double) == "Boolean:Long:Int:Boolean2Double"
    result &= FunctionSignature.getSignatureBooleanLongLongInt2Double(helperBooleanLongLongInt2Double) == "Boolean:Long:Long:Int2Double"
    result &= FunctionSignature.getSignatureBooleanLongLongLong2Double(helperBooleanLongLongLong2Double) == "Boolean:Long:Long:Long2Double"
    result &= FunctionSignature.getSignatureBooleanLongLongDouble2Double(helperBooleanLongLongDouble2Double) == "Boolean:Long:Long:Double2Double"
    result &= FunctionSignature.getSignatureBooleanLongLongString2Double(helperBooleanLongLongString2Double) == "Boolean:Long:Long:String2Double"
    result &= FunctionSignature.getSignatureBooleanLongLongBoolean2Double(helperBooleanLongLongBoolean2Double) == "Boolean:Long:Long:Boolean2Double"
    result &= FunctionSignature.getSignatureBooleanLongDoubleInt2Double(helperBooleanLongDoubleInt2Double) == "Boolean:Long:Double:Int2Double"
    result &= FunctionSignature.getSignatureBooleanLongDoubleLong2Double(helperBooleanLongDoubleLong2Double) == "Boolean:Long:Double:Long2Double"
    result &= FunctionSignature.getSignatureBooleanLongDoubleDouble2Double(helperBooleanLongDoubleDouble2Double) == "Boolean:Long:Double:Double2Double"
    result &= FunctionSignature.getSignatureBooleanLongDoubleString2Double(helperBooleanLongDoubleString2Double) == "Boolean:Long:Double:String2Double"
    result &= FunctionSignature.getSignatureBooleanLongDoubleBoolean2Double(helperBooleanLongDoubleBoolean2Double) == "Boolean:Long:Double:Boolean2Double"
    result &= FunctionSignature.getSignatureBooleanLongStringInt2Double(helperBooleanLongStringInt2Double) == "Boolean:Long:String:Int2Double"
    result &= FunctionSignature.getSignatureBooleanLongStringLong2Double(helperBooleanLongStringLong2Double) == "Boolean:Long:String:Long2Double"
    result &= FunctionSignature.getSignatureBooleanLongStringDouble2Double(helperBooleanLongStringDouble2Double) == "Boolean:Long:String:Double2Double"
    result &= FunctionSignature.getSignatureBooleanLongStringString2Double(helperBooleanLongStringString2Double) == "Boolean:Long:String:String2Double"
    result &= FunctionSignature.getSignatureBooleanLongStringBoolean2Double(helperBooleanLongStringBoolean2Double) == "Boolean:Long:String:Boolean2Double"
    result &= FunctionSignature.getSignatureBooleanLongBooleanInt2Double(helperBooleanLongBooleanInt2Double) == "Boolean:Long:Boolean:Int2Double"
    result &= FunctionSignature.getSignatureBooleanLongBooleanLong2Double(helperBooleanLongBooleanLong2Double) == "Boolean:Long:Boolean:Long2Double"
    result &= FunctionSignature.getSignatureBooleanLongBooleanDouble2Double(helperBooleanLongBooleanDouble2Double) == "Boolean:Long:Boolean:Double2Double"
    result &= FunctionSignature.getSignatureBooleanLongBooleanString2Double(helperBooleanLongBooleanString2Double) == "Boolean:Long:Boolean:String2Double"
    result &= FunctionSignature.getSignatureBooleanLongBooleanBoolean2Double(helperBooleanLongBooleanBoolean2Double) == "Boolean:Long:Boolean:Boolean2Double"
    result &= FunctionSignature.getSignatureBooleanDoubleIntInt2Double(helperBooleanDoubleIntInt2Double) == "Boolean:Double:Int:Int2Double"
    result &= FunctionSignature.getSignatureBooleanDoubleIntLong2Double(helperBooleanDoubleIntLong2Double) == "Boolean:Double:Int:Long2Double"
    result &= FunctionSignature.getSignatureBooleanDoubleIntDouble2Double(helperBooleanDoubleIntDouble2Double) == "Boolean:Double:Int:Double2Double"
    result &= FunctionSignature.getSignatureBooleanDoubleIntString2Double(helperBooleanDoubleIntString2Double) == "Boolean:Double:Int:String2Double"
    result &= FunctionSignature.getSignatureBooleanDoubleIntBoolean2Double(helperBooleanDoubleIntBoolean2Double) == "Boolean:Double:Int:Boolean2Double"
    result &= FunctionSignature.getSignatureBooleanDoubleLongInt2Double(helperBooleanDoubleLongInt2Double) == "Boolean:Double:Long:Int2Double"
    result &= FunctionSignature.getSignatureBooleanDoubleLongLong2Double(helperBooleanDoubleLongLong2Double) == "Boolean:Double:Long:Long2Double"
    result &= FunctionSignature.getSignatureBooleanDoubleLongDouble2Double(helperBooleanDoubleLongDouble2Double) == "Boolean:Double:Long:Double2Double"
    result &= FunctionSignature.getSignatureBooleanDoubleLongString2Double(helperBooleanDoubleLongString2Double) == "Boolean:Double:Long:String2Double"
    result &= FunctionSignature.getSignatureBooleanDoubleLongBoolean2Double(helperBooleanDoubleLongBoolean2Double) == "Boolean:Double:Long:Boolean2Double"
    result &= FunctionSignature.getSignatureBooleanDoubleDoubleInt2Double(helperBooleanDoubleDoubleInt2Double) == "Boolean:Double:Double:Int2Double"
    result &= FunctionSignature.getSignatureBooleanDoubleDoubleLong2Double(helperBooleanDoubleDoubleLong2Double) == "Boolean:Double:Double:Long2Double"
    result &= FunctionSignature.getSignatureBooleanDoubleDoubleDouble2Double(helperBooleanDoubleDoubleDouble2Double) == "Boolean:Double:Double:Double2Double"
    result &= FunctionSignature.getSignatureBooleanDoubleDoubleString2Double(helperBooleanDoubleDoubleString2Double) == "Boolean:Double:Double:String2Double"
    result &= FunctionSignature.getSignatureBooleanDoubleDoubleBoolean2Double(helperBooleanDoubleDoubleBoolean2Double) == "Boolean:Double:Double:Boolean2Double"
    result &= FunctionSignature.getSignatureBooleanDoubleStringInt2Double(helperBooleanDoubleStringInt2Double) == "Boolean:Double:String:Int2Double"
    result &= FunctionSignature.getSignatureBooleanDoubleStringLong2Double(helperBooleanDoubleStringLong2Double) == "Boolean:Double:String:Long2Double"
    result &= FunctionSignature.getSignatureBooleanDoubleStringDouble2Double(helperBooleanDoubleStringDouble2Double) == "Boolean:Double:String:Double2Double"
    result &= FunctionSignature.getSignatureBooleanDoubleStringString2Double(helperBooleanDoubleStringString2Double) == "Boolean:Double:String:String2Double"
    result &= FunctionSignature.getSignatureBooleanDoubleStringBoolean2Double(helperBooleanDoubleStringBoolean2Double) == "Boolean:Double:String:Boolean2Double"
    result &= FunctionSignature.getSignatureBooleanDoubleBooleanInt2Double(helperBooleanDoubleBooleanInt2Double) == "Boolean:Double:Boolean:Int2Double"
    result &= FunctionSignature.getSignatureBooleanDoubleBooleanLong2Double(helperBooleanDoubleBooleanLong2Double) == "Boolean:Double:Boolean:Long2Double"
    result &= FunctionSignature.getSignatureBooleanDoubleBooleanDouble2Double(helperBooleanDoubleBooleanDouble2Double) == "Boolean:Double:Boolean:Double2Double"
    result &= FunctionSignature.getSignatureBooleanDoubleBooleanString2Double(helperBooleanDoubleBooleanString2Double) == "Boolean:Double:Boolean:String2Double"
    result &= FunctionSignature.getSignatureBooleanDoubleBooleanBoolean2Double(helperBooleanDoubleBooleanBoolean2Double) == "Boolean:Double:Boolean:Boolean2Double"
    result &= FunctionSignature.getSignatureBooleanStringIntInt2Double(helperBooleanStringIntInt2Double) == "Boolean:String:Int:Int2Double"
    result &= FunctionSignature.getSignatureBooleanStringIntLong2Double(helperBooleanStringIntLong2Double) == "Boolean:String:Int:Long2Double"
    result &= FunctionSignature.getSignatureBooleanStringIntDouble2Double(helperBooleanStringIntDouble2Double) == "Boolean:String:Int:Double2Double"
    result &= FunctionSignature.getSignatureBooleanStringIntString2Double(helperBooleanStringIntString2Double) == "Boolean:String:Int:String2Double"
    result &= FunctionSignature.getSignatureBooleanStringIntBoolean2Double(helperBooleanStringIntBoolean2Double) == "Boolean:String:Int:Boolean2Double"
    result &= FunctionSignature.getSignatureBooleanStringLongInt2Double(helperBooleanStringLongInt2Double) == "Boolean:String:Long:Int2Double"
    result &= FunctionSignature.getSignatureBooleanStringLongLong2Double(helperBooleanStringLongLong2Double) == "Boolean:String:Long:Long2Double"
    result &= FunctionSignature.getSignatureBooleanStringLongDouble2Double(helperBooleanStringLongDouble2Double) == "Boolean:String:Long:Double2Double"
    result &= FunctionSignature.getSignatureBooleanStringLongString2Double(helperBooleanStringLongString2Double) == "Boolean:String:Long:String2Double"
    result &= FunctionSignature.getSignatureBooleanStringLongBoolean2Double(helperBooleanStringLongBoolean2Double) == "Boolean:String:Long:Boolean2Double"
    result &= FunctionSignature.getSignatureBooleanStringDoubleInt2Double(helperBooleanStringDoubleInt2Double) == "Boolean:String:Double:Int2Double"
    result &= FunctionSignature.getSignatureBooleanStringDoubleLong2Double(helperBooleanStringDoubleLong2Double) == "Boolean:String:Double:Long2Double"
    result &= FunctionSignature.getSignatureBooleanStringDoubleDouble2Double(helperBooleanStringDoubleDouble2Double) == "Boolean:String:Double:Double2Double"
    result &= FunctionSignature.getSignatureBooleanStringDoubleString2Double(helperBooleanStringDoubleString2Double) == "Boolean:String:Double:String2Double"
    result &= FunctionSignature.getSignatureBooleanStringDoubleBoolean2Double(helperBooleanStringDoubleBoolean2Double) == "Boolean:String:Double:Boolean2Double"
    result &= FunctionSignature.getSignatureBooleanStringStringInt2Double(helperBooleanStringStringInt2Double) == "Boolean:String:String:Int2Double"
    result &= FunctionSignature.getSignatureBooleanStringStringLong2Double(helperBooleanStringStringLong2Double) == "Boolean:String:String:Long2Double"
    result &= FunctionSignature.getSignatureBooleanStringStringDouble2Double(helperBooleanStringStringDouble2Double) == "Boolean:String:String:Double2Double"
    result &= FunctionSignature.getSignatureBooleanStringStringString2Double(helperBooleanStringStringString2Double) == "Boolean:String:String:String2Double"
    result &= FunctionSignature.getSignatureBooleanStringStringBoolean2Double(helperBooleanStringStringBoolean2Double) == "Boolean:String:String:Boolean2Double"
    result &= FunctionSignature.getSignatureBooleanStringBooleanInt2Double(helperBooleanStringBooleanInt2Double) == "Boolean:String:Boolean:Int2Double"
    result &= FunctionSignature.getSignatureBooleanStringBooleanLong2Double(helperBooleanStringBooleanLong2Double) == "Boolean:String:Boolean:Long2Double"
    result &= FunctionSignature.getSignatureBooleanStringBooleanDouble2Double(helperBooleanStringBooleanDouble2Double) == "Boolean:String:Boolean:Double2Double"
    result &= FunctionSignature.getSignatureBooleanStringBooleanString2Double(helperBooleanStringBooleanString2Double) == "Boolean:String:Boolean:String2Double"
    result &= FunctionSignature.getSignatureBooleanStringBooleanBoolean2Double(helperBooleanStringBooleanBoolean2Double) == "Boolean:String:Boolean:Boolean2Double"
    result &= FunctionSignature.getSignatureBooleanBooleanIntInt2Double(helperBooleanBooleanIntInt2Double) == "Boolean:Boolean:Int:Int2Double"
    result &= FunctionSignature.getSignatureBooleanBooleanIntLong2Double(helperBooleanBooleanIntLong2Double) == "Boolean:Boolean:Int:Long2Double"
    result &= FunctionSignature.getSignatureBooleanBooleanIntDouble2Double(helperBooleanBooleanIntDouble2Double) == "Boolean:Boolean:Int:Double2Double"
    result &= FunctionSignature.getSignatureBooleanBooleanIntString2Double(helperBooleanBooleanIntString2Double) == "Boolean:Boolean:Int:String2Double"
    result &= FunctionSignature.getSignatureBooleanBooleanIntBoolean2Double(helperBooleanBooleanIntBoolean2Double) == "Boolean:Boolean:Int:Boolean2Double"
    result &= FunctionSignature.getSignatureBooleanBooleanLongInt2Double(helperBooleanBooleanLongInt2Double) == "Boolean:Boolean:Long:Int2Double"
    result &= FunctionSignature.getSignatureBooleanBooleanLongLong2Double(helperBooleanBooleanLongLong2Double) == "Boolean:Boolean:Long:Long2Double"
    result &= FunctionSignature.getSignatureBooleanBooleanLongDouble2Double(helperBooleanBooleanLongDouble2Double) == "Boolean:Boolean:Long:Double2Double"
    result &= FunctionSignature.getSignatureBooleanBooleanLongString2Double(helperBooleanBooleanLongString2Double) == "Boolean:Boolean:Long:String2Double"
    result &= FunctionSignature.getSignatureBooleanBooleanLongBoolean2Double(helperBooleanBooleanLongBoolean2Double) == "Boolean:Boolean:Long:Boolean2Double"
    result &= FunctionSignature.getSignatureBooleanBooleanDoubleInt2Double(helperBooleanBooleanDoubleInt2Double) == "Boolean:Boolean:Double:Int2Double"
    result &= FunctionSignature.getSignatureBooleanBooleanDoubleLong2Double(helperBooleanBooleanDoubleLong2Double) == "Boolean:Boolean:Double:Long2Double"
    result &= FunctionSignature.getSignatureBooleanBooleanDoubleDouble2Double(helperBooleanBooleanDoubleDouble2Double) == "Boolean:Boolean:Double:Double2Double"
    result &= FunctionSignature.getSignatureBooleanBooleanDoubleString2Double(helperBooleanBooleanDoubleString2Double) == "Boolean:Boolean:Double:String2Double"
    result &= FunctionSignature.getSignatureBooleanBooleanDoubleBoolean2Double(helperBooleanBooleanDoubleBoolean2Double) == "Boolean:Boolean:Double:Boolean2Double"
    result &= FunctionSignature.getSignatureBooleanBooleanStringInt2Double(helperBooleanBooleanStringInt2Double) == "Boolean:Boolean:String:Int2Double"
    result &= FunctionSignature.getSignatureBooleanBooleanStringLong2Double(helperBooleanBooleanStringLong2Double) == "Boolean:Boolean:String:Long2Double"
    result &= FunctionSignature.getSignatureBooleanBooleanStringDouble2Double(helperBooleanBooleanStringDouble2Double) == "Boolean:Boolean:String:Double2Double"
    result &= FunctionSignature.getSignatureBooleanBooleanStringString2Double(helperBooleanBooleanStringString2Double) == "Boolean:Boolean:String:String2Double"
    result &= FunctionSignature.getSignatureBooleanBooleanStringBoolean2Double(helperBooleanBooleanStringBoolean2Double) == "Boolean:Boolean:String:Boolean2Double"
    result &= FunctionSignature.getSignatureBooleanBooleanBooleanInt2Double(helperBooleanBooleanBooleanInt2Double) == "Boolean:Boolean:Boolean:Int2Double"
    result &= FunctionSignature.getSignatureBooleanBooleanBooleanLong2Double(helperBooleanBooleanBooleanLong2Double) == "Boolean:Boolean:Boolean:Long2Double"
    result &= FunctionSignature.getSignatureBooleanBooleanBooleanDouble2Double(helperBooleanBooleanBooleanDouble2Double) == "Boolean:Boolean:Boolean:Double2Double"
    result &= FunctionSignature.getSignatureBooleanBooleanBooleanString2Double(helperBooleanBooleanBooleanString2Double) == "Boolean:Boolean:Boolean:String2Double"
    result &= FunctionSignature.getSignatureBooleanBooleanBooleanBoolean2Double(helperBooleanBooleanBooleanBoolean2Double) == "Boolean:Boolean:Boolean:Boolean2Double"
    assert(result)
  }
  def helperIntIntIntInt2String(a1: iFun, a2: iFun, a3: iFun, a4: iFun): sFun = sFunDummy
  def helperIntIntIntLong2String(a1: iFun, a2: iFun, a3: iFun, a4: lFun): sFun = sFunDummy
  def helperIntIntIntDouble2String(a1: iFun, a2: iFun, a3: iFun, a4: dFun): sFun = sFunDummy
  def helperIntIntIntString2String(a1: iFun, a2: iFun, a3: iFun, a4: sFun): sFun = sFunDummy
  def helperIntIntIntBoolean2String(a1: iFun, a2: iFun, a3: iFun, a4: bFun): sFun = sFunDummy
  def helperIntIntLongInt2String(a1: iFun, a2: iFun, a3: lFun, a4: iFun): sFun = sFunDummy
  def helperIntIntLongLong2String(a1: iFun, a2: iFun, a3: lFun, a4: lFun): sFun = sFunDummy
  def helperIntIntLongDouble2String(a1: iFun, a2: iFun, a3: lFun, a4: dFun): sFun = sFunDummy
  def helperIntIntLongString2String(a1: iFun, a2: iFun, a3: lFun, a4: sFun): sFun = sFunDummy
  def helperIntIntLongBoolean2String(a1: iFun, a2: iFun, a3: lFun, a4: bFun): sFun = sFunDummy
  def helperIntIntDoubleInt2String(a1: iFun, a2: iFun, a3: dFun, a4: iFun): sFun = sFunDummy
  def helperIntIntDoubleLong2String(a1: iFun, a2: iFun, a3: dFun, a4: lFun): sFun = sFunDummy
  def helperIntIntDoubleDouble2String(a1: iFun, a2: iFun, a3: dFun, a4: dFun): sFun = sFunDummy
  def helperIntIntDoubleString2String(a1: iFun, a2: iFun, a3: dFun, a4: sFun): sFun = sFunDummy
  def helperIntIntDoubleBoolean2String(a1: iFun, a2: iFun, a3: dFun, a4: bFun): sFun = sFunDummy
  def helperIntIntStringInt2String(a1: iFun, a2: iFun, a3: sFun, a4: iFun): sFun = sFunDummy
  def helperIntIntStringLong2String(a1: iFun, a2: iFun, a3: sFun, a4: lFun): sFun = sFunDummy
  def helperIntIntStringDouble2String(a1: iFun, a2: iFun, a3: sFun, a4: dFun): sFun = sFunDummy
  def helperIntIntStringString2String(a1: iFun, a2: iFun, a3: sFun, a4: sFun): sFun = sFunDummy
  def helperIntIntStringBoolean2String(a1: iFun, a2: iFun, a3: sFun, a4: bFun): sFun = sFunDummy
  def helperIntIntBooleanInt2String(a1: iFun, a2: iFun, a3: bFun, a4: iFun): sFun = sFunDummy
  def helperIntIntBooleanLong2String(a1: iFun, a2: iFun, a3: bFun, a4: lFun): sFun = sFunDummy
  def helperIntIntBooleanDouble2String(a1: iFun, a2: iFun, a3: bFun, a4: dFun): sFun = sFunDummy
  def helperIntIntBooleanString2String(a1: iFun, a2: iFun, a3: bFun, a4: sFun): sFun = sFunDummy
  def helperIntIntBooleanBoolean2String(a1: iFun, a2: iFun, a3: bFun, a4: bFun): sFun = sFunDummy
  def helperIntLongIntInt2String(a1: iFun, a2: lFun, a3: iFun, a4: iFun): sFun = sFunDummy
  def helperIntLongIntLong2String(a1: iFun, a2: lFun, a3: iFun, a4: lFun): sFun = sFunDummy
  def helperIntLongIntDouble2String(a1: iFun, a2: lFun, a3: iFun, a4: dFun): sFun = sFunDummy
  def helperIntLongIntString2String(a1: iFun, a2: lFun, a3: iFun, a4: sFun): sFun = sFunDummy
  def helperIntLongIntBoolean2String(a1: iFun, a2: lFun, a3: iFun, a4: bFun): sFun = sFunDummy
  def helperIntLongLongInt2String(a1: iFun, a2: lFun, a3: lFun, a4: iFun): sFun = sFunDummy
  def helperIntLongLongLong2String(a1: iFun, a2: lFun, a3: lFun, a4: lFun): sFun = sFunDummy
  def helperIntLongLongDouble2String(a1: iFun, a2: lFun, a3: lFun, a4: dFun): sFun = sFunDummy
  def helperIntLongLongString2String(a1: iFun, a2: lFun, a3: lFun, a4: sFun): sFun = sFunDummy
  def helperIntLongLongBoolean2String(a1: iFun, a2: lFun, a3: lFun, a4: bFun): sFun = sFunDummy
  def helperIntLongDoubleInt2String(a1: iFun, a2: lFun, a3: dFun, a4: iFun): sFun = sFunDummy
  def helperIntLongDoubleLong2String(a1: iFun, a2: lFun, a3: dFun, a4: lFun): sFun = sFunDummy
  def helperIntLongDoubleDouble2String(a1: iFun, a2: lFun, a3: dFun, a4: dFun): sFun = sFunDummy
  def helperIntLongDoubleString2String(a1: iFun, a2: lFun, a3: dFun, a4: sFun): sFun = sFunDummy
  def helperIntLongDoubleBoolean2String(a1: iFun, a2: lFun, a3: dFun, a4: bFun): sFun = sFunDummy
  def helperIntLongStringInt2String(a1: iFun, a2: lFun, a3: sFun, a4: iFun): sFun = sFunDummy
  def helperIntLongStringLong2String(a1: iFun, a2: lFun, a3: sFun, a4: lFun): sFun = sFunDummy
  def helperIntLongStringDouble2String(a1: iFun, a2: lFun, a3: sFun, a4: dFun): sFun = sFunDummy
  def helperIntLongStringString2String(a1: iFun, a2: lFun, a3: sFun, a4: sFun): sFun = sFunDummy
  def helperIntLongStringBoolean2String(a1: iFun, a2: lFun, a3: sFun, a4: bFun): sFun = sFunDummy
  def helperIntLongBooleanInt2String(a1: iFun, a2: lFun, a3: bFun, a4: iFun): sFun = sFunDummy
  def helperIntLongBooleanLong2String(a1: iFun, a2: lFun, a3: bFun, a4: lFun): sFun = sFunDummy
  def helperIntLongBooleanDouble2String(a1: iFun, a2: lFun, a3: bFun, a4: dFun): sFun = sFunDummy
  def helperIntLongBooleanString2String(a1: iFun, a2: lFun, a3: bFun, a4: sFun): sFun = sFunDummy
  def helperIntLongBooleanBoolean2String(a1: iFun, a2: lFun, a3: bFun, a4: bFun): sFun = sFunDummy
  def helperIntDoubleIntInt2String(a1: iFun, a2: dFun, a3: iFun, a4: iFun): sFun = sFunDummy
  def helperIntDoubleIntLong2String(a1: iFun, a2: dFun, a3: iFun, a4: lFun): sFun = sFunDummy
  def helperIntDoubleIntDouble2String(a1: iFun, a2: dFun, a3: iFun, a4: dFun): sFun = sFunDummy
  def helperIntDoubleIntString2String(a1: iFun, a2: dFun, a3: iFun, a4: sFun): sFun = sFunDummy
  def helperIntDoubleIntBoolean2String(a1: iFun, a2: dFun, a3: iFun, a4: bFun): sFun = sFunDummy
  def helperIntDoubleLongInt2String(a1: iFun, a2: dFun, a3: lFun, a4: iFun): sFun = sFunDummy
  def helperIntDoubleLongLong2String(a1: iFun, a2: dFun, a3: lFun, a4: lFun): sFun = sFunDummy
  def helperIntDoubleLongDouble2String(a1: iFun, a2: dFun, a3: lFun, a4: dFun): sFun = sFunDummy
  def helperIntDoubleLongString2String(a1: iFun, a2: dFun, a3: lFun, a4: sFun): sFun = sFunDummy
  def helperIntDoubleLongBoolean2String(a1: iFun, a2: dFun, a3: lFun, a4: bFun): sFun = sFunDummy
  def helperIntDoubleDoubleInt2String(a1: iFun, a2: dFun, a3: dFun, a4: iFun): sFun = sFunDummy
  def helperIntDoubleDoubleLong2String(a1: iFun, a2: dFun, a3: dFun, a4: lFun): sFun = sFunDummy
  def helperIntDoubleDoubleDouble2String(a1: iFun, a2: dFun, a3: dFun, a4: dFun): sFun = sFunDummy
  def helperIntDoubleDoubleString2String(a1: iFun, a2: dFun, a3: dFun, a4: sFun): sFun = sFunDummy
  def helperIntDoubleDoubleBoolean2String(a1: iFun, a2: dFun, a3: dFun, a4: bFun): sFun = sFunDummy
  def helperIntDoubleStringInt2String(a1: iFun, a2: dFun, a3: sFun, a4: iFun): sFun = sFunDummy
  def helperIntDoubleStringLong2String(a1: iFun, a2: dFun, a3: sFun, a4: lFun): sFun = sFunDummy
  def helperIntDoubleStringDouble2String(a1: iFun, a2: dFun, a3: sFun, a4: dFun): sFun = sFunDummy
  def helperIntDoubleStringString2String(a1: iFun, a2: dFun, a3: sFun, a4: sFun): sFun = sFunDummy
  def helperIntDoubleStringBoolean2String(a1: iFun, a2: dFun, a3: sFun, a4: bFun): sFun = sFunDummy
  def helperIntDoubleBooleanInt2String(a1: iFun, a2: dFun, a3: bFun, a4: iFun): sFun = sFunDummy
  def helperIntDoubleBooleanLong2String(a1: iFun, a2: dFun, a3: bFun, a4: lFun): sFun = sFunDummy
  def helperIntDoubleBooleanDouble2String(a1: iFun, a2: dFun, a3: bFun, a4: dFun): sFun = sFunDummy
  def helperIntDoubleBooleanString2String(a1: iFun, a2: dFun, a3: bFun, a4: sFun): sFun = sFunDummy
  def helperIntDoubleBooleanBoolean2String(a1: iFun, a2: dFun, a3: bFun, a4: bFun): sFun = sFunDummy
  def helperIntStringIntInt2String(a1: iFun, a2: sFun, a3: iFun, a4: iFun): sFun = sFunDummy
  def helperIntStringIntLong2String(a1: iFun, a2: sFun, a3: iFun, a4: lFun): sFun = sFunDummy
  def helperIntStringIntDouble2String(a1: iFun, a2: sFun, a3: iFun, a4: dFun): sFun = sFunDummy
  def helperIntStringIntString2String(a1: iFun, a2: sFun, a3: iFun, a4: sFun): sFun = sFunDummy
  def helperIntStringIntBoolean2String(a1: iFun, a2: sFun, a3: iFun, a4: bFun): sFun = sFunDummy
  def helperIntStringLongInt2String(a1: iFun, a2: sFun, a3: lFun, a4: iFun): sFun = sFunDummy
  def helperIntStringLongLong2String(a1: iFun, a2: sFun, a3: lFun, a4: lFun): sFun = sFunDummy
  def helperIntStringLongDouble2String(a1: iFun, a2: sFun, a3: lFun, a4: dFun): sFun = sFunDummy
  def helperIntStringLongString2String(a1: iFun, a2: sFun, a3: lFun, a4: sFun): sFun = sFunDummy
  def helperIntStringLongBoolean2String(a1: iFun, a2: sFun, a3: lFun, a4: bFun): sFun = sFunDummy
  def helperIntStringDoubleInt2String(a1: iFun, a2: sFun, a3: dFun, a4: iFun): sFun = sFunDummy
  def helperIntStringDoubleLong2String(a1: iFun, a2: sFun, a3: dFun, a4: lFun): sFun = sFunDummy
  def helperIntStringDoubleDouble2String(a1: iFun, a2: sFun, a3: dFun, a4: dFun): sFun = sFunDummy
  def helperIntStringDoubleString2String(a1: iFun, a2: sFun, a3: dFun, a4: sFun): sFun = sFunDummy
  def helperIntStringDoubleBoolean2String(a1: iFun, a2: sFun, a3: dFun, a4: bFun): sFun = sFunDummy
  def helperIntStringStringInt2String(a1: iFun, a2: sFun, a3: sFun, a4: iFun): sFun = sFunDummy
  def helperIntStringStringLong2String(a1: iFun, a2: sFun, a3: sFun, a4: lFun): sFun = sFunDummy
  def helperIntStringStringDouble2String(a1: iFun, a2: sFun, a3: sFun, a4: dFun): sFun = sFunDummy
  def helperIntStringStringString2String(a1: iFun, a2: sFun, a3: sFun, a4: sFun): sFun = sFunDummy
  def helperIntStringStringBoolean2String(a1: iFun, a2: sFun, a3: sFun, a4: bFun): sFun = sFunDummy
  def helperIntStringBooleanInt2String(a1: iFun, a2: sFun, a3: bFun, a4: iFun): sFun = sFunDummy
  def helperIntStringBooleanLong2String(a1: iFun, a2: sFun, a3: bFun, a4: lFun): sFun = sFunDummy
  def helperIntStringBooleanDouble2String(a1: iFun, a2: sFun, a3: bFun, a4: dFun): sFun = sFunDummy
  def helperIntStringBooleanString2String(a1: iFun, a2: sFun, a3: bFun, a4: sFun): sFun = sFunDummy
  def helperIntStringBooleanBoolean2String(a1: iFun, a2: sFun, a3: bFun, a4: bFun): sFun = sFunDummy
  def helperIntBooleanIntInt2String(a1: iFun, a2: bFun, a3: iFun, a4: iFun): sFun = sFunDummy
  def helperIntBooleanIntLong2String(a1: iFun, a2: bFun, a3: iFun, a4: lFun): sFun = sFunDummy
  def helperIntBooleanIntDouble2String(a1: iFun, a2: bFun, a3: iFun, a4: dFun): sFun = sFunDummy
  def helperIntBooleanIntString2String(a1: iFun, a2: bFun, a3: iFun, a4: sFun): sFun = sFunDummy
  def helperIntBooleanIntBoolean2String(a1: iFun, a2: bFun, a3: iFun, a4: bFun): sFun = sFunDummy
  def helperIntBooleanLongInt2String(a1: iFun, a2: bFun, a3: lFun, a4: iFun): sFun = sFunDummy
  def helperIntBooleanLongLong2String(a1: iFun, a2: bFun, a3: lFun, a4: lFun): sFun = sFunDummy
  def helperIntBooleanLongDouble2String(a1: iFun, a2: bFun, a3: lFun, a4: dFun): sFun = sFunDummy
  def helperIntBooleanLongString2String(a1: iFun, a2: bFun, a3: lFun, a4: sFun): sFun = sFunDummy
  def helperIntBooleanLongBoolean2String(a1: iFun, a2: bFun, a3: lFun, a4: bFun): sFun = sFunDummy
  def helperIntBooleanDoubleInt2String(a1: iFun, a2: bFun, a3: dFun, a4: iFun): sFun = sFunDummy
  def helperIntBooleanDoubleLong2String(a1: iFun, a2: bFun, a3: dFun, a4: lFun): sFun = sFunDummy
  def helperIntBooleanDoubleDouble2String(a1: iFun, a2: bFun, a3: dFun, a4: dFun): sFun = sFunDummy
  def helperIntBooleanDoubleString2String(a1: iFun, a2: bFun, a3: dFun, a4: sFun): sFun = sFunDummy
  def helperIntBooleanDoubleBoolean2String(a1: iFun, a2: bFun, a3: dFun, a4: bFun): sFun = sFunDummy
  def helperIntBooleanStringInt2String(a1: iFun, a2: bFun, a3: sFun, a4: iFun): sFun = sFunDummy
  def helperIntBooleanStringLong2String(a1: iFun, a2: bFun, a3: sFun, a4: lFun): sFun = sFunDummy
  def helperIntBooleanStringDouble2String(a1: iFun, a2: bFun, a3: sFun, a4: dFun): sFun = sFunDummy
  def helperIntBooleanStringString2String(a1: iFun, a2: bFun, a3: sFun, a4: sFun): sFun = sFunDummy
  def helperIntBooleanStringBoolean2String(a1: iFun, a2: bFun, a3: sFun, a4: bFun): sFun = sFunDummy
  def helperIntBooleanBooleanInt2String(a1: iFun, a2: bFun, a3: bFun, a4: iFun): sFun = sFunDummy
  def helperIntBooleanBooleanLong2String(a1: iFun, a2: bFun, a3: bFun, a4: lFun): sFun = sFunDummy
  def helperIntBooleanBooleanDouble2String(a1: iFun, a2: bFun, a3: bFun, a4: dFun): sFun = sFunDummy
  def helperIntBooleanBooleanString2String(a1: iFun, a2: bFun, a3: bFun, a4: sFun): sFun = sFunDummy
  def helperIntBooleanBooleanBoolean2String(a1: iFun, a2: bFun, a3: bFun, a4: bFun): sFun = sFunDummy
  def helperLongIntIntInt2String(a1: lFun, a2: iFun, a3: iFun, a4: iFun): sFun = sFunDummy
  def helperLongIntIntLong2String(a1: lFun, a2: iFun, a3: iFun, a4: lFun): sFun = sFunDummy
  def helperLongIntIntDouble2String(a1: lFun, a2: iFun, a3: iFun, a4: dFun): sFun = sFunDummy
  def helperLongIntIntString2String(a1: lFun, a2: iFun, a3: iFun, a4: sFun): sFun = sFunDummy
  def helperLongIntIntBoolean2String(a1: lFun, a2: iFun, a3: iFun, a4: bFun): sFun = sFunDummy
  def helperLongIntLongInt2String(a1: lFun, a2: iFun, a3: lFun, a4: iFun): sFun = sFunDummy
  def helperLongIntLongLong2String(a1: lFun, a2: iFun, a3: lFun, a4: lFun): sFun = sFunDummy
  def helperLongIntLongDouble2String(a1: lFun, a2: iFun, a3: lFun, a4: dFun): sFun = sFunDummy
  def helperLongIntLongString2String(a1: lFun, a2: iFun, a3: lFun, a4: sFun): sFun = sFunDummy
  def helperLongIntLongBoolean2String(a1: lFun, a2: iFun, a3: lFun, a4: bFun): sFun = sFunDummy
  def helperLongIntDoubleInt2String(a1: lFun, a2: iFun, a3: dFun, a4: iFun): sFun = sFunDummy
  def helperLongIntDoubleLong2String(a1: lFun, a2: iFun, a3: dFun, a4: lFun): sFun = sFunDummy
  def helperLongIntDoubleDouble2String(a1: lFun, a2: iFun, a3: dFun, a4: dFun): sFun = sFunDummy
  def helperLongIntDoubleString2String(a1: lFun, a2: iFun, a3: dFun, a4: sFun): sFun = sFunDummy
  def helperLongIntDoubleBoolean2String(a1: lFun, a2: iFun, a3: dFun, a4: bFun): sFun = sFunDummy
  def helperLongIntStringInt2String(a1: lFun, a2: iFun, a3: sFun, a4: iFun): sFun = sFunDummy
  def helperLongIntStringLong2String(a1: lFun, a2: iFun, a3: sFun, a4: lFun): sFun = sFunDummy
  def helperLongIntStringDouble2String(a1: lFun, a2: iFun, a3: sFun, a4: dFun): sFun = sFunDummy
  def helperLongIntStringString2String(a1: lFun, a2: iFun, a3: sFun, a4: sFun): sFun = sFunDummy
  def helperLongIntStringBoolean2String(a1: lFun, a2: iFun, a3: sFun, a4: bFun): sFun = sFunDummy
  def helperLongIntBooleanInt2String(a1: lFun, a2: iFun, a3: bFun, a4: iFun): sFun = sFunDummy
  def helperLongIntBooleanLong2String(a1: lFun, a2: iFun, a3: bFun, a4: lFun): sFun = sFunDummy
  def helperLongIntBooleanDouble2String(a1: lFun, a2: iFun, a3: bFun, a4: dFun): sFun = sFunDummy
  def helperLongIntBooleanString2String(a1: lFun, a2: iFun, a3: bFun, a4: sFun): sFun = sFunDummy
  def helperLongIntBooleanBoolean2String(a1: lFun, a2: iFun, a3: bFun, a4: bFun): sFun = sFunDummy
  def helperLongLongIntInt2String(a1: lFun, a2: lFun, a3: iFun, a4: iFun): sFun = sFunDummy
  def helperLongLongIntLong2String(a1: lFun, a2: lFun, a3: iFun, a4: lFun): sFun = sFunDummy
  def helperLongLongIntDouble2String(a1: lFun, a2: lFun, a3: iFun, a4: dFun): sFun = sFunDummy
  def helperLongLongIntString2String(a1: lFun, a2: lFun, a3: iFun, a4: sFun): sFun = sFunDummy
  def helperLongLongIntBoolean2String(a1: lFun, a2: lFun, a3: iFun, a4: bFun): sFun = sFunDummy
  def helperLongLongLongInt2String(a1: lFun, a2: lFun, a3: lFun, a4: iFun): sFun = sFunDummy
  def helperLongLongLongLong2String(a1: lFun, a2: lFun, a3: lFun, a4: lFun): sFun = sFunDummy
  def helperLongLongLongDouble2String(a1: lFun, a2: lFun, a3: lFun, a4: dFun): sFun = sFunDummy
  def helperLongLongLongString2String(a1: lFun, a2: lFun, a3: lFun, a4: sFun): sFun = sFunDummy
  def helperLongLongLongBoolean2String(a1: lFun, a2: lFun, a3: lFun, a4: bFun): sFun = sFunDummy
  def helperLongLongDoubleInt2String(a1: lFun, a2: lFun, a3: dFun, a4: iFun): sFun = sFunDummy
  def helperLongLongDoubleLong2String(a1: lFun, a2: lFun, a3: dFun, a4: lFun): sFun = sFunDummy
  def helperLongLongDoubleDouble2String(a1: lFun, a2: lFun, a3: dFun, a4: dFun): sFun = sFunDummy
  def helperLongLongDoubleString2String(a1: lFun, a2: lFun, a3: dFun, a4: sFun): sFun = sFunDummy
  def helperLongLongDoubleBoolean2String(a1: lFun, a2: lFun, a3: dFun, a4: bFun): sFun = sFunDummy
  def helperLongLongStringInt2String(a1: lFun, a2: lFun, a3: sFun, a4: iFun): sFun = sFunDummy
  def helperLongLongStringLong2String(a1: lFun, a2: lFun, a3: sFun, a4: lFun): sFun = sFunDummy
  def helperLongLongStringDouble2String(a1: lFun, a2: lFun, a3: sFun, a4: dFun): sFun = sFunDummy
  def helperLongLongStringString2String(a1: lFun, a2: lFun, a3: sFun, a4: sFun): sFun = sFunDummy
  def helperLongLongStringBoolean2String(a1: lFun, a2: lFun, a3: sFun, a4: bFun): sFun = sFunDummy
  def helperLongLongBooleanInt2String(a1: lFun, a2: lFun, a3: bFun, a4: iFun): sFun = sFunDummy
  def helperLongLongBooleanLong2String(a1: lFun, a2: lFun, a3: bFun, a4: lFun): sFun = sFunDummy
  def helperLongLongBooleanDouble2String(a1: lFun, a2: lFun, a3: bFun, a4: dFun): sFun = sFunDummy
  def helperLongLongBooleanString2String(a1: lFun, a2: lFun, a3: bFun, a4: sFun): sFun = sFunDummy
  def helperLongLongBooleanBoolean2String(a1: lFun, a2: lFun, a3: bFun, a4: bFun): sFun = sFunDummy
  def helperLongDoubleIntInt2String(a1: lFun, a2: dFun, a3: iFun, a4: iFun): sFun = sFunDummy
  def helperLongDoubleIntLong2String(a1: lFun, a2: dFun, a3: iFun, a4: lFun): sFun = sFunDummy
  def helperLongDoubleIntDouble2String(a1: lFun, a2: dFun, a3: iFun, a4: dFun): sFun = sFunDummy
  def helperLongDoubleIntString2String(a1: lFun, a2: dFun, a3: iFun, a4: sFun): sFun = sFunDummy
  def helperLongDoubleIntBoolean2String(a1: lFun, a2: dFun, a3: iFun, a4: bFun): sFun = sFunDummy
  def helperLongDoubleLongInt2String(a1: lFun, a2: dFun, a3: lFun, a4: iFun): sFun = sFunDummy
  def helperLongDoubleLongLong2String(a1: lFun, a2: dFun, a3: lFun, a4: lFun): sFun = sFunDummy
  def helperLongDoubleLongDouble2String(a1: lFun, a2: dFun, a3: lFun, a4: dFun): sFun = sFunDummy
  def helperLongDoubleLongString2String(a1: lFun, a2: dFun, a3: lFun, a4: sFun): sFun = sFunDummy
  def helperLongDoubleLongBoolean2String(a1: lFun, a2: dFun, a3: lFun, a4: bFun): sFun = sFunDummy
  def helperLongDoubleDoubleInt2String(a1: lFun, a2: dFun, a3: dFun, a4: iFun): sFun = sFunDummy
  def helperLongDoubleDoubleLong2String(a1: lFun, a2: dFun, a3: dFun, a4: lFun): sFun = sFunDummy
  def helperLongDoubleDoubleDouble2String(a1: lFun, a2: dFun, a3: dFun, a4: dFun): sFun = sFunDummy
  def helperLongDoubleDoubleString2String(a1: lFun, a2: dFun, a3: dFun, a4: sFun): sFun = sFunDummy
  def helperLongDoubleDoubleBoolean2String(a1: lFun, a2: dFun, a3: dFun, a4: bFun): sFun = sFunDummy
  def helperLongDoubleStringInt2String(a1: lFun, a2: dFun, a3: sFun, a4: iFun): sFun = sFunDummy
  def helperLongDoubleStringLong2String(a1: lFun, a2: dFun, a3: sFun, a4: lFun): sFun = sFunDummy
  def helperLongDoubleStringDouble2String(a1: lFun, a2: dFun, a3: sFun, a4: dFun): sFun = sFunDummy
  def helperLongDoubleStringString2String(a1: lFun, a2: dFun, a3: sFun, a4: sFun): sFun = sFunDummy
  def helperLongDoubleStringBoolean2String(a1: lFun, a2: dFun, a3: sFun, a4: bFun): sFun = sFunDummy
  def helperLongDoubleBooleanInt2String(a1: lFun, a2: dFun, a3: bFun, a4: iFun): sFun = sFunDummy
  def helperLongDoubleBooleanLong2String(a1: lFun, a2: dFun, a3: bFun, a4: lFun): sFun = sFunDummy
  def helperLongDoubleBooleanDouble2String(a1: lFun, a2: dFun, a3: bFun, a4: dFun): sFun = sFunDummy
  def helperLongDoubleBooleanString2String(a1: lFun, a2: dFun, a3: bFun, a4: sFun): sFun = sFunDummy
  def helperLongDoubleBooleanBoolean2String(a1: lFun, a2: dFun, a3: bFun, a4: bFun): sFun = sFunDummy
  def helperLongStringIntInt2String(a1: lFun, a2: sFun, a3: iFun, a4: iFun): sFun = sFunDummy
  def helperLongStringIntLong2String(a1: lFun, a2: sFun, a3: iFun, a4: lFun): sFun = sFunDummy
  def helperLongStringIntDouble2String(a1: lFun, a2: sFun, a3: iFun, a4: dFun): sFun = sFunDummy
  def helperLongStringIntString2String(a1: lFun, a2: sFun, a3: iFun, a4: sFun): sFun = sFunDummy
  def helperLongStringIntBoolean2String(a1: lFun, a2: sFun, a3: iFun, a4: bFun): sFun = sFunDummy
  def helperLongStringLongInt2String(a1: lFun, a2: sFun, a3: lFun, a4: iFun): sFun = sFunDummy
  def helperLongStringLongLong2String(a1: lFun, a2: sFun, a3: lFun, a4: lFun): sFun = sFunDummy
  def helperLongStringLongDouble2String(a1: lFun, a2: sFun, a3: lFun, a4: dFun): sFun = sFunDummy
  def helperLongStringLongString2String(a1: lFun, a2: sFun, a3: lFun, a4: sFun): sFun = sFunDummy
  def helperLongStringLongBoolean2String(a1: lFun, a2: sFun, a3: lFun, a4: bFun): sFun = sFunDummy
  def helperLongStringDoubleInt2String(a1: lFun, a2: sFun, a3: dFun, a4: iFun): sFun = sFunDummy
  def helperLongStringDoubleLong2String(a1: lFun, a2: sFun, a3: dFun, a4: lFun): sFun = sFunDummy
  def helperLongStringDoubleDouble2String(a1: lFun, a2: sFun, a3: dFun, a4: dFun): sFun = sFunDummy
  def helperLongStringDoubleString2String(a1: lFun, a2: sFun, a3: dFun, a4: sFun): sFun = sFunDummy
  def helperLongStringDoubleBoolean2String(a1: lFun, a2: sFun, a3: dFun, a4: bFun): sFun = sFunDummy
  def helperLongStringStringInt2String(a1: lFun, a2: sFun, a3: sFun, a4: iFun): sFun = sFunDummy
  def helperLongStringStringLong2String(a1: lFun, a2: sFun, a3: sFun, a4: lFun): sFun = sFunDummy
  def helperLongStringStringDouble2String(a1: lFun, a2: sFun, a3: sFun, a4: dFun): sFun = sFunDummy
  def helperLongStringStringString2String(a1: lFun, a2: sFun, a3: sFun, a4: sFun): sFun = sFunDummy
  def helperLongStringStringBoolean2String(a1: lFun, a2: sFun, a3: sFun, a4: bFun): sFun = sFunDummy
  def helperLongStringBooleanInt2String(a1: lFun, a2: sFun, a3: bFun, a4: iFun): sFun = sFunDummy
  def helperLongStringBooleanLong2String(a1: lFun, a2: sFun, a3: bFun, a4: lFun): sFun = sFunDummy
  def helperLongStringBooleanDouble2String(a1: lFun, a2: sFun, a3: bFun, a4: dFun): sFun = sFunDummy
  def helperLongStringBooleanString2String(a1: lFun, a2: sFun, a3: bFun, a4: sFun): sFun = sFunDummy
  def helperLongStringBooleanBoolean2String(a1: lFun, a2: sFun, a3: bFun, a4: bFun): sFun = sFunDummy
  def helperLongBooleanIntInt2String(a1: lFun, a2: bFun, a3: iFun, a4: iFun): sFun = sFunDummy
  def helperLongBooleanIntLong2String(a1: lFun, a2: bFun, a3: iFun, a4: lFun): sFun = sFunDummy
  def helperLongBooleanIntDouble2String(a1: lFun, a2: bFun, a3: iFun, a4: dFun): sFun = sFunDummy
  def helperLongBooleanIntString2String(a1: lFun, a2: bFun, a3: iFun, a4: sFun): sFun = sFunDummy
  def helperLongBooleanIntBoolean2String(a1: lFun, a2: bFun, a3: iFun, a4: bFun): sFun = sFunDummy
  def helperLongBooleanLongInt2String(a1: lFun, a2: bFun, a3: lFun, a4: iFun): sFun = sFunDummy
  def helperLongBooleanLongLong2String(a1: lFun, a2: bFun, a3: lFun, a4: lFun): sFun = sFunDummy
  def helperLongBooleanLongDouble2String(a1: lFun, a2: bFun, a3: lFun, a4: dFun): sFun = sFunDummy
  def helperLongBooleanLongString2String(a1: lFun, a2: bFun, a3: lFun, a4: sFun): sFun = sFunDummy
  def helperLongBooleanLongBoolean2String(a1: lFun, a2: bFun, a3: lFun, a4: bFun): sFun = sFunDummy
  def helperLongBooleanDoubleInt2String(a1: lFun, a2: bFun, a3: dFun, a4: iFun): sFun = sFunDummy
  def helperLongBooleanDoubleLong2String(a1: lFun, a2: bFun, a3: dFun, a4: lFun): sFun = sFunDummy
  def helperLongBooleanDoubleDouble2String(a1: lFun, a2: bFun, a3: dFun, a4: dFun): sFun = sFunDummy
  def helperLongBooleanDoubleString2String(a1: lFun, a2: bFun, a3: dFun, a4: sFun): sFun = sFunDummy
  def helperLongBooleanDoubleBoolean2String(a1: lFun, a2: bFun, a3: dFun, a4: bFun): sFun = sFunDummy
  def helperLongBooleanStringInt2String(a1: lFun, a2: bFun, a3: sFun, a4: iFun): sFun = sFunDummy
  def helperLongBooleanStringLong2String(a1: lFun, a2: bFun, a3: sFun, a4: lFun): sFun = sFunDummy
  def helperLongBooleanStringDouble2String(a1: lFun, a2: bFun, a3: sFun, a4: dFun): sFun = sFunDummy
  def helperLongBooleanStringString2String(a1: lFun, a2: bFun, a3: sFun, a4: sFun): sFun = sFunDummy
  def helperLongBooleanStringBoolean2String(a1: lFun, a2: bFun, a3: sFun, a4: bFun): sFun = sFunDummy
  def helperLongBooleanBooleanInt2String(a1: lFun, a2: bFun, a3: bFun, a4: iFun): sFun = sFunDummy
  def helperLongBooleanBooleanLong2String(a1: lFun, a2: bFun, a3: bFun, a4: lFun): sFun = sFunDummy
  def helperLongBooleanBooleanDouble2String(a1: lFun, a2: bFun, a3: bFun, a4: dFun): sFun = sFunDummy
  def helperLongBooleanBooleanString2String(a1: lFun, a2: bFun, a3: bFun, a4: sFun): sFun = sFunDummy
  def helperLongBooleanBooleanBoolean2String(a1: lFun, a2: bFun, a3: bFun, a4: bFun): sFun = sFunDummy
  def helperDoubleIntIntInt2String(a1: dFun, a2: iFun, a3: iFun, a4: iFun): sFun = sFunDummy
  def helperDoubleIntIntLong2String(a1: dFun, a2: iFun, a3: iFun, a4: lFun): sFun = sFunDummy
  def helperDoubleIntIntDouble2String(a1: dFun, a2: iFun, a3: iFun, a4: dFun): sFun = sFunDummy
  def helperDoubleIntIntString2String(a1: dFun, a2: iFun, a3: iFun, a4: sFun): sFun = sFunDummy
  def helperDoubleIntIntBoolean2String(a1: dFun, a2: iFun, a3: iFun, a4: bFun): sFun = sFunDummy
  def helperDoubleIntLongInt2String(a1: dFun, a2: iFun, a3: lFun, a4: iFun): sFun = sFunDummy
  def helperDoubleIntLongLong2String(a1: dFun, a2: iFun, a3: lFun, a4: lFun): sFun = sFunDummy
  def helperDoubleIntLongDouble2String(a1: dFun, a2: iFun, a3: lFun, a4: dFun): sFun = sFunDummy
  def helperDoubleIntLongString2String(a1: dFun, a2: iFun, a3: lFun, a4: sFun): sFun = sFunDummy
  def helperDoubleIntLongBoolean2String(a1: dFun, a2: iFun, a3: lFun, a4: bFun): sFun = sFunDummy
  def helperDoubleIntDoubleInt2String(a1: dFun, a2: iFun, a3: dFun, a4: iFun): sFun = sFunDummy
  def helperDoubleIntDoubleLong2String(a1: dFun, a2: iFun, a3: dFun, a4: lFun): sFun = sFunDummy
  def helperDoubleIntDoubleDouble2String(a1: dFun, a2: iFun, a3: dFun, a4: dFun): sFun = sFunDummy
  def helperDoubleIntDoubleString2String(a1: dFun, a2: iFun, a3: dFun, a4: sFun): sFun = sFunDummy
  def helperDoubleIntDoubleBoolean2String(a1: dFun, a2: iFun, a3: dFun, a4: bFun): sFun = sFunDummy
  def helperDoubleIntStringInt2String(a1: dFun, a2: iFun, a3: sFun, a4: iFun): sFun = sFunDummy
  def helperDoubleIntStringLong2String(a1: dFun, a2: iFun, a3: sFun, a4: lFun): sFun = sFunDummy
  def helperDoubleIntStringDouble2String(a1: dFun, a2: iFun, a3: sFun, a4: dFun): sFun = sFunDummy
  def helperDoubleIntStringString2String(a1: dFun, a2: iFun, a3: sFun, a4: sFun): sFun = sFunDummy
  def helperDoubleIntStringBoolean2String(a1: dFun, a2: iFun, a3: sFun, a4: bFun): sFun = sFunDummy
  def helperDoubleIntBooleanInt2String(a1: dFun, a2: iFun, a3: bFun, a4: iFun): sFun = sFunDummy
  def helperDoubleIntBooleanLong2String(a1: dFun, a2: iFun, a3: bFun, a4: lFun): sFun = sFunDummy
  def helperDoubleIntBooleanDouble2String(a1: dFun, a2: iFun, a3: bFun, a4: dFun): sFun = sFunDummy
  def helperDoubleIntBooleanString2String(a1: dFun, a2: iFun, a3: bFun, a4: sFun): sFun = sFunDummy
  def helperDoubleIntBooleanBoolean2String(a1: dFun, a2: iFun, a3: bFun, a4: bFun): sFun = sFunDummy
  def helperDoubleLongIntInt2String(a1: dFun, a2: lFun, a3: iFun, a4: iFun): sFun = sFunDummy
  def helperDoubleLongIntLong2String(a1: dFun, a2: lFun, a3: iFun, a4: lFun): sFun = sFunDummy
  def helperDoubleLongIntDouble2String(a1: dFun, a2: lFun, a3: iFun, a4: dFun): sFun = sFunDummy
  def helperDoubleLongIntString2String(a1: dFun, a2: lFun, a3: iFun, a4: sFun): sFun = sFunDummy
  def helperDoubleLongIntBoolean2String(a1: dFun, a2: lFun, a3: iFun, a4: bFun): sFun = sFunDummy
  def helperDoubleLongLongInt2String(a1: dFun, a2: lFun, a3: lFun, a4: iFun): sFun = sFunDummy
  def helperDoubleLongLongLong2String(a1: dFun, a2: lFun, a3: lFun, a4: lFun): sFun = sFunDummy
  def helperDoubleLongLongDouble2String(a1: dFun, a2: lFun, a3: lFun, a4: dFun): sFun = sFunDummy
  def helperDoubleLongLongString2String(a1: dFun, a2: lFun, a3: lFun, a4: sFun): sFun = sFunDummy
  def helperDoubleLongLongBoolean2String(a1: dFun, a2: lFun, a3: lFun, a4: bFun): sFun = sFunDummy
  def helperDoubleLongDoubleInt2String(a1: dFun, a2: lFun, a3: dFun, a4: iFun): sFun = sFunDummy
  def helperDoubleLongDoubleLong2String(a1: dFun, a2: lFun, a3: dFun, a4: lFun): sFun = sFunDummy
  def helperDoubleLongDoubleDouble2String(a1: dFun, a2: lFun, a3: dFun, a4: dFun): sFun = sFunDummy
  def helperDoubleLongDoubleString2String(a1: dFun, a2: lFun, a3: dFun, a4: sFun): sFun = sFunDummy
  def helperDoubleLongDoubleBoolean2String(a1: dFun, a2: lFun, a3: dFun, a4: bFun): sFun = sFunDummy
  def helperDoubleLongStringInt2String(a1: dFun, a2: lFun, a3: sFun, a4: iFun): sFun = sFunDummy
  def helperDoubleLongStringLong2String(a1: dFun, a2: lFun, a3: sFun, a4: lFun): sFun = sFunDummy
  def helperDoubleLongStringDouble2String(a1: dFun, a2: lFun, a3: sFun, a4: dFun): sFun = sFunDummy
  def helperDoubleLongStringString2String(a1: dFun, a2: lFun, a3: sFun, a4: sFun): sFun = sFunDummy
  def helperDoubleLongStringBoolean2String(a1: dFun, a2: lFun, a3: sFun, a4: bFun): sFun = sFunDummy
  def helperDoubleLongBooleanInt2String(a1: dFun, a2: lFun, a3: bFun, a4: iFun): sFun = sFunDummy
  def helperDoubleLongBooleanLong2String(a1: dFun, a2: lFun, a3: bFun, a4: lFun): sFun = sFunDummy
  def helperDoubleLongBooleanDouble2String(a1: dFun, a2: lFun, a3: bFun, a4: dFun): sFun = sFunDummy
  def helperDoubleLongBooleanString2String(a1: dFun, a2: lFun, a3: bFun, a4: sFun): sFun = sFunDummy
  def helperDoubleLongBooleanBoolean2String(a1: dFun, a2: lFun, a3: bFun, a4: bFun): sFun = sFunDummy
  def helperDoubleDoubleIntInt2String(a1: dFun, a2: dFun, a3: iFun, a4: iFun): sFun = sFunDummy
  def helperDoubleDoubleIntLong2String(a1: dFun, a2: dFun, a3: iFun, a4: lFun): sFun = sFunDummy
  def helperDoubleDoubleIntDouble2String(a1: dFun, a2: dFun, a3: iFun, a4: dFun): sFun = sFunDummy
  def helperDoubleDoubleIntString2String(a1: dFun, a2: dFun, a3: iFun, a4: sFun): sFun = sFunDummy
  def helperDoubleDoubleIntBoolean2String(a1: dFun, a2: dFun, a3: iFun, a4: bFun): sFun = sFunDummy
  def helperDoubleDoubleLongInt2String(a1: dFun, a2: dFun, a3: lFun, a4: iFun): sFun = sFunDummy
  def helperDoubleDoubleLongLong2String(a1: dFun, a2: dFun, a3: lFun, a4: lFun): sFun = sFunDummy
  def helperDoubleDoubleLongDouble2String(a1: dFun, a2: dFun, a3: lFun, a4: dFun): sFun = sFunDummy
  def helperDoubleDoubleLongString2String(a1: dFun, a2: dFun, a3: lFun, a4: sFun): sFun = sFunDummy
  def helperDoubleDoubleLongBoolean2String(a1: dFun, a2: dFun, a3: lFun, a4: bFun): sFun = sFunDummy
  def helperDoubleDoubleDoubleInt2String(a1: dFun, a2: dFun, a3: dFun, a4: iFun): sFun = sFunDummy
  def helperDoubleDoubleDoubleLong2String(a1: dFun, a2: dFun, a3: dFun, a4: lFun): sFun = sFunDummy
  def helperDoubleDoubleDoubleDouble2String(a1: dFun, a2: dFun, a3: dFun, a4: dFun): sFun = sFunDummy
  def helperDoubleDoubleDoubleString2String(a1: dFun, a2: dFun, a3: dFun, a4: sFun): sFun = sFunDummy
  def helperDoubleDoubleDoubleBoolean2String(a1: dFun, a2: dFun, a3: dFun, a4: bFun): sFun = sFunDummy
  def helperDoubleDoubleStringInt2String(a1: dFun, a2: dFun, a3: sFun, a4: iFun): sFun = sFunDummy
  def helperDoubleDoubleStringLong2String(a1: dFun, a2: dFun, a3: sFun, a4: lFun): sFun = sFunDummy
  def helperDoubleDoubleStringDouble2String(a1: dFun, a2: dFun, a3: sFun, a4: dFun): sFun = sFunDummy
  def helperDoubleDoubleStringString2String(a1: dFun, a2: dFun, a3: sFun, a4: sFun): sFun = sFunDummy
  def helperDoubleDoubleStringBoolean2String(a1: dFun, a2: dFun, a3: sFun, a4: bFun): sFun = sFunDummy
  def helperDoubleDoubleBooleanInt2String(a1: dFun, a2: dFun, a3: bFun, a4: iFun): sFun = sFunDummy
  def helperDoubleDoubleBooleanLong2String(a1: dFun, a2: dFun, a3: bFun, a4: lFun): sFun = sFunDummy
  def helperDoubleDoubleBooleanDouble2String(a1: dFun, a2: dFun, a3: bFun, a4: dFun): sFun = sFunDummy
  def helperDoubleDoubleBooleanString2String(a1: dFun, a2: dFun, a3: bFun, a4: sFun): sFun = sFunDummy
  def helperDoubleDoubleBooleanBoolean2String(a1: dFun, a2: dFun, a3: bFun, a4: bFun): sFun = sFunDummy
  def helperDoubleStringIntInt2String(a1: dFun, a2: sFun, a3: iFun, a4: iFun): sFun = sFunDummy
  def helperDoubleStringIntLong2String(a1: dFun, a2: sFun, a3: iFun, a4: lFun): sFun = sFunDummy
  def helperDoubleStringIntDouble2String(a1: dFun, a2: sFun, a3: iFun, a4: dFun): sFun = sFunDummy
  def helperDoubleStringIntString2String(a1: dFun, a2: sFun, a3: iFun, a4: sFun): sFun = sFunDummy
  def helperDoubleStringIntBoolean2String(a1: dFun, a2: sFun, a3: iFun, a4: bFun): sFun = sFunDummy
  def helperDoubleStringLongInt2String(a1: dFun, a2: sFun, a3: lFun, a4: iFun): sFun = sFunDummy
  def helperDoubleStringLongLong2String(a1: dFun, a2: sFun, a3: lFun, a4: lFun): sFun = sFunDummy
  def helperDoubleStringLongDouble2String(a1: dFun, a2: sFun, a3: lFun, a4: dFun): sFun = sFunDummy
  def helperDoubleStringLongString2String(a1: dFun, a2: sFun, a3: lFun, a4: sFun): sFun = sFunDummy
  def helperDoubleStringLongBoolean2String(a1: dFun, a2: sFun, a3: lFun, a4: bFun): sFun = sFunDummy
  def helperDoubleStringDoubleInt2String(a1: dFun, a2: sFun, a3: dFun, a4: iFun): sFun = sFunDummy
  def helperDoubleStringDoubleLong2String(a1: dFun, a2: sFun, a3: dFun, a4: lFun): sFun = sFunDummy
  def helperDoubleStringDoubleDouble2String(a1: dFun, a2: sFun, a3: dFun, a4: dFun): sFun = sFunDummy
  def helperDoubleStringDoubleString2String(a1: dFun, a2: sFun, a3: dFun, a4: sFun): sFun = sFunDummy
  def helperDoubleStringDoubleBoolean2String(a1: dFun, a2: sFun, a3: dFun, a4: bFun): sFun = sFunDummy
  def helperDoubleStringStringInt2String(a1: dFun, a2: sFun, a3: sFun, a4: iFun): sFun = sFunDummy
  def helperDoubleStringStringLong2String(a1: dFun, a2: sFun, a3: sFun, a4: lFun): sFun = sFunDummy
  def helperDoubleStringStringDouble2String(a1: dFun, a2: sFun, a3: sFun, a4: dFun): sFun = sFunDummy
  def helperDoubleStringStringString2String(a1: dFun, a2: sFun, a3: sFun, a4: sFun): sFun = sFunDummy
  def helperDoubleStringStringBoolean2String(a1: dFun, a2: sFun, a3: sFun, a4: bFun): sFun = sFunDummy
  def helperDoubleStringBooleanInt2String(a1: dFun, a2: sFun, a3: bFun, a4: iFun): sFun = sFunDummy
  def helperDoubleStringBooleanLong2String(a1: dFun, a2: sFun, a3: bFun, a4: lFun): sFun = sFunDummy
  def helperDoubleStringBooleanDouble2String(a1: dFun, a2: sFun, a3: bFun, a4: dFun): sFun = sFunDummy
  def helperDoubleStringBooleanString2String(a1: dFun, a2: sFun, a3: bFun, a4: sFun): sFun = sFunDummy
  def helperDoubleStringBooleanBoolean2String(a1: dFun, a2: sFun, a3: bFun, a4: bFun): sFun = sFunDummy
  def helperDoubleBooleanIntInt2String(a1: dFun, a2: bFun, a3: iFun, a4: iFun): sFun = sFunDummy
  def helperDoubleBooleanIntLong2String(a1: dFun, a2: bFun, a3: iFun, a4: lFun): sFun = sFunDummy
  def helperDoubleBooleanIntDouble2String(a1: dFun, a2: bFun, a3: iFun, a4: dFun): sFun = sFunDummy
  def helperDoubleBooleanIntString2String(a1: dFun, a2: bFun, a3: iFun, a4: sFun): sFun = sFunDummy
  def helperDoubleBooleanIntBoolean2String(a1: dFun, a2: bFun, a3: iFun, a4: bFun): sFun = sFunDummy
  def helperDoubleBooleanLongInt2String(a1: dFun, a2: bFun, a3: lFun, a4: iFun): sFun = sFunDummy
  def helperDoubleBooleanLongLong2String(a1: dFun, a2: bFun, a3: lFun, a4: lFun): sFun = sFunDummy
  def helperDoubleBooleanLongDouble2String(a1: dFun, a2: bFun, a3: lFun, a4: dFun): sFun = sFunDummy
  def helperDoubleBooleanLongString2String(a1: dFun, a2: bFun, a3: lFun, a4: sFun): sFun = sFunDummy
  def helperDoubleBooleanLongBoolean2String(a1: dFun, a2: bFun, a3: lFun, a4: bFun): sFun = sFunDummy
  def helperDoubleBooleanDoubleInt2String(a1: dFun, a2: bFun, a3: dFun, a4: iFun): sFun = sFunDummy
  def helperDoubleBooleanDoubleLong2String(a1: dFun, a2: bFun, a3: dFun, a4: lFun): sFun = sFunDummy
  def helperDoubleBooleanDoubleDouble2String(a1: dFun, a2: bFun, a3: dFun, a4: dFun): sFun = sFunDummy
  def helperDoubleBooleanDoubleString2String(a1: dFun, a2: bFun, a3: dFun, a4: sFun): sFun = sFunDummy
  def helperDoubleBooleanDoubleBoolean2String(a1: dFun, a2: bFun, a3: dFun, a4: bFun): sFun = sFunDummy
  def helperDoubleBooleanStringInt2String(a1: dFun, a2: bFun, a3: sFun, a4: iFun): sFun = sFunDummy
  def helperDoubleBooleanStringLong2String(a1: dFun, a2: bFun, a3: sFun, a4: lFun): sFun = sFunDummy
  def helperDoubleBooleanStringDouble2String(a1: dFun, a2: bFun, a3: sFun, a4: dFun): sFun = sFunDummy
  def helperDoubleBooleanStringString2String(a1: dFun, a2: bFun, a3: sFun, a4: sFun): sFun = sFunDummy
  def helperDoubleBooleanStringBoolean2String(a1: dFun, a2: bFun, a3: sFun, a4: bFun): sFun = sFunDummy
  def helperDoubleBooleanBooleanInt2String(a1: dFun, a2: bFun, a3: bFun, a4: iFun): sFun = sFunDummy
  def helperDoubleBooleanBooleanLong2String(a1: dFun, a2: bFun, a3: bFun, a4: lFun): sFun = sFunDummy
  def helperDoubleBooleanBooleanDouble2String(a1: dFun, a2: bFun, a3: bFun, a4: dFun): sFun = sFunDummy
  def helperDoubleBooleanBooleanString2String(a1: dFun, a2: bFun, a3: bFun, a4: sFun): sFun = sFunDummy
  def helperDoubleBooleanBooleanBoolean2String(a1: dFun, a2: bFun, a3: bFun, a4: bFun): sFun = sFunDummy
  def helperStringIntIntInt2String(a1: sFun, a2: iFun, a3: iFun, a4: iFun): sFun = sFunDummy
  def helperStringIntIntLong2String(a1: sFun, a2: iFun, a3: iFun, a4: lFun): sFun = sFunDummy
  def helperStringIntIntDouble2String(a1: sFun, a2: iFun, a3: iFun, a4: dFun): sFun = sFunDummy
  def helperStringIntIntString2String(a1: sFun, a2: iFun, a3: iFun, a4: sFun): sFun = sFunDummy
  def helperStringIntIntBoolean2String(a1: sFun, a2: iFun, a3: iFun, a4: bFun): sFun = sFunDummy
  def helperStringIntLongInt2String(a1: sFun, a2: iFun, a3: lFun, a4: iFun): sFun = sFunDummy
  def helperStringIntLongLong2String(a1: sFun, a2: iFun, a3: lFun, a4: lFun): sFun = sFunDummy
  def helperStringIntLongDouble2String(a1: sFun, a2: iFun, a3: lFun, a4: dFun): sFun = sFunDummy
  def helperStringIntLongString2String(a1: sFun, a2: iFun, a3: lFun, a4: sFun): sFun = sFunDummy
  def helperStringIntLongBoolean2String(a1: sFun, a2: iFun, a3: lFun, a4: bFun): sFun = sFunDummy
  def helperStringIntDoubleInt2String(a1: sFun, a2: iFun, a3: dFun, a4: iFun): sFun = sFunDummy
  def helperStringIntDoubleLong2String(a1: sFun, a2: iFun, a3: dFun, a4: lFun): sFun = sFunDummy
  def helperStringIntDoubleDouble2String(a1: sFun, a2: iFun, a3: dFun, a4: dFun): sFun = sFunDummy
  def helperStringIntDoubleString2String(a1: sFun, a2: iFun, a3: dFun, a4: sFun): sFun = sFunDummy
  def helperStringIntDoubleBoolean2String(a1: sFun, a2: iFun, a3: dFun, a4: bFun): sFun = sFunDummy
  def helperStringIntStringInt2String(a1: sFun, a2: iFun, a3: sFun, a4: iFun): sFun = sFunDummy
  def helperStringIntStringLong2String(a1: sFun, a2: iFun, a3: sFun, a4: lFun): sFun = sFunDummy
  def helperStringIntStringDouble2String(a1: sFun, a2: iFun, a3: sFun, a4: dFun): sFun = sFunDummy
  def helperStringIntStringString2String(a1: sFun, a2: iFun, a3: sFun, a4: sFun): sFun = sFunDummy
  def helperStringIntStringBoolean2String(a1: sFun, a2: iFun, a3: sFun, a4: bFun): sFun = sFunDummy
  def helperStringIntBooleanInt2String(a1: sFun, a2: iFun, a3: bFun, a4: iFun): sFun = sFunDummy
  def helperStringIntBooleanLong2String(a1: sFun, a2: iFun, a3: bFun, a4: lFun): sFun = sFunDummy
  def helperStringIntBooleanDouble2String(a1: sFun, a2: iFun, a3: bFun, a4: dFun): sFun = sFunDummy
  def helperStringIntBooleanString2String(a1: sFun, a2: iFun, a3: bFun, a4: sFun): sFun = sFunDummy
  def helperStringIntBooleanBoolean2String(a1: sFun, a2: iFun, a3: bFun, a4: bFun): sFun = sFunDummy
  def helperStringLongIntInt2String(a1: sFun, a2: lFun, a3: iFun, a4: iFun): sFun = sFunDummy
  def helperStringLongIntLong2String(a1: sFun, a2: lFun, a3: iFun, a4: lFun): sFun = sFunDummy
  def helperStringLongIntDouble2String(a1: sFun, a2: lFun, a3: iFun, a4: dFun): sFun = sFunDummy
  def helperStringLongIntString2String(a1: sFun, a2: lFun, a3: iFun, a4: sFun): sFun = sFunDummy
  def helperStringLongIntBoolean2String(a1: sFun, a2: lFun, a3: iFun, a4: bFun): sFun = sFunDummy
  def helperStringLongLongInt2String(a1: sFun, a2: lFun, a3: lFun, a4: iFun): sFun = sFunDummy
  def helperStringLongLongLong2String(a1: sFun, a2: lFun, a3: lFun, a4: lFun): sFun = sFunDummy
  def helperStringLongLongDouble2String(a1: sFun, a2: lFun, a3: lFun, a4: dFun): sFun = sFunDummy
  def helperStringLongLongString2String(a1: sFun, a2: lFun, a3: lFun, a4: sFun): sFun = sFunDummy
  def helperStringLongLongBoolean2String(a1: sFun, a2: lFun, a3: lFun, a4: bFun): sFun = sFunDummy
  def helperStringLongDoubleInt2String(a1: sFun, a2: lFun, a3: dFun, a4: iFun): sFun = sFunDummy
  def helperStringLongDoubleLong2String(a1: sFun, a2: lFun, a3: dFun, a4: lFun): sFun = sFunDummy
  def helperStringLongDoubleDouble2String(a1: sFun, a2: lFun, a3: dFun, a4: dFun): sFun = sFunDummy
  def helperStringLongDoubleString2String(a1: sFun, a2: lFun, a3: dFun, a4: sFun): sFun = sFunDummy
  def helperStringLongDoubleBoolean2String(a1: sFun, a2: lFun, a3: dFun, a4: bFun): sFun = sFunDummy
  def helperStringLongStringInt2String(a1: sFun, a2: lFun, a3: sFun, a4: iFun): sFun = sFunDummy
  def helperStringLongStringLong2String(a1: sFun, a2: lFun, a3: sFun, a4: lFun): sFun = sFunDummy
  def helperStringLongStringDouble2String(a1: sFun, a2: lFun, a3: sFun, a4: dFun): sFun = sFunDummy
  def helperStringLongStringString2String(a1: sFun, a2: lFun, a3: sFun, a4: sFun): sFun = sFunDummy
  def helperStringLongStringBoolean2String(a1: sFun, a2: lFun, a3: sFun, a4: bFun): sFun = sFunDummy
  def helperStringLongBooleanInt2String(a1: sFun, a2: lFun, a3: bFun, a4: iFun): sFun = sFunDummy
  def helperStringLongBooleanLong2String(a1: sFun, a2: lFun, a3: bFun, a4: lFun): sFun = sFunDummy
  def helperStringLongBooleanDouble2String(a1: sFun, a2: lFun, a3: bFun, a4: dFun): sFun = sFunDummy
  def helperStringLongBooleanString2String(a1: sFun, a2: lFun, a3: bFun, a4: sFun): sFun = sFunDummy
  def helperStringLongBooleanBoolean2String(a1: sFun, a2: lFun, a3: bFun, a4: bFun): sFun = sFunDummy
  def helperStringDoubleIntInt2String(a1: sFun, a2: dFun, a3: iFun, a4: iFun): sFun = sFunDummy
  def helperStringDoubleIntLong2String(a1: sFun, a2: dFun, a3: iFun, a4: lFun): sFun = sFunDummy
  def helperStringDoubleIntDouble2String(a1: sFun, a2: dFun, a3: iFun, a4: dFun): sFun = sFunDummy
  def helperStringDoubleIntString2String(a1: sFun, a2: dFun, a3: iFun, a4: sFun): sFun = sFunDummy
  def helperStringDoubleIntBoolean2String(a1: sFun, a2: dFun, a3: iFun, a4: bFun): sFun = sFunDummy
  def helperStringDoubleLongInt2String(a1: sFun, a2: dFun, a3: lFun, a4: iFun): sFun = sFunDummy
  def helperStringDoubleLongLong2String(a1: sFun, a2: dFun, a3: lFun, a4: lFun): sFun = sFunDummy
  def helperStringDoubleLongDouble2String(a1: sFun, a2: dFun, a3: lFun, a4: dFun): sFun = sFunDummy
  def helperStringDoubleLongString2String(a1: sFun, a2: dFun, a3: lFun, a4: sFun): sFun = sFunDummy
  def helperStringDoubleLongBoolean2String(a1: sFun, a2: dFun, a3: lFun, a4: bFun): sFun = sFunDummy
  def helperStringDoubleDoubleInt2String(a1: sFun, a2: dFun, a3: dFun, a4: iFun): sFun = sFunDummy
  def helperStringDoubleDoubleLong2String(a1: sFun, a2: dFun, a3: dFun, a4: lFun): sFun = sFunDummy
  def helperStringDoubleDoubleDouble2String(a1: sFun, a2: dFun, a3: dFun, a4: dFun): sFun = sFunDummy
  def helperStringDoubleDoubleString2String(a1: sFun, a2: dFun, a3: dFun, a4: sFun): sFun = sFunDummy
  def helperStringDoubleDoubleBoolean2String(a1: sFun, a2: dFun, a3: dFun, a4: bFun): sFun = sFunDummy
  def helperStringDoubleStringInt2String(a1: sFun, a2: dFun, a3: sFun, a4: iFun): sFun = sFunDummy
  def helperStringDoubleStringLong2String(a1: sFun, a2: dFun, a3: sFun, a4: lFun): sFun = sFunDummy
  def helperStringDoubleStringDouble2String(a1: sFun, a2: dFun, a3: sFun, a4: dFun): sFun = sFunDummy
  def helperStringDoubleStringString2String(a1: sFun, a2: dFun, a3: sFun, a4: sFun): sFun = sFunDummy
  def helperStringDoubleStringBoolean2String(a1: sFun, a2: dFun, a3: sFun, a4: bFun): sFun = sFunDummy
  def helperStringDoubleBooleanInt2String(a1: sFun, a2: dFun, a3: bFun, a4: iFun): sFun = sFunDummy
  def helperStringDoubleBooleanLong2String(a1: sFun, a2: dFun, a3: bFun, a4: lFun): sFun = sFunDummy
  def helperStringDoubleBooleanDouble2String(a1: sFun, a2: dFun, a3: bFun, a4: dFun): sFun = sFunDummy
  def helperStringDoubleBooleanString2String(a1: sFun, a2: dFun, a3: bFun, a4: sFun): sFun = sFunDummy
  def helperStringDoubleBooleanBoolean2String(a1: sFun, a2: dFun, a3: bFun, a4: bFun): sFun = sFunDummy
  def helperStringStringIntInt2String(a1: sFun, a2: sFun, a3: iFun, a4: iFun): sFun = sFunDummy
  def helperStringStringIntLong2String(a1: sFun, a2: sFun, a3: iFun, a4: lFun): sFun = sFunDummy
  def helperStringStringIntDouble2String(a1: sFun, a2: sFun, a3: iFun, a4: dFun): sFun = sFunDummy
  def helperStringStringIntString2String(a1: sFun, a2: sFun, a3: iFun, a4: sFun): sFun = sFunDummy
  def helperStringStringIntBoolean2String(a1: sFun, a2: sFun, a3: iFun, a4: bFun): sFun = sFunDummy
  def helperStringStringLongInt2String(a1: sFun, a2: sFun, a3: lFun, a4: iFun): sFun = sFunDummy
  def helperStringStringLongLong2String(a1: sFun, a2: sFun, a3: lFun, a4: lFun): sFun = sFunDummy
  def helperStringStringLongDouble2String(a1: sFun, a2: sFun, a3: lFun, a4: dFun): sFun = sFunDummy
  def helperStringStringLongString2String(a1: sFun, a2: sFun, a3: lFun, a4: sFun): sFun = sFunDummy
  def helperStringStringLongBoolean2String(a1: sFun, a2: sFun, a3: lFun, a4: bFun): sFun = sFunDummy
  def helperStringStringDoubleInt2String(a1: sFun, a2: sFun, a3: dFun, a4: iFun): sFun = sFunDummy
  def helperStringStringDoubleLong2String(a1: sFun, a2: sFun, a3: dFun, a4: lFun): sFun = sFunDummy
  def helperStringStringDoubleDouble2String(a1: sFun, a2: sFun, a3: dFun, a4: dFun): sFun = sFunDummy
  def helperStringStringDoubleString2String(a1: sFun, a2: sFun, a3: dFun, a4: sFun): sFun = sFunDummy
  def helperStringStringDoubleBoolean2String(a1: sFun, a2: sFun, a3: dFun, a4: bFun): sFun = sFunDummy
  def helperStringStringStringInt2String(a1: sFun, a2: sFun, a3: sFun, a4: iFun): sFun = sFunDummy
  def helperStringStringStringLong2String(a1: sFun, a2: sFun, a3: sFun, a4: lFun): sFun = sFunDummy
  def helperStringStringStringDouble2String(a1: sFun, a2: sFun, a3: sFun, a4: dFun): sFun = sFunDummy
  def helperStringStringStringString2String(a1: sFun, a2: sFun, a3: sFun, a4: sFun): sFun = sFunDummy
  def helperStringStringStringBoolean2String(a1: sFun, a2: sFun, a3: sFun, a4: bFun): sFun = sFunDummy
  def helperStringStringBooleanInt2String(a1: sFun, a2: sFun, a3: bFun, a4: iFun): sFun = sFunDummy
  def helperStringStringBooleanLong2String(a1: sFun, a2: sFun, a3: bFun, a4: lFun): sFun = sFunDummy
  def helperStringStringBooleanDouble2String(a1: sFun, a2: sFun, a3: bFun, a4: dFun): sFun = sFunDummy
  def helperStringStringBooleanString2String(a1: sFun, a2: sFun, a3: bFun, a4: sFun): sFun = sFunDummy
  def helperStringStringBooleanBoolean2String(a1: sFun, a2: sFun, a3: bFun, a4: bFun): sFun = sFunDummy
  def helperStringBooleanIntInt2String(a1: sFun, a2: bFun, a3: iFun, a4: iFun): sFun = sFunDummy
  def helperStringBooleanIntLong2String(a1: sFun, a2: bFun, a3: iFun, a4: lFun): sFun = sFunDummy
  def helperStringBooleanIntDouble2String(a1: sFun, a2: bFun, a3: iFun, a4: dFun): sFun = sFunDummy
  def helperStringBooleanIntString2String(a1: sFun, a2: bFun, a3: iFun, a4: sFun): sFun = sFunDummy
  def helperStringBooleanIntBoolean2String(a1: sFun, a2: bFun, a3: iFun, a4: bFun): sFun = sFunDummy
  def helperStringBooleanLongInt2String(a1: sFun, a2: bFun, a3: lFun, a4: iFun): sFun = sFunDummy
  def helperStringBooleanLongLong2String(a1: sFun, a2: bFun, a3: lFun, a4: lFun): sFun = sFunDummy
  def helperStringBooleanLongDouble2String(a1: sFun, a2: bFun, a3: lFun, a4: dFun): sFun = sFunDummy
  def helperStringBooleanLongString2String(a1: sFun, a2: bFun, a3: lFun, a4: sFun): sFun = sFunDummy
  def helperStringBooleanLongBoolean2String(a1: sFun, a2: bFun, a3: lFun, a4: bFun): sFun = sFunDummy
  def helperStringBooleanDoubleInt2String(a1: sFun, a2: bFun, a3: dFun, a4: iFun): sFun = sFunDummy
  def helperStringBooleanDoubleLong2String(a1: sFun, a2: bFun, a3: dFun, a4: lFun): sFun = sFunDummy
  def helperStringBooleanDoubleDouble2String(a1: sFun, a2: bFun, a3: dFun, a4: dFun): sFun = sFunDummy
  def helperStringBooleanDoubleString2String(a1: sFun, a2: bFun, a3: dFun, a4: sFun): sFun = sFunDummy
  def helperStringBooleanDoubleBoolean2String(a1: sFun, a2: bFun, a3: dFun, a4: bFun): sFun = sFunDummy
  def helperStringBooleanStringInt2String(a1: sFun, a2: bFun, a3: sFun, a4: iFun): sFun = sFunDummy
  def helperStringBooleanStringLong2String(a1: sFun, a2: bFun, a3: sFun, a4: lFun): sFun = sFunDummy
  def helperStringBooleanStringDouble2String(a1: sFun, a2: bFun, a3: sFun, a4: dFun): sFun = sFunDummy
  def helperStringBooleanStringString2String(a1: sFun, a2: bFun, a3: sFun, a4: sFun): sFun = sFunDummy
  def helperStringBooleanStringBoolean2String(a1: sFun, a2: bFun, a3: sFun, a4: bFun): sFun = sFunDummy
  def helperStringBooleanBooleanInt2String(a1: sFun, a2: bFun, a3: bFun, a4: iFun): sFun = sFunDummy
  def helperStringBooleanBooleanLong2String(a1: sFun, a2: bFun, a3: bFun, a4: lFun): sFun = sFunDummy
  def helperStringBooleanBooleanDouble2String(a1: sFun, a2: bFun, a3: bFun, a4: dFun): sFun = sFunDummy
  def helperStringBooleanBooleanString2String(a1: sFun, a2: bFun, a3: bFun, a4: sFun): sFun = sFunDummy
  def helperStringBooleanBooleanBoolean2String(a1: sFun, a2: bFun, a3: bFun, a4: bFun): sFun = sFunDummy
  def helperBooleanIntIntInt2String(a1: bFun, a2: iFun, a3: iFun, a4: iFun): sFun = sFunDummy
  def helperBooleanIntIntLong2String(a1: bFun, a2: iFun, a3: iFun, a4: lFun): sFun = sFunDummy
  def helperBooleanIntIntDouble2String(a1: bFun, a2: iFun, a3: iFun, a4: dFun): sFun = sFunDummy
  def helperBooleanIntIntString2String(a1: bFun, a2: iFun, a3: iFun, a4: sFun): sFun = sFunDummy
  def helperBooleanIntIntBoolean2String(a1: bFun, a2: iFun, a3: iFun, a4: bFun): sFun = sFunDummy
  def helperBooleanIntLongInt2String(a1: bFun, a2: iFun, a3: lFun, a4: iFun): sFun = sFunDummy
  def helperBooleanIntLongLong2String(a1: bFun, a2: iFun, a3: lFun, a4: lFun): sFun = sFunDummy
  def helperBooleanIntLongDouble2String(a1: bFun, a2: iFun, a3: lFun, a4: dFun): sFun = sFunDummy
  def helperBooleanIntLongString2String(a1: bFun, a2: iFun, a3: lFun, a4: sFun): sFun = sFunDummy
  def helperBooleanIntLongBoolean2String(a1: bFun, a2: iFun, a3: lFun, a4: bFun): sFun = sFunDummy
  def helperBooleanIntDoubleInt2String(a1: bFun, a2: iFun, a3: dFun, a4: iFun): sFun = sFunDummy
  def helperBooleanIntDoubleLong2String(a1: bFun, a2: iFun, a3: dFun, a4: lFun): sFun = sFunDummy
  def helperBooleanIntDoubleDouble2String(a1: bFun, a2: iFun, a3: dFun, a4: dFun): sFun = sFunDummy
  def helperBooleanIntDoubleString2String(a1: bFun, a2: iFun, a3: dFun, a4: sFun): sFun = sFunDummy
  def helperBooleanIntDoubleBoolean2String(a1: bFun, a2: iFun, a3: dFun, a4: bFun): sFun = sFunDummy
  def helperBooleanIntStringInt2String(a1: bFun, a2: iFun, a3: sFun, a4: iFun): sFun = sFunDummy
  def helperBooleanIntStringLong2String(a1: bFun, a2: iFun, a3: sFun, a4: lFun): sFun = sFunDummy
  def helperBooleanIntStringDouble2String(a1: bFun, a2: iFun, a3: sFun, a4: dFun): sFun = sFunDummy
  def helperBooleanIntStringString2String(a1: bFun, a2: iFun, a3: sFun, a4: sFun): sFun = sFunDummy
  def helperBooleanIntStringBoolean2String(a1: bFun, a2: iFun, a3: sFun, a4: bFun): sFun = sFunDummy
  def helperBooleanIntBooleanInt2String(a1: bFun, a2: iFun, a3: bFun, a4: iFun): sFun = sFunDummy
  def helperBooleanIntBooleanLong2String(a1: bFun, a2: iFun, a3: bFun, a4: lFun): sFun = sFunDummy
  def helperBooleanIntBooleanDouble2String(a1: bFun, a2: iFun, a3: bFun, a4: dFun): sFun = sFunDummy
  def helperBooleanIntBooleanString2String(a1: bFun, a2: iFun, a3: bFun, a4: sFun): sFun = sFunDummy
  def helperBooleanIntBooleanBoolean2String(a1: bFun, a2: iFun, a3: bFun, a4: bFun): sFun = sFunDummy
  def helperBooleanLongIntInt2String(a1: bFun, a2: lFun, a3: iFun, a4: iFun): sFun = sFunDummy
  def helperBooleanLongIntLong2String(a1: bFun, a2: lFun, a3: iFun, a4: lFun): sFun = sFunDummy
  def helperBooleanLongIntDouble2String(a1: bFun, a2: lFun, a3: iFun, a4: dFun): sFun = sFunDummy
  def helperBooleanLongIntString2String(a1: bFun, a2: lFun, a3: iFun, a4: sFun): sFun = sFunDummy
  def helperBooleanLongIntBoolean2String(a1: bFun, a2: lFun, a3: iFun, a4: bFun): sFun = sFunDummy
  def helperBooleanLongLongInt2String(a1: bFun, a2: lFun, a3: lFun, a4: iFun): sFun = sFunDummy
  def helperBooleanLongLongLong2String(a1: bFun, a2: lFun, a3: lFun, a4: lFun): sFun = sFunDummy
  def helperBooleanLongLongDouble2String(a1: bFun, a2: lFun, a3: lFun, a4: dFun): sFun = sFunDummy
  def helperBooleanLongLongString2String(a1: bFun, a2: lFun, a3: lFun, a4: sFun): sFun = sFunDummy
  def helperBooleanLongLongBoolean2String(a1: bFun, a2: lFun, a3: lFun, a4: bFun): sFun = sFunDummy
  def helperBooleanLongDoubleInt2String(a1: bFun, a2: lFun, a3: dFun, a4: iFun): sFun = sFunDummy
  def helperBooleanLongDoubleLong2String(a1: bFun, a2: lFun, a3: dFun, a4: lFun): sFun = sFunDummy
  def helperBooleanLongDoubleDouble2String(a1: bFun, a2: lFun, a3: dFun, a4: dFun): sFun = sFunDummy
  def helperBooleanLongDoubleString2String(a1: bFun, a2: lFun, a3: dFun, a4: sFun): sFun = sFunDummy
  def helperBooleanLongDoubleBoolean2String(a1: bFun, a2: lFun, a3: dFun, a4: bFun): sFun = sFunDummy
  def helperBooleanLongStringInt2String(a1: bFun, a2: lFun, a3: sFun, a4: iFun): sFun = sFunDummy
  def helperBooleanLongStringLong2String(a1: bFun, a2: lFun, a3: sFun, a4: lFun): sFun = sFunDummy
  def helperBooleanLongStringDouble2String(a1: bFun, a2: lFun, a3: sFun, a4: dFun): sFun = sFunDummy
  def helperBooleanLongStringString2String(a1: bFun, a2: lFun, a3: sFun, a4: sFun): sFun = sFunDummy
  def helperBooleanLongStringBoolean2String(a1: bFun, a2: lFun, a3: sFun, a4: bFun): sFun = sFunDummy
  def helperBooleanLongBooleanInt2String(a1: bFun, a2: lFun, a3: bFun, a4: iFun): sFun = sFunDummy
  def helperBooleanLongBooleanLong2String(a1: bFun, a2: lFun, a3: bFun, a4: lFun): sFun = sFunDummy
  def helperBooleanLongBooleanDouble2String(a1: bFun, a2: lFun, a3: bFun, a4: dFun): sFun = sFunDummy
  def helperBooleanLongBooleanString2String(a1: bFun, a2: lFun, a3: bFun, a4: sFun): sFun = sFunDummy
  def helperBooleanLongBooleanBoolean2String(a1: bFun, a2: lFun, a3: bFun, a4: bFun): sFun = sFunDummy
  def helperBooleanDoubleIntInt2String(a1: bFun, a2: dFun, a3: iFun, a4: iFun): sFun = sFunDummy
  def helperBooleanDoubleIntLong2String(a1: bFun, a2: dFun, a3: iFun, a4: lFun): sFun = sFunDummy
  def helperBooleanDoubleIntDouble2String(a1: bFun, a2: dFun, a3: iFun, a4: dFun): sFun = sFunDummy
  def helperBooleanDoubleIntString2String(a1: bFun, a2: dFun, a3: iFun, a4: sFun): sFun = sFunDummy
  def helperBooleanDoubleIntBoolean2String(a1: bFun, a2: dFun, a3: iFun, a4: bFun): sFun = sFunDummy
  def helperBooleanDoubleLongInt2String(a1: bFun, a2: dFun, a3: lFun, a4: iFun): sFun = sFunDummy
  def helperBooleanDoubleLongLong2String(a1: bFun, a2: dFun, a3: lFun, a4: lFun): sFun = sFunDummy
  def helperBooleanDoubleLongDouble2String(a1: bFun, a2: dFun, a3: lFun, a4: dFun): sFun = sFunDummy
  def helperBooleanDoubleLongString2String(a1: bFun, a2: dFun, a3: lFun, a4: sFun): sFun = sFunDummy
  def helperBooleanDoubleLongBoolean2String(a1: bFun, a2: dFun, a3: lFun, a4: bFun): sFun = sFunDummy
  def helperBooleanDoubleDoubleInt2String(a1: bFun, a2: dFun, a3: dFun, a4: iFun): sFun = sFunDummy
  def helperBooleanDoubleDoubleLong2String(a1: bFun, a2: dFun, a3: dFun, a4: lFun): sFun = sFunDummy
  def helperBooleanDoubleDoubleDouble2String(a1: bFun, a2: dFun, a3: dFun, a4: dFun): sFun = sFunDummy
  def helperBooleanDoubleDoubleString2String(a1: bFun, a2: dFun, a3: dFun, a4: sFun): sFun = sFunDummy
  def helperBooleanDoubleDoubleBoolean2String(a1: bFun, a2: dFun, a3: dFun, a4: bFun): sFun = sFunDummy
  def helperBooleanDoubleStringInt2String(a1: bFun, a2: dFun, a3: sFun, a4: iFun): sFun = sFunDummy
  def helperBooleanDoubleStringLong2String(a1: bFun, a2: dFun, a3: sFun, a4: lFun): sFun = sFunDummy
  def helperBooleanDoubleStringDouble2String(a1: bFun, a2: dFun, a3: sFun, a4: dFun): sFun = sFunDummy
  def helperBooleanDoubleStringString2String(a1: bFun, a2: dFun, a3: sFun, a4: sFun): sFun = sFunDummy
  def helperBooleanDoubleStringBoolean2String(a1: bFun, a2: dFun, a3: sFun, a4: bFun): sFun = sFunDummy
  def helperBooleanDoubleBooleanInt2String(a1: bFun, a2: dFun, a3: bFun, a4: iFun): sFun = sFunDummy
  def helperBooleanDoubleBooleanLong2String(a1: bFun, a2: dFun, a3: bFun, a4: lFun): sFun = sFunDummy
  def helperBooleanDoubleBooleanDouble2String(a1: bFun, a2: dFun, a3: bFun, a4: dFun): sFun = sFunDummy
  def helperBooleanDoubleBooleanString2String(a1: bFun, a2: dFun, a3: bFun, a4: sFun): sFun = sFunDummy
  def helperBooleanDoubleBooleanBoolean2String(a1: bFun, a2: dFun, a3: bFun, a4: bFun): sFun = sFunDummy
  def helperBooleanStringIntInt2String(a1: bFun, a2: sFun, a3: iFun, a4: iFun): sFun = sFunDummy
  def helperBooleanStringIntLong2String(a1: bFun, a2: sFun, a3: iFun, a4: lFun): sFun = sFunDummy
  def helperBooleanStringIntDouble2String(a1: bFun, a2: sFun, a3: iFun, a4: dFun): sFun = sFunDummy
  def helperBooleanStringIntString2String(a1: bFun, a2: sFun, a3: iFun, a4: sFun): sFun = sFunDummy
  def helperBooleanStringIntBoolean2String(a1: bFun, a2: sFun, a3: iFun, a4: bFun): sFun = sFunDummy
  def helperBooleanStringLongInt2String(a1: bFun, a2: sFun, a3: lFun, a4: iFun): sFun = sFunDummy
  def helperBooleanStringLongLong2String(a1: bFun, a2: sFun, a3: lFun, a4: lFun): sFun = sFunDummy
  def helperBooleanStringLongDouble2String(a1: bFun, a2: sFun, a3: lFun, a4: dFun): sFun = sFunDummy
  def helperBooleanStringLongString2String(a1: bFun, a2: sFun, a3: lFun, a4: sFun): sFun = sFunDummy
  def helperBooleanStringLongBoolean2String(a1: bFun, a2: sFun, a3: lFun, a4: bFun): sFun = sFunDummy
  def helperBooleanStringDoubleInt2String(a1: bFun, a2: sFun, a3: dFun, a4: iFun): sFun = sFunDummy
  def helperBooleanStringDoubleLong2String(a1: bFun, a2: sFun, a3: dFun, a4: lFun): sFun = sFunDummy
  def helperBooleanStringDoubleDouble2String(a1: bFun, a2: sFun, a3: dFun, a4: dFun): sFun = sFunDummy
  def helperBooleanStringDoubleString2String(a1: bFun, a2: sFun, a3: dFun, a4: sFun): sFun = sFunDummy
  def helperBooleanStringDoubleBoolean2String(a1: bFun, a2: sFun, a3: dFun, a4: bFun): sFun = sFunDummy
  def helperBooleanStringStringInt2String(a1: bFun, a2: sFun, a3: sFun, a4: iFun): sFun = sFunDummy
  def helperBooleanStringStringLong2String(a1: bFun, a2: sFun, a3: sFun, a4: lFun): sFun = sFunDummy
  def helperBooleanStringStringDouble2String(a1: bFun, a2: sFun, a3: sFun, a4: dFun): sFun = sFunDummy
  def helperBooleanStringStringString2String(a1: bFun, a2: sFun, a3: sFun, a4: sFun): sFun = sFunDummy
  def helperBooleanStringStringBoolean2String(a1: bFun, a2: sFun, a3: sFun, a4: bFun): sFun = sFunDummy
  def helperBooleanStringBooleanInt2String(a1: bFun, a2: sFun, a3: bFun, a4: iFun): sFun = sFunDummy
  def helperBooleanStringBooleanLong2String(a1: bFun, a2: sFun, a3: bFun, a4: lFun): sFun = sFunDummy
  def helperBooleanStringBooleanDouble2String(a1: bFun, a2: sFun, a3: bFun, a4: dFun): sFun = sFunDummy
  def helperBooleanStringBooleanString2String(a1: bFun, a2: sFun, a3: bFun, a4: sFun): sFun = sFunDummy
  def helperBooleanStringBooleanBoolean2String(a1: bFun, a2: sFun, a3: bFun, a4: bFun): sFun = sFunDummy
  def helperBooleanBooleanIntInt2String(a1: bFun, a2: bFun, a3: iFun, a4: iFun): sFun = sFunDummy
  def helperBooleanBooleanIntLong2String(a1: bFun, a2: bFun, a3: iFun, a4: lFun): sFun = sFunDummy
  def helperBooleanBooleanIntDouble2String(a1: bFun, a2: bFun, a3: iFun, a4: dFun): sFun = sFunDummy
  def helperBooleanBooleanIntString2String(a1: bFun, a2: bFun, a3: iFun, a4: sFun): sFun = sFunDummy
  def helperBooleanBooleanIntBoolean2String(a1: bFun, a2: bFun, a3: iFun, a4: bFun): sFun = sFunDummy
  def helperBooleanBooleanLongInt2String(a1: bFun, a2: bFun, a3: lFun, a4: iFun): sFun = sFunDummy
  def helperBooleanBooleanLongLong2String(a1: bFun, a2: bFun, a3: lFun, a4: lFun): sFun = sFunDummy
  def helperBooleanBooleanLongDouble2String(a1: bFun, a2: bFun, a3: lFun, a4: dFun): sFun = sFunDummy
  def helperBooleanBooleanLongString2String(a1: bFun, a2: bFun, a3: lFun, a4: sFun): sFun = sFunDummy
  def helperBooleanBooleanLongBoolean2String(a1: bFun, a2: bFun, a3: lFun, a4: bFun): sFun = sFunDummy
  def helperBooleanBooleanDoubleInt2String(a1: bFun, a2: bFun, a3: dFun, a4: iFun): sFun = sFunDummy
  def helperBooleanBooleanDoubleLong2String(a1: bFun, a2: bFun, a3: dFun, a4: lFun): sFun = sFunDummy
  def helperBooleanBooleanDoubleDouble2String(a1: bFun, a2: bFun, a3: dFun, a4: dFun): sFun = sFunDummy
  def helperBooleanBooleanDoubleString2String(a1: bFun, a2: bFun, a3: dFun, a4: sFun): sFun = sFunDummy
  def helperBooleanBooleanDoubleBoolean2String(a1: bFun, a2: bFun, a3: dFun, a4: bFun): sFun = sFunDummy
  def helperBooleanBooleanStringInt2String(a1: bFun, a2: bFun, a3: sFun, a4: iFun): sFun = sFunDummy
  def helperBooleanBooleanStringLong2String(a1: bFun, a2: bFun, a3: sFun, a4: lFun): sFun = sFunDummy
  def helperBooleanBooleanStringDouble2String(a1: bFun, a2: bFun, a3: sFun, a4: dFun): sFun = sFunDummy
  def helperBooleanBooleanStringString2String(a1: bFun, a2: bFun, a3: sFun, a4: sFun): sFun = sFunDummy
  def helperBooleanBooleanStringBoolean2String(a1: bFun, a2: bFun, a3: sFun, a4: bFun): sFun = sFunDummy
  def helperBooleanBooleanBooleanInt2String(a1: bFun, a2: bFun, a3: bFun, a4: iFun): sFun = sFunDummy
  def helperBooleanBooleanBooleanLong2String(a1: bFun, a2: bFun, a3: bFun, a4: lFun): sFun = sFunDummy
  def helperBooleanBooleanBooleanDouble2String(a1: bFun, a2: bFun, a3: bFun, a4: dFun): sFun = sFunDummy
  def helperBooleanBooleanBooleanString2String(a1: bFun, a2: bFun, a3: bFun, a4: sFun): sFun = sFunDummy
  def helperBooleanBooleanBooleanBoolean2String(a1: bFun, a2: bFun, a3: bFun, a4: bFun): sFun = sFunDummy
  test("getSignature_sFun_5") {
    var result = true
    result &= FunctionSignature.getSignatureIntIntIntInt2String(helperIntIntIntInt2String) == "Int:Int:Int:Int2String"
    result &= FunctionSignature.getSignatureIntIntIntLong2String(helperIntIntIntLong2String) == "Int:Int:Int:Long2String"
    result &= FunctionSignature.getSignatureIntIntIntDouble2String(helperIntIntIntDouble2String) == "Int:Int:Int:Double2String"
    result &= FunctionSignature.getSignatureIntIntIntString2String(helperIntIntIntString2String) == "Int:Int:Int:String2String"
    result &= FunctionSignature.getSignatureIntIntIntBoolean2String(helperIntIntIntBoolean2String) == "Int:Int:Int:Boolean2String"
    result &= FunctionSignature.getSignatureIntIntLongInt2String(helperIntIntLongInt2String) == "Int:Int:Long:Int2String"
    result &= FunctionSignature.getSignatureIntIntLongLong2String(helperIntIntLongLong2String) == "Int:Int:Long:Long2String"
    result &= FunctionSignature.getSignatureIntIntLongDouble2String(helperIntIntLongDouble2String) == "Int:Int:Long:Double2String"
    result &= FunctionSignature.getSignatureIntIntLongString2String(helperIntIntLongString2String) == "Int:Int:Long:String2String"
    result &= FunctionSignature.getSignatureIntIntLongBoolean2String(helperIntIntLongBoolean2String) == "Int:Int:Long:Boolean2String"
    result &= FunctionSignature.getSignatureIntIntDoubleInt2String(helperIntIntDoubleInt2String) == "Int:Int:Double:Int2String"
    result &= FunctionSignature.getSignatureIntIntDoubleLong2String(helperIntIntDoubleLong2String) == "Int:Int:Double:Long2String"
    result &= FunctionSignature.getSignatureIntIntDoubleDouble2String(helperIntIntDoubleDouble2String) == "Int:Int:Double:Double2String"
    result &= FunctionSignature.getSignatureIntIntDoubleString2String(helperIntIntDoubleString2String) == "Int:Int:Double:String2String"
    result &= FunctionSignature.getSignatureIntIntDoubleBoolean2String(helperIntIntDoubleBoolean2String) == "Int:Int:Double:Boolean2String"
    result &= FunctionSignature.getSignatureIntIntStringInt2String(helperIntIntStringInt2String) == "Int:Int:String:Int2String"
    result &= FunctionSignature.getSignatureIntIntStringLong2String(helperIntIntStringLong2String) == "Int:Int:String:Long2String"
    result &= FunctionSignature.getSignatureIntIntStringDouble2String(helperIntIntStringDouble2String) == "Int:Int:String:Double2String"
    result &= FunctionSignature.getSignatureIntIntStringString2String(helperIntIntStringString2String) == "Int:Int:String:String2String"
    result &= FunctionSignature.getSignatureIntIntStringBoolean2String(helperIntIntStringBoolean2String) == "Int:Int:String:Boolean2String"
    result &= FunctionSignature.getSignatureIntIntBooleanInt2String(helperIntIntBooleanInt2String) == "Int:Int:Boolean:Int2String"
    result &= FunctionSignature.getSignatureIntIntBooleanLong2String(helperIntIntBooleanLong2String) == "Int:Int:Boolean:Long2String"
    result &= FunctionSignature.getSignatureIntIntBooleanDouble2String(helperIntIntBooleanDouble2String) == "Int:Int:Boolean:Double2String"
    result &= FunctionSignature.getSignatureIntIntBooleanString2String(helperIntIntBooleanString2String) == "Int:Int:Boolean:String2String"
    result &= FunctionSignature.getSignatureIntIntBooleanBoolean2String(helperIntIntBooleanBoolean2String) == "Int:Int:Boolean:Boolean2String"
    result &= FunctionSignature.getSignatureIntLongIntInt2String(helperIntLongIntInt2String) == "Int:Long:Int:Int2String"
    result &= FunctionSignature.getSignatureIntLongIntLong2String(helperIntLongIntLong2String) == "Int:Long:Int:Long2String"
    result &= FunctionSignature.getSignatureIntLongIntDouble2String(helperIntLongIntDouble2String) == "Int:Long:Int:Double2String"
    result &= FunctionSignature.getSignatureIntLongIntString2String(helperIntLongIntString2String) == "Int:Long:Int:String2String"
    result &= FunctionSignature.getSignatureIntLongIntBoolean2String(helperIntLongIntBoolean2String) == "Int:Long:Int:Boolean2String"
    result &= FunctionSignature.getSignatureIntLongLongInt2String(helperIntLongLongInt2String) == "Int:Long:Long:Int2String"
    result &= FunctionSignature.getSignatureIntLongLongLong2String(helperIntLongLongLong2String) == "Int:Long:Long:Long2String"
    result &= FunctionSignature.getSignatureIntLongLongDouble2String(helperIntLongLongDouble2String) == "Int:Long:Long:Double2String"
    result &= FunctionSignature.getSignatureIntLongLongString2String(helperIntLongLongString2String) == "Int:Long:Long:String2String"
    result &= FunctionSignature.getSignatureIntLongLongBoolean2String(helperIntLongLongBoolean2String) == "Int:Long:Long:Boolean2String"
    result &= FunctionSignature.getSignatureIntLongDoubleInt2String(helperIntLongDoubleInt2String) == "Int:Long:Double:Int2String"
    result &= FunctionSignature.getSignatureIntLongDoubleLong2String(helperIntLongDoubleLong2String) == "Int:Long:Double:Long2String"
    result &= FunctionSignature.getSignatureIntLongDoubleDouble2String(helperIntLongDoubleDouble2String) == "Int:Long:Double:Double2String"
    result &= FunctionSignature.getSignatureIntLongDoubleString2String(helperIntLongDoubleString2String) == "Int:Long:Double:String2String"
    result &= FunctionSignature.getSignatureIntLongDoubleBoolean2String(helperIntLongDoubleBoolean2String) == "Int:Long:Double:Boolean2String"
    result &= FunctionSignature.getSignatureIntLongStringInt2String(helperIntLongStringInt2String) == "Int:Long:String:Int2String"
    result &= FunctionSignature.getSignatureIntLongStringLong2String(helperIntLongStringLong2String) == "Int:Long:String:Long2String"
    result &= FunctionSignature.getSignatureIntLongStringDouble2String(helperIntLongStringDouble2String) == "Int:Long:String:Double2String"
    result &= FunctionSignature.getSignatureIntLongStringString2String(helperIntLongStringString2String) == "Int:Long:String:String2String"
    result &= FunctionSignature.getSignatureIntLongStringBoolean2String(helperIntLongStringBoolean2String) == "Int:Long:String:Boolean2String"
    result &= FunctionSignature.getSignatureIntLongBooleanInt2String(helperIntLongBooleanInt2String) == "Int:Long:Boolean:Int2String"
    result &= FunctionSignature.getSignatureIntLongBooleanLong2String(helperIntLongBooleanLong2String) == "Int:Long:Boolean:Long2String"
    result &= FunctionSignature.getSignatureIntLongBooleanDouble2String(helperIntLongBooleanDouble2String) == "Int:Long:Boolean:Double2String"
    result &= FunctionSignature.getSignatureIntLongBooleanString2String(helperIntLongBooleanString2String) == "Int:Long:Boolean:String2String"
    result &= FunctionSignature.getSignatureIntLongBooleanBoolean2String(helperIntLongBooleanBoolean2String) == "Int:Long:Boolean:Boolean2String"
    result &= FunctionSignature.getSignatureIntDoubleIntInt2String(helperIntDoubleIntInt2String) == "Int:Double:Int:Int2String"
    result &= FunctionSignature.getSignatureIntDoubleIntLong2String(helperIntDoubleIntLong2String) == "Int:Double:Int:Long2String"
    result &= FunctionSignature.getSignatureIntDoubleIntDouble2String(helperIntDoubleIntDouble2String) == "Int:Double:Int:Double2String"
    result &= FunctionSignature.getSignatureIntDoubleIntString2String(helperIntDoubleIntString2String) == "Int:Double:Int:String2String"
    result &= FunctionSignature.getSignatureIntDoubleIntBoolean2String(helperIntDoubleIntBoolean2String) == "Int:Double:Int:Boolean2String"
    result &= FunctionSignature.getSignatureIntDoubleLongInt2String(helperIntDoubleLongInt2String) == "Int:Double:Long:Int2String"
    result &= FunctionSignature.getSignatureIntDoubleLongLong2String(helperIntDoubleLongLong2String) == "Int:Double:Long:Long2String"
    result &= FunctionSignature.getSignatureIntDoubleLongDouble2String(helperIntDoubleLongDouble2String) == "Int:Double:Long:Double2String"
    result &= FunctionSignature.getSignatureIntDoubleLongString2String(helperIntDoubleLongString2String) == "Int:Double:Long:String2String"
    result &= FunctionSignature.getSignatureIntDoubleLongBoolean2String(helperIntDoubleLongBoolean2String) == "Int:Double:Long:Boolean2String"
    result &= FunctionSignature.getSignatureIntDoubleDoubleInt2String(helperIntDoubleDoubleInt2String) == "Int:Double:Double:Int2String"
    result &= FunctionSignature.getSignatureIntDoubleDoubleLong2String(helperIntDoubleDoubleLong2String) == "Int:Double:Double:Long2String"
    result &= FunctionSignature.getSignatureIntDoubleDoubleDouble2String(helperIntDoubleDoubleDouble2String) == "Int:Double:Double:Double2String"
    result &= FunctionSignature.getSignatureIntDoubleDoubleString2String(helperIntDoubleDoubleString2String) == "Int:Double:Double:String2String"
    result &= FunctionSignature.getSignatureIntDoubleDoubleBoolean2String(helperIntDoubleDoubleBoolean2String) == "Int:Double:Double:Boolean2String"
    result &= FunctionSignature.getSignatureIntDoubleStringInt2String(helperIntDoubleStringInt2String) == "Int:Double:String:Int2String"
    result &= FunctionSignature.getSignatureIntDoubleStringLong2String(helperIntDoubleStringLong2String) == "Int:Double:String:Long2String"
    result &= FunctionSignature.getSignatureIntDoubleStringDouble2String(helperIntDoubleStringDouble2String) == "Int:Double:String:Double2String"
    result &= FunctionSignature.getSignatureIntDoubleStringString2String(helperIntDoubleStringString2String) == "Int:Double:String:String2String"
    result &= FunctionSignature.getSignatureIntDoubleStringBoolean2String(helperIntDoubleStringBoolean2String) == "Int:Double:String:Boolean2String"
    result &= FunctionSignature.getSignatureIntDoubleBooleanInt2String(helperIntDoubleBooleanInt2String) == "Int:Double:Boolean:Int2String"
    result &= FunctionSignature.getSignatureIntDoubleBooleanLong2String(helperIntDoubleBooleanLong2String) == "Int:Double:Boolean:Long2String"
    result &= FunctionSignature.getSignatureIntDoubleBooleanDouble2String(helperIntDoubleBooleanDouble2String) == "Int:Double:Boolean:Double2String"
    result &= FunctionSignature.getSignatureIntDoubleBooleanString2String(helperIntDoubleBooleanString2String) == "Int:Double:Boolean:String2String"
    result &= FunctionSignature.getSignatureIntDoubleBooleanBoolean2String(helperIntDoubleBooleanBoolean2String) == "Int:Double:Boolean:Boolean2String"
    result &= FunctionSignature.getSignatureIntStringIntInt2String(helperIntStringIntInt2String) == "Int:String:Int:Int2String"
    result &= FunctionSignature.getSignatureIntStringIntLong2String(helperIntStringIntLong2String) == "Int:String:Int:Long2String"
    result &= FunctionSignature.getSignatureIntStringIntDouble2String(helperIntStringIntDouble2String) == "Int:String:Int:Double2String"
    result &= FunctionSignature.getSignatureIntStringIntString2String(helperIntStringIntString2String) == "Int:String:Int:String2String"
    result &= FunctionSignature.getSignatureIntStringIntBoolean2String(helperIntStringIntBoolean2String) == "Int:String:Int:Boolean2String"
    result &= FunctionSignature.getSignatureIntStringLongInt2String(helperIntStringLongInt2String) == "Int:String:Long:Int2String"
    result &= FunctionSignature.getSignatureIntStringLongLong2String(helperIntStringLongLong2String) == "Int:String:Long:Long2String"
    result &= FunctionSignature.getSignatureIntStringLongDouble2String(helperIntStringLongDouble2String) == "Int:String:Long:Double2String"
    result &= FunctionSignature.getSignatureIntStringLongString2String(helperIntStringLongString2String) == "Int:String:Long:String2String"
    result &= FunctionSignature.getSignatureIntStringLongBoolean2String(helperIntStringLongBoolean2String) == "Int:String:Long:Boolean2String"
    result &= FunctionSignature.getSignatureIntStringDoubleInt2String(helperIntStringDoubleInt2String) == "Int:String:Double:Int2String"
    result &= FunctionSignature.getSignatureIntStringDoubleLong2String(helperIntStringDoubleLong2String) == "Int:String:Double:Long2String"
    result &= FunctionSignature.getSignatureIntStringDoubleDouble2String(helperIntStringDoubleDouble2String) == "Int:String:Double:Double2String"
    result &= FunctionSignature.getSignatureIntStringDoubleString2String(helperIntStringDoubleString2String) == "Int:String:Double:String2String"
    result &= FunctionSignature.getSignatureIntStringDoubleBoolean2String(helperIntStringDoubleBoolean2String) == "Int:String:Double:Boolean2String"
    result &= FunctionSignature.getSignatureIntStringStringInt2String(helperIntStringStringInt2String) == "Int:String:String:Int2String"
    result &= FunctionSignature.getSignatureIntStringStringLong2String(helperIntStringStringLong2String) == "Int:String:String:Long2String"
    result &= FunctionSignature.getSignatureIntStringStringDouble2String(helperIntStringStringDouble2String) == "Int:String:String:Double2String"
    result &= FunctionSignature.getSignatureIntStringStringString2String(helperIntStringStringString2String) == "Int:String:String:String2String"
    result &= FunctionSignature.getSignatureIntStringStringBoolean2String(helperIntStringStringBoolean2String) == "Int:String:String:Boolean2String"
    result &= FunctionSignature.getSignatureIntStringBooleanInt2String(helperIntStringBooleanInt2String) == "Int:String:Boolean:Int2String"
    result &= FunctionSignature.getSignatureIntStringBooleanLong2String(helperIntStringBooleanLong2String) == "Int:String:Boolean:Long2String"
    result &= FunctionSignature.getSignatureIntStringBooleanDouble2String(helperIntStringBooleanDouble2String) == "Int:String:Boolean:Double2String"
    result &= FunctionSignature.getSignatureIntStringBooleanString2String(helperIntStringBooleanString2String) == "Int:String:Boolean:String2String"
    result &= FunctionSignature.getSignatureIntStringBooleanBoolean2String(helperIntStringBooleanBoolean2String) == "Int:String:Boolean:Boolean2String"
    result &= FunctionSignature.getSignatureIntBooleanIntInt2String(helperIntBooleanIntInt2String) == "Int:Boolean:Int:Int2String"
    result &= FunctionSignature.getSignatureIntBooleanIntLong2String(helperIntBooleanIntLong2String) == "Int:Boolean:Int:Long2String"
    result &= FunctionSignature.getSignatureIntBooleanIntDouble2String(helperIntBooleanIntDouble2String) == "Int:Boolean:Int:Double2String"
    result &= FunctionSignature.getSignatureIntBooleanIntString2String(helperIntBooleanIntString2String) == "Int:Boolean:Int:String2String"
    result &= FunctionSignature.getSignatureIntBooleanIntBoolean2String(helperIntBooleanIntBoolean2String) == "Int:Boolean:Int:Boolean2String"
    result &= FunctionSignature.getSignatureIntBooleanLongInt2String(helperIntBooleanLongInt2String) == "Int:Boolean:Long:Int2String"
    result &= FunctionSignature.getSignatureIntBooleanLongLong2String(helperIntBooleanLongLong2String) == "Int:Boolean:Long:Long2String"
    result &= FunctionSignature.getSignatureIntBooleanLongDouble2String(helperIntBooleanLongDouble2String) == "Int:Boolean:Long:Double2String"
    result &= FunctionSignature.getSignatureIntBooleanLongString2String(helperIntBooleanLongString2String) == "Int:Boolean:Long:String2String"
    result &= FunctionSignature.getSignatureIntBooleanLongBoolean2String(helperIntBooleanLongBoolean2String) == "Int:Boolean:Long:Boolean2String"
    result &= FunctionSignature.getSignatureIntBooleanDoubleInt2String(helperIntBooleanDoubleInt2String) == "Int:Boolean:Double:Int2String"
    result &= FunctionSignature.getSignatureIntBooleanDoubleLong2String(helperIntBooleanDoubleLong2String) == "Int:Boolean:Double:Long2String"
    result &= FunctionSignature.getSignatureIntBooleanDoubleDouble2String(helperIntBooleanDoubleDouble2String) == "Int:Boolean:Double:Double2String"
    result &= FunctionSignature.getSignatureIntBooleanDoubleString2String(helperIntBooleanDoubleString2String) == "Int:Boolean:Double:String2String"
    result &= FunctionSignature.getSignatureIntBooleanDoubleBoolean2String(helperIntBooleanDoubleBoolean2String) == "Int:Boolean:Double:Boolean2String"
    result &= FunctionSignature.getSignatureIntBooleanStringInt2String(helperIntBooleanStringInt2String) == "Int:Boolean:String:Int2String"
    result &= FunctionSignature.getSignatureIntBooleanStringLong2String(helperIntBooleanStringLong2String) == "Int:Boolean:String:Long2String"
    result &= FunctionSignature.getSignatureIntBooleanStringDouble2String(helperIntBooleanStringDouble2String) == "Int:Boolean:String:Double2String"
    result &= FunctionSignature.getSignatureIntBooleanStringString2String(helperIntBooleanStringString2String) == "Int:Boolean:String:String2String"
    result &= FunctionSignature.getSignatureIntBooleanStringBoolean2String(helperIntBooleanStringBoolean2String) == "Int:Boolean:String:Boolean2String"
    result &= FunctionSignature.getSignatureIntBooleanBooleanInt2String(helperIntBooleanBooleanInt2String) == "Int:Boolean:Boolean:Int2String"
    result &= FunctionSignature.getSignatureIntBooleanBooleanLong2String(helperIntBooleanBooleanLong2String) == "Int:Boolean:Boolean:Long2String"
    result &= FunctionSignature.getSignatureIntBooleanBooleanDouble2String(helperIntBooleanBooleanDouble2String) == "Int:Boolean:Boolean:Double2String"
    result &= FunctionSignature.getSignatureIntBooleanBooleanString2String(helperIntBooleanBooleanString2String) == "Int:Boolean:Boolean:String2String"
    result &= FunctionSignature.getSignatureIntBooleanBooleanBoolean2String(helperIntBooleanBooleanBoolean2String) == "Int:Boolean:Boolean:Boolean2String"
    result &= FunctionSignature.getSignatureLongIntIntInt2String(helperLongIntIntInt2String) == "Long:Int:Int:Int2String"
    result &= FunctionSignature.getSignatureLongIntIntLong2String(helperLongIntIntLong2String) == "Long:Int:Int:Long2String"
    result &= FunctionSignature.getSignatureLongIntIntDouble2String(helperLongIntIntDouble2String) == "Long:Int:Int:Double2String"
    result &= FunctionSignature.getSignatureLongIntIntString2String(helperLongIntIntString2String) == "Long:Int:Int:String2String"
    result &= FunctionSignature.getSignatureLongIntIntBoolean2String(helperLongIntIntBoolean2String) == "Long:Int:Int:Boolean2String"
    result &= FunctionSignature.getSignatureLongIntLongInt2String(helperLongIntLongInt2String) == "Long:Int:Long:Int2String"
    result &= FunctionSignature.getSignatureLongIntLongLong2String(helperLongIntLongLong2String) == "Long:Int:Long:Long2String"
    result &= FunctionSignature.getSignatureLongIntLongDouble2String(helperLongIntLongDouble2String) == "Long:Int:Long:Double2String"
    result &= FunctionSignature.getSignatureLongIntLongString2String(helperLongIntLongString2String) == "Long:Int:Long:String2String"
    result &= FunctionSignature.getSignatureLongIntLongBoolean2String(helperLongIntLongBoolean2String) == "Long:Int:Long:Boolean2String"
    result &= FunctionSignature.getSignatureLongIntDoubleInt2String(helperLongIntDoubleInt2String) == "Long:Int:Double:Int2String"
    result &= FunctionSignature.getSignatureLongIntDoubleLong2String(helperLongIntDoubleLong2String) == "Long:Int:Double:Long2String"
    result &= FunctionSignature.getSignatureLongIntDoubleDouble2String(helperLongIntDoubleDouble2String) == "Long:Int:Double:Double2String"
    result &= FunctionSignature.getSignatureLongIntDoubleString2String(helperLongIntDoubleString2String) == "Long:Int:Double:String2String"
    result &= FunctionSignature.getSignatureLongIntDoubleBoolean2String(helperLongIntDoubleBoolean2String) == "Long:Int:Double:Boolean2String"
    result &= FunctionSignature.getSignatureLongIntStringInt2String(helperLongIntStringInt2String) == "Long:Int:String:Int2String"
    result &= FunctionSignature.getSignatureLongIntStringLong2String(helperLongIntStringLong2String) == "Long:Int:String:Long2String"
    result &= FunctionSignature.getSignatureLongIntStringDouble2String(helperLongIntStringDouble2String) == "Long:Int:String:Double2String"
    result &= FunctionSignature.getSignatureLongIntStringString2String(helperLongIntStringString2String) == "Long:Int:String:String2String"
    result &= FunctionSignature.getSignatureLongIntStringBoolean2String(helperLongIntStringBoolean2String) == "Long:Int:String:Boolean2String"
    result &= FunctionSignature.getSignatureLongIntBooleanInt2String(helperLongIntBooleanInt2String) == "Long:Int:Boolean:Int2String"
    result &= FunctionSignature.getSignatureLongIntBooleanLong2String(helperLongIntBooleanLong2String) == "Long:Int:Boolean:Long2String"
    result &= FunctionSignature.getSignatureLongIntBooleanDouble2String(helperLongIntBooleanDouble2String) == "Long:Int:Boolean:Double2String"
    result &= FunctionSignature.getSignatureLongIntBooleanString2String(helperLongIntBooleanString2String) == "Long:Int:Boolean:String2String"
    result &= FunctionSignature.getSignatureLongIntBooleanBoolean2String(helperLongIntBooleanBoolean2String) == "Long:Int:Boolean:Boolean2String"
    result &= FunctionSignature.getSignatureLongLongIntInt2String(helperLongLongIntInt2String) == "Long:Long:Int:Int2String"
    result &= FunctionSignature.getSignatureLongLongIntLong2String(helperLongLongIntLong2String) == "Long:Long:Int:Long2String"
    result &= FunctionSignature.getSignatureLongLongIntDouble2String(helperLongLongIntDouble2String) == "Long:Long:Int:Double2String"
    result &= FunctionSignature.getSignatureLongLongIntString2String(helperLongLongIntString2String) == "Long:Long:Int:String2String"
    result &= FunctionSignature.getSignatureLongLongIntBoolean2String(helperLongLongIntBoolean2String) == "Long:Long:Int:Boolean2String"
    result &= FunctionSignature.getSignatureLongLongLongInt2String(helperLongLongLongInt2String) == "Long:Long:Long:Int2String"
    result &= FunctionSignature.getSignatureLongLongLongLong2String(helperLongLongLongLong2String) == "Long:Long:Long:Long2String"
    result &= FunctionSignature.getSignatureLongLongLongDouble2String(helperLongLongLongDouble2String) == "Long:Long:Long:Double2String"
    result &= FunctionSignature.getSignatureLongLongLongString2String(helperLongLongLongString2String) == "Long:Long:Long:String2String"
    result &= FunctionSignature.getSignatureLongLongLongBoolean2String(helperLongLongLongBoolean2String) == "Long:Long:Long:Boolean2String"
    result &= FunctionSignature.getSignatureLongLongDoubleInt2String(helperLongLongDoubleInt2String) == "Long:Long:Double:Int2String"
    result &= FunctionSignature.getSignatureLongLongDoubleLong2String(helperLongLongDoubleLong2String) == "Long:Long:Double:Long2String"
    result &= FunctionSignature.getSignatureLongLongDoubleDouble2String(helperLongLongDoubleDouble2String) == "Long:Long:Double:Double2String"
    result &= FunctionSignature.getSignatureLongLongDoubleString2String(helperLongLongDoubleString2String) == "Long:Long:Double:String2String"
    result &= FunctionSignature.getSignatureLongLongDoubleBoolean2String(helperLongLongDoubleBoolean2String) == "Long:Long:Double:Boolean2String"
    result &= FunctionSignature.getSignatureLongLongStringInt2String(helperLongLongStringInt2String) == "Long:Long:String:Int2String"
    result &= FunctionSignature.getSignatureLongLongStringLong2String(helperLongLongStringLong2String) == "Long:Long:String:Long2String"
    result &= FunctionSignature.getSignatureLongLongStringDouble2String(helperLongLongStringDouble2String) == "Long:Long:String:Double2String"
    result &= FunctionSignature.getSignatureLongLongStringString2String(helperLongLongStringString2String) == "Long:Long:String:String2String"
    result &= FunctionSignature.getSignatureLongLongStringBoolean2String(helperLongLongStringBoolean2String) == "Long:Long:String:Boolean2String"
    result &= FunctionSignature.getSignatureLongLongBooleanInt2String(helperLongLongBooleanInt2String) == "Long:Long:Boolean:Int2String"
    result &= FunctionSignature.getSignatureLongLongBooleanLong2String(helperLongLongBooleanLong2String) == "Long:Long:Boolean:Long2String"
    result &= FunctionSignature.getSignatureLongLongBooleanDouble2String(helperLongLongBooleanDouble2String) == "Long:Long:Boolean:Double2String"
    result &= FunctionSignature.getSignatureLongLongBooleanString2String(helperLongLongBooleanString2String) == "Long:Long:Boolean:String2String"
    result &= FunctionSignature.getSignatureLongLongBooleanBoolean2String(helperLongLongBooleanBoolean2String) == "Long:Long:Boolean:Boolean2String"
    result &= FunctionSignature.getSignatureLongDoubleIntInt2String(helperLongDoubleIntInt2String) == "Long:Double:Int:Int2String"
    result &= FunctionSignature.getSignatureLongDoubleIntLong2String(helperLongDoubleIntLong2String) == "Long:Double:Int:Long2String"
    result &= FunctionSignature.getSignatureLongDoubleIntDouble2String(helperLongDoubleIntDouble2String) == "Long:Double:Int:Double2String"
    result &= FunctionSignature.getSignatureLongDoubleIntString2String(helperLongDoubleIntString2String) == "Long:Double:Int:String2String"
    result &= FunctionSignature.getSignatureLongDoubleIntBoolean2String(helperLongDoubleIntBoolean2String) == "Long:Double:Int:Boolean2String"
    result &= FunctionSignature.getSignatureLongDoubleLongInt2String(helperLongDoubleLongInt2String) == "Long:Double:Long:Int2String"
    result &= FunctionSignature.getSignatureLongDoubleLongLong2String(helperLongDoubleLongLong2String) == "Long:Double:Long:Long2String"
    result &= FunctionSignature.getSignatureLongDoubleLongDouble2String(helperLongDoubleLongDouble2String) == "Long:Double:Long:Double2String"
    result &= FunctionSignature.getSignatureLongDoubleLongString2String(helperLongDoubleLongString2String) == "Long:Double:Long:String2String"
    result &= FunctionSignature.getSignatureLongDoubleLongBoolean2String(helperLongDoubleLongBoolean2String) == "Long:Double:Long:Boolean2String"
    result &= FunctionSignature.getSignatureLongDoubleDoubleInt2String(helperLongDoubleDoubleInt2String) == "Long:Double:Double:Int2String"
    result &= FunctionSignature.getSignatureLongDoubleDoubleLong2String(helperLongDoubleDoubleLong2String) == "Long:Double:Double:Long2String"
    result &= FunctionSignature.getSignatureLongDoubleDoubleDouble2String(helperLongDoubleDoubleDouble2String) == "Long:Double:Double:Double2String"
    result &= FunctionSignature.getSignatureLongDoubleDoubleString2String(helperLongDoubleDoubleString2String) == "Long:Double:Double:String2String"
    result &= FunctionSignature.getSignatureLongDoubleDoubleBoolean2String(helperLongDoubleDoubleBoolean2String) == "Long:Double:Double:Boolean2String"
    result &= FunctionSignature.getSignatureLongDoubleStringInt2String(helperLongDoubleStringInt2String) == "Long:Double:String:Int2String"
    result &= FunctionSignature.getSignatureLongDoubleStringLong2String(helperLongDoubleStringLong2String) == "Long:Double:String:Long2String"
    result &= FunctionSignature.getSignatureLongDoubleStringDouble2String(helperLongDoubleStringDouble2String) == "Long:Double:String:Double2String"
    result &= FunctionSignature.getSignatureLongDoubleStringString2String(helperLongDoubleStringString2String) == "Long:Double:String:String2String"
    result &= FunctionSignature.getSignatureLongDoubleStringBoolean2String(helperLongDoubleStringBoolean2String) == "Long:Double:String:Boolean2String"
    result &= FunctionSignature.getSignatureLongDoubleBooleanInt2String(helperLongDoubleBooleanInt2String) == "Long:Double:Boolean:Int2String"
    result &= FunctionSignature.getSignatureLongDoubleBooleanLong2String(helperLongDoubleBooleanLong2String) == "Long:Double:Boolean:Long2String"
    result &= FunctionSignature.getSignatureLongDoubleBooleanDouble2String(helperLongDoubleBooleanDouble2String) == "Long:Double:Boolean:Double2String"
    result &= FunctionSignature.getSignatureLongDoubleBooleanString2String(helperLongDoubleBooleanString2String) == "Long:Double:Boolean:String2String"
    result &= FunctionSignature.getSignatureLongDoubleBooleanBoolean2String(helperLongDoubleBooleanBoolean2String) == "Long:Double:Boolean:Boolean2String"
    result &= FunctionSignature.getSignatureLongStringIntInt2String(helperLongStringIntInt2String) == "Long:String:Int:Int2String"
    result &= FunctionSignature.getSignatureLongStringIntLong2String(helperLongStringIntLong2String) == "Long:String:Int:Long2String"
    result &= FunctionSignature.getSignatureLongStringIntDouble2String(helperLongStringIntDouble2String) == "Long:String:Int:Double2String"
    result &= FunctionSignature.getSignatureLongStringIntString2String(helperLongStringIntString2String) == "Long:String:Int:String2String"
    result &= FunctionSignature.getSignatureLongStringIntBoolean2String(helperLongStringIntBoolean2String) == "Long:String:Int:Boolean2String"
    result &= FunctionSignature.getSignatureLongStringLongInt2String(helperLongStringLongInt2String) == "Long:String:Long:Int2String"
    result &= FunctionSignature.getSignatureLongStringLongLong2String(helperLongStringLongLong2String) == "Long:String:Long:Long2String"
    result &= FunctionSignature.getSignatureLongStringLongDouble2String(helperLongStringLongDouble2String) == "Long:String:Long:Double2String"
    result &= FunctionSignature.getSignatureLongStringLongString2String(helperLongStringLongString2String) == "Long:String:Long:String2String"
    result &= FunctionSignature.getSignatureLongStringLongBoolean2String(helperLongStringLongBoolean2String) == "Long:String:Long:Boolean2String"
    result &= FunctionSignature.getSignatureLongStringDoubleInt2String(helperLongStringDoubleInt2String) == "Long:String:Double:Int2String"
    result &= FunctionSignature.getSignatureLongStringDoubleLong2String(helperLongStringDoubleLong2String) == "Long:String:Double:Long2String"
    result &= FunctionSignature.getSignatureLongStringDoubleDouble2String(helperLongStringDoubleDouble2String) == "Long:String:Double:Double2String"
    result &= FunctionSignature.getSignatureLongStringDoubleString2String(helperLongStringDoubleString2String) == "Long:String:Double:String2String"
    result &= FunctionSignature.getSignatureLongStringDoubleBoolean2String(helperLongStringDoubleBoolean2String) == "Long:String:Double:Boolean2String"
    result &= FunctionSignature.getSignatureLongStringStringInt2String(helperLongStringStringInt2String) == "Long:String:String:Int2String"
    result &= FunctionSignature.getSignatureLongStringStringLong2String(helperLongStringStringLong2String) == "Long:String:String:Long2String"
    result &= FunctionSignature.getSignatureLongStringStringDouble2String(helperLongStringStringDouble2String) == "Long:String:String:Double2String"
    result &= FunctionSignature.getSignatureLongStringStringString2String(helperLongStringStringString2String) == "Long:String:String:String2String"
    result &= FunctionSignature.getSignatureLongStringStringBoolean2String(helperLongStringStringBoolean2String) == "Long:String:String:Boolean2String"
    result &= FunctionSignature.getSignatureLongStringBooleanInt2String(helperLongStringBooleanInt2String) == "Long:String:Boolean:Int2String"
    result &= FunctionSignature.getSignatureLongStringBooleanLong2String(helperLongStringBooleanLong2String) == "Long:String:Boolean:Long2String"
    result &= FunctionSignature.getSignatureLongStringBooleanDouble2String(helperLongStringBooleanDouble2String) == "Long:String:Boolean:Double2String"
    result &= FunctionSignature.getSignatureLongStringBooleanString2String(helperLongStringBooleanString2String) == "Long:String:Boolean:String2String"
    result &= FunctionSignature.getSignatureLongStringBooleanBoolean2String(helperLongStringBooleanBoolean2String) == "Long:String:Boolean:Boolean2String"
    result &= FunctionSignature.getSignatureLongBooleanIntInt2String(helperLongBooleanIntInt2String) == "Long:Boolean:Int:Int2String"
    result &= FunctionSignature.getSignatureLongBooleanIntLong2String(helperLongBooleanIntLong2String) == "Long:Boolean:Int:Long2String"
    result &= FunctionSignature.getSignatureLongBooleanIntDouble2String(helperLongBooleanIntDouble2String) == "Long:Boolean:Int:Double2String"
    result &= FunctionSignature.getSignatureLongBooleanIntString2String(helperLongBooleanIntString2String) == "Long:Boolean:Int:String2String"
    result &= FunctionSignature.getSignatureLongBooleanIntBoolean2String(helperLongBooleanIntBoolean2String) == "Long:Boolean:Int:Boolean2String"
    result &= FunctionSignature.getSignatureLongBooleanLongInt2String(helperLongBooleanLongInt2String) == "Long:Boolean:Long:Int2String"
    result &= FunctionSignature.getSignatureLongBooleanLongLong2String(helperLongBooleanLongLong2String) == "Long:Boolean:Long:Long2String"
    result &= FunctionSignature.getSignatureLongBooleanLongDouble2String(helperLongBooleanLongDouble2String) == "Long:Boolean:Long:Double2String"
    result &= FunctionSignature.getSignatureLongBooleanLongString2String(helperLongBooleanLongString2String) == "Long:Boolean:Long:String2String"
    result &= FunctionSignature.getSignatureLongBooleanLongBoolean2String(helperLongBooleanLongBoolean2String) == "Long:Boolean:Long:Boolean2String"
    result &= FunctionSignature.getSignatureLongBooleanDoubleInt2String(helperLongBooleanDoubleInt2String) == "Long:Boolean:Double:Int2String"
    result &= FunctionSignature.getSignatureLongBooleanDoubleLong2String(helperLongBooleanDoubleLong2String) == "Long:Boolean:Double:Long2String"
    result &= FunctionSignature.getSignatureLongBooleanDoubleDouble2String(helperLongBooleanDoubleDouble2String) == "Long:Boolean:Double:Double2String"
    result &= FunctionSignature.getSignatureLongBooleanDoubleString2String(helperLongBooleanDoubleString2String) == "Long:Boolean:Double:String2String"
    result &= FunctionSignature.getSignatureLongBooleanDoubleBoolean2String(helperLongBooleanDoubleBoolean2String) == "Long:Boolean:Double:Boolean2String"
    result &= FunctionSignature.getSignatureLongBooleanStringInt2String(helperLongBooleanStringInt2String) == "Long:Boolean:String:Int2String"
    result &= FunctionSignature.getSignatureLongBooleanStringLong2String(helperLongBooleanStringLong2String) == "Long:Boolean:String:Long2String"
    result &= FunctionSignature.getSignatureLongBooleanStringDouble2String(helperLongBooleanStringDouble2String) == "Long:Boolean:String:Double2String"
    result &= FunctionSignature.getSignatureLongBooleanStringString2String(helperLongBooleanStringString2String) == "Long:Boolean:String:String2String"
    result &= FunctionSignature.getSignatureLongBooleanStringBoolean2String(helperLongBooleanStringBoolean2String) == "Long:Boolean:String:Boolean2String"
    result &= FunctionSignature.getSignatureLongBooleanBooleanInt2String(helperLongBooleanBooleanInt2String) == "Long:Boolean:Boolean:Int2String"
    result &= FunctionSignature.getSignatureLongBooleanBooleanLong2String(helperLongBooleanBooleanLong2String) == "Long:Boolean:Boolean:Long2String"
    result &= FunctionSignature.getSignatureLongBooleanBooleanDouble2String(helperLongBooleanBooleanDouble2String) == "Long:Boolean:Boolean:Double2String"
    result &= FunctionSignature.getSignatureLongBooleanBooleanString2String(helperLongBooleanBooleanString2String) == "Long:Boolean:Boolean:String2String"
    result &= FunctionSignature.getSignatureLongBooleanBooleanBoolean2String(helperLongBooleanBooleanBoolean2String) == "Long:Boolean:Boolean:Boolean2String"
    result &= FunctionSignature.getSignatureDoubleIntIntInt2String(helperDoubleIntIntInt2String) == "Double:Int:Int:Int2String"
    result &= FunctionSignature.getSignatureDoubleIntIntLong2String(helperDoubleIntIntLong2String) == "Double:Int:Int:Long2String"
    result &= FunctionSignature.getSignatureDoubleIntIntDouble2String(helperDoubleIntIntDouble2String) == "Double:Int:Int:Double2String"
    result &= FunctionSignature.getSignatureDoubleIntIntString2String(helperDoubleIntIntString2String) == "Double:Int:Int:String2String"
    result &= FunctionSignature.getSignatureDoubleIntIntBoolean2String(helperDoubleIntIntBoolean2String) == "Double:Int:Int:Boolean2String"
    result &= FunctionSignature.getSignatureDoubleIntLongInt2String(helperDoubleIntLongInt2String) == "Double:Int:Long:Int2String"
    result &= FunctionSignature.getSignatureDoubleIntLongLong2String(helperDoubleIntLongLong2String) == "Double:Int:Long:Long2String"
    result &= FunctionSignature.getSignatureDoubleIntLongDouble2String(helperDoubleIntLongDouble2String) == "Double:Int:Long:Double2String"
    result &= FunctionSignature.getSignatureDoubleIntLongString2String(helperDoubleIntLongString2String) == "Double:Int:Long:String2String"
    result &= FunctionSignature.getSignatureDoubleIntLongBoolean2String(helperDoubleIntLongBoolean2String) == "Double:Int:Long:Boolean2String"
    result &= FunctionSignature.getSignatureDoubleIntDoubleInt2String(helperDoubleIntDoubleInt2String) == "Double:Int:Double:Int2String"
    result &= FunctionSignature.getSignatureDoubleIntDoubleLong2String(helperDoubleIntDoubleLong2String) == "Double:Int:Double:Long2String"
    result &= FunctionSignature.getSignatureDoubleIntDoubleDouble2String(helperDoubleIntDoubleDouble2String) == "Double:Int:Double:Double2String"
    result &= FunctionSignature.getSignatureDoubleIntDoubleString2String(helperDoubleIntDoubleString2String) == "Double:Int:Double:String2String"
    result &= FunctionSignature.getSignatureDoubleIntDoubleBoolean2String(helperDoubleIntDoubleBoolean2String) == "Double:Int:Double:Boolean2String"
    result &= FunctionSignature.getSignatureDoubleIntStringInt2String(helperDoubleIntStringInt2String) == "Double:Int:String:Int2String"
    result &= FunctionSignature.getSignatureDoubleIntStringLong2String(helperDoubleIntStringLong2String) == "Double:Int:String:Long2String"
    result &= FunctionSignature.getSignatureDoubleIntStringDouble2String(helperDoubleIntStringDouble2String) == "Double:Int:String:Double2String"
    result &= FunctionSignature.getSignatureDoubleIntStringString2String(helperDoubleIntStringString2String) == "Double:Int:String:String2String"
    result &= FunctionSignature.getSignatureDoubleIntStringBoolean2String(helperDoubleIntStringBoolean2String) == "Double:Int:String:Boolean2String"
    result &= FunctionSignature.getSignatureDoubleIntBooleanInt2String(helperDoubleIntBooleanInt2String) == "Double:Int:Boolean:Int2String"
    result &= FunctionSignature.getSignatureDoubleIntBooleanLong2String(helperDoubleIntBooleanLong2String) == "Double:Int:Boolean:Long2String"
    result &= FunctionSignature.getSignatureDoubleIntBooleanDouble2String(helperDoubleIntBooleanDouble2String) == "Double:Int:Boolean:Double2String"
    result &= FunctionSignature.getSignatureDoubleIntBooleanString2String(helperDoubleIntBooleanString2String) == "Double:Int:Boolean:String2String"
    result &= FunctionSignature.getSignatureDoubleIntBooleanBoolean2String(helperDoubleIntBooleanBoolean2String) == "Double:Int:Boolean:Boolean2String"
    result &= FunctionSignature.getSignatureDoubleLongIntInt2String(helperDoubleLongIntInt2String) == "Double:Long:Int:Int2String"
    result &= FunctionSignature.getSignatureDoubleLongIntLong2String(helperDoubleLongIntLong2String) == "Double:Long:Int:Long2String"
    result &= FunctionSignature.getSignatureDoubleLongIntDouble2String(helperDoubleLongIntDouble2String) == "Double:Long:Int:Double2String"
    result &= FunctionSignature.getSignatureDoubleLongIntString2String(helperDoubleLongIntString2String) == "Double:Long:Int:String2String"
    result &= FunctionSignature.getSignatureDoubleLongIntBoolean2String(helperDoubleLongIntBoolean2String) == "Double:Long:Int:Boolean2String"
    result &= FunctionSignature.getSignatureDoubleLongLongInt2String(helperDoubleLongLongInt2String) == "Double:Long:Long:Int2String"
    result &= FunctionSignature.getSignatureDoubleLongLongLong2String(helperDoubleLongLongLong2String) == "Double:Long:Long:Long2String"
    result &= FunctionSignature.getSignatureDoubleLongLongDouble2String(helperDoubleLongLongDouble2String) == "Double:Long:Long:Double2String"
    result &= FunctionSignature.getSignatureDoubleLongLongString2String(helperDoubleLongLongString2String) == "Double:Long:Long:String2String"
    result &= FunctionSignature.getSignatureDoubleLongLongBoolean2String(helperDoubleLongLongBoolean2String) == "Double:Long:Long:Boolean2String"
    result &= FunctionSignature.getSignatureDoubleLongDoubleInt2String(helperDoubleLongDoubleInt2String) == "Double:Long:Double:Int2String"
    result &= FunctionSignature.getSignatureDoubleLongDoubleLong2String(helperDoubleLongDoubleLong2String) == "Double:Long:Double:Long2String"
    result &= FunctionSignature.getSignatureDoubleLongDoubleDouble2String(helperDoubleLongDoubleDouble2String) == "Double:Long:Double:Double2String"
    result &= FunctionSignature.getSignatureDoubleLongDoubleString2String(helperDoubleLongDoubleString2String) == "Double:Long:Double:String2String"
    result &= FunctionSignature.getSignatureDoubleLongDoubleBoolean2String(helperDoubleLongDoubleBoolean2String) == "Double:Long:Double:Boolean2String"
    result &= FunctionSignature.getSignatureDoubleLongStringInt2String(helperDoubleLongStringInt2String) == "Double:Long:String:Int2String"
    result &= FunctionSignature.getSignatureDoubleLongStringLong2String(helperDoubleLongStringLong2String) == "Double:Long:String:Long2String"
    result &= FunctionSignature.getSignatureDoubleLongStringDouble2String(helperDoubleLongStringDouble2String) == "Double:Long:String:Double2String"
    result &= FunctionSignature.getSignatureDoubleLongStringString2String(helperDoubleLongStringString2String) == "Double:Long:String:String2String"
    result &= FunctionSignature.getSignatureDoubleLongStringBoolean2String(helperDoubleLongStringBoolean2String) == "Double:Long:String:Boolean2String"
    result &= FunctionSignature.getSignatureDoubleLongBooleanInt2String(helperDoubleLongBooleanInt2String) == "Double:Long:Boolean:Int2String"
    result &= FunctionSignature.getSignatureDoubleLongBooleanLong2String(helperDoubleLongBooleanLong2String) == "Double:Long:Boolean:Long2String"
    result &= FunctionSignature.getSignatureDoubleLongBooleanDouble2String(helperDoubleLongBooleanDouble2String) == "Double:Long:Boolean:Double2String"
    result &= FunctionSignature.getSignatureDoubleLongBooleanString2String(helperDoubleLongBooleanString2String) == "Double:Long:Boolean:String2String"
    result &= FunctionSignature.getSignatureDoubleLongBooleanBoolean2String(helperDoubleLongBooleanBoolean2String) == "Double:Long:Boolean:Boolean2String"
    result &= FunctionSignature.getSignatureDoubleDoubleIntInt2String(helperDoubleDoubleIntInt2String) == "Double:Double:Int:Int2String"
    result &= FunctionSignature.getSignatureDoubleDoubleIntLong2String(helperDoubleDoubleIntLong2String) == "Double:Double:Int:Long2String"
    result &= FunctionSignature.getSignatureDoubleDoubleIntDouble2String(helperDoubleDoubleIntDouble2String) == "Double:Double:Int:Double2String"
    result &= FunctionSignature.getSignatureDoubleDoubleIntString2String(helperDoubleDoubleIntString2String) == "Double:Double:Int:String2String"
    result &= FunctionSignature.getSignatureDoubleDoubleIntBoolean2String(helperDoubleDoubleIntBoolean2String) == "Double:Double:Int:Boolean2String"
    result &= FunctionSignature.getSignatureDoubleDoubleLongInt2String(helperDoubleDoubleLongInt2String) == "Double:Double:Long:Int2String"
    result &= FunctionSignature.getSignatureDoubleDoubleLongLong2String(helperDoubleDoubleLongLong2String) == "Double:Double:Long:Long2String"
    result &= FunctionSignature.getSignatureDoubleDoubleLongDouble2String(helperDoubleDoubleLongDouble2String) == "Double:Double:Long:Double2String"
    result &= FunctionSignature.getSignatureDoubleDoubleLongString2String(helperDoubleDoubleLongString2String) == "Double:Double:Long:String2String"
    result &= FunctionSignature.getSignatureDoubleDoubleLongBoolean2String(helperDoubleDoubleLongBoolean2String) == "Double:Double:Long:Boolean2String"
    result &= FunctionSignature.getSignatureDoubleDoubleDoubleInt2String(helperDoubleDoubleDoubleInt2String) == "Double:Double:Double:Int2String"
    result &= FunctionSignature.getSignatureDoubleDoubleDoubleLong2String(helperDoubleDoubleDoubleLong2String) == "Double:Double:Double:Long2String"
    result &= FunctionSignature.getSignatureDoubleDoubleDoubleDouble2String(helperDoubleDoubleDoubleDouble2String) == "Double:Double:Double:Double2String"
    result &= FunctionSignature.getSignatureDoubleDoubleDoubleString2String(helperDoubleDoubleDoubleString2String) == "Double:Double:Double:String2String"
    result &= FunctionSignature.getSignatureDoubleDoubleDoubleBoolean2String(helperDoubleDoubleDoubleBoolean2String) == "Double:Double:Double:Boolean2String"
    result &= FunctionSignature.getSignatureDoubleDoubleStringInt2String(helperDoubleDoubleStringInt2String) == "Double:Double:String:Int2String"
    result &= FunctionSignature.getSignatureDoubleDoubleStringLong2String(helperDoubleDoubleStringLong2String) == "Double:Double:String:Long2String"
    result &= FunctionSignature.getSignatureDoubleDoubleStringDouble2String(helperDoubleDoubleStringDouble2String) == "Double:Double:String:Double2String"
    result &= FunctionSignature.getSignatureDoubleDoubleStringString2String(helperDoubleDoubleStringString2String) == "Double:Double:String:String2String"
    result &= FunctionSignature.getSignatureDoubleDoubleStringBoolean2String(helperDoubleDoubleStringBoolean2String) == "Double:Double:String:Boolean2String"
    result &= FunctionSignature.getSignatureDoubleDoubleBooleanInt2String(helperDoubleDoubleBooleanInt2String) == "Double:Double:Boolean:Int2String"
    result &= FunctionSignature.getSignatureDoubleDoubleBooleanLong2String(helperDoubleDoubleBooleanLong2String) == "Double:Double:Boolean:Long2String"
    result &= FunctionSignature.getSignatureDoubleDoubleBooleanDouble2String(helperDoubleDoubleBooleanDouble2String) == "Double:Double:Boolean:Double2String"
    result &= FunctionSignature.getSignatureDoubleDoubleBooleanString2String(helperDoubleDoubleBooleanString2String) == "Double:Double:Boolean:String2String"
    result &= FunctionSignature.getSignatureDoubleDoubleBooleanBoolean2String(helperDoubleDoubleBooleanBoolean2String) == "Double:Double:Boolean:Boolean2String"
    result &= FunctionSignature.getSignatureDoubleStringIntInt2String(helperDoubleStringIntInt2String) == "Double:String:Int:Int2String"
    result &= FunctionSignature.getSignatureDoubleStringIntLong2String(helperDoubleStringIntLong2String) == "Double:String:Int:Long2String"
    result &= FunctionSignature.getSignatureDoubleStringIntDouble2String(helperDoubleStringIntDouble2String) == "Double:String:Int:Double2String"
    result &= FunctionSignature.getSignatureDoubleStringIntString2String(helperDoubleStringIntString2String) == "Double:String:Int:String2String"
    result &= FunctionSignature.getSignatureDoubleStringIntBoolean2String(helperDoubleStringIntBoolean2String) == "Double:String:Int:Boolean2String"
    result &= FunctionSignature.getSignatureDoubleStringLongInt2String(helperDoubleStringLongInt2String) == "Double:String:Long:Int2String"
    result &= FunctionSignature.getSignatureDoubleStringLongLong2String(helperDoubleStringLongLong2String) == "Double:String:Long:Long2String"
    result &= FunctionSignature.getSignatureDoubleStringLongDouble2String(helperDoubleStringLongDouble2String) == "Double:String:Long:Double2String"
    result &= FunctionSignature.getSignatureDoubleStringLongString2String(helperDoubleStringLongString2String) == "Double:String:Long:String2String"
    result &= FunctionSignature.getSignatureDoubleStringLongBoolean2String(helperDoubleStringLongBoolean2String) == "Double:String:Long:Boolean2String"
    result &= FunctionSignature.getSignatureDoubleStringDoubleInt2String(helperDoubleStringDoubleInt2String) == "Double:String:Double:Int2String"
    result &= FunctionSignature.getSignatureDoubleStringDoubleLong2String(helperDoubleStringDoubleLong2String) == "Double:String:Double:Long2String"
    result &= FunctionSignature.getSignatureDoubleStringDoubleDouble2String(helperDoubleStringDoubleDouble2String) == "Double:String:Double:Double2String"
    result &= FunctionSignature.getSignatureDoubleStringDoubleString2String(helperDoubleStringDoubleString2String) == "Double:String:Double:String2String"
    result &= FunctionSignature.getSignatureDoubleStringDoubleBoolean2String(helperDoubleStringDoubleBoolean2String) == "Double:String:Double:Boolean2String"
    result &= FunctionSignature.getSignatureDoubleStringStringInt2String(helperDoubleStringStringInt2String) == "Double:String:String:Int2String"
    result &= FunctionSignature.getSignatureDoubleStringStringLong2String(helperDoubleStringStringLong2String) == "Double:String:String:Long2String"
    result &= FunctionSignature.getSignatureDoubleStringStringDouble2String(helperDoubleStringStringDouble2String) == "Double:String:String:Double2String"
    result &= FunctionSignature.getSignatureDoubleStringStringString2String(helperDoubleStringStringString2String) == "Double:String:String:String2String"
    result &= FunctionSignature.getSignatureDoubleStringStringBoolean2String(helperDoubleStringStringBoolean2String) == "Double:String:String:Boolean2String"
    result &= FunctionSignature.getSignatureDoubleStringBooleanInt2String(helperDoubleStringBooleanInt2String) == "Double:String:Boolean:Int2String"
    result &= FunctionSignature.getSignatureDoubleStringBooleanLong2String(helperDoubleStringBooleanLong2String) == "Double:String:Boolean:Long2String"
    result &= FunctionSignature.getSignatureDoubleStringBooleanDouble2String(helperDoubleStringBooleanDouble2String) == "Double:String:Boolean:Double2String"
    result &= FunctionSignature.getSignatureDoubleStringBooleanString2String(helperDoubleStringBooleanString2String) == "Double:String:Boolean:String2String"
    result &= FunctionSignature.getSignatureDoubleStringBooleanBoolean2String(helperDoubleStringBooleanBoolean2String) == "Double:String:Boolean:Boolean2String"
    result &= FunctionSignature.getSignatureDoubleBooleanIntInt2String(helperDoubleBooleanIntInt2String) == "Double:Boolean:Int:Int2String"
    result &= FunctionSignature.getSignatureDoubleBooleanIntLong2String(helperDoubleBooleanIntLong2String) == "Double:Boolean:Int:Long2String"
    result &= FunctionSignature.getSignatureDoubleBooleanIntDouble2String(helperDoubleBooleanIntDouble2String) == "Double:Boolean:Int:Double2String"
    result &= FunctionSignature.getSignatureDoubleBooleanIntString2String(helperDoubleBooleanIntString2String) == "Double:Boolean:Int:String2String"
    result &= FunctionSignature.getSignatureDoubleBooleanIntBoolean2String(helperDoubleBooleanIntBoolean2String) == "Double:Boolean:Int:Boolean2String"
    result &= FunctionSignature.getSignatureDoubleBooleanLongInt2String(helperDoubleBooleanLongInt2String) == "Double:Boolean:Long:Int2String"
    result &= FunctionSignature.getSignatureDoubleBooleanLongLong2String(helperDoubleBooleanLongLong2String) == "Double:Boolean:Long:Long2String"
    result &= FunctionSignature.getSignatureDoubleBooleanLongDouble2String(helperDoubleBooleanLongDouble2String) == "Double:Boolean:Long:Double2String"
    result &= FunctionSignature.getSignatureDoubleBooleanLongString2String(helperDoubleBooleanLongString2String) == "Double:Boolean:Long:String2String"
    result &= FunctionSignature.getSignatureDoubleBooleanLongBoolean2String(helperDoubleBooleanLongBoolean2String) == "Double:Boolean:Long:Boolean2String"
    result &= FunctionSignature.getSignatureDoubleBooleanDoubleInt2String(helperDoubleBooleanDoubleInt2String) == "Double:Boolean:Double:Int2String"
    result &= FunctionSignature.getSignatureDoubleBooleanDoubleLong2String(helperDoubleBooleanDoubleLong2String) == "Double:Boolean:Double:Long2String"
    result &= FunctionSignature.getSignatureDoubleBooleanDoubleDouble2String(helperDoubleBooleanDoubleDouble2String) == "Double:Boolean:Double:Double2String"
    result &= FunctionSignature.getSignatureDoubleBooleanDoubleString2String(helperDoubleBooleanDoubleString2String) == "Double:Boolean:Double:String2String"
    result &= FunctionSignature.getSignatureDoubleBooleanDoubleBoolean2String(helperDoubleBooleanDoubleBoolean2String) == "Double:Boolean:Double:Boolean2String"
    result &= FunctionSignature.getSignatureDoubleBooleanStringInt2String(helperDoubleBooleanStringInt2String) == "Double:Boolean:String:Int2String"
    result &= FunctionSignature.getSignatureDoubleBooleanStringLong2String(helperDoubleBooleanStringLong2String) == "Double:Boolean:String:Long2String"
    result &= FunctionSignature.getSignatureDoubleBooleanStringDouble2String(helperDoubleBooleanStringDouble2String) == "Double:Boolean:String:Double2String"
    result &= FunctionSignature.getSignatureDoubleBooleanStringString2String(helperDoubleBooleanStringString2String) == "Double:Boolean:String:String2String"
    result &= FunctionSignature.getSignatureDoubleBooleanStringBoolean2String(helperDoubleBooleanStringBoolean2String) == "Double:Boolean:String:Boolean2String"
    result &= FunctionSignature.getSignatureDoubleBooleanBooleanInt2String(helperDoubleBooleanBooleanInt2String) == "Double:Boolean:Boolean:Int2String"
    result &= FunctionSignature.getSignatureDoubleBooleanBooleanLong2String(helperDoubleBooleanBooleanLong2String) == "Double:Boolean:Boolean:Long2String"
    result &= FunctionSignature.getSignatureDoubleBooleanBooleanDouble2String(helperDoubleBooleanBooleanDouble2String) == "Double:Boolean:Boolean:Double2String"
    result &= FunctionSignature.getSignatureDoubleBooleanBooleanString2String(helperDoubleBooleanBooleanString2String) == "Double:Boolean:Boolean:String2String"
    result &= FunctionSignature.getSignatureDoubleBooleanBooleanBoolean2String(helperDoubleBooleanBooleanBoolean2String) == "Double:Boolean:Boolean:Boolean2String"
    result &= FunctionSignature.getSignatureStringIntIntInt2String(helperStringIntIntInt2String) == "String:Int:Int:Int2String"
    result &= FunctionSignature.getSignatureStringIntIntLong2String(helperStringIntIntLong2String) == "String:Int:Int:Long2String"
    result &= FunctionSignature.getSignatureStringIntIntDouble2String(helperStringIntIntDouble2String) == "String:Int:Int:Double2String"
    result &= FunctionSignature.getSignatureStringIntIntString2String(helperStringIntIntString2String) == "String:Int:Int:String2String"
    result &= FunctionSignature.getSignatureStringIntIntBoolean2String(helperStringIntIntBoolean2String) == "String:Int:Int:Boolean2String"
    result &= FunctionSignature.getSignatureStringIntLongInt2String(helperStringIntLongInt2String) == "String:Int:Long:Int2String"
    result &= FunctionSignature.getSignatureStringIntLongLong2String(helperStringIntLongLong2String) == "String:Int:Long:Long2String"
    result &= FunctionSignature.getSignatureStringIntLongDouble2String(helperStringIntLongDouble2String) == "String:Int:Long:Double2String"
    result &= FunctionSignature.getSignatureStringIntLongString2String(helperStringIntLongString2String) == "String:Int:Long:String2String"
    result &= FunctionSignature.getSignatureStringIntLongBoolean2String(helperStringIntLongBoolean2String) == "String:Int:Long:Boolean2String"
    result &= FunctionSignature.getSignatureStringIntDoubleInt2String(helperStringIntDoubleInt2String) == "String:Int:Double:Int2String"
    result &= FunctionSignature.getSignatureStringIntDoubleLong2String(helperStringIntDoubleLong2String) == "String:Int:Double:Long2String"
    result &= FunctionSignature.getSignatureStringIntDoubleDouble2String(helperStringIntDoubleDouble2String) == "String:Int:Double:Double2String"
    result &= FunctionSignature.getSignatureStringIntDoubleString2String(helperStringIntDoubleString2String) == "String:Int:Double:String2String"
    result &= FunctionSignature.getSignatureStringIntDoubleBoolean2String(helperStringIntDoubleBoolean2String) == "String:Int:Double:Boolean2String"
    result &= FunctionSignature.getSignatureStringIntStringInt2String(helperStringIntStringInt2String) == "String:Int:String:Int2String"
    result &= FunctionSignature.getSignatureStringIntStringLong2String(helperStringIntStringLong2String) == "String:Int:String:Long2String"
    result &= FunctionSignature.getSignatureStringIntStringDouble2String(helperStringIntStringDouble2String) == "String:Int:String:Double2String"
    result &= FunctionSignature.getSignatureStringIntStringString2String(helperStringIntStringString2String) == "String:Int:String:String2String"
    result &= FunctionSignature.getSignatureStringIntStringBoolean2String(helperStringIntStringBoolean2String) == "String:Int:String:Boolean2String"
    result &= FunctionSignature.getSignatureStringIntBooleanInt2String(helperStringIntBooleanInt2String) == "String:Int:Boolean:Int2String"
    result &= FunctionSignature.getSignatureStringIntBooleanLong2String(helperStringIntBooleanLong2String) == "String:Int:Boolean:Long2String"
    result &= FunctionSignature.getSignatureStringIntBooleanDouble2String(helperStringIntBooleanDouble2String) == "String:Int:Boolean:Double2String"
    result &= FunctionSignature.getSignatureStringIntBooleanString2String(helperStringIntBooleanString2String) == "String:Int:Boolean:String2String"
    result &= FunctionSignature.getSignatureStringIntBooleanBoolean2String(helperStringIntBooleanBoolean2String) == "String:Int:Boolean:Boolean2String"
    result &= FunctionSignature.getSignatureStringLongIntInt2String(helperStringLongIntInt2String) == "String:Long:Int:Int2String"
    result &= FunctionSignature.getSignatureStringLongIntLong2String(helperStringLongIntLong2String) == "String:Long:Int:Long2String"
    result &= FunctionSignature.getSignatureStringLongIntDouble2String(helperStringLongIntDouble2String) == "String:Long:Int:Double2String"
    result &= FunctionSignature.getSignatureStringLongIntString2String(helperStringLongIntString2String) == "String:Long:Int:String2String"
    result &= FunctionSignature.getSignatureStringLongIntBoolean2String(helperStringLongIntBoolean2String) == "String:Long:Int:Boolean2String"
    result &= FunctionSignature.getSignatureStringLongLongInt2String(helperStringLongLongInt2String) == "String:Long:Long:Int2String"
    result &= FunctionSignature.getSignatureStringLongLongLong2String(helperStringLongLongLong2String) == "String:Long:Long:Long2String"
    result &= FunctionSignature.getSignatureStringLongLongDouble2String(helperStringLongLongDouble2String) == "String:Long:Long:Double2String"
    result &= FunctionSignature.getSignatureStringLongLongString2String(helperStringLongLongString2String) == "String:Long:Long:String2String"
    result &= FunctionSignature.getSignatureStringLongLongBoolean2String(helperStringLongLongBoolean2String) == "String:Long:Long:Boolean2String"
    result &= FunctionSignature.getSignatureStringLongDoubleInt2String(helperStringLongDoubleInt2String) == "String:Long:Double:Int2String"
    result &= FunctionSignature.getSignatureStringLongDoubleLong2String(helperStringLongDoubleLong2String) == "String:Long:Double:Long2String"
    result &= FunctionSignature.getSignatureStringLongDoubleDouble2String(helperStringLongDoubleDouble2String) == "String:Long:Double:Double2String"
    result &= FunctionSignature.getSignatureStringLongDoubleString2String(helperStringLongDoubleString2String) == "String:Long:Double:String2String"
    result &= FunctionSignature.getSignatureStringLongDoubleBoolean2String(helperStringLongDoubleBoolean2String) == "String:Long:Double:Boolean2String"
    result &= FunctionSignature.getSignatureStringLongStringInt2String(helperStringLongStringInt2String) == "String:Long:String:Int2String"
    result &= FunctionSignature.getSignatureStringLongStringLong2String(helperStringLongStringLong2String) == "String:Long:String:Long2String"
    result &= FunctionSignature.getSignatureStringLongStringDouble2String(helperStringLongStringDouble2String) == "String:Long:String:Double2String"
    result &= FunctionSignature.getSignatureStringLongStringString2String(helperStringLongStringString2String) == "String:Long:String:String2String"
    result &= FunctionSignature.getSignatureStringLongStringBoolean2String(helperStringLongStringBoolean2String) == "String:Long:String:Boolean2String"
    result &= FunctionSignature.getSignatureStringLongBooleanInt2String(helperStringLongBooleanInt2String) == "String:Long:Boolean:Int2String"
    result &= FunctionSignature.getSignatureStringLongBooleanLong2String(helperStringLongBooleanLong2String) == "String:Long:Boolean:Long2String"
    result &= FunctionSignature.getSignatureStringLongBooleanDouble2String(helperStringLongBooleanDouble2String) == "String:Long:Boolean:Double2String"
    result &= FunctionSignature.getSignatureStringLongBooleanString2String(helperStringLongBooleanString2String) == "String:Long:Boolean:String2String"
    result &= FunctionSignature.getSignatureStringLongBooleanBoolean2String(helperStringLongBooleanBoolean2String) == "String:Long:Boolean:Boolean2String"
    result &= FunctionSignature.getSignatureStringDoubleIntInt2String(helperStringDoubleIntInt2String) == "String:Double:Int:Int2String"
    result &= FunctionSignature.getSignatureStringDoubleIntLong2String(helperStringDoubleIntLong2String) == "String:Double:Int:Long2String"
    result &= FunctionSignature.getSignatureStringDoubleIntDouble2String(helperStringDoubleIntDouble2String) == "String:Double:Int:Double2String"
    result &= FunctionSignature.getSignatureStringDoubleIntString2String(helperStringDoubleIntString2String) == "String:Double:Int:String2String"
    result &= FunctionSignature.getSignatureStringDoubleIntBoolean2String(helperStringDoubleIntBoolean2String) == "String:Double:Int:Boolean2String"
    result &= FunctionSignature.getSignatureStringDoubleLongInt2String(helperStringDoubleLongInt2String) == "String:Double:Long:Int2String"
    result &= FunctionSignature.getSignatureStringDoubleLongLong2String(helperStringDoubleLongLong2String) == "String:Double:Long:Long2String"
    result &= FunctionSignature.getSignatureStringDoubleLongDouble2String(helperStringDoubleLongDouble2String) == "String:Double:Long:Double2String"
    result &= FunctionSignature.getSignatureStringDoubleLongString2String(helperStringDoubleLongString2String) == "String:Double:Long:String2String"
    result &= FunctionSignature.getSignatureStringDoubleLongBoolean2String(helperStringDoubleLongBoolean2String) == "String:Double:Long:Boolean2String"
    result &= FunctionSignature.getSignatureStringDoubleDoubleInt2String(helperStringDoubleDoubleInt2String) == "String:Double:Double:Int2String"
    result &= FunctionSignature.getSignatureStringDoubleDoubleLong2String(helperStringDoubleDoubleLong2String) == "String:Double:Double:Long2String"
    result &= FunctionSignature.getSignatureStringDoubleDoubleDouble2String(helperStringDoubleDoubleDouble2String) == "String:Double:Double:Double2String"
    result &= FunctionSignature.getSignatureStringDoubleDoubleString2String(helperStringDoubleDoubleString2String) == "String:Double:Double:String2String"
    result &= FunctionSignature.getSignatureStringDoubleDoubleBoolean2String(helperStringDoubleDoubleBoolean2String) == "String:Double:Double:Boolean2String"
    result &= FunctionSignature.getSignatureStringDoubleStringInt2String(helperStringDoubleStringInt2String) == "String:Double:String:Int2String"
    result &= FunctionSignature.getSignatureStringDoubleStringLong2String(helperStringDoubleStringLong2String) == "String:Double:String:Long2String"
    result &= FunctionSignature.getSignatureStringDoubleStringDouble2String(helperStringDoubleStringDouble2String) == "String:Double:String:Double2String"
    result &= FunctionSignature.getSignatureStringDoubleStringString2String(helperStringDoubleStringString2String) == "String:Double:String:String2String"
    result &= FunctionSignature.getSignatureStringDoubleStringBoolean2String(helperStringDoubleStringBoolean2String) == "String:Double:String:Boolean2String"
    result &= FunctionSignature.getSignatureStringDoubleBooleanInt2String(helperStringDoubleBooleanInt2String) == "String:Double:Boolean:Int2String"
    result &= FunctionSignature.getSignatureStringDoubleBooleanLong2String(helperStringDoubleBooleanLong2String) == "String:Double:Boolean:Long2String"
    result &= FunctionSignature.getSignatureStringDoubleBooleanDouble2String(helperStringDoubleBooleanDouble2String) == "String:Double:Boolean:Double2String"
    result &= FunctionSignature.getSignatureStringDoubleBooleanString2String(helperStringDoubleBooleanString2String) == "String:Double:Boolean:String2String"
    result &= FunctionSignature.getSignatureStringDoubleBooleanBoolean2String(helperStringDoubleBooleanBoolean2String) == "String:Double:Boolean:Boolean2String"
    result &= FunctionSignature.getSignatureStringStringIntInt2String(helperStringStringIntInt2String) == "String:String:Int:Int2String"
    result &= FunctionSignature.getSignatureStringStringIntLong2String(helperStringStringIntLong2String) == "String:String:Int:Long2String"
    result &= FunctionSignature.getSignatureStringStringIntDouble2String(helperStringStringIntDouble2String) == "String:String:Int:Double2String"
    result &= FunctionSignature.getSignatureStringStringIntString2String(helperStringStringIntString2String) == "String:String:Int:String2String"
    result &= FunctionSignature.getSignatureStringStringIntBoolean2String(helperStringStringIntBoolean2String) == "String:String:Int:Boolean2String"
    result &= FunctionSignature.getSignatureStringStringLongInt2String(helperStringStringLongInt2String) == "String:String:Long:Int2String"
    result &= FunctionSignature.getSignatureStringStringLongLong2String(helperStringStringLongLong2String) == "String:String:Long:Long2String"
    result &= FunctionSignature.getSignatureStringStringLongDouble2String(helperStringStringLongDouble2String) == "String:String:Long:Double2String"
    result &= FunctionSignature.getSignatureStringStringLongString2String(helperStringStringLongString2String) == "String:String:Long:String2String"
    result &= FunctionSignature.getSignatureStringStringLongBoolean2String(helperStringStringLongBoolean2String) == "String:String:Long:Boolean2String"
    result &= FunctionSignature.getSignatureStringStringDoubleInt2String(helperStringStringDoubleInt2String) == "String:String:Double:Int2String"
    result &= FunctionSignature.getSignatureStringStringDoubleLong2String(helperStringStringDoubleLong2String) == "String:String:Double:Long2String"
    result &= FunctionSignature.getSignatureStringStringDoubleDouble2String(helperStringStringDoubleDouble2String) == "String:String:Double:Double2String"
    result &= FunctionSignature.getSignatureStringStringDoubleString2String(helperStringStringDoubleString2String) == "String:String:Double:String2String"
    result &= FunctionSignature.getSignatureStringStringDoubleBoolean2String(helperStringStringDoubleBoolean2String) == "String:String:Double:Boolean2String"
    result &= FunctionSignature.getSignatureStringStringStringInt2String(helperStringStringStringInt2String) == "String:String:String:Int2String"
    result &= FunctionSignature.getSignatureStringStringStringLong2String(helperStringStringStringLong2String) == "String:String:String:Long2String"
    result &= FunctionSignature.getSignatureStringStringStringDouble2String(helperStringStringStringDouble2String) == "String:String:String:Double2String"
    result &= FunctionSignature.getSignatureStringStringStringString2String(helperStringStringStringString2String) == "String:String:String:String2String"
    result &= FunctionSignature.getSignatureStringStringStringBoolean2String(helperStringStringStringBoolean2String) == "String:String:String:Boolean2String"
    result &= FunctionSignature.getSignatureStringStringBooleanInt2String(helperStringStringBooleanInt2String) == "String:String:Boolean:Int2String"
    result &= FunctionSignature.getSignatureStringStringBooleanLong2String(helperStringStringBooleanLong2String) == "String:String:Boolean:Long2String"
    result &= FunctionSignature.getSignatureStringStringBooleanDouble2String(helperStringStringBooleanDouble2String) == "String:String:Boolean:Double2String"
    result &= FunctionSignature.getSignatureStringStringBooleanString2String(helperStringStringBooleanString2String) == "String:String:Boolean:String2String"
    result &= FunctionSignature.getSignatureStringStringBooleanBoolean2String(helperStringStringBooleanBoolean2String) == "String:String:Boolean:Boolean2String"
    result &= FunctionSignature.getSignatureStringBooleanIntInt2String(helperStringBooleanIntInt2String) == "String:Boolean:Int:Int2String"
    result &= FunctionSignature.getSignatureStringBooleanIntLong2String(helperStringBooleanIntLong2String) == "String:Boolean:Int:Long2String"
    result &= FunctionSignature.getSignatureStringBooleanIntDouble2String(helperStringBooleanIntDouble2String) == "String:Boolean:Int:Double2String"
    result &= FunctionSignature.getSignatureStringBooleanIntString2String(helperStringBooleanIntString2String) == "String:Boolean:Int:String2String"
    result &= FunctionSignature.getSignatureStringBooleanIntBoolean2String(helperStringBooleanIntBoolean2String) == "String:Boolean:Int:Boolean2String"
    result &= FunctionSignature.getSignatureStringBooleanLongInt2String(helperStringBooleanLongInt2String) == "String:Boolean:Long:Int2String"
    result &= FunctionSignature.getSignatureStringBooleanLongLong2String(helperStringBooleanLongLong2String) == "String:Boolean:Long:Long2String"
    result &= FunctionSignature.getSignatureStringBooleanLongDouble2String(helperStringBooleanLongDouble2String) == "String:Boolean:Long:Double2String"
    result &= FunctionSignature.getSignatureStringBooleanLongString2String(helperStringBooleanLongString2String) == "String:Boolean:Long:String2String"
    result &= FunctionSignature.getSignatureStringBooleanLongBoolean2String(helperStringBooleanLongBoolean2String) == "String:Boolean:Long:Boolean2String"
    result &= FunctionSignature.getSignatureStringBooleanDoubleInt2String(helperStringBooleanDoubleInt2String) == "String:Boolean:Double:Int2String"
    result &= FunctionSignature.getSignatureStringBooleanDoubleLong2String(helperStringBooleanDoubleLong2String) == "String:Boolean:Double:Long2String"
    result &= FunctionSignature.getSignatureStringBooleanDoubleDouble2String(helperStringBooleanDoubleDouble2String) == "String:Boolean:Double:Double2String"
    result &= FunctionSignature.getSignatureStringBooleanDoubleString2String(helperStringBooleanDoubleString2String) == "String:Boolean:Double:String2String"
    result &= FunctionSignature.getSignatureStringBooleanDoubleBoolean2String(helperStringBooleanDoubleBoolean2String) == "String:Boolean:Double:Boolean2String"
    result &= FunctionSignature.getSignatureStringBooleanStringInt2String(helperStringBooleanStringInt2String) == "String:Boolean:String:Int2String"
    result &= FunctionSignature.getSignatureStringBooleanStringLong2String(helperStringBooleanStringLong2String) == "String:Boolean:String:Long2String"
    result &= FunctionSignature.getSignatureStringBooleanStringDouble2String(helperStringBooleanStringDouble2String) == "String:Boolean:String:Double2String"
    result &= FunctionSignature.getSignatureStringBooleanStringString2String(helperStringBooleanStringString2String) == "String:Boolean:String:String2String"
    result &= FunctionSignature.getSignatureStringBooleanStringBoolean2String(helperStringBooleanStringBoolean2String) == "String:Boolean:String:Boolean2String"
    result &= FunctionSignature.getSignatureStringBooleanBooleanInt2String(helperStringBooleanBooleanInt2String) == "String:Boolean:Boolean:Int2String"
    result &= FunctionSignature.getSignatureStringBooleanBooleanLong2String(helperStringBooleanBooleanLong2String) == "String:Boolean:Boolean:Long2String"
    result &= FunctionSignature.getSignatureStringBooleanBooleanDouble2String(helperStringBooleanBooleanDouble2String) == "String:Boolean:Boolean:Double2String"
    result &= FunctionSignature.getSignatureStringBooleanBooleanString2String(helperStringBooleanBooleanString2String) == "String:Boolean:Boolean:String2String"
    result &= FunctionSignature.getSignatureStringBooleanBooleanBoolean2String(helperStringBooleanBooleanBoolean2String) == "String:Boolean:Boolean:Boolean2String"
    result &= FunctionSignature.getSignatureBooleanIntIntInt2String(helperBooleanIntIntInt2String) == "Boolean:Int:Int:Int2String"
    result &= FunctionSignature.getSignatureBooleanIntIntLong2String(helperBooleanIntIntLong2String) == "Boolean:Int:Int:Long2String"
    result &= FunctionSignature.getSignatureBooleanIntIntDouble2String(helperBooleanIntIntDouble2String) == "Boolean:Int:Int:Double2String"
    result &= FunctionSignature.getSignatureBooleanIntIntString2String(helperBooleanIntIntString2String) == "Boolean:Int:Int:String2String"
    result &= FunctionSignature.getSignatureBooleanIntIntBoolean2String(helperBooleanIntIntBoolean2String) == "Boolean:Int:Int:Boolean2String"
    result &= FunctionSignature.getSignatureBooleanIntLongInt2String(helperBooleanIntLongInt2String) == "Boolean:Int:Long:Int2String"
    result &= FunctionSignature.getSignatureBooleanIntLongLong2String(helperBooleanIntLongLong2String) == "Boolean:Int:Long:Long2String"
    result &= FunctionSignature.getSignatureBooleanIntLongDouble2String(helperBooleanIntLongDouble2String) == "Boolean:Int:Long:Double2String"
    result &= FunctionSignature.getSignatureBooleanIntLongString2String(helperBooleanIntLongString2String) == "Boolean:Int:Long:String2String"
    result &= FunctionSignature.getSignatureBooleanIntLongBoolean2String(helperBooleanIntLongBoolean2String) == "Boolean:Int:Long:Boolean2String"
    result &= FunctionSignature.getSignatureBooleanIntDoubleInt2String(helperBooleanIntDoubleInt2String) == "Boolean:Int:Double:Int2String"
    result &= FunctionSignature.getSignatureBooleanIntDoubleLong2String(helperBooleanIntDoubleLong2String) == "Boolean:Int:Double:Long2String"
    result &= FunctionSignature.getSignatureBooleanIntDoubleDouble2String(helperBooleanIntDoubleDouble2String) == "Boolean:Int:Double:Double2String"
    result &= FunctionSignature.getSignatureBooleanIntDoubleString2String(helperBooleanIntDoubleString2String) == "Boolean:Int:Double:String2String"
    result &= FunctionSignature.getSignatureBooleanIntDoubleBoolean2String(helperBooleanIntDoubleBoolean2String) == "Boolean:Int:Double:Boolean2String"
    result &= FunctionSignature.getSignatureBooleanIntStringInt2String(helperBooleanIntStringInt2String) == "Boolean:Int:String:Int2String"
    result &= FunctionSignature.getSignatureBooleanIntStringLong2String(helperBooleanIntStringLong2String) == "Boolean:Int:String:Long2String"
    result &= FunctionSignature.getSignatureBooleanIntStringDouble2String(helperBooleanIntStringDouble2String) == "Boolean:Int:String:Double2String"
    result &= FunctionSignature.getSignatureBooleanIntStringString2String(helperBooleanIntStringString2String) == "Boolean:Int:String:String2String"
    result &= FunctionSignature.getSignatureBooleanIntStringBoolean2String(helperBooleanIntStringBoolean2String) == "Boolean:Int:String:Boolean2String"
    result &= FunctionSignature.getSignatureBooleanIntBooleanInt2String(helperBooleanIntBooleanInt2String) == "Boolean:Int:Boolean:Int2String"
    result &= FunctionSignature.getSignatureBooleanIntBooleanLong2String(helperBooleanIntBooleanLong2String) == "Boolean:Int:Boolean:Long2String"
    result &= FunctionSignature.getSignatureBooleanIntBooleanDouble2String(helperBooleanIntBooleanDouble2String) == "Boolean:Int:Boolean:Double2String"
    result &= FunctionSignature.getSignatureBooleanIntBooleanString2String(helperBooleanIntBooleanString2String) == "Boolean:Int:Boolean:String2String"
    result &= FunctionSignature.getSignatureBooleanIntBooleanBoolean2String(helperBooleanIntBooleanBoolean2String) == "Boolean:Int:Boolean:Boolean2String"
    result &= FunctionSignature.getSignatureBooleanLongIntInt2String(helperBooleanLongIntInt2String) == "Boolean:Long:Int:Int2String"
    result &= FunctionSignature.getSignatureBooleanLongIntLong2String(helperBooleanLongIntLong2String) == "Boolean:Long:Int:Long2String"
    result &= FunctionSignature.getSignatureBooleanLongIntDouble2String(helperBooleanLongIntDouble2String) == "Boolean:Long:Int:Double2String"
    result &= FunctionSignature.getSignatureBooleanLongIntString2String(helperBooleanLongIntString2String) == "Boolean:Long:Int:String2String"
    result &= FunctionSignature.getSignatureBooleanLongIntBoolean2String(helperBooleanLongIntBoolean2String) == "Boolean:Long:Int:Boolean2String"
    result &= FunctionSignature.getSignatureBooleanLongLongInt2String(helperBooleanLongLongInt2String) == "Boolean:Long:Long:Int2String"
    result &= FunctionSignature.getSignatureBooleanLongLongLong2String(helperBooleanLongLongLong2String) == "Boolean:Long:Long:Long2String"
    result &= FunctionSignature.getSignatureBooleanLongLongDouble2String(helperBooleanLongLongDouble2String) == "Boolean:Long:Long:Double2String"
    result &= FunctionSignature.getSignatureBooleanLongLongString2String(helperBooleanLongLongString2String) == "Boolean:Long:Long:String2String"
    result &= FunctionSignature.getSignatureBooleanLongLongBoolean2String(helperBooleanLongLongBoolean2String) == "Boolean:Long:Long:Boolean2String"
    result &= FunctionSignature.getSignatureBooleanLongDoubleInt2String(helperBooleanLongDoubleInt2String) == "Boolean:Long:Double:Int2String"
    result &= FunctionSignature.getSignatureBooleanLongDoubleLong2String(helperBooleanLongDoubleLong2String) == "Boolean:Long:Double:Long2String"
    result &= FunctionSignature.getSignatureBooleanLongDoubleDouble2String(helperBooleanLongDoubleDouble2String) == "Boolean:Long:Double:Double2String"
    result &= FunctionSignature.getSignatureBooleanLongDoubleString2String(helperBooleanLongDoubleString2String) == "Boolean:Long:Double:String2String"
    result &= FunctionSignature.getSignatureBooleanLongDoubleBoolean2String(helperBooleanLongDoubleBoolean2String) == "Boolean:Long:Double:Boolean2String"
    result &= FunctionSignature.getSignatureBooleanLongStringInt2String(helperBooleanLongStringInt2String) == "Boolean:Long:String:Int2String"
    result &= FunctionSignature.getSignatureBooleanLongStringLong2String(helperBooleanLongStringLong2String) == "Boolean:Long:String:Long2String"
    result &= FunctionSignature.getSignatureBooleanLongStringDouble2String(helperBooleanLongStringDouble2String) == "Boolean:Long:String:Double2String"
    result &= FunctionSignature.getSignatureBooleanLongStringString2String(helperBooleanLongStringString2String) == "Boolean:Long:String:String2String"
    result &= FunctionSignature.getSignatureBooleanLongStringBoolean2String(helperBooleanLongStringBoolean2String) == "Boolean:Long:String:Boolean2String"
    result &= FunctionSignature.getSignatureBooleanLongBooleanInt2String(helperBooleanLongBooleanInt2String) == "Boolean:Long:Boolean:Int2String"
    result &= FunctionSignature.getSignatureBooleanLongBooleanLong2String(helperBooleanLongBooleanLong2String) == "Boolean:Long:Boolean:Long2String"
    result &= FunctionSignature.getSignatureBooleanLongBooleanDouble2String(helperBooleanLongBooleanDouble2String) == "Boolean:Long:Boolean:Double2String"
    result &= FunctionSignature.getSignatureBooleanLongBooleanString2String(helperBooleanLongBooleanString2String) == "Boolean:Long:Boolean:String2String"
    result &= FunctionSignature.getSignatureBooleanLongBooleanBoolean2String(helperBooleanLongBooleanBoolean2String) == "Boolean:Long:Boolean:Boolean2String"
    result &= FunctionSignature.getSignatureBooleanDoubleIntInt2String(helperBooleanDoubleIntInt2String) == "Boolean:Double:Int:Int2String"
    result &= FunctionSignature.getSignatureBooleanDoubleIntLong2String(helperBooleanDoubleIntLong2String) == "Boolean:Double:Int:Long2String"
    result &= FunctionSignature.getSignatureBooleanDoubleIntDouble2String(helperBooleanDoubleIntDouble2String) == "Boolean:Double:Int:Double2String"
    result &= FunctionSignature.getSignatureBooleanDoubleIntString2String(helperBooleanDoubleIntString2String) == "Boolean:Double:Int:String2String"
    result &= FunctionSignature.getSignatureBooleanDoubleIntBoolean2String(helperBooleanDoubleIntBoolean2String) == "Boolean:Double:Int:Boolean2String"
    result &= FunctionSignature.getSignatureBooleanDoubleLongInt2String(helperBooleanDoubleLongInt2String) == "Boolean:Double:Long:Int2String"
    result &= FunctionSignature.getSignatureBooleanDoubleLongLong2String(helperBooleanDoubleLongLong2String) == "Boolean:Double:Long:Long2String"
    result &= FunctionSignature.getSignatureBooleanDoubleLongDouble2String(helperBooleanDoubleLongDouble2String) == "Boolean:Double:Long:Double2String"
    result &= FunctionSignature.getSignatureBooleanDoubleLongString2String(helperBooleanDoubleLongString2String) == "Boolean:Double:Long:String2String"
    result &= FunctionSignature.getSignatureBooleanDoubleLongBoolean2String(helperBooleanDoubleLongBoolean2String) == "Boolean:Double:Long:Boolean2String"
    result &= FunctionSignature.getSignatureBooleanDoubleDoubleInt2String(helperBooleanDoubleDoubleInt2String) == "Boolean:Double:Double:Int2String"
    result &= FunctionSignature.getSignatureBooleanDoubleDoubleLong2String(helperBooleanDoubleDoubleLong2String) == "Boolean:Double:Double:Long2String"
    result &= FunctionSignature.getSignatureBooleanDoubleDoubleDouble2String(helperBooleanDoubleDoubleDouble2String) == "Boolean:Double:Double:Double2String"
    result &= FunctionSignature.getSignatureBooleanDoubleDoubleString2String(helperBooleanDoubleDoubleString2String) == "Boolean:Double:Double:String2String"
    result &= FunctionSignature.getSignatureBooleanDoubleDoubleBoolean2String(helperBooleanDoubleDoubleBoolean2String) == "Boolean:Double:Double:Boolean2String"
    result &= FunctionSignature.getSignatureBooleanDoubleStringInt2String(helperBooleanDoubleStringInt2String) == "Boolean:Double:String:Int2String"
    result &= FunctionSignature.getSignatureBooleanDoubleStringLong2String(helperBooleanDoubleStringLong2String) == "Boolean:Double:String:Long2String"
    result &= FunctionSignature.getSignatureBooleanDoubleStringDouble2String(helperBooleanDoubleStringDouble2String) == "Boolean:Double:String:Double2String"
    result &= FunctionSignature.getSignatureBooleanDoubleStringString2String(helperBooleanDoubleStringString2String) == "Boolean:Double:String:String2String"
    result &= FunctionSignature.getSignatureBooleanDoubleStringBoolean2String(helperBooleanDoubleStringBoolean2String) == "Boolean:Double:String:Boolean2String"
    result &= FunctionSignature.getSignatureBooleanDoubleBooleanInt2String(helperBooleanDoubleBooleanInt2String) == "Boolean:Double:Boolean:Int2String"
    result &= FunctionSignature.getSignatureBooleanDoubleBooleanLong2String(helperBooleanDoubleBooleanLong2String) == "Boolean:Double:Boolean:Long2String"
    result &= FunctionSignature.getSignatureBooleanDoubleBooleanDouble2String(helperBooleanDoubleBooleanDouble2String) == "Boolean:Double:Boolean:Double2String"
    result &= FunctionSignature.getSignatureBooleanDoubleBooleanString2String(helperBooleanDoubleBooleanString2String) == "Boolean:Double:Boolean:String2String"
    result &= FunctionSignature.getSignatureBooleanDoubleBooleanBoolean2String(helperBooleanDoubleBooleanBoolean2String) == "Boolean:Double:Boolean:Boolean2String"
    result &= FunctionSignature.getSignatureBooleanStringIntInt2String(helperBooleanStringIntInt2String) == "Boolean:String:Int:Int2String"
    result &= FunctionSignature.getSignatureBooleanStringIntLong2String(helperBooleanStringIntLong2String) == "Boolean:String:Int:Long2String"
    result &= FunctionSignature.getSignatureBooleanStringIntDouble2String(helperBooleanStringIntDouble2String) == "Boolean:String:Int:Double2String"
    result &= FunctionSignature.getSignatureBooleanStringIntString2String(helperBooleanStringIntString2String) == "Boolean:String:Int:String2String"
    result &= FunctionSignature.getSignatureBooleanStringIntBoolean2String(helperBooleanStringIntBoolean2String) == "Boolean:String:Int:Boolean2String"
    result &= FunctionSignature.getSignatureBooleanStringLongInt2String(helperBooleanStringLongInt2String) == "Boolean:String:Long:Int2String"
    result &= FunctionSignature.getSignatureBooleanStringLongLong2String(helperBooleanStringLongLong2String) == "Boolean:String:Long:Long2String"
    result &= FunctionSignature.getSignatureBooleanStringLongDouble2String(helperBooleanStringLongDouble2String) == "Boolean:String:Long:Double2String"
    result &= FunctionSignature.getSignatureBooleanStringLongString2String(helperBooleanStringLongString2String) == "Boolean:String:Long:String2String"
    result &= FunctionSignature.getSignatureBooleanStringLongBoolean2String(helperBooleanStringLongBoolean2String) == "Boolean:String:Long:Boolean2String"
    result &= FunctionSignature.getSignatureBooleanStringDoubleInt2String(helperBooleanStringDoubleInt2String) == "Boolean:String:Double:Int2String"
    result &= FunctionSignature.getSignatureBooleanStringDoubleLong2String(helperBooleanStringDoubleLong2String) == "Boolean:String:Double:Long2String"
    result &= FunctionSignature.getSignatureBooleanStringDoubleDouble2String(helperBooleanStringDoubleDouble2String) == "Boolean:String:Double:Double2String"
    result &= FunctionSignature.getSignatureBooleanStringDoubleString2String(helperBooleanStringDoubleString2String) == "Boolean:String:Double:String2String"
    result &= FunctionSignature.getSignatureBooleanStringDoubleBoolean2String(helperBooleanStringDoubleBoolean2String) == "Boolean:String:Double:Boolean2String"
    result &= FunctionSignature.getSignatureBooleanStringStringInt2String(helperBooleanStringStringInt2String) == "Boolean:String:String:Int2String"
    result &= FunctionSignature.getSignatureBooleanStringStringLong2String(helperBooleanStringStringLong2String) == "Boolean:String:String:Long2String"
    result &= FunctionSignature.getSignatureBooleanStringStringDouble2String(helperBooleanStringStringDouble2String) == "Boolean:String:String:Double2String"
    result &= FunctionSignature.getSignatureBooleanStringStringString2String(helperBooleanStringStringString2String) == "Boolean:String:String:String2String"
    result &= FunctionSignature.getSignatureBooleanStringStringBoolean2String(helperBooleanStringStringBoolean2String) == "Boolean:String:String:Boolean2String"
    result &= FunctionSignature.getSignatureBooleanStringBooleanInt2String(helperBooleanStringBooleanInt2String) == "Boolean:String:Boolean:Int2String"
    result &= FunctionSignature.getSignatureBooleanStringBooleanLong2String(helperBooleanStringBooleanLong2String) == "Boolean:String:Boolean:Long2String"
    result &= FunctionSignature.getSignatureBooleanStringBooleanDouble2String(helperBooleanStringBooleanDouble2String) == "Boolean:String:Boolean:Double2String"
    result &= FunctionSignature.getSignatureBooleanStringBooleanString2String(helperBooleanStringBooleanString2String) == "Boolean:String:Boolean:String2String"
    result &= FunctionSignature.getSignatureBooleanStringBooleanBoolean2String(helperBooleanStringBooleanBoolean2String) == "Boolean:String:Boolean:Boolean2String"
    result &= FunctionSignature.getSignatureBooleanBooleanIntInt2String(helperBooleanBooleanIntInt2String) == "Boolean:Boolean:Int:Int2String"
    result &= FunctionSignature.getSignatureBooleanBooleanIntLong2String(helperBooleanBooleanIntLong2String) == "Boolean:Boolean:Int:Long2String"
    result &= FunctionSignature.getSignatureBooleanBooleanIntDouble2String(helperBooleanBooleanIntDouble2String) == "Boolean:Boolean:Int:Double2String"
    result &= FunctionSignature.getSignatureBooleanBooleanIntString2String(helperBooleanBooleanIntString2String) == "Boolean:Boolean:Int:String2String"
    result &= FunctionSignature.getSignatureBooleanBooleanIntBoolean2String(helperBooleanBooleanIntBoolean2String) == "Boolean:Boolean:Int:Boolean2String"
    result &= FunctionSignature.getSignatureBooleanBooleanLongInt2String(helperBooleanBooleanLongInt2String) == "Boolean:Boolean:Long:Int2String"
    result &= FunctionSignature.getSignatureBooleanBooleanLongLong2String(helperBooleanBooleanLongLong2String) == "Boolean:Boolean:Long:Long2String"
    result &= FunctionSignature.getSignatureBooleanBooleanLongDouble2String(helperBooleanBooleanLongDouble2String) == "Boolean:Boolean:Long:Double2String"
    result &= FunctionSignature.getSignatureBooleanBooleanLongString2String(helperBooleanBooleanLongString2String) == "Boolean:Boolean:Long:String2String"
    result &= FunctionSignature.getSignatureBooleanBooleanLongBoolean2String(helperBooleanBooleanLongBoolean2String) == "Boolean:Boolean:Long:Boolean2String"
    result &= FunctionSignature.getSignatureBooleanBooleanDoubleInt2String(helperBooleanBooleanDoubleInt2String) == "Boolean:Boolean:Double:Int2String"
    result &= FunctionSignature.getSignatureBooleanBooleanDoubleLong2String(helperBooleanBooleanDoubleLong2String) == "Boolean:Boolean:Double:Long2String"
    result &= FunctionSignature.getSignatureBooleanBooleanDoubleDouble2String(helperBooleanBooleanDoubleDouble2String) == "Boolean:Boolean:Double:Double2String"
    result &= FunctionSignature.getSignatureBooleanBooleanDoubleString2String(helperBooleanBooleanDoubleString2String) == "Boolean:Boolean:Double:String2String"
    result &= FunctionSignature.getSignatureBooleanBooleanDoubleBoolean2String(helperBooleanBooleanDoubleBoolean2String) == "Boolean:Boolean:Double:Boolean2String"
    result &= FunctionSignature.getSignatureBooleanBooleanStringInt2String(helperBooleanBooleanStringInt2String) == "Boolean:Boolean:String:Int2String"
    result &= FunctionSignature.getSignatureBooleanBooleanStringLong2String(helperBooleanBooleanStringLong2String) == "Boolean:Boolean:String:Long2String"
    result &= FunctionSignature.getSignatureBooleanBooleanStringDouble2String(helperBooleanBooleanStringDouble2String) == "Boolean:Boolean:String:Double2String"
    result &= FunctionSignature.getSignatureBooleanBooleanStringString2String(helperBooleanBooleanStringString2String) == "Boolean:Boolean:String:String2String"
    result &= FunctionSignature.getSignatureBooleanBooleanStringBoolean2String(helperBooleanBooleanStringBoolean2String) == "Boolean:Boolean:String:Boolean2String"
    result &= FunctionSignature.getSignatureBooleanBooleanBooleanInt2String(helperBooleanBooleanBooleanInt2String) == "Boolean:Boolean:Boolean:Int2String"
    result &= FunctionSignature.getSignatureBooleanBooleanBooleanLong2String(helperBooleanBooleanBooleanLong2String) == "Boolean:Boolean:Boolean:Long2String"
    result &= FunctionSignature.getSignatureBooleanBooleanBooleanDouble2String(helperBooleanBooleanBooleanDouble2String) == "Boolean:Boolean:Boolean:Double2String"
    result &= FunctionSignature.getSignatureBooleanBooleanBooleanString2String(helperBooleanBooleanBooleanString2String) == "Boolean:Boolean:Boolean:String2String"
    result &= FunctionSignature.getSignatureBooleanBooleanBooleanBoolean2String(helperBooleanBooleanBooleanBoolean2String) == "Boolean:Boolean:Boolean:Boolean2String"
    assert(result)
  }
  def helperIntIntIntInt2Boolean(a1: iFun, a2: iFun, a3: iFun, a4: iFun): bFun = bFunDummy
  def helperIntIntIntLong2Boolean(a1: iFun, a2: iFun, a3: iFun, a4: lFun): bFun = bFunDummy
  def helperIntIntIntDouble2Boolean(a1: iFun, a2: iFun, a3: iFun, a4: dFun): bFun = bFunDummy
  def helperIntIntIntString2Boolean(a1: iFun, a2: iFun, a3: iFun, a4: sFun): bFun = bFunDummy
  def helperIntIntIntBoolean2Boolean(a1: iFun, a2: iFun, a3: iFun, a4: bFun): bFun = bFunDummy
  def helperIntIntLongInt2Boolean(a1: iFun, a2: iFun, a3: lFun, a4: iFun): bFun = bFunDummy
  def helperIntIntLongLong2Boolean(a1: iFun, a2: iFun, a3: lFun, a4: lFun): bFun = bFunDummy
  def helperIntIntLongDouble2Boolean(a1: iFun, a2: iFun, a3: lFun, a4: dFun): bFun = bFunDummy
  def helperIntIntLongString2Boolean(a1: iFun, a2: iFun, a3: lFun, a4: sFun): bFun = bFunDummy
  def helperIntIntLongBoolean2Boolean(a1: iFun, a2: iFun, a3: lFun, a4: bFun): bFun = bFunDummy
  def helperIntIntDoubleInt2Boolean(a1: iFun, a2: iFun, a3: dFun, a4: iFun): bFun = bFunDummy
  def helperIntIntDoubleLong2Boolean(a1: iFun, a2: iFun, a3: dFun, a4: lFun): bFun = bFunDummy
  def helperIntIntDoubleDouble2Boolean(a1: iFun, a2: iFun, a3: dFun, a4: dFun): bFun = bFunDummy
  def helperIntIntDoubleString2Boolean(a1: iFun, a2: iFun, a3: dFun, a4: sFun): bFun = bFunDummy
  def helperIntIntDoubleBoolean2Boolean(a1: iFun, a2: iFun, a3: dFun, a4: bFun): bFun = bFunDummy
  def helperIntIntStringInt2Boolean(a1: iFun, a2: iFun, a3: sFun, a4: iFun): bFun = bFunDummy
  def helperIntIntStringLong2Boolean(a1: iFun, a2: iFun, a3: sFun, a4: lFun): bFun = bFunDummy
  def helperIntIntStringDouble2Boolean(a1: iFun, a2: iFun, a3: sFun, a4: dFun): bFun = bFunDummy
  def helperIntIntStringString2Boolean(a1: iFun, a2: iFun, a3: sFun, a4: sFun): bFun = bFunDummy
  def helperIntIntStringBoolean2Boolean(a1: iFun, a2: iFun, a3: sFun, a4: bFun): bFun = bFunDummy
  def helperIntIntBooleanInt2Boolean(a1: iFun, a2: iFun, a3: bFun, a4: iFun): bFun = bFunDummy
  def helperIntIntBooleanLong2Boolean(a1: iFun, a2: iFun, a3: bFun, a4: lFun): bFun = bFunDummy
  def helperIntIntBooleanDouble2Boolean(a1: iFun, a2: iFun, a3: bFun, a4: dFun): bFun = bFunDummy
  def helperIntIntBooleanString2Boolean(a1: iFun, a2: iFun, a3: bFun, a4: sFun): bFun = bFunDummy
  def helperIntIntBooleanBoolean2Boolean(a1: iFun, a2: iFun, a3: bFun, a4: bFun): bFun = bFunDummy
  def helperIntLongIntInt2Boolean(a1: iFun, a2: lFun, a3: iFun, a4: iFun): bFun = bFunDummy
  def helperIntLongIntLong2Boolean(a1: iFun, a2: lFun, a3: iFun, a4: lFun): bFun = bFunDummy
  def helperIntLongIntDouble2Boolean(a1: iFun, a2: lFun, a3: iFun, a4: dFun): bFun = bFunDummy
  def helperIntLongIntString2Boolean(a1: iFun, a2: lFun, a3: iFun, a4: sFun): bFun = bFunDummy
  def helperIntLongIntBoolean2Boolean(a1: iFun, a2: lFun, a3: iFun, a4: bFun): bFun = bFunDummy
  def helperIntLongLongInt2Boolean(a1: iFun, a2: lFun, a3: lFun, a4: iFun): bFun = bFunDummy
  def helperIntLongLongLong2Boolean(a1: iFun, a2: lFun, a3: lFun, a4: lFun): bFun = bFunDummy
  def helperIntLongLongDouble2Boolean(a1: iFun, a2: lFun, a3: lFun, a4: dFun): bFun = bFunDummy
  def helperIntLongLongString2Boolean(a1: iFun, a2: lFun, a3: lFun, a4: sFun): bFun = bFunDummy
  def helperIntLongLongBoolean2Boolean(a1: iFun, a2: lFun, a3: lFun, a4: bFun): bFun = bFunDummy
  def helperIntLongDoubleInt2Boolean(a1: iFun, a2: lFun, a3: dFun, a4: iFun): bFun = bFunDummy
  def helperIntLongDoubleLong2Boolean(a1: iFun, a2: lFun, a3: dFun, a4: lFun): bFun = bFunDummy
  def helperIntLongDoubleDouble2Boolean(a1: iFun, a2: lFun, a3: dFun, a4: dFun): bFun = bFunDummy
  def helperIntLongDoubleString2Boolean(a1: iFun, a2: lFun, a3: dFun, a4: sFun): bFun = bFunDummy
  def helperIntLongDoubleBoolean2Boolean(a1: iFun, a2: lFun, a3: dFun, a4: bFun): bFun = bFunDummy
  def helperIntLongStringInt2Boolean(a1: iFun, a2: lFun, a3: sFun, a4: iFun): bFun = bFunDummy
  def helperIntLongStringLong2Boolean(a1: iFun, a2: lFun, a3: sFun, a4: lFun): bFun = bFunDummy
  def helperIntLongStringDouble2Boolean(a1: iFun, a2: lFun, a3: sFun, a4: dFun): bFun = bFunDummy
  def helperIntLongStringString2Boolean(a1: iFun, a2: lFun, a3: sFun, a4: sFun): bFun = bFunDummy
  def helperIntLongStringBoolean2Boolean(a1: iFun, a2: lFun, a3: sFun, a4: bFun): bFun = bFunDummy
  def helperIntLongBooleanInt2Boolean(a1: iFun, a2: lFun, a3: bFun, a4: iFun): bFun = bFunDummy
  def helperIntLongBooleanLong2Boolean(a1: iFun, a2: lFun, a3: bFun, a4: lFun): bFun = bFunDummy
  def helperIntLongBooleanDouble2Boolean(a1: iFun, a2: lFun, a3: bFun, a4: dFun): bFun = bFunDummy
  def helperIntLongBooleanString2Boolean(a1: iFun, a2: lFun, a3: bFun, a4: sFun): bFun = bFunDummy
  def helperIntLongBooleanBoolean2Boolean(a1: iFun, a2: lFun, a3: bFun, a4: bFun): bFun = bFunDummy
  def helperIntDoubleIntInt2Boolean(a1: iFun, a2: dFun, a3: iFun, a4: iFun): bFun = bFunDummy
  def helperIntDoubleIntLong2Boolean(a1: iFun, a2: dFun, a3: iFun, a4: lFun): bFun = bFunDummy
  def helperIntDoubleIntDouble2Boolean(a1: iFun, a2: dFun, a3: iFun, a4: dFun): bFun = bFunDummy
  def helperIntDoubleIntString2Boolean(a1: iFun, a2: dFun, a3: iFun, a4: sFun): bFun = bFunDummy
  def helperIntDoubleIntBoolean2Boolean(a1: iFun, a2: dFun, a3: iFun, a4: bFun): bFun = bFunDummy
  def helperIntDoubleLongInt2Boolean(a1: iFun, a2: dFun, a3: lFun, a4: iFun): bFun = bFunDummy
  def helperIntDoubleLongLong2Boolean(a1: iFun, a2: dFun, a3: lFun, a4: lFun): bFun = bFunDummy
  def helperIntDoubleLongDouble2Boolean(a1: iFun, a2: dFun, a3: lFun, a4: dFun): bFun = bFunDummy
  def helperIntDoubleLongString2Boolean(a1: iFun, a2: dFun, a3: lFun, a4: sFun): bFun = bFunDummy
  def helperIntDoubleLongBoolean2Boolean(a1: iFun, a2: dFun, a3: lFun, a4: bFun): bFun = bFunDummy
  def helperIntDoubleDoubleInt2Boolean(a1: iFun, a2: dFun, a3: dFun, a4: iFun): bFun = bFunDummy
  def helperIntDoubleDoubleLong2Boolean(a1: iFun, a2: dFun, a3: dFun, a4: lFun): bFun = bFunDummy
  def helperIntDoubleDoubleDouble2Boolean(a1: iFun, a2: dFun, a3: dFun, a4: dFun): bFun = bFunDummy
  def helperIntDoubleDoubleString2Boolean(a1: iFun, a2: dFun, a3: dFun, a4: sFun): bFun = bFunDummy
  def helperIntDoubleDoubleBoolean2Boolean(a1: iFun, a2: dFun, a3: dFun, a4: bFun): bFun = bFunDummy
  def helperIntDoubleStringInt2Boolean(a1: iFun, a2: dFun, a3: sFun, a4: iFun): bFun = bFunDummy
  def helperIntDoubleStringLong2Boolean(a1: iFun, a2: dFun, a3: sFun, a4: lFun): bFun = bFunDummy
  def helperIntDoubleStringDouble2Boolean(a1: iFun, a2: dFun, a3: sFun, a4: dFun): bFun = bFunDummy
  def helperIntDoubleStringString2Boolean(a1: iFun, a2: dFun, a3: sFun, a4: sFun): bFun = bFunDummy
  def helperIntDoubleStringBoolean2Boolean(a1: iFun, a2: dFun, a3: sFun, a4: bFun): bFun = bFunDummy
  def helperIntDoubleBooleanInt2Boolean(a1: iFun, a2: dFun, a3: bFun, a4: iFun): bFun = bFunDummy
  def helperIntDoubleBooleanLong2Boolean(a1: iFun, a2: dFun, a3: bFun, a4: lFun): bFun = bFunDummy
  def helperIntDoubleBooleanDouble2Boolean(a1: iFun, a2: dFun, a3: bFun, a4: dFun): bFun = bFunDummy
  def helperIntDoubleBooleanString2Boolean(a1: iFun, a2: dFun, a3: bFun, a4: sFun): bFun = bFunDummy
  def helperIntDoubleBooleanBoolean2Boolean(a1: iFun, a2: dFun, a3: bFun, a4: bFun): bFun = bFunDummy
  def helperIntStringIntInt2Boolean(a1: iFun, a2: sFun, a3: iFun, a4: iFun): bFun = bFunDummy
  def helperIntStringIntLong2Boolean(a1: iFun, a2: sFun, a3: iFun, a4: lFun): bFun = bFunDummy
  def helperIntStringIntDouble2Boolean(a1: iFun, a2: sFun, a3: iFun, a4: dFun): bFun = bFunDummy
  def helperIntStringIntString2Boolean(a1: iFun, a2: sFun, a3: iFun, a4: sFun): bFun = bFunDummy
  def helperIntStringIntBoolean2Boolean(a1: iFun, a2: sFun, a3: iFun, a4: bFun): bFun = bFunDummy
  def helperIntStringLongInt2Boolean(a1: iFun, a2: sFun, a3: lFun, a4: iFun): bFun = bFunDummy
  def helperIntStringLongLong2Boolean(a1: iFun, a2: sFun, a3: lFun, a4: lFun): bFun = bFunDummy
  def helperIntStringLongDouble2Boolean(a1: iFun, a2: sFun, a3: lFun, a4: dFun): bFun = bFunDummy
  def helperIntStringLongString2Boolean(a1: iFun, a2: sFun, a3: lFun, a4: sFun): bFun = bFunDummy
  def helperIntStringLongBoolean2Boolean(a1: iFun, a2: sFun, a3: lFun, a4: bFun): bFun = bFunDummy
  def helperIntStringDoubleInt2Boolean(a1: iFun, a2: sFun, a3: dFun, a4: iFun): bFun = bFunDummy
  def helperIntStringDoubleLong2Boolean(a1: iFun, a2: sFun, a3: dFun, a4: lFun): bFun = bFunDummy
  def helperIntStringDoubleDouble2Boolean(a1: iFun, a2: sFun, a3: dFun, a4: dFun): bFun = bFunDummy
  def helperIntStringDoubleString2Boolean(a1: iFun, a2: sFun, a3: dFun, a4: sFun): bFun = bFunDummy
  def helperIntStringDoubleBoolean2Boolean(a1: iFun, a2: sFun, a3: dFun, a4: bFun): bFun = bFunDummy
  def helperIntStringStringInt2Boolean(a1: iFun, a2: sFun, a3: sFun, a4: iFun): bFun = bFunDummy
  def helperIntStringStringLong2Boolean(a1: iFun, a2: sFun, a3: sFun, a4: lFun): bFun = bFunDummy
  def helperIntStringStringDouble2Boolean(a1: iFun, a2: sFun, a3: sFun, a4: dFun): bFun = bFunDummy
  def helperIntStringStringString2Boolean(a1: iFun, a2: sFun, a3: sFun, a4: sFun): bFun = bFunDummy
  def helperIntStringStringBoolean2Boolean(a1: iFun, a2: sFun, a3: sFun, a4: bFun): bFun = bFunDummy
  def helperIntStringBooleanInt2Boolean(a1: iFun, a2: sFun, a3: bFun, a4: iFun): bFun = bFunDummy
  def helperIntStringBooleanLong2Boolean(a1: iFun, a2: sFun, a3: bFun, a4: lFun): bFun = bFunDummy
  def helperIntStringBooleanDouble2Boolean(a1: iFun, a2: sFun, a3: bFun, a4: dFun): bFun = bFunDummy
  def helperIntStringBooleanString2Boolean(a1: iFun, a2: sFun, a3: bFun, a4: sFun): bFun = bFunDummy
  def helperIntStringBooleanBoolean2Boolean(a1: iFun, a2: sFun, a3: bFun, a4: bFun): bFun = bFunDummy
  def helperIntBooleanIntInt2Boolean(a1: iFun, a2: bFun, a3: iFun, a4: iFun): bFun = bFunDummy
  def helperIntBooleanIntLong2Boolean(a1: iFun, a2: bFun, a3: iFun, a4: lFun): bFun = bFunDummy
  def helperIntBooleanIntDouble2Boolean(a1: iFun, a2: bFun, a3: iFun, a4: dFun): bFun = bFunDummy
  def helperIntBooleanIntString2Boolean(a1: iFun, a2: bFun, a3: iFun, a4: sFun): bFun = bFunDummy
  def helperIntBooleanIntBoolean2Boolean(a1: iFun, a2: bFun, a3: iFun, a4: bFun): bFun = bFunDummy
  def helperIntBooleanLongInt2Boolean(a1: iFun, a2: bFun, a3: lFun, a4: iFun): bFun = bFunDummy
  def helperIntBooleanLongLong2Boolean(a1: iFun, a2: bFun, a3: lFun, a4: lFun): bFun = bFunDummy
  def helperIntBooleanLongDouble2Boolean(a1: iFun, a2: bFun, a3: lFun, a4: dFun): bFun = bFunDummy
  def helperIntBooleanLongString2Boolean(a1: iFun, a2: bFun, a3: lFun, a4: sFun): bFun = bFunDummy
  def helperIntBooleanLongBoolean2Boolean(a1: iFun, a2: bFun, a3: lFun, a4: bFun): bFun = bFunDummy
  def helperIntBooleanDoubleInt2Boolean(a1: iFun, a2: bFun, a3: dFun, a4: iFun): bFun = bFunDummy
  def helperIntBooleanDoubleLong2Boolean(a1: iFun, a2: bFun, a3: dFun, a4: lFun): bFun = bFunDummy
  def helperIntBooleanDoubleDouble2Boolean(a1: iFun, a2: bFun, a3: dFun, a4: dFun): bFun = bFunDummy
  def helperIntBooleanDoubleString2Boolean(a1: iFun, a2: bFun, a3: dFun, a4: sFun): bFun = bFunDummy
  def helperIntBooleanDoubleBoolean2Boolean(a1: iFun, a2: bFun, a3: dFun, a4: bFun): bFun = bFunDummy
  def helperIntBooleanStringInt2Boolean(a1: iFun, a2: bFun, a3: sFun, a4: iFun): bFun = bFunDummy
  def helperIntBooleanStringLong2Boolean(a1: iFun, a2: bFun, a3: sFun, a4: lFun): bFun = bFunDummy
  def helperIntBooleanStringDouble2Boolean(a1: iFun, a2: bFun, a3: sFun, a4: dFun): bFun = bFunDummy
  def helperIntBooleanStringString2Boolean(a1: iFun, a2: bFun, a3: sFun, a4: sFun): bFun = bFunDummy
  def helperIntBooleanStringBoolean2Boolean(a1: iFun, a2: bFun, a3: sFun, a4: bFun): bFun = bFunDummy
  def helperIntBooleanBooleanInt2Boolean(a1: iFun, a2: bFun, a3: bFun, a4: iFun): bFun = bFunDummy
  def helperIntBooleanBooleanLong2Boolean(a1: iFun, a2: bFun, a3: bFun, a4: lFun): bFun = bFunDummy
  def helperIntBooleanBooleanDouble2Boolean(a1: iFun, a2: bFun, a3: bFun, a4: dFun): bFun = bFunDummy
  def helperIntBooleanBooleanString2Boolean(a1: iFun, a2: bFun, a3: bFun, a4: sFun): bFun = bFunDummy
  def helperIntBooleanBooleanBoolean2Boolean(a1: iFun, a2: bFun, a3: bFun, a4: bFun): bFun = bFunDummy
  def helperLongIntIntInt2Boolean(a1: lFun, a2: iFun, a3: iFun, a4: iFun): bFun = bFunDummy
  def helperLongIntIntLong2Boolean(a1: lFun, a2: iFun, a3: iFun, a4: lFun): bFun = bFunDummy
  def helperLongIntIntDouble2Boolean(a1: lFun, a2: iFun, a3: iFun, a4: dFun): bFun = bFunDummy
  def helperLongIntIntString2Boolean(a1: lFun, a2: iFun, a3: iFun, a4: sFun): bFun = bFunDummy
  def helperLongIntIntBoolean2Boolean(a1: lFun, a2: iFun, a3: iFun, a4: bFun): bFun = bFunDummy
  def helperLongIntLongInt2Boolean(a1: lFun, a2: iFun, a3: lFun, a4: iFun): bFun = bFunDummy
  def helperLongIntLongLong2Boolean(a1: lFun, a2: iFun, a3: lFun, a4: lFun): bFun = bFunDummy
  def helperLongIntLongDouble2Boolean(a1: lFun, a2: iFun, a3: lFun, a4: dFun): bFun = bFunDummy
  def helperLongIntLongString2Boolean(a1: lFun, a2: iFun, a3: lFun, a4: sFun): bFun = bFunDummy
  def helperLongIntLongBoolean2Boolean(a1: lFun, a2: iFun, a3: lFun, a4: bFun): bFun = bFunDummy
  def helperLongIntDoubleInt2Boolean(a1: lFun, a2: iFun, a3: dFun, a4: iFun): bFun = bFunDummy
  def helperLongIntDoubleLong2Boolean(a1: lFun, a2: iFun, a3: dFun, a4: lFun): bFun = bFunDummy
  def helperLongIntDoubleDouble2Boolean(a1: lFun, a2: iFun, a3: dFun, a4: dFun): bFun = bFunDummy
  def helperLongIntDoubleString2Boolean(a1: lFun, a2: iFun, a3: dFun, a4: sFun): bFun = bFunDummy
  def helperLongIntDoubleBoolean2Boolean(a1: lFun, a2: iFun, a3: dFun, a4: bFun): bFun = bFunDummy
  def helperLongIntStringInt2Boolean(a1: lFun, a2: iFun, a3: sFun, a4: iFun): bFun = bFunDummy
  def helperLongIntStringLong2Boolean(a1: lFun, a2: iFun, a3: sFun, a4: lFun): bFun = bFunDummy
  def helperLongIntStringDouble2Boolean(a1: lFun, a2: iFun, a3: sFun, a4: dFun): bFun = bFunDummy
  def helperLongIntStringString2Boolean(a1: lFun, a2: iFun, a3: sFun, a4: sFun): bFun = bFunDummy
  def helperLongIntStringBoolean2Boolean(a1: lFun, a2: iFun, a3: sFun, a4: bFun): bFun = bFunDummy
  def helperLongIntBooleanInt2Boolean(a1: lFun, a2: iFun, a3: bFun, a4: iFun): bFun = bFunDummy
  def helperLongIntBooleanLong2Boolean(a1: lFun, a2: iFun, a3: bFun, a4: lFun): bFun = bFunDummy
  def helperLongIntBooleanDouble2Boolean(a1: lFun, a2: iFun, a3: bFun, a4: dFun): bFun = bFunDummy
  def helperLongIntBooleanString2Boolean(a1: lFun, a2: iFun, a3: bFun, a4: sFun): bFun = bFunDummy
  def helperLongIntBooleanBoolean2Boolean(a1: lFun, a2: iFun, a3: bFun, a4: bFun): bFun = bFunDummy
  def helperLongLongIntInt2Boolean(a1: lFun, a2: lFun, a3: iFun, a4: iFun): bFun = bFunDummy
  def helperLongLongIntLong2Boolean(a1: lFun, a2: lFun, a3: iFun, a4: lFun): bFun = bFunDummy
  def helperLongLongIntDouble2Boolean(a1: lFun, a2: lFun, a3: iFun, a4: dFun): bFun = bFunDummy
  def helperLongLongIntString2Boolean(a1: lFun, a2: lFun, a3: iFun, a4: sFun): bFun = bFunDummy
  def helperLongLongIntBoolean2Boolean(a1: lFun, a2: lFun, a3: iFun, a4: bFun): bFun = bFunDummy
  def helperLongLongLongInt2Boolean(a1: lFun, a2: lFun, a3: lFun, a4: iFun): bFun = bFunDummy
  def helperLongLongLongLong2Boolean(a1: lFun, a2: lFun, a3: lFun, a4: lFun): bFun = bFunDummy
  def helperLongLongLongDouble2Boolean(a1: lFun, a2: lFun, a3: lFun, a4: dFun): bFun = bFunDummy
  def helperLongLongLongString2Boolean(a1: lFun, a2: lFun, a3: lFun, a4: sFun): bFun = bFunDummy
  def helperLongLongLongBoolean2Boolean(a1: lFun, a2: lFun, a3: lFun, a4: bFun): bFun = bFunDummy
  def helperLongLongDoubleInt2Boolean(a1: lFun, a2: lFun, a3: dFun, a4: iFun): bFun = bFunDummy
  def helperLongLongDoubleLong2Boolean(a1: lFun, a2: lFun, a3: dFun, a4: lFun): bFun = bFunDummy
  def helperLongLongDoubleDouble2Boolean(a1: lFun, a2: lFun, a3: dFun, a4: dFun): bFun = bFunDummy
  def helperLongLongDoubleString2Boolean(a1: lFun, a2: lFun, a3: dFun, a4: sFun): bFun = bFunDummy
  def helperLongLongDoubleBoolean2Boolean(a1: lFun, a2: lFun, a3: dFun, a4: bFun): bFun = bFunDummy
  def helperLongLongStringInt2Boolean(a1: lFun, a2: lFun, a3: sFun, a4: iFun): bFun = bFunDummy
  def helperLongLongStringLong2Boolean(a1: lFun, a2: lFun, a3: sFun, a4: lFun): bFun = bFunDummy
  def helperLongLongStringDouble2Boolean(a1: lFun, a2: lFun, a3: sFun, a4: dFun): bFun = bFunDummy
  def helperLongLongStringString2Boolean(a1: lFun, a2: lFun, a3: sFun, a4: sFun): bFun = bFunDummy
  def helperLongLongStringBoolean2Boolean(a1: lFun, a2: lFun, a3: sFun, a4: bFun): bFun = bFunDummy
  def helperLongLongBooleanInt2Boolean(a1: lFun, a2: lFun, a3: bFun, a4: iFun): bFun = bFunDummy
  def helperLongLongBooleanLong2Boolean(a1: lFun, a2: lFun, a3: bFun, a4: lFun): bFun = bFunDummy
  def helperLongLongBooleanDouble2Boolean(a1: lFun, a2: lFun, a3: bFun, a4: dFun): bFun = bFunDummy
  def helperLongLongBooleanString2Boolean(a1: lFun, a2: lFun, a3: bFun, a4: sFun): bFun = bFunDummy
  def helperLongLongBooleanBoolean2Boolean(a1: lFun, a2: lFun, a3: bFun, a4: bFun): bFun = bFunDummy
  def helperLongDoubleIntInt2Boolean(a1: lFun, a2: dFun, a3: iFun, a4: iFun): bFun = bFunDummy
  def helperLongDoubleIntLong2Boolean(a1: lFun, a2: dFun, a3: iFun, a4: lFun): bFun = bFunDummy
  def helperLongDoubleIntDouble2Boolean(a1: lFun, a2: dFun, a3: iFun, a4: dFun): bFun = bFunDummy
  def helperLongDoubleIntString2Boolean(a1: lFun, a2: dFun, a3: iFun, a4: sFun): bFun = bFunDummy
  def helperLongDoubleIntBoolean2Boolean(a1: lFun, a2: dFun, a3: iFun, a4: bFun): bFun = bFunDummy
  def helperLongDoubleLongInt2Boolean(a1: lFun, a2: dFun, a3: lFun, a4: iFun): bFun = bFunDummy
  def helperLongDoubleLongLong2Boolean(a1: lFun, a2: dFun, a3: lFun, a4: lFun): bFun = bFunDummy
  def helperLongDoubleLongDouble2Boolean(a1: lFun, a2: dFun, a3: lFun, a4: dFun): bFun = bFunDummy
  def helperLongDoubleLongString2Boolean(a1: lFun, a2: dFun, a3: lFun, a4: sFun): bFun = bFunDummy
  def helperLongDoubleLongBoolean2Boolean(a1: lFun, a2: dFun, a3: lFun, a4: bFun): bFun = bFunDummy
  def helperLongDoubleDoubleInt2Boolean(a1: lFun, a2: dFun, a3: dFun, a4: iFun): bFun = bFunDummy
  def helperLongDoubleDoubleLong2Boolean(a1: lFun, a2: dFun, a3: dFun, a4: lFun): bFun = bFunDummy
  def helperLongDoubleDoubleDouble2Boolean(a1: lFun, a2: dFun, a3: dFun, a4: dFun): bFun = bFunDummy
  def helperLongDoubleDoubleString2Boolean(a1: lFun, a2: dFun, a3: dFun, a4: sFun): bFun = bFunDummy
  def helperLongDoubleDoubleBoolean2Boolean(a1: lFun, a2: dFun, a3: dFun, a4: bFun): bFun = bFunDummy
  def helperLongDoubleStringInt2Boolean(a1: lFun, a2: dFun, a3: sFun, a4: iFun): bFun = bFunDummy
  def helperLongDoubleStringLong2Boolean(a1: lFun, a2: dFun, a3: sFun, a4: lFun): bFun = bFunDummy
  def helperLongDoubleStringDouble2Boolean(a1: lFun, a2: dFun, a3: sFun, a4: dFun): bFun = bFunDummy
  def helperLongDoubleStringString2Boolean(a1: lFun, a2: dFun, a3: sFun, a4: sFun): bFun = bFunDummy
  def helperLongDoubleStringBoolean2Boolean(a1: lFun, a2: dFun, a3: sFun, a4: bFun): bFun = bFunDummy
  def helperLongDoubleBooleanInt2Boolean(a1: lFun, a2: dFun, a3: bFun, a4: iFun): bFun = bFunDummy
  def helperLongDoubleBooleanLong2Boolean(a1: lFun, a2: dFun, a3: bFun, a4: lFun): bFun = bFunDummy
  def helperLongDoubleBooleanDouble2Boolean(a1: lFun, a2: dFun, a3: bFun, a4: dFun): bFun = bFunDummy
  def helperLongDoubleBooleanString2Boolean(a1: lFun, a2: dFun, a3: bFun, a4: sFun): bFun = bFunDummy
  def helperLongDoubleBooleanBoolean2Boolean(a1: lFun, a2: dFun, a3: bFun, a4: bFun): bFun = bFunDummy
  def helperLongStringIntInt2Boolean(a1: lFun, a2: sFun, a3: iFun, a4: iFun): bFun = bFunDummy
  def helperLongStringIntLong2Boolean(a1: lFun, a2: sFun, a3: iFun, a4: lFun): bFun = bFunDummy
  def helperLongStringIntDouble2Boolean(a1: lFun, a2: sFun, a3: iFun, a4: dFun): bFun = bFunDummy
  def helperLongStringIntString2Boolean(a1: lFun, a2: sFun, a3: iFun, a4: sFun): bFun = bFunDummy
  def helperLongStringIntBoolean2Boolean(a1: lFun, a2: sFun, a3: iFun, a4: bFun): bFun = bFunDummy
  def helperLongStringLongInt2Boolean(a1: lFun, a2: sFun, a3: lFun, a4: iFun): bFun = bFunDummy
  def helperLongStringLongLong2Boolean(a1: lFun, a2: sFun, a3: lFun, a4: lFun): bFun = bFunDummy
  def helperLongStringLongDouble2Boolean(a1: lFun, a2: sFun, a3: lFun, a4: dFun): bFun = bFunDummy
  def helperLongStringLongString2Boolean(a1: lFun, a2: sFun, a3: lFun, a4: sFun): bFun = bFunDummy
  def helperLongStringLongBoolean2Boolean(a1: lFun, a2: sFun, a3: lFun, a4: bFun): bFun = bFunDummy
  def helperLongStringDoubleInt2Boolean(a1: lFun, a2: sFun, a3: dFun, a4: iFun): bFun = bFunDummy
  def helperLongStringDoubleLong2Boolean(a1: lFun, a2: sFun, a3: dFun, a4: lFun): bFun = bFunDummy
  def helperLongStringDoubleDouble2Boolean(a1: lFun, a2: sFun, a3: dFun, a4: dFun): bFun = bFunDummy
  def helperLongStringDoubleString2Boolean(a1: lFun, a2: sFun, a3: dFun, a4: sFun): bFun = bFunDummy
  def helperLongStringDoubleBoolean2Boolean(a1: lFun, a2: sFun, a3: dFun, a4: bFun): bFun = bFunDummy
  def helperLongStringStringInt2Boolean(a1: lFun, a2: sFun, a3: sFun, a4: iFun): bFun = bFunDummy
  def helperLongStringStringLong2Boolean(a1: lFun, a2: sFun, a3: sFun, a4: lFun): bFun = bFunDummy
  def helperLongStringStringDouble2Boolean(a1: lFun, a2: sFun, a3: sFun, a4: dFun): bFun = bFunDummy
  def helperLongStringStringString2Boolean(a1: lFun, a2: sFun, a3: sFun, a4: sFun): bFun = bFunDummy
  def helperLongStringStringBoolean2Boolean(a1: lFun, a2: sFun, a3: sFun, a4: bFun): bFun = bFunDummy
  def helperLongStringBooleanInt2Boolean(a1: lFun, a2: sFun, a3: bFun, a4: iFun): bFun = bFunDummy
  def helperLongStringBooleanLong2Boolean(a1: lFun, a2: sFun, a3: bFun, a4: lFun): bFun = bFunDummy
  def helperLongStringBooleanDouble2Boolean(a1: lFun, a2: sFun, a3: bFun, a4: dFun): bFun = bFunDummy
  def helperLongStringBooleanString2Boolean(a1: lFun, a2: sFun, a3: bFun, a4: sFun): bFun = bFunDummy
  def helperLongStringBooleanBoolean2Boolean(a1: lFun, a2: sFun, a3: bFun, a4: bFun): bFun = bFunDummy
  def helperLongBooleanIntInt2Boolean(a1: lFun, a2: bFun, a3: iFun, a4: iFun): bFun = bFunDummy
  def helperLongBooleanIntLong2Boolean(a1: lFun, a2: bFun, a3: iFun, a4: lFun): bFun = bFunDummy
  def helperLongBooleanIntDouble2Boolean(a1: lFun, a2: bFun, a3: iFun, a4: dFun): bFun = bFunDummy
  def helperLongBooleanIntString2Boolean(a1: lFun, a2: bFun, a3: iFun, a4: sFun): bFun = bFunDummy
  def helperLongBooleanIntBoolean2Boolean(a1: lFun, a2: bFun, a3: iFun, a4: bFun): bFun = bFunDummy
  def helperLongBooleanLongInt2Boolean(a1: lFun, a2: bFun, a3: lFun, a4: iFun): bFun = bFunDummy
  def helperLongBooleanLongLong2Boolean(a1: lFun, a2: bFun, a3: lFun, a4: lFun): bFun = bFunDummy
  def helperLongBooleanLongDouble2Boolean(a1: lFun, a2: bFun, a3: lFun, a4: dFun): bFun = bFunDummy
  def helperLongBooleanLongString2Boolean(a1: lFun, a2: bFun, a3: lFun, a4: sFun): bFun = bFunDummy
  def helperLongBooleanLongBoolean2Boolean(a1: lFun, a2: bFun, a3: lFun, a4: bFun): bFun = bFunDummy
  def helperLongBooleanDoubleInt2Boolean(a1: lFun, a2: bFun, a3: dFun, a4: iFun): bFun = bFunDummy
  def helperLongBooleanDoubleLong2Boolean(a1: lFun, a2: bFun, a3: dFun, a4: lFun): bFun = bFunDummy
  def helperLongBooleanDoubleDouble2Boolean(a1: lFun, a2: bFun, a3: dFun, a4: dFun): bFun = bFunDummy
  def helperLongBooleanDoubleString2Boolean(a1: lFun, a2: bFun, a3: dFun, a4: sFun): bFun = bFunDummy
  def helperLongBooleanDoubleBoolean2Boolean(a1: lFun, a2: bFun, a3: dFun, a4: bFun): bFun = bFunDummy
  def helperLongBooleanStringInt2Boolean(a1: lFun, a2: bFun, a3: sFun, a4: iFun): bFun = bFunDummy
  def helperLongBooleanStringLong2Boolean(a1: lFun, a2: bFun, a3: sFun, a4: lFun): bFun = bFunDummy
  def helperLongBooleanStringDouble2Boolean(a1: lFun, a2: bFun, a3: sFun, a4: dFun): bFun = bFunDummy
  def helperLongBooleanStringString2Boolean(a1: lFun, a2: bFun, a3: sFun, a4: sFun): bFun = bFunDummy
  def helperLongBooleanStringBoolean2Boolean(a1: lFun, a2: bFun, a3: sFun, a4: bFun): bFun = bFunDummy
  def helperLongBooleanBooleanInt2Boolean(a1: lFun, a2: bFun, a3: bFun, a4: iFun): bFun = bFunDummy
  def helperLongBooleanBooleanLong2Boolean(a1: lFun, a2: bFun, a3: bFun, a4: lFun): bFun = bFunDummy
  def helperLongBooleanBooleanDouble2Boolean(a1: lFun, a2: bFun, a3: bFun, a4: dFun): bFun = bFunDummy
  def helperLongBooleanBooleanString2Boolean(a1: lFun, a2: bFun, a3: bFun, a4: sFun): bFun = bFunDummy
  def helperLongBooleanBooleanBoolean2Boolean(a1: lFun, a2: bFun, a3: bFun, a4: bFun): bFun = bFunDummy
  def helperDoubleIntIntInt2Boolean(a1: dFun, a2: iFun, a3: iFun, a4: iFun): bFun = bFunDummy
  def helperDoubleIntIntLong2Boolean(a1: dFun, a2: iFun, a3: iFun, a4: lFun): bFun = bFunDummy
  def helperDoubleIntIntDouble2Boolean(a1: dFun, a2: iFun, a3: iFun, a4: dFun): bFun = bFunDummy
  def helperDoubleIntIntString2Boolean(a1: dFun, a2: iFun, a3: iFun, a4: sFun): bFun = bFunDummy
  def helperDoubleIntIntBoolean2Boolean(a1: dFun, a2: iFun, a3: iFun, a4: bFun): bFun = bFunDummy
  def helperDoubleIntLongInt2Boolean(a1: dFun, a2: iFun, a3: lFun, a4: iFun): bFun = bFunDummy
  def helperDoubleIntLongLong2Boolean(a1: dFun, a2: iFun, a3: lFun, a4: lFun): bFun = bFunDummy
  def helperDoubleIntLongDouble2Boolean(a1: dFun, a2: iFun, a3: lFun, a4: dFun): bFun = bFunDummy
  def helperDoubleIntLongString2Boolean(a1: dFun, a2: iFun, a3: lFun, a4: sFun): bFun = bFunDummy
  def helperDoubleIntLongBoolean2Boolean(a1: dFun, a2: iFun, a3: lFun, a4: bFun): bFun = bFunDummy
  def helperDoubleIntDoubleInt2Boolean(a1: dFun, a2: iFun, a3: dFun, a4: iFun): bFun = bFunDummy
  def helperDoubleIntDoubleLong2Boolean(a1: dFun, a2: iFun, a3: dFun, a4: lFun): bFun = bFunDummy
  def helperDoubleIntDoubleDouble2Boolean(a1: dFun, a2: iFun, a3: dFun, a4: dFun): bFun = bFunDummy
  def helperDoubleIntDoubleString2Boolean(a1: dFun, a2: iFun, a3: dFun, a4: sFun): bFun = bFunDummy
  def helperDoubleIntDoubleBoolean2Boolean(a1: dFun, a2: iFun, a3: dFun, a4: bFun): bFun = bFunDummy
  def helperDoubleIntStringInt2Boolean(a1: dFun, a2: iFun, a3: sFun, a4: iFun): bFun = bFunDummy
  def helperDoubleIntStringLong2Boolean(a1: dFun, a2: iFun, a3: sFun, a4: lFun): bFun = bFunDummy
  def helperDoubleIntStringDouble2Boolean(a1: dFun, a2: iFun, a3: sFun, a4: dFun): bFun = bFunDummy
  def helperDoubleIntStringString2Boolean(a1: dFun, a2: iFun, a3: sFun, a4: sFun): bFun = bFunDummy
  def helperDoubleIntStringBoolean2Boolean(a1: dFun, a2: iFun, a3: sFun, a4: bFun): bFun = bFunDummy
  def helperDoubleIntBooleanInt2Boolean(a1: dFun, a2: iFun, a3: bFun, a4: iFun): bFun = bFunDummy
  def helperDoubleIntBooleanLong2Boolean(a1: dFun, a2: iFun, a3: bFun, a4: lFun): bFun = bFunDummy
  def helperDoubleIntBooleanDouble2Boolean(a1: dFun, a2: iFun, a3: bFun, a4: dFun): bFun = bFunDummy
  def helperDoubleIntBooleanString2Boolean(a1: dFun, a2: iFun, a3: bFun, a4: sFun): bFun = bFunDummy
  def helperDoubleIntBooleanBoolean2Boolean(a1: dFun, a2: iFun, a3: bFun, a4: bFun): bFun = bFunDummy
  def helperDoubleLongIntInt2Boolean(a1: dFun, a2: lFun, a3: iFun, a4: iFun): bFun = bFunDummy
  def helperDoubleLongIntLong2Boolean(a1: dFun, a2: lFun, a3: iFun, a4: lFun): bFun = bFunDummy
  def helperDoubleLongIntDouble2Boolean(a1: dFun, a2: lFun, a3: iFun, a4: dFun): bFun = bFunDummy
  def helperDoubleLongIntString2Boolean(a1: dFun, a2: lFun, a3: iFun, a4: sFun): bFun = bFunDummy
  def helperDoubleLongIntBoolean2Boolean(a1: dFun, a2: lFun, a3: iFun, a4: bFun): bFun = bFunDummy
  def helperDoubleLongLongInt2Boolean(a1: dFun, a2: lFun, a3: lFun, a4: iFun): bFun = bFunDummy
  def helperDoubleLongLongLong2Boolean(a1: dFun, a2: lFun, a3: lFun, a4: lFun): bFun = bFunDummy
  def helperDoubleLongLongDouble2Boolean(a1: dFun, a2: lFun, a3: lFun, a4: dFun): bFun = bFunDummy
  def helperDoubleLongLongString2Boolean(a1: dFun, a2: lFun, a3: lFun, a4: sFun): bFun = bFunDummy
  def helperDoubleLongLongBoolean2Boolean(a1: dFun, a2: lFun, a3: lFun, a4: bFun): bFun = bFunDummy
  def helperDoubleLongDoubleInt2Boolean(a1: dFun, a2: lFun, a3: dFun, a4: iFun): bFun = bFunDummy
  def helperDoubleLongDoubleLong2Boolean(a1: dFun, a2: lFun, a3: dFun, a4: lFun): bFun = bFunDummy
  def helperDoubleLongDoubleDouble2Boolean(a1: dFun, a2: lFun, a3: dFun, a4: dFun): bFun = bFunDummy
  def helperDoubleLongDoubleString2Boolean(a1: dFun, a2: lFun, a3: dFun, a4: sFun): bFun = bFunDummy
  def helperDoubleLongDoubleBoolean2Boolean(a1: dFun, a2: lFun, a3: dFun, a4: bFun): bFun = bFunDummy
  def helperDoubleLongStringInt2Boolean(a1: dFun, a2: lFun, a3: sFun, a4: iFun): bFun = bFunDummy
  def helperDoubleLongStringLong2Boolean(a1: dFun, a2: lFun, a3: sFun, a4: lFun): bFun = bFunDummy
  def helperDoubleLongStringDouble2Boolean(a1: dFun, a2: lFun, a3: sFun, a4: dFun): bFun = bFunDummy
  def helperDoubleLongStringString2Boolean(a1: dFun, a2: lFun, a3: sFun, a4: sFun): bFun = bFunDummy
  def helperDoubleLongStringBoolean2Boolean(a1: dFun, a2: lFun, a3: sFun, a4: bFun): bFun = bFunDummy
  def helperDoubleLongBooleanInt2Boolean(a1: dFun, a2: lFun, a3: bFun, a4: iFun): bFun = bFunDummy
  def helperDoubleLongBooleanLong2Boolean(a1: dFun, a2: lFun, a3: bFun, a4: lFun): bFun = bFunDummy
  def helperDoubleLongBooleanDouble2Boolean(a1: dFun, a2: lFun, a3: bFun, a4: dFun): bFun = bFunDummy
  def helperDoubleLongBooleanString2Boolean(a1: dFun, a2: lFun, a3: bFun, a4: sFun): bFun = bFunDummy
  def helperDoubleLongBooleanBoolean2Boolean(a1: dFun, a2: lFun, a3: bFun, a4: bFun): bFun = bFunDummy
  def helperDoubleDoubleIntInt2Boolean(a1: dFun, a2: dFun, a3: iFun, a4: iFun): bFun = bFunDummy
  def helperDoubleDoubleIntLong2Boolean(a1: dFun, a2: dFun, a3: iFun, a4: lFun): bFun = bFunDummy
  def helperDoubleDoubleIntDouble2Boolean(a1: dFun, a2: dFun, a3: iFun, a4: dFun): bFun = bFunDummy
  def helperDoubleDoubleIntString2Boolean(a1: dFun, a2: dFun, a3: iFun, a4: sFun): bFun = bFunDummy
  def helperDoubleDoubleIntBoolean2Boolean(a1: dFun, a2: dFun, a3: iFun, a4: bFun): bFun = bFunDummy
  def helperDoubleDoubleLongInt2Boolean(a1: dFun, a2: dFun, a3: lFun, a4: iFun): bFun = bFunDummy
  def helperDoubleDoubleLongLong2Boolean(a1: dFun, a2: dFun, a3: lFun, a4: lFun): bFun = bFunDummy
  def helperDoubleDoubleLongDouble2Boolean(a1: dFun, a2: dFun, a3: lFun, a4: dFun): bFun = bFunDummy
  def helperDoubleDoubleLongString2Boolean(a1: dFun, a2: dFun, a3: lFun, a4: sFun): bFun = bFunDummy
  def helperDoubleDoubleLongBoolean2Boolean(a1: dFun, a2: dFun, a3: lFun, a4: bFun): bFun = bFunDummy
  def helperDoubleDoubleDoubleInt2Boolean(a1: dFun, a2: dFun, a3: dFun, a4: iFun): bFun = bFunDummy
  def helperDoubleDoubleDoubleLong2Boolean(a1: dFun, a2: dFun, a3: dFun, a4: lFun): bFun = bFunDummy
  def helperDoubleDoubleDoubleDouble2Boolean(a1: dFun, a2: dFun, a3: dFun, a4: dFun): bFun = bFunDummy
  def helperDoubleDoubleDoubleString2Boolean(a1: dFun, a2: dFun, a3: dFun, a4: sFun): bFun = bFunDummy
  def helperDoubleDoubleDoubleBoolean2Boolean(a1: dFun, a2: dFun, a3: dFun, a4: bFun): bFun = bFunDummy
  def helperDoubleDoubleStringInt2Boolean(a1: dFun, a2: dFun, a3: sFun, a4: iFun): bFun = bFunDummy
  def helperDoubleDoubleStringLong2Boolean(a1: dFun, a2: dFun, a3: sFun, a4: lFun): bFun = bFunDummy
  def helperDoubleDoubleStringDouble2Boolean(a1: dFun, a2: dFun, a3: sFun, a4: dFun): bFun = bFunDummy
  def helperDoubleDoubleStringString2Boolean(a1: dFun, a2: dFun, a3: sFun, a4: sFun): bFun = bFunDummy
  def helperDoubleDoubleStringBoolean2Boolean(a1: dFun, a2: dFun, a3: sFun, a4: bFun): bFun = bFunDummy
  def helperDoubleDoubleBooleanInt2Boolean(a1: dFun, a2: dFun, a3: bFun, a4: iFun): bFun = bFunDummy
  def helperDoubleDoubleBooleanLong2Boolean(a1: dFun, a2: dFun, a3: bFun, a4: lFun): bFun = bFunDummy
  def helperDoubleDoubleBooleanDouble2Boolean(a1: dFun, a2: dFun, a3: bFun, a4: dFun): bFun = bFunDummy
  def helperDoubleDoubleBooleanString2Boolean(a1: dFun, a2: dFun, a3: bFun, a4: sFun): bFun = bFunDummy
  def helperDoubleDoubleBooleanBoolean2Boolean(a1: dFun, a2: dFun, a3: bFun, a4: bFun): bFun = bFunDummy
  def helperDoubleStringIntInt2Boolean(a1: dFun, a2: sFun, a3: iFun, a4: iFun): bFun = bFunDummy
  def helperDoubleStringIntLong2Boolean(a1: dFun, a2: sFun, a3: iFun, a4: lFun): bFun = bFunDummy
  def helperDoubleStringIntDouble2Boolean(a1: dFun, a2: sFun, a3: iFun, a4: dFun): bFun = bFunDummy
  def helperDoubleStringIntString2Boolean(a1: dFun, a2: sFun, a3: iFun, a4: sFun): bFun = bFunDummy
  def helperDoubleStringIntBoolean2Boolean(a1: dFun, a2: sFun, a3: iFun, a4: bFun): bFun = bFunDummy
  def helperDoubleStringLongInt2Boolean(a1: dFun, a2: sFun, a3: lFun, a4: iFun): bFun = bFunDummy
  def helperDoubleStringLongLong2Boolean(a1: dFun, a2: sFun, a3: lFun, a4: lFun): bFun = bFunDummy
  def helperDoubleStringLongDouble2Boolean(a1: dFun, a2: sFun, a3: lFun, a4: dFun): bFun = bFunDummy
  def helperDoubleStringLongString2Boolean(a1: dFun, a2: sFun, a3: lFun, a4: sFun): bFun = bFunDummy
  def helperDoubleStringLongBoolean2Boolean(a1: dFun, a2: sFun, a3: lFun, a4: bFun): bFun = bFunDummy
  def helperDoubleStringDoubleInt2Boolean(a1: dFun, a2: sFun, a3: dFun, a4: iFun): bFun = bFunDummy
  def helperDoubleStringDoubleLong2Boolean(a1: dFun, a2: sFun, a3: dFun, a4: lFun): bFun = bFunDummy
  def helperDoubleStringDoubleDouble2Boolean(a1: dFun, a2: sFun, a3: dFun, a4: dFun): bFun = bFunDummy
  def helperDoubleStringDoubleString2Boolean(a1: dFun, a2: sFun, a3: dFun, a4: sFun): bFun = bFunDummy
  def helperDoubleStringDoubleBoolean2Boolean(a1: dFun, a2: sFun, a3: dFun, a4: bFun): bFun = bFunDummy
  def helperDoubleStringStringInt2Boolean(a1: dFun, a2: sFun, a3: sFun, a4: iFun): bFun = bFunDummy
  def helperDoubleStringStringLong2Boolean(a1: dFun, a2: sFun, a3: sFun, a4: lFun): bFun = bFunDummy
  def helperDoubleStringStringDouble2Boolean(a1: dFun, a2: sFun, a3: sFun, a4: dFun): bFun = bFunDummy
  def helperDoubleStringStringString2Boolean(a1: dFun, a2: sFun, a3: sFun, a4: sFun): bFun = bFunDummy
  def helperDoubleStringStringBoolean2Boolean(a1: dFun, a2: sFun, a3: sFun, a4: bFun): bFun = bFunDummy
  def helperDoubleStringBooleanInt2Boolean(a1: dFun, a2: sFun, a3: bFun, a4: iFun): bFun = bFunDummy
  def helperDoubleStringBooleanLong2Boolean(a1: dFun, a2: sFun, a3: bFun, a4: lFun): bFun = bFunDummy
  def helperDoubleStringBooleanDouble2Boolean(a1: dFun, a2: sFun, a3: bFun, a4: dFun): bFun = bFunDummy
  def helperDoubleStringBooleanString2Boolean(a1: dFun, a2: sFun, a3: bFun, a4: sFun): bFun = bFunDummy
  def helperDoubleStringBooleanBoolean2Boolean(a1: dFun, a2: sFun, a3: bFun, a4: bFun): bFun = bFunDummy
  def helperDoubleBooleanIntInt2Boolean(a1: dFun, a2: bFun, a3: iFun, a4: iFun): bFun = bFunDummy
  def helperDoubleBooleanIntLong2Boolean(a1: dFun, a2: bFun, a3: iFun, a4: lFun): bFun = bFunDummy
  def helperDoubleBooleanIntDouble2Boolean(a1: dFun, a2: bFun, a3: iFun, a4: dFun): bFun = bFunDummy
  def helperDoubleBooleanIntString2Boolean(a1: dFun, a2: bFun, a3: iFun, a4: sFun): bFun = bFunDummy
  def helperDoubleBooleanIntBoolean2Boolean(a1: dFun, a2: bFun, a3: iFun, a4: bFun): bFun = bFunDummy
  def helperDoubleBooleanLongInt2Boolean(a1: dFun, a2: bFun, a3: lFun, a4: iFun): bFun = bFunDummy
  def helperDoubleBooleanLongLong2Boolean(a1: dFun, a2: bFun, a3: lFun, a4: lFun): bFun = bFunDummy
  def helperDoubleBooleanLongDouble2Boolean(a1: dFun, a2: bFun, a3: lFun, a4: dFun): bFun = bFunDummy
  def helperDoubleBooleanLongString2Boolean(a1: dFun, a2: bFun, a3: lFun, a4: sFun): bFun = bFunDummy
  def helperDoubleBooleanLongBoolean2Boolean(a1: dFun, a2: bFun, a3: lFun, a4: bFun): bFun = bFunDummy
  def helperDoubleBooleanDoubleInt2Boolean(a1: dFun, a2: bFun, a3: dFun, a4: iFun): bFun = bFunDummy
  def helperDoubleBooleanDoubleLong2Boolean(a1: dFun, a2: bFun, a3: dFun, a4: lFun): bFun = bFunDummy
  def helperDoubleBooleanDoubleDouble2Boolean(a1: dFun, a2: bFun, a3: dFun, a4: dFun): bFun = bFunDummy
  def helperDoubleBooleanDoubleString2Boolean(a1: dFun, a2: bFun, a3: dFun, a4: sFun): bFun = bFunDummy
  def helperDoubleBooleanDoubleBoolean2Boolean(a1: dFun, a2: bFun, a3: dFun, a4: bFun): bFun = bFunDummy
  def helperDoubleBooleanStringInt2Boolean(a1: dFun, a2: bFun, a3: sFun, a4: iFun): bFun = bFunDummy
  def helperDoubleBooleanStringLong2Boolean(a1: dFun, a2: bFun, a3: sFun, a4: lFun): bFun = bFunDummy
  def helperDoubleBooleanStringDouble2Boolean(a1: dFun, a2: bFun, a3: sFun, a4: dFun): bFun = bFunDummy
  def helperDoubleBooleanStringString2Boolean(a1: dFun, a2: bFun, a3: sFun, a4: sFun): bFun = bFunDummy
  def helperDoubleBooleanStringBoolean2Boolean(a1: dFun, a2: bFun, a3: sFun, a4: bFun): bFun = bFunDummy
  def helperDoubleBooleanBooleanInt2Boolean(a1: dFun, a2: bFun, a3: bFun, a4: iFun): bFun = bFunDummy
  def helperDoubleBooleanBooleanLong2Boolean(a1: dFun, a2: bFun, a3: bFun, a4: lFun): bFun = bFunDummy
  def helperDoubleBooleanBooleanDouble2Boolean(a1: dFun, a2: bFun, a3: bFun, a4: dFun): bFun = bFunDummy
  def helperDoubleBooleanBooleanString2Boolean(a1: dFun, a2: bFun, a3: bFun, a4: sFun): bFun = bFunDummy
  def helperDoubleBooleanBooleanBoolean2Boolean(a1: dFun, a2: bFun, a3: bFun, a4: bFun): bFun = bFunDummy
  def helperStringIntIntInt2Boolean(a1: sFun, a2: iFun, a3: iFun, a4: iFun): bFun = bFunDummy
  def helperStringIntIntLong2Boolean(a1: sFun, a2: iFun, a3: iFun, a4: lFun): bFun = bFunDummy
  def helperStringIntIntDouble2Boolean(a1: sFun, a2: iFun, a3: iFun, a4: dFun): bFun = bFunDummy
  def helperStringIntIntString2Boolean(a1: sFun, a2: iFun, a3: iFun, a4: sFun): bFun = bFunDummy
  def helperStringIntIntBoolean2Boolean(a1: sFun, a2: iFun, a3: iFun, a4: bFun): bFun = bFunDummy
  def helperStringIntLongInt2Boolean(a1: sFun, a2: iFun, a3: lFun, a4: iFun): bFun = bFunDummy
  def helperStringIntLongLong2Boolean(a1: sFun, a2: iFun, a3: lFun, a4: lFun): bFun = bFunDummy
  def helperStringIntLongDouble2Boolean(a1: sFun, a2: iFun, a3: lFun, a4: dFun): bFun = bFunDummy
  def helperStringIntLongString2Boolean(a1: sFun, a2: iFun, a3: lFun, a4: sFun): bFun = bFunDummy
  def helperStringIntLongBoolean2Boolean(a1: sFun, a2: iFun, a3: lFun, a4: bFun): bFun = bFunDummy
  def helperStringIntDoubleInt2Boolean(a1: sFun, a2: iFun, a3: dFun, a4: iFun): bFun = bFunDummy
  def helperStringIntDoubleLong2Boolean(a1: sFun, a2: iFun, a3: dFun, a4: lFun): bFun = bFunDummy
  def helperStringIntDoubleDouble2Boolean(a1: sFun, a2: iFun, a3: dFun, a4: dFun): bFun = bFunDummy
  def helperStringIntDoubleString2Boolean(a1: sFun, a2: iFun, a3: dFun, a4: sFun): bFun = bFunDummy
  def helperStringIntDoubleBoolean2Boolean(a1: sFun, a2: iFun, a3: dFun, a4: bFun): bFun = bFunDummy
  def helperStringIntStringInt2Boolean(a1: sFun, a2: iFun, a3: sFun, a4: iFun): bFun = bFunDummy
  def helperStringIntStringLong2Boolean(a1: sFun, a2: iFun, a3: sFun, a4: lFun): bFun = bFunDummy
  def helperStringIntStringDouble2Boolean(a1: sFun, a2: iFun, a3: sFun, a4: dFun): bFun = bFunDummy
  def helperStringIntStringString2Boolean(a1: sFun, a2: iFun, a3: sFun, a4: sFun): bFun = bFunDummy
  def helperStringIntStringBoolean2Boolean(a1: sFun, a2: iFun, a3: sFun, a4: bFun): bFun = bFunDummy
  def helperStringIntBooleanInt2Boolean(a1: sFun, a2: iFun, a3: bFun, a4: iFun): bFun = bFunDummy
  def helperStringIntBooleanLong2Boolean(a1: sFun, a2: iFun, a3: bFun, a4: lFun): bFun = bFunDummy
  def helperStringIntBooleanDouble2Boolean(a1: sFun, a2: iFun, a3: bFun, a4: dFun): bFun = bFunDummy
  def helperStringIntBooleanString2Boolean(a1: sFun, a2: iFun, a3: bFun, a4: sFun): bFun = bFunDummy
  def helperStringIntBooleanBoolean2Boolean(a1: sFun, a2: iFun, a3: bFun, a4: bFun): bFun = bFunDummy
  def helperStringLongIntInt2Boolean(a1: sFun, a2: lFun, a3: iFun, a4: iFun): bFun = bFunDummy
  def helperStringLongIntLong2Boolean(a1: sFun, a2: lFun, a3: iFun, a4: lFun): bFun = bFunDummy
  def helperStringLongIntDouble2Boolean(a1: sFun, a2: lFun, a3: iFun, a4: dFun): bFun = bFunDummy
  def helperStringLongIntString2Boolean(a1: sFun, a2: lFun, a3: iFun, a4: sFun): bFun = bFunDummy
  def helperStringLongIntBoolean2Boolean(a1: sFun, a2: lFun, a3: iFun, a4: bFun): bFun = bFunDummy
  def helperStringLongLongInt2Boolean(a1: sFun, a2: lFun, a3: lFun, a4: iFun): bFun = bFunDummy
  def helperStringLongLongLong2Boolean(a1: sFun, a2: lFun, a3: lFun, a4: lFun): bFun = bFunDummy
  def helperStringLongLongDouble2Boolean(a1: sFun, a2: lFun, a3: lFun, a4: dFun): bFun = bFunDummy
  def helperStringLongLongString2Boolean(a1: sFun, a2: lFun, a3: lFun, a4: sFun): bFun = bFunDummy
  def helperStringLongLongBoolean2Boolean(a1: sFun, a2: lFun, a3: lFun, a4: bFun): bFun = bFunDummy
  def helperStringLongDoubleInt2Boolean(a1: sFun, a2: lFun, a3: dFun, a4: iFun): bFun = bFunDummy
  def helperStringLongDoubleLong2Boolean(a1: sFun, a2: lFun, a3: dFun, a4: lFun): bFun = bFunDummy
  def helperStringLongDoubleDouble2Boolean(a1: sFun, a2: lFun, a3: dFun, a4: dFun): bFun = bFunDummy
  def helperStringLongDoubleString2Boolean(a1: sFun, a2: lFun, a3: dFun, a4: sFun): bFun = bFunDummy
  def helperStringLongDoubleBoolean2Boolean(a1: sFun, a2: lFun, a3: dFun, a4: bFun): bFun = bFunDummy
  def helperStringLongStringInt2Boolean(a1: sFun, a2: lFun, a3: sFun, a4: iFun): bFun = bFunDummy
  def helperStringLongStringLong2Boolean(a1: sFun, a2: lFun, a3: sFun, a4: lFun): bFun = bFunDummy
  def helperStringLongStringDouble2Boolean(a1: sFun, a2: lFun, a3: sFun, a4: dFun): bFun = bFunDummy
  def helperStringLongStringString2Boolean(a1: sFun, a2: lFun, a3: sFun, a4: sFun): bFun = bFunDummy
  def helperStringLongStringBoolean2Boolean(a1: sFun, a2: lFun, a3: sFun, a4: bFun): bFun = bFunDummy
  def helperStringLongBooleanInt2Boolean(a1: sFun, a2: lFun, a3: bFun, a4: iFun): bFun = bFunDummy
  def helperStringLongBooleanLong2Boolean(a1: sFun, a2: lFun, a3: bFun, a4: lFun): bFun = bFunDummy
  def helperStringLongBooleanDouble2Boolean(a1: sFun, a2: lFun, a3: bFun, a4: dFun): bFun = bFunDummy
  def helperStringLongBooleanString2Boolean(a1: sFun, a2: lFun, a3: bFun, a4: sFun): bFun = bFunDummy
  def helperStringLongBooleanBoolean2Boolean(a1: sFun, a2: lFun, a3: bFun, a4: bFun): bFun = bFunDummy
  def helperStringDoubleIntInt2Boolean(a1: sFun, a2: dFun, a3: iFun, a4: iFun): bFun = bFunDummy
  def helperStringDoubleIntLong2Boolean(a1: sFun, a2: dFun, a3: iFun, a4: lFun): bFun = bFunDummy
  def helperStringDoubleIntDouble2Boolean(a1: sFun, a2: dFun, a3: iFun, a4: dFun): bFun = bFunDummy
  def helperStringDoubleIntString2Boolean(a1: sFun, a2: dFun, a3: iFun, a4: sFun): bFun = bFunDummy
  def helperStringDoubleIntBoolean2Boolean(a1: sFun, a2: dFun, a3: iFun, a4: bFun): bFun = bFunDummy
  def helperStringDoubleLongInt2Boolean(a1: sFun, a2: dFun, a3: lFun, a4: iFun): bFun = bFunDummy
  def helperStringDoubleLongLong2Boolean(a1: sFun, a2: dFun, a3: lFun, a4: lFun): bFun = bFunDummy
  def helperStringDoubleLongDouble2Boolean(a1: sFun, a2: dFun, a3: lFun, a4: dFun): bFun = bFunDummy
  def helperStringDoubleLongString2Boolean(a1: sFun, a2: dFun, a3: lFun, a4: sFun): bFun = bFunDummy
  def helperStringDoubleLongBoolean2Boolean(a1: sFun, a2: dFun, a3: lFun, a4: bFun): bFun = bFunDummy
  def helperStringDoubleDoubleInt2Boolean(a1: sFun, a2: dFun, a3: dFun, a4: iFun): bFun = bFunDummy
  def helperStringDoubleDoubleLong2Boolean(a1: sFun, a2: dFun, a3: dFun, a4: lFun): bFun = bFunDummy
  def helperStringDoubleDoubleDouble2Boolean(a1: sFun, a2: dFun, a3: dFun, a4: dFun): bFun = bFunDummy
  def helperStringDoubleDoubleString2Boolean(a1: sFun, a2: dFun, a3: dFun, a4: sFun): bFun = bFunDummy
  def helperStringDoubleDoubleBoolean2Boolean(a1: sFun, a2: dFun, a3: dFun, a4: bFun): bFun = bFunDummy
  def helperStringDoubleStringInt2Boolean(a1: sFun, a2: dFun, a3: sFun, a4: iFun): bFun = bFunDummy
  def helperStringDoubleStringLong2Boolean(a1: sFun, a2: dFun, a3: sFun, a4: lFun): bFun = bFunDummy
  def helperStringDoubleStringDouble2Boolean(a1: sFun, a2: dFun, a3: sFun, a4: dFun): bFun = bFunDummy
  def helperStringDoubleStringString2Boolean(a1: sFun, a2: dFun, a3: sFun, a4: sFun): bFun = bFunDummy
  def helperStringDoubleStringBoolean2Boolean(a1: sFun, a2: dFun, a3: sFun, a4: bFun): bFun = bFunDummy
  def helperStringDoubleBooleanInt2Boolean(a1: sFun, a2: dFun, a3: bFun, a4: iFun): bFun = bFunDummy
  def helperStringDoubleBooleanLong2Boolean(a1: sFun, a2: dFun, a3: bFun, a4: lFun): bFun = bFunDummy
  def helperStringDoubleBooleanDouble2Boolean(a1: sFun, a2: dFun, a3: bFun, a4: dFun): bFun = bFunDummy
  def helperStringDoubleBooleanString2Boolean(a1: sFun, a2: dFun, a3: bFun, a4: sFun): bFun = bFunDummy
  def helperStringDoubleBooleanBoolean2Boolean(a1: sFun, a2: dFun, a3: bFun, a4: bFun): bFun = bFunDummy
  def helperStringStringIntInt2Boolean(a1: sFun, a2: sFun, a3: iFun, a4: iFun): bFun = bFunDummy
  def helperStringStringIntLong2Boolean(a1: sFun, a2: sFun, a3: iFun, a4: lFun): bFun = bFunDummy
  def helperStringStringIntDouble2Boolean(a1: sFun, a2: sFun, a3: iFun, a4: dFun): bFun = bFunDummy
  def helperStringStringIntString2Boolean(a1: sFun, a2: sFun, a3: iFun, a4: sFun): bFun = bFunDummy
  def helperStringStringIntBoolean2Boolean(a1: sFun, a2: sFun, a3: iFun, a4: bFun): bFun = bFunDummy
  def helperStringStringLongInt2Boolean(a1: sFun, a2: sFun, a3: lFun, a4: iFun): bFun = bFunDummy
  def helperStringStringLongLong2Boolean(a1: sFun, a2: sFun, a3: lFun, a4: lFun): bFun = bFunDummy
  def helperStringStringLongDouble2Boolean(a1: sFun, a2: sFun, a3: lFun, a4: dFun): bFun = bFunDummy
  def helperStringStringLongString2Boolean(a1: sFun, a2: sFun, a3: lFun, a4: sFun): bFun = bFunDummy
  def helperStringStringLongBoolean2Boolean(a1: sFun, a2: sFun, a3: lFun, a4: bFun): bFun = bFunDummy
  def helperStringStringDoubleInt2Boolean(a1: sFun, a2: sFun, a3: dFun, a4: iFun): bFun = bFunDummy
  def helperStringStringDoubleLong2Boolean(a1: sFun, a2: sFun, a3: dFun, a4: lFun): bFun = bFunDummy
  def helperStringStringDoubleDouble2Boolean(a1: sFun, a2: sFun, a3: dFun, a4: dFun): bFun = bFunDummy
  def helperStringStringDoubleString2Boolean(a1: sFun, a2: sFun, a3: dFun, a4: sFun): bFun = bFunDummy
  def helperStringStringDoubleBoolean2Boolean(a1: sFun, a2: sFun, a3: dFun, a4: bFun): bFun = bFunDummy
  def helperStringStringStringInt2Boolean(a1: sFun, a2: sFun, a3: sFun, a4: iFun): bFun = bFunDummy
  def helperStringStringStringLong2Boolean(a1: sFun, a2: sFun, a3: sFun, a4: lFun): bFun = bFunDummy
  def helperStringStringStringDouble2Boolean(a1: sFun, a2: sFun, a3: sFun, a4: dFun): bFun = bFunDummy
  def helperStringStringStringString2Boolean(a1: sFun, a2: sFun, a3: sFun, a4: sFun): bFun = bFunDummy
  def helperStringStringStringBoolean2Boolean(a1: sFun, a2: sFun, a3: sFun, a4: bFun): bFun = bFunDummy
  def helperStringStringBooleanInt2Boolean(a1: sFun, a2: sFun, a3: bFun, a4: iFun): bFun = bFunDummy
  def helperStringStringBooleanLong2Boolean(a1: sFun, a2: sFun, a3: bFun, a4: lFun): bFun = bFunDummy
  def helperStringStringBooleanDouble2Boolean(a1: sFun, a2: sFun, a3: bFun, a4: dFun): bFun = bFunDummy
  def helperStringStringBooleanString2Boolean(a1: sFun, a2: sFun, a3: bFun, a4: sFun): bFun = bFunDummy
  def helperStringStringBooleanBoolean2Boolean(a1: sFun, a2: sFun, a3: bFun, a4: bFun): bFun = bFunDummy
  def helperStringBooleanIntInt2Boolean(a1: sFun, a2: bFun, a3: iFun, a4: iFun): bFun = bFunDummy
  def helperStringBooleanIntLong2Boolean(a1: sFun, a2: bFun, a3: iFun, a4: lFun): bFun = bFunDummy
  def helperStringBooleanIntDouble2Boolean(a1: sFun, a2: bFun, a3: iFun, a4: dFun): bFun = bFunDummy
  def helperStringBooleanIntString2Boolean(a1: sFun, a2: bFun, a3: iFun, a4: sFun): bFun = bFunDummy
  def helperStringBooleanIntBoolean2Boolean(a1: sFun, a2: bFun, a3: iFun, a4: bFun): bFun = bFunDummy
  def helperStringBooleanLongInt2Boolean(a1: sFun, a2: bFun, a3: lFun, a4: iFun): bFun = bFunDummy
  def helperStringBooleanLongLong2Boolean(a1: sFun, a2: bFun, a3: lFun, a4: lFun): bFun = bFunDummy
  def helperStringBooleanLongDouble2Boolean(a1: sFun, a2: bFun, a3: lFun, a4: dFun): bFun = bFunDummy
  def helperStringBooleanLongString2Boolean(a1: sFun, a2: bFun, a3: lFun, a4: sFun): bFun = bFunDummy
  def helperStringBooleanLongBoolean2Boolean(a1: sFun, a2: bFun, a3: lFun, a4: bFun): bFun = bFunDummy
  def helperStringBooleanDoubleInt2Boolean(a1: sFun, a2: bFun, a3: dFun, a4: iFun): bFun = bFunDummy
  def helperStringBooleanDoubleLong2Boolean(a1: sFun, a2: bFun, a3: dFun, a4: lFun): bFun = bFunDummy
  def helperStringBooleanDoubleDouble2Boolean(a1: sFun, a2: bFun, a3: dFun, a4: dFun): bFun = bFunDummy
  def helperStringBooleanDoubleString2Boolean(a1: sFun, a2: bFun, a3: dFun, a4: sFun): bFun = bFunDummy
  def helperStringBooleanDoubleBoolean2Boolean(a1: sFun, a2: bFun, a3: dFun, a4: bFun): bFun = bFunDummy
  def helperStringBooleanStringInt2Boolean(a1: sFun, a2: bFun, a3: sFun, a4: iFun): bFun = bFunDummy
  def helperStringBooleanStringLong2Boolean(a1: sFun, a2: bFun, a3: sFun, a4: lFun): bFun = bFunDummy
  def helperStringBooleanStringDouble2Boolean(a1: sFun, a2: bFun, a3: sFun, a4: dFun): bFun = bFunDummy
  def helperStringBooleanStringString2Boolean(a1: sFun, a2: bFun, a3: sFun, a4: sFun): bFun = bFunDummy
  def helperStringBooleanStringBoolean2Boolean(a1: sFun, a2: bFun, a3: sFun, a4: bFun): bFun = bFunDummy
  def helperStringBooleanBooleanInt2Boolean(a1: sFun, a2: bFun, a3: bFun, a4: iFun): bFun = bFunDummy
  def helperStringBooleanBooleanLong2Boolean(a1: sFun, a2: bFun, a3: bFun, a4: lFun): bFun = bFunDummy
  def helperStringBooleanBooleanDouble2Boolean(a1: sFun, a2: bFun, a3: bFun, a4: dFun): bFun = bFunDummy
  def helperStringBooleanBooleanString2Boolean(a1: sFun, a2: bFun, a3: bFun, a4: sFun): bFun = bFunDummy
  def helperStringBooleanBooleanBoolean2Boolean(a1: sFun, a2: bFun, a3: bFun, a4: bFun): bFun = bFunDummy
  def helperBooleanIntIntInt2Boolean(a1: bFun, a2: iFun, a3: iFun, a4: iFun): bFun = bFunDummy
  def helperBooleanIntIntLong2Boolean(a1: bFun, a2: iFun, a3: iFun, a4: lFun): bFun = bFunDummy
  def helperBooleanIntIntDouble2Boolean(a1: bFun, a2: iFun, a3: iFun, a4: dFun): bFun = bFunDummy
  def helperBooleanIntIntString2Boolean(a1: bFun, a2: iFun, a3: iFun, a4: sFun): bFun = bFunDummy
  def helperBooleanIntIntBoolean2Boolean(a1: bFun, a2: iFun, a3: iFun, a4: bFun): bFun = bFunDummy
  def helperBooleanIntLongInt2Boolean(a1: bFun, a2: iFun, a3: lFun, a4: iFun): bFun = bFunDummy
  def helperBooleanIntLongLong2Boolean(a1: bFun, a2: iFun, a3: lFun, a4: lFun): bFun = bFunDummy
  def helperBooleanIntLongDouble2Boolean(a1: bFun, a2: iFun, a3: lFun, a4: dFun): bFun = bFunDummy
  def helperBooleanIntLongString2Boolean(a1: bFun, a2: iFun, a3: lFun, a4: sFun): bFun = bFunDummy
  def helperBooleanIntLongBoolean2Boolean(a1: bFun, a2: iFun, a3: lFun, a4: bFun): bFun = bFunDummy
  def helperBooleanIntDoubleInt2Boolean(a1: bFun, a2: iFun, a3: dFun, a4: iFun): bFun = bFunDummy
  def helperBooleanIntDoubleLong2Boolean(a1: bFun, a2: iFun, a3: dFun, a4: lFun): bFun = bFunDummy
  def helperBooleanIntDoubleDouble2Boolean(a1: bFun, a2: iFun, a3: dFun, a4: dFun): bFun = bFunDummy
  def helperBooleanIntDoubleString2Boolean(a1: bFun, a2: iFun, a3: dFun, a4: sFun): bFun = bFunDummy
  def helperBooleanIntDoubleBoolean2Boolean(a1: bFun, a2: iFun, a3: dFun, a4: bFun): bFun = bFunDummy
  def helperBooleanIntStringInt2Boolean(a1: bFun, a2: iFun, a3: sFun, a4: iFun): bFun = bFunDummy
  def helperBooleanIntStringLong2Boolean(a1: bFun, a2: iFun, a3: sFun, a4: lFun): bFun = bFunDummy
  def helperBooleanIntStringDouble2Boolean(a1: bFun, a2: iFun, a3: sFun, a4: dFun): bFun = bFunDummy
  def helperBooleanIntStringString2Boolean(a1: bFun, a2: iFun, a3: sFun, a4: sFun): bFun = bFunDummy
  def helperBooleanIntStringBoolean2Boolean(a1: bFun, a2: iFun, a3: sFun, a4: bFun): bFun = bFunDummy
  def helperBooleanIntBooleanInt2Boolean(a1: bFun, a2: iFun, a3: bFun, a4: iFun): bFun = bFunDummy
  def helperBooleanIntBooleanLong2Boolean(a1: bFun, a2: iFun, a3: bFun, a4: lFun): bFun = bFunDummy
  def helperBooleanIntBooleanDouble2Boolean(a1: bFun, a2: iFun, a3: bFun, a4: dFun): bFun = bFunDummy
  def helperBooleanIntBooleanString2Boolean(a1: bFun, a2: iFun, a3: bFun, a4: sFun): bFun = bFunDummy
  def helperBooleanIntBooleanBoolean2Boolean(a1: bFun, a2: iFun, a3: bFun, a4: bFun): bFun = bFunDummy
  def helperBooleanLongIntInt2Boolean(a1: bFun, a2: lFun, a3: iFun, a4: iFun): bFun = bFunDummy
  def helperBooleanLongIntLong2Boolean(a1: bFun, a2: lFun, a3: iFun, a4: lFun): bFun = bFunDummy
  def helperBooleanLongIntDouble2Boolean(a1: bFun, a2: lFun, a3: iFun, a4: dFun): bFun = bFunDummy
  def helperBooleanLongIntString2Boolean(a1: bFun, a2: lFun, a3: iFun, a4: sFun): bFun = bFunDummy
  def helperBooleanLongIntBoolean2Boolean(a1: bFun, a2: lFun, a3: iFun, a4: bFun): bFun = bFunDummy
  def helperBooleanLongLongInt2Boolean(a1: bFun, a2: lFun, a3: lFun, a4: iFun): bFun = bFunDummy
  def helperBooleanLongLongLong2Boolean(a1: bFun, a2: lFun, a3: lFun, a4: lFun): bFun = bFunDummy
  def helperBooleanLongLongDouble2Boolean(a1: bFun, a2: lFun, a3: lFun, a4: dFun): bFun = bFunDummy
  def helperBooleanLongLongString2Boolean(a1: bFun, a2: lFun, a3: lFun, a4: sFun): bFun = bFunDummy
  def helperBooleanLongLongBoolean2Boolean(a1: bFun, a2: lFun, a3: lFun, a4: bFun): bFun = bFunDummy
  def helperBooleanLongDoubleInt2Boolean(a1: bFun, a2: lFun, a3: dFun, a4: iFun): bFun = bFunDummy
  def helperBooleanLongDoubleLong2Boolean(a1: bFun, a2: lFun, a3: dFun, a4: lFun): bFun = bFunDummy
  def helperBooleanLongDoubleDouble2Boolean(a1: bFun, a2: lFun, a3: dFun, a4: dFun): bFun = bFunDummy
  def helperBooleanLongDoubleString2Boolean(a1: bFun, a2: lFun, a3: dFun, a4: sFun): bFun = bFunDummy
  def helperBooleanLongDoubleBoolean2Boolean(a1: bFun, a2: lFun, a3: dFun, a4: bFun): bFun = bFunDummy
  def helperBooleanLongStringInt2Boolean(a1: bFun, a2: lFun, a3: sFun, a4: iFun): bFun = bFunDummy
  def helperBooleanLongStringLong2Boolean(a1: bFun, a2: lFun, a3: sFun, a4: lFun): bFun = bFunDummy
  def helperBooleanLongStringDouble2Boolean(a1: bFun, a2: lFun, a3: sFun, a4: dFun): bFun = bFunDummy
  def helperBooleanLongStringString2Boolean(a1: bFun, a2: lFun, a3: sFun, a4: sFun): bFun = bFunDummy
  def helperBooleanLongStringBoolean2Boolean(a1: bFun, a2: lFun, a3: sFun, a4: bFun): bFun = bFunDummy
  def helperBooleanLongBooleanInt2Boolean(a1: bFun, a2: lFun, a3: bFun, a4: iFun): bFun = bFunDummy
  def helperBooleanLongBooleanLong2Boolean(a1: bFun, a2: lFun, a3: bFun, a4: lFun): bFun = bFunDummy
  def helperBooleanLongBooleanDouble2Boolean(a1: bFun, a2: lFun, a3: bFun, a4: dFun): bFun = bFunDummy
  def helperBooleanLongBooleanString2Boolean(a1: bFun, a2: lFun, a3: bFun, a4: sFun): bFun = bFunDummy
  def helperBooleanLongBooleanBoolean2Boolean(a1: bFun, a2: lFun, a3: bFun, a4: bFun): bFun = bFunDummy
  def helperBooleanDoubleIntInt2Boolean(a1: bFun, a2: dFun, a3: iFun, a4: iFun): bFun = bFunDummy
  def helperBooleanDoubleIntLong2Boolean(a1: bFun, a2: dFun, a3: iFun, a4: lFun): bFun = bFunDummy
  def helperBooleanDoubleIntDouble2Boolean(a1: bFun, a2: dFun, a3: iFun, a4: dFun): bFun = bFunDummy
  def helperBooleanDoubleIntString2Boolean(a1: bFun, a2: dFun, a3: iFun, a4: sFun): bFun = bFunDummy
  def helperBooleanDoubleIntBoolean2Boolean(a1: bFun, a2: dFun, a3: iFun, a4: bFun): bFun = bFunDummy
  def helperBooleanDoubleLongInt2Boolean(a1: bFun, a2: dFun, a3: lFun, a4: iFun): bFun = bFunDummy
  def helperBooleanDoubleLongLong2Boolean(a1: bFun, a2: dFun, a3: lFun, a4: lFun): bFun = bFunDummy
  def helperBooleanDoubleLongDouble2Boolean(a1: bFun, a2: dFun, a3: lFun, a4: dFun): bFun = bFunDummy
  def helperBooleanDoubleLongString2Boolean(a1: bFun, a2: dFun, a3: lFun, a4: sFun): bFun = bFunDummy
  def helperBooleanDoubleLongBoolean2Boolean(a1: bFun, a2: dFun, a3: lFun, a4: bFun): bFun = bFunDummy
  def helperBooleanDoubleDoubleInt2Boolean(a1: bFun, a2: dFun, a3: dFun, a4: iFun): bFun = bFunDummy
  def helperBooleanDoubleDoubleLong2Boolean(a1: bFun, a2: dFun, a3: dFun, a4: lFun): bFun = bFunDummy
  def helperBooleanDoubleDoubleDouble2Boolean(a1: bFun, a2: dFun, a3: dFun, a4: dFun): bFun = bFunDummy
  def helperBooleanDoubleDoubleString2Boolean(a1: bFun, a2: dFun, a3: dFun, a4: sFun): bFun = bFunDummy
  def helperBooleanDoubleDoubleBoolean2Boolean(a1: bFun, a2: dFun, a3: dFun, a4: bFun): bFun = bFunDummy
  def helperBooleanDoubleStringInt2Boolean(a1: bFun, a2: dFun, a3: sFun, a4: iFun): bFun = bFunDummy
  def helperBooleanDoubleStringLong2Boolean(a1: bFun, a2: dFun, a3: sFun, a4: lFun): bFun = bFunDummy
  def helperBooleanDoubleStringDouble2Boolean(a1: bFun, a2: dFun, a3: sFun, a4: dFun): bFun = bFunDummy
  def helperBooleanDoubleStringString2Boolean(a1: bFun, a2: dFun, a3: sFun, a4: sFun): bFun = bFunDummy
  def helperBooleanDoubleStringBoolean2Boolean(a1: bFun, a2: dFun, a3: sFun, a4: bFun): bFun = bFunDummy
  def helperBooleanDoubleBooleanInt2Boolean(a1: bFun, a2: dFun, a3: bFun, a4: iFun): bFun = bFunDummy
  def helperBooleanDoubleBooleanLong2Boolean(a1: bFun, a2: dFun, a3: bFun, a4: lFun): bFun = bFunDummy
  def helperBooleanDoubleBooleanDouble2Boolean(a1: bFun, a2: dFun, a3: bFun, a4: dFun): bFun = bFunDummy
  def helperBooleanDoubleBooleanString2Boolean(a1: bFun, a2: dFun, a3: bFun, a4: sFun): bFun = bFunDummy
  def helperBooleanDoubleBooleanBoolean2Boolean(a1: bFun, a2: dFun, a3: bFun, a4: bFun): bFun = bFunDummy
  def helperBooleanStringIntInt2Boolean(a1: bFun, a2: sFun, a3: iFun, a4: iFun): bFun = bFunDummy
  def helperBooleanStringIntLong2Boolean(a1: bFun, a2: sFun, a3: iFun, a4: lFun): bFun = bFunDummy
  def helperBooleanStringIntDouble2Boolean(a1: bFun, a2: sFun, a3: iFun, a4: dFun): bFun = bFunDummy
  def helperBooleanStringIntString2Boolean(a1: bFun, a2: sFun, a3: iFun, a4: sFun): bFun = bFunDummy
  def helperBooleanStringIntBoolean2Boolean(a1: bFun, a2: sFun, a3: iFun, a4: bFun): bFun = bFunDummy
  def helperBooleanStringLongInt2Boolean(a1: bFun, a2: sFun, a3: lFun, a4: iFun): bFun = bFunDummy
  def helperBooleanStringLongLong2Boolean(a1: bFun, a2: sFun, a3: lFun, a4: lFun): bFun = bFunDummy
  def helperBooleanStringLongDouble2Boolean(a1: bFun, a2: sFun, a3: lFun, a4: dFun): bFun = bFunDummy
  def helperBooleanStringLongString2Boolean(a1: bFun, a2: sFun, a3: lFun, a4: sFun): bFun = bFunDummy
  def helperBooleanStringLongBoolean2Boolean(a1: bFun, a2: sFun, a3: lFun, a4: bFun): bFun = bFunDummy
  def helperBooleanStringDoubleInt2Boolean(a1: bFun, a2: sFun, a3: dFun, a4: iFun): bFun = bFunDummy
  def helperBooleanStringDoubleLong2Boolean(a1: bFun, a2: sFun, a3: dFun, a4: lFun): bFun = bFunDummy
  def helperBooleanStringDoubleDouble2Boolean(a1: bFun, a2: sFun, a3: dFun, a4: dFun): bFun = bFunDummy
  def helperBooleanStringDoubleString2Boolean(a1: bFun, a2: sFun, a3: dFun, a4: sFun): bFun = bFunDummy
  def helperBooleanStringDoubleBoolean2Boolean(a1: bFun, a2: sFun, a3: dFun, a4: bFun): bFun = bFunDummy
  def helperBooleanStringStringInt2Boolean(a1: bFun, a2: sFun, a3: sFun, a4: iFun): bFun = bFunDummy
  def helperBooleanStringStringLong2Boolean(a1: bFun, a2: sFun, a3: sFun, a4: lFun): bFun = bFunDummy
  def helperBooleanStringStringDouble2Boolean(a1: bFun, a2: sFun, a3: sFun, a4: dFun): bFun = bFunDummy
  def helperBooleanStringStringString2Boolean(a1: bFun, a2: sFun, a3: sFun, a4: sFun): bFun = bFunDummy
  def helperBooleanStringStringBoolean2Boolean(a1: bFun, a2: sFun, a3: sFun, a4: bFun): bFun = bFunDummy
  def helperBooleanStringBooleanInt2Boolean(a1: bFun, a2: sFun, a3: bFun, a4: iFun): bFun = bFunDummy
  def helperBooleanStringBooleanLong2Boolean(a1: bFun, a2: sFun, a3: bFun, a4: lFun): bFun = bFunDummy
  def helperBooleanStringBooleanDouble2Boolean(a1: bFun, a2: sFun, a3: bFun, a4: dFun): bFun = bFunDummy
  def helperBooleanStringBooleanString2Boolean(a1: bFun, a2: sFun, a3: bFun, a4: sFun): bFun = bFunDummy
  def helperBooleanStringBooleanBoolean2Boolean(a1: bFun, a2: sFun, a3: bFun, a4: bFun): bFun = bFunDummy
  def helperBooleanBooleanIntInt2Boolean(a1: bFun, a2: bFun, a3: iFun, a4: iFun): bFun = bFunDummy
  def helperBooleanBooleanIntLong2Boolean(a1: bFun, a2: bFun, a3: iFun, a4: lFun): bFun = bFunDummy
  def helperBooleanBooleanIntDouble2Boolean(a1: bFun, a2: bFun, a3: iFun, a4: dFun): bFun = bFunDummy
  def helperBooleanBooleanIntString2Boolean(a1: bFun, a2: bFun, a3: iFun, a4: sFun): bFun = bFunDummy
  def helperBooleanBooleanIntBoolean2Boolean(a1: bFun, a2: bFun, a3: iFun, a4: bFun): bFun = bFunDummy
  def helperBooleanBooleanLongInt2Boolean(a1: bFun, a2: bFun, a3: lFun, a4: iFun): bFun = bFunDummy
  def helperBooleanBooleanLongLong2Boolean(a1: bFun, a2: bFun, a3: lFun, a4: lFun): bFun = bFunDummy
  def helperBooleanBooleanLongDouble2Boolean(a1: bFun, a2: bFun, a3: lFun, a4: dFun): bFun = bFunDummy
  def helperBooleanBooleanLongString2Boolean(a1: bFun, a2: bFun, a3: lFun, a4: sFun): bFun = bFunDummy
  def helperBooleanBooleanLongBoolean2Boolean(a1: bFun, a2: bFun, a3: lFun, a4: bFun): bFun = bFunDummy
  def helperBooleanBooleanDoubleInt2Boolean(a1: bFun, a2: bFun, a3: dFun, a4: iFun): bFun = bFunDummy
  def helperBooleanBooleanDoubleLong2Boolean(a1: bFun, a2: bFun, a3: dFun, a4: lFun): bFun = bFunDummy
  def helperBooleanBooleanDoubleDouble2Boolean(a1: bFun, a2: bFun, a3: dFun, a4: dFun): bFun = bFunDummy
  def helperBooleanBooleanDoubleString2Boolean(a1: bFun, a2: bFun, a3: dFun, a4: sFun): bFun = bFunDummy
  def helperBooleanBooleanDoubleBoolean2Boolean(a1: bFun, a2: bFun, a3: dFun, a4: bFun): bFun = bFunDummy
  def helperBooleanBooleanStringInt2Boolean(a1: bFun, a2: bFun, a3: sFun, a4: iFun): bFun = bFunDummy
  def helperBooleanBooleanStringLong2Boolean(a1: bFun, a2: bFun, a3: sFun, a4: lFun): bFun = bFunDummy
  def helperBooleanBooleanStringDouble2Boolean(a1: bFun, a2: bFun, a3: sFun, a4: dFun): bFun = bFunDummy
  def helperBooleanBooleanStringString2Boolean(a1: bFun, a2: bFun, a3: sFun, a4: sFun): bFun = bFunDummy
  def helperBooleanBooleanStringBoolean2Boolean(a1: bFun, a2: bFun, a3: sFun, a4: bFun): bFun = bFunDummy
  def helperBooleanBooleanBooleanInt2Boolean(a1: bFun, a2: bFun, a3: bFun, a4: iFun): bFun = bFunDummy
  def helperBooleanBooleanBooleanLong2Boolean(a1: bFun, a2: bFun, a3: bFun, a4: lFun): bFun = bFunDummy
  def helperBooleanBooleanBooleanDouble2Boolean(a1: bFun, a2: bFun, a3: bFun, a4: dFun): bFun = bFunDummy
  def helperBooleanBooleanBooleanString2Boolean(a1: bFun, a2: bFun, a3: bFun, a4: sFun): bFun = bFunDummy
  def helperBooleanBooleanBooleanBoolean2Boolean(a1: bFun, a2: bFun, a3: bFun, a4: bFun): bFun = bFunDummy
  test("getSignature_bFun_5") {
    var result = true
    result &= FunctionSignature.getSignatureIntIntIntInt2Boolean(helperIntIntIntInt2Boolean) == "Int:Int:Int:Int2Boolean"
    result &= FunctionSignature.getSignatureIntIntIntLong2Boolean(helperIntIntIntLong2Boolean) == "Int:Int:Int:Long2Boolean"
    result &= FunctionSignature.getSignatureIntIntIntDouble2Boolean(helperIntIntIntDouble2Boolean) == "Int:Int:Int:Double2Boolean"
    result &= FunctionSignature.getSignatureIntIntIntString2Boolean(helperIntIntIntString2Boolean) == "Int:Int:Int:String2Boolean"
    result &= FunctionSignature.getSignatureIntIntIntBoolean2Boolean(helperIntIntIntBoolean2Boolean) == "Int:Int:Int:Boolean2Boolean"
    result &= FunctionSignature.getSignatureIntIntLongInt2Boolean(helperIntIntLongInt2Boolean) == "Int:Int:Long:Int2Boolean"
    result &= FunctionSignature.getSignatureIntIntLongLong2Boolean(helperIntIntLongLong2Boolean) == "Int:Int:Long:Long2Boolean"
    result &= FunctionSignature.getSignatureIntIntLongDouble2Boolean(helperIntIntLongDouble2Boolean) == "Int:Int:Long:Double2Boolean"
    result &= FunctionSignature.getSignatureIntIntLongString2Boolean(helperIntIntLongString2Boolean) == "Int:Int:Long:String2Boolean"
    result &= FunctionSignature.getSignatureIntIntLongBoolean2Boolean(helperIntIntLongBoolean2Boolean) == "Int:Int:Long:Boolean2Boolean"
    result &= FunctionSignature.getSignatureIntIntDoubleInt2Boolean(helperIntIntDoubleInt2Boolean) == "Int:Int:Double:Int2Boolean"
    result &= FunctionSignature.getSignatureIntIntDoubleLong2Boolean(helperIntIntDoubleLong2Boolean) == "Int:Int:Double:Long2Boolean"
    result &= FunctionSignature.getSignatureIntIntDoubleDouble2Boolean(helperIntIntDoubleDouble2Boolean) == "Int:Int:Double:Double2Boolean"
    result &= FunctionSignature.getSignatureIntIntDoubleString2Boolean(helperIntIntDoubleString2Boolean) == "Int:Int:Double:String2Boolean"
    result &= FunctionSignature.getSignatureIntIntDoubleBoolean2Boolean(helperIntIntDoubleBoolean2Boolean) == "Int:Int:Double:Boolean2Boolean"
    result &= FunctionSignature.getSignatureIntIntStringInt2Boolean(helperIntIntStringInt2Boolean) == "Int:Int:String:Int2Boolean"
    result &= FunctionSignature.getSignatureIntIntStringLong2Boolean(helperIntIntStringLong2Boolean) == "Int:Int:String:Long2Boolean"
    result &= FunctionSignature.getSignatureIntIntStringDouble2Boolean(helperIntIntStringDouble2Boolean) == "Int:Int:String:Double2Boolean"
    result &= FunctionSignature.getSignatureIntIntStringString2Boolean(helperIntIntStringString2Boolean) == "Int:Int:String:String2Boolean"
    result &= FunctionSignature.getSignatureIntIntStringBoolean2Boolean(helperIntIntStringBoolean2Boolean) == "Int:Int:String:Boolean2Boolean"
    result &= FunctionSignature.getSignatureIntIntBooleanInt2Boolean(helperIntIntBooleanInt2Boolean) == "Int:Int:Boolean:Int2Boolean"
    result &= FunctionSignature.getSignatureIntIntBooleanLong2Boolean(helperIntIntBooleanLong2Boolean) == "Int:Int:Boolean:Long2Boolean"
    result &= FunctionSignature.getSignatureIntIntBooleanDouble2Boolean(helperIntIntBooleanDouble2Boolean) == "Int:Int:Boolean:Double2Boolean"
    result &= FunctionSignature.getSignatureIntIntBooleanString2Boolean(helperIntIntBooleanString2Boolean) == "Int:Int:Boolean:String2Boolean"
    result &= FunctionSignature.getSignatureIntIntBooleanBoolean2Boolean(helperIntIntBooleanBoolean2Boolean) == "Int:Int:Boolean:Boolean2Boolean"
    result &= FunctionSignature.getSignatureIntLongIntInt2Boolean(helperIntLongIntInt2Boolean) == "Int:Long:Int:Int2Boolean"
    result &= FunctionSignature.getSignatureIntLongIntLong2Boolean(helperIntLongIntLong2Boolean) == "Int:Long:Int:Long2Boolean"
    result &= FunctionSignature.getSignatureIntLongIntDouble2Boolean(helperIntLongIntDouble2Boolean) == "Int:Long:Int:Double2Boolean"
    result &= FunctionSignature.getSignatureIntLongIntString2Boolean(helperIntLongIntString2Boolean) == "Int:Long:Int:String2Boolean"
    result &= FunctionSignature.getSignatureIntLongIntBoolean2Boolean(helperIntLongIntBoolean2Boolean) == "Int:Long:Int:Boolean2Boolean"
    result &= FunctionSignature.getSignatureIntLongLongInt2Boolean(helperIntLongLongInt2Boolean) == "Int:Long:Long:Int2Boolean"
    result &= FunctionSignature.getSignatureIntLongLongLong2Boolean(helperIntLongLongLong2Boolean) == "Int:Long:Long:Long2Boolean"
    result &= FunctionSignature.getSignatureIntLongLongDouble2Boolean(helperIntLongLongDouble2Boolean) == "Int:Long:Long:Double2Boolean"
    result &= FunctionSignature.getSignatureIntLongLongString2Boolean(helperIntLongLongString2Boolean) == "Int:Long:Long:String2Boolean"
    result &= FunctionSignature.getSignatureIntLongLongBoolean2Boolean(helperIntLongLongBoolean2Boolean) == "Int:Long:Long:Boolean2Boolean"
    result &= FunctionSignature.getSignatureIntLongDoubleInt2Boolean(helperIntLongDoubleInt2Boolean) == "Int:Long:Double:Int2Boolean"
    result &= FunctionSignature.getSignatureIntLongDoubleLong2Boolean(helperIntLongDoubleLong2Boolean) == "Int:Long:Double:Long2Boolean"
    result &= FunctionSignature.getSignatureIntLongDoubleDouble2Boolean(helperIntLongDoubleDouble2Boolean) == "Int:Long:Double:Double2Boolean"
    result &= FunctionSignature.getSignatureIntLongDoubleString2Boolean(helperIntLongDoubleString2Boolean) == "Int:Long:Double:String2Boolean"
    result &= FunctionSignature.getSignatureIntLongDoubleBoolean2Boolean(helperIntLongDoubleBoolean2Boolean) == "Int:Long:Double:Boolean2Boolean"
    result &= FunctionSignature.getSignatureIntLongStringInt2Boolean(helperIntLongStringInt2Boolean) == "Int:Long:String:Int2Boolean"
    result &= FunctionSignature.getSignatureIntLongStringLong2Boolean(helperIntLongStringLong2Boolean) == "Int:Long:String:Long2Boolean"
    result &= FunctionSignature.getSignatureIntLongStringDouble2Boolean(helperIntLongStringDouble2Boolean) == "Int:Long:String:Double2Boolean"
    result &= FunctionSignature.getSignatureIntLongStringString2Boolean(helperIntLongStringString2Boolean) == "Int:Long:String:String2Boolean"
    result &= FunctionSignature.getSignatureIntLongStringBoolean2Boolean(helperIntLongStringBoolean2Boolean) == "Int:Long:String:Boolean2Boolean"
    result &= FunctionSignature.getSignatureIntLongBooleanInt2Boolean(helperIntLongBooleanInt2Boolean) == "Int:Long:Boolean:Int2Boolean"
    result &= FunctionSignature.getSignatureIntLongBooleanLong2Boolean(helperIntLongBooleanLong2Boolean) == "Int:Long:Boolean:Long2Boolean"
    result &= FunctionSignature.getSignatureIntLongBooleanDouble2Boolean(helperIntLongBooleanDouble2Boolean) == "Int:Long:Boolean:Double2Boolean"
    result &= FunctionSignature.getSignatureIntLongBooleanString2Boolean(helperIntLongBooleanString2Boolean) == "Int:Long:Boolean:String2Boolean"
    result &= FunctionSignature.getSignatureIntLongBooleanBoolean2Boolean(helperIntLongBooleanBoolean2Boolean) == "Int:Long:Boolean:Boolean2Boolean"
    result &= FunctionSignature.getSignatureIntDoubleIntInt2Boolean(helperIntDoubleIntInt2Boolean) == "Int:Double:Int:Int2Boolean"
    result &= FunctionSignature.getSignatureIntDoubleIntLong2Boolean(helperIntDoubleIntLong2Boolean) == "Int:Double:Int:Long2Boolean"
    result &= FunctionSignature.getSignatureIntDoubleIntDouble2Boolean(helperIntDoubleIntDouble2Boolean) == "Int:Double:Int:Double2Boolean"
    result &= FunctionSignature.getSignatureIntDoubleIntString2Boolean(helperIntDoubleIntString2Boolean) == "Int:Double:Int:String2Boolean"
    result &= FunctionSignature.getSignatureIntDoubleIntBoolean2Boolean(helperIntDoubleIntBoolean2Boolean) == "Int:Double:Int:Boolean2Boolean"
    result &= FunctionSignature.getSignatureIntDoubleLongInt2Boolean(helperIntDoubleLongInt2Boolean) == "Int:Double:Long:Int2Boolean"
    result &= FunctionSignature.getSignatureIntDoubleLongLong2Boolean(helperIntDoubleLongLong2Boolean) == "Int:Double:Long:Long2Boolean"
    result &= FunctionSignature.getSignatureIntDoubleLongDouble2Boolean(helperIntDoubleLongDouble2Boolean) == "Int:Double:Long:Double2Boolean"
    result &= FunctionSignature.getSignatureIntDoubleLongString2Boolean(helperIntDoubleLongString2Boolean) == "Int:Double:Long:String2Boolean"
    result &= FunctionSignature.getSignatureIntDoubleLongBoolean2Boolean(helperIntDoubleLongBoolean2Boolean) == "Int:Double:Long:Boolean2Boolean"
    result &= FunctionSignature.getSignatureIntDoubleDoubleInt2Boolean(helperIntDoubleDoubleInt2Boolean) == "Int:Double:Double:Int2Boolean"
    result &= FunctionSignature.getSignatureIntDoubleDoubleLong2Boolean(helperIntDoubleDoubleLong2Boolean) == "Int:Double:Double:Long2Boolean"
    result &= FunctionSignature.getSignatureIntDoubleDoubleDouble2Boolean(helperIntDoubleDoubleDouble2Boolean) == "Int:Double:Double:Double2Boolean"
    result &= FunctionSignature.getSignatureIntDoubleDoubleString2Boolean(helperIntDoubleDoubleString2Boolean) == "Int:Double:Double:String2Boolean"
    result &= FunctionSignature.getSignatureIntDoubleDoubleBoolean2Boolean(helperIntDoubleDoubleBoolean2Boolean) == "Int:Double:Double:Boolean2Boolean"
    result &= FunctionSignature.getSignatureIntDoubleStringInt2Boolean(helperIntDoubleStringInt2Boolean) == "Int:Double:String:Int2Boolean"
    result &= FunctionSignature.getSignatureIntDoubleStringLong2Boolean(helperIntDoubleStringLong2Boolean) == "Int:Double:String:Long2Boolean"
    result &= FunctionSignature.getSignatureIntDoubleStringDouble2Boolean(helperIntDoubleStringDouble2Boolean) == "Int:Double:String:Double2Boolean"
    result &= FunctionSignature.getSignatureIntDoubleStringString2Boolean(helperIntDoubleStringString2Boolean) == "Int:Double:String:String2Boolean"
    result &= FunctionSignature.getSignatureIntDoubleStringBoolean2Boolean(helperIntDoubleStringBoolean2Boolean) == "Int:Double:String:Boolean2Boolean"
    result &= FunctionSignature.getSignatureIntDoubleBooleanInt2Boolean(helperIntDoubleBooleanInt2Boolean) == "Int:Double:Boolean:Int2Boolean"
    result &= FunctionSignature.getSignatureIntDoubleBooleanLong2Boolean(helperIntDoubleBooleanLong2Boolean) == "Int:Double:Boolean:Long2Boolean"
    result &= FunctionSignature.getSignatureIntDoubleBooleanDouble2Boolean(helperIntDoubleBooleanDouble2Boolean) == "Int:Double:Boolean:Double2Boolean"
    result &= FunctionSignature.getSignatureIntDoubleBooleanString2Boolean(helperIntDoubleBooleanString2Boolean) == "Int:Double:Boolean:String2Boolean"
    result &= FunctionSignature.getSignatureIntDoubleBooleanBoolean2Boolean(helperIntDoubleBooleanBoolean2Boolean) == "Int:Double:Boolean:Boolean2Boolean"
    result &= FunctionSignature.getSignatureIntStringIntInt2Boolean(helperIntStringIntInt2Boolean) == "Int:String:Int:Int2Boolean"
    result &= FunctionSignature.getSignatureIntStringIntLong2Boolean(helperIntStringIntLong2Boolean) == "Int:String:Int:Long2Boolean"
    result &= FunctionSignature.getSignatureIntStringIntDouble2Boolean(helperIntStringIntDouble2Boolean) == "Int:String:Int:Double2Boolean"
    result &= FunctionSignature.getSignatureIntStringIntString2Boolean(helperIntStringIntString2Boolean) == "Int:String:Int:String2Boolean"
    result &= FunctionSignature.getSignatureIntStringIntBoolean2Boolean(helperIntStringIntBoolean2Boolean) == "Int:String:Int:Boolean2Boolean"
    result &= FunctionSignature.getSignatureIntStringLongInt2Boolean(helperIntStringLongInt2Boolean) == "Int:String:Long:Int2Boolean"
    result &= FunctionSignature.getSignatureIntStringLongLong2Boolean(helperIntStringLongLong2Boolean) == "Int:String:Long:Long2Boolean"
    result &= FunctionSignature.getSignatureIntStringLongDouble2Boolean(helperIntStringLongDouble2Boolean) == "Int:String:Long:Double2Boolean"
    result &= FunctionSignature.getSignatureIntStringLongString2Boolean(helperIntStringLongString2Boolean) == "Int:String:Long:String2Boolean"
    result &= FunctionSignature.getSignatureIntStringLongBoolean2Boolean(helperIntStringLongBoolean2Boolean) == "Int:String:Long:Boolean2Boolean"
    result &= FunctionSignature.getSignatureIntStringDoubleInt2Boolean(helperIntStringDoubleInt2Boolean) == "Int:String:Double:Int2Boolean"
    result &= FunctionSignature.getSignatureIntStringDoubleLong2Boolean(helperIntStringDoubleLong2Boolean) == "Int:String:Double:Long2Boolean"
    result &= FunctionSignature.getSignatureIntStringDoubleDouble2Boolean(helperIntStringDoubleDouble2Boolean) == "Int:String:Double:Double2Boolean"
    result &= FunctionSignature.getSignatureIntStringDoubleString2Boolean(helperIntStringDoubleString2Boolean) == "Int:String:Double:String2Boolean"
    result &= FunctionSignature.getSignatureIntStringDoubleBoolean2Boolean(helperIntStringDoubleBoolean2Boolean) == "Int:String:Double:Boolean2Boolean"
    result &= FunctionSignature.getSignatureIntStringStringInt2Boolean(helperIntStringStringInt2Boolean) == "Int:String:String:Int2Boolean"
    result &= FunctionSignature.getSignatureIntStringStringLong2Boolean(helperIntStringStringLong2Boolean) == "Int:String:String:Long2Boolean"
    result &= FunctionSignature.getSignatureIntStringStringDouble2Boolean(helperIntStringStringDouble2Boolean) == "Int:String:String:Double2Boolean"
    result &= FunctionSignature.getSignatureIntStringStringString2Boolean(helperIntStringStringString2Boolean) == "Int:String:String:String2Boolean"
    result &= FunctionSignature.getSignatureIntStringStringBoolean2Boolean(helperIntStringStringBoolean2Boolean) == "Int:String:String:Boolean2Boolean"
    result &= FunctionSignature.getSignatureIntStringBooleanInt2Boolean(helperIntStringBooleanInt2Boolean) == "Int:String:Boolean:Int2Boolean"
    result &= FunctionSignature.getSignatureIntStringBooleanLong2Boolean(helperIntStringBooleanLong2Boolean) == "Int:String:Boolean:Long2Boolean"
    result &= FunctionSignature.getSignatureIntStringBooleanDouble2Boolean(helperIntStringBooleanDouble2Boolean) == "Int:String:Boolean:Double2Boolean"
    result &= FunctionSignature.getSignatureIntStringBooleanString2Boolean(helperIntStringBooleanString2Boolean) == "Int:String:Boolean:String2Boolean"
    result &= FunctionSignature.getSignatureIntStringBooleanBoolean2Boolean(helperIntStringBooleanBoolean2Boolean) == "Int:String:Boolean:Boolean2Boolean"
    result &= FunctionSignature.getSignatureIntBooleanIntInt2Boolean(helperIntBooleanIntInt2Boolean) == "Int:Boolean:Int:Int2Boolean"
    result &= FunctionSignature.getSignatureIntBooleanIntLong2Boolean(helperIntBooleanIntLong2Boolean) == "Int:Boolean:Int:Long2Boolean"
    result &= FunctionSignature.getSignatureIntBooleanIntDouble2Boolean(helperIntBooleanIntDouble2Boolean) == "Int:Boolean:Int:Double2Boolean"
    result &= FunctionSignature.getSignatureIntBooleanIntString2Boolean(helperIntBooleanIntString2Boolean) == "Int:Boolean:Int:String2Boolean"
    result &= FunctionSignature.getSignatureIntBooleanIntBoolean2Boolean(helperIntBooleanIntBoolean2Boolean) == "Int:Boolean:Int:Boolean2Boolean"
    result &= FunctionSignature.getSignatureIntBooleanLongInt2Boolean(helperIntBooleanLongInt2Boolean) == "Int:Boolean:Long:Int2Boolean"
    result &= FunctionSignature.getSignatureIntBooleanLongLong2Boolean(helperIntBooleanLongLong2Boolean) == "Int:Boolean:Long:Long2Boolean"
    result &= FunctionSignature.getSignatureIntBooleanLongDouble2Boolean(helperIntBooleanLongDouble2Boolean) == "Int:Boolean:Long:Double2Boolean"
    result &= FunctionSignature.getSignatureIntBooleanLongString2Boolean(helperIntBooleanLongString2Boolean) == "Int:Boolean:Long:String2Boolean"
    result &= FunctionSignature.getSignatureIntBooleanLongBoolean2Boolean(helperIntBooleanLongBoolean2Boolean) == "Int:Boolean:Long:Boolean2Boolean"
    result &= FunctionSignature.getSignatureIntBooleanDoubleInt2Boolean(helperIntBooleanDoubleInt2Boolean) == "Int:Boolean:Double:Int2Boolean"
    result &= FunctionSignature.getSignatureIntBooleanDoubleLong2Boolean(helperIntBooleanDoubleLong2Boolean) == "Int:Boolean:Double:Long2Boolean"
    result &= FunctionSignature.getSignatureIntBooleanDoubleDouble2Boolean(helperIntBooleanDoubleDouble2Boolean) == "Int:Boolean:Double:Double2Boolean"
    result &= FunctionSignature.getSignatureIntBooleanDoubleString2Boolean(helperIntBooleanDoubleString2Boolean) == "Int:Boolean:Double:String2Boolean"
    result &= FunctionSignature.getSignatureIntBooleanDoubleBoolean2Boolean(helperIntBooleanDoubleBoolean2Boolean) == "Int:Boolean:Double:Boolean2Boolean"
    result &= FunctionSignature.getSignatureIntBooleanStringInt2Boolean(helperIntBooleanStringInt2Boolean) == "Int:Boolean:String:Int2Boolean"
    result &= FunctionSignature.getSignatureIntBooleanStringLong2Boolean(helperIntBooleanStringLong2Boolean) == "Int:Boolean:String:Long2Boolean"
    result &= FunctionSignature.getSignatureIntBooleanStringDouble2Boolean(helperIntBooleanStringDouble2Boolean) == "Int:Boolean:String:Double2Boolean"
    result &= FunctionSignature.getSignatureIntBooleanStringString2Boolean(helperIntBooleanStringString2Boolean) == "Int:Boolean:String:String2Boolean"
    result &= FunctionSignature.getSignatureIntBooleanStringBoolean2Boolean(helperIntBooleanStringBoolean2Boolean) == "Int:Boolean:String:Boolean2Boolean"
    result &= FunctionSignature.getSignatureIntBooleanBooleanInt2Boolean(helperIntBooleanBooleanInt2Boolean) == "Int:Boolean:Boolean:Int2Boolean"
    result &= FunctionSignature.getSignatureIntBooleanBooleanLong2Boolean(helperIntBooleanBooleanLong2Boolean) == "Int:Boolean:Boolean:Long2Boolean"
    result &= FunctionSignature.getSignatureIntBooleanBooleanDouble2Boolean(helperIntBooleanBooleanDouble2Boolean) == "Int:Boolean:Boolean:Double2Boolean"
    result &= FunctionSignature.getSignatureIntBooleanBooleanString2Boolean(helperIntBooleanBooleanString2Boolean) == "Int:Boolean:Boolean:String2Boolean"
    result &= FunctionSignature.getSignatureIntBooleanBooleanBoolean2Boolean(helperIntBooleanBooleanBoolean2Boolean) == "Int:Boolean:Boolean:Boolean2Boolean"
    result &= FunctionSignature.getSignatureLongIntIntInt2Boolean(helperLongIntIntInt2Boolean) == "Long:Int:Int:Int2Boolean"
    result &= FunctionSignature.getSignatureLongIntIntLong2Boolean(helperLongIntIntLong2Boolean) == "Long:Int:Int:Long2Boolean"
    result &= FunctionSignature.getSignatureLongIntIntDouble2Boolean(helperLongIntIntDouble2Boolean) == "Long:Int:Int:Double2Boolean"
    result &= FunctionSignature.getSignatureLongIntIntString2Boolean(helperLongIntIntString2Boolean) == "Long:Int:Int:String2Boolean"
    result &= FunctionSignature.getSignatureLongIntIntBoolean2Boolean(helperLongIntIntBoolean2Boolean) == "Long:Int:Int:Boolean2Boolean"
    result &= FunctionSignature.getSignatureLongIntLongInt2Boolean(helperLongIntLongInt2Boolean) == "Long:Int:Long:Int2Boolean"
    result &= FunctionSignature.getSignatureLongIntLongLong2Boolean(helperLongIntLongLong2Boolean) == "Long:Int:Long:Long2Boolean"
    result &= FunctionSignature.getSignatureLongIntLongDouble2Boolean(helperLongIntLongDouble2Boolean) == "Long:Int:Long:Double2Boolean"
    result &= FunctionSignature.getSignatureLongIntLongString2Boolean(helperLongIntLongString2Boolean) == "Long:Int:Long:String2Boolean"
    result &= FunctionSignature.getSignatureLongIntLongBoolean2Boolean(helperLongIntLongBoolean2Boolean) == "Long:Int:Long:Boolean2Boolean"
    result &= FunctionSignature.getSignatureLongIntDoubleInt2Boolean(helperLongIntDoubleInt2Boolean) == "Long:Int:Double:Int2Boolean"
    result &= FunctionSignature.getSignatureLongIntDoubleLong2Boolean(helperLongIntDoubleLong2Boolean) == "Long:Int:Double:Long2Boolean"
    result &= FunctionSignature.getSignatureLongIntDoubleDouble2Boolean(helperLongIntDoubleDouble2Boolean) == "Long:Int:Double:Double2Boolean"
    result &= FunctionSignature.getSignatureLongIntDoubleString2Boolean(helperLongIntDoubleString2Boolean) == "Long:Int:Double:String2Boolean"
    result &= FunctionSignature.getSignatureLongIntDoubleBoolean2Boolean(helperLongIntDoubleBoolean2Boolean) == "Long:Int:Double:Boolean2Boolean"
    result &= FunctionSignature.getSignatureLongIntStringInt2Boolean(helperLongIntStringInt2Boolean) == "Long:Int:String:Int2Boolean"
    result &= FunctionSignature.getSignatureLongIntStringLong2Boolean(helperLongIntStringLong2Boolean) == "Long:Int:String:Long2Boolean"
    result &= FunctionSignature.getSignatureLongIntStringDouble2Boolean(helperLongIntStringDouble2Boolean) == "Long:Int:String:Double2Boolean"
    result &= FunctionSignature.getSignatureLongIntStringString2Boolean(helperLongIntStringString2Boolean) == "Long:Int:String:String2Boolean"
    result &= FunctionSignature.getSignatureLongIntStringBoolean2Boolean(helperLongIntStringBoolean2Boolean) == "Long:Int:String:Boolean2Boolean"
    result &= FunctionSignature.getSignatureLongIntBooleanInt2Boolean(helperLongIntBooleanInt2Boolean) == "Long:Int:Boolean:Int2Boolean"
    result &= FunctionSignature.getSignatureLongIntBooleanLong2Boolean(helperLongIntBooleanLong2Boolean) == "Long:Int:Boolean:Long2Boolean"
    result &= FunctionSignature.getSignatureLongIntBooleanDouble2Boolean(helperLongIntBooleanDouble2Boolean) == "Long:Int:Boolean:Double2Boolean"
    result &= FunctionSignature.getSignatureLongIntBooleanString2Boolean(helperLongIntBooleanString2Boolean) == "Long:Int:Boolean:String2Boolean"
    result &= FunctionSignature.getSignatureLongIntBooleanBoolean2Boolean(helperLongIntBooleanBoolean2Boolean) == "Long:Int:Boolean:Boolean2Boolean"
    result &= FunctionSignature.getSignatureLongLongIntInt2Boolean(helperLongLongIntInt2Boolean) == "Long:Long:Int:Int2Boolean"
    result &= FunctionSignature.getSignatureLongLongIntLong2Boolean(helperLongLongIntLong2Boolean) == "Long:Long:Int:Long2Boolean"
    result &= FunctionSignature.getSignatureLongLongIntDouble2Boolean(helperLongLongIntDouble2Boolean) == "Long:Long:Int:Double2Boolean"
    result &= FunctionSignature.getSignatureLongLongIntString2Boolean(helperLongLongIntString2Boolean) == "Long:Long:Int:String2Boolean"
    result &= FunctionSignature.getSignatureLongLongIntBoolean2Boolean(helperLongLongIntBoolean2Boolean) == "Long:Long:Int:Boolean2Boolean"
    result &= FunctionSignature.getSignatureLongLongLongInt2Boolean(helperLongLongLongInt2Boolean) == "Long:Long:Long:Int2Boolean"
    result &= FunctionSignature.getSignatureLongLongLongLong2Boolean(helperLongLongLongLong2Boolean) == "Long:Long:Long:Long2Boolean"
    result &= FunctionSignature.getSignatureLongLongLongDouble2Boolean(helperLongLongLongDouble2Boolean) == "Long:Long:Long:Double2Boolean"
    result &= FunctionSignature.getSignatureLongLongLongString2Boolean(helperLongLongLongString2Boolean) == "Long:Long:Long:String2Boolean"
    result &= FunctionSignature.getSignatureLongLongLongBoolean2Boolean(helperLongLongLongBoolean2Boolean) == "Long:Long:Long:Boolean2Boolean"
    result &= FunctionSignature.getSignatureLongLongDoubleInt2Boolean(helperLongLongDoubleInt2Boolean) == "Long:Long:Double:Int2Boolean"
    result &= FunctionSignature.getSignatureLongLongDoubleLong2Boolean(helperLongLongDoubleLong2Boolean) == "Long:Long:Double:Long2Boolean"
    result &= FunctionSignature.getSignatureLongLongDoubleDouble2Boolean(helperLongLongDoubleDouble2Boolean) == "Long:Long:Double:Double2Boolean"
    result &= FunctionSignature.getSignatureLongLongDoubleString2Boolean(helperLongLongDoubleString2Boolean) == "Long:Long:Double:String2Boolean"
    result &= FunctionSignature.getSignatureLongLongDoubleBoolean2Boolean(helperLongLongDoubleBoolean2Boolean) == "Long:Long:Double:Boolean2Boolean"
    result &= FunctionSignature.getSignatureLongLongStringInt2Boolean(helperLongLongStringInt2Boolean) == "Long:Long:String:Int2Boolean"
    result &= FunctionSignature.getSignatureLongLongStringLong2Boolean(helperLongLongStringLong2Boolean) == "Long:Long:String:Long2Boolean"
    result &= FunctionSignature.getSignatureLongLongStringDouble2Boolean(helperLongLongStringDouble2Boolean) == "Long:Long:String:Double2Boolean"
    result &= FunctionSignature.getSignatureLongLongStringString2Boolean(helperLongLongStringString2Boolean) == "Long:Long:String:String2Boolean"
    result &= FunctionSignature.getSignatureLongLongStringBoolean2Boolean(helperLongLongStringBoolean2Boolean) == "Long:Long:String:Boolean2Boolean"
    result &= FunctionSignature.getSignatureLongLongBooleanInt2Boolean(helperLongLongBooleanInt2Boolean) == "Long:Long:Boolean:Int2Boolean"
    result &= FunctionSignature.getSignatureLongLongBooleanLong2Boolean(helperLongLongBooleanLong2Boolean) == "Long:Long:Boolean:Long2Boolean"
    result &= FunctionSignature.getSignatureLongLongBooleanDouble2Boolean(helperLongLongBooleanDouble2Boolean) == "Long:Long:Boolean:Double2Boolean"
    result &= FunctionSignature.getSignatureLongLongBooleanString2Boolean(helperLongLongBooleanString2Boolean) == "Long:Long:Boolean:String2Boolean"
    result &= FunctionSignature.getSignatureLongLongBooleanBoolean2Boolean(helperLongLongBooleanBoolean2Boolean) == "Long:Long:Boolean:Boolean2Boolean"
    result &= FunctionSignature.getSignatureLongDoubleIntInt2Boolean(helperLongDoubleIntInt2Boolean) == "Long:Double:Int:Int2Boolean"
    result &= FunctionSignature.getSignatureLongDoubleIntLong2Boolean(helperLongDoubleIntLong2Boolean) == "Long:Double:Int:Long2Boolean"
    result &= FunctionSignature.getSignatureLongDoubleIntDouble2Boolean(helperLongDoubleIntDouble2Boolean) == "Long:Double:Int:Double2Boolean"
    result &= FunctionSignature.getSignatureLongDoubleIntString2Boolean(helperLongDoubleIntString2Boolean) == "Long:Double:Int:String2Boolean"
    result &= FunctionSignature.getSignatureLongDoubleIntBoolean2Boolean(helperLongDoubleIntBoolean2Boolean) == "Long:Double:Int:Boolean2Boolean"
    result &= FunctionSignature.getSignatureLongDoubleLongInt2Boolean(helperLongDoubleLongInt2Boolean) == "Long:Double:Long:Int2Boolean"
    result &= FunctionSignature.getSignatureLongDoubleLongLong2Boolean(helperLongDoubleLongLong2Boolean) == "Long:Double:Long:Long2Boolean"
    result &= FunctionSignature.getSignatureLongDoubleLongDouble2Boolean(helperLongDoubleLongDouble2Boolean) == "Long:Double:Long:Double2Boolean"
    result &= FunctionSignature.getSignatureLongDoubleLongString2Boolean(helperLongDoubleLongString2Boolean) == "Long:Double:Long:String2Boolean"
    result &= FunctionSignature.getSignatureLongDoubleLongBoolean2Boolean(helperLongDoubleLongBoolean2Boolean) == "Long:Double:Long:Boolean2Boolean"
    result &= FunctionSignature.getSignatureLongDoubleDoubleInt2Boolean(helperLongDoubleDoubleInt2Boolean) == "Long:Double:Double:Int2Boolean"
    result &= FunctionSignature.getSignatureLongDoubleDoubleLong2Boolean(helperLongDoubleDoubleLong2Boolean) == "Long:Double:Double:Long2Boolean"
    result &= FunctionSignature.getSignatureLongDoubleDoubleDouble2Boolean(helperLongDoubleDoubleDouble2Boolean) == "Long:Double:Double:Double2Boolean"
    result &= FunctionSignature.getSignatureLongDoubleDoubleString2Boolean(helperLongDoubleDoubleString2Boolean) == "Long:Double:Double:String2Boolean"
    result &= FunctionSignature.getSignatureLongDoubleDoubleBoolean2Boolean(helperLongDoubleDoubleBoolean2Boolean) == "Long:Double:Double:Boolean2Boolean"
    result &= FunctionSignature.getSignatureLongDoubleStringInt2Boolean(helperLongDoubleStringInt2Boolean) == "Long:Double:String:Int2Boolean"
    result &= FunctionSignature.getSignatureLongDoubleStringLong2Boolean(helperLongDoubleStringLong2Boolean) == "Long:Double:String:Long2Boolean"
    result &= FunctionSignature.getSignatureLongDoubleStringDouble2Boolean(helperLongDoubleStringDouble2Boolean) == "Long:Double:String:Double2Boolean"
    result &= FunctionSignature.getSignatureLongDoubleStringString2Boolean(helperLongDoubleStringString2Boolean) == "Long:Double:String:String2Boolean"
    result &= FunctionSignature.getSignatureLongDoubleStringBoolean2Boolean(helperLongDoubleStringBoolean2Boolean) == "Long:Double:String:Boolean2Boolean"
    result &= FunctionSignature.getSignatureLongDoubleBooleanInt2Boolean(helperLongDoubleBooleanInt2Boolean) == "Long:Double:Boolean:Int2Boolean"
    result &= FunctionSignature.getSignatureLongDoubleBooleanLong2Boolean(helperLongDoubleBooleanLong2Boolean) == "Long:Double:Boolean:Long2Boolean"
    result &= FunctionSignature.getSignatureLongDoubleBooleanDouble2Boolean(helperLongDoubleBooleanDouble2Boolean) == "Long:Double:Boolean:Double2Boolean"
    result &= FunctionSignature.getSignatureLongDoubleBooleanString2Boolean(helperLongDoubleBooleanString2Boolean) == "Long:Double:Boolean:String2Boolean"
    result &= FunctionSignature.getSignatureLongDoubleBooleanBoolean2Boolean(helperLongDoubleBooleanBoolean2Boolean) == "Long:Double:Boolean:Boolean2Boolean"
    result &= FunctionSignature.getSignatureLongStringIntInt2Boolean(helperLongStringIntInt2Boolean) == "Long:String:Int:Int2Boolean"
    result &= FunctionSignature.getSignatureLongStringIntLong2Boolean(helperLongStringIntLong2Boolean) == "Long:String:Int:Long2Boolean"
    result &= FunctionSignature.getSignatureLongStringIntDouble2Boolean(helperLongStringIntDouble2Boolean) == "Long:String:Int:Double2Boolean"
    result &= FunctionSignature.getSignatureLongStringIntString2Boolean(helperLongStringIntString2Boolean) == "Long:String:Int:String2Boolean"
    result &= FunctionSignature.getSignatureLongStringIntBoolean2Boolean(helperLongStringIntBoolean2Boolean) == "Long:String:Int:Boolean2Boolean"
    result &= FunctionSignature.getSignatureLongStringLongInt2Boolean(helperLongStringLongInt2Boolean) == "Long:String:Long:Int2Boolean"
    result &= FunctionSignature.getSignatureLongStringLongLong2Boolean(helperLongStringLongLong2Boolean) == "Long:String:Long:Long2Boolean"
    result &= FunctionSignature.getSignatureLongStringLongDouble2Boolean(helperLongStringLongDouble2Boolean) == "Long:String:Long:Double2Boolean"
    result &= FunctionSignature.getSignatureLongStringLongString2Boolean(helperLongStringLongString2Boolean) == "Long:String:Long:String2Boolean"
    result &= FunctionSignature.getSignatureLongStringLongBoolean2Boolean(helperLongStringLongBoolean2Boolean) == "Long:String:Long:Boolean2Boolean"
    result &= FunctionSignature.getSignatureLongStringDoubleInt2Boolean(helperLongStringDoubleInt2Boolean) == "Long:String:Double:Int2Boolean"
    result &= FunctionSignature.getSignatureLongStringDoubleLong2Boolean(helperLongStringDoubleLong2Boolean) == "Long:String:Double:Long2Boolean"
    result &= FunctionSignature.getSignatureLongStringDoubleDouble2Boolean(helperLongStringDoubleDouble2Boolean) == "Long:String:Double:Double2Boolean"
    result &= FunctionSignature.getSignatureLongStringDoubleString2Boolean(helperLongStringDoubleString2Boolean) == "Long:String:Double:String2Boolean"
    result &= FunctionSignature.getSignatureLongStringDoubleBoolean2Boolean(helperLongStringDoubleBoolean2Boolean) == "Long:String:Double:Boolean2Boolean"
    result &= FunctionSignature.getSignatureLongStringStringInt2Boolean(helperLongStringStringInt2Boolean) == "Long:String:String:Int2Boolean"
    result &= FunctionSignature.getSignatureLongStringStringLong2Boolean(helperLongStringStringLong2Boolean) == "Long:String:String:Long2Boolean"
    result &= FunctionSignature.getSignatureLongStringStringDouble2Boolean(helperLongStringStringDouble2Boolean) == "Long:String:String:Double2Boolean"
    result &= FunctionSignature.getSignatureLongStringStringString2Boolean(helperLongStringStringString2Boolean) == "Long:String:String:String2Boolean"
    result &= FunctionSignature.getSignatureLongStringStringBoolean2Boolean(helperLongStringStringBoolean2Boolean) == "Long:String:String:Boolean2Boolean"
    result &= FunctionSignature.getSignatureLongStringBooleanInt2Boolean(helperLongStringBooleanInt2Boolean) == "Long:String:Boolean:Int2Boolean"
    result &= FunctionSignature.getSignatureLongStringBooleanLong2Boolean(helperLongStringBooleanLong2Boolean) == "Long:String:Boolean:Long2Boolean"
    result &= FunctionSignature.getSignatureLongStringBooleanDouble2Boolean(helperLongStringBooleanDouble2Boolean) == "Long:String:Boolean:Double2Boolean"
    result &= FunctionSignature.getSignatureLongStringBooleanString2Boolean(helperLongStringBooleanString2Boolean) == "Long:String:Boolean:String2Boolean"
    result &= FunctionSignature.getSignatureLongStringBooleanBoolean2Boolean(helperLongStringBooleanBoolean2Boolean) == "Long:String:Boolean:Boolean2Boolean"
    result &= FunctionSignature.getSignatureLongBooleanIntInt2Boolean(helperLongBooleanIntInt2Boolean) == "Long:Boolean:Int:Int2Boolean"
    result &= FunctionSignature.getSignatureLongBooleanIntLong2Boolean(helperLongBooleanIntLong2Boolean) == "Long:Boolean:Int:Long2Boolean"
    result &= FunctionSignature.getSignatureLongBooleanIntDouble2Boolean(helperLongBooleanIntDouble2Boolean) == "Long:Boolean:Int:Double2Boolean"
    result &= FunctionSignature.getSignatureLongBooleanIntString2Boolean(helperLongBooleanIntString2Boolean) == "Long:Boolean:Int:String2Boolean"
    result &= FunctionSignature.getSignatureLongBooleanIntBoolean2Boolean(helperLongBooleanIntBoolean2Boolean) == "Long:Boolean:Int:Boolean2Boolean"
    result &= FunctionSignature.getSignatureLongBooleanLongInt2Boolean(helperLongBooleanLongInt2Boolean) == "Long:Boolean:Long:Int2Boolean"
    result &= FunctionSignature.getSignatureLongBooleanLongLong2Boolean(helperLongBooleanLongLong2Boolean) == "Long:Boolean:Long:Long2Boolean"
    result &= FunctionSignature.getSignatureLongBooleanLongDouble2Boolean(helperLongBooleanLongDouble2Boolean) == "Long:Boolean:Long:Double2Boolean"
    result &= FunctionSignature.getSignatureLongBooleanLongString2Boolean(helperLongBooleanLongString2Boolean) == "Long:Boolean:Long:String2Boolean"
    result &= FunctionSignature.getSignatureLongBooleanLongBoolean2Boolean(helperLongBooleanLongBoolean2Boolean) == "Long:Boolean:Long:Boolean2Boolean"
    result &= FunctionSignature.getSignatureLongBooleanDoubleInt2Boolean(helperLongBooleanDoubleInt2Boolean) == "Long:Boolean:Double:Int2Boolean"
    result &= FunctionSignature.getSignatureLongBooleanDoubleLong2Boolean(helperLongBooleanDoubleLong2Boolean) == "Long:Boolean:Double:Long2Boolean"
    result &= FunctionSignature.getSignatureLongBooleanDoubleDouble2Boolean(helperLongBooleanDoubleDouble2Boolean) == "Long:Boolean:Double:Double2Boolean"
    result &= FunctionSignature.getSignatureLongBooleanDoubleString2Boolean(helperLongBooleanDoubleString2Boolean) == "Long:Boolean:Double:String2Boolean"
    result &= FunctionSignature.getSignatureLongBooleanDoubleBoolean2Boolean(helperLongBooleanDoubleBoolean2Boolean) == "Long:Boolean:Double:Boolean2Boolean"
    result &= FunctionSignature.getSignatureLongBooleanStringInt2Boolean(helperLongBooleanStringInt2Boolean) == "Long:Boolean:String:Int2Boolean"
    result &= FunctionSignature.getSignatureLongBooleanStringLong2Boolean(helperLongBooleanStringLong2Boolean) == "Long:Boolean:String:Long2Boolean"
    result &= FunctionSignature.getSignatureLongBooleanStringDouble2Boolean(helperLongBooleanStringDouble2Boolean) == "Long:Boolean:String:Double2Boolean"
    result &= FunctionSignature.getSignatureLongBooleanStringString2Boolean(helperLongBooleanStringString2Boolean) == "Long:Boolean:String:String2Boolean"
    result &= FunctionSignature.getSignatureLongBooleanStringBoolean2Boolean(helperLongBooleanStringBoolean2Boolean) == "Long:Boolean:String:Boolean2Boolean"
    result &= FunctionSignature.getSignatureLongBooleanBooleanInt2Boolean(helperLongBooleanBooleanInt2Boolean) == "Long:Boolean:Boolean:Int2Boolean"
    result &= FunctionSignature.getSignatureLongBooleanBooleanLong2Boolean(helperLongBooleanBooleanLong2Boolean) == "Long:Boolean:Boolean:Long2Boolean"
    result &= FunctionSignature.getSignatureLongBooleanBooleanDouble2Boolean(helperLongBooleanBooleanDouble2Boolean) == "Long:Boolean:Boolean:Double2Boolean"
    result &= FunctionSignature.getSignatureLongBooleanBooleanString2Boolean(helperLongBooleanBooleanString2Boolean) == "Long:Boolean:Boolean:String2Boolean"
    result &= FunctionSignature.getSignatureLongBooleanBooleanBoolean2Boolean(helperLongBooleanBooleanBoolean2Boolean) == "Long:Boolean:Boolean:Boolean2Boolean"
    result &= FunctionSignature.getSignatureDoubleIntIntInt2Boolean(helperDoubleIntIntInt2Boolean) == "Double:Int:Int:Int2Boolean"
    result &= FunctionSignature.getSignatureDoubleIntIntLong2Boolean(helperDoubleIntIntLong2Boolean) == "Double:Int:Int:Long2Boolean"
    result &= FunctionSignature.getSignatureDoubleIntIntDouble2Boolean(helperDoubleIntIntDouble2Boolean) == "Double:Int:Int:Double2Boolean"
    result &= FunctionSignature.getSignatureDoubleIntIntString2Boolean(helperDoubleIntIntString2Boolean) == "Double:Int:Int:String2Boolean"
    result &= FunctionSignature.getSignatureDoubleIntIntBoolean2Boolean(helperDoubleIntIntBoolean2Boolean) == "Double:Int:Int:Boolean2Boolean"
    result &= FunctionSignature.getSignatureDoubleIntLongInt2Boolean(helperDoubleIntLongInt2Boolean) == "Double:Int:Long:Int2Boolean"
    result &= FunctionSignature.getSignatureDoubleIntLongLong2Boolean(helperDoubleIntLongLong2Boolean) == "Double:Int:Long:Long2Boolean"
    result &= FunctionSignature.getSignatureDoubleIntLongDouble2Boolean(helperDoubleIntLongDouble2Boolean) == "Double:Int:Long:Double2Boolean"
    result &= FunctionSignature.getSignatureDoubleIntLongString2Boolean(helperDoubleIntLongString2Boolean) == "Double:Int:Long:String2Boolean"
    result &= FunctionSignature.getSignatureDoubleIntLongBoolean2Boolean(helperDoubleIntLongBoolean2Boolean) == "Double:Int:Long:Boolean2Boolean"
    result &= FunctionSignature.getSignatureDoubleIntDoubleInt2Boolean(helperDoubleIntDoubleInt2Boolean) == "Double:Int:Double:Int2Boolean"
    result &= FunctionSignature.getSignatureDoubleIntDoubleLong2Boolean(helperDoubleIntDoubleLong2Boolean) == "Double:Int:Double:Long2Boolean"
    result &= FunctionSignature.getSignatureDoubleIntDoubleDouble2Boolean(helperDoubleIntDoubleDouble2Boolean) == "Double:Int:Double:Double2Boolean"
    result &= FunctionSignature.getSignatureDoubleIntDoubleString2Boolean(helperDoubleIntDoubleString2Boolean) == "Double:Int:Double:String2Boolean"
    result &= FunctionSignature.getSignatureDoubleIntDoubleBoolean2Boolean(helperDoubleIntDoubleBoolean2Boolean) == "Double:Int:Double:Boolean2Boolean"
    result &= FunctionSignature.getSignatureDoubleIntStringInt2Boolean(helperDoubleIntStringInt2Boolean) == "Double:Int:String:Int2Boolean"
    result &= FunctionSignature.getSignatureDoubleIntStringLong2Boolean(helperDoubleIntStringLong2Boolean) == "Double:Int:String:Long2Boolean"
    result &= FunctionSignature.getSignatureDoubleIntStringDouble2Boolean(helperDoubleIntStringDouble2Boolean) == "Double:Int:String:Double2Boolean"
    result &= FunctionSignature.getSignatureDoubleIntStringString2Boolean(helperDoubleIntStringString2Boolean) == "Double:Int:String:String2Boolean"
    result &= FunctionSignature.getSignatureDoubleIntStringBoolean2Boolean(helperDoubleIntStringBoolean2Boolean) == "Double:Int:String:Boolean2Boolean"
    result &= FunctionSignature.getSignatureDoubleIntBooleanInt2Boolean(helperDoubleIntBooleanInt2Boolean) == "Double:Int:Boolean:Int2Boolean"
    result &= FunctionSignature.getSignatureDoubleIntBooleanLong2Boolean(helperDoubleIntBooleanLong2Boolean) == "Double:Int:Boolean:Long2Boolean"
    result &= FunctionSignature.getSignatureDoubleIntBooleanDouble2Boolean(helperDoubleIntBooleanDouble2Boolean) == "Double:Int:Boolean:Double2Boolean"
    result &= FunctionSignature.getSignatureDoubleIntBooleanString2Boolean(helperDoubleIntBooleanString2Boolean) == "Double:Int:Boolean:String2Boolean"
    result &= FunctionSignature.getSignatureDoubleIntBooleanBoolean2Boolean(helperDoubleIntBooleanBoolean2Boolean) == "Double:Int:Boolean:Boolean2Boolean"
    result &= FunctionSignature.getSignatureDoubleLongIntInt2Boolean(helperDoubleLongIntInt2Boolean) == "Double:Long:Int:Int2Boolean"
    result &= FunctionSignature.getSignatureDoubleLongIntLong2Boolean(helperDoubleLongIntLong2Boolean) == "Double:Long:Int:Long2Boolean"
    result &= FunctionSignature.getSignatureDoubleLongIntDouble2Boolean(helperDoubleLongIntDouble2Boolean) == "Double:Long:Int:Double2Boolean"
    result &= FunctionSignature.getSignatureDoubleLongIntString2Boolean(helperDoubleLongIntString2Boolean) == "Double:Long:Int:String2Boolean"
    result &= FunctionSignature.getSignatureDoubleLongIntBoolean2Boolean(helperDoubleLongIntBoolean2Boolean) == "Double:Long:Int:Boolean2Boolean"
    result &= FunctionSignature.getSignatureDoubleLongLongInt2Boolean(helperDoubleLongLongInt2Boolean) == "Double:Long:Long:Int2Boolean"
    result &= FunctionSignature.getSignatureDoubleLongLongLong2Boolean(helperDoubleLongLongLong2Boolean) == "Double:Long:Long:Long2Boolean"
    result &= FunctionSignature.getSignatureDoubleLongLongDouble2Boolean(helperDoubleLongLongDouble2Boolean) == "Double:Long:Long:Double2Boolean"
    result &= FunctionSignature.getSignatureDoubleLongLongString2Boolean(helperDoubleLongLongString2Boolean) == "Double:Long:Long:String2Boolean"
    result &= FunctionSignature.getSignatureDoubleLongLongBoolean2Boolean(helperDoubleLongLongBoolean2Boolean) == "Double:Long:Long:Boolean2Boolean"
    result &= FunctionSignature.getSignatureDoubleLongDoubleInt2Boolean(helperDoubleLongDoubleInt2Boolean) == "Double:Long:Double:Int2Boolean"
    result &= FunctionSignature.getSignatureDoubleLongDoubleLong2Boolean(helperDoubleLongDoubleLong2Boolean) == "Double:Long:Double:Long2Boolean"
    result &= FunctionSignature.getSignatureDoubleLongDoubleDouble2Boolean(helperDoubleLongDoubleDouble2Boolean) == "Double:Long:Double:Double2Boolean"
    result &= FunctionSignature.getSignatureDoubleLongDoubleString2Boolean(helperDoubleLongDoubleString2Boolean) == "Double:Long:Double:String2Boolean"
    result &= FunctionSignature.getSignatureDoubleLongDoubleBoolean2Boolean(helperDoubleLongDoubleBoolean2Boolean) == "Double:Long:Double:Boolean2Boolean"
    result &= FunctionSignature.getSignatureDoubleLongStringInt2Boolean(helperDoubleLongStringInt2Boolean) == "Double:Long:String:Int2Boolean"
    result &= FunctionSignature.getSignatureDoubleLongStringLong2Boolean(helperDoubleLongStringLong2Boolean) == "Double:Long:String:Long2Boolean"
    result &= FunctionSignature.getSignatureDoubleLongStringDouble2Boolean(helperDoubleLongStringDouble2Boolean) == "Double:Long:String:Double2Boolean"
    result &= FunctionSignature.getSignatureDoubleLongStringString2Boolean(helperDoubleLongStringString2Boolean) == "Double:Long:String:String2Boolean"
    result &= FunctionSignature.getSignatureDoubleLongStringBoolean2Boolean(helperDoubleLongStringBoolean2Boolean) == "Double:Long:String:Boolean2Boolean"
    result &= FunctionSignature.getSignatureDoubleLongBooleanInt2Boolean(helperDoubleLongBooleanInt2Boolean) == "Double:Long:Boolean:Int2Boolean"
    result &= FunctionSignature.getSignatureDoubleLongBooleanLong2Boolean(helperDoubleLongBooleanLong2Boolean) == "Double:Long:Boolean:Long2Boolean"
    result &= FunctionSignature.getSignatureDoubleLongBooleanDouble2Boolean(helperDoubleLongBooleanDouble2Boolean) == "Double:Long:Boolean:Double2Boolean"
    result &= FunctionSignature.getSignatureDoubleLongBooleanString2Boolean(helperDoubleLongBooleanString2Boolean) == "Double:Long:Boolean:String2Boolean"
    result &= FunctionSignature.getSignatureDoubleLongBooleanBoolean2Boolean(helperDoubleLongBooleanBoolean2Boolean) == "Double:Long:Boolean:Boolean2Boolean"
    result &= FunctionSignature.getSignatureDoubleDoubleIntInt2Boolean(helperDoubleDoubleIntInt2Boolean) == "Double:Double:Int:Int2Boolean"
    result &= FunctionSignature.getSignatureDoubleDoubleIntLong2Boolean(helperDoubleDoubleIntLong2Boolean) == "Double:Double:Int:Long2Boolean"
    result &= FunctionSignature.getSignatureDoubleDoubleIntDouble2Boolean(helperDoubleDoubleIntDouble2Boolean) == "Double:Double:Int:Double2Boolean"
    result &= FunctionSignature.getSignatureDoubleDoubleIntString2Boolean(helperDoubleDoubleIntString2Boolean) == "Double:Double:Int:String2Boolean"
    result &= FunctionSignature.getSignatureDoubleDoubleIntBoolean2Boolean(helperDoubleDoubleIntBoolean2Boolean) == "Double:Double:Int:Boolean2Boolean"
    result &= FunctionSignature.getSignatureDoubleDoubleLongInt2Boolean(helperDoubleDoubleLongInt2Boolean) == "Double:Double:Long:Int2Boolean"
    result &= FunctionSignature.getSignatureDoubleDoubleLongLong2Boolean(helperDoubleDoubleLongLong2Boolean) == "Double:Double:Long:Long2Boolean"
    result &= FunctionSignature.getSignatureDoubleDoubleLongDouble2Boolean(helperDoubleDoubleLongDouble2Boolean) == "Double:Double:Long:Double2Boolean"
    result &= FunctionSignature.getSignatureDoubleDoubleLongString2Boolean(helperDoubleDoubleLongString2Boolean) == "Double:Double:Long:String2Boolean"
    result &= FunctionSignature.getSignatureDoubleDoubleLongBoolean2Boolean(helperDoubleDoubleLongBoolean2Boolean) == "Double:Double:Long:Boolean2Boolean"
    result &= FunctionSignature.getSignatureDoubleDoubleDoubleInt2Boolean(helperDoubleDoubleDoubleInt2Boolean) == "Double:Double:Double:Int2Boolean"
    result &= FunctionSignature.getSignatureDoubleDoubleDoubleLong2Boolean(helperDoubleDoubleDoubleLong2Boolean) == "Double:Double:Double:Long2Boolean"
    result &= FunctionSignature.getSignatureDoubleDoubleDoubleDouble2Boolean(helperDoubleDoubleDoubleDouble2Boolean) == "Double:Double:Double:Double2Boolean"
    result &= FunctionSignature.getSignatureDoubleDoubleDoubleString2Boolean(helperDoubleDoubleDoubleString2Boolean) == "Double:Double:Double:String2Boolean"
    result &= FunctionSignature.getSignatureDoubleDoubleDoubleBoolean2Boolean(helperDoubleDoubleDoubleBoolean2Boolean) == "Double:Double:Double:Boolean2Boolean"
    result &= FunctionSignature.getSignatureDoubleDoubleStringInt2Boolean(helperDoubleDoubleStringInt2Boolean) == "Double:Double:String:Int2Boolean"
    result &= FunctionSignature.getSignatureDoubleDoubleStringLong2Boolean(helperDoubleDoubleStringLong2Boolean) == "Double:Double:String:Long2Boolean"
    result &= FunctionSignature.getSignatureDoubleDoubleStringDouble2Boolean(helperDoubleDoubleStringDouble2Boolean) == "Double:Double:String:Double2Boolean"
    result &= FunctionSignature.getSignatureDoubleDoubleStringString2Boolean(helperDoubleDoubleStringString2Boolean) == "Double:Double:String:String2Boolean"
    result &= FunctionSignature.getSignatureDoubleDoubleStringBoolean2Boolean(helperDoubleDoubleStringBoolean2Boolean) == "Double:Double:String:Boolean2Boolean"
    result &= FunctionSignature.getSignatureDoubleDoubleBooleanInt2Boolean(helperDoubleDoubleBooleanInt2Boolean) == "Double:Double:Boolean:Int2Boolean"
    result &= FunctionSignature.getSignatureDoubleDoubleBooleanLong2Boolean(helperDoubleDoubleBooleanLong2Boolean) == "Double:Double:Boolean:Long2Boolean"
    result &= FunctionSignature.getSignatureDoubleDoubleBooleanDouble2Boolean(helperDoubleDoubleBooleanDouble2Boolean) == "Double:Double:Boolean:Double2Boolean"
    result &= FunctionSignature.getSignatureDoubleDoubleBooleanString2Boolean(helperDoubleDoubleBooleanString2Boolean) == "Double:Double:Boolean:String2Boolean"
    result &= FunctionSignature.getSignatureDoubleDoubleBooleanBoolean2Boolean(helperDoubleDoubleBooleanBoolean2Boolean) == "Double:Double:Boolean:Boolean2Boolean"
    result &= FunctionSignature.getSignatureDoubleStringIntInt2Boolean(helperDoubleStringIntInt2Boolean) == "Double:String:Int:Int2Boolean"
    result &= FunctionSignature.getSignatureDoubleStringIntLong2Boolean(helperDoubleStringIntLong2Boolean) == "Double:String:Int:Long2Boolean"
    result &= FunctionSignature.getSignatureDoubleStringIntDouble2Boolean(helperDoubleStringIntDouble2Boolean) == "Double:String:Int:Double2Boolean"
    result &= FunctionSignature.getSignatureDoubleStringIntString2Boolean(helperDoubleStringIntString2Boolean) == "Double:String:Int:String2Boolean"
    result &= FunctionSignature.getSignatureDoubleStringIntBoolean2Boolean(helperDoubleStringIntBoolean2Boolean) == "Double:String:Int:Boolean2Boolean"
    result &= FunctionSignature.getSignatureDoubleStringLongInt2Boolean(helperDoubleStringLongInt2Boolean) == "Double:String:Long:Int2Boolean"
    result &= FunctionSignature.getSignatureDoubleStringLongLong2Boolean(helperDoubleStringLongLong2Boolean) == "Double:String:Long:Long2Boolean"
    result &= FunctionSignature.getSignatureDoubleStringLongDouble2Boolean(helperDoubleStringLongDouble2Boolean) == "Double:String:Long:Double2Boolean"
    result &= FunctionSignature.getSignatureDoubleStringLongString2Boolean(helperDoubleStringLongString2Boolean) == "Double:String:Long:String2Boolean"
    result &= FunctionSignature.getSignatureDoubleStringLongBoolean2Boolean(helperDoubleStringLongBoolean2Boolean) == "Double:String:Long:Boolean2Boolean"
    result &= FunctionSignature.getSignatureDoubleStringDoubleInt2Boolean(helperDoubleStringDoubleInt2Boolean) == "Double:String:Double:Int2Boolean"
    result &= FunctionSignature.getSignatureDoubleStringDoubleLong2Boolean(helperDoubleStringDoubleLong2Boolean) == "Double:String:Double:Long2Boolean"
    result &= FunctionSignature.getSignatureDoubleStringDoubleDouble2Boolean(helperDoubleStringDoubleDouble2Boolean) == "Double:String:Double:Double2Boolean"
    result &= FunctionSignature.getSignatureDoubleStringDoubleString2Boolean(helperDoubleStringDoubleString2Boolean) == "Double:String:Double:String2Boolean"
    result &= FunctionSignature.getSignatureDoubleStringDoubleBoolean2Boolean(helperDoubleStringDoubleBoolean2Boolean) == "Double:String:Double:Boolean2Boolean"
    result &= FunctionSignature.getSignatureDoubleStringStringInt2Boolean(helperDoubleStringStringInt2Boolean) == "Double:String:String:Int2Boolean"
    result &= FunctionSignature.getSignatureDoubleStringStringLong2Boolean(helperDoubleStringStringLong2Boolean) == "Double:String:String:Long2Boolean"
    result &= FunctionSignature.getSignatureDoubleStringStringDouble2Boolean(helperDoubleStringStringDouble2Boolean) == "Double:String:String:Double2Boolean"
    result &= FunctionSignature.getSignatureDoubleStringStringString2Boolean(helperDoubleStringStringString2Boolean) == "Double:String:String:String2Boolean"
    result &= FunctionSignature.getSignatureDoubleStringStringBoolean2Boolean(helperDoubleStringStringBoolean2Boolean) == "Double:String:String:Boolean2Boolean"
    result &= FunctionSignature.getSignatureDoubleStringBooleanInt2Boolean(helperDoubleStringBooleanInt2Boolean) == "Double:String:Boolean:Int2Boolean"
    result &= FunctionSignature.getSignatureDoubleStringBooleanLong2Boolean(helperDoubleStringBooleanLong2Boolean) == "Double:String:Boolean:Long2Boolean"
    result &= FunctionSignature.getSignatureDoubleStringBooleanDouble2Boolean(helperDoubleStringBooleanDouble2Boolean) == "Double:String:Boolean:Double2Boolean"
    result &= FunctionSignature.getSignatureDoubleStringBooleanString2Boolean(helperDoubleStringBooleanString2Boolean) == "Double:String:Boolean:String2Boolean"
    result &= FunctionSignature.getSignatureDoubleStringBooleanBoolean2Boolean(helperDoubleStringBooleanBoolean2Boolean) == "Double:String:Boolean:Boolean2Boolean"
    result &= FunctionSignature.getSignatureDoubleBooleanIntInt2Boolean(helperDoubleBooleanIntInt2Boolean) == "Double:Boolean:Int:Int2Boolean"
    result &= FunctionSignature.getSignatureDoubleBooleanIntLong2Boolean(helperDoubleBooleanIntLong2Boolean) == "Double:Boolean:Int:Long2Boolean"
    result &= FunctionSignature.getSignatureDoubleBooleanIntDouble2Boolean(helperDoubleBooleanIntDouble2Boolean) == "Double:Boolean:Int:Double2Boolean"
    result &= FunctionSignature.getSignatureDoubleBooleanIntString2Boolean(helperDoubleBooleanIntString2Boolean) == "Double:Boolean:Int:String2Boolean"
    result &= FunctionSignature.getSignatureDoubleBooleanIntBoolean2Boolean(helperDoubleBooleanIntBoolean2Boolean) == "Double:Boolean:Int:Boolean2Boolean"
    result &= FunctionSignature.getSignatureDoubleBooleanLongInt2Boolean(helperDoubleBooleanLongInt2Boolean) == "Double:Boolean:Long:Int2Boolean"
    result &= FunctionSignature.getSignatureDoubleBooleanLongLong2Boolean(helperDoubleBooleanLongLong2Boolean) == "Double:Boolean:Long:Long2Boolean"
    result &= FunctionSignature.getSignatureDoubleBooleanLongDouble2Boolean(helperDoubleBooleanLongDouble2Boolean) == "Double:Boolean:Long:Double2Boolean"
    result &= FunctionSignature.getSignatureDoubleBooleanLongString2Boolean(helperDoubleBooleanLongString2Boolean) == "Double:Boolean:Long:String2Boolean"
    result &= FunctionSignature.getSignatureDoubleBooleanLongBoolean2Boolean(helperDoubleBooleanLongBoolean2Boolean) == "Double:Boolean:Long:Boolean2Boolean"
    result &= FunctionSignature.getSignatureDoubleBooleanDoubleInt2Boolean(helperDoubleBooleanDoubleInt2Boolean) == "Double:Boolean:Double:Int2Boolean"
    result &= FunctionSignature.getSignatureDoubleBooleanDoubleLong2Boolean(helperDoubleBooleanDoubleLong2Boolean) == "Double:Boolean:Double:Long2Boolean"
    result &= FunctionSignature.getSignatureDoubleBooleanDoubleDouble2Boolean(helperDoubleBooleanDoubleDouble2Boolean) == "Double:Boolean:Double:Double2Boolean"
    result &= FunctionSignature.getSignatureDoubleBooleanDoubleString2Boolean(helperDoubleBooleanDoubleString2Boolean) == "Double:Boolean:Double:String2Boolean"
    result &= FunctionSignature.getSignatureDoubleBooleanDoubleBoolean2Boolean(helperDoubleBooleanDoubleBoolean2Boolean) == "Double:Boolean:Double:Boolean2Boolean"
    result &= FunctionSignature.getSignatureDoubleBooleanStringInt2Boolean(helperDoubleBooleanStringInt2Boolean) == "Double:Boolean:String:Int2Boolean"
    result &= FunctionSignature.getSignatureDoubleBooleanStringLong2Boolean(helperDoubleBooleanStringLong2Boolean) == "Double:Boolean:String:Long2Boolean"
    result &= FunctionSignature.getSignatureDoubleBooleanStringDouble2Boolean(helperDoubleBooleanStringDouble2Boolean) == "Double:Boolean:String:Double2Boolean"
    result &= FunctionSignature.getSignatureDoubleBooleanStringString2Boolean(helperDoubleBooleanStringString2Boolean) == "Double:Boolean:String:String2Boolean"
    result &= FunctionSignature.getSignatureDoubleBooleanStringBoolean2Boolean(helperDoubleBooleanStringBoolean2Boolean) == "Double:Boolean:String:Boolean2Boolean"
    result &= FunctionSignature.getSignatureDoubleBooleanBooleanInt2Boolean(helperDoubleBooleanBooleanInt2Boolean) == "Double:Boolean:Boolean:Int2Boolean"
    result &= FunctionSignature.getSignatureDoubleBooleanBooleanLong2Boolean(helperDoubleBooleanBooleanLong2Boolean) == "Double:Boolean:Boolean:Long2Boolean"
    result &= FunctionSignature.getSignatureDoubleBooleanBooleanDouble2Boolean(helperDoubleBooleanBooleanDouble2Boolean) == "Double:Boolean:Boolean:Double2Boolean"
    result &= FunctionSignature.getSignatureDoubleBooleanBooleanString2Boolean(helperDoubleBooleanBooleanString2Boolean) == "Double:Boolean:Boolean:String2Boolean"
    result &= FunctionSignature.getSignatureDoubleBooleanBooleanBoolean2Boolean(helperDoubleBooleanBooleanBoolean2Boolean) == "Double:Boolean:Boolean:Boolean2Boolean"
    result &= FunctionSignature.getSignatureStringIntIntInt2Boolean(helperStringIntIntInt2Boolean) == "String:Int:Int:Int2Boolean"
    result &= FunctionSignature.getSignatureStringIntIntLong2Boolean(helperStringIntIntLong2Boolean) == "String:Int:Int:Long2Boolean"
    result &= FunctionSignature.getSignatureStringIntIntDouble2Boolean(helperStringIntIntDouble2Boolean) == "String:Int:Int:Double2Boolean"
    result &= FunctionSignature.getSignatureStringIntIntString2Boolean(helperStringIntIntString2Boolean) == "String:Int:Int:String2Boolean"
    result &= FunctionSignature.getSignatureStringIntIntBoolean2Boolean(helperStringIntIntBoolean2Boolean) == "String:Int:Int:Boolean2Boolean"
    result &= FunctionSignature.getSignatureStringIntLongInt2Boolean(helperStringIntLongInt2Boolean) == "String:Int:Long:Int2Boolean"
    result &= FunctionSignature.getSignatureStringIntLongLong2Boolean(helperStringIntLongLong2Boolean) == "String:Int:Long:Long2Boolean"
    result &= FunctionSignature.getSignatureStringIntLongDouble2Boolean(helperStringIntLongDouble2Boolean) == "String:Int:Long:Double2Boolean"
    result &= FunctionSignature.getSignatureStringIntLongString2Boolean(helperStringIntLongString2Boolean) == "String:Int:Long:String2Boolean"
    result &= FunctionSignature.getSignatureStringIntLongBoolean2Boolean(helperStringIntLongBoolean2Boolean) == "String:Int:Long:Boolean2Boolean"
    result &= FunctionSignature.getSignatureStringIntDoubleInt2Boolean(helperStringIntDoubleInt2Boolean) == "String:Int:Double:Int2Boolean"
    result &= FunctionSignature.getSignatureStringIntDoubleLong2Boolean(helperStringIntDoubleLong2Boolean) == "String:Int:Double:Long2Boolean"
    result &= FunctionSignature.getSignatureStringIntDoubleDouble2Boolean(helperStringIntDoubleDouble2Boolean) == "String:Int:Double:Double2Boolean"
    result &= FunctionSignature.getSignatureStringIntDoubleString2Boolean(helperStringIntDoubleString2Boolean) == "String:Int:Double:String2Boolean"
    result &= FunctionSignature.getSignatureStringIntDoubleBoolean2Boolean(helperStringIntDoubleBoolean2Boolean) == "String:Int:Double:Boolean2Boolean"
    result &= FunctionSignature.getSignatureStringIntStringInt2Boolean(helperStringIntStringInt2Boolean) == "String:Int:String:Int2Boolean"
    result &= FunctionSignature.getSignatureStringIntStringLong2Boolean(helperStringIntStringLong2Boolean) == "String:Int:String:Long2Boolean"
    result &= FunctionSignature.getSignatureStringIntStringDouble2Boolean(helperStringIntStringDouble2Boolean) == "String:Int:String:Double2Boolean"
    result &= FunctionSignature.getSignatureStringIntStringString2Boolean(helperStringIntStringString2Boolean) == "String:Int:String:String2Boolean"
    result &= FunctionSignature.getSignatureStringIntStringBoolean2Boolean(helperStringIntStringBoolean2Boolean) == "String:Int:String:Boolean2Boolean"
    result &= FunctionSignature.getSignatureStringIntBooleanInt2Boolean(helperStringIntBooleanInt2Boolean) == "String:Int:Boolean:Int2Boolean"
    result &= FunctionSignature.getSignatureStringIntBooleanLong2Boolean(helperStringIntBooleanLong2Boolean) == "String:Int:Boolean:Long2Boolean"
    result &= FunctionSignature.getSignatureStringIntBooleanDouble2Boolean(helperStringIntBooleanDouble2Boolean) == "String:Int:Boolean:Double2Boolean"
    result &= FunctionSignature.getSignatureStringIntBooleanString2Boolean(helperStringIntBooleanString2Boolean) == "String:Int:Boolean:String2Boolean"
    result &= FunctionSignature.getSignatureStringIntBooleanBoolean2Boolean(helperStringIntBooleanBoolean2Boolean) == "String:Int:Boolean:Boolean2Boolean"
    result &= FunctionSignature.getSignatureStringLongIntInt2Boolean(helperStringLongIntInt2Boolean) == "String:Long:Int:Int2Boolean"
    result &= FunctionSignature.getSignatureStringLongIntLong2Boolean(helperStringLongIntLong2Boolean) == "String:Long:Int:Long2Boolean"
    result &= FunctionSignature.getSignatureStringLongIntDouble2Boolean(helperStringLongIntDouble2Boolean) == "String:Long:Int:Double2Boolean"
    result &= FunctionSignature.getSignatureStringLongIntString2Boolean(helperStringLongIntString2Boolean) == "String:Long:Int:String2Boolean"
    result &= FunctionSignature.getSignatureStringLongIntBoolean2Boolean(helperStringLongIntBoolean2Boolean) == "String:Long:Int:Boolean2Boolean"
    result &= FunctionSignature.getSignatureStringLongLongInt2Boolean(helperStringLongLongInt2Boolean) == "String:Long:Long:Int2Boolean"
    result &= FunctionSignature.getSignatureStringLongLongLong2Boolean(helperStringLongLongLong2Boolean) == "String:Long:Long:Long2Boolean"
    result &= FunctionSignature.getSignatureStringLongLongDouble2Boolean(helperStringLongLongDouble2Boolean) == "String:Long:Long:Double2Boolean"
    result &= FunctionSignature.getSignatureStringLongLongString2Boolean(helperStringLongLongString2Boolean) == "String:Long:Long:String2Boolean"
    result &= FunctionSignature.getSignatureStringLongLongBoolean2Boolean(helperStringLongLongBoolean2Boolean) == "String:Long:Long:Boolean2Boolean"
    result &= FunctionSignature.getSignatureStringLongDoubleInt2Boolean(helperStringLongDoubleInt2Boolean) == "String:Long:Double:Int2Boolean"
    result &= FunctionSignature.getSignatureStringLongDoubleLong2Boolean(helperStringLongDoubleLong2Boolean) == "String:Long:Double:Long2Boolean"
    result &= FunctionSignature.getSignatureStringLongDoubleDouble2Boolean(helperStringLongDoubleDouble2Boolean) == "String:Long:Double:Double2Boolean"
    result &= FunctionSignature.getSignatureStringLongDoubleString2Boolean(helperStringLongDoubleString2Boolean) == "String:Long:Double:String2Boolean"
    result &= FunctionSignature.getSignatureStringLongDoubleBoolean2Boolean(helperStringLongDoubleBoolean2Boolean) == "String:Long:Double:Boolean2Boolean"
    result &= FunctionSignature.getSignatureStringLongStringInt2Boolean(helperStringLongStringInt2Boolean) == "String:Long:String:Int2Boolean"
    result &= FunctionSignature.getSignatureStringLongStringLong2Boolean(helperStringLongStringLong2Boolean) == "String:Long:String:Long2Boolean"
    result &= FunctionSignature.getSignatureStringLongStringDouble2Boolean(helperStringLongStringDouble2Boolean) == "String:Long:String:Double2Boolean"
    result &= FunctionSignature.getSignatureStringLongStringString2Boolean(helperStringLongStringString2Boolean) == "String:Long:String:String2Boolean"
    result &= FunctionSignature.getSignatureStringLongStringBoolean2Boolean(helperStringLongStringBoolean2Boolean) == "String:Long:String:Boolean2Boolean"
    result &= FunctionSignature.getSignatureStringLongBooleanInt2Boolean(helperStringLongBooleanInt2Boolean) == "String:Long:Boolean:Int2Boolean"
    result &= FunctionSignature.getSignatureStringLongBooleanLong2Boolean(helperStringLongBooleanLong2Boolean) == "String:Long:Boolean:Long2Boolean"
    result &= FunctionSignature.getSignatureStringLongBooleanDouble2Boolean(helperStringLongBooleanDouble2Boolean) == "String:Long:Boolean:Double2Boolean"
    result &= FunctionSignature.getSignatureStringLongBooleanString2Boolean(helperStringLongBooleanString2Boolean) == "String:Long:Boolean:String2Boolean"
    result &= FunctionSignature.getSignatureStringLongBooleanBoolean2Boolean(helperStringLongBooleanBoolean2Boolean) == "String:Long:Boolean:Boolean2Boolean"
    result &= FunctionSignature.getSignatureStringDoubleIntInt2Boolean(helperStringDoubleIntInt2Boolean) == "String:Double:Int:Int2Boolean"
    result &= FunctionSignature.getSignatureStringDoubleIntLong2Boolean(helperStringDoubleIntLong2Boolean) == "String:Double:Int:Long2Boolean"
    result &= FunctionSignature.getSignatureStringDoubleIntDouble2Boolean(helperStringDoubleIntDouble2Boolean) == "String:Double:Int:Double2Boolean"
    result &= FunctionSignature.getSignatureStringDoubleIntString2Boolean(helperStringDoubleIntString2Boolean) == "String:Double:Int:String2Boolean"
    result &= FunctionSignature.getSignatureStringDoubleIntBoolean2Boolean(helperStringDoubleIntBoolean2Boolean) == "String:Double:Int:Boolean2Boolean"
    result &= FunctionSignature.getSignatureStringDoubleLongInt2Boolean(helperStringDoubleLongInt2Boolean) == "String:Double:Long:Int2Boolean"
    result &= FunctionSignature.getSignatureStringDoubleLongLong2Boolean(helperStringDoubleLongLong2Boolean) == "String:Double:Long:Long2Boolean"
    result &= FunctionSignature.getSignatureStringDoubleLongDouble2Boolean(helperStringDoubleLongDouble2Boolean) == "String:Double:Long:Double2Boolean"
    result &= FunctionSignature.getSignatureStringDoubleLongString2Boolean(helperStringDoubleLongString2Boolean) == "String:Double:Long:String2Boolean"
    result &= FunctionSignature.getSignatureStringDoubleLongBoolean2Boolean(helperStringDoubleLongBoolean2Boolean) == "String:Double:Long:Boolean2Boolean"
    result &= FunctionSignature.getSignatureStringDoubleDoubleInt2Boolean(helperStringDoubleDoubleInt2Boolean) == "String:Double:Double:Int2Boolean"
    result &= FunctionSignature.getSignatureStringDoubleDoubleLong2Boolean(helperStringDoubleDoubleLong2Boolean) == "String:Double:Double:Long2Boolean"
    result &= FunctionSignature.getSignatureStringDoubleDoubleDouble2Boolean(helperStringDoubleDoubleDouble2Boolean) == "String:Double:Double:Double2Boolean"
    result &= FunctionSignature.getSignatureStringDoubleDoubleString2Boolean(helperStringDoubleDoubleString2Boolean) == "String:Double:Double:String2Boolean"
    result &= FunctionSignature.getSignatureStringDoubleDoubleBoolean2Boolean(helperStringDoubleDoubleBoolean2Boolean) == "String:Double:Double:Boolean2Boolean"
    result &= FunctionSignature.getSignatureStringDoubleStringInt2Boolean(helperStringDoubleStringInt2Boolean) == "String:Double:String:Int2Boolean"
    result &= FunctionSignature.getSignatureStringDoubleStringLong2Boolean(helperStringDoubleStringLong2Boolean) == "String:Double:String:Long2Boolean"
    result &= FunctionSignature.getSignatureStringDoubleStringDouble2Boolean(helperStringDoubleStringDouble2Boolean) == "String:Double:String:Double2Boolean"
    result &= FunctionSignature.getSignatureStringDoubleStringString2Boolean(helperStringDoubleStringString2Boolean) == "String:Double:String:String2Boolean"
    result &= FunctionSignature.getSignatureStringDoubleStringBoolean2Boolean(helperStringDoubleStringBoolean2Boolean) == "String:Double:String:Boolean2Boolean"
    result &= FunctionSignature.getSignatureStringDoubleBooleanInt2Boolean(helperStringDoubleBooleanInt2Boolean) == "String:Double:Boolean:Int2Boolean"
    result &= FunctionSignature.getSignatureStringDoubleBooleanLong2Boolean(helperStringDoubleBooleanLong2Boolean) == "String:Double:Boolean:Long2Boolean"
    result &= FunctionSignature.getSignatureStringDoubleBooleanDouble2Boolean(helperStringDoubleBooleanDouble2Boolean) == "String:Double:Boolean:Double2Boolean"
    result &= FunctionSignature.getSignatureStringDoubleBooleanString2Boolean(helperStringDoubleBooleanString2Boolean) == "String:Double:Boolean:String2Boolean"
    result &= FunctionSignature.getSignatureStringDoubleBooleanBoolean2Boolean(helperStringDoubleBooleanBoolean2Boolean) == "String:Double:Boolean:Boolean2Boolean"
    result &= FunctionSignature.getSignatureStringStringIntInt2Boolean(helperStringStringIntInt2Boolean) == "String:String:Int:Int2Boolean"
    result &= FunctionSignature.getSignatureStringStringIntLong2Boolean(helperStringStringIntLong2Boolean) == "String:String:Int:Long2Boolean"
    result &= FunctionSignature.getSignatureStringStringIntDouble2Boolean(helperStringStringIntDouble2Boolean) == "String:String:Int:Double2Boolean"
    result &= FunctionSignature.getSignatureStringStringIntString2Boolean(helperStringStringIntString2Boolean) == "String:String:Int:String2Boolean"
    result &= FunctionSignature.getSignatureStringStringIntBoolean2Boolean(helperStringStringIntBoolean2Boolean) == "String:String:Int:Boolean2Boolean"
    result &= FunctionSignature.getSignatureStringStringLongInt2Boolean(helperStringStringLongInt2Boolean) == "String:String:Long:Int2Boolean"
    result &= FunctionSignature.getSignatureStringStringLongLong2Boolean(helperStringStringLongLong2Boolean) == "String:String:Long:Long2Boolean"
    result &= FunctionSignature.getSignatureStringStringLongDouble2Boolean(helperStringStringLongDouble2Boolean) == "String:String:Long:Double2Boolean"
    result &= FunctionSignature.getSignatureStringStringLongString2Boolean(helperStringStringLongString2Boolean) == "String:String:Long:String2Boolean"
    result &= FunctionSignature.getSignatureStringStringLongBoolean2Boolean(helperStringStringLongBoolean2Boolean) == "String:String:Long:Boolean2Boolean"
    result &= FunctionSignature.getSignatureStringStringDoubleInt2Boolean(helperStringStringDoubleInt2Boolean) == "String:String:Double:Int2Boolean"
    result &= FunctionSignature.getSignatureStringStringDoubleLong2Boolean(helperStringStringDoubleLong2Boolean) == "String:String:Double:Long2Boolean"
    result &= FunctionSignature.getSignatureStringStringDoubleDouble2Boolean(helperStringStringDoubleDouble2Boolean) == "String:String:Double:Double2Boolean"
    result &= FunctionSignature.getSignatureStringStringDoubleString2Boolean(helperStringStringDoubleString2Boolean) == "String:String:Double:String2Boolean"
    result &= FunctionSignature.getSignatureStringStringDoubleBoolean2Boolean(helperStringStringDoubleBoolean2Boolean) == "String:String:Double:Boolean2Boolean"
    result &= FunctionSignature.getSignatureStringStringStringInt2Boolean(helperStringStringStringInt2Boolean) == "String:String:String:Int2Boolean"
    result &= FunctionSignature.getSignatureStringStringStringLong2Boolean(helperStringStringStringLong2Boolean) == "String:String:String:Long2Boolean"
    result &= FunctionSignature.getSignatureStringStringStringDouble2Boolean(helperStringStringStringDouble2Boolean) == "String:String:String:Double2Boolean"
    result &= FunctionSignature.getSignatureStringStringStringString2Boolean(helperStringStringStringString2Boolean) == "String:String:String:String2Boolean"
    result &= FunctionSignature.getSignatureStringStringStringBoolean2Boolean(helperStringStringStringBoolean2Boolean) == "String:String:String:Boolean2Boolean"
    result &= FunctionSignature.getSignatureStringStringBooleanInt2Boolean(helperStringStringBooleanInt2Boolean) == "String:String:Boolean:Int2Boolean"
    result &= FunctionSignature.getSignatureStringStringBooleanLong2Boolean(helperStringStringBooleanLong2Boolean) == "String:String:Boolean:Long2Boolean"
    result &= FunctionSignature.getSignatureStringStringBooleanDouble2Boolean(helperStringStringBooleanDouble2Boolean) == "String:String:Boolean:Double2Boolean"
    result &= FunctionSignature.getSignatureStringStringBooleanString2Boolean(helperStringStringBooleanString2Boolean) == "String:String:Boolean:String2Boolean"
    result &= FunctionSignature.getSignatureStringStringBooleanBoolean2Boolean(helperStringStringBooleanBoolean2Boolean) == "String:String:Boolean:Boolean2Boolean"
    result &= FunctionSignature.getSignatureStringBooleanIntInt2Boolean(helperStringBooleanIntInt2Boolean) == "String:Boolean:Int:Int2Boolean"
    result &= FunctionSignature.getSignatureStringBooleanIntLong2Boolean(helperStringBooleanIntLong2Boolean) == "String:Boolean:Int:Long2Boolean"
    result &= FunctionSignature.getSignatureStringBooleanIntDouble2Boolean(helperStringBooleanIntDouble2Boolean) == "String:Boolean:Int:Double2Boolean"
    result &= FunctionSignature.getSignatureStringBooleanIntString2Boolean(helperStringBooleanIntString2Boolean) == "String:Boolean:Int:String2Boolean"
    result &= FunctionSignature.getSignatureStringBooleanIntBoolean2Boolean(helperStringBooleanIntBoolean2Boolean) == "String:Boolean:Int:Boolean2Boolean"
    result &= FunctionSignature.getSignatureStringBooleanLongInt2Boolean(helperStringBooleanLongInt2Boolean) == "String:Boolean:Long:Int2Boolean"
    result &= FunctionSignature.getSignatureStringBooleanLongLong2Boolean(helperStringBooleanLongLong2Boolean) == "String:Boolean:Long:Long2Boolean"
    result &= FunctionSignature.getSignatureStringBooleanLongDouble2Boolean(helperStringBooleanLongDouble2Boolean) == "String:Boolean:Long:Double2Boolean"
    result &= FunctionSignature.getSignatureStringBooleanLongString2Boolean(helperStringBooleanLongString2Boolean) == "String:Boolean:Long:String2Boolean"
    result &= FunctionSignature.getSignatureStringBooleanLongBoolean2Boolean(helperStringBooleanLongBoolean2Boolean) == "String:Boolean:Long:Boolean2Boolean"
    result &= FunctionSignature.getSignatureStringBooleanDoubleInt2Boolean(helperStringBooleanDoubleInt2Boolean) == "String:Boolean:Double:Int2Boolean"
    result &= FunctionSignature.getSignatureStringBooleanDoubleLong2Boolean(helperStringBooleanDoubleLong2Boolean) == "String:Boolean:Double:Long2Boolean"
    result &= FunctionSignature.getSignatureStringBooleanDoubleDouble2Boolean(helperStringBooleanDoubleDouble2Boolean) == "String:Boolean:Double:Double2Boolean"
    result &= FunctionSignature.getSignatureStringBooleanDoubleString2Boolean(helperStringBooleanDoubleString2Boolean) == "String:Boolean:Double:String2Boolean"
    result &= FunctionSignature.getSignatureStringBooleanDoubleBoolean2Boolean(helperStringBooleanDoubleBoolean2Boolean) == "String:Boolean:Double:Boolean2Boolean"
    result &= FunctionSignature.getSignatureStringBooleanStringInt2Boolean(helperStringBooleanStringInt2Boolean) == "String:Boolean:String:Int2Boolean"
    result &= FunctionSignature.getSignatureStringBooleanStringLong2Boolean(helperStringBooleanStringLong2Boolean) == "String:Boolean:String:Long2Boolean"
    result &= FunctionSignature.getSignatureStringBooleanStringDouble2Boolean(helperStringBooleanStringDouble2Boolean) == "String:Boolean:String:Double2Boolean"
    result &= FunctionSignature.getSignatureStringBooleanStringString2Boolean(helperStringBooleanStringString2Boolean) == "String:Boolean:String:String2Boolean"
    result &= FunctionSignature.getSignatureStringBooleanStringBoolean2Boolean(helperStringBooleanStringBoolean2Boolean) == "String:Boolean:String:Boolean2Boolean"
    result &= FunctionSignature.getSignatureStringBooleanBooleanInt2Boolean(helperStringBooleanBooleanInt2Boolean) == "String:Boolean:Boolean:Int2Boolean"
    result &= FunctionSignature.getSignatureStringBooleanBooleanLong2Boolean(helperStringBooleanBooleanLong2Boolean) == "String:Boolean:Boolean:Long2Boolean"
    result &= FunctionSignature.getSignatureStringBooleanBooleanDouble2Boolean(helperStringBooleanBooleanDouble2Boolean) == "String:Boolean:Boolean:Double2Boolean"
    result &= FunctionSignature.getSignatureStringBooleanBooleanString2Boolean(helperStringBooleanBooleanString2Boolean) == "String:Boolean:Boolean:String2Boolean"
    result &= FunctionSignature.getSignatureStringBooleanBooleanBoolean2Boolean(helperStringBooleanBooleanBoolean2Boolean) == "String:Boolean:Boolean:Boolean2Boolean"
    result &= FunctionSignature.getSignatureBooleanIntIntInt2Boolean(helperBooleanIntIntInt2Boolean) == "Boolean:Int:Int:Int2Boolean"
    result &= FunctionSignature.getSignatureBooleanIntIntLong2Boolean(helperBooleanIntIntLong2Boolean) == "Boolean:Int:Int:Long2Boolean"
    result &= FunctionSignature.getSignatureBooleanIntIntDouble2Boolean(helperBooleanIntIntDouble2Boolean) == "Boolean:Int:Int:Double2Boolean"
    result &= FunctionSignature.getSignatureBooleanIntIntString2Boolean(helperBooleanIntIntString2Boolean) == "Boolean:Int:Int:String2Boolean"
    result &= FunctionSignature.getSignatureBooleanIntIntBoolean2Boolean(helperBooleanIntIntBoolean2Boolean) == "Boolean:Int:Int:Boolean2Boolean"
    result &= FunctionSignature.getSignatureBooleanIntLongInt2Boolean(helperBooleanIntLongInt2Boolean) == "Boolean:Int:Long:Int2Boolean"
    result &= FunctionSignature.getSignatureBooleanIntLongLong2Boolean(helperBooleanIntLongLong2Boolean) == "Boolean:Int:Long:Long2Boolean"
    result &= FunctionSignature.getSignatureBooleanIntLongDouble2Boolean(helperBooleanIntLongDouble2Boolean) == "Boolean:Int:Long:Double2Boolean"
    result &= FunctionSignature.getSignatureBooleanIntLongString2Boolean(helperBooleanIntLongString2Boolean) == "Boolean:Int:Long:String2Boolean"
    result &= FunctionSignature.getSignatureBooleanIntLongBoolean2Boolean(helperBooleanIntLongBoolean2Boolean) == "Boolean:Int:Long:Boolean2Boolean"
    result &= FunctionSignature.getSignatureBooleanIntDoubleInt2Boolean(helperBooleanIntDoubleInt2Boolean) == "Boolean:Int:Double:Int2Boolean"
    result &= FunctionSignature.getSignatureBooleanIntDoubleLong2Boolean(helperBooleanIntDoubleLong2Boolean) == "Boolean:Int:Double:Long2Boolean"
    result &= FunctionSignature.getSignatureBooleanIntDoubleDouble2Boolean(helperBooleanIntDoubleDouble2Boolean) == "Boolean:Int:Double:Double2Boolean"
    result &= FunctionSignature.getSignatureBooleanIntDoubleString2Boolean(helperBooleanIntDoubleString2Boolean) == "Boolean:Int:Double:String2Boolean"
    result &= FunctionSignature.getSignatureBooleanIntDoubleBoolean2Boolean(helperBooleanIntDoubleBoolean2Boolean) == "Boolean:Int:Double:Boolean2Boolean"
    result &= FunctionSignature.getSignatureBooleanIntStringInt2Boolean(helperBooleanIntStringInt2Boolean) == "Boolean:Int:String:Int2Boolean"
    result &= FunctionSignature.getSignatureBooleanIntStringLong2Boolean(helperBooleanIntStringLong2Boolean) == "Boolean:Int:String:Long2Boolean"
    result &= FunctionSignature.getSignatureBooleanIntStringDouble2Boolean(helperBooleanIntStringDouble2Boolean) == "Boolean:Int:String:Double2Boolean"
    result &= FunctionSignature.getSignatureBooleanIntStringString2Boolean(helperBooleanIntStringString2Boolean) == "Boolean:Int:String:String2Boolean"
    result &= FunctionSignature.getSignatureBooleanIntStringBoolean2Boolean(helperBooleanIntStringBoolean2Boolean) == "Boolean:Int:String:Boolean2Boolean"
    result &= FunctionSignature.getSignatureBooleanIntBooleanInt2Boolean(helperBooleanIntBooleanInt2Boolean) == "Boolean:Int:Boolean:Int2Boolean"
    result &= FunctionSignature.getSignatureBooleanIntBooleanLong2Boolean(helperBooleanIntBooleanLong2Boolean) == "Boolean:Int:Boolean:Long2Boolean"
    result &= FunctionSignature.getSignatureBooleanIntBooleanDouble2Boolean(helperBooleanIntBooleanDouble2Boolean) == "Boolean:Int:Boolean:Double2Boolean"
    result &= FunctionSignature.getSignatureBooleanIntBooleanString2Boolean(helperBooleanIntBooleanString2Boolean) == "Boolean:Int:Boolean:String2Boolean"
    result &= FunctionSignature.getSignatureBooleanIntBooleanBoolean2Boolean(helperBooleanIntBooleanBoolean2Boolean) == "Boolean:Int:Boolean:Boolean2Boolean"
    result &= FunctionSignature.getSignatureBooleanLongIntInt2Boolean(helperBooleanLongIntInt2Boolean) == "Boolean:Long:Int:Int2Boolean"
    result &= FunctionSignature.getSignatureBooleanLongIntLong2Boolean(helperBooleanLongIntLong2Boolean) == "Boolean:Long:Int:Long2Boolean"
    result &= FunctionSignature.getSignatureBooleanLongIntDouble2Boolean(helperBooleanLongIntDouble2Boolean) == "Boolean:Long:Int:Double2Boolean"
    result &= FunctionSignature.getSignatureBooleanLongIntString2Boolean(helperBooleanLongIntString2Boolean) == "Boolean:Long:Int:String2Boolean"
    result &= FunctionSignature.getSignatureBooleanLongIntBoolean2Boolean(helperBooleanLongIntBoolean2Boolean) == "Boolean:Long:Int:Boolean2Boolean"
    result &= FunctionSignature.getSignatureBooleanLongLongInt2Boolean(helperBooleanLongLongInt2Boolean) == "Boolean:Long:Long:Int2Boolean"
    result &= FunctionSignature.getSignatureBooleanLongLongLong2Boolean(helperBooleanLongLongLong2Boolean) == "Boolean:Long:Long:Long2Boolean"
    result &= FunctionSignature.getSignatureBooleanLongLongDouble2Boolean(helperBooleanLongLongDouble2Boolean) == "Boolean:Long:Long:Double2Boolean"
    result &= FunctionSignature.getSignatureBooleanLongLongString2Boolean(helperBooleanLongLongString2Boolean) == "Boolean:Long:Long:String2Boolean"
    result &= FunctionSignature.getSignatureBooleanLongLongBoolean2Boolean(helperBooleanLongLongBoolean2Boolean) == "Boolean:Long:Long:Boolean2Boolean"
    result &= FunctionSignature.getSignatureBooleanLongDoubleInt2Boolean(helperBooleanLongDoubleInt2Boolean) == "Boolean:Long:Double:Int2Boolean"
    result &= FunctionSignature.getSignatureBooleanLongDoubleLong2Boolean(helperBooleanLongDoubleLong2Boolean) == "Boolean:Long:Double:Long2Boolean"
    result &= FunctionSignature.getSignatureBooleanLongDoubleDouble2Boolean(helperBooleanLongDoubleDouble2Boolean) == "Boolean:Long:Double:Double2Boolean"
    result &= FunctionSignature.getSignatureBooleanLongDoubleString2Boolean(helperBooleanLongDoubleString2Boolean) == "Boolean:Long:Double:String2Boolean"
    result &= FunctionSignature.getSignatureBooleanLongDoubleBoolean2Boolean(helperBooleanLongDoubleBoolean2Boolean) == "Boolean:Long:Double:Boolean2Boolean"
    result &= FunctionSignature.getSignatureBooleanLongStringInt2Boolean(helperBooleanLongStringInt2Boolean) == "Boolean:Long:String:Int2Boolean"
    result &= FunctionSignature.getSignatureBooleanLongStringLong2Boolean(helperBooleanLongStringLong2Boolean) == "Boolean:Long:String:Long2Boolean"
    result &= FunctionSignature.getSignatureBooleanLongStringDouble2Boolean(helperBooleanLongStringDouble2Boolean) == "Boolean:Long:String:Double2Boolean"
    result &= FunctionSignature.getSignatureBooleanLongStringString2Boolean(helperBooleanLongStringString2Boolean) == "Boolean:Long:String:String2Boolean"
    result &= FunctionSignature.getSignatureBooleanLongStringBoolean2Boolean(helperBooleanLongStringBoolean2Boolean) == "Boolean:Long:String:Boolean2Boolean"
    result &= FunctionSignature.getSignatureBooleanLongBooleanInt2Boolean(helperBooleanLongBooleanInt2Boolean) == "Boolean:Long:Boolean:Int2Boolean"
    result &= FunctionSignature.getSignatureBooleanLongBooleanLong2Boolean(helperBooleanLongBooleanLong2Boolean) == "Boolean:Long:Boolean:Long2Boolean"
    result &= FunctionSignature.getSignatureBooleanLongBooleanDouble2Boolean(helperBooleanLongBooleanDouble2Boolean) == "Boolean:Long:Boolean:Double2Boolean"
    result &= FunctionSignature.getSignatureBooleanLongBooleanString2Boolean(helperBooleanLongBooleanString2Boolean) == "Boolean:Long:Boolean:String2Boolean"
    result &= FunctionSignature.getSignatureBooleanLongBooleanBoolean2Boolean(helperBooleanLongBooleanBoolean2Boolean) == "Boolean:Long:Boolean:Boolean2Boolean"
    result &= FunctionSignature.getSignatureBooleanDoubleIntInt2Boolean(helperBooleanDoubleIntInt2Boolean) == "Boolean:Double:Int:Int2Boolean"
    result &= FunctionSignature.getSignatureBooleanDoubleIntLong2Boolean(helperBooleanDoubleIntLong2Boolean) == "Boolean:Double:Int:Long2Boolean"
    result &= FunctionSignature.getSignatureBooleanDoubleIntDouble2Boolean(helperBooleanDoubleIntDouble2Boolean) == "Boolean:Double:Int:Double2Boolean"
    result &= FunctionSignature.getSignatureBooleanDoubleIntString2Boolean(helperBooleanDoubleIntString2Boolean) == "Boolean:Double:Int:String2Boolean"
    result &= FunctionSignature.getSignatureBooleanDoubleIntBoolean2Boolean(helperBooleanDoubleIntBoolean2Boolean) == "Boolean:Double:Int:Boolean2Boolean"
    result &= FunctionSignature.getSignatureBooleanDoubleLongInt2Boolean(helperBooleanDoubleLongInt2Boolean) == "Boolean:Double:Long:Int2Boolean"
    result &= FunctionSignature.getSignatureBooleanDoubleLongLong2Boolean(helperBooleanDoubleLongLong2Boolean) == "Boolean:Double:Long:Long2Boolean"
    result &= FunctionSignature.getSignatureBooleanDoubleLongDouble2Boolean(helperBooleanDoubleLongDouble2Boolean) == "Boolean:Double:Long:Double2Boolean"
    result &= FunctionSignature.getSignatureBooleanDoubleLongString2Boolean(helperBooleanDoubleLongString2Boolean) == "Boolean:Double:Long:String2Boolean"
    result &= FunctionSignature.getSignatureBooleanDoubleLongBoolean2Boolean(helperBooleanDoubleLongBoolean2Boolean) == "Boolean:Double:Long:Boolean2Boolean"
    result &= FunctionSignature.getSignatureBooleanDoubleDoubleInt2Boolean(helperBooleanDoubleDoubleInt2Boolean) == "Boolean:Double:Double:Int2Boolean"
    result &= FunctionSignature.getSignatureBooleanDoubleDoubleLong2Boolean(helperBooleanDoubleDoubleLong2Boolean) == "Boolean:Double:Double:Long2Boolean"
    result &= FunctionSignature.getSignatureBooleanDoubleDoubleDouble2Boolean(helperBooleanDoubleDoubleDouble2Boolean) == "Boolean:Double:Double:Double2Boolean"
    result &= FunctionSignature.getSignatureBooleanDoubleDoubleString2Boolean(helperBooleanDoubleDoubleString2Boolean) == "Boolean:Double:Double:String2Boolean"
    result &= FunctionSignature.getSignatureBooleanDoubleDoubleBoolean2Boolean(helperBooleanDoubleDoubleBoolean2Boolean) == "Boolean:Double:Double:Boolean2Boolean"
    result &= FunctionSignature.getSignatureBooleanDoubleStringInt2Boolean(helperBooleanDoubleStringInt2Boolean) == "Boolean:Double:String:Int2Boolean"
    result &= FunctionSignature.getSignatureBooleanDoubleStringLong2Boolean(helperBooleanDoubleStringLong2Boolean) == "Boolean:Double:String:Long2Boolean"
    result &= FunctionSignature.getSignatureBooleanDoubleStringDouble2Boolean(helperBooleanDoubleStringDouble2Boolean) == "Boolean:Double:String:Double2Boolean"
    result &= FunctionSignature.getSignatureBooleanDoubleStringString2Boolean(helperBooleanDoubleStringString2Boolean) == "Boolean:Double:String:String2Boolean"
    result &= FunctionSignature.getSignatureBooleanDoubleStringBoolean2Boolean(helperBooleanDoubleStringBoolean2Boolean) == "Boolean:Double:String:Boolean2Boolean"
    result &= FunctionSignature.getSignatureBooleanDoubleBooleanInt2Boolean(helperBooleanDoubleBooleanInt2Boolean) == "Boolean:Double:Boolean:Int2Boolean"
    result &= FunctionSignature.getSignatureBooleanDoubleBooleanLong2Boolean(helperBooleanDoubleBooleanLong2Boolean) == "Boolean:Double:Boolean:Long2Boolean"
    result &= FunctionSignature.getSignatureBooleanDoubleBooleanDouble2Boolean(helperBooleanDoubleBooleanDouble2Boolean) == "Boolean:Double:Boolean:Double2Boolean"
    result &= FunctionSignature.getSignatureBooleanDoubleBooleanString2Boolean(helperBooleanDoubleBooleanString2Boolean) == "Boolean:Double:Boolean:String2Boolean"
    result &= FunctionSignature.getSignatureBooleanDoubleBooleanBoolean2Boolean(helperBooleanDoubleBooleanBoolean2Boolean) == "Boolean:Double:Boolean:Boolean2Boolean"
    result &= FunctionSignature.getSignatureBooleanStringIntInt2Boolean(helperBooleanStringIntInt2Boolean) == "Boolean:String:Int:Int2Boolean"
    result &= FunctionSignature.getSignatureBooleanStringIntLong2Boolean(helperBooleanStringIntLong2Boolean) == "Boolean:String:Int:Long2Boolean"
    result &= FunctionSignature.getSignatureBooleanStringIntDouble2Boolean(helperBooleanStringIntDouble2Boolean) == "Boolean:String:Int:Double2Boolean"
    result &= FunctionSignature.getSignatureBooleanStringIntString2Boolean(helperBooleanStringIntString2Boolean) == "Boolean:String:Int:String2Boolean"
    result &= FunctionSignature.getSignatureBooleanStringIntBoolean2Boolean(helperBooleanStringIntBoolean2Boolean) == "Boolean:String:Int:Boolean2Boolean"
    result &= FunctionSignature.getSignatureBooleanStringLongInt2Boolean(helperBooleanStringLongInt2Boolean) == "Boolean:String:Long:Int2Boolean"
    result &= FunctionSignature.getSignatureBooleanStringLongLong2Boolean(helperBooleanStringLongLong2Boolean) == "Boolean:String:Long:Long2Boolean"
    result &= FunctionSignature.getSignatureBooleanStringLongDouble2Boolean(helperBooleanStringLongDouble2Boolean) == "Boolean:String:Long:Double2Boolean"
    result &= FunctionSignature.getSignatureBooleanStringLongString2Boolean(helperBooleanStringLongString2Boolean) == "Boolean:String:Long:String2Boolean"
    result &= FunctionSignature.getSignatureBooleanStringLongBoolean2Boolean(helperBooleanStringLongBoolean2Boolean) == "Boolean:String:Long:Boolean2Boolean"
    result &= FunctionSignature.getSignatureBooleanStringDoubleInt2Boolean(helperBooleanStringDoubleInt2Boolean) == "Boolean:String:Double:Int2Boolean"
    result &= FunctionSignature.getSignatureBooleanStringDoubleLong2Boolean(helperBooleanStringDoubleLong2Boolean) == "Boolean:String:Double:Long2Boolean"
    result &= FunctionSignature.getSignatureBooleanStringDoubleDouble2Boolean(helperBooleanStringDoubleDouble2Boolean) == "Boolean:String:Double:Double2Boolean"
    result &= FunctionSignature.getSignatureBooleanStringDoubleString2Boolean(helperBooleanStringDoubleString2Boolean) == "Boolean:String:Double:String2Boolean"
    result &= FunctionSignature.getSignatureBooleanStringDoubleBoolean2Boolean(helperBooleanStringDoubleBoolean2Boolean) == "Boolean:String:Double:Boolean2Boolean"
    result &= FunctionSignature.getSignatureBooleanStringStringInt2Boolean(helperBooleanStringStringInt2Boolean) == "Boolean:String:String:Int2Boolean"
    result &= FunctionSignature.getSignatureBooleanStringStringLong2Boolean(helperBooleanStringStringLong2Boolean) == "Boolean:String:String:Long2Boolean"
    result &= FunctionSignature.getSignatureBooleanStringStringDouble2Boolean(helperBooleanStringStringDouble2Boolean) == "Boolean:String:String:Double2Boolean"
    result &= FunctionSignature.getSignatureBooleanStringStringString2Boolean(helperBooleanStringStringString2Boolean) == "Boolean:String:String:String2Boolean"
    result &= FunctionSignature.getSignatureBooleanStringStringBoolean2Boolean(helperBooleanStringStringBoolean2Boolean) == "Boolean:String:String:Boolean2Boolean"
    result &= FunctionSignature.getSignatureBooleanStringBooleanInt2Boolean(helperBooleanStringBooleanInt2Boolean) == "Boolean:String:Boolean:Int2Boolean"
    result &= FunctionSignature.getSignatureBooleanStringBooleanLong2Boolean(helperBooleanStringBooleanLong2Boolean) == "Boolean:String:Boolean:Long2Boolean"
    result &= FunctionSignature.getSignatureBooleanStringBooleanDouble2Boolean(helperBooleanStringBooleanDouble2Boolean) == "Boolean:String:Boolean:Double2Boolean"
    result &= FunctionSignature.getSignatureBooleanStringBooleanString2Boolean(helperBooleanStringBooleanString2Boolean) == "Boolean:String:Boolean:String2Boolean"
    result &= FunctionSignature.getSignatureBooleanStringBooleanBoolean2Boolean(helperBooleanStringBooleanBoolean2Boolean) == "Boolean:String:Boolean:Boolean2Boolean"
    result &= FunctionSignature.getSignatureBooleanBooleanIntInt2Boolean(helperBooleanBooleanIntInt2Boolean) == "Boolean:Boolean:Int:Int2Boolean"
    result &= FunctionSignature.getSignatureBooleanBooleanIntLong2Boolean(helperBooleanBooleanIntLong2Boolean) == "Boolean:Boolean:Int:Long2Boolean"
    result &= FunctionSignature.getSignatureBooleanBooleanIntDouble2Boolean(helperBooleanBooleanIntDouble2Boolean) == "Boolean:Boolean:Int:Double2Boolean"
    result &= FunctionSignature.getSignatureBooleanBooleanIntString2Boolean(helperBooleanBooleanIntString2Boolean) == "Boolean:Boolean:Int:String2Boolean"
    result &= FunctionSignature.getSignatureBooleanBooleanIntBoolean2Boolean(helperBooleanBooleanIntBoolean2Boolean) == "Boolean:Boolean:Int:Boolean2Boolean"
    result &= FunctionSignature.getSignatureBooleanBooleanLongInt2Boolean(helperBooleanBooleanLongInt2Boolean) == "Boolean:Boolean:Long:Int2Boolean"
    result &= FunctionSignature.getSignatureBooleanBooleanLongLong2Boolean(helperBooleanBooleanLongLong2Boolean) == "Boolean:Boolean:Long:Long2Boolean"
    result &= FunctionSignature.getSignatureBooleanBooleanLongDouble2Boolean(helperBooleanBooleanLongDouble2Boolean) == "Boolean:Boolean:Long:Double2Boolean"
    result &= FunctionSignature.getSignatureBooleanBooleanLongString2Boolean(helperBooleanBooleanLongString2Boolean) == "Boolean:Boolean:Long:String2Boolean"
    result &= FunctionSignature.getSignatureBooleanBooleanLongBoolean2Boolean(helperBooleanBooleanLongBoolean2Boolean) == "Boolean:Boolean:Long:Boolean2Boolean"
    result &= FunctionSignature.getSignatureBooleanBooleanDoubleInt2Boolean(helperBooleanBooleanDoubleInt2Boolean) == "Boolean:Boolean:Double:Int2Boolean"
    result &= FunctionSignature.getSignatureBooleanBooleanDoubleLong2Boolean(helperBooleanBooleanDoubleLong2Boolean) == "Boolean:Boolean:Double:Long2Boolean"
    result &= FunctionSignature.getSignatureBooleanBooleanDoubleDouble2Boolean(helperBooleanBooleanDoubleDouble2Boolean) == "Boolean:Boolean:Double:Double2Boolean"
    result &= FunctionSignature.getSignatureBooleanBooleanDoubleString2Boolean(helperBooleanBooleanDoubleString2Boolean) == "Boolean:Boolean:Double:String2Boolean"
    result &= FunctionSignature.getSignatureBooleanBooleanDoubleBoolean2Boolean(helperBooleanBooleanDoubleBoolean2Boolean) == "Boolean:Boolean:Double:Boolean2Boolean"
    result &= FunctionSignature.getSignatureBooleanBooleanStringInt2Boolean(helperBooleanBooleanStringInt2Boolean) == "Boolean:Boolean:String:Int2Boolean"
    result &= FunctionSignature.getSignatureBooleanBooleanStringLong2Boolean(helperBooleanBooleanStringLong2Boolean) == "Boolean:Boolean:String:Long2Boolean"
    result &= FunctionSignature.getSignatureBooleanBooleanStringDouble2Boolean(helperBooleanBooleanStringDouble2Boolean) == "Boolean:Boolean:String:Double2Boolean"
    result &= FunctionSignature.getSignatureBooleanBooleanStringString2Boolean(helperBooleanBooleanStringString2Boolean) == "Boolean:Boolean:String:String2Boolean"
    result &= FunctionSignature.getSignatureBooleanBooleanStringBoolean2Boolean(helperBooleanBooleanStringBoolean2Boolean) == "Boolean:Boolean:String:Boolean2Boolean"
    result &= FunctionSignature.getSignatureBooleanBooleanBooleanInt2Boolean(helperBooleanBooleanBooleanInt2Boolean) == "Boolean:Boolean:Boolean:Int2Boolean"
    result &= FunctionSignature.getSignatureBooleanBooleanBooleanLong2Boolean(helperBooleanBooleanBooleanLong2Boolean) == "Boolean:Boolean:Boolean:Long2Boolean"
    result &= FunctionSignature.getSignatureBooleanBooleanBooleanDouble2Boolean(helperBooleanBooleanBooleanDouble2Boolean) == "Boolean:Boolean:Boolean:Double2Boolean"
    result &= FunctionSignature.getSignatureBooleanBooleanBooleanString2Boolean(helperBooleanBooleanBooleanString2Boolean) == "Boolean:Boolean:Boolean:String2Boolean"
    result &= FunctionSignature.getSignatureBooleanBooleanBooleanBoolean2Boolean(helperBooleanBooleanBooleanBoolean2Boolean) == "Boolean:Boolean:Boolean:Boolean2Boolean"
    assert(result)
  }
}
