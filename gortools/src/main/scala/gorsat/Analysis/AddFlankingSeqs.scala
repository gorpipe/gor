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

package gorsat.Analysis

import gorsat.Commands.{Analysis, RowHeader}
import org.gorpipe.gor.model.Row
import org.gorpipe.gor.session.GorSession
import org.gorpipe.model.gor.iterators.RefSeq

case class AddFlankingSeqs(session: GorSession, l : Int, rCols : Array[Int], outgoingHeader: RowHeader) extends Analysis {
  private val buildSize = session.getProjectContext.getReferenceBuild.getBuildSize

  override def isTypeInformationMaintained: Boolean = true

  override def setRowHeader(header: RowHeader): Unit = {
    rowHeader = header
    if (pipeTo != null) {
      pipeTo.setRowHeader(outgoingHeader.propagateTypes(rowHeader))
    }
  }

  object GenomeLookup {
    val refSeq: RefSeq = session.getProjectContext.createRefSeq()
    def theBase(chr : String, pos : Int): Char = {
      refSeq.getBase(chr,pos)
    }

    def close(): Unit = {
      refSeq.close()
    }
  }

  def sequence4Col(r : Row, l  : Int, c : Int) : String = {
    val pos = r.colAsInt(c)
    val chr = r.chr
    val seqStart = if (pos - l > 0) pos-l else 1
    val seqEnd = if (pos + l < buildSize.get(chr)) pos + l else buildSize.get(chr)-1
    val theSeq = new StringBuilder(100)
    var i = seqStart
    while (i <= seqEnd) {
      if (i == pos) {
        theSeq.append("(")
        theSeq.append(GenomeLookup.theBase(chr,pos))
        theSeq.append(")")
      } else theSeq.append(GenomeLookup.theBase(chr,i))
      i += 1
    }
    theSeq.toString()
  }
  override def process(r : Row) {
    val seq4Cols = rCols.map( c => sequence4Col(r,l,c) )
    super.process(r.rowWithAddedColumn(seq4Cols.tail.foldLeft(seq4Cols.head) ( _ +"\t"+ _) ))
  }
  override def finish() {
    GenomeLookup.close()
  }
}
