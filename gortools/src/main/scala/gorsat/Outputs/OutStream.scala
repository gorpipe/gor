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

package gorsat.Outputs

import gorsat.Commands.Output
import org.gorpipe.gor.model.Row

import java.io.OutputStream

class OutStream(header: String = null, outputStream: OutputStream) extends Output {
  val out = new java.io.BufferedWriter(new java.io.OutputStreamWriter(outputStream), 1024 * 100)

  def setup(): Unit = {
    if (header != null) out.write(processHeader(header) + "\n")
  }

  def process(r: Row): Unit = {
    out.write(processRow(r).toString)
    out.write('\n')
  }

  def finish(): Unit = {
    out.flush
    out.close
  }

  protected def processHeader(header: String): String = {
    header
  }

  protected def processRow(r: Row): String = {
    r.toString()
  }

  def getHeader : String = header
}