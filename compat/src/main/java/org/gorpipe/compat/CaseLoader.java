package org.gorpipe.compat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Loads every case file under the module's cases/ directory, schema-validates
 * each record, and merges deterministically by sorted path.
 *
 * Any violation aborts the entire load. A partially-valid corpus must never run,
 * because a case that silently failed to load is coverage the report would claim
 * but not have.
 */
public final class CaseLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CaseLoader() {
    }

    /**
     * The :compat module directory. Tests run with the module as working
     * directory under Gradle; the fallback covers being run from the repo root.
     */
    public static Path moduleRoot() {
        Path cwd = Paths.get("").toAbsolutePath();
        if (Files.isDirectory(cwd.resolve("cases"))) {
            return cwd;
        }
        Path nested = cwd.resolve("compat");
        if (Files.isDirectory(nested.resolve("cases"))) {
            return nested;
        }
        throw new IllegalStateException(
                "Cannot locate the :compat module directory from " + cwd
                        + " — expected a cases/ subdirectory here or under compat/");
    }

    public static List<CompatCase> loadAll() {
        return loadFrom(moduleRoot().resolve("cases"));
    }

    public static List<CompatCase> loadFrom(Path casesDir) {
        JsonSchema schema = loadSchema();
        List<CompatCase> all = new ArrayList<>();
        Map<String, String> seenIds = new HashMap<>();

        for (Path file : caseFiles(casesDir)) {
            for (Map<String, Object> raw : readYaml(file)) {
                validate(schema, raw, file);

                CompatCase c = MAPPER.convertValue(raw, CompatCase.class);
                c.sourceFile = file.toAbsolutePath().toString();

                String previous = seenIds.put(c.id, c.sourceFile);
                if (previous != null) {
                    throw new IllegalStateException("Duplicate case id '" + c.id
                            + "' in " + c.sourceFile + " and " + previous);
                }
                all.add(c);
            }
        }
        return all;
    }

    private static JsonSchema loadSchema() {
        Path schemaPath = moduleRoot().resolve("schema/gor-compat-case.schema.json");
        try (InputStream in = Files.newInputStream(schemaPath)) {
            JsonNode node = MAPPER.readTree(in);
            return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(node);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read schema at " + schemaPath, e);
        }
    }

    private static void validate(JsonSchema schema, Map<String, Object> raw, Path file) {
        Set<ValidationMessage> violations = schema.validate(MAPPER.valueToTree(raw));
        if (!violations.isEmpty()) {
            String detail = violations.stream()
                    .map(ValidationMessage::getMessage)
                    .collect(Collectors.joining("; "));
            throw new IllegalStateException("Schema violation in " + file
                    + " for case '" + raw.get("id") + "': " + detail);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> readYaml(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            Object parsed = new Yaml().load(in);
            if (parsed == null) {
                return Collections.emptyList();
            }
            if (!(parsed instanceof List)) {
                throw new IllegalStateException(
                        "Case file must contain a YAML list of cases: " + file);
            }
            return (List<Map<String, Object>>) parsed;
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read case file " + file, e);
        }
    }

    private static List<Path> caseFiles(Path casesDir) {
        if (!Files.isDirectory(casesDir)) {
            return Collections.emptyList();
        }
        try (Stream<Path> walk = Files.walk(casesDir)) {
            return walk.filter(p -> p.toString().endsWith(".yml"))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot enumerate case files under " + casesDir, e);
        }
    }
}
