/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
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

import gorsat.Commands.Analysis
import gorsat.Iterators.ChromBoundedIteratorSource
import gorsat.gorsatGorIterator.MemoryMonitorUtil
import gorsat.parser.ParseUtilities.{allelesFoundVCF, varSignature}
import org.gorpipe.gor.model.Row
import org.gorpipe.gor.session.GorSession
import org.gorpipe.model.gor.iterators.RowSource
import org.gorpipe.model.gor.RowObj

object VarJoinAnalysis {
  case class ParameterHolder(ic : Boolean, ir : Boolean)
  case class varSEGinfo(start : Int,stop : Int, r : Row, ref : String, alt : String)

  def iabs(x : Int): Int = if (x > 0) x else -x

  // More or less the same logic as in SegOverlap (in gorsatUtilities.scala)

  case class SegVarOverlap(session: GorSession, ph: ParameterHolder, inRightSource : RowSource, missingSeg : String, exactJoin : Boolean, leftJoin : Boolean, fuzzFactor : Int, joinType : String,
                           lleq : List[Int], lreq : List[Int], caseInsensitive : Boolean, maxSegSize : Int, lRefCol : Int, lAltCol : Int, rRefCol : Int, rAltCol : Int, allShare : Int,
                           plainCols : Array[Int], negjoin:Boolean, inclusOnly : Boolean) extends Analysis {

    var rightSource = new ChromBoundedIteratorSource(inRightSource)
    var rightSourceMonitorUtil: MemoryMonitorUtil = if (MemoryMonitorUtil.memoryMonitorActive) new MemoryMonitorUtil(MemoryMonitorUtil.basicOutOfMemoryHandler) else null

    val refSeqProvider = session.getProjectContext.createRefSeq()


    type myRowBufferType = scala.collection.mutable.ArrayBuffer[varSEGinfo]
    var lastRightChr = "chr"
    var lastRightPos = 0
    var maxLeftStop: Int = -1
    var lastLeftChr = "chr"
    var lastSeekChr = "chr"
    val empty: String = missingSeg.split("\t",-1)(0)
    var leftStart = 0
    var leftStop = 0
    val noDistance = 0
    val maxReads = 10000
    var groupClean = 0
    var haplThreas = 1
    val minAllShare: Int = if (allShare <= 1) 1 else allShare
    val plain: Boolean = if (plainCols != null) true else false
    val leq: Array[Int] = lleq.toArray
    val req: Array[Int] = lreq.toArray
    var nothingFromRight: Boolean = if (missingSeg == "") true else false

    if (negjoin) nothingFromRight = true

    var ic = false
    var ir = false
    if (ph != null) {
      if (ph.ic) ic = true
      if (ph.ir) ir = true
    }


    case class GroupHolder() {
      val rowBuffer = new Array[myRowBufferType](2)
      rowBuffer(0) = new myRowBufferType
      rowBuffer(1) = new myRowBufferType
      var buffer = 0
      var bufferSize = 0
    }

    var singleGroupHolder = GroupHolder()
    var groupMap = new scala.collection.mutable.HashMap[String, GroupHolder ]
    //  val useGroup = if (lleq == Nil) false else true
    val useGroup = true

    var gr : GroupHolder = _
    if (!useGroup) groupMap += ("#GR0#" -> singleGroupHolder)

    override def process(lr : Row) {
      if (useGroup && lr.chr != lastLeftChr) {
        groupMap = new scala.collection.mutable.HashMap[String, GroupHolder]
      }
      val lRef = lr.colAsString(lRefCol).toString.toUpperCase()
      val lAlt = lr.colAsString(lAltCol).toString.toUpperCase()

      leftStart = lr.pos; leftStop = leftStart + lRef.length

      // Check if we need to fetch more segments from the right-source, i.e. have we moved upwards with the left-source
      if ( ((maxLeftStop < leftStop && lastLeftChr == lr.chr) || lastLeftChr < lr.chr) &&
        (lastRightChr < lr.chr || (lastRightChr == lr.chr && lastRightPos <= leftStop + fuzzFactor))) {
        if (lr.chr == lastSeekChr && !rightSource.hasNext) { /* do nothing */ }
        else if (lr.chr > lastRightChr) {
          rightSource.setPosition(lr.chr,(lr.pos-fuzzFactor-maxSegSize).max(0))
          lastSeekChr = lr.chr
        } else if (lr.chr == lastRightChr && lr.pos - fuzzFactor - maxSegSize > lastRightPos) {
          rightSource.moveToPosition(lr.chr,lr.pos-fuzzFactor-maxSegSize)
          lastSeekChr = lr.chr
        }
        var keepOn = true
        while (rightSource.hasNext && keepOn) { // Read segments from the right-source
          val rr = rightSource.next()

          if (MemoryMonitorUtil.memoryMonitorActive && rightSourceMonitorUtil != null) {
            rightSourceMonitorUtil.check("VarJoinRightSource", rightSourceMonitorUtil.lineNum, rr)
          }

          var rightStart = 0
          var rightStop = 0
          val rRef = rr.colAsString(rRefCol).toString.toUpperCase()
          val rAlt = rr.colAsString(rAltCol).toString.toUpperCase()
          rightStart = rr.pos
          rightStop = rightStart + rRef.length
          val rSeg = varSEGinfo(rightStart,rightStop,rr,rRef,rAlt)

          val rHashAdd = if (allShare != -1) "#" else if (exactJoin) rRef+'-'+rAlt+(rightStart % 16) else varSignature(rRef,rAlt)

          if (useGroup) {
            val groupKeyRight = if (lreq == Nil) rHashAdd else (if (caseInsensitive) rr.selectedColumns(req).toUpperCase else rr.selectedColumns(req))+rHashAdd
            groupMap.get(groupKeyRight) match {
              case Some(x) =>
                gr = x
              case None =>
                gr = GroupHolder()
                groupMap += (groupKeyRight -> gr)
            }
          } else gr = singleGroupHolder

          if (rr.chr == lr.chr && rightStop >= leftStart - fuzzFactor) {
            if (gr.rowBuffer(gr.buffer).size <= gr.bufferSize) gr.rowBuffer(gr.buffer) += rSeg else gr.rowBuffer(gr.buffer)(gr.bufferSize) = rSeg
            gr.bufferSize += 1
            lastRightChr = rr.chr; lastRightPos = rightStart // rr.pos
          }
          if (rr.chr > lr.chr || (rr.chr == lr.chr && rightStart > leftStop + fuzzFactor)) keepOn = false  // Continue until there is no overlap with the left-seg
        }
      }
      val lSeg = varSEGinfo(leftStart,leftStop,lr,lRef,lAlt)
      var ovlaps = 0

      val lHashAdd = if (allShare != -1) "#" else if (exactJoin) lRef+'-'+lAlt+(leftStart % 16) else varSignature(lRef,lAlt)

      if (useGroup) {
        val groupKeyLeft = if (lleq == Nil) lHashAdd else (if (caseInsensitive) lr.selectedColumns(leq).toUpperCase else lr.selectedColumns(leq))+lHashAdd
        groupMap.get(groupKeyLeft) match {
          case Some(x) => gr = x
          case None =>
            gr = GroupHolder()
            groupMap += (groupKeyLeft -> gr)
        }
      } else gr = singleGroupHolder

      val nextBuffer = (gr.buffer + 1) % 2
      var nextBufferSize = 0
      var i = 0
      while (i < gr.bufferSize) {
        val rSeg = gr.rowBuffer(gr.buffer)(i)
        val rr = rSeg.r
        var use_row_again = true
        if (lr.chr == rr.chr && lSeg.start-fuzzFactor < rSeg.stop && lSeg.stop+fuzzFactor > rSeg.start && (
          exactJoin && (lSeg.start == rSeg.start && lSeg.ref == rSeg.ref && lSeg.alt == rSeg.alt) ||
            allelesFoundVCF(lSeg.start,lSeg.ref,lSeg.alt,rSeg.start,rSeg.ref,rSeg.alt,refSeqProvider,lr.chr) >= minAllShare ) ) {
          if (!negjoin && !ic) {
            if (ir) super.process(rr)
            else if (inclusOnly) { if (ovlaps < 1) super.process(lr) }
            else if (nothingFromRight) super.process(lr)
            else {
              if (plain) super.process(lr.rowWithAddedColumn(rr.selectedColumns(plainCols)))
              else super.process(lr.rowWithAddedColumn(rr.colsSlice(1,rr.numCols)))
            }
          }
          ovlaps += 1
          if (ir) use_row_again = false
        }
        if (!((rr.chr == lr.chr && rSeg.stop+fuzzFactor < lSeg.start) || rr.chr < lr.chr) && use_row_again) {
          if (gr.rowBuffer(nextBuffer).size <= nextBufferSize) gr.rowBuffer(nextBuffer) += gr.rowBuffer(gr.buffer)(i)
          else gr.rowBuffer(nextBuffer)(nextBufferSize) = gr.rowBuffer(gr.buffer)(i)
          gr.rowBuffer(gr.buffer)(i) = null
          nextBufferSize += 1
        }
        i += 1
      }
      if (ovlaps == 0 && leftJoin && !inclusOnly) {
        if (nothingFromRight) super.process(lr)
        else super.process(RowObj(lr.chr,lr.pos,lr.otherCols+missingSeg))
      } else if (ic) {
        super.process(lr.rowWithAddedColumn(ovlaps.toString))
      }
      gr.buffer = nextBuffer
      gr.bufferSize = nextBufferSize
      if ((lr.chr == lastLeftChr && maxLeftStop < leftStop) || lr.chr != lastLeftChr) maxLeftStop = leftStop
      lastLeftChr = lr.chr
      groupClean += 1
      if (groupClean == 10000 && groupMap.size > 1) {
        groupMap.keys.foreach( k => {
          val gr = groupMap(k)
          val nextBuffer = (gr.buffer + 1) % 2
          var nextBufferSize = 0
          var i = 0
          while (i < gr.bufferSize) {
            val rSeg = gr.rowBuffer(gr.buffer)(i)
            val rr = rSeg.r
            if (!((rr.chr == lr.chr && rSeg.stop+fuzzFactor < lSeg.start) || rr.chr < lr.chr)) {
              if (gr.rowBuffer(nextBuffer).size <= nextBufferSize) gr.rowBuffer(nextBuffer) += gr.rowBuffer(gr.buffer)(i)
              else gr.rowBuffer(nextBuffer)(nextBufferSize) = gr.rowBuffer(gr.buffer)(i)
              gr.rowBuffer(gr.buffer)(i) = null
              nextBufferSize += 1
            }
            i += 1
          }
          gr.buffer = nextBuffer
          gr.bufferSize = nextBufferSize
          if (nextBufferSize == 0) { groupMap.remove(k) }
        })
        groupClean = 0
      }

    }
    override def finish() {
      try { refSeqProvider.close() } finally { rightSource.close() }
    }
  }

