package gorsat.compat;

import com.networknt.schema.JsonSchema;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class UTestCompatibilitySuiteLint {

    private static final int MAX_PIPE_STEPS_DEFAULT = 3;
    private static final Set<String> CATEGORY_PIPE_EXEMPT = Set.of("script", "bulk");

    private static boolean strictLint() {
        return Boolean.getBoolean("compat.strictMetadata");
    }

    @Test
    public void singleBehaviorPipeCount() throws IOException {
        org.junit.Assume.assumeTrue("Lint disabled. Enable with -Dcompat.strictMetadata=true", strictLint());
        List<UTestCompatibilitySuite.CompatCase> cases = loadAllCases();
        List<String> violations = new ArrayList<>();
        for (UTestCompatibilitySuite.CompatCase c : cases) {
            if (CATEGORY_PIPE_EXEMPT.contains(c.category)) {
                continue;
            }
            if ("pipeline".equalsIgnoreCase(c.behavior)) {
                continue;
            }
            int pipes = countPipeSteps(c.query);
            if (pipes > MAX_PIPE_STEPS_DEFAULT) {
                violations.add(c.id + " has " + pipes + " pipe steps (max " + MAX_PIPE_STEPS_DEFAULT
                        + " unless category is " + CATEGORY_PIPE_EXEMPT + " or behavior is 'pipeline')");
            }
        }
        if (!violations.isEmpty()) {
            Assert.fail("One-behavior lint failed:\n  " + String.join("\n  ", violations));
        }
    }

    @Test
    public void noDuplicateBodies() throws IOException {
        org.junit.Assume.assumeTrue("Lint disabled. Enable with -Dcompat.strictMetadata=true", strictLint());
        List<UTestCompatibilitySuite.CompatCase> cases = loadAllCases();
        Map<String, List<String>> bodies = new LinkedHashMap<>();
        for (UTestCompatibilitySuite.CompatCase c : cases) {
            String key = bodyKey(c);
            bodies.computeIfAbsent(key, k -> new ArrayList<>()).add(c.id);
        }
        List<String> dups = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : bodies.entrySet()) {
            if (e.getValue().size() > 1) {
                dups.add("identical (query,mode,expected,inputs) across: " + e.getValue());
            }
        }
        if (!dups.isEmpty()) {
            Assert.fail("Duplicate-body lint failed:\n  " + String.join("\n  ", dups));
        }
    }

    @Test
    public void inputsReferencedByQuery() throws IOException {
        org.junit.Assume.assumeTrue("Lint disabled. Enable with -Dcompat.strictMetadata=true", strictLint());
        List<UTestCompatibilitySuite.CompatCase> cases = loadAllCases();
        List<String> violations = new ArrayList<>();
        for (UTestCompatibilitySuite.CompatCase c : cases) {
            for (UTestCompatibilitySuite.CompatInputFile in : c.inputs) {
                String token = "${ROOT}/" + in.path;
                if (!c.query.contains(token)) {
                    violations.add(c.id + " input '" + in.path + "' is not referenced as '" + token + "' in query: " + c.query);
                }
            }
        }
        if (!violations.isEmpty()) {
            Assert.fail("Input-reference lint failed:\n  " + String.join("\n  ", violations));
        }
    }

    private static List<UTestCompatibilitySuite.CompatCase> loadAllCases() throws IOException {
        JsonSchema schema = UTestCompatibilitySuite.loadSchema();
        return UTestCompatibilitySuite.loadCases(schema);
    }

    private static int countPipeSteps(String query) {
        if (query == null || query.isEmpty()) {
            return 0;
        }
        int pipes = 0;
        boolean inSingle = false;
        boolean inDouble = false;
        boolean inSubquery = false;
        for (int i = 0; i < query.length(); i++) {
            char ch = query.charAt(i);
            if (ch == '\'' && !inDouble) {
                inSingle = !inSingle;
            } else if (ch == '"' && !inSingle) {
                inDouble = !inDouble;
            } else if (!inSingle && !inDouble) {
                if (ch == '(') {
                    inSubquery = true;
                } else if (ch == ')') {
                    inSubquery = false;
                } else if (ch == '|' && !inSubquery) {
                    pipes++;
                }
            }
        }
        return pipes;
    }

    private static String bodyKey(UTestCompatibilitySuite.CompatCase c) {
        StringBuilder sb = new StringBuilder();
        sb.append(c.mode).append("");
        sb.append(c.query).append("");
        sb.append(c.expected == null ? "" : c.expected).append("");
        sb.append(c.expectedFile == null ? "" : c.expectedFile).append("");
        sb.append(c.errorContains == null ? "" : c.errorContains).append("");
        Map<String, String> sortedInputs = new HashMap<>();
        for (UTestCompatibilitySuite.CompatInputFile in : c.inputs) {
            String value = in.content != null ? "C:" + in.content
                    : in.contentFile != null ? "F:" + in.contentFile
                    : "";
            sortedInputs.put(in.path, value);
        }
        Set<String> keys = new TreeSet<>(sortedInputs.keySet());
        for (String k : keys) {
            sb.append(k).append("=").append(sortedInputs.get(k)).append("");
        }
        return sb.toString();
    }
}
