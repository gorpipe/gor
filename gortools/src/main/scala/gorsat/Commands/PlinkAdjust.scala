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

import gorsat.Commands.CommandParseUtilities.{hasOption, stringValueOfOption}
import gorsat.external.plink.PlinkAdjustAdaptor
import org.gorpipe.gor.GorContext

class PlinkAdjust extends CommandInfo("PLINKADJUSTMENT",
  CommandArguments("", "", 0, -1, ignoreIllegalArguments = true),
  CommandOptions(gorCommand = true, norCommand = true))
{
  override def processArguments(context: GorContext, argString: String, inputArguments: Array[String], options : Array[String], executeNor : Boolean, forcedInputHeader : String, commandRuntime:CommandRuntime): CommandParsingResult = {
    val sort = hasOption(options, "-s")

    val pip = if(hasOption(options, "-c")) new PlinkAdjustAdaptor(forcedInputHeader, stringValueOfOption(options, "-c"), sort) else new PlinkAdjustAdaptor(forcedInputHeader, sort)
    CommandParsingResult(pip, pip.getHeader)
  }
}