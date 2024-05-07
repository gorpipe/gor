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
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.model.{GorOptions, MonitorIterator}
import org.gorpipe.gor.monitor.GorMonitor
import org.gorpipe.gor.session.GorContext
import org.gorpipe.gor.util.StringUtil
import org.gorpipe.model.gor.iterators.TimedRowSource
import org.gorpipe.util.standalone.GorStandalone

class FastGorSource(inOptions: String, gorRoot: String, context: GorContext, executeNor: Boolean, gm: GorMonitor, minLogTime: Int) extends TimedRowSource {
  var options: String = inOptions
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
  private val pathToTime = if (context != null && context.getSession != null && inOptions != "") context.getSession.getCache.getSeekTimes else null
  private val usingCache = inOptions != "" && pathToTime != null
  private var estSeekTime: Long = -1
  private var soughtTwice = false
  private var soughtOnce = false

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

  override def openSource(chr: String, pos: Int, end: Int): Unit = {
    if (theSource == null) {
      val gorOptions = GorOptions.createGorOptions(context, StringUtil.splitReserveQuotesAndParenthesesToArray(options))
      theSource = gorOptions.getIterator(gm)

      initStats(context, theSource.getSourceName, "FastGorSource")
      incStat("openSource")

      if(gm != null) {
        theSource = new MonitorIterator(theSource, gm, minLogTime)
      }
      val header = theSource.getHeader
      setHeader(header)
      headerLength = header.split("\t").length
      if (chrpos != "") {
        theSource.seek(seekChr, seekPos)
      }
    }
    if (chr!=null) {
      theSource.seek(chr, pos)
    }
  }

  def close(): Unit = synchronized {
    //Update the cache
    if (usingCache) {
      context.getSession.getCache.getSeekTimes.put(inOptions, estSeekTime)
    }
    if (theSource != null) theSource.close()
    theSource = null
  }

  override def getHeader: String = {
    val header = super.getHeader
    if (header == null || header.isEmpty) {
      openSource()
    }
    super.getHeader
  }

  override def pushdownFilter(gorwhere: String): Boolean = {
    if (theSource == null) openSource()
    theSource.pushdownFilter(gorwhere)
  }
}
