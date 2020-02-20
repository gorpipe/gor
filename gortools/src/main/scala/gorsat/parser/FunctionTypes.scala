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

import org.gorpipe.gor.ColumnValueProvider

object FunctionTypes {
  type sFun = ColumnValueProvider => String
  type dFun = ColumnValueProvider => Double
  type iFun = ColumnValueProvider => Int
  type bFun = ColumnValueProvider => Boolean
  type aFun = ColumnValueProvider => Any
  type lFun = ColumnValueProvider => Long

  val Empty: String = "e"
  val StringVal: String = "sv"
  val DoubleVal: String = "dv"
  val IntVal: String = "iv"
  val BooleanVal: String = "bv"
  val LongVal: String = "lv"
  val StringFun: String = "String"
  val DoubleFun: String = "Double"
  val IntFun: String = "Int"
  val BooleanFun: String = "Boolean"
  val AnyFun: String = "af"
  val LongFun: String = "Long"
  val StringList: String = "sl"
  val ArgumentSeparator: String = ":"
  val ReturnSeparator: String = "2"

  def getArgumentTypesFromSignature(sig: String): Array[String] = {
    val parts = sig.split(ReturnSeparator)
    if(parts(0) == Empty)
      Array[String]()
    else
      parts(0).split(ArgumentSeparator)
  }

  def getReturnTypeFromSignature(sig: String): String = {
    val parts = sig.split(ReturnSeparator)
    parts(1)
  }

  def getSignature(args: List[String], ret: String): String = {
    val prefix = if(args.isEmpty) Empty else args.mkString(ArgumentSeparator)
    prefix + ReturnSeparator + ret
  }

  def dFunToLambda(l: dFun): CvpDoubleLambda = {
    cvp: ColumnValueProvider => {
      l(cvp)
    }
  }

  def iFunToLambda(l: iFun): CvpIntegerLambda = {
    cvp: ColumnValueProvider => {
      l(cvp)
    }
  }

  def lFunToLambda(l: lFun): CvpLongLambda = {
    cvp: ColumnValueProvider => {
      l(cvp)
    }
  }

  def sFunToLambda(l: sFun): CvpStringLambda = {
    cvp: ColumnValueProvider => {
      l(cvp)
    }
  }

  def bFunToLambda(l: bFun): CvpBooleanLambda = {
    cvp: ColumnValueProvider => {
      l(cvp)
    }
  }
}
