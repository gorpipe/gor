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

import java.io.{File, PrintWriter}
import java.nio.file.{Files, Path}

import org.gorpipe.exceptions.{ExceptionUtilities, GorException}
import Commands.CommandParseUtilities
import process._
import org.apache.commons.io.FileUtils
import org.gorpipe.exceptions.{ExceptionUtilities, GorException, GorUserException}
import org.gorpipe.gor.GorContext
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FunSuite}
import org.slf4j.LoggerFactory

import scala.collection.mutable.ListBuffer

@RunWith(classOf[JUnitRunner])
class UTestCommandParsing extends FunSuite with BeforeAndAfter {

  private val logger = LoggerFactory.getLogger(this.getClass)

  case class TestEntry(command:String, arguments: String, header: String, testShouldSucceed: Boolean, norContext: Boolean = false, description:String = "")

  val context: GorContext = new GenericSessionFactory().create().getGorContext
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

  before {

    DynIterator.createGorIterator_$eq(PipeInstance.createGorIterator)

    // Initialize the command map
    GorPipeCommands.register()

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

    patientsPathSEGWithRefReference = Files.createTempFile("patientSEGWithRef", ".gor")
    outputFile = patientsPathSEGWithRefReference.toFile
    outputFile.deleteOnExit()
    outputWriter = new PrintWriter(outputFile)
    outputWriter.println("chr\tstart\tstop\tvalue\tdate\tref\treference")
    outputWriter.println("chr1\t500\t1000\t1.0\t2010-10-3\tA\tAA")
    outputWriter.close()

    patientsPathSEGWithAltCallAllele = Files.createTempFile("patientSEGWithAllele", ".gor")
    outputFile = patientsPathSEGWithAltCallAllele.toFile
    outputFile.deleteOnExit()
    outputWriter = new PrintWriter(outputFile)
    outputWriter.println("chr\tstart\tstop\tvalue\tdate\tAlt\tCall\tAllele")
    outputWriter.println("chr1\t500\t1000\t1.0\t2010-10-3\tA\tAA\tAAA")
    outputWriter.close()

    patientsPathSEGWithReference = Files.createTempFile("patientSEGWithRef2", ".gor")
    outputFile = patientsPathSEGWithReference.toFile
    outputFile.deleteOnExit()
    outputWriter = new PrintWriter(outputFile)
    outputWriter.println("chr\tstart\tstop\tvalue\tdate\treference")
    outputWriter.println("chr1\t500\t1000\t1.0\t2010-10-3\tAA")
    outputWriter.close()

    patientsPathSEGWithAllele = Files.createTempFile("patientSEGWithAllele2", ".gor")
    outputFile = patientsPathSEGWithAllele.toFile
    outputFile.deleteOnExit()
    outputWriter = new PrintWriter(outputFile)
    outputWriter.println("chr\tstart\tstop\tvalue\tdate\tAllele")
    outputWriter.println("chr1\t500\t1000\t1.0\t2010-10-3\tAAA")
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

    threeColumnsPath = Files.createTempFile("threecolumns", ".txt")
    outputFile = threeColumnsPath.toFile
    outputFile.deleteOnExit()
    outputWriter = new PrintWriter(outputFile)
    outputWriter.println("#data\tvalue\tfoo")
    outputWriter.println("d1\t100\tbar")
    outputWriter.close()

    norColumnsPath = Files.createTempFile("norcolumns", ".txt")
    outputFile = norColumnsPath.toFile
    outputFile.deleteOnExit()
    outputWriter = new PrintWriter(outputFile)
    outputWriter.println("#values\tbucket")
    outputWriter.println("v1\tfoo")
    outputWriter.close()

    pnListPath = Files.createTempFile("pnlist", ".txt")
    outputFile = pnListPath.toFile
    outputFile.deleteOnExit()
    outputWriter = new PrintWriter(outputFile)
    outputWriter.println("PN1,PN2,PN3,PN4")
    outputWriter.close()
  }

  def getArguments(x: TestEntry): Array[String] = {
    CommandParseUtilities.quoteSafeSplit(x.arguments, ' ')
  }

  def performTests(testsToPerform: ListBuffer[TestEntry]): Unit = {
    var errorMessage = ""

    testsToPerform.foreach{
      x =>
        val arguments = getArguments(x)
        val command = GorPipeCommands.getInfo(x.command)

        if (command == null) assert(false, "Command " + x.command + " not found!")

        var succeeded = x.testShouldSucceed

        try {
          val result = command.init(context, x.norContext, x.header, x.command + " " + x.arguments, arguments)
          if (result.step != null) {
            try {
              result.step.securedFinish(null)
            } catch {
              case _:Throwable => // Do nothing
            }
          }
        } catch {
          case gpe:GorUserException =>
            succeeded = !x.testShouldSucceed
            errorMessage = gpe.getMessage
          case gue:GorException =>
            logger.warn("We do not want to get to this point, all exception should be GorUserException derived. " + gue.getMessage)
            succeeded = !x.testShouldSucceed
            errorMessage = ExceptionUtilities.gorExceptionToString(gue)
          case e:Throwable =>
            // TODO: This should not be a successful route, we need to fail the test here, change when ready
            logger.warn("We should never see his exception when parsing commands.\n" + e.getMessage)
            succeeded = !x.testShouldSucceed
            errorMessage = e.getMessage
        }

        assert(succeeded, ":: Command Parsing => " + x.command + " " + x.arguments + (if (errorMessage != "")" => " + errorMessage else ""))
    }
  }

