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

package org.gorpipe.model.gor

import org.gorpipe.exceptions.GorDataException
import org.gorpipe.model.genome.files.gor.{Row, RowBase}

object RowObj {

  abstract class BinaryHolder

  def splitArray(s: CharSequence): Array[Int] = {
    var i = 0
    var n = 0
    var cols = 1
    while (i < s.length) {
      if (s.charAt(i) == '\t') cols += 1
      i += 1
    }
    i = 0
    n = 0
    val seps = new Array[Int](cols)
    while (i < s.length) {
      if (s.charAt(i) == '\t') {
        seps(n) = i; n += 1
      }
      i += 1
    }
    seps(n) = i
    seps
  }

  def colInt(n: Int, str: CharSequence, sa: Array[Int]): Int = {
    var start = if (n == 0) 0 else sa(n - 1) + 1
    var stop = sa(n)
    while (start < sa(n) && str.charAt(start) == ' ') start += 1
    while (stop > 0 && str.charAt(stop - 1) == ' ') stop -= 1
    if (start == stop) return 0
    var isNegative = false
    var i = start
    var num = 0
    if (str.charAt(i) == '-') {
      isNegative = true; i += 1
    }
    while (i < stop) {
      val si = str.charAt(i)
      if (si < '0' || si > '9') {
        throw new java.lang.NumberFormatException("Error in "+str.subSequence(start,stop)+". "+"Row: "+str+" column: "+n)
      }
      else {
        num = num * 10 + (si - '0'); i += 1
      }
    }
    if (isNegative) -num else num
  }

  def colString(n: Int, str: CharSequence, sa: Array[Int]):CharSequence = {
    val start = if (n == 0) 0 else sa(n - 1) + 1
    val stop = sa(n)
    str.subSequence(start, stop)
  }

  def peekAtColumn(n: Int, str: CharSequence, sa: Array[Int]): Char = {
    val start = if (n == 0) 0 else sa(n - 1) + 1
    return str.charAt(start);
  }

  def colDouble(n: Int, str: CharSequence, sa: Array[Int]): Double = {
    var start = if (n == 0) 0 else sa(n - 1) + 1
    var stop = sa(n)
    while (start < sa(n) && str.charAt(start) == ' ') start += 1
    while (stop > 0 && str.charAt(stop - 1) == ' ') stop -= 1
    val len = stop - start
    if (len == 0) return Double.NaN
    if (len >= 3) {
      if ((str.charAt(start) == 'I' || str.charAt(start) == 'i') && (str.charAt(start + 1) == 'N' ||
        str.charAt(start + 1) == 'n') && (str.charAt(start + 2) == 'F' || str.charAt(start + 2) == 'f')) return Double.PositiveInfinity

      if ((str.charAt(start) == 'N' || str.charAt(start) == 'n') && (str.charAt(start + 1) == 'A' ||
        str.charAt(start + 1) == 'a') && (str.charAt(start + 2) == 'N' || str.charAt(start + 2) == 'n')) return Double.NaN

    }
    if (len >= 4) {
      if (str.charAt(start) == '-' && (str.charAt(start + 1) == 'I' || str.charAt(start + 1) == 'i') && (str.charAt(start + 2) == 'N' ||
        str.charAt(start + 2) == 'n') && (str.charAt(start + 3) == 'F' || str.charAt(start + 4) == 'f')) return Double.NegativeInfinity
    }
    str.subSequence(start, stop).toString.toDouble
  }

  def colLong(n: Int, str: CharSequence, sa: Array[Int]): Long = {
    var start = if (n == 0) 0 else sa(n - 1) + 1
    var stop = sa(n)
    while (start < sa(n) && str.charAt(start) == ' ') start += 1
    while (stop > 0 && str.charAt(stop - 1) == ' ') stop -= 1
    val len = stop - start
    if (len == 0) return Double.NaN.toLong
    if (len >= 3) {
      if ((str.charAt(start) == 'I' || str.charAt(start) == 'i') && (str.charAt(start + 1) == 'N' ||
        str.charAt(start + 1) == 'n') && (str.charAt(start + 2) == 'F' || str.charAt(start + 2) == 'f')) return Double.PositiveInfinity.toLong

      if ((str.charAt(start) == 'N' || str.charAt(start) == 'n') && (str.charAt(start + 1) == 'A' ||
        str.charAt(start + 1) == 'a') && (str.charAt(start + 2) == 'N' || str.charAt(start + 2) == 'n')) return Double.NaN.toLong

    }
    if (len >= 4) {
      if (str.charAt(start) == '-' && (str.charAt(start + 1) == 'I' || str.charAt(start + 1) == 'i') && (str.charAt(start + 2) == 'N' ||
        str.charAt(start + 2) == 'n') && (str.charAt(start + 3) == 'F' || str.charAt(start + 4) == 'f')) return Double.NegativeInfinity.toLong
    }
    str.subSequence(start, stop).toString.toLong
  }

  val emptyStringBuilder=  new java.lang.StringBuilder()
  def colsSlice(m: Int, n: Int, str: String, sa: Array[Int]): java.lang.StringBuilder = {
    // Slice style range,
    if (n > sa.length || n < m || m < 0) throw new GorDataException("colsSlice: illegal columns " + m + ", " + n, n, str)
    if (m == n) return emptyStringBuilder
    val start = if (m == 0) 0 else sa(m - 1) + 1
    val stop = sa(n - 1)
    val strbuff = new java.lang.StringBuilder(stop - start)
    var i = start
    while (i < stop) {
      strbuff.append(str(i)); i += 1
    }
    strbuff
  }

  def colsSelect(c: Array[Int], str: CharSequence, sa: Array[Int]): (Array[Int], java.lang.StringBuilder) = {
    var ci = 0
    var length = 0
    while (ci < c.length) {
      val n = c(ci)
      if (n >= sa.length || n < 0) throw new GorDataException("colsSelect: illegal column " + (n + 1), n+1, c.mkString(","))
      val start = if (n == 0) 0 else sa(n - 1) + 1
      val stop = sa(n)
      length += stop - start
      ci += 1
    }
    if (c.length > 1) length += c.length - 1
    val strbuff = new java.lang.StringBuilder(length)
    val nsa = new Array[Int](c.length)
    ci = 0
    while (ci < c.length) {
      val n = c(ci)
      val start = if (n == 0) 0 else sa(n - 1) + 1
      val stop = sa(n)
      var i = start
      while (i < stop) {
        strbuff.append(str.charAt(i)); i += 1
      }
      nsa(ci) = if (ci == 0) stop - start else nsa(ci - 1) + (stop - start) + 1
      ci += 1
      if (ci < c.length) strbuff.append('\t')
    }
    (nsa, strbuff)
  }

  def apply(chr: String, pos: Int, allCols: java.lang.StringBuilder, sa: Array[Int], bH: BinaryHolder) : Row = {
    new RowBase(chr, pos, allCols, sa, bH)
  }

  def apply(chr: String, pos: Int, theOtherCols: String): Row = StoR(chr + "\t" + pos + "\t" + theOtherCols)

  def apply(x: CharSequence): Row = StoR(x)

  def apply(x: CharSequence, numCols: Int): Row = {
    new RowBase(x, numCols)
  }

  def binary(chr: String, pos: Int, bH: BinaryHolder) = new RowBase(chr, pos, null, null, bH)

  def StoR(str: CharSequence): Row = {
    new RowBase(str)
  }

}
