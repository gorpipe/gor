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
import org.gorpipe.gor.model.Row
import org.gorpipe.model.gor.RowObj

object VarCountState {

  case class VarCountState(grCols: List[Int]) extends BinState {

    case class StatHolder() {
      var allCount: Long = 0
    }

    val useGroup: Boolean = grCols.nonEmpty
    var groupMap = scala.collection.mutable.HashMap.empty[String, StatHolder]
    var singleStatHolder = StatHolder()
    if (!useGroup) groupMap += ("theOnlyGroup" -> singleStatHolder)
    val grColsArray: Array[Int] = grCols.toArray

    def initStatHolder(sh: StatHolder) {
      sh.allCount = 0
    }

    def initialize(binInfo: BinInfo): Unit = {
      if (useGroup) groupMap = scala.collection.mutable.HashMap.empty[String, StatHolder]
      else initStatHolder(singleStatHolder)
    }

    def process(r: Row) {
      var sh: StatHolder = null
      if (useGroup) {
        val groupID = r.selectedColumns(grColsArray)
        groupMap.get(groupID) match {
          case Some(x) => sh = x
          case None =>
            sh = StatHolder()
            initStatHolder(sh)
            groupMap += (groupID -> sh)
        }
      } else sh = singleStatHolder
      sh.allCount += 1
    }

    def sendToNextProcessor(bi: BinInfo, nextProcessor: Processor) {
      for (key <- groupMap.keys.toList.sorted) {
        var sh: StatHolder = null
        if (useGroup) sh = groupMap(key) else sh = groupMap("theOnlyGroup")
        var line: String = ""
        if (bi.sto - bi.sta > 1) line += bi.chr + "\t" + bi.sta + "\t" + bi.sto
        else line += bi.chr + "\t" + bi.sto
        if (useGroup) line += "\t" + key
        line += "\t" + sh.allCount
        nextProcessor.process(RowObj(line))
      }
    }
  }


  case class VarCountFactory(grCols: List[Int]) extends BinFactory {
    def create: BinState =
      VarCountState(grCols)
  }

  case class VarCountAggregate(grCols: List[Int], maxReadLength: Int) extends
    BinAnalysis(RegularRowHandler(1), BinAggregator(
      VarCountFactory(grCols), maxReadLength + 200, maxReadLength)) {
  }
}
