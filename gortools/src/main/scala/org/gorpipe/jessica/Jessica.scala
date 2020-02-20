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

package org.gorpipe.jessica

import gorsat.BatchedReadSourceConfig
import gorsat.process.PipeOptions
import javax.swing.SwingUtilities
import org.gorpipe.base.config.ConfigManager
import org.gorpipe.exceptions.ExceptionUtilities
import org.gorpipe.gor.servers.GorConfig
import org.gorpipe.model.genome.files.gor.DbSource
import org.gorpipe.model.util.ConfigUtil
import org.slf4j.LoggerFactory

object Jessica extends App {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val consoleLogger = LoggerFactory.getLogger("console." + this.getClass)

  var version: String = getClass.getPackage.getImplementationVersion
  if (version eq null) {
    version = "Unknown"
  }

  // Parse the input parameters
  val commandlineOptions = new PipeOptions
  commandlineOptions.parseOptions(args)

  val brsConfig: BatchedReadSourceConfig = ConfigManager.createPrefixConfig("jessica", classOf[BatchedReadSourceConfig])
  val gorConfig: GorConfig = ConfigManager.createPrefixConfig("jessica", classOf[GorConfig])

  ExceptionUtilities.setShowStackTrace(true)

  // Initialize config
  ConfigUtil.loadConfig("jessica")

  // Initialize database connections
  DbSource.initInConsoleApp()

  SwingUtilities.invokeLater(() => {
    val jessicaRunner = new JessicaRunner
    jessicaRunner.init(commandlineOptions)
    val frame = new MainWindow(jessicaRunner)

    frame.setVisible(true)
  })
}
