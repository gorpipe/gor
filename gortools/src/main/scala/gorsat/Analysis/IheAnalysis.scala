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
import org.gorpipe.gor.haplotype.GBasicRulesCalculator
import org.gorpipe.gor.model.Row

object IheAnalysis {

  // All these columns must be found and set to different values.
  case class ColumnOptions() {

    var majAlleleCol: Int = -1
    var secAlleleCol: Int = -1
    var depthCol: Int = -1
    var aCol: Int = -1
    var cCol: Int = -1
    var gCol: Int = -1
    var tCol: Int = -1

    var majAlleleFCol: Int = -1
    var secAlleleFCol: Int = -1
    var depthFCol: Int = -1
    var aFCol: Int = -1
    var cFCol: Int = -1
    var gFCol: Int = -1
    var tFCol: Int = -1

    var majAlleleMCol: Int = -1
    var secAlleleMCol: Int = -1
    var depthMCol: Int = -1
    var aMCol: Int = -1
    var cMCol: Int = -1
    var gMCol: Int = -1
    var tMCol: Int = -1

    var gtCol: Int = -1
    var pgtCol: Int = -1
    var lodCol: Int = -1
    var gt2Col: Int = -1

    var gtFCol: Int = -1
    var pgtFCol: Int = -1
    var lodFCol: Int = -1
    var gt2FCol: Int = -1

    var gtMCol: Int = -1
    var pgtMCol: Int = -1
    var lodMCol: Int = -1
    var gt2MCol: Int = -1
  }

  class IHEContext {
    /*
  IUPAC code Meaning
  A A
  C C
  G G
  T T
  M A or C
  R A or G
  W A or T
  S C or G
  Y C or T
  K G or T
  V A or C or G
  H A or C or T
  D A or G or T
  B C or G or T
  N G or A or T or C
  */

    val A: Byte = 1
    val C: Byte = 2
    val G: Byte = 3
    val T: Byte = 4
    val U: Byte = 0

    val iupacGT = Map('A' -> Array(A, A), 'C' -> Array(C, C), 'G' -> Array(G, G), 'T' -> Array(T, T), 'M' -> Array(A, C),
      'R' -> Array(A, G), 'W' -> Array(A, T), 'S' -> Array(C, G), 'Y' -> Array(C, T), 'K' -> Array(G, T))

    val iupacArrayPos = Map('A' -> 0, 'C' -> 1, 'G' -> 2, 'T' -> 3, 'M' -> 4, 'R' -> 5, 'W' -> 6, 'S' -> 7, 'Y' -> 8, 'K' -> 9)

    val iupacArray = Array('A', 'C', 'G', 'T', 'M', 'R', 'W', 'S', 'Y', 'K')

    val iupacCodes = iupacGT.keys
    var allCodes: List[String] = Nil
    for (c <- iupacCodes; p <- iupacCodes; m <- iupacCodes) allCodes ::= (("" + c) + p) + m

    val brc = new GBasicRulesCalculator
    brc.setUnknownAllele(U)

    val trioIHEarray = new Array[Int](1000)
    allCodes.foreach(t => {
      val (child, father, mother) = (t(0), t(1), t(2))
      val inheritanceStatus = brc.checkBasicRules(iupacGT(child), iupacGT(father), iupacGT(mother))
      if (inheritanceStatus == GBasicRulesCalculator.INCOMPATIBLE_GENOTYPES) {
        trioIHEarray(iupacArrayPos(child) + iupacArrayPos(father) * 10 + iupacArrayPos(mother) * 100) = 1
      } else {
        trioIHEarray(iupacArrayPos(child) + iupacArrayPos(father) * 10 + iupacArrayPos(mother) * 100) = 0
      }
    })

    type freqEst = Array[Double]

    def inheritanceErrorProb(childProb: freqEst, fatherProb: freqEst, motherProb: freqEst): Double = {
      var iheSum = 0.0
      var nonIheSum = 0.0
      var i = 0
      while (i < trioIHEarray.length) {
        val cpos = i % 10
        val fpos = (i / 10) % 10
        val mpos = (i / 100) % 10
        val prod = childProb(cpos) * fatherProb(fpos) * motherProb(mpos)
        nonIheSum += (1.0 - trioIHEarray(i)) * prod
        i += 1
      }
      return nonIheSum
    }

    def gtArray(maj: Char, sec: Char): Array[Byte] = {
      val l = if (maj < sec) maj else sec
      val r = if (maj < sec) sec else maj
      if (l == 'A' && r == 'A') return Array(A, A)
      if (l == 'A' && r == 'C') return Array(A, C)
      if (l == 'A' && r == 'G') return Array(A, G)
      if (l == 'A' && r == 'T') return Array(A, T)
      if (l == 'C' && r == 'C') return Array(C, C)
      if (l == 'C' && r == 'G') return Array(C, G)
      if (l == 'C' && r == 'T') return Array(C, T)
      if (l == 'G' && r == 'G') return Array(G, G)
      if (l == 'G' && r == 'T') return Array(G, T)
      return Array(T, T)
    }

