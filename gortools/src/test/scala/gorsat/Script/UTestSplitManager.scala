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

package gorsat.Script

import gorsat.DynIterator
import gorsat.process.{GenericSessionFactory, PipeInstance, PipeOptions, TestSessionFactory}
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.{GorContext, ReferenceBuildDefaults}
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner


@RunWith(classOf[JUnitRunner])
class UTestSplitManager extends FunSuite {

  def createDefaultSplitManager(): SplitManager = {
    val splits = SplitManager.parseBuildSizeSplit(ReferenceBuildDefaults.buildSizeGeneric)
    SplitManager("Foo", splits, SplitManager.REGULAR_REPLACEMENT_PATTERN)
  }

  def createHg38SplitManager(): SplitManager = {
    val splits = SplitManager.parseSplitSizeSplit(ReferenceBuildDefaults.buildSizeHg38(), ReferenceBuildDefaults.buildSplitHg38())
    SplitManager("Bar", splits, SplitManager.SPLIT_REPLACEMENT_PATTERN)
  }

  def createCustomSplitManager(splitSize: Int, overlap: Int = 0): SplitManager = {
    val splits = SplitManager.parseArbitrarySplit(ReferenceBuildDefaults.buildSizeGeneric, splitSize, overlap)
    SplitManager("Car", splits, SplitManager.SPLIT_REPLACEMENT_PATTERN)
  }

  def createNestedSplitManager(query: String, context: GorContext): SplitManager = {
    val splits = SplitManager.parseNestedSplit(context, query)
    SplitManager("Car", splits, SplitManager.SPLIT_REPLACEMENT_PATTERN)
  }

  test("Initialize SplitManager with the default split") {
    val manager = createDefaultSplitManager()
    assert(manager.chromosomeSplits.size == 26)
  }

  test("Expand gor query with default split") {
    val manager = createDefaultSplitManager()
    val commandGroup = manager.expandCommand("gor [xxx] | top 10", "xxx")

    assert(commandGroup.commandEntries.length == 1)
    assert(commandGroup.commandEntries.head.query == "gor [xxx] | top 10")
  }

  test("Expand pgor query with default split but no split replacement pattern") {
    val manager = createDefaultSplitManager()
    val commandGroup = manager.expandCommand("pgor [xxx] | top 10", "xxx")

    assert(commandGroup.commandEntries.length == 1)
    assert(commandGroup.commandEntries.head.query == "pgor [xxx] | top 10")
  }

  test("Expand gor query with default split with replacement pattern") {
    val manager = createDefaultSplitManager()
    val commandGroup = manager.expandCommand("gor -p " + SplitManager.REGULAR_REPLACEMENT_PATTERN
      + " [xxx] | top 10", "xxx")

    assert(commandGroup.commandEntries.length == 26)
    assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chr1:0-250000000 [xxx] | top 10"))
    assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chr16:0-100000000 [xxx] | top 10"))
  }

  test("Initialize SplitManager with the hg38 split") {
    val manager = createHg38SplitManager()
    assert(manager.chromosomeSplits.size == 39)
  }

  test("Expand gor query with hg38 split with replacement pattern") {
    val manager = createHg38SplitManager()
    val commandGroup = manager.expandCommand("gor -p " + SplitManager.SPLIT_REPLACEMENT_PATTERN
      + " [xxx] | top 10", "xxx")

    assert(commandGroup.commandEntries.length == 39)
    assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chr1:0-123399999 [xxx] | top 10"))
    assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chr1:123400000-248956422 [xxx] | top 10"))
    assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chr16:0-90338345 [xxx] | top 10"))
  }

  test("Expand with custom split (below 1000) on default build size") {
    val manager = createCustomSplitManager(100)
    val commandGroup = manager.expandCommand("gor -p " + SplitManager.SPLIT_REPLACEMENT_PATTERN
      + " [xxx] -split 100 | top 10", "xxx")

    assert(commandGroup.commandEntries.length == 134)
    assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chr1:0-29999999 [xxx] | top 10"))
    assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chr1:30000000-59999999 [xxx] | top 10"))
    assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chr22:0-29999999 [xxx] | top 10"))
  }

  test("Expand with custom split (above 1000) on default build size") {
    val manager = createCustomSplitManager(5000000)
    val commandGroup = manager.expandCommand("gor -p " + SplitManager.SPLIT_REPLACEMENT_PATTERN
      + " [xxx] -split 5000000 | top 10", "xxx")

    assert(commandGroup.commandEntries.length == 742)
    assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chr1:0-4999999 [xxx] | top 10"))
    assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chr1:5000000-9999999 [xxx] | top 10"))
    assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chr22:70000000-74999999 [xxx] | top 10"))
  }

