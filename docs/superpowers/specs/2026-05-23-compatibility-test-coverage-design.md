# Compatibility Test Suite Coverage Improvement Design

**Date:** 2026-05-23
**Target:** 50% instruction coverage / 40% branch coverage (JaCoCo language-execution scope)
**Baseline:** 16.3% instructions / 10.6% branches (1025 cases)

---

## Problem

The compatibility test suite has 1025 cases covering all 61 pipe commands and 12 function groups, but coverage sits at 16%/10%. The cases are too shallow — they hit happy paths but miss flag combinations, error paths, boundary values, and non-obvious control-flow branches. The JaCoCo floor thresholds (15%/10%) are barely above baseline and provide no meaningful regression protection.

---

## Approach

Two sequential phases:

1. **Command/Function Audit** — systematic enumeration of every command and function group to add missing flag, error, and boundary cases
2. **JaCoCo Gap Fill** — run coverage report after Phase 1, drill into red branches, write targeted cases

Both methods are needed: the audit covers the structured option space efficiently; JaCoCo catches non-obvious control-flow that is hard to enumerate from reading command signatures alone.

---

## Phase 1: Command/Function Audit

### Scope

- All 61 pipe commands registered in `GorPipeCommands.scala`
- All 12 function groups: `alg`, `bool`, `conv`, `convert`, `date`, `diagnostic`, `genomic`, `list`, `math`, `pr`, `string`, `trig`

### Process per command

1. Read the command source in `gortools/src/main/scala/gorsat/Commands/` and its corresponding `Analysis/` class.
2. Enumerate: accepted flags/options, required vs optional arguments, column-reference parameters.
3. Check existing `cmd.<command>.*` cases in `compatibility-cases.yml`.
4. Add cases for each gap:
   - **Missing flags** — one case per flag not yet exercised
   - **Error paths** — wrong argument count, wrong type, unknown column name, missing required arg
   - **Boundary values** — 0-row input, single-row input, empty string columns, max/min numeric values where relevant
   - **Flag combinations** — where two flags interact (e.g. `-h` + `-s` on `JOIN`)

### Process per function group

Same pattern scoped to `fn.<group>.*` cases:
- One case per function variant not yet covered
- Type-error case for each function (wrong argument type)
- Boundary case (0, negative, empty string, null-like) where the function has defined behavior

### Expected output

~800–1000 new YAML cases added to `compatibility-cases.yml`, organized within existing category structure. Total suite size: ~1800–2025 cases.

### Audit checklist

Track progress here. Check off each command/group as its audit is complete.

#### Commands (61 total)

- [ ] BUCKETSPLIT
- [ ] CALC
- [ ] COLNUM
- [ ] COLS2LIST
- [ ] COLSPLIT
- [ ] COLTYPE
- [ ] CSVCC
- [ ] CSVSEL
- [ ] DISTINCT
- [ ] DISTLOC
- [ ] FDIST
- [ ] FIRST
- [ ] GAVA
- [ ] GOR (source)
- [ ] GORROWS
- [ ] GORROW
- [ ] GTGEN
- [ ] GTLD
- [ ] GROUP
- [ ] INSET
- [ ] JOIN
- [ ] KING
- [ ] LAST
- [ ] LDANNO
- [ ] LEFTWHERE
- [ ] LIFTOVER
- [ ] LINES
- [ ] LINEFILTER
- [ ] LOG
- [ ] LOOKUP
- [ ] MAP
- [ ] MERGE
- [ ] MULTIMAP
- [ ] NOR (source)
- [ ] NORROWS
- [ ] OVERLAP
- [ ] PARTGOR
- [ ] PEDPIVOT
- [ ] PGOR
- [ ] PICK
- [ ] PIVOT
- [ ] PREFIX
- [ ] RANK
- [ ] REGRESSION
- [ ] RENAME
- [ ] REPLACE
- [ ] ROWNUM
- [ ] SAMPLE
- [ ] SEG
- [ ] SEGHIST
- [ ] SEGPROJ
- [ ] SEGSPAN
- [ ] SELECT
- [ ] SKIP
- [ ] SORT
- [ ] SPLIT
- [ ] TOP
- [ ] UNPIVOT
- [ ] VARJOIN
- [ ] VARIANTS
- [ ] VARNORM
- [ ] VERIFYORDER
- [ ] WAIT (if applicable)
- [ ] WHERE
- [ ] WRITE

#### Function groups (12 total)

- [ ] alg
- [ ] bool
- [ ] conv
- [ ] convert
- [ ] date
- [ ] diagnostic
- [ ] genomic
- [ ] list
- [ ] math
- [ ] pr
- [ ] string
- [ ] trig

---

## Phase 2: JaCoCo Gap Fill

### Trigger

After Phase 1 cases are committed and passing.

### Process

1. Run the coverage report:
   ```bash
   ./gradlew :gortools:jacocoCompatibilityTestReport
   open gortools/build/reports/jacoco/jacocoCompatibilityTestReport/html/index.html
   ```

2. Drill into packages in this priority order (by expected branch density):
   - `gorsat.Commands`
   - `gorsat.Analysis`
   - `gorsat.Macros`
   - `gorsat.Script`
   - `gorsat.process`
   - `gorsat.parser`
   - `gorsat.InputSources`
   - `gorsat.Iterators`

3. For each red/yellow branch: identify the condition, write the minimum query that triggers it, add a case.

4. Batch workflow — write ~50 cases, re-run report, verify ratio improved, continue.

### Floor ratchet schedule

Update `gortools/build.gradle` `jacocoCompatibilityCoverageVerification` limits at each checkpoint:

| Checkpoint | Instruction floor | Branch floor |
|-----------|------------------|-------------|
| Baseline (current) | 0.15 | 0.10 |
| After Phase 1 | 0.25 | 0.20 |
| Mid Phase 2 | 0.35 | 0.30 |
| Phase 2 complete | 0.50 | 0.40 |

Never lower a floor. Each checkpoint requires a passing `jacocoCompatibilityCoverageVerification` run before the next ratchet.

---

## Doc Updates

`docs/compatibility_test_suite.md` receives these additions (no removals):

1. **Stats table** — update counts and percentages after each phase; add "last updated" date.
2. **Coverage milestone section** — new section documenting the ratchet schedule above with target dates.
3. **Coverage-driven workflow** — expand the existing section with:
   - How to read the JaCoCo HTML report (which columns matter, how to navigate to a specific branch)
   - How to map a red branch to a minimal triggering query
   - How to verify a new case actually hits the targeted branch (run report before/after, confirm ratio change)
4. **Phase audit checklist** — the checklist above, kept in sync as commands are completed.

---

## Success Criteria

- `./gradlew :gortools:jacocoCompatibilityCoverageVerification` passes with floors at `0.50` (instructions) and `0.40` (branches)
- All new cases pass `./gradlew -Dcompat.strictMetadata=true :gortools:compatibilityTest`
- `docs/compatibility_test_suite.md` stats section reflects final case count and coverage numbers
- All audit checklist items checked off

---

## Out of Scope

- Pushing above 50%/40% (deferred to a future milestone)
- Modifying the test runner (`UTestCompatibilitySuite.java`) or schema
- Adding new test categories beyond the existing seven
- Coverage of packages outside the defined language-execution scope
