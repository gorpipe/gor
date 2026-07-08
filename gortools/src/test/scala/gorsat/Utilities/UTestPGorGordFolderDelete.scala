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
import org.gorpipe.gor.model.DriverBackedFileReader
import org.gorpipe.gor.session.{GorSession, ProjectContext, SystemContext}
import org.gorpipe.gor.table.util.PathUtils
import org.junit.Assert
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner

import java.nio.file.{Files, Path}

// Regression test for ENGKNOW-3656: `pgor ... | write x.gord` deleted the target gord folder
// even when the file cache already held the result and the write was skipped at execution time,
// leaving the skipped write with nothing to build the dictionary from. The fix gates the
// folder-delete in PGor.makeGorDict (and PartGor/Parallel) on the file-cache signature.
//
// The original crash only reproduces with a persistent/server file cache (the local
// LocalFileCacheClient resolves the cached link to the folder and re-runs the write, masking
// the bug). So this white-box test injects a stub file cache that reports a cache hit for the
// signature and asserts the delete decision directly.
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
}
