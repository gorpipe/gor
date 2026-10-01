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
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{Callable, CountDownLatch, Executors, TimeUnit}
import gorsat.gorsatGorIterator.MapAndListUtilities
import gorsat.process.GenericSessionFactory
import org.gorpipe.gor.model.Row
import org.gorpipe.model.gor.iterators.LineIterator
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner

/**
 * Tests for the map/multimap loading and caching in MapAndListUtilities (ENGKNOW-3933).
 */
@RunWith(classOf[JUnitRunner])
class UTestMapAndListUtilities extends AnyFunSuite {

  private def file(lines: String*): File = {
    val file = File.createTempFile("UTestMapAndListUtilities", ".tsv")
    file.deleteOnExit()
    val writer = new PrintWriter(file)
    try lines.foreach(writer.println) finally writer.close()
    file
  }

  /** Line iterator over in memory lines that counts the lines read, optionally waiting before the first line. */
  private class CountingLineIterator(lines: Seq[String], counter: AtomicInteger, beforeFirst: () => Unit = () => ())
    extends LineIterator {
    private val it = lines.iterator
    private var first = true

    override def nextLine: String = {
      if (first) {
        first = false
        beforeFirst()
      }
      counter.incrementAndGet()
      it.next()
    }

    override def hasNext: Boolean = it.hasNext

    override def next(): Row = throw new UnsupportedOperationException

    override def close(): Unit = {}
  }

  test("Case insensitive and case sensitive map of the same file are cached separately") {
    val input = file("Abc\tv1")
    val session = new GenericSessionFactory().create()

    val sensitive = MapAndListUtilities.getSingleHashMap(input.getAbsolutePath, caseInsensitive = false, 1, Array(1),
      asSet = false, skipEmpty = false, session)
    val insensitive = MapAndListUtilities.getSingleHashMap(input.getAbsolutePath, caseInsensitive = true, 1, Array(1),
      asSet = false, skipEmpty = false, session)

    assert(sensitive.containsKey("Abc"))
    assert(insensitive.containsKey("ABC"))
  }

  test("Case insensitive and case sensitive multimap of the same file are cached separately") {
    val input = file("Abc\tv1")
    val session = new GenericSessionFactory().create()

    val sensitive = MapAndListUtilities.getMultiHashMap(input.getAbsolutePath, caseInsensitive = false, session)
    val insensitive = MapAndListUtilities.getMultiHashMap(input.getAbsolutePath, caseInsensitive = true, session)

    assert(sensitive.containsKey("Abc"))
    assert(insensitive.containsKey("ABC"))
  }

  test("Maps with different input and output columns do not share a cache entry") {
    // ic=1,oc=[23] and ic=12,oc=[3] used to produce the same cache key.
    val input = file((0 until 24).map(i => s"c$i").mkString("\t"))
    val session = new GenericSessionFactory().create()

    val first = MapAndListUtilities.getSingleHashMap(input.getAbsolutePath, caseInsensitive = false, 1, Array(23),
      asSet = false, skipEmpty = false, session)
    val second = MapAndListUtilities.getSingleHashMap(input.getAbsolutePath, caseInsensitive = false, 12, Array(3),
      asSet = false, skipEmpty = false, session)

    assert(first.get("c0") == "c23")
    assert(second.get((0 until 12).map(i => s"c$i").mkString("\t")) == "c3")
  }

  test("Maps with and without skipEmpty of the same file are cached separately") {
    val input = file("k1\ta", "k1\t")
    val session = new GenericSessionFactory().create()

    val keepEmpty = MapAndListUtilities.getSingleHashMap(input.getAbsolutePath, caseInsensitive = false, 1, Array(1),
      asSet = false, skipEmpty = false, session)
    val skipEmpty = MapAndListUtilities.getSingleHashMap(input.getAbsolutePath, caseInsensitive = false, 1, Array(1),
      asSet = false, skipEmpty = true, session)

    assert(keepEmpty.get("k1") == "a,")
    assert(skipEmpty.get("k1") == "a")
  }

  test("Concurrent loads of the same map in a session read the file once") {
    val lines = (1 to 1000).map(i => s"k$i\tv${i % 10}")
    val session = new GenericSessionFactory().create()
    val linesRead = new AtomicInteger()
    val threads = 4
    val allStarted = new CountDownLatch(threads)
    val executor = Executors.newFixedThreadPool(threads)
    try {
      val results = (1 to threads).map { _ =>
        executor.submit(new Callable[java.util.Map[String, String]] {
          override def call(): java.util.Map[String, String] = {
            allStarted.countDown()
            val iterator = new CountingLineIterator(lines, linesRead, () => Thread.sleep(200))
            MapAndListUtilities.getSingleHashMap("concurrent.tsv", iterator, caseInsensitive = false, 1, Array(1),
              asSet = false, skipEmpty = false, session)
          }
        })
      }.map(_.get(30, TimeUnit.SECONDS))

      assert(linesRead.get() == lines.size)
      results.foreach(map => assert(map eq results.head))
    } finally {
      executor.shutdownNow()
    }
  }

  test("Repeated map values share one string instance") {
    val input = file("k1\tsame", "k2\tsame", "k3\tother")
    val session = new GenericSessionFactory().create()

    val map = MapAndListUtilities.getSingleHashMap(input.getAbsolutePath, asSet = false, skipEmpty = false, session)

    assert(map.get("k1") == "same")
    assert(map.get("k1") eq map.get("k2"))
    assert(map.get("k3") == "other")
  }

  test("Repeated multimap values share one string instance and keep their order") {
    val input = file("k1\tsame", "k1\tother", "k2\tsame")
    val session = new GenericSessionFactory().create()

    val map = MapAndListUtilities.getMultiHashMap(input.getAbsolutePath, caseInsensitive = false, session)

    assert(map.get("k1").toSeq == Seq("same", "other"))
    assert(map.get("k2").toSeq == Seq("same"))
    assert(map.get("k1")(0) eq map.get("k2")(0))
  }
}
