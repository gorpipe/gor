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
import org.gorpipe.model.gor.RowObj
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.util.Sorting.quickSort

object IteratorUtilities {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def sortGorFile(inputFileName: String, outputFileName: String, selCols: String = "all", spaceDelim: Boolean = false, useHeader: Boolean = false, skip: Int, gorRoot: String = "") {

    def fileLines(file: java.io.File) = scala.io.Source.fromFile(file, "utf-8", scala.io.Source.DefaultBufSize * 100).getLines // toList

    var lines = 0
    var fileNum = 1
    val batch = 1000000
    val inputFileSource: scala.Iterator[String] = fileLines(new java.io.File(inputFileName))
    var inputArray = new Array[Row](batch)
    var ordFileList: List[String] = List()

    var useCols: List[Int] = Nil
    var pickCols: List[Int] = Nil

    if (selCols != "all") {

      val cols = selCols.split("[ ,;]")

      cols.foreach(x => {
        val ss = x.split('-')
        var sta = 0
        var sto = 0
        if (ss.length == 1) {
          sta = ss(0).toInt
          sto = sta
        }
        if (ss.length == 2) {
          sta = ss(0).toInt
          sto = sta
          if (ss(1).length > 0) sto = ss(1).toInt
        }
        for (i <- sta to sto) useCols ::= i
      })
    }

    if (selCols != "all") pickCols = useCols.reverse.filter(_ >= 1)

    var linesSkipped = 0
    while (linesSkipped < skip) {
      inputFileSource.next
      linesSkipped += 1
    }

    var header: String = null
    if (useHeader) {
      if (spaceDelim) header = inputFileSource.next.split(""" +""", -1).mkString("\t") else header = inputFileSource.next
    }

    if (useHeader && selCols != "all") {
      val col = header.split("\t", -1)
      header = pickCols.map(c => col(c - 1)).mkString("\t")
    }

    while (inputFileSource.hasNext) {
      var line: String = null
      if (spaceDelim) line = inputFileSource.next.split(""" +""", -1).mkString("\t")
      else line = inputFileSource.next

      if (selCols != "all") {
        val col = line.split("\t", -1)
        line = pickCols.map(c => col(c - 1)).mkString("\t")
      }

      inputArray(lines) = RowObj(line)
      lines += 1
      if (lines == batch) {
        quickSort(inputArray)
        val outputFile = outputFileName + "_" + fileNum + ".gorsat.tmp"
        val fos = new java.io.FileOutputStream(outputFile)
        val osw = new java.io.OutputStreamWriter(fos)
        val bout = new java.io.BufferedWriter(osw, 1024 * 1000)
        inputArray.foreach(x => {
          val temp = x.toString.split("\t", -1)
          if (temp.size != 22) logger.debug("Temp size: "+temp.size)
        })
        inputArray.foreach(x => bout.write(x.toString + "\n"))
        bout.close()
        osw.close()
        fos.close()
        fileNum += 1
        lines = 0
        inputArray = new Array[Row](batch)
        ordFileList = outputFile :: ordFileList
      }
    }
    if (lines > 0) {
      val finalArray = new Array[Row](lines)
      for (i <- 0 until lines) finalArray(i) = inputArray(i)
      quickSort(finalArray)
      val outputFile = outputFileName + "_" + fileNum + ".gorsat.tmp"
      val os = new java.io.FileOutputStream(outputFile)
      var bout = new java.io.BufferedWriter(new java.io.OutputStreamWriter(os), 1024 * 1000)
      finalArray.foreach(x => bout.write(x + "\n"))
      bout.close()
      os.close()
      ordFileList = outputFile :: ordFileList
    }

    val os = new java.io.FileOutputStream(outputFileName)
    val out = new java.io.BufferedWriter(new java.io.OutputStreamWriter(os), 1024 * 100)

    if (useHeader) out.write(header + "\n")

    val sessionFactory = new GenericSessionFactory()
    val rowsource = new MultiFileSource(ordFileList, gorRoot, null, sessionFactory.create().getGorContext)
    rowsource.seek("chr1", 0)
    while (rowsource.hasNext) {
      val arow = rowsource.next()
      out.write(arow + "\n")
    }
    out.close()
    rowsource.close()

    ordFileList.foreach(x => {
      val f = new java.io.File(x)
      f.delete
    })

  }

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
    val su: String = s.toUpperCase
    su.endsWith(".NOR") || su.endsWith(".NORZ") || su.endsWith(".TSV")
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
