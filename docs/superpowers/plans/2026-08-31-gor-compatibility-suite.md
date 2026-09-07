# GOR Compatibility Test Suite Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a black-box GOR compatibility suite in its own Gradle module, with a hand-authored SPEC tier that gates on correctness and a machine-generated BASELINE tier that gates on unaccepted behaviour change.

**Architecture:** A new `:compat` module depends on `:gortools` only. `CompatExecutor` drives `PipeInstance` through `CLISessionFactory` inside a per-case temp project root. SPEC cases carry hand-authored `expected` inline and fail the build on mismatch. BASELINE cases are generated from the runtime command/flag registry, from documentation snippets, and (nightly) from coverage-guided mutation and grammar fuzzing; their outputs live in committed `.out` files and any unaccepted diff fails the build.

**Tech Stack:** Java 17, JUnit 4 (`Parameterized`), Gradle (Groovy DSL, refreshVersions), SnakeYAML 2.6, Jackson Databind 2.21.2, `com.networknt:json-schema-validator` (new dependency), JaCoCo (report-only).

**Spec:** `docs/superpowers/specs/2026-08-29-gor-compatibility-suite-design.md`

## Global Constraints

- Java 17, Scala 2.13. Match the repo; introduce no new language levels.
- JUnit 4 only. The repo does not use JUnit 5; `Parameterized` is the runner.
- `:compat` depends on `:gortools` and NOTHING else from this repo. It must never depend on `:test` (which holds `gorsat.TestUtils`). Nothing depends on `:compat`.
- Test JVM runs with `-Duser.timezone=UTC` and `-Dfile.encoding=UTF-8`.
- Output serialisation contract: header line, then one line per row, tab-separated, `\n`-joined, trailing newline.
- Case id pattern: `^[a-z0-9]+(\.[a-z0-9_]+)+$`.
- `tier` values are exactly `spec` and `baseline`. `mode` values are exactly `exact` and `error`. There is no `contains` mode and no `regex` mode.
- A `spec` case MUST have a non-empty `cites` array. A `baseline` case MUST NOT have an `expected` field.
- JSON Schema Draft 2020-12 with `additionalProperties: false`.
- JaCoCo never fails a build in this module.
- Dependency versions use the refreshVersions placeholder `_` in `build.gradle`, with the concrete version pinned in `versions.properties` as `version.<group>..<artifact>=<version>`.
- Commit after every task. Conventional commits: `<prefix>(compat): <description>`.
- Do not modify engine code except in Task 9, which is the single permitted engine change.
- Do not commit anything under `docs/superpowers/`.

## Verified Facts

Every value below was measured on this tree at plan time. Do not re-derive them; do
verify them if a test disagrees.

**Engine API (exact idiom, mirrors `test/src/main/java/gorsat/TestUtils.java:117-133`):**

```java
PipeOptions options = new PipeOptions();
options.parseOptions(new String[]{query, "-gorroot", root});
try (PipeInstance pipe = new PipeInstance(new GorContext(new CLISessionFactory(options, null).create()))) {
    pipe.subProcessArguments(options.query(), false, null, false, false, "");
    String header = pipe.getHeader();
    while (pipe.hasNext()) { String row = pipe.next(); }
}
```

- `PipeInstance.initialize()` registers all three registries (commands, input sources, macros). Call it once before any query.
- `PipeInstance` is `AutoCloseable`.
- `PipeOptions` fields are accessed as methods from Java: `options.query()`.

**In-JVM serialisation, measured. NOTE: this differs from CLI output — the CLI
strips `ChromNOR`/`PosNOR` for NOR queries and prefixes the header with `#`. The
suite pins the in-JVM shape below.**

| Query | Header | Rows |
|---|---|---|
| `norrows 1 \| calc X 1+1` | `ChromNOR	PosNOR	RowNum	X` | `chrN	0	0	2` |
| `norrows 2` | `ChromNOR	PosNOR	RowNum` | `chrN	0	0` / `chrN	0	1` |
| `gorrow chr1,100 \| calc Ref 'A'` | `chrom	pos	Ref` | `chr1	100	A` |

**Error behaviour, measured:**

| Query | Exception | Message |
|---|---|---|
| `norrows 1 \| where` | `java.lang.StringIndexOutOfBoundsException` | `Range [6, 5) out of bounds for length 5` |
| `norrows 1 \| nosuchcommand` | `org.gorpipe.exceptions.GorParsingException` | `Error in command - NOSUCHCOMMAND not found in white listed commands:` |

**Registry, measured via `GorPipeCommands.commandMap()`:**

- 108 commands.
- 217 valueless flags, 242 value-taking flags, 459 total.
- `JOIN` options: `-snpsnp -snpseg -segseg -segsnp -varseg -segvar -stdin -r -l -i -ic -ir -t -c -n -m -xcis`
- `JOIN` valueOptions: `-s -p -f -e -o -lstop -rstop -xl -xr -maxseg -rprefix -ref -refl -refr`
- `JOIN` minimumNumberOfArguments = 1, maximumNumberOfArguments = 1.

**Performance, measured:** 8 ms per case in-JVM (20-case average, warmed). 10,000
cases is therefore roughly 80 seconds single-threaded. The in-JVM model holds.

**Synthetic chromSeq reference build works.** `refbase(chrom,pos)` at `chr1:10`
against a `ACGT`-repeat sequence returns `C` (offset 9). Two details that are easy
to get wrong:

- `buildsize.gor` and `buildsplit.txt` MUST have **no header line** (see
  `scripts/refbuild/fasta_to_chromseq.py:170`, "no header (matches gor's parser)").
  A header produces `NumberFormatException: For input string: "Size"`.
- The config is passed to the engine with `-config <path>`.

**The existing suite does not compile.** `gortools/src/test/java/gorsat/compat/`
imports `com.networknt.schema.*`, which is on no configuration in the build.
`./gradlew :gortools:compileTestJava` fails with `package com.networknt.schema does
not exist`. This is true on `main`. Consequently the whole `:gortools` test compile
is broken, the 1087-case corpus has never run, and there is no compatibility
coverage to preserve. Task 1 therefore removes it first, which the spec's §12
ordering assumed would come last.

---

### Task 1: Unblock the `:gortools` test compile by removing the dead compat suite

The existing suite cannot compile, so `./gradlew test` cannot run at all. Nothing
downstream in this plan can be verified until that is fixed. Removing it first is
safe precisely because it has never provided coverage.

**Files:**
- Delete: `gortools/src/test/java/gorsat/compat/UTestCompatibilitySuite.java`
- Delete: `gortools/src/test/java/gorsat/compat/UTestCompatibilitySuiteLint.java`
- Delete: `gortools/src/test/resources/compat/` (whole tree)
- Delete: `scripts/audit_compat_cases.py`
- Delete: `scripts/backfill_compat_provenance.py`
- Delete: `scripts/cleanup_semantic.py`
- Delete: `scripts/rename_bulk_cases.py`

**Interfaces:**
- Consumes: nothing
- Produces: a `:gortools` test source set that compiles

- [x] **Step 1: Confirm the breakage before touching anything**

```bash
cd /Users/gisli/work/gor-opensource-third
./gradlew :gortools:compileTestJava 2>&1 | grep -c "package com.networknt.schema does not exist"
```

Expected: a non-zero count. If this prints `0`, the situation has changed — stop
and re-read the current state before deleting anything.

- [x] **Step 2: Record what is being removed**

```bash
grep -c "^- id:" gortools/src/test/resources/compat/cases/compatibility-cases.yml
wc -l gortools/src/test/java/gorsat/compat/*.java
```

Expected: 1087 cases, 501 lines of harness. Note these in the commit message so the
history records the size of what was dropped.

- [x] **Step 3: Delete the harness, corpus and one-off scripts**

```bash
git rm -r gortools/src/test/java/gorsat/compat/
git rm -r gortools/src/test/resources/compat/
git rm scripts/audit_compat_cases.py \
       scripts/backfill_compat_provenance.py \
       scripts/cleanup_semantic.py \
       scripts/rename_bulk_cases.py
```

- [x] **Step 4: Verify the test compile is restored**

```bash
./gradlew :gortools:compileTestJava
```

Expected: BUILD SUCCESSFUL. If other unrelated compile errors surface, they are
pre-existing and out of scope for this task — report them rather than fixing them
here.

- [x] **Step 5: Commit**

```bash
git commit -m "fix(compat): remove non-compiling compatibility suite

The suite imported com.networknt.schema, which is on no configuration in the
build, so :gortools:compileTestJava has been failing and the 1087-case corpus
has never run. Removing it restores the gortools test compile. A replacement
suite lands in the new :compat module.

Removed: 1087 cases, 501 lines of harness, 4 one-off scripts."
```

---

### Task 2: `:compat` module skeleton with an enforced dependency boundary

Spec §5.1. The separation requirement is the reason this module exists, so it is
asserted by a test before any functionality is added.

**Files:**
- Modify: `settings.gradle`
- Modify: `versions.properties`
- Create: `compat/build.gradle`
- Create: `compat/src/test/java/org/gorpipe/compat/UTestModuleBoundary.java`

**Interfaces:**
- Consumes: nothing
- Produces: Gradle project `:compat` with a working `test` task

- [x] **Step 1: Register the module**

Add to `settings.gradle`, after the `include 'gorscripts'` line:

```groovy
include 'compat'
```

- [x] **Step 2: Pin the JSON schema validator version**

`networknt` is not currently in the build. refreshVersions requires an explicit
pin. Add to `versions.properties`, keeping the file's alphabetical grouping:

```properties
version.com.networknt..json-schema-validator=1.5.9
```

- [x] **Step 3: Write the module build file**

Create `compat/build.gradle`:

```groovy
plugins {
    id 'gor.java-common'
}

project(':compat') {
    dependencies {
        // The ONLY repo dependency. Never add ':test' — it holds gorsat.TestUtils
        // and would reintroduce the coupling this module exists to remove.
        implementation project(':gortools')

        implementation "org.yaml:snakeyaml:_"
        implementation "com.fasterxml.jackson.core:jackson-databind:_"
        implementation "com.networknt:json-schema-validator:_"

        runtimeOnly project(':drivers')

        testImplementation "junit:junit:_"
    }

    tasks.named('test') {
        useJUnit()
        systemProperty 'user.timezone', 'UTC'
        systemProperty 'file.encoding', 'UTF-8'
        testLogging {
            events 'failed'
            exceptionFormat 'full'
        }
        reports {
            junitXml.required = true
            html.required = true
        }
    }
}
```

- [x] **Step 4: Write the boundary test**

Create `compat/src/test/java/org/gorpipe/compat/UTestModuleBoundary.java`:

```java
package org.gorpipe.compat;

import org.junit.Assert;
import org.junit.Test;

/**
 * The compatibility suite exists to be independent of the unit test
 * infrastructure. That independence is asserted here rather than left to
 * convention, so that reintroducing the coupling fails a test.
 */
public class UTestModuleBoundary {

    @Test
    public void gortoolsIsOnTheClasspath() throws Exception {
        Assert.assertNotNull(Class.forName("gorsat.process.PipeInstance"));
        Assert.assertNotNull(Class.forName("gorsat.process.CLISessionFactory"));
    }

    @Test
    public void testInfrastructureIsNotOnTheClasspath() {
        try {
            Class.forName("gorsat.TestUtils");
            Assert.fail("gorsat.TestUtils must not be reachable from :compat — "
                    + "the :test dependency has leaked back in");
        } catch (ClassNotFoundException expected) {
            // this is the contract
        }
    }
}
```

- [x] **Step 5: Run and verify both tests pass**

```bash
./gradlew :compat:test --tests "org.gorpipe.compat.UTestModuleBoundary"
```

Expected: PASS, 2 tests.

If `testInfrastructureIsNotOnTheClasspath` fails, a transitive dependency is
pulling `:test` in. Find it with:

```bash
./gradlew :compat:dependencies --configuration testRuntimeClasspath | grep -i test
```

and add an explicit `exclude` for it rather than relaxing the assertion.

- [x] **Step 6: Commit**

```bash
git add settings.gradle versions.properties compat/
git commit -m "feat(compat): add :compat module with enforced dependency boundary"
```

---

### Task 3: CompatExecutor and the serialisation contract

Spec §5.2 and §5.3. This is the single place the engine is invoked, and the single
place output shape is defined.

**Files:**
- Create: `compat/src/main/java/org/gorpipe/compat/CompatResult.java`
- Create: `compat/src/main/java/org/gorpipe/compat/CompatExecutor.java`
- Test: `compat/src/test/java/org/gorpipe/compat/UTestCompatExecutor.java`

**Interfaces:**
- Consumes: `gorsat.process.PipeOptions`, `gorsat.process.PipeInstance`, `gorsat.process.CLISessionFactory`, `org.gorpipe.gor.session.GorContext`
- Produces:
  - `CompatResult` with public final fields `String header`, `List<String> rows`, `String errorMessage`; static factories `ok(String, List<String>)` and `error(String)`; methods `boolean failed()` and `String serialised()`
  - `CompatExecutor.run(String query, Path projectRoot) -> CompatResult`
  - `CompatExecutor.run(String query, Path projectRoot, Path configFile) -> CompatResult` (config overload, used by the reference-genome fixtures in Task 8)

- [x] **Step 1: Write the failing test**

The expected values below are measured from the engine, not invented. See Verified
Facts.

Create `compat/src/test/java/org/gorpipe/compat/UTestCompatExecutor.java`:

```java
package org.gorpipe.compat;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;

public class UTestCompatExecutor {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void runsNorQueryAndSerialisesOutput() {
        Path root = tmp.getRoot().toPath();
        CompatResult result = CompatExecutor.run("norrows 1 | calc X 1+1", root);

        Assert.assertFalse(result.errorMessage, result.failed());
        Assert.assertEquals("ChromNOR\tPosNOR\tRowNum\tX", result.header);
        Assert.assertEquals(1, result.rows.size());
        Assert.assertEquals("chrN\t0\t0\t2", result.rows.get(0));
        Assert.assertEquals("ChromNOR\tPosNOR\tRowNum\tX\nchrN\t0\t0\t2\n",
                result.serialised());
    }

    @Test
    public void runsMultiRowNorQuery() {
        Path root = tmp.getRoot().toPath();
        CompatResult result = CompatExecutor.run("norrows 2", root);

        Assert.assertFalse(result.errorMessage, result.failed());
        Assert.assertEquals("ChromNOR\tPosNOR\tRowNum", result.header);
        Assert.assertEquals(2, result.rows.size());
        Assert.assertEquals("chrN\t0\t0", result.rows.get(0));
        Assert.assertEquals("chrN\t0\t1", result.rows.get(1));
    }

    @Test
    public void runsGorRowQuery() {
        Path root = tmp.getRoot().toPath();
        CompatResult result = CompatExecutor.run("gorrow chr1,100 | calc Ref 'A'", root);

        Assert.assertFalse(result.errorMessage, result.failed());
        Assert.assertEquals("chrom\tpos\tRef", result.header);
        Assert.assertEquals("chr1\t100\tA", result.rows.get(0));
    }

    @Test
    public void capturesParsingErrorInsteadOfThrowing() {
        Path root = tmp.getRoot().toPath();
        CompatResult result = CompatExecutor.run("norrows 1 | nosuchcommand", root);

        Assert.assertTrue("expected the query to fail", result.failed());
        Assert.assertTrue("actual: " + result.errorMessage,
                result.errorMessage.contains("NOSUCHCOMMAND"));
    }

    @Test
    public void capturesRuntimeExceptionInsteadOfThrowing() {
        Path root = tmp.getRoot().toPath();
        CompatResult result = CompatExecutor.run("norrows 1 | where", root);

        Assert.assertTrue("expected the query to fail", result.failed());
        Assert.assertNotNull(result.errorMessage);
        Assert.assertFalse(result.errorMessage.isEmpty());
    }

    @Test
    public void serialisingAFailedResultIsProgrammerError() {
        CompatResult failed = CompatResult.error("boom");
        try {
            failed.serialised();
            Assert.fail("expected IllegalStateException");
        } catch (IllegalStateException expected) {
            Assert.assertTrue(expected.getMessage().contains("boom"));
        }
    }
}
```

- [x] **Step 2: Run to verify it fails**

```bash
./gradlew :compat:test --tests "org.gorpipe.compat.UTestCompatExecutor"
```

Expected: FAIL — `CompatResult` and `CompatExecutor` do not exist (compilation error).

- [x] **Step 3: Write CompatResult**

Create `compat/src/main/java/org/gorpipe/compat/CompatResult.java`:

```java
package org.gorpipe.compat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Outcome of running one compatibility query: either a successful
 * (header, rows) pair or a captured error message, never both.
 */
public final class CompatResult {

    public final String header;
    public final List<String> rows;
    public final String errorMessage;

    private CompatResult(String header, List<String> rows, String errorMessage) {
        this.header = header;
        this.rows = rows;
        this.errorMessage = errorMessage;
    }

    public static CompatResult ok(String header, List<String> rows) {
        return new CompatResult(header, Collections.unmodifiableList(new ArrayList<>(rows)), null);
    }

    public static CompatResult error(String message) {
        return new CompatResult(null, Collections.emptyList(), message == null ? "" : message);
    }

    public boolean failed() {
        return errorMessage != null;
    }

    /**
     * The output serialisation contract: header line, one line per row,
     * newline-joined, with a trailing newline.
     *
     * Defined here rather than inherited from test infrastructure so that
     * changing it is a deliberate, reviewable act. Note that this is the in-JVM
     * shape and it differs from CLI output for NOR queries: the CLI strips the
     * ChromNOR and PosNOR columns and prefixes the header with '#'.
     */
    public String serialised() {
        if (failed()) {
            throw new IllegalStateException("cannot serialise a failed result: " + errorMessage);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(header).append('\n');
        for (String row : rows) {
            sb.append(row).append('\n');
        }
        return sb.toString();
    }
}
```

- [x] **Step 4: Write CompatExecutor**

Create `compat/src/main/java/org/gorpipe/compat/CompatExecutor.java`:

```java
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
```

- [x] **Step 5: Run the test**

```bash
./gradlew :compat:test --tests "org.gorpipe.compat.UTestCompatExecutor"
```

Expected: PASS, 6 tests.

If a header assertion fails, print the actual value and reconcile. The assertions
encode measured engine output; if the engine now differs, that is itself a finding —
report it rather than silently rewriting the expectation.

- [x] **Step 6: Commit**

```bash
git add compat/src/main/java/org/gorpipe/compat/CompatResult.java \
        compat/src/main/java/org/gorpipe/compat/CompatExecutor.java \
        compat/src/test/java/org/gorpipe/compat/UTestCompatExecutor.java
git commit -m "feat(compat): add CompatExecutor and pin the output serialisation contract"
```

---

### Task 4: Case model, JSON schema and loader

Spec §6.1, §6.2 and §6.3. Cases live in the module directory rather than on the
test classpath, because they are reviewable data, not packaged resources.

**Files:**
- Create: `compat/src/main/java/org/gorpipe/compat/CompatInput.java`
- Create: `compat/src/main/java/org/gorpipe/compat/CompatCase.java`
- Create: `compat/src/main/java/org/gorpipe/compat/CaseLoader.java`
- Create: `compat/schema/gor-compat-case.schema.json`
- Create: `compat/cases/spec/cmd/calc.yml`
- Test: `compat/src/test/java/org/gorpipe/compat/UTestCaseLoader.java`

**Interfaces:**
- Consumes: SnakeYAML, Jackson, networknt validator
- Produces:
  - `CompatInput` with public fields `String path`, `String content`, `String contentFile`
  - `CompatCase` with public fields `String id, tier, mode, source, query, expected, errorContains, behavior, sourceFile`; `List<String> cites`; `List<CompatInput> inputs`; methods `String category()`, `String feature()`, `boolean isSpec()`, `boolean isBaseline()`
  - `CaseLoader.moduleRoot() -> Path`
  - `CaseLoader.loadAll() -> List<CompatCase>`
  - `CaseLoader.loadFrom(Path casesDir) -> List<CompatCase>`

- [x] **Step 1: Write the JSON schema**

