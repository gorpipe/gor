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
import org.gorpipe.gor.driver.linkfile.LinkFileUtil
import org.gorpipe.gor.driver.meta.DataType
import org.gorpipe.gor.model.Row
import org.gorpipe.gor.session.{GorSession, ProjectContext}
import org.gorpipe.gor.table.util.PathUtils
import org.gorpipe.gor.util.DataUtil
import org.gorpipe.model.gor.RowObj
import org.slf4j.{Logger, LoggerFactory}

import java.util.UUID
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/*

NOTES:
1. Write for pgor is generally forbidden, except when writing gord files.
2. Explict link file writing is not allowed, link files to given data files are allowed using the -link option.

The GOR write has several different "modes" of operation:

1. Single file write
   gor ... | write output.gor

   In this mode a single file is created with the name output.gor. If the file already exists it will be overwritten.

2. Forked write with variable in filename
   gor ... | write -f col output_#{fork}.gor

   In this mode a file is created for each fork value with the fork value replacing the #{fork} variable in the filename.

3. GOR dictionary write
   pgor ... | write output.gord

   In this mode a GOR dictionary file is created (if it does not already exist) and file for each part is created,
   using fingerprints for the file names.   Additional gord file, thedict.gord, is creaate within the folder.

4. Forked directory write
   gor ... | write -f col -d output_dir/

   In this mode a directory is created (if it does not already exist) and a subfolders are created for each fork value.

5. Link file write
   gor ... | write -link output.link

   In this mode a data file with a unique name is created in the default data location and a link file with the specified name
   is created pointing to the data file.

Modes that do not work:

6. Directory  write
   gor ... | write -d output_dir/

   In this mode a directory is created (if it does not already exist) and a file with a unique name is created
   inside the directory. If the directory already exists the file will be created inside the existing directory.

   This works for gor but is kind of pointless.
   Does not work for pgor, which could make sense to allow gor write.

 7. Forked link file write
   gor ... | write -f col -link output_#{fork}.gor

   In this mode a data file with a unique name is created in the default data location for each fork value and link files with the specified name
   are created pointing to the data files.
    This mode is not supported.
 */

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
                            linkFileMeta: String = "",
                            command: String = null,
                            infer: Boolean = false,
                            maxseg: Boolean = false
                           )

case class ForkWrite(forkCol: Int,
                     fullFileName: String,
                     session: GorSession,
                     inHeader: String,
                     options: OutputOptions) extends Analysis {

  private val log: Logger = LoggerFactory.getLogger(ForkWrite.this.getClass)

  case class FileHolder(forkValue: String) {
    if (forkCol >= 0 && options.useFolder.isEmpty && !(fullFileName.contains("#{fork}") || fullFileName.contains("""${fork}"""))) {
      throw new GorResourceException("WRITE error: #{fork} of ${fork}missing from filename.", fullFileName)
    }

    val projectContext = session.getProjectContext
    var fileName : String = inferFileName(fullFileName, forkValue)
    ensureDir(projectContext, fileName, parent = true)

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

  def inferFileName(inFileName: String, forkValue: String): String = {
    var inferredFileName: String = ""
    if(options.useFolder.nonEmpty) {
      val folder = options.useFolder.get
      val fn = if (inFileName.isEmpty || DataUtil.isGord(folder)) {
        val uuid = UUID.randomUUID().toString
        val folderEnding = FilenameUtils.getExtension(folder)
        val ending = if (folderEnding.nonEmpty) "." + folderEnding else (if (options.nor) DataType.NOR.suffix else DataType.GORZ.suffix)
        s"$uuid${if(DataUtil.isGord(folder)) DataType.GORZ.suffix else ending}"
      } else {
        inFileName
      }

      val dir = if(folder.endsWith("/")) folder else folder + "/"

      if (forkCol >= 0) {
        val cols = inHeader.split("\t")
        val fork = cols(forkCol) + "=" + forkValue
        val forkdir = dir + fork
        inferredFileName = forkdir + "/" + fn
      } else {
        inferredFileName = dir + fn
      }
    } else {
      inferredFileName = if (forkCol >= 0) {
        inFileName.replace("#{fork}", forkValue).replace("""${fork}""", forkValue)
      } else {
        inFileName
      }
    }

    inferredFileName
  }

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

    // Only write links for files that are NOT inside gord
    if (options.useFolder.isEmpty && !singleFileHolder.fileName.contains(".gord/")) {
      if (useFork) {
        forkMap.values.foreach(sh => {
          val linkData = LinkFileUtil.extractLink(session.getProjectContext.getFileReader, sh.fileName,
            inferFileName(options.linkFile, sh.forkValue), options.linkFileMeta, getMd5)

          if (linkData.linkFile().nonEmpty) {
            LinkFileUtil.writeLinkFile(session.getProjectContext.getFileReader, linkData)
          }
        })
      } else {
        val linkData = LinkFileUtil.extractLink(session.getProjectContext.getFileReader, singleFileHolder.fileName, options.linkFile, options.linkFileMeta, getMd5)

        if (linkData.linkFile().nonEmpty) {
          LinkFileUtil.writeLinkFile(session.getProjectContext.getFileReader, linkData)
        }
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
}
