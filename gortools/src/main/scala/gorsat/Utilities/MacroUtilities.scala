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

// gorsatMacros.scala
// (c) deCODE genetics
// 19th March. 2012, Hakon Gudbjartsson

package gorsat.Utilities

import gorsat.Utilities.AnalysisUtilities._
import gorsat.Commands.CommandParseUtilities
import gorsat.Script.{ExecutionBlock, ScriptParsers}
import gorsat.gorsatGorIterator.MapAndListUtilities.singleHashMap
import org.gorpipe.exceptions.{GorParsingException, GorResourceException, GorSystemException}
import org.gorpipe.gor.model.FileReader
import org.gorpipe.gor.session.GorContext
import org.gorpipe.gor.table.PathUtils
import org.slf4j.{Logger, LoggerFactory}

import java.nio.file.{Files, Paths}

object MacroUtilities {
  private val log: Logger = LoggerFactory.getLogger(this.getClass)

  private def zeroParCmdReplacer(input: String, matchPatt: String, replCmd: String): String = {
    if (!input.toUpperCase.contains(matchPatt.toUpperCase)) return input
    val IC = "(?i)"
    if (matchPatt.startsWith("#") && matchPatt.endsWith("#")) return input.replaceAll(IC + matchPatt, replCmd)
    var cmd = " " + input.trim + " "
    val uMatchPatt = matchPatt.toUpperCase.trim
    val N = 100
    var i = 0
    while (cmd.toUpperCase.startsWith(uMatchPatt + ",") && i < N) {
      cmd = cmd.replaceAll(IC + matchPatt.trim + ",", replCmd.trim + ",")
      i += 1
    }
    i = 0
    while (cmd.toUpperCase.endsWith("," + uMatchPatt) && i < N) {
      cmd = cmd.slice(0, cmd.length - ("," + matchPatt.trim).length) + "," + replCmd.trim
      i += 1
    }
    i = 0
    while (cmd.toUpperCase.contains("(" + uMatchPatt + ")") && i < N) {
      cmd = cmd.replaceAll(IC +"""\(""" + matchPatt.trim +"""\)""", "(" + replCmd.trim + ")")
      i += 1
    }
    i = 0
    while (cmd.toUpperCase.contains("(" + uMatchPatt + ",") && i < N) {
      cmd = cmd.replaceAll(IC +"""\(""" + matchPatt.trim + ",", "(" + replCmd.trim + ",")
      i += 1
    }
    i = 0
    while (cmd.toUpperCase.contains("," + uMatchPatt + ")") && i < N) {
      cmd = cmd.replaceAll(IC + "," + matchPatt.trim +"""\)""", "," + replCmd.trim + ")")
      i += 1
    }
    i = 0
    while (cmd.toUpperCase.contains("," + uMatchPatt + ",") && i < N) {
      cmd = cmd.replaceAll(IC + "," + matchPatt.trim + ",", "," + replCmd.trim + ",")
      i += 1
    }

    List((" ", " "), ("""\|""", "|")).foreach(sp => {
      var i = 0
      while (cmd.toUpperCase.startsWith(uMatchPatt + sp._2) && i < N) {
        cmd = cmd.replaceAll(IC + matchPatt.trim + sp._1, replCmd.trim + sp._2)
        i += 1
      }
      i = 0
      while (cmd.toUpperCase.endsWith(sp._2 + uMatchPatt) && i < N) {
        cmd = cmd.slice(0, cmd.length - (sp._2 + matchPatt.trim).length) + sp._2 + replCmd.trim
        i += 1
      }
      i = 0
      while (cmd.toUpperCase.contains(sp._2 + uMatchPatt + sp._2) && i < N) {
        cmd = cmd.replaceAll(IC + sp._1 + matchPatt.trim + sp._1, sp._2 + replCmd.trim + sp._2)
        i += 1
      }
      i = 0
      while (cmd.toUpperCase.contains(sp._2 + uMatchPatt + ",") && i < N) {
        cmd = cmd.replaceAll(IC + sp._1 + matchPatt.trim + ",", sp._2 + replCmd.trim + ",")
        i += 1
      }
      i = 0
      while (cmd.toUpperCase.contains("," + uMatchPatt + sp._2) && i < N) {
        cmd = cmd.replaceAll(IC + "," + matchPatt.trim + sp._1, "," + replCmd.trim + sp._2)
        i += 1
      }
      i = 0
      while (cmd.toUpperCase.contains(sp._2 + uMatchPatt + "|") && i < N) {
        cmd = cmd.replaceAll(IC + sp._1 + matchPatt.trim +"""\|""", sp._2 + replCmd.trim + "|")
        i += 1
      }
      i = 0
      while (cmd.toUpperCase.contains("|" + uMatchPatt + sp._2) && i < N) {
        cmd = cmd.replaceAll(IC +"""\|""" + matchPatt.trim + sp._1, "|" + replCmd.trim + sp._2)
        i += 1
      }
      i = 0
      while (cmd.toUpperCase.contains(sp._2 + uMatchPatt + ")") && i < N) {
        cmd = cmd.replaceAll(IC + sp._1 + matchPatt.trim + ")", sp._2 + replCmd.trim + ")")
        i += 1
      }
      i = 0
      while (cmd.toUpperCase.contains("(" + uMatchPatt + sp._2) && i < N) {
        cmd = cmd.replaceAll(IC +"""\(""" + matchPatt.trim + sp._1, "(" + replCmd.trim + sp._2)
        i += 1
      }
    })
    cmd
  }

