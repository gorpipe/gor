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

import gorsat.Commands._
import org.gorpipe.model.genome.files.gor.Row
import org.gorpipe.model.gor.RowObj

object PivotAnalysis {

  case class PivotState(groupCols: Array[Int],
                        pivotCol: Int,
                        pivotMap: scala.collection.Map[String, Int],
                        valueCols: Array[Int],
                        emptyString: String) extends BinState {
    var allColumns: Array[Array[(String, Boolean)]] = _
    var groupMap = Map.empty[String, Array[Array[(String, Boolean)]]]
    val grColsArray: Array[Int] = groupCols.filter(x => x > 1)

    def initialize(binInfo: BinInfo): Unit = {
      groupMap = Map.empty[String, Array[Array[(String, Boolean)]]]
    }

    def process(r: Row) {
      val groupID = if (grColsArray.length > 0) r.selectedColumns(grColsArray) + "\t" else ""

      groupMap.get(groupID) match {
        case Some(x) => allColumns = x
        case None =>
          allColumns = Array.ofDim[(String, Boolean)](pivotMap.size, valueCols.length)
          var i = 0
          while (i < pivotMap.size) {
            var j = 0
            while (j < valueCols.length) {
              allColumns(i)(j) = ("", false)
              j += 1
            }
            i += 1
          }
          groupMap += (groupID -> allColumns)
      }

      pivotMap.get(r.colAsString(pivotCol).toString.trim) match {
        case Some(index) =>
          var j = 0
          while (j < valueCols.length) {
            if (!allColumns(index)(j)._2) allColumns(index)(j) = (r.colAsString(valueCols(j)).toString, true)
            j += 1
          }
        case None => /* do nothing - pivot value not listed */
      }
    }

    def sendToNextProcessor(bi: BinInfo, nextProcessor: Processor) {
      for (key <- groupMap.keys) {
        allColumns = groupMap(key)
        val theOtherCols = new StringBuilder
        theOtherCols.append(key)
        var i = 0
        while (i < pivotMap.size) {
          var j = 0
          while (j < valueCols.length) {
            if (j > 0) {
              theOtherCols.append("\t")
            }
            if (allColumns(i)(j)._2) {
              theOtherCols.append(allColumns(i)(j)._1)
            }
            else {
              theOtherCols.append(emptyString)
            }
            j += 1
          }
          i += 1
          if (i < pivotMap.size) theOtherCols.append("\t")
        }
        val outRow = RowObj(bi.chr, bi.sto, theOtherCols.toString())
        nextProcessor.process(outRow)
      }
    }
  }

  case class PivotFactory(groupCols: Array[Int],
                          pivotCol: Int,
                          pivotMap: Array[String],
                          valueCols: Array[Int],
                          emptyString: String) extends BinFactory {
    var truePivotMap = Map.empty[String, Int]
    for (i <- pivotMap.indices) truePivotMap = truePivotMap + (pivotMap(i) -> i)

    def create: BinState = PivotState(groupCols, pivotCol, truePivotMap, valueCols, emptyString)
  }

  case class PivotAnalysis(groupCols: Array[Int],
                           pivotCol: Int,
                           pivotOrderMap: Array[String],
                           valueCols: Array[Int],
                           emptyString: String, outgoingHeader: RowHeader) extends
    BinAnalysis(RegularRowHandler(1), BinAggregator(PivotFactory(groupCols, pivotCol, pivotOrderMap, valueCols,
      emptyString), 1, 0))
  {
    override def isTypeInformationMaintained: Boolean = outgoingHeader != null

    override def setRowHeader(header: RowHeader): Unit = {
      rowHeader = header
      if (pipeTo != null) {
        pipeTo.setRowHeader(outgoingHeader.propagateTypes(rowHeader))
      }
    }
  }

  case class PivotAnalysisOrdered(groupCols: Array[Int],
                           pivotCol: Int,
                           pivotOrderMap: Array[String],
                           valueCols: Array[Int],
                           emptyString: String, outgoingHeader: RowHeader) extends
    BinAnalysis(GroupingColumnRowHandler(1, groupCols), BinAggregator(PivotFactory(groupCols, pivotCol, pivotOrderMap,
      valueCols, emptyString), 1, 0))
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