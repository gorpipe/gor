package org.gorpipe.compat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Re-captures every baseline output and rewrites the committed files.
 *
 * This is the one operation that can erase a caught regression, so it refuses to
 * run in CI: baselines move only by a human act on a workstation, leaving the
 * before/after diff in the pull request where a reviewer sees it.
 */
public final class BaselineAccept {

    private BaselineAccept() {
    }

    public static void main(String[] args) {
        if (inContinuousIntegration()) {
            System.err.println("Refusing to accept baselines in CI. Baselines are accepted "
                    + "by a human on a workstation so that the diff lands in review.");
            System.exit(1);
        }

        List<CompatCase> cases = CaseLoader.loadAll();
        Map<Path, Map<String, String>> byFile = new LinkedHashMap<>();
        Map<String, String> previous = BaselineStore.loadAll(
                CaseLoader.moduleRoot().resolve("baselines"));

        int changed = 0;
        int added = 0;
        int baselineCases = 0;

        for (CompatCase c : cases) {
            if (!c.isBaseline()) {
                continue;
            }
            baselineCases++;
            String output = BaselineStore.render(CaseRunner.run(c));
            byFile.computeIfAbsent(BaselineStore.pathFor(c), k -> new TreeMap<>())
                    .put(c.id, output);

            String before = previous.get(c.id);
            if (before == null) {
                added++;
            } else if (!before.equals(output)) {
                changed++;
                System.out.println("CHANGED " + c.id);
            }
        }

        for (Map.Entry<Path, Map<String, String>> e : byFile.entrySet()) {
            BaselineStore.write(e.getKey(), e.getValue());
        }
        int pruned = pruneOrphanedBaselineFiles(byFile.keySet());

        System.out.printf("Accepted %d baseline cases across %d files: %d new, %d changed, "
                + "%d orphaned file(s) removed.%n",
                baselineCases, byFile.size(), added, changed, pruned);
        if (changed > 0) {
            System.out.println("Review the rewritten baselines before committing — each changed "
                    + "case is a behaviour change someone must vouch for.");
        }
    }

    /**
     * Deletes baseline files that no case maps to any more.
     *
     * A file still holding at least one live case is rewritten with only that
     * case's blocks, so orphaned blocks disappear on their own; a file whose every
     * case is gone would otherwise be left behind as a permanent artefact.
     */
    private static int pruneOrphanedBaselineFiles(Set<Path> live) {
        Path root = CaseLoader.moduleRoot().resolve("baselines");
        if (!Files.isDirectory(root)) {
            return 0;
        }
        int removed = 0;
        try (Stream<Path> walk = Files.walk(root)) {
            List<Path> orphaned = walk
                    .filter(p -> p.toString().endsWith(".out"))
                    .filter(p -> !live.contains(p))
                    .sorted()
                    .collect(Collectors.toList());
            for (Path file : orphaned) {
                Files.delete(file);
                removed++;
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot prune orphaned baselines under " + root, e);
        }
        return removed;
    }

    private static boolean inContinuousIntegration() {
        return System.getenv("CI") != null
                || System.getenv("GITLAB_CI") != null
                || System.getenv("GITHUB_ACTIONS") != null;
    }
}
