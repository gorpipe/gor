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

// gorsatParse.scala
// (c) deCODE genetics
// 30th August. 2011, Hakon Gudbjartsson

package gorsat.parser

import java.util.Locale

import org.gorpipe.exceptions._
import org.gorpipe.model.util.GChiSquared2by2
import gorsat.process.PipeOptions
import org.gorpipe.exceptions.{GorDataException, GorParsingException, GorSystemException}
import org.gorpipe.gor.GorContext
import org.gorpipe.model.gor.iterators.RefSeq
import org.gorpipe.model.util.GFisherExact2by2
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.language.{implicitConversions, reflectiveCalls}


object ParseUtilities {

  private val logger = LoggerFactory.getLogger(this.getClass)

  /**
    * This method calculates 2-tailed P value
    * @param minCase
    * @param majCase
    * @param minCtrl
    * @param majCtrl
    * @return Double
    */
  def twoByTwoPvalTwoTailed(minCase: Int, majCase: Int, minCtrl: Int, majCtrl: Int): Double = {
    if (minCase < 10 || majCase < 10 || minCtrl < 10 || majCtrl < 10) {
      GFisherExact2by2.computeTwoTailed(minCase, majCase, minCtrl, majCtrl)
    } else {
      val chisq = GChiSquared2by2.computeLogLikelihoodChiSquared(minCase, majCase, minCtrl, majCtrl)
      GChiSquared2by2.getPValue(chisq)
    }
  }

  /**
    * This method calculates a one tailed P value, taking a minor case, major case, minor control and major control as parameters and returning a P-value.
    * @param minCase
    * @param majCase
    * @param minCtrl
    * @param majCtrl
    * @return Double
    */
  def twoByTwoPvalOneTailed(minCase: Int, majCase: Int, minCtrl: Int, majCtrl: Int): Double = {
    if (minCase < 10 || majCase < 10 || minCtrl < 10 || majCtrl < 10) {
      GFisherExact2by2.computeOneTailed(minCase, majCase, minCtrl, majCtrl)
    } else {
      val chisq = GChiSquared2by2.computeLogLikelihoodChiSquared(minCase, majCase, minCtrl, majCtrl)
      GChiSquared2by2.getPValue(chisq) / 2.0
    }
  }

  /**
    * given a nucleotide string, tag string, and delimiter term, returns a tagged substring based on the position of the term.
    * @param str the string of tags
    * @param tag the tag
    * @param term term used to delimit the tags
    * @return
    */
  def genTag(str: String, tag: String, term: String): String = {
    var res = ""
    val start = if( str.startsWith(tag+"=") ) 0 else {
      val st = str.indexOf(term+tag+"=")
      if (st == -1) return "NOT_FOUND"
      st+1
    }
    val s2 = str.substring(start,str.length)
    val stop = s2.indexOf(term)
    if (stop == -1) {
      res = str.substring(start+(tag+"=").length,str.length)
    } else {
      res = str.substring(start+(tag+"=").length,stop+start)
    }
    if (res == "") {
      throw new GorDataException(s"$tag is missing value, check file data: ")
    }
    res
  }

  /**
    * given a string and tag string, returns a BAM string delimited by a " "
    * @param str
    * @param tag
    * @return
    */
  def bamTag(str: String, tag: String): String = genTag(str, tag, " ")

  /**
    * given a string and a tag string, returns a GFF string delimited by a ";"
    * @param str
    * @param tag
    * @return
    */
  def gffTag(str: String, tag: String): String = genTag(str, tag, ";")

  val complementPairsMap = new Array[Char](256)
  for (i <- 1 to complementPairsMap.length) complementPairsMap(i - 1) = '#'
  complementPairsMap('a') = 't'
  complementPairsMap('t') = 'a'
  complementPairsMap('c') = 'g'
  complementPairsMap('g') = 'c'
  complementPairsMap('n') = 'n'
  complementPairsMap('u') = 'u'
  complementPairsMap(')') = '('
  complementPairsMap('A') = 'T'
  complementPairsMap('T') = 'A'
  complementPairsMap('C') = 'G'
  complementPairsMap('G') = 'C'
  complementPairsMap('N') = 'N'
  complementPairsMap('U') = 'U'
  complementPairsMap('(') = ')'

