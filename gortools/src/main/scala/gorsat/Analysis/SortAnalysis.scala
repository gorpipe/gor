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

import gorsat.Commands.{Processor, _}
import org.gorpipe.gor.model.Row
import org.gorpipe.gor.model.Row.SortInfo
import org.gorpipe.gor.session.GorSession

case class SortAnalysis(header: String,
                        session: GorSession,
                        range: Int,
                        sortInfo: Array[Row.SortInfo] = null)
  extends BinAnalysis(SortRowHandler(2 * (1 + range / 1001.min(range))), BinAggregator(SortFactory(header,
    session, sortInfo, 1000.min(range) + 2), 1000.min(range) + 2, 1000.min(range)))
{
  override def isTypeInformationMaintained: Boolean = true
}

case class SortState(sortStep: SortGenome) extends BinState {
  def initialize(binInfo: BinInfo): Unit = {}

  def process(r: Row) {
    sortStep.process(r)
  }

  def sendToNextProcessor(bi: BinInfo, nextProcessor: Processor) {
    sortStep.nextProcessor = nextProcessor
    sortStep.finish
    sortStep.reinit()
  }
}

case class SortFactory(header: String, session: GorSession, sortInfo: Array[SortInfo], div: Int) extends BinFactory {
  def create: BinState = SortState(SortGenome(header, session, sortInfo, div))
}

case class SortRowHandler(binsize: Int) extends RowHandler {
  val binIDgen = RegularBinIDgen(binsize)

  def process(r: Row, BA: BinAggregator) {
    val chr = r.chr
    val pos = r.pos
    val binID = binIDgen.ID(pos)
    val (sta, sto) = binIDgen.StartAndStop(binID)
    BA.update(r, binID, chr, sta, sto)
  } // One ouput row per input row
}