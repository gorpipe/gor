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

package gorsat.Commands

import gorsat.Analysis.SortAnalysis
import gorsat.Analysis.VarJoinAnalysis.{ParameterHolder, SegVarJoinSegOverlap, SegVarJoinSegOverlapInclusOnly}
import gorsat.Commands.CommandParseUtilities._
import gorsat.Iterators.{EatStdInputSource, StdInputSourceIterator}
import gorsat.Utilities.IteratorUtilities
import gorsat.process.SourceProvider
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.GorConstants
import org.gorpipe.gor.session.GorContext
import org.gorpipe.model.gor.iterators.RowSource

import scala.collection.mutable.ListBuffer

class VarJoin extends CommandInfo("VARJOIN",
  CommandArguments("-l -i -ir -ic -n -stdin -r -xcis -norm -nonorm",
    "-p -s -e -xl -xr -maxseg -rprefix -ref -alt -refl -altl -refr -altr -as -span", 1, 1),
  CommandOptions(gorCommand = true, verifyCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {
    var usedFiles = ListBuffer.empty[String]
    var exactJoin = false
    val varjoinType = if (context.getSession.getProjectContext.getVarJoinType == "undefined") System.getProperty("gor.varjointype", "nonorm") else context.getSession.getProjectContext.getVarJoinType
    if (varjoinType.toLowerCase.contains("nonorm")) exactJoin = false
    if (hasOption(args,"-norm") || varjoinType.toLowerCase == "norm") exactJoin = true
    if (hasOption(args,"-nonorm")) exactJoin = false


    val prefix = stringValueOfOptionWithDefault(args,"-rprefix","")

    // The range of the largest variant
    val maxSegSize = intValueOfOptionWithDefaultWithRangeCheck(args,"-maxseg", 1000, 0)

    // Such that zero-length refseqs are compared.
    var fuzzFactor = intValueOfOptionWithDefaultWithRangeCheck(args,"-span", 100, 0)
    if (fuzzFactor > 1000000) { throw new GorParsingException("Span cannot exceed 1Mb!  This leads to slow execution and heavy memory usage.") }
    if (exactJoin) fuzzFactor = 0


    var startChr = GorConstants.FIRST_POSSIBLE_CHROMOSOME_VALUE
    var stopChr = GorConstants.LAST_POSSIBLE_CHROMOSOME_VALUE
    var startPos = 0
    var stopPos = 250000000
    var rangeSpecified = false


    if (hasOption(args,"-p")) {
      val range = stringValueOfOption(args,"-p").replace(",","").replace(" ","")
      val rcol = range.split("[:|-]")
      if (rcol.nonEmpty) { startChr = rcol(0); stopChr = startChr }
      if (rcol.length>1) { startPos = rcol(1).toInt-maxSegSize-fuzzFactor; if (!range.endsWith("-")) stopPos = rcol(1).toInt+maxSegSize+fuzzFactor }
      if (rcol.length>2) stopPos = rcol(2).toInt+fuzzFactor
      if (startPos<0) startPos = 0
      rangeSpecified = true
    }

    val emptyString = replaceSingleQuotes(stringValueOfOptionWithDefault(args,"-e",""))

    val inclusOnly = hasOption(args,"-i")
    var doLeftJoin = hasOption(args,"-l")

    var rightFile : String = null
    var stdInput : RowSource = null
    var segSource : RowSource = null
    var rightColStart = 1

    var aPipeStep : Analysis = null // Unshielded pipe step
    val leftHeader = forcedInputHeader

    val leq = columnsOfOptionWithNil(args, "-xl", leftHeader, executeNor)

    var rightHeader = ""

    val ph = ParameterHolder(hasOption(args, "-ic"), hasOption(args, "-ir"))

    try {

      rightFile = iargs(0).trim
      if (hasOption(args,"-s")) rightFile += " -s " + stringValueOfOption(args,"-s")

      val inputSource = SourceProvider(rightFile, context, executeNor = executeNor, isNor = false)
      segSource = inputSource.source
      usedFiles = ListBuffer.empty[String] ++ inputSource.usedFiles
      rightHeader = inputSource.header

      if (rangeSpecified) {
        stdInput = new EatStdInputSource(startChr, startPos, stopChr, stopPos, maxSegSize + fuzzFactor, "")
      } else {
        stdInput = StdInputSourceIterator("")
      }

      val req = columnsOfOptionWithNil(args, "-xr", rightHeader, executeNor)

      if (leq.size * req.size == 0 && leq.size + req.size != 0) {
        throw new GorParsingException("Error in equi-join. You must specify both -xl and -xr, not just one of them.")
      }

      if (leftHeader.split("\t",-1).length < 4) {
        throw new GorParsingException(s"Error in left-source. Insufficient number of columns in the left-source to denote (chr,pos,ref,alt): $leftHeader")
      }
      if (rightHeader.split("\t",-1).length < 4) {
        throw new GorParsingException(s"Error in right-source. Insufficient number of columns in the right-source to denote (chr,pos,ref,alt): $rightHeader")
      }
      if ( !(hasOption(args,"-refl") || hasOption(args,"-ref")) && leftHeader.split("\t", -1).map(_.toUpperCase).count(x => x == "REF" || x == "REFERENCE") > 1) {
        throw new GorParsingException(s"Error in left-source - ambiguous reference columns in left-source. Use -ref or -refl.\nHeader: $leftHeader")
      }
      if (!(hasOption(args,"-refr") || hasOption(args,"-ref")) && rightHeader.split("\t", -1).map(_.toUpperCase).count(x => x == "REF" || x == "REFERENCE") > 1) {
        throw new GorParsingException(s"Error in right-source - ambiguous reference columns in right-source.  Use -ref or -refr.\nHeader: $leftHeader")
      }
      if (!(hasOption(args,"-altl") || hasOption(args,"-alt")) && leftHeader.split("\t", -1).map(_.toUpperCase).count(x => x == "ALT" || x == "CALL" || x == "ALLELE") > 1) {
        throw new GorParsingException(s"Error in left-source - ambiguous alt/call/allele columns in left-source.  Use -alt or -altl.\nHeader: $leftHeader")
      }
      if (!(hasOption(args,"-altr") || hasOption(args,"-alt")) && rightHeader.split("\t", -1).map(_.toUpperCase).count(x => x == "ALT" || x == "CALL" || x == "ALLELE") > 1) {
        throw new GorParsingException(s"Error in right-source - ambiguous alt/call/allele columns in right-source.  Use -alt or -altr.\nHeader: $leftHeader")
      }
      if (hasOption(args, "-ic") && (hasOption(args, "-l") || hasOption(args, "-n") || hasOption(args, "-i") || hasOption(args, "-ir"))) {
        throw new GorParsingException("Error in -ic option - this option is NOT compatible with any of the following options: -l,-n,-i, -ir", "-ic")
      }
      if (hasOption(args, "-ir") && (hasOption(args, "-l") || hasOption(args, "-n") || hasOption(args, "-i") || hasOption(args, "-ic"))) {
        throw new GorParsingException("Error in -ir option - this option is NOT compatible with any of the following options: -l,-n,-i, -ic", "-ir")
      }


      var lRef = leftHeader.split("\t",-1).indexWhere( x => x.toUpperCase == "REF" )
      if (lRef<0) lRef = leftHeader.split("\t",-1).indexWhere( x => x.toUpperCase == "REFERENCE" )
      if (lRef<0) lRef = 2

      var lAlt = leftHeader.split("\t",-1).indexWhere( x => x.toUpperCase == "ALT" )
      if (lAlt<0) lAlt = leftHeader.split("\t",-1).indexWhere( x => x.toUpperCase == "CALL" )
      if (lAlt<0) lAlt = leftHeader.split("\t",-1).indexWhere( x => x.toUpperCase == "ALLELE" )
      if (lAlt<0) lAlt = 3

      var rRef = rightHeader.split("\t",-1).indexWhere( x => x.toUpperCase == "REF" )
      if (rRef<0) rRef = rightHeader.split("\t",-1).indexWhere( x => x.toUpperCase == "REFERENCE" )
      if (rRef<0) rRef = 2

      var rAlt = rightHeader.split("\t",-1).indexWhere( x => x.toUpperCase == "ALT" )
      if (rAlt<0) rAlt = rightHeader.split("\t",-1).indexWhere( x => x.toUpperCase == "CALL" )
      if (rAlt<0) rAlt = rightHeader.split("\t",-1).indexWhere( x => x.toUpperCase == "ALLELE" )
      if (rAlt<0) rAlt = 3

      var lrRef = -1
      var lrAlt = -1

      if (hasOption(args,"-ref")) lrRef = columnOfOption(args, "-ref", leftHeader, executeNor)
      if (hasOption(args,"-alt")) lrAlt = columnOfOption(args,"-alt",leftHeader, executeNor)

      if (lrRef != -1) { lRef = lrRef }
      if (lrAlt != -1) { lAlt = lrAlt }

      if (hasOption(args,"-ref")) lrRef = columnOfOption(args,"-ref",rightHeader, executeNor)
      if (hasOption(args,"-alt")) lrAlt = columnOfOption(args,"-alt",rightHeader, executeNor)

      if (lrRef != -1) { rRef = lrRef }
      if (lrAlt != -1) { rAlt = lrAlt }

      if (hasOption(args,"-refl")) lRef = columnOfOption(args,"-refl",leftHeader, executeNor)
      if (hasOption(args,"-altl")) lAlt = columnOfOption(args,"-altl",leftHeader, executeNor)
      if (hasOption(args,"-refr")) rRef = columnOfOption(args,"-refr",rightHeader, executeNor)
      if (hasOption(args,"-altr")) rAlt = columnOfOption(args,"-altr",rightHeader, executeNor)

      var allShare = -1
      if (hasOption(args,"-as")) allShare = intValueOfOptionWithRangeCheck(args,"-as", 0)

      rightHeader = rightHeader.split("\t",-1).map(x => if (prefix != "") prefix+"_"+x else x).mkString("\t")

      if (inclusOnly || hasOption(args,"-n")) rightHeader = ""


      val rightColsA = rightHeader.split("\t"); // rightColsA(1) = "Pos2"
      var rightCols = rightColsA.toList
      var missingSEG = Range(0,rightCols.length-1).toList.map(x => emptyString).mkString("\t")


      var plainCols : Array[Int] = null
      if (hasOption(args,"-r")) {
        plainCols = rightCols.zipWithIndex.collect{ case si:(String,Int) if si._2 > 1 && si._2 != rRef && si._2 != rAlt => si._2 }.toArray
        rightCols = rightCols.zipWithIndex.collect{ case si:(String,Int) if si._2 > 1 && si._2 != rRef && si._2 != rAlt => si._1 }
        rightColStart = 0
        missingSEG = rightCols.indices.toList.map(x => emptyString).mkString("\t")
      }

      var combinedHeader = rightCols.slice(rightColStart,rightCols.length).foldLeft(leftHeader) ( _ + "\t" + _ )

      if (inclusOnly) combinedHeader = leftHeader else combinedHeader = IteratorUtilities.validHeader(combinedHeader)

      if (hasOption(args, "-ic")) combinedHeader = IteratorUtilities.validHeader(leftHeader+"\tOverlapCount")
      if (hasOption(args, "-ir")) combinedHeader = rightHeader


      if (combinedHeader != leftHeader) missingSEG = "\t"+missingSEG

      val caseInsensitive = hasOption(args,"-xcis")

      var negjoin = false

      if (hasOption(args,"-n")) { // non-overlap
        negjoin = true

        val missPatt = "\tgorjoin#keep"
        combinedHeader = leftHeader
        doLeftJoin = true

        aPipeStep = SegVarJoinSegOverlap(context.getSession, ph,segSource, missPatt, exactJoin, doLeftJoin, fuzzFactor, leq, req, caseInsensitive, maxSegSize,
          lRef, lAlt, rRef, rAlt, allShare, null, negjoin)  // | NegFilter(noCols, missPatt)

      } // end non-overlap
      else { // overlap

        if (inclusOnly) {
          aPipeStep = SegVarJoinSegOverlapInclusOnly(context.getSession, ph, segSource, missingSEG, exactJoin, doLeftJoin, fuzzFactor, leq, req, caseInsensitive, maxSegSize,
            lRef, lAlt, rRef, rAlt, allShare, null, negjoin)
        } else {
          aPipeStep = SegVarJoinSegOverlap(context.getSession, ph,segSource, missingSEG, exactJoin, doLeftJoin, fuzzFactor, leq, req, caseInsensitive, maxSegSize,
            lRef, lAlt, rRef, rAlt, allShare, plainCols, negjoin)
        }

        if (hasOption(args, "-ir") && !exactJoin) {
          aPipeStep = aPipeStep  | SortAnalysis(combinedHeader,context.getSession,1000)
        }
      } // end overlap

      CommandParsingResult(aPipeStep, combinedHeader, usedFiles.toArray)
    } catch {
      case e : Exception =>
        if (stdInput != null) stdInput.close()
        if (segSource != null) segSource.close()
        throw e
    }

  }
}
