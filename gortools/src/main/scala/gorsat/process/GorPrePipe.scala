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

// GorPrePipe.scala
// (c) deCODE genetics
// 19th March, 2012, Hakon Gudbjartsson

package gorsat.process

import gorsat.Commands.{CommandArguments, CommandParseUtilities}
import gorsat.MacroUtilities._
import gorsat.Script.{ScriptEngineFactory, ScriptParsers, SplitManager}
import gorsat.gorsatGorIterator.MapAndListUtilities
import gorsat.gorsatGorIterator.MapAndListUtilities.singleHashMap
import org.gorpipe.gor.GorSession

import scala.collection.JavaConverters._

object GorPrePipe {

  val MAX_NUMBER_OF_STEPS = 10000

  var availCommands: Array[String] = GorPipeCommands.getGorCommands ++ GorInputSources.getInputSources
  val availNorCommands: Array[String] = GorPipeCommands.getNorCommands ++ GorInputSources.getInputSources

  private val commandsContainingInputSources = Array[String]("GOR","NOR","MAP","MULTIMAP","INSET","MERGE","JOIN","LEFTJOIN","VARJOIN"/*,"CSVSEL","CSVCC","GTGEN"*/)

  private def getCommandArgument(command: String) : Option[CommandArguments] = {
    val commandResult = GorPipeCommands.commandMap.get(command)

    if (commandResult.isDefined) {
      Option(commandResult.get.commandArguments)
    } else {
      val inputResult = GorInputSources.commandMap.get(command)

      if (inputResult.isDefined) {
        Option(inputResult.get.commandArguments)
      } else {
        Option.empty[CommandArguments]
      }
    }
  }

  def getUsedFiles(inputCommand: String, session: GorSession): List[String] = {

    val pipeSteps = CommandParseUtilities.quoteSafeSplit(inputCommand, '|')

    var usedFiles: List[String] = Nil

    for (i <- pipeSteps.indices) {
      val fullcommand = if (i == 0 && !(pipeSteps(0).toUpperCase.trim.startsWith("GOR") || pipeSteps(0).toUpperCase.trim.startsWith("NOR"))) "GOR " + pipeSteps(i).trim else pipeSteps(i).trim
      val command = fullcommand.split(' ')(0).toUpperCase
      var loopcommand = command
      val argString = CommandParseUtilities.quoteSafeSplitAndTrim(fullcommand, ' ').mkString(" ")

      if (commandsContainingInputSources.contains(command)) loopcommand = "JOIN"

      loopcommand match {
        case "JOIN" =>
          val cargs = CommandParseUtilities.quoteSafeSplit(argString, ' ')

          val commandArgument = getCommandArgument(command)

          if (commandArgument.isDefined) {
            val (iargs, _) = CommandParseUtilities.validateCommandArguments(cargs.slice(1, MAX_NUMBER_OF_STEPS), commandArgument.get)

            if (iargs.length > 0) {
              val rightFile = iargs(0).trim

              if (CommandParseUtilities.isNestedCommand(rightFile)) {
                // Nested pipe command
                val iteratorCommand = CommandParseUtilities.parseNestedCommand(rightFile)
                val subFiles = getUsedFiles(iteratorCommand, session)
                usedFiles :::= subFiles
              } else {
                val inputArguments = iargs.slice(0, iargs.length).toList
                if (inputArguments.exists(inputArgument => inputArgument.endsWith(".gord") || inputArgument.endsWith(".nord"))) {
                  var tags: List[String] = List[String]()
                  if (CommandParseUtilities.hasOption(cargs, "-f")) {
                    tags = CommandParseUtilities.stringValueOfOption(cargs, "-f")
                      .split("[, ]")
                      .map(tagEntry => CommandParseUtilities.replaceSingleQuotes(tagEntry))
                      .toList
                  } else if (CommandParseUtilities.hasOption(cargs, "-ff")) {
                    val tagFileName = CommandParseUtilities.stringValueOfOption(cargs, "-ff")
                    if (!CommandParseUtilities.isNestedCommand(tagFileName)) {
                      tags = MapAndListUtilities.readArray(tagFileName, session.getProjectContext.getFileReader).toList
                    }
                  }
                  val dictFiles = inputArguments.collect {
                    case s: String if (s.endsWith(".gord") || s.endsWith(".nord")) && !s.startsWith("-") =>
                      if (tags.isEmpty) "#gordict#" + s else "#gordict#" + s + "#gortags#" + tags.mkString(",")
                  }
                  val otherFiles = inputArguments.filter(argumentEntry => !argumentEntry.startsWith("-") && !(argumentEntry.endsWith(".gord") || argumentEntry.endsWith(".nord")))
                  usedFiles :::= otherFiles ::: dictFiles
                } else usedFiles :::= inputArguments.filter(argumentEntry => !argumentEntry.startsWith("-"))
              }
            }
          }
        case _ =>
          /* do nothing */
      }
    }

    usedFiles.distinct
  }


  def pgorReplacer(inputCommand: String): String = {
    var mic = inputCommand
    for (p <- List("p", "P"); g <- List("g", "G"); o <- List("o", "O"); r <- List("r", "R")) {
      val pgor = p + g + o + r + " "
      if (mic.contains(pgor)) {
        mic = CommandParseUtilities.repeatedQuoteSafeReplace(mic, pgor, "gor  ", mic.length + 1)
        val (splitOpt, splitSize, splitOverlap) = ScriptParsers.splitOptionParser(mic)
        if (splitOpt != "") {
          val repstr = if (splitOverlap != "") "-" + splitOpt + splitSize + ":" + splitOverlap + " " else "-" + splitOpt + splitSize + " "
          mic = mic.replace(repstr, "").replace(SplitManager.WHERE_SPLIT_WINDOW, "2=2")
        }
      }
    }

    mic
  }

  // Server side - alias replacement done in client
  def getAliasesAndCreates(inputCommand: String, session: GorSession): Array[String] = {

    var createdFiles = Map.empty[String, String]
    var equiVFlist: List[(String, String)] = Nil
    var aliases:singleHashMap = new java.util.HashMap[String, String]()

    var outArray = Array.empty[String]

    val inputCommands = CommandParseUtilities.quoteSafeSplit(inputCommand, ';')

    val sessionFactory = new GenericSessionFactory()
    val mainAliasMap:singleHashMap = new java.util.HashMap[String, String]()

    val engine = ScriptEngineFactory.create(session.getGorContext, scriptAnalyser = false)

    try {
      val modifiedInputCommands = inputCommands.map(x => pgorReplacer(replaceAllAliases(x, mainAliasMap)))
      engine.execute(modifiedInputCommands)
      createdFiles = engine.getCreatedFiles
      equiVFlist = engine.getVirtualFiles
      aliases = engine.getAliases

      outArray ++= createdFiles.toList.map(x => "createdFiles\t" + x._1 + "\t" + x._2)
      outArray ++= equiVFlist.map(x => "equiVFlist\t" + x._1 + "\t" + x._2)
      outArray ++= aliases.asScala.map(x => "aliases\t" + x._1 + "\t" + x._2)

    } catch {
      case e: Exception => /* nothing */
    }

    outArray
  }
}



