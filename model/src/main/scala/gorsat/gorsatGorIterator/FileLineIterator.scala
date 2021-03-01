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

package gorsat.gorsatGorIterator

import org.gorpipe.exceptions.GorSystemException
import org.gorpipe.gor.model.{FileReader, Row}
import org.gorpipe.model.gor.RowObj
import org.gorpipe.model.gor.iterators.LineIterator

case class FileLineIterator(fileName: String, reader: FileReader) extends LineIterator {
  var haveLoadedLines = false
  var allNORrows: List[String] = Nil

  def hasNext: Boolean = {
    if (!haveLoadedLines) {
      allNORrows = reader.readAll(fileName).toList
      haveLoadedLines = true
    }
    allNORrows != Nil
  }

  def next : Row = {
    RowObj( nextLine )
  }
  def nextLine: String = {
    if (hasNext) {
      val temp = allNORrows.head
      allNORrows = allNORrows.tail
      temp
    }
    else {
      throw new GorSystemException("FileLineIterator.next called on false hasNext for file: " + fileName, null)
    }
  }

  def close() {
    allNORrows = null
  }
}
