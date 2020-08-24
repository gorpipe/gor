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

import gorsat.AnalysisUtilities._
import gorsat.Commands.CommandParseUtilities._
import gorsat.Commands.{Analysis, CommandParsingResult}
import gorsat.Iterators.ChromBoundedIteratorSource
import gorsat.process.SourceProvider
import gorsat.{Analysis, IteratorUtilities}
import org.gorpipe.exceptions.{GorDataException, GorParsingException}
import org.gorpipe.gor.{GorConstants, GorContext}
import org.gorpipe.model.genome.files.gor.Row
import org.gorpipe.model.gor.iterators.{RowSource, SingleFileSource}
import org.gorpipe.model.gor.{MemoryMonitorUtil, RowObj}
import org.slf4j.LoggerFactory

import scala.collection.mutable.ListBuffer

object  JoinAnalysis {

  private val logger = LoggerFactory.getLogger(this.getClass)

  case class ParameterHolder(varsegleft: Boolean, varsegright: Boolean, lref: Int, rref: Int, negjoin: Boolean, caseInsensitive: Boolean, ic: Boolean, ir: Boolean)

  case class SegOverlap(ph: ParameterHolder, inRightSource: RowSource, missingSeg: String, leftJoin: Boolean, fuzzFactor: Int, iJoinType: String,
                        lstop: Int, rstop: Int, lleq: List[Int], lreq: List[Int], maxSegSize: Int, plain: Boolean, inclusOnly: Boolean = false) extends Analysis {

    var rightSource = new ChromBoundedIteratorSource(inRightSource)
    var rightSourceMonitorUtil: MemoryMonitorUtil = if (MemoryMonitorUtil.memoryMonitorActive) new MemoryMonitorUtil(MemoryMonitorUtil.basicOutOfMemoryHandler) else null
    type myRowBufferType = scala.collection.mutable.ArrayBuffer[SEGinfo]
    var lastRightChr: String = GorConstants.FIRST_POSSIBLE_CHROMOSOME_VALUE
    var lastRightPos = 0
    var maxLeftStop: Int = -1
    var lastLeftChr: String = GorConstants.FIRST_POSSIBLE_CHROMOSOME_VALUE
    var lastSeekChr: String = GorConstants.FIRST_POSSIBLE_CHROMOSOME_VALUE

    var joinType: String = iJoinType

    var negjoin = false

    var elimCols = 0
    if (plain) {
      if (joinType == "snpseg" || joinType == "segseg") elimCols = 1 else elimCols = 0
    }
    var nothingFromRight: Boolean = if (missingSeg == "") true else false

    var varsegleft = false
    var varsegright = false
    var lref = 3
    var rref = 3
    var caseInsensitive = false
    var ic = false
    var ir = false

    if (ph != null) {
      if (ph.varsegleft) joinType = "seg" + joinType.slice(3, 6)
      if (ph.varsegright) joinType = joinType.slice(0, 3) + "seg"
      varsegleft = ph.varsegleft
      varsegright = ph.varsegright
      lref = ph.lref
      rref = ph.rref
      if (plain && varsegright) elimCols = -1
      if (ph.negjoin) {
        nothingFromRight = true; negjoin = true
      }
      if (ph.caseInsensitive) caseInsensitive = true
      if (ph.ic) ic = true
      if (ph.ir) ir = true
    }

    var segseg: Boolean = if (joinType == "segseg") true else false
    var snpsnp: Boolean = if (joinType == "snpsnp") true else false
    var segsnp: Boolean = if (joinType == "segsnp") true else false

    var leftStart = 0
    var leftStop = 0
    var next_leftStart = 0
    val noDistance: Boolean = if (plain || fuzzFactor == 0 && joinType == "snpsnp") true else false
    var groupClean = 0
    val leq: Array[Int] = lleq.toArray
    val req: Array[Int] = lreq.toArray
    var ovlaps = 0

    case class GroupHolder() {
      val rowBuffer = new Array[myRowBufferType](2)
      rowBuffer(0) = new myRowBufferType
      rowBuffer(1) = new myRowBufferType
      var buffer = 0
      var bufferSize = 0
    }

    var singleGroupHolder = GroupHolder()
    var groupMap = new scala.collection.mutable.HashMap[String, GroupHolder]
    val useGroup: Boolean = if (lleq == Nil) false else true
    var gr: GroupHolder = _
    if (!useGroup) groupMap += ("#GR0#" -> singleGroupHolder)

    def output_row(lSeg: SEGinfo, rSeg: SEGinfo) {

      if (!negjoin && !ic) {
        val lr = lSeg.r
        val rr = rSeg.r
        if (ir) {
          super.process(rr)
        } else if (inclusOnly) {
          if (ovlaps < 1) super.process(lr)
        }
        else if (nothingFromRight) {
          super.process(lr)
        } else if (noDistance) {
          super.process(lr.joinedWithSlice(rr, 2 + elimCols, rr.numCols))
        } else {
          super.process(lr.joinedWithSliceAndAddedColumn(distSegSeg(lSeg, rSeg).toString, rr, 1, rr.numCols));
        }
      }
      ovlaps += 1
    }

    def nested_process(lr: Row, next_lr: Row) {

      if (segseg || segsnp) {
        try {
          if (varsegleft) {
            leftStart = lr.pos - 1
            leftStop = leftStart + lr.colAsString(lref).length
            if (next_lr != null) next_leftStart = next_lr.pos - 1
          } else {
            leftStart = lr.pos
            leftStop = lr.colAsInt(lstop)
            if (next_lr != null) next_leftStart = next_lr.pos
          }
        } catch {
          case e: Exception =>
            val exception = new GorDataException(s"Illegal stop position in column #${lstop + 1} in the JOIN left-source :" + lr, e)
            throw exception
        }
      } else {
        leftStart = lr.pos - 1
        leftStop = lr.pos
        if (next_lr != null) next_leftStart = next_lr.pos - 1
      }


      //#####
      // Find overlap with right-rows in the buffer

      val lSeg = SEGinfo(leftStart, leftStop, lr)
      ovlaps = 0

      var groupKeyLeft: String = null
      if (useGroup) {
        groupKeyLeft = if (caseInsensitive) lr.selectedColumns(leq).toUpperCase else lr.selectedColumns(leq)
        groupMap.get(groupKeyLeft) match {
          case Some(x) => gr = x
          case None =>
            gr = GroupHolder()
            groupMap += (groupKeyLeft -> gr)
        }
      } else {
        gr = singleGroupHolder
      }

      val nextBuffer = (gr.buffer + 1) % 2
      var nextBufferSize = 0
      var i = 0
      while (i < gr.bufferSize) {
        val rSeg = gr.rowBuffer(gr.buffer)(i)
        val rr = rSeg.r
        var use_row_again = true
        if (lr.chr == rr.chr && lSeg.start - fuzzFactor < rSeg.stop && lSeg.stop + fuzzFactor > rSeg.start) {
          output_row(lSeg, rSeg)
          if (ir) use_row_again = false
        }
        if (!((rr.chr == lr.chr && rSeg.stop + fuzzFactor < lSeg.start) || rr.chr < lr.chr) && use_row_again) {
          if (gr.rowBuffer(nextBuffer).size <= nextBufferSize) {
            gr.rowBuffer(nextBuffer) += gr.rowBuffer(gr.buffer)(i)
          } else {
            gr.rowBuffer(nextBuffer)(nextBufferSize) = gr.rowBuffer(gr.buffer)(i)
          }
          gr.rowBuffer(gr.buffer)(i) = null
          nextBufferSize += 1
        }
        i += 1
      }

      gr.buffer = nextBuffer
      gr.bufferSize = nextBufferSize


      //##########
      // Check if we need to fetch more segments from the right-source, i.e. have we moved upwards with the left-source
      if (((maxLeftStop < leftStop && lastLeftChr == lr.chr) || lastLeftChr < lr.chr) &&
        (lastRightChr < lr.chr || (lastRightChr == lr.chr && lastRightPos <= leftStop + fuzzFactor))) {
        if (lr.chr == lastSeekChr && !rightSource.hasNext) {
          /* do nothing */
        }
        else if (lr.chr > lastRightChr) {
          if (snpsnp || segsnp) {
            rightSource.setPosition(lr.chr, (lr.pos - fuzzFactor - maxSegSize).max(0))
          } else {
            rightSource.setPosition(lr.chr, (lr.pos - fuzzFactor - maxSegSize).max(0))
          }
          lastSeekChr = lr.chr
        } else if (lr.chr == lastRightChr && lr.pos - fuzzFactor - maxSegSize > lastRightPos) {
          if (snpsnp || segsnp) {
            rightSource.moveToPosition(lr.chr, (lr.pos - fuzzFactor - maxSegSize).max(0))
          } else {
            rightSource.moveToPosition(lr.chr, lr.pos - fuzzFactor - maxSegSize)
          }
          lastSeekChr = lr.chr
        }
        var keepOn = true
        while (keepOn && rightSource.hasNext) { // Read segments from the right-source
          val rr = rightSource.next()

          if (MemoryMonitorUtil.memoryMonitorActive && rightSourceMonitorUtil != null) {
            rightSourceMonitorUtil.check("JoinRightSource", rightSourceMonitorUtil.lineNum, rr)
          }

          var rightStart = 0
          var rightStop = 0

          if (snpsnp || segsnp) {
            if (varsegright) {
              rightStart = rr.pos - 1
              rightStop = rightStart + rr.colAsString(rref).length
            } else {
              rightStart = rr.pos - 1
              rightStop = rr.pos
            }
          } else {
            rightStart = rr.pos
            try {
              if (varsegright) {
                rightStart = rr.pos - 1
                rightStop = rightStart + rr.colAsString(rref).length
              } else {
                rightStop = rr.colAsInt(rstop)
              }
            } catch {
              case e: Exception =>
                val exception = new GorDataException(s"Illegal stop position in column #${rstop + 1} in the JOIN right-source : " + rr, e)
                throw exception
            }
          }

          val rSeg = SEGinfo(rightStart, rightStop, rr)

          var groupKeyRight: String = null
          if (useGroup) {
            groupKeyRight = if (caseInsensitive) rr.selectedColumns(req).toUpperCase else rr.selectedColumns(req)
            groupMap.get(groupKeyRight) match {
              case Some(x) => gr = x
              case None =>
                gr = GroupHolder()
                groupMap += (groupKeyRight -> gr)
            }
          } else {
            gr = singleGroupHolder
          }

          var use_row_again = true
          if (lr.chr == rr.chr && lSeg.start - fuzzFactor < rSeg.stop && lSeg.stop + fuzzFactor > rSeg.start && (!useGroup || groupKeyLeft == groupKeyRight)) {
            output_row(lSeg, rSeg)
            if (ir) use_row_again = false
          }

          lastRightChr = rr.chr
          lastRightPos = rightStart // rr.pos

          if (use_row_again && (next_lr != null && ((rr.chr == next_lr.chr && rightStop >= next_leftStart - fuzzFactor) || rr.chr >= next_lr.chr))) {
            // Only insert row to buffer if overlap with next row
            if (gr.rowBuffer(gr.buffer).size <= gr.bufferSize) gr.rowBuffer(gr.buffer) += rSeg else gr.rowBuffer(gr.buffer)(gr.bufferSize) = rSeg
            gr.bufferSize += 1
          }
          if (rr.chr > lr.chr || (rr.chr == lr.chr && rightStart > leftStop + fuzzFactor)) keepOn = false // Continue until there is no overlap with the left-seg
        }

      }


      //#######
      if (ovlaps == 0 && leftJoin && !inclusOnly) {
        if (nothingFromRight) {
          super.process(lr)
        } else {
          super.process(RowObj(lr.chr, lr.pos, lr.otherCols + missingSeg))
        }
      } else if (ic) {
        super.process(lr.rowWithAddedColumn(ovlaps.toString))
      }

      if ((lr.chr == lastLeftChr && maxLeftStop < leftStop) || lr.chr != lastLeftChr) maxLeftStop = leftStop
      lastLeftChr = lr.chr

      groupClean += 1
      if (groupClean == 100000 && useGroup && groupMap.size > 1) {
        groupMap.keys.foreach(k => {
          val gr = groupMap(k)
          val nextBuffer = (gr.buffer + 1) % 2
          var nextBufferSize = 0
          var i = 0
          while (i < gr.bufferSize) {
            val rSeg = gr.rowBuffer(gr.buffer)(i)
            val rr = rSeg.r
            if (!((rr.chr == lr.chr && rSeg.stop + fuzzFactor < lSeg.start) || rr.chr < lr.chr)) {
              if (gr.rowBuffer(nextBuffer).size <= nextBufferSize) {
                gr.rowBuffer(nextBuffer) += gr.rowBuffer(gr.buffer)(i)
              } else {
                gr.rowBuffer(nextBuffer)(nextBufferSize) = gr.rowBuffer(gr.buffer)(i)
              }
              gr.rowBuffer(gr.buffer)(i) = null
              nextBufferSize += 1
            }
            i += 1
          }
          gr.buffer = nextBuffer
          gr.bufferSize = nextBufferSize
          if (nextBufferSize == 0) {
            groupMap.remove(k)
          }
        })
        groupClean = 0
      }
    }

    var prev_row: Row = _

    override def process(lr: Row) {
      if (prev_row == null) {
        prev_row = lr
      } else {
        nested_process(prev_row, lr)
        prev_row = lr
      }
    }

    override def finish() {
      try {
        if (prev_row != null) {
          nested_process(prev_row, null)
        }
      } finally {
        rightSource.close()
      }
    }
  }

