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

package gorsat.process

import org.gorpipe.gor.stats.StatsCollector
import org.gorpipe.gor.{GorRunner, GorSession}

/**
  * Base class used to execute gor queries. Supports user created session, iterator and runner. All resources are
  * closed within the execution loop.
  */
abstract class GorExecutionEngine{
  var session: GorSession = _

  def execute(): Unit = {
    session = createSession()
    var iterator: PipeInstance = null
    val listener = session.getEventLogger

    session.getGorContext.start("")
    try{
      iterator = createIterator(session)

      val runner = createRunner(session)

      if (runner != null) {
        runner.run(iterator.theInputSource, iterator.thePipeStep)
      }
    } finally {
      if (iterator != null) {
        iterator.close()
      }
    }
    session.getGorContext.end()
    listener.endSession()
  }

  protected def createSession(): GorSession
  protected def createIterator(session: GorSession): PipeInstance
  protected def createRunner(session: GorSession): GorRunner

}
