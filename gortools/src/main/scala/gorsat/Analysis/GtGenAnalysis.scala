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

import gorsat.Commands._
import gorsat.Iterators.ChromBoundedIteratorSource
import gorsat.gorsatGorIterator.{MapAndListUtilities, MemoryMonitorUtil}
import gorsat.{PnBucketParsing, PnBucketTable}
import org.gorpipe.exceptions.GorDataException
import org.gorpipe.gor.model.Row
import org.gorpipe.gor.session.GorContext
import org.gorpipe.model.gor.iterators.{LineIterator, RowSource}
import org.gorpipe.model.gor.RowObj

import scala.collection.mutable

object GtGenAnalysis {


  case class GtGenState(context: GorContext, lookupSignature: String, GtCol: Int, PNCol: Int, grCols: List[Int]) extends BinState {

    case class ColHolder() {
      var buckValueCols: Array[mutable.StringBuilder] = _
    }

    val useGroup: Boolean = if (grCols.nonEmpty) true else false

    var pbt: PnBucketTable = _
    var groupMap = scala.collection.mutable.HashMap.empty[String, ColHolder]
    var singleColHolder = ColHolder()
    if (!useGroup) groupMap += ("theOnlyGroup" -> singleColHolder)
    val grColsArray: Array[Int] = grCols.toArray


    def initColHolder(sh: ColHolder) {
      sh.buckValueCols = new Array[mutable.StringBuilder](pbt.numberOfBuckets)
      var i = 0
      while (i < pbt.numberOfBuckets) {
        val buckSize = pbt.buckIdxToBuckSize(i)
        sh.buckValueCols(i) = new mutable.StringBuilder(buckSize)
        sh.buckValueCols(i).setLength(buckSize)
        var j = 0
        while (j < buckSize) {
          sh.buckValueCols(i).setCharAt(j, '4') // Unspecified, only 4 will possibly be changed to 0 and not 3.
          j += 1
        }
        i += 1
      }
    }

    def initialize(binInfo: BinInfo): Unit = {
      pbt = context.getSession.getCache.getObjectHashMap.get(lookupSignature).asInstanceOf[PnBucketTable]
      if (pbt == null) throw new GorDataException("Non existing bucket info for lookupSignature " + lookupSignature)
      if (useGroup) groupMap = scala.collection.mutable.HashMap.empty[String, ColHolder]
      else initColHolder(singleColHolder)
    }

    def process(r: Row) {
      var sh: ColHolder = null
      if (useGroup) {
        val groupID = r.selectedColumns(grColsArray)
        groupMap.get(groupID) match {
          case Some(x) => sh = x
          case None =>
            sh = ColHolder()
            initColHolder(sh)
            groupMap += (groupID -> sh)
        }
      } else sh = singleColHolder

      val PNtag = r.colAsString(PNCol).toString

      pbt.pnToIdx.get(PNtag) match {
        case Some(idx) =>
          val buckID = pbt.getBucketIdxFromPn(idx)
          val buckPos = pbt.getBucketPos(idx)
          sh.buckValueCols(buckID).setCharAt(buckPos, r.colAsString(GtCol).charAt(0)) // Set the GT into the values col
        case None =>
          if (PNtag != "") throw new GorDataException("No bucket information found for tag value: " + PNtag + "\n")
      }
    }

    def sendToNextProcessor(bi: BinInfo, nextProcessor: Processor) {
      for (key <- groupMap.keys.toList.sorted) {
        var sh: ColHolder = null
        if (useGroup) sh = groupMap(key) else sh = groupMap("theOnlyGroup")
        val linestart: String = bi.chr + "\t" + (bi.sta + 1) + (if (useGroup) "\t" + key else "")
        var i = 0
        while (i < sh.buckValueCols.length) {
          val line = linestart + '\t' + pbt.getBucketNameFromIdx(i) + '\t' + sh.buckValueCols(i)
          nextProcessor.process(RowObj(line))
          i += 1
        }
      }
    }
  }

  case class GtGenFactory(context: GorContext, lookupSignature: String, GtCol: Int, PNCol: Int, grCols: List[Int]) extends BinFactory {
    def create: BinState =
      GtGenState(context, lookupSignature, GtCol, PNCol, grCols)
  }

