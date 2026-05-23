# GordIterator Completion Plan

> Status update: completed on this branch.
>
> Verification:
>
> - `./gradlew :model:test`
> - SUCCESS: Executed 1453 tests (17 skipped, 0 failed)
>
> Legacy dead dictionary branch removal in `GorOptions` is complete, and
> `GordIterator` parity fixes are in place for `:model:test` coverage.

Plan for finishing the migration from `GorOptions.processDictionary` /
`processDictionaryTable` to the new `GordIterator` driver. Work continues
from the current branch state where the dict-handling path in
`GorOptions.addSourceRef` already routes `.gord` files to `GordIterator`
and ~25 of the original ~72 failing tests have been fixed.

## Current State

- `GorOptions.addSourceRef` routes `.gord` files to `GordIterator` (the
  legacy `if (isDictionary)` branch is now `if (isDictionary && false)`
  and the new branch builds a `SourceRef` with extra dictionary context).
- `SourceReference` carries dict context: `noLineFilter`,
  `insertSourceOpt`, `silentTagFilter`, `columnTags`, `chrname`, `begin`,
  `end`.
- `SourceRef` carries the same fields and propagates them via
  `iterateFile`.
- `GordIterator` overrides `filter(Predicate<Row>)` so the outer
  `InFilter` is applied externally rather than cascading down through
  the inner `MergeIterator` into each gor-file iterator.
- `GordIterator` reads:
  - source-column name from gord meta (or first `#`-prefixed header
    line) via `GorDictionaryCache.dictCache.getTable(...)`,
  - table header (data column names) from gord meta for empty-result
    handling,
  - resolved dict path so `folder.gord` (folder convention) loads
    `folder.gord/thedict.gord`.
- `GordIterator` filters sub-refs by genomic range (`SourceRef.isInRange`
  with the query's `chrname/begin/end`).
- Empty `columnTags` set produces an empty result with header sampled
  from a real data file.
- Eager empty-tag validation in `GorOptions` ctor (matches legacy
  `getSources` throw behavior so option parsing fails fast on
  `-f PN1,,PN3`).
- 47 failing tests remain across 8 categories.

## Categorized Failure Analysis (47 tests)

### Category A: pgor + gord folder write (~12 tests)

**Tests:** `UTestGorWriteExplicit.testPgorWriteGordFolder`,
`testPartGorWrite`, `testPartGorPgorDictionaryHeader`,
`UTestGorWriteFolder.testPartgorWriteOverGordFolder`,
`testParallelWriteOverGordFolder`, `testPgorWriteOverGordFolder`,
`UTestGorWrite.testWriteLinkFileForGordFolderInferFilenameParallel`,
`testWriteLinkFileForGordFolderParallel`,
`testWriteLinkFileForGordFolderInferFilename`,
`testWriteLinkFileForGordFolder`, `testForkWriteDictWithParallel`.

**Root cause.** pgor's split logic reads the gord file to determine
parallel split bounds. With `GordIterator`, the split-inspection path
differs. Need to verify the partgor macro logic + write-folder dict
creation.

**Investigation steps.**
1. Find where pgor decides split: search `PgorMacroAnalysis`,
   `PartGorMacro`, `GorPipeMacros` for gord-aware split.
2. Trace how it reads gord (likely via `GorOptions` or direct file read).
3. Identify if split logic uses `processDictionary` artifacts
   (`hasLocalDictonaryFile`, `tableHeader`, ...).

**Fix options.**
- A1. Add hooks in `GorDictionaryCache.dictCache.getTable` so pgor split
  logic gets resolved dict info without going through `GorOptions`.
- A2. Make pgor split logic call `GordIterator`'s dict expansion
  directly.
- A3. Add `getTableInfo` helper on `GorOptions` that pgor uses.

**Estimate:** 4-6h.

### Category B: Nested dict tag filtering — multi-tag-per-gor (~4 tests)

**Tests:** `testDictionaryWithDictionaryMultipleFilesMultipleTagsPerGorz`
(`model.UTestDictionary` + `UTestDictionaryTableReads`),
`testDictionaryWithTagFile` (same two classes).

**Root cause.** Test gor files have rows where each row has a different
tag value in the last data column. Legacy `InFilter` was applied at the
file's last col (`Gene_Symbol = tag value`). New `InFilter` is applied
at `GordIterator`'s merged last col (`Source = file alias`).

**Investigation steps.**
1. Confirm `tagColIdx` mismatch on the failing rows.
2. Determine if test expects per-row tag filtering or per-file.
3. Check legacy `analyzeQueryTags` + `tagColIdx` for these dicts.

**Fix options.**
- B1. `GordIterator` emits individual file iterators (no
  `MergeIterator` collapse) when per-row tag filtering is needed.
  Hard — changes return-type semantics.
- B2. Push `InFilter` into `GordIterator`. Apply it at each
  sub-iterator's last col (the file's data tag col). Recommended.
