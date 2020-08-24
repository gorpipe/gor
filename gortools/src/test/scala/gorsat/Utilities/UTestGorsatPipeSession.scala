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

package gorsat.Utilities

import java.io.{File, PrintWriter}

import gorsat.gorsatGorIterator.{MapAndListUtilities, MemoryMonitorUtil}
import gorsat.process.GenericSessionFactory
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.slf4j.LoggerFactory

import scala.util.Random

/**
 * Test GorsatPipeSession.
 */
@RunWith(classOf[JUnitRunner])
class UTestGorPipeSession extends FunSuite {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def fileGenerator(prefix: String, suffix: String, lines: Int, lineGenerator: Int => String): File = {
    val file: File = File.createTempFile(prefix, suffix)
    file.deleteOnExit()

    val writer = new PrintWriter(file)
    try {
      for (i <- 1 to lines) {
        writer.println(lineGenerator(i))
      }
    } finally {
      writer.close()
    }

    file
  }


  test("HashMap - NotOutOfMemory") {
    val seed = System.nanoTime()
    val random = new Random(seed)
    logger.info("UTestGorPipeSession - HashMap - NotOutOfMemory - Seed: {}", seed)
    val input: File = fileGenerator("TestHashMap", ".nor", 10000, _ => random.nextInt(25) + "\t" + random.nextInt(1000000))

    val originalMemoryMonitorMinFreeMem = MemoryMonitorUtil.memoryMonitorMinFreeMemMB
    MemoryMonitorUtil.memoryMonitorMinFreeMemMB = 100

    try {
      val gorPipeSession = new GenericSessionFactory().create()
      MapAndListUtilities.getSingleHashMap(input.getAbsolutePath, asSet = false, skipEmpty = false, gorPipeSession)
      MapAndListUtilities.getMultiHashMap(input.getAbsolutePath, caseInsensitive = false, gorPipeSession)

      // Success if no exception.
    } finally {
      MemoryMonitorUtil.memoryMonitorMinFreeMemMB = originalMemoryMonitorMinFreeMem
    }
  }

  test("HashMap - OutOfMemory") {
    val seed = System.nanoTime()
    val random = new Random(seed)
    logger.info("UTestGorPipeSession - HashMap - OutOfMemory - Seed: {}", seed)
    val input: File = fileGenerator("TestHashMap", ".nor", 10000, _ => random.nextInt(25) + "\t" + random.nextInt(1000000))

    val originalMemoryMonitorMinFreeMem = MemoryMonitorUtil.memoryMonitorMinFreeMemMB
    MemoryMonitorUtil.memoryMonitorMinFreeMemMB = 100000000;  // Force out of memory check to fail.

    try {
      val gorPipeSession = new GenericSessionFactory().create()

      var gotSingleError = false
      try {
        MapAndListUtilities.getSingleHashMap(input.getAbsolutePath, asSet = false, skipEmpty = false, gorPipeSession)
      } catch {
        case _: Exception => gotSingleError = true
      }
      assert(gotSingleError)

      var gotMultiError = false
      try {
        MapAndListUtilities.getMultiHashMap(input.getAbsolutePath, caseInsensitive = false, gorPipeSession)
      } catch {
        case _: Exception => gotMultiError = true
      }
      assert(gotMultiError)
    } finally {
      MemoryMonitorUtil.memoryMonitorMinFreeMemMB = originalMemoryMonitorMinFreeMem
    }
  }

}
