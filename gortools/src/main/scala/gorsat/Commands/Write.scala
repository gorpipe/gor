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

package gorsat.Commands

import java.util.zip.Deflater
import gorsat.Analysis.{ForkWrite, OutputOptions}
import gorsat.Outputs.OutFile
import gorsat.Commands.CommandParseUtilities._
import org.apache.commons.io.FilenameUtils
import org.gorpipe.exceptions.{GorParsingException, GorResourceException}
import org.gorpipe.gor.binsearch.GorIndexType
import org.gorpipe.gor.driver.linkfile.LinkFileUtil
import org.gorpipe.gor.driver.meta.{DataType, SourceReference}
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource
import org.gorpipe.gor.model.{BaseMeta, GorMeta}
import org.gorpipe.gor.session.GorContext
import org.gorpipe.gor.util.DataUtil
import org.slf4j.{Logger, LoggerFactory}

import scala.jdk.CollectionConverters._

object Write {
    val log: Logger = LoggerFactory.getLogger(this.getClass)

    // Meta keys GOR maintains itself; -meta may not set them.
    private val systemMetaKeys = Set(
      BaseMeta.HEADER_VERSION_KEY, BaseMeta.HEADER_FILE_FORMAT_KEY, BaseMeta.HEADER_SERIAL_KEY,
      BaseMeta.HEADER_CREATED_KEY, BaseMeta.HEADER_LINE_COUNT_KEY, BaseMeta.HEADER_SCHEMA_KEY,
      BaseMeta.MAXSEG_SCHEMA_KEY, BaseMeta.HEADER_MD5_KEY, BaseMeta.HEADER_TAGS_KEY, BaseMeta.HEADER_COLUMNS_KEY,
      GorMeta.HEADER_QUERY_KEY, GorMeta.HEADER_RANGE_KEY, GorMeta.HEADER_CARDCOL_KEY)

    // Keys must read back from a meta file unchanged (BaseMeta.parsePropertyLine splits on '=' and ':').
    private val metaKeyPattern = "[A-Z0-9_.-]+".r

    /**
     * Parse the -meta option value: comma separated key=value pairs, optionally quoted (see BaseMeta.parseKeyValues).
     * Keys are upper cased and limited to letters, digits, '_', '.' and '-'.
     */
    def parseMetaOption(option: String): Map[String, String] = {
      BaseMeta.parseKeyValues(option).asScala.map { kv =>
        val key = kv.getKey.toUpperCase
        if (key.isEmpty || kv.getValue == null) {
          throw new GorParsingException(s"Invalid -meta entry '${kv.getKey}', expected key=value", "-meta")
        }
        if (!metaKeyPattern.matches(key)) {
          throw new GorParsingException(s"Invalid -meta key '${kv.getKey}', use letters, digits, '_', '.' or '-'", "-meta")
        }
        if (systemMetaKeys.contains(key)) {
          throw new GorParsingException(s"Meta key $key is maintained by GOR and can not be set with -meta", "-meta")
        }
        key -> kv.getValue
      }.toMap
    }
}

