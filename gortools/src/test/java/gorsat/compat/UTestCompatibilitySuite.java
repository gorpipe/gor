package gorsat.compat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import gorsat.TestUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

@RunWith(Parameterized.class)
public class UTestCompatibilitySuite {
    private static final ObjectMapper SCHEMA_MAPPER = new ObjectMapper();

    @Parameterized.Parameter
    public CompatCase compatCase;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() throws IOException {
        JsonSchema schema = loadSchema();
        List<CompatCase> loadedCases = loadCases(schema);
        if (Boolean.getBoolean("compat.generateManifests")) {
            writeManifests(loadedCases);
        }
        List<Object[]> result = new ArrayList<>();
        for (CompatCase c : loadedCases) {
            result.add(new Object[]{c});
        }
        return result;
    }

    @Test
    public void runCompatibilityCase() throws IOException {
        Path root = Files.createTempDirectory("gor-compat-");
        try {
            for (CompatInputFile inputFile : compatCase.inputs) {
                Path filePath = root.resolve(inputFile.path);
                if (filePath.getParent() != null) {
                    Files.createDirectories(filePath.getParent());
                }
                Files.writeString(filePath, resolveInputContent(inputFile), StandardCharsets.UTF_8);
            }

            String query = compatCase.query.replace("${ROOT}", root.toAbsolutePath().toString());

            if ("error".equals(compatCase.mode)) {
                try {
                    TestUtils.runGorPipe(query);
                    Assert.fail("Expected query to fail: " + compatCase.id);
                } catch (Exception e) {
                    if (compatCase.errorContains != null && !compatCase.errorContains.isEmpty()) {
                        String message = e.getMessage() == null ? "" : e.getMessage();
                        Assert.assertTrue("Expected error to contain '" + compatCase.errorContains + "' but was '" + message + "'", message.contains(compatCase.errorContains));
                    }
                }
                return;
            }

            String actual = TestUtils.runGorPipe(query);
            String expected = resolveExpected(compatCase);
            if ("exact".equals(compatCase.mode)) {
                Assert.assertEquals(normalize(expected), normalize(actual));
            } else if ("contains".equals(compatCase.mode)) {
                Assert.assertTrue("Expected output to contain '" + expected + "' but output was '" + actual + "'", normalize(actual).contains(normalize(expected)));
            } else if ("regex".equals(compatCase.mode)) {
                Assert.assertTrue("Expected output to match regex '" + expected + "' but output was '" + actual + "'", Pattern.compile(normalize(expected), Pattern.DOTALL).matcher(normalize(actual)).find());
            } else {
                Assert.fail("Unsupported mode: " + compatCase.mode);
            }
        } finally {
            deleteRecursively(root);
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        Files.walk(root)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private static void writeManifests(List<CompatCase> cases) throws IOException {
        String manifestDir = System.getProperty("compat.manifestDir", Path.of("gortools", "build", "compat", "manifests").toString());
        Path manifestRoot = Path.of(manifestDir);
        Files.createDirectories(manifestRoot);

        Map<String, List<String>> docsToCases = new TreeMap<>();
        Map<String, List<String>> junitToCases = new TreeMap<>();
        Map<String, List<String>> featureScopeToCases = new TreeMap<>();

        for (CompatCase c : cases) {
            for (String doc : c.provenanceDocs) {
                docsToCases.computeIfAbsent(doc, k -> new ArrayList<>()).add(c.id);
            }
            for (String junit : c.provenanceJunit) {
                junitToCases.computeIfAbsent(junit, k -> new ArrayList<>()).add(c.id);
            }
            String scopeKey = c.category + "/" + c.feature;
            featureScopeToCases.computeIfAbsent(scopeKey, k -> new ArrayList<>()).add(c.id);
        }

        writeManifestYaml(manifestRoot.resolve("docs-to-cases.yaml"), docsToCases);
        writeManifestYaml(manifestRoot.resolve("junit-to-cases.yaml"), junitToCases);
        writeManifestYaml(manifestRoot.resolve("feature-scope-to-cases.yaml"), featureScopeToCases);
    }

    private static void writeManifestYaml(Path path, Map<String, List<String>> mapping) throws IOException {
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : mapping.entrySet()) {
            out.append("- source: \"").append(escapeYaml(entry.getKey())).append("\"\n");
            out.append("  cases:\n");
            for (String caseId : entry.getValue()) {
                out.append("    - \"").append(escapeYaml(caseId)).append("\"\n");
            }
        }
        Files.writeString(path, out.toString(), StandardCharsets.UTF_8);
    }

    private static String escapeYaml(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    static JsonSchema loadSchema() throws IOException {
        try (InputStream schemaIn = UTestCompatibilitySuite.class.getClassLoader().getResourceAsStream("compat/schema/gor-compat-case.schema.json")) {
            if (schemaIn == null) {
                throw new IOException("Missing compatibility case schema resource: compat/schema/gor-compat-case.schema.json");
            }
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
            return factory.getSchema(schemaIn);
        }
    }

    private static void validateAgainstSchema(JsonSchema schema, Map<String, Object> rawCase, String caseId) {
        JsonNode node = SCHEMA_MAPPER.valueToTree(rawCase);
        Set<ValidationMessage> messages = schema.validate(node);
        if (messages.isEmpty()) {
            return;
        }
        Set<String> sorted = new TreeSet<>();
        for (ValidationMessage m : messages) {
            sorted.add(m.getMessage());
        }
        throw new IllegalArgumentException("Schema violation in case '" + caseId + "': " + String.join("; ", sorted));
    }

    private static String resolveExpected(CompatCase c) throws IOException {
        if (c.expected != null && !c.expected.isEmpty()) {
            return c.expected;
        }
        if (c.expectedFile != null && !c.expectedFile.isEmpty()) {
            InputStream expectedIn = UTestCompatibilitySuite.class.getClassLoader().getResourceAsStream("compat/" + c.expectedFile);
            if (expectedIn == null) {
                throw new IOException("Missing expectedFile resource: compat/" + c.expectedFile + " for " + c.id);
            }
            try (InputStream in = expectedIn) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private static String resolveInputContent(CompatInputFile inputFile) throws IOException {
        if (inputFile.content != null) {
            return inputFile.content;
        }
        if (inputFile.contentFile != null && !inputFile.contentFile.isEmpty()) {
            InputStream inputIn = UTestCompatibilitySuite.class.getClassLoader().getResourceAsStream("compat/" + inputFile.contentFile);
            if (inputIn == null) {
                throw new IOException("Missing input contentFile resource: compat/" + inputFile.contentFile);
            }
            try (InputStream in = inputIn) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        return "";
    }

    static List<CompatCase> loadCases(JsonSchema schema) throws IOException {
        Yaml yaml = new Yaml();
        try (InputStream in = UTestCompatibilitySuite.class.getClassLoader().getResourceAsStream("compat/cases/compatibility-cases.yml")) {
            if (in == null) {
                throw new IOException("Missing compatibility cases resource");
            }
            Object loaded = yaml.load(in);
            if (!(loaded instanceof List)) {
                throw new IOException("Expected compatibility-cases.yml to contain a top-level list");
            }
            List<?> rawCases = (List<?>) loaded;
            List<CompatCase> cases = new ArrayList<>();
            Set<String> ids = new HashSet<>();
            for (Object rawCase : rawCases) {
                if (!(rawCase instanceof Map)) {
                    continue;
                }
                Map<String, Object> map = (Map<String, Object>) rawCase;
                String idForError = stringValue(map.get("id"));
                if (idForError == null) idForError = "<missing-id>";
                validateAgainstSchema(schema, map, idForError);
                CompatCase c = new CompatCase();
                c.id = stringValue(map.get("id"));
                c.query = stringValue(map.get("query"));
                c.mode = stringValue(map.get("mode"));
                c.expected = stringValue(map.get("expected"));
                c.expectedFile = stringValue(map.get("expectedFile"));
                c.errorContains = stringValue(map.get("errorContains"));
                c.category = stringValue(map.get("category"));
                c.feature = stringValue(map.get("feature"));
                c.behavior = stringValue(map.get("behavior"));
                c.provenanceDocs = stringListValue(map.get("docs"));
                c.provenanceJunit = stringListValue(map.get("junit"));
                populateDerivedMetadata(c);
                c.inputs = new ArrayList<>();
                Object rawInputs = map.get("inputs");
                if (rawInputs instanceof List) {
                    for (Object rawInput : (List<?>) rawInputs) {
                        if (!(rawInput instanceof Map)) {
                            continue;
                        }
                        Map<String, Object> inputMap = (Map<String, Object>) rawInput;
                        CompatInputFile inputFile = new CompatInputFile();
                        inputFile.path = stringValue(inputMap.get("path"));
                        inputFile.content = stringValue(inputMap.get("content"));
                        inputFile.contentFile = stringValue(inputMap.get("contentFile"));
                        c.inputs.add(inputFile);
                    }
                }
                validateCase(c, ids);
                cases.add(c);
            }
            return cases;
        }
    }

    private static void populateDerivedMetadata(CompatCase c) {
        if (c.category == null || c.category.isEmpty()) {
            int dotIndex = c.id == null ? -1 : c.id.indexOf('.');
            c.category = dotIndex > 0 ? c.id.substring(0, dotIndex) : "uncategorized";
        }
        if (c.feature == null || c.feature.isEmpty()) {
            c.feature = c.category;
        }
        if (c.behavior == null || c.behavior.isEmpty()) {
            c.behavior = c.id;
        }
        if (Boolean.getBoolean("compat.strictMetadata")) {
            return;
        }
        if (c.provenanceDocs.isEmpty()) {
            c.provenanceDocs.add("compat/generated");
        }
        if (c.provenanceJunit.isEmpty()) {
            c.provenanceJunit.add("compat/generated");
        }
    }

    private static void validateCase(CompatCase c, Set<String> ids) {
        if (!ids.add(c.id)) {
            throw new IllegalArgumentException("Duplicate case id: " + c.id);
        }
        if (c.inputs == null) {
            c.inputs = new ArrayList<>();
        }
        if (Boolean.getBoolean("compat.strictMetadata")) {
            if (c.category == null || c.category.isEmpty()) {
                throw new IllegalArgumentException("Case category is required: " + c.id);
            }
            if (c.feature == null || c.feature.isEmpty()) {
                throw new IllegalArgumentException("Case feature is required: " + c.id);
            }
            if (c.behavior == null || c.behavior.isEmpty()) {
                throw new IllegalArgumentException("Case behavior is required: " + c.id);
            }
            if (c.provenanceDocs.isEmpty()) {
                throw new IllegalArgumentException("Case docs provenance is required: " + c.id);
            }
            if (c.provenanceJunit.isEmpty()) {
                throw new IllegalArgumentException("Case junit provenance is required: " + c.id);
            }
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\\t", "\t").replace("\r\n", "\n").stripTrailing();
    }

    private static List<String> stringListValue(Object value) {
        if (!(value instanceof List)) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        for (Object item : (List<?>) value) {
            if (item != null) {
                String asString = String.valueOf(item);
                if (!asString.isEmpty()) {
                    result.add(asString);
                }
            }
        }
        return result;
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    public static class CompatCase {
        String id;
        String query;
        String mode;
        String expected;
        String expectedFile;
        String errorContains;
        String category;
        String feature;
        String behavior;
        List<String> provenanceDocs = new ArrayList<>();
        List<String> provenanceJunit = new ArrayList<>();
        List<CompatInputFile> inputs = new ArrayList<>();

        @Override
        public String toString() {
            return id;
        }
    }

    public static class CompatInputFile {
        String path;
        String content;
        String contentFile;
    }
}
