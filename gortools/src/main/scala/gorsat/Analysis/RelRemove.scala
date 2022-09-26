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

import gorsat.Commands.Analysis
import gorsat.DynIterator.DynamicNorSource
import gorsat.gorsatGorIterator.MapAndListUtilities
import gorsat.gorsatGorIterator.MapAndListUtilities.singleHashMap
import org.gorpipe.gor.model.Row
import org.gorpipe.gor.session.GorSession
import org.gorpipe.model.gor.RowObj
import org.gorpipe.model.gor.iterators.LineIterator

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer



case class RelRemove(session: GorSession,
                     rightSource: DynamicNorSource, sepcc : Boolean, removeSymbol : String, colNum : Int, weightCols : List[Int]) extends Analysis {

  var iooa_counter = -1
  val orderMap = scala.collection.mutable.Map.empty[String, Int]
  def iooa(s: String): Int = {
    orderMap.get(s) match {
        case Some(x) => x;
        case None =>
          iooa_counter += 1; orderMap += (s -> iooa_counter); iooa_counter
    }
  }

  var allRows = new ArrayBuffer[Row]
  var relationships = new ArrayBuffer[(Int,Int)]
  var weights = new ArrayBuffer[Int]
  val useWeight = if (weightCols.size == 1) true else false
  val weightCol = if (useWeight) weightCols.head else -1

  override def setup(): Unit = {
    allRows = new ArrayBuffer[Row]
    relationships = new ArrayBuffer[(Int,Int)]
  }

  override def process(r: Row): Unit = {
    val pni = iooa(r.colAsString(2).toString)
    if (pni >= allRows.size) allRows += r else allRows(pni) = r
    var weight = 0
    if (useWeight) {
      try {
        weight = r.colAsInt(weightCol)
      } catch {
        case e : Exception => weight = 0
      }
      if (weight > 100) weight = 100
      if (weight < 0) weight = 0
    }
    if (pni >= weights.size) weights += weight else weights(pni) = weight
  }

  override def finish(): Unit = {
    readRelationships()
    var repSyms = new Array[String](colNum)
    var repVals = Array.ofDim[Byte](allRows.size,colNum)
    for (col <- 0 until colNum) {
      if (!useWeight || col+3 != weightCol) {
        val (cc, qt, plink, card, rSym) = setupForColumn(3 + col)
        val rs = if (plink && card.contains("0")) "0" else if (plink && card.contains("-9")) "-9" else if (card.contains("EXCL")) "EXCL" else "NA"
        repSyms(col) = rs
        relativeRank()
        relativeElimination()
        var i = 0
        while (i < allRows.size) {
          repVals(i)(col) = if (realStates(i).elim) 1.toByte else 0.toByte
          i += 1
        }
      }
    }

    var i = 0
    while (i < allRows.size) {
      if (repVals(i).exists(x => x == 1.toByte)) {
        var x = allRows(i).colsSlice(0,3)
        var c = 0
        while (c < colNum) {
          if (repVals(i)(c) == 1.toByte) x += "\t" + (if (removeSymbol == "") repSyms(c) else removeSymbol)
          else x += "\t" + allRows(i).colAsString(3+c).toString
          c += 1
        }
        super.process(RowObj(x))
      } else super.process(allRows(i))
      i += 1
    }
    repSyms = null
    repVals = null
    allRows = null
    relationships = null
    weights = null
  }

  def readRelationships()= {
    while (rightSource.hasNext) {
      val relRow = rightSource.next()
      var pn1 : Int = -1
      var pn2 : Int = -1
      orderMap.get(relRow.colAsString(2).toString) match { /* First column in a NOR relation has index 2 */
        case Some(x) => pn1 = x;
        case None => pn1 = -1;
      }
      orderMap.get(relRow.colAsString(3).toString) match {
        case Some(x) => pn2 = x;
        case None => pn2 = -1;
      }
      if (pn1 >= 0 && pn2 >= 0) { val x = if (pn1 < pn2) (pn1,pn2) else (pn2,pn1); if (pn1!=pn2) relationships += x } /* transitivity assumed */
    }
    relationships = relationships.distinct /* we don't want duplicate relationships, e.g. we assume implicit transitivity */
  }

  case class realState(var inUse : Boolean, var aCase : Boolean, var Score : Int, var elim : Boolean, var needCheck : Boolean, var rels : List[Int])

  var realStates : Array[realState] = null;

  def analyzeCardinality(col : Int) = {
    var s = scala.collection.mutable.Set.empty[String];
    var rSymNA = false
    var rSymEXCL = false
    var i = 0;
    while (i < allRows.size && s.size < 4) {
      s += allRows(i).colAsString(col).toString.toUpperCase
      i += 1
    }
    var qt = true
    i = 0
    while (i < allRows.size && qt == true) {
      val x = allRows(i).colAsString(col).toString.toUpperCase
      if (x == "EXCL" || x == "NA") {
        if (x == "NA") rSymNA = true
        if (x == "EXCL") rSymEXCL = true
      } else if ( x.length >= 1 && (x.charAt(0) >= '0' && x.charAt(0) <= '9') || x.charAt(0) == '.' || x.charAt(0) == '-' || x.charAt(0) == 'N') {
        try {
          x.toDouble
        } catch {
          case e : Exception => qt = false
        }
      } else {
        qt = false;
      }
      i += 1
    }
    val card = if (s.size < 4) s.toList.sorted.mkString(",") else ""
    if (card.contains("EXCL")) rSymEXCL = true
    if (card.contains("NA")) rSymNA = true
    val plink = if (card == "0,1,2" || card == "-9,1,2" || card == "1,2,NA" || card == "1,2" || card == "-9,1" || card == "-9,2") true else false
    val rSym = if (rSymEXCL) "EXCL" else if (rSymNA) "NA" else if (card == "0,1,2") "0" else if (card == "-9,1,2" || card == "-9,1" || card == "-9,2") "-9" else "NA"
    val cc = if (plink || card == "0,1" || card == "0,1,NA" || card == "-9,0,1" || card == "CASE,CTRL" || card == "CASE,CTRL,EXCL" || card == "CASE,CTRL,NA"
      || card == "CASE,EXCL" || card == "CTRL,EXCL" || card == "CASE,NA" || card == "CTRL,NA" || card == "CASE" || card == "CTRL") true else false
    (cc, qt, plink, card, rSym)
  }

  def setupForColumn(col : Int) = {
    val (cc,qt,plink,card,rSym) = analyzeCardinality(col)
    realStates = new Array[realState](allRows.size)
    val ctrlShiftScore = 10000; /* assumes that relative count cannot exceed 10k */
    var i = 0
    while (i < realStates.size) {
      val weightScore = (100-weights(i)) * ctrlShiftScore * 10 /* High weight lowers score, i.e. less likely to be removed */
      val pheno = allRows(i).colAsString(col).toString.toUpperCase
      if (cc) { /* CC traits */
        val aCase = if (plink && pheno == "2" || !plink && (pheno == "1" || pheno == "CASE") ) true else false
        val aCtrl = if (!aCase && (plink && pheno == "1" || !plink && (pheno == "0" || pheno == "CTRL") )) true else false
        if (aCase) {
          realStates(i) = new realState(true, true, weightScore, false, false, Nil)
        } else if (aCtrl) {
          realStates(i) = new realState(true, false, ctrlShiftScore + weightScore, false, false, Nil)
        } else {
          realStates(i) = new realState(false, false, weightScore, false, false, null)
        }
      } else if (qt) { /* QT trait */
        if (pheno != "EXCL" && pheno != "NA") {
          realStates(i) = new realState(true, false, weightScore, false, false, Nil)
        } else {
          realStates(i) = new realState(false, false, weightScore, false, false, null)
        }
      } else { /* illegal trait */
        realStates(i) = new realState(false, false, weightScore, false, false, null)
      }
      i += 1
    }
    relationships.foreach(pair => {
      if (realStates(pair._1).inUse && realStates(pair._2).inUse && (!sepcc || realStates(pair._1).aCase == realStates(pair._2).aCase)) {
        realStates(pair._1).rels ::= pair._2
        realStates(pair._2).rels ::= pair._1
      }
    })
    (cc,qt,plink,card,rSym)
  }

  val realRankMap = scala.collection.mutable.Map.empty[Int, scala.collection.mutable.Set[Int]];

  def relativeRank() = {
    var i = 0
    while (i < realStates.size) {
      if (realStates(i).inUse) {
        val relCount = realStates(i).rels.size
        realRankMap.get(relCount + realStates(i).Score) match {
          case Some(x) => x += i
          case None => if (relCount > 0) { realRankMap += (relCount + realStates(i).Score -> scala.collection.mutable.Set(i)) }
        }
      }
      i += 1
    }
   // realRankMap.toList.sortBy(_._1).foreach( x => println(x._1+" - set size "+x._2.size))
  }

  def relativeElimination() = {
    while (realRankMap.size > 0) {
      val largestGroup = realRankMap.keys.toList.max
        while (realRankMap(largestGroup).size > 0) {
        val pnToRemove = realRankMap(largestGroup).head
        if (realStates(pnToRemove).elim) throw new Exception("already eliminated:"+pnToRemove)

        if (realStates(pnToRemove).needCheck) { /* we need to prune the relatives and move the pn into new rank group */
          realStates(pnToRemove).needCheck = false
          var newRels : List[Int] = Nil
          realStates(pnToRemove).rels.foreach(x => {if (!realStates(x).elim) newRels ::= x })

          val newRank = largestGroup - (realStates(pnToRemove).rels.size - newRels.size)
          realStates(pnToRemove).rels = newRels
          realRankMap(largestGroup) -= pnToRemove /* remove from the old rank group and add to new one */
          if (newRels.size > 0) { /* only put to a lower group if it still has relatives */
            realRankMap.get(newRank) match {
              case Some(x) => x += pnToRemove
              case None => realRankMap += (newRank -> scala.collection.mutable.Set(pnToRemove))
            }
          }
        } else { /* mark the relatives before we delete this sample */
          realStates(pnToRemove).rels.foreach(x => { realStates(x).needCheck = true})
          realStates(pnToRemove).elim = true
          realRankMap(largestGroup) -= pnToRemove
        }
      }  /* continue with the max-rank group */
      realRankMap.remove(largestGroup)
    } /* continue until all groups are empty */

  }

}
