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

import java.io.{File, OutputStream}
import gorsat.Commands.Output
import htsjdk.samtools.util.Md5CalculatingOutputStream
import org.gorpipe.gor.model.{FileReader, Row}
import org.gorpipe.gor.session.GorSession

import java.nio.file.Paths

/**
  * @param name Name of the file to be written.
  * @param header Header of the incoming source.
  * @param skipHeader Whether the header should be written or not.
  * @param append Whether we should write the output to the beginning or end of the file.
  */
class NorFileOut(name: String, fileReader: FileReader, header: String, skipHeader: Boolean = false, append: Boolean = false, md5: Boolean = false, md5File: Boolean = false)
  extends NorOutStream(header, {
      val finalFileOutputStream = fileReader.getOutputStream(name, append)
      if (md5) {
        new Md5CalculatingOutputStream(finalFileOutputStream, if(md5File) fileReader.toAbsolutePath(name + ".md5") else null)
      } else {
        finalFileOutputStream
      }},
    skipHeader) {
}
