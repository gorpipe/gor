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

// gorsatGorIterator.scala
// (c) deCODE genetics
// 17th May, 2011, Hakon Gudbjartsson

package gorsat.gorsatGorIterator

import gorsat.Commands.{Analysis, CommandParseUtilities}
import gorsat.DynIterator.DynamicRowSource
import org.gorpipe.gor.{GorContext, GorSession}
import org.gorpipe.model.genome.files.gor.{GorIterator, GorMonitor}
import org.gorpipe.model.gor.iterators.RowSource

abstract class gorsatGorIterator(context: GorContext) extends GorIterator {
  val session = context.getSession
  var theIterator : RowSource = _
  var thePipeStep : Analysis = _
  var theInputSource: RowSource = _
  var combinedHeader : String = _
  var theParams = ""
  var usedFiles : List[String] = Nil
  var fixHeader : Boolean = true
  var isNorContext = false


  def processArguments(args : Array[String], executeNor : Boolean, forcedInputHeader : String = ""): RowSource

  def scalaPipeStepInit(iparams : String, forcedInputHeader : String = "") {
    if (theIterator != null) close()
    val args = CommandParseUtilities.quoteSafeSplit(iparams + " -stdin",' ')
    processArguments(args, isNorContext,forcedInputHeader)
    theParams = iparams
    theIterator = null
  }

  def scalaInit(iparams : String, forcedInputHeader : String = "") {
    if (theIterator != null) {
      close()
    }

    val args = Array(iparams)
    processArguments(args, isNorContext,forcedInputHeader)
    theParams = iparams
  }

  override def init(params : String, gm : GorMonitor): Unit = { context.getSession.getSystemContext.setMonitor(gm); scalaInit(params) }

  def seek(chr : String, pos : Int) {  // We must re-initialize if seek is applied
    if (theIterator != null) close()
    val dynIterator = new DynamicRowSource(theParams, context)
    dynIterator.setPositionWithoutChrLimits(chr,pos)
    theIterator = dynIterator
  }

  def getSession : GorSession = {
    context.getSession
  }

  def getHeader : String = combinedHeader
  def hasNext : Boolean = theIterator.hasNext
  def next : String = theIterator.next().toString
  def close() {
    if (theIterator != null) {
      theIterator.close()
    }
    if (context != null && context.getSession != null) context.getSession.close()
  }

  def getPipeStep : Analysis = thePipeStep
}

// ends gorsatGorIterator



