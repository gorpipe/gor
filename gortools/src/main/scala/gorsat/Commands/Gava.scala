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

import gorsat.Analysis.VaastAnalysis
import gorsat.Commands.CommandParseUtilities._
import gorsat.Utilities.IteratorUtilities.validHeader
import gorsat.gorsatGorIterator.MapAndListUtilities
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.session.GorContext

class Gava extends CommandInfo("GAVA",
  CommandArguments("-recessive -dominant -noMaxAlleleCounts -protective -debug", "-caselist -casefile -ctrllist -ctrlfile -grouping -bailout -casepene -ctrlpene -maxAf", 1, 1),
  CommandOptions(gorCommand = true, verifyCommand = true, cancelCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {

    val CASElist:List[String] = if (hasOption(args, "-caselist")) stringValueOfOption(args, "-caselist").split(',').toList.map(replaceSingleQuotes)
    else if (hasOption(args, "-casefile")) MapAndListUtilities.readArray(stringValueOfOption(args, "-casefile"), context.getSession.getProjectContext.getFileReader).toList
    else Nil

    val CTRLlist:List[String] = if (hasOption(args, "-ctrllist")) stringValueOfOption(args, "-ctrllist").split(',').toList.map(replaceSingleQuotes)
    else if (hasOption(args, "-ctrlfile")) MapAndListUtilities.readArray(stringValueOfOption(args, "-ctrlfile"), context.getSession.getProjectContext.getFileReader).toList
    else Nil

    if (CASElist == Nil) {
      throw new GorParsingException("Error in cases - none specified.  Please check your -caselist argument or -casefile file: ")
    }

    if (CTRLlist == Nil) {
      throw new GorParsingException("Error in cases - none specified.  Please check your -ctrllist argument or -ctrlfile file: ")
    }

    val maxIterations = parseIntWithRangeCheck("maxIterations", iargs(0), 0)

    val pipeStep = VaastAnalysis(context.getSession, CASElist, CTRLlist, maxIterations)

    if (hasOption(args, "-recessive")) pipeStep.recessive = true
    if (hasOption(args, "-dominant")) pipeStep.dominant = true
    if (pipeStep.recessive && pipeStep.dominant) {
      throw new GorParsingException("Error in options - not possible to use both recessive and dominant modeling: ")
    }
    if (hasOption(args, "-casepene")) pipeStep.casepene = intValueOfOption(args, "-casepene")
    if (hasOption(args, "-ctrlpene")) pipeStep.ctrlpene = intValueOfOption(args, "-ctrlpene")
    if (hasOption(args, "-noMaxAlleleCounts")) pipeStep.noMaxAlleleCounts = true
    if (hasOption(args, "-protective")) pipeStep.protective = true
    if (hasOption(args, "-grouping")) pipeStep.collapsingThreshold = intValueOfOption(args, "-grouping")
    if (hasOption(args, "-bailout")) pipeStep.bailOutAfter = intValueOfOption(args, "-bailout")
    if (hasOption(args, "-usePhase")) pipeStep.usePhase = true
    if (hasOption(args, "-maxAf")) pipeStep.maxAf = doubleValueOfOption(args, "-maxAf")
    if (hasOption(args, "-debug")) pipeStep.debug = true

    val leftHeader =  forcedInputHeader
    val headerItems = leftHeader.split("\t", -1)

    pipeStep.geneCol = headerItems.indexWhere(x => x.toUpperCase == "GENE_SYMBOL")
    if (pipeStep.geneCol < 0) pipeStep.geneCol = headerItems.indexWhere(x => x.toUpperCase == "GENE")
    if (pipeStep.geneCol < 0) pipeStep.geneCol = headerItems.indexWhere(x => x.toUpperCase == "GROUP")
    if (pipeStep.geneCol < 0) pipeStep.geneCol = 3


    pipeStep.posCol = headerItems.indexWhere(x => x.toUpperCase == "POS")
    if (pipeStep.posCol < 0) pipeStep.posCol = 5

    pipeStep.altCol = headerItems.indexWhere(x => x.toUpperCase == "ALT")
    if (pipeStep.altCol < 0) pipeStep.altCol = headerItems.indexWhere(x => x.toUpperCase == "CALL")
    if (pipeStep.altCol < 0) pipeStep.altCol = headerItems.indexWhere(x => x.toUpperCase == "ALLELE")
    if (pipeStep.altCol < 0) pipeStep.altCol = 6

    pipeStep.refCol = headerItems.indexWhere(x => x.toUpperCase == "REF")
    if (pipeStep.refCol < 0) pipeStep.refCol = headerItems.indexWhere(x => x.toUpperCase == "REFERENCE")
    if (pipeStep.refCol < 0) pipeStep.refCol = 7

    pipeStep.pnCol = headerItems.indexWhere(x => x.toUpperCase == "PN")
    if (pipeStep.pnCol < 0) pipeStep.pnCol = headerItems.indexWhere(x => x.toUpperCase == "SUBJECT")
    if (pipeStep.pnCol < 0) pipeStep.pnCol = 8

    pipeStep.callCopiesCol = headerItems.indexWhere(x => x.toUpperCase == "CALLCOPIES")
    if (pipeStep.callCopiesCol < 0) pipeStep.callCopiesCol = headerItems.indexWhere(x => x.toUpperCase == "ZYGOSITY")
    if (pipeStep.callCopiesCol < 0) pipeStep.callCopiesCol = 9

    pipeStep.phaseCol = headerItems.indexWhere(x => x.toUpperCase == "PHASE")
    if (pipeStep.phaseCol < 0) pipeStep.phaseCol = 10

    pipeStep.scoreCol = headerItems.indexWhere(x => x.toUpperCase == "SCORE")
    if (pipeStep.scoreCol < 0) pipeStep.scoreCol = 11

    pipeStep.initializeColumnArrays()

    val combinedHeader = validHeader("Chrom\tgene_start\tgene_end\tgene_symbol\tchiPVal\tpVal\tIterations")

    CommandParsingResult(pipeStep, combinedHeader)
  }
}

