# ENGKNOW-3577: create with `write -link` cache name fix — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make a `create ... | write -link X` store the actual written data-file name in the create cache so the create can be referenced afterwards.

**Architecture:** The data-file name for a `-link` write is derived by the non-idempotent `LinkFileUtil.inferDataFileNameFromLinkFile` (random suffix). For a plain create it is derived twice — once at plan time (`BaseScriptExecutionEngine.resolveCache`, cached) and again at exec time (`ForkWrite`, actual write) — producing two different names. Fix: in `resolveCache`, inject the resolved name into the executed `write` command so `ForkWrite` reuses it instead of re-deriving. One derivation, cache and disk agree.

**Tech Stack:** Java 17 + Scala 2.13, Gradle multi-module, JUnit 4, `TestUtils.runGorPipe`.

**Spec:** `docs/superpowers/specs/2026-06-18-ENGKNOW-3577-create-write-link-cache-design.md`

---

### Task 1: Regression test + fix for create/write-link cache name

**Files:**
- Test: `gortools/src/test/java/gorsat/UTestGorWrite.java` (add one `@Test` method)
- Modify: `gortools/src/main/java/gorsat/Script/BaseScriptExecutionEngine.java:67-83` (`resolveCache`)

Background facts (already verified, do not re-investigate):
- `UTestGorWrite` already imports `LinkFile`, `FileSource`, `Files`, `Path`, `GorDriverConfig`, `Assert`, and has `@Rule TemporaryFolder workDir` exposed via `workDirPath`, plus `@Rule EnvironmentVariables environmentVariables`. The `@Before setupTest()` sets `workDirPath`.
- `BaseScriptExecutionEngine.java` already imports `CommandParseUtilities` (used at line 325) and `Strings`, `DataType`, `PathUtils`, `Write`, `Tuple`, `Optional`.
- `resolveCache` already computes positional args via `write.validateArguments(args)` (passed into `parseBaseOptions`).

---

- [ ] **Step 1: Write the failing regression test**

Add this method to `gortools/src/test/java/gorsat/UTestGorWrite.java` (place it right after `testWriteLinkFileWithInferFileName`, ends at line 260):

```java
    @Test
    public void testCreateWriteLinkCacheName() throws IOException {
        // ENGKNOW-3577: a create whose query ends in `write -link` must store the actual
        // written data file name in the cache, so referencing the create afterwards works.
        environmentVariables.set(GorDriverConfig.GOR_DRIVER_LINK_MANAGED_DATA_ROOT_URL,
                workDirPath.resolve("managed_data").toString());

        String query = "create #test# = gorrow chr1,1,100 | write -link test.gor; nor [#test#]";
        String result = TestUtils.runGorPipe(query, "-gorroot", workDirPath.toString());

        // The reference resolves and the original row survives the create -> link -> read round trip.
        Assert.assertTrue("unexpected result: " + result, result.contains("chr1\t1\t100"));

        // The link points at a data file that actually exists on disk.
        var linkFile = LinkFile.load(new FileSource(workDirPath.resolve("test.gor.link").toString()));
        Assert.assertEquals(1, linkFile.getEntriesCount());
        Assert.assertTrue(Files.exists(Path.of(linkFile.getLatestEntry().url())));
    }
```

- [ ] **Step 2: Run the test to verify it FAILS**

Run:
```bash
./gradlew :gortools:test --tests "gorsat.UTestGorWrite.testCreateWriteLinkCacheName"
```
Expected: FAIL. `nor [#test#]` resolves to the plan-time derived data-file name (random suffix A), which differs from the exec-time written name (random suffix B), so the file does not exist — the run throws (e.g. a `GorResourceException`/file-not-found) and the test fails before reaching the assertions.

- [ ] **Step 3: Implement the fix in `resolveCache`**

In `gortools/src/main/java/gorsat/Script/BaseScriptExecutionEngine.java`, replace the current `resolveCache` body (lines 67-83):

