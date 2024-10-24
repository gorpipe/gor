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

import gorsat.Commands.CommandParseUtilities.{hasOption, rangeOfOption, stringValueOfOption}
import gorsat.Commands._
import gorsat.Utilities.AnalysisUtilities
import gorsat.process.GorJavaUtilities
import org.gorpipe.gor.driver.GorDriverFactory
import org.gorpipe.gor.driver.meta.SourceReferenceBuilder
import org.gorpipe.gor.model.GorOptions
import org.gorpipe.gor.session.{GorContext, GorSession}
import org.gorpipe.gor.util.SqlReplacer

import java.util

/**
 *
 */
object System {

//  private def processAllArguments(session: GorSession, argString: String, iargs: Array[String],
//                                  args: Array[String], isNorContext: Boolean): InputSourceParsingResult = {
//
//    val command = AnalysisUtilities.extractExternalSource(iargs(0))
//
//    //AnalysisUtilities.validateExternalSource(iargs(0))
//    //val myCommand = AnalysisUtilities.extractExternalSource(iargs(0))
//
//    //    val range = if (hasOption(args, "-p")) {
//    //      rangeOfOption(args, "-p")
//    //    } else {
//    //      GenomicRange.Range(null, 0, -1)
//    //    }
//    //    val tags = GorOptions.tagsFromOptions(session, args)
//    //    val database = if (hasOption(args, "-db")) stringValueOfOption(args, "-db") else null
//    //
//    //    val map = new util.HashMap[String, Object]()
//    //    map.put(SqlReplacer.KEY_CHROM, range.chromosome)
//    //    map.put(SqlReplacer.KEY_BPSTART, range.start.toString)
//    //    map.put(SqlReplacer.KEY_BPSTOP, range.stop.toString)
//    //    map.put(SqlReplacer.KEY_DATABASE, database)
//    //    map.put(SqlReplacer.KEY_TAGS, tags)
//    //    GorJavaUtilities.updateWithProjectInfo(session, map)
//    //
//    //    val iteratorSource = GorJavaUtilities.getDbIteratorSource(myCommand, map, database, !isNorContext, false)
//
//    InputSourceParsingResult(iteratorSource, "", isNorContext)
//  }
}

//class System() extends InputSourceInfo("SYSTEM", CommandArguments("", "", 1)) {
//
//  override def processArguments(context: GorContext, argString: String, iargs: Array[String],
//                                args: Array[String]): InputSourceParsingResult = {
//
//    val command = iargs(0)
//    val reader = context.getSession.getProjectContext.getFileReader
//
//    val dataSource = reader.resolveUrl(new SourceReferenceBuilder(inputData)
//      .commonRoot(reader.getCommonRoot())
//      .securityContext(reader.getSecurityContext()).isFallback(false).build())
//
//    val factory = GorDriverFactory.fromConfig()
//    val it = factory.createMetaIterator(dataSource, reader)
//
//    InputSourceParsingResult(it,
//      null,
//      isNorContext = true,
//    )
//  }
//}
//
//class CommandMove() extends InpurtSourceInfo("MV", CommandArguments("", "", 1)) {
//  override def processArguments(context: GorContext, argString: String, iargs: Array[String],
//                                args: Array[String]): InputSourceParsingResult = {
//
//  }
//
//}