  test("Command: BUG") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "BUG"
    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "process:0.001", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "setup:0.1", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "finish:0.9", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "foo:0.001", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "process:-0.1", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "process:5.1", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "process:bar", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "process:bar -h -g dfssdf", defaultHeaderSNP, testShouldSucceed = false)

    performTests(testsToPerform)
  }

  test("Command: VERIFYORDER") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "VERIFYORDER"
    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "foo -h", defaultHeaderSNP, testShouldSucceed = false)

    performTests(testsToPerform)
  }

  test("Command: VARIANTS") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "VARIANTS"
    val header = defaultHeaderSNP + "\tCIGAR\tQUAL\tSEQ"
    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-count", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-gc 3", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-gc 100,foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-readlength 1000", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-readlength foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-bpmergedist 1000", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-bpmergedist foo", header, testShouldSucceed = false)

    performTests(testsToPerform)
  }

  test("Command: BASES") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "BASES"
    val header = defaultHeaderSNP + "\tCIGAR\tQUAL\tSEQ"
    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-count", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-gc 3", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-gc 100,foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-readlength 1000", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-readlength foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-bpmergedist 10", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-bpmergedist foo", header, testShouldSucceed = false)

    performTests(testsToPerform)
  }

  test("Command: CMD") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "CMD"
    testsToPerform += TestEntry(commandName, "{foo}", "", testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "{foo}", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "{foo.yml}", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "{foo.yml(bar)}", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "{foo.yml?bar=foo}", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "{foo.yml:bar=foo}", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-h -u -e", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "{foo} -h -u -e", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-u {cat <(gor <(gor ../tests/data/gor/genes.gorz) | group chrom -count) <(cmd -u printf 'chrZ\\t0\\t10\\t10\\nchrZ\\t1\\t10\\t10\\n')}", defaultHeaderSNP, testShouldSucceed = true)

    context.getSession.getSystemContext.setServer(true)
    performTests(testsToPerform)
    context.getSession.getSystemContext.setServer(false)
  }

  test("Command: COLNUM") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "COLNUM"
    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-h", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-h foo", defaultHeaderSNP, testShouldSucceed = false)

    performTests(testsToPerform)
  }

  test("Command: COUNTROWS") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "COUNTROWS"
    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-h", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-h foo", defaultHeaderSNP, testShouldSucceed = false)

    performTests(testsToPerform)
  }

  test("Command: DISTINCT") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "DISTINCT"
    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-h", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-h foo", defaultHeaderSNP, testShouldSucceed = false)

    performTests(testsToPerform)
  }

  test("Command: SEGHIST") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "SEGHIST"
    val header ="Chrom\tbpStart\tbpStop\tCount"
    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-100", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "100", header, testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: DISTLOC") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "DISTLOC"
    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "13", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-11", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "foo", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "foo -h", defaultHeaderSNP, testShouldSucceed = false)

    performTests(testsToPerform)
  }

  test("Command: WRITE") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "WRITE"
    val header = defaultHeaderSNP + "\tc1\tc2\tc3"
    testsToPerform += TestEntry(commandName, "foo", "", testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "foo", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "foo -f 3", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "foo_#{fork}.txt -f 3", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "foo -f 3", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "foo -i", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "foo -i NONE", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "foo -i CHROM", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "foo -i FULL", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "foo -i foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "foo -r -c -m", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "foo_#{fork}.txt -r -c -m -f 3 -i FULL", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "foo_#{fork}.txt -r -c -m -f 3 -i FULL -t", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "foo_#{fork}.txt -r -c -m -f 3 -i FULL -t 'foo,bar'", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "foo_#{fork}.txt -r -c -m -f 3 -i FULL -t ''", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "foo_#{fork}.txt -t 'foo,bar'", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "foo -prefix bla", header, true)
    testsToPerform += TestEntry(commandName, "foo -prefix", header, false)

    // "-r -c -m -i", "-f"
    performTests(testsToPerform)

    //Cleanup files created by the write command
    FileUtils.deleteQuietly(new File("foo"))
    FileUtils.deleteQuietly(new File("foo.md5"))
    FileUtils.deleteQuietly(new File("foo_bar.txt"))
    FileUtils.deleteQuietly(new File("foo_bar.txt.md5"))
    FileUtils.deleteQuietly(new File("foo_foo.txt"))
    FileUtils.deleteQuietly(new File("foo_foo.txt.md5"))
  }

  test("Command: ATMAX") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "ATMAX"
    val header = defaultHeaderSNP + "\tc1\tc2\tc3"
    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "chrom", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "chrom c1", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "genome c1", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1000 c1", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-1000 c1", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "chrom c1 c2", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "chrom c1", header, testShouldSucceed = false, norContext = true)
    testsToPerform += TestEntry(commandName, "1000 c1", header, testShouldSucceed = false, norContext = true)
    testsToPerform += TestEntry(commandName, "c1", header, testShouldSucceed = true, norContext = true)
    testsToPerform += TestEntry(commandName, "chrom c1 -last", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "chrom c1 -gc c1,c2", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "chrom c1 -gc c1,foo", header, testShouldSucceed = false)

    performTests(testsToPerform)
  }

  test("Command: ATMIN") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "ATMIN"
    val header = defaultHeaderSNP + "\tc1\tc2\tc3"
    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "chrom", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "chrom c1", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "genome c1", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1000 c1", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-1000 c1", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "chrom c1 c2", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "chrom c1", header, testShouldSucceed = false, norContext = true)
    testsToPerform += TestEntry(commandName, "1000 c1", header, testShouldSucceed = false, norContext = true)
    testsToPerform += TestEntry(commandName, "c1", header, testShouldSucceed = true, norContext = true)
    testsToPerform += TestEntry(commandName, "chrom c1 -last", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "chrom c1 -gc c1,c2", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "chrom c1 -gc c1,foo", header, testShouldSucceed = false)

    performTests(testsToPerform)
  }

  test("Command: BAMFLAG") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "BAMFLAG"
    val header = defaultHeaderSNP + "\tFlag"
    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-v", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-h", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "foo -h", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-h -c -u", header, testShouldSucceed = false)

    performTests(testsToPerform)
  }


  test("Command: BUCKETSPLIT") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "BUCKETSPLIT"
    val header = defaultHeaderSNP + "\tc1\tc2\tc3"

    // Invalid argument counts, header and option settings.
    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "c1 c2", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "c1 c2 c3", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "c1 c2 c3 -b 'x'", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "cxxx 100 -vs 10", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "c1 100 -vs 10", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "c1 100 -vs 10", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "c1 100 -vs", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "c1 100 -s", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "c1 100 -vs -b 'x'", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "c1 100 -s -b 'x'", header, testShouldSucceed = false)

    // Invalid argument combinations or values.
    testsToPerform += TestEntry(commandName, "c1 c2 -b 'x'", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "c1 100 -b 'x'", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "c1 100 -vs 0", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "c1 100 -s 'abc'", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "c1 100 -s abc", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "c1 100 -s ',' -vs 10", header, testShouldSucceed = false)

    // Successful
    testsToPerform += TestEntry(commandName, "c1 1 -s ','", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "c1 100 -vs 10", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "c1 100 -vs 10 -b 'x'", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "c1 100 -s ':'", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "c1 100 -s ':' -b 'x'", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "c1 100 -s :", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "c1 100 -s : -b x", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "c1 100 -s ':' -b 'abc'", header, testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: CALC") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "CALC"
    val header = defaultHeaderSNP + "\tFlag"
    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "newCol", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "13 dsdw", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "newCol,13 ex1,ex2", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "x,y ex1,ex2", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "x,y ex1", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "x ex1,ex2", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "newCol dsdw", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "newCol 1.0", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "newCol 1.0 -1.0 + LISTMAP(Flag,'x < 0')", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "newCol 1.0 -1.0 + LISTMAPXX(Flag,'x < 0')", defaultHeaderSNP, testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: COLSPLIT") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "COLSPLIT"
    val header = defaultHeaderSNP + "\tc1,c2,c3"
    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "3 3 foo", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "4 3 foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "3 8 foo", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "3 3 foo -s", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "3 3 foo -s ;", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "3 3 foo -s ';'", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "3 3 foo -m foo", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "3 3 foo -m 'foo'", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "3 3 foo -o", header, testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: CSVCC") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "CSVCC"
    val header = defaultHeaderSNP + "\tvalues\tbucket"
    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, twoColumnsPath.toString, header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, twoColumnsPath.toString + " " + twoColumnsPath.toString, defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, twoColumnsPath.toString + " " + twoColumnsPath.toString, header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "<(gor " + twoColumnsPath.toString + ") <(gor " + twoColumnsPath.toString + ")", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "<(nor " + twoColumnsPath.toString + ") <(nor " + twoColumnsPath.toString + ")", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "<(nor " + twoColumnsPath.toString + ") <(nor " + twoColumnsPath.toString + ") -gc 1,2", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "<(nor " + twoColumnsPath.toString + ") <(nor " + twoColumnsPath.toString + ") -gc foo,7", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "<(nor " + twoColumnsPath.toString + ") <(nor " + twoColumnsPath.toString + ") -vs 1000", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "<(nor " + twoColumnsPath.toString + ") <(nor " + twoColumnsPath.toString + ") -vs -1000", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "<(nor " + twoColumnsPath.toString + ") <(nor " + twoColumnsPath.toString + ") -vs foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "<(nor " + twoColumnsPath.toString + ") <(nor " + twoColumnsPath.toString + ") -s", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "<(nor " + twoColumnsPath.toString + ") <(nor " + twoColumnsPath.toString + ") -s ;", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "<(nor " + twoColumnsPath.toString + ") <(nor " + twoColumnsPath.toString + ") -s ';'", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "<(nor " + twoColumnsPath.toString + ") <(nor " + twoColumnsPath.toString + ") -u", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "<(nor " + twoColumnsPath.toString + ") <(nor " + twoColumnsPath.toString + ") -u foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "<(nor " + twoColumnsPath.toString + ") <(nor " + twoColumnsPath.toString + ") -u 0", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "<(nor " + twoColumnsPath.toString + ") <(nor " + twoColumnsPath.toString + ") -u 3", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "<(nor " + twoColumnsPath.toString + ") <(nor " + twoColumnsPath.toString + ") -u 4", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "<(nor " + twoColumnsPath.toString + ") <(nor " + twoColumnsPath.toString + ") -probphased -u 3", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "<(nor " + twoColumnsPath.toString + ") <(nor " + twoColumnsPath.toString + ") -probunphased -u 3", header, testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: CSVSEL") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "CSVSEL"
    val header = defaultHeaderSNP + "\tvalues\tbucket"
    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, twoColumnsPath.toString, header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, twoColumnsPath.toString + " " + singleColumnsPath.toString, defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, twoColumnsPath.toString + " " + singleColumnsPath.toString + " -s ','", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "<(gor " + twoColumnsPath.toString + ") <(gor " + singleColumnsPath.toString + ")", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "<(nor " + twoColumnsPath.toString + ") <(nor " + singleColumnsPath.toString + ") -s ','", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "<(nor " + twoColumnsPath.toString + ") <(nor " + singleColumnsPath.toString + ") -s ',' -gc 1,2", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "<(nor " + twoColumnsPath.toString + ") <(nor " + singleColumnsPath.toString + ") -gc foo,7", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "<(nor " + twoColumnsPath.toString + ") <(nor " + singleColumnsPath.toString + ") -vs 1000", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "<(nor " + twoColumnsPath.toString + ") <(nor " + singleColumnsPath.toString + ") -vs -1000", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "<(nor " + twoColumnsPath.toString + ") <(nor " + singleColumnsPath.toString + ") -vs foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "<(nor " + twoColumnsPath.toString + ") <(nor " + singleColumnsPath.toString + ") -s", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "<(nor " + twoColumnsPath.toString + ") <(nor " + singleColumnsPath.toString + ") -s ;", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "<(nor " + twoColumnsPath.toString + ") <(nor " + singleColumnsPath.toString + ") -s ';'", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "<(nor " + twoColumnsPath.toString + ") <(nor " + singleColumnsPath.toString + ") -s ',' -u", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "<(nor " + twoColumnsPath.toString + ") <(nor " + singleColumnsPath.toString + ") -s ',' -u foo", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, twoColumnsPath.toString + " " + singleColumnsPath.toString + " -gc #3,#4 -vs 2 -vcf -threshold 0.9", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, twoColumnsPath.toString + " " + singleColumnsPath.toString + " -gc #3,#4 -s ',' -vcf -threshold 0.9", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, twoColumnsPath.toString + " " + singleColumnsPath.toString + " -gc #3,#4 -vs 1 -vcf -threshold 0.9", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, twoColumnsPath.toString + " " + singleColumnsPath.toString + " -gc #3,#4 -vs 1 -vcf", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, twoColumnsPath.toString + " " + singleColumnsPath.toString + " -gc #3,#4 -s ',' -vcf -threshold 0.9", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, twoColumnsPath.toString + " " + singleColumnsPath.toString + " -gc #3,#4 -s ',' -threshold 0.9", header, testShouldSucceed = false)

    performTests(testsToPerform)
  }

  test("Command: GAVA") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "GAVA"
    val header = defaultHeaderSNP
    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "100", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "100 -caselist PN1,PN2 -ctrllist PN3,PN4", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "foo -caselist PN1,PN2 -ctrllist PN3,PN4", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-100 -caselist PN1,PN2 -ctrllist PN3,PN4", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "100 -casefile " + pnListPath.toString + " -ctrlfile " + pnListPath.toString, defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "100 -caselist PN1,PN2 -ctrllist PN3,PN4 -recessive -dominant", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "100 -caselist PN1,PN2 -ctrllist PN3,PN4 -noMaxAlleleCounts -protective -debug", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "100 -caselist PN1,PN2 -ctrllist PN3,PN4", defaultHeaderSNP + "\tGENE_SYMBOL\tPOS\tALT\tREF\tPN\tCALLCOPIES\tPHASE", testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "100 -caselist PN1,PN2 -ctrllist PN3,PN4", defaultHeaderSNP + "\tGENE\tPOS\tCALL\tREFERENCE\tSUBJECT\tZYGOSITY", testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "100 -caselist PN1,PN2 -ctrllist PN3,PN4", defaultHeaderSNP + "\tGROUP\tPOS\tALLELE\tSCORE", testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: GRANNO") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "GRANNO"
    val header = defaultHeaderSNP + "\tc1\tc2\tc3"
    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "100", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "100 -count", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "foo -count", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "chromo -count", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "genome -count", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1 -range -count -cdist -min -med -max -dis -set -lis -avg -std -sum -h", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1 -gc 3,4 -count", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "1 -gc foo -count", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "1 -gc c1,c2,c3 -count", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1 -gc 3,4 -count", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1 -ac 3,4 -count", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1 -sc 3,4 -count", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1 -ic 3,4 -count", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1 -fc 3,4 -count", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1 -gc 3,4 -count -len foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "1 -gc 3,4 -count -len -1000", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "1 -gc 3,4 -count -len 1000", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1 -gc 3,4 -count -s", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "1 -gc 3,4 -count -s ;", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1 -gc 3,4 -count -s ';'", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1 -gc 3,4 -count -s ';'", header, testShouldSucceed = false, norContext = true)

    performTests(testsToPerform)
  }

  test("Command: GREP") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "GREP"
    val header = defaultHeaderSNP + "\tc1\tc2\tc3"
    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "foo", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "'foo'", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "'foo' -v -s", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "'foo' -s", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "'foo' -c c1,c2", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "'foo' -c c1,foo", header, testShouldSucceed = false)

    performTests(testsToPerform)
  }

  test("Command: GROUP") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "GROUP"
    val header = defaultHeaderSNP + "\tc1\tc2\tc3"
    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "100", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "100", defaultHeaderSNP, testShouldSucceed = false, norContext = true)
    testsToPerform += TestEntry(commandName, "100 -count", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "100 -gc 3,4 -count", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "foo -gc 3,4 -count", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "chromo -gc 3,4 -count", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "genome -gc 3,4 -count", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1 -count -cdist -min -med -max -dis -set -lis -avg -std -sum -h", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "1 -gc 3,4 -count -cdist -min -med -max -dis -set -lis -avg -std -sum -h", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1 -gc 3,4 -count", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "1 -gc foo -count", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "1 -gc c1,c2,c3 -count", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1 -gc 3,4 -count", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1 -ac 3,4 -count", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1 -sc 3,4 -count", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1 -ic 3,4 -count", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1 -fc 3,4 -count", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1 -gc 3,4 -count -len foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "1 -gc 3,4 -count -len -1000", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "1 -gc 3,4 -count -len 1000", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1 -gc 3,4 -count -s", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "1 -gc 3,4 -count -s ;", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1 -gc 3,4 -count -s ';'", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1 -gc 3,4 -count -steps", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "1 -gc 3,4 -count -steps foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "1 -gc 3,4 -count -steps -100", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "1 -gc 3,4 -count -steps 100", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "100 -gc 3,4 -count -ic c2 -sc c3 -fc c1 -max", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "100 -gc 3,4 -count -fc c1 -max", header, testShouldSucceed = false)

    performTests(testsToPerform)
  }

  test("Command: IHE") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "IHE"
    val header = defaultHeaderSEG + "\tMajorAllele\tSecondAllele\tDepth\tAdepth\tCdepth\tGdepth\tTdepth\t" +
      "MajorAllelex\tSecondAllelex\tDepthx\tAdepthx\tCdepthx\tGdepthx\tTdepthx\t" +
      "MajorAllelexx\tSecondAllelexx\tDepthxx\tAdepthxx\tCdepthxx\tGdepthxx\tTdepthxx\t" +
      "GT\tpGT\tLOD\tGT2\t" +
      "GTx\tpGTx\tLODx\tGT2x\t" +
      "GTxx\tpGTxx\tLODxx\tGT2xx"
    val header2 = defaultHeaderSEG + "\tMajorAllele\tSecondAllele\tDepth\tAdepth\tCdepth\tGdepth\tTdepth\t" +
      "MajorAllelex\tSecondAllelex\tDepthx\tAdepthx\tCdepthx\tGdepthx\tTdepthx\t" +
      "MajorAllelexx\tSecondAllelexx\tDepthxx\tAdepthxx\tCdepthxx\tGdepthxx\tTdepthxx"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "", header2, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "ALL", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-v", header, testShouldSucceed = false)

    performTests(testsToPerform)
  }

  test("Command: JOIN") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "JOIN"
    val header = defaultHeaderSEG + "\tvalue"

    /*testsToPerform += TestEntry(commandName, "", "", false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, false)
    testsToPerform += TestEntry(commandName, patientsPathSNP.toString, defaultHeaderSNP, false)
    testsToPerform += TestEntry(commandName, patientsPathSNP.toString + " " + patientsPathSEG.toString, defaultHeaderSNP, false)
    testsToPerform += TestEntry(commandName, patientsPathSNP.toString +  " -snpsnp", defaultHeaderSNP, true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString +  " -snpseg", defaultHeaderSNP, true)
    testsToPerform += TestEntry(commandName, patientsPathSNP.toString +  " -segsnp", defaultHeaderSEG, true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString +  " -segseg", defaultHeaderSEG, true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString +  " -varseg", defaultHeaderSEG, true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString +  " -segvar", defaultHeaderSEG, true)
    testsToPerform += TestEntry(commandName, patientsPathSNP.toString +  " -snpsnp -i -r -c -h -xcis", defaultHeaderSNP, true)
    testsToPerform += TestEntry(commandName, patientsPathSNP.toString +  " -snpsnp -i -m -r -c -h -xcis", defaultHeaderSNP, false)
    testsToPerform += TestEntry(commandName, patientsPathSNP.toString +  " -snpsnp -n -r -c -h -xcis", defaultHeaderSNP, true)
    testsToPerform += TestEntry(commandName, patientsPathSNP.toString +  " -snpsnp -n -m -r -c -h -xcis", defaultHeaderSNP, false)
    testsToPerform += TestEntry(commandName, patientsPathSNP.toString +  " -snpsnp -ic -c -h -xcis", defaultHeaderSNP, true)
    testsToPerform += TestEntry(commandName, patientsPathSNP.toString +  " -snpsnp -ic -ir -c -h -xcis", defaultHeaderSNP, false)
    testsToPerform += TestEntry(commandName, patientsPathSNP.toString +  " -snpsnp -ir -c -h -xcis", defaultHeaderSNP, true)
    testsToPerform += TestEntry(commandName, patientsPathSNP.toString +  " -snpsnp -ir -ic -c -h -xcis", defaultHeaderSNP, false)
    testsToPerform += TestEntry(commandName, patientsPathSNP.toString +  " -snpsnp -l -t", defaultHeaderSNP, true)
    testsToPerform += TestEntry(commandName, patientsPathSNP.toString +  " -varseg -ref", defaultHeaderSEG, false)
    testsToPerform += TestEntry(commandName, patientsPathSNP.toString +  " -varseg", defaultHeaderSEG + "\tREF\tREFERENCE", false)
    testsToPerform += TestEntry(commandName, patientsPathSNP.toString +  " -segvar -ref", defaultHeaderSEG, false)
    testsToPerform += TestEntry(commandName, patientsPathSEGWithRefReference.toString +  " -segvar", defaultHeaderSEG, false)
    testsToPerform += TestEntry(commandName, patientsPathSNP.toString +  " -snpsnp -f", defaultHeaderSNP, false)
    testsToPerform += TestEntry(commandName, patientsPathSNP.toString +  " -snpsnp -f foo", defaultHeaderSNP, false)
    testsToPerform += TestEntry(commandName, patientsPathSNP.toString +  " -snpsnp -f -10", defaultHeaderSNP, false)
    testsToPerform += TestEntry(commandName, patientsPathSNP.toString +  " -snpsnp -f 10", defaultHeaderSNP, true)
    testsToPerform += TestEntry(commandName, patientsPathSNP.toString +  " -snpsnp -p", defaultHeaderSNP, false)
    testsToPerform += TestEntry(commandName, patientsPathSNP.toString +  " -snpsnp -p chr1", defaultHeaderSNP, true)
    testsToPerform += TestEntry(commandName, patientsPathSNP.toString +  " -snpsnp -p chrX:0-1000", defaultHeaderSNP, true)
    testsToPerform += TestEntry(commandName, patientsPathSNP.toString +  " -snpsnp -e", defaultHeaderSNP, false)
    testsToPerform += TestEntry(commandName, patientsPathSNP.toString +  " -snpsnp -e _", defaultHeaderSNP, true)
    testsToPerform += TestEntry(commandName, patientsPathSNP.toString +  " -snpsnp -e '_'", defaultHeaderSNP, true)
    testsToPerform += TestEntry(commandName, patientsPathSNP.toString +  " -snpsnp -xl", defaultHeaderSNP, false)
    testsToPerform += TestEntry(commandName, patientsPathSNP.toString +  " -snpsnp -xl hash -xr value", defaultHeaderSNP + "\thash", true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString +  " -varseg -ref value", defaultHeaderSEG + "\tvalue", true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString +  " -segvar -ref value", defaultHeaderSEG, true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString +  " -varseg -refl value", defaultHeaderSEG + "\tvalue", true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString +  " -segvar -refl value", header, true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString +  " -varseg -lstop stop", header, true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString +  " -segvar -rstop value", header, true)
    testsToPerform += TestEntry(commandName, "<(gor " + patientsPathSNP.toString +  ") -snpsnp", defaultHeaderSNP, true)
    testsToPerform += TestEntry(commandName, patientsPathSNP.toString +  " -snpsnp -s", defaultHeaderSNP, false)*/
    testsToPerform += TestEntry(commandName, patientsPathSNP.toString +  " -snpsnp -s foo", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSNP.toString +  " -snpsnp -o", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSNP.toString +  " -snpsnp -o -2", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSNP.toString +  " -snpsnp -o 2", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSNP.toString +  " -snpsnp -h", defaultHeaderSNP, testShouldSucceed = false)

    // Base: -r -l -i -ic -ir -t -c -n -m -h -xcis  ++ -s -p -f -e -o -lstop -rstop -xl -xr -maxseg -rprefix -ref -refl -refr
    // -i != -l -n -t -o -m
    // -n != -l -i -t -o -m
    // -ic != -l -i -t -o -m -n -ir
    // -ir != -l -i -t -o -m -n -ic
    // varseg != -refl -ref  !(left header contains "REF" and "REFERENCE")
    // segvar != -refr -ref  !(right header contains "REF" and "REFERENCE")

    performTests(testsToPerform)
  }

  // TODO: Fix this, am unable to have this run successfully
  ignore("Command: LDANNO") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "LDANNO"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSNP.toString, defaultHeaderSNP + "\tPOS2", testShouldSucceed = true)

    //"-s -nt -h"

    performTests(testsToPerform)
  }

  ignore("Command: LDJOIN") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "LDJOIN"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)

    performTests(testsToPerform)
  }

  ignore("Command: LIFTOVER") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "LIFTOVER"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString, defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -snp -seg -var -bam", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -snp", defaultHeaderSNP, testShouldSucceed = false)


    // "-snp -seg -var -bam -all", "-ref -alt -build"

    performTests(testsToPerform)
  }

  test("Command: MAP") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "MAP"
    val header = defaultHeaderSEG + "\tjoincol"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString, defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -cartesian", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -c", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -c foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -c joincol", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -c joincol -b -h -e -cis -not", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -c joincol -m", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -c joincol -m foo", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -c joincol -n", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -c joincol -n value,date", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -c joincol -n value date", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "<(gor " +  patientsPathSEG.toString + ") -c joincol", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "<( nor " +  norColumnsPath.toString + ") -c joincol ", header, testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: INSET") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "INSET"
    val header = defaultHeaderSEG + "\tjoincol"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString, defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -cartesian", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -c", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -c foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -c joincol", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -c joincol -b -h -e -cis -not", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -c joincol -m", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -c joincol -m foo", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -c joincol -n", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -c joincol -n value,date", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -c joincol -n value date", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "<(gor " +  patientsPathSEG.toString + ") -c joincol", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "<(nor " +  norColumnsPath.toString + ") -c joincol ", header, testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: MULTIMAP") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "MULTIMAP"
    val header = defaultHeaderSEG + "\tjoincol"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString, defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -cartesian", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -c", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -c foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -c joincol", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -c joincol -b -h -e -cis -not", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -c joincol -m", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -c joincol -m foo", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -c joincol -n", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -c joincol -n value,date", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -c joincol -n value date", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "<(gor " +  patientsPathSEG.toString + ") -c joincol", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "<(nor " +  norColumnsPath.toString + ") -c joincol ", header, testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: DAGMAP") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "DAGMAP"
    val header = defaultHeaderSEG + "\tjoincol"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString, defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -cartesian", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -c", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -c foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -c joincol", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -c joincol -cis", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -c joincol -n", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "<(gor " +  patientsPathSEG.toString + ") -c joincol", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "<(nor " +  norColumnsPath.toString + ") -c joincol ", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -c joincol -dp", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -c joincol -dl", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -c joincol -dl -10", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -c joincol -dl 10", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -c joincol -ps", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -c joincol -ps ;", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -c joincol -ps ';'", header, testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: MERGE") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "MERGE"
    val header = defaultHeaderSEG + "\tjoincol"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString, defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "<(gor " +  patientsPathSEG.toString + ")", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "<(nor " +  patientsPathSEG.toString + ")", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -u -s -i", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -e", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -e ;", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -e ';'", defaultHeaderSNP, testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: PEDPIVOT") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "PEDPIVOT"
    val header = defaultHeaderSEG + "\tvalue\tc1\tc2\tc3"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "value", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "value " + threeColumnsPath.toString, header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "value " + threeColumnsPath.toString + " -v -a", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "value " + threeColumnsPath.toString + " -gc", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "value " + threeColumnsPath.toString + " -gc c1,foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "value " + threeColumnsPath.toString + " -gc c1,c2", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "value " + threeColumnsPath.toString + " -e", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "value " + threeColumnsPath.toString + " -e ;", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "value " + threeColumnsPath.toString + " -e ';'", header, testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: PILEUP") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "PILEUP"
    val header = defaultHeaderSEG + "\tiSize\tSeq\tFlag\tMapQ\tCigar\tQual\tc1\tc2\tc3"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-nf -df -sex -gt -depth", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-p", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-p chr1", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-p chr1:0-10000", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-i", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-i foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-i -10000", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-i 5000", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-q", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-q foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-q -10", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-q 10", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-q 1000", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-bq", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-bq foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-bq -10", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-bq 10", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-bq 1000", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-gc", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-gc c1,foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-gc c1,c2", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-mprob", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-mprob foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-mprob -0.01", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-mprob 0.01", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-span", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-span foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-span -100", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-span 100", header, testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: PIVOT") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "PIVOT"
    val header = defaultHeaderSEG + "\tpivotcol\tc1\tc2"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "foo", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "pivotcol", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "pivotcol -v", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "pivotcol -v v1,v2", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "pivotcol -v v1,v2 -e", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "pivotcol -v v1,v2 -e ;", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "pivotcol -v v1,v2 -e ';'", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "pivotcol -v v1,v2 -gc", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "pivotcol -v v1,v2 -gc c1,foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "pivotcol -v v1,v2 -gc c1,c2", header, testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: RANK") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "RANK"
    val header = defaultHeaderSEG + "\trankcol\tc1\tc2"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "foo rankcol", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "rankcol", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "rankcol", header, testShouldSucceed = true, norContext = true)
    testsToPerform += TestEntry(commandName, "-1000 rankcol", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "foo rankcol", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "1000 rankcol", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "chromo rankcol", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "genome rankcol", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1000 rankcol -q -z -b -c", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1000 rankcol -q -z -b -c", header, testShouldSucceed = false, norContext = true)
    testsToPerform += TestEntry(commandName, "rankcol -q -z -b -c", header, testShouldSucceed = true, norContext = true)
    testsToPerform += TestEntry(commandName, "1000 rankcol -rmax", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "1000 rankcol -rmax -3", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "1000 rankcol -rmax 3", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1000 rankcol -gc", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "1000 rankcol -gc c1,foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "1000 rankcol -gc c1,c2", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1000 rankcol -o", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "1000 rankcol -o foo", header, testShouldSucceed = true) // TODO: This is an error and should not pass, need more type checks
    testsToPerform += TestEntry(commandName, "1000 rankcol -o asc", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1000 rankcol -o desc", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1000 rankcol -o desc", header, testShouldSucceed = false, norContext = true)
    testsToPerform += TestEntry(commandName, "rankcol -o desc", header, testShouldSucceed = true, norContext = true)

    performTests(testsToPerform)
  }

  test("Command: REPLACE") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "REPLACE"
    val header = defaultHeaderSEG + "\treplacecol\tgene"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "replacecol", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "replacecol 1000 - 500", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "replacecol 1.0 -1.0 + LISTMAP(Flag,'x < 0')", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "GENE IF(GENE='0','OUTSIDE_GENES',GENE)", header, testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: SED") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "SED"
    val header = defaultHeaderSEG + "\tc1\tc2\tc3"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "foo", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "foo bar", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "foo bar -i -f", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "foo bar -c", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "foo bar -c c1,foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "foo bar -c c1,2", header, testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: SELECT") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "SELECT"
    val header = defaultHeaderSEG + "\tc1\tc2\tc3"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "c8,1-9", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "c1", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "c1,1,c2 c3", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1-3 c3", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1-3 c3 -sort", header, testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: TRYSELECT") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "TRYSELECT"
    val header = defaultHeaderSEG + "\tc1\tc2\tc3"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "c8,1-9", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "c1", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "c1,1,c2 c3", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1-3 c3", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1-3 c3 -sort", header, testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: HIDE") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "HIDE"
    val header = defaultHeaderSEG + "\tc1\tc2\tc3"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "c8,1-9", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "c1", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "c1,1,c2 c3", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1-3 c3", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1-3 c3 -sort", header, testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: TRYHIDE") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "TRYHIDE"
    val header = defaultHeaderSEG + "\tc1\tc2\tc3"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "c8,1-9", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "c1", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "c1,1,c2 c3", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1-3 c3", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1-3 c3 -sort", header, testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: COLUMNSORT") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "COLUMNSORT"
    val header = defaultHeaderSEG + "\tc1\tc2\tc3"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "c8,1-9", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "c1", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "c1,1,c2 c3", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1-3 c3", header, testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: SELFJOIN") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "SELFJOIN"
    val header = defaultHeaderSEG + "\tc1\tc2\tc3"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-h", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-f", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-f foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-f -1000", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-f 1000", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-x", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-x c1,foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-x c1,c2", header, testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: SEQ") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "SEQ"
    val header = defaultHeaderSEG + "\tc1\tc2\tc3"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-l", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-l foo", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-l -20", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-l 20", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-c", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-c 1-2", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-c c1,foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-c c1,c2", header, testShouldSucceed = true)

    //"-c -l"


    performTests(testsToPerform)
  }

  test("Command: SPLIT") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "SPLIT"
    val header = defaultHeaderSEG + "\tc1\tc2\tc3"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "c1,c2", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1-3,c3", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1-9", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "1-2 3", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "c1,c2 -s", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "c1,c2 -s ,", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "c1,c2 -s ','", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "c1,c2 -s '[,:;]'", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "c1,c2 -e", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "c1,c2 -e _", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "c1,c2 -e '_'", header, testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: TEE") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "TEE"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "foo", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "<(got " + patientsPathSEG.toString + ")", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, ">(TOP 10)", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, ">(TOP 10) -h", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, ">(TOPXXX 10)", defaultHeaderSNP, testShouldSucceed = false)

    performTests(testsToPerform)
  }

  test("Command: UNTIL") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "UNTIL"
    val header = defaultHeaderSEG + "\tc1\tc2\tc3"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "c1 > c2", header, testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: THROWIF") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "THROWIF"
    val header = defaultHeaderSEG + "\tc1\tc2\tc3"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "c1 > c2", header, testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: VARJOIN") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "VARJOIN"
    val header = defaultHeaderSEG + "\tvalue\tdate"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSNP.toString, defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSNP.toString + " " + patientsPathSEG.toString, defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString, header , testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -norm", header , testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -nonorm", header , testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -rprefix", header , testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -rprefix foo", header , testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -maxseg", header , testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -maxseg foo", header , testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -maxseg -10000", header , testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -maxseg 10000", header , testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -span", header , testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -span foo", header , testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -span -500", header , testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -span 500", header , testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -p", header , testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -p chr1", header , testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -p chr1:0-10000", header , testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -e", header , testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -e _", header , testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -e '_'", header , testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -i -l", header , testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "<(gor " +  patientsPathSEG.toString + ")", header , testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -s", header , testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -s date", header , testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -ic", header , testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -ic -ir", header , testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -ir", header , testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -ir -ic", header , testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -xr", header , testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -xl", header , testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -xr foo", header , testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -xl foo", header , testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -xr value -xl value", header , testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -ref", header + "\tREF\tREFERENCE" , testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEGWithRefReference.toString + " -ref", header , testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -alt", header + "\tALT\tCALL\tALLELE" , testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEGWithAltCallAllele.toString + " -alt", header , testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEGWithRefReference.toString + " -refl value -refr value", header  + "\tREFERENCE", testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEGWithAllele.toString + " -altl value -altr value", header  + "\tALLELE", testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -ref", header , testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -ref value", header , testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -alt", header , testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -alt value", header , testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -refl", header , testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -refl value", header , testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -refr", header , testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -refr value", header , testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -altl", header , testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -altl value", header , testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -altr", header , testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -altr value", header , testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, patientsPathSEG.toString + " -xcis -n", header , testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: RANGE") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "RANGE"
    val header = defaultHeaderSEG + "\tc1\tc2\tc3"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "chr1:5000-6000", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "foo bar", header, testShouldSucceed = true) // TODO: This should fail!

    performTests(testsToPerform)
  }

  test("Command: IOTEST") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "IOTEST"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-p", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-v", defaultHeaderSNP, testShouldSucceed = false)

    performTests(testsToPerform)
  }

  test("Command: LEFTWHERE") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "LEFTWHERE"
    val header = defaultHeaderSNP + "\tnewCol\tfoo\tbar"
    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "newCol", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "newCol dsdw", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "newCol 1 > 0", header, testShouldSucceed = true) // NEWCOL CANNOT BE THE LAST COLUMN
    testsToPerform += TestEntry(commandName, "newCol true", header, testShouldSucceed = true)

    performTests(testsToPerform)
  }


  test("Command: LOG") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "LOG"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-t", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-t foo", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-t -1000", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-t 1000", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-l", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-l foo", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-a", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-a trace", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-a debug", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-a info", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-a warn", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-a error", defaultHeaderSNP, testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: LOGLEVEL") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "LOGLEVEL"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "trace", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "debug", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "info", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "warn", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "error", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "error -label", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "error -label foo", defaultHeaderSNP, testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: ROOTLOGLEVEL") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "ROOTLOGLEVEL"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "trace", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "debug", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "info", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "warn", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "error", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "error -label", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "error -label foo", defaultHeaderSNP, testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: VARMERGE") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "VARMERGE"
    val header = defaultHeaderSEG + "\tref\tallele"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "ref", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "ref allele", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "ref allele -seg -nonorm", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "ref allele -span", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "ref allele -span foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "ref allele -span -1000", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "ref allele -span 1000", header, testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: SPAN") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "SPAN"
    val header = defaultHeaderSEG + "\tc1\tc2\tc3"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-gc", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-gc c1,foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-gc c1,c2", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-gc 1-3", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-maxseg", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-maxseg foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-maxseg -5000", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-maxseg 5000", header, testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: SEGSPAN") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "SEGSPAN"
    val header = defaultHeaderSEG + "\tc1\tc2\tc3"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-gc", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-gc c1,foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-gc c1,c2", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-gc 1-3", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-maxseg", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-maxseg foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-maxseg -5000", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-maxseg 5000", header, testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: ROWNUM") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "ROWNUM"
    val header = defaultHeaderSEG + "\tc1\tc2\tc3"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-v", header, testShouldSucceed = false)

    performTests(testsToPerform)
  }

  test("Command: SEGPROJ") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "SEGPROJ"
    val header = defaultHeaderSEG + "\tc1\tc2\tc3"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSEG, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-gc", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-gc c1,foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-gc c1,c2", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-sumcol", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-sumcol c1", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-sumcol c1,c2", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-f", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-f foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-f -10", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-f 10", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-maxseg", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-maxseg foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-maxseg -10", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-maxseg 10", header, testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: SKIP") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "SKIP"
    val header = defaultHeaderSEG + "\tc1\tc2\tc3"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-1000", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1000", header, testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: SORT") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "SORT"
    val header = defaultHeaderSEG + "\tc1\tc2\tc3"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "1000", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1000", header, testShouldSucceed = false, norContext = true)
    testsToPerform += TestEntry(commandName, "foo -c c1", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "chromo -c c1", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "genome -c c1", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1000 -c c1", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-1000 -c c1", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "1000 -c", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "1000 -c c1,foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "1000 -c c1,c2", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1000 -c c1,c2", header, testShouldSucceed = false, norContext = true)
    testsToPerform += TestEntry(commandName, "-c c1,c2", header, testShouldSucceed = true, norContext = true)

    performTests(testsToPerform)
  }

  test("Command: TOP") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "TOP"
    val header = defaultHeaderSEG + "\tc1\tc2\tc3"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-h", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-1000", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1000", header, testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: FIRST") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "FIRST"
    val header = defaultHeaderSEG + "\tc1\tc2\tc3"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-h", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-1000", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "1000", header, testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: UNPIVOT") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "UNPIVOT"
    val header = defaultHeaderSEG + "\tc1\tc2\tc3"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-h", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "1-2", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "c1", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "c1,foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "c1,c2,c3", header, testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: UPTO") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "UPTO"
    val header = defaultHeaderSEG + "\tc1\tc2\tc3"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "chr1", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "chr1:foo", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "chr1:foo", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "chr1:10000", defaultHeaderSNP, testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: VARNORM") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "VARNORM"
    val header = defaultHeaderSEG + "\tref\tallele"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "ref", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "ref allele", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "ref allele -seg -left -right -trim", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "ref allele -seg -left -trim", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "ref allele -seg -right -trim", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "ref allele -span", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "ref allele -span foo", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "ref allele -span -1000", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "ref allele -span 1000", header, testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: WAIT") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "WAIT"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "foo", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-1000", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "1000", defaultHeaderSNP, testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: WHERE") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "WHERE"
    val header = defaultHeaderSNP + "\tvalue\tdata"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "foo", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "value", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "value = 1 or data > 0.0", header, testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: TRYWHERE") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "TRYWHERE"
    val header = defaultHeaderSNP + "\tvalue\tdata"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "foo", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "value", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "value = 1 or data > 0.0", header, testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: VALIDATECOLUMNS") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "VALIDATECOLUMNS"
    val header = defaultHeaderSNP + "\tvalue\tdata"

    testsToPerform += TestEntry(commandName, "", "", testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "", defaultHeaderSNP, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "foo", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-n", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-n 100", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "-n 0", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-n -100", header, testShouldSucceed = false)

    performTests(testsToPerform)
  }

  test("Command: REGRESSION") {
    var testsToPerform = ListBuffer.empty[TestEntry]
    // Initialize all the tests here
    val commandName = "REGRESSION"
    val header = "CHROM\tPOS\tREF\tALT\tVALUES"

    testsToPerform += TestEntry(commandName, "", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "-linear", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "pheno.tsv -linear -imp -covar cov.tsv", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "pheno.tsv -linear -imp -covar cov.tsv -s \",\"", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "pheno.tsv -linear -covar cov.tsv", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "pheno.tsv -linear -covar cov.tsv -s \",\"", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "pheno.tsv -linear -imp", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "pheno.tsv -linear -imp -s \",\"", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "pheno.tsv -linear", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "pheno.tsv -linear -s \",\"", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "pheno.tsv -logistic -imp -covar cov.tsv", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "pheno.tsv -logistic -imp -covar cov.tsv -s \",\"", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "pheno.tsv -logistic -covar cov.tsv", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "pheno.tsv -logistic -covar cov.tsv -s \",\"", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "pheno.tsv -logistic -imp", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "pheno.tsv -logistic -imp -s \",\"", header, testShouldSucceed = false)
    testsToPerform += TestEntry(commandName, "pheno.tsv -logistic", header, testShouldSucceed = true)
    testsToPerform += TestEntry(commandName, "pheno.tsv -logistic -s \",\"", header, testShouldSucceed = true)
    performTests(testsToPerform)
  }

  test(s"Command: REGSEL") {
    var testsToPerform = ListBuffer.empty[TestEntry]

    testsToPerform += TestEntry("REGSEL", "", "", testShouldSucceed = false)
    testsToPerform += TestEntry("REGSEL", "dest", "", testShouldSucceed = false)
    testsToPerform += TestEntry("REGSEL", "dest source", "source", testShouldSucceed = false)
    testsToPerform += TestEntry("REGSEL", "dest source expr extra", "source", testShouldSucceed = false)
    testsToPerform += TestEntry("REGSEL", "dest wrong_source expr", "source", testShouldSucceed = false)

    testsToPerform += TestEntry("REGSEL", "dest source expr", "source", testShouldSucceed = true)
    testsToPerform += TestEntry("REGSEL", "col1,col2,col3 source expr", "source", testShouldSucceed = true)
    testsToPerform += TestEntry("REGSEL", "col1,col2,col3 source \"x|y\"", "source", testShouldSucceed = true)

    performTests(testsToPerform)
  }

  test("Command: BINARYWRITE") {
    var testsToPerform = ListBuffer.empty[TestEntry]

    val header = "CHROM\tPOS\tREF\tALT\tVALUES"
    val badHeader = "CHROM\tPOS\tREF\tVALUES"
    val anotherBadHeader = "CHROM\tPOS\tALT\tVAlUES"
    val yetAnotherBadHeader = "CHROM\tPOS\tREF\tALT"

    testsToPerform += TestEntry("BINARYWRITE", "", "", testShouldSucceed = false)
    testsToPerform += TestEntry("BINARYWRITE", "blabla.pgen", header, testShouldSucceed = true)
    testsToPerform += TestEntry("BINARYWRITE", "blabla.pgen -imp", header, testShouldSucceed = true)
    testsToPerform += TestEntry("BINARYWRITE", "blabla.pgen -imp -threshold 0.9", header, testShouldSucceed = true)
    testsToPerform += TestEntry("BINARYWRITE", "blabla.pgen", badHeader, false)
    testsToPerform += TestEntry("BINARYWRITE", "blabla.pgen", anotherBadHeader, false)
    testsToPerform += TestEntry("BINARYWRITE", "blabla.pgen", yetAnotherBadHeader, false)

    performTests(testsToPerform)
  }

  test("Command: PLINKREGRESSION") {
    var testsToPerform = ListBuffer.empty[TestEntry]

    val header = "CHROM\tPOS\tRSID\tREF\tALT\tVALUES"
    val badHeader = "CHROM\tPOS\tALT\tVALUES"

    testsToPerform += TestEntry("PLINKREGRESSION", "", "", testShouldSucceed = false)
    testsToPerform += TestEntry("PLINKREGRESSION", "pheno.tsv", header, testShouldSucceed = true)
    testsToPerform += TestEntry("PLINKREGRESSION", "pheno.tsv", badHeader, testShouldSucceed = false)
    testsToPerform += TestEntry("PLINKREGRESSION", "pheno.tsv -covar covars.tsv", header, testShouldSucceed = true)
    testsToPerform += TestEntry("PLINKREGRESSION", "pheno.tsv -covar covars.tsv -imp", header, testShouldSucceed = true)
    testsToPerform += TestEntry("PLINKREGRESSION", "pheno.tsv -covar covars.tsv -imp -threshold 0.8", header, testShouldSucceed = true)
    testsToPerform += TestEntry("PLINKREGRESSION", "pheno.tsv -covar covars.tsv -threshold 0.8", header, testShouldSucceed = false)

    performTests(testsToPerform)
  }

  test("Command: COLS2LIST") {
    var testsToPerform = ListBuffer.empty[TestEntry]

    // No arguments
    testsToPerform += TestEntry("COLS2LIST", "", "", testShouldSucceed = false)

    // Simple case
    testsToPerform += TestEntry("COLS2LIST", "a,b,c collapsed", "a\tb\tc\td\te", testShouldSucceed = true)

    // Keep option
    testsToPerform += TestEntry("COLS2LIST", "a,b,c collapsed -gc d", "a\tb\tc\td\te", testShouldSucceed = true)

    // Separator option
    testsToPerform += TestEntry("COLS2LIST", "a,b,c collapsed -sep ';'", "a\tb\tc\td\te", testShouldSucceed = true)

    // Keep and separator options
    testsToPerform += TestEntry("COLS2LIST", "a,b,c collapsed -sep ';' -gc d,e", "a\tb\tc\td\te", testShouldSucceed = true)

    // Too many arguments
    testsToPerform += TestEntry("COLS2LIST", "a,b,c d,e,f collapsed", "a\tb\tc\td\te", testShouldSucceed = false)

    performTests(testsToPerform)
  }

  test("Command: Adjust") {
    var testsToPerform = ListBuffer.empty[TestEntry]

    val header = "CHROM\tPOS\tOTHER\tSTAT"

    testsToPerform += TestEntry("ADJUST", "", header, testShouldSucceed = false)
    testsToPerform += TestEntry("ADJUST", "-gcc -gc 3 -pc 4", header, testShouldSucceed = true)

    performTests(testsToPerform)
  }
}