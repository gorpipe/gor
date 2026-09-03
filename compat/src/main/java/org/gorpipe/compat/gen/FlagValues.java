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
 * Curated sample values for value-taking flags.
 *
 * The registry declares that a flag takes a value but not what kind, so the
 * generator cannot invent one. An unmapped flag is reported as a gap rather than
 * turned into a case that fails for the wrong reason.
 */
public final class FlagValues {

    private static final String WILDCARD = "*";

    private final Map<String, Map<String, String>> byCommand;

    private FlagValues(Map<String, Map<String, String>> byCommand) {
        this.byCommand = byCommand;
    }

    public static FlagValues load() {
        Path file = CaseLoader.moduleRoot().resolve("inventory/flag-values.yml");
        return loadFrom(file);
    }

    @SuppressWarnings("unchecked")
    public static FlagValues loadFrom(Path file) {
        Map<String, Map<String, String>> parsed = new LinkedHashMap<>();
        if (!Files.exists(file)) {
            return new FlagValues(parsed);
        }
        CuratedYaml.requireUniqueTopLevelKeys(file);
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
        return new FlagValues(parsed);
    }

    public String valueFor(String command, String flag) {
        Map<String, String> exact = byCommand.getOrDefault(command, Collections.emptyMap());
        if (exact.containsKey(flag)) {
            return exact.get(flag);
        }
        return byCommand.getOrDefault(WILDCARD, Collections.emptyMap()).get(flag);
    }

    public boolean has(String command, String flag) {
        return valueFor(command, flag) != null;
    }
}
