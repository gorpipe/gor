package gorsat.liftover;

import gorsat.Commands.CommandParseUtilities;

import java.util.Locale;

/**
 * Resolves the REF and ALT columns of a variant liftover, shared by LIFTOVER_ANNOTATE and LIFTOVER. The result is
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
