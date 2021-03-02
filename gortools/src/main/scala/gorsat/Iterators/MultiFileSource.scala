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

import org.gorpipe.gor.model.{GenomicIterator, GenomicIteratorBase, Row}
import org.gorpipe.gor.model.Row.SortInfo
import org.gorpipe.gor.session.GorContext

import scala.collection.mutable

class MultiFileSource(fileList: List[String], fileMap: mutable.Map[String, String], gorRoot: String, sortInfo: Array[SortInfo], context: GorContext) extends GenomicIteratorBase {
  var rowsource: GenomicIterator = _
  if (fileList.length < 2) {
    if (fileMap != null) rowsource = new SingleFileSource(fileList.head, gorRoot, context)
    else rowsource = new SingleFileSource(fileList.head,  gorRoot, context)
  } else {
    val (lf, rf) = fileList splitAt (fileList.length / 2)
    rowsource = new DuoSourceGen(lf, rf, null, null, "Files", fileMap, gorRoot, sortInfo, context)
  }

  def this(fileList: List[String], gorRoot: String, sortInfo: Array[SortInfo], context: GorContext) = this(fileList, null, gorRoot, sortInfo, context)

  override def hasNext: Boolean = rowsource.hasNext

  override def next(): Row = rowsource.next()

  override def seek(seekChr: String, seekPos: Int): Boolean = rowsource.seek(seekChr, seekPos)

  def close(): Unit = rowsource.close()

  override def getHeader: String = rowsource.getHeader
}
