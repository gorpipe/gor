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

import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.model.ColumnValueProvider
import org.junit.runner.RunWith
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.junit.JUnitRunner

case class Column(name: String, tpe: String, value: Any)

case class MockCvp(columns: Array[Column], parser: ParseArith) extends ColumnValueProvider {
  set(parser)

  private def set(parser: ParseArith): Unit = {
    val names = columns.map(c => c.name)
    val types = columns.map(c => c.tpe)
    parser.setColumnNamesAndTypes(names, types)
  }

  override def stringValue(col: Int): String = {
    val c = columns(col)
    c.tpe match {
      case "D" => c.value.asInstanceOf[Double].toString
      case "L" => c.value.asInstanceOf[Long].toString
      case "I" => c.value.asInstanceOf[Int].toString
      case "S" => c.value.asInstanceOf[String]
      case _ => throw new RuntimeException("Wrong type")
    }
  }

  override def intValue(col: Int): Int = {
    val c = columns(col)
    if (c.tpe == "I") c.value.asInstanceOf[Int] else throw new RuntimeException("Wrong type")
  }

  override def longValue(col: Int): Long = {
    val c = columns(col)
    c.tpe match {
      case "L" => c.value.asInstanceOf[Long]
      case "I" => c.value.asInstanceOf[Int].toLong
      case _ => throw new RuntimeException("Wrong type")
    }
  }

  override def doubleValue(col: Int): Double = {
    val c = columns(col)
    c.tpe match {
      case "D" => c.value.asInstanceOf[Double]
      case "L" => c.value.asInstanceOf[Long].toDouble
      case "I" => c.value.asInstanceOf[Int].toDouble
      case _ => throw new RuntimeException("Wrong type")
    }
  }
}

@RunWith(classOf[JUnitRunner])
class UTestParseArith extends AnyFlatSpec {
  "Number parsing" should "recognize an Int" in {
    val p = ParseArith()
    val result = p.compileCalculation("42")
    assert(result == FunctionTypes.IntFun)
  }

  it should "recognize a negative Int" in {
    val p = ParseArith()
    val result = p.compileCalculation("-42")
    assert(result == FunctionTypes.IntFun)
  }

  it should "recognize an Int with a +" in {
    val p = ParseArith()
    val result = p.compileCalculation("+42")
    assert(result == FunctionTypes.IntFun)
  }

  it should "recognize a Long" in {
    val p = ParseArith()
    val result = p.compileCalculation("17179869184")
    assert(result == FunctionTypes.LongFun)
  }

  it should "recognize a negative Long" in {
    val p = ParseArith()
    val result = p.compileCalculation("-17179869184")
    assert(result == FunctionTypes.LongFun)
  }

  it should "recognize an Long with a +" in {
    val p = ParseArith()
    val result = p.compileCalculation("+17179869184")
    assert(result == FunctionTypes.LongFun)
  }

  it should "recognize a Double" in {
    val p = ParseArith()
    val result = p.compileCalculation("3.14")
    assert(result == FunctionTypes.DoubleFun)
  }

  it should "recognize a negative Double" in {
    val p = ParseArith()
    val result = p.compileCalculation("-3.14")
    assert(result == FunctionTypes.DoubleFun)
  }

  it should "recognize an Double with a +" in {
    val p = ParseArith()
    val result = p.compileCalculation("+3.14")
    assert(result == FunctionTypes.DoubleFun)
  }

  "^" should "work for Int" in {
    val p = ParseArith()
    val result = p.compileCalculation("0^0")
    assert(result == FunctionTypes.IntFun)
  }

  it should "work for Long" in {
    val p = ParseArith()
    val result = p.compileCalculation("17179869184^0")
    assert(result == FunctionTypes.LongFun)
  }

  it should "work for Double" in {
    val p = ParseArith()
    val result = p.compileCalculation("0.0^0.0")
    assert(result == FunctionTypes.DoubleFun)
  }

