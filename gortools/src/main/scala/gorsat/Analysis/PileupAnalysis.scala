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

import org.gorpipe.model.gor.RowObj.BinaryHolder
import gorsat.Commands.{Analysis, BinAggregator, BinAnalysis, BinFactory, BinInfo, BinState, Processor, RegularBinIDgen, RowHandler}
import org.gorpipe.exceptions.GorDataException
import org.gorpipe.gor.GorSession
import org.gorpipe.model.genome.files.gor.Row
import org.gorpipe.model.gor.RowObj
import org.gorpipe.model.gor.iterators.RefSeq

object GorPileup {

  case class Parameters() {
    var nonReferenceProb = 0.01
    var maxInsert: Int = Int.MaxValue
    var minQual = 0
    var minBaseQual = 1
    var noFilter = true
    var numGTs = 10
    var callSnps = false
    var depthOnly = false
  }

  case class PileupColumns() {
    var iSizeCol = 9
    var seqBasesCol = 10
    var flagCol = 3
    var qualityCol = 4
    var cigarCol = 5
    var baseQualCol = 11
    var ChiCol = 5
    var DelsCol = 11
    var InsCol = 13
  }

  case class baseHolder(var code: Char, var qual: Int, var groupN: Int, var groupID: String) extends BinaryHolder

  val iupacArrayPos: Map[Char, Int] = Map('A' -> 0, 'C' -> 1, 'G' -> 2, 'T' -> 3, 'M' -> 4, 'R' -> 5, 'W' -> 6, 'S' -> 7, 'Y' -> 8, 'K' -> 9)
  val iupacArray = Array('A', 'C', 'G', 'T', 'M', 'R', 'W', 'S', 'Y', 'K')
  val iupacGTArray = Array("A", "C", "G", "T", "AC", "AG", "AT", "CG", "CT", "GT")

  val epsilonArray: Array[Double] = Range(0, 255).map(qual => scala.math.pow(10.0, (qual - 33) / (-10.0))).toArray

