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

package gorsat.parser

import gorsat.parser.FunctionSignature._
import gorsat.parser.FunctionTypes.{dFun, iFun, sFun}
import gorsat.parser.ParseUtilities._
import gorsat.process.GorJavaUtilities
import org.gorpipe.exceptions.GorParsingException

object GenomeFunctions {
  def register(functions: FunctionRegistry): Unit = {
    functions.register("CHARS2PRPRPR", FunctionSignature.getSignatureString2String(chars2prprpr), chars2prprpr _)
    functions.register("CHARS2PRPR", FunctionSignature.getSignatureString2String(chars2prpr), chars2prpr _)
    functions.register("CHAR2PR", getSignatureString2String(char2pr), char2pr _)
    functions.register("PR2CHAR", getSignatureDouble2String(pr2char), pr2char _)
    functions.register("PRPR2CHARS", getSignatureString2String(prpr2chars), prpr2chars _)
    functions.register("PRPR2CHARS", getSignatureStringString2String(prpr2charsWithSeparator), prpr2charsWithSeparator _)
    functions.register("PRPRPR2CHARS", getSignatureString2String(prprpr2chars), prprpr2chars _)
    functions.register("PRPRPR2CHARS", getSignatureStringString2String(prprpr2charsWithSeparator), prprpr2charsWithSeparator _)
    functions.register("CHARS2PRHOM", getSignatureString2Double(chars2prhom), chars2prhom _)
    functions.register("CHARS2PRHET", getSignatureString2Double(chars2prhet), chars2prhet _)
    functions.register("CHARS2DOSE", getSignatureString2Double(chars2dose), chars2dose _)
    functions.register("REVCOMPL", getSignatureString2String(revcompl), revcompl _)
    functions.register("RC", getSignatureString2String(revcompl), revcompl _)
    functions.register("IUPAC2GT", getSignatureString2String(iupac2gt), iupac2gt _)
    functions.register("REVCIGAR", getSignatureString2String(revCigar), revCigar _)
    functions.register("CODONS2AMINOS", getSignatureString2String(codons2aminos), codons2aminos _)
    functions.register("CODONS2SHORTAMINOS", getSignatureString2String(codons2shortAminos), codons2shortAminos _)
    functions.register("VCFGTITEM", getSignatureStringString2String(vcfGtItem), vcfGtItem _)
    functions.register("VARSIG", getSignatureStringString2String(varSignature), varSignature _)
    functions.register("BAMTAG", getSignatureStringString2String(bamTag), bamTag _)
    functions.register("GFFTAG", getSignatureStringString2String(gffTag), gffTag _)
    functions.register("HAPLDIFF", getSignatureStringString2Int(haplDiff), haplDiff _)
    functions.register("IHA", getSignatureStringString2Int(iha), iha _)
    functions.register("VCFFORMATTAG", getSignatureStringStringString2String(vcfFormatTag), vcfFormatTag _)
    functions.register("IUPACGTSTAT", getSignatureStringStringString2String(iupacGtStat), iupacGtStat _)
    functions.register("TAG", getSignatureStringStringString2String(tag), tag _)
    functions.register("IUPACMA", getSignatureStringStringString2String(iupacma), iupacma _)
    functions.register("IUPACFA", getSignatureStringStringString2String(iupacfa), iupacfa _)
    functions.register("IOOA", getSignatureString2Int(iooa), iooa _)
    functions.register("CHARS2GT", getSignatureStringDouble2String(chars2Gt), chars2Gt _)
    functions.register("CHARSPHASED2GT", getSignatureStringDouble2String(charsPhased2Gt), charsPhased2Gt _)
    functions.registerWithOwner("REFBASE", getSignatureStringInt2String(removeOwner(refBase)), refBase _)
    functions.registerWithOwner("REFBASES", getSignatureStringIntInt2String(removeOwner(refBases)), refBases _)
    functions.registerWithOwner("GTFA", "String:Int:String:String:Int:String:String:Int:String:String2String", gtfa _ )
    functions.registerWithOwner("GTMA", "String:Int:String:String:Int:String:String:Int:String:String2String", gtma _ )
    functions.registerWithOwner("GTSTAT", "String:Int:String:String:Int:String:String:Int:String:String2String", gtstat _ )
    functions.registerWithOwner("GTSHARE", "String:Int:String:String:Int:String:String2Int", gtshare _ )
  }

