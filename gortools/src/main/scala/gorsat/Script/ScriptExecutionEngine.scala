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

import gorsat.Commands.CommandParseUtilities
import gorsat.Commands.CommandParseUtilities.{cleanupQuery, cleanupQueryAndSplit, quoteSafeSplit}
import gorsat.Utilities.MacroUtilities._
import gorsat.Script.ScriptExecutionEngine.ExecutionBlocks
import gorsat.gorsatGorIterator.MapAndListUtilities.singleHashMap
import gorsat.Utilities.{AnalysisUtilities, MacroUtilities, StringUtilities}
import gorsat.process.{GorJavaUtilities, GorPipeMacros, GorPrePipe, PipeInstance}
import gorsat.DynIterator
import org.gorpipe.exceptions.{GorException, GorParsingException, GorResourceException}
import org.gorpipe.gor.session.{GorContext, GorSession}
import org.gorpipe.gor.GorScriptAnalyzer
import org.gorpipe.gor.model.GorParallelQueryHandler
import org.slf4j.{Logger, LoggerFactory}

import java.util.concurrent.ConcurrentHashMap

object ScriptExecutionEngine {
  // Set the dyniterator iterator create function
  DynIterator.createGorIterator = (context: GorContext) => PipeInstance.createGorIterator(context)

  private val log: Logger = LoggerFactory.getLogger(this.getClass)
  type ExecutionBlocks = java.util.Map[String, ExecutionBlock]

  val GOR_FINAL = "gorfinal"
  val INCLUDE_KEYWORD: String = "include"

  def parseScript(commands: Array[String]): java.util.Map[String, ExecutionBlock] = {
    var creates = new ConcurrentHashMap[String, ExecutionBlock]()

    commands.foreach(command => {
      val (a, b) = ScriptParsers.createParser(command)
      if (a != "") {
        val vf = virtualFiles(b)
        val batchGroupName: String = validateCreateName(a)
        creates.put("[" + batchGroupName + "]", ExecutionBlock(batchGroupName, b, null, vf.toArray))
      } else {
        if (creates.contains("[]")) {
          throw new GorParsingException("Only one final command is allowed")
        }
        val batchGroupName = GOR_FINAL
        val vf = virtualFiles(command)
        creates.put("[]",ExecutionBlock(batchGroupName, command, null, vf.toArray))
      }
    })

    creates
  }

  private def validateCreateName(a: String) = {
    val batchGroupName = a.trim
    if (batchGroupName.contains("[") || batchGroupName.contains("]")) {
      throw new GorParsingException(s"'$batchGroupName' is not a valid name")
    }
    batchGroupName
  }
}

/**
  * Class to execute gor scripts. Scripts are executed with the supplied query handler.
  *
  * @param queryHandler Remote query handler
  * @param context      Current gor pipe session
  */
