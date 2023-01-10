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

import gorsat.Commands.Analysis
import org.gorpipe.gor.GorConstants
import org.gorpipe.gor.model.Row
import org.gorpipe.model.gor.RowObj

import scala.collection.mutable

/**
  * This class is used in the implementation of the SegProj command. It expects columns as
  * output by the Chopper analysis step.
  * @param grCols
  * @param sumColumns
  * @param header
  */
case class VennSeg(grCols: List[Int], sumColumns: List[Int], header: String) extends Analysis {

  case class GroupHolder() {
    var oldCounter = 0
    var oldChr: String = GorConstants.FIRST_POSSIBLE_CHROMOSOME_VALUE
    var oldPos: Int = -1
    var overlapCounter = 0
  }

  // Note that this is cheating a bit - this step removes a column but it was added
  // in Chopper - these steps work together to implement in SeqProj so they simply pass
  // the header with types along.
  override def isTypeInformationMaintained: Boolean = true

  val grColsArray: Array[Int] = grCols.toArray
  val useGroup: Boolean = if (grColsArray.length > 0) true else false
  val sumColumnsArray: Array[Int] = sumColumns.toArray
  var groupMap = mutable.Map.empty[String, GroupHolder]
  val singleGroupHolder = GroupHolder()
  if (!useGroup) groupMap += ("theOnlyGroup" -> singleGroupHolder)
  var sh: GroupHolder = _
  var groupID = ""


  override def process(r: Row): Unit = {
    val stasto = r.colAsString(2)

    if (useGroup) {
      groupID = r.selectedColumns(grColsArray)
      groupMap.get(groupID) match {
        case Some(x) => sh = x
        case None =>
          sh = GroupHolder()
          groupMap += (groupID -> sh)
      }
    } else sh = singleGroupHolder


    if (!(r.chr == sh.oldChr && r.pos == sh.oldPos)) {
      // write last overlap segment
      if (sh.overlapCounter != 0) {
        if (useGroup) super.process(RowObj(sh.oldChr, sh.oldPos, s"${r.pos}\t$groupID\t${sh.overlapCounter}"))
        else super.process(RowObj(sh.oldChr, sh.oldPos, s"${r.pos}\t${sh.overlapCounter}"))
      }
    }

    var incrementValue = 1

    if (sumColumnsArray.length > 0) {
      incrementValue = r.colAsInt(sumColumnsArray(0))
    }

    sh.oldChr = r.chr
    sh.oldPos = r.pos
    if (stasto == "sta") {
      sh.overlapCounter += incrementValue
    } else {
      sh.overlapCounter -= incrementValue
    }
    if (useGroup && sh.overlapCounter == 0) groupMap.remove(groupID)
  }
}
