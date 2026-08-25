/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2019 WuXi NextCode Inc.
 *  Copyright (C) 2024 GeneDx, LLC
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

package gorsat.Utilities

import gorsat.Macros.PGor
import gorsat.QueryHandlers.GeneralQueryHandler
import gorsat.Script.ExecutionBlock
import gorsat.process.{FreemarkerReportBuilder, GenericRunnerFactory, GorSessionCacheManager, SessionBasedQueryEvaluator}
import org.gorpipe.gor.clients.LocalFileCacheClient
import org.gorpipe.gor.model.{DriverBackedFileReader, GorOptions}
import org.gorpipe.gor.session.{GorSession, ProjectContext, SystemContext}
import org.gorpipe.gor.table.util.PathUtils
import org.junit.Assert
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters._

// White-box regression tests for the pgor gord-folder cache-hit path. Both bugs here only
// reproduce with a persistent/server file cache: the local LocalFileCacheClient resolves the
// cached link to the folder and re-runs the write, masking them -- so these tests inject a stub
// cache / craft cache links and drive GeneralQueryHandler/PGor directly.
//
// ENGKNOW-3656: `pgor ... | write x.gord` deleted the target gord folder even when the file cache
// already held the result and the write was skipped at execution time, leaving the skipped write
// with nothing to build the dictionary from. The fix gates the folder-delete in PGor.makeGorDict
// (and PartGor/Parallel) on the file-cache signature. Covered by the makeGorDict tests below.
//
// ENGKNOW-3670: on a cache hit the result cache hands GeneralQueryHandler.generateDictionaryFile a
// raw `<fp>.gord.link` path. Before the fix it checked project-root/thedict.gord (never present) so
// it always rebuilt, and rebuilt through the unresolved link, writing thedict.gord INSIDE the
// `.gord.link` file -> "Not a directory". The fix resolves the link, skips when thedict.gord is
// present, and targets the resolved folder in the fallback. Covered by the generateDictionaryFile
// tests below.
//
// Written as an AnyFunSuite because `:gortools:testScala` discovers tests via
// `org.scalatest.tools.Runner -R build/classes/scala/test`, which only picks up
// org.scalatest.Suite subclasses (matches the sibling convention: UTestMacroUtilities,
// UTestRegression).
@RunWith(classOf[JUnitRunner])
class UTestPGorGordFolderDelete extends AnyFunSuite {

  private val SIGNATURE = "SIGENGKNOW3656"

  /** Build a GorSession rooted at `root` whose file cache reports a hit for exactly `SIGNATURE`
    * (returning `cachedFile`) and a miss (null) otherwise -- mimicking a persistent server cache. */
  private def sessionWithStubCache(root: Path, cachedFile: String): GorSession = {
    val rootStr = root.toAbsolutePath.toString
    val requestId = "engknow3656"
    val session = new GorSession(requestId)
    val fileReader = new DriverBackedFileReader("", rootStr, System.currentTimeMillis())
    val stubCache = new LocalFileCacheClient(fileReader, rootStr) {
      override def lookupFile(fingerprint: String): String =
        if (fingerprint == SIGNATURE) cachedFile else null
    }
    val projectContext = new ProjectContext.Builder()
      .setRoot(rootStr)
      .setCacheDir(Files.createDirectories(root.resolve("result_cache")).toAbsolutePath.toString)
      .setFileReader(fileReader)
      .setFileCache(stubCache)
      .setQueryHandler(new GeneralQueryHandler(session.getGorContext, false))
      .setQueryEvaluator(new SessionBasedQueryEvaluator(session))
      .build()
    val systemContext = new SystemContext.Builder()
      .setReportBuilder(new FreemarkerReportBuilder(session))
      .setRunnerFactory(new GenericRunnerFactory())
      .setServer(false)
      .setStartTime(System.currentTimeMillis())
      .build()
    session.init(projectContext, systemContext, GorSessionCacheManager.getCache(requestId))
    session
  }

  /** Create an existing gord folder with a sentinel part file, then invoke PGor.makeGorDict.
    * The folder-delete decision runs before the split machinery (which needs reference data we
    * do not configure here), so we tolerate a later failure from makeGorDict and assert only the
    * delete side effect. Returns whether the sentinel (hence the folder) survived. */
  private def makeGorDictAndCheckFolder(session: GorSession, folder: Path): Boolean = {
    Files.createDirectories(folder)
    val sentinel = folder.resolve("sentinel.gorz")
    Files.createFile(sentinel)

    val create = ExecutionBlock(
      groupName = "[thepgorquery]",
      query = s"pgor <(gorrows -p chr1:0-10) | write ${folder.toAbsolutePath}",
      signature = SIGNATURE,
      dependencies = Array.empty[String])

    try {
      new PGor().makeGorDict(
        session.getGorContext,
        noWithin = false,
        createKey = "[thepgorquery]",
        create = create,
        replacePattern = "chr1",
        queryAppend = " <(gorrows -p chr1:0-10)",
        cachePath = PathUtils.markAsFolder(folder.toAbsolutePath.toString),
        useGordFolder = true,
        noDict = false,
        hasForkWrite = false)
    } catch {
      // The split/dictionary construction after the delete decision may fail in this minimal
      // session; the delete has already happened (or not) by then, which is what we assert.
      case _: Throwable => ()
    }

    Files.exists(sentinel)
  }