  private def prefixParCmdReplacer(prefix: String, n: Int, originalinput: String, matchPatt: String, replCmd: String): String = {
    var input = originalinput
    val IC = "(?i)"
    var shiftPos = 0
    var counter = 0
    val macroReg = ("""(?i)\Q""" + matchPatt +"""\E""").r
    var keepOn = true
    while (keepOn && counter < 100 && shiftPos < input.length) {
      val br = if (prefix == "(") matchPatt.slice(1, matchPatt.length).indexOf('(') + 1 else matchPatt.indexOf('(')
      var matchPos = -1
      if (prefix == "") {
        if (input.toUpperCase.startsWith(matchPatt.slice(0, br + 1).toUpperCase)) matchPos = 0
      }
      else matchPos = CommandParseUtilities.quoteSafeIndexOf(input.slice(shiftPos, input.length).toUpperCase, matchPatt.slice(0, br + 1).toUpperCase) + shiftPos
      if (matchPos < shiftPos) return input
      val rinput = input.slice(matchPos + br + 1, input.length)
      val allarguments = CommandParseUtilities.quoteSafeSplitNoValidation(rinput, ')')
      if (!(rinput.length > allarguments(0).length)) return input // Missing a closing bracket

      val arglist = CommandParseUtilities.quoteSafeSplit(allarguments(0), ',')
      if (arglist.length == n) {
        var partempl = "("
        for (i <- 1 to n) partempl += "$" + i + (if (i < n) "," else ")")
        val newMatchPatt = matchPatt.replace(partempl,"""\(""" + arglist.mkString(",") +"""\)""")
        var replText = replCmd
        for (i <- 1 to arglist.length) replText = replText.replace("$" + i, arglist(i - 1).trim)
        input = input.replaceAll(IC + newMatchPatt, replText)
        shiftPos = 0
        counter += 1
      } else shiftPos += allarguments(0).length + 1
      macroReg.findFirstIn(input) match {
        case Some(_) => keepOn = true;
        case None => keepOn = false
      }
    }
    input
  }

  private def nParCmdReplacer(n: Int, originalinput: String, matchPatt: String, replCmd: String): String = {
    var input = originalinput
    List("", " ", ",","""\(""").foreach(p => input = prefixParCmdReplacer(p, n, input, p + matchPatt, p + replCmd)) // compatible with regex
    input
  }

