package gorsat.Analysis

import java.lang

import gorsat.Commands.Analysis
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.model.Row
import org.gorpipe.model.gor.RowObj

abstract class GtTransposeAnalysis(pns: Array[String], bucketToPnIdxList: Map[String, (Array[Int], Array[Int])],
                                   markerToIdxMap: Map[String, Int], bCol: Int, vCol: Int, colIndices: Array[Int],
                                   cols: Boolean) extends Analysis {

  val markerCount: Int = markerToIdxMap.size
  val pnCount: Int = pns.length

  def handleGenotypes(values: CharSequence, pnAbsIds: Array[Int], pnRelIds: Array[Int], markerIdx: Int): Unit

  override def process(r: Row): Unit = {
    val markerId = r.selectedColumns(colIndices)
    markerToIdxMap.get(markerId) match {
      case Some(markerIdx) => {
        val bucket = r.colAsString(bCol).toString
        bucketToPnIdxList.get(bucket) match {
          case Some((pnAbsIds, pnRelIds)) => {
            val values = r.colAsString(vCol)
            handleGenotypes(values, pnAbsIds, pnRelIds, markerIdx)
          }
          case None => //Unused bucket
        }
      }
      case None => //Marker that we are not interested in
    }
  }

  def writeGtsToBuilder(pnIdx: Int, builder: java.lang.StringBuilder): Unit = {
    if (cols) {
      writeSepGtsToBuilder(pnIdx, builder)
    } else {
      writeNoSepGtsToBuilder(pnIdx, builder)
    }
  }

  def writeSepGtsToBuilder(pnIdx: Int, builder: java.lang.StringBuilder): Unit

  def writeNoSepGtsToBuilder(pnIdx: Int, builder: java.lang.StringBuilder): Unit

  override def finish(): Unit = {
    var idx = 0
    while (idx < pns.length) {
      val pn = pns(idx)
      val builder = new java.lang.StringBuilder("chrA\t1\t" + pn)
      writeGtsToBuilder(idx, builder)
      super.process(RowObj(builder))
      idx += 1
    }
  }
}

case class FixedWidthGtTransposeAnalysis(pns: Array[String], bucketToPnIdxList: Map[String, (Array[Int], Array[Int])],
                                         markerToIdxMap: Map[String, Int],
                                         bCol: Int, vCol: Int, colIndices: Array[Int],
                                         cols: Boolean, width: Int)
  extends GtTransposeAnalysis(pns, bucketToPnIdxList, markerToIdxMap, bCol, vCol, colIndices, cols) {

  val pnIdxToGtArray = Array.fill[Char](pnCount, markerCount * width)(' ')

  override def handleGenotypes(values: CharSequence, pnAbsIds: Array[Int], pnRelIds: Array[Int], markerIdx: Int): Unit = {
    var i = 0
    while (i < pnRelIds.length) {
      val pnRelIdx = pnRelIds(i)
      val pnAbsIdx = pnAbsIds(i)
      val gts = pnIdxToGtArray(pnAbsIdx)
      var j = 0
      while (j < width) {
        gts(width * markerIdx + j) = values.charAt(width * pnRelIdx + j)
        j += 1
      }
      i += 1
    }
  }

  override def writeSepGtsToBuilder(pnIdx: Int, builder: lang.StringBuilder): Unit = {
    val gts = pnIdxToGtArray(pnIdx)
    var idx = 0
    while (idx < markerCount) {
      builder.append('\t')
      builder.append(gts, idx * width, width)
      idx += 1
    }
  }

  override def writeNoSepGtsToBuilder(pnIdx: Int, builder: lang.StringBuilder): Unit = {
    val gts = pnIdxToGtArray(pnIdx)
    builder.append('\t')
    builder.append(gts)
  }
}

case class SepGtTransposeAnalysis(pns: Array[String], bucketToPnIdxList: Map[String, (Array[Int], Array[Int])],
                                  markerToIdxMap: Map[String, Int],
                                  bCol: Int, vCol: Int, colIndices: Array[Int], cols: Boolean, sep: Char)
  extends GtTransposeAnalysis(pns, bucketToPnIdxList, markerToIdxMap, bCol, vCol, colIndices, cols) {

  val pnIdxToGtArray = Array.fill[CharSequence](pnCount, markerCount)("")

  override def handleGenotypes(values: CharSequence, pnAbsIds: Array[Int], pnRelIds: Array[Int], markerIdx: Int): Unit = {
    var valueIdx = 0
    var wantedPnIdx = 0
    var pnInBucket = 0
    while (wantedPnIdx < pnAbsIds.length) {
      val pnRelIdx = pnRelIds(wantedPnIdx)
      while (pnInBucket < pnRelIdx) {
        while (values.charAt(valueIdx) != sep) {
          valueIdx += 1
        }
        valueIdx += 1 //We are at the separator, must jump over it.
        pnInBucket += 1
      }
      val from = valueIdx
      while (valueIdx < values.length() && values.charAt(valueIdx) != sep) {
        valueIdx += 1
      }
      pnIdxToGtArray(pnAbsIds(wantedPnIdx))(markerIdx) = values.subSequence(from, valueIdx)
      if (valueIdx < values.length()) valueIdx += 1 //Must jump over the separator
      pnInBucket += 1
      wantedPnIdx += 1
    }
  }

  override def writeSepGtsToBuilder(pnIdx: Int, builder: java.lang.StringBuilder): Unit = {
    val gts = pnIdxToGtArray(pnIdx)
    var idx = 0
    while (idx < markerCount) {
      builder.append('\t')
      builder.append(gts(idx))
      idx += 1
    }
  }

  override def writeNoSepGtsToBuilder(pnIdx: Int, builder: java.lang.StringBuilder): Unit = {
    val gts = pnIdxToGtArray(pnIdx)
    builder.append('\t')
    builder.append(gts.head)
    var idx = 1
    while (idx < markerCount) {
      builder.append(sep)
      builder.append(gts(idx))
      idx += 1
    }
  }
}

case class GtTransposeFactory(pns: Array[String], bucketToPnIdxList: Map[String, (Array[Int], Array[Int])],
                              markerToIdxMap: Map[String, Int],
                              bCol: Int, vCol: Int, colIndices: Array[Int],
                              sep: Option[Char], width: Option[Int], cols: Boolean) {

  def getAnalysis(): GtTransposeAnalysis = {
    (sep, width) match {
      case (Some(_), Some(_)) => throw new GorParsingException("Separator and width can not both be set.")
      case (Some(actualSep), None) => SepGtTransposeAnalysis(pns, bucketToPnIdxList, markerToIdxMap, bCol, vCol, colIndices, cols, actualSep)
      case (None, Some(actualWidth)) => FixedWidthGtTransposeAnalysis(pns, bucketToPnIdxList, markerToIdxMap, bCol, vCol, colIndices, cols, actualWidth)
      case (None, None) => throw new GorParsingException("Either separator or width must be set.")
    }
  }
}
