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

import java.io._
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

import gorsat.Commands.Analysis
import gorsat.process.StatisticalAdjustment
import org.apache.commons.io.FileUtils
import org.gorpipe.gor.model.Row
import org.gorpipe.model.gor.RowObj

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.io.Source

case class AdjustAnalysis(adOpt: AdjustOptions, pCol: Int, grCols: List[Int]) extends Analysis {
  case class StatHolder(file: File) {
    val pValues = new ArrayBuffer[Double]()
    var bhValues: Array[Double] =_
    var byValues: Array[Double] =_
    var sdValues: Array[Double] =_
    var holmValues: Array[Double] =_
    var rank: Array[Int] =_
    var invSqrtLambda: Double =_
    var count = 0
    var rowIdx = 0
    val rowOS = new GZIPOutputStream(new FileOutputStream(file), 32 * 1024)
  }

  val useGroup = grCols.nonEmpty

  lazy val groupMap = scala.collection.mutable.LinkedHashMap.empty[String, StatHolder]
  lazy val theHolder = getNewStatHolder()

  lazy val rowMapper = RowMapper(adOpt)

  lazy val grColsArray: Array[Int] = grCols.toArray

  override def process(r: Row) {
    val sh = getStatHolder(r)
    val columnValue = r.colAsDouble(pCol)
    val adjustedColumnValue = if (columnValue == 0) {
      Double.MinPositiveValue*10
    } else {
      columnValue
    }
    sh.pValues += adjustedColumnValue
    r.writeRowToStream(sh.rowOS)
    sh.rowOS.write('\n')
    sh.count += 1
  }

  override def finish(): Unit = {
    if (useGroup) flushHolders(groupMap)
    else flushSingleHolder(theHolder)
  }

  var numberOfStatHolders = 0

  private def getNewStatHolder(): StatHolder = {
    val file = File.createTempFile("adjustAnalysisTmpFile", numberOfStatHolders + ".tmp.gz")
    numberOfStatHolders += 1
    StatHolder(file)
  }

  private def flushSingleHolder(sh: StatHolder): Unit = {
    sh.rowOS.close()
    val outRows = adjust(sh)
    outRows.foreach(r => super.process(r))
  }

  def flushHolders(groupMap: mutable.LinkedHashMap[String, StatHolder]): Unit = {
    val statHolders = groupMap.toArray.map(_._2)
    statHolders.foreach(_.rowOS.close())
    val adjustedRowBuffers = statHolders.map(adjust)
    val queue = mutable.PriorityQueue.empty[(Row, Int)](implicitly[Ordering[(Row, Int)]].reverse)

    initializeQueue(adjustedRowBuffers, queue)

    while (queue.nonEmpty) {
      val (rowToProcess, bufferIdx) = queue.dequeue()
      super.process(rowToProcess)
      val it = adjustedRowBuffers(bufferIdx)
      if (it.hasNext) {
        queue += ((it.next(), bufferIdx))
      }
    }

    deleteFiles(statHolders)
  }

  private def deleteFiles(statHolders: Array[StatHolder]) = {
    statHolders.foreach(sh => {
      try {
        FileUtils.forceDelete(sh.file)
      } catch {
        case _: IOException =>
          System.gc()
          FileUtils.forceDelete(sh.file)
      }
    })
  }

  private def initializeQueue(rowIterators: Array[Iterator[Row]], queue: mutable.PriorityQueue[(Row, Int)]) = {
    rowIterators.zipWithIndex.filter(_._1.hasNext).foreach({
      case (it, idx) => queue += ((it.next(), idx))
    })
  }

  private def getStatHolder(r: Row): StatHolder = {
    if (useGroup) {
      val groupID = r.selectedColumns(grColsArray)
      groupMap.get(groupID) match {
        case Some(currSh) => currSh
        case None =>
          val newSh = getNewStatHolder()
          groupMap += (groupID -> newSh)
          newSh
      }
    } else theHolder
  }

  case class RowMapper(adjustOptions: AdjustOptions) {
    val functions: List[(Int, StatHolder) => Double] = {
      var flb: List[(Int, StatHolder) => Double] = Nil
      if (adjustOptions.gcc) flb ::= {
        (idx, sh) => StatisticalAdjustment.genomic_control_correct_p(sh.pValues(idx), sh.invSqrtLambda)
      }
      if (adjustOptions.qq) flb ::= {
        (idx, sh) => (sh.rank(idx) + 0.5) / sh.count
      }
      if (adjustOptions.bonf) flb ::= {
        (idx, sh) => StatisticalAdjustment.bonferroni(sh.pValues(idx), sh.count)
      }
      if (adjustOptions.holm) flb ::= {
        (idx, sh) => sh.holmValues(idx)
      }
      if (adjustOptions.ss) flb ::= {
        (idx, sh) => StatisticalAdjustment.sidak_ss(sh.pValues(idx), sh.count)
      }
      if (adjustOptions.sd) flb ::= {
        (idx, sh) => sh.sdValues(idx)
      }
      if (adjustOptions.bh) flb ::= {
        (idx, sh) => sh.bhValues(idx)
      }
      if (adjustOptions.by) flb ::= {
        (idx, sh) => sh.byValues(idx)
      }
      flb.reverse
    }

    def map(r: Row, rowIdx: Int, sh: StatHolder): Row = {
      var colIdx = r.numCols() - 2
      r.addColumns(functions.length)
      functions.foreach(func => {
        r.setColumn(colIdx, "%.5g".format(func(rowIdx, sh)))
        colIdx += 1
      })
      r
    }
  }

  private def adjust(sh: StatHolder): Iterator[Row] = {
    lazy val sortedPValues = sh.pValues.toArray.sorted
    lazy val pInd = Array.tabulate(sh.count)(i => i).sortBy(i => sh.pValues(i))
    lazy val adjustedPValues = new Array[Double](sh.count)

    if (adOpt.gcc) {
      sh.invSqrtLambda = StatisticalAdjustment.getInvSqrtLambda_p(sortedPValues)
    }
    if (adOpt.holm) {
      sh.holmValues = new Array[Double](sh.count)
      StatisticalAdjustment.holm_bonferroni(sortedPValues, adjustedPValues)
      copyInSpecificOrder(adjustedPValues, pInd, sh.holmValues)
    }
    if (adOpt.sd) {
      sh.sdValues = new Array[Double](sh.count)
      StatisticalAdjustment.sidak_sd(sortedPValues, adjustedPValues)
      copyInSpecificOrder(adjustedPValues, pInd, sh.sdValues)
    }
    if (adOpt.bh) {
      sh.bhValues = new Array[Double](sh.count)
      StatisticalAdjustment.benjamini_hochberg(sortedPValues, adjustedPValues)
      copyInSpecificOrder(adjustedPValues, pInd, sh.bhValues)
    }
    if (adOpt.by) {
      sh.byValues = new Array[Double](sh.count)
      StatisticalAdjustment.benjamini_yekutieli(sortedPValues, adjustedPValues)
      copyInSpecificOrder(adjustedPValues, pInd, sh.byValues)
    }
    if (adOpt.qq) {
      StatisticalAdjustment.invert(pInd)
      sh.rank = pInd
    }
    Source.fromInputStream(new GZIPInputStream(new FileInputStream(sh.file), 32 * 1024))
      .getLines().map(s => RowObj.apply(s)).zipWithIndex.map({ case (r, rowIdx) => rowMapper.map(r, rowIdx, sh) })
  }

  private def copyInSpecificOrder[T](toCopy: Array[T], order: Array[Int], copy: Array[T]): Unit = {
    val len = toCopy.length
    var idx = 0
    while (idx < len) {
      copy(order(idx)) = toCopy(idx)
      idx += 1
    }
  }
}

case class AdjustOptions(gcc: Boolean,
                         qq: Boolean,
                         bh: Boolean,
                         by: Boolean,
                         ss: Boolean,
                         sd: Boolean,
                         holm: Boolean,
                         bonf: Boolean)
