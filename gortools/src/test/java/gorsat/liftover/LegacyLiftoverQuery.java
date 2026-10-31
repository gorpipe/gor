package gorsat.liftover;

import gorsat.Commands.CommandArguments;
import gorsat.Commands.CommandInfo;
import gorsat.Commands.CommandOptions;
import gorsat.Commands.CommandParseUtilities;
import org.gorpipe.exceptions.GorParsingException;

/**
 * Test-only oracle: the pipe text that LIFTOVER expanded to before P4 (ENGKNOW-4117, Task 10).
 * <p>
 * A Java port of the option handling and the four query branches of {@code Liftover.processArguments}
 * (Liftover.scala:36-247 at c86f39a7), with every query fragment kept byte-identical. Only the
 * {@code PipeInstance} compile step is dropped, so {@code "gor IN | " + expand(chain, opts, header)} runs today's
 * LIFTOVER as a plain query. After P4 this is the only copy of the expansion. Do not edit the fragments: the
 * differential test (UTestGorLiftoverDifferential) uses this class as the "old" side.
 */
public final class LegacyLiftoverQuery {

    /** LIFTOVER's argument spec before P4, frozen. */
    private static final CommandInfo LEGACY_LIFTOVER = new CommandInfo("LIFTOVER",
            new CommandArguments("-snp -seg -var -bam -all", "-ref -alt -build", 1, 1, false),
            new CommandOptions(true, false, false, true, true, false), false);

    private LegacyLiftoverQuery() {
    }

