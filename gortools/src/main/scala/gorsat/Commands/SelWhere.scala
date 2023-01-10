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

import gorsat.Analysis.Select2
import gorsat.parser.{CalcCompiler, HeaderCVP}
import org.gorpipe.gor.SyntaxChecker
import org.gorpipe.gor.session.GorContext

class SelWhere extends CommandInfo( "SELWHERE",
  CommandArguments("", "", 2, -1, ignoreIllegalArguments = true),
  CommandOptions(gorCommand = true, norCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String],
                                args: Array[String], executeNor: Boolean,
                                forcedInputHeader: String): CommandParsingResult = {
    val expression = args.mkString(" ")
    val syntaxChecker = new SyntaxChecker
    val context = syntaxChecker.parseFilter(expression)
    val calcCompiler = new CalcCompiler()

    val columns = Array[String]("colname", "colnum")
    val types = Array[String]("S", "I")
    calcCompiler.setColumnNamesAndTypes(columns, types)
    val calcLambda = context.accept(calcCompiler)

    val headerColumns = forcedInputHeader.split('\t')
    val cvp = new HeaderCVP(forcedInputHeader)
    val selectedColumns = headerColumns.zipWithIndex.filter({
      case (_, index) => {
        cvp.setCurrentColumn(index + 1)
        calcLambda.evaluateBoolean(cvp)
      }
    }).map({ case (_, index) => index + 1}).toIndexedSeq
    val header = selectedColumns.map(ix => headerColumns(ix - 1)).mkString("\t")
    CommandParsingResult(Select2(selectedColumns:_*), header)
  }
}