  case class SegJoinSegOverlap(ph: ParameterHolder, rightSource: RowSource, missingB: String, leftJoin: Boolean, fuzz: Int,
                               lstop: Int, rstop: Int, leq: List[Int], req: List[Int], maxSegSize: Int, plain: Boolean) extends Analysis {
    this | SegOverlap(ph, rightSource, missingB, leftJoin, fuzz, "segseg", lstop, rstop, leq, req, maxSegSize, plain)
  }

  case class SegJoinSegOverlapInclusOnly(ph: ParameterHolder, rightSource: RowSource, missingB: String, leftJoin: Boolean, fuzz: Int,
                                         lstop: Int, rstop: Int, leq: List[Int], req: List[Int], maxSegSize: Int) extends Analysis {
    this | SegOverlap(ph, rightSource, missingB, leftJoin, fuzz, "segseg", lstop, rstop, leq, req, maxSegSize, plain = false, inclusOnly = true)
  }

  case class SegJoinSnpOverlap(ph: ParameterHolder, rightSource: RowSource, missingB: String, leftJoin: Boolean, fuzz: Int,
                               lstop: Int, leq: List[Int], req: List[Int], maxSegSize: Int, plain: Boolean) extends Analysis {
    this | SegOverlap(ph, rightSource, missingB, leftJoin, fuzz, "segsnp", lstop, 2, leq, req, maxSegSize, plain)
  }

