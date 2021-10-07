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

import gorsat.Analysis.CheckOrder

import java.lang
import java.nio.file.{Files, Path, Paths, StandardCopyOption}
import gorsat.Utilities.AnalysisUtilities.writeList
import gorsat.Commands.{CommandParseUtilities, Processor}
import gorsat.DynIterator.DynamicRowSource
import gorsat.Outputs.OutFile
import gorsat.QueryHandlers.GeneralQueryHandler.{findCacheFile, findOverheadTime, getRelativeFileLocationForDictionaryFileReferences, runCommand}
import gorsat.Utilities.AnalysisUtilities
import gorsat.process.{GorJavaUtilities, ParallelExecutor}
import org.gorpipe.client.FileCache
import org.gorpipe.exceptions.{GorException, GorSystemException, GorUserException}
import org.gorpipe.gor.binsearch.GorIndexType
import org.gorpipe.gor.model.{DriverBackedFileReader, FileReader, GorMeta, GorParallelQueryHandler, GorServerFileReader}
import org.gorpipe.gor.monitor.GorMonitor
import org.gorpipe.gor.session.GorContext
import org.gorpipe.gor.table.{PathUtils, TableHeader}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import java.util.Optional

class GeneralQueryHandler(context: GorContext, header: Boolean) extends GorParallelQueryHandler {

