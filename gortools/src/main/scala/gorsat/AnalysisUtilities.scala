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

// gorsatUtilities_new.scala
// (c) deCODE genetics
// 17th May, 2011, Hakon Gudbjartsson

package gorsat

import java.nio.file.{Files, Path, Paths, StandardOpenOption}
import java.util.stream.Collectors

import gorsat.Commands.CommandParseUtilities
import gorsat.DynIterator._
import gorsat.Macros.PartGor
import gorsat.Script.SplitManager
import gorsat.gorsatGorIterator.MapAndListUtilities
import gorsat.gorsatGorIterator.MapAndListUtilities.singleHashMap
import gorsat.process.GorJavaUtilities
import org.gorpipe.exceptions.{GorParsingException, GorResourceException, GorSystemException}
import org.gorpipe.gor.{GorContext, GorSession}
import org.gorpipe.model.genome.files.gor.{FileReader, GorOptions, Row}
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.reflect.io.File

object AnalysisUtilities {

  // Various utility Analysis classes
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val consoleLogger = LoggerFactory.getLogger("console." + this.getClass)

  case class ParameterHolder(varsegleft: Boolean, varsegright: Boolean, lref: Int, rref: Int)

  type slAggregate = List[String] => String
  val slFirst: slAggregate = (x: List[String]) => x.head
  val slToList: slAggregate = (x: List[String]) => x.tail.foldLeft(x.head) (_ + ", " + _)
  val slToSet: slAggregate = (x: List[String]) => {
    val y = x.distinct
    y.tail.foldLeft(y.head) (_ + ", " + _)
  }
  val slCount: slAggregate = (x: List[String]) => x.size.toString
  val slCountDist: slAggregate = (x: List[String]) => (mutable.Set() ++ x).size.toString
  val slMax: slAggregate = (x: List[String]) => x.tail.foldLeft(x.head) ((x, y) => if (x > y) x else y)
  val slMin: slAggregate = (x: List[String]) => x.tail.foldLeft(x.head) ((x, y) => if (x < y) x else y)

  case class SEGinfo(start: Int, stop: Int, r: Row)

  def distSegSeg(seg1: SEGinfo, seg2: SEGinfo): Int = {
    if (seg1.start < seg2.stop && seg1.stop > seg2.start) return 0
    val diststart = distSnpSeg(seg1.start + 1, seg2)
    val diststop = distSnpSeg(seg1.stop, seg2)
    if (Math.abs(diststart) < Math.abs(diststop)) diststart else diststop
  }

  def distSnpSeg(pos: Int, seg: SEGinfo): Int = {
    if (seg.start < pos && pos <= seg.stop) return 0
    if (pos <= seg.start) seg.start + 1 - pos
    else seg.stop - pos // first version defined it as positive only distance, e.g. snp.pos - seg.stop
  }

  def readKeyValuePairs(gortoolFile: String, fileReader: FileReader): singleHashMap = {
    val theMap = new java.util.HashMap[String,String]
    MapAndListUtilities.readArray(gortoolFile, fileReader).filter(x => x.indexOf('\t') > 0).foreach(x => {
      val y = x.split("\t", -1)
      theMap.put(y(0), y(1))
    })
    theMap
  }

  def writeList(fileName: String, m: List[String]) : Unit = {
    writeList(Paths.get(fileName), m)
  }

  def writeList(filePath: java.nio.file.Path, m: List[String]) : Unit = {
    val out = Files.newBufferedWriter(filePath, StandardOpenOption.CREATE)
    m.foreach(t => out.write(t + "\n"))
    out.close
  }

  case class NestedCommandSourceInterface(rootSource: Either[DynamicNorSource, DynamicRowSource],
                                          usedFiles: List[String],
                                          header: String)

