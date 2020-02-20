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

import Commands.CommandParseUtilities
import Commands.CommandParseUtilities._
import org.gorpipe.exceptions.GorParsingException
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.slf4j.{Logger, LoggerFactory}

@RunWith(classOf[JUnitRunner])
class UTestCommandParsingAnalysisUtilities extends FunSuite{

  private val log: Logger = LoggerFactory.getLogger(this.getClass)

  private val testArray = Array("-h", "-b", "foo", "-v", "bar", "tar", "-foobar")
  private val testArrayUpper = Array("-H", "-B", "foo", "-V", "bar", "tar", "-FooBar")

  test("Index of Option") {
    assert(indexOfOption(testArray, "-v") == 3, "Index of -v should be 3")
    assert(indexOfOption(testArray, "-h") == 0, "Index of -h should be 0")
    assert(indexOfOption(testArray, "-foobar") == 6, "Index -foobar should be 6")
    assert(indexOfOption(testArray, "-notthere") == -1, "Index -notthere should be -1")
    assert(indexOfOption(testArray, "-V") == -1, "Index in upper case not available")
  }

  test("Has Option") {
    assert(!hasOption(testArray, ""), "Empty options does not exist")
    assert(hasOption(testArray, "-v"), "Option -v exists")
    assert(!hasOption(testArray, "-V"), "Option -V exists")
    assert(!hasOption(testArray, "-h -v"), "Option -h -v does not exist")
    assert(hasOption(testArray, "-foobar"), "Option -foobar exists")
    assert(!hasOption(testArray, "-notthere"), "Option -notthere does not exist")
  }

  test("Has Option upper case") {
    assert(!hasOption(testArrayUpper, ""), "Empty options does not exist")
    assert(hasOption(testArrayUpper, "-V"), "Option -V exists")
    assert(!hasOption(testArrayUpper, "-v"), "Option -v exists")
    assert(!hasOption(testArrayUpper, "-H -V"), "Option -H -V does not exist")
    assert(hasOption(testArrayUpper, "-FooBar"), "Option -FooBar exists")
    assert(!hasOption(testArrayUpper, "-NOTTHERE"), "Option -notthere does not exist")
  }

  test("Get string value options") {
    var thrown = intercept[GorParsingException] {
      stringValueOfOption(testArray, "")
    }
    assert(thrown.getMessage == "Value option  is not found")

    thrown = intercept[GorParsingException] {
      stringValueOfOption(testArray, "-foobar")
    }
    assert(thrown.getMessage == "Value not found for option -foobar")

    thrown = intercept[GorParsingException] {
      stringValueOfOption(testArray, "-h")
    }
    assert(thrown.getMessage == "Value missing for option -h")

    assert(stringValueOfOption(testArray, "-b") == "foo", "Value for option -b is foo")
    assert(stringValueOfOption(testArray, "-v") == "bar", "Value for option -v is bar")

    thrown = intercept[GorParsingException] {
      stringValueOfOption(testArray, "-V")
    }
    assert(thrown.getMessage == "Value option -V is not found")
  }

  test("Get string value options upper case") {
    var thrown = intercept[GorParsingException] {
      stringValueOfOption(testArrayUpper, "")
    }
    assert(thrown.getMessage == "Value option  is not found")

    thrown = intercept[GorParsingException] {
      stringValueOfOption(testArrayUpper, "-FooBar")
    }
    assert(thrown.getMessage == "Value not found for option -FooBar")

    thrown = intercept[GorParsingException] {
      stringValueOfOption(testArrayUpper, "-H")
    }
    assert(thrown.getMessage == "Value missing for option -H")

    assert(stringValueOfOption(testArrayUpper, "-B") == "foo", "Value for option -B is foo")
    assert(stringValueOfOption(testArrayUpper, "-V") == "bar", "Value for option -V is bar")

    thrown = intercept[GorParsingException] {
      stringValueOfOption(testArrayUpper, "-v")
    }
    assert(thrown.getMessage == "Value option -v is not found")
  }

  test("Get string value options with ") {
    var thrown = intercept[GorParsingException] {
      stringValueOfOption(testArray, "")
    }
    assert(thrown.getMessage == "Value option  is not found")

    thrown = intercept[GorParsingException] {
      stringValueOfOption(testArray, "-foobar")
    }
    assert(thrown.getMessage == "Value not found for option -foobar")

    thrown = intercept[GorParsingException] {
      stringValueOfOption(testArray, "-h")
    }
    assert(thrown.getMessage == "Value missing for option -h")

    assert(stringValueOfOption(testArray, "-b") == "foo", "Value for option -b is foo")
    assert(stringValueOfOption(testArray, "-v") == "bar", "Value for option -v is bar")
  }

