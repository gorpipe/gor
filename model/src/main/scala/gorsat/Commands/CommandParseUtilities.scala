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

import gorsat.Commands.GenomicRange.Range
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.model.GorCommand

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.language.postfixOps
import scala.util.control.Exception.allCatch

object CommandParseUtilities {

  def validateCommandArguments(arguments: Array[String], commandArguments: CommandArguments): (Array[String], Array[String]) = {
    validateInputArguments(arguments, commandArguments.options, commandArguments.valueOptions,
      commandArguments.minimumNumberOfArguments, commandArguments.maximumNumberOfArguments,
      commandArguments.ignoreIllegalArguments)
  }

  def isNegativeNumber(option: String): Boolean = {
    try {
      val value = option.toDouble
      value < 0.0
    } catch {
      case _: Throwable => false
    }
  }

  def validateInputArguments(arguments: Array[String],
                             options: String,
                             valueOptions: String,
                             minimumNumberOfArguments: Int = -1,
                             maximumNumberOfArguments: Int = -1,
                             ignoreIllegalArguments: Boolean = false): (Array[String], Array[String]) = {
    case class Option(option: String, var optionType: Int)

    val validArguments = arguments.filter(x => x.length > 0)
    val optionsArray = options.split(' ')
    val valueOptionsArray = valueOptions.split(' ')
    var optionsList = new ListBuffer[Option]

    validArguments.foreach {
      x =>
        val option = x
        if (optionsArray.exists(x => x.equals(option))) {
          optionsList += Option(x, 0)
        } else if (valueOptionsArray.exists(x => x.equals(option))) {
          optionsList += Option(x, 1)
        } else if (!ignoreIllegalArguments && option.startsWith("-") && !isNegativeNumber(option)) {
          optionsList += Option(x, 3)
        } else {
          optionsList += Option(x, 2)
        }
    }

    var outputArguments = new ListBuffer[String]
    var illegalArguments = new ListBuffer[String]
    var lastWasValueOption = false
    var lastOption = ""

    optionsList.foreach {
      x =>
        if (lastWasValueOption && x.optionType < 2) {
          illegalArguments += lastOption
          illegalArguments += x.option
          lastWasValueOption = false
        } else if (x.optionType == 1) {
          lastWasValueOption = true
        } else if (x.optionType == 2) {
          if (!lastWasValueOption) {
            outputArguments += x.option
          }
          lastWasValueOption = false
        } else if (x.optionType == 3) {
          illegalArguments += x.option
          lastWasValueOption = false
        }

        lastOption = x.option
    }

    if (lastWasValueOption) {
      illegalArguments += lastOption
    }

    if (minimumNumberOfArguments != -1 && outputArguments.length < minimumNumberOfArguments) {
      illegalArguments ++= outputArguments
    }

    if (maximumNumberOfArguments != -1 && outputArguments.length > maximumNumberOfArguments) {
      illegalArguments ++= outputArguments.slice(maximumNumberOfArguments, outputArguments.length)
    }

    (outputArguments.toArray, illegalArguments.toArray)
  }

  def indexOfOption(args: Array[String], name: String, from: Int = 0): Int = {
    if (name == null || name == "") {
      -1
    } else {
      args.indexWhere(x => x == name, from)
    }
  }

  def hasOption(args: Array[String], name: String): Boolean = {
    indexOfOption(args, name) != -1
  }

  def stringValueOfOption(args: Array[String], name: String): String = {
    val index = indexOfOption(args, name)
    if (index == -1) {
      throw new GorParsingException(s"Value option $name is not found", name, "")
    }
    if (args(index).length > name.length) return args(index).substring(name.length)
    if (args.length <= index + 1) {
      throw new GorParsingException(s"Value not found for option $name", name, "")
    }
    if (args(index + 1).startsWith("-") && !isNegativeNumber(args(index + 1))) {
      throw new GorParsingException(s"Value missing for option $name", name, args(index + 1))
    }
    args(index + 1)
  }

  def charValueOfOption(args: Array[String], name: String): Char = {
    val idx = indexOfOption(args, name) + 1
    if (idx == 0) {
      throw new GorParsingException(s"Value option $name is not found", name, "")
    } else if (idx >= args.length) {
      throw new GorParsingException(s"Value not found for option $name", name, "")
    } else {
      val s = args(idx)
      val len = args(idx).length
      if (len == 1) {
        s.head
      } else if (len == 3 && ((s(0) == '\'' && s(len - 1) == '\'') || (s(0) == '\"' || s(len - 1) == '\"'))) {
        s(1)
      } else {
        throw new GorParsingException(s"Character option $name only takes one character as argument.\n" +
          s"Current argument ${args(idx)} is invalid.", name, "")
      }
    }
  }

