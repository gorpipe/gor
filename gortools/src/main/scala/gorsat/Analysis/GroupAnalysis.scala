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
import org.gorpipe.exceptions.GorDataException
import org.gorpipe.gor.model.Row
import org.gorpipe.gor.session.GorSession
import org.gorpipe.model.gor.RowObj

import scala.collection.mutable

object GroupAnalysis {

  // Aggregate the columns per bin in the stream
  case class AggregateState(binSize: Int, useSegment: Boolean, useCount: Boolean, useCdist: Boolean, useMax: Boolean,
                            useMin: Boolean, useMed: Boolean, useDis: Boolean, useSet: Boolean, useLis: Boolean,
                            useAvg: Boolean, useStd: Boolean, useSum: Boolean,
                            acCols: List[Int], icCols: List[Int], fcCols: List[Int], grCols: List[Int], setLen: Int,
                            truncate: Boolean, sepVal: String) extends BinState {

    case class StatHolder(numCols: Int) {
      val sums = new Array[Double](numCols)
      val sq_sums = new Array[Double](numCols)
      val fMax = new Array[Double](numCols)
      val fMin = new Array[Double](numCols)
      val aMax = new Array[String](numCols)
      val aMin = new Array[String](numCols)
      val ns = new Array[Int](numCols)
      val aList = new Array[List[String]](numCols)
      val fList = new Array[List[Double]](numCols)
      val sbuff = new Array[mutable.StringBuilder](numCols)
      sbuff.indices.foreach(i => {
        sbuff(i) = new mutable.StringBuilder(100)
      })
      var gList = List.empty[String]
      var allCount: Long = 0
    }

    def maxLen(s: String, maxLen: Int = 200): String = {
      if (s.length > maxLen) {
        if (truncate) {
          s.substring(0, maxLen.min(s.length)) + "..."
        } else {
          throw new GorDataException("String is too long")
        }
      } else {
        s
      }
    }


    val anyCols: List[(Int, Char)] = (acCols.map((_, 'a')) ::: icCols.map((_, 'i')) ::: fcCols.map((_, 'f')))
      .sortWith((x, y) => x._1 < y._1)
    val numCols: Int = anyCols.size
    val collectLists: Boolean = useDis || useMed || useSet
    val useGroup: Boolean = if (grCols.nonEmpty) true else false

    var groupMap = scala.collection.mutable.HashMap.empty[String, StatHolder]
    val grColsArray: Array[Int] = grCols.toArray

    def formatDouble(d: Double): String = (d formatted "%1.1f").replace(',', '.')

    def initStatHolder(sh: StatHolder) {
      var i = 0
      while (i < anyCols.size) {
        sh.sums(i) = 0.0
        sh.sq_sums(i) = 0.0
        sh.ns(i) = 0
        sh.aList(i) = Nil
        sh.fList(i) = Nil
        sh.sbuff(i).setLength(0)
        i += 1
      }
      sh.allCount = 0
      sh.gList = Nil
    }

    def initialize(binInfo: BinInfo): Unit = {
      groupMap = scala.collection.mutable.HashMap.empty[String, StatHolder]
    }

    def process(r: Row) {
      var sh: StatHolder = null
      val groupID = if (useGroup) r.selectedColumns(grColsArray) else ""

      groupMap.get(groupID) match {
        case Some(x) => sh = x
        case None =>
          sh = StatHolder(numCols)
          initStatHolder(sh)
          groupMap += (groupID -> sh)
      }

      sh.allCount += 1
      if (useCdist) sh.gList ::= r.toString
      var i = 0
      while (i < anyCols.size) {
        val j = anyCols(i)._1
        val cType = anyCols(i)._2
        if (cType == 'f' || cType == 'i') {
          try {
            val v = r.colAsDouble(j)
            if (!v.isNaN) {
              if (collectLists) sh.fList(i) ::= v
              if (useLis) {
                if (sh.ns(i) > 0) sh.sbuff(i).append(sepVal)
                sh.sbuff(i).append(r.colAsString(j))
              }
              sh.sums(i) += v
              sh.sq_sums(i) += v * v
              if (sh.ns(i) == 0) {
                sh.fMin(i) = v
                sh.fMax(i) = v
              } else {
                if (v < sh.fMin(i)) sh.fMin(i) = v
                if (v > sh.fMax(i)) sh.fMax(i) = v
              }
              sh.ns(i) += 1
            }
          } catch {
            case _: Exception => /* do nothing */
          }
        }
        else if (cType == 'a') {
          try {
            val v = r.colAsString(j).toString
            if (collectLists) sh.aList(i) ::= v
            if (useLis) {
              if (sh.ns(i) > 0) sh.sbuff(i).append(sepVal)
              sh.sbuff(i).append(r.colAsString(j))
            }
            if (sh.ns(i) == 0) {
              sh.aMin(i) = v
              sh.aMax(i) = v
            } else {
              if (v < sh.aMin(i)) sh.aMin(i) = v
              if (v > sh.aMax(i)) sh.aMax(i) = v
            }
            sh.ns(i) += 1
          } catch {
            case _: Exception => /* do nothing */
          }
        }
        i += 1
      }
    }

    def sendToNextProcessor(bi: BinInfo, nextProcessor: Processor): Unit = {
      for (key <- groupMap.keys.toList.sorted) {
        val sh = groupMap(key)
        val lineBuilder = new mutable.StringBuilder
        lineBuilder.append(bi.chr)
        lineBuilder.append('\t')
        if (useSegment) {
          lineBuilder.append(bi.sta)
          lineBuilder.append('\t')
          lineBuilder.append(bi.sto)
        }
        else {
          lineBuilder.append(bi.sto)
        }
        if (useGroup) {
          lineBuilder.append('\t')
          lineBuilder.append(key)
        }
        if (useCount) {
          lineBuilder.append('\t')
          lineBuilder.append(sh.allCount)
        }
        if (useCdist) {
          lineBuilder.append('\t')
          lineBuilder.append(sh.gList.distinct.size)
        }
        var i = 0
        while (i < anyCols.size) {
          val cType = anyCols(i)._2
          if (sh.ns(i) > 0) {
            if (cType == 'i' || cType == 'f') {
              val mean = sh.sums(i) / sh.ns(i)
              val variance = (sh.sq_sums(i) / sh.ns(i) - mean * mean).abs
              if (cType == 'i') {
                if (useMin) {
                  lineBuilder.append('\t')
                  lineBuilder.append(sh.fMin(i).toLong)
                }
                if (useMed) {
                  val fArr = sh.fList(i).sorted.toArray
                  lineBuilder.append('\t')
                  lineBuilder.append(fArr(fArr.length / 2).toLong)
                }
                if (useMax) {
                  lineBuilder.append('\t')
                  lineBuilder.append(sh.fMax(i).toLong)
                }
                if (useSet) {
                  val y = sh.fList(i).map(_.toLong).distinct.sorted
                  lineBuilder.append('\t')
                  lineBuilder.append(maxLen(y.mkString(sepVal), setLen))
                }
                if (useLis) {
                  lineBuilder.append('\t')
                  lineBuilder.append(maxLen(sh.sbuff(i).toString, setLen))
                }
              } else {
                if (useMin) {
                  lineBuilder.append('\t')
                  lineBuilder.append(sh.fMin(i))
                }
                if (useMed) {
                  val fArr = sh.fList(i).sorted.toArray
                  var median = 0.0
                  if (fArr.length % 2 == 0) {
                    val idx = fArr.length / 2
                    median = (fArr(idx - 1) + fArr(idx)) / 2.0
                  } else {
                    median = fArr(fArr.length / 2)
                  }
                  lineBuilder.append('\t')
                  lineBuilder.append(median)
                }
                if (useMax) {
                  lineBuilder.append('\t')
                  lineBuilder.append(sh.fMax(i))
                }
                if (useSet) {
                  val y = sh.fList(i).distinct.sorted
                  lineBuilder.append('\t')
                  lineBuilder.append(maxLen(y.mkString(sepVal), setLen))
                }
                if (useLis) {
                  lineBuilder.append('\t')
                  lineBuilder.append(maxLen(sh.sbuff(i).toString, setLen))
                }
              }
              if (useDis) {
                lineBuilder.append('\t')
                lineBuilder.append(sh.fList(i).distinct.length)
              }
              if (useAvg) {
                lineBuilder.append('\t')
                lineBuilder.append(mean)
              }
              if (useStd) {
                lineBuilder.append('\t')
                lineBuilder.append(scala.math.sqrt(variance))
              }
              if (useSum) {
                if (cType == 'i') {
                  lineBuilder.append('\t')
                  lineBuilder.append(sh.sums(i).toLong)
                } else {
                  lineBuilder.append('\t')
                  lineBuilder.append(sh.sums(i))
                }
              }
            } else { // the 'a' case
              if (useMin) {
                lineBuilder.append('\t')
                lineBuilder.append(sh.aMin(i))
              }
              if (useMed) {
                val aArr: Array[String] = sh.aList(i).sorted.toArray
                lineBuilder.append('\t')
                lineBuilder.append(aArr(aArr.length / 2))
              }
              if (useMax) {
                lineBuilder.append('\t')
                lineBuilder.append(sh.aMax(i))
              }
              if (useSet) {
                val sortedDistinctList = sh.aList(i).distinct.sorted
                lineBuilder.append('\t')
                lineBuilder.append(maxLen(sortedDistinctList.mkString(sepVal), setLen))
              }
              if (useLis) {
                lineBuilder.append('\t')
                lineBuilder.append(maxLen(sh.sbuff(i).toString, setLen))
              }
              if (useDis) {
                lineBuilder.append('\t')
                lineBuilder.append(sh.aList(i).distinct.length)
              }
            }
          } else {
            if (useMin) lineBuilder.append('\t')
            if (useMed) lineBuilder.append('\t')
            if (useMax) lineBuilder.append('\t')
            if (useSet) lineBuilder.append('\t')
            if (useLis) lineBuilder.append('\t')
            if (useDis) lineBuilder.append('\t')
            if (cType == 'i' || cType == 'f') {
              if (useAvg) lineBuilder.append('\t')
              if (useStd) lineBuilder.append('\t')
              if (useSum) lineBuilder.append('\t')
            }
          }
          i += 1
        }
        nextProcessor.process(RowObj(lineBuilder.toString))
      }

      groupMap = null
    }
  }

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


