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

object AlgebraicFunctions {
  def register(functions: FunctionRegistry): Unit = {
    functions.register("MAX", getSignatureDoubleDouble2Double(max), max _)
    functions.register("MAX", getSignatureLongLong2Long(maxLong), maxLong _)
    functions.register("MAX", getSignatureIntInt2Int(maxInt), maxInt _)
    functions.register("MAX", getSignatureStringString2String(maxString), maxString _)
    functions.register("MIN", getSignatureDoubleDouble2Double(min), min _)
    functions.register("MIN", getSignatureLongLong2Long(minLong), minLong _)
    functions.register("MIN", getSignatureIntInt2Int(minInt), minInt _)
    functions.register("MIN", getSignatureStringString2String(minString), minString _)
    functions.register("POW", getSignatureDoubleDouble2Double(pow), pow _)
    functions.register("ABS", getSignatureInt2Int(absInt), absInt _)
    functions.register("CEIL", getSignatureDouble2Int(ceil), ceil _)
    functions.register("SQRT", getSignatureDouble2Double(sqrt), sqrt _)
    functions.register("SQR", getSignatureDouble2Double(sqr), sqr _)
    functions.register("LOG", getSignatureDouble2Double(log), log _)
    functions.register("LN", getSignatureDouble2Double(ln), ln _)
    functions.register("EXP", getSignatureDouble2Double(exp), exp _)
    functions.register("ABS", getSignatureDouble2Double(abs), abs _)
    functions.register("FLOOR", getSignatureDouble2Int(floor), floor _)
    functions.register("MOD", getSignatureIntInt2Int(modInt), modInt _)
    functions.register("DIV", getSignatureIntInt2Int(divInt), divInt _)
    functions.register("RANDOM", getSignatureEmpty2Double(() => random()), random _)
    functions.register("SEGOVERLAP", getSignatureIntIntIntInt2Int(segOverlap), segOverlap _)
    functions.register("SEGDIST", getSignatureIntIntIntInt2Int(segDist), segDist _)
  }

  def segDist(ex1: iFun, ex2: iFun, ex3: iFun, ex4: iFun): iFun = {
    cvp => {
      0.max(ex1(cvp) - ex4(cvp) + 1).max(ex3(cvp) - ex2(cvp) + 1)
    }
  }

  def segOverlap(start1: iFun, stop1: iFun, start2: iFun, stop2: iFun): iFun = {
    cvp => {
      if (stop2(cvp) > start1(cvp) && start2(cvp) < stop1(cvp)) 1 else 0
    }
  }

  def random(): dFun = {
    _ => {
      Math.random()
    }
  }

  def minString(ex1: sFun, ex2: sFun): sFun = {
    cvp => {
      if (ex1(cvp) < ex2(cvp)) ex1(cvp) else ex2(cvp)
    }
  }

  def maxString(ex1: sFun, ex2: sFun): sFun = {
    cvp => {
      if (ex1(cvp) > ex2(cvp)) ex1(cvp) else ex2(cvp)
    }
  }

  def modInt(ex1: iFun, ex2: iFun): iFun = {
    cvp => {
      ex1(cvp) % ex2(cvp)
    }
  }

  def divInt(ex1: iFun, ex2: iFun): iFun = {
    cvp => {
      ex1(cvp) / ex2(cvp)
    }
  }

  def minInt(ex1: iFun, ex2: iFun): iFun = {
    cvp => {
      ex1(cvp).min(ex2(cvp))
    }
  }

  def minLong(ex1: lFun, ex2: lFun): lFun = {
    cvp => {
      ex1(cvp).min(ex2(cvp))
    }
  }

  def maxInt(ex1: iFun, ex2: iFun): iFun = {
    cvp => {
      ex1(cvp).max(ex2(cvp))
    }
  }

  def maxLong(ex1: lFun, ex2: lFun): lFun = {
    cvp => {
      ex1(cvp).max(ex2(cvp))
    }
  }

  def sqrt(ex: dFun): dFun = {
    cvp => {
      scala.math.sqrt(ex(cvp))
    }
  }

  def sqr(ex: dFun): dFun = {
    cvp => {
      ex(cvp) * ex(cvp)
    }
  }

  def log(ex: dFun): dFun = {
    cvp => {
      scala.math.log(ex(cvp)) / scala.math.log(10)
    }
  }

  def ln(ex: dFun): dFun = {
    cvp => {
      scala.math.log(ex(cvp))
    }
  }

  def exp(ex: dFun): dFun = {
    cvp => {
      scala.math.exp(ex(cvp))
    }
  }

  def abs(ex: dFun): dFun = {
    cvp => {
      if (ex(cvp) < 0.0) -ex(cvp) else ex(cvp)
    }
  }

  def floor(ex: dFun): iFun = {
    cvp => {
      scala.math.floor(ex(cvp)).toInt
    }
  }

  def ceil(ex: dFun): iFun = {
    cvp => {
      scala.math.ceil(ex(cvp)).toInt
    }
  }

  def absInt(ex: iFun): iFun = {
    cvp => {
      if (ex(cvp) < 0) -ex(cvp) else ex(cvp)
    }
  }

  def pow(ex1: dFun, ex2: dFun): dFun = {
    cvp => {
      scala.math.pow(ex1(cvp), ex2(cvp))
    }
  }

  def min(ex1: dFun, ex2: dFun): dFun = {
    cvp => {
      ex1(cvp).min(ex2(cvp))
    }
  }

  def max(ex1: dFun, ex2: dFun): dFun = {
    cvp => {
      ex1(cvp).max(ex2(cvp))
    }
  }
}
