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

package gorsat.QueryHandlers

import java.io.File
import java.lang
import java.nio.file.{Files, Paths}

import org.gorpipe.exceptions.{GorException, GorSystemException}
import org.gorpipe.model.genome.files.gor.GorParallelQueryHandler
import gorsat.AnalysisUtilities
import gorsat.AnalysisUtilities.writeList
import gorsat.Commands.CommandParseUtilities
import gorsat.DynIterator.DynamicRowSource
import gorsat.Outputs.OutFile
import gorsat.QueryHandlers.GeneralQueryHandler.{findCacheFile, findOverheadTime, runCommand}
import gorsat.process.ParallelExecutor
import org.gorpipe.client.FileCache
import org.gorpipe.exceptions.{GorException, GorSystemException, GorUserException}
import org.gorpipe.gor.GorContext
import org.gorpipe.model.genome.files.binsearch.GorIndexType
import org.gorpipe.model.genome.files.gor.{GorMonitor, GorParallelQueryHandler}
import org.slf4j.LoggerFactory


class GeneralQueryHandler(context: GorContext, header: Boolean) extends GorParallelQueryHandler {

  def executeBatch(commandSignatures: Array[String], commandsToExecute: Array[String], batchGroupNames: Array[String], gorMonitor: GorMonitor): Array[String] = {
    val fileNames = new Array[String](commandSignatures.length)
    val fileCache = context.getSession.getProjectContext.getFileCache
    var commandList: List[() => Unit] = Nil
    val useMd5 = System.getProperty("gor.caching.md5.enabled", "false").toBoolean

    for (i <- commandSignatures.indices) {
      val executeFunction = block2Function {

        val (commandSignature, commandToExecute, batchGroupName) = (commandSignatures(i), commandsToExecute(i), batchGroupNames(i))
        val nested = context.createNestedContext(batchGroupName, commandSignature, commandToExecute)

        try {
          var cacheFile: String = fileCache.lookupFile(commandSignature)

          // Do this if we have result cache active or if we are running locally and the local cacheFile does not exist.
          if (cacheFile == null) {
            val startTime = System.currentTimeMillis
            cacheFile = findCacheFile(commandSignature, commandToExecute, header, fileCache, AnalysisUtilities.theCacheDirectory(context.getSession))
            val resultFileName = runCommand(nested, commandToExecute, cacheFile, useMd5)
            if (fileCache != null) {
              val extension = CommandParseUtilities.getExtensionForQuery(commandToExecute, header)
              val overheadTime = findOverheadTime(commandToExecute)
              cacheFile = fileCache.store(Paths.get(resultFileName), commandSignature, extension, overheadTime + System.currentTimeMillis - startTime)
            }
          } else {
            nested.cached(cacheFile)
          }
          fileNames(i) = cacheFile
        } catch {
          case gue: GorUserException =>
            gue.setQuerySource(batchGroupName)
            gue.setContext(nested)
            throw gue
        }
      }
      commandList ::= executeFunction
    }

    if (commandList != Nil) parallelExecution(commandList.reverse.toArray)
    fileNames
  }


  def parallelExecution(commands: Array[() => Unit]): Unit = {
    val pe = new ParallelExecutor(context.getSession.getSystemContext.getWorkers, commands)
    try
      pe.parallelExecute()
    catch {
      case ge: GorException =>
        throw ge
      case e: Throwable =>
        throw new GorSystemException(e)
    }
  }

  def block2Function(block: => Unit): () => Unit = {
    () => block
  }



  override def setForce(force: Boolean): Unit = {

  }

  override def setQueryTime(time: lang.Long): Unit = {

  }

  override def getWaitTime: Long = {
    -1
  }
}

object GeneralQueryHandler {

  private val logger = LoggerFactory.getLogger(this.getClass)

  /**
    * @return full path to the cache file.
    */
  def findCacheFile(commandSignature: String, commandToExecute: String, header: Boolean, fileCache: FileCache, cacheDirectory: String): String = {
    fileCache.tempLocation(commandSignature,
      CommandParseUtilities.getExtensionForQuery(commandToExecute, header))
  }

  def runCommand(context: GorContext, commandToExecute: String, outfile: String, useMd5: Boolean): String = {
    context.start(outfile)
    // We are using absolute paths here
    val result = if (commandToExecute.toUpperCase().startsWith(CommandParseUtilities.GOR_DICTIONARY_PART)) {
      writeOutGorDictionaryPart(commandToExecute, outfile)
    } else if (commandToExecute.toUpperCase().startsWith(CommandParseUtilities.GOR_DICTIONARY)) {
      writeOutGorDictionary(commandToExecute, outfile)
    } else if (commandToExecute.toUpperCase().startsWith(CommandParseUtilities.NOR_DICTIONARY)) {
      writeOutNorDictionaryPart(commandToExecute, outfile)
    } else {
      runCommandInternal(context, commandToExecute, outfile, useMd5)
    }
    context.end()
    result
  }