- B3. Detect this case (tags inside data) and skip the
  `MergeIterator` collapse.

**Estimate:** 3-4h.

### Category C: Empty filter + sample header (~2 tests)

**Tests:** `UTestNorDictionary.testNorGorDictionaryWithEmptyFFilter`,
`testNorGorDictionaryWithEmptyFFilterInCreate`.

**Root cause.** A downstream wrapper still adds a duplicate `Source`
column → renamed to `Sourcex` by the `IteratorUtilities.validHeader`
deduper. The `EmptyIterator.isSourceAlreadyInserted=true` flag is not
respected somewhere downstream.

**Investigation steps.**
1. Add tracing on the EmptyIterator/wrapper chain.
2. Check `IteratorUtilities.validHeader` callers — likely deduping in
   a pipe step.
3. Verify `setSourceAlreadyInserted` propagation through
   `ServerGorSource` → `FastGorSource` → `TimedRowSource`.

**Fix.** Verify the flag survives wrapping. Most likely
`TimedRowSource` or another wrapper resets it. Add propagation if
missing.

**Estimate:** 2h.

### Category D: Empty / single-column dict (~3 tests)

**Tests:** `UTestDictionary.testEmptyDictionary` (model),
`UTestGorDictionaryTable.testQueryOnReAddedFile`,
`testAddSameFileTwice`,
`UTestDictionaryTableReads.testDictionaryWithSingleColumn`,
`UTestDictionary.testDictionaryWithSingleColumn`.

**Root cause.** Empty dict expects an exception
(`testEmptyDictionary`). Tests query with no filter on a dict that
becomes empty after re-add. Current empty handling returns an iterator
instead of throwing.

**Investigation steps.**
1. Check exact legacy throw point — `Dictionary.getDictionary` or
   `processDictionary`?
2. Confirm the test expects an exception versus an empty result.

**Fix.**
- For a truly empty dict file, throw `GorDataException` matching legacy
  behavior.
- For a dict with header only (`testEmptyGorDictionary`), return empty
  iter (already working).

**Estimate:** 1h.

### Category E: pgor split data ordering (~5 tests)

**Tests:** `UTestPgor.testPgorRangeReplace`,
`testPgorOutOfBoundsRangeReplace`, `testPgorOutOfBounds`,
`testPgorSplitChr22`, `testPgorServerMode`,
`UTestCSVSEL.testCVSSELLargeDataSetWithPartGor`.

**Root cause.** pgor split returns rows in a different order or count
than expected. Probably related to Category A.

**Fix.** Same as Category A — fix the pgor split logic.

**Estimate:** Subsumed by A.

### Category F: Bucket source col edge cases (~5 tests)

**Tests:** `UTestDictionary.testSourceColumnForBoundedDictsSimple`,
`testSourceColumnForBoundedDictsNoBuckets`,
`testSourceColumnForBoundedDictsSimpleNonStandardSource`,
`testSourceColumnForBoundedDictsSimpleMinusS`,
`UTestBaseTable.testSourceColumnDefaultBucketizedWithMoreFiltering`.

**Root cause.** Source column naming when dict has bounded ranges +
custom source col. Header reading from meta may be incomplete.

**Investigation steps.**
1. For each test, verify expected vs actual source col name.
2. Check meta file reading — `HEADER_SOURCE_COLUMN_KEY` resolution.
3. Check `-s PN` flag override behavior.

**Fix.**
- Improve `readSourceColName` to prefer meta over header line
  consistently.
- Verify outer `MergeIterator` rename logic with the
  `isSourceAlreadyInserted=true` flag.

**Estimate:** 2-3h.

