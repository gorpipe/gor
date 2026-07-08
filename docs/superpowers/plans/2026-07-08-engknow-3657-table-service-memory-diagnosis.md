# Table-service Memory Diagnosis Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a reusable local load harness that reproduces the table-service OOM under mixed read+write dictionary load, profile it, and identify the dominant memory consumer with evidence.

**Architecture:** A Java harness in the `test` module (which depends on all main modules and holds shared test infra like `GorDictionarySetup`). A fixture builder generates dictionaries at prod-like scale; a thread-pool load driver issues a mixed read+write operation stream against `GorDictionaryTable`; a memory sampler records peak heap and RSS. A `main` entrypoint runs it under JFR / `-XX:+HeapDumpOnOutOfMemoryError` at a prod-like `-Xmx`; a `@Category(SlowTests)` JUnit validates the harness stresses the target path. A runbook + findings doc capture the diagnosis.

**Tech Stack:** Java 17, JUnit 4 (`@Category(org.gorpipe.test.SlowTests)`), Gradle (`slowTest` task), JFR, `jcmd`, `java.lang.management` MXBeans. Dictionary API: `org.gorpipe.gor.table.dictionary.gor.GorDictionaryTable`. Fixture helper: `org.gorpipe.test.GorDictionarySetup`.

## Global Constraints

- Package for all new harness code: `org.gorpipe.test.memory`.
- Harness production code lives in `test/src/main/java/...`; harness tests in `test/src/test/java/...`.
- Copyright header: copy the `BEGIN_COPYRIGHT ... END_COPYRIGHT` block verbatim from any existing file in the `test` module (e.g. `test/src/main/java/org/gorpipe/test/GorDictionarySetup.java`) at the top of every new `.java` file.
- Scope is **diagnosis only** — no fix to `DictionaryEntries`, S3 buffers, or caches in this plan. The fix is a separate ticket.
- Slow/heavy tests use `@org.junit.experimental.categories.Category(org.gorpipe.test.SlowTests.class)` so they run under `./gradlew :test:slowTest`, not the default `test` task.
- Commit trailer on every commit:
  ```
  Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
  ```

## File Structure

- Create `test/src/main/java/org/gorpipe/test/memory/MemoryLoadConfig.java` — immutable config of all load knobs (dict size, buckets, tags, tables, concurrency, read:write ratio, duration), built from defaults + system properties.
- Create `test/src/main/java/org/gorpipe/test/memory/MemorySampler.java` — background sampler recording peak heap-used and peak RSS.
- Create `test/src/main/java/org/gorpipe/test/memory/DictionaryFixture.java` — builds dictionaries at scale on a filesystem or S3 root using `GorDictionarySetup`.
- Create `test/src/main/java/org/gorpipe/test/memory/TableServiceLoadDriver.java` — thread pool issuing mixed read+write ops against `GorDictionaryTable`; returns an op-count summary.
- Create `test/src/main/java/org/gorpipe/test/memory/TableServiceLoadMain.java` — `main` entrypoint: config → fixture → driver → summary; for profiler-attached runs.
- Create `test/src/test/java/org/gorpipe/test/memory/UTestTableServiceLoad.java` — `@Category(SlowTests)` test validating the harness stresses the dictionary path (peak heap scales with dict-size knob).
- Create `docs/superpowers/runbooks/2026-07-08-engknow-3657-profiling-runbook.md` — exact commands to run the harness under JFR + heap-dump at prod `-Xmx`, and how to analyze the dump.
- Create `docs/superpowers/findings/2026-07-08-engknow-3657-memory-findings.md` — findings template to fill after profiling.

---

### Task 1: MemoryLoadConfig — load knobs

**Files:**
- Create: `test/src/main/java/org/gorpipe/test/memory/MemoryLoadConfig.java`
- Test: `test/src/test/java/org/gorpipe/test/memory/UTestMemoryLoadConfig.java`

