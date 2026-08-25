# ENGKNOW-3657 Profiling Runbook

## 1. Get the prod heap ceiling
Fetch the table-service pod memory limit from its helm values (or Grafana
`kube_pod_container_resource_limits{resource="memory"}`). Use it as `-Xmx` below
so the local run reproduces the OOM.

## 2. Build the classpath
```bash
./gradlew :test:testClasses
CP=$(./gradlew -q :test:printTestClasspath 2>/dev/null || echo "see note")
# If :printTestClasspath is not defined, run via gradle JavaExec or use the
# test runtime classpath from `./gradlew :test:dependencies`.
```

## 3. Run under JFR + heap-dump-on-OOM at the prod ceiling
```bash
java -Xmx<PROD_LIMIT> \
     -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/engknow3657.hprof \
     -XX:StartFlightRecording=filename=/tmp/engknow3657.jfr,settings=profile,dumponexit=true \
     -Dgor.memtest.fileCount=20000 -Dgor.memtest.tableCount=16 \
     -Dgor.memtest.threads=8 -Dgor.memtest.readWritePercent=70 \
     -Dgor.memtest.durationSeconds=300 \
     -cp "$CP" org.gorpipe.test.memory.TableServiceLoadMain
```
Ramp `fileCount` / `tableCount` until it OOMs at the prod ceiling.

## 4. Analyze
- Heap dump: open `engknow3657.hprof` in Eclipse MAT or `jhat`. Sort by
  **retained size**. Run "Dominator Tree" and "Path to GC Roots" on the top
  objects. Check the hypotheses: `DictionaryEntries.rawLines` /
  `tagHashToLines` / `contentHashToLines`; `byte[]` part buffers (~64 MB);
  cache maps.
- JFR: open `engknow3657.jfr` in JDK Mission Control → Memory → Allocation.
  Identify top allocation sites.
- Off-heap: compare `peakRssMB` vs `peakHeapMB` from the harness summary. A
  large gap points at native/CRT S3 buffers — rerun with `-XX:NativeMemoryTracking=summary`
  and `jcmd <pid> VM.native_memory summary`.

## 5. Fallback: prod heap dump
If it will not reproduce locally, add `-XX:+HeapDumpOnOutOfMemoryError`
`-XX:HeapDumpPath=/data` to the table-service pod JVM args and pull the dump
after the next OOM.
