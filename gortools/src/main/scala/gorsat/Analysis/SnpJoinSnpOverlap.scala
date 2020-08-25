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

import gorsat.Utilities.AnalysisUtilities.ParameterHolder
import gorsat.Commands.Analysis
import org.gorpipe.model.gor.iterators.RowSource

case class SnpJoinSnpOverlap(ph: ParameterHolder, rightSource: RowSource, missingB: String, leftJoin: Boolean, fuzz: Int,
                             leq: List[Int], req: List[Int], plain: Boolean) extends Analysis {
  this | SegOverlap(ph, rightSource, missingB, leftJoin, fuzz, "snpsnp", 2, 2, leq, req, 1, plain)
}
