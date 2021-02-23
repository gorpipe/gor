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
import org.gorpipe.gor.session.GorContext
import org.gorpipe.gor.table.PathUtils

import java.nio.file.{Files, Paths}

/***
  * PGOR macro used to preprocess standalone pgor commands into create statement plus gor query. Also performs expansion
  * based on active build and splits. Also if a custom split options are used it performs a split based on input chromosome,
  * start and stop positions. pgor expansions will always result in gor queries in the form of 'gor -p [position and range]
  * <([original query])'.
  */

class PGor extends MacroInfo("PGOR", CommandArguments("-nowithin", "", 1, -1, ignoreIllegalArguments = true)) {

  def generateCachepath(context: GorContext, fingerprint: String): String = {
    val fileCache = context.getSession.getProjectContext.getFileCache
    val cachefile = fileCache.tempLocation(fingerprint, ".gord")
    val rootPathStr = context.getSession.getProjectContext.getRoot.split("[ \t]+")(0)
    val rootPath = Paths.get(rootPathStr).normalize()
    val cacheFilePath = Paths.get(cachefile)
    if (cacheFilePath.isAbsolute) {
      PathUtils.relativize(rootPath,cacheFilePath).toString
    } else cacheFilePath.toString
  }

  def fileCacheLookup(context: GorContext, fingerprint: String): (String, Boolean) = {
    if(fingerprint!=null) {
      val fileCache = context.getSession.getProjectContext.getFileCache
      val cachefile = fileCache.lookupFile(fingerprint)
      if (cachefile == null) (generateCachepath(context, fingerprint), false)
      else (cachefile, true)
    } else (null, false)
  }

  def appendQuery(finalQuery: String, lastCmd: String, hasWrite: Boolean): String = {
    " <(" + finalQuery + ")" + (if(hasWrite) {
      " | " + lastCmd
    } else {
      ""
    })
  }

  def getCachePath(create: ExecutionBlock, context: GorContext, skipcache: Boolean): (Boolean, String, String) = {
    val innerQuery = create.query.trim.slice(5, create.query.length)
    val querySplit = CommandParseUtilities.quoteSafeSplit(innerQuery,'|')
    val lastCmd = querySplit.last.trim
    val lastCmdLower = lastCmd.toLowerCase
    val hasWrite = lastCmdLower.startsWith("write ")
    val hasWriteFile = hasWrite & lastCmdLower.endsWith(".gord")
    val finalQuery = if(hasWrite) querySplit.slice(0,querySplit.length-1).mkString("|") else innerQuery
    if(skipcache) {
      val queryAppend = appendQuery(finalQuery, lastCmd, false)
      (false, null, queryAppend)
    } else if(hasWriteFile) {
      val cacheRes = lastCmd.split(" ").last
      val cacheFileExists = Files.exists(Paths.get(cacheRes))
      val queryAppend = " <(" + finalQuery + ")" + " | " + lastCmd
      (cacheFileExists, cacheRes, queryAppend)
    } else {
      val fingerprint = create.signature
      val (cachefile, cacheFileExists) = fileCacheLookup(context, fingerprint)
      val queryAppend = appendQuery(finalQuery, lastCmd, hasWrite)
      (cacheFileExists, cachefile, queryAppend)
    }
  }

  override protected def processArguments(createKey: String,
                                          create: ExecutionBlock,
                                          context: GorContext,
                                          doHeader: Boolean,
                                          inputArguments: Array[String],
                                          options: Array[String],
                                          skipCache: Boolean): MacroParsingResult = {
    var partitionedGorCommands = Map.empty[String, ExecutionBlock]
    var theDependencies: List[String] = Nil
    val replacePattern = if (SplitManager.useWholeChromosomeSplit(create.query))
      SplitManager.REGULAR_REPLACEMENT_PATTERN else SplitManager.SPLIT_REPLACEMENT_PATTERN
    val createSlice = create.query.trim.slice(5, create.query.length)

    if (!doHeader) {
      val noWithin = CommandParseUtilities.hasOption(options, "-nowithin")

      var cachePath: String = null
      val useGordFolders = java.lang.Boolean.parseBoolean(System.getProperty("org.gorpipe.gor.driver.gord.folders"))
      val theCommand = if(useGordFolders) {
        val (cacheFileExists, theCachePath, theQueryAppend) = getCachePath(create, context, skipCache)
        cachePath = theCachePath
        if (!cacheFileExists) {
          val (tcmd, theDeps: List[String], partGorCmds: Map[String, ExecutionBlock]) = makeGorDict(context, noWithin, createKey, create, replacePattern, theQueryAppend, cachePath)
          theDependencies = theDeps
          partitionedGorCommands = partGorCmds
          tcmd
        } else {
          "gor " + cachePath
        }
      } else {
        val (tcmd, theDeps: List[String], partGorCmds: Map[String, ExecutionBlock]) = makeGorDict(context, noWithin, createKey, create, replacePattern, " <("+createSlice+")", cachePath)
        theDependencies = theDeps
        partitionedGorCommands = partGorCmds
        tcmd
      }
      partitionedGorCommands += (createKey -> Script.ExecutionBlock(create.groupName, theCommand, create.signature,
        theDependencies.toArray, create.batchGroupName, cachePath, isDictionary = true))
    } else {
      partitionedGorCommands += (createKey -> ExecutionBlock(create.groupName,
        "xxxxgor " + createSlice, create.signature,
        create.dependencies, create.batchGroupName)) // this should nolonger happen
    }

    MacroParsingResult(partitionedGorCommands, null)
  }

  def makeGorDict(context: GorContext, noWithin: Boolean, createKey: String, create: ExecutionBlock, replacePattern: String, queryAppend: String, cachePath: String): (String,List[String],Map[String, ExecutionBlock]) = {
    var partitionedGorCommands = Map.empty[String, ExecutionBlock]
    var theDependencies: List[String] = Nil
    val theKey = createKey.slice(1, createKey.length - 1)
    val gorReplacement = if( noWithin ) "gor -nowithin -p " else "gor -p "
    val partitionKey = "[" + theKey + "_" + replacePattern + "]"
    val newQuery = gorReplacement + replacePattern + queryAppend
    partitionedGorCommands += (partitionKey -> Script.ExecutionBlock(partitionKey, newQuery, create.signature,
      create.dependencies, create.batchGroupName, cachePath))

    val splitManager = SplitManager.createFromCommand(create.groupName, newQuery, context)

    splitManager.chromosomeSplits.keys.foreach(chrKey => {
      val parKey = "[" + theKey + "_" + chrKey + "]"
      theDependencies ::= parKey
    })

    val cmd = splitManager.chromosomeSplits.keys.foldLeft("gordict")((x, y) => x + " [" + theKey + "_" + y + "] " +
      splitManager.chromosomeSplits(y).range)
    (cmd, theDependencies, partitionedGorCommands)
  }

  override def preProcessCommand(commands: Array[String], context: GorContext): Array[String] = {
    standardGorPreProcessing(commands, context, "thepgorquery")
  }

}