  def gtfa(owner: ParseArith, ex1: sFun, ex2: iFun, ex3: sFun, ex4: sFun, ex5: iFun, ex6: sFun, ex7: sFun, ex8: iFun, ex9: sFun, ex10: sFun): sFun = {
    line => {
      ParseUtilities.fatherGTVCF(ex2(line), ex3(line), ex4(line), ex5(line), ex6(line), ex7(line), ex8(line), ex9(line), ex10(line), owner.refSeq, ex1(line))
    }
  }

  def gtma(owner: ParseArith, ex1: sFun, ex2: iFun, ex3: sFun, ex4: sFun, ex5: iFun, ex6: sFun, ex7: sFun, ex8: iFun, ex9: sFun, ex10: sFun): sFun = {
    line => {
      ParseUtilities.motherGTVCF(ex2(line), ex3(line), ex4(line), ex5(line), ex6(line), ex7(line), ex8(line), ex9(line), ex10(line), owner.refSeq, ex1(line))
    }
  }

  def gtstat(owner: ParseArith, ex1: sFun, ex2: iFun, ex3: sFun, ex4: sFun, ex5: iFun, ex6: sFun, ex7: sFun, ex8: iFun, ex9: sFun, ex10: sFun): sFun = {
    line => {
      ParseUtilities.gtStatVCF(ex2(line), ex3(line), ex4(line), ex5(line), ex6(line), ex7(line), ex8(line), ex9(line), ex10(line), owner.refSeq, ex1(line))
    }
  }

  def gtshare(owner: ParseArith, ex1: sFun, ex2: iFun, ex3: sFun, ex4: sFun, ex5: iFun, ex6: sFun, ex7: sFun): iFun = {
    line => {
      allelesFoundVCF(ex2(line), ex3(line), ex4(line), ex5(line), ex6(line), ex7(line), owner.refSeq, ex1(line))
    }
  }

  def refBases(owner: ParseArith, ex1: sFun, ex2: iFun, ex3: iFun): sFun = {
    cvp => {
      owner.refSeq.getBases(ex1(cvp), ex2(cvp), ex3(cvp))
    }
  }

  def refBase(owner: ParseArith, ex1: sFun, ex2: iFun): sFun = {
    cvp => {
      owner.refSeq.getBase(ex1(cvp), ex2(cvp)).toString
    }
  }

  def charsPhased2Gt(ex1: sFun, ex2: dFun): sFun = {
    cvp => {
      val c = ex1(cvp).charAt(0)
      if (c == ' ') "3"
      else {
        val pfalt = GorJavaUtilities.pArray(c)
        val pmalt = GorJavaUtilities.pArray(ex1(cvp).charAt(1))
        val p0 = (1.0 - pfalt) * (1.0 - pmalt)
        val p1 = (1.0 - pfalt) * pmalt + pfalt * (1.0 - pmalt)
        val p2 = pfalt * pmalt
        val thres = ex2(cvp)
        if (p2 >= thres) "2" else if (p1 >= thres) "1" else if (p0 >= thres) "0" else "3"
      }
    }
  }

  def chars2Gt(ex1: sFun, ex2: dFun): sFun = {
    cvp => {
      val c = ex1(cvp).charAt(0)
      if (c == ' ') "3"
      else {
        val p1 = GorJavaUtilities.pArray(c)
        val p2 = GorJavaUtilities.pArray(ex1(cvp).charAt(1))
        val p0 = 1.0 - p1 - p2
        val thres = ex2(cvp)
        if (p2 >= thres) "2" else if (p1 >= thres) "1" else if (p0 >= thres) "0" else "3"
      }
    }
  }

  def iooa(ex: sFun): iFun = {
    val orderMap = scala.collection.mutable.Map.empty[String, Int]
    var counter = 0
    cvp => {
      val k = ex(cvp)
      orderMap.get(k) match {
        case Some(x) => x;
        case None =>
          counter += 1; orderMap += (k -> counter); counter
      }
    }
  }

  def iupacma(ex1: sFun, ex2: sFun, ex3: sFun): sFun = {
    cvp => {
      motherGT(ex1(cvp), ex2(cvp), ex3(cvp))
    }
  }

