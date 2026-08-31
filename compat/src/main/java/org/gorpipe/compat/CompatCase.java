package org.gorpipe.compat;

import java.util.ArrayList;
import java.util.List;

/**
 * One compatibility case.
 *
 * A spec case is hand-authored: it carries its expected output inline and cites
 * where that expectation came from. A baseline case is generated: it carries no
 * expected output, because its output lives in a committed baseline file.
 */
public final class CompatCase {

    public String id;
    public String tier;
    public String mode;
    public String source;
    public String query;
    public String expected;
    public String errorContains;
    public String behavior;
    public List<String> cites = new ArrayList<>();
    public List<CompatInput> inputs = new ArrayList<>();

    /** Absolute path of the file this case was loaded from; used in messages. */
    public String sourceFile;

    public String category() {
        return id.split("\\.")[0];
    }

    public String feature() {
        String[] parts = id.split("\\.");
        return parts.length > 1 ? parts[1] : "";
    }

    public boolean isSpec() {
        return "spec".equals(tier);
    }

    public boolean isBaseline() {
        return "baseline".equals(tier);
    }

    @Override
    public String toString() {
        return id;
    }
}
