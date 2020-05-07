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

import gorsat.Commands.CommandParseUtilities._
import gorsat.IteratorUtilities.validHeader
import gorsat.process.PipeInstance
import org.gorpipe.exceptions.GorParsingException
import org.gorpipe.gor.GorContext


class Liftover extends CommandInfo("LIFTOVER",
  CommandArguments("-snp -seg -var -bam -all", "-ref -alt -build", 1, 1),
  CommandOptions(gorCommand = true, verifyCommand = true, cancelCommand = true))
{
  override def processArguments(context: GorContext, argString: String, iargs: Array[String], args: Array[String], executeNor: Boolean, forcedInputHeader: String): CommandParsingResult = {

    var typeCount = 0
    var liftoverType = "seg"
    if (hasOption(args, "-snp")) {
      liftoverType = "snp"; typeCount += 1
    }
    if (hasOption(args, "-seg")) {
      liftoverType = "seg"; typeCount += 1
    }
    if (hasOption(args, "-var")) {
      liftoverType = "var"; typeCount += 1
    }
    if (hasOption(args, "-bam")) {
      liftoverType = "bam"; typeCount += 1
    }

    if (typeCount > 1) throw new GorParsingException("Error in liftover type - specify only one liftover type (-snp, -seg, or -var): ")

    val leftHeader = forcedInputHeader
    val liftoverFile = iargs(0).trim

    var lRef = leftHeader.split("\t", -1).indexWhere(x => x.toUpperCase == "REF")
    if (lRef < 0) lRef = leftHeader.split("\t", -1).indexWhere(x => x.toUpperCase == "REFERENCE")
    if (lRef < 0) lRef = 2

    var lAlt = leftHeader.split("\t", -1).indexWhere(x => x.toUpperCase == "ALT")
    if (lAlt < 0) lAlt = leftHeader.split("\t", -1).indexWhere(x => x.toUpperCase == "CALL")
    if (lAlt < 0) lAlt = leftHeader.split("\t", -1).indexWhere(x => x.toUpperCase == "ALLELE")
    if (lAlt < 0) lAlt = 3

    if (hasOption(args, "-ref")) lRef = columnOfOption(args, "-ref", leftHeader, executeNor)
    if (hasOption(args, "-alt")) lAlt = columnOfOption(args, "-alt", leftHeader, executeNor)

    lRef += 1
    lAlt += 1

    val buildPrefix = stringValueOfOptionWithDefault(args, "-build","hgold")
    val all = hasOption(args, "-all")

    val lhCols = leftHeader.split("\t")

    if (lhCols.length < 2) {
      throw new GorParsingException(s"Number of columns for left header needs to be at least 2.\nLeft header: $leftHeader")
    }

    val lhColnum = lhCols.size
    val lhChrom = lhCols(0)
    val lhStart = lhCols(1)
    var command = ""

    var combinedHeader = validHeader(leftHeader)

    // ########## distance reference can cause problems!!

    if (liftoverType == "snp") {

      command =
        """join -snpseg -maxseg 100000000 -l -e 0 -rprefix liftover """ + liftoverFile +
          """
            | calc liftover_nnStart IF(liftover_qStrand='+',liftover_qStart+#2-liftover_tStart,1+liftover_qStart+(liftover_tEnd-#2))
            | calc fulloverlap IF(liftover_qstrand != '0' and #2-1>=liftover_tStart and #2-1<=liftover_tEnd,'mapped','unmapped')
            | leftwhere #""" + lhColnum +
          """ fulloverlap = 'mapped'
            | calc liftover_liftoverStatus if(fulloverlap = 'mapped','mapped','unmapped')
            | calc liftover_score if(fulloverlap='mapped',1.0*liftover_qscore,0.0)""" + (if (!all)
          """
            | rank 1 liftover_score -gc #2-distance[-1] -o desc
            | where rank_liftover_score = 1""" else "") +
          """
            | calc liftover_nChrom IF(fulloverlap = 'mapped',liftover_qChrom,#1)
            | calc liftover_nStart IF(fulloverlap = 'mapped',int(liftover_nnStart),int(0))
            | select liftover_nChrom,liftover_nStart,3-distance[-1],1-2,liftover_qStrand,liftover_liftoverStatus""" + (if (all) ",liftover_score " else "") +
          """
            | rename #1 """ + lhChrom +
          """ | rename #2 """ + lhStart +
          """ | rename #repl#1 """ + buildPrefix + "_" + lhChrom +
          """ | rename #repl#2 """ + buildPrefix + "_" + lhStart +
          """ | rename liftover_(.*) """ + buildPrefix +
          """_#{1} | sort genome"""

      List(1, 2).foreach(x => {
        command = command.replace("#repl#" + x, "#" + (lhColnum + x))
      })


    } else if (liftoverType == "var") {

      command =
        """join -varseg -maxseg 100000000 -l -e 0 -rprefix liftover """ + liftoverFile +
          """
            | calc liftover_nnStart IF(liftover_qStrand='+',liftover_qStart+#2-liftover_tStart,1+liftover_qStart+(liftover_tEnd-(#2-1+len(#""" + lRef +
          """))))
            | calc fulloverlap IF(liftover_qstrand != '0' and #2-1>=liftover_tStart and (#2-1+len(#""" + lRef +
          """))<=liftover_tEnd,'mapped','unmapped')
            | leftwhere #""" + lhColnum +
          """ fulloverlap = 'mapped'
            | calc liftover_liftoverStatus if(fulloverlap = 'mapped','mapped','unmapped')
            | calc liftover_score if(fulloverlap='mapped',1.0*liftover_qscore,0.0)""" + (if (!all)
          """
            | rank 1 liftover_score -gc #2-distance[-1] -o desc
            | where rank_liftover_score = 1""" else "") +
          """
            | calc liftover_nChrom IF(fulloverlap = 'mapped',liftover_qChrom,#1)
            | calc liftover_nStart IF(fulloverlap = 'mapped',int(liftover_nnStart),int(0))
            | replace #""" + lRef +
          """ if(liftover_qstrand='-',revcompl(#""" + lRef +
          """),#""" + lRef +
          """) | replace #""" + lAlt +
          """ if(liftover_qstrand='-',revcompl(#""" + lAlt +
          """),#""" + lAlt +
          """)
            | select liftover_nChrom,liftover_nStart,3-distance[-1],1-2,liftover_qStrand,liftover_liftoverStatus""" + (if (all) ",liftover_score " else "") +
          """
            | rename #1 """ + lhChrom +
          """ | rename #2 """ + lhStart +
          """ | rename #repl#1 """ + buildPrefix + "_" + lhChrom +
          """ | rename #repl#2 """ + buildPrefix + "_" + lhStart +
          """ | rename liftover_(.*) """ + buildPrefix +
          """_#{1} | sort genome"""

      List(1, 2).foreach(x => {
        command = command.replace("#repl#" + x, "#" + (lhColnum + x))
      })


    } else if (liftoverType == "bam") {

      val lhEnd = "End"

      command =
        """join -segseg -maxseg 100000000 -l -e 0 -rprefix liftover """ + liftoverFile +
          """
            | calc liftover_nnStart IF(liftover_qStrand='+',liftover_qStart+#2-liftover_tStart,liftover_qStart+(liftover_tEnd-#3))
            | calc liftover_nnEnd IF(liftover_qStrand='+',liftover_qStart+#3-liftover_tStart,liftover_qStart+(liftover_tEnd-#2))
            | calc fulloverlap IF(liftover_qstrand != '0' and #2>=liftover_tStart and #3<=liftover_tEnd,'mapped','unmapped')
            | leftwhere #""" + lhColnum +
          """ fulloverlap = 'mapped'
            | calc liftover_liftoverStatus if(fulloverlap = 'mapped','mapped','unmapped')
            | calc liftover_score if(fulloverlap='mapped',1.0*liftover_qscore,0.0)""" + (if (!all)
          """
            | rank 1 liftover_score -gc #2-distance[-1] -o desc
            | where rank_liftover_score = 1""" else "") +
          """
            | calc liftover_nChrom IF(fulloverlap = 'mapped',liftover_qChrom,#1)
            | calc liftover_nStart IF(fulloverlap = 'mapped',int(liftover_nnStart),int(0))
            | calc liftover_nEnd IF(fulloverlap = 'mapped',int(liftover_nnEnd),int(1))
            | replace SEQ if(liftover_qstrand='-',revcompl(SEQ),SEQ)
            | replace QUAL if(liftover_qstrand='-',reverse(QUAL),QUAL)
            | replace CIGAR if(liftover_qstrand='-',revcigar(CIGAR),CIGAR)
            | replace MPOS if(isint(MPOS),MPOS-#2+liftover_nStart,MPOS)
            | select liftover_nChrom,liftover_nStart,liftover_nEnd,""" + (if (lhColnum > 3) "4-distance[-1]," else "") +
          """1-3,liftover_qStrand,liftover_liftoverStatus""" + (if (all) ",liftover_score " else "") +
          """
            | rename #1 """ + lhChrom +
          """ | rename #2 """ + lhStart +
          """ | rename #3 """ + lhEnd +
          """ | rename #repl#1 """ + buildPrefix + "_" + lhChrom +
          """ | rename #repl#2 """ + buildPrefix + "_" + lhStart +
          """ | rename #repl#3 """ + buildPrefix + "_" + lhEnd +
          """ | rename liftover_(.*) """ + buildPrefix +
          """_#{1} | sort genome"""

      List(1, 2, 3).foreach(x => {
        command = command.replace("#repl#" + x, "#" + (lhColnum + x))
      })

    } else {

      val lhEnd = lhCols(2)

      command =
        """join -segseg -maxseg 100000000 -l -e 0 -rprefix liftover """ + liftoverFile +
          """
            | calc liftover_nnStart IF(liftover_qStrand='+',liftover_qStart+#2-liftover_tStart,liftover_qStart+(liftover_tEnd-#3))
            | calc liftover_nnEnd IF(liftover_qStrand='+',liftover_qStart+#3-liftover_tStart,liftover_qStart+(liftover_tEnd-#2))
            | calc fulloverlap IF(liftover_qstrand != '0' and #2>=liftover_tStart and #3<=liftover_tEnd,'mapped','unmapped')
            | leftwhere #""" + lhColnum +
          """ fulloverlap = 'mapped'
            | calc liftover_liftoverStatus if(fulloverlap = 'mapped','mapped','unmapped')
            | calc liftover_score if(fulloverlap='mapped',1.0*liftover_qscore,0.0)""" + (if (!all)
          """
            | rank 1 liftover_score -gc #2-distance[-1] -o desc
            | where rank_liftover_score = 1""" else "") +
          """
            | calc liftover_nChrom IF(fulloverlap = 'mapped',liftover_qChrom,#1)
            | calc liftover_nStart IF(fulloverlap = 'mapped',int(liftover_nnStart),int(0))
            | calc liftover_nEnd IF(fulloverlap = 'mapped',int(liftover_nnEnd),int(1))
            | select liftover_nChrom,liftover_nStart,liftover_nEnd,""" + (if (lhColnum > 3) "4-distance[-1]," else "") +
          """1-3,liftover_qStrand,liftover_liftoverStatus""" + (if (all) ",liftover_score " else "") +
          """
            | rename #1 """ + lhChrom +
          """ | rename #2 """ + lhStart +
          """ | rename #3 """ + lhEnd +
          """ | rename #repl#1 """ + buildPrefix + "_" + lhChrom +
          """ | rename #repl#2 """ + buildPrefix + "_" + lhStart +
          """ | rename #repl#3 """ + buildPrefix + "_" + lhEnd +
          """ | rename liftover_(.*) """ + buildPrefix +
          """_#{1} | sort genome"""

      List(1, 2, 3).foreach(x => {
        command = command.replace("#repl#" + x, "#" + (lhColnum + x))
      })

    }


    val itDyn = PipeInstance.createGorIterator(context)
    itDyn.scalaPipeStepInit(command, combinedHeader)
    combinedHeader = itDyn.getHeader

    CommandParsingResult(itDyn.thePipeStep, combinedHeader)
  }
}

