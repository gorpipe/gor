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
import org.gorpipe.exceptions.GorDataException
import org.gorpipe.model.genome.files.gor.Row

case class CheckOrder(message: String = "") extends Analysis {
  var n = 0L
  var lastChr = ""
  var lastPos = -1

  override def isTypeInformationMaintained: Boolean = true

  override def process(r: Row) {
    if (r.chr < lastChr || (r.chr == lastChr && r.pos < lastPos)) {
      val extra = if (message.isEmpty) "" else message + "\n"
      throw new GorDataException(extra + "Wrong order observed at row: " + r + "\nLast row: " + lastChr + ":" + lastPos, -1, getHeader(), r.toString)
    } else {
      super.process(r)
    }
    lastChr = r.chr
    lastPos = r.pos
    n += 1
  }
}