**Interfaces:**
- Consumes: nothing.
- Produces: `MemoryLoadConfig` with public final int/long fields `fileCount`, `rowsPerChr`, `bucketSize`, `tableCount`, `threads`, `readWritePercent` (0-100, percent of ops that are reads), `durationSeconds`; static factory `MemoryLoadConfig.fromSystemProperties()`; constructor `MemoryLoadConfig(int fileCount, int rowsPerChr, int bucketSize, int tableCount, int threads, int readWritePercent, int durationSeconds)`.

- [ ] **Step 1: Write the failing test**

```java
package org.gorpipe.test.memory;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UTestMemoryLoadConfig {

    @Test
    public void defaultsAreSane() {
        MemoryLoadConfig c = new MemoryLoadConfig(1000, 100, 100, 4, 8, 70, 30);
        assertEquals(1000, c.fileCount);
        assertEquals(70, c.readWritePercent);
        assertTrue(c.threads > 0);
    }

    @Test
    public void systemPropertiesOverrideDefaults() {
        System.setProperty("gor.memtest.fileCount", "5000");
        try {
            MemoryLoadConfig c = MemoryLoadConfig.fromSystemProperties();
            assertEquals(5000, c.fileCount);
        } finally {
            System.clearProperty("gor.memtest.fileCount");
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :test:test --tests "org.gorpipe.test.memory.UTestMemoryLoadConfig"`
Expected: FAIL — `MemoryLoadConfig` does not exist (compilation error).

- [ ] **Step 3: Write minimal implementation**

```java
package org.gorpipe.test.memory;

/** Immutable set of load knobs for the table-service memory harness. */
public final class MemoryLoadConfig {
    public final int fileCount;      // data files per dictionary (~ dictionary lines)
    public final int rowsPerChr;     // rows per chromosome per data file
    public final int bucketSize;     // files per bucket
    public final int tableCount;     // dictionaries held concurrently
    public final int threads;        // load driver concurrency
    public final int readWritePercent; // percent of ops that are reads (0-100)
    public final int durationSeconds;

    public MemoryLoadConfig(int fileCount, int rowsPerChr, int bucketSize, int tableCount,
                            int threads, int readWritePercent, int durationSeconds) {
        this.fileCount = fileCount;
        this.rowsPerChr = rowsPerChr;
        this.bucketSize = bucketSize;
        this.tableCount = tableCount;
        this.threads = threads;
        this.readWritePercent = readWritePercent;
        this.durationSeconds = durationSeconds;
    }

    public static MemoryLoadConfig fromSystemProperties() {
        return new MemoryLoadConfig(
                intProp("gor.memtest.fileCount", 2000),
                intProp("gor.memtest.rowsPerChr", 100),
                intProp("gor.memtest.bucketSize", 100),
                intProp("gor.memtest.tableCount", 8),
                intProp("gor.memtest.threads", Runtime.getRuntime().availableProcessors()),
                intProp("gor.memtest.readWritePercent", 70),
                intProp("gor.memtest.durationSeconds", 60));
    }

    private static int intProp(String key, int def) {
        return Integer.parseInt(System.getProperty(key, String.valueOf(def)));
    }

    @Override
    public String toString() {
        return "MemoryLoadConfig{fileCount=" + fileCount + ", rowsPerChr=" + rowsPerChr
                + ", bucketSize=" + bucketSize + ", tableCount=" + tableCount
                + ", threads=" + threads + ", readWritePercent=" + readWritePercent
                + ", durationSeconds=" + durationSeconds + "}";
    }
}
```