  def stringArrayOfOption(args: Array[String], name: String): Array[String] = {
    var index = indexOfOption(args, name)

    if (index == -1) {
      throw new GorParsingException(s"Value option $name is not found", name, "")
    }

    var results = new ListBuffer[String]()

    while (index != -1) {

      if (args(index).length > name.length) {
        results += args(index).substring(name.length)
      } else {
        if (args.length <= index + 1) {
          throw new GorParsingException(s"Value not found for option $name", name, "")
        }
        if (args(index + 1).startsWith("-") && !isNegativeNumber(args(index + 1))) {
          throw new GorParsingException(s"Value missing for option $name", name, args(index + 1))
        }

        results += args(index + 1)
      }

      index = indexOfOption(args, name, index + 1)
    }

    results.toArray
  }

  def stringValueOfOptionWithDefault(args: Array[String], name: String, defaultValue: String): String = {
    if (hasOption(args, name)) {
      stringValueOfOption(args, name)
    } else {
      defaultValue
    }
  }

  def intValueOfOption(args: Array[String], name: String): Int = {
    val optionValue: String = stringValueOfOption(args, name)
    var value: Int = -1
    try {
      value = optionValue.toInt
    } catch {
      case _: Throwable => throw new GorParsingException(s"Value $optionValue supplied with option $name is not a valid number", name, optionValue)
    }

    value
  }

  def intValueOfOptionWithDefault(args: Array[String], name: String, defaultValue: Int = 0): Int = {
    if (hasOption(args, name)) {
      val optionValue: String = stringValueOfOption(args, name)
      var value: Int = -1
      try {
        value = optionValue.toInt
      } catch {
        case _: Throwable => throw new GorParsingException(s"Value $optionValue supplied with option $name is not a valid number", name, optionValue)
      }
      value
    } else {
      defaultValue
    }
  }

  def longValueOfOption(args: Array[String], name: String): Long = {
    val optionValue: String = stringValueOfOption(args, name)
    var value: Long = -1
    try {
      value = optionValue.toInt
    } catch {
      case _: Throwable => throw new GorParsingException(s"Value $optionValue supplied with option $name is not a valid number", name, optionValue)
    }

    value
  }

  def longValueOfOptionWithDefault(args: Array[String], name: String, defaultValue: Long = 0): Long = {
    if (hasOption(args, name)) {
      val optionValue: String = stringValueOfOption(args, name)
      var value: Long = -1
      try {
        value = optionValue.toLong
      } catch {
        case _: Throwable => throw new GorParsingException(s"Value $optionValue supplied with option $name is not a valid number", name, optionValue)
      }
      value
    } else {
      defaultValue
    }
  }

  def doubleValueOfOption(args: Array[String], name: String): Double = {
    val optionValue: String = stringValueOfOption(args, name)
    var value: Double = -1
    try {
      value = optionValue.toDouble
    } catch {
      case _: Throwable => throw new GorParsingException(s"Value $optionValue supplied with option $name is not a valid number", name, optionValue)
    }

    value
  }

  def doubleValueOfOptionWithDefault(args: Array[String], name: String, defaultValue: Double = 0.0): Double = {
    if (hasOption(args, name)) {
      val optionValue: String = stringValueOfOption(args, name)
      var value: Double = -1
      try {
        value = optionValue.toDouble
      } catch {
        case _: Throwable => throw new GorParsingException(s"Value $optionValue supplied with option $name is not a valid number", name, optionValue)
      }
      value
    } else {
      defaultValue
    }
  }

  def stringValueOfOptionWithErrorCheck(args: Array[String], name: String, validOptionValues: Array[String]): String = {
    val value = stringValueOfOption(args, name)

    if (validOptionValues != null && !validOptionValues.map(x => x.toUpperCase()).contains(value.toUpperCase())) {
      val options = validOptionValues.mkString(",")
      throw new GorParsingException(s"Value option $name does not contain any of the following values: $options", name, value)
    }

    value
  }

  def stringValueOfOptionWithDefaultWithErrorCheck(args: Array[String], name: String, defaultValue: String, validOptionValues: Array[String]): String = {
    val value = stringValueOfOptionWithDefault(args, name, defaultValue)

    if (validOptionValues != null && !validOptionValues.map(x => x.toUpperCase()).contains(value.toUpperCase())) {
      val options = validOptionValues.mkString(",")
      throw new GorParsingException(s"Value option $name does not contain any of the following values: $options", name, value)
    }

    value
  }

  def testIntValueRange(name: String, value: Int, minimumValue: Int = Int.MinValue, maximumValue: Int = Int.MaxValue): Unit = {
    if (value < minimumValue) {
      throw new GorParsingException(s"Value $value for option $name cannot be lower than $minimumValue", name, value.toString)
    }

    if (value > maximumValue) {
      throw new GorParsingException(s"Value $value for option $name cannot be greater than $maximumValue", name, value.toString)
    }
  }

  private def testDoubleValueRange(name: String, value: Double, minimumValue: Double = Double.MinValue, maximumValue: Double = Double.MaxValue): Unit = {
    if (value < minimumValue) {
      throw new GorParsingException(s"Value $value for option $name cannot be lower than $minimumValue", name, value.toString)
    }

    if (value > maximumValue) {
      throw new GorParsingException(s"Value $value for option $name cannot be greater than $maximumValue", name, value.toString)
    }
  }

