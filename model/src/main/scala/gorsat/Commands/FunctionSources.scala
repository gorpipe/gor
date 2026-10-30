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

import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.session.GorSession
import org.gorpipe.gor.util.DataUtil

import scala.collection.concurrent.TrieMap

/**
  * A function source is an input source written as a function call, name(arg1, arg2, ...), which resolves to a
  * query. It is read as a nested query, i.e. it is accepted wherever <(query) is accepted.
  */
trait FunctionSource {
  def name: String

  /**
    * @param args    the arguments of the function call, trimmed
    * @param session session used to resolve the query
    * @return the query, including its leading input source (e.g. gor or nor), the function source reads from
    */
  def query(args: Array[String], session: GorSession): String

  /**
    * @param args    the arguments of the function call, trimmed
    * @param session session used to resolve the pipe steps
    * @return the pipe steps, e.g. for the PIPESTEPS command, the function source resolves to
    */
  def pipeSteps(args: Array[String], session: GorSession): String = {
    throw new GorParsingException(s"$name can not be used as pipe steps")
  }
}

object FunctionSources {
  private val sources = TrieMap.empty[String, FunctionSource]

  register(ReportFunctionSource)

  def register(source: FunctionSource): Unit = {
    sources.put(source.name.toUpperCase, source)
  }

  def isFunctionSource(input: String): Boolean = {
    find(input.trim).isDefined
  }

  /**
    * Resolves a function source, e.g. report(file.yml, top=10), to the query it reads from.
    */
  def parse(input: String, session: GorSession): String = {
    val (source, args) = resolve(input)
    source.query(args, session)
  }

  /**
    * Resolves a function source, e.g. report(file.yml, mystep), to the pipe steps it reads.
    */
  def parsePipeSteps(input: String, session: GorSession): String = {
    val (source, args) = resolve(input)
    source.pipeSteps(args, session)
  }

  private def resolve(input: String): (FunctionSource, Array[String]) = {
    val trimmed = input.trim
    val source = find(trimmed).getOrElse(throw new GorParsingException(s"Not a function source: $trimmed"))
    val argString = trimmed.substring(source.name.length + 1, trimmed.length - 1)
    (source, CommandParseUtilities.quoteSquareBracketsSafeSplit(argString, ',').map(_.trim).filter(_.nonEmpty))
  }

  private def find(trimmed: String): Option[FunctionSource] = {
    val open = trimmed.indexOf('(')
    if (open <= 0 || !trimmed.endsWith(")")) {
      None
    } else {
      sources.get(trimmed.substring(0, open).toUpperCase)
        .filter(_ => CommandParseUtilities.findEndBracketPos(open + 1, trimmed) == trimmed.length - 1)
    }
  }
}

/**
  * report(file.yml[::ReportName] [, key=value | entry]...) renders a yml report builder, a local file or a url such as
  * template://org/category/name/version.yml. Arguments are the same as for file.yml(...): key=value pairs set report
  * arguments and an argument without a value selects the query entry to render. It can also be used as the argument of
  * PIPESTEPS.
  */
object ReportFunctionSource extends FunctionSource {
  override val name: String = "report"

  private val REPORT_NAME_SEPARATOR = "::"

  override def query(args: Array[String], session: GorSession): String = {
    if (args.isEmpty) {
      throw new GorParsingException("report requires a yml report builder file or url as its first argument, " +
        "e.g. report(file.yml, top=10)")
    }

    val (resource, reportName) = splitReportName(args.head)
    if (!DataUtil.isYml(resource)) {
      throw new GorParsingException(s"report requires a yml report builder file or url, got: $resource")
    }

    val parameters = new java.util.LinkedHashMap[String, String]()
    args.tail.foreach { arg =>
      val eq = arg.indexOf('=')
      if (eq == -1) parameters.put(arg, null)
      else parameters.put(arg.substring(0, eq).trim, unquote(arg.substring(eq + 1).trim))
    }

    session.getSystemContext.getReportBuilder.renderQuery(resource, reportName, parameters)
  }

  // Pipe steps are rendered as a query, with the leading input source removed as for file.yml(...)
  override def pipeSteps(args: Array[String], session: GorSession): String = {
    val steps = query(args, session)
    val upper = steps.toUpperCase
    if (upper.startsWith("GOR ") || upper.startsWith("NOR ")) steps.substring(4).trim else steps
  }

  private def splitReportName(arg: String): (String, String) = {
    val i = arg.toLowerCase.indexOf(".yml" + REPORT_NAME_SEPARATOR)
    if (i == -1) (arg, null)
    else (arg.substring(0, i + 4), arg.substring(i + 4 + REPORT_NAME_SEPARATOR.length).trim)
  }

  private def unquote(value: String): String = {
    if (value.length > 1 && value.startsWith("'") && value.endsWith("'")) value.substring(1, value.length - 1) else value
  }
}
