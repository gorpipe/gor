package org.gorpipe.compat.gen;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Guards the curated inventory files against a repeated top-level key.
 *
 * YAML resolves a repeated key by taking the last one, so appending a second
 * section for a command silently discards the first. That happened to
 * flag-values.yml with a second PRGTGEN section: two entries vanished, and the
 * only symptom was a generated case count that had fallen when it should have
 * risen. These files are appended to by hand, so the mistake is easy to repeat and
 * invisible once made.
 */
final class CuratedYaml {

    private CuratedYaml() {
    }

    /**
     * Fails when the file repeats a top-level key.
     *
     * Checked on the text rather than the parsed map, because by the time SnakeYAML
     * has produced a map the duplicate is already gone.
     */
    static void requireUniqueTopLevelKeys(Path file) {
        if (!Files.exists(file)) {
            return;
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + file, e);
        }

        Set<String> seen = new HashSet<>();
        List<String> repeated = new ArrayList<>();
        for (String line : lines) {
            // A top-level key starts at column zero and ends in a colon. Quoted
            // keys such as "*": are included; list items and comments are not.
            if (line.isEmpty() || line.startsWith("#") || line.startsWith(" ")
                    || line.startsWith("-") || !line.trim().endsWith(":")) {
                continue;
            }
            String key = line.trim();
            key = key.substring(0, key.length() - 1).replace("\"", "").trim();
            if (!seen.add(key)) {
                repeated.add(key);
            }
        }
        if (!repeated.isEmpty()) {
            throw new IllegalStateException("Repeated top-level key(s) in " + file + ": "
                    + String.join(", ", repeated)
                    + ". YAML keeps only the last, so the earlier entries would be"
                    + " discarded silently — merge the sections instead.");
        }
    }
}