  val iupac2GTs: Map[String, String] = Map.apply("A" -> "AA",
    "C" -> "CC", "G" -> "GG",
    "T" -> "TT", "M" -> "AC",
    "R" -> "AG", "W" -> "AT",
    "S" -> "CG", "Y" -> "CT",
    "K" -> "GT", "N" -> "ACGT",
    "U" -> "ACGT", "" -> "ACGT",
    "-" -> "ACGT", "?" -> "ACGT")

  /**
    * this takes a nucleotide string in IUPAC standard and returns a string of alleles delimited by "/"
    * @param code
    * @return
    */
  def iupac2GT(code: String): String = {
    val alleles = iupac2GTs.apply(code)
    alleles.mkString("/")
  }

  /**
    * this method takes a nucleotide character as an input and returns the corresponding integer code for a single nucleotide
    * @param nucleotide
    * @return
    */
  def nucleotide2integer(nucleotide: Char): Int = nucleotide match {
    case 'A' | 'a' => 0
    case 'C' | 'c' => 1
    case 'G' | 'g' => 2
    case 'T' | 't' => 3
    case _ =>
      throw new GorParsingException("Unknown nucleotide " + nucleotide)
      4
  }

  val codon2aminoacid = List(
    "GCT, GCC, GCA, GCG" -> "Ala/A",
    "CGT, CGC, CGA, CGG, AGA, AGG" -> "Arg/R",
    "AAT, AAC" -> "Asn/N",
    "GAT, GAC" -> "Asp/D",
    "TGT, TGC" -> "Cys/C",
    "CAA, CAG" -> "Gln/Q",
    "GAA, GAG" -> "Glu/E",
    "GGT, GGC, GGA, GGG" -> "Gly/G",
    "CAT, CAC" -> "His/H",
    "ATT, ATC, ATA" -> "Ile/I",
    "ATG" -> "Met/M",
    "TTA, TTG, CTT, CTC, CTA, CTG" -> "Leu/L",
    "AAA, AAG" -> "Lys/K",
    "ATG" -> "Met/M",
    "TTT, TTC" -> "Phe/F",
    "CCT, CCC, CCA, CCG" -> "Pro/P",
    "TCT, TCC, TCA, TCG, AGT, AGC" -> "Ser/S",
    "ACT, ACC, ACA, ACG" -> "Thr/T",
    "TGG" -> "Trp/W",
    "TAT, TAC" -> "Tyr/Y",
    "GTT, GTC, GTA, GTG" -> "Val/V",
    "TAA, TGA, TAG" -> "STO/X")

  val codon2aminoAcidLookupLong = new Array[String](4 * 4 * 4 + 1)
  val codon2aminoAcidLookupShort = new Array[String](4 * 4 * 4 + 1)

  codon2aminoacid.foreach(x => {
    val xx = x._1.split(',').map(_.trim)
    codon2aminoAcidLookupLong(0) = "???"
    codon2aminoAcidLookupShort(0) = "?"
    xx.foreach(t => {
      codon2aminoAcidLookupLong(aminoAcid2Integer(t)) = x._2.slice(0, 3)
      codon2aminoAcidLookupShort(aminoAcid2Integer(t)) = x._2.slice(4, 5)
    })
  })

  val byteArray = new Array[Byte](2000)

  /**
    * this method takes a  runtime stream string as an input and returns a system message
    * @param s
    * @return
    */
  def system(s: String): String = {
    val p = Runtime.getRuntime.exec(s)
    val in = p.getInputStream
    var systemMessage = ""
    try {
      var total = 0
      var r = in.read(byteArray, 0, byteArray.length)
      while (r != -1 && total < byteArray.length) {
        total += r
        r = in.read(byteArray, total, byteArray.length - total)
      }
      if (total == byteArray.length) r = in.read
      systemMessage = new String(byteArray, 0, total).trim
      if (r != -1) systemMessage += "... additional truncated data"
    } catch {
      case e: GorSystemException => systemMessage = e.getMessage
    } finally {
      in.close()
    }
    systemMessage
  }

