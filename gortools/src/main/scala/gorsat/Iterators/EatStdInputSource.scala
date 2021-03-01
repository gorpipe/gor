/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
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

import org.gorpipe.exceptions.GorSystemException
import org.gorpipe.model.gor.RowObj

class EatStdInputSource(staChr: String, staPos: Int, stoChr: String, stoPos: Int, shift: Int, name: String) extends SingleStdInputSource(name) {

  override def hasNext: Boolean = {
    if (!mustReCheck) return myHasNext
    mustReCheck = false
    if (!posSet) {
      throw new GorSystemException("Row position has not bee set", null)
    }
    while (theIterator.hasNext) {
      val line = theIterator.next()
      myNext = RowObj(line)
      if (myNext.chr >= staChr && myNext.chr <= stoChr && myNext.pos + shift >= staPos && myNext.pos - shift <= stoPos) {
        myHasNext = true
        return true
      }
    }
    myHasNext = false
    false
  }
}
