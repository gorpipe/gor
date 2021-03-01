/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
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

import java.io.{File, PrintWriter}
import java.nio.charset.Charset
import java.nio.file.{Files, Path, Paths}
import java.sql.DriverManager

import Commands.CommandParseUtilities
import process.{GenericSessionFactory, GorInputSources, GorPipeCommands, PipeInstance}
import org.apache.commons.io.FileUtils
import org.gorpipe.exceptions.{ExceptionUtilities, GorException, GorUserException}
import org.gorpipe.gor.model.DbSource
import org.gorpipe.gor.session.GorContext
import org.gorpipe.model.gor.iterators.RowSource
import org.gorpipe.test.utils.FileTestUtils
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSuite}
import org.slf4j.LoggerFactory

import scala.collection.mutable.ListBuffer

@RunWith(classOf[JUnitRunner])
class UTestInputSourceParsing extends FunSuite with BeforeAndAfter with MockitoSugar {

  private val logger = LoggerFactory.getLogger(this.getClass)

  case class TestEntry(inputSource:String, arguments: String, testShouldSucceed: Boolean, fromMain: Boolean = true)

  var context: GorContext = _
  val defaultHeaderSNP = "Chrom\tpos"
  val defaultHeaderSEG = "Chrom\tstart\tstop"
  var patientsPathSNP: Path = _
  var patientsPathSEG: Path = _
  var patientsPathSEGWithRefReference: Path = _
  var patientsPathSEGWithAltCallAllele: Path = _
  var patientsPathSEGWithReference: Path = _
  var patientsPathSEGWithAllele: Path = _
  var singleColumnsPath: Path = _
  var twoColumnsPath: Path = _
  var threeColumnsPath: Path = _
  var norColumnsPath: Path = _
  var pnListPath: Path = _

  var ermGordPath = ""

  before {
    System.setProperty("derby.stream.error.field", "MyApp.DEV_NULL")

    DynIterator.createGorIterator_$eq(PipeInstance.createGorIterator)

    // Initialize the command map
    GorInputSources.register()
    GorPipeCommands.register()

    // Initialize session
    val factory = new GenericSessionFactory()
    context = factory.create().getGorContext;

    // Create a test file
    patientsPathSNP = Files.createTempFile("patientSNP", ".gor")
    var outputFile: File = patientsPathSNP.toFile
    outputFile.deleteOnExit()
    var outputWriter: PrintWriter = new PrintWriter(outputFile)
    outputWriter.println("chr\tpos\tvalue\tdate")
    outputWriter.println("chr1\t500\t1.0\t2010-10-3")
    outputWriter.println("chr1\t1000\t2.0\t2010-10-3")
    outputWriter.close()

    patientsPathSEG = Files.createTempFile("patientSEG", ".gor")
    outputFile = patientsPathSEG.toFile
    outputFile.deleteOnExit()
    outputWriter = new PrintWriter(outputFile)
    outputWriter.println("chr\tstart\tstop\tvalue\tdate")
    outputWriter.println("chr1\t500\t1000\t1.0\t2010-10-3")
    outputWriter.println("chr1\t1500\t2000\t2.0\t2010-10-3")
    outputWriter.close()

    // Create a test file
    singleColumnsPath = Files.createTempFile("singlecolumns", ".txt")
    outputFile = singleColumnsPath.toFile
    outputFile.deleteOnExit()
    outputWriter = new PrintWriter(outputFile)
    outputWriter.println("#data")
    outputWriter.println("d1")
    outputWriter.close()

    twoColumnsPath = Files.createTempFile("twocolumns", ".txt")
    outputFile = twoColumnsPath.toFile
    outputFile.deleteOnExit()
    outputWriter = new PrintWriter(outputFile)
    outputWriter.println("#data\tvalue")
    outputWriter.println("d1\t100")
    outputWriter.close()

    Class.forName("org.apache.derby.jdbc.EmbeddedDriver")
    val paths = createTestDataBase_Derby
    System.setProperty("gor.db.credentials", paths(2))
    DbSource.initInConsoleApp()

    var tempDirectory = FileTestUtils.createTempDirectory(this.getClass.getName)
    var ermGord = FileTestUtils.createTempFile(tempDirectory, "erm.gord",
      "genes.gor\tOTHER\tchr1\t1\tchrZ\t1\tbull")
    ermGordPath = ermGord.getCanonicalPath
  }

  def getArguments(x: TestEntry): Array[String] = {
    CommandParseUtilities.quoteSafeSplit(x.arguments, ' ')
  }

