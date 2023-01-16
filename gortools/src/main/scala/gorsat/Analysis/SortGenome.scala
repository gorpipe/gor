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

import java.util
import gorsat.Commands.Analysis
import gorsat.Iterators.{MultiFileSource, RowArrayIterator}
import gorsat.Outputs.OutFile
import gorsat.process.{GenericGorRunner, GenericSessionFactory}
import org.gorpipe.exceptions.custom.GorWriteQuotaExceededException
import org.gorpipe.gor.driver.meta.DataType
import org.gorpipe.gor.model.{GenomicIterator, Row}
import org.gorpipe.gor.session.GorSession

case class SortGenome(header: String, session: GorSession, sortInfo: Array[Row.SortInfo], div: Int = 1) extends Analysis {
  var lines = 0
  var batchSize: Int = System.getProperty("gor.sort.batchSize", "2000000").toInt / div
  var maxBufferSize: Int = batchSize * 100
  var bufferSize = 0
  private var alreadySorted = true

  private var inputArray = new Array[Row](batchSize)
  private var ordFileList: List[String] = List()
  private var wroteBuffer = false
  // If no quota is set we default to 0 and do not deal with write quotas
  private val writeQuota: Long = System.getProperty("gor.querylimits.writequota.mb", "0").toLong * 1024 * 1024
  private var writeQuotaUsed = 0L

  override def isTypeInformationMaintained: Boolean = true

  def reinit(): Unit = {
    resetBuffer()

    ordFileList = List()
    wroteBuffer = false
    writeQuotaUsed = 0L
  }

  def resetBuffer(): Unit = {
    var i = 0
    while(i < lines) {
      inputArray(i) = null
      i += 1
    }
    lines = 0
    bufferSize = 0
    alreadySorted = true
  }

  private def flushToDisk(): Unit = {
    ensureSorted()
    val outputArray = inputArray.take(lines)
    resetBuffer()

    val f = java.io.File.createTempFile("gorsort", DataType.GORZ.suffix)
    f.deleteOnExit()
    val outputFile = f.getAbsolutePath
    ordFileList = outputFile :: ordFileList
    wroteBuffer = true

    val runner = new GenericGorRunner
    val sortFileReader = session.getProjectContext.getSystemFileReader
    runner.run(RowArrayIterator(outputArray, outputArray.length), OutFile.driver(outputFile, sortFileReader, header, skipHeader = false, OutputOptions(writeMeta = false)))
  }

  private def ensureSorted(): Unit = {
    if (!alreadySorted) util.Arrays.parallelSort(inputArray, 0, lines, (o1: Row, o2: Row) => o1.advancedCompare(o2, sortInfo))
  }

  override def process(r: Row): Unit = {
    if (writeQuota > 0) {
      writeQuotaUsed += r.getAllCols.length
      if (writeQuotaUsed > writeQuota) {
        throw new GorWriteQuotaExceededException(s"Write quota exceeded. Write quota is set to ${writeQuota / (1024 * 1024)} MB")
      }
    }
    if (alreadySorted && lines > 0 && r.advancedCompare(inputArray(lines - 1), sortInfo) < 0) {
      alreadySorted = false
    }
    inputArray(lines) = r
    bufferSize += r.getAllCols.length
    lines += 1
    if (lines == batchSize || bufferSize > maxBufferSize) {
      flushToDisk()
    }
  }

  override def finish(): Unit = {
    if (wroteBuffer) {
      var rSource: GenomicIterator = null
      try {
        if (lines > 0) flushToDisk()
        val gorString = ordFileList.mkString(" ")
        val sessionFactory = new GenericSessionFactory()
        rSource = new MultiFileSource(gorString.split(' ').toList, null, "", sortInfo, sessionFactory.create().getGorContext)
        rSource.seek("", 0)

        while (rSource.hasNext) super.process(rSource.next())
      } finally {
        if (rSource != null) {
          try {
            rSource.close()
          } catch {
            case _: Exception =>
              // Do nothing
          }
        }
        ordFileList.foreach(x => {
          try {
            val f = new java.io.File(x)
            f.delete
          } catch {
            case _: Exception =>
              // Do nothing
          }
        })
      }
    } else {
      ensureSorted()
      inputArray.take(lines).foreach(r => super.process(r))
    }
  }
}
