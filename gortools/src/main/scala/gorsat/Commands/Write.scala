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

import java.util.zip.Deflater

import gorsat.Analysis.{ForkWrite, ForkWriteOptions}
import gorsat.Commands.CommandParseUtilities._
import org.gorpipe.exceptions.{GorParsingException, GorResourceException}
import org.gorpipe.gor.GorContext
import org.gorpipe.model.genome.files.binsearch.GorIndexType

class Write extends CommandInfo("WRITE",
  CommandArguments("-r -c -m -d", "-f -i -t -l -prefix", 1),
  CommandOptions(gorCommand = true, norCommand = true, verifyCommand = true)) {
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {

    var fileName = replaceSingleQuotes(iargs.mkString(" "))
    if (context.getSession.getSystemContext.getServer) {
      context.getSession.getProjectContext.validateWriteAllowed(fileName)
      fileName = context.getSession.getProjectContext.getWritePath(fileName)
    }

    var forkCol = -1
    var remove = false
    var columnCompress: Boolean = false
    var md5 = false
    var idx = GorIndexType.NONE
    var compressionLevel = Deflater.BEST_SPEED
    var useFolder = false

    if (hasOption(args, "-f")) forkCol = columnOfOption(args, "-f", forcedInputHeader, executeNor)
    remove = hasOption(args, "-r")
    useFolder = hasOption(args, "-d")
    columnCompress = hasOption(args, "-c")
    md5 = hasOption(args, "-m")
    if (hasOption(args, "-l")) compressionLevel = stringValueOfOptionWithErrorCheck(args, "-l", Array("0","1","2","3","4","5","6","7","8","9")).toInt

    var indexing = "NONE"

    if (hasOption(args, "-i")) {
      indexing = stringValueOfOptionWithErrorCheck(args, "-i", Array("NONE", "CHROM", "FULL"))
    }

    val prefixFile = if (hasOption(args, "-prefix")) {
      Option(stringValueOfOption(args, "-prefix"))
    } else None

    if (hasOption(args, "-t") && !hasOption(args, "-f")) {
      throw new GorParsingException("Option -t is only valid with the -f option.", "-t")
    }

    val tagArray = replaceSingleQuotes(stringValueOfOptionWithDefault(args, "-t", "")).split(",", -1).map(x => x.trim).distinct

    indexing match {
      case "NONE" => idx = GorIndexType.NONE
      case "CHROM" => idx = GorIndexType.CHROMINDEX
      case "FULL" => idx = GorIndexType.FULLINDEX
    }

    CommandParsingResult(ForkWrite(forkCol, fileName, forcedInputHeader, executeNor, ForkWriteOptions(remove, columnCompress, md5, idx, tagArray, prefixFile, compressionLevel, useFolder)), forcedInputHeader)
  }
}
