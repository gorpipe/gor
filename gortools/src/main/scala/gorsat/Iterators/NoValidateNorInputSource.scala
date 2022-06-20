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

package gorsat.Iterators

import org.gorpipe.gor.model.{FileReader, NoValidateRowBase, QuoteSafeRowBase, Row}

class NoValidateNorInputSource(fileName: String, fileReader: FileReader, readStdin: Boolean, forceReadHeader: Boolean, maxWalkDepth: Int, followLinks: Boolean, showModificationDate: Boolean, ignoreEmptyLines: Boolean) extends NorInputSource(fileName, fileReader, readStdin, forceReadHeader, maxWalkDepth, followLinks, showModificationDate, ignoreEmptyLines) {
  override def next(): Row = {
    new NoValidateRowBase("chrN\t0\t" + nextLine(), myHeaderLength)
  }
}
