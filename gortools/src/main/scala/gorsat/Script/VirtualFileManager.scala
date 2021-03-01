/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
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

import gorsat.Commands.CommandParseUtilities
import gorsat.Script.ScriptExecutionEngine.ExecutionBlocks
import gorsat.Utilities.MacroUtilities
import org.gorpipe.exceptions.GorParsingException
import org.slf4j.{Logger, LoggerFactory}

object VirtualFileManager {
  private val log: Logger = LoggerFactory.getLogger(this.getClass)
}

/**
  * Manager to maintain virtual file relations for a gor script. Supports mapping create statements to cache files
  * and variations of virtual names to the correct virtual file entry (e.g. [xxx], [ xxx] and [xxx ] are all the same)
  */
class VirtualFileManager {

  private var virtualFileMap = Map.empty[String, VirtualFileEntry]
  private val externalVirtualSearchPattern = "\\[.+?:.+?\\]".r

  def add(name: String): VirtualFileEntry = {

    if (name == null || name.isEmpty) throw new GorParsingException(s"Supplied virtual file entry name is empty: $name")

    val groupName = MacroUtilities.getVirtualFileGroupName(name)

    virtualFileMap.get(groupName) match {
      case Some(x) => x
      case None =>
        val entry = VirtualFileEntry("[" + groupName + "]")
        val virtualName = if (name.startsWith("[")) name else "[" + name + "]"
        entry.isExternal = isExternalVirtualFile(virtualName)
        virtualFileMap += (groupName -> entry)
        entry
    }
  }

  def size: Int = virtualFileMap.size

  def get(name: String): Option[VirtualFileEntry] = {
    val groupName = MacroUtilities.getVirtualFileGroupName(name)
    virtualFileMap.get(groupName)
  }

  def addRange(executionBlocks: ExecutionBlocks): Unit = {
    executionBlocks.foreach { executionBlockEntry =>
      add(executionBlockEntry._2)
    }
  }

  def add(executionBlock: ExecutionBlock): Unit = {
    add(executionBlock.groupName)
    addQuery(executionBlock.query)
  }

  def addQuery(query: String): Unit = {
    val virtualFiles = MacroUtilities.virtualFiles(query)

    virtualFiles.foreach { virtualFile =>
      add(virtualFile)
    }
  }

  def getUnusedVirtualFileEntries: Array[VirtualFileEntry] = {
    virtualFileMap.values.filter(x => x.isOriginal).filter(y => y.fileName == null || y.fileName.isEmpty).toArray
  }

  def updateCreatedFile(name: String, fileName: String): Unit = {
    get(name) match {
      case Some(x) => if (fileName != null && fileName.nonEmpty) x.fileName = fileName else throw new GorParsingException(s"Supplied virtual file name is empty: $fileName, for file entry: $name")
      case None => throw new GorParsingException(s"Unable to locate virtual file entry $name for file: $fileName")
    }
  }

  def replaceVirtualFiles(query: String): String = {
    val virtualFileList = MacroUtilities.virtualFiles(query)
    var outStr = query

    virtualFileList.foreach { virtualFile =>
      val name = MacroUtilities.getVirtualFileGroupName(virtualFile)

      virtualFileMap.get(name) match {
        case Some(x) =>
          if (x.fileName != null) {
            outStr = CommandParseUtilities.quoteSafeReplace(outStr, virtualFile, x.fileName)
          }
        case None =>
          VirtualFileManager.log.warn("There was no reference to create statement '{}' in replaceVirtualFiles", virtualFileList)
      }
    }

    outStr
  }

  def getCreatedFiles: Map[String, String] = {
    virtualFileMap.map(x => (x._2.name, x._2.fileName)).filter(y => y._2 != null && y._2.nonEmpty)
  }

  def getExternalVirtualFiles: Array[VirtualFileEntry] = {
    virtualFileMap.values.filter(x => x.isExternal).toArray
  }

  def areDependenciesReady(dependencies: Array[String]): Boolean = {
    dependencies.count(x => get(x).nonEmpty && get(x).get.fileName != null) == dependencies.length
  }

  def setAllAsOriginal(): Unit = {
    virtualFileMap.values.foreach(x => x.isOriginal = true)
  }

  private def isExternalVirtualFile(virtualFileName: String): Boolean = {
    externalVirtualSearchPattern.findFirstIn(virtualFileName).nonEmpty
  }
}