  it should "work for Double and Int" in {
    val p = ParseArith()
    val cvp = MockCvp(Array(Column("x", "D", 3.14), Column("y", "I", 4)), p)
    val result = p.compileCalculation("x^y")
    assert(result == FunctionTypes.DoubleFun)
  }

  "Column reference" should "throw GorParsingException when not found" in {
    val p = ParseArith()
    intercept[GorParsingException] {
      p.compileCalculation("x + 1")
    }
  }

  it should "recognize an Int variable" in {
    val p = ParseArith()
    val cvp = MockCvp(Array(Column("x", "I", 42)), p)
    val result = p.compileCalculation("x+1")
    assert(result == "Int")
  }

  it should "evaluate an Int variable" in {
    val p = ParseArith()
    val cvp = MockCvp(Array(Column("x", "I", 42)), p)

    p.compileCalculation("x+1")
    val result = p.evalIntFunction(cvp)
    assert(result == 43)
  }

  it should "recognize a Long variable" in {
    val p = ParseArith()
    val cvp = MockCvp(Array(Column("x", "L", 42L)), p)
    val result = p.compileCalculation("x+1")
    assert(result == "Long")
  }

  it should "evaluate a Long variable" in {
    val p = ParseArith()
    val cvp = MockCvp(Array(Column("x", "L", 42L)), p)

    p.compileCalculation("x+1")
    val result = p.evalLongFunction(cvp)
    assert(result == 43)
  }

  it should "recognize a Double variable" in {
    val p = ParseArith()
    val cvp = MockCvp(Array(Column("x", "D", 3.14)), p)
    val result = p.compileCalculation("x+1")
    assert(result == "Double")
  }

  it should "evaluate a Double variable" in {
    val p = ParseArith()
    val cvp = MockCvp(Array(Column("x", "D", 3.14)), p)

    p.compileCalculation("x+1")
    val result = p.evalDoubleFunction(cvp)
    assert(result == 3.14 + 1.0)
  }

  it should "work for an Int variable in a Double expression" in {
    val p = ParseArith()
    val cvp = MockCvp(Array(Column("x", "I", 42)), p)
    val result = p.compileCalculation("x+1.0")
    assert(result == "Double")
  }

  it should "work for a Long variable in a Double expression" in {
    val p = ParseArith()
    val cvp = MockCvp(Array(Column("x", "L", 17179869184L)), p)
    val result = p.compileCalculation("x+1.0")
    assert(result == "Double")
  }

  "Less than" should "work for Long" in {
    val p = ParseArith()
    val cvp = MockCvp(Array(Column("x", "L", Int.MaxValue.toLong + 32), Column("y", "L", Int.MinValue.toLong - 32)), p)
    p.compileFilter("y<x")
    assert(p.evalBooleanFunction(cvp))
  }

  it should "work for mixed number types" in {
    val p = ParseArith()
    val cvp = MockCvp(Array(Column("x", "I", 42), Column("y", "L", Int.MinValue.toLong - 32)), p)
    p.compileFilter("y<x")
    assert(p.evalBooleanFunction(cvp))
  }

  it should "return false for NaN<1.0" in {
    val p = ParseArith()
    val cvp = MockCvp(Array(Column("x", "D", Double.NaN), Column("y", "D", 1.0)), p)
    p.compileFilter("x<y")
    assert(!p.evalBooleanFunction(cvp))
  }

  it should "return false for 1.0<NaN" in {
    val p = ParseArith()
    val cvp = MockCvp(Array(Column("x", "D", Double.NaN), Column("y", "D", 1.0)), p)
    p.compileFilter("y<x")
    assert(!p.evalBooleanFunction(cvp))
  }

  it should "return true for 1.0<Positive Infinity" in {
    val p = ParseArith()
    val cvp = MockCvp(Array(Column("x", "D", Double.PositiveInfinity), Column("y", "D", 1.0)), p)
    p.compileFilter("y<x")
    assert(p.evalBooleanFunction(cvp))
  }

  it should "return false for 1.0<Negative Infinity" in {
    val p = ParseArith()
    val cvp = MockCvp(Array(Column("x", "D", Double.NegativeInfinity), Column("y", "D", 1.0)), p)
    p.compileFilter("y<x")
    assert(!p.evalBooleanFunction(cvp))
  }

