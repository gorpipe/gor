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

package gorsat.gorsatGorIterator

import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Collectors
import org.gorpipe.gor.model.{DriverBackedFileReader, FileReader}
import org.gorpipe.gor.session.GorSession
import org.gorpipe.model.gor.iterators.LineIterator

import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.IterableHasAsScala

object MapAndListUtilities {

  type set = java.util.Set[String]
  type singleHashMap =  java.util.Map[String,String] //scala.collection.mutable.HashMap[String, String]
  type multiHashMap = java.util.Map[String, Array[String]] //scala.collection.mutable.HashMap[String, List[String]]

  def exists(filename: String, reader: FileReader): Boolean = {

    if (filename == null) return false

    reader match {
      case dbfr: DriverBackedFileReader => dbfr.exists(filename)
      case _ => Files.exists( reader.toPath(filename) )
    }
  }

  def getSingleHashMap(filename: String, caseInsensitive: Boolean, ic: Int, oc: Array[Int],
                       asSet: Boolean, skipEmpty: Boolean, session: GorSession): singleHashMap = {
    if(!exists(filename, session.getProjectContext.getSystemFileReader)) return new java.util.HashMap[String, String]()

    getSingleHashMap(filename, FileLineIterator(filename, session.getProjectContext.getSystemFileReader), caseInsensitive,
      ic, oc, asSet, skipEmpty, session)
  }

  def getMultiHashMap(filename: String, caseInsensitive: Boolean, ic: Int, oc: Array[Int],
                      session: GorSession): multiHashMap = {
    if (!exists(filename, session.getProjectContext.getSystemFileReader)) return new java.util.HashMap[String, Array[String]]

    getMultiHashMap(filename, FileLineIterator(filename, session.getProjectContext.getSystemFileReader), caseInsensitive, ic, oc, session)
  }

  def getMultiHashMap(filename: String, iterator: LineIterator,
                      caseInsensitive: Boolean, session: GorSession): multiHashMap =
    getMultiHashMap(filename, iterator, caseInsensitive, 1, Array(1), session)

  def getSingleHashMap(filename: String, asSet: Boolean, skipEmpty: Boolean, session: GorSession): singleHashMap =
    getSingleHashMap(filename, caseInsensitive = false, 1, Array(1), asSet = asSet, skipEmpty = skipEmpty, session)

  def getMultiHashMap(filename: String, caseInsensitive: Boolean, session: GorSession): multiHashMap =
    getMultiHashMap(filename, caseInsensitive, 1, Array(1), session: GorSession)

  def getStringArray(filename: String, session: GorSession): Array[String] = {
    if (!exists(filename, session.getProjectContext.getSystemFileReader)) return Array.empty[String]

    getStringArray(filename, FileLineIterator(filename, session.getProjectContext.getSystemFileReader), session)
  }

  def getStringTraversable(filename: String, session: GorSession): Iterable[String] = {
    val reader = session.getProjectContext.getSystemFileReader.getReader(filename)
    try {
      val lines = reader.lines()
      val list = lines.collect(Collectors.toList[String])
      list.asScala
    } finally {
      reader.close()
    }
  }

  def getStringArray(filename: String, iterator: LineIterator, session: GorSession) : Array[String] = {
    val extFilename = "listmap" + filename
    syncGetStringArray(extFilename, session) match {
      case Some(aArray) =>
        iterator.close()
        aArray
      case None =>
        try {
          var theList : List[String] = Nil

          while (iterator.hasNext) {
            theList ::= iterator.nextLine
          }
          theList = theList.reverse
          val theArray = theList.toArray
          syncAddStringArray(extFilename, theArray, session)
          theArray
        } finally {
          iterator.close()
        }
    }
  }

  // Locks so that concurrent pipelines of a request, which share the session cache, load a given map only once.
  private val loadLocks = new ConcurrentHashMap[String, Object]()

  private def cacheKey(kind: String, filename: String, ic: Int, oc: Array[Int], asSet: Boolean,
                       caseInsensitive: Boolean): String =
    s"$kind|$filename|$ic|${oc.mkString(",")}|$asSet|$caseInsensitive"

