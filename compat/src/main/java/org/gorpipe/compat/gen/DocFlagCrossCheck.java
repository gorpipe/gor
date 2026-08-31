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

            SurfaceInventory.CommandSurface surface = inventory.commands().get(commandName);
            if (surface == null) {
                // A page with no matching command. Real, but a different finding
                // from a flag mismatch, so it is not reported here.
                continue;
            }
            pagesParsed++;

            Set<String> documented = documentedFlags(page);
            Set<String> registered = new TreeSet<>(surface.allFlags());

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

    private static Set<String> documentedFlags(Path page) {
        Set<String> flags = new TreeSet<>();
        try {
            Matcher m = DOC_FLAG.matcher(Files.readString(page, StandardCharsets.UTF_8));
            while (m.find()) {
                flags.add(m.group(1));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + page, e);
        }
        return flags;
    }
}
