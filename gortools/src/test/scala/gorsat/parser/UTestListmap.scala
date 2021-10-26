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
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class UTestListmap extends ListFunctionsTestHelper {
  "listmap" should "handle single item" in {
    val result: String = evalExpression(
      "listmap(col1, 'int(x)+10')",
      "3"
    )
    assert(result == "13")
  }

  it should "handle multiple items" in {
    val result: String = evalExpression(
      "listmap(col1, '\\'item \\'+x')",
      "3,4,7,15"
    )
    assert(result == "item 3,item 4,item 7,item 15")
  }

  it should "handle multiple items with a custom single char delimiter" in {
    val result: String = evalExpression(
      "listmap('a:b:cd:eee', 'x+x', ':')"
    )
    assert(result == "aa:bb:cdcd:eeeeee")
  }

  it should "handle multiple items with a custom two char delimiter" in {
    val result: String = evalExpression(
      "listmap('axxbxxcdxxeee', 'x+x', 'xx')"
    )
    assert(result == "aaxxbbxxcdcdxxeeeeee")
  }

  it should "handle multiple items with a long delimiter" in {
    val result: String = evalExpression(
      "listmap('a<delimiter>b<delimiter>cd<delimiter>eee', 'x+x', '<delimiter>')"
    )
    assert(result == "aa<delimiter>bb<delimiter>cdcd<delimiter>eeeeee")
  }

  it should "handle multiple items with a single control character delimiter" in {
    val result: String = evalExpression(
      "listmap('a\rb\rcd\reee', 'x+x', '\r')"
    )
    assert(result == "aa\rbb\rcdcd\reeeeee")
  }

  it should "handle multiple items with a special delimiter" in {
    val result: String = evalExpression(
      "listmap('a\\\\!b\\\\!cd\\\\!eee', 'x+x', '\\\\!')"
    )
    assert(result == "aa\\!bb\\!cdcd\\!eeeeee")
  }

  it should "handle empty delimiter" in {
    val result: String = evalExpression(
      "listmap(col1, '\\'item \\'+x', '')",
      "3471"
    )
    assert(result == "item 3item 4item 7item 1")
  }
}