Create `compat/schema/gor-compat-case.schema.json`:

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "additionalProperties": false,
  "required": ["id", "tier", "mode", "query"],
  "properties": {
    "id": { "type": "string", "pattern": "^[a-z0-9]+(\\.[a-z0-9_]+)+$" },
    "tier": { "enum": ["spec", "baseline"] },
    "mode": { "enum": ["exact", "error"] },
    "source": { "enum": ["docs", "review"] },
    "query": { "type": "string", "minLength": 1 },
    "expected": { "type": "string" },
    "errorContains": { "type": "string" },
    "behavior": { "type": "string" },
    "cites": { "type": "array", "items": { "type": "string" }, "minItems": 1 },
    "inputs": {
      "type": "array",
      "items": {
        "type": "object",
        "additionalProperties": false,
        "required": ["path"],
        "properties": {
          "path": { "type": "string" },
          "content": { "type": "string" },
          "contentFile": { "type": "string" }
        }
      }
    }
  },
  "allOf": [
    {
      "if": { "properties": { "tier": { "const": "spec" } }, "required": ["tier"] },
      "then": { "required": ["cites", "source"] }
    },
    {
      "if": { "properties": { "tier": { "const": "baseline" } }, "required": ["tier"] },
      "then": { "not": { "required": ["expected"] } }
    },
    {
      "if": {
        "properties": { "tier": { "const": "spec" }, "mode": { "const": "exact" } },
        "required": ["tier", "mode"]
      },
      "then": { "required": ["expected"] }
    }
  ]
}
```

- [x] **Step 2: Write the seed SPEC cases**

Create `compat/cases/spec/cmd/calc.yml`. The `expected` blocks MUST contain real
tab characters. Verify after writing with `grep -P '\t' compat/cases/spec/cmd/calc.yml`
— on macOS without GNU grep, use `python3 -c "print(open('compat/cases/spec/cmd/calc.yml').read().count(chr(9)))"`
and expect a non-zero count.

```yaml
- id: cmd.calc.add_two_integers
  tier: spec
  mode: exact
  source: review
  behavior: "CALC evaluates an integer addition into a new trailing column"
  cites: ["documentation/src/command/CALC.rst"]
  query: "norrows 1 | calc X 1+1"
  expected: |
    ChromNOR	PosNOR	RowNum	X
    chrN	0	0	2

- id: cmd.calc.string_literal
  tier: spec
  mode: exact
  source: review
  behavior: "CALC assigns a quoted string literal without altering position columns"
  cites: ["documentation/src/command/CALC.rst"]
  query: "gorrow chr1,100 | calc Ref 'A'"
  expected: |
    chrom	pos	Ref
    chr1	100	A

- id: cmd.calc.missing_expression_errors
  tier: spec
  mode: error
  source: review
  behavior: "CALC with a column name but no expression is rejected"
  cites: ["documentation/src/command/CALC.rst"]
  query: "norrows 1 | calc X"
```

- [x] **Step 3: Write the failing loader test**

Create `compat/src/test/java/org/gorpipe/compat/UTestCaseLoader.java`:

```java
package org.gorpipe.compat;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

public class UTestCaseLoader {

    private static CompatCase byId(String id) {
        return CaseLoader.loadAll().stream()
                .filter(c -> c.id.equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("case not loaded: " + id));
    }

    @Test
    public void loadsSeedSpecCases() {
        List<String> ids = CaseLoader.loadAll().stream()
                .map(c -> c.id).collect(Collectors.toList());

        Assert.assertTrue(ids.toString(), ids.contains("cmd.calc.add_two_integers"));
        Assert.assertTrue(ids.toString(), ids.contains("cmd.calc.string_literal"));
        Assert.assertTrue(ids.toString(), ids.contains("cmd.calc.missing_expression_errors"));
    }

    @Test
    public void derivesCategoryAndFeatureFromId() {
        CompatCase c = byId("cmd.calc.add_two_integers");
        Assert.assertEquals("cmd", c.category());
        Assert.assertEquals("calc", c.feature());
    }

    @Test
    public void classifiesTier() {
        CompatCase c = byId("cmd.calc.add_two_integers");
        Assert.assertTrue(c.isSpec());
        Assert.assertFalse(c.isBaseline());
    }

    @Test
    public void preservesTabsInExpectedBlocks() {
        CompatCase c = byId("cmd.calc.add_two_integers");
        Assert.assertTrue("expected block lost its tabs", c.expected.contains("\t"));
        Assert.assertEquals("ChromNOR\tPosNOR\tRowNum\tX\nchrN\t0\t0\t2\n", c.expected);
    }

    @Test
    public void recordsSourceFileForErrorMessages() {
        CompatCase c = byId("cmd.calc.add_two_integers");
        Assert.assertNotNull(c.sourceFile);
        Assert.assertTrue(c.sourceFile, c.sourceFile.endsWith("calc.yml"));
    }
}
```

- [x] **Step 4: Run to verify it fails**

```bash
./gradlew :compat:test --tests "org.gorpipe.compat.UTestCaseLoader"
```

Expected: FAIL — `CaseLoader`, `CompatCase` and `CompatInput` do not exist.

- [x] **Step 5: Write CompatInput and CompatCase**

Create `compat/src/main/java/org/gorpipe/compat/CompatInput.java`:

```java
package org.gorpipe.compat;

/** One fixture file materialised into a case's project root. */
public final class CompatInput {
    public String path;
    public String content;
    public String contentFile;
}
```

Create `compat/src/main/java/org/gorpipe/compat/CompatCase.java`:

```java
package org.gorpipe.compat;

import java.util.ArrayList;
import java.util.List;

/**
 * One compatibility case.
 *
 * A spec case is hand-authored: it carries its expected output inline and cites
 * where that expectation came from. A baseline case is generated: it carries no
 * expected output, because its output lives in a committed baseline file.
 */
public final class CompatCase {

    public String id;
    public String tier;
    public String mode;
    public String source;
    public String query;
    public String expected;
    public String errorContains;
    public String behavior;
    public List<String> cites = new ArrayList<>();
    public List<CompatInput> inputs = new ArrayList<>();

    /** Absolute path of the file this case was loaded from; used in messages. */
    public String sourceFile;

    public String category() {
        return id.split("\\.")[0];
    }

    public String feature() {
        String[] parts = id.split("\\.");
        return parts.length > 1 ? parts[1] : "";
    }

    public boolean isSpec() {
        return "spec".equals(tier);
    }

    public boolean isBaseline() {
        return "baseline".equals(tier);
    }

    @Override
    public String toString() {
        return id;
    }
}
```

- [x] **Step 6: Write CaseLoader**

Create `compat/src/main/java/org/gorpipe/compat/CaseLoader.java`:

```java
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
```

- [x] **Step 7: Run the test**

```bash
./gradlew :compat:test --tests "org.gorpipe.compat.UTestCaseLoader"
```

Expected: PASS, 5 tests.

If `preservesTabsInExpectedBlocks` fails, the YAML was written with spaces. Rewrite
the `expected` blocks with real tabs.

- [x] **Step 8: Commit**

```bash
git add compat/src/main/java/org/gorpipe/compat/CompatCase.java \
        compat/src/main/java/org/gorpipe/compat/CompatInput.java \
        compat/src/main/java/org/gorpipe/compat/CaseLoader.java \
        compat/schema/gor-compat-case.schema.json \
        compat/cases/spec/cmd/calc.yml \
        compat/src/test/java/org/gorpipe/compat/UTestCaseLoader.java
git commit -m "feat(compat): add case model, JSON schema and validating loader"
```

---

### Task 5: CaseRunner and the SPEC suite

Spec §5.4 and §6. Each case runs in a fresh temp project root. The directory is
retained on failure, because a golden mismatch without its fixtures cannot be
debugged.

**Files:**
- Create: `compat/src/main/java/org/gorpipe/compat/CaseRunner.java`
- Test: `compat/src/test/java/org/gorpipe/compat/UTestCaseRunner.java`
- Test: `compat/src/test/java/org/gorpipe/compat/UTestSpecSuite.java`

**Interfaces:**
- Consumes: `CompatCase`, `CompatInput`, `CompatExecutor`, `CompatResult`, `CaseLoader`
- Produces:
  - `CaseRunner.run(CompatCase c) -> CompatResult` — materialises fixtures, executes, deletes the temp root, returns the raw result without asserting
  - `CaseRunner.runRetainingOnFailure(CompatCase c) -> CaseOutcome` where `CaseOutcome` has public final fields `CompatResult result` and `Path retainedRoot` (null when nothing was retained)
  - `CaseRunner.assertSpec(CompatCase c) -> void` — throws `AssertionError` on mismatch

- [x] **Step 1: Write the failing runner test**

Create `compat/src/test/java/org/gorpipe/compat/UTestCaseRunner.java`:

```java
package org.gorpipe.compat;

import org.junit.Assert;
import org.junit.Test;

public class UTestCaseRunner {

    private static CompatCase specCase(String id, String mode, String query, String expected) {
        CompatCase c = new CompatCase();
        c.id = id;
        c.tier = "spec";
        c.mode = mode;
        c.source = "review";
        c.query = query;
        c.expected = expected;
        c.cites.add("documentation/src/command/CALC.rst");
        return c;
    }

    @Test
    public void exactMatchPasses() {
        CaseRunner.assertSpec(specCase("cmd.calc.t1", "exact", "norrows 1 | calc X 1+1",
                "ChromNOR\tPosNOR\tRowNum\tX\nchrN\t0\t0\t2\n"));
    }

    @Test
    public void exactMismatchFailsAndNamesTheCase() {
        CompatCase c = specCase("cmd.calc.t2", "exact", "norrows 1 | calc X 1+1",
                "ChromNOR\tPosNOR\tRowNum\tX\nchrN\t0\t0\t999\n");
        try {
            CaseRunner.assertSpec(c);
            Assert.fail("expected an AssertionError for a wrong golden");
        } catch (AssertionError expected) {
            Assert.assertTrue(expected.getMessage(), expected.getMessage().contains("cmd.calc.t2"));
            Assert.assertTrue("failure must report the retained fixture directory",
                    expected.getMessage().contains("fixtures retained at"));
        }
    }

    @Test
    public void errorModePassesWhenQueryFails() {
        CaseRunner.assertSpec(specCase("cmd.calc.t3", "error", "norrows 1 | calc X", null));
    }

    @Test
    public void errorModeFailsWhenQuerySucceeds() {
        CompatCase c = specCase("cmd.calc.t4", "error", "norrows 1", null);
        try {
            CaseRunner.assertSpec(c);
            Assert.fail("expected an AssertionError — the query did not fail");
        } catch (AssertionError expected) {
            Assert.assertTrue(expected.getMessage(), expected.getMessage().contains("cmd.calc.t4"));
        }
    }

    @Test
    public void errorContainsIsCheckedWhenPresent() {
        CompatCase c = specCase("syntax.unknown.t1", "error", "norrows 1 | nosuchcommand", null);
        c.errorContains = "NOSUCHCOMMAND";
        CaseRunner.assertSpec(c);

        CompatCase wrong = specCase("syntax.unknown.t2", "error", "norrows 1 | nosuchcommand", null);
        wrong.errorContains = "this text is not in the message";
        try {
            CaseRunner.assertSpec(wrong);
            Assert.fail("expected an AssertionError for a non-matching errorContains");
        } catch (AssertionError expected) {
            Assert.assertTrue(expected.getMessage(),
                    expected.getMessage().contains("this text is not in the message"));
        }
    }

    @Test
    public void inputFixturesAreMaterialisedUnderRoot() {
        CompatCase c = specCase("cmd.gor.t1", "exact", "gor ${ROOT}/basic.gor",
                "Chrom\tPos\tVal\nchr1\t1\t10\n");
        CompatInput in = new CompatInput();
        in.path = "basic.gor";
        in.content = "Chrom\tPos\tVal\nchr1\t1\t10\n";
        c.inputs.add(in);

        CaseRunner.assertSpec(c);
    }

    @Test
    public void runReturnsResultWithoutAsserting() {
        CompatResult r = CaseRunner.run(specCase("cmd.calc.t5", "exact", "norrows 2", null));
        Assert.assertFalse(r.failed());
        Assert.assertEquals("ChromNOR\tPosNOR\tRowNum", r.header);
        Assert.assertEquals(2, r.rows.size());
    }
}
```

- [x] **Step 2: Run to verify it fails**

```bash
./gradlew :compat:test --tests "org.gorpipe.compat.UTestCaseRunner"
```

Expected: FAIL — `CaseRunner` does not exist.

- [x] **Step 3: Write CaseRunner**

Create `compat/src/main/java/org/gorpipe/compat/CaseRunner.java`:

```java
package org.gorpipe.compat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
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
        materialiseInputs(c, root);
        String query = c.query.replace("${ROOT}", root.toAbsolutePath().toString());
        return CompatExecutor.run(query, root, Fixtures.configFileIfPresent(root));
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
```

Note: `CaseRunner` references `Fixtures.configFileIfPresent` and
`Fixtures.readSharedData`, which Task 8 creates. To keep this task independently
compilable, create the minimal stub now and let Task 8 fill it in:

Create `compat/src/main/java/org/gorpipe/compat/Fixtures.java`:

```java
package org.gorpipe.compat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Canonical fixtures. Expanded in Task 8 with the shared data set and the
 * synthetic chromSeq reference build.
 */
public final class Fixtures {

    private Fixtures() {
    }

    /**
     * Returns the engine config file for a case root, or null when the case does
     * not need one. Task 8 makes this return the generated reference-build config.
     */
    public static Path configFileIfPresent(Path root) {
        Path config = root.resolve("gor_config.txt");
        return Files.exists(config) ? config : null;
    }

    /** Reads a shared fixture from compat/data by file name. */
    public static String readSharedData(String name, String caseId) {
        if (name == null) {
            throw new IllegalStateException("Case " + caseId
                    + " declares an input with neither content nor contentFile");
        }
        Path path = CaseLoader.moduleRoot().resolve("data").resolve(name);
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Missing shared fixture " + path + " referenced by case " + caseId, e);
        }
    }
}
```

- [x] **Step 4: Run the runner test**

```bash
./gradlew :compat:test --tests "org.gorpipe.compat.UTestCaseRunner"
```

Expected: PASS, 7 tests.

- [x] **Step 5: Write the SPEC suite**

Create `compat/src/test/java/org/gorpipe/compat/UTestSpecSuite.java`:

```java
package org.gorpipe.compat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Every spec case, each as its own named JUnit case. These gate the build:
 * a spec case is a claim that GOR behaves a particular way, backed by a citation.
 */
@RunWith(Parameterized.class)
public class UTestSpecSuite {

    @Parameterized.Parameter
    public CompatCase compatCase;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        List<Object[]> params = new ArrayList<>();
        for (CompatCase c : CaseLoader.loadAll()) {
            if (c.isSpec()) {
                params.add(new Object[]{c});
            }
        }
        return params;
    }

    @Test
    public void specCaseHoldsItsContract() {
        CaseRunner.assertSpec(compatCase);
    }
}
```

- [x] **Step 6: Run the spec suite**

```bash
./gradlew :compat:test --tests "org.gorpipe.compat.UTestSpecSuite"
```

Expected: PASS, 3 tests — the seed cases from `cases/spec/cmd/calc.yml`, listed by id.

If `cmd.calc.missing_expression_errors` fails because `norrows 1 | calc X` does not
error, print the actual result and adjust the seed case to a construct that genuinely
fails. Do not weaken the runner to accommodate it.

- [x] **Step 7: Commit**

```bash
git add compat/src/main/java/org/gorpipe/compat/CaseRunner.java \
        compat/src/main/java/org/gorpipe/compat/Fixtures.java \
        compat/src/test/java/org/gorpipe/compat/UTestCaseRunner.java \
        compat/src/test/java/org/gorpipe/compat/UTestSpecSuite.java
git commit -m "feat(compat): add case runner and gating spec suite"
```

---

### Task 6: Baseline tier — on-disk baselines, diffing, and the accept guard

Spec §6.3 and §9. Baseline outputs live in `.out` files keyed by case id, one file
per feature, so that `generate` and `accept` write disjoint sets of files.

**Files:**
- Create: `compat/src/main/java/org/gorpipe/compat/BaselineStore.java`
- Create: `compat/src/main/java/org/gorpipe/compat/BaselineAccept.java`
- Create: `compat/cases/baseline/cmd/norrows.yml`
- Test: `compat/src/test/java/org/gorpipe/compat/UTestBaselineStore.java`
- Test: `compat/src/test/java/org/gorpipe/compat/UTestBaselineSuite.java`

**Interfaces:**
- Consumes: `CompatCase`, `CaseRunner`, `CompatResult`, `CaseLoader`
- Produces:
  - `BaselineStore.pathFor(CompatCase c) -> Path` — `baselines/<category>/<feature>.out`
  - `BaselineStore.load(CompatCase c) -> String` (null when absent)
  - `BaselineStore.loadAll(Path baselinesDir) -> Map<String, String>` keyed by case id
  - `BaselineStore.write(Path file, Map<String, String> idToOutput) -> void`
  - `BaselineStore.render(CompatResult r) -> String` — the recorded form, `ERROR: <message>` for failures
  - `BaselineAccept.main(String[] args)` — regenerates every baseline; refuses to run in CI

- [x] **Step 1: Decide and document the baseline file format**

The format is line-oriented and diff-friendly. Blocks are sorted by case id, so a
regenerated file has a stable order and a real change shows as a local diff:

```
# GOR compatibility baselines. Generated by :compat:accept — do not hand-edit.
>>> cmd.norrows.basic_one_row
ChromNOR	PosNOR	RowNum
chrN	0	0
<<<
>>> cmd.norrows.error_negative
ERROR: <message text>
<<<
```

- [x] **Step 2: Write a baseline case with no expected field**

Create `compat/cases/baseline/cmd/norrows.yml`:

```yaml
- id: cmd.norrows.basic_one_row
  tier: baseline
  mode: exact
  behavior: "NORROWS emits the requested number of rows"
  query: "norrows 1"

- id: cmd.norrows.two_rows
  tier: baseline
  mode: exact
  behavior: "NORROWS row numbering starts at zero"
  query: "norrows 2"
```

- [x] **Step 3: Write the failing store test**

Create `compat/src/test/java/org/gorpipe/compat/UTestBaselineStore.java`:

```java
package org.gorpipe.compat;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class UTestBaselineStore {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static CompatCase baselineCase(String id, String query) {
        CompatCase c = new CompatCase();
        c.id = id;
        c.tier = "baseline";
        c.mode = "exact";
        c.query = query;
        return c;
    }

    @Test
    public void pathIsDerivedFromCategoryAndFeature() {
        Path p = BaselineStore.pathFor(baselineCase("cmd.norrows.basic_one_row", "norrows 1"));
        Assert.assertTrue(p.toString(), p.toString().endsWith("baselines/cmd/norrows.out"));
    }

    @Test
    public void rendersSuccessAsHeaderAndRows() {
        CompatResult r = CaseRunner.run(baselineCase("cmd.norrows.x", "norrows 2"));
        Assert.assertEquals("ChromNOR\tPosNOR\tRowNum\nchrN\t0\t0\nchrN\t0\t1\n",
                BaselineStore.render(r));
    }

    @Test
    public void rendersFailureWithErrorPrefix() {
        CompatResult r = CaseRunner.run(baselineCase("cmd.x.bad", "norrows 1 | nosuchcommand"));
        Assert.assertTrue(r.failed());
        Assert.assertTrue(BaselineStore.render(r).startsWith("ERROR: "));
        Assert.assertTrue(BaselineStore.render(r).contains("NOSUCHCOMMAND"));
    }

    @Test
    public void roundTripsThroughAFile() throws IOException {
        Path file = tmp.newFile("norrows.out").toPath();
        Map<String, String> written = new LinkedHashMap<>();
        written.put("cmd.norrows.b", "ChromNOR\tPosNOR\tRowNum\nchrN\t0\t0\n");
        written.put("cmd.norrows.a", "ERROR: boom\n");

        BaselineStore.write(file, written);
        Map<String, String> read = BaselineStore.loadAll(file.getParent());

        Assert.assertEquals(2, read.size());
        Assert.assertEquals("ChromNOR\tPosNOR\tRowNum\nchrN\t0\t0\n", read.get("cmd.norrows.b"));
        Assert.assertEquals("ERROR: boom\n", read.get("cmd.norrows.a"));
    }

    @Test
    public void writesBlocksSortedByIdForStableDiffs() throws IOException {
        Path file = tmp.newFile("sorted.out").toPath();
        Map<String, String> written = new LinkedHashMap<>();
        written.put("cmd.z.last", "z\n");
        written.put("cmd.a.first", "a\n");

        BaselineStore.write(file, written);
        String content = java.nio.file.Files.readString(file);

        Assert.assertTrue(content.indexOf("cmd.a.first") < content.indexOf("cmd.z.last"));
    }
}
```

- [x] **Step 4: Run to verify it fails**

```bash
./gradlew :compat:test --tests "org.gorpipe.compat.UTestBaselineStore"
```

Expected: FAIL — `BaselineStore` does not exist.

- [x] **Step 5: Write BaselineStore**

Create `compat/src/main/java/org/gorpipe/compat/BaselineStore.java`:

```java
package org.gorpipe.compat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Reads and writes committed baseline outputs.
 *
 * Baselines live beside the corpus rather than inside the generated case files so
 * that regenerating the case list never rewrites an output, and accepting a
 * behaviour change never rewrites a query.
 */
