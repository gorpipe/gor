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

package gorsat.Commands;

import gorsat.Analysis.SortGenome;
import gorsat.Utilities.IteratorUtilities;
import gorsat.liftover.FileChainSource;
import gorsat.liftover.LiftoverArgs;
import gorsat.liftover.LiftoverRelocateAnalysis;
import gorsat.liftover.OverlapPolicy;
import gorsat.liftover.RelocateColumns;
import gorsat.liftover.RelocateMode;
import gorsat.liftover.ResolvedChain;
import org.gorpipe.exceptions.GorParsingException;
import org.gorpipe.gor.session.GorContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * LIFTOVER: moves rows to their lifted location on the target assembly and sorts the result. Every mode (-snp, -var,
 * -seg, -bam, -all) and nested-query chains run on the same engine as LIFTOVER_ANNOTATE (ENGKNOW-4117, N17).
 */
public class Liftover extends CommandInfo {

    private static final String[] MODE_FLAGS = {"-snp", "-seg", "-var", "-bam"};
    private static final RelocateMode[] MODES = {RelocateMode.SNP, RelocateMode.SEG, RelocateMode.VAR,
            RelocateMode.BAM};

    public Liftover() {
        super("LIFTOVER",
                new CommandArguments("-snp -seg -var -bam -all", "-ref -alt -build", 1, 1, false),
                new CommandOptions(true, false, false, true, true, false),
                false);
    }

    @Override
    public CommandParsingResult processArguments(GorContext context, String argString, String[] iargs,
                                                 String[] args, boolean executeNor, String forcedInputHeader) {
        int modeIndex = -1;
        for (int i = 0; i < MODE_FLAGS.length; i++) {
            if (CommandParseUtilities.hasOption(args, MODE_FLAGS[i])) {
                if (modeIndex >= 0) {
                    throw new GorParsingException("Error in liftover type - specify only one liftover type (-snp, "
                            + "-seg, or -var): ");
                }
                modeIndex = i;
            }
        }
        RelocateMode mode = modeIndex >= 0 ? MODES[modeIndex] : RelocateMode.SEG;
        String modeFlag = modeIndex >= 0 ? MODE_FLAGS[modeIndex] : "-seg";

        String[] inCols = IteratorUtilities.validHeader(forcedInputHeader, true).split("\t", -1);
        if (inCols.length < 2) {
            throw new GorParsingException("Number of columns for left header needs to be at least 2.\nLeft header: "
                    + forcedInputHeader);
        }
        if (mode.isSpan() && inCols.length < 3) {
            throw new GorParsingException("LIFTOVER " + modeFlag + " requires an end column (#3), the input has none");
        }

        // REF and ALT by name or option, else #3 and #4 (resolved in every mode, as before P4, so a bad -ref/-alt fails)
        int refByName = LiftoverArgs.refColumn(args, forcedInputHeader, executeNor);
        int altByName = LiftoverArgs.altColumn(args, forcedInputHeader, executeNor);
        int ref = refByName >= 0 ? refByName : 2;
        int alt = altByName >= 0 ? altByName : 3;

        RelocateColumns cols;
        if (mode == RelocateMode.VAR) {
            if (ref >= inCols.length) {
                throw new GorParsingException("-var requires a REF column (REF/REFERENCE, -ref or #3)");
            }
            if (alt >= inCols.length) {
                throw new GorParsingException("-var requires an ALT column (ALT/CALL/ALLELE, -alt or #4)");
            }
            cols = new RelocateColumns(ref, alt, -1, -1, -1, -1, -1);
        } else if (mode == RelocateMode.BAM) {
            cols = new RelocateColumns(-1, -1, bamColumn(inCols, "SEQ", true), bamColumn(inCols, "QUAL", true),
                    bamColumn(inCols, "CIGAR", true), bamColumn(inCols, "MPOS", true),
                    bamColumn(inCols, "MRNM", false));
        } else {
            cols = RelocateColumns.NONE;
        }

        String build = CommandParseUtilities.stringValueOfOptionWithDefault(args, "-build", "hgold");
        boolean all = CommandParseUtilities.hasOption(args, "-all");

        // output: target location, the input columns after the location, source location, strand, status[, score]
        int loc = mode.isSpan() ? 3 : 2;
        List<String> locNames = mode == RelocateMode.BAM
                ? List.of(inCols[0], inCols[1], "End") : Arrays.asList(inCols).subList(0, loc);
        List<String> names = new ArrayList<>(locNames);
        names.addAll(Arrays.asList(inCols).subList(loc, inCols.length));
        for (String n : locNames) {
            names.add(build + "_" + n);
        }
        names.add(build + "_qStrand");
        names.add(build + "_liftoverStatus");
        if (all) {
            names.add(build + "_score");
        }

        List<String> types = new ArrayList<>(List.of("S", "I"));
        if (mode.isSpan()) {
            types.add("I");
        }
        for (int i = loc; i < inCols.length; i++) {
            types.add(Integer.toString(i));
        }
        for (int i = 0; i < loc; i++) {
            types.add(Integer.toString(i));
        }
        types.add("S");
        types.add("S");
        if (all) {
            types.add("D");
        }

        Set<String> seen = new HashSet<>();
        for (String n : names) {
            if (!seen.add(n.toUpperCase(Locale.ROOT))) {
                throw new GorParsingException("LIFTOVER: output column " + n + " already exists in the input, rename "
                        + "it or use another -build", "-build", build);
            }
        }

        ResolvedChain resolved = FileChainSource.resolve(iargs[0], context);
        String outHeader = String.join("\t", names);
        LiftoverRelocateAnalysis relocate = new LiftoverRelocateAnalysis(mode, all, cols,
                new RowHeader(names.toArray(new String[0]), types.toArray(new String[0])),
                () -> FileChainSource.cached(resolved, OverlapPolicy.BEST, context));
        relocate.setContext(context);

        SortGenome sort = new SortGenome(outHeader, context.getSession(),
                Sort.parseSortColumns(new String[0], false, outHeader), 1);
        return new CommandParsingResult(relocate.$bar(sort), outHeader,
                resolved.usedFiles().toArray(new String[0]), null);
    }

    private static int bamColumn(String[] inCols, String name, boolean required) {
        for (int i = 0; i < inCols.length; i++) {
            if (inCols[i].trim().toUpperCase(Locale.ROOT).equals(name)) {
                return i;
            }
        }
        if (required) {
            throw new GorParsingException("-bam requires a " + name + " column");
        }
        return -1;
    }
}
