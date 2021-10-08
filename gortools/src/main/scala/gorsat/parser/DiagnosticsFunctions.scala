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

import java.lang.management.ManagementFactory
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Date
import com.sun.management.{OperatingSystemMXBean, UnixOperatingSystemMXBean}
import gorsat.parser.FunctionSignature._
import gorsat.parser.FunctionTypes.{dFun, iFun, sFun}
import gorsat.process.GorPipe
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource
import org.gorpipe.gor.model.{ColumnValueProvider, DriverBackedFileReader}

import java.nio.file.{Files, Paths}
import java.time.Instant

object DiagnosticsFunctions {
  val osBean: OperatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean.asInstanceOf[OperatingSystemMXBean]

  def register(functions: FunctionRegistry, pathfunctions: FunctionRegistry): Unit = {
    functions.registerWithOwner("TIME", getSignatureEmpty2Int(removeOwner(time)), time _)
    functions.register("MINORVERSION", getSignatureEmpty2Int(minorVersion _), minorVersion _)
    functions.register("MAJORVERSION", getSignatureEmpty2Int(majorVersion _), majorVersion _)
    functions.register("HOSTNAME", getSignatureEmpty2String(hostname _), hostname _)
    functions.register("IP", getSignatureEmpty2String(ip _), ip _)
    functions.register("ARCH", getSignatureEmpty2String(arch _), arch _)
    functions.register("GORVERSION", getSignatureEmpty2String(gorVersion _), gorVersion _)
    functions.register("JAVAVERSION", getSignatureEmpty2String(javaVersion _), javaVersion _)
    functions.register("THREADID", getSignatureEmpty2Double(threadId _), threadId _)
    functions.register("CPULOAD", getSignatureEmpty2Double(cpuLoad _), cpuLoad _)
    functions.register("SYSCPULOAD", getSignatureEmpty2Double(sysCpuLoad _), sysCpuLoad _)
    functions.register("FREE", getSignatureEmpty2Double(free _), free _)
    functions.register("TOTALMEM", getSignatureEmpty2Double(totalMem _), totalMem _)
    functions.register("FREEMEM", getSignatureEmpty2Double(freeMem _), freeMem _)
    functions.register("MAXMEM", getSignatureEmpty2Double(maxMem _), maxMem _)
    functions.register("AVAILCPU", getSignatureEmpty2Double(availCpu _), availCpu _)
    functions.register("OPENFILES", getSignatureEmpty2Double(openFiles _), openFiles _)
    functions.register("MAXFILES", getSignatureEmpty2Double(maxFiles _), maxFiles _)
    functions.register("FILEPATH", getSignatureString2String(filePath _), filePath _)
    functions.register("FILECONTENT", getSignatureString2String(fileContent _), fileContent _)
    functions.register("FILEINFO", getSignatureString2String(fileInfo _), fileInfo _)
    pathfunctions.register("FILEPATH", getSignatureString2String(filePath _), filePath _)
    pathfunctions.register("FILECONTENT", getSignatureString2String(fileContent _), fileContent _)
    functions.registerWithOwner("AVGROWSPERMILLIS", getSignatureEmpty2Double(removeOwner(getAvgRowsPerMilliSecond)), getAvgRowsPerMilliSecond _)
    functions.registerWithOwner("AVGBASESPERMILLIS", getSignatureEmpty2Double(removeOwner(getAvgBasesPerMilliSecond)), getAvgBasesPerMilliSecond _)
    functions.registerWithOwner("AVGSEEKTIMEMILLIS", getSignatureEmpty2Double(removeOwner(getAvgSeekTimeMilliSecond)), getAvgSeekTimeMilliSecond _)
  }

  def getAvgRowsPerMilliSecond(owner: ParseArith): dFun = {
    _ => {
      owner.getAvgRowsPerMilliSecond
    }
  }

  def getAvgBasesPerMilliSecond(owner: ParseArith): dFun = {
    _ => {
      owner.getAvgBasesPerMilliSecond
    }
  }

  def getAvgSeekTimeMilliSecond(owner: ParseArith): dFun = {
    _ => {
      owner.getAvgSeekTimeMilliSecond
    }
  }