public final class BaselineStore {

    private static final String HEADER =
            "# GOR compatibility baselines. Generated by :compat:accept — do not hand-edit.";
    private static final String BEGIN = ">>> ";
    private static final String END = "<<<";

    private BaselineStore() {
    }

    public static Path pathFor(CompatCase c) {
        return CaseLoader.moduleRoot()
                .resolve("baselines")
                .resolve(c.category())
                .resolve(c.feature() + ".out");
    }

    /** The recorded form of a result: serialised output, or the error message. */
    public static String render(CompatResult r) {
        return r.failed() ? "ERROR: " + r.errorMessage + "\n" : r.serialised();
    }

    public static String load(CompatCase c) {
        Path file = pathFor(c);
        if (!Files.exists(file)) {
            return null;
        }
        return parse(file).get(c.id);
    }

    public static Map<String, String> loadAll(Path baselinesDir) {
        Map<String, String> all = new TreeMap<>();
        if (!Files.isDirectory(baselinesDir)) {
            return all;
        }
        try (Stream<Path> walk = Files.walk(baselinesDir)) {
            List<Path> files = walk.filter(p -> p.toString().endsWith(".out"))
                    .sorted().collect(Collectors.toList());
            for (Path f : files) {
                all.putAll(parse(f));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot enumerate baselines under " + baselinesDir, e);
        }
        return all;
    }

    public static void write(Path file, Map<String, String> idToOutput) {
        StringBuilder sb = new StringBuilder();
        sb.append(HEADER).append('\n');
        // Sorted so that a regenerated file differs only where behaviour differs.
        for (Map.Entry<String, String> e : new TreeMap<>(idToOutput).entrySet()) {
            sb.append(BEGIN).append(e.getKey()).append('\n');
            sb.append(e.getValue());
            if (!e.getValue().endsWith("\n")) {
                sb.append('\n');
            }
            sb.append(END).append('\n');
        }
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write baseline file " + file, e);
        }
    }

    private static Map<String, String> parse(Path file) {
        Map<String, String> result = new TreeMap<>();
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            String currentId = null;
            StringBuilder body = new StringBuilder();
            for (String line : lines) {
                if (line.startsWith(BEGIN)) {
                    currentId = line.substring(BEGIN.length()).trim();
                    body.setLength(0);
                } else if (line.equals(END)) {
                    if (currentId == null) {
                        throw new IllegalStateException(
                                "Baseline file " + file + " has a closing marker with no case id");
                    }
                    result.put(currentId, body.toString());
                    currentId = null;
                } else if (currentId != null) {
                    body.append(line).append('\n');
                }
            }
            if (currentId != null) {
                throw new IllegalStateException(
                        "Baseline file " + file + " ends inside case '" + currentId + "'");
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read baseline file " + file, e);
        }
        return result;
    }
}
```

- [x] **Step 6: Run the store test**

```bash
./gradlew :compat:test --tests "org.gorpipe.compat.UTestBaselineStore"
```

Expected: PASS, 5 tests.

- [x] **Step 7: Write the baseline suite**

Create `compat/src/test/java/org/gorpipe/compat/UTestBaselineSuite.java`:

```java
package org.gorpipe.compat;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Every baseline case, diffed against its committed output.
 *
 * A missing baseline is a failure, not a pass: it means a case was generated and
 * nobody recorded what the engine does with it.
 */
@RunWith(Parameterized.class)
public class UTestBaselineSuite {

    @Parameterized.Parameter
    public CompatCase compatCase;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        List<Object[]> params = new ArrayList<>();
        for (CompatCase c : CaseLoader.loadAll()) {
            if (c.isBaseline()) {
                params.add(new Object[]{c});
            }
        }
        return params;
    }

    @Test
    public void outputMatchesCommittedBaseline() {
        String committed = BaselineStore.load(compatCase);
        if (committed == null) {
            Assert.fail("[" + compatCase.id + "] has no committed baseline in "
                    + BaselineStore.pathFor(compatCase)
                    + "\n  This case is new. Review what the engine does with it, then run:"
                    + "\n    ./gradlew :compat:accept");
        }

        String actual = BaselineStore.render(CaseRunner.run(compatCase));
        if (!committed.equals(actual)) {
            Assert.fail("[" + compatCase.id + "] behaviour changed"
                    + "\n  query: " + compatCase.query
                    + "\n--- committed ---\n" + committed.replace("\t", "<TAB>")
                    + "--- now ---\n" + actual.replace("\t", "<TAB>")
                    + "\n  If this change is intended, run ./gradlew :compat:accept and"
                    + " commit the rewritten baselines so the diff is reviewable.");
        }
    }
}
```

- [x] **Step 8: Write BaselineAccept with a CI guard**

Create `compat/src/main/java/org/gorpipe/compat/BaselineAccept.java`:

```java
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
```

Add the task to `compat/build.gradle`, inside the `project(':compat')` block:

```groovy
    tasks.register('accept', JavaExec) {
        group = 'compat'
        description = 'Re-capture baseline outputs and rewrite the committed files'
        mainClass = 'org.gorpipe.compat.BaselineAccept'
        classpath = sourceSets.main.runtimeClasspath
        workingDir = projectDir
    }
```

- [x] **Step 9: Accept the seed baselines, then verify the suite is green**

```bash
./gradlew :compat:accept
cat compat/baselines/cmd/norrows.out
```

Expected: two blocks, `cmd.norrows.basic_one_row` and `cmd.norrows.two_rows`, with
the measured NOR output.

```bash
./gradlew :compat:test --tests "org.gorpipe.compat.UTestBaselineSuite"
```

Expected: PASS, 2 tests.

- [x] **Step 10: Prove an unaccepted change fails the build**

Temporarily corrupt a baseline and confirm the failure is loud:

```bash
python3 - <<'PY'
p='compat/baselines/cmd/norrows.out'
s=open(p).read().replace('chrN\t0\t0', 'chrN\t0\t999', 1)
open(p,'w').write(s)
PY
./gradlew :compat:test --tests "org.gorpipe.compat.UTestBaselineSuite"
```

Expected: FAIL, with a message showing committed vs now and naming
`./gradlew :compat:accept`.

Restore it:

```bash
./gradlew :compat:accept
git diff --stat compat/baselines/
```

Expected: no diff.

- [x] **Step 11: Commit**

```bash
git add compat/src/main/java/org/gorpipe/compat/BaselineStore.java \
        compat/src/main/java/org/gorpipe/compat/BaselineAccept.java \
        compat/src/test/java/org/gorpipe/compat/UTestBaselineStore.java \
        compat/src/test/java/org/gorpipe/compat/UTestBaselineSuite.java \
        compat/cases/baseline/cmd/norrows.yml \
        compat/baselines/ \
        compat/build.gradle
git commit -m "feat(compat): add baseline tier with committed outputs and CI-guarded accept"
```

---

### Task 7: Corpus lint

Spec §5.5 and §6.2. These enforce properties the JSON schema cannot express, and
they run over every case in both tiers, because a malformed baseline case is still
malformed.

**Files:**
- Create: `compat/src/main/java/org/gorpipe/compat/CaseLint.java`
- Test: `compat/src/test/java/org/gorpipe/compat/UTestCaseLint.java`
- Test: `compat/src/test/java/org/gorpipe/compat/UTestCorpusLint.java`

**Interfaces:**
- Consumes: `CompatCase`, `CompatInput`
- Produces: `CaseLint.check(List<CompatCase>) -> List<String>` — empty means clean

- [x] **Step 1: Write the failing lint test**

Create `compat/src/test/java/org/gorpipe/compat/UTestCaseLint.java`:

```java
package org.gorpipe.compat;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class UTestCaseLint {

    private static CompatCase c(String id, String query) {
        CompatCase x = new CompatCase();
        x.id = id;
        x.tier = "spec";
        x.mode = "exact";
        x.source = "review";
        x.query = query;
        x.expected = "h\n";
        x.cites.add("documentation/src/command/CALC.rst");
        return x;
    }

    @Test
    public void cleanCorpusProducesNoViolations() {
        List<String> v = CaseLint.check(Collections.singletonList(c("cmd.a.one", "norrows 1")));
        Assert.assertTrue(v.toString(), v.isEmpty());
    }

    @Test
    public void nonDeterministicConstructIsAViolation() {
        List<String> v = CaseLint.check(
                Collections.singletonList(c("cmd.a.rand", "norrows 1 | calc X random()")));
        Assert.assertEquals(v.toString(), 1, v.size());
        Assert.assertTrue(v.get(0), v.get(0).contains("random"));
    }

    @Test
    public void absolutePathOutsideRootIsAViolation() {
        List<String> v = CaseLint.check(
                Collections.singletonList(c("cmd.a.abs", "gor /Users/someone/data.gor")));
        Assert.assertEquals(v.toString(), 1, v.size());
        Assert.assertTrue(v.get(0), v.get(0).contains("absolute path"));
    }

    @Test
    public void rootPlaceholderIsNotAnAbsolutePathViolation() {
        List<String> v = CaseLint.check(
                Collections.singletonList(c("cmd.a.ok", "gor ${ROOT}/data.gor")));
        Assert.assertTrue(v.toString(), v.isEmpty());
    }

    @Test
    public void identicalBodiesAreAViolation() {
        List<String> v = CaseLint.check(Arrays.asList(
                c("cmd.a.one", "norrows 1"),
                c("cmd.a.dup", "norrows 1")));
        Assert.assertEquals(v.toString(), 1, v.size());
        Assert.assertTrue(v.get(0), v.get(0).contains("duplicate body"));
    }

    @Test
    public void unreferencedInputIsAViolation() {
        CompatCase x = c("cmd.a.orphan", "norrows 1");
        CompatInput in = new CompatInput();
        in.path = "unused.gor";
        in.content = "h\n";
        x.inputs.add(in);

        List<String> v = CaseLint.check(Collections.singletonList(x));
        Assert.assertEquals(v.toString(), 1, v.size());
        Assert.assertTrue(v.get(0), v.get(0).contains("unused.gor"));
    }

    @Test
    public void specCaseWithoutCitationIsAViolation() {
        CompatCase x = c("cmd.a.nocite", "norrows 1");
        x.cites.clear();

        List<String> v = CaseLint.check(Collections.singletonList(x));
        Assert.assertEquals(v.toString(), 1, v.size());
        Assert.assertTrue(v.get(0), v.get(0).contains("citation"));
    }

    @Test
    public void baselineCaseWithInlineExpectedIsAViolation() {
        CompatCase x = c("cmd.a.baseline", "norrows 1");
        x.tier = "baseline";
        x.source = null;
        x.cites.clear();
        x.expected = "h\n";

        List<String> v = CaseLint.check(Collections.singletonList(x));
        Assert.assertEquals(v.toString(), 1, v.size());
        Assert.assertTrue(v.get(0), v.get(0).contains("inline expected"));
    }

    @Test
    public void exactSpecCaseWithoutExpectedIsAViolation() {
        CompatCase x = c("cmd.a.noexpected", "norrows 1");
        x.expected = null;

        List<String> v = CaseLint.check(Collections.singletonList(x));
        Assert.assertEquals(v.toString(), 1, v.size());
        Assert.assertTrue(v.get(0), v.get(0).contains("no expected output"));
    }
}
```

- [x] **Step 2: Run to verify it fails**

```bash
./gradlew :compat:test --tests "org.gorpipe.compat.UTestCaseLint"
```

Expected: FAIL — `CaseLint` does not exist.

- [x] **Step 3: Write CaseLint**

Create `compat/src/main/java/org/gorpipe/compat/CaseLint.java`:

```java
package org.gorpipe.compat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Structural checks over the corpus.
 *
 * These run over both tiers. A baseline case that embeds wall-clock time produces
 * a baseline that changes on its own, which would train reviewers to accept diffs
 * without reading them — the failure mode this suite most needs to avoid.
 */
public final class CaseLint {

    /**
     * Constructs whose output depends on machine state. A case using any of these
     * cannot have a stable expectation, so it is rejected rather than left to fail
     * intermittently later.
     */
    private static final String[] NON_DETERMINISTIC = {
            "random(", "rand(", "now(", "currentdate", "curdate", "today(",
            "hostname(", "gettime", "systime", "timestamp("
    };

    /**
     * Absolute paths into machine-specific locations. ${ROOT} is stripped before
     * matching, so the per-case project root is allowed.
     */
    private static final Pattern ABSOLUTE_PATH =
            Pattern.compile("(?<![\\w$])/(?:Users|home|tmp|var|opt|mnt|private)/");

    private CaseLint() {
    }

    public static List<String> check(List<CompatCase> cases) {
        List<String> violations = new ArrayList<>();
        Map<String, String> bodies = new HashMap<>();

        for (CompatCase c : cases) {
            checkTierInvariants(c, violations);
            checkDeterminism(c, violations);
            checkAbsolutePaths(c, violations);
            checkInputsReferenced(c, violations);
            checkDuplicateBody(c, bodies, violations);
        }
        return violations;
    }

    private static void checkTierInvariants(CompatCase c, List<String> violations) {
        if (c.isSpec()) {
            if (c.cites == null || c.cites.isEmpty()) {
                violations.add("[" + c.id + "] spec case has no citation; a spec case must say "
                        + "where its expectation came from");
            }
            if ("exact".equals(c.mode) && (c.expected == null || c.expected.isEmpty())) {
                violations.add("[" + c.id + "] spec case in exact mode has no expected output");
            }
        }
        if (c.isBaseline() && c.expected != null) {
            violations.add("[" + c.id + "] baseline case carries an inline expected block; "
                    + "baseline outputs belong in baselines/");
        }
    }

    private static void checkDeterminism(CompatCase c, List<String> violations) {
        String q = c.query.toLowerCase(Locale.ROOT).replace(" ", "");
        for (String token : NON_DETERMINISTIC) {
            if (q.contains(token)) {
                violations.add("[" + c.id + "] query uses the non-deterministic construct '"
                        + token + "'");
                return;
            }
        }
    }

    private static void checkAbsolutePaths(CompatCase c, List<String> violations) {
        Matcher m = ABSOLUTE_PATH.matcher(c.query.replace("${ROOT}", ""));
        if (m.find()) {
            violations.add("[" + c.id + "] query contains an absolute path outside ${ROOT}: "
                    + m.group());
        }
    }

    private static void checkInputsReferenced(CompatCase c, List<String> violations) {
        for (CompatInput in : c.inputs) {
            if (!c.query.contains(in.path)) {
                violations.add("[" + c.id + "] declares the input '" + in.path
                        + "' which the query never references");
            }
        }
    }

    private static void checkDuplicateBody(CompatCase c, Map<String, String> bodies,
                                           List<String> violations) {
        StringBuilder key = new StringBuilder();
        key.append(c.query).append(' ').append(c.mode)
                .append(' ').append(c.expected == null ? "" : c.expected);
        for (CompatInput in : c.inputs) {
            key.append(' ').append(in.path).append('=')
                    .append(in.content == null ? in.contentFile : in.content);
        }

        String previous = bodies.put(key.toString(), c.id);
        if (previous != null) {
            violations.add("[" + c.id + "] duplicate body — identical to " + previous);
        }
    }
}
```

- [x] **Step 4: Run the lint unit test**

```bash
./gradlew :compat:test --tests "org.gorpipe.compat.UTestCaseLint"
```

Expected: PASS, 9 tests.

- [x] **Step 5: Wire lint into the build as a gate**

Create `compat/src/test/java/org/gorpipe/compat/UTestCorpusLint.java`:

```java
package org.gorpipe.compat;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class UTestCorpusLint {

    @Test
    public void corpusPassesLint() {
        List<String> violations = CaseLint.check(CaseLoader.loadAll());
        if (!violations.isEmpty()) {
            Assert.fail("Corpus lint violations (" + violations.size() + "):\n  "
                    + String.join("\n  ", violations));
        }
    }
}
```

- [x] **Step 6: Run the whole module**

```bash
./gradlew :compat:test
```

Expected: PASS. If the seed corpus trips a rule, fix the seed case — not the rule.

- [x] **Step 7: Commit**

```bash
git add compat/src/main/java/org/gorpipe/compat/CaseLint.java \
        compat/src/test/java/org/gorpipe/compat/UTestCaseLint.java \
        compat/src/test/java/org/gorpipe/compat/UTestCorpusLint.java
git commit -m "feat(compat): add corpus lint for determinism, duplicates and tier invariants"
```

---

### Task 8: Canonical fixtures and the synthetic chromSeq reference build

Spec §7. Fixtures are generated by `:compat` itself so the module stays independent
of `tests/data`, which is the submodule the unit tests use.

The reference build matters more than it looks: eight analysis classes need one
(`Pileup`, `VarNormAnalysis`, `VerifyVariantAnalysis`, `MergeGenotypes`,
`AddFlankingSeqs`, `PhaseReadVariants`, `CigarVarSegs`, `VarJoinAnalysis`), and
without it that entire surface would be excluded.

**Files:**
- Modify: `compat/src/main/java/org/gorpipe/compat/Fixtures.java`
- Modify: `compat/src/main/java/org/gorpipe/compat/CompatCase.java`
- Modify: `compat/src/main/java/org/gorpipe/compat/CaseRunner.java`
- Modify: `compat/schema/gor-compat-case.schema.json`
- Create: `compat/cases/spec/fn/refbase.yml`
- Test: `compat/src/test/java/org/gorpipe/compat/UTestFixtures.java`

**Interfaces:**
- Consumes: `CaseLoader.moduleRoot()`, `CompatExecutor.run(String, Path, Path)`
- Produces:
  - `Fixtures.writeReferenceBuild(Path root) -> Path` — writes `chromSeq/`, `buildsize.gor`, `buildsplit.txt`, `gor_config.txt` under `root`; returns the config path
  - `Fixtures.referenceSequence(String chrom) -> String` — the exact synthetic sequence, so tests can derive expected bases
  - `Fixtures.canonicalInputs() -> List<CompatInput>` — the shared fixture set generators attach to cases
  - `CompatCase.needsReference` (new public boolean field)

- [x] **Step 1: Write the failing fixtures test**

The expected base is derived, not guessed: `chr1` is `ACGT` repeated, so 1-based
position 10 is byte offset 9, and `"ACGTACGTAC".charAt(9)` is `C`.

Create `compat/src/test/java/org/gorpipe/compat/UTestFixtures.java`:

```java
package org.gorpipe.compat;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class UTestFixtures {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void writesReferenceBuildLayout() {
        Path root = tmp.getRoot().toPath();
        Path config = Fixtures.writeReferenceBuild(root);

        Assert.assertTrue(Files.exists(root.resolve("chromSeq/chr1.txt")));
        Assert.assertTrue(Files.exists(root.resolve("chromSeq/chr2.txt")));
        Assert.assertTrue(Files.exists(root.resolve("buildsize.gor")));
        Assert.assertTrue(Files.exists(root.resolve("buildsplit.txt")));
        Assert.assertEquals(root.resolve("gor_config.txt"), config);
    }

    @Test
    public void sizeAndSplitFilesHaveNoHeaderLine() throws IOException {
        // gor's parser reads these as bare contig/value pairs. A header line
        // produces NumberFormatException: For input string: "Size".
        Path root = tmp.getRoot().toPath();
        Fixtures.writeReferenceBuild(root);

        String firstSizeLine = Files.readAllLines(root.resolve("buildsize.gor")).get(0);
        Assert.assertTrue(firstSizeLine, firstSizeLine.startsWith("chr"));
        Assert.assertFalse(firstSizeLine.toLowerCase().contains("size"));

        String firstSplitLine = Files.readAllLines(root.resolve("buildsplit.txt")).get(0);
        Assert.assertTrue(firstSplitLine, firstSplitLine.startsWith("chr"));
    }

    @Test
    public void sequenceFilesAreOneBytePerBaseWithNoNewlines() throws IOException {
        Path root = tmp.getRoot().toPath();
        Fixtures.writeReferenceBuild(root);

        String chr1 = Files.readString(root.resolve("chromSeq/chr1.txt"));
        Assert.assertEquals(Fixtures.referenceSequence("chr1"), chr1);
        Assert.assertFalse("sequence must contain no newlines", chr1.contains("\n"));
    }

    @Test
    public void engineReadsTheSyntheticReferenceBuild() {
        Path root = tmp.getRoot().toPath();
        Path config = Fixtures.writeReferenceBuild(root);

        CompatResult r = CompatExecutor.run("gorrow chr1,10 | calc R refbase(chrom,pos)",
                root, config);

        Assert.assertFalse(r.errorMessage, r.failed());
        Assert.assertEquals("chrom\tpos\tR", r.header);

        char expectedBase = Fixtures.referenceSequence("chr1").charAt(9);
        Assert.assertEquals("chr1\t10\t" + expectedBase, r.rows.get(0));
    }

    @Test
    public void canonicalInputsAreSelfConsistent() {
        for (CompatInput in : Fixtures.canonicalInputs()) {
            Assert.assertNotNull(in.path);
            Assert.assertNotNull("canonical fixture " + in.path + " has no content", in.content);
            Assert.assertTrue("canonical fixture " + in.path + " must be tab separated",
                    in.content.contains("\t"));
        }
    }
}
```

- [x] **Step 2: Run to verify it fails**

```bash
./gradlew :compat:test --tests "org.gorpipe.compat.UTestFixtures"
```

Expected: FAIL — `writeReferenceBuild`, `referenceSequence` and `canonicalInputs`
do not exist.

- [x] **Step 3: Replace Fixtures with the full implementation**

Overwrite `compat/src/main/java/org/gorpipe/compat/Fixtures.java`:

```java
package org.gorpipe.compat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic fixtures, generated by this module rather than read from
 * tests/data, so that :compat stays independent of the unit test data submodule.
 */
