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

import gorsat.Commands.CommandParseUtilities
import gorsat.DynIterator
import gorsat.QueryHandlers.GeneralQueryHandler
import gorsat.process.{GenericSessionFactory, GorPipeCommands, GorPipeMacros, PipeInstance}
import org.gorpipe.exceptions.{GorException, GorParsingException}
import org.gorpipe.test.utils.FileTestUtils
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FunSuite}

@RunWith(classOf[JUnitRunner])
class UTestScriptExecutionEngine extends FunSuite with BeforeAndAfter {

  def createScriptExecutionEngine(): ScriptExecutionEngine = {
    val context = new GenericSessionFactory().create().getGorContext
    val queryHandler = new GeneralQueryHandler(context, false)
    val localQueryHandler = new GeneralQueryHandler(context, false)
    new ScriptExecutionEngine(queryHandler, localQueryHandler, context)
  }

  def performTest(commands: Array[String]): Unit = {
    try {
      val engine = createScriptExecutionEngine()
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
      val engine = createScriptExecutionEngine()
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
      val engine = createScriptExecutionEngine()
      engine.execute(gorCommands)
    }

    assert(throwable != null)
  }

  test("Test alias replacement in script") {
    val commands = Array(
      "def ##foo## = gorrows -p chr1:1-100",
      "def ##bar## = top 10",
      "##foo## | ##bar##"
    )
    val engine = createScriptExecutionEngine()
    val result = engine.execute(commands)

    assert("gorrows -p chr1:1-100|top 10" == result)
  }

  test("Script with external virtual relations which should fail") {
    val commands = Array("create xxx = gor [grid:foobar] | top 10",
      "gor [xxx]")
    val engine = createScriptExecutionEngine()
    val thrown = intercept[GorParsingException](engine.execute(commands))

    assert(thrown.getMessage.startsWith("Unresolved external virtual"))
  }

  test("Script with bad create name should fail") {
    val commands = Array("create [xxx] = gor 1.mem | top 10", "gor [xxx]")
    val engine = createScriptExecutionEngine()
    val thrown = intercept[GorParsingException](engine.execute(commands))

    assert(thrown.getMessage.contains("is not a valid name"))
  }
}
