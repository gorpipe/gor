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

package gorsat.parser

object StringDistance {
  /**
    * Finds the string in the given list that is closest to <i>str</i>, ignoring case.
    *
    * @param str The input string
    * @param maxDistance The maximum distance considered
    * @param strings The candidates to match against
    * @return The closest string from the candidates, or an empty string if none is found
    *         within the maximum distance.
    */
  def findClosest(str: String, maxDistance: Int, strings: Seq[String]): String = {
    if(strings.isEmpty) return ""

    val distances = strings.map(s => levenshtein(str.toUpperCase, s.toUpperCase))
    val lowest = distances.min
    if(lowest <= maxDistance) {
      val ix = distances.indexOf(lowest)
      strings(ix)
    } else {
      ""
    }
  }

  /**
    * Find the Levenshtein distance between two strings, that is, the number of edits
    * (deletions, insertions and substitutions) required to transform the former string
    * into the latter.
    * @param str1 First input string
    * @param str2 Second input string
    * @return The Levenshtein distance between the strings
    */
  def levenshtein(str1: String, str2: String): Int = {
    def min(nums: Int*): Int = nums.min

    val lenStr1 = str1.length
    val lenStr2 = str2.length

    val d: Array[Array[Int]] = Array.ofDim(lenStr1 + 1, lenStr2 + 1)

    for (i <- 0 to lenStr1) d(i)(0) = i
    for (j <- 0 to lenStr2) d(0)(j) = j

    for (i <- 1 to lenStr1; j <- 1 to lenStr2) {
      val cost = if (str1(i - 1) == str2(j - 1)) 0 else 1

      d(i)(j) = min(
        d(i-1)(j  ) + 1,     // deletion
        d(i  )(j-1) + 1,     // insertion
        d(i-1)(j-1) + cost   // substitution
      )
    }

    d(lenStr1)(lenStr2)
  }
}
