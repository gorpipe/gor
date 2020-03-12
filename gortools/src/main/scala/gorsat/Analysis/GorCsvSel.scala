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

package gorsat.Analysis

import java.lang

import gorsat.Commands.{BinAggregator, BinAnalysis, BinFactory, BinInfo, BinState, Processor, RegularRowHandler}
import gorsat.gorsatGorIterator.MapAndListUtilities
import gorsat.process.GenotypeLookupUtilities
import gorsat.process.GorJavaUtilities.VCFValue
import org.gorpipe.exceptions.GorDataException
import org.gorpipe.gor.GorSession
import org.gorpipe.model.genome.files.gor.{Line, Row, RowBase}
import org.gorpipe.model.gor.RowObj
import org.gorpipe.model.gor.iterators.LineIterator

import scala.collection.mutable

object GorCsvSel {

  case class SaHolder(var seps: scala.collection.mutable.ArrayBuffer[Int])

  def splitArray(s: CharSequence, offset: Int, sah: SaHolder, sepval: Char = ','): Unit = {
    var i = offset
    var n = 0
    while (i < s.length) {
      if (s.charAt(i) == sepval || s.charAt(i) == '\t') {
        if (n < sah.seps.length) sah.seps(n) = i else sah.seps += i
        n += 1
      }
      i += 1
    }
    if (n < sah.seps.length) sah.seps(n) = i else sah.seps += i
    n += 1
    if (n < sah.seps.length) sah.seps = sah.seps.slice(0, n)
    // sah.seps.toArray
  }

  def colString(n: Int, str: CharSequence, offset: Int, sah: SaHolder): String = {
    val start = if (n == 0) offset else sah.seps(n - 1) + 1
    val stop = sah.seps(n)
    str.subSequence(start, stop).toString
  }

  def colCharMove(n: Int, str: CharSequence, offset: Int, sah: SaHolder, o: java.lang.StringBuilder): Unit = {
    val start = if (n == 0) offset else sah.seps(n - 1) + 1
    val stop = sah.seps(n)
    var i = start
    while (i < stop) {
      o.append(str.charAt(i))
      i += 1
    }
  }

  def colStringFixed(n: Int, str: CharSequence, offset: Int, valSize: Int, sepSize: Int): String = {
    val start = offset + n * (valSize + sepSize)
    val stop = start + valSize
    str.subSequence(start, stop).toString
  }

  def colCharMoveFixed(n: Int, str: CharSequence, offset: Int, o: java.lang.StringBuilder, valSize: Int, sepSize: Int): Unit = {
    val start = offset + n * (valSize + sepSize)
    val stop = start + valSize
    var i = start
    while (i < stop) {
      o.append(str.charAt(i))
      i += 1
    }
  }

