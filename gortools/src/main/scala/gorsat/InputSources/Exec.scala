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

import gorsat.Commands.{CommandArguments, CommandParseUtilities, InputSourceInfo, InputSourceParsingResult}
import gorsat.Iterators.{CountingNorRowIterator, RowListIterator}
import org.gorpipe.gor.cli.GorExecCLI
import org.gorpipe.gor.cli.link.LinkUpdateCommand
import org.gorpipe.gor.model.{GorCommand, NoValidateRowBase, Row}
import org.gorpipe.gor.session.GorContext
import picocli.CommandLine

import java.io.{ByteArrayOutputStream, PrintStream, PrintWriter}
import scala.collection.mutable.ListBuffer
import scala.util.Using

/**
 * Execute selected gor <command> commands in NOR context.
 */
class Exec() extends InputSourceInfo("EXEC", CommandArguments("","", 2, 10, ignoreIllegalArguments=true), isNorCommand = true) {

  override def processArguments(context: GorContext, argString: String, iargs: Array[String],
                                args: Array[String]): InputSourceParsingResult = {

    val result = processExec(context, argString, iargs.slice(1, iargs.length), args)

    val header = "Chrom\tpos\t" + result(0)
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


    val oldOut = System.out
    val oldErr = System.err
    val std_baos = new ByteArrayOutputStream()
    val err_baos = new ByteArrayOutputStream()
    val std_ps = new PrintStream(std_baos, true)
    val err_ps = new PrintStream(std_baos, true)
    var exitCode = -128

    try {
      System.setOut(std_ps)
      System.setErr(err_ps)
       exitCode = new CommandLine(new GorExecCLI)
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
      System.setOut(oldOut)
      System.setErr(oldErr)
      std_ps.close()
      err_ps.close()
    }

    Array("Status\tStdOut\tStdErr", exitCode + "\t\"" + std_baos.toString.stripLineEnd + "\"\t\"" + err_baos.toString.stripLineEnd + "\"")
  }
}
