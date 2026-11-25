package gorsat.liftover;

import org.gorpipe.exceptions.GorParsingException;

import java.util.Locale;

/**
 * 0-based column positions of the chain columns in a chain file or nested query. tStart is the row position
 * and chrom the row contig, so they have no entry.
 */
public record ChainColumns(int tEnd, int qChrom, int qStart, int qEnd, int qStrand, int qScore) {

    /** Finds the columns by header name, case-insensitively. A missing column is a GorParsingException. */
    public static ChainColumns fromHeader(String header, String chainPath) {
        String[] names = header.split("\t", -1);
        return new ChainColumns(
                find(names, "tEnd", chainPath),
                find(names, "qChrom", chainPath),
                find(names, "qStart", chainPath),
                find(names, "qEnd", chainPath),
                find(names, "qStrand", chainPath),
                find(names, "qScore", chainPath));
    }

    private static int find(String[] names, String name, String chainPath) {
        String wanted = name.toUpperCase(Locale.ROOT);
        // columns 0 and 1 are the chromosome and position
        for (int i = 2; i < names.length; i++) {
            if (names[i].trim().toUpperCase(Locale.ROOT).equals(wanted)) {
                return i;
            }
        }
        throw new GorParsingException("Liftover chain " + chainPath + " has no " + name + " column, header: "
                + header(names), "chain", chainPath);
    }

    private static String header(String[] names) {
        return String.join(",", names);
    }
}
