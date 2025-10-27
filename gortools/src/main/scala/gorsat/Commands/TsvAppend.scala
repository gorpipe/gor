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

import gorsat.Analysis.{ForkWrite, OutputOptions}
import gorsat.Commands.CommandParseUtilities._
import org.apache.commons.io.FilenameUtils
import org.gorpipe.exceptions.{GorParsingException, GorResourceException}
import org.gorpipe.gor.driver.meta.DataType
import org.gorpipe.gor.session.GorContext
import org.gorpipe.gor.util.StringUtil


class TsvAppend extends CommandInfo("TSVAPPEND",
  CommandArguments("-noheader", "-prefix -link", 0),
  CommandOptions(gorCommand = false, norCommand = true, verifyCommand = true)) {
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {

    val fileName = replaceSingleQuotes(iargs.mkString(" "))
    if (fileName.isEmpty) throw new GorResourceException("No file or folder specified","")

    val fileType = "." + FilenameUtils.getExtension(fileName.toLowerCase())
    val ALLOWED_FILE_TYPES = Set(".nor", ".tsv")
    if (!ALLOWED_FILE_TYPES.contains(fileType)) {
      throw new GorResourceException("WRITE error: invalid file type", fileName)
    }

    val fileExists = context.getSession.getProjectContext.getFileReader.exists(fileName)

    val skipHeader = hasOption(args, "-noheader") || fileExists
    if (!fileExists && skipHeader && DataType.getWritableFormats().contains(fileType)) {
      throw new GorParsingException("Option -noheader (skip header) is not valid with gor/gorz/nor/norz")
    }

    if (fileExists) {
      val existingHeader = context.getSession.getProjectContext.getFileReader.readHeaderLine(fileName)

      if (!StringUtil.isEmpty(forcedInputHeader)) {
        val headerToCheck = forcedInputHeader.replaceFirst("ChromNOR\tPosNOR\t", "")
        if (!headerToCheck.equals(existingHeader)) {
          throw new GorResourceException(s"WRITE error:  When appending to $fileName to the file header ($existingHeader) " +
            s"is different to the addition header ($forcedInputHeader)", fileName)
        }
      }
    }

    var prefixFile: Option[String] = None
    var prefix: Option[String] = None
    if (hasOption(args, "-prefix")) {
      val prfx = stringValueOfOption(args, "-prefix")
      if (prfx.startsWith("'")) prefix = Option(prfx.substring(1, prfx.length - 1).replace("\\n", "\n").replace("\\t", "\t"))
      else prefixFile = Option(prfx)
    }

    val (link, linkVersion) = if (hasOption(args, "-link")) {
      (stringValueOfOption(args, "-link"), 1)
    } else {
      ("", 0)
    }

    val fixedHeader = forcedInputHeader.split("\t").slice(0, 2).mkString("\t")

    CommandParsingResult(
      ForkWrite(-1,
        fileName,
        context.getSession,
        forcedInputHeader,
        OutputOptions(
          nor = true,
          prefix=prefix,
          prefixFile=prefixFile,
          skipHeader=skipHeader,
          linkFile=link,
          linkFileVersion=linkVersion,
          command=argString
        )
      ),
      fixedHeader
    )
  }
}
