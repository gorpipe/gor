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

package gorsat.Utilities

import java.nio.file.attribute.PosixFilePermission
import java.nio.file.{Files, Paths}

import gorsat.Commands.{BinaryWrite, CommandInfo, CommandParseUtilities, Write}
import org.gorpipe.exceptions.GorParsingException

import scala.collection.JavaConverters._

object Utilities {

  def makeTempFile(value: String, cacheDir: String): String = {
    val hash = Math.abs(value.hashCode) + ""
    val cacheFile = if (cacheDir != null) {
      Paths.get(cacheDir).resolve(hash)
    } else {
      Files.createTempFile(hash, ".sh")
    }
    Files.write(cacheFile, value.getBytes)
    val perms = Set(PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_READ)
    Files.setPosixFilePermissions(cacheFile, perms.asJava)
    cacheFile.toAbsolutePath.toString
  }

  /**
    * Check if a query contains a certain command
    *
    * @param query
    * @param commandInfo
    * @return
    */
  def queryContainsCommand(query: String, commandInfo: CommandInfo): Boolean = {
    query.toUpperCase.contains(commandInfo.name)
  }

  /**
    * Check if a query contains a WRITE command
    *
    * @param query
    * @return
    */
  def queryContainsWriteCommand(query: String): Boolean = {
    queryContainsCommand(query, new Write) ||
      queryContainsCommand(query, new BinaryWrite)
  }

  /**
    * Get the filename of the output file of the WRITE command, null if none
    *
    * @param query
    * @return
    */
  def getWriteFilename(query: String): String = {
    val command = getWriteCommand(query)
    if (command == null) return null
    val writeIndex = query.trim.toUpperCase.indexOf(command.name + " ")
    if (writeIndex < 0) {
      throw new GorParsingException(s"Unable to get the filename for the write command of ", query, "")
    }
    val queryRemainder = query.substring(writeIndex + command.name.length + 1)
    val commandRemainder = CommandParseUtilities.quoteSafeSplit(queryRemainder, '|')(0)
    val commandArgs = CommandParseUtilities.quoteSafeSplit(commandRemainder, ' ')
    val arguments = command.validateArguments(commandArgs)
    arguments(0)
  }

  /**
    * Get what WRITE command is used in the query, null if none
    *
    * @param query
    * @return
    */
  def getWriteCommand(query: String): CommandInfo = {
    if (Utilities.queryContainsCommand(query, new Write))
      return new Write
    else if (Utilities.queryContainsCommand(query, new BinaryWrite))
      return new BinaryWrite
    null
  }

}