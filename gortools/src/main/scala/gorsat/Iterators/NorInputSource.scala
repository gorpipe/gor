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

import java.util
import java.util.stream

import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.model.genome.files.gor.Row
import org.gorpipe.exceptions.{GorParsingException, GorSystemException}
import org.gorpipe.gor.stats.StatsCollector
import org.gorpipe.model.genome.files.gor.{FileReader, Row}
import org.gorpipe.model.gor.RowObj
import org.gorpipe.model.gor.iterators.RowSource

import scala.collection.mutable
import scala.io.StdIn

class NorInputSource(fileName: String, fileReader: FileReader, readStdin: Boolean, forceReadHeader: Boolean, maxWalkDepth: Int, showModificationDate: Boolean, ignoreEmptyLines: Boolean) extends RowSource {
  private var stats: StatsCollector = _
  private var statsSenderId = -1

  var myHasNext: Boolean = false
  var myNext: String = _
  var mustReCheck: Boolean = true
  var haveReadHeader: Boolean = false
  var myHeader: String = _
  val useCSV: Boolean = fileName.toUpperCase.endsWith(".CSV")
  var haveLoadedLines = false
  val IGNORE_PATTERN = "##"


  val norRowSource: stream.Stream[String] = if (!readStdin) fileReader.iterateFile(fileName, maxWalkDepth, showModificationDate) else throw new GorParsingException("Stdin not supported in NOR context.")
  val norRowIterator: util.Iterator[String] = norRowSource.filter(x => !x.startsWith(IGNORE_PATTERN)).iterator()

  override def hasNext: Boolean = {
    if (!haveReadHeader) {
      getHeader
    }
    if (!mustReCheck) return myHasNext
    if (readStdin) {
      myNext = StdIn.readLine()
    } else {
      myNext = if (norRowIterator.hasNext) norRowIterator.next() else null
    }
    if (myNext != null) myHasNext = true else myHasNext = false
    mustReCheck = false

    if (ignoreEmptyLines) {
      while (myNext != null && myNext.isEmpty && myHasNext) {
        mustReCheck = true
        hasNext
      }
    }

    myHasNext
  }

  def nextLine(): String = {
    if (hasNext) {
      mustReCheck = true
      if (useCSV) {
        myNext.replace(',', '\t')
      } else {
        myNext
      }
    } else {
      throw new GorSystemException("NorInputSource.hasNext: getRow call on false hasNext!", null)
    }
  }

  override def next(): Row = {
    RowObj("chrN\t0\t" + nextLine())
  }

  override def setPosition(seekChr: String, seekPos: Int) {
    /* do nothing */
  }

  // def close = { allNORrows = Nil }
  def close(): Unit = {
    if (norRowSource != null) norRowSource.close()
  }

  def createNewHeader(): String = {
    val builder = new mutable.StringBuilder()

    builder.append("ChromNOR\tPosNOR")

    val columns = myHeader.split("\t", -1)

    for (i <- Range(1, columns.length + 1)) {
      builder.append("\tcol")
      builder.append(i)
    }

    builder.toString()
  }

  override def getHeader: String = {
    if (haveReadHeader) return myHeader
    myHeader = if (readStdin) StdIn.readLine() else if (norRowIterator.hasNext) norRowIterator.next() else null
    if (myHeader == null) {
      myHasNext = false
      mustReCheck = false
      return ""
    }
    if (useCSV) myHeader = myHeader.replace(',', '\t')
    if (myHeader.startsWith("#") || myHeader.startsWith("ChromNOR") || forceReadHeader) {
      mustReCheck = true
      while (myHeader.startsWith("#")) {
        myHeader = myHeader.slice(1, myHeader.length)
      }
      myHeader = "ChromNOR\tPosNOR\t" + myHeader
    } else {
      myNext = myHeader
      myHasNext = true
      mustReCheck = false
      myHeader = createNewHeader()
    }
    haveReadHeader = true
    myHeader
  }
}
