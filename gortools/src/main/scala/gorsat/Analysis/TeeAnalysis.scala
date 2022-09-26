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
import org.gorpipe.gor.model.Row
import org.gorpipe.model.gor.RowObj

case class TeeAnalysis(teeStep: Analysis) extends Analysis {

  var noMoreCount = 0  // left and right report only once each

  override def reportWantsNoMore() : Unit = {
    if (noMoreCount>0 && !wantsNoMore) {
      if (pipeFrom!=null) pipeFrom.reportWantsNoMore()
      wantsNoMore = true
    } else noMoreCount += 1
  }

  override def setup() : Unit = {
    teeStep.securedSetup(null)
    teeStep.from(this)
  }

  override def process(r : Row) : Unit ={
    if (!teeStep.wantsNoMore) {
      teeStep.process(RowObj(r.toString))
    }
    super.process(r)
  }

  override def finish() : Unit = {
    teeStep.securedFinish(null)
    super.finish()
  }
}
