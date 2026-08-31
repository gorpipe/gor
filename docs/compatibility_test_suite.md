# Compatibility Test Suite

## Overview

The compatibility test suite is a data-driven, output-contract test framework
for the GOR query language. Every test case is a self-contained YAML record
that pairs one GOR query with one expected output, exercises exactly one
language behavior, and carries provenance links back to the documentation page
and JUnit test class that motivated it.

The suite is the authoritative answer to the question *"does the GOR query
language still behave as documented?"* after any change to the query engine.

### Key properties

- **One behavior per case.** A case may use multiple pipe steps to *set up*
  the scenario (e.g. `norrows | calc | where`), but the asserted behavior
  must be singular.
- **Deterministic outputs only.** Cases that depend on system state
  (timestamps, random seeds, CPU load) are excluded.
- **Inline-first.** Input fixtures and expected output are kept inline in YAML
  whenever they are small (≤ 5 rows). External files are used only for large
  or binary fixtures.
- **Provenance-required.** Every case must link to the documentation page and
  JUnit class that validates the same behavior.
- **Coverage-gated.** A JaCoCo threshold over the language-execution scope
  prevents regression.

---

## Repository layout

```text
gortools/src/test/
├── java/gorsat/compat/
│   ├── UTestCompatibilitySuite.java      # parameterized JUnit runner
│   └── UTestCompatibilitySuiteLint.java  # structural lint checks
└── resources/compat/
    ├── schema/
    │   └── gor-compat-case.schema.json   # JSON Schema (Draft 2020-12)
    ├── cases/
    │   └── compatibility-cases.yml       # all 1025 test cases
    ├── data/
    │   └── common/                       # shared input fixtures
    └── expected/
        └── command/                      # file-based expected outputs

scripts/
├── backfill_compat_provenance.py         # bulk-adds docs/junit to cases
├── rename_bulk_cases.py                  # renames opaque ids to meaningful ones
├── audit_compat_cases.py                 # removes duplicates; tags multi-step cases
└── cleanup_semantic.py                   # removes non-failing semantic cases
```

---

## Test case format

Every entry in `compatibility-cases.yml` is a YAML mapping with the following
fields.

### Required

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Dot-separated identifier matching `^[a-z0-9]+(\.[a-z0-9_]+)+$`. Must be unique across all cases. |
| `mode` | enum | Comparison mode: `exact`, `contains`, `regex`, or `error`. |
| `query` | string | The GOR query to execute. Use `${ROOT}` as a placeholder for the temporary directory where input fixtures are written. |

### Conditional

| Field | Type | Description |
|-------|------|-------------|
| `expected` | string | Inline expected output. Required when `mode` is not `error`, unless `expectedFile` is provided instead. |
| `expectedFile` | string | Path (relative to `compat/`) to a file containing the expected output. Mutually exclusive with `expected`. |
| `errorContains` | string | Substring the exception message must contain. Only valid when `mode: error`. |
| `inputs` | list | Input fixture files to create before the query runs. Each entry has `path` (relative to `${ROOT}`) and either `content` (inline string) or `contentFile` (path relative to `compat/`). |

### Optional metadata

| Field | Type | Description |
|-------|------|-------------|
| `docs` | list[string] | Documentation files that describe the feature under test. |
| `junit` | list[string] | JUnit test files that cover the same behavior. |
| `category` | string | Derived automatically from the first id segment if omitted. |
| `feature` | string | Derived automatically from the second id segment if omitted. |
| `behavior` | string | Short description of what is asserted. Defaults to the id. Set to `pipeline` to suppress the single-behavior pipe-count lint for intentionally multi-step cases. |

### Example — exact match

```yaml
- id: cmd.calc.add_1_plus_1
  docs:
    - "documentation/src/command/CALC.rst"
  junit:
    - "gortools/src/test/java/gorsat/Commands/UTestCalc.java"
  mode: exact
  query: "norrows 1 | calc X 1+1"
  expected: |
    ChromNOR	PosNOR	RowNum	X
    chrN	0	0	2
```

### Example — error case

```yaml
- id: syntax.error.norrows_where_no_condition
  docs:
    - "documentation/src/languageBasics.rst"
  junit:
    - "gortools/src/test/java/gorsat/UTestSyntaxChecker.java"
  mode: error
  query: "norrows 1 | where"
```

### Example — file-based input