  // Count depth for each base type in pileup analysis
  case class pooledPileupState(session: GorSession, grCols: List[Int], pa: Parameters, refSeq: RefSeq) extends BinState {

    case class StatHolder() {
      var As: Int = 0
      var Cs: Int = 0
      var Gs: Int = 0
      var Ts: Int = 0
      var Dels: Int = 0
      var Ins: Int = 0
      var likelihoods: Array[Double] = new Array[Double](iupacArray.length)
      var count = 0
    }

    val nonReferenceProb: Double = pa.nonReferenceProb
    val numGTs: Int = pa.numGTs
    val callSnps: Boolean = pa.callSnps
    val depthOnly: Boolean = pa.depthOnly

    var groupStates = new scala.collection.mutable.ArrayBuffer[StatHolder]
    var groupIDs = new scala.collection.mutable.ArrayBuffer[String]
    var totGroups: Int = -1
    val useGroup: Boolean = if (grCols.nonEmpty) true else false
    var singleStatHolder = StatHolder()
    val notUsedYet = "#"
    var theRefBase: Char = '?'
    val priors = new Array[Double](10)

    var thePos = 0

    def formatDouble(d: Double): String = (d formatted "%1.1f").replace(',', '.')

    def formatDouble4(d: Double): String = (d formatted "%1.4f").replace(',', '.')

    def formatDoubleScientific2(d: Double): String = (d formatted "%1.2e").replace(',', '.')

    def phredScore(d: Double): String = (d formatted "%1.2e").replace(',', '.')

    //def PhredScore(d : Double) = (java.lang.Math.log10(1.0-d)*(-1000.0)).toInt.min(99999)

    def initStatHolder(sh: StatHolder) {
      sh.As = 0
      if (!depthOnly) {
        sh.Cs = 0
        sh.Gs = 0
        sh.Ts = 0
        sh.Dels = 0
        sh.Ins = 0
        var i = 0
        while (i < iupacArray.length) {
          sh.likelihoods(i) = priors(i)
          i += 1
        }
        sh.count = 0
      }
    }

    override
    def initialize(bi: BinInfo): Unit = {
      thePos = bi.sto
      theRefBase = refSeq.getBase(bi.chr, bi.sto)
      var i = 0
      while (i < iupacArray.length) {
        priors(i) = if (i < numGTs) nonReferenceProb / (numGTs - 1) else 0.0; i += 1
      }
      theRefBase.toUpper match {
        case 'A' => priors(0) = 1.0 - nonReferenceProb
        case 'C' => priors(1) = 1.0 - nonReferenceProb
        case 'G' => priors(2) = 1.0 - nonReferenceProb
        case 'T' => priors(3) = 1.0 - nonReferenceProb
        case _ => /* do nothing since the priors are already set to be all equal */
      }
      if (useGroup) {
        groupStates = new scala.collection.mutable.ArrayBuffer[StatHolder]
        groupIDs = new scala.collection.mutable.ArrayBuffer[String]
        totGroups = -1
      } else initStatHolder(singleStatHolder)
    }

    def process(r: Row) {
      var sh: StatHolder = null
      val baseHolder = r.bH.asInstanceOf[baseHolder]
      val base = baseHolder.code
      val qual = baseHolder.qual

      if (useGroup) {
        val groupN = baseHolder.groupN
        if (groupN > totGroups) {
          totGroups = groupN
          while (groupStates.length < totGroups + 1) {
            val tsh = StatHolder()
            initStatHolder(tsh)
            groupStates += tsh
          }
          sh = groupStates(groupN)
          while (groupIDs.length < totGroups + 1) groupIDs += notUsedYet
          groupIDs(groupN) = baseHolder.groupID
        } else {
          sh = groupStates(groupN)
          groupIDs(groupN) = baseHolder.groupID
        }
      } else sh = singleStatHolder

      if (depthOnly) {
        if (base != 'I') sh.As += 1
      } else {
        if (base == 'A') sh.As += 1
        else if (base == 'C') sh.Cs += 1
        else if (base == 'G') sh.Gs += 1
        else if (base == 'T') sh.Ts += 1
        else if (base == 'D') sh.Dels += 1 else if (base == 'I') sh.Ins += 1
      }

      if (callSnps) {

        if (!(base == 'I' || base == 'D')) {
          val epsilon = epsilonArray(qual)
          var g = 0
          while (g < numGTs) {
            var p: Double = 0.0
            if (g < 4) { // Homozygous genotypes
              p = if (base == iupacGTArray(g)(0)) 1.0 - epsilon else epsilon / 3
            } else { // Heterozygous genotypes
              p = if (base == iupacGTArray(g)(0)) 1.0 - epsilon else epsilon / 3
              p = (p + (if (base == iupacGTArray(g)(1)) 1.0 - epsilon else epsilon / 3)) / 2
            }
            sh.likelihoods(g) *= p
            g += 1
          }
        }
        sh.count += 1
        if (sh.count == 20) {
          val sum = sh.likelihoods.sum
          var i = 0
          while (i < sh.likelihoods.length) {
            sh.likelihoods(i) /= sum; i += 1
          }
          sh.count = 0
        }

      } // callSnps
    }

    def sendToNextProcessor(bi: BinInfo, nextProcessor: Processor) {
      val allKeys = if (useGroup) groupIDs.zipWithIndex.toList.sortWith((x, y) => x._1 < y._1) else List(("theOnlyGroup", 0))
      for (key <- allKeys; if key._1 != notUsedYet) {
        var sh: StatHolder = null
        if (useGroup) sh = groupStates(key._2) else sh = singleStatHolder
        val lineBuilder = new StringBuilder
        lineBuilder.append(bi.chr)
        lineBuilder.append('\t')
        lineBuilder.append(bi.sto)
        lineBuilder.append('\t')
        if (useGroup) {
          lineBuilder.append(key._1)
          lineBuilder.append('\t')
        }
        lineBuilder.append(theRefBase)
        lineBuilder.append('\t')

        if (depthOnly) {
          lineBuilder.append(sh.As)
        } else {
          var majorAllele = "?"
          var minorAllele = "?"
          var chiSquare = 0.0
          if (sh.As + sh.Cs + sh.Gs + sh.Ts > 0) {
            val ordAlleles = List(("A", sh.As), ("C", sh.Cs), ("G", sh.Gs), ("T", sh.Ts)).sortWith((x, y) => x._2 > y._2)
            val topAlleles = ordAlleles.filter(x => x._2 > 0).slice(0, 2)
            majorAllele = topAlleles.head._1
            minorAllele = if (topAlleles.length > 1) topAlleles.tail.head._1 else majorAllele
            val lowAlleleCounts = ordAlleles.tail.map(x => x._2)
            val lowAlleleSum = lowAlleleCounts.sum
            val lowAlleleSquare = lowAlleleCounts.foldLeft(0.0) ((x, y) => {
              x + (y - lowAlleleSum / 3.0) * (y - lowAlleleSum / 3.0)
            })
            chiSquare = if (majorAllele == minorAllele) 0.0 else lowAlleleSquare / (lowAlleleSum / 3.0)
          }
          lineBuilder.append(majorAllele)
          lineBuilder.append('\t')
          lineBuilder.append(minorAllele)
          lineBuilder.append('\t')
          lineBuilder.append(formatDouble(scala.math.sqrt(chiSquare)))
          lineBuilder.append('\t')
          lineBuilder.append(sh.As + sh.Cs + sh.Gs + sh.Ts + sh.Dels)
          lineBuilder.append('\t')
          lineBuilder.append(sh.As)
          lineBuilder.append('\t')
          lineBuilder.append(sh.Cs)
          lineBuilder.append('\t')
          lineBuilder.append(sh.Gs)
          lineBuilder.append('\t')
          lineBuilder.append(sh.Ts)
          lineBuilder.append('\t')
          lineBuilder.append(sh.Dels)
          lineBuilder.append('\t')
          lineBuilder.append(sh.Ins)
        }
        if (callSnps) {
          val pTot = sh.likelihoods.sum
          val pCond = sh.likelihoods.map(p => p / pTot)
          val pOrd = iupacArray.zip(pCond).toList.sortWith((x, y) => x._2 > y._2).toArray

          lineBuilder.append('\t')
          lineBuilder.append(pOrd(0)._1)
          lineBuilder.append('\t')
          lineBuilder.append(formatDouble4(pOrd(0)._2))
          val lod = java.lang.Math.log10(pOrd(0)._2 / pOrd(1)._2)
          lineBuilder.append('\t')
          lineBuilder.append(formatDouble(lod))
          lineBuilder.append('\t')
          lineBuilder.append(pOrd(1)._1)
          if (theRefBase.toUpper != pOrd(0)._1) {
            lineBuilder.append("\t1")
          } else lineBuilder.append("\t0")

        } // callSnps

        nextProcessor.process(RowObj(lineBuilder.toString))

      }
    }
  }