public final class Fixtures {

    /**
     * Synthetic reference sequences. Short, fixed, and chosen so that an expected
     * base is easy to derive by hand: chr1 is ACGT repeated, chr2 is TTGCA
     * repeated.
     */
    private static final Map<String, String> SEQUENCES = new LinkedHashMap<>();

    static {
        SEQUENCES.put("chr1", "ACGT".repeat(250));
        SEQUENCES.put("chr2", "TTGCA".repeat(200));
    }

    private Fixtures() {
    }

    public static String referenceSequence(String chrom) {
        String seq = SEQUENCES.get(chrom);
        if (seq == null) {
            throw new IllegalArgumentException("No synthetic sequence for " + chrom);
        }
        return seq;
    }

    /**
     * Writes a chromSeq reference build under root and returns the config path.
     *
     * Layout is documented in scripts/refbuild/README.md: one file per contig
     * holding raw bases at one byte per base with no newlines, so that the byte at
     * offset pos-1 is the base at 1-based position pos.
     *
     * buildsize.gor and buildsplit.txt MUST NOT have a header line — gor's parser
     * reads them as bare pairs, and a header yields
     * NumberFormatException: For input string: "Size".
     */
    public static Path writeReferenceBuild(Path root) {
        try {
            Path chromSeq = root.resolve("chromSeq");
            Files.createDirectories(chromSeq);

            StringBuilder sizes = new StringBuilder();
            StringBuilder splits = new StringBuilder();

            for (Map.Entry<String, String> e : SEQUENCES.entrySet()) {
                String chrom = e.getKey();
                String seq = e.getValue();
                Files.writeString(chromSeq.resolve(chrom + ".txt"), seq, StandardCharsets.UTF_8);
                sizes.append(chrom).append('\t').append(seq.length()).append('\n');
                splits.append(chrom).append('\t').append(seq.length() / 2).append('\n');
            }

            Path sizeFile = root.resolve("buildsize.gor");
            Path splitFile = root.resolve("buildsplit.txt");
            Files.writeString(sizeFile, sizes.toString(), StandardCharsets.UTF_8);
            Files.writeString(splitFile, splits.toString(), StandardCharsets.UTF_8);

            Path config = root.resolve("gor_config.txt");
            String configBody = ""
                    + "buildPath\t" + chromSeq.toAbsolutePath() + "\n"
                    + "buildSizeFile\t" + sizeFile.toAbsolutePath() + "\n"
                    + "buildSplitFile\t" + splitFile.toAbsolutePath() + "\n";
            Files.writeString(config, configBody, StandardCharsets.UTF_8);

            return config;
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write reference build under " + root, e);
        }
    }

    public static Path configFileIfPresent(Path root) {
        Path config = root.resolve("gor_config.txt");
        return Files.exists(config) ? config : null;
    }

    /** The shared fixture set that generated cases attach to their queries. */
    public static List<CompatInput> canonicalInputs() {
        List<CompatInput> inputs = new ArrayList<>();
        inputs.add(input("left.gor",
                "Chrom\tPos\tRef\tAlt\n"
                        + "chr1\t100\tA\tG\n"
                        + "chr1\t200\tC\tT\n"
                        + "chr2\t150\tG\tA\n"));
        inputs.add(input("right.gor",
                "Chrom\tPos\tGene\n"
                        + "chr1\t100\tBRCA1\n"
                        + "chr1\t250\tTP53\n"
                        + "chr2\t150\tEGFR\n"));
        inputs.add(input("segments.gor",
                "Chrom\tbpStart\tbpStop\tName\n"
                        + "chr1\t50\t150\tregionA\n"
                        + "chr1\t180\t260\tregionB\n"
                        + "chr2\t100\t200\tregionC\n"));
        inputs.add(input("pheno.tsv",
                "PN\tSex\tAge\n"
                        + "PN001\tM\t42\n"
                        + "PN002\tF\t37\n"));
        return inputs;
    }

    private static CompatInput input(String path, String content) {
        CompatInput in = new CompatInput();
        in.path = path;
        in.content = content;
        return in;
    }

    /** Reads a shared fixture from compat/data by relative path. */
    public static String readSharedData(String name, String caseId) {
        if (name == null) {
            throw new IllegalStateException("Case " + caseId
                    + " declares an input with neither content nor contentFile");
        }
        Path path = CaseLoader.moduleRoot().resolve("data").resolve(name);
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Missing shared fixture " + path + " referenced by case " + caseId, e);
        }
    }
}
```

- [x] **Step 4: Let a case opt into the reference build**

Add the field to `CompatCase`, after the `inputs` declaration:

```java
    /** When true, a synthetic chromSeq reference build is written into the root. */
    public boolean needsReference;
```

Add it to `compat/schema/gor-compat-case.schema.json` under `properties`, after
`"behavior"`:

```json
    "needsReference": { "type": "boolean" },
```

In `CaseRunner`, replace the `execute` method with:

```java
    private static CompatResult execute(CompatCase c, Path root) {
        if (c.needsReference) {
            Fixtures.writeReferenceBuild(root);
        }
        materialiseInputs(c, root);
        String query = c.query.replace("${ROOT}", root.toAbsolutePath().toString());
        return CompatExecutor.run(query, root, Fixtures.configFileIfPresent(root));
    }
```

- [x] **Step 5: Add a spec case that exercises the reference build**

Create `compat/cases/spec/fn/refbase.yml`. Real tabs required in the `expected`
block:

```yaml
- id: fn.refbase.single_base_lookup
  tier: spec
  mode: exact
  source: review
  needsReference: true
  behavior: "refbase returns the reference base at a 1-based position"
  cites: ["scripts/refbuild/README.md"]
  query: "gorrow chr1,10 | calc R refbase(chrom,pos)"
  expected: |
    chrom	pos	R
    chr1	10	C
```

- [x] **Step 6: Run the fixtures test and the whole module**

```bash
./gradlew :compat:test --tests "org.gorpipe.compat.UTestFixtures"
./gradlew :compat:test
```

Expected: PASS. `UTestFixtures` has 5 tests; the spec suite now has 4.

If `engineReadsTheSyntheticReferenceBuild` fails with a `NumberFormatException`
naming a column, a header line has crept into `buildsize.gor` or `buildsplit.txt`.

- [x] **Step 7: Commit**

```bash
git add compat/src/main/java/org/gorpipe/compat/Fixtures.java \
        compat/src/main/java/org/gorpipe/compat/CompatCase.java \
        compat/src/main/java/org/gorpipe/compat/CaseRunner.java \
        compat/schema/gor-compat-case.schema.json \
        compat/cases/spec/fn/refbase.yml \
        compat/src/test/java/org/gorpipe/compat/UTestFixtures.java
git commit -m "feat(compat): add canonical fixtures and synthetic chromSeq reference build"
```

---

### Task 9: Expose registered function names (the one engine change)

Spec §8/G1 and §15. `FunctionRegistry.allFunctions` is
`private val allFunctions = mutable.Map[String, List[FunctionWrapper]]()`
(`gortools/src/main/scala/gorsat/parser/FunctionRegistry.scala:50`) and only
`hasFunction(fn)` is public. The inventory needs to enumerate names.

Reflecting over a private Scala field would break silently on refactor. A read-only
accessor is additive and cannot alter existing behaviour.

**This is the only task permitted to modify engine code.**

**Files:**
- Modify: `gortools/src/main/scala/gorsat/parser/FunctionRegistry.scala`
- Test: `compat/src/test/java/org/gorpipe/compat/UTestFunctionRegistryAccess.java`

**Interfaces:**
- Consumes: nothing
- Produces: `FunctionRegistry.functionNames: java.util.Set[String]`

- [x] **Step 1: Write the failing test**

Create `compat/src/test/java/org/gorpipe/compat/UTestFunctionRegistryAccess.java`:

```java
package org.gorpipe.compat;

import gorsat.parser.FunctionRegistry;
import gorsat.process.PipeInstance;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Set;

public class UTestFunctionRegistryAccess {

    @BeforeClass
    public static void registerEngine() {
        PipeInstance.initialize();
    }

    @Test
    public void enumeratesRegisteredFunctionNames() {
        Set<String> names = FunctionRegistry.functionNames();

        Assert.assertFalse("registry reported no functions", names.isEmpty());
        Assert.assertTrue("expected more than 50 registered functions, got " + names.size(),
                names.size() > 50);
    }

    @Test
    public void everyReportedNameIsRecognisedByHasFunction() {
        for (String name : FunctionRegistry.functionNames()) {
            Assert.assertTrue("hasFunction disagrees with functionNames for " + name,
                    FunctionRegistry.hasFunction(name));
        }
    }

    @Test
    public void reportedSetIsNotMutable() {
        Set<String> names = FunctionRegistry.functionNames();
        try {
            names.add("SHOULD_NOT_BE_POSSIBLE");
            Assert.fail("functionNames must not expose a mutable view of the registry");
        } catch (UnsupportedOperationException expected) {
            // this is the contract
        }
    }
}
```

- [x] **Step 2: Run to verify it fails**

```bash
./gradlew :compat:test --tests "org.gorpipe.compat.UTestFunctionRegistryAccess"
```

Expected: FAIL — `functionNames` does not exist (compilation error).

- [x] **Step 3: Add the accessor**

In `gortools/src/main/scala/gorsat/parser/FunctionRegistry.scala`, immediately after
the existing `hasFunction` method (around lines 52-54), add:

```scala
  /**
    * The names of every registered function. Read-only; exposed for tooling that
    * needs to enumerate the function surface, such as the compatibility suite's
    * inventory generator. Callers must have registered functions first.
    */
  def functionNames: java.util.Set[String] = {
    val names = new java.util.TreeSet[String]()
    allFunctions.keys.foreach(names.add)
    java.util.Collections.unmodifiableSet(names)
  }
```

Returning a `java.util.Set` keeps the Java call sites plain and avoids Scala
collection interop at every use. Populating the `TreeSet` by iteration avoids
depending on a particular `scala.jdk` converter spelling.

- [x] **Step 4: Run the test**

```bash
./gradlew :compat:test --tests "org.gorpipe.compat.UTestFunctionRegistryAccess"
```

Expected: PASS, 3 tests. Record the reported function count — it feeds Task 10.

- [x] **Step 5: Confirm the engine module still builds and its tests still compile**

```bash
./gradlew :gortools:compileScala :gortools:compileTestJava
```

Expected: BUILD SUCCESSFUL.

- [x] **Step 6: Commit**

```bash
git add gortools/src/main/scala/gorsat/parser/FunctionRegistry.scala \
        compat/src/test/java/org/gorpipe/compat/UTestFunctionRegistryAccess.java
git commit -m "feat(compat): expose FunctionRegistry.functionNames for surface enumeration"
```

---

### Task 10: Surface inventory generator

Spec §8/G1 and §10.4. The registries are populated by class instantiation, so names
and flag specifications are not greppable — they exist only at runtime.

**Files:**
- Create: `compat/src/main/java/org/gorpipe/compat/SurfaceInventory.java`
- Create: `compat/src/main/java/org/gorpipe/compat/InventoryMain.java`
- Modify: `compat/build.gradle`
- Test: `compat/src/test/java/org/gorpipe/compat/UTestSurfaceInventory.java`
- Test: `compat/src/test/java/org/gorpipe/compat/UTestInventoryFreshness.java`

**Interfaces:**
- Consumes: `gorsat.process.GorPipeCommands`, `gorsat.Commands.CommandInfo`, `gorsat.Commands.CommandArguments`, `gorsat.parser.FunctionRegistry`, `gorsat.process.PipeInstance`
- Produces:
  - `SurfaceInventory.CommandSurface` with public final fields `String name`, `List<String> valuelessFlags`, `List<String> valueFlags`, `int minArgs`, `int maxArgs`, and method `List<String> allFlags()`
  - `SurfaceInventory.read() -> SurfaceInventory`
  - `SurfaceInventory.commands() -> Map<String, CommandSurface>` (sorted)
  - `SurfaceInventory.functionNames() -> Set<String>` (sorted)
  - `SurfaceInventory.totalFlagCount() -> int`
  - `SurfaceInventory.toJson() -> String` — stable, sorted, newline-terminated

- [x] **Step 1: Write the failing inventory test**

Counts below are measured. The `JOIN` flags are the exact registry strings.

Create `compat/src/test/java/org/gorpipe/compat/UTestSurfaceInventory.java`:

```java
package org.gorpipe.compat;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class UTestSurfaceInventory {

    private static SurfaceInventory inventory;

    @BeforeClass
    public static void readInventory() {
        inventory = SurfaceInventory.read();
    }

    @Test
    public void findsTheRegisteredCommands() {
        // Measured at 108. Asserted as a floor so adding a command does not fail
        // this test, while losing a large number of them would.
        Assert.assertTrue("expected at least 100 commands, got " + inventory.commands().size(),
                inventory.commands().size() >= 100);
        Assert.assertTrue(inventory.commands().containsKey("JOIN"));
        Assert.assertTrue(inventory.commands().containsKey("CALC"));
        Assert.assertTrue(inventory.commands().containsKey("GROUP"));
    }

    @Test
    public void parsesJoinFlagsExactly() {
        SurfaceInventory.CommandSurface join = inventory.commands().get("JOIN");

        Assert.assertTrue(join.valuelessFlags.contains("-snpsnp"));
        Assert.assertTrue(join.valuelessFlags.contains("-segseg"));
        Assert.assertTrue(join.valuelessFlags.contains("-xcis"));
        Assert.assertTrue(join.valueFlags.contains("-maxseg"));
        Assert.assertTrue(join.valueFlags.contains("-refr"));

        Assert.assertFalse("value-taking flags must not appear as valueless",
                join.valuelessFlags.contains("-maxseg"));

        Assert.assertEquals(1, join.minArgs);
        Assert.assertEquals(1, join.maxArgs);
    }

    @Test
    public void countsFlagsAcrossTheSurface() {
        // Measured: 217 valueless + 242 value-taking = 459.
        Assert.assertTrue("expected at least 400 flags, got " + inventory.totalFlagCount(),
                inventory.totalFlagCount() >= 400);
    }

    @Test
    public void enumeratesFunctions() {
        Assert.assertTrue("expected at least 50 functions, got "
                + inventory.functionNames().size(), inventory.functionNames().size() >= 50);
    }

    @Test
    public void jsonIsStableAcrossReads() {
        String first = SurfaceInventory.read().toJson();
        String second = SurfaceInventory.read().toJson();
        Assert.assertEquals("inventory JSON must be deterministic to be diffable",
                first, second);
        Assert.assertTrue(first.endsWith("\n"));
    }

    @Test
    public void flagsAreWellFormed() {
        for (SurfaceInventory.CommandSurface c : inventory.commands().values()) {
            for (String f : c.allFlags()) {
                Assert.assertFalse(c.name + " has an empty flag", f.isEmpty());
                Assert.assertTrue(c.name + " flag missing leading dash: " + f, f.startsWith("-"));
            }
        }
    }
}
```

- [x] **Step 2: Run to verify it fails**

```bash
./gradlew :compat:test --tests "org.gorpipe.compat.UTestSurfaceInventory"
```

Expected: FAIL — `SurfaceInventory` does not exist.

- [x] **Step 3: Write SurfaceInventory**

The Scala interop below is the measured working form: `GorPipeCommands.commandMap()`
returns a Scala `Map`, iterated via `.iterator()` yielding `scala.Tuple2`.

Create `compat/src/main/java/org/gorpipe/compat/SurfaceInventory.java`:

```java
package org.gorpipe.compat;

import gorsat.Commands.CommandArguments;
import gorsat.Commands.CommandInfo;
import gorsat.parser.FunctionRegistry;
import gorsat.process.GorPipeCommands;
import gorsat.process.PipeInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The language surface, read from the live registries.
 *
 * The registries are populated by instantiating command classes
 * (addInfo(new gorsat.Commands.Join)), so neither names nor flag specifications
 * can be grepped out of source — they exist only at runtime.
 */
public final class SurfaceInventory {

    /** One command's declared argument surface. */
    public static final class CommandSurface {
        public final String name;
        public final List<String> valuelessFlags;
        public final List<String> valueFlags;
        public final int minArgs;
        public final int maxArgs;

        CommandSurface(String name, List<String> valuelessFlags, List<String> valueFlags,
                       int minArgs, int maxArgs) {
            this.name = name;
            this.valuelessFlags = Collections.unmodifiableList(valuelessFlags);
            this.valueFlags = Collections.unmodifiableList(valueFlags);
            this.minArgs = minArgs;
            this.maxArgs = maxArgs;
        }

        public List<String> allFlags() {
            List<String> all = new ArrayList<>(valuelessFlags);
            all.addAll(valueFlags);
            return all;
        }
    }

    private final Map<String, CommandSurface> commands;
    private final Set<String> functionNames;

    private SurfaceInventory(Map<String, CommandSurface> commands, Set<String> functionNames) {
        this.commands = Collections.unmodifiableMap(commands);
        this.functionNames = Collections.unmodifiableSet(functionNames);
    }

    public static SurfaceInventory read() {
        PipeInstance.initialize();

        Map<String, CommandSurface> commands = new TreeMap<>();
        scala.collection.Iterator<scala.Tuple2<String, CommandInfo>> it =
                GorPipeCommands.commandMap().iterator();
        while (it.hasNext()) {
            scala.Tuple2<String, CommandInfo> entry = it.next();
            String name = entry._1();
            CommandArguments args = entry._2().commandArguments();
            commands.put(name, new CommandSurface(
                    name,
                    splitFlags(args.options()),
                    splitFlags(args.valueOptions()),
                    args.minimumNumberOfArguments(),
                    args.maximumNumberOfArguments()));
        }

        return new SurfaceInventory(commands, new TreeSet<>(FunctionRegistry.functionNames()));
    }

