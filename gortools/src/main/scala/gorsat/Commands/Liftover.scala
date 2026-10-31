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

import gorsat.Analysis.SortGenome
import gorsat.Commands.CommandParseUtilities._
import gorsat.Utilities.IteratorUtilities.validHeader
import gorsat.liftover.{FileChainSource, LiftoverArgs, LiftoverRelocateAnalysis, OverlapPolicy, RelocateColumns, RelocateMode}
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.session.GorContext

import java.util.Locale

/**
 * LIFTOVER: moves rows to their lifted location on the target assembly and sorts the result. Every mode (-snp, -var,
 * -seg, -bam, -all) and nested-query chains run on the same engine as LIFTOVER_ANNOTATE (ENGKNOW-4117, N17).
 */
class Liftover extends CommandInfo("LIFTOVER",
  CommandArguments("-snp -seg -var -bam -all", "-ref -alt -build", 1, 1),
  CommandOptions(gorCommand = true, verifyCommand = true, cancelCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {

    val modes = List("-snp" -> RelocateMode.SNP, "-seg" -> RelocateMode.SEG, "-var" -> RelocateMode.VAR,
      "-bam" -> RelocateMode.BAM).filter(m => hasOption(args, m._1))
    if (modes.length > 1) throw new GorParsingException("Error in liftover type - specify only one liftover type (-snp, -seg, or -var): ")
    val mode = modes.headOption.map(_._2).getOrElse(RelocateMode.SEG)

    val inCols = validHeader(forcedInputHeader).split("\t", -1)
    if (inCols.length < 2) {
      throw new GorParsingException(s"Number of columns for left header needs to be at least 2.\nLeft header: $forcedInputHeader")
    }
    if (mode.isSpan && inCols.length < 3) {
      throw new GorParsingException(s"LIFTOVER ${modes.headOption.map(_._1).getOrElse("-seg")} requires an end column (#3), the input has none")
    }

    // REF and ALT by name or option, else #3 and #4 (resolved in every mode, as before P4, so a bad -ref/-alt fails)
    val ref = Some(LiftoverArgs.refColumn(args, forcedInputHeader, executeNor)).filter(_ >= 0).getOrElse(2)
    val alt = Some(LiftoverArgs.altColumn(args, forcedInputHeader, executeNor)).filter(_ >= 0).getOrElse(3)

    def bamColumn(name: String, required: Boolean): Int = {
      val i = inCols.indexWhere(_.trim.toUpperCase(Locale.ROOT) == name)
      if (i < 0 && required) throw new GorParsingException(s"-bam requires a $name column")
      i
    }

    val cols = mode match {
      case RelocateMode.VAR =>
        if (ref >= inCols.length) throw new GorParsingException("-var requires a REF column (REF/REFERENCE, -ref or #3)")
        if (alt >= inCols.length) throw new GorParsingException("-var requires an ALT column (ALT/CALL/ALLELE, -alt or #4)")
        new RelocateColumns(ref, alt, -1, -1, -1, -1, -1)
      case RelocateMode.BAM =>
        new RelocateColumns(-1, -1, bamColumn("SEQ", required = true), bamColumn("QUAL", required = true),
          bamColumn("CIGAR", required = true), bamColumn("MPOS", required = true), bamColumn("MRNM", required = false))
      case _ => RelocateColumns.NONE
    }

    val build = stringValueOfOptionWithDefault(args, "-build", "hgold")
    val all = hasOption(args, "-all")

    // output: target location, the input columns after the location, source location, strand, status[, score]
    val loc = if (mode.isSpan) 3 else 2
    val locNames = if (mode == RelocateMode.BAM) List(inCols(0), inCols(1), "End") else inCols.take(loc).toList
    val names = locNames ++ inCols.drop(loc) ++ locNames.map(build + "_" + _) ++
      List(build + "_qStrand", build + "_liftoverStatus") ++ (if (all) List(build + "_score") else Nil)
    val types = List("S", "I") ++ (if (mode.isSpan) List("I") else Nil) ++ (loc until inCols.length).map(_.toString) ++
      (0 until loc).map(_.toString) ++ List("S", "S") ++ (if (all) List("D") else Nil)

    val seen = scala.collection.mutable.HashSet[String]()
    names.foreach(n => if (!seen.add(n.toUpperCase(Locale.ROOT))) {
      throw new GorParsingException(s"LIFTOVER: output column $n already exists in the input, rename it or use another -build", "-build", build)
    })

    val resolved = FileChainSource.resolve(iargs(0), context)
    val outHeader = names.mkString("\t")
    val relocate = new LiftoverRelocateAnalysis(mode, all, cols, RowHeader(names.toArray, types.toArray),
      () => FileChainSource.cached(resolved, OverlapPolicy.BEST, context))
    relocate.setContext(context)

    CommandParsingResult(
      relocate | SortGenome(outHeader, context.getSession, Sort.parseSortColumns(Array.empty[String], executeNor = false, outHeader)),
      outHeader, resolved.usedFiles.toArray(new Array[String](0)))
  }
}
