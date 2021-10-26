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

package gorsat.Utilities

import java.nio.file.Files
import gorsat.Analysis.{PhaseReadVariants, SkipAnalysis}
import gorsat.Commands.CommandParseUtilities
import gorsat.Iterators.RowListIterator
import gorsat.Monitors.MemoryMonitor
import gorsat.Outputs.ToList
import gorsat.gorsatGorIterator.MemoryMonitorUtil
import gorsat.process.GenericGorRunner
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.model.{Row, RowBase}
import org.gorpipe.gor.session.{GorSession, ProjectContext}
import org.gorpipe.model.gor.RowObj
import org.gorpipe.model.gor.iterators.RefSeq
import org.gorpipe.test.SlowTests
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfter, Ignore}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar

import scala.collection.mutable.ListBuffer

/**
  * Test PhaseReadVariants
  *
  * Exptected input and output format:
  *
  * chr pos ref alt varpos varqual id [other columns]
  *
  * Reference for chr1:1000 and chr2:1000 are cut out of hg19 with 1000 matching 100000.
  * 0123456789012345678901234567890
  * cactaagcacacagagaataatgtctagaat
  *
  */
@RunWith(classOf[JUnitRunner])
class UTestPhaseReadVariants extends AnyFunSuite with BeforeAndAfter with MockitoSugar {

  // Create mock objects

  val mockGorPipeSession: GorSession = mock[GorSession]
  val mockProjectContext: ProjectContext = mock[ProjectContext]
  val mockRefSeq: RefSeq = mock[RefSeq]

  when(mockGorPipeSession.getProjectContext).thenReturn(mockProjectContext)
  when(mockProjectContext.createRefSeq()).thenReturn(mockRefSeq)
  when(mockRefSeq.getBase("chr1", 1000)).thenReturn('C')
  when(mockRefSeq.getBase("chr1", 1001)).thenReturn('A')
  when(mockRefSeq.getBase("chr1", 1002)).thenReturn('C')
  when(mockRefSeq.getBase("chr1", 1003)).thenReturn('T')
  when(mockRefSeq.getBase("chr1", 1004)).thenReturn('A')
  when(mockRefSeq.getBase("chr1", 1005)).thenReturn('A')
  when(mockRefSeq.getBase("chr1", 1006)).thenReturn('G')
  when(mockRefSeq.getBase("chr1", 1007)).thenReturn('C')
  when(mockRefSeq.getBase("chr1", 1008)).thenReturn('A')
  when(mockRefSeq.getBase("chr1", 1009)).thenReturn('C')
  mockRefSeq.close()


  val maxBpMergeDist = 4
  // This test supports both using the mock session and to use external config files.
  val gorPipeSession: GorSession = mockGorPipeSession

  def runListTest(inputList: List[Row], expectedList: List[Row], maxMergeDist: Int = maxBpMergeDist): Unit = {
    val outputRowList = new ListBuffer[Row]
    val runner = new GenericGorRunner()
    runner.run(RowListIterator(inputList), PhaseReadVariants(maxMergeDist, gorPipeSession) | ToList(outputRowList))
    assert(outputRowList.toList == expectedList)
  }

  test("Not change - empty stream") {
    runListTest(List(), List())
  }

  test("No change - too big gap") {
    val inputList = List(
      RowObj("chr1\t1000\tC\tA\t20\t5\t1"),
      RowObj("chr1\t1004\tA\tG\t49\t5\t1")
    )
    runListTest(inputList, inputList)
  }

  test("No change - different phases (id)") {
    val inputList = List(
      RowObj("chr1\t1000\tC\tA\t20\t5\t1"),
      RowObj("chr1\t1001\tA\tG\t49\t5\t2")
    )
    runListTest(inputList, inputList)
  }

  test("No change - different chromosomes") {
    val inputList = List(
      RowObj("chr1\t1000\tC\tA\t20\t5\t1"),
      RowObj("chr2\t1001\tA\tC\t49\t5\t1")
    )
    runListTest(inputList, inputList)
  }

  test("No change - no variation") {
    val inputList = List(
      RowObj("chr1\t1000\tC\tA\t20\t5\t1"),
      RowObj("chr1\t1001\tA\tA\t49\t5\t1")
    )
    runListTest(inputList, inputList)
  }