  case class SegJoinSnpOverlapInclusOnly(ph: ParameterHolder, rightSource: RowSource, missingB: String, leftJoin: Boolean, fuzz: Int,
                                         lstop: Int, leq: List[Int], req: List[Int], maxSegSize: Int) extends Analysis {
    this | SegOverlap(ph, rightSource, missingB, leftJoin, fuzz, "segsnp", lstop, 2, leq, req, maxSegSize, plain = false, inclusOnly = true)
  }

  case class SnpJoinSegOverlap(ph: ParameterHolder, rightSource: RowSource, missingB: String, leftJoin: Boolean, fuzz: Int,
                               rstop: Int, leq: List[Int], req: List[Int], maxSegSize: Int, plain: Boolean) extends Analysis {
    this | SegOverlap(ph, rightSource, missingB, leftJoin, fuzz, "snpseg", 2, rstop, leq, req, maxSegSize, plain)
  }

  case class SnpJoinSegOverlapInclusOnly(ph: ParameterHolder, rightSource: RowSource, missingB: String, leftJoin: Boolean, fuzz: Int,
                                         rstop: Int, leq: List[Int], req: List[Int], maxSegSize: Int) extends Analysis {
    this | SegOverlap(ph, rightSource, missingB, leftJoin, fuzz, "snpseg", 2, rstop, leq, req, maxSegSize, plain = false, inclusOnly = true)
  }

