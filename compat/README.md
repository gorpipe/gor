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

## What the generators produce

- **Flag matrix** — one case per command flag, plus a bare invocation per command
  so the 32 commands that declare no flags are covered too.
- **Function matrix** — one case per registered CALC function, with arguments built
  from the function's signature (`String:Int2String` becomes `FN('a',1)`).
- **Input source matrix** — a bare invocation plus one case per flag for each input
  source. Every query begins with one, and `GOR` alone declares 23 flags.
- **Doc harvest** — self-contained snippets lifted from `documentation/src`.
- **Nightly only** (`:compat:generateNightly`) — mutants and grammar-fuzz cases.
  Mutants are filtered by coverage feedback: each candidate runs under the JaCoCo
  agent and is kept only if it reached a probe nothing had reached, which is why
  the nightly run keeps about 109 of 500 rather than all of them. Without the
  agent the filter is skipped and generation says so.

## Curated inputs the generators need

The registries do not carry everything a runnable case needs, so three files supply
the rest. Each is curated deliberately; nothing here is guessed.

- `inventory/flag-values.yml` — sample values for value-taking flags. Most entries
  come from the flag's documented argument name (`-gc cols` is a column), one
  curated value per argument kind. A flag with no entry is reported in
  `inventory/gaps.txt` rather than turned into a broken case.
- `inventory/command-args.yml` — per-command positional arguments and required
  companion flags. `GROUP` takes a bin size where a file would be wrong, and `JOIN`
  rejects any invocation with no join type.
- `inventory/input-source-args.yml` — the same, for input sources, whose arguments
  vary more: `GOR` wants a file, `GORROW` a position, `NORROWS` a row count.
- `inventory/exclusions.yml` — surface deliberately left uncovered, each entry with
  a reason. Every generator reads this file, so leaving out a command (`cmd.CMD`),
  a function (`fn.SYSTEM`) or an input source (`is.SQL`) means writing down why.
  `CMD` and `SQL` exist as both a pipe command and an input source, and each needs
  its own entry.

## Keeping baselines meaningful

A baseline that changes on its own is worse than no baseline: it teaches reviewers
to approve baseline diffs without reading them. Three mechanisms prevent it, and
each catches what the others cannot.

- **Root canonicalisation.** The project root is a fresh temp directory per run, so
  `CaseRunner` puts `${ROOT}` back wherever it appears in output — including in
  engine error messages, which quote paths freely.
- **A static screen.** `CaseLint` rejects queries using the clock, randomness,
  machine identity, JVM state (`MAXMEM`, `OPENFILES`) or build identity
  (`GORVERSION` embeds the git SHA). These are stable within one process, so only a
  list can catch them.
- **A runtime probe.** `CaseStability` runs each candidate under two roots of
  different path length and drops it if the output differs — `PIPESTEPS` reports the
  *length* of a path, which no list would have predicted. Drops are recorded in
  `inventory/unreproducible.txt`, and each one is a finding about the engine.

Every case also runs under a time bound (20s by default,
`-Dcompat.caseTimeoutSeconds`). A query that never returns records a timeout rather
than wedging CI.

## What gates, and what does not

Gates: a failing spec case, an unaccepted baseline diff, a lint violation, a stale
inventory, or a baseline case with no committed output.

Does not gate: code coverage, surface completeness, or the documentation
cross-check. All are printed on every run as diagnostics. The suite this replaced
gated on a coverage ratio, and 35% of its cases responded by asserting a single
token.
