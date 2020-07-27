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

// GorPipe.scala
// (c) deCODE genetics
// 17th May, 2011, Hakon Gudbjartsson

package gorsat.process

import gorsat._
import org.gorpipe.base.config.ConfigManager
import org.gorpipe.exceptions.{ExceptionUtilities, GorException}
import org.gorpipe.gor.servers.GorConfig
import org.gorpipe.logging.GorLogbackUtil
import org.gorpipe.model.genome.files.gor.{DbSource, DefaultFileReader}
import org.gorpipe.model.util.ConfigUtil
import org.slf4j.LoggerFactory

import scala.language.postfixOps

object GorPipe extends GorPipeFirstOrderCommands {

  private val consoleLogger = LoggerFactory.getLogger("console." + this.getClass)

  var version: String = getClass.getPackage.getImplementationVersion
  if (version eq null) {
    version = "Unknown"
  }

  val brsConfig: BatchedReadSourceConfig = ConfigManager.createPrefixConfig("gor", classOf[BatchedReadSourceConfig])
  val gorConfig: GorConfig = ConfigManager.createPrefixConfig("gor", classOf[GorConfig])

  /**
    * Main definition accepts an argument string and ensures database sources are initialized.
    */
  def main(args: Array[String]) {

    // Display help
    if (args.length < 1 || args(0).isEmpty || args(0).toUpperCase.startsWith("HELP")) {
      helpCommand(args, new DefaultFileReader(""))
      System.exit(0)
    }

    // Parse the input parameters
    val commandlineOptions = new PipeOptions
    commandlineOptions.parseOptions(args)

    ExceptionUtilities.setShowStackTrace(commandlineOptions .showStackTrace)

    if (commandlineOptions.version) {
      printOutGORPipeVersion()
      System.exit(0)
    }

    GorLogbackUtil.initLog("gorpipe")

    // Initialize config
    ConfigUtil.loadConfig("gor")

    // Initialize database connections
    DbSource.initInConsoleApp()


    var exitCode = 0
    //todo find a better way to construct

    val executionEngine = new CLIGorExecutionEngine(commandlineOptions, null, null)

    try {
      executionEngine.execute()
    } catch {
      case ge: GorException =>
        consoleLogger.error(ExceptionUtilities.gorExceptionToString(ge))
        exitCode = -1
      case ex: Throwable =>
        consoleLogger.error("Unexpected error, please report if you see this.\n" + ex.getMessage, ex)
        exitCode = -1
    }

    System.exit(exitCode)
  }

  private def printOutGORPipeVersion(): Unit = {
    val version = getClass.getPackage.getImplementationVersion
    System.out.println(if (version != null) version else "No implementation version found.")
  }
}