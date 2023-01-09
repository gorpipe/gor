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
import gorsat.Script
import gorsat.Script._
import gorsat.Utilities.MacroUtilities.getCachePath
import org.apache.commons.io.FileUtils
import org.gorpipe.gor.session.GorContext

import java.nio.file.{Files, Path}
import java.util

/***
  * PGOR macro used to preprocess standalone pgor commands into create statement plus gor query. Also performs expansion
  * based on active build and splits. Also if a custom split options are used it performs a split based on input chromosome,
  * start and stop positions. pgor expansions will always result in gor queries in the form of 'gor -p [position and range]
  * <([original query])'.
  */

class PGor extends MacroInfo("PGOR", CommandArguments("-nowithin", "-gordfolder", 1, -1, ignoreIllegalArguments = true)) {

  override protected def processArguments(createKey: String,
                                          create: ExecutionBlock,
                                          context: GorContext,
                                          doHeader: Boolean,
                                          inputArguments: Array[String],
                                          options: Array[String],
                                          skipCache: Boolean): MacroParsingResult = {
    var partitionedGorCommands = new util.LinkedHashMap[String, ExecutionBlock]()
    var theDependencies: util.List[String] = new util.ArrayList[String]()
    val replacePattern = if (SplitManager.useWholeChromosomeSplit(create.query))
      SplitManager.REGULAR_REPLACEMENT_PATTERN else SplitManager.SPLIT_REPLACEMENT_PATTERN
    val createSlice = create.query.trim.slice(5, create.query.length)

    if (!doHeader) {
      val noWithin = CommandParseUtilities.hasOption(options, "-nowithin")
      val (hasDictFolderWrite, cacheFileExists, hasForkWrite, theCachePath, theQueryAppend) = getCachePath(create, context, skipCache)
      val useGordFolders = CommandParseUtilities.hasOption(options, "-gordfolder") || hasDictFolderWrite

      var cachePath: String = null
      val theCommand = if(useGordFolders) {
        cachePath = theCachePath
        if (!cacheFileExists) {
          val noDict = CommandParseUtilities.stringValueOfOptionWithDefault(options, "-gordfolder","dict").equals("nodict")
          val (tcmd, theDeps: util.List[String], partGorCmds: util.LinkedHashMap[String, ExecutionBlock]) = makeGorDict(context, noWithin, createKey, create, replacePattern, theQueryAppend, cachePath, useGordFolders, noDict, hasForkWrite)
          theDependencies = theDeps
          partitionedGorCommands = partGorCmds
          tcmd
        } else {
          "gor " + cachePath
        }
      } else {
        val (tcmd, theDeps: util.List[String], partGorCmds: util.LinkedHashMap[String, ExecutionBlock]) = makeGorDict(context, noWithin, createKey, create, replacePattern, " <("+createSlice+")", cachePath, useGordFolders, false, hasForkWrite = hasForkWrite)
        theDependencies = theDeps
        partitionedGorCommands = partGorCmds
        tcmd
      }
      val theDependenciesArray = theDependencies.toArray(new Array[String](0))
      partitionedGorCommands.put(createKey, Script.ExecutionBlock(create.groupName, theCommand, create.signature,
        theDependenciesArray, create.batchGroupName, cachePath, isDictionary = true))
    } else {
      partitionedGorCommands.put(createKey, ExecutionBlock(create.groupName,
        "xxxxgor " + createSlice, create.signature,
        create.dependencies, create.batchGroupName)) // this should nolonger happen
    }

    MacroParsingResult(partitionedGorCommands, null)
  }

  def makeGorDict(context: GorContext, noWithin: Boolean, createKey: String, create: ExecutionBlock, replacePattern: String, queryAppend: String, cachePath: String, useGordFolder: Boolean, noDict: Boolean, hasForkWrite: Boolean): (String,util.List[String],util.LinkedHashMap[String, ExecutionBlock]) = {
    val partitionedGorCommands = new util.LinkedHashMap[String, ExecutionBlock]()
    val theDependencies: util.List[String] = new util.ArrayList[String]()
    val theKey = createKey.slice(1, createKey.length - 1)
    val gorReplacement = if( noWithin ) "gor -nowithin -p " else "gor -p "
    val partitionKey = "[" + theKey + "_" + replacePattern + "]"
    val newQuery = gorReplacement + replacePattern + queryAppend

    if (useGordFolder && cachePath != null && cachePath.length > 1) {
      val folderPath = Path.of(cachePath)
      if (Files.exists(folderPath) && Files.isDirectory(folderPath)) {
        FileUtils.deleteDirectory(folderPath.toFile)
      }
    }

    partitionedGorCommands.put(partitionKey,Script.ExecutionBlock(partitionKey, newQuery, create.signature,
      create.dependencies, create.batchGroupName, cachePath, hasForkWrite = hasForkWrite))

    val splitManager = SplitManager.createFromCommand(create.groupName, newQuery, context)

    splitManager.chromosomeSplits.keys.foreach(chrKey => {
      val parKey = "[" + theKey + "_" + chrKey + "]"
      theDependencies.add(0, parKey);
    })

    var gordict = if (useGordFolder) CommandParseUtilities.GOR_DICTIONARY_FOLDER else CommandParseUtilities.GOR_DICTIONARY
    if (noDict) {
      gordict += " -nodict"
    }

    val cmd = splitManager.chromosomeSplits.keys.foldLeft(gordict)((x, y) => x + " [" + theKey + "_" + y + "] " +
      splitManager.chromosomeSplits(y).getRange)
    (cmd, theDependencies, partitionedGorCommands)
  }

  override def preProcessCommand(commands: Array[String], context: GorContext): Array[String] = {
    standardGorPreProcessing(commands, context, "thepgorquery")
  }

}