  case class AggregateFactory(binSize: Int, useSegment: Boolean, useCount: Boolean, useCdist: Boolean, useMax: Boolean,
                              useMin: Boolean, useMed: Boolean, useDis: Boolean, useSet: Boolean, useLis: Boolean,
                              useAvg: Boolean, useStd: Boolean, useSum: Boolean,
                              acCols: List[Int], icCols: List[Int], fcCols: List[Int], grCols: List[Int],
                              setLen: Int, truncate: Boolean, sepVal: String) extends BinFactory {
    def create: BinState =
      AggregateState(binSize, useSegment, useCount, useCdist, useMax, useMin, useMed, useDis, useSet, useLis, useAvg,
        useStd, useSum, acCols, icCols, fcCols, grCols, setLen, truncate, sepVal)
  }

  case class Aggregate(binSize: Int, useCount: Boolean, useCdist: Boolean, useMax: Boolean, useMin: Boolean,
                       useMed: Boolean, useDis: Boolean, useSet: Boolean, useLis: Boolean, useAvg: Boolean,
                       useStd: Boolean, useSum: Boolean,
                       acCols: List[Int], icCols: List[Int], fcCols: List[Int], grCols: List[Int], setLen: Int,
                       truncate: Boolean, sepVal: String, outgoingHeader: RowHeader) extends
    BinAnalysis(RegularRowHandler(binSize), BinAggregator(
      AggregateFactory(binSize, binSize > 1, useCount, useCdist, useMax, useMin, useMed, useDis, useSet, useLis, useAvg,
        useStd, useSum, acCols, icCols, fcCols, grCols, setLen, truncate, sepVal), 2, 1)) {


    override def isTypeInformationMaintained: Boolean = outgoingHeader != null

    override def setRowHeader(header: RowHeader): Unit = {
      rowHeader = header
      if(pipeTo != null && outgoingHeader != null) {
        pipeTo.setRowHeader(outgoingHeader.propagateTypes(rowHeader))
      }
    }
  }

