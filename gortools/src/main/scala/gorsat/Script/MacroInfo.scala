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

package gorsat.Script

import gorsat.Commands.CommandArguments
import gorsat.Commands.CommandParseUtilities.validateCommandArguments
import gorsat.Utilities.MacroUtilities
import org.gorpipe.exceptions.{GorParsingException, GorSystemException}
import org.gorpipe.gor.session.GorContext

/**
  * Base class for script macros. Each macro supports a CommandArguments class describing valid inputs and options.
  *
  * @param name
  * @param commandArguments
  */
abstract case class MacroInfo(name: String,
                     commandArguments: CommandArguments
                      ) {

  def init(createKey: String,
           create: ExecutionBlock,
           context: GorContext,
           doHeader: Boolean,
           args: Array[String],
           skipCache: Boolean): MacroParsingResult = {
    // Test if gor pipe session is valid, if not throw exception
    if (context == null) {
      throw new GorSystemException("Gor context cannot be null", null)
    }
    if (context.getSession == null) {
      throw new GorSystemException("Gor Pipe Session cannot be null", null)
    }

    val (arguments, illegalArguments) = validateCommandArguments(args, commandArguments)

    if (!commandArguments.ignoreIllegalArguments && illegalArguments.length > 0) {
      // We have invalid arguments and need to throw exception
      throw new GorParsingException(s"Invalid arguments in $name. Argument(s) ${illegalArguments.mkString(",")} not a part of this command")
    }

    if (commandArguments.minimumNumberOfArguments != -1 && arguments.length < commandArguments.minimumNumberOfArguments) {
      throw new GorParsingException(s"$name: Minimum number of arguments not reached. Requires at least ${commandArguments.minimumNumberOfArguments} argument(s).")
    }

    if (commandArguments.maximumNumberOfArguments != -1 && arguments.length > commandArguments.maximumNumberOfArguments) {
      throw new GorParsingException(s"$name: Maximum number of arguments reached. No more than ${commandArguments.maximumNumberOfArguments} argument(s) allowed.")
    }

    processArguments(createKey, create, context, doHeader, arguments, args, skipCache)
  }

  protected def processArguments(createKey: String,
                                 create: ExecutionBlock,
                                 context: GorContext,
                                 doHeader: Boolean,
                                 inputArguments: Array[String],
                                 options: Array[String],
                                 skipCache: Boolean): MacroParsingResult = {
    throw new NotImplementedError()
  }

  def preProcessCommand(commands: Array[String], context: GorContext): Array[String] = {
    throw new NotImplementedError()
  }

  def standardGorPreProcessing(commands: Array[String], context: GorContext, createName: String): Array[String] = {
    standardPreProcessing(commands, "GOR", context, createName)
  }

  def standardNorPreProcessing(commands: Array[String], context: GorContext, createName: String): Array[String] = {
    standardPreProcessing(commands, "NOR", context, createName)
  }

  private def standardPreProcessing(commands: Array[String], summaryCommand: String, context: GorContext, createName: String): Array[String] = {
    var newCommands: List[String] = Nil
    commands.foreach { command =>
      val (a, _) = ScriptParsers.createParser(command)

      if (a.isEmpty) {
        val commandPosition = command.toUpperCase.indexOf(this.name)
        val theCommand = if (MacroUtilities.isLastCommandWrite(command)) {
          s"$summaryCommand [$createName] | top 0"
        } else {
          s"$summaryCommand [$createName]"
        }
        val extraCreate = s"create $createName = ${this.name.toLowerCase} ${command.slice(commandPosition + this.name.length + 1, command.length)}"

        newCommands ::= theCommand
        newCommands ::= extraCreate
      } else {
        newCommands ::= command
      }
    }
    newCommands.toArray
  }
}
