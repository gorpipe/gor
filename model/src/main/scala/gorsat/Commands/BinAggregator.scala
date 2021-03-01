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

import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.model.Row


case class BinAggregator(binFactory: BinFactory, numBins: Int, window: Int, useKeyForChrom: Boolean = false) {
  if (numBins < window) {
    throw new GorParsingException(s"Error in BinAggregator setup - numBins ($numBins) must be larger than window ($window): ")
  }
  private val bins: Array[BinState] = Array.fill(numBins) { binFactory.create }
  private val binInfo: Array[BinInfo] = Array.fill(numBins) { new BinInfo }
  private var sBinID = 0
  private var eBinID = 0
  private var currentKey: String = _
  private var nextProcessor: Processor = _

  def setNextProcessor(p: Processor) {
    nextProcessor = p
  }

  def mInd(binID: Int): Int = {
    val cand = binID % numBins
    if (cand < 0) cand + numBins
    else cand
  }

  def update(r: Row, binID: Int, key: String, sta: Int, sto: Int) {
    val mID = mInd(binID)
    if (currentKey != key || binID - sBinID >= numBins) {
      flush(key, binID)
    }

    val currentBinInfo = binInfo(mID)
    val currentBin = bins(mID)
    if (!currentBinInfo.used) {
      currentBinInfo.chr = if (useKeyForChrom) key else r.chr
      currentBinInfo.key = key
      currentBinInfo.sta = sta
      currentBinInfo.sto = sto
      currentBin.initialize(currentBinInfo)
    }
    currentBinInfo.used = true
    currentBin.process(r)

    if (eBinID < binID) {
      eBinID = binID
    }

    if (sBinID > binID) {
      // This is to allow for small deviation from GOR, i.e. backward order in a sliding window
      sBinID = binID
    }
  }

  def flush(newKey: String, curBinID: Int): Unit = {
    var mEnd = 0
    if (curBinID - sBinID >= numBins + window || newKey != currentKey) {
      // We must flush everything
      mEnd = eBinID - sBinID
    } else {
      mEnd = curBinID - window - sBinID // This number must be positive because of the condition on binID-sBinID on flush-call
    }
    for (i <- 0 to mEnd) {
      val mID = mInd(sBinID + i)
      if (binInfo(mID).used) {
        bins(mID).sendToNextProcessor(binInfo(mID), nextProcessor)
        binInfo(mID).used = false
      }
    }
    eBinID = curBinID
    if (curBinID - sBinID >= numBins + window || newKey != currentKey) {
      sBinID = curBinID
      currentKey = newKey
    } else sBinID += mEnd
  }

  def finalFlush(): Unit = {
    val mEnd = eBinID - sBinID
    try {
      for (i <- 0 to mEnd) {
        val mID = mInd(sBinID + i)
        if (binInfo(mID).used) {
          bins(mID).sendToNextProcessor(binInfo(mID), nextProcessor)
          binInfo(mID).used = false
        }
      }
    } finally {
      for (i <- 0 to mEnd) {
        try {
          val mID = mInd(sBinID + i)
          bins(mID).close()
        } catch {
          case _: Exception =>
          // ignore close error
        }
      }
    }
    currentKey = null
  }
}

