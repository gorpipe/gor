package org.gorpipe.compat.gen;

import org.gorpipe.compat.SurfaceInventory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Diffs the flags documented on each command page against the flags the registry
 * actually declares.
 *
 * This finds two defects no test case can: a flag that works but is undocumented,
 * and a flag the documentation promises that no longer exists. Reported, not
 * gated — a documentation gap should not block an engine change.
 */
public final class DocFlagCrossCheck {

    public static final class CrossCheckResult {
        /** Registered but absent from the command's Options table. */
        public final List<String> undocumented;
        /** Documented but not registered: a stale page, or a dropped flag. */
        public final List<String> phantom;
        public final int pagesParsed;

        CrossCheckResult(List<String> undocumented, List<String> phantom, int pagesParsed) {
            this.undocumented = Collections.unmodifiableList(undocumented);
            this.phantom = Collections.unmodifiableList(phantom);
            this.pagesParsed = pagesParsed;
        }
    }

    /**
     * Every flag the engine registers under this name, or null when nothing does.
     *
     * A name can be owned by more than one registry: CMD is both a pipe command and
     * an input source, and only the input source declares the documented -n.
     * Comparing a page against one registry alone reported that as a phantom flag
     * of a command that does in fact document it.
     */
    private static Set<String> registeredFlagsFor(SurfaceInventory inventory, String name) {
        Set<String> flags = new TreeSet<>();
        boolean known = false;
        for (Map<String, SurfaceInventory.CommandSurface> registry : java.util.Arrays.asList(
                inventory.commands(), inventory.inputSources(), inventory.macros())) {
            SurfaceInventory.CommandSurface surface = registry.get(name);
            if (surface != null) {
                known = true;
                flags.addAll(surface.allFlags());
            }
        }
        return known ? flags : null;
    }

    /** Flags in the docs appear as inline literals, e.g. ``-count``. */
    private static final Pattern DOC_FLAG = Pattern.compile("``(-[a-zA-Z][a-zA-Z0-9]*)``");

    private DocFlagCrossCheck() {
    }

    public static CrossCheckResult run(SurfaceInventory inventory) {
        List<String> undocumented = new ArrayList<>();
        List<String> phantom = new ArrayList<>();
        int pagesParsed = 0;

        Path commandDir = DocHarvester.docRoot().resolve("command");
        if (!Files.isDirectory(commandDir)) {
            return new CrossCheckResult(undocumented, phantom, 0);
        }

        for (Path page : DocHarvester.rstFiles(commandDir)) {
            String commandName = page.getFileName().toString()
                    .replaceAll("\\.rst$", "")
                    .toUpperCase(Locale.ROOT);

            Set<String> registered = registeredFlagsFor(inventory, commandName);
            if (registered == null) {
                // A page with no matching command, input source or macro. Real, but
                // a different finding from a flag mismatch, so it is not reported here.
                continue;
            }
            pagesParsed++;

            Set<String> documented = documentedFlags(page);

            for (String flag : registered) {
                if (!documented.contains(flag)) {
                    undocumented.add(commandName + " " + flag);
                }
            }
            for (String flag : documented) {
                if (!registered.contains(flag)) {
                    phantom.add(commandName + " " + flag);
                }
            }
        }
        return new CrossCheckResult(undocumented, phantom, pagesParsed);
    }

    /**
     * The flags a page's Options table declares.
     *
     * Scoped to the Options section, and within it to the first cell of each grid
     * table row, because pages routinely discuss other commands' flags in prose —
     * the CSVSEL page explains GOR's -f and -ff, and CIGARSEGS notes a deprecated
     * -ref. Counting those as the page's own flags reported them as phantom flags
     * of the wrong command. All 73 command pages that document flags use the same
     * grid table under an Options heading; the other 43 declare none.
     */
    private static Set<String> documentedFlags(Path page) {
        Set<String> flags = new TreeSet<>();
        for (String line : optionsSection(page)) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("|")) {
                continue;
            }
            int closing = trimmed.indexOf('|', 1);
            String firstCell = closing < 0 ? trimmed.substring(1)
                    : trimmed.substring(1, closing);
            Matcher m = DOC_FLAG.matcher(firstCell);
            while (m.find()) {
                flags.add(m.group(1));
            }
        }
        return flags;
    }

    /** Lines from the Options heading up to the next section heading. */
    private static List<String> optionsSection(Path page) {
        List<String> lines = readLines(page);
        List<String> section = new ArrayList<>();
        boolean inSection = false;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.trim().equals("Options") && isHeading(lines, i)) {
                inSection = true;
                continue;
            }
            if (inSection) {
                if (!line.trim().isEmpty() && isHeading(lines, i)) {
                    break;
                }
                section.add(line);
            }
        }
        return section;
    }

    /** True when the next line is a reStructuredText underline for this one. */
    private static boolean isHeading(List<String> lines, int index) {
        if (index + 1 >= lines.size()) {
            return false;
        }
        String underline = lines.get(index + 1).trim();
        if (underline.length() < lines.get(index).trim().length() || underline.isEmpty()) {
            return false;
        }
        char first = underline.charAt(0);
        if ("=-~^\"'".indexOf(first) < 0) {
            return false;
        }
        return underline.chars().allMatch(c -> c == first);
    }

    private static List<String> readLines(Path page) {
        try {
            return Files.readAllLines(page, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + page, e);
        }
    }
}
