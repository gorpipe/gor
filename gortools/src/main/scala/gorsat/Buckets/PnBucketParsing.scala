package gorsat.Buckets

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object PnBucketParsing {

  def parse(tbSource: Iterable[String]): PnBucketTable = {
    val bucketToIdx = mutable.Map.empty[String, Int]
    val bucketIdxToName = ArrayBuffer.empty[String]
    val bucketCounts = ArrayBuffer.empty[Int]
    val pnToIdx = mutable.Map.empty[String, Int]
    val pnIdxToName = ArrayBuffer.empty[String]
    val pnIdxToBucketIdx = ArrayBuffer.empty[Int]
    val pnIdxToBucketPos = ArrayBuffer.empty[Int]

    for (line <- tbSource) {
      val tabIdx = line.indexOf('\t')
      val (pn, bucket) = (line.substring(0, tabIdx), line.substring(tabIdx + 1))
        pnToIdx += (pn -> pnToIdx.size)
        pnIdxToName += pn

        val bucketIdx = bucketToIdx.get(bucket) match {
          case Some(idx) => idx
          case None =>
            val idx = bucketToIdx.size
            bucketCounts += 0
            bucketIdxToName += bucket
            bucketToIdx += (bucket -> idx)
            idx
        }
        pnIdxToBucketIdx += bucketIdx
        pnIdxToBucketPos += bucketCounts(bucketIdx)
        bucketCounts(bucketIdx) += 1

    }

    new PnBucketTable(bucketToIdx.toMap, bucketIdxToName.toArray, bucketCounts.toArray,
      pnToIdx.toMap, pnIdxToName.toArray, pnIdxToBucketIdx.toArray, pnIdxToBucketPos.toArray)
  }
}

class PnBucketTable(val buckNameToIdx: Map[String, Int], val buckIdxToName: Array[String], val buckIdxToBuckSize: Array[Int],
                    val pnToIdx: Map[String, Int], val pnIdxToName: Array[String], val pnIdxToBuckIdx: Array[Int], val pnIdxToBuckPos: Array[Int],
                    validator: String => Boolean = PnBucketValidators.passThrough) {
  val numberOfBuckets: Int = buckNameToIdx.size
  val numberOfPns: Int = pnIdxToName.length


  def getBucketIdxFromName(bucketName: String): Int = buckNameToIdx.get(bucketName) match {
    case Some(idx) => idx
    case None => throw new IllegalArgumentException(s"Unknown bucket $bucketName")
  }

  def getBucketNameFromIdx(bucketIdx: Int): String = buckIdxToName(bucketIdx)

  def getPnIdx(pn: String): Int = pnToIdx.get(pn) match {
    case Some(idx) => if (validator(pn)) idx else -1
    case None => throw new IllegalArgumentException(s"Unknown pn $pn")
  }

  def getPnIdxSafe(pn: String): Int = pnToIdx.get(pn) match {
    case Some(idx) => if (validator(pn)) idx else -1
    case None => -1
  }

  def getPnNameFromIdx(pnIdx: Int): String = pnIdxToName(pnIdx)

  def getBucketIdxFromPn(pnIdx: Int): Int = pnIdxToBuckIdx(pnIdx)

  def getBucketIdxFromPn(pn: String): Int = pnIdxToBuckIdx(getPnIdx(pn))

  def getBucketNameFromPn(pnIdx: Int): String = buckIdxToName(pnIdxToBuckIdx(pnIdx))

  def getBucketNameFromPn(pn: String): String = buckIdxToName(pnIdxToBuckIdx(getPnIdx(pn)))

  def getBucketSize(bucketIdx: Int): Int = buckIdxToBuckSize(bucketIdx)

  def getBucketSize(bucket: String): Int = buckIdxToBuckSize(getBucketIdxFromName(bucket))

  def getBucketPos(pnIdx: Int): Int = pnIdxToBuckPos(pnIdx)

  def getBucketPos(pn: String): Int = pnIdxToBuckPos(getPnIdx(pn))

  def filter(pns: Iterable[String], validator: String => Boolean = PnBucketValidators.deleted): PnBucketTable = {
    val pnsFilt = ArrayBuffer.empty[String]
    val buckToIdxFilt = mutable.Map.empty[String, Int]
    val buckIdxToNameFilt = ArrayBuffer.empty[String]
    val buckSizes = ArrayBuffer.empty[Int]
    val pnToIdxFilt = mutable.Map.empty[String, Int]
    val pnIdxToBuckIdxFilt = ArrayBuffer.empty[Int]
    val pnIdxToBuckPosFilt = ArrayBuffer.empty[Int]

    for (pn <- pns) {
      if (validator(pn)) {
        pnsFilt += pn

        val oldTagIdx = getPnIdx(pn)
        val oldBucketIdx = getBucketIdxFromPn(oldTagIdx)
        val bucket = getBucketNameFromIdx(oldBucketIdx)
        val bucketIdx = buckToIdxFilt.get(bucket) match {
          case Some(idx) => idx
          case None =>
            val idx = buckToIdxFilt.size
            buckSizes += buckIdxToBuckSize(oldBucketIdx)
            buckIdxToNameFilt += bucket
            buckToIdxFilt += (bucket -> idx)
            idx
        }
        pnIdxToBuckIdxFilt += bucketIdx
        pnIdxToBuckPosFilt += getBucketPos(oldTagIdx)
        pnToIdxFilt += (pn -> pnToIdxFilt.size)
      }
    }

    new PnBucketTable(buckToIdxFilt.toMap, buckIdxToNameFilt.toArray, buckSizes.toArray,
      pnToIdxFilt.toMap, pnsFilt.toArray, pnIdxToBuckIdxFilt.toArray, pnIdxToBuckPosFilt.toArray,
      PnBucketValidators.deleted)
  }

  /**
   * Indexes the pns such that first come the pns in the first bucket, than the pns in the second bucket and so on.
   *
   * Once the table has been filtered, the functionality of this method is undefined.
   */
  def indexByBucket(): PnBucketTable = {
    val newPnToIdx = mutable.Map.empty[String, Int]
    val newPnIdxToName = new Array[String](numberOfPns)
    val newPnIdxToBuckIdx = new Array[Int](pnIdxToBuckIdx.length)
    val newPnIdxToBuckPos = new Array[Int](pnIdxToBuckPos.length)
    val bucketOffsets = buckIdxToBuckSize.iterator.take(numberOfBuckets - 1)
      .scanLeft(0)((sum, curr) => sum + curr).toArray
    for ((pn, pnIdx) <- pnToIdx) {
      val bucketIdx = getBucketIdxFromPn(pnIdx)
      val bucketPos = getBucketPos(pnIdx)
      val newPnIdx = bucketOffsets(bucketIdx) + bucketPos
      newPnToIdx += (pn -> newPnIdx)
      newPnIdxToName(newPnIdx) = pn
      newPnIdxToBuckIdx(newPnIdx) = bucketIdx
      newPnIdxToBuckPos(newPnIdx) = bucketPos
    }

    new PnBucketTable(buckNameToIdx, buckIdxToName, buckIdxToBuckSize,
      newPnToIdx.toMap, newPnIdxToName, newPnIdxToBuckIdx, newPnIdxToBuckPos)
  }
}
