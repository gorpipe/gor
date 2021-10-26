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

package gorsat.parser

import org.junit.runner.RunWith
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class UTestStringDistance extends AnyFlatSpec
{
  "levenshtein" should "return 0 for identical strings" in {
    val dist = StringDistance.levenshtein("first", "first");
    assert(dist == 0)
  }

  it should "return 1 for a close match" in {
    val dist = StringDistance.levenshtein("first", "frst");
    assert(dist == 1)
  }

  "findClosest" should "return closest match" in {
    val closest = StringDistance.findClosest("columnName", 3, List("columnNames", "different", "clmnNm"))
    assert(closest == "columnNames")
  }

  it should "return closest match ignoring case" in {
    val closest = StringDistance.findClosest("COLUMNNAME", 3, List("columnNames", "different", "clmnNm"))
    assert(closest == "columnNames")
  }

  it should "not return a match if nothing is close" in {
    val closest = StringDistance.findClosest("notClose", 3, List("columnNames", "different", "clmnNm"))
    assert(closest == "")
  }
}
