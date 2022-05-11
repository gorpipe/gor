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

import scala.collection.mutable.ListBuffer

/**
  * Single batch of execuition in the script execution engine. A batch is a collection of non dependent execution blocks
  * from the input script.
  *
  * @param level    Counter indicating the batch number
  */
case class ExecutionBatch(level: Int) {

  private val executionBlocks = new  ListBuffer[ExecutionBlock]
  private val executionCommands = new ListBuffer[ExecutionCommand]

  def createNewBlock(dataSource: String, query: String, signature: String, dependencies: Array[String], sourceName: String, cachePath: String): ExecutionBlock = {
    var block = ExecutionBlock(dataSource, query, signature, dependencies, sourceName, cachePath)
    executionBlocks += block
    block
  }

  def getBlocks: Array[ExecutionBlock] = executionBlocks.toArray
  def hasBlocks: Boolean = executionBlocks.nonEmpty

  def createNewCommand(signature: String, query: String, batchGroupName: String, createName: String, cacheFile: String) : ExecutionCommand = synchronized {
    var command = ExecutionCommand(signature, query, batchGroupName, createName, cacheFile)
    executionCommands += command
    command
  }

  def getCommands: Array[ExecutionCommand] = executionCommands.toArray
  def hasCommands: Boolean = executionCommands.nonEmpty


}