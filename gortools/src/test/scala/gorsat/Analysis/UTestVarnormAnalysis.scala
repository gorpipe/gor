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

import gorsat.{TestUtils => GorTestUtils}
import gorsat.Iterators.RowListIterator
import gorsat.Outputs.ToList
import gorsat.process.GenericGorRunner
import org.gorpipe.gor.model.Row
import org.gorpipe.gor.session.{GorSession, ProjectContext}
import org.gorpipe.model.gor.RowObj
import org.gorpipe.model.gor.iterators.RefSeq
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.{anyInt, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar

import scala.collection.mutable.ListBuffer

/**
 * Tests for VarNormAnalysis using a mocked RefSeq so no real reference genome is required.
 * Each test that exercises indel shifting sets up its own RefSeq stubs inline.
 */
@RunWith(classOf[JUnitRunner])
class UTestVarnormAnalysis extends AnyFunSuite with MockitoSugar with BeforeAndAfterEach {

  val header    = "Chrom\tPos\tRef\tAlt"
  val refCol    = 2
  val alleleCol = 3

  val mockSession        : GorSession     = mock[GorSession]
  val mockProjectContext : ProjectContext = mock[ProjectContext]
  val mockRefSeq         : RefSeq         = mock[RefSeq]

  when(mockSession.getProjectContext).thenReturn(mockProjectContext)
  when(mockProjectContext.createRefSeq()).thenReturn(mockRefSeq)

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockRefSeq)
  }

  def runVarNorm(rows: List[Row],
                 leftnormalize: Boolean = true,
                 vcfForm: Boolean = true,
                 seg: Boolean = false,
                 mergeSpan: Int = 1000): List[Row] = {
    val out    = new ListBuffer[Row]
    val runner = new GenericGorRunner()
    runner.run(
      RowListIterator(rows),
      VarNormAnalysis(refCol, alleleCol, vcfForm, seg, header, leftnormalize, mergeSpan, mockSession) | ToList(out)
    )
    out.toList
  }

  private def mockRefSeqFrom(startPos: Int, ref: String): Unit = {
    // Serve single-base and interval lookups from a contiguous reference window.
    doAnswer(invocation => {
      val pos = invocation.getArgument[Int](1)
      ref.charAt(pos - startPos)
    }).when(mockRefSeq).getBase(eqTo("chr1"), anyInt())

    doAnswer(invocation => {
      val start = invocation.getArgument[Int](1)
      val end = invocation.getArgument[Int](2)
      ref.substring(start - startPos, end - startPos + 1)
    }).when(mockRefSeq).getBases(eqTo("chr1"), anyInt(), anyInt())
  }

  private def mockRefSeqFrom(ref: String): Unit = mockRefSeqFrom(1, ref)

  // ---- tests that require no refSeq calls ----

  test("SNP - passthrough, no refSeq needed") {
    val input = List(RowObj("chr1\t1000\tA\tT"))
    assert(runVarNorm(input) == input)
  }

  test("Identical ref and alt - passthrough, no refSeq needed") {
    val input = List(RowObj("chr1\t1000\tACGT\tACGT"))
    assert(runVarNorm(input) == input)
  }

  test("MNP trims common prefix only - no refSeq needed") {
    // ref="CAG", alt="CAC" → trim prefix "CA" and suffix "" → SNP G→C at pos 1002
    val input    = List(RowObj("chr1\t1000\tCAG\tCAC"))
    val expected = List(RowObj("chr1\t1002\tG\tC"))
    assert(runVarNorm(input) == expected)
  }

  test("MNP trims common N prefix only - no refSeq needed") {
    // ref="CAG", alt="CAC" → trim prefix "CA" and suffix "" → SNP G→C at pos 1002
    val input    = List(RowObj("chr1\t1000\tNNG\tNNC"))
    val expected = List(RowObj("chr1\t1002\tG\tC"))
    assert(runVarNorm(input) == expected)
  }

  test("MNP trims common prefix and suffix to SNP - no refSeq needed") {
    // ref="ACGT", alt="ACTT" → trim prefix "AC", suffix "T" → SNP G→T at pos 1002
    val input    = List(RowObj("chr1\t1000\tACGT\tACTT"))
    val expected = List(RowObj("chr1\t1002\tG\tT"))
    assert(runVarNorm(input) == expected)
  }

  test("MNP trims common N prefix and suffix to SNP - no refSeq needed") {
    // ref="ACGT", alt="ACTT" → trim prefix "AC", suffix "T" → SNP G→T at pos 1002
    val input    = List(RowObj("chr1\t1000\tNNGN\tNNTN"))
    val expected = List(RowObj("chr1\t1002\tG\tT"))
    assert(runVarNorm(input) == expected)
  }

  // ---- simple position shifts via prefix/suffix trimming (SNPs and MNPs, no indel sliding) ----
  // Left and right flags behave identically for non-indels — only parsimony applies.

  test("Prefix trim only - position shifts right by 1") {
    // ref="AG", alt="AT" → prefix 'A' removed → SNP G→T at pos 1001
    val input    = List(RowObj("chr1\t1000\tAG\tAT"))
    val expected = List(RowObj("chr1\t1001\tG\tT"))
    assert(runVarNorm(input) == expected)
  }

  test("Suffix trim only - position unchanged, ref and alt shortened") {
    // ref="GA", alt="TA" → suffix 'A' removed → SNP G→T at pos 1000
    val input    = List(RowObj("chr1\t1000\tGA\tTA"))
    val expected = List(RowObj("chr1\t1000\tG\tT"))
    assert(runVarNorm(input) == expected)
  }

  test("Prefix and suffix trimmed - position shifts right by prefix length") {
    // ref="ACGT", alt="ATGT" → prefix 'A', suffix 'GT' removed → SNP C→T at pos 1001
    val input    = List(RowObj("chr1\t1000\tACGT\tATGT"))
    val expected = List(RowObj("chr1\t1001\tC\tT"))
    assert(runVarNorm(input) == expected)
  }

  test("Right-normalize flag - identical to left for SNPs, no indel sliding") {
    // ref="ACG", alt="ATG" → prefix 'A', suffix 'G' removed → SNP C→T at pos 1001
    // -right flag makes no difference for substitutions
    val input    = List(RowObj("chr1\t1000\tACG\tATG"))
    val expected = List(RowObj("chr1\t1001\tC\tT"))
    assert(runVarNorm(input, leftnormalize = false) == expected)
  }

  test("N as common prefix - trimmed away, position shifts right") {
    // ref="NA", alt="NC" → prefix 'N' removed → SNP A→C at pos 1001
    val input    = List(RowObj("chr1\t1000\tNA\tNC"))
    val expected = List(RowObj("chr1\t1001\tA\tC"))
    assert(runVarNorm(input) == expected)
  }

  test("Multi-char N prefix - both Ns trimmed, position shifts right by 2") {
    // ref="NNA", alt="NNC" → prefix "NN" removed → SNP A→C at pos 1002
    val input    = List(RowObj("chr1\t1000\tNNA\tNNC"))
    val expected = List(RowObj("chr1\t1002\tA\tC"))
    assert(runVarNorm(input) == expected)
  }

  test("N as common suffix - trimmed, position unchanged") {
    // ref="GN", alt="TN" → suffix 'N' removed → SNP G→T at pos 1000
    val input    = List(RowObj("chr1\t1000\tGN\tTN"))
    val expected = List(RowObj("chr1\t1000\tG\tT"))
    assert(runVarNorm(input) == expected)
  }

  test("N in ref after prefix trim - N is the substituted base") {
    // ref="AN", alt="AT" → prefix 'A' removed → SNP N→T at pos 1001
    val input    = List(RowObj("chr1\t1000\tAN\tAT"))
    val expected = List(RowObj("chr1\t1001\tN\tT"))
    assert(runVarNorm(input) == expected)
  }

  test("N stops prefix match - shorter prefix trimmed, N left in ref") {
    // ref="ANCA", alt="ATCA" → prefix 'A' trimmed (stops because 'N'≠'T'), suffix 'CA' trimmed
    // → SNP N→T at pos 1001
    val input    = List(RowObj("chr1\t1000\tANCA\tATCA"))
    val expected = List(RowObj("chr1\t1001\tN\tT"))
    assert(runVarNorm(input) == expected)
  }

  test("N in both prefix and suffix - both trimmed, inner bases exposed") {
    // ref="NANG", alt="NCNG" → prefix 'N', suffix 'NG' trimmed → SNP A→C at pos 1001
    val input    = List(RowObj("chr1\t1000\tNANG\tNCNG"))
    val expected = List(RowObj("chr1\t1001\tA\tC"))
    assert(runVarNorm(input) == expected)
  }

  test("N in middle of MNP ref, prefix and suffix trimmed around it") {
    // ref="ACNGT", alt="ACTGT" → prefix "AC", suffix "GT" trimmed → SNP N→T at pos 1002
    val input    = List(RowObj("chr1\t1000\tACNGT\tACTGT"))
    val expected = List(RowObj("chr1\t1002\tN\tT"))
    assert(runVarNorm(input) == expected)
  }

  test("Right-normalize with N in ref - same parsimony trimming as left") {
    // ref="TNA", alt="TCA" → prefix 'T', suffix 'A' trimmed → SNP N→C at pos 1001
    val input    = List(RowObj("chr1\t1000\tTNA\tTCA"))
    val expected = List(RowObj("chr1\t1001\tN\tC"))
    assert(runVarNorm(input, leftnormalize = false) == expected)
  }

  // ---- right-normalize insertion ----

  test("Right-normalize insertion - VCF form, shifts by 1") {
    // ref="A", alt="AC" at 1000
    // Trim: prefix 'A' removed → insertion "C" at 1001
    // Right-norm: refSeq at 1001 = 'C' matches → shift to 1002
    // VCF anchor 'C' at 1001 → ref="C", alt="CC" at 1001
    mockRefSeqFrom(1001, "CA")

    val input    = List(RowObj("chr1\t1000\tA\tAC"))
    val expected = List(RowObj("chr1\t1001\tC\tCC"))
    assert(runVarNorm(input, leftnormalize = false) == expected)
  }

  // ---- left-normalize insertion ----

  test("Left-normalize insertion - VCF form, shifts by 1") {
    // ref="C", alt="CC" at 3001
    // Trim: prefix 'C' removed → insertion "C" at 3002
    // Left-norm: refSeq at 3001 = 'C' matches → shift to 3001
    // VCF anchor 'T' at 3000 → ref="T", alt="TC" at 3000
    mockRefSeqFrom(3000, "TC")

    val input    = List(RowObj("chr1\t3001\tC\tCC"))
    val expected = List(RowObj("chr1\t3000\tT\tTC"))
    assert(runVarNorm(input) == expected)
  }

  // ---- right-normalize deletion ----

  test("Right-normalize deletion - VCF form, no shift when downstream base differs") {
    // ref="ATT", alt="A" at 2000
    // Trim: prefix 'A' removed → deletion "TT" at 2001
    // Right-norm: refSeq at 2003 = 'G', does not match 'T' → no shift
    // VCF anchor 'A' at 2000 → ref="ATT", alt="A" at 2000 (unchanged)
    when(mockRefSeq.getBase("chr1", 2003)).thenReturn('G')
    when(mockRefSeq.getBase("chr1", 2000)).thenReturn('A')

    val input = List(RowObj("chr1\t2000\tATT\tA"))
    assert(runVarNorm(input, leftnormalize = false) == input)
  }

  // ---- left-normalize deletion ----

  test("Left-normalize deletion - VCF form, shifts by 3 into upstream repeat") {
    // ref="TTA", alt="A" at 5001
    // Trim: suffix 'A' removed → deletion "TT" at 5001
    // Left-norm: upstream TTT repeat at 4998-5000 causes 3-step shift
    // VCF anchor 'C' at 4997 → ref="CTT", alt="C" at 4997
    mockRefSeqFrom(4997, "CTTT")

    val input    = List(RowObj("chr1\t5001\tTTA\tA"))
    val expected = List(RowObj("chr1\t4997\tCTT\tC"))
    assert(runVarNorm(input) == expected)
  }

  // ---- vcfForm = true with empty ref input (anchor base injected from refSeq) ----
  // When input ref is empty (GOR-native insertion) and vcfForm=true, the code adds
  // an anchor base from refSeq at aNewPos-1 and shifts the output position left by 1.

  test("Empty ref, vcfForm=true, left-norm, no shift - anchor added at pos-1") {
    // ref="", alt="G" at 10001; refSeq at 10000='A' does not match 'G' → no sliding shift
    // Anchor 'A' injected at 10000 → ref="A", alt="AG" at 10000
    when(mockRefSeq.getBase("chr1", 10000)).thenReturn('A')

    val input    = List(RowObj("chr1\t10001\t\tG"))
    val expected = List(RowObj("chr1\t10000\tA\tAG"))
    assert(runVarNorm(input) == expected)
  }

  test("Empty ref, vcfForm=true, left-norm, shift by 1 - anchor added after shift") {
    // ref="", alt="C" at 10101; refSeq at 10100='C' matches → inner loop shifts by 1
    // outer loop stops (10099='A' ≠ 10100='C'); anchor 'A' at 10099
    // → ref="A", alt="AC" at 10099
    mockRefSeqFrom(10099, "AC")

    val input    = List(RowObj("chr1\t10101\t\tC"))
    val expected = List(RowObj("chr1\t10099\tA\tAC"))
    assert(runVarNorm(input) == expected)
  }

  test("Empty ref, vcfForm=true, right-norm, no shift - anchor added at pos-1") {
    // ref="", alt="A" at 10200; refSeq at 10200='T' does not match 'A' → no sliding shift
    // Anchor 'C' from refSeq at 10199 → ref="C", alt="CA" at 10199
    when(mockRefSeq.getBase("chr1", 10200)).thenReturn('T')
    when(mockRefSeq.getBase("chr1", 10199)).thenReturn('C')

    val input    = List(RowObj("chr1\t10200\t\tA"))
    val expected = List(RowObj("chr1\t10199\tC\tCA"))
    assert(runVarNorm(input, leftnormalize = false) == expected)
  }

  test("Empty ref, vcfForm=true, right-norm, shift by 1 - anchor at shifted pos-1") {
    // ref="", alt="G" at 10300; refSeq at 10300='G' matches → inner loop shifts by 1
    // outer loop stops (10300='G' ≠ 10301='T'); anchor 'G' at 10300
    // → ref="G", alt="GG" at 10300
    mockRefSeqFrom(10300, "GT")

    val input    = List(RowObj("chr1\t10300\t\tG"))
    val expected = List(RowObj("chr1\t10300\tG\tGG"))
    assert(runVarNorm(input, leftnormalize = false) == expected)
  }

  test("Empty ref, vcfForm=true, N in alt stops left shift - anchor still added") {
    // ref="", alt="N" at 10400; inner loop stops immediately (N in alt)
    // Anchor 'A' from refSeq at 10399 → ref="A", alt="AN" at 10399
    when(mockRefSeq.getBase("chr1", 10399)).thenReturn('A')

    val input    = List(RowObj("chr1\t10400\t\tN"))
    val expected = List(RowObj("chr1\t10399\tA\tAN"))
    assert(runVarNorm(input) == expected)
  }

  // ---- trim mode (vcfForm = false) ----

  test("Right-normalize insertion - trim mode, shifts by 1 without anchor base") {
    // ref="", alt="G" at 6000 (GOR native format, no VCF anchor)
    // Right-norm: refSeq at 6000 = 'G' matches → shift to 6001
    // No VCF anchor added → ref="", alt="G" at 6001
    mockRefSeqFrom(6000, "GA")

    val input    = List(RowObj("chr1\t6000\t\tG"))
    val expected = List(RowObj("chr1\t6001\t\tG"))
    assert(runVarNorm(input, leftnormalize = false, vcfForm = false) == expected)
  }

  test("Left-normalize deletion - trim mode, shifts into upstream repeat") {
    // ref="TT", alt="" at 5001 (GOR native format)
    // Left-norm: refSeq at 5000 = 'T', 4999 = 'T', 4998 = 'T' → shifts fully into repeat
    // then 4997='C' != getBase at 4997+2=4999='T' → stops; aNewRef updated from getBases
    // No VCF anchor → ref="TT", alt="" at 4998
    mockRefSeqFrom(4997, "CTTT")

    val input    = List(RowObj("chr1\t5001\tTT\t"))
    val expected = List(RowObj("chr1\t4998\tTT\t"))
    assert(runVarNorm(input, vcfForm = false) == expected)
  }

  // ---- N in variant alleles (inner-loop N checks) ----

  test("MNP with N in alt - trims to N SNP, no refSeq needed") {
    // ref="ACG", alt="ACN" → trim prefix "AC" → SNP G→N at pos 8002
    // N in alt is treated as an uncertain base; parsimony still trims the common prefix
    val input    = List(RowObj("chr1\t8000\tACG\tACN"))
    val expected = List(RowObj("chr1\t8002\tG\tN"))
    assert(runVarNorm(input) == expected)
  }

  test("Left-normalize insertion - N in alt stops inner loop, no shift") {
    // ref="C", alt="CN" at 7000 → trim → insertion "N" at 7001
    // Inner loop: allele char is 'N' → `!= 'N'` fails at i=0 → no shift
    // VCF anchor 'C' at 7000 → output unchanged
    when(mockRefSeq.getBase("chr1", 7000)).thenReturn('C')

    val input = List(RowObj("chr1\t7000\tC\tCN"))
    assert(runVarNorm(input) == input)
  }

  test("Left-normalize deletion - N in ref stops inner loop, no shift") {
    // ref="CN", alt="C" at 7100 → trim → deletion "N" at 7101
    // Inner loop: ref char is 'N' → `!= 'N'` fails at i=0 → no shift
    // VCF anchor 'C' at 7100 → output unchanged
    when(mockRefSeq.getBase("chr1", 7100)).thenReturn('C')

    val input = List(RowObj("chr1\t7100\tCN\tC"))
    assert(runVarNorm(input) == input)
  }

  test("Left-normalize deletion - N at end of deleted ref, trim mode - no refSeq call, no shift") {
    // ref="TN", alt="" at 9300 (trim mode)
    // Inner loop: ref(1)='N' → `!= 'N'` fails at i=0, no getBase call made
    when(mockRefSeq.getBase("chr1", 9300)).thenReturn('T')  // would be called if guard didn't fire

    val input = List(RowObj("chr1\t9300\tTN\t"))
    assert(runVarNorm(input, vcfForm = false) == input)
  }

  test("Left-normalize insertion - N at end of inserted alt, trim mode - no refSeq call, no shift") {
    // ref="", alt="TN" at 9400 (trim mode)
    // Inner loop: allele(1)='N' → `!= 'N'` fails at i=0, no getBase call made
    val input = List(RowObj("chr1\t9400\t\tTN"))
    assert(runVarNorm(input, vcfForm = false) == input)
  }

  test("VCF deletion - N at end of deleted sequence stops shift, anchor from refSeq") {
    // ref="ATN", alt="A" at 9000 → trim → deletion "TN" at 9001
    // Inner loop: ref(1)='N' → stops at i=0; anchor = getBase(9000)='A'
    // Output unchanged (no shift even though surrounding bases could match)
    when(mockRefSeq.getBase("chr1", 9000)).thenReturn('A')

    val input = List(RowObj("chr1\t9000\tATN\tA"))
    assert(runVarNorm(input) == input)
  }

  test("VCF deletion - N at start of deleted sequence, partial left-shift, N preserved in ref") {
    // ref="ANT", alt="A" at 9100 → trim → deletion "NT" at 9101
    // Inner: ref(1)='T'!='N', getBase(9100)='T'==ref(1) → i=1; then ref(0)='N' → stop
    // Partial shift by 1: aNewRef = getBases(9100,9100)+"N" = "TN"; anchor = getBase(9099)='C'
    // Output: ref="CTN", alt="C" at 9099 — N is preserved in the shifted allele
    mockRefSeqFrom(9099, "CT")

    val input    = List(RowObj("chr1\t9100\tANT\tA"))
    val expected = List(RowObj("chr1\t9099\tCTN\tC"))
    assert(runVarNorm(input) == expected)
  }

  test("VCF insertion - N as ref anchor is replaced by actual refSeq base") {
    // ref="N", alt="NC" at 9200 → trim → insertion "C" at 9201
    // Left-norm: getBase(9200)='A' ≠ 'C' → no shift
    // VCF anchor = getBase(9200)='A'; N anchor is discarded, real base used instead
    // Output: ref="A", alt="AC" at 9200
    when(mockRefSeq.getBase("chr1", 9200)).thenReturn('A')

    val input    = List(RowObj("chr1\t9200\tN\tNC"))
    val expected = List(RowObj("chr1\t9200\tA\tAC"))
    assert(runVarNorm(input) == expected)
  }

  test("Right-normalize insertion - N in alt treated as ordinary base, no N guard") {
    // ref="", alt="NA" at 9500 (trim mode, right-norm)
    // Right-norm has NO N guard; getBase(9500)='T' ≠ 'N' → stops at i=0, no shift
    when(mockRefSeq.getBase("chr1", 9500)).thenReturn('T')

    val input = List(RowObj("chr1\t9500\t\tNA"))
    assert(runVarNorm(input, leftnormalize = false, vcfForm = false) == input)
  }

  test("Left-normalize deletion - N in refSeq mid-sequence stops inner loop via mismatch") {
    // ref="ACGT", alt="" at 9600 (trim mode) — 4-base deletion
    // Inner checks from end: ref(3)='T'!='N', getBase(9599)='T'=='T' → i=1
    //   ref(2)='G'!='N', getBase(9598)='G'=='G' → i=2
    //   ref(1)='C'!='N', getBase(9597)='N'≠'C' → mismatch stops at i=2
    // Partial shift by 2: aNewRef = getBases(9598,9599)+"AC" = "GT"+"AC" = "GTAC" at pos 9598
    mockRefSeqFrom(9597, "NGT")

    val input    = List(RowObj("chr1\t9600\tACGT\t"))
    val expected = List(RowObj("chr1\t9598\tGTAC\t"))
    assert(runVarNorm(input, vcfForm = false) == expected)
  }

  // ---- N in refSeq lookups (outer-loop N checks and inner-loop mismatches) ----

  test("Left-normalize deletion - N in refSeq stops inner loop via mismatch, no shift") {
    // ref="TT", alt="" at 7301 (trim mode)
    // Inner loop i=0: getBase(7300)='N' ≠ ref(1)='T' → mismatch stops loop (no N guard needed)
    // No shift; output unchanged
    when(mockRefSeq.getBase("chr1", 7300)).thenReturn('N')

    val input = List(RowObj("chr1\t7301\tTT\t"))
    assert(runVarNorm(input, vcfForm = false) == input)
  }

  test("Left-normalize deletion - N in refSeq stops outer loop after inner completes") {
    // ref="TT", alt="" at 7401 (trim mode)
    // Inner: getBase(7400)='T'==ref(1), getBase(7399)='T'==ref(0) → i=2 (inner exhausted)
    // Outer i=2: getBase(7398)='N' != getBase(7400)='T' → mismatch stops further shift
    //   (outer loop has no N-guard; the period-L spacing makes the repeat→N boundary a mismatch)
    // Shift by 2: output pos 7399 with ref "TT" from getBases
    mockRefSeqFrom(7398, "NTT")

    val input    = List(RowObj("chr1\t7401\tTT\t"))
    val expected = List(RowObj("chr1\t7399\tTT\t"))
    assert(runVarNorm(input, vcfForm = false) == expected)
  }

  test("Left-normalize insertion - N in refSeq stops outer loop after inner completes") {
    // ref="", alt="A" at 7602 (trim mode: input is pure insertion)
    // Inner: getBase(7601)='A'==allele(0) → i=1 (inner exhausted)
    // Outer i=1: getBase(7600)='A' ≠ 'N', getBase(7600)==getBase(7601)='A' → i=2
    // Outer i=2: getBase(7599)='N' != getBase(7600)='A' → mismatch stops
    //   (outer loop has no N-guard; the period-L spacing makes the repeat→N boundary a mismatch)
    // Shift by 2: allele updated from getBases to "A" at pos 7600
    mockRefSeqFrom(7599, "NAA")

    val input    = List(RowObj("chr1\t7602\t\tA"))
    val expected = List(RowObj("chr1\t7600\t\tA"))
    assert(runVarNorm(input, vcfForm = false) == expected)
  }

  // --- move

  test("Left-normalize insert - SNP") {
    mockRefSeqFrom("AAATTTGGG")
    val input    = List(RowObj("chr1\t6\tT\tTT"))
    val expected = List(RowObj("chr1\t3\tA\tAT"))
    assert(runVarNorm(input) == expected)
  }

  test("Left-normalize dele - trim to MNP") {
    mockRefSeqFrom("AAAGCGCGCTT")
    val input    = List(RowObj("chr1\t7\tCGC\tC"))
    val expected = List(RowObj("chr1\t3\tAGC\tA"))
    assert(runVarNorm(input) == expected)
  }

  test("right-normalize insert - SNP") {
    mockRefSeqFrom("AAATTTGGG")
    val input    = List(RowObj("chr1\t3\tT\tTT"))
    val expected = List(RowObj("chr1\t6\tT\tTT"))
    assert(runVarNorm(input, leftnormalize = false) == expected)
  }

  test("Left-normalize insert with N") {
    mockRefSeqFrom("NNNNNNNGGG")
    val input    = List(RowObj("chr1\t7\tN\tNN"))
    val expected = List(RowObj("chr1\t7\tN\tNN"))
    assert(runVarNorm(input) == expected)
  }

  test("Left-normalize delete with N") {
    mockRefSeqFrom("NNNNNNNGGG")
    val input    = List(RowObj("chr1\t7\tNN\tN"))
    val expected = List(RowObj("chr1\t7\tNN\tN"))
    assert(runVarNorm(input) == expected)
  }

  test("right-normalize insert with N") {
    mockRefSeqFrom("AAANNNNNNN")
    val input    = List(RowObj("chr1\t4\tN\tNN"))
    val expected = List(RowObj("chr1\t4\tN\tNN"))
    assert(runVarNorm(input, leftnormalize = false) == expected)
  }

  test("right-normalize delete with N") {
    mockRefSeqFrom("AAANNNNNNN")
    val input    = List(RowObj("chr1\t4\tNN\tN"))
    val expected = List(RowObj("chr1\t4\tNN\tN"))
    assert(runVarNorm(input, leftnormalize = false) == expected)
  }

  // ---- ordering ----

  test("Row order at same position is preserved after normalization") {
    // Two SNPs at the same position: neither is normalized (1:1), order must be preserved.
    // Original testVarnormOrder defined this inline data but had a copy-paste bug
    // and ran the wrong query — this is the corrected test.
    val input = List(
      RowObj("chr1\t14203\tT\tC"),
      RowObj("chr1\t14203\tA\tG")
    )
    assert(runVarNorm(input) == input)
  }

  test("Row order at same position is preserved after normalization if N in ref") {
    // Two SNPs at the same position: neither is normalized (1:1), order must be preserved.
    // Original testVarnormOrder defined this inline data but had a copy-paste bug
    // and ran the wrong query — this is the corrected test.
    val input = List(
      RowObj("chr1\t2634144\tC\tA"),
      RowObj("chr1\t2684218\tN\tA")
    )
    assert(runVarNorm(input) == input)
  }

  // ---- integration tests (require test data, no mocked refSeq) ----

  test("Right-then-left normalization round-trip is idempotent on dbsnp data") {
    // Applying right-norm then left-norm must produce variants that are gtshare-equivalent
    // at every step. Any failure here means normalization is not producing a canonical form.
    val query =
      "gor -p chr2 ../tests/data/gor/dbsnp_test.gorz" +
      " | calc oldpos pos | calc oldref #3 | calc oldalt #4" +
      " | varnorm #3 #4 -right" +
      " | calc oldposx pos | calc oldrefx #3 | calc oldaltx #4" +
      " | varnorm #3 #4 -left" +
      " | where gtshare(chrom,pos,#3,#4,oldpos,oldref,oldalt) = 0" +
      "    or  gtshare(chrom,oldpos,oldref,oldalt,oldposx,oldrefx,oldaltx) = 0" +
      "    or  gtshare(chrom,pos,#3,#4,oldposx,oldrefx,oldaltx) = 0" +
      " | rownum | throwif rownum = 1"
    assert(GorTestUtils.runGorPipeCount(query) == 0)
  }
}