  it should "work for mixed numeric expression on the left" in {
    val p = ParseArith()
    val cvp = MockCvp(Array(Column("x", "I", 1)), p)
    p.compileFilter("x*0.01<1")
    assert(p.evalBooleanFunction(cvp))
  }

  it should "work for mixed numeric expression on the right" in {
    val p = ParseArith()
    val cvp = MockCvp(Array(Column("x", "I", 1)), p)
    p.compileFilter("1<x*0.01")
    assert(!p.evalBooleanFunction(cvp))
  }

  "Greater than" should "work for Long" in {
    val p = ParseArith()
    val cvp = MockCvp(Array(Column("x", "L", Int.MaxValue.toLong + 32), Column("y", "L", Int.MinValue.toLong - 32)), p)
    p.compileFilter("x>y")
    assert(p.evalBooleanFunction(cvp))
  }

  it should "return false for NaN>1.0" in {
    val p = ParseArith()
    val cvp = MockCvp(Array(Column("x", "D", Double.NaN), Column("y", "D", 1.0)), p)
    p.compileFilter("x>y")
    assert(!p.evalBooleanFunction(cvp))
  }

  it should "return false for 1.0>NaN" in {
    val p = ParseArith()
    val cvp = MockCvp(Array(Column("x", "D", Double.NaN), Column("y", "D", 1.0)), p)
    p.compileFilter("y>x")
    assert(!p.evalBooleanFunction(cvp))
  }

  it should "return false for 1.0>Positive Infinity" in {
    val p = ParseArith()
    val cvp = MockCvp(Array(Column("x", "D", Double.PositiveInfinity), Column("y", "D", 1.0)), p)
    p.compileFilter("y>x")
    assert(!p.evalBooleanFunction(cvp))
  }

  it should "return true for 1.0>Negative Infinity" in {
    val p = ParseArith()
    val cvp = MockCvp(Array(Column("x", "D", Double.NegativeInfinity), Column("y", "D", 1.0)), p)
    p.compileFilter("y>x")
    assert(p.evalBooleanFunction(cvp))
  }

  it should "work for mixed numeric expression on the left" in {
    val p = ParseArith()
    val cvp = MockCvp(Array(Column("x", "I", 1)), p)
    p.compileFilter("x*0.01>1")
    assert(!p.evalBooleanFunction(cvp))
  }

  it should "work for mixed numeric expression on the right" in {
    val p = ParseArith()
    val cvp = MockCvp(Array(Column("x", "I", 1)), p)
    p.compileFilter("1>x*0.01")
    assert(p.evalBooleanFunction(cvp))
  }

  "Equals" should "return false for NaN=1.0" in {
    val p = ParseArith()
    val cvp = MockCvp(Array(Column("x", "D", Double.NaN), Column("y", "D", 1.0)), p)
    p.compileFilter("x=y")
    assert(!p.evalBooleanFunction(cvp))
  }

  it should "return false for 1.0=NaN" in {
    val p = ParseArith()
    val cvp = MockCvp(Array(Column("x", "D", Double.NaN), Column("y", "D", 1.0)), p)
    p.compileFilter("y=x")
    assert(!p.evalBooleanFunction(cvp))
  }

  "Function expecting Double" should "accept an Int variable" in {
    val p = ParseArith()
    val cvp = MockCvp(Array(Column("x", "I", 42)), p)

    p.compileCalculation("sin(x)")
  }

  it should "accept an Int value" in {
    val p = ParseArith()
    val cvp = MockCvp(Array(Column("x", "I", 42)), p)

    val result = p.compileCalculation("sin(42)")
    assert(result == "Double")
  }

  it should "accept a Long variable" in {
    val p = ParseArith()
    val cvp = MockCvp(Array(Column("x", "L", 42)), p)

    p.compileCalculation("sin(x)")
  }

  it should "accept a Long value" in {
    val p = ParseArith()

    val result = p.compileCalculation("sin(17179869184)")
    assert(result == "Double")
  }

