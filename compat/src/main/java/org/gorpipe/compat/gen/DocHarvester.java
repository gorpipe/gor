package org.gorpipe.compat.gen;

import org.gorpipe.compat.CaseLint;
import org.gorpipe.compat.CaseLoader;
import org.gorpipe.compat.CompatCase;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Harvests runnable queries from the documentation.
 *
 * A documentation snippet that no longer runs is itself a compatibility defect, so
 * the self-contained ones are worth pinning even though the pages rarely state
 * their output.
 */
public final class DocHarvester {

    public static final class HarvestResult {
        public final List<CompatCase> cases;
        public final List<String> skipped;

        HarvestResult(List<CompatCase> cases, List<String> skipped) {
            this.cases = Collections.unmodifiableList(cases);
            this.skipped = Collections.unmodifiableList(skipped);
        }
    }

    private static final String CODE_BLOCK = "code-block:: gor";

    private DocHarvester() {
    }

    public static Path docRoot() {
        return CaseLoader.moduleRoot().getParent().resolve("documentation/src");
    }

    public static HarvestResult harvest() {
        List<CompatCase> cases = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        Set<String> usedIds = new HashSet<>();
        Map<String, String> seenQueries = new HashMap<>();

        Path root = docRoot();
        if (!Files.isDirectory(root)) {
            return new HarvestResult(cases, skipped);
        }

        for (Path page : rstFiles(root)) {
            int index = 0;
            for (String snippet : extractSnippets(page)) {
                index++;
                String reason = rejectionReason(snippet);
                String label = root.relativize(page) + " block " + index;
                if (reason != null) {
                    skipped.add(label + ": " + reason);
                    continue;
                }
                // The same example is often repeated across related pages; a second
                // copy adds no coverage and corpus lint rejects duplicate bodies.
                String duplicateOf = seenQueries.get(snippet);
                if (duplicateOf != null) {
                    skipped.add(label + ": duplicate of " + duplicateOf);
                    continue;
                }
                CompatCase c = new CompatCase();
                c.id = uniqueId(page, index, usedIds);
                c.tier = "baseline";
                c.mode = "exact";
                c.behavior = "Harvested from documentation/src/" + root.relativize(page);
                c.query = snippet;
                cases.add(c);
                seenQueries.put(snippet, c.id);
            }
        }
        return new HarvestResult(cases, skipped);
    }

    /**
     * Returns why a snippet cannot become a hermetic case, or null when it can.
     *
     * The bar is deliberately high: a snippet that needs project data or is a
     * usage template would produce a case that fails for reasons unrelated to
     * compatibility.
     */
    private static String rejectionReason(String snippet) {
        String lower = snippet.toLowerCase(Locale.ROOT);
        if (snippet.contains("#")) {
            return "references project data (#ref#)";
        }
        if (snippet.contains("...") || snippet.contains("[") || snippet.contains("<")) {
            return "usage template, not a runnable query";
        }
        if (!(lower.startsWith("gorrow") || lower.startsWith("norrows"))) {
            return "not self-contained; needs a data file";
        }
        if (snippet.contains(";")) {
            return "multi-statement script";
        }
        // Screened here rather than left to fail in the suite: a snippet built on
        // random() or a clock produces a baseline that changes on its own.
        String token = CaseLint.nonDeterministicToken(snippet);
        if (token != null) {
            return "non-deterministic construct '" + token + "'";
        }
        return null;
    }

    private static String uniqueId(Path page, int index, Set<String> used) {
        String base = page.getFileName().toString()
                .replaceAll("\\.rst$", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_");
        String id = "docs." + base + ".block_" + index;
        while (!used.add(id)) {
            id = id + "_x";
        }
        return id;
    }

    /**
     * Extracts the body of each gor code block. reStructuredText delimits a
     * literal block by indentation, so the block ends at the first non-blank line
     * indented no further than the directive.
     */
    private static List<String> extractSnippets(Path page) {
        List<String> snippets = new ArrayList<>();
        List<String> lines = readLines(page);

        for (int i = 0; i < lines.size(); i++) {
            if (!lines.get(i).contains(CODE_BLOCK)) {
                continue;
            }
            int directiveIndent = indentOf(lines.get(i));
            StringBuilder body = new StringBuilder();
            for (int j = i + 1; j < lines.size(); j++) {
                String line = lines.get(j);
                if (line.isBlank()) {
                    continue;
                }
                if (indentOf(line) <= directiveIndent) {
                    break;
                }
                if (body.length() > 0) {
                    body.append(' ');
                }
                body.append(line.trim());
            }
            String snippet = body.toString().trim();
            if (!snippet.isEmpty()) {
                snippets.add(snippet);
            }
        }
        return snippets;
    }

    /**
     * Indentation width in columns. Tabs count as a full stop of 8 because the
     * pages mix tab- and space-indented blocks, and treating a tab as zero would
     * end every tab-indented block before its first line.
     */
    private static int indentOf(String line) {
        int n = 0;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == ' ') {
                n++;
            } else if (ch == '\t') {
                n += 8 - (n % 8);
            } else {
                break;
            }
        }
        return n;
    }

    private static List<String> readLines(Path page) {
        try {
            return Files.readAllLines(page, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read documentation page " + page, e);
        }
    }

    static List<Path> rstFiles(Path root) {
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(p -> p.toString().endsWith(".rst"))
                    .sorted().collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot enumerate documentation under " + root, e);
        }
    }
}
