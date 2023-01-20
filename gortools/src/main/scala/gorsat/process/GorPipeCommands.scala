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

package gorsat.process

import gorsat.Commands
import gorsat.Commands.{PlinkRegression, RelRemoveCommand, Select, _}

/**
  * Methods to register and access gor commands or pipe steps. Commands need to be registered before use.
  */
object GorPipeCommands {

  var commandMap = Map.empty[String, CommandInfo]

  def addInfo(analysisInfo: CommandInfo): Unit = {
    commandMap += (analysisInfo.name.toUpperCase -> analysisInfo)
  }

  def getInfo(commandName: String) : CommandInfo = {
    val info = commandMap.getOrElse(commandName.toUpperCase, null)
    info
  }

  def getMemoryMonitorCommands : Array[String] = {
    commandMap.filter{x => x._2.commandOptions.memoryMonitorCommand}.map{x => x._2.name}.toArray
  }

  def getGorCommands : Array[String] = {
    commandMap.filter{x => x._2.commandOptions.gorCommand}.map{x => x._2.name}.toArray
  }

  def getNorCommands : Array[String] = {
    commandMap.filter{x => x._2.commandOptions.norCommand}.map{x => x._2.name}.toArray
  }

  def getVerifyCommands : Array[String] = {
    commandMap.filter{x => x._2.commandOptions.verifyCommand}.map{x => x._2.name}.toArray
  }

  def getWholeChromosomeSplitCommands : Array[String] = {
    commandMap.filter{x => x._2.commandOptions.ignoreSplitCommand}.map{ x => x._2.name}.toArray
  }

  def getCommandInfoTable: String = {
    val builder: StringBuilder = new StringBuilder()

    builder.append("Command\tGOR\tNOR\tVerify\tMemory\tCancel\tMinNumArgs\tMaxNumArgs\tArguments\tValueArguments\n")
    commandMap.toSeq.sortWith(_._1 < _._1).foreach{x => builder.append(x._2.name)
      builder.append("\t")
      builder.append(x._2.commandOptions.gorCommand)
      builder.append("\t")
      builder.append(x._2.commandOptions.norCommand)
      builder.append("\t")
      builder.append(x._2.commandOptions.verifyCommand)
      builder.append("\t")
      builder.append(x._2.commandOptions.memoryMonitorCommand)
      builder.append("\t")
      builder.append(x._2.commandOptions.cancelCommand)
      builder.append("\t")
      builder.append(x._2.commandArguments.minimumNumberOfArguments)
      builder.append("\t")
      builder.append(x._2.commandArguments.maximumNumberOfArguments)
      builder.append("\t")
      builder.append(x._2.commandArguments.options)
      builder.append("\t")
      builder.append(x._2.commandArguments.valueOptions)
      builder.append("\n")
    }

    builder.toString()
  }

