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

package gorsat.parser

import java.util

import gorsat.Analysis.DagMapAnalysis
import gorsat.Commands.RowHeader
import gorsat.parser.FunctionTypes._
import gorsat.parser.ParseUtilities._
import org.gorpipe.exceptions.{GorParsingException, GorSystemException}
import org.gorpipe.gor.SyntaxChecker
import org.gorpipe.gor.model.{ColumnValueProvider, GenomicIterator}
import org.gorpipe.gor.session.GorContext
import org.gorpipe.model.gor.iterators.RefSeq

import scala.collection.mutable
import scala.util.parsing.combinator.JavaTokenParsers

case class ColumnInfo(name: String, dataType: String) {}

/**
  * The ParseArith class is used to compile and evaluate expressions used in CALC and WHERE commands.
  * @param rs An optional row source (not sure if this is really used)
  */
class ParseArith(rs: GenomicIterator = null) extends JavaTokenParsers with Serializable {

  @transient private val functions = CalcFunctions.registry
  @transient private val pathfunctions = CalcFunctions.pathutilregistry

  private val subFilters = new util.ArrayList[ParseArith]

  var refSeq: RefSeq = _
  var context: GorContext = new GorContext()
  var executeNor = false
  private var outputType: String = "Boolean, String, Int, Double, Long"
  @transient var stringFunction: sFun = _
  @transient var intFunction: iFun = _
  @transient var doubleFunction: dFun = _
  @transient var longFunction: lFun = _
  @transient var booleanFunction: bFun = _
  private var doubleVariableMap = Map.empty[String, Int]
  private var longVariableMap = Map.empty[String, Int]
  var stringVariableMap = Map.empty[String, Int]
  private var intVariableMap = Map.empty[String, Int]
  private var doubleCols = List("#?f","""$?f""")
  private var intCols = List("#?i","""$?i""")
  private var stringCols = List("#?s","""$?s""")

  private var columns = Map.empty[String, ColumnInfo]
  private var columnNames = Array.empty[String]

  private var orgColNames: Array[String] = _
  private var orgColTypes: Array[String] = _

  @transient private val calcCompiler = new CalcCompiler(this)
  private var calcLambda: TypedCalcLambda = _
  private var compileAntlr = false
  private var runAntlr = false
  private var compileClassic = true
  private var runClassic = true

  setMode()

  private def setMode(): Unit = {
    val mode = System.getenv("GOR_CALCMODE")

    mode match {
      case "classic" | null =>
        compileAntlr = false
        runAntlr = false
        compileClassic = true
        runClassic = true
      case "compileAntlr" =>
        compileAntlr = true
        runAntlr = false
        compileClassic = true
        runClassic = true
      case "runAntlr" =>
        compileAntlr = true
        runAntlr = true
        compileClassic = false
        runClassic = false
      }
  }

  def close(): Unit = {
    if (refSeq != null) refSeq.close()
    subFilters.forEach(f => f.close())
  }

  /**
    * Create a sub-filter owned by this parser. This is used by functions that have their own expressions,
    * such as LISTFILTER.
    * Expressions compiled by the returned instance can refer to columns known by this instance - there is
    * no need to explicitly set column information.
    * The returned instance shares the session with this instance.
    * @return A ParseArith instance
    */
  def createSubFilter(): ParseArith = {
    val f = new ParseArith()
    subFilters.add(f)
    if (context != null) f.setContext(context, executeNor)
    addSpecialVariablesToSubFilter(f)
    f
  }

  def setContext(ctx: GorContext, executeNor: Boolean): Unit = {
    this.context = ctx
    this.executeNor = executeNor
    if (refSeq == null) refSeq = context.getSession.getProjectContext.createRefSeq()
  }

  def setColumnNamesAndTypes(colNames: Array[String], colTypes: Array[String]): Unit = {
    orgColNames = colNames
    orgColTypes = colTypes
    calcCompiler.setColumnNamesAndTypes(colNames, colTypes)

    if (colNames.length != colTypes.length) {
      val msg = "Error in column names - column names and type arrays differ in size"
      throw new GorSystemException(msg, null)
    }
    val colSymbol = "#"

    columns = columns.empty
    for(i <- colNames.indices) {
      columns += colNames(i).toUpperCase -> ColumnInfo(colNames(i), colTypes(i))
    }
    columnNames = colNames

    val sSuff = "S"
    val fSuff = "F"
    val iSuff = "I"
    val lSuff = "L"

    for (ii <- Range(0, colNames.length - (if (executeNor) 2 else 0))) {
      val i = if (executeNor) ii + 2 else ii
      val cn = colNames(i).toUpperCase

      val colType = colTypes(i).toUpperCase.charAt(0)
      val byName = cn -> i
      val byColumnSymbol = colSymbol + (ii + 1) -> i
      val byDollarSymbol = "$" + (ii + 1) -> i
      if (colType == 'D' || colType == 'L' || colType == 'I') {
        doubleVariableMap += byName
        doubleVariableMap += byColumnSymbol
        doubleVariableMap += byDollarSymbol
        if (colType == 'L' || colType == 'I') {
          longVariableMap += byName
          longVariableMap += byColumnSymbol
          longVariableMap += byDollarSymbol
          if (colType == 'I') {
            intVariableMap += byName
            intVariableMap += byColumnSymbol
            intVariableMap += byDollarSymbol
          }
        }
      }
      stringVariableMap += byName
      List(colSymbol, "$").foreach(cs => {
        stringVariableMap += (cs + (ii + 1) + sSuff -> i)
        stringVariableMap += (cs + (ii + 1) -> i)
        doubleVariableMap += (cs + (ii + 1) + fSuff -> i)
        doubleVariableMap += (cs + (ii + 1) + iSuff -> i)
        doubleVariableMap += (cs + (ii + 1) + lSuff -> i)
        longVariableMap += (cs + (ii + 1) + fSuff -> i)
        longVariableMap += (cs + (ii + 1) + iSuff -> i)
        longVariableMap += (cs + (ii + 1) + lSuff -> i)
        intVariableMap += (cs + (ii + 1) + iSuff -> i)
      })
    }
    stringVariableMap += (colSymbol + "RC" -> -3)
    stringVariableMap += ("$RC" -> -3)
    stringVariableMap += (colSymbol + "rc" -> -3)
    stringVariableMap += ("$rc" -> -3)
  }

  def aDagSet(r1: String, r2: String): Set[String] = {
    if (context != null) {
      DagMapAnalysis.dagSet(r1, r2, context.getSession)
    } else {
      throw new GorSystemException("Session should not be null when processing dag set.", null)
    }
  }