(Prepend the copyright header block above the `package` line.)

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :test:test --tests "org.gorpipe.test.memory.UTestMemoryLoadConfig"`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add test/src/main/java/org/gorpipe/test/memory/MemoryLoadConfig.java \
        test/src/test/java/org/gorpipe/test/memory/UTestMemoryLoadConfig.java
git commit -m "$(cat <<'EOF'
test(ENGKNOW-3657): add MemoryLoadConfig knobs for memory harness

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: MemorySampler — peak heap + RSS

**Files:**
- Create: `test/src/main/java/org/gorpipe/test/memory/MemorySampler.java`
- Test: `test/src/test/java/org/gorpipe/test/memory/UTestMemorySampler.java`

**Interfaces:**
- Consumes: nothing.
- Produces: `MemorySampler` with `void start()`, `void stop()`, `long peakHeapUsedBytes()`, `long peakRssBytes()` (returns `-1` if RSS unavailable on this OS), `long currentHeapUsedBytes()`.

- [ ] **Step 1: Write the failing test**

```java
package org.gorpipe.test.memory;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class UTestMemorySampler {

    @Test
    public void recordsPeakHeapAfterAllocation() throws Exception {
        MemorySampler sampler = new MemorySampler(50);
        sampler.start();
        byte[][] hold = new byte[64][];
        for (int i = 0; i < hold.length; i++) {
            hold[i] = new byte[1024 * 1024]; // 64 MB total
            Thread.sleep(5);
        }
        sampler.stop();
        assertTrue("peak heap should exceed 32MB", sampler.peakHeapUsedBytes() > 32L * 1024 * 1024);
        assertTrue("hold retained", hold[0][0] == 0); // keep alive
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :test:test --tests "org.gorpipe.test.memory.UTestMemorySampler"`
Expected: FAIL — `MemorySampler` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package org.gorpipe.test.memory;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Background thread sampling heap-used (via MXBean) and RSS (via /proc on Linux). */
public class MemorySampler {
    private final long intervalMillis;
    private volatile boolean running;
    private volatile long peakHeap;
    private volatile long peakRss = -1;
    private Thread thread;

    public MemorySampler(long intervalMillis) {
        this.intervalMillis = intervalMillis;
    }

    public void start() {
        running = true;
        thread = new Thread(this::loop, "memory-sampler");
        thread.setDaemon(true);
        thread.start();
    }

    private void loop() {
        while (running) {
            sampleOnce();
            try { Thread.sleep(intervalMillis); } catch (InterruptedException e) { return; }
        }
    }

    private void sampleOnce() {
        long heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
        if (heap > peakHeap) peakHeap = heap;
        long rss = readRssBytes();
        if (rss > peakRss) peakRss = rss;
    }

    /** Reads VmRSS from /proc/self/status. Returns -1 if unavailable (e.g. macOS). */
    private long readRssBytes() {
        Path status = Path.of("/proc/self/status");
        if (!Files.exists(status)) return -1;
        try {
            List<String> lines = Files.readAllLines(status);
            for (String line : lines) {
                if (line.startsWith("VmRSS:")) {
                    String[] parts = line.trim().split("\\s+"); // "VmRSS:  12345 kB"
                    return Long.parseLong(parts[1]) * 1024;
                }
            }
        } catch (Exception e) {
            return -1;
        }
        return -1;
    }

    public void stop() {
        running = false;
        if (thread != null) thread.interrupt();
        sampleOnce();
    }

    public long peakHeapUsedBytes() { return peakHeap; }
    public long peakRssBytes() { return peakRss; }
    public long currentHeapUsedBytes() {
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    }
}
```

(Prepend copyright header.)

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :test:test --tests "org.gorpipe.test.memory.UTestMemorySampler"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add test/src/main/java/org/gorpipe/test/memory/MemorySampler.java \
        test/src/test/java/org/gorpipe/test/memory/UTestMemorySampler.java
git commit -m "$(cat <<'EOF'
test(ENGKNOW-3657): add MemorySampler for peak heap/RSS tracking

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 3: DictionaryFixture — build dictionaries at scale (filesystem)

**Files:**
- Create: `test/src/main/java/org/gorpipe/test/memory/DictionaryFixture.java`
- Test: `test/src/test/java/org/gorpipe/test/memory/UTestDictionaryFixture.java`
- Reference (do not modify): `test/src/main/java/org/gorpipe/test/GorDictionarySetup.java`, `model/src/main/java/org/gorpipe/gor/table/dictionary/gor/GorDictionaryTable.java`

**Interfaces:**
- Consumes: `MemoryLoadConfig` (Task 1).
- Produces: `DictionaryFixture` with constructor `DictionaryFixture(Path root)`; method `List<Path> createTables(MemoryLoadConfig config)` returning the `.gord` paths (one per table); method `GorDictionaryTable openTable(Path gordPath)` returning a built `GorDictionaryTable`. The `.gord` paths and `openTable` are consumed by Task 4.

- [ ] **Step 1: Write the failing test**

```java
package org.gorpipe.test.memory;

import org.gorpipe.gor.table.dictionary.gor.GorDictionaryEntry;
import org.gorpipe.gor.table.dictionary.gor.GorDictionaryTable;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UTestDictionaryFixture {

    @Test
    public void createsReadableTablesAtScale() throws Exception {
        Path root = Files.createTempDirectory("memtest-fixture");
        DictionaryFixture fixture = new DictionaryFixture(root);
        MemoryLoadConfig config = new MemoryLoadConfig(50, 10, 10, 3, 4, 70, 5);

        List<Path> tables = fixture.createTables(config);

        assertEquals(3, tables.size());
        for (Path gord : tables) {
            assertTrue("gord file exists", Files.exists(gord));
            GorDictionaryTable table = fixture.openTable(gord);
            List<? extends GorDictionaryEntry> all = table.filter().get();
            assertTrue("dictionary has entries", all.size() > 0);
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :test:test --tests "org.gorpipe.test.memory.UTestDictionaryFixture"`
Expected: FAIL — `DictionaryFixture` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package org.gorpipe.test.memory;

import org.gorpipe.gor.table.dictionary.gor.GorDictionaryTable;
import org.gorpipe.test.GorDictionarySetup;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds Gor dictionaries at prod-like scale for the memory harness.
 * Wraps {@link GorDictionarySetup} to generate data files + a .gord dictionary per table.
 */
public class DictionaryFixture {
    private final Path root;

    public DictionaryFixture(Path root) {
        this.root = root;
    }

    public List<Path> createTables(MemoryLoadConfig config) throws IOException {
        List<Path> tables = new ArrayList<>();
        int[] chrs = {1, 2, 3};
        for (int t = 0; t < config.tableCount; t++) {
            String name = "memtest_table_" + t;
            String[] sources = new String[config.fileCount];
            for (int i = 0; i < config.fileCount; i++) sources[i] = "PN" + i;
            Map<String, List<String>> data = GorDictionarySetup.createDataFilesMap(
                    name, root, config.fileCount, chrs, config.rowsPerChr, "PN", true, sources);

            Path gord = root.resolve(name + ".gord");
            GorDictionaryTable table = new GorDictionaryTable.Builder<>(gord).build();
            table.insert(data);
            table.save();
            tables.add(gord);
        }
        return tables;
    }

    public GorDictionaryTable openTable(Path gordPath) {
        return new GorDictionaryTable.Builder<>(gordPath).build();
    }
}
```

(Prepend copyright header. If `insert(Map)` or `save()` signatures differ at compile time, check `DictionaryTable.java:129,304` and adjust the calls — the public methods `insert(Map<String,List<String>>)` and `save()` exist there.)

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :test:test --tests "org.gorpipe.test.memory.UTestDictionaryFixture"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add test/src/main/java/org/gorpipe/test/memory/DictionaryFixture.java \
        test/src/test/java/org/gorpipe/test/memory/UTestDictionaryFixture.java
git commit -m "$(cat <<'EOF'
test(ENGKNOW-3657): add DictionaryFixture to build dictionaries at scale

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 4: TableServiceLoadDriver — mixed read+write load

**Files:**
- Create: `test/src/main/java/org/gorpipe/test/memory/TableServiceLoadDriver.java`
- Test: `test/src/test/java/org/gorpipe/test/memory/UTestTableServiceLoadDriver.java`

**Interfaces:**
- Consumes: `MemoryLoadConfig` (Task 1), `DictionaryFixture` + `GorDictionaryTable` (Task 3).
- Produces: `TableServiceLoadDriver` with constructor `TableServiceLoadDriver(List<Path> tablePaths, DictionaryFixture fixture, MemoryLoadConfig config)`; method `LoadResult run()` where `LoadResult` is a public static nested class with public final fields `long readOps`, `long writeOps`, `long errors`. Consumed by Task 5.

- [ ] **Step 1: Write the failing test**

```java
package org.gorpipe.test.memory;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class UTestTableServiceLoadDriver {

    @Test
    public void runsMixedLoadAndCountsOps() throws Exception {
        Path root = Files.createTempDirectory("memtest-driver");
        MemoryLoadConfig config = new MemoryLoadConfig(30, 10, 10, 2, 4, 70, 2);
        DictionaryFixture fixture = new DictionaryFixture(root);
        List<Path> tables = fixture.createTables(config);

        TableServiceLoadDriver driver = new TableServiceLoadDriver(tables, fixture, config);
        TableServiceLoadDriver.LoadResult result = driver.run();

        assertTrue("did some reads", result.readOps > 0);
        assertTrue("did some writes", result.writeOps > 0);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :test:test --tests "org.gorpipe.test.memory.UTestTableServiceLoadDriver"`
Expected: FAIL — `TableServiceLoadDriver` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package org.gorpipe.test.memory;

import org.gorpipe.gor.table.dictionary.gor.GorDictionaryTable;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Drives a mixed read+write load against a set of Gor dictionaries using a thread pool.
 * Reads: tag-filtered {@code filter().tags(..).get()}. Writes: insert a new data line + save.
 */
public class TableServiceLoadDriver {
    private final List<Path> tablePaths;
    private final DictionaryFixture fixture;
    private final MemoryLoadConfig config;

    public TableServiceLoadDriver(List<Path> tablePaths, DictionaryFixture fixture, MemoryLoadConfig config) {
        this.tablePaths = tablePaths;
        this.fixture = fixture;
        this.config = config;
    }

    public LoadResult run() throws InterruptedException {
        AtomicLong readOps = new AtomicLong();
        AtomicLong writeOps = new AtomicLong();
        AtomicLong errors = new AtomicLong();
        long deadline = System.nanoTime() + config.durationSeconds * 1_000_000_000L;

        ExecutorService pool = Executors.newFixedThreadPool(config.threads);
        for (int i = 0; i < config.threads; i++) {
            pool.submit(() -> {
                while (System.nanoTime() < deadline) {
                    Path gord = tablePaths.get(ThreadLocalRandom.current().nextInt(tablePaths.size()));
                    try {
                        if (ThreadLocalRandom.current().nextInt(100) < config.readWritePercent) {
                            doRead(gord);
                            readOps.incrementAndGet();
                        } else {
                            doWrite(gord);
                            writeOps.incrementAndGet();
                        }
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    }
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(config.durationSeconds + 30, TimeUnit.SECONDS);
        return new LoadResult(readOps.get(), writeOps.get(), errors.get());
    }

    private void doRead(Path gord) {
        GorDictionaryTable table = fixture.openTable(gord);
        String tag = "PN" + ThreadLocalRandom.current().nextInt(config.fileCount);
        table.filter().tags(tag).get();
    }

    private void doWrite(Path gord) {
        GorDictionaryTable table = fixture.openTable(gord);
        int id = ThreadLocalRandom.current().nextInt(1_000_000);
        // insert(String...) accepts raw dictionary lines: "file\talias".
        table.insert("memtest_extra_" + id + ".gor\tEXTRA" + id);
        table.save();
    }

    public static class LoadResult {
        public final long readOps;
        public final long writeOps;
        public final long errors;
        public LoadResult(long readOps, long writeOps, long errors) {
            this.readOps = readOps; this.writeOps = writeOps; this.errors = errors;
        }
        @Override public String toString() {
            return "reads=" + readOps + " writes=" + writeOps + " errors=" + errors;
        }
    }
}
```

(Prepend copyright header. `insert(String...)` and `save()` are on `DictionaryTable` at lines 187 and 129. If writes to the same table from multiple threads deadlock on the file lock, that is itself a finding — record it and reduce write concurrency in the config rather than changing product code.)

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :test:test --tests "org.gorpipe.test.memory.UTestTableServiceLoadDriver"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add test/src/main/java/org/gorpipe/test/memory/TableServiceLoadDriver.java \
        test/src/test/java/org/gorpipe/test/memory/UTestTableServiceLoadDriver.java
git commit -m "$(cat <<'EOF'
test(ENGKNOW-3657): add TableServiceLoadDriver for mixed read+write load

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 5: TableServiceLoadMain — profiler entrypoint

**Files:**
- Create: `test/src/main/java/org/gorpipe/test/memory/TableServiceLoadMain.java`
- Test: `test/src/test/java/org/gorpipe/test/memory/UTestTableServiceLoadMain.java`

**Interfaces:**
- Consumes: `MemoryLoadConfig.fromSystemProperties()` (Task 1), `MemorySampler` (Task 2), `DictionaryFixture` (Task 3), `TableServiceLoadDriver` (Task 4).
- Produces: `TableServiceLoadMain` with `public static void main(String[] args)` and a testable `public static String runOnce(MemoryLoadConfig config, java.nio.file.Path root)` returning a one-line summary string containing `peakHeapMB=` and the op counts.

- [ ] **Step 1: Write the failing test**

```java
package org.gorpipe.test.memory;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;

public class UTestTableServiceLoadMain {

    @Test
    public void runOnceProducesSummary() throws Exception {
        Path root = Files.createTempDirectory("memtest-main");
        MemoryLoadConfig config = new MemoryLoadConfig(30, 10, 10, 2, 4, 70, 2);
        String summary = TableServiceLoadMain.runOnce(config, root);
        assertTrue("summary reports peak heap", summary.contains("peakHeapMB="));
        assertTrue("summary reports reads", summary.contains("reads="));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :test:test --tests "org.gorpipe.test.memory.UTestTableServiceLoadMain"`
Expected: FAIL — `TableServiceLoadMain` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package org.gorpipe.test.memory;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Entry point for profiler-attached runs of the table-service memory harness.
 * Run with a prod-like heap and profiling flags, e.g.:
 *   java -Xmx2g -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp \
 *        -XX:StartFlightRecording=filename=load.jfr,settings=profile \
 *        -Dgor.memtest.fileCount=20000 -Dgor.memtest.tableCount=16 \
 *        -cp <classpath> org.gorpipe.test.memory.TableServiceLoadMain
 */
public class TableServiceLoadMain {

    public static void main(String[] args) throws Exception {
        MemoryLoadConfig config = MemoryLoadConfig.fromSystemProperties();
        Path root = Files.createTempDirectory("memtest-run");
        System.out.println("Config: " + config);
        System.out.println(runOnce(config, root));
    }

    public static String runOnce(MemoryLoadConfig config, Path root) throws Exception {
        MemorySampler sampler = new MemorySampler(100);
        sampler.start();
        try {
            DictionaryFixture fixture = new DictionaryFixture(root);
            var tables = fixture.createTables(config);
            TableServiceLoadDriver driver = new TableServiceLoadDriver(tables, fixture, config);
            TableServiceLoadDriver.LoadResult result = driver.run();
            System.gc();
            Thread.sleep(200); // let sampler catch post-GC retained heap
            sampler.stop();
            long peakHeapMB = sampler.peakHeapUsedBytes() / (1024 * 1024);
            long peakRssMB = sampler.peakRssBytes() < 0 ? -1 : sampler.peakRssBytes() / (1024 * 1024);
            return "peakHeapMB=" + peakHeapMB + " peakRssMB=" + peakRssMB + " " + result;
        } finally {
            sampler.stop();
        }
    }
}
```

(Prepend copyright header.)

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :test:test --tests "org.gorpipe.test.memory.UTestTableServiceLoadMain"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add test/src/main/java/org/gorpipe/test/memory/TableServiceLoadMain.java \
        test/src/test/java/org/gorpipe/test/memory/UTestTableServiceLoadMain.java
git commit -m "$(cat <<'EOF'
test(ENGKNOW-3657): add TableServiceLoadMain profiler entrypoint

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 6: Scaling validation (SlowTests)

**Files:**
- Create: `test/src/test/java/org/gorpipe/test/memory/UTestTableServiceLoad.java`

**Interfaces:**
- Consumes: everything from Tasks 1-5.
- Produces: nothing (validation test only).

This test proves the harness actually stresses the dictionary path: peak retained heap must grow when the `fileCount` (dictionary size) knob grows. If it doesn't, the harness is not exercising the suspected memory driver and must be fixed before profiling.

- [ ] **Step 1: Write the failing test**

```java
package org.gorpipe.test.memory;

import org.gorpipe.test.SlowTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;

@Category(SlowTests.class)
public class UTestTableServiceLoad {

    private long peakHeapForFileCount(int fileCount) throws Exception {
        Path root = Files.createTempDirectory("memtest-scale-" + fileCount);
        MemoryLoadConfig config = new MemoryLoadConfig(fileCount, 20, 100, 4, 4, 70, 3);
        MemorySampler sampler = new MemorySampler(50);
        sampler.start();
        DictionaryFixture fixture = new DictionaryFixture(root);
        var tables = fixture.createTables(config);
        new TableServiceLoadDriver(tables, fixture, config).run();
        System.gc();
        Thread.sleep(200);
        sampler.stop();
        return sampler.peakHeapUsedBytes();
    }

    @Test
    public void peakHeapScalesWithDictionarySize() throws Exception {
        long small = peakHeapForFileCount(500);
        long large = peakHeapForFileCount(8000);
        assertTrue("peak heap should grow with dictionary size: small=" + small + " large=" + large,
                large > small * 1.5);
    }
}
```

- [ ] **Step 2: Run test to verify it fails, then passes**

Run: `./gradlew :test:slowTest --tests "org.gorpipe.test.memory.UTestTableServiceLoad"`
Expected: PASS if the harness stresses the dictionary path. If it FAILS (heap does not scale), the harness is not exercising the target — investigate the read/write paths in Tasks 3-4 before proceeding. Do not weaken the assertion to force a pass.

- [ ] **Step 3: Commit**

```bash
git add test/src/test/java/org/gorpipe/test/memory/UTestTableServiceLoad.java
git commit -m "$(cat <<'EOF'
test(ENGKNOW-3657): validate harness stresses dictionary memory path

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 7: S3-backed fixture variant

**Files:**
- Modify: `test/src/main/java/org/gorpipe/test/memory/DictionaryFixture.java`
- Test: `test/src/test/java/org/gorpipe/test/memory/UTestDictionaryFixtureS3.java`

**Interfaces:**
- Consumes: `MemoryLoadConfig` (Task 1); the existing filesystem `createTables` (Task 3).
- Produces: on `DictionaryFixture`, a new constructor `DictionaryFixture(Path localRoot, String s3Root)` and method `List<Path> createTablesOnS3(MemoryLoadConfig config)` writing `.gord` + data to an `s3://bucket/prefix` root via the S3 driver, returning the `.gord` `Path`s (as `s3://...` URIs resolved through the gor `FileReader`). This variant exercises the S3 multipart write buffers (`gor.s3.write.chunksize`).

This task confirms the 64 MB multipart-buffer hypothesis. It is gated on a dev bucket: the test skips (via `org.junit.Assume`) if the env var `GOR_MEMTEST_S3_ROOT` is unset, so CI without S3 credentials stays green.

- [ ] **Step 1: Write the failing test**

```java
package org.gorpipe.test.memory;

import org.junit.Assume;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class UTestDictionaryFixtureS3 {

    @Test
    public void createsTablesOnS3WhenBucketConfigured() throws Exception {
        String s3Root = System.getenv("GOR_MEMTEST_S3_ROOT"); // e.g. s3://my-dev-bucket/memtest
        Assume.assumeTrue("GOR_MEMTEST_S3_ROOT not set; skipping S3 fixture test", s3Root != null);

        Path localRoot = Files.createTempDirectory("memtest-s3-local");
        MemoryLoadConfig config = new MemoryLoadConfig(20, 10, 10, 1, 2, 50, 2);
        DictionaryFixture fixture = new DictionaryFixture(localRoot, s3Root);

        List<Path> tables = fixture.createTablesOnS3(config);

        assertTrue("created at least one S3 table", tables.size() >= 1);
    }
}
```

- [ ] **Step 2: Run test to verify it is skipped locally**

Run: `./gradlew :test:test --tests "org.gorpipe.test.memory.UTestDictionaryFixtureS3"`
Expected: PASS as **skipped/ignored** (Assume fails when env unset). This confirms the gate works without credentials.

- [ ] **Step 3: Write minimal implementation**

Add to `DictionaryFixture` (keep the existing filesystem members and methods):

```java
    private final String s3Root; // null for filesystem-only fixtures

    public DictionaryFixture(Path root, String s3Root) {
        this.root = root;
        this.s3Root = s3Root;
    }

    /**
     * Builds dictionaries whose .gord and bucket files live under an s3:// root,
     * exercising the S3 driver write path (multipart buffers). Data files are
     * generated locally then written to S3 via the gor table's FileReader.
     */
    public List<Path> createTablesOnS3(MemoryLoadConfig config) throws IOException {
        if (s3Root == null) throw new IllegalStateException("s3Root not configured");
        List<Path> tables = new ArrayList<>();
        int[] chrs = {1, 2, 3};
        for (int t = 0; t < config.tableCount; t++) {
            String name = "memtest_s3_table_" + t;
            String[] sources = new String[config.fileCount];
            for (int i = 0; i < config.fileCount; i++) sources[i] = "PN" + i;
            Map<String, List<String>> data = GorDictionarySetup.createDataFilesMap(
                    name, root, config.fileCount, chrs, config.rowsPerChr, "PN", true, sources);

            String gordUri = s3Root.endsWith("/") ? s3Root + name + ".gord" : s3Root + "/" + name + ".gord";
            GorDictionaryTable table = new GorDictionaryTable.Builder<>(gordUri).build();
            table.insert(data);
            table.save();
            tables.add(Path.of(gordUri));
        }
        return tables;
    }
```

(The existing single-arg constructor from Task 3 sets `s3Root = null`; update it to `this(root, null)` or add the field assignment. `GorDictionaryTable.Builder<>(String)` accepts a URI string per `GorDictionaryTable.java:64`.)

- [ ] **Step 4: Run against a real dev bucket (manual, optional in CI)**

Run:
```bash
GOR_MEMTEST_S3_ROOT=s3://<dev-bucket>/memtest \
  ./gradlew :test:test --tests "org.gorpipe.test.memory.UTestDictionaryFixtureS3"
```
Expected: PASS (not skipped) — `.gord` objects appear under the S3 prefix. Requires AWS/OCI credentials in the environment.

- [ ] **Step 5: Commit**

```bash
git add test/src/main/java/org/gorpipe/test/memory/DictionaryFixture.java \
        test/src/test/java/org/gorpipe/test/memory/UTestDictionaryFixtureS3.java
git commit -m "$(cat <<'EOF'
test(ENGKNOW-3657): add S3-backed dictionary fixture variant

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 8: Profiling runbook + findings doc

**Files:**
- Create: `docs/superpowers/runbooks/2026-07-08-engknow-3657-profiling-runbook.md`
- Create: `docs/superpowers/findings/2026-07-08-engknow-3657-memory-findings.md`

**Interfaces:**
- Consumes: `TableServiceLoadMain` (Task 5).
- Produces: documentation only.

- [ ] **Step 1: Write the runbook**

Create `docs/superpowers/runbooks/2026-07-08-engknow-3657-profiling-runbook.md` with this content:

````markdown
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
````

- [ ] **Step 2: Write the findings template**

Create `docs/superpowers/findings/2026-07-08-engknow-3657-memory-findings.md`:

````markdown
# ENGKNOW-3657 Memory Findings

> Fill in after running the profiling runbook. This is the deliverable of the diagnosis task.

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
````

- [ ] **Step 3: Commit**

```bash
git add docs/superpowers/runbooks/2026-07-08-engknow-3657-profiling-runbook.md \
        docs/superpowers/findings/2026-07-08-engknow-3657-memory-findings.md
git commit -m "$(cat <<'EOF'
docs(ENGKNOW-3657): add profiling runbook and findings template

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Post-plan: run the diagnosis

After Task 8, execute the runbook, fill the findings doc, and open the follow-up fix ticket. The fix itself is out of scope for this plan.
