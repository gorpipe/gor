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

package gorsat.Script

import org.gorpipe.exceptions.GorParsingException
import gorsat.Commands.CommandParseUtilities
import gorsat.DynIterator
import gorsat.QueryHandlers.GeneralQueryHandler
import gorsat.Script.ScriptExecutionEngine.ExecutionBlocks
import gorsat.process.{GenericSessionFactory, GorPipeCommands, GorPipeMacros, PipeInstance}
import org.gorpipe.exceptions.{GorException, GorParsingException}
import org.gorpipe.gor.GorContext
import org.gorpipe.test.utils.FileTestUtils
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FunSuite}

@RunWith(classOf[JUnitRunner])
class UTestScriptExecutionEngine extends FunSuite with BeforeAndAfter {

  def createScriptExecutionEngine(listener: ScriptExecutionListener): ScriptExecutionEngine = {
    val context = new GenericSessionFactory().create().getGorContext
    val queryHandler = new GeneralQueryHandler(context, false)
    val headerQueryHandler = new GeneralQueryHandler(context, true)
    val usedListend = if (listener == null) new DefaultListener else listener
    new ScriptExecutionEngine(queryHandler, headerQueryHandler, context, usedListend)
  }

  def performTest(commands: Array[String], listener: ScriptExecutionListener): Unit = {
    try {
      val engine = createScriptExecutionEngine(listener)
      engine.execute(commands)
    } catch {
      case e: GorException => // Do nothing
        e.printStackTrace()
    }
  }

  protected var bGordPath = ""

  before {
    DynIterator.createGorIterator = PipeInstance.createGorIterator
    GorPipeCommands.register()
    GorPipeMacros.register()
    var tempDirectory = FileTestUtils.createTempDirectory(this.getClass.getName)
    var bGord = FileTestUtils.createTempFile(tempDirectory, "b.gord",
      "leftjoin.gor|xbucket.gorz\ta\nleftjoin.gor|xbucket.gorz\tb"
    )
    bGordPath = bGord.getCanonicalPath
  }

  test("Script with incorrect query, error in top") {
    val igorCommands = Array("def ##foo## = nor -h",
      "def ##bar## = top 10f",
      "create xxx = ##foo## ../tests/config/build37split.txt | ##bar##",
      "gor [xxx]")

    val throwable = intercept[GorException] {
      val engine = createScriptExecutionEngine(new DefaultListener)
      engine.execute(igorCommands)
    }

    assert(throwable != null)
  }

  test("Multiple non-create statements not allowed") {
    // See https://nextcode.atlassian.net/browse/GOP-850
    val script =
      """
        |create x = gorrows 1,1;
        ||top 10;
        |gor [x]
        |""".stripMargin
    val gorCommands = CommandParseUtilities.quoteSafeSplitAndTrim(script, ';') // In case this is a command line script

    val throwable = intercept[GorException] {
      val engine = createScriptExecutionEngine(new DefaultListener)
      engine.execute(gorCommands)
    }

    assert(throwable != null)
  }

  test("Test alias replacement in script") {

    val igorCommands = Array("def ##foo## = nor -h",
      "def ##bar## = top 10",
      "##foo## ../tests/config/build37split.txt | ##bar##")

    case class Listener() extends ScriptExecutionListener {
      override def beforeAlias(gorCommands: Array[String]): Unit = {
        assert(gorCommands.length == 3)
        assert(gorCommands(0) == igorCommands(0))
        assert(gorCommands(1) == igorCommands(1))
        assert(gorCommands(2) == igorCommands(2))
      }

      override def afterAlias(gorCommands: Array[String]): Unit = {
        assert(gorCommands.length == 1)
        assert(gorCommands(0) == "nor -h ../tests/config/build37split.txt | top 10")
      }
    }

    performTest(igorCommands, Listener())
  }

  test("Test alias where no aliases are used") {
    val igorCommands = Array("def ##foo## = nor -h",
      "def ##bar## = top 10",
      "##foo## ../tests/config/build37split.txt | top 10")

    case class Listener() extends ScriptExecutionListener {
      override def beforeAlias(gorCommands: Array[String]): Unit = {
        assert(gorCommands.length == 3)
        assert(gorCommands(0) == igorCommands(0))
        assert(gorCommands(1) == igorCommands(1))
        assert(gorCommands(2) == igorCommands(2))
      }

      override def afterAlias(gorCommands: Array[String]): Unit = {
        assert(gorCommands.length == 1)
        assert(gorCommands(0) == "nor -h ../tests/config/build37split.txt | top 10")
      }
    }

    performTest(igorCommands, Listener())
  }