```java
    private Optional<Tuple<String,Boolean>> resolveCache(GorContext context, String lastCommand, ExecutionBlock queryBlock) {
        var write = new Write();
        var args = lastCommand.substring("write ".length()).split(" ");
        var options = write.parseBaseOptions(context, write.validateArguments(args), args, false);
        var outFile = options._1();
        if (Strings.isNullOrEmpty(outFile)) {
            if (queryBlock.signature() != null) {
                var writeFilePath = context.getSession().getProjectContext().getFileCache().tempLocation(queryBlock.signature(), DataType.GORD.suffix);
                writeFilePath = PathUtils.relativize(context.getSession().getProjectContext().getProjectRoot(), writeFilePath);
                queryBlock.query_$eq(queryBlock.query() + " " + writeFilePath);
                outFile = writeFilePath;
            } else {
                outFile = null;
            }
        }
        return !Strings.isNullOrEmpty(outFile) ? resolveForkPathParent(outFile) : Optional.empty();
    }
```

with:

```java
    private Optional<Tuple<String,Boolean>> resolveCache(GorContext context, String lastCommand, ExecutionBlock queryBlock) {
        var write = new Write();
        var args = lastCommand.substring("write ".length()).split(" ");
        var iargs = write.validateArguments(args);
        var options = write.parseBaseOptions(context, iargs, args, false);
        var outFile = options._1();
        if (Strings.isNullOrEmpty(outFile)) {
            if (queryBlock.signature() != null) {
                var writeFilePath = context.getSession().getProjectContext().getFileCache().tempLocation(queryBlock.signature(), DataType.GORD.suffix);
                writeFilePath = PathUtils.relativize(context.getSession().getProjectContext().getProjectRoot(), writeFilePath);
                queryBlock.query_$eq(queryBlock.query() + " " + writeFilePath);
                outFile = writeFilePath;
            } else {
                outFile = null;
            }
        } else if (iargs.length == 0 && CommandParseUtilities.hasOption(args, "-link")) {
            // ENGKNOW-3577: with `write -link X` (no explicit data file), the data file name is
            // inferred from the link and is non-idempotent (random suffix). Inject the resolved
            // name into the executed write so ForkWrite reuses it instead of re-deriving a
            // different name, keeping the create cache entry and the written file in sync.
            queryBlock.query_$eq(queryBlock.query() + " " + outFile);
        }
        return !Strings.isNullOrEmpty(outFile) ? resolveForkPathParent(outFile) : Optional.empty();
    }
```

Changes: capture `iargs` once and reuse it in `parseBaseOptions`; add the `else if` branch that injects the resolved `outFile` into the query for the link-inferred case.

- [ ] **Step 4: Run the regression test to verify it PASSES**

Run:
```bash
./gradlew :gortools:test --tests "gorsat.UTestGorWrite.testCreateWriteLinkCacheName"
```
Expected: PASS.

- [ ] **Step 5: Run the full write test class to check for regressions**

Run:
```bash
./gradlew :gortools:test --tests "gorsat.UTestGorWrite"
```
Expected: PASS (all existing `write -link` / infer / fork-write / gord-folder tests still green — the new branch only triggers when there is no positional file argument AND `-link` is present).

- [ ] **Step 6: Commit**

```bash
git add gortools/src/main/java/gorsat/Script/BaseScriptExecutionEngine.java gortools/src/test/java/gorsat/UTestGorWrite.java
git commit -m "fix(ENGKNOW-3577): store actual link data name in create cache

A create ending in `write -link X` derived the data file name twice
(plan time in resolveCache, exec time in ForkWrite); the name is
non-idempotent (random suffix), so the cached name did not match the
written file and referencing the create failed. resolveCache now injects
the resolved name into the executed write so ForkWrite reuses it.

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Self-review notes

- **Spec coverage:** root cause (resolveCache double-derive) → Step 3 fix; regression test → Steps 1-2; no-LinkFileUtil-change preserved (only `BaseScriptExecutionEngine` + test touched); follow-up (`MacroUtilities.getCachePath`) intentionally not in this plan.
- **Detection criterion** matches `Write.scala:52` (`fileName.isEmpty && linkOpt.nonEmpty`) — here `iargs.length == 0` ⇒ empty `fileName` (parseBaseOptions builds `fileName` from `iargs.mkString(" ")`), and `hasOption(args, "-link")`.
- **Idempotency:** after injection the executed `write` carries a positional filename, so any re-parse skips inference (no second random name).
- **No placeholders.** All code shown in full.
```
