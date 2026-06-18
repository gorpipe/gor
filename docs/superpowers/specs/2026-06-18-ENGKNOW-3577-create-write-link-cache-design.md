# ENGKNOW-3577 — `create` with `write -link` stores wrong name in cache

**Type:** Bug fix
**Ticket:** [ENGKNOW-3577](https://genedx.atlassian.net/browse/ENGKNOW-3577) (Bug, Highest)
**Branch:** `add-test-server` (current)
**Date:** 2026-06-18

## Problem

A `create` whose query ends in `write -link <linkfile>` (no explicit data filename) stores
an incorrect data-file name in the create cache. Referencing the create afterwards fails
because the cached name points at a file that was never written.

Reproduction:

```
create #test# = gorrows -p chr1:1-100
    | write -link test.gorz.link
;

nor [#test#]
```

`nor [#test#]` fails — the cache holds a data-file name that does not exist on disk.

## Root cause

`LinkFileUtil.inferDataFileNameFromLinkFile` (`model/.../linkfile/LinkFileUtil.java:50`)
derives the data-file path for a `-link` write. Its own doc comment (lines 23–31) states the
result **must be idempotent** ("we can not use random or time in the path"), but line 89
injects a random 3-character suffix:

```java
var uniqueIdPart = (link.getSerial() + 1) + "-" + RandomStringUtils.insecure().nextAlphanumeric(3);
```

The random suffix was added in ENGKNOW-3362 (versioned-link integrity). The serial portion is
stable across calls within one create (link file not yet written → serial = 1 both times);
only the random portion differs.

For a plain `create ... | write -link X`, the data-file name is derived **twice**:

1. **Plan time** — `BaseScriptExecutionEngine.resolveCache`
   (`gortools/.../Script/BaseScriptExecutionEngine.java:67`) calls
   `Write.parseBaseOptions`, which (filename empty + `-link` present) calls
   `inferDataFileNameFromLinkFile` → **name #1** (random A). Returned as the create cache result.
2. **Exec time** — the executed query still carries the bare `write -link X` (the query is
   only mutated in the *empty-outFile* branch, lines 72–81; the link case leaves it untouched
   because `outFile` is non-empty). `ForkWrite` re-parses the write → `inferDataFileNameFromLinkFile`
   → **name #2** (random B). Data + link file written here.

name #1 ≠ name #2. Cache → #1, data → #2. Reference of `[#test#]` resolves to #1, which does
not exist.

`pgor`/`partgor`/`parallel` creates use a parallel path
(`MacroUtilities.getCachePath`, `gortools/.../Utilities/MacroUtilities.scala:452`) with the same
double-derivation shape, but those write gord folders with different naming. **Out of scope**
for this ticket (see Follow-ups).

## Fix — derive once, reuse the name

In `resolveCache`, when the write filename was inferred from `-link` (no positional file
argument **and** `-link` present), inject the already-resolved `outFile` into the executed query
as the explicit write target — the same mutation pattern already used for the empty-outFile
branch.

Effect:

- `ForkWrite` parses an explicit filename → **does not re-derive** → no second random suffix.
- Data written to #1, link file points to #1, cache stores #1. All three agree.
- The mutation is also self-stabilising: a subsequent parse sees the positional filename and
  skips inference entirely (idempotent).

No change to `LinkFileUtil` — the ENGKNOW-3362 uniqueness/integrity behaviour is preserved.

### Detection of the link-inferred case

Inside `resolveCache`:

- `iargs = write.validateArguments(args)` is empty (no positional data filename), **and**
- the `-link` option is present in `args`.

When both hold and `outFile` is non-empty (i.e. it came from link inference), append `outFile`
to `queryBlock.query()` so the executed `write` becomes
`... | write <outFile> -link <linkfile>`.

### Touch points

| File | Change |
|------|--------|
| `gortools/src/main/java/gorsat/Script/BaseScriptExecutionEngine.java` | `resolveCache`: inject resolved data-file name into the query for the link-inferred case |
| `gortools/src/test/java/gorsat/UTestGorWrite.java` | New regression test |

## Regression test

Model on `testWriteLinkFileWithInferFileName` (`UTestGorWrite.java:246`).

```
create #test# = gorrows -p chr1:1-100 | write -link test.gorz.link ; nor [#test#]
```

Assertions:

1. The query runs without error (currently throws — file not found).
2. `nor [#test#]` returns the expected 99 rows.
3. The data file referenced by `test.gorz.link` exists, and the cached create result resolves
   to that same file (cache name == actual written name).

Test must **fail before** the fix and **pass after**.

## Out of scope / Follow-ups

- `MacroUtilities.getCachePath` (pgor/partgor/parallel link writes) — same double-derivation
  shape. Verify whether it exhibits the same defect; file a follow-up if so.
- Making `inferDataFileNameFromLinkFile` idempotent again (removing the random suffix) was
  rejected: it risks regressing the ENGKNOW-3362 concurrent-write uniqueness guarantee.
- The doc comment on `inferDataFileNameFromLinkFile` (`LinkFileUtil.java:23-31`) promises an
  idempotent result, which the random suffix (line 89) violates. This fix works *around* the
  non-idempotency rather than removing it, so the comment is now misleading — update the comment
  (or the implementation) in a follow-up.

## Verification

```bash
./gradlew :gortools:test --tests "gorsat.UTestGorWrite.testCreateWriteLinkCacheName"
./gradlew :gortools:test --tests "gorsat.UTestGorWrite"
```