  def executeBatch(commandSignatures: Array[String], commandsToExecute: Array[String], batchGroupNames: Array[String], cacheFiles: Array[String], gorMonitor: GorMonitor): Array[String] = {
    val fileNames = new Array[String](commandSignatures.length)
    val fileCache = context.getSession.getProjectContext.getFileCache
    var commandList: List[() => Unit] = Nil
    val useMd5 = System.getProperty("gor.caching.md5.enabled", "false").toBoolean

    for (i <- commandSignatures.indices) {
      val executeFunction = block2Function {

        val (commandSignature, commandToExecute, batchGroupName) = (commandSignatures(i), commandsToExecute(i), batchGroupNames(i))
        val nested = context.createNestedContext(batchGroupName, commandSignature, commandToExecute)

        try {
          val newCacheFile = cacheFiles(i)
          fileNames(i) = if(newCacheFile!=null) {
            val cachePath = Paths.get(newCacheFile)
            val isGord = commandToExecute.toUpperCase().startsWith(CommandParseUtilities.GOR_DICTIONARY)
            var cacheRes = newCacheFile
            if(!Files.exists(cachePath) || Files.isDirectory(cachePath)) {
              val startTime = System.currentTimeMillis
              val resultFileName = runCommand(nested, commandToExecute, if(isGord) newCacheFile else null, useMd5, theTheDict = true)
              if (fileCache != null && resultFileName.contains(GorServerFileReader.RESULT_CACHE_DIR)) {
                val extension = CommandParseUtilities.getExtensionForQuery(commandToExecute, header)
                val overheadTime = findOverheadTime(commandToExecute)
                cacheRes = fileCache.store(Paths.get(resultFileName), commandSignature, extension, overheadTime + System.currentTimeMillis - startTime)
              }
            }
            cacheRes
          } else {
            var cacheFile = fileCache.lookupFile(commandSignature)
            // Do this if we have result cache active or if we are running locally and the local cacheFile does not exist.
            if (cacheFile == null) {
              val startTime = System.currentTimeMillis
              cacheFile = findCacheFile(commandSignature, commandToExecute, header, fileCache, AnalysisUtilities.theCacheDirectory(context.getSession))
              val resultFileName = runCommand(nested, commandToExecute, cacheFile, useMd5, theTheDict = false)
              if (fileCache != null) {
                val extension = CommandParseUtilities.getExtensionForQuery(commandToExecute, header)
                val overheadTime = findOverheadTime(commandToExecute)
                cacheFile = fileCache.store(Paths.get(resultFileName), commandSignature, extension, overheadTime + System.currentTimeMillis - startTime)
              }
            } else {
              nested.cached(cacheFile)
            }
            cacheFile
          }
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

  def runCommand(context: GorContext, commandToExecute: String, outfile: String, useMd5: Boolean, theTheDict: Boolean): String = {
    context.start(outfile)
    // We are using absolute paths here
    val result = if (commandToExecute.toUpperCase().startsWith(CommandParseUtilities.GOR_DICTIONARY_PART) || commandToExecute.toUpperCase().startsWith(CommandParseUtilities.GOR_DICTIONARY_FOLDER_PART)) {
      writeOutGorDictionaryPart(commandToExecute, context.getSession.getProjectContext.getFileReader, outfile, theTheDict)
    } else if (commandToExecute.toUpperCase().startsWith(CommandParseUtilities.GOR_DICTIONARY)) {
      writeOutGorDictionary(commandToExecute, context.getSession.getProjectContext.getFileReader, outfile, theTheDict)
    } else if (commandToExecute.toUpperCase().startsWith(CommandParseUtilities.NOR_DICTIONARY)) {
      writeOutNorDictionaryPart(commandToExecute, context.getSession.getProjectContext.getFileReader, outfile)
    } else {
      runCommandInternal(context, commandToExecute, outfile, useMd5)
    }
    context.end()
    result
  }

  private def runCommandInternal(context: GorContext, commandToExecute: String, outfile: String, useMd5: Boolean): String = {
    val theSource = new DynamicRowSource(commandToExecute, context)
    val theHeader = theSource.getHeader

    val projectContext = context.getSession.getProjectContext
    val projectRoot = Paths.get(projectContext.getProjectRoot)
    val temp_cacheFile = if(outfile!=null) AnalysisUtilities.getTempFileName(outfile) else null
    val oldName = if(temp_cacheFile!=null) {
      var tmpcache = Paths.get(temp_cacheFile)
      if(!tmpcache.isAbsolute) {
        tmpcache = projectRoot.resolve(tmpcache)
      }
      tmpcache
    } else null
    try {
      val nor = theSource.isNor
      var newName: Path = null
      // TODO: Get a gor config instance somehow into gorpipeSession or gorContext?
      if (useMd5) {
        val runner = context.getSession.getSystemContext.getRunnerFactory.create()
        val ps: Processor = if(outfile!=null) {
          val out = OutFile(temp_cacheFile, projectContext.getFileReader, theHeader, skipHeader = false, columnCompress = false, nor = nor, useMd5, md5File = true, GorIndexType.NONE)
          if(nor) out else CheckOrder() | out
        } else null
        runner.run(theSource, ps)
        val md5File = Paths.get(oldName + ".md5")
        if (Files.exists(md5File)) {
          val md5SumLines = Files.readAllLines(md5File)

          if (md5SumLines.size() > 0 && md5SumLines.get(0).nonEmpty) {
            val extension = outfile.slice(outfile.lastIndexOfSlice("."), outfile.length)
            newName = md5File.getParent.resolve(md5SumLines.get(0) + extension)
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

        if (newName == null) {
          newName = Paths.get(outfile)
          if(!newName.isAbsolute) {
            newName = projectRoot.resolve(newName)
          }
        }
      } else {
        val runner = context.getSession.getSystemContext.getRunnerFactory.create()
        val ps: Processor = if(outfile!=null) {
          val out = OutFile(temp_cacheFile, projectContext.getFileReader, theHeader, skipHeader = false, nor = nor, md5 = true, command = commandToExecute)
          if (nor) out else CheckOrder() | out
        } else null
        runner.run(theSource, ps)
        if(outfile!=null) {
          newName = Paths.get(outfile)
          if(!newName.isAbsolute) {
            newName = projectRoot.resolve(newName)
          }
        }
      }

      if(oldName!=null && Files.exists(oldName) && !oldName.equals(newName)) {
        Files.move(oldName, newName)
        val oldMetaName = Paths.get(oldName + ".meta")
        if (Files.exists(oldMetaName)) {
          Files.move(oldMetaName, oldMetaName.getParent.resolve(newName.getFileName.toString + ".meta"), StandardCopyOption.REPLACE_EXISTING)
        }
        newName.toString
      } else ""
    } catch {
      case e: Exception =>
        try {
          Files.delete(oldName)
        } catch {
          case _: Exception => /* do nothing */
        }
        throw e
    } finally {
      theSource.close()
    }
  }

  private def writeOutGorDictionaryFolder(outfolderpath: Path, useTheDict: Boolean): Unit = {
    val outpath = if(useTheDict) {
      outfolderpath.resolve("thedict.gord")
    } else {
      outfolderpath.resolve(outfolderpath.getFileName)
    }
    GorJavaUtilities.writeDictionaryFromMeta(outfolderpath, outpath)
  }

  def dictRangeFromSeekRange(inp: String, prefix: String): String = {
    val cep = inp.split(':')
    val stasto = if (cep.length > 1) cep(1).split('-') else Array("0", Integer.MAX_VALUE.toString)
    val (c, sp, ep) = (cep(0), stasto(0), if (stasto.length > 1 && stasto(1).nonEmpty) stasto(1) else Integer.MAX_VALUE.toString)
    prefix + c + "\t" + sp + "\t" + c + "\t" + ep
  }

  private def getDictList(dictFiles: List[String], chromsrange: List[String], root: String): List[String] = {
    var chrI = 0
    val useMetaFile = System.getProperty("gor.use.meta.dictionary","false")
    if(useMetaFile!=null && useMetaFile.toLowerCase.equals("true")) {
      dictFiles.zip(chromsrange).map(x => {
        val f = x._1
        chrI += 1
        val rf = getRelativeFileLocationForDictionaryFileReferences(f)
        val prefix = rf + "\t" + chrI + "\t"
        val metaPath = Paths.get(PathUtils.resolve(root, f+".meta"))
        val opt: Optional[String] = if (Files.exists(metaPath)) {
          GorJavaUtilities.readRangeFromMeta(metaPath, prefix)
        } else {
          val ret = dictRangeFromSeekRange(x._2, prefix)
          Optional.of[String](ret)
        }
        opt
      }).flatMap(o => o.stream().iterator().asScala)
    } else {
      dictFiles.zip(chromsrange).map(x => {
        val f = x._1
        chrI += 1
        val rf = getRelativeFileLocationForDictionaryFileReferences(f)
        val prefix = rf + "\t" + chrI + "\t"
        dictRangeFromSeekRange(x._2, prefix)
      })
    }
  }

  private def getPartDictList(dictFiles: List[String], partitions: List[String]): List[String] = {
    dictFiles.zip(partitions).map(x => {
      val f = getRelativeFileLocationForDictionaryFileReferences(x._1)
      val part = x._2
      // file, alias
      f + "\t" + part
    })
  }

  private def writeOutGorDictionary(commandToExecute: String, fileReader: FileReader, outfile: String, useTheDict: Boolean): String = {
    val (outpath,root) = getOutPath(outfile, fileReader)
    if(Files.isDirectory(outpath)) {
      if (!commandToExecute.toLowerCase.contains("-nodict")) writeOutGorDictionaryFolder(outpath, useTheDict)
    } else {
      val w = commandToExecute.split(' ')
      var dictFiles: List[String] = Nil
      var chromsrange: List[String] = Nil
      var i = 1
      while (i < w.length - 1) {
        dictFiles ::= w(i)
        chromsrange ::= w(i + 1)
        i += 2
      }
      val tableHeader = new TableHeader
      if(dictFiles.nonEmpty) {
        val header = fileReader.readHeaderLine(dictFiles.head).split("\t")
        tableHeader.setColumns(header)
      }
      tableHeader.setTableColumns(TableHeader.DEFULT_RANGE_TABLE_HEADER)
      val dictList = getDictList(dictFiles, chromsrange, root)
      writeList(outpath, tableHeader.formatHeader(), dictList)
    }
    outpath.toString
  }

  def writeOutNorDictionaryPart(commandToExecute: String, fileReader: FileReader, outfile: String): String = {
    val w = commandToExecute.split(' ')
    var dictFiles: List[String] = Nil
    var partitions: List[String] = Nil
    var i = 1
    while (i < w.length - 1) {
      dictFiles ::= w(i)
      partitions ::= w(i + 1)
      i += 2
    }
    val tableHeader = new TableHeader
    val header = fileReader.readHeaderLine(dictFiles.head).split("\t")
    tableHeader.setColumns(header)
    val dictList = getPartDictList(dictFiles, partitions)
    writeList(outfile, tableHeader.formatHeader(), dictList)

    outfile
  }

  private def getOutPath(outfile: String, fileReader: FileReader): (Path,String) = {
    val outpath = Paths.get(outfile)
    fileReader match {
      case driverBackedFileReader: DriverBackedFileReader =>
        val root = driverBackedFileReader.getCommonRoot
        if (!outpath.isAbsolute && root != null) {
          val rootPath = Paths.get(root).normalize()
          (rootPath.resolve(outpath),root)
        } else (outpath,root)
      case _ => (outpath,"")
    }
  }

  private def writeOutGorDictionaryPart(commandToExecute: String, fileReader: FileReader, outfile: String, useTheDict: Boolean): String = {
    val (outpath,_) = getOutPath(outfile, fileReader)
    if(Files.isDirectory(outpath)) {
      if (!commandToExecute.toLowerCase.contains("-nodict")) writeOutGorDictionaryFolder(outpath, useTheDict)
    } else {
      val w = commandToExecute.split(' ')
      var dictFiles: List[String] = Nil
      var partitions: List[String] = Nil
      var i = 1
      while (i < w.length - 1) {
        dictFiles ::= w(i)
        partitions ::= w(i + 1)
        i += 2
      }
      val tableHeader = new TableHeader
      val header = fileReader.readHeaderLine(dictFiles.head).split("\t")
      tableHeader.setColumns(header)
      val dictList = getPartDictList(dictFiles, partitions)
      writeList(outfile, tableHeader.formatHeader(), dictList)
    }
    outpath.toString
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
