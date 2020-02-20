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

package gorsat

import java.nio.file.{Files, Paths}

import collection.JavaConverters._
import gorsat.Commands.CommandParseUtilities
import org.gorpipe.gor.GorSession

/**
  * Created by sigmar on 16/02/2017.
  */
object ReportUtilities {
  /**
    * Parses the input yml command with freemarker parsing library. Command can include oher pipe steps as this method
    * only looks at the first intput source and processes the input command if the input source contains a reference to
    * yml file.
    *
    * @param cmd      Input command
    * @param session  Session used to read yml file
    * @return Fully constructed gor query based on the input command
    */
  def parseYaml(cmd: String, session: GorSession): String = {
    val cacheDir = session.getProjectContext.getCacheDir
    val splitCommands = CommandParseUtilities.quoteSafeSplitNoValidation(cmd, ' ')
    val urlParameter = cmd.contains("yml?")
    val parameter = parseParameter(cmd)
    val parameterMap = getParameterMap(parameter, urlParameter)
    val queryList = getQueryList(splitCommands, parameterMap, session, cacheDir)
    val returnQuery = getReturnQuery(queryList)
    returnQuery
  }

  private def parseParameter(cmd: String): String = {
    var parameter = ""
    var index = cmd.indexOf("yml(")
    if (index != -1) {
      parameter = cmd.substring(index + 4, cmd.lastIndexOf(')'))
    } else {
      index = cmd.indexOf("yml?")
      if (index != -1) {
        parameter = cmd.substring(index + 4)
      } else {
        index = cmd.indexOf("yml:")
        if (index != -1) {
          val firstIndex = cmd.indexOf('(', index + 4) + 1
          val lastIndex = cmd.lastIndexOf(')')
          parameter = cmd.substring(firstIndex, lastIndex)
        }
      }
    }
    parameter
  }

  private def getParameterMap(parameter: String, urlParameter: Boolean): Map[String, String] = {
    if (parameter.length > 0) {
      val parameterSplit = if (urlParameter) {
        CommandParseUtilities.quoteSquareBracketsSafeSplit(parameter, '&')
      } else {
        CommandParseUtilities.quoteSquareBracketsSafeSplit(parameter, ',')
      }
      parameterSplit.map(p => p.trim.split("=")).map(p => p(0).trim -> {
        if (p.length > 1) {
          val pp = p(1).trim
          if (pp.startsWith("'") & pp.endsWith("'")) pp.substring(1, pp.length - 1) else pp
        } else null
      }).toMap
    } else Map.empty[String, String]
  }

  private def getQuery(parameterMap: Map[String, String], commandSplitItem: String, session: GorSession, cacheDir: String): String = {
    if (commandSplitItem.contains(".yml")) {
      var index = commandSplitItem.indexOf(':')
      val indexOfParentheses = commandSplitItem.indexOf('(')
      var report: String = null
      var parameterMapJava = parameterMap.asJava
      if (index == -1 || index > indexOfParentheses) {
        if (indexOfParentheses == -1) {
          index = commandSplitItem.indexOf('?')
          if (index == -1) {
            index = commandSplitItem.length
          } else {
            val hrep = parameterMap.find(p => p._2 == null)
            if (hrep.isDefined) report = hrep.get._1
            val value = parameterMap.get("query")
            parameterMapJava = new java.util.HashMap[String, String](parameterMapJava)
            parameterMapJava.remove(report)
            if (value.isDefined) parameterMapJava.put(value.get, null)
          }
        } else index = indexOfParentheses
      } else {
        var k = commandSplitItem.indexOf('(')
        if (k == -1) k = commandSplitItem.length
        report = commandSplitItem.substring(index + 2, k)
      }

      val optfunc = FreemarkerQueryUtilities.requestQuery(commandSplitItem.substring(0, index),
        session.getProjectContext.getFileReader,
        session.getProjectContext.getQueryEvaluator,
        report,
        parameterMapJava,
        cacheDir)

      optfunc.get()
    } else {
      val paths = Paths.get(commandSplitItem)
      val bytes = Files.readAllBytes(paths)
      var query = new String(bytes).trim
      parameterMap.foreach(p => {
        query = query.replace("${" + p._1 + "}", p._2)
      })
      query
    }
  }

  private def getQueryList(commandSplit: Array[String], parameterMap: Map[String, String], session: GorSession, cacheDir: String): IndexedSeq[String] = {
    val queryList = commandSplit.map(commandSplitItem => {
      mapQuery(commandSplitItem, parameterMap, session, cacheDir)
    }
    ).map(p => p.toString.trim)
    queryList
  }

  private def mapQuery(p: String, parameterMap: Map[String, String], session: GorSession, cacheDir: String): String = {
    if (p.endsWith(".gorq") || p.contains(".yml")) {
      getQuery(parameterMap, p, session, cacheDir)
    } else
      p
  }

  private def getReturnQuery(queryList: IndexedSeq[String]): String = {
    val returnQuery = queryList.mkString(" ")
    val returnQueryUpperCase = returnQuery.toUpperCase
    if (returnQueryUpperCase.startsWith("GOR ") || returnQueryUpperCase.startsWith("NOR ")) returnQuery.slice(4, returnQuery.length) else returnQuery
  }
}
