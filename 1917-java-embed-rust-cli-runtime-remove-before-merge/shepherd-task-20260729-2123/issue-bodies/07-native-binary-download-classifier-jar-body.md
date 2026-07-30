## Overview

Create the `copilot-native/` Maven module (`com.github:copilot-sdk-java-runtime`) that downloads the `runtime.node` binary for `linux-x64` only via `npm pack` and packages it as a `linux-x64` classifier JAR with SHA-512 integrity verification.

**This is task 4.7 of 9 in the implementation plan.** Tasks are assigned, completed, and merged serially in this listed order. Tasks 4.1–4.6 are complete on the base branch before this task begins.

**Branch:** `edburns/1917-java-embed-rust-cli-runtime-dd-3039145` on `upstream`

## Plan and supporting resources

On the `edburns/1917-java-embed-rust-cli-runtime-dd-3039145` branch, the directory `1917-java-embed-rust-cli-runtime-remove-before-merge` contains the plan (`1917-embed-cli-runtime-ignorance-reduction-plan.md`) and supporting resources (spikes, prototypes, diagrams).

**Read the entire plan before working.**

## Relevant plan sections to carefully re-read

- **Section 3.1 — Maven module structure** — Resolution: Option B — multi-module reactor. `copilot-native` module uses multiple `maven-jar-plugin` executions per platform. Plus a placeholder primary JAR. The `copilot-native-all` uber-JAR module is deferred.
- **Section 3.2 — How do native binaries enter the build** — Resolution: Option C variant — `npm pack` per-platform via `exec-maven-plugin`. SHA-512 integrity verification against `nodejs/package-lock.json`. Temporary invariant: `linux-x64` only. Node.js is required for this module but NOT for the main SDK.
- **Section 4.7 — Native binary download and classifier JAR module** (the primary task description).
- **Hard scope invariant (Phase 4 preamble):** All native implementation work is limited to **Ubuntu 24.04 on linux-x64 only**. Other platforms are out of scope.

## Resolved decisions that constrain this task

- **Module GAV:** `com.github:copilot-sdk-java-runtime`
- **Version alignment:** `${project.version}` matches the npm `@github/copilot-<platform>` package version. No separate version property needed.
- **Download mechanism:** `npm pack @github/copilot-linux-x64@${project.version}` in `generate-resources` phase via `exec-maven-plugin`.
- **Staging path:** `target/native-staging/linux-x64/native/linux-x64/runtime.node` (after `tar` extraction).
- **Integrity verification:** Read `integrity` field (SHA-512) from `${copilot.sdk.root}/nodejs/package-lock.json` for `@github/copilot-linux-x64`. Verify downloaded `.tgz` against it.
- **Placeholder primary JAR:** Contains only `native/lib/copilot-runtime.properties` with `placeholder=true`. Satisfies Maven Central validation.
- **Classifier JAR:** One `maven-jar-plugin` execution with `<classifier>linux-x64</classifier>`, packaging from `target/native-staging/linux-x64/`.
- **Resource path convention:** `native/<classifier>/runtime.node` and `native/<classifier>/platform.properties` (containing `classifier=linux-x64` and `version=${project.version}`).
- **Node.js dependency:** Required for this module. Already required for Java E2E tests (replay proxy), so no new build dependency introduced.
- **Module can be skipped:** Developers working only on SDK Java code can run `mvn -pl sdk` to skip this module entirely.

## Deliverables

### Files to create

1. **`java/copilot-native/pom.xml`** — Module POM with:
   - Parent: `copilot-sdk-java-parent`
   - GAV: `com.github:copilot-sdk-java-runtime`
   - `exec-maven-plugin` in `generate-resources` phase: runs `npm pack @github/copilot-linux-x64@${project.version}`
   - `tar` extraction step to `target/native-staging/linux-x64/native/linux-x64/runtime.node`
   - SHA-512 integrity verification step reading from `${copilot.sdk.root}/nodejs/package-lock.json`
   - Default `maven-jar-plugin` execution producing placeholder primary JAR
   - Additional `maven-jar-plugin` execution with `<classifier>linux-x64</classifier>`, packaging from `target/native-staging/linux-x64/`
   - Optional: `build-helper-maven-plugin` wiring prepared for future Gradle Module Metadata expansion

2. **`java/copilot-native/src/main/resources/native/lib/copilot-runtime.properties`** — Placeholder properties file: `placeholder=true`, `version=${project.version}`

3. **Platform properties for the classifier JAR** — The build step must produce `native/linux-x64/platform.properties` containing:
   ```properties
   classifier=linux-x64
   version=${project.version}
   ```

### Files to modify

4. **`java/pom.xml`** (parent POM) — Uncomment/add `copilot-native` in `<modules>`. Ensure `copilot.sdk.root` property is defined so the native module can reference `nodejs/package-lock.json`.

## Gating tests and criteria

1. **Classifier JAR produced:** `mvn package -pl copilot-native` produces the `linux-x64` classifier JAR containing `native/linux-x64/runtime.node`.
2. **Placeholder JAR produced:** The primary JAR contains `native/lib/copilot-runtime.properties` and NO native binaries.
3. **SHA-512 verification passes:** The integrity hash from `nodejs/package-lock.json` matches the downloaded `.tgz`.
4. **Reactor build:** `mvn clean verify` from `java/` runs the full reactor (including this module).
5. **SDK-only build:** `mvn -pl sdk clean verify` still works (skipping the native module).
6. **All prior tests pass:** `mvn verify` from `java/` passes.

## Out of scope

- Building classifier JARs for any platform other than `linux-x64` (deferred to later phase).
- Monolithic uber-JAR assembly (`copilot-native-all` module) — deferred.
- Gradle Module Metadata (`.module`) file — deferred (wiring can be prepared but not activated).
- Publishing to Maven Central — separate workflow concern.