  case class SnpJoinSnpOverlap(ph: ParameterHolder, rightSource: RowSource, missingB: String, leftJoin: Boolean, fuzz: Int,
                               leq: List[Int], req: List[Int], maxSegSize: Int, plain: Boolean) extends Analysis {
    this | SegOverlap(ph, rightSource, missingB, leftJoin, fuzz, "snpsnp", 2, 2, leq, req, maxSegSize, plain)
  }

  case class SnpJoinSnpOverlapInclusOnly(ph: ParameterHolder, rightSource: RowSource, missingB: String, leftJoin: Boolean, fuzz: Int,
                                         leq: List[Int], req: List[Int], maxSegSize: Int) extends Analysis {
    this | SegOverlap(ph, rightSource, missingB, leftJoin, fuzz, "snpsnp", 2, 2, leq, req, maxSegSize, plain = false, inclusOnly = true)
  }

  def parseArguments(context: GorContext, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String = ""): CommandParsingResult = {

    var usedFiles = ListBuffer.empty[String]

    if (!(hasOption(args, "-snpsnp") || hasOption(args, "-snpseg") || hasOption(args, "-segseg") || hasOption(args, "-segsnp") || hasOption(args, "-varseg") || hasOption(args, "-segvar"))) {
      throw new GorParsingException("Specify the type of join, e.g. -snpsnp, -snpseg, -segseg, -segsnp, -varseg, or -segvar.")
    }

    if ((iargs.length > 3 && !hasOption(args, "-stdin")) || (iargs.length > 2 && hasOption(args, "-stdin"))) {
      logger.debug("iargs " + iargs.toList)
      throw new GorParsingException(" Too many files specified for JOIN")
    }

    var joinType = "segseg"
    if (hasOption(args, "-snpsnp")) joinType = "snpsnp"
    if (hasOption(args, "-snpseg")) joinType = "snpseg"
    if (hasOption(args, "-segsnp")) joinType = "segsnp"
    if (hasOption(args, "-segseg")) joinType = "segseg"

    val gorRoot = context.getSession.getProjectContext.getRoot

    val prefix = stringValueOfOptionWithDefault(args, "-rprefix","")

    var maxSegSize = if (hasOption(args, "-snpsnp") || hasOption(args, "-segsnp") || hasOption(args, "-varsnp")) {
      1
    } else if (hasOption(args, "-segvar")) {
      300
    }  else {
      3000000
    } // The range of the largest gene

    if (hasOption(args, "-maxseg")) maxSegSize = intValueOfOption(args, "-maxseg")

    var fuzzFactor = 0
    if (hasOption(args, "-f")) fuzzFactor = intValueOfOptionWithRangeCheck(args, "-f", 0)


    var startChr = GorConstants.FIRST_POSSIBLE_CHROMOSOME_VALUE
    var stopChr = GorConstants.LAST_POSSIBLE_CHROMOSOME_VALUE
    var startPos = 0
    var stopPos = 250000000
    var rangeSpecified = false

    if (hasOption(args, "-p")) {
      val range = stringValueOfOption(args, "-p").replace(",", "").replace(" ", "")
      val rcol = range.split("[:|-]")
      if (rcol.nonEmpty) {
        startChr = rcol(0)
        stopChr = startChr
      }
      if (rcol.length > 1) {
        startPos = rcol(1).toInt - maxSegSize - fuzzFactor
        if (!range.endsWith("-")) stopPos = parseIntWithRangeCheck("right column 1", rcol(1)) + maxSegSize + fuzzFactor
      }
      if (rcol.length > 2) stopPos = parseIntWithRangeCheck("right column 2", rcol(2)) + fuzzFactor
      if (startPos < 0) startPos = 0
      rangeSpecified = true
    }

    var emptyString = ""
    if (hasOption(args, "-e")) emptyString = replaceSingleQuotes(stringValueOfOption(args, "-e"))

    val inclusOnly = hasOption(args, "-i")

    if (inclusOnly && (hasOption(args, "-l") || hasOption(args, "-n") || hasOption(args, "-t") || hasOption(args, "-o") || hasOption(args, "-m"))) {
      throw new GorParsingException("The -i option is NOT compatible with any of the following options: -l,-m,-n,-o,-t", "-i")
    }
    if (hasOption(args, "-n") && (hasOption(args, "-l") || hasOption(args, "-i") || hasOption(args, "-t") || hasOption(args, "-o") || hasOption(args, "-m"))) {
      throw new GorParsingException("The -n option is NOT compatible with any of the following options: -l,-m,-i,-o,-t", "-n")
    }
    if (hasOption(args, "-ic") && (hasOption(args, "-l") || hasOption(args, "-i") || hasOption(args, "-t") || hasOption(args, "-o") || hasOption(args, "-m") || hasOption(args, "-n") || hasOption(args, "-ir"))) {
      throw new GorParsingException("The -ic option is NOT compatible with any of the following options: -l,-m,-n,-i,-o,-t, -ir", "-ic")
    }
    if (hasOption(args, "-ir") && (hasOption(args, "-l") || hasOption(args, "-i") || hasOption(args, "-t") || hasOption(args, "-o") || hasOption(args, "-m") || hasOption(args, "-n") || hasOption(args, "-ic"))) {
      throw new GorParsingException("The -ir option is NOT compatible with any of the following options: -l,-m,-n,-i,-o,-t, -ic", "-ir")
    }

    var doLeftJoin = hasOption(args, "-l")
    var rightFile: String = null
    var stdInput: RowSource = null
    var segSource: RowSource = null
    var isSourceSet = false
    var rightHeader = ""
    val leftHeader = forcedInputHeader

    rightFile = iargs(0).trim

    try {
      val inputSource = SourceProvider(rightFile, context, executeNor = executeNor, isNor = false)
      segSource = inputSource.source
      usedFiles = ListBuffer.empty[String] ++ inputSource.usedFiles
      rightHeader = inputSource.header
      isSourceSet = inputSource.dynSource.ne(null)
    } catch {
      case e: Exception =>
        if (segSource != null) segSource.close()
        throw e
    }

    if (hasOption(args, "-s")) rightFile += " -s " + stringValueOfOption(args, "-s")

    var leq: List[Int] = Nil
    var req: List[Int] = Nil
    var useHash = false

    if (hasOption(args, "-xl")) {
      leq = columnsOfOption(args, "-xl", leftHeader)
      useHash = true
    }


    if (hasOption(args, "-varseg") && !(hasOption(args, "-refl") || hasOption(args, "-ref")) && leftHeader.split("\t", -1).map(_.toUpperCase).count(x => x == "REF" || x == "REFERENCE") > 1) {
      throw new GorParsingException("Ambiguous reference columns in left-source.  Use -ref or -refl.\nHeader: " + leftHeader, "-varseg")
    }
    if (hasOption(args, "-segvar") && !(hasOption(args, "-refr") || hasOption(args, "-ref")) && rightHeader.split("\t", -1).map(_.toUpperCase).count(x => x == "REF" || x == "REFERENCE") > 1) {
      throw new GorParsingException("Ambiguous reference columns in right-source.  Use -ref or -refr.\nHeader: " + leftHeader, "-segvar")
    }

    var lRef = leftHeader.split("\t", -1).indexWhere(x => x.toUpperCase == "REF")
    if (lRef < 0) lRef = leftHeader.split("\t", -1).indexWhere(x => x.toUpperCase == "REFERENCE")
    if (lRef < 0) lRef = 2

    var rRef = rightHeader.split("\t", -1).indexWhere(x => x.toUpperCase == "REF")
    if (rRef < 0) rRef = rightHeader.split("\t", -1).indexWhere(x => x.toUpperCase == "REFERENCE")
    if (rRef < 0) rRef = 2

    var lrRef = -1

    if (hasOption(args, "-ref") &&
      hasOption(args, "-varseg")) {
      lrRef = columnOfOption(args, "-ref", leftHeader, executeNor)
    }

    if (lrRef != -1) {
      lRef = lrRef
    }

    if (hasOption(args, "-ref") && hasOption(args, "-segvar")) lrRef = columnOfOption(args, "-ref", rightHeader, executeNor)

    if (lrRef != -1) {
      rRef = lrRef
    }

    if (hasOption(args, "-refl")) lRef = columnOfOption(args, "-refl", leftHeader, executeNor)
    if (hasOption(args, "-refr")) rRef = columnOfOption(args, "-refr", rightHeader, executeNor)

    var lstop = 2 // Zero-based for #3
    var rstop = 2
    if (hasOption(args, "-lstop")) lstop = columnOfOption(args, "-lstop", leftHeader, executeNor)
    if (hasOption(args, "-rstop")) rstop = columnOfOption(args, "-rstop", rightHeader, executeNor)


    val ph = ParameterHolder(hasOption(args, "-varseg"), hasOption(args, "-segvar"), lRef, rRef, hasOption(args, "-n"), hasOption(args, "-xcis"), hasOption(args, "-ic"), hasOption(args, "-ir"))

    try {
      if (!isSourceSet && !hasOption(args, "-maxseg") && !(hasOption(args, "-snpsnp") || hasOption(args, "-segsnp") || hasOption(args, "-segvar"))) { // check the size of segments if maxseg is not specified
        maxSegSize = getFileMaxSegSize(rightFile, lstop, context)
      }



      val useToList = hasOption(args, "-t")
      val useCount = hasOption(args, "-c") && useToList
      var onlyMin = hasOption(args, "-m") && fuzzFactor > 0
      var nClos = 1

      if (hasOption(args, "-o")) {
        onlyMin = true
        nClos = intValueOfOptionWithRangeCheck(args, "-o", 0)
      }

      if (hasOption(args, "-xr")) {
        req = columnsOfOption(args, "-xr", rightHeader)
        useHash = true
      }

      if (leq.size * req.size == 0 && leq.size + req.size != 0) {
        throw new GorParsingException("For equi-join, you MUST specify both -xl and -xr, NOT just one of them.")
      }

      rightHeader = rightHeader.split("\t", -1).map(x => if (prefix != "") prefix + "_" + x else x).mkString("\t")

      if (inclusOnly || hasOption(args, "-n")) rightHeader = ""

      val rightColsA = rightHeader.split("\t"); // rightColsA(1) = "Pos2"
      val rightCols = rightColsA.toList
      val groupCols = leftHeader.split("\t").length - 2


      var plain = false

      if (hasOption(args, "-r") && !hasOption(args, "-i")) plain = true
      var rightColStart = 1
      if (plain && !hasOption(args, "-segvar")) {
        if (joinType == "snpseg" || joinType == "segseg") rightColStart += 2 else rightColStart += 1
      }

      var combinedHeader = ""

      if (fuzzFactor > 0 || joinType == "snpseg" || joinType == "segseg" || joinType == "segsnp") {
        combinedHeader = rightCols.slice(rightColStart, rightCols.length).foldLeft(leftHeader + (if (useCount) "\toverlapCount" else "") + (if (!plain) "\tdistance" else "")) (_ + "\t" + _)
      } else {
        combinedHeader = rightCols.slice(2, rightCols.length).foldLeft(leftHeader + (if (useCount) "\toverlapCount" else "")) (_ + "\t" + _)
      }


      if (inclusOnly) combinedHeader = leftHeader

      var missingSEG = Range(0, combinedHeader.split("\t").length - leftHeader.split("\t").length).toList.map(x => emptyString).mkString("\t")

      if (leftHeader.split("\t").length != 2) missingSEG = "\t" + missingSEG

      if (hasOption(args, "-ic")) combinedHeader = leftHeader + "\tOverlapCount"
      if (hasOption(args, "-ir")) combinedHeader = rightHeader

      combinedHeader = IteratorUtilities.validHeader(combinedHeader)

      var aPipeStep: Analysis = null // Unshielded pipe step

      if (hasOption(args, "-n")) { // non-overlap

        val missPatt = "\tgorjoin#keep"
        combinedHeader = leftHeader
        doLeftJoin = true

        if (joinType == "segseg") {
          aPipeStep = SegJoinSegOverlap(ph, segSource, missPatt, doLeftJoin, fuzzFactor, lstop, rstop, leq, req, maxSegSize, plain) // | NegFilter(noCols, missPatt)
        } else if (joinType == "snpseg") {
          aPipeStep = SnpJoinSegOverlap(ph, segSource, missPatt, doLeftJoin, fuzzFactor, rstop, leq, req, maxSegSize, plain) // | NegFilter(noCols, missPatt)
        } else if (joinType == "segsnp") {
          aPipeStep = SegJoinSnpOverlap(ph, segSource, missPatt, doLeftJoin, fuzzFactor, lstop, leq, req, maxSegSize, plain) // | NegFilter(noCols, missPatt)
        } else if (joinType == "snpsnp") {
          aPipeStep = SnpJoinSnpOverlap(ph, segSource, missPatt, doLeftJoin, fuzzFactor, leq, req, maxSegSize, plain) // | NegFilter(noCols, missPatt)
        } // end non-overlap
      } else { // overlap

        if (joinType == "segseg") {
          if (inclusOnly) {
            aPipeStep = SegJoinSegOverlapInclusOnly(ph, segSource, missingSEG, doLeftJoin, fuzzFactor, lstop, rstop, leq, req, maxSegSize)
          } else if (onlyMin) {
            if (useToList) {
              aPipeStep = SegJoinSegOverlap(ph, segSource, missingSEG, doLeftJoin, fuzzFactor, lstop, rstop, leq, req, maxSegSize, plain) | NClosest(groupCols, nClos) | ToList(groupCols, useCount)
            } else {
              aPipeStep = SegJoinSegOverlap(ph, segSource, missingSEG, doLeftJoin, fuzzFactor, lstop, rstop, leq, req, maxSegSize, plain) | NClosest(groupCols, nClos)
            }
          } else {
            if (useToList) {
              aPipeStep = SegJoinSegOverlap(ph, segSource, missingSEG, doLeftJoin, fuzzFactor, lstop, rstop, leq, req, maxSegSize, plain) | Analysis.ToList(groupCols, useCount)
            }
            else {
              aPipeStep = SegJoinSegOverlap(ph, segSource, missingSEG, doLeftJoin, fuzzFactor, lstop, rstop, leq, req, maxSegSize, plain)
            }
          }
        } else if (joinType == "segsnp") {
          if (inclusOnly) {
            aPipeStep = SegJoinSnpOverlapInclusOnly(ph, segSource, missingSEG, doLeftJoin, fuzzFactor, lstop, leq, req, maxSegSize)
          } else if (onlyMin) {
            if (useToList) {
              aPipeStep = SegJoinSnpOverlap(ph, segSource, missingSEG, doLeftJoin, fuzzFactor, lstop, leq, req, maxSegSize, plain) | NClosest(groupCols, nClos) | Analysis.ToList(groupCols, useCount)
            }
            else {
              aPipeStep = SegJoinSnpOverlap(ph, segSource, missingSEG, doLeftJoin, fuzzFactor, lstop, leq, req, maxSegSize, plain) | NClosest(groupCols, nClos)
            }
          } else {
            if (useToList) {
              aPipeStep = SegJoinSnpOverlap(ph, segSource, missingSEG, doLeftJoin, fuzzFactor, lstop, leq, req, maxSegSize, plain) | Analysis.ToList(groupCols, useCount)
            } else {
              aPipeStep = SegJoinSnpOverlap(ph, segSource, missingSEG, doLeftJoin, fuzzFactor, lstop, leq, req, maxSegSize, plain)
            }
          }

        } else if (joinType == "snpseg") {
          if (inclusOnly) {
            aPipeStep = SnpJoinSegOverlapInclusOnly(ph, segSource, missingSEG, doLeftJoin, fuzzFactor, rstop, leq, req, maxSegSize)
          } else if (onlyMin) {
            if (useToList) {
              aPipeStep = SnpJoinSegOverlap(ph, segSource, missingSEG, doLeftJoin, fuzzFactor, rstop, leq, req, maxSegSize, plain) | NClosest(groupCols, nClos) | Analysis.ToList(groupCols, useCount)
            } else {
              aPipeStep = SnpJoinSegOverlap(ph, segSource, missingSEG, doLeftJoin, fuzzFactor, rstop, leq, req, maxSegSize, plain) | NClosest(groupCols, nClos)
            }
          } else {
            if (useToList) {
              aPipeStep = SnpJoinSegOverlap(ph, segSource, missingSEG, doLeftJoin, fuzzFactor, rstop, leq, req, maxSegSize, plain) | Analysis.ToList(groupCols, useCount)
            } else {
              aPipeStep = SnpJoinSegOverlap(ph, segSource, missingSEG, doLeftJoin, fuzzFactor, rstop, leq, req, maxSegSize, plain)
            }
          }

        } else if (joinType == "snpsnp") {
          if (inclusOnly) {
            aPipeStep = SnpJoinSnpOverlapInclusOnly(ph, segSource, missingSEG, doLeftJoin, fuzzFactor, leq, req, maxSegSize)
          } else if (onlyMin) {
            if (useToList) {
              aPipeStep = SnpJoinSnpOverlap(ph, segSource, missingSEG, doLeftJoin, fuzzFactor, leq, req, maxSegSize, plain) | NClosest(groupCols, nClos) | Analysis.ToList(groupCols, useCount)
            } else {
              aPipeStep = SnpJoinSnpOverlap(ph, segSource, missingSEG, doLeftJoin, fuzzFactor, leq, req, maxSegSize, plain) | NClosest(groupCols, nClos)
            }
          } else {
            if (useToList) {
              aPipeStep = SnpJoinSnpOverlap(ph, segSource, missingSEG, doLeftJoin, fuzzFactor, leq, req, maxSegSize, plain) | Analysis.ToList(groupCols, useCount)
            } else {
              aPipeStep = SnpJoinSnpOverlap(ph, segSource, missingSEG, doLeftJoin, fuzzFactor, leq, req, maxSegSize, plain)
            }
          }
        }
      } // end overlap

      val pipeStep = InRange(startChr, startPos, stopChr, stopPos) | aPipeStep

      CommandParsingResult(pipeStep, combinedHeader, usedFiles.toArray)
    } catch {
      case e: Exception =>
        if (stdInput != null) stdInput.close()
        if (segSource != null) segSource.close()
        throw e
    }
  } // end processArguments

  def getFileMaxSegSize(extFilename: String, lStop: Int, context: GorContext): Int = synchronized {
    Option(context.getSession.getCache.getFileSegMap.get(extFilename)) match {
      case Some(theSize) => return theSize
      case None =>
        var maxSegSize = 0
        val segSource = new SingleFileSource(extFilename, context.getSession.getProjectContext.getRoot, context)
        try {
          var counter = 0
          while (segSource.hasNext && counter < 10000000) {
            val r = segSource.next()
            val segSize = r.colAsInt(lStop) - r.pos
            maxSegSize = maxSegSize.max(segSize)
            counter += 1
          }
        } catch {
          case e: Exception =>
            throw new RuntimeException("Error in estimating the segment size in file: " + extFilename, e)
        }
        finally {
          segSource.close()
        }
        context.getSession.getCache.getFileSegMap.put(extFilename, maxSegSize)
        return maxSegSize
    }
  }
}