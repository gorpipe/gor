# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## About GORpipe

GORpipe is a genomic analysis tool based on a Genomic Ordered Relational (GOR) architecture. It uses a declarative query language combining ideas from SQL and Unix shell pipe syntax to analyze large sets of genomic and phenotypic tabular data in a parallel execution engine.

## Build Commands

```bash
# Build local installation
./gradlew installDist
# or:
make build

# Run GOR after building
./gortools/build/install/gor-scripts/bin/gorpipe "gor ..."

# Clean
./gradlew clean

# Compile with all warnings (useful for catching issues)
make compile-all-with-warnings
```

## Testing

```bash
# Run standard unit tests
./gradlew test

# Run slow tests
./gradlew slowTest

# Run integration tests
./gradlew integrationTest

# Run all tests
make all-test

# Run a single test class
./gradlew test --tests "org.gorpipe.gor.TestClassName"

# Run a single test method
./gradlew test --tests "org.gorpipe.gor.TestClassName.testMethodName"

# Run tests in a specific module
./gradlew :gortools:test --tests "gorsat.Script.UTestSignature"

# Run ScalaTest tests in gortools (not auto-discovered by Gradle)
./gradlew :gortools:testScala
```

Tests are categorized with JUnit `@Category` annotations:
- `SlowTests` — run with `slowTest` task
- `IntegrationTests` — run with `integrationTest` task
- `DbTests` — run with `dbTest` task

Test data lives in `tests/data/` and is loaded as a git submodule (from `gor-test-data` repo). Initialize it with:
```bash
git submodule update --init --recursive
```

## Module Architecture

This is a multi-module Gradle project with a layered dependency structure:

```
auth
  ↓
base → util
  ↓      ↓
  └→ model (Scala+Java, genomic data structures)
       ↓
    drivers (S3, GCS, Azure, OCI storage drivers)
       ↓
    gortools (main query engine — ANTLR4 grammar, gorsat package)
       ↓
   gorscripts (CLI and command-line tools)
```

The `test` module provides shared test infrastructure and depends on all main modules. The `external` module contains vendored/third-party code.

**Key modules:**
- **`model`** — Core genomic data abstractions; mixed Java/Scala, uses Parquet, SQLite, PostgreSQL, Caffeine caching. Defines the `Row`, `GenomicIterator`, `Analysis`, `CommandInfo`, and `SourceProvider` interfaces.
- **`gortools`** — Query engine entry point; contains the ANTLR4 grammar in `src/main/antlr`, and the `gorsat` package with all GOR commands/functions written primarily in Scala. Command/macro registries live here.
- **`drivers`** — Pluggable storage drivers (auto-discovered via `@AutoService`); each cloud provider (S3, GCS, Azure, OCI) is a separate driver.
- **`gorscripts`** — CLI entry points using picocli; main class is `GorCLI` (`gorscripts/src/main/java/org/gorpipe/gor/cli/GorCLI.java`).

## Technology Stack

- **Java 17**, **Scala 2.13** — mixed codebase; most query engine logic is Scala, infrastructure is Java
- **Gradle** with Groovy DSL plugins in `buildSrc/`
- **ANTLR4** — query language grammar in `gortools/src/main/antlr/`
- **JUnit 4** with ScalaTest/ScalaCheck for Scala modules
- Build configuration shared via `buildSrc/src/main/groovy/`:
  - `gor.java-common.gradle` — common Java/Scala settings applied to all modules
  - `gor.java-library.gradle` — publishing configuration for library modules
  - `gor.scala-common.gradle` — Scala 2.13 compilation config
  - `gor.java-application.gradle` — CLI/application distribution config
- ANTLR generates sources into `gortools/build/generated-src/antlr/main` (visitor pattern enabled)

## Query Execution Pipeline

Understanding how a GOR query executes end-to-end:

1. **Parsing** — `GorScript.g4` (ANTLR4) defines the grammar. Scripts go through alias expansion → include injection → macro preprocessing in `ScriptExecutionEngine.scala`.
2. **Command lookup** — All pipe commands are registered in `GorPipeCommands.scala` via `commandMap`. Each entry is a `CommandInfo` instance.
3. **Analysis chain** — Each pipe step produces an `Analysis` (Scala abstract class in `model`). Analysis instances are chained via `pipeTo`, forming a processing pipeline. Key methods: `setRowHeader()` (called once with incoming schema), `process(r: Row)` (called per row), `finish()` (cleanup).
4. **Row iteration** — Source data is read via `GenomicIterator` (implements `Iterator<Row>`), which supports `seek(chr, pos)` for genomic range queries.
5. **Output** — A `GorRunner` (created by `GorExecutionEngine`) drives the iterator and collects results.

Key files:
- `gortools/src/main/scala/gorsat/process/GorPipeCommands.scala` — command registry
- `gortools/src/main/scala/gorsat/process/GorPipeMacros.scala` — macro registry (PGOR, PARTGOR, etc.)
- `gortools/src/main/scala/gorsat/Script/ScriptExecutionEngine.scala` — script preprocessing
- `model/src/main/scala/gorsat/Commands/Analysis.scala` — base analysis class
- `model/src/main/java/org/gorpipe/gor/model/Row.java` — row interface
- `model/src/main/java/org/gorpipe/gor/model/GenomicIterator.java` — iterator interface

## Extension Points

**Adding a new GOR pipe command:**
1. Create a Scala class in `gortools/src/main/scala/gorsat/Commands/` extending `CommandInfo`
2. Implement `processArguments()` — parse args and return `CommandParsingResult` containing an `Analysis` instance
3. Create a corresponding `Analysis` subclass in `gortools/src/main/scala/gorsat/Analysis/` implementing `process()`, `setRowHeader()`, and `finish()`
4. Register in `GorPipeCommands.register()` in `GorPipeCommands.scala`

**Adding a new storage driver:**
1. Create a class in `drivers/src/main/java/org/gorpipe/<provider>/` implementing `SourceProvider`
2. Annotate with `@AutoService(SourceProvider.class)` — drivers are auto-discovered at runtime
3. Add the provider entry under `META-INF/services/`

**Adding a new macro:**
1. Create in `gortools/src/main/scala/gorsat/Macros/` extending `MacroInfo`
2. Register in `GorPipeMacros.register()`

## Test Patterns

Tests that exercise the query engine need to initialize the registries:
```java
GorPipeCommands.register();
GorInputSources.register();
```
Use `TestUtils.runGorPipe("gor ...")` for integration-style query tests.

## Local Publishing

To test changes in a dependent project:

```bash
# Publish to Maven Local (~/.m2)
make publish-local
# Then in dependent project: ./gradlew ... -PuseMavenLocal
```

## Versioning

- Version stored in `VERSION` file at repo root
- Semantic versioning: `<major>.<minor>.<patch>`
- Development versions use `-SNAPSHOT` suffix
- Releases: `make release-milestone-from-master MILESTONE=X.Y.Z`
- Dependency versions managed in `versions.properties` (refreshVersions plugin); update with `./gradlew refreshVersions`
