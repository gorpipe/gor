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

import gorsat.Analysis.TeeAnalysis
import gorsat.Commands.CommandParseUtilities._
import gorsat.Outputs.StdOut2
import gorsat.process.PipeInstance
import org.gorpipe.gor.GorContext

class Tee extends CommandInfo("TEE",
  CommandArguments("-h", " ", 1, 1),
  CommandOptions(gorCommand = true, norCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {

    val outputPipeCmd = CommandParseUtilities.parseNestedProcessCommand(iargs(0))

    val combinedHeader = if (forcedInputHeader == "") "?" else forcedInputHeader

    context.getSession.setNorContext(executeNor)
    val teeIt = PipeInstance.createGorIterator(context)
    teeIt.scalaPipeStepInit(outputPipeCmd,combinedHeader)

    val teeStep = teeIt.getPipeStep
    val teeHeader = if (hasOption(args,"-h")) teeIt.getHeader else ""

    val pipeStep: Analysis = TeeAnalysis(teeStep | StdOut2(teeHeader))

    CommandParsingResult(pipeStep, teeHeader)
  }
}
