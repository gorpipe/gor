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

package gorsat.Iterators

import org.gorpipe.exceptions.GorSystemException

import scala.io.StdIn

class StdInputSource() extends IteratorSource {
  var myHasNext: Boolean = false
  var myNext: String = _
  var mustReCheck: Boolean = true
  var haveReadHeader: Boolean = false
  var myHeader: String = _

  def hasNext: Boolean = {
    if (!haveReadHeader) {
      val temp = getHeader
    }
    if (!mustReCheck) return myHasNext
    myNext = StdIn.readLine()
    if (myNext != null) myHasNext = true else myHasNext = false
    mustReCheck = false
    myHasNext
  }

  def next(): String = {
    if (hasNext) {
      mustReCheck = true
      myNext
    } else {
      throw new GorSystemException("hasNext: getRow call on false hasNext!", null)
    }
  }

  def setPosition(seekChr: String, seekPos: Int) {
    /* do nothing */
  }

  def close(): Unit = {}

  def getHeader: String = {
    if (haveReadHeader) return myHeader
    myHeader = StdIn.readLine()
    if (myHeader == null) {
      myHasNext = false
      return ""
    }
    val cols = myHeader.split("\t", -1)
    mustReCheck = true
    haveReadHeader = true
    myHeader
  }
}
