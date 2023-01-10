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

package gorsat.Outputs

import gorsat.Analysis.OutputOptions

import java.util.zip.Deflater
import gorsat.Commands.{Analysis, Output}
import org.gorpipe.gor.binsearch.{GorIndexType, GorZipLexOutputStream}
import org.gorpipe.gor.driver.meta.DataType
import org.gorpipe.gor.model.{FileReader, GorMeta, Row}
import org.gorpipe.gor.util.DataUtil

import java.nio.file.Paths

/**
  * @param fileName Name of the file to be written.
  * @param header The header of the incoming source.
  * @param skipHeader Whether the header should be written or not.
  * @param append Whether we should write the output to the beginning or end of the file.
  * @param colcompress Whether a column compression should be used or not.
  * @param md5 Whether the md5 sum of the file's content should be written to a side file or not.
  * @param idx Whether and index file should be written.
  */
class GORzip(fileName: String, fileReader: FileReader, header: String = null, skipHeader: Boolean = false, append: Boolean = false, options: OutputOptions, schema: Array[String]) extends Output {

  val out = new GorZipLexOutputStream(fileReader.getOutputStream(fileName, append), options.columnCompress, options.md5, if(options.md5File) fileReader.toAbsolutePath(fileName+".md5") else null, if (options.idx != GorIndexType.NONE) fileReader.getOutputStream(fileName + DataType.GORI.suffix) else null, options.idx, options.compressionLevel)

  override def getName: String = fileName

  override def setup(): Unit = {
    getMeta.initMetaStats(options.cardCol, header, options.infer, options.maxseg)
    if (schema != null) getMeta.setSchema(schema)
    if (options.command != null) getMeta.setQuery(options.command)
    if (header != null & !skipHeader) out.setHeader(header)
  }

  override def process(r: Row): Unit = {
    getMeta.updateMetaStats(r)
    out.write(r)
  }

  override def finish(): Unit = {
    try {
      out.close()
      getMeta.setMd5(out.getMd5)
    } finally {
      if (options.writeMeta) {
        val metaout = fileReader.getOutputStream(DataUtil.toFile(fileName, DataType.META))
        metaout.write(getMeta.formatHeader().getBytes())
        metaout.close()
      }
    }
  }
}