  def addSpecialVariablesToSubFilter(filter: ParseArith): Unit = {
    filter.stringVariableMap = stringVariableMap
    filter.doubleVariableMap = doubleVariableMap
    filter.longVariableMap = longVariableMap
    filter.intVariableMap = intVariableMap
    List("X", "I").foreach(x => {
      filter.stringVariableMap -= x
      filter.doubleVariableMap -= x
      filter.longVariableMap -= x
      filter.intVariableMap -= x
    })
    filter.intVariableMap += ("I" -> -1)
    filter.longVariableMap += ("I" -> -1)
    filter.doubleVariableMap += ("I" -> -1)
    filter.stringVariableMap += ("I" -> -1)
    filter.stringVariableMap += ("X" -> -2)

    filter.calcCompiler.setColumnNamesAndTypes(orgColNames, orgColTypes)
    filter.calcCompiler.addSpecialVars()
  }

  def getAvgRowsPerMilliSecond: Double = {
    if(rs != null) rs.getAvgRowsPerMilliSecond else -1.0
  }

  def getAvgBasesPerMilliSecond: Double = {
    if(rs != null) rs.getAvgBasesPerMilliSecond else -1.0
  }

  def getAvgSeekTimeMilliSecond: Double = {
    if(rs != null) rs.getAvgSeekTimeMilliSecond else -1.0
  }

  def getHeader: RowHeader = {
    RowHeader(orgColNames, orgColTypes)
  }

  def StringVariableHandler(colName: String): sFun = {
    val column = stringVariableMap(colName.toUpperCase)
    cvp => {
      cvp.stringValue(column)
    }
  }

  def StringVariableName: Parser[String] {
    def apply(in: Input): ParseResult[String]
  } = new Parser[String] {
    def apply(in: Input): ParseResult[String] = name(in) match {
      case Success(x, in1) => stringVariableMap.get(x.toUpperCase) match {
        case Some(_) =>
          Success(x, in1)
        case None =>
          var suffix = ""
          val variableName = x.toUpperCase
          if(columns.contains(variableName)) {
            suffix += s" $x is a column of type ${typeCharToString(columns(variableName).dataType)}."
          } else {
            val closest = StringDistance.findClosest(variableName, 3, columnNames.toSeq)
            if(!closest.isEmpty) {
              suffix = s" Did you mean: $closest?"
            }
          }
          Failure(s"Invalid String variable name: $x.$suffix", in1)
      }
      case Failure(msg, next) =>
        Failure(msg, next)
    }
  }

  def DoubleVariableHandler(colName: String): dFun = {
    val column = doubleVariableMap(colName.toUpperCase)
    cvp => {
      cvp.doubleValue(column)
    }
  }

  def DoubleVariableName: Parser[String] {
    def apply(in: Input): ParseResult[String]
  } = new Parser[String] {
    def apply(in: Input): ParseResult[String] =
      name(in) match {
      case Success(x, in1) =>
        val variableName = x.toUpperCase
        doubleVariableMap.get(variableName) match {
        case Some(_) =>
          Success(x, in1)
        case None =>
          var suffix = ""
          if(columns.contains(variableName)) {
            suffix += s" $x is a column of type ${typeCharToString(columns(variableName).dataType)}."
          } else {
            val closest = StringDistance.findClosest(variableName, 3, columnNames.toSeq)
            if(!closest.isEmpty) {
              suffix = s" Did you mean: $closest?"
            }
          }
          Failure(s"Invalid Float/Double variable name: $x.$suffix", in1)
      }
      case Failure(msg, next) =>
        Failure(msg, next)
    }
  }

  def LongVariableHandler(colName: String): lFun = {
    val column = longVariableMap(colName.toUpperCase)
    cvp => {
      cvp.longValue(column)
    }
  }

  def LongVariableName: Parser[String] {
    def apply(in: Input): ParseResult[String]
  } = new Parser[String] {
    def apply(in: Input): ParseResult[String] = name(in) match {
      case Success(x, in1) =>
        val variableName = x.toUpperCase
        longVariableMap.get(variableName) match {
          case Some(_) =>
            Success(x, in1)
          case None =>
            var suffix = ""
            if(columns.contains(variableName)) {
              suffix += s" $x is a column of type ${typeCharToString(columns(variableName).dataType)}."
            } else {
              val closest = StringDistance.findClosest(variableName, 3, columnNames.toSeq)
              if(!closest.isEmpty) {
                suffix = s" Did you mean: $closest?"
              }
            }
            Failure(s"Invalid Float/Double variable name: $x.$suffix", in1)
        }
      case Failure(msg, next) =>
        Failure(msg, next)
    }
  }

  def IntVariableHandler(colName: String): iFun = {
    val column = intVariableMap(colName.toUpperCase)
    cvp => {
      cvp.intValue(column)
    }
  }

  def IntVariableName: Parser[String] {
    def apply(in: Input): ParseResult[String]
  } = new Parser[String] {
    def apply(in: Input): ParseResult[String] = name(in) match {
      case Success(x, in1) =>
        val variableName = x.toUpperCase
        intVariableMap.get(variableName) match {
          case Some(_) =>
            Success(x, in1)
          case None =>
            var suffix = ""
            if(columns.contains(variableName)) {
              suffix += s" $x is a column of type ${typeCharToString(columns(variableName).dataType)}."
            } else {
              val closest = StringDistance.findClosest(variableName, 3, columnNames.toSeq)
              if(!closest.isEmpty) {
                suffix = s" Did you mean: $closest?"
              }
            }
            Failure(s"Invalid Float/Double variable name: $x.$suffix", in1)
        }
      case Failure(msg, next) =>
        Failure(msg, next)
    }
  }

  def intNumber: Parser[String] {
    def apply(in: Input): ParseResult[String]
  } = new Parser[String] {
    def apply(in: Input): ParseResult[String] = myfloatingPointNumber(in) match {
      case Success(x, in1) =>
        var t = false
        try {
          x.toInt; t = true
        } catch {
          case _: java.lang.NumberFormatException => t = false
        }
        if (t) {
          Success(x, in1)
        } else {
          Failure(s"Invalid integer: $x", in1)
        }
      case Failure(msg, next) =>
        Failure(msg, next)

    }
  }

  def longNumber: Parser[String] {
    def apply(in: Input): ParseResult[String]
  } = new Parser[String] {
    def apply(in: Input): ParseResult[String] = myfloatingPointNumber(in) match {
      case Success(x, in1) =>
        var t = false
        try {
          x.toLong; t = true
        } catch {
          case _: java.lang.NumberFormatException => t = false
        }
        if (t) {
          Success(x, in1)
        } else {
          Failure(s"Invalid long: $x", in1)
        }
      case Failure(msg, next) =>
        Failure(msg, next)
    }
  }

