# GOR Compatibility Test Suite — Design

Date: 2026-08-29
Status: Design approved, not yet implemented
Replaces: the existing suite at `gortools/src/test/java/gorsat/compat/`
Supersedes: `docs/compatibility_test_suite.md` (to be rewritten from this spec)

---

## 1. Purpose

The compatibility suite answers two questions, and keeps them separate:

- **Is GOR correct?** — a hand-authored corpus of queries with reviewed expected
  outputs, sourced from documentation or human judgement.
- **Did GOR change?** — a machine-generated corpus spanning the command, flag and
  function surface, whose outputs are captured into committed baselines and diffed
  on every run.

It is black-box and end-to-end. Every case is a GOR query plus an expected output.
It never reaches into engine internals.

It is completely separate from the unit and integration suites: its own Gradle
module, its own dependency graph, no shared code and no shared test infrastructure.

---

## 2. Ground truth

Measured on this working tree, 2026-08-29. These numbers are the basis of the
design; earlier planning documents in this repo carried numbers that no longer
match the tree and have been discarded.

| Fact | Measured |
|---|---|
| Existing suite | committed on `main`; 1087 cases in one 10,598-line YAML (292K) |
| Modes in use | `exact` 568 (52%), `contains` 381 (35%), `error` 135 (12%), `regex` 3 |
| Case categories | cmd 472, fn 456, syntax 43, semantic 32, mode 31, parser 30, script 23 |
| Distinct queries | 1086 of 1087 — near-zero duplication |
| Cases with fixtures | 82 |
| Runner | `UTestCompatibilitySuite.java` 363 lines, `UTestCompatibilitySuiteLint.java` 138 lines |
| Coupling | imports `gorsat.TestUtils`; lives in the `:gortools` test source set |
| Gradle task | none — the suite rides `:gortools:test` |
| Pipe commands | ~112, via `GorPipeCommands.register()` |
| Input sources | ~17 |
| Macros | ~5 |
| Documentation | 302 `.rst` files; 117 command pages, 96 function pages |
| Doc query blocks | 568 `code-block:: gor` across 240 files |
| Helper scripts | 4 one-off Python scripts in `scripts/` |

Two registry facts determine what can be generated:

- `GorPipeCommands.commandMap` is a public `Map[String, CommandInfo]`. `CommandInfo`
  is a case class exposing `commandArguments`, and `CommandArguments(options,
  valueOptions, minimumNumberOfArguments, maximumNumberOfArguments,
  ignoreIllegalArguments)` holds flags as space-separated strings. Example, from
  `Join.scala`: options `-snpsnp -snpseg -segseg -segsnp -varseg -segvar -stdin -r
  -l -i -ic -ir -t -c -n -m -xcis`, valueOptions `-s -p -f -e -o -lstop -rstop -xl
  -xr -maxseg -rprefix -ref -refl -refr`.
  **Commands and flags are fully enumerable at runtime with no engine change.**
- `FunctionRegistry.allFunctions` is `private val`, with only `hasFunction(fn)`
  public. **Function names are not enumerable** without a change.

---

## 3. Why the existing suite is replaced

Three defects, each structural rather than incidental.

**Assertion strength.** All 381 `contains` cases assert exactly one line, and
usually a single token: `chr1`, `allCount`, `mapq`, `\t7`. A JOIN emitting wrong
rows, wrong counts or wrong columns passes every one of them. Thirty-five percent
of the corpus cannot detect wrong output.

**Provenance is fictional.** All 1087 cases cite both a documentation file and a
JUnit class — a 100% hit rate produced by `scripts/backfill_compat_provenance.py`
stamping citations in bulk. The citations are therefore not evidence that anything
was checked against anything.

**No separation.** The suite lives inside the `:gortools` test source set, imports
`gorsat.TestUtils`, and has no Gradle task of its own. It is a unit test that
happens to be table-driven.

The queries are the one sound asset — 1086 distinct, spanning seven categories. The
decision below is nonetheless to scrap the corpus rather than migrate it, because
the generated tier reaches the same surface with better assertions and without
inheriting 1087 unreviewed expectations.

---

## 4. Decisions