  test("Script pre processing of non macro gor query") {
    val igorCommands = Array("gor ../tests/genes.gor | top 10")

    case class Listener() extends ScriptExecutionListener {
      override def beforePreProcessing(gorCommands: Array[String]): Unit = {
        assert(gorCommands(0) == igorCommands(0))
      }

      override def afterPreProcessing(gorCommands: Array[String]): Unit = {
        assert(gorCommands(0) == igorCommands(0))
      }
    }

    performTest(igorCommands, Listener())
  }

  test("Script pre processing of pgor macro query") {
    val igorCommands = Array("pgor ../tests/data/gor/genes.gor | top 10")

    case class Listener() extends ScriptExecutionListener {
      override def beforePreProcessing(gorCommands: Array[String]): Unit = {
        assert(gorCommands(0) == igorCommands(0))
      }

      override def afterPreProcessing(gorCommands: Array[String]): Unit = {
        assert(gorCommands.length == 2)
        assert(gorCommands(0) == "create thepgorquery = pgor ../tests/data/gor/genes.gor | top 10")
        assert(gorCommands(1) == "GOR [thepgorquery]")
      }
    }

    performTest(igorCommands, Listener())
  }

  test("Script pre processing of partgor macro query") {
    val igorCommands = Array("partgor -parts 5 -dict " + bGordPath + " <(gor -f #{tags} ../tests/data/gor/genes.gor)| top 10")

    case class Listener() extends ScriptExecutionListener {
      override def beforePreProcessing(gorCommands: Array[String]): Unit = {
        assert(gorCommands(0) == igorCommands(0))
      }

      override def afterPreProcessing(gorCommands: Array[String]): Unit = {
        assert(gorCommands.length == 2)
        assert(gorCommands(0) == "create thepartgorquery = partgor -parts 5 -dict " + bGordPath + " <(gor -f #{tags} ../tests/data/gor/genes.gor)| top 10")
        assert(gorCommands(1) == "GOR [thepartgorquery]")
      }
    }

    performTest(igorCommands, Listener())
  }

  test("Script pre processing of tablefunction macro query") {
    val igorCommands = Array("tablefunction ../tests/data/reports/test3.yml()")

    case class Listener() extends ScriptExecutionListener {
      override def beforePreProcessing(gorCommands: Array[String]): Unit = {
        assert(gorCommands(0) == igorCommands(0))
      }

      override def afterPreProcessing(gorCommands: Array[String]): Unit = {
        assert(gorCommands.length == 2)
        assert(gorCommands(0) == "create tablefunctionquery = tablefunction ../tests/data/reports/test3.yml()")
        assert(gorCommands(1) == "GOR [tablefunctionquery]")
      }
    }

    performTest(igorCommands, Listener())
  }

  test("Batch execution with no dependencies in gor script") {
    val igorCommands = Array("create xxx = gorrow chr1,1,1",
      "create yyy = gorrow chr2,2,2",
      "gorrow chr1,1,1")

    case class Listener() extends ScriptExecutionListener {
      var counter1 = 0

      override def beforeExecuteBatch(executionBatch: ExecutionBatch): Unit = {
        if (counter1 == 0) {
          assert(executionBatch.getBlocks.length == 3)
          assert(executionBatch.getBlocks(0).batchGroupName == "xxx")
          assert(executionBatch.getBlocks(1).batchGroupName == "yyy")
          assert(executionBatch.getBlocks(2).batchGroupName == "gorfinal")
        }
        counter1 += 1
      }

      var counter2 = 0

      override def afterExecuteBatch(executionBatch: ExecutionBatch): Unit = {
        if (counter2 == 0) {
          assert(executionBatch.getBlocks.length == 3)
          // We should expect 2 commands as the final gor query has been removed
          assert(executionBatch.getCommands.length == 2)
          assert(executionBatch.getCommands(0).batchGroupName == "[xxx]")
          assert(executionBatch.getCommands(1).batchGroupName == "[yyy]")
        }
        counter2 += 1
      }
    }

    performTest(igorCommands, Listener())
  }

