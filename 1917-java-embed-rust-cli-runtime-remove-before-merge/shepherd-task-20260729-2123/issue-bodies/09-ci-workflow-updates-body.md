## Overview

Modify `.github/workflows/java-sdk-tests.yml` to add a new `java-sdk-inprocess` CI job that runs the full Java E2E test suite under the InProcess FFI transport on Ubuntu `linux-x64`.

**This is task 4.9 of 9 in the implementation plan.** Tasks are assigned, completed, and merged serially in this listed order. Tasks 4.1–4.8 are complete on the base branch before this task begins.

**Branch:** `edburns/1917-java-embed-rust-cli-runtime-dd-3039145` on `upstream`

## Plan and supporting resources

On the `edburns/1917-java-embed-rust-cli-runtime-dd-3039145` branch, the directory `1917-java-embed-rust-cli-runtime-remove-before-merge` contains the plan (`1917-embed-cli-runtime-ignorance-reduction-plan.md`) and supporting resources (spikes, prototypes, diagrams).

**Read the entire plan before working.**

## Relevant plan sections to carefully re-read

- **Section 3.11 — E2E testing with InProcess transport** — Resolution: Run full E2E suite under both transports:
  - **CI job A** (existing): subprocess transport — existing `java-sdk-tests.yml` job, NO changes.
  - **CI job B** (new): InProcess transport — same test suite, new Maven profile (`-Pinprocess`) with `COPILOT_SDK_DEFAULT_CONNECTION=inprocess`, `forkCount=1`, `parallel=none`.
  CI job B requires `runtime.node` on classpath (from `copilot-native` module). The matrix runs both jobs for confidence that both transports produce identical behavior.
- **Section 3.12 — CI/CD workflow changes** — Resolution:
  - Modify existing `java-sdk-tests.yml` — add a new `java-sdk-inprocess` job (separate job, NOT a matrix entry, matching .NET pattern).
  - Existing `java-sdk` job is completely unchanged.
  - InProcess CI job scope: `ubuntu-latest` (linux-x64) only.
  - Native binaries provisioned via `copilot-native` module's `generate-resources` phase (`npm pack`).
  - No explicit availability check — the `-Pinprocess` profile is the gating mechanism.
- **Section 4.9 — CI workflow updates** (the primary task description).
- **Hard scope invariant:** No CI work for platforms other than `linux-x64` in this phase.

## Resolved decisions that constrain this task

- **Job name:** `java-sdk-inprocess` (a separate job, not a matrix entry).
- **Runner:** `ubuntu-latest` only.
- **Profile:** `-Pinprocess` Maven profile (created in task 4.8).
- **Prerequisites:** The `copilot-native` module must be built first (it downloads `runtime.node` for `linux-x64` via `npm pack`). The InProcess CI job must either:
  - Build the full reactor (`mvn clean verify -Pinprocess` from `java/`), which includes `copilot-native`, OR
  - Have a prerequisite step that builds `copilot-native` to produce the classifier JAR before running tests.
- **Existing job unchanged:** The existing `java-sdk` job must not be modified.
- **Node.js required:** The InProcess CI job needs Node.js (for `npm pack` in the `copilot-native` module AND for the replay proxy in E2E tests).

## Deliverables

### Files to modify

1. **`.github/workflows/java-sdk-tests.yml`** — Add the `java-sdk-inprocess` job:
   - Runs on `ubuntu-latest`
   - Sets up JDK (same version as existing job)
   - Sets up Node.js (same version as existing job, needed for `npm pack` and replay proxy)
   - Runs `mvn clean verify -Pinprocess` from the `java/` directory (full reactor build including `copilot-native`)
   - Ensure env var `COPILOT_SDK_DEFAULT_CONNECTION=inprocess` is set (handled by profile, but verify)
   - Same caching, artifact upload, and failure reporting patterns as the existing job

## Gating tests and criteria

1. **InProcess CI job defined:** The workflow YAML contains a `java-sdk-inprocess` job that runs on `ubuntu-latest`.
2. **Profile activation:** The job invokes Maven with `-Pinprocess`.
3. **Node.js available:** The job sets up Node.js for `npm pack` and replay proxy.
4. **Existing job unchanged:** The existing `java-sdk` (subprocess) job is identical to before.
5. **YAML validity:** The workflow YAML is syntactically valid.
6. **All prior tests pass:** `mvn verify` from `java/` passes locally.
7. **Spotless compliance:** `mvn spotless:check` passes.

## Out of scope

- Adding InProcess CI jobs for macOS, Windows, ARM, or any platform other than `linux-x64`.
- Publishing workflows or Maven Central deployment changes.
- Changes to any Java source code — this task is CI workflow only.