| # | Decision | Rationale |
|---|---|---|
| D1 | Two layers: a reviewed SPEC corpus and a generated BASELINE corpus | A reviewed golden answers "is this correct"; a captured baseline answers "did this change". One corpus cannot do both without one of the two answers being fake |
| D2 | Cross-version detection is a committed baseline file, not a second engine at test time | No dual classpath, no subprocess, no artifact download. Diffing HEAD against a committed baseline gives the same signal at a fraction of the machinery |
| D3 | The existing 1087 cases are scrapped, not migrated | Their assertions are weak and their provenance is bulk-stamped. Generation reaches the same surface without importing unreviewed expectations |
| D4 | The breadth tier is machine-generated | Hand-authoring is bounded at hundreds of cases. Real command × flag × function coverage is only reachable by generation |
| D5 | Four generators, phased: flag matrix and doc harvest on PRs; coverage-guided and grammar fuzz nightly | The first two are the spine and are cheap. The latter two are expensive and noisy, and earn their place only in a nightly lane |
| D6 | New top-level `:compat` module, executing in-JVM through the public API | Separation enforced by the dependency graph rather than convention. In-JVM keeps a multi-thousand-case corpus tractable |
| D7 | CI gates on a failing SPEC case or an unaccepted baseline diff. Nothing else | Coverage and surface completeness are diagnostics. Gating on a coverage ratio is what produced 381 single-token assertions |
| D8 | Generated cases and their baselines are both committed, one file per command | A behaviour change then arrives in the PR as the actual before/after output, which is what makes accepting a change reviewable |

---

## 5. Module and execution model

### 5.1 Module

New top-level module `compat/`, registered in `settings.gradle`.

- Depends on `:gortools` (transitively `:model`, `:base`).
- Does **not** depend on `:test`. That module holds `gorsat.TestUtils`, and
  depending on it would reintroduce the coupling this module exists to remove.
- Nothing depends on `:compat`.

`UTestModuleBoundary` asserts that `gorsat.process.PipeInstance` resolves and that
`gorsat.TestUtils` does not. If the dependency is ever added back, that test fails.
The separation is structural, not a rule someone has to remember.

### 5.2 Executor

```
CompatExecutor.run(query, projectRoot) -> CompatResult { header, rows[], error? }
```

Builds a session from `CLISessionFactory` — the factory the real CLI uses — scoped
to `projectRoot`, parses the query with `PipeOptions`, drives `PipeInstance`, and
serialises the result. Engine exceptions are captured into `error`, never thrown
out of the executor.

### 5.3 Output serialisation is part of the contract

Defined here rather than inherited:

- Header line, then one line per row.
- Tab-separated, `\n`-joined, trailing newline.
- No locale-dependent formatting.
- Test JVM runs with `-Duser.timezone=UTC` and `-Dfile.encoding=UTF-8`.

The existing suite inherits `TestUtils`'s ad-hoc `header + "\n" + rows` shape by
accident. Any change to that shape would silently invalidate every expected value
in the corpus. Pinning it here makes changing it a deliberate, reviewable act.

### 5.4 Execution environment

Each case runs in a fresh temporary directory that becomes the project root, so
`${ROOT}` resolution, relative paths and write targets behave as they do for a real
user in a real project directory.

The temp directory is deleted on success. **On failure it is retained and its path
printed** — debugging a golden mismatch without its fixtures is impractical.

### 5.5 Determinism

Enforced by lint at load, not by documentation. A case may not reference wall-clock
time, the current date, random seeds, hostname, environment-specific values,
thread counts, or absolute paths outside `${ROOT}`. Violations fail the load.

Grammar fuzzing (§8, G4) is seeded from a committed fixed seed for the same reason.

---

## 6. The two corpora

|  | SPEC | BASELINE |
|---|---|---|
| Authored by | human | generator |
| Assertion | `exact` / `error` | full-output diff against committed baseline |
| Source of truth | documentation, or human review | the previously committed output |
| On failure | build red | build red until fixed, or baseline re-committed |
| Expected size | hundreds | thousands |
| Answers | "is this correct?" | "did this change?" |

Both tiers use the same executor, the same case format and the same runner. The
difference is where the expected output comes from and who vouches for it.

### 6.1 Case format

```yaml
- id: cmd.join.snpsnp_basic
  tier: spec              # spec | baseline
  mode: exact             # exact | error
  source: docs            # docs | review        (spec tier only)
  cites: ["documentation/src/command/JOIN.rst"]
  query: "gor ${ROOT}/left.gor | join -snpsnp ${ROOT}/right.gor"
  inputs:
    - path: left.gor
      content: |
        Chrom	Pos	Ref
        chr1	100	A
  expected: |
    Chrom	Pos	Ref	distance	Chromx	Posx	Alt
    chr1	100	A	0	chr1	100	G
```

