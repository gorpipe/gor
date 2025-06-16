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

import java.util.zip.Deflater
import gorsat.Commands.{Analysis, Output, RowHeader}
import gorsat.Outputs.OutFile
import org.apache.commons.io.FilenameUtils
import org.gorpipe.exceptions.GorResourceException
import org.gorpipe.gor.binsearch.GorIndexType
import org.gorpipe.gor.driver.meta.DataType
import org.gorpipe.gor.model.{DriverBackedFileReader, GorMeta, GorOptions, Row}
import org.gorpipe.gor.session.{GorSession, ProjectContext}
import org.gorpipe.gor.table.util.PathUtils
import org.gorpipe.gor.util.DataUtil
import org.gorpipe.model.gor.RowObj

import java.util.UUID
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

case class OutputOptions(remove: Boolean = false,
                            columnCompress: Boolean = false,
                            md5: Boolean = false,
                            md5File: Boolean = false,
                            nor: Boolean = false,
                            idx: GorIndexType = GorIndexType.NONE,
                            forkTags: Array[String] = new Array[String](0),
                            dictTags: Array[String] = new Array[String](0),
                            prefix: Option[String] = None,
                            prefixFile: Option[String] = None,
                            compressionLevel: Int = Deflater.BEST_SPEED,
                            useFolder: Option[String] = Option.empty,
                            skipHeader: Boolean = false,
                            writeMeta: Boolean = true,
                            cardCol: String = null,
                            linkFile: String = "",
                            command: String = null,
                            infer: Boolean = false,
                            maxseg: Boolean = false
                           )