  def intValueOfOptionWithRangeCheck(args: Array[String], name: String, minimumValue: Int = Int.MinValue, maximumValue: Int = Int.MaxValue): Int = {
    val value = intValueOfOption(args, name)
    testIntValueRange(name, value, minimumValue, maximumValue)
    value
  }

  def doubleValueOfOptionWithRangeCheck(args: Array[String], name: String, minimumValue: Double = Double.MinValue, maximumValue: Double = Double.MaxValue): Double = {
    val value = doubleValueOfOption(args, name)
    testDoubleValueRange(name, value, minimumValue, maximumValue)
    value
  }

  def intValueOfOptionWithDefaultWithRangeCheck(args: Array[String], name: String, defaultValue: Int, minimumValue: Int = Int.MinValue, maximumValue: Int = Int.MaxValue): Int = {
    val value = intValueOfOptionWithDefault(args, name, defaultValue)
    testIntValueRange(name, value, minimumValue, maximumValue)
    value
  }

  def doubleValueOfOptionWithDefaultWithRangeCheck(args: Array[String], name: String, defaultValue: Double, minimumValue: Double = Double.MinValue, maximumValue: Double = Double.MaxValue): Double = {
    val value = doubleValueOfOptionWithDefault(args, name, defaultValue)
    testDoubleValueRange(name, value, minimumValue, maximumValue)
    value
  }

  def validateColumn(columnName: String): Unit = {
    if (isDoubleNumber(columnName.trim())) {
      throw new GorParsingException("Column name cannot be a number: " + columnName)
    }
  }

  private def isDoubleNumber(s: String): Boolean = (allCatch opt s.toDouble).isDefined

  //In this method we assume that everything is uppercase.
  def columnNumber(columnNumberOrName: String, columnsMap: Map[String, Int], columnsArray: Array[String], forNor: Boolean, ignoreNonExisting: Boolean = false): Int = {
    var col = -1
    var name = ""
    var offset = 0
    try {
      col = columnNumberOrName.toInt

      if (col < 0) {
        throw new GorParsingException(s"Column $columnNumberOrName results in a negative index")
      }

      if (forNor) col += 2
    } catch {
      case _: Exception =>

        if (columnNumberOrName.indexOf("[") > 0 && columnNumberOrName.indexOf("]") > columnNumberOrName.indexOf("[")) {
          try {
            val (b, e) = (columnNumberOrName.indexOf("["), columnNumberOrName.indexOf("]"))
            name = columnNumberOrName.substring(0, b).toUpperCase
            offset = columnNumberOrName.substring(b + 1, e).toInt
          } catch {
            case _: Exception => throw new GorParsingException(s"Column $columnNumberOrName has illegal offset.")
          }
        } else {
          name = columnNumberOrName.toUpperCase
        }

        var n = -1
        val tmp = columnsMap.getOrElse(name, -1)
        if (tmp != -1) n = tmp + offset

        if (n == -1 && !ignoreNonExisting) {
          val s = columnsArray.zipWithIndex.map(x => (x._2 + 1) + ": " + x._1).mkString("\n")
          throw new GorParsingException(s"Column $columnNumberOrName is not in the following header:" + columnsArray.mkString("\t") + "\n\n" + s)
        } else {
          col = n + 1
        }
    }
    if (!ignoreNonExisting && (col < 1 || col > columnsArray.length)) {
      val colNum = col + (if (forNor) -2 else 0)
      val colLen = columnsArray.length
      val columnsString = columnsArray.mkString("\t")
      throw new GorParsingException(s"Column number $colNum is not in the following header with $colLen columns: $columnsString")
    }
    col
  }

  def columnsFromHeader(columns: String, header: String = "", forNor: Boolean = false, ignoreNonExisting: Boolean = false): List[Int] = {
    var a = columns.replace("#", "").replace("""$""", "")
    val headerArray = header.split("\t", -1).map(_.toUpperCase)
    val headerIndexMap = headerArray.zipWithIndex.toMap
    if (a.endsWith("-")) a += (if (forNor) headerArray.length - 2 else headerArray.length)
    a = a.replace("-,", "-" + (if (forNor) headerArray.length - 2 else headerArray.length) + ",")
    val cols = a.split("[,;]")
    var useCols: List[Int] = Nil
    var ss: Array[String] = null
    cols.foreach(x => {
      if (x == "") throw new GorParsingException(s"Illegal empty field in the column list: $columns", "", columns)
      var sta = 0
      var sto = 0

      if (x.contains('*')) {
        if (x.last == '*' && x.indexOf('*') == x.length - 1) {
          val tmp = x.substring(0, x.length - 1).toUpperCase
          var j = 0
          while (j < headerArray.length) {
            if (headerArray(j).startsWith(tmp)) useCols ::= j
            j += 1
          }
        } else {
          throw new GorParsingException(s"Wildcard is only allowed in the end of the column name, e.g. X* and not as $x", "", columns)
        }
      } else {
        ss = quoteSafeSplit(x.replace("[", "'[").replace("]", "]'"), '-').map(x => x.replace("'", "").toUpperCase)

        try {
          if (ss.length == 1) {
            sta = columnNumber(ss(0), headerIndexMap, headerArray, forNor, ignoreNonExisting)
            sto = sta
          }

          if (ss.length == 2) {
            sta = columnNumber(ss(0), headerIndexMap, headerArray, forNor, ignoreNonExisting)
            sto = sta

            if (ss(1).length > 0) {
              sto = columnNumber(ss(1), headerIndexMap, headerArray, forNor, ignoreNonExisting)
            }
          }

          var j = math.max(0, sta - 1)
          while (j < sto) {
            useCols ::= j
            j += 1
          }
        } catch {
          case gpe: GorParsingException =>
            gpe.setPayload(columns)
            throw gpe
        }
      }
    })

    useCols.reverse
  }

