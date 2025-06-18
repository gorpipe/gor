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
import java.nio.file.{Files, Path, Paths}
import gorsat.Utilities.AnalysisUtilities.writeList
import gorsat.Commands.{CommandParseUtilities, Processor}
import gorsat.DynIterator.DynamicRowSource
import gorsat.Outputs.OutFile
import gorsat.QueryHandlers.GeneralQueryHandler.{findCacheFile, findOverheadTime, runCommand}
import gorsat.Utilities.{AnalysisUtilities, MacroUtilities}
import gorsat.process.{GorJavaUtilities, ParallelExecutor}
import org.apache.commons.compress.utils.FileNameUtils
import org.gorpipe.client.FileCache
import org.gorpipe.exceptions.{GorException, GorSystemException, GorUserException}
import org.gorpipe.gor.binsearch.GorIndexType
import org.gorpipe.gor.driver.meta.DataType
import org.gorpipe.gor.model.{DriverBackedFileReader, FileReader, GorMeta, GorOptions, GorParallelQueryHandler}
import org.gorpipe.gor.monitor.GorMonitor
import org.gorpipe.gor.session.GorContext
import org.gorpipe.gor.table.TableHeader
import org.gorpipe.gor.table.dictionary.DictionaryTableMeta
import org.gorpipe.gor.table.util.PathUtils
import org.gorpipe.gor.util.DataUtil
import org.slf4j.LoggerFactory

import java.util.Optional
import scala.jdk.CollectionConverters.IteratorHasAsScala

class GeneralQueryHandler(context: GorContext, header: Boolean) extends GorParallelQueryHandler {

  def getResultsLinkPath(nested: GorContext, writeLocationPath: String, linkCacheFileNameBase: String): (Path, String) = {
    val linkCacheFileNameBaseAdjusted = if (writeLocationPath.endsWith(".gor") && linkCacheFileNameBase.endsWith(".gorz")) {
      linkCacheFileNameBase.substring(0, linkCacheFileNameBase.length - 1)
    } else {
      linkCacheFileNameBase
    }
    val linkCacheFilePath = Path.of(linkCacheFileNameBaseAdjusted + ".link")

    Files.writeString(linkCacheFilePath,
      PathUtils.resolve(nested.getSession.getProjectContext.getProjectRoot, writeLocationPath).toString)
    val extension = FileNameUtils.getExtension(linkCacheFileNameBaseAdjusted) + ".link"

    (linkCacheFilePath, extension)
  }

  def runAndStoreLinkFileInCache(nested: GorContext, writeLocationPath: String, fileCache: FileCache, useMd5: Boolean): String = {
    val startTime = System.currentTimeMillis
    val fileReader = nested.getSession.getProjectContext.getFileReader
    val commandToExecute = nested.getCommand
    val commandSignature = nested.getSignature
    val isGord = commandToExecute.toUpperCase().startsWith(CommandParseUtilities.GOR_DICTIONARY)
    val noDict = commandToExecute.toLowerCase.contains(" -nodict ")
    val writeGord = isGord && !noDict
    var cacheRes = writeLocationPath
    val resultFileName = runCommand(nested, commandToExecute, if (isGord) writeLocationPath else null, useMd5, theTheDict = true)
    val isCacheDir = fileReader.resolveUrl(writeLocationPath,true).isDirectory()

    if(fileCache != null && (!isCacheDir || writeGord)) {
        val candidateCacheFileName = findCacheFile(commandSignature, commandToExecute, header, fileCache, AnalysisUtilities.theCacheDirectory(context.getSession))
        val resultLinkPath = getResultsLinkPath(nested, writeLocationPath, candidateCacheFileName)
        val overheadTime = findOverheadTime(commandToExecute)
        val md5 = if (useMd5) loadMd5(resultLinkPath._1) else ""
        cacheRes = fileCache.store(resultLinkPath._1, commandSignature, resultLinkPath._2, overheadTime + System.currentTimeMillis - startTime, md5)
    }
    cacheRes
  }

  def runAndStoreInCache(nested: GorContext, fileCache: FileCache, useMd5: Boolean): String = {
    val startTime = System.currentTimeMillis
    val commandToExecute = nested.getCommand
    val commandSignature = nested.getSignature
    var cacheFile = findCacheFile(commandSignature, commandToExecute, header, fileCache, AnalysisUtilities.theCacheDirectory(context.getSession))
    val resultFileName = runCommand(nested, commandToExecute, cacheFile, useMd5, theTheDict = false)
    if (fileCache != null) {
      val extension = CommandParseUtilities.getExtensionForQuery(commandToExecute, header)
      val overheadTime = findOverheadTime(commandToExecute)
      val md5 = if (useMd5) loadMd5(Paths.get(resultFileName)) else ""
      cacheFile = fileCache.store(Paths.get(resultFileName), commandSignature, extension, overheadTime + System.currentTimeMillis - startTime, md5)
    }
    cacheFile
  }

