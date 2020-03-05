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

import gorsat.QueryHandlers.{GeneralQueryHandler, QueryHandler}
import org.gorpipe.gor.GorContext

/**
  * Static method to create an instance of ScriptExecutionEngine based on the current configuration represented in
  * the gor pipe session.
  */
object ScriptEngineFactory {

  def create(context: GorContext, scriptAnalyser: Boolean) : ScriptExecutionEngine = {
    create(context, scriptAnalyser, doHeader = true)
  }

  def create(context: GorContext, scriptAnalyser: Boolean, doHeader: Boolean) : ScriptExecutionEngine = {
    val remoteQueryHandler = if (context.getSession.getProjectContext.getQueryHandler != null) {
      context.getSession.getProjectContext.getQueryHandler
    } else {
      new QueryHandler(context)
    }

    val localQueryHandler = new GeneralQueryHandler(context, doHeader)
    val analyzer:ScriptExecutionListener = if(scriptAnalyser) new ConsoleLogListener() else new DefaultListener

    new ScriptExecutionEngine(remoteQueryHandler, localQueryHandler, context, analyzer)
  }
}