  def relexpr: Parser[bFun] =
    relterm ~ rep("OR".ignoreCase ~> relterm) ^^ {
      case seed ~ fs => fs.foldLeft(seed)((a, f) => {
        cvp => {
          a(cvp) || f(cvp)
        }
      })
  }

  def relterm: Parser[bFun] =
    predi ~ rep("AND".ignoreCase ~> predi) ^^ {
      case seed ~ fs => fs.foldLeft(seed)((a, f) => {
        cvp => {
          a(cvp) && f(cvp)
        }
      })
  }

  def predi: Parser[bFun] =
    ipredicomp |||
    lpredicomp |||
    dpredicomp |||
    spredicomp |||
    "(" ~> relexpr <~ ")" |
    "NOT".ignoreCase ~ "(" ~> relexpr <~ ")" ^^ (
      x => { line: ColumnValueProvider => {!x(line)} }) |
    "ISINT".ignoreCase ~ "(" ~> StringVariableName <~ ")" ^^ (
      x => { line: ColumnValueProvider => {
        val t = StringVariableHandler(x)
        var b = if (t(line) == "") false else true
        try {
          t(line).toInt
        } catch {
          case _: Exception => b = false
        }
        b
    }
    }) |
    "ISFLOAT".ignoreCase ~ "(" ~> StringVariableName <~ ")" ^^ (
      x => {
        line: ColumnValueProvider => {
        val t = StringVariableHandler(x)
        var b = if (t(line) == "") false else true
        try {
          t(line).toDouble
        } catch {
          case _: Exception => b = false
        }
        b
      }
    }) |
    "ISLONG".ignoreCase ~ "(" ~> StringVariableName <~ ")" ^^ (
      x => {
        line: ColumnValueProvider => {
          val t = StringVariableHandler(x)
          var b = if (t(line) == "") false else true
          try {
            t(line).toLong
          } catch {
            case _: Exception => b = false
          }
          b
        }
      })

  def compareop[R](implicit ev: R => Ordered[R]): Parser[(ColumnValueProvider=>R, ColumnValueProvider=>R, ColumnValueProvider) => Boolean] =
    ("==" ^^ (_ => {
      (l: ColumnValueProvider=>R, r: ColumnValueProvider=>R, line: ColumnValueProvider) => {
        l(line) == r(line)
      }
    })) |
      ("<=" ^^ (_ => { (l: ColumnValueProvider=>R, r: ColumnValueProvider=>R, line: ColumnValueProvider) => {
        l(line) <= r(line)
      }
      })) |
      (">=" ^^ (_ => { (l: ColumnValueProvider=>R, r: ColumnValueProvider=>R, line: ColumnValueProvider) => {
        l(line) >= r(line)
      }
      })) |
      ("<>" ^^ (_ => { (l: ColumnValueProvider=>R, r: ColumnValueProvider=>R, line: ColumnValueProvider) => {
        l(line) != r(line)
      }
      })) |
      ("=" ^^ (_ => { (l: ColumnValueProvider=>R, r: ColumnValueProvider=>R, line: ColumnValueProvider) => {
        l(line) == r(line)
      }
      })) |
      ("<" ^^ (_ => { (l: ColumnValueProvider=>R, r: ColumnValueProvider=>R, line: ColumnValueProvider) => {
        l(line) < r(line)
      }
      })) |
      (">" ^^ (_ => {
        (l: ColumnValueProvider=>R, r: ColumnValueProvider=>R, line: ColumnValueProvider) => {
          l(line) > r(line)
        }
      })) |
      ("!=" ^^ (_ => { (l: ColumnValueProvider=>R, r: ColumnValueProvider=>R, line: ColumnValueProvider) => {
        l(line) != r(line)
      }
      }))

  // We need to special case Double to deal correctly with NaN
  def compareopDouble: Parser[(ColumnValueProvider=>Double, ColumnValueProvider=>Double, ColumnValueProvider) => Boolean] =
    ("==" ^^ (_ => {
      (l: ColumnValueProvider=>Double, r: ColumnValueProvider=>Double, line: ColumnValueProvider) => {
        l(line) == r(line)
      }
    })) |
      ("<=" ^^ (_ => { (l: ColumnValueProvider=>Double, r: ColumnValueProvider=>Double, line: ColumnValueProvider) => {
        l(line) <= r(line)
      }
      })) |
      (">=" ^^ (_ => { (l: ColumnValueProvider=>Double, r: ColumnValueProvider=>Double, line: ColumnValueProvider) => {
        l(line) >= r(line)
      }
      })) |
      ("<>" ^^ (_ => { (l: ColumnValueProvider=>Double, r: ColumnValueProvider=>Double, line: ColumnValueProvider) => {
        l(line) != r(line)
      }
      })) |
      ("=" ^^ (_ => { (l: ColumnValueProvider=>Double, r: ColumnValueProvider=>Double, line: ColumnValueProvider) => {
        l(line) == r(line)
      }
      })) |
      ("<" ^^ (_ => { (l: ColumnValueProvider=>Double, r: ColumnValueProvider=>Double, line: ColumnValueProvider) => {
        l(line) < r(line)
      }
      })) |
      (">" ^^ (_ => {
        (l: ColumnValueProvider=>Double, r: ColumnValueProvider=>Double, line: ColumnValueProvider) => {
          l(line) > r(line)
        }
      })) |
      ("!=" ^^ (_ => { (l: ColumnValueProvider=>Double, r: ColumnValueProvider=>Double, line: ColumnValueProvider) => {
        l(line) != r(line)
      }
      }))

  def dpredicomp: Parser[bFun] {
    def apply(in: Input): ParseResult[bFun]
  } = new Parser[bFun] {
    def apply(in: Input): ParseResult[bFun] = {
      val p = (dexpr ~ compareopDouble ~ dexpr) ^^ {
        case l ~ cpop ~ r =>
          (line: ColumnValueProvider) => cpop(l, r, line)
      }
      p(in)
    }
  }

  def lpredicomp: Parser[bFun] {
    def apply(in: Input): ParseResult[bFun]
  } = new Parser[bFun] {
    def apply(in: Input): ParseResult[bFun] = {
      val p = (lexpr ~ compareop[Long] ~ lexpr) ^^ {
        case l ~ cpop ~ r =>
          (line: ColumnValueProvider) => cpop(l, r, line)
      }
      p(in)
    }
  }

