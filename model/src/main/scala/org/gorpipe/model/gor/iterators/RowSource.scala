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

import org.gorpipe.gor.GorContext
import org.gorpipe.gor.driver.providers.stream.datatypes.gor.GorHeader
import org.gorpipe.gor.stats.StatsCollector
import org.gorpipe.model.genome.files.gor.Row
import org.gorpipe.model.gor.Pipes

abstract class RowSource extends java.util.Iterator[Row] with AutoCloseable {
  var header = ""
  var bufferSize: Int = Pipes.rowsToProcessBuffer
  private var ex : Throwable = _
  var parent : RowSource = _
  private var stats: StatsCollector = _
  private var statsSenderId: Int = -1

  def getParent : RowSource = parent
  def setParent(rs : RowSource): Unit = { parent = rs }
  def getAvgSeekTimeMilliSecond = 0.0
  def getAvgRowsPerMilliSecond = 0.0
  def getAvgBasesPerMilliSecond = 0.0
  def getAvgBatchSize = 0.0
  def getCurrentBatchSize = 0
  def getCurrentBatchLoc = 0
  def getCurrentBatchRow( i : Int ) : Row = null
  override def hasNext : Boolean
  override def next() : Row
  def setPosition(seekChr: String, seekPos : Int)
  def moveToPosition(seekChr: String, seekPos : Int, maxReads: Int = 10000): Unit = setPosition(seekChr, seekPos)
  def close()
  def terminateReading() { /* do nothing */ }
  def getHeader : String = header
  def setHeader(x : String) { header = x }
  def getBufferSize : Int = bufferSize
  def setBufferSize( bs : Int ) { bufferSize = bs }
  def isBuffered = false
  def pushdownFilter(gorwhere: String) : Boolean = false
  def pushdownCalc(formula: String, colName: String) : Boolean = false
  def pushdownSelect() : Boolean = false
  def pushdownWrite(filename: String) : Boolean = false
  def pushdownGor(cmd: String) : Boolean = false
  def pushdownTop(limit: Int) : Boolean = false

  def getGorHeader: GorHeader = null

  def setEx(throwable: Throwable) : Unit = {
    if (ex == null || throwable == null) {
      ex = throwable
    }
  }

  def getEx: Throwable = {
    ex
  }

  def initStats(context: GorContext, sender: String, annotation: String) = {
    if (context != null) {
      stats = context.getStats
      if (stats != null) {
        statsSenderId = stats.registerSender(sender, annotation)
      }
    }
  }

  def incStat(name: String): Unit = {
    if (stats != null) {
      stats.inc(statsSenderId, name)
    }
  }

  override def clone(): RowSource = {
    val rs = new RowSource {
      /**
       * Close the data source, releasing all resources (typically files).
       */
      override def close(): Unit = ???

      override def hasNext: Boolean = ???

      override def next(): Row = ???

      override def setPosition(seekChr: String, seekPos: Int): Unit = ???
    }
    rs.bufferSize = bufferSize
    rs.ex = ex
    rs.parent = parent
    rs
  }

}