  private def oneParCmdReplacer(input: String, matchPatt: String, replCmd: String): String = {
    var cmd = input
    val IC = "(?i)"
    val theReg = (IC + ".*" + matchPatt.replace("""($n)""","""\(([^)]+)\)""") + ".*[\r\n]*").r
    try {
      while (true) {
        val theReg(b) = cmd
        val replText = replCmd.replace("$n", b)
        val newMatchPatt = matchPatt.replace("($n)","""\(""" + b +"""\)""")
        cmd = cmd.replaceAll(IC + newMatchPatt, replText)
      }
    } catch {
      case _: Exception => /* do nothing */
    }
    cmd
  }

  private def parCmdReplacer(input: String, matchPatt: String, replCmd: String): String = {
    // If the replCmd has any backslashes (Windows file names, for example) they get
    // lost in the replacements done below with regexes
    if (replCmd.contains(input)) throw new GorParsingException("Replace string contains the definition itself")
    val replCmdFixup = replCmd.replace("\\", "\\\\")

    if (matchPatt.endsWith("($n)")) oneParCmdReplacer(input, matchPatt, replCmdFixup)
    else if (matchPatt.endsWith("($1)")) nParCmdReplacer(1, input, matchPatt, replCmdFixup)
    else if (matchPatt.endsWith("($1,$2)")) nParCmdReplacer(2, input, matchPatt, replCmdFixup)
    else if (matchPatt.endsWith("($1,$2,$3)")) nParCmdReplacer(3, input, matchPatt, replCmdFixup)
    else if (matchPatt.endsWith("($1,$2,$3,$4)")) nParCmdReplacer(4, input, matchPatt, replCmdFixup)
    else if (matchPatt.endsWith("($1,$2,$3,$4,$5)")) nParCmdReplacer(5, input, matchPatt, replCmdFixup)
    else if (matchPatt.endsWith("($1,$2,$3,$4,$5,$6)")) nParCmdReplacer(6, input, matchPatt, replCmdFixup)
    else zeroParCmdReplacer(input, matchPatt, replCmdFixup)
  }

  def extractAliases(commands: Array[String]): singleHashMap = {
    val aliases = new java.util.HashMap[String,String]()

    commands.foreach(command => {
      val (a, b) = ScriptParsers.definitionParser(command)
      if (a != "") aliases.put(a.trim, b)
    })

    aliases
  }

  def applyAliases(commands: Array[String], aliases: singleHashMap) : Array[String] = {
    var gorCommands: List[String] = Nil

    commands.foreach(x => {
      val (a, _) = ScriptParsers.definitionParser(x)
      if (a == "") gorCommands ::= x
    })

    gorCommands.map(x => replaceAllAliases(x, aliases)).reverse.toArray
  }

  def replaceAllAliases(inputArgString: String, fileMap: singleHashMap): String = {
    var keepOn = true
    var argString = inputArgString
    var i = 0
    while (keepOn && i < 100000) {
      i += 1
      val argStringBefore = argString
      fileMap.keySet().toArray().foreach(f => argString = parCmdReplacer(argString, f.toString, fileMap.get(f)))
      if (argString != argStringBefore) keepOn = true else keepOn = false
    }

    argString
  }

  def commandWithMacroReplacements(command: String, gorAliasFile: String, fileReader: FileReader): String = {
    var fileMap: singleHashMap = null
    try {
      fileMap = readKeyValuePairs(gorAliasFile, fileReader)
    } catch {
      case e: Exception => throw new GorResourceException("Error when reading alias macro file.", gorAliasFile, e)
    }
    var argString = ""
    try {
      argString = CommandParseUtilities.quoteSafeSplitAndTrim(command, ' ').mkString(" ")
      return replaceAllAliases(argString, fileMap)
    } catch {
      case e: Exception => throw new GorSystemException("Error when replacing aliases and macros!", e)
    }

    "ERROR in alias replacement"
  }

