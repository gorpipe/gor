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

import java.util.zip.DataFormatException

import gorsat.Commands.Analysis
import gorsat.parser.ParseUtilities.{cvsSplitArray, generateVarSeq, mergedReference}
import org.gorpipe.gor.GorConstants
import org.gorpipe.gor.model.Row
import org.gorpipe.gor.session.GorSession
import org.gorpipe.model.gor.RowObj
import org.gorpipe.model.gor.iterators.RefSeq

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

case class MergeGenotypes(refCol: Int, alleleCol: Int, seg: Boolean, header: String, normalize: Boolean, mergeSpan: Int, session: GorSession) extends Analysis {
  private val rangeChrStart = GorConstants.FIRST_POSSIBLE_CHROMOSOME_VALUE
  var rangeChr: String = rangeChrStart
  var rangeStopPos: Int = -1
  var rangeStartPos: Int = -1
  var refseq = ""
  var allRows = new ArrayBuffer[Row]
  var minStartPos: Int = 0
  val hCols: Int = header.split("\t").length
  val maxCols: Int = refCol.max(alleleCol) + 1
  val colArray: Array[Int] = Range(maxCols, hCols).toArray

  var refSeq: RefSeq = session.getProjectContext.createRefSeq()

  override def isTypeInformationMaintained: Boolean = true

  def normalizeVariant(refSeq: String, startPos: Int, ialleles: String): (String, Int, String) = {

    if (refSeq.length <= 1 || ialleles.length <= 1) return (refSeq, startPos, ialleles)
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

      i = 0
      val tmp = allele.length.min(refSeq.length) - 1
      while (i < tmp && allele(i) == refSeq(i)) i += 1
      val refStart = i - (if (allele(i) != refSeq(i) && allele.length != refSeq.length && i > 0) 1 else 0)
      var refCut = 0

      while (allele.length - refCut > refStart + 1 && refSeq.length - refCut > refStart + 1
        && allele(allele.length - 1 - refCut) == refSeq(refSeq.length - 1 - refCut)) {
        refCut += 1
      }

      val aNewRef = refSeq.slice(refStart, refSeq.length - refCut)
      val aNewPos = startPos + refStart
      val aNewAllele = allele.slice(refStart, allele.length - refCut)
      if (alleleNum > 1) {
        if (aNewRef != newRef || aNewPos != newStartPos) return (refSeq, startPos, ialleles)
        else {
          newAlleles += alleleSep + aNewAllele
        }
      } else {
        newAlleles = aNewAllele
        newRef = aNewRef
        newStartPos = aNewPos
      }
      alleleNum += 1
    })
    (newRef, newStartPos, newAlleles)
  }

  def outputModifiedRows(flushPosition: Int): Unit = {
    val posRowMap = mutable.Map.empty[Int, ArrayBuffer[Row]]
    var normVarMap = mutable.Map.empty[String, (String, Int, String)]

    allRows.foreach(rr => {
      val alleles = rr.colAsString(alleleCol).toString
      val rrRef = rr.colAsString(refCol)
      var newAlleles = ""

      var l: String = null
      var theNewStartPos = -1

      if (normalize && !(rrRef.length == 1 && alleles.length == 1 || rrRef == alleles) || !normalize) {
        /* modify alleles */
        val (nRefseq, nRangeStartPos, nNewAlleles) = {
          val lookup = rrRef + "\t" + rr.pos + "\t" + alleles
          normVarMap.get(lookup) match {
            case Some(normVar) => normVar
            case None =>
              if (rrRef != refseq) {
                val allIndex = cvsSplitArray(alleles)
                var allOne = 0
                while (allOne < allIndex.length) {
                  val allOneStart = if (allOne == 0) 0 else allIndex(allOne - 1) + 1
                  if ((rr.pos - rangeStartPos) < 0) throw new DataFormatException("rangeStartPos < position in the row. Input source most likely in wrong genomic order!!!")
                  newAlleles += generateVarSeq(refseq, rangeStartPos, rrRef.length, alleles, rr.pos, allOneStart, allIndex(allOne))

                  if (allOne + 1 < allIndex.length) newAlleles += alleles(allIndex(allOne)) /* Add the separator */
                  allOne += 1
                }
              } else newAlleles = alleles
              val normVar = if (normalize) normalizeVariant(refseq, rangeStartPos, newAlleles) else (refseq, rangeStartPos, newAlleles)
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
        case Some(ab: ArrayBuffer[Row]) => ab += RowObj(l)
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

  override def process(r: Row): Unit = {
    val aRefseq = r.colAsString(refCol).toString
    val stopPos = r.pos + aRefseq.length
    if (r.chr == rangeChr && r.pos <= rangeStopPos + mergeSpan) {
      // extending
      if (stopPos > rangeStopPos) {
        refseq = mergedReference(rangeStartPos, refseq, r.pos, aRefseq, refSeq, r.chr)
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
      refseq = aRefseq
      allRows = new ArrayBuffer[Row]
    }
    allRows += r

    if (refseq.length > mergeSpan * 2) {
      minStartPos = r.pos
      outputModifiedRows(rangeStartPos + mergeSpan)
      refseq = refseq.slice(minStartPos - rangeStartPos, refseq.length)
      rangeStartPos = minStartPos
    }
  }

  override def finish(): Unit = {
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
