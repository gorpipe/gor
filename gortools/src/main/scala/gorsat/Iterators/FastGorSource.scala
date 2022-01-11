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

import gorsat.Commands.CommandParseUtilities
import org.gorpipe.exceptions.{GorParsingException, GorSystemException}
import org.gorpipe.gor.model.{GenomicIterator, GorOptions, MonitorIterator, Row}
import org.gorpipe.gor.monitor.GorMonitor
import org.gorpipe.gor.session.GorContext
import org.gorpipe.gor.util.StringUtil
import org.gorpipe.model.gor.iterators.TimedRowSource
import org.gorpipe.util.standalone.GorStandalone

class FastGorSource(inOptions: String, gorRoot: String, context: GorContext, executeNor: Boolean, gm: GorMonitor, minLogTime: Int) extends TimedRowSource {
  private val useAdaptiveMTP = System.getProperty("gor.iterators.useAdaptiveMTP", "true").toBoolean //MTP = moveToPosition
  var posSet: Boolean = false
  var gorSource: GenomicIterator = _
  var options: String = if (gorRoot != "" && (!GorStandalone.isStandalone || !GorStandalone.isURL(inOptions))) "-r " + gorRoot + " " + inOptions else inOptions
  val args: Array[String] = options.split(' ')
  var chrpos = ""
  var seekChr = ""
  var seekPos = 0
  try {
    if (CommandParseUtilities.hasOption(args, "-seek")) {
      chrpos = CommandParseUtilities.stringValueOfOption(args, "-seek")
      options = options.replace("-seek " + chrpos, "")
      seekChr = chrpos.split(':')(0)
      seekPos = chrpos.split(':')(1).toInt
    }
  } catch {
    case _:Exception => throw new GorParsingException("Failed to parse seek position: " + args.mkString(","), "-seek", chrpos)
  }
  var headerLength: Int = 0
  private var maxDist: Int = 100000000 //The maximum distance for streaming.
  private var check = 1000 //When streaming, after check many iterations we check whether we have been spending to much time streaming.
  private val pathToTime = if (context != null && context.getSession != null && inOptions != "") context.getSession.getCache.getSeekTimes else null
  private val usingCache = inOptions != "" && pathToTime != null
  private var estSeekTime: Long = -1
  private var soughtTwice = false
  private var soughtOnce = false
  private val expWCoef = 0.1  //The coefficient to use in the exponential weighting of the measured values.
  private val oneMinusExpWCoef = 1 - expWCoef
  private var maxDistEstimated = false
  private var maxReads = 100000  //The maximum number of reads (next calls) when running to a destination instead of seeking.
  private val checksPerMaxRun = 10  //We check

  private var lastCount = 0
  private var lastDist = 0
  private var lastTime = 0L

  var bp_per_time = -1.0
  var time_to_seek = -1L
  var seek_time = System.nanoTime()
  var seek_pos = -1
  var last_pos = -1
  var row_tik = 0
  var rows_per_time_meas = 1
  var target_time_per_time_meas = 1000000 // 1 ms
  var last_time = seek_time
  var the_time = last_time
  var alpha = 0.1
  var last_chrom = ""

  if (context != null && context.getSession != null) {
    context.getSession.setNorContext(executeNor)
  }

  /**
    * The seek times are cached. If we have been working with the same file in the same session
    * we should use the seektime estimate since then.
    */
  if (usingCache && pathToTime.containsKey(inOptions)) {
    estSeekTime = pathToTime.get(inOptions)
    soughtTwice = true; soughtOnce = true
  }

  override def toString: String = {
    inOptions
  }

  def openSource() {
    if (gorSource == null) {
      val gorOptions = GorOptions.createGorOptions(context, StringUtil.splitReserveQuotesAndParenthesesToArray(options))
      gorSource = gorOptions.getIterator(gm)

      initStats(context, gorSource.getSourceName, "FastGorSource")
      incStat("openSource")

      if(gm != null) {
        gorSource = new MonitorIterator(gorSource, gm, minLogTime)
      }
      val header = gorSource.getHeader
      setHeader(header)
      headerLength = header.split("\t").length
      if (chrpos != "") {
        gorSource.seek(seekChr, seekPos)
      }
    }
  }

  override def hasNext: Boolean = {
    incStat("hasNext")
    if (gorSource == null ) openSource()
    if (!mustReCheck) return myHasNext
    if(gorSource.hasNext) {
      myNext = gorSource.next
    } else {
      myNext = null
    }
    myHasNext = myNext != null
    mustReCheck = false
    myHasNext
  }

