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
import org.gorpipe.exceptions.GorDataException
import org.gorpipe.gor.model.Row
import org.gorpipe.gor.session.GorSession

case class VerifyVariantAnalysis(session: GorSession, refColumn: Int, altColumn: Int, maxLen: Int) extends Analysis {
  override def isTypeInformationNeeded: Boolean = false
  override def isTypeInformationMaintained: Boolean = true

  private val refSeqProvider = session.getProjectContext.createRefSeq()

  override def process(r: Row): Unit = {
    val refFromRow = r.colAsString(refColumn).toString
    val bases = refSeqProvider.getBases(r.chr, r.pos, r.pos + refFromRow.length() - 1)
    if (!refFromRow.equalsIgnoreCase(bases)) {
      throw new GorDataException("Ref does not match the build", refColumn, getHeader(), r.toString)
    }
    val altFromRow = r.colAsString(altColumn)
    if (altFromRow.length() > maxLen) {
      throw new GorDataException("Variant exceeds maximum length", altColumn, getHeader(), r.toString)
    }
    super.process(r)
  }
}
