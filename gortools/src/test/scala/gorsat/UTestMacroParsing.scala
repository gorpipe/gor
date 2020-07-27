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
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FunSuite}

/**
  * Class to test individual macro input sources. This includes exansion and pre processing tests.
  */
@RunWith(classOf[JUnitRunner])
class UTestMacroParsing extends FunSuite with BeforeAndAfter {

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
      Option(macroInfo.get.init(block.groupName, block, context, doHeader = false, options))
    } else {
      Option(null)
    }
  }

  test("Macro: PGOR - block expansion") {
    val resultBlocks = performBlockExpansionTest("pgor",
      ExecutionBlock("[xxx]", "pgor ../tests/data/gor/genes.gor | top 10", null, "xxx"),
      Array("../tests/data/gor/genes.gor"))

    if (resultBlocks.nonEmpty) {
      val commands = resultBlocks.get.createCommands

      assert(commands.size == 2)
      assert(commands.head._1 == "[xxx_##SPLIT_CHR_REPLACE##]")
      assert(commands.last._1 == "[xxx]")

      val command1 = commands.head._2
      val command2 = commands.last._2
      assert(command1.query == "gor -p ##SPLIT_CHR_REPLACE## <(../tests/data/gor/genes.gor | top 10)")
      assert(command2.query == "gordict [xxx_chr11] chr11:0-150000000 [xxx_chr5] chr5:0-200000000 [xxx_chr22] chr22:0-100000000 [xxx_chr8] chr8:0-150000000 [xxx_chr19] chr19:0-100000000 [xxx_chrY] chrY:0-100000000 [xxx_chr1] chr1:0-250000000 [xxx_chr15] chr15:0-150000000 [xxx_chr12] chr12:0-150000000 [xxx_chr18] chr18:0-100000000 [xxx_chr20] chr20:0-100000000 [xxx_chr2] chr2:0-250000000 [xxx_chr13] chr13:0-150000000 [xxx_chr7] chr7:0-200000000 [xxx_chr14] chr14:0-150000000 [xxx_chr3] chr3:0-200000000 [xxx_chrM] chrM:0-20000 [xxx_chr17] chr17:0-100000000 [xxx_chrXY] chrXY:0-1 [xxx_chr4] chr4:0-200000000 [xxx_chr6] chr6:0-200000000 [xxx_chr9] chr9:0-150000000 [xxx_chrX] chrX:0-200000000 [xxx_chr10] chr10:0-150000000 [xxx_chr21] chr21:0-100000000 [xxx_chr16] chr16:0-100000000")
    } else {
      fail
    }
  }

  test("Macro: PGOR - block expansion, build hg38") {
    val resultBlocks = performBlockExpansionTest("pgor",
      ExecutionBlock("[xxx]", "pgor ../tests/data/gor/genes.gor | top 10", null, "xxx"),
      Array("../tests/data/gor/genes.gor"))

    if (resultBlocks.nonEmpty) {
      val commands = resultBlocks.get.createCommands

      assert(commands.size == 2)
      assert(commands.head._1 == "[xxx_##SPLIT_CHR_REPLACE##]")
      assert(commands.last._1 == "[xxx]")

      val command1 = commands.head._2
      val command2 = commands.last._2
      assert(command1.query == "gor -p ##SPLIT_CHR_REPLACE## <(../tests/data/gor/genes.gor | top 10)")
      assert(command2.query == "gordict [xxx_chr11] chr11:0-150000000 [xxx_chr5] chr5:0-200000000 [xxx_chr22] chr22:0-100000000 [xxx_chr8] chr8:0-150000000 [xxx_chr19] chr19:0-100000000 [xxx_chrY] chrY:0-100000000 [xxx_chr1] chr1:0-250000000 [xxx_chr15] chr15:0-150000000 [xxx_chr12] chr12:0-150000000 [xxx_chr18] chr18:0-100000000 [xxx_chr20] chr20:0-100000000 [xxx_chr2] chr2:0-250000000 [xxx_chr13] chr13:0-150000000 [xxx_chr7] chr7:0-200000000 [xxx_chr14] chr14:0-150000000 [xxx_chr3] chr3:0-200000000 [xxx_chrM] chrM:0-20000 [xxx_chr17] chr17:0-100000000 [xxx_chrXY] chrXY:0-1 [xxx_chr4] chr4:0-200000000 [xxx_chr6] chr6:0-200000000 [xxx_chr9] chr9:0-150000000 [xxx_chrX] chrX:0-200000000 [xxx_chr10] chr10:0-150000000 [xxx_chr21] chr21:0-100000000 [xxx_chr16] chr16:0-100000000")
    } else {
      fail
    }
  }

  test("Macro: PGOR - block expansion, build hg38 with full chromosome split") {
    val resultBlocks = performBlockExpansionTest("pgor",
      ExecutionBlock("[xxx]", "pgor ../tests/data/gor/genes.gor | group chromo -gc 1-4 -count | top 10", null, "xxx"),
      Array("../tests/data/gor/genes.gor"))

    if (resultBlocks.nonEmpty) {
      val commands = resultBlocks.get.createCommands

      assert(commands.size == 2)
      assert(commands.head._1 == "[xxx_##REGULAR_CHR_REPLACE##]")
      assert(commands.last._1 == "[xxx]")

      val command1 = commands.head._2
      val command2 = commands.last._2
      assert(command1.query == "gor -p ##REGULAR_CHR_REPLACE## <(../tests/data/gor/genes.gor | group chromo -gc 1-4 -count | top 10)")
      assert(command2.query == "gordict [xxx_chr11] chr11:0-150000000 [xxx_chr5] chr5:0-200000000 [xxx_chr22] chr22:0-100000000 [xxx_chr8] chr8:0-150000000 [xxx_chr19] chr19:0-100000000 [xxx_chrY] chrY:0-100000000 [xxx_chr1] chr1:0-250000000 [xxx_chr15] chr15:0-150000000 [xxx_chr12] chr12:0-150000000 [xxx_chr18] chr18:0-100000000 [xxx_chr20] chr20:0-100000000 [xxx_chr2] chr2:0-250000000 [xxx_chr13] chr13:0-150000000 [xxx_chr7] chr7:0-200000000 [xxx_chr14] chr14:0-150000000 [xxx_chr3] chr3:0-200000000 [xxx_chrM] chrM:0-20000 [xxx_chr17] chr17:0-100000000 [xxx_chrXY] chrXY:0-1 [xxx_chr4] chr4:0-200000000 [xxx_chr6] chr6:0-200000000 [xxx_chr9] chr9:0-150000000 [xxx_chrX] chrX:0-200000000 [xxx_chr10] chr10:0-150000000 [xxx_chr21] chr21:0-100000000 [xxx_chr16] chr16:0-100000000")
    } else {
      fail
    }
  }

  test("Macro: PARALLEL - block expansion") {
    val resultBlocks = performBlockExpansionTest("parallel",
      ExecutionBlock("[xxx]", "parallel -parts <(norrows 10 -offset 10) <(norrows 10 -offset 10 | calc dd rownum*#{col:rownum}) | top 10", null, "xxx"),
      Array("-parts", "<(norrows 10 -offset 10)","<(norrows 10 -offset 10 | calc dd rownum*#{col:rownum})"))

    if (resultBlocks.nonEmpty) {
      val commands = resultBlocks.get.createCommands

      assert(commands.size == 11)
      assert(commands.contains("[xxx_1]"))
      assert(commands.contains("[xxx_9]"))
      assert(commands.contains("[xxx]"))

      val command1 = commands.get("[xxx_1]")
      assert(command1.get.query == "norrows 10 -offset 10 | calc dd rownum*10 |  top 10")
    } else {
      fail
    }
  }

}
