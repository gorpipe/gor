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

import gorsat.Analysis.ThrowIfAnalysis
import gorsat.Commands.CommandParseUtilities.hasOption
import org.gorpipe.gor.session.GorContext

class ThrowIf extends CommandInfo("THROWIF",
  CommandArguments("-retriable", "", 1, -1, ignoreIllegalArguments = true),
  CommandOptions(gorCommand = true, norCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String],
                                args: Array[String], executeNor: Boolean, forcedInputHeader: String)
  : CommandParsingResult =
  {
    val pipeStep = ThrowIfAnalysis(context, executeNor, iargs.mkString(" "), forcedInputHeader, hasOption(args, "-retriable"))
    CommandParsingResult(pipeStep, forcedInputHeader)
  }
}