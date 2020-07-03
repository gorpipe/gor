package gorsat.Analysis

import gorsat.Commands.Analysis
import gorsat.gorsatGorIterator.MapAndListUtilities
import gorsat.gtgen.GPParser.parseDoubleTriplet
import gorsat.gtgen.GPParser.glToGp
import gorsat.gtgen.GPParser.plToGp
import gorsat.gtgen.GTGen
import org.gorpipe.gor.GorContext
import org.gorpipe.model.genome.files.gor.Row
import org.gorpipe.model.gor.RowObj
import org.gorpipe.model.gor.RowObj.BinaryHolder
import org.gorpipe.model.gor.iterators.{LineIterator, RowSource}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object PrGtGenAnalysis {

  case class LeftSourceAnalysis(context: GorContext, lookupSignature: String,
                                fileName: String, iteratorCommand: String, iterator: LineIterator,
                                plCol: Int, glCol: Int, gpCol: Int, crCol: Int, dCol: Int, pnCol: Int, grCols: List[Int],
                                af: Double = 0.05, error: Double, tripSep: Char = ';') extends Analysis {

    val grColsArray = grCols.toArray
    var lastChr = ""
    var lastPos = -1
    var firstRow = true

    val addToGtGen = if (crCol != -1 && dCol != -1) {
      (r: Row, idx: Int, gtGen: GTGen) => {
        val d = r.colAsInt(dCol)
        val c = math.round(d * r.colAsDouble(crCol)).toInt
        gtGen.addData(idx, c, d)
      }
    } else {
      (r: Row, idx: Int, gtGen: GTGen) => {
        val ps = getTriple(r)
        gtGen.addData(idx, ps(0), ps(1), ps(2))
      }
    }

    lazy val getTriple = if (gpCol != -1) {
      (r: Row) => parseDoubleTriplet(r.colAsString(gpCol), tripSep)
    } else if (plCol != -1) {
      (r: Row) => plToGp(r.colAsString(plCol), tripSep)
    } else {
      (r: Row) => glToGp(r.colAsString(glCol), tripSep)
    }

    override def process(r: Row): Unit = {
      if (!firstRow && (lastChr != r.chr || lastPos != r.pos)) flush()
      if (firstRow) firstRow = false
      lastChr = r.chr
      lastPos = r.pos
      val gh = getGroupHolder(r)
      val pn = r.colAsString(pnCol).toString
      if (pn != "") { //If pn == "" then this is a dummy row for a rare variant.
        val tagIdx = ti.getTagIdx(pn)
        addToGtGen(r, tagIdx, gh.gtGen)
      }
    }

    def flush(): Unit = {
      for ((groupId, gh) <- groupMap.toList.sortBy(_._1)) {
        val rowString = if (grCols.isEmpty) lastChr + "\t" + lastPos
        else lastChr + "\t" + lastPos + "\t" + groupId
        val rowToSend = RowObj(rowString, gh)
        super.process(rowToSend)
      }
      groupMap.clear()
    }

    override def finish(): Unit = {
      if (!this.isInErrorState) flush()
      ti.unregisterUser(this)
      if (iterator != null) iterator.close()
    }

    val groupMap = mutable.Map.empty[String, GroupHolder]

    private def getGroupHolder(r: Row): GroupHolder = {
      val groupId = if (grColsArray.isEmpty) "theOnlyGroup" else r.selectedColumns(grColsArray)
      groupMap.get(groupId) match {
        case Some(gh) => gh
        case None =>
          val gh = getNewGroupHolder
          groupMap += (groupId -> gh)
          gh
      }
    }

    def getNewGroupHolder: GroupHolder = {
      val gh = GroupHolder(new GTGen(error, ti.numberOfSamples))
      gh.gtGen.setAF(af)
      gh
    }

    val ti = context.getSession.getCache.getObjectHashMap.computeIfAbsent(lookupSignature, _ => {
      val l = {
        try {
          if (iteratorCommand != "") MapAndListUtilities.getStringArray(iteratorCommand, iterator, context.getSession)
          else MapAndListUtilities.getStringArray(fileName, context.getSession)
        } catch {
          case e: Exception =>
            iterator.close()
            throw e
        }
      }
      val bucketToIdxAndCounter = mutable.Map.empty[String, (Int, Int)]
      val tagBuckPosMap = new mutable.HashMap[String, (Int, Int)]
      l.foreach(line => {
        val tabIdx = line.indexOf('\t')
        val (tag, bucket) = (line.substring(0, tabIdx), line.substring(tabIdx + 1))
        val (bucketIdx, idxInBucket) = bucketToIdxAndCounter.get(bucket) match {
          case Some((idx, count)) => (idx, count)
          case None => (bucketToIdxAndCounter.size, 0)
        }
        tagBuckPosMap += (tag -> (bucketIdx, idxInBucket))
        bucketToIdxAndCounter += (bucket -> (bucketIdx, idxInBucket + 1))
      })
      val numBuckets = bucketToIdxAndCounter.size
      val buckIdBuckName = new Array[String](numBuckets)
      val buckIdBuckSize = new Array[Int](numBuckets)
      for ((bucket, (idx, size)) <- bucketToIdxAndCounter) {
        buckIdBuckName(idx) = bucket
        buckIdBuckSize(idx) = size
      }
      TagInfo(tagBuckPosMap, buckIdBuckName, buckIdBuckSize, l.length)
    }).asInstanceOf[TagInfo]
    ti.registerUser(this)
  }

  case class GroupHolder(gtGen: GTGen) extends BinaryHolder

  case class TagInfo(tagBuckPosMap: mutable.Map[String, (Int, Int)],
                     buckIdBuckName: Array[String],
                     buckIdBuckSize: Array[Int],
                     numberOfSamples: Int) {

    val buckSums = buckIdBuckSize.scanLeft(0)((sum, curr) => sum + curr)

    def getTagIdx(tag: String): Int = {
      tagBuckPosMap.get(tag) match {
        case Some((bucketIdx, pos)) => buckSums(bucketIdx) + pos
        case None => throw new IllegalArgumentException("Unknown tag:\t" + tag)
      }
    }

    val users = mutable.Set.empty[AnyRef]

    def registerUser(user: AnyRef): Unit = users += user

    def unregisterUser(user: AnyRef): Unit = users -= user
  }

  case class AFANSourceAnalysis(afSource: RowSource, context: GorContext, lookupSignature: String, grCols: List[Int], afCol: Int, anCol: Int) extends Analysis {
    val ti = context.getSession.getCache.getObjectHashMap.get(lookupSignature).asInstanceOf[TagInfo]
    ti.registerUser(this)

    var lastChr = ""
    var lastPos = -1
    var firstRow = true

    val grColsArray = grCols.toArray

    case class AFANRow(chr: String, pos: Int, af: Double, an: Int)

    var rightBatch: Map[String, AFANRow] = Map.empty
    var offsetRow: (String, AFANRow) = _

    def refreshRightBatch(): Unit = {
      rightBatch = Map.empty
      afSource.moveToPosition(lastChr, lastPos)
      if (offsetRow != null && offsetRow._2.chr == lastChr && offsetRow._2.pos == lastPos) {
        rightBatch += (offsetRow._1 -> offsetRow._2)
        offsetRow = null
      }
      var keepOn = true
      while (keepOn && afSource.hasNext) {
        val r = afSource.next()
        val afanRow = getAFANRow(r)
        val groupId = getGroupId(r)
        if (afanRow.chr == lastChr && afanRow.pos == lastPos) {
          rightBatch += (groupId -> afanRow)
        } else {
          offsetRow = (groupId, afanRow)
          keepOn = false
        }
      }
    }

    def getAFANRow(r: Row): AFANRow = {
      AFANRow(r.chr, r.pos, r.colAsDouble(afCol), r.colAsInt(anCol))
    }

    def getGroupId(r: Row): String = {
      if (grColsArray.isEmpty) "theOnlyGroup" else r.selectedColumns(grColsArray)
    }

    override def process(r: Row): Unit = {
      if (r.chr != lastChr || r.pos != lastPos) {
        lastChr = r.chr
        lastPos = r.pos
        refreshRightBatch()
      }
      val groupId = getGroupId(r)
      rightBatch.get(groupId) match {
        case Some(afanRow) => {
          val gh = r.bH.asInstanceOf[GroupHolder]
          gh.gtGen.setPrior(afanRow.af, afanRow.an)
        }
        case None => //Do nothing
      }
      super.process(r)
    }

    override def finish(): Unit = {
      ti.unregisterUser(this)
      try {
        afSource.close()
      } catch {
        case _: Exception => //Don't give a fuck
      }
    }
  }

  case class RightSourceAnalysis(rightSource: RowSource, context: GorContext, lookupSignature: String,
                                 depthCol: Int, pnCol: Int, threshold: Double, maxSegSize: Int, maxIt: Int = 20, tol: Double = 1e-5,
                                 tripSep: Char = ';', sepOut: Boolean = false, outSep: Char = ',') extends Analysis {

    val ti = context.getSession.getCache.getObjectHashMap.get(lookupSignature).asInstanceOf[TagInfo]
    ti.registerUser(this)

    val gts = new Array[Double](3 * ti.numberOfSamples)

    case class SegmentRowWithDepthAndPn(chr: String, begin: Int, end: Int, depth: Int, pn: String) {
      val pnIdx = ti.getTagIdx(pn)
    }

    var currentRowBuffer = new ArrayBuffer[SegmentRowWithDepthAndPn]()
    var nextRowBuffer = new ArrayBuffer[SegmentRowWithDepthAndPn]()

    override def process(r: Row): Unit = {
      val r_gh = r.bH.asInstanceOf[GroupHolder]
      val gtGen = r_gh.gtGen
      join(r, gtGen)

      val converged = gtGen.impute(gts, tol, maxIt)
      writeOutRows(r, r_gh, gtGen, converged)
    }

    private def writeOutRows(r: Row, r_gh: GroupHolder, gtGen: GTGen, converged: Boolean): Unit = {
      if (converged) {
        val rowPrefix = r.toString + '\t' + "%5.5g\t%d".format(gtGen.getAF, gtGen.getAn) + '\t'
        var bucketIdx = 0
        val len = ti.buckIdBuckSize.length
        var sampleIdx: Int = 0
        while (bucketIdx < len) {
          val bucketSize = ti.buckIdBuckSize(bucketIdx)
          val bucketName = ti.buckIdBuckName(bucketIdx)
          val sb = new mutable.StringBuilder(getStringBuilderSizeHint(rowPrefix, bucketName, bucketSize))
          sb ++= rowPrefix
          sb ++= ti.buckIdBuckName(bucketIdx)
          sb += '\t'
          writeGenotypes(sb, sampleIdx, sampleIdx + bucketSize, r_gh.gtGen)
          sampleIdx += bucketSize
          super.process(RowObj(sb.toString))
          bucketIdx += 1
        }
      } else {
        val rowPrefix = r.toString + "\t.\t.\t"
        ti.buckIdBuckName.foreach(bucket => super.process(RowObj(rowPrefix + bucket + "\t")))
      }
    }

    val getStringBuilderSizeHint = if (threshold == -1) {
      if (sepOut) {
        (prefix: String, bucketName: String, bucketSize: Int) => {
          prefix.length + bucketName.length + 21 * bucketSize + 2
        }
      } else {
        (prefix: String, bucketName: String, bucketSize: Int) => {
          prefix.length + bucketName.length + 2 * bucketSize + 2
        }
      }
    } else {
      if (sepOut) {
        (prefix: String, bucketName: String, bucketSize: Int) => {
          prefix.length + bucketName.length + 2 * bucketSize + 2
        }
      } else {
        (prefix: String, bucketName: String, bucketSize: Int) => {
          prefix.length + bucketName.length + bucketSize + 2
        }
      }
    }

    val writeGenotypes = if (threshold == -1) {
      if (sepOut) {
        (sb: mutable.StringBuilder, offset: Int, len: Int, gtGen: GTGen) => {
          writeGtsTriplets(sb, offset, len, gtGen)
        }
      } else {
        (sb: mutable.StringBuilder, offset: Int, len: Int, gtGen: GTGen) => {
          writeGtsEncoded(sb, offset, len, gtGen)
        }
      }
    } else {
      if (sepOut) {
        (sb: mutable.StringBuilder, offset: Int, len: Int, gtGen: GTGen) => {
          writeHcSep(sb, offset, len, gtGen)
        }
      } else {
        (sb: mutable.StringBuilder, offset: Int, len: Int, gtGen: GTGen) => {
          writeHc(sb, offset, len, gtGen)
        }
      }
    }

    private def join(r: Row, gtGen: GTGen): Unit= {
      var idx = 0
      while (idx < currentRowBuffer.length && currentRowBuffer(idx).chr < r.chr) idx += 1

      joinWithBuffer(r, gtGen, idx)
      joinWithSource(r, gtGen)
      swapRowBuffers()
    }

    def joinWithSource(r: Row, gtGen: GTGen): Unit = {
      rightSource.moveToPosition(r.chr, (r.pos - maxSegSize + 1).max(0))
      var keepOn = true
      while (keepOn && rightSource.hasNext) {
        val rr = rightSource.next()
        val sr = SegmentRowWithDepthAndPn(rr.chr, rr.pos, rr.colAsInt(2), rr.colAsInt(depthCol), rr.colAsString(pnCol).toString)
        keepOn = matchWithSegRow(r, sr, gtGen)
      }
    }

    def matchWithSegRow(r: Row, sr: SegmentRowWithDepthAndPn, gtGen: GTGen): Boolean = {
      var toReturn = true
      if (sr.chr == r.chr) {
        if (r.pos <= sr.end) {
          if (sr.begin <= r.pos) {
            if (!gtGen.hasCoverage(sr.pnIdx)) gtGen.addData(sr.pnIdx, 0, sr.depth)
          } else {
            toReturn = false
          }
          nextRowBuffer += sr
        }
      } else {
        nextRowBuffer += sr
        toReturn = false
      }
      toReturn
    }

    def joinWithBuffer(r: Row, gtGen: GTGen, offset: Int): Unit = {
      var keepOn = true
      var idx = offset
      while (idx < currentRowBuffer.length && keepOn) {
        val sr = currentRowBuffer(idx)
        keepOn = matchWithSegRow(r, sr, gtGen)
        idx += 1
      }
      while (idx < currentRowBuffer.length) {
        nextRowBuffer += currentRowBuffer(idx)
        idx += 1
      }
    }

    def writeGtsEncoded(sb: mutable.StringBuilder, offset: Int, upTo: Int, gtGen: GTGen): Unit = {
      var idx = offset
      while (idx < upTo) {
        if (gtGen.hasCoverage(idx)) {
          sb.append(math.round((1 - gts(3 * idx + 1)) * 93 + 33).toChar)
          sb.append(math.round((1 - gts(3 * idx + 2)) * 93 + 33).toChar)
        } else {
          sb.append("  ")
        }
        idx += 1
      }
    }

    def writeGtsTriplets(sb: mutable.StringBuilder, offset: Int, upTo: Int, gtGen: GTGen): Unit = {
      writeGtTriplet(sb, gtGen, offset)
      var idx = offset + 1
      while (idx < upTo) {
        sb.append(outSep)
        writeGtTriplet(sb, gtGen, idx)
        idx += 1
      }
    }

    def writeGtTriplet(sb: mutable.StringBuilder, gtGen: GTGen, idx: Int): Unit = {
      if (gtGen.hasCoverage(idx)) {
        sb.append("%5.5g".format(gts(3 * idx)))
        sb.append(tripSep)
        sb.append("%5.5g".format(gts(3 * idx + 1)))
        sb.append(tripSep)
        sb.append("%5.5g".format(gts(3 * idx + 2)))
      } else {
        sb.append(tripSep)
        sb.append(tripSep)
      }
    }

    def writeHc(sb: mutable.StringBuilder, offset: Int, upTo: Int, gtGen: GTGen): Unit = {
      var idx = offset
      while (idx < upTo) {
        writeHcToBuilder(sb, gtGen, idx)
        idx += 1
      }
    }

    def writeHcToBuilder(sb: StringBuilder, gtGen: GTGen, idx: Int): Unit = {
      if (gtGen.hasCoverage(idx)) {
        if (gts(3 * idx) >= threshold) {
          sb.append('0')
        } else if (gts(3 * idx + 1) >= threshold) {
          sb.append('1')
        } else if (gts(3 * idx + 2) >= threshold) {
          sb.append('2')
        } else {
          sb.append('3')
        }
      } else {
        sb.append('3')
      }
    }

    def writeHcSep(sb: StringBuilder, offset: Int, upTo: Int, gtGen: GTGen): Unit = {
      writeHcToBuilder(sb, gtGen, offset)
      var idx = offset + 1
      while (idx < upTo) {
        sb += outSep
        writeHcToBuilder(sb, gtGen, idx)
        idx += 1
      }
    }

    override def finish(): Unit = {
      ti.unregisterUser(this)
      if (ti.users.isEmpty) context.getSession.getCache.getObjectHashMap.remove(lookupSignature)
      rightSource.close()
    }

    private def swapRowBuffers(): Unit = {
      val tmp = currentRowBuffer
      currentRowBuffer = nextRowBuffer
      nextRowBuffer = tmp
      nextRowBuffer.clear
    }
  }
}
