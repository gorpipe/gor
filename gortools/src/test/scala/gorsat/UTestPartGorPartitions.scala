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

package gorsat

import Macros.PartGor
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class UTestPartGorPartitions extends FunSuite {

  test("PGOR split test: parts") {
    val buckets = createBuckets(5, 100)

    val buckmap = PartGor.readDictionaryBucketTags(createPeonList(1,300), 1, buckets)
    val partitions = PartGor.groupTagsByBuckets(("parts",3), buckmap, server = false)

    assert(partitions.size == 3)

    partitions.foreach { t =>
      assert (t._2.length < 101)
    }
  }

  test("PGOR split test: parts with size larger than max bucket size") {
    val buckets = createBuckets(5, 100)

    val buckmap = PartGor.readDictionaryBucketTags(createPeonList(1,400), 1, buckets)
    val partitions = PartGor.groupTagsByBuckets(("parts",3), buckmap, server = false)

    assert(partitions.size == 3)

    // The peons should be evenly distributed among the buckets
    partitions.foreach { t =>
      assert (t._2.length < 135)
    }
  }

  test("PGOR split test: partsize") {
    val buckets = createBuckets(5, 100)

    val buckmap = PartGor.readDictionaryBucketTags(createPeonList(1,300), 1, buckets)
    val partitions = PartGor.groupTagsByBuckets(("partsize",50), buckmap, server = false)

    assert(partitions.size == 6)

    partitions.foreach { t =>
      assert (t._2.length < 51)
    }
  }

  test("PGOR split test: partsize larger than bucket size") {
    val buckets = createBuckets(5, 100)

    val buckmap = PartGor.readDictionaryBucketTags(createPeonList(1,500), 1, buckets)
    val partitions = PartGor.groupTagsByBuckets(("partsize",150), buckmap, server = false)

    assert(partitions.size == 3)

    partitions.foreach { t =>
      assert (t._2.length < 170)
    }
  }

  test("PGOR split test: partsize, uneven bucketsizes") {
    val buckets = createBuckets(5, 100)

    val buckmap = PartGor.readDictionaryBucketTags(createPeonList(30,270), 1, buckets)
    val partitions = PartGor.groupTagsByBuckets(("partsize",50), buckmap, server = false)

    assert(partitions.size == 5)

    partitions.foreach { t =>
      assert (t._2.length < 51)
    }
  }

  test("PGOR split test: partsize, one bucket not full") {
    val buckets = createBuckets(5, 100)

    val buckmap = PartGor.readDictionaryBucketTags(createPeonList(30,70), 1, buckets)
    val partitions = PartGor.groupTagsByBuckets(("partsize",50), buckmap, server = false)

    assert(partitions.size == 1)

    partitions.foreach { t =>
      assert (t._2.length < 42)
    }
  }

  test("PGOR split test: partscale") {
    val buckets = createBuckets(5, 100)

    val buckmap = PartGor.readDictionaryBucketTags(createPeonList(1,100), 1, buckets)
    val partitions = PartGor.groupTagsByBuckets(("partscale",0.5), buckmap, server = false)

    assert(partitions.size == 2)

    partitions.foreach { t =>
      assert (t._2.length < 51)
    }
  }

  def createBuckets(numberOfBuckets: Int, numberOfFilesPerBucket: Int): Array[String] = {
    var result = new scala.collection.mutable.ArrayBuffer[String]()
    var peonCount = 1

    for (bucketNumber <- 1 to numberOfBuckets) {
      for (fileNumber <- 1 to numberOfFilesPerBucket) {
        result += "File_" + bucketNumber + "_" + fileNumber + "|Bucket_" + bucketNumber + "\tPN_" + peonCount
        peonCount += 1
      }
    }

    result.toArray
  }

  def createPeonList(from: Int, to: Int): String = {
    var result = new scala.collection.mutable.ArrayBuffer[String]()

    for (pnNumber <- from to to)  {
      result += "PN_" + pnNumber
    }

    result.mkString(",")
  }
}