  def ipredicomp: Parser[bFun] {
    def apply(in: Input): ParseResult[bFun]
  }  = new Parser[bFun] {
    def apply(in: Input): ParseResult[bFun] = {
      val p = (iexpr ~ compareop[Int] ~ iexpr) ^^ {
        case l ~ cpop ~ r =>
          (line: ColumnValueProvider) => cpop(l, r, line)
      }
      p(in)
    }
  }

  def stringComparison: Parser[bFun] {
    def apply(in: Input): ParseResult[bFun]
  } = new Parser[bFun] {
    def apply(in: Input): ParseResult[bFun] = {
      val p = sexpr ~ compareop[String] ~ sexpr ^^ {
        case l ~ cpop ~ r => (line: ColumnValueProvider) => cpop(l, r, line)
      }
      p(in)
    }
  }

  def spredicomp: Parser[bFun] =
    stringComparison |
    (sexpr ~ "LIKE".ignoreCase ~ myStringLiteral) ^^ {
      case l ~ _ ~ r =>
        val patt = r.replace("*", ".*").replace("?", ".")
        (line: ColumnValueProvider) => {
          l(line).matches(patt)
        }
    } |
    (sexpr ~ "RLIKE".ignoreCase ~ myStringLiteral) ^^ {
      case l ~ _ ~ r =>
        (line: ColumnValueProvider) => {
          l(line).matches(r)
        }
    } |
    (sexpr ~ "~" ~ myStringLiteral) ^^ {
      case l ~ _ ~ r =>
        val patt = r.toUpperCase().replace("*", ".*").replace("?", ".")
        (line: ColumnValueProvider) => {
          l(line).toUpperCase().matches(patt)
        }
    } |
    (sexpr ~ "IN".ignoreCase ~ ("(" ~> stringLiteralList <~ ")")) ^^ {
      case l ~ _ ~ r =>
        val s = r.toSet
        (line: ColumnValueProvider) => s.contains(l(line))
    } |
    genericFunction[bFun](FunctionTypes.BooleanFun) |
    (sexpr ~ "INDAG".ignoreCase ~ ("(" ~> (myStringLiteralFilename ~ "," ~ myStringLiteral) <~ ")")) ^^ {
      case l ~ _ ~ (r1 ~ "," ~ r2) =>
        val s = aDagSet(r1, r2)
        (line: ColumnValueProvider) => {
          s.contains(l(line).toUpperCase())
        }
    }

  def iexpr: Parser[iFun] =
    iterm ~ rep(
      "+" ~> iterm ^^ { d => (x: iFun, line: ColumnValueProvider) => x(line) + d(line) } |
      "-" ~> iterm ^^ { d => (x: iFun, line: ColumnValueProvider) => x(line) - d(line) }
    ) ^^ {
      case seed ~ fs =>
        fs.foldLeft(seed)((a, f) => {
          line => f(a, line)
        })
    }

  def iterm: Parser[iFun] =
    ifactor ~ rep(
      "*" ~> ifactor ^^ (d => (x: iFun, line: ColumnValueProvider) => x(line) * d(line))
    ) ^^ {
      case seed ~ fs =>
        fs.foldLeft(seed)((a, f) => {
          line => f(a, line)
        })
    }

  def ifactor: Parser[iFun] =
    "-" ~> ixfactor ^^ (
      x => { line: ColumnValueProvider => {-x(line)} }
    ) |
    "+" ~> ixfactor ^^ (
      x => { line: ColumnValueProvider => {+x(line)} }
    ) |
    ixfactor

  def ixfactor: Parser[iFun] =
    ifunction ~ "^" ~ ifactor ^^ {
      case e ~ "^" ~ v => (line: ColumnValueProvider) => {scala.math.pow(e(line), v(line)).toInt}
    } |
    ivalue ~ "^" ~ ifactor ^^ {
      case e ~ "^" ~ v => (line: ColumnValueProvider) => {scala.math.pow(e(line), v(line)).toInt}
    } |
    ("(" ~> iexpr <~ ")") ~ "^" ~ ifactor ^^ {
      case e ~ "^" ~ v => (line: ColumnValueProvider) => {scala.math.pow(e(line), v(line)).toInt}
    } |
    "(" ~> iexpr <~ ")" |
    ifunction |
    ivalue

  def intIfMatcher: Parser[iFun] {
    def apply(in: Input): ParseResult[iFun]
  } = new Parser[iFun] {
    def apply(in: Input): ParseResult[iFun] = {
      val p = "IF".ignoreCase ~ "(" ~ relexpr ~ "," ~ iexpr ~ "," ~ iexpr ~ ")" ^^ {
        case _ ~ "(" ~ ex1 ~ "," ~ ex2 ~ "," ~ ex3 ~ ")" => (line: ColumnValueProvider) => {
          if (ex1(line)) ex2(line) else ex3(line)
        }
      }
      p(in)
    }
  }

  def ifunction: Parser[iFun] =
    intIfMatcher |
    genericFunction[iFun](FunctionTypes.IntFun) |
    "GTSHARE".ignoreCase ~ "(" ~ sexpr ~ "," ~ iexpr ~ "," ~ sexpr ~ "," ~ sexpr ~ "," ~ iexpr ~ "," ~ sexpr ~ "," ~ sexpr ~ ")" ^^ {
      case _ ~ "(" ~ ex1 ~ "," ~ ex2 ~ "," ~ ex3 ~ "," ~ ex4 ~ "," ~ ex5 ~ "," ~ ex6 ~ "," ~ ex7 ~ ")" => line => {
        allelesFoundVCF(ex2(line), ex3(line), ex4(line), ex5(line), ex6(line), ex7(line), refSeq, ex1(line))
      }
    }

  def ivalue: Parser[iFun] =
    intNumber ^^ (x => { _: ColumnValueProvider => x.toInt }) |
    IntVariableName ^^ (x => {IntVariableHandler(x)})

  def myIntNumber: Parser[String] = """-?\d+\w*""".r

  def lexpr: Parser[lFun] =
    lterm ~ rep(
      "+" ~> lterm ^^ (l => (x: lFun, line: ColumnValueProvider) => x(line) + l(line)) |
      "-" ~> lterm ^^ (l => (x: lFun, line: ColumnValueProvider) => x(line) - l(line))
    ) ^^ { case seed ~ fs => fs.foldLeft(seed)((a, f) => { line => f(a, line) }) }

