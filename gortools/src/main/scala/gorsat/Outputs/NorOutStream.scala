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

import org.gorpipe.gor.model.Row

import java.io.OutputStream

class NorOutStream(val header: String = null, val outputStream: OutputStream, skipHeader: Boolean = false) extends OutStream(header, outputStream) {
  override def setup {
    if (header != null & !skipHeader) out.write("#" + header.split("\t", -1).slice(2, 1000000).mkString("\t") + "\n")
  }

  override def process(r: Row) {
    out.write(r.otherCols)
    out.write('\n')
  }
}