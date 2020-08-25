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

import gorsat.Commands._
import gorsat.Utilities.StringUtilities
import org.gorpipe.gor.model.Row

import scala.collection.mutable.ArrayBuffer

// For the time being, RANGE aggregation is implemented separately (with code duplication :)
case class RangeAggregate(maxRange: Int, useCount: Boolean, useCdist: Boolean, useMax: Boolean, useMin: Boolean,
                          useMed: Boolean, useDis: Boolean, useSet: Boolean, useLis: Boolean, useAvg: Boolean,
                          useStd: Boolean, useSum: Boolean,
                          acCols: List[Int], icCols: List[Int], fcCols: List[Int], grCols: List[Int], setLen: Int,
                          sepVal: String, outgoingHeader: RowHeader) extends Analysis {

  override def isTypeInformationMaintained: Boolean = true

  override def setRowHeader(header: RowHeader): Unit = {
    rowHeader = header
    if (pipeTo != null) {
      pipeTo.setRowHeader(outgoingHeader.propagateTypes(rowHeader))
    }
  }

  case class StatHolder(numCols: Int) {
    val sums = new Array[Double](numCols)
    val sq_sums = new Array[Double](numCols)
    val fMax = new Array[Double](numCols)
    val fMin = new Array[Double](numCols)
    val aMax = new Array[String](numCols)
    val aMin = new Array[String](numCols)
    val ns = new Array[Int](numCols)
    val aList = new Array[List[String]](numCols)
    val fList = new Array[List[Double]](numCols)
    var gList = List.empty[String]
    var allCount: Long = 0
    var pos = 0
    var chr = "chr"
    var lineSet = false
    var line = ""
  }

  val anyCols: List[(Int, Char)] = (acCols.map((_, 'a')) ::: icCols.map((_, 'i')) ::: fcCols.map((_, 'f'))).sortWith(
    (x, y) => x._1 < y._1)
  val numCols: Int = anyCols.size
  val collectLists: Boolean = useDis || useMed || useSet || useLis
  val useGroup: Boolean = if (grCols.nonEmpty) true else false

  var groupMap = scala.collection.mutable.HashMap.empty[String, StatHolder]
  var singleStatHolder = StatHolder(numCols)
  if (!useGroup) groupMap += ("theOnlyGroup" -> singleStatHolder)
  val grColsArray: Array[Int] = grCols.toArray

  var rowBuffer = new Array[ArrayBuffer[Row]](2)
  rowBuffer(0) = new ArrayBuffer[Row]
  rowBuffer(1) = new ArrayBuffer[Row]
  var buffer = 0
  var bufferSize = 0
  var rowCount = 0
  var bufferChr = "chr"
  val emptyCheckSize = 10000

  def formatDouble(d: Double): String = (d formatted "%1.1f").replace(',', '.')

  def initStatHolder(sh: StatHolder, chr: String, pos: Int) {
    var i = 0
    while (i < anyCols.size) {
      sh.sums(i) = 0.0
      sh.sq_sums(i) = 0.0
      sh.ns(i) = 0
      sh.aList(i) = Nil
      sh.fList(i) = Nil
      i += 1
    }
    sh.allCount = 0
    sh.gList = Nil
    sh.pos = pos
    sh.chr = chr
    sh.line = ""
    sh.lineSet = false
  }

  def initializeBucket(chr: String, pos: Int) {
    if (useGroup) {
      groupMap = scala.collection.mutable.HashMap.empty[String, StatHolder]
    } else {
      initStatHolder(singleStatHolder, chr, pos)
    }
  }

  def outputLine(r: Row) {
    var sh: StatHolder = null
    if (useGroup) {
      val key = r.selectedColumns(grColsArray)
      try {
        sh = groupMap(key)
      } catch {
        case e: Exception => System.err.println("error " + buffer + " " + r + "\n"); throw e
      }
    } else {
      sh = groupMap("theOnlyGroup")
    }

    if (!sh.lineSet) {
      val lineBuilder = new StringBuilder()
      if (useCount) {
        lineBuilder.append('\t')
        lineBuilder.append(sh.allCount)
      }
      if (useCdist) {
        lineBuilder.append('\t')
        lineBuilder.append(sh.gList.distinct.size)
      }
      var i = 0
      while (i < anyCols.size) {
        val cType = anyCols(i)._2
        if (sh.ns(i) > 0) {
          if (cType == 'i' || cType == 'f') {
            val mean = sh.sums(i) / sh.ns(i)
            val variance = (sh.sq_sums(i) / sh.ns(i) - mean * mean).abs
            if (cType == 'i') {
              if (useMin) {
                lineBuilder.append('\t')
                lineBuilder.append(sh.fMin(i).toLong)
              }
              if (useMed) {
                val fArr = sh.fList(i).sorted.toArray
                lineBuilder.append('\t')
                lineBuilder.append(fArr(fArr.length / 2).toLong)
              }
              if (useMax) {
                lineBuilder.append('\t')
                lineBuilder.append(sh.fMax(i).toLong)
              }
              if (useSet) {
                val y = sh.fList(i).map(_.toLong).distinct.sorted
                lineBuilder.append('\t')
                StringUtilities.addWhile(lineBuilder, lineBuilder.length + setLen, sepVal, y)
              }
              if (useLis) {
                val y = sh.fList(i).map(_.toLong).reverse
                lineBuilder.append('\t')
                StringUtilities.addWhile(lineBuilder, lineBuilder.length + setLen, sepVal, y)
              }
            } else {
              if (useMin) {
                lineBuilder.append('\t')
                lineBuilder.append(sh.fMin(i))
              }
              if (useMed) {
                val fArr = sh.fList(i).sorted.toArray
                lineBuilder.append('\t')
                lineBuilder.append(fArr(fArr.length / 2))
              }
              if (useMax) {
                lineBuilder.append('\t')
                lineBuilder.append(sh.fMax(i))
              }
              if (useSet) {
                val y = sh.fList(i).distinct.sorted
                lineBuilder.append('\t')
                StringUtilities.addWhile(lineBuilder, lineBuilder.length + setLen, sepVal, y)
              }
              if (useLis) {
                val y = sh.fList(i).reverse
                lineBuilder.append('\t')
                StringUtilities.addWhile(lineBuilder, lineBuilder.length + setLen, sepVal, y)
              }
            }
            if (useDis) {
              lineBuilder.append('\t')
              lineBuilder.append(sh.fList(i).distinct.length)
            }
            if (useAvg) {
              lineBuilder.append('\t')
              lineBuilder.append(mean)
            }
            if (useStd) {
              lineBuilder.append('\t')
              lineBuilder.append(scala.math.sqrt(variance))
            }
            if (useSum) {
              if (cType == 'i') {
                lineBuilder.append('\t')
                lineBuilder.append(sh.sums(i).toLong)
              } else {
                lineBuilder.append('\t')
                lineBuilder.append(sh.sums(i))
              }
            }
          } else { // the 'a' case
            if (useMin) {
              lineBuilder.append('\t')
              lineBuilder.append(sh.aMin(i))
            }
            if (useMed) {
              val aArr: Array[String] = sh.aList(i).sorted.toArray
              lineBuilder.append('\t')
              lineBuilder.append(aArr(aArr.length / 2))
            }
            if (useMax) {
              lineBuilder.append('\t')
              lineBuilder.append(sh.aMax(i))
            }
            if (useSet) {
              val y = sh.aList(i).distinct.sorted
              lineBuilder.append('\t')
              StringUtilities.addWhile(lineBuilder, lineBuilder.length + setLen, sepVal, y)
            }
            if (useLis) {
              val y = sh.aList(i).reverse
              lineBuilder.append('\t')
              StringUtilities.addWhile(lineBuilder, lineBuilder.length + setLen, sepVal, y)
            }
            if (useDis) {
              lineBuilder.append('\t')
              lineBuilder.append(sh.aList(i).distinct.length)
            }
          }
        } else {
          if (useMin) lineBuilder.append('\t')
          if (useMed) lineBuilder.append('\t')
          if (useMax) lineBuilder.append('\t')
          if (useSet) lineBuilder.append('\t')
          if (useLis) lineBuilder.append('\t')
          if (useDis) lineBuilder.append('\t')
          if (cType == 'i' || cType == 'f') {
            if (useAvg) lineBuilder.append('\t')
            if (useStd) lineBuilder.append('\t')
            if (useSum) lineBuilder.append('\t')
          }
        }
        i += 1
      }
      sh.line = lineBuilder.substring(1, lineBuilder.length)
      sh.lineSet = true
    }
    super.process(r.rowWithAddedColumn(sh.line))
  }

  override def process(r: Row) {
    if (rowCount > emptyCheckSize || r.chr != bufferChr) {
      rowCount = 0
      var nextBufferSize = 0
      val nextBuffer = (buffer + 1) % 2
      var i = 0
      while (i < bufferSize) {
        val ri = rowBuffer(buffer)(i)
        if (ri.pos < r.pos - maxRange && r.chr == ri.chr || ri.chr != r.chr) {
          outputLine(ri)
        } else {
          if (rowBuffer(nextBuffer).size <= nextBufferSize) {
            rowBuffer(nextBuffer) += ri
          } else {
            rowBuffer(nextBuffer)(nextBufferSize) = ri
          }
          nextBufferSize += 1
        }
        rowBuffer(buffer)(i) = null
        i += 1
      }
      bufferSize = nextBufferSize
      buffer = nextBuffer
      if (useGroup) {
        groupMap.keys.foreach(k => {
          val gr = groupMap(k)
          if (gr.pos < r.pos - maxRange && gr.chr == r.chr || gr.chr != r.chr) groupMap.remove(k)
        })
      }
      bufferChr = r.chr
    }

    rowCount += 1
    if (rowBuffer(buffer).size <= bufferSize) {
      rowBuffer(buffer) += r
    } else {
      rowBuffer(buffer)(bufferSize) = r
    }
    bufferSize += 1

    var sh: StatHolder = null
    if (useGroup) {
      val groupID = r.selectedColumns(grColsArray)
      groupMap.get(groupID) match {
        case Some(x) => sh = x
        case None =>
          sh = StatHolder(numCols)
          initStatHolder(sh, r.chr, r.pos)
          groupMap += (groupID -> sh)
      }
    } else {
      sh = singleStatHolder
    }
    sh.pos = r.pos
    sh.lineSet = false

    sh.allCount += 1
    if (useCdist) sh.gList ::= r.toString
    var i = 0
    while (i < anyCols.size) {
      val j = anyCols(i)._1
      val cType = anyCols(i)._2
      if (cType == 'f' || cType == 'i') {
        try {
          val v = r.colAsDouble(j)
          if (!v.isNaN) {
            if (collectLists) sh.fList(i) ::= v
            sh.sums(i) += v
            sh.sq_sums(i) += v * v
            if (sh.ns(i) == 0) {
              sh.fMin(i) = v
              sh.fMax(i) = v
            } else {
              if (v < sh.fMin(i)) sh.fMin(i) = v
              if (v > sh.fMax(i)) sh.fMax(i) = v
            }
            sh.ns(i) += 1
          }
        } catch {
          case _: Exception => /* do nothing */
        }
      }
      else if (cType == 'a') {
        try {
          val v = r.colAsString(j).toString
          if (collectLists) sh.aList(i) ::= v
          if (sh.ns(i) == 0) {
            sh.aMin(i) = v
            sh.aMax(i) = v
          } else {
            if (v < sh.aMin(i)) sh.aMin(i) = v
            if (v > sh.aMax(i)) sh.aMax(i) = v
          }
          sh.ns(i) += 1
        } catch {
          case _: Exception => /* do nothing */
        }
      }
      i += 1
    }

  }

  override def finish {
    var i = 0
    while (i < bufferSize) {
      outputLine(rowBuffer(buffer)(i)); i += 1
    }
  }
}


