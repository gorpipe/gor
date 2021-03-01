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

package org.gorpipe.jessica

import gorsat.Commands.{Analysis, RowHeader}

case class PipeStepInfo(source: String, rowHeader: RowHeader, pipeStep: Analysis, exception: Throwable) {
  override def toString: String = {
    val output = new StringBuilder
    output.append(source)
    output.append("\n")
    if(rowHeader != null) {
      output.append(rowHeader.toStringWithTypes)
      output.append("\n")
      if(pipeStep != null) {
        output.append(pipeStep.toString)
        output.append("\n")
      }
    } else if(exception != null) {
      output.append(exception.getMessage)
    }
    output.append("\n")
    output.mkString
  }
}