  def columnsFromHeaderWithValidation(columns: String, header: String = "", forNor: Boolean = false,
                                      minimumNumberOfColumns: Int = -1, maximumNumberOfColumns: Int = -1,
                                      ignoreNonExisting: Boolean = false): List[Int] = {
    var columnIndices: List[Int] = Nil

    try {
      columnIndices = columnsFromHeader(columns, header, forNor, ignoreNonExisting)
    } catch {
      case _: GorParsingException =>

    }

    if (minimumNumberOfColumns != -1 && columnIndices.length < minimumNumberOfColumns) {
      throw new GorParsingException(s"Column(s) $columns require at least $minimumNumberOfColumns column(s)", "", columns)
    }

    if (maximumNumberOfColumns != -1 && columnIndices.length > maximumNumberOfColumns) {
      throw new GorParsingException(s"Column(s) $columns cannot have more than $maximumNumberOfColumns column(s)", "", columns)
    }

    columnIndices
  }

  def columnFromHeader(column: String, header: String, forNor: Boolean): Int = {
    columnsFromHeaderWithValidation(column, header, forNor, 1, 1).head
  }

  def columnsOfOption(args: Array[String], name: String, header: String, forNor: Boolean = false, ignoreNonExisting: Boolean = false): List[Int] = {
    val argument = stringValueOfOption(args, name)
    try {
      val columnIndices = columnsFromHeader(argument, header, forNor, ignoreNonExisting)
      columnIndices
    } catch {
      case gpe: GorParsingException =>
        gpe.setOption(name)
        gpe.setPayload(argument)
        throw gpe
    }
  }

  def columnsOfOptionWithNil(args: Array[String], name: String, header: String, forNor: Boolean = false, ignoreNonExisting: Boolean = false): List[Int] = {
    if (!hasOption(args, name)) {
      return Nil
    }

    columnsOfOption(args, name, header, forNor, ignoreNonExisting)
  }

  def columnsOfOptionWithValidation(args: Array[String], name: String, header: String, forNor: Boolean = false,
                                    minimumNumberOfColumns: Int = -1, maximumNumberOfColumns: Int = -1,
                                    ignoreNonExisting: Boolean = false): List[Int] = {
    val columnIndices = columnsOfOption(args, name, header, forNor, ignoreNonExisting)

    if (minimumNumberOfColumns != -1 && columnIndices.length < minimumNumberOfColumns) {
      throw new GorParsingException(s"Option $name requires at least $minimumNumberOfColumns column(s)", name, "")
    }

    if (maximumNumberOfColumns != -1 && columnIndices.length > maximumNumberOfColumns) {
      throw new GorParsingException(s"Option $name cannot have more than $maximumNumberOfColumns column(s)", name, "")
    }

    columnIndices
  }

  def columnOfOption(args: Array[String], name: String, header: String, forNor: Boolean = false): Int = {
    columnsOfOptionWithValidation(args, name, header, forNor, 1, 1).head
  }

  def parseIntWithRangeCheck(name: String, value: String, minimumValue: Int = Int.MinValue, maximumValue: Int = Int.MaxValue): Int = {

    var intValue: Int = 0

    try {
      intValue = if (value.isEmpty) 0 else value.toInt
    } catch {
      case _: Throwable => throw new GorParsingException(s"Failed to convert variable $name of value $value to number.")
    }

    testIntValueRange(name, intValue, minimumValue, maximumValue)

    intValue
  }

  def rangeOfOption(args: Array[String], name: String): Range = {
    val range = stringValueOfOption(args, name).replace("\"", "").replace(",", "").replace(" ", "")

    try {
      parseRange(range)
    } catch {
      case gpe: GorParsingException =>
        gpe.setOption(name)
        gpe.setPayload(range)
        throw gpe
    }
  }

  def parseRange(range: String): Range = {
    val rcol = range.split("[:|-]")

    if (rcol.length > 3) {
      throw new GorParsingException(s"Invalid range value '$range'. One of the following is supported: 'chrom', " +
        s"'chrom:start-' or 'chrom:start-stop'")
    }
    var startChr = ""
    var startPos = 0
    var stopPos = Integer.MAX_VALUE
    if (rcol.nonEmpty) startChr = rcol(0)

    if (rcol.length > 1) {
      val sanitized = rcol(1).toLowerCase().replace("m", "000000").replace(",", "")
      startPos = parseIntWithRangeCheck("start", sanitized)
      if (!range.endsWith("-")) {
        stopPos = startPos
      }
    }

    if (rcol.length > 2) {
      val sanitized = rcol(2).toLowerCase().replace("m", "000000").replace(",", "")
      stopPos = parseIntWithRangeCheck("stop", sanitized, startPos)
    }

    GenomicRange.Range(startChr, startPos, stopPos)
  }