  test("makeGorDict does NOT delete the gord folder when the signature is cached") {
    val root = Files.createTempDirectory("uTestPGorGordFolderDelete")
    root.toFile.deleteOnExit()
    val folder = root.resolve("folder.gord")
    // Any non-null lookup result marks the signature as cached (fileCacheLookup only null-checks).
    val cachedFile = Files.createFile(root.resolve("cached.gorz")).toAbsolutePath.toString

    val survived = makeGorDictAndCheckFolder(sessionWithStubCache(root, cachedFile), folder)

    Assert.assertTrue("gord folder must be preserved when the result is cached", survived)
  }

  test("makeGorDict deletes the gord folder when the signature is not cached") {
    val root = Files.createTempDirectory("uTestPGorGordFolderDelete")
    root.toFile.deleteOnExit()
    val folder = root.resolve("folder.gord")

    // cachedFile = null -> lookupFile returns null -> cache miss -> folder is cleared for rewrite.
    val survived = makeGorDictAndCheckFolder(sessionWithStubCache(root, cachedFile = null), folder)

    Assert.assertFalse("gord folder must be deleted when the result is not cached", survived)
  }

  // ---- ENGKNOW-3670: generateDictionaryFile must not rebuild through the raw .gord.link ----

  /** Create a real gord folder and a `<name>.gord.link` file whose content is that folder's
    * absolute path (mimicking a result-cache link entry). Returns the link file path. */
  private def makeGordLink(root: Path, folderName: String, withDict: Boolean): Path = {
    val folder = Files.createDirectories(root.resolve(folderName))
    Files.createFile(folder.resolve("part_chr1.gorz"))
    if (withDict) Files.createFile(folder.resolve(GorOptions.DEFAULT_FOLDER_DICTIONARY_NAME))
    val link = root.resolve(folderName + ".link")
    Files.write(link, folder.toAbsolutePath.toString.getBytes(StandardCharsets.UTF_8))
    link
  }

  /** Absolute paths of every regular file under `root` whose name contains "-temp-". */
  private def tempFilesUnder(root: Path): List[Path] =
    scala.util.Using.resource(Files.walk(root)) { stream =>
      stream.iterator().asScala.filter(Files.isRegularFile(_))
        .filter(_.getFileName.toString.contains("-temp-")).toList
    }

  /** Walk `t`'s cause chain (bounded, to guard against cyclic causes) and concatenate every
    * class name + message, so an assertion can inspect the whole chain, not just the wrapper. */
  private def fullTrace(t: Throwable): String = {
    val sb = new StringBuilder
    var cur: Throwable = t
    var seen = 0
    while (cur != null && seen < 20) {
      sb.append(cur.getClass.getName).append(": ").append(cur.getMessage).append('\n')
      cur = cur.getCause
      seen += 1
    }
    sb.toString
  }

  test("generateDictionaryFile skips the rebuild on a cache hit (thedict.gord present): no crash, no temp under the link") {
    val root = Files.createTempDirectory("uTestGenDict3670Hit")
    root.toFile.deleteOnExit()
    val session = sessionWithStubCache(root, cachedFile = null)
    val handler = new GeneralQueryHandler(session.getGorContext, false)
    val link = makeGordLink(root, "cached.gord", withDict = true)

    // Must not throw (pre-fix: GorResourceException "Not a directory" from writing under the link).
    handler.generateDictionaryFile(
      commandToExecute = "GORDICTFOLDER [x] chr1",
      fileReader = session.getProjectContext.getFileReader,
      useMd5 = false,
      cacheFile = link.toAbsolutePath.toString)

    Assert.assertTrue("link file must remain a regular file", Files.isRegularFile(link))
    Assert.assertTrue("no -temp- dictionary file may be created", tempFilesUnder(root).isEmpty)
  }

  test("generateDictionaryFile fallback (thedict.gord missing) never writes under the .gord.link file") {
    val root = Files.createTempDirectory("uTestGenDict3670Miss")
    root.toFile.deleteOnExit()
    val session = sessionWithStubCache(root, cachedFile = null)
    val handler = new GeneralQueryHandler(session.getGorContext, false)
    val link = makeGordLink(root, "cached.gord", withDict = false)

    // The rebuild may fail for unrelated reasons in this minimal session (no real part blocks) --
    // that is fine. What must never happen is the ENGKNOW-3670 crash: pre-fix, the rebuild runs
    // through the raw `<fp>.gord.link` FILE, failing with a "Not a directory" error whose message /
    // cause chain names the `.gord.link` path. Capture the outcome so that crash surfaces.
    val outcome = scala.util.Try {
      handler.generateDictionaryFile(
        commandToExecute = "GORDICTFOLDER [x] chr1",
        fileReader = session.getProjectContext.getFileReader,
        useMd5 = false,
        cacheFile = link.toAbsolutePath.toString)
    }

    outcome.failed.foreach { t =>
      val trace = fullTrace(t)
      Assert.assertFalse(
        s"rebuild must never attempt to write through the raw .gord.link FILE path, but got:\n$trace",
        trace.contains(".gord.link"))
      Assert.assertFalse(
        s"rebuild must never fail with the ENGKNOW-3670 'Not a directory' crash, but got:\n$trace",
        trace.contains("Not a directory"))
    }

    Assert.assertTrue("link file must remain a regular file", Files.isRegularFile(link))
    val underLink = tempFilesUnder(root).filter(_.toString.contains(".gord.link"))
    Assert.assertTrue("no dictionary temp file may be created under the .gord.link path", underLink.isEmpty)
  }
}