  /**
   * Returns the cached value if present, otherwise loads it while holding a lock for the request and key, so
   * concurrent callers wait for the first load instead of each building their own copy.
   */
  private def loadOnce[T](extFilename: String, iterator: LineIterator, session: GorSession)
                         (cached: => Option[T])(load: => T): T = {
    cached match {
      case Some(value) =>
        iterator.close()
        value
      case None =>
        val lockKey = String.valueOf(session.getRequestId) + "|" + extFilename
        val lock = loadLocks.computeIfAbsent(lockKey, _ => new Object)
        try {
          lock.synchronized {
            cached match {
              case Some(value) =>
                iterator.close()
                value
              case None => load
            }
          }
        } finally {
          loadLocks.remove(lockKey, lock)
        }
    }
  }

  /**
   * Shares equal strings while one map is loaded, map value columns tend to repeat a lot. Bounded so that maps with
   * mostly unique values do not pay for a large pool.
   */
  private class StringPool(maxSize: Int = 100000) {
    private val pool = new java.util.HashMap[String, String]()

    def apply(s: String): String = {
      val existing = pool.get(s)
      if (existing != null) {
        existing
      } else {
        if (pool.size < maxSize) pool.put(s, s)
        s
      }
    }
  }

  def getSingleHashMap(filename: String, iterator: LineIterator, caseInsensitive: Boolean, ic: Int,
                       oc: Array[Int], asSet: Boolean, skipEmpty: Boolean, session: GorSession): singleHashMap =  {
    val extFilename = cacheKey("map", filename, ic, oc, asSet, caseInsensitive)
    val ocl = oc.length
    loadOnce(extFilename, iterator, session)(syncGetSingleHashMap(extFilename, session)) {
        try {
          val colMap = new java.util.HashMap[String, String]()
          val values = new StringPool()

          val mmu: MemoryMonitorUtil =  new MemoryMonitorUtil(MemoryMonitorUtil.basicOutOfMemoryHandler)

          while (iterator.hasNext) {
            val x = iterator.nextLine
            val cols = x.split("\t", -1)
            mmu.check("getSingleHashMap", mmu.lineNum, x)
            if (asSet) {
              val lookupString =
                if (caseInsensitive) cols.slice(0, 1.max(ic)).mkString("\t").toUpperCase
                else cols.slice(0, 1.max(ic)).mkString("\t")
              colMap.put(lookupString, "1")
            } else {
              //            if (cols.length >= ic + oc) colMap += (cols.slice(0,ic).mkString("\t") -> cols.slice(ic,ic+oc).mkString("\t"))
              if (cols.length >= ic + ocl) {
                val lookupString =
                  if (caseInsensitive) cols.slice(0, ic).mkString("\t").toUpperCase
                  else cols.slice(0, ic).mkString("\t")
                if (colMap.getOrDefault(lookupString,null) == null) {
                  colMap.put(lookupString, values(oc.tail.map(c => cols(c)).foldLeft(cols(oc.head))(_ + "\t" + _)))
                } else {
                  val existingValues = colMap.get(lookupString).split("\t",-1)
                  val newValues = if( skipEmpty ) existingValues.zip(oc.map(c => cols(c))).map(_.productIterator.filter(_.toString.nonEmpty).mkString(",")) else existingValues.zip(oc.map(c => cols(c))).map(x => x._1 + "," + x._2 )
                  colMap.put(lookupString, values(newValues.tail.foldLeft(newValues.head)(_ + "\t" + _)))
                }
              }
            }
          }
          syncAddSingleHashMap(extFilename, colMap, session)
          colMap
        } finally {
          iterator.close()
        }
    }
  }

