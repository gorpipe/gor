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
import gorsat.parser.FunctionTypes._
import org.gorpipe.exceptions.GorDataException

import scala.collection.mutable
import scala.math.BigInt

object TypeConversionFunctions {
  def register(functions: FunctionRegistry): Unit = {
    functions.register("ROUND", getSignatureDouble2Int(round), round _)
    functions.register("STR", getSignatureDouble2String(double2String), double2String _)
    functions.register("STR", getSignatureInt2String(int2String), int2String _)
    functions.register("STRING", getSignatureDouble2String(double2String), double2String _)
    functions.register("STRING", getSignatureInt2String(int2String), int2String _)
    functions.register("BASE26", getSignatureInt2String(base26), base26 _)
    functions.register("BASEPN", getSignatureInt2String(basePn), basePn _)
    functions.register("FLOAT", getSignatureString2Double(string2Float), string2Float _)
    functions.register("FLOAT", getSignatureInt2Double(int2Float), int2Float _)
    functions.register("FLOAT", getSignatureLong2Double(long2Float), long2Float _)
    functions.register("FLOAT", getSignatureDouble2Double(float2Float), float2Float _)
    functions.register("FLOAT", getSignatureStringDouble2Double(string2FloatWithDefault), string2FloatWithDefault _)
    functions.register("NUMBER", getSignatureString2Double(string2Float), string2Float _)
    functions.register("NUMBER", getSignatureDouble2Double(float2Float), float2Float _)
    functions.register("INT", getSignatureString2Int(string2Int), string2Int _)
    functions.register("INT", getSignatureDouble2Int(double2Int), double2Int _)
    functions.register("INT", getSignatureLong2Int(long2Int), long2Int _)
    functions.register("LONG", getSignatureDouble2Long(double2Long), double2Long _)
    functions.register("LONG", getSignatureString2Long(string2Long), string2Long _)
    functions.register("LONG", getSignatureLong2Long(long2Long), long2Long _)
    functions.register("LONG", getSignatureInt2Long(int2Long), int2Long _)
    functions.register("FORM", getSignatureDoubleIntInt2String(form), form _)
    functions.register("EFORM", getSignatureDoubleIntInt2String(eform), eform _)
    functions.register("GFORM", getSignatureDoubleIntInt2String(gform), gform _)
    functions.register("ISINT", getSignatureString2Boolean(isInt), isInt _)
    functions.register("ISLONG", getSignatureString2Boolean(isLong), isLong _)
    functions.register("ISFLOAT", getSignatureString2Boolean(isFloat), isFloat _)
  }

  def isInt(ex1: sFun): bFun = {
    cvp => {
      val str = ex1(cvp)
      var b = if (str == "") false else true
      try {
        str.toInt
      } catch {
        case _: Exception => b = false
      }
      b
    }
  }

  def isLong(ex1: sFun): bFun = {
    cvp => {
      val str = ex1(cvp)
      var b = if (str == "") false else true
      try {
        str.toLong
      } catch {
        case _: Exception => b = false
      }
      b
    }
  }

  def isFloat(ex1: sFun): bFun = {
    cvp => {
      val str = ex1(cvp)
      str.nonEmpty && {
        try {
          if (str.length > 1) {
            str.toDouble
            true
          } else str.charAt(0) >= '0' && str.charAt(0) <= '9'
        } catch {
          case _: NumberFormatException => false
        }
      }
    }
  }

  def eform(ex1: dFun, ex2: iFun, ex3: iFun): sFun = {
    cvp => {
      val fs = ParseUtilities.eString(ex2(cvp), ex3(cvp))
      ParseUtilities.formNum(ex1(cvp), fs)
    }
  }

  def gform(ex1: dFun, ex2: iFun, ex3: iFun): sFun = {
    cvp => {
      val fs = ParseUtilities.gString(ex2(cvp), ex3(cvp))
      ParseUtilities.formNum(ex1(cvp), fs)
    }
  }

  def form(ex1: dFun, ex2: iFun, ex3: iFun): sFun = {
    cvp => {
      val fs = ParseUtilities.fString(ex2(cvp), ex3(cvp))
      ParseUtilities.formNum(ex1(cvp), fs)
    }
  }

  def string2Int(ex: sFun): iFun = {
    cvp => {
      ex(cvp).toInt
    }
  }

  def int2String(ex1: iFun): sFun = {
    cvp => ex1(cvp).toString
  }

  def double2String(ex1: dFun): sFun = {
    cvp => ex1(cvp).toString
  }

  def double2Int(ex: dFun): iFun = {
    cvp => {
      val d = ex(cvp)
      if (d > Int.MaxValue || d < Int.MinValue) {
        throw new GorDataException(s"$d is too large for an int value")
      }
      d.toInt
    }
  }

  def long2Int(ex: lFun): iFun = {
    cvp => {
      val l = ex(cvp)
      if (l > Int.MaxValue || l < Int.MinValue) {
        throw new GorDataException(s"$l is too large for an int value")
      }
      l.toInt
    }
  }

  def int2Long(ex: iFun): lFun = {
    cvp => {
      ex(cvp)
    }
  }

  def long2Long(ex: lFun): lFun = {
    cvp => {
      ex(cvp)
    }
  }

  def double2Long(ex: dFun): lFun = {
    cvp => {
      val d = ex(cvp)
      if (d > Long.MaxValue || d < Long.MinValue) {
        throw new GorDataException(s"$d is too large for a long value")
      }
      d.toLong
    }
  }

  def string2Long(ex: sFun): lFun = {
    cvp => {
      ex(cvp).toLong
    }
  }

  def base26(ex1: iFun): sFun = {
    cvp => BigInt(ex1(cvp)).toString(26)
  }

  def basePn(ex1: iFun): sFun = {
    cvp => {
      // 26 = Number of english upper case letters A-Z.
      val temp = BigInt(ex1(cvp) - 1).toString(26)
      val base = new mutable.StringBuilder("AAAAAAA")
      var i = 0
      while (i < temp.length) {
        base.setCharAt(6 - i, (65 + Character.digit(temp.charAt(temp.length - i - 1), 26) ).toChar)
        i += 1
      }
      base.toString
    }
  }

  def string2FloatWithDefault(ex1: sFun, ex2: dFun): dFun = {
    cvp => {
      try {
        ex1(cvp).toDouble
      } catch {
        case _: NumberFormatException => ex2(cvp)
      }
    }
  }

  def int2Float(ex: iFun): dFun = {
    cvp =>
      ex(cvp).toInt
  }
  def long2Float(ex: lFun): dFun = {
    cvp =>
      ex(cvp).toDouble
  }
  def float2Float(ex: dFun): dFun = {
    cvp =>
      ex(cvp)
  }
  def string2Float(ex: sFun): dFun = {
    cvp => {
      val s = ex(cvp)
      try {
        s.toDouble
      } catch {
        case _: NumberFormatException => throw new GorDataException(s"'$s' is not a valid number")
      }
    }
  }

  def round(ex: dFun): iFun = {
    cvp => {
      scala.math.round(ex(cvp)).toInt
    }
  }
}
