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

package gorsat.process

import java.util.UUID

import gorsat.Commands.CommandParseUtilities
import gorsat.gorsatGorIterator.MapAndListUtilities
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.model.DefaultFileReader

object PipeOptions {
  def parseInputArguments(args:Array[String]) : PipeOptions = {
    val options = new PipeOptions()
    options.parseOptions(args)
    options
  }

  def getQueryFromArgs(args: Array[String]): String = {
    val fileName = CommandParseUtilities.stringValueOfOptionWithDefault(args, "-script", null)
    val fromArgs = getRawQueryFromArgs(args)
    if (fromArgs.nonEmpty && fileName != null) {
      throw new GorParsingException("-script option used when query is passed on command line")
    }
    val rawQuery = if (fileName != null) getQueryFromFile(fileName) else fromArgs
    cleanUpQuery(rawQuery)
  }

  private def getQueryFromFile(fileName: String) = {
    val fileReader = new DefaultFileReader("")
    MapAndListUtilities.readArray(fileName, fileReader).mkString(" ")
  }

  private def getRawQueryFromArgs(args: Array[String]) = {
    var i = 0
    var result = ""
    while( i < args.length && !allOptionNames.contains(args(i))) {
      result += args(i) + " "
      i += 1
    }
    result
  }

  private def cleanUpQuery(rawQuery: String) = {
    val fixedQuery = rawQuery.replace("\n", " ")
    val argString = CommandParseUtilities.quoteSafeSplitAndTrim(fixedQuery, ' ').mkString(" ")
    CommandParseUtilities.quoteSafeSplitAndTrim(argString, ';').mkString(";")
  }

  private val allOptionNames: List[String] = List(
    "-aliases",
    "-stdin",
    "-nor",
    "-config",
    "-cachedir",
    "-logdir",
    "-workers",
    "-script",
    "-helpfile",
    "-stacktrace",
    "-version",
    "-prepipe",
    "-scriptanalyser",
    "-queryhandler",
    "-gorroot",
    "-requestid",
    "-stats")
}

/**
  * This class contains all input variables for PipeInstance
  */
class PipeOptions {

  // Input query either direct from input or from the -script option
  var query: String = _
  //Specifies whether gor should be run in pre-pipe mode, used for error checks and syntax highlighting. Value from the -prepipe option
  var prePipe: Boolean = false
  // The first input step is from the stdin, no input source in the query. Value from the -stdin option
  var stdIn: Boolean = false
  // Gor should run in a forced not context mode. Value from the -nor option
  var norContext: Boolean = false
  // Shows the stack trace when errors occur. Value from the -stacktrace option
  var showStackTrace: Boolean = false
  // Sets the current project root. Value from the -gorroot option
  var gorRoot: String = _
  // Sets the cache dir, full path or relative to project root. Value from the -cacheDir option
  var cacheDir: String = _
  // Sets the log directory, normally not used and only for legacy compatibility. Value from the -logdir option (depricated?)
  var logDir: String = _
  // Specifies which query handler to use. Value from the -queryhandler option
  var queryHandler: String = _
  // Sets the path to an alias file, full path or relative to project root. Value from the -aliases option
  var aliasFile: String = _
  // Relative or full path to the current config. If gorroot is set this path is relative to root. Value from the -cachedir option
  var configFile: String = _
  // Path to an external help file. Value from the -helpfile option
  var helpFile: String = _
  // Specifies the number of workers to use for the current execution. Value from the -queryhandler option
  var workers: Int = 0
  // Show current build version and git SHA hash commit
  var version: Boolean = false
  // Sets the external requestid used for the gor query
  var requestId:String = _
  // Enable request stats?
  var stats: Boolean = false

  def parseOptions(args: Array[String]): Unit = {
    this.aliasFile = CommandParseUtilities.stringValueOfOptionWithDefault(args, "-aliases", null)
    this.stdIn = CommandParseUtilities.hasOption(args, "-stdin")
    this.norContext = CommandParseUtilities.hasOption(args, "-nor")
    this.configFile = CommandParseUtilities.stringValueOfOptionWithDefault(args, "-config", null)
    this.cacheDir = CommandParseUtilities.stringValueOfOptionWithDefault(args, "-cachedir", System.getProperty("java.io.tmpdir"))
    this.logDir = CommandParseUtilities.stringValueOfOptionWithDefault(args, "-logdir", null)
    this.workers = CommandParseUtilities.intValueOfOptionWithDefault(args, "-workers")
    this.query = PipeOptions.getQueryFromArgs(args)
    this.helpFile = CommandParseUtilities.stringValueOfOptionWithDefault(args, "-helpfile", null)
    this.showStackTrace = CommandParseUtilities.hasOption(args, "-stacktrace")
    this.version = CommandParseUtilities.hasOption(args, "-version")
    this.stats = CommandParseUtilities.hasOption(args, "-stats")

    // Following options should not be part of the documentation
    this.prePipe = CommandParseUtilities.hasOption(args, "-prepipe")
    this.queryHandler = CommandParseUtilities.stringValueOfOptionWithDefault(args, "-queryhandler", null)
    this.gorRoot = CommandParseUtilities.stringValueOfOptionWithDefault(args, "-gorroot", "")
    this.requestId = CommandParseUtilities.stringValueOfOptionWithDefault(args, "-requestid", UUID.randomUUID().toString)
  }
}