  "Function expecting String" should "accept an Int variable" in {
    val p = ParseArith()
    val cvp = MockCvp(Array(Column("x", "I", 42)), p)

    val result = p.compileCalculation("upper(x)")
    assert(result == "String")
  }

  it should "not accept an Int value" in {
    val p = ParseArith()
    val thrown = intercept[GorParsingException] {
      p.compileCalculation("upper(42)")
    }
  }

  "Function expecting Double" should "report on wrong argument type when passed a string column" in {
    val p = ParseArith()
    val cvp = MockCvp(Array(Column("x", "S", "3.14")), p)

    val thrown = intercept[GorParsingException] {
      p.compileCalculation("log(#1)")
    }
  }

  it should "allow spaces before opening parentheses" in {
    val p = ParseArith()
    val result = p.compileCalculation("exp (1.0)")
    assert(result == "Double")
  }

  it should "allow spaces after opening parentheses" in {
    val p = ParseArith()
    val result = p.compileCalculation("exp( 1.0)")
    assert(result == "Double")
  }

  it should "allow spaces before closing parentheses" in {
    val p = ParseArith()
    val result = p.compileCalculation("exp(1.0 )")
    assert(result == "Double")
  }

  "IF" should "allow comparing double to NaN" in {
    assertConditionalExpression(Array(Column("x", "D", 3.14)), "x != NaN")
  }

  it should "not allow different types for true/false case when first is numeric" in {
    val p = ParseArith()
    val cvp = MockCvp(Array(Column("x", "D", 3.14)), p)

    val thrown = intercept[GorParsingException] {
      p.compileCalculation("if(x != 3.14, 1, 'false')")
    }
  }

  it should "not allow different types for true/false case when first is string" in {
    val p = ParseArith()
    val cvp = MockCvp(Array(Column("x", "D", 3.14)), p)

    val thrown = intercept[GorParsingException] {
      p.compileCalculation("if(x != 3.14, 'true', 0)")
    }
  }

  "Division" should "return float for float divided by float" in {
    val columns = Array(Column("x", "D", 3.14), Column("y", "D", 1.7))
    assertDoubleExpression(columns, "x/y", 3.14 / 1.7)
  }

  it should "return float for float divided by int" in {
    val columns = Array(Column("x", "D", 3.14), Column("y", "I", 4))
    assertDoubleExpression(columns, "x/y",3.14 / 4)
  }

  it should "return float for int divided by int" in {
    val columns = Array(Column("x", "I", 6), Column("y", "I", 3))
    assertDoubleExpression(columns, "x/y", 2.0)
  }

  it should "return float for int constant divided by int constant" in {
    assertDoubleExpression(Array(), "3/2", 1.5)
  }

  it should "return float for float constant divided by int constant" in {
    assertDoubleExpression(Array(), "3.4/2", 1.7)
  }

  it should "return float for int constant divided by float constant" in {
    assertDoubleExpression(Array(), "3/0.5", 6.0)
  }

  "Multiplication" should "return float for float multiplied by float" in {
    val columns = Array(Column("x", "D", 3.14), Column("y", "D", 1.7))
    assertDoubleExpression(columns, "x*y", 3.14 * 1.7)
  }

  it should "return float for float multiplied by int" in {
    val columns = Array(Column("x", "D", 3.14), Column("y", "I", 4))
    assertDoubleExpression(columns, "x*y", 3.14 * 4)
  }

  it should "return int for int multiplied by int" in {
    val columns = Array(Column("x", "I", 6), Column("y", "I", 3))
    assertIntExpression(columns, "x*y", 18)
  }

  it should "return int for int constant multiplied by int constant" in {
    assertIntExpression(Array(), "3*2", 6)
  }

  it should "return float for float constant multiplied by int constant" in {
    assertDoubleExpression(Array(), "3.4*2", 6.8)
  }

  it should "return float for int constant multiplied by float constant" in {
    assertDoubleExpression(Array(), "3*0.5", 1.5)
  }

