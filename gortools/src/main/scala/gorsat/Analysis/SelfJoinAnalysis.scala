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

import gorsat.Utilities.AnalysisUtilities.ParameterHolder
import gorsat.Commands._
import gorsat.Iterators.RowListIterator
import gorsat.process.GenericGorRunner
import org.gorpipe.exceptions.GorDataException
import org.gorpipe.gor.model.Row
import org.gorpipe.model.gor.BinaryHolder

import scala.collection.mutable.ListBuffer

object SelfJoinAnalysis {

  case class typeHolder(var rowType: Char) extends BinaryHolder

  case class SelfJoinFactory(missingSEG: String, fuzz: Int, req: List[Int]) extends BinFactory {
    def create: BinState = SelfJoinState(missingSEG, fuzz, req)
  }

  case class SelfJoinRowHandler(binsize: Int, fuzz: Int, binN: Int) extends RowHandler {
    val binIDgen = RegularBinIDgen(binsize)
    val leftType = typeHolder('L') // This object is used to denote left-row when it is received in SelfJoinState
    val rightType = typeHolder('R') // This object is used to denote right-row

    def process(r: Row, BA: BinAggregator): Unit = {
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

  case class SelfJoinState(missingSEG: String, fuzz: Int, lreq: List[Int]) extends BinState {
    var lRows: List[Row] = _
    var rRows: List[Row] = _
    val empty: String = missingSEG.split("\t", -1)(0)
    val req: Array[Int] = lreq.toArray

    def initialize(bi: BinInfo): Unit = {
      lRows = Nil
      rRows = Nil
    }

    def process(r: Row): Unit = {
      val leftOrRight = r.bH.asInstanceOf[typeHolder]
      if (leftOrRight.rowType == 'L') lRows ::= r else rRows ::= r
    }

    def sendToNextProcessor(bi: BinInfo, nextProcessor: Processor): Unit = {
      if (rRows.nonEmpty && lRows.length * rRows.length < 400) {
        for (lr <- lRows.reverse) {
          var overlaps = 0
          for (rr <- rRows.reverse) if (rr.pos - fuzz - 1 < lr.pos && lr.pos <= rr.pos + fuzz && (lreq == Nil || rr.selectedColumns(req) == lr.selectedColumns(req))) {
            nextProcessor.process(lr.rowWithAddedColumn((rr.pos - lr.pos) - (if (rr.pos - lr.pos > 0) 1 else 0) + "\t" + rr.pos + "\t" + rr.otherCols))
            overlaps += 1
          }
          if (overlaps == 0 && missingSEG != "") nextProcessor.process(lr.rowWithAddedColumn(empty + "\t" + missingSEG))
        }
      } else if (lRows.nonEmpty && rRows.nonEmpty) {
        val itLeft = RowListIterator(lRows.reverse)
        val itRight = RowListIterator(rRows.reverse)
        val outList = new ListBuffer[Row]
        val runner = new GenericGorRunner
        runner.run(itLeft, SnpJoinSnpOverlap(ParameterHolder(varsegleft = false, varsegright = false, 2, 2),
          itRight, missingSEG, leftJoin = false, fuzz, lreq, lreq, plain = false) | gorsat.Outputs.ToList(outList))
        outList.foreach(r => nextProcessor.process(r))
      }
      lRows = Nil
      rRows = Nil
    }
  }

  case class SelfJoinAnalysis(binSize: Int, missingSEG: String, fuzz: Int, req: List[Int], binN: Int) extends
    BinAnalysis(SelfJoinRowHandler(binSize, fuzz, binN), BinAggregator(SelfJoinFactory(missingSEG, fuzz, req), binN + 10, binN)) {
  }
}