`contains` and `regex` do not exist as modes. Not deprecated — absent. That is what
prevents the single-token failure mode from recurring.

### 6.2 Load-time validation

JSON Schema Draft 2020-12, `additionalProperties: false`, validated before any case
executes. Beyond the schema:

- Duplicate ids across files are a load error.
- A `spec` case with no `cites` is a load error.
- A flag in a query that the target command does not declare is a load error —
  which incidentally catches typos.
- Determinism violations (§5.5) are a load error.

### 6.3 Where expected output lives

A SPEC case carries its `expected` inline, as shown above — it is hand-authored and
belongs next to the query a person wrote.

A BASELINE case carries **no** `expected`. Its output lives in the paired file under
`baselines/`, keyed by case id, one file per command. Keeping generated outputs out
of the generated case files means `generate` and `accept` write disjoint sets of
files: regenerating the case list never rewrites a baseline, and accepting a
behaviour change never rewrites a query.

Case files and baselines are read from the module directory (`compat/cases`,
`compat/baselines`), not from the test classpath. They are data under review, not
packaged resources, and keeping them out of `src/test/resources` keeps the
distinction visible.

---

## 7. Fixtures

Fixtures are generated by `:compat` itself, deterministically. They are not
borrowed from `tests/data` — that is the submodule the unit tests use, and
borrowing it would reintroduce the coupling D6 removes.

The canonical set: a gor file, a nor file, a dictionary, a segment file, a PN list,
a VCF, and a **synthetic chromSeq reference build**.

### 7.1 The synthetic reference build

Grepping `createRefSeq` / `ChromSeq` across the command and analysis packages finds
eight files requiring a reference genome: `Pileup`, `VarNormAnalysis`,
`VerifyVariantAnalysis`, `MergeGenotypes`, `AddFlankingSeqs` (SEQ),
`PhaseReadVariants`, `CigarVarSegs`, `VarJoinAnalysis`.

Excluding all of these would remove the variant-handling surface — the part of GOR
most likely to break silently — from the suite entirely.

It does not need to be excluded. `scripts/refbuild/README.md` documents the format:
a folder holding one `<contig>.txt` per chromosome, raw bases at one byte per base
with no newlines (offset `pos-1` is the base at 1-based `pos`), alongside
`buildsize.gor`, `buildsplit.txt` and `gor_config.txt`. `tests/data/ref_mini` is an
existing instance.

`:compat` therefore synthesises its own: a few kb of fixed bases across chr1 and
chr2 with the three metadata files, written into the per-case temp root and pointed
at by `buildPath`. Deterministic, hermetic, and it keeps eight commands testable.

### 7.2 Exclusions

Some surface will still be untestable hermetically. Exclusions are determined
**empirically from the first flag-matrix run**, not assumed now. Each requires a
written reason:

```yaml
- element: cmd.EXAMPLE.-flag
  reason: "requires a live database connection"
  ticket: null
```

The count is printed on every run and adding one is a reviewable diff. This is the
pressure valve that stops the surface report being quietly gamed.

---

## 8. Generators

Each is a Gradle task in `:compat` running a JavaExec main. All write committed
case files. None are loose scripts — the four existing Python helpers in `scripts/`
are precisely the thing to avoid: they rotted against a corpus that changed.

### G1 — Flag matrix (spine)

Calls `GorPipeCommands.register()`, walks `commandMap`, and for each of the ~112
commands splits `commandArguments.options` (valueless) from `.valueOptions`
(value-taking). Emits one baseline case per flag against the canonical fixtures.
Adds one case per function per arity.

Two limits, stated rather than hidden:

- **Value-taking flags need a value the registry does not carry.** JOIN declares
  `-maxseg` but not that it wants an integer. A curated `inventory/flag-values.yml`
  maps command+flag to a sample value. Any value-flag with no entry becomes a line
  in the gap report, never a broken case. This file is the manual residue of the
  generator; it is small and reviewable.
- **Function names require an engine change.** `FunctionRegistry.allFunctions` is
  private. This design adds one additive accessor:

  ```scala
  def functionNames: Set[String] = allFunctions.keySet.toSet
  ```

  This is the **only engine change in the design**. The alternative — reflecting
  over a private Scala field — breaks silently on refactor.