```yaml
- id: parser.basic.gor_query
  docs:
    - "documentation/src/languageBasics.rst"
    - "documentation/src/basicGORqueries.rst"
  junit:
    - "gortools/src/test/java/gorsat/UTestParser.java"
  mode: exact
  query: "gor ${ROOT}/basic.gor"
  expected: |
    Chrom	Pos	Val
    chr1	1	10
  inputs:
    - path: basic.gor
      content: |
        Chrom	Pos	Val
        chr1	1	10
```

---

## ID naming convention

IDs follow the pattern `<category>.<feature>.<behavior>`.

| Category | Meaning |
|----------|---------|
| `cmd` | A specific GOR pipe command (`cmd.where`, `cmd.calc`, `cmd.join`, …) |
| `fn` | A built-in function (`fn.math`, `fn.string`, `fn.list`, `fn.pr`, …) |
| `parser` | Query parsing, whitespace, literal forms, identifier rules |
| `syntax` | Syntactically invalid queries that must produce an error |
| `semantic` | Structurally valid but semantically invalid queries (unknown columns, etc.) |
| `script` | Multi-statement scripts using `def` / `create` / virtual relations |
| `mode` | Source-mode tests: `gorrow`, `gorrows`, `nor`, `norrows` |

The behavior segment uses `_` as a word separator and should be specific
enough to distinguish the case from its neighbors without reading the query.
When a case requires more than three pipe steps to express a single behavior,
add `behavior: pipeline` to suppress the pipe-count lint.

---

## Current suite statistics

| Category | Cases | Notes |
|----------|------:|-------|
| `cmd` | 442 | All 61 pipe commands represented |
| `fn` | 435 | 12 function groups: alg, bool, conv, convert, date, diagnostic, genomic, list, math, pr, string, trig |
| `syntax` | 41 | Negative cases across parser, argument, and expression errors |
| `semantic` | 30 | Unknown columns, type mismatches, missing sources |
| `parser` | 30 | Whitespace, literals, identifiers, expressions, gorrow/norrows |
| `mode` | 27 | Source-mode variants |
| `script` | 20 | `def`, `create`, virtual relations, macro expansion |
| **Total** | **1025** | |

JaCoCo language-execution scope (packages: `gorsat.parser`, `gorsat.Commands`,
`gorsat.Analysis`, `gorsat.Macros`, `gorsat.Script`, `gorsat.process`,
`gorsat.InputSources`, `gorsat.Iterators`):

- **Instructions:** 16.3% (56,334 / 344,749) — baseline, ratchet toward 100%
- **Branches:** 10.6% (2,652 / 25,126) — baseline, ratchet toward 100%

---

## Running the suite

### Standard run (default mode)

```bash
./gradlew :gortools:compatibilityTest
```

Runs all 1025 cases, generates provenance manifests, produces JUnit XML and
HTML reports, and checks coverage against the current floor threshold.

### Strict mode (CI)

```bash
./gradlew -Dcompat.strictMetadata=true \
    :gortools:compatibilityTest \
    :gortools:verifyCompatibilityManifests \
    :gortools:jacocoCompatibilityTestReport \
    :gortools:jacocoCompatibilityCoverageVerification
```

Strict mode activates:

- **Provenance enforcement** — every case must have non-empty `docs` and `junit`
  lists (no `compat/generated` stubs).
- **Lint checks** — pipe-count, duplicate-body, and unreferenced-input assertions.
- **Manifest gate** — build fails if any of the three manifest files is empty.
- **Coverage gate** — build fails if the JaCoCo ratio falls below the configured floor.

### Gradle tasks

| Task | Purpose |
|------|---------|
| `:gortools:compatibilityTest` | Runs the parameterized test suite and lint class |
| `:gortools:verifyCompatibilityManifests` | Fails if `docs-to-cases.yaml`, `junit-to-cases.yaml`, or `feature-scope-to-cases.yaml` is missing or empty |
| `:gortools:jacocoCompatibilityTestReport` | Produces JaCoCo HTML + XML scoped to language packages |
| `:gortools:jacocoCompatibilityCoverageVerification` | Enforces the minimum INSTRUCTION and BRANCH ratios |

### System properties

| Property | Default | Effect |
|----------|---------|--------|
| `compat.strictMetadata` | `false` | Enables provenance enforcement and lint checks |
| `compat.generateManifests` | `true` | Writes provenance manifests to `gortools/build/compat/manifests/` |
| `compat.manifestDir` | `gortools/build/compat/manifests` | Output directory for manifest files |

### CI artifacts

