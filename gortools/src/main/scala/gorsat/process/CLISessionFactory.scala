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

package gorsat.process

import java.nio.file.Paths

import gorsat.QueryHandlers.GeneralQueryHandler
import gorsat.Utilities.AnalysisUtilities
import org.gorpipe.gor._
import org.gorpipe.gor.clients.LocalFileCacheClient
import org.gorpipe.gor.model._
import org.gorpipe.gor.session.{EventLogger, GorSession, ProjectContext, SystemContext}
import org.gorpipe.gor.util.StringUtil

/**
  * Factory class to create session which relates to running gor in a command line. The session is created based on
  * command line options.
  *
  * @param pipeOptions     GorPipe command line options
  * @param securityContext Security context if needed
  */
class CLISessionFactory(pipeOptions: PipeOptions, securityContext: String = null) extends GorSessionFactory {

  override def create(): GorSession = {
    val requestId = pipeOptions.requestId
    val useSubFolder = System.getProperty("gor.local.filecache.usesubfolders", "true").toBoolean
    val subFolderSize = System.getProperty("gor.local.filecache.subfoldersize", "2").toInt

    val session = new GorSession(requestId)

    val projectRoot = Paths.get(pipeOptions.gorRoot)
    var cacheDir = Paths.get(pipeOptions.cacheDir)
    if(!cacheDir.isAbsolute) cacheDir = projectRoot.resolve(cacheDir)

    val projectContextBuilder = new ProjectContext.Builder()
    val projectContext = projectContextBuilder
      .setAliasFile(pipeOptions.aliasFile)
      .setCacheDir(pipeOptions.cacheDir)
      .setConfigFile(pipeOptions.configFile)
      .setLogDirectory(pipeOptions.logDir)
      .setConfigFile(pipeOptions.configFile)
      .setRoot(pipeOptions.gorRoot)
      .setFileReader(createFileReader(pipeOptions.gorRoot))
      .setFileCache(new LocalFileCacheClient(cacheDir, useSubFolder, subFolderSize))
      .setQueryHandler(createQueryHandler(pipeOptions.queryHandler, session))
      .setQueryEvaluator(new SessionBasedQueryEvaluator(session))
      .build()

    val systemContextBuilder = new SystemContext.Builder()
    val systemContext = systemContextBuilder
      .setCommandWhitelist(GorJavaUtilities.readWhiteList(AnalysisUtilities.getWhiteListCommandFilePath(projectContext.getRealProjectRootPath)))
      .setReportBuilder(new FreemarkerReportBuilder(session))
      .setRunnerFactory(new GenericRunnerFactory())
      .setServer(false)
      .setWorkers(pipeOptions.workers)
      .setStartTime(System.currentTimeMillis())
      .build()

    val cache = GorSessionCacheManager.getCache(requestId)

    session.setNorContext(pipeOptions.norContext)

    val eventLogger: EventLogger = if (pipeOptions.stats) new RequestStats(session) else new DefaultEventLogger

    session.init(projectContext, systemContext, cache, eventLogger)

    session
  }

  private def createFileReader(gorRoot: String): FileReader = {
    if (!StringUtil.isEmpty(gorRoot)) {
      new DriverBackedFileReader(securityContext, gorRoot, null)
    } else {
      GorFileReaderContext.DEFAULT_READER
    }
  }

  private def createQueryHandler(queryHandlerName: String, session: GorSession): GorParallelQueryHandler = {
    new GeneralQueryHandler(session.getGorContext, false)
  }
}
