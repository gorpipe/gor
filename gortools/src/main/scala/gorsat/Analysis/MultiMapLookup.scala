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
import gorsat.gorsatGorIterator.MapAndListUtilities.multiHashMap
import org.gorpipe.gor.model.Row
import org.gorpipe.gor.session.GorSession
import org.gorpipe.model.gor.RowObj
import org.gorpipe.model.gor.iterators.LineIterator

import scala.collection.JavaConverters._

case class MultiMapLookup(session: GorSession, iteratorCommand: String, iterator: LineIterator, fileName: String, columns: Array[Int], caseInsensitive: Boolean, outCols: Array[Int], missingVal: String, returnMiss: Boolean, cartesian: Boolean) extends Analysis {
  val returnMissing: Boolean = if (returnMiss) true else false
  val singleCol: Boolean = if (columns.length == 1) true else false
  var key: String = _
  var mapVal: String = ""
  var colMap: multiHashMap = _
  val colArray: Array[Int] = columns

  override def setup() {
    if (iteratorCommand != "") colMap = MapAndListUtilities.getMultiHashMap(iteratorCommand, iterator,
      caseInsensitive, columns.length, outCols, session)
    else colMap = MapAndListUtilities.getMultiHashMap(fileName, caseInsensitive, columns.length, outCols,
      session)
  }

  override def process(r: Row) {

    if (singleCol) key = r.colAsString(columns.head).toString
    else key = r.selectedColumns(colArray)

    val allCols = r.getAllCols
    if (cartesian) {
      colMap.asScala.foreach(x => x._2.filter(y => !y.startsWith("#")).foreach(z => {
        super.process(RowObj.apply(allCols + "\t" + z))
      }))
    } else {
      Option(colMap.get(if (caseInsensitive) key.toUpperCase else key)) match {
        case Some(x) => x.foreach(y => super.process(RowObj.apply(allCols + "\t" + y)))
        case None => if (returnMissing) super.process(RowObj.apply(allCols + "\t" + missingVal))
      }
    }
  }
}