  test("No Change - snp + snp, - 0 merge distance") {
    val inputList = List(
      RowObj("chr1\t1000\tC\tA\t20\t5\t1"),
      RowObj("chr1\t1001\tA\tG\t49\t5\t1")
    )

    runListTest(inputList, inputList, 0)
  }

  test("Change - snp + snp") {
    val inputList = List(
      RowObj("chr1\t1000\tC\tA\t20\t5\t1"),
      RowObj("chr1\t1001\tA\tG\t49\t5\t1")
    )
    val expectedOutputList = List(
      RowObj("chr1\t1000\tC\tA\t20\t5\t1"),
      RowObj("chr1\t1000\tCA\tAG\t20\t5\t1"),
      RowObj("chr1\t1001\tA\tG\t49\t5\t1")
    )

    runListTest(inputList, expectedOutputList)
  }

  test("Change - snp + snp - gap") {
    val inputList = List(
      RowObj("chr1\t1000\tC\tA\t20\t5\t1"),
      RowObj("chr1\t1002\tC\tT\t49\t5\t1")
    )
    val expectedOutputList = List(
      RowObj("chr1\t1000\tC\tA\t20\t5\t1"),
      RowObj("chr1\t1000\tCAC\tAAT\t20\t5\t1"),
      RowObj("chr1\t1002\tC\tT\t49\t5\t1")
    )

    runListTest(inputList, expectedOutputList)
  }

  test("Change - snp + snp - gap with presence") {
    val inputList = List(
      RowObj("chr1\t1000\tC\tA\t20\t5\t1"),
      RowObj("chr1\t1001\tA\tA\t20\t5\t1"),
      RowObj("chr1\t1002\tC\tT\t49\t5\t1")
    )
    val expectedOutputList = List(
      RowObj("chr1\t1000\tC\tA\t20\t5\t1"),
      RowObj("chr1\t1000\tCAC\tAAT\t20\t5\t1"),
      RowObj("chr1\t1001\tA\tA\t20\t5\t1"),
      RowObj("chr1\t1002\tC\tT\t49\t5\t1")
    )

    runListTest(inputList, expectedOutputList)
  }

  // This should not happen, as we should not have change in the same bp in the same read.
  test("Change - snp + snp + snp - same location") {
    val inputList = List(
      RowObj("chr1\t1000\tC\tA\t20\t5\t1"),
      RowObj("chr1\t1000\tC\tT\t20\t5\t1"),
      RowObj("chr1\t1001\tA\tT\t49\t5\t1")
    )
    val expectedOutputList = List(
      RowObj("chr1\t1000\tC\tA\t20\t5\t1"),
      RowObj("chr1\t1000\tCA\tAT\t20\t5\t1"),
      RowObj("chr1\t1000\tC\tT\t20\t5\t1"),
      RowObj("chr1\t1000\tCA\tTT\t20\t5\t1"),
      RowObj("chr1\t1001\tA\tT\t49\t5\t1")
    )

    runListTest(inputList, expectedOutputList)
  }

  test("Change - snp + snp - different quality low first") {
    val inputList = List(
      RowObj("chr1\t1000\tC\tA\t20\t5\t1"),
      RowObj("chr1\t1001\tA\tG\t49\t10\t1")
    )
    val expectedOutputList = List(
      RowObj("chr1\t1000\tC\tA\t20\t5\t1"),
      RowObj("chr1\t1000\tCA\tAG\t20\t5\t1"),
      RowObj("chr1\t1001\tA\tG\t49\t10\t1")
    )

    runListTest(inputList, expectedOutputList)
  }

  test("Change - snp + snp - different quality low second") {
    val inputList = List(
      RowObj("chr1\t1000\tC\tA\t20\t15\t1"),
      RowObj("chr1\t1001\tA\tG\t49\t10\t1")
    )
    val expectedOutputList = List(
      RowObj("chr1\t1000\tC\tA\t20\t15\t1"),
      RowObj("chr1\t1000\tCA\tAG\t20\t10\t1"),
      RowObj("chr1\t1001\tA\tG\t49\t10\t1")
    )

    runListTest(inputList, expectedOutputList)
  }

