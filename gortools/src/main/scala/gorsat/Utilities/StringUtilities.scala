/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
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

import java.security.MessageDigest

import scala.math.BigInt

object StringUtilities {

  def addWhile[T](builder: StringBuilder, maxLen: Int, sep: String, toAdd: List[T]): Unit = {
    if (toAdd.nonEmpty) {
      var current = toAdd.head.toString
      var len = builder.length + current.length
      if (len <= maxLen) {
        builder.append(current)
      }
      var idx = 1
      var keepOn = true
      while (idx < toAdd.length && keepOn) {
        current = toAdd(idx).toString
        len += sep.length + current.length
        if (maxLen < len) keepOn = false
        else {
          builder.append(sep)
          builder.append(current)
        }
        idx += 1
      }
    }
  }

  def createMD5(s: String): String = {
    val MD5 = MessageDigest.getInstance("MD5")
    val md5RepresentedAsByteString = BigInt(MD5.digest(s.getBytes)).abs.toString(26)
    md5RepresentedAsByteString
  }
}
