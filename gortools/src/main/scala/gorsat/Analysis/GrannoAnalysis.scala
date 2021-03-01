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
import org.gorpipe.gor.model.Row
import org.gorpipe.gor.session.GorSession

object GrannoAnalysis {

  case class GenomeRowHandler() extends RowHandler {
    val binIDgen = RegularBinIDgen(1)

    def process(r: Row, BA: BinAggregator) {
      val binID = binIDgen.ID(1)
      binIDgen.StartAndStop(binID)
      BA.update(r, binID, "chrA", 0, 1000000000)
    } // One ouput row per input row
  }

  case class ChromRowHandler(session: GorSession) extends RowHandler {
    val binIDgen = RegularBinIDgen(1)

    def process(r: Row, BA: BinAggregator) {
      val chr = r.chr
      val binID = binIDgen.ID(1)
      try {
        val (sta, sto) = (0, session.getProjectContext.getReferenceBuild.getBuildSize.get(r.chr))
        BA.update(r, binID, chr, sta, sto)
      } catch {
        case _: Exception =>
          val (sta, sto) = (0, 1000000000)
          BA.update(r, binID, chr, sta, sto)
      }
    } // One ouput row per input row
  }


  case class AggregateFactory(binSize: Int, useCount: Boolean, useCdist: Boolean, useMax: Boolean, useMin: Boolean,
                              useMed: Boolean, useDis: Boolean, useSet: Boolean, useLis: Boolean, useAvg: Boolean,
                              useStd: Boolean, useSum: Boolean,
                              acCols: List[Int], icCols: List[Int], fcCols: List[Int], grCols: List[Int],
                              setLen: Int, sepVal: String) extends BinFactory {
    def create: BinState =
      AggregateState(binSize, useCount, useCdist, useMax, useMin, useMed, useDis, useSet, useLis, useAvg, useStd,
        useSum, acCols, icCols, fcCols, grCols, setLen, sepVal)
  }

  case class Aggregate(binSize: Int, useCount: Boolean, useCdist: Boolean, useMax: Boolean, useMin: Boolean,
                       useMed: Boolean, useDis: Boolean, useSet: Boolean, useLis: Boolean, useAvg: Boolean,
                       useStd: Boolean, useSum: Boolean,
                       acCols: List[Int], icCols: List[Int], fcCols: List[Int], grCols: List[Int], setLen: Int,
                       sepVal: String, outgoingHeader: RowHeader) extends
    BinAnalysis(RegularRowHandler(binSize), BinAggregator(
      AggregateFactory(binSize, useCount, useCdist, useMax, useMin, useMed, useDis, useSet, useLis, useAvg, useStd,
        useSum, acCols, icCols, fcCols, grCols, setLen, sepVal), 2, 1))
  {
    override def isTypeInformationMaintained: Boolean = true

    override def setRowHeader(header: RowHeader): Unit = {
      rowHeader = header
      if (pipeTo != null) {
        pipeTo.setRowHeader(outgoingHeader.propagateTypes(rowHeader))
      }
    }
  }

  case class OrderedAggregate(binSize: Int, useCount: Boolean, useCdist: Boolean, useMax: Boolean, useMin: Boolean,
                       useMed: Boolean, useDis: Boolean, useSet: Boolean, useLis: Boolean, useAvg: Boolean,
                       useStd: Boolean, useSum: Boolean,
                       acCols: List[Int], icCols: List[Int], fcCols: List[Int], grCols: List[Int], setLen: Int,
                       sepVal: String, outgoingHeader: RowHeader) extends
    BinAnalysis(GroupingColumnRowHandler(binSize, grCols.toArray), BinAggregator(
      AggregateFactory(binSize, useCount, useCdist, useMax, useMin, useMed, useDis, useSet, useLis, useAvg, useStd,
        useSum, acCols, icCols, fcCols, grCols, setLen, sepVal), 2, 1))
  {
    override def isTypeInformationMaintained: Boolean = true

    override def setRowHeader(header: RowHeader): Unit = {
      rowHeader = header
      if (pipeTo != null) {
        pipeTo.setRowHeader(outgoingHeader.propagateTypes(rowHeader))
      }
    }
  }

  case class ChromAggregate(session: GorSession, useCount: Boolean, useCdist: Boolean, useMax: Boolean,
                            useMin: Boolean,
                            useMed: Boolean, useDis: Boolean, useSet: Boolean, useLis: Boolean, useAvg: Boolean,
                            useStd: Boolean, useSum: Boolean,
                            acCols: List[Int], icCols: List[Int], fcCols: List[Int], grCols: List[Int], setLen: Int,
                            sepVal: String, outgoingHeader: RowHeader) extends
    BinAnalysis(ChromRowHandler(session), BinAggregator(
      AggregateFactory(1, useCount, useCdist, useMax, useMin, useMed, useDis, useSet, useLis, useAvg, useStd, useSum,
        acCols, icCols, fcCols, grCols, setLen, sepVal), 2, 1))
  {
    override def isTypeInformationMaintained: Boolean = true

    override def setRowHeader(header: RowHeader): Unit = {
      rowHeader = header
      if (pipeTo != null) {
        pipeTo.setRowHeader(outgoingHeader.propagateTypes(rowHeader))
      }
    }
  }

  case class GenomeAggregate(useCount: Boolean, useCdist: Boolean, useMax: Boolean, useMin: Boolean,
                             useMed: Boolean, useDis: Boolean, useSet: Boolean, useLis: Boolean, useAvg: Boolean,
                             useStd: Boolean, useSum: Boolean,
                             acCols: List[Int], icCols: List[Int], fcCols: List[Int], grCols: List[Int], setLen: Int,
                             sepVal: String, outgoingHeader: RowHeader) extends
    BinAnalysis(GenomeRowHandler(), BinAggregator(
      AggregateFactory(1, useCount, useCdist, useMax, useMin, useMed, useDis, useSet, useLis, useAvg, useStd, useSum,
        acCols, icCols, fcCols, grCols, setLen, sepVal), 2, 1))
  {
    override def isTypeInformationMaintained: Boolean = true

    override def setRowHeader(header: RowHeader): Unit = {
      rowHeader = header
      if (pipeTo != null) {
        pipeTo.setRowHeader(outgoingHeader.propagateTypes(rowHeader))
      }
    }
  }

}