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

package gorsat

import Script.{ExecutionBlock, MacroParsingResult}
import process.{GenericSessionFactory, _}
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner

/**
  * Class to test individual macro input sources. This includes exansion and pre processing tests.
  */
@RunWith(classOf[JUnitRunner])
class UTestMacroParsing extends AnyFunSuite with BeforeAndAfter {

  before {
    GorPipeCommands.register()
    GorPipeMacros.register()
    DynIterator.createGorIterator_$eq(PipeInstance.createGorIterator)
  }

  private def performPreProcessingTest(macroName: String, dataPath: String, queryName: String): Unit = {
    val context = new GenericSessionFactory().create().getGorContext
    val queries = Array(s"$macroName $dataPath | top 10")

    val macroInfo = GorPipeMacros.getInfo(macroName)

    if (macroInfo.nonEmpty) {
      val result = macroInfo.get.preProcessCommand(queries, context)

      assert(result.length == 2)
      assert(result(0) == s"create $queryName = $macroName $dataPath | top 10")
      assert(result(1) == s"GOR [$queryName]")
    } else {
      fail()
    }
  }

  test("Macro: PGOR - pre parsing") {
    performPreProcessingTest("pgor", "../tests/data/gor/genes.gor", "thepgorquery")
  }

  test("Macro: PARTGOR - pre parsing") {
    performPreProcessingTest("partgor", "../tests/data/gor/genes.gor", "thepartgorquery")
  }

  test("Macro: TABLEFUNCTION - pre parsing") {
    performPreProcessingTest("tablefunction", "../tests/data/reports/test3.yml", "tablefunctionquery")
  }

  test("Macro: PARALLEL - gor pre parsing") {
    performPreProcessingTest("parallel", "-parts <(../tests/data/gor/genes.gor | top 10) <(gorrows -p chr1:0-100)", "theparallelquery")
  }

  def performBlockExpansionTest(macroName: String, block: ExecutionBlock, options: Array[String]): Option[MacroParsingResult] = {
    val macroInfo = GorPipeMacros.getInfo(macroName)
    val context = new GenericSessionFactory().create().getGorContext

    if (macroInfo.nonEmpty) {
      Option(macroInfo.get.init(block.groupName, block, context, doHeader = false, options, false))
    } else {
      Option(null)
    }
  }

  test("Macro: PGOR - block expansion") {
    val resultBlocks = performBlockExpansionTest("pgor",
      ExecutionBlock("[xxx]", "pgor ../tests/data/gor/genes.gor | top 10", null, null, "xxx"),
      Array("../tests/data/gor/genes.gor"))

    if (resultBlocks.nonEmpty) {
      val commands = resultBlocks.get.createCommands

      assert(commands.size == 2)
      val eset = commands.entrySet().iterator()
      val head = eset.next()
      val last = eset.next()
      assert(head.getKey == "[xxx_##SPLIT_CHR_REPLACE##]")
      assert(last.getKey == "[xxx]")

      val command1 = head.getValue
      val command2 = last.getValue
      assertResult("gor -p ##SPLIT_CHR_REPLACE## <(../tests/data/gor/genes.gor | top 10)")(command1.query)
      val command2query = command2.query.split(" \\[").sorted.mkString(" [")
      assertResult( "GORDICT [xxx_chr10a] chr10:0-40349999 [xxx_chr10b] chr10:40350000- [xxx_chr11a] chr11:0-52749999 [xxx_chr11b] chr11:52750000- [xxx_chr12a] chr12:0-34999999 [xxx_chr12b] chr12:35000000- [xxx_chr13] chr13 [xxx_chr14] chr14 [xxx_chr15] chr15 [xxx_chr16] chr16 [xxx_chr17] chr17 [xxx_chr18] chr18 [xxx_chr19] chr19 [xxx_chr1a] chr1:0-123999999 [xxx_chr1b] chr1:124000000- [xxx_chr20] chr20 [xxx_chr21] chr21 [xxx_chr22] chr22 [xxx_chr2a] chr2:0-93099999 [xxx_chr2b] chr2:93100000- [xxx_chr3a] chr3:0-91349999 [xxx_chr3b] chr3:91350000- [xxx_chr4a] chr4:0-50749999 [xxx_chr4b] chr4:50750000- [xxx_chr5a] chr5:0-47649999 [xxx_chr5b] chr5:47650000- [xxx_chr6a] chr6:0-60124999 [xxx_chr6b] chr6:60125000- [xxx_chr7a] chr7:0-59329999 [xxx_chr7b] chr7:59330000- [xxx_chr8a] chr8:0-45499999 [xxx_chr8b] chr8:45500000- [xxx_chr9a] chr9:0-53699999 [xxx_chr9b] chr9:53700000- [xxx_chrM] chrM [xxx_chrXY] chrXY [xxx_chrXa] chrX:0-59999999 [xxx_chrXb] chrX:60000000- [xxx_chrY] chrY")(command2query)
    } else {
      fail()
    }
  }