  def virtualFiles(str: String): List[String] = {
    var tailCmd = str
    var vFiles: List[String] = Nil
    var keepOn = true
    while (keepOn) {
      val sta = CommandParseUtilities.quoteSafeIndexOf(tailCmd, "[")
      if (sta >= 0) {
        val sto = CommandParseUtilities.quoteSafeIndexOf(tailCmd, "]")
        if (sto > sta) {
          val vFileCandidate = tailCmd.slice(sta, sto + 1)
          tailCmd = tailCmd.slice(sto + 1, tailCmd.length)
          if (!vFileCandidate.contains(",")) {
            try {
              vFileCandidate.slice(1, vFileCandidate.length - 1).toInt
            } catch {
              case _: java.lang.NumberFormatException =>
                vFiles ::= vFileCandidate
            }
          }
          keepOn = true
        } else keepOn = false
      } else keepOn = false
    }
    vFiles
  }

  def replaceVirtualFiles(str: String, vf: Map[String, String], equiVFreverse: Map[String, List[String]]): String = {
    var outStr = str
    vf.foreach(y => {
      equiVFreverse.get(y._1) match {
        case Some(aList) => aList.foreach(a => outStr = CommandParseUtilities.quoteSafeReplace(outStr, a, y._2))
        case None =>
          log.warn("There was no reference to create statement '{}' in replaceVirtualFiles", y._1)
      }
    })
    outStr
  }

  def javaReplaceVirtualFiles(str: String, avf: Array[String], aequiVF: Array[String]): String = {
    var outStr = str
    val equiVF = aequiVF.map(x => {
      val y = x.split("\t")
      (y(0), y(1))
    }).toMap
    val vf = avf.map(x => {
      val y = x.split("\t")
      (y(0), y(1))
    }).toMap

    vf.foreach(y => {
      equiVF.foreach(z => {
        val (a, b) = z
        val x = (a, y._2)
        if (b == y._1) outStr = CommandParseUtilities.quoteSafeReplace(outStr, x._1, x._2)
      }) // The equivalence loop
    })
    outStr
  }

  def javaExternalVirtualFiles(str: String): Array[String] = {
    val vf = virtualFiles(str)
    var equiVF = Map.empty[String, String]
    vf.foreach(x => {
      val ef = equivalentVirtualFile(x)
      if (isExternalVirtFile(ef)) equiVF += (x -> CommandParseUtilities.repeatedQuoteSafeReplace(equivalentVirtualFile(x), "  ", " ", str.length))
    })
    equiVF.toArray.map(x => x._1 + "\t" + x._2)
  }

  def isExternalVirtFile(x: String): Boolean = {
    if (List("SDL", "DM", "CLIPBOARD", "FILE", "GRID", "PN", "GORGRID", "SCRIPT").exists(t => equivalentVirtualFile(x).toUpperCase.startsWith("[" + t + ":"))) true else false
  }

  /**
    * Method to remove white spaces from a virtual file placeholder, e.g. [xxx ] - >[xxx}
    *
    * @param virtualFile Virtual file placeholder, e.g. [xxx]
    * @return Virtual file placeholder with whitespaces removed, e.g. [ xxx] -> [xxx]
    */
  def equivalentVirtualFile(virtualFile: String): String = {
    var done = true
    var mx = virtualFile
    var c = 0
    do {
      done = true
      c += 1
      val ix = mx
      if (mx.startsWith("[ ")) mx = "[" + mx.slice(2, mx.length)
      if (mx.endsWith(" ]")) mx = mx.slice(0, mx.length - 2) + "]"
      if (ix != mx) done = false
    } while (!done && c < 100000)
    mx
  }

  /**
    * Returns the base virtual file name, e.g. [xxx] -> xxx and [ yyy ] -> yyy
    *
    * @param virtualFileName  Virtual file placeholder, e.g. [xxx]
    * @return Virtual file name without brackets and extra spaces.
    */
  def getVirtualFileGroupName(virtualFileName: String) : String = {
    virtualFileName.replace("[","").replace("]", "").trim
  }