    /**
     * The pipe steps LIFTOVER expanded {@code liftover <chainArg> <opts>} to, on an input whose header is
     * {@code leftHeader} (the header the step sees, tab separated).
     *
     * @param chainArg the chain argument, a file path or a nested query such as {@code <(gor chain.gor)}
     * @param opts     the LIFTOVER options, for example {@code "-var -all -build hg19"}
     */
    public static String expand(String chainArg, String opts, String leftHeader) {
        String[] args = CommandParseUtilities.quoteSafeSplit((chainArg + " " + opts).trim(), ' ');
        String[] iargs = LEGACY_LIFTOVER.validateArguments(args);

        int typeCount = 0;
        String liftoverType = "seg";
        if (CommandParseUtilities.hasOption(args, "-snp")) {
            liftoverType = "snp";
            typeCount += 1;
        }
        if (CommandParseUtilities.hasOption(args, "-seg")) {
            liftoverType = "seg";
            typeCount += 1;
        }
        if (CommandParseUtilities.hasOption(args, "-var")) {
            liftoverType = "var";
            typeCount += 1;
        }
        if (CommandParseUtilities.hasOption(args, "-bam")) {
            liftoverType = "bam";
            typeCount += 1;
        }

        if (typeCount > 1) {
            throw new GorParsingException("Error in liftover type - specify only one liftover type (-snp, -seg, or -var): ");
        }

        String liftoverFile = iargs[0].trim();

        String[] hcols = leftHeader.split("\t", -1);
        int lRef = indexOfUpper(hcols, "REF");
        if (lRef < 0) lRef = indexOfUpper(hcols, "REFERENCE");
        if (lRef < 0) lRef = 2;

        int lAlt = indexOfUpper(hcols, "ALT");
        if (lAlt < 0) lAlt = indexOfUpper(hcols, "CALL");
        if (lAlt < 0) lAlt = indexOfUpper(hcols, "ALLELE");
        if (lAlt < 0) lAlt = 3;

        if (CommandParseUtilities.hasOption(args, "-ref")) {
            lRef = CommandParseUtilities.columnOfOption(args, "-ref", leftHeader, false);
        }
        if (CommandParseUtilities.hasOption(args, "-alt")) {
            lAlt = CommandParseUtilities.columnOfOption(args, "-alt", leftHeader, false);
        }

        lRef += 1;
        lAlt += 1;

        String buildPrefix = CommandParseUtilities.stringValueOfOptionWithDefault(args, "-build", "hgold");
        boolean all = CommandParseUtilities.hasOption(args, "-all");

        String[] lhCols = leftHeader.split("\t");

        if (lhCols.length < 2) {
            throw new GorParsingException("Number of columns for left header needs to be at least 2.\nLeft header: "
                    + leftHeader);
        }

        int lhColnum = lhCols.length;
        String lhChrom = lhCols[0];
        String lhStart = lhCols[1];
        String command;

        if (liftoverType.equals("snp")) {

            command =
                    "join -snpseg -maxseg 100000000 -l -e 0 -rprefix liftover " + liftoverFile +
                    "\n            | calc liftover_nnStart IF(liftover_qStrand='+',liftover_qStart+#2-liftover_tStart,1+liftover_qStart+(liftover_tEnd-#2))\n            | calc fulloverlap IF(liftover_qstrand != '0' and #2-1>=liftover_tStart and #2-1<=liftover_tEnd,'mapped','unmapped')\n            | leftwhere #" + lhColnum +
                    " fulloverlap = 'mapped'\n            | calc liftover_liftoverStatus if(fulloverlap = 'mapped','mapped','unmapped')\n            | calc liftover_score if(fulloverlap='mapped',1.0*liftover_qscore,0.0)" + (!all ?
                    "\n            | rank 1 liftover_score -gc #2-distance[-1] -o desc\n            | where rank_liftover_score = 1" : "") +
                    "\n            | calc liftover_nChrom IF(fulloverlap = 'mapped',liftover_qChrom,#1)\n            | calc liftover_nStart IF(fulloverlap = 'mapped',int(liftover_nnStart),int(0))\n            | select liftover_nChrom,liftover_nStart,3-distance[-1],1-2,liftover_qStrand,liftover_liftoverStatus" + (all ? ",liftover_score " : "") +
                    "\n          " +
                    " | rename #repl#1 " + buildPrefix + "_" + lhChrom +
                    " | rename #repl#2 " + buildPrefix + "_" + lhStart +
                    " | rename #1 " + lhChrom +
                    " | rename #2 " + lhStart +
                    " | rename liftover_(.*) " + buildPrefix +
                    "_#{1} | sort genome";

            for (int x = 1; x <= 2; x++) {
                command = command.replace("#repl#" + x, "#" + (lhColnum + x));
            }

        } else if (liftoverType.equals("var")) {

            command =
                    "join -varseg -maxseg 100000000 -l -e 0 -rprefix liftover " + liftoverFile +
                    "\n            | calc liftover_nnStart IF(liftover_qStrand='+',liftover_qStart+#2-liftover_tStart,1+liftover_qStart+(liftover_tEnd-(#2-1+len(#" + lRef +
                    "))))\n            | calc fulloverlap IF(liftover_qstrand != '0' and #2-1>=liftover_tStart and (#2-1+len(#" + lRef +
                    "))<=liftover_tEnd,'mapped','unmapped')\n            | leftwhere #" + lhColnum +
                    " fulloverlap = 'mapped'\n            | calc liftover_liftoverStatus if(fulloverlap = 'mapped','mapped','unmapped')\n            | calc liftover_score if(fulloverlap='mapped',1.0*liftover_qscore,0.0)" + (!all ?
                    "\n            | rank 1 liftover_score -gc #2-distance[-1] -o desc\n            | where rank_liftover_score = 1" : "") +
                    "\n            | calc liftover_nChrom IF(fulloverlap = 'mapped',liftover_qChrom,#1)\n            | calc liftover_nStart IF(fulloverlap = 'mapped',int(liftover_nnStart),int(0))\n            | replace #" + lRef +
                    " if(liftover_qstrand='-',revcompl(#" + lRef +
                    "),#" + lRef +
                    ") | replace #" + lAlt +
                    " if(liftover_qstrand='-',revcompl(#" + lAlt +
                    "),#" + lAlt +
                    ")\n            | select liftover_nChrom,liftover_nStart,3-distance[-1],1-2,liftover_qStrand,liftover_liftoverStatus" + (all ? ",liftover_score " : "") +
                    "\n          " +
                    " | rename #repl#1 " + buildPrefix + "_" + lhChrom +
                    " | rename #repl#2 " + buildPrefix + "_" + lhStart +
                    " | rename #1 " + lhChrom +
                    " | rename #2 " + lhStart +
                    " | rename liftover_(.*) " + buildPrefix +
                    "_#{1} | sort genome";

            for (int x = 1; x <= 2; x++) {
                command = command.replace("#repl#" + x, "#" + (lhColnum + x));
            }

        } else if (liftoverType.equals("bam")) {

            String lhEnd = "End";

            command =
                    "join -segseg -maxseg 100000000 -l -e 0 -rprefix liftover " + liftoverFile +
                    "\n            | calc liftover_nnStart IF(liftover_qStrand='+',liftover_qStart+#2-liftover_tStart,liftover_qStart+(liftover_tEnd-#3))\n            | calc liftover_nnEnd IF(liftover_qStrand='+',liftover_qStart+#3-liftover_tStart,liftover_qStart+(liftover_tEnd-#2))\n            | calc fulloverlap IF(liftover_qstrand != '0' and #2>=liftover_tStart and #3<=liftover_tEnd,'mapped','unmapped')\n            | leftwhere #" + lhColnum +
                    " fulloverlap = 'mapped'\n            | calc liftover_liftoverStatus if(fulloverlap = 'mapped','mapped','unmapped')\n            | calc liftover_score if(fulloverlap='mapped',1.0*liftover_qscore,0.0)" + (!all ?
                    "\n            | rank 1 liftover_score -gc #2-distance[-1] -o desc\n            | where rank_liftover_score = 1" : "") +
                    "\n            | calc liftover_nChrom IF(fulloverlap = 'mapped',liftover_qChrom,#1)\n            | calc liftover_nStart IF(fulloverlap = 'mapped',int(liftover_nnStart),int(0))\n            | calc liftover_nEnd IF(fulloverlap = 'mapped',int(liftover_nnEnd),int(1))\n            | replace SEQ if(liftover_qstrand='-',revcompl(SEQ),SEQ)\n            | replace QUAL if(liftover_qstrand='-',reverse(QUAL),QUAL)\n            | replace CIGAR if(liftover_qstrand='-',revcigar(CIGAR),CIGAR)\n            | replace MPOS if(isint(MPOS),MPOS-#2+liftover_nStart,MPOS)\n            | select liftover_nChrom,liftover_nStart,liftover_nEnd," + (lhColnum > 3 ? "4-distance[-1]," : "") +
                    "1-3,liftover_qStrand,liftover_liftoverStatus" + (all ? ",liftover_score " : "") +
                    "\n          " +
                    " | rename #repl#1 " + buildPrefix + "_" + lhChrom +
                    " | rename #repl#2 " + buildPrefix + "_" + lhStart +
                    " | rename #repl#3 " + buildPrefix + "_" + lhEnd +
                    " | rename #1 " + lhChrom +
                    " | rename #2 " + lhStart +
                    " | rename #3 " + lhEnd +
                    " | rename liftover_(.*) " + buildPrefix +
                    "_#{1} | sort genome";

            for (int x = 1; x <= 3; x++) {
                command = command.replace("#repl#" + x, "#" + (lhColnum + x));
            }

        } else {

            String lhEnd = lhCols[2];

            command =
                    "join -segseg -maxseg 100000000 -l -e 0 -rprefix liftover " + liftoverFile +
                    "\n            | calc liftover_nnStart IF(liftover_qStrand='+',liftover_qStart+#2-liftover_tStart,liftover_qStart+(liftover_tEnd-#3))\n            | calc liftover_nnEnd IF(liftover_qStrand='+',liftover_qStart+#3-liftover_tStart,liftover_qStart+(liftover_tEnd-#2))\n            | calc fulloverlap IF(liftover_qstrand != '0' and #2>=liftover_tStart and #3<=liftover_tEnd,'mapped','unmapped')\n            | leftwhere #" + lhColnum +
                    " fulloverlap = 'mapped'\n            | calc liftover_liftoverStatus if(fulloverlap = 'mapped','mapped','unmapped')\n            | calc liftover_score if(fulloverlap='mapped',1.0*liftover_qscore,0.0)" + (!all ?
                    "\n            | rank 1 liftover_score -gc #2-distance[-1] -o desc\n            | where rank_liftover_score = 1" : "") +
                    "\n            | calc liftover_nChrom IF(fulloverlap = 'mapped',liftover_qChrom,#1)\n            | calc liftover_nStart IF(fulloverlap = 'mapped',int(liftover_nnStart),int(0))\n            | calc liftover_nEnd IF(fulloverlap = 'mapped',int(liftover_nnEnd),int(1))\n            | select liftover_nChrom,liftover_nStart,liftover_nEnd," + (lhColnum > 3 ? "4-distance[-1]," : "") +
                    "1-3,liftover_qStrand,liftover_liftoverStatus" + (all ? ",liftover_score " : "") +
                    "\n          " +
                    " | rename #repl#1 " + buildPrefix + "_" + lhChrom +
                    " | rename #repl#2 " + buildPrefix + "_" + lhStart +
                    " | rename #repl#3 " + buildPrefix + "_" + lhEnd +
                    " | rename #1 " + lhChrom +
                    " | rename #2 " + lhStart +
                    " | rename #3 " + lhEnd +
                    " | rename liftover_(.*) " + buildPrefix +
                    "_#{1} | sort genome";

            for (int x = 1; x <= 3; x++) {
                command = command.replace("#repl#" + x, "#" + (lhColnum + x));
            }
        }

        return command;
    }

    /** Scala's {@code indexWhere(x => x.toUpperCase == name)}. */
    private static int indexOfUpper(String[] cols, String name) {
        for (int i = 0; i < cols.length; i++) {
            if (cols[i].toUpperCase().equals(name)) {
                return i;
            }
        }
        return -1;
    }
}