  /**
    * this method evaluates an input string and returns a system message.
    * @param s
    * @return
    */
  def eval(s: String, context: GorContext): String = {
    val pipeInstance = gorsat.process.PipeInstance.createGorIterator(context)
    pipeInstance.init(s)

    val rs = pipeInstance.getRowSource
    var systemMessage = ""
    try {
      if (rs.hasNext) {
        val nextItems = rs.next().otherCols.split('\t')

        if (nextItems.nonEmpty) {
          systemMessage = nextItems.head.trim
        }
      }
    } catch {
      case e: GorSystemException => systemMessage = e.getMessage
    } finally {
      rs.close()
    }
    systemMessage
  }

  /**
    * This method takes an amino acid string and converts it to an integer value
    * @param amino
    * @return
    */
  def aminoAcid2Integer(amino: String):Int = { 1 + nucleotide2integer(amino(0)) + nucleotide2integer(amino(1)) * 4 + nucleotide2integer(amino(2)) * 16 }

  /**
    * This method takes a codon string and converts it to it's corresponding amino acid.
    * @param codon
    * @param longFormat
    * @return
    */
  def codons2aminos(codon: String, longFormat: Boolean = true): String = {
    val strbuff = new mutable.StringBuilder(codon.length + 1)
    val noCodons = codon.length / 3
    var i = 0
    while (i < noCodons) {
      try {
        val x = aminoAcid2Integer(codon.slice(i * 3, i * 3 + 3))
        if (longFormat) strbuff.append(codon2aminoAcidLookupLong(x)) else strbuff.append(codon2aminoAcidLookupShort(x))
      } catch {
        case _: GorParsingException => if (longFormat) strbuff.append("???") else strbuff.append("?")
      }
      i += 1
    }
    if (codon.length > noCodons * 3) if (longFormat) strbuff.append("???") else strbuff.append("?")
    strbuff.toString
  }

  /**
    * This converts a string parameter to a string interpolated with delimiters based on a a wordSize
    * @param c
    * @param wordSize
    * @param sep
    * @return
    */
  def string2wordString(c: String, wordSize: Int, sep: String): String = {
    val noWords = c.length / wordSize
    val strbuff = new mutable.StringBuilder(c.length + noWords)
    var i = 0
    while (i < noWords) {
      if (i != 0) strbuff.append(sep)
      val x = c.slice(i * wordSize, i * wordSize + wordSize)
      strbuff.append(x)
      i += 1
    }
    if (c.length > noWords * wordSize) strbuff.append((if (i != 0) sep else "") + c.slice(i * wordSize, i * wordSize + wordSize))
    strbuff.toString
  }

  /**
    * this method returns the reverse complement of a nucleotide string, e.g. GGG -> CCC
    * @param str
    * @return
    */
  def reverseComplement(str: String): String = {
    str.reverse.map(x => {
      val temp = complementPairsMap(x); if (temp == '#') x else temp
    })
  }

  /**
    * This method takes a string, performs a reversal operation on the string, and returns a string
    * @param str
    * @return
    */
  def revCigar(str: String): String = {
    var start = 0
    var i = 0
    val ostr = new mutable.StringBuilder(str.length)
    ostr.length = str.length
    while (i < str.length) {
      if (str(i) > '9') {
        var j = start; while (j <= i) {
          ostr.setCharAt(str.length - 1 - i + j - start, str(j)); j += 1
        }; start = i + 1
      }
      i += 1
    }
    ostr.toString
  }

  /**
    * This method takes an IUPAC standard string and an allele string and returns an Int
    * @param iupac IUPAC standard
    * @param allele allele
    * @return
    */
  def IHA(iupac: String, allele: String): Int = {
    if (iupac2GTs(iupac).indexOf(allele) >= 0) 1 else 0
  }

