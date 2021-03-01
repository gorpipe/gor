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

package gorsat.parser

import org.gorpipe.gor.model.ColumnValueProvider

/**
  * A forwarding column value provider. When the special column value ReplaceColumn is used the value is
  * looked up using the column currently set as the <i>replaceCol</i>
 *
  * @param cvp The ColumnValueProvider used for actual lookup
  */
case class ReplaceCvp(cvp: ColumnValueProvider) extends ColumnValueProvider {
  var replaceCol: Int = 0

  override def stringValue(col: Int): String = {
    val ix = if (col == -3) replaceCol else col
    cvp.stringValue(ix)
  }

  override def intValue(col: Int): Int = {
    val ix = if (col == -3) replaceCol else col
    cvp.intValue(ix)
  }

  override def longValue(col: Int): Long = {
    val ix = if (col == -3) replaceCol else col
    cvp.longValue(ix)
  }

  override def doubleValue(col: Int): Double = {
    val ix = if (col == -3) replaceCol else col
    cvp.doubleValue(ix)

  }
}