  def filePath(f: (ColumnValueProvider) => String): sFun = {
    s => {
      f(s)
    }
  }

  def fileContent(f: (ColumnValueProvider) => String): sFun = {
    s => {
      Files.lines(Paths.get(f(s))).skip(1).findFirst().get()
    }
  }

  def fileInfo(f: (ColumnValueProvider) => String): sFun = {
    s => {
      val driverBackedFileReader = new DriverBackedFileReader(null, null, null)
      val path = f(s)
      var signature = "-"
      var lastmod = 0L
      var unique = "-"
      var len = -1L
      var readable = "false"
      try {
        val ds = driverBackedFileReader.resolveUrl(path)
        val meta = ds.getSourceMetadata
        signature = driverBackedFileReader.getFileSignature(path)
        lastmod = meta.getLastModified
        unique = meta.getUniqueId
        val ss = ds.asInstanceOf[StreamSource]
        len = ss.getSourceMetadata.getLength
      } catch {
        case _: Exception =>
      }
      try {
        val ds = driverBackedFileReader.resolveUrl(path)
        val ss = ds.asInstanceOf[StreamSource]
        val is = ss.open()
        is.read()
        readable = "true"
        is.close()
      } catch {
        case _: Exception =>
      }
      path + "," + signature + "," + Instant.ofEpochMilli(lastmod).toString + "," + unique + "," + len + "," + readable
    }
  }

  def maxFiles(): dFun = {
    _ => {
      osBean match {
        case bean: UnixOperatingSystemMXBean => bean.getMaxFileDescriptorCount
        case _ => throw new GorParsingException("Error in MAXFILES - this is only available on a unix platform: ")
      }
    }
  }

  def openFiles(): dFun = {
    _ => {
      osBean match {
        case bean: UnixOperatingSystemMXBean => bean.getOpenFileDescriptorCount
        case _ => throw new GorParsingException("Error in OPENFILES - this is only available on a unix platform: ")
      }
    }
  }

  def availCpu(): dFun = {
    _ => {
      Runtime.getRuntime.availableProcessors()
    }
  }

  def maxMem(): dFun = {
    _ => {
      Runtime.getRuntime.maxMemory()
    }
  }

  def freeMem(): dFun = {
    _ => {
      Runtime.getRuntime.freeMemory()
    }
  }

  def totalMem(): dFun = {
    _ => {
      Runtime.getRuntime.totalMemory()
    }
  }

  def free(): dFun = {
    _ => {
      osBean.getFreePhysicalMemorySize
    }
  }

  def threadId(): dFun = {
    _ => {
      Thread.currentThread().getId
    }
  }

  def cpuLoad(): dFun = {
    _ => {
      osBean.getProcessCpuLoad
    }
  }

  def sysCpuLoad(): dFun = {
    _ => {
      osBean.getSystemCpuLoad
    }
  }

  def javaVersion(): sFun = {
    _ => {
      System.getProperty("java.version")
    } // Use Runtime.version() in Java 9
  }

  def gorVersion(): sFun = {
    _ => {
      GorPipe.version
    }
  }

  def arch(): sFun = {
    _ => {
      osBean.getArch
    }
  }

  def ip(): sFun = {
    _ => {
      InetAddress.getLocalHost.getHostAddress
    }
  }

  def hostname(): sFun = {
    _ => {
      InetAddress.getLocalHost.getHostName
    }
  }

  def minorVersion(): iFun = {
    _ => {
      val spl = GorPipe.version.split(" ")(0).split("-")(0).split("\\.")
      var ret = -1
      if (spl.length > 1) try {
        ret = Integer.parseInt(spl(1))
      } catch {
        case e: Exception =>
          e.printStackTrace()
      }
      ret
    }
  }

  def majorVersion(): iFun = {
    _ => {
      val spl = GorPipe.version.split(" ")(0).split("-")(0).split("\\.")
      var ret = -1
      if (spl.length > 0) try {
        ret = Integer.parseInt(spl(0))
      } catch {
        case e: Exception =>
          e.printStackTrace()
      }
      ret
    }
  }

  def time(owner: ParseArith): iFun = {
    _ => {
      (System.currentTimeMillis() - owner.context.getStartedAt).toInt
    }
  }
}
