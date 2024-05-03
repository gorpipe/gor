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

import gorsat.Commands.CommandParseUtilities.{hasOption, stringValueOfOption}

import java.nio.file.attribute.PosixFilePermission
import java.nio.file.{Files, Paths}
import gorsat.Commands.{BinaryWrite, CommandInfo, CommandParseUtilities, InputSourceParsingResult, Write}
import gorsat.Iterators.RowListIterator
import org.gorpipe.exceptions.GorParsingException

import scala.jdk.CollectionConverters.SetHasAsJava

object Utilities {

  def makeTempFile(value: String, cacheDir: String): String = {
    val hash = Math.abs(value.hashCode).toString
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

  def handleNoValidFilePaths(args: Array[String]): InputSourceParsingResult = {
    if (!hasOption(args, "-dh")) {
      throw new GorParsingException("Default header (-dh) is required when none of the provided file paths are valid.")
    } else {
      // return an iterator that only delivers the header defined with -dh.
      val header = stringValueOfOption(args, "-dh")
      val headerCols = header.split(",")
      if (headerCols.length < 2 || headerCols(0).isEmpty || headerCols(1).isEmpty) {
        throw new GorParsingException("-dh requires at least 2 non-empty comma-separated values")
      }
      val inputSource = RowListIterator(List())
      inputSource.setHeader(headerCols.mkString("\t"))
      InputSourceParsingResult(inputSource, header, isNorContext = false)
    }
  }

}