  /**
    * This method takes two ints and returns a parametrized "f" string
    * @param a
    * @param b
    * @return
    */
  def fString(a: Int, b: Int): String = "%" + a + "." + b + "f"

  /**
    * This method takes two ints and returns a parametrized "e" string
    * @param a
    * @param b
    * @return
    */
  def eString(a: Int, b: Int): String = "%" + a + "." + b + "e"

  /**
    * This method takes a double and a string and returns a reformatted string with delimiters replaced.
    *
    * @param d
    * @param f
    * @return
    */
  def formNum(d: java.lang.Double, f: String): String = if (d.isNaN) d.toString else String.format(Locale.ROOT, f, d)

  /**
    * This method takes three strings representing the child, father, and mother nucleotides, and an int representing the father or mother, and returns
    * a nucleotide string
    *
    * @param child
    * @param father
    * @param mother
    * @param fORm
    * @return
    */
  def parentsGT(child: String, father: String, mother: String, fORm: Int): String = {
    try {
      if (child == "A" || child == "C" || child == "G" || child == "T") return child
      val cGT = iupac2GTs(child)
      val fGT = iupac2GTs(father)
      val fGT0 = fGT.indexOf(cGT(0))
      val fGT1 = fGT.indexOf(cGT(1))
      if (fGT0 >= 0 && !(fGT1 >= 0)) return cGT(0).toString
      val mGT = iupac2GTs(mother)
      val mGT0 = mGT.indexOf(cGT(0))
      val mGT1 = mGT.indexOf(cGT(1))
      if (mGT0 >= 0 && !(mGT1 >= 0)) return cGT(1).toString
      return cGT(fORm).toString
    } catch {
      case e: GorSystemException => logger.error(child + father + mother, e); ""
    }
  }

  /**
    * this method takes three nucleotide strings and returns a string representing the father's nucleotide string.
    * @param child
    * @param father
    * @param mother
    * @return
    */
  def fatherGT(child: String, father: String, mother: String): String = parentsGT(child, father, mother, 0)

  /**
    * this method takes three nucleotide strings and returns a string representing the mother's nucleotide string.
    *
    * @param child
    * @param father
    * @param mother
    * @return
    */
  def motherGT(child: String, father: String, mother: String): String = parentsGT(child, mother, father, 1)

  /**
    * This method takes 3 nucleotide strings representing the child, father, and mother, and returns a string representing the phase, 1 or 0.
    *
    * @param child
    * @param father
    * @param mother
    * @return
    */
  def phaseKnown(child: String, father: String, mother: String): String = {
    if (child == "U") return "0"
    if (child == "A" || child == "C" || child == "G" || child == "T") return "1"
    var fHet = true
    if (father == "A" || father == "C" || father == "G" || father == "T") fHet = false
    var mHet = true
    if (mother == "A" || mother == "C" || mother == "G" || mother == "T") mHet = false
    if (fHet && mHet && (father == mother || mother == "U" || father == "U")) return "0" else return "1"
  }

  /**
    * this method takes 3 nucleotide strings from the child, father, and mother, and returns a string representing the state of ihe.
    *
    * @param child
    * @param father
    * @param mother
    * @return
    */
  def ihe(child: String, father: String, mother: String): String = {
    val cGT = iupac2GTs(child)
    val fGT = iupac2GTs(father)
    val mGT = iupac2GTs(mother)

    if (fGT.indexOf(cGT(0)) >= 0 && mGT.indexOf(cGT(1)) >= 0) return "0"
    if (fGT.indexOf(cGT(1)) >= 0 && mGT.indexOf(cGT(0)) >= 0) return "0"
    return "1"
  }

  /**
    * this method takes 3 nucleotide strings from the child, father, and mother, and returns a string representing the gtStat condition.
    *
    * @param iChild
    * @param iFactor
    * @param iMother
    * @return
    */
  def gtStat(iChild: String, iFactor: String, iMother: String): String = {
    val c = if (iChild == "") "U" else iChild
    val f = if (iFactor == "") "U" else iFactor
    val m = if (iMother == "") "U" else iMother
    if (ihe(c, f, m) == "1") return "0"
    else if (phaseKnown(c, f, m) == "1") return "2"
    else return "1"
  }

