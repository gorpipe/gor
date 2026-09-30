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

import gorsat.Commands.CommandParseUtilities._
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.session.GorContext

/**
 * VARNORM_WITH_BUILD build refcol altcol [options] - same as VARNORM but normalizes against the reference build
 * (chromSeq folder) given as the first argument instead of the project default build.
 */
class VarNormWithBuild extends CommandInfo("VARNORM_WITH_BUILD",
  CommandArguments("-seg -left -right -trim", "-span", 3, 3),
  CommandOptions(gorCommand = true, verifyCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {
    val build = replaceSingleQuotes(iargs(0))
    // A missing build would silently read as N and leave variants unnormalized, so fail up front.
    // Check with the unsecure reader, as that is what RefSeqFromChromSeq reads the build with.
    if (!context.getSession.getProjectContext.getFileReader.unsecure().exists(build)) {
      throw new GorParsingException(s"Reference build $build does not exist: ", "build", build)
    }
    VarNorm.parse(context, iargs(1), iargs(2), args, executeNor, forcedInputHeader, Some(build))
  }
}
