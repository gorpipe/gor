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
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource
import org.gorpipe.model.genome.files.gor.{DriverBackedFileReader, FileReader, RacFile}
import org.slf4j.{Logger, LoggerFactory}

class RefSeqFromConfig(ipath : String, fileReader : FileReader) extends RefSeq {
  private val log: Logger = LoggerFactory.getLogger(RefSeqFromConfig.this.getClass)

  val path: String = ipath.replace( """\""", "/")
  val buffLength = 10000
  val lufo = new LUFO[Array[Byte]](10)
  // Keep a LUFO cache with 10 last used buffers
  var lastKey: String = ""
  var lastBuff: Array[Byte] = _
  var noReferenceBuildFound = false
  var filemap = new util.HashMap[String,Optional[RacFile]]

  override def close(): Unit = {
    filemap.entrySet().stream().forEach( f => f.getValue.ifPresent(f => f.close()) )
    filemap.clear()
  }

  override def finalize(): Unit = {
    try {
      close()
    } finally {
      super.finalize()
    }
  }

  def getBase(chr: String, pos: Int): Char = {
    if (noReferenceBuildFound) return 'N'
    try {
      val (buffKey, offset) = getKeyAndOffset(chr, pos)

      if (buffKey == lastKey) return refByteToChar(lastBuff(pos - offset - 1))
      lufo.getObject(buffKey) match {
        case Some(buffer) =>
          lastKey = buffKey
          lastBuff = buffer
          refByteToChar(buffer(pos - offset - 1))
        case None =>
          val chrFilePath =path + "/" + chr + ".txt"
          val f = if( filemap.containsKey(chrFilePath) ) filemap.get(chrFilePath) else {
            val cf = Optional.ofNullable(fileReader match {
              case dbfr: DriverBackedFileReader =>
                val ds = dbfr.resolveUrl(chrFilePath)
                if (ds.exists()) new StreamSourceRacFile(ds.asInstanceOf[StreamSource]) else null
              case _ =>
                fileReader.openFile(chrFilePath)
            })
            filemap.put(chrFilePath, cf)
            cf
          }
          if( !f.isPresent ) {
            throw new GorResourceException("Reference file "+chrFilePath+" does not exist", chrFilePath)
          } else {
            val buff = new Array[Byte](buffLength)
            f.get().seek(offset)
            val l = f.get().read(buff, 0, buffLength)
            if( l == -1 ){
              log.warn("Trying to read "+chr+":"+pos+" from reference file " + chrFilePath + " of length "+f.get.length()+" from offset " + offset)
              return 'N'
            }
            lufo.addObject(buffKey, buff)
            refByteToChar(buff(pos - offset - 1))
          }
      }
    } catch {
      case ioex: IOException =>
        throw new GorResourceException("Reference build " + path + " inaccessible", path, ioex)
      case ex: Exception => {
        noReferenceBuildFound = true
        log.debug("Warning: Reference build " + path + "\n\n"+ex.getMessage)
      }
        'N'
    }
  }

  def getBases(chr: String, pos1: Int, pos2: Int): String = {
    if (pos1 == pos2) return getBase(chr, pos1).toString
    if ((pos1 - 1) / buffLength == (pos2 - 1) / buffLength) {
      val (buffKey, offset) = getKeyAndOffset(chr, pos1)

      if (buffKey != lastKey) {
        val temp = getBase(chr, pos1)
        val temp2 = getBase(chr, pos1)
      }
      val strbuff = new StringBuilder(pos2 - pos1 + 1)
      var i = pos1
      while (i <= pos2) {
        strbuff.append(refByteToChar(lastBuff(i - offset - 1)))
        i += 1
      }
      return strbuff.toString
      // return lastBuff.slice(pos1-offset-1,pos2-offset-1).toString
    }
    val strbuff = new StringBuilder(pos2 - pos1 + 1)
    var i = pos1
    while (i <= pos2) {
      strbuff.append(getBase(chr, i))
      i += 1
    }
    strbuff.toString
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
        timeMap = timeMap.insert(timeCounter,key)
        timeCounter += 1
        Some(keyMap(key).obj)
      } else None
    }

    def addObject(key : String, obj : T): Unit = {
      keyMap += (key -> LufoTuple(obj,timeCounter))
      timeMap = timeMap.insert(timeCounter,key)
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
