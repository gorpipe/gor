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

package gorsat.process

import java.nio.file.Paths
import java.util.UUID

import gorsat.QueryHandlers.GeneralQueryHandler
import gorsat.Utilities.AnalysisUtilities
import org.gorpipe.gor._
import org.gorpipe.gor.clients.LocalFileCacheClient
import org.gorpipe.gor.model.DriverBackedFileReader
import org.gorpipe.gor.session.{GorSession, ProjectContext, SystemContext}
import org.gorpipe.util.standalone.GorStandalone

/**
  * Factory class to create a session for standalone Sequence Miner.
  *
  * @param aliasFile    Alias file used for the session
  * @param configFile   Configuration file used for the session
  * @param projectName  Name of the project being opened in SM
  */
class StandaloneSessionFactory(aliasFile:String, configFile:String, projectName:String) extends GorSessionFactory{

  val CACHE_DIR_NAME = "result_cache"

  override def create(): GorSession = {
    val requestId = UUID.randomUUID().toString
    val session = new GorSession(requestId)

    val projectContextBuilder = new ProjectContext.Builder()
    val projectContext = projectContextBuilder
        .setCacheDir(CACHE_DIR_NAME)
        .setConfigFile(configFile)
        .setProjectName(projectName)
        .setAliasFile(aliasFile)
        .setRoot(GorStandalone.getStandaloneRoot)
        .setFileReader(new DriverBackedFileReader(null, GorStandalone.getStandaloneRoot, null))
        .setFileCache(new LocalFileCacheClient(Paths.get(GorStandalone.getStandaloneRoot, CACHE_DIR_NAME),
          GorStandalone.getResultCacheUseSubfolders,
          GorStandalone.getResultCacheSubfolderSize))
        .setQueryHandler(new GeneralQueryHandler(session.getGorContext, false))
        .setQueryEvaluator(new SessionBasedQueryEvaluator(session))
        .build()

    val systemContextBuilder = new SystemContext.Builder()
    val systemContext = systemContextBuilder
      .setCommandWhitelist(GorJavaUtilities.readWhiteList(AnalysisUtilities.getWhiteListCommandFilePath(projectContext.getRealProjectRootPath)))
      .setReportBuilder(new FreemarkerReportBuilder(session))
      .setRunnerFactory(new GenericRunnerFactory())
      .setServer(false)
      .setWorkers(Runtime.getRuntime.availableProcessors())
      .setStartTime(System.currentTimeMillis())
      .build()

    val cache = GorSessionCacheManager.getCache(requestId)

    session.init(projectContext, systemContext, cache)

    session
  }
}
