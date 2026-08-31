package org.gorpipe.compat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Outcome of running one compatibility query: either a successful
 * (header, rows) pair or a captured error message, never both.
 */
public final class CompatResult {

    public final String header;
    public final List<String> rows;
    public final String errorMessage;

    private CompatResult(String header, List<String> rows, String errorMessage) {
        this.header = header;
        this.rows = rows;
        this.errorMessage = errorMessage;
    }

    public static CompatResult ok(String header, List<String> rows) {
        return new CompatResult(header, Collections.unmodifiableList(new ArrayList<>(rows)), null);
    }

    public static CompatResult error(String message) {
        return new CompatResult(null, Collections.emptyList(), message == null ? "" : message);
    }

    public boolean failed() {
        return errorMessage != null;
    }

    /**
     * The output serialisation contract: header line, one line per row,
     * newline-joined, with a trailing newline.
     *
     * Defined here rather than inherited from test infrastructure so that
     * changing it is a deliberate, reviewable act. Note that this is the in-JVM
     * shape and it differs from CLI output for NOR queries: the CLI strips the
     * ChromNOR and PosNOR columns and prefixes the header with '#'.
     */
    public String serialised() {
        if (failed()) {
            throw new IllegalStateException("cannot serialise a failed result: " + errorMessage);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(header).append('\n');
        for (String row : rows) {
            sb.append(row).append('\n');
        }
        return sb.toString();
    }
}
