# ENGKNOW-3657 Memory Findings

> Fill in after running the profiling runbook. This is the deliverable of the diagnosis task.

## Observed during harness build (pre-profiling)

1. **Harness validated — retained heap scales with dictionary size.** The load harness (`org.gorpipe.test.memory.*`, run via `./gradlew :test:slowTest --tests "org.gorpipe.test.memory.UTestTableServiceLoad"`) models a session/table cache by holding opened `GorDictionaryTable` instances for the run and measuring post-GC retained heap. Retained-heap delta grows with the `fileCount` (dictionary size) knob (validated, 2/2 stable runs). This confirms the harness exercises the suspected retention path (spec hypothesis #1) and is ready for profiling. Note: an earlier peak-heap-during-run metric did NOT scale — peak captured transient allocation churn, not retention; retained-heap-after-GC is the correct signal.

2. **Candidate root cause surfaced (needs profiling confirmation): `DictionaryEntries` lazy-load data race.** `model/src/main/java/org/gorpipe/gor/table/dictionary/DictionaryEntries.java` (~lines 127-133) guards its lazy load with a non-volatile `dataLoaded` boolean checked OUTSIDE the `synchronized loadLinesAndUpdateIndices()` — a double-checked-locking JMM race, despite the class javadoc claiming thread safety (line 40). Harmless when each request gets its own table instance (the field is only written once, idempotently), but REACHABLE if the real Table Service caches/shares `GorDictionaryTable` instances across concurrent reads. Worst case is a redundant reload (loadLinesAndUpdateIndices is idempotent + synchronized), not corruption — so this is a correctness/thread-safety smell to verify under profiling, and it directly relates to hypothesis #1 (whether dictionaries are retained/shared via a cache). Flag for a follow-up ticket if the profiler confirms table instances are cached/shared in production.

## Reproduction
- Prod heap ceiling used (`-Xmx`): __
- Knobs that triggered OOM: fileCount=__ tableCount=__ threads=__ duration=__
- Summary line from harness: __

## Dominant consumer
- Class / structure: __
- Retained size: __ MB (__ % of heap)
- Path to GC roots: __
- Retained past request end? (session cache / static): __

## Hypothesis verdicts
- [ ] DictionaryEntries retention (rawLines + multimaps): CONFIRMED / REFUTED — evidence: __
- [ ] 64 MB multipart write buffers: CONFIRMED / REFUTED — evidence: __
- [ ] Caches (tagsToListCache / client / metadata): CONFIRMED / REFUTED — evidence: __
- [ ] Off-heap / CRT S3 (RSS >> heap): CONFIRMED / REFUTED — evidence: __
- [ ] Unlisted consumer: __

## Recommendation
- Proposed fix direction: __
- Follow-up ticket: __
