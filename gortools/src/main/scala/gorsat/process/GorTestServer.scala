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

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.{AppenderBase, ConsoleAppender}
import ch.qos.logback.core.encoder.Encoder
import ch.qos.logback.core.joran.spi.ConsoleTarget
import com.fasterxml.jackson.databind.ObjectMapper
import com.sun.net.httpserver.{HttpExchange, HttpServer}
import java.io.{ByteArrayOutputStream, OutputStream, PrintStream}
import java.net.{InetSocketAddress, URLDecoder}
import java.nio.charset.StandardCharsets
import java.util.UUID
import org.gorpipe.exceptions.{ExceptionUtilities, GorException}
import org.gorpipe.gor.cli.render.ReportCommand
import org.gorpipe.gor.util.DataUtil
import org.slf4j.LoggerFactory

import scala.jdk.CollectionConverters._

object GorTestServer {

  private val logger = LoggerFactory.getLogger(this.getClass)
  // The logger gorpipe reports a failed query with ("console." + GorPipe.getClass, named here so
  // GorPipe is not initialized), so the error reaches stderr formatted the same way
  private val consoleLogger = LoggerFactory.getLogger("console.class gorsat.process.GorPipe$")
  private val jsonMapper = new ObjectMapper()

  /** Exit status of a gorpipe process whose query failed (it calls System.exit(-1)). */
  val FailedQueryExitCode = 255

  /**
    * What gorpipe would have given for a query: its exit status, what it wrote to stdout (also
    * the part written before a failure) and what it wrote to stderr. `error` is the reply text
    * for a failed query.
    */
  case class QueryResult(exitCode: Int, stdout: String, stderr: String, error: String = "")

  def start(port: Int, baseOptions: PipeOptions): Unit = {
    // Errors read as gorpipe prints them, which shows no stack trace without -stacktrace
    ExceptionUtilities.setShowStackTrace(baseOptions.showStackTrace)
    create(port, baseOptions)
    Thread.currentThread().join()
  }

