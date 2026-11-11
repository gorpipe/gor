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
import java.util.List;
import java.util.Set;

/**
 * LIFTOVER_ANNOTATE: appends the lifted-over coordinates of every row, in place and in input order, as extra
 * columns. One of -snp, -var, -seg, -seg1 selects the input type. The chain is a file or a nested gor query.
 */
public class LiftoverAnnotate extends CommandInfo {

    private static final String DEFAULT_PREFIX = "liftover_";
    private static final String BAD_PREFIX_CHARS = "\\/*+-'$;,";
    private static final String[] VALUE_OPTIONS = {"-failon", "-passthrough", "-mapped", "-prefix", "-overlap",
            "-tolerance", "-chrom", "-pos", "-end", "-ref", "-alt"};

    public LiftoverAnnotate() {
        super("LIFTOVER_ANNOTATE",
                new CommandArguments("-snp -var -seg -seg1 -diag -aligned -nogapfill",
                        "-prefix -passthrough -mapped -overlap -tolerance -failon -chrom -pos -end -ref -alt",
                        1, 1, false),
                new CommandOptions(true, true, false, true, true, false),
                false);
    }

    @Override
    public CommandParsingResult processArguments(GorContext context, String argString, String[] iargs,
                                                 String[] args, boolean executeNor, String forcedInputHeader) {
        LiftoverArgs.rejectRepeatedOptions(args, VALUE_OPTIONS);
        LiftMode mode = parseMode(args);
        boolean diag = CommandParseUtilities.hasOption(args, "-diag");
        boolean fillGaps = !CommandParseUtilities.hasOption(args, "-nogapfill");
        boolean span = mode == LiftMode.SEG || mode == LiftMode.SEG1;
        if (!span && diag) {
            throw new GorParsingException("-diag is only valid with -seg and -seg1", "-diag", "");
        }

        if (!span && CommandParseUtilities.hasOption(args, "-end")) {
            throw new GorParsingException("-end is only valid with -seg or -seg1", "-end", "");
        }
        boolean aligned = CommandParseUtilities.hasOption(args, "-aligned");
        if (aligned && mode != LiftMode.VAR) {
            throw new GorParsingException("-aligned is only valid with -var", "-aligned", "");
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

        double tolerance = LiftoverArgs.tolerance(args, span, "-seg and -seg1");

        ContigPolicy policy = LiftoverArgs.contigPolicy(args);

        Set<String> failOn = LiftoverArgs.failOn(args, mode.statuses(), mode.flag());

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
                LiftoverAnnotateAnalysis.outputColumns(mode, prefix, diag, aligned);
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
                policy, tolerance, diag, failOn, aligned);
        LiftoverAnnotateAnalysis analysis = new LiftoverAnnotateAnalysis(config,
                LiftoverAnnotateAnalysis.outgoingHeader(header, mode, prefix, diag, aligned),
                () -> FileChainSource.cached(resolved, overlap, fillGaps, context), context);
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
}
