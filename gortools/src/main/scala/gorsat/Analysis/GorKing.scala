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

import gorsat.Commands.{BinAggregator, BinAnalysis, BinFactory, BinInfo, BinState, Processor, RegularRowHandler, RowHeader}
import gorsat.gorsatGorIterator.MapAndListUtilities
import org.gorpipe.exceptions.GorDataException
import org.gorpipe.gor.GorSession
import org.gorpipe.model.genome.files.gor.{GorMonitor, Line, Row}
import org.gorpipe.model.gor.RowObj
import org.gorpipe.model.gor.RowObj.BinaryHolder
import org.gorpipe.model.gor.iterators.LineIterator
import gorsat.Commands.Analysis

object GorKing {

  case class binaryHolder(bui : BucketInfo, af : Float) extends BinaryHolder

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

  def colCharMove(n: Int, str: CharSequence, offset: Int, sah: SaHolder, o: Array[Char], oc : Int): Unit = {
    val start = if (n == 0) offset else sah.seps(n - 1) + 1
    val stop = sah.seps(n)
    var i = start
    while (i < stop) {
      o(oc) = str.charAt(i)
      i += 1
    }
  }

  def colCharMoveFixed(n: Int, str: CharSequence, offset: Int, o: Array[Char], oc : Int, valSize: Int, sepSize: Int): Unit = {
    val start = offset + n * (valSize + sepSize)
    val stop = start + valSize
    var i = start
    while (i < stop) {
      o(oc) = str.charAt(i)
      i += 1
    }
  }