  def getExtraStepsFromQuery(query: String): String = {
    // Add the final gor step with reference to the create group and append the rest of the command steps
    val querySteps = CommandParseUtilities.quoteSafeSplit(query, '|')

    var extraCommands = ""

    if (querySteps.length > 1) {
      extraCommands = " | " + querySteps.slice(1, 10000).mkString(" | ")
    }
    extraCommands
  }

  def appendQuery(finalQuery: String, lastCmd: String, hasWrite: Boolean, cachefile: String = null, cacheFileExists: Boolean = false): String = {
    " <(" + finalQuery + ")" + (if(cachefile!=null && !cacheFileExists) {
      " | " + (if(hasWrite) lastCmd + (if(cachefile.endsWith(".gord") && !lastCmd.contains(" -d ")) " -d " else " ") + cachefile else "write -d " + cachefile)
    } else if(hasWrite) {
      " | " + lastCmd
    } else {
      ""
    })
  }

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

  def getCachePath(create: ExecutionBlock, context: GorContext, skipcache: Boolean): (Boolean, String, String) = {
    val k = create.query.indexOf(" ")
    val cmdname = create.query.substring(0,k).toLowerCase
    var innerQuery = create.query.trim.slice(k+1, create.query.length)
    if(innerQuery.startsWith("-gordfolder")) {
      var u = innerQuery.indexOf(" ")
      while (innerQuery.charAt(u)==' ') u+=1
      u = innerQuery.indexOf(" ",u)
      innerQuery = innerQuery.substring(u+1)
    }
    var querySplit = CommandParseUtilities.quoteSafeSplit(innerQuery,'|')
    if(querySplit.length==1&&querySplit.head.endsWith(")")&&cmdname.equals("partgor")) {
      val nested = CommandParseUtilities.quoteSafeSplit(querySplit.head,' ').last
      val inested = nested.substring(2,nested.length-1)
      querySplit = CommandParseUtilities.quoteSafeSplit(inested,'|')
    }
    val lastCmd = querySplit.last.trim
    val lastCmdLower = lastCmd.toLowerCase
    val hasWrite = lastCmdLower.startsWith("write ")
    val didx = if(hasWrite) lastCmd.indexOf(" -d ") else 0
    val lidx = if(hasWrite) lastCmdLower.indexOf(".gord/") else 0
    val writeDir = if (didx>0) {
      var k = didx+4
      while (lastCmd.charAt(k)==' ') k += 1
      val e = lastCmd.indexOf(' ',k)
      if (e == -1) lastCmd.substring(k).trim else lastCmd.substring(k,e).trim
    } else if (lidx>0) {
      val k = lastCmd.lastIndexOf(' ',lidx)+1
      lastCmd.substring(k,lidx+5)
    } else null
    val hasWriteFile = hasWrite & lastCmdLower.endsWith(".gord")
    val finalQuery = if(hasWrite) querySplit.slice(0,querySplit.length-1).mkString("|") else innerQuery
    if(skipcache) {
      val queryAppend = appendQuery(finalQuery, lastCmd, false)
      (false, null, queryAppend)
    } else if(writeDir != null || hasWriteFile) {
      val cacheRes = if(writeDir!=null) writeDir else lastCmd.split(" ").last
      val cachepath = Paths.get(cacheRes)
      val cacheFileExists = Files.exists(cachepath) && !Files.isDirectory(cachepath)
      val queryAppend = " <(" + finalQuery + ")" + " | " + lastCmd
      (cacheFileExists, cacheRes, queryAppend)
    } else {
      val fingerprint = create.signature
      val (cachefile, cacheFileExists) = fileCacheLookup(context, fingerprint)
      val queryAppend = appendQuery(finalQuery, lastCmd, hasWrite, cachefile, cacheFileExists)
      (cacheFileExists, cachefile, queryAppend)
    }
  }
}
