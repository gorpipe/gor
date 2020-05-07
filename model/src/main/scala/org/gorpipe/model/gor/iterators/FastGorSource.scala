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

import gorsat.Commands.CommandParseUtilities
import org.gorpipe.exceptions.{GorParsingException, GorSystemException}
import org.gorpipe.gor.GorContext
import org.gorpipe.model.genome.files.gor._
import org.gorpipe.util.gorutil.standalone.GorStandalone
import org.gorpipe.util.string.StringUtil

class FastGorSource(inOptions: String, gorRoot: String, context: GorContext, executeNor: Boolean, gm: GorMonitor, minLogTime: Int, readAll: Boolean = true) extends TimedRowSource {
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
  private var maxReads = 10000  //The maximum number of reads (next calls) when running to a destination instead of seeking.
  private val checksPerMaxRun = 10  //We check whether

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

  override def next(): Row = {
    incStat("next")
    if (hasNext) {
      mustReCheck = true
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

  /**
    * The new moveToPosition method.
    * adaptiveMoveToPosition decides whether to run or seek depending on estimates on how long
    * we can run within @estSeekTime. The estimates are computed using exponential weighting of the
    * the measured values.
    */
  def adaptiveMoveToPosition(seekChr: String, seekPos: Int) {
    incStat("adaptiveMoveToPosition")
    if (myNext == null || (myNext.chr != null && ((myNext.pos >= seekPos && myNext.chr == seekChr) || myNext.chr > seekChr)))
      return //We are at the position so nothing more to do.
    val dist = seekPos - myNext.pos
    if (soughtTwice && dist < maxDist) {
      carefulRunner(seekChr, seekPos, dist)
    } else if (hasNext) {
      setPosition(seekChr, seekPos)
    }
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
      setPosition(seekChr, seekPos)
    }
  }

  private def carefulRunner(seekChr: String, seekPos: Int, dist: Int) {
    val startPos = myNext.pos
    var reachedPos = false
    var late = false
    var theNext: Row = null
    var count = 0
    var modCounter = 0
    var seek = false
    var startTime = System.nanoTime
    var distLeft = dist
    while (!reachedPos && !seek && hasNext) {
      count += 1
      modCounter += 1
      theNext = next()
      if ((seekPos <= theNext.pos && seekChr == theNext.chr) || theNext.chr > seekChr) reachedPos = true
      // If we have not reached the position after a plenty of iterations we should check whether we have been spending to much time streaming.
      else if (modCounter == check) {
        modCounter = 0
        val time = System.nanoTime - startTime
        //If we have spent more time streaming then it takes to seek we should consider to give up on streaming.
        if (time > estSeekTime) {
          //Update the estimates
          val tmpDist = theNext.pos - startPos
          distLeft -= tmpDist
          updateEstimates(count, tmpDist, time)
          count = 0
          if ((late || distLeft > maxDist) && hasNext) {
            seek = true
            setPosition(seekChr,seekPos)
          }
          late = true //If we are late again we will seek.
        }
      }
    }
    if (reachedPos) {
      val time = System.nanoTime - startTime
      if(time > estSeekTime) {
        updateEstimates(count, distLeft, time)
      }
      myHasNext = true
      mustReCheck = false
      myNext = theNext
    }
  }

  private def updateEstimates(count: Int, dist: Int, time: Long): Unit = {
    val timeRatio = estSeekTime.toDouble / time
    lazy val coef = Math.pow(oneMinusExpWCoef, 1 / timeRatio)
    maxReads = if (maxDistEstimated) ((1 - coef) * count + coef * maxReads).toInt else (count * timeRatio).toInt
    maxDist = if (maxDistEstimated) ((1 - coef) * dist + coef * maxReads).toInt else (dist * timeRatio).toInt
    maxDistEstimated = true; check = maxReads / checksPerMaxRun
  }

  override def setPosition(seekChr: String, seekPos: Int) {
    posSet = true
    mustReCheck = true
    if (useAdaptiveMTP) {
      newSetPosition(seekChr, seekPos)
    } else {
      oldSetPosition(seekChr, seekPos)
    }
  }

  def newSetPosition(seekChr: String, seekPos: Int): Unit = {
    if (gorSource == null) openSource()
    val startTime = System.nanoTime
    gorSource.seek(seekChr, seekPos)
    myHasNext = hasNext
    val measuredSeekTime = System.nanoTime - startTime
    if (soughtOnce) {
      if (soughtTwice) {
        val oldEstSeekTime = estSeekTime
        estSeekTime = (expWCoef * measuredSeekTime + oneMinusExpWCoef * estSeekTime).toLong
      } else {
        soughtTwice = true
        estSeekTime = measuredSeekTime
      }
    } else soughtOnce = true
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
    if (header == null || header.length == 0) {
      openSource()
    }
    super.getHeader
  }

  override def pushdownFilter(gorwhere: String): Boolean = {
    if (gorSource == null) openSource()
    gorSource.pushdownFilter(gorwhere)
  }
}
