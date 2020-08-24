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

package gorsat.Iterators

import org.gorpipe.model.genome.files.gor.Row
import org.gorpipe.model.gor.RowObj
import org.gorpipe.model.gor.iterators.RowSource

case class RowArrayIterator(lineList: Array[Row], length: Int) extends RowSource {
  var l: Array[Row] = lineList
  var index = 0

  override def hasNext: Boolean = length - index > 0

  override def next(): Row = {
    val r = l(index); index += 1; r
  }

  override def setPosition(seekChr: String, seekPos: Int) {}

  def close {}
}

object RowArrayIterator {
  def apply(lines: Array[String]): RowArrayIterator = {
    new RowArrayIterator(lines.map(x => RowObj.apply(x)), lines.length)
  }
}