  case class KingState(session: GorSession,
                         lookupSignature: String,
                         buckCol: Int,
                         valCol: Int,
                         grCols: List[Int],
                         afCol: Int,
                         sepVals: String,
                         valSize: Int,
                         uv: String) extends BinState {

    case class ColHolder() {
      var buckRows: Array[CharSequence] = _
      var offsetArray: Array[Int] = _
      var splitArr: Array[SaHolder] = _
      var af : Float = _
    }

    val useGroup: Boolean = grCols.nonEmpty
    val sepSize: Int = if (valSize > 0) 0 else if (valSize == -1) 1 else sepVals.length.min(1)
    val sepval: Char = if (sepSize == 1 && valSize == -1) sepVals(0) else ','
    val unknownVal: String = if (valSize != -1) (uv + Range(uv.length + 1, 1000).map((x: Int) => {
      x - x / 10
    }).mkString("")).slice(0, valSize) else uv
    var unknown: Boolean = uv != ""

    var bui: BucketInfo = _
    var groupMap = scala.collection.mutable.HashMap.empty[String, ColHolder]
    var singleColHolder = ColHolder()
    if (!useGroup) groupMap += ("theOnlyGroup" -> singleColHolder)
    val grColsArray: Array[Int] = grCols.toArray
    var ladd = new java.lang.StringBuilder(1024 * 4)

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

          sh.af = r.colAsDouble(afCol).toFloat
          sh.buckRows(buckNo) = line
          val offset = if (useLineObject) 0 else r.sa(valCol - 1) + 1
          sh.offsetArray(buckNo) = offset
          if (valSize == -1) {
            splitArray(line, offset, sh.splitArr(buckNo), sepval)
          }
        case None => /* Do nothing - a row representing unused bucket */
      }

    }

    def sendToNextProcessor(bi: BinInfo, nextProcessor: Processor) {
      for (key <- groupMap.keys.toList.sorted) {
        val theBinaryHolderRow = RowObj("chrA", 0, "")
        var sh: ColHolder = null
        if (useGroup) sh = groupMap(key) else sh = groupMap("theOnlyGroup")
        val af = sh.af
        var r: CharSequence = null
        var outCol = 0
        try {
          r = null
          outCol = 0
            while (outCol < bui.outputBucketPos.length) {
              val buckNo = bui.outputBucketID(outCol)
              val buckPos = bui.outputBucketPos(outCol)
              r = sh.buckRows(buckNo)
              val offset = sh.offsetArray(buckNo)
              if (r == null) {
                bui.GTS1(outCol) = '3'
              } else {
                  if (valSize == -1) {
                  colCharMove(buckPos, r, offset, sh.splitArr(buckNo), bui.GTS1, outCol)
                } else {
                    colCharMoveFixed(buckPos, r, offset, bui.GTS1, outCol, valSize, sepSize)
                  }
              }
              outCol += 1
            }
            outCol = 0
            while (outCol < bui.outputBucketPos2.length) {
              val buckNo = bui.outputBucketID2(outCol)
              val buckPos = bui.outputBucketPos2(outCol)
              r = sh.buckRows(buckNo)
              val offset = sh.offsetArray(buckNo)
              if (r == null) {
                bui.GTS2(outCol) = '3'
              } else {
                if (valSize == -1) {
                  colCharMove(buckPos, r, offset, sh.splitArr(buckNo), bui.GTS2, outCol)
                } else {
                  colCharMoveFixed(buckPos, r, offset, bui.GTS2, outCol, valSize, sepSize)
                }
              }
              outCol += 1
            }


          theBinaryHolderRow.bH = binaryHolder(bui,af)

          if (!nextProcessor.wantsNoMore) {
            nextProcessor.process(theBinaryHolderRow)
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

  case class KingFactory(session: GorSession,
                           lookupSignature: String,
                           buckCol: Int,
                           valCol: Int,
                           grCols: List[Int],
                           afCol: Int,
                           sepVal: String,
                           valSize: Int,
                           uv: String) extends BinFactory {
    def create: BinState =
      KingState(session, lookupSignature, buckCol, valCol, grCols, afCol, sepVal, valSize, uv)
  }

  case class BucketInfo() {
    var outputBucketID: Array[Int] = _
    var outputBucketPos: Array[Int] = _
    var outputBucketID2: Array[Int] = _
    var outputBucketPos2: Array[Int] = _
    var bucketIDMap = scala.collection.mutable.Map.empty[String, Int]
    var outputTags: Array[String] = _
    var outputTags2: Array[String] = _
    var GTS1 : Array[Char] = _
    var GTS2 : Array[Char] = _
  }

  case class KingAggregate(pi0thr: Float, phithr: Float, thetathr: Float, t_pi0: Boolean, t_phi: Boolean, t_theta: Boolean, gm : GorMonitor) extends Analysis {
    var IBS0: Array[Int] = _
    var XX: Array[Int] = _
    var Nhet: Array[Int] = _
    var Nhom: Array[Int] = _
    var NAai: Array[Int] = _
    var NAaj: Array[Int] = _
    var tpq: Array[Float] = _
    var kpq: Array[Float] = _
    var count: Array[Int] = _
    var needsInitialization: Boolean = true
    var bh: binaryHolder = _
    var gtSize1: Int = 0
    var gtSize2: Int = 0
    var gtPairSize: Int = 0
    var cancelled: Boolean = false

    override def isTypeInformationMaintained : Boolean = true

    override def setRowHeader(header: RowHeader): Unit = {
      val columnNames: Array[String] = "Chrom\tPos\tPN1\tPN2\tIBS0\tXX\ttpq\tkpq\tNhet\tNhom\tNAai\tNAaj\tcount\tpi0\tphi\ttheta".split('\t')
      val columnTypes: Array[String] = "S\tI\tS\tS\tI\tI\tD\tD\tI\tI\tI\tI\tI\tD\tD\tD".split('\t')
      super.setRowHeader(RowHeader(columnNames,columnTypes))
    }

    override def process(r: Row): Unit = {

      bh = r.bH.asInstanceOf[binaryHolder]
      if (needsInitialization) {
        needsInitialization = false
        gtSize1 = bh.bui.GTS1.length
        gtSize2 = bh.bui.GTS2.length
        gtPairSize = gtSize1 * gtSize2
        IBS0 = new Array[Int](gtPairSize)
        XX = new Array[Int](gtPairSize)
        Nhet = new Array[Int](gtPairSize)
        Nhom = new Array[Int](gtPairSize)
        NAai = new Array[Int](gtPairSize)
        NAaj = new Array[Int](gtPairSize)
        tpq = new Array[Float](gtPairSize)
        kpq = new Array[Float](gtPairSize)
        count = new Array[Int](gtPairSize)
      }

      val af = bh.af
      var i: Int = 0
      while (i < gtSize1) {
        val gt1 = bh.bui.GTS1(i)
        var j: Int = 0
        while (j < gtSize2) {
          val gt2 = bh.bui.GTS2(j)
          if (gt1 != '3' && gt2 != '3') {
            val ai = i * gtSize2 + j
            count(ai) += 1
            tpq(ai) += 2.0f*af*af*(1.0f-af)*(1.0f-af)
            kpq(ai) += 2.0f*af*(1.0f-af)

            if (gt1 == '0' && gt2 == '2' || gt1 == '2' && gt2 == '0') IBS0(ai) += 1
            /*
            | calc IBS0 if(values='02' or values = '20',1,0)
            */
            if (gt1 == '0' && gt2 == '1' || gt1 == '1' && gt2 == '0' || gt1 == '2' && gt2 == '1' || gt1 == '1' && gt2 == '2') {
              XX(ai) += 1
            }
            else if (gt1 == '0' && gt2 == '2' || gt1 == '2' && gt2 == '0') {
              XX(ai) += 4
              Nhom(ai) += 1
            }
            /* else XX(ai) += 0
            | calc XX if(values='01' or values = '10' or values = '21' or values = '12',1,if(values='02' or values = '20',4,0))
            | calc Nhom if(values = '02' or values = '20',1,0)
            */
            if (gt1 == '1' && gt2 == '1') Nhet(ai) += 1
            /*
            | calc Nhet if(values = '11',1,0)
             */
            if (gt1 == '1') NAai(ai) += 1
            /*
            | calc NAai if(left(values,1)='1',1,0)
            */
            if (gt2 == '1') NAaj(ai) += 1
            /*
            | calc NAaj if(right(values,1)='1',1,0)
            */
          }
          j += 1
        }
        i += 1
      }
      if (gm != null && gm.isCancelled()) {
        reportWantsNoMore
        cancelled = true
      }
    }

    override def finish: Unit = {
      val skip_test = if (!t_pi0 && !t_phi && !t_theta) true else false
      if (!cancelled && !needsInitialization) {
        var i: Int = 0
        while (i < gtSize1) {
          val PNi = bh.bui.outputTags(i)
          var j: Int = 0
          while (j < gtSize2) {
            val PNj = bh.bui.outputTags2(j)
            val ai = i*gtSize2+j
            val pi0 = IBS0(ai)/tpq(ai)
            val phi = 0.5f-XX(ai)/(4.0f*kpq(ai))
            val theta = (Nhet(ai)-2.0f*Nhom(ai))/(NAai(ai)+NAaj(ai))
            if (skip_test || (!t_pi0 || pi0 < pi0thr) && (!t_phi || phi > phithr) && (!t_theta || theta > thetathr) ) {
              super.process(RowObj("chrA\t0\t" + PNi + '\t' + PNj + '\t' + IBS0(ai)
                + '\t' + XX(ai) + '\t' + tpq(ai) + '\t' + kpq(ai) + '\t' + Nhet(ai)
                + '\t' + Nhom(ai) + '\t' + NAai(ai) + '\t' + NAaj(ai) + '\t' + count(ai) + '\t' + pi0 + '\t' + phi + '\t' + theta))
            }
            j += 1
          }
          i += 1
        }
      }
    }
  }

  case class KingAnalysis(fileName1: String, iteratorCommand1: String, iterator1: LineIterator, fileName2: String, iteratorCommand2: String, iterator2: LineIterator, fileName3: String, iteratorCommand3: String, iterator3: LineIterator, buckCol: Int, valCol: Int,
                            grCols: List[Int], afCol : Int, sepVal: String, valSize: Int, uv: String, session: GorSession) extends
    BinAnalysis(RegularRowHandler(1), BinAggregator(KingFactory(session, fileName1 + "#" + iteratorCommand1 + "#" + fileName2 + "#" + iteratorCommand2 + "#" + fileName3 + "#" + iteratorCommand3, buckCol, valCol, grCols, afCol, sepVal, valSize, uv), 2, 1)) {

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
    val outputOrderMap2 = scala.collection.mutable.Map.empty[String, Int]
    var outputCounter2: Int = -1

    def outputOrder(b: String): Int = outputOrderMap.get(b) match {
      case Some(x) => x;
      case None =>
        outputCounter += 1
        outputOrderMap += (b -> outputCounter)
        outputCounter
    }
    def outputOrder2(b: String): Int = outputOrderMap2.get(b) match {
      case Some(x) => x;
      case None =>
        outputCounter2 += 1
        outputOrderMap2 += (b -> outputCounter2)
        outputCounter2
    }

    val lookupSignature: String = fileName1 + "#" + iteratorCommand1 + "#" + fileName2 + "#" + iteratorCommand2 + "#" + fileName3 + "#" + iteratorCommand3

    session.getCache.getObjectHashMap.computeIfAbsent(lookupSignature, f => {
      var l1 = Array.empty[String]
      var l2 = Array.empty[String]
      var l3 = Array.empty[String]

      try {
        if (iteratorCommand1 != "") l1 = MapAndListUtilities.getStringArray(iteratorCommand1, iterator1, session)
        else l1 = MapAndListUtilities.getStringArray(fileName1, session)

        if (iteratorCommand2 != "") l2 = MapAndListUtilities.getStringArray(iteratorCommand2, iterator2, session)
        else l2 = MapAndListUtilities.getStringArray(fileName2, session)

        if (iteratorCommand3 != "") l3 = MapAndListUtilities.getStringArray(iteratorCommand3, iterator3, session)
        else l3 = MapAndListUtilities.getStringArray(fileName3, session)
      } catch {
        case e: Exception =>
          iterator1.close()
          iterator2.close()
          iterator3.close()
          throw e
      }

      var tags: List[String] = Nil
      var tags2: List[String] = Nil
      l2.foreach(x => {
        outputOrder(x)
        tags ::= x
      })
      l3.foreach(x => {
        outputOrder2(x)
        tags2 ::= x
      })
      val bi = BucketInfo()
      val outputSize = outputOrderMap.size
      val outputSize2 = outputOrderMap2.size
      bi.outputBucketID = new Array[Int](outputSize)
      bi.outputBucketPos = new Array[Int](outputSize)
      bi.outputBucketID2 = new Array[Int](outputSize2)
      bi.outputBucketPos2 = new Array[Int](outputSize2)
      bi.GTS1 = new Array[Char](outputSize)
      bi.GTS2 = new Array[Char](outputSize2)

      val tagMap = outputOrderMap.clone()
      val tagMap2 = outputOrderMap2.clone()

      l1.foreach(x => {
        val r = x.split("\t")
        val (tag, bid) = (r(0), r(1))
        tagMap.remove(tag)
        tagMap2.remove(tag)

        val buckid = bucketID(bid)
        val buckpos = bucketOrder(bid, tag)

        if (outputOrder(tag) < outputSize) {
          bi.outputBucketID(outputOrder(tag)) = buckid
          bi.outputBucketPos(outputOrder(tag)) = buckpos
        }
        if (outputOrder2(tag) < outputSize2) {
          bi.outputBucketID2(outputOrder2(tag)) = buckid
          bi.outputBucketPos2(outputOrder2(tag)) = buckpos
        }
      })

      if (tagMap.nonEmpty) throw new GorDataException("There are tags in the second input file which are not defined in the first tag/bucket input, including: " + tagMap.keys.toList.slice(0, 10).mkString(","))
      if (tagMap2.nonEmpty) throw new GorDataException("There are tags in the third input file which are not defined in the first tag/bucket input, including: " + tagMap2.keys.toList.slice(0, 10).mkString(","))

      bi.bucketIDMap = bucketIDMap
      bi.outputTags = tags.reverse.toArray
      bi.outputTags2 = tags2.reverse.toArray
      bi
    })
  }
}