  def getMultiHashMap(filename: String, iterator: LineIterator, caseInsensitive: Boolean, ic: Int,
                      oc: Array[Int], session: GorSession): multiHashMap = {
    val extFilename = cacheKey("multimap", filename, ic, oc, asSet = false, caseInsensitive)
    val ocl = oc.length
    loadOnce(extFilename, iterator, session)(syncGetMultiHashMap(extFilename, session)) {
        val multiMap = new java.util.HashMap[String, ListBuffer[String]]()
        try {
          val values = new StringPool()
          val mmu: MemoryMonitorUtil = new MemoryMonitorUtil(MemoryMonitorUtil.basicOutOfMemoryHandler)
          while (iterator.hasNext) {
            val x = iterator.nextLine
            val cols = x.split("\t", -1)
            mmu.check("getMultiHashMap", mmu.lineNum, x)
            if (cols.length >= ic + ocl) {
              val (a, b) = (cols.slice(0, ic).mkString("\t"), values(oc.tail.map(c => cols(c)).foldLeft(cols(oc.head))(_ + "\t" + _)))
              val cisa = if (caseInsensitive) a.toUpperCase else a
              if (multiMap.containsKey(cisa)) {
                multiMap.put(cisa, multiMap.get(cisa) += b)
              } else {
                val buffer = ListBuffer(b)
                multiMap.put(cisa, buffer)
              }
            }
          }
          // Move the entries over rather than copying them, so both maps are not fully held at the same time.
          val multiOutputMap = new java.util.HashMap[String, Array[String]]((multiMap.size / 0.75f).toInt + 1)
          val entries = multiMap.entrySet().iterator()
          while (entries.hasNext) {
            val entry = entries.next()
            multiOutputMap.put(entry.getKey, entry.getValue.toArray)
            entries.remove()
          }
          syncAddMultiHashMap(extFilename, multiOutputMap, session)
          multiOutputMap
        } finally {
          iterator.close()
        }
    }
  }

  def syncGetSet(extFilename: String, session: GorSession): Option[set] = {
    session.getCache.getSets.synchronized {
      Option(session.getCache.getSets.getOrDefault(extFilename, null))
    }
  }

  def syncAddSet(extFilename: String, colMap: set, session: GorSession) : Unit = {
    session.getCache.getSets.synchronized {
      Option(session.getCache.getSets.getOrDefault(extFilename, null)) match {
        case Some(_) => /* do nothing */
        case None =>
          session.getCache.getSets.put(extFilename, colMap)
      }
    }
  }

  def syncGetSingleHashMap(extFilename: String, session: GorSession): Option[singleHashMap] = {
    session.getCache.getSingleHashMaps.synchronized {
      Option(session.getCache.getSingleHashMaps.getOrDefault(extFilename, null))
    }
  }

  def syncAddSingleHashMap(extFilename: String, colMap: singleHashMap, session: GorSession) : Unit = {
    session.getCache.getSingleHashMaps.synchronized {
      Option(session.getCache.getSingleHashMaps.getOrDefault(extFilename, null)) match {
        case Some(_) => /* do nothing */
        case None =>
          session.getCache.getSingleHashMaps.put(extFilename, colMap)
      }
    }
  }

  def syncGetMultiHashMap(extFilename: String, session: GorSession): Option[multiHashMap] = {
    session.getCache.getMultiHashMaps.synchronized {
      Option(session.getCache.getMultiHashMaps.getOrDefault(extFilename, null))
    }
  }

  def syncAddMultiHashMap(extFilename: String, colMap: multiHashMap, session: GorSession) : Unit = synchronized {
    session.getCache.getMultiHashMaps.synchronized {
      Option(session.getCache.getMultiHashMaps.getOrDefault(extFilename, null)) match {
        case Some(_) => /* do nothing */
        case None =>
          session.getCache.getMultiHashMaps.put(extFilename, colMap)
      }
    }
  }

  def syncGetStringArray(extFilename: String, session: GorSession): Option[Array[String]] = synchronized {
    session.getCache.getListMaps.synchronized {
      Option(session.getCache.getListMaps.getOrDefault(extFilename, null))
    }
  }

  def syncAddStringArray(extFilename: String, theArray: Array[String], session: GorSession) : Unit = synchronized {
    session.getCache.getListMaps.synchronized {
      Option(session.getCache.getListMaps.getOrDefault(extFilename, null)) match {
        case Some(_) => /* do nothing */
        case None =>
          session.getCache.getListMaps.put(extFilename, theArray)
      }
    }
  }

  def readArray(fileName: String, fileReader: FileReader): Array[String] = {
    fileReader.readAll(fileName)
  }
}
