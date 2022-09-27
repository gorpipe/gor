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
import org.gorpipe.gor.model.Row
import org.gorpipe.model.gor.RowObj

case class CigarSegsAnalysis(cigarCol: Int, grCols: Array[Int]) extends Analysis {

  val grColsArray = grCols
  var id: Long = 0
  val useGroup = if (grColsArray.length > 0) true else false

  def parseCigar(s: CharSequence, p: Int) = {
    var num = 0
    var control: Char = 'E'
    var pos = p
    while (pos < s.length && control == 'E') {
      if (s.charAt(pos) >= '0' && s.charAt(pos) <= '9') {
        num = num * 10 + (s.charAt(pos) - '0'); pos += 1
      }
      else control = s.charAt(pos)
    }
    if (control == 'E') {
      throw new GorDataException("Cigar string error in " + s, -1)
    }
    (num, control, pos + 1)
  }

  override def process(r: Row): Unit = {
    var readShift = 0
    var refShift = 0
    var ci = 0
    val gccolumns = if (useGroup) r.selectedColumns(grColsArray) else ""
    val cigar = r.colAsString(cigarCol)
    while (ci < cigar.length) {
      val (numBases, cigarControl, nextCi) = parseCigar(cigar, ci)
      ci = nextCi
      id += 1
      cigarControl match {
        case 'M' | '=' | 'X' => {
          if (useGroup) super.process(RowObj(r.chr, r.pos - 1 + refShift, (r.pos + refShift + numBases - 1) + "\t" + id + "\t" + gccolumns))
          else super.process(RowObj(r.chr, r.pos - 1 + refShift, (r.pos + refShift + numBases - 1) + "\t" + id))
          refShift += numBases
          readShift += numBases
        }
        case 'N' => {
          refShift += numBases
        }
        case 'S' => {
          readShift += numBases
        }
        case 'H' => {
          /* do nothing */
        }
        case 'D' => {
          refShift += numBases
        }
        case 'I' => {
          readShift += numBases
        }
        case 'P' => {
          /* do nothing */
        }
        case _ => {
          throw new GorDataException("Unknown symbol in cigar string " + cigar, cigarCol, getHeader(), r.toString)
        }
      }
    }
  }
}
