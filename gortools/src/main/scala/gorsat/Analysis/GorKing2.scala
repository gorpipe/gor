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

import gorsat.Commands.{Analysis, BinAggregator, BinAnalysis, BinFactory, BinInfo, BinState, Processor, RegularRowHandler, RowHeader}
import gorsat.gorsatGorIterator.MapAndListUtilities
import gorsat.{PnBucketParsing, PnBucketTable}
import org.gorpipe.exceptions.GorDataException
import org.gorpipe.gor.model.{Line, Row}
import org.gorpipe.gor.monitor.GorMonitor
import org.gorpipe.gor.session.GorSession
import org.gorpipe.model.gor.RowObj
import org.gorpipe.model.gor.RowObj.BinaryHolder
import org.gorpipe.model.gor.iterators.LineIterator

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object GorKing2 {

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
      maxUsedBuckets = bui.numberOfBuckets
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

      bui.buckNameToIdx.get(r.colAsString(buckCol).toString) match {
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
            while (outCol < bui.numberOfPns) {
              val buckNo = bui.getBucketIdxFromPn(outCol)
              val buckPos = bui.getBucketPos(outCol)
              r = sh.buckRows(buckNo)
              val offset = sh.offsetArray(buckNo)
              if (r == null) {
                bui.GTS(outCol) = '3'
              } else {
                  if (valSize == -1) {
                  colCharMove(buckPos, r, offset, sh.splitArr(buckNo), bui.GTS, outCol)
                } else {
                    colCharMoveFixed(buckPos, r, offset, bui.GTS, outCol, valSize, sepSize)
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
            throw new GorDataException("Missing values in bucket " + r.toString.split("\t")(buckCol) + " in searching for tag " + bui.getPnNameFromIdx(outCol) + " no " + outCol + " in output\nin row\n" + r + "\n\n", e)
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

  case class BucketInfo(pbt: PnBucketTable, idPairs: Array[(Int,Int)], GTS : Array[Char])
    extends PnBucketTable(pbt.buckNameToIdx, pbt.buckIdxToName, pbt.buckIdxToBuckSize,
      pbt.pnToIdx, pbt.pnIdxToName, pbt.pnIdxToBuckIdx, pbt.pnIdxToBuckPos) {
  }

  case class KingAggregate(pi0thr: Float, phithr: Float, thetathr: Float, t_pi0: Boolean, t_phi: Boolean, t_theta: Boolean, maxVars : Int, gm : GorMonitor) extends Analysis {
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
    var gtSize: Int = 0
    var gtPairSize: Int = 0
    var cancelled: Boolean = false

    override def isTypeInformationMaintained : Boolean = true

    override def setRowHeader(header: RowHeader): Unit = {
      val columnNames: Array[String] = "Chrom\tPos\tPN1\tPN2\tIBS0\tXX\ttpq\tkpq\tNhet\tNhom\tNAai\tNAaj\tcount\tpi0\tphi\ttheta".split('\t')
      val columnTypes: Array[String] = "S\tI\tS\tS\tI\tI\tD\tD\tI\tI\tI\tI\tI\tD\tD\tD".split('\t')
      super.setRowHeader(RowHeader(columnNames,columnTypes))
    }

    var numberOfPNs = 1;
    var numMarkers = maxVars;
    var markerNumber = 0

    var gtbarray : Array[Array[Byte]] = null
    var tpqc : Array[Float] = null
    var kpqc : Array[Float] = null
    var idPairs: Array[(Int,Int)] = null

    def setGTb(pn : Int, marker : Int, gt : Char) : Unit = { /* only to be used once per seat */
      val shift = (marker % 4)*2
      val mpos = marker / 4
      gtbarray(pn)(mpos) = (gtbarray(pn)(mpos) | (((gt-'0') & 3) << shift)).toByte
    }
    def getGTb(pn : Int, marker : Int) : Char = { /* not used in the optimized access */
      val shift : Int = (marker % 4)*2
      val mpos : Int = marker / 4
      return ('0'+((gtbarray(pn)(mpos) >>> shift) & 3)).toChar
    }

    override def process(r: Row): Unit = {
      bh = r.bH.asInstanceOf[binaryHolder]
      if (needsInitialization) {
        needsInitialization = false
        numberOfPNs = bh.bui.GTS.length
        gtbarray = Array.ofDim[Byte](numberOfPNs,(numMarkers+3)/4)
        val af = bh.af
        tpqc = Array.ofDim[Float](numMarkers)
        kpqc = Array.ofDim[Float](numMarkers)
        idPairs = bh.bui.idPairs
      }
      val af = bh.af
      if (markerNumber < numMarkers) {
        tpqc(markerNumber) = 2.0f*af*af*(1.0f-af)*(1.0f-af)
        kpqc(markerNumber) = 2.0f*af*(1.0f-af)
        var pn = 0
        while (pn < numberOfPNs) {
          val gt = bh.bui.GTS(pn)
          setGTb(pn, markerNumber, gt)
          pn += 1
        }
        markerNumber += 1
      } else {
        throw new GorDataException(s"Too many variants: variant at ($r.chrom,$r.pos) exceeds the maximum of $numMarkers variants\n")
      }
    }

    def super_process_batch(outRows : List[Row]) = synchronized {
      outRows.foreach(super.process(_))
    }

    def process_group(pns1 : Array[Int], pns2 : Array[Int]): Unit = {
      val skip_test = if (!t_pi0 && !t_phi && !t_theta) true else false
      var outrows : List[Row] = Nil
      pns1.foreach(pn1 => {
        pns2.foreach(pn2 => {
          if (!wantsNoMore) {
            var IBS0: Int = 0
            var XX: Int = 0
            var Nhet: Int = 0
            var Nhom: Int = 0
            var NAai: Int = 0
            var NAaj: Int = 0
            var tpq: Double = 0.0
            var kpq: Double = 0.0
            var count: Int = 0

            var mi = 0
            var shift = 0
            var mpos = 0
            var temp1 = gtbarray(pn1)(mpos)
            var temp2 = gtbarray(pn2)(mpos)

            while (mi < markerNumber) {

              val gt1 = ('0'+((temp1 >>> shift) & 3)).toChar
              val gt2 = ('0'+((temp2 >>> shift) & 3)).toChar

              if (gt1 != '3' && gt2 != '3') {
                count += 1
                tpq += tpqc(mi)
                kpq += kpqc(mi)

                if (gt1 == '0' && gt2 == '2' || gt1 == '2' && gt2 == '0') IBS0 += 1

                if (gt1 == '0' && gt2 == '1' || gt1 == '1' && gt2 == '0' || gt1 == '2' && gt2 == '1' || gt1 == '1' && gt2 == '2') {
                  XX += 1
                } else if (gt1 == '0' && gt2 == '2' || gt1 == '2' && gt2 == '0') {
                  XX += 4
                  Nhom += 1
                }

                if (gt1 == '1' && gt2 == '1') Nhet += 1

                if (gt1 == '1') NAai += 1

                if (gt2 == '1') NAaj += 1
              }

              mi += 1
              shift += 2
              if (mi % 4 == 0 && mi < markerNumber) {
                mpos += 1; shift = 0; temp1 = gtbarray(pn1)(mpos); temp2 = gtbarray(pn2)(mpos);
              }
            }

            val PNi = bh.bui.getPnNameFromIdx(pn1)
            val PNj = bh.bui.getPnNameFromIdx(pn2)
            val pi0 = IBS0/tpq
            val phi = 0.5f-XX/(4.0f*kpq)
            val theta = (Nhet-2.0f*Nhom)/(NAai+NAaj)
            if (skip_test || (!t_pi0 || pi0 < pi0thr) && (!t_phi || phi > phithr) && (!t_theta || theta > thetathr) ) {
              outrows ::= RowObj("chrA\t0\t" + PNi + '\t' + PNj + '\t' + IBS0
                + '\t' + XX + '\t' + tpq + '\t' + kpq + '\t' + Nhet
                + '\t' + Nhom + '\t' + NAai + '\t' + NAaj + '\t' + count + '\t' + pi0 + '\t' + phi + '\t' + theta)

              if (outrows.length > 100) { super_process_batch(outrows); outrows = Nil }
            }
          }
        })
      })
      if (outrows.length > 0) { super_process_batch(outrows); outrows = Nil }
    }

    override def finish: Unit = {

        if (!cancelled && !needsInitialization) {
          var pns1 = idPairs.map(_._1).distinct
          var pns2 = idPairs.map(_._2).distinct
          if (pns2.length > pns1.length) {
            val temp = pns1
            pns1 = pns2
            pns2 = temp
          }
          val grsize = (pns1.length / 100).max(1) // split for max 100 threads
          val pns1groups = pns1.zipWithIndex.groupBy(_._2 / grsize).map(_._2).map(_.map(_._2))
          pns1groups.foreach(x => process_group(x, pns2))
          gtbarray = Array.ofDim[Byte](1,1) /* Free the memory */
        }
      }
  }

  case class KingAnalysis(fileName1: String, iteratorCommand1: String, iterator1: LineIterator, fileName2: String, iteratorCommand2: String, iterator2: LineIterator, buckCol: Int, valCol: Int,
                            grCols: List[Int], afCol : Int, sepVal: String, valSize: Int, uv: String, session: GorSession) extends
    BinAnalysis(RegularRowHandler(1), BinAggregator(KingFactory(session, "king2#"+fileName1 + "#" + iteratorCommand1 + "#" + fileName2 + "#" + iteratorCommand2, buckCol, valCol, grCols, afCol, sepVal, valSize, uv), 2, 1)) {

    val lookupSignature: String = "king2#"+fileName1 + "#" + iteratorCommand1 + "#" + fileName2 + "#" + iteratorCommand2

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

      val gtPairSize = l2.length
      val idPairs = new Array[(Int,Int)](gtPairSize)
      var idPairCount = 0
      val idNameToIdx = mutable.Map.empty[String, Int]
      val tags = ArrayBuffer.empty[String]

      l2.foreach(x => {
        val tabIdx = x.indexOf('\t')
        val (r0, r1) = (x.substring(0, tabIdx), x.substring(tabIdx + 1))
        val id1 = idNameToIdx.get(r0) match {
          case Some(idx) => idx
          case None =>
            val idx = idNameToIdx.size
            tags += r0
            idNameToIdx += (r0 -> idx)
            idx
        }
        val id2 = idNameToIdx.get(r1) match {
          case Some(idx) => idx
          case None =>
            val idx = idNameToIdx.size
            tags += r1
            idNameToIdx += (r1 -> idx)
            idx
        }
        idPairs(idPairCount) = (id1,id2)
        // System.out.println(idPairCount+" ("+id1+","+id2+") = ("+r(0)+","+r(1)+") ("+tags(id1)+","+tags(id2)+")")
        idPairCount += 1
      })
      val pbt = PnBucketParsing.parse(l1).filter(tags)
      val GTS = new Array[Char](pbt.numberOfPns)
      BucketInfo(pbt, idPairs, GTS)
    })
  }
}