```text
gortools/build/
├── reports/tests/compatibilityTest/          # JUnit HTML report
├── test-results/compatibilityTest/           # JUnit XML (uploaded to CI)
├── compat/manifests/
│   ├── docs-to-cases.yaml
│   ├── junit-to-cases.yaml
│   └── feature-scope-to-cases.yaml
└── reports/jacoco/jacocoCompatibilityTestReport/
    ├── html/index.html
    └── jacocoCompatibilityTestReport.xml
```

---

## Validation gates

The runner validates every case at load time. Any violation aborts the entire
run before any test executes.

### Schema validation

Each case is validated against `compat/schema/gor-compat-case.schema.json`
(JSON Schema Draft 2020-12) via `com.networknt:json-schema-validator`.
The schema enforces:

- Required fields (`id`, `mode`, `query`).
- `id` matches the naming pattern.
- `mode` is one of the four allowed values.
- `error` mode cases must not have `expected` or `expectedFile`.
- Non-`error` cases must have exactly one of `expected` / `expectedFile`.
- No additional properties are allowed on any case or input object.

### Cross-cutting checks

Performed by the runner after schema validation:

- **Unique IDs** — duplicate `id` values abort with the conflicting id.

### Lint checks (strict mode only)

Performed by `UTestCompatibilitySuiteLint`:

- **Single-behavior pipe count** — cases outside the `script` category and
  without `behavior: pipeline` may not use more than three pipe operators.
- **No duplicate bodies** — no two cases may share identical
  `(query, mode, expected, inputs)` tuples.
- **Inputs referenced** — every path listed under `inputs` must appear in the
  query as `${ROOT}/<path>`.

---

## Adding new cases

1. Open `gortools/src/test/resources/compat/cases/compatibility-cases.yml`.

2. Append a new YAML record following the format above. Choose an `id` in the
   `<category>.<feature>.<behavior>` convention. Verify it is not already used:

   ```bash
   grep "^- id: your.proposed.id" gortools/src/test/resources/compat/cases/compatibility-cases.yml
   ```

3. Add `docs` and `junit` provenance, referencing the closest existing
   documentation page and JUnit class.

4. Run in default mode to confirm the case passes (or fails as expected for
   `error` mode cases):

   ```bash
   ./gradlew :gortools:compatibilityTest
   ```

5. Run in strict mode to confirm lint passes:

   ```bash
   ./gradlew -Dcompat.strictMetadata=true :gortools:compatibilityTest
   ```

### Coverage-driven workflow

To find untested language paths:

```bash
./gradlew :gortools:jacocoCompatibilityTestReport
open gortools/build/reports/jacoco/jacocoCompatibilityTestReport/html/index.html
```

Drill into a package, identify a red branch or method, then write a case that
exercises it. After the case passes, re-run the report and confirm the ratio
improved. Once a batch of new cases is stable, raise the floor thresholds in
`gortools/build.gradle`:

```groovy
limit {
    counter = 'INSTRUCTION'
    value   = 'COVEREDRATIO'
    minimum = 0.20  // raised from previous value
}
```

---

## JSON Schema reference

Schema file: `gortools/src/test/resources/compat/schema/gor-compat-case.schema.json`

Loaded once at JVM startup and applied to every case before any test executes.
Adding a field to a case without declaring it in the schema produces a
`Schema violation` error that identifies the offending case id and the
JSON-pointer path of the violation.

The schema uses `"additionalProperties": false`. To add a new optional field,
add it to the `properties` object in the schema file.

---

## Provenance manifests

After each run the runner writes three YAML manifests to
`gortools/build/compat/manifests/`:

- `docs-to-cases.yaml` — maps each documentation file to the case ids that
  cite it. Useful for finding which cases are affected when a doc page changes.
- `junit-to-cases.yaml` — maps each JUnit class to the case ids that cite it.
  Useful for finding which compatibility cases cover a given JUnit test.
- `feature-scope-to-cases.yaml` — maps `<category>/<feature>` keys to case
  ids. Useful for coverage gap analysis by feature area.

---

## Maintenance scripts

The `scripts/` directory at the repository root contains helper scripts used
during the initial build-out of the suite.