  def lterm: Parser[lFun] =
    lfactor ~ rep(
      "*" ~> lfactor ^^ (d => (x: lFun, line: ColumnValueProvider) => x(line) * d(line))
    ) ^^ { case seed ~ fs => fs.foldLeft(seed)((a, f) => { line => f(a, line) }) }

  def lfactor: Parser[lFun] =
    "-" ~> lxfactor ^^ (
      x => { line: ColumnValueProvider => {-x(line)} }
    ) |
    "+" ~> lxfactor ^^ (
      x => { line: ColumnValueProvider => {+x(line)} }
    ) |
    lxfactor

  def lxfactor: Parser[lFun] =
    lfunction ~ "^" ~ lfactor ^^ {
      case e ~ "^" ~ v => (line: ColumnValueProvider) => {scala.math.pow(e(line).toDouble, v(line).toDouble).toLong}
    } |
    lvalue ~ "^" ~ lfactor ^^ {
      case e ~ "^" ~ v => (line: ColumnValueProvider) => {scala.math.pow(e(line).toDouble, v(line).toDouble).toLong}
    } |
    ("(" ~> lexpr <~ ")") ~ "^" ~ lfactor ^^ {
      case e ~ "^" ~ v => (line: ColumnValueProvider) => {scala.math.pow(e(line).toDouble, v(line).toDouble).toLong}
    } |
    "(" ~> lexpr <~ ")" |
    lfunction |
    lvalue

  def lfunction: Parser[lFun] =
    genericFunction[lFun](FunctionTypes.LongFun)

  def lvalue: Parser[lFun] =
    longNumber ^^ (x => {
      _: ColumnValueProvider => x.toLong
    }) |
    LongVariableName ^^ (x => {
      LongVariableHandler(x)
    })

  def dexpr: Parser[dFun] =
    dterm ~ rep(
      "+" ~> dterm ^^ (d => (x: dFun, line: ColumnValueProvider) => x(line) + d(line)) |
      "-" ~> dterm ^^ (d => (x: dFun, line: ColumnValueProvider) => x(line) - d(line))
    ) ^^ {
      case seed ~ fs => fs.foldLeft(seed)((a, f) => { line => f(a, line) })
    } |
    iexpr ^^ (x => {line: ColumnValueProvider => x(line).toDouble} )

  def dterm: Parser[dFun] =
    dfactor ~ rep(
      "*" ~> dfactor ^^ (d => (x: dFun, line: ColumnValueProvider) => x(line) * d(line)) |
      "/" ~> dfactor ^^ (d => (x: dFun, line: ColumnValueProvider) => x(line) / d(line))
    ) ^^ {
      case seed ~ fs =>
        fs.foldLeft(seed)((a, f) => { line => f(a, line) })
    } |
    iterm ^^ (x => {line: ColumnValueProvider => x(line).toDouble} )
    lterm ^^ (x => {line: ColumnValueProvider => x(line).toDouble} )

  def dfactor: Parser[dFun] =
    "-" ~> xfactor ^^ (
      x => { line: ColumnValueProvider => {-x(line)} }
    ) |
    "+" ~> xfactor ^^ (
      x => { line: ColumnValueProvider => {+x(line)} }
    ) |
    xfactor |
    ixfactor ^^ (x => {line: ColumnValueProvider => x(line).toDouble} )
    lxfactor ^^ (x => {line: ColumnValueProvider => x(line).toDouble} )

  def xfactor: Parser[dFun] =
    dfunction ~ "^" ~ dfactor ^^ {
      case e ~ "^" ~ v => (line: ColumnValueProvider) => {scala.math.pow(e(line), v(line))}
    } |
    dvalue ~ "^" ~ dfactor ^^ {
      case e ~ "^" ~ v => (line: ColumnValueProvider) => {scala.math.pow(e(line), v(line))}
    } |
    ("(" ~> dexpr <~ ")") ~ "^" ~ dfactor ^^ {
      case e ~ "^" ~ v => (line: ColumnValueProvider) => {scala.math.pow(e(line), v(line))}
    } |
    "(" ~> dexpr <~ ")" |
    dfunction |
    dvalue

  def functionArgsForVariant(fn: String, sig: String): Parser[List[TypedExpression]] {
    def apply(in: Input): ParseResult[List[TypedExpression]]
  } = new Parser[List[TypedExpression]] {
    def apply(in: Input): ParseResult[List[TypedExpression]] = {
      var args: List[TypedExpression] = List[TypedExpression]()
      val types = FunctionTypes.getArgumentTypesFromSignature(sig)
      var input = in
      for(arg <- types) {
        arg match {
          case FunctionTypes.DoubleFun =>
            dexpr(input) match {
              case Success(d, in1) =>
                args = args :+ TypedExpression(FunctionTypes.DoubleFun, d)
                input = in1
              case Failure(msg, next) =>
                return Failure(msg, next)
            }
          case FunctionTypes.IntFun =>
            iexpr(input) match {
              case Success(i, in1) =>
                args = args :+ TypedExpression(FunctionTypes.IntFun, i)
                input = in1
              case Failure(msg, next) =>
                return Failure(msg, next)
            }
          case FunctionTypes.StringFun =>
            sexpr(input) match {
              case Success(s, in1) =>
                args = args :+ TypedExpression(FunctionTypes.StringFun, s)
                input = in1
              case Failure(msg, next) =>
                return Failure(msg, next)
            }
          case FunctionTypes.LongFun =>
            lexpr(input) match {
              case Success(l, in1) =>
                args = args :+ TypedExpression(FunctionTypes.LongFun, l)
                input = in1
              case Failure(msg, next) =>
                return Failure(msg, next)
            }
          case FunctionTypes.BooleanFun =>
            relexpr(input) match {
              case Success(b, in1) =>
                args = args :+ TypedExpression(FunctionTypes.BooleanFun, b)
                input = in1
              case Failure(msg, next) =>
                return Failure(msg, next)
            }
          case FunctionTypes.StringList =>
            stringLiteralList(input) match {
              case Success(sl, in1) =>
                args = args :+ TypedExpression(FunctionTypes.StringList, sl)
                input = in1
              case Failure(msg, next) =>
                return Failure(msg, next)
            }
        }
        if (args.length < types.length) {
          literal(",")(input) match {
            case Success(_, in1) =>
              input = in1
            case Failure(msg, next) =>
              return Failure(msg, next)
          }
        }
      }
      literal(")")(input) match {
        case Success(_, in1) =>
          Success(args, in1)
        case Failure(msg, next) =>
          Failure(msg, next)
      }
    }
  }

  def applyFunction[R](fn: String, sig: String, args: List[TypedExpression]): R = {
    val mangledName = s"${fn}_$sig"
    val wrapper = functions.lookupWrapper(mangledName)
    wrapper.call[R](this, args)
  }

