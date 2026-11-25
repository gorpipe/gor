# GOR Compatibility Test Suite

The `:compat` module is a black-box, end-to-end test suite for the GOR query
language. Every case is a GOR query plus an expected output. It never reaches into
engine internals, and it is independent of the unit and integration suites: its own
Gradle module, depending on `:gortools` and `:model` only, never on `:test`.

How to run it, add cases and handle failures: [compat/README.md](../compat/README.md).
What it has turned up so far: [compat/FINDINGS.md](../compat/FINDINGS.md).

## Purpose

The suite answers two questions and keeps them apart.

- **Is GOR correct?** The *spec* tier: hand-written cases whose expected output is
  derived from the documentation, or from a reviewed judgement where the
  documentation is silent, and cites its source.
- **Did GOR change?** The *baseline* tier: machine-generated cases spanning the
  command, flag, function, input-source and macro surface. Their outputs are
  committed and diffed on every run.

One corpus cannot answer both. A captured output says what the engine does, not
what it should do. So baselines are change detectors, not correctness claims;
correctness lives in the spec tier.

## Decisions

| Decision | Why |
|---|---|
| Two tiers, spec and baseline | A reviewed golden answers "is this correct"; a captured baseline answers "did this change" |
| Change detection is a committed baseline, not a second engine at test time | No dual classpath or artifact download; diffing against committed output gives the same signal |
| Baseline breadth is generated, not hand-written | Hand-authoring stops at hundreds of cases. Command × flag × function coverage is only reachable by generation |
| Generated cases and their outputs are both committed, one file per command | A behaviour change arrives in the pull request as the actual before/after output, which is what makes accepting it reviewable |
| Separate `:compat` module, run in-JVM through the public API | Separation is enforced by the dependency graph (a boundary test fails if `:test` creeps in), and in-JVM keeps a corpus of about a thousand cases to about a minute |
| Coverage never gates | A suite gated on a coverage ratio fills with single-token assertions. Coverage and surface completeness are printed as diagnostics instead |
| Baselines move only by a person on a workstation | `:compat:accept` refuses to run in CI and reports how many cases changed |

## What fails a build

- a spec case whose output no longer matches
- a baseline case whose output differs from its committed baseline
- a lint violation (for example a query using the clock, randomness or build identity)
- a stale inventory or generated corpus, meaning the language surface changed
  without `:compat:inventory` / `:compat:generate` being rerun
- a baseline case with no committed output

Nothing else. `./gradlew :compat:report` prints surface coverage, exclusions and
the documentation cross-check on every run; that report is the worklist for new
cases.

## CI

`:compat:test` runs as part of the root `./gradlew test` in the GitHub Actions
`build.yml` workflow. It needs neither the test-data submodule nor integration
secrets, since the module generates its own fixtures.

The nightly generators (coverage-filtered mutants and grammar fuzz,
`:compat:generateNightly`) and `:compat:coverageReport` exist but are **not
currently scheduled** in any CI workflow.

## Current state and open work

- The spec tier covers a minority of commands. Most of the surface is reached
  only by baselines, which record behaviour without claiming it is right.
- Some baselines record only an error, because the command needs a fixture or
  argument the generators do not yet supply (the LIFTOVER family needs a chain
  file, for example). Such a baseline marks a gap, not coverage.
- Behaviour the suite has pinned but nobody has decided is correct is listed in
  [FINDINGS.md §2](../compat/FINDINGS.md). Those entries need decisions, not more
  cases.

Run `./gradlew :compat:report` for current counts rather than trusting any number
written into a document.
