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

package gorsat.Commands

import gorsat.Analysis.BugN
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.session.GorContext

class Bug extends CommandInfo("BUG",
  CommandArguments("", "", 0, 1),
  CommandOptions(gorCommand = true)) {

  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {
    var TopNv = "process:0.01"
    try {
      TopNv = iargs(0)
    } catch {
      case e: Exception => /* do nothing */
    }

    // We should test this further
    val subArguments = TopNv.split(':')

    if (subArguments.length != 2) throw new GorParsingException("Incorrect formatting of argument, should e [setup,process,finish]:[value from 0-1]")

    val bugTypes = Array("process", "setup", "finish")
    val bugType = subArguments(0).toLowerCase()

    if (!bugTypes.contains(bugType)) throw new GorParsingException("Allowed bug types are setup, process or finish")

    val value = try { Some(subArguments(1).toDouble) } catch { case _ : Throwable => None }
    if (value.isEmpty) throw new GorParsingException("Number value not well defined.")

    val numberValue = value.get
    if (numberValue < 0.0 || numberValue > 1.0) throw new GorParsingException("Number value should be between 0 and 1.")

    CommandParsingResult(BugN(bugType, numberValue), forcedInputHeader)
  }

}
