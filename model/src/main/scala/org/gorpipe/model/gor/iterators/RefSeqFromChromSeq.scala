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

package org.gorpipe.model.gor.iterators

import java.io.IOException
import java.util
import java.util.Optional
import org.gorpipe.exceptions.GorResourceException
import org.gorpipe.gor.driver.adapters.StreamSourceRacFile
import org.gorpipe.gor.driver.meta.DataType
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource
import org.gorpipe.gor.model.{DriverBackedFileReader, FileReader, RacFile}
import org.gorpipe.gor.reference.FolderMigrator
import org.gorpipe.gor.util.DataUtil
import org.slf4j.{Logger, LoggerFactory}

import java.nio.file.{Files, Path, Paths}

object RefSeqFromChromSeq {
   var downloadTriggered : util.Map[String, java.lang.Boolean] =
    new java.util.concurrent.ConcurrentHashMap[String, java.lang.Boolean]
}

// IMPORTANT NOTE ON THREAD SAFETY:
// This class is NOT thread safe. It is expected that each thread that needs to access reference sequence data
// will have its own instance of this class.
// 1.  The LUFO cache is per se thread safe but it is very small so if used by many threads it will cause a lot
//     of thrasing and bad performance.
// 2. The use of the filemap is NOT thread safe and is very likely to cause incorrect reads of the cache data.

class RefSeqFromChromSeq(ipath : String, fileReader : FileReader) extends RefSeq {
  private val GOR_REFSEQ_CACHE_FOLDER = System.getProperty("gor.refseq.cache.folder")
  private val GOR_REFSEQ_CACHE_DOWNLOAD = Option(System.getProperty("gor.refseq.cache.download", "true")).exists(_.toBoolean)

  private val log: Logger = LoggerFactory.getLogger(RefSeqFromChromSeq.this.getClass)

  lazy val path: String = getBuildPath(ipath)
  val buffLength = 10000
  val lufo = new LUFO[Array[Byte]](10)
  // Keep a LUFO cache with 10 last used buffers
  var lastKey: String = ""
  var lastBuff: Array[Byte] = _
  var noReferenceBuildFound = false
  val filemap = new util.HashMap[String, Optional[RacFile]]
  val notfoundmap = new util.HashSet[String]

  override def close(): Unit = {
    filemap.entrySet().stream().forEach( f => f.getValue.ifPresent(f => f.close()) )
    filemap.clear()
  }

