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

import java.text.{DecimalFormat, DecimalFormatSymbols}
import java.util.Locale

import gorsat.Commands.{BinAggregator, BinAnalysis, BinFactory, BinInfo, BinState, Processor, RegularRowHandler}
import gorsat.gorsatGorIterator.MapAndListUtilities
import org.gorpipe.exceptions.GorDataException
import org.gorpipe.gor.GorSession
import org.gorpipe.model.genome.files.gor.Row
import org.gorpipe.model.gor.RowObj
import org.gorpipe.model.gor.iterators.LineIterator

object GorCsvCC {

  val numNonArgs = 2
  val pArray: Array[Double] = Range(0, 128).map(qual => 1.0 - (qual - 33) / 93.0).toArray

  case class SaHolder(var seps: scala.collection.mutable.ArrayBuffer[Int])

  def splitArray(s: CharSequence, offset: Int, sah: SaHolder, sepval: Char = ','): Unit = {
    var i = offset
    var n = 0
    while (i < s.length) {
      if (s.charAt(i) == sepval) {
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

  case class CsvCCState(session: GorSession,
                        lookupSignature: String,
                        buckCol: Int,
                        valCol: Int,
                        grCols: List[Int],
                        sepVals: String,
                        valSize: Int,
                        uv: String,
                        use_phase: Boolean,
                        use_prob: Boolean,
                        use_threshold: Boolean,
                        p_threshold: Double) extends BinState {

    case class ColHolder() {
      var buckRows: Array[CharSequence] = _
      var splitArr: Array[SaHolder] = _
      var phenoStatusCounter: Array[Array[Int]] = _
      var phenoStatusFloatCounter: Array[Array[Double]] = _
    }

    val useGroup: Boolean = if (grCols.nonEmpty) true else false
    val sepSize: Int = if (valSize == -1) 1 else sepVals.length.min(1)
    val sepval: Char = if (sepSize == 1 && valSize == -1) sepVals(0) else ','
    val unknown: Boolean = if (uv != "") true else false
    val uc: Int = if (unknown) uv.toInt else -1

    var bui: BucketInfo = _
    var groupMap = scala.collection.mutable.HashMap.empty[String, ColHolder]
    var singleColHolder = ColHolder()
    if (!useGroup) groupMap += ("theOnlyGroup" -> singleColHolder)
    val grColsArray: Array[Int] = grCols.toArray

    val fd = new DecimalFormat("#.#", DecimalFormatSymbols.getInstance(Locale.ROOT))
    val fd3 = new DecimalFormat("#.###", DecimalFormatSymbols.getInstance(Locale.ROOT))

    var line: CharSequence = ""
    var maxUsedBuckets = 0
    var maxPhenoStats = 0

    def initColHolder(sh: ColHolder) {
      if (sh.buckRows == null) {
        sh.buckRows = new Array[CharSequence](maxUsedBuckets)
        sh.splitArr = new Array[SaHolder](maxUsedBuckets)
        sh.phenoStatusCounter = new Array[Array[Int]](maxPhenoStats)
        sh.phenoStatusFloatCounter = new Array[Array[Double]](maxPhenoStats)
        var i = 0
        while (i < maxPhenoStats) {
          sh.phenoStatusCounter(i) = new Array[Int](5)
          sh.phenoStatusFloatCounter(i) = new Array[Double](5)
          i += 1
        }
      }
      var i = 0
      while (i < sh.buckRows.length) {
        sh.buckRows(i) = null
        if (valSize == -1) sh.splitArr(i) = SaHolder(new scala.collection.mutable.ArrayBuffer[Int](100))
        i += 1
      }
      i = 0
      while (i < maxPhenoStats) {
        var j = 0
        while (j < 5) {
          sh.phenoStatusCounter(i)(j) = 0
          sh.phenoStatusFloatCounter(i)(j) = 0.0
          j += 1
        }
        i += 1
      }
    }

    def initialize(binInfo: BinInfo): Unit = {
      bui = session.getCache.getObjectHashMap.get(lookupSignature).asInstanceOf[BucketInfo]
      if( bui == null ) throw new GorDataException("Non existing bucket info for lookupSignature " + lookupSignature)

      maxUsedBuckets = bui.bucketIDMap.size
      maxPhenoStats = bui.phenoMap.size
      if (useGroup) groupMap = scala.collection.mutable.HashMap.empty[String, ColHolder]
      else initColHolder(singleColHolder)
    }

    def process(r: Row) {
      line = r.colAsString(valCol)
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
          if (valSize == -1) {
            splitArray(line, 0, sh.splitArr(buckNo), sepval)
          }
        case None => /* Do nothing - a row representing unused bucket */
      }
    }

    def sendToNextProcessor(bi: BinInfo, nextProcessor: Processor) {
      var p0: Double = 0.0
      var p1: Double = 0.0
      var p2: Double = 0.0
      for (key <- groupMap.keys.toList.sorted) {
        var sh: ColHolder = null
        if (useGroup) sh = groupMap(key) else sh = groupMap("theOnlyGroup")
        val line: String = bi.chr + "\t" + (bi.sta + 1) + (if (useGroup) "\t" + key else "")
        var phenorow = 0
        var rstr: CharSequence = null
        var theTag = 0
        try {
          while (phenorow < bui.phenorowsLeft.length && !nextProcessor.wantsNoMore) {
            val tag = bui.phenorowsLeft(phenorow)
            val phenostatus = bui.phenorowsRight(phenorow)
            theTag = tag
            val buckNo = bui.outputBucketID(tag)
            val buckPos = bui.outputBucketPos(tag)
            rstr = sh.buckRows(buckNo)
            if (rstr == null) {
              if (unknown) {
                sh.phenoStatusCounter(phenostatus)(uc) += 1
                if (use_prob) sh.phenoStatusFloatCounter(phenostatus)(uc) += 1.0
              } else throw new RuntimeException("Problem with input data when generating row: " + line + "\n\n")
            } else {
              var start = 0

              if (valSize == -1) {
                val sah = sh.splitArr(buckNo)
                if (buckPos > 0) start = sah.seps(buckPos - 1) + 1
              } else {
                if (buckPos > 0) start = buckPos * (valSize + sepSize)
              }
              if (!use_prob) {
                val gt = rstr.charAt(start)
                if (gt == '0') sh.phenoStatusCounter(phenostatus)(0) += 1
                else if (gt == '1') sh.phenoStatusCounter(phenostatus)(1) += 1
                else if (gt == '2') sh.phenoStatusCounter(phenostatus)(2) += 1
                else sh.phenoStatusCounter(phenostatus)(3) += 1
              } else {
                if (rstr.charAt(start) == ' ') {
                  sh.phenoStatusCounter(phenostatus)(3) += 1
                } else {
                  if (use_phase) {
                    val pfalt = pArray(rstr.charAt(start))
                    val pmalt = pArray(rstr.charAt(start + 1))
                    p0 = (1.0 - pfalt) * (1.0 - pmalt)
                    p1 = (1.0 - pfalt) * pmalt + pfalt * (1.0 - pmalt)
                    p2 = pfalt * pmalt
                  } else {
                    p1 = pArray(rstr.charAt(start))
                    p2 = pArray(rstr.charAt(start + 1))
                    p0 = 1 - p1 - p2
                  }
                  if (use_threshold) {
                    var update_unknown = true
                    if (p0 >= p_threshold) {
                      sh.phenoStatusCounter(phenostatus)(0) += 1
                      update_unknown = false
                    }
                    if (p1 >= p_threshold) {
                      sh.phenoStatusCounter(phenostatus)(1) += 1
                      update_unknown = false
                    }
                    if (p2 >= p_threshold) {
                      sh.phenoStatusCounter(phenostatus)(2) += 1
                      update_unknown = false
                    }
                    if (update_unknown) sh.phenoStatusCounter(phenostatus)(3) += 1
                  } else {
                    sh.phenoStatusFloatCounter(phenostatus)(0) += p0
                    sh.phenoStatusFloatCounter(phenostatus)(1) += p1
                    sh.phenoStatusFloatCounter(phenostatus)(2) += p2
                  }
                }
              }
            }
            phenorow += 1
          }
          if (!nextProcessor.wantsNoMore) {
            var i = 0
            while (i < maxPhenoStats) {
              val pheno = bui.phenoMap(i)
              var j = 0
              while (j < 4.max(uc + 1)) {
                if (!use_prob || use_threshold) {
                  val counts = sh.phenoStatusCounter(i)(j)
                  nextProcessor.process(RowObj(line + '\t' + pheno + '\t' + j + '\t' + counts))
                } else {
                  val counts = sh.phenoStatusFloatCounter(i)(j)
                  nextProcessor.process(RowObj(line + '\t' + pheno + '\t' + j + '\t' + fd3.format(counts)))
                }
                j += 1
              }
              i += 1
            }
          }
        } catch {
          case e: java.lang.IndexOutOfBoundsException =>
            val bucket = bui.bucketIDMap.filter(b => b._2 == theTag).keys.mkString(",")
            throw new GorDataException("Missing values in bucket " + bucket + " in searching for tag " + bui.outputTags(theTag) + "\nin row with\n" + rstr + "\n\n", e)
        }
      }
      // cleanup
      if (useGroup) {
        groupMap.clear()
      }
    }
  }


  case class CsvCCFactory(session: GorSession,
                          lookupSignature: String,
                          buckCol: Int,
                          valCol: Int,
                          grCols: List[Int],
                          sepVal: String,
                          valSize: Int,
                          uv: String,
                          use_phase: Boolean,
                          use_prob: Boolean,
                          use_threshold: Boolean,
                          p_threshold: Double) extends BinFactory {

    def create: BinState =
      CsvCCState(session, lookupSignature, buckCol, valCol, grCols, sepVal, valSize, uv, use_phase, use_prob, use_threshold, p_threshold)
  }


  case class BucketInfo() {
    var outputBucketID: Array[Int] = _
    var outputBucketPos: Array[Int] = _
    var bucketIDMap = scala.collection.mutable.Map.empty[String, Int]
    var outputTags: Array[String] = _
    var phenoMap = scala.collection.immutable.Map.empty[Int, String]
    var phenorowsLeft: Array[Int] = _
    var phenorowsRight: Array[Int] = _
  }

  def lookup(fileName1: String, iteratorCommand1: String, fileName2: String, iteratorCommand2: String): String = {
    (fileName1 + "#" + iteratorCommand1 + "#" + fileName2 + "#" + iteratorCommand2).replace("| top 0 ","")
  }

  case class CsvCCAnalysis(fileName1: String, iteratorCommand1: String, iterator1: LineIterator, fileName2: String, iteratorCommand2: String, iterator2: LineIterator, buckCol: Int, valCol: Int,
                           grCols: List[Int], sepVal: String, valSize: Int, uv: String, use_phase: Boolean, use_prob: Boolean, use_threshold: Boolean, p_threshold: Double, session: GorSession) extends
    BinAnalysis(RegularRowHandler(1), BinAggregator(CsvCCFactory(session, lookup(fileName1, iteratorCommand1, fileName2, iteratorCommand2), buckCol, valCol, grCols, sepVal, valSize, uv, use_phase, use_prob, use_threshold, p_threshold), 2, 1)) {
    var bucketIDMap = scala.collection.mutable.Map.empty[String, Int]
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

    var bucketOrderMap = scala.collection.mutable.Map.empty[String, Int]
    var bucketCounterMap = scala.collection.mutable.Map.empty[String, BucketCounter]

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

    val phenoOrderMap = scala.collection.mutable.Map.empty[String, Int]
    var phenoCounter: Int = -1

    def phenotOrder(b: String): Int = phenoOrderMap.get(b) match {
      case Some(x) => x;
      case None =>
        phenoCounter += 1
        phenoOrderMap += (b -> phenoCounter)
        phenoCounter
    }


    override def setup: Unit = {
      super.setup()

      val lookupSignature: String = lookup(fileName1, iteratorCommand1, fileName2, iteratorCommand2)

      session.getCache.getObjectHashMap.computeIfAbsent(lookupSignature, _ => {
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

        var phenorows: List[(Int, Int)] = Nil
        var tags: List[String] = Nil
        l2.foreach(x => {
          val r = x.split("\t")
          var phenotype = "pheno"
          var ccstatus = ""
          var tag = ""
          if (r.length == 3) {
            tag = r(0)
            phenotype = r(1)
            ccstatus = r(2)
          } else if (r.length == 2) {
            tag = r(0)
            ccstatus = r(1)
          } else {
            throw new RuntimeException("Incorrect number of columns in the phenostatus file.  Use either (tag,pheno,status) or (tag,status): " + r.mkString(",") + "\n\n")
          }
          val tagID = outputOrder(tag) // The equivalence of PN/tag order
          tags ::= tag
          val phenostatusID = if (r.length == 3) phenotOrder(phenotype + '\t' + ccstatus) else phenotOrder(ccstatus)
          phenorows ::= (tagID, phenostatusID)
        })
        val bi = BucketInfo()
        val outputSize = outputOrderMap.size
        bi.outputBucketID = new Array[Int](outputSize)
        bi.outputBucketPos = new Array[Int](outputSize)
        bi.phenorowsLeft = phenorows.reverse.map(_._1).toArray
        bi.phenorowsRight = phenorows.reverse.map(_._2).toArray
        bi.phenoMap = phenoOrderMap.toList.map(x => {
          (x._2, x._1)
        }).toMap[Int, String]

        val tagMap = outputOrderMap.clone()

        l1.foreach(x => {
          val r = x.split("\t")
          val (tag, bid) = (r(0), r(1))
          tagMap.remove(tag)
          val bucketid = bucketID(bid)
          val bucketpos = bucketOrder(bid, tag)
          if (outputOrder(tag) < outputSize) {
            bi.outputBucketID(outputOrder(tag)) = bucketid
            bi.outputBucketPos(outputOrder(tag)) = bucketpos
          }
        })

        if (tagMap.nonEmpty) throw new RuntimeException("There are tags in the second input file which are not defined in the first tag/bucket input, including: " + tagMap.keys.toList.slice(0, 10).mkString(","))

        bi.bucketIDMap = bucketIDMap
        bi.outputTags = tags.reverse.toArray
        bi
      })

      // cleanup
      bucketIDMap = null
      bucketOrderMap = null
      bucketCounterMap = null
    }
  }
}