  case class CsvSelState(session: GorSession,
                         lookupSignature: String,
                         buckCol: Int,
                         valCol: Int,
                         grCols: List[Int],
                         sepVals: String,
                         outputRows: Boolean,
                         hideSome: Boolean,
                         toHide: mutable.Set[String],
                         valSize: Int,
                         toVCF: Boolean,
                         vcfThreshold: Double,
                         doseOption: Boolean,
                         uv: String) extends BinState {

    case class ColHolder() {
      var buckRows: Array[CharSequence] = _
      var offsetArray: Array[Int] = _
      var splitArr: Array[SaHolder] = _
    }
    lazy val vcfValue = new VCFValue(vcfThreshold)

    lazy val vcfMap: (CharSequence, Int) => String = if (vcfThreshold == -1) {
      (vals, idx) => GenotypeLookupUtilities.numchar2Genotype.get(vals.charAt(idx))
    } else {
      (vals, idx) => vcfValue.get(vals.charAt(idx), vals.charAt(idx + 1))
    }

    val useGroup: Boolean = if (grCols.nonEmpty) true else false
    val sepSize: Int = if (valSize > 0) 0 else if (valSize == -1) 1 else sepVals.length.min(1)
    val sepval: Char = if (sepSize == 1 && valSize == -1) sepVals(0) else ','
    val unknownVal: String = if (valSize != -1) (uv + Range(uv.length + 1, 1000).map((x: Int) => {
      x - x / 10
    }).mkString("")).slice(0, valSize) else uv
    var unknown: Boolean = if (uv != "") true else false

    var bui: BucketInfo = _
    var groupMap = scala.collection.mutable.HashMap.empty[String, ColHolder]
    var singleColHolder = ColHolder()
    if (!useGroup) groupMap += ("theOnlyGroup" -> singleColHolder)
    val grColsArray: Array[Int] = grCols.toArray
    var ladd = new java.lang.StringBuilder(1024 * 4)

    lazy val getProbs: String => (Double, Double) = (s: String) => (1 - (s(0) - 33) / 93.0, 1 - (s(1) - 33) / 93.0)
    lazy val addToBuilder: (lang.StringBuilder, (Double, Double)) => lang.StringBuilder = if (doseOption) {
      (lineBuilder: java.lang.StringBuilder, p: (Double, Double)) => {
        lineBuilder.append(p._1 + 2 * p._2)
      }
    } else {
      (lineBuilder: java.lang.StringBuilder, p: (Double, Double)) => {
        lineBuilder.append(1 - p._1 - p._2)
        lineBuilder.append(',')
        lineBuilder.append(p._1)
        lineBuilder.append(',')
        lineBuilder.append(p._2)
      }
    }

    def initColHolder(sh: ColHolder) {
      if (sh.buckRows == null) {
        sh.buckRows = new Array[CharSequence](maxUsedBuckets)
        sh.offsetArray = new Array[Int](maxUsedBuckets)
        sh.splitArr = new Array[SaHolder](maxUsedBuckets)
      }
      var i = 0
      while (i < sh.buckRows.length) {
        sh.buckRows(i) = null
        if (valSize == -1) sh.splitArr(i) = SaHolder(new scala.collection.mutable.ArrayBuffer[Int](100))
        i += 1
      }
    }

    var line: CharSequence = ""
    var maxUsedBuckets = 0

    def initialize(binInfo: BinInfo): Unit = {
      bui = session.getCache.getObjectHashMap.get(lookupSignature).asInstanceOf[BucketInfo]
      if (bui == null) throw new GorDataException("Non existing bucket info for lookupSignature " + lookupSignature)
      maxUsedBuckets = bui.bucketIDMap.size
      if (useGroup) groupMap = scala.collection.mutable.HashMap.empty[String, ColHolder]
      else initColHolder(singleColHolder)
    }

    def process(r: Row) {
      var useLineObject = false
      if (r.isInstanceOf[Line]){
        line = r.colAsString(valCol)
        useLineObject = true
      } else {
        line = r.toString
      }

      bui.bucketIDMap.get(r.colAsString(buckCol).toString) match {
        case Some(buckNo) =>
          var sh: ColHolder = null
          if (useGroup) {
            val groupID = r.selectedColumns(grColsArray)
            groupMap.get(groupID) match {
              case Some(x) => sh = x
              case None =>
                sh = ColHolder()
                initColHolder(sh)
                groupMap += (groupID -> sh)
            }
          } else sh = singleColHolder

          sh.buckRows(buckNo) = line
          val offset = if (useLineObject) 0 else r.sa(valCol - 1) + 1
          sh.offsetArray(buckNo) = offset
          if (valSize == -1) {
            splitArray(line, offset, sh.splitArr(buckNo), sepval)
          }
        case None => /* Do nothing - a row representing unused bucket */
      }

    }

    var lastSize = 0
    def sendToNextProcessor(bi: BinInfo, nextProcessor: Processor) {
      for (key <- groupMap.keys.toList.sorted) {
        var sh: ColHolder = null
        if (useGroup) sh = groupMap(key) else sh = groupMap("theOnlyGroup")
        ladd = new java.lang.StringBuilder(lastSize)
        var outCol = 0
        var r: CharSequence = null
        try {
          var groupNum = 0
          if (useGroup) {
            var k = 0
            groupNum += 1
            while (k < key.length) {
              if (key.charAt(k) == '\t') groupNum += 1
              k += 1
            }
          }
          val sa = if (toVCF) new Array[Int](6 + groupNum + bui.outputBucketPos.length) else new Array[Int](3 + groupNum)
          ladd.append(bi.chr)
          var i = 0
          sa(i) = ladd.length()
          i += 1
          ladd.append('\t')
          ladd.append(bi.sta + 1)
          sa(i) = ladd.length()
          i += 1
          if (useGroup) {
            var k = key.indexOf('\t')
            while (k != -1) {
              sa(i) = ladd.length() + k + 1
              i += 1
              k = key.indexOf('\t', k + 1)
            }
            ladd.append('\t' + key)
            sa(i) = ladd.length()
            i += 1
          }
          val line = ladd.toString

          if (toVCF) {
            if (!nextProcessor.wantsNoMore) {
              if (outputRows) ladd.append(bui.outputTags.mkString(","))
              ladd.append('\t')
              ladd.append('.')
              sa(i) = ladd.length()
              i += 1

              ladd.append('\t')
              ladd.append('.')
              sa(i) = ladd.length()
              i += 1

              ladd.append('\t')
              ladd.append('.')
              sa(i) = ladd.length()
              i += 1

              ladd.append('\t')
              if (vcfThreshold == -1) ladd.append("GT")
              else ladd.append("GT:GP")
              sa(i) = ladd.length()
              i += 1

              while (outCol < bui.outputBucketPos.length) {
                val buckNo = bui.outputBucketID(outCol)
                val buckPos = bui.outputBucketPos(outCol)
                r = sh.buckRows(buckNo)
                if (r == null) {
                  ladd.append('\t')
                  ladd.append(bui.outputTags(outCol))
                  ladd.append(unknownVal)
                } else if(valSize == -1) {
                  val offset = sh.offsetArray(buckNo)
                  val cs = colString(buckPos, r, offset, sh.splitArr(buckNo))
                  addToBuilder(ladd, getProbs(cs))
                } else if(valSize == 1) {
                  val offset = sh.offsetArray(buckNo)
                  val start = offset + buckPos * (valSize + sepSize)
                  ladd.append(vcfMap(r, start))
                } else {
                  val offset = sh.offsetArray(buckNo)
                  val start = offset + buckPos * (valSize + sepSize)
                  ladd.append(vcfMap(r, start))
                }
                sa(i) = ladd.length()
                i += 1
                outCol += 1
              }
              if (!outputRows && !nextProcessor.wantsNoMore) {
                if (i < sa.length) sa(i) = ladd.length()
                lastSize = Math.max(lastSize, ladd.length())
                nextProcessor.process(new RowBase(bi.chr, bi.sta+1, ladd, sa, null))
              }
            }
          } else {
            ladd.append('\t')
            while (outCol < bui.outputBucketPos.length && !nextProcessor.wantsNoMore) {
              val buckNo = bui.outputBucketID(outCol)
              val buckPos = bui.outputBucketPos(outCol)
              r = sh.buckRows(buckNo)
              if (r == null) {
                if (unknown) {
                  if (outputRows) nextProcessor.process(RowObj(String.join("\t", line, bui.outputTags(outCol), unknownVal)))
                  else {
                    if (outCol != 0 && sepSize != 0) ladd.append(sepval)
                    ladd.append(unknownVal)
                  }
                } else if(!nextProcessor.pipeFrom.wantsNoMore) {
                  throw new GorDataException("Problem with input data when generating row: " + line + "\n\n")
                }
              } else {
                val offset = sh.offsetArray(buckNo)
                if (valSize == -1) {
                  if (outputRows) {
                    val cs = colString(buckPos, r, offset, sh.splitArr(buckNo))
                    if (!(hideSome && toHide.contains(cs))) nextProcessor.process(RowObj(String.join("\t", line, bui.outputTags(outCol), cs)))
                  }
                  else {
                    if (outCol != 0 && sepSize != 0) ladd.append(sepval)
                    colCharMove(buckPos, r, offset, sh.splitArr(buckNo), ladd)
                  }
                } else {
                  if (outputRows) {
                    val csf = colStringFixed(buckPos, r, offset, valSize, sepSize)
                    if (!(hideSome && toHide.contains(csf))) nextProcessor.process(RowObj(String.join("\t", line, bui.outputTags(outCol), csf)))
                  }
                  else {
                    if (outCol != 0 && sepSize != 0) ladd.append(sepval)
                    colCharMoveFixed(buckPos, r, offset, ladd, valSize, sepSize)
                  }
                }
              }
              outCol += 1
            }
            if (!outputRows && !nextProcessor.wantsNoMore) {
              if( i < sa.length ) sa(i) = ladd.length()
              lastSize = Math.max(lastSize, ladd.length())
              nextProcessor.process(new RowBase(bi.chr, bi.sta+1, ladd, sa, null))
            }
          }
        } catch {
          case e: java.lang.IndexOutOfBoundsException =>
            throw new GorDataException("Missing values in bucket " + r.toString.split("\t")(buckCol) + " in searching for tag " + bui.outputTags(outCol) + " no " + outCol + " in output\nin row\n" + r + "\n\n", e)
        }
      }
      // cleanup
      if (useGroup) {
        groupMap.clear()
      }
    }
  }

