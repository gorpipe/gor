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

package gorsat.Analysis

import java.nio.file.{Files, Paths, StandardOpenOption}
import java.util.zip.Deflater
import gorsat.Commands.{Analysis, Output}
import gorsat.Outputs.{GORzip, OutFile}
import org.gorpipe.exceptions.GorResourceException
import org.gorpipe.gor.binsearch.GorIndexType
import org.gorpipe.gor.model.{GorOptions, Row}
import org.gorpipe.model.gor.RowObj

import java.util.UUID
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

case class OutputOptions(remove: Boolean = false,
                            columnCompress: Boolean = false,
                            md5: Boolean = false,
                            md5File: Boolean = true,
                            nor: Boolean = false,
                            idx: GorIndexType = GorIndexType.NONE,
                            tags: Array[String] = null,
                            prefix: Option[String] = None,
                            prefixFile: Option[String] = None,
                            compressionLevel: Int = Deflater.BEST_SPEED,
                            useFolder: Boolean = false,
                            skipHeader: Boolean = false
                           )

case class ForkWrite(forkCol: Int,
                     fullFileName: String,
                     inHeader: String,
                     options: OutputOptions) extends Analysis {

  case class FileHolder(forkValue: String) {
    if (forkCol >= 0 && !options.useFolder && !(fullFileName.contains("#{fork}") || fullFileName.contains("""${fork}"""))) {
      throw new GorResourceException("WRITE error: #{fork} of ${fork}missing from filename.", fullFileName)
    }
    var fileName : String = _
    if(forkCol >= 0 && options.useFolder) {
      val dir = Paths.get(fullFileName)
      val cols = inHeader.split("\t")
      val forkdir = dir.resolve(cols(forkCol)+"="+forkValue)
      if(!Files.exists(forkdir)) {
        Files.createDirectories(forkdir)
      }
      fileName = forkdir.resolve(dir.getFileName).toString
    } else fileName = fullFileName.replace("#{fork}", forkValue).replace("""${fork}""", forkValue)
    var fileOpen = false
    var headerWritten = false
    var rowBuffer = new ArrayBuffer[Row]
    var out: Output = _
  }

  var minChr: String = null
  var minPos: Int = -1
  var maxChr: String = null
  var maxPos: Int = -1

  var useFork: Boolean = forkCol >= 0
  var forkMap = mutable.Map.empty[String, FileHolder]
  val tagSet: mutable.Set[String] = scala.collection.mutable.Set()++options.tags
  var singleFileHolder: FileHolder = FileHolder("")
  if (!useFork) forkMap += ("theOnlyFile" -> singleFileHolder)
  var openFiles = 0
  val maxOpenFiles = 5000
  var maxBufferSize = 1000
  var counter = 0
  var somethingToWrite = false
  var header: String = inHeader
  if (options.remove) {
    val cols: Array[String] = inHeader.split("\t")
    val headerBuilder = new mutable.StringBuilder(inHeader.length + cols.length - 1)
    headerBuilder.append(cols(0))
    headerBuilder.append('\t')
    if(forkCol==0) headerBuilder.append("posnor\t")
    headerBuilder.append(cols(1))
    var c = 2
    while (c < cols.length) {
      if (c != forkCol) {
        headerBuilder.append('\t')
        headerBuilder.append(cols(c))
      }
      c += 1
    }
    header = headerBuilder.toString
  }

  def outFile(name: String, skipHeader: Boolean): Output = {
    if (options.useFolder) {
      val p = Paths.get(name)
      Files.createDirectories(p)
      val uuid = UUID.randomUUID().toString
      val noptions = OutputOptions(options.remove, options.columnCompress, true, false, options.nor, options.idx, options.tags, options.prefix, options.prefixFile, options.compressionLevel, options.useFolder, options.skipHeader)
      OutFile.driver(p.resolve(uuid+".gorz").toString, header, skipHeader, noptions)
    } else {
      OutFile.driver(name, header, skipHeader, options)
    }
  }

  def openFile(sh: FileHolder) {
    val name = sh.fileName
    val skipHeader = options.skipHeader || (if (!sh.headerWritten) {
      sh.headerWritten = true
      false
    } else {
      true
    })
    sh.out = outFile(name, skipHeader)
    sh.out.setup()
    sh.fileOpen = true
    openFiles += 1
    sh.rowBuffer.foreach(x => {
      sh.out.process(x)
    })
    sh.rowBuffer = new ArrayBuffer[Row]
  }

  override def process(ir: Row) {
    somethingToWrite = true
    var sh: FileHolder = null
    if (useFork) {
      val forkID = ir.colAsString(forkCol).toString
      forkMap.get(forkID) match {
        case Some(x) => sh = x
        case None =>
          sh = FileHolder(forkID)
          tagSet.remove(forkID)
          forkMap += (forkID -> sh)
      }
    } else sh = singleFileHolder

    var r = ir
    if (options.remove) {
      if(forkCol > 0) r.removeColumn(forkCol)
      else {
        r = RowObj("chrN\t0\t"+r.pos+"\t"+r.otherCols())
      }
    }

    if(minChr==null) {
      minChr = ir.chr
      minPos = ir.pos
    }

    maxChr = ir.chr
    maxPos = ir.pos

    if (sh.fileOpen) {
      sh.out.process(r)
    } else {
      if (sh.rowBuffer.length < maxBufferSize) {
        sh.rowBuffer += r
      } else if (openFiles < maxOpenFiles) {
        sh.rowBuffer += r
        openFile(sh)
      } else {
        forkMap.values.filter(_.fileOpen).foreach(sh => {
          sh.out.finish()
          sh.fileOpen = false
          openFiles -= 1
        })
        sh.rowBuffer += r
        openFile(sh)
      }
    }
  }

  def move(name: String, md5: String): Unit = {
    val p = Paths.get(name)
    val parent = p.getParent
    val respath = md5 + ".gorz"
    val d = parent.resolve(respath)
    if(!Files.exists(d)) {
      Files.move(p, d)
      val dict = parent.resolve(GorOptions.DEFAULT_FOLDER_DICTIONARY_NAME)
      Files.writeString(dict, respath + "\t1\t" + minChr + "\t" + minPos + "\t" + maxChr + "\t" + maxPos + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND)
    }
  }

  def outFinish(sh : FileHolder): Unit = {
    sh.out.finish()
    sh.out match {
      case of: OutFile =>
        val md5 = of.md5
        if(md5!=null) move(of.getName, md5)
      case of: GORzip =>
        val md5 = of.out.getMd5
        if(md5!=null) move(of.getName(), md5)
      case _ =>
    }
  }

  override def finish() {
    forkMap.values.foreach(sh => {
      if (sh.fileOpen) {
        if (sh.out != null) outFinish(sh)
        sh.fileOpen = false
        openFiles -= 1
      }
      if (sh.rowBuffer.nonEmpty) {
        openFile(sh)
        if (sh.out != null) outFinish(sh)
        sh.fileOpen = false
      }
    })
    if (!somethingToWrite && !useFork) {
      val out = outFile(fullFileName, false)
      out.setup()
      out.finish()
    }

    // Test the tag files and create them if we are not in error
    if (!isInErrorState) {
      // Create all missing tag files
      tagSet.foreach(x => {
        if (!x.isEmpty) {
          try {
            val fileHolder = FileHolder(x)
            openFile(fileHolder)
            fileHolder.out.finish()
          } catch {
            case _:Exception =>
              // Ignore at this time
          }
        }
      })
    }
  }
}