  def isNestedCommand(query: String): Boolean = {
    val trimmedQuery = query.trim()
    trimmedQuery.startsWith("<(")
  }

  def isNestedProcessCommand(query: String): Boolean = {
    val trimmedQuery = query.trim()
    trimmedQuery.startsWith(">(")
  }

  def findEndBracketPos(offset: Int, s: String): Int = { // Adapted from quoteSafeSplit
    val c = ')'
    var backSlashCount = 0
    var withinQuotes = false
    var withinBrackets = 0
    var quoteType = ' '
    var t = offset
    var i = offset
    val q1 = '"'
    val q2 = "'"(0)
    val brop = '('
    val brcl = ')'
    while (i < s.length) {
      if (s(i) == c) {
        if (!(withinQuotes || withinBrackets > 0)) {
          return t
        } else {
          t += 1
        }
        if (c == brcl && !withinQuotes) withinBrackets -= 1
        if (c == brop && !withinQuotes) withinBrackets += 1
      } else if (s(i) == q1 || s(i) == q2) {
        if (withinQuotes && (backSlashCount % 2) == 0) {
          if (s(i) == quoteType) withinQuotes = false
          t += 1
        } else if (backSlashCount % 2 == 0) {
          withinQuotes = true
          quoteType = s(i)
          t += 1
        } else {
          t += 1
        }
      } else if ((s(i) == brop || s(i) == brcl) && !withinQuotes) {
        if (s(i) == brop) withinBrackets += 1
        if (s(i) == brcl) withinBrackets -= 1
        t += 1
      } else {
        t += 1
      }
      if (s(i) == '\\') backSlashCount += 1 else backSlashCount = 0
      i += 1
    }
    -1
  }

  def parseNestedProcessCommand(command: String): String = {
    val trimmedCommand = command.trim()
    val ip = quoteSafeIndexOf(trimmedCommand, ">(")
    if (ip < 0) {
      throw new GorParsingException("Bad nested process gorpipe command: " + trimmedCommand)
    }
    val ep = findEndBracketPos(ip + 2, trimmedCommand)
    if (ep < ip + 2 || ep < trimmedCommand.length - 1) {
      throw new GorParsingException("Nested process gorpipe command not closed with a bracket: " + trimmedCommand)
    }
    trimmedCommand.slice(ip + 2, ep).trim
  }

  def parseNestedCommand(command: String): String = {
    parseNestedCommandUntrimmed(command).trim
  }

  def parseNestedCommandUntrimmed(command: String): String = {
    val trimmedCommand = command.trim()
    val ip = quoteSafeIndexOf(trimmedCommand, "<(")
    if (ip < 0) {
      throw new GorParsingException("Bad nested gorpipe command: " + trimmedCommand)
    }
    val ep = findEndBracketPos(ip + 2, trimmedCommand)
    if (ep < ip + 2 || ep < trimmedCommand.length - 1) {
      throw new GorParsingException("Nested gorpipe command not closed with a bracket: " + trimmedCommand)
    }
    trimmedCommand.slice(ip + 2, ep)
  }

  def replaceSingleQuotes(x: String): String = {
    if (x != null) {
      if (x.startsWith("'") && x.endsWith("'") && x.length > 1 || x.startsWith("\"") && x.endsWith("\"") && x.length > 1) return x.slice(1, x.length - 1)
    }

    x
  }

  def removeComments(query: String, allowQuotes: Boolean): String = {
    val command = new GorCommand(query)
    command.getWithoutComments(allowQuotes)
  }

  def removeComments(query: String): String = {
    val command = new GorCommand(query)
    command.getWithoutComments
  }


  def removeComments(commands: Array[String]): Array[String] = {
    val command = new GorCommand(commands.mkString("\r\n"))
    command.getWithoutComments.split("\r\n")
  }

  def quoteSafeReplace(str: String, a: String, b: String): String = {
    var keepOn = true
    val outStr = new mutable.StringBuilder(str.length + 1000)
    var right = str
    while (keepOn) {
      val sta = quoteSafeIndexOf(right, a)
      if (sta >= 0) {
        val left = right.slice(0, sta)
        right = right.slice(sta + a.length, right.length)
        outStr.append(left + b)
        keepOn = true
      } else {
        keepOn = false
      }
    }
    outStr.append(right)
    outStr.toString
  }

  def repeatedQuoteSafeReplace(str: String, a: String, b: String, maxlen: Int): String = {
    var ostr = str
    var keepOn = true
    var i = 0
    while (keepOn) {
      i += 1
      val nstr = quoteSafeReplace(ostr, a, b)
      if (nstr == ostr || nstr.length > maxlen) keepOn = false else ostr = nstr
    }
    ostr
  }

