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

import gorsat.Commands.CommandParseUtilities
import gorsat.DynIterator.DynamicRowSource
import gorsat.Script.SplitManager.{CHROM_PATTERN, MAXIMUM_NUMBER_OF_SPLITS, RANGETAG_PATTERN, START_PATTERN, STOP_PATTERN, WHERE_SPLIT_WINDOW}
import gorsat.process.GorPipeCommands
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.session.GorContext

import java.util

/**
  * Manager to perform split replacements on gor macros such as pgor and tablefunction. Call expand command to expand
  * macros based on their replacement value.
  *
  * @param groupName            Batch group name of the command being expanded
  * @param chromosomeSplits     Map of available splits and associated filter is applicable
  * @param replacementPattern   Pattern to be replaced, current patterns are ##REGULAR_CHR_REPLACE## and ##SPLIT_CHR_REPLACE##
  */
case class SplitManager( groupName: String, chromosomeSplits:Map[String,SplitEntry], replacementPattern: String) {

  if (chromosomeSplits != null && chromosomeSplits.size > MAXIMUM_NUMBER_OF_SPLITS) {
    throw new GorParsingException(s"Too many splits for query. The maximum allowed number of splits is $MAXIMUM_NUMBER_OF_SPLITS but the current query splits into ${chromosomeSplits.size} jobs")
  }

  def patternReplace(str: String, rangeInfo: SplitEntry): String = {
    str.replace(replacementPattern, rangeInfo.getRange)
      .replace(WHERE_SPLIT_WINDOW, rangeInfo.getFilter)
      .replace(CHROM_PATTERN, rangeInfo.chrom)
      .replace(START_PATTERN, rangeInfo.start.toString)
      .replace(STOP_PATTERN, (if(rangeInfo.end == -1) SplitManager.MAX_CHROM_SIZE else rangeInfo.end).toString)
      .replace(RANGETAG_PATTERN, rangeInfo.tag)
  }

  def expandCommand(commandToExecute: String, batchGroupName: String, cachePath: String = null): CommandGroup = {
    var expandedCommands: util.List[CommandEntry] = new util.ArrayList[CommandEntry]()
    var removeFromCreates = false

    if (commandToExecute.contains(replacementPattern)) {
      val (splitOpt, splits, splitOverlap, _) = ScriptParsers.splitOptionParser(commandToExecute)

      chromosomeSplits.foreach(k => {
        val (n, c, g) = (groupName.replace(replacementPattern, k._1),
          patternReplace(commandToExecute, k._2), batchGroupName)

        var mc = c

        if (splitOpt != "") {
          val repstr = if (splitOverlap != "") "-" + splitOpt + splits + ":" + splitOverlap + " " else "-" + splitOpt + splits + " "
          mc = mc.replace(repstr, "")
        }
        expandedCommands.add(0, CommandEntry(n, mc, g, cachePath))
      })

      removeFromCreates = true
    } else {
      expandedCommands.add(0, CommandEntry(groupName, commandToExecute, batchGroupName, cachePath))
    }

    CommandGroup(expandedCommands, removeFromCreates)
  }
}


object SplitManager {

  val REGULAR_REPLACEMENT_PATTERN = "##REGULAR_CHR_REPLACE##"
  val SPLIT_REPLACEMENT_PATTERN = "##SPLIT_CHR_REPLACE##"
  val WHERE_SPLIT_WINDOW = "##WHERE_SPLIT_WINDOW##"
  val CHROM_PATTERN = "#{CHROM}"
  val START_PATTERN = "#{BPSTART}"
  val STOP_PATTERN = "#{BPSTOP}"
  val RANGETAG_PATTERN = "#{RANGETAG}"
  val MAXIMUM_NUMBER_OF_SPLITS: Int = System.getProperty("gor.validation.split.maxcount", "5000").toInt
  val MAX_CHROM_SIZE = 1000000000

  def createFromCommand(groupName: String, commandToExecute: String, context: GorContext) : SplitManager = {

    var splitManager: SplitManager = null

    if (commandToExecute.contains(REGULAR_REPLACEMENT_PATTERN)) {
      splitManager = SplitManager(groupName, parseBuildSizeSplit(context.getSession.getProjectContext.getReferenceBuild.getBuildSize), REGULAR_REPLACEMENT_PATTERN)
    } else {
      splitManager = SplitManager(groupName,
        parseSplitSizeSplit(context.getSession.getProjectContext.getReferenceBuild.getBuildSize,
          context.getSession.getProjectContext.getReferenceBuild.getBuildSplit),
        SPLIT_REPLACEMENT_PATTERN)
    }

    if (commandToExecute.contains(REGULAR_REPLACEMENT_PATTERN) || commandToExecute.contains(SPLIT_REPLACEMENT_PATTERN)) {
      val (splitOpt, splits, splitOverlap, splitZero) = ScriptParsers.splitOptionParser(commandToExecute)

      if (CommandParseUtilities.isNestedCommand(splits)) {
        splitManager = SplitManager(groupName, parseNestedSplit(context, splits, if(splitZero) 0 else 1), splitManager.replacementPattern)
      } else if (splitOpt != "") {
        splitManager = SplitManager(groupName, parseArbitrarySplit(context.getSession.getProjectContext.getReferenceBuild.getBuildSize, splits.toInt,
          if (splitOverlap == "") 0 else splitOverlap.toInt), splitManager.replacementPattern)
      }
    }

    splitManager
  }