  test("Batch execution with only final query dependencies in gor script") {
    val igorCommands = Array("create xxx = gorrow chr1,1,1",
      "create yyy = gorrow chr2,2,2",
      "gor [xxx] [yyy]")

    case class Listener() extends ScriptExecutionListener {
      var counter1 = 0

      override def beforeExecuteBatch(executionBatch: ExecutionBatch): Unit = {
        if (counter1 == 0) {
          assert(executionBatch.getBlocks.length == 2)
          assert(executionBatch.getBlocks(0).batchGroupName == "xxx")
          assert(executionBatch.getBlocks(1).batchGroupName == "yyy")
        } else if (counter1 == 1) {
          assert(executionBatch.getBlocks.length == 1)
          assert(executionBatch.getBlocks(0).batchGroupName == "gorfinal")
        }
        counter1 += 1
      }

      var counter2 = 0

      override def afterExecuteBatch(executionBatch: ExecutionBatch): Unit = {
        if (counter2 == 0) {
          assert(executionBatch.getBlocks.length == 2)
          assert(executionBatch.getCommands.length == 2)
          assert(executionBatch.getCommands(0).batchGroupName == "[xxx]")
          assert(executionBatch.getCommands(1).batchGroupName == "[yyy]")
        } else if (counter2 == 1) {
          assert(executionBatch.getBlocks.length == 1)
          assert(executionBatch.getCommands.length == 0)
        }
        counter2 += 1
      }
    }

    performTest(igorCommands, Listener())
  }

  test("Batch execution with only multi level query dependencies in gor script") {
    val igorCommands = Array("create xxx = gorrow chr1,1,1",
      "create yyy = gorrow chr2,2,2",
      "create zzz = gor [xxx]",
      "create vvv = gor [yyy]",
      "create ccc = gor [zzz] [vvv] | top 1",
      "gor [ccc]")

    case class Listener() extends ScriptExecutionListener {
      var counter1 = 0

      override def beforeExecuteBatch(executionBatch: ExecutionBatch): Unit = {
        if (counter1 == 0) {
          assert(executionBatch.getBlocks.length == 2)
          assert(executionBatch.getBlocks(0).batchGroupName == "xxx")
          assert(executionBatch.getBlocks(1).batchGroupName == "yyy")
        } else if (counter1 == 1) {
          assert(executionBatch.getBlocks.length == 2)
          assert(executionBatch.getBlocks(0).batchGroupName == "vvv")
          assert(executionBatch.getBlocks(1).batchGroupName == "zzz")
        } else if (counter1 == 2) {
          assert(executionBatch.getBlocks.length == 1)
          assert(executionBatch.getBlocks(0).batchGroupName == "ccc")
        } else if (counter1 == 3) {
          assert(executionBatch.getBlocks.length == 1)
          assert(executionBatch.getBlocks(0).batchGroupName == "gorfinal")
        } else {
          assert(executionBatch.getBlocks.length == 0)
        }
        counter1 += 1
      }

      var counter2 = 0

      override def afterExecuteBatch(executionBatch: ExecutionBatch): Unit = {
        if (counter2 == 0) {
          assert(executionBatch.getBlocks.length == 2)
          assert(executionBatch.getCommands.length == 2)
          assert(executionBatch.getCommands(0).batchGroupName == "[xxx]")
          assert(executionBatch.getCommands(1).batchGroupName == "[yyy]")
        } else if (counter2 == 1) {
          assert(executionBatch.getBlocks.length == 2)
          assert(executionBatch.getCommands.length == 2)
          assert(executionBatch.getCommands(0).batchGroupName == "[vvv]")
          assert(executionBatch.getCommands(1).batchGroupName == "[zzz]")
        } else if (counter2 == 2) {
          assert(executionBatch.getBlocks.length == 1)
          assert(executionBatch.getCommands.length == 1)
          assert(executionBatch.getCommands(0).batchGroupName == "[ccc]")
        } else if (counter2 == 3) {
          assert(executionBatch.getBlocks.length == 1)
          assert(executionBatch.getCommands.length == 0)
        } else {
          assert(executionBatch.getBlocks.length == 0)
          assert(executionBatch.getCommands.length == 0)
        }
        counter2 += 1
      }
    }

    performTest(igorCommands, Listener())
  }

  test("Expansion of gor query should not happen") {
    val igorCommands = Array("gor ../tests/data/gor/genes.gor | top 10")

    case class Listener() extends ScriptExecutionListener {
      override def beforeSplitExpand(commandToExecute: String, splitManager: SplitManager): Unit = {
        assert(commandToExecute == "gor ../tests/data/gor/genes.gor|top 10")
      }

      override def afterSplitExpand(commandToExecute: String, commandGroup: CommandGroup, splitManager: SplitManager): Unit = {
        assert(commandGroup.commandEntries.length == 1)
      }
    }

    performTest(igorCommands, Listener())
  }

