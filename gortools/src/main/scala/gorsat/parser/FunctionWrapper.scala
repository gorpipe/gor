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

import gorsat.parser.FunctionTypes._
import org.gorpipe.exceptions.GorParsingException

import scala.collection.JavaConverters._

/**
  * A FunctionWrapper wraps a function that can be used in expressions
  * parsed and evaluated by ParseArith.
  * @param name The name of the function
  */
case class FunctionWrapper(name: String, signature: String, myFunction: Any ) {
  val expectedArgs = FunctionTypes.getArgumentTypesFromSignature(signature).toList

  lazy val mangledName: String = name + "_" + signature
  lazy val returnType: String = getReturnTypeFromSignature(signature)

  def call[R](owner: ParseArith, args: List[TypedExpression] = List.empty): R = {
    callImpl[R](owner, args, expectedArgs)
  }

  def call[R](owner: ParseArith, args: java.util.List[TypedExpression]): R = {
    val scalaArgs: List[TypedExpression] = args.asScala.toList
    call(owner, scalaArgs)
  }

  private type Reducer[A, R] = (ParseArith, A, List[TypedExpression], List[String]) => R

  private case class ReducerSet[R](
    s: Reducer[sFun, R], 
    i: Reducer[iFun, R], 
    d: Reducer[dFun, R],
    l: Reducer[lFun, R],
    b: Reducer[bFun, R],
    sl: Reducer[List[String], R]
  ) {}

  private def reduceArgs[A, R](owner: ParseArith, args: List[TypedExpression], argTypes: List[String], next: ReducerSet[R]): R = {
    val arg1 = args.head
    val arg1Type = argTypes.head
    arg1Type match {
      case FunctionTypes.DoubleFun =>
        arg1.expression match {
          case s: FunctionTypes.dFun =>
            next.d(owner, s, args.tail, argTypes.tail)
          case j: CvpDoubleLambda =>
            next.d(owner, j.evaluate, args.tail, argTypes.tail)
        }
      case FunctionTypes.IntFun =>
        arg1.expression match {
          case s: FunctionTypes.iFun =>
            next.i(owner, s, args.tail, argTypes.tail)
          case j: CvpIntegerLambda =>
            next.i(owner, j.evaluate, args.tail, argTypes.tail)
        }
      case FunctionTypes.StringFun =>
        arg1.expression match {
          case s: FunctionTypes.sFun =>
            next.s(owner, s, args.tail, argTypes.tail)
          case j: CvpStringLambda =>
            next.s(owner, j.evaluate, args.tail, argTypes.tail)
        }
      case FunctionTypes.BooleanFun =>
        arg1.expression match {
          case s: FunctionTypes.bFun =>
            next.b(owner, s, args.tail, argTypes.tail)
          case j: CvpBooleanLambda =>
            next.b(owner, j.evaluate, args.tail, argTypes.tail)
        }
      case FunctionTypes.LongFun =>
        arg1.expression match {
          case s: FunctionTypes.lFun =>
            next.l(owner, s, args.tail, argTypes.tail)
          case j: CvpLongLambda =>
            next.l(owner, j.evaluate, args.tail, argTypes.tail)
        }
      case FunctionTypes.StringList =>
        arg1.expression match {
          case s: List[String] =>
            next.sl(owner, s, args.tail, argTypes.tail)
        }
    }
  }

  protected def callImpl[R]
    (owner: ParseArith, args: List[TypedExpression], argTypes: List[String]): R =
  {
    if(args.isEmpty && argTypes.isEmpty) {
      val f = myFunction.asInstanceOf[ParseArith => R]
      f(owner)
    } else {
      val rs = ReducerSet(call1[sFun, R], call1[iFun, R], call1[dFun, R], call1[lFun, R], call1[bFun, R], call1[List[String], R])
      reduceArgs(owner, args, argTypes, rs )
    }
  }

  private def call1[A1, R](owner: ParseArith, arg1: A1, args: List[TypedExpression], argTypes: List[String]): R = {
    if(args.isEmpty && argTypes.isEmpty) {
      val f = myFunction.asInstanceOf[(ParseArith, A1) => R]
      f(owner, arg1)
    } else {
      def next[A2, R0](owner: ParseArith, arg2: A2, args: List[TypedExpression], argTypes: List[String]): R0 = {
        call2[A1, A2, R0](owner, arg1, arg2, args, argTypes)
      }
      val rs = ReducerSet(next[sFun, R], next[iFun, R], next[dFun, R], next[lFun, R], next[bFun, R], next[List[String], R])
      reduceArgs(owner, args, argTypes, rs)
    }
  }

  private def call2[A1, A2, R](owner: ParseArith, arg1: A1, arg2: A2, args: List[TypedExpression], argTypes: List[String]): R = {
    if(args.isEmpty && argTypes.isEmpty) {
      val f = myFunction.asInstanceOf[(ParseArith, A1, A2) => R]
      f(owner, arg1, arg2)
    } else {
      def next[A3, R0](owner: ParseArith, arg3: A3, args: List[TypedExpression], argTypes: List[String]): R0 = {
        call3[A1, A2, A3, R0](owner, arg1, arg2, arg3, args, argTypes)
      }
      val rs = ReducerSet(next[sFun, R], next[iFun, R], next[dFun, R], next[lFun, R], next[bFun, R], next[List[String], R])
      reduceArgs(owner, args, argTypes, rs)
    }
  }

  private def call3[A1, A2, A3, R](owner: ParseArith, arg1: A1, arg2: A2, arg3: A3, args: List[TypedExpression], argTypes: List[String]): R = {
    if(args.isEmpty && argTypes.isEmpty) {
      val f = myFunction.asInstanceOf[(ParseArith, A1, A2, A3) => R]
      f(owner, arg1, arg2, arg3)
    } else {
      def next[A4, R0](owner: ParseArith, arg4: A4, args: List[TypedExpression], argTypes: List[String]): R0 = {
        call4[A1, A2, A3, A4, R0](owner, arg1, arg2, arg3, arg4, args, argTypes)
      }
      val rs = ReducerSet(next[sFun, R], next[iFun, R], next[dFun, R], next[lFun, R], next[bFun, R], next[List[String], R])
      reduceArgs(owner, args, argTypes, rs)
    }
  }

  private def call4[A1, A2, A3, A4, R](owner: ParseArith, arg1: A1, arg2: A2, arg3: A3, arg4: A4, args: List[TypedExpression], argTypes: List[String]): R = {
    if(args.isEmpty && argTypes.isEmpty) {
      val f = myFunction.asInstanceOf[(ParseArith, A1, A2, A3, A4) => R]
      f(owner, arg1, arg2, arg3, arg4)
    } else {
      def next[A5, R0](owner: ParseArith, arg5: A5, args: List[TypedExpression], argTypes: List[String]): R0 = {
        call5[A1, A2, A3, A4, A5, R0](owner, arg1, arg2, arg3, arg4, arg5, args, argTypes)
      }
      val rs = ReducerSet(next[sFun, R], next[iFun, R], next[dFun, R], next[lFun, R], next[bFun, R], next[List[String], R])
      reduceArgs(owner, args, argTypes, rs)
    }
  }

  private def call5[A1, A2, A3, A4, A5, R](owner: ParseArith, arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, args: List[TypedExpression], argTypes: List[String]): R = {
    if(args.isEmpty && argTypes.isEmpty) {
      val f = myFunction.asInstanceOf[(ParseArith, A1, A2, A3, A4, A5) => R]
      f(owner, arg1, arg2, arg3, arg4, arg5)
    } else {
      def next[A6, R0](owner: ParseArith, arg6: A6, args: List[TypedExpression], argTypes: List[String]): R0 = {
        call6[A1, A2, A3, A4, A5, A6, R0](owner, arg1, arg2, arg3, arg4, arg5, arg6, args, argTypes)
      }
      val rs = ReducerSet(next[sFun, R], next[iFun, R], next[dFun, R], next[lFun, R], next[bFun, R], next[List[String], R])
      reduceArgs(owner, args, argTypes, rs)
    }
  }

  private def call6[A1, A2, A3, A4, A5, A6, R](owner: ParseArith, arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, arg6: A6, args: List[TypedExpression], argTypes: List[String]): R = {
    if(args.isEmpty && argTypes.isEmpty) {
      val f = myFunction.asInstanceOf[(ParseArith, A1, A2, A3, A4, A5, A6) => R]
      f(owner, arg1, arg2, arg3, arg4, arg5, arg6)
    } else {
      def next[A7, R0](owner: ParseArith, arg7: A7, args: List[TypedExpression], argTypes: List[String]): R0 = {
        call7[A1, A2, A3, A4, A5, A6, A7, R0](owner, arg1, arg2, arg3, arg4, arg5, arg6, arg7, args, argTypes)
      }
      val rs = ReducerSet(next[sFun, R], next[iFun, R], next[dFun, R], next[lFun, R], next[bFun, R], next[List[String], R])
      reduceArgs(owner, args, argTypes, rs)
    }
  }

  private def call7[A1, A2, A3, A4, A5, A6, A7, R](owner: ParseArith, arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, arg6: A6, arg7: A7, args: List[TypedExpression], argTypes: List[String]): R = {
    if(args.isEmpty && argTypes.isEmpty) {
      val f = myFunction.asInstanceOf[(ParseArith, A1, A2, A3, A4, A5, A6, A7) => R]
      f(owner, arg1, arg2, arg3, arg4, arg5, arg6, arg7)
    } else {
      def next[A8, R0](owner: ParseArith, arg8: A8, args: List[TypedExpression], argTypes: List[String]): R0 = {
        call8[A1, A2, A3, A4, A5, A6, A7, A8, R0](owner, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, args, argTypes)
      }
      val rs = ReducerSet(next[sFun, R], next[iFun, R], next[dFun, R], next[lFun, R], next[bFun, R], next[List[String], R])
      reduceArgs(owner, args, argTypes, rs)
    }
  }

  private def call8[A1, A2, A3, A4, A5, A6, A7, A8, R](owner: ParseArith, arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, arg6: A6, arg7: A7, arg8: A8, args: List[TypedExpression], argTypes: List[String]): R = {
    if(args.isEmpty && argTypes.isEmpty) {
      val f = myFunction.asInstanceOf[(ParseArith, A1, A2, A3, A4, A5, A6, A7, A8) => R]
      f(owner, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8)
    } else {
      def next[A9, R0](owner: ParseArith, arg9: A9, args: List[TypedExpression], argTypes: List[String]): R0 = {
        call9[A1, A2, A3, A4, A5, A6, A7, A8, A9, R0](owner, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, args, argTypes)
      }
      val rs = ReducerSet(next[sFun, R], next[iFun, R], next[dFun, R], next[lFun, R], next[bFun, R], next[List[String], R])
      reduceArgs(owner, args, argTypes, rs)
    }
  }

  private def call9[A1, A2, A3, A4, A5, A6, A7, A8, A9, R](owner: ParseArith, arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, arg6: A6, arg7: A7, arg8: A8, arg9: A9, args: List[TypedExpression], argTypes: List[String]): R = {
    if(args.isEmpty && argTypes.isEmpty) {
      val f = myFunction.asInstanceOf[(ParseArith, A1, A2, A3, A4, A5, A6, A7, A8, A9) => R]
      f(owner, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9)
    } else {
      def next[A10, R0](owner: ParseArith, arg10: A10, args: List[TypedExpression], argTypes: List[String]): R0 = {
        call10[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, R0](owner, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, args, argTypes)
      }
      val rs = ReducerSet(next[sFun, R], next[iFun, R], next[dFun, R], next[lFun, R], next[bFun, R], next[List[String], R])
      reduceArgs(owner, args, argTypes, rs)
    }
  }

  private def call10[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, R](owner: ParseArith, arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, arg6: A6, arg7: A7, arg8: A8, arg9: A9, arg10: A10, args: List[TypedExpression], argTypes: List[String]): R = {
    if(args.isEmpty && argTypes.isEmpty) {
      val f = myFunction.asInstanceOf[(ParseArith, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10) => R]
      f(owner, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10)
    } else {
      throw new GorParsingException("Functions with more than 10 arguments are not supported")
    }
  }
}
