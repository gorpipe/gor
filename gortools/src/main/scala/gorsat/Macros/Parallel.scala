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

package gorsat.Macros

import gorsat.Commands.{CommandArguments, CommandParseUtilities}
import gorsat.Script.{ExecutionBlock, MacroInfo, MacroParsingResult, ScriptParsers}
import gorsat.Utilities.MacroUtilities
import gorsat.process.{GorInputSources, GorJavaUtilities, GorPipeMacros, SourceProvider}
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.session.GorContext
import org.gorpipe.gor.table.util.PathUtils

import java.util

/**
  * PArallel macro is used to create generic paralisation of gor commands. Parallel takes in a split file or nested
  * query through the -parts option. Each row in the split file will create a parallel execution of the resulting
  * gor query. Executed query can be instantiated with #{column_name} replacement operator.
  *
  * <p><p>Example:
  * <p> parallel -parts [somesplitfile] <(gor data.gor | where count > #{mincount})
  */
class Parallel extends MacroInfo("PARALLEL", CommandArguments("-gordfolder", "-parts -limit", 1, 1)) {

  override protected def processArguments(createKey: String,
                                          create: ExecutionBlock,
                                          context: GorContext,
                                          doHeader: Boolean,
                                          inputArguments: Array[String],
                                          options: Array[String],
                                          skipCache: Boolean): MacroParsingResult = {

    val cmdToModify = if (CommandParseUtilities.isNestedCommand(inputArguments(0))) {
      CommandParseUtilities.parseNestedCommand(inputArguments(0))
    } else {
      throw new GorParsingException(s"Parallel requires a nested query as input. Current input is: ${inputArguments(0)}")
    }

    if (!CommandParseUtilities.hasOption(options, "-parts")) {
      throw new GorParsingException("Parallel requires -parts option")
    }

    val taskExecutionLimit = CommandParseUtilities.intValueOfOptionWithDefault(options, "-limit", 10000 )
    val parallelQuery = CommandParseUtilities.parseNestedCommand(inputArguments.head)
    val inputSource1 = SourceProvider(CommandParseUtilities.stringValueOfOption(options, "-parts"), context, executeNor = true, isNor = false)
    val partsSource = inputSource1.source
    val header = inputSource1.header
    val extraCommands: String = MacroUtilities.getExtraStepsFromQuery(create.query).trim
    val parGorCommands = new util.LinkedHashMap[String, ExecutionBlock]()
    val theKey = createKey.slice(1, createKey.length - 1)
    var theDependencies: List[String] = Nil
    var partitionIndex = 1
    var cachePath: String = null
    val (hasDictFolderWrite, _, hasForkWrite, theCachePath, _) = MacroUtilities.getCachePath(create, context, skipCache)
    val useGordFolders: Boolean = CommandParseUtilities.hasOption(options, "-gordfolder") || hasDictFolderWrite
    if (useGordFolders) {
      cachePath = PathUtils.markAsFolder(theCachePath)
    }


    try {
      val columns = getColumnsFromQuery(parallelQuery, header, forNor = true)

      if (columns.isEmpty) {
        throw new GorParsingException("Input query must contain replacement tags, e.g. #{col:column_name}")
      }

      while (partsSource.hasNext) {
        val row = partsSource.next()
        val parKey = "[" + theKey + "_" + partitionIndex + "]"

        var newCommand = parallelQuery

        columns.foreach{ x =>
          newCommand = newCommand.replace("#{col:" + x._1 + "}", row.colAsString(x._2))
        }

        if (useGordFolders) {
          val i = newCommand.indexOf(' ')+1
          val srcmd = newCommand.substring(0,i)
          if (GorJavaUtilities.isPGorCmd(srcmd)) newCommand = srcmd+"-gordfolder nodict "+newCommand.substring(i)
        }
        if (extraCommands.nonEmpty) newCommand += " " + extraCommands

        parGorCommands.put(parKey, ExecutionBlock(create.groupName, newCommand, create.signature, create.dependencies, create.batchGroupName, cachePath, hasForkWrite = hasForkWrite))
        theDependencies ::= parKey

        partitionIndex += 1

        if (partitionIndex >= taskExecutionLimit) {
          throw new GorParsingException(s"Maximum number of concurrent tasks limit exceeded. Maximum number of tasks are $taskExecutionLimit")
        }
      }
    } finally {
      partsSource.close()
    }

    val theCommand = Range(1,parGorCommands.size+1).foldLeft(getDictionaryType(cmdToModify,useGordFolders)) ((x, y) => x + " [" + theKey + "_" + y + "] " + y)
    parGorCommands.put(createKey, ExecutionBlock(create.groupName, theCommand, create.signature, theDependencies.toArray, create.batchGroupName, cachePath, isDictionary = true, hasForkWrite = hasForkWrite))

    MacroParsingResult(parGorCommands, null)
  }

  override def preProcessCommand(commands: Array[String], context: GorContext): Array[String] = {
    var newCommands: List[String] = Nil
    val createName = "theparallelquery"

    commands.foreach { command =>
      val (a, b) = ScriptParsers.createParser(command)

      if (a.isEmpty) {
        val steps = CommandParseUtilities.quoteSafeSplitAndTrim(command, '|')
        val arguments = CommandParseUtilities.quoteSafeSplitAndTrim(steps.head, ' ')
        val macroInfo = GorPipeMacros.getInfo(this.name)
        val (inputs, options) =  CommandParseUtilities.validateCommandArguments(arguments.slice(1,Int.MaxValue), macroInfo.get.commandArguments)

        if (inputs.isEmpty) {
          throw new GorParsingException(s"Missing nested query as input for ${this.name}")
        }

        var summaryCommand = "GOR"
        val nestedQuery = CommandParseUtilities.parseNestedCommand(inputs.head)
        val firstCommand = CommandParseUtilities.getFirstCommand(nestedQuery)
        if (GorInputSources.isNorCommand(firstCommand)) summaryCommand = "NOR"

        val commandPosition = command.toUpperCase.indexOf(this.name)
        val theCommand = s"$summaryCommand [$createName]"
        val extraCreate = s"create $createName = ${this.name.toLowerCase} ${command.slice(commandPosition + this.name.length + 1, command.length)}"

        newCommands ::= theCommand
        newCommands ::= extraCreate
      } else {
        newCommands ::= command
      }
    }
    newCommands.toArray
  }

  private def getDictionaryType(query: String, useGordFolder: Boolean = false): String = {
    val firstCommand = CommandParseUtilities.getFirstCommand(query)
    if(GorInputSources.isNorCommand(firstCommand)) CommandParseUtilities.NOR_DICTIONARY_PART else if(useGordFolder) CommandParseUtilities.GOR_DICTIONARY_FOLDER_PART else CommandParseUtilities.GOR_DICTIONARY_PART
  }

  private def getColumnsFromQuery(parallelQuery: String, header:String, forNor: Boolean): Map[String, Int] = {
    val matcherIterator = "#?\\{col:(.+?)\\}".r.findAllIn(parallelQuery)
    var columnMap = Map.empty[String, Int]

    matcherIterator.foreach{x =>
      val columnName = x.slice(6, x.length-1)
      try {
        columnMap += (columnName -> CommandParseUtilities.columnFromHeader(columnName, header, forNor = true))
      } catch {
        case ex: GorParsingException => throw new GorParsingException(s"Missing column from replacement tag $x.", ex)
      }
    }

    columnMap
  }
}


