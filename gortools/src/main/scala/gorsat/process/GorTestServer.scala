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

import com.sun.net.httpserver.{HttpExchange, HttpServer}
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import org.gorpipe.exceptions.{ExceptionUtilities, GorException}
import org.slf4j.LoggerFactory

object GorTestServer {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def start(port: Int, baseOptions: PipeOptions): Unit = {
    val server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0)

    server.createContext("/query", (exchange: HttpExchange) => {
      try {
        if (exchange.getRequestMethod.equalsIgnoreCase("POST")) {
          val query = new String(exchange.getRequestBody.readAllBytes(), "UTF-8").trim
          val (status, body) = executeQuery(query, baseOptions)
          val bytes = body.getBytes("UTF-8")
          exchange.getResponseHeaders.set("Content-Type", "text/plain; charset=UTF-8")
          exchange.sendResponseHeaders(status, bytes.length)
          val os = exchange.getResponseBody
          os.write(bytes)
          os.close()
        } else {
          exchange.sendResponseHeaders(405, -1)
        }
      } finally {
        exchange.close()
      }
    })

    server.start()
    System.err.println(s"GOR test server listening on port $port")
    logger.info(s"GOR test server started on port $port")

    Thread.currentThread().join()
  }

  private def executeQuery(query: String, baseOptions: PipeOptions): (Int, String) = {
    val opts = new PipeOptions()
    opts.query = PipeOptions.cleanUpQueryAndSplit(query).mkString(";")
    opts.gorRoot = baseOptions.gorRoot
    opts.configFile = baseOptions.configFile
    opts.aliasFile = baseOptions.aliasFile
    opts.cacheDir = baseOptions.cacheDir
    opts.workers = baseOptions.workers
    opts.color = "none"

    val out = new ByteArrayOutputStream()
    val engine = new CLIGorExecutionEngine(opts, null, null, out)
    try {
      engine.execute()
      (200, out.toString("UTF-8"))
    } catch {
      case ge: GorException =>
        val msg = ExceptionUtilities.gorExceptionToString(ge)
        logger.error("Query failed: {}", msg)
        (500, msg)
      case ex: Throwable =>
        val msg = Option(ex.getMessage).getOrElse(ex.getClass.getName)
        logger.error("Query failed: {}", msg, ex)
        (500, msg)
    }
  }
}
