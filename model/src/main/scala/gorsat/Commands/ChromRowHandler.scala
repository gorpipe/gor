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

package gorsat.Commands

import org.gorpipe.gor.GorSession
import org.gorpipe.model.genome.files.gor.Row

case class ChromRowHandler(session: GorSession) extends RowHandler {
  val binIDgen = RegularBinIDgen(1)

  def process(r: Row, BA: BinAggregator) {
    val chr = r.chr
    val binID = binIDgen.ID(1)
    try {
      val (sta, sto) = (0, session.getProjectContext.getReferenceBuild.getBuildSize.get(r.chr))
      BA.update(r, binID, chr, sta, sto)
    } catch {
      case _: Exception =>
        val (sta, sto) = (0, 1000000000)
        BA.update(r, binID, chr, sta, sto)
    }
  } // One ouput row per input row
}
