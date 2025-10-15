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

import gorsat.QueryHandlers.GeneralQueryHandler
import org.gorpipe.gor.clients.LocalFileCacheClient
import org.gorpipe.gor.model.{AccessControlContext, DriverBackedFileReader, DriverBackedSecureFileReader}
import org.gorpipe.gor.session.{GorSession, ProjectContext, SystemContext}
import org.gorpipe.gor.util.StringUtil

import java.util
import scala.jdk.javaapi.CollectionConverters

/**
  * Factory method tho create session used for tests
  *
  * @param pipeOptions          Command line options for gorpipe
  * @param whitelistedCmdFiles  File for white listing
  * @param server               Indicates if the session is running on server or not
  */
class TestSessionFactory(pipeOptions: PipeOptions, whitelistedCmdFiles:String, server:Boolean,
                         securityContext:String = null,
                         allowedWriteLocations:Array[String] = Array[String]("test", "user_data", "result_cache")) extends GorSessionFactory {

  override def create(): GorSession = {
    val requestId = pipeOptions.requestId
    val useSubFolder = false
    val subFolderSize = 0

    val session = new GorSession(requestId)

    val allowedWriteLocationList = CollectionConverters.asJava(allowedWriteLocations)

    val fileReader = createFileReader(pipeOptions.gorRoot, securityContext, server, allowedWriteLocationList, System.currentTimeMillis());
    val projectContextBuilder = new ProjectContext.Builder()
    val projectContext = projectContextBuilder
      .setAliasFile(pipeOptions.aliasFile)
      .setCacheDir(pipeOptions.cacheDir)
      .setConfigFile(pipeOptions.configFile)
      .setLogDirectory(pipeOptions.logDir)
      .setConfigFile(pipeOptions.configFile)
      .setRoot(pipeOptions.gorRoot)
      .setProjectName("project_10004")
      .setFileReader(fileReader)
      .setFileCache(new LocalFileCacheClient(fileReader, pipeOptions.cacheDir, useSubFolder, subFolderSize))
      .setQueryHandler(new GeneralQueryHandler(session.getGorContext, false))
      .setQueryEvaluator(new SessionBasedQueryEvaluator(session))
      .build()

    val systemContextBuilder = new SystemContext.Builder()
    val systemContext = systemContextBuilder
      .setCommandWhitelist(GorJavaUtilities.readWhiteList(whitelistedCmdFiles))
      .setReportBuilder(new FreemarkerReportBuilder(session))
      .setRunnerFactory(new GenericRunnerFactory())
      .setServer(server)
      .setWorkers(pipeOptions.workers)
      .setStartTime(System.currentTimeMillis())
      .build()

    val cache = GorSessionCacheManager.getCache(requestId)

    session.setNorContext(pipeOptions.norContext)
    session.init(projectContext, systemContext, cache)

    session
  }

  def createFileReader(gorRoot: String, securityContext: String = null, server: Boolean = false,
                       writeLocations: util.List[String], startTime: Long): DriverBackedFileReader = {
    val emptyGorRoot = StringUtil.isEmpty(gorRoot)
    if (!emptyGorRoot || !StringUtil.isEmpty(securityContext)) {
      if(server) {
        new DriverBackedSecureFileReader(gorRoot, securityContext,
          AccessControlContext.builder().withWriteLocations(writeLocations).build(), startTime)
      } else {
        new DriverBackedFileReader(securityContext, if(emptyGorRoot) "." else gorRoot, startTime)
      }
    } else {
      new DriverBackedFileReader(securityContext, if(emptyGorRoot) "." else gorRoot, startTime)
    }
  }
}