  def functionArgs[R](fn: String, returnType: String): Parser[R] {
    def apply(in: Input): ParseResult[R]
  } = new Parser[R] {
    def apply(in: Input): ParseResult[R] = {
      val variants = functions.getVariantsByReturnType(fn, returnType)
      var errorMessage = ""
      var maxLength = 0
      var errorInput = in
      for( sig <- variants) {
        functionArgsForVariant(fn, sig)(in) match {
          case Success(args, in1) =>
            val f = applyFunction[R](fn, sig, args)
            return Success(f, in1)
          case Failure(msg, next) =>
            if(next.offset >= maxLength) {
              errorMessage = msg
              maxLength = next.offset
              errorInput = next
            }
        }
      }

      Failure(errorMessage, errorInput)
    }
  }

  def genericFunction[R](returnType: String): Parser[R] {
    def apply(in: Input): ParseResult[R]
  } = new Parser[R] {
    def apply(in: Input): ParseResult[R] = {
      ident(in) match {
        case Success(fn, in1) =>
          val variants = functions.getVariantsByReturnType(fn.toUpperCase, returnType)
          if (variants.isEmpty) {
            Failure(s"Unrecognized function: $fn", in1)
          } else {
            literal("(")(in1) match {
              case Success(_, in2) =>
                functionArgs[R](fn.toUpperCase, returnType)(in2) match {
                  case Success(x, in3) =>
                    Success(x, in3)
                  case Failure(msg, next) =>
                    Failure(msg, next)
                }
              case Failure(msg, next) =>
                Failure(s"not a function: $msg", next)
            }
          }
        case Failure(_, next) =>
          Failure("not an identifier", next)
      }
    }
  }

  def doubleIfMatcher: Parser[dFun] {
    def apply(in: Input): ParseResult[dFun]
  } = new Parser[dFun] {
    def apply(in: Input): ParseResult[dFun] = {
      val p = "IF".ignoreCase ~ "(" ~ relexpr ~ "," ~ dexpr ~ "," ~ dexpr ~ ")" ^^ {
        case _ ~ "(" ~ ex1 ~ "," ~ ex2 ~ "," ~ ex3 ~ ")" => (line: ColumnValueProvider) => {
          if (ex1(line)) ex2(line) else ex3(line)
        }
      }
      p(in)
    }
  }

  def dfunction: Parser[dFun] =
    doubleIfMatcher |
    genericFunction[dFun](FunctionTypes.DoubleFun)

  def dvalue: Parser[dFun] =
    myfloatingPointNumber ^^ (x => { _: ColumnValueProvider => x.toDouble }) |
    DoubleVariableName ^^ (x => {DoubleVariableHandler(x)})

  def myfloatingPointNumber: Parser[String] =
    basePos ^^ (s => s.substring(0, s.length - 2).replace(",", "")) |
    """(NaN)|(\d+(\.\d*)?|\d*\.\d+)([eE][+-]?\d+)?[fFdD]?""".r

  def basePos: Parser[String] = basePos1 | basePos2 | basePos3 | basePos4

  def basePos1: Parser[String] = """\d{1,3}bp""".r

  def basePos2: Parser[String] = """\d{1,3}\,\d{3}bp""".r

  def basePos3: Parser[String] = """\d{1,3}\,\d{3}\,\d{3}bp""".r

  def basePos4: Parser[String] = """\d{1,3}\,\d{3}\,\d{3}\,\d{3}bp""".r

  def sexpr: Parser[sFun] =
    sterm ~ rep("+" ~> sterm ^^ (d => (x: sFun, line: ColumnValueProvider) => x(line) + d(line))) ^^ {
      case seed ~ fs =>
        fs.foldLeft(seed)((a, f) => { line => f(a, line) })
    }

  def sterm: Parser[sFun] =
    sfunction | svalue

  def stringIfMatcher: Parser[sFun] {
    def apply(in: Input): ParseResult[sFun]
  } = new Parser[sFun] {
    def apply(in: Input): ParseResult[sFun] = {
      val p = "IF".ignoreCase ~ "(" ~ relexpr ~ "," ~ sexpr ~ "," ~ sexpr ~ ")" ^^ {
        case _ ~ "(" ~ ex1 ~ "," ~ ex2 ~ "," ~ ex3 ~ ")" => (line: ColumnValueProvider) => {
          if (ex1(line)) ex2(line) else ex3(line)
        }
      }
      p(in)
    }
  }

  def sfunction: Parser[sFun] =
    stringIfMatcher |
    genericFunction[sFun](FunctionTypes.StringFun) |
    "GTSTAT".ignoreCase ~ "(" ~ sexpr ~ "," ~ iexpr ~ "," ~ sexpr ~ "," ~ sexpr ~ "," ~ iexpr ~ "," ~ sexpr ~ "," ~ sexpr ~ "," ~ iexpr ~ "," ~ sexpr ~ "," ~ sexpr ~ ")" ^^ {
      case _ ~ "(" ~ ex1 ~ "," ~ ex2 ~ "," ~ ex3 ~ "," ~ ex4 ~ "," ~ ex5 ~ "," ~ ex6 ~ "," ~ ex7 ~ "," ~ ex8 ~ "," ~ ex9 ~ "," ~ ex10 ~ ")" => (line: ColumnValueProvider) => {
        ParseUtilities.gtStatVCF(ex2(line), ex3(line), ex4(line), ex5(line), ex6(line), ex7(line), ex8(line), ex9(line), ex10(line), refSeq, ex1(line))
      }
    } |
    "GTFA".ignoreCase ~ "(" ~ sexpr ~ "," ~ iexpr ~ "," ~ sexpr ~ "," ~ sexpr ~ "," ~ iexpr ~ "," ~ sexpr ~ "," ~ sexpr ~ "," ~ iexpr ~ "," ~ sexpr ~ "," ~ sexpr ~ ")" ^^ {
      case _ ~ "(" ~ ex1 ~ "," ~ ex2 ~ "," ~ ex3 ~ "," ~ ex4 ~ "," ~ ex5 ~ "," ~ ex6 ~ "," ~ ex7 ~ "," ~ ex8 ~ "," ~ ex9 ~ "," ~ ex10 ~ ")" => (line: ColumnValueProvider) => {
        ParseUtilities.fatherGTVCF(ex2(line), ex3(line), ex4(line), ex5(line), ex6(line), ex7(line), ex8(line), ex9(line), ex10(line), refSeq, ex1(line))
      }
    } |
    "GTMA".ignoreCase ~ "(" ~ sexpr ~ "," ~ iexpr ~ "," ~ sexpr ~ "," ~ sexpr ~ "," ~ iexpr ~ "," ~ sexpr ~ "," ~ sexpr ~ "," ~ iexpr ~ "," ~ sexpr ~ "," ~ sexpr ~ ")" ^^ {
      case _ ~ "(" ~ ex1 ~ "," ~ ex2 ~ "," ~ ex3 ~ "," ~ ex4 ~ "," ~ ex5 ~ "," ~ ex6 ~ "," ~ ex7 ~ "," ~ ex8 ~ "," ~ ex9 ~ "," ~ ex10 ~ ")" => (line: ColumnValueProvider) => {
        ParseUtilities.motherGTVCF(ex2(line), ex3(line), ex4(line), ex5(line), ex6(line), ex7(line), ex8(line), ex9(line), ex10(line), refSeq, ex1(line))
      }
    }