  case class GtGenAnalysis(fileName1: String, iteratorCommand1: String, iterator1: LineIterator, GtCol: Int, PNCol: Int,
                           grCols: List[Int], context: GorContext, lookupSignature: String) extends
    BinAnalysis(RegularRowHandler(1), BinAggregator(GtGenFactory(context, lookupSignature, GtCol, PNCol, grCols), 2, 1)) {

    context.getSession.getCache.getObjectHashMap.computeIfAbsent(lookupSignature, _ => {
      var l1 = Array.empty[String]
      try {
        if (iteratorCommand1 != "") l1 = MapAndListUtilities.getStringArray(iteratorCommand1, iterator1, context.getSession)
        else l1 = MapAndListUtilities.getStringArray(fileName1, context.getSession)
      } catch {
        case e: Exception =>
          iterator1.close()
          throw e
      }
      PnBucketParsing.parse(l1)
    })
  }


  case class CovSEGinfo(start: Int, stop: Int, r : Row, var buckPos : Int, var values : mutable.StringBuilder)

  case class TheSegOverlap(inRightSource: RowSource, bucketCol : Int, PNcol : Int, maxSegSize: Int, context: GorContext, lookupSignature : String) extends Analysis {

    var pbt: PnBucketTable = _

    val rstop = 2

    var rightSource = new ChromBoundedIteratorSource(inRightSource)
    var rightSourceMonitorUtil: MemoryMonitorUtil = if (MemoryMonitorUtil.memoryMonitorActive) new MemoryMonitorUtil(MemoryMonitorUtil.basicOutOfMemoryHandler) else null
    type myRowBufferType = scala.collection.mutable.ArrayBuffer[CovSEGinfo]
    var lastRightChr = "chr"
    var lastRightPos = 0
    var maxLeftStop: Int = -1
    var lastLeftChr = "chr"
    var lastSeekChr = "chr"

    var leftStart = 0
    var leftStop = 0
    var next_leftStart = 0

    var groupClean = 0

    val valuesCol: Int = bucketCol +1

    case class GroupHolder() {
      val rowBuffer = new Array[myRowBufferType](2)
      rowBuffer(0) = new myRowBufferType
      rowBuffer(1) = new myRowBufferType
      var buffer = 0
      var bufferSize = 0
    }

    var singleGroupHolder = GroupHolder()
    var groupMap = new scala.collection.mutable.HashMap[Int, GroupHolder]
    val useGroup = true
    var gr: GroupHolder = _
    if (!useGroup) groupMap += (-1 -> singleGroupHolder)

    def set_coverage(lSeg: CovSEGinfo, rSeg: CovSEGinfo) {
      if (lSeg.values.charAt(rSeg.buckPos) =='4') lSeg.values.setCharAt(rSeg.buckPos, '0')
    }

    def nested_process(lr: Row, next_lr: Row) {

      leftStart = lr.pos - 1
      leftStop = lr.pos
      if (next_lr != null) next_leftStart = next_lr.pos - 1

      //#####
      // Find overlap with right-rows in the buffer

      val lSeg = CovSEGinfo(leftStart, leftStop, lr, 0, new mutable.StringBuilder(lr.colAsString(valuesCol).toString)) // bucketPos is irrelevant for the left-row

      var groupKeyLeft : Int = -1

      groupKeyLeft = pbt.buckNameToIdx(lr.colAsString(bucketCol).toString)
      groupMap.get(groupKeyLeft) match {
        case Some(x) => gr = x
        case None =>
          gr = GroupHolder()
          groupMap += (groupKeyLeft -> gr)
      }

      val nextBuffer = (gr.buffer + 1) % 2
      var nextBufferSize = 0
      var i = 0
      while (i < gr.bufferSize) {
        val rSeg = gr.rowBuffer(gr.buffer)(i)
        val rr = rSeg.r
        if (lr.chr == rr.chr && lSeg.start < rSeg.stop && lSeg.stop > rSeg.start) {
          set_coverage(lSeg, rSeg)
        }
        if (!(rr.chr == lr.chr && rSeg.stop < lSeg.start) || rr.chr < lr.chr) {
          if (gr.rowBuffer(nextBuffer).size <= nextBufferSize) gr.rowBuffer(nextBuffer) += gr.rowBuffer(gr.buffer)(i)
          else gr.rowBuffer(nextBuffer)(nextBufferSize) = gr.rowBuffer(gr.buffer)(i)
          gr.rowBuffer(gr.buffer)(i) = null
          nextBufferSize += 1
        }
        i += 1
      }

      gr.buffer = nextBuffer
      gr.bufferSize = nextBufferSize

      //##########
      // Check if we need to fetch more segments from the right-source, i.e. have we moved upwards with the left-source
      if (((maxLeftStop < leftStop && lastLeftChr == lr.chr) || lastLeftChr < lr.chr) &&
        (lastRightChr < lr.chr || (lastRightChr == lr.chr && lastRightPos <= leftStop))) {
        if (lr.chr == lastSeekChr && !rightSource.hasNext) {
          /* do nothing */
        } else if (lr.chr > lastRightChr) {
          rightSource.setPosition(lr.chr, (lr.pos - maxSegSize).max(0))
          lastSeekChr = lr.chr
        } else if (lr.chr == lastRightChr && lr.pos - maxSegSize > lastRightPos) {
          rightSource.moveToPosition(lr.chr, lr.pos - maxSegSize)
          lastSeekChr = lr.chr
        }
        var keepOn = true
        while (keepOn && rightSource.hasNext) { // Read segments from the right-source
          val rr = rightSource.next()

          if (MemoryMonitorUtil.memoryMonitorActive && rightSourceMonitorUtil != null) {
            rightSourceMonitorUtil.check("JoinRightSource", rightSourceMonitorUtil.lineNum, rr)
          }

          var rightStart = 0
          var rightStop = 0

          rightStart = rr.pos
          try {
            rightStop = rr.colAsInt(rstop)
          } catch {
            case e: Exception =>
              val exception = new GorDataException(s"Illegal stop position in column #${rstop + 1} in the JOIN right-source : " + rr, e)
              throw exception
          }

          val rSeg = CovSEGinfo(rightStart, rightStop, rr, 0, null)  // The bucketPos value is set later.  No values from right

          var groupKeyRight : Int = -1

          pbt.pnToIdx.get(rr.colAsString(PNcol).toString) match {
            case Some(idx) => {
              rSeg.buckPos = pbt.pnIdxToBuckPos(idx)
              groupKeyRight = pbt.getBucketIdxFromPn(idx)
              groupMap.get(groupKeyRight) match {
                case Some(x) => gr = x
                case None =>
                  gr = GroupHolder()
                  groupMap += (groupKeyRight -> gr)
              }
              if (lr.chr == rr.chr && lSeg.start < rSeg.stop && lSeg.stop > rSeg.start && (!useGroup || groupKeyLeft == groupKeyRight)) {
                set_coverage(lSeg, rSeg)
              }
            }
            case None => //No match. Do nothing.
          }

          lastRightChr = rr.chr
          lastRightPos = rightStart // rr.pos

          if (next_lr != null && ((rr.chr == next_lr.chr && rightStop >= next_leftStart) || rr.chr >= next_lr.chr)) {
            // Only insert row to buffer if overlap with next row
            if (gr.rowBuffer(gr.buffer).size <= gr.bufferSize) gr.rowBuffer(gr.buffer) += rSeg else gr.rowBuffer(gr.buffer)(gr.bufferSize) = rSeg
            gr.bufferSize += 1
          }
          if (rr.chr > lr.chr || (rr.chr == lr.chr && rightStart > leftStop)) keepOn = false // Continue until there is no overlap with the left-seg
        }
      }

      //#######
      super.process(RowObj(lr.colsSlice(0,bucketCol+1)+"\t"+ lSeg.values.toString().replace('4','3')))

      if ((lr.chr == lastLeftChr && maxLeftStop < leftStop) || lr.chr != lastLeftChr) maxLeftStop = leftStop
      lastLeftChr = lr.chr

 /* this should not be needed because the -xl -xr (PN-list) is constant
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
            if (!((rr.chr == lr.chr && rSeg.stop < lSeg.start) || rr.chr < lr.chr)) {
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
      */
    }

    var prev_row: Row = _

    override def process(lr: Row) {
      if (prev_row == null) prev_row = lr
      else {
        nested_process(prev_row, lr); prev_row = lr
      }
    }

    override def finish() {
      try {
        if (prev_row != null) {
          nested_process(prev_row, null)
        }
      } finally {
        rightSource.close()
      }
    }

    override def setup(): Unit = {
      pbt = context.getSession.getCache.getObjectHashMap.get(lookupSignature).asInstanceOf[PnBucketTable]
      if (pbt == null) throw new GorDataException("Non existing bucket info for lookupSignature " + lookupSignature)
    }
  }

  case class CoverageOverlap(rightSource: RowSource, bucketCol : Int, PNCol : Int, maxSegSize: Int, context: GorContext, lookupSignature : String) extends Analysis {
    this | TheSegOverlap(rightSource, bucketCol, PNCol, maxSegSize, context, lookupSignature)
  }

}