  def getSignature(commandToExecute: String): String = {
    var result = ""
    val name = "SIGNATURE"

    if (commandToExecute.toUpperCase.contains(name)) {
      val command = getSignatureCommand(commandToExecute)

      if (command.nonEmpty) {
        val args = CommandParseUtilities.quoteSafeSplit(command, ' ').map(x => x.toLowerCase)

        if (CommandParseUtilities.hasOption(args, "-timeres")) {
          val timeDiff = CommandParseUtilities.intValueOfOption(args, "-timeres")
          val currentTime = System.currentTimeMillis() / 1000L

          if (timeDiff > 0) {
            result = ((currentTime / timeDiff) * timeDiff).toString
          } else {
            result = currentTime.toString
          }

          result = " " + result
        } else {
          val exception = new GorParsingException("Needs to have one of the following options: -timeres")
          exception.setCommandName(name)

          throw exception
        }
      }
    }

    result
  }

  private def getSignatureCommand(commandToExecute:String): String = {
    val name = "SIGNATURE"
    var result = ""
    val commands = CommandParseUtilities.quoteSafeSplit(commandToExecute, '|')

    if (commandToExecute.contains(SplitManager.SPLIT_REPLACEMENT_PATTERN) ||
      commandToExecute.contains(SplitManager.REGULAR_REPLACEMENT_PATTERN)) {
      // Extract the nested query and recursively call back to this method
      val options = CommandParseUtilities.quoteSafeSplit(commandToExecute, ' ')
      val optionsArray = options.map(x => x.trim).filter(x => x.startsWith("<("))

      if (optionsArray.length > 0) {
        result = getSignatureCommand(CommandParseUtilities.parseNestedCommand(optionsArray(0)))
      }
    } else {
      val commandArray = commands.map(x => x.trim).filter(x => x.toUpperCase.startsWith(name))

      if (commandArray.length > 0) {
        result = commandArray(0)
      }
    }

    result
  }

  def checkAliasNameReplacement(gorCommands: Array[String], fileMap: singleHashMap): Unit = {
    gorCommands.filter(x => x.startsWith("def")).foreach(x => fileMap.remove(x.substring(4, x.indexOf('=')).trim))
  }

  def theCacheDirectory(session: GorSession): String = {
    val gorRoot = session.getProjectContext.getRealProjectRootPath
    var cacheDir = Paths.get(if (session.getProjectContext.getCacheDir == null) "gortemp" else session.getProjectContext.getCacheDir)
    cacheDir = if (cacheDir.isAbsolute) cacheDir else gorRoot.resolve(cacheDir)
    if (!Files.exists(cacheDir)) {
      if (session.getProjectContext.getCacheDir == null) {
        cacheDir = Paths.get(System.getProperty("java.io.tmpdir"))
      } else {
        throw new GorSystemException("Cache directory given by -cacheDir ('" + cacheDir + "') does not exist", null)
      }
    }
    cacheDir.toString
  }

  def getTempFileName(outfile: String): String = {
    if (outfile.endsWith( """.norz""")) outfile.replace( """.norz""", """.temptempfile.norz""")
    else if (outfile.endsWith( """.tsv""")) outfile.replace( """.tsv""", """.temptempfile.tsv""")
    else if (outfile.endsWith( """.txt""")) outfile.replace( """.txt""", """.temptempfile.txt""")
    else if (outfile.endsWith( """.nor""")) outfile.replace( """.nor""", """.temptempfile.nor""")
    else outfile.replace( """.gorz""", """.temptempfile.gorz""")

  }

  def validateExternalSource(input:String): Unit = {
    val trimmedInput = input.trim

    if(!trimmedInput.startsWith("{") || !trimmedInput.endsWith("}")) {
      throw new GorParsingException("External commands need to be enclosed within curly brackets, e.g. {...}")
    }
  }

  def extractExternalSource(input:String): String = {
    val trimmedInput = input.trim

    if(input.length > 2 && trimmedInput.startsWith("{") && trimmedInput.endsWith("}")) {
      input.substring(1, input.length -1)
    } else {
      input
    }
  }