  private def runCommandInternal(context: GorContext, commandToExecute: String, outfile: String, useMd5: Boolean): String = {
    val theSource = new DynamicRowSource(commandToExecute, context)
    val theHeader = theSource.getHeader
    val temp_cacheFile = AnalysisUtilities.getTempFileName(outfile)

    try {
      val nor = theSource.isNor
      var oldName: File = null
      var newName: File = null
      // TODO: Get a gor config instance somehow into gorpipeSession or gorContext?
      if (useMd5) {
        val runner = context.getSession.getSystemContext.getRunnerFactory.create()
        runner.run(theSource, OutFile(temp_cacheFile, theHeader, skipHeader = false, columnCompress = false, nor = nor, useMd5, GorIndexType.NONE))
        oldName = new java.io.File(temp_cacheFile)
        val md5File = Paths.get(temp_cacheFile + ".md5")
        if (Files.exists(md5File)) {
          val md5SumLines = Files.readAllLines(md5File)

          if (md5SumLines.size() > 0 && md5SumLines.get(0).length > 0) {
            val extension = outfile.slice(outfile.lastIndexOfSlice("."), outfile.length)
            newName = md5File.getParent.resolve(md5SumLines.get(0) + extension).toFile
            try {
              //Files.delete(md5File)
            } catch {
              case _: Exception => /* Do nothing */
            }
          } else {
            logger.warn("MD5 file names are enabled bu the md5 files are not returning any values.")
          }
        } else {
          logger.warn("MD5 files are enabled but no md5 files are found when storing files in filecahce.")
        }

        if (newName == null){
          newName = new java.io.File(outfile)
        }
      } else {
        val runner = context.getSession.getSystemContext.getRunnerFactory.create()
        runner.run(theSource, OutFile(temp_cacheFile, theHeader, skipHeader = false, nor = nor))
        newName = new java.io.File(outfile)
        oldName = new java.io.File(temp_cacheFile)
      }

      oldName.renameTo(newName)
      newName.toString
    } catch {
      case e: Exception =>
        try {
          new java.io.File(temp_cacheFile).delete
        } catch {
          case _: Exception => /* do nothing */
        }
        throw e
    }
  }

  private def writeOutGorDictionary(commandToExecute: String, outfile: String): String = {
    val w = commandToExecute.split(' ')
    var dictFiles: List[String] = Nil
    var chromsrange: List[String] = Nil
    var i = 1
    while (i < w.length - 1) {
      dictFiles ::= getRelativeFileLocationForDictionaryFileReferences(w(i))
      chromsrange ::= w(i + 1)
      i += 2
    }
    var chrI = 0
    val dictList = dictFiles.zip(chromsrange).map(x => {
      val f = x._1
      val cep = x._2.split(':')
      val stasto = cep(1).split('-')
      val (c, sp, ep) = (cep(0), stasto(0), stasto(1))
      chrI += 1
      // file, alias, chrom, startpos, chrom, endpos
      f + "\t" + chrI + "\t" + c + "\t" + sp + "\t" + c + "\t" + ep
    })
    writeList(outfile, dictList)

    outfile
  }

  def writeOutNorDictionaryPart(commandToExecute: String, outfile: String): String = {
    val w = commandToExecute.split(' ')
    var dictFiles: List[String] = Nil
    var partitions: List[String] = Nil
    var i = 1
    while (i < w.length - 1) {
      dictFiles ::= getRelativeFileLocationForDictionaryFileReferences(w(i))
      partitions ::= w(i + 1)
      i += 2
    }
    val dictList = dictFiles.zip(partitions).map(x => {
      val f = x._1
      val part = x._2
      // file, alias
      f + "\t" + part
    })
    writeList(outfile, dictList)

    outfile
  }

  private def writeOutGorDictionaryPart(commandToExecute: String, outfile: String): String = {
    val w = commandToExecute.split(' ')
    var dictFiles: List[String] = Nil
    var partitions: List[String] = Nil
    var i = 1
    while (i < w.length - 1) {
      dictFiles ::= getRelativeFileLocationForDictionaryFileReferences(w(i))
      partitions ::= w(i + 1)
      i += 2
    }
    val dictList = dictFiles.zip(partitions).map(x => {
      val f = x._1
      val part = x._2
      // file, alias
      f + "\t" + part
    })
    writeList(outfile, dictList)

    outfile
  }

  def getRelativeFileLocationForDictionaryFileReferences(fileName: String): String = {
    if(fileName.startsWith("/")) fileName else "../" * fileName.count(x => x == '/') + fileName
  }

  def findOverheadTime(commandToExecute: String): Long = {
    var overheadTime = 0
    if (commandToExecute.startsWith(CommandParseUtilities.GOR_DICTIONARY_PART)) {
      overheadTime = 1000 * 60 * 10 // 10 minutes
    } else if (commandToExecute.startsWith(CommandParseUtilities.GOR_DICTIONARY)) {
      overheadTime = 1000 * 60 * 10 // 10 minutes
    }
    overheadTime
  }

}