  def cleanCommandStrings(queries: Array[String]): Array[String] = {
    queries.map(x => cleanCommandString(x))
  }

  def cleanCommandString(query: String): String = {
    val ml = 100000
    CommandParseUtilities.repeatedQuoteSafeReplace(
      CommandParseUtilities.repeatedQuoteSafeReplace(
        CommandParseUtilities.repeatedQuoteSafeReplace(query, "  ", " ", ml), " |", "|", ml), "| ", "|", ml).trim
  }

  val DEFAULT_EXTENSION: String = System.getProperty("org.gorpipe.gor.query.default_extension", ".gorz")
  val TXT_EXTENSION: String = ".txt"
  val TSV_EXTENSION: String = ".tsv"
  val PARQUET_EXTENSION: String = ".parquet"
  val GOR_DICTIONARY_EXTENSION: String = ".gord"
  val GOR_DICTIONARY: String = "GORDICT"
  val GOR_DICTIONARY_FOLDER: String = "GORDICTFOLDER"
  val GOR_DICTIONARY_PART: String = "GORDICTPART"
  val GOR_DICTIONARY_FOLDER_PART: String = "GORDICTFOLDERPART"
  val NOR_DICTIONARY_EXTENSION: String = ".nord"
  val NOR_DICTIONARY: String = "NORDICT"
  val NOR_DICTIONARY_PART: String = "NORDICTPART"

  val INPUT_TO_EXTENSION: Map[String, String] = Map(
    ("CMD -N", TXT_EXTENSION),
    ("NORCMD", TXT_EXTENSION),
    ("NOR", TSV_EXTENSION),
    ("SDL", TSV_EXTENSION),
    ("NORSQL", TSV_EXTENSION),
    ("NORSTDIN", TSV_EXTENSION),
    ("NORROWS", TSV_EXTENSION),
    ("SPARK", PARQUET_EXTENSION),
    ("SELECT", PARQUET_EXTENSION),
    (GOR_DICTIONARY, GOR_DICTIONARY_EXTENSION),
    (GOR_DICTIONARY_FOLDER, GOR_DICTIONARY_EXTENSION),
    (GOR_DICTIONARY_PART, GOR_DICTIONARY_EXTENSION),
    (GOR_DICTIONARY_FOLDER_PART, GOR_DICTIONARY_EXTENSION),
    (NOR_DICTIONARY, NOR_DICTIONARY_EXTENSION),
    (NOR_DICTIONARY_PART, NOR_DICTIONARY_EXTENSION)
  )

  val HEADER_EXTENSIONS: Map[String, String] = Map(
    (TXT_EXTENSION, ".header.txt"),
    (TSV_EXTENSION, ".header.tsv"),
    (DEFAULT_EXTENSION, ".header.gor")
  )

  /**
    * Returns the header to use based on the input query.
    *
    * @param query    Input query
    * @param isHeader Indicates if we are retrieving the extension for a header file
    * @return Extension for the input query on the form ".xxx"
    */
  def getExtensionForQuery(query: String, isHeader: Boolean): String = {

    val trimmed = query.trim.toUpperCase()

    if (trimmed.length == 0) {
      return DEFAULT_EXTENSION
    }

    val finalStatement = CommandParseUtilities.quoteSafeSplitAndTrim(trimmed, ';').last
    val steps = CommandParseUtilities.quoteSafeSplitAndTrim(finalStatement, '|')
    val hasToGor = steps.contains("TOGOR")
    val inputSource = if (hasToGor) "GOR" else CommandParseUtilities.quoteSafeSplitAndTrim(steps.head, ' ').head

    val extension = INPUT_TO_EXTENSION.getOrElse(inputSource, DEFAULT_EXTENSION)

    if (isHeader) HEADER_EXTENSIONS.getOrElse(extension, extension) else extension
  }

  /**
    * Returns true if the query is a dictionary.
    *
    * @param commandToExecute Command to test
    * @return True if dictioanry, false otherwise
    */
  def isDictionaryQuery(commandToExecute: String): Boolean = {
    val commandUpperCase = commandToExecute.toUpperCase()
    commandUpperCase.startsWith(GOR_DICTIONARY) || commandUpperCase.startsWith(NOR_DICTIONARY)
  }

  private def getDefaultQuotes: Array[SplitQuote] = Array[SplitQuote](SplitQuote('\''), SplitQuote('"'))

  private def getDefaultBlocks: Array[SplitBlock] = Array[SplitBlock](SplitBlock('(', ')'), SplitBlock('{', '}'))

  private val getDefaultSpecialChars : Set[Char] = Set('\\', '\'', '"', '(', ')', '{', '}')

  case class SplitQuote(quote: Char) {
    var withinQuotes = false
  }

  case class SplitBlock(startBlock: Char, endBlock: Char) {
    var withinBlock = 0
  }

