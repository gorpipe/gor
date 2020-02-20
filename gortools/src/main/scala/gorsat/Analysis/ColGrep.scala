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

case class ColGrep(pattern : String, useAllCols : Boolean, mCols : Array[Int], caseInsensitive : Boolean, inverted: Boolean) extends Analysis {

  val reg = if (caseInsensitive) ("""(?i)""" + pattern + """""").r else pattern.r
  val colArray = mCols

  override def isTypeInformationMaintained: Boolean = true

  override def process(r : Row) {
    val stringToMatch = if (useAllCols) r.toString else r.selectedColumns(colArray)
    reg.findFirstIn(stringToMatch) match {
      case Some(x) => if(!inverted) super.process(r)
      case None => if(inverted) super.process(r)
    }
  }
}
