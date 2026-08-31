package org.gorpipe.compat;

import gorsat.process.CLISessionFactory;
import gorsat.process.PipeInstance;
import gorsat.process.PipeOptions;
import org.gorpipe.gor.session.GorContext;

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

    private CompatExecutor() {
    }

    public static CompatResult run(String query, Path projectRoot) {
        return run(query, projectRoot, null);
    }

    public static CompatResult run(String query, Path projectRoot, Path configFile) {
        String[] args = configFile == null
                ? new String[]{query, "-gorroot", projectRoot.toAbsolutePath().toString()}
                : new String[]{query, "-gorroot", projectRoot.toAbsolutePath().toString(),
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