  // Whole chromesome splits
  def parseBuildSizeSplit(buildSizes: java.util.Map[String, Integer]): Map[String, SplitEntry] = {
    var chromosomeSplits = Map.empty[String, SplitEntry]

    buildSizes.entrySet().forEach(c => {
      chromosomeSplits += (c.getKey -> SplitEntry(c.getKey, 0, getUpperBounds(c.getKey, c.getValue, buildSizes)))
    })

    chromosomeSplits
  }

  // From build split file (max two splits per chromosome)
  def parseSplitSizeSplit(buildSizes: java.util.Map[String, Integer], buildSplits: java.util.Map[String, Integer]): Map[String, SplitEntry] = {
    var chromosomeSplits = Map.empty[String, SplitEntry]

    buildSizes.entrySet().forEach(c => {
      Option(buildSplits.getOrDefault(c.getKey, null)) match {
        case Some(chromosomeSplit) =>
          chromosomeSplits += ((c.getKey + "a") -> SplitEntry(c.getKey, 0, chromosomeSplit - 1))
          chromosomeSplits += ((c.getKey + "b") -> SplitEntry(c.getKey, chromosomeSplit, getUpperBounds(c.getKey, c.getValue, buildSizes)))
        case None =>
          chromosomeSplits += (c.getKey -> SplitEntry(c.getKey, 0, getUpperBounds(c.getKey, c.getValue, buildSizes)))
      }
    })

    chromosomeSplits
  }

  // Split option split.
  def parseArbitrarySplit(buildSizes: java.util.Map[String, Integer], iSplitSize: Int, splitOverlap: Int): Map[String, SplitEntry] = {
    var chromosomeSplits = Map.empty[String, SplitEntry]

    // TODO: This split is not clear, there is a different split method based on the split size???
    var splitSize = if (iSplitSize <= 1000 && splitOverlap == 0) (3000000000L / iSplitSize).toInt else iSplitSize

    buildSizes.entrySet().forEach(c => {
      var beginBp = 0
      var endBp = splitSize
      var no = 1

      if (c.getValue / splitSize > 100) {
        throw new GorParsingException("Error in -split option - PGOR does now allow more than 100 splits per chromosome.\nCurrently using " +
          (c.getValue / splitSize) + " splits. Usage -split size[:overlap] : ")
      }

      if (splitOverlap > splitSize / 2) {
        throw new GorParsingException(s"Error in -split - overlap is too large compared with the split size $splitSize. Usage -split size[:overlap] : ")
      }

      while (beginBp <= c.getValue) {
        chromosomeSplits += ((c.getKey + "_" + no) -> SplitEntry(c.getKey, 0.max(beginBp - splitOverlap),  getUpperBounds(c.getKey, endBp - 1 + splitOverlap, buildSizes)))
        beginBp += splitSize
        endBp += splitSize
        no += 1
      }
    })

    chromosomeSplits
  }

  def parseNestedSplit(context: GorContext, query: String, base: Int = 1): Map[String, SplitEntry] = {
    var chromosomeSplits = Map.empty[String, SplitEntry]

    val iteratorCommand = CommandParseUtilities.parseNestedCommand(query).trim
    var dynamicSource: DynamicRowSource = null

    try {
      dynamicSource = new DynamicRowSource(iteratorCommand, context)
      var i = 0
      var lastChr = ""
      while (dynamicSource.hasNext) {
        val row = dynamicSource.next()
        if (!row.chr.equals(lastChr)) {
          i = 1
          lastChr = row.chr
        }
        val pos = row.pos + base
        val end = row.colAsInt(2)
        val tag = if(row.numCols() > 3) row.colAsString(3).toString else ""
        chromosomeSplits += ((row.chr + "_" + i) -> SplitEntry(row.chr, pos, end, tag))
        i += 1
      }
    } finally {
      if (dynamicSource != null) dynamicSource.close()
    }

    chromosomeSplits
  }

  def useWholeChromosomeSplit(command: String): Boolean = {
    val commandUpperCase = command.toUpperCase
    var commandsOk = true
    val commandsToCheck = GorPipeCommands.getWholeChromosomeSplitCommands

    commandsToCheck.foreach(command => {
      if (commandUpperCase.contains(command)) {
        val y = commandUpperCase.replaceAll(command + " +1[^0-9]", "xxx")
        if (y.contains(command + " ")) commandsOk = false else commandsOk
      }
    })

    !commandsOk
  }

  def getUpperBounds(chrom: String, pos: Int, buildSizes: java.util.Map[String, Integer]): Int = {
    if (buildSizes.containsKey(chrom) && pos >= buildSizes.get(chrom)) -1 else pos
  }
}
