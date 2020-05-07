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

package gorsat.Iterators

import org.gorpipe.exceptions.GorSystemException
import org.gorpipe.model.genome.files.gor.Row
import org.gorpipe.model.gor.RowObj
import org.gorpipe.model.gor.iterators.RowSource

class SingleIteratorSource(protected val theIterator: IteratorSource, protected val iterName: String) extends RowSource {
  protected var myNext: Row = _
  protected var myHasNext: Boolean = false
  protected var posSet: Boolean = false
  protected var mustReCheck: Boolean = true

  override def hasNext: Boolean = {
    if (!mustReCheck) return myHasNext
    mustReCheck = false
    if (theIterator.hasNext) {
      val x = theIterator.next()
      myNext = RowObj(x)
      myHasNext = true
      return true
    }
    myHasNext = false
    false
  }

  override def next(): Row = {
    if (hasNext) {
      mustReCheck = true
      myNext
    } else {
      throw new GorSystemException("hasNext: getRow call on false hasNext!", null)
    }
  }

  override def setPosition(seekChr: String, seekPos: Int) {
    //	val e = new Exception; e.printStackTrace
    posSet = true
    mustReCheck = true
    theIterator.setPosition(seekChr, seekPos)
  }

  override def moveToPosition(seekChr: String, seekPos: Int, maxReads: Int = 10000) {
    var reads = 0
    var reachedPos = false
    var theNext: Row = null
    if (myNext != null && myNext.pos == seekPos && myNext.chr == seekChr) return
    while (reads < maxReads && !reachedPos && hasNext) {
      if (hasNext) theNext = next()
      if ((seekPos <= theNext.pos && seekChr == theNext.chr) || seekChr < theNext.chr) reachedPos = true else reads += 1
    }
    if (reachedPos) {
      myHasNext = true
      mustReCheck = false
      myNext = theNext
    }
    else if (hasNext) setPosition(seekChr, seekPos)
  }

  def close: Unit = theIterator.close()

  override def getHeader: String = {
    theIterator.getHeader
  }
}
