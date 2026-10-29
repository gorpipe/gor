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
 * Guards the curated inventory files against a repeated key.
 *
 * YAML resolves a repeated key by taking the last one, so appending a second
 * section for a command silently discards the first. That happened to
 * flag-values.yml with a second PRGTGEN section: two entries vanished, and the
 * only symptom was a generated case count that had fallen when it should have
 * risen. Merging those two sections then left six flags listed twice inside the
 * one block, where the same rule applies one level down and the earlier value is
 * what gets dropped. These files are appended to by hand, so the mistake is easy
 * to repeat and invisible once made.
 */
final class CuratedYaml {

    private CuratedYaml() {
    }

    /**
     * Fails when the file repeats a key, at the top level or within one block.
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

        Set<String> seenTopLevel = new HashSet<>();
        Set<String> seenNested = new HashSet<>();
        List<String> repeated = new ArrayList<>();
        String block = "";
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || line.startsWith("-")) {
                continue;
            }
            // A top-level key starts at column zero and ends in a colon. Quoted
            // keys such as "*": are included; list items and comments are not.
            if (!line.startsWith(" ")) {
                if (!trimmed.endsWith(":")) {
                    continue;
                }
                block = unquote(trimmed.substring(0, trimmed.length() - 1));
                if (!seenTopLevel.add(block)) {
                    repeated.add(block);
                }
                continue;
            }
            // A nested key: indented, and "key: value" rather than a bare value.
            int colon = colonEndingTheKey(trimmed);
            if (colon < 0 || block.isEmpty()) {
                continue;
            }
            String key = unquote(trimmed.substring(0, colon));
            if (!seenNested.add(block + " " + key)) {
                repeated.add(block + " " + key);
            }
        }
        if (!repeated.isEmpty()) {
            throw new IllegalStateException("Repeated key(s) in " + file + ": "
                    + String.join(", ", repeated)
                    + ". YAML keeps only the last, so the earlier entries would be"
                    + " discarded silently — merge the sections instead.");
        }
    }

    /**
     * The index of the colon that ends the key, or -1 when the line is not a
     * mapping entry. A quoted key may itself contain a colon — "-ps": ":" — so a
     * quoted key ends at its closing quote and an unquoted one at its first colon.
     */
    private static int colonEndingTheKey(String trimmed) {
        if (trimmed.startsWith("\"")) {
            int close = trimmed.indexOf('"', 1);
            if (close < 0 || close + 1 >= trimmed.length() || trimmed.charAt(close + 1) != ':') {
                return -1;
            }
            return close + 1;
        }
        int colon = trimmed.indexOf(':');
        return colon <= 0 ? -1 : colon;
    }

    private static String unquote(String key) {
        return key.replace("\"", "").trim();
    }
}
