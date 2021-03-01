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

package gorsat.Macros

import gorsat.Commands.CommandArguments
import gorsat.Script._
import org.gorpipe.gor.session.GorContext

/**
  * Macro which expands report builder (*.yml) files into gor script.
  */
class TableFunction extends MacroInfo("TABLEFUNCTION", CommandArguments("", "", 1, -1, ignoreIllegalArguments = true)) {

  override protected def processArguments(createKey: String,
                                          create: ExecutionBlock,
                                          context: GorContext,
                                          doHeader: Boolean,
                                          inputArguments: Array[String],
                                          options: Array[String],
                                          skipCache: Boolean): MacroParsingResult = {

    var creates = Map.empty[String, ExecutionBlock]
    val newQuery = "gor " + create.query.substring(this.name.length+1)
    creates += (createKey -> ExecutionBlock(create.groupName, newQuery, create.signature, create.dependencies, create.batchGroupName))

    MacroParsingResult(creates, null)
  }

  override def preProcessCommand(commands: Array[String], context: GorContext): Array[String] = {
    standardGorPreProcessing(commands, context, "tablefunctionquery")
  }
}