  def svalue: Parser[sFun] =
    myStringLiteral ^^ (x => (_: ColumnValueProvider) => x) |
    StringVariableName ^^ (x => {StringVariableHandler(x)})

  def name: Parser[String] {
    def apply(in: Input): ParseResult[String]
  } = new Parser[String] {
    def apply(in: Input): ParseResult[String] = {
      val p = colRef | "#rc" | "#RC" | """$rc""" | """$RC""" | """[[0-9]*a-zA-Z_][\w.]*""".r
      val result = p(in)
      result match {
        case Failure(msg, next) =>
          Failure("column name expected - " + msg, next)
        case other => other
      }
    }
  }

  def myQuotedStringLiteral: Parser[String] =
    ("'" +"""(\\.|[^'\\]*)*""" + "'").r | ("\"" +"""(\\.|[^"\\]*)*""" + "\"").r

  private def stripBackslashAndQuotes(x: String): String = {
    val strbuff = new mutable.StringBuilder(x.length)
    var c: Int = 1
    var inBackSlash = false
    while (c < x.length - 1) {
      val xc: Char = x.charAt(c)
      if (inBackSlash || xc != '\\') {
        strbuff.append(xc)
        inBackSlash = false
      } else {
        inBackSlash = true
      }
      c += 1
    }
    strbuff.toString
  }

  def myStringLiteral: Parser[String] = myQuotedStringLiteral ^^ (x => /* x.substring(1,x.length-1) */ stripBackslashAndQuotes(x))

  //parses filename for INDAG command- lexer first checks myQuotedStringLiteral, strips backslashes and quotes, otherwise (|||) parses the filename.
  def myStringLiteralFilename: Parser[String] = myQuotedStringLiteral ^^ (x => /* x.substring(1,x.length-1) */ stripBackslashAndQuotes(x)) ||| """(\\.|[^'^,^\n^\(^\)\\]*)*""".r

  def stringLiteralList: Parser[List[String]] = myStringLiteral ~ rep("," ~> myStringLiteral) ^^ { case h ~ t => h :: t }

  def colRef: Parser[String] =
    """[\#|\$]\d{1,3}[i|f|s|I|F|S]*""".r

  private def clearEvalFunctions(): Unit = {
    stringFunction = null
    intFunction = null
    doubleFunction = null
    longFunction = null
    booleanFunction = null
  }

  def compileCalculationWithAntlr(input: String) = {
    val syntaxChecker = new SyntaxChecker
    try {
      val context = syntaxChecker.parseCalc(input)
      calcLambda = context.accept(calcCompiler)
      outputType = calcLambda.getType
    } catch {
      case e: GorParsingException =>
        val where = input.substring(0, e.getPos) + " <-- "
        val errorMessage = e.getMessage
        val finalMessage = s"$errorMessage\n$input\n$where"
        throw new GorParsingException(finalMessage, e)
    }

    outputType
  }

  def getBooleanFunction(): ColumnValueProvider => Boolean = {
    if (runClassic) {
      booleanFunction
    } else {
      calcLambda.evaluateBoolean
    }
  }

  def getStringFunction(): ColumnValueProvider => String = {
    if (runClassic) {
      stringFunction
    } else {
      calcLambda.evaluateString
    }
  }

  def getDoubleFunction(): ColumnValueProvider => Double = {
    if (runClassic) {
      doubleFunction
    } else {
      calcLambda.evaluateDouble
    }
  }

  def getLongFunction(): ColumnValueProvider => Long = {
    if (runClassic) {
      longFunction
    } else {
      calcLambda.evaluateLong
    }
  }

  def getIntFunction(): ColumnValueProvider => Int = {
    if (runClassic) {
      intFunction
    } else {
      calcLambda.evaluateInt
    }
  }

  def getCompiledBooleanFunction(): ColumnValueProvider => Boolean = {
    outputType.charAt(0) match {
      case 'B' => getBooleanFunction()
    }
  }

  def getCompiledStringFunction(): ColumnValueProvider => String = {
    outputType.charAt(0) match {
      case 'S' => getStringFunction()
      case 'D' => cvp: ColumnValueProvider => getDoubleFunction().apply(cvp).toString
      case 'L' => cvp: ColumnValueProvider => getLongFunction().apply(cvp).toString
      case 'I' => cvp: ColumnValueProvider => getIntFunction().apply(cvp).toString
      case 'B' => cvp: ColumnValueProvider => getBooleanFunction().apply(cvp).toString
      case _ => _: ColumnValueProvider => ""
    }
  }