  test("Macro: PGOR - block expansion, build hg38") {
    val resultBlocks = performBlockExpansionTest("pgor",
      ExecutionBlock("[xxx]", "pgor ../tests/data/gor/genes.gor | top 10", null, null, "xxx"),
      Array("../tests/data/gor/genes.gor"))

    if (resultBlocks.nonEmpty) {
      val commands = resultBlocks.get.createCommands

      assert(commands.size == 2)
      val eset = commands.entrySet().iterator()
      val head = eset.next()
      val last = eset.next()
      assert(head.getKey == "[xxx_##SPLIT_CHR_REPLACE##]")
      assert(last.getKey == "[xxx]")

      val command1 = head.getValue
      val command2 = last.getValue
      assertResult("gor -p ##SPLIT_CHR_REPLACE## <(../tests/data/gor/genes.gor | top 10)")(command1.query)
      val command2query = command2.query.split(" \\[").sorted.mkString(" [")
      assertResult("GORDICT [xxx_chr10a] chr10:0-40349999 [xxx_chr10b] chr10:40350000- [xxx_chr11a] chr11:0-52749999 [xxx_chr11b] chr11:52750000- [xxx_chr12a] chr12:0-34999999 [xxx_chr12b] chr12:35000000- [xxx_chr13] chr13 [xxx_chr14] chr14 [xxx_chr15] chr15 [xxx_chr16] chr16 [xxx_chr17] chr17 [xxx_chr18] chr18 [xxx_chr19] chr19 [xxx_chr1a] chr1:0-123999999 [xxx_chr1b] chr1:124000000- [xxx_chr20] chr20 [xxx_chr21] chr21 [xxx_chr22] chr22 [xxx_chr2a] chr2:0-93099999 [xxx_chr2b] chr2:93100000- [xxx_chr3a] chr3:0-91349999 [xxx_chr3b] chr3:91350000- [xxx_chr4a] chr4:0-50749999 [xxx_chr4b] chr4:50750000- [xxx_chr5a] chr5:0-47649999 [xxx_chr5b] chr5:47650000- [xxx_chr6a] chr6:0-60124999 [xxx_chr6b] chr6:60125000- [xxx_chr7a] chr7:0-59329999 [xxx_chr7b] chr7:59330000- [xxx_chr8a] chr8:0-45499999 [xxx_chr8b] chr8:45500000- [xxx_chr9a] chr9:0-53699999 [xxx_chr9b] chr9:53700000- [xxx_chrM] chrM [xxx_chrXY] chrXY [xxx_chrXa] chrX:0-59999999 [xxx_chrXb] chrX:60000000- [xxx_chrY] chrY")(command2query)
    } else {
      fail()
    }
  }

  test("Macro: PGOR - block expansion, build hg38 with full chromosome split") {
    val resultBlocks = performBlockExpansionTest("pgor",
      ExecutionBlock("[xxx]", "pgor ../tests/data/gor/genes.gor | group chromo -gc 1-4 -count | top 10", null, null, "xxx"),
      Array("../tests/data/gor/genes.gor"))

    if (resultBlocks.nonEmpty) {
      val commands = resultBlocks.get.createCommands

      assert(commands.size == 2)
      val eset = commands.entrySet().iterator()
      val head = eset.next()
      val last = eset.next()
      assert(head.getKey == "[xxx_##REGULAR_CHR_REPLACE##]")
      assert(last.getKey == "[xxx]")

      val command1 = head.getValue
      val command2 = last.getValue
      assertResult("gor -p ##REGULAR_CHR_REPLACE## <(../tests/data/gor/genes.gor | group chromo -gc 1-4 -count | top 10)")(command1.query)
      val command2query = command2.query.split(" \\[").sorted.mkString(" [")
      assertResult("GORDICT [xxx_chr10] chr10 [xxx_chr11] chr11 [xxx_chr12] chr12 [xxx_chr13] chr13 [xxx_chr14] chr14 [xxx_chr15] chr15 [xxx_chr16] chr16 [xxx_chr17] chr17 [xxx_chr18] chr18 [xxx_chr19] chr19 [xxx_chr1] chr1 [xxx_chr20] chr20 [xxx_chr21] chr21 [xxx_chr22] chr22 [xxx_chr2] chr2 [xxx_chr3] chr3 [xxx_chr4] chr4 [xxx_chr5] chr5 [xxx_chr6] chr6 [xxx_chr7] chr7 [xxx_chr8] chr8 [xxx_chr9] chr9 [xxx_chrM] chrM [xxx_chrXY] chrXY [xxx_chrX] chrX [xxx_chrY] chrY")(command2query)
    } else {
      fail()
    }
  }

  test("Macro: PARALLEL - block expansion") {
    val resultBlocks = performBlockExpansionTest("parallel",
      ExecutionBlock("[xxx]", "parallel -parts <(norrows 10 -offset 10) <(norrows 10 -offset 10 | calc dd rownum*#{col:rownum}) | top 10", null, null, "xxx"),
      Array("-parts", "<(norrows 10 -offset 10)","<(norrows 10 -offset 10 | calc dd rownum*#{col:rownum})"))

    if (resultBlocks.nonEmpty) {
      val commands = resultBlocks.get.createCommands

      assert(commands.size == 11)
      assert(commands.containsKey("[xxx_1]"))
      assert(commands.containsKey("[xxx_9]"))
      assert(commands.containsKey("[xxx]"))

      val command1 = commands.get("[xxx_1]")
      assert(command1.query.equals("norrows 10 -offset 10 | calc dd rownum*10 |  top 10"))
    } else {
      fail()
    }
  }

}