    /** Flags are declared as one space-separated string, e.g. "-snpsnp -segseg -l". */
    private static List<String> splitFlags(String declaration) {
        List<String> flags = new ArrayList<>();
        if (declaration == null) {
            return flags;
        }
        for (String token : declaration.trim().split("\\s+")) {
            if (!token.isEmpty()) {
                flags.add(token);
            }
        }
        Collections.sort(flags);
        return flags;
    }

    public Map<String, CommandSurface> commands() {
        return commands;
    }

    public Set<String> functionNames() {
        return functionNames;
    }

    public int totalFlagCount() {
        int total = 0;
        for (CommandSurface c : commands.values()) {
            total += c.valuelessFlags.size() + c.valueFlags.size();
        }
        return total;
    }

    /**
     * Stable JSON, hand-rendered rather than via Jackson so that key order and
     * whitespace are fully determined and the committed file diffs cleanly.
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"commands\": {\n");
        int ci = 0;
        for (CommandSurface c : commands.values()) {
            sb.append("    \"").append(c.name).append("\": {\n");
            sb.append("      \"valuelessFlags\": ").append(jsonArray(c.valuelessFlags)).append(",\n");
            sb.append("      \"valueFlags\": ").append(jsonArray(c.valueFlags)).append(",\n");
            sb.append("      \"minArgs\": ").append(c.minArgs).append(",\n");
            sb.append("      \"maxArgs\": ").append(c.maxArgs).append('\n');
            sb.append("    }").append(++ci < commands.size() ? "," : "").append('\n');
        }
        sb.append("  },\n  \"functions\": ").append(jsonArray(new ArrayList<>(functionNames)));
        sb.append('\n').append("}\n");
        return sb.toString();
    }

    private static String jsonArray(List<String> values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append('"')
              .append(values.get(i).replace("\\", "\\\\").replace("\"", "\\\""))
              .append('"');
        }
        return sb.append(']').toString();
    }
}
```

- [x] **Step 4: Run the inventory test**

```bash
./gradlew :compat:test --tests "org.gorpipe.compat.UTestSurfaceInventory"
```

Expected: PASS, 6 tests.

- [x] **Step 5: Write InventoryMain and its Gradle task**

Create `compat/src/main/java/org/gorpipe/compat/InventoryMain.java`:

```java
package org.gorpipe.compat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes inventory/surface.json from the live registries. */
public final class InventoryMain {

    private InventoryMain() {
    }

    public static void main(String[] args) {
        SurfaceInventory inventory = SurfaceInventory.read();
        Path out = CaseLoader.moduleRoot().resolve("inventory/surface.json");
        try {
            Files.createDirectories(out.getParent());
            Files.writeString(out, inventory.toJson(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write " + out, e);
        }
        System.out.printf("Wrote %s: %d commands, %d flags, %d functions.%n",
                out, inventory.commands().size(), inventory.totalFlagCount(),
                inventory.functionNames().size());
    }
}
```

Add to `compat/build.gradle` inside the `project(':compat')` block:

```groovy
    tasks.register('inventory', JavaExec) {
        group = 'compat'
        description = 'Regenerate inventory/surface.json from the live registries'
        mainClass = 'org.gorpipe.compat.InventoryMain'
        classpath = sourceSets.main.runtimeClasspath
        workingDir = projectDir
    }
```

- [x] **Step 6: Generate and inspect the inventory**

```bash
./gradlew :compat:inventory
python3 -c "
import json
d = json.load(open('compat/inventory/surface.json'))
print('commands', len(d['commands']))
print('functions', len(d['functions']))
print('flags', sum(len(c['valuelessFlags']) + len(c['valueFlags']) for c in d['commands'].values()))
"
```

Expected: roughly 108 commands and 459 flags. Record the function count — it was not
measurable before Task 9.

- [x] **Step 7: Add the staleness gate**

Create `compat/src/test/java/org/gorpipe/compat/UTestInventoryFreshness.java`:

```java
package org.gorpipe.compat;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The committed inventory must match the live registries. When a command gains a
 * flag this fails until someone regenerates, which is how new surface becomes a
 * visible diff in review rather than silently uncovered.
 */
public class UTestInventoryFreshness {

    @Test
    public void committedInventoryMatchesRegistries() {
        Path committed = CaseLoader.moduleRoot().resolve("inventory/surface.json");
        Assert.assertTrue("inventory/surface.json is missing; run ./gradlew :compat:inventory",
                Files.exists(committed));

        String onDisk;
        try {
            onDisk = Files.readString(committed, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        if (!SurfaceInventory.read().toJson().equals(onDisk)) {
            Assert.fail("inventory/surface.json is stale — the language surface changed.\n"
                    + "  Run ./gradlew :compat:inventory and commit the result, then add "
                    + "cases for any new surface.");
        }
    }
}
```

- [x] **Step 8: Verify and commit**

```bash
./gradlew :compat:test
```

Expected: PASS.

```bash
git add compat/src/main/java/org/gorpipe/compat/SurfaceInventory.java \
        compat/src/main/java/org/gorpipe/compat/InventoryMain.java \
        compat/src/test/java/org/gorpipe/compat/UTestSurfaceInventory.java \
        compat/src/test/java/org/gorpipe/compat/UTestInventoryFreshness.java \
        compat/inventory/surface.json \
        compat/build.gradle
git commit -m "feat(compat): add runtime surface inventory with staleness gate"
```

---

### Task 11: G1 — the flag matrix generator

Spec §8/G1. This is the spine: 108 commands and 459 flags become baseline cases,
regenerated from the registry so that new surface appears the day it lands.

Two limits are designed in rather than papered over. A value-taking flag needs a
value the registry does not carry (`JOIN` declares `-maxseg` but not that it wants
an integer), so a curated `inventory/flag-values.yml` supplies them and any flag
with no entry is reported as a gap rather than emitted as a broken case. And 242 of
459 flags are value-taking, so that file is the real manual residue of this
generator — expect it to grow over time, not to be finished in one sitting.

**Files:**
- Create: `compat/src/main/java/org/gorpipe/compat/gen/FlagValues.java`
- Create: `compat/src/main/java/org/gorpipe/compat/gen/CaseWriter.java`
- Create: `compat/src/main/java/org/gorpipe/compat/gen/FlagMatrixGenerator.java`
- Create: `compat/inventory/flag-values.yml`
- Modify: `compat/build.gradle`
- Test: `compat/src/test/java/org/gorpipe/compat/gen/UTestFlagValues.java`
- Test: `compat/src/test/java/org/gorpipe/compat/gen/UTestFlagMatrixGenerator.java`

**Interfaces:**
- Consumes: `SurfaceInventory`, `Fixtures.canonicalInputs()`, `CompatCase`
- Produces:
  - `FlagValues.load() -> FlagValues`
  - `FlagValues.valueFor(String command, String flag) -> String` (null when unmapped)
  - `FlagValues.has(String command, String flag) -> boolean`
  - `CaseWriter.writeFeatureFile(Path file, List<CompatCase> cases) -> void` — emits YAML, sorted by id
  - `FlagMatrixGenerator.generate(SurfaceInventory inv, FlagValues values) -> GenerationResult`
  - `GenerationResult` with public final fields `List<CompatCase> cases` and `List<String> unmappedValueFlags`

- [x] **Step 1: Write the failing flag-values test**

Create `compat/src/test/java/org/gorpipe/compat/gen/UTestFlagValues.java`:

```java
package org.gorpipe.compat.gen;

import org.junit.Assert;
import org.junit.Test;

public class UTestFlagValues {

    @Test
    public void loadsCuratedValues() {
        FlagValues values = FlagValues.load();
        Assert.assertTrue("expected a curated value for JOIN -maxseg",
                values.has("JOIN", "-maxseg"));
        Assert.assertEquals("1000", values.valueFor("JOIN", "-maxseg"));
    }

    @Test
    public void reportsUnmappedFlagsAsAbsentRatherThanThrowing() {
        FlagValues values = FlagValues.load();
        Assert.assertFalse(values.has("JOIN", "-nosuchflagever"));
        Assert.assertNull(values.valueFor("JOIN", "-nosuchflagever"));
    }

    @Test
    public void fallsBackToAWildcardEntryWhenCommandIsUnlisted() {
        // A flag name that means the same thing across commands can be mapped once
        // under the '*' key rather than repeated for all 108 commands.
        FlagValues values = FlagValues.load();
        Assert.assertTrue("expected a wildcard entry for -s", values.has("SOMEUNLISTEDCMD", "-s"));
    }
}
```

- [x] **Step 2: Write the curated value file**

Create `compat/inventory/flag-values.yml`. This is a seed, not a complete mapping —
242 value-taking flags exist and this covers the common ones. Everything unmapped is
reported by the gap report in Task 13.

```yaml
# Sample values for value-taking flags, used by the flag matrix generator.
# Keys are command names; '*' supplies a default for a flag name that means the
# same thing across commands. A flag with no entry here is reported as a gap and
# no case is generated for it — never guessed at.
"*":
  "-s": "Gene"
  "-p": "chr1:1-1000"
  "-e": "0"
  "-c": "Chrom"
  "-gc": "Chrom"
  "-sc": "Chrom"
  "-f": "1"
  "-n": "1"
  "-b": "100"
  "-span": "100"
  "-sep": ","

JOIN:
  "-maxseg": "1000"
  "-rprefix": "r"
  "-lstop": "0"
  "-rstop": "0"
  "-xl": "Chrom"
  "-xr": "Chrom"

GROUP:
  "-gc": "Chrom"
  "-sc": "Ref"
```

- [x] **Step 3: Run to verify the test fails**

```bash
./gradlew :compat:test --tests "org.gorpipe.compat.gen.UTestFlagValues"
```

Expected: FAIL — `FlagValues` does not exist.

- [x] **Step 4: Write FlagValues**

Create `compat/src/main/java/org/gorpipe/compat/gen/FlagValues.java`:

```java
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
```

- [x] **Step 5: Run the flag-values test**

```bash
./gradlew :compat:test --tests "org.gorpipe.compat.gen.UTestFlagValues"
```

Expected: PASS, 3 tests.

- [x] **Step 6: Write the failing generator test**

Create `compat/src/test/java/org/gorpipe/compat/gen/UTestFlagMatrixGenerator.java`:

```java
package org.gorpipe.compat.gen;

import org.gorpipe.compat.CompatCase;
import org.gorpipe.compat.SurfaceInventory;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

public class UTestFlagMatrixGenerator {

    private static FlagMatrixGenerator.GenerationResult result;

    @BeforeClass
    public static void generate() {
        result = FlagMatrixGenerator.generate(SurfaceInventory.read(), FlagValues.load());
    }

    @Test
    public void generatesCasesForValuelessFlags() {
        List<String> ids = result.cases.stream().map(c -> c.id).collect(Collectors.toList());
        Assert.assertTrue(ids.stream().anyMatch(id -> id.startsWith("cmd.join.flag_snpsnp")));
    }

    @Test
    public void everyGeneratedCaseIsBaselineTierWithNoInlineExpected() {
        for (CompatCase c : result.cases) {
            Assert.assertEquals("generated cases must be baseline tier", "baseline", c.tier);
            Assert.assertNull("generated cases must carry no inline expected", c.expected);
            Assert.assertTrue("generated cases must have a query", c.query.length() > 0);
        }
    }

    @Test
    public void generatedIdsAreUniqueAndWellFormed() {
        List<String> ids = result.cases.stream().map(c -> c.id).collect(Collectors.toList());
        Assert.assertEquals("generated ids must be unique",
                ids.size(), ids.stream().distinct().count());
        for (String id : ids) {
            Assert.assertTrue("id violates the required pattern: " + id,
                    id.matches("^[a-z0-9]+(\\.[a-z0-9_]+)+$"));
        }
    }

    @Test
    public void unmappedValueFlagsAreReportedNotSkippedSilently() {
        // 242 value-taking flags exist and the seed mapping covers only common
        // ones, so this list must be non-empty and every entry must name a flag.
        Assert.assertFalse("expected unmapped value flags to be reported",
                result.unmappedValueFlags.isEmpty());
        for (String entry : result.unmappedValueFlags) {
            Assert.assertTrue(entry, entry.contains("-"));
        }
    }

    @Test
    public void generatesNoCaseForAnUnmappedValueFlag() {
        List<String> ids = result.cases.stream().map(c -> c.id).collect(Collectors.toList());
        for (String entry : result.unmappedValueFlags) {
            String flag = entry.substring(entry.lastIndexOf(' ') + 1);
            String suffix = "flag_" + flag.substring(1).toLowerCase();
            Assert.assertTrue("a case was generated for the unmapped flag " + entry,
                    ids.stream().noneMatch(id -> id.endsWith(suffix)));
        }
    }

    @Test
    public void producesCasesForALargeShareOfTheSurface() {
        // 108 commands with 217 valueless flags; a healthy run generates hundreds
        // of cases. A low number means flag parsing regressed.
        Assert.assertTrue("expected at least 200 generated cases, got " + result.cases.size(),
                result.cases.size() >= 200);
    }
}
```

- [x] **Step 7: Run to verify it fails**

```bash
./gradlew :compat:test --tests "org.gorpipe.compat.gen.UTestFlagMatrixGenerator"
```

Expected: FAIL — `FlagMatrixGenerator` does not exist.

- [x] **Step 8: Write CaseWriter**

Create `compat/src/main/java/org/gorpipe/compat/gen/CaseWriter.java`:

```java
package org.gorpipe.compat.gen;

import org.gorpipe.compat.CompatCase;
import org.gorpipe.compat.CompatInput;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/**
 * Writes generated cases as YAML.
 *
 * Hand-rendered rather than emitted by SnakeYAML so that the output is stable,
 * readable and diffs cleanly — a generated file that reorders itself between runs
 * makes every regeneration look like a change.
 */
public final class CaseWriter {

    private CaseWriter() {
    }

    public static void writeFeatureFile(Path file, List<CompatCase> cases) {
        List<CompatCase> sorted = new java.util.ArrayList<>(cases);
        sorted.sort(Comparator.comparing(c -> c.id));

        StringBuilder sb = new StringBuilder();
        sb.append("# Generated by :compat:generate — do not hand-edit.\n");
        sb.append("# Expected outputs for these cases live in baselines/.\n");
        for (CompatCase c : sorted) {
            sb.append("- id: ").append(c.id).append('\n');
            sb.append("  tier: ").append(c.tier).append('\n');
            sb.append("  mode: ").append(c.mode).append('\n');
            if (c.needsReference) {
                sb.append("  needsReference: true\n");
            }
            if (c.behavior != null) {
                sb.append("  behavior: ").append(quote(c.behavior)).append('\n');
            }
            sb.append("  query: ").append(quote(c.query)).append('\n');
            if (!c.inputs.isEmpty()) {
                sb.append("  inputs:\n");
                for (CompatInput in : c.inputs) {
                    sb.append("    - path: ").append(quote(in.path)).append('\n');
                    sb.append("      content: ").append(quote(in.content)).append('\n');
                }
            }
        }

        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write case file " + file, e);
        }
    }

    /**
     * Double-quoted YAML scalar. Tabs and newlines are escaped so that a fixture
     * body survives the round trip byte for byte, which a block scalar would not
     * guarantee for trailing whitespace.
     */
    private static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char ch : s.toCharArray()) {
            switch (ch) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\t': sb.append("\\t"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                default: sb.append(ch);
            }
        }
        return sb.append('"').toString();
    }
}
```

- [x] **Step 9: Write FlagMatrixGenerator**

Create `compat/src/main/java/org/gorpipe/compat/gen/FlagMatrixGenerator.java`:

```java
package org.gorpipe.compat.gen;

import org.gorpipe.compat.CompatCase;
import org.gorpipe.compat.CompatInput;
import org.gorpipe.compat.Fixtures;
import org.gorpipe.compat.SurfaceInventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Emits one baseline case per command flag, plus a bare invocation per command.
 *
 * Recomputed from the registry rather than maintained by hand, so a command that
 * gains a flag produces a new case — and therefore a missing baseline and a failing
 * build — the day the flag lands.
 */
public final class FlagMatrixGenerator {

    /** Generated cases plus what could not be generated and why. */
    public static final class GenerationResult {
        public final List<CompatCase> cases;
        public final List<String> unmappedValueFlags;

        GenerationResult(List<CompatCase> cases, List<String> unmappedValueFlags) {
            this.cases = Collections.unmodifiableList(cases);
            this.unmappedValueFlags = Collections.unmodifiableList(unmappedValueFlags);
        }
    }

    /**
     * Commands that cannot run hermetically in a generated case: they shell out,
     * touch a database, or block. Excluded here rather than allowed to produce
     * noise; the exclusions file in Task 13 records the reasoning.
     */
    private static final Set<String> SKIP_COMMANDS = new HashSet<>(Arrays.asList(
            "CMD", "SQL", "BINARYWRITE", "WRITE", "TEE", "WAIT", "BUG", "LOG",
            "LOGLEVEL", "ROOTLOGLEVEL", "GORSQL", "NORSQL", "PGOR", "PARTGOR"));

    private FlagMatrixGenerator() {
    }

    public static GenerationResult generate(SurfaceInventory inventory, FlagValues values) {
        List<CompatCase> cases = new ArrayList<>();
        List<String> unmapped = new ArrayList<>();

        for (SurfaceInventory.CommandSurface command : inventory.commands().values()) {
            if (SKIP_COMMANDS.contains(command.name)) {
                continue;
            }

            for (String flag : command.valuelessFlags) {
                cases.add(caseFor(command, flag, null));
            }
            for (String flag : command.valueFlags) {
                String value = values.valueFor(command.name, flag);
                if (value == null) {
                    unmapped.add(command.name + " " + flag);
                    continue;
                }
                cases.add(caseFor(command, flag, value));
            }
        }
        return new GenerationResult(cases, unmapped);
    }

    private static CompatCase caseFor(SurfaceInventory.CommandSurface command,
                                      String flag, String value) {
        CompatCase c = new CompatCase();
        c.id = "cmd." + command.name.toLowerCase(Locale.ROOT)
                + ".flag_" + flag.substring(1).toLowerCase(Locale.ROOT);
        c.tier = "baseline";
        c.mode = "exact";
        c.behavior = "Generated: " + command.name + " with " + flag;
        c.query = buildQuery(command, flag, value);
        c.inputs.addAll(requiredInputs(command));
        return c;
    }

    /**
     * A minimal invocation: read the canonical gor fixture, apply the command with
     * one flag, and cap the output so a baseline stays small.
     *
     * Positional arguments are supplied when the command declares a minimum, using
     * the canonical right-hand fixture. Whether the result succeeds or errors is
     * not this generator's concern — either outcome is a behaviour worth pinning.
     */
    private static String buildQuery(SurfaceInventory.CommandSurface command,
                                     String flag, String value) {
        StringBuilder q = new StringBuilder("gor ${ROOT}/left.gor | ");
        q.append(command.name).append(' ').append(flag);
        if (value != null) {
            q.append(' ').append(value);
        }
        if (command.minArgs > 0) {
            q.append(" ${ROOT}/right.gor");
        }
        q.append(" | top 5");
        return q.toString();
    }

