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

import gorsat.Commands._
import gorsat.InputSources.{Cmd, Gorif, Meta, Nor, Sql}

/**
  * Methods to register and access gor input sources. Input sources need to be registered before use.
  */
object GorInputSources {

  var commandMap = Map.empty[String, InputSourceInfo]

  def addInfo(inputSourceInfo: InputSourceInfo): Unit = {
    commandMap += (inputSourceInfo.name.toUpperCase -> inputSourceInfo)
  }

  def getInfo(commandName: String) : InputSourceInfo = {
    commandMap.getOrElse(commandName.toUpperCase, null)
  }

  def getInputSources : Array[String] = {
    commandMap.map{x => x._2.name}.toArray
  }

  def getInputSourceInfoTable: String = {
    val builder: StringBuilder = new StringBuilder()

    builder.append("Command\tMinNumArgs\tMaxNumArgs\tArguments\tValueArguments\n")
    commandMap.toSeq.sortWith(_._1 < _._1).foreach{x => builder.append(x._2.name)
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
      addInfo(new gorsat.InputSources.Gor)
      addInfo(new Gorif)
      addInfo(new Nor.Nor)
      addInfo(new Nor.Norif)
      addInfo(new Nor.GorNor)
      addInfo(new gorsat.InputSources.Gorrow)
      addInfo(new gorsat.InputSources.Gorrows)
      addInfo(new gorsat.InputSources.Norrows)
      addInfo(new Cmd.Cmd)
      addInfo(new Cmd.NorCmd)
      addInfo(new Cmd.GorCmd)
      addInfo(new Sql.Sql)
      addInfo(new Sql.NorSql)
      addInfo(new Sql.GorSql)
      addInfo(new Meta)
    }
  }

  def isNorCommand(command: String): Boolean = {
    if (command != null) {
      val info = getInfo(command)

      if (info == null) {
        false
      } else {
        info.isNorCommand
      }
    } else {
      false
    }
  }
}
