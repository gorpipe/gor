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

package gorsat.Commands

import org.gorpipe.gor.model.Row

class RangeStat {
  var minChr: String = null
  var minPos: Int = -1
  var maxChr: String = null
  var maxPos: Int = -1

  def updateRange(ir: Row): Unit = {
    if(minChr==null) {
      minChr = ir.chr
      minPos = ir.pos
    }
    maxChr = ir.chr
    maxPos = ir.pos
  }

  override def toString: String = {
    minChr + "\t" + minPos + "\t" + maxChr + "\t" + maxPos + "\n"
  }

  def generateDictEntry(respath: String): String = {
    respath + "\t1\t" + toString
  }
}

abstract class Output extends Processor {
  var pipeFrom : Processor = _
  var name : String = _
  def getName: String = null
  def getMd5: String = null
  def getRange: RangeStat = null
  def reportWantsNoMore() {
    if (pipeFrom!=null && !wantsNoMore) pipeFrom.reportWantsNoMore()
    wantsNoMore = true
  }
  def from (from : Processor) {
    pipeFrom = from
  }
  final def securedSetup(oe : Throwable) {
    try {
      setup()
    } catch {
      case e : Throwable => // We are in the end so we only throw the appropriate exception
        if (oe != null) throw oe
        else throw e
    }
  }
  final def securedFinish(oe : Throwable) {
    try {
      finish()
    } catch {
      case e : Throwable => // We are in the end so we only throw the appropriate exception
        if (oe != null) throw oe
        else throw e
    }
  }
}
