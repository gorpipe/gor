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
import org.gorpipe.gor.GorSession
import org.gorpipe.model.genome.files.gor.Row
import org.gorpipe.model.gor.RowObj

case class SegHistAnalysis(maxCount: Int, session: GorSession) extends Analysis {
  var currentCount = 0
  var currentChrom = ""
  var currentStart = 0
  var currentEnd = 0
  var segmentExist = false

  override def process(r: Row): Unit = {
    if (r.chr != currentChrom) {
      if (segmentExist) { /* output the segment because we are switching chrom */
        currentEnd = session.getProjectContext.getReferenceBuild.getBuildSize.get(currentChrom)
        super.process(RowObj(currentChrom+'\t'+currentStart+'\t'+currentEnd+'\t'+currentCount))
      }
      /* Initialize a new chromosome, currentEnd will is defined below */
      currentChrom = r.chr
      currentStart = 0
      currentCount = 0
      currentEnd = 0
      segmentExist = true
    }

    var newCount = r.colAsInt(3) /* Mandatory count column - should surround with try catch for better error msg. */
    val newEnd = r.colAsInt(2)
    var newStart = r.pos
    while (currentCount + newCount > maxCount && newEnd-currentEnd > 1) { /* We cannot split a single base */
      /* We need to split current segment */
      val extraCount = currentCount + newCount - maxCount
      currentEnd = (newEnd-1).min((newEnd - (newEnd - newStart) * extraCount.toDouble / newCount).max(newStart + 1).toInt)
      super.process(RowObj(currentChrom + '\t' + currentStart + '\t' + currentEnd + '\t' + maxCount))
      /* Initialize the remainder with the leftovers */
      newCount = extraCount
      newStart = currentEnd
      currentStart = newStart
      currentCount = 0
    }
    /* Now we extend the current segment knowing we are not exceeding the count */
    currentCount += newCount
    currentEnd = currentEnd.max(newEnd)
  }

  override def finish(): Unit = {
    if (segmentExist) { /* output the segment because we are closing the stream and ending the chrom */
      currentEnd = session.getProjectContext.getReferenceBuild.getBuildSize.get(currentChrom)
      super.process(RowObj(currentChrom+'\t'+currentStart+'\t'+currentEnd+'\t'+currentCount))

    } /* if segments don't exist, we have no data on the chromosome */
  }

}