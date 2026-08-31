package org.gorpipe.compat.gen;

import org.gorpipe.compat.CaseLoader;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Curated per-command invocation details.
 *
 * The registry records how many positional arguments a command takes but not what
 * they are — JOIN wants a right-hand source, GROUP wants a bin size — and does not
 * record that some commands reject any invocation lacking a particular flag. Both
 * are supplied here rather than guessed, for the same reason as {@link FlagValues}:
 * a generated case that fails on a missing companion argument records nothing
 * about the flag it was meant to exercise.
 */
public final class CommandArgs {

    /** The canonical right-hand source, used when a command names no other. */
    public static final String DEFAULT_POSITIONAL = "${ROOT}/right.gor";

    private final Map<String, Map<String, String>> byCommand;

    private CommandArgs(Map<String, Map<String, String>> byCommand) {
        this.byCommand = byCommand;
    }

    public static CommandArgs load() {
        return loadFrom(CaseLoader.moduleRoot().resolve("inventory/command-args.yml"));
    }

    @SuppressWarnings("unchecked")
    public static CommandArgs loadFrom(Path file) {
        Map<String, Map<String, String>> parsed = new LinkedHashMap<>();
        if (!Files.exists(file)) {
            return new CommandArgs(parsed);
        }
        try (InputStream in = Files.newInputStream(file)) {
            Object raw = new Yaml().load(in);
            if (raw instanceof Map) {
                for (Map.Entry<String, Object> e : ((Map<String, Object>) raw).entrySet()) {
                    if (e.getValue() instanceof Map) {
                        parsed.put(e.getKey(), (Map<String, String>) e.getValue());
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + file, e);
        }
        return new CommandArgs(parsed);
    }

    /** Flags this command needs regardless of the flag under test; "" when none. */
    public String requiredFlags(String command) {
        return entry(command).getOrDefault("requiredFlags", "");
    }

    /** The positional argument to supply when the command declares a minimum. */
    public String positional(String command) {
        return entry(command).getOrDefault("positional", DEFAULT_POSITIONAL);
    }

    /**
     * Whether a positional argument was curated for this command explicitly.
     *
     * Declared arity is not reliable on its own: GROUP and GRANNO report minArgs 0
     * and still reject an invocation with no bin size, so an explicit entry wins
     * over the registry.
     */
    public boolean hasExplicitPositional(String command) {
        return entry(command).containsKey("positional");
    }

    private Map<String, String> entry(String command) {
        return byCommand.getOrDefault(command, Collections.emptyMap());
    }
}
