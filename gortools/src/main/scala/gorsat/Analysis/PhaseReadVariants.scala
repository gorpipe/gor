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
import org.gorpipe.gor.session.GorSession
import org.gorpipe.model.gor.RowObj
import org.gorpipe.model.gor.iterators.RefSeq

/**
  * Passes all variations through but also adds all combinations of combined variants (with the same id)
  * that fit within the given max merge distance.
  *
  * @param maxBpMergeDist max bp distance between the start of the first variation and the end of the last one.
  * @param session session info.
  *
  *                       Assumes the following input/output format:
  *
  *                       chr                 chromosome
  *                       pos                 position
  *                       ref                 reference
  *                       alt                 alt
  *                       varpos              position of the first variant.
  *                       varqual             minimum quality of the variants.
  *                       id                  id
  *                       [other columns]     other columns are passed through.
  *
  */
case class PhaseReadVariants(maxBpMergeDist: Int, session: GorSession) extends Analysis {
  var oldChr: String = _
  var oldId: String = _
  var buffer = new scala.collection.mutable.ArrayBuffer[Row]

  val refSeq: RefSeq = session.getProjectContext.createRefSeq()

  def outputRows(): Unit = {

    var rowIndex = 0
    while (rowIndex < buffer.length) {
      val row = buffer(rowIndex)

      // The basic variation.

      super.process(row)

      // Add phased combined variations.

      var ref = row.colAsString(2).toString
      var alt = row.colAsString(3).toString
      if (ref != alt) {
        // Find combined variations.
        var secondRowIndex = rowIndex + 1
        var quality = row.colAsInt(5)

        while (secondRowIndex < buffer.length) {
          val secondRow = buffer(secondRowIndex)
          val secondRowRef = secondRow.colAsString(2).toString
          var secondRowAlt = secondRow.colAsString(3).toString

          // Ignore non-variations.
          if (secondRowRef != secondRowAlt) {

            // Check if this variation is within the merge distance.
            // varDistance > ref.length || secondRowRef.length < ref.length ensures that the second row is not
            // changing the same bp as row (checks the change is longer than the reference with the exception of
            // inserts).
            val varDistance = Math.abs(secondRow.pos - row.pos) + secondRowRef.length
            if (varDistance <= maxBpMergeDist
              && (varDistance > ref.length || secondRowRef.length < secondRowAlt.length)) {

              // Get the reference
              val startingRefPos = ref.length
              var refIndex = startingRefPos
              while (refIndex < varDistance) {
                ref += refSeq.getBase(row.chr, row.pos + refIndex).toUpper
                refIndex += 1
              }

              // For snp + insert at the same location, we skip the first bp, as it us the same as ref.
              if (row.pos == secondRow.pos && secondRowAlt.length > secondRowRef.length) {
                secondRowAlt = secondRowAlt.substring(1)
              }

              // The variation
              alt = alt +
                ref.substring(startingRefPos, Math.max(startingRefPos, ref.length - secondRowRef.length)) +
                secondRowAlt

              quality = Math.min(quality, secondRow.colAsInt(5))

              // Write out the new row.
              super.process(RowObj(row.chr, row.pos, ref + "\t" + alt + "\t" + row.colAsInt(4) + "\t" + quality + "\t" + row.otherColsSlice(6, row.numCols)))
            }
          }

          secondRowIndex += 1
        }
      }

      rowIndex += 1
    }

    // Finally clear
    buffer.clear()
  }

  override def process(r: Row) {
    // Id column is number 7.
    val id = r.colAsString(6).toString
    if (r.chr != oldChr || id != oldId) {
      outputRows()
      oldChr = r.chr
      oldId = id
    }

    buffer += r
  }

  override def finish() {
    try {
      if (!isInErrorState) {
        outputRows()
      }
    } finally {
      refSeq.close()
    }
  }
}
