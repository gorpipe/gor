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

import scala.collection.mutable.ArrayBuffer

case class ExecutionGraph(gorCommands: Array[String]) {
  val blocks: java.util.Map[String, ExecutionBlock] = ScriptExecutionEngine.parseScript(gorCommands)
  var remainingBlocks: java.util.Map[String, ExecutionBlock] = new java.util.HashMap[String, ExecutionBlock]()
  val root: ExecutionBlock = blocks.getOrDefault("[]", null)

  val levels: Array[List[ExecutionBlock]] = buildLevels()


  private def buildLevels(): Array[List[ExecutionBlock]] = {
    val l = ArrayBuffer[List[ExecutionBlock]]()
    if (root != null) {
      l += List(root)
      remainingBlocks = new java.util.HashMap()
      remainingBlocks.remove("[]")
      var nextLevel = buildNextLevel(l(0))
      while (nextLevel.nonEmpty) {
        l += nextLevel
        nextLevel = buildNextLevel(nextLevel)
      }
    }
    l.reverse.toArray
  }

  private def buildNextLevel(current: List[ExecutionBlock]): List[ExecutionBlock] = {
    var nextLevel = List[ExecutionBlock]()
    current.foreach( block => {
      nextLevel = addToNextLevel(block, nextLevel)
    })
    removeInterdependencies(nextLevel)
  }

  private def removeInterdependencies(blocks: List[ExecutionBlock]) = {
    val byName = mapBlocksByName(blocks)
    var noInterdependencies = List[ExecutionBlock]()
    blocks.foreach( block => {
      block.dependencies.foreach(d => {
        byName.get(d) match {
          case Some(eb) => remainingBlocks.put(d, eb)
          case None =>
        }
      })
      if (!remainingBlocks.containsKey("[" + block.groupName + "]")) {
        //case Some(_) =>
        //case None =>
          noInterdependencies = noInterdependencies ++ List(block)
      }
    })
    noInterdependencies
  }

  private def mapBlocksByName(nextLevel: List[ExecutionBlock]) = {
    var byName = Map[String, ExecutionBlock]()
    nextLevel.foreach(block => {
      byName += "[" + block.groupName + "]" -> block
    })
    byName
  }

  private def addToNextLevel(block: ExecutionBlock, nextLevel: List[ExecutionBlock]): List[ExecutionBlock] = {
    remainingBlocks.remove(block.groupName)
    val level = block.dependencies.map(d => {
      if (remainingBlocks.containsKey(d)) {
        remainingBlocks.remove(d)
      } else {
        null
      }
    }).filter( p => p != null).toList
    nextLevel ++ level
  }
}