  test("Change - snp + snp - extra columns") {
    val inputList = List(
      RowObj("chr1\t1000\tC\tA\t20\t5\t1\text1\text2"),
      RowObj("chr1\t1001\tA\tG\t49\t5\t1\text3\text4")
    )
    val expectedOutputList = List(
      RowObj("chr1\t1000\tC\tA\t20\t5\t1\text1\text2"),
      RowObj("chr1\t1000\tCA\tAG\t20\t5\t1\text1\text2"),
      RowObj("chr1\t1001\tA\tG\t49\t5\t1\text3\text4")
    )

    runListTest(inputList, expectedOutputList)
  }

  test("Change - snp + long snp ") {
    val inputList = List(
      RowObj("chr1\t1000\tC\tA\t20\t5\t1"),
      RowObj("chr1\t1001\tACT\tGTA\t49\t5\t1")
    )
    val expectedOutputList = List(
      RowObj("chr1\t1000\tC\tA\t20\t5\t1"),
      RowObj("chr1\t1000\tCACT\tAGTA\t20\t5\t1"),
      RowObj("chr1\t1001\tACT\tGTA\t49\t5\t1")
    )

    runListTest(inputList, expectedOutputList, 4)
  }

  test("Change - long snp + snp - with gab") {
    val inputList = List(
      RowObj("chr1\t1000\tCA\tAC\t20\t5\t1"),
      RowObj("chr1\t1003\tT\tA\t49\t5\t1")
    )
    val expectedOutputList = List(
      RowObj("chr1\t1000\tCA\tAC\t20\t5\t1"),
      RowObj("chr1\t1000\tCACT\tACCA\t20\t5\t1"),
      RowObj("chr1\t1003\tT\tA\t49\t5\t1")
    )

    runListTest(inputList, expectedOutputList, 4)
  }

  test("Change - long snp + snp + snp - with gabs") {
    val inputList = List(
      RowObj("chr1\t1000\tCA\tAC\t20\t5\t1"),
      RowObj("chr1\t1003\tT\tA\t49\t5\t1"),
      RowObj("chr1\t1005\tA\tT\t49\t5\t1")
    )
    val expectedOutputList = List(
      RowObj("chr1\t1000\tCA\tAC\t20\t5\t1"),
      RowObj("chr1\t1000\tCACT\tACCA\t20\t5\t1"),
      RowObj("chr1\t1000\tCACTAA\tACCAAT\t20\t5\t1"),
      RowObj("chr1\t1003\tT\tA\t49\t5\t1"),
      RowObj("chr1\t1003\tTAA\tAAT\t49\t5\t1"),
      RowObj("chr1\t1005\tA\tT\t49\t5\t1")
    )

    runListTest(inputList, expectedOutputList, 8)
  }

  // 0123456789012345678901234567890
  // cactaagcacacagagaataatgtctagaat


  test("Change - long snp + snp + snp - with gabs, too long") {
    val inputList = List(
      RowObj("chr1\t1000\tCA\tAC\t20\t5\t1"),
      RowObj("chr1\t1003\tT\tA\t49\t5\t1"),
      RowObj("chr1\t1005\tA\tT\t49\t5\t1")
    )
    val expectedOutputList = List(
      RowObj("chr1\t1000\tCA\tAC\t20\t5\t1"),
      RowObj("chr1\t1000\tCACT\tACCA\t20\t5\t1"),
      RowObj("chr1\t1003\tT\tA\t49\t5\t1"),
      RowObj("chr1\t1003\tTAA\tAAT\t49\t5\t1"),
      RowObj("chr1\t1005\tA\tT\t49\t5\t1")
    )

    runListTest(inputList, expectedOutputList, 4)
  }

  test("Change - long snp + snp + long snp - with gabs, too long") {
    val inputList = List(
      RowObj("chr1\t1000\tCA\tAC\t20\t5\t1"),
      RowObj("chr1\t1003\tT\tA\t49\t5\t1"),
      RowObj("chr1\t1005\tAGC\tTAA\t49\t5\t1")
    )
    val expectedOutputList = List(
      RowObj("chr1\t1000\tCA\tAC\t20\t5\t1"),
      RowObj("chr1\t1000\tCACT\tACCA\t20\t5\t1"),
      RowObj("chr1\t1003\tT\tA\t49\t5\t1"),
      RowObj("chr1\t1003\tTAAGC\tAATAA\t49\t5\t1"),
      RowObj("chr1\t1005\tAGC\tTAA\t49\t5\t1")
    )

    runListTest(inputList, expectedOutputList, 7)
  }

