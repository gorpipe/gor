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
import java.util.concurrent.{Callable, CountDownLatch, ExecutionException, ExecutorService, Executors, Future, TimeUnit}
import gorsat.gorsatGorIterator.MapAndListUtilities
import gorsat.process.GenericSessionFactory
import org.gorpipe.gor.model.Row
import org.gorpipe.gor.session.{GorSession, GorSessionCache}
import org.gorpipe.model.gor.iterators.LineIterator
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner

import scala.util.Try

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

  private def async[T](executor: ExecutorService)(body: => T): Future[T] =
    executor.submit(new Callable[T] {
      override def call(): T = body
    })

  private def loadMap(name: String, iterator: LineIterator, session: GorSession): java.util.Map[String, String] =
    MapAndListUtilities.getSingleHashMap(name, iterator, caseInsensitive = false, 1, Array(1), asSet = false,
      skipEmpty = false, session)

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
            allStarted.await()
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

  test("A caller waiting for another load of the same map can be interrupted") {
    val session = new GenericSessionFactory().create()
    val loading = new CountDownLatch(1)
    val release = new CountDownLatch(1)
    val waiterDone = new CountDownLatch(1)
    val executor = Executors.newFixedThreadPool(2)
    try {
      val loader = async(executor) {
        loadMap("interrupt.tsv", new CountingLineIterator(Seq("k\tv"), new AtomicInteger(), () => {
          loading.countDown()
          release.await()
        }), session)
      }
      loading.await()
      val waiter = async(executor) {
        try loadMap("interrupt.tsv", new CountingLineIterator(Seq("k\tv"), new AtomicInteger()), session)
        finally waiterDone.countDown()
      }
      Thread.sleep(200)
      waiter.cancel(true)

      assert(waiterDone.await(5, TimeUnit.SECONDS), "waiter did not stop when interrupted")
      release.countDown()
      assert(loader.get(30, TimeUnit.SECONDS).get("k") == "v")
    } finally {
      release.countDown()
      executor.shutdownNow()
    }
  }

  test("A failed load fails the callers waiting for it instead of each of them retrying") {
    val session = new GenericSessionFactory().create()
    val loads = new AtomicInteger()
    val threads = 4
    val allStarted = new CountDownLatch(threads)
    val executor = Executors.newFixedThreadPool(threads)
    try {
      val results = (1 to threads).map { _ =>
        async(executor) {
          allStarted.countDown()
          allStarted.await()
          loadMap("failing.tsv", new CountingLineIterator(Seq("k\tv"), new AtomicInteger(), () => {
            loads.incrementAndGet()
            Thread.sleep(200)
            throw new IllegalStateException("load failed")
          }), session)
        }
      }.map(future => Try(future.get(30, TimeUnit.SECONDS)))

      assert(loads.get() == 1)
      results.foreach { result =>
        val cause = result.failed.get.asInstanceOf[ExecutionException].getCause
        assert(cause.getMessage == "load failed")
      }

      // A later call loads again.
      val map = loadMap("failing.tsv", new CountingLineIterator(Seq("k\tv"), new AtomicInteger()), session)
      assert(map.get("k") == "v")
    } finally {
      executor.shutdownNow()
    }
  }

  test("Sessions with the same request id but separate caches do not wait for each other") {
    val first = new GenericSessionFactory().create()
    val second = new GorSession(first.getRequestId)
    second.init(first.getProjectContext, first.getSystemContext, new GorSessionCache())
    val loading = new CountDownLatch(1)
    val release = new CountDownLatch(1)
    val executor = Executors.newFixedThreadPool(2)
    try {
      val firstLoad = async(executor) {
        loadMap("shared-request.tsv", new CountingLineIterator(Seq("k\tv1"), new AtomicInteger(), () => {
          loading.countDown()
          release.await()
        }), first)
      }
      loading.await()
      val secondLoad = async(executor) {
        loadMap("shared-request.tsv", new CountingLineIterator(Seq("k\tv2"), new AtomicInteger()), second)
      }

      assert(secondLoad.get(5, TimeUnit.SECONDS).get("k") == "v2")
      release.countDown()
      assert(firstLoad.get(30, TimeUnit.SECONDS).get("k") == "v1")
    } finally {
      release.countDown()
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
