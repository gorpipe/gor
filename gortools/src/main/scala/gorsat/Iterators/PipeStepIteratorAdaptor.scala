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

import java.util

import gorsat.Commands.Analysis
import org.gorpipe.exceptions.GorSystemException
import org.gorpipe.model.genome.files.gor.Row
import org.gorpipe.model.gor.iterators.RowSource

class PipeStepIteratorAdaptor(var sourceIterator: RowSource, val pipeStep: Analysis, val theHeader: String) extends RowSource {
  var rowQueue = new util.LinkedList[Row]
  var mustReCheck = true
  var myHasNext = false
  var myNext: Row = _

  case class BufferAdaptor() extends Analysis {
    override def process(r: Row) {
      if (!wantsNoMore) {
        rowQueue.add(r)
      } else {
        bufferedPipeStep.wantsNoMore = true
      }
    }
  }

  var bufferedPipeStep: Analysis = if (pipeStep != null) pipeStep | BufferAdaptor() else BufferAdaptor()
  bufferedPipeStep.securedSetup(null)

  override def hasNext: Boolean = {
    if (!mustReCheck) return myHasNext
    if (rowQueue.size() > 0) myHasNext = true
    else {
      while (sourceIterator.hasNext && rowQueue.size() == 0 && !bufferedPipeStep.wantsNoMore) {
        bufferedPipeStep.process(sourceIterator.next())
      }
      if (rowQueue.size() == 0) bufferedPipeStep.securedFinish(null)
      myHasNext = rowQueue.size() > 0
    }
    mustReCheck = false
    myHasNext
  }

  override def next(): Row = {
    if (hasNext) {
      mustReCheck = true
      myNext = rowQueue.poll()
      myNext
    } else {
      throw new GorSystemException("In PipeStepIteratorAdaptor hasNext: getRow call on false hasNext!", null)
    }
  }

  override def setPosition(seekChr: String, seekPos: Int): Unit = {
    mustReCheck = true
    rowQueue.clear()
    sourceIterator.setPosition(seekChr, seekPos)
  }

  def close: Unit = {
    mustReCheck = true
    rowQueue.clear()
    sourceIterator.close
  }

  override def getHeader: String = {
    theHeader
  }
}
