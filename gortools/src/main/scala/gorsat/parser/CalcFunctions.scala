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

import gorsat.Utilities.StringUtilities
import gorsat.parser.FunctionSignature._
import gorsat.parser.FunctionTypes._
import gorsat.parser.ParseUtilities.{eval, system}
import gorsat.process.GorJavaUtilities.CmdParams
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.model.ColumnValueProvider

object CalcFunctions {
  /**
    * A global function registry that is used by the ParseArith
    * class. This provides the default set of functions used
    * in the CALC command and WHERE filters.
    */
  lazy val registry: FunctionRegistry = FunctionRegistry()
  lazy val pathutilregistry: FunctionRegistry = FunctionRegistry()
  register(registry, pathutilregistry)

  def register(functions: FunctionRegistry, pathfunctions: FunctionRegistry): Unit = {
    functions.register("ONCE", getSignatureInt2Int(onceInt), onceInt _)
    functions.register("ONCE", getSignatureDouble2Double(onceDouble), onceDouble _)
    functions.register("SLEEP", getSignatureInt2String(sleep), sleep _)
    functions.register("MD5", getSignatureString2String(md5), md5 _)
    functions.register("DECODE", getSignatureStringString2String(decode), decode _)
    functions.register("CATCH", getSignatureStringString2String(calccatchString), calccatchString _)
    functions.register("CATCH", getSignatureIntString2String(calccatchInt), calccatchInt _)
    functions.register("CATCH", getSignatureDoubleString2String(calccatchDouble), calccatchDouble _)
    functions.registerWithOwner("TIMESIGNATURE", getSignatureString2String(removeOwner(timeSignature)), timeSignature _)
    functions.registerWithOwner("SYSTEM", getSignatureString2String(removeOwner(systemFunction)), systemFunction _)
    functions.registerWithOwner("EVAL", getSignatureString2String(removeOwner(evalFunc)), evalFunc _)

    AlgebraicFunctions.register(functions)
    DiagnosticsFunctions.register(functions, pathfunctions)
    GenomeFunctions.register(functions)
    ListFunctions.register(functions)
    StatisticalFunctions.register(functions)
    StringFunctions.register(functions)
    TimeAndDateFunctions.register(functions)
    TrigonometricFunctions.register(functions)
    TypeConversionFunctions.register(functions)
  }

  def evalFunc(owner: ParseArith, ex: sFun): sFun = {
    cvp => {
      eval(ex(cvp), owner.context)
    }
  }

  def systemFunction(owner: ParseArith, ex: sFun): sFun = {
    cvp => {
      val whitelistMap = if (owner.context.getSession.getSystemContext.getCommandWhitelist != null) owner.context.getSession.getSystemContext.getCommandWhitelist.asInstanceOf[java.util.Map[String, CmdParams]] else null
      val cmd = ex(cvp)
      val spl = cmd.indexOf(' ')
      val i = if (spl == -1) cmd.length else spl
      val cmdname = cmd.substring(0, i).trim
      if (whitelistMap != null && whitelistMap.containsKey(cmdname) && whitelistMap.get(cmdname).isFormula) {
        val cmdparams = whitelistMap.get(cmdname)
        val rest = cmd.substring(i)
        system(cmdparams.getCommand + rest)
      } else {
        if (owner.context.getSession.getSystemContext.getServer) "Command " + cmdname + " not allowed to run"
        else system(cmd)
      }
    }
  }
  def timeSignature(owner: ParseArith, ex: sFun): sFun = {
    cvp => {
      owner.context.getSession.getProjectContext.getFileReader.getFileSignature(ex(cvp))
    }
  }

  def calccatchString(ex1: sFun, ex2: sFun): sFun = {
    cvp =>
      calccatchInner(ex1, ex2, cvp)
  }

  def calccatchInt(ex1: iFun, ex2: sFun): sFun = {
    cvp =>
      calccatchInner(ex1, ex2, cvp)
  }

  def calccatchDouble(ex1: dFun, ex2: sFun): sFun = {
    cvp =>
      calccatchInner(ex1, ex2, cvp)
  }

  private def calccatchInner(ex1: aFun, ex2: sFun, cvp: ColumnValueProvider): String = {
    try {
      ex1(cvp).toString
    } catch {
      case e: GorParsingException =>
        val emessage = e.getMessage
        if (emessage != null) ex2(cvp).replace("#{e}", emessage)
        else ex2(cvp)
    }
  }

  def decode(ex1: sFun, ex2: sFun): sFun = {
    var mapDefined = false
    var elseValue:String = null
    val myMap = new scala.collection.mutable.HashMap[String,String]

    cvp => {
      if (!mapDefined) {
        mapDefined = true
        val s2 = ex2(cvp)
        val slist = s2.split(",",-1).map(_.trim)
        if (slist.length < 2) throw new GorParsingException("The map parameter needs at least two values. Example: ...|calc x DECODE(col,'a,1')")
        val n = slist.length/2
        var i = 0
        while (i < n) {
          myMap.update(slist(i*2),slist(i*2+1))
          i = i + 1
        }
        if (slist.length > 2*n) elseValue = slist.last
      }
      val s1 = ex1(cvp)
      myMap.get(s1) match {
        case Some(x) => x
        case None => if (elseValue != null) elseValue else s1
      }
    }
  }

  def md5(ex: sFun): sFun = {
    cvp => {
      StringUtilities.createMD5(ex(cvp))
    }
  }

  def sleep(ex1: iFun): sFun = {
    cvp => {
      Thread.sleep(ex1(cvp))
      ""
    }
  }

  def onceDouble(ex: dFun): dFun = {
    var onceVal: Double = 0.0
    var valNotSet = true
    cvp => {
      if (valNotSet) { onceVal = ex(cvp); valNotSet = false }
      onceVal
    }
  }

  def onceInt(ex: iFun): iFun = {
    var onceVal: Int = 0
    var valNotSet = true
    cvp => {
      if (valNotSet) { onceVal = ex(cvp); valNotSet = false }
      onceVal
    }
  }
}