  /**
    * Compiles the given string as an expression. If the compilation succeeds,
    * evalStringFunction, evealIntFunction, evalDoubleFunction, evalLongFunction or
    * evalBooleanFunction can be called on this parser instance to evaluate the
    * expression, depending on the resulting type.
    * <br><br>
    * If the compilation fails, a GorParsingException is thrown.
    *
    * @param input A string representing a Boolean expression
    * @return A string representing the type of the expression. See the constants
    *         in FunctionTypes.
    */
  def compileCalculation(input: String): String = {
    if (compileAntlr) {
      compileCalculationWithAntlr(input)
    }

    if (compileClassic) {
      clearEvalFunctions()

      var errorMessage = ""
      var expressionType = ""
      var maxLength = 0

      val ir = parseAll(iexpr, input)
      ir match {
        case Success(e, _) =>
          intFunction = e
          outputType = FunctionTypes.IntFun
        case Failure(msg, next) =>
          errorMessage = msg
          expressionType = "Integer"
          maxLength = next.offset
          val lr = parseAll(lexpr, input)
          lr match {
            case Success(e, _) =>
              longFunction = e
              outputType = FunctionTypes.LongFun
            case Failure(msg1, next1) =>
              if(next1.offset >= maxLength) {
                errorMessage = msg1
                expressionType = "Long"
                maxLength = next1.offset
              }
              val dr = parseAll(dexpr, input)
              dr match {
                case Success(e, _) =>
                  doubleFunction = e
                  outputType = FunctionTypes.DoubleFun
                case Failure(msg2, next2) =>
                  if(next2.offset >= maxLength) {
                    errorMessage = msg2
                    expressionType = "Double"
                    maxLength = next2.offset
                  }
                  val i = input.indexOf("(")
                  val parseInput = if(i > 0) {
                    val fn = input.substring(0, i).toUpperCase
                    if (pathfunctions.hasFunction(fn)) {
                      s"${input.substring(0, i + 1)}'${input.substring(i + 1, input.length - 1).trim}')"
                    } else input
                  } else input
                  val sr = parseAll(sexpr, parseInput)
                  sr match {
                    case Success(e, _) =>
                      stringFunction = e
                      outputType = FunctionTypes.StringFun
                    case Failure(msg3, next3) =>
                      if(next3.offset >= maxLength) {
                        errorMessage = msg3
                        expressionType = "String"
                        maxLength = next3.offset
                      }
                      val where = s"${input.substring(0, maxLength)} <-- "
                      val finalMessage = s"Expression compiled as $expressionType\n$errorMessage\n$input\n$where"

                      throw new GorParsingException(finalMessage, input)

                  }
              }
          }
      }
    }
    outputType
  }

  /**
    * Compiles the given string as Boolean expression. If the compilation succeeds,
    * evalBooleanFunction can be called on this parser instance to evaluate the
    * filter expression.
    * <br><br>
    * If the compilation fails, a GorParsingException is thrown.
    *
    * @param input A string representing a Boolean expression
    */
  def compileFilter(input: String): Unit = {
    if (compileAntlr) {
      val syntaxChecker = new SyntaxChecker
      val context = syntaxChecker.parseFilter(input)
      calcLambda = context.accept(calcCompiler)
    }

    if (compileClassic) {
      clearEvalFunctions()
      parseAll(relexpr, input) match {
        case Success(e, _) =>
          outputType = FunctionTypes.BooleanFun
          booleanFunction = e
        case f: NoSuccess => throw new GorParsingException(f.msg, input)
      }
    }
  }

  def evalFunction(cvp: ColumnValueProvider): String = {
    if (runClassic) {
      if (outputType == "String") evalStringFunction(cvp)
      else if (outputType == "Double") evalDoubleFunction(cvp).toString
      else if (outputType == "Long") evalLongFunction(cvp).toString
      else if (outputType == "Int") evalIntFunction(cvp).toString
      else if (outputType == "Boolean") evalBooleanFunction(cvp).toString
      else ""
    } else {
      outputType.charAt(0) match {
        case 'S' => calcLambda.evaluateString(cvp)
        case 'D' => calcLambda.evaluateDouble(cvp).toString
        case 'L' => calcLambda.evaluateLong(cvp).toString
        case 'I' => calcLambda.evaluateInt(cvp).toString
        case 'B' => calcLambda.evaluateBoolean(cvp).toString
        case _ => ""
      }
    }
  }

  /**
    * Evaluates the current function using the given column value provider.
    * A function returning a string must have been compiled using compileCalculation
    * prior to calling this.
    * @param cvp A ColumnValueProvider - typically the current row in a GOR pipeline
    * @return The result of evaluating the function
    */
  def evalStringFunction(cvp: ColumnValueProvider): String =
    if (runClassic) {
      stringFunction(cvp: ColumnValueProvider)
    } else {
      calcLambda.evaluateString(cvp)
    }

  /**
    * Evaluates the current function using the given column value provider.
    * A function returning an integer must have been compiled using compileCalculation
    * prior to calling this.
    * @param cvp A ColumnValueProvider - typically the current row in a GOR pipeline
    * @return The result of evaluating the function
    */
  def evalIntFunction(cvp: ColumnValueProvider): Int =
    if (runClassic) {
      intFunction(cvp: ColumnValueProvider)
    } else {
      calcLambda.evaluateInt(cvp)
    }

  /**
    * Evaluates the current function using the given column value provider.
    * A function returning a double must have been compiled using compileCalculation
    * prior to calling this.
    * @param cvp A ColumnValueProvider - typically the current row in a GOR pipeline
    * @return The result of evaluating the function
    */
  def evalDoubleFunction(cvp: ColumnValueProvider): Double =
    if (runClassic) {
      doubleFunction(cvp: ColumnValueProvider)
    } else {
      calcLambda.evaluateDouble(cvp)
    }

  /**
    * Evaluates the current function using the given column value provider.
    * A function returning a long must have been compiled using compileCalculation
    * prior to calling this.
    * @param cvp A ColumnValueProvider - typically the current row in a GOR pipeline
    * @return The result of evaluating the function
    */
  def evalLongFunction(cvp: ColumnValueProvider): Long =
    if (runClassic) {
      longFunction(cvp: ColumnValueProvider)
    } else {
      calcLambda.evaluateLong(cvp)
    }


  /**
    * Evaluates the current function using the given column value provider.
    * A function returning a boolean must have been compiled using compileCalculation
    * prior to calling this.
    * @param cvp A ColumnValueProvider - typically the current row in a GOR pipeline
    * @return The result of evaluating the function
    */
  def evalBooleanFunction(cvp: ColumnValueProvider): Boolean =
    if (runClassic) {
      booleanFunction(cvp: ColumnValueProvider)
    } else {
      calcLambda.evaluateBoolean(cvp)
    }

  // Implicit conversion of String to a CaseInsensitive - basically adding the
  // ignoreCase method to strings.
  private implicit def makeCaseInsensitive(str: String): CaseInsensitive = new CaseInsensitive(str)

  /**
    * Wrapper class for String, adding a method to make regular expressions
    * case insensitive.
    * @param str a regular expression (or a string constant)
    */
  private class CaseInsensitive(str: String) {
    /**
      * Aid for regular expression parsers to make the given expression case insensitive.
      * @return a regular expression that ignores case when matching
      */
    def ignoreCase: Parser[String] = ("""(?i)\Q""" + str + """\E""").r
  }

  def typeCharToString(tpe: String): String = {
    tpe match {
      case "S" => "String"
      case "I" => "Integer"
      case "L" => "Long"
      case "B" => "Boolean"
      case "D" => "Double"
    }
  }
}

object ParseArith {
  def apply() = new ParseArith()
}