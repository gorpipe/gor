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

// gorsatGorIterator.scala
// (c) deCODE genetics
// 17th May, 2011, Hakon Gudbjartsson

package gorsat.gorsatGorIterator

import org.gorpipe.gor.model.{GenomicIterator, GorIterator}
import org.gorpipe.gor.session.GorContext

abstract class gorsatGorIterator(context: GorContext) extends GorIterator {
  var fixHeader : Boolean = true
  var isNorContext = false

  def getRowSource: GenomicIterator
  def getUsedFiles: List[String]
  def processArguments(args : Array[String], executeNor : Boolean, forcedInputHeader : String = ""): GenomicIterator

  def scalaInit(iparams : String, forcedInputHeader : String = ""): Unit

}

// ends gorsatGorIterator



