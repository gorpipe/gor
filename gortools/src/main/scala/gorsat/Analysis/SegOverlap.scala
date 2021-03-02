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

package gorsat.Analysis

import gorsat.Utilities.AnalysisUtilities.{ParameterHolder, SEGinfo, distSegSeg}
import gorsat.Commands.Analysis
import gorsat.Iterators.ChromBoundedIteratorSource
import org.gorpipe.exceptions.GorDataException
import org.gorpipe.gor.model.{GenomicIterator, Row}
import org.gorpipe.model.gor.RowObj


case class SegOverlap(ph: ParameterHolder, inRightSource: GenomicIterator, missingSeg: String, leftJoin: Boolean, fuzzFactor: Int, iJoinType: String,
                      lstop: Int, rstop: Int, lleq: List[Int], lreq: List[Int], maxSegSize: Int, plain: Boolean, inclusOnly: Boolean = false) extends Analysis {

  var rightSource = new ChromBoundedIteratorSource(inRightSource)

  type myRowBufferType = scala.collection.mutable.ArrayBuffer[SEGinfo]
  var lastRightChr = "chr"
  var lastRightPos = 0
  var maxLeftStop = -1
  var lastLeftChr = "chr"
  var lastSeekChr = "chr"

  var joinType = iJoinType

  var elimCols = 0
  if (plain) {
    if (joinType == "snpseg" || joinType == "segseg") elimCols = 1 else elimCols = 0
  }
  val nothingFromRight = if (missingSeg == "") true else false

  var varsegleft = false
  var varsegright = false
  var lref = 3
  var rref = 3
  if (ph != null) {
    if (ph.varsegleft) joinType = "seg" + joinType.slice(3, 6)
    if (ph.varsegright) joinType = joinType.slice(0, 3) + "seg"
    varsegleft = ph.varsegleft
    varsegright = ph.varsegright
    lref = ph.lref
    rref = ph.rref
    if (plain && varsegright) elimCols = -1
  }

  var segseg = if (joinType == "segseg") true else false
  var snpsnp = if (joinType == "snpsnp") true else false
  var segsnp = if (joinType == "segsnp") true else false

  var leftStart = 0
  var leftStop = 0
  val noDistance = if (plain || fuzzFactor == 0 && joinType == "snpsnp") true else false
  var groupClean = 0
  val leq = lleq.toArray
  val req = lreq.toArray

  case class GroupHolder() {
    val rowBuffer = new Array[myRowBufferType](2)
    rowBuffer(0) = new myRowBufferType
    rowBuffer(1) = new myRowBufferType
    var buffer = 0
    var bufferSize = 0
  }

  var singleGroupHolder = GroupHolder()
  var groupMap = new scala.collection.mutable.HashMap[String, GroupHolder]
  val useGroup = if (lleq == Nil) false else true
  var gr: GroupHolder = _
  if (!useGroup) groupMap += ("#GR0#" -> singleGroupHolder)

  override def process(lr: Row) {
    if (useGroup && lr.chr != lastLeftChr) {
      groupMap = new scala.collection.mutable.HashMap[String, GroupHolder]
    }
    if (segseg || segsnp) {
      try {
        if (varsegleft) {
          leftStart = lr.pos
          leftStop = leftStart - 1 + lr.colAsString(lref).length
        } else {
          leftStart = lr.pos
          leftStop = lr.colAsInt(lstop)
        }
      } catch {
        case e: Exception => throw new GorDataException("Illegal stop position in column #" + (lstop + 1) +
          " in the JOIN left-source.", lstop + 1, "", lr.toString(), e)
      }
    } else {
      if (varsegleft) {
        leftStart = lr.pos - 1
        leftStop = leftStart + lr.colAsString(lref).length
      } else {
        leftStart = lr.pos - 1
        leftStop = lr.pos
      }
    }
    // Check if we need to fetch more segments from the right-source, i.e. have we moved upwards with the left-source
    if (((maxLeftStop < leftStop && lastLeftChr == lr.chr) || lastLeftChr < lr.chr) &&
      (lastRightChr < lr.chr || (lastRightChr == lr.chr && lastRightPos <= leftStop + fuzzFactor))) {
      if (lr.chr == lastSeekChr && !rightSource.hasNext) {
        /* do nothing */
      }
      else if (lr.chr > lastRightChr) {
        if (snpsnp || segsnp) rightSource.seek(lr.chr, (lr.pos - fuzzFactor).max(0))
        else rightSource.seek(lr.chr, (lr.pos - fuzzFactor - maxSegSize).max(0))
        lastSeekChr = lr.chr
      } else if (lr.chr == lastRightChr && lr.pos - fuzzFactor - maxSegSize > lastRightPos) {
        if (snpsnp || segsnp) rightSource.moveToPosition(lr.chr, (lr.pos - fuzzFactor).max(0))
        else rightSource.moveToPosition(lr.chr, lr.pos - fuzzFactor - maxSegSize)
        lastSeekChr = lr.chr
      }
      var keepOn = true
      while (keepOn && rightSource.hasNext) {
        // Read segments from the right-source
        val rr = rightSource.next
        var rightStart = 0
        var rightStop = 0

        if (snpsnp || segsnp) {
          if (varsegright) {
            rightStart = rr.pos - 1
            rightStop = rr.pos + rr.colAsString(rref).length
          } else {
            rightStart = rr.pos - 1
            rightStop = rr.pos
          }
        } else {
          rightStart = rr.pos
          try {
            if (varsegright) {
              rightStop = rightStart - 1 + rr.colAsString(rref).length
            } else {
              rightStop = rr.colAsInt(rstop)
            }
          } catch {
            case e: Exception => throw new GorDataException("Illegal stop position in column #" + (rstop + 1) + " in the JOIN right-source.", rstop + 1, "", rr.toString(), e);
          }
        }
        val rSeg = SEGinfo(rightStart, rightStop, rr)

        if (useGroup) {
          val groupKeyRight = rr.selectedColumns(req)
          groupMap.get(groupKeyRight) match {
            case Some(x) => gr = x
            case None => {
              gr = GroupHolder()
              groupMap += (groupKeyRight -> gr)
            }
          }
        } else gr = singleGroupHolder

        if (rr.chr == lr.chr && rightStop >= leftStart - fuzzFactor) {
          if (gr.rowBuffer(gr.buffer).size <= gr.bufferSize) gr.rowBuffer(gr.buffer) += rSeg else gr.rowBuffer(gr.buffer)(gr.bufferSize) = rSeg
          gr.bufferSize += 1
          lastRightChr = rr.chr
          lastRightPos = rightStart // rr.pos
        }
        if (rr.chr > lr.chr || (rr.chr == lr.chr && rightStart > leftStop + fuzzFactor)) keepOn = false // Continue until there is no overlap with the left-seg
      }

    }
    val lSeg = SEGinfo(leftStart, leftStop, lr)
    var ovlaps = 0

    var groupKeyLeft = null
    if (useGroup) {
      val groupKeyLeft = lr.selectedColumns(leq)
      groupMap.get(groupKeyLeft) match {
        case Some(x) => gr = x
        case None => {
          gr = GroupHolder()
          groupMap += (groupKeyLeft -> gr)
        }
      }
    } else gr = singleGroupHolder

    val nextBuffer = (gr.buffer + 1) % 2
    var nextBufferSize = 0
    var i = 0
    while (i < gr.bufferSize) {
      val rSeg = gr.rowBuffer(gr.buffer)(i)
      val rr = rSeg.r
      if (lr.chr == rr.chr && lSeg.start - fuzzFactor < rSeg.stop && lSeg.stop + fuzzFactor > rSeg.start) {
        if (inclusOnly) {
          if (ovlaps < 1) super.process(lr)
        }
        else if (nothingFromRight) super.process(lr)
        else if (noDistance) super.process(lr.rowWithAddedColumn(rr.colsSlice(2 + elimCols, rr.numCols)))
        else super.process(lr.rowWithAddedColumn(distSegSeg(lSeg, rSeg) + "\t" + rr.colsSlice(1, rr.numCols)))
        ovlaps += 1
      }
      if (!((rr.chr == lr.chr && rSeg.stop + fuzzFactor < lSeg.start) || rr.chr < lr.chr)) {
        if (gr.rowBuffer(nextBuffer).size <= nextBufferSize) gr.rowBuffer(nextBuffer) += gr.rowBuffer(gr.buffer)(i)
        else gr.rowBuffer(nextBuffer)(nextBufferSize) = gr.rowBuffer(gr.buffer)(i)
        gr.rowBuffer(gr.buffer)(i) = null
        nextBufferSize += 1
      }
      i += 1
    }
    if (ovlaps == 0 && leftJoin && !inclusOnly) {
      if (nothingFromRight) super.process(lr)
      else super.process(RowObj(lr.chr, lr.pos, lr.otherCols + missingSeg))
    }
    gr.buffer = nextBuffer
    gr.bufferSize = nextBufferSize
    if ((lr.chr == lastLeftChr && maxLeftStop < leftStop) || lr.chr != lastLeftChr) maxLeftStop = leftStop
    lastLeftChr = lr.chr
    groupClean += 1
    if (groupClean == 100000 && useGroup && groupMap.size > 1) {
      groupMap.keys.foreach(k => {
        val gr = groupMap(k)
        val nextBuffer = (gr.buffer + 1) % 2
        var nextBufferSize = 0
        var i = 0
        while (i < gr.bufferSize) {
          val rSeg = gr.rowBuffer(gr.buffer)(i)
          val rr = rSeg.r
          if (!((rr.chr == lr.chr && rSeg.stop + fuzzFactor < lSeg.start) || rr.chr < lr.chr)) {
            if (gr.rowBuffer(nextBuffer).size <= nextBufferSize) gr.rowBuffer(nextBuffer) += gr.rowBuffer(gr.buffer)(i)
            else gr.rowBuffer(nextBuffer)(nextBufferSize) = gr.rowBuffer(gr.buffer)(i)
            gr.rowBuffer(gr.buffer)(i) = null
            nextBufferSize += 1
          }
          i += 1
        }
        gr.buffer = nextBuffer
        gr.bufferSize = nextBufferSize
        if (nextBufferSize == 0) {
          groupMap.remove(k)
        }
      })
      groupClean = 0
    }
  }

  override def finish {
    rightSource.close
  }


}