class ScriptExecutionEngine(queryHandler: GorParallelQueryHandler,
                            localQueryHandler: GorParallelQueryHandler,
                            context: GorContext) {

  private var executionBlocks: ExecutionBlocks = new ConcurrentHashMap[String, ExecutionBlock]()
  private var aliases: singleHashMap = new ConcurrentHashMap[String, String]()
  private var fileSignatureMap = new ConcurrentHashMap[String,String]() //Map.empty[String, String]
  private var singleFileSignatureMap = new ConcurrentHashMap[String,String]()
  private val virtualFileManager = new VirtualFileManager

  private val eventLogger = context.getSession.getEventLogger

  def getCreatedFiles: Map[String, String] = {
    virtualFileManager.getCreatedFiles
  }

  def getVirtualFiles: List[(String, String)] = {
    Nil
  }

  def getAliases: singleHashMap = {
    aliases
  }

  def injectIncludes(gorCommands: Array[String], level: Int = 0): Array[String] = {
    if (level>10) throw new GorResourceException("Too many levels of includes, possible circular dependency", gorCommands.filter(q => q.toLowerCase.startsWith("include")).head)
    gorCommands.flatMap(q => {
      val incl = ScriptParsers.includeParser(q.trim)
      if (!incl.equals("")) {
        val includeFileContent = new String(context.getSession.getProjectContext.getFileReader.getInputStream(incl).readAllBytes())
        val argString = CommandParseUtilities.removeComments(includeFileContent)
        val includeCreates = cleanupQueryAndSplit(argString)
        injectIncludes(includeCreates.slice(0, includeCreates.length-1), level+1)
      } else Array(q.trim)
    })
  }

  def executeVirtualFile(virtualFile: String, gorCommands: Array[String]): String = {
    execute(gorCommands, virtualFile = virtualFile)
  }

  def executeSuggestName(gorCommands: Array[String]): String = {
    execute(gorCommands, suggestName = true)
  }

  def execute(commands: Array[String], validate: Boolean = true, suggestName: Boolean = false, virtualFile: String = ""): String = {
    // Apply aliases to query, this replaces the def entries
    aliases = extractAliases(commands)
    var gorCommands = applyAliases(commands, aliases)

    // Include external gor scripts
    gorCommands = injectIncludes(gorCommands)

    // Apply aliases again to query in case includes need alias update, this replaces the def entries
    aliases = extractAliases(gorCommands)
    gorCommands = applyAliases(gorCommands, aliases)

    // Preprocess the script, change macros to create + gor statements
    gorCommands = performScriptPreProcessing(gorCommands)

    // This is some cleanup, is it needed?
    gorCommands = CommandParseUtilities.cleanCommandStrings(gorCommands)

    val analyzer = new GorScriptAnalyzer(gorCommands.mkString(";"))
    eventLogger.tasks(analyzer.getTasks)

    val gorCommand = processScript(gorCommands, validate, suggestName)

    if (virtualFile != "") {
      val temp = virtualFileManager.getCreatedFiles.filter(x => x._1 == ("[" + virtualFile + "]")).toList
      if (temp.isEmpty) return "" else return temp.head._2
    }

    gorCommand
  }

  private def performScriptPreProcessing(gorCommands: Array[String]): Array[String] = {
    var processedGorCommands = gorCommands
    val commands = CommandParseUtilities.quoteSafeSplit(gorCommands.last, '|')
    val commandName = CommandParseUtilities.quoteSafeSplit(commands(0), ' ')(0)

    val info = GorPipeMacros.getInfo(commandName)

    if (info.nonEmpty) {
      processedGorCommands = info.get.preProcessCommand(processedGorCommands, context)
    }

    processedGorCommands
  }

  private def processScript(igorCommands: Array[String], validate: Boolean, suggestName: Boolean): String = {
    // Parse script to execution blocks and a list of all virtual files
    // We collect all execution blocks as they are removed when executed and if there are
    // any left there is an error, something was not executed
    executionBlocks = ScriptExecutionEngine.parseScript(igorCommands)

    // Validate executionblocks for external references
    preValidateExecution()

    // Update virtual file list with initial execution blocks
    virtualFileManager.addRange(executionBlocks)

    // The initial execution blocks need to be flagged as original and will cause an error if any of them are
    // left at the end of execution
    virtualFileManager.setAllAsOriginal()

    var gorCommand = ""
    var level = 0
    var executionBatch: ExecutionBatch = null
    var allUsedFiles: java.util.List[String] = null

    do {
      level += 1

      // Create a new batch of execution blocks which are independent from each other
      executionBatch = getNextBatch(level)
      val (gorCmd,usedFiles) = GorJavaUtilities.processBlocks(context, suggestName, executionBlocks, aliases, fileSignatureMap, singleFileSignatureMap, virtualFileManager, executionBatch, validate, gorCommand)
      gorCommand = gorCmd
      allUsedFiles = usedFiles
      // Execute the current batch
      executeBatch(executionBatch, suggestName)
    } while (executionBatch.hasBlocks)

    // We'll need to validate the current execution and throw exception if there are still execution blocks available
    // IN the final execution list
    if (validate) postValidateExecution(suggestName)

    if (suggestName) gorCommand = StringUtilities.createMD5(igorCommands.mkString(" ") + allUsedFiles.stream.distinct.sorted.map(x => GorJavaUtilities.fileFingerPrint(x, singleFileSignatureMap, context.getSession).mkString(" ")))
    gorCommand
  }

  private def preValidateExecution(): Unit = {
    var externalVirtualRelation: List[String] = Nil
    executionBlocks.values.forEach(block => {
      MacroUtilities.virtualFiles(block.query).foreach { relation =>
        if (MacroUtilities.isExternalVirtFile(relation)) {
          externalVirtualRelation ::= relation
        }
      }
    })

    if (externalVirtualRelation.nonEmpty) {
      throw new GorParsingException(s"Unresolved external virtual relations found: ${externalVirtualRelation.mkString(",")}")
    }
  }

  private def postValidateExecution(suggestName: Boolean): Unit = {
    val unusedEntries = virtualFileManager.getUnusedVirtualFileEntries

    if (unusedEntries.length > 0) {
      // We should warn about unused entries
      unusedEntries.filter(x => !x.name.contains(ScriptExecutionEngine.GOR_FINAL))
        .foreach(entry => ScriptExecutionEngine.log.warn(s"No reference to virtual file: ${entry.name}"))
    }

    if (!executionBlocks.keySet().isEmpty && !suggestName) {
      var message = "Could not create the following queries due to virtual dependencies:\n"
      executionBlocks.keySet().forEach(x => message += "\t" + (x + " = ").replace("[] = ", " ") + executionBlocks.get(x).query.substring(0, Math.min(executionBlocks.get(x).query.length, 50)) + "\n")
      throw new GorParsingException(message)
    }
  }

  private def getNextBatch(level: Int): ExecutionBatch = {
    val executionBatch = ExecutionBatch(level)
    executionBlocks.forEach( (e1,e2) => {
      virtualFileManager.get(e1) match {
        case Some(x) =>
          if (x.fileName == null) {
            createBlockIfAvailable(executionBatch, e1, e2)
          }
        case None =>
          createBlockIfAvailable(executionBatch, e1, e2)
      }
    })

    executionBatch
  }

  private def createBlockIfAvailable(executionBatch: ExecutionBatch, key: String, executionBlock: ExecutionBlock): Unit = {
    val dependencies = executionBlock.dependencies
    if (dependencies.isEmpty || virtualFileManager.areDependenciesReady(dependencies)) {
      executionBatch.createNewBlock(key, executionBlock.query, executionBlock.signature, dependencies, executionBlock.groupName, executionBlock.cachePath)
    }
  }

  private def executeBatch(executionBatch: ExecutionBatch, suggestName: Boolean): Unit = {
    val dictionaryExecutions = executionBatch.getCommands.filter(x => CommandParseUtilities.isDictionaryQuery(x.query))
    val regularExecutions = executionBatch.getCommands.filter(x => !CommandParseUtilities.isDictionaryQuery(x.query))

    if (!suggestName) {
      if (!dictionaryExecutions.isEmpty) runQueryHandler(dictionaryExecutions)
      if (!regularExecutions.isEmpty) runQueryHandler(regularExecutions)
    } else {
      dictionaryExecutions.foreach(x => executionBlocks.remove(x.createName))
      regularExecutions.foreach(x => executionBlocks.remove(x.createName))
    }
  }

  private def runQueryHandler(executionCommands: Array[ExecutionCommand]) {
    if (executionCommands != null && !executionCommands.isEmpty) {
      val activeQueryHandler = if (CommandParseUtilities.isDictionaryQuery(executionCommands.head.query)) localQueryHandler else queryHandler

      val cacheFiles = activeQueryHandler.executeBatch(executionCommands.map(x => x.signature),
        executionCommands.map(x => x.query),
        executionCommands.map(x => x.createName),
        executionCommands.map(x => x.cacheFile),
        context.getSession.getSystemContext.getMonitor).toList

      executionCommands.map(x => x.createName).zip(cacheFiles).foreach(x => {
        virtualFileManager.add(x._1)
        virtualFileManager.updateCreatedFile(x._1, x._2)
        executionBlocks.remove(x._1)
      })

      if (ScriptExecutionEngine.log.isDebugEnabled) {
        executionCommands.foreach { x =>
          ScriptExecutionEngine.log.debug("runQueryHandler input: {} - {} - {} - {}", x.signature, x.query, x.batchGroupName, x.createName)
        }
        getCreatedFiles.foreach(x => {
          if (x._2 != null) {
            ScriptExecutionEngine.log.debug("runQueryHandler createdFiles: {} - {} - {}", x._1, x._2, "")
          }
        })
      }
    }
  }
}
