# ENGKNOW-3657 — Table-service Memory Usage: Diagnosis Design

- **Ticket:** [ENGKNOW-3657](https://genedx.atlassian.net/browse/ENGKNOW-3657) — "GOR: Table-service memory usage"
- **Branch:** `ENGKNOW-3657-gor-table-service-memory-usage`
- **Status of problem:** Table-service pods hit OOMKilled under heavy load. Cause not yet pinned.
- **Scope of this task:** **Diagnosis only.** Reproduce, profile, and identify the dominant memory consumer with hard evidence. Any fix is a separate follow-up ticket.

## Goal

Reproduce the table-service OOM locally under a mixed read+write dictionary load, profile heap allocation and retention, and identify the **dominant** memory consumer, backed by heap-dump / JFR evidence. Deliver a findings document and a follow-up fix ticket.

## Evidence driving the design

From Grafana (`grafanacloud-genedx-prom`), crash window 2026-07-06:

- Request rate peaked at ~5-6 req/s; ~700 requests over an 8h window — **low throughput**.
- OOM occurred despite low volume ⇒ **cost is per-operation, not throughput-driven**. A few heavy operations (large dictionaries, large writes) exhaust the heap.
- Mixed read+write load; ~322 inserted lines over the 8h window.
- The `tableservice_requests_count` recording rule collapses the `request_type` and `table` labels, so per-type / per-table split is **not** available from Prometheus. Loki logs can refine the mix later if the harness needs tighter calibration.

**Implication:** the harness needs **modest concurrency but heavy per-operation weight** — large dictionaries and real writes — not high RPS.

## Where the harness lives

A new benchmark/test **in `gor-opensource`**. The memory-holding code lives here:

- `model` — `org.gorpipe.gor.table.dictionary.*` (dictionary/table data structures)
- `drivers` — `org.gorpipe.s3.driver.*` (S3 read/write, multipart buffers)

The table-service repo is a thin service over this library, so the leak is reproducible without deploying the k8s service. The harness runs as a `@Category(SlowTests)` JUnit test plus a `main` entrypoint for profiler-attached runs.

## Components

1. **Fixture builder** — generates synthetic dictionaries at prod-like scale. Configurable: number of lines, buckets, tags, and tables. Filesystem-backed first; an S3-backed variant second (to exercise the S3 driver and multipart write buffers).
2. **Load driver** — a thread pool issuing a mixed operation stream:
   - Reads: open dictionary, tag-filtered `getOptimizedEntries`, iterate results.
   - Writes: insert lines, bucketize, commit.
   - Knobs: read:write ratio, concurrency, dictionary size, duration.
   - Emits ops/s and latency so load is quantified.
3. **Memory observation** — runs at `-Xmx` set to the prod pod memory limit (pulled from the table-service helm values), with:
   - `-XX:+HeapDumpOnOutOfMemoryError`
   - JFR recording (allocation profile + old-gen occupancy)
   - periodic `jcmd <pid> GC.heap_info` logging
   - **RSS vs heap tracking** — the CRT-based S3 async client allocates off-heap; a heap dump alone would miss it. Use Native Memory Tracking (NMT) if off-heap growth is suspected.
4. **Analysis** — load the heap dump / JFR, rank retained size by class, and trace GC roots for the top consumers.

## Data flow

```
fixture builder → dictionaries (fs / S3)
       ↓
load driver (thread pool, mixed read+write)
       ↓
gor library: table/dictionary (model) → drivers/S3
       ↓
JFR + heap dump + GC.heap_info + RSS
       ↓
analysis → findings report
```

## Hypotheses to confirm or refute

The profiler decides — these are starting points, not conclusions.

1. **`DictionaryEntries` retention (top suspect).**
   `model/.../table/dictionary/DictionaryEntries.java` holds `rawLines: List<T>` plus two `ListMultimap`s (`tagHashToLines`, `contentHashToLines`). Memory scales with dictionary size × number of concurrently-held tables. Key question: are these retained past request end (e.g. via a session cache)?
2. **64 MB multipart write buffers.**
   `gor.s3.write.chunksize` defaults to `1<<26` (64 MB), buffered as `byte[]` per part. Concurrent writes multiply this: 64 MB × concurrency.
3. **Caches.**
   `DefaultTableAccessOptimizer.tagsToListCache` (100 full file-lists), the S3 client cache (CRT clients are heavy), and the S3 metadata cache (bounded at 10k — likely fine).
4. **An unlisted consumer** — surfaced by the retention profile.

## Deliverables

- A reusable load harness, checked in, which doubles as a perf/memory regression guard.
- A findings document: the dominant consumer, evidence (retained sizes, GC-root path), and a recommendation.
- A separate follow-up fix ticket.

## Testing the harness

- Reproduce OOM at prod-like `-Xmx`.
- Show memory scales with the dialed knob (dictionary size / concurrency) — confirms the harness stresses the right path.
- Keep the harness as a perf regression test.

## Risks and fallbacks

- **OOM won't reproduce locally** → capture a prod heap dump instead (add `-XX:+HeapDumpOnOutOfMemoryError` to the pod).
- **MinIO/localstack ≠ real S3 buffering** → run the S3 write-buffer variant against a real dev bucket.
- **Off-heap growth invisible to heap dump** → watch RSS vs heap; use NMT.

## Open inputs

- Prod pod memory limit (from table-service helm values / Grafana) → sets the harness `-Xmx`.
