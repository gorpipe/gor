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

import gorsat.Script.ScriptExecutionEngine.ExecutionBlocks
import org.slf4j.{Logger, LoggerFactory}

/**
  * Writes the script engine's state to a log output. Can be used to analyze the execution state of the script engine.
  */
class ConsoleLogListener extends ScriptExecutionListener {

  private val logger: Logger = LoggerFactory.getLogger("console." + this.getClass)

  override def beforeAlias(igorCommands: Array[String]): Unit = {
    logout("Before Alias:")
    igorCommands.foreach(x =>logout(x))
    logout("")
  }

  override def afterAlias(gorCommands: Array[String]): Unit = {
    logout("After Alias:")
    gorCommands.foreach(x => logout(x))
    logout("")
  }

  override def afterPreProcessing(gorCommands: Array[String]): Unit = {
    logout("After Pre Processing:")
    gorCommands.foreach(x =>logout(x))
    logout("")
  }

  override def beforeMacroExpand(executionBlocks: ExecutionBlocks): Unit = {
    logout("Before Macro Expand:")
    executionBlocks.foreach(x => logout(x.toString()))
    logout("")
  }

  override def afterMacroExpand(executionBlocks: ExecutionBlocks): Unit = {
    logout("After Macro Expand:")
    executionBlocks.foreach(x => logout(x.toString()))
    logout("")
  }

  override def beforeExecuteBatch(executionBatch: ExecutionBatch): Unit = {
    logout("Before Execute Batch:")
    logout( "Level: " + executionBatch.level)
    logout("Blocks:")
    executionBatch.getBlocks.foreach(x => logout(x.toString))
    logout("Commands:")
    executionBatch.getCommands.foreach(x => logout(x.toString))
    logout("")
  }

  override def afterExecuteBatch(executionBatch: ExecutionBatch): Unit = {
    logout("After Execute Batch:")
    logout( "Level: " + executionBatch.level)
    logout("Blocks:")
    executionBatch.getBlocks.foreach(x => logout(x.toString))
    logout("Commands:")
    executionBatch.getCommands.foreach(x => logout(x.toString))
    logout("")
  }

  override def beforeVirtualFileReplacement(commandToExecute: String, virtualFileManager: VirtualFileManager): Unit = {
    logout("Before Virtual File Replacement:")
    logout("Command: " + commandToExecute)
    logout("")
  }

  override def afterVirtualFileReplacement(commandToExecute: String): Unit = {
    logout("After Virtual File Replacement:")
    logout("Command: " + commandToExecute)
    logout("")
  }

  private def maxLine(line:String): String = {
    val maxLineSize = 200

    if (line.length > maxLineSize)
      line.substring(0, maxLineSize-3) + "..."
    else
      line
  }

  private def logout(line: String): Unit = {
    logger.info(maxLine(line))
  }
}
