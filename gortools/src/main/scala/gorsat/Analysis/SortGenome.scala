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

package gorsat.Analysis

import java.util

import gorsat.Commands.Analysis
import gorsat.Iterators.{MultiFileSource, RowArrayIterator}
import gorsat.Outputs.OutFile
import gorsat.process.{GenericGorRunner, GenericSessionFactory}
import org.gorpipe.exceptions.custom.GorWriteQuotaExceededException
import org.gorpipe.gor.model.Row
import org.gorpipe.gor.session.GorSession
import org.gorpipe.model.gor.iterators.RowSource

case class SortGenome(header: String, session: GorSession, sortInfo: Array[Row.SortInfo], div: Int = 1) extends Analysis {
  var lines = 0
  var batch: Int = System.getProperty("gor.sort.batchSize", "2000000").toInt / div
  var maxBufferSize: Int = batch * 100
  var bufferSize = 0
  var alreadySorted = true

  var inputArray = new Array[Row](batch)
  var ordFileList: List[String] = List()
  var wroteBuffer = false
  // If no quota is set we default to 0 and do not deal with write quotas
  val writeQuota: Long = System.getProperty("gor.querylimits.writequota.mb", "0").toLong * 1024 * 1024
  var writeQuotaUsed = 0l

  override def isTypeInformationMaintained: Boolean = true

  def reinit() {
    inputArray = new Array[Row](batch)
    ordFileList = List()
    lines = 0
    bufferSize = 0
    alreadySorted = true
    wroteBuffer = false
    writeQuotaUsed = 0l
  }

  def sortBuffer(inputArray: Array[Row], length: Int) {
    if (!alreadySorted) util.Arrays.parallelSort(inputArray, 0, length, (o1: Row, o2: Row) => o1.advancedCompare(o2, sortInfo))
    val f = java.io.File.createTempFile("gorsort", ".gorz")
    f.deleteOnExit()
    val outputFile = f.getAbsolutePath

    lines = 0
    bufferSize = 0
    alreadySorted = true
    ordFileList = outputFile :: ordFileList
    wroteBuffer = true
    val runner = new GenericGorRunner
    runner.run(RowArrayIterator(inputArray, length), OutFile(outputFile, header))
  }

  override def process(r: Row) {
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
    if (lines == batch || bufferSize > maxBufferSize) {
      sortBuffer(inputArray, lines)
    }
  }

  override def finish() {
    if (wroteBuffer) {
      var rSource: RowSource = null
      try {
        if (lines > 0) sortBuffer(inputArray, lines)
        inputArray = null
        val gorString = ordFileList.mkString(" ")
        val sessionFactory = new GenericSessionFactory()
        rSource = new MultiFileSource(gorString.split(' ').toList, null, "", sortInfo, sessionFactory.create().getGorContext)
        rSource.setPosition("chr", 0)

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
      if (!alreadySorted) util.Arrays.parallelSort(inputArray, 0, lines, (o1: Row, o2: Row) => o1.advancedCompare(o2, sortInfo))
      inputArray.take(lines).foreach(r => super.process(r))
      inputArray = null
    }
  }
}