  def varSignature(ref: String, alt: String): String = {
    var i = 0
    var a = 0
    var g = 0
    var c = 0
    var t = 0
    while (i < alt.length) {
      if (alt(i) == 'A') a += 1 else if (alt(i) == 'G') g += 1 else if (alt(i) == 'C') c += 1 else if (alt(i) == 'T') t += 1
      i += 1
    }
    i = 0
    while (i < ref.length) {
      if (ref(i) == 'A') a -= 1 else if (ref(i) == 'G') g -= 1 else if (ref(i) == 'C') c -= 1 else if (ref(i) == 'T') t -= 1
      i += 1
    }
    return "A" + a + "C" + c + "G" + g + "T" + t
  }

  def generateVarSeq(refSeq: String, refPos: Int, allRefSize: Int, alleles: String, allelePos: Int, alleleStart: Int, alleleStop: Int): String = {
    val alleleLength = alleleStop - alleleStart
    val startDiff = allelePos - refPos
    val seqLength = refSeq.length.max(allRefSize) + alleleLength - allRefSize
    val insSize = alleleLength - allRefSize
    val bases = new mutable.StringBuilder(seqLength)
    var p = 0
    while (p < startDiff) {
      bases.append(refSeq(p)); p += 1
    }
    while (p < alleleLength + startDiff) {
      bases.append(alleles(p - startDiff + alleleStart)); p += 1
    }
    while (p < seqLength) {
      bases.append(refSeq(p - insSize)); p += 1
    }
    return bases.toString
  }

  def sameVarSeqs(varSeq1: String, refSeq: String, refPos: Int, allRefSize: Int, alleles: String, allelePos: Int, alleleStart: Int, allleleStop: Int): Boolean = {
    val alleleLength = allleleStop - alleleStart
    val startDiff = allelePos - refPos
    val seqLength = refSeq.length.max(allRefSize) + alleleLength - allRefSize
    val insSize = alleleLength - allRefSize
    var p = 0
    var p1 = 0
    val vl = varSeq1.length - 1
    while (p < startDiff) {
      if (p1 > vl || varSeq1(p1) != refSeq(p)) return false; p += 1; p1 += 1
    }
    while (p < alleleLength + startDiff) {
      if (p1 > vl || varSeq1(p1) != alleles(p - startDiff + alleleStart)) return false; p += 1; p1 += 1
    }
    while (p < seqLength) {
      if (p1 > vl || varSeq1(p1) != refSeq(p - insSize)) return false; p += 1; p1 += 1
    }
    if (p1 != varSeq1.length) return false
    else return true
  }

  def mergedReference(pos1: Int, ref1: String, pos2: Int, ref2: String, refSeq: RefSeq, theChrom: String): String = {
    val start = pos1.min(pos2)
    val stop = (pos1 + ref1.length - 1).max(pos2 + ref2.length - 1)
    val seq = new mutable.StringBuilder(stop - start + 1)
    seq.length = stop - start + 1
    /*
    var p = 0
    while (p < ref1.length) { seq.setCharAt(p+pos1-start,ref1(p)); p += 1 }
    p = 0
    while (p < ref2.length) { seq.setCharAt(p+pos2-start,ref2(p)); p += 1 }
    */
    var p = start
    while (p <= stop) {
      if (p - pos1 >= 0 && p - pos1 < ref1.length) seq.setCharAt(p - start, ref1(p - pos1))
      else if (p - pos2 >= 0 && p - pos2 < ref2.length) seq.setCharAt(p - start, ref2(p - pos2))
      else if (refSeq == null) seq.setCharAt(p - start, 'N')
      else seq.setCharAt(p - start, refSeq.getBase(theChrom, p).toUpper)
      p += 1
    }
    return seq.toString
  }