Because attribution is recomputed from the registry rather than hand-maintained, a
command that gains a flag produces a new case the day the flag lands.

### G2 — Documentation harvest

Two products from the 568 `code-block:: gor` blocks across 240 pages:

- **Runnable snippets.** Self-contained blocks (rooted in `gorrow` / `norrows`)
  become baseline cases. Blocks referencing project data such as `#dbsnp#` are not
  hermetic and are skipped with a recorded reason. A documentation snippet that no
  longer runs is itself a compatibility defect.
- **Flag cross-check.** The 117 command pages carry machine-readable Options
  tables. Parsing them and diffing against the runtime registry reports
  **undocumented flags** (registered, absent from the docs) and **phantom flags**
  (documented, not registered — either a stale page or a dropped flag). No test
  case can find either class of defect. This is cheap, and is likely the first
  thing the suite surfaces.

### G3 — Coverage-guided (nightly, behind a flag)

Mutates existing cases, runs candidates under JaCoCo, and keeps only those reaching
new branches. This is what actually drives branch coverage upward. Expensive, and
its output is semantically meaningless queries — acceptable, because its only
assertion is "did this change".

### G4 — Grammar fuzz (nightly, behind a flag)

Generates syntactically valid queries from `GorScript.g4`. Most output is invalid
at a semantic level, so its baseline is largely error-message pinning — which is
genuine parser-contract coverage. Seeded from a **committed fixed seed**, or the
baselines churn on every run and the tier becomes noise. Case count is capped.

---

## 9. Baseline lifecycle

```
./gradlew :compat:generate    # regenerate case files from registry / docs / grammar
./gradlew :compat:accept      # re-capture baselines, rewrite committed outputs
./gradlew :compat:test        # run everything, diff against committed baselines
```

CI runs only `test`, plus a staleness check that `generate` produces no diff.

The change workflow:

1. A developer changes engine behaviour.
2. `:compat:test` goes red, listing every case whose output moved, with before and
   after inline.
3. The developer reads the diff. Unintended: a regression has been caught, fix it.
   Intended: run `:compat:accept` and commit the rewritten baselines.
4. **The PR then contains the actual before/after output of every affected case.**
   The reviewer sees the behaviour change itself, not a summary of it. This is the
   payoff of D8.

A newly added flag flows through the same path: `generate` emits its case, the case
has no baseline, and the build fails asking for `accept`. A flag cannot land
without its behaviour being recorded and reviewed.

Two guards:

- `accept` refuses to run in CI. Baselines move only by a human act on a
  workstation.
- `accept` prints how many cases changed. A large number in a PR is itself the
  signal.

The residual risk is a developer reflexively accepting a real regression. The
mitigation is that the diff lands in the PR, where a reviewer sees it.

---

## 10. CI, runtime and reporting

### 10.1 Runtime

**Unmeasured, and the one number that can invalidate a choice in this design.** The
existing suite has no Gradle task; it rides `:gortools:test`, so its cost is
buried.

Measuring per-case session-construction cost is implementation task 1, before any
generator is built. At ~50ms per case, 10,000 cases is roughly eight minutes
single-threaded and the in-JVM model holds comfortably. At ~500ms it does not, and
the model must be revisited before the corpus is built rather than after.

Levers: Gradle `maxParallelForks`, JUnit `Parameterized`, and reusing the JVM
across cases. Sessions are **not** reused across cases — each case needs its own
project root.

### 10.2 Lanes

| Lane | Runs |
|---|---|
| PR | SPEC corpus, BASELINE corpus from G1+G2, `generate` staleness check |
| Nightly | the above, plus G3, G4, JaCoCo, and the full reports |

### 10.3 What fails a build

1. A SPEC case failing its `exact` or `error` assertion.
2. A BASELINE case whose output differs from its committed baseline.
3. A schema violation, duplicate id, unknown flag, missing `cites` on a spec case,
   or determinism violation.
4. Committed generated cases differing from a fresh `generate`.

Nothing else. Coverage does not fail a build. Surface completeness does not fail a
build.

### 10.4 Diagnostics

Printed every run, gating nothing:

```
SPEC       412 cases   green
BASELINE  8,140 cases   0 diffs
SURFACE    commands 104/112   flags n/m   functions n/m
           excluded 6 (see inventory/exclusions.yml)
DOCS       undocumented flags 23   phantom flags 7
COVERAGE   instruction 61.2%   branch 44.8%     (nightly)
```

