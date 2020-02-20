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

package gorsat

/**
  * Copyright (c) 2018 WuxiNextCode Inc.
  * All Rights Reserved.
  *
  * This software is the confidential and proprietary information of
  * WuxiNextCode Inc. ("Confidential Information"). You shall not
  * disclose such Confidential Information and shall use it only in
  * accordance with the terms of the license agreement you entered into
  * with WuxiNextCode.
  *
  * Created by Hjalti on 12/6/18.
  *
  */

package gorsat

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class UTestStringAnalysisUtilities extends FunSuite {

  test("UTestStringUtilities") {
    val builder = new StringBuilder
    val list = Range(0,10).map(_=> "hjalti").toList
    StringUtilities.addWhile(builder, 68, "\t", list)
    assert(builder.length == 62)

    builder.clear()
    StringUtilities.addWhile(builder, 100, "\t", list)
    assert(builder.length == 69)

    builder.clear()
    StringUtilities.addWhile(builder, 1, "blablablab", list)
    assert(builder.isEmpty)
  }
}
