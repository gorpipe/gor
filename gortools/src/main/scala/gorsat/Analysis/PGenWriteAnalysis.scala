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

import gorsat.Commands.Analysis
import org.gorpipe.gor.model.{FileReader, Row}
import org.gorpipe.gor.driver.pgen.{PGenWriter, PGenWriterFactory, VariantRecord}
import org.gorpipe.model.gor.RowObj

case class PGenWriteAnalysis(fileName: String, batch: Int, imputed: Boolean, threshold: Float, group: Boolean, refIdx: Int, altIdx: Int, rsIdIdx: Int, valueIdx: Int, fileReader: FileReader = null) extends Analysis {
  val BATCH_REPLACE = "#{batch}"
  var output = if (batch == -1) PGenWriterFactory.getPGenWriter(fileName, refIdx, altIdx, rsIdIdx, valueIdx, group, imputed, threshold, fileReader) else null
  var currentBatch = ""

  override def process(r: Row): Unit = {
    if (batch != -1) {
      val batchValue = r.stringValue(batch)
      if (!batchValue.equals(currentBatch)) {
        if (output!=null) {
          output.close()
          val batchName = fileName.replace(BATCH_REPLACE, currentBatch)
          super.process(RowObj("chrN",0,batchName))
        }
        currentBatch = batchValue
        val batchName = fileName.replace(BATCH_REPLACE, currentBatch)
        output = PGenWriterFactory.getPGenWriter(batchName, refIdx, altIdx, rsIdIdx, valueIdx, group, imputed, threshold, fileReader)
      }
    }
    output.write(r)
  }

  override def finish(): Unit = {
    if (output != null) output.close()
    if (currentBatch.nonEmpty) {
      val batchName = fileName.replace(BATCH_REPLACE, currentBatch)
      super.process(RowObj("chrN",0,batchName))
    }
    super.finish()
  }
}