Numbers above are illustrative shape; real values come from the first run. The
surface and coverage lines are how the next gap is found. They are deliberately not
the gate.

---

## 11. Repository layout

```
compat/
  build.gradle
  src/main/java/org/gorpipe/compat/
    CompatExecutor.java              engine driver
    CompatResult.java
    CaseLoader.java                  glob, parse, schema-validate, merge
    CaseRunner.java                  per-case temp root, compare, retain on failure
    CaseLint.java                    determinism, duplicates, provenance
    Fixtures.java                    canonical fixtures incl. synthetic chromSeq build
    inventory/SurfaceInventory.java  registries -> surface.json
    gen/FlagMatrixGenerator.java
    gen/DocHarvester.java
    gen/CoverageGuidedGenerator.java
    gen/GrammarFuzzer.java
  src/test/java/org/gorpipe/compat/
    UTestModuleBoundary.java         asserts :test is unreachable
    UTestCompatSuite.java            parameterized runner (JUnit 4)
    UTestCompatLint.java
  cases/spec/cmd/join.yml            hand-authored, gating
  cases/baseline/cmd/join.yml        generated
  baselines/cmd/join.out             committed outputs
  inventory/surface.json             regenerated and diffed in CI
  inventory/flag-values.yml          curated sample values for value-taking flags
  inventory/exclusions.yml           reasoned, counted
  schema/gor-compat-case.schema.json
```

One file per command throughout. A JOIN change touches `join.yml` and `join.out`
only: reviewable diffs, no merge conflicts between people working on different
commands, and a per-command `git log`.

Loading globs the tree and merges deterministically by sorted path.

JUnit 4 throughout, matching the rest of the repository.

---

## 12. Removal of the existing suite

Removed **only once `:compat` is green**, so there is never a window with no
compatibility coverage:

- `gortools/src/test/java/gorsat/compat/UTestCompatibilitySuite.java`
- `gortools/src/test/java/gorsat/compat/UTestCompatibilitySuiteLint.java`
- `gortools/src/test/resources/compat/**`
- `scripts/audit_compat_cases.py`
- `scripts/backfill_compat_provenance.py`
- `scripts/cleanup_semantic.py`
- `scripts/rename_bulk_cases.py`

`docs/compatibility_test_suite.md` is rewritten from this spec.

---

## 13. Rollout

1. Measure per-case execution cost (§10.1). This gates the execution model.
2. Stand up `:compat` with the boundary test, `CompatExecutor`, loader, schema, and
   ~20 hand-written SPEC cases proving both modes and the failure paths.
3. Fixtures, including the synthetic chromSeq reference build.
4. Surface inventory and the `generate` / `accept` / staleness machinery.
5. G1 flag matrix. First real baseline corpus; first exclusions list.
6. G2 doc harvest, including the flag cross-check report.
7. Delete the existing suite and rewrite its documentation.
8. G3 and G4 behind flags, wired into the nightly lane.
9. Ongoing: SPEC cases authored against the surface report, which is the worklist.

---

## 14. Risks

| Risk | Mitigation |
|---|---|
| A developer reflexively runs `accept` on a real regression | The before/after output lands in the PR diff where a reviewer sees it. `accept` cannot run in CI and reports how many cases changed |
| Baselines freeze current bugs | They are explicitly change-detectors, not correctness claims. Correctness lives in the SPEC tier, and the two are never conflated |
| Per-case runtime makes a multi-thousand-case corpus infeasible | Measured first, before generators are built. Levers are parallel forks and JVM reuse |
| G4 grammar fuzzing produces churning baselines | Committed fixed seed, capped case count, nightly lane only |
| `flag-values.yml` becomes a large hand-maintained file | Value-flags with no entry degrade to a gap-report line, never a broken case. The file grows only as far as someone chooses to push coverage |
| The exclusions list becomes a dumping ground | Written reason required, count printed every run, additions are reviewable diffs |
| Generated corpus bloats the repository | One file per command; roughly a few hundred bytes per case puts ~10k cases in the low megabytes |

---

## 15. Open items

- Per-case execution cost — measured in rollout step 1, before the execution model
  is committed to.
- Real surface counts for flags and functions — available once `SurfaceInventory`
  first runs.
- The exclusions list — determined empirically from the first G1 run.
- Whether `functionNames` lands as an engine accessor or the function tier is
  deferred — the accessor is preferred and is the only engine change proposed.