  private def measure_time_and_speed(the_pos: Int) {
    the_time = System.nanoTime()
    val dt = (the_time-last_time).toDouble
    last_time = the_time

    row_tik = 0
    if (dt < 0.5*target_time_per_time_meas) rows_per_time_meas = rows_per_time_meas* (target_time_per_time_meas/dt).min(2.0).toInt
    else if (dt > 2*target_time_per_time_meas) rows_per_time_meas = rows_per_time_meas* (target_time_per_time_meas /dt).max(0.5).toInt
    if (rows_per_time_meas <1 ) rows_per_time_meas = 1

    /* The first estimate of bp_per_time should be high since the clock is set to zero only when the first row shows up */
    if (bp_per_time < 0) bp_per_time = (the_pos-seek_pos).toDouble/(the_time-seek_time)
    else bp_per_time = alpha * (the_pos-seek_pos).toDouble/(the_time-seek_time) + (1-alpha)* bp_per_time
  }

  override def next(): Row = {
    incStat("next")
    if (hasNext) {
      mustReCheck = true
      last_pos = myNext.pos
      if (seek_pos < 0) seek_pos = myNext.pos
      row_tik += 1
      if (row_tik >=  rows_per_time_meas) measure_time_and_speed(last_pos)
      myNext
    } else {
      throw new GorSystemException("hasNext: getRow call on false hasNext!", null)
    }
  }

  override def moveToPosition(seekChr: String, seekPos: Int, maxReads: Int = 10000): Unit = {
    if (useAdaptiveMTP) {
      adaptiveMoveToPosition(seekChr, seekPos)
    } else {
      fixedMoveToPosition(seekChr, seekPos, maxReads)
    }
  }

  def adaptiveMoveToPosition(chrom: String,pos: Int, maxReads: Int = 1000000) {
    incStat("adaptiveMoveToPosition")
    if (myNext == null || (myNext.chr != null && ((myNext.pos >= seekPos && myNext.chr == seekChr) || myNext.chr > seekChr)))
      return
    val time_to_stream = (pos - last_pos)/bp_per_time
    if (last_chrom == chrom && time_to_seek < 1.5 * time_to_stream) fixedMoveToPosition(chrom,pos,1000000)
    else seek(chrom,pos)
  }

  //The old moveToPosition. Its execution plan is not adaptive.
  def fixedMoveToPosition(seekChr: String, seekPos: Int, maxReads: Int) {
    incStat("fixedMoveToPosition")
    var reads = 0
    var reachedPos = false
    var theNext: Row = null
    if (myNext != null && myNext.pos == seekPos && myNext.chr == seekChr) return

    while (reads < maxReads && !reachedPos && hasNext) {
      theNext = next()
      if ((seekPos <= theNext.pos && seekChr == theNext.chr) || seekChr < theNext.chr) reachedPos = true else reads += 1
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
    posSet = true
    mustReCheck = true
    if (useAdaptiveMTP) {
      setPosition(seekChr, seekPos)
    } else {
      oldSetPosition(seekChr, seekPos)
    }
    true
  }

  def setPosition(chrom: String,pos: Int) {
    if (gorSource == null) openSource()
    the_time = System.nanoTime();
    gorSource.seek(chrom,pos)
    val dt = System.nanoTime()-the_time
    row_tik = 0
    myHasNext = hasNext
    seek_time = System.nanoTime()
    last_chrom = chrom
    seek_pos = -1 /* will be set to the right value on first getRow */
    last_time= seek_time
    if (time_to_seek < 0) time_to_seek = dt
    else time_to_seek = (alpha*dt + (1-alpha)*time_to_seek).toLong
  }

  def oldSetPosition(seekChr: String, seekPos: Int) {
    if (gorSource == null) openSource()
    gorSource.seek(seekChr, seekPos)
    myHasNext = hasNext
  }

  def close(): Unit = synchronized {
    //Update the cache
    if (usingCache) {
      context.getSession.getCache.getSeekTimes.put(inOptions, estSeekTime)
    }
    if (gorSource != null) gorSource.close()
    gorSource = null
  }

  override def getHeader: String = {
    val header = super.getHeader
    if (header == null || header.isEmpty) {
      openSource()
    }
    super.getHeader
  }

  override def pushdownFilter(gorwhere: String): Boolean = {
    if (gorSource == null) openSource()
    gorSource.pushdownFilter(gorwhere)
  }
}
