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
import gorsat.Script.{ExecutionBlock, MacroInfo, MacroParsingResult}
import gorsat.Utilities.{AnalysisUtilities, MacroUtilities}
import gorsat.gorsatGorIterator.MapAndListUtilities
import gorsat.process.GorJavaUtilities
import org.gorpipe.exceptions.{GorDataException, GorParsingException}
import org.gorpipe.gor.model.FileReader
import org.gorpipe.gor.session.{GorContext, GorSession}

import scala.collection.mutable

/**
  * The partgor macro expands a partgor query into gor queries with different input filters. Partgor support dictionary inputs
  * and a list of PN's to be used in the query. Partgor will then expand the query into gor commands with -f options which
  * have been pupulated based on input and bucker split options. Partgor macro required the input query to be a nested
  * query with the #{tags} replacement pattern. A partgor query 'partgor -dict [dictionary] -parts 5 <(gor [data] -f #{tags} ...)'
  * will result in 5 or less queries where the input dictionary has been split into 'gor [data] -f [tags per part] ...'.
  * Partgor support multiple split options but the main emphasis is to minimize open file handles and maximize cache hit rates.
  */
class PartGor extends MacroInfo("PARTGOR", CommandArguments("-gordfolder", "-s -p -f -ff -fs -nf -dict -parts -partsize -partscale", 1)) {

  override protected def processArguments(createKey: String,
                                          create: ExecutionBlock,
                                          context: GorContext,
                                          doHeader: Boolean,
                                          inputArguments: Array[String],
                                          options: Array[String],
                                          skipCache: Boolean): MacroParsingResult = {

    val tags = AnalysisUtilities.getFilterTags(options, context, doHeader)
    val dictionary = getDictionary(options, context.getSession)
    val partitionSplitMethod = getSplitMethod(options, context.getSession)

    val bucketMap = PartGor.readDictionaryBucketTagsFromFile(dictionary, tags, context.getSession.getProjectContext.getFileReader)

    validateTags(tags, bucketMap, context.getSession)

    val partitions = PartGor.groupTagsByBuckets(partitionSplitMethod, bucketMap, context.getSession.getSystemContext.getServer)

    val cmdToModify = if (CommandParseUtilities.isNestedCommand(inputArguments(0))) {
      CommandParseUtilities.parseNestedCommand(inputArguments(0))
    } else {
      throw new GorParsingException(s"PartGor requires a nested query as input. Current input is: ${inputArguments(0)}")
    }

    val extraCommands: String = MacroUtilities.getExtraStepsFromQuery(create.query)
    var parGorCommands = Map.empty[String, ExecutionBlock]

    var cachePath: String = null
    val (hasDictFolderWrite, _, _, theCachePath, _) = MacroUtilities.getCachePath(create, context, skipCache)
    val useGordFolders = CommandParseUtilities.hasOption(options, "-gordfolder") || hasDictFolderWrite
    if (useGordFolders) {
      cachePath = theCachePath
    }

    val theKey = createKey.slice(1, createKey.length - 1)
    var theDependencies: List[String] = Nil
    partitions.keys.foreach(partitionKey => {
      val parKey = "[" + theKey + "_" + partitionKey + "]"
      val theCmd =  PartGor.replaceTags(cmdToModify, partitions(partitionKey).toArray)
      var newCmd = if (!GorJavaUtilities.isGorCmd(theCmd)) {
        "gor " + theCmd
      } else if(useGordFolders && GorJavaUtilities.isPGorCmd(theCmd)) {
        val k = theCmd.indexOf(' ')+1
        theCmd.substring(0,k)+"-gordfolder nodict "+theCmd.substring(k)
      } else theCmd

      if (extraCommands.nonEmpty) newCmd += extraCommands

      parGorCommands += (parKey -> ExecutionBlock(create.groupName, newCmd, create.signature, create.dependencies, create.batchGroupName, cachePath))
      theDependencies ::= parKey
    })

    val gordict = if (useGordFolders) "gordictfolderpart" else "gordictpart"
    val theCommand = partitions.keys.foldLeft(gordict) ((x, y) => x + " [" + theKey + "_" + y + "] " + y)
    parGorCommands += (createKey -> ExecutionBlock(create.groupName, theCommand, create.signature, theDependencies.toArray, create.batchGroupName, cachePath, isDictionary = true))

    MacroParsingResult(parGorCommands, null)
  }

  override def preProcessCommand(commands: Array[String], context: GorContext): Array[String] = {
    standardGorPreProcessing(commands, context, "thepartgorquery")
  }