| Script | Purpose |
|--------|---------|
| `backfill_compat_provenance.py` | Adds `docs` and `junit` blocks to cases that have neither, deriving provenance from the case id prefix. Re-run after bulk-importing new cases. |
| `rename_bulk_cases.py` | Renames opaque numeric ids to meaningful `<category>.<feature>.<behavior>` ids. Edit the `RENAMES` dict and re-run to rename additional cases. |
| `audit_compat_cases.py` | Removes exact-duplicate cases and adds `behavior: pipeline` to multi-step cases. Re-run after any bulk import. |
| `cleanup_semantic.py` | Removes semantic error cases whose queries do not actually produce an error in the current engine, and strips unused `inputs` blocks. |

---

## Reproducing the suite from scratch

Follow these phases to build an equivalent suite for a different GOR module or
to rebuild this one from nothing.

### Phase 1 — Foundation

1. Write the JSON Schema (`gor-compat-case.schema.json`).
2. Write the parameterized JUnit runner (`UTestCompatibilitySuite.java`):
   - Loads the YAML file from the classpath.
   - Validates each case against the schema at load time.
   - Runs each case in a temporary directory.
   - Supports `exact`, `contains`, `regex`, and `error` comparison modes.
   - Writes provenance manifests when `-Dcompat.generateManifests=true`.
3. Add the Gradle task `compatibilityTest` in `gortools/build.gradle`.
4. Add 5 smoke cases covering: basic gor query, pipe chain, missing arg error,
   unknown column error, `WHERE`, `CALC`, and `TOP`.
5. Confirm the smoke cases pass.

### Phase 2 — Initial contract

6. Expand to 25+ cases covering priority features.
7. Ensure each case validates exactly one behavior.

### Phase 3 — Hardening

8. Wire JSON-schema validation into the runner load path
   (`com.networknt:json-schema-validator`; Jackson is already on the classpath).
9. Add `UTestCompatibilitySuiteLint`: pipe-count lint, duplicate-body lint,
   unreferenced-inputs lint. Gate all three behind `-Dcompat.strictMetadata`.
10. Add strict provenance enforcement: when `-Dcompat.strictMetadata=true`,
    the auto-stub fallback (`compat/generated`) is disabled and cases without
    real `docs`/`junit` links fail the run.
11. Add `verifyCompatibilityManifests` Gradle task.
12. Remove the `onlyIf { project.hasProperty('compatCoverageEnforce') }` guard
    from `jacocoCompatibilityCoverageVerification`. Set initial floor thresholds
    at measured baseline values.
13. Scope JaCoCo `classDirectories` to language-execution packages:
    `gorsat.parser`, `gorsat.Commands`, `gorsat.Analysis`, `gorsat.Macros`,
    `gorsat.Script`, `gorsat.process`, `gorsat.InputSources`, `gorsat.Iterators`.
14. Update CI (`build.yml` / `.gitlab-ci.yml`) to run
    `-Dcompat.strictMetadata=true` and to publish manifests and JaCoCo reports
    as artifacts.

### Phase 4 — Bulk import and cleanup

15. Bulk-add cases from existing JUnit tests and documentation examples.
16. Run `backfill_compat_provenance.py` to add provenance.
17. Run `audit_compat_cases.py` to remove duplicates and tag multi-step cases.
18. Run in strict mode; fix all lint violations.

### Phase 5 — Category expansion

19. Expand thin categories to target counts:
    - `parser.*` → ≥ 30 (whitespace, case, literals, identifiers, operators)
    - `syntax.error.*` → ≥ 30 (one case per recognized error class)
    - `semantic.*` → ≥ 30 (unknown columns, type errors, missing sources)
    - `script.*` → ≥ 20 (`def`, `create`, macro expansion)
20. All new cases must satisfy the schema, provenance, and lint gates from the start.

### Phase 6 — Coverage ratchet

21. Measure baseline coverage with `jacocoCompatibilityTestReport`.
22. Set initial floor slightly below the baseline (e.g. baseline 16% → floor 15%).
23. Add targeted cases for uncovered branches identified in the JaCoCo HTML report.
24. Ratchet floors upward in increments (15% → 20% → 30% → … → 100% for the
    defined scope).

Update floor thresholds in `gortools/build.gradle` as coverage improves:

```groovy
violationRules {
    rule {
        limit {
            counter = 'INSTRUCTION'
            value   = 'COVEREDRATIO'
            minimum = 0.15   // raise toward 1.0 as cases are added
        }
    }
    rule {
        limit {
            counter = 'BRANCH'
            value   = 'COVEREDRATIO'
            minimum = 0.10   // raise toward 1.0 as cases are added
        }
    }
}
```

The long-term target is 1.0 for both counters within the defined
language-execution scope.
