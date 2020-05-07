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

package gorsat.Iterators

import org.gorpipe.gor.driver.providers.stream.datatypes.gor.GorHeader
import gorsat.Commands._
import org.gorpipe.model.genome.files.gor.Row
import org.gorpipe.model.gor.RowObj
import org.gorpipe.model.gor.iterators.RowSource

case class CountingGorRowIterator(genomicRange: GenomicRange.Range, segmentValue: Int, stepValue: Int) extends RowSource {
  var chromPosition: Int = genomicRange.start

  override def hasNext: Boolean = chromPosition < genomicRange.stop

  override def next(): Row = {
    var rowValues = genomicRange.chromosome + "\t" + chromPosition

    if (segmentValue > Int.MinValue) {
      rowValues += "\t" + (chromPosition + segmentValue)
    }

    chromPosition += stepValue

    RowObj(rowValues)
  }

  override def getHeader: String = {
    "chrom\t" + (
      if (segmentValue > Int.MinValue)
        "bpStart\tbpStop"
      else
        "pos"
      )
  }


  override def getGorHeader: GorHeader = {
    val header = new GorHeader()
    header.addColumn("chrom", "S")
    if(segmentValue > Int.MinValue) {
      header.addColumn("bpStart", "I")
      header.addColumn("bpStop", "I")
    } else {
      header.addColumn("pos", "I")
    }
    header
  }

  override def setPosition(seekChr: String, seekPos: Int) {}

  def close {}
}
