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
import gorsat.parser.FunctionTypes.dFun

object TrigonometricFunctions {
  def register(functions: FunctionRegistry): Unit = {
    functions.register("SIN", getSignatureDouble2Double(sin), sin _)
    functions.register("COS", getSignatureDouble2Double(cos), cos _)
    functions.register("ASIN", getSignatureDouble2Double(asin), asin _)
    functions.register("ACOS", getSignatureDouble2Double(acos), acos _)
    functions.register("TAN", getSignatureDouble2Double(tan), tan _)
    functions.register("ATAN", getSignatureDouble2Double(atan), atan _)
  }

  def sin(ex: dFun): dFun = {
    cvp => {
      scala.math.sin(ex(cvp))
    }
  }

  def cos(ex: dFun): dFun = {
    cvp => {
      scala.math.cos(ex(cvp))
    }
  }

  def asin(ex: dFun): dFun = {
    cvp => {
      scala.math.asin(ex(cvp))
    }
  }

  def acos(ex: dFun): dFun = {
    cvp => {
      scala.math.acos(ex(cvp))
    }
  }

  def tan(ex: dFun): dFun = {
    cvp => {
      scala.math.tan(ex(cvp))
    }
  }

  def atan(ex: dFun): dFun = {
    cvp => {
      scala.math.atan(ex(cvp))
    }
  }
}
