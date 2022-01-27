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
import org.gorpipe.gor.model.{GenomicIterator, GenomicIteratorBase, Row}

abstract class TimedRowSource extends GenomicIteratorBase {
  val useAdaptiveMTP = System.getProperty("gor.iterators.useAdaptiveMTP", "true").toBoolean //MTP = moveToPosition

  var myHasNext: Boolean = false
  var myNext: Row = _
  var mustReCheck: Boolean = true
  var measeureTime = false

  var theSource : GenomicIterator = _

  var posSet: Boolean = false
  var bp_per_time = -1.0
  var time_to_seek = -1L
  var seek_time = System.nanoTime()
  var seek_pos = -1
  var last_pos = -1
  var last_time_pos = -1
  var last_time = seek_time
  var row_tik = 0
  var rows_per_time_meas = 1
  var target_time_per_time_meas = 1000000 // 1 ms
  var max_target_time_per_time_meas = 50000000 // 50ms
  var the_time = last_time
  var alpha = 0.33
  var seekalpha = 0.5
  var last_chrom = ""
  var seekCount = 0
  var tryStreamCount = 0
  var ss_max_time : Long = 50000000 // 50ms
  var ss_stop = false
  var ss_use = false
  var ss_start_time = the_time

  val doLog = false
  val sumLog = false
  var lastSeek = false
  var switchCount = 0
  var sumSeekCount = 0
  var sumStreamCount = 0

  override def moveToPosition(seekChr: String, seekPos: Int, maxReads: Int = 10000): Unit = {
    if (useAdaptiveMTP) {
      measeureTime = true
      adaptiveMoveToPosition(seekChr, seekPos)
    } else {
      ss_stop = false
      ss_use = false
      fixedMoveToPosition(seekChr, seekPos, maxReads)
    }
  }

  def openSource(seekChr: String, seekPos: Int, endPos: Int): Unit

  def openSource(seekChr: String, seekPos: Int): Unit = openSource(seekChr, seekPos, -1)

  def openSource(): Unit = openSource(null, -1)

  override def hasNext : Boolean = {
    incStat("hasNext")
    if (!mustReCheck) return myHasNext
    if (theSource == null) openSource()
    mustReCheck = false
    myHasNext = theSource.hasNext
    if (myHasNext) myNext = theSource.next() else myNext = null

    myHasNext
  }

  override def next(): Row = {
    incStat("next")
    if (hasNext) {
      mustReCheck = true
      if (measeureTime) {
        last_pos = myNext.pos
        if (seek_pos < 0) seek_pos = myNext.pos
        row_tik += 1
        if (row_tik >= rows_per_time_meas && myNext != null) measure_time_and_speed(myNext.chr, last_pos)
      }
      myNext
    } else {
      throw new GorSystemException("hasNext: getRow call on false hasNext!", null)
    }
  }

  def measure_time_and_speed(the_chr: String, the_pos: Int) {
    the_time = System.nanoTime()
    val dt = (the_time-last_time).toDouble

    row_tik = 0
    if (dt < 0.5*target_time_per_time_meas) rows_per_time_meas = (rows_per_time_meas* (1.0*target_time_per_time_meas/dt).min(2.0)).toInt
    else if (dt > 2*target_time_per_time_meas) rows_per_time_meas = (rows_per_time_meas* (1.0*target_time_per_time_meas/dt).max(0.5)).toInt
    if (rows_per_time_meas <1 ) rows_per_time_meas = 1

    /* The first estimate of bp_per_time should be high since the clock is set to zero only when the first row shows up */
    if (bp_per_time < 0) bp_per_time = (the_pos-last_time_pos).toDouble/(the_time-last_time)
    else bp_per_time = alpha * (the_pos-last_time_pos).toDouble/(the_time-last_time) + (1-alpha)* bp_per_time
    last_time = the_time
    last_time_pos = the_pos

    if (the_time-seek_time > target_time_per_time_meas && the_time-seek_time > 250000000 /* 250ms */ && target_time_per_time_meas < max_target_time_per_time_meas /* 50ms */) {
      target_time_per_time_meas = (target_time_per_time_meas.toDouble*1.1).toInt
      if (doLog) System.out.println("target_time_per_time_meas "+target_time_per_time_meas/1000000)
    }

    if (ss_use && the_time-ss_start_time > ss_max_time) ss_stop = true

  }

