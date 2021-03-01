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

import gorsat.Commands.{Analysis, RowHeader}
import org.gorpipe.gor.GorConstants
import org.gorpipe.gor.model.Row
import org.gorpipe.model.gor.RowObj

import scala.collection.mutable.Map

case class ProjectSegments(grCols: List[Int], maxSegSize: Int, outgoingHeader: RowHeader) extends Analysis {
  private var rangeChr = GorConstants.FIRST_POSSIBLE_CHROMOSOME_VALUE
  var rangeStopPos = -1
  var rangeStartPos = -1
  var segCount = 0
  var therchr = ""
  var therpos = -1
  var stopPos = -1

  val grColsArray = grCols.toArray
  val useGroup = if (grColsArray.length > 0) true else false

  override def isTypeInformationMaintained: Boolean = true

  override def setRowHeader(header: RowHeader): Unit = {
    rowHeader = header
    if (pipeTo != null) {
      pipeTo.setRowHeader(outgoingHeader.propagateTypes(rowHeader))
    }
  }

  case class GroupHolder() {
    var rangeChr: String = GorConstants.FIRST_POSSIBLE_CHROMOSOME_VALUE
    var rangeStopPos = -1
    var rangeStartPos = -1
    var segCount = 0
    var grID = ""
  }

  var groupMap = Map.empty[String, GroupHolder]

  def chopSegments(rchr: String, rpos: Int) {
    var allRows: List[Row] = Nil
    groupMap.keys.foreach(key => {
      val gr = groupMap(key)
      var somethingChopped = true
      while (somethingChopped) {
        somethingChopped = false
        if (gr.rangeStopPos - 1 - gr.rangeStartPos > maxSegSize && (gr.rangeStartPos + maxSegSize < rpos || rchr != gr.rangeChr)) {
          allRows ::= RowObj(gr.rangeChr, gr.rangeStartPos, (gr.rangeStartPos + maxSegSize).toString + "\t" + gr.segCount + "\t" + gr.grID)
          gr.rangeStartPos += maxSegSize
          somethingChopped = true
        }
      }
      if (gr.rangeStartPos >= gr.rangeStopPos - 1) groupMap.remove(key)
      else if (!(rchr == gr.rangeChr && rpos < gr.rangeStopPos)) {
        /*
        while (gr.rangeStartPos + maxSegSize < gr.rangeStopPos) {
          allRows ::= Row(gr.rangeChr, gr.rangeStartPos, (gr.rangeStartPos + maxSegSize).toString + "\txxx" + gr.segCount + "\t" + gr.grID)
          gr.rangeStartPos += maxSegSize
        }
        */
        allRows ::= RowObj(gr.rangeChr, gr.rangeStartPos, (gr.rangeStopPos - 1).toString + "\t" + gr.segCount + "\t" + gr.grID)
        groupMap.remove(key)
      }
    })
    allRows.sortWith((x, y) => x.compareTo(y) < 0).foreach(super.process(_))
    allRows = Nil
  }


  override def process(r: Row) {

    therchr = r.chr
    therpos = r.pos
    stopPos = r.colAsInt(2) + 1 // Causes adjacent segments to be merged

    if (useGroup) {
      val groupID = r.selectedColumns(grColsArray)
      var groupFound = false
      chopSegments(therchr, therpos)
      groupMap.get(groupID) match {
        case Some(gr) => {
          if (r.chr == gr.rangeChr && r.pos < gr.rangeStopPos) {
            // extending
            gr.rangeStopPos = gr.rangeStopPos.max(stopPos)
            gr.segCount += 1
          } else {
            // See if we need to output existing range else keep and update the span
            //super.process(Row(gr.rangeChr, gr.rangeStartPos, (gr.rangeStopPos - 1).toString + "\t" + gr.segCount + "\t" + gr.grID))
            System.err.println("we should not get here")
            gr.rangeChr = r.chr
            gr.rangeStartPos = r.pos
            gr.rangeStopPos = stopPos
            gr.segCount = 1
          }
        }
        case None => {
          val gr = GroupHolder()
          gr.rangeChr = r.chr
          gr.rangeStartPos = r.pos
          gr.rangeStopPos = stopPos
          gr.segCount = 1
          gr.grID = groupID
          groupMap += (groupID -> gr)
        }
      }
    } else { // no grouping
      if (r.chr == rangeChr && r.pos < rangeStopPos) {
        // extending
        rangeStopPos = rangeStopPos.max(stopPos)
        segCount += 1
      } else {
        // See if we need to output existing range
        if (rangeChr != GorConstants.FIRST_POSSIBLE_CHROMOSOME_VALUE) super.process(RowObj(rangeChr, rangeStartPos, (rangeStopPos - 1).toString + "\t" + segCount))
        rangeChr = r.chr
        rangeStartPos = r.pos
        rangeStopPos = stopPos
        segCount = 1
      }
    }
  }

  override def finish {
    if (useGroup) {
      chopSegments(therchr, stopPos)
      // See if we need to output existing range
      groupMap.keys.foreach(key => {
        val gr = groupMap(key)
        super.process(RowObj(gr.rangeChr, gr.rangeStartPos, (gr.rangeStopPos - 1).toString + "\t" + gr.segCount + "\t" + gr.grID))
      })
    } else { // no grouping
      // See if we need to output existing range
      if (rangeChr != GorConstants.FIRST_POSSIBLE_CHROMOSOME_VALUE) super.process(RowObj(rangeChr, rangeStartPos, (rangeStopPos - 1).toString + "\t" + segCount))
    }
  }
}
