package gorsat.liftover;

import gorsat.Commands.CommandParseUtilities;
import org.gorpipe.exceptions.GorParsingException;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Argument parsing shared by LIFTOVER_ANNOTATE and LIFTOVER: the contig lists (-passthrough, -mapped), -failon and
 * repeated options, with the same rules and error messages in both commands.
 * <p>
 * Resolves the REF and ALT columns of a variant liftover. The result is
 * a 0-based index into the full input row, -1 when no column is found. -ref/-alt give the column explicitly (name
 * or number, and for nor input the column number counts the user columns); otherwise the header is searched
 * case-insensitively for REF then REFERENCE, and ALT then CALL then ALLELE.
 */
public final class LiftoverArgs {

    private LiftoverArgs() {
    }

    public static int refColumn(String[] args, String header, boolean executeNor) {
        if (CommandParseUtilities.hasOption(args, "-ref")) {
            return CommandParseUtilities.columnOfOption(args, "-ref", header, executeNor);
        }
        return byName(header, "REF", "REFERENCE");
    }

    public static int altColumn(String[] args, String header, boolean executeNor) {
        if (CommandParseUtilities.hasOption(args, "-alt")) {
            return CommandParseUtilities.columnOfOption(args, "-alt", header, executeNor);
        }
        return byName(header, "ALT", "CALL", "ALLELE");
    }

    /** The option readers use the first occurrence only, so a repeated value option would be silently ignored. */
    public static void rejectRepeatedOptions(String[] args, String... options) {
        List<String> all = Arrays.asList(args);
        for (String option : options) {
            if (Collections.frequency(all, option) > 1) {
                throw new GorParsingException(option + " given more than once", option, "");
            }
        }
    }

    /**
     * The contig policy of -passthrough and -mapped. -passthrough absent is the empty set, -mapped absent is null (no
     * restriction). A contig in both lists is an error.
     */
    public static ContigPolicy contigPolicy(String[] args) {
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
        return new ContigPolicy(passthrough, mapped);
    }

    /** A comma separated contig list. Empty entries and duplicates are errors; "none" may mean the empty set. */
    static Set<String> contigList(String[] args, String option, boolean noneIsEmpty) {
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

    /** The default mapped_across_gap / size_changed boundary of -tolerance. */
    public static final double DEFAULT_TOLERANCE = 0.05;

    /**
     * -tolerance, a number in [0,1), only valid for the segment types (span), which spanFlags names in the error;
     * {@link #DEFAULT_TOLERANCE} without the option.
     */
    public static double tolerance(String[] args, boolean span, String spanFlags) {
        if (!CommandParseUtilities.hasOption(args, "-tolerance")) {
            return DEFAULT_TOLERANCE;
        }
        if (!span) {
            throw new GorParsingException("-tolerance is only valid with " + spanFlags, "-tolerance", "");
        }
        double tolerance = CommandParseUtilities.doubleValueOfOption(args, "-tolerance");
        if (!(tolerance >= 0 && tolerance < 1)) {
            throw new GorParsingException("Invalid -tolerance " + tolerance + ", it must be in [0,1)",
                    "-tolerance", Double.toString(tolerance));
        }
        return tolerance;
    }

    /**
     * The -failon statuses, each checked against the statuses the input type can report; empty without -failon.
     * modeFlag names the input type in the error message.
     */
    public static Set<String> failOn(String[] args, List<String> statuses, String modeFlag) {
        if (!CommandParseUtilities.hasOption(args, "-failon")) {
            return Set.of();
        }
        String value = CommandParseUtilities.stringValueOfOption(args, "-failon");
        Set<String> failOn = new LinkedHashSet<>();
        for (String s : value.split(",", -1)) {
            String status = s.trim();
            if (!statuses.contains(status)) {
                throw new GorParsingException("Invalid -failon status '" + status + "' for " + modeFlag
                        + ", valid: " + String.join(",", statuses).toLowerCase(Locale.ROOT), "-failon", value);
            }
            failOn.add(status);
        }
        return failOn;
    }

    /** The first match in priority order of the names, each looked up over the whole header; -1 if none. */
    private static int byName(String header, String... names) {
        String[] cols = header.split("\t", -1);
        for (String name : names) {
            for (int i = 0; i < cols.length; i++) {
                if (cols[i].trim().toUpperCase(Locale.ROOT).equals(name)) {
                    return i;
                }
            }
        }
        return -1;
    }
}
