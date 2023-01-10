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

package gorsat.Utilities

import java.io.FileNotFoundException
import gorsat.DynIterator.DynamicNorSource
import gorsat.Iterators.{FastGorSource, MultiFileSource}
import gorsat.gorsatGorIterator.MapAndListUtilities
import gorsat.process.{GenericSessionFactory, SourceProvider}
import org.gorpipe.exceptions.{GorDataException, GorResourceException, GorSystemException}
import org.gorpipe.gor.model.Row
import org.gorpipe.gor.monitor.GorMonitor
import org.gorpipe.gor.session.{GorContext, GorSession}
import org.gorpipe.gor.util.DataUtil
import org.gorpipe.model.gor.RowObj
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.util.Sorting.quickSort
import scala.util.Using

object IteratorUtilities {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def getHeader(filename: String, gorRoot: String, context: GorContext): String = {
    val gm: GorMonitor = null
    val minLogTime = Int.MaxValue
    var header = ""
    context.getSession.getCache.getHeaderMap.synchronized {
      Option(context.getSession.getCache.getHeaderMap.getOrDefault(filename, null)) match {
        case Some(h) => header = h
        case None =>
          var gs:FastGorSource = null
          try {
            gs = new FastGorSource(filename, gorRoot, context, false, gm, minLogTime)
            header = gs.getHeader
            context.getSession.getCache.getHeaderMap.put(filename, header)
          } finally {
            if (gs != null) gs.close()
          }
      }
    }
    header
  }

  def validHeader(header: String): String = {
    val cols = header.split("\t", -1)
    val badSymbols: List[Char] = List('\\', '/', '*', '+', '-','\'','$',';',',')
    val usedCols: mutable.HashSet[String] = mutable.HashSet()

    //replacing escape characters
    var colMatrix: Array[Char] = null
    val builder = new StringBuilder(header.length)
    var isFirst = true
    var j:Int = 0
    cols.foreach { col =>
      var column = col

      colMatrix = col.toCharArray
      while (j < colMatrix.length) {
        if (badSymbols.contains(colMatrix(j))) colMatrix(j) = 'x'
        j += 1
      }
      j = 0
      column = new String(colMatrix)

      //just appending x to used columns
      var colToUp = column.toUpperCase
      while (usedCols.contains(colToUp)) {
        column = column + "x"
        colToUp = column.toUpperCase
      }
      usedCols.add(colToUp)

      if (!isFirst) {
        builder.append("\t")
      }

      isFirst = false
      builder.append(column)
    }

    builder.toString()
  }

  def getFirstLine(fileName: String, session: GorSession): String = {
    try {
      var header = session.getProjectContext.getFileReader.readHeaderLine(fileName)
      if (header == null) {
        throw new GorDataException("Error in getFirstLine in MAP.  header is null.  Problem with file: " + fileName)
      }
      if (header(0) == '#') header = header.slice(1, 1000000)
      header;
    } catch {
      case fnfe:FileNotFoundException =>
        throw new GorResourceException("Map file not found.", fileName, fnfe);
      case e: Exception =>
        throw new GorSystemException("Error in getFirstLine in MAP.  Problem with file: " + fileName, e)
    }
  }

  def shouldWrapInNor(s: String): Boolean = {
    DataUtil.isNorSource(s)
  }

  // todo: Add explicit unit tests
  def getStringArrayFromFileOrNestedQuery(source: String, context: GorContext): Array[String] = {
    // Some file types need to be read through nor to deal with the header in the file
    val adjustedSource = if(shouldWrapInNor(source)) s"<(nor $source)" else source

    val inputSource = SourceProvider(adjustedSource, context, executeNor = true, isNor = true)
    if (inputSource.iteratorCommand.nonEmpty) {
      MapAndListUtilities.getStringArray(inputSource.iteratorCommand, inputSource.dynSource
        .asInstanceOf[DynamicNorSource], context.getSession)
    } else {
      MapAndListUtilities.getStringArray(source, context.getSession)
    }
  }
}