  case class OrderedAggregate(binSize: Int, useCount: Boolean, useCdist: Boolean, useMax: Boolean, useMin: Boolean,
                       useMed: Boolean, useDis: Boolean, useSet: Boolean, useLis: Boolean, useAvg: Boolean,
                       useStd: Boolean, useSum: Boolean,
                       acCols: List[Int], icCols: List[Int], fcCols: List[Int], grCols: List[Int], setLen: Int,
                       truncate: Boolean, sepVal: String, outgoingHeader: RowHeader) extends
    BinAnalysis(GroupingColumnRowHandler(binSize, grCols.toArray), BinAggregator(
      AggregateFactory(binSize, binSize > 1, useCount, useCdist, useMax, useMin, useMed, useDis, useSet, useLis, useAvg,
        useStd, useSum, acCols, icCols, fcCols, grCols, setLen, truncate, sepVal), 2, 1)) {


    override def isTypeInformationMaintained: Boolean = outgoingHeader != null

    override def setRowHeader(header: RowHeader): Unit = {
      rowHeader = header
      if(pipeTo != null && outgoingHeader != null) {
        pipeTo.setRowHeader(outgoingHeader.propagateTypes(rowHeader))
      }
    }
  }

  case class SlideAggregate(slideSteps: Int, binSize: Int, useCount: Boolean, useCdist: Boolean, useMax: Boolean,
                            useMin: Boolean, useMed: Boolean, useDis: Boolean, useSet: Boolean, useLis: Boolean,
                            useAvg: Boolean, useStd: Boolean, useSum: Boolean,
                            acCols: List[Int], icCols: List[Int], fcCols: List[Int], grCols: List[Int], setLen: Int,
                            truncate: Boolean, sepVal: String, outgoingHeader: RowHeader) extends
    BinAnalysis(SlidingRowHandler(binSize, slideSteps), BinAggregator(
      AggregateFactory(binSize, binSize > 1, useCount, useCdist, useMax, useMin, useMed, useDis, useSet, useLis, useAvg,
        useStd, useSum, acCols, icCols, fcCols, grCols, setLen, truncate, sepVal), 2 * 2 * slideSteps, 2 * 1 * slideSteps)) {

    override def isTypeInformationMaintained: Boolean = outgoingHeader != null

    override def setRowHeader(header: RowHeader): Unit = {
      rowHeader = header
      if(pipeTo != null) {
        pipeTo.setRowHeader(outgoingHeader.propagateTypes(rowHeader))
      }
    }
  }

