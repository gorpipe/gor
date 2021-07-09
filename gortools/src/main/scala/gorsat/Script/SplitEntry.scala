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

package gorsat.Script

case class SplitEntry(chrom: String, start: Int, end: Int, tag: String = "") {
  def getRange: String = {
    if (end > 0) {
      chrom + ":" + start + "-" + end
    } else {
      chrom + (if (start > 0) ":" + start + "-" else "")
    }

  }

  // TODO: Can this be removed.  Note: when we switch this method there was minor change in the behaviour for
  //       arbitrary splits with overlap (the overlap was not included in the filter but only in the start/end).
  //       And for nested splits they were 1 off for the end.
  def getFilter: String = {
    if (end > 0) {
      0.max(start) + " <= #2i and #2i <= " + end
    } else if (start > 0) {
      start + " <= #2i"
    } else {
      ""
    }
  }
}