  def performTests(testsToPerform: ListBuffer[TestEntry]): Unit = {
    var errorMessage = ""

    testsToPerform.foreach {
      x =>
        val arguments = getArguments(x)
        val inputSourceInfo = GorInputSources.getInfo(x.inputSource)

        if (inputSourceInfo == null) assert(inputSourceInfo == null, "Input source not found!")

        var succeeded = x.testShouldSucceed
        var rowSource: RowSource = null

        try {
          val result = inputSourceInfo.init(context, "", x.inputSource + " " + x.arguments, arguments)
          rowSource = result.inputSource

        } catch {
          case gpe: GorUserException =>
            succeeded = !x.testShouldSucceed
            errorMessage = gpe.getMessage
          case gue: GorException =>
            logger.warn("We do not want to get to this point, all exception should be GorUserException derived. " + gue.getMessage)
            succeeded = !x.testShouldSucceed
            errorMessage = ExceptionUtilities.gorExceptionToString(gue)
          case e: Throwable =>
            // TODO: This should not be a successful route, we need to fail the test here, change when ready
            logger.warn("We should never see his exception when parsing commands.")
            succeeded = !x.testShouldSucceed
            errorMessage = e.getMessage
        } finally {
          if (rowSource != null) {
            rowSource.close()
          }
        }

        assert(succeeded, ":: Command Parsing => " + x.inputSource + " " + x.arguments + (if (errorMessage != "") " => " + errorMessage else ""))
    }
  }

  test("Input Source: GORROW") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val inputSourceName = "GORROW"
    testsToPerform += TestEntry(inputSourceName, "", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "chr1", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "chr1,1", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, "chr1,x", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "1,1", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, "chr1,1,1", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, "chr1,1,x", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "chr1,x,1", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "CHRGG,1,1", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, "CHRGG,100,1", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "1,100,1000", testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Input Source: NORROWS") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val inputSourceName = "NORROWS"
    testsToPerform += TestEntry(inputSourceName, "", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "chr1", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "chr1,1", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "1000", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, "-1", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "-step 1000", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "-step 1000 1000", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, "-step -1000 1000", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "-offset 1000", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "-offset 1000 1000", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, "-offset -1000 1000", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "-step 1000 -offset 1000", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "-step 1000 -offset 1000 1000", testShouldSucceed = true)


    performTests(testsToPerform)
  }

  test("Input Source: CMD") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val inputSourceName = "CMD"

    testsToPerform += TestEntry(inputSourceName, "-n {echo 'foo'}", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, "-n {echo -n 'foo'}", testShouldSucceed = true)

