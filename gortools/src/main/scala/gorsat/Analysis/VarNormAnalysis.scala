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
import org.gorpipe.gor.GorConstants
import org.gorpipe.gor.model.Row
import org.gorpipe.gor.session.GorSession
import org.gorpipe.model.gor.RowObj
import org.gorpipe.model.gor.iterators.RefSeq

import scala.collection.mutable.{ArrayBuffer, Map}

case class VarNormAnalysis(refCol: Int, alleleCol: Int, vcfForm: Boolean, seg: Boolean, header: String, leftnormalize: Boolean, mergeSpan: Int, session: GorSession) extends Analysis {
  private val rangeChrStart = GorConstants.FIRST_POSSIBLE_CHROMOSOME_VALUE
  var rangeChr: String = rangeChrStart
  var rangeStopPos: Int = -1
  var rangeStartPos: Int = -1
  var allRows = new ArrayBuffer[Row]
  var minStartPos: Int = 0
  val hCols: Int = header.split("\t").length
  val maxCols: Int = refCol.max(alleleCol) + 1
  val colArray: Array[Int] = Range(maxCols, hCols).toArray

  var refSeq: RefSeq = session.getProjectContext.createRefSeq()

  override def isTypeInformationMaintained: Boolean = true

  def normalizeVariant(chrom: String, pos: Int, ref: String, ialleles: String, leftnormalize: Boolean): (String, Int, String) = {

    var multiAllele = false
    var i = 0
    var alleleSep = ','
    while (i < ialleles.length && !multiAllele) {
      if (ialleles(i) == ',' || ialleles(i) == '/' || ialleles(i) == '|') {
        multiAllele = true; alleleSep = ialleles(i)
      }
      i += 1
    }
    var newStartPos = -1
    var newRef = ""
    var newAlleles = ""
    var alleleNum = 1

    ialleles.split(alleleSep).foreach(allele => {

      var i = 0
      while (allele.length > i && ref.length > i && allele(i) == ref(i)) {
        i += 1
      }
      var j = 0
      while (allele.length - i > j && ref.length - i > j && allele(allele.length - j - 1) == ref(ref.length - j - 1)) {
        j += 1
      }


      var aNewPos = pos + i
      var aNewRef = ref.slice(i, ref.length - j)
      var aNewAllele = allele.slice(i, allele.length - j)

      if (leftnormalize) {
        if (aNewRef.length == 0 && aNewAllele.length > 0) { // Insertion
          var i = 0
          while (i < aNewAllele.length && refSeq.getBase(chrom, aNewPos - i - 1).toUpper == aNewAllele(aNewAllele.length - 1 - i).toUpper) {
            i += 1
          }
          while (i >= aNewAllele.length && refSeq.getBase(chrom, aNewPos - i - 1).toUpper == refSeq.getBase(chrom, aNewPos - i - 1 + aNewAllele.length).toUpper) {
            i += 1
          }
          aNewPos -= i
          if (i >= aNewAllele.length) {
            aNewAllele = refSeq.getBases(chrom, aNewPos, aNewPos + aNewAllele.length - 1).toUpperCase
          } else if (i > 0) {
            aNewAllele = refSeq.getBases(chrom, aNewPos, aNewPos + i - 1).toUpperCase + aNewAllele.slice(0, aNewAllele.length - i).toUpperCase
          }
        } else if (aNewRef.length > 0 && aNewAllele.length == 0) { // Deletion
          var i = 0
          while (i < aNewRef.length && refSeq.getBase(chrom, aNewPos - i - 1).toUpper == aNewRef(aNewRef.length - 1 - i).toUpper) {
            i += 1
          }
          while (i >= aNewRef.length && refSeq.getBase(chrom, aNewPos - i - 1).toUpper == refSeq.getBase(chrom, aNewPos - i - 1 + aNewRef.length).toUpper) {
            i += 1
          }
          aNewPos -= i
          if (i >= aNewRef.length) {
            aNewRef = refSeq.getBases(chrom, aNewPos, aNewPos + aNewRef.length - 1).toUpperCase
          } else if (i > 0) {
            aNewRef = refSeq.getBases(chrom, aNewPos, aNewPos + i - 1).toUpperCase + aNewRef.slice(0, aNewRef.length - i).toUpperCase
          }
        }
      } else { // Right-normalize
        if (aNewRef.length == 0 && aNewAllele.length > 0) { // Insertion
          var i = 0

          while (i < aNewAllele.length && refSeq.getBase(chrom, aNewPos + i).toUpper == aNewAllele(i).toUpper) {
            i += 1
          }
          while (i >= aNewAllele.length && refSeq.getBase(chrom, aNewPos + i - aNewAllele.length).toUpper == refSeq.getBase(chrom, aNewPos + i).toUpper) {
            i += 1
          }
          aNewPos += i
          if (i >= aNewAllele.length) {
            aNewAllele = refSeq.getBases(chrom, aNewPos - aNewAllele.length, aNewPos - 1).toUpperCase
          } else if (i > 0) {
            aNewAllele = aNewAllele.slice(i, aNewAllele.length).toUpperCase + refSeq.getBases(chrom, aNewPos - i, aNewPos - 1).toUpperCase
          }
        } else if (aNewRef.length > 0 && aNewAllele.length == 0) { // Deletion
          var i = 0
          while (i < aNewRef.length && refSeq.getBase(chrom, aNewPos + i + aNewRef.length).toUpper == aNewRef(i).toUpper) {
            i += 1
          }
          while (i >= aNewRef.length && refSeq.getBase(chrom, aNewPos + i + aNewRef.length).toUpper == refSeq.getBase(chrom, aNewPos + i).toUpper) {
            i += 1
          }
          aNewPos += i
          if (i >= aNewRef.length) {
            aNewRef = refSeq.getBases(chrom, aNewPos - aNewRef.length, aNewPos - 1).toUpperCase
          } else if (i > 0) {
            aNewRef = aNewRef.slice(i, aNewRef.length).toUpperCase + refSeq.getBases(chrom, aNewPos - i + aNewRef.length, aNewPos - 1 + aNewRef.length).toUpperCase
          }
        }
      }
      var addBase = false
      if (alleleNum > 1) {
        if (aNewRef != newRef || aNewPos != newStartPos) return (ref, pos, ialleles)
        else {
          newAlleles += alleleSep + (if (addBase) refSeq.getBase(chrom, aNewPos - 1).toUpper + aNewAllele else aNewAllele)
        }
      } else {
        addBase = if (vcfForm && (aNewAllele.length == 0 || aNewRef.length == 0)) true else false
        newAlleles = if (addBase) refSeq.getBase(chrom, aNewPos - 1).toUpper + aNewAllele else aNewAllele
        newRef = if (addBase) refSeq.getBase(chrom, aNewPos - 1).toUpper + aNewRef else aNewRef
        newStartPos = if (addBase) aNewPos - 1 else aNewPos
      }
      alleleNum += 1
    })
    (newRef, newStartPos, newAlleles)
  }

  def outputModifiedRows(flushPosition: Int) {
    val posRowMap = Map.empty[Int, ArrayBuffer[Row]]
    var normVarMap = Map.empty[String, (String, Int, String)]

    allRows.foreach(rr => {
      val alleles = rr.colAsString(alleleCol).toString
      val rrRef = rr.colAsString(refCol).toString
      var newAlleles = ""

      var l: String = null
      var theNewStartPos = -1

      if (!(rrRef.length == 1 && alleles.length == 1 || rrRef == alleles)) {
        /* modify alleles */
        val (nRefseq, nRangeStartPos, nNewAlleles) = {
          val lookup = rrRef + "\t" + rr.pos + "\t" + alleles
          normVarMap.get(lookup) match {
            case Some(normVar) => normVar
            case None =>
              newAlleles = alleles
              val normVar = if (leftnormalize) normalizeVariant(rr.chr, rr.pos, rrRef, newAlleles, leftnormalize = true) else normalizeVariant(rr.chr, rr.pos, rrRef, newAlleles, leftnormalize = false)
              normVarMap += (lookup -> normVar)
              normVar
          }
        }
        l = rr.colAsString(0) + "\t" + nRangeStartPos
        var c = 2
        if (seg) {
          l += "\t" + (nRangeStartPos + nRefseq.length)
          c = 3
        }
        while (c < maxCols) {
          if (c == refCol) l += "\t" + nRefseq
          else if (c == alleleCol) l += "\t" + nNewAlleles
          else l += "\t" + rr.colAsString(c)
          c += 1
        }
        if (hCols > maxCols) l += "\t" + rr.selectedColumns(colArray)
        theNewStartPos = nRangeStartPos
      } else {
        theNewStartPos = rr.pos
        l = rr.toString
      }

      posRowMap.get(theNewStartPos) match {
        case Some(ab: ArrayBuffer[Row]) =>
          ab += RowObj(l)
        case None =>
          val ab = new ArrayBuffer[Row]
          ab += RowObj(l)
          //posRowMap += (theNewStartPos -> ab)  // Changed because of a bug in scala  2.9 (https://issues.scala-lang.org/browse/SI-5681)
          posRowMap(theNewStartPos) = ab
      }

    })
    var newAllRows = new ArrayBuffer[Row]
    for (key <- posRowMap.keys.toList.sorted) {
      posRowMap(key).foreach(r => if (flushPosition == -1 || r.pos < flushPosition) super.process(r) else {
        newAllRows += r; minStartPos = minStartPos.min(r.pos)
      })
    }
    allRows = newAllRows
  }


  override def process(r: Row) {
    val aRefseq = r.colAsString(refCol)
    val stopPos = r.pos + aRefseq.length
    if (r.chr == rangeChr && r.pos <= rangeStopPos + mergeSpan) {
      // extending
      if (stopPos > rangeStopPos) {
        rangeStopPos = stopPos
      }
    } else {
      // See if we need to output existing range
      if (rangeChr != rangeChrStart) {
        outputModifiedRows(-1)
      }
      rangeChr = r.chr
      rangeStartPos = r.pos
      rangeStopPos = stopPos
      allRows = new ArrayBuffer[Row]
    }
    allRows += r

    if (r.pos > rangeStartPos + mergeSpan * 2) {
      minStartPos = r.pos
      outputModifiedRows(rangeStartPos + mergeSpan)
      rangeStartPos = minStartPos
    }
  }

  override def finish() {
    // See if we need to output existing range
    try {
      if (rangeChr != rangeChrStart) {
        outputModifiedRows(-1)
      }
    } finally {
      refSeq.close()
    }
  }
}