  test("Change - snp + extra long snp ") {
    val inputList = List(
      RowObj("chr1\t1000\tC\tA\t20\t5\t1"),
      RowObj("chr1\t1001\tACTAAG\tTGGCCT\t49\t5\t1")
    )
    val expectedOutputList = List(
      RowObj("chr1\t1000\tC\tA\t20\t5\t1"),
      RowObj("chr1\t1001\tACTAAG\tTGGCCT\t49\t5\t1")
    )

    runListTest(inputList, expectedOutputList, 4)
  }

  test("Change - extra long snp + snp ") {
    val inputList = List(
      RowObj("chr1\t1000\tCACTA\tTGGGG\t20\t5\t1"),
      RowObj("chr1\t1005\tA\tT\t49\t5\t1")
    )
    val expectedOutputList = List(
      RowObj("chr1\t1000\tCACTA\tTGGGG\t20\t5\t1"),
      RowObj("chr1\t1005\tA\tT\t49\t5\t1")
    )

    runListTest(inputList, expectedOutputList)
  }

  test("Change - snp + insert") {
    val inputList = List(
      RowObj("chr1\t1000\tC\tA\t20\t5\t1"),
      RowObj("chr1\t1001\tA\tAC\t49\t5\t1")
    )
    val expectedOutputList = List(
      RowObj("chr1\t1000\tC\tA\t20\t5\t1"),
      RowObj("chr1\t1000\tCA\tAAC\t20\t5\t1"),
      RowObj("chr1\t1001\tA\tAC\t49\t5\t1")
    )

    runListTest(inputList, expectedOutputList)
  }

  test("Change - snp + deletion") {
    val inputList = List(
      RowObj("chr1\t1000\tC\tA\t20\t5\t1"),
      RowObj("chr1\t1001\tA\t\t49\t5\t1")
    )
    val expectedOutputList = List(
      RowObj("chr1\t1000\tC\tA\t20\t5\t1"),
      RowObj("chr1\t1000\tCA\tA\t20\t5\t1"),
      RowObj("chr1\t1001\tA\t\t49\t5\t1")
    )

    runListTest(inputList, expectedOutputList)
  }

  // 0123456789012345678901234567890
  // cactaagcacacagagaataatgtctagaat
  // -----------------------

  test("Change - snp + insert + snp, same location") {
    val inputList = List(
      RowObj("chr1\t1000\tC\tA\t20\t5\t1"),
      RowObj("chr1\t1000\tC\tCC\t20\t5\t1"),
      RowObj("chr1\t1003\tT\tA\t49\t5\t1")
    )
    val expectedOutputList = List(
      RowObj("chr1\t1000\tC\tA\t20\t5\t1"),
      RowObj("chr1\t1000\tC\tAC\t20\t5\t1"),
      RowObj("chr1\t1000\tCACT\tACACA\t20\t5\t1"),
      RowObj("chr1\t1000\tC\tCC\t20\t5\t1"),
      RowObj("chr1\t1000\tCACT\tCCACA\t20\t5\t1"),
      RowObj("chr1\t1003\tT\tA\t49\t5\t1")
    )

    runListTest(inputList, expectedOutputList)
  }

  // Strictly speaking this is not a valid order as the snp at 1000 should always be before the insert
  // 1000 (the insert is really at 10000.5).
  // You would expect get the same results as in prev test, maybe in different order, but the result
  // is different (line 5 is incorrect should be ACACA and CCACA is missing).
  // So for comment out this test.
  // TODO: Should we remove this?
  /*
  test("Change - insert + snp + snp, same location") {
    val inputList = List(
      RowObj("chr1\t1000\tC\tCC\t20\t5\t1"),
      RowObj("chr1\t1000\tC\tA\t20\t5\t1"),
      RowObj("chr1\t1003\tT\tA\t49\t5\t1")
    )
    val expectedOutputList = List(
      RowObj("chr1\t1000\tC\tCC\t20\t5\t1"),
      RowObj("chr1\t1000\tC\tAC\t20\t5\t1"),
      RowObj("chr1\t1000\tCACT\tACACA\t20\t5\t1"),
      RowObj("chr1\t1000\tC\tA\t20\t5\t1"),
      RowObj("chr1\t1000\tCACT\tAACA\t20\t5\t1"),
      RowObj("chr1\t1003\tT\tA\t49\t5\t1")
    )

    runListTest(inputList, expectedOutputList)
  }
  */

