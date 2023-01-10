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
import gorsat.parser.FunctionTypes.{bFun, iFun, sFun}
import gorsat.parser.ParseUtilities.string2wordString

object StringFunctions {
  def register(functions: FunctionRegistry): Unit = {
    functions.register("ONCE", getSignatureString2String(onceString), onceString _)
    functions.register("UPPER", getSignatureString2String(upper), upper _)
    functions.register("LOWER", getSignatureString2String(lower), lower _)
    functions.register("TRIM", getSignatureString2String(trim), trim _)
    functions.register("BRACKETS", getSignatureString2String(brackets), brackets _)
    functions.register("UNBRACKET", getSignatureString2String(unbracket), unbracket _)
    functions.register("SQUOTE", getSignatureString2String(squote), squote _)
    functions.register("SUNQUOTE", getSignatureString2String(sunquote), sunquote _)
    functions.register("DQUOTE", getSignatureString2String(dquote), dquote _)
    functions.register("DUNQUOTE", getSignatureString2String(dunquote), dunquote _)
    functions.register("REVERSE", getSignatureString2String(reverse), reverse _)
    functions.register("STR", getSignatureString2String(str), str _)
    functions.register("POSOF", getSignatureStringString2Int(posOf), posOf _)
    functions.register("CONTAINSCOUNT", getSignatureStringString2Int(containsCount), containsCount _)
    functions.register("CSCONTAINSCOUNT", getSignatureStringString2Int(csContainsCount), csContainsCount _)
    functions.register("LISTHASCOUNT", getSignatureStringString2Int(listHasCount), listHasCount _)
    functions.register("CSLISTHASCOUNT", getSignatureStringString2Int(csListHasCount), csListHasCount _)
    functions.register("REPLACE", getSignatureStringStringString2String(replace), replace _)
    functions.register("LEN", getSignatureString2Int(len), len _)
    functions.register("LEFT", getSignatureStringInt2String(left), left _)
    functions.register("RIGHT", getSignatureStringInt2String(right), right _)
    functions.register("STR2LIST", getSignatureStringInt2String(str2List), str2List _)
    functions.register("STR2LIST", getSignatureStringIntString2String(str2ListWithSeparator), str2ListWithSeparator _)
    functions.register("SUBSTR", getSignatureStringIntInt2String(substr), substr _)
    functions.register("MID", getSignatureStringIntInt2String(mid), mid _)
    functions.register("CONTAINS", getSignatureStringString2Boolean(contains), contains _)
    functions.register("CSCONTAINS", getSignatureStringString2Boolean(cscontains), cscontains _)
    functions.register("CSCONTAINSALL", getSignatureStringString2Boolean(cscontains), cscontains _)
    functions.register("CHAR", getSignatureStringInt2Int(char), char _)
    functions.register("CONTAINSANY", getSignatureStringString2Boolean(containsAny), containsAny _)
    functions.register("CSCONTAINSANY", getSignatureStringString2Boolean(csContainsAny), csContainsAny _)
    functions.register("REGSEL", getSignatureStringString2String(regsel), regsel _)
  }

  def regsel(ex1: sFun, ex2: sFun): sFun = {
    cvp => {
      val r = ex2(cvp).r
      var x = ""
      try {
        val r(m) = ex1(cvp); x = m
      } catch {
        case _: Exception => /* */
      }
      x
    }
  }

  def csContainsAny(ex1: sFun, ex2: sFun): bFun = {
    cvp => {
      ex2(cvp).split(",", -1).exists(ex1(cvp).contains(_))
    }
  }

  def containsAny(ex1: sFun, ex2: sFun): bFun = {
    cvp => {
      ex2(cvp).split(",", -1).map(x => x.toUpperCase).exists(ex1(cvp).toUpperCase.contains(_))
    }
  }

  def char(ex1: sFun, ex2: iFun): iFun = {
    cvp => {
      ex1(cvp).charAt(ex2(cvp)).toInt
    }
  }

  def cscontains(ex1: sFun, ex2: sFun): bFun = {
    cvp =>
      ex1(cvp).contains(ex2(cvp))
  }

  def contains(ex1: sFun, ex2: sFun): bFun = {
    cvp => {
      ex1(cvp).toUpperCase.contains(ex2(cvp).toUpperCase)
    }
  }

  def mid(ex1: sFun, ex2: iFun, ex3: iFun): sFun = {
    cvp => {
      val s1 = ex1(cvp)
      val s2 = ex2(cvp)
      s1.substring(s2, (s2 + ex3(cvp)).min(s1.length))
    }
  }

