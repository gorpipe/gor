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

import scala.collection.mutable.ArrayBuffer

object RankAnalysis {

  case class Parameters() {
    var asc = false
    var useZ = false
    var useCount = false
    var useDistr = false
    var useRankOne = false
    var maxRank: Int = Int.MaxValue
  }

  // Aggregate the columns per bin in the stream
  case class RankState(binSize: Int, rankCol: Int, grCols: List[Int], pa: Parameters) extends BinState {

    case class StatHolder() {
      var fList: List[(Int, Double)] = _
    }

    case class RankInfoHolder() {
      var rank: Int = -1
      var cum: Int = 0
      var freq: Int = 0
      var count: Int = 0
      var z: Double = 0.0
      var rOne: Int = -1
    }

    val useGroup: Boolean = if (grCols.nonEmpty) true else false

    var groupMap = Map.empty[String, StatHolder]
    val grColsArray: Array[Int] = grCols.toArray

    var rownum = 0
    var allRows = new ArrayBuffer[(Row, RankInfoHolder)]

    def formatDouble(d: Double): String = (d formatted "%1.4f").replace(',', '.')

    def initStatHolder(sh: StatHolder) {
      sh.fList = Nil
    }

    def initialize(binInfo: BinInfo): Unit = {
      groupMap = Map.empty[String, StatHolder]
      rownum = 0
      allRows = new ArrayBuffer[(Row, RankInfoHolder)]
    }

    def process(r: Row) {
      var sh: StatHolder = null

      val groupID = if (useGroup) r.selectedColumns(grColsArray) else ""

      groupMap.get(groupID) match {
        case Some(x) => sh = x
        case None =>
          sh = StatHolder()
          initStatHolder(sh)
          groupMap += (groupID -> sh)
      }

      val xx = (r, RankInfoHolder())
      allRows += xx
      val theValue = r.colAsDouble(rankCol)
      sh.fList ::= (rownum, theValue)
      rownum += 1
    }

    def sendToNextProcessor(bi: BinInfo, nextProcessor: Processor) {
      for (key <- groupMap.keys.toList) {
        val sh = groupMap(key)

        val oArr = if (pa.asc) {
          sh.fList.toArray.sortWith((x, y) => x._2 < y._2)
        } else {
          sh.fList.toArray.sortWith((x, y) => x._2 > y._2)
        }

        var i = 0
        var maxRank = 1
        val rankHist = new ArrayBuffer[Int]
        while (i < oArr.length) {
          if (i == 0) {
            rankHist += 0
          } else if (i > 0 && oArr(i - 1)._2 != oArr(i)._2) {
            rankHist += 0
            maxRank += 1
          }
          allRows(oArr(i)._1)._2.rank = maxRank
          allRows(oArr(i)._1)._2.count = oArr.length
          rankHist(maxRank - 1) += 1
          i += 1
        }
        if (pa.useDistr) {
          val rankCumHist = new Array[Int](maxRank)
          var sum = 0
          var i = 0
          while (i < maxRank) {
            sum += rankHist(i)
            rankCumHist(i) = sum
            i += 1
          }

          i = 0
          while (i < oArr.length) {
            allRows(oArr(i)._1)._2.cum = rankCumHist(allRows(oArr(i)._1)._2.rank - 1)
            allRows(oArr(i)._1)._2.freq = rankHist(allRows(oArr(i)._1)._2.rank - 1)
            i += 1
          }
        }
        if (pa.useZ) {
          var sq_sum = 0.0
          var r_sum = 0.0
          var allEqual = true
          var i = 0
          while (i < oArr.length) {
            val v = oArr(i)._2
            sq_sum += v * v
            r_sum += v
            if (i > 0) if (oArr(i)._2 != oArr(i - 1)._2) allEqual = false
            i += 1
          }
          val mean = r_sum / oArr.length
          val variance = if (oArr.length == 1) 1.0 else (sq_sum - oArr.length * mean * mean) / (oArr.length - 1)
          val stdev = scala.math.sqrt(variance)
          i = 0
          while (i < oArr.length) {
            if (allEqual) allRows(oArr(i)._1)._2.z = 0.0 else allRows(oArr(i)._1)._2.z = (oArr(i)._2 - mean) / stdev
            i += 1
          }
        }
        if (pa.useRankOne) {
          i = 0
          while (i < oArr.length) {
            allRows(oArr(i)._1)._2.rOne = oArr(0)._1
            i += 1
          }
        }
      } // groups

      var i = 0
      while (i < allRows.size) {
        val x = allRows(i)
        if (x._2.rank <= pa.maxRank) {
          val sBuilder = new StringBuilder
          sBuilder.append(x._2.rank)
          if (pa.useDistr) {
            sBuilder.append('\t')
            sBuilder.append(formatDouble((x._2.cum + 0.0) / x._2.count))
            sBuilder.append('\t')
            sBuilder.append(formatDouble((x._2.freq + 0.0) / x._2.count))
          }
          if (pa.useZ) {
            sBuilder.append('\t')
            sBuilder.append(formatDouble(x._2.z))
          }
          if (pa.useCount) {
            sBuilder.append('\t')
            sBuilder.append(x._2.count)
          }
          if (pa.useRankOne) {
            sBuilder.append('\t')
            sBuilder.append(allRows(x._2.rOne)._1.colAsString(rankCol))
          }

          nextProcessor.process(x._1.rowWithAddedColumn(sBuilder.toString))
        }
        allRows(i) = null
        i += 1
      }

      allRows = null

    }

    groupMap = null
  }


  case class RankFactory(binSize: Int, rc: Int, grCols: List[Int], pa: Parameters) extends BinFactory {
    def create: BinState =
      RankState(binSize, rc, grCols, pa)
  }

  case class Rank(binSize: Int, rc: Int, grCols: List[Int], pa: Parameters, outgoingHeader: RowHeader) extends
    BinAnalysis(RegularRowHandler(binSize), BinAggregator(
      RankFactory(binSize, rc, grCols, pa), 2, 1))
  {
    override def isTypeInformationMaintained: Boolean = outgoingHeader != null

    override def setRowHeader(header: RowHeader): Unit = {
      rowHeader = header
      if (pipeTo != null) {
        pipeTo.setRowHeader(outgoingHeader.propagateTypes(rowHeader))
      }
    }
  }

  case class ChromRank(session: GorSession, rc: Int, grCols: List[Int], pa: Parameters,
                       outgoingHeader: RowHeader) extends
    BinAnalysis(ChromRowHandler(session), BinAggregator(
      RankFactory(1, rc, grCols, pa), 2, 1))
  {
    override def isTypeInformationMaintained: Boolean = outgoingHeader != null

    override def setRowHeader(header: RowHeader): Unit = {
      rowHeader = header
      if (pipeTo != null) {
        pipeTo.setRowHeader(outgoingHeader.propagateTypes(rowHeader))
      }
    }
  }

  case class GenomeRank(rc: Int, grCols: List[Int], pa: Parameters, outgoingHeader: RowHeader) extends
    BinAnalysis(GenomeRowHandler(), BinAggregator(
      RankFactory(1, rc, grCols, pa), 2, 1))
  {
    override def isTypeInformationMaintained: Boolean = outgoingHeader != null

    override def setRowHeader(header: RowHeader): Unit = {
      rowHeader = header
      if (pipeTo != null) {
        pipeTo.setRowHeader(outgoingHeader.propagateTypes(rowHeader))
      }
    }
  }

}