  case class CsvSelFactory(session: GorSession,
                           lookupSignature: String,
                           buckCol: Int,
                           valCol: Int,
                           grCols: List[Int],
                           sepVal: String,
                           outputRows: Boolean,
                           hideSome: Boolean,
                           toHide: mutable.Set[String],
                           valSize: Int,
                           toVCF: Boolean,
                           vcfThreshold: Double,
                           doseOption: Boolean,
                           uv: String) extends BinFactory {
    def create: BinState =
      CsvSelState(session, lookupSignature, buckCol, valCol, grCols, sepVal, outputRows, hideSome, toHide, valSize, toVCF, vcfThreshold, doseOption, uv)
  }

  case class BucketInfo() {
    var outputBucketID: Array[Int] = _
    var outputBucketPos: Array[Int] = _
    var bucketIDMap = scala.collection.mutable.Map.empty[String, Int]
    var outputTags: Array[String] = _
  }

  case class CsvSelAnalysis(fileName1: String, iteratorCommand1: String, iterator1: LineIterator, fileName2: String, iteratorCommand2: String, iterator2: LineIterator, buckCol: Int, valCol: Int,
                            grCols: List[Int], sepVal: String, outputRows: Boolean, hideSome: Boolean, toHide: mutable.Set[String] = null, valSize: Int, toVCF: Boolean, vcfThreshold: Double, doseOption: Boolean, uv: String, session: GorSession) extends
    BinAnalysis(RegularRowHandler(1), BinAggregator(CsvSelFactory(session, fileName1 + "#" + iteratorCommand1 + "#" + fileName2 + "#" + iteratorCommand2, buckCol, valCol, grCols, sepVal, outputRows, hideSome, toHide, valSize, toVCF, vcfThreshold, doseOption, uv), 2, 1)) {

    val bucketIDMap = scala.collection.mutable.Map.empty[String, Int]
    var bucketID: Int = -1

    def bucketID(b: String): Int = bucketIDMap.get(b) match {
      case Some(x) => x;
      case None =>
        bucketID += 1
        bucketIDMap += (b -> bucketID)
        bucketID
    }

    case class BucketCounter() {
      var c: Integer = -1
    }

    val bucketOrderMap = scala.collection.mutable.Map.empty[String, Int]
    val bucketCounterMap = scala.collection.mutable.Map.empty[String, BucketCounter]

    def bucketOrder(b: String, t: String): Int = {
      val bc = bucketCounterMap.get(b) match {
        case Some(x) => x;
        case None =>
          bucketCounterMap += (b -> BucketCounter())
          bucketCounterMap(b)
      }
      bucketOrderMap.get("#bu:" + b + "#ta:" + t) match {
        case Some(x) => x;
        case None =>
          bc.c += 1
          bucketOrderMap += (("#bu:" + b + "#ta:" + t) -> bc.c)
          bc.c
      }
    }

    val outputOrderMap = scala.collection.mutable.Map.empty[String, Int]
    var outputCounter: Int = -1

    def outputOrder(b: String): Int = outputOrderMap.get(b) match {
      case Some(x) => x;
      case None =>
        outputCounter += 1
        outputOrderMap += (b -> outputCounter)
        outputCounter
    }

    val lookupSignature: String = fileName1 + "#" + iteratorCommand1 + "#" + fileName2 + "#" + iteratorCommand2

    session.getCache.getObjectHashMap.computeIfAbsent(lookupSignature, f => {
        var l1 = Array.empty[String]
        var l2 = Array.empty[String]

        try {
          if (iteratorCommand1 != "") l1 = MapAndListUtilities.getStringArray(iteratorCommand1, iterator1, session)
          else l1 = MapAndListUtilities.getStringArray(fileName1, session)

          if (iteratorCommand2 != "") l2 = MapAndListUtilities.getStringArray(iteratorCommand2, iterator2, session)
          else l2 = MapAndListUtilities.getStringArray(fileName2, session)
        } catch {
          case e: Exception =>
            iterator1.close()
            iterator2.close()
            throw e
        }

        var tags: List[String] = Nil
        l2.foreach(x => {
          outputOrder(x)
          tags ::= x
        })
        val bi = BucketInfo()
        val outputSize = outputOrderMap.size
        bi.outputBucketID = new Array[Int](outputSize)
        bi.outputBucketPos = new Array[Int](outputSize)

        val tagMap = outputOrderMap.clone()

        l1.foreach(x => {
          val r = x.split("\t")
          val (tag, bid) = (r(0), r(1))
          tagMap.remove(tag)

          val buckid = bucketID(bid)
          val buckpos = bucketOrder(bid, tag)

          if (outputOrder(tag) < outputSize) {
            bi.outputBucketID(outputOrder(tag)) = buckid
            bi.outputBucketPos(outputOrder(tag)) = buckpos
          }
        })

        if (tagMap.nonEmpty) throw new GorDataException("There are tags in the second input file which are not defined in the first tag/bucket input, including: " + tagMap.keys.toList.slice(0, 10).mkString(","))

        bi.bucketIDMap = bucketIDMap
        bi.outputTags = tags.reverse.toArray
        bi
    })
  }
}
