package org.gorpipe.compat;

import gorsat.process.CLISessionFactory;
import gorsat.process.PipeInstance;
import gorsat.process.PipeOptions;
import org.gorpipe.gor.session.GorContext;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs one GOR query in-process, using the same option parsing and session
 * factory the command line uses.
 *
 * Engine failures are captured into the result rather than propagated, because
 * "this query fails with this message" is itself a behaviour the suite pins.
 */
public final class CompatExecutor {

    static {
        // Registers pipe commands, input sources and macros. Idempotent and
        // synchronized inside the engine.
        PipeInstance.initialize();
    }

    /** Directory under the case root that receives CREATE and PGOR results. */
    static final String CACHE_DIR = "result_cache";

    private CompatExecutor() {
    }

    public static CompatResult run(String query, Path projectRoot) {
        return run(query, projectRoot, null);
    }

    public static CompatResult run(String query, Path projectRoot, Path configFile) {
        String root = projectRoot.toAbsolutePath().toString();
        // CREATE and PGOR materialise intermediate results into the cache directory,
        // which otherwise defaults to java.io.tmpdir: shared by every case and every
        // run. Keeping it inside the case's own root means a case can never be
        // served a result another case or an earlier engine build produced.
        Path cachePath = projectRoot.resolve(CACHE_DIR);
        try {
            // The engine refuses a -cachedir that does not exist.
            Files.createDirectories(cachePath);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String cacheDir = cachePath.toAbsolutePath().toString();
        String[] args = configFile == null
                ? new String[]{query, "-gorroot", root, "-cachedir", cacheDir}
                : new String[]{query, "-gorroot", root, "-cachedir", cacheDir,
                               "-config", configFile.toAbsolutePath().toString()};

        try {
            PipeOptions options = new PipeOptions();
            options.parseOptions(args);

            try (PipeInstance pipe = new PipeInstance(
                    new GorContext(new CLISessionFactory(options, null).create()))) {
                pipe.subProcessArguments(options.query(), false, null, false, false, "");

                String header = pipe.getHeader();
                List<String> rows = new ArrayList<>();
                while (pipe.hasNext()) {
                    rows.add(pipe.next());
                }
                return CompatResult.ok(header, rows);
            }
        } catch (Throwable t) {
            // Throwable, not Exception: the engine raises bare runtime errors for
            // some malformed queries. One measured example is
            // "norrows 1 | where", which throws StringIndexOutOfBoundsException.
            return CompatResult.error(describe(t));
        }
    }

    private static String describe(Throwable t) {
        String message = t.getMessage();
        if (message == null || message.isEmpty()) {
            return t.getClass().getName();
        }
        return message;
    }
}