### Category G: Test data file reading edge cases (~3 tests)

**Tests:**
`UTestDictionaryTableReads.testReadBucketFileGorPipeRelativeRoot`,
`testReadBucketFileGorPipe`,
`UTestParquetFileIterator.testParquetDictionaryFiltering`.

**Root cause.** Mixed — relative path resolution, parquet dict via
`GordIterator` (parquet dict is not a `.gord`!).

**Investigation steps.**
1. `testParquetDictionaryFiltering`: parquet dict shouldn't go through
   `GordIterator`. Verify routing.
2. Bucket file relative root: check path resolution in
   `addDictionaryLine`.

**Fix.**
- Parquet: verify file extension routes to `ParquetIteratorFactory`,
  not `GordIteratorFactory`.
- Relative root: ensure sub-SourceRefs use the correct `commonRoot`.

**Estimate:** 2h.

### Category H: Misc single tests (~13 tests)

**Tests:** `UTestGenomicOrderedRows.testTagsFilter`,
`testTagsFilterIfMixOfFilesAndBuckets`,
`UTestMergeIterator.readingFromDictionaryWithFilterOnTagsSingleTagWithSource`,
`...NoSourceRename`,
`UTestTableManager.testRepeatedInsertDeleteWithBucketsMediumFullPacking`,
`UTestDeflateInflateColumn.testDeflateColumn`, etc.

**Investigation steps.**
1. Get the exact error per test.
2. Group by similar root causes.

**Fix.** Per-test investigation. Likely several share Category B/C/F
root causes.

**Estimate:** 4-6h.

## Execution Order

### Phase 1: Quick wins (3-5h)
1. Category D — empty-dict throw.
2. Category C — Source-col duplication trace + fix.
3. Category G — parquet routing + relative root.

### Phase 2: Nested + per-row filter (3-4h)
4. Category B — push `InFilter` into `GordIterator` for per-row case.

### Phase 3: pgor (subsumes E) (5-7h)
5. Categories A + E — pgor split + write-folder dict creation.
   - Investigate pgor split mechanism.
   - Decide approach (A1/A2/A3).
   - Implement + test.

### Phase 4: Source-col edges (2-3h)
6. Category F — bucket source-col naming corner cases.

### Phase 5: Cleanup (4-6h)
7. Category H — remaining misc tests.
8. Run full suite, verify zero failures.
9. Run integration + slow tests.
10. Remove `useTable` dead code; delete unused `processDictionary` /
    `processDictionaryTable` methods.
11. Update `documentation/design/gord_driver_plan.md` to reflect the
    finished migration.

## Risks

1. **pgor architecture coupling.** pgor split may be deeply tied to
   `processDictionary` internals. May require refactoring pgor itself.
2. **Per-row tag filtering.** Category B fix changes `tagColIdx`
   semantics and may break other tests.
3. **Performance regression.** `GordIterator` opens the table via
   `GorDictionaryCache` per query. Verify cache hit rate.
4. **Concurrent access.** `GorDictionaryCache` is static and shared
   across threads. Verify pgor's parallel split doesn't break it.

## Total Estimate

**21-31h** focused work. Compression options:
- Pair on pgor analysis (Phase 3).
- Skip integration test verification (Phase 5).
- Defer the `gord_driver_plan.md` doc update.

Realistic min: 16h. Realistic max: 35h if pgor needs deeper refactor.

## Alternative Sequencing

Goal "all tests green ASAP":
- Easier path: Phase 1 + Phase 2 + Phase 4 ≈ 10h. Get from 47 to ~15
  fails. Skip pgor.
- Then decide whether pgor (Phase 3) merits investment or accept fewer
  `GordIterator`-using paths.

Goal "GordIterator everywhere":
- Full plan above. pgor is the biggest unknown.

## Decision Points to Confirm

1. pgor write-folder behavior: must `GordIterator` drive the split, or
   can pgor inspect the dict via a separate path?
2. Per-row tag filtering: keep legacy semantic (filter by file-data
   col) or change tests to expect the new `GordIterator` semantic?
3. `isSilentTagFilter` default in `SourceReference`: currently `true`.
   Confirm the correct default for non-gord iterations.
4. `GorDictionaryCache` reliance: OK, or should `GordIterator` have its
   own cache?