// Aggregate the columns per bin in the stream
case class AggregateState(binSize: Int, useCount: Boolean, useCdist: Boolean, useMax: Boolean, useMin: Boolean,
                          useMed: Boolean, useDis: Boolean, useSet: Boolean, useLis: Boolean, useAvg: Boolean,
                          useStd: Boolean, useSum: Boolean,
                          acCols: List[Int], icCols: List[Int], fcCols: List[Int], grCols: List[Int], setLen: Int,
                          sepVal: String) extends BinState {

  case class StatHolder(numCols: Int) {
    val sums = new Array[Double](numCols)
    val sq_sums = new Array[Double](numCols)
    val fMax = new Array[Double](numCols)
    val fMin = new Array[Double](numCols)
    val aMax = new Array[String](numCols)
    val aMin = new Array[String](numCols)
    val ns = new Array[Int](numCols)
    val aList = new Array[List[String]](numCols)
    val fList = new Array[List[Double]](numCols)
    var gList = List.empty[String]
    var allCount = 0
    var lineSet = false
    var line = ""
  }

  def maxLen(s: String, maxLen: Int = 200): String = {
    if (s.length > maxLen) {
      s.substring(0, maxLen) + "..."
    } else {
      s
    }
  }

  val anyCols: List[(Int, Char)] = (acCols.map((_, 'a')) ::: icCols.map((_, 'i')) ::: fcCols.map((_, 'f'))).sortWith(
    (x, y) => x._1 < y._1)
  val numCols: Int = anyCols.size
  val collectLists: Boolean = useDis || useMed || useSet || useLis
  val useGroup: Boolean = if (grCols.nonEmpty) true else false

  var groupMap = Map.empty[String, StatHolder]
  var singleStatHolder = StatHolder(numCols)
  if (!useGroup) groupMap += ("theOnlyGroup" -> singleStatHolder)
  val grColsArray: Array[Int] = grCols.toArray

  var allRows = new ArrayBuffer[Row]

  def formatDouble(d: Double): String = (d formatted "%1.1f").replace(',', '.')

  def initStatHolder(sh: StatHolder) {
    var i = 0
    while (i < anyCols.size) {
      sh.sums(i) = 0.0
      sh.sq_sums(i) = 0.0
      sh.ns(i) = 0
      sh.aList(i) = Nil
      sh.fList(i) = Nil
      i += 1
    }
    sh.allCount = 0
    sh.gList = Nil
    sh.line = ""
    sh.lineSet = false
  }


  def initialize(binInfo: BinInfo): Unit = {
    if (useGroup) {
      groupMap = Map.empty[String, StatHolder]
    } else {
      initStatHolder(singleStatHolder)
    }
    allRows = new ArrayBuffer[Row]
  }

  def process(r: Row) {
    allRows += r
    var sh: StatHolder = null
    if (useGroup) {
      val groupID = r.selectedColumns(grColsArray)
      groupMap.get(groupID) match {
        case Some(x) => sh = x
        case None =>
          sh = StatHolder(numCols)
          initStatHolder(sh)
          groupMap += (groupID -> sh)
      }
    } else {
      sh = singleStatHolder
    }
    sh.allCount += 1
    if (useCdist) sh.gList ::= r.toString
    var i = 0
    while (i < anyCols.size) {
      val j = anyCols(i)._1
      val cType = anyCols(i)._2
      if (cType == 'f' || cType == 'i') {
        try {
          val v = r.colAsDouble(j)
          if (!v.isNaN) {
            if (collectLists) sh.fList(i) ::= v
            sh.sums(i) += v
            sh.sq_sums(i) += v * v
            if (sh.ns(i) == 0) {
              sh.fMin(i) = v
              sh.fMax(i) = v
            } else {
              if (v < sh.fMin(i)) sh.fMin(i) = v
              if (v > sh.fMax(i)) sh.fMax(i) = v
            }
            sh.ns(i) += 1
          }
        } catch {
          case _: Exception => /* do nothing */
        }
      }
      else if (cType == 'a') {
        try {
          val v = r.colAsString(j).toString
          if (collectLists) sh.aList(i) ::= v
          if (sh.ns(i) == 0) {
            sh.aMin(i) = v
            sh.aMax(i) = v
          } else {
            if (v < sh.aMin(i)) sh.aMin(i) = v
            if (v > sh.aMax(i)) sh.aMax(i) = v
          }
          sh.ns(i) += 1
        } catch {
          case _: Exception => /* do nothing */
        }
      }
      i += 1
    }
  }

  def sendToNextProcessor(bi: BinInfo, nextProcessor: Processor) {
    allRows.foreach(r => {
      var sh: StatHolder = null
      if (useGroup) {
        val key = grColsArray.map(r.colAsString).mkString("\t")
        sh = groupMap(key)
      } else {
        sh = groupMap("theOnlyGroup")
      }

      if (!sh.lineSet) {
        val lineBuilder = new StringBuilder()
        if (useCount) {
          lineBuilder.append('\t')
          lineBuilder.append(sh.allCount)
        }
        if (useCdist) {
          lineBuilder.append('\t')
          lineBuilder.append(sh.gList.distinct.size)
        }
        var i = 0
        while (i < anyCols.size) {
          val cType = anyCols(i)._2
          if (sh.ns(i) > 0) {
            if (cType == 'i' || cType == 'f') {
              val mean = sh.sums(i) / sh.ns(i)
              val variance = sh.sq_sums(i) / sh.ns(i) - mean * mean
              if (cType == 'i') {
                if (useMin) {
                  lineBuilder.append('\t')
                  lineBuilder.append(sh.fMin(i).toLong)
                }
                if (useMed) {
                  val fArr = sh.fList(i).sorted.toArray
                  lineBuilder.append('\t')
                  lineBuilder.append(fArr(fArr.length / 2).toLong)
                }
                if (useMax) {
                  lineBuilder.append('\t')
                  lineBuilder.append(sh.fMax(i).toLong)
                }
                if (useSet) {
                  val y = sh.fList(i).map(_.toLong).distinct.sorted
                  lineBuilder.append('\t')
                  StringUtilities.addWhile(lineBuilder, lineBuilder.length + setLen, sepVal, y)
                }
                if (useLis) {
                  val y = sh.fList(i).map(_.toLong).reverse
                  lineBuilder.append('\t')
                  StringUtilities.addWhile(lineBuilder, lineBuilder.length + setLen, sepVal, y)
                }
              } else { // Double
                if (useMin) {
                  lineBuilder.append('\t')
                  lineBuilder.append(sh.fMin(i))
                }
                if (useMed) {
                  val fArr = sh.fList(i).sorted.toArray
                  lineBuilder.append('\t')
                  lineBuilder.append(fArr(fArr.length / 2))
                }
                if (useMax) {
                  lineBuilder.append('\t')
                  lineBuilder.append(sh.fMax(i))
                }
                if (useSet) {
                  val y = sh.fList(i).distinct.sorted
                  lineBuilder.append('\t')
                  StringUtilities.addWhile(lineBuilder, lineBuilder.length + setLen, sepVal, y)
                }
                if (useLis) {
                  val y = sh.fList(i).reverse
                  lineBuilder.append('\t')
                  StringUtilities.addWhile(lineBuilder, lineBuilder.length + setLen, sepVal, y)
                }
              }
              if (useDis) {
                lineBuilder.append('\t')
                lineBuilder.append(sh.fList(i).distinct.length)
              }
              if (useAvg) {
                lineBuilder.append('\t')
                lineBuilder.append(mean)
              }
              if (useStd) {
                lineBuilder.append('\t')
                lineBuilder.append(scala.math.sqrt(variance))
              }
              if (useSum) {
                if (cType == 'i') {
                  lineBuilder.append('\t')
                  lineBuilder.append(sh.sums(i).toLong)
                } else {
                  lineBuilder.append('\t')
                  lineBuilder.append(sh.sums(i))
                }
              }
            } else { // the 'a' case
              if (useMin) {
                lineBuilder.append('\t')
                lineBuilder.append(sh.aMin(i))
              }
              if (useMed) {
                val aArr: Array[String] = sh.aList(i).sorted.toArray
                lineBuilder.append('\t')
                lineBuilder.append(aArr(aArr.length / 2))
              }
              if (useMax) {
                lineBuilder.append('\t')
                lineBuilder.append(sh.aMax(i))
              }
              if (useSet) {
                val y = sh.aList(i).distinct.sorted
                lineBuilder.append('\t')
                StringUtilities.addWhile(lineBuilder, lineBuilder.length + setLen, sepVal, y)
              }
              if (useLis) {
                val y = sh.aList(i).reverse
                lineBuilder.append('\t')
                StringUtilities.addWhile(lineBuilder, lineBuilder.length + setLen, sepVal, y)
              }
              if (useDis) {
                lineBuilder.append('\t')
                lineBuilder.append(sh.aList(i).distinct.length)
              }
            }
          } else {
            if (useMin) lineBuilder.append('\t')
            if (useMed) lineBuilder.append('\t')
            if (useMax) lineBuilder.append('\t')
            if (useSet) lineBuilder.append('\t')
            if (useLis) lineBuilder.append('\t')
            if (useDis) lineBuilder.append('\t')
            if (cType == 'i' || cType == 'f') {
              if (useAvg) lineBuilder.append('\t')
              if (useStd) lineBuilder.append('\t')
              if (useSum) lineBuilder.append('\t')
            }
          }
          i += 1
        }
        sh.line = lineBuilder.substring(1, lineBuilder.length)
        sh.lineSet = true
      }
      nextProcessor.process(r.rowWithAddedColumn(sh.line))
    })
  }
}