  def validateTags(tags: String, bucketMap: mutable.Map[String, (List[String], Int)], session: GorSession): Unit = {
    if (tags.nonEmpty) {
      val tagSet = tags.split(',').toList
      val tagsFound = bucketMap.keys.map(x => bucketMap(x)._1).fold(Nil)((t, x) => {
        x ::: t
      }).map(x => x -> true).toMap
      val temp = tagSet.filter(x => !tagsFound.contains(x))
      if (temp.nonEmpty) {
        throw new GorParsingException("Error in tag set - tags are not found in the dictionary: " + temp.slice(0, 100))
      }
    }
  }

  def getSplitMethod(largs: Array[String], session: GorSession): (String, Double) = {
    val hasParts = CommandParseUtilities.hasOption(largs, "-parts")
    val hasPartSize = CommandParseUtilities.hasOption(largs, "-partsize")
    val hasPartScale = CommandParseUtilities.hasOption(largs, "-partscale")

    val count = List(hasParts, hasPartScale, hasPartSize).count(_ == true)

    if (count > 1) {
      throw new GorParsingException("Error in split method - only one split method can be defined for partgor. Use -parts, -partsize or -partscale: ")
    }

    if (hasParts) {
      ("parts", CommandParseUtilities.doubleValueOfOption(largs, "-parts"))
    } else if (hasPartSize) {
      ("partsize", CommandParseUtilities.doubleValueOfOption(largs, "-partsize"))
    } else if (hasPartScale) {
      ("partscale", CommandParseUtilities.doubleValueOfOption(largs, "-partscale"))
    } else {
      ("", -1.0)
    }
  }

  def getDictionary(largs: Array[String], session: GorSession): String = {
    if (CommandParseUtilities.hasOption(largs, "-dict")) {
      CommandParseUtilities.stringValueOfOption(largs, "-dict")
    } else {
      throw new GorParsingException("Error in dictionary file - file missing. Use partgor with -dict and reference a dictionary file: ")
    }
  }
}

object PartGor {
  def readDictionaryBucketTagsFromFile(fileName: String, tags: String, fileReader: FileReader): mutable.Map[String, (List[String], Int)] = {
    val dr = MapAndListUtilities.readArray(fileName, fileReader)
    val header = dr.head.split("\t")
    if (header.length == 1) {
      throw new GorDataException("Dictionary does only have one column and no tags. Check " + fileName)
    }
    val dictContents = if (header(0).startsWith("#")) dr.tail else dr
    readDictionaryBucketTags(tags, header.length, dictContents)
  }

  def readDictionaryBucketTags(tags: String, numColumns: Int, dictContents: Array[String]): mutable.Map[String, (List[String], Int)] = {
    if (numColumns == 7) {
      getMultiTagBucketMap(tags, dictContents)
    } else {
      getBucketMap(tags, dictContents)
    }
  }


  private def getBucketMap(tags: String, dictContents: Array[String]) = {
    val bucketMap = scala.collection.mutable.HashMap.empty[String, (List[String], Int)]
    val taglist = tags.split(',').map(x => (x, true)).toMap
    dictContents.map(x => {
      val tempcols = x.split("\t")
      val bucksep = tempcols(0).indexOf('|')
      var bucket: String = null
      var rowitem: String = null
      if (bucksep != -1) {
        bucket = tempcols(0).slice(bucksep + 1, tempcols(0).length)
        rowitem = tempcols(1)
      } else {
        bucket = ""
        rowitem = tempcols(1)
      }
      val allrowItems = if (rowitem.contains(",")) rowitem.split(',').toList else List(rowitem)
      (bucket, allrowItems, allrowItems.size)
    }).collect {
      case s: (String, List[String], Int) if s._1.slice(0, 2) != "D|" =>
        if (tags == "") s else (s._1, s._2.filter(taglist.contains), s._3)
    }.foreach(
      item => {
        bucketMap.get(item._1) match {
          case Some(x) =>
            bucketMap += (item._1 -> (x._1 ::: item._2, x._2 + item._3))
          case None =>
            bucketMap += (item._1 -> (item._2, item._3))
        }
      }
    )
    bucketMap
  }