class Write extends CommandInfo("WRITE",
  CommandArguments("-r -c -m -inferschema -maxseg -noheader", "-d -f -i -t -l -tags -card -prefix -link -linkmeta -meta", 0),
  CommandOptions(gorCommand = true, norCommand = true, verifyCommand = true)) {

  def parseBaseOptions(context: GorContext, iargs: Array[String], args: Array[String], executeNor: Boolean): (String, Option[String], Boolean) = {
    var fileName = replaceSingleQuotes(iargs.mkString(" "))

    val linkOpt = if (hasOption(args, "-link")) stringValueOfOption(args, "-link") else ""
    val linkMetaOpt = if (hasOption(args, "-linkmeta")) stringValueOfOption(args, "-linkmeta") else ""

    fileName = if (fileName.isEmpty && linkOpt.nonEmpty) {
      val linkMetaInfo = LinkFileUtil.extractLinkMetaInfo(linkMetaOpt)
      val linkSourceRef = new SourceReference(DataUtil.toLink(linkOpt),
        context.getSession.getProjectContext.getFileReader.getSecurityContext,
        context.getSession.getProjectContext.getFileReader.getCommonRoot, null, null, true);
      // Infer the full file name from the link (and default locations)
      LinkFileUtil.inferDataFileNameFromLinkFile(
        context.getSession.getProjectContext.getFileReader.resolveDataSource(linkSourceRef).asInstanceOf[StreamSource],
        linkMetaInfo.linkFileMeta,
        context.getSession.getProjectContext.getFileReader);
    } else {
      fileName
    }

    val useFolder = if (hasOption(args, "-d")) {
      Option.apply(stringValueOfOption(args, "-d"))
    } else if(!executeNor && DataUtil.isGord(fileName)) {
      val fn = fileName
      Option.apply(fn)
    } else {
      Option.empty
    }

    val hasFork = hasOption(args, "-f")

    (fileName, useFolder, hasFork)
  }

  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {

    val (fileName, useFolder,  _) = parseBaseOptions(context, iargs, args, executeNor)

    var forkCol = -1
    var remove = false
    var columnCompress: Boolean = false
    var md5 = false
    var idx = GorIndexType.NONE
    var compressionLevel = Deflater.BEST_SPEED
    var skipHeader = false

    if (hasOption(args, "-f")) forkCol = columnOfOption(args, "-f", forcedInputHeader, executeNor)
    remove = hasOption(args, "-r")
    columnCompress = hasOption(args, "-c")
    md5 = hasOption(args, "-m")
    if (hasOption(args, "-l")) compressionLevel = stringValueOfOptionWithErrorCheck(args, "-l", Array("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")).toInt

    val linkOpt = if (hasOption(args, "-link")) stringValueOfOption(args, "-link") else ""
    val linkMetaOpt = if (hasOption(args, "-linkmeta")) stringValueOfOption(args, "-linkmeta") else ""

    if (fileName.isEmpty && useFolder.isEmpty && linkOpt.isEmpty) {
      throw new GorResourceException("No file or folder specified","")
    };

    val infer = hasOption(args, "-inferschema")
    val maxseg = hasOption(args, "-maxseg")

    val card = stringValueOfOptionWithDefault(args, "-card", null)

    var prefixFile: Option[String] = None
    var prefix: Option[String] = None
    if (hasOption(args, "-prefix")) {
      val prfx = stringValueOfOption(args, "-prefix")
      if (prfx.startsWith("'")) prefix = Option(prfx.substring(1, prfx.length - 1).replace("\\n", "\n").replace("\\t", "\t"))
      else prefixFile = Option(prfx)
    }

    if (hasOption(args, "-t") && !hasOption(args, "-f")) {
      throw new GorParsingException("Option -t is only valid with the -f option.", "-t")
    }

    val forkTagArray = replaceSingleQuotes(stringValueOfOptionWithDefault(args, "-t", "")).split(",", -1).map(x => x.trim).distinct

    val dictTagArray = replaceSingleQuotes(stringValueOfOptionWithDefault(args, "-tags", "")).split(",", -1).map(x => x.trim).distinct

    def handleIndex = {
      var indexing = "NONE"

      if (hasOption(args, "-i")) {
        indexing = stringValueOfOptionWithErrorCheck(args, "-i", Array("NONE", "CHROM", "FULL", "TABIX"))
      }

      indexing match {
        case "NONE" => idx = GorIndexType.NONE
        case "CHROM" => idx = GorIndexType.CHROMINDEX
        case "FULL" => idx = GorIndexType.FULLINDEX
        case "TABIX" => idx = GorIndexType.TABIX
      }

      if (idx == GorIndexType.NONE && context.getSession != null && fileName.nonEmpty) {
        val forkValue = ""
        val dataSource = context.getSession.getProjectContext.getFileReader.resolveUrl(fileName.replace("#{fork}", forkValue).replace("""${fork}""", forkValue), true)
        if (dataSource != null) {
          idx = dataSource.useIndex()
        }
      }
    }

    handleIndex

    skipHeader = hasOption(args, "-noheader")
    val fileType = "." + FilenameUtils.getExtension(fileName.toLowerCase())
    if (skipHeader && DataType.getWritableFormats().contains(fileType)) {
      throw new GorParsingException("Option -noheader (skip header) is not valid with gor/gorz/nor/norz")
    }

    val nor = executeNor || (forkCol == 0 && remove)
    val meta = if (hasOption(args, "-meta")) Write.parseMetaOption(stringValueOfOption(args, "-meta")) else Map.empty[String, String]

    val build = Option(context.getSession)
      .flatMap(s => Option(s.getProjectContext.getReferenceBuild))
      .filter(_.isKnownVersion)
      .map(_.getVersion)
      .orNull

    val options = OutputOptions(
      remove,
      columnCompress,
      true,
      md5,
      nor,
      idx,
      forkTagArray,
      dictTagArray,
      prefix,
      prefixFile,
      compressionLevel,
      useFolder,
      skipHeader,
      cardCol = card,
      linkFile = linkOpt,
      linkFileMeta = linkMetaOpt,
      command = argString,
      infer = infer,
      maxseg = maxseg,
      build = build,
      meta = meta,
      gorMetaFile = true
    )

    // -meta can only be stored in outputs that get a .meta file.
    if (meta.nonEmpty) {
      val outputName = useFolder match {
        case Some(folder) if ForkWrite.namesFilesInFolder(fileName, folder) => ForkWrite.folderFileEnding(folder, nor)
        case _ => fileName
      }
      if (!OutFile.writesMetaFile(outputName, options)) {
        throw new GorParsingException("Option -meta is only valid with gor, gorz, norz and vcf output (not with -noheader)", "-meta")
      }
    }

    CommandParsingResult(
      ForkWrite(forkCol, fileName, context.getSession, forcedInputHeader, options),
      forcedInputHeader
    )
  }
}
