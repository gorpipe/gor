package gorsat.Commands;

import gorsat.Utilities.IteratorUtilities;
import gorsat.liftover.ContigPolicy;
import gorsat.liftover.FileChainSource;
import gorsat.liftover.LiftMode;
import gorsat.liftover.LiftoverAnnotateAnalysis;
import gorsat.liftover.LiftoverAnnotateConfig;
import gorsat.liftover.LiftoverArgs;
import gorsat.liftover.OverlapPolicy;
import gorsat.liftover.ResolvedChain;
import org.gorpipe.exceptions.GorParsingException;
import org.gorpipe.gor.session.GorContext;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * LIFTOVER_ANNOTATE: appends the lifted-over coordinates of every row, in place and in input order, as extra
 * columns. One of -snp, -var, -seg, -seg1 selects the input type. The chain is a file or a nested gor query.
 */
public class LiftoverAnnotate extends CommandInfo {

    private static final String DEFAULT_PREFIX = "liftover_";
    private static final String BAD_PREFIX_CHARS = "\\/*+-'$;,";
    private static final double DEFAULT_TOLERANCE = 0.05;

    public LiftoverAnnotate() {
        super("LIFTOVER_ANNOTATE",
                new CommandArguments("-snp -var -seg -seg1 -diag",
                        "-prefix -passthrough -mapped -overlap -tolerance -failon -chrom -pos -end -ref -alt",
                        1, 1, false),
                new CommandOptions(true, true, false, true, true, false),
                false);
    }

    @Override
    public CommandParsingResult processArguments(GorContext context, String argString, String[] iargs,
                                                 String[] args, boolean executeNor, String forcedInputHeader) {
        LiftMode mode = parseMode(args);
        boolean diag = CommandParseUtilities.hasOption(args, "-diag");
        boolean span = mode == LiftMode.SEG || mode == LiftMode.SEG1;
        if (!span && diag) {
            throw new GorParsingException("-diag is only valid with -seg and -seg1", "-diag", "");
        }
        if (!span && CommandParseUtilities.hasOption(args, "-tolerance")) {
            throw new GorParsingException("-tolerance is only valid with -seg and -seg1", "-tolerance", "");
        }

        if (!span && CommandParseUtilities.hasOption(args, "-end")) {
            throw new GorParsingException("-end is only valid with -seg or -seg1", "-end", "");
        }
        if (mode != LiftMode.VAR) {
            for (String option : new String[]{"-ref", "-alt"}) {
                if (CommandParseUtilities.hasOption(args, option)) {
                    throw new GorParsingException(option + " is only valid with -var", option, "");
                }
            }
        }

        String prefix = CommandParseUtilities.stringValueOfOptionWithDefault(args, "-prefix", DEFAULT_PREFIX);
        for (int i = 0; i < prefix.length(); i++) {
            if (BAD_PREFIX_CHARS.indexOf(prefix.charAt(i)) >= 0) {
                throw new GorParsingException("Invalid -prefix '" + prefix + "', it cannot contain any of "
                        + "\\ / * + - ' $ ; ,", "-prefix", prefix);
            }
        }

        OverlapPolicy overlap = OverlapPolicy.parse(CommandParseUtilities.stringValueOfOptionWithDefaultWithErrorCheck(
                args, "-overlap", "error", new String[]{"error", "best"}));

        double tolerance = DEFAULT_TOLERANCE;
        if (CommandParseUtilities.hasOption(args, "-tolerance")) {
            tolerance = CommandParseUtilities.doubleValueOfOption(args, "-tolerance");
            if (!(tolerance >= 0 && tolerance < 1)) {
                throw new GorParsingException("Invalid -tolerance " + tolerance + ", it must be in [0,1)",
                        "-tolerance", Double.toString(tolerance));
            }
        }

        Set<String> passthrough = CommandParseUtilities.hasOption(args, "-passthrough")
                ? contigList(args, "-passthrough", true) : Set.of();
        Set<String> mapped = CommandParseUtilities.hasOption(args, "-mapped")
                ? contigList(args, "-mapped", false) : null;
        if (mapped != null) {
            for (String c : passthrough) {
                if (mapped.contains(c)) {
                    throw new GorParsingException("Contig " + c + " is in both -passthrough and -mapped",
                            "-mapped", c);
                }
            }
        }

        Set<String> failOn = parseFailOn(args, mode);

        String header = forcedInputHeader;
        int chromCol;
        int posCol;
        int endCol = -1;
        int refCol = -1;
        int altCol = -1;
        if (executeNor) {
            requireOption(args, "-chrom", "nor input");
            requireOption(args, "-pos", "nor input");
            if (span) {
                requireOption(args, "-end", mode.flag());
            }
        }
        chromCol = CommandParseUtilities.hasOption(args, "-chrom")
                ? CommandParseUtilities.columnOfOption(args, "-chrom", header, executeNor) : 0;
        posCol = CommandParseUtilities.hasOption(args, "-pos")
                ? CommandParseUtilities.columnOfOption(args, "-pos", header, executeNor) : 1;
        if (span) {
            endCol = CommandParseUtilities.hasOption(args, "-end")
                    ? CommandParseUtilities.columnOfOption(args, "-end", header, executeNor) : 2;
            if (endCol >= header.split("\t", -1).length) {
                throw new GorParsingException(mode.flag() + " requires an end column, the input has none "
                        + "(use -end)", "-end", "");
            }
        }
        if (mode == LiftMode.VAR) {
            refCol = LiftoverArgs.refColumn(args, header, executeNor);
            altCol = LiftoverArgs.altColumn(args, header, executeNor);
            if (refCol < 0) {
                throw new GorParsingException("-var requires a REF column (REF/REFERENCE or -ref)", "-ref", "");
            }
            if (altCol < 0) {
                throw new GorParsingException("-var requires an ALT column (ALT/CALL/ALLELE or -alt)", "-alt", "");
            }
        }

        List<LiftoverAnnotateAnalysis.OutputColumn> outputColumns =
                LiftoverAnnotateAnalysis.outputColumns(mode, prefix, diag);
        String[] inputColumns = header.split("\t", -1);
        for (LiftoverAnnotateAnalysis.OutputColumn out : outputColumns) {
            for (String in : inputColumns) {
                if (in.trim().equalsIgnoreCase(out.name())) {
                    throw new GorParsingException("Output column " + out.name() + " already exists in input",
                            "-prefix", prefix);
                }
            }
        }

        ResolvedChain resolved = FileChainSource.resolve(iargs[0], context);

        List<String> outNames = new ArrayList<>(List.of(inputColumns));
        for (LiftoverAnnotateAnalysis.OutputColumn out : outputColumns) {
            outNames.add(out.name());
        }
        String outHeader = IteratorUtilities.validHeader(String.join("\t", outNames), true);

        LiftoverAnnotateConfig config = new LiftoverAnnotateConfig(mode, chromCol, posCol, endCol, refCol, altCol,
                new ContigPolicy(passthrough, mapped), tolerance, diag, failOn);
        LiftoverAnnotateAnalysis analysis = new LiftoverAnnotateAnalysis(config,
                LiftoverAnnotateAnalysis.outgoingHeader(header, mode, prefix, diag),
                () -> FileChainSource.cached(resolved, overlap, context), context);
        return new CommandParsingResult(analysis, outHeader, resolved.usedFiles().toArray(new String[0]), null);
    }

