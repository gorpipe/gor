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

package gorsat.process

import gorsat.Commands.CommandParseUtilities
import gorsat.DynIterator.{DynamicNorSource, DynamicRowSource}
import gorsat.Iterators.SingleFileSource
import gorsat.Utilities.IteratorUtilities
import gorsat.{DynIterator, Iterators}
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.session.GorContext
import org.gorpipe.model.gor.iterators.RowSource

/**
  * SourceProvider simplifies access to sources used by various commands, where the source can be either
  * a file or a nested query.
  */
class SourceProvider(inputSource: String, context: GorContext, executeNor: Boolean, isNor: Boolean) {

  var iteratorCommand = ""
  var usedFiles = Array.empty[String]
  var header = ""
  var source: RowSource = _
  var dynSource: DynIterator.DynamicRowSource = _

  if (inputSource.slice(0, 2) == "<(") {
    handleNestedCommand()
  } else {
    handleFile()
  }

  private def handleFile(): Unit = {
    if (isNor) {
      header = IteratorUtilities.getFirstLine(inputSource, context.getSession)
      usedFiles +: inputSource
    } else {
      source = if (executeNor) {
        if (inputSource.endsWith(".gorz") || inputSource.endsWith(".gor")) {
          new Iterators.ServerNorGorSource(inputSource, context, executeNor)
        } else {
          new Iterators.NorInputSource(inputSource, context.getSession.getProjectContext.getFileReader, false, true, 0,
            false, false)
        }
      } else {
        new SingleFileSource(inputSource, context.getSession.getProjectContext.getRoot, context)
      }
      usedFiles ++= Array(inputSource)

      header = source.getHeader
      usedFiles +: inputSource
    }
  }

  private def handleNestedCommand(): Unit = {
    iteratorCommand = CommandParseUtilities.parseNestedCommand(inputSource).trim
    dynSource = inferSource()
    usedFiles ++= dynSource.usedFiles
    source = dynSource
    header = dynSource.getLineHeader
    dynSource.close()
  }

  private def inferSource(): DynamicRowSource = {
    if (isNor) {
      if (!iteratorCommand.toUpperCase.trim.startsWith("NOR")) {
        throw new GorParsingException("Nested queries in this context must be defined using the NOR command.")
      }
      new DynamicNorSource(iteratorCommand, context)
    } else {
      new DynamicRowSource(iteratorCommand, context)
    }
  }
}

object SourceProvider {
  def apply(inputSource: String, context: GorContext, executeNor: Boolean, isNor: Boolean): SourceProvider = {
    new SourceProvider(inputSource, context, executeNor, isNor)
  }
}
