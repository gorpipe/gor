package gorsat

import gorsat.Buckets.PnBucketParsing
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class UTestPnBucketParsing extends AnyFunSuite {

  test("test parse") {
    val tbSource = Range.inclusive(1, 100).map(i => s"PN$i\tBUCKET${(i - 1) / 10 + 1}").toList
    val pnBucketTable = PnBucketParsing.parse(tbSource)

    for ((pn, pnIdx) <- Range(0, 100).map(i => (s"PN${i+1}", i))) {
      assert(pnBucketTable.getPnIdx(pn) == pnIdx)

      val bucketIdx = pnIdx / 10
      assert(bucketIdx == pnBucketTable.getBucketIdxFromPn(pn))
      assert(bucketIdx == pnBucketTable.getBucketIdxFromPn(pnIdx))

      val bucketName = s"BUCKET${bucketIdx + 1}"
      assert(bucketName == pnBucketTable.getBucketNameFromPn(pn))
      assert(bucketName == pnBucketTable.getBucketNameFromPn(pnIdx))

      val bucketPos = pnIdx % 10
      assert(bucketPos == pnBucketTable.getBucketPos(pn))
      assert(bucketPos == pnBucketTable.getBucketPos(pnIdx))
    }

    for ((bucket, bucketIdx) <- Range(0, 10).map(i => (s"BUCKET${i+1}", i))) {
      assert(bucketIdx == pnBucketTable.getBucketIdxFromName(bucket))
      assert(bucket == pnBucketTable.getBucketNameFromIdx(bucketIdx))
      assert(10 == pnBucketTable.getBucketSize(bucket))
      assert(10 == pnBucketTable.getBucketSize(bucketIdx))
    }
  }

  test("test parse 2") {
    val tbSource = Range(0, 100).map(i => s"PN${i+1}\tBUCKET${i%10 + 1}").toList
    val pnBucketTable = PnBucketParsing.parse(tbSource)

    for ((pn, pnIdx) <- Range(0, 100).map(i => (s"PN${i+1}", i))) {
      assert(pnBucketTable.getPnIdx(pn) == pnIdx)

      val bucketIdx = pnIdx % 10
      assert(bucketIdx == pnBucketTable.getBucketIdxFromPn(pn))
      assert(bucketIdx == pnBucketTable.getBucketIdxFromPn(pnIdx))

      val bucketName = s"BUCKET${bucketIdx + 1}"
      assert(bucketName == pnBucketTable.getBucketNameFromPn(pn))
      assert(bucketName == pnBucketTable.getBucketNameFromPn(pnIdx))

      val bucketPos = pnIdx / 10
      assert(bucketPos == pnBucketTable.getBucketPos(pn))
      assert(bucketPos == pnBucketTable.getBucketPos(pnIdx))
    }

    for ((bucket, bucketIdx) <- Range(0, 10).map(i => (s"BUCKET${i+1}", i))) {
      assert(bucketIdx == pnBucketTable.getBucketIdxFromName(bucket))
      assert(bucket == pnBucketTable.getBucketNameFromIdx(bucketIdx))
      assert(10 == pnBucketTable.getBucketSize(bucket))
      assert(10 == pnBucketTable.getBucketSize(bucketIdx))
    }
  }

  test("test index") {
    val tbSource = Range(0, 100).map(i => s"PN${i+1}\tBUCKET${i%10 + 1}").toList
    val pnBucketTable = PnBucketParsing.parse(tbSource).indexByBucket()

    for ((pn, oldPnIdx, newPnIdx) <- Range(0, 100).map(i => (s"PN${i+1}", i, 10 * (i % 10) + i / 10))) {
      assert(pnBucketTable.getPnIdx(pn) == newPnIdx)

      val bucketIdx = oldPnIdx % 10
      assert(bucketIdx == pnBucketTable.getBucketIdxFromPn(pn))
      assert(bucketIdx == pnBucketTable.getBucketIdxFromPn(newPnIdx))

      val bucketName = s"BUCKET${bucketIdx + 1}"
      assert(bucketName == pnBucketTable.getBucketNameFromPn(pn))
      assert(bucketName == pnBucketTable.getBucketNameFromPn(newPnIdx))

      val bucketPos = oldPnIdx / 10
      assert(bucketPos == pnBucketTable.getBucketPos(pn))
      assert(bucketPos == pnBucketTable.getBucketPos(newPnIdx))
    }
  }

  test("test filter") {
    val tbSource = Range(0, 100).map(i => s"PN${i+1}\tBUCKET${i / 10 + 1}").toList
    val pns = Range(0, 10).map(i => s"PN${10 * i + 7}")
    val pnBucketTable = PnBucketParsing.parse(tbSource).filter(pns)
    for ((pn, pnIdx, bucketIdx) <- Range(0,10).map(i => (s"PN${10 * i + 7}", i, i))) {
      assert(pnBucketTable.getPnIdx(pn) == pnIdx)

      assert(bucketIdx == pnBucketTable.getBucketIdxFromPn(pn))
      assert(bucketIdx == pnBucketTable.getBucketIdxFromPn(pnIdx))

      val bucketName = s"BUCKET${bucketIdx + 1}"
      assert(bucketName == pnBucketTable.getBucketNameFromPn(pn))
      assert(bucketName == pnBucketTable.getBucketNameFromPn(pnIdx))

      assert(6 == pnBucketTable.getBucketPos(pn))
      assert(6 == pnBucketTable.getBucketPos(pnIdx))
    }
  }

  test("test filter 2") {
    val tbSource = Range(0, 100).map(i => s"PN${i+1}\tBUCKET${i / 10 + 1}").toList
    val pns = Range(0, 10).map(i => s"PN${i + 1}")
    val pnBucketTable = PnBucketParsing.parse(tbSource).filter(pns)
    for ((pn, pnIdx) <- Range(0,10).map(i => (s"PN${i + 1}", i))) {
      assert(pnBucketTable.getPnIdx(pn) == pnIdx)

      assert(0 == pnBucketTable.getBucketIdxFromPn(pn))
      assert(0 == pnBucketTable.getBucketIdxFromPn(pnIdx))

      val bucketName = "BUCKET1"
      assert(bucketName == pnBucketTable.getBucketNameFromPn(pn))
      assert(bucketName == pnBucketTable.getBucketNameFromPn(pnIdx))

      assert(pnIdx == pnBucketTable.getBucketPos(pn))
      assert(pnIdx == pnBucketTable.getBucketPos(pnIdx))
    }
  }
}