  def cvsSplitArray(s: String): Array[Int] = {
    var i = 0
    var n = 0
    var cols = 1
    while (i < s.length) {
      if (s(i) == ',' || s(i) == '/' || s(i) == '|') cols += 1
      i += 1
    }
    i = 0
    n = 0
    val seps = new Array[Int](cols)
    while (i < s.length && n < cols) {
      if (s(i) == ',' || s(i) == '/' || s(i) == '|') {
        seps(n) = i; n += 1
      }
      i += 1
    }
    seps(n) = i
    seps
  }

  def compareVCFgt(pos1: Int, ref1: String, alleles1: String, pos2: Int, ref2: String, alleles2: String, refSeq: RefSeq, theChrom: String): Int = {
    /* Make reference comparison true only if the exact same representation.  This works only for single allele notation, i.e. no comma-sep. list of alleles */
    if (ref1 == alleles1) {
      if (ref2 == alleles2 && pos1 == pos2) return 1 else return 0
    }
    if (ref1.length == 1 && alleles1.length == 1 && ref2.length == 1 && alleles2.length == 1) {
      if (pos1 == pos2 && alleles1 == alleles2) return 1 else return 0
    }
    if (pos1 == pos2 && alleles1 == alleles2 && ref1 == ref2) return 1
    val allIndex1 = cvsSplitArray(alleles1)
    val allIndex2 = cvsSplitArray(alleles2)
    val mergePos = pos1.min(pos2)
    val mergedRef = mergedReference(pos1, ref1, pos2, ref2, refSeq, theChrom)
    var ssame = 0
    var tests = 0
    var allOne = 0
    while (allOne < allIndex1.length) {
      val allOneStart = if (allOne == 0) 0 else allIndex1(allOne - 1) + 1
      var allTwo = 0
      var nsame = 0
      while (allTwo < allIndex2.length) {
        val allTwoStart = if (allTwo == 0) 0 else allIndex2(allTwo - 1) + 1
        val varSeq1 = generateVarSeq(mergedRef, mergePos, ref1.length, alleles1, pos1, allOneStart, allIndex1(allOne))
        if (sameVarSeqs(varSeq1, mergedRef, mergePos, ref2.length, alleles2, pos2, allTwoStart, allIndex2(allTwo))) nsame += 1
        allTwo += 1
        tests += 1
      }
      allOne += 1
      if (nsame > 0) ssame += 1
    }
    if (tests > 2 && ssame > 1) return 2
    if (tests == 2 && ssame > 1) return 1
    if (ssame > 0) return 1
    return 0
  }

  def allelesFoundVCF(pos1: Int, ref1: String, alleles1: String, pos2: Int, ref2: String, alleles2: String, refSeq: RefSeq, theChrom: String): Int = {
    if (pos2 > pos1 + 10000 || pos1 > pos2 + 10000) return 0
    if (((pos2 > pos1 && pos1 + ref1.length < pos2) || (pos1 > pos2 && pos2 + ref2.length < pos1)) && ref1 == alleles1 && ref2 == alleles2) return 0
    return compareVCFgt(pos1, ref1, alleles1, pos2, ref2, alleles2, refSeq, theChrom)
  }

  def gtStatVCF(posC: Int, refC: String, allelesC: String, posF: Int, refF: String, allelesF: String, posM: Int, refM: String, allelesM: String, refSeq: RefSeq, theChrom: String): String = {
    val childAlleles = allelesC.split("[,|/]")
    if (childAlleles.length == 1) {
      val foundInFather = allelesFoundVCF(posC, refC, allelesC, posF, refF, allelesF, refSeq, theChrom)
      val foundInMother = allelesFoundVCF(posC, refC, allelesC, posM, refM, allelesM, refSeq, theChrom)
      if (foundInFather + foundInMother >= 2) return "2" else return "0"
    } else {
      val foundInFather1 = allelesFoundVCF(posC, refC, childAlleles(0), posF, refF, allelesF, refSeq, theChrom)
      val foundInMother1 = allelesFoundVCF(posC, refC, childAlleles(0), posM, refM, allelesM, refSeq, theChrom)
      val foundInFather2 = allelesFoundVCF(posC, refC, childAlleles(1), posF, refF, allelesF, refSeq, theChrom)
      val foundInMother2 = allelesFoundVCF(posC, refC, childAlleles(1), posM, refM, allelesM, refSeq, theChrom)

      if (allelesC.indexOf('|') > -1 && ((foundInFather1 > 0 && foundInMother2 > 9) || (foundInFather2 > 0 && foundInMother1 > 0))) return "2"
      if (foundInFather1 > 0 && foundInMother1 == 0 && foundInMother2 > 0) return "2"
      if (foundInFather2 > 0 && foundInMother2 == 0 && foundInMother1 > 0) return "2"
      if (foundInMother1 > 0 && foundInFather1 == 0 && foundInFather2 > 0) return "2"
      if (foundInMother2 > 0 && foundInFather2 == 0 && foundInFather1 > 0) return "2"
      if (foundInFather1 + foundInMother1 >= 2 && foundInFather2 + foundInMother2 >= 2) return "1"
      return "0"
    }
  }

