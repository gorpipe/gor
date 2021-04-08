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

package org.gorpipe.jessica

import gorsat.Commands.{CommandParseUtilities, RowHeader}
import gorsat.process.{PipeInstance, PipeOptions}
import org.gorpipe.gor.model.Row
import org.gorpipe.jessica

class JessicaRunner extends GorQueryHandler {
  var exception: Throwable = _
  var query = ""
  var pipeInfo = List.empty[PipeStepInfo]
  var output = Array.empty[Row]
  var outputHeader: RowHeader = _
  var root = ""

  /**
    * Initializes the runner, to ensure inputChanged is responsive from the start
    */
  def init(options: PipeOptions): Unit = {
    root = options.gorRoot
    val dummyOptions = new PipeOptions
    dummyOptions.parseOptions(Array("gorrow", "1,1"))
    val executionEngine = new JessicaGorExecutionEngine(dummyOptions, null, null)
    val session = executionEngine.createSession()
    val iterator = executionEngine.createIterator(session)
  }

  override def setQuery(input: String): Unit = {
    query = input
  }

  override def parse(): Boolean = {
    exception = null
    pipeInfo = List.empty[PipeStepInfo]
    var currentInput = query
    try {
      val pipeSteps = CommandParseUtilities.quoteSafeSplitAndTrim(query, '|')

      for(i <- pipeSteps.indices) {
        val steps = pipeSteps.slice(0, i + 1)
        currentInput = steps(i)
        val partialCommandLine = steps.mkString("|")
        val partialCommandLineOptions = new PipeOptions
        partialCommandLineOptions.parseOptions(partialCommandLine.split(" "))
        partialCommandLineOptions.gorRoot = root

        val executionEngine = new JessicaGorExecutionEngine(partialCommandLineOptions, null, null)
        val session = executionEngine.createSession()
        val iterator = executionEngine.createIterator(session)

        setOutputHeader(iterator)
        pipeInfo = jessica.PipeStepInfo(pipeSteps(i), outputHeader, iterator.thePipeStep, null) :: pipeInfo
      }
      true
    } catch {
      case ex: Throwable =>
        exception = ex
        pipeInfo = jessica.PipeStepInfo(currentInput, null, null, ex) :: pipeInfo
        false
    }
  }

  override def run(): Boolean = {
    exception = null
    output = Array.empty[Row]
    val options = new PipeOptions
    val cmdLine = query
    options.parseOptions(cmdLine.split(" "))
    options.gorRoot = root

    val executionEngine = new JessicaGorExecutionEngine(options, null, null)
    val session = executionEngine.createSession()
    var iterator: PipeInstance = null
    try {
      iterator = executionEngine.createIterator(session)
      val runner = executionEngine.createRunner(session)
      runner.run(iterator.theInputSource, iterator.thePipeStep)
      output = executionEngine.output.toArray
      setOutputHeader(iterator)
      true
    } catch {
      case e: Throwable =>
        exception = e
        false
    } finally {
      if (iterator != null) {
        iterator.close()
      }
    }
  }

  override def getSteps: Array[PipeStepInfo] = pipeInfo.reverse.toArray

  override def getResults: Array[Row] = output

  override def getException: Throwable = exception

  override def getOutputHeader: RowHeader = {
    outputHeader
  }

  def setOutputHeader(iterator: PipeInstance): Unit = {
    var last = iterator.thePipeStep
    while (last.pipeTo != null) {
      last = last.pipeTo
    }

    outputHeader = if (last.rowHeader != null) last.rowHeader else RowHeader(iterator.getHeader)
  }
}
