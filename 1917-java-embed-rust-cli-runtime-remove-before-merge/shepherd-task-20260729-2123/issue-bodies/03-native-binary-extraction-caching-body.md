## Overview

Create the `NativeRuntimeLoader` class that locates the `runtime.node` native binary on the classpath, extracts it to a versioned cache directory, and returns the filesystem path for JNA to load.

**This is task 4.3 of 9 in the implementation plan.** Tasks are assigned, completed, and merged serially in this listed order. Tasks 4.1 (Parent POM restructure) and 4.2 (Platform detection utility) are complete on the base branch before this task begins.

**Branch:** `edburns/1917-java-embed-rust-cli-runtime-dd-3039145` on `upstream`

## Plan and supporting resources

On the `edburns/1917-java-embed-rust-cli-runtime-dd-3039145` branch, the directory `1917-java-embed-rust-cli-runtime-remove-before-merge` contains the plan (`1917-embed-cli-runtime-ignorance-reduction-plan.md`) and supporting resources (spikes, prototypes, diagrams).

**Read the entire plan before working.**

## Relevant plan sections to carefully re-read

- **Section 3.7 — Native binary extraction and caching** — Resolution: Extract classpath resource `native/<classifier>/runtime.node` to `~/.copilot/runtime-cache/<version>/<classifier>/runtime.node`. Key decisions:
  - Version source: primary artifact version from top-level POM, written by Maven resource filtering to a `.properties` resource.
  - Atomicity: unique sibling temp file + `Files.move(temp, cached, ATOMIC_MOVE)`. No file locks.
  - Cache invalidation: version key + cheap regular/non-empty file check. No startup hash.
  - Permissions: do NOT set executable bit on `runtime.node`.
  - Cleanup: none — old versions retained.
- **Section 3.13 — Classpath-first or path-first native resolution** — Resolution: Resolution order is `COPILOT_CLI_PATH` (explicit) → classpath resource (classifier JAR) → alongside bundled CLI.
- **Section 3.6 — Platform detection implementation** — uses `PlatformDetector.detectClassifier()` (task 4.2).
- **Section 4.3 — Native binary extraction and caching** (the primary task description).
- **TDD discipline for all implementation steps** — write tests first, implement until green, refactor, gate before proceeding. Tests must be runnable without a real `runtime.node` binary.

## Resolved decisions that constrain this task

- **Cache path:** `~/.copilot/runtime-cache/<version>/<classifier>/runtime.node`
- **Version source:** Maven resource filtering writes `${project.version}` into a `.properties` resource in the SDK artifact. `NativeRuntimeLoader` reads that resource. Do NOT use `Package.getImplementationVersion()`. A missing or blank version resource is an error → clear exception.
- **Extraction atomicity:** (1) Check for existing cache entry (regular, non-empty file → return it). (2) Create cache directory. (3) Create unique temp file in same directory with `CREATE_NEW`. (4) Copy classpath resource to temp file; reject empty result; flush; `FileChannel.force(true)`. (5) `Files.move(temp, cached, ATOMIC_MOVE)`. If target already exists (another process won), accept the winner after regular/non-empty check. If filesystem doesn't support atomic moves, fail with clear error. (6) Delete caller's temp file in `finally` block.
- **No file locking** — explicitly rejected in plan.
- **No startup hash** — cheap regular/non-empty sanity check only.
- **No execute permission** — do NOT call `setExecutable(true)` on `runtime.node`. JNA's `dlopen` does not require execute permission.
- **Resolution order:** `COPILOT_CLI_PATH` env var → classpath resource → alongside bundled CLI.
- **Uber-jar readiness:** When multiple platform JARs are on the classpath, `NativeRuntimeLoader` must filter by the detected classifier, not grab the first `runtime.node` found. The uber-jar approach is deferred but the loader must be ready for it.
- **Package:** `com.github.copilot.ffi`

## Deliverables

### Files to create

1. **`java/sdk/src/main/java/com/github/copilot/ffi/NativeRuntimeLoader.java`** — Locates, extracts, and caches the native binary. Key methods:
   - `resolve()` — returns the filesystem path to the `runtime.node` binary, following the resolution order above.
   - Private extraction logic following the atomic publish sequence.
   - Uses `PlatformDetector.detectClassifier()` from task 4.2.
   - Reads version from a filtered `.properties` resource.

2. **`java/sdk/src/main/resources/copilot-runtime.properties`** (or similar) — Contains `version=${project.version}`, processed by Maven resource filtering. The exact resource path/name is up to you but must be consistent with what `NativeRuntimeLoader` reads.

3. **`java/sdk/src/test/java/com/github/copilot/ffi/NativeRuntimeLoaderTest.java`** — Unit tests including:
   - Extraction from classpath resource to cache directory.
   - Cache hit (already extracted, regular non-empty file → no re-extraction).
   - Concurrent extraction safety (two threads extracting simultaneously).
   - Atomic rename behavior.
   - Version properties resource reading.
   - `COPILOT_CLI_PATH` override takes priority.
   - Missing classpath resource → clear exception.
   - Missing version properties → clear exception.
   - All tests use temp directories and test classpath resources — no real `runtime.node` needed.

## Gating tests and criteria

1. **Unit tests pass:** All tests in `NativeRuntimeLoaderTest` pass.
2. **Extraction correctness:** Binary extracted to `~/.copilot/runtime-cache/<version>/<classifier>/runtime.node` (verified by tests using temp directories).
3. **Concurrent safety:** Two threads extracting simultaneously both succeed without corruption.
4. **All prior tests pass:** `mvn verify` from `java/` passes (reactor build including prior tasks).
5. **Spotless compliance:** `mvn spotless:check` passes.

## Out of scope

- JNA binding or loading the native library into memory (task 4.4).
- Downloading native binaries from npm (task 4.7 — the `copilot-native` module).
- Testing with a real `runtime.node` binary (task 4.8 — E2E tests).