  // Should not happen as we can't have exactly the same bp change in the same read.
  // The result differs if we change the ordering see next test!
  test("Change - snp + deletion + snp, same location") {
    val inputList = List(
      RowObj("chr1\t1000\tC\tA\t20\t5\t1"),
      RowObj("chr1\t1000\tC\t\t20\t5\t1"),
      RowObj("chr1\t1003\tT\tA\t49\t5\t1")
    )
    val expectedOutputList = List(
      RowObj("chr1\t1000\tC\tA\t20\t5\t1"),
      RowObj("chr1\t1000\tCACT\tAACA\t20\t5\t1"),
      RowObj("chr1\t1000\tC\t\t20\t5\t1"),
      RowObj("chr1\t1000\tCACT\tACA\t20\t5\t1"),
      RowObj("chr1\t1003\tT\tA\t49\t5\t1")
    )

    runListTest(inputList, expectedOutputList)
  }

  // Should not happen as we can't have exactly the same bp change in the samne read.
  // The result differs if we change the ordering see prev test!
  test("Change - deletion + snp + snp, same location") {
    val inputList = List(
      RowObj("chr1\t1000\tC\t\t20\t5\t1"),
      RowObj("chr1\t1000\tC\tA\t20\t5\t1"),
      RowObj("chr1\t1003\tT\tA\t49\t5\t1")
    )
    val expectedOutputList = List(
      RowObj("chr1\t1000\tC\t\t20\t5\t1"),
      RowObj("chr1\t1000\tCACT\tACA\t20\t5\t1"),
      RowObj("chr1\t1000\tC\tA\t20\t5\t1"),
      RowObj("chr1\t1000\tCACT\tAACA\t20\t5\t1"),
      RowObj("chr1\t1003\tT\tA\t49\t5\t1")
    )

    runListTest(inputList, expectedOutputList)
  }

  test("Change - insert + snp") {
    val inputList = List(
      RowObj("chr1\t1000\tC\tCA\t20\t5\t1"),
      RowObj("chr1\t1001\tA\tG\t49\t5\t1")
    )
    val expectedOutputList = List(
      RowObj("chr1\t1000\tC\tCA\t20\t5\t1"),
      RowObj("chr1\t1000\tCA\tCAG\t20\t5\t1"),
      RowObj("chr1\t1001\tA\tG\t49\t5\t1")
    )

    runListTest(inputList, expectedOutputList)
  }

  test("Change - deletion + snp") {
    val inputList = List(
      RowObj("chr1\t1000\tC\t\t20\t5\t1"),
      RowObj("chr1\t1001\tA\tG\t49\t5\t1")
    )
    val expectedOutputList = List(
      RowObj("chr1\t1000\tC\t\t20\t5\t1"),
      RowObj("chr1\t1000\tCA\tG\t20\t5\t1"),
      RowObj("chr1\t1001\tA\tG\t49\t5\t1")
    )

    runListTest(inputList, expectedOutputList)
  }

  test("Change - deletion + insert") {
    val inputList = List(
      RowObj("chr1\t1000\tC\t\t20\t5\t1"),
      RowObj("chr1\t1001\tA\tGA\t49\t5\t1")
    )
    val expectedOutputList = List(
      RowObj("chr1\t1000\tC\t\t20\t5\t1"),
      RowObj("chr1\t1000\tCA\tGA\t20\t5\t1"),
      RowObj("chr1\t1001\tA\tGA\t49\t5\t1")
    )

    runListTest(inputList, expectedOutputList)
  }

  test("Change - insert + deletion") {
    val inputList = List(
      RowObj("chr1\t1000\tC\tAG\t20\t5\t1"),
      RowObj("chr1\t1001\tA\t\t49\t5\t1")
    )
    val expectedOutputList = List(
      RowObj("chr1\t1000\tC\tAG\t20\t5\t1"),
      RowObj("chr1\t1000\tCA\tAG\t20\t5\t1"),
      RowObj("chr1\t1001\tA\t\t49\t5\t1")
    )

    runListTest(inputList, expectedOutputList)
  }

  test("Change - insert + insert") {
    val inputList = List(
      RowObj("chr1\t1000\tC\tAC\t20\t5\t1"),
      RowObj("chr1\t1001\tA\tGA\t49\t5\t1")
    )
    val expectedOutputList = List(
      RowObj("chr1\t1000\tC\tAC\t20\t5\t1"),
      RowObj("chr1\t1000\tCA\tACGA\t20\t5\t1"),
      RowObj("chr1\t1001\tA\tGA\t49\t5\t1")
    )

    runListTest(inputList, expectedOutputList)
  }

