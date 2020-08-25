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
import org.gorpipe.exceptions.GorResourceException
import org.gorpipe.gor.model.Row
import org.gorpipe.model.gor.RowObj

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

case class VarGroupAnalysis(refCol: Int, altCol: Int, valCol: Int, grCols: List[Int], sep: String) extends Analysis {

  case class ColHolder() {
    val values = new ArrayBuffer[String]()
    val alts = new ArrayBuffer[String]()
    var count = 0
  }

  val useGroup = grCols.nonEmpty

  //When the first row at a given position arrives, we hope that it is also the last row for that position and therefore
  //do not load the hash maps, i.e. we are in lazy mode.
  var lazyMode = true

  lazy val refMap = new mutable.HashMap[String, mutable.Map[String, ColHolder]]
  lazy val grColsArray = grCols.toArray

  var lastChr = ""
  var lastPos = -1
  var lastRow: Row =_

  var theColHolder: ColHolder =_

  override def process(r: Row) {
    if (lastRow != null) {
      if (lastChr != r.chr || lastPos != r.pos) {
        flush()
        lazyMode = true
      } else {
        if (lazyMode) {
          addToBuffer(lastRow)
          lazyMode = false
        }
        addToBuffer(r)
      }
    }
    lastRow = r
    lastChr = r.chr
    lastPos = r.pos
  }

  override def finish(): Unit = {
    flush()
  }

  private def flush(): Unit = {
    if (lazyMode) flushRow
    else flushColHolders
  }

  private def flushRow: Unit = {
    val ref = lastRow.colAsString(refCol)
    val groupId = () => lastRow.selectedColumns(grColsArray)
    val addAlts = (sb: StringBuilder) => sb.append(lastRow.colAsString(altCol))
    val valueCol = mapBiAllelic(lastRow.colAsString(valCol).toString)
    val rowToProcess = getRow(ref, groupId, addAlts, valueCol)
    super.process(rowToProcess)
  }

  private def flushColHolders = {
    for ((ref, groupMap) <- refMap.toList.sortBy(_._1)) {
      for ((groupId, colHolder) <- groupMap.toList.sortBy(_._1)) {
        val rowToProcess = getRowFromColHolder(ref, groupId, colHolder)
        super.process(rowToProcess)
        colHolder.values.clear()
        colHolder.alts.clear()
        colHolder.count = 0
      }
      groupMap.clear()
    }
    refMap.clear()
  }

  private def addToBuffer(r: Row): Unit = {
    val colHolder = {
      val ref = r.colAsString(refCol).toString
      val groupMap = {
        refMap.get(ref) match {
          case Some(gm) => gm
          case None =>
            val gm = new mutable.HashMap[String, ColHolder]()
            refMap += (ref -> gm)
            gm
        }
      }
      val groupId = if (useGroup) r.selectedColumns(grColsArray) else "theGroup"
      groupMap.get(groupId) match {
        case Some(ch) => ch
        case None =>
          val ch = ColHolder()
          groupMap += (groupId -> ch)
          ch
      }
    }
    colHolder.values += r.colAsString(valCol).toString
    colHolder.alts += r.colAsString(altCol).toString
    colHolder.count += 1
  }

  /**
    * In the relative genotypes we interpret 0 as other and 1 as the alternative.
    */
  private def getAbsoluteGenotype(relGenoTypes: Array[Byte]): String = {
    val numberOfAltAlleles = relGenoTypes.length
    var unknownCount = 0
    var (former, latter) = (0, 0)
    var idx = numberOfAltAlleles - 1
    while (idx != -1 && latter == 0) {
      relGenoTypes(idx) match {
        case '0' => //Homozygous in the other allele. Nothing to do.
        case '1' => latter = idx + 1
        case '2' =>
          former = idx + 1
          latter = idx + 1
        case '3' => unknownCount += 1
        case _=> throw new GorResourceException("Inconsistent genotypes.", "VARGROUP")
      }
      idx -= 1
    }

    while (idx != -1 && former == 0) {
      relGenoTypes(idx) match {
        case '0' => //Homozygous in the other allele. Nothing to do.
        case '1' => former = idx + 1
        case '2' => throw new GorResourceException("Inconsistent genotypes.", "VARGROUP")
        case '3' => unknownCount += 1
        case _=> throw new GorResourceException("Inconsistent genotypes.", "VARGROUP")
      }
      idx -= 1
    }

    while (idx != -1) {
      relGenoTypes(idx) match {
        case '0' => //Nothing to do
        case '1' => throw new GorResourceException("Inconsistent genotypes.", "VARGROUP")
        case '2' => throw new GorResourceException("Inconsistent genotypes.", "VARGROUP")
        case '3' => unknownCount += 1
        case _=> throw new GorResourceException("Inconsistent genotypes.", "VARGROUP")
      }
      idx -= 1
    }

    if (unknownCount == 0) former + "/" + latter
    else if (unknownCount == numberOfAltAlleles) "./."
    else if (latter == 0 || former == 0) "./" + latter
    else throw new GorResourceException("Inconsistent genotypes.", "VARGROUP")
  }

  private def mergeGenotypes(values: ArrayBuffer[String]): String = {
    if (!values.tail.forall(_.length == values.head.length)) {
      throw new GorResourceException("The value columns must have the same length!", "VARGROUP")
    } else if (values.length == 1) {
      mapBiAllelic(values.head)
    } else values.toArray.map(_.getBytes()).transpose.map(getAbsoluteGenotype).mkString(sep)
  }

  private def mapBiAllelic(value: String): String = {
    value.map({
      case '0' => "0/0"
      case '1' => "0/1"
      case '2' => "1/1"
      case '3' => "./."
      case _ => throw new GorResourceException("Inconsistent genotypes.", "VARGROUP")
    }).mkString(sep)
  }

  private def getRowFromColHolder(ref: String, groupId: String, colHolder: ColHolder) = {
    val mergedGenotypes = mergeGenotypes(colHolder.values)
    val addAlts = (sb: StringBuilder) => {
      val alts = colHolder.alts
      alts.tail.foldLeft(sb.append(alts.head))((inSb, alt) => {
        inSb.append(',')
        inSb.append(alt)
      })
    }
    getRow(ref,() => groupId, addAlts, mergedGenotypes)
  }

  private def getRow(ref: CharSequence, groupCols: () => CharSequence, addAlts: StringBuilder => StringBuilder, valueCol: CharSequence): Row = {
    val sb = new StringBuilder
    sb.append(lastChr)
    sb.append('\t')
    sb.append(lastPos)
    sb.append('\t')
    sb.append(ref)
    sb.append('\t')
    addAlts(sb)
    if (useGroup) {
      sb.append('\t')
      sb.append(groupCols())
    }
    sb.append('\t')
    sb.append(valueCol)
    RowObj.apply(sb)
  }
}
