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

import org.gorpipe.gor.driver.providers.stream.datatypes.gor.GorHeader
import org.gorpipe.model.genome.files.gor.{GenomicIterator, Line, Row}
import org.gorpipe.model.gor.Pipes

abstract class RowSource extends GenomicIterator with AutoCloseable {
  var bufferSize: Int = Pipes.rowsToProcessBuffer
  private var ex : Throwable = _
  var parent : RowSource = _

  def getParent : RowSource = parent
  def setParent(rs : RowSource): Unit = { parent = rs }
  def getAvgSeekTimeMilliSecond = 0.0
  def getAvgRowsPerMilliSecond = 0.0
  def getAvgBasesPerMilliSecond = 0.0
  def getAvgBatchSize = 0.0
  def getCurrentBatchSize = 0
  def getCurrentBatchLoc = 0
  def getCurrentBatchRow( i : Int ) : Row = null
  def setPosition(seekChr: String, seekPos : Int) { seek(seekChr, seekPos) }
  def moveToPosition(seekChr: String, seekPos : Int, maxReads: Int = 10000): Unit = setPosition(seekChr, seekPos)
  def close()
  def terminateReading() { /* do nothing */ }
  def getBufferSize : Int = bufferSize
  def setBufferSize( bs : Int ) { bufferSize = bs }
  def isBuffered = false

  def getGorHeader: GorHeader = null

  def setEx(throwable: Throwable) : Unit = {
    if (ex == null || throwable == null) {
      ex = throwable
    }
  }

  def getEx: Throwable = {
    ex
  }

  override def clone(): RowSource = {
    val rs = new RowSource {
      /**
       * Close the data source, releasing all resources (typically files).
       */
      override def close(): Unit = ???
    }
    rs.bufferSize = bufferSize
    rs.ex = ex
    rs.parent = parent
    rs
  }

  override def next(line: Line): Boolean = {
    if( hasNext ) {
      val row = next()
      line.copyColumnsFrom(row)
      return true
    }
    false
  }

  override def seek(seekChr: String, seekPos: Int) = false
}
