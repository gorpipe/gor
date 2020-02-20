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

package gorsat.Analysis

import gorsat.Commands.CommandParseUtilities._
import gorsat.Commands._
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.GorSession
import org.gorpipe.model.genome.files.gor.Row


object AtAnalysis {

  case class GroupStatHolder() {
    var row: Row = _
    var value: Double = Double.NaN
  }

  case class Parameters() {
    var useLast = false
    var useMax = false
  }

  case class AtState(binSize: Int, testColumn: Int, groupColumns: Array[Int], parameters: Parameters) extends BinState {

    val useGroup: Boolean = if (groupColumns.nonEmpty) true else false
    var groupMap = Map.empty[String, GroupStatHolder]
    var comparison: (Double, Double) => Boolean = _
    val groupColumnsArray: Array[Int] = groupColumns

    comparison = (parameters.useMax, parameters.useLast) match {
      case (true, true) => _ >= _
      case (false, true) => _ <= _
      case (false, false) => _ < _
      case _ => _ > _
    }

    override def initialize(binInfo: BinInfo): Unit = {
      // Here we need to select the comparison method used to compare the row
      groupMap = Map.empty[String, GroupStatHolder]
    }

    override def process(r: Row): Unit = {

      // Get all the values being used
      var currentGroup: GroupStatHolder = null
      val currentValue = r.colAsDouble(testColumn)
      val currentGroupID = if (useGroup) r.selectedColumns(groupColumnsArray) else ""

      // Construct the group map, note if no group is defined we still use "" as the default group
      groupMap.get(currentGroupID) match {
        case Some(x) => currentGroup = x
        case None =>
          currentGroup = GroupStatHolder()
          groupMap += (currentGroupID -> currentGroup)
      }

      // Test comparison per group
      if (currentGroup.row == null) {
        currentGroup.row = r
        currentGroup.value = currentValue
      } else {
        if (comparison(currentValue, currentGroup.value)) {
          currentGroup.value = currentValue
          currentGroup.row = r
        }
      }
    }

    override def sendToNextProcessor(binInfo: BinInfo, nextProcessor: Processor): Unit = {

      // make sure output is in order
      val rows = if(useGroup) {
        groupMap.values
          .map(entry => entry.row)
          .filter(row => row != null)
          .toList
          .sortWith((row1, row2) => row1.compareTo(row2) < 0)
      } else {
        groupMap.values
          .map(entry => entry.row)
          .filter(row => row != null)
      }

      rows.foreach(row => nextProcessor.process(row))

      // Cleanup
      for ((_, value) <- groupMap) {
        value.row = null
      }

      groupMap = null
    }
  }

  case class AtFactory(binSize: Int, testColumn: Int, groupColumns: Array[Int], parameters: Parameters) extends BinFactory {
    def create: BinState =
      AtState(binSize, testColumn, groupColumns, parameters)
  }

  case class At(binSize: Int, testColumn: Int, groupColumns: Array[Int], parameters: Parameters) extends
    BinAnalysis(RegularRowHandler(binSize), BinAggregator(
      AtFactory(binSize, testColumn, groupColumns, parameters), 2, 1))
  {
    override def isTypeInformationMaintained: Boolean = true
  }

  case class ChromAt(session: GorSession, testColumn: Int, groupColumns: Array[Int], parameters: Parameters) extends
    BinAnalysis(ChromRowHandler(session), BinAggregator(
      AtFactory(1, testColumn, groupColumns, parameters), 2, 1))
  {
    override def isTypeInformationMaintained: Boolean = true
  }

  case class GenomeAt(testColumn: Int, groupColumns: Array[Int], parameters: Parameters) extends
    BinAnalysis(GenomeRowHandler(), BinAggregator(
      AtFactory(1, testColumn, groupColumns, parameters), 2, 1))
  {
    override def isTypeInformationMaintained: Boolean = true
  }


  def process(useMax: Boolean, session: GorSession, argString: String, iargs:Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {

    if (iargs.length == 2 && executeNor) {
      throw new GorParsingException(s"Cannot have binSize option when running a nor query: ${iargs(0)}. Command has 2 input options but only accepts 1.")
    }

    if ((executeNor && iargs.length < 1) || (!executeNor && iargs.length < 2)) {
      throw new GorParsingException("Too few input arguments supplied for atmin/atmax.")
    }

    val parameters = Parameters()

    parameters.useLast = hasOption(args, "-last")
    parameters.useMax = useMax

    //Tsting for the group option and then extracting the columns to use
    val groupColumns = columnsOfOptionWithNil(args, "-gc", forcedInputHeader, executeNor).distinct

    // Different processing when handling gor vs nor as nor is just gor with -NOR option
    val binSizeDescription = if (executeNor) "1" else iargs(0).toUpperCase
    var binSize = if (executeNor) 1 else 1000000000
    if (!(binSizeDescription.startsWith("CHR") || binSizeDescription.startsWith("GEN") || executeNor))
      binSize = parseIntWithRangeCheck("binSize", iargs(0), 1)

    val testColumn = columnFromHeader(iargs(1 - (if (executeNor) 1 else 0)), forcedInputHeader, executeNor)

    var pipeStep: Analysis = null

    // Test the
    if (binSizeDescription.startsWith("CHR")) {
      pipeStep = ChromAt(session, testColumn, groupColumns.toArray, parameters)
    } else if (binSizeDescription.startsWith("GEN")) {
      pipeStep = GenomeAt(testColumn, groupColumns.toArray, parameters)
    } else {
      pipeStep = At(binSize, testColumn, groupColumns.toArray, parameters)
    }

    CommandParsingResult(pipeStep, forcedInputHeader)
  }
}

