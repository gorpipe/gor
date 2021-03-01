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

import gorsat.Analysis.JoinAnalysis
import org.gorpipe.gor.session.GorContext

class LeftJoin extends CommandInfo("LEFTJOIN",
  CommandArguments("-snpsnp -snpseg -segseg -segsnp -varseg -segvar -stdin -r -l -i -ic -ir -t -c -n -m -h -xcis",
  "-s -p -f -e -o -lstop -rstop -xl -xr -maxseg -rprefix -ref -refl -refr", 1, 1),
  CommandOptions(gorCommand = true, memoryMonitorCommand = true, verifyCommand = true, cancelCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {
    JoinAnalysis.parseArguments(context, iargs, args :+ "-l", executeNor, forcedInputHeader)
  }
}