  /**
    * Starts the server on 127.0.0.1:port (0 picks a free port) and returns it without blocking.
    *
    * POST /query runs a GOR query and answers with what gorpipe writes to stdout, or 500 and the
    * error text when it fails. A request with `Accept: application/json` gets instead, both on
    * success (200) and on failure (500), a JSON object with what a gorpipe process would have
    * given: `exitCode` (0, or 255 for a failed query), `stdout` (on failure, what the query wrote
    * before it failed) and `stderr` (what was written to stderr while the query ran, with the
    * error reported as gorpipe reports it).
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
      handlePost(exchange, query => answerQuery(query, requestOptions(baseOptions, exchange), wantsJson(exchange))))
    server.createContext("/render", (exchange: HttpExchange) =>
      handlePost(exchange, report => textReply(renderReport(report, requestOptions(baseOptions, exchange)))))
    server.start()
    val boundPort = server.getAddress.getPort
    System.err.println(s"GOR test server listening on port $boundPort")
    logger.info(s"GOR test server started on port $boundPort")
    server
  }

  private val TextContentType = "text/plain; charset=UTF-8"
  private val JsonContentType = "application/json; charset=UTF-8"

  private def wantsJson(exchange: HttpExchange): Boolean = {
    val accept = exchange.getRequestHeaders.get("Accept")
    accept != null && accept.stream().anyMatch(a => a.toLowerCase.contains("application/json"))
  }

  private def textReply(reply: (Int, String)): (Int, String, String) = (reply._1, reply._2, TextContentType)

  private def answerQuery(query: String, opts: PipeOptions, json: Boolean): (Int, String, String) = {
    val result = runQuery(query, opts)
    val status = if (result.exitCode == 0) 200 else 500
    if (json) {
      val node = jsonMapper.createObjectNode()
      node.put("exitCode", result.exitCode)
      node.put("stdout", result.stdout)
      node.put("stderr", result.stderr)
      (status, jsonMapper.writeValueAsString(node), JsonContentType)
    } else if (result.exitCode == 0) {
      (status, result.stdout, TextContentType)
    } else {
      (status, result.error, TextContentType)
    }
  }

  /**
    * Answers a POST with the status, body and content type `handler` returns for its request body. Every
    * request gets an answer: a handler that throws is answered with 500 and the error, as a request that
    * closes without one reads to a client like a server that went away.
    */
  private[process] def handlePost(exchange: HttpExchange, handler: String => (Int, String, String)): Unit = {
    try {
      if (exchange.getRequestMethod.equalsIgnoreCase("POST")) {
        val request = new String(exchange.getRequestBody.readAllBytes(), "UTF-8").trim
        val (status, body, contentType) =
          try handler(request)
          catch { case ex: Throwable => textReply(errorReply("Request", ex)) }
        val bytes = body.getBytes("UTF-8")
        exchange.getResponseHeaders.set("Content-Type", contentType)
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

  /**
    * Runs a query as gorpipe would, and keeps what gorpipe would have written to stderr: all that
    * is written to System.err while it runs, and what logback's console appenders write to the
    * process's stderr past System.err (with jansi, as gorpipe's console log does). That is where
    * gorpipe's console log and the error of a failed query go. Queries run one at a time (the
    * server has a single thread), so the capture holds only this query's output.
    */
  private[process] def runQuery(query: String, opts: PipeOptions): QueryResult = {
    opts.query = PipeOptions.cleanUpQueryAndSplit(query).mkString(";")

    val out = new ByteArrayOutputStream()
    val err = new ByteArrayOutputStream()
    val originalErr = System.err
    val teeErr = new PrintStream(new TeeOutputStream(originalErr, err), true, StandardCharsets.UTF_8)
    var failure: Option[Throwable] = None
    val stopMirroring = mirrorConsoleAppenders(err)
    System.setErr(teeErr)
    try {
      val engine = new CLIGorExecutionEngine(opts, null, null, out)
      try {
        engine.execute()
      } catch {
        // Reported as GorPipe.main reports them
        case ge: GorException =>
          failure = Some(ge)
          consoleLogger.error(ExceptionUtilities.gorExceptionToString(ge))
        case ex: Throwable =>
          failure = Some(ex)
          consoleLogger.error("Unexpected error, please report if you see this.\n" + ex.getMessage, ex)
      }
    } finally {
      teeErr.flush()
      System.setErr(originalErr)
      stopMirroring()
    }
    val stdout = out.toString(StandardCharsets.UTF_8)
    val stderr = err.toString(StandardCharsets.UTF_8)
    failure match {
      case Some(ex) => QueryResult(FailedQueryExitCode, stdout, stderr, errorReply("Query", ex)._2)
      case None => QueryResult(0, stdout, stderr)
    }
  }

  /**
    * Also writes to `capture` what each logback console appender writes to stderr without going
    * through System.err, until the returned function is called. An appender whose stream is
    * logback's System.err delegate is left out, as the System.err capture already has its output.
    */
  private def mirrorConsoleAppenders(capture: OutputStream): () => Unit = {
    LoggerFactory.getILoggerFactory match {
      case context: LoggerContext =>
        val mirrors = for {
          logger <- context.getLoggerList.asScala.toList
          appender <- logger.iteratorForAppenders().asScala.toList
          console <- appender match {
            case c: ConsoleAppender[ILoggingEvent @unchecked] if c.isStarted &&
              c.getTarget == ConsoleTarget.SystemErr.getName &&
              (c.getOutputStream ne ConsoleTarget.SystemErr.getStream) => Some(c)
            case _ => None
          }
        } yield {
          val mirror = new MirrorAppender(console.getEncoder, capture)
          mirror.setContext(context)
          mirror.setName("gor-test-server-mirror-" + console.getName)
          mirror.start()
          logger.addAppender(mirror)
          (logger, mirror)
        }
        () => mirrors.foreach { case (logger, mirror) =>
          logger.detachAppender(mirror)
          mirror.stop()
        }
      case _ => () => ()
    }
  }

  /** Encodes events with another appender's encoder and writes them to `out`. */
  private class MirrorAppender(encoder: Encoder[ILoggingEvent], out: OutputStream)
    extends AppenderBase[ILoggingEvent] {
    override def append(event: ILoggingEvent): Unit = out.write(encoder.encode(event))
  }

  /** Writes to both streams. */
  private class TeeOutputStream(first: OutputStream, second: OutputStream) extends OutputStream {
    override def write(b: Int): Unit = { first.write(b); second.write(b) }
    override def write(b: Array[Byte], off: Int, len: Int): Unit = {
      first.write(b, off, len)
      second.write(b, off, len)
    }
    override def flush(): Unit = { first.flush(); second.flush() }
  }
}
