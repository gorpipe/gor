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

import gorsat.Utilities.AnalysisUtilities.getSignature
import gorsat.Commands.CommandParseUtilities
import gorsat.Utilities.MacroUtilities._
import gorsat.Script.ScriptExecutionEngine.ExecutionBlocks
import gorsat.gorsatGorIterator.MapAndListUtilities.singleHashMap
import gorsat.Utilities.{AnalysisUtilities, MacroUtilities, StringUtilities}
import gorsat.process.{GorPipeMacros, GorPrePipe, PipeInstance}
import gorsat.DynIterator
import org.gorpipe.exceptions.{GorParsingException, GorResourceException}
import org.gorpipe.gor.session.{GorContext, GorSession}
import org.gorpipe.gor.GorScriptAnalyzer
import org.gorpipe.gor.model.GorParallelQueryHandler
import org.slf4j.{Logger, LoggerFactory}

object ScriptExecutionEngine {
  // Set the dyniterator iterator create function
  DynIterator.createGorIterator = (context: GorContext) => PipeInstance.createGorIterator(context)

  private val log: Logger = LoggerFactory.getLogger(this.getClass)
  type ExecutionBlocks = Map[String, ExecutionBlock]

  val GOR_FINAL = "gorfinal"

  def parseScript(commands: Array[String], ignoreDeps: Boolean = false): Map[String, ExecutionBlock] = {
    var creates = Map.empty[String, ExecutionBlock]

    commands.foreach(command => {
      val (a, b) = ScriptParsers.createParser(command)
      if (a != "") {
        val vf = if(ignoreDeps) Array[String]() else virtualFiles(b).toArray
        val batchGroupName: String = validateCreateName(a)
        creates += ("[" + batchGroupName + "]" -> ExecutionBlock(batchGroupName, b, vf))
      } else {
        if (creates.contains("[]")) {
          throw new GorParsingException("Only one final command is allowed")
        }
        val batchGroupName = GOR_FINAL
        val vf = virtualFiles(command)
        creates += ("[]" -> ExecutionBlock(batchGroupName, command, vf.toArray))
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

  private var executionBlocks: ExecutionBlocks = Map.empty[String, ExecutionBlock]
  private var aliases: singleHashMap = new java.util.HashMap[String, String]()
  private var fileSignatureMap = Map.empty[String, String]
  private var singleFileSignatureMap = Map.empty[String, String]
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

  def execute(commands: Array[String], validate: Boolean = true): String = {
    // Apply aliases to query, this replaces the def entries
    aliases = extractAliases(commands)
    var gorCommands = applyAliases(commands, aliases)

    // Preprocess the script, change macros to create + gor statements
    gorCommands = performScriptPreProcessing(gorCommands)

    // This is some cleanup, is it needed?
    gorCommands = CommandParseUtilities.cleanCommandStrings(gorCommands)

    val analyzer = new GorScriptAnalyzer(gorCommands.mkString(";"))
    eventLogger.tasks(analyzer.getTasks)

    val gorCommand = processScript(gorCommands, validate)

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

  private def processScript(igorCommands: Array[String], validate: Boolean): String = {
    // Parse script to execution blocks and a list of all virtual files
    // We collect all execution blocks as they are removed when executed and if there are
    // any left there is an error, something was not executed

    val lastCommand = igorCommands.last
    val lastCommandLower = lastCommand.toLowerCase
    var valid : Boolean = false
    if(context.getAllowMergeQuery && (lastCommandLower.startsWith("select ") || lastCommandLower.startsWith("spark ") || lastCommand.contains("/*+"))) {
      val gorfinal = if(lastCommandLower.startsWith("gor")) "gor [extrafinal]" else "nor [extrafinal]"
      val gorhead = "create extrafinal = "+igorCommands.mkString(";")
      val acmd = Array(gorhead,gorfinal)
      executionBlocks = ScriptExecutionEngine.parseScript(acmd, true)
    } else {
      executionBlocks = ScriptExecutionEngine.parseScript(igorCommands)
      valid = validate
    }

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

    do {
      level += 1

      // Create a new batch of execution blocks which are independent from each other
      executionBatch = getNextBatch(level)
      executionBatch.getBlocks.foreach(firstLevelBlock => {
        // Replace any virtual file in the current query
        firstLevelBlock.query = virtualFileManager.replaceVirtualFiles(firstLevelBlock.query)

        // Expand the executionBlock with macros
        val newExecutionBlocks = expandMacros(Map(firstLevelBlock.groupName -> firstLevelBlock))

        // We need to determine if there is any dependency in the new executions, remove dependent blocks and
        // add them to the executionBlocks map
        val (activeExecutionBlocks, dependentExecutionBlocks) = splitBasedOnDependencies(newExecutionBlocks)

        virtualFileManager.addRange(activeExecutionBlocks)

        activeExecutionBlocks.foreach { newExecutionBlock =>
          // Get the command to finally execute
          val commandToExecute = newExecutionBlock._2.query

          // Extract used files from the final gor command
          val usedFiles = getUsedFiles(commandToExecute)

          // Create the split manager to use from the query (might contain -split option)
          val splitManager = SplitManager.createFromCommand(newExecutionBlock._1, commandToExecute, context)

          // Expand execution blocks based on the active split
          val commandGroup = splitManager.expandCommand(commandToExecute, newExecutionBlock._1)

          // Remove this command from the execution blocks if needed
          if (commandGroup.removeFromCreate) {
            executionBlocks -= firstLevelBlock.groupName
          }

          // Update gorcommand and add new queries if needed
          commandGroup.commandEntries.foreach(cte => {
            if (cte.createName == "[]") {
              // This is the final command, we apply it and remove it from the execution blocks
              gorCommand = cte.query
              executionBlocks -= firstLevelBlock.groupName
            } else {
              // We need to create a new dictionary query to the batch to get the results from expanded queries
              val fileSignature = getFileSignatureAndUpdateSignatureMap(commandToExecute, usedFiles)
              val querySignature = StringUtilities.createMD5(cte.query + fileSignature)
              executionBatch.createNewCommand(querySignature, cte.query, cte.batchGroupName, cte.createName)
              eventLogger.commandCreated(cte.createName, firstLevelBlock.groupName, querySignature, cte.query)
            }
          })
        }

        // Add dictionary entries back to the execution blocks lists but process other entries
        executionBlocks ++= dependentExecutionBlocks
      })

      // Execute the current batch
      executeBatch(executionBatch)
    } while (executionBatch.hasBlocks)

    // We'll need to validate the current execution and throw exception if there are still execution blocks available
    // IN the final execution list
    if (valid) postValidateExecution()

    gorCommand
  }

  private def preValidateExecution(): Unit = {
    var externalVirtualRelation: List[String] = Nil
    executionBlocks.values.foreach { block =>
      MacroUtilities.virtualFiles(block.query).foreach { relation =>
        if (MacroUtilities.isExternalVirtFile(relation)) {
          externalVirtualRelation ::= relation
        }
      }
    }

    if (externalVirtualRelation.nonEmpty) {
      throw new GorParsingException(s"Unresolved external virtual relations found: ${externalVirtualRelation.mkString(",")}")
    }
  }

  private def postValidateExecution(): Unit = {
    val unusedEntries = virtualFileManager.getUnusedVirtualFileEntries

    if (unusedEntries.length > 0) {
      // We should warn about unused entries
      unusedEntries.filter(x => !x.name.contains(ScriptExecutionEngine.GOR_FINAL))
        .foreach(entry => ScriptExecutionEngine.log.warn(s"No reference to virtual file: ${entry.name}"))
    }

    if (executionBlocks.keys.nonEmpty) {
      var message = "Could not create the following queries due to virtual dependencies:\n"
      executionBlocks.keys.foreach(x => message += "\t" + (x + " = ").replace("[] = ", " ") + executionBlocks(x).query.substring(0, Math.min(executionBlocks(x).query.length, 50)) + "\n")
      throw new GorParsingException(message)
    }
  }

  private def getNextBatch(level: Int): ExecutionBatch = {
    val executionBatch = ExecutionBatch(level)
    executionBlocks.foreach(e => {
      virtualFileManager.get(e._1) match {
        case Some(x) =>
          if (x.fileName == null) {
            createBlockIfAvailable(executionBatch, e._1, e._2)
          }
        case None =>
          createBlockIfAvailable(executionBatch, e._1, e._2)
      }
    })

    executionBatch
  }

  private def createBlockIfAvailable(executionBatch: ExecutionBatch, key: String, executionBlock: ExecutionBlock): Unit = {
    val dependencies = executionBlock.dependencies
    if (dependencies.isEmpty || virtualFileManager.areDependenciesReady(dependencies)) {
      executionBatch.createNewBlock(key, executionBlock.query, dependencies, executionBlock.groupName)
    }
  }

  private def splitBasedOnDependencies(executionBlocks: ExecutionBlocks): (ExecutionBlocks, ExecutionBlocks) = {
    var activeExecutionBlocks: ExecutionBlocks = Map.empty[String, ExecutionBlock]
    var dependantExecutionBlocks: ExecutionBlocks = Map.empty[String, ExecutionBlock]

    executionBlocks.foreach { executionBlock =>
      virtualFileManager.get(executionBlock._1) match {
        case Some(x) =>
          if (x.fileName == null) {
            val dependencies = executionBlock._2.dependencies
            if (dependencies.isEmpty || virtualFileManager.areDependenciesReady(dependencies)) {
              activeExecutionBlocks += executionBlock
            } else {
              dependantExecutionBlocks += executionBlock
            }
          }
        case None =>
          val dependencies = executionBlock._2.dependencies
          if (dependencies.isEmpty || virtualFileManager.areDependenciesReady(dependencies)) {
            activeExecutionBlocks += executionBlock
          } else {
            dependantExecutionBlocks += executionBlock
          }
      }
    }

    (activeExecutionBlocks, dependantExecutionBlocks)
  }

  def getUsedFiles(commandToExecute: String): List[String] = {
    if (CommandParseUtilities.isDictionaryQuery(commandToExecute)) {
      // The header does not matter here
      val w = commandToExecute.split(' ')
      var usedFiles: List[String] = Nil
      var i = 1
      while (i < w.length - 1) {
        usedFiles ::= w(i)
        i += 2
      }
      usedFiles.sorted
    } else {
      GorPrePipe.getUsedFiles(commandToExecute, context.getSession).sorted
    }
  }

  def getFileSignatureAndUpdateSignatureMap(commandToExecute: String, usedFiles: List[String]): String = {
    var fileSignature = ""
    if (CommandParseUtilities.isDictionaryQuery(commandToExecute)) {
      fileSignature = StringUtilities.createMD5(usedFiles.mkString(" "))
    } else {
      val signatureKey = getSignature(commandToExecute)
      val fileListKey = usedFiles.mkString(" ") + signatureKey
      fileSignatureMap.get(fileListKey) match {
        case Some(signature) =>
          fileSignature = signature
        case None =>
          fileSignature = StringUtilities.createMD5(usedFiles.map(x => fileFingerPrint(x, context.getSession)).mkString(" ") + signatureKey)
          fileSignatureMap += (fileListKey -> fileSignature)
      }
    }

    fileSignature
  }

  private def executeBatch(executionBatch: ExecutionBatch): Unit = {
    val dictionaryExecutions = executionBatch.getCommands.filter(x => CommandParseUtilities.isDictionaryQuery(x.query))
    val regularExecutions = executionBatch.getCommands.filter(x => !CommandParseUtilities.isDictionaryQuery(x.query))

    if (!dictionaryExecutions.isEmpty) runQueryHandler(dictionaryExecutions)
    if (!regularExecutions.isEmpty) runQueryHandler(regularExecutions)
  }

  private def fileFingerPrint(fileName: String, gorPipeSession: GorSession): String = {
    singleFileSignatureMap.get(fileName) match {
      case Some(signature) =>
        signature
      case None =>
        val signature = if (fileName.startsWith("#gordict#")) {
          getGorDictSignature(gorPipeSession, fileName)
        } else {
          val fileReader = gorPipeSession.getProjectContext.getFileReader
          val cacheDirectory = AnalysisUtilities.theCacheDirectory(gorPipeSession)
          // TODO: Get a gor config instance somehow into gorpipeSession or gorContext to skip using system.getProperty here
          val x_f_name = fileName.split('/').last.split('.').head
          val tmp: String = if (System.getProperty("gor.caching.md5.enabled", "false").toBoolean
            && x_f_name.split('.').head.endsWith("_md5")) {
            // cache files with md5 have the md5 sum encoded in the filename
            return x_f_name.split('.').head
          } else if (fileName.startsWith(cacheDirectory)) {
            return "0"
          } else {
            try {
              fileReader.getFileSignature(fileName)
            } catch {
              case e: Exception => throw new GorResourceException("In fileFingerPrint: file " + fileName + " does not exist!", fileName, e)
            }
          }
          tmp
        }
        singleFileSignatureMap += (fileName -> signature)
        signature
    }
  }

  private def getGorDictSignature(gorPipeSession: GorSession, fileName: String) = {
    val fileReader = gorPipeSession.getProjectContext.getFileReader
    val hasTags = fileName.contains("#gortags#")
    val dictFile = fileName.substring("#gordict#".length, if (hasTags) fileName.indexOf("#gortags#") else fileName.length())
    val dictTags = if (hasTags) fileName.substring(fileName.indexOf("#gortags#") + "#gortags#".length, fileName.length).split(',') else null
    try {
      if (dictTags != null && dictTags.length > 9) {
        fileReader.getFileSignature(dictFile)
      } else {
        fileReader.getDictionarySignature(dictFile, dictTags)
      }
    } catch {
      case e: GorResourceException =>
        throw new GorResourceException(s"Could not get signature for file", dictFile, e)
    }
  }

  private def runQueryHandler(executionCommands: Array[ExecutionCommand]) {
    if (executionCommands != null && !executionCommands.isEmpty) {
      val activeQueryHandler = if (CommandParseUtilities.isDictionaryQuery(executionCommands.head.query)) localQueryHandler else queryHandler

      val cacheFiles = activeQueryHandler.executeBatch(executionCommands.map(x => x.signature),
        executionCommands.map(x => x.query),
        executionCommands.map(x => x.createName),
        context.getSession.getSystemContext.getMonitor).toList

      executionCommands.map(x => x.createName).zip(cacheFiles).foreach(x => {
        virtualFileManager.add(x._1)
        virtualFileManager.updateCreatedFile(x._1, x._2)
        executionBlocks -= x._1
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

  private def expandMacros(creates: ExecutionBlocks): ExecutionBlocks = {

    var activeCreates = creates
    var macroCreated = false

    do {
      var newCreates = Map.empty[String, ExecutionBlock]
      macroCreated = false
      // Go through each command
      activeCreates.foreach { create =>
        // Parse each command and get the macro name
        val commands = CommandParseUtilities.quoteSafeSplit(create._2.query, '|')

        if (commands.length > 0) {
          // Get the macro object and process the executionblock
          val commandOptions = CommandParseUtilities.quoteSafeSplit(commands(0), ' ')
          val macroEntry = GorPipeMacros.getInfo(commandOptions(0))

          if (macroEntry.isEmpty) {
            newCreates += create
          } else {
            macroCreated = true
            val macroResult = macroEntry.get.init(create._1,
              create._2,
              context,
              false,
              commandOptions.slice(1, commandOptions.length))

            newCreates ++= macroResult.createCommands
            if (macroResult.aliases != null) {
              aliases.putAll(macroResult.aliases)
            }
          }
        }
      }

      activeCreates = newCreates

    } while (macroCreated)

    // return the expanded macros
    activeCreates
  }
}
