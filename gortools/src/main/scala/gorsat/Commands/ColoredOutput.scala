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

import gorsat.Analysis.ColoredOutputAnalysis
import gorsat.Commands.CommandParseUtilities.hasOption
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.model.{RowColorize, RowRotatingColorize, RowTypeColorize}
import org.gorpipe.gor.session.GorContext
import org.gorpipe.gor.util.ConsoleColors


class ColoredOutput() extends CommandInfo("COLOREDOUTPUT",
  CommandArguments("-t", "", 0, 0),
  CommandOptions(gorCommand = true, norCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {

    if (context.getSession().getSystemContext().getServer()) {
      throw new GorParsingException(this.name + " is not available for server environment.")
    }

    val typeFormat = hasOption(args, "-t")


    val formatter : RowColorize = if (typeFormat) {
      new RowTypeColorize()
    } else {
      new RowRotatingColorize()
    }

    CommandParsingResult(
      ColoredOutputAnalysis(formatter),
      colorizeHeader(forcedInputHeader, formatter)
    )
  }

  private def colorizeHeader(header: String, formatter: RowColorize) : String = {
    val items = header.split("\t")

    for (i <- 2 until items.length) {
      items(i) = formatter.formatHeaderColumn(i, items(i))
    }

    items.mkString("\t")
  }
}
