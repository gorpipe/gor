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
| 1.2 | `INVSTUDENT` **hung forever** on a probability outside `[0,1]`: colt's root finder never converges, so the query never returns and takes its worker with it. Tracked separately by [ENGKNOW-3776](https://genedx.atlassian.net/browse/ENGKNOW-3776); the fix that shipped is main's (#182), which also rejects a probability of 0 and a degrees-of-freedom below 1, and supersedes the branch's version. | `fn.invstudent.rejects_probability_above_one` |
| 1.3 | `LEFTWHERE` **crashed** with `tail of empty list` when the named column was the last one, and — once that was fixed — emitted a row with a trailing tab, one column wider than its own header. The line also sliced the header *string* using column indices. | `cmd.leftwhere.last_column_leaves_nothing_to_blank` |
| 1.4 | `GAVA -usePhase` was documented and implemented but **absent from the declaration**, so every invocation using it was rejected. | `cmd.gava.usephase_is_accepted` |
| 1.5 | `GAVA` **crashed** with a bare `ArrayIndexOutOfBoundsException` on an input narrower than the fixed column positions it falls back to. | `cmd.gava.narrow_input_is_rejected_not_crashed` |
| 1.6 | `GTLD`'s rejection message named `-sumLD`/`-calcLD`, **which are not flags of any command** — the registry has `-sum`/`-calc`. Anyone following the message failed again. | `cmd.gtld.rejection_names_the_real_flags` |
| 1.7 | `JOIN.rst` and `VARJOIN.rst` documented `-ic` as adding "a column named **overlap**"; the engine appends `OverlapCount`. A column name is compatibility surface. | `cmd.join.ic_adds_an_overlap_column` |
| 1.8 | The previous compatibility suite **had never compiled** — it imported `com.networknt.schema`, which was on no configuration, so `:gortools:test` did not build where it lived and its 1087 cases had never run. It existed only in an unpushed local commit and never reached `origin/main`. | Removed in the first commit of the engine branch |

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

### 2.8 `MAP -not` emits rows narrower than its own header

Documented as "Negate condition, i.e. NOTINSET", but it does not negate anything:

```
gor left.gor | MAP -c Ref -not map.tsv

Chrom  Pos  Ref  Alt  Label      <- 5 columns
chr1   100  A    G    alpha      <- 5, and this row matched, so NOTINSET should have dropped it
chr1   200  C    T               <- 4 columns
chr2   150  G    A               <- 4 columns
```

Every row is kept, including the matched one, and the rows that missed the lookup
are emitted one field short of the header. The harm is not cosmetic — a following
step cannot read them:

```
gor left.gor | MAP -c Ref -not map.tsv | calc N len(Label)   ->  ERROR
```

`MAP -not -b` is well formed (a 0/1 column, five fields throughout, though the
matched row still carries the mapped value rather than 1), and `INSET -c Ref -not`
does the sane thing: keeps only the unmatched rows and adds no column.

Not fixed, because the right behaviour is a judgement call — drop the matched rows
as NOTINSET implies, keep them all with the missing value, or reject `-not`
without `-b`. Same family as 1.3, where LEFTWHERE emitted a row one field *too
wide*; that one broke a parse downstream, this one breaks the next pipe step.

Correct behaviour around it is pinned by `cmd.map.drops_a_row_whose_lookup_misses`,
`cmd.map.m_supplies_the_value_for_a_failed_lookup` and
`cmd.inset.not_keeps_only_the_rows_absent_from_the_set`.

### 2.9 Undocumented conventions now pinned

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
| `NOR` over a nested GOR stream puts the row's own chromosome in `ChromNOR` (`chr1`), where `NORROWS` and file sources give `chrN` | `nested.nor_reads_a_nested_gor_stream` |

### 2.10 `CSVSEL -hide` blacklists values, not tags

Not a defect — the documentation says so plainly — but the flag name reads as if
it took tag ids, and a blacklist of tag ids silently hides nothing at all, since
no tag id ever equals a genotype value. Pinned from both sides so the rule cannot
drift: `cmd.csvsel.hide_blacklists_values_rather_than_tags` asserts what it does,
and `cmd.csvsel.hide_without_tag_is_rejected` asserts that `-hide` without `-tag`
is refused at parse time rather than ignored.

### 2.11 About 400 registered flags are absent from their documentation page

Reported every run by the documentation cross-check, listed in
`inventory/gaps.txt`. Documentation work rather than code, but it is the largest
single gap the suite measures. Phantom flags — documented but not registered —
are at zero after the GTLD and JOIN/VARJOIN fixes.


### 2.12 CREATE references are case-sensitive, but the documentation's example is not

`materializedViews.rst` creates `##GWASsubset##` and reads it back as
`[##gwassubset##]`, twice. The engine rejects that:

```
create ##Sub## = gor a.gor | top 1; gor [##sub##]
->  No reference to virtual file: [##sub##]
```

So the documented example does not run. Either the documentation is wrong, or
references were meant to be case-insensitive the way DEF macro names are (which
`script.def.macro_name_is_case_insensitive` confirms they are). A decision for
someone; until then the engine's answer is pinned.

Case: `script.create.reference_is_case_sensitive_despite_the_docs`.

### 2.13 TOP inside a nested right source restarts at every seek

```
gor a.gor | join -snpsnp -r <(gor a.gor | select 1,2 | top 2 | calc tag 'x')
```

From the start, the nested stream is `chr1:100, chr1:300`, yet `chr2:50` also
matches. ROWNUM in the same position numbers `chr2:50` as 1 rather than 3. Both are
consistent with the nested query restarting wherever JOIN seeks it; that mechanism
is inferred from the output, not read from the code. Either way, the right source
is not the relation the query appears to describe: it depends on where it was
seeked. `nestedStreams.rst` presents seekability as the advantage of nested streams
and says nothing about row-limiting or order-dependent steps inside them. TOP and
ROWNUM are measured; other order-dependent steps are likely affected but untested.

Cases: `nested.top_in_a_seekable_right_source_restarts_at_each_seek`,
`nested.rownum_in_a_seekable_right_source_restarts_at_each_seek`. The ordinary nested
JOIN is pinned separately by `nested.join_reads_a_nested_right_source`.

### 2.14 A dictionary with aliases adds a `Source` column without `-s`

`GOR.rst` describes a source column only under `-s`. Reading a `.gord` whose entries
carry an alias appends one named `Source`, holding each file's alias, with or
without `-s`; `-s name` renames it. A one-column `.gord` (files, no aliases) adds
no column. The engine names a default for it
(`DictionaryTableMeta.DEFAULT_SOURCE_COLUMN`), which suggests it is intended, but
where it is switched on was not traced. Either way, a query written from the
documentation gets a column it did not expect.

Cases: `dict.combines_its_files_in_genomic_order`, `dict.f_selects_files_by_tag`,
`dict.ff_reads_tags_from_a_file`, `dict.ff_accepts_a_nested_query`,
`dict.fs_tolerates_an_unknown_tag`.

### 2.15 PGOR inside a nested query is refused with a misleading message

`PGOR.rst` says PGOR cannot be used within nested queries, and it is refused, but
the message treats `pgor` as a file name:

```
gor <(pgor a.gor)  ->  Input source does not exist: pgor
```

Same family as 1.6: the rejection is right, the message sends the user the wrong way.

Case: `pgor.is_rejected_inside_a_nested_query`.

### 2.16 `PGOR -split` cuts chromosomes under GROUP, giving wrong counts

`PGOR.rst`: *"If any of the commands GROUP, GRANNO, RANK, or SORT are present in the
query using binsize other than 1 base, only whole chromosomes (contigs) are used,
avoiding boundary effect issues arising from partition of query range."* With
`-split`, that does not hold:

```
pgor -split 100000000 a.gor | group chrom -count     (three chr1 rows, 100 / 150M / 220M)

Chrom  bpStart  bpStop     allCount
chr1   0        250000000  1
chr1   0        250000000  1
chr1   0        250000000  1
```

Each partition aggregates its own rows and the partitions are concatenated, so a
per-chromosome count comes back as one row per partition. The documented
behaviour would give a single `chr1` row counting 3. The query does not fail, and
the output is well-formed: it looks right. Not measured without `-split`, where the
default comes from the build split configuration:
`pgor.distributive_aggregate_matches_gor` agrees with GOR, but its `chr1` rows are
200 bases apart, so it cannot show whether the default partitioning cuts a
chromosome.

Case: `pgor.split_cuts_a_chromosome_even_under_group`.

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
| `PARTGOR`, `PARALLEL` | Need a dictionary table with `#{tags}`, or a nested gorpipe command. Dictionaries are now covered (`dict.*`), so PARTGOR is reachable next; PARALLEL still needs its parts query |
| `-nf`, bucketed dictionaries | `-nf` only differs from `-f` when one file holds several tags, which needs a bucketed dictionary fixture |
| 12 input-source flags | Read as strings with no documented argument name — a value would be a guess. Listed in `inventory/gaps.txt` |
| `SIGNATURE -timeres`, `KING -sym` | Clock-derived and order-unstable respectively; excluded by name, their commands otherwise covered |
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
- **Coverage is a poor measure of this suite.** It reaches 25.7% of `:gortools`
  instructions alone, but only **+3.3pp of branch coverage on top of the unit
  tests** — most of what it executes, they already cover. Its value is the
  committed outputs (905 when this was measured) that fail when behaviour moves, which no ratio can see. Use
  `:compat:marginalCoverageReport`, not `:compat:coverageReport`, when quoting a
  number. Note also that 28% of `:gortools` is one generated class,
  `gorsat/parser/FunctionSignature`, whose 3,907 `getSignature` overloads are
  reachable only by direct invocation and never by running a query — excluding it,
  the figures are 35.4% for this suite against 51.0% for the unit tests.
- **Gaps are split into excluded and reachable** in the report because most of what
  remains is deliberate; one combined number read as dozens of open leads when
  there were nine. Both reached zero for commands and for flags, so every pipe
  command and every flag on one was covered or excluded with a reason. Merging
  main's LIFTOVER rework reopened 18 reachable flag gaps; `:compat:report` has
  the current figure. The
  report names the reachable flag gaps outright while there are few of them, since
  a count alone gave no way to find out which they were.
- **A repeated top-level key in a curated YAML file silently discards the earlier
  one.** This cost 13 cases before `CuratedYaml.requireUniqueTopLevelKeys` was
  added, and it had already happened twice.
- **The flag matrix tests one flag at a time**, so a flag that only means something
  beside another flag generates a case that records nothing: `CSVSEL -hide` is
  rejected without `-tag`, and `DAGMAP -ps` names the separator of a column only
  `-dp` turns on, so its generated output is byte-identical to the bare case. Those
  pairings are asserted as spec cases instead. The generated cases are kept, since
  what they record — the rejection, and the absence of an effect — is also real.
- **A command's required flags are curation, not discovery.** `INSET` and `DAGMAP`
  both reject a query with no `-c` before looking at anything else, so until
  `command-args.yml` named it, 16 of their generated baselines recorded one parse
  error and nothing about GOR. A baseline whose content is `ERROR: Value option -c
  is not found` is worth reading as a gap, not as coverage.
- **The duplicate-key rule applies one level down too.** Merging the two `PRGTGEN`
  sections left six flags listed twice inside the one block, where YAML again keeps
  the last and drops the first silently — SnakeYAML only warns. `CuratedYaml` now
  checks nested keys as well as top-level ones.
