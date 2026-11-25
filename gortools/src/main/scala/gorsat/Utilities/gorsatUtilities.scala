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
import java.nio.file.{AtomicMoveNotSupportedException, Files, Paths, StandardCopyOption}
import java.security.MessageDigest
import java.util.HexFormat
import gorsat.Commands.InputSourceParsingResult
import gorsat.Iterators.RowListIterator
import org.gorpipe.exceptions.GorParsingException

import scala.jdk.CollectionConverters.SetHasAsJava
import gorsat.process.NorStreamIterator.HEADER_PREFIX


object Utilities {

  /**
    * Write value to a file named by its SHA-256 digest in cacheDir (or a new temp file if cacheDir is null) and return
    * the file's absolute path. Equal values map to the same file. The file is written to a temp file first and then
    * moved into place, so concurrent writers and readers never see a partly written file.
    */
  def makeTempFile(value: String, cacheDir: String): String = {
    val bytes = value.getBytes
    val hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))
    val perms = Set(PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_READ).asJava
    val cacheFile = if (cacheDir != null) {
      val dir = Paths.get(cacheDir)
      val target = dir.resolve(hash)
      val tmp = Files.createTempFile(dir, hash, ".tmp")
      try {
        Files.write(tmp, bytes)
        Files.setPosixFilePermissions(tmp, perms)
        try {
          Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch {
          case _: AtomicMoveNotSupportedException =>
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING)
        }
      } finally {
        Files.deleteIfExists(tmp)
      }
      target
    } else {
      val tmp = Files.createTempFile(hash, ".sh")
      Files.write(tmp, bytes)
      Files.setPosixFilePermissions(tmp, perms)
      tmp
    }
    cacheFile.toAbsolutePath.toString
  }

  def handleNoValidFilePaths(args: Array[String], isNor:Boolean): InputSourceParsingResult = {
    if (!hasOption(args, "-dh")) {
      throw new GorParsingException("Default header (-dh) is required when none of the provided file paths are valid.")
    } else {
      // return an iterator that only delivers the header defined with -dh.
      val header = stringValueOfOption(args, "-dh")
      val headerCols = header.split(",")
      val inputSource = RowListIterator(List())
      if (isNor) {
        if (headerCols.length < 1 || headerCols(0).isEmpty) {
          throw new GorParsingException("For NOR -dh requires at least 1 non-empty value")
        }
        inputSource.setHeader(HEADER_PREFIX + headerCols.mkString("\t"))
      } else {
        if (headerCols.length < 2 || headerCols(0).isEmpty || headerCols(1).isEmpty) {
          throw new GorParsingException("For GOR -dh requires at least 2 non-empty comma-separated values")
        }
        inputSource.setHeader(headerCols.mkString("\t"))
      }

      InputSourceParsingResult(inputSource, header, isNorContext = isNor)
    }
  }

}