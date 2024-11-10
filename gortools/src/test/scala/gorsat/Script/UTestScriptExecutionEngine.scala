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
import gorsat.{DynIterator, TestUtils}
import gorsat.QueryHandlers.GeneralQueryHandler
import gorsat.process.{GenericSessionFactory, GorPipeCommands, GorPipeMacros, PipeInstance}
import org.gorpipe.exceptions.{GorException, GorParsingException}
import org.gorpipe.gor.table.dictionary.gor.GorDictionaryTable
import org.gorpipe.test.GorDictionarySetup
import org.gorpipe.test.utils.FileTestUtils
import org.junit.Assert
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner

import java.nio.file.{Files, Path}
import java.util
import java.util.{List, Map}

@RunWith(classOf[JUnitRunner])
class UTestScriptExecutionEngine extends AnyFunSuite with BeforeAndAfter {

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

  test("Script with virtual relation is not found should fail") {
    val commands = Array(
      "create ##foo## = gorrows -p chr1:1-10",
      "gor [##foo##] [##not_exist_A##] [##not_exist_B##]"
    )
    val engine = createScriptExecutionEngine()
    val thrown = intercept[GorParsingException](engine.execute(commands))
    assert(thrown.getMessage.equals(
      "Could not create the following queries due to virtual dependencies:\n\t gor [##foo##] [##not_exist_A##] [##not_exist_B##]\n" +
        "No reference to virtual file: [##not_exist_A##]\n" +
        "No reference to virtual file: [##not_exist_B##]\n"))
  }

  test("Filesignature of dict folder should changed when thedict is updated") {
    val session = new GenericSessionFactory().create()

    val name = "testDictFolderSignature"
    val workDirPath = Path.of(FileTestUtils.createTempDirectory(name).getCanonicalPath)
    val dictPath = workDirPath.resolve(name + ".gord")

    Files.createDirectories(dictPath)
    val dataFiles: Map[String, List[String]] = GorDictionarySetup.createDataFilesMap(
      name, dictPath, 30, Array(1, 2, 3), 30, "PN", true,
      Array("PN1", "PN2", "PN3", "PN4", "PN5", "PN6", "PN7", "PN8", "PN9", "PN10", "PN11", "PN12", "PN13", "PN14",
        "PN15", "PN16", "PN17", "PN18", "PN19", "PN20", "PN21", "PN22", "PN23", "PN24", "PN25", "PN26", "PN27", "PN28",
        "PN29", "PN30"));
    val table: GorDictionaryTable = TestUtils.createFolderDictionaryWithData(name, workDirPath, dataFiles);

    val originalFingerprint = createScriptExecutionEngine().fileFingerPrint("#gordict#" + dictPath, session)
    assert(originalFingerprint != null)

    dictPath.resolve("thedict.gord").toFile.setLastModified(System.currentTimeMillis() + 10000)
    val updatedFingerprint = createScriptExecutionEngine().fileFingerPrint("#gordict#" + dictPath, session)

    assert(originalFingerprint != updatedFingerprint)
  }

  test("Filesignature of gorif file should changed when the file is updated") {
    val name = "testGorifFileSignature"
    val workDirPath = Path.of(FileTestUtils.createTempDirectory(name).getCanonicalPath).resolve(name)
    Files.createDirectories(workDirPath.resolve("cache"))

    //val session = new GenericSessionFactory().create()
    val args: Array[String] = Array("-gorroot", workDirPath.toString, "-cachedir", workDirPath.resolve("cache").toString, "-requestid", "test")
    val session = TestUtils.createSession(args, null, false)

    val testFile: Path = workDirPath.resolve("test.gor")

    val command: String = "gorif -dh chrom,pos,val " + testFile

    // Use new engine instance to simulate different queries.
    Files.writeString(testFile, "#chrom\tpos\tval\nchr\t1\tA")
    testFile.toFile.setLastModified(System.currentTimeMillis() - 100000)

    val usedFiles: util.List[String] = createScriptExecutionEngine().getUsedFiles(command, session)
    Assert.assertEquals(1, usedFiles.size())

    val fileFingerprint = createScriptExecutionEngine().fileFingerPrint(testFile.toString, session)
    val fileFingerprint2 = createScriptExecutionEngine().fileFingerPrint(testFile.toString, session)
    Assert.assertEquals(fileFingerprint, fileFingerprint2)

    val signature: String = createScriptExecutionEngine().getFileSignatureAndUpdateSignatureMap(session, command, usedFiles)
    val signature2: String = createScriptExecutionEngine().getFileSignatureAndUpdateSignatureMap(session, command, usedFiles)
    Assert.assertEquals(signature, signature2)

    Files.writeString(testFile, "#chrom\tpos\tval\nchr\t1\tB")

    val fileFingerprint3 = createScriptExecutionEngine().fileFingerPrint(testFile.toString, session)
    Assert.assertNotEquals(fileFingerprint, fileFingerprint3)

    val signature3: String = createScriptExecutionEngine().getFileSignatureAndUpdateSignatureMap(session, command, usedFiles)
    Assert.assertNotEquals(signature, signature3)
  }

  test("Filesignature of norif file should changed when the file is updated") {
    val name = "testNorifFileSignature"
    val workDirPath = Path.of(FileTestUtils.createTempDirectory(name).getCanonicalPath).resolve(name)
    Files.createDirectories(workDirPath.resolve("cache"))

    //val session = new GenericSessionFactory().create()
    val args: Array[String] = Array("-gorroot", workDirPath.toString, "-cachedir", workDirPath.resolve("cache").toString, "-requestid", "test")
    val session = TestUtils.createSession(args, null, false)


    val testFile: Path = workDirPath.resolve("test.gor")

    val command: String = "norif -dh chrom,pos,val " + testFile

    // Use new engine instance to simulate different queries.
    Files.writeString(testFile, "#chrom\tpos\tval\nchr\t1\tA")
    testFile.toFile.setLastModified(System.currentTimeMillis() - 100000)

    val usedFiles: util.List[String] = createScriptExecutionEngine().getUsedFiles(command, session)
    Assert.assertEquals(1, usedFiles.size())

    val fileFingerprint = createScriptExecutionEngine().fileFingerPrint(testFile.toString, session)
    val fileFingerprint2 = createScriptExecutionEngine().fileFingerPrint(testFile.toString, session)
    Assert.assertEquals(fileFingerprint, fileFingerprint2)

    val signature: String = createScriptExecutionEngine().getFileSignatureAndUpdateSignatureMap(session, command, usedFiles)
    val signature2: String = createScriptExecutionEngine().getFileSignatureAndUpdateSignatureMap(session, command, usedFiles)
    Assert.assertEquals(signature, signature2)

    Files.writeString(testFile, "#chrom\tpos\tval\nchr\t1\tB")

    val fileFingerprint3 = createScriptExecutionEngine().fileFingerPrint(testFile.toString, session)
    Assert.assertNotEquals(fileFingerprint, fileFingerprint3)

    val signature3: String = createScriptExecutionEngine().getFileSignatureAndUpdateSignatureMap(session, command, usedFiles)
    Assert.assertNotEquals(signature, signature3)
  }


}
