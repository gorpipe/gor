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

import org.gorpipe.model.genome.files.gor.Row
import org.gorpipe.model.genome.files.gor.Row
import gorsat.Commands.Analysis
import org.gorpipe.model.genome.files.gor.{GorMonitor, Row}

case class MonitorLog(logname : String, n : Int, gm : GorMonitor) extends Analysis {
  var m = 0L
  override def process(r : Row) {
    m += 1; if ((m % n) == 0) gm.log(logname+"> ("+m+") "+r.toColString)
    super.process(r)
  }
}