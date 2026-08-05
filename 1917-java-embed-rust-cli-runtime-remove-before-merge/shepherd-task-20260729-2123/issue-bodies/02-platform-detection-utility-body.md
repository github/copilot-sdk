## Overview

Create the `PlatformDetector` utility class that determines the current platform's `os`, `arch`, `libc` and produces the classifier string used to locate the correct `runtime.node` native binary.

**This is task 4.2 of 9 in the implementation plan.** Tasks are assigned, completed, and merged serially in this listed order. Task 4.1 (Parent POM restructure) is complete on the base branch before this task begins.

**Branch:** `edburns/1917-java-embed-rust-cli-runtime-dd-3039145` on `upstream`

## Plan and supporting resources

On the `edburns/1917-java-embed-rust-cli-runtime-dd-3039145` branch, the directory `1917-java-embed-rust-cli-runtime-remove-before-merge` contains the plan (`1917-embed-cli-runtime-ignorance-reduction-plan.md`) and supporting resources (spikes, prototypes, diagrams).

**Read the entire plan before working.**

## Relevant plan sections to carefully re-read

- **Section 3.6 — Platform detection implementation** — Resolution: Pure-Java detector shape with `detectOs()`, `detectArch()`, `detectLinuxLibc()`, `detectClassifier()`. ELF PT_INTERP parsing for musl vs. glibc on Linux. Read the three spikes:
  - `1917-java-embed-rust-cli-runtime-remove-before-merge/spike-3-6-platform-detection-darwin-arm64/`
  - `1917-java-embed-rust-cli-runtime-remove-before-merge/spike-3-6-platform-detection-linux-x64/`
  - `1917-java-embed-rust-cli-runtime-remove-before-merge/spike-3-6-platform-detection-win32-x64/`
- **Section 3.3 — JNA binding interface design** — Resolution: Package `com.github.copilot.ffi` for all FFI-related classes.
- **Section 4.2 — Platform detection utility** (the primary task description)
- **TDD discipline for all implementation steps** — write tests first, implement until green, refactor, gate before proceeding.

## Resolved decisions that constrain this task

- **Package:** `com.github.copilot.ffi` (Section 3.3 Resolution).
- **Classifier format:** `<os>-<arch>` for non-Linux; `linux-<arch>` for glibc Linux; `linuxmusl-<arch>` for musl Linux. The 8 ADR-007 classifiers are: `linux-x64`, `linux-arm64`, `linuxmusl-x64`, `linuxmusl-arm64`, `darwin-x64`, `darwin-arm64`, `win32-x64`, `win32-arm64`.
- **OS mapping:** `os.name` → `darwin | linux | win32`.
- **Arch mapping:** `os.arch` aliases (`amd64`/`x86_64`/`x64` → `x64`; `aarch64`/`arm64` → `arm64`).
- **Linux libc detection:** Read `/proc/self/exe`, parse ELF PT_INTERP from the first 2 KB:
  - Contains `/ld-musl-` → `MUSL`
  - Contains `/ld-linux-` → `GLIBC`
  - Parse/read failure → `UNKNOWN` (treated as glibc for classifier purposes)
- **Allow-list:** Include an allow-list for the 8 supported classifiers so unsupported tuples fail fast.
- **Scope limit:** This phase's gating criteria only require correct output on `linux-x64` (Ubuntu 24.04). Multi-platform and musl-specific gating are deferred.

## Deliverables

### Files to create

1. **`java/sdk/src/main/java/com/github/copilot/ffi/PlatformDetector.java`** — Standalone utility class. Methods:
   - `detectOs()` — maps `System.getProperty("os.name")` to `darwin | linux | win32`
   - `detectArch()` — maps `System.getProperty("os.arch")` aliases to `x64 | arm64`
   - `detectLinuxLibc()` — reads `/proc/self/exe`, parses ELF PT_INTERP, classifies as `GLIBC | MUSL | UNKNOWN`
   - `detectClassifier()` — combines os/arch/libc into the classifier string
   - ELF parsing logic: private, pure Java, no subprocesses, no external dependencies
   - Classifier derivation should be table-driven

2. **`java/sdk/src/test/java/com/github/copilot/ffi/PlatformDetectorTest.java`** — Unit tests with:
   - Mocked system properties for each OS/arch combination
   - Test ELF binary fragments for PT_INTERP parsing (glibc and musl linker paths)
   - Verification of all 8 valid classifiers in the allow-list
   - Verification that unsupported tuples fail fast
   - At least one test exercising the success path and one test exercising the failure/edge-case path for each public method

## Gating tests and criteria

1. **Correct classifier on Ubuntu linux-x64:** `PlatformDetector.detectClassifier()` returns `"linux-x64"` on `ubuntu-latest`.
2. **Unit tests pass:** All tests in `PlatformDetectorTest` pass.
3. **All prior tests pass:** `mvn verify` from `java/` passes (reactor build including task 4.1's structure).
4. **Spotless compliance:** `mvn spotless:check` passes.

## Out of scope

- Runtime-specific testing on macOS, Windows, ARM, or musl platforms (deferred to later phase).
- Native binary extraction or loading (task 4.3).
- JNA dependencies or bindings (task 4.4).
