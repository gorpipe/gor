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

import gorsat.Commands.CommandParseUtilities
import gorsat.process.GorPipeMacros

object ScriptParsers {

  /**
    * Regex parser for create statements. Returns create name and query.
    *
    * @param createStatement  Create statements to be parsed
    * @return Returns create name and associated query
    */
  def createParser(createStatement: String): (String, String) = {
    val comReg = """[ ]*(?i)\Qcreate\E ([^=]+)[ ]*=[ ]*(.*)""".r
    try {
      val comReg(a1, comm) = createStatement
      (a1, comm)
    } catch {
      case _: Exception => ("", "")
    }
  }

  /**
    * Regex parser for def statements. Returns def name and payload.
    *
    * @param defStatement  Def statements to be parsed
    * @return Returns def name and associated payload
    */
  def definitionParser(defStatement: String): (String, String) = {
    val comReg = """[ ]*(?i)\Qdef\E ([^=]+)[ ]*=[ ]*(.*)""".r
    try {
      val comReg(al, comm) = defStatement
      (al, comm)
    } catch {
      case _: Exception => ("", "")
    }
  }

  /**
    * Regex parser for split options.
    *
    * @param splitOption  Payload from -split option
    * @return  Returns the split options
    */
  def splitOptionParser(splitOption: String): (String, String, String, Boolean) = { // Returns the Split option (for case-sensitive replace), split size and split overlap.
    val comReg = """.*\-((?i)\Qspli\E[\S]+[ ]+)([0-9:]*).*""".r
    try {
      val comReg(cmd, al) = splitOption
      if (al.contains(":")) {
        val x = al.split(':')
        (cmd, x(0), x(1), false)
      } else if( al.isEmpty ) {
        var a = CommandParseUtilities.quoteSafeSplit(splitOption, ' ')
        var i = 0
        while( !a(i).contains("-split") ) i += 1
        if(  a(i).startsWith("<(-split") ) {
          a = CommandParseUtilities.quoteSafeSplit(a(i).substring(2,a(i).length-1), ' ')
          i = 0
        }
        val zbased = a(i).equals("-splitzero")
        (cmd, a(i+1), "", zbased)
      } else {
        (cmd, al, "", false)
      }
    } catch {
      case _: Exception =>
        ("", "", "", false)
    }
  }

  /**
    * Determines if the input query is a script. If the inputnhas more than 1 line it is a script, also if the input query
    * starts with a known macro it is a script.
    *
    * @param commands Array of all script lines
    * @return True if input is gor script
    */
  def isScript(commands: Array[String]): Boolean = {
    if (commands.length == 1) {
      // Test if we see the first command in the macro list
      val commandInUpperCase = commands.head.trim.toUpperCase

      GorPipeMacros.macrosMap.keys.foreach{x =>
        if (commandInUpperCase.startsWith(x)) return true
      }
    }

    commands.length > 1
  }
}