  def substr(ex1: sFun, ex2: iFun, ex3: iFun): sFun = {
    cvp => {
      val s1 = ex1(cvp)
      s1.substring(ex2(cvp), ex3(cvp).min(s1.length))
    }
  }

  def str2List(ex1: sFun, ex2: iFun): sFun = {
    cvp => {
      val wordSize = ex2(cvp).max(1)
      string2wordString(ex1(cvp), wordSize, ",")
    }
  }

  def str2ListWithSeparator(ex1: sFun, ex2: iFun, ex3: sFun): sFun = {
    cvp => {
      val wordSize = ex2(cvp).max(1)
      string2wordString(ex1(cvp), wordSize, ex3(cvp))
    }
  }

  def left(ex1: sFun, ex2: iFun): sFun = {
    cvp => {
      val s1 = ex1(cvp)
      s1.substring(0, ex2(cvp).min(s1.length))
    }
  }

  def right(ex1: sFun, ex2: iFun): sFun = {
    cvp => {
      val s1 = ex1(cvp)
      s1.substring(0.max(s1.length - ex2(cvp)))
    }
  }

  def len(ex: sFun): iFun = {
    cvp => {
      ex(cvp).length
    }
  }

  def replace(ex1: sFun, ex2: sFun, ex3: sFun): sFun = {
    cvp => {
      ex1(cvp).replace(ex2(cvp), ex3(cvp))
    }
  }

  def csListHasCount(ex1: sFun, ex2: sFun): iFun = {
    cvp => {
      val tmp = ex1(cvp).split(",", -1)
      ex2(cvp).split(",", -1).count(x => tmp.contains(x))
    }
  }

  def listHasCount(ex1: sFun, ex2: sFun): iFun = {
    cvp => {
      val tmp = ex1(cvp).split(",", -1).map(b => b.toUpperCase)
      ex2(cvp).split(",", -1).map(x => x.toUpperCase).count(x => tmp.contains(x))
    }
  }

  def csContainsCount(ex1: sFun, ex2: sFun): iFun = {
    cvp => {
      ex2(cvp).split(",", -1).count(ex1(cvp).contains(_))
    }
  }

  def containsCount(ex1: sFun, ex2: sFun): iFun = {
    cvp => {
      ex2(cvp).split(",", -1).map(x => x.toUpperCase).count(ex1(cvp).toUpperCase.contains(_))
    }
  }

  def posOf(ex1: sFun, ex2: sFun): iFun = {
    cvp => {
      ex1(cvp).indexOf(ex2(cvp))
    }
  }

  def str(ex: sFun): sFun = {
    cvp => {
      ex(cvp)
    }
  }
  def reverse(ex: sFun): sFun = {
    cvp => {
      ex(cvp).reverse
    }
  }

  def onceString(ex: sFun): sFun = {
    var onceVal: String = null
    cvp => {
      if (onceVal == null) onceVal = ex(cvp)
      onceVal
    }
  }

  def upper(ex: sFun): sFun = {
    cvp => {
      ex(cvp).toUpperCase
    }
  }

  def lower(ex: sFun): sFun = {
    cvp => {
      ex(cvp).toLowerCase
    }
  }

  def trim(ex: sFun): sFun = {
    cvp => {
      ex(cvp).trim
    }
  }

  def brackets(ex: sFun): sFun = {
    cvp => {
      s"(${ex(cvp)})"
    }
  }

  def unbracket(ex: sFun): sFun = {
    cvp => {
      val x = ex(cvp)
      var sta = 0
      var sto = x.length
      if (x.length > 1) {
        if (x.charAt(0) == '(') sta += 1
        if (x.charAt(x.length - 1) == ')') sto -= 1
      }
      x.slice(sta, sto)
    }
  }

  def squote(ex: sFun): sFun = {
    cvp => {
      s"'${ex(cvp).replace("'", "\\'")}'"
    }
  }

  def sunquote(ex: sFun): sFun = {
    cvp => {
      val s = ex(cvp)
      if (s.length > 1 && s.startsWith("'") && s.endsWith("'") && s(s.length-2) != '\\') {
        s.substring(1, s.length-1).replace("\\'", "'")
      } else {
        s
      }
    }
  }

  def dquote(ex: sFun): sFun = {
    cvp => {
      s"\"${ex(cvp).replace("\"", "\\\"")}\""
    }
  }

  def dunquote(ex: sFun): sFun = {
    cvp => {
      val s = ex(cvp)
      if (s.length > 1 && s.startsWith("\"") && s.endsWith("\"") && s(s.length-2) != '\\') {
        s.substring(1, s.length-1).replace("\\\"", "\"")
      } else {
        s
      }
    }
  }
}