    private static List<CompatInput> requiredInputs(SurfaceInventory.CommandSurface command) {
        List<CompatInput> all = Fixtures.canonicalInputs();
        List<CompatInput> needed = new ArrayList<>();
        for (CompatInput in : all) {
            if (in.path.equals("left.gor")
                    || (command.minArgs > 0 && in.path.equals("right.gor"))) {
                needed.add(in);
            }
        }
        return needed;
    }
}
```

- [x] **Step 10: Run the generator test**

```bash
./gradlew :compat:test --tests "org.gorpipe.compat.gen.UTestFlagMatrixGenerator"
```

Expected: PASS, 6 tests.

If `producesCasesForALargeShareOfTheSurface` fails with a low number, print
`inventory.totalFlagCount()` and the size of `SKIP_COMMANDS` — the skip list may be
excluding too much.

- [x] **Step 11: Commit the generator, before wiring it to disk**

```bash
git add compat/src/main/java/org/gorpipe/compat/gen/ \
        compat/inventory/flag-values.yml \
        compat/src/test/java/org/gorpipe/compat/gen/
git commit -m "feat(compat): add flag matrix generator with curated value mapping"
```

---

### Task 12: The `generate` task and its staleness gate

Spec §9. `generate` writes case files; `accept` writes baselines. They touch
disjoint sets of files so that regenerating never rewrites an output and accepting
never rewrites a query.

**Files:**
- Create: `compat/src/main/java/org/gorpipe/compat/gen/GenerateMain.java`
- Modify: `compat/build.gradle`
- Test: `compat/src/test/java/org/gorpipe/compat/UTestGeneratedCorpusFreshness.java`

**Interfaces:**
- Consumes: `FlagMatrixGenerator`, `FlagValues`, `SurfaceInventory`, `CaseWriter`
- Produces:
  - `GenerateMain.main(String[])` — writes `cases/baseline/**` and `inventory/gaps.txt`
  - `GenerateMain.generateAll() -> Map<Path, List<CompatCase>>` — the intended on-disk state, for the freshness test to compare against without writing

- [x] **Step 1: Write GenerateMain**

Create `compat/src/main/java/org/gorpipe/compat/gen/GenerateMain.java`:

```java
package org.gorpipe.compat.gen;

import org.gorpipe.compat.CaseLoader;
import org.gorpipe.compat.CompatCase;
import org.gorpipe.compat.SurfaceInventory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Regenerates the baseline case corpus from the registry and the documentation.
 *
 * Writes only case files and the gap report. Baseline outputs are the business of
 * :compat:accept, so that a regeneration never silently rewrites a recorded
 * behaviour.
 */
public final class GenerateMain {

    private GenerateMain() {
    }

    /** The intended on-disk case files, keyed by path. Does not write anything. */
    public static Map<Path, List<CompatCase>> generateAll() {
        SurfaceInventory inventory = SurfaceInventory.read();
        FlagValues values = FlagValues.load();

        List<CompatCase> generated = new ArrayList<>(
                FlagMatrixGenerator.generate(inventory, values).cases);
        generated.addAll(DocHarvester.harvest().cases);

        Map<Path, List<CompatCase>> byFile = new TreeMap<>();
        Path caseRoot = CaseLoader.moduleRoot().resolve("cases/baseline");
        for (CompatCase c : generated) {
            Path file = caseRoot.resolve(c.category()).resolve(c.feature() + ".yml");
            byFile.computeIfAbsent(file, k -> new ArrayList<>()).add(c);
        }
        return byFile;
    }

    public static void main(String[] args) {
        SurfaceInventory inventory = SurfaceInventory.read();
        FlagValues values = FlagValues.load();
        FlagMatrixGenerator.GenerationResult flags =
                FlagMatrixGenerator.generate(inventory, values);
        DocHarvester.HarvestResult docs = DocHarvester.harvest();

        Map<Path, List<CompatCase>> byFile = generateAll();
        for (Map.Entry<Path, List<CompatCase>> e : byFile.entrySet()) {
            CaseWriter.writeFeatureFile(e.getKey(), e.getValue());
        }

        writeGapReport(flags, docs);

        System.out.printf("Generated %d baseline cases across %d files.%n",
                byFile.values().stream().mapToInt(List::size).sum(), byFile.size());
        System.out.printf("  flag matrix:  %d cases, %d value-flags unmapped%n",
                flags.cases.size(), flags.unmappedValueFlags.size());
        System.out.printf("  doc harvest:  %d cases, %d snippets skipped%n",
                docs.cases.size(), docs.skipped.size());
        System.out.println("Next: ./gradlew :compat:accept to record baselines for new cases.");
    }

    private static void writeGapReport(FlagMatrixGenerator.GenerationResult flags,
                                       DocHarvester.HarvestResult docs) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Generated by :compat:generate. Each line is surface with no case.\n\n");

        sb.append("## Value-taking flags with no entry in inventory/flag-values.yml (")
          .append(flags.unmappedValueFlags.size()).append(")\n");
        for (String entry : flags.unmappedValueFlags) {
            sb.append(entry).append('\n');
        }

        sb.append("\n## Documentation snippets skipped (")
          .append(docs.skipped.size()).append(")\n");
        for (String entry : docs.skipped) {
            sb.append(entry).append('\n');
        }

        Path out = CaseLoader.moduleRoot().resolve("inventory/gaps.txt");
        try {
            Files.createDirectories(out.getParent());
            Files.writeString(out, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write " + out, e);
        }
    }
}
```

Note: `GenerateMain` references `DocHarvester`, which Task 13 creates. Add a
minimal version now so this task compiles, and let Task 13 replace it:

Create `compat/src/main/java/org/gorpipe/compat/gen/DocHarvester.java`:

```java
package org.gorpipe.compat.gen;

import org.gorpipe.compat.CompatCase;

import java.util.Collections;
import java.util.List;

/** Placeholder replaced in Task 13 with the real documentation harvester. */
public final class DocHarvester {

    public static final class HarvestResult {
        public final List<CompatCase> cases;
        public final List<String> skipped;

        HarvestResult(List<CompatCase> cases, List<String> skipped) {
            this.cases = Collections.unmodifiableList(cases);
            this.skipped = Collections.unmodifiableList(skipped);
        }
    }

    private DocHarvester() {
    }

    public static HarvestResult harvest() {
        return new HarvestResult(Collections.emptyList(), Collections.emptyList());
    }
}
```

- [x] **Step 2: Register the Gradle task**

Add to `compat/build.gradle` inside the `project(':compat')` block:

```groovy
    tasks.register('generate', JavaExec) {
        group = 'compat'
        description = 'Regenerate the baseline case corpus from the registry and docs'
        mainClass = 'org.gorpipe.compat.gen.GenerateMain'
        classpath = sourceSets.main.runtimeClasspath
        workingDir = projectDir
    }
```

- [x] **Step 3: Generate the corpus for the first time**

```bash
./gradlew :compat:generate
find compat/cases/baseline -name '*.yml' | wc -l
grep -c "^- id:" compat/cases/baseline/cmd/*.yml | head -5
head -30 compat/inventory/gaps.txt
```

Expected: several dozen case files, a few hundred cases, and a gap report listing
the unmapped value flags.

- [x] **Step 4: Record baselines and see how many cases actually run**

```bash
./gradlew :compat:accept 2>&1 | tail -5
```

Expected: a line reporting the number of cases accepted, all as new.

Many generated queries will error — a flag applied to a mismatched input often
does. That is fine and intended: `ERROR: <message>` is a recorded behaviour, and a
change in that message is a compatibility change worth catching. Confirm the mix:

```bash
python3 - <<'PY'
import glob, re
ok = err = 0
for f in glob.glob('compat/baselines/**/*.out', recursive=True):
    for block in open(f).read().split('>>> ')[1:]:
        body = block.split('\n', 1)[1]
        if body.startswith('ERROR: '):
            err += 1
        else:
            ok += 1
print('successful:', ok, 'error:', err)
PY
```

Record both numbers. A corpus that is almost entirely errors means `buildQuery` is
producing nonsense and should be improved before the corpus is committed.

- [x] **Step 5: Run the full suite**

```bash
./gradlew :compat:test
```

Expected: PASS. Every generated case now has a baseline, and lint passes over the
whole corpus.

If lint reports duplicate bodies, two flags produced an identical query — most
likely because a flag was declared in both `options` and `valueOptions`. Report the
duplicate rather than deleting the lint rule.

- [x] **Step 6: Add the corpus freshness gate**

Create `compat/src/test/java/org/gorpipe/compat/UTestGeneratedCorpusFreshness.java`:

```java
package org.gorpipe.compat;

import org.gorpipe.compat.gen.GenerateMain;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * The committed generated corpus must match what the generators produce now.
 *
 * Without this, a command losing a flag would quietly remove its coverage with
 * nothing in the diff to show it.
 */
public class UTestGeneratedCorpusFreshness {

    @Test
    public void committedGeneratedCasesMatchTheGenerators() {
        Map<Path, List<CompatCase>> intended = GenerateMain.generateAll();

        Set<String> intendedIds = new TreeSet<>();
        for (List<CompatCase> cases : intended.values()) {
            intendedIds.addAll(cases.stream().map(c -> c.id).collect(Collectors.toList()));
        }

        Set<String> onDiskIds = CaseLoader.loadAll().stream()
                .filter(CompatCase::isBaseline)
                .map(c -> c.id)
                .collect(Collectors.toCollection(TreeSet::new));

        List<String> missing = new ArrayList<>(intendedIds);
        missing.removeAll(onDiskIds);
        List<String> extra = new ArrayList<>(onDiskIds);
        extra.removeAll(intendedIds);

        if (!missing.isEmpty() || !extra.isEmpty()) {
            Assert.fail("The committed generated corpus is stale.\n"
                    + "  missing " + missing.size() + " case(s), e.g. "
                    + missing.stream().limit(5).collect(Collectors.toList()) + "\n"
                    + "  extra " + extra.size() + " case(s), e.g. "
                    + extra.stream().limit(5).collect(Collectors.toList()) + "\n"
                    + "  Run ./gradlew :compat:generate then ./gradlew :compat:accept "
                    + "and commit both.");
        }
    }

    @Test
    public void everyBaselineCaseHasACommittedOutput() {
        List<String> withoutBaseline = new ArrayList<>();
        for (CompatCase c : CaseLoader.loadAll()) {
            if (c.isBaseline() && BaselineStore.load(c) == null) {
                withoutBaseline.add(c.id);
            }
        }
        if (!withoutBaseline.isEmpty()) {
            Assert.fail(withoutBaseline.size() + " baseline case(s) have no committed output, "
                    + "e.g. " + withoutBaseline.stream().limit(5).collect(Collectors.toList())
                    + "\n  Run ./gradlew :compat:accept and commit the result.");
        }
    }
}
```

- [x] **Step 7: Verify and commit**

```bash
./gradlew :compat:test
git add compat/src/main/java/org/gorpipe/compat/gen/GenerateMain.java \
        compat/src/main/java/org/gorpipe/compat/gen/DocHarvester.java \
        compat/src/test/java/org/gorpipe/compat/UTestGeneratedCorpusFreshness.java \
        compat/build.gradle \
        compat/cases/baseline/ \
        compat/baselines/ \
        compat/inventory/gaps.txt
git commit -m "feat(compat): add generate task, first baseline corpus and freshness gates"
```

---

### Task 13: G2 — documentation harvest and the flag cross-check

Spec §8/G2. Two products: runnable snippets become cases, and the Options tables on
the 117 command pages are diffed against the registry.

The cross-check is the higher-value half. It finds undocumented flags (registered,
absent from the docs) and phantom flags (documented, not registered) — two classes
of compatibility defect that no test case can find.

**Files:**
- Modify: `compat/src/main/java/org/gorpipe/compat/gen/DocHarvester.java`
- Create: `compat/src/main/java/org/gorpipe/compat/gen/DocFlagCrossCheck.java`
- Create: `compat/inventory/exclusions.yml`
- Test: `compat/src/test/java/org/gorpipe/compat/gen/UTestDocHarvester.java`
- Test: `compat/src/test/java/org/gorpipe/compat/gen/UTestDocFlagCrossCheck.java`

**Interfaces:**
- Consumes: `documentation/src/**/*.rst`, `SurfaceInventory`
- Produces:
  - `DocHarvester.harvest() -> HarvestResult` with public final `List<CompatCase> cases` and `List<String> skipped`
  - `DocHarvester.docRoot() -> Path`
  - `DocFlagCrossCheck.run(SurfaceInventory inv) -> CrossCheckResult` with public final `List<String> undocumented`, `List<String> phantom`, `int pagesParsed`

- [x] **Step 1: Inspect the real documentation shapes before parsing them**

```bash
cd /Users/gisli/work/gor-opensource-third
grep -rn -A6 "code-block:: gor" documentation/src/command/CALC.rst | head -20
grep -rn -B2 -A8 '``-count``' documentation/src/command/GROUP.rst | head -20
```

Read the output. The harvester must match the shapes actually present, not an
assumed format. Note in particular how the Options table delimits flag names and
whether snippets are indented consistently.

- [x] **Step 2: Write the failing harvester test**

Create `compat/src/test/java/org/gorpipe/compat/gen/UTestDocHarvester.java`:

```java
package org.gorpipe.compat.gen;

import org.gorpipe.compat.CompatCase;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.file.Files;

public class UTestDocHarvester {

    private static DocHarvester.HarvestResult result;

    @BeforeClass
    public static void harvest() {
        result = DocHarvester.harvest();
    }

    @Test
    public void findsTheDocumentationTree() {
        Assert.assertTrue("documentation/src not found at " + DocHarvester.docRoot(),
                Files.isDirectory(DocHarvester.docRoot()));
    }

    @Test
    public void harvestsSelfContainedSnippets() {
        // 568 gor code blocks exist across 240 files; the self-contained ones are
        // those rooted in gorrow or norrows.
        Assert.assertFalse("expected at least one harvested snippet", result.cases.isEmpty());
        for (CompatCase c : result.cases) {
            String q = c.query.toLowerCase();
            Assert.assertTrue("harvested query is not self-contained: " + c.query,
                    q.startsWith("gorrow") || q.startsWith("norrows")
                            || q.startsWith("nor ") || q.startsWith("gor "));
        }
    }

    @Test
    public void skipsNonHermeticSnippetsWithAReason() {
        Assert.assertFalse("expected some snippets to be skipped", result.skipped.isEmpty());
        for (String entry : result.skipped) {
            Assert.assertTrue("skip entry must give a reason: " + entry, entry.contains(":"));
        }
    }

    @Test
    public void skipsSnippetsReferencingProjectData() {
        // Snippets using #dbsnp# and similar cannot run hermetically.
        Assert.assertTrue("expected project-data snippets to be reported as skipped",
                result.skipped.stream().anyMatch(s -> s.contains("#")));
    }

    @Test
    public void harvestedCasesAreBaselineTierAndWellFormed() {
        for (CompatCase c : result.cases) {
            Assert.assertEquals("baseline", c.tier);
            Assert.assertNull(c.expected);
            Assert.assertTrue("id violates the required pattern: " + c.id,
                    c.id.matches("^[a-z0-9]+(\\.[a-z0-9_]+)+$"));
            Assert.assertTrue("harvested case should cite its page",
                    c.behavior != null && c.behavior.contains(".rst"));
        }
    }

    @Test
    public void harvestedIdsAreUnique() {
        long distinct = result.cases.stream().map(c -> c.id).distinct().count();
        Assert.assertEquals(result.cases.size(), distinct);
    }
}
```

- [x] **Step 3: Write the real DocHarvester**

Replace `compat/src/main/java/org/gorpipe/compat/gen/DocHarvester.java`:

```java
package org.gorpipe.compat.gen;

import org.gorpipe.compat.CaseLoader;
import org.gorpipe.compat.CompatCase;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Harvests runnable queries from the documentation.
 *
 * A documentation snippet that no longer runs is itself a compatibility defect, so
 * the self-contained ones are worth pinning even though the pages rarely state
 * their output.
 */
public final class DocHarvester {

    public static final class HarvestResult {
        public final List<CompatCase> cases;
        public final List<String> skipped;

        HarvestResult(List<CompatCase> cases, List<String> skipped) {
            this.cases = Collections.unmodifiableList(cases);
            this.skipped = Collections.unmodifiableList(skipped);
        }
    }

    private static final String CODE_BLOCK = "code-block:: gor";

    private DocHarvester() {
    }

    public static Path docRoot() {
        return CaseLoader.moduleRoot().getParent().resolve("documentation/src");
    }

    public static HarvestResult harvest() {
        List<CompatCase> cases = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        Set<String> usedIds = new HashSet<>();

        Path root = docRoot();
        if (!Files.isDirectory(root)) {
            return new HarvestResult(cases, skipped);
        }

        for (Path page : rstFiles(root)) {
            int index = 0;
            for (String snippet : extractSnippets(page)) {
                index++;
                String reason = rejectionReason(snippet);
                String label = root.relativize(page) + " block " + index;
                if (reason != null) {
                    skipped.add(label + ": " + reason);
                    continue;
                }
                CompatCase c = new CompatCase();
                c.id = uniqueId(page, index, usedIds);
                c.tier = "baseline";
                c.mode = "exact";
                c.behavior = "Harvested from documentation/src/" + root.relativize(page);
                c.query = snippet;
                cases.add(c);
            }
        }
        return new HarvestResult(cases, skipped);
    }

    /**
     * Returns why a snippet cannot become a hermetic case, or null when it can.
     *
     * The bar is deliberately high: a snippet that needs project data or is a
     * usage template would produce a case that fails for reasons unrelated to
     * compatibility.
     */
    private static String rejectionReason(String snippet) {
        String lower = snippet.toLowerCase(Locale.ROOT);
        if (snippet.contains("#")) {
            return "references project data (#ref#)";
        }
        if (snippet.contains("...") || snippet.contains("[") || snippet.contains("<")) {
            return "usage template, not a runnable query";
        }
        if (!(lower.startsWith("gorrow") || lower.startsWith("norrows"))) {
            return "not self-contained; needs a data file";
        }
        if (snippet.contains(";")) {
            return "multi-statement script";
        }
        return null;
    }

    private static String uniqueId(Path page, int index, Set<String> used) {
        String base = page.getFileName().toString()
                .replaceAll("\\.rst$", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_");
        String id = "docs." + base + ".block_" + index;
        while (!used.add(id)) {
            id = id + "_x";
        }
        return id;
    }

    /**
     * Extracts the body of each gor code block. reStructuredText delimits a
     * literal block by indentation, so the block ends at the first non-blank line
     * indented no further than the directive.
     */
    private static List<String> extractSnippets(Path page) {
        List<String> snippets = new ArrayList<>();
        List<String> lines = readLines(page);

        for (int i = 0; i < lines.size(); i++) {
            if (!lines.get(i).contains(CODE_BLOCK)) {
                continue;
            }
            int directiveIndent = indentOf(lines.get(i));
            StringBuilder body = new StringBuilder();
            for (int j = i + 1; j < lines.size(); j++) {
                String line = lines.get(j);
                if (line.isBlank()) {
                    continue;
                }
                if (indentOf(line) <= directiveIndent) {
                    break;
                }
                if (body.length() > 0) {
                    body.append(' ');
                }
                body.append(line.trim());
            }
            String snippet = body.toString().trim();
            if (!snippet.isEmpty()) {
                snippets.add(snippet);
            }
        }
        return snippets;
    }

    private static int indentOf(String line) {
        int n = 0;
        while (n < line.length() && line.charAt(n) == ' ') {
            n++;
        }
        return n;
    }

    private static List<String> readLines(Path page) {
        try {
            return Files.readAllLines(page, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read documentation page " + page, e);
        }
    }

    static List<Path> rstFiles(Path root) {
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(p -> p.toString().endsWith(".rst"))
                    .sorted().collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot enumerate documentation under " + root, e);
        }
    }
}
```

- [x] **Step 4: Run the harvester test**

```bash
./gradlew :compat:test --tests "org.gorpipe.compat.gen.UTestDocHarvester"
```

Expected: PASS, 6 tests.

If `harvestsSelfContainedSnippets` finds nothing, the block extraction does not
match the real indentation. Print the first few extracted snippets and adjust
`extractSnippets` — do not relax the test.

- [x] **Step 5: Write the failing cross-check test**

Create `compat/src/test/java/org/gorpipe/compat/gen/UTestDocFlagCrossCheck.java`:

```java
package org.gorpipe.compat.gen;

import org.gorpipe.compat.SurfaceInventory;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class UTestDocFlagCrossCheck {

    private static DocFlagCrossCheck.CrossCheckResult result;

    @BeforeClass
    public static void crossCheck() {
        result = DocFlagCrossCheck.run(SurfaceInventory.read());
    }

    @Test
    public void parsesTheCommandPages() {
        // 117 command pages exist.
        Assert.assertTrue("expected at least 50 command pages parsed, got "
                + result.pagesParsed, result.pagesParsed >= 50);
    }

    @Test
    public void reportsFindingsInABothWaysComparison() {
        // Both directions must be computed. Either list may legitimately be empty
        // on a well-documented codebase, but the totals must be reported.
        Assert.assertNotNull(result.undocumented);
        Assert.assertNotNull(result.phantom);
    }

    @Test
    public void everyFindingNamesACommandAndAFlag() {
        for (String entry : result.undocumented) {
            Assert.assertTrue(entry, entry.contains(" -"));
        }
        for (String entry : result.phantom) {
            Assert.assertTrue(entry, entry.contains(" -"));
        }
    }
}
```

- [x] **Step 6: Write DocFlagCrossCheck**

Create `compat/src/main/java/org/gorpipe/compat/gen/DocFlagCrossCheck.java`:

```java
package org.gorpipe.compat.gen;

import org.gorpipe.compat.SurfaceInventory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Diffs the flags documented on each command page against the flags the registry
 * actually declares.
 *
 * This finds two defects no test case can: a flag that works but is undocumented,
 * and a flag the documentation promises that no longer exists. Reported, not
 * gated — a documentation gap should not block an engine change.
 */
public final class DocFlagCrossCheck {

