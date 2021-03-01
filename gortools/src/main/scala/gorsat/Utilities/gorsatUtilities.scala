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
}