    private static LiftMode parseMode(String[] args) {
        LiftMode found = null;
        int count = 0;
        for (LiftMode m : LiftMode.values()) {
            if (CommandParseUtilities.hasOption(args, m.flag())) {
                found = m;
                count++;
            }
        }
        if (count != 1) {
            throw new GorParsingException("Specify exactly one of -snp, -var, -seg, -seg1");
        }
        return found;
    }

    private static void requireOption(String[] args, String option, String why) {
        if (!CommandParseUtilities.hasOption(args, option)) {
            throw new GorParsingException(option + " is required with " + why, option, "");
        }
    }

    /** A comma separated contig list. Empty entries and duplicates are errors; "none" may mean the empty set. */
    private static Set<String> contigList(String[] args, String option, boolean noneIsEmpty) {
        String value = CommandParseUtilities.stringValueOfOption(args, option).trim();
        if (noneIsEmpty && value.equals("none")) {
            return Set.of();
        }
        Set<String> contigs = new LinkedHashSet<>();
        for (String c : value.split(",", -1)) {
            String contig = c.trim();
            if (contig.isEmpty()) {
                throw new GorParsingException("Empty contig in " + option + " list '" + value + "'", option, value);
            }
            if (!contigs.add(contig)) {
                throw new GorParsingException("Duplicate contig " + contig + " in " + option, option, value);
            }
        }
        return contigs;
    }

    private static Set<String> parseFailOn(String[] args, LiftMode mode) {
        if (!CommandParseUtilities.hasOption(args, "-failon")) {
            return Set.of();
        }
        String value = CommandParseUtilities.stringValueOfOption(args, "-failon");
        Set<String> failOn = new LinkedHashSet<>();
        for (String s : value.split(",", -1)) {
            String status = s.trim();
            if (!mode.statuses().contains(status)) {
                throw new GorParsingException("Invalid -failon status '" + status + "' for " + mode.flag()
                        + ", valid: " + String.join(",", mode.statuses()).toLowerCase(Locale.ROOT), "-failon", value);
            }
            failOn.add(status);
        }
        return failOn;
    }
}
