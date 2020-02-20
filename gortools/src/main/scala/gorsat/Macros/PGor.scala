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

package gorsat.Macros

import gorsat.Commands.{CommandArguments, CommandParseUtilities}
import gorsat.Script
import gorsat.Script._
import org.gorpipe.gor.GorContext

/***
  * PGOR macro used to preprocess standalone pgor commands into create statement plus gor query. Also performs expansion
  * based on active build and splits. Also if a custom split options are used it performs a split based on input chromosome,
  * start and stop positions. pgor expansions will always result in gor queries in the form of 'gor -p [position and range]
  * <([original query])'.
  */

class PGor extends MacroInfo("PGOR", CommandArguments("-nowithin", "", 1, -1, ignoreIllegalArguments = true)) {

  override protected def processArguments(createKey: String,
                                          create: ExecutionBlock,
                                          context: GorContext,
                                          doHeader: Boolean,
                                          inputArguments: Array[String],
                                          options: Array[String]): MacroParsingResult = {


    var partitionedGorCommands = Map.empty[String, ExecutionBlock]
    var theDependencies: List[String] = Nil

    val replacePattern = if (SplitManager.useWholeChromosomeSplit(create.query))
      SplitManager.REGULAR_REPLACEMENT_PATTERN else SplitManager.SPLIT_REPLACEMENT_PATTERN


    if (!doHeader) {
      val noWithin = CommandParseUtilities.hasOption(options, "-nowithin")

      val theKey = createKey.slice(1, createKey.length - 1)

      val gorReplacement = if( noWithin ) "gor -nowithin -p " else "gor -p "
      val partitionKey = "[" + theKey + "_" + replacePattern + "]"
      val newQuery = gorReplacement + replacePattern + " <(" + create.query.trim.slice(5, create.query.length) + ")"
      partitionedGorCommands += (partitionKey -> Script.ExecutionBlock(partitionKey, newQuery,
        create.dependencies, create.batchGroupName))

      val splitManager = SplitManager.createFromCommand(create.groupName, newQuery, context)

      splitManager.chromosomeSplits.keys.foreach(chrKey => {
        val parKey = "[" + theKey + "_" + chrKey + "]"
        theDependencies ::= parKey
      })

      val theCommand = splitManager.chromosomeSplits.keys.foldLeft("gordict") ((x, y) => x + " [" + theKey + "_" + y + "] " +
        splitManager.chromosomeSplits(y).range)
      partitionedGorCommands += (createKey -> Script.ExecutionBlock(create.groupName, theCommand,
        theDependencies.toArray, create.batchGroupName, isDictionary = true))
    } else {
      partitionedGorCommands += (createKey -> ExecutionBlock(create.groupName,
        "xxxxgor " + create.query.trim.slice(5, create.query.length),
        create.dependencies, create.batchGroupName)) // this should nolonger happen
    }

    MacroParsingResult(partitionedGorCommands, null)
  }

  override def preProcessCommand(commands: Array[String], context: GorContext): Array[String] = {
    standardGorPreProcessing(commands, context, "thepgorquery")
  }

}