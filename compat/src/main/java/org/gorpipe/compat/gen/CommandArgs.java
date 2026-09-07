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

    /** The primary fixture every case reads unless the command needs another shape. */
    public static final String DEFAULT_SOURCE = "${ROOT}/left.gor";

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
        CuratedYaml.requireUniqueTopLevelKeys(file);
        try (InputStream in = Files.newInputStream(file)) {
            Object raw = new Yaml().load(in);
            if (raw instanceof Map) {
                for (Map.Entry<String, Object> e : ((Map<String, Object>) raw).entrySet()) {
                    if (!(e.getValue() instanceof Map)) {
                        continue;
                    }
                    // Values are coerced to String because YAML types them for us:
                    // needsReference: true arrives as a Boolean, and reading it out
                    // of a Map<String, String> would fail at the cast.
                    Map<String, String> fields = new LinkedHashMap<>();
                    for (Map.Entry<Object, Object> f
                            : ((Map<Object, Object>) e.getValue()).entrySet()) {
                        fields.put(String.valueOf(f.getKey()), String.valueOf(f.getValue()));
                    }
                    parsed.put(e.getKey(), fields);
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
     * The positional argument to use in a NOR query, when it differs.
     *
     * Some commands take a bin size in GOR and none in NOR — the documentation
     * gives "GROUP binsize [attributes]" for gor and "GROUP [attributes]" for nor —
     * so the GOR positional would be read as an attribute there.
     */
    public String norPositional(String command) {
        Map<String, String> entry = entry(command);
        return entry.containsKey("norPositional")
                ? entry.get("norPositional")
                : positional(command);
    }

    /** Whether a NOR-specific positional was curated for this command. */
    public boolean hasNorPositional(String command) {
        return entry(command).containsKey("norPositional");
    }

    /**
     * Whether the case must run in a NOR query rather than a GOR one.
     *
     * RELREMOVE and TSVAPPEND reject a GOR query outright — "trying to execute
     * RELREMOVE in a gor query" — so a generated case for them could only ever
     * record that refusal.
     */
    public boolean nor(String command) {
        return Boolean.parseBoolean(entry(command).getOrDefault("nor", "false"));
    }

    /**
     * Whether the case needs the synthetic chromSeq reference build written into
     * its project root.
     *
     * VERIFYVARIANT and its relatives read the reference to check a variant against
     * it, and report "Ref does not match the build" without one. The harness has
     * been able to write a reference build since the fixtures went in, but no
     * generator asked for one.
     */
    public boolean needsReference(String command) {
        return Boolean.parseBoolean(entry(command).getOrDefault("needsReference", "false"));
    }

    /**
     * The file the case should read, for a command that needs a particular shape.
     *
     * BASES and CIGARSEGS require a CIGAR column and BAMFLAG a Flag column, none of
     * which the primary fixture carries, so every generated case for them failed on
     * the input rather than on the flag under test.
     */
    public String source(String command) {
        return entry(command).getOrDefault("source", DEFAULT_SOURCE);
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
