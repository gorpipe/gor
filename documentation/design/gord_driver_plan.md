## Plan: Add `.gord` Driver Support to the GOR Driver Framework

### 1) Confirm expected `.gord` behavior and scope
- Define what `.gord` should do in the driver framework (read semantics, filtering behavior, metadata behavior, tag handling, partition behavior).
- Decide whether support should include only read-path first, or also metadata/index optimizations.
- Align expected behavior with current runtime behavior for dictionary files (`.gord`) to avoid regressions.

### 2) Map current dictionary handling path vs driver framework path
- Trace where `.gord` is currently handled outside the driver framework.
- Identify all entry points where `DataType.GORD` is resolved and dispatched.
- Document integration points needed so `StreamSourceIteratorFactory` can own `.gord` consistently.

### 3) Define target architecture for `.gord` driver
- Define package and class layout under `model/src/main/java/org/gorpipe/gor/driver/providers/stream/datatypes/gord/`.
- Plan core classes:
  - `GordFile` (extends `StreamSourceFile` or relevant specialized source file)
  - `GordIteratorFactory` (implements `StreamSourceIteratorFactory`, with `@AutoService`)
  - iterator implementation(s) for dictionary expansion/streaming
  - optional meta iterator strategy (reuse `FileMetaIterator` or custom)
- Decide reuse boundaries with existing dictionary/parsing utilities to avoid duplicated logic.

### 4) Resolve `DataType.GORD` framework registration details
- Revisit `DataType.GORD` contract and its `hasDependents` handling.
- Plan required updates so `.gord` can be safely recognized by stream provider registration and dependency/index resolution.
- Validate implications for link types and `fromFileName` behavior.

### 5) Define iterator behavior contract
- Specify how `.gord` iterator emits rows (ordering guarantees, segment filtering, tag/sample filtering).
- Specify behavior for nested dictionaries, relative paths, and missing entries.
- Specify error behavior (invalid dictionary lines, unreadable referenced files, mixed schema).
- Specify compatibility constraints with existing GOR query commands (`gor`, `pgor`, `nor -asdict`, joins).

### 6) Define metadata behavior
- Decide what `createMetaIterator` should return for `.gord`:
  - dictionary-level metadata,
  - pass-through from referenced files,
  - or both.
- Define how metadata rows are normalized and labeled.

### 7) Define performance and scalability approach
- Plan strategy for large dictionaries (lazy loading vs eager parsing).
- Plan batching/streaming and resource lifecycle for underlying iterators.
- Plan optional support for seek/index behavior where meaningful.

### 8) Define compatibility and migration strategy
- Ensure behavior parity with current non-driver `.gord` handling.
- Decide rollout strategy (feature flag, staged enablement, or full switch).
- Plan fallback behavior if new driver path fails.

### 9) Test plan (unit + integration)
- Add unit tests for:
  - source resolution,
  - dictionary parsing,
  - tag/filter handling,
  - nested dictionary behavior,
  - metadata behavior,
  - error scenarios.
- Add integration tests comparing old vs new behavior on representative `.gord` datasets.
- Add regression tests for commands known to rely on `.gord` semantics.

### 10) Validation gates before merge
- Run module build and all relevant test suites.
- Run lint/type/diagnostic checks used by this repo.
- Verify no behavior changes for existing drivers (`gorz`, `vcf`, `bam`) and no registration conflicts.

### 11) Implementation checklist (execution order)
1. Finalize scope/behavior spec for `.gord`.
2. Implement `gord` datatype package and `GordIteratorFactory` skeleton.
3. Implement `GordFile` resolution and source wiring.
4. Implement core iterator logic for dictionary expansion.
5. Implement metadata iterator behavior.
6. Update data type/registration integration points.
7. Add comprehensive tests.
8. Run full validation and fix issues.
9. Remove temporary toggles (if any) and finalize.