  def adaptiveMoveToPosition(seekChr: String, seekPos: Int, maxReads: Int = 1000000) {
    incStat("adaptiveMoveToPosition "+seekChr+" "+seekPos)
    if (myNext == null || (myNext.chr != null && ((myNext.pos >= seekPos && myNext.chr == seekChr) || myNext.chr > seekChr)))
      return
    val time_to_stream = (seekPos - last_pos)/bp_per_time
    val seekStreamRatio = time_to_seek/time_to_stream.toFloat;

    if(doLog) {
      if (seekStreamRatio < 0.0) System.out.println("seekpos  " + seekPos + " last_pos " + last_pos + " time_to_stream " + time_to_stream + " bp_per_time " + bp_per_time)
      System.out.println("seek/stream " + seekStreamRatio + ", time_to_seek " + time_to_seek / 1000000.0)
      // if (seekStreamRatio<0.0) System.out.println("seek/stream "+seekStreamRatio)
    }

    if (sumLog && (sumSeekCount+sumStreamCount % 100 == 1 || switchCount % 10 == 1)) {
      System.out.println("switchCount "+switchCount)
      System.out.println("sumSeekCount "+sumSeekCount)
      System.out.println("sumStreamCount "+sumStreamCount)
    }


    if (tryStreamCount <3) {
      ss_use = true
      ss_start_time = System.nanoTime()
      ss_max_time = 50000000 // 50ms
      ss_stop = false
    } else {
      ss_use = true
      ss_start_time = System.nanoTime()
      ss_max_time = time_to_seek + 250000000 // 250ms
      ss_stop = false
    }
    if (seekCount<1 || seekCount < 3 &&  tryStreamCount >= 1) seek(seekChr,seekPos)
    else if (last_chrom == seekChr && (seekStreamRatio > 1.0 || seekStreamRatio < 0.0 || tryStreamCount < 3)) { tryStreamCount += 1; fixedMoveToPosition(seekChr,seekPos, maxReads) }
    else seek(seekChr,seekPos)

  }

  def fixedMoveToPosition(seekChr: String, seekPos: Int, maxReads: Int) {
    incStat("fixedMoveToPosition "+seekChr+" "+seekPos+" seekCount "+seekCount+", tryStreamCount "+tryStreamCount)
    if (sumLog) {
      if (lastSeek) switchCount += 1
      lastSeek = false
      sumStreamCount += 1
    }
    var reads = 0
    var reachedPos = false
    var theNext: Row = null
    if (myNext != null && myNext.pos == seekPos && myNext.chr == seekChr) return

    while (reads < maxReads && !reachedPos && hasNext) {
      theNext = next()
      if ((seekPos <= theNext.pos && seekChr == theNext.chr) || seekChr < theNext.chr) reachedPos = true
      else { if (ss_stop) {
        ss_use = false
        ss_stop = false
        if (doLog || sumLog) System.out.println("Terminate reading - reads "+reads)
        reads = maxReads
      } else reads += 1 }
    }
    if (reachedPos) {
      myHasNext = true
      mustReCheck = false
      myNext = theNext
    } else if (hasNext) {
      seek(seekChr, seekPos)
    }
  }

  override def seek(seekChr: String, seekPos: Int): Boolean = {
    incStat("seek "+seekChr+" "+seekPos)
    posSet = true
    mustReCheck = true
    if (useAdaptiveMTP) {
      setPosition(seekChr, seekPos)
    } else {
      oldSetPosition(seekChr, seekPos)
    }
    seekCount += 1
    last_time = System.nanoTime()
    last_time_pos = seekPos
    true
  }

  def oldSetPosition(seekChr: String, seekPos: Int) {
    openSource(seekChr, seekPos)
    myHasNext = hasNext
  }

  def setPosition(chrom: String,pos: Int) {
    the_time = System.nanoTime();
    openSource(chrom, pos)
    if (doLog && seekCount < 10) System.out.println("setPosition "+chrom+" "+pos+", the_time "+the_time)
    if (sumLog) {
      if (!lastSeek) switchCount += 1
      lastSeek = true
      sumSeekCount += 1
    }
    row_tik = 0
    myHasNext = hasNext
    seek_time = System.nanoTime()
    val dt = seek_time - the_time
    last_chrom = chrom
    seek_pos = -1 /* will be set to the right value on first getRow */
    last_time = seek_time
    last_time_pos = pos
    if (time_to_seek < 0) time_to_seek = dt
    else time_to_seek = (seekalpha*dt + (1-seekalpha)*time_to_seek).toLong
    if (doLog) System.out.println("setPosition time_to_seek "+dt+", the_time "+the_time)
  }

}