  def getBuildPath(iRefPath: String): String = {
    val refPath = iRefPath.replace("""\""", "/")
    if (GOR_REFSEQ_CACHE_FOLDER != null && !GOR_REFSEQ_CACHE_FOLDER.isEmpty) {
      val fullRefPath = fileReader.toAbsolutePath(refPath)
      val fullCachePath: Path = getFullCachePath(fullRefPath)
      if (Files.exists(fullCachePath)) {
        log.debug("Using cached reference build {}", fullCachePath.toString)
        return fullCachePath.toString
      } else if (GOR_REFSEQ_CACHE_DOWNLOAD && (RefSeqFromChromSeq.downloadTriggered.putIfAbsent(fullRefPath.toString, true) == null)) {
        // Only trigger download once per client
        triggerRefSeqDownload(fullRefPath, fullCachePath)
      }
    }
    refPath
  }

  def getFullCachePath(fullRefPath: Path) = {
    val realRefPath = fullRefPath.toRealPath()
    val partialRefPath = realRefPath.getParent.getFileName.resolve(realRefPath.getFileName)
    // To make sure we don't mix up different reference builds we use the full real path as cache sub-path.
    val fullCachePath = Paths.get(GOR_REFSEQ_CACHE_FOLDER).resolve(partialRefPath).normalize()
    fullCachePath
  }

  private def triggerRefSeqDownload(orgPath: Path, cachePath: Path) = {
    val refseqDownloaderThread = new Thread(new Runnable {
      override def run(): Unit = {
        try {
          log.info("Start downloading reference build {} to {}", orgPath, cachePath)
          FolderMigrator.migrate(orgPath, cachePath)
          log.info("Done (or already in progress) downloading reference build {} to {}", orgPath, cachePath)
        } catch {
            case e: Exception =>
              log.error("Error downloading reference build {} to {} - {}", orgPath, cachePath, e.getMessage, e)
          }
      }
    })

    refseqDownloaderThread.setName("RefSeqDownloader")
    refseqDownloaderThread.setDaemon(false)
    refseqDownloaderThread.start()
  }

  def getBase(chr: String, pos: Int): Char = {
    // Positions below 1 are outside the chromosome, same as positions past its end.
    if (pos < 1) return 'N'
    val (buffKey, offset) = getKeyAndOffset(chr, pos)
    val buff = getBuffer(chr, pos, buffKey, offset)
    if (buff == null) 'N' else refByteToChar(buff(pos - offset - 1))
  }

  def getBases(chr: String, pos1: Int, pos2: Int): String = {
    // Positions below 1 are outside the chromosome and read as 'N', same as positions past its end.
    // Handle them before the buffer lookup: (pos - 1) / buffLength truncates toward zero, so pos 0 maps to buffer 0.
    if (pos1 < 1) {
      val leading = "N" * (math.min(pos2, 0) - pos1 + 1)
      return if (pos2 < 1) leading else leading + getBases(chr, 1, pos2)
    }
    if (pos1 == pos2) return getBase(chr, pos1).toString
    val strbuff = new StringBuilder(pos2 - pos1 + 1)
    var start = pos1
    // Load each buffer the range touches once. A buffer that can not be loaded reads as 'N' for its whole part.
    while (start <= pos2) {
      val (buffKey, offset) = getKeyAndOffset(chr, start)
      val end = math.min(pos2, offset + buffLength)
      val buff = getBuffer(chr, start, buffKey, offset)
      if (buff == null) {
        strbuff.append("N" * (end - start + 1))
      } else {
        var i = start
        while (i <= end) {
          strbuff.append(refByteToChar(buff(i - offset - 1)))
          i += 1
        }
      }
      start = end + 1
    }
    strbuff.toString
  }

  /**
    * Get the buffer for buffKey, from lastBuff, the LUFO cache or the reference file.
    * lastKey/lastBuff are only set when a buffer is returned, so lastBuff always belongs to lastKey.
    * @return the buffer, or null if the contig has no sequence in the build or the read failed.
    */
  private def getBuffer(chr: String, pos: Int, buffKey: String, offset: Int): Array[Byte] = {
    if (noReferenceBuildFound) return null
    if (buffKey == lastKey) return lastBuff
    try {
      val buff = lufo.getObject(buffKey) match {
        case Some(buffer) => buffer
        case None => readBuffer(chr, pos, buffKey, offset)
      }
      if (buff != null) {
        lastKey = buffKey
        lastBuff = buff
      }
      buff
    } catch {
      case ioex: IOException =>
        throw new GorResourceException("Reference build " + path + " inaccessible", path, ioex)
      case ex: Exception =>
        log.warn(String.format("Returning 'N' for reference build %s (%s:%d)", path, chr, pos), ex)
        null
    }
  }

  private def readBuffer(chr: String, pos: Int, buffKey: String, offset: Int): Array[Byte] = {
    val chrFilePath = DataUtil.toFile(path + "/" + chr, DataType.TXT)
    val f = if( filemap.containsKey(chrFilePath) ) filemap.get(chrFilePath) else {
      val cf = Optional.ofNullable(fileReader match {
        case dbfr: DriverBackedFileReader =>
          val ds = dbfr.unsecure().resolveUrl(chrFilePath)
          if (ds.exists()) new StreamSourceRacFile(ds.asInstanceOf[StreamSource]) else null
        case _ =>
          fileReader.openFile(chrFilePath)
      })
      filemap.put(chrFilePath, cf)
      cf
    }
    if( !f.isPresent ) {
      if (!notfoundmap.contains(chrFilePath)) {
        notfoundmap.add(chrFilePath)
        log.warn("Reference build " + path + "\n\nReference file "+chrFilePath+" does not exist", chrFilePath)
      }
      return null
    }
    val buff = new Array[Byte](buffLength)
    f.get().seek(offset)
    val l = f.get().read(buff, 0, buffLength)
    lufo.addObject(buffKey, buff)
    if( l == -1 ) {
      // Past the end of the chromosome, the buffer stays zero and reads as 'N'.
      log.warn("Trying to read "+chr+":"+pos+" from reference file " + chrFilePath + " of length "+f.get.length()+" from offset " + offset)
    }
    buff
  }

  /**
    * Convert reference byte to reference char.
    * @param b  byte to convert.
    * @return character
    */
  private def refByteToChar(b: Byte) : Char = {
    if (b != 0) b.toChar else 'N'
  }

  private def getKeyAndOffset(chr: String, pos: Int): (String, Int) = {
    (chr + "-" + ((pos - 1) / buffLength), ((pos - 1) / buffLength) * buffLength)
  }

  class LUFO[T](maxSize : Int) {

    case class LufoTuple(var obj : T, var counter : Long)

    var keyMap = Map.empty[String,LufoTuple]
    var timeMap = new scala.collection.immutable.TreeMap[Long, String]
    var timeCounter : Long = 0

    def getObject(key : String) : Option[T] = {
      if (keyMap.contains(key)) {
        val oldTime = keyMap(key).counter
        keyMap(key).counter = timeCounter
        timeMap -= oldTime
        timeMap = timeMap.updated(timeCounter,key)
        timeCounter += 1
        Some(keyMap(key).obj)
      } else None
    }

    def addObject(key : String, obj : T): Unit = {
      keyMap += (key -> LufoTuple(obj,timeCounter))
      timeMap = timeMap.updated(timeCounter,key)
      timeCounter+=1
      if (keyMap.size>maxSize) {
        val oldestTime = timeMap.firstKey
        val oldestKey = timeMap(oldestTime)
        keyMap -= oldestKey
        timeMap -= oldestTime
      }
    }
  }
}
