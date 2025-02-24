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

package gorsat.Monitors

import gorsat.Commands.Analysis
import org.gorpipe.gor.model.Row
import org.gorpipe.gor.monitor.GorMonitor

case class MonitorProgress(milliSec : Int, gm : GorMonitor) extends Analysis {
  var m = 0L
  var t = System.currentTimeMillis
  var lastRowChr : String = ""
  var lastRowPos : Int = 0

  override def isTypeInformationMaintained: Boolean = true

  override def process(r : Row): Unit = {
    m += 1
    if (r.chr != lastRowChr) {
      if (lastRowChr != "") gm.log("GOR progress: "+lastRowChr+":"+lastRowPos)
    }
    if (m > 10) {
      val t2 = System.currentTimeMillis
      if (t2 - t > milliSec) {
        gm.log("GOR progress: "+r.chr+":"+r.pos)
        t = t2
        m = 0
      }
    }
    lastRowChr = r.chr
    lastRowPos = r.pos
    super.process(r)
  }
  override def finish(): Unit = {
    if (lastRowChr != "") gm.log("GOR progress: "+lastRowChr+":"+lastRowPos)
  }
}