  def getFilterTags(largs: Array[String], context: GorContext, doHeader: Boolean): String = {
    var tags = ""
    val hasFileFilter = CommandParseUtilities.hasOption(largs, "-ff")
    val hasFilter = CommandParseUtilities.hasOption(largs, "-f")

    if (hasFileFilter && hasFilter) {
      throw new GorParsingException("Error in options - -ff and -f are mutually exclusive, please select only one option: ")
    }

    if (hasFileFilter) {
      var iteratorCommand = ""
      var dsource: DynamicNorSource = null
      var rightFile = CommandParseUtilities.stringValueOfOption(largs, "-ff")

      if (rightFile.toUpperCase.endsWith(".NORZ") && !(rightFile.slice(0, 2) == "<(")) {
        if (doHeader) rightFile = "<(nor " + rightFile + " | top 0 )"
        else rightFile = "<(nor " + rightFile + " )"
      }

      if (rightFile.slice(0, 2) == "<(") {
        iteratorCommand = CommandParseUtilities.parseNestedCommand(rightFile)
        if (!iteratorCommand.toUpperCase.startsWith("NOR")) {
          throw new GorParsingException("Error in nested query - nested queries in this context must be NOR queries: ")
        }
        val strbuff = new mutable.StringBuilder(1000)
        try {
          dsource = new DynamicNorSource(iteratorCommand, context)
          var line: String = null
          var isFirst = true
          while (dsource.hasNext) {
            line = dsource.nextLine
            val indexOfTab = line.indexOf('\t')
            val aTag = if (indexOfTab < 0) line else line.slice(0, indexOfTab)
            if (!isFirst) {
              strbuff.append(',')
            }
            isFirst = false
            strbuff.append(aTag)
          }
        } finally {
          dsource.close
        }
        tags = strbuff.toString
      } else {
        // Ugly hack in good company. See usage of readTags in GorOptions
        // This fixes an edge case where the file is not found because as we enter this block the root has not been
        // appended to rightFile but the rightFile is root less
        if (context.getSession.getProjectContext.getRoot.nonEmpty && !File(rightFile).exists) {
          rightFile = PartGor.fullFileName(context.getSession, rightFile)
        }
        tags = GorOptions.readTags(rightFile).stream().collect(Collectors.joining(","))
      }
    } else if (hasFilter) {
      tags = CommandParseUtilities.stringValueOfOption(largs, "-f").replace("'", "")
    }

    if (doHeader) {
      tags.split(',').toList.head
    } else {
      tags
    }
  }

  /**
    * Loads aliases into a key value map and caches it in the active session.
    *
    * @param aliasFileName        Alias file name
    * @param session              Session used to read and cache aliases
    * @param defaultAliasFileName Default alias file name, used if aliasFileName is not found
    * @return Key value map of alias to data source
    */
  def loadAliases(aliasFileName: String, session: GorSession, defaultAliasFileName: String): singleHashMap = {
    var gaFile: String = defaultAliasFileName
    var fileMap: singleHashMap = null
    if (aliasFileName != null) {
      gaFile = aliasFileName
      try {
        fileMap = MapAndListUtilities.getSingleHashMap(gaFile, asSet = false, skipEmpty = false, session)
      } catch {
        case e: Exception => throw new GorResourceException("Alias file was not found!", gaFile, e)
      }
    } else {
      try {
        fileMap = MapAndListUtilities.getSingleHashMap(gaFile, asSet = false, skipEmpty = false, session)
      } catch {
        case _: Exception => ( /* Ignore cases when default alias file is not found */ )
      }
    }

    fileMap
  }

  /**
    * Get the path of the white list command file, using the gor.cmd.whitelist.file system property and the project root.
    * The existence of the file is not verified.
    *
    * @return full path to the white list command file if specified, otherwise NULL.
    */
  def getWhiteListCommandFilePath(rootPath: Path): Path = {
    val whitelistFileConfig = System.getProperty("gor.cmd.whitelist.file")
    if (whitelistFileConfig == null || whitelistFileConfig.isEmpty) {
      return null
    }

    GorJavaUtilities.resolveWhiteListFilePath(whitelistFileConfig, rootPath)
  }
}

