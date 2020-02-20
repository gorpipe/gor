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

/**
  * Base class for listening in on the script execution engine. All the main steps in the engine are covered
  * with a start before and after events and the relevelnt data from that execution.
  */
abstract class ScriptExecutionListener {
  def afterMacroExpand(executionBlocks: ExecutionBlocks): Unit = {}
  def beforeMacroExpand(executionBlocks: ExecutionBlocks): Unit = {}

  def afterVirtualFileReplacement(commandToExecute: String): Unit = {}
  def beforeVirtualFileReplacement(commandToExecute: String, virtualFileManager: VirtualFileManager): Unit = {}

  def afterExecution(gorCommand: String, createdFiles: Map[String, String]): Unit = {}
  def beforeExecution(gorCommands: Array[String]): Unit = {}

  def beforeExecuteQueryHandler(executionBatch: ExecutionBatch, dictionaryExecutions: Array[ExecutionCommand], regularExecutions: Array[ExecutionCommand]): Unit = {}
  def afterExecuteQueryHandler(executionBatch: ExecutionBatch, dictionaryExecutions: Array[ExecutionCommand], regularExecutions: Array[ExecutionCommand]): Unit = {}

  def afterSplitExpand(commandToExecute: String, commandGroup: CommandGroup, splitManager: SplitManager): Unit = {}
  def beforeSplitExpand(commandToExecute: String, splitManager: SplitManager): Unit = {}

  def afterExecuteBatch(executionBatch: ExecutionBatch): Unit = {}
  def beforeExecuteBatch(executionBatch: ExecutionBatch): Unit = {}

  def afterPreProcessing(gorCommands: Array[String]): Unit = {}
  def beforePreProcessing(gorCommands: Array[String]): Unit = {}

  def afterAlias(gorCommands: Array[String]): Unit = {}
  def beforeAlias(igorCommands: Array[String]): Unit = {}
}