  test("Get string value options with validation") {
    val allowedOptionValues = Array("foo", "tar", "bara")

    assert(stringValueOfOptionWithErrorCheck(testArray, "-b", null) == "foo")
    assert(stringValueOfOptionWithErrorCheck(testArray, "-b", allowedOptionValues) == "foo")

    var thrown = intercept[GorParsingException] {
      stringValueOfOptionWithErrorCheck(testArray, "-h", allowedOptionValues)
    }
    assert(thrown.getMessage == "Value missing for option -h")

    thrown = intercept[GorParsingException] {
      stringValueOfOptionWithErrorCheck(testArray, "-v", allowedOptionValues)
    }
    assert(thrown != null)

    thrown = intercept[GorParsingException] {
      stringValueOfOptionWithErrorCheck(testArray, "-B", allowedOptionValues)
    }
    assert(thrown != null)
  }

  test("Get string value options in upper with validation") {
    val allowedOptionValues = Array("foo", "tar", "bara")

    assert(stringValueOfOptionWithErrorCheck(testArrayUpper, "-B", null) == "foo")
    assert(stringValueOfOptionWithErrorCheck(testArrayUpper, "-B", allowedOptionValues) == "foo")

    var thrown = intercept[GorParsingException] {
      stringValueOfOptionWithErrorCheck(testArrayUpper, "-H", allowedOptionValues)
    }
    assert(thrown.getMessage == "Value missing for option -H")

    thrown = intercept[GorParsingException] {
      stringValueOfOptionWithErrorCheck(testArrayUpper, "-V", allowedOptionValues)
    }
    assert(thrown != null)

    thrown = intercept[GorParsingException] {
      stringValueOfOptionWithErrorCheck(testArrayUpper, "-b", allowedOptionValues)
    }
    assert(thrown != null)
  }

  test("Get integer value with range check") {
    val testArrayInteger = Array("-a", "-10", "-b", "10", "-c", "foo", "-D", "100000", "-e", "-f")

    assert(intValueOfOptionWithRangeCheck(testArrayInteger, "-a") == -10)
    assert(intValueOfOptionWithRangeCheck(testArrayInteger, "-b") == 10)
    assert(intValueOfOptionWithRangeCheck(testArrayInteger, "-D") == 100000)
    var thrown = intercept[GorParsingException] {
      intValueOfOptionWithRangeCheck(testArrayInteger, "-c")
    }
    assert(thrown != null)

    thrown = intercept[GorParsingException] {
      intValueOfOptionWithRangeCheck(testArrayInteger, "-e")
    }
    assert(thrown != null)

    thrown = intercept[GorParsingException] {
      intValueOfOptionWithRangeCheck(testArrayInteger, "-f")
    }
    assert(thrown != null)

    thrown = intercept[GorParsingException] {
      intValueOfOptionWithRangeCheck(testArrayInteger, "-b", 100, 1000)
    }
    assert(thrown != null)

    thrown = intercept[GorParsingException] {
      intValueOfOptionWithRangeCheck(testArrayInteger, "-d", 0, 100)
    }
    assert(thrown != null)
  }

  test("Get double value with range check") {
    val testArrayDouble = Array("-a", "-10.0", "-b", "10.0", "-c", "foo", "-d", "100000", "-e", "-f")

    assert(doubleValueOfOptionWithRangeCheck(testArrayDouble, "-a") == -10)
    assert(doubleValueOfOptionWithRangeCheck(testArrayDouble, "-b") == 10)
    assert(doubleValueOfOptionWithRangeCheck(testArrayDouble, "-d") == 100000)
    var thrown = intercept[GorParsingException] {
      doubleValueOfOptionWithRangeCheck(testArrayDouble, "-c")
    }
    assert(thrown != null)

    thrown = intercept[GorParsingException] {
      doubleValueOfOptionWithRangeCheck(testArrayDouble, "-e")
    }
    assert(thrown != null)

    thrown = intercept[GorParsingException] {
      doubleValueOfOptionWithRangeCheck(testArrayDouble, "-f")
    }
    assert(thrown != null)

    thrown = intercept[GorParsingException] {
      doubleValueOfOptionWithRangeCheck(testArrayDouble, "-b", 100, 1000)
    }
    assert(thrown != null)

    thrown = intercept[GorParsingException] {
      doubleValueOfOptionWithRangeCheck(testArrayDouble, "-d", 0, 100)
    }
    assert(thrown != null)
  }

  test("Get column number from index or name for gor") {
    val tmp = Array("chrom","start","a","b","c","d","e").map(_.toUpperCase)
    val columns = tmp.zipWithIndex.toMap

    assert(columnNumber("CHROM", columns, tmp, false) == 1)
    assert(columnNumber("A", columns, tmp, false) == 3)
    assert(columnNumber("A[1]", columns, tmp, false) == 4)
    assert(columnNumber("1", columns, tmp, false) == 1)
    assert(columnNumber("FOO", columns, tmp, false, true) == 0)

    var thrown = intercept[GorParsingException] {
      columnNumber("FOO", columns, tmp, false)
    }
    assert(thrown != null)

    thrown = intercept[GorParsingException] {
      columnNumber("-1", columns, tmp, false)
    }
    assert(thrown != null)

    thrown = intercept[GorParsingException] {
      columnNumber("20", columns, tmp, false)
    }
    assert(thrown != null)
  }