  /**
    * Splits inputString with the splitCharacter. The split is performed so that it is quote safe, does not split inside quotes,
    * and block save, does not split within blocks. Default quote operators are " and '. Default block operators are '(' closed with ')'
    * and '{' closed with '}'.
    *
    * @param inputString    String to be split
    * @param splitCharacter Character used to split the inputString
    * @return Split array
    */
  def quoteSafeSplit(inputString: String, splitCharacter: Char): Array[String] = {
    quoteCustomSafeSplit(inputString, splitCharacter, getDefaultQuotes, getDefaultBlocks, getDefaultSpecialChars, true)
  }

  def quoteSafeSplitNoValidation(inputString: String, splitCharacter: Char): Array[String] = {
    quoteCustomSafeSplit(inputString, splitCharacter, getDefaultQuotes, getDefaultBlocks, getDefaultSpecialChars, false)
  }

  /**
    * See quoteSafeSplit(String, Char) for more information. In addition to perform quote safe split this method trims
    * all entries in the split array.
    *
    * @param inputString    String to be split
    * @param splitCharacter Character used to split the inputString
    * @return Split array
    */
  def quoteSafeSplitAndTrim(inputString: String, splitCharacter: Char): Array[String] = {
    quoteSafeSplit(inputString, splitCharacter)
      .map(_.trim)
      .filter(x => x != "")
  }

  /**
    * See quoteSafeSplit(String, Char) for more information. This method perfoms the quote safe split where additional
    * '[' and ']' block operators are added.
    *
    * @param inputString    String to be split
    * @param splitCharacter Character used to split the inputString
    * @return Split array
    */
  def quoteSquareBracketsSafeSplit(inputString: String, splitCharacter: Char): Array[String] = {
    val blocks = getDefaultBlocks :+ SplitBlock('[', ']')
    val specialChars = getDefaultSpecialChars ++ Set('[', ']')
    quoteCustomSafeSplit(inputString, splitCharacter, getDefaultQuotes, blocks, specialChars, true)
  }

  /**
   * See quoteSafeSplit(String, Char) for more information. This method perfoms the quote safe split where additional
   * '{' and '}' block operators are added.
   *
   * @param inputString      String to be split
   * @param splitCharacter   Character used to split the inputString
   * @return                 Split array
   */
  def quoteCurlyBracketsSafeSplit(inputString: String, splitCharacter: Char): Array[String] = {
    val blocks = getDefaultBlocks :+ SplitBlock('{', '}')
    val specialChars = getDefaultSpecialChars ++ Set('{', '}')
    quoteCustomSafeSplit(inputString, splitCharacter, getDefaultQuotes, blocks, specialChars, false)
  }

  /**
    * See quoteSafeSplit(String, Char) for more information. This method perfoms the quote safe split where additional
    * '<' and '>' block operators are added.
    *
    * @param inputString      String to be split
    * @param splitCharacter   Character used to split the inputString
    * @return                 Split array
    */
  def quoteAngleBracketsSafeSplit(inputString: String, splitCharacter: Char): Array[String] = {
    val blocks = getDefaultBlocks :+ SplitBlock('<', '>')
    val specialChars = getDefaultSpecialChars ++ Set('<', '>')
    quoteCustomSafeSplit(inputString, splitCharacter, getDefaultQuotes, blocks, specialChars, false)
  }

  private def consumeQuoted(inputString: String, start: Int, end: Int): Int = {
    var currentChar = inputString(start)
    var backSlashCount = 0
    val quoteChar = currentChar
    var i = start + 1
    while (i < end && ((inputString(i) != quoteChar) || (backSlashCount % 2 != 0))) {
      currentChar = inputString.charAt(i)
      if (currentChar == '\\') backSlashCount += 1 else backSlashCount = 0
      i += 1
    }
    i
  }

  private def quoteCustomSafeSplit(inputString: String, splitCharacter: Char, quotes: Array[SplitQuote], blocks: Array[SplitBlock], specialChars: Set[Char], validateBlocks: Boolean): Array[String] = {
    if (inputString == null) {
      return null
    }

    if (inputString.length == 0) {
      return Array[String](inputString)
    }

    var backSlashCount = 0
    var words = List[String]()
    var i = 0
    var start = i

    var withinBlock = false
    val inputLength = inputString.length
    while (i < inputLength) {
      val currentChar = inputString.charAt(i)

      if (currentChar == splitCharacter) {
        if (!withinBlock) {
          words = inputString.substring(start, i) :: words
          start = i + 1
        }
        backSlashCount = 0
      } else if (specialChars.contains(currentChar)) {
        if (currentChar == '\\') {
          backSlashCount += 1
        } else {
          backSlashCount = 0
          if (quotes.exists(x => x.quote == currentChar) && (backSlashCount % 2) == 0) {
            i = consumeQuoted(inputString, i, inputLength)
          } else {
            withinBlock = false
            blocks.foreach { x =>
              if (x.startBlock == currentChar) x.withinBlock += 1
              if (x.endBlock == currentChar) x.withinBlock -= 1
              if (x.withinBlock > 0) withinBlock = true
            }
          }
        }
      }

      i += 1
    }
    if (start < inputLength) words = inputString.substring(start) :: words

    // Validate the final state
    if (validateBlocks) {
      blocks.foreach { x =>
        if (x.withinBlock != 0) {
          throw new GorParsingException(s"Non matching ${x.startBlock} or ${x.endBlock} block found in: $inputString")
        }
      }
    }

    words.reverse.toArray
  }

