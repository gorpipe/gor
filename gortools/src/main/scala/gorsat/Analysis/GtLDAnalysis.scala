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

import gorsat.Utilities.AnalysisUtilities.{ParameterHolder, SEGinfo}
import gorsat.Commands._
import gorsat.Iterators.{ChromBoundedIteratorSource, RowListIterator}
import gorsat.process.GenericGorRunner
import org.gorpipe.exceptions.GorDataException
import org.gorpipe.gor.model.Row
import org.gorpipe.model.gor.RowObj
import org.gorpipe.model.gor.RowObj.BinaryHolder
import org.gorpipe.model.gor.iterators.RowSource

object GtLDAnalysis {


  case class LDSegOverlap(ph: ParameterHolder, inRightSource: RowSource, missingSeg: String, leftJoin: Boolean, fuzzFactor: Int, iJoinType: String,
                        lstop: Int, rstop: Int, lleq: List[Int], lreq: List[Int], otherCols : Array[Int], valuesCol : Int, maxSegSize: Int, plain: Boolean, inclusOnly: Boolean = false) extends Analysis {

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
        } else if (lr.chr > lastRightChr) {
          if (snpsnp || segsnp) rightSource.setPosition(lr.chr, (lr.pos - fuzzFactor).max(0))
          else rightSource.setPosition(lr.chr, (lr.pos - fuzzFactor - maxSegSize).max(0))
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
      while (i < gr.bufferSize && !wantsNoMore) {
        val rSeg = gr.rowBuffer(gr.buffer)(i)
        val rr = rSeg.r
        if (lr.chr == rr.chr && lSeg.start - fuzzFactor < rSeg.stop && lSeg.stop + fuzzFactor > rSeg.start) {
          val LDs = LDstatCalc(lr.colAsString(valuesCol).toString,rr.colAsString(valuesCol).toString)
          nextProcessor.process(RowObj(lr.chr+"\t"+lr.pos+"\t"+lr.selectedColumns(otherCols)+"\t"+((rr.pos - lr.pos) - (if (rr.pos - lr.pos > 0) 1 else 0) + "\t"
            + rr.pos+"\t"+rr.selectedColumns(otherCols))+"\t"
            + LDs.x11 + "\t" + LDs.x12 + "\t" + LDs.x21 + "\t" + LDs.x22 ))

        }
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

  case class LDSnpJoinSnpOverlap(ph: ParameterHolder, rightSource: RowSource, missingB: String, leftJoin: Boolean, fuzz: Int,
                               leq: List[Int], req: List[Int], otherCols : Array[Int], valuesCol : Int, plain: Boolean) extends Analysis {
    this | LDSegOverlap(ph, rightSource, missingB, leftJoin, fuzz, "snpsnp", 2, 2, leq, req, otherCols, valuesCol, 1, plain)
  }

  case class LDstats(x11 : Int, x12 : Int, x21 : Int, x22 : Int)

  def LDstatCalc(a : String, b : String) : LDstats = {
    var i = 0
    var x11 = 0; var x12 = 0; var x21 = 0; var x22 = 0
    while (i < a.length) {
      if (a(i) == '0') {
        if (b(i) == '0') {
          x11 += 4
        } else if (b(i)=='1') {
          x11 += 2
          x12 += 2
        } else if (b(i)=='2') {
          x12 += 4
        }
      } else if (a(i)=='1') {
        if (b(i) == '0') {
          x11 += 2
          x21 += 2
        } else if (b(i)=='1') {
          x11 += 1
          x12 += 1
          x21 += 1
          x22 += 1
        } else if (b(i)=='2') {
          x12 += 2
          x22 += 2
        }
      } else if (a(i)=='2') {
        if (b(i) == '0') {
          x21 += 4
        } else if (b(i)=='1') {
          x21 += 2
          x22 += 2
        } else if (b(i)=='2') {
          x22 += 4
        }
      }
      i += 1
    }
    LDstats(x11,x12,x21,x22)
  }


  case class typeHolder(var rowType: Char) extends BinaryHolder

  case class LDSelfJoinFactory(missingSEG: String, fuzz: Int, req: List[Int], otherCols: List[Int], valuesCol : Int, useOnlyAsLeftVar: Int) extends BinFactory {
    def create: BinState = LDSelfJoinState(missingSEG, fuzz, req, otherCols, valuesCol, useOnlyAsLeftVar)
  }

  case class LDSelfJoinRowHandler(binsize: Int, fuzz: Int, binN: Int) extends RowHandler {
    val binIDgen = RegularBinIDgen(binsize)
    val leftType = typeHolder('L') // This object is used to denote left-row when it is received in SelfJoinState
    val rightType = typeHolder('R') // This object is used to denote right-row

    def process(r: Row, BA: BinAggregator) {
      val chr = r.chr
      val pos = r.pos

      var binID = binIDgen.ID(pos)
      val (sta, sto) = binIDgen.StartAndStop(binID)
      r.bH = leftType
      BA.update(r, binID, chr, sta, sto)

      r.bH = rightType
      val start = if (pos > fuzz) pos - fuzz else 0
      val stop = pos + fuzz
      val binIDstart = binIDgen.ID(start)
      val binIDstop = binIDgen.ID(stop)
      if (binIDstop - binIDstart > binN) {
        throw new GorDataException("Window of " + binN + " too small, given the specified binsize")
      } // ##THOUSAND
      binID = binIDstart
      while (binID <= binIDstop) {
        val (sta, sto) = binIDgen.StartAndStop(binID)
        BA.update(r, binID, chr, sta, sto)
        binID += 1
      }
    }
  }


  case class LDSelfJoinState(missingSEG: String, fuzz: Int, lreq: List[Int], iotherCols: List[Int], valuesCol : Int, useOnlyAsLeftVar: Int) extends BinState {
    var lRows: List[Row] = _
    var rRows: List[Row] = _
    val empty = missingSEG.split("\t", -1)(0)
    val req = lreq.toArray
    val otherCols = iotherCols.toArray
    val noEquijoin = if (lreq == Nil) true else false

    def initialize(bi: BinInfo): Unit = {
      lRows = Nil
      rRows = Nil
    }

    def process(r: Row) {
      val leftOrRight = r.bH.asInstanceOf[typeHolder]
      if (leftOrRight.rowType == 'L' && (useOnlyAsLeftVar == -1 || r.colAsInt(useOnlyAsLeftVar) > 0)) lRows ::= r
      if (leftOrRight.rowType == 'R') rRows ::= r
    }

    def sendToNextProcessor(bi: BinInfo, nextProcessor: Processor) {
      if (nextProcessor.wantsNoMore) return
      if (rRows.nonEmpty && lRows.length * rRows.length < 400) {
        for (lr <- lRows.reverse;
             rr <- rRows.reverse) {
          if (nextProcessor.wantsNoMore) return
          if (rr.pos - fuzz - 1 < lr.pos && lr.pos <= rr.pos + fuzz && (noEquijoin || rr.selectedColumns(req) == lr.selectedColumns(req))) {
              val LDs = LDstatCalc(lr.colAsString(valuesCol).toString,rr.colAsString(valuesCol).toString)
              val r = RowObj(lr.chr+"\t"+lr.pos+"\t"+lr.selectedColumns(otherCols)+"\t"+((rr.pos - lr.pos) - (if (rr.pos - lr.pos > 0) 1 else 0) + "\t"
                + rr.pos+"\t"+rr.selectedColumns(otherCols))+"\t"
                + LDs.x11 + "\t" + LDs.x12 + "\t" + LDs.x21 + "\t" + LDs.x22 )
              nextProcessor.process(r)
          }
        }
      } else if (lRows.nonEmpty && rRows.nonEmpty) {
        val itLeft = RowListIterator(lRows.reverse)
        val itRight = RowListIterator(rRows.reverse)
        val runner = new GenericGorRunner()
        val params = ParameterHolder(varsegleft = false, varsegright = false, 2, 2)
        val pipe = LDSnpJoinSnpOverlap(params, itRight, missingSEG, leftJoin = false, fuzz, lreq, lreq, otherCols, valuesCol, plain = false) |
          Forwarder(nextProcessor)
        runner.run(itLeft, pipe)
      }
      lRows = Nil
      rRows = Nil
    }
  }

  case class LDSelfJoinAnalysis(binSize: Int, missingSEG: String, fuzz: Int, req: List[Int], otherCols : List[Int], valuesCol : Int, useOnlyAsLeftVar: Int, binN: Int) extends
    BinAnalysis(LDSelfJoinRowHandler(binSize, fuzz, binN), BinAggregator(LDSelfJoinFactory(missingSEG, fuzz, req, otherCols, valuesCol, useOnlyAsLeftVar), binN + 10, binN)) {
  }

  def fd(d: Double): String = (d formatted "%6.4f").replace(',', '.')

  case class LDcalculation(x11Col : Int) extends Analysis {
    val x12Col = x11Col+1
    val x21Col = x11Col+2
    val x22Col = x11Col+3

    override def process(r: Row): Unit = {
      val x11 = r.colAsInt(x11Col)
      val x12 = r.colAsInt(x12Col)
      val x21 = r.colAsInt(x21Col)
      val x22 = r.colAsInt(x22Col)
      val N = (x11+x12+x21+x22).toDouble
      val p1=(x11+x12)/N
      val q1=(x11+x21)/N
      val D = x11/N-p1*q1
      val d = if (D<0) (p1*q1).max((1.0-p1)*(1.0-q1)) else (p1*(1-q1)).min((1.0-p1)*q1)
      val rd = Math.sqrt(p1*(1.0-p1)*q1*(1.0-q1))
      val Dm = D/d
      val rcoeff = D/rd
      super.process(r.rowWithAddedColumns(""+fd(Dm)+"\t"+fd(rcoeff)))
    }
  }
}