  test("Change - deletion + deletion") {
    val inputList = List(
      RowObj("chr1\t1000\tC\t\t20\t5\t1"),
      RowObj("chr1\t1001\tA\t\t49\t5\t1")
    )
    val expectedOutputList = List(
      RowObj("chr1\t1000\tC\t\t20\t5\t1"),
      RowObj("chr1\t1000\tCA\t\t20\t5\t1"),
      RowObj("chr1\t1001\tA\t\t49\t5\t1")
    )

    runListTest(inputList, expectedOutputList)
  }

  test("Change - insert + insert, with gap") {
    val inputList = List(
      RowObj("chr1\t1000\tC\tAC\t20\t5\t1"),
      RowObj("chr1\t1002\tC\tGA\t49\t5\t1")
    )
    val expectedOutputList = List(
      RowObj("chr1\t1000\tC\tAC\t20\t5\t1"),
      RowObj("chr1\t1000\tCAC\tACAGA\t20\t5\t1"),
      RowObj("chr1\t1002\tC\tGA\t49\t5\t1")
    )

    runListTest(inputList, expectedOutputList)
  }

  test("Change - deletion + deletion, with gap") {
    val inputList = List(
      RowObj("chr1\t1000\tC\t\t20\t5\t1"),
      RowObj("chr1\t1002\tC\t\t49\t5\t1")
    )
    val expectedOutputList = List(
      RowObj("chr1\t1000\tC\t\t20\t5\t1"),
      RowObj("chr1\t1000\tCAC\tA\t20\t5\t1"),
      RowObj("chr1\t1002\tC\t\t49\t5\t1")
    )

    runListTest(inputList, expectedOutputList)
  }


  // 0123456789012345678901234567890
  // cactaagcacacagagaataatgtctagaat
  // -----------------------

}

/**
  * Test MemoryMonitorUtil.
  *
  */
@RunWith(classOf[JUnitRunner])
class UTestMemoryMonitorUtil extends AnyFunSuite {

  test("NotOutOfMemory") {
    val mmu: MemoryMonitorUtil = new MemoryMonitorUtil((actualFreeMem: Long, args: List[_]) => {
      assert(false)
    }, rowsBetweenChecks = 1, reqMinFreeMemMB = 100)

    mmu.check()
  }

  test("OutOfMemory") {
    var gotError = false
    val mmu: MemoryMonitorUtil = new MemoryMonitorUtil((actualFreeMem: Long, args: List[_]) => {
      gotError = true
    }, rowsBetweenChecks = 1, reqMinFreeMemMB = 100000000, reqMinFreeMemRatio = 1)

    mmu.check()
    assert(gotError)
  }

  test("ErrorParam") {
    var errorParam = -1
    val mmu: MemoryMonitorUtil = new MemoryMonitorUtil((actualFreeMem: Long, args: List[_]) => {
      errorParam = args.head.asInstanceOf[Int]
    }, rowsBetweenChecks = 1, reqMinFreeMemMB = 100000000, reqMinFreeMemRatio = 1)

    mmu.check(781)
    assert(errorParam == 781)
  }

  test("CallNum") {
    val rowsBetweenCheck = 100
    var errorCount = 0
    val mmu: MemoryMonitorUtil = new MemoryMonitorUtil((actualFreeMem: Long, args: List[_]) => {
      errorCount += 1
    }, rowsBetweenChecks = rowsBetweenCheck, reqMinFreeMemMB = 100000000, reqMinFreeMemRatio = 1)

    val callCount = 200
    for (i <- 1 to callCount) {
      mmu.check()
    }
    assert(mmu.lineNum == callCount)
    assert(errorCount == callCount / rowsBetweenCheck)
  }

  test("NotOutOfMemoryRatio") {
    val mmu: MemoryMonitorUtil = new MemoryMonitorUtil((actualFreeMem: Long, args: List[_]) => {
      assert(false)
    }, rowsBetweenChecks = 1, reqMinFreeMemRatio = 0.1f)

    mmu.check()
  }

