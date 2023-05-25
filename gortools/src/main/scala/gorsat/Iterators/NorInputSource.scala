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

import gorsat.Commands.CommandParseUtilities
import org.gorpipe.exceptions.{GorParsingException, GorSystemException}
import org.gorpipe.gor.model.{FileReader, GenomicIteratorBase, QuoteSafeRowBase, Row}
import org.gorpipe.gor.util.DataUtil

import java.io.{BufferedReader, InputStreamReader}
import java.util
import java.util.stream
import java.util.zip.GZIPInputStream
import scala.collection.mutable
import scala.io.StdIn

class NorInputSource(fileName: String, fileReader: FileReader, readStdin: Boolean, forceReadHeader: Boolean, maxWalkDepth: Int, followLinks: Boolean, showModificationDate: Boolean, ignoreEmptyLines: Boolean) extends GenomicIteratorBase {

  var myHasNext: Boolean = false
  var myNext: String = _
  var mustReCheck: Boolean = true
  var haveReadHeader: Boolean = false
  var myHeader: String = _
  var myHeaderLength = 0
  private val useCSV: Boolean = DataUtil.isAnyCsv(fileName)
  val filter: String => Boolean = (s: String) => !s.startsWith("##")


  private val norRowSource: stream.Stream[String] = if (!readStdin) {
    // ToDo: Add support for compressed files in the driver framework.
    if (DataUtil.isGZip(fileName)) {
      new BufferedReader(new InputStreamReader(new GZIPInputStream(fileReader.getInputStream(fileName)))).lines()
    } else {
      fileReader.iterateFile(fileName, maxWalkDepth, followLinks, showModificationDate)
    }
  } else {
    throw new GorParsingException("Stdin not supported in NOR context.")
  }

  private val norRowIterator: util.Iterator[String] = if (DataUtil.isMeta(fileName))
    norRowSource.iterator()
  else
    norRowSource.filter(filter(_)).iterator()

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
        val nextSplit = CommandParseUtilities.quoteSafeSplit(myNext, ',')
        nextSplit.mkString("\t")
      } else {
        myNext
      }
    } else {
      throw new GorSystemException("NorInputSource.hasNext: getRow call on false hasNext!", null)
    }
  }

  override def next(): Row = {
    new QuoteSafeRowBase("chrN\t0\t" + nextLine(), myHeaderLength)
  }

  override def seek(seekChr: String, seekPos: Int): Boolean = {
    /* do nothing */
    true
  }

  // def close = { allNORrows = Nil }
  def close(): Unit = {
    if (norRowSource != null) norRowSource.close()
  }

  private def createNewHeader(): String = {
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
    myHeaderLength = myHeader.split("\t").length
    myHeader
  }
}