case class ForkWrite(forkCol: Int,
                     fullFileName: String,
                     session: GorSession,
                     inHeader: String,
                     options: OutputOptions) extends Analysis {

  case class FileHolder(forkValue: String) {
    if (forkCol >= 0 && options.useFolder.isEmpty && !(fullFileName.contains("#{fork}") || fullFileName.contains("""${fork}"""))) {
      throw new GorResourceException("WRITE error: #{fork} of ${fork}missing from filename.", fullFileName)
    }
    var fileName : String = _
    val projectContext = session.getProjectContext
    if(options.useFolder.nonEmpty) {
      val folder = options.useFolder.get
      ensureDir(projectContext, folder)
      val fn = if(fullFileName.isEmpty) {
        val uuid = UUID.randomUUID().toString
        val ending = folder.substring(folder.lastIndexOf('.'))
        s"$uuid${if(DataUtil.isGord(folder)) DataType.GORZ.suffix else ending}"
      } else {
        fullFileName
      }

      val dir = if(folder.endsWith("/")) folder else folder + "/"

      if (forkCol >= 0) {
        val cols = inHeader.split("\t")
        val fork = cols(forkCol) + "=" + forkValue
        val forkdir = dir + fork
        ensureDir(projectContext, forkdir)
        fileName = forkdir + "/" + fn
      } else {
        fileName = dir + fn
      }
    } else {
      fileName = fullFileName.replace("#{fork}", forkValue).replace("""${fork}""", forkValue)
      ensureDir(projectContext, fileName, parent = true)
    }
    var fileOpen = false
    var headerWritten = false
    var rowBuffer = new ArrayBuffer[Row]
    var out: Output = _
  }

  def ensureDir(projectContext: ProjectContext, path: String, parent: Boolean = false): Unit = {
    val fileReader = projectContext.getFileReader
    val dir = if (parent) {
      val parent = PathUtils.getParent(path)
      if (parent != null) parent else null
    } else {
      path
    }

    if (dir != null && !fileReader.exists(dir)) {
      fileReader.createDirectories(dir)
    }
  }

  var useFork: Boolean = forkCol >= 0
  var forkMap = mutable.Map.empty[String, FileHolder]
  val forkTagSet: mutable.Set[String] = scala.collection.mutable.Set()++options.forkTags
  val dictTagSet: mutable.Set[String] = scala.collection.mutable.Set()++options.dictTags
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

  override def isTypeInformationMaintained: Boolean = true

  /**
    * Creates OutFile with given name
    * if the path is a directory save a file with generated md5 sum as name under directory
    * @param name
    * @param skipHeader
    * @return
    */
  def createOutFile(name: String, skipHeader: Boolean): Output = {
    if (rowHeader == null || useFork) {
      OutFile.driver(name, session.getProjectContext.getFileReader, header, skipHeader, options)
    } else {
      if (!rowHeader.toString.equals(header)) {
        rowHeader = RowHeader(header, rowHeader.columnTypes)
      }
      OutFile.driver(name, session.getProjectContext.getFileReader, rowHeader, skipHeader, options)
    }
  }

  def openFile(sh: FileHolder): Unit = {
    val name = sh.fileName
    val skipHeader = options.skipHeader || (if (!sh.headerWritten) {
      sh.headerWritten = true
      false
    } else {
      true
    })
    sh.out = createOutFile(name, skipHeader)
    sh.out.setup()
    sh.fileOpen = true
    openFiles += 1
    sh.rowBuffer.foreach(x => {
      sh.out.process(x)
    })
    sh.rowBuffer = new ArrayBuffer[Row]
  }

  override def process(ir: Row): Unit = {
    somethingToWrite = true
    var sh: FileHolder = null
    if (useFork) {
      val forkID = ir.colAsString(forkCol).toString
      forkMap.get(forkID) match {
        case Some(x) => sh = x
        case None =>
          sh = FileHolder(forkID)
          forkTagSet.remove(forkID)
          forkMap += (forkID -> sh)
      }
    } else sh = singleFileHolder

    var r = ir
    if (options.remove) {
      if(forkCol > 0) r.removeColumn(forkCol)
      else {
        r = RowObj(s"chrN\t0\t${r.pos}\t${r.otherCols()}")
      }
    }

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

  def outFinish(sh : FileHolder): Unit = {
    sh.out.finish()
  }

  override def finish(): Unit = {
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
    if (options.useFolder.isEmpty && !somethingToWrite && !useFork) {
      val out = createOutFile(fullFileName, false)
      out.setup()
      out.finish()
    }

    // Test the tag files and create them if we are not in error
    if (!isInErrorState&&useFork) {
      // Create all missing tag files
      forkTagSet.foreach(x => {
        if (x.nonEmpty) {
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

    if (useFork) {
      forkMap.values.foreach(sh => {
        val (linkFile, linkFileContent) = extractLink(sh.fileName)

        if (linkFile.nonEmpty) {
          writeLinkFile(linkFile, linkFileContent)
        }
      })
    } else {
      val (linkFile, linkFileContent) = extractLink(fullFileName)

      if (linkFile.nonEmpty) {
        writeLinkFile(linkFile, linkFileContent)
      }
    }
  }

  // Available after finish.
  def getMd5: String = {
    if (!useFork && singleFileHolder.out != null) {
      singleFileHolder.out.getMeta.getMd5
    } else {
      ""
    }
  }

  private def extractLink(fileName: String) : (String,String) = {
    var linkFile = options.linkFile
    var linkFileContent = ""
    if (fileName.nonEmpty) {
      if (linkFile.isEmpty) {
        val dataSource = session.getProjectContext.getFileReader.resolveUrl(fileName, true)
        if (dataSource != null && dataSource.forceLink()) {
          linkFile = dataSource.getProjectLinkFile
          linkFileContent = dataSource.getProjectLinkFileContent
        }
      } else {
        linkFileContent = PathUtils.resolve(session.getProjectContext.getProjectRoot, fileName)
      }
    }
    (linkFile,linkFileContent)
  }

  private def writeLinkFile(linkFile: String, linkFileContent: String) : Unit = {
    val linkFileToWrite = if (DataUtil.isLink(linkFile))
      linkFile
    else
      DataUtil.toFile(linkFile, DataType.LINK)

    // Use non driver file reader as this is exception from the write no links rule, add extra resolve with the
    // server reader to validate (skip link extension as writing links is allow forbidden).
    session.getProjectContext.getFileReader.resolveUrl(FilenameUtils.removeExtension(linkFileToWrite), true)
    val fileReader = new DriverBackedFileReader(session.getProjectContext.getFileReader.getSecurityContext,
      session.getProjectContext.getProjectRoot)
    val os = fileReader.getOutputStream(linkFileToWrite)
    try {
      os.write(linkFileContent.getBytes())
      os.write('\n')
    } finally {
      if (os != null) os.close()
    }
  }
}
