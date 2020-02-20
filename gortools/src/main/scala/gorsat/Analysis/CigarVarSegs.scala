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
import org.gorpipe.gor.GorSession
import org.gorpipe.model.genome.files.gor.Row
import org.gorpipe.model.gor.RowObj
import org.gorpipe.model.gor.iterators.RefSeq

case class CigarVarSegs(cigarCol: Int, grCols: Array[Int], useRef: Boolean, outputBases: Boolean, seqBasesCol: Int,
                        seqQualCol: Int, session: GorSession) extends Analysis {

  val grColsArray: Array[Int] = grCols
  var id: Long = 0
  val useGroup: Boolean = if (grColsArray.length > 0) true else false

  var readShift = 0
  var refShift = 0
  var ci = 0
  var gccolumns = ""
  var cigar : CharSequence = ""
  var ref = ""
  var alt = ""
  var refpos: Int = 0
  var theRow: Row = _

  var varPos = 0
  var qualBases : CharSequence = ""

  val refSeq: RefSeq = session.getProjectContext.createRefSeq()

  def parseCigar(s: CharSequence, p: Int): (Int, Char, Int) = {
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

  def varQual(x: Int): Int = if (x < qualBases.length) qualBases.charAt(x) - 33; else 0

  def outputRow() {
    if (ref != "" || alt != "") {
      if (useRef) {
        if (ref.length == alt.length) {
          var b = 0
          while (b < ref.length) {
            val (re, al) = (ref(b).toUpper, alt(b).toUpper)
            if (re != al || outputBases) {
              if (useGroup) super.process(RowObj(theRow.chr, refpos + b, re + "\t" + al + "\t" + (varPos + b) + "\t" + varQual(varPos + b) + (if (!outputBases) "\t" + id else "\tM") + "\t" + gccolumns))
              else super.process(RowObj(theRow.chr, refpos + b, re + "\t" + al + "\t" + (varPos + b) + "\t" + varQual(varPos + b) + (if (!outputBases) "\t" + id else "\tM")))
            }
            b += 1
          }
        } else {
          if (outputBases) {
            val cigarControl = if (alt.length > ref.length) "I" else "D"
            var b = 0
            while (b < alt.length.max(ref.length)) {
              val (re, al) = (if (b < ref.length) ref(b).toUpper else "", if (b < alt.length) alt(b).toUpper else "")
              if (useGroup) super.process(RowObj(theRow.chr, refpos + b, re + "\t" + al + "\t" + (varPos + b) + "\t" + varQual(varPos + b) + "\t" + cigarControl + "\t" + gccolumns))
              else super.process(RowObj(theRow.chr, refpos + b, re + "\t" + al + "\t" + (varPos + b) + "\t" + varQual(varPos + b) + "\t" + cigarControl))
              b += 1
            }
          } else {
            if (useGroup) super.process(RowObj(theRow.chr, refpos, ref.toUpperCase + "\t" + alt + "\t" + varPos + "\t" + varQual(varPos) + "\t" + id + "\t" + gccolumns))
            else super.process(RowObj(theRow.chr, refpos, ref.toUpperCase + "\t" + alt + "\t" + varPos + "\t" + varQual(varPos) + "\t" + id))
          }
        }
      } else {
        if (useGroup) super.process(RowObj(theRow.chr, refpos, alt + "\t" + id + "\t" + gccolumns))
        else super.process(RowObj(theRow.chr, refpos, alt + "\t" + id))
      }
      ref = ""
      alt = ""
    }
  }

  override def finish() {
    refSeq.close()
  }

  override def process(r: Row) {
    readShift = 0
    refShift = 0
    ci = 0
    gccolumns = if (useGroup) r.selectedColumns(grColsArray) else ""
    cigar = r.colAsString(cigarCol)
    val seqBases = r.colAsString(seqBasesCol)
    qualBases = r.colAsString(seqQualCol)
    refpos = r.pos
    ref = ""
    alt = ""
    theRow = r
    varPos = 0
    id += 1

    while (ci < cigar.length) {
      val (numBases, cigarControl, nextCi) = parseCigar(cigar, ci)
      ci = nextCi
      cigarControl match {
        case 'M' | '=' | 'X' =>
          alt += seqBases.subSequence(readShift, readShift + numBases)
          if (useRef) {
            var i = 0
            while (i < numBases) {
              ref += refSeq.getBase(r.chr, r.pos + refShift + i); i += 1
            }
          }
          refShift += numBases
          readShift += numBases
          outputRow()
          refpos += numBases
          varPos = readShift
        case 'N' =>
          refShift += numBases
          refpos += numBases
          varPos = readShift
        case 'S' =>
          readShift += numBases
          varPos = readShift
        case 'H' =>
          /* do nothing */
        case 'D' =>
          if (useRef) {
            var i = 0
            while (i < numBases) {
              ref += refSeq.getBase(r.chr, r.pos + refShift + i); i += 1
            }
          }
          refShift += numBases
          outputRow()
          refpos += numBases
          varPos = readShift
        case 'I' =>
          ref = if (outputBases) "" else refSeq.getBase(r.chr, r.pos + refShift - 1).toString
          alt += ref.toUpperCase + seqBases.subSequence(readShift, readShift + numBases)
          readShift += numBases
          refpos -= 1
          outputRow()
          refpos += 1
          varPos = readShift
        case 'P' =>
          outputRow()
        case _ =>
          throw new GorDataException("Unknown symbol in cigar string: " + cigar, cigarCol, this.getHeader(), r.toString)
      }
    }
  }
}