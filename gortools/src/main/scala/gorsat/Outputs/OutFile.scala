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

import java.io._
import java.nio.file.{Files, Path, Paths}
import java.util.zip.Deflater

import gorsat.Analysis.OutputOptions
import gorsat.Commands.Output
import gorsat.parquet.GorParquetFileOut
import htsjdk.samtools.util.{BlockCompressedInputStream, BlockCompressedOutputStream, Md5CalculatingOutputStream}
import htsjdk.tribble.index.tabix.{TabixFormat, TabixIndexCreator}
import htsjdk.tribble.readers.{AsciiLineReader, AsciiLineReaderIterator}
import htsjdk.tribble.util.LittleEndianOutputStream
import htsjdk.variant.vcf.VCFCodec
import org.gorpipe.exceptions.GorResourceException
import org.gorpipe.gor.binsearch.GorIndexType
import org.gorpipe.gor.model.Row

/**
  * @param name Name of the file to be written.
  * @param header Header of the incoming source.
  * @param skipHeader Whether the header should be written or not.
  * @param append Whether we should write the output to the beginning or end of the file.
  * @param md5 Whether the md5 sum of the file's content should be written to a side file or not.
  */
class OutFile(name: String, header: String, skipHeader: Boolean = false, append: Boolean = false, md5File: Boolean = true, md5: Boolean, idx: GorIndexType, compressionLevel: Int) extends Output {
  val finalFileOutputStream = new java.io.FileOutputStream(name, append)
  val interceptingFileOutputStream: OutputStream =
    if (md5) {
      new Md5CalculatingOutputStream(finalFileOutputStream, new File(name + ".md5"))
    } else {
      finalFileOutputStream
    }
  val gzippedOutputStream: OutputStream =
    if (name.toLowerCase.endsWith(".gz") || name.toLowerCase.endsWith(".bgz")) {
      val p : Path = null
      new BlockCompressedOutputStream(interceptingFileOutputStream, p, compressionLevel)
    } else {
      interceptingFileOutputStream
    }
  val out: Writer =
    new java.io.OutputStreamWriter(new BufferedOutputStream(gzippedOutputStream, 1024 * 128))

  override def getName: String = name

  override def getMd5: String = {
    interceptingFileOutputStream match {
      case stream: Md5CalculatingOutputStream =>
        stream.md5()
      case _ => null
    }
  }

  def setup {
    if (header != null & !skipHeader) {
      if (!header.startsWith("#")) {
        out.write("#")
      }
      out.write(header + "\n")
    }
  }

  def process(r: Row) {
    out.write(r.toString)
    out.write('\n')
  }

  def finish {
    out.flush()
    out.close()

    if(idx == GorIndexType.TABIX) {
      val gp = Paths.get(name)

      val bcis = new BlockCompressedInputStream(Files.newInputStream(gp))

      val gpi = Paths.get(name+".tbi")
      val tbi = new TabixIndexCreator(TabixFormat.VCF)

      val outputStream = new LittleEndianOutputStream(new BlockCompressedOutputStream(gpi.toFile))
      val codec = new VCFCodec
      val lineReader = AsciiLineReader.from(bcis)
      val iterator = new AsciiLineReaderIterator(lineReader)
      codec.readActualHeader(iterator)
      while (iterator.hasNext) {
        val position = iterator.getPosition
        val currentContext = codec.decode(iterator.next)
        tbi.addFeature(currentContext, position)
      }
      val index = tbi.finalizeIndex(iterator.getPosition)
      iterator.close()
      index.write(outputStream)
      outputStream.close()
    }
  }
}

object OutFile {

  def vcfHeader(prefix: String, header: String): String = {
    prefix + "\n" + (if(header.startsWith("#")) header else "#"+header)
  }

  def driver(name: String, inheader: String, skipHeader: Boolean, options: OutputOptions): Output = {
    val nameUpper = name.toUpperCase
    val isVCF = nameUpper.endsWith(".VCF") || nameUpper.endsWith(".VCF.GZ") || nameUpper.endsWith(".VCF.BGZ")

    var append = skipHeader
    val header = if(options.prefix.isDefined) {
      val pref = options.prefix.get
      if(isVCF) {
        vcfHeader(pref, inheader)
      } else {
        pref + inheader
      }
    } else if(options.prefixFile.isEmpty && isVCF) {
      vcfHeader("##fileformat=VCFv4.2", inheader)
    } else {
      append = append || {
        options.prefixFile match {
          case Some(prefixName) =>
            writePrefix(prefixName, name)
            true
          case None => false
        }
      }
      inheader
    }

    try {
      if (nameUpper.endsWith(".GORZ") || nameUpper.endsWith(".NORZ")) {
        new GORzip(name, header, skipHeader, append, options.columnCompress, options.md5, options.md5File, options.idx, options.compressionLevel)
      } else if (nameUpper.endsWith(".TSV") || nameUpper.endsWith(".NOR")) {
        new NorFileOut(name, header, skipHeader, append, options.md5)
      } else if (nameUpper.endsWith(".PARQUET")) {
        new GorParquetFileOut(name, header, options.nor)
      } else if (options.nor) {
        new CmdFileOut(name, header, skipHeader, append)
      } else {
        new OutFile(name, header, skipHeader, append, options.md5File, options.md5, options.idx, options.compressionLevel)
      }
    } catch {
      case e: FileNotFoundException => throw new GorResourceException(s"Can't write to file", name, e)
    }
  }

  def apply(name: String, header: String, skipHeader: Boolean, columnCompress: Boolean, nor: Boolean, md5: Boolean, md5File: Boolean, idx: GorIndexType, prefixFile: Option[String] = None, compressionLevel: Int = Deflater.BEST_SPEED): Output =
    driver(name, header, skipHeader, OutputOptions(remove = false, columnCompress = columnCompress, md5 = md5, md5File = md5File, nor = nor, idx, null, None, prefixFile, compressionLevel))

  def apply(name: String, header: String, skipHeader: Boolean, nor: Boolean, md5: Boolean): Output = driver(name, header, skipHeader, OutputOptions(nor = nor, md5 = md5))

  def apply(name: String, header: String, skipHeader: Boolean, nor: Boolean): Output = driver(name, header, skipHeader, OutputOptions(nor = nor))

  def apply(name: String, header: String, skipHeader: Boolean): Output = driver(name, header, skipHeader, OutputOptions())

  def apply(name: String, header: String): Output = driver(name, header, skipHeader = false, OutputOptions())

  def apply(name: String): Output = driver(name, null, skipHeader = false, OutputOptions())

  def writePrefix(prefixFileName: String, fileName: String): Unit = {
    val is = new FileInputStream(prefixFileName)
    val os = new FileOutputStream(fileName)
    val buffer = new Array[Byte](1024) //Reasonable buffer size - there is usually not that much in the file.
    var addNewLine = false
    var read = is.read(buffer)
    while (read > 0) {
      os.write(buffer, 0, read)
      addNewLine = buffer(read - 1) != '\n'
      read = is.read(buffer)
    }
    if (addNewLine) os.write('\n')
    is.close()
    os.close()
  }
}