  def loadMd5(file : Path): String = {
    val md5Path = Path.of(file + ".md5")
    if (Files.exists(md5Path)) Files.readAllLines(md5Path).get(0)  else ""
  }


  def isDictionaryFolderMacro(cmdUpper : String): Boolean = {
    cmdUpper.startsWith(CommandParseUtilities.GOR_DICTIONARY_FOLDER_PART) || cmdUpper.startsWith(CommandParseUtilities.GOR_DICTIONARY_FOLDER)
  }

  def generateDictionaryFile(commandToExecute: String, fileRoot: String, fileReader: FileReader, useMd5: Boolean, cacheFile: String): Unit = {
    if (isDictionaryFolderMacro(commandToExecute.toUpperCase()) && !fileReader.exists(PathUtils.resolve(fileRoot, GorOptions.DEFAULT_FOLDER_DICTIONARY_NAME))) {
      runCommand(context, commandToExecute, cacheFile, useMd5, theTheDict = true)
    }
  }

  def executeBatch(commandSignatures: Array[String], commandsToExecute: Array[String], batchGroupNames: Array[String], cacheFiles: Array[String], gorMonitor: GorMonitor): Array[String] = {
    val fileNames = new Array[String](commandSignatures.length)
    val fileCache = context.getSession.getProjectContext.getFileCache
    val fileReader = context.getSession.getProjectContext.getFileReader
    val fileRoot = context.getSession.getProjectContext.getProjectRoot
    var commandList: List[() => Unit] = Nil
    val useMd5 = System.getProperty("gor.caching.md5.enabled", "false").toBoolean

    for (i <- commandSignatures.indices) {
      val executeFunction = block2Function {

        val (commandSignature, commandToExecute, batchGroupName) = (commandSignatures(i), commandsToExecute(i), batchGroupNames(i))
        val nested = context.createNestedContext(batchGroupName, commandSignature, commandToExecute)

        try {
          var cacheFile = fileCache.lookupFile(commandSignature)
          cacheFile = GorJavaUtilities.verifyLinkFileLastModified(context.getSession.getProjectContext,cacheFile)
          // Do this if we have result cache active or if we are running locally and the local cacheFile does not exist.
          fileNames(i) = if (cacheFile == null) {
            val writeLocationPath = cacheFiles(i)
            if (writeLocationPath != null) {
              runAndStoreLinkFileInCache(nested, writeLocationPath, fileCache, useMd5)
            } else {
              runAndStoreInCache(nested, fileCache, useMd5)
            }
          } else {
            generateDictionaryFile(commandToExecute, fileRoot, fileReader, useMd5, cacheFile)
            nested.cached(cacheFile)
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
    val fileReader = context.getSession.getProjectContext.getSystemFileReader
    val result = if (commandToExecute.toUpperCase().startsWith(CommandParseUtilities.GOR_DICTIONARY_PART) || commandToExecute.toUpperCase().startsWith(CommandParseUtilities.GOR_DICTIONARY_FOLDER_PART)) {
      writeOutGorDictionaryPart(commandToExecute, fileReader, outfile, theTheDict)
    } else if (commandToExecute.toUpperCase().startsWith(CommandParseUtilities.GOR_DICTIONARY)) {
      writeOutGorDictionary(commandToExecute, fileReader, outfile, theTheDict)
    } else if (commandToExecute.toUpperCase().startsWith(CommandParseUtilities.NOR_DICTIONARY)) {
      writeOutNorDictionaryPart(commandToExecute, fileReader, outfile)
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
    val fileReader = projectContext.getFileReader.asInstanceOf[DriverBackedFileReader]
    val projectRoot = projectContext.getProjectRoot
    val temp_cacheFile = if(outfile!=null) AnalysisUtilities.getTempFileName(outfile) else null
    val oldName = if(temp_cacheFile!=null) {
      var tmpcache = temp_cacheFile
      if(!PathUtils.isAbsolutePath(tmpcache)) {
        tmpcache = PathUtils.resolve(projectRoot,tmpcache)
      }
      tmpcache
    } else null
    try {
      val nor = theSource.isNor
      var newName: String = null
      // TODO: Get a gor config instance somehow into gorpipeSession or gorContext?
      if (useMd5) {
        val runner = context.getSession.getSystemContext.getRunnerFactory.create()
        val ps: Processor = if(outfile!=null) {
          val out = OutFile(temp_cacheFile, fileReader, theHeader, skipHeader = false, columnCompress = false, nor = nor, useMd5, md5File = true, infer = false, GorIndexType.NONE)
          if(nor) out else CheckOrder() | out
        } else null
        runner.run(theSource, ps)
        val md5File = s"$oldName.md5"
        if (fileReader.exists(md5File)) {
          val md5SumLines = fileReader.readAll(md5File)

          if (md5SumLines.nonEmpty && md5SumLines(0).nonEmpty) {
            val extension = outfile.slice(outfile.lastIndexOfSlice("."), outfile.length)
            val md5FileParent = PathUtils.getParent(md5File)
            newName = PathUtils.resolve(md5FileParent,md5SumLines(0) + extension)
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
          newName = outfile
          if(!PathUtils.isAbsolutePath(newName)) {
            newName = PathUtils.resolve(projectRoot,newName)
          }
        }
      } else {
        val runner = context.getSession.getSystemContext.getRunnerFactory.create()
        val ps: Processor = if(outfile!=null) {
          val out = OutFile(temp_cacheFile, fileReader, theHeader, skipHeader = false, nor = nor, md5 = useMd5, command = commandToExecute)
          if (nor) out else CheckOrder() | out
        } else null
        runner.run(theSource, ps)
        if(outfile!=null) {
          newName = outfile
          if(!PathUtils.isAbsolutePath(newName)) {
            newName = PathUtils.resolve(projectRoot,newName)
          }
        }
      }

      if(oldName!=null && fileReader.exists(oldName) && !oldName.equals(newName)) {
        fileReader.move(oldName, newName)
        val oldMetaName = DataUtil.toFile(oldName, DataType.META)
        if (fileReader.exists(oldMetaName)) {
          val parent = PathUtils.getParent(oldMetaName)
          val name = PathUtils.getFileName(newName)

          fileReader.move(oldMetaName, DataUtil.toFile(s"$parent/$name", DataType.META))
        }
        newName
      } else ""
    } catch {
      case e: Exception =>
        try {
          fileReader.delete(oldName)
        } catch {
          case _: Exception => /* do nothing */
        }
        throw e
    } finally {
      theSource.close()
    }
  }

  private def writeOutGorDictionaryFolder(fileReader: FileReader, outfolderpath: String, useTheDict: Boolean): Unit = {
    val outpath = if(useTheDict) {
      if (outfolderpath.endsWith("/")) s"$outfolderpath${GorOptions.DEFAULT_FOLDER_DICTIONARY_NAME}" else s"$outfolderpath/${GorOptions.DEFAULT_FOLDER_DICTIONARY_NAME}";
    } else {
      var idx = outfolderpath.lastIndexOf("/")
      if (idx == -1) {
        s"$outfolderpath/$outfolderpath"
      } else if (idx == outfolderpath.length) {
        idx = outfolderpath.lastIndexOf("/", idx-1)
        s"$outfolderpath/${outfolderpath.substring(idx+1,outfolderpath.length-1)}"
      } else {
        s"$outfolderpath/${outfolderpath.substring(idx+1)}"
      }
    }
    GorJavaUtilities.writeDictionaryFromMeta(fileReader, outfolderpath, outpath)
  }

  def dictRangeFromSeekRange(inp: String, prefix: String): String = {
    val cep = inp.split(':')
    val stasto = if (cep.length > 1) cep(1).split('-') else Array("0", Integer.MAX_VALUE.toString)
    val (c, sp, ep) = (cep(0), stasto(0), if (stasto.length > 1 && stasto(1).nonEmpty) stasto(1) else Integer.MAX_VALUE.toString)
    s"$prefix$c\t$sp\t$c\t$ep"
  }

  private def getDictList(dictFiles: List[String], chromsrange: List[String], fileReader: FileReader): List[String] = {
    var chrI = 0
    val useMetaFile = System.getProperty("gor.use.meta.dictionary","true")
    if(useMetaFile!=null && useMetaFile.toLowerCase.equals("true")) {
      dictFiles.zip(chromsrange).map(x => {
        val f = x._1
        chrI += 1
        val rf = getRelativeFileLocationForDictionaryFileReferences(f)
        val prefix = s"$rf\t$chrI\t"
        val metaPath = DataUtil.toFile(f, DataType.META)
        val opt: Optional[String] = if (fileReader.exists(metaPath)) {
          val meta = GorMeta.createAndLoad(fileReader, metaPath)
          if (meta.getLineCount == -1L) {
            val ret = dictRangeFromSeekRange(x._2, prefix)
            Optional.of[String](ret)
          } else if(meta.getLineCount > 0L) {
            Optional.of[String](prefix + meta.getRange().formatAsTabDelimited())
          } else {
            Optional.empty()
          }
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
        val prefix = s"$rf\t$chrI\t"
        dictRangeFromSeekRange(x._2, prefix)
      })
    }
  }

  private def getPartDictList(dictFiles: List[String], partitions: List[String]): List[String] = {
    dictFiles.zip(partitions).map(x => {
      val f = getRelativeFileLocationForDictionaryFileReferences(x._1)
      val part = x._2
      // file, alias
      s"$f\t$part"
    })
  }

  private def writeOutGorDictionary(commandToExecute: String, fileReader: FileReader, outfile: String, useTheDict: Boolean): String = {
    if(fileReader.isDirectory(outfile)) {
      if (!commandToExecute.toLowerCase.contains("-nodict")) writeOutGorDictionaryFolder(fileReader, outfile, useTheDict)
    } else {
      val w = commandToExecute.split(' ')
      var dictFiles: List[String] = Nil
      var chromsrange: List[String] = Nil
      var i = 1
      while (i < w.length - 1) {
        validateFile(w(i), outfile)
        dictFiles ::= w(i)
        chromsrange ::= w(i + 1)
        i += 2
      }
      val tableHeader = new TableHeader
      if(dictFiles.nonEmpty) {
        val header = fileReader.readHeaderLine(dictFiles.head).split("\t")
        tableHeader.setColumns(header)
      }
      tableHeader.setProperty(DictionaryTableMeta.HEADER_LINE_FILTER_KEY, "false")
      // For skip writing out the header, as tools will not cope if the lines will not match
      //tableHeader.setFileHeader(DictionaryTableMeta.DEFULT_RANGE_TABLE_HEADER)
      val dictList = getDictList(dictFiles, chromsrange, fileReader)
      writeList(fileReader, outfile, tableHeader.formatHeader(), dictList)
    }
    outfile
  }

  private def validateFile(name: String, outfile: String): Unit = {
    if (isVirtualRelation(name)) {
      throw new GorSystemException(String.format("Dictionary command for %s contains virtual relation %s at exec time", outfile, name), null)
    }
  }

  private def isVirtualRelation(name: String): Boolean = {
    name.startsWith("[") && name.endsWith("]")
  }

  def writeOutNorDictionaryPart(commandToExecute: String, fileReader: FileReader, outfile: String): String = {
    val w = commandToExecute.split(' ')
    var dictFiles: List[String] = Nil
    var partitions: List[String] = Nil
    var i = 1
    while (i < w.length - 1) {
      validateFile(w(i), outfile)
      dictFiles ::= w(i)
      partitions ::= w(i + 1)
      i += 2
    }
    val tableHeader = new TableHeader
    tableHeader.setProperty(DictionaryTableMeta.HEADER_LINE_FILTER_KEY, "false")
    val header = fileReader.readHeaderLine(dictFiles.head).split("\t")
    tableHeader.setColumns(header)
    val dictList = getPartDictList(dictFiles, partitions)
    writeList(fileReader, outfile, tableHeader.formatHeader(), dictList)

    outfile
  }

  private def writeOutGorDictionaryPart(commandToExecute: String, fileReader: FileReader, outfile: String, useTheDict: Boolean): String = {
    if(fileReader.isDirectory(outfile)) {
      if (!commandToExecute.toLowerCase.contains("-nodict")) writeOutGorDictionaryFolder(fileReader, outfile, useTheDict)
    } else {
      val w = commandToExecute.split(' ')
      var dictFiles: List[String] = Nil
      var partitions: List[String] = Nil
      var i = 1
      while (i < w.length - 1) {
        validateFile(w(i), outfile)
        dictFiles ::= w(i)
        partitions ::= w(i + 1)
        i += 2
      }
      val tableHeader = new TableHeader
      val header = GorJavaUtilities.parseDictionaryColumn(dictFiles.toArray, fileReader)
      tableHeader.setProperty(DictionaryTableMeta.HEADER_LINE_FILTER_KEY, "false")
      if (header.isPresent) tableHeader.setColumns(header.get())
      val dictList = getPartDictList(dictFiles, partitions)
      writeList(fileReader, outfile, tableHeader.formatHeader(), dictList)
    }
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
