package org.gorpipe.compat;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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

        System.out.printf("Accepted %d baseline cases across %d files: %d new, %d changed.%n",
                baselineCases, byFile.size(), added, changed);
        if (changed > 0) {
            System.out.println("Review the rewritten baselines before committing — each changed "
                    + "case is a behaviour change someone must vouch for.");
        }
    }

    private static boolean inContinuousIntegration() {
        return System.getenv("CI") != null
                || System.getenv("GITLAB_CI") != null
                || System.getenv("GITHUB_ACTIONS") != null;
    }
}