  /* Flag Chr Description

  0x0001 the read is paired in sequencing
  0x0002 the read is mapped in a proper pair
  0x0004 the query sequence itself is unmapped
  0x0008 the mate is unmapped
  0x0010 strand of the query (1 for reverse)
  0x0020 strand of the mate
  0x0040 the read is the first read in a pair
  0x0080 the read is the second read in a pair
  0x0100 the alignment is not primary
  0x0200 the read fails platform/vendor quality checks
  0x0400 the read is either a PCR or an optical duplicate

  */

  case class pooledPileupRowHandler(grCols: List[Int], pa: Parameters, columns: PileupColumns) extends RowHandler {
    val maxInsert: Int = pa.maxInsert
    val minQual: Int = pa.minQual
    val minBaseQual: Int = pa.minBaseQual
    val noFilter: Boolean = pa.noFilter

    val binIDgen = RegularBinIDgen(1)
    val base = baseHolder('?', 0, 0, "") // This object is used for every base in the seq to transfer the base-code
    var r1 = RowObj("chr1", 0, "")
    var totGroups: Int = -1
    val useGroup: Boolean = if (grCols.nonEmpty) true else false
    var groupMap = Map.empty[String, Int]
    var baseQualSeq : CharSequence = ""
    var minBaseQualChar: Char = (33 + minBaseQual).toChar
    val grColsArray: Array[Int] = grCols.toArray

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
        throw new GorDataException("cigar error in " + s)
      }
      (num, control, pos + 1)
    }

    def abs(x: Int): Int = if (x < 0) -x else x

    def process(r: Row, BA: BinAggregator) {
      val iSize = scala.math.abs(r.colAsInt(columns.iSizeCol))
      val seqBases = r.colAsString(columns.seqBasesCol)
      val flag = r.colAsInt(columns.flagCol)
      val quality = r.colAsInt(columns.qualityCol)
      val cigar = r.colAsString(columns.cigarCol)
      baseQualSeq = r.colAsString(columns.baseQualCol)

      r1.bH = base
      r1.chr = r.chr

      if ((((flag & 0x0200) == 0 && (flag & 0x0400) == 0 && (flag & 0x0002) == 2 && iSize != 0) || noFilter) && abs(iSize) <= maxInsert && quality >= minQual) {
        var groupN = 0
        if (useGroup) {
          val groupID = r.selectedColumns(grColsArray)
          groupMap.get(groupID) match {
            case Some(x) => groupN = x
            case None =>
              totGroups += 1
              groupN = totGroups
              groupMap += (groupID -> groupN)
          }
          base.groupN = groupN
          base.groupID = groupID
        } else {
          base.groupN = 0
          base.groupID = "theOnlyGroup"
        }

        var readShift = 0
        var refShift = 0
        var ci = 0
        while (ci < cigar.length) {
          val (numBases, cigarControl, nextCi) = parseCigar(cigar, ci)
          ci = nextCi

          cigarControl match {
            case 'M' | 'X' | '=' =>
              var i = 0
              var binID = binIDgen.ID(r.pos + refShift + i)
              var (sta, sto) = binIDgen.StartAndStop(binID)
              while (i < numBases) {
                base.code = seqBases.charAt(readShift + i)
                r1.pos = r.pos + refShift + i
                base.qual = if (readShift + i < baseQualSeq.length) baseQualSeq.charAt(readShift + i) else minBaseQualChar
                if (base.qual >= minBaseQualChar) BA.update(r1, binID, r.chr, sta, sto)
                binID += 1
                sta += 1
                sto += 1
                i += 1
              }
              refShift += numBases
              readShift += numBases
            case 'S' =>
              readShift += numBases
            case 'H' =>
              // TODO: Do nothing here?
              /* readShift += numBases; refShift += numBases */
            case 'D' =>
              var i = 0
              while (i < numBases) {
                val binID = binIDgen.ID(r.pos + refShift + i)
                val (sta, sto) = binIDgen.StartAndStop(binID)
                base.code = 'D'
                r1.pos = r.pos + refShift + i
                BA.update(r1, binID, r.chr, sta, sto)
                i += 1
              }
              refShift += numBases
            case 'N' =>
              refShift += numBases
            case 'I' =>
              val binID = binIDgen.ID(r.pos + refShift)
              val (sta, sto) = binIDgen.StartAndStop(binID)
              base.code = 'I'
              r1.pos = r.pos + refShift
              BA.update(r1, binID, r.chr, sta, sto)
              readShift += numBases
            case 'P' =>
              /* do nothing */
            case _ =>
              throw new GorDataException("unknown symbol in cigar string " + cigar)
          }
        }
      }
    }
  }


  case class pooledPileupFactory(session: GorSession, grCols: List[Int], pa: Parameters, refSeq: RefSeq) extends BinFactory {
    def create: BinState = pooledPileupState(session, grCols, pa, refSeq)
  }

  case class pooledPileup(session: GorSession, grCols: List[Int], pa: Parameters, columns: PileupColumns, span: Int, refSeq: RefSeq) extends
    BinAnalysis(pooledPileupRowHandler(grCols, pa, columns), BinAggregator(pooledPileupFactory(session, grCols, pa, refSeq), span + 50, span)) {

    override def finish: Unit = {
      super.finish

      if (refSeq != null) {
        refSeq.close()
      }
    }
  }

  case class TerminatorValue(var terminateNow: Boolean)

  case class Terminator(t: TerminatorValue) extends Analysis {
    override def process(r: Row) {
      if (t.terminateNow) reportWantsNoMore()
      else super.process(r)
    }
  }
}
