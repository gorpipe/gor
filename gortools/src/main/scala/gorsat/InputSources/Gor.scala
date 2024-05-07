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

package gorsat.InputSources

import gorsat.Commands.CommandParseUtilities._
import gorsat.Commands.GenomicRange.Range
import gorsat.Commands._
import gorsat.DynIterator
import gorsat.DynIterator._
import gorsat.Iterators.ServerGorSource
import gorsat.process.PipeOptions
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.model.GenomicIterator
import org.gorpipe.gor.session.GorContext
import org.gorpipe.gor.util.DataUtil
import org.gorpipe.model.gor.Pipes

import scala.collection.mutable.ListBuffer


object Gor {
  val options: List[String] = List("-nowithin", "-stdin", "-nf", "-fs", "-w", "-Y", "-g", "-q")
  val valueOptions: List[String] = List("-s", "-f", "-ff", "-b", "-Z", "-dict", "-parts", "-p", "-seek", "-idx", "-ref", "-c", "-H", "-X")
}

class Gor() extends InputSourceInfo("GOR", CommandArguments(Gor.options.mkString(" "), Gor.valueOptions.mkString(" "), 1)) {

  override def processArguments(context: GorContext, argString: String, iargs: Array[String],
                                args: Array[String]): InputSourceParsingResult = {

    val usedFiles = ListBuffer.empty[String]
    var inputSource: GenomicIterator = null
    var mergeSteps: Array[String] = null
    val inputFile = iargs(0)
    var range:Range = null

    val noWithin = hasOption(args, "-nowithin")
    val bufferSizeSet = hasOption(args, "-b")
    val bufferSize = intValueOfOptionWithDefaultWithRangeCheck(args, "-b", Pipes.rowsToProcessBuffer, 0)

    if (CommandParseUtilities.isNestedCommand(inputFile)) {
      // Nested source, e.g. used by PGOR
      try {
        var iteratorCommand = CommandParseUtilities.parseNestedCommand(inputFile)

        val partSplit = intValueOfOptionWithDefaultWithRangeCheck(args, "-parts", 1, 1)

        if (partSplit > 1) {
          var pi = 0
          var tags = ""
          if (hasOption(args, "-f")) {
            tags = stringValueOfOption(args, "-f")

            if (!iteratorCommand.contains("#{tags}")) {
              throw new GorParsingException("Using -parts option requires that the nested query contains #{tags} placeholder which is currently missing.")
            }
          }

          val partitions = tags.split(',').toList.groupBy(_ => {
            pi += 1
            pi % partSplit
          })

          val iterCommands = new Array[String](partitions.size)
          pi = 0
          partitions.foreach(x => {
            val ics = quoteSafeSplit(iteratorCommand, '|')
            ics(0) = ics(0).replace("#{tags}", x._2.mkString(","))
            iterCommands(pi) = ics.mkString("|")
            pi += 1
          })

          if (hasOption(args, "-seek")) {
            val seekRange = rangeOfOption(args, "-seek")
            for (i <- iterCommands.indices) iterCommands(i) = addStartSelector(iterCommands(i),
              seekRange.chromosome, seekRange.start, -1, seekOnly = true, if (bufferSizeSet) bufferSize else -1, isHeader = false, context.getSession)
          } else if (hasOption(args, "-p")) {
            val range = rangeOfOption(args, "-p")
            for (i <- iterCommands.indices) iterCommands(i) = addStartSelector(iterCommands(i), range.chromosome,
              range.start, range.stop, seekOnly = false, if (bufferSizeSet) bufferSize else -1, isHeader = false, context.getSession)
          }

          if (partitions.nonEmpty) {
            iteratorCommand = iterCommands(0)
            mergeSteps = (iterCommands(0) :: iterCommands.slice(1, iterCommands.length).map("merge <(" + _ + ")").toList).toArray
          }
        }

        val dynamicSource: DynamicRowSource = if ((iteratorCommand.toUpperCase.startsWith("NOR ") || iteratorCommand.toUpperCase.startsWith("NORIF ")) && !iteratorCommand.replaceAll(" ","").toUpperCase.contains("|TOGOR"))
          new gorsat.DynIterator.DynamicGorNorSource(iteratorCommand, context) else new DynamicRowSource(iteratorCommand, context)
        if (hasOption(args, "-b")) dynamicSource.setBufferSize(bufferSize)
        if (hasOption(args, "-seek")) {
          val seekRange = rangeOfOption(args, "-seek")
          dynamicSource.setPositionWithoutChrLimits(seekRange.chromosome, seekRange.start)
        } else if (hasOption(args, "-p")) {
          range = rangeOfOption(args, "-p")
          dynamicSource.setRange(range.chromosome, range.start, range.stop)
        }

        inputSource = dynamicSource
        usedFiles ++= dynamicSource.usedFiles
      } catch {
        case e: Exception => if (inputSource != null) {
          inputSource.close()
        }
          throw e
      }
    } else {
      if (DataUtil.isYml(inputFile)) {
        val qr = context.getSession.getSystemContext.getReportBuilder.parse(iargs(0))
        val qra = Array(qr)
        val gorpipe = DynIterator.createGorIterator(context)

        val options = new PipeOptions()
        options.parseOptions(qra)
        gorpipe.processArguments(qra, executeNor = false)

        if (gorpipe.getRowSource != null) {
          inputSource = gorpipe.getRowSource
        }
      } else {
        usedFiles ++= iargs.toList.filter(x => !x.startsWith("-"))
        val queryToDriver = args.mkString(" ")

        inputSource = new ServerGorSource(queryToDriver, context, false)
      }
    }

    InputSourceParsingResult(inputSource,
      null,
      isNorContext = false,
      range,
      bufferSize,
      noWithin = noWithin,
      usedFiles.toArray,
      mergeSteps)
  }


}
