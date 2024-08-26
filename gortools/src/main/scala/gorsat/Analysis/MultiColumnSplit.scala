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

package gorsat.Analysis

import gorsat.Commands.Analysis
import org.gorpipe.gor.model.Row
import org.gorpipe.model.gor.RowObj

import scala.collection.mutable.StringBuilder

/**
 * Implementation of the SPLIT command (not COLSPLIT) when multiple columns are split
 */
case class MultiColumnSplit(totalNumberOfColumns: Int, splitColumns: Array[Int], separator: String, empty: String) extends Analysis {
  val sc = splitColumns.sorted
  val splitcol = Range(0, totalNumberOfColumns).map(x => if (sc.contains(x)) true else false).toArray
  val splitcolArray = new Array[Array[String]](sc.length)

  override def isTypeInformationMaintained: Boolean = true

  /*
   * After split, we want the type of the split columns to be re-inferred
   */
  override def columnsWithoutTypes(invalidOnInput: Array[Int]): Array[Int] = {
    if (invalidOnInput == null) sc else invalidOnInput.concat(sc)
    // this might yield duplicated values in the result, which cause no harm
  }

  override def process(r: Row): Unit = {
    if (!r.toString.contains(separator)) super.process(r)
    else {
      var i = 0
      var maxSplitLength = 0
      while (i < sc.length) {
        splitcolArray(i) = r.colAsString(sc(i)).toString.split(separator, -1)
        maxSplitLength = maxSplitLength.max(splitcolArray(i).length)
        i += 1
      }
      var j = 0
      while (j < maxSplitLength) {
        val strbuff = new StringBuilder(r.getAllCols.length)
        strbuff.append(r.chr)
        var i = 1
        var sc_i = 0
        while (i < totalNumberOfColumns) {
          if (!splitcol(i)) strbuff.append("\t" + r.colAsString(i))
          else {
            if (j < splitcolArray(sc_i).length) strbuff.append("\t" + splitcolArray(sc_i)(j))
            else strbuff.append("\t" + empty)
            sc_i += 1
          }
          i += 1
        }
        super.process(RowObj(strbuff.toString))
        j += 1
      }
    }
  }
}
