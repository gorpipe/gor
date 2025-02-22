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

package gorsat.Commands

import org.gorpipe.exceptions.GorSystemException
import org.gorpipe.gor.model.Row
import org.gorpipe.gor.session.GorContext
import org.gorpipe.gor.stats.StatsCollector

abstract class Analysis() extends Processor with Cloneable {
  var pipeTo: Analysis = _
  var pipeFrom: Processor = _
  var nextProcessor: Processor = _
  var alreadyFinished = false
  var isInErrorState = false
  var rowHeader: RowHeader = _ // Input row header.
  var cloned : Analysis = _
  var isCloned = false

  private[this] var _statsCollector: StatsCollector = _
  private[this] var _context: GorContext = _
  var statsSenderId: Int = -1
  var statsSenderName: String = ""
  var statsSenderAnnotation: String = ""
  def statsCollector: StatsCollector = _statsCollector

  def setContext(value: GorContext): Unit = {
    _context = value
    _statsCollector = value.getStats
    if (statsCollector != null && statsSenderName.nonEmpty) {
      statsSenderId = statsCollector.registerSender(statsSenderName, statsSenderAnnotation)
    }
  }

  def statsInc(name: String): Unit = {
    if(statsCollector != null) {
      statsCollector.inc(statsSenderId, name)
    }
  }

  def statsDec(name: String): Unit = {
    if(statsCollector != null) {
      statsCollector.dec(statsSenderId, name)
    }
  }

  def statsAdd(name: String, delta: Double): Unit = {
    if(statsCollector != null) {
      statsCollector.add(statsSenderId, name, delta)
    }
  }

  def init(cloned: Analysis) = {
    isCloned = true
    if (pipeTo != null) {
      cloned.pipeTo = pipeTo.clone
      cloned.nextProcessor = cloned.pipeTo

      if( cloned.pipeTo != null ) {
        cloned.pipeTo.pipeFrom = cloned
      }
    } else if (pipeFrom != null) {
      cloned.pipeFrom = pipeFrom.asInstanceOf[Analysis].clone
      cloned.pipeFrom.asInstanceOf[Analysis].pipeTo = cloned
      cloned.pipeFrom.asInstanceOf[Analysis].nextProcessor = cloned
    }
    cloned.rowHeader = rowHeader
  }

  override def clone: Analysis = {
    cloned = new Analysis() {}
    init(cloned)
    cloned
  }

  override def toString: String = {
    (if (pipeFrom == null) "" else " | ") + (if (pipeTo != null) this.getClass.getSimpleName + pipeTo.toString else this.getClass.getSimpleName)
  }

  def from(from: Processor) : Unit = {
    pipeFrom = from
  }

  def reportWantsNoMore() : Unit = {
    if (pipeFrom != null && !wantsNoMore) pipeFrom.reportWantsNoMore()
    wantsNoMore = true
  }

  def reset() : Unit = {
    if (pipeFrom != null && wantsNoMore) pipeFrom.reset()
    if (pipeTo != null && wantsNoMore) {
      wantsNoMore = false
      pipeTo.reset()
    }
    wantsNoMore = false
    alreadyFinished = false
  }

  def |(to: Analysis): Analysis = {
    if (pipeTo != null) {
      pipeTo | to
    }
    else {
      pipeTo = to
      nextProcessor = to
      to.from(this)
      if (rowHeader != null) {
        setRowHeader(rowHeader)
      }
    }
    this
  }

  def |(to: Output): Analysis = {
    var last: Analysis = this
    while (last.pipeTo != null) last = last.pipeTo
    last.nextProcessor = to
    to.from(this)
    this
  }

  def getName: String = {
    null
  }

  // Input header string
  def getHeader: String = {
    if (rowHeader != null) rowHeader.toString else null
  }

  def setup() : Unit = {}

  // To be implemented by the Analysis developer
  final def securedSetup(oe: Throwable) : Unit = {
    try {
      setup()
    } catch {
      case e: Throwable =>
        if (oe != null && nextProcessor != null) nextProcessor.securedSetup(oe) // This exception happened first
        else if (nextProcessor != null) nextProcessor.securedSetup(e)
        if (oe != null) throw oe
        else throw e // Either way, this will throw exception, but ensure that setup is called to end
    }
    if (nextProcessor != null) nextProcessor.securedSetup(oe)
  }

  def process(r: Row) : Unit = {
    if (alreadyFinished)
      throw new GorSystemException("Analysis step already finished", null)
    if (!wantsNoMore && nextProcessor != null)
      nextProcessor.process(r)
  }

  def finish() : Unit = {}

  // To be implemented by the Analysis developer
  final def securedFinish(oe: Throwable) : Unit = {
    if (alreadyFinished) return
    try {
      isInErrorState = oe != null
      finish()
    } catch {
      case e: Throwable =>
        if (oe != null && nextProcessor != null) nextProcessor.securedFinish(oe) // This exception happened first
        else if (nextProcessor != null) nextProcessor.securedFinish(e)
        if (oe != null) throw oe
        else throw e // Either way, this will throw exception, but ensure that finish is called to end
    }
    if (nextProcessor != null) nextProcessor.securedFinish(oe)
    alreadyFinished = true
  }

  def setRowHeader(header: RowHeader): Unit = {
    rowHeader = header
    if (isTypeInformationMaintained && pipeTo != null)
      pipeTo.setRowHeader(rowHeader)
  }

  def isTypeInformationNeeded: Boolean = {
    if (pipeTo == null) false else pipeTo.isTypeInformationNeeded
  }

  /**
   * Does execution of this step preserve column type information?
   * The value false (default) means if a downstream step requires types, then they must be inferred.
   * true means that columns, except for those returned by columnsWithoutTypes(), already
   * have type info which need not be inferred.
   */
  def isTypeInformationMaintained: Boolean = false

  /**
   * For the case isTypeInformationMaintained=true, which columns do not have their types maintained
   * null is valid, and equivalent to an empty array, i.e. all column types are maintained
   * (Not to be invoked in subclasses with isTypeInformationMaintained=false which means no column types are maintained)
   * Result is used logically as the set of elements (i.e., .contains()) so duplicated elements are harmless
   *
   * @param untypedColumns column numbers which have no valid types from prior step
   * @return column numbers on the output whose type is not present or not valid
   */
  def columnsWithoutTypes(untypedColumns: Array[Int]): Array[Int] = {
    // default step does not add untyped columns or invalidate type of existing columns
    untypedColumns
  }
}
