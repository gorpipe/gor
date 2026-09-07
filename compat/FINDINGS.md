# Findings from building the compatibility suite

Everything the suite turned up while the corpus was being built: defects that were
fixed, behaviour that was pinned but left alone, and limitations of the suite
itself. Kept here rather than scattered across commit messages and tickets so the
open items stay findable.

Each entry says where the evidence lives. A **case** is a spec case that fails if
the behaviour changes — those are the entries that cannot rot silently.

---

## 1. Defects found and fixed

All on branch `ENGKNOW-3780-gor-engine-defects`, tracked by
[ENGKNOW-3780](https://genedx.atlassian.net/browse/ENGKNOW-3780) except where noted.
None of them altered a query that already worked, with the single exception of
COLUMNSORT, which is called out below.

| # | Defect | Evidence |
|---|---|---|
| 1.1 | `COLUMNSORT`/`COLUMNREORDER` **discarded the columns they were given** when fewer than three were named, duplicating the position columns as `Chromx`/`Posx` instead. `Select.parseArguments` replaced the caller's selection rather than prepending to it, and the exclusion set still held the discarded columns so they vanished from the remainder too. **Reached six commands** — also `HIDE`, `SELECT`, `TRYHIDE`, `TRYSELECT` — found by fixing two and watching 11 baselines move. This is the only change on the branch that alters output of queries that previously ran clean. | `cmd.columnsort.moves_named_columns_to_the_front` |
| 1.2 | `INVSTUDENT` **hung forever** on a probability outside `[0,1]`: colt's root finder never converges, so the query never returns and takes its worker with it. Tracked separately by [ENGKNOW-3776](https://genedx.atlassian.net/browse/ENGKNOW-3776). | `fn.invstudent.rejects_probability_above_one` |
| 1.3 | `LEFTWHERE` **crashed** with `tail of empty list` when the named column was the last one, and — once that was fixed — emitted a row with a trailing tab, one column wider than its own header. The line also sliced the header *string* using column indices. | `cmd.leftwhere.last_column_leaves_nothing_to_blank` |
| 1.4 | `GAVA -usePhase` was documented and implemented but **absent from the declaration**, so every invocation using it was rejected. | `cmd.gava.usephase_is_accepted` |
| 1.5 | `GAVA` **crashed** with a bare `ArrayIndexOutOfBoundsException` on an input narrower than the fixed column positions it falls back to. | `cmd.gava.narrow_input_is_rejected_not_crashed` |
| 1.6 | `GTLD`'s rejection message named `-sumLD`/`-calcLD`, **which are not flags of any command** — the registry has `-sum`/`-calc`. Anyone following the message failed again. | `cmd.gtld.rejection_names_the_real_flags` |
| 1.7 | `JOIN.rst` and `VARJOIN.rst` documented `-ic` as adding "a column named **overlap**"; the engine appends `OverlapCount`. A column name is compatibility surface. | `cmd.join.ic_adds_an_overlap_column` |
| 1.8 | The previous compatibility suite **had never compiled** — it imported `com.networknt.schema`, which was on no configuration, so `:gortools:test` did not build on `main` and its 1087 cases had never run. | Removed in the first commit of the engine branch |

---

## 2. Open — pinned, not fixed

Behaviour the suite now asserts, where a change would be caught, but which nobody
has decided is correct. **These are the ones worth a decision.**

### 2.1 Silent 32-bit integer overflow — raised on ENGKNOW-3780

```
2147483647 + 1  ->  -2147483648
2147483647 * 2  ->  -2
```

Expression arithmetic wraps rather than promoting or failing. Bounded by
measurement, and the bounds matter: **aggregation does not wrap** (`GROUP -sum` of
two values of 2e9 gives 4e9), a literal of 3e9 is carried at full width, and
mixing in a double promotes correctly. So the exposure is expression arithmetic on
values inside the range whose result leaves it — `calc X Pos*1000000`, or adding
two large integer columns.

Cases: `cmd.calc.integer_arithmetic_overflows_silently`,
`cmd.calc.multiplication_overflows_too`,
`cmd.group.sum_does_not_overflow_at_the_integer_limit`.

### 2.2 Silent non-finite results

`1/0` yields `Infinity` and `0/0` yields `NaN`, both without failing, so those
values travel downstream into whatever consumes the result.

Cases: `cmd.calc.division_by_zero_is_infinity_not_an_error`,
`cmd.calc.zero_over_zero_is_nan`.

### 2.3 Quoting a number silently changes the comparison

```
WHERE Pos > 99     -> both rows      (numeric)
WHERE Pos > '99'   -> no rows        (lexicographic: '100' sorts before '99')
WHERE Pos < '99'   -> both rows      (the opposite answer)
```

The query does not fail. It returns the wrong rows and looks fine — the worst
shape a difference can take. Worth considering whether a quoted literal compared
against a numeric column should warn.

Cases: `cmd.where.unquoted_number_compares_numerically`,
`cmd.where.quoted_number_compares_as_text`,
`cmd.where.quoted_number_reverses_the_expected_answer`.

### 2.4 `SUBSTR` is asymmetric about out-of-range arguments

An end past the end of the string is clamped; a start past it throws. One
out-of-range argument is forgiven and the other is not.

Cases: `fn.substr.end_beyond_the_string_is_clamped`,
`fn.substr.start_beyond_the_string_fails`.

### 2.5 `KING -sym` emits rows in a nondeterministic order

Identical values, different order between runs. Excluded by case id in
`inventory/exclusions.yml` rather than left to a probe that catches it only about
half the time. Worth fixing at source if pair ordering is meant to be stable.

### 2.6 `PIPESTEPS` reports a path's length in its errors

`begin 0, end -1, length 89` — the length of the project root path. Harmless in
itself, but it makes the output environment-dependent, which is why the case had
to be given a real dialog file before it could be pinned at all.

### 2.7 `PRGTGEN` crashes on a malformed likelihood triplet

A comma-separated `PL` value where the separator is `;` by default runs the parser
off the end of the string: `StringIndexOutOfBoundsException`. Same class as 1.5 —
bad data producing an internal exception rather than a message.

### 2.8 Undocumented conventions now pinned

Each of these is a real contract that nothing stated. A change to any would look
like an improvement and break callers silently.

| Convention | Case |
|---|---|
| Chromosomes sort **lexicographically** — `chr10` before `chr2`, `chr9` before `chrX` | `cmd.merge.orders_chromosomes_lexicographically` |
| Reading a source neither validates nor reorders it; the ordering contract belongs to whoever wrote the file | `cmd.gor.does_not_reorder_or_reject_an_unsorted_source` |
| `SORT` does **not** preserve arrival order at equal positions | `cmd.sort.equal_positions_are_ordered_by_the_remaining_columns` |
| `ROUND` breaks ties **upward toward positive infinity**, so `-2.5` gives `-2`, not `-3` | `fn.round.half_way_goes_up`, `fn.round.negative_half_way_goes_toward_positive` |
| `POSOF` counts from **zero**; the documentation never says | `fn.posof.position_is_zero_based` |
| `SUBSTR` starts at zero with an exclusive end — derived from the docs calling `substr(x,2,3)` the middle character of a five-character string | `fn.substr.start_is_zero_based_and_end_is_exclusive` |
| Doubles print in full to `10^7` then switch to scientific notation, and below `10^-3`; `4/2` prints as `2.0`, so comparing against the text `2` fails | `cmd.calc.a_double_at_ten_million_switches_to_scientific_notation`, `cmd.calc.exact_division_still_produces_a_double` |
| Without a reference build, every chromosome is assumed **250,000,000** long | `cmd.group.chromosome_length_falls_back_to_a_default` |
| Aggregate columns appear in a fixed order — `min, max, avg, sum` — regardless of the order the flags were written | `cmd.group.aggregate_columns_are_named_and_ordered_by_the_engine` |

### 2.9 397 registered flags are absent from their documentation page

Reported every run by the documentation cross-check, listed in
`inventory/gaps.txt`. Documentation work rather than code, but it is the largest
single gap the suite measures. Phantom flags — documented but not registered —
are at zero after the GTLD and JOIN/VARJOIN fixes.

---

## 3. Not reached, and why

Not defects. Recorded so nobody re-derives the reason.

| Surface | Reason |
|---|---|
| `LIFTOVER` | Needs a chain-file shape the fixtures do not build |
| `VARGROUP` | Rejects the bucket fixture's genotypes as "Inconsistent genotypes" |
| `PLINKREGRESSION` | Shells out to the external `plink2` binary |
| `CMD`, `SQL`, `GORSQL`, `NORSQL`, `EXEC` and the input sources of the same names | Shell out or need a live database |
| `WRITE`, `BINARYWRITE`, `TEE`, `TSVAPPEND` | Write outside the result stream; the output is not the row set |
| `PARTGOR`, `PARALLEL` | Need a dictionary table with `#{tags}`, or a nested gorpipe command |
| 9 command flags and 12 input-source flags | Read as strings with no documented argument name — a value would be a guess. Listed in `inventory/gaps.txt` |
| 24 functions | Report the clock, machine, JVM or build identity; screened by `CaseLint` |

---

## 4. Notes on the suite itself

Limitations of the harness, worth knowing before trusting a number from it.

- **The stability probe is probabilistic.** It runs a case three times, varying the
  project root's path length. That reliably catches output containing a path, but a
  case whose *row order* varies is caught only sometimes — `KING -sym` was caught,
  released, and had to be named explicitly. A known-unstable case belongs in
  `exclusions.yml`, not in the probe.
- **The static screen and the probe cover different blind spots**, and both are
  needed. The probe found `CPULOAD`, `FREEMEM`, `THREADID`; it could not find
  `MAXMEM`, `GORVERSION`, `IP` or `ARCH`, which are stable within a run and change
  between machines or commits. Four separate additions to the list came from
  baselines moving for no reason — the last, `MAJORVERSION`/`MINORVERSION`, from
  rebasing onto an upstream VERSION bump, which is exactly the event they track and
  exactly what makes them useless as behaviour.
- **Coverage is a poor measure of this suite.** It reaches 21.4% of `:gortools`
  instructions alone, but only **+2.2pp of branch coverage on top of the unit
  tests** — 85% of what it executes, they already cover. Its value is the 902
  committed outputs that fail when behaviour moves, which no ratio can see. Use
  `:compat:marginalCoverageReport`, not `:compat:coverageReport`, when quoting a
  number.
- **Gaps are split into excluded and reachable** in the report because most of what
  remains is deliberate; one combined number read as dozens of open leads when
  there were nine.
- **A repeated top-level key in a curated YAML file silently discards the earlier
  one.** This cost 13 cases before `CuratedYaml.requireUniqueTopLevelKeys` was
  added, and it had already happened twice.
