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

package org.gorpipe.model.gor.iterators

import org.gorpipe.gor.model.Row

abstract class TimedRowSource extends RowSource {
  var myHasNext: Boolean = false
  var myNext: Row = _
  var mustReCheck: Boolean = true

  override def moveToPosition(seekChr : String, seekPos : Int, maxReads : Int) {
    var reads = 0
    var reachedPos = false
    var theNext : Row = null
    if (myNext != null && myNext.pos == seekPos && myNext.chr == seekChr) return

    if (myNext != null && seekChr == myNext.chr) {
      val avgSeekTimeMillis = getAvgSeekTimeMilliSecond
      var etaMillis = if( getAvgBasesPerMilliSecond > 0 ) (seekPos - myNext.pos) / getAvgBasesPerMilliSecond else 0.0
      var estRowNum = etaMillis * getAvgRowsPerMilliSecond
      while (!reachedPos && hasNext && (etaMillis < avgSeekTimeMillis || estRowNum < getAvgBatchSize)) {
        theNext = next()
        if ((seekPos <= theNext.pos && seekChr == theNext.chr) || seekChr < theNext.chr) reachedPos = true else reads += 1

        etaMillis = if( getAvgBasesPerMilliSecond > 0 ) (seekPos - theNext.pos) / getAvgBasesPerMilliSecond else 0.0
        estRowNum = etaMillis * getAvgRowsPerMilliSecond
      }
    }
    if (reachedPos) {
      myHasNext = true; mustReCheck = false; myNext = theNext
    } else if (hasNext) {
      setPosition(seekChr,seekPos)
    }
  }
}
