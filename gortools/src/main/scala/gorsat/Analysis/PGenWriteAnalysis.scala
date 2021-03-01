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

import gorsat.Commands.Analysis
import org.gorpipe.gor.model.Row
import org.gorpipe.gor.driver.pgen.PGenWriterFactory

case class PGenWriteAnalysis(fileName: String, imputed: Boolean, threshold: Float, group: Boolean, refIdx: Int, altIdx: Int, rsIdIdx: Int, valueIdx: Int) extends Analysis {

  val output = PGenWriterFactory.getPGenWriter(fileName, refIdx, altIdx, rsIdIdx, valueIdx, group, imputed, threshold)

  override def process(r: Row): Unit = {
    output.write(r)
  }

  override def finish(): Unit = {
    output.close()
  }
}