  private def getMultiTagBucketMap(tags: String, idictContents: Array[String]) = {
    /* Multi-tag buckets, require chrom range as well */
    val bucketMap = scala.collection.mutable.HashMap.empty[String, (List[String], Int)]
    val taglist = tags.split(',').map(x => (x, true)).toMap
    // Handling dict with multiple files having same tag combination, e.g. using additional chromosome partitions
    val dictContents = idictContents.map( x => "\t1\t1\t1\t1\t1\t" + x.split("\t")(6)).distinct.zipWithIndex.map(l => l._2+l._1)

    dictContents.map(x => {
      val tempcols = x.split("\t")
      val bucket = tempcols(0)
      val allrowItems = tempcols(6).split(',').toList
      (bucket, allrowItems, allrowItems.size)
    }).map(x => if (tags == "") x else (x._1, x._2.filter(taglist.contains), x._3)).foreach(
      item => {
        bucketMap.get(item._1) match {
          case Some(x) =>
            bucketMap += (item._1 -> (x._1 ::: item._2, x._2 + item._3))
          case None =>
            bucketMap += (item._1 -> (item._2, item._3))
        }
      }
    )
    bucketMap
  }

  def groupTagsByBuckets(partitionSplitMethod: (String, Double),
                         bucketTagsCount: scala.collection.mutable.Map[String, (List[String], Int)],
                         server: Boolean): scala.collection.immutable.Map[String, List[String]] = {
    var splitGroup = mutable.Map.empty[String, List[String]]


    def takeNfromList(N: Int, theList: List[String]): (List[String], List[String]) = {
      var take: List[String] = Nil
      var remain = theList
      var i = 0
      while (i < N && remain.nonEmpty) {
        take ::= remain.head
        remain = remain.tail
        i += 1
      }
      (take, remain)
    }

    def smallestGroup: (String, Int) = {
      if (splitGroup.keys.isEmpty) return ("", -1)
      var minKey = splitGroup.keys.head
      var minSize = splitGroup(minKey).length
      splitGroup.keys.foreach(x => if (splitGroup(x).length < minSize) {
        minKey = x
        minSize = splitGroup(x).length
        if (minSize == 0) return (minKey, minSize)
      })
      (minKey, minSize)
    }

    def largestBucket(useUnbuckettized: Boolean, targetGroupSize : Int): (String, Int) = {
      val theKeys = bucketTagsCount.keys.filter(x => useUnbuckettized || x != "")
      if (theKeys.isEmpty) return ("", -1)
      var maxKey = theKeys.head
      var maxSize = bucketTagsCount(maxKey)._1.length
      theKeys.foreach(x => if (bucketTagsCount(x)._1.length > maxSize) {
        maxKey = x
        maxSize = bucketTagsCount(x)._1.size
        if (maxSize >= targetGroupSize) return (maxKey, maxSize)
      })
      (maxKey, maxSize)
    }

    def splitToParts(suggestedSplitSize: Int): scala.collection.immutable.Map[String, List[String]] = {
      var targetGroupSize = 1
      /* Create the splits */
      var splitSize = suggestedSplitSize
      val usedParts = bucketTagsCount.map(x => x._2._1.size).sum
      var bucketSize = 1
      bucketTagsCount.filter(x => x._1 != "").foreach(x => if (x._2._1.size > bucketSize) bucketSize = x._2._1.size)
      if (bucketSize == 1) bucketSize = 100
      if (suggestedSplitSize < 1) splitSize = (usedParts.toFloat / bucketSize + 0.9999).toInt.max(1)
      targetGroupSize = 1.max(Math.round((usedParts * 1.0) / splitSize).toInt)


      if (suggestedSplitSize == -1 && bucketSize != 1 && Math.abs((targetGroupSize - bucketSize).toFloat / bucketSize) < 0.25) {
        targetGroupSize = bucketSize
        System.out.println("targetGroupSize2 "+targetGroupSize)
      }

      if (bucketTagsCount.keys.exists(_ == "")) {
        splitSize += 1 + bucketTagsCount("")._1.size / targetGroupSize
        System.out.println("splitSize "+splitSize)
      }

      splitIntoPartitions(splitSize,targetGroupSize)
    }

    def splitToPartSize(partSize: Int): scala.collection.immutable.Map[String, List[String]] = {
      var targetGroupSize = 1
      /* Create the splits */
      val usedParts = bucketTagsCount.map(x => x._2._1.size).sum
      var bucketSize = 1
      bucketTagsCount.filter(x => x._1 != "").foreach(x => if (x._2._2 > bucketSize) bucketSize = x._2._2)
      if (bucketSize == 1) bucketSize = 100
      var splitSize = (usedParts.toFloat / partSize + 0.999999).toInt.max(1)

      /* if (bucketTagsCount.keys.exists(_ == "")) {
        splitSize += 1 + bucketTagsCount("")._1.size / partSize
      } */

      targetGroupSize = partSize.max(1) /* (usedParts.toFloat/splitSize).toInt.max(1) a global variable use in splitIntoPartitions */
      splitIntoPartitions(splitSize,targetGroupSize)
    }

    def splitIntoPartitions(numberOfParts: Int, targetGroupSize: Int): scala.collection.immutable.Map[String, List[String]] = {
      for (i <- 1 to numberOfParts) splitGroup += (i.toString -> Nil)

      // System.out.println("numberOfParts "+numberOfParts)
      // System.out.println("targetGroupSize "+targetGroupSize)

      for (useUnbuckettized <- List(false, true)) {
        var keepOn = true
        while (keepOn) {
          val (minGroup, minSize) = smallestGroup
          val (maxBucket, maxSize) = largestBucket(useUnbuckettized,targetGroupSize)
          if (maxSize > 0) {
            val (a, b) = takeNfromList((targetGroupSize - minSize).max(1), bucketTagsCount(maxBucket)._1)
            if (a != Nil) {
              if (b.nonEmpty) bucketTagsCount += (maxBucket -> (b, 0)) else bucketTagsCount.remove(maxBucket)
              val temp = splitGroup(minGroup)
              splitGroup += (minGroup -> (temp ::: a))
            }
          }
          if (maxSize <= 0 || bucketTagsCount.keys.isEmpty) keepOn = false
        }
      }

      splitGroup.toList.filter(x => x._2.nonEmpty).map(x => (x._1, x._2.sorted)).toMap
    }

    def splitToPartScale(partScale: Double): Predef.Map[String, List[String]] = {
      // Find the maximum bucket size
      var bucketSize = 1
      bucketTagsCount.filter(x => x._1 != "").foreach(x => if (x._2._1.size > bucketSize) bucketSize = x._2._1.size)
      splitToPartSize((partScale * bucketSize).toInt)
    }

    partitionSplitMethod match {
      case ("parts", _) => splitToParts(partitionSplitMethod._2.toInt)
      case ("partsize", _) => splitToPartSize(partitionSplitMethod._2.toInt)
      case ("partscale", _) => splitToPartScale(partitionSplitMethod._2)
      case _ => splitToParts(-1)
    }
  }

