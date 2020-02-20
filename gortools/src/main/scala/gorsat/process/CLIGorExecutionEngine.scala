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

package gorsat.process

import java.io.File
import java.nio.charset.Charset

import gorsat.AnalysisUtilities
import gorsat.Commands.CommandParseUtilities
import gorsat.MacroUtilities.replaceAllAliases
import gorsat.Outputs.{NorStdOut, StdOut}
import org.apache.commons.io.FileUtils
import org.gorpipe.gor.{GorRunner, GorSession, RequestStats}

/**
  * Execution engine for GOR running as command line. This class takes as input the command line options, construct a
  * command line session and executes the command line to the stdout.
  *
  * @param pipeOptions          GorPipe command line options
  * @param whitelistedCmdFiles  File containing whitelisted commands
  * @param securityContext      Security context if needed
  */
class CLIGorExecutionEngine(pipeOptions: PipeOptions, whitelistedCmdFiles:String = null, securityContext:String = null) extends GorExecutionEngine {

  def this(args:Array[String], whitelistedCmdFiles:String, securityContext:String) {
    this(PipeOptions.parseInputArguments(args), whitelistedCmdFiles, securityContext)
  }

  override protected def createSession(): GorSession = {
    val sessionFactory = new CLISessionFactory(pipeOptions)
    sessionFactory.create()
  }

  override protected def createIterator(session: GorSession): PipeInstance = {

    var queryToExecute = pipeOptions.query
    val fileAliasMap = AnalysisUtilities.loadAliases(pipeOptions.aliasFile, session, "gor_aliases.txt")

    AnalysisUtilities.checkAliasNameReplacement(CommandParseUtilities.quoteSafeSplitAndTrim(queryToExecute, ';'), fileAliasMap) //needs a test
    queryToExecute = replaceAllAliases(queryToExecute, fileAliasMap)

    val iterator = new PipeInstance(session.getGorContext)
    iterator.subProcessArguments(queryToExecute, pipeOptions.fileSignature, pipeOptions.virtualFile, pipeOptions.scriptAnalyzer, pipeOptions.stdIn, "")

    var header = iterator.combinedHeader
    if (containsWriteCommand(pipeOptions.query)) header = null

    // Add steps that return the output of the pipe
    iterator.thePipeStep = if (session.getNorContext || iterator.isNorContext) {
      iterator.thePipeStep | NorStdOut(header)
    } else {
      iterator.thePipeStep | StdOut(header)
    }

    iterator
  }

  override protected def createRunner(session: GorSession): GorRunner = {
    new GenericGorRunner()
  }

  override def execute(): Unit = {
    super.execute()
    if (pipeOptions.stats) {
      val rs = session.getEventLogger.asInstanceOf[RequestStats]
      rs.saveToJson()
    }
  }

  private def containsWriteCommand(query:String): Boolean = {
    val entries = CommandParseUtilities.quoteSafeSplit(query, ';')
    var containsWrite = false

    if (entries.nonEmpty) {
      val commands = CommandParseUtilities.quoteSafeSplitAndTrim(entries.last, '|')

      if (commands.nonEmpty) {
        // Need to replace this in the future
        containsWrite = commands.last.toUpperCase().startsWith("WRITE ")
      }
    }

    containsWrite
  }
}