  "Error reporting" should "suggest a column name if name is close to existing names when column is numeric" in {
    val p = ParseArith()
    val cvp = MockCvp(Array(Column("depth", "D", "3.14")), p)

    val expressionSource = "log(deth)"
    val thrown = intercept[GorParsingException] {
      p.compileCalculation(expressionSource)
    }
    assert(thrown.getMessage.contains("deth"))
    assert(thrown.getMessage.contains("depth"))
  }

  it should "suggest a column name if name is close to existing names when column is string" in {
    val p = ParseArith()
    val cvp = MockCvp(Array(Column("reference", "S", "a")), p)

    val expressionSource = "upper(refence)"
    val thrown = intercept[GorParsingException] {
      p.compileCalculation(expressionSource)
    }
    assert(thrown.getMessage.contains("refence"))
    assert(thrown.getMessage.contains("reference"))
  }

  "Comparison operator precedence" should "be higher than +" in {
    assertConditionalExpression(Array(Column("x", "I", 5), Column("y", "I", 4)), "x + 3 > y")
    assertConditionalExpression(Array(Column("x", "I", 10), Column("y", "I", 4)), "x > y+3")
    assertConditionalExpression(Array(Column("x", "D", 5.0), Column("y", "D", 4.0)), "x + 3.0 > y")
    assertConditionalExpression(Array(Column("x", "D", 10.0), Column("y", "D", 4.0)), "x > y+3.0")
    assertConditionalExpression(Array(Column("x", "S", "def"), Column("y", "S", "abc")), "x + 'def' > y")
    assertConditionalExpression(Array(Column("x", "S", "def"), Column("y", "S", "abc")), "x > y+'def'")
    assertConditionalExpression(Array(Column("x", "I", 1), Column("y", "I", 5)), "x + 3 < y")
    assertConditionalExpression(Array(Column("x", "I", 1), Column("y", "I", 5)), "x < y+3")
    assertConditionalExpression(Array(Column("x", "D", 1.0), Column("y", "D", 5.0)), "x + 3.0 < y")
    assertConditionalExpression(Array(Column("x", "D", 1.0), Column("y", "D", 5.0)), "x < y+3.0")
    assertConditionalExpression(Array(Column("x", "S", "abc"), Column("y", "S", "def")), "x + 'def' < y")
    assertConditionalExpression(Array(Column("x", "S", "abc"), Column("y", "S", "def")), "x < y+'def'")
  }

  it should "be higher than -" in {
    assertConditionalExpression(Array(Column("x", "I", 15), Column("y", "I", 4)), "x - 3 > y")
    assertConditionalExpression(Array(Column("x", "I", 10), Column("y", "I", 4)), "x > y-3")
    assertConditionalExpression(Array(Column("x", "D", 15.0), Column("y", "D", 4.0)), "x - 3.0 > y")
    assertConditionalExpression(Array(Column("x", "D", 10.0), Column("y", "D", 4.0)), "x > y-3.0")
    assertConditionalExpression(Array(Column("x", "I", 1), Column("y", "I", 5)), "x - 3 < y")
    assertConditionalExpression(Array(Column("x", "I", 1), Column("y", "I", 5)), "x < y-3")
    assertConditionalExpression(Array(Column("x", "D", 1.0), Column("y", "D", 5.0)), "x - 3.0 < y")
    assertConditionalExpression(Array(Column("x", "D", 1.0), Column("y", "D", 5.0)), "x < y-3.0")
  }

  it should "be higher than *" in {
    assertConditionalExpression(Array(Column("x", "I", 15), Column("y", "I", 4)), "x * 3 > y")
    assertConditionalExpression(Array(Column("x", "I", 50), Column("y", "I", 4)), "x > y*3")
    assertConditionalExpression(Array(Column("x", "D", 15.0), Column("y", "D", 4.0)), "x * 3.0 > y")
    assertConditionalExpression(Array(Column("x", "D", 50.0), Column("y", "D", 4.0)), "x > y*3.0")
    assertConditionalExpression(Array(Column("x", "I", 1), Column("y", "I", 5)), "x * 3 < y")
    assertConditionalExpression(Array(Column("x", "I", 1), Column("y", "I", 5)), "x < y*3")
    assertConditionalExpression(Array(Column("x", "D", 1.0), Column("y", "D", 5.0)), "x * 3.0 < y")
    assertConditionalExpression(Array(Column("x", "D", 1.0), Column("y", "D", 5.0)), "x < y*3.0")
  }