  test("Expand with custom split (above 1000) on default build size with overlap") {
    val manager = createCustomSplitManager(5000000, 1000000)
    val commandGroup = manager.expandCommand("gor -p " + SplitManager.SPLIT_REPLACEMENT_PATTERN
      + " [xxx] -split 5000000 | top 10", "xxx")

    assert(commandGroup.commandEntries.length == 742)
    assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chr1:0-5999999 [xxx] | top 10"))
    assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chr1:4000000-10999999 [xxx] | top 10"))
    assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chr22:69000000-75999999 [xxx] | top 10"))
  }

  test("Expand split with custom query") {
    val options = new PipeOptions()
    options.parseOptions(Array.empty[String])
    val factory = new TestSessionFactory(options, null, false)
    val context = factory.create().getGorContext
    DynIterator.createGorIterator = PipeInstance.createGorIterator
    val manager = createNestedSplitManager("<(pgor ../tests/data/gor/genes.gor | top 1 | select 1-3 | signature -timeres 1)", context)
    val commandGroup = manager.expandCommand("gor -p " + SplitManager.SPLIT_REPLACEMENT_PATTERN
      + " [xxx] -split <(pgor ../tests/data/gor/genes.gor | top 1 | select 1-3) | top 10", "xxx")

    assert(commandGroup.commandEntries.length == 25)
    assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chr1:11868-14412 [xxx] | top 10"))
    assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chr10:60000-60544 [xxx] | top 10"))
    assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chr11:75779-76143 [xxx] | top 10"))
    assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chr12:67606-69138 [xxx] | top 10"))
    assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chr13:19041311-19059588 [xxx] | top 10"))
    assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chr14:19109938-19118336 [xxx] | top 10"))
    assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chr15:20083768-20093074 [xxx] | top 10"))
    assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chr16:61552-64093 [xxx] | top 10"))
    assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chr17:4960-5048 [xxx] | top 10"))
    assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chr18:11102-15928 [xxx] | top 10"))
    assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chr19:60104-70966 [xxx] | top 10"))
    assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chr2:38813-46870 [xxx] | top 10"))
    assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chr20:68350-77174 [xxx] | top 10"))
    assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chr21:9683190-9683272 [xxx] | top 10"))
    assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chr22:16062156-16063236 [xxx] | top 10"))
    assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chr3:65430-66175 [xxx] | top 10"))
    assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chr4:48990-50018 [xxx] | top 10"))
    assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chr5:58312-59030 [xxx] | top 10"))
    assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chr6:105918-106856 [xxx] | top 10"))
    assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chr7:19756-35479 [xxx] | top 10"))
    assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chr8:14090-14320 [xxx] | top 10"))
    assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chr9:11055-11620 [xxx] | top 10"))
    assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chrM:576-647 [xxx] | top 10"))
    assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chrX:170409-172712 [xxx] | top 10"))
    assert(commandGroup.commandEntries.map(x => x.query).contains("gor -p chrY:2654895-2655740 [xxx] | top 10"))
  }

  test("Too many splits") {
    val context = new GenericSessionFactory().create().getGorContext
    DynIterator.createGorIterator = PipeInstance.createGorIterator
    val thrown = intercept[GorParsingException](createNestedSplitManager("<(gor ../tests/data/gor/genes.gor | top 6000)", context))

    assert(thrown.getMessage.startsWith("Too many splits for query."))
  }

  test("Basic query with normal split") {
    assert(SplitManager.useWholeChromosomeSplit("pgor #dbsnp# | top 100") == false)
  }

  test("Query with that forces whole chrome split") {
    assert(SplitManager.useWholeChromosomeSplit("pgor #dbsnp# | rank 1000000 pos") == true)
  }

  ignore("Basic query with normal split but command name (that forces whole chrome split) in text") {
    assert(SplitManager.useWholeChromosomeSplit("pgor #dbsnp# | calc somefield = 'Rank this high' | top 100") == false)
  }

  ignore("Basic query with normal split but command name (that forces whole chrome split) in field name") {
    assert(SplitManager.useWholeChromosomeSplit("pgor #dbsnp# | calc rank = 'some text' | top 100") == false)
  }

}
