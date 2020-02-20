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

import java.text.SimpleDateFormat
import java.util.Date

import gorsat.parser.FunctionTypes.{lFun, sFun}
import gorsat.parser.FunctionSignature._

object TimeAndDateFunctions {
  def register(functions: FunctionRegistry): Unit = {
    functions.register("EPOCH", getSignatureEmpty2Long(() => epoch()), epoch _)
    functions.register("EPOCH", getSignatureStringString2Long(epochWithDateAndFormat), epochWithDateAndFormat _)
    functions.register("EDATE", getSignatureLong2String(edate), edate _)
    functions.register("EDATE", getSignatureLongString2String(edateWithFormat), edateWithFormat _)
    functions.register("DATE", getSignatureString2String(date), date _)
  }

  def date(ex: sFun): sFun = {
    cvp => {
      new SimpleDateFormat(ex(cvp)).format(new Date())
    }
  }

  def epoch(): lFun = _ => new Date().getTime

  def epochWithDateAndFormat(ex1: sFun, ex2: sFun): lFun = {
    cvp => new SimpleDateFormat(ex2(cvp)).parse(ex1(cvp)).getTime
  }

  def edate(ex1: lFun): sFun = cvp => {
    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(ex1(cvp)))
  }

  def edateWithFormat(ex1: lFun, ex2: sFun): sFun = cvp => {
    new SimpleDateFormat(ex2(cvp)).format(new Date(ex1(cvp)))
  }
}
