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
import gorsat.gorsatGorIterator.MapAndListUtilities
import gorsat.gorsatGorIterator.MapAndListUtilities.singleHashMap
import org.gorpipe.gor.GorSession
import org.gorpipe.model.genome.files.gor.Row
import org.gorpipe.model.gor.RowObj
import org.gorpipe.model.gor.iterators.LineIterator

import scala.collection.JavaConverters._

case class MapLookup(session: GorSession,
                     iteratorCommand: String,
                     iterator: LineIterator,
                     fileName: String,
                     columns: Array[Int],
                     negate: Boolean,
                     caseInsensitive: Boolean,
                     outCols: Array[Int],
                     missingVal: String,
                     returnMiss: Boolean,
                     inSet: Boolean,
                     inSetCol: Boolean,
                     cartesian: Boolean,
                     skipEmpty: Boolean) extends Analysis {
  val returnMissing: Boolean = if (returnMiss && !inSet || inSetCol) true else false
  val singleCol: Boolean = if (columns.length == 1) true else false
  var key: String = _
  var mapVal: String = ""
  var colMap: singleHashMap = _
  val colArray: Array[Int] = columns

  override def setup() {
    val useSet = inSet || (cartesian && outCols.length == 1)
    if (iteratorCommand != "") colMap = MapAndListUtilities.getSingleHashMap(iteratorCommand, iterator,
      caseInsensitive, columns.length, outCols, useSet, skipEmpty,session)
    else colMap = MapAndListUtilities.getSingleHashMap(fileName, caseInsensitive, columns.length,
      outCols, useSet, skipEmpty, session)
  }

  override def process(r: Row) {
    if (singleCol) key = r.colAsString(columns.head).toString
    else {
      // key = (r.colAsString(columns(0)) /: columns.tail.map(c => r.colAsString(c))) (_ + "#" + _)
      key = r.selectedColumns(colArray)
    }

    if (cartesian) {
      val allCols = r.getAllCols
      if (outCols.length == 1) {
        colMap.asScala.foreach(y => super.process(RowObj.apply(allCols + "\t" + y._1)))
      } else {
        colMap.asScala.foreach(y => super.process(RowObj.apply(allCols + "\t" + y._1 + "\t" + y._2)))
      }
    } else {
      Option(colMap.get(if (caseInsensitive) key.toUpperCase else key)) match {
        case Some(x) => if (inSet) {
          if (negate) {
            if (inSetCol) {
              r.addSingleColumnToRow("0")
              super.process(r)
            }
          } else {
            if (inSetCol) {
              r.addSingleColumnToRow("1")
              super.process(r)
            } else super.process(r)
          }
        } else {
          val row = r.rowWithAddedColumn(x)
          super.process(row)
        }

        case None => if (returnMissing) super.process(r.rowWithAddedColumn(missingVal))
        else if (negate) super.process(r)
      }
    }
  }
}