  test("OutOfMemoryRatio") {
    var gotError = false
    val mmu: MemoryMonitorUtil = new MemoryMonitorUtil((actualFreeMem: Long, args: List[_]) => {
      gotError = true
    }, rowsBetweenChecks = 1, reqMinFreeMemRatio = 1)

    mmu.check()
    assert(gotError)
  }

  test("DefaultParams") {
    val mmu: MemoryMonitorUtil = new MemoryMonitorUtil((actualFreeMem: Long, args: List[_]) => {
      // Should be default off.
      assert(false)
    }, rowsBetweenChecks = 1)

    mmu.check()
    assert(mmu.minFreeMem <= 0)
  }

  test("NotOutOfMemoryRatio_NoGC") {
    val mmu: MemoryMonitorUtil = new MemoryMonitorUtil((actualFreeMem: Long, args: List[_]) => {
      assert(false)
    }, rowsBetweenChecks = 1, reqMinFreeMemRatio = 0.1f, gcRatio = 1.1f)

    mmu.check()
    assert(mmu.lastGCTime == 0)
  }

  test("NotOutOfMemoryRatio_GC") {
    val mmu: MemoryMonitorUtil = new MemoryMonitorUtil((actualFreeMem: Long, args: List[_]) => {
      assert(false)
    }, rowsBetweenChecks = 1, reqMinFreeMemRatio = 0.1f, gcRatio = 10)

    mmu.check()
    assert(mmu.lastGCTime > 0)
  }

}

/**
  * Test MemoryMonitorUtil.
  * Long running tests.
  *
  */
// "Ignore, fails often with out of memory.  Maybe not relative anymore as we don't use ratio anymore."
@Ignore
@RunWith(classOf[JUnitRunner])
@Category(Array(classOf[SlowTests]))
class UTestSlowMemoryMonitorUtil extends AnyFunSuite {
  test("OutOfMemory_defaultRatio") {
    var gotError = false
    val mmu: MemoryMonitorUtil = new MemoryMonitorUtil((actualFreeMem: Long, args: List[_]) => {
      gotError = true
    }, rowsBetweenChecks = 1, reqMinFreeMemMB = 100000000)

    // Ask for the menFreeMem to be 10TB but the ratio will use 1/4 of the max mem instead.
    val bytesHog = ((Runtime.getRuntime.freeMemory() + Runtime.getRuntime.maxMemory() - Runtime.getRuntime.totalMemory()) * 0.8).toLong
    val memoryHog = scala.collection.mutable.ListBuffer.empty[Array[Byte]]
    for (i <- 1 to 1000) {
      memoryHog += Array.fill[Byte]((bytesHog / 1000).toInt)(0.toByte)
    }

    mmu.check()
    assert(gotError)
  }

}

/**
  * Test MemoryMonitor.
  */
@RunWith(classOf[JUnitRunner])
class UTestMemoryMonitor extends AnyFunSuite {

  test("NotOutOfMemory") {
    val mm = MemoryMonitor("TestMemoryMonitor", minFreeMemMB = 100);
    val pipe = mm | SkipAnalysis(Integer.MAX_VALUE)

    for (i <- 1 to 5000) {
      pipe.process(new RowBase("chr1", i, "Test Row", null, null))
    }

    assert(mm.mmu.lineNum == 5000)
  }

  // Ignored (GOR-498) because when running test inside docker we don't go out of memory
  ignore("OutOfMemory") {
    var gotError = ""
    val max = Runtime.getRuntime.maxMemory()
    val mm = MemoryMonitor("TestMemoryMonitor", minFreeMemMB = (max / 1000000 / 5).toInt)
    val pipe = mm | SkipAnalysis(Integer.MAX_VALUE)

    val bytesHog = ((Runtime.getRuntime.freeMemory() + Runtime.getRuntime.maxMemory() - Runtime.getRuntime.totalMemory()) * 0.9).toLong
    val increments = 100000
    val memoryHog = scala.collection.mutable.ListBuffer.empty[Array[Byte]]

    try {
      for (i <- 1 to increments) {
        memoryHog += Array.fill[Byte]((bytesHog / increments).toInt)(0.toByte)
        pipe.process(new RowBase("chr1", i, "Test Row", null, null))
      }
    } catch {
      case e: Exception => gotError = e.getMessage
    }

    assert(gotError.startsWith("MemoryMonitor: Out of memory executing TestMemoryMonitor"))
  }

}