  test("Get column number from index or name for nor") {
    val tmp = Array("chrom","start","a","b","c","d","e").map(_.toUpperCase)
    val columns = tmp.zipWithIndex.toMap

    assert(columnNumber("CHROM", columns, tmp, true) == 1)
    assert(columnNumber("A", columns, tmp, true) == 3)
    assert(columnNumber("A[1]", columns, tmp, true) == 4)
    assert(columnNumber("1", columns, tmp, true) == 3)
    assert(columnNumber("FOO", columns, tmp, true, true) == 0)

    var thrown = intercept[GorParsingException] {
      columnNumber("FOO", columns, tmp, true)
    }
    assert(thrown != null)

    thrown = intercept[GorParsingException] {
      columnNumber("-1", columns, tmp, true)
    }
    assert(thrown != null)

    thrown = intercept[GorParsingException] {
      columnNumber("20", columns, tmp, true)
    }
    assert(thrown != null)
  }

  test("Get column numbers from index or name for nor") {
    val header  = "chrom\tstart\ta\tb\tc\td\te"

    var columns = columnsFromHeader("a,b,e", header)
    assert(columns.length == 3)
    assert(columns.head == 2 && columns(1) == 3 && columns(2) == 6)

    columns = columnsFromHeader("a[1],b,e", header)
    assert(columns.length == 3)
    assert(columns.head == 3 && columns(1) == 3 && columns(2) == 6)

    columns = columnsFromHeader("1-3,e", header)
    assert(columns.length == 4)
    assert(columns.head == 0 && columns(1) == 1 && columns(2) == 2 && columns(3) == 6)

    columns = columnsFromHeader("1,5-", header)
    assert(columns.length == 4)
    assert(columns.head == 0 && columns(1) == 4 && columns(2) == 5 && columns(3) == 6)

    columns = columnsFromHeader("5-10", header, false, true)
    assert(columns.length == 6)
    assert(columns.head == 4 && columns(1) == 5 && columns(2) == 6 && columns(3) == 7 && columns(4) == 8 && columns(5) == 9)

    columns = columnsFromHeader("a,f,g", header, false, true)
    assert(columns.length == 1)
    assert(columns.head == 2)

    val header2 = "chrom\tpos\ta1\ta2\ta3"

    columns = columnsFromHeader("a*", header2)
    assert(columns.length == 3)
    assert(columns.head == 2 && columns(1) == 3 && columns(2) == 4)

    columns = columnsFromHeader("a*,1", header2)
    assert(columns.length == 4)
    assert(columns.head == 2 && columns(1) == 3 && columns(2) == 4 && columns(3) == 0)

    var thrown = intercept[GorParsingException] {
      columnsFromHeader("", header)
    }
    assert(thrown != null)

    thrown = intercept[GorParsingException] {
      columnsFromHeader("e[2]", header)
    }
    assert(thrown != null)

    thrown = intercept[GorParsingException] {
      columnsFromHeader("1-10", header)
    }
    assert(thrown != null)

    thrown = intercept[GorParsingException] {
      columnsFromHeader("*a,1", header2)
    }
    assert(thrown != null)

  }

  test("Get column number with validation") {
    val header  = "chrom\tstart\ta\tb\tc\td\te"

    var columns = columnsOfOptionWithValidation(Array("-gc", "a,b"), "-gc", header, false, -1, -1)
    assert(columns.length == 2)
    assert(columns.head == 2 && columns(1) == 3)

    columns = columnsOfOptionWithValidation(Array("-gc", "a,b"), "-gc", header, false, 0, 3)
    assert(columns.length == 2)
    assert(columns.head == 2 && columns(1) == 3)

    columns = columnsOfOptionWithValidation(Array("-gc", "a,b,q"), "-gc", header, false, 0, 3, true)
    assert(columns.length == 2)
    assert(columns.head == 2 && columns(1) == 3)

    columns = columnsOfOptionWithValidation(Array("-gc", "a,b"), "-gc", header, true, -1, -1)
    assert(columns.length == 2)
    assert(columns.head == 2 && columns(1) == 3)

    columns = columnsOfOptionWithValidation(Array("-gc", "a,b"), "-gc", header, true, 0, 3)
    assert(columns.length == 2)
    assert(columns.head == 2 && columns(1) == 3)

    var thrown = intercept[GorParsingException] {
      columnsOfOptionWithValidation(Array("-gc", "a,b"), "-gc", header, false, 3, -1)
    }
    assert(thrown != null)

    thrown = intercept[GorParsingException] {
      columnsOfOptionWithValidation(Array("-gc", "a,b,c,d"), "-gc", header, false, -1, 3)
    }
    assert(thrown != null)

    thrown = intercept[GorParsingException] {
      columnsOfOptionWithValidation(Array("-gc", "a,b,q"), "-gc", header)
    }
    assert(thrown != null)
  }
}