  test("Expansion of pgor query with default split") {
    val igorCommands = Array("pgor ../tests/data/gor/genes.gor | top 10")

    case class Listener() extends ScriptExecutionListener {
      var count1 = 0

      override def beforeSplitExpand(commandToExecute: String, splitManager: SplitManager): Unit = {
        if (count1 == 0) {
          assert(commandToExecute == "gor -p ##SPLIT_CHR_REPLACE## <(../tests/data/gor/genes.gor|top 10)")
        } else if (count1 == 1) {
          assert(commandToExecute.startsWith("gordict "))
        }
        count1 += 1
      }

      var count2 = 0

      override def afterSplitExpand(commandToExecute: String, commandGroup: CommandGroup, splitManager: SplitManager): Unit = {
        if (count2 == 0) {
          assert(commandGroup.commandEntries.length == 26)
          assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chr1:0-250000000 <(../tests/data/gor/genes.gor|top 10)"))
        } else if (count2 == 1) {
          assert(commandGroup.commandEntries.length == 1)
          assert(commandGroup.commandEntries.map(x => x.query).mkString("\n").contains("gordict "))
        }
        count2 += 1
      }
    }

    performTest(igorCommands, Listener())
  }

  test("Split Expansion of partgor query with gor subquery") {
    val igorCommands = Array("partgor -parts 5 -dict " + bGordPath + " <(gor -f #{tags} ../tests/data/gor/genes.gor | where 2=2)| top 10")

    case class Listener() extends ScriptExecutionListener {
      var count1 = 0

      override def beforeSplitExpand(commandToExecute: String, splitManager: SplitManager): Unit = {
        if (count1 == 0) {
          assert(commandToExecute == "gor -f a,b ../tests/data/gor/genes.gor|where 2=2 | top 10")
        }
        count1 += 1
      }

      var count2 = 0

      override def afterSplitExpand(commandToExecute: String, commandGroup: CommandGroup, splitManager: SplitManager): Unit = {
        if (count2 == 0) {
          assert(commandGroup.commandEntries.length == 1)
          assert(commandGroup.commandEntries.map(x => x.query).contains("gor -f a,b ../tests/data/gor/genes.gor|where 2=2 | top 10"))
        }
        count2 += 1
      }
    }

    performTest(igorCommands, Listener())
  }

  test("Split expansion of partgor query with pgor subquery") {
    val igorCommands = Array("partgor -parts 5 -dict " + bGordPath + " <(pgor -f #{tags} ../tests/data/gor/genes.gor | where 2=2)| top 10")

    case class Listener() extends ScriptExecutionListener {
      var count1 = 0

      override def beforeSplitExpand(commandToExecute: String, splitManager: SplitManager): Unit = {
        if (count1 == 0) {
          assert(commandToExecute == "gor -p ##SPLIT_CHR_REPLACE## <(-f a,b ../tests/data/gor/genes.gor|where 2=2 | top 10)")
        }
        count1 += 1
      }

      var count2 = 0

      override def afterSplitExpand(commandToExecute: String, commandGroup: CommandGroup, splitManager: SplitManager): Unit = {
        if (count2 == 0) {
          assert(commandGroup.commandEntries.length == 26)
          assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chr1:0-250000000 <(-f a,b ../tests/data/gor/genes.gor|where 2=2 | top 10)"))
        }
        count2 += 1
      }
    }

    performTest(igorCommands, Listener())
  }

  test("Macro expansion of partgor query with pgor subquery") {
    val igorCommands = Array("partgor -parts 5 -dict " + bGordPath + " <(pgor -f #{tags} ../tests/data/gor/genes.gor | where 2=2)| top 10")

    case class Listener() extends ScriptExecutionListener {
      var count1 = 0

      override def beforeSplitExpand(commandToExecute: String, splitManager: SplitManager): Unit = {
        if (count1 == 0) {
          assert(commandToExecute == "gor -p ##SPLIT_CHR_REPLACE## <(-f a,b ../tests/data/gor/genes.gor|where 2=2 | top 10)")
        }
        count1 += 1
      }

      var count2 = 0

      override def afterSplitExpand(commandToExecute: String, commandGroup: CommandGroup, splitManager: SplitManager): Unit = {
        if (count2 == 0) {
          assert(commandGroup.commandEntries.length == 26)
          assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chr1:0-250000000 <(-f a,b ../tests/data/gor/genes.gor|where 2=2 | top 10)"))
        }
        count2 += 1
      }
    }

    performTest(igorCommands, Listener())
  }

