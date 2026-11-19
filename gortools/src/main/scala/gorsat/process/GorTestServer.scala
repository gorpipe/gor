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
import java.net.{InetSocketAddress, URLDecoder}
import java.nio.charset.StandardCharsets
import java.util.UUID
import org.gorpipe.exceptions.{ExceptionUtilities, GorException}
import org.gorpipe.gor.cli.render.ReportCommand
import org.gorpipe.gor.util.DataUtil
import org.slf4j.LoggerFactory

object GorTestServer {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def start(port: Int, baseOptions: PipeOptions): Unit = {
    create(port, baseOptions)
    Thread.currentThread().join()
  }

  /**
    * Starts the server on 127.0.0.1:port (0 picks a free port) and returns it without blocking.
    *
    * POST /query runs a GOR query and answers with what gorpipe writes to stdout.
    * POST /render renders a yml report, e.g. path/report.yml(arg='x'), and answers with what
    * `gor render report` writes to stdout, so a client can skip starting a JVM per render. Its optional
    * `root` parameter (/render?root=<dir>) is the directory the render runs from, as the CLI runs from its
    * working directory: the report and any path it reads while rendering resolve against it. Without
    * `root` they resolve against the server's own gorroot.
    *
    * POST /query takes `root` too, and `config` (/query?root=<dir>&config=<file>): the query then runs as
    * `gorpipe -gorroot <dir> -config <file>` would, so one server answers queries on any gorroot. Without
    * them it uses the server's own gorroot and config.
    */
  def create(port: Int, baseOptions: PipeOptions): HttpServer = {
    val server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0)
    server.createContext("/query", (exchange: HttpExchange) =>
      handlePost(exchange, query => executeQuery(query, requestOptions(baseOptions, exchange))))
    server.createContext("/render", (exchange: HttpExchange) =>
      handlePost(exchange, report => renderReport(report, requestOptions(baseOptions, exchange))))
    server.start()
    val boundPort = server.getAddress.getPort
    System.err.println(s"GOR test server listening on port $boundPort")
    logger.info(s"GOR test server started on port $boundPort")
    server
  }

  /**
    * Answers a POST with the status and body `handler` returns for its request body. Every request gets an
    * answer: a handler that throws is answered with 500 and the error, as a request that closes without one
    * reads to a client like a server that went away.
    */
  private[process] def handlePost(exchange: HttpExchange, handler: String => (Int, String)): Unit = {
    try {
      if (exchange.getRequestMethod.equalsIgnoreCase("POST")) {
        val request = new String(exchange.getRequestBody.readAllBytes(), "UTF-8").trim
        val (status, body) =
          try handler(request)
          catch { case ex: Throwable => errorReply("Request", ex) }
        val bytes = body.getBytes("UTF-8")
        exchange.getResponseHeaders.set("Content-Type", "text/plain; charset=UTF-8")
        val responseLength = if (bytes.length == 0) -1L else bytes.length.toLong
        exchange.sendResponseHeaders(status, responseLength)
        if (bytes.length > 0) {
          val os = exchange.getResponseBody
          os.write(bytes)
          os.close()
        }
      } else {
        exchange.sendResponseHeaders(405, -1)
      }
    } finally {
      exchange.close()
    }
  }

  /**
    * Options for one request's session, taken from the server's own, with the gorroot and config file of the
    * request's `root` and `config` parameters when it has them. Each request gets a fresh id, so it gets its
    * own GorSessionCache instead of sharing key "-1" with every other request.
    */
  private def requestOptions(baseOptions: PipeOptions, exchange: HttpExchange): PipeOptions = {
    val opts = new PipeOptions()
    opts.gorRoot = queryParameter(exchange, "root").getOrElse(baseOptions.gorRoot)
    opts.configFile = queryParameter(exchange, "config").getOrElse(baseOptions.configFile)
    opts.aliasFile = baseOptions.aliasFile
    opts.cacheDir = baseOptions.cacheDir
    opts.workers = baseOptions.workers
    opts.color = baseOptions.color
    opts.requestId = UUID.randomUUID().toString
    opts
  }

  private def errorReply(what: String, ex: Throwable): (Int, String) = ex match {
    case ge: GorException =>
      val msg = ExceptionUtilities.gorExceptionToString(ge)
      logger.error("{} failed: {}", what, msg)
      (500, msg)
    case _ =>
      val msg = Option(ex.getMessage).getOrElse(ex.getClass.getName)
      logger.error(s"$what failed: {}", msg, ex)
      (500, msg)
  }

  /** The decoded value of query parameter `name` in the request URI, if it has one. */
  private def queryParameter(exchange: HttpExchange, name: String): Option[String] =
    Option(exchange.getRequestURI.getRawQuery).toSeq
      .flatMap(_.split('&'))
      .map(_.split("=", 2))
      .collectFirst { case Array(key, value) if key == name => URLDecoder.decode(value, StandardCharsets.UTF_8) }

  private def renderReport(report: String, opts: PipeOptions): (Int, String) = {
    if (!DataUtil.isYml(report)) return (400, "Input is not a yml report.")
    try {
      val session = new CLISessionFactory(opts, "").create()
      try {
        // `gor render report` prints the rendered report with println
        (200, ReportCommand.renderReport(session, report, false) + "\n")
      } finally {
        session.close()
      }
    } catch {
      case ex: Throwable => errorReply("Render", ex)
    } finally {
      GorSessionCacheManager.invalidateCache(opts.requestId)
    }
  }

  private def executeQuery(query: String, opts: PipeOptions): (Int, String) = {
    opts.query = PipeOptions.cleanUpQueryAndSplit(query).mkString(";")

    val out = new ByteArrayOutputStream()
    val engine = new CLIGorExecutionEngine(opts, null, null, out)
    try {
      engine.execute()
      (200, out.toString("UTF-8"))
    } catch {
      case ex: Throwable => errorReply("Query", ex)
    }
  }
}