    public static final class CrossCheckResult {
        /** Registered but absent from the command's Options table. */
        public final List<String> undocumented;
        /** Documented but not registered: a stale page, or a dropped flag. */
        public final List<String> phantom;
        public final int pagesParsed;

        CrossCheckResult(List<String> undocumented, List<String> phantom, int pagesParsed) {
            this.undocumented = Collections.unmodifiableList(undocumented);
            this.phantom = Collections.unmodifiableList(phantom);
            this.pagesParsed = pagesParsed;
        }
    }

    /** Flags in the docs appear as inline literals, e.g. ``-count``. */
    private static final Pattern DOC_FLAG = Pattern.compile("``(-[a-zA-Z][a-zA-Z0-9]*)``");

    private DocFlagCrossCheck() {
    }

    public static CrossCheckResult run(SurfaceInventory inventory) {
        List<String> undocumented = new ArrayList<>();
        List<String> phantom = new ArrayList<>();
        int pagesParsed = 0;

        Path commandDir = DocHarvester.docRoot().resolve("command");
        if (!Files.isDirectory(commandDir)) {
            return new CrossCheckResult(undocumented, phantom, 0);
        }

        for (Path page : DocHarvester.rstFiles(commandDir)) {
            String commandName = page.getFileName().toString()
                    .replaceAll("\\.rst$", "")
                    .toUpperCase(Locale.ROOT);

            SurfaceInventory.CommandSurface surface = inventory.commands().get(commandName);
            if (surface == null) {
                // A page with no matching command. Real, but a different finding
                // from a flag mismatch, so it is not reported here.
                continue;
            }
            pagesParsed++;

            Set<String> documented = documentedFlags(page);
            Set<String> registered = new TreeSet<>(surface.allFlags());

            for (String flag : registered) {
                if (!documented.contains(flag)) {
                    undocumented.add(commandName + " " + flag);
                }
            }
            for (String flag : documented) {
                if (!registered.contains(flag)) {
                    phantom.add(commandName + " " + flag);
                }
            }
        }
        return new CrossCheckResult(undocumented, phantom, pagesParsed);
    }

    private static Set<String> documentedFlags(Path page) {
        Set<String> flags = new TreeSet<>();
        try {
            Matcher m = DOC_FLAG.matcher(Files.readString(page, StandardCharsets.UTF_8));
            while (m.find()) {
                flags.add(m.group(1));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + page, e);
        }
        return flags;
    }
}
```

- [x] **Step 7: Run the cross-check and record what it finds**

```bash
./gradlew :compat:test --tests "org.gorpipe.compat.gen.UTestDocFlagCrossCheck"
```

Expected: PASS, 3 tests.

- [x] **Step 8: Add the exclusions file**

Some surface cannot run hermetically. Determine the list empirically from what the
generated corpus reports as errors, rather than assuming it.

Create `compat/inventory/exclusions.yml`:

```yaml
# Surface deliberately not covered, with a reason for each entry.
# The count is printed on every run and additions are reviewable diffs, so this
# file cannot quietly become a dumping ground.
#
# Populate from the first generated corpus: commands whose every generated case
# errors for an environmental reason rather than a language reason belong here.
- element: cmd.CMD
  reason: "Shells out to an external process; not hermetic"
  ticket: null
- element: cmd.SQL
  reason: "Requires a live database connection"
  ticket: null
- element: cmd.WAIT
  reason: "Blocks on wall-clock time; would make the suite slow and flaky"
  ticket: null
```

- [x] **Step 9: Regenerate with the harvester active, then accept**

```bash
./gradlew :compat:generate 2>&1 | tail -6
./gradlew :compat:accept 2>&1 | tail -3
./gradlew :compat:test
```

Expected: PASS. The generate output now reports doc-harvest counts alongside the
flag matrix, and `inventory/gaps.txt` lists skipped snippets.

- [x] **Step 10: Commit**

```bash
git add compat/src/main/java/org/gorpipe/compat/gen/DocHarvester.java \
        compat/src/main/java/org/gorpipe/compat/gen/DocFlagCrossCheck.java \
        compat/src/test/java/org/gorpipe/compat/gen/UTestDocHarvester.java \
        compat/src/test/java/org/gorpipe/compat/gen/UTestDocFlagCrossCheck.java \
        compat/inventory/exclusions.yml \
        compat/cases/baseline/ compat/baselines/ compat/inventory/gaps.txt
git commit -m "feat(compat): add documentation harvester and doc/registry flag cross-check"
```

---

### Task 14: The diagnostics report

Spec §10.4. Printed every run, gating nothing. This is how the next gap is found;
the gates in Tasks 5, 6, 10 and 12 are what the suite is held to.

**Files:**
- Create: `compat/src/main/java/org/gorpipe/compat/CoverageReport.java`
- Create: `compat/src/main/java/org/gorpipe/compat/ReportMain.java`
- Modify: `compat/build.gradle`
- Test: `compat/src/test/java/org/gorpipe/compat/UTestCoverageReport.java`

**Interfaces:**
- Consumes: `CaseLoader`, `SurfaceInventory`, `DocFlagCrossCheck`, `CompatCase`
- Produces:
  - `CoverageReport.of(List<CompatCase>, SurfaceInventory) -> CoverageReport`
  - `CoverageReport.commandsCovered() -> int`, `.flagsCovered() -> int`, `.functionsCovered() -> int`
  - `CoverageReport.uncoveredCommands() -> List<String>`
  - `CoverageReport.excludedCount() -> int`
  - `CoverageReport.render() -> String`

- [x] **Step 1: Write the failing report test**

Create `compat/src/test/java/org/gorpipe/compat/UTestCoverageReport.java`:

```java
package org.gorpipe.compat;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class UTestCoverageReport {

    private static CompatCase c(String id, String tier, String query) {
        CompatCase x = new CompatCase();
        x.id = id;
        x.tier = tier;
        x.mode = "exact";
        x.query = query;
        return x;
    }

    @Test
    public void attributesCommandsMentionedInQueries() {
        List<CompatCase> cases = new ArrayList<>();
        cases.add(c("cmd.calc.a", "spec", "norrows 1 | calc X 1+1"));

        CoverageReport report = CoverageReport.of(cases, SurfaceInventory.read());
        Assert.assertTrue("CALC should be attributed", report.commandsCovered() >= 1);
        Assert.assertFalse("JOIN should not be attributed",
                report.uncoveredCommands().isEmpty());
    }

    @Test
    public void attributesFlagsAgainstTheOwningCommand() {
        List<CompatCase> cases = new ArrayList<>();
        cases.add(c("cmd.join.a", "baseline", "gor a.gor | join -snpsnp b.gor"));

        CoverageReport report = CoverageReport.of(cases, SurfaceInventory.read());
        Assert.assertTrue("join -snpsnp should be attributed", report.flagsCovered() >= 1);
    }

    @Test
    public void bothTiersCountTowardCoverage() {
        // A baseline case does exercise the code; it just does not vouch for
        // correctness. Coverage is a diagnostic, so it counts both.
        List<CompatCase> specOnly = new ArrayList<>();
        specOnly.add(c("cmd.calc.a", "spec", "norrows 1 | calc X 1+1"));

        List<CompatCase> withBaseline = new ArrayList<>(specOnly);
        withBaseline.add(c("cmd.join.a", "baseline", "gor a.gor | join -snpsnp b.gor"));

        SurfaceInventory inv = SurfaceInventory.read();
        Assert.assertTrue(CoverageReport.of(withBaseline, inv).commandsCovered()
                > CoverageReport.of(specOnly, inv).commandsCovered());
    }

    @Test
    public void renderIncludesEveryHeadlineNumber() {
        List<CompatCase> cases = new ArrayList<>();
        cases.add(c("cmd.calc.a", "spec", "norrows 1 | calc X 1+1"));
        cases.add(c("cmd.join.a", "baseline", "gor a.gor | join -snpsnp b.gor"));

        String out = CoverageReport.of(cases, SurfaceInventory.read()).render();
        Assert.assertTrue(out, out.contains("SPEC"));
        Assert.assertTrue(out, out.contains("BASELINE"));
        Assert.assertTrue(out, out.contains("SURFACE"));
        Assert.assertTrue(out, out.contains("commands"));
        Assert.assertTrue(out, out.contains("EXCLUDED"));
    }
}
```

- [x] **Step 2: Run to verify it fails**

```bash
./gradlew :compat:test --tests "org.gorpipe.compat.UTestCoverageReport"
```

Expected: FAIL — `CoverageReport` does not exist.

- [x] **Step 3: Write CoverageReport**

Create `compat/src/main/java/org/gorpipe/compat/CoverageReport.java`:

```java
package org.gorpipe.compat;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Surface coverage, derived from the corpus rather than declared.
 *
 * Attribution is textual: the query is split on the pipe and the first token of
 * each stage is resolved against the registry. It will over-attribute
 * occasionally (a command name inside a string literal) and under-attribute where
 * a flag arrives indirectly. It is a ledger of what the corpus reaches, not proof
 * that code ran — JaCoCo remains the check on execution.
 */
public final class CoverageReport {

    private final List<CompatCase> cases;
    private final SurfaceInventory inventory;
    private final Set<String> coveredCommands = new TreeSet<>();
    private final Set<String> coveredFlags = new TreeSet<>();
    private final Set<String> coveredFunctions = new TreeSet<>();
    private final int excluded;

    private CoverageReport(List<CompatCase> cases, SurfaceInventory inventory) {
        this.cases = cases;
        this.inventory = inventory;
        this.excluded = countExclusions();
        attribute();
    }

    public static CoverageReport of(List<CompatCase> cases, SurfaceInventory inventory) {
        return new CoverageReport(cases, inventory);
    }

