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

import gorsat.Script.MacroInfo

/**
  * Methods to register and access gor script macros. Macros need to be registered before use.
  */
object GorPipeMacros {

  var macrosMap = Map.empty[String, MacroInfo]

  def addInfo(analysisInfo: MacroInfo): Unit = {
    macrosMap += (analysisInfo.name.toUpperCase -> analysisInfo)
  }

  def getInfo(macroName: String) : Option[MacroInfo] = {
    val info = macrosMap.getOrElse(macroName.toUpperCase, null)
    Option(info)
  }

  def getMacroInfoTable: String = {
    val builder: StringBuilder = StringBuilder.newBuilder

    builder.append("Command\tMinNumArgs\tMaxNumArgs\tArguments\tValueArguments\n")
    macrosMap.toSeq.sortWith(_._1 < _._1).foreach{ x => builder.append(x._2.name)
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
    if (macrosMap.isEmpty) {
      addInfo(new gorsat.Macros.PGor)
      addInfo(new gorsat.Macros.PartGor)
      addInfo(new gorsat.Macros.TableFunction)
      addInfo(new gorsat.Macros.Parallel)
    }
  }
}
