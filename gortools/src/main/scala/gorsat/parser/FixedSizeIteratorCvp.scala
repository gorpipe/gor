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

package gorsat.parser

import java.util.NoSuchElementException

import org.gorpipe.exceptions.GorDataException
import org.gorpipe.gor.model.ColumnValueProvider

/**
  * A column value provider for a list column with fixed size items. The value functions are overridden
  * to recognize special columns, for getting an individual item from the list, or the index of the current
  * item. Calling the 'next' method affects the behavior of the value functions. If the column passed to
  * the value functions is not one of the special values, the 'cvp' is used to get the value.
  * @param cvp A ColumnValueProvider - typically a Row object
  * @param source The string containing the list to be accessed
  * @param itemSize The size of an item
  */
case class FixedSizeIteratorCvp(cvp: ColumnValueProvider, source: String, itemSize: Int) extends ColumnValueProvider {
  private var start = -itemSize
  private var curIndex = 0

  if(source.length % itemSize != 0) {
    throw new GorDataException("Column size incorrect for fixed size iteration")
  }

  def hasNext: Boolean = {
    start + itemSize < source.length
  }

  def next(): Unit = {
    if(!hasNext) throw new NoSuchElementException

    start += itemSize
    curIndex += 1
  }

  def stringValue(): String = {
    source.substring(start, start + itemSize)
  }

  override def stringValue(col: Int): String = {
    col match {
      case SpecialColumns.ListItem => stringValue()
      case SpecialColumns.ListIndex => curIndex.toString
      case _ => cvp.stringValue(col)
    }
  }

  override def intValue(col: Int): Int = {
    col match {
      case SpecialColumns.ListItem => stringValue().toInt
      case SpecialColumns.ListIndex => curIndex
      case _ => cvp.intValue(col)
    }
  }

  override def longValue(col: Int): Long = {
    col match {
      case SpecialColumns.ListItem => stringValue().toLong
      case SpecialColumns.ListIndex => curIndex.toLong
      case _ => cvp.longValue(col)
    }
  }

  override def doubleValue(col: Int): Double = {
    col match {
      case SpecialColumns.ListItem => stringValue().toDouble
      case SpecialColumns.ListIndex => curIndex.toDouble
      case _ => cvp.doubleValue(col)
    }
  }

}