  def register() : Unit = synchronized {
    if (commandMap.isEmpty) {
      addInfo(new gorsat.Commands.CountRows)
      addInfo(new gorsat.Commands.VerifyOrder)
      addInfo(new gorsat.Commands.LogLevel)
      addInfo(new gorsat.Commands.RootLogLevel)
      addInfo(new gorsat.Commands.Log)
      addInfo(new gorsat.Commands.Wait)
      addInfo(new gorsat.Commands.First)
      addInfo(new gorsat.Commands.Top)
      addInfo(new gorsat.Commands.Bug)
      addInfo(new gorsat.Commands.RowNum)
      addInfo(new Span.Span)
      addInfo(new Span.SegSpan)
      addInfo(new gorsat.Commands.SegProj)
      addInfo(new gorsat.Commands.SegHist)
      addInfo(new gorsat.Commands.Skip)
      addInfo(new gorsat.Commands.DistLoc)
      addInfo(new gorsat.Commands.Sort)
      addInfo(new gorsat.Commands.UpTo)
      addInfo(new gorsat.Commands.VarMerge)
      addInfo(new gorsat.Commands.VarNorm)
      addInfo(new gorsat.Commands.Write)
      addInfo(new gorsat.Commands.CigarSegs)
      addInfo(new gorsat.Commands.Variants)
      addInfo(new gorsat.Commands.Bases)
      addInfo(new gorsat.Commands.ColNum)
      addInfo(new gorsat.Commands.Where)
      addInfo(new gorsat.Commands.TryWhere)
      addInfo(new gorsat.Commands.Until)
      addInfo(new gorsat.Commands.ThrowIf)
      addInfo(new gorsat.Commands.Calc)
      addInfo(new gorsat.Commands.CalcIfMissing)
      addInfo(new gorsat.Commands.LeftWhere)
      addInfo(new gorsat.Commands.Replace)
      addInfo(new gorsat.Commands.UnPivot)
      addInfo(new gorsat.Commands.Rename)
      addInfo(new gorsat.Commands.Prefix)
      addInfo(new gorsat.Commands.Distinct)
      addInfo(new gorsat.Commands.AtMax)
      addInfo(new gorsat.Commands.AtMin)
      addInfo(new gorsat.Commands.Group)
      addInfo(new Select.Select)
      addInfo(new gorsat.Commands.TrySelect)
      addInfo(new gorsat.Commands.Hide)
      addInfo(new gorsat.Commands.TryHide)
      addInfo(new gorsat.Commands.ColumnSort)
      addInfo(new gorsat.Commands.Join)
      addInfo(new gorsat.Commands.LeftJoin)
      addInfo(new gorsat.Commands.IOTest)
      addInfo(new gorsat.Commands.VarJoin)
      addInfo(new gorsat.Commands.SelfJoin)
      addInfo(new gorsat.Commands.GtLD)
      addInfo(new gorsat.Commands.CsvSel)
      addInfo(new gorsat.Commands.CsvCC)
      addInfo(new gorsat.Commands.GtGen)
      addInfo(new gorsat.Commands.King)
      addInfo(new gorsat.Commands.King2)
      addInfo(new gorsat.Commands.Queen)
      addInfo(new gorsat.Commands.Regression)
      addInfo(new PlinkRegression)
      addInfo(new PlinkAdjust)
      addInfo(new gorsat.Commands.Granno)
      addInfo(new gorsat.Commands.Rank)
      addInfo(new gorsat.Commands.Pivot)
      addInfo(new gorsat.Commands.Pileup)
      addInfo(new Commands.MapCommand.Map)
      addInfo(new gorsat.Commands.RelRemoveCommand.RelRemove)
      addInfo(new gorsat.Commands.DAGMap)
      addInfo(new gorsat.Commands.MultiMap)
      addInfo(new gorsat.Commands.Inset)
      addInfo(new gorsat.Commands.IHE)
      addInfo(new gorsat.Commands.Split)
      addInfo(new gorsat.Commands.Merge)
      addInfo(new gorsat.Commands.Tee)
      addInfo(new gorsat.Commands.ColSplit)
      addInfo(new gorsat.Commands.SED)
      addInfo(new gorsat.Commands.Gava)
      addInfo(new gorsat.Commands.PedPivot)
      addInfo(new gorsat.Commands.Liftover)
      addInfo(new Cmd.Cmd)
      addInfo(new gorsat.Commands.PipeSteps)
      addInfo(new gorsat.Commands.Seq)
      addInfo(new gorsat.Commands.Range)
      addInfo(new gorsat.Commands.GorrowInfo)
      addInfo(new gorsat.Commands.ValidateColumns)
      addInfo(new gorsat.Commands.ToGor)
      addInfo(new gorsat.Commands.BAMFlag)
      addInfo(new gorsat.Commands.Grep)
      addInfo(new gorsat.Commands.Signature)
      addInfo(new gorsat.Commands.BucketSplit)
      addInfo(new gorsat.Commands.RegSel)
      addInfo(new gorsat.Commands.Cols2List)
      addInfo(new BinaryWrite)
      addInfo(new gorsat.Commands.ColType)
      addInfo(new gorsat.Commands.SetColType)
      addInfo(new gorsat.Commands.Adjust)
      addInfo(new gorsat.Commands.VerifyColType)
      addInfo(new gorsat.Commands.VarGroup)
      addInfo(new gorsat.Commands.GtTranspose)
      addInfo(new gorsat.Commands.King)
      addInfo(new gorsat.Commands.King2)
      addInfo(new gorsat.Commands.GtGen)
      addInfo(new gorsat.Commands.PrGtGen)
      addInfo(new gorsat.Commands.SelWhere)
      addInfo(new gorsat.Commands.VerifyVariant)
      addInfo(new gorsat.Commands.ColumnReorder)
      addInfo(new gorsat.Commands.DeflateColumn)
      addInfo(new gorsat.Commands.InflateColumn)
    }
  }
}
