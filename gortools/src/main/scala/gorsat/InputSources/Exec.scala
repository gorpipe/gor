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

package gorsat.InputSources

import gorsat.Commands.{CommandArguments, InputSourceInfo, InputSourceParsingResult}
import gorsat.Iterators.RowListIterator
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.cli.GorExecCLI
import org.gorpipe.gor.model.{NoValidateRowBase, Row}
import org.gorpipe.gor.session.GorContext
import picocli.CommandLine

import java.io.{ByteArrayOutputStream, PrintStream}
import java.util.stream.{Collectors, IntStream}
import scala.collection.mutable.ListBuffer

/**
 * Execute selected gor <command> commands in NOR context.
 */
class Exec() extends InputSourceInfo("EXEC", CommandArguments("","", 2, ignoreIllegalArguments=true), isNorCommand = true) {

  override def processArguments(context: GorContext, argString: String, iargs: Array[String],
                                args: Array[String]): InputSourceParsingResult = {

    var result = processExec(context, argString, iargs.slice(1, iargs.length), args)

    if (result.size == 0 || !result(0).startsWith("#")) {
      val columns = result(0).split("\t")
      val header = IntStream.rangeClosed(1, columns.length).mapToObj((i: Int) => "col" + i).collect(Collectors.joining("\t"))
      result = result.prepended(header)
    } else {
      // Remove the leading '#' from the header line
      result(0) = result(0).substring(1)
    }

    val header = "ChromNOR\tPosNOR\t" +  result(0)
    val myHeaderLength = header.split("\t").length
    val lineList = new ListBuffer[Row]()
    for (row <- result.slice(1, result.length)) {
      lineList += new NoValidateRowBase("chrN\t0\t" + row, myHeaderLength)
    }

    val inputSource = RowListIterator(lineList.toList)
    inputSource.setHeader(header)
    InputSourceParsingResult(inputSource, header, isNorContext = true)
  }

  def processExec(context: GorContext, argString: String, iargs: Array[String],
                    args: Array[String]): Array[String] = {


    val std_baos = new ByteArrayOutputStream()
    val err_baos = new ByteArrayOutputStream()
    val std_ps = new PrintStream(std_baos, true)
    val err_ps = new PrintStream(std_baos, true)
    var exitCode = -128

    try {
       exitCode = new CommandLine(new GorExecCLI(std_ps, err_ps))
        .setExitCodeExceptionMapper(new CommandLine.IExitCodeExceptionMapper {
          override def getExitCode(e: Throwable): Int = {
            // Don't map exist codes.
            throw new IllegalArgumentException(s"EXEC command: ${argString} failed: ${e.getMessage}", e)
          }
        })
        .setDefaultValueProvider((argSpec: CommandLine.Model.ArgSpec) => {
          if (argSpec.paramLabel() == "<securityContext>") {
            context.getSession.getProjectContext.getFileReader.getSecurityContext
          } else if (argSpec.paramLabel() == "<projectRoot>") {
            context.getSession.getProjectContext.getFileReader.getCommonRoot
          } else {
            null
          }
        })
        .execute(args.slice(1, args.length): _*)
    } finally {
      std_ps.close()
      err_ps.close()
    }
    if (exitCode != 0) {
      throw new GorParsingException(s"EXEC command: ${argString} failed with exit code: ${exitCode} and output:\n${std_baos.toString}\n${err_baos.toString}")
    }

    var rawLines = std_baos.toString
    var stdLines = if (rawLines.nonEmpty) rawLines.substring(rawLines.indexOf("#"), rawLines.length).stripLineEnd.split("\n")
                   else Array[String]()

    stdLines
  }
}
