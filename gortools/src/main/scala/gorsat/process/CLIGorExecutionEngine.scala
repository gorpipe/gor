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

import gorsat.Commands.{CommandParseUtilities, Processor, RowHeader}
import gorsat.Utilities.MacroUtilities.replaceAllAliases
import gorsat.Outputs.{ColorStdOut, NorColorStdOut, NorStdOut, OutStream, StdOut}
import gorsat.Utilities.{AnalysisUtilities, MacroUtilities}
import org.gorpipe.gor.session.{GorRunner, GorSession}
import org.gorpipe.gor.RequestStats
import org.gorpipe.gor.driver.meta.DataType
import org.gorpipe.gor.util.DataUtil
import org.gorpipe.gor.model.{RowRotatingColorize, RowTypeColorize}

/**
  * Execution engine for GOR running as command line. This class takes as input the command line options, construct a
  * command line session and executes the command line to the stdout.
  *
  * @param pipeOptions          GorPipe command line options
  * @param whitelistedCmdFiles  File containing whitelisted commands
  * @param securityContext      Security context if needed
  */
class CLIGorExecutionEngine(pipeOptions: PipeOptions, whitelistedCmdFiles:String = null, securityContext:String = null) extends GorExecutionEngine {

  def this(args:Array[String], whitelistedCmdFiles:String, securityContext:String) = {
    this(PipeOptions.parseInputArguments(args), whitelistedCmdFiles, securityContext)
  }

  override protected def createSession(): GorSession = {
    val sessionFactory = new CLISessionFactory(pipeOptions, securityContext)
    sessionFactory.create()
  }

  override protected def createIterator(session: GorSession): PipeInstance = {

    var queryToExecute = pipeOptions.query
    val fileAliasMap = AnalysisUtilities.loadAliases(pipeOptions.aliasFile, session, DataUtil.toFile("gor_aliases", DataType.TXT))

    AnalysisUtilities.checkAliasNameReplacement(CommandParseUtilities.quoteSafeSplitAndTrim(queryToExecute, ';'), fileAliasMap) //needs a test
    queryToExecute = replaceAllAliases(queryToExecute, fileAliasMap)

    val iterator = new PipeInstance(session.getGorContext)
    iterator.init(queryToExecute, pipeOptions.stdIn, "", pipeOptions.fileSignature, pipeOptions.virtualFile)

    var instance = iterator
    if (MacroUtilities.isWrite(pipeOptions.query)) instance = null

    iterator.thePipeStep = iterator.thePipeStep |
      createStdOut(session.getNorContext || iterator.isNorContext, pipeOptions.color, iterator)

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

  private def createStdOut(isNor: Boolean, color: String, iterator: PipeInstance): OutStream = {
    val c = color.toLowerCase()

    if (isNor) {
      if (c.startsWith("r")) {
        NorColorStdOut(iterator, new RowRotatingColorize())
      } else if(c.startsWith("t")) {
        NorColorStdOut(iterator, new RowTypeColorize())
      } else {
        NorStdOut(if (iterator == null) null else iterator.getHeader())
      }
    } else {
      if (c.startsWith("r")) {
        ColorStdOut(iterator, new RowRotatingColorize())
      } else if (c.startsWith("t")) {
        ColorStdOut(iterator, new RowTypeColorize())
      } else {
        StdOut(if (iterator == null) null else iterator.getHeader())
      }
    }
  }
}
