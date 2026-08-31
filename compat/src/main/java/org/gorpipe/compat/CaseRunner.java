package org.gorpipe.compat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Executes one case in an isolated temporary project root.
 *
 * On failure the temporary directory is deliberately retained and its path
 * reported: debugging a mismatch without the fixtures that produced it is
 * impractical.
 */
public final class CaseRunner {

    /** Result of a run plus the fixture directory, if it was kept for debugging. */
    public static final class CaseOutcome {
        public final CompatResult result;
        public final Path retainedRoot;

        CaseOutcome(CompatResult result, Path retainedRoot) {
            this.result = result;
            this.retainedRoot = retainedRoot;
        }
    }

    private CaseRunner() {
    }

    /** Runs the case and always cleans up. Used by generators and the baseline tier. */
    public static CompatResult run(CompatCase c) {
        Path root = createRoot();
        try {
            return execute(c, root);
        } finally {
            deleteRecursively(root);
        }
    }

    /** Runs the case, keeping the fixture directory when the caller may need it. */
    public static CaseOutcome runRetainingOnFailure(CompatCase c) {
        Path root = createRoot();
        CompatResult result;
        try {
            result = execute(c, root);
        } catch (RuntimeException e) {
            return new CaseOutcome(CompatResult.error(String.valueOf(e.getMessage())), root);
        }
        return new CaseOutcome(result, root);
    }

    /** Asserts a spec case against its inline expectation. */
    public static void assertSpec(CompatCase c) {
        CaseOutcome outcome = runRetainingOnFailure(c);
        boolean passed = false;
        try {
            compare(c, outcome.result, outcome.retainedRoot);
            passed = true;
        } finally {
            if (passed) {
                deleteRecursively(outcome.retainedRoot);
            }
        }
    }

    private static Path createRoot() {
        try {
            return Files.createTempDirectory("gor-compat-");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static CompatResult execute(CompatCase c, Path root) {
        if (c.needsReference) {
            Fixtures.writeReferenceBuild(root);
        }
        materialiseInputs(c, root);
        String query = c.query.replace("${ROOT}", root.toAbsolutePath().toString());
        return canonicalise(CompatExecutor.run(query, root, Fixtures.configFileIfPresent(root)),
                root);
    }

    /**
     * Puts ${ROOT} back wherever the temporary project root appears in the output.
     *
     * The root is a fresh directory per run and engine error messages routinely
     * quote paths, so without this an affected case's output would differ on every
     * run and could never be pinned to a baseline. Both the temp path and its
     * resolved form are replaced: on macOS /var/folders is a symlink into
     * /private/var/folders, and the engine reports whichever it was handed.
     */
    private static CompatResult canonicalise(CompatResult result, Path root) {
        List<String> paths = new ArrayList<>();
        paths.add(root.toAbsolutePath().toString());
        try {
            String real = root.toRealPath().toString();
            if (!paths.contains(real)) {
                paths.add(real);
            }
        } catch (IOException ignored) {
            // The root is gone or unreadable; the absolute form is all we can strip.
        }
        // Longest first, so replacing the shorter form cannot leave a fragment of
        // the longer one behind.
        paths.sort(Comparator.comparingInt(String::length).reversed());

        if (result.failed()) {
            return CompatResult.error(replaceAll(result.errorMessage, paths));
        }
        List<String> rows = new ArrayList<>(result.rows.size());
        for (String row : result.rows) {
            rows.add(replaceAll(row, paths));
        }
        return CompatResult.ok(replaceAll(result.header, paths), rows);
    }

    private static String replaceAll(String text, List<String> paths) {
        String out = text;
        for (String path : paths) {
            out = out.replace(path, "${ROOT}");
        }
        return out;
    }

    private static void compare(CompatCase c, CompatResult result, Path root) {
        if ("error".equals(c.mode)) {
            if (!result.failed()) {
                throw new AssertionError(message(c, root,
                        "expected the query to fail, but it succeeded with header: " + result.header));
            }
            if (c.errorContains != null && !c.errorContains.isEmpty()
                    && !result.errorMessage.contains(c.errorContains)) {
                throw new AssertionError(message(c, root,
                        "expected the error to contain '" + c.errorContains
                                + "' but it was '" + result.errorMessage + "'"));
            }
            return;
        }

        if (result.failed()) {
            throw new AssertionError(message(c, root,
                    "query failed unexpectedly: " + result.errorMessage));
        }

        String actual = result.serialised();
        if (!c.expected.equals(actual)) {
            throw new AssertionError(message(c, root,
                    "output mismatch\n--- expected ---\n" + visible(c.expected)
                            + "--- actual ---\n" + visible(actual)));
        }
    }

    /** Renders tabs visibly; an invisible tab difference is otherwise unreadable. */
    private static String visible(String s) {
        return s.replace("\t", "<TAB>");
    }

    private static String message(CompatCase c, Path root, String detail) {
        return "[" + c.id + "] (" + c.tier + ", " + c.mode + ") " + detail
                + "\n  query: " + c.query
                + "\n  fixtures retained at: " + (root == null ? "<none>" : root.toAbsolutePath());
    }

    private static void materialiseInputs(CompatCase c, Path root) {
        for (CompatInput in : c.inputs) {
            Path target = root.resolve(in.path);
            try {
                if (target.getParent() != null) {
                    Files.createDirectories(target.getParent());
                }
                String content = in.content != null
                        ? in.content
                        : Fixtures.readSharedData(in.contentFile, c.id);
                Files.writeString(target, content, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(
                        "Cannot write fixture " + in.path + " for case " + c.id, e);
            }
        }
    }

    static void deleteRecursively(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // Best effort. A leaked temp directory must never fail a run.
                }
            });
        } catch (IOException ignored) {
            // Best effort.
        }
    }
}
