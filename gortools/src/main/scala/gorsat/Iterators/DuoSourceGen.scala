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

import org.gorpipe.exceptions.GorSystemException
import org.gorpipe.gor.GorContext
import org.gorpipe.model.genome.files.gor.Row
import org.gorpipe.model.genome.files.gor.Row.SortInfo
import org.gorpipe.model.gor.iterators.{RowSource, SingleFileSource}

import scala.collection.mutable

class DuoSourceGen(leftList: List[String], rightList: List[String],
                   lsl: List[RowSource], rsl: List[RowSource], sourceType: String, fileMap: mutable.Map[String, String], gorRoot: String, sortInfo: Array[SortInfo], context: GorContext) extends RowSource {
  var leftsource: RowSource = _
  var rightsource: RowSource = _
  if (sourceType == "Files") {
    if (leftList.length < 2) {
      if (fileMap != null) leftsource = new SingleFileSource(leftList.head, gorRoot, context)
      else leftsource = new SingleFileSource(leftList.head, gorRoot, context)
    } else {
      val (lf, rf) = leftList splitAt (leftList.length / 2)
      leftsource = new DuoSourceGen(lf, rf, null, null, "Files", fileMap, gorRoot, sortInfo, context)
    }
    if (rightList.length < 2) {
      if (fileMap != null) rightsource = new SingleFileSource(rightList.head, gorRoot, context)
      else rightsource = new SingleFileSource(rightList.head,  gorRoot, context)
    } else {
      val (lf, rf) = rightList splitAt (rightList.length / 2)
      rightsource = new DuoSourceGen(lf, rf, null, null, "Files", fileMap, gorRoot, sortInfo, context)
    }
  } else {
    if (lsl.length < 2) {
      leftsource = lsl.head
    } else {
      val (lf, rf) = lsl splitAt (lsl.length / 2)
      leftsource = new DuoSourceGen(null, null, lf, rf, "NOT Files", fileMap, gorRoot, sortInfo, context)
    }
    if (rsl.length < 2) {
      rightsource = rsl.head
    } else {
      val (lf, rf) = rsl splitAt (rsl.length / 2)
      rightsource = new DuoSourceGen(null, null, lf, rf, "NOT Files", fileMap, gorRoot, sortInfo, context)
    }
  }
  var myNext: Row = _
  var rightHasNext: Boolean = false
  var leftHasNext: Boolean = false
  var rightMyNext: Row = _
  var leftMyNext: Row = _
  var posSet: Boolean = false
  var notCalledHasNext = true

  def takeFromLeft(): Unit = {
    myNext = leftMyNext
    leftHasNext = leftsource.hasNext
    leftMyNext = if (leftHasNext) leftsource.next() else null
  }

  def takeFromRight(): Unit = {
    myNext = rightMyNext
    rightHasNext = rightsource.hasNext
    rightMyNext = if (rightHasNext) rightsource.next() else null
  }

  override def next(): Row = {
    if (notCalledHasNext) if (!hasNext) {
      throw new GorSystemException("hasNext in duoSourceGen: getRow call on false hasNext!", null)
    }
    if (leftHasNext && rightHasNext) {
      if (leftMyNext.advancedCompare(rightMyNext, sortInfo) < 0) takeFromLeft() else takeFromRight()
    }
    else if (leftHasNext) takeFromLeft()
    else if (rightHasNext) takeFromRight()
    else myNext = null
    myNext
  }

  override def hasNext: Boolean = {
    if (!posSet) {
      throw new GorSystemException("Row position has not bee set", null)
    }
    if (notCalledHasNext) {
      takeFromLeft()
      takeFromRight()
      notCalledHasNext = false
    }
    leftHasNext || rightHasNext
  }

  override def setPosition(seekChr: String, seekPos: Int) {
    leftsource.setPosition(seekChr, seekPos)
    rightsource.setPosition(seekChr, seekPos)
    posSet = true
    notCalledHasNext = true
  }

  def close(): Unit = {
    rightsource.close()
    leftsource.close()
  }

  override def getHeader: String = leftsource.getHeader // Should actuall check if the right and left are identical
}
