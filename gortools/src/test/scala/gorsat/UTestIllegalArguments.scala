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

package gorsat

import Commands.CommandParseUtilities._
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.slf4j.{Logger, LoggerFactory}

@RunWith(classOf[JUnitRunner])
class UTestIllegalArguments extends FunSuite{

  private val log: Logger = LoggerFactory.getLogger(this.getClass)
  test("No options") {
    val (arguments, illegalArguments) = validateInputArguments(Array(""), "", "")

    assert(arguments.length == 0, "Number of arguments should be 0")
    assert(illegalArguments.length == 0, "illegal arguments should be empty")
  }

  test("With options, no input") {
    val (arguments, illegalArguments) = validateInputArguments(Array(""), "-a -b", "-c -d")

    assert(arguments.length == 0, "Number of arguments should be 0")
    assert(illegalArguments.length == 0, "illegal arguments should be empty")
  }

  test("With options, matching no value input") {
    val (arguments, illegalArguments) = validateInputArguments(Array("-a", "-b"), "-a -b", "-c -d")

    assert(arguments.length == 0, "Number of arguments should be 0")
    assert(illegalArguments.length == 0, "illegal arguments should be empty")
  }

  test("With options, matching value input") {
    val (arguments, illegalArguments) = validateInputArguments(Array("-c", "p1", "-d", "p2"), "-a -b", "-c -d")

    assert(arguments.length == 0, "Number of arguments should be 0")
    assert(illegalArguments.length == 0, "illegal arguments should be empty")
  }

  test("With options, matching value input with missing values") {
    val (arguments, illegalArguments) = validateInputArguments(Array("-c", "p1", "-d"), "-a -b", "-c -d")

    assert(arguments.length == 0, "Number of arguments should be 0")
    assert(illegalArguments.length == 1, "illegal arguments = " + illegalArguments.mkString(","))
  }

  test("With options, invalid input options") {
    val (arguments, illegalArguments) = validateInputArguments(Array("-c", "-d"), "-a -b", "-c -d")

    // This should be an issue as there are
    assert(arguments.length == 0, "Number of arguments should be 0")
    assert(illegalArguments.length == 2, "illegal arguments = " + illegalArguments.mkString(","))
  }

  test("With options, invalid input value options with overlapping options") {
    val (arguments, illegalArguments) = validateInputArguments(Array("-a", "p1", "-b", "p2"), "-a -b", "-c -d")

    // Why is this being parsed as option -a and -b with 2 arguments
    assert(arguments.length == 2, "Number of arguments should be 0")
    assert(illegalArguments.length == 0, "illegal arguments = " + illegalArguments.mkString(","))
  }

  test("With options, invalid input value options with discrete options") {
    val (arguments, illegalArguments) = validateInputArguments(Array("-e", "p1", "-f", "p2"), "-a -b", "-c -d")

    // Why is this being parsed as option -a and -b with 2 arguments
    assert(arguments.length == 2, "Number of arguments should be 0")
    assert(illegalArguments.length == 2, "illegal arguments = " + illegalArguments.mkString(","))
  }

  test("Test options with min number of input arguments succeeding") {
    val (arguments, illegalArguments) = validateInputArguments(Array("p1", "p2"), "", "", 2)

    // Why is this being parsed as option -a and -b with 2 arguments
    assert(illegalArguments.length == 0, "illegal arguments = " + illegalArguments.mkString(","))
    assert(arguments.length == 2, "Number of arguments should be 2")
  }

  test("Test options with min number of input arguments failing") {
    val (arguments, illegalArguments) = validateInputArguments(Array("p1"), "", "", 2)

    // Why is this being parsed as option -a and -b with 2 arguments
    assert(illegalArguments.length == 1, "illegal arguments = " + illegalArguments.mkString(","))
    assert(arguments.length == 1, "Number of arguments should be 1")
  }

  test("Test options with max number of input arguments succeeding") {
    val (arguments, illegalArguments) = validateInputArguments(Array("p1", "p2"), "", "", -1, 2)

    // Why is this being parsed as option -a and -b with 2 arguments
    assert(illegalArguments.length == 0, "illegal arguments = " + illegalArguments.mkString(","))
    assert(arguments.length == 2, "Number of arguments should be 2")
  }

  test("Test options with max number of input arguments failing") {
    val (arguments, illegalArguments) = validateInputArguments(Array("p1", "p2", "p3", "p4"), "", "", -1, 2)

    // Why is this being parsed as option -a and -b with 2 arguments
    assert(illegalArguments.length == 2, "illegal arguments = " + illegalArguments.mkString(","))
    assert(arguments.length == 4, "Number of arguments should be 4")
  }

  test("Speed test 2") {
    var t1 = System.nanoTime()
    validateInputArguments(Array("-a", "-b", "-c", "p1"), "-a -b", "-c")
    validateInputArguments(Array("-a", "-b", "-c", "p1", "-d", "-e", "foo", "bar"), "-a -b", "-c")

    val totalTime = (System.nanoTime() - t1) / 1000000.0
  }

  test("Speed test 1") {
    var t1 = System.nanoTime()
    validateInputArguments(Array("-a", "-b", "-c", "p1"), "-a -b", "-c", -1)
    validateInputArguments(Array("-a", "-b", "-c", "p1", "-d", "-e", "foo", "bar"), "-a -b", "-c", -1)

    val totalTime = (System.nanoTime() - t1) / 1000000.0
  }
}