    testsToPerform += TestEntry(inputSourceName, "", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "{echo 'foo'}", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, "-p {echo 'foo'}", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "-p chr1 {echo 'foo'}", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, "-f 'foo' {echo 'foo'}", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, "-p chr1 -f 'foo' {echo 'foo'}", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, "-s {echo 'foo'}", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "-s foo {echo 'foo'}", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "-s gor {echo 'foo'}", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, "-b {echo 'foo'}", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "-b 10 {echo 'foo'}", testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Input Source: GORCMD") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val inputSourceName = "GORCMD"

    testsToPerform += TestEntry(inputSourceName, "-n {echo 'foo'}", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "-n {echo -n 'foo'}", testShouldSucceed = false)

    testsToPerform += TestEntry(inputSourceName, "", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "{echo 'foo'}", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, "-p {echo 'foo'}", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "-p chr1 {echo 'foo'}", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, "-s {echo 'foo'}", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "-s foo {echo 'foo'}", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "-s gor {echo 'foo'}", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, "-b {echo 'foo'}", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "-b 10 {echo 'foo'}", testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Input Source: NORCMD") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val inputSourceName = "NORCMD"

    testsToPerform += TestEntry(inputSourceName, "-n {echo 'foo'}", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "-n {echo -n 'foo'}", testShouldSucceed = false)

    testsToPerform += TestEntry(inputSourceName, "", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "{echo 'foo'}", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, "-p {echo 'foo'}", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "-p chr1 {echo 'foo'}", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, "-s {echo 'foo'}", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "-s foo {echo 'foo'}", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "-s gor {echo 'foo'}", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, "-b {echo 'foo'}", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "-b 10 {echo 'foo'}", testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Input Source: NOR") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val inputSourceName = "NOR"

    testsToPerform += TestEntry(inputSourceName, "", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, singleColumnsPath + " -h -r -asdict", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, singleColumnsPath + " -f PN1,PN2,PN3", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, singleColumnsPath + " -f", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, singleColumnsPath + " -ff", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, singleColumnsPath + " -ff " + singleColumnsPath, testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, "<(nor " + singleColumnsPath + ")", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, "<(gor " + singleColumnsPath + ")", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, "<(cmd " + singleColumnsPath + ")", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, "<(" + singleColumnsPath + ")", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, "<(" + singleColumnsPath + ")asd", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "../tests/data/reports/test.yml", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, "../tests/data/gor/genes.gor", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, "../tests/data/gor/genes.gorz", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, ermGordPath, testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Input Source: GOR") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val inputSourceName = "GOR"

    testsToPerform += TestEntry(inputSourceName, "", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, patientsPathSNP + " -stdin -nf -fs -w -Y -g -q", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, patientsPathSNP + " -stdin -nf -fs -w -Y -g -j -q", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, patientsPathSNP + " -b", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, patientsPathSNP + " -b -10", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, patientsPathSNP + " -b 10", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, patientsPathSNP + " -seek", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, patientsPathSNP + " -seek chr1", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, patientsPathSNP + " -seek chr1:10-10-10", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, patientsPathSNP + " -seek chr1:10-10", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, patientsPathSNP + " -seek chr1:10", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, patientsPathSNP + " -p", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, patientsPathSNP + " -p chr1", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, patientsPathSNP + " -p chr1:10-10", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, patientsPathSNP + " -p chr1:10", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, patientsPathSNP + " -p chr1:-10-100", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, "<(" + patientsPathSNP + ") -parts", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "<(" + patientsPathSNP + ") -parts -10", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "<(" + patientsPathSNP + ") -parts 10", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, "<(" + patientsPathSNP + ") -parts 10 -f", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "<(" + patientsPathSNP + ") -parts 10 -f PN1,PN2,PN3", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "<(" + patientsPathSNP + " -f #{tags}) -parts 10 -f PN1,PN2,PN3", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, "<(" + patientsPathSNP + ") -parts 10 -seek", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "<(" + patientsPathSNP + ") -parts 10 -seek chr1:1000", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, "<(" + patientsPathSNP + ") -parts 10 -p", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "<(" + patientsPathSNP + ") -parts 10 -p chr1:1000-2000", testShouldSucceed = true)

    // Legacy stuff
    testsToPerform += TestEntry(inputSourceName, patientsPathSNP + " -pchr1:10", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, patientsPathSNP + " -pchr1:10 -Zksjdflkjlkjlkjlkljk", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, patientsPathSNP + " -p chr1:10 -Z ksjdflkjlkjlkjlkljk -H 1", testShouldSucceed = true)


    performTests(testsToPerform)
  }

  test("Input Source: SQL") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val inputSourceName = "SQL"

    testsToPerform += TestEntry(inputSourceName, "{foo}", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "-n {foo}", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "-n {select * from rda.variant_annotations}", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, "{select * from rda.variant_annotations}", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, "{select * from foo}", testShouldSucceed = false)

    performTests(testsToPerform)
  }

  test("Input Source: GORSQL") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val inputSourceName = "GORSQL"

    testsToPerform += TestEntry(inputSourceName, "{foo}", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "-n {select * from rda.variant_annotations}", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "{select * from rda.variant_annotations}", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, "{select * from foo}", testShouldSucceed = false)

    performTests(testsToPerform)
  }

  test("Input Source: NORSQL") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val inputSourceName = "NORSQL"

    testsToPerform += TestEntry(inputSourceName, "{foo}", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "-n {select * from rda.variant_annotations}", testShouldSucceed = false)
    testsToPerform += TestEntry(inputSourceName, "{select * from rda.variant_annotations}", testShouldSucceed = true)
    testsToPerform += TestEntry(inputSourceName, "{select * from foo}", testShouldSucceed = false)

    performTests(testsToPerform)
  }

  def createTestDataBase_Derby: Array[String] = {
    Class.forName("org.apache.derby.jdbc.EmbeddedDriver")
    val tmpDirectory = com.google.common.io.Files.createTempDir
    tmpDirectory.deleteOnExit()
    val databasePath = Paths.get(tmpDirectory.getAbsolutePath, "testDB")
    val credentialsPath = Paths.get(tmpDirectory.getAbsolutePath, "gor.derby.credentials")
    val connectionString = "jdbc:derby:" + databasePath.toString + ";create=true"
    // Create test database
    try {
      val connection = DriverManager.getConnection(connectionString)
      try {
        val statement = connection.createStatement
        statement.executeUpdate("CREATE SCHEMA RDA")
        statement.executeUpdate("CREATE TABLE rda.variant_annotations\n(" + "CHROMO VARCHAR(10),\n" + "POS INT,\n" + "PROJECT_ID VARCHAR(30))")
        statement.executeUpdate("INSERT INTO RDA.VARIANT_ANNOTATIONS VALUES\n" + "('chr1',0,'10004')," + "('chr1',1,'10004')," + "('chr1',2,'10004')," + "('chr1',3,'10004')," + "('chr1',4,'10004')")
        statement.executeUpdate("CREATE VIEW rda.v_variant_annotations as select * from rda.variant_annotations")
        statement.close()
      } finally if (connection != null) connection.close()
    } catch {
      case _: Exception => // Do Nothing
    }
    // Create test db configuration
    val dbConfiguration = "name\tdriver\turl\tuser\tpwd\nrda\torg.apache.derby.jdbc.EmbeddedDriver\tjdbc:derby:" + databasePath + "\trda\tbeta3"
    FileUtils.writeStringToFile(credentialsPath.toFile, dbConfiguration, Charset.defaultCharset)
    Array[String](tmpDirectory.getAbsolutePath, databasePath.toString, credentialsPath.toString)
  }
}