  /**
    * Finds the quote safe index of searchString in the inputString. Quote safe index ignores texts within single quote (')
    * and double quotes (") when looking for the search pattern. It also ignores '[]' and '{}' block operators.
    *
    * @param inputString  Input string to matsh the searchString pattern
    * @param searchString Search string pattern to locate the index of
    * @param par          ???
    * @param from         Offset into the inputString where to start looking for the searchString pattern
    * @return Zero based index into the inputString where the searchString pattern is located. Returns -1 if
    *         pattern is not found.
    */
  def quoteSafeIndexOf(inputString: CharSequence, searchString: String, par: Boolean = false, from: Int = 0): Int = {
    quoteCustomSafeIndexOf(inputString, searchString, getDefaultQuotes, getDefaultBlocks, par, from)
  }

  def quoteCustomSafeIndexOf(inputString: CharSequence, searchString: String, quotes: Array[SplitQuote], blocks: Array[SplitBlock], par: Boolean, from: Int): Int = {
    var backSlashCount = 0
    var withinQuotes = false
    var quoteType = ' '
    var i = from
    var j = 0
    var patternQuote = false
    var wq: Boolean = false
    var pq: Boolean = false
    var qt: Char = ' '
    var bc: Int = 0 // QuoteState(

    // Reset
    quotes.foreach(x => x.withinQuotes = false)
    blocks.foreach(x => x.withinBlock = 0)

    try {
      while (i < inputString.length) {
        val withinBrackets = blocks.exists(x => x.withinBlock > 0)
        val c = inputString.charAt(i)

        if (quotes.exists(x => x.quote == c)) {
          if (withinQuotes && (backSlashCount % 2) == 0) {
            if (c == quoteType) {
              withinQuotes = false
              patternQuote = false
            }
          } else if (backSlashCount % 2 == 0) {
            withinQuotes = true
            quoteType = c
            if (searchString(j) == quoteType) patternQuote = true
          }
        }

        if (withinQuotes && c == '\\') backSlashCount += 1 else backSlashCount = 0

        if (j == 0) {
          wq = withinQuotes
          pq = patternQuote
          qt = quoteType
          bc = backSlashCount
        }
        if (c == searchString(j)) {
          if ((!withinQuotes || (withinQuotes && patternQuote)) && (!par || withinBrackets)) {
            j += 1
            if (j == searchString.length) return i - j + 1
          } else {
            if (j > 1) {
              i -= (j - 1)
              withinQuotes = wq
              patternQuote = pq
              quoteType = qt
              backSlashCount = bc
            }
            j = 0
          }
        } else {
          if (j > 1) {
            i -= (j - 1)
            withinQuotes = wq
            patternQuote = pq
            quoteType = qt
            backSlashCount = bc

          }
          j = 0
        }

        if (!withinQuotes) {
          blocks.foreach { x =>
            if (x.startBlock == c) {
              x.withinBlock += 1
            } else if (x.endBlock == c) x.withinBlock -= 1
          }
        }

        i += 1
      }
    } catch {
      case e: java.lang.StringIndexOutOfBoundsException =>
        throw new GorParsingException("Error in string - unmatched quotes: " + inputString, e)
    }
    -1
  }

  /**
    * Cleans up the input query, removing all unwanted whitespaces and performing fixed formatting.
    *
    * @param query input query to be cleaned
    * @return cleaned query string
    */
  def cleanupQuery(query: String): String = {
    cleanupQuery(query, " | ", ";")
  }

  /**
    * Cleans up the input query, removing all unwanted whitespaces and performing fixed formatting.
    *
    * @param query input query to be cleaned
    * @return cleaned queries
    */
  def cleanupQueryAndSplit(query: String): Array[String] = {
    cleanupQueryAndSplit(query, " | ")
  }

  /**
    * Cleans up the input query, removing all unwanted whitespaces and performing pretty formatting.
    *
    * @param query input query to be cleaned
    * @return cleaned and prettied query string
    */
  def cleanupQueryWithFormat(query: String): String = {
    cleanupQuery(query, "\n\t| ", ";\n\n")
  }

  private def cleanupQuery(query: String, stepFormat: String, commandFormat: String): String = {
    cleanupQueryAndSplit(query, stepFormat).mkString(commandFormat)
  }

  private def cleanupQueryAndSplit(query: String, stepFormat: String): Array[String] = {
    val fixedQuery = query.replace('\n', ' ').replace('\r', ' ')
    val queries = quoteSafeSplitAndTrim(fixedQuery, ';')

    var commands = Array.empty[String]

    queries.foreach { x =>
      commands +:= quoteSafeSplitAndTrim(x, '|').mkString(stepFormat)
    }

    commands.reverse
  }

  def getFirstCommand(query: String): String = {
    val steps = quoteSafeSplitAndTrim(query, '|')

    if (steps.nonEmpty) {
      val entries = quoteSafeSplitAndTrim(steps.head, ' ')
      entries.head
    } else {
      ""
    }
  }
}
