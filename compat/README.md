# GOR Compatibility Suite

Black-box end-to-end tests over the GOR query language. Every case is a query plus
an expected output. Completely separate from the unit and integration suites: this
module depends on `:gortools` and `:model` only, never on `:test`.

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

## Curated inputs the generators need

The registries do not carry everything a runnable case needs, so three files supply
the rest. Each is curated deliberately; nothing here is guessed.

- `inventory/flag-values.yml` — sample values for value-taking flags. A flag with no
  entry is reported in `inventory/gaps.txt` rather than turned into a broken case.
- `inventory/command-args.yml` — per-command positional arguments and required
  companion flags. `GROUP` takes a bin size where a file would be wrong, and `JOIN`
  rejects any invocation with no join type.
- `inventory/exclusions.yml` — surface deliberately left uncovered, each entry with
  a reason. The generator reads this file, so leaving a command out means writing
  down why.

## What gates, and what does not

Gates: a failing spec case, an unaccepted baseline diff, a lint violation, a stale
inventory, or a baseline case with no committed output.

Does not gate: code coverage, surface completeness, or the documentation
cross-check. All are printed on every run as diagnostics. The suite this replaced
gated on a coverage ratio, and 35% of its cases responded by asserting a single
token.
