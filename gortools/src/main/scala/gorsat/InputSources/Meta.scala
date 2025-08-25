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

package gorsat.InputSources

import gorsat.Commands._
import org.gorpipe.gor.driver.GorDriverFactory
import org.gorpipe.gor.driver.meta.{SourceReference, SourceReferenceBuilder}
import org.gorpipe.gor.session.GorContext


class Meta() extends InputSourceInfo("META", CommandArguments("", "", 1)) {

  override def processArguments(context: GorContext, argString: String, iargs: Array[String],
                                args: Array[String]): InputSourceParsingResult = {

    val inputData = iargs(0)
    val reader = context.getSession.getProjectContext.getFileReader

    val dataSource = reader.resolveUrl(new SourceReferenceBuilder(inputData)
      .commonRoot(reader.getCommonRoot())
      .securityContext(reader.getSecurityContext())
      .isFallback(false)
      .queryTime(reader.getQueryTime)
      .build())

    val factory =GorDriverFactory.fromConfig()
    val it = factory.createMetaIterator(dataSource, reader)

    InputSourceParsingResult(it,
      null,
      isNorContext = true,
    )
  }


}