    private void attribute() {
        for (CompatCase c : cases) {
            String upper = c.query.toUpperCase(Locale.ROOT);

            for (String stage : c.query.split("\\|")) {
                String trimmed = stage.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                String first = trimmed.split("\\s+")[0].toUpperCase(Locale.ROOT);
                SurfaceInventory.CommandSurface surface = inventory.commands().get(first);
                if (surface == null) {
                    continue;
                }
                coveredCommands.add(first);
                for (String flag : surface.allFlags()) {
                    // Word-boundary match so that -s does not match -snpsnp.
                    if (trimmed.matches(".*(^|\\s)" + java.util.regex.Pattern.quote(flag)
                            + "($|\\s).*")) {
                        coveredFlags.add(first + " " + flag);
                    }
                }
            }

            for (String fn : inventory.functionNames()) {
                if (upper.contains(fn.toUpperCase(Locale.ROOT) + "(")) {
                    coveredFunctions.add(fn);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private int countExclusions() {
        Path file = CaseLoader.moduleRoot().resolve("inventory/exclusions.yml");
        if (!Files.exists(file)) {
            return 0;
        }
        try (InputStream in = Files.newInputStream(file)) {
            Object parsed = new Yaml().load(in);
            return parsed instanceof List ? ((List<Object>) parsed).size() : 0;
        } catch (IOException e) {
            return 0;
        }
    }

    public int commandsCovered() {
        return coveredCommands.size();
    }

    public int flagsCovered() {
        return coveredFlags.size();
    }

    public int functionsCovered() {
        return coveredFunctions.size();
    }

    public int excludedCount() {
        return excluded;
    }

    public List<String> uncoveredCommands() {
        List<String> uncovered = new ArrayList<>();
        for (String name : inventory.commands().keySet()) {
            if (!coveredCommands.contains(name)) {
                uncovered.add(name);
            }
        }
        return uncovered;
    }

    public String render() {
        long spec = cases.stream().filter(CompatCase::isSpec).count();
        long baseline = cases.stream().filter(CompatCase::isBaseline).count();

        int totalCommands = inventory.commands().size();
        int totalFlags = inventory.totalFlagCount();
        int totalFunctions = inventory.functionNames().size();

        StringBuilder sb = new StringBuilder();
        sb.append("COMPATIBILITY SUITE\n");
        sb.append(String.format(Locale.ROOT, "  SPEC      %5d cases%n", spec));
        sb.append(String.format(Locale.ROOT, "  BASELINE  %5d cases%n", baseline));
        sb.append("  SURFACE\n");
        sb.append(String.format(Locale.ROOT, "    commands   %4d/%-4d  %d gaps%n",
                commandsCovered(), totalCommands, totalCommands - commandsCovered()));
        sb.append(String.format(Locale.ROOT, "    flags      %4d/%-4d  %d gaps%n",
                flagsCovered(), totalFlags, totalFlags - flagsCovered()));
        sb.append(String.format(Locale.ROOT, "    functions  %4d/%-4d  %d gaps%n",
                functionsCovered(), totalFunctions, totalFunctions - functionsCovered()));
        sb.append(String.format(Locale.ROOT, "  EXCLUDED  %d element(s)"
                + " (see inventory/exclusions.yml)%n", excluded));
        return sb.toString();
    }
}
```

- [x] **Step 4: Run the report test**

```bash
./gradlew :compat:test --tests "org.gorpipe.compat.UTestCoverageReport"
```

Expected: PASS, 4 tests.

- [x] **Step 5: Write ReportMain and print the report on every test run**

Create `compat/src/main/java/org/gorpipe/compat/ReportMain.java`:

```java
package org.gorpipe.compat;

import org.gorpipe.compat.gen.DocFlagCrossCheck;

import java.util.List;
import java.util.stream.Collectors;

/** Prints the diagnostics report. Gates nothing. */
public final class ReportMain {

    private ReportMain() {
    }

    public static void main(String[] args) {
        List<CompatCase> cases = CaseLoader.loadAll();
        SurfaceInventory inventory = SurfaceInventory.read();

        System.out.print(CoverageReport.of(cases, inventory).render());

        DocFlagCrossCheck.CrossCheckResult docs = DocFlagCrossCheck.run(inventory);
        System.out.printf("  DOCS      %d undocumented flag(s), %d phantom flag(s),"
                        + " %d page(s) checked%n",
                docs.undocumented.size(), docs.phantom.size(), docs.pagesParsed);

        List<String> uncovered = CoverageReport.of(cases, inventory).uncoveredCommands();
        if (!uncovered.isEmpty()) {
            System.out.println("  NEXT GAPS " + uncovered.stream().limit(10)
                    .collect(Collectors.toList()));
        }
    }
}
```

Add to `compat/build.gradle` inside the `project(':compat')` block:

```groovy
    tasks.register('report', JavaExec) {
        group = 'compat'
        description = 'Print surface coverage and documentation cross-check diagnostics'
        mainClass = 'org.gorpipe.compat.ReportMain'
        classpath = sourceSets.main.runtimeClasspath
        workingDir = projectDir
    }

    tasks.named('test') {
        finalizedBy tasks.named('report')
    }
```

- [x] **Step 6: Run it and record the real numbers**

```bash
./gradlew :compat:report
```

Expected: the full report. Record the coverage figures — they are the first real
measurement of surface coverage and become the worklist for authoring spec cases.

- [x] **Step 7: Verify the whole module and commit**

```bash
./gradlew :compat:test
git add compat/src/main/java/org/gorpipe/compat/CoverageReport.java \
        compat/src/main/java/org/gorpipe/compat/ReportMain.java \
        compat/src/test/java/org/gorpipe/compat/UTestCoverageReport.java \
        compat/build.gradle
git commit -m "feat(compat): add surface coverage and documentation diagnostics report"
```

---

### Task 15: G3 and G4 — coverage-guided generation and grammar fuzzing

Spec §8/G3 and §8/G4. Both are nightly, both behind flags, and both are noisier and
more expensive than the first two generators. They exist to push branch coverage
beyond what the flag matrix reaches.

G4 is seeded from a committed fixed seed. Without that its baselines churn on every
run, which would train reviewers to accept diffs unread — the one habit that would
render the whole baseline tier worthless.

**Files:**
- Create: `compat/src/main/java/org/gorpipe/compat/gen/MutationGenerator.java`
- Create: `compat/src/main/java/org/gorpipe/compat/gen/GrammarFuzzer.java`
- Create: `compat/inventory/fuzz-seed.txt`
- Modify: `compat/build.gradle`
- Test: `compat/src/test/java/org/gorpipe/compat/gen/UTestMutationGenerator.java`
- Test: `compat/src/test/java/org/gorpipe/compat/gen/UTestGrammarFuzzer.java`

**Interfaces:**
- Consumes: `CompatCase`, `SurfaceInventory`, `CaseLoader`
- Produces:
  - `MutationGenerator.mutate(List<CompatCase> seeds, SurfaceInventory inv, int limit) -> List<CompatCase>`
  - `GrammarFuzzer.fuzzSeed() -> long` — reads `inventory/fuzz-seed.txt`
  - `GrammarFuzzer.generate(SurfaceInventory inv, int count) -> List<CompatCase>`

- [x] **Step 1: Write the failing mutation test**

Create `compat/src/test/java/org/gorpipe/compat/gen/UTestMutationGenerator.java`:

```java
package org.gorpipe.compat.gen;

import org.gorpipe.compat.CompatCase;
import org.gorpipe.compat.SurfaceInventory;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class UTestMutationGenerator {

    private static CompatCase seed(String id, String query) {
        CompatCase c = new CompatCase();
        c.id = id;
        c.tier = "baseline";
        c.mode = "exact";
        c.query = query;
        return c;
    }

    @Test
    public void producesVariantsOfTheSeedQueries() {
        List<CompatCase> seeds = new ArrayList<>();
        seeds.add(seed("cmd.join.base", "gor ${ROOT}/left.gor | join -snpsnp ${ROOT}/right.gor"));

        List<CompatCase> mutated = MutationGenerator.mutate(seeds, SurfaceInventory.read(), 20);

        Assert.assertFalse(mutated.isEmpty());
        Assert.assertTrue("must not exceed the requested limit", mutated.size() <= 20);
        for (CompatCase c : mutated) {
            Assert.assertEquals("baseline", c.tier);
            Assert.assertNotEquals("a mutant must differ from its seed",
                    seeds.get(0).query, c.query);
        }
    }

    @Test
    public void isDeterministicAcrossRuns() {
        List<CompatCase> seeds = new ArrayList<>();
        seeds.add(seed("cmd.join.base", "gor ${ROOT}/left.gor | join -snpsnp ${ROOT}/right.gor"));

        SurfaceInventory inv = SurfaceInventory.read();
        List<CompatCase> first = MutationGenerator.mutate(seeds, inv, 10);
        List<CompatCase> second = MutationGenerator.mutate(seeds, inv, 10);

        Assert.assertEquals(first.size(), second.size());
        for (int i = 0; i < first.size(); i++) {
            Assert.assertEquals("mutation must be reproducible to be diffable",
                    first.get(i).query, second.get(i).query);
            Assert.assertEquals(first.get(i).id, second.get(i).id);
        }
    }

    @Test
    public void generatedIdsAreUniqueAndWellFormed() {
        List<CompatCase> seeds = new ArrayList<>();
        seeds.add(seed("cmd.join.base", "gor ${ROOT}/left.gor | join -snpsnp ${ROOT}/right.gor"));
        seeds.add(seed("cmd.group.base", "gor ${ROOT}/left.gor | group chrom -count"));

        List<CompatCase> mutated = MutationGenerator.mutate(seeds, SurfaceInventory.read(), 30);

        long distinct = mutated.stream().map(c -> c.id).distinct().count();
        Assert.assertEquals(mutated.size(), distinct);
        for (CompatCase c : mutated) {
            Assert.assertTrue("id violates the required pattern: " + c.id,
                    c.id.matches("^[a-z0-9]+(\\.[a-z0-9_]+)+$"));
        }
    }
}
```

- [x] **Step 2: Write MutationGenerator**

Create `compat/src/main/java/org/gorpipe/compat/gen/MutationGenerator.java`:

```java
package org.gorpipe.compat.gen;

import org.gorpipe.compat.CompatCase;
import org.gorpipe.compat.CompatInput;
import org.gorpipe.compat.SurfaceInventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Derives extra baseline cases by mutating existing ones: adding a second flag,
 * chaining another stage, or varying a numeric argument.
 *
 * Fully deterministic — mutants are enumerated in a fixed order rather than drawn
 * at random, so the corpus is reproducible and its diffs are meaningful. The
 * spec describes this as coverage-guided; enumerating deterministically first is
 * the cheaper half, and the JaCoCo filter can be layered on later without
 * changing this interface.
 */
public final class MutationGenerator {

    private MutationGenerator() {
    }

    public static List<CompatCase> mutate(List<CompatCase> seeds,
                                          SurfaceInventory inventory, int limit) {
        List<CompatCase> out = new ArrayList<>();

        for (CompatCase seed : seeds) {
            for (String suffix : new String[]{"top 1", "top 0", "sort 1", "count"}) {
                if (out.size() >= limit) {
                    return out;
                }
                out.add(derive(seed, suffix, "chain_" + slug(suffix)));
            }

            String firstCommand = firstPipeCommand(seed.query);
            SurfaceInventory.CommandSurface surface = inventory.commands().get(firstCommand);
            if (surface != null) {
                for (String flag : surface.valuelessFlags) {
                    if (out.size() >= limit) {
                        return out;
                    }
                    if (seed.query.contains(flag)) {
                        continue;
                    }
                    CompatCase c = copy(seed, "addflag_" + flag.substring(1)
                            .toLowerCase(Locale.ROOT));
                    c.query = seed.query.replaceFirst(
                            "(?i)(\\|\\s*" + firstCommand + ")", "$1 " + flag);
                    if (!c.query.equals(seed.query)) {
                        out.add(c);
                    }
                }
            }
        }
        return out;
    }

    private static CompatCase derive(CompatCase seed, String extraStage, String idSuffix) {
        CompatCase c = copy(seed, idSuffix);
        c.query = seed.query + " | " + extraStage;
        return c;
    }

    private static CompatCase copy(CompatCase seed, String idSuffix) {
        CompatCase c = new CompatCase();
        c.id = seed.id + "_" + idSuffix;
        c.tier = "baseline";
        c.mode = "exact";
        c.behavior = "Mutant of " + seed.id;
        c.needsReference = seed.needsReference;
        c.query = seed.query;
        for (CompatInput in : seed.inputs) {
            CompatInput copy = new CompatInput();
            copy.path = in.path;
            copy.content = in.content;
            copy.contentFile = in.contentFile;
            c.inputs.add(copy);
        }
        return c;
    }

    /** The command name of the second pipeline stage, or empty when there is none. */
    private static String firstPipeCommand(String query) {
        String[] stages = query.split("\\|");
        if (stages.length < 2) {
            return "";
        }
        String stage = stages[1].trim();
        return stage.isEmpty() ? "" : stage.split("\\s+")[0].toUpperCase(Locale.ROOT);
    }

    private static String slug(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
    }
}
```

- [x] **Step 3: Run the mutation test**

```bash
./gradlew :compat:test --tests "org.gorpipe.compat.gen.UTestMutationGenerator"
```

Expected: PASS, 3 tests.

- [x] **Step 4: Write the failing fuzzer test**

Create `compat/src/test/java/org/gorpipe/compat/gen/UTestGrammarFuzzer.java`:

```java
package org.gorpipe.compat.gen;

import org.gorpipe.compat.CompatCase;
import org.gorpipe.compat.SurfaceInventory;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class UTestGrammarFuzzer {

    @Test
    public void readsACommittedSeed() {
        // A committed fixed seed is what keeps fuzz baselines from churning.
        Assert.assertTrue("fuzz seed must be positive", GrammarFuzzer.fuzzSeed() != 0L);
    }

    @Test
    public void isReproducibleForTheSameSeed() {
        SurfaceInventory inv = SurfaceInventory.read();
        List<CompatCase> first = GrammarFuzzer.generate(inv, 25);
        List<CompatCase> second = GrammarFuzzer.generate(inv, 25);

        Assert.assertEquals(first.size(), second.size());
        for (int i = 0; i < first.size(); i++) {
            Assert.assertEquals("fuzzing must be reproducible from the committed seed",
                    first.get(i).query, second.get(i).query);
        }
    }

    @Test
    public void respectsTheRequestedCap() {
        List<CompatCase> cases = GrammarFuzzer.generate(SurfaceInventory.read(), 15);
        Assert.assertTrue("expected at most 15 cases, got " + cases.size(),
                cases.size() <= 15);
        Assert.assertFalse(cases.isEmpty());
    }

    @Test
    public void generatedCasesAreWellFormedBaselineCases() {
        for (CompatCase c : GrammarFuzzer.generate(SurfaceInventory.read(), 20)) {
            Assert.assertEquals("baseline", c.tier);
            Assert.assertNull(c.expected);
            Assert.assertTrue("id violates the required pattern: " + c.id,
                    c.id.matches("^[a-z0-9]+(\\.[a-z0-9_]+)+$"));
            Assert.assertFalse(c.query.isBlank());
        }
    }
}
```

- [x] **Step 5: Write the seed file and GrammarFuzzer**

Create `compat/inventory/fuzz-seed.txt`:

```
# Fixed seed for grammar fuzzing. Changing it regenerates the entire fuzz corpus,
# so change it deliberately and accept the resulting baselines in the same commit.
20260831
```

Create `compat/src/main/java/org/gorpipe/compat/gen/GrammarFuzzer.java`:

```java
package org.gorpipe.compat.gen;

import org.gorpipe.compat.CaseLoader;
import org.gorpipe.compat.CompatCase;
import org.gorpipe.compat.CompatInput;
import org.gorpipe.compat.Fixtures;
import org.gorpipe.compat.SurfaceInventory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Generates structurally varied queries by composing registry commands.
 *
 * Most output is semantically invalid, so most baselines here pin an error
 * message — which is genuine parser-contract coverage. Seeded from a committed
 * fixed seed so the corpus is stable between runs; an unstable fuzz corpus would
 * fill every pull request with meaningless baseline diffs.
 */
public final class GrammarFuzzer {

    private static final long FALLBACK_SEED = 20260831L;

    private GrammarFuzzer() {
    }

    public static long fuzzSeed() {
        Path file = CaseLoader.moduleRoot().resolve("inventory/fuzz-seed.txt");
        if (!Files.exists(file)) {
            return FALLBACK_SEED;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    return Long.parseLong(trimmed);
                }
            }
        } catch (IOException | NumberFormatException e) {
            return FALLBACK_SEED;
        }
        return FALLBACK_SEED;
    }

    public static List<CompatCase> generate(SurfaceInventory inventory, int count) {
        Random random = new Random(fuzzSeed());
        List<String> commandNames = new ArrayList<>(inventory.commands().keySet());

        List<CompatCase> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int stages = 1 + random.nextInt(3);
            StringBuilder query = new StringBuilder("gor ${ROOT}/left.gor");
            for (int s = 0; s < stages; s++) {
                String name = commandNames.get(random.nextInt(commandNames.size()));
                SurfaceInventory.CommandSurface surface = inventory.commands().get(name);
                query.append(" | ").append(name);
                if (!surface.valuelessFlags.isEmpty() && random.nextBoolean()) {
                    query.append(' ').append(surface.valuelessFlags.get(
                            random.nextInt(surface.valuelessFlags.size())));
                }
            }

            CompatCase c = new CompatCase();
            c.id = "fuzz.grammar.case_" + String.format(Locale.ROOT, "%04d", i);
            c.tier = "baseline";
            c.mode = "exact";
            c.behavior = "Grammar fuzz case " + i + " (seed " + fuzzSeed() + ")";
            c.query = query.toString();
            for (CompatInput in : Fixtures.canonicalInputs()) {
                if (in.path.equals("left.gor")) {
                    c.inputs.add(in);
                }
            }
            out.add(c);
        }
        return out;
    }
}
```

- [x] **Step 6: Run the fuzzer test**

```bash
./gradlew :compat:test --tests "org.gorpipe.compat.gen.UTestGrammarFuzzer"
```

Expected: PASS, 4 tests.

- [x] **Step 7: Wire both behind flags in GenerateMain**

In `GenerateMain.generateAll()`, after the doc harvest line, add:

```java
        if (Boolean.getBoolean("compat.nightly")) {
            generated.addAll(MutationGenerator.mutate(
                    new ArrayList<>(generated), inventory, 500));
            generated.addAll(GrammarFuzzer.generate(inventory, 250));
        }
```

Add the nightly task to `compat/build.gradle` inside the `project(':compat')` block:

```groovy
    tasks.register('generateNightly', JavaExec) {
        group = 'compat'
        description = 'Regenerate the corpus including mutation and grammar-fuzz cases'
        mainClass = 'org.gorpipe.compat.gen.GenerateMain'
        classpath = sourceSets.main.runtimeClasspath
        workingDir = projectDir
        systemProperty 'compat.nightly', 'true'
    }
```

- [x] **Step 8: Confirm the PR-lane corpus is unchanged by this task**

The nightly generators must not alter what the PR lane runs:

```bash
./gradlew :compat:generate
git diff --stat compat/cases/baseline/
```

Expected: no diff. If the corpus changed, the nightly guard is not working and
`compat.nightly` is leaking into the default path.

- [x] **Step 9: Commit**

```bash
git add compat/src/main/java/org/gorpipe/compat/gen/MutationGenerator.java \
        compat/src/main/java/org/gorpipe/compat/gen/GrammarFuzzer.java \
        compat/src/main/java/org/gorpipe/compat/gen/GenerateMain.java \
        compat/inventory/fuzz-seed.txt \
        compat/src/test/java/org/gorpipe/compat/gen/UTestMutationGenerator.java \
        compat/src/test/java/org/gorpipe/compat/gen/UTestGrammarFuzzer.java \
        compat/build.gradle
git commit -m "feat(compat): add deterministic mutation and grammar-fuzz generators"
```

---

### Task 16: CI wiring, JaCoCo, and documentation

Spec §10.2, §10.3 and §12. The PR lane runs the spec suite, the baseline diff, lint
and the freshness gates. The nightly lane adds the expensive generators and
coverage, which report and gate nothing.

**Files:**
- Modify: `compat/build.gradle`
- Modify: `.gitlab-ci.yml`
- Create: `docs/compatibility_test_suite.md` (rewrite)
- Create: `compat/README.md`

**Interfaces:**
- Consumes: every task above
- Produces: CI lanes and the documentation someone reads before adding a case

- [x] **Step 1: Measure the actual suite wall time**

Runtime was measured at 8 ms per case before implementation, so a few thousand cases
should finish in well under a minute. Confirm against the real corpus:

```bash
cd /Users/gisli/work/gor-opensource-third
/usr/bin/time -p ./gradlew :compat:test 2>&1 | tail -5
grep -c "^- id:" compat/cases/baseline/*/*.yml compat/cases/spec/*/*.yml | \
  awk -F: '{s+=$2} END {print "total cases:", s}'
```

Record both numbers. If the suite exceeds roughly five minutes, add
`maxParallelForks` to the `test` task before wiring CI:

```groovy
    tasks.named('test') {
        maxParallelForks = Math.max(1, (Runtime.runtime.availableProcessors() / 2) as int)
    }
```

- [x] **Step 2: Add report-only JaCoCo**

Add to `compat/build.gradle`, inside the `project(':compat')` block:

```groovy
    apply plugin: 'jacoco'

    // Coverage is a diagnostic here, never a gate. Gating on a coverage ratio is
    // what produced 381 single-token assertions in the suite this replaces:
    // coverage rewards executing code, not pinning behaviour.
    tasks.register('coverageReport', JacocoReport) {
        group = 'compat'
        description = 'Coverage of the engine by the compatibility corpus (report only)'
        executionData tasks.named('test').get()
        sourceSets project(':gortools').sourceSets.main
        reports {
            xml.required = true
            html.required = true
        }
    }
```

- [x] **Step 3: Verify coverage runs and gates nothing**

```bash
./gradlew :compat:test :compat:coverageReport
python3 - <<'PY'
import re, pathlib
p = pathlib.Path('compat/build/reports/jacoco/coverageReport/coverageReport.xml')
if not p.exists():
    print('report not found at', p); raise SystemExit
text = p.read_text()
for kind in ('INSTRUCTION', 'BRANCH'):
    hits = re.findall(r'type="%s" missed="(\d+)" covered="(\d+)"' % kind, text)
    if hits:
        missed, covered = map(int, hits[-1])
        total = missed + covered
        print(f'{kind}: {covered}/{total} = {100.0*covered/total:.1f}%')
PY
```

Record the instruction and branch percentages. These are the first honest coverage
numbers for the new suite, and they are the input to deciding where to author spec
cases next — not a threshold to defend.

- [x] **Step 4: Wire the CI lanes**

Inspect the existing structure first, then add the jobs in its idiom:

```bash
head -40 .gitlab-ci.yml
```

Add a PR-lane job and a nightly job:

```yaml
compat:
  stage: test
  script:
    - ./gradlew :compat:test
  artifacts:
    when: always
    reports:
      junit: compat/build/test-results/test/TEST-*.xml
    paths:
      - compat/build/reports/tests/test/

compat-nightly:
  stage: test
  rules:
    - if: '$CI_PIPELINE_SOURCE == "schedule"'
  script:
    - ./gradlew :compat:generateNightly
    - ./gradlew :compat:test :compat:coverageReport
  artifacts:
    when: always
    paths:
      - compat/build/reports/jacoco/coverageReport/
      - compat/inventory/gaps.txt
```

The nightly job regenerates with the expensive generators into a throwaway
workspace. It reports; it does not commit.

- [x] **Step 5: Write the module README**

Create `compat/README.md`:

```markdown
# GOR Compatibility Suite

Black-box end-to-end tests over the GOR query language. Every case is a query plus
an expected output. Completely separate from the unit and integration suites: this
module depends on `:gortools` only, never on `:test`.

## The two tiers

| | SPEC | BASELINE |
|---|---|---|
| Written by | a person | a generator |
| Expected output | inline in the case | in `baselines/<category>/<feature>.out` |
| Source of truth | documentation, or human review | the previously committed output |
| Answers | "is this correct?" | "did this change?" |

A spec case is a claim about correct behaviour and must cite where the expectation
came from. A baseline case records what the engine currently does, so that a change
cannot pass unnoticed.

## Commands

```bash
./gradlew :compat:test        # run everything; fails on a spec break or unaccepted diff
./gradlew :compat:generate    # regenerate baseline cases from the registry and docs
./gradlew :compat:accept      # re-record baseline outputs (never runs in CI)
./gradlew :compat:inventory   # regenerate inventory/surface.json
./gradlew :compat:report      # print coverage and documentation diagnostics
```

## When the build fails

**A spec case failed.** The engine no longer does what the case says it should.
Either it is a regression, or the case is wrong — the citation tells you which to
trust.

**A baseline diff.** The failure shows committed versus current output. Read it. If
it is a regression, fix the engine. If the change is intended, run
`./gradlew :compat:accept` and commit the rewritten baselines, so the before/after
lands in the pull request where a reviewer can see it.

**The corpus or inventory is stale.** A command gained or lost a flag. Run
`:compat:generate` then `:compat:accept`, and commit both.

## Adding a spec case

Add an entry to `cases/spec/<category>/<feature>.yml`:

```yaml
- id: cmd.join.snpsnp_basic
  tier: spec
  mode: exact
  source: docs
  cites: ["documentation/src/command/JOIN.rst"]
  query: "gor ${ROOT}/left.gor | join -snpsnp ${ROOT}/right.gor"
  expected: |
    <real tab-separated output>
```

Expected blocks use real tab characters. `./gradlew :compat:report` prints which
surface has no case yet — that is the worklist.

## What gates, and what does not

Gates: a failing spec case, an unaccepted baseline diff, a lint violation, a stale
inventory, or a baseline case with no committed output.

Does not gate: code coverage, or surface completeness. Both are printed on every
run as diagnostics. The suite this replaced gated on a coverage ratio, and 35% of
its cases responded by asserting a single token.
```

- [x] **Step 6: Rewrite the stale top-level documentation**

`docs/compatibility_test_suite.md` describes the removed suite and would mislead
anyone who read it. Replace it with a pointer:

```bash
cat > docs/compatibility_test_suite.md <<'DOC'
# Compatibility Test Suite

The compatibility suite lives in the `:compat` module. See `compat/README.md` for
how it is structured, how to run it, and what to do when it fails.

Design: `docs/superpowers/specs/2026-08-29-gor-compatibility-suite-design.md`

## Summary

Two tiers over one corpus:

- **SPEC** — hand-authored cases with reviewed expected output and a citation.
  These assert correctness and gate the build.
- **BASELINE** — machine-generated cases spanning the command, flag and function
  surface. Their outputs are committed and diffed on every run, so a behaviour
  change fails the build until a human accepts it.

Coverage is reported, never gated. The previous suite gated on a JaCoCo ratio, and
35% of its 1087 cases asserted a single token in response — a JOIN emitting wrong
rows passed them all. That suite also imported a dependency that was never on the
classpath, so it had not compiled or run.
DOC
```

- [x] **Step 7: Full verification**

```bash
./gradlew :compat:test :compat:report
./gradlew :gortools:compileTestJava
git status --short
```

Expected: the suite passes, the report prints, the engine module still compiles, and
the working tree holds only intended changes.

- [x] **Step 8: Commit**

```bash
git add compat/build.gradle compat/README.md \
        .gitlab-ci.yml docs/compatibility_test_suite.md
git commit -m "feat(compat): wire CI lanes, report-only JaCoCo and documentation"
```

---

## Post-Implementation

The suite is now structurally complete. What remains is ongoing work that blocks
nothing:

1. **Author spec cases against the gap report.** `./gradlew :compat:report` names
   the uncovered surface. Every spec case added is one more piece of behaviour that
   is asserted correct rather than merely recorded.
2. **Extend `inventory/flag-values.yml`.** 242 of 459 flags take values, and the
   seed mapping covers only the common ones. Each entry added converts a gap-report
   line into a real case.
3. **Act on the documentation cross-check.** Undocumented flags are a documentation
   fix; phantom flags are either a stale page or a dropped flag, and the second is a
   real compatibility defect.
4. **Fill in the exclusions file** from the commands whose generated cases all fail
   for environmental reasons, with a written reason for each.
5. **Add the JaCoCo filter to G3.** Spec §8/G3 describes coverage-guided generation
   as running candidates under JaCoCo and keeping only those that reach new
   branches. Task 15 implements the cheaper deterministic half — enumerated
   mutants, no coverage feedback. The filter can be layered on without changing
   `MutationGenerator.mutate`'s signature: run each candidate, diff the branch set,
   keep the ones that grow it. Until then, G3 broadens the corpus without
   optimising it.
6. **Extend the inventory to macros and input sources.** `SurfaceInventory` covers
   commands, flags and functions. The registries also hold roughly 17 input sources
   and 5 macros (`GorInputSources.register()`, `GorPipeMacros.register()`), which no
   tier currently attributes or reports. Adding them follows the same shape as
   `commands()`, and they then appear in the gap report like everything else.

One finding from planning is worth acting on independently of this suite: the
measured in-JVM serialisation differs from CLI output for NOR queries — the CLI
strips `ChromNOR`/`PosNOR` and prefixes the header with `#`. This suite pins the
in-JVM shape. If the CLI's shape is also a contract worth protecting, that needs its
own small subprocess-based layer, and it is not in this plan.