  def iupacfa(ex1: sFun, ex2: sFun, ex3: sFun): sFun = {
    cvp => {
      fatherGT(ex1(cvp), ex2(cvp), ex3(cvp))
    }
  }

  def tag(ex1: sFun, ex2: sFun, ex3: sFun): sFun = {
    cvp => {
      genTag(ex1(cvp), ex2(cvp), ex3(cvp))
    }
  }

  def iupacGtStat(ex1: sFun, ex2: sFun, ex3: sFun): sFun = {
    cvp => {
      gtStat(ex1(cvp), ex2(cvp), ex3(cvp))
    }
  }

  private def getFieldIndex(fieldValue: String, formatValue: String) = {
    var columnIndex = 0
    var start = 0
    var nextColonPos: Int = getEndOfField(formatValue, start)
    while (start < formatValue.length && formatValue.substring(start, nextColonPos) != fieldValue) {
      start = nextColonPos + 1
      nextColonPos = getEndOfField(formatValue, start)
      columnIndex += 1
    }
    if (start >= formatValue.length) {
      columnIndex = -1
    }

    columnIndex
  }

  private def getEndOfField(formatValue: String, start: Int) = {
    val ix = formatValue.indexOf(':', start)
    val nextColonPos = if (ix > -1) ix else formatValue.length
    nextColonPos
  }

  private def getFieldValue(formatValue: String, fieldIndex: Int) = {
    var columnIndex = 0
    var start = 0
    var nextColonPos = getEndOfField(formatValue, start)
    while (nextColonPos + 1 < formatValue.length && columnIndex < fieldIndex) {
      start = nextColonPos + 1
      nextColonPos = getEndOfField(formatValue, start)
      columnIndex += 1
    }
    if (columnIndex == fieldIndex) {
      formatValue.substring(start, nextColonPos)
    } else {
      ""
    }
  }

  def vcfFormatTag(format: sFun, value: sFun, field: sFun): sFun = {
    cvp =>
      {
        val fieldIndex = getFieldIndex(field(cvp), format(cvp))
        if (fieldIndex >= 0) {
          getFieldValue(value(cvp), fieldIndex)
        } else {
          "NOT_FOUND"
        }
    }
  }

  def iha(ex1: sFun, ex2: sFun): iFun = {
    cvp => {
      ParseUtilities.IHA(ex1(cvp), ex2(cvp))
    }
  }

  def haplDiff(ex1: sFun, ex2: sFun): iFun = {
    cvp => {
      val (a, b) = (ex1(cvp), ex2(cvp))
      var s = 0
      var i = 0
      while (i < a.length.min(b.length)) {
        if (a.charAt(i) != b.charAt(i)) s += 1
        i += 1
      }
      if (a.length != b.length) s += (a.length - b.length).max(b.length - a.length)
      s
    }
  }

  def gffTag(ex1: sFun, ex2: sFun): sFun = {
    cvp => {
      ParseUtilities.gffTag(ex1(cvp), ex2(cvp))
    }
  }
  def bamTag(ex1: sFun, ex2: sFun): sFun = {
    cvp => {
      ParseUtilities.bamTag(ex1(cvp), ex2(cvp))
    }
  }

  def varSignature(ex1: sFun, ex2: sFun): sFun = {
    cvp => {
      ParseUtilities.varSignature(ex1(cvp), ex2(cvp))
    }
  }

  def vcfGtItem(ex1: sFun, ex2: sFun): sFun = {
    cvp => {
      val gt = ex1(cvp).split("""/|\|""").map(_.toInt)
      val items = ex2(cvp).split(',')
      try {
        items((gt(0) * (gt(0) + 1) / 2) + gt(1))
      } catch {
        case _: Exception => throw new GorParsingException("Error in VCFGTITEM - index " + gt.toList + " out of range for " + ex2(cvp) + ": ")
      }
    }
  }

  def codons2shortAminos(ex: sFun): sFun = {
    cvp => {
      ParseUtilities.codons2aminos(ex(cvp), longFormat = false)
    }
  }

  def codons2aminos(ex: sFun): sFun = {
    cvp => {
      ParseUtilities.codons2aminos(ex(cvp))
    }
  }

