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
import org.gorpipe.gor.model.{GenomicIterator, Row}
import org.gorpipe.model.gor.RowObj
import org.gorpipe.model.gor.RowObj.BinaryHolder

object GtLDAnalysis {


  case class LDSegOverlap(ph: ParameterHolder, inRightSource: GenomicIterator, missingSeg: String, leftJoin: Boolean, fuzzFactor: Int, iJoinType: String,
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
            val LDs = LDstatCalc(lr.colAsString(valuesCol).toString, rr.colAsString(valuesCol).toString)
            nextProcessor.process(RowObj(lr.chr + "\t" + lr.pos + "\t" + lr.selectedColumns(otherCols) + "\t" + ((rr.pos - lr.pos) /* - (if (rr.pos - lr.pos > 0) 1 else 0) */ + "\t"
              + rr.pos + "\t" + rr.selectedColumns(otherCols)) + "\t"
              + LDs.g00+"\t"+LDs.g10+"\t"+LDs.g20+"\t"+LDs.g01+"\t"+LDs.g11+"\t"+LDs.g21+"\t"+LDs.g02+"\t"+LDs.g12+"\t"+LDs.g22))

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

  case class LDSnpJoinSnpOverlap(ph: ParameterHolder, rightSource: GenomicIterator, missingB: String, leftJoin: Boolean, fuzz: Int,
                               leq: List[Int], req: List[Int], otherCols : Array[Int], valuesCol : Int, plain: Boolean) extends Analysis {
    this | LDSegOverlap(ph, rightSource, missingB, leftJoin, fuzz, "snpsnp", 2, 2, leq, req, otherCols, valuesCol, 1, plain)
  }

  case class LDstats(g00 : Int, g10 : Int, g20 : Int, g01 : Int, g11 :Int, g21 : Int, g02 : Int, g12 : Int, g22 : Int)

  def LDstatCalc(a : String, b : String) : LDstats = {
    var i = 0
    var g00 = 0; var g10 = 0; var g20 = 0;
    var g01 = 0; var g11 = 0; var g21 = 0;
    var g02 = 0; var g12 = 0; var g22 = 0;

    while (i < a.length) {
      if (a(i) == '0') {
        if (b(i) == '0') {
          g00 += 1
        } else if (b(i)=='1') {
          g01 += 1
        } else if (b(i)=='2') {
          g02 += 1
        }
      } else if (a(i)=='1') {
        if (b(i) == '0') {
          g10 += 1
        } else if (b(i)=='1') {
          g11 += 1
        } else if (b(i)=='2') {
          g12 += 1
        }
      } else if (a(i)=='2') {
        if (b(i) == '0') {
          g20 += 1
        } else if (b(i)=='1') {
          g21 += 1
        } else if (b(i)=='2') {
          g22 += 1
        }
      }
      i += 1
    }
    LDstats(g00,g10,g20,g01,g11,g21,g02,g12,g22)
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
              val LDs = LDstatCalc(lr.colAsString(valuesCol).toString, rr.colAsString(valuesCol).toString)
              val r = RowObj(lr.chr + "\t" + lr.pos + "\t" + lr.selectedColumns(otherCols) + "\t" + ((rr.pos - lr.pos) /* - (if (rr.pos - lr.pos > 0) 1 else 0) */ + "\t"
                + rr.pos + "\t" + rr.selectedColumns(otherCols)) + "\t"
                + LDs.g00+"\t"+LDs.g10+"\t"+LDs.g20+"\t"+LDs.g01+"\t"+LDs.g11+"\t"+LDs.g21+"\t"+LDs.g02+"\t"+LDs.g12+"\t"+LDs.g22)
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

  case class LDcalculation(g00Col : Int) extends Analysis {
    val g10Col = g00Col+1
    val g20Col = g00Col+2
    val g01Col = g00Col+3
    val g11Col = g00Col+4
    val g21Col = g00Col+5
    val g02Col = g00Col+6
    val g12Col = g00Col+7
    val g22Col = g00Col+8

    override def process(r: Row): Unit = {
      val g00 = r.colAsDouble(g00Col)
      val g10 = r.colAsDouble(g10Col)
      val g20 = r.colAsDouble(g20Col)
      val g01 = r.colAsDouble(g01Col)
      val g11 = r.colAsDouble(g11Col)
      val g21 = r.colAsDouble(g21Col)
      val g02 = r.colAsDouble(g02Col)
      val g12 = r.colAsDouble(g12Col)
      val g22 = r.colAsDouble(g22Col)

      val N : Double = g00+g10+g20+g01+g11+g21+g02+g12+g22;
      var G00 = g00/N; var G10 = g10/N; var G20 = g20/N;
      var G01 = g01/N; var G11 = g11/N; var G21 = g21/N;
      var G02 = g02/N; var G12 = g12/N; var G22 = g22/N;
      var h00 = G00+(G01+G10)*0.5
      var h10 = (G10+G11+G21)*0.5+G20
      var h01 = (G01+G11+G12)*0.5+G02
      var h11 = G22+(G21+G12)*0.5
      // println("start h00 "+h00+" h01 "+h01+" h10 "+h10+" h11 "+h11+" sum "+(h00+h01+h10+h11))
      val h0000 = G00
      val h1000 = G10
      val h1010 = G20
      val h0001 = G01
      val h1110 = G21
      val h0101 = G02
      val h1101 = G12
      val h1111 = G22
      var it = 0
      var h1001_last = G11+1.0e-5
      var h0011_last = G11+1.0e-5
      var keep_on = true
      while (it <= 20 && keep_on) {
        it += 1
        // E-step
        val t1 = h10*h01
        val t2 = h00*h11
        val h1001 = G11*t1/(t1+t2)
        val h0011 = G11*t2/(t1+t2)
        // println("G11 "+G11+" h1001 "+h1001+" h0011 "+h0011)
        // M-step
        h00 = h0000*2+h1000+h0001+h0011
        h10 = h1000+h1010*2+h1001+h1110
        h01 = h0001+h1001+h0101*2+h1101
        h11 = h1110+h0011+h1101+h1111*2
        val s = 1.0/(h00+h10+h01+h11)
        h00 = h00*s
        h01 = h01*s
        h10 = h10*s
        h11 = h11*s
        // println("it "+it+" h00 "+h00+" h01 "+h01+" h10 "+h10+" h11 "+h11+" sum "+(h00+h01+h10+h11))
        val d1 = h1001_last-h1001
        val d2 = h0011_last-h0011
        // println("error "+(math.sqrt(d1*d1+d2*d2)/(h1001+h1001_last+h0011_last+h0011)))
        if(math.sqrt(d1*d1+d2*d2)/(h1001+h1001_last+h0011_last+h0011) < 0.0001) keep_on = false else keep_on = true
        h1001_last = h1001
        h0011_last = h0011
      }

      val Nh = h00+h01+h10+h11
      val p1 = (h00+h01)/Nh
      val q1 = (h00+h10)/Nh
      val D = h00*h11-h10*h01
      val d = if (D<0.0) (p1*q1).max((1.0-p1)*(1.0-q1)) else (p1*(1-q1)).min((1.0-p1)*q1)
      val rd = Math.sqrt(p1*(1.0-p1)*q1*(1.0-q1))
      val Dm = D/d
      val rcoeff = D/rd

      // println("D "+D+" rd "+rd+" Dm "+Dm+" rcoeff "+rcoeff)

      super.process(r.rowWithAddedColumns(""+fd(D)+"\t"+fd(Dm)+"\t"+fd(rcoeff)))
    }
  }
}
