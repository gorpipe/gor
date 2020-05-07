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

package org.gorpipe.model.gor.iterators

import org.gorpipe.exceptions.GorSystemException
import org.gorpipe.gor.driver.providers.stream.datatypes.gor.GorHeader
import org.gorpipe.model.genome.files.gor.Row

class SingleRowIteratorSource(theIterator: RowSource) extends RowSource{
  protected var myNext : Row = _
  protected var myHasNext : Boolean = false
  protected var posSet : Boolean = false
  protected var mustReCheck : Boolean = true

  override def getAvgBasesPerMilliSecond: Double = theIterator.getAvgBasesPerMilliSecond
  override def getAvgRowsPerMilliSecond: Double = theIterator.getAvgRowsPerMilliSecond
  override def getAvgBatchSize: Double = theIterator.getAvgBatchSize
  override def getAvgSeekTimeMilliSecond: Double = theIterator.getAvgSeekTimeMilliSecond

  override def getCurrentBatchLoc: Int = theIterator.getCurrentBatchLoc
  override def getCurrentBatchSize: Int = theIterator.getCurrentBatchSize
  override def getCurrentBatchRow(i: Int): Row = theIterator.getCurrentBatchRow(i)

  override def toString: String = {
    theIterator.toString
  }

  override def hasNext : Boolean = {
    if (!mustReCheck) return myHasNext
    mustReCheck = false
    myHasNext = theIterator.hasNext
    if (myHasNext) myNext = theIterator.next()
    myHasNext
  }
  override def next() : Row = {
    if (hasNext) {
      mustReCheck = true
      myNext
    } else {
      throw new GorSystemException("Error: singleRowIteratorSource.hasNext: getRow call on false hasNext!", null)
    }
  }

  override def setPosition(seekChr : String, seekPos : Int) {
    posSet = true
    mustReCheck = true
    theIterator.setPosition(seekChr,seekPos)
  }

  override def moveToPosition(seekChr : String, seekPos : Int, maxReads: Int = 10000) {
    theIterator.moveToPosition(seekChr,seekPos, maxReads)
  }

  def close(): Unit = {
    theIterator.close()
  }
  override def getHeader: String = theIterator.getHeader

  override def getGorHeader: GorHeader = theIterator.getGorHeader

  override def pushdownFilter(gorwhere: String): Boolean = theIterator.pushdownFilter(gorwhere)

  override def pushdownTop(limit: Int): Boolean = theIterator.pushdownTop(limit)
}