  def revCigar(ex: sFun): sFun = {
    cvp => {
      ParseUtilities.revCigar(ex(cvp))
    }
  }

  def iupac2gt(ex: sFun): sFun = {
    cvp => {
      ParseUtilities.iupac2GT(ex(cvp))
    }
  }

  def revcompl(ex: sFun): sFun = {
    cvp => {
      ParseUtilities.reverseComplement(ex(cvp))
    }
  }

  def chars2prprpr(ex: sFun): sFun = {
    cvp => {
      val c = ex(cvp).charAt(0)
      if (c == ' ') "0;0;0"
      else {
        val c2 = ex(cvp).charAt(1)
        GorJavaUtilities.prprprFunction.get(c,c2)
      }
    }
  }

  def chars2prpr(ex: sFun): sFun = {
    cvp => {
      val c = ex(cvp).charAt(0)
      if (c == ' ') "0;0;0" // we should possibly throw an error here
      else {
        val c2 = ex(cvp).charAt(1)
        GorJavaUtilities.prprFunction.get(c,c2)
      }
    }
  }

  def char2pr(ex: sFun): sFun = {
    cvp => {
      val c = ex(cvp).charAt(0) // we should possibly throw an error here
      if (c == ' ') "0;0;0" else GorJavaUtilities.prArray(c)
    }
  }

  def pr2char(ex: dFun): sFun = {
    cvp => {
      (Math.round((1.0 - ex(cvp)) * 93.0) + 33).toChar.toString
    }
  }

  def prpr2chars(ex: sFun): sFun = {
    cvp => {
      val pp = ex(cvp).split(';')
      (Math.round((1.0 - pp(0).toDouble) * 93.0) + 33).toChar.toString + (Math.round((1.0 - pp(1).toDouble) * 93.0) + 33).toChar.toString
    }
  }

  def prprpr2chars(ex: sFun): sFun = {
    cvp => {
      val pp = ex(cvp).split(';')
      val p0 = pp(0).toDouble
      val p1 = pp(1).toDouble
      val p2 = pp(2).toDouble
      if (p1 == 0.0 && p2 == 0.0 && p0 == 0.0) "  " // two spaces mean 0;0;0
      else Math.round(((1.0 - p1) * 93.0) + 33).toChar.toString + Math.round(((1.0 - p2) * 93.0) + 33).toChar.toString
    }
  }

  def prpr2charsWithSeparator(ex1: sFun, ex2:sFun): sFun = {
    cvp => {
      val sep = ex2(cvp)
      val pp = ex1(cvp).split(sep)
      (Math.round((1.0 - pp(0).toDouble) * 93.0) + 33).toChar.toString + (Math.round((1.0 - pp(1).toDouble) * 93.0) + 33).toChar.toString
    }
  }

  def prprpr2charsWithSeparator(ex1: sFun, ex2:sFun): sFun = {
    cvp => {
      val sep = ex2(cvp)
      val pp = ex1(cvp).split(sep)
      val p0 = pp(0).toDouble
      val p1 = pp(1).toDouble
      val p2 = pp(2).toDouble
      if (p1 == 0.0 && p2 == 0.0 && p0 == 0.0) "  " // two spaces mean 0;0;0
      else {
        val ch1 = Math.round(((1.0 - p1) * 93.0) + 33).toChar
        val ch2 = Math.round(((1.0 - p2) * 93.0) + 33).toChar
        s"$ch1$ch2"
      }
    }
  }

  def chars2prhom(ex: sFun): dFun = {
    cvp => {
      val c = ex(cvp).charAt(0)
      if (c == ' ') 0.0 else GorJavaUtilities.pArray(c)
    }
  }

  def chars2prhet(ex: sFun): dFun = {
    cvp => {
      val c = ex(cvp).charAt(1)
      if (c == ' ') 0.0 else GorJavaUtilities.pArray(c)
    }
  }

  def chars2dose(ex: sFun): dFun = {
    cvp => {
      val c0 = ex(cvp).charAt(0)
      val c1 = ex(cvp).charAt(1)
      if (c0 == ' ') 0.0 else GorJavaUtilities.pArray(c0) + 2 * GorJavaUtilities.pArray(c1)
    }
  }
}
