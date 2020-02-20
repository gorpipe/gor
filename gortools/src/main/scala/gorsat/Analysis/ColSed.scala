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
import org.gorpipe.model.genome.files.gor.Row
import org.gorpipe.model.gor.RowObj

case class ColSed(matchPattern : String, replacePattern : String,
                  replaceCols : Array[Boolean], caseInsensitive : Boolean, replaceAll : Boolean) extends Analysis {

  val reg = if (caseInsensitive) ("""(?i)""" + matchPattern + """""").r else matchPattern.r
  val numCols = replaceCols.length
  var modCols = new Array[CharSequence](numCols)

  override def isTypeInformationMaintained: Boolean = true

  override def process(r : Row) {

    reg.findFirstIn(r.toString) match {
      case Some(x) => {
        modCols(0) = r.colAsString(0)
        modCols(1) = r.colAsString(1)
        var i = 2
        while (i < numCols) {
          if (replaceCols(i)) {
            if (replaceAll) modCols(i) = reg.replaceAllIn(r.colAsString(i),replacePattern)
            else modCols(i) = reg.replaceFirstIn(r.colAsString(i),replacePattern)
          } else modCols(i) = r.colAsString(i)
          i += 1
        }

        super.process(RowObj(modCols.mkString("\t")))
      }
      case None => super.process(r)
    }
  }
}