    def probGT(As: Int, Cs: Int, Gs: Int, Ts: Int): freqEst = {
      val s: Double = As + Cs + Gs + Ts
      val (pA, pC, pG, pT) = (As / s, Cs / s, Gs / s, Ts / s)
      return Array(pA * pA, pC * pC, pG * pG, pT * pT, 2.0 * pA * pC, 2.0 * pA * pG, 2.0 * pA * pT, 2.0 * pC * pG, 2.0 * pC * pT, 2.0 * pG * pT)
    }

    def prob2GT(gt: Char, pGT: Double, lod: Double, gt2: Char): freqEst = {
      val pGT2 = pGT * scala.math.pow(10.0, -lod)
      var r = (1.0 - pGT - pGT2).max(0.0)
      val pSum = pGT + pGT2 + 8 * r
      r = r / pSum
      val pA = new Array[Double](10)
      var g = 0
      while (g < 10) {
        if (iupacArray(g) == gt) pA(g) = pGT / pSum
        else if (iupacArray(g) == gt2) pA(g) = pGT2 / pSum
        else pA(g) = r
        g += 1
      }
      return pA
    }
  }

  case class IHEs(columnOptions: ColumnOptions) extends Analysis {

    val iheContext = new IHEContext

    def fd(d: Double) = (d formatted "%1.1f").replace(',', '.')

    def fd4(d: Double) = (d formatted "%1.4f").replace(',', '.')

    def fe2(d: Double) = (d formatted "%1.2e").replace(',', '.')

    override def process(r: Row) {

      val majAlleleC = r.colAsString(columnOptions.majAlleleCol).charAt(0)
      val secAlleleC = r.colAsString(columnOptions.secAlleleCol).charAt(0)
      val deptC = r.colAsInt(columnOptions.depthCol)
      val aC = r.colAsInt(columnOptions.aCol)
      val cC = r.colAsInt(columnOptions.cCol)
      val gC = r.colAsInt(columnOptions.gCol)
      val tC = r.colAsInt(columnOptions.tCol)

      val majAlleleF = r.colAsString(columnOptions.majAlleleFCol).charAt(0)
      val secAlleleF = r.colAsString(columnOptions.secAlleleFCol).charAt(0)
      val deptF = r.colAsInt(columnOptions.depthFCol)
      val aF = r.colAsInt(columnOptions.aFCol)
      val cF = r.colAsInt(columnOptions.cFCol)
      val gF = r.colAsInt(columnOptions.gFCol)
      val tF = r.colAsInt(columnOptions.tFCol)

      val majAlleleM = r.colAsString(columnOptions.majAlleleMCol).charAt(0)
      val secAlleleM = r.colAsString(columnOptions.secAlleleMCol).charAt(0)
      val deptM = r.colAsInt(columnOptions.depthMCol)
      val aM = r.colAsInt(columnOptions.aMCol)
      val cM = r.colAsInt(columnOptions.cMCol)
      val gM = r.colAsInt(columnOptions.gMCol)
      val tM = r.colAsInt(columnOptions.tMCol)


      val dIHE = (iheContext.brc.checkBasicRules(iheContext.gtArray(majAlleleC, secAlleleC), iheContext.gtArray(majAlleleF, secAlleleF),
        iheContext.gtArray(majAlleleM, secAlleleM)) == GBasicRulesCalculator.INCOMPATIBLE_GENOTYPES)


      val gtC = r.colAsString(columnOptions.gtCol).charAt(0)
      val pgtC = r.colAsDouble(columnOptions.pgtCol)
      val lodC = r.colAsDouble(columnOptions.lodCol)
      val gt2C = r.colAsString(columnOptions.gt2Col).charAt(0)

      val gtF = r.colAsString(columnOptions.gtFCol).charAt(0)
      val pgtF = r.colAsDouble(columnOptions.pgtFCol)
      val lodF = r.colAsDouble(columnOptions.lodFCol)
      val gt2F = r.colAsString(columnOptions.gt2FCol).charAt(0)

      val gtM = r.colAsString(columnOptions.gtMCol).charAt(0)
      val pgtM = r.colAsDouble(columnOptions.pgtMCol)
      val lodM = r.colAsDouble(columnOptions.lodMCol)
      val gt2M = r.colAsString(columnOptions.gt2MCol).charAt(0)

      val pNIHE2 = iheContext.inheritanceErrorProb(iheContext.prob2GT(gtC, pgtC, lodC, gt2C), iheContext.prob2GT(gtF, pgtF, lodF, gt2F), iheContext.prob2GT(gtM, pgtM, lodM, gt2M))

      super.process(r.rowWithAddedColumn((if (dIHE) 1 else 0) + "\t" + fe2(pNIHE2)))
    }
  }
}