  def fatherGTVCF(posC: Int, refC: String, allelesC: String, posF: Int, refF: String, allelesF: String, posM: Int, refM: String, allelesM: String, refSeq: RefSeq, theChrom: String): String = {
    val childAlleles = allelesC.split("[,|/]")
    if (childAlleles.length == 1) {
      return childAlleles(0)
    } else {
      if (allelesC.indexOf('|') > -1) return childAlleles(0)
      val foundInFather1 = allelesFoundVCF(posC, refC, childAlleles(0), posF, refF, allelesF, refSeq, theChrom)
      val foundInMother1 = allelesFoundVCF(posC, refC, childAlleles(0), posM, refM, allelesM, refSeq, theChrom)
      val foundInFather2 = allelesFoundVCF(posC, refC, childAlleles(1), posF, refF, allelesF, refSeq, theChrom)
      val foundInMother2 = allelesFoundVCF(posC, refC, childAlleles(1), posM, refM, allelesM, refSeq, theChrom)
      if (foundInFather1 > 0 && !(foundInMother1 > 0)) return childAlleles(0)
      if (foundInFather2 > 0 && !(foundInMother2 > 0)) return childAlleles(1)
      var motherAllele = -1
      if (foundInMother1 > 0 && !(foundInFather1 > 0)) motherAllele = 0
      if (foundInMother2 > 0 && !(foundInFather2 > 0)) motherAllele = 1
      if (motherAllele != 0) return childAlleles(0) else if (motherAllele != 1) return childAlleles(1)
      return childAlleles(0)
    }
  }

  def motherGTVCF(posC: Int, refC: String, allelesC: String, posF: Int, refF: String, allelesF: String, posM: Int, refM: String, allelesM: String, refSeq: RefSeq, theChrom: String): String = {
    val childAlleles = allelesC.split("[,|/]")
    if (childAlleles.length == 1) {
      return childAlleles(0)
    } else {
      if (allelesC.indexOf('|') > -1) return childAlleles(1)
      val foundInFather1 = allelesFoundVCF(posC, refC, childAlleles(0), posF, refF, allelesF, refSeq, theChrom)
      val foundInMother1 = allelesFoundVCF(posC, refC, childAlleles(0), posM, refM, allelesM, refSeq, theChrom)
      val foundInFather2 = allelesFoundVCF(posC, refC, childAlleles(1), posF, refF, allelesF, refSeq, theChrom)
      val foundInMother2 = allelesFoundVCF(posC, refC, childAlleles(1), posM, refM, allelesM, refSeq, theChrom)

      if (foundInMother1 > 0 && !(foundInFather1 > 0)) return childAlleles(0)
      if (foundInMother2 > 0 && !(foundInFather2 > 0)) return childAlleles(1)
      var fatherAllele = -1
      if (foundInFather1 > 0 && !(foundInMother1 > 0)) fatherAllele = 0
      if (foundInFather2 > 0 && !(foundInMother2 > 0)) fatherAllele = 1
      if (fatherAllele != 1) return childAlleles(1) else if (fatherAllele != 0) return childAlleles(0)
      return childAlleles(1)
    }
  }
}