  case class ChromAggregate(session: GorSession, useCount: Boolean, useCdist: Boolean, useMax: Boolean,
                            useMin: Boolean, useMed: Boolean, useDis: Boolean, useSet: Boolean, useLis: Boolean,
                            useAvg: Boolean, useStd: Boolean, useSum: Boolean,
                            acCols: List[Int], icCols: List[Int], fcCols: List[Int], grCols: List[Int], setLen: Int,
                            truncate: Boolean, sepVal: String, outgoingHeader: RowHeader) extends
    BinAnalysis(ChromRowHandler(session), BinAggregator(
      AggregateFactory(1, true, useCount, useCdist, useMax, useMin, useMed, useDis, useSet, useLis, useAvg, useStd,
        useSum, acCols, icCols, fcCols, grCols, setLen, truncate, sepVal), 2, 1)) {

    override def isTypeInformationMaintained: Boolean = outgoingHeader != null

    override def setRowHeader(header: RowHeader): Unit = {
      rowHeader = header
      if(pipeTo != null) {
        pipeTo.setRowHeader(outgoingHeader.propagateTypes(rowHeader))
      }
    }
  }

  case class GenomeAggregate(useCount: Boolean, useCdist: Boolean, useMax: Boolean, useMin: Boolean,
                             useMed: Boolean, useDis: Boolean, useSet: Boolean, useLis: Boolean, useAvg: Boolean,
                             useStd: Boolean, useSum: Boolean,
                             acCols: List[Int], icCols: List[Int], fcCols: List[Int], grCols: List[Int], setLen: Int,
                             truncate: Boolean, sepVal: String, outgoingHeader: RowHeader) extends
    BinAnalysis(GenomeRowHandler(), BinAggregator(
      AggregateFactory(1, true, useCount, useCdist, useMax, useMin, useMed, useDis, useSet, useLis, useAvg, useStd,
        useSum, acCols, icCols, fcCols, grCols, setLen, truncate, sepVal), 2, 1, useKeyForChrom = true)) {

    override def isTypeInformationMaintained: Boolean = outgoingHeader != null

    override def setRowHeader(header: RowHeader): Unit = {
      rowHeader = header
      if(pipeTo != null) {
        pipeTo.setRowHeader(outgoingHeader.propagateTypes(rowHeader))
      }
    }
  }

}