  test("Macro expansion of gor") {
    val igorCommands = Array("gor ../tests/data/gor/genes.gor | top 10")

    case class Listener() extends ScriptExecutionListener {
      override def beforeMacroExpand(executionBlocks: ExecutionBlocks): Unit = {
        assert(executionBlocks.size == 1)
        assert(executionBlocks.head._2.query == "gor ../tests/data/gor/genes.gor|top 10")
      }

      override def afterMacroExpand(executionBlocks: ExecutionBlocks): Unit = {
        assert(executionBlocks.size == 1)
        assert(executionBlocks.values.map(x => x.query).toList.contains("gor ../tests/data/gor/genes.gor|top 10"))
      }
    }

    performTest(igorCommands, Listener())
  }

  test("Macro expansion of pgor query") {
    val igorCommands = Array("pgor ../tests/data/gor/genes.gor | top 10")

    case class Listener() extends ScriptExecutionListener {
      override def beforeMacroExpand(executionBlocks: ExecutionBlocks): Unit = {
        assert(executionBlocks.size == 2)
        assert(executionBlocks.head._2.query == "pgor ../tests/data/gor/genes.gor|top 10")
      }

      override def afterMacroExpand(executionBlocks: ExecutionBlocks): Unit = {
        assert(executionBlocks.size == 3)
        assert(executionBlocks.values.map(x => x.query).toList.contains("gor -p ##SPLIT_CHR_REPLACE## <(../tests/data/gor/genes.gor|top 10)"))
      }
    }

    performTest(igorCommands, Listener())
  }

  test("Virtual replacement of queries with no virtual relations") {
    val igorCommands = Array("create xxx = gor ../tests/data/gor/genes.gor | top 10",
      "gor [xxx]")

    case class Listener() extends ScriptExecutionListener {

      var counter1 = 0

      override def beforeVirtualFileReplacement(commandToExecute: String, virtualFileManager: VirtualFileManager): Unit = {
        if (counter1 == 0) {
          assert(commandToExecute == "gor ../tests/data/gor/genes.gor|top 10")
        }
        counter1 += 1
      }

      var counter2 = 0

      override def afterVirtualFileReplacement(commandToExecute: String): Unit = {
        if (counter2 == 0) {
          assert(commandToExecute == "gor ../tests/data/gor/genes.gor|top 10")
        }
        counter2 += 1
      }
    }

    performTest(igorCommands, Listener())
  }

  test("Virtual replacement of queries with virtual relations") {
    val igorCommands = Array("create xxx = gor ../tests/data/gor/genes.gor | top 10",
      "create yyy = gor [xxx]",
      "gor [yyy]")

    case class Listener() extends ScriptExecutionListener {

      var counter1 = 0

      override def beforeVirtualFileReplacement(commandToExecute: String, virtualFileManager: VirtualFileManager): Unit = {
        if (counter1 == 0) {
          assert(commandToExecute == "gor ../tests/data/gor/genes.gor|top 10")
        } else if (counter1 == 1) {
          assert(commandToExecute.contains(".gorz"))
        }
        counter1 += 1
      }

      var counter2 = 0

      override def afterVirtualFileReplacement(commandToExecute: String): Unit = {
        if (counter2 == 0) {
          assert(commandToExecute == "gor ../tests/data/gor/genes.gor|top 10")
        } else if (counter2 == 1) {
          assert(commandToExecute.contains("gor /") && commandToExecute.contains(".gorz"))
        }
        counter2 += 1
      }
    }

    performTest(igorCommands, Listener())
  }

  test("Script with external virtual relations which should fail") {
    val commands = Array("create xxx = gor [grid:foobar] | top 10",
      "gor [xxx]")
    val engine = createScriptExecutionEngine(null)
    val thrown = intercept[GorParsingException](engine.execute(commands))

    assert(thrown.getMessage.startsWith("Unresolved external virtual"))
  }

  test("Script with bad create name should fail") {
    val commands = Array("create [xxx] = gor 1.mem | top 10", "gor [xxx]")
    val engine = createScriptExecutionEngine(null)
    val thrown = intercept[GorParsingException](engine.execute(commands))

    assert(thrown.getMessage.contains("is not a valid name"))
  }
}