  "Adding parentheses" should "not change types" in {
    assertDoubleExpression(Array(Column("a", "I", 1), Column("b", "I", 2)), "if(1>2,float('NaN'),a/b)", 0.5)
    assertDoubleExpression(Array(Column("a", "I", 1), Column("b", "I", 2)), "if(1>2,float('NaN'),(a)/(b))", 0.5)
    assertDoubleExpression(Array(Column("a", "I", 1), Column("b", "I", 2)), "if(1>2,float('NaN'),((a)/(b)))", 0.5)
    assertDoubleExpression(Array(Column("a", "L", 1L), Column("b", "L", 2L)), "if(1>2,float('NaN'),a/b)", 0.5)
    assertDoubleExpression(Array(Column("a", "L", 1L), Column("b", "L", 2L)), "if(1>2,float('NaN'),(a)/(b))", 0.5)
    assertDoubleExpression(Array(Column("a", "L", 1L), Column("b", "L", 2L)), "if(1>2,float('NaN'),((a)/(b)))", 0.5)
    assertDoubleExpression(Array(Column("a", "I", 1), Column("b", "L", 2L)), "if(1>2,float('NaN'),a/b)", 0.5)
    assertDoubleExpression(Array(Column("a", "I", 1), Column("b", "L", 2L)), "if(1>2,float('NaN'),(a)/(b))", 0.5)
    assertDoubleExpression(Array(Column("a", "I", 1), Column("b", "L", 2L)), "if(1>2,float('NaN'),((a)/(b)))", 0.5)
    assertDoubleExpression(Array(Column("a", "L", 1L), Column("b", "I", 2)), "if(1>2,float('NaN'),a/b)", 0.5)
    assertDoubleExpression(Array(Column("a", "L", 1L), Column("b", "I", 2)), "if(1>2,float('NaN'),(a)/(b))", 0.5)
    assertDoubleExpression(Array(Column("a", "L", 1L), Column("b", "I", 2)), "if(1>2,float('NaN'),((a)/(b)))", 0.5)
  }

  "Column names with dot" should "work in calc" in {
    val p = ParseArith()
    val cvp = MockCvp(Array(Column("a.1", "I", 1)), p)
    p.compileCalculation("a.1 + 1")
    assert(p.evalIntFunction(cvp) == 2)
  }

  it should "work in filter" in {
    val p = ParseArith()
    val cvp = MockCvp(Array(Column("a.1", "I", 1)), p)
    p.compileFilter("a.1 > 0")
    assert(p.evalBooleanFunction(cvp))
  }

  private def assertConditionalExpression(columns: Array[Column], expr: String): Unit = {
    val p = ParseArith()

    val cvp = MockCvp(columns, p)
    val fullExpr = s"if($expr, 'true', 'false')"
    val cr = p.compileCalculation(fullExpr)
    assert(cr == "String")

    val result = p.evalStringFunction(cvp)
    assert(result == "true")
  }

  private def assertDoubleExpression(columns: Array[Column], expr: String, expected: Double): Unit = {
    val p = ParseArith()

    val cvp = MockCvp(columns, p)
    val cr = p.compileCalculation(expr)
    assert(cr == "Double")

    val result = p.evalDoubleFunction(cvp)
    assert(result == expected)
  }

  private def assertIntExpression(columns: Array[Column], expression: String, expectedValue: Int): Unit = {
    val p = ParseArith()

    val cvp = MockCvp(columns, p)

    val cr = p.compileCalculation(expression)
    assert(cr == "Int")

    val result = p.evalIntFunction(cvp)
    assert(result == expectedValue)
  }
}