  case class SegVarJoinSegOverlap(session: GorSession, ph: ParameterHolder, rightSource : RowSource, missingB : String, exactJoin : Boolean, leftJoin : Boolean, fuzz : Int,
                                  leq : List[Int], req : List[Int], caseInsensitive : Boolean, maxSegSize : Int,
                                  lRef : Int, lAlt : Int, rRef : Int, rAlt : Int, allShare : Int, plainCols : Array[Int], negjoin:Boolean) extends Analysis {
    this | SegVarOverlap(session, ph,rightSource, missingB, exactJoin, leftJoin, fuzz, "segseg", leq, req, caseInsensitive, maxSegSize, lRef, lAlt, rRef, rAlt, allShare, plainCols, negjoin, inclusOnly = false)
  }
  case class SegVarJoinSegOverlapInclusOnly(session: GorSession, ph: ParameterHolder, rightSource : RowSource, missingB : String, exactJoin : Boolean, leftJoin : Boolean, fuzz : Int,
                                            leq : List[Int], req : List[Int], caseInsensitive : Boolean, maxSegSize : Int,
                                            lRef : Int, lAlt : Int, rRef : Int, rAlt : Int, allShare : Int, plainCols : Array[Int], negjoin:Boolean) extends Analysis {
    this | SegVarOverlap(session, ph,rightSource, missingB, exactJoin, leftJoin, fuzz, "segseg", leq, req, caseInsensitive, maxSegSize, lRef, lAlt, rRef, rAlt, allShare, plainCols, negjoin, inclusOnly = true)
  }
}
