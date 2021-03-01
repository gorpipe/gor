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

package gorsat.Outputs

import gorsat.Commands.Output
import org.gorpipe.gor.model.Row

/**
  * @param name Name of the file to be written.
  * @param header Header of the incoming source.
  * @param skipHeader Whether the header should be written or not.
  * @param append Whether we should write the output to the beginning or end of the file.
  */
class CmdFileOut(name: String, header: String, skipHeader: Boolean = false, append: Boolean = false) extends Output {
  val out = new java.io.BufferedWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(name, append)), 1024 * 100)

  def setup {
    if (header != null & !skipHeader) {
      val outHeader = header.split("\t", -1).slice(2, 1000000).mkString("\t")
      if (!outHeader.startsWith("#")) {
        out.write("#")
      }
      out.write(outHeader  + "\n")
    }
  }

  def process(r: Row) {
    out.write(r.otherCols)
    out.write('\n')
  }

  def finish {
    out.flush
    out.close
  }
}