  def fullFileName(session: GorSession, fileName: String): String = {
    var gorRoot = session.getProjectContext.getRoot.split(' ')(0)
    if (gorRoot.endsWith("/")) gorRoot = gorRoot.slice(0, gorRoot.length - 1)
    val aFile = fileName.replace( """\""", "/")
    val bFile = if (gorRoot == "") aFile else gorRoot + "/" + aFile
    val resultantFileName = if (bFile.startsWith("/") && bFile(1) != '/') "/" + bFile else bFile
    resultantFileName
  }

  val TAG_PLACEMENT_HOLDER = "#{tags}"
  val TAG_PLACEMENT_HOLDER_SINGLE_QUOTE = "#{tags:q}"
  val TAG_PLACEMENT_HOLDER_DOUBLE_QUOTE = "#{tags:dq}"

  def validateTagsInSubquery(subQuery: String) {
    val subQueryUpper = subQuery.toUpperCase

    if (!(subQueryUpper.contains(TAG_PLACEMENT_HOLDER.toUpperCase)
      || subQueryUpper.contains(TAG_PLACEMENT_HOLDER_SINGLE_QUOTE.toUpperCase)
      || subQueryUpper.contains(TAG_PLACEMENT_HOLDER_DOUBLE_QUOTE.toUpperCase))) {
        throw new GorParsingException(s"Error in $subQuery - sub-queries must include the #{tags}, #{tags:q} or #{tags:dq} option: ")
    }
  }
  
  def replaceTags(cmdToModify: String, tags: Array[String]) : String = {
    validateTagsInSubquery(cmdToModify)
    
    var replacedCmd = cmdToModify
    if (replacedCmd.contains(TAG_PLACEMENT_HOLDER)) {
      replacedCmd = replacedCmd.replace(TAG_PLACEMENT_HOLDER, tags.mkString(","))
    }
    if (replacedCmd.contains(TAG_PLACEMENT_HOLDER_SINGLE_QUOTE)) {
      replacedCmd = replacedCmd.replace(TAG_PLACEMENT_HOLDER_SINGLE_QUOTE, tags.map(x => "'" + x + "'").mkString(","))
    }
    if (replacedCmd.contains(TAG_PLACEMENT_HOLDER_DOUBLE_QUOTE)) {
      replacedCmd = replacedCmd.replace(TAG_PLACEMENT_HOLDER_DOUBLE_QUOTE, tags.map(x => "\"" + x + "\"").mkString(","))
    }

